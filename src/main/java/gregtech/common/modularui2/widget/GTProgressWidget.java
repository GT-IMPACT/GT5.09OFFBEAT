package gregtech.common.modularui2.widget;

import codechicken.nei.recipe.GuiCraftingRecipe;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.common.modularui2.theme.ProgressbarWidgetTheme;
import net.minecraft.util.StatCollector;

/**
 * GTNH's themed progress widget and NEI recipe transfer area, adapted to this recipe map API.
 * Source: GTNewHorizons/GT5-Unofficial, f0472cfbd44ab9c8d5a27df7e98c64a42eb1c03f.
 */
public class GTProgressWidget extends ProgressWidget implements Interactable {

    private String transferRectId;
    private Object[] transferRectArguments = new Object[0];

    public GTProgressWidget neiTransferRect(String id, Object[] arguments, String tooltip) {
        transferRectId = id;
        transferRectArguments = arguments == null ? new Object[0] : arguments;
        if (tooltip != null) {
            tooltipBuilder(builder -> builder.addLine(tooltip));
        }
        return this;
    }

    public GTProgressWidget neiTransferRect(String id) {
        return neiTransferRect(id, null, StatCollector.translateToLocal("nei.recipe.tooltip"));
    }

    public GTProgressWidget neiTransferRect(GT_Recipe_Map recipeMap) {
        return neiTransferRect(recipeMap.mNEIName);
    }

    @Override
    public void onInit() {
        WidgetThemeEntry<?> themeEntry = getWidgetTheme(getContext().getTheme());
        if (themeEntry.getTheme() instanceof ProgressbarWidgetTheme) {
            ProgressbarWidgetTheme theme = (ProgressbarWidgetTheme) themeEntry.getTheme();
            texture(theme.getEmptyTexture(), theme.getFullTexture(), theme.getImageSize());
        }
        super.onInit();
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (mouseButton == 0 && transferRectId != null && !transferRectId.isEmpty()) {
            GuiCraftingRecipe.openRecipeGui(transferRectId, transferRectArguments);
            return Result.SUCCESS;
        }
        return Interactable.super.onMousePressed(mouseButton);
    }
}
