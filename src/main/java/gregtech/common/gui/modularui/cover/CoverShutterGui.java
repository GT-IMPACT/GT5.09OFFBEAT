package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverShutter;
import gregtech.common.covers.modes.ShutterMode;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.modularui2.sync.LinkedBoolValue;
import gregtech.common.modularui2.widget.SelectButton;

public class CoverShutterGui extends CoverBaseGui<CoverShutter> {

    public CoverShutterGui(CoverShutter cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.shutter";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<ShutterMode, ?> mode = new EnumSyncValue<>(
            ShutterMode.class,
            cover::getShutterMode,
            cover::setShutterMode
        ).allowC2S();
        syncManager.syncValue("mode", mode);
        String[] labels = {
            "gt.interact.desc.Shutter.Open_Enabled",
            "gt.interact.desc.Shutter.Open_Disabled",
            "gt.interact.desc.Shutter.OutAllow",
            "gt.interact.desc.Shutter.InAllow"
        };
        Grid choices = new Grid().marginLeft(WIDGET_MARGIN).coverChildren()
            .minElementMarginRight(WIDGET_MARGIN).minElementMarginBottom(2)
            .minElementMarginTop(0).minElementMarginLeft(0).alignment(Alignment.CenterLeft);
        for (ShutterMode selected : ShutterMode.values()) {
            choices.row(
                new SelectButton().value(LinkedBoolValue.of(mode, selected))
                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK).size(16),
                IKey.lang(labels[selected.ordinal()]).asWidget()
            );
        }
        column.child(choices);
    }
}
