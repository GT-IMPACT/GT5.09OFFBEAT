package gregtech.api.modularui2.common;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Dyes;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

/** Shared GTNH widgets used by machine screens and popup panels. */
public final class CommonWidgets {

    public static final int MACHINE_HEADER_HEIGHT = 9;

    private CommonWidgets() {
    }

    public static GhostCircuitSlotWidget createCircuitSlot(PanelSyncManager syncManager, IMetaTileEntity machine) {
        if (!(machine instanceof IConfigurationCircuitSupport)
            || !((IConfigurationCircuitSupport) machine).allowSelectCircuit()) {
            throw new IllegalArgumentException("Machine does not support virtual configuration circuits");
        }
        return new GhostCircuitSlotWidget(machine, syncManager);
    }

    /** The title is decorative and deliberately omitted from the server widget tree. */
    public static Widget<?> createMachineTitle(IMetaTileEntity mte, int panelWidth) {
        if (!NetworkUtils.isClient()) {
            return null;
        }
        String title = mte.getLocalName();
        byte colorization = mte.getBaseMetaTileEntity().getColorization();
        boolean showSwatch = GregTech_API.sColoredGUI && colorization >= 0 && colorization < Dyes.VALUES.length;
        int swatchOuterSize = showSwatch ? 9 : 0;
        int childPadding = 4;
        int headerWidth = Math.max(1, panelWidth - 34);
        int maxWidth = Math.max(1, headerWidth - (showSwatch ? swatchOuterSize + childPadding : 0));
        FontRenderer font = TextRenderer.getFontRenderer();
        String visibleTitle = title;
        if (font.getStringWidth(title) > maxWidth) {
            String ellipsis = "\u2026";
            int availableWidth = Math.max(0, maxWidth - font.getStringWidth(ellipsis));
            visibleTitle = font.trimStringToWidth(title, availableWidth) + ellipsis;
        }

        Flow row = Flow.row()
            .pos(6, 5)
            .size(headerWidth, font.FONT_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .childPadding(childPadding);
        if (showSwatch) {
            Dyes dye = Dyes.get(colorization);
            short[] rgba = dye.mRGBa;
            int color = 0xff000000 | rgba[0] << 16 | rgba[1] << 8 | rgba[2];
            row.child(new ParentWidget<>()
                .size(swatchOuterSize, swatchOuterSize)
                .widgetTheme(GTWidgetThemes.BACKGROUND_COLOR_SWATCH)
                .child(new Widget<>()
                    .size(7, 7)
                    .pos(1, 1)
                    .background(new Rectangle().color(color))
                    .tooltip(tooltip -> tooltip.add(dye.mName))));
        }
        row.child(IKey.str(visibleTitle)
            .asWidget()
            .size(maxWidth, font.FONT_HEIGHT)
            .widgetTheme(GTWidgetThemes.TEXT_TITLE));
        return row;
    }

    public static ParentWidget<?> createFluidScreen(int width, int height) {
        return new ParentWidget<>()
            .size(width, height)
            .padding(3, 2, 3, 2)
            .background(GTGuiTextures.PICTURE_SCREEN_BLACK);
    }

    public static Flow createInvertButtonRow(BooleanSyncValue invertedSyncer) {
        return Flow.row()
            .child(new ToggleButton()
                .value(invertedSyncer)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                .size(16))
            .child(IKey.dynamic(() -> invertedSyncer.getValue()
                ? StatCollector.translateToLocal("gt.interact.desc.inverted")
                : StatCollector.translateToLocal("gt.interact.desc.normal")).asWidget())
            .coverChildren()
            .childPadding(2);
    }
}
