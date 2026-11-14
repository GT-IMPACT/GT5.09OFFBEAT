package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.drawable.text.LangKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.fluid.FluidInteractions;
import com.cleanroommc.modularui.value.sync.ByteSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_Utility;
import gregtech.common.items.GT_FluidDisplayItem;
import gregtech.common.modularui2.widget.FluidLockSlotWidget;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2FluidHatchGuiTest {

    @Test
    public void standardHatchesUseNativePanelsAndKeepEveryVisibleControlInsideItsBounds() throws Throwable {
        run("layout");
    }

    @Test
    public void nativeContainerSlotsSupportPickupShiftAndNeiWithoutExposingOldDisplayItems() throws Throwable {
        run("inventory");
    }

    @Test
    public void nativeModePacketsCycleAllTenModesAndPreserveTheLockedFluidOnReload() throws Throwable {
        run("mode");
    }

    @Test
    public void phantomFluidLockSamplesRealDisplayItemsAndContainersWithoutCreatingFluid() throws Throwable {
        run("lock");
    }

    @Test
    public void liveTankClicksConserveContainerFluidAndRejectClientTankValueReplacement() throws Throwable {
        run("tank");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GTGuis.init();
        new GT_FluidDisplayItem();
        FluidContainerRegistry.registerFluidContainer(
            new FluidStack(FluidRegistry.WATER, 1000),
            new ItemStack(Items.water_bucket),
            new ItemStack(Items.bucket)
        );
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try (HeadlessClient ignored = new HeadlessClient()) {
            if ("layout".equals(scenario)) {
                for (int tier : new int[]{0, 1, 4, 8, 9}) {
                    checkLayout(new Fixture(false, tier));
                    checkLayout(new Fixture(true, tier));
                }
                checkMultiInputLayout(4, 4, 64_000);
                checkMultiInputLayout(9, 5, 128_000);
            } else if ("inventory".equals(scenario)) {
                checkInventory(new Fixture(false, 1));
                checkInventory(new Fixture(true, 1));
            } else if ("mode".equals(scenario)) {
                checkMode(new Fixture(true, 1));
            } else if ("lock".equals(scenario)) {
                checkLock(new Fixture(true, 1));
            } else if ("tank".equals(scenario)) {
                checkTank(new Fixture(false, 1));
                checkTank(new Fixture(true, 1));
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            Thread.currentThread().setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) {
        assertTrue(fixture.machine.useMui2());
        assertNull(fixture.handlers.findSyncHandlerNullable("powerSwitch", 0));
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(MetaTileEntityGuiHandler.INSTANCE.canInteractWith(fixture.player, fixture.data));
        assertEquals(38, fixture.container.inventorySlots.size());
        assertEquals(176, fixture.panel.getArea().width);
        assertEquals(175, fixture.panel.getArea().height);
        int physicalSlots = 0;
        int playerSlots = 0;
        for (Object entry : fixture.container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            if (slot.getItemHandler() == fixture.machine.getInventoryHandler()) {
                assertTrue(slot.getSlotIndex() == 0 || slot.getSlotIndex() == 1);
                physicalSlots++;
            } else {
                assertEquals("player_inventory", slot.getSlotGroupName());
                playerSlots++;
            }
        }
        assertEquals(2, physicalSlots);
        assertEquals(36, playerSlots);
        assertFalse(fixture.outputSlot.isCanPut());
        assertTrue(fixture.outputSlot.isCanTake());
        assertTrue(fixture.inputSlot.isCanPut() && fixture.inputSlot.isCanTake());
        assertFalse(fixture.tank.getSyncHandler().isPhantom());
        assertTrue(fixture.tank.getSyncHandler().canFillSlot());
        assertTrue(fixture.tank.getSyncHandler().canDrainSlot());

        List<IWidget> frames = new ArrayList<>();
        int screens = 0;
        int fluidSlots = 0;
        for (IWidget widget : fixture.widgets) {
            if (widget.getArea().width == 71 && widget.getArea().height == 54) {
                screens++;
            }
            if (widget instanceof FluidSlot) {
                fluidSlots++;
            }
            if (widget instanceof ItemSlot || widget instanceof FluidSlot || widget == fixture.modeButton) {
                Area area = widget.getArea();
                String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, 0);
                assertTrue("Unresolved " + details,
                    widget.resizer().isXCalculated() && widget.resizer().isYCalculated()
                        && widget.resizer().isWidthCalculated() && widget.resizer().isHeightCalculated());
                assertTrue("Left " + details, area.x >= fixture.panel.getArea().x);
                assertTrue("Top " + details, area.y >= fixture.panel.getArea().y);
                assertTrue("Right " + details, area.ex() <= fixture.panel.getArea().ex());
                assertTrue("Bottom " + details, area.ey() <= fixture.panel.getArea().ey());
                for (IWidget other : frames) {
                    assertFalse("Intersecting " + details + " with "
                        + GT_Mui2InputBusTest.geometry(other, fixture.panel, 0), area.intersects(other.getArea()));
                }
                frames.add(widget);
            }
        }
        assertEquals(fixture.output ? 2 : 1, screens);
        assertEquals(2, fluidSlots);
        assertEquals(fixture.output ? 41 : 40, frames.size());
        if (fixture.output) {
            assertNotNull(fixture.lock);
            assertNotNull(fixture.modeButton);
            assertEquals(10, fixture.modeButton.getStateCount());
            assertTrue(fixture.lock.getSyncHandler().isPhantom());
        } else {
            assertNull(fixture.lock);
            assertNull(fixture.modeButton);
        }
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkMultiInputLayout(int slotCount, int tier, int capacity) throws Exception {
        GTMTE_Multi_Hatch_Input hatch = new GTMTE_Multi_Hatch_Input(
            "mui2_multi_input", slotCount, tier, new String[0], null, capacity
        );
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        GT_StructureIntegrationTest.TestTile tile =
            new GT_StructureIntegrationTest.TestTile(world, hatch, ForgeDirection.NORTH);
        world.putTile(4, 64, 7, tile);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        PosGuiData data = new PosGuiData(player, 4, 64, 7);
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager handlers = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        ModularPanel panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
        WidgetTree.collectSyncValues(handlers, panel);
        ModularContainer container = new NativeContainer();
        container.construct(player, sync, settings, panel.getName(), data);
        player.openContainer = container;
        GT_Mui2InputBusTest.prepareLayout(panel, sync);
        freezeTranslationCache(panel);
        initializeResizers(panel, new Viewport());
        WidgetTree.resizeInternal(panel.resizer(), false);

        List<FluidSlot> slots = new ArrayList<>();
        collectFluidSlots(panel, slots);
        assertEquals(slotCount, slots.size());
        int side = slotCount == 4 ? 2 : 3;
        Area first = slots.get(0).getArea();
        for (int i = 0; i < slotCount; i++) {
            FluidSlot slot = slots.get(i);
            Area area = slot.getArea();
            assertEquals(first.x + i % side * first.width, area.x);
            assertEquals(first.y + i / side * first.height, area.y);
            assertTrue(slot.resizer().isXCalculated() && slot.resizer().isYCalculated());
            FluidSlotSyncHandler handler = slot.getSyncHandler();
            assertTrue(handler.canFillSlot() && handler.canDrainSlot());
            assertEquals(capacity, handler.getFluidTank().getCapacity());
            FluidStack fluid = new FluidStack(FluidRegistry.WATER, i + 1);
            hatch.setFluid(fluid, i);
            assertSame(fluid, handler.getFluidTank().getFluid());
        }
    }

    private static void collectFluidSlots(IWidget widget, List<FluidSlot> slots) {
        if (widget instanceof FluidSlot) {
            slots.add((FluidSlot) widget);
        }
        for (IWidget child : widget.getChildren()) {
            collectFluidSlots(child, slots);
        }
    }

    private static void checkInventory(Fixture fixture) {
        ItemSlot input = fixture.itemWidget(0);
        ItemSlot output = fixture.itemWidget(1);
        assertTrue(((IVanillaSlot) input).handleAsVanillaSlot());
        assertSame(fixture.inputSlot, ((IVanillaSlot) input).getVanillaSlot());
        assertTrue(((IVanillaSlot) output).handleAsVanillaSlot());
        assertSame(fixture.outputSlot, ((IVanillaSlot) output).getVanillaSlot());
        ItemStack display = new ItemStack(Items.book);
        fixture.machine.mInventory[2] = display;
        ItemStack oldLock = null;
        if (fixture.output) {
            oldLock = new ItemStack(Items.paper);
            fixture.machine.mInventory[3] = oldLock;
        }
        fixture.machine.mFluid = new FluidStack(FluidRegistry.WATER, 2345);
        ItemStack cursor = new ItemStack(Items.bucket, 5);
        fixture.player.inventory.setItemStack(cursor);
        fixture.container.slotClick(fixture.inputSlot.slotNumber, 0, 0, fixture.player);
        assertNull(fixture.player.inventory.getItemStack());
        assertEquals(5, fixture.machine.mInventory[0].stackSize);
        fixture.container.slotClick(fixture.inputSlot.slotNumber, 0, 0, fixture.player);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals(5, fixture.player.inventory.getItemStack().stackSize);
        fixture.player.inventory.setInventorySlotContents(9, fixture.player.inventory.getItemStack());
        fixture.player.inventory.setItemStack(null);
        fixture.container.slotClick(fixture.playerSlot(9).slotNumber, 0, 1, fixture.player);
        assertNull(fixture.player.inventory.getStackInSlot(9));
        assertEquals(5, fixture.machine.mInventory[0].stackSize);
        fixture.container.slotClick(fixture.inputSlot.slotNumber, 0, 1, fixture.player);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals(5, fixture.playerAmount(Items.bucket));

        ItemStack rejected = new ItemStack(Items.bucket, 3);
        fixture.player.inventory.setItemStack(rejected);
        fixture.container.slotClick(fixture.outputSlot.slotNumber, 0, 0, fixture.player);
        assertNull(fixture.machine.mInventory[1]);
        assertSame(rejected, fixture.player.inventory.getItemStack());
        fixture.player.inventory.setItemStack(null);
        fixture.machine.mInventory[1] = new ItemStack(Items.bucket, 2);
        fixture.container.slotClick(fixture.outputSlot.slotNumber, 0, 1, fixture.player);
        assertNull(fixture.machine.mInventory[1]);
        assertEquals(7, fixture.playerAmount(Items.bucket));
        assertSame(display, fixture.machine.mInventory[2]);
        if (fixture.output) {
            assertSame(oldLock, fixture.machine.mInventory[3]);
        }
        assertEquals(2345, fixture.machine.getFluidAmount());
    }

    private static void checkMode(Fixture fixture) throws Exception {
        GT_MetaTileEntity_Hatch_Output machine = (GT_MetaTileEntity_Hatch_Output) fixture.machine;
        ByteSyncValue mode = fixture.handlers.findSyncHandler("mode", ByteSyncValue.class);
        assertTrue(mode.isAllowC2S());
        assertSame(mode, fixture.modeButton.getIntValue());
        boolean[] itemModes = {true, true, false, false, true, true, false, false, true, false};
        boolean[] steamModes = {true, true, true, true, false, false, false, false, false, false};
        boolean[] liquidModes = {true, false, true, false, true, false, true, false, true, true};
        for (byte selected = 0; selected < 10; selected++) {
            machine.mFluid = new FluidStack(FluidRegistry.WATER, 2000);
            sendMode(fixture, selected);
            assertEquals(selected, machine.getMode());
            assertEquals(itemModes[selected], machine.outputsItems());
            assertEquals(steamModes[selected], machine.outputsSteam());
            assertEquals(liquidModes[selected], machine.outputsLiquids());
            assertEquals(selected >= 8, machine.isFluidLocked());
            assertEquals(2000, machine.getFluidAmount());
            assertSame(selected >= 8 ? FluidRegistry.WATER : null, machine.getLockedFluid());
        }
        sendMode(fixture, (byte) 8);
        fixture.tank.getSyncHandler().setValue(new FluidStack(FluidRegistry.WATER, 1500), true, false);
        assertEquals(8, machine.getMode());
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertEquals(1500, machine.getFluidAmount());
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        Fixture restored = new Fixture(true, 1);
        restored.machine.loadNBTData(saved);
        GT_MetaTileEntity_Hatch_Output restoredMachine = (GT_MetaTileEntity_Hatch_Output) restored.machine;
        assertEquals(8, restoredMachine.getMode());
        assertSame(FluidRegistry.WATER, restoredMachine.getLockedFluid());
        assertEquals(1500, restoredMachine.getFluidAmount());
    }

    private static void checkLock(Fixture fixture) throws Exception {
        GT_MetaTileEntity_Hatch_Output machine = (GT_MetaTileEntity_Hatch_Output) fixture.machine;
        FluidLockSlotWidget lock = fixture.lock;
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        assertEquals(1, lock.fill(water, false));
        assertEquals(0, machine.getMode());
        assertNull(machine.getLockedFluid());
        assertNull(machine.mFluid);
        assertEquals(1, lock.fill(water, true));
        assertEquals(9, machine.getMode());
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertNull(machine.mFluid);
        assertEquals(1000, water.amount);
        lock.drain(1, false);
        assertEquals(9, machine.getMode());
        lock.drain(1, true);
        assertEquals(0, machine.getMode());
        assertNull(machine.getLockedFluid());

        machine.mFluid = new FluidStack(FluidRegistry.WATER, 2345);
        sendMode(fixture, (byte) 8);
        assertEquals(1, lock.fill(water, false));
        assertEquals(8, machine.getMode());
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertEquals(1, lock.fill(water, true));
        assertEquals(8, machine.getMode());
        assertEquals(2345, machine.getFluidAmount());

        sendMode(fixture, (byte) 0);
        ItemStack cursor = new ItemStack(Items.diamond, 7);
        fixture.player.inventory.setItemStack(cursor);
        sendFluidValue(lock.getSyncHandler(), new FluidStack(FluidRegistry.LAVA, 4000));
        assertEquals(9, machine.getMode());
        assertSame(FluidRegistry.LAVA, machine.getLockedFluid());
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(7, cursor.stackSize);
        assertEquals(2345, machine.getFluidAmount());
        assertSame(FluidRegistry.WATER, machine.mFluid.getFluid());

        lock.drain(1, true);
        ItemStack display = GT_Utility.getFluidDisplayStack(new FluidStack(FluidRegistry.WATER, 5000), true);
        assertNotNull(display);
        assertTrue(ModularUI.Mods.NEI.isLoaded());
        assertSame(FluidRegistry.WATER, FluidInteractions.getFluidForItem(display).getFluid());
        sendFluidDrag(lock.getSyncHandler(), display);
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertEquals(9, machine.getMode());
        assertEquals(1, display.stackSize);
        assertEquals(5000, GT_Utility.getFluidFromDisplayStack(display).amount);
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(7, cursor.stackSize);
        assertEquals(2345, machine.getFluidAmount());

        lock.drain(1, true);
        ItemStack zeroDisplay = GT_Utility.getFluidDisplayStack(new FluidStack(FluidRegistry.WATER, 5000), false);
        assertEquals(0, GT_Utility.getFluidFromDisplayStack(zeroDisplay).amount);
        sendFluidDrag(lock.getSyncHandler(), zeroDisplay);
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertEquals(9, machine.getMode());
        assertEquals(0, GT_Utility.getFluidFromDisplayStack(zeroDisplay).amount);
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(2345, machine.getFluidAmount());

        lock.drain(1, true);
        ItemStack bucket = new ItemStack(Items.water_bucket, 3);
        fixture.player.inventory.setItemStack(bucket);
        sendFluidClick(lock.getSyncHandler(), 0);
        assertSame(bucket, fixture.player.inventory.getItemStack());
        assertEquals(3, bucket.stackSize);
        assertSame(Items.water_bucket, bucket.getItem());
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertEquals(2345, machine.getFluidAmount());
        fixture.player.inventory.setItemStack(null);
        sendFluidClick(lock.getSyncHandler(), 0);
        assertNull(machine.getLockedFluid());
        assertEquals(0, machine.getMode());
        assertEquals(2345, machine.getFluidAmount());
    }

    private static void checkTank(Fixture fixture) throws Exception {
        FluidSlotSyncHandler tank = fixture.tank.getSyncHandler();
        assertEquals(1000, tank.getFluidTank().fill(new FluidStack(FluidRegistry.WATER, 1000), false));
        assertNull(fixture.machine.mFluid);
        sendFluidValue(tank, new FluidStack(FluidRegistry.LAVA, 99999));
        assertNull(fixture.machine.mFluid);
        fixture.player.inventory.setItemStack(new ItemStack(Items.water_bucket));
        sendFluidClick(tank, 0);
        assertEquals(1000, fixture.machine.getFluidAmount());
        assertSame(FluidRegistry.WATER, fixture.machine.mFluid.getFluid());
        assertSame(Items.bucket, fixture.player.inventory.getItemStack().getItem());
        assertEquals(1, fixture.player.inventory.getItemStack().stackSize);
        assertEquals(400, tank.getFluidTank().drain(400, false).amount);
        assertEquals(1000, fixture.machine.getFluidAmount());
        sendFluidClick(tank, 0);
        assertEquals(0, fixture.machine.getFluidAmount());
        assertSame(Items.water_bucket, fixture.player.inventory.getItemStack().getItem());
        assertEquals(1, fixture.player.inventory.getItemStack().stackSize);
        assertEquals(0, fixture.playerAmount(Items.bucket));
        assertEquals(0, fixture.playerAmount(Items.water_bucket));
        if (fixture.lock != null) {
            assertNull(((GT_MetaTileEntity_Hatch_Output) fixture.machine).getLockedFluid());
        }
    }

    private static void sendMode(Fixture fixture, byte selected) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeByte(selected);
            fixture.handlers.findSyncHandler("mode", ByteSyncValue.class)
                .readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendFluidValue(FluidSlotSyncHandler handler, FluidStack selected) {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            NetworkUtils.writeFluidStack(packet, selected);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendFluidClick(FluidSlotSyncHandler handler, int button) {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            new MouseData(Side.SERVER, button, false, false, false).writeToPacket(packet);
            handler.readOnServer(FluidSlotSyncHandler.SYNC_CLICK, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendFluidDrag(FluidSlotSyncHandler handler, ItemStack stack) {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            new MouseData(Side.SERVER, 0, false, false, false).writeToPacket(packet);
            NetworkUtils.writeItemStack(packet, stack);
            handler.readOnServer(4, packet);
        } finally {
            packet.release();
        }
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static final class Fixture {

        private final boolean output;
        private final GT_MetaTileEntity_BasicTank machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PosGuiData data;
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final NativeContainer container = new NativeContainer();
        private final List<IWidget> widgets = new ArrayList<>();
        private ModularSlot inputSlot;
        private ModularSlot outputSlot;
        private FluidSlot tank;
        private FluidLockSlotWidget lock;
        private CycleButtonWidget modeButton;

        private Fixture(boolean output, int tier) throws Exception {
            this.output = output;
            machine = output ? new GT_MetaTileEntity_Hatch_Output("mui2_fluid_output", tier, "", null)
                : new GT_MetaTileEntity_Hatch_Input("mui2_fluid_input", tier, "", null);
            GT_StructureIntegrationTest.TestTile tile =
                new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            data = new PosGuiData(player, 4, 64, 7);
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(player, sync, settings, panel.getName(), data);
            player.openContainer = container;
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            collect(panel);
            assertNotNull(inputSlot);
            assertNotNull(outputSlot);
            assertNotNull(tank);
            freezeTranslationCache(panel);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            if (widget instanceof ItemSlot) {
                ModularSlot slot = ((ItemSlot) widget).getSlot();
                if (slot.getItemHandler() == machine.getInventoryHandler()) {
                    if (slot.getSlotIndex() == 0) {
                        inputSlot = slot;
                    } else if (slot.getSlotIndex() == 1) {
                        outputSlot = slot;
                    }
                }
            } else if (widget instanceof FluidLockSlotWidget) {
                lock = (FluidLockSlotWidget) widget;
            } else if (widget instanceof FluidSlot) {
                FluidSlot fluid = (FluidSlot) widget;
                if (!fluid.getSyncHandler().isPhantom() && fluid.getSyncHandler().controlsAmount()) {
                    tank = fluid;
                }
            }
            if (widget.isName("output_mode")) {
                modeButton = (CycleButtonWidget) widget;
            }
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private ItemSlot itemWidget(int index) {
            for (IWidget widget : widgets) {
                if (widget instanceof ItemSlot) {
                    ItemSlot slot = (ItemSlot) widget;
                    if (slot.getSlot().getItemHandler() == machine.getInventoryHandler()
                        && slot.getSlot().getSlotIndex() == index) {
                        return slot;
                    }
                }
            }
            throw new AssertionError("No machine slot widget " + index);
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

        private int playerAmount(Item item) {
            int amount = 0;
            for (int index = 0; index < 36; index++) {
                ItemStack stack = player.inventory.getStackInSlot(index);
                if (stack != null && stack.getItem() == item) {
                    amount += stack.stackSize;
                }
            }
            return amount;
        }
    }

    static void freezeTranslationCache(IWidget widget) throws Exception {
        if (widget instanceof TextWidget) {
            TextWidget<?> text = (TextWidget<?>) widget;
            if (text.getKey() instanceof LangKey) {
                LangKey key = (LangKey) text.getKey();
                Object[] arguments = key.getArgsSupplier().get();
                String value = I18n.format(key.getKeySupplier().get(), arguments == null ? new Object[0] : arguments);
                // Only LangKey's client-frame cache is replaced; real text, font metrics and resizers stay in use.
                GT_ProcessingTestSupport.setField(text, "key", IKey.str(value.replace("\\n", "\n")));
            }
        }
        for (IWidget child : widget.getChildren()) {
            freezeTranslationCache(child);
        }
    }

    static final class HeadlessClient implements AutoCloseable {

        private final Field minecraftField = Minecraft.class.getDeclaredField("theMinecraft");
        private final Field localeField = I18n.class.getDeclaredField("i18nLocale");
        private final Field neiLoaded = ModularUI.Mods.class.getDeclaredField("loaded");
        private final Field neiInitialized = ModularUI.Mods.class.getDeclaredField("initialized");
        private final Minecraft previousMinecraft;
        private final FontRenderer previousFont;
        private final Object previousLocale;
        private final boolean previousNeiLoaded;
        private final boolean previousNeiInitialized;
        private final boolean previousNeaLoaded;
        private final boolean previousNeaInitialized;

        HeadlessClient() throws Exception {
            minecraftField.setAccessible(true);
            localeField.setAccessible(true);
            neiLoaded.setAccessible(true);
            neiInitialized.setAccessible(true);
            previousMinecraft = Minecraft.getMinecraft();
            previousFont = previousMinecraft == null ? null : previousMinecraft.fontRenderer;
            previousLocale = localeField.get(null);
            previousNeiLoaded = neiLoaded.getBoolean(ModularUI.Mods.NEI);
            previousNeiInitialized = neiInitialized.getBoolean(ModularUI.Mods.NEI);
            previousNeaLoaded = neiLoaded.getBoolean(ModularUI.Mods.NEA);
            previousNeaInitialized = neiInitialized.getBoolean(ModularUI.Mods.NEA);
            // NEI is installed in this fixture; its real StackInfo and GT display adapters are left untouched.
            neiLoaded.setBoolean(ModularUI.Mods.NEI, true);
            neiInitialized.setBoolean(ModularUI.Mods.NEI, true);
            neiLoaded.setBoolean(ModularUI.Mods.NEA, false);
            neiInitialized.setBoolean(ModularUI.Mods.NEA, true);
            Minecraft client = previousMinecraft;
            if (client == null) {
                Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
                Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
                Constructor<?> constructor = (Constructor<?>) factoryClass
                    .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                    .invoke(factory, Minecraft.class, Object.class.getDeclaredConstructor());
                constructor.setAccessible(true);
                client = (Minecraft) constructor.newInstance();
                minecraftField.set(null, client);
            }
            if (client.fontRenderer == null) {
                client.fontRenderer = new MetricsFontRenderer();
            }
            Properties properties = new Properties();
            for (String domain : new String[]{"minecraft", "gregtech"}) {
                String path = "assets/" + domain + "/lang/en_US.lang";
                try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                    assertNotNull(path, stream);
                    properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
                }
            }
            Map<String, String> translations = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                translations.put(key, properties.getProperty(key));
            }
            Locale locale = new Locale();
            GT_ProcessingTestSupport.setField(locale, "field_135032_a", translations);
            localeField.set(null, locale);
        }

        @Override
        public void close() throws Exception {
            if (previousMinecraft != null) {
                previousMinecraft.fontRenderer = previousFont;
            }
            minecraftField.set(null, previousMinecraft);
            localeField.set(null, previousLocale);
            neiLoaded.setBoolean(ModularUI.Mods.NEI, previousNeiLoaded);
            neiInitialized.setBoolean(ModularUI.Mods.NEI, previousNeiInitialized);
            neiLoaded.setBoolean(ModularUI.Mods.NEA, previousNeaLoaded);
            neiInitialized.setBoolean(ModularUI.Mods.NEA, previousNeaInitialized);
        }
    }

    /** Real vanilla font metrics and resources; rendering and texture uploads are outside the layout test. */
    private static final class MetricsFontRenderer extends FontRenderer {

        private MetricsFontRenderer() {
            super(new GameSettings(), new ResourceLocation("textures/font/ascii.png"), null, false);
            onResourceManagerReload(null);
        }

        @Override
        protected void bindTexture(ResourceLocation location) {
        }

        @Override
        protected InputStream getResourceInputStream(ResourceLocation location) throws IOException {
            String path = "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                throw new IOException("Missing vanilla font asset " + path);
            }
            return stream;
        }
    }

    private static final class NativeContainer extends ModularContainer implements ContainerAccessor {

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
            return "Fluid hatch GUI viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
