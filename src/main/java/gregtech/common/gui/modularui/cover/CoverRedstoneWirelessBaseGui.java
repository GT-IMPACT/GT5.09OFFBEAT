package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.CoverRedstoneWirelessBase;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;

public class CoverRedstoneWirelessBaseGui extends CoverBaseGui<CoverRedstoneWirelessBase> {

    public CoverRedstoneWirelessBaseGui(CoverRedstoneWirelessBase cover) {
        super(cover);
    }

    @Override
    protected String getGuiId() {
        return "cover.redstone_wireless";
    }

    @Override
    public void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data) {
        IntSyncValue frequency = new IntSyncValue(cover::getFrequency, cover::setFrequency).allowC2S();
        BooleanSyncValue privacy = new BooleanSyncValue(cover::isPrivateChannel, cover::setPrivateChannel).allowC2S();
        syncManager.syncValue("frequency", frequency);
        syncManager.syncValue("private_channel", privacy);
        column.child(positionRow(Flow.row().marginBottom(4)
                .child(makeNumberField(88).marginRight(2).height(12)
                    .numbersInt(0, CoverRedstoneWirelessBase.MAX_CHANNEL).value(frequency))
                .child(IKey.lang("gt.interact.desc.freq").asWidget())))
            .child(positionRow(Flow.row()
                .child(new ToggleButton().size(16).marginRight(2).value(privacy)
                    .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS)
                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK))
                .child(IKey.lang("gt.interact.desc.RedstoneWirelessBase.Use_Private_Freq").asWidget())
                .paddingRight(TICK_RATE_BUTTON_SIZE)));
    }
}
