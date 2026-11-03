package gregtech.common.net;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gregtech.api.net.GT_Packet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;

public final class MessageSetFlaskCapacity extends GT_Packet {
    private int capacity, dimID, playerID;

    public MessageSetFlaskCapacity() {
        super(true);
    }

    public MessageSetFlaskCapacity(int capacity, int dimID, int playerID) {
        super(false);
        this.capacity = capacity;
        this.dimID = dimID;
        this.playerID = playerID;
    }

    public MessageSetFlaskCapacity(int capacity, EntityPlayer p) {
        super(false);
        this.capacity = capacity;
        this.dimID = p.worldObj.provider.dimensionId;
        this.playerID = p.getEntityId();
    }

    @Override
    public byte getPacketID() {
        return 5;
    }

    @Override
    public byte[] encode() {
        ByteArrayDataOutput tOut = ByteStreams.newDataOutput(10);
        tOut.writeInt(capacity);
        tOut.writeInt(dimID);
        tOut.writeInt(playerID);
        return tOut.toByteArray();
    }

    @Override
    public GT_Packet decode(ByteArrayDataInput aData) {
        return new MessageSetFlaskCapacity(aData.readInt(), aData.readInt(), aData.readInt());
    }

    @Override
    public void process(IBlockAccess aWorld) {
        // Keep packet ID 5 reserved; the flask now uses its MUI2 inventory GUI synchronization.
    }
}
