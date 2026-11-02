package gregtech.api.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Public fluid display entry points used by ModularUI2's GTNH recipe viewer integration. */
public final class GTUtility {

    private GTUtility() {
    }

    public static ItemStack getFluidDisplayStack(FluidStack fluid, boolean useStackSize) {
        return GT_Utility.getFluidDisplayStack(fluid, useStackSize);
    }

    public static ItemStack getFluidDisplayStack(Fluid fluid) {
        return GT_Utility.getFluidDisplayStack(fluid);
    }
}
