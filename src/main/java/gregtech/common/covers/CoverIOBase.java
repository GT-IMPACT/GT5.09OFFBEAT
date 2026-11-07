package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.common.covers.modes.BlockMode;
import gregtech.common.covers.modes.MachineProcessingCondition;
import gregtech.common.covers.modes.TransferMode;

/** GTNH's shared transfer settings over the existing twelve persisted conveyor/pump modes. */
public abstract class CoverIOBase extends CoverLegacyData {

    protected CoverIOBase(CoverContext context) {
        super(context);
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    public TransferMode getIOMode() {
        return (coverData & 1) == 1 ? TransferMode.IMPORT : TransferMode.EXPORT;
    }

    public void setIOMode(TransferMode mode) {
        if (mode != null) {
            setGuiVariable(mode == TransferMode.EXPORT ? coverData & ~1 : coverData | 1);
        }
    }

    public MachineProcessingCondition getMachineProcessingCondition() {
        if (coverData % 6 < 2) {
            return MachineProcessingCondition.ALWAYS;
        }
        if (coverData % 6 == 2 || coverData % 6 == 3) {
            return MachineProcessingCondition.CONDITIONAL;
        }
        return MachineProcessingCondition.INVERTED;
    }

    public void setMachineProcessingCondition(MachineProcessingCondition mode) {
        if (mode == null) {
            return;
        }
        switch (mode) {
        case ALWAYS:
            setGuiVariable(coverData > 5 ? 0x6 | coverData & ~0xE : coverData & ~0xE);
            break;
        case CONDITIONAL:
            setGuiVariable(coverData > 5 ? 0x8 | coverData & ~0xE : 0x2 | coverData & ~0xE);
            break;
        case INVERTED:
            setGuiVariable(coverData > 5 ? 0xA | coverData & ~0xE : 0x4 | coverData & ~0xE);
            break;
        }
    }

    public BlockMode getBlockMode() {
        return coverData < 6 ? BlockMode.BLOCK : BlockMode.ALLOW;
    }

    public void setBlockMode(BlockMode mode) {
        if (mode == BlockMode.ALLOW && coverData <= 5) {
            setGuiVariable(coverData + 6);
        } else if (mode == BlockMode.BLOCK && coverData > 5) {
            setGuiVariable(coverData - 6);
        }
    }
}
