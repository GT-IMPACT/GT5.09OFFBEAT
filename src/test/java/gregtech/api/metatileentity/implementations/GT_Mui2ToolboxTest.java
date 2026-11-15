package gregtech.api.metatileentity.implementations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.network.ModularNetwork;
import com.cleanroommc.modularui.network.NetworkHandler;
import com.cleanroommc.modularui.network.packets.OpenGuiPacket;
import com.cleanroommc.modularui.network.packets.PacketSyncHandler;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.asm.transformers.ModAPITransformer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.asm.ASMModParser;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enchants.Enchantment_Radioactivity;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.ToolboxSlot;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.net.GT_Packet_ToolboxEvent;
import gregtech.api.util.GT_Config;
import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;
import gregtech.common.items.toolbox.ToolboxUtil;
import gregtech.common.tools.GT_Tool_Screwdriver_LV;
import gregtech.common.tools.GT_Tool_Wrench_LV;
import io.netty.channel.embedded.EmbeddedChannel;

public class GT_Mui2ToolboxTest {

    @Test
    public void inventoryLocksHeldToolboxAndPersistsOnlyRealChanges() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "open", "");
        }
    }

    @Test
    public void rightClickOpensInventoryAfterPlacementHandlerCopiesHeldStack() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "rightClick", "");
        }
    }

    @Test
    public void vanillaClosePersistsToolboxContents() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "vanillaClose", "");
        }
    }

    @Test
    public void logoutPersistsOpenToolboxContentsBeforePlayerSave() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "logout", "");
        }
    }

    @Test
    public void toolboxesUseChargedElectricToolsForMaintenance() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "electricMaintenance", "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GT_ProcessingTestSupport.setField(Loader.instance(), "namedMods", new HashMap<String, ModContainer>());

        Launch.classLoader.registerTransformer(ModAPITransformer.class.getName());
        ASMDataTable optionalApis = new ASMDataTable();
        for (String resource : new String[] {
            "gregtech/common/items/ItemGTToolbox.class",
            "gregtech/api/items/GT_MetaGenerated_Tool.class"
        }) {
            try (InputStream bytes = Launch.classLoader.getResourceAsStream(resource)) {
                new ASMModParser(bytes).sendToTable(optionalApis, null);
            }
        }
        for (Object transformer : Launch.classLoader.getTransformers()) {
            if (transformer instanceof ModAPITransformer) {
                ((ModAPITransformer) transformer).initTable(optionalApis);
            }
        }

        GTGuis.init();
        if (scenario.equals("electricMaintenance")) {
            checkElectricMaintenance();
            return;
        }
        if (!scenario.equals("open")) {
            checkRightClick(scenario);
            return;
        }

        EntityPlayer player = new GT_StructureIntegrationTest.TestPlayer(new GT_ProcessingTestSupport.ProcessingWorld());
        ItemStack toolbox = new ItemStack(new ItemGTToolbox("test_toolbox", "Test Toolbox", ""));
        int hotbarSlot = 3;
        player.inventory.currentItem = hotbarSlot;
        player.inventory.setInventorySlotContents(hotbarSlot, toolbox);

        PlayerInventoryGuiData data = new PlayerInventoryGuiData(player, InventoryTypes.PLAYER, hotbarSlot);
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager handlers = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        settings.defaultCanInteractWith(GuiFactories.playerInventory(), data);
        ModularPanel panel = GuiFactories.playerInventory().createPanel(data, handlers, settings);
        WidgetTree.collectSyncValues(handlers, panel);
        ModularContainer container = settings.createContainer();
        container.construct(player, sync, settings, panel.getName(), data);
        player.openContainer = container;

        ModularSlot wrench = findSlot(container, ToolboxSlot.WRENCH.getSlotID(), false);
        ModularSlot general = findSlot(container, ToolboxSlot.GENERIC_SLOT0.getSlotID(), false);
        ModularSlot held = findSlot(container, hotbarSlot, true);
        ItemStack sticks = new ItemStack(Items.stick, 8);
        assertEquals(50, container.inventorySlots.size());
        assertFalse(ItemGTToolbox.isInventoryOpen(toolbox));
        assertFalse(wrench.isItemValid(sticks));
        assertFalse(general.isItemValid(toolbox));
        assertFalse(held.canTakeStack(player));
        assertFalse(held.isItemValid(sticks));

        assertTrue(general.isItemValid(sticks));
        general.putStack(sticks);
        assertTrue(general.isItemValid(sticks));
        assertEquals(64, general.getItemStackLimit(sticks));
        assertSame(sticks, general.getStack());
        assertFalse(ToolboxUtil.getItemInside(toolbox, ToolboxSlot.GENERIC_SLOT0).isPresent());
        assertEquals(8, general.decrStackSize(8).stackSize);
        assertNull(general.getStack());
        assertFalse(ToolboxUtil.getItemInside(toolbox, ToolboxSlot.GENERIC_SLOT0).isPresent());
    }

    private static void checkRightClick(String scenario) throws Exception {
        NetworkHandler.CHANNEL.registerMessage((message, context) -> null, OpenGuiPacket.class, 0, Side.CLIENT);
        NetworkHandler.CHANNEL.registerMessage((message, context) -> null, PacketSyncHandler.class, 1, Side.CLIENT);
        World world = new GT_ProcessingTestSupport.ProcessingWorld();
        ToolboxPlayer player = createServerPlayer(world);
        try (Connection connection = new Connection(player)) {
            ItemGTToolbox item = new ItemGTToolbox("test_toolbox", "Test Toolbox", "");
            ItemStack held = new ItemStack(item);
            player.inventory.setInventorySlotContents(0, held);
            held.useItemRightClick(world, player);
            assertTrue(player.openContainer instanceof ModularContainer);
            ModularContainer container = (ModularContainer) player.openContainer;

            ItemStack replacement = held.copy();
            player.inventory.setInventorySlotContents(0, replacement);
            GT_Packet_ToolboxEvent.inventoryOpened(0).queueEvent(player);
            GT_Packet_ToolboxEvent.applyEvent(player);

            assertSame(container, player.openContainer);
            assertTrue(container.canInteractWith(player));
            assertTrue(ItemGTToolbox.isInventoryOpen(replacement));
            assertFalse(ItemGTToolbox.isInventoryOpen(held));

            player.inventory.setInventorySlotContents(9, new ItemStack(Items.stick, 8));
            ModularSlot source = findSlot(container, 9, true);
            container.transferStackInSlot(player, source.slotNumber);
            assertNull(player.inventory.getStackInSlot(9));
            assertEquals(8, findSlot(container, ToolboxSlot.GENERIC_SLOT0.getSlotID(), false).getStack().stackSize);
            assertFalse(ToolboxUtil.getItemInside(replacement, ToolboxSlot.GENERIC_SLOT0).isPresent());
            if (scenario.equals("vanillaClose")) {
                player.playerNetServerHandler.processCloseWindow(new C0DPacketCloseWindow(container.windowId));
            } else if (scenario.equals("logout")) {
                GT_Mod.gregtechproxy.onToolboxPlayerLogout(new PlayerLoggedOutEvent(player));
            } else {
                ModularNetwork.SERVER.closeContainer(0, true, player, false);
            }
            assertEquals(8, ToolboxUtil.getItemInside(replacement, ToolboxSlot.GENERIC_SLOT0).get().stackSize);
            assertFalse(ItemGTToolbox.isInventoryOpen(replacement));

            if (scenario.equals("vanillaClose")) {
                // A subsequent ModularUI close must not replay an already committed inventory.
                ToolboxUtil.saveItemInside(replacement, null, ToolboxSlot.GENERIC_SLOT0);
                ModularNetwork.SERVER.closeContainer(0, true, player, false);
                assertFalse(ToolboxUtil.getItemInside(replacement, ToolboxSlot.GENERIC_SLOT0).isPresent());
            }
        }
    }

    private static void checkElectricMaintenance() {
        GT_Config.sConfigFileIDs = GregTech_API.sMachineFile.mConfig;
        new Enchantment_Radioactivity();
        GT_MetaGenerated_Tool tools = new GT_MetaGenerated_Tool("test_maintenance_tools") {
        };
        GregTech_API.registerWrench(tools.addTool(120, "LV wrench", "", new GT_Tool_Wrench_LV()));
        GregTech_API.registerScrewdriver(tools.addTool(150, "LV screwdriver", "", new GT_Tool_Screwdriver_LV()));
        ItemList.Tool_NoteBook.set(Items.book);

        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        EntityPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        ItemStack toolbox = new ItemStack(new ItemGTToolbox("test_toolbox", "Test Toolbox", ""));
        player.inventory.setInventorySlotContents(0, toolbox);

        long[] electricStats = { 100_000, 32, 1, -1 };
        ItemStack wrench = tools.getToolWithStats(120, 1, Materials.Steel, Materials.StainlessSteel, electricStats);
        ItemStack screwdriver = tools.getToolWithStats(150, 1, Materials.Steel, Materials.StainlessSteel, electricStats);
        tools.setCharge(wrench, 0);
        tools.setCharge(screwdriver, 0);
        assertTrue(ToolboxSlot.WRENCH.test(wrench));
        assertTrue(ToolboxSlot.SCREWDRIVER.test(screwdriver));
        assertFalse(ToolboxSlot.WRENCH.test(screwdriver));
        tools.setCharge(wrench, 1_000);
        tools.setCharge(screwdriver, 1_000);

        ToolboxItemStackHandler contents = new ToolboxItemStackHandler(toolbox);
        contents.setStackInSlot(ToolboxSlot.WRENCH.getSlotID(), wrench);
        contents.setStackInSlot(ToolboxSlot.SCREWDRIVER.getSlotID(), screwdriver);
        ToolboxUtil.saveToolbox(toolbox, contents, tag ->
            tag.setInteger(ItemGTToolbox.CURRENT_TOOL_KEY, ToolboxSlot.WRENCH.getSlotID()));

        GT_MetaTileEntity_Hatch_Maintenance hatch = new GT_MetaTileEntity_Hatch_Maintenance(
            "test_maintenance", 1, "", null, false);
        GT_StructureIntegrationTest.TestTile tile =
            new GT_StructureIntegrationTest.TestTile(world, hatch, ForgeDirection.NORTH);
        assertTrue(tile.onRightclick(player, (byte) ForgeDirection.NORTH.ordinal(), 0.5F, 0.5F, 0.5F));
        assertTrue(hatch.mWrench);
        assertTrue(hatch.mScrewdriver);
        assertEquals(900, tools.getRealCharge(ToolboxUtil.getItemInside(toolbox, ToolboxSlot.WRENCH).get()));
        assertEquals(900, tools.getRealCharge(ToolboxUtil.getItemInside(toolbox, ToolboxSlot.SCREWDRIVER).get()));
    }

    private static ToolboxPlayer createServerPlayer(World world) throws Exception {
        Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
        Constructor<?> constructor = (Constructor<?>) factoryClass
            .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
            .invoke(factory, ToolboxPlayer.class, EntityPlayer.class.getConstructor(World.class, GameProfile.class));
        constructor.setAccessible(true);
        return (ToolboxPlayer) constructor.newInstance(world, new GameProfile(UUID.randomUUID(), "ToolboxPlayer"));
    }

    private static final class ToolboxPlayer extends EntityPlayerMP {

        private ToolboxPlayer() {
            super(null, null, null, null);
        }
    }

    private static final class Connection implements AutoCloseable {

        private final EmbeddedChannel channel;

        private Connection(EntityPlayerMP player) {
            NetworkManager network = new NetworkManager(false);
            channel = new EmbeddedChannel(network);
            new NetHandlerPlayServer(null, network, player);
        }

        @Override
        public void close() {
            channel.finish();
        }
    }

    private static ModularSlot findSlot(ModularContainer container, int index, boolean playerSlot) {
        for (Object entry : container.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot instanceof ModularSlot) {
                ModularSlot modular = (ModularSlot) slot;
                boolean toolboxSlot = modular.getItemHandler() instanceof ToolboxItemStackHandler;
                if (slot.getSlotIndex() == index && toolboxSlot != playerSlot) {
                    return modular;
                }
            }
        }
        throw new AssertionError("Missing container slot " + index);
    }
}
