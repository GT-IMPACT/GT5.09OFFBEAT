package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.network.play.server.S2DPacketOpenWindow;

public class CoverCrafting extends CoverLegacyData {

    public CoverCrafting(CoverContext context) {
        super(context);
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        ICoverable tile = getTile();
        if ((player instanceof EntityPlayerMP)) {
            ((EntityPlayerMP) player).getNextWindowId();
            ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(
                    new S2DPacketOpenWindow(((EntityPlayerMP) player).currentWindowId, 1, "Crafting", 9, true));
            ((EntityPlayerMP) player).openContainer = new ContainerWorkbench(
                ((EntityPlayerMP) player).inventory,
                ((EntityPlayerMP) player).worldObj,
                tile.getXCoord(),
                tile.getYCoord(),
                tile.getZCoord()
            ) {

                public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
                    return true;
                }
            };
            ((EntityPlayerMP) player).openContainer.windowId = ((EntityPlayerMP) player).currentWindowId;
            ((EntityPlayerMP) player).openContainer.addCraftingToCrafters((EntityPlayerMP) player);
        }
        return true;
    }
}
