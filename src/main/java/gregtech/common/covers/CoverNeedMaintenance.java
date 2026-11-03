package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.modes.MaintenanceAlertCondition;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.gui.modularui.cover.CoverNeedMaintenanceGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

public class CoverNeedMaintenance extends CoverLegacyBehavior {

    public CoverNeedMaintenance(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    public MaintenanceAlertCondition getMaintenanceAlertCondition() {
        int mode = getVariable() >>> 1;
        return mode < MaintenanceAlertCondition.values().length
            ? MaintenanceAlertCondition.values()[mode] : MaintenanceAlertCondition.ISSUE_1;
    }

    public void setMaintenanceAlertCondition(MaintenanceAlertCondition condition) {
        if (condition != null) {
            setGuiVariable((getVariable() & 1) | (condition.ordinal() << 1));
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
        return new CoverNeedMaintenanceGui(this);
    }
}
