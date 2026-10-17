package gregtech.common.items;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

public class ItemDebug extends Item {

    private static final ItemDebug DebugItem = new ItemDebug();

    private ItemDebug() {
        // I am a singleton
    }

    public static ItemDebug getInstance() {
        return DebugItem;
    }

    /** Keep the failed lookup on the stack so NEI can explain the broken recipe. */
    public static ItemStack missing(String source, long amount) {
        ItemStack stack = new ItemStack(DebugItem, (int) Math.max(0, Math.min(64, amount)));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("MissingItem", source);
        tag.setLong("RequestedAmount", amount);
        stack.setTagCompound(tag);
        return stack;
    }

    public void registerItem() {
        super.setHasSubtypes(false);
        final String unlocalizedName = "debug_item";
        super.setUnlocalizedName(unlocalizedName);
        super.setMaxStackSize(64);
        super.setTextureName("gregtech" + ":" + "Error");
        GameRegistry.registerItem(getInstance(), unlocalizedName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean b) {
        list.add(StatCollector.translateToLocal("item.debug_item.description"));
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("MissingItem")) {
            list.add(StatCollector.translateToLocal("item.debug_item.missing") + " " + tag.getString("MissingItem"));
            list.add(StatCollector.translateToLocal("item.debug_item.amount") + " " + tag.getLong("RequestedAmount"));
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack item, World world, EntityPlayer player) {
        player.swingItem();
        return item;
    }
}
