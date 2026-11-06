package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Utility;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Each recipe output keeps its corresponding slot in the controller inventory.
 */
public final class InventoryProcessingOutputs implements IProcessingOutputSink {
    private final IInventory inventory;
    private final int firstSlot;
    private final int slotCount;
    private boolean alwaysProtectItems;

    public InventoryProcessingOutputs(IInventory inventory, int firstSlot, int slotCount) {
        if (firstSlot < 0 || slotCount < 0 || firstSlot > inventory.getSizeInventory() - slotCount) {
            throw new IllegalArgumentException("Output slots are outside the inventory");
        }
        this.inventory = inventory;
        this.firstSlot = firstSlot;
        this.slotCount = slotCount;
    }

    public InventoryProcessingOutputs setAlwaysProtectItems(boolean enabled) {
        alwaysProtectItems = enabled;
        return this;
    }

    @Override
    public CheckRecipeResult canFit(ItemStack[] items, FluidStack[] fluids, boolean protectItems,
            boolean protectFluids) {
        if (protectItems || alwaysProtectItems) {
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item != null && item.stackSize > availableSpace(i, item)) {
                    return CheckRecipeResult.ITEM_OUTPUT_FULL;
                }
            }
        }
        if (protectFluids) {
            for (FluidStack fluid : fluids) {
                if (fluid != null && fluid.amount > 0) {
                    return CheckRecipeResult.FLUID_OUTPUT_FULL;
                }
            }
        }
        return CheckRecipeResult.SUCCESS;
    }

    @Override
    public boolean areFluidOutputsEmpty() {
        return true;
    }

    /** Retains every unaccepted item in the pending array for a later attempt. */
    public boolean deliver(ItemStack[] pending) {
        boolean complete = true;
        if (pending == null) {
            return true;
        }
        for (int i = 0; i < pending.length; i++) {
            ItemStack item = pending[i];
            if (item == null || item.stackSize <= 0) {
                continue;
            }
            int accepted = Math.min(item.stackSize, availableSpace(i, item));
            if (accepted > 0) {
                ItemStack stored = inventory.getStackInSlot(firstSlot + i);
                ItemStack updated = stored == null ? item.copy() : stored.copy();
                updated.stackSize = (stored == null ? 0 : stored.stackSize) + accepted;
                inventory.setInventorySlotContents(firstSlot + i, updated);
                item.stackSize -= accepted;
            }
            complete &= item.stackSize == 0;
        }
        return complete;
    }

    @Override
    public boolean deliver(ItemStack[] items, FluidStack[] fluids, boolean protectItems, boolean protectFluids) {
        boolean complete = deliver(items);
        if (fluids != null) {
            for (FluidStack fluid : fluids) {
                if (fluid != null && fluid.amount > 0) {
                    if (protectFluids) {
                        complete = false;
                    } else {
                        fluid.amount = 0;
                    }
                }
            }
        }
        return complete;
    }

    private int availableSpace(int outputIndex, ItemStack item) {
        if (outputIndex >= slotCount) {
            return 0;
        }
        ItemStack stored = inventory.getStackInSlot(firstSlot + outputIndex);
        if (stored != null && !GT_Utility.areStacksEqual(stored, item)) {
            return 0;
        }
        int limit = Math.min(item.getMaxStackSize(), inventory.getInventoryStackLimit());
        return Math.max(0, limit - (stored == null ? 0 : stored.stackSize));
    }
}
