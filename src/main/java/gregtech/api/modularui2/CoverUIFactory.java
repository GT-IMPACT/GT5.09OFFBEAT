package gregtech.api.modularui2;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.common.covers.Cover;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

/** GTNH cover factory with host identity, distance and access validation. */
public final class CoverUIFactory extends AbstractUIFactory<CoverGuiData> {

    public static final CoverUIFactory INSTANCE = new CoverUIFactory();

    private CoverUIFactory() {
        super("gregtech:cover");
    }

    public void open(EntityPlayerMP player, int coverID, ICoverable tile, ForgeDirection side) {
        if (player == null || tile == null || player instanceof FakePlayer || player.worldObj != tile.getWorld()) {
            return;
        }
        CoverGuiData data = new CoverGuiData(
                player,
                coverID,
                tile.getXCoord(),
                tile.getYCoord(),
                tile.getZCoord(),
                side,
                false
        );
        if (canInteractWith(player, data)) {
            GuiManager.open(this, data, player);
        }
    }

    @Override
    public IGuiHolder<CoverGuiData> getGuiHolder(CoverGuiData data) {
        return data.getCover();
    }

    @Override
    public ModularPanel createPanel(CoverGuiData data, PanelSyncManager syncManager, UISettings settings) {
        Cover cover = data.getCover();
        ICoverable tile = data.getCoverable();
        ModularPanel panel = cover.buildUI(data, syncManager, settings);
        settings.canInteractWith(player -> canInteractWith(player, data)
                && data.getTileEntity() == tile && tile.getCoverAtSide(data.getSide()) == cover);
        return panel;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, CoverGuiData data) {
        if (player == null || player instanceof FakePlayer || !super.canInteractWith(player, data)
                || player.worldObj != data.getWorld()
                || data.getSquaredDistance(player) >= MetaTileEntityGuiHandler.MAX_INTERACTION_DISTANCE) {
            return false;
        }
        TileEntity tile = data.getTileEntity();
        if (!(tile instanceof ICoverable)) {
            return false;
        }
        ICoverable host = (ICoverable) tile;
        Cover cover = host.getCoverAtSide(data.getSide());
        return cover.getCoverID() == data.getCoverID() && cover.useMui2()
                && cover.hasCoverGUI() && host.isUseableByPlayer(player);
    }

    @Override
    public void writeGuiData(CoverGuiData data, PacketBuffer buffer) {
        buffer.writeVarIntToBuffer(data.getX());
        buffer.writeVarIntToBuffer(data.getY());
        buffer.writeVarIntToBuffer(data.getZ());
        buffer.writeInt(data.getCoverID());
        buffer.writeByte(data.getSide().ordinal());
    }

    @Override
    public CoverGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        int x = buffer.readVarIntFromBuffer();
        int y = buffer.readVarIntFromBuffer();
        int z = buffer.readVarIntFromBuffer();
        int coverID = buffer.readInt();
        ForgeDirection side = ForgeDirection.getOrientation(buffer.readUnsignedByte());
        return new CoverGuiData(
                player,
                coverID,
                x,
                y,
                z,
                side,
                false
        );
    }
}
