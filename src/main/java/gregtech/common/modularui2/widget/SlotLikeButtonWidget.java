package gregtech.common.modularui2.widget;

import codechicken.nei.guihook.GuiContainerManager;
import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerIngredientProvider;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.SlotTheme;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.function.Supplier;

/** GTNH item selection button with item tooltips, slot highlighting and NEI ingredient interaction. */
public class SlotLikeButtonWidget extends ButtonWidget<SlotLikeButtonWidget>
    implements RecipeViewerIngredientProvider {

    private static final InventoryBasic DUMMY_INVENTORY = new InventoryBasic("", false, 1);
    private static final Slot NEI_DUMMY_SLOT = new Slot(DUMMY_INVENTORY, 0, 1, 1);

    private final Supplier<ItemStack> itemSupplier;
    private final IDrawable itemDrawable;

    public SlotLikeButtonWidget(ItemStack itemStack) {
        this(() -> itemStack);
    }

    public SlotLikeButtonWidget(Supplier<ItemStack> itemSupplier) {
        this.itemSupplier = itemSupplier;
        this.itemDrawable = new DynamicDrawable(() -> new ItemDrawable(itemSupplier.get()).asIcon());
        disableHoverBackground();
        size(18);
    }

    @Override
    public WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getItemSlotTheme();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        renderSlotDecoration(false);
        itemDrawable.drawAtZero(context, getArea(), widgetTheme.getTheme());
        renderSlotDecoration(true);
        if (isHovering()) {
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.colorMask(true, true, true, false);
            GuiDraw.drawRect(
                1,
                1,
                16,
                16,
                getHoverColor(widgetTheme.getTheme())
            );
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.disableBlend();
        }
    }

    private void renderSlotDecoration(boolean overlay) {
        if (!ModularUI.Mods.NEI.isLoaded()) {
            return;
        }
        GuiContainerManager manager = GuiContainerManager.getManager();
        GuiScreen screen = getScreen().getScreenWrapper().getGuiScreen();
        if (manager != null && screen instanceof GuiContainer && manager.window == screen) {
            NEI_DUMMY_SLOT.putStack(itemSupplier.get());
            if (overlay) {
                manager.renderSlotOverlay(NEI_DUMMY_SLOT);
            } else {
                manager.renderSlotUnderlay(NEI_DUMMY_SLOT);
            }
        }
    }

    private int getHoverColor(WidgetTheme widgetTheme) {
        if (widgetTheme instanceof SlotTheme) {
            return ((SlotTheme) widgetTheme).getSlotHoverColor();
        }
        return ITheme.getDefault().getItemSlotTheme().getTheme().getSlotHoverColor();
    }

    @Override
    public ItemStack getStackForRecipeViewer() {
        return itemSupplier.get();
    }
}
