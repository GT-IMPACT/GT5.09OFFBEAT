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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

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
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEFluidStack;
import cpw.mods.fml.common.Optional;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IDataCopyable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputMEGui;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/**
 * Fluid output adapted from GTNH's MTEHatchOutputME and MTEHatchOutputMEBase
 * to the legacy hatch and AE2 APIs. The cache holds fluids until ME accepts them.
 *
 * @see <a href="https://github.com/GTNewHorizons/GT5-Unofficial/tree/master/src/main/java/gregtech/common/tileentities/machines/outputme">GTNH ME outputs</a>
 */
public class GT_MetaTileEntity_Hatch_Output_ME extends GT_MetaTileEntity_Hatch_Output
        implements IMEOutput, IPowerChannelState, IDataCopyable {

    private static final String COPIED_DATA_IDENTIFIER = "outputHatchME";
    private static final String[] DESCRIPTION = { "Fluid Output for Multiblocks", "Stores directly into ME",
            "Default buffer: 128,000 L; insert a fluid storage cell to expand",
            "Cache Mode stores output in the inserted cell", "Requires an ME channel and power",
            "Wire cutter toggles ME connections on all sides", "Screwdriver toggles Cache Mode",
            "Shift right click with screwdriver toggles Check Mode" };
    private boolean additionalConnection;
    private final List<FluidStack> cachedFluids = new ArrayList<FluidStack>();
    private AENetworkProxy gridProxy;
    private BaseActionSource requestSource;
    private final GT_MEOutputStorage<IAEFluidStack> storage = new GT_MEOutputStorage<IAEFluidStack>(
            StorageChannel.FLUIDS,
            128000,
            () -> mInventory[0],
            this::getProxy,
            this::getRequest,
            this::markStorageDirty,
            this::returnToCache,
            this::getBufferedAmount
    );

    @Override
    public GT_MEOutputStorage<IAEFluidStack> getMEOutputStorage() {
        return storage;
    }

    @Override
    public long getMEOutputAmount() {
        long amount = 0;
        for (FluidStack cached : cachedFluids) {
            amount += cached.amount;
        }
        return amount;
    }

    public GT_MetaTileEntity_Hatch_Output_ME(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 4);
    }

    @Override
    public String[] getDescription() {
        return DESCRIPTION;
    }

    public GT_MetaTileEntity_Hatch_Output_ME(String aName, int aTier, String[] aDescription,
            ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_Output_ME(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_ME_HATCH) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return getTexturesActive(aBaseTexture);
    }

    /** Strict capacity for transactional processing, including cell filters and buffered output. */
    public long getProcessingCapacity(FluidStack stack) {
        return GregTech_API.mAE2 && stack != null && canStore(stack)
                ? Math.max(0, storage.getCapacity() - getMEOutputAmount()) : 0;
    }

    public long getProcessingCapacity(FluidStack stack, long reservedSameType) {
        long capacity = getProcessingCapacity(stack);
        if (capacity > 0 && reservedSameType > 0 && storage.shouldCheck() && !canStore(stack, reservedSameType)) {
            return 0;
        }
        return capacity;
    }

    @Override
    public int fill(FluidStack aFluid, boolean doFill) {
        if (!GregTech_API.mAE2 || aFluid == null || aFluid.getFluid() == null || aFluid.amount <= 0) {
            return 0;
        }
        if (!storage.canAccept(getMEOutputAmount(), aFluid.amount) || !canStore(aFluid)) {
            return 0;
        }
        int accepted = aFluid.amount;
        if (!doFill || accepted == 0) {
            return accepted;
        }
        storage.accepted();
        if (storage.isVoidCell()) {
            markStorageDirty();
            return accepted;
        }
        for (FluidStack cached : cachedFluids) {
            if (cached.isFluidEqual(aFluid) && cached.amount <= Integer.MAX_VALUE - accepted) {
                cached.amount += accepted;
                markStorageDirty();
                return accepted;
            }
        }
        FluidStack cached = aFluid.copy();
        cached.amount = accepted;
        cachedFluids.add(cached);
        markStorageDirty();
        return accepted;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public int getFluidAmount() {
        long amount = 0;
        for (FluidStack cached : cachedFluids) {
            amount += cached.amount;
        }
        return (int) Math.min(Integer.MAX_VALUE, amount);
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

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        // Do not run the ordinary hatch's adjacent-tank output logic.
        if (aBaseMetaTileEntity.isServerSide() && GregTech_API.mAE2) {
            storage.tick(aTick);
            if (aTick % 20 == 0) {
                aBaseMetaTileEntity.setActive(getProxy() != null && getProxy().isActive());
            }
            if (storage.shouldFlush(cachedFluids.isEmpty())) {
                flushCache(aBaseMetaTileEntity);
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
        Iterator<FluidStack> iterator = cachedFluids.iterator();
        while (iterator.hasNext()) {
            FluidStack cached = iterator.next();
            int remaining = store(cached);
            if (remaining != cached.amount) {
                cached.amount = remaining;
                if (remaining == 0) {
                    iterator.remove();
                }
                markStorageDirty();
            }
        }
    }

    /** Returns the amount the network could not accept. */
    @Optional.Method(modid = "appliedenergistics2")
    protected int store(FluidStack fluid) {
        try {
            AENetworkProxy proxy = getProxy();
            IAEFluidStack input = AEApi.instance().storage().createFluidStack(fluid);
            IAEFluidStack rest = storage.insert(input, proxy.getStorage().getFluidInventory());
            return rest == null ? 0 : (int) rest.getStackSize();
        } catch (GridAccessException ignored) {
            return fluid.amount;
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private BaseActionSource getRequest() {
        if (requestSource == null) {
            requestSource = new MachineSource((IActionHost) getBaseMetaTileEntity());
        }
        return requestSource;
    }

    @Optional.Method(modid = "appliedenergistics2")
    protected boolean canStore(FluidStack fluid) {
        return canStore(fluid, 0);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private boolean canStore(FluidStack fluid, long reservedSameType) {
        if (mInventory[0] == null) {
            return true;
        }
        return storage.canStore(AEApi.instance().storage().createFluidStack(fluid), reservedSameType);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private long getBufferedAmount(IAEFluidStack stack) {
        FluidStack fluid = stack.getFluidStack();
        long amount = 0;
        for (FluidStack cached : cachedFluids) {
            if (cached.isFluidEqual(fluid)) {
                amount += cached.amount;
            }
        }
        return amount;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void returnToCache(IAEFluidStack stack) {
        long amount = stack.getStackSize();
        while (amount > 0) {
            FluidStack fluid = stack.getFluidStack();
            fluid.amount = (int) Math.min(Integer.MAX_VALUE, amount);
            cachedFluids.add(fluid);
            amount -= fluid.amount;
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
        if (gridProxy == null && getBaseMetaTileEntity() instanceof IGridProxyable) {
            gridProxy = new AENetworkProxy(
                (IGridProxyable) getBaseMetaTileEntity(),
                "proxy",
                ItemList.Hatch_Output_ME.get(1),
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
        return gridProxy;
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
    @Optional.Method(modid = "appliedenergistics2")
    public AECableType getCableConnectionType(ForgeDirection side) {
        return side != ForgeDirection.UNKNOWN && (additionalConnection || isOutputFacing((byte) side.ordinal()))
                ? AECableType.SMART : AECableType.NONE;
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
        for (FluidStack cached : cachedFluids) {
            list.appendTag(cached.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("cachedFluids", list);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void saveProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) {
            proxy.writeToNBT(nbt);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        additionalConnection = nbt.getBoolean("meAdditionalConnection");
        cachedFluids.clear();
        if (nbt.hasKey("meStorageCell")) {
            mInventory[0] = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("meStorageCell"));
        }
        storage.load(nbt);
        NBTTagList list = nbt.getTagList("cachedFluids", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            FluidStack cached = FluidStack.loadFluidStackFromNBT(list.getCompoundTagAt(i));
            if (cached != null && cached.amount > 0) {
                cachedFluids.add(cached);
            }
        }
        if (GregTech_API.mAE2) {
            loadProxy(nbt);
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
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ItemStack dataStick = player.inventory.getCurrentItem();
            if (ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
                boolean pasted = pasteCopiedData(player, dataStick.stackTagCompound);
                player.addChatMessage(new ChatComponentTranslation(
                        "GT5U.hatch.outputme.settings." + (pasted ? "loaded" : "invalid")));
                return pasted;
            }
        }
        if (base.isServerSide()) {
            base.openGUI(player);
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
            dataStick.setStackDisplayName("ME Output Hatch Configuration");
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
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEHatchOutputMEGui(this).build(data, syncManager, settings);
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    private List<IAEFluidStack> getCacheList() {
        List<IAEFluidStack> stacks = new ArrayList<>();
        for (FluidStack cached : cachedFluids) {
            stacks.add(AEFluidStack.create(cached));
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
                "GT5U.waila.hatch.outputme.fluid_cache_capacity",
                GT_Utility.formatNumbers(accessor.getNBTData().getLong("cacheCapacity"))));
        GT_MEOutputStorage.WailaHelper.getWailaCacheBody("fluid", tooltip, accessor);
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
        GT_MEOutputStorage.WailaHelper.getWailaAdvancedBody("fluid", tooltip, accessor);
    }

    @Override
    public String[] getInfoData() {
        return storage.getInfoData(getCacheList(), "GT5U.infodata.hatch.output_me",
                stack -> stack.getFluidStack().getLocalizedName());
    }

    @Override
    public boolean outputsSteam() {
        return true;
    }

    @Override
    public boolean outputsLiquids() {
        return true;
    }

    @Override
    public boolean outputsItems() {
        return false;
    }

    @Override
    public boolean isFiltered() {
        return GregTech_API.mAE2 && storage.isFiltered();
    }

    @Override
    public boolean acceptsFluidLock(Fluid fluid) {
        return false;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public boolean isFilteredToFluid(FluidStack stack) {
        return stack != null && (!isFiltered()
                || storage.isAllowed(AEApi.instance().storage().createFluidStack(stack)));
    }

    @Override
    public OutputHatchType getHatchType() {
        if (storage.isCacheMode()) {
            return isFiltered() ? OutputHatchType.MECacheFiltered : OutputHatchType.MECacheUnfiltered;
        }
        return isFiltered() ? OutputHatchType.MEFiltered : OutputHatchType.MEUnfiltered;
    }

    @Override
    public boolean isFluidLocked() {
        return isFiltered();
    }

    @Override
    public String getLockedFluidName() {
        // ME partitions may allow several fluids; a legacy single-fluid lock must not restrict them.
        return null;
    }

    @Override
    public boolean doesFillContainers() {
        return false;
    }

    @Override
    public boolean doesEmptyContainers() {
        return false;
    }

    @Override
    public boolean displaysItemStack() {
        return false;
    }

    @Override
    public boolean canTankBeEmptied() {
        return false;
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
    public boolean allowPutStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) {
        return false;
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
}
