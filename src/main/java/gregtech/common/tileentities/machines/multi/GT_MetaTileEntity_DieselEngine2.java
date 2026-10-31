package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Dynamo;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_DynamoMulti;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;

public class GT_MetaTileEntity_DieselEngine2 extends GT_MetaTileEntity_DieselEngine {
    protected int fuelConsumption = 0;
    protected int fuelValue = 0;
    protected int fuelRemaining = 0;
    protected boolean boostEu = false;

    public GT_MetaTileEntity_DieselEngine2(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_DieselEngine2(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return engineTooltip(new MultiblockTooltipBuilder(mName)
                .addMachineType("Combustion Engine Mk2")
                .addInfo("Consumes flammable fuel and lubricant; oxygen enables boosted output")
                .addInfo("Output: 8192 EU/t normally, 24576 EU/t when boosted")
                .addInfo("Oxygen: 3 L/t; lubricant: 2 L per 80 ticks (3 L when boosted)"));
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[]{getCasingType().getTexture(), TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_DIESEL_ENGINE_ACTIVE : Textures.BlockIcons.OVERLAY_FRONT_DIESEL_ENGINE)};
        }
        return new ITexture[]{getCasingType().getTexture()};
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<FluidStack> tFluids = getStoredFluids();
        Collection<GT_Recipe> tRecipeList = GT_Recipe.GT_Recipe_Map.sDieselFuels.mRecipeList;

        if (tFluids.size() > 0 && tRecipeList != null) { //Does input hatch have a diesel fuel?
            for (FluidStack hatchFluid1 : tFluids) { //Loops through hatches
                for (GT_Recipe aFuel : tRecipeList) { //Loops through diesel fuel recipes
                    FluidStack tLiquid;
                    if ((tLiquid = GT_Utility.getFluidForFilledItem(aFuel.getRepresentativeInput(0), true)) != null) { //Create fluidstack from current recipe
                        if (hatchFluid1.isFluidEqual(tLiquid)) { //Has a diesel fluid
                            fuelConsumption = tLiquid.amount = boostEu ? (10240 / aFuel.mSpecialValue) : (8192 / aFuel.mSpecialValue); //Calc fuel consumption
                            fuelConsumption = tLiquid.amount = Math.max(tLiquid.amount, 1);
                            if (depleteInput(tLiquid)) { //Deplete that amount
                                boostEu = depleteInput(Materials.Oxygen.getGas(3L));

                                if (tFluids.contains(Materials.Lubricant.getFluid(2L))) { //Has lubricant?
                                    //Deplete Lubricant. 1000L should = 1 hour of runtime (if baseEU = 2048)
                                    if (mRuntime % 80 == 0 || mRuntime == 0)
                                        depleteInput(Materials.Lubricant.getFluid(boostEu ? 3 : 2));
                                } else return false;

                                fuelValue = aFuel.mSpecialValue;
                                fuelRemaining = hatchFluid1.amount; //Record available fuel
                                if (mEfficiency < 2000)
                                    this.mEUt = 0;
                                else
                                    this.mEUt = 8192;
                                this.mProgresstime = 1;
                                this.mMaxProgresstime = 1;
                                this.mEfficiencyIncrease = 40;
                                if (this.mDynamoHatches.size() > 0) {
                                    for (GT_MetaTileEntity_Hatch_Dynamo hatch : mDynamoHatches) {
                                        if ((hatch.getBaseMetaTileEntity().getOutputVoltage() * hatch.mAmpers) < (mEUt * mEfficiency / 10000L)) {
                                            explodeMultiblock();
                                        }
                                    }
                                }
                                if (this.mDynamoHatchesMulti.size() > 0) {
                                    for (GT_MetaTileEntity_Hatch_DynamoMulti hatch : mDynamoHatchesMulti) {
                                        if ((hatch.getBaseMetaTileEntity().getOutputVoltage() * hatch.Amp) < (mEUt * mEfficiency / 10000L)) {
                                            explodeMultiblock();
                                        }
                                    }
                                }
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

    @Override
    public ICasing getCasingType() {
        return Casings.ROBUST_TUNGSTENSTEEL;
    }

    @Override
    public ICasing getIntakeType() {
        return Casings.TUNGSTENSTEEL_PIPE;
    }

    @Override
    public ICasing getGearboxType() {
        return Casings.TUNGSTENSTEEL_GEARBOX;
    }

    @Deprecated
    @Override
    public net.minecraft.block.Block getCasingBlock() {
        return getCasingType().getBlock();
    }

    @Deprecated
    @Override
    public byte getCasingMeta() {
        return (byte) getCasingType().getMetadata();
    }

    @Deprecated
    @Override
    public net.minecraft.block.Block getIntakeBlock() {
        return getIntakeType().getBlock();
    }

    @Deprecated
    @Override
    public byte getIntakeMeta() {
        return (byte) getIntakeType().getMetadata();
    }

    @Deprecated
    @Override
    public net.minecraft.block.Block getGearboxBlock() {
        return getGearboxType().getBlock();
    }

    @Deprecated
    @Override
    public byte getGearboxMeta() {
        return (byte) getGearboxType().getMetadata();
    }

    @Deprecated
    @Override
    public byte getCasingTextureIndex() {
        return (byte) getCasingType().getTextureId();
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_DieselEngine2(this.mName);
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return boostEu ? 30000 : 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 60;
    }

    @Override
    public String[] getInfoData() {
        int mPollutionReduction = 0;
        for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                mPollutionReduction = Math.max(tHatch.calculatePollutionReduction(100), mPollutionReduction);
            }
        }

        long storedEnergy = 0;
        long maxEnergy = 0;

        for (GT_MetaTileEntity_Hatch_Dynamo tHatch : mDynamoHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }

        for (GT_MetaTileEntity_Hatch_DynamoMulti tHatch : mDynamoHatchesMulti) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }


        return new String[]{
                EnumChatFormatting.BLUE + "Diesel Engine" + EnumChatFormatting.RESET,
                StatCollector.translateToLocal("GT5U.multiblock.energy") + ": " +
                        EnumChatFormatting.GREEN + Long.toString(storedEnergy) + EnumChatFormatting.RESET + " EU / " +
                        EnumChatFormatting.YELLOW + Long.toString(maxEnergy) + EnumChatFormatting.RESET + " EU",
                StatCollector.translateToLocal("GT5U.engine.output") + ": " + EnumChatFormatting.GREEN + (mEUt * mEfficiency / 10000) + EnumChatFormatting.RESET + " EU/t",
                StatCollector.translateToLocal("GT5U.engine.consumption") + ": " + EnumChatFormatting.YELLOW + fuelConsumption + EnumChatFormatting.RESET + " L/t",
                StatCollector.translateToLocal("GT5U.engine.value") + ": " + EnumChatFormatting.YELLOW + fuelValue + EnumChatFormatting.RESET + " EU/L",
                StatCollector.translateToLocal("GT5U.turbine.fuel") + ": " + EnumChatFormatting.GOLD + fuelRemaining + EnumChatFormatting.RESET + " L",
                StatCollector.translateToLocal("GT5U.engine.efficiency") + ": " + EnumChatFormatting.YELLOW + (mEfficiency / 100F) + EnumChatFormatting.YELLOW + " %",
                StatCollector.translateToLocal("GT5U.multiblock.pollution") + ": " + EnumChatFormatting.GREEN + mPollutionReduction + EnumChatFormatting.RESET + " %"

        };
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }
}