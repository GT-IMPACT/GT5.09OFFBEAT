package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.gui.modularui.util.BusHatchInputContainer;
import gregtech.common.items.GT_FluidDisplayItem;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.modularui2.sync.GhostCircuitSyncHandler;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2BusHatchInputGuiTest {

    @Test
    public void nativeWindowPreservesLegacySlotOrderCoordinatesAndReadOnlyTankPresentation() throws Throwable {
        run("layout");
    }

    @Test
    public void pickupShiftAndNeiUseTheOriginalContainerInputGridAndOutputRules() throws Throwable {
        run("inventory");
    }

    @Test
    public void containerProcessingConservesFluidAndDisplayInteractionsCannotChangeTheTank() throws Throwable {
        run("fluid");
    }

    @Test
    public void machineNbtPreservesItemsTankCapacityAndExistingToolSettings() throws Throwable {
        run("persistence");
    }

    @Test
    public void circuitAndModePacketsPreserveItemsAndControlRecipeInsertion() throws Throwable {
        run("controls");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            try {
                runtime.run(getClass(), scenario, "");
            } catch (Throwable failure) {
                // JUnit lazily loads its comparison formatter from this runtime's class loader.
                failure.getMessage();
                throw failure;
            }
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GTGuis.init();
        new GT_FluidDisplayItem();
        new GT_IntegratedCircuit_Item();
        ItemList.Bottle_Empty.set(Items.glass_bottle);
        FluidContainerData water = new FluidContainerData(
            new FluidStack(FluidRegistry.WATER, 1000),
            new ItemStack(Items.water_bucket),
            new ItemStack(Items.bucket)
        );
        FluidContainerRegistry.registerFluidContainer(water);
        GT_Utility.addFluidContainerData(water);
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try (GT_Mui2FluidHatchGuiTest.HeadlessClient ignored = new GT_Mui2FluidHatchGuiTest.HeadlessClient()) {
            Fixture fixture = new Fixture();
            if ("layout".equals(scenario)) {
                checkLayout(fixture);
            } else if ("inventory".equals(scenario)) {
                checkInventory(fixture);
            } else if ("fluid".equals(scenario)) {
                checkFluid(fixture);
            } else if ("persistence".equals(scenario)) {
                checkPersistence(fixture);
            } else if ("controls".equals(scenario)) {
                checkControls(fixture);
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            Thread.currentThread().setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) {
        assertTrue(fixture.machine.useMui2());
        assertEquals(176, fixture.panel.getArea().width);
        assertEquals(166, fixture.panel.getArea().height);
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(fixture.container.canInteractWith(fixture.player));
        assertEquals(54, fixture.container.inventorySlots.size());
        assertNotNull(fixture.handlers.findPanelHandlerNullable("ghostCircuitPanel"));
        assertEquals(20, fixture.machine.mInventory.length);
        boolean[] playerSlots = new boolean[36];
        for (int index = 0; index < 54; index++) {
            ModularSlot slot = (ModularSlot) fixture.container.inventorySlots.get(index);
            if (index < 18) {
                assertSame(fixture.machine.getInventoryHandler(), slot.getItemHandler());
                assertEquals(index, slot.getSlotIndex());
                if (index == 0 || index == 17) {
                    assertEquals(index == 0 ? 8 : 44, slot.xDisplayPosition);
                    assertEquals(62, slot.yDisplayPosition);
                } else {
                    assertEquals(80 + 18 * ((index - 1) % 4), slot.xDisplayPosition);
                    assertEquals(8 + 18 * ((index - 1) / 4), slot.yDisplayPosition);
                }
            } else {
                assertEquals("player_inventory", slot.getSlotGroupName());
                int playerSlot = slot.getSlotIndex();
                assertTrue(playerSlot >= 0 && playerSlot < 36);
                assertFalse(playerSlots[playerSlot]);
                playerSlots[playerSlot] = true;
                assertEquals(8 + 18 * (playerSlot % 9), slot.xDisplayPosition);
                assertEquals(playerSlot < 9 ? 142 : 84 + 18 * (playerSlot / 9 - 1), slot.yDisplayPosition);
            }
        }
        checkFrame(fixture, fixture.itemWidget(0), 7, 61);
        for (int index = 1; index <= 16; index++) {
            checkFrame(
                fixture,
                fixture.itemWidget(index),
                79 + 18 * ((index - 1) % 4),
                7 + 18 * ((index - 1) / 4)
            );
        }
        checkFrame(fixture, fixture.itemWidget(17), 43, 61);
        checkFrame(fixture, fixture.display, 54, 31);
        checkFrame(fixture, fixture.widget("circuit"), 61, 61);
        checkFrame(fixture, fixture.widget("sorting"), 153, 7);
        checkFrame(fixture, fixture.widget("insertion_limit"), 153, 25);
        assertFalse(fixture.display.getSyncHandler().isPhantom());
        assertFalse(fixture.display.getSyncHandler().canFillSlot());
        assertFalse(fixture.display.getSyncHandler().canDrainSlot());
        assertFalse(fixture.machineSlot(17).isCanPut());
        assertTrue(fixture.machineSlot(17).isCanTake());
        assertNull(fixture.findMachineSlot(18));
        assertNull(fixture.findMachineSlot(19));
        int items = 0;
        int displays = 0;
        int circuits = 0;
        int toggles = 0;
        List<IWidget> frames = new ArrayList<>();
        for (IWidget widget : fixture.widgets) {
            if (widget instanceof GhostCircuitSlotWidget) {
                circuits++;
            }
            if (widget instanceof ToggleButton) {
                toggles++;
            }
            if (widget instanceof FluidSlot) {
                displays++;
            }
            if (!(widget instanceof ItemSlot) && !(widget instanceof FluidSlot)
                    && !(widget instanceof GhostCircuitSlotWidget) && !(widget instanceof ToggleButton)) {
                continue;
            }
            if (widget instanceof ItemSlot) {
                items++;
            }
            Area area = widget.getArea();
            String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, 0);
            assertTrue(
                "Unresolved " + details,
                widget.resizer().isXCalculated() && widget.resizer().isYCalculated()
                    && widget.resizer().isWidthCalculated() && widget.resizer().isHeightCalculated()
            );
            assertTrue("Left " + details, area.x >= fixture.panel.getArea().x);
            assertTrue("Top " + details, area.y >= fixture.panel.getArea().y);
            assertTrue("Right " + details, area.ex() <= fixture.panel.getArea().ex());
            assertTrue("Bottom " + details, area.ey() <= fixture.panel.getArea().ey());
            for (IWidget other : frames) {
                assertFalse(
                    "Intersecting " + details + " with " + GT_Mui2InputBusTest.geometry(other, fixture.panel, 0),
                    area.intersects(other.getArea())
                );
            }
            frames.add(widget);
        }
        assertEquals(55, items);
        assertEquals(1, displays);
        assertEquals(1, circuits);
        assertEquals(2, toggles);
        fixture.player.inventory.setItemStack(null);
        fixture.player.inventory.setInventorySlotContents(fixture.player.inventory.currentItem, null);
        for (byte side = 0; side < 6; side++) {
            assertTrue(fixture.tile.onRightclick(fixture.player, side, 0.5F, 0.5F, 0.5F));
        }
        fixture.world.isRemote = true;
        try {
            assertTrue(fixture.machine.onRightclick(fixture.tile, fixture.player));
            assertTrue(GTGuis.open(fixture.player, fixture.machine));
        } finally {
            fixture.world.isRemote = false;
        }
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkInventory(Fixture fixture) {
        for (int index = 0; index < 18; index++) {
            ItemSlot widget = fixture.itemWidget(index);
            assertTrue(((IVanillaSlot) widget).handleAsVanillaSlot());
            assertSame(fixture.machineSlot(index), ((IVanillaSlot) widget).getVanillaSlot());
        }
        ItemStack ordinary = new ItemStack(Items.iron_ingot, 20);
        fixture.shiftFromPlayer(ordinary);
        assertNull(fixture.machine.mInventory[0]);
        assertSame(Items.iron_ingot, fixture.machine.mInventory[1].getItem());
        assertEquals(20, fixture.machine.mInventory[1].stackSize);
        fixture.container.slotClick(fixture.machineSlot(1).slotNumber, 0, 1, fixture.player);
        assertNull(fixture.machine.mInventory[1]);
        assertSame(Items.iron_ingot, fixture.player.inventory.getStackInSlot(8).getItem());
        assertEquals(20, fixture.player.inventory.getStackInSlot(8).stackSize);

        fixture.shiftFromPlayer(new ItemStack(Items.water_bucket));
        assertSame(Items.water_bucket, fixture.machine.mInventory[0].getItem());
        assertNull(fixture.machine.mInventory[1]);
        fixture.container.slotClick(fixture.machineSlot(0).slotNumber, 0, 1, fixture.player);
        assertNull(fixture.machine.mInventory[0]);
        assertSame(Items.water_bucket, fixture.player.inventory.getStackInSlot(7).getItem());

        ItemStack blocked = new ItemStack(Items.stick);
        fixture.machine.mInventory[0] = blocked;
        fixture.shiftFromPlayer(new ItemStack(Items.water_bucket));
        assertSame(blocked, fixture.machine.mInventory[0]);
        assertSame(Items.water_bucket, fixture.machine.mInventory[1].getItem());

        fixture.machine.mInventory[0] = null;
        ItemStack manual = new ItemStack(Items.apple, 4);
        fixture.player.inventory.setItemStack(manual);
        fixture.container.slotClick(fixture.machineSlot(0).slotNumber, 0, 0, fixture.player);
        assertNull(fixture.player.inventory.getItemStack());
        assertSame(Items.apple, fixture.machine.mInventory[0].getItem());
        assertEquals(4, fixture.machine.mInventory[0].stackSize);
        fixture.machine.restrictFluid = true;
        fixture.player.inventory.setItemStack(new ItemStack(Items.water_bucket));
        fixture.container.slotClick(fixture.machineSlot(2).slotNumber, 0, 0, fixture.player);
        assertNull(fixture.player.inventory.getItemStack());
        assertSame(Items.water_bucket, fixture.machine.mInventory[2].getItem());
        fixture.shiftFromPlayer(new ItemStack(Items.apple, 3));
        assertEquals(4, fixture.machine.mInventory[0].stackSize);
        assertSame(Items.apple, fixture.machine.mInventory[3].getItem());
        assertEquals(3, fixture.machine.mInventory[3].stackSize);

        ItemStack rejected = new ItemStack(Items.bucket, 3);
        fixture.player.inventory.setItemStack(rejected);
        fixture.container.slotClick(fixture.machineSlot(17).slotNumber, 0, 0, fixture.player);
        assertNull(fixture.machine.mInventory[17]);
        assertSame(rejected, fixture.player.inventory.getItemStack());
        fixture.player.inventory.setItemStack(null);
        fixture.machine.mInventory[17] = new ItemStack(Items.bucket, 2);
        fixture.container.slotClick(fixture.machineSlot(17).slotNumber, 0, 1, fixture.player);
        assertNull(fixture.machine.mInventory[17]);
        assertNull(fixture.findMachineSlot(18));
    }

    private static void checkFluid(Fixture fixture) throws Exception {
        fixture.player.inventory.setItemStack(new ItemStack(Items.water_bucket));
        fixture.container.slotClick(fixture.machineSlot(0).slotNumber, 0, 0, fixture.player);
        fixture.machine.onPreTick(fixture.tile, 1);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals(1000, fixture.machine.getFluidAmount());
        assertSame(FluidRegistry.WATER, fixture.machine.mFluid.getFluid());
        assertSame(Items.bucket, fixture.machine.mInventory[17].getItem());
        fixture.container.slotClick(fixture.machineSlot(17).slotNumber, 0, 0, fixture.player);
        ItemStack cursor = fixture.player.inventory.getItemStack();
        assertSame(Items.bucket, cursor.getItem());
        FluidSlotSyncHandler display = fixture.display.getSyncHandler();
        for (int button = 0; button <= 1; button++) {
            PacketBuffer click = new PacketBuffer(Unpooled.buffer());
            try {
                new MouseData(Side.SERVER, button, false, false, false).writeToPacket(click);
                display.readOnServer(FluidSlotSyncHandler.SYNC_CLICK, click);
            } finally {
                click.release();
            }
            assertEquals(1000, fixture.machine.getFluidAmount());
            assertSame(cursor, fixture.player.inventory.getItemStack());
            assertEquals(1, cursor.stackSize);
        }
        PacketBuffer forged = new PacketBuffer(Unpooled.buffer());
        try {
            NetworkUtils.writeFluidStack(forged, new FluidStack(FluidRegistry.LAVA, 40000));
            display.readOnServer(ValueSyncHandler.SYNC_VALUE, forged);
        } finally {
            forged.release();
        }
        assertEquals(1000, fixture.machine.getFluidAmount());
        assertSame(FluidRegistry.WATER, fixture.machine.mFluid.getFluid());
        ItemStack dragged = GT_Utility.getFluidDisplayStack(FluidRegistry.LAVA);
        assertFalse(fixture.display.handleDragAndDrop(dragged, 0));
        assertEquals(1, dragged.stackSize);
        assertEquals(1000, fixture.machine.getFluidAmount());
        display.updateCacheFromSource(false);
        assertEquals(1000, display.getValue().amount);
        TextWidget<?> amount = (TextWidget<?>) fixture.widget("fluid_amount");
        assertEquals(GT_Utility.parseNumberToString(1000),
            EnumChatFormatting.getTextWithoutFormattingCodes(amount.getKey().getFormatted()));

        fixture.container.slotClick(fixture.machineSlot(0).slotNumber, 0, 0, fixture.player);
        assertNull(fixture.player.inventory.getItemStack());
        fixture.machine.onPreTick(fixture.tile, 2);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals(0, fixture.machine.getFluidAmount());
        assertSame(Items.water_bucket, fixture.machine.mInventory[17].getItem());
        assertEquals(1, fixture.machine.mInventory[17].stackSize);
        for (int index = 1; index <= 16; index++) {
            assertNull(fixture.machine.mInventory[index]);
        }
    }

    private static void checkControls(Fixture fixture) throws Exception {
        GhostCircuitSlotWidget circuit = (GhostCircuitSlotWidget) fixture.widget("circuit");
        ItemStack cursor = new ItemStack(Items.apple, 3);
        fixture.player.inventory.setItemStack(cursor);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeShort(26);
            circuit.getSyncHandler().readOnServer(GhostCircuitSyncHandler.SYNC_CIRCUIT_CONFIG, packet);
        } finally {
            packet.release();
        }
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(3, cursor.stackSize);
        GhostCircuitItemStackHandler ghost = new GhostCircuitItemStackHandler(fixture.machine);
        assertEquals(26, ghost.getCircuitConfig());
        assertEquals(0, ghost.getStackInSlot(0).stackSize);
        assertNull(fixture.machine.decrStackSize(fixture.machine.getCircuitSlot(), 1));
        assertNull(fixture.machine.getInventoryHandler().extractItem(fixture.machine.getCircuitSlot(), 1, false));
        sendBoolean((ToggleButton) fixture.widget("sorting"), false);
        assertTrue(fixture.machine.disableSort);
        fixture.machine.mInventory[5] = new ItemStack(Items.iron_ingot, 4);
        fixture.machine.updateSlots();
        assertNotNull(fixture.machine.mInventory[5]);
        sendBoolean((ToggleButton) fixture.widget("sorting"), true);
        fixture.machine.updateSlots();
        assertEquals(4, fixture.machine.mInventory[1].stackSize);
        assertNull(fixture.machine.mInventory[5]);
        sendBoolean((ToggleButton) fixture.widget("insertion_limit"), true);
        assertFalse(fixture.machine.disableLimited);
        assertTrue(fixture.machine.allowPutStack(fixture.tile, 1, (byte) 2, new ItemStack(Items.iron_ingot)));
        assertFalse(fixture.machine.allowPutStack(fixture.tile, 2, (byte) 2, new ItemStack(Items.iron_ingot)));
        assertTrue(fixture.machine.allowPutStack(fixture.tile, 2, (byte) 2, new ItemStack(Items.gold_ingot)));
        sendBoolean((ToggleButton) fixture.widget("insertion_limit"), false);
        assertTrue(fixture.machine.allowPutStack(fixture.tile, 2, (byte) 2, new ItemStack(Items.iron_ingot)));
        assertEquals(26, ghost.getCircuitConfig());
    }

    private static void sendBoolean(ToggleButton toggle, boolean value) throws Exception {
        BooleanSyncValue handler = (BooleanSyncValue) toggle.getSyncHandler();
        assertTrue(handler.isAllowC2S());
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeBoolean(value);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void checkPersistence(Fixture fixture) {
        fixture.player.setSneaking(true);
        fixture.machine.onScrewdriverRightClick((byte) 2, fixture.player, 0.5F, 0.5F, 0.5F);
        assertTrue(fixture.machine.disableSort);
        assertTrue(fixture.machine.onSolderingToolRightClick((byte) 2, (byte) 2, fixture.player, 0.5F, 0.5F, 0.5F));
        assertTrue(fixture.machine.restrictFluid);
        ItemStack grid = new ItemStack(Items.gold_ingot, 5);
        grid.setTagCompound(new NBTTagCompound());
        grid.getTagCompound().setString("preserved", "combined_input");
        fixture.machine.mInventory[0] = new ItemStack(Items.water_bucket);
        fixture.machine.mInventory[1] = grid;
        fixture.machine.mInventory[17] = new ItemStack(Items.bucket, 7);
        fixture.machine.mFluid = new FluidStack(FluidRegistry.WATER, 27000);
        fixture.machine.mInventory[18] = GT_Utility.getFluidDisplayStack(fixture.machine.mFluid, false);
        new GhostCircuitItemStackHandler(fixture.machine).setCircuitConfig(26);
        fixture.machine.disableLimited = false;
        assertEquals(2, GTMTE_BusHatch_Input.getStoredInputs(fixture.machine).size());
        assertSame(grid, GTMTE_BusHatch_Input.getStoredInputs(fixture.machine).get(1));

        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_bus_hatch_input_test");
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[32000];
        GregTech_API.METATILEENTITIES[32000] = fixture.machine;
        fixture.tile.setMetaTileID((short) 32000);
        try {
            NBTTagCompound saved = new NBTTagCompound();
            fixture.tile.writeToNBT(saved);
            BaseMetaTileEntity tile = new BaseMetaTileEntity();
            tile.setWorldObj(fixture.world);
            tile.readFromNBT(saved);
            GTMTE_BusHatch_Input restored = (GTMTE_BusHatch_Input) tile.getMetaTileEntity();
            assertNotNull(restored);
            assertTrue(restored.useMui2());
            assertEquals(20, restored.mInventory.length);
            assertEquals(32000, restored.getCapacity());
            assertTrue(restored.disableSort);
            assertTrue(restored.restrictFluid);
            assertFalse(restored.disableLimited);
            GhostCircuitItemStackHandler ghost = new GhostCircuitItemStackHandler(restored);
            assertEquals(26, ghost.getCircuitConfig());
            assertEquals(0, ghost.getStackInSlot(0).stackSize);
            assertEquals(27000, restored.getFluidAmount());
            assertSame(FluidRegistry.WATER, restored.mFluid.getFluid());
            assertSame(Items.water_bucket, restored.mInventory[0].getItem());
            assertEquals(5, restored.mInventory[1].stackSize);
            assertEquals("combined_input", restored.mInventory[1].getTagCompound().getString("preserved"));
            assertSame(Items.bucket, restored.mInventory[17].getItem());
            assertEquals(7, restored.mInventory[17].stackSize);
            assertEquals(2, GTMTE_BusHatch_Input.getStoredInputs(restored).size());
            assertSame(ghost.getStackInSlot(0), GTMTE_BusHatch_Input.getStoredInputs(restored).get(0));
            assertSame(restored.mInventory[1], GTMTE_BusHatch_Input.getStoredInputs(restored).get(1));
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previous;
        }
    }

    private static void checkFrame(Fixture fixture, IWidget widget, int x, int y) {
        assertEquals(fixture.panel.getArea().x + x, widget.getArea().x);
        assertEquals(fixture.panel.getArea().y + y, widget.getArea().y);
        assertEquals(18, widget.getArea().width);
        assertEquals(18, widget.getArea().height);
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static final class Fixture {

        private final GTMTE_BusHatch_Input machine = new GTMTE_BusHatch_Input(
            "mui2_bus_hatch_input",
            4,
            new String[0],
            null,
            32000
        );
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final NativePlayer player;
        private final PosGuiData data;
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final NativeContainer container = new NativeContainer();
        private final List<IWidget> widgets = new ArrayList<>();
        private FluidSlot display;

        private Fixture() throws Exception {
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new NativePlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            data = new PosGuiData(player, 4, 64, 7);
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            assertTrue(settings.hasCustomContainer());
            assertEquals(BusHatchInputContainer.class, settings.createContainer().getClass());
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(player, sync, settings, panel.getName(), data);
            player.openContainer = container;
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            collect(panel);
            assertNotNull(display);
            GT_Mui2FluidHatchGuiTest.freezeTranslationCache(panel);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            if (widget.isName("fluid_display")) {
                display = (FluidSlot) widget;
            }
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private IWidget widget(String name) {
            for (IWidget widget : widgets) {
                if (widget.isName(name)) {
                    return widget;
                }
            }
            throw new AssertionError("No widget " + name);
        }

        private ItemSlot itemWidget(int index) {
            return (ItemSlot) widget(index == 0 ? "container_input"
                : index == 17 ? "container_output" : "input_" + index);
        }

        private ModularSlot findMachineSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            return null;
        }

        private ModularSlot machineSlot(int index) {
            ModularSlot slot = findMachineSlot(index);
            assertNotNull(slot);
            return slot;
        }

        private ModularSlot playerSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() != machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            throw new AssertionError("No player slot " + index);
        }

        private void shiftFromPlayer(ItemStack stack) {
            player.inventory.setInventorySlotContents(9, stack);
            player.inventory.setItemStack(null);
            container.slotClick(playerSlot(9).slotNumber, 0, 1, player);
            assertNull(player.inventory.getStackInSlot(9));
        }
    }

    private static final class NativeContainer extends BusHatchInputContainer implements ContainerAccessor {

        private final Field dragEvent;

        private NativeContainer() throws Exception {
            dragEvent = Container.class.getDeclaredField("field_94536_g");
            dragEvent.setAccessible(true);
        }

        @Override
        public int getDragEvent() {
            try {
                return dragEvent.getInt(this);
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static final class Viewport extends StaticResizer {

        private final Area area = new Area(0, 0, 600, 600);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Legacy combined input hatch viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }

    private static final class NativePlayer extends GT_StructureIntegrationTest.TestPlayer {

        private NativePlayer(World world) {
            super(world);
        }

        @Override
        public void openGui(
            Object mod,
            int guiId,
            World world,
            int x,
            int y,
            int z
        ) {
            throw new AssertionError("Combined input hatch attempted to open a legacy GUI");
        }
    }
}
