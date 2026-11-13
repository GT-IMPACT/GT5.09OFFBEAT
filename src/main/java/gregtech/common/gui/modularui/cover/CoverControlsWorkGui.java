package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.conditions.RedstoneCondition;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.modularui2.sync.LinkedBoolValue;
import gregtech.common.modularui2.widget.SelectButton;

/** Shared standalone/popup controls for the native machine-controller cover. */
public class CoverControlsWorkGui extends CoverBaseGui<CoverControlsWork> {

    public CoverControlsWorkGui(CoverControlsWork cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.machine_controller";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<RedstoneCondition, ?> condition = new EnumSyncValue<>(
            RedstoneCondition.class,
            cover::getRedstoneCondition,
            mode -> {
                if (cover.isValid()) {
                    cover.setRedstoneCondition(mode);
                }
            }
        ).allowC2S();
        syncManager.syncValue("condition_mode", condition);
        Grid choices = new Grid().marginLeft(WIDGET_MARGIN)
            .coverChildren()
            .minElementMarginRight(WIDGET_MARGIN)
            .minElementMarginBottom(2)
            .alignment(Alignment.CenterLeft);
        for (RedstoneCondition mode : RedstoneCondition.values()) {
            choices.row(
                new SelectButton().value(LinkedBoolValue.of(condition, mode)).overlay(overlay(mode)).size(16),
                IKey.lang(mode.getTranslationKey()).asWidget()
            );
        }
        BooleanSyncValue safeMode = new BooleanSyncValue(cover::isSafeMode, enabled -> {
            if (cover.isValid()) {
                cover.setSafeMode(enabled);
            }
        }).allowC2S();
        choices.row(
            new ToggleButton().value(safeMode)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS)
                .size(16),
            IKey.lang("gt.ui.cover.work.safe_mode").asWidget()
        );
        column.child(choices);
    }

    private static UITexture overlay(RedstoneCondition mode) {
        switch (mode) {
            case ENABLE_WITH_REDSTONE:
                return GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON;
            case DISABLE_WITH_REDSTONE:
                return GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF;
            default:
                return GTGuiTextures.OVERLAY_BUTTON_CROSS;
        }
    }
}
