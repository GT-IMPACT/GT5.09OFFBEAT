package gregtech.common.items;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.items.GT_Generic_Item;
import gregtech.common.gui.modularui.item.RedstoneSnifferGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemRedstoneSniffer extends GT_Generic_Item implements IGuiHolder<PlayerInventoryGuiData> {

    public ItemRedstoneSniffer() {
        super("Item_Redstone_Sniffer", "Redstone Sniffer", "What are these frequencies?!");
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            GuiFactories.playerInventory().openFromMainHand(player);
        }
        return super.onItemRightClick(stack, world, player);
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new RedstoneSnifferGui(data, syncManager).build();
    }
}
