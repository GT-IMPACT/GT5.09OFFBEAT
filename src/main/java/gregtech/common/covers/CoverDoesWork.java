package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.modes.DetectionMode;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.gui.modularui.cover.CoverDoesWorkGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

/** GTNH activity detection modes over the compatible integer flags used by the legacy backend. */
public class CoverDoesWork extends CoverLegacyBehavior {

    public CoverDoesWork(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    public DetectionMode getDetectionMode() {
        if ((getVariable() & 2) != 0) {
            return DetectionMode.MACHINE_IDLE;
        }
        return (getVariable() & 4) != 0 ? DetectionMode.MACHINE_ENABLED : DetectionMode.RECIPE_PROGRESS;
    }

    public void setDetectionMode(DetectionMode mode) {
        if (mode == null) {
            return;
        }
        switch (mode) {
            case MACHINE_IDLE:
                setGuiVariable((getVariable() & ~4) | 2);
                break;
            case MACHINE_ENABLED:
                setGuiVariable((getVariable() & ~2) | 4);
                break;
            default:
                setGuiVariable(getVariable() & ~6);
                break;
        }
    }

    public RedstoneMode getRedstoneMode() {
        return (getVariable() & 1) != 0 ? RedstoneMode.INVERTED : RedstoneMode.NORMAL;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (mode != null) {
            setGuiVariable(mode == RedstoneMode.INVERTED ? getVariable() | 1 : getVariable() & ~1);
        }
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverDoesWorkGui(this);
    }
}
