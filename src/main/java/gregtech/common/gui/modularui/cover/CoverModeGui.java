package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverMode;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

public class CoverModeGui extends CoverBaseGui<CoverMode> {

    public CoverModeGui(CoverMode cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover." + cover.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        IntSyncValue mode = new IntSyncValue(cover::getMode, cover::setMode).allowC2S();
        syncManager.syncValue("mode", mode);
        column.child(makeRowLayout().paddingRight(TICK_RATE_BUTTON_SIZE)
                .child(positionRow(Flow.row()
                        .child(new CycleButtonWidget().stateCount(cover.getModeCount()).value(mode)
                                .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC).size(16))
                        .child(IKey.dynamic(cover::getModeName).asWidget()))));
    }
}
