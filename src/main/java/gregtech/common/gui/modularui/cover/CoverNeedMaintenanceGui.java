package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverNeedMaintenance;
import gregtech.common.covers.modes.MaintenanceAlertCondition;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.modularui2.sync.LinkedBoolValue;
import gregtech.common.modularui2.widget.SelectButton;
import net.minecraft.util.StatCollector;

public class CoverNeedMaintenanceGui extends CoverBaseGui<CoverNeedMaintenance> {

    public CoverNeedMaintenanceGui(CoverNeedMaintenance cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.maintenance";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<MaintenanceAlertCondition, ?> threshold = new EnumSyncValue<>(
            MaintenanceAlertCondition.class,
            cover::getMaintenanceAlertCondition,
            cover::setMaintenanceAlertCondition
        ).allowC2S();
        EnumSyncValue<RedstoneMode, ?> redstone = new EnumSyncValue<>(
            RedstoneMode.class,
            cover::getRedstoneMode,
            cover::setRedstoneMode
        ).allowC2S();
        syncManager.syncValue("threshold", threshold);
        syncManager.syncValue("redstone_mode", redstone);
        Grid first = new Grid().coverChildren().marginRight(4).minElementMargin(1, 1)
            .alignment(Alignment.CenterLeft);
        Grid second = new Grid().coverChildren().minElementMargin(1, 1).alignment(Alignment.CenterLeft);
        for (MaintenanceAlertCondition condition : MaintenanceAlertCondition.values()) {
            Grid target = condition.ordinal() < 4 ? first : second;
            IKey label;
            IKey tooltip;
            if (condition.ordinal() < 5) {
                String count = Integer.toString(condition.ordinal() + 1);
                label = condition == MaintenanceAlertCondition.ISSUE_1 ? IKey.lang("gt.interact.desc.issue")
                    : IKey.lang("gt.interact.desc.issues", count);
                tooltip = IKey.lang("gt.interact.desc.need_maint_count", count, "");
            } else {
                label = IKey.lang(condition == MaintenanceAlertCondition.ROTOR_20
                    ? "gt.interact.desc.issue_rotor_low" : "gt.interact.desc.issue_rotor_dead");
                tooltip = IKey.lang(condition == MaintenanceAlertCondition.ROTOR_20
                    ? "gt.interact.desc.need_maint_rotor_lo" : "gt.interact.desc.need_maint_rotor_hi");
            }
            target.row(
                new SelectButton().value(LinkedBoolValue.of(threshold, condition))
                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK).addTooltipLine(tooltip).size(16),
                label.asWidget()
            );
        }
        second.row(
            new CycleButtonWidget().value(redstone)
                .stateOverlay(RedstoneMode.NORMAL, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                .stateOverlay(RedstoneMode.INVERTED, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON).size(16),
            IKey.dynamic(() -> StatCollector.translateToLocal(redstone.getValue() == RedstoneMode.NORMAL
                ? "gt.interact.desc.normal" : "gt.interact.desc.inverted")).asWidget()
        );
        column.child(Flow.row().marginLeft(WIDGET_MARGIN).coverChildren()
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN).child(first).child(second));
    }
}
