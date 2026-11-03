package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.modes.ShutterMode;
import gregtech.common.gui.modularui.cover.CoverShutterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

/** Typed GTNH MUI2 controls over the existing shutter behavior and integer storage. */
public class CoverShutter extends CoverLegacyBehavior {

    public CoverShutter(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    public ShutterMode getShutterMode() {
        int mode = getVariable();
        return mode >= 0 && mode < ShutterMode.values().length
            ? ShutterMode.values()[mode] : ShutterMode.OPEN_IF_ENABLED;
    }

    public void setShutterMode(ShutterMode mode) {
        if (mode != null) {
            setGuiVariable(mode.ordinal());
        }
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverShutterGui(this);
    }
}
