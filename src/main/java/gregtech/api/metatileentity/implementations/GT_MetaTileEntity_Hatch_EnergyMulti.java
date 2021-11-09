package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import static gregtech.api.enums.GT_Values.V;

public class GT_MetaTileEntity_Hatch_EnergyMulti extends GT_MetaTileEntity_Hatch {
	
	public final int Amp;
	
	public GT_MetaTileEntity_Hatch_EnergyMulti(int aID, String aNameRegional, int aTier, int aAmp) {
		super(aID, "impact.hatch.multi.energy." + aTier, aNameRegional, aTier, 0, new String[]{
				"Energy Injector for Multiblocks", "Accepts up to " + EnumChatFormatting.YELLOW + aAmp + EnumChatFormatting.GRAY + " Amp"
		});
		this.Amp = aAmp;
	}
	
	public GT_MetaTileEntity_Hatch_EnergyMulti(String aName, int aTier, int aAmp, String aDescription, ITexture[][][] aTextures) {
		super(aName, aTier, 0, aDescription, aTextures);
		this.Amp = aAmp;
	}
	
	public ITexture[] getTexturesActive(ITexture aBaseTexture) {
		return new ITexture[]{aBaseTexture, Textures.BlockIcons.ENERGY_IN_POWER_MULTI[this.mTier]};
	}
	
	public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
		return new ITexture[]{aBaseTexture, Textures.BlockIcons.ENERGY_IN_POWER_MULTI[this.mTier]};
	}
	
	public boolean isSimpleMachine() {
		return true;
	}
	
	public boolean isFacingValid(byte aFacing) {
		return true;
	}
	
	public boolean isAccessAllowed(EntityPlayer aPlayer) {
		return true;
	}
	
	public boolean isEnetInput() {
		return true;
	}
	
	public boolean isInputFacing(byte aSide) {
		return aSide == this.getBaseMetaTileEntity().getFrontFacing();
	}
	
	public boolean isValidSlot(int aIndex) {
		return false;
	}
	
	public long getMinimumStoredEU() {
		return 128L * (long) this.Amp;
	}
	
	public long maxEUInput() {
		return V[this.mTier];
	}
	
	public long maxEUStore() {
		return 512L + V[this.mTier] * 4L * (long) this.Amp;
	}
	
	public long maxAmperesIn() {
		return this.Amp + (this.Amp >> 2);
	}
	
	public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
		return new GT_MetaTileEntity_Hatch_EnergyMulti(this.mName, this.mTier, this.Amp, this.mDescription, this.mTextures);
	}
	
	public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return false;
	}
	
	public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return false;
	}
	
	public String[] getDescription() {
		return new String[]{
				this.mDescription,
				"Amperes In" + ": " + EnumChatFormatting.GREEN + this.maxAmperesIn() + " A"};
	}
}