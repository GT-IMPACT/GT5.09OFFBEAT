package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Mixed furnace inputs share one coil-sized cycle and one overclock
 * calculation.
 */
public final class SmeltingProcessingLogic extends ProcessingLogic {
    private final IntSupplier level;
    private final IntSupplier discount;

    public SmeltingProcessingLogic(IntSupplier level, IntSupplier discount) {
        this.level = level;
        this.discount = discount;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public int getAvailableParallel() {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, level.getAsInt() * 8L));
    }

    @Override
    public Result process(Request request) {
        int coilLevel = level.getAsInt();
        int coilDiscount = discount.getAsInt();
        if (coilLevel <= 0 || coilDiscount <= 0) {
            return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
        }
        if (request.voltage <= 0) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_VOLTAGE);
        }
        try {
            long power = Math.multiplyExact(request.voltage, request.amperage);
            OverclockCalculator calculator = new OverclockCalculator()
                    .setEUtModifier((double) coilLevel / coilDiscount);
            if (power < calculator.recipePower(4)) {
                return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
            }
            OverclockCalculator.Result clock = calculator.calculate(4, 512, power, 1);
            efficiencyAdjustedEUt(clock.eut, request.efficiency);
            int limit = Math.toIntExact(coilLevel * 8L);
            int batchLimit = ParallelHelper.batchLimit(limit, clock.duration, request.enabled(BATCH));
            CheckRecipeResult failure = CheckRecipeResult.NO_RECIPE;
            for (ProcessingInputs source : request.inputs) {
                ProcessingInputs inputs = source.copy();
                List<ItemStack> products = new ArrayList<>();
                List<ItemStack> capacityProducts = new ArrayList<>();
                boolean found;
                do {
                    found = false;
                    for (ItemStack item : inputs.items) {
                        if (products.size() >= batchLimit) {
                            break;
                        }
                        if (item == null || item.stackSize <= 0) {
                            continue;
                        }
                        ItemStack output = GT_ModHandler.getSmeltingOutput(item, false, null);
                        if (output == null) {
                            continue;
                        }
                        if (request.enabled(PROTECT_ITEMS)) {
                            ItemStack combined = reserveOutput(capacityProducts, output);
                            CheckRecipeResult fit = request.outputs.canFit(capacityProducts.toArray(new ItemStack[0]),
                                    new FluidStack[0], true, false);
                            if (!fit.wasSuccessful()) {
                                combined.stackSize -= output.stackSize;
                                if (combined.stackSize == 0) {
                                    capacityProducts.remove(combined);
                                }
                                failure = fit;
                                continue;
                            }
                        }
                        products.add(output.copy());
                        item.stackSize--;
                        found = true;
                    }
                } while (found && products.size() < batchLimit);
                if (products.isEmpty()) {
                    continue;
                }
                int operations = products.size();
                int parallel = Math.min(limit, operations);
                int duration = Math.toIntExact(((long) clock.duration * operations + parallel - 1) / parallel);
                Result result = Result.cycle(inputs, clock.eut, duration, parallel, operations,
                        products.toArray(new ItemStack[0]), new FluidStack[0]);
                if (!inputs.commit()) {
                    return Result.failure(CheckRecipeResult.INPUT_CHANGED);
                }
                return result;
            }
            return Result.failure(failure);
        } catch (OverclockCalculator.DurationOverflowException exception) {
            return Result.failure(CheckRecipeResult.DURATION_OVERFLOW);
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    private static ItemStack reserveOutput(List<ItemStack> outputs, ItemStack addition) {
        for (ItemStack output : outputs) {
            if (output.isItemEqual(addition) && ItemStack.areItemStackTagsEqual(output, addition)) {
                output.stackSize = Math.addExact(output.stackSize, addition.stackSize);
                return output;
            }
        }
        ItemStack output = addition.copy();
        outputs.add(output);
        return output;
    }
}
