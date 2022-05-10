package gregtech.api.metatileentity.implementations;

import appeng.api.storage.IMEMonitorHandlerReceiver;
import gregtech.api.render.TextureFactory;
import net.minecraft.entity.player.EntityPlayer;
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
import appeng.api.storage.data.IAEFluidStack;
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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;


import java.util.Arrays;

public class GT_MetaTileEntity_Hatch_Output_ME extends GT_MetaTileEntity_Hatch_Output {
    private BaseActionSource requestSource = null;
    private AENetworkProxy gridProxy = null;
    private int update = 0;

    public GT_MetaTileEntity_Hatch_Output_ME(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, new String[]{
                "Fluid Output for Multiblocks", "Stores directly into ME"});
    }

    public GT_MetaTileEntity_Hatch_Output_ME(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_Output_ME(mName, mTier, mDescriptionArray, mTextures);
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
	public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
		super.onPostTick(aBaseMetaTileEntity, aTick);
		if (aBaseMetaTileEntity.isServerSide()) {
			if (aBaseMetaTileEntity.isAllowedToWork() && (aTick & 0x7) == 0 && mFluid != null) {
                FluidStack tDrained = drain(mFluid.amount, true);
                if (tDrained != null) {
                    FluidStack tRemaining = store_fluid(tDrained, true);
                    if (tRemaining != null && tRemaining.amount > 0) {
                        super.fill(tRemaining, true);
                    }
                }
			}
		}
	}

    @Override
    public int fill(FluidStack aFluid, boolean doFill) {
        FluidStack remaining = store_fluid(aFluid.copy(), doFill);
        if (remaining != null) {
            return super.fill(remaining, doFill);
        }
        return 0;
    }

    /**
     * Attempt to store fluid in connected ME network. Returns how much fluid did not fit (if the network was down e.g.)
     *
     * @param stack input fluid
     * @return amount of fluid left over
     */
    @Optional.Method(modid = "appliedenergistics2")
    public FluidStack store_fluid(final FluidStack stack, boolean doFill) {
        if (stack == null)
            return null;
        if (!doFill)
            return stack.copy();
        try {
            AENetworkProxy proxy = getProxy();
            if (proxy == null) {
                return stack.copy();
            }
            IMEMonitor<IAEFluidStack> sg = proxy.getStorage().getFluidInventory();
            IAEFluidStack toStore = AEApi.instance().storage().createFluidStack(stack);
            IAEFluidStack rest = Platform.poweredInsert( proxy.getEnergy(), sg, toStore, getRequest());
            if (rest != null) {
                return rest.getFluidStack().copy();
            }
            return null;
        }
        catch( final GridAccessException ignored )
        {
        }
        return stack.copy();
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
                gridProxy = new AENetworkProxy((IGridProxyable)getBaseMetaTileEntity(), "proxy", ItemList.Hatch_Output_ME.get(1), true);
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
