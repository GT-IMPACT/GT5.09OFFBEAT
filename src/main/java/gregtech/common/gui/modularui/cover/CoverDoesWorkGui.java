package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BinaryEnumSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverDoesWork;
import gregtech.common.covers.modes.DetectionMode;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.modularui2.sync.LinkedBoolValue;
import gregtech.common.modularui2.widget.SelectButton;
import net.minecraft.util.StatCollector;

public class CoverDoesWorkGui extends CoverBaseGui<CoverDoesWork> {

    public CoverDoesWorkGui(CoverDoesWork cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.activity_detector";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<DetectionMode, ?> detection = new EnumSyncValue<>(
            DetectionMode.class,
            cover::getDetectionMode,
            cover::setDetectionMode
        ).allowC2S();
        BinaryEnumSyncValue<RedstoneMode, ?> redstone = new BinaryEnumSyncValue<>(
            RedstoneMode.class,
            cover::getRedstoneMode,
            cover::setRedstoneMode
        ).allowC2S();
        syncManager.syncValue("detection_mode", detection);
        syncManager.syncValue("redstone_mode", redstone);
        Flow modes = Flow.row().coverChildren();
        modes.child(new SelectButton().value(LinkedBoolValue.of(detection, DetectionMode.RECIPE_PROGRESS))
            .overlay(GTGuiTextures.OVERLAY_BUTTON_PROGRESS).size(16));
        modes.child(new SelectButton().value(LinkedBoolValue.of(detection, DetectionMode.MACHINE_IDLE))
            .overlay(GTGuiTextures.OVERLAY_BUTTON_CHECKMARK).size(16));
        modes.child(new SelectButton().value(LinkedBoolValue.of(detection, DetectionMode.MACHINE_ENABLED))
            .overlay(GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON).size(16));
        column.child(new Grid().marginLeft(WIDGET_MARGIN).coverChildren()
            .minElementMarginRight(WIDGET_MARGIN).minElementMarginBottom(2)
            .minElementMarginTop(0).minElementMarginLeft(0).alignment(Alignment.CenterLeft)
            .row(modes, IKey.dynamic(() -> StatCollector.translateToLocal(detectionLabel(detection.getValue())))
                .asWidget())
            .row(new ToggleButton().value(redstone)
                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                    .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF).size(16),
                IKey.dynamic(() -> StatCollector.translateToLocal(redstone.getValue() == RedstoneMode.INVERTED
                    ? "gt.interact.desc.inverted" : "gt.interact.desc.normal")).asWidget()));
    }

    private static String detectionLabel(DetectionMode mode) {
        switch (mode) {
            case MACHINE_IDLE:
                return "gt.interact.desc.MachineIdle";
            case MACHINE_ENABLED:
                return "gt.interact.desc.MachineEnabled";
            default:
                return "gt.interact.desc.RecipeProgress";
        }
    }
}
