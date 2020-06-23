package gregtech.api.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GT_Slot_Holo extends Slot {
    public final int mSlotIndex;
    public boolean mCanInsertItem, mCanStackItem;
    public int mMaxStacksize = 127;

    public GT_Slot_Holo(IInventory par1iInventory, int slotIndex, int xDisplayPosition, int yDisplayPosition, boolean aCanInsertItem, boolean aCanStackItem, int aMaxStacksize) {
        super(par1iInventory, slotIndex, xDisplayPosition, yDisplayPosition);
        mCanInsertItem = aCanInsertItem;
        mCanStackItem = aCanStackItem;
        mMaxStacksize = aMaxStacksize;
        mSlotIndex = slotIndex;
    }

    @Override
    public boolean isItemValid(ItemStack par1ItemStack) {
        return mCanInsertItem;
    }

    @Override
    public int getSlotStackLimit() {
        return mMaxStacksize;
    }

    @Override
    public boolean getHasStack() {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int par1) {
        if (!mCanStackItem) return null;
        return super.decrStackSize(par1);
    }

    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        return false;
    }
}
