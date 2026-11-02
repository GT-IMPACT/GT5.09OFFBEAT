package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_DigitalChestBase;
import net.minecraft.item.ItemStack;

/** The existing digital chest window, preserving its layout and render-only third slot. */
public class MTEDigitalChestGui {

    private static final UITexture BACKGROUND = UITexture.builder()
        .location("gregtech", "gui/BasicTank")
        .imageSize(256, 256)
        .subAreaXYWH(0, 4, 176, 162)
        .canApplyTheme()
        .build();

    private final GT_MetaTileEntity_DigitalChestBase machine;

    public MTEDigitalChestGui(GT_MetaTileEntity_DigitalChestBase machine) {
        this.machine = machine;
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup("input", 1);
        syncManager.registerSlotGroup("output", 1, false);
        syncManager.registerSlotGroup("display", 1, false);
        IntSyncValue count = new IntSyncValue(machine::getItemCount);
        syncManager.syncValue("itemCount", count);
        int header = CommonWidgets.MACHINE_HEADER_HEIGHT;
        return GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .doesBindPlayerInventory(false)
            .doesAddGregTechLogo(false)
            .build()
            .child(BACKGROUND.asWidget().pos(0, header + 4).size(176, 162))
            .child(new ItemSlot().slot(createSlot(0).slotGroup("input"))
                .disableThemeBackground(true).pos(79, 16 + header))
            .child(new ItemSlot().slot(createSlot(1).canPut(false).slotGroup("output"))
                .disableThemeBackground(true).pos(79, 52 + header))
            .child(new PhantomItemSlot().slot(createDisplaySlot().slotGroup("display"))
                .disableThemeBackground(true).pos(58, 41 + header))
            .child(IKey.str("Item Amount").asWidget()
                .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE).pos(10, 20 + header))
            .child(IKey.dynamic(() -> GT_Utility.parseNumberToString(count.getIntValue())).asWidget()
                .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE).pos(10, 30 + header))
            .child(IKey.lang("container.inventory").asWidget().pos(8, 72 + header))
            .child(SlotGroupWidget.playerInventory((index, slot) -> slot.disableThemeBackground(true).size(18))
                .pos(7, 83 + header));
    }

    private ModularSlot createSlot(int index) {
        return new MachineModularSlot(machine.getInventoryHandler(), index, machine.getBaseMetaTileEntity());
    }

    private ModularSlot createDisplaySlot() {
        return new MachineModularSlot(machine.getInventoryHandler(), 2, machine.getBaseMetaTileEntity()) {

            @Override
            public void putStack(ItemStack stack) {
                // Match GT_Slot_Render: client synchronization may update the picture, player actions may not.
                if (machine.getBaseMetaTileEntity().isClientSide()) {
                    super.putStack(stack);
                }
            }
        }.accessibility(false, false).canDragInto(false);
    }
}
