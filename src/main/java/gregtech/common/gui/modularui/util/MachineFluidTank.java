package gregtech.common.gui.modularui.util;

import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import gregtech.api.modularui2.GTGuis;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

/** Live tank view: slot synchronization and container interaction share the machine's stored fluid. */
public final class MachineFluidTank implements IFluidTank {

    private final GT_MetaTileEntity_BasicTank machine;
    private final boolean output;

    public MachineFluidTank(GT_MetaTileEntity_BasicTank machine, boolean output) {
        this.machine = machine;
        this.output = output;
    }

    @Override
    public FluidStack getFluid() {
        return output ? machine.getDrainableStack() : machine.getFillableStack();
    }

    @Override
    public int getFluidAmount() {
        FluidStack fluid = getFluid();
        return fluid == null ? 0 : fluid.amount;
    }

    @Override
    public int getCapacity() {
        return machine.getCapacity();
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(getFluid(), getCapacity());
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        FluidStack stored = getFluid();
        if (stored != null && !stored.isFluidEqual(resource)) {
            return 0;
        }
        int accepted = Math.min(resource.amount, Math.max(0, getCapacity() - getFluidAmount()));
        if (doFill && accepted > 0) {
            FluidStack filled = stored == null ? resource.copy() : stored.copy();
            filled.amount = getFluidAmount() + accepted;
            setFluid(filled);
        }
        return accepted;
    }

    @Override
    public FluidStack drain(int maximum, boolean doDrain) {
        FluidStack stored = getFluid();
        if (stored == null || maximum <= 0 || stored.amount <= 0) {
            return null;
        }
        FluidStack drained = stored.copy();
        drained.amount = Math.min(stored.amount, maximum);
        if (doDrain) {
            FluidStack remaining = stored.copy();
            remaining.amount -= drained.amount;
            setFluid(remaining.amount == 0 ? null : remaining);
        }
        return drained;
    }

    private void setFluid(FluidStack fluid) {
        if (output) {
            machine.setDrainableStack(fluid);
        } else {
            machine.setFillableStack(fluid);
        }
        GTGuis.markDirty(machine);
    }
}
