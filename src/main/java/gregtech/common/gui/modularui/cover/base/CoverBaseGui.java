package gregtech.common.gui.modularui.cover.base;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.covers.Cover;
import gregtech.common.modularui2.widget.CoverTickRateButton;
import net.minecraft.item.ItemStack;

import java.util.Arrays;

/** GTNH cover screen template shared by standalone windows and machine cover popups. */
public abstract class CoverBaseGui<T extends Cover> {

    protected static final int WIDGET_MARGIN = 5;
    protected static final int ROW_PADDING = 3;
    protected static final int ROW_ELEMENT_PADDING = 2;
    protected static final int TICK_RATE_BUTTON_SIZE = 20;

    protected final T cover;

    public CoverBaseGui(T cover) {
        this.cover = cover;
    }

    /** Stable resource-pack identifier, conventionally cover.snake_case. */
    protected abstract String getGuiId();

    /** Setters must also check cover.isValid(), since a packet can arrive before the next close check. */
    public abstract void addUIWidgets(PanelSyncManager syncManager, Flow column, CoverGuiData data);

    public final ModularPanel createStandalonePanel(
        PanelSyncManager syncManager,
        UISettings settings,
        CoverGuiData data
    ) {
        return createBasePanel(syncManager, settings, data).coverChildren();
    }

    public final ModularPanel createPopUpPanel(
        PanelSyncManager syncManager,
        UISettings settings,
        CoverGuiData data,
        IWidget parent
    ) {
        ModularPanel panel = createBasePanel(syncManager, settings, data).coverChildren();
        if (positionRelativeToCoverButton()) {
            panel.relative(parent).right(24);
        }
        return panel;
    }

    protected ModularPanel createBasePanel(PanelSyncManager syncManager, UISettings settings, CoverGuiData data) {
        ModularPanel panel = new ModularPanel(getGuiId() + "." + data.getSide());
        if (data.isPopUp()) {
            panel.child(ButtonWidget.panelCloseButton());
        }
        syncManager.addCloseListener(player -> markDirty());
        syncManager.onServerTick(() -> {
            if (!cover.isValid()) {
                panel.closeIfOpen();
            }
        });
        Flow mainColumn = Flow.column()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .margin(WIDGET_MARGIN);
        panel.child(mainColumn);

        Flow header = Flow.row()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .marginBottom(4)
            .marginRight(data.isPopUp() ? 12 : 0)
            .coverChildren();
        addTitleToUI(header, data);
        mainColumn.child(header);

        Flow widgets = Flow.column()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .coverChildren();
        addUIWidgets(syncManager, widgets, data);
        mainColumn.child(widgets);
        if (cover.getMinimumTickRate() > 0 && cover.allowsTickRateAddition()) {
            panel.child(new CoverTickRateButton(cover, syncManager).right(4).bottom(4));
        }
        if (!data.isPopUp() && doesBindPlayerInventory()) {
            mainColumn.child(SlotGroupWidget.playerInventory(false).marginTop(4));
        }
        return panel;
    }

    protected void addTitleToUI(Flow title, CoverGuiData data) {
        ItemStack stack = cover.asItemStack();
        if (stack == null) {
            return;
        }
        title.child(new ItemDrawable(stack).asWidget().size(16));
        if (!data.isPopUp() || shouldIncludeTitleInPopUp()) {
            title.child(IKey.str(stack.getDisplayName())
                .asWidget()
                .marginLeft(4)
                .verticalCenter()
                .widgetTheme(GTWidgetThemes.TEXT_TITLE));
        }
    }

    protected Flow makeRowLayout() {
        return Flow.column()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .marginLeft(WIDGET_MARGIN)
            .childPadding(ROW_PADDING);
    }

    protected int getStringMaxWidth(String... strings) {
        if (FMLCommonHandler.instance().getSide().isServer()) {
            return 100;
        }
        return IKey.renderer.getMaxWidth(Arrays.asList(strings));
    }

    protected Flow positionRow(Flow row) {
        return row.coverChildren().childPadding(ROW_ELEMENT_PADDING);
    }

    protected Flow makeNamedColumn(IKey name) {
        return Flow.column()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(name.asWidget());
    }

    protected TextFieldWidget makeNumberField(int width) {
        return new TextFieldWidget().setFormatAsInteger(true).width(width).height(12);
    }

    protected TextFieldWidget makeNumberField() {
        return makeNumberField(80);
    }

    protected boolean doesBindPlayerInventory() {
        return false;
    }

    protected boolean positionRelativeToCoverButton() {
        return false;
    }

    protected boolean shouldIncludeTitleInPopUp() {
        return true;
    }

    private void markDirty() {
        ICoverable tile = cover.getTile();
        if (tile != null && tile.getWorld() != null && tile.isServerSide()) {
            tile.markDirty();
            tile.getWorld().getChunkFromBlockCoords(tile.getXCoord(), tile.getZCoord()).setChunkModified();
        }
    }
}
