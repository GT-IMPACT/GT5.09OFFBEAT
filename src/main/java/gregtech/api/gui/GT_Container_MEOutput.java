package gregtech.api.gui;

import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MEOutputStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GT_Container_MEOutput extends GT_ContainerMetaTile_Machine {
    public boolean cacheMode;
    public int refreshSeconds = 2;
    public long buffered, capacity;

    public GT_Container_MEOutput(InventoryPlayer player, IGregTechTileEntity tile) {
        super(player, tile);
    }

    private IMEOutput output() { return (IMEOutput) mTileEntity.getMetaTileEntity(); }

    @Override
    public void addSlots(InventoryPlayer player) {
        addSlotToContainer(new Slot(mTileEntity, 0, 80, 35) {
            @Override public int getSlotStackLimit() { return 1; }
            @Override public boolean isItemValid(ItemStack stack) {
                return output().getMEOutputStorage().isValidCell(stack);
            }
            @Override public void onSlotChanged() {
                super.onSlotChanged();
                if (mTileEntity.isServerSide()) output().getMEOutputStorage().updateCell();
            }
        });
        // WindowItems also writes empty control slots on the client. They must not alias the cell inventory.
        IInventory controls = new InventoryBasic("ME output controls", false, 3);
        addControlSlot(controls, 0, 8);
        addControlSlot(controls, 1, 110);
        addControlSlot(controls, 2, 138);
    }

    private void addControlSlot(IInventory controls, int index, int x) {
        addSlotToContainer(new GT_Slot_Holo(controls, index, x, 35, false, false, 1));
    }

    @Override public int getSlotCount() { return 1; }
    @Override public int getShiftClickSlotCount() { return 1; }

    @Override
    public ItemStack slotClick(int slot, int mouseButton, int clickMode, EntityPlayer player) {
        if (slot < 1 || slot > 3) return super.slotClick(slot, mouseButton, clickMode, player);
        if (!canInteractWith(player) || mTileEntity.isClientSide()
                || (mouseButton != 0 && mouseButton != 1) || (clickMode != 0 && clickMode != 1)) return null;
        GT_MEOutputStorage<?> storage = output().getMEOutputStorage();
        int step = clickMode == 1 ? 10 : 1;
        switch (slot) {
            case 1: storage.setCacheMode(!storage.isCacheMode()); break;
            case 2: storage.setRefreshSeconds(storage.getRefreshSeconds() - step); break;
            case 3: storage.setRefreshSeconds(storage.getRefreshSeconds() + step); break;
        }
        detectAndSendChanges();
        return null;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (mTileEntity.isClientSide() || !(mTileEntity.getMetaTileEntity() instanceof IMEOutput)) return;
        GT_MEOutputStorage<?> storage = output().getMEOutputStorage();
        cacheMode = storage.isCacheMode();
        refreshSeconds = storage.getRefreshSeconds();
        buffered = output().getMEOutputAmount();
        capacity = storage.getCapacity();
        for (Object listener : crafters) {
            ICrafting crafter = (ICrafting) listener;
            crafter.sendProgressBarUpdate(this, 100, cacheMode ? 1 : 0);
            crafter.sendProgressBarUpdate(this, 101, refreshSeconds & 65535);
            crafter.sendProgressBarUpdate(this, 102, refreshSeconds >>> 16);
            for (int part = 0; part < 4; part++) {
                crafter.sendProgressBarUpdate(this, 103 + part, (int) (buffered >>> (part * 16)) & 65535);
                crafter.sendProgressBarUpdate(this, 107 + part, (int) (capacity >>> (part * 16)) & 65535);
            }
        }
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 100) cacheMode = value != 0;
        else if (id == 101) refreshSeconds = (refreshSeconds & 0xffff0000) | (value & 65535);
        else if (id == 102) refreshSeconds = (refreshSeconds & 65535) | (value << 16);
        else if (id >= 103 && id < 111) {
            int shift = ((id - 103) % 4) * 16;
            long mask = 65535L << shift;
            if (id < 107) buffered = (buffered & ~mask) | ((value & 65535L) << shift);
            else capacity = (capacity & ~mask) | ((value & 65535L) << shift);
        } else super.updateProgressBar(id, value);
    }
}
