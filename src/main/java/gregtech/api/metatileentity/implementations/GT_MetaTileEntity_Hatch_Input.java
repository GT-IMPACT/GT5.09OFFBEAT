package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class GT_MetaTileEntity_Hatch_Input extends GT_MetaTileEntity_Hatch {
    public static boolean mIgnoreMap = false;
    public GT_Recipe_Map mRecipeMap = null;
    public static Fluid mLockedFluid;

    public GT_MetaTileEntity_Hatch_Input(int aID, String aName, String aNameRegional, int aTier) {
        this(aID, aName, aNameRegional, aTier, new String[] { "Fluid Input for Multiblocks", "Capacity: " + GT_Utility.formatNumbers(8000L * (1L << aTier)) + "L" });
    }

    public GT_MetaTileEntity_Hatch_Input(int aID, String aName, String aNameRegional, int aTier, String[] aDescription) {
        this(aID, 3, aName, aNameRegional, aTier, aDescription);
    }

    public GT_MetaTileEntity_Hatch_Input(int aID, int aSlot, String aName, String aNameRegional, int aTier) {
        this(aID, aSlot, aName, aNameRegional, aTier, new String[] { "Fluid Input for Multiblocks", });
        mDescriptionArray[1] = "Capacity: " + GT_Utility.formatNumbers(getCapacityPerTank(aTier, aSlot)) + "L";
    }

    public GT_MetaTileEntity_Hatch_Input(int aID, int aSlot, String aName, String aNameRegional, int aTier, String[] aDescription) {
        super(aID, aName, aNameRegional, aTier, aSlot, aDescription);
    }

    public GT_MetaTileEntity_Hatch_Input(int aID, String aName, String aNameRegional, int aTier, int allSlotCount, String[] strings) {
        super(aID, aName, aNameRegional, aTier, allSlotCount, strings);
    }

    public GT_MetaTileEntity_Hatch_Input(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 3, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_Input(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 3, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_Input(String aName, int aSlots, int aTier, String[] aDescription,
                                         ITexture[][][] aTextures) {
        super(aName, aTier, aSlots, aDescription, aTextures);
    }

    public int getCapacityPerTank(int aTier, int aSlot) {
        return (int) (8000L * (1L << aTier) / aSlot);
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
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_Input(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) return true;
        aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    public boolean doesFillContainers() {
        return true;
        //return false;
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

    public void updateSlots() {
        if (mInventory[getInputSlot()] != null && mInventory[getInputSlot()].stackSize <= 0)
            mInventory[getInputSlot()] = null;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return true;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aIndex == 1;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aIndex == 0;
    }

    @Override
    public int getCapacity() {
        return (2 << mTier + 2) * 1000;
    }

    @Override
    public int getTankPressure() {
        return -100;
    }
}
