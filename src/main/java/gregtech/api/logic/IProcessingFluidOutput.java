package gregtech.api.logic;

import net.minecraftforge.fluids.FluidStack;

/** Adapter for local tanks that are not standard GT output hatches. */
public interface IProcessingFluidOutput {

    FluidStack getStoredFluid();

    int getCapacity();

    boolean accepts(FluidStack fluid);

    boolean isRestricted();

    int fill(FluidStack fluid, boolean execute);
}
