package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.ByteSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.gui.modularui.singleblock.base.MTEBufferBaseGui;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_ItemDistributor;
import net.minecraftforge.common.util.ForgeDirection;

public class MTEItemDistributorGui extends MTEBufferBaseGui<GT_MetaTileEntity_ItemDistributor> {

    public MTEItemDistributorGui(GT_MetaTileEntity_ItemDistributor machine) {
        super(machine);
    }

    @Override
    protected boolean supportsStocking() {
        return false;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager).child(
            new ItemSlotGridBuilder(machine.inventoryHandler, syncManager).size(9, 3)
                .modularSlotSupplier(MachineModularSlot.supplier(baseMetaTileEntity))
                .build());
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        IPanelHandler sideConfigPanel = syncManager.syncedPanel("sideConfigPanel", true,
            (panelSyncManager, syncHandler) -> openSideConfigPanel(panel));
        return super.createBottomLeftCornerFlow(panel, syncManager)
            .child(new ButtonWidget<>().onMousePressed(mouseButton -> {
                if (!sideConfigPanel.isPanelOpen()) {
                    sideConfigPanel.openPanel();
                } else {
                    sideConfigPanel.closePanel();
                }
                return true;
            })
                .overlay(GuiTextures.GEAR)
                .addTooltipLine(IKey.lang("GT5U.machines.item_distributor.open_config_panel.tooltip"))
                .tooltipShowUpTimer(20))
            .child(GTGuiTextures.PICTURE_ARROW_22_RED.asWidget().size(68, 22).marginLeft(1));
    }

    private ModularPanel openSideConfigPanel(ModularPanel parent) {
        ModularPanel panel = GTGuis.createPopUpPanel("sideConfigPanel")
            .relative(parent)
            .coverChildren()
            .padding(5)
            .topRel(0)
            .leftRel(1);
        Flow textColumn = Flow.column().coverChildren().childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.START);
        textColumn.child(IKey.lang("GT5U.machines.item_distributor.config_panel").asWidget().marginBottom(10));
        for (int index = 0; index < 6; index++) {
            textColumn.child(createConfigRow(index));
        }
        return panel.child(textColumn);
    }

    private Flow createConfigRow(int index) {
        ByteSyncValue itemsPerSideSyncer = new ByteSyncValue(
            () -> machine.getItemsPerSide(index), value -> machine.setItemsPerSide(index, value)).allowC2S();
        Flow configRow = Flow.row().coverChildren().childPadding(3);
        configRow.child(IKey.str(ForgeDirection.getOrientation(index).toString()).asWidget().width(30));
        configRow.child(new TextFieldWidget().width(35).value(itemsPerSideSyncer)
            .numbersInt(0, 127).setMaxLength(3));
        configRow.childIf(index == baseMetaTileEntity.getFrontFacing(), () -> IKey.str("(Front)").asWidget());
        configRow.childIf(index == baseMetaTileEntity.getBackFacing(), () -> IKey.str("(Back)").asWidget());
        return configRow;
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + 4;
    }
}
