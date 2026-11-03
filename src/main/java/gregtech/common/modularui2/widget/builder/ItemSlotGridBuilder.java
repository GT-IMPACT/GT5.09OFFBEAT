package gregtech.common.modularui2.widget.builder;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** GTNH inventory-grid builder with shared insertion, extraction and shift-click grouping rules. */
public class ItemSlotGridBuilder {

    private final IItemHandler itemHandler;
    private final PanelSyncManager syncManager;
    private int width;
    private int height;
    private int indexOffset;
    private String slotGroupKey = "item_inv";
    private boolean hasSlotGroup = true;
    private boolean canPut = true;
    private boolean canTake = true;
    private Predicate<ItemStack> filter = stack -> true;
    private Supplier<? extends ItemSlot> itemSlotSupplier = ItemSlot::new;
    private BiFunction<IItemHandler, Integer, ? extends ModularSlot> modularSlotSupplier = ModularSlot::new;

    public ItemSlotGridBuilder(IItemHandler itemHandler, PanelSyncManager syncManager) {
        this.itemHandler = Objects.requireNonNull(itemHandler);
        this.syncManager = Objects.requireNonNull(syncManager);
    }

    public ItemSlotGridBuilder size(int size) {
        return size(size, size);
    }

    public ItemSlotGridBuilder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ItemSlotGridBuilder slotGroupKey(String slotGroupKey) {
        this.slotGroupKey = Objects.requireNonNull(slotGroupKey);
        return this;
    }

    public ItemSlotGridBuilder hasSlotGroup(boolean hasSlotGroup) {
        this.hasSlotGroup = hasSlotGroup;
        return this;
    }

    public ItemSlotGridBuilder canPut(boolean canPut) {
        this.canPut = canPut;
        return this;
    }

    public ItemSlotGridBuilder canTake(boolean canTake) {
        this.canTake = canTake;
        return this;
    }

    public ItemSlotGridBuilder accessibility(boolean canPut, boolean canTake) {
        return canPut(canPut).canTake(canTake);
    }

    public ItemSlotGridBuilder indexOffset(int indexOffset) {
        this.indexOffset = indexOffset;
        return this;
    }

    public ItemSlotGridBuilder filter(Predicate<ItemStack> filter) {
        this.filter = Objects.requireNonNull(filter);
        return this;
    }

    public ItemSlotGridBuilder itemSlotSupplier(Supplier<? extends ItemSlot> supplier) {
        this.itemSlotSupplier = Objects.requireNonNull(supplier);
        return this;
    }

    public ItemSlotGridBuilder modularSlotSupplier(BiFunction<IItemHandler, Integer, ? extends ModularSlot> supplier) {
        this.modularSlotSupplier = Objects.requireNonNull(supplier);
        return this;
    }

    public Grid build() {
        if (width <= 0 || height <= 0 || indexOffset < 0
            || (long) width * height + indexOffset > itemHandler.getSlots()) {
            throw new IllegalStateException("Grid dimensions and offset must fit the backing inventory");
        }
        if (hasSlotGroup) {
            syncManager.registerSlotGroup(slotGroupKey, height);
        }
        return new Grid().coverChildren().gridOfWidthHeight(width, height, (x, y, index) -> {
            ModularSlot slot = modularSlotSupplier.apply(itemHandler, index + indexOffset)
                .accessibility(canPut, canTake)
                .filter(filter);
            if (hasSlotGroup) {
                slot.slotGroup(slotGroupKey);
            }
            return itemSlotSupplier.get().slot(slot);
        });
    }
}
