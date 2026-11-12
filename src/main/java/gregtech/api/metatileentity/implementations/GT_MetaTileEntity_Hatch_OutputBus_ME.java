package gregtech.api.metatileentity.implementations;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import appeng.api.AEApi;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.Optional;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OutputBusType;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputBusMEGui;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class GT_MetaTileEntity_Hatch_OutputBus_ME extends GT_MetaTileEntity_Hatch_OutputBus
        implements IMEOutput, IPowerChannelState {

    private static final String COPIED_DATA_IDENTIFIER = "outputBusME";
    private boolean additionalConnection;
    private BaseActionSource requestSource = null;
    private AENetworkProxy gridProxy = null;
    private final List<ItemStack> cachedItems = new ArrayList<ItemStack>();
    private final GT_MEOutputStorage<IAEItemStack> storage = new GT_MEOutputStorage<IAEItemStack>(
            StorageChannel.ITEMS,
            1600,
            () -> mInventory[0],
            this::getProxy,
            this::getRequest,
            this::markStorageDirty,
            this::returnToCache,
            this::getBufferedAmount
    );

    @Override
    public GT_MEOutputStorage<IAEItemStack> getMEOutputStorage() {
        return storage;
    }

    @Override
    public long getMEOutputAmount() {
        long amount = 0;
        for (ItemStack cached : cachedItems) {
            amount += cached.stackSize;
        }
        return amount;
    }

    public GT_MetaTileEntity_Hatch_OutputBus_ME(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            1,
            1,
            new String[]{
                "Item Output for Multiblocks", "Stores directly into ME",
                "Default buffer: 1,600 items; insert a storage cell to expand",
                "Cache Mode stores output in the inserted cell", "Requires an ME channel and power",
                "Wire cutter toggles ME connections on all sides", "Screwdriver toggles Cache Mode",
                "Shift right click with screwdriver toggles Check Mode"}
        );
    }

    public GT_MetaTileEntity_Hatch_OutputBus_ME(
        String aName,
        int aTier,
        String[] aDescription,
        ITexture[][][] aTextures
    ) {
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
        if (GregTech_API.mAE2) {
            readyProxy();
            if (aBaseMetaTileEntity.isServerSide()) {
                storage.updateCell();
            }
        }
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (slot == 0 && GregTech_API.mAE2 && base != null && base.isServerSide()) {
            storage.updateCell();
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void readyProxy() {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) {
            proxy.onReady();
        }
    }

    /** Strict capacity for transactional processing; does not use the legacy per-tick overflow allowance. */
    public long getProcessingCapacity(ItemStack stack) {
        return GregTech_API.mAE2 && stack != null && canStore(stack)
                ? Math.max(0, storage.getCapacity() - getMEOutputAmount()) : 0;
    }

    public long getProcessingCapacity(ItemStack stack, long reservedSameType) {
        long capacity = getProcessingCapacity(stack);
        if (capacity > 0 && reservedSameType > 0 && storage.shouldCheck() && !canStore(stack, reservedSameType)) {
            return 0;
        }
        return capacity;
    }

    @Override
    public boolean storeAll(ItemStack aStack) {
        if (!GregTech_API.mAE2 || aStack == null || aStack.stackSize <= 0) {
            return false;
        }
        if (!storage.canAccept(getMEOutputAmount(), aStack.stackSize) || !canStore(aStack)) {
            return false;
        }
        storage.accepted();
        if (storage.isVoidCell()) {
            aStack.stackSize = 0;
            markStorageDirty();
            return true;
        }
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
            if (tick % 20 == 0) {
                base.setActive(getProxy() != null && getProxy().isActive());
            }
            if (storage.shouldFlush(cachedItems.isEmpty())) {
                flushCache(base);
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void flushCache(IGregTechTileEntity base) {
        AENetworkProxy proxy = getProxy();
        base.setActive(proxy != null && proxy.isActive());
        if (proxy == null || !proxy.isActive() || !base.isAllowedToWork()) {
            return;
        }
        storage.flushed();
        Iterator<ItemStack> iterator = cachedItems.iterator();
        while (iterator.hasNext()) {
            ItemStack cached = iterator.next();
            int remaining = store(cached);
            if (remaining != cached.stackSize) {
                cached.stackSize = remaining;
                if (remaining == 0) {
                    iterator.remove();
                }
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
        if (stack == null) {
            return 0;
        }
        try {
            AENetworkProxy proxy = getProxy();
            if (proxy == null || !proxy.isActive() || !getBaseMetaTileEntity().isAllowedToWork()) {
                return stack.stackSize;
            }
            IMEMonitor<IAEItemStack> sg = proxy.getStorage().getItemInventory();
            IAEItemStack toStore = AEApi.instance().storage().createItemStack(stack);
            IAEItemStack rest = storage.insert(toStore, sg);
            if (rest != null) {
                return (int) rest.getStackSize();
            }
            return 0;
        } catch (GridAccessException ignored) {
        }
        return stack.stackSize;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private BaseActionSource getRequest() {
        if (requestSource == null) {
            requestSource = new MachineSource((IActionHost) getBaseMetaTileEntity());
        }
        return requestSource;
    }

    @Optional.Method(modid = "appliedenergistics2")
    protected boolean canStore(ItemStack stack) {
        return canStore(stack, 0);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private boolean canStore(ItemStack stack, long reservedSameType) {
        if (mInventory[0] == null) {
            return true;
        }
        return storage.canStore(AEApi.instance().storage().createItemStack(stack), reservedSameType);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private long getBufferedAmount(IAEItemStack stack) {
        ItemStack item = stack.getItemStack();
        long amount = 0;
        for (ItemStack cached : cachedItems) {
            if (GT_Utility.areStacksEqual(cached, item)) {
                amount += cached.stackSize;
            }
        }
        return amount;
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
        return forgeDirection != ForgeDirection.UNKNOWN
                && (additionalConnection || isOutputFacing((byte) forgeDirection.ordinal()))
                ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aPlayer instanceof EntityPlayerMP) {
            ItemStack dataStick = aPlayer.inventory.getCurrentItem();
            if (ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
                boolean pasted = pasteCopiedData(aPlayer, dataStick.stackTagCompound);
                aPlayer.addChatMessage(new ChatComponentTranslation(
                        "GT5U.hatch.outputme.settings." + (pasted ? "loaded" : "invalid")));
                return pasted;
            }
        }
        if (aBaseMetaTileEntity.isServerSide()) {
            aBaseMetaTileEntity.openGUI(aPlayer);
        }
        return true;
    }

    @Override
    public void onLeftclick(IGregTechTileEntity base, EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        ItemStack dataStick = player.inventory.getCurrentItem();
        if (ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
            dataStick.stackTagCompound = getCopiedData(player);
            dataStick.setStackDisplayName("ME Output Bus Configuration");
            player.addChatMessage(new ChatComponentTranslation("GT5U.hatch.outputme.settings.saved"));
        }
    }

    @Override
    public String getCopiedDataIdentifier(EntityPlayer player) {
        return COPIED_DATA_IDENTIFIER;
    }

    @Override
    public NBTTagCompound getCopiedData(EntityPlayer player) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("type", getCopiedDataIdentifier(player));
        nbt.setBoolean("additionalConnection", additionalConnection);
        nbt.setByte("color", getBaseMetaTileEntity().getColorization());
        nbt.setBoolean("cacheMode", storage.isCacheMode());
        nbt.setBoolean("checkMode", storage.getCheckMode());
        nbt.setInteger("myPriority", storage.getPriority());
        return nbt;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer player, NBTTagCompound nbt) {
        if (nbt == null || !getCopiedDataIdentifier(player).equals(nbt.getString("type"))) {
            return false;
        }
        setConnectsToAllSides(nbt.getBoolean("additionalConnection"));
        getBaseMetaTileEntity().setColorization(nbt.getByte("color"));
        storage.setCacheMode(nbt.getBoolean("cacheMode"));
        storage.setCheckMode(nbt.getBoolean("checkMode"));
        storage.setPriority(nbt.getInteger("myPriority"));
        return true;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public boolean acceptsItemLock() {
        return false;
    }

    @Override
    public boolean isFiltered() {
        return GregTech_API.mAE2 && storage.isFiltered();
    }

    @Override
    public boolean isLocked() {
        return isFiltered();
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public boolean isFilteredToItem(ItemStack stack) {
        return stack != null && (!isFiltered()
                || storage.isAllowed(AEApi.instance().storage().createItemStack(stack)));
    }

    @Override
    public OutputBusType getBusType() {
        if (storage.isCacheMode()) {
            return isFiltered() ? OutputBusType.MECacheFiltered : OutputBusType.MECacheUnfiltered;
        }
        return isFiltered() ? OutputBusType.MEFiltered : OutputBusType.MEUnfiltered;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEHatchOutputBusMEGui(this).build(data, syncManager, settings);
    }

    @Override
    public boolean isValidSlot(int index) {
        return index == 0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return GregTech_API.mAE2 && index == 0 && storage.isValidCell(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) {
        return false;
    }

    @Override
    public void onScrewdriverRightClick(
        byte side,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        if (getBaseMetaTileEntity().isServerSide() && GregTech_API.mAE2) {
            if (player.isSneaking()) {
                storage.setCheckMode(!storage.getCheckMode());
                player.addChatMessage(new ChatComponentTranslation(
                        "GT5U.hatch.outputme.checkMode." + storage.getCheckMode()));
                if (storage.getCheckMode()) {
                    player.addChatMessage(new ChatComponentTranslation("GT5U.hatch.outputme.checkMode.desc"));
                }
            } else {
                storage.setCacheMode(!storage.isCacheMode());
                player.addChatMessage(new ChatComponentTranslation(
                        "GT5U.hatch.outputme.cacheMode." + storage.isCacheMode()));
                if (storage.isCacheMode()) {
                    player.addChatMessage(new ChatComponentTranslation("GT5U.hatch.outputme.cacheMode.desc"));
                }
            }
        }
    }

    @Override
    public boolean onWireCutterRightClick(
        byte side,
        byte wrenchingSide,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        if (!getBaseMetaTileEntity().isServerSide() || !GregTech_API.mAE2) {
            return false;
        }
        setConnectsToAllSides(!additionalConnection);
        player.addChatMessage(new ChatComponentTranslation("GT5U.hatch.additionalConnection." + additionalConnection));
        return true;
    }

    public boolean connectsToAllSides() {
        return additionalConnection;
    }

    public void setConnectsToAllSides(boolean connects) {
        additionalConnection = connects;
        if (GregTech_API.mAE2) {
            updateProxySides();
        }
        markStorageDirty();
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
                gridProxy = new AENetworkProxy(
                    (IGridProxyable) getBaseMetaTileEntity(),
                    "proxy",
                    ItemList.Hatch_Output_Bus_ME.get(1),
                    true
                );
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
                gridProxy.setValidSides(getValidProxySides());
                IGregTechTileEntity base = getBaseMetaTileEntity();
                if (base != null && base.getWorld() != null) {
                    gridProxy.setOwner(base.getWorld().getPlayerEntityByName(base.getOwnerName()));
                }
                byte color = getBaseMetaTileEntity().getColorization();
                gridProxy.setColor(color < 0 ? AEColor.Transparent : AEColor.values()[15 - color]);
            }
        }
        return this.gridProxy;
    }

    @Override
    public void onFacingChange() {
        if (GregTech_API.mAE2) {
            updateProxySides();
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void updateProxySides() {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) {
            proxy.setValidSides(getValidProxySides());
        }
    }

    private EnumSet<ForgeDirection> getValidProxySides() {
        return additionalConnection ? EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN))
                : EnumSet.of(ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing()));
    }

    @Override
    public void onColorChangeServer(byte color) {
        super.onColorChangeServer(color);
        if (GregTech_API.mAE2) {
            updateProxyColor(color);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void updateProxyColor(byte color) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) {
            proxy.setColor(color < 0 ? AEColor.Transparent : AEColor.values()[15 - color]);
            if (proxy.getNode() != null) {
                proxy.getNode().updateState();
            }
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        saveCache(nbt);
        if (GregTech_API.mAE2) {
            saveProxy(nbt);
        }
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        saveCache(nbt);
        // The machine block drops valid inventory slots separately.
        nbt.removeTag("meStorageCell");
    }

    private void saveCache(NBTTagCompound nbt) {
        nbt.setBoolean("meAdditionalConnection", additionalConnection);
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
        additionalConnection = nbt.getBoolean("meAdditionalConnection");
        cachedItems.clear();
        if (nbt.hasKey("meStorageCell")) {
            mInventory[0] = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("meStorageCell"));
        }
        storage.load(nbt);
        NBTTagList list = nbt.getTagList("cachedItems", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound stackNbt = list.getCompoundTagAt(i);
            ItemStack cached = ItemStack.loadItemStackFromNBT(stackNbt);
            if (cached == null) {
                continue;
            }
            cached.stackSize = stackNbt.getInteger("cachedCount");
            if (cached.stackSize > 0) {
                cachedItems.add(cached);
            }
        }
        if (GregTech_API.mAE2) {
            loadProxy(nbt);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void saveProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) {
            proxy.writeToNBT(nbt);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void loadProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) {
            if (nbt.hasKey("proxy")) {
                proxy.readFromNBT(nbt);
            }
            updateProxySides();
            updateProxyColor(getBaseMetaTileEntity().getColorization());
        }
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    private List<IAEItemStack> getCacheList() {
        List<IAEItemStack> stacks = new ArrayList<>();
        for (ItemStack cached : cachedItems) {
            stacks.add(AEItemStack.create(cached));
        }
        return stacks;
    }

    @Override
    @Optional.Method(modid = "Waila")
    public void getWailaNBTData(
        EntityPlayerMP player,
        TileEntity tile,
        NBTTagCompound tag,
        World world,
        int x,
        int y,
        int z
    ) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        storage.getWailaNBTData(tag, getCacheList());
    }

    @Override
    @Optional.Method(modid = "Waila")
    public void getWailaBody(
        ItemStack stack,
        List<String> tooltip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    ) {
        super.getWailaBody(stack, tooltip, accessor, config);
        tooltip.add(StatCollector.translateToLocalFormatted(
                "GT5U.waila.hatch.outputme.item_cache_capacity",
                GT_Utility.formatNumbers(accessor.getNBTData().getLong("cacheCapacity"))));
        GT_MEOutputStorage.WailaHelper.getWailaCacheBody("item", tooltip, accessor);
    }

    @Override
    @Optional.Method(modid = "Waila")
    public boolean hasWailaAdvancedBody(ItemStack stack, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return GT_MEOutputStorage.WailaHelper.hasWailaAdvancedBody(accessor);
    }

    @Override
    @Optional.Method(modid = "Waila")
    public void getWailaAdvancedBody(
        ItemStack stack,
        List<String> tooltip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    ) {
        super.getWailaAdvancedBody(stack, tooltip, accessor, config);
        GT_MEOutputStorage.WailaHelper.getWailaAdvancedBody("item", tooltip, accessor);
    }

    @Override
    public String[] getInfoData() {
        return storage.getInfoData(getCacheList(), "GT5U.infodata.hatch.output_bus_me",
                stack -> stack.getItemStack().getDisplayName());
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
