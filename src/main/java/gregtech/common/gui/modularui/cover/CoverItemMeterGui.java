package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverItemMeter;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import java.util.function.Consumer;

/** GTNH item detector layout, including threshold, slot selector and synchronized item preview. */
public class CoverItemMeterGui extends CoverBaseGui<CoverItemMeter> {

    public CoverItemMeterGui(CoverItemMeter cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.item_meter";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        column.child(positionRow(createRedstoneModeRow(syncManager)))
            .child(positionRow(createItemThresholdRow(syncManager)))
            .child(positionRow(createSlotRow(syncManager)));
    }

    private Flow createRedstoneModeRow(PanelSyncManager syncManager) {
        BooleanSyncValue inverted = new BooleanSyncValue(cover::isInverted, cover::setInverted).allowC2S();
        syncManager.syncValue("inverted", inverted);
        return Flow.row().marginBottom(4)
            .child(new ToggleButton().value(inverted)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                .tooltipDynamic(tooltip -> tooltip.addLine(getRedstoneModeTranslation())).marginRight(4))
            .child(IKey.dynamic(this::getRedstoneModeTranslation).asWidget());
    }

    private String getRedstoneModeTranslation() {
        return StatCollector.translateToLocal(
            cover.isInverted() ? "gt.interact.desc.inverted.tooltip" : "gt.interact.desc.normal.tooltip");
    }

    private Flow createItemThresholdRow(PanelSyncManager syncManager) {
        IntSyncValue threshold = new IntSyncValue(cover::getThreshold, cover::setThreshold).allowC2S();
        syncManager.syncValue("threshold", threshold);
        return Flow.row().marginBottom(4)
            .child(makeNumberField(50).value(threshold).defaultNumber(0)
                .numbersInt(0, Integer.MAX_VALUE).marginRight(2))
            .child(IKey.lang("gt.interact.desc.itemthreshold").asWidget());
    }

    private Flow createSlotRow(PanelSyncManager syncManager) {
        IItemHandler inventory = getInventoryHandler();
        IntSyncValue slot = new IntSyncValue(cover::getSlot, cover::setSlot).allowC2S();
        GenericSyncValue<ItemStack, ?> display = GenericSyncValue.forItem(
            () -> queryMTEItem(inventory, slot.getIntValue()), null);
        syncManager.syncValue("slot", slot);
        syncManager.syncValue("slot_preview", display);
        return Flow.row()
            .child(makeNumberField(50).value(slot).defaultNumber(-1)
                .numbersInt(-1, Math.max(-1, cover.getTile().getSizeInventory() - 1)).marginRight(2))
            .child(createItemDisplayWidget(slot, display))
            .child(IKey.lang("gt.interact.desc.item_slot").asWidget().paddingRight(TICK_RATE_BUTTON_SIZE));
    }

    private IItemHandler getInventoryHandler() {
        ICoverable tile = cover.getTile();
        if (tile.isDead() || !(tile instanceof IGregTechTileEntity) || tile.getSizeInventory() <= 0) {
            return null;
        }
        IGregTechTileEntity gregTechTile = (IGregTechTileEntity) tile;
        return gregTechTile.getMetaTileEntity() == null ? null : new InvWrapper(gregTechTile.getMetaTileEntity());
    }

    private Consumer<RichTooltip> getItemDisplayTooltip(IntSyncValue slot) {
        return tooltip -> {
            if (slot.getIntValue() == -1) {
                tooltip.addLine(StatCollector.translateToLocal("gt.interact.desc.any_slot"));
            }
        };
    }

    private IWidget createItemDisplayWidget(IntSyncValue slot, GenericSyncValue<ItemStack, ?> display) {
        return new ItemDisplayWidget().item(display).displayAmount(false).tooltipDynamic(getItemDisplayTooltip(slot));
    }

    private ItemStack queryMTEItem(IItemHandler inventory, int slot) {
        if (inventory != null && slot >= 0 && slot < inventory.getSlots() && inventory.getStackInSlot(slot) != null) {
            return inventory.getStackInSlot(slot);
        }
        return ItemList.Display_ITS_FREE.get(1);
    }
}
