package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Function;
import java.util.function.Supplier;

/** Prepares a fixed fuel burn with all auxiliary fluids before committing any input. */
public final class FuelProcessingLogic extends ProcessingLogic {

    private final Supplier<GT_Recipe.GT_Recipe_Map> recipeMap;
    private final Function<GT_Recipe, FuelCycle> prepare;
    private int consumption;
    private GT_Recipe currentRecipe;

    public FuelProcessingLogic(
            Supplier<GT_Recipe.GT_Recipe_Map> recipeMap,
            Function<GT_Recipe, FuelCycle> prepare
    ) {
        this.recipeMap = recipeMap;
        this.prepare = prepare;
    }

    @Override
    public Result process(Request request) {
        GT_Recipe.GT_Recipe_Map map = recipeMap.get();
        if (map == null) {
            return Result.failure(CheckRecipeResult.RECIPE_MAP_UNAVAILABLE);
        }
        if (request.enabled(PROTECT_FLUIDS) && !request.outputs.areFluidOutputsEmpty()) {
            return Result.failure(CheckRecipeResult.FLUID_OUTPUT_FULL);
        }
        CheckRecipeResult failure = CheckRecipeResult.NO_RECIPE;
        try {
            for (ProcessingInputs source : request.inputs) {
                for (GT_Recipe recipe : map.mRecipeList) {
                    if (!recipe.mEnabled || recipe.mFakeRecipe) {
                        continue;
                    }
                    FuelCycle cycle = prepare.apply(recipe);
                    if (cycle == null || cycle.recipe == null && (cycle.fuel == null || cycle.fuel.amount <= 0)) {
                        continue;
                    }
                    ProcessingInputs inputs = source.copy();
                    if (cycle.recipe != null ? !inputs.consume(cycle.recipe, 1) : !inputs.consumeFluid(cycle.fuel)) {
                        continue;
                    }
                    boolean available = true;
                    for (FluidStack auxiliary : cycle.auxiliaries) {
                        if (!inputs.consumeFluid(auxiliary)) {
                            available = false;
                            break;
                        }
                    }
                    if (!available) {
                        failure = CheckRecipeResult.MACHINE_CONDITION;
                        continue;
                    }
                    Math.addExact(Math.multiplyExact(cycle.generation / 10000, request.efficiency),
                            cycle.generation % 10000 * request.efficiency / 10000);
                    ItemStack[] items = cycle.recipe == null ? new ItemStack[0]
                            : ProcessingInputs.copyItems(cycle.recipe.mOutputs);
                    FluidStack[] outputs = request.outputs.prepareFluids(cycle.outputs);
                    CheckRecipeResult fit = request.outputs.canFit(items, outputs,
                            request.enabled(PROTECT_ITEMS), request.enabled(PROTECT_FLUIDS));
                    if (!fit.wasSuccessful()) {
                        failure = fit;
                        continue;
                    }
                    Result result = Result.cycle(inputs, -cycle.generation, cycle.duration, 1, 1,
                            items, outputs);
                    if (recipeMap.get() != map || !inputs.commit()) {
                        return Result.failure(CheckRecipeResult.INPUT_CHANGED);
                    }
                    consumption = cycle.fuel == null ? 0 : cycle.fuel.amount;
                    currentRecipe = recipe;
                    return result;
                }
            }
            return Result.failure(failure);
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    public GT_Recipe getCurrentRecipe() {
        return currentRecipe;
    }

    public int getConsumption() {
        return consumption;
    }

    public static final class FuelCycle {

        private final GT_Recipe recipe;
        private final long generation;
        private final int duration;
        private final FluidStack fuel;
        private final FluidStack[] auxiliaries;
        private final FluidStack[] outputs;

        public FuelCycle(GT_Recipe recipe, long generation, int duration) {
            if (generation < 0 || duration <= 0) {
                throw new IllegalArgumentException("Invalid fuel burn");
            }
            this.recipe = recipe;
            this.generation = generation;
            this.duration = duration;
            fuel = null;
            auxiliaries = new FluidStack[0];
            outputs = ProcessingInputs.copyFluids(recipe.mFluidOutputs);
        }

        public FuelCycle(
                long generation,
                int duration,
                FluidStack fuel,
                FluidStack[] auxiliaries,
                FluidStack[] outputs
        ) {
            if (generation < 0 || duration <= 0) {
                throw new IllegalArgumentException("Invalid fuel burn");
            }
            recipe = null;
            this.generation = generation;
            this.duration = duration;
            this.fuel = fuel == null ? null : fuel.copy();
            this.auxiliaries = ProcessingInputs.copyFluids(auxiliaries);
            this.outputs = ProcessingInputs.copyFluids(outputs);
        }
    }
}
