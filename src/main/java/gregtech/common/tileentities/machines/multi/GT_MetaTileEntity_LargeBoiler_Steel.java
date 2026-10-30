package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaTileEntity_LargeBoiler_Steel
        extends GT_MetaTileEntity_LargeBoiler {
    public GT_MetaTileEntity_LargeBoiler_Steel(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeBoiler_Steel(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeBoiler_Steel(this.mName);
    }
    
    @Override
    public ICasing getCasingType() {
        return Casings.SOLID_STEEL;
    }

    @Override
    public ICasing getPipeType() {
        return Casings.STEEL_PIPE;
    }

    @Override
    public ICasing getFireboxType() {
        return Casings.STEEL_FIREBOX;
    }

    @Override
    public int getEUt() {
        return 600;
    }

    @Override
    public int getEfficiencyIncrease() {
        return 12;
    }

    @Override
    int runtimeBoost(int mTime) {
        return mTime * 150 / 100;
    }
}
