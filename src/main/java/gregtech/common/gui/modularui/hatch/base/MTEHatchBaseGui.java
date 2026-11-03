package gregtech.common.gui.modularui.hatch.base;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.gui.modularui.singleblock.base.MTEMachineWithFluidScreenBaseGui;

/** GTNH's shared hatch panel, using this branch's title and player inventory layout. */
public class MTEHatchBaseGui<T extends GT_MetaTileEntity_Hatch> extends MTEMachineWithFluidScreenBaseGui<T> {

    public MTEHatchBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected ModularPanel getBasePanel(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .setWidth(getBasePanelWidth())
            .setHeight(getBasePanelHeight())
            .doesAddGregTechLogo(false)
            .doesAddGhostCircuitSlot(false)
            .doesBindPlayerInventory(false)
            .build();
    }

    @Override
    protected boolean supportsTopRightCornerFlow() {
        return false;
    }
}
