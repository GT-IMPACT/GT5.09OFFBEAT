package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.modes.PlayerDetectionMode;
import gregtech.common.gui.modularui.cover.CoverPlayerDetectorGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

public class CoverPlayerDetector extends CoverLegacyBehavior {

    public CoverPlayerDetector(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    public PlayerDetectionMode getPlayerDetectionMode() {
        switch (getVariable()) {
            case 1:
                return PlayerDetectionMode.ONLY_OWNER;
            case 2:
                return PlayerDetectionMode.OTHER_PLAYERS;
            default:
                return PlayerDetectionMode.ANY_PLAYER;
        }
    }

    public void setPlayerDetectionMode(PlayerDetectionMode mode) {
        if (mode == null) {
            return;
        }
        // The existing backend uses 1 for owner and 2 for others, opposite to the old GUI labels.
        switch (mode) {
            case ONLY_OWNER:
                setGuiVariable(1);
                break;
            case OTHER_PLAYERS:
                setGuiVariable(2);
                break;
            default:
                setGuiVariable(0);
                break;
        }
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverPlayerDetectorGui(this);
    }
}
