package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

/**
 * Prepares a complete fuel burn without changing inputs or the boiler's warmup
 * state.
 */
public final class BoilerProcessingLogic extends ProcessingLogic {
    public static final class State {
        public final int heat;
        public final int warmupRate;
        public final int efficiency;
        public final int maximumEfficiency;
        public final int circuit;
        public final int fuelRemainder;
        public final int heatRemainder;

        public State(int heat, int warmupRate, int efficiency, int maximumEfficiency, int circuit, int fuelRemainder,
                int heatRemainder) {
            this.heat = heat;
            this.warmupRate = warmupRate;
            this.efficiency = efficiency;
            this.maximumEfficiency = maximumEfficiency;
            this.circuit = circuit;
            this.fuelRemainder = fuelRemainder;
            this.heatRemainder = heatRemainder;
        }
    }

    /** Published only after the input transaction succeeds. */
    public static final class Burn {
        public final int heat;
        public final int circuit;
        public final int efficiencyIncrease;
        public final int tickEfficiencyIncrease;
        public final int fuelRemainder;
        public final int heatRemainder;

        private Burn(int heat, int circuit, int efficiencyIncrease, int tickEfficiencyIncrease, int fuelRemainder,
                int heatRemainder) {
            this.heat = heat;
            this.circuit = circuit;
            this.efficiencyIncrease = efficiencyIncrease;
            this.tickEfficiencyIncrease = tickEfficiencyIncrease;
            this.fuelRemainder = fuelRemainder;
            this.heatRemainder = heatRemainder;
        }
    }

    private final Supplier<State> stateSupplier;
    private final LongUnaryOperator runtimeBoost;
    private Burn burn;

    public BoilerProcessingLogic(Supplier<State> stateSupplier, LongUnaryOperator runtimeBoost) {
        this.stateSupplier = stateSupplier;
        this.runtimeBoost = runtimeBoost;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public Result process(Request request) {
        State state = stateSupplier.get();
        if (state.heat <= 0 || state.warmupRate <= 0) {
            return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
        }
        try {
            for (ProcessingInputs source : request.inputs) {
                Result diesel = liquidFuel(request, source, state, GT_Recipe.GT_Recipe_Map.sDieselFuels, true);
                if (diesel != null) {
                    return diesel;
                }
                Result dense = liquidFuel(request, source, state, GT_Recipe.GT_Recipe_Map.sDenseLiquidFuels, false);
                if (dense != null) {
                    return dense;
                }
                ProcessingInputs inputs = source.copy();
                for (ItemStack item : inputs.items) {
                    if (item == null || item.stackSize <= 0 || GT_Utility.getFluidForFilledItem(item, true) != null) {
                        continue;
                    }
                    int fuelValue = GT_ModHandler.getFuelValue(item);
                    if (fuelValue / 80 <= 0) {
                        continue;
                    }
                    int remainder = state.fuelRemainder + fuelValue % 80;
                    long duration = runtimeBoost.applyAsLong((long) fuelValue / 80 + remainder / 80);
                    ItemStack container = GT_Utility.getContainerItem(GT_Utility.copyAmount(1, item), true);
                    item.stackSize--;
                    return finish(request, inputs, state, duration, 1, true, remainder % 80,
                            container == null ? new ItemStack[0] : new ItemStack[] { container });
                }
            }
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    private Result liquidFuel(Request request, ProcessingInputs source, State state, GT_Recipe.GT_Recipe_Map map,
            boolean diesel) {
        for (GT_Recipe recipe : map.mRecipeList) {
            FluidStack fluid = GT_Utility.getFluidForFilledItem(recipe.getRepresentativeInput(0), true);
            if (fluid == null || (diesel && recipe.mSpecialValue <= 1)) {
                continue;
            }
            fluid.amount = 1000;
            ProcessingInputs inputs = source.copy();
            if (!inputs.consumeFluid(fluid)) {
                continue;
            }
            long duration = runtimeBoost.applyAsLong(diesel ? recipe.mSpecialValue / 2 : recipe.mSpecialValue * 2L);
            return finish(request, inputs, state, Math.max(1, duration), diesel ? 4 : 1, false, state.fuelRemainder,
                    new ItemStack[0]);
        }
        return null;
    }

    private Result finish(Request request, ProcessingInputs inputs, State state, long duration, int warmupMultiplier,
            boolean solid, int fuelRemainder, ItemStack[] containers) {
        int heat = Math.max(25, state.heat - 25 * state.circuit);
        int heatRemainder = state.heatRemainder;
        if (state.efficiency >= state.maximumEfficiency) {
            long totalHeat = Math.addExact(Math.multiplyExact(duration, state.heat), heatRemainder);
            duration = totalHeat / heat;
            heatRemainder = (int) (totalHeat % heat);
        }
        int ticks = Math.toIntExact(duration);
        if (ticks <= 0) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
        long warmup = Math.multiplyExact(Math.multiplyExact(duration, state.warmupRate), warmupMultiplier);
        int tickWarmup = solid && warmup > 5000 ? 20 : 0;
        // Efficiency is capped at 10000 by the machine after the burn.
        int cycleWarmup = tickWarmup > 0 ? 0 : (int) Math.min(10000, warmup);
        // Protection gates the next fuel charge. Once lit, a burn never pauses for its
        // steam output.
        if (request.enabled(PROTECT_FLUIDS) && !request.outputs.areFluidOutputsEmpty()) {
            return Result.failure(CheckRecipeResult.FLUID_OUTPUT_FULL);
        }
        int steam = Math.toIntExact(Math.max(1, heat * 2L * state.maximumEfficiency / 10000));
        CheckRecipeResult fit = request.outputs.canFit(containers, new FluidStack[] { GT_ModHandler.getSteam(steam) },
                request.enabled(PROTECT_ITEMS), request.enabled(PROTECT_FLUIDS));
        if (!fit.wasSuccessful()) {
            return Result.failure(fit);
        }
        Burn prepared = new Burn(heat, state.circuit, cycleWarmup, tickWarmup, fuelRemainder, heatRemainder);
        if (!inputs.commit()) {
            return Result.failure(CheckRecipeResult.INPUT_CHANGED);
        }
        burn = prepared;
        return Result.fuelBurn(inputs, ticks, containers);
    }

    public Burn getBurn() {
        return burn;
    }
}
