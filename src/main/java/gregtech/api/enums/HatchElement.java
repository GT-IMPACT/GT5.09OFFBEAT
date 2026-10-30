package gregtech.api.enums;

import net.minecraft.util.StatCollector;

import java.util.Locale;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

/**
 * Logical requirements: an input/output can be a bus or a fluid hatch, including IMPACT variants.
 */
public enum HatchElement implements IHatchElement {
    Maintenance {
        public boolean matches(IMetaTileEntity tile) {
            return tile instanceof GT_MetaTileEntity_Hatch_Maintenance;
        }

        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            return machine.mMaintenanceHatches.size();
        }

        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return machine.addMaintenanceToMachineList(tile, texture);
        }
    },
    Energy {
        public boolean matches(IMetaTileEntity tile) {
            return tile instanceof GT_MetaTileEntity_Hatch_Energy || tile instanceof GT_MetaTileEntity_Hatch_EnergyMulti;
        }

        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            return machine.mEnergyHatches.size() + machine.mEnergyHatchesMulti.size();
        }

        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return machine.addEnergyInputToMachineList(tile, texture);
        }
    },
    Input {
        public boolean matches(IMetaTileEntity tile) {
            return tile instanceof GT_MetaTileEntity_Hatch_Input || tile instanceof GT_MetaTileEntity_Hatch_InputBus
                    || tile instanceof GTMTE_Multi_Hatch_Input || tile instanceof GTMTE_BusHatch_Input;
        }

        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            return machine.mInputHatches.size() + machine.mInputBusses.size()
                    + machine.mQuadrInputHatches.size() + machine.mInputBusHatches.size();
        }

        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return machine.addInputToMachineList(tile, texture);
        }
    },
    Output {
        public boolean matches(IMetaTileEntity tile) {
            return tile instanceof GT_MetaTileEntity_Hatch_Output || tile instanceof GT_MetaTileEntity_Hatch_OutputBus;
        }

        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            return machine.mOutputHatches.size() + machine.mOutputBusses.size();
        }

        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return machine.addOutputToMachineList(tile, texture);
        }
    };

    @Override
    public String getDescription() {
        return StatCollector.translateToLocal("gt.tooltip.hatch." + name().toLowerCase(Locale.ROOT));
    }
}
