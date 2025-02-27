package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;

public class GT_MetaTileEntity_LargeTurbine_Plasma extends GT_MetaTileEntity_LargeTurbine {

    public GT_MetaTileEntity_LargeTurbine_Plasma(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeTurbine_Plasma(String aName) {
        super(aName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[1][aColorIndex + 1], aFacing == aSide ? aActive ? TextureFactory.of(Textures.BlockIcons.LARGETURBINE_TU_ACTIVE5) : TextureFactory.of(Textures.BlockIcons.LARGETURBINE_TU5) : Textures.BlockIcons.casingTexturePages[0][60]};
    }


    public String[] getDescription() {
        return new String[]{
                "Controller Block for the Large Plasma Generator",
                "Size(WxHxD): 3x3x4 (Hollow), Controller (Front centered)",
                "1x Plasma Input Hatch (Side centered)",
                "1x Maintenance Hatch (Side centered)",
                "1x Dynamo Hatch (Back centered)",
                "Tungstensteel Turbine Casings for the rest (24 at least!)",
                "Needs a Turbine Rotor (Inside controller GUI) in order for",
                "Turbine to remember the characteristics of Turbine Rotor (after that Turbine Rotor will be removed)",};
    }

    public int getFuelValue(FluidStack aLiquid) {
        if (aLiquid == null || GT_Recipe_Map.sTurbineFuels == null) return 0;
        FluidStack tLiquid;
        Collection<GT_Recipe> tRecipeList = GT_Recipe_Map.sPlasmaFuels.mRecipeList;
        if (tRecipeList != null) for (GT_Recipe tFuel : tRecipeList)
            if ((tLiquid = GT_Utility.getFluidForFilledItem(tFuel.getRepresentativeInput(0), true)) != null)
                if (aLiquid.isFluidEqual(tLiquid)) return tFuel.mSpecialValue;
        return 0;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeTurbine_Plasma(mName);
    }

    @Override
    public Block getCasingBlock() {
        return GregTech_API.sBlockCasings4;
    }

    @Override
    public byte getCasingMeta() {
        return 12;
    }

    @Override
    public byte getCasingTextureIndex() {
        return 60;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    @Override
    int fluidIntoPower(ArrayList<FluidStack> aFluids, int aOptFlow, int aBaseEff) {

        aOptFlow *= 40;
        int tEU = 0;

        int actualOptimalFlow = 0;

        if (!aFluids.isEmpty()) {

            FluidStack firstFuelType = new FluidStack(aFluids.get(0), 0); // Identify a SINGLE type of fluid to process.  Doesn't matter which one. Ignore the rest!
            int fuelValue = getFuelValue(firstFuelType);

            actualOptimalFlow = GT_Utility.safeInt((long) Math.ceil((double) (aOptFlow + fuelValue - 1) / (double) fuelValue));
            this.realOptFlow = actualOptimalFlow; // For scanner info

            int remainingFlow =  GT_Utility.safeInt((long) (actualOptimalFlow * 1.25f)); // Allowed to use up to 125% of optimal flow.  Variable required outside of loop for multi-hatch scenarios.            int flow = 0;
            int flow = 0;
            int totalFlow = 0;

            for (FluidStack aFluid : aFluids) {
                if (aFluid.isFluidEqual(firstFuelType)) {
                    flow = aFluid.amount; // Get all (steam) in hatch
                    flow = Math.min(flow, Math.min(remainingFlow, (int) (actualOptimalFlow * 1.25f))); // try to use up to 125% of optimal flow w/o exceeding remainingFlow
                    depleteInput(new FluidStack(aFluid, flow)); // deplete that amount
                    this.storedFluid = aFluid.amount;
                    remainingFlow -= flow; // track amount we're allowed to continue depleting from hatches
                    totalFlow += flow; // track total input used
                }
            }
            String fn = FluidRegistry.getFluidName(firstFuelType);
            String[] nameSegments = fn.split("\\.", 2);
            if (nameSegments.length == 2) {
                String outputName = nameSegments[1];
                FluidStack output = FluidRegistry.getFluidStack(outputName, totalFlow);
                if (output == null) {
                    output = FluidRegistry.getFluidStack("molten." + outputName, totalFlow);
                }
                if (output != null) {
                    addOutput(output);
                }
            }
            tEU = (int) (Math.min((float) actualOptimalFlow, totalFlow) * fuelValue);

            //GT_FML_LOGGER.info(totalFlow+" : "+fuelValue+" : "+aOptFlow+" : "+actualOptimalFlow+" : "+tEU);

            if (totalFlow != actualOptimalFlow) {
                float efficiency = 1.0f - Math.abs(((totalFlow - (float) actualOptimalFlow) / actualOptimalFlow));
                if (totalFlow > actualOptimalFlow) {
                    efficiency = 1.0f;
                }
                if (efficiency < 0)
                    efficiency = 0; // Can happen with really ludicrously poor inefficiency.
                tEU *= efficiency;
                tEU = Math.max(1, (int) ((long) tEU * (long) aBaseEff / 10000L));
            } else {
                tEU = (int) ((long) tEU * (long) aBaseEff / 10000L);
            }

            return tEU;

        }
        return 0;
    }


   /* 
    * moved to super
    * 
    * @Override
    public String[] getInfoData() {
        String tRunning = mMaxProgresstime>0 ?
                EnumChatFormatting.GREEN+"Turbine running"+EnumChatFormatting.RESET :
                EnumChatFormatting.RED+"Turbine stopped"+EnumChatFormatting.RESET;
        String tMaintainance = getIdealStatus() == getRepairStatus() ?
                EnumChatFormatting.GREEN+"No Maintainance issues"+EnumChatFormatting.RESET :
                EnumChatFormatting.RED+"Needs Maintainance"+EnumChatFormatting.RESET ;
        int tDura = 0;

        if (mInventory[1] != null && mInventory[1].getItem() instanceof GT_MetaGenerated_Tool_01) {
            tDura = GT_Utility.safeInt((long)(100.0f / GT_MetaGenerated_Tool.getToolMaxDamage(mInventory[1]) * (GT_MetaGenerated_Tool.getToolDamage(mInventory[1]))+1));
        }

        long storedEnergy=0;
        long maxEnergy=0;
        for(GT_MetaTileEntity_Hatch_Dynamo tHatch : mDynamoHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy+=tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy+=tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }

        return new String[]{
                EnumChatFormatting.BLUE+"Large Turbine"+EnumChatFormatting.RESET,
                "Stored Energy:",
                EnumChatFormatting.GREEN + Long.toString(storedEnergy) + EnumChatFormatting.RESET +" EU / "+
                        EnumChatFormatting.YELLOW + Long.toString(maxEnergy) + EnumChatFormatting.RESET +" EU",
                tRunning,
                "Current Output: "+EnumChatFormatting.RED+mEUt+EnumChatFormatting.RESET+" EU/t",
                "Optimal Flow: "+EnumChatFormatting.YELLOW+GT_Utility.safeInt((long)realOptFlow)+EnumChatFormatting.RESET+" L/s",
                "Fuel Remaining: "+EnumChatFormatting.GOLD+storedFluid+EnumChatFormatting.RESET+"L",
                "Current Speed: "+EnumChatFormatting.YELLOW+(mEfficiency/100F)+EnumChatFormatting.RESET+"%",
                "Turbine Damage: "+EnumChatFormatting.RED+Integer.toString(tDura)+EnumChatFormatting.RESET+"%",
                tMaintainance,
        };
    }*/
}
