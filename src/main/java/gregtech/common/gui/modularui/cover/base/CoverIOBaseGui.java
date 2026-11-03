package gregtech.common.gui.modularui.cover.base;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverIOBase;
import gregtech.common.covers.modes.BlockMode;
import gregtech.common.covers.modes.MachineProcessingCondition;
import gregtech.common.covers.modes.TransferMode;
import gregtech.common.modularui2.widget.builder.EnumSeriesBuilder;
import net.minecraft.util.StatCollector;

/** GTNH's common pump/conveyor panel: transfer direction, processing condition and blocking. */
public class CoverIOBaseGui extends CoverBaseGui<CoverIOBase> {

    private final String guiId;

    public CoverIOBaseGui(CoverIOBase cover, String guiId) {
        super(cover);
        this.guiId = guiId;
    }

    @Override
    protected String getGuiId() {
        return guiId;
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        EnumSyncValue<TransferMode, ?> ioMode = new EnumSyncValue<>(
            TransferMode.class, cover::getIOMode, cover::setIOMode).allowC2S();
        syncManager.syncValue("io_mode", ioMode);
        IWidget directionButtons = new EnumSeriesBuilder<>(TransferMode.class).value(ioMode)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_EXPORT, GTGuiTextures.OVERLAY_BUTTON_IMPORT)
            .build(GuiAxis.X);

        EnumSyncValue<MachineProcessingCondition, ?> condition = new EnumSyncValue<>(
            MachineProcessingCondition.class,
            cover::getMachineProcessingCondition,
            cover::setMachineProcessingCondition
        ).allowC2S();
        syncManager.syncValue("condition_mode", condition);
        IWidget conditionButtons = new EnumSeriesBuilder<>(MachineProcessingCondition.class).value(condition)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_CHECKMARK,
                GTGuiTextures.OVERLAY_BUTTON_USE_PROCESSING_STATE,
                GTGuiTextures.OVERLAY_BUTTON_USE_INVERTED_PROCESSING_STATE)
            .build(GuiAxis.X);

        EnumSyncValue<BlockMode, ?> blocking = new EnumSyncValue<>(
            BlockMode.class, cover::getBlockMode, cover::setBlockMode).allowC2S();
        syncManager.syncValue("block_mode", blocking);
        IWidget blockingButtons = new EnumSeriesBuilder<>(BlockMode.class).value(blocking)
            .overlay(new DynamicDrawable(() -> ioMode.getValue() == TransferMode.EXPORT
                    ? GTGuiTextures.OVERLAY_BUTTON_ALLOW_INPUT : GTGuiTextures.OVERLAY_BUTTON_ALLOW_OUTPUT),
                new DynamicDrawable(() -> ioMode.getValue() == TransferMode.EXPORT
                    ? GTGuiTextures.OVERLAY_BUTTON_BLOCK_INPUT : GTGuiTextures.OVERLAY_BUTTON_BLOCK_OUTPUT))
            .tooltip(IKey.dynamic(() -> ioMode.getValue() == TransferMode.EXPORT
                    ? StatCollector.translateToLocal("gt.interact.desc.conveyor.AllowIn")
                    : StatCollector.translateToLocal("gt.interact.desc.conveyor.AllowOut")),
                IKey.dynamic(() -> ioMode.getValue() == TransferMode.EXPORT
                    ? StatCollector.translateToLocal("gt.interact.desc.conveyor.BlockIn")
                    : StatCollector.translateToLocal("gt.interact.desc.conveyor.BlockOut")))
            .build(GuiAxis.X);

        String input = StatCollector.translateToLocal("gt.interact.desc.conveyor.InputBlock");
        String output = StatCollector.translateToLocal("gt.interact.desc.conveyor.OutputBlock");
        if (syncManager.isClient()) {
            IKey.renderer.setAlignment(Alignment.TopLeft, -1, -1);
        }
        IWidget blockingLabel = IKey.dynamic(() -> ioMode.getValue() == TransferMode.EXPORT ? input : output)
            .asWidget().coverChildrenHeight().width(getStringMaxWidth(input, output))
            .marginRight(TICK_RATE_BUTTON_SIZE);
        column.child(new Grid().marginLeft(WIDGET_MARGIN).coverChildren()
            .minElementMarginRight(WIDGET_MARGIN).minElementMarginBottom(2)
            .minElementMarginTop(0).minElementMarginLeft(0).alignment(Alignment.CenterLeft)
            .row(directionButtons, IKey.lang("gt.interact.desc.Pump.ExpImp").asWidget())
            .row(conditionButtons, IKey.lang("gt.interact.desc.conveyor.Conditional").asWidget())
            .row(blockingButtons, blockingLabel));
    }
}
