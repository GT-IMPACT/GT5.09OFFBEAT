package gregtech.common.tileentities.machines.basic;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class GT_MetaTileEntity_AirCondenser extends GT_MetaTileEntity_BasicMachine {

    public GT_MetaTileEntity_AirCondenser(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, 1, "Condense " + 100 * (1 << aTier - 1) * (1 << aTier - 1) + " L per tick of Air.", 0, 0, "AirCondenser.png", "",
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_DRAIN),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_DRAIN),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_DRAIN),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_DRAIN),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT),
                new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT));
    }

    public GT_MetaTileEntity_AirCondenser(String aName, int aTier, String aDescription, ITexture[][][] aTextures, String aGUIName, String aNEIName) {
        super(aName, aTier, 1, aDescription, aTextures, 0, 0, aGUIName, aNEIName);
    }

    public GT_MetaTileEntity_AirCondenser(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, String aGUIName, String aNEIName) {
        super(aName, aTier, 1, aDescription, aTextures, 0, 0, aGUIName, aNEIName);
    }

    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_AirCondenser(this.mName, this.mTier, this.mDescriptionArray, this.mTextures, this.mGUIName, this.mNEIName);
    }

    public GT_Recipe.GT_Recipe_Map getRecipeList() {
        return null;
    }

    public int getCapacity() {
        return 100 * (1 << this.mTier - 1) * (1 << this.mTier - 1) * 20;
    }

    public int checkRecipe() {
        FluidStack tOutput = Materials.Air.getGas((100 * (1 << this.mTier - 1) * (1 << this.mTier - 1)));
        if (tOutput != null) {
            if (canOutput(tOutput)) {
                calculateOverclockedNess(32, 16);
                //In case recipe is too OP for that machine
                if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
                    return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
                this.mOutputFluid = tOutput;
                return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
            }
        }
        return DID_NOT_FIND_RECIPE;
    }
}
