package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * One-tick fuel cycles. All flow calculations operate on snapshots until
 * commit.
 */
public final class TurbineProcessingLogic extends ProcessingLogic {
    public enum FuelType {
        STEAM, SUPERHEATED_STEAM, GAS, PLASMA
    }

    public static final class State {
        public final int optimalFlow;
        public final int efficiency;
        public final long power;
        public final int steamRemainder;

        public State(int optimalFlow, int efficiency, long power, int steamRemainder) {
            this.optimalFlow = optimalFlow;
            this.efficiency = efficiency;
            this.power = power;
            this.steamRemainder = steamRemainder;
        }
    }

    private final FuelType type;
    private final Supplier<State> stateSupplier;
    private final ToIntFunction<FluidStack> fuelValue;
    private long optimalFlow;
    private long storedFuel;
    private long consumedFuel;
    private int steamRemainder;

    public TurbineProcessingLogic(FuelType type, Supplier<State> stateSupplier, ToIntFunction<FluidStack> fuelValue) {
        this.type = type;
        this.stateSupplier = stateSupplier;
        this.fuelValue = fuelValue;
    }

    public static int findFuelValue(GT_Recipe.GT_Recipe_Map map, FluidStack fluid) {
        if (map == null || fluid == null) {
            return 0;
        }
        for (GT_Recipe recipe : map.mRecipeList) {
            FluidStack fuel = GT_Utility.getFluidForFilledItem(recipe.getRepresentativeInput(0), true);
            if (fuel != null && fuel.isFluidEqual(fluid)) {
                return recipe.mSpecialValue;
            }
        }
        return 0;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public Result process(Request request) {
        State state = stateSupplier.get();
        if (state.optimalFlow <= 0 || state.efficiency <= 0) {
            return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
        }
        if (request.inputs.isEmpty()) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        try {
            return prepare(request, state);
        } catch (OutputOverflowException exception) {
            return Result.failure(CheckRecipeResult.OUTPUT_OVERFLOW);
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    private Result prepare(Request request, State state) {
        ProcessingInputs inputs = request.inputs.get(0).copy();
        FluidStack fuel = firstFuel(inputs.fluids);
        int value = fuel == null ? 0 : fuelValue.applyAsInt(fuel);
        boolean steam = type == FuelType.STEAM || type == FuelType.SUPERHEATED_STEAM;
        long optimum = steam ? state.optimalFlow : optimalFuelFlow(state.optimalFlow, value);
        long remaining = Math.min(Integer.MAX_VALUE, optimum * 5 / 4);
        long total = 0;
        long stored = 0;
        if (fuel != null && optimum > 0) {
            for (FluidStack fluid : inputs.fluids) {
                if (fluid != null && fluid.amount > 0 && matchesFuel(fluid, fuel)) {
                    stored = Math.addExact(stored, fluid.amount);
                    int take = (int) Math.min(fluid.amount, remaining);
                    fluid.amount -= take;
                    remaining -= take;
                    total += take;
                }
            }
        }
        long target = power(total, optimum, value, state.efficiency);
        long nextPower = ramp(state.power, target);
        if (nextPower <= 0) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        int remainder = state.steamRemainder;
        FluidStack output = null;
        if (total > 0) {
            switch (type) {
            case STEAM:
                long water = total + remainder;
                output = GT_ModHandler.getDistilledWater(water / 160);
                remainder = (int) (water % 160);
                break;
            case SUPERHEATED_STEAM:
                output = GT_ModHandler.getSteam(total);
                break;
            case PLASMA:
                output = cooledPlasma(fuel, Math.toIntExact(total));
                break;
            default:
                break;
            }
        }
        FluidStack[] outputs = output == null || output.amount <= 0 ? new FluidStack[0] : new FluidStack[] { output };
        outputs = request.outputs.prepareFluids(outputs);
        CheckRecipeResult fit = request.outputs.canFit(new ItemStack[0], outputs, false,
                request.enabled(PROTECT_FLUIDS));
        if (!fit.wasSuccessful()) {
            return Result.failure(fit);
        }
        if (!inputs.commit()) {
            return Result.failure(CheckRecipeResult.INPUT_CHANGED);
        }
        optimalFlow = optimum;
        storedFuel = stored - total;
        consumedFuel = total;
        steamRemainder = remainder;
        return Result.generation(inputs, nextPower, outputs);
    }

    private FluidStack firstFuel(FluidStack[] fluids) {
        for (FluidStack fluid : fluids) {
            if (fluid != null && fluid.amount > 0
                    && (type == FuelType.GAS || type == FuelType.PLASMA || matchesFuel(fluid, fluid))) {
                return fluid;
            }
        }
        return null;
    }

    private boolean matchesFuel(FluidStack fluid, FluidStack first) {
        String name = fluid.getFluid().getUnlocalizedName(fluid);
        switch (type) {
        case STEAM:
            return name.equals("fluid.steam") || name.equals("ic2.fluidSteam")
                    || name.equals("fluid.mfr.steam.still.name");
        case SUPERHEATED_STEAM:
            return name.equals("ic2.fluidSuperheatedSteam");
        default:
            return fluid.isFluidEqual(first);
        }
    }

    private long optimalFuelFlow(int rotorFlow, int value) {
        if (value <= 0) {
            return 0;
        }
        if (type == FuelType.GAS) {
            return rotorFlow / value;
        }
        // Preserve this fork's plasma flow rounding and forty-tick fuel multiplier.
        long scaled = (long) rotorFlow * 40 + value - 1L;
        return (scaled + value - 1L) / value;
    }

    private long power(long flow, long optimum, int value, int efficiency) {
        if (flow <= 0 || optimum <= 0) {
            return 0;
        }
        long raw = type == FuelType.GAS ? Math.multiplyExact(flow, value)
                : type == FuelType.PLASMA ? Math.multiplyExact(Math.min(flow, optimum), value)
                        : Math.min(flow, optimum);
        if (flow != optimum) {
            float fitting = 1.0f - Math.abs((flow - optimum) / (float) optimum);
            if (flow > optimum && type != FuelType.GAS) {
                fitting = 1.0f;
            }
            raw = (long) (raw * fitting);
        }
        int divisor = type == FuelType.STEAM ? 20000 : 10000;
        long result = Math.addExact(Math.multiplyExact(raw / divisor, efficiency),
                raw % divisor * efficiency / divisor);
        return flow != optimum && type != FuelType.GAS ? Math.max(1, result) : result;
    }

    public static long ramp(long previous, long target) {
        long difference = Math.subtractExact(target, previous);
        long change = Math.max(10, Math.abs(difference) / 100);
        return Math.abs(difference) <= change ? target : Math.addExact(previous, difference > 0 ? change : -change);
    }

    private static FluidStack cooledPlasma(FluidStack fuel, int amount) {
        String name = FluidRegistry.getFluidName(fuel);
        int separator = name.indexOf('.');
        if (separator < 0) {
            return null;
        }
        String material = name.substring(separator + 1);
        FluidStack output = FluidRegistry.getFluidStack(material, amount);
        return output == null ? FluidRegistry.getFluidStack("molten." + material, amount) : output;
    }

    public long getOptimalFlow() {
        return optimalFlow;
    }

    public long getStoredFuel() {
        return storedFuel;
    }

    public long getConsumedFuel() {
        return consumedFuel;
    }

    public int getSteamRemainder() {
        return steamRemainder;
    }
}
