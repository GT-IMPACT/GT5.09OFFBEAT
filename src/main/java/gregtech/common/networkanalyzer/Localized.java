package gregtech.common.networkanalyzer;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.util.GT_Utility;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.StatCollector;

import java.nio.charset.StandardCharsets;

/** Translates analyzer labels on the receiving client. */
public final class Localized {

    private final String key;
    private final long[] args;

    public Localized(String key, long... args) {
        this.key = key;
        this.args = args;
    }

    public void encode(ByteBuf out) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.writeBytes(bytes);
        out.writeByte(args.length);
        for (long arg : args) {
            out.writeLong(arg);
        }
    }

    public static Localized decode(ByteArrayDataInput in) {
        byte[] bytes = new byte[in.readUnsignedShort()];
        in.readFully(bytes);
        long[] args = new long[in.readUnsignedByte()];
        for (int i = 0; i < args.length; i++) {
            args[i] = in.readLong();
        }
        return new Localized(new String(bytes, StandardCharsets.UTF_8), args);
    }

    @Override
    public String toString() {
        Object[] formatted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            formatted[i] = GT_Utility.formatNumbers(args[i]);
        }
        return StatCollector.translateToLocalFormatted(key, formatted);
    }
}
