package gregtech.common.tileentities.machines.multi;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import space.gtimpact.virtual_world.api.VirtualAPI;
import space.gtimpact.virtual_world.api.core.WorldPos;
import space.gtimpact.virtual_world.api.services.mining.fluids.FluidExtractionResult;
import space.gtimpact.virtual_world.api.services.scanning.fluids.FluidVeinResult;

import static gregtech.common.GT_UndergroundOil.DIVIDER;

public abstract class GT_MetaTileEntity_OilDrillBase extends GT_MetaTileEntity_DrillerBase {

    private int mOilId = 0;

    public GT_MetaTileEntity_OilDrillBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_OilDrillBase(String aName) {
        super(aName);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("mOilId", mOilId);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        mOilId = aNBT.getInteger("mOilId");
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return addCompactDrillerStructure(new MultiblockTooltipBuilder(mName)
                .addMachineType("Oil / Gas / Fluid Drilling Rig")
                .addInfo("Extracts fluids from the local 4 x 4 chunk vein at bedrock or the world's bottom.")
                .addInfo("Yield depends on input voltage and remaining reserves.")
                .addInfo("Retracts pipes when yield is at or below the sum of circuit settings.")
                .addInfo("Circuits: Data Access Hatch, or controller after drilling.")
                .addInfo("Mining pipes: controller or Input Bus."))
                .addHatchInfo("Output Hatches", "1+; bottom layer")
                .addHatchInfo("Data Access Hatches", "optional; bottom layer (circuits)")
                .toolTipFinisher();
    }


    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS;
    }

    protected abstract int getRangeInChunks();

    @Override
    protected StructureRequirements.Builder createBaseRequirements() {
        return StructureRequirements.builder(getCasingType(), 0).atLeast(DrillerHatch.FLUID_OUTPUT);
    }

    @Override
    protected long getDrillingEUt() {
        return 7L << (getDrillingTier() * 2);
    }

    @Override
    protected int getDrillingDuration() {
        return 20;
    }

    @Override
    protected CheckRecipeResult prepareAtBottom(DrillingPlan plan) {
        switch (plan.lowerPipe(true)) {
            case 0:
                plan.nextState = STATE_DOWNWARD;
                return CheckRecipeResult.SUCCESS;
            case 3:
                plan.nextState = STATE_UPWARD;
                return CheckRecipeResult.SUCCESS;
            default:
                break;
        }
        if (reachingVoidOrBedrock()) {
            OilReserve reserve = readOilReserve();
            if (reserve != null && reserve.remaining > 1 && (mOilId <= 0 || mOilId == reserve.fluid.getFluidID())) {
                float speed = .5F + (getDrillingTier() - getMinTier()) * .25F;
                double amount = Math.floor(Math.max(reserve.remaining - 1, 1000.0) * speed / DIVIDER
                        * ((double) getRangeInChunks() * 16 * speed * 1.2F));
                if (amount > Integer.MAX_VALUE) {
                    return CheckRecipeResult.OUTPUT_OVERFLOW;
                }
                if (amount > 0 && amount > getTotalConfigValue()) {
                    plan.fluids = new FluidStack[]{new FluidStack(reserve.fluid, (int) amount)};
                    plan.commitResource = () -> consumeOilReserve(reserve);
                    plan.afterCommit = () -> mOilId = reserve.fluid.getFluidID();
                    return CheckRecipeResult.SUCCESS;
                }
            }
        }
        plan.nextState = STATE_UPWARD;
        return CheckRecipeResult.SUCCESS;
    }

    protected OilReserve readOilReserve() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        FluidVeinResult result = VirtualAPI.INSTANCE.getMining().getFluidVeinStateAtBlock(
                tile.getWorld().provider.dimensionId, new WorldPos(tile.getXCoord(), tile.getZCoord()));
        if (result == null || result.getFluid().getFluid() == null) {
            return null;
        }
        return new OilReserve(result.getFluid().getFluid(), result.getRemainingVolume());
    }

    protected boolean consumeOilReserve(OilReserve expected) {
        OilReserve current = readOilReserve();
        if (current == null || current.remaining != expected.remaining || !current.fluid.isFluidEqual(expected.fluid)) {
            return false;
        }
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        FluidExtractionResult result = VirtualAPI.INSTANCE.getMining().extractFluidAtBlock(
                tile.getWorld().provider.dimensionId, new WorldPos(tile.getXCoord(), tile.getZCoord()), 1);
        return result != null && result.getExtractedVolume() == 1;
    }

    protected static final class OilReserve {
        public final FluidStack fluid;
        public final int remaining;

        public OilReserve(FluidStack fluid, int remaining) {
            this.fluid = fluid.copy();
            this.remaining = remaining;
        }
    }
}
