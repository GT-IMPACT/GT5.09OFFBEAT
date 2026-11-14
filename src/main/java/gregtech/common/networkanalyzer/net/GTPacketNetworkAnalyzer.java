package gregtech.common.networkanalyzer.net;

import appeng.api.util.DimensionalCoord;
import com.google.common.io.ByteArrayDataInput;
import gregtech.api.net.GT_Packet;
import gregtech.common.networkanalyzer.NetworkAnalyzerData;
import gregtech.common.networkanalyzer.events.WorldOverlayRenderer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class GTPacketNetworkAnalyzer extends GT_Packet {

    public static final byte PACKET_ID = 12;

    private final DimensionalCoord target;
    private final NetworkAnalyzerData data;

    public GTPacketNetworkAnalyzer() {
        super(true);
        target = null;
        data = null;
    }

    public GTPacketNetworkAnalyzer(DimensionalCoord target, NetworkAnalyzerData data) {
        super(false);
        this.target = target;
        this.data = data;
    }

    @Override
    public byte getPacketID() {
        return PACKET_ID;
    }

    @Override
    public byte[] encode() {
        ByteBuf out = Unpooled.buffer();
        try {
            out.writeInt(target.x);
            out.writeInt(target.y);
            out.writeInt(target.z);
            out.writeInt(target.getDimension());
            data.writeExternal(out);
            byte[] payload = new byte[out.readableBytes()];
            out.readBytes(payload);
            return payload;
        } finally {
            out.release();
        }
    }

    @Override
    public GT_Packet decode(ByteArrayDataInput in) {
        DimensionalCoord target = new DimensionalCoord(in.readInt(), in.readInt(), in.readInt(), in.readInt());
        NetworkAnalyzerData data = new NetworkAnalyzerData();
        data.readExternal(in);
        return new GTPacketNetworkAnalyzer(target, data);
    }

    @Override
    public void process(IBlockAccess access) {
        if (access instanceof World) {
            World world = (World) access;
            if (world.isRemote && world.provider.dimensionId == target.getDimension()) {
                WorldOverlayRenderer.accept(world, target, data);
            }
        }
    }
}
