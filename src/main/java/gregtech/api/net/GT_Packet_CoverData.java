package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.network.ByteBufUtils;
import gregtech.api.metatileentity.CoverableTileEntity;
import gregtech.common.covers.Cover;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Server-to-client cover state; a matching side and cover ID are required when applying it. */
public final class GT_Packet_CoverData extends GT_Packet {

    private int dimension;
    private int x;
    private int y;
    private int z;
    private byte side;
    private int coverID;
    private NBTTagCompound state;

    public GT_Packet_CoverData() {
        super(true);
    }

    public GT_Packet_CoverData(CoverableTileEntity tile, Cover cover) {
        super(false);
        dimension = tile.getWorld().provider.dimensionId;
        x = tile.getXCoord();
        y = tile.getYCoord();
        z = tile.getZCoord();
        side = (byte) cover.getSide().ordinal();
        coverID = cover.getCoverID();
        state = cover.writeToNBT(new NBTTagCompound());
    }

    @Override
    public byte getPacketID() {
        return 11;
    }

    @Override
    public byte[] encode() {
        ByteArrayDataOutput data = ByteStreams.newDataOutput();
        data.writeInt(dimension);
        data.writeInt(x);
        data.writeInt(y);
        data.writeInt(z);
        data.writeByte(side);
        data.writeInt(coverID);
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufUtils.writeTag(buffer, state);
            byte[] payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            data.writeInt(payload.length);
            data.write(payload);
        } finally {
            buffer.release();
        }
        return data.toByteArray();
    }

    @Override
    public GT_Packet decode(ByteArrayDataInput data) {
        GT_Packet_CoverData packet = new GT_Packet_CoverData();
        packet.dimension = data.readInt();
        packet.x = data.readInt();
        packet.y = data.readInt();
        packet.z = data.readInt();
        packet.side = data.readByte();
        packet.coverID = data.readInt();
        int length = data.readInt();
        if (length < 0 || length > 32769) {
            throw new IllegalArgumentException("Invalid cover packet size: " + length);
        }
        byte[] payload = new byte[length];
        data.readFully(payload);
        ByteBuf buffer = Unpooled.wrappedBuffer(payload);
        try {
            packet.state = ByteBufUtils.readTag(buffer);
        } finally {
            buffer.release();
        }
        return packet;
    }

    @Override
    public void process(IBlockAccess access) {
        if (!(access instanceof World)) {
            return;
        }
        World world = (World) access;
        if (!world.isRemote || world.provider.dimensionId != dimension || side < 0 || side >= 6
            || state == null || !world.blockExists(x, y, z)) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof CoverableTileEntity) {
            ((CoverableTileEntity) tile).updateAttachedCover(coverID, ForgeDirection.getOrientation(side), state);
        }
    }
}
