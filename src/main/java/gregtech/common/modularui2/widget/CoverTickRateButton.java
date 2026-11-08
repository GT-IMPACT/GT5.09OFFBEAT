package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.Cover;

/** GTNH cover tick-rate control; the client only requests bounded server-side steps. */
public class CoverTickRateButton extends ButtonWidget<CoverTickRateButton> {

    private final Cover cover;

    public CoverTickRateButton(Cover cover, PanelSyncManager syncManager) {
        this.cover = cover;
        overlay(GTGuiTextures.OVERLAY_BUTTON_HOURGLASS);
        syncHandler(new InteractionSyncHandler().setOnMousePressed(this::onClick));
        tooltipBuilder(this::buildTooltip);

        IntSyncValue tickRate = new IntSyncValue(cover::getTickRateAddition, cover::setTickRateAddition);
        tickRate.allowC2S(false);
        tickRate.setChangeListener(this::markTooltipDirty);
        syncManager.syncValue("tickRateAddition", tickRate);
    }

    private void onClick(MouseData mouseData) {
        if (!cover.isValid() || !cover.allowsTickRateAddition()
            || mouseData.mouseButton != 0 && mouseData.mouseButton != 1) {
            return;
        }
        int iterations = mouseData.ctrl ? 5 : 1;
        for (int step = 0; step < iterations; step++) {
            cover.adjustTickRateMultiplier(mouseData.mouseButton == 1);
        }
    }

    private void buildTooltip(RichTooltip tooltip) {
        String bound = "";
        if (cover.getTickRateAddition() == 0) {
            bound = IKey.lang("gt.cover.info.button.bounds_notification.minimum").get();
        } else if (cover.isTickRateAdditionMax()) {
            bound = IKey.lang("gt.cover.info.button.bounds_notification.maximum").get();
        }
        tooltip.addLine(IKey.lang("gt.cover.info.button.tick_rate.1", cover.getTickRate(), bound))
            .addLine(IKey.lang("gt.cover.info.button.tick_rate.2"))
            .addLine(IKey.lang("gt.cover.info.button.tick_rate.3"));
    }
}
