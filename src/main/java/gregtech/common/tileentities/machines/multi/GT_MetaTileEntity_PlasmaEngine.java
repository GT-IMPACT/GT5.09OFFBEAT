package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Dynamo;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;

public class GT_MetaTileEntity_PlasmaEngine extends GT_MetaTileEntity_DieselEngine {
    protected int fuelConsumption = 0;
    protected int fuelValue = 0;
    protected int fuelRemaining = 0;
    protected boolean boostEu = false;

    public GT_MetaTileEntity_PlasmaEngine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_PlasmaEngine(String aName) {
        super(aName);
    }

    public String[] getDescription() {
        return new String[]{
        		"Controller Block for the Large Plasma Engine",
                "Size(WxHxD): 3x3x4, Controller (front centered)",
                "3x3x4 of Radiation Proof Machine Casing (hollow, Min 16!)",
                "2x Core Chamber Casing inside the Hollow Casing",
                "8x Intermix Chamber Casing (around controller)",
                "2x Input Hatch (Plasma/Helium) (one of the Casings next to a Gear Box)",
                "1x Maintenance Hatch (one of the Casings next to a Gear Box)",
                "1x Muffler Hatch (top middle back, next to the rear Gear Box)",
                "1x Dynamo Hatch (back centered)",
                "Intermix Chamber Casings must not be obstructed in front (only air blocks)",
                "Supply Plasma and 1000L of Helium per hour to run.",
                "Supply 40L of Hydrogen per second to boost output (optional).",
                "Default: Produces 524288EU/t at 100% efficiency",
                "Boosted: Produces 1572846EU/t at 150% efficiency",
                "Causes " + 20 * getPollutionPerTick(null) + " Pollution per second"};
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[]{Textures.BlockIcons.casingTexturePages[0][44], TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_DIESEL_ENGINE_ACTIVE : Textures.BlockIcons.OVERLAY_FRONT_DIESEL_ENGINE)};
        }
        return new ITexture[]{Textures.BlockIcons.casingTexturePages[0][44]};
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<FluidStack> tFluids = getStoredFluids();
        Collection<GT_Recipe> tRecipeList = GT_Recipe.GT_Recipe_Map.sPlasmaFuels.mRecipeList;

        if(tFluids.size() > 0 && tRecipeList != null) { //Does input hatch have a plasma fuel?
            for (FluidStack hatchFluid1 : tFluids) { //Loops through hatches
                for(GT_Recipe aFuel : tRecipeList) { //Loops through plasma fuel recipes
                    FluidStack tLiquid;
                    if ((tLiquid = GT_Utility.getFluidForFilledItem(aFuel.getRepresentativeInput(0), true)) != null) { //Create fluidstack from current recipe
                        if (hatchFluid1.isFluidEqual(tLiquid)) { //Has a diesel fluid
                            fuelConsumption = tLiquid.amount = boostEu ? (1048576 / aFuel.mSpecialValue) : (524288 / aFuel.mSpecialValue); //Calc fuel consumption
                            if(depleteInput(tLiquid)) { //Deplete that amount
                                boostEu = depleteInput(Materials.Hydrogen.getGas(4L));

                                if(tFluids.contains(Materials.Helium.getGas(2L))) { //Has helium?
                                    if(mRuntime % 72 == 0 || mRuntime == 0) depleteInput(Materials.Helium.getGas(boostEu ? 2 : 1));
                                } else return false;

                                fuelValue = aFuel.mSpecialValue;
                                fuelRemaining = hatchFluid1.amount; //Record available fuel
                                this.mEUt = mEfficiency < 2000 ? 0 : 524288; //Output 0 if startup is less than 20%
                                this.mProgresstime = 1;
                                this.mMaxProgresstime = 1;
                                this.mEfficiencyIncrease = 50;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        this.mEUt = 0;
        this.mEfficiency = 0;
        return false;
    }
    
    public Block getCasingBlock() {
        return GregTech_API.sBlockCasings3;
    }

    public byte getCasingMeta() {
        return 12;
    }

    public Block getIntakeBlock() {
        return GregTech_API.sBlockCasings5;
    }

    public byte getIntakeMeta() {
        return 8;
    }

    public Block getGearboxBlock() {
        return GregTech_API.sBlockCasings5;
    }

    public byte getGearboxMeta() {
        return 15;
    }

    public byte getCasingTextureIndex() {
        return 44;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_PlasmaEngine(this.mName);
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return boostEu ? 30000 : 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 120;
    }

    @Override
    public String[] getInfoData() {
        return new String[]{
                "Plasma Engine",
                "Current Output: " + mEUt * mEfficiency / 10000 + " EU/t",
                "Fuel Consumption: " + fuelConsumption + "L/t",
                "Fuel Value: " + fuelValue + " EU/L",
                "Fuel Remaining: " + fuelRemaining + " Litres",
                "Current Efficiency: " + (mEfficiency / 100) + "%",
                getIdealStatus() == getRepairStatus() ? "No Maintainance issues" : "Needs Maintainance"};
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }
}
