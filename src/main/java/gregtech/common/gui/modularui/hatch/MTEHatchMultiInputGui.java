package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.fluid.FluidStackTank;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import gregtech.api.metatileentity.implementations.GTMTE_Multi_Hatch_Input;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

public class MTEHatchMultiInputGui extends MTEHatchBaseGui<GTMTE_Multi_Hatch_Input> {

    public MTEHatchMultiInputGui(GTMTE_Multi_Hatch_Input hatch) {
        super(hatch);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        FluidStackTank[] fluidTanks = machine.getFluidTanks();
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(fluidTanks.length)));
        int rows = (fluidTanks.length + columns - 1) / columns;

        return super.createContentSection(panel, syncManager).child(
            new Grid().coverChildren()
                .gridOfWidthHeight(columns, rows, (x, y, index) -> {
                    if (index >= fluidTanks.length) {
                        return null;
                    }
                    return new FluidSlot().syncHandler(new FluidSlotSyncHandler(fluidTanks[index]));
                })
                .center());
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return true;
    }
}
