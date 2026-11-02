package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverFluidRegulator;
import gregtech.common.covers.modes.MachineProcessingCondition;
import gregtech.common.covers.modes.TransferMode;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.modularui2.widget.builder.EnumSeriesBuilder;
import net.minecraft.util.StatCollector;

import java.text.NumberFormat;

import static gregtech.common.covers.CoverFluidRegulator.TICK_RATE_MAX;
import static gregtech.common.covers.CoverFluidRegulator.TICK_RATE_MIN;

/** GTNH's full regulator layout and interval/condition controls. */
public class CoverFluidRegulatorGui extends CoverBaseGui<CoverFluidRegulator> {

    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    static {
        numberFormat.setMaximumFractionDigits(2);
    }

    public CoverFluidRegulatorGui(CoverFluidRegulator cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.fluid_regulator";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<TransferMode, ?> ioModeSyncValue = new EnumSyncValue<>(
            TransferMode.class, cover::getIOMode, cover::setIOMode).allowC2S();
        syncManager.syncValue("io_mode", ioModeSyncValue);
        EnumSyncValue<MachineProcessingCondition, ?> conditionModeSyncValue = new EnumSyncValue<>(
            MachineProcessingCondition.class, cover::getMachineProcessingCondition,
            cover::setMachineProcessingCondition).allowC2S();
        syncManager.syncValue("condition_mode", conditionModeSyncValue);
        column.child(makeRowLayout().child(positionRow(makeTransferModeRow(ioModeSyncValue)))
            .child(positionRow(makeMachineConditionModeRow(conditionModeSyncValue)))
            .child(positionRow(makeSpeedConfigRow()))
            .child(positionRow(makeAverageSpeedRow()).marginTop(ROW_PADDING)));
    }

    private static Flow makeTransferModeRow(EnumSyncValue<TransferMode, ?> value) {
        return Flow.row()
            .child(new ParentWidget<>().child(new EnumSeriesBuilder<>(TransferMode.class).value(value)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_EXPORT, GTGuiTextures.OVERLAY_BUTTON_IMPORT)
                .build(GuiAxis.X)).width(80))
            .child(IKey.lang("gt.interact.desc.fluid_regulator.ExpImp").asWidget());
    }

    private static Flow makeMachineConditionModeRow(EnumSyncValue<MachineProcessingCondition, ?> value) {
        return Flow.row()
            .child(new ParentWidget<>().child(new EnumSeriesBuilder<>(MachineProcessingCondition.class).value(value)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_CHECKMARK, GTGuiTextures.OVERLAY_BUTTON_USE_PROCESSING_STATE,
                    GTGuiTextures.OVERLAY_BUTTON_USE_INVERTED_PROCESSING_STATE)
                .build(GuiAxis.X)).width(80))
            .child(IKey.lang("gt.interact.desc.fluid_regulator.Conditional").asWidget());
    }

    private Flow makeSpeedConfigRow() {
        return Flow.row()
            .child(makeNumberField().value(new IntSyncValue(cover::getSpeed, cover::setSpeed).allowC2S())
                .numbersInt(cover::getMinSpeed, cover::getMaxSpeed).setFocusOnGuiOpen(true))
            .child(IKey.lang("gt.interact.desc.fluid_regulator.L").asWidget())
            .child(makeNumberField(36)
                .value(new IntSyncValue(cover::getTickRateForUi, cover::setTickRateForUi).allowC2S())
                .numbersInt(this::validateTickRate, () -> TICK_RATE_MIN, () -> TICK_RATE_MAX).defaultNumber(20))
            .child(IKey.lang("gt.interact.desc.fluid_regulator.Ticks").asWidget());
    }

    private int validateTickRate(int value) {
        long speed = Math.abs((long) cover.getSpeed());
        long transferRate = cover.getTransferRate();
        if (speed > transferRate * value) {
            value = (int) ((speed + transferRate - 1) / transferRate);
        }
        return value;
    }

    private Flow makeAverageSpeedRow() {
        return Flow.row().collapseDisabledChild()
            .child(IKey.dynamic(this::getAverageSpeedText).color(Color.RED.main).asWidget()
                .setEnabledIf(widget -> isAtSpeedLimit()))
            .child(IKey.dynamic(this::getAverageSpeedText).asWidget().setEnabledIf(widget -> !isAtSpeedLimit()));
    }

    private boolean isAtSpeedLimit() {
        return Math.abs(cover.getSpeed()) == cover.getMaxSpeed();
    }

    private String getAverageSpeedText() {
        return StatCollector.translateToLocal("gt.interact.desc.fluid_regulator.Average") + " "
            + numberFormat.format(cover.getTickRateForUi() == 0 ? 0 : cover.getSpeed() * 20d / cover.getTickRateForUi())
            + " " + StatCollector.translateToLocal("gt.interact.desc.fluid_regulator.L_Sec");
    }
}
