package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import gregtech.common.GT_Client;
import gregtech.common.GT_Pollution;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;

public class GT_Packet_Pollution extends GT_Packet_New {
    private int pollution;

    public GT_Packet_Pollution() {
        super(true);
    }

    public GT_Packet_Pollution(int pollution) {
        super(false);
        this.pollution = pollution;
    }

    @Override
    public void encode(ByteBuf aOut) {
        aOut.writeInt(pollution);
    }

    @Override
    public GT_Packet_New decode(ByteArrayDataInput aData) {
        return new GT_Packet_Pollution(aData.readInt());
    }

    @Override
    public void process(IBlockAccess aWorld) {
        GT_Pollution.mPlayerPollution = pollution;
    }

    @Override
    public byte getPacketID() {
        return 4;
    }
}