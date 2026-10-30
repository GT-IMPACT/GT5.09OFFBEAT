package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaTileEntity_LargeBoiler_Bronze
        extends GT_MetaTileEntity_LargeBoiler {
    public GT_MetaTileEntity_LargeBoiler_Bronze(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeBoiler_Bronze(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeBoiler_Bronze(this.mName);
    }
    
    @Override
    public ICasing getCasingType() {
        return Casings.BRONZE_PLATED_BRICKS;
    }

    @Override
    public ICasing getPipeType() {
        return Casings.BRONZE_PIPE;
    }

    @Override
    public ICasing getFireboxType() {
        return Casings.BRONZE_FIREBOX;
    }

    @Override
    public int getEUt() {
        return 400;
    }

    @Override
    public int getEfficiencyIncrease() {
        return 16;
    }

    @Override
    int runtimeBoost(int mTime) {
        return mTime * 2;
    }
}
