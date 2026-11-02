package gregtech.common.gui.modularui.util;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.modularui2.GTGuis;
import net.minecraft.item.ItemStack;

/** MUI2 inventory view over the actual machine inventory, preserving its validation and dirty notifications. */
public class MachineItemHandler extends ItemStackHandler {

    private final IMetaTileEntity owner;

    public MachineItemHandler(ItemStack[] inventory, IMetaTileEntity owner) {
        super(inventory);
        this.owner = owner;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        return isCircuitSlot(slot) ? 0 : owner.getInventoryStackLimit();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        return stack == null || !isCircuitSlot(slot) && owner.isItemValidForSlot(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (stack != null && (stack.stackSize <= 0 || !isItemValid(slot, stack))) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (amount <= 0 || isCircuitSlot(slot)) {
            return null;
        }
        ItemStack extracted = super.extractItem(slot, amount, simulate);
        return simulate && extracted != null ? extracted.copy() : extracted;
    }

    private boolean isCircuitSlot(int slot) {
        return owner instanceof IConfigurationCircuitSupport
            && slot == ((IConfigurationCircuitSupport) owner).getCircuitSlot();
    }

    @Override
    public void setSize(int size) {
        if (size != getSlots()) {
            throw new IllegalArgumentException("Machine inventory size cannot change: " + size);
        }
        // ItemStackHandler normally replaces its backing array, detaching it from the machine inventory.
        for (int slot = 0; slot < size; slot++) {
            stacks.set(slot, null);
            onContentsChanged(slot);
        }
    }

    @Override
    protected void onLoad() {
        for (int slot = 0; slot < getSlots(); slot++) {
            onContentsChanged(slot);
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        IGregTechTileEntity base = owner.getBaseMetaTileEntity();
        if (base != null && base.isServerSide()) {
            // The shared array alone does not update BaseMetaTileEntity's machine wake-up flag.
            base.setInventorySlotContents(slot, getStackInSlot(slot));
            GTGuis.markDirty(owner);
        }
    }
}
