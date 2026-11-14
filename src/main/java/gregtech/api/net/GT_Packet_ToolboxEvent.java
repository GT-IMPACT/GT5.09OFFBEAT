package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularContainer;
import gregtech.api.enums.ToolboxSlot;
import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.toolbox.ToolboxUtil;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class GT_Packet_ToolboxEvent extends GT_Packet {

    public static final byte PACKET_ID = 13;
    private static final int UI_OPEN = -2;
    private static final Map<EntityPlayerMP, GT_Packet_ToolboxEvent> requests = new ConcurrentHashMap<>();

    private final int inventorySlot;
    private final int selection;

    public GT_Packet_ToolboxEvent() {
        super(true);
        inventorySlot = -1;
        selection = ItemGTToolbox.NO_TOOL_SELECTED;
    }

    public GT_Packet_ToolboxEvent(int inventorySlot, int selection) {
        super(false);
        this.inventorySlot = inventorySlot;
        this.selection = selection;
    }

    public static GT_Packet_ToolboxEvent inventoryOpened(int inventorySlot) {
        return new GT_Packet_ToolboxEvent(inventorySlot, UI_OPEN);
    }

    @Override
    public byte getPacketID() {
        return PACKET_ID;
    }

    @Override
    public byte[] encode() {
        return new byte[] { (byte) inventorySlot, (byte) selection };
    }

    @Override
    public GT_Packet decode(ByteArrayDataInput input) {
        return new GT_Packet_ToolboxEvent(input.readByte(), input.readByte());
    }

    @Override
    public void process(IBlockAccess world) {
        // Toolbox events are applied on the player's server tick.
    }

    public void queueEvent(EntityPlayerMP player) {
        if (inventorySlot >= 0 && inventorySlot < 9
            && (selection == UI_OPEN || selection == ItemGTToolbox.NO_TOOL_SELECTED || ToolboxSlot.slotIsTool(selection))) {
            requests.put(player, this);
        }
    }

    public static void applyEvent(EntityPlayerMP player) {
        GT_Packet_ToolboxEvent request = requests.remove(player);
        if (request != null) {
            if (request.selection == UI_OPEN) {
                request.markInventoryOpen(player);
            } else {
                request.select(player);
            }
        }
    }

    public static void clearEvents() {
        requests.clear();
    }

    private void markInventoryOpen(EntityPlayerMP player) {
        if (player.isDead || !(player.openContainer instanceof ModularContainer)) {
            return;
        }
        ModularContainer container = (ModularContainer) player.openContainer;
        if (!(container.getGuiData() instanceof PlayerInventoryGuiData)) {
            return;
        }
        PlayerInventoryGuiData data = (PlayerInventoryGuiData) container.getGuiData();
        if (data.getPlayer() != player || data.getInventoryType() != InventoryTypes.PLAYER
            || data.getSlotIndex() != inventorySlot || !container.getSyncManager().isOpen()) {
            return;
        }
        ItemStack toolbox = data.getUsedItemStack();
        if (toolbox == null || !(toolbox.getItem() instanceof ItemGTToolbox) || ItemGTToolbox.isInventoryOpen(toolbox)) {
            return;
        }
        NBTTagCompound tag = toolbox.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            toolbox.setTagCompound(tag);
        }
        tag.setBoolean(ItemGTToolbox.TOOLBOX_OPEN_KEY, true);
        ItemGTToolbox.playSound(player, "Open", 2.0F);
    }

    private void select(EntityPlayerMP player) {
        if (player.isDead || player.inventory.currentItem != inventorySlot) {
            return;
        }

        ItemStack toolbox = player.inventory.getCurrentItem();
        if (toolbox == null || !(toolbox.getItem() instanceof ItemGTToolbox)
            || ItemGTToolbox.isInventoryOpen(toolbox)) {
            return;
        }
        NBTTagCompound tag = toolbox.getTagCompound();
        if (tag == null) {
            return;
        }

        if (selection == ItemGTToolbox.NO_TOOL_SELECTED) {
            if (tag.hasKey(ItemGTToolbox.CURRENT_TOOL_KEY)) {
                tag.removeTag(ItemGTToolbox.CURRENT_TOOL_KEY);
                ItemGTToolbox.playSound(player, "Close", 1);
            }
        } else {
            ToolboxSlot slot = ToolboxSlot.getBySlot(selection).get();
            if (ToolboxUtil.getItemInside(toolbox, slot).filter(slot::test).isPresent()
                && (!tag.hasKey(ItemGTToolbox.CURRENT_TOOL_KEY)
                    || tag.getInteger(ItemGTToolbox.CURRENT_TOOL_KEY) != selection)) {
                tag.setInteger(ItemGTToolbox.CURRENT_TOOL_KEY, selection);
                ItemGTToolbox.playSound(player, "Draw", ThreadLocalRandom.current().nextFloat() * 0.3F + 0.7F);
            }
        }
    }
}
