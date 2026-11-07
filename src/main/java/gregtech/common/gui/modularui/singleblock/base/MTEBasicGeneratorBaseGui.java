package gregtech.common.gui.modularui.singleblock.base;

import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicGenerator;

/** GTNH generator specialization; generators inherit the complete tank screen and container I/O. */
public class MTEBasicGeneratorBaseGui<T extends GT_MetaTileEntity_BasicGenerator> extends MTEBasicTankBaseGui<T> {

    public MTEBasicGeneratorBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected boolean supportsPowerSwitch() {
        return true;
    }
}
