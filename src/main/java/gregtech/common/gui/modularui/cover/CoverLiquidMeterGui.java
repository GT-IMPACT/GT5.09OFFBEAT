package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.common.covers.CoverLiquidMeter;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

/** GTNH fluid detector layout and controls. */
public class CoverLiquidMeterGui extends CoverBaseGui<CoverLiquidMeter> {

    private final int maxCapacity;

    public CoverLiquidMeterGui(CoverLiquidMeter cover, int maxCapacity) {
        super(cover);
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected String getGuiId() {
        return "cover.liquid_meter";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        column.child(makeRowLayout()
            .child(positionRow(RowHelper.makeInvertRedstoneRow(cover)))
            .child(positionRow(makeFluidThresholdRow(syncManager))));
    }

    private Flow makeFluidThresholdRow(PanelSyncManager syncManager) {
        IntSyncValue threshold = new IntSyncValue(cover::getThreshold, cover::setThreshold).allowC2S();
        syncManager.syncValue("threshold", threshold);
        return Flow.row().name("fluid_threshold")
            .child(makeNumberField().value(threshold).numbersInt(0, maxCapacity).setFocusOnGuiOpen(true))
            .child(IKey.lang("gt.interact.desc.Fluid_Meter.FluidThreshold").asWidget())
            .paddingRight(TICK_RATE_BUTTON_SIZE);
    }
}
