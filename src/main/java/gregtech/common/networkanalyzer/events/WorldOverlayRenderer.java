package gregtech.common.networkanalyzer.events;

import appeng.api.util.DimensionalCoord;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.common.networkanalyzer.Localized;
import gregtech.common.networkanalyzer.NetworkAnalyzerData;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.GraphNodeState;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.OverlayEdge;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.OverlayNode;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.SeverityLevel;
import gregtech.common.tools.ItemNetworkAnalyzer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import java.util.List;

public class WorldOverlayRenderer {

    private static final float NONE_SIZE = 0.10f;
    private static final float FORK_SIZE = 0.15f;
    private static final float TERMINAL_SIZE = 0.2f;

    private static int nodeColor(GraphNodeState state) {
        switch (state) {
            case INPUT:
                return 0xF18F01;
            case OUTPUT:
                return 0x66CCFF;
            case FORK:
                return 0x00FF00;
            default:
                throw new AssertionError(state);
        }
    }

    private static int severityColor(SeverityLevel level) {
        switch (level) {
            case NONE:
                return 0x7D7D7D;
            case SUCCESS:
                return 0x00FF00;
            case WARNING:
                return 0xE9C46A;
            case ALERT:
                return 0xD62828;
            default:
                throw new AssertionError(level);
        }
    }

    private static int[] unpackRgb(int rgb) {
        return new int[] { (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF };
    }

    private static final double MAX_LABEL_DISTANCE_SQ = 36 * 36;
    private static final double MAX_NODE_DISTANCE_SQ = 36 * 36;
    private static final float LINE_WIDTH = 3f;

    private static volatile Snapshot snapshot;
    private static Snapshot renderedSnapshot;
    private static int staticList;

    private static final class Snapshot {

        private final World world;
        private final DimensionalCoord target;
        private final NetworkAnalyzerData data;

        private Snapshot(World world, DimensionalCoord target, NetworkAnalyzerData data) {
            this.world = world;
            this.target = target;
            this.data = data;
        }
    }

    public static void accept(World world, DimensionalCoord target, NetworkAnalyzerData data) {
        snapshot = new Snapshot(world, target, data);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new WorldOverlayRenderer());
        staticList = GL11.glGenLists(1);
    }

    @SubscribeEvent
    public void onRenderWorldLastEvent(RenderWorldLastEvent ev) {
        final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        if (player == null) {
            return;
        }

        final ItemStack stack = player.inventory.getCurrentItem();

        final Snapshot current = snapshot;
        if (current == null || current.world != player.worldObj) {
            renderedSnapshot = null;
            return;
        }

        final DimensionalCoord target = stack == null || !(stack.getItem() instanceof ItemNetworkAnalyzer)
            ? null : ItemNetworkAnalyzer.getLocation(stack);
        if (target == null || target.getDimension() != player.dimension || !target.isEqual(current.target)) {
            return;
        }

        final EntityLivingBase p = Minecraft.getMinecraft().renderViewEntity;
        final double viewX = p.lastTickPosX + (p.posX - p.lastTickPosX) * ev.partialTicks;
        final double viewY = p.lastTickPosY + (p.posY - p.lastTickPosY) * ev.partialTicks;
        final double viewZ = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * ev.partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-viewX, -viewY, -viewZ);

        doRender(current, viewX, viewY, viewZ);

        GL11.glPopMatrix();
    }

    private static void doRender(Snapshot current, double viewX, double viewY, double viewZ) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_LINE_BIT | GL11.GL_HINT_BIT | GL11.GL_CURRENT_BIT);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (renderedSnapshot != current) {
            renderedSnapshot = current;
            GL11.glNewList(staticList, GL11.GL_COMPILE);

            renderNodes(current.data.nodes, viewX, viewY, viewZ);

            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

            renderEdges(current.data.edges, LINE_WIDTH);

            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);

            GL11.glEndList();
        }

        GL11.glCallList(staticList);

        MovingObjectPosition position = Minecraft.getMinecraft().objectMouseOver;

        if (position != null && position.typeOfHit != MovingObjectType.BLOCK) {
            position = null;
        }

        // Labels are rendered every frame because they need to face the camera
        for (OverlayNode node : current.data.nodes) {
            final double nx = node.x + 0.5d;
            final double ny = node.y + 0.5d;
            final double nz = node.z + 0.5d;

            if (position != null && node.tooltip != null
                && node.tooltip.length > 0
                && node.x == position.blockX
                && node.y == position.blockY
                && node.z == position.blockZ) {
                renderFloatingText(translate(node.tooltip), nx, ny, nz, 0xFFFFFF);
            } else if (node.label != null && node.label.length > 0
                && distSq(nx, ny, nz, viewX, viewY, viewZ) <= MAX_LABEL_DISTANCE_SQ) {
                renderFloatingText(translate(node.label), nx, ny, nz, 0xFFFFFF);
            }
        }

        GL11.glPopAttrib();
    }

    private static String translate(Localized[] parts) {
        StringBuilder lines = new StringBuilder();
        for (Localized part : parts) {
            if (lines.length() > 0) {
                lines.append('\n');
            }
            lines.append(part);
        }
        return lines.toString();
    }

    private static double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        final double dz = z1 - z2;

        return dx * dx + dy * dy + dz * dz;
    }

    private static void setColorRGBA_F(Tessellator tessellator, int argb) {
        tessellator.setColorRGBA_F(
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >> 8) & 0xFF) / 255f,
            (argb & 0xFF) / 255f,
            ((argb >> 24) & 0xFF) / 255f);
    }

    private static void renderFloatingText(String text, double x, double y, double z, int color) {
        final RenderManager renderManager = RenderManager.instance;
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        final Tessellator tessellator = Tessellator.instance;

        final String[] lines = text.split("\n");
        final int lineHeight = fontRenderer.FONT_HEIGHT + 1;

        final float scale = 0.027f;
        GL11.glColor4f(1f, 1f, 1f, 0.5f);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(renderManager.playerViewX, 1.0f, 0.0f, 0.0f);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(line));
        }
        final int halfWidth = maxWidth / 2;
        final int totalHeight = lines.length * lineHeight;
        final int yOffset = -(totalHeight / 2);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tessellator.startDrawingQuads();
        setColorRGBA_F(tessellator, 0x80000000);
        tessellator.addVertex(-halfWidth - 1, yOffset - 1, 0.0d);
        tessellator.addVertex(-halfWidth - 1, yOffset + totalHeight, 0.0d);
        tessellator.addVertex(halfWidth + 1, yOffset + totalHeight, 0.0d);
        tessellator.addVertex(halfWidth + 1, yOffset - 1, 0.0d);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 0.5f);
        for (int i = 0; i < lines.length; i++) {
            final int lineY = yOffset + i * lineHeight;
            final int lineWidth = fontRenderer.getStringWidth(lines[i]);
            fontRenderer.drawString(lines[i], -lineWidth / 2, lineY, color);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        for (int i = 0; i < lines.length; i++) {
            final int lineY = yOffset + i * lineHeight;
            final int lineWidth = fontRenderer.getStringWidth(lines[i]);
            fontRenderer.drawString(lines[i], -lineWidth / 2, lineY, color);
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    private static void renderNodes(List<OverlayNode> nodes, double viewX, double viewY, double viewZ) {
        final Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_QUADS);

        for (OverlayNode node : nodes) {
            boolean terminal = node.level != SeverityLevel.NONE && node.state != null
                && node.state != GraphNodeState.FORK;
            if (!terminal && distSq(node.x, node.y, node.z, viewX, viewY, viewZ) > MAX_NODE_DISTANCE_SQ) {
                continue;
            }

            final float size = node.level == SeverityLevel.NONE || node.state == null ? NONE_SIZE
                : node.state == GraphNodeState.FORK ? FORK_SIZE : TERMINAL_SIZE;
            final double nx = node.x + 0.5d;
            final double ny = node.y + 0.5d;
            final double nz = node.z + 0.5d;
            final int[] color = unpackRgb(node.level == SeverityLevel.SUCCESS && node.state != null
                ? nodeColor(node.state) : severityColor(node.level));

            tess.setColorRGBA(color[0], color[1], color[2], 255); // +Y
            tess.addVertex(nx - size, ny + size, nz + size);
            tess.addVertex(nx + size, ny + size, nz + size);
            tess.addVertex(nx + size, ny + size, nz - size);
            tess.addVertex(nx - size, ny + size, nz - size);

            tess.setColorRGBA(color[0] / 2, color[1] / 2, color[2] / 2, 255); // -Y
            tess.addVertex(nx + size, ny - size, nz - size);
            tess.addVertex(nx + size, ny - size, nz + size);
            tess.addVertex(nx - size, ny - size, nz + size);
            tess.addVertex(nx - size, ny - size, nz - size);

            tess.setColorRGBA(color[0] * 8 / 10, color[1] * 8 / 10, color[2] * 8 / 10, 255); // +/- Z
            tess.addVertex(nx + size, ny - size, nz + size);
            tess.addVertex(nx + size, ny + size, nz + size);
            tess.addVertex(nx - size, ny + size, nz + size);
            tess.addVertex(nx - size, ny - size, nz + size);
            tess.addVertex(nx - size, ny + size, nz - size);
            tess.addVertex(nx + size, ny + size, nz - size);
            tess.addVertex(nx + size, ny - size, nz - size);
            tess.addVertex(nx - size, ny - size, nz - size);

            tess.setColorRGBA(color[0] * 6 / 10, color[1] * 6 / 10, color[2] * 6 / 10, 255); // +/- X
            tess.addVertex(nx + size, ny + size, nz - size);
            tess.addVertex(nx + size, ny + size, nz + size);
            tess.addVertex(nx + size, ny - size, nz + size);
            tess.addVertex(nx + size, ny - size, nz - size);
            tess.addVertex(nx - size, ny - size, nz + size);
            tess.addVertex(nx - size, ny + size, nz + size);
            tess.addVertex(nx - size, ny + size, nz - size);
            tess.addVertex(nx - size, ny - size, nz - size);
        }

        tess.draw();
    }

    private static void renderEdges(List<OverlayEdge> edges, float width) {
        final float previousLineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        final Tessellator tess = Tessellator.instance;

        GL11.glLineWidth(width);
        tess.startDrawing(GL11.GL_LINES);

        for (OverlayEdge edge : edges) {
            final int[] color = unpackRgb(severityColor(edge.level));
            tess.setColorRGBA(color[0], color[1], color[2], 255);
            tess.addVertex(edge.x1 + 0.5d, edge.y1 + 0.5d, edge.z1 + 0.5d);
            tess.addVertex(edge.x2 + 0.5d, edge.y2 + 0.5d, edge.z2 + 0.5d);
        }

        tess.draw();
        GL11.glLineWidth(previousLineWidth);
    }

}
