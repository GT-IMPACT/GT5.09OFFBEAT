package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.IntSupplier;

/**
 * Fixed-rate cycles without inventory recipes, such as room upkeep or a sealed
 * burn.
 */
public class TimedProcessingLogic extends ProcessingLogic {
    private final long eut;
    private final IntSupplier duration;

    public TimedProcessingLogic(long eut, IntSupplier duration) {
        if (eut < 0) {
            throw new IllegalArgumentException("Negative consumption");
        }
        this.eut = eut;
        this.duration = duration;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public Result process(Request request) {
        try {
            if (eut > Math.multiplyExact(request.voltage, request.amperage)) {
                return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
            }
            int ticks = duration.getAsInt();
            if (ticks <= 0) {
                return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
            }
            return Result.cycle(null, eut, ticks, 1, 1, new ItemStack[0], new FluidStack[0]);
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }
}
