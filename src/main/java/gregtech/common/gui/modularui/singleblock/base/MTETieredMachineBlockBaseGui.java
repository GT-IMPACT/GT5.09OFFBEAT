package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_TieredMachineBlock;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonButtons;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.gui.modularui.util.MachineModularSlot;

/**
 * GTNH's reusable singleblock panel: content, corner controls and a separate inventory row.
 * Source: GTNewHorizons/GT5-Unofficial, f0472cfbd44ab9c8d5a27df7e98c64a42eb1c03f.
 */
public class MTETieredMachineBlockBaseGui<T extends GT_MetaTileEntity_TieredMachineBlock> {

    public static final int SLOT_SIZE = 18;
    private static final int BORDER = 4;

    protected final T machine;
    protected final IGregTechTileEntity baseMetaTileEntity;

    public MTETieredMachineBlockBaseGui(T machine) {
        this.machine = machine;
        baseMetaTileEntity = machine.getBaseMetaTileEntity();
    }

    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        registerSyncValues(syncManager);
        ModularPanel panel = getBasePanel(guiData, syncManager, uiSettings);
        Flow column = Flow.column()
            .fullWidth()
            .height(getBasePanelHeight())
            .top(CommonWidgets.MACHINE_HEADER_HEIGHT)
            .padding(BORDER)
            .child(createContentHolder(panel, syncManager));
        if (doesBindPlayerInventory()) {
            column.child(createInventoryRow(panel, syncManager));
        }
        panel.child(column);
        if (supportsTopRightCornerFlow()) {
            panel.child(createTopRightCornerFlow(panel, syncManager));
        }
        return panel;
    }

    protected void registerSyncValues(PanelSyncManager syncManager) {
        syncManager.registerSlotGroup("special", 1, false);
        if (!supportsPowerSwitch()) {
            return;
        }
        syncManager.syncValue("powerSwitch", new BooleanSyncValue(baseMetaTileEntity::isAllowedToWork, value -> {
            if (value) {
                baseMetaTileEntity.enableWorking();
            } else {
                baseMetaTileEntity.disableWorking();
            }
            GTGuis.markDirty(machine);
        }).allowC2S());
    }

    protected ModularPanel getBasePanel(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return GTGuis.mteTemplatePanelBuilder(machine, guiData, syncManager, uiSettings)
            .setWidth(getBasePanelWidth())
            .setHeight(getBasePanelHeight())
            .doesAddGregTechLogo(false)
            .doesBindPlayerInventory(false)
            .build();
    }

    protected int getBasePanelWidth() {
        return 176;
    }

    protected int getBasePanelHeight() {
        return 166;
    }

    protected boolean doesBindPlayerInventory() {
        return true;
    }

    protected int getContentHolderWidth() {
        return getBasePanelWidth() - 2 * BORDER;
    }

    protected int getContentHolderHeight() {
        return getBasePanelHeight() - 2 * BORDER - (doesBindPlayerInventory() ? 80 : 0);
    }

    protected int getContentPaddingHorizontal() {
        return 3;
    }

    protected int getContentPaddingVertical() {
        return 2;
    }

    protected boolean supportsBottomRowOverlap() {
        return false;
    }

    protected ParentWidget<?> createContentHolder(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> holder = supportsBottomRowOverlap() ? new ParentWidget<>() : Flow.column().childPadding(2);
        return holder.size(getContentHolderWidth(), getContentHolderHeight())
            .padding(getContentPaddingHorizontal(), getContentPaddingVertical())
            .child(createContentSection(panel, syncManager))
            .child(createBottomSection(panel, syncManager));
    }

    protected static ParentWidget<?> getEmptyContent() {
        return new ParentWidget<>().expanded().fullWidth();
    }

    protected static ParentWidget<?> getOverlappingEmptyContent() {
        return new ParentWidget<>().full();
    }

    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return supportsBottomRowOverlap() ? getOverlappingEmptyContent() : getEmptyContent();
    }

    protected ParentWidget<?> createBottomSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> section = new ParentWidget<>().fullWidth().height(SLOT_SIZE);
        if (supportsBottomLeftCornerFlow()) {
            section.child(createBottomLeftCornerFlow(panel, syncManager));
        }
        if (supportsBottomRightCornerFlow()) {
            section.child(createBottomRightCornerFlow(panel, syncManager));
        }
        if (supportsBottomRowOverlap()) {
            section.bottomRel(0);
        }
        return section;
    }

    protected boolean supportsBottomLeftCornerFlow() {
        return true;
    }

    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row().coverChildren().verticalCenter().leftRel(0);
    }

    protected boolean supportsBottomRightCornerFlow() {
        return true;
    }

    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        Flow row = Flow.row().mainAxisAlignment(Alignment.MainAxis.END)
            .reverseLayout(true).coverChildren().verticalCenter().rightRel(0);
        if (doesAddGregTechLogo()) {
            row.child(makeLogoWidget());
        }
        return row;
    }

    protected boolean doesAddGregTechLogo() {
        return true;
    }

    protected Widget<?> makeLogoWidget() {
        return new Widget<>().widgetTheme(GTWidgetThemes.PICTURE_LOGO).size(SLOT_SIZE);
    }

    protected IWidget createInventoryRow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row().fullWidth().height(76)
            .mainAxisAlignment(Alignment.MainAxis.CENTER)
            .child(SlotGroupWidget.playerInventory(false));
    }

    protected boolean supportsTopRightCornerFlow() {
        return true;
    }

    protected boolean supportsPowerSwitch() {
        return true;
    }

    protected Flow createTopRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        Flow column = Flow.column().coverChildren().top(BORDER).right(BORDER);
        if (supportsPowerSwitch()) {
            column.child(createPowerSwitchButton());
        }
        return column;
    }

    protected ToggleButton createPowerSwitchButton() {
        return CommonButtons.createSmallPowerSwitchButton("powerSwitch")
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true);
    }

    protected ModularSlot createMachineSlot(int index) {
        return new MachineModularSlot(machine.getInventoryHandler(), index, baseMetaTileEntity);
    }
}
