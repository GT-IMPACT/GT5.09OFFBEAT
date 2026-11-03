package gregtech.api.modularui2;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.integration.nei.NEIUtil;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;

import codechicken.nei.recipe.CheatItemHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

/**
 * This class is responsible for client-only functionalities of the GUI. Override {@link IGuiHolder#createScreen} to
 * use.
 */
@SideOnly(Side.CLIENT)
public class GTModularScreen extends ModularScreen {

    private final CheatItemHandler neiCheatHandler = new CheatItemHandler();

    public GTModularScreen(@NotNull ModularPanel mainPanel, GTGuiTheme theme) {
        super("gregtech", mainPanel);
        useTheme(theme.getId());
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        if (ModularUI.Mods.NEI.isLoaded()) {
            ItemStack draggedStack = NEIUtil.getNEIDragAndDropTarget(getContext());
            IWidget hovered = getContext().getTopHovered();
            // MUI2 consumes NEI ghost drags before NEI can process ordinary inventory slots.
            if (draggedStack != null && hovered instanceof IVanillaSlot
                && ((IVanillaSlot) hovered).handleAsVanillaSlot()
                && neiCheatHandler.handleDragNDrop(
                    (GuiContainer) getScreenWrapper().getGuiScreen(),
                    getContext().getMouseX(),
                    getContext().getMouseY(),
                    draggedStack,
                    mouseButton
                )) {
                NEIUtil.stopNEIGhostDrag();
                getContext().removeFocus();
                return true;
            }
        }
        return super.onMousePressed(mouseButton);
    }
}

