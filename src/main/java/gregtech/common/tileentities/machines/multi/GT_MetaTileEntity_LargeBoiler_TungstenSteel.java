package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaTileEntity_LargeBoiler_TungstenSteel
        extends GT_MetaTileEntity_LargeBoiler {
    public GT_MetaTileEntity_LargeBoiler_TungstenSteel(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeBoiler_TungstenSteel(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeBoiler_TungstenSteel(this.mName);
    }
    
    @Override
    public ICasing getCasingType() {
        return Casings.ROBUST_TUNGSTENSTEEL;
    }

    @Override
    public ICasing getPipeType() {
        return Casings.TUNGSTENSTEEL_PIPE;
    }

    @Override
    public ICasing getFireboxType() {
        return Casings.TUNGSTENSTEEL_FIREBOX;
    }

    @Override
    public int getEUt() {
        return 1000;
    }

    @Override
    public int getEfficiencyIncrease() {
        return 4;
    }

    @Override
    int runtimeBoost(int mTime) {
        return mTime * 120 / 100;
    }

}
