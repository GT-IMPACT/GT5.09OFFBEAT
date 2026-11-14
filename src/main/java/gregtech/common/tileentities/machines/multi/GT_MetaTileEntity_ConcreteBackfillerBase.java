package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;

import net.minecraft.nbt.NBTTagCompound;

public abstract class GT_MetaTileEntity_ConcreteBackfillerBase extends GT_MetaTileEntity_DrillerBase {

    private int mLastXOff = 0, mLastZOff = 0;

    public GT_MetaTileEntity_ConcreteBackfillerBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_ConcreteBackfillerBase(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return addBackfillerStructure(new MultiblockTooltipBuilder(mName)
                .addMachineType("Concrete Backfiller")
                .addInfo("Fills underground air pockets with concrete as the mining pipe rises.")
                .addInfo("Consumes 144 L of concrete per block.")
                .addInfo("Range: 16 blocks in each horizontal direction.")
                .addInfo("Mining pipes: controller or optional Input Bus."))
                .toolTipFinisher();
    }

    protected final MultiblockTooltipBuilder addBackfillerStructure(MultiblockTooltipBuilder tooltip) {
        return addCompactDrillerStructure(tooltip)
                .addHatchInfo("Input and Output Buses", "optional, up to 5 total; bottom layer (mining pipes)")
                .addHatchInfo("Input Hatches", "1+; bottom layer (concrete)");
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("backfillX", mLastXOff);
        tag.setInteger("backfillZ", mLastZOff);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        mLastXOff = tag.getInteger("backfillX");
        mLastZOff = tag.getInteger("backfillZ");
    }

    protected abstract int getRadius();

    @Override
    protected StructureRequirements.Builder createBaseRequirements() {
        return StructureRequirements.builder(getCasingType(), 0).atLeast(DrillerHatch.FLUID_INPUT);
    }

    @Override
    protected long getDrillingEUt() {
        return 6L << (Math.max(1, getDrillingTier()) * 2);
    }

    @Override
    protected int getDrillingDuration() {
        int tier = Math.max(1, getDrillingTier());
        return Math.max(1, (workState == STATE_UPWARD ? 240 : 80) / (1 << tier));
    }

    @Override
    protected CheckRecipeResult prepareUpward(DrillingPlan plan) {
        if (isRefillableBlock(plan.pipeX, plan.headY - 1, plan.pipeZ)) {
            return prepareRefill(plan, plan.pipeX, plan.headY - 1, plan.pipeZ);
        }
        int radius = getRadius();
        int startX = Math.max(-radius, Math.min(radius, mLastXOff));
        int startZ = Math.max(-radius, Math.min(radius, mLastZOff));
        if (startX == 0 && startZ == 0) {
            startX = -radius;
            startZ = -radius;
        }
        if (plan.headY != plan.drillY) {
            for (int x = startX; x <= radius; x++) {
                for (int z = x == startX ? startZ : -radius; z <= radius; z++) {
                    if (isRefillableBlock(plan.pipeX + x, plan.headY, plan.pipeZ + z)) {
                        final int nextX = x;
                        final int nextZ = z;
                        plan.afterCommit = () -> {
                            mLastXOff = nextX;
                            mLastZOff = nextZ;
                        };
                        return prepareRefill(plan, plan.pipeX + x, plan.headY, plan.pipeZ + z);
                    }
                }
            }
        }
        plan.afterCommit = () -> {
            mLastXOff = 0;
            mLastZOff = 0;
        };
        return super.prepareUpward(plan);
    }

    private boolean isRefillableBlock(int x, int y, int z) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        return tile.getBlock(x, y, z).isAir(tile.getWorld(), x, y, z)
                && !tile.getBlock(x, y, z).getMaterial().isSolid()
                && mayPlaceBlock(x, y, z, GregTech_API.sBlockConcretes, 8);
    }

    private CheckRecipeResult prepareRefill(DrillingPlan plan, int x, int y, int z) {
        if (!plan.inputs.consumeFluid(Materials.Concrete.getMolten(144L))) {
            return CheckRecipeResult.NO_RECIPE;
        }
        plan.world.setBlock(x, y, z, GregTech_API.sBlockConcretes, 8);
        return CheckRecipeResult.SUCCESS;
    }
}
