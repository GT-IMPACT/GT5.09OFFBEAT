package gregtech.api.interfaces.metatileentity;

import net.minecraftforge.fluids.Fluid;

/** GTNH contract for a phantom fluid filter, independent of the physical tank inventory. */
public interface IFluidLockableMui2 {

    void setLockedFluid(Fluid lockedFluid);

    Fluid getLockedFluid();

    void lockFluid(boolean lock);

    boolean isFluidLocked();

    boolean acceptsFluidLock(Fluid fluid);
}
