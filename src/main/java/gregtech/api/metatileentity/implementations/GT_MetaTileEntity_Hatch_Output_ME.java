package gregtech.api.metatileentity.implementations;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.AEApi;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.StorageChannel;
import appeng.api.util.AECableType;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import cpw.mods.fml.common.Optional;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.gui.GT_Container_MEOutput;
import gregtech.api.gui.GT_GUIContainer_MEOutput;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Utility;

/**
 * Fluid output adapted from GTNH's MTEHatchOutputME and MTEHatchOutputMEBase
 * to the legacy hatch and AE2 APIs. The cache holds fluids until ME accepts them.
 *
 * @see <a href="https://github.com/GTNewHorizons/GT5-Unofficial/tree/master/src/main/java/gregtech/common/tileentities/machines/outputme">GTNH ME outputs</a>
 */
public class GT_MetaTileEntity_Hatch_Output_ME extends GT_MetaTileEntity_Hatch_Output implements IMEOutput, IPowerChannelState {
    private static final String[] DESCRIPTION = { "Fluid Output for Multiblocks", "Stores directly into ME",
            "Default buffer: 128,000 L; insert a fluid storage cell to expand",
            "Cache Mode stores output in the inserted cell", "Requires an ME channel and power",
            "Connect ME cable to the front", "Screwdriver toggles Cache Mode" };
    private final List<FluidStack> cachedFluids = new ArrayList<FluidStack>();
    private AENetworkProxy gridProxy;
    private BaseActionSource requestSource;
    private final GT_MEOutputStorage<IAEFluidStack> storage = new GT_MEOutputStorage<IAEFluidStack>(
            StorageChannel.FLUIDS, 128000, () -> mInventory[0], this::getProxy, this::getRequest,
            this::markStorageDirty, this::returnToCache);

    @Override
    public GT_MEOutputStorage<IAEFluidStack> getMEOutputStorage() { return storage; }

    @Override
    public long getMEOutputAmount() {
        long amount = 0;
        for (FluidStack cached : cachedFluids) amount += cached.amount;
        return amount;
    }

    public GT_MetaTileEntity_Hatch_Output_ME(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 4);
    }

    @Override
    public String[] getDescription() { return DESCRIPTION; }

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

    @Override
    public int fill(FluidStack aFluid, boolean doFill) {
        if (!GregTech_API.mAE2 || aFluid == null || aFluid.getFluid() == null || aFluid.amount <= 0) return 0;
        if (!storage.canAccept(getMEOutputAmount()) || !canStore(aFluid)) return 0;
        int accepted = aFluid.amount;
        if (!doFill || accepted == 0) return accepted;
        storage.accepted();
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
    public int getCapacity() { return (int) Math.min(Integer.MAX_VALUE, storage.getCapacity()); }

    @Override
    public int getFluidAmount() {
        long amount = 0;
        for (FluidStack cached : cachedFluids) amount += cached.amount;
        return (int) Math.min(Integer.MAX_VALUE, amount);
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
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        // Do not run the ordinary hatch's adjacent-tank output logic.
        if (aBaseMetaTileEntity.isServerSide() && GregTech_API.mAE2) {
            storage.tick(aTick);
            if (aTick % 20 == 0) aBaseMetaTileEntity.setActive(getProxy() != null && getProxy().isActive());
            if (storage.shouldFlush(cachedFluids.isEmpty())) flushCache(aBaseMetaTileEntity);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void flushCache(IGregTechTileEntity base) {
        AENetworkProxy proxy = getProxy();
        base.setActive(proxy != null && proxy.isActive());
        if (proxy == null || !proxy.isActive() || !base.isAllowedToWork()) return;
        storage.flushed();
        Iterator<FluidStack> iterator = cachedFluids.iterator();
        while (iterator.hasNext()) {
            FluidStack cached = iterator.next();
            int remaining = store(cached);
            if (remaining != cached.amount) {
                cached.amount = remaining;
                if (remaining == 0) iterator.remove();
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
        if (requestSource == null) requestSource = new MachineSource((IActionHost) getBaseMetaTileEntity());
        return requestSource;
    }

    @Optional.Method(modid = "appliedenergistics2")
    protected boolean canStore(FluidStack fluid) {
        if (mInventory[0] == null) return true;
        return storage.canStore(AEApi.instance().storage().createFluidStack(fluid));
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
            gridProxy = new AENetworkProxy((IGridProxyable) getBaseMetaTileEntity(), "proxy", ItemList.Hatch_Output_ME.get(1), true);
            gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            gridProxy.setValidSides(EnumSet.of(ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing())));
        }
        return gridProxy;
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
    @Optional.Method(modid = "appliedenergistics2")
    public AECableType getCableConnectionType(ForgeDirection side) {
        return isOutputFacing((byte) side.ordinal()) ? AECableType.SMART : AECableType.NONE;
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
        for (FluidStack cached : cachedFluids) list.appendTag(cached.writeToNBT(new NBTTagCompound()));
        nbt.setTag("cachedFluids", list);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void saveProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.writeToNBT(nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        cachedFluids.clear();
        if (nbt.hasKey("meStorageCell")) mInventory[0] = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("meStorageCell"));
        storage.load(nbt);
        NBTTagList list = nbt.getTagList("cachedFluids", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            FluidStack cached = FluidStack.loadFluidStackFromNBT(list.getCompoundTagAt(i));
            if (cached != null && cached.amount > 0) cachedFluids.add(cached);
        }
        if (GregTech_API.mAE2) loadProxy(nbt);
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void loadProxy(NBTTagCompound nbt) {
        AENetworkProxy proxy = getProxy();
        if (proxy != null && nbt.hasKey("proxy")) proxy.readFromNBT(nbt);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player) {
        if (base.isServerSide()) base.openGUI(player);
        return true;
    }

    @Override
    public Object getServerGUI(int id, InventoryPlayer player, IGregTechTileEntity base) {
        return new GT_Container_MEOutput(player, base);
    }

    @Override
    public Object getClientGUI(int id, InventoryPlayer player, IGregTechTileEntity base) {
        return new GT_GUIContainer_MEOutput(player, base, "ME Fluid Output");
    }

    @Override
    public boolean isGivingInformation() { return true; }

    @Override
    public String[] getInfoData() {
        List<String> info = new ArrayList<String>();
        long amount = 0;
        for (FluidStack cached : cachedFluids) amount += cached.amount;
        info.add("ME Output Hatch: " + GT_Utility.formatNumbers(amount) + " / "
                + GT_Utility.formatNumbers(storage.getCapacity()) + " L buffered");
        for (FluidStack cached : cachedFluids) {
            info.add(cached.getLocalizedName() + ": " + GT_Utility.formatNumbers(cached.amount) + " L");
        }
        return info.toArray(new String[info.size()]);
    }

    @Override
    public boolean outputsSteam() { return true; }
    @Override
    public boolean outputsLiquids() { return true; }
    @Override
    public boolean outputsItems() { return false; }
    @Override
    public boolean isFluidLocked() { return false; }
    @Override
    public boolean doesFillContainers() { return false; }
    @Override
    public boolean doesEmptyContainers() { return false; }
    @Override
    public boolean displaysItemStack() { return false; }
    @Override
    public boolean canTankBeEmptied() { return false; }
    @Override
    public boolean isValidSlot(int index) { return index == 0; }
    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return GregTech_API.mAE2 && index == 0 && storage.isValidCell(stack);
    }
    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) { return false; }
    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) { return false; }
    @Override
    public void onScrewdriverRightClick(byte side, EntityPlayer player, float x, float y, float z) {
        if (getBaseMetaTileEntity().isServerSide() && GregTech_API.mAE2) {
            storage.setCacheMode(!storage.isCacheMode());
            GT_Utility.sendChatToPlayer(player, "Cache Mode: " + (storage.isCacheMode() ? "On" : "Off"));
        }
    }
}
