package gregtech.common.tileentities.machines.basic;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import static gregtech.api.enums.Textures.BlockIcons.*;

public class GT_MetaTileEntity_PotionBrewer
        extends GT_MetaTileEntity_BasicMachine {
    public GT_MetaTileEntity_PotionBrewer(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, 1, "Brewing your Drinks", 1, 0, "PotionBrewer.png", "",
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_SIDE_POTIONBREWER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_SIDE_POTIONBREWER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_SIDE_POTIONBREWER),
                        TextureFactory.builder().addIcon(OVERLAY_SIDE_POTIONBREWER_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_FRONT_POTIONBREWER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_POTIONBREWER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_FRONT_POTIONBREWER),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_POTIONBREWER_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_TOP_POTIONBREWER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_TOP_POTIONBREWER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_TOP_POTIONBREWER),
                        TextureFactory.builder().addIcon(OVERLAY_TOP_POTIONBREWER_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_BOTTOM_POTIONBREWER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_BOTTOM_POTIONBREWER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_BOTTOM_POTIONBREWER),
                        TextureFactory.builder().addIcon(OVERLAY_BOTTOM_POTIONBREWER_GLOW).glow().build())
        );
    }

    public GT_MetaTileEntity_PotionBrewer(String aName, int aTier, String aDescription, ITexture[][][] aTextures, String aGUIName, String aNEIName) {
        super(aName, aTier, 1, aDescription, aTextures, 1, 0, aGUIName, aNEIName);
    }

    public GT_MetaTileEntity_PotionBrewer(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, String aGUIName, String aNEIName) {
        super(aName, aTier, 1, aDescription, aTextures, 1, 0, aGUIName, aNEIName);
    }

    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_PotionBrewer(this.mName, this.mTier, this.mDescriptionArray, this.mTextures, this.mGUIName, this.mNEIName);
    }

    public GT_Recipe.GT_Recipe_Map getRecipeList() {
        return GT_Recipe.GT_Recipe_Map.sBrewingRecipes;
    }

    public int checkRecipe() {
        int tCheck = super.checkRecipe();
        if (tCheck != DID_NOT_FIND_RECIPE) {
            return tCheck;
        }
        calculateOverclockedNess(4,128);
        //In case recipe is too OP for that machine
        if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        return 0;
    }

    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return (super.allowPutStack(aBaseMetaTileEntity, aIndex, aSide, aStack)) && (getRecipeList().containsInput(aStack));
    }

    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return super.isFluidInputAllowed(aFluid);
    }

    public int getCapacity() {
        return 750;
    }
}
