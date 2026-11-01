package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.network.ByteBufUtils;
import gregtech.api.enums.GT_Values;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Server-to-client snapshot of the outputs of the running cycle, scoped to an
 * open window.
 */
public final class GT_Packet_ProcessingOutputs extends GT_Packet {
    private int windowId;
    private int dimension;
    private int x;
    private int y;
    private int z;
    private NBTTagCompound outputs;

    public GT_Packet_ProcessingOutputs() {
        super(true);
    }

    public GT_Packet_ProcessingOutputs(GT_Container_ProcessingMachine container, NBTTagCompound outputs) {
        super(false);
        windowId = container.windowId;
        dimension = container.mTileEntity.getWorld().provider.dimensionId;
        x = container.mTileEntity.getXCoord();
        y = container.mTileEntity.getYCoord();
        z = container.mTileEntity.getZCoord();
        this.outputs = (NBTTagCompound) outputs.copy();
    }

    @Override
    public byte getPacketID() {
        return 10;
    }

    @Override
    public byte[] encode() {
        ByteArrayDataOutput data = ByteStreams.newDataOutput();
        data.writeInt(windowId);
        data.writeInt(dimension);
        data.writeInt(x);
        data.writeInt(y);
        data.writeInt(z);
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufUtils.writeTag(buffer, outputs);
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
        GT_Packet_ProcessingOutputs packet = new GT_Packet_ProcessingOutputs();
        packet.windowId = data.readInt();
        packet.dimension = data.readInt();
        packet.x = data.readInt();
        packet.y = data.readInt();
        packet.z = data.readInt();
        int length = data.readInt();
        if (length < 0 || length > 32769) {
            throw new IllegalArgumentException("Invalid processing output packet size: " + length);
        }
        byte[] payload = new byte[length];
        data.readFully(payload);
        ByteBuf buffer = Unpooled.wrappedBuffer(payload);
        try {
            packet.outputs = ByteBufUtils.readTag(buffer);
        } finally {
            buffer.release();
        }
        return packet;
    }

    @Override
    public void process(IBlockAccess world) {
        if (!(world instanceof World) || !((World) world).isRemote) {
            return;
        }
        EntityPlayer player = GT_Values.GT.getThePlayer();
        if (player != null && player.openContainer instanceof GT_Container_ProcessingMachine) {
            applyTo((GT_Container_ProcessingMachine) player.openContainer);
        }
    }

    public void applyTo(GT_Container_ProcessingMachine container) {
        if (outputs != null && container.windowId == windowId
                && container.mTileEntity.getWorld().provider.dimensionId == dimension
                && container.mTileEntity.getXCoord() == x && container.mTileEntity.getYCoord() == y
                && container.mTileEntity.getZCoord() == z) {
            container.receiveOutputs(outputs);
        }
    }
}
