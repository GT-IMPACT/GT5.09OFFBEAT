package gregtech.common.tileentities.machines.multi;

import static gregtech.api.enums.GT_Values.VN;
import static gregtech.common.GT_UndergroundOil.DIVIDER;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Utility;
import space.gtimpact.virtual_world.api.VirtualAPI;
import space.gtimpact.virtual_world.api.core.WorldPos;
import space.gtimpact.virtual_world.api.services.mining.fluids.FluidExtractionResult;

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

    protected String[] getDescriptionInternal(String tierSuffix) {
        String casings = getCasingBlockItem().get(0).getDisplayName();
        return new String[]{
                "Controller Block for the Oil/Gas/Fluid Drilling Rig " + (tierSuffix != null ? tierSuffix : ""),
                "Size(WxHxD): 3x7x3", "Controller (Front middle at bottom)",
                "3x1x3 Base of " + casings,
                "1x3x1 " + casings + " pillar (Center of base)",
                "1x3x1 " + getFrameMaterial().mName + " Frame Boxes (Each pillar side and on top)",
                "1x Output Hatch (One of base casings)",
                "1x Maintenance Hatch (One of base casings)",
                "1x " + VN[getMinTier()] + "+ Energy Hatch (Any bottom layer casing)",
                "Working on 4 * 4 chunks (underground vein size)",
                "Use Programmed Circuits to ignore near exhausted oil field"};
    }


    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "DrillingRig.png");
    }

    protected abstract int getRangeInChunks();

    @Override
    protected boolean checkHatches() {
        return !mMaintenanceHatches.isEmpty() && !mOutputHatches.isEmpty() && !mEnergyHatches.isEmpty();
    }

    @Override
    protected void setElectricityStats() {
        this.mEfficiency = getCurrentEfficiency(null);
        this.mEfficiencyIncrease = 10000;
        int tier = Math.max(0, GT_Utility.getTier(getMaxInputVoltage()));
        this.mEUt = -7 << (tier << 1);
        this.mMaxProgresstime = 20;
    }

    @Override
    protected boolean workingAtBottom(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe, int yHead, int oldYHead) {
        switch (tryLowerPipeState(true)) {
            case 0:
                workState = STATE_DOWNWARD;
                setElectricityStats();
                return true;
            case 3:
                workState = STATE_UPWARD;
                return true;
        }

        if (reachingVoidOrBedrock() && tryFillChunkList()) {

            float speed = .5F + (GT_Utility.getTier(getMaxInputVoltage()) - getMinTier()) * .25F;

            FluidStack tFluid = pumpOil(speed);

            if (tFluid != null && tFluid.amount > getTotalConfigValue()) {

                this.mOutputFluids = new FluidStack[]{tFluid};
                return true;
            }
        }

        workState = STATE_UPWARD;

        return true;
    }

    private FluidExtractionResult extractFluidFromVirtualUndergroundVein(int amount) {
        IGregTechTileEntity te = getBaseMetaTileEntity();
        return VirtualAPI.INSTANCE.getMining().extractFluidAtBlock(
                te.getWorld().provider.dimensionId,
                new WorldPos(te.getXCoord(), te.getZCoord()),
                amount
        );
    }

    private boolean tryFillChunkList() {
        FluidStack tFluid;

        if (mOilId <= 0) {

            FluidExtractionResult result = extractFluidFromVirtualUndergroundVein(0);

            if (result != null) {

                tFluid = result.getFluid().getFluid();
                if (tFluid != null) {
                    mOilId = tFluid.getFluidID();
                }

                return result.getRemainingVolume() > 0;
            }
        }

        return mOilId > 0;
    }

    private FluidStack pumpOil(float speed) {
        if (mOilId <= 0) return null;

        FluidStack tFluid, tOil;

        tOil = new FluidStack(FluidRegistry.getFluid(mOilId), 0);

        FluidExtractionResult result = extractFluidFromVirtualUndergroundVein(1);

        if (result != null) {

            int rateExtract = getRangeInChunks();
            int veinSize = result.getRemainingVolume();

            int fluidExtracted = (int) Math.floor(((double) veinSize) * (double) speed / DIVIDER * ((double) rateExtract * 16 * speed * 1.2F));

            if (veinSize > 0) {
                FluidStack fluid = result.getFluid().getFluid();
                if (fluid != null) {
                    tFluid = new FluidStack(fluid, fluidExtracted);
                    if (tOil.isFluidEqual(tFluid)) {
                        tOil.amount += tFluid.amount;
                    }
                }
            }
        }

        return tOil.amount == 0 ? null : tOil;
    }
}
