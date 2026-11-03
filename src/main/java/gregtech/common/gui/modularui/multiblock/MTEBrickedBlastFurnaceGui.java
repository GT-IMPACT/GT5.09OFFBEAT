package gregtech.common.gui.modularui.multiblock;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.GTProgressWidget;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_PrimitiveBlastFurnace;

/** GTNH's six-slot primitive blast furnace window, shared by both local furnace variants. */
public class MTEBrickedBlastFurnaceGui {

    private final GT_MetaTileEntity_PrimitiveBlastFurnace machine;

    public MTEBrickedBlastFurnaceGui(GT_MetaTileEntity_PrimitiveBlastFurnace machine) {
        this.machine = machine;
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup("item_inv", 0);
        int header = CommonWidgets.MACHINE_HEADER_HEIGHT;
        return GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .build()
            .child(SlotGroupWidget.builder()
                .matrix("I", "I", "I")
                .key('I', index -> {
                    WidgetThemeKey<?> theme;
                    switch (index) {
                        case 0:
                            theme = GTWidgetThemes.OVERLAY_ITEM_SLOT_INGOT;
                            break;
                        case 1:
                            theme = GTWidgetThemes.OVERLAY_ITEM_SLOT_DUST;
                            break;
                        case 2:
                            theme = GTWidgetThemes.OVERLAY_ITEM_SLOT_FURNACE;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected input slot " + index);
                    }
                    return new ItemSlot().slot(createSlot(index))
                        .widgetTheme(theme);
                })
                .build()
                .pos(33, 15 + header))
            .child(SlotGroupWidget.builder()
                .matrix("III")
                .key('I', index -> {
                    WidgetThemeKey<?> theme;
                    switch (index) {
                        case 0:
                            theme = GTWidgetThemes.OVERLAY_ITEM_SLOT_INGOT;
                            break;
                        case 1:
                        case 2:
                            theme = GTWidgetThemes.OVERLAY_ITEM_SLOT_DUST;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected output slot " + index);
                    }
                    return new ItemSlot().slot(createSlot(index + 3).accessibility(false, true))
                        .widgetTheme(theme);
                })
                .build()
                .pos(85, 24 + header))
            .child(new GTProgressWidget().neiTransferRect(GT_Recipe_Map.sPrimitiveBlastRecipes)
                .name("primitive_progress")
                .value(new DoubleSyncValue(() -> machine.mMaxProgresstime > 0
                    ? (double) machine.mProgresstime / machine.mMaxProgresstime : 0))
                .texture(GTGuiTextures.PROGRESSBAR_ARROW_BBF, 20)
                .pos(58, 24 + header)
                .size(20, 18));
    }

    private ModularSlot createSlot(int index) {
        return new MachineModularSlot(machine.getInventoryHandler(), index, machine.getBaseMetaTileEntity())
            .slotGroup("item_inv");
    }
}
