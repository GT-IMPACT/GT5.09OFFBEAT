package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.gui.modularui.util.FilterSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;

/** GTNH output-bus grid, item filter and tier-dependent panel dimensions. */
public class MTEHatchOutputBusGui extends MTEHatchBaseGui<GT_MetaTileEntity_Hatch_OutputBus> {

    public MTEHatchOutputBusGui(GT_MetaTileEntity_Hatch_OutputBus hatch) {
        super(hatch);
    }

    protected int getDimension() {
        return Math.max(1, machine.mTier + 1);
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomLeftCornerFlow(panel, syncManager)
            .child(new FilterSlot(machine::getLockedItem, machine::setLockedItem).name("output_filter"));
    }

    @Override
    protected int getBasePanelHeight() {
        int dimension = getDimension();
        return super.getBasePanelHeight() + (dimension > 4 ? (dimension - 3) * SLOT_SIZE : 0);
    }

    @Override
    protected int getBasePanelWidth() {
        return super.getBasePanelWidth() + Math.max(0, SLOT_SIZE * (getDimension() - 9));
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager).child(
            new ItemSlotGridBuilder(machine.getInventoryHandler(), syncManager).size(getDimension())
                .build()
                .center());
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return getDimension() <= 4;
    }
}
