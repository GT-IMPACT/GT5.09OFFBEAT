package gregtech.api.logic;

import gregtech.api.enums.GT_Values;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Prepares filtration; pollution and tool wear are applied by the machine after
 * commit.
 */
public final class AirFilterProcessingLogic extends ProcessingLogic {
    private final Supplier<State> stateSupplier;
    private final Supplier<GT_Recipe> filterRecipe;
    private int reduction;
    private int toolEfficiency;

    public AirFilterProcessingLogic(Supplier<State> stateSupplier, Supplier<GT_Recipe> filterRecipe) {
        this.stateSupplier = stateSupplier;
        this.filterRecipe = filterRecipe;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    public int getReduction() {
        return reduction;
    }

    public int getToolEfficiency() {
        return toolEfficiency;
    }

    @Override
    public Result process(Request request) {
        try {
            State state = stateSupplier.get();
            long eut = state.polluted ? filtrationPower(state.voltage) : 32;
            long power = Math.multiplyExact(request.voltage, request.amperage);
            if (eut <= 0 || eut > power) {
                return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
            }
            efficiencyAdjustedEUt(eut, request.efficiency);
            ProcessingInputs inputs = request.inputs.get(0).copy();
            ItemStack[] outputs = new ItemStack[0];
            int preparedReduction = 0;
            if (state.polluted) {
                GT_Recipe recipe = filterRecipe.get();
                ItemStack[] candidates = filterCandidates(inputs.items);
                boolean filtered = recipe.isRecipeInputEqual(true, null, candidates);
                if (filtered) {
                    outputs = new ItemStack[] { recipe.getOutput(0) };
                }
                long amount = Math.multiplyExact(state.mufflerReduction, filtered ? 2L : 1L);
                amount = GT_Utility.safeInt(Math.multiplyExact(amount, state.toolEfficiency)) / 10000;
                preparedReduction = GT_Utility.safeInt(Math.multiplyExact(amount, request.efficiency) / 10000);
            }
            CheckRecipeResult fit = request.outputs.canFit(outputs, new FluidStack[0], request.enabled(PROTECT_ITEMS),
                    false);
            if (!fit.wasSuccessful()) {
                return Result.failure(fit);
            }
            Result result = Result.cycle(inputs, eut, 200, 1, 1, outputs, new FluidStack[0]);
            if (!state.unchanged.getAsBoolean() || !inputs.commit()) {
                return Result.failure(CheckRecipeResult.INPUT_CHANGED);
            }
            reduction = preparedReduction;
            toolEfficiency = state.toolEfficiency;
            return result;
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    private static long filtrationPower(long voltage) {
        int tier = Math.max(0, GT_Utility.getTier(voltage));
        long eut = voltage - (voltage >> 2);
        if (tier == 0) {
            return eut / 4;
        }
        long control = Math.max(eut, GT_Values.V[1]);
        while (control <= GT_Values.V[tier - 1]) {
            control = Math.multiplyExact(control, 4);
            eut = Math.multiplyExact(eut, 4);
        }
        return Math.max(1, eut);
    }

    /**
     * Preserve the original two distinct stacks, choosing the largest duplicate.
     */
    private static ItemStack[] filterCandidates(ItemStack[] items) {
        List<ItemStack> candidates = new ArrayList<>();
        for (ItemStack item : items) {
            int duplicate = -1;
            for (int i = 0; i < candidates.size(); i++) {
                if (GT_Utility.areStacksEqual(candidates.get(i), item)) {
                    duplicate = i;
                    break;
                }
            }
            if (duplicate < 0) {
                candidates.add(item);
            } else if (candidates.get(duplicate).stackSize < item.stackSize) {
                candidates.remove(duplicate);
                candidates.add(item);
            }
        }
        return candidates.subList(0, Math.min(2, candidates.size())).toArray(new ItemStack[0]);
    }

    public static final class State {
        public final boolean polluted;
        public final long voltage;
        public final long mufflerReduction;
        public final int toolEfficiency;
        public final BooleanSupplier unchanged;

        public State(boolean polluted, long voltage, long mufflerReduction, int toolEfficiency,
                BooleanSupplier unchanged) {
            this.polluted = polluted;
            this.voltage = voltage;
            this.mufflerReduction = mufflerReduction;
            this.toolEfficiency = toolEfficiency;
            this.unchanged = unchanged;
        }
    }
}
