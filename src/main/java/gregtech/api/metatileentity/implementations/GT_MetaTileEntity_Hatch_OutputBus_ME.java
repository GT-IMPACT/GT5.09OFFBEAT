package gregtech.api.metatileentity.implementations;

import appeng.api.storage.IMEMonitorHandlerReceiver;
import gregtech.api.render.TextureFactory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import gregtech.api.GregTech_API;
import appeng.api.networking.GridFlags;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.Platform;
import cpw.mods.fml.common.Optional;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GT_Utility;


import java.util.Arrays;

public class GT_MetaTileEntity_Hatch_OutputBus_ME extends GT_MetaTileEntity_Hatch_OutputBus {
    private BaseActionSource requestSource = null;
    private AENetworkProxy gridProxy = null;
    private int update = 0;
    private static final int DEFAULT_TIER = 3; // HV (4x4)

    public GT_MetaTileEntity_Hatch_OutputBus_ME(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, DEFAULT_TIER, getSlots(DEFAULT_TIER), new String[]{
                "Item Output for Multiblocks", "Stores directly into ME"});
    }

    public GT_MetaTileEntity_Hatch_OutputBus_ME(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, getSlots(aTier), aDescription, aTextures);
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
        getProxy();
    }

    @Override
    public boolean storeAll(ItemStack aStack) {
        if (!GregTech_API.mAE2)
            return false;
        aStack.stackSize = store(aStack);
        return aStack.stackSize == 0 ? true : super.storeAll(aStack);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aBaseMetaTileEntity.isAllowedToWork() && (aTick&0x7)==0) {
            storeInventory(aBaseMetaTileEntity);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public void storeInventory(IInventory aInventory) {
        try {
            AENetworkProxy proxy = getProxy();
            if (proxy == null) {
                return;
            }
            IMEMonitor<IAEItemStack> sg = proxy.getStorage().getItemInventory();
            for (int i = 0, mInventoryLength = mInventory.length; i < mInventoryLength; i++) {
                ItemStack aStack = mInventory[i];
                if (GT_Utility.isStackInvalid(aStack)) {
                    continue;
                }
                IAEItemStack toStore = AEApi.instance().storage().createItemStack(aStack);
                IAEItemStack rest = Platform.poweredInsert( proxy.getEnergy(), sg, toStore, getRequest());
                if (rest != null) {
                    aStack.stackSize = (int)rest.getStackSize();
                } else {
                    mInventory[i] = null;
                }
            }
        } catch (final GridAccessException ignored) {

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
            if (proxy == null)
                return stack.stackSize;
            IMEMonitor<IAEItemStack> sg = proxy.getStorage().getItemInventory();
            IAEItemStack toStore = AEApi.instance().storage().createItemStack(stack);
            IAEItemStack rest = Platform.poweredInsert( proxy.getEnergy(), sg, toStore, getRequest());
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

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return isOutputFacing((byte)forgeDirection.ordinal()) ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) return true;
        aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            if (getBaseMetaTileEntity() instanceof IGridProxyable) {
                gridProxy = new AENetworkProxy((IGridProxyable)getBaseMetaTileEntity(), "proxy", ItemList.Hatch_Output_Bus_ME.get(1), true);
                gridProxy.onReady();
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            }
        }
        return this.gridProxy;
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
