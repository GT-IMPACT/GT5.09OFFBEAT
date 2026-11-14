package gregtech.common.gui.modularui.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;

import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;
import gregtech.common.items.toolbox.ToolboxUtil;

public final class ToolboxContainer extends ModularContainer {

    private final PlayerInventoryGuiData data;
    private final ToolboxItemStackHandler contents;
    private boolean saved;

    public ToolboxContainer(PlayerInventoryGuiData data, ToolboxItemStackHandler contents) {
        this.data = data;
        this.contents = contents;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        saveContents();
    }

    @Override
    public void onModularContainerOpened() {
        saved = false;
    }

    public void saveContents() {
        EntityPlayer player = data.getPlayer();
        if (player.worldObj.isRemote || saved) {
            return;
        }

        ItemStack toolbox = data.getUsedItemStack();
        if (toolbox == null || !(toolbox.getItem() instanceof ItemGTToolbox)) {
            return;
        }
        ToolboxUtil.saveToolbox(toolbox, contents, tag -> {
            tag.removeTag(ItemGTToolbox.TOOLBOX_OPEN_KEY);
            tag.removeTag(ItemGTToolbox.BROKEN_TOOL_ANIMATION_END_KEY);
            tag.removeTag(ItemGTToolbox.RECENTLY_BROKEN_SLOT_KEY);
        });
        data.setUsedItemStack(toolbox);
        saved = true;
        ItemGTToolbox.playSound(player, "Close", 1.0F);
    }
}
