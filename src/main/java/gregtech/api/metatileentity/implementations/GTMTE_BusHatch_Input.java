package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_Container_BusHatch_Input;
import gregtech.api.gui.GT_GUIContainer_BusHatch_Input;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class GTMTE_BusHatch_Input extends GT_MetaTileEntity_Hatch {
	
	public static boolean mIgnoreMap = false;
	public GT_Recipe.GT_Recipe_Map mRecipeMap = null;
	public boolean disableSort;
	
	public GTMTE_BusHatch_Input(int aID, String aName, String aNameRegional, int aTier) {
		super(aID, aName, aNameRegional, aTier, 19, new String[]{
				"Fluid and Items Input for Multiblocks",
				"Capacity Fluid: 32,000 L",
				"Capacity Items: 16 stacks"
		});
	}
	
	public GTMTE_BusHatch_Input(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
		super(aName, aTier, 19, aDescription, aTextures);
	}
	
	@Override
	public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
		return new GTMTE_BusHatch_Input(mName, mTier, mDescriptionArray, mTextures);
	}
	
	@Override
	public ITexture[] getTexturesActive(ITexture aBaseTexture) {
		return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN)};
	}
	
	@Override
	public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
		return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN)};
	}
	
	@Override
	public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
		if (aBaseMetaTileEntity.isClientSide()) return true;
		aBaseMetaTileEntity.openGUI(aPlayer);
		return true;
	}
	
	@Override
	public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_Container_BusHatch_Input(aPlayerInventory, aBaseMetaTileEntity);
	}
	
	@Override
	public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_GUIContainer_BusHatch_Input(aPlayerInventory, aBaseMetaTileEntity, getLocalName());
	}
	
	@Override
	public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
		if (aBaseMetaTileEntity.isServerSide()) {
			if (aBaseMetaTileEntity.hasInventoryBeenModified()) {
				fillStacksIntoFirstSlots();
			}
			
			if (aTimer % 8 == 0) {
				IInventory tTileEntity = aBaseMetaTileEntity.getIInventoryAtSide(aBaseMetaTileEntity.getFrontFacing());
				if (tTileEntity != null && mInventory[getOutputSlot()] != null) {
					GT_Utility.moveOneItemStack(aBaseMetaTileEntity, tTileEntity,
							aBaseMetaTileEntity.getFrontFacing(), aBaseMetaTileEntity.getBackFacing(),
							null, false, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
				}
				
				
			}
		}
	}
	
	protected void fillStacksIntoFirstSlots() {
		if (disableSort) {
			for (int i = 1; i <= 16; i++)
				for (int j = i + 1; j <= 16; j++)
					if (mInventory[j] != null && mInventory[j].stackSize <= 0 && (mInventory[i] == null || GT_Utility.areStacksEqual(mInventory[i], mInventory[j])))
						GT_Utility.moveStackFromSlotAToSlotB(getBaseMetaTileEntity(), getBaseMetaTileEntity(), j, i, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
		} else {
			for (int i = 1; i <= 16; i++)
				for (int j = i + 1; j <= 16; j++)
					if (mInventory[j] != null && (mInventory[i] == null || GT_Utility.areStacksEqual(mInventory[i], mInventory[j])))
						GT_Utility.moveStackFromSlotAToSlotB(getBaseMetaTileEntity(), getBaseMetaTileEntity(), j, i, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
		}
	}
	
	public void updateSlots() {
		if (mInventory[0] != null && mInventory[0].stackSize <= 0) {
			mInventory[0] = null;
		}
		for (int i = 1; i <= 16; i++)
			if (mInventory[i] != null && mInventory[i].stackSize <= 0) mInventory[i] = null;
		fillStacksIntoFirstSlots();
	}
	
	@Override
	public void onScrewdriverRightClick(byte aSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
		if (!getBaseMetaTileEntity().getCoverBehaviorAtSide(aSide).isGUIClickable(aSide, getBaseMetaTileEntity().getCoverIDAtSide(aSide), getBaseMetaTileEntity().getCoverDataAtSide(aSide), getBaseMetaTileEntity()))
			return;
		if (aPlayer.isSneaking()) {
			disableSort = !disableSort;
			GT_Utility.sendChatToPlayer(aPlayer, trans("200", "Sort mode: " + (disableSort ? "Disabled" : "Enabled")));
		}
	}
	
	public String trans(String aKey, String aEnglish) {
		return GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_" + aKey, aEnglish, false);
	}
	
	@Override
	public void saveNBTData(NBTTagCompound aNBT) {
		super.saveNBTData(aNBT);
		aNBT.setBoolean("disableSort", disableSort);
	}
	
	@Override
	public void loadNBTData(NBTTagCompound aNBT) {
		super.loadNBTData(aNBT);
		disableSort = aNBT.getBoolean("disableSort");
	}
	
	@Override
	public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return aIndex == 17;
	}
	
	@Override
	public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		if (aIndex == 0) return aStack.getItem() instanceof IFluidContainerItem &&
				((IFluidContainerItem) aStack.getItem()).getCapacity(aStack) > 0 &&
				((IFluidContainerItem) aStack.getItem()).getFluid(aStack) != null;
		if (aIndex == 17) return false;
		return aIndex >= 1 && aIndex <= 16;
	}
	
	@Override
	public int getOutputSlot() {
		return 17;
	}
	
	@Override
	public int getStackDisplaySlot() {
		return 18;
	}
	
	@Override
	public boolean doesFillContainers() {
		return true;
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
	public boolean doesEmptyContainers() {
		return true;
	}
	
	@Override
	public boolean canTankBeFilled() {
		return true;
	}
	
	@Override
	public boolean canTankBeEmptied() {
		return true;
	}
	
	@Override
	public boolean displaysItemStack() {
		return true;
	}
	
	@Override
	public boolean displaysStackSize() {
		return false;
	}
	
	@Override
	public boolean isFluidInputAllowed(FluidStack aFluid) {
		return true;
	}
	
	@Override
	public int getCapacity() {
		return 32000;
	}
	
	@Override
	public int getTankPressure() {
		return -100;
	}
}
