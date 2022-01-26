package gregtech.common.tileentities.machines.basic;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import static gregtech.api.enums.Textures.BlockIcons.*;

public class GT_MetaTileEntity_Disassembler
        extends GT_MetaTileEntity_BasicMachine {

    public GT_MetaTileEntity_Disassembler(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, 1,
                "Disassembles Machines at " + Math.min(50 + 10 * aTier, 100) + "% Efficiency", 1, 9,
                "Disassembler.png", "",
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_SIDE_DISASSEMBLER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_SIDE_DISASSEMBLER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_SIDE_DISASSEMBLER),
                        TextureFactory.builder().addIcon(OVERLAY_SIDE_DISASSEMBLER_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_FRONT_DISASSEMBLER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_DISASSEMBLER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_FRONT_DISASSEMBLER),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_DISASSEMBLER_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_TOP_DISASSEMBLER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_TOP_DISASSEMBLER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_TOP_DISASSEMBLER),
                        TextureFactory.builder().addIcon(OVERLAY_TOP_DISASSEMBLER_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_BOTTOM_DISASSEMBLER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_BOTTOM_DISASSEMBLER_ACTIVE_GLOW).glow().build()),
                TextureFactory.of(
                        TextureFactory.of(OVERLAY_BOTTOM_DISASSEMBLER),
                        TextureFactory.builder().addIcon(OVERLAY_BOTTOM_DISASSEMBLER_GLOW).glow().build()));
    }

    public GT_MetaTileEntity_Disassembler(String aName, int aTier, String aDescription,
                                          ITexture[][][] aTextures, String aGUIName, String aNEIName) {
        super(aName, aTier, 1, aDescription, aTextures, 1, 9, aGUIName, aNEIName);
    }

    public GT_MetaTileEntity_Disassembler(String aName, int aTier, String[] aDescription,
                                          ITexture[][][] aTextures, String aGUIName, String aNEIName) {
        super(aName, aTier, 1, aDescription, aTextures, 1, 9, aGUIName, aNEIName);
    }

    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Disassembler(this.mName, this.mTier, this.mDescriptionArray,
                this.mTextures, this.mGUIName, this.mNEIName);
    }

    @Override
    public GT_Recipe_Map getRecipeList() {
        return GT_Recipe_Map.sDisassemblerRecipes;
    }

    public int checkRecipe() {
        NBTTagCompound tNBT = getInputAt(0).getTagCompound();
        if (tNBT != null) {
            tNBT = tNBT.getCompoundTag("GT.CraftingComponents");
            if (tNBT != null) {
                boolean isAnyOutput = false;
                calculateOverclockedNessDisassembler(16);
                this.mMaxProgresstime = 80;
                //In case recipe is too OP for that machine
                if (mEUt == Integer.MAX_VALUE - 1)//&& mMaxProgresstime==Integer.MAX_VALUE-1
                {
                    return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
                }
                for (int i = 0; i < this.mOutputItems.length; i++) {
                    if (getBaseMetaTileEntity().getRandomNumber(100) < 50 + 10 * this.mTier) {
                        this.mOutputItems[i] = GT_Utility.loadItem(tNBT, "Ingredient." + i);
                        if (this.mOutputItems[i] != null) {
                            this.mMaxProgresstime *= 1.7F;
                            isAnyOutput = true;
                        }
                    }
                }
                if (!isAnyOutput) {
                    return DID_NOT_FIND_RECIPE;
                }
                for (int i = mTier - 5; i > 0; i--) {
                    this.mMaxProgresstime >>= 1;
                    if (this.mMaxProgresstime == 0) {
                        this.mEUt = this.mEUt >> 1;
                    }
                }
                if (this.mEUt == 0) {
                    mEUt = 1;
                }
                if (this.mMaxProgresstime == 0) {
                    this.mMaxProgresstime = 1;
                }
                getInputAt(0).stackSize -= 1;
                return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
            }
        } else {
            return super.checkRecipe();
        }
        return DID_NOT_FIND_RECIPE;
    }

    private void calculateOverclockedNessDisassembler(int aEUt) {
        if (mTier == 0) {
            mEUt = aEUt >> 2;
        } else {
            //Long EUt calculation
            long xEUt = aEUt;
            //Isnt too low EUt check?
            long tempEUt = xEUt < GT_Values.V[1] ? GT_Values.V[1] : xEUt;

            while (tempEUt <= GT_Values.V[mTier - 1] * (long) mAmperage) {
                tempEUt <<= 2;//this actually controls overclocking
                xEUt <<= 2;//this is effect of overclocking
            }
            if (xEUt > Integer.MAX_VALUE - 1) {
                mEUt = Integer.MAX_VALUE - 1;
            } else {
                mEUt = (int) xEUt;
            }
        }
    }

    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide,
                                 ItemStack aStack) {
        return //(aIndex == 4 && GT_Utility.areStacksEqual(aStack, new ItemStack(Items.egg))) ||
                (super.allowPutStack(aBaseMetaTileEntity, aIndex, aSide, aStack)) && (
                        aStack.getTagCompound() != null) && (
                        aStack.getTagCompound().getCompoundTag("GT.CraftingComponents") != null);
    }
}
