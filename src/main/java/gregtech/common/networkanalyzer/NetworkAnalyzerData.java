package gregtech.common.networkanalyzer;

import java.util.ArrayList;
import java.util.List;

import com.google.common.io.ByteArrayDataInput;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;

public class NetworkAnalyzerData {

    public enum AnalyzerModes {

        TOPOLOGY,
        CRITICAL,
        ACTIVE,

        AMPERAGE,
        VOLTAGE;

        public static final AnalyzerModes[] VALUES = values();
    }

    public enum GraphNodeState {
        INPUT,
        OUTPUT,
        FORK;

        private static final GraphNodeState[] VALUES = values();
    }

    public enum SeverityLevel {
        NONE,
        SUCCESS,
        WARNING,
        ALERT;

        private static final SeverityLevel[] VALUES = values();
    }

    public static class OverlayNode {

        public final int x;
        public final int y;
        public final int z;

        public final GraphNodeState state;
        public final SeverityLevel level;

        public final Localized[] label;
        public final Localized[] tooltip;

        public OverlayNode(
            int x,
            int y,
            int z,
            GraphNodeState state,
            SeverityLevel level,
            Localized[] label,
            Localized[] tooltip
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.state = state;
            this.level = level;
            this.label = label;
            this.tooltip = tooltip;
        }

        public OverlayNode(
            TileEntity tile,
            GraphNodeState state,
            SeverityLevel level,
            Localized[] label,
            Localized[] tooltip
        ) {
            this(tile.xCoord, tile.yCoord, tile.zCoord, state, level, label, tooltip);
        }

    }

    public static class OverlayEdge {

        public final int x1;
        public final int y1;
        public final int z1;

        public final int x2;
        public final int y2;
        public final int z2;

        public final SeverityLevel level;

        public OverlayEdge(
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2,
            SeverityLevel level
        ) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.level = level;
        }

        public OverlayEdge(TileEntity tileA, TileEntity tileB, SeverityLevel level) {
            this(tileA.xCoord, tileA.yCoord, tileA.zCoord, tileB.xCoord, tileB.yCoord, tileB.zCoord, level);
        }
    }

    private static <T> T getIndexSafe(T[] values, int index) {
        return index >= 0 && index < values.length ? values[index] : null;
    }

    public final List<OverlayNode> nodes = new ArrayList<>();
    public final List<OverlayEdge> edges = new ArrayList<>();

    public void add(OverlayNode node) {
        this.nodes.add(node);
    }

    public void add(OverlayEdge edge) {
        this.edges.add(edge);
    }

    public void readExternal(ByteArrayDataInput in) {
        int nodeCount = in.readInt();
        int linkCount = in.readInt();

        for (int i = 0; i < nodeCount; i++) {
            final int x = in.readInt();
            final int y = in.readInt();
            final int z = in.readInt();
            final GraphNodeState state = getIndexSafe(GraphNodeState.VALUES, in.readByte());
            final SeverityLevel level = readSeverity(in);
            final Localized[] label = readLocalizedArray(in);
            final Localized[] tooltip = readLocalizedArray(in);

            this.nodes.add(new OverlayNode(x, y, z, state, level, label, tooltip));
        }

        for (int i = 0; i < linkCount; i++) {
            final int x1 = in.readInt();
            final int y1 = in.readInt();
            final int z1 = in.readInt();

            final int x2 = in.readInt();
            final int y2 = in.readInt();
            final int z2 = in.readInt();

            final SeverityLevel level = readSeverity(in);

            this.edges.add(new OverlayEdge(x1, y1, z1, x2, y2, z2, level));
        }
    }

    public void writeExternal(ByteBuf out) {
        out.writeInt(this.nodes.size());
        out.writeInt(this.edges.size());

        for (OverlayNode node : this.nodes) {
            out.writeInt(node.x);
            out.writeInt(node.y);
            out.writeInt(node.z);
            out.writeByte(node.state == null ? -1 : node.state.ordinal());
            out.writeByte(node.level.ordinal());

            writeLocalizedArray(out, node.label);
            writeLocalizedArray(out, node.tooltip);
        }

        for (OverlayEdge edge : this.edges) {
            out.writeInt(edge.x1);
            out.writeInt(edge.y1);
            out.writeInt(edge.z1);

            out.writeInt(edge.x2);
            out.writeInt(edge.y2);
            out.writeInt(edge.z2);

            out.writeByte(edge.level.ordinal());
        }
    }

    private static SeverityLevel readSeverity(ByteArrayDataInput in) {
        SeverityLevel level = getIndexSafe(SeverityLevel.VALUES, in.readByte());
        if (level == null) {
            throw new IllegalArgumentException("Invalid network analyzer severity");
        }
        return level;
    }

    private Localized[] readLocalizedArray(ByteArrayDataInput in) {
        final int count = in.readInt();
        if (count == 0) {
            return null;
        }
        final Localized[] result = new Localized[count];
        for (int i = 0; i < count; i++) {
            result[i] = Localized.decode(in);
        }
        return result;
    }

    private void writeLocalizedArray(ByteBuf out, Localized[] arr) {
        if (arr == null || arr.length == 0) {
            out.writeInt(0);
            return;
        }
        out.writeInt(arr.length);
        for (Localized l : arr) {
            l.encode(out);
        }
    }

}
