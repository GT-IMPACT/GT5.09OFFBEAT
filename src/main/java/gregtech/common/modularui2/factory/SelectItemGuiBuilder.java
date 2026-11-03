package gregtech.common.modularui2.factory;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.modularui2.sync.SelectItemServerAction;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** GTNH selection popup template shared by circuit configuration and other item choice screens. */
public class SelectItemGuiBuilder {

    public static final int DESELECTED = -1;
    private static final int COLUMNS = 9;
    private static final int GUI_WIDTH = 18 * COLUMNS + 14;

    private final ModularPanel panel;
    private final List<ItemStack> choices;

    private ItemStack headerItem;
    private IKey title;
    private int selected;
    private IntSyncValue selectedSyncHandler;
    private SelectItemServerAction onSelectedServerAction;
    private IOnSelectedAction onSelectedClientAction;
    private IDrawable currentItemSlotOverlay;
    private Consumer<SlotLikeButtonWidget> currentItemWidgetCustomizer;
    private BiConsumer<Integer, SlotLikeButtonWidget> choiceWidgetCustomizer;
    private boolean allowDeselected;
    private boolean built;

    public SelectItemGuiBuilder(ModularPanel panel, List<ItemStack> choices) {
        this.panel = panel;
        this.choices = new ArrayList<>(choices);
    }

    public SelectItemGuiBuilder setHeaderItem(ItemStack headerItem) {
        this.headerItem = headerItem;
        return this;
    }

    public SelectItemGuiBuilder setTitle(IKey title) {
        this.title = title;
        return this;
    }

    public SelectItemGuiBuilder setSelected(int selected) {
        if (selected < DESELECTED || selected >= choices.size()
            || built && !allowDeselected && selected == DESELECTED) {
            return this;
        }
        this.selected = selected;
        return this;
    }

    public SelectItemGuiBuilder setSelectedSyncHandler(IntSyncValue selectedSyncHandler) {
        this.selectedSyncHandler = selectedSyncHandler;
        return this;
    }

    public SelectItemGuiBuilder setOnSelectedServerAction(SelectItemServerAction onSelectedServerAction) {
        this.onSelectedServerAction = onSelectedServerAction;
        return this;
    }

    public SelectItemGuiBuilder setOnSelectedClientAction(IOnSelectedAction onSelectedClientAction) {
        this.onSelectedClientAction = onSelectedClientAction;
        return this;
    }

    public SelectItemGuiBuilder setCurrentItemSlotOverlay(IDrawable currentItemSlotOverlay) {
        this.currentItemSlotOverlay = currentItemSlotOverlay;
        return this;
    }

    public SelectItemGuiBuilder setCurrentItemWidgetCustomizer(Consumer<SlotLikeButtonWidget> customizer) {
        this.currentItemWidgetCustomizer = customizer;
        return this;
    }

    public SelectItemGuiBuilder setChoiceWidgetCustomizer(BiConsumer<Integer, SlotLikeButtonWidget> customizer) {
        this.choiceWidgetCustomizer = customizer;
        return this;
    }

    public SelectItemGuiBuilder setAllowDeselected(boolean allowDeselected) {
        this.allowDeselected = allowDeselected;
        return this;
    }

    public ModularPanel build() {
        if (built) {
            throw new IllegalStateException("Selection popup was already built");
        }
        if (choices.isEmpty()) {
            throw new IllegalStateException("Choices must not be empty");
        }
        if (onSelectedServerAction != null) {
            onSelectedServerAction.setSelectionBounds(choices.size(), allowDeselected);
        }
        int rows = (choices.size() - 1) / COLUMNS + 1;
        panel.size(GUI_WIDTH, 53 + 18 * rows);
        if (headerItem != null || title != null) {
            Flow header = Flow.row().coverChildren().childPadding(4).pos(5, 5);
            if (headerItem != null) {
                header.child(new ItemDrawable(headerItem).asWidget().size(16));
            }
            if (title != null) {
                header.child(title.asWidget());
            }
            panel.child(header);
        }
        panel.child(IKey.lang("GT5U.gui.select.current")
            .asWidget()
            .leftRel(0.5f, -12, 1)
            .height(18)
            .top(22));
        SlotLikeButtonWidget currentItem = new SlotLikeButtonWidget(() -> selected >= 0 ? choices.get(selected) : null)
            .background(GTGuiTextures.SLOT_ITEM_DARK, new DynamicDrawable(() -> currentItemSlotOverlay != null
                ? currentItemSlotOverlay : IDrawable.EMPTY))
            .playClickSound(false)
            .onMousePressed(mouseButton -> true)
            .horizontalCenter()
            .top(22);
        if (currentItemWidgetCustomizer != null) {
            currentItemWidgetCustomizer.accept(currentItem);
        }
        panel.child(currentItem);

        List<List<IWidget>> choiceRows = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            List<IWidget> rowWidgets = new ArrayList<>();
            choiceRows.add(rowWidgets);
            for (int column = 0; column < COLUMNS; column++) {
                final int index = row * COLUMNS + column;
                if (index >= choices.size()) {
                    break;
                }
                SlotLikeButtonWidget choice = new SlotLikeButtonWidget(choices.get(index))
                    .background(new DynamicDrawable(() -> selected == index
                        ? GTGuiTextures.SLOT_ITEM_DARK : GTGuiTextures.SLOT_ITEM_STANDARD))
                    .onMousePressed(mouseButton -> select(index, mouseButton));
                if (choiceWidgetCustomizer != null) {
                    choiceWidgetCustomizer.accept(index, choice);
                }
                rowWidgets.add(choice);
            }
        }
        panel.child(new Grid().coverChildren().pos(7, 46).grid(choiceRows));
        if (selectedSyncHandler != null) {
            setSelected(selectedSyncHandler.getIntValue());
            selectedSyncHandler.setChangeListener(() -> setSelected(selectedSyncHandler.getIntValue()));
        }
        if (!allowDeselected && selected == DESELECTED) {
            setSelected(0);
        }
        built = true;
        return panel;
    }

    private boolean select(int index, int mouseButton) {
        if (mouseButton != 0 && mouseButton != 1) {
            return false;
        }
        if (mouseButton == 1 && !allowDeselected) {
            return true;
        }
        setSelected(mouseButton == 0 ? index : DESELECTED);
        MouseData mouseData = MouseData.create(mouseButton);
        if (onSelectedServerAction != null) {
            onSelectedServerAction.send(selected, mouseData);
        }
        if (onSelectedClientAction != null) {
            onSelectedClientAction.accept(selected, mouseData);
        }
        return true;
    }

    @FunctionalInterface
    public interface IOnSelectedAction {

        void accept(int selected, MouseData mouseData);
    }
}
