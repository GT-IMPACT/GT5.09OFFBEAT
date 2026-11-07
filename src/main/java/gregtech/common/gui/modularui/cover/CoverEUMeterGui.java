package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverEUMeter;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

/** GTNH energy detector layout and controls. */
public class CoverEUMeterGui extends CoverBaseGui<CoverEUMeter> {

    public CoverEUMeterGui(CoverEUMeter cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.eu_meter";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        column.child(makeRowLayout()
            .child(positionRow(makeEnergyTypeRow(syncManager)))
            .child(positionRow(RowHelper.makeInvertRedstoneRow(cover)))
            .child(positionRow(makeEnergyThresholdRow(syncManager))));
    }

    private Flow makeEnergyTypeRow(PanelSyncManager syncManager) {
        EnumSyncValue<CoverEUMeter.EnergyType, ?> type = new EnumSyncValue<>(
            CoverEUMeter.EnergyType.class,
            cover::getType,
            cover::setType
        ).allowC2S();
        syncManager.syncValue("energy_type", type);
        CycleButtonWidget button = new CycleButtonWidget().overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
            .value(type).size(16);
        for (CoverEUMeter.EnergyType energyType : CoverEUMeter.EnergyType.values()) {
            button.addTooltip(energyType.ordinal(), energyType.getTooltip());
        }
        return Flow.row().child(button).child(IKey.dynamic(() -> cover.getType().getTitle()).asWidget());
    }

    private Flow makeEnergyThresholdRow(PanelSyncManager syncManager) {
        LongSyncValue capacity = new LongSyncValue(() -> cover.getType().getTileEntityEnergyCapacity(cover.getTile()));
        syncManager.syncValue("energy_capacity", capacity);
        LongSyncValue threshold = new LongSyncValue(cover::getThreshold, cover::setThresdhold).allowC2S();
        syncManager.syncValue("threshold", threshold);
        return makeNamedColumn(IKey.lang("gt.interact.desc.EnergyThreshold"))
            .child(makeNumberField(120).value(threshold)
                .numbersLong(() -> 0L, capacity::getLongValue)
                .setFocusOnGuiOpen(true))
            .paddingRight(TICK_RATE_BUTTON_SIZE);
    }
}
