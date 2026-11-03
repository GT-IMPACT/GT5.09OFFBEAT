package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicTankBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class GT_MetaTileEntity_Hatch_Input extends GT_MetaTileEntity_Hatch {

    public static boolean mIgnoreMap = false;
    public GT_Recipe_Map mRecipeMap = null;
    public static Fluid mLockedFluid;
    public boolean disableFilter = true;

    public GT_MetaTileEntity_Hatch_Input(int aID, String aName, String aNameRegional, int aTier) {
        this(aID, aName, aNameRegional, aTier, new String[] { "Fluid Input for Multiblocks", "Capacity: " + GT_Utility.formatNumbers(8000L * (1L << aTier)) + "L" });
    }

    public GT_MetaTileEntity_Hatch_Input(int aID, String aName, String aNameRegional, int aTier, String[] aDescription) {
        this(aID, 3, aName, aNameRegional, aTier, aDescription);
    }

    public GT_MetaTileEntity_Hatch_Input(int aID, int aSlot, String aName, String aNameRegional, int aTier) {
        this(aID, aSlot, aName, aNameRegional, aTier, new String[] {
                "Fluid Input for Multiblocks",
                "Capacity: " + GT_Utility.formatNumbers((int) (8000L * (1L << aTier) / aSlot)) + "L"
        });
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
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setBoolean("disableFilter", disableFilter);
        if (mRecipeMap != null) {
            tag.setString("recipeMap", mRecipeMap.mUnlocalizedName);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        // This branch's legacy input hatches accepted any fluid and did not write a filter flag.
        disableFilter = !tag.hasKey("disableFilter") || tag.getBoolean("disableFilter");
        mRecipeMap = null;
        for (GT_Recipe_Map map : GT_Recipe_Map.sMappings) {
            if (map.mUnlocalizedName.equals(tag.getString("recipeMap"))) {
                mRecipeMap = map;
                break;
            }
        }
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        }
        aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEBasicTankBaseGui<>(this).build(data, syncManager, settings);
    }

    @Override
    public boolean doesFillContainers() {
        return false;
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
        // Recipes consume fluid stacks directly, including partial amounts.
        markStorageDirty();
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return mRecipeMap == null || disableFilter || mRecipeMap.containsInput(aFluid);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 1;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 0
            && (mRecipeMap == null || disableFilter || mRecipeMap.containsInput(aStack)
                || mRecipeMap.containsInput(GT_Utility.getFluidForFilledItem(aStack, true)));
    }

    @Override
    public void onScrewdriverRightClick(
        byte side,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        if (!getBaseMetaTileEntity().getCoverAtSide(ForgeDirection.getOrientation(side)).isGUIClickable()) {
            return;
        }
        disableFilter = !disableFilter;
        GT_Utility.sendChatToPlayer(
            player,
            StatCollector.translateToLocal("GT5U.hatch.disableFilter." + disableFilter)
        );
        markDirty();
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
