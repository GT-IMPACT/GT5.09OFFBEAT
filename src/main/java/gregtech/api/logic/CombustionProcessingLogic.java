package gregtech.api.logic;

import gregtech.api.enums.Materials;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Supplier;

/** Fuel, oxygen and lubricant are committed together for one engine tick. */
public final class CombustionProcessingLogic extends ProcessingLogic {
    public static final class Settings {
        public final int power;
        public final int boostedFuelPower;
        public final int oxygen;
        public final int lubricant;
        public final int lubricantPeriod;
        public final int warmup;

        public Settings(int power, int boostedFuelPower, int oxygen, int lubricant, int lubricantPeriod, int warmup) {
            this.power = power;
            this.boostedFuelPower = boostedFuelPower;
            this.oxygen = oxygen;
            this.lubricant = lubricant;
            this.lubricantPeriod = lubricantPeriod;
            this.warmup = warmup;
        }
    }

    public static final class State {
        public final int runtime;
        public final int efficiency;
        public final boolean boosted;

        public State(int runtime, int efficiency, boolean boosted) {
            this.runtime = runtime;
            this.efficiency = efficiency;
            this.boosted = boosted;
        }
    }

    private final Settings settings;
    private final Supplier<State> stateSupplier;
    private boolean boosted;
    private int consumption;
    private int fuelValue;
    private int remaining;

    public CombustionProcessingLogic(Settings settings, Supplier<State> stateSupplier) {
        this.settings = settings;
        this.stateSupplier = stateSupplier;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public Result process(Request request) {
        State state = stateSupplier.get();
        for (ProcessingInputs source : request.inputs) {
            for (FluidStack supplied : source.fluids) {
                for (GT_Recipe recipe : GT_Recipe.GT_Recipe_Map.sDieselFuels.mRecipeList) {
                    FluidStack fuel = GT_Utility.getFluidForFilledItem(recipe.getRepresentativeInput(0), true);
                    if (fuel == null || !fuel.isFluidEqual(supplied) || recipe.mSpecialValue <= 0) {
                        continue;
                    }
                    ProcessingInputs inputs = source.copy();
                    // Preserve the engine's existing one-tick delay when changing boosted fuel
                    // consumption.
                    int amount = Math.max(1,
                            (state.boosted ? settings.boostedFuelPower : settings.power) / recipe.mSpecialValue);
                    fuel.amount = amount;
                    if (!inputs.consumeFluid(fuel)) {
                        continue;
                    }
                    boolean nextBoost = inputs.consumeFluid(Materials.Oxygen.getGas(settings.oxygen));
                    FluidStack lubricant = Materials.Lubricant.getFluid(settings.lubricant);
                    if (!inputs.copy().consumeFluid(lubricant)) {
                        return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
                    }
                    if (state.runtime % settings.lubricantPeriod == 0) {
                        lubricant.amount = settings.lubricant + (nextBoost ? 1 : 0);
                        if (!inputs.consumeFluid(lubricant)) {
                            return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
                        }
                    }
                    long stored = 0;
                    for (FluidStack fluid : inputs.fluids) {
                        if (fluid.isFluidEqual(fuel)) {
                            stored += fluid.amount;
                        }
                    }
                    Result result = Result.cycle(inputs, state.efficiency < 2000 ? 0 : -settings.power, 1, 1, 1,
                            new ItemStack[0], new FluidStack[0]);
                    if (!inputs.commit()) {
                        return Result.failure(CheckRecipeResult.INPUT_CHANGED);
                    }
                    boosted = nextBoost;
                    consumption = amount;
                    fuelValue = recipe.mSpecialValue;
                    remaining = (int) Math.min(Integer.MAX_VALUE, stored);
                    return result;
                }
            }
        }
        return Result.failure(CheckRecipeResult.NO_RECIPE);
    }

    public boolean isBoosted() {
        return boosted;
    }

    public int getConsumption() {
        return consumption;
    }

    public int getFuelValue() {
        return fuelValue;
    }

    public int getRemaining() {
        return remaining;
    }

    public int getWarmup() {
        return settings.warmup;
    }
}
