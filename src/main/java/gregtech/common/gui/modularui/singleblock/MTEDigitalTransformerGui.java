package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Digital_Transformer;

import static gregtech.api.enums.GT_Values.VN;

/** Keeps the original Digital Transformer artwork, arithmetic controls and readouts on MUI2. */
public class MTEDigitalTransformerGui {

    private static final int TEXT_COLOR = 16448255;
    private static final UITexture BODY = UITexture.builder()
        .location("gregtech", "gui/Teleporter")
        .imageSize(256, 256)
        .subAreaXYWH(0, 4, 176, 162)
        .canApplyTheme()
        .build();

    private final GT_MetaTileEntity_Digital_Transformer machine;

    public MTEDigitalTransformerGui(GT_MetaTileEntity_Digital_Transformer machine) {
        this.machine = machine;
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        int headerHeight = CommonWidgets.MACHINE_HEADER_HEIGHT;
        IntSyncValue eut = new IntSyncValue(() -> machine.EUT);
        IntSyncValue amperage = new IntSyncValue(() -> machine.AMP);
        syncManager.syncValue("eut", eut);
        syncManager.syncValue("amperage", amperage);
        ModularPanel panel = GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .setWidth(176)
            .setHeight(166)
            .doesBindPlayerInventory(false)
            .doesAddGregTechLogo(false)
            .build();

        // Keep every body coordinate; only the old top border is omitted beneath the shared internal title.
        panel.child(BODY.asWidget().pos(0, headerHeight + 4).size(176, 162).decoration());
        panel.child(SlotGroupWidget.playerInventory((index, slot) -> slot.background(IDrawable.EMPTY))
            .pos(7, headerHeight + 83));
        panel.child(text(IKey.str("Digital Trans"), 8 + headerHeight));
        panel.child(text(IKey.dynamic(() -> "EUT: " + eut.getIntValue()), 24 + headerHeight));
        panel.child(text(IKey.dynamic(() -> "TIER: " + tier(eut.getIntValue())), 32 + headerHeight));
        panel.child(text(IKey.dynamic(() -> "AMP: " + amperage.getIntValue()), 40 + headerHeight));
        panel.child(text(IKey.dynamic(() -> "SUM: " + (long) amperage.getIntValue() * eut.getIntValue()),
            48 + headerHeight));

        int[] columns = {8, 26, 152, 134};
        for (int column = 0; column < columns.length; column++) {
            for (int row = 0; row < 4; row++) {
                int action = column * 4 + row;
                String syncName = "adjust_" + action;
                InteractionSyncHandler handler = new InteractionSyncHandler().setOnMousePressed(mouse -> {
                    if (!mouse.isClient() && MetaTileEntityGuiHandler.INSTANCE.canInteractWith(data.getPlayer(), data)
                        && data.getTileEntity() == machine.getBaseMetaTileEntity()
                        && machine.getBaseMetaTileEntity().getMetaTileEntity() == machine
                        && machine.adjustOutputSettings(action, mouse.shift)) {
                        GTGuis.markDirty(machine);
                    }
                });
                syncManager.syncValue(syncName, handler);
                panel.child(new ButtonWidget<>()
                    .syncHandler(handler)
                    .background(IDrawable.EMPTY)
                    .hoverBackground(new Rectangle().color(0x80ffffff))
                    .size(16)
                    .pos(columns[column], 5 + row * 18 + headerHeight));
            }
        }
        return panel;
    }

    private static TextWidget<?> text(IKey key, int y) {
        // The old foreground drew whole signed values, even when they extended beyond the black screen.
        return key.asWidget().pos(46, y).size(200, 9).textAlign(Alignment.TopLeft).color(TEXT_COLOR).shadow(false);
    }

    private static String tier(int eut) {
        return VN[GT_Utility.getTier(eut < 0 ? -eut : eut)];
    }
}
