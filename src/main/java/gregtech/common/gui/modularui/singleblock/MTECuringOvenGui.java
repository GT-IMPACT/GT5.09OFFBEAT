package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.gui.modularui.util.MachineItemHandler;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_CuringOven;
import net.minecraft.item.ItemStack;

/** The existing 1by1 curing oven window, using MUI2's shared factory and live slot adapter. */
public class MTECuringOvenGui {

    private static final UITexture BACKGROUND = UITexture.builder()
        .location("gregtech", "gui/1by1")
        .imageSize(256, 256)
        .subAreaXYWH(0, 4, 176, 162)
        .canApplyTheme()
        .build();

    private final GT_MetaTileEntity_CuringOven machine;
    private final MachineItemHandler inventory;

    public MTECuringOvenGui(GT_MetaTileEntity_CuringOven machine) {
        this.machine = machine;
        inventory = new MachineItemHandler(machine.mInventory, machine) {

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // The old plain Slot accepts tools at index 0; the machine excludes it from automation.
                return slot == 0 || super.isItemValid(slot, stack);
            }
        };
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup("item_inv", 1);
        int header = CommonWidgets.MACHINE_HEADER_HEIGHT;
        return GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .setWidth(176)
            .setHeight(166)
            .doesBindPlayerInventory(false)
            .doesAddGregTechLogo(false)
            .doesAddGhostCircuitSlot(false)
            .build()
            .child(BACKGROUND.asWidget().pos(0, header + 4).size(176, 162))
            .child(new ItemSlot()
                .slot(new MachineModularSlot(inventory, 0, machine.getBaseMetaTileEntity())
                    .slotGroup("item_inv"))
                .disableThemeBackground(true)
                .size(18)
                .pos(79, 34 + header))
            .child(SlotGroupWidget.playerInventory((index, slot) -> slot.disableThemeBackground(true).size(18))
                .pos(7, 83 + header));
    }
}
