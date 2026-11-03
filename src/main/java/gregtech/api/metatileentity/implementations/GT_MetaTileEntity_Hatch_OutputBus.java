package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OutputBusType;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IDataCopyable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IItemLockable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputBusGui;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

public class GT_MetaTileEntity_Hatch_OutputBus extends GT_MetaTileEntity_Hatch
    implements IItemLockable, IDataCopyable {

    private static final String DATA_STICK_DATA_TYPE = "outputBusFilter";
    private static final String LOCKED_ITEM_NBT_KEY = "lockedItem";

    protected ItemStack lockedItem;

    public static int getSlots(int tier) {
        return (tier + 1) * (tier + 1);
    }

    public GT_MetaTileEntity_Hatch_OutputBus(int aID, String aName, String aNameRegional, int aTier) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            getSlots(aTier),
            new String[]{"Item Output for Multiblocks",
                "Capacity: " + getSlots(aTier) + " stack" + (getSlots(aTier) >= 2 ? "s" : "")}
        );
    }

    public GT_MetaTileEntity_Hatch_OutputBus(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, getSlots(aTier), aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_OutputBus(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, getSlots(aTier), aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_OutputBus(
        int aID,
        String aName,
        String aNameRegional,
        int aTier,
        int inventorySize,
        String[] aDescription
    ) {
        super(aID, aName, aNameRegional, aTier, inventorySize, aDescription);
    }

    public GT_MetaTileEntity_Hatch_OutputBus(
        String aName,
        int aTier,
        int inventorySize,
        String[] aDescription,
        ITexture[][][] aTextures
    ) {
        super(aName, aTier, inventorySize, aDescription, aTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT)};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT)};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return true;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_OutputBus(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player) {
        if (!acceptsItemLock() || !(player instanceof EntityPlayerMP)) {
            base.openGUI(player);
            return true;
        }
        ItemStack dataStick = player.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
            base.openGUI(player);
            return true;
        }
        if (!pasteCopiedData(player, dataStick.stackTagCompound)) {
            player.addChatMessage(new ChatComponentTranslation("GT5U.machines.output_bus.invalid"));
            return false;
        }
        player.addChatMessage(new ChatComponentTranslation("GT5U.machines.output_bus.loaded"));
        return true;
    }

    @Override
    public void onLeftclick(IGregTechTileEntity base, EntityPlayer player) {
        if (!acceptsItemLock() || !(player instanceof EntityPlayerMP)) {
            return;
        }
        ItemStack dataStick = player.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
            return;
        }
        dataStick.stackTagCompound = getCopiedData(player);
        setDataStickName(dataStick);
        player.addChatMessage(new ChatComponentTranslation("GT5U.machines.output_bus.saved"));
    }

    protected void setDataStickName(ItemStack dataStick) {
        dataStick.setStackDisplayName("Output Bus Configuration");
    }

    @Override
    public NBTTagCompound getCopiedData(EntityPlayer player) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("type", DATA_STICK_DATA_TYPE);
        if (lockedItem != null) {
            nbt.setTag(LOCKED_ITEM_NBT_KEY, lockedItem.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer player, @Nullable NBTTagCompound nbt) {
        if (nbt == null || !DATA_STICK_DATA_TYPE.equals(nbt.getString("type"))) {
            return false;
        }
        setLockedItem(nbt.hasKey(LOCKED_ITEM_NBT_KEY)
            ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(LOCKED_ITEM_NBT_KEY)) : null);
        return true;
    }

    @Override
    public String getCopiedDataIdentifier(EntityPlayer player) {
        return DATA_STICK_DATA_TYPE;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEHatchOutputBusGui(this).build(data, syncManager, settings);
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        if (lockedItem != null) {
            nbt.setTag(LOCKED_ITEM_NBT_KEY, lockedItem.writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        setLockedItem(nbt.hasKey(LOCKED_ITEM_NBT_KEY)
            ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(LOCKED_ITEM_NBT_KEY)) : null);
    }

    @Override
    public void setLockedItem(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            clearLock();
        } else {
            lockedItem = itemStack.copy();
            lockedItem.stackSize = 1;
            GTGuis.markDirty(this);
        }
    }

    @Nullable
    @Override
    public ItemStack getLockedItem() {
        return lockedItem;
    }

    @Override
    public void clearLock() {
        lockedItem = null;
        GTGuis.markDirty(this);
    }

    @Override
    public boolean isLocked() {
        return lockedItem != null;
    }

    @Override
    public boolean acceptsItemLock() {
        return true;
    }

    public boolean isFiltered() {
        return isLocked();
    }

    public OutputBusType getBusType() {
        return isFiltered() ? OutputBusType.StandardFiltered : OutputBusType.StandardUnfiltered;
    }

    /** GTNH's transactional output filter matches item, damage and NBT. */
    public boolean isFilteredToItem(ItemStack stack) {
        return lockedItem != null && stack != null && lockedItem.isItemEqual(stack)
            && ItemStack.areItemStackTagsEqual(lockedItem, stack);
    }

    /** Accepted items are removed from the supplied stack; only its remainder stays with the caller. */
    public boolean storeAll(ItemStack stack) {
        return storePartial(stack, false);
    }

    /** Simulation mutates only the caller's disposable remainder, leaving the bus inventory untouched. */
    public boolean storePartial(ItemStack stack, boolean simulate) {
        if (!simulate) {
            GTGuis.markDirty(this);
        }
        if (lockedItem != null && !lockedItem.isItemEqual(stack)) {
            return false;
        }
        for (int i = 0; i < mInventory.length && stack.stackSize > 0; i++) {
            ItemStack slot = mInventory[i];
            if (!GT_Utility.isStackInvalid(slot) && !GT_Utility.areStacksEqual(slot, stack)) {
                continue;
            }
            int inSlot = slot == null ? 0 : Math.max(0, slot.stackSize);
            int toInsert = Math.min(
                Math.max(0, Math.min(getInventoryStackLimit(), getStackSizeLimit(i, stack) - inSlot)),
                stack.stackSize
            );
            if (toInsert == 0) {
                continue;
            }
            if (!simulate) {
                if (GT_Utility.isStackInvalid(slot)) {
                    mInventory[i] = slot = stack.splitStack(0);
                }
                slot.stackSize += toInsert;
            }
            stack.stackSize -= toInsert;
        }
        return stack.stackSize == 0;
    }

    public int getStackSizeLimit(int slot, @Nullable ItemStack stack) {
        return Math.min(getInventoryStackLimit(), stack == null ? 64 : stack.getMaxStackSize());
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing();
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aBaseMetaTileEntity.isAllowedToWork() && (aTick&0x7)==0) {
            IInventory tTileEntity =aBaseMetaTileEntity.getIInventoryAtSide(aBaseMetaTileEntity.getFrontFacing());
            if(tTileEntity!=null){
                for (ItemStack aMInventory : mInventory)
                    GT_Utility.moveOneItemStack(aBaseMetaTileEntity, tTileEntity,
                            aBaseMetaTileEntity.getFrontFacing(), aBaseMetaTileEntity.getBackFacing(),
                            null, false, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
            }
        }
    }
}
