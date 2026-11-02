package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_ModHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The hot-fluid transaction is separate from distilled-water consumption during
 * the thermal cycle.
 */
public final class HeatExchangeProcessingLogic extends ProcessingLogic {
    private final IntSupplier circuit;
    private final Supplier<Float> penalty;
    private int heat;
    private int threshold;
    private boolean superheated;

    public HeatExchangeProcessingLogic(IntSupplier circuit, Supplier<Float> penalty) {
        this.circuit = circuit;
        this.penalty = penalty;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public Result process(Request request) {
        if (request.inputs.isEmpty() || request.inputs.get(0).fluids.length == 0) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        ProcessingInputs inputs = request.inputs.get(0).copy();
        FluidStack hot = inputs.fluids[0];
        if (hot.amount <= 0) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        boolean lava = GT_ModHandler.isLava(hot);
        if (!lava && !hot.isFluidEqual(FluidRegistry.getFluidStack("ic2hotcoolant", 1))) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        int configuration = circuit.getAsInt();
        int reduction = configuration >= 1 && configuration <= GT_IntegratedCircuit_Item.MAX_CONFIG
            ? configuration - 1 : 0;
        float efficiency = 1f - reduction * penalty.get();
        int nextThreshold = (4000 - 150 * reduction) / (lava ? 4 : 5);
        int amount = Math.min(hot.amount, nextThreshold * 2);
        float generated = amount * (lava ? 4f : 10f) * efficiency;
        if (!Float.isFinite(generated) || generated < 0 || generated >= Integer.MAX_VALUE) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
        int nextHeat = (int) generated;
        boolean nextSuperheated = hot.amount >= nextThreshold;
        long steamAmount = nextSuperheated ? nextHeat : nextHeat * 2L;
        if (steamAmount > Integer.MAX_VALUE) {
            return Result.failure(CheckRecipeResult.OUTPUT_OVERFLOW);
        }
        FluidStack cold = FluidRegistry.getFluidStack(lava ? "ic2pahoehoelava" : "ic2coolant", amount);
        FluidStack steam = nextSuperheated ? FluidRegistry.getFluidStack("ic2superheatedsteam", nextHeat)
                : GT_ModHandler.getSteam(nextHeat * 2L);
        if (cold == null || steam == null) {
            return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
        }
        if (request.enabled(PROTECT_FLUIDS) && !request.outputs.areFluidOutputsEmpty()) {
            return Result.failure(CheckRecipeResult.FLUID_OUTPUT_FULL);
        }
        CheckRecipeResult fit = request.outputs.canFit(new ItemStack[0], new FluidStack[] { cold, steam }, false,
                request.enabled(PROTECT_FLUIDS));
        if (!fit.wasSuccessful()) {
            return Result.failure(fit);
        }
        hot.amount -= amount;
        Result result = Result.cycle(inputs, 0, 20, 1, 1, new ItemStack[0], new FluidStack[] { cold });
        if (!inputs.commit()) {
            return Result.failure(CheckRecipeResult.INPUT_CHANGED);
        }
        heat = nextHeat;
        threshold = nextThreshold;
        superheated = nextSuperheated;
        return result;
    }

    public int getHeat() {
        return heat;
    }

    public int getThreshold() {
        return threshold;
    }

    public boolean isSuperheated() {
        return superheated;
    }
}
