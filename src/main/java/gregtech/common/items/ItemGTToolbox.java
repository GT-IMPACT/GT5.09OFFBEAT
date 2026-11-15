package gregtech.common.items;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static gregtech.api.enums.GT_Values.V;
import static gregtech.api.enums.GT_Values.MOD_ID;

import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.world.BlockEvent;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.gtnhlib.GTNHLib;

import appeng.api.implementations.items.IAEWrench;
import buildcraft.api.tools.IToolWrench;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.api.tool.ITool;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.ToolboxSlot;
import gregtech.api.interfaces.IDamagableItem;
import gregtech.api.items.GT_Generic_Item;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.modularui2.ToolboxSelectGuiFactory;
import gregtech.api.net.GT_Packet_ToolboxEvent;
import gregtech.api.util.GT_ModHandler;
import gregtech.common.gui.modularui.item.ToolboxContainer;
import gregtech.common.gui.modularui.item.ToolboxInventoryGui;
import gregtech.common.items.toolbox.ToolboxElectricManager;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;
import gregtech.common.items.toolbox.ToolboxPickBlockDecider;
import gregtech.common.items.toolbox.ToolboxUtil;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import mods.railcraft.api.core.items.IToolCrowbar;
import mrtjp.projectred.api.IScrewdriver;

@InterfaceList(
    value = { @Interface(iface = "mods.railcraft.api.core.items.IToolCrowbar", modid = "Railcraft"),
        @Interface(iface = "buildcraft.api.tools.IToolWrench", modid = "BuildCraft|Core"),
        @Interface(iface = "crazypants.enderio.api.tool.ITool", modid = "EnderIO"),
        @Interface(iface = "mrtjp.projectred.api.IScrewdriver", modid = "ProjRed|Core"), })
public class ItemGTToolbox extends GT_Generic_Item implements IGuiHolder<PlayerInventoryGuiData>, ISpecialElectricItem,
    IDamagableItem, IToolCrowbar, IToolWrench, ITool, IScrewdriver, IAEWrench {

    public static final String CONTENTS_KEY = "gt5u.toolbox:Contents";
    public static final String TOOLBOX_OPEN_KEY = "gt5u.toolbox:ToolboxOpen";
    public static final String CURRENT_TOOL_KEY = "gt5u.toolbox:SelectedSlot";
    public static final String RECENTLY_BROKEN_SLOT_KEY = "gt5u.toolbox:RecentlyBroken";
    public static final String BROKEN_TOOL_ANIMATION_END_KEY = "gt5u.toolbox:BrokenToolAnimationEnd";
    public static final int NO_TOOL_SELECTED = -1;

    /**
     * The open inventory owns the toolbox contents while its container is active.
     */
    public static boolean isInventoryOpen(ItemStack toolbox) {
        return toolbox.hasTagCompound() && toolbox.getTagCompound().getBoolean(TOOLBOX_OPEN_KEY);
    }

    private static final int CHARGE_TICK = 20;
    private static final long BROKEN_TOOL_ANIMATION_LENGTH = 60;
    private static final long GUI_OPEN_DELAY = 5;

    public ItemGTToolbox(final String aUnlocalized, final String aEnglish, final String aEnglishTooltip) {
        super(aUnlocalized, aEnglish, aEnglishTooltip);
        setMaxStackSize(1);
        setNoRepair();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(final IIconRegister iconRegister) {
        super.registerIcons(iconRegister);
        for (ToolboxSlot slot : ToolboxSlot.TOOL_SLOTS) {
            slot.registerIcon(iconRegister);
        }
    }

    @Override
    public void onUpdate(final ItemStack toolbox, final World world, final Entity entity, final int timer, final boolean isInHand) {
        if (world.isRemote) {
            return;
        }

        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            final ToolboxItemStackHandler handler = new ToolboxItemStackHandler(toolbox);
            boolean shouldUpdate = false;

            // If the tool being used has a very bad mining speed, it's possible for it to not be able to finish mining
            // a block because the equipped stack changes. We can, after a fashion, check to see if the tool is
            // currently being used by checking the arm swing animation.
            if (player.ticksExisted % CHARGE_TICK == 0 && ToolboxUtil.canCharge(toolbox) && !player.isSwingInProgress) {
                final ItemStack battery = handler.extractItem(ToolboxSlot.BATTERY.getSlotID(), 1, true);
                if (battery != null && battery.getItem() instanceof IElectricItem) {
                    IElectricItem batteryItem = (IElectricItem) battery.getItem();
                    shouldUpdate = getElectricManager(battery).map(batteryManager -> {
                        boolean dirty = false;
                        double remainingCharge = batteryManager.discharge(
                            battery,
                            getMaxVoltage(batteryItem.getTier(battery)) * CHARGE_TICK,
                            Integer.MAX_VALUE,
                            true,
                            true,
                            true
                        );

                        for (final ToolboxSlot slot : ToolboxSlot.values()) {
                            if (slot == ToolboxSlot.BATTERY) {
                                continue;
                            }

                            if (remainingCharge <= 0) {
                                break;
                            }
                            final double availableCharge = remainingCharge;

                            final ItemStack slotStack = handler.extractItem(slot.getSlotID(), 1, true);
                            if (slotStack == null || !(slotStack.getItem() instanceof IElectricItem)) {
                                continue;
                            }

                            IElectricItem slotItem = (IElectricItem) slotStack.getItem();
                            final double powerUsed = getElectricManager(slotStack).map(slotManager -> slotManager.charge(
                                slotStack,
                                (int) Math.min(availableCharge, getMaxVoltage(slotItem.getTier(slotStack)) * CHARGE_TICK),
                                Integer.MAX_VALUE,
                                true,
                                false)).orElse(0d);

                            if (powerUsed > 0) {
                                batteryManager.discharge(battery, powerUsed, Integer.MAX_VALUE, true, true, false);
                                remainingCharge -= powerUsed;
                                dirty = true;
                                handler.setStackInSlot(slot.getSlotID(), slotStack);
                            }
                        }

                        if (dirty) {
                            handler.setStackInSlot(ToolboxSlot.BATTERY.getSlotID(), battery);
                        }

                        return dirty;
                    }).orElse(false);
                }
            }

            if (shouldUpdate) {
                ToolboxUtil.saveToolbox(toolbox, handler);
            }

            if (!toolbox.hasTagCompound()) {
                toolbox.setTagCompound(new NBTTagCompound());
            }
            final NBTTagCompound tag = toolbox.getTagCompound();

            // Handle broken tool animation
            if (tag.hasKey(RECENTLY_BROKEN_SLOT_KEY)) {
                if (isInventoryOpen(toolbox) || (tag.hasKey(CURRENT_TOOL_KEY) && tag.getInteger(CURRENT_TOOL_KEY) != NO_TOOL_SELECTED)) {
                    tag.removeTag(BROKEN_TOOL_ANIMATION_END_KEY);
                    tag.removeTag(RECENTLY_BROKEN_SLOT_KEY);
                } else if (!tag.hasKey(BROKEN_TOOL_ANIMATION_END_KEY)) {
                    tag.setLong(BROKEN_TOOL_ANIMATION_END_KEY, player.getEntityWorld().getTotalWorldTime() + BROKEN_TOOL_ANIMATION_LENGTH);
                } else if (tag.getLong(BROKEN_TOOL_ANIMATION_END_KEY) < world.getTotalWorldTime()) {
                    tag.removeTag(BROKEN_TOOL_ANIMATION_END_KEY);
                    tag.removeTag(RECENTLY_BROKEN_SLOT_KEY);
                }
            }
        }
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack toolbox, final World world, final EntityPlayer player) {
        if (player instanceof EntityPlayerMP && canOpenInventoryGui(toolbox, world)) {
            GuiFactories.playerInventory().openFromMainHand(player);
        }

        return super.onItemRightClick(toolbox, world, player);
    }

    @Override
    public boolean onItemUseFirst(final ItemStack toolbox, final EntityPlayer player, final World world, final int x,
        final int y, final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        return ToolboxUtil.getSelectedToolType(toolbox)
            .map(slot -> {
                final ToolboxItemStackHandler handler = new ToolboxItemStackHandler(toolbox);
                final ItemStack tool = handler.getStackInSlot(slot.getSlotID());
                boolean result = false;

                if (tool != null && tool.getItem() != null) {
                    result = tool.getItem()
                        .onItemUseFirst(tool, player, world, x, y, z, side, hitX, hitY, hitZ);
                    ToolboxUtil.saveItemInside(toolbox, tool, slot);
                }

                return result;
            })
            .orElse(false);
    }

    @Override
    public String getItemStackDisplayName(final ItemStack toolbox) {
        String base = super.getItemStackDisplayName(toolbox);
        return ToolboxUtil.getSelectedToolType(toolbox).map(slot -> StatCollector.translateToLocalFormatted(
            "GT5U.item.toolbox.name_template",
            base,
            StatCollector.translateToLocal("GT5U.gui.text.toolbox.slot_title." + slot.name().toLowerCase(java.util.Locale.ROOT))
        )).orElse(base);
    }

    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(final ItemStack toolbox, final EntityPlayer player, final List tooltipList,
        final boolean f3mode) {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        String use = getTooltipKeyName(settings.keyBindUseItem.getKeyCode());
        String pickBlock = getTooltipKeyName(settings.keyBindPickBlock.getKeyCode());
        String sneak = getTooltipKeyName(settings.keyBindSneak.getKeyCode());
        Optional<ItemStack> selectedTool = ToolboxUtil.getSelectedTool(toolbox);

        if (selectedTool.isPresent()) {
            tooltipList.add(
                StatCollector.translateToLocalFormatted(
                    "GT5U.item.toolbox.tooltip.deselect_tool",
                    sneak,
                    pickBlock));
        }
        tooltipList.add(StatCollector.translateToLocalFormatted("GT5U.item.toolbox.tooltip.open_toolbox", use));
        tooltipList.add(StatCollector.translateToLocalFormatted("GT5U.item.toolbox.tooltip.select_tool", pickBlock));
        tooltipList.add(StatCollector.translateToLocalFormatted("GT5U.item.toolbox.tooltip.radial_menu", sneak, pickBlock));
        tooltipList.add(StatCollector.translateToLocalFormatted("GT5U.item.toolbox.tooltip.repair", use));

        selectedTool.ifPresent(tool -> {
            long maxDamage = GT_MetaGenerated_Tool.getToolMaxDamage(tool);
            tooltipList.add(StatCollector.translateToLocalFormatted(
                "GT5U.item.toolbox.tooltip.tool_durability",
                formatNumber(maxDamage - GT_MetaGenerated_Tool.getToolDamage(tool)),
                formatNumber(maxDamage)));

            GT_MetaGenerated_Tool toolItem = (GT_MetaGenerated_Tool) tool.getItem();
            Long[] electricStats = toolItem.getElectricStats(tool);
            if (electricStats != null) {
                tooltipList.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
                    "GT5U.item.toolbox.tooltip.tool_charge",
                    formatNumber(toolItem.getRealCharge(tool)),
                    formatNumber(Math.abs(electricStats[0]))));
            }
        });

        ToolboxUtil.withBatteryAndManager(toolbox, (battery, manager) -> {
            IElectricItem batteryItem = (IElectricItem) battery.getItem();
            tooltipList.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
                "GT5U.item.toolbox.tooltip.battery_charge",
                formatNumber(manager.getCharge(battery)),
                formatNumber(batteryItem.getMaxCharge(battery))));
        });
    }

    @SideOnly(Side.CLIENT)
    private static String getTooltipKeyName(int keyCode) {
        switch (keyCode) {
            case -100:
                return StatCollector.translateToLocal("GT5U.item.toolbox.key.left_mouse");
            case -99:
                return StatCollector.translateToLocal("GT5U.item.toolbox.key.right_mouse");
            case -98:
                return StatCollector.translateToLocal("GT5U.item.toolbox.key.middle_mouse");
            case Keyboard.KEY_LSHIFT:
                return StatCollector.translateToLocal("GT5U.item.toolbox.key.left_shift");
            case Keyboard.KEY_NONE:
                return StatCollector.translateToLocal("GT5U.item.toolbox.key.unbound");
            default:
                return GameSettings.getKeyDisplayString(keyCode);
        }
    }

    @Override
    public ModularPanel buildUI(final PlayerInventoryGuiData data, final PanelSyncManager syncManager,
        final UISettings settings) {
        int slot = data.getSlotIndex();
        ToolboxItemStackHandler stackHandler = new ToolboxItemStackHandler(data.getUsedItemStack());
        ToolboxContainer container = new ToolboxContainer(data, stackHandler);
        settings.customContainer(() -> container);
        syncManager.addOpenListener(player -> {
            if (player.worldObj.isRemote) {
                GT_Values.NW.sendToServer(GT_Packet_ToolboxEvent.inventoryOpened(slot));
            }
        });
        if (!syncManager.isClient()) {
            settings.canInteractWith(player -> {
                ItemStack held = data.getUsedItemStack();
                return held != null && held.getItem() == this;
            });
            syncManager.addCloseListener(player -> container.saveContents());
        }
        return new ToolboxInventoryGui(syncManager, data, stackHandler).build();
    }

    @Override
    public ModularScreen createScreen(final PlayerInventoryGuiData data, final ModularPanel mainPanel) {
        return new ModularScreen(MOD_ID, mainPanel);
    }

    @Override
    public boolean doDamageToItem(final ItemStack toolbox, final int vanillaDamage) {
        if (toolbox == null || !(toolbox.getItem() instanceof ItemGTToolbox)) {
            return false;
        }

        return ToolboxUtil.getSelectedTool(toolbox).map(toolStack -> {
            if (toolStack.getItem() instanceof GT_MetaGenerated_Tool) {
                GT_MetaGenerated_Tool tool = (GT_MetaGenerated_Tool) toolStack.getItem();
                final ToolboxItemStackHandler handler = new ToolboxItemStackHandler(toolbox);

                if (tool.doDamageToItem(toolStack, vanillaDamage)) {
                    handler.setCurrentTool(toolStack);
                    ToolboxUtil.saveToolbox(toolbox, handler);

                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    public boolean shouldDrawHighlightGrid(DrawBlockHighlightEvent event) {
        if (event.currentItem == null || !(event.currentItem.getItem() instanceof ItemGTToolbox)) {
            return false;
        }

        return !ToolboxPickBlockDecider.getSuggestedTool(event)
            .isEmpty();
    }

    /**
     * Imposes a small delay between breaking a tool inside and opening the GUI. If a user breaks a tool via methods
     * other than breaking a block (e.g.: connecting pipes with a wrench), it can open the GUI immediately after the
     * tool breaks because the user has the use item bind depressed.
     *
     * @param toolbox The item stack of the toolbox
     * @param world   The world where the GUI is opening
     * @return true if it isn't a few ticks immediately after breaking a tool
     */
    private static boolean canOpenInventoryGui(final ItemStack toolbox, final World world) {
        final NBTTagCompound tag = toolbox.hasTagCompound() ? toolbox.getTagCompound() : new NBTTagCompound();
        final boolean recentlyBrokenTool = tag.hasKey(RECENTLY_BROKEN_SLOT_KEY);

        if (recentlyBrokenTool && (tag.hasKey(BROKEN_TOOL_ANIMATION_END_KEY)
            ? tag.getLong(BROKEN_TOOL_ANIMATION_END_KEY) - world.getTotalWorldTime()
            : BROKEN_TOOL_ANIMATION_LENGTH) > BROKEN_TOOL_ANIMATION_LENGTH - GUI_OPEN_DELAY) {
            return false;
        }

        // noinspection SimplifyOptionalCallChains
        return !world.isRemote && !ToolboxUtil.getSelectedToolType(toolbox)
            .isPresent();
    }

    /**
     * Gets the currently equipped toolbox.
     *
     * @param player The player to interrogate
     * @return An optional with the toolbox's item stack, or empty if the user is not wielding a toolbox
     */
    private static Optional<ItemStack> getToolboxIfEquipped(EntityPlayer player) {
        ItemStack stack = player == null ? null : player.getCurrentEquippedItem();
        return stack != null && stack.getItem() instanceof ItemGTToolbox ? Optional.of(stack) : Optional.empty();
    }

    private static void sendChangeToolPacket(int inventorySlot, int newToolType) {
        GT_Values.NW.sendToServer(
            new GT_Packet_ToolboxEvent(inventorySlot, newToolType));
    }

    // region Event Handlers

    @SideOnly(Side.CLIENT)
    public boolean onPickBlock(final ItemStack toolbox, final EntityPlayer player) {
        if (!(toolbox.getItem() instanceof ItemGTToolbox)) {
            return false;
        }

        final int inventorySlot = player.inventory.currentItem;
        final Optional<ToolboxSlot> selectedToolType = ToolboxUtil.getSelectedToolType(toolbox);

        if (player.isSneaking()) {
            if (selectedToolType.isPresent()) {
                sendChangeToolPacket(inventorySlot, NO_TOOL_SELECTED);
                return true;
            }
        }

        final ToolboxItemStackHandler handler = new ToolboxItemStackHandler(toolbox);
        int toolCount = 0;
        int lastSlot = -1;
        for (ToolboxSlot slot : ToolboxSlot.TOOL_SLOTS) {
            if (handler.getStackInSlot(slot.getSlotID()) != null) {
                toolCount++;
                lastSlot = slot.getSlotID();
            }
        }

        if (toolCount == 0) {
            GTNHLib.proxy.printMessageAboveHotbar(
                StatCollector.translateToLocal("GT5U.gui.text.toolbox.error.no_tools"),
                120,
                true,
                true);
            return false;
        }

        if (!player.isSneaking()) {
            if (toolCount == 1) {
                sendChangeToolPacket(inventorySlot, selectedToolType.isPresent() ? NO_TOOL_SELECTED : lastSlot);
                return true;
            } else {
                for (ToolboxSlot suggested : ToolboxPickBlockDecider.getSuggestedTool(player)) {
                    if (handler.getStackInSlot(suggested.getSlotID()) != null) {
                        sendChangeToolPacket(inventorySlot, suggested.getSlotID());
                        return true;
                    }
                }
            }
        }

        ToolboxSelectGuiFactory.INSTANCE.open(player);
        return true;
    }

    public void onBlockHarvestingEvent(ItemStack toolbox, BlockEvent.HarvestDropsEvent event) {
        ToolboxUtil.getSelectedToolType(toolbox).ifPresent(slot ->
            ToolboxUtil.getSelectedTool(toolbox).ifPresent(tool -> {
                GT_MetaGenerated_Tool item = (GT_MetaGenerated_Tool) tool.getItem();
                item.onHarvestBlockEvent(
                    event.drops, tool, event.harvester, event.block, event.x, event.y, event.z,
                    (byte) event.blockMetadata, event.fortuneLevel, event.isSilkTouching, event
                );
                ToolboxUtil.saveItemInside(toolbox, tool, slot);
            })
        );
    }

    // region Vanilla Tool Harvesting Methods
    @Override
    public boolean canHarvestBlock(final Block block, final ItemStack toolbox) {
        return ToolboxUtil.getSelectedTool(toolbox).map(
            tool -> tool.getItem() instanceof GT_MetaGenerated_Tool && tool.getItem().canHarvestBlock(block, tool))
            .orElse(false);
    }

    @Override
    public float getDigSpeed(final ItemStack toolbox, final Block block, final int metadata) {
        return ToolboxUtil.getSelectedTool(toolbox)
            .filter(tool -> tool.getItem() instanceof GT_MetaGenerated_Tool)
            .map(
                tool -> tool.getItem()
                    .getDigSpeed(tool, block, metadata))
            .orElse(0f);
    }

    @Override
    public EnumAction getItemUseAction(final ItemStack toolbox) {
        return ToolboxUtil.getSelectedTool(toolbox)
            .filter(tool -> tool.getItem() != null)
            .map(
                tool -> tool.getItem()
                    .getItemUseAction(tool))
            .orElse(EnumAction.none);
    }

    @Override
    public boolean onBlockDestroyed(final ItemStack toolbox, final World worldIn, final Block blockIn, final int x,
        final int y, final int z, final EntityLivingBase entity) {

        return ToolboxUtil.getSelectedToolType(toolbox)
            .map(
                toolboxSlot -> ToolboxUtil.getSelectedTool(toolbox)
                    .filter(tool -> tool.getItem() != null)
                    .map(tool -> {
                        final boolean result = tool.getItem()
                            .onBlockDestroyed(tool, worldIn, blockIn, x, y, z, entity);
                        if (result) {
                            ToolboxUtil.saveItemInside(toolbox, tool, toolboxSlot);
                        }

                        return result;
                    })
                    .orElse(false))
            .orElse(false);

    }

    @Override
    public boolean onBlockStartBreak(final ItemStack toolbox, final int x, final int y, final int z,
        final EntityPlayer player) {
        return ToolboxUtil.getSelectedToolType(toolbox).map(slot ->
            ToolboxUtil.getSelectedTool(toolbox).map(tool -> {
                boolean result = tool.getItem().onBlockStartBreak(tool, x, y, z, player);
                ToolboxUtil.saveItemInside(toolbox, tool, slot);
                return result;
            }).orElse(false)
        ).orElse(false);
    }

    @Override
    public int getHarvestLevel(final ItemStack toolbox, final String toolClass) {
        return ToolboxUtil.getSelectedTool(toolbox)
            .filter(tool -> tool.getItem() instanceof GT_MetaGenerated_Tool)
            .map(
                tool -> tool.getItem()
                    .getHarvestLevel(tool, toolClass))
            .orElse(-1);
    }
    // endregion

    // region Electric Item Functions

    public static Optional<IElectricItemManager> getElectricManager(final ItemStack itemStack) {
        if (itemStack == null) {
            return Optional.empty();
        }

        final Item item = itemStack.getItem();

        IElectricItemManager manager = null;
        if (item instanceof ISpecialElectricItem) {
            manager = ((ISpecialElectricItem) item).getManager(itemStack);
        } else if (item instanceof IElectricItem) {
            manager = ic2.api.item.ElectricItem.manager;
        }

        return Optional.ofNullable(manager);
    }

    private static long getMaxVoltage(int tier) {
        if (tier >= GT_Values.V.length) {
            tier = GT_Values.V.length - 1;
        }

        return GT_Values.V[tier];
    }

    @Override
    public IElectricItemManager getManager(final ItemStack toolbox) {
        return new ToolboxElectricManager();
    }

    @Override
    public boolean canProvideEnergy(final ItemStack itemStack) {
        return false;
    }

    @Override
    public Item getChargedItem(final ItemStack itemStack) {
        return this;
    }

    @Override
    public Item getEmptyItem(final ItemStack itemStack) {
        return this;
    }

    @Override
    public double getMaxCharge(final ItemStack toolbox) {
        return ToolboxUtil.getBattery(toolbox).map(battery -> battery.getItem() instanceof IElectricItem ? ((IElectricItem) battery.getItem()).getMaxCharge(battery) : 0d).orElse(0d);
    }

    @Override
    public double getTransferLimit(final ItemStack toolbox) {
        return ToolboxUtil.getBattery(toolbox).map(battery -> battery.getItem() instanceof IElectricItem ? ((IElectricItem) battery.getItem()).getTransferLimit(battery) : 0d).orElse(0d);
    }

    @Override
    public int getTier(final ItemStack toolbox) {
        return ToolboxUtil.getBattery(toolbox)
            .map(battery -> {
                if (GT_ModHandler.isElectricItem(battery)) {
                    final IElectricItem item = (IElectricItem) battery.getItem();
                    return item == null ? null : item.getTier(battery);
                }

                return null;
            })
            .orElse(0);

    }

    // endregion

    // region Cross-mod Compatibility
    @Override
    public boolean canWrench(final ItemStack toolbox, final EntityPlayer player, final int x, final int y,
        final int z) {
        return ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.WRENCH);
    }

    @Override
    public boolean canWrench(final EntityPlayer entityPlayer, final int x, final int y, final int z) {
        return getToolboxIfEquipped(entityPlayer).map(toolbox -> ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.WRENCH))
            .orElse(false);
    }

    @Override
    public void wrenchUsed(final EntityPlayer entityPlayer, final int x, final int y, final int z) {
    }

    @Override
    public boolean canUse(final ItemStack toolbox, final EntityPlayer player, final int x, final int y, final int z) {
        return ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.WRENCH);
    }

    @Override
    public void used(final ItemStack stack, final EntityPlayer player, final int x, final int y, final int z) {
    }

    @Override
    public boolean shouldHideFacades(final ItemStack toolbox, final EntityPlayer player) {
        return ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.WRENCH);
    }

    @Override
    public boolean canWhack(final EntityPlayer player, final ItemStack toolbox, final int x, final int y, final int z) {
        return ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.CROWBAR);
    }

    @Override
    public void onWhack(final EntityPlayer player, final ItemStack toolbox, final int x, final int y, final int z) {
        ToolboxUtil.damageSelectedTool(toolbox);
    }

    @Override
    public boolean canLink(final EntityPlayer player, final ItemStack toolbox, final EntityMinecart cart) {
        return player.isSneaking() && ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.CROWBAR);
    }

    @Override
    public void onLink(final EntityPlayer player, final ItemStack toolbox, final EntityMinecart cart) {
        ToolboxUtil.damageSelectedTool(toolbox);
    }

    @Override
    public boolean canBoost(final EntityPlayer player, final ItemStack toolbox, final EntityMinecart cart) {
        return !player.isSneaking() && ToolboxUtil.isToolOfType(toolbox, ToolboxSlot.CROWBAR);
    }

    @Override
    public void onBoost(final EntityPlayer player, final ItemStack toolbox, final EntityMinecart cart) {
        ToolboxUtil.damageSelectedTool(toolbox);
    }

    @Override
    public boolean canUse(final EntityPlayer player, final ItemStack stack) {
        return ToolboxUtil.isToolOfType(stack, ToolboxSlot.SCREWDRIVER);
    }

    @Override
    public void damageScrewdriver(final EntityPlayer player, final ItemStack toolbox) {
        ToolboxUtil.damageSelectedTool(toolbox);
    }
    // endregion
}
