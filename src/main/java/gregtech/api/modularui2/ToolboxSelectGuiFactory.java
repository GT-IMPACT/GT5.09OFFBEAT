package gregtech.api.modularui2;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import cpw.mods.fml.relauncher.Side;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.ToolboxSlot;
import gregtech.api.net.GT_Packet_ToolboxEvent;
import gregtech.common.gui.modularui.widget.radialmenu.RadialMenuBuilder;
import gregtech.common.gui.modularui.widget.radialmenu.RadialMenu.RadialMenuClickHandler;
import gregtech.common.gui.modularui.widget.radialmenu.RadialMenuTheme;
import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;

/**
 * GUI for selecting the active tool.
 * <p>
 * <b>NOTE:</b> This GUI may not always open depending on the situation that the toolbox finds itself in (e.g. only one
 * tool inside.) See {@link ItemGTToolbox#onPickBlock(ItemStack, EntityPlayer)} for more information.
 *
 * @see gregtech.common.gui.modularui.item.ToolboxInventoryGui
 */
public class ToolboxSelectGuiFactory extends AbstractUIFactory<GuiData> {

    public static final ToolboxSelectGuiFactory INSTANCE = new ToolboxSelectGuiFactory();

    private ToolboxSelectGuiFactory() {
        super("gregtech:toolbox_select");
    }

    public void open(EntityPlayer player) {
        if (player.getEntityWorld().isRemote) {
            GuiManager.openFromClient(this, new GuiData(player));
        }
    }

    @Override
    public @NotNull IGuiHolder<GuiData> getGuiHolder(final GuiData data) {
        return SelectGuiHolder.INSTANCE;
    }

    @Override
    public void writeGuiData(final GuiData guiData, final PacketBuffer buffer) {
    }

    @Override
    public @NotNull GuiData readGuiData(final EntityPlayer player, final PacketBuffer buffer) {
        return new GuiData(player);
    }

    static class SelectGuiHolder implements IGuiHolder<GuiData> {

        static final SelectGuiHolder INSTANCE = new SelectGuiHolder();

        private SelectGuiHolder() {
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public ModularPanel buildUI(final GuiData data, final PanelSyncManager syncManager, final UISettings settings) {
            final int inventorySlot = data.getPlayer().inventory.currentItem;
            final ItemStack toolbox = data.getPlayer().inventory.getStackInSlot(inventorySlot);
            final ModularPanel panel = ModularPanel.defaultPanel("ToolboxSelectUI");
            if (toolbox == null || !(toolbox.getItem() instanceof ItemGTToolbox)) {
                return panel;
            }
            final ToolboxItemStackHandler itemHandler = new ToolboxItemStackHandler(toolbox);

            panel.fullScreenInvisible();
            panel.child(
                new RadialMenuBuilder("gt5:toolbox:select_gui", syncManager).innerIcon(toolbox)
                    .theme(
                        new RadialMenuTheme(new float[] { 0f, 0f, 0f, 0.4f }, new float[] { 0.25f, 0.25f, 0.25f, 1f }))
                    .pipe(builder -> {
                        builder.innerIcon(toolbox)
                            .option()
                            .label(IKey.lang("GT5U.gui.text.toolbox.radial.unselect"))
                            .onClicked(onClick(data.getPlayer(), inventorySlot, ItemGTToolbox.NO_TOOL_SELECTED))
                            .done();

                        for (ToolboxSlot slot : ToolboxSlot.TOOL_SLOTS) {
                            final ItemStack toolStack = itemHandler.getStackInSlot(slot.getSlotID());
                            if (toolStack != null) {
                                builder.option()
                                    .label(new ItemDrawable(toolStack).asIcon())
                                    .onClicked(onClick(data.getPlayer(), inventorySlot, slot.getSlotID()))
                                    .done();
                            } else {
                                final Optional<UITexture> overlay = slot.getOverlay();
                                final Icon icon;

                                if (overlay.isPresent()) {
                                    icon = IDrawable.of(
                                        slot.getOverlay()
                                            .get(),
                                        GTGuiTextures.OVERLAY_BUTTON_CROSS)
                                        .asIcon();
                                } else {
                                    icon = IDrawable.of(GTGuiTextures.OVERLAY_BUTTON_CROSS)
                                        .asIcon();
                                }

                                builder.option()
                                    .label(icon.size(16, 16))
                                    .onClicked(() -> false)
                                    .done();
                            }
                        }


                    })
                    .build()
                    .relativeToScreen()
                    .top(0)
                    .bottom(0)
                    .left(0)
                    .right(0));
            return panel;
        }

        static RadialMenuClickHandler onClick(EntityPlayer player, int inventorySlot, int selection) {
            return (menu, option, button, side) -> {
                if (side == Side.CLIENT) {
                    GT_Values.NW.sendToServer(new GT_Packet_ToolboxEvent(inventorySlot, selection));
                    player.closeScreen();
                }
            };
        }
    }
}
