package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.gui.modularui.cover.base.CoverIOBaseGui;

public class CoverPump extends CoverIOBase {

    public CoverPump(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverIOBaseGui(this, "cover.pump");
    }
}
