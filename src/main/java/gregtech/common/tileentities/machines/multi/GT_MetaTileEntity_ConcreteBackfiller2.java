package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.MultiblockTooltipBuilder;

public class GT_MetaTileEntity_ConcreteBackfiller2 extends GT_MetaTileEntity_ConcreteBackfillerBase {
    public GT_MetaTileEntity_ConcreteBackfiller2(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_ConcreteBackfiller2(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_ConcreteBackfiller2(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.STABLE_TITANIUM;
    }

    @Override
    protected Materials getFrameMaterial() {
        return Materials.Titanium;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return addDrillerStructure(new MultiblockTooltipBuilder(mName)
                .addMachineType("Advanced Concrete Backfiller")
                .addInfo("Fills underground air pockets with concrete")
                .addInfo("Consumes 144 L of concrete per block")
                .addInfo("Radius: circuit sum x 2; default 64, maximum 128")
                .addInfo("Circuits: controller or Data Access Hatch"));
    }

    @Override
    protected int getRadius() {
        int tConfig = getTotalConfigValue() * 2;
        return tConfig >= 128 ? 128 : tConfig <= 0 ? 64 : tConfig;
    }

    @Override
    protected int getMinTier() {
        return 4;
    }
}