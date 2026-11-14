package gregtech.common.gui.modularui.item;

import appeng.api.util.DimensionalCoord;
import appeng.client.render.BlockPosHighlighter;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.covers.CoverRedstoneWirelessBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.WorldProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Adapted from the GTNH Redstone Sniffer for IMPACT's regular wireless covers. */
public class RedstoneSnifferGui {

    private static final String LANG = "gt.item.redstone_sniffer.";
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int EVEN_ROW_COLOR = 0xFF4F5277;
    private static final int ODD_ROW_COLOR = 0xFF433A60;
    private static final Comparator<SnifferEntry> ENTRY_ORDER = Comparator
        .comparing((SnifferEntry entry) -> entry.privateChannel)
        .thenComparingInt(entry -> entry.frequency)
        .thenComparing(entry -> !entry.transmitter)
        .thenComparingInt(entry -> entry.dimension)
        .thenComparingInt(entry -> entry.x)
        .thenComparingInt(entry -> entry.y)
        .thenComparingInt(entry -> entry.z);

    private final PlayerInventoryGuiData data;
    private final PanelSyncManager syncManager;
    private String frequencyFilter = "";

    public RedstoneSnifferGui(PlayerInventoryGuiData data, PanelSyncManager syncManager) {
        this.data = data;
        this.syncManager = syncManager;
    }

    public ModularPanel build() {
        ModularPanel panel = ModularPanel.defaultPanel("redstone_sniffer");
        panel.resizer().sizeRel(0.5f, 0.75f).center();
        panel.widgetTheme(GTWidgetThemes.BACKGROUND_REDSTONE_SNIFFER);

        ListWidget<IWidget, ?> wirelessCovers = new ListWidget<>();
        GenericListSyncHandler<SnifferEntry> entries = new GenericListSyncHandler<>(
            this::getEntries,
            null,
            SnifferEntry::read,
            SnifferEntry::write,
            (first, second) -> ENTRY_ORDER.compare(first, second) == 0
                && first.dimensionName.equals(second.dimensionName),
            null
        );
        entries.setChangeListener(() -> {
            if (syncManager.isClient()) {
                wirelessCovers.removeAll();
                int row = 0;
                for (SnifferEntry entry : entries.getValue()) {
                    wirelessCovers.child(createRow(panel, entry, row++));
                }
            }
        });
        syncManager.syncValue("regular_map", entries);

        panel.child(Flow.column().margin(10)
            .child(Flow.row().heightRel(0.1f).marginBottom(10)
                .child(new TextWidget<>(IKey.lang(LANG + "frequency_filter")).color(TEXT_COLOR).marginRight(4))
                .child(new TextFieldWidget().widthRel(0.3f).setTextColor(TEXT_COLOR)
                    .setMaxLength(5)
                    .autoUpdateOnChange(true)
                    .value(new StringValue.Dynamic(() -> frequencyFilter, value -> frequencyFilter = value))))
            .child(Flow.row().heightRel(0.1f)
                .child(columnText(IKey.lang(LANG + "type"), 0.25f))
                .child(columnText(IKey.lang(LANG + "private"), 0.15f))
                .child(columnText(IKey.lang(LANG + "frequency"), 0.2f))
                .child(columnText(IKey.lang(LANG + "dimension"), 0.25f))
                .child(columnText(IKey.lang(LANG + "action"), 0.15f)))
            .child(wirelessCovers.widthRel(1).expanded()));
        return panel;
    }

    private List<SnifferEntry> getEntries() {
        if (data.getPlayer().worldObj.isRemote) {
            return Collections.emptyList();
        }

        List<SnifferEntry> entries = new ArrayList<>();
        for (CoverRedstoneWirelessBase cover : CoverRedstoneWirelessBase.getLoadedWirelessCovers()) {
            ICoverable tile = cover.getTile();
            if (tile == null || tile.getWorld() == null || !cover.isValid()) {
                continue;
            }

            WorldProvider provider = tile.getWorld().provider;
            entries.add(new SnifferEntry(
                cover.getFrequency(),
                cover.isPrivateChannel(),
                cover.isTransmitter(),
                tile.getXCoord(),
                tile.getYCoord(),
                tile.getZCoord(),
                provider.dimensionId,
                provider.getDimensionName()
            ));
        }
        entries.sort(ENTRY_ORDER);
        return entries;
    }

    private Flow createRow(ModularPanel panel, SnifferEntry entry, int row) {
        String frequency = Integer.toString(entry.frequency);
        return Flow.row().widthRel(1).height(30)
            .setEnabledIf(widget -> frequencyFilter.isEmpty() || frequencyFilter.equals(frequency))
            .background(new Rectangle().color(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR))
            .child(columnText(IKey.lang(LANG + (entry.transmitter ? "transmitter" : "receiver")), 0.25f))
            .child(columnText(IKey.lang(entry.privateChannel ? "gui.yes" : "gui.no"), 0.15f))
            .child(columnText(IKey.str(frequency), 0.2f))
            .child(columnText(IKey.str(entry.dimensionName), 0.25f))
            .child(Flow.row().widthRel(0.15f).mainAxisAlignment(Alignment.MainAxis.CENTER)
                .child(new ButtonWidget<>().size(25)
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_REDSTONESNIFFERLOCATE.asIcon().size(19))
                    .tooltip(tooltip -> tooltip.addLine(IKey.lang(LANG + "locate")))
                    .onMousePressed(mouseButton -> {
                        if (mouseButton != 0) {
                            return false;
                        }
                        highlight(entry);
                        panel.closeIfOpen();
                        return true;
                    })));
    }

    private static TextWidget<?> columnText(IKey label, float width) {
        return new TextWidget<>(label).widthRel(width).color(TEXT_COLOR).textAlign(Alignment.Center);
    }

    @SideOnly(Side.CLIENT)
    private void highlight(SnifferEntry entry) {
        EntityPlayer player = data.getPlayer();
        if (player.dimension == entry.dimension) {
            double deltaX = entry.x + 0.5 - player.posX;
            double deltaY = entry.y + 0.5 - player.posY - player.getEyeHeight();
            double deltaZ = entry.z + 0.5 - player.posZ;
            player.rotationYaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
            player.rotationPitch = (float) Math.toDegrees(Math.atan2(-deltaY, Math.hypot(deltaX, deltaZ)));
        }
        BlockPosHighlighter.highlightBlocks(
            player,
            Collections.singletonList(new DimensionalCoord(entry.x, entry.y, entry.z, entry.dimension)),
            LANG + "highlight_message",
            LANG + "wrong_dim_message"
        );
    }

    private static final class SnifferEntry {

        private final int frequency;
        private final boolean privateChannel;
        private final boolean transmitter;
        private final int x;
        private final int y;
        private final int z;
        private final int dimension;
        private final String dimensionName;

        private SnifferEntry(
            int frequency,
            boolean privateChannel,
            boolean transmitter,
            int x,
            int y,
            int z,
            int dimension,
            String dimensionName
        ) {
            this.frequency = frequency;
            this.privateChannel = privateChannel;
            this.transmitter = transmitter;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.dimensionName = dimensionName;
        }

        private static SnifferEntry read(PacketBuffer buffer) throws IOException {
            return new SnifferEntry(
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readStringFromBuffer(32767)
            );
        }

        private static void write(PacketBuffer buffer, SnifferEntry entry) throws IOException {
            buffer.writeInt(entry.frequency);
            buffer.writeBoolean(entry.privateChannel);
            buffer.writeBoolean(entry.transmitter);
            buffer.writeInt(entry.x);
            buffer.writeInt(entry.y);
            buffer.writeInt(entry.z);
            buffer.writeInt(entry.dimension);
            buffer.writeStringToBuffer(entry.dimensionName);
        }
    }
}
