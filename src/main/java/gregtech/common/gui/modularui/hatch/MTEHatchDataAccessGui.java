package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_DataAccess;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;

/** GTNH's tier-dependent data-stick inventory. */
public class MTEHatchDataAccessGui extends MTEHatchBaseGui<GT_MetaTileEntity_Hatch_DataAccess> {

    public MTEHatchDataAccessGui(GT_MetaTileEntity_Hatch_DataAccess machine) {
        super(machine);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager).child(
            new ItemSlotGridBuilder(machine.getInventoryHandler(), syncManager)
                .size(machine.mTier == 4 ? 2 : 4)
                .slotGroupKey("data_inv")
                .modularSlotSupplier((handler, index) -> createMachineSlot(index))
                .itemSlotSupplier(() -> new ItemSlot().backgroundOverlay(GTGuiTextures.OVERLAY_SLOT_CIRCUIT))
                .build()
                .center());
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return true;
    }
}
