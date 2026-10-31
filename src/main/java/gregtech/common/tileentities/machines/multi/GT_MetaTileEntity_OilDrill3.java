package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaTileEntity_OilDrill3 extends GT_MetaTileEntity_OilDrillBase {
    public GT_MetaTileEntity_OilDrill3(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_OilDrill3(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_OilDrill3(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.ROBUST_TUNGSTENSTEEL;
    }

    @Override
    protected Materials getFrameMaterial() {
        return Materials.TungstenSteel;
    }

    @Override
    protected int getRangeInChunks() {
        return 8;
    }

    @Override
    protected int getMinTier() {
        return 4;
    }
}