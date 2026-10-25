package gregtech.api.metatileentity.implementations;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.implementations.IPowerChannelState;
import gregtech.api.GregTech_API;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import cpw.mods.fml.common.Optional;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.gui.GT_Container_MEOutput;
import gregtech.api.gui.GT_GUIContainer_MEOutput;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;


public class GT_MetaTileEntity_Hatch_OutputBus_ME extends GT_MetaTileEntity_Hatch_OutputBus implements IMEOutput, IPowerChannelState {
    private BaseActionSource requestSource = null;
    private AENetworkProxy gridProxy = null;
    private final List<ItemStack> cachedItems = new ArrayList<ItemStack>();
    private final GT_MEOutputStorage<IAEItemStack> storage = new GT_MEOutputStorage<IAEItemStack>(
            StorageChannel.ITEMS, 1600, () -> mInventory[0], this::getProxy, this::getRequest,
            this::markStorageDirty, this::returnToCache);

    @Override
    public GT_MEOutputStorage<IAEItemStack> getMEOutputStorage() { return storage; }

    @Override
    public long getMEOutputAmount() {
        long amount = 0;
        for (ItemStack cached : cachedItems) amount += cached.stackSize;
        return amount;
    }

    public GT_MetaTileEntity_Hatch_OutputBus_ME(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 1, 1, new String[]{
                "Item Output for Multiblocks", "Stores directly into ME",
                "Default buffer: 1,600 items; insert a storage cell to expand",
                "Cache Mode stores output in the inserted cell", "Requires an ME channel and power",
                "Connect ME cable to the front", "Screwdriver toggles Cache Mode"});
    }

    public GT_MetaTileEntity_Hatch_OutputBus_ME(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 1, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_OutputBus_ME(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_ME_HATCH)};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_ME_HATCH)};
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (GregTech_API.mAE2) readyProxy();
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void readyProxy() {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.onReady();
    }

    @Override
    public boolean storeAll(ItemStack aStack) {
        if (!GregTech_API.mAE2 || aStack == null || aStack.stackSize <= 0) return false;
        if (!storage.canAccept(getMEOutputAmount()) || !canStore(aStack)) return false;
        storage.accepted();
        for (ItemStack cached : cachedItems) {
            if (GT_Utility.areStacksEqual(cached, aStack)
                    && cached.stackSize <= Integer.MAX_VALUE - aStack.stackSize) {
                cached.stackSize += aStack.stackSize;
                aStack.stackSize = 0;
                markStorageDirty();
                return true;
            }
        }
        cachedItems.add(aStack.copy());
        aStack.stackSize = 0;
        markStorageDirty();
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        if (base.isServerSide() && GregTech_API.mAE2) {
            storage.tick(tick);
            if (tick % 20 == 0) base.setActive(getProxy() != null && getProxy().isActive());
            if (storage.shouldFlush(cachedItems.isEmpty())) flushCache(base);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void flushCache(IGregTechTileEntity base) {
        AENetworkProxy proxy = getProxy();
        base.setActive(proxy != null && proxy.isActive());
        if (proxy == null || !proxy.isActive() || !base.isAllowedToWork()) return;
        storage.flushed();
        Iterator<ItemStack> iterator = cachedItems.iterator();
        while (iterator.hasNext()) {
            ItemStack cached = iterator.next();
            int remaining = store(cached);
            if (remaining != cached.stackSize) {
                cached.stackSize = remaining;
                if (remaining == 0) iterator.remove();
                markStorageDirty();
            }
        }
    }

    /**
     * Attempt to store items in connected ME network. Returns how many items did not fit (if the network was down e.g.)
     *
     * @param stack  input stack
     * @return amount of items left over
     */
    @Optional.Method(modid = "appliedenergistics2")
    public int store(final ItemStack stack) {
        if (stack == null)
            return 0;
        try {
            AENetworkProxy proxy = getProxy();
            if (proxy == null || !proxy.isActive() || !getBaseMetaTileEntity().isAllowedToWork())
                return stack.stackSize;
            IMEMonitor<IAEItemStack> sg = proxy.getStorage().getItemInventory();
            IAEItemStack toStore = AEApi.instance().storage().createItemStack(stack);
            IAEItemStack rest = storage.insert(toStore, sg);
            if (rest != null)
                return (int)rest.getStackSize();
            return 0;
        }
        catch( final GridAccessException ignored )
        {
        }
        return stack.stackSize;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private BaseActionSource getRequest() {
        if (requestSource == null)
            requestSource = new MachineSource((IActionHost)getBaseMetaTileEntity());
        return requestSource;
    }

    @Optional.Method(modid = "appliedenergistics2")
    protected boolean canStore(ItemStack stack) {
        if (mInventory[0] == null) return true;
        return storage.canStore(AEApi.instance().storage().createItemStack(stack));
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void returnToCache(IAEItemStack stack) {
        long amount = stack.getStackSize();
        while (amount > 0) {
            ItemStack item = stack.getItemStack();
            item.stackSize = (int) Math.min(Integer.MAX_VALUE, amount);
            cachedItems.add(item);
            amount -= item.stackSize;
        }
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return isOutputFacing((byte)forgeDirection.ordinal()) ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isServerSide()) aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    public Object getServerGUI(int id, InventoryPlayer player, IGregTechTileEntity base) {
        return new GT_Container_MEOutput(player, base);
    }

    @Override
    public Object getClientGUI(int id, InventoryPlayer player, IGregTechTileEntity base) {
        return new GT_GUIContainer_MEOutput(player, base, "ME Item Output");
    }

    @Override
    public boolean isValidSlot(int index) { return index == 0; }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return GregTech_API.mAE2 && index == 0 && storage.isValidCell(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) { return false; }

    @Override
    public void onScrewdriverRightClick(byte side, EntityPlayer player, float x, float y, float z) {
        if (getBaseMetaTileEntity().isServerSide() && GregTech_API.mAE2) {
            storage.setCacheMode(!storage.isCacheMode());
            GT_Utility.sendChatToPlayer(player, "Cache Mode: " + (storage.isCacheMode() ? "On" : "Off"));
        }
    }

    @Override
    public boolean isPowered() {
        return getProxy() != null && getProxy().isPowered();
    }

    @Override
    public boolean isActive() {
        return getProxy() != null && getProxy().isActive();
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            if (getBaseMetaTileEntity() instanceof IGridProxyable) {
                gridProxy = new AENetworkProxy((IGridProxyable)getBaseMetaTileEntity(), "proxy", ItemList.Hatch_Output_Bus_ME.get(1), true);
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
                gridProxy.setValidSides(EnumSet.of(ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing())));
            }
        }
        return this.gridProxy;
    }

    @Override
    public void onFacingChange() {
        if (GregTech_API.mAE2) updateProxySides();
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void updateProxySides() {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.setValidSides(
                EnumSet.of(ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing())));
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        saveCache(nbt);
        if (GregTech_API.mAE2) saveProxy(nbt);
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        saveCache(nbt);
        // The machine block drops valid inventory slots separately.
        nbt.removeTag("meStorageCell");
    }

    private void saveCache(NBTTagCompound nbt) {
        storage.save(nbt);
        NBTTagList list = new NBTTagList();
        for (ItemStack cached : cachedItems) {
            NBTTagCompound stack = cached.writeToNBT(new NBTTagCompound());
            // ItemStack's standard Count tag is only a byte.
            stack.setInteger("cachedCount", cached.stackSize);
            list.appendTag(stack);
        }
        nbt.setTag("cachedItems", list);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        cachedItems.clear();
        if (nbt.hasKey("meStorageCell")) mInventory[0] = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("meStorageCell"));
        storage.load(nbt);
        NBTTagList list = nbt.getTagList("cachedItems", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound stackNbt = list.getCompoundTagAt(i);
            ItemStack cached = ItemStack.loadItemStackFromNBT(stackNbt);
            if (cached == null) continue;
            cached.stackSize = stackNbt.getInteger("cachedCount");
            if (cached.stackSize > 0) cachedItems.add(cached);
        }
        if (GregTech_API.mAE2) loadProxy(nbt);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void saveProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.writeToNBT(nbt);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void loadProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null && nbt.hasKey("proxy")) proxy.readFromNBT(nbt);
    }

    @Override
    public boolean isGivingInformation() { return true; }

    @Override
    public String[] getInfoData() {
        List<String> info = new ArrayList<String>();
        long amount = 0;
        for (ItemStack cached : cachedItems) amount += cached.stackSize;
        info.add("ME Output Bus: " + GT_Utility.formatNumbers(amount) + " / "
                + GT_Utility.formatNumbers(storage.getCapacity()) + " items buffered");
        for (ItemStack cached : cachedItems) {
            info.add(cached.getDisplayName() + ": " + GT_Utility.formatNumbers(cached.stackSize));
        }
        return info.toArray(new String[info.size()]);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public boolean isValid(Object verificationToken) {
        try {
            return (getProxy().getGrid() == verificationToken);
        } catch (GridAccessException e) {
            return false;
        }
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void gridChanged() {
    }
}
