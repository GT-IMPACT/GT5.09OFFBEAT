package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import gregtech.common.gui.modularui.util.MachineFluidTank;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import java.util.function.Predicate;

/** GTNH tank template bound to the machine's actual tank and container slots. */
public class MTEBasicTankBaseGui<T extends GT_MetaTileEntity_BasicTank>
    extends MTEMachineWithFluidScreenBaseGui<T> {

    public MTEBasicTankBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        IFluidTank tank = getFluidTank();
        Flow row = Flow.row().coverChildren().childPadding(1).verticalCenter();
        if (supportsFluidScreen()) {
            row.child(createScreen(panel, syncManager, tank));
        }
        if (supportsFluidIOColumn()) {
            row.child(createIO(
                panel,
                syncManager,
                machine.getInputSlot(),
                machine.getOutputSlot(),
                tank
            ));
        }
        return super.createContentSection(panel, syncManager).child(row);
    }

    protected IFluidTank getFluidTank() {
        return new MachineFluidTank(machine, false);
    }

    @Override
    protected boolean canFillFluidSlot() {
        return machine.canTankBeFilled();
    }

    @Override
    protected boolean canDrainFluidSlot() {
        return machine.canTankBeEmptied();
    }

    @Override
    protected Predicate<FluidStack> getFluidSlotFilter() {
        return machine::isFluidInputAllowed;
    }
}
