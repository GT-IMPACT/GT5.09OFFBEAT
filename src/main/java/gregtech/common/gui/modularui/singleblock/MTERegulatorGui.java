package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.gui.modularui.singleblock.base.MTEBufferBaseGui;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_Regulator;
import net.minecraft.item.ItemStack;

public class MTERegulatorGui extends MTEBufferBaseGui<GT_MetaTileEntity_Regulator> {

    public MTERegulatorGui(GT_MetaTileEntity_Regulator machine) {
        super(machine);
    }

    @Override
    protected boolean supportsEmitRedstone() {
        return false;
    }

    @Override
    protected boolean supportsInvertRedstone() {
        return false;
    }

    @Override
    protected boolean supportsStocking() {
        return false;
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomLeftCornerFlow(panel, syncManager)
            .child(new ItemSlot().slot(createMachineSlot(19).slotGroup("special"))
                .backgroundOverlay(GTGuiTextures.OVERLAY_SLOT_CHARGER).marginLeft(SLOT_SIZE))
            .child(GTGuiTextures.PICTURE_ARROW_22_RED.asWidget().size(86, 22).marginLeft(1));
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        IntSyncValue[] targetSlotSyncers = new IntSyncValue[9];
        for (int index = 0; index < targetSlotSyncers.length; index++) {
            targetSlotSyncers[index] = syncManager.findSyncHandler("targetSlot", index, IntSyncValue.class);
        }
        Flow mainRow = Flow.row().coverChildren();
        mainRow.child(new ItemSlotGridBuilder(machine.inventoryHandler, syncManager).size(3)
            .modularSlotSupplier(MachineModularSlot.supplier(baseMetaTileEntity))
            .build());
        mainRow.child(new Grid().coverChildren()
            .gridOfWidthHeight(3, 3, (x, y, index) -> createFilterSlot(index + 9))
            .background(GTGuiTextures.PICTURE_SLOTS_HOLO_3BY3));
        mainRow.child(new Grid().gridOfWidthHeight(3, 3, (x, y, index) -> new ButtonWidget<>()
            .onMousePressed(mouseButton -> {
                targetSlotSyncers[index].setIntValue(Math.min(99, Math.max(0,
                    targetSlotSyncers[index].getIntValue()
                        + (mouseButton == 0 ? -1 : 1) * (Interactable.hasShiftDown() ? 16 : 1))));
                return true;
            })
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .overlay(getTargetSlotText(targetSlotSyncers[index]))
            .hoverOverlay(getTargetSlotText(targetSlotSyncers[index]),
                new Rectangle().color(Color.withAlpha(Color.WHITE.main, 0x60)).asIcon().size(16)))
            .coverChildren()
            .background(GTGuiTextures.PICTURE_SLOTS_HOLO_3BY3));
        return super.createContentSection(panel, syncManager).child(mainRow);
    }

    private ItemSlot createFilterSlot(int index) {
        return new PhantomItemSlot().syncHandler(new PhantomItemSlotSH(createMachineSlot(index)) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null;
            }

            @Override
            protected void phantomClick(MouseData mouseData, ItemStack cursorStack) {
                if (cursorStack != null) {
                    getSlot().putStack(cursorStack.copy());
                } else if (getSlot().getStack() != null) {
                    ItemStack changed = getSlot().getStack().copy();
                    int change = mouseData.shift ? 8 : 1;
                    changed.stackSize += mouseData.mouseButton == 0 ? -change : change;
                    changed.stackSize = Math.min(changed.stackSize, changed.getMaxStackSize());
                    getSlot().putStack(changed.stackSize > 0 ? changed : null);
                }
            }

            @Override
            protected void phantomScroll(MouseData mouseData) {
            }
        }).disableThemeBackground(true).disableHoverThemeBackground(true);
    }

    private static IKey getTargetSlotText(IntSyncValue targetSlotSyncer) {
        return IKey.dynamic(() -> String.valueOf(targetSlotSyncer.getIntValue()));
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        for (int index = 0; index < 9; index++) {
            int slot = index;
            syncManager.syncValue("targetSlot", slot, new IntSyncValue(() -> machine.mTargetSlots[slot], value -> {
                machine.mTargetSlots[slot] = Math.min(99, Math.max(0, value));
                GTGuis.markDirty(machine);
            }).allowC2S());
        }
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + 4;
    }
}
