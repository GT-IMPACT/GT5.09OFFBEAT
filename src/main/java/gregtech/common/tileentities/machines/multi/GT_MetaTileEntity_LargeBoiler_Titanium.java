package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaTileEntity_LargeBoiler_Titanium
        extends GT_MetaTileEntity_LargeBoiler {
    public GT_MetaTileEntity_LargeBoiler_Titanium(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeBoiler_Titanium(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeBoiler_Titanium(this.mName);
    }
    
    @Override
    public ICasing getCasingType() {
        return Casings.STABLE_TITANIUM;
    }

    @Override
    public ICasing getPipeType() {
        return Casings.TITANIUM_PIPE;
    }

    @Override
    public ICasing getFireboxType() {
        return Casings.TITANIUM_FIREBOX;
    }

    @Override
    public int getEUt() {
        return 800;
    }

    @Override
    public int getEfficiencyIncrease() {
        return 8;
    }

    @Override
    int runtimeBoost(int mTime) {
        return mTime * 130 / 100;
    }
}