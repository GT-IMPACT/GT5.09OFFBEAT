package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.common.gui.modularui.cover.CoverModeGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

public abstract class CoverMode extends CoverLegacyData {

    private final String[] modes;

    protected CoverMode(CoverContext context, String[] modes) {
        super(context);
        this.modes = modes;
    }

    public int getModeCount() {
        return modes.length;
    }

    public int getMode() {
        return Math.floorMod(coverData, modes.length);
    }

    public String getModeName() {
        return modes[getMode()];
    }

    public void setMode(int mode) {
        setGuiVariable(Math.floorMod(mode, modes.length));
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverModeGui(this);
    }
}
