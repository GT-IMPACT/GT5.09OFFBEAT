package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.api.value.ISyncOrValue;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import gregtech.api.interfaces.metatileentity.IFluidLockableMui2;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

/** GTNH phantom fluid filter. It selects a fluid type without creating or consuming fluid. */
public class FluidLockSlotWidget extends GTFluidSlot implements IFluidTank {

    private final IFluidLockableMui2 machine;

    public FluidLockSlotWidget(IFluidLockableMui2 machine) {
        this.machine = machine;
        alwaysShowFull(true);
        syncHandler(this);
    }

    @Override
    public FluidSlot syncHandler(IFluidTank fluidTank) {
        return syncHandler(new FluidLockSyncHandler(fluidTank, machine));
    }

    @Override
    public FluidSlot syncHandler(FluidSlotSyncHandler syncHandler) {
        if (syncHandler == null || syncHandler.getFluidTank() != this) {
            throw new IllegalArgumentException("Fluid lock slots must use their own phantom tank");
        }
        setSyncOrValue(ISyncOrValue.orEmpty(syncHandler
            .phantom(true)
            .controlsAmount(false)
            .filter(stack -> stack == null || stack.getFluid() != null && machine.acceptsFluidLock(stack.getFluid()))));
        return this;
    }

    @Override
    protected void addToolTip(RichTooltip tooltip) {
        FluidStack fluid = getFluid();
        if (fluid != null) {
            tooltip.addFromFluid(fluid).spaceLine(2);
            tooltip.addLine(StatCollector.translateToLocal("GT5U.hatch.filter.tooltip.0"));
        } else {
            tooltip.addLine(StatCollector.translateToLocal("GT5U.hatch.filter.none"));
        }
    }

    @Override
    public FluidStack getFluid() {
        return machine.getLockedFluid() == null ? null : new FluidStack(machine.getLockedFluid(), 1);
    }

    @Override
    public int getFluidAmount() {
        return machine.getLockedFluid() == null ? 0 : 1;
    }

    @Override
    public int getCapacity() {
        return 1;
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(getFluid(), getCapacity());
    }

    @Override
    public int fill(FluidStack stack, boolean doFill) {
        if (stack == null || stack.amount <= 0 || stack.getFluid() == null
            || !machine.acceptsFluidLock(stack.getFluid())) {
            return 0;
        }
        if (doFill) {
            machine.setLockedFluid(stack.getFluid());
            machine.lockFluid(true);
        }
        return 1;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain > 0 && doDrain) {
            machine.setLockedFluid(null);
            machine.lockFluid(false);
        }
        return null;
    }

    /** Validate phantom value packets before the library clears the previous filter. */
    private static final class FluidLockSyncHandler extends FluidSlotSyncHandler {

        private final IFluidLockableMui2 machine;

        private FluidLockSyncHandler(IFluidTank tank, IFluidLockableMui2 machine) {
            super(tank);
            this.machine = machine;
        }

        @Override
        public void setValue(FluidStack value, boolean setSource, boolean sync) {
            if (value != null && (value.amount <= 0 || value.getFluid() == null
                || !machine.acceptsFluidLock(value.getFluid()))) {
                return;
            }
            super.setValue(value, setSource, sync);
        }
    }
}
