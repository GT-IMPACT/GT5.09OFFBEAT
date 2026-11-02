package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.singleblock.base.MTEFilterBaseGui;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_Filter;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Supplier;

public class MTEFilterGui extends MTEFilterBaseGui<GT_MetaTileEntity_Filter> {

    public MTEFilterGui(GT_MetaTileEntity_Filter machine) {
        super(machine);
    }

    @Override
    protected BooleanSyncValue createInvertFilterSyncValue() {
        return new BooleanSyncValue(() -> machine.bInvertFilter, value -> {
            machine.bInvertFilter = value;
            GTGuis.markDirty(machine);
        }).allowC2S();
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("ignoreNbt", new BooleanSyncValue(() -> machine.bIgnoreNBT, value -> {
            machine.bIgnoreNBT = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        Flow mainRow = Flow.row().childPadding(1).coverChildren();
        mainRow.child(new Rectangle().asWidget().size(8, 6));
        mainRow.child(new Grid().coverChildren()
            .gridOfWidthHeight(3, 3, (x, y, index) -> createFilterSlot(index + 9))
            .background(GTGuiTextures.PICTURE_SLOTS_HOLO_3BY3));
        mainRow.child(GTGuiTextures.PICTURE_ARROW_24_BLUE.asWidget().size(25, 24));
        mainRow.child(new ItemSlotGridBuilder(machine.inventoryHandler, syncManager).size(3)
            .modularSlotSupplier(MachineModularSlot.supplier(baseMetaTileEntity))
            .build());
        mainRow.child(GTGuiTextures.PICTURE_ARROW_24_RED.asWidget().size(17, 24));
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
                    getSlot().putStack(GT_Utility.copyAmount(1, cursorStack));
                } else if (mouseData.mouseButton == 0) {
                    getSlot().putStack(null);
                }
            }

            @Override
            protected void phantomScroll(MouseData mouseData) {
            }
        }).disableThemeBackground(true).disableHoverThemeBackground(true);
    }

    @Override
    protected boolean supportsStocking() {
        return false;
    }

    @Override
    protected List<Pair<Boolean, Supplier<IWidget>>> createButtonList(
        ModularPanel panel,
        PanelSyncManager syncManager
    ) {
        List<Pair<Boolean, Supplier<IWidget>>> buttons = super.createButtonList(panel, syncManager);
        buttons.add(Pair.of(true,
            () -> createButton(syncManager, "ignoreNbt", GTGuiTextures.OVERLAY_BUTTON_NBT)));
        return buttons;
    }
}
