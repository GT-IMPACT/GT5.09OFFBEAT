package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_Container_StorageTank;
import gregtech.api.gui.GT_GUIContainer_StorageTank;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.render.TextureFactory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import static gregtech.api.enums.Textures.BlockIcons.MACHINE_CASINGS;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_PIPE_OUT;

public abstract class GT_MetaTileEntity_StorageTank extends GT_MetaTileEntity_BasicTank {
	
	public boolean OutputFluid = false, mVoidFluidPart = false, mVoidFluidFull = false;
	public String lockedFluidName = null;
	public boolean mMode = false;
	
	public GT_MetaTileEntity_StorageTank(int aID, String aName, String aNameRegional, int aTier, int aInvSlotCount, String aDescription, ITexture... aTextures) {
		super(aID, aName, aNameRegional, aTier, aInvSlotCount, aDescription, aTextures);
	}
	
	public GT_MetaTileEntity_StorageTank(String aName, int aTier, int aInvSlotCount, String aDescription, ITexture[][][] aTextures) {
		super(aName, aTier, aInvSlotCount, aDescription, aTextures);
	}
	
	public GT_MetaTileEntity_StorageTank(String aName, int aTier, int aInvSlotCount, String[] aDescription, ITexture[][][] aTextures) {
		super(aName, aTier, aInvSlotCount, aDescription, aTextures);
	}
	
	@Override
	public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
		ITexture base = MACHINE_CASINGS[mTier][aColorIndex + 1];
		ITexture[] overlay = new ITexture[]{base,TextureFactory.of(textureOverlay()),TextureFactory.builder().addIcon(textureGlowOverlay()).glow().build()};
		if (aSide == 1 && aSide != aFacing) return overlay;
		if (aSide == aFacing) return new ITexture[]{base, TextureFactory.of(OVERLAY_PIPE_OUT)};
		return new ITexture[]{base};
	}
	
	protected abstract Textures.BlockIcons textureOverlay();
	
	protected abstract Textures.BlockIcons textureGlowOverlay();
	
	@Override
	public ITexture[][][] getTextureSet(ITexture[] aTextures) {
		return new ITexture[0][0][0];
	}
	
	@Override
	public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_GUIContainer_StorageTank(aPlayerInventory, aBaseMetaTileEntity, getLocalName());
	}
	
	@Override
	public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_Container_StorageTank(aPlayerInventory, aBaseMetaTileEntity);
	}
	
	public void saveNBTData(NBTTagCompound aNBT) {
		super.saveNBTData(aNBT);
		aNBT.setBoolean("OutputFluid", this.OutputFluid);
		aNBT.setBoolean("mVoidFluidPart", this.mVoidFluidPart);
		aNBT.setBoolean("mVoidFluidFull", this.mVoidFluidFull);
		aNBT.setBoolean("mMode", mMode);
		if (lockedFluidName != null && lockedFluidName.length() != 0) aNBT.setString("lockedFluidName", lockedFluidName);
		else aNBT.removeTag("lockedFluidName");
		
	}
	
	public void loadNBTData(NBTTagCompound aNBT) {
		super.loadNBTData(aNBT);
		this.OutputFluid    = aNBT.getBoolean("OutputFluid");
		this.mVoidFluidPart = aNBT.getBoolean("mVoidFluidPart");
		this.mVoidFluidFull = aNBT.getBoolean("mVoidFluidFull");
		mMode               = aNBT.getBoolean("mMode");
		lockedFluidName     = aNBT.getString("lockedFluidName");
		lockedFluidName     = lockedFluidName.length() == 0 ? null : lockedFluidName;
	}
	
	public boolean isFluidInputAllowed(FluidStack aFluid) {
		return !mMode || getLockedFluidName() == null || getLockedFluidName().equals(aFluid.getUnlocalizedName());
	}
	
	public String getLockedFluidName() {
		return lockedFluidName;
	}
	
	public void setLockedFluidName(String lockedFluidName) {
		this.lockedFluidName = lockedFluidName;
	}
	
	@Override
	public void onEmptyingContainerWhenEmpty() {
		if (this.lockedFluidName == null && this.mFluid != null) {
			this.setLockedFluidName(this.mFluid.getUnlocalizedName());
		}
	}
	
	public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
		super.onPostTick(aBaseMetaTileEntity, aTick);
		if (this.getBaseMetaTileEntity().isServerSide() && (aTick & 0x7) == 0) {
			
			if (mVoidFluidPart && mFluid != null && mFluid.amount >= getCapacity()) {
				mVoidFluidFull = false;
				mFluid.amount  = getCapacity() - (getCapacity() * 25 / 100);
			}
			
			if (mVoidFluidFull && mFluid != null) {
				mVoidFluidPart = false;
				mFluid         = null;
			}
			
			IFluidHandler tTileEntity = aBaseMetaTileEntity.getITankContainerAtSide(aBaseMetaTileEntity.getFrontFacing());
			if (tTileEntity != null) {
				
				if (this.OutputFluid) {
					for (boolean temp = true; temp && mFluid != null; ) {
						temp = false;
						FluidStack tDrained = aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()), Math.max(1, mFluid.amount), false);
						if (tDrained != null) {
							int tFilledAmount = tTileEntity.fill(ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()), tDrained, false);
							if (tFilledAmount > 0) {
								temp = true;
								tTileEntity.fill(ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()), aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()), tFilledAmount, true), true);
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean isLiquidOutput(byte aSide) {
		return true;
	}
	
	@Override
	public boolean isLiquidInput(byte aSide) {
		return aSide != getBaseMetaTileEntity().getFrontFacing();
	}
	
	@Override
	public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return aIndex == 1;
	}
	
	@Override
	public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return aIndex == 0;
	}
}