package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaTileEntity_ConcreteBackfiller1 extends GT_MetaTileEntity_ConcreteBackfillerBase {
    public GT_MetaTileEntity_ConcreteBackfiller1(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_ConcreteBackfiller1(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_ConcreteBackfiller1(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.SOLID_STEEL;
    }

    @Override
    protected Materials getFrameMaterial() {
        return Materials.Steel;
    }

    @Override
    protected int getRadius() {
        return 16;
    }

    @Override
    protected int getMinTier() {
        return 2;
    }
}