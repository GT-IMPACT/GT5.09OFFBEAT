package gregtech.api.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Shared GTNH helpers used by native ModularUI2 widgets and covers. */
public final class GTUtility {

    private GTUtility() {
    }

    public static ItemStack getFluidDisplayStack(FluidStack fluid, boolean useStackSize) {
        return GT_Utility.getFluidDisplayStack(fluid, useStackSize);
    }

    public static ItemStack getFluidDisplayStack(Fluid fluid) {
        return GT_Utility.getFluidDisplayStack(fluid);
    }

    public static FluidStack getFluidFromDisplayStack(ItemStack stack) {
        return GT_Utility.getFluidFromDisplayStack(stack);
    }

    public static byte convertRatioToRedstone(long used, long max, int threshold, boolean inverted) {
        byte signal;
        if (used <= 0) {
            signal = 0;
        } else if (used >= max) {
            signal = 15;
        } else {
            signal = (byte) (1 + (14 * used) / max);
        }
        if (inverted) {
            signal = (byte) (15 - signal);
        }
        if (threshold > 0) {
            if (inverted && used >= threshold || !inverted && used < threshold) {
                return 0;
            }
        }
        return signal;
    }
}
