package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Maintenance;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** GTNH's manual repair target and automatic maintenance inventory. */
public class MTEHatchMaintenanceGui extends MTEHatchBaseGui<GT_MetaTileEntity_Hatch_Maintenance> {

    private final boolean auto;

    public MTEHatchMaintenanceGui(GT_MetaTileEntity_Hatch_Maintenance machine, boolean auto) {
        super(machine);
        this.auto = auto;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> parent = super.createContentSection(panel, syncManager);
        parent.child(GuiTextures.BUBBLE.asWidget()
            .name("maintenance_help")
            .background(GTGuiTextures.BUTTON_STANDARD)
            .topRel(0)
            .rightRel(0)
            .addTooltipStringLines(Arrays.asList(StatCollector.translateToLocal(
                auto ? "GT5U.gui.text.autorepair_info" : "GT5U.gui.text.repair_info"
            ).split("\\\\n"))));
        parent.childIf(auto, () -> new ItemSlotGridBuilder(machine.getInventoryHandler(), syncManager)
            .size(2)
            .modularSlotSupplier((handler, index) -> createMachineSlot(index))
            .build()
            .center());
        parent.childIf(!auto, () -> GTGuiTextures.SLOT_MAINTENANCE.asWidget()
            .name("maintenance_background")
            .size(20)
            .center());
        parent.childIf(!auto, () -> createSimpleMaintenanceSlot(syncManager));
        return parent;
    }

    private ItemSlot createSimpleMaintenanceSlot(PanelSyncManager syncManager) {
        return new PhantomItemSlot() {

            @Override
            public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
                return false;
            }

            @Override
            public PhantomItemSlot slot(ModularSlot slot) {
                return syncHandler(new PhantomItemSlotSH(slot) {

                    @Override
                    protected void phantomClick(MouseData mouseData, ItemStack cursorStack) {
                        if (cursorStack == null) {
                            return;
                        }
                        EntityPlayer player = syncManager.getPlayer();
                        if (player == null) {
                            return;
                        }
                        machine.onToolClick(cursorStack, player);
                        if (cursorStack.stackSize < 1) {
                            syncManager.setCursorItem(null);
                        } else {
                            syncManager.setCursorItem(cursorStack);
                        }
                        GTGuis.markDirty(machine);
                    }
                });
            }
        }.slot(new ModularSlot(machine.getInventoryHandler(), 0))
            .name("maintenance_slot")
            .size(25)
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .center();
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return true;
    }
}
