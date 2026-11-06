package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Checks a prepared world operation before committing its external effects. */
public final class WorldProcessingLogic extends ProcessingLogic {
    private final Function<Request, Operation> prepare;

    public WorldProcessingLogic(Function<Request, Operation> prepare) {
        this.prepare = prepare;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public Result process(Request request) {
        try {
            Operation operation = prepare.apply(request);
            if (!operation.status.wasSuccessful()) {
                return Result.failure(operation.status);
            }
            long power = Math.multiplyExact(request.voltage, request.amperage);
            if (operation.eut > power) {
                return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
            }
            if (operation.eut >= 0) {
                efficiencyAdjustedEUt(operation.eut, request.efficiency);
            } else {
                long generation = Math.negateExact(operation.eut);
                Math.addExact(Math.multiplyExact(generation / 10000, request.efficiency),
                        generation % 10000 * request.efficiency / 10000);
            }
            CheckRecipeResult fit = request.outputs.canFit(operation.items, operation.fluids,
                    request.enabled(PROTECT_ITEMS), request.enabled(PROTECT_FLUIDS));
            if (!fit.wasSuccessful()) {
                return Result.failure(fit);
            }
            Result result = Result.cycle(null, operation.eut, operation.duration, 1, 1, operation.items,
                    operation.fluids);
            return operation.commit.getAsBoolean() ? result : Result.failure(CheckRecipeResult.INPUT_CHANGED);
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    public static final class Operation {
        private final CheckRecipeResult status;
        private final long eut;
        private final int duration;
        private final ItemStack[] items;
        private final FluidStack[] fluids;
        private final BooleanSupplier commit;

        public Operation(long eut, int duration, ItemStack[] items, FluidStack[] fluids, BooleanSupplier commit) {
            if (eut == Long.MIN_VALUE || duration <= 0) {
                throw new IllegalArgumentException("Invalid world operation cycle");
            }
            status = CheckRecipeResult.SUCCESS;
            this.eut = eut;
            this.duration = duration;
            this.items = ProcessingInputs.copyItems(items);
            this.fluids = ProcessingInputs.copyFluids(fluids);
            this.commit = commit;
        }

        private Operation(CheckRecipeResult status) {
            this.status = status;
            eut = 0;
            duration = 0;
            items = new ItemStack[0];
            fluids = new FluidStack[0];
            commit = () -> false;
        }

        public static Operation failure(CheckRecipeResult status) {
            return new Operation(status);
        }
    }
}
