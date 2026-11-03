package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverPlayerDetector;
import gregtech.common.covers.modes.PlayerDetectionMode;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.modularui2.sync.LinkedBoolValue;
import gregtech.common.modularui2.widget.SelectButton;

public class CoverPlayerDetectorGui extends CoverBaseGui<CoverPlayerDetector> {

    public CoverPlayerDetectorGui(CoverPlayerDetector cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.player_detector";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<PlayerDetectionMode, ?> mode = new EnumSyncValue<>(
            PlayerDetectionMode.class,
            cover::getPlayerDetectionMode,
            cover::setPlayerDetectionMode
        ).allowC2S();
        syncManager.syncValue("mode", mode);
        String[] labels = {
            "gt.interact.desc.Player_Detector.Any_Player",
            "gt.interact.desc.Player_Detector.Other_Players",
            "gt.interact.desc.Player_Detector.Only_Owner"
        };
        String[] tooltips = {
            "gt.interact.desc.Player_Detector.Emit_Player_Close",
            "gt.interact.desc.Player_Detector.Emit_Player_No_Close",
            "gt.interact.desc.Player_Detector.Emit_You_Close"
        };
        Grid choices = new Grid().marginLeft(WIDGET_MARGIN).coverChildren()
            .minElementMarginRight(WIDGET_MARGIN).minElementMarginBottom(2)
            .minElementMarginTop(0).minElementMarginLeft(0).alignment(Alignment.CenterLeft);
        for (PlayerDetectionMode selected : PlayerDetectionMode.values()) {
            choices.row(
                new SelectButton().value(LinkedBoolValue.of(mode, selected))
                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                    .addTooltipLine(IKey.lang(tooltips[selected.ordinal()])).size(16),
                IKey.lang(labels[selected.ordinal()]).asWidget()
            );
        }
        column.child(choices);
    }
}
