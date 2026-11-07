package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.common.gui.modularui.multiblock.ProcessingGuiState;
import gregtech.common.modularui2.widget.CoverTabButton;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static gregtech.api.gui.GT_Container_ProcessingMachine.BATCH_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_Y;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_WIDTH;
import static gregtech.api.gui.GT_Container_ProcessingMachine.LOCK_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.NEI_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.POWER_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.RECIPE_MAP_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.SEPARATION_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.VOIDING_SLOT;
import static gregtech.api.logic.ProcessingLogic.BATCH;
import static gregtech.api.logic.ProcessingLogic.LOCK_RECIPE;
import static gregtech.api.logic.ProcessingLogic.PROTECT_FLUIDS;
import static gregtech.api.logic.ProcessingLogic.PROTECT_ITEMS;
import static gregtech.api.logic.ProcessingLogic.SEPARATE_INPUTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2MultiblockGuiTest {

    @Test
    public void allModeMasksAndRecipeMapOptionsKeepLegacyControlGeometry() throws Throwable {
        run("layout");
    }

    @Test
    public void nativeInventoryAndOptionalControllerKeepPositionsAndShiftClick() throws Throwable {
        run("inventory");
    }

    @Test
    public void realControlPacketsPreserveModeCyclesPowerAndRecipeSelection() throws Throwable {
        run("controls");
    }

    @Test
    public void firstCompleteSnapshotPublishesStateOutputsAndLongValuesTogether() throws Throwable {
        run("state");
    }

    @Test
    public void clientCannotPublishProcessingStateToTheServer() throws Throwable {
        run("serverOnly");
    }

    @Test
    public void controlPacketsRejectClosedDistantDeniedAndDetachedWindows() throws Throwable {
        run("access");
    }

    @Test
    public void inventorySlotLockingAlsoWorksForOtherMachineTypes() throws Throwable {
        run("slotLock");
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
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            GTGuis.init();
            switch (scenario) {
                case "layout":
                    checkLayout();
                    break;
                case "inventory":
                    checkInventory();
                    break;
                case "controls":
                    checkControls();
                    break;
                case "state":
                    checkState();
                    break;
                case "serverOnly":
                    checkServerOnly();
                    break;
                case "access":
                    checkAccess();
                    break;
                case "slotLock":
                    checkSlotLock();
                    break;
                default:
                    throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkLayout() throws Exception {
        for (int supported = 0; supported <= ProcessingLogic.ALL_MODES; supported++) {
            for (int mapCount = 0; mapCount <= 2; mapCount++) {
                for (boolean nei : new boolean[]{false, true}) {
                    TestMachine machine = new TestMachine();
                    machine.supportedModes = supported;
                    machine.maps = maps(mapCount);
                    machine.nei = nei;
                    Fixture fixture = new Fixture(machine);
                    GT_Container_ProcessingMachine legacy = fixture.legacy();
                    fixture.resize();
                    assertEquals(GUI_WIDTH, fixture.panel.getArea().width);
                    assertEquals(GUI_HEIGHT, fixture.panel.getArea().height);
                    IWidget display = fixture.widget("processing_display");
                    assertArea(fixture, display, 0, 0, GUI_WIDTH, GUI_HEIGHT);
                    int coverTabs = 0;
                    for (IWidget widget : fixture.widgets) {
                        if (widget instanceof CoverTabButton) {
                            assertArea(fixture, widget, -17, 1 + 22 * coverTabs, 20, 20);
                            coverTabs++;
                        }
                        assertFalse(widget instanceof GhostCircuitSlotWidget);
                    }
                    assertEquals(6, coverTabs);
                    boolean lockVisible = legacy.isControlVisible(LOCK_SLOT);
                    boolean batchVisible = legacy.isControlVisible(BATCH_SLOT);
                    if (!nei && mapCount < 2 && lockVisible != batchVisible) {
                        int loneControl = lockVisible ? LOCK_SLOT : BATCH_SLOT;
                        assertEquals(legacy.getSlot(POWER_SLOT).xDisplayPosition,
                            legacy.getSlot(loneControl).xDisplayPosition);
                    }
                    for (int control = VOIDING_SLOT; control <= RECIPE_MAP_SLOT; control++) {
                        IWidget button = fixture.findWidget("processing_control_" + control);
                        boolean visible = legacy.isControlVisible(control);
                        if (button == null) {
                            assertFalse("Missing visible control " + control, visible);
                            continue;
                        }
                        button.onUpdate();
                        assertEquals("Control " + control + ", supported " + supported, visible, button.isEnabled());
                        if (visible) {
                            Slot expected = legacy.getSlot(control);
                            assertArea(
                                fixture,
                                button,
                                expected.xDisplayPosition - 1,
                                expected.yDisplayPosition - 1,
                                18,
                                18
                            );
                        }
                    }
                }
            }
        }
        Fixture standard = new Fixture(new GT_MetaTileEntity_VacuumFreezer("mui2_real_multiblock"));
        assertTrue(standard.machine.useMui2());
        assertSame(standard.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(standard.data));
    }

    private static void checkInventory() throws Exception {
        for (boolean controller : new boolean[]{false, true}) {
            TestMachine machine = new TestMachine();
            machine.controller = controller;
            Fixture fixture = new Fixture(machine);
            GT_Container_ProcessingMachine legacy = fixture.legacy();
            fixture.resize();
            assertEquals(controller ? 37 : 36, fixture.container.inventorySlots.size());
            int playerWidgets = 0;
            int controllerWidgets = 0;
            for (IWidget widget : fixture.widgets) {
                if (!(widget instanceof ItemSlot)) {
                    continue;
                }
                ItemSlot itemSlot = (ItemSlot) widget;
                if (widget.isName("processing_controller")) {
                    assertTrue(controller);
                    controllerWidgets++;
                    assertSame(fixture.controllerSlot(), itemSlot.getSlot());
                    assertArea(fixture, widget, CONTROLLER_X - 1, CONTROLLER_Y - 1, 18, 18);
                    continue;
                }
                assertTrue(widget.getName(), widget.getName().startsWith("slot_"));
                int index = Integer.parseInt(widget.getName().substring("slot_".length()));
                ModularSlot nativeSlot = fixture.playerSlot(index);
                Slot expected = legacyPlayerSlot(legacy, fixture, index);
                assertSame(expected.getStack(), nativeSlot.getStack());
                assertArea(fixture, widget, expected.xDisplayPosition - 1, expected.yDisplayPosition - 1, 18, 18);
                playerWidgets++;
            }
            assertEquals(36, playerWidgets);
            assertEquals(controller ? 1 : 0, controllerWidgets);
            if (!controller) {
                continue;
            }
            ModularSlot slot = fixture.controllerSlot();
            assertEquals(1, slot.getSlotIndex());
            assertSame(machine.getInventoryHandler(), slot.getItemHandler());
            assertTrue(slot.isCanPut());
            assertTrue(slot.isCanTake());
            ItemStack incoming = new ItemStack(Items.stick, 5);
            fixture.player.inventory.mainInventory[9] = incoming;
            assertNotNull(fixture.container.transferStackInSlot(fixture.player, fixture.playerSlot(9).slotNumber));
            assertEquals(5, machine.mInventory[1].stackSize);
            assertNull(fixture.player.inventory.mainInventory[9]);
            assertNotNull(fixture.container.transferStackInSlot(fixture.player, slot.slotNumber));
            assertNull(machine.mInventory[1]);
            int returned = 0;
            for (ItemStack stack : fixture.player.inventory.mainInventory) {
                if (stack != null && stack.getItem() == Items.stick) {
                    returned += stack.stackSize;
                }
            }
            assertEquals(5, returned);
            assertEquals(0, machine.getProcessingModes());
        }
    }

    private static void checkControls() throws Exception {
        TestMachine machine = new TestMachine();
        machine.maps = maps(3);
        Fixture fixture = new Fixture(machine);
        for (boolean shift : new boolean[]{false, true}) {
            for (int button = 0; button <= 1; button++) {
                machine.setProcessingModes(0);
                int[] protection = button == 0
                    ? new int[]{PROTECT_ITEMS | PROTECT_FLUIDS, PROTECT_FLUIDS, PROTECT_ITEMS, 0}
                    : new int[]{PROTECT_ITEMS, PROTECT_FLUIDS, PROTECT_ITEMS | PROTECT_FLUIDS, 0};
                for (int expected : protection) {
                    fixture.press(VOIDING_SLOT, button, shift);
                    assertEquals(expected, machine.getProcessingModes());
                }
                int[] controls = {SEPARATION_SLOT, BATCH_SLOT, LOCK_SLOT};
                int[] flags = {SEPARATE_INPUTS, BATCH, LOCK_RECIPE};
                for (int index = 0; index < controls.length; index++) {
                    fixture.press(controls[index], button, shift);
                    assertEquals(flags[index], machine.getProcessingModes());
                    fixture.press(controls[index], button, shift);
                    assertEquals(0, machine.getProcessingModes());
                }
                boolean allowed = fixture.tile.isAllowedToWork();
                fixture.press(POWER_SLOT, button, shift);
                assertEquals(!allowed, fixture.tile.isAllowedToWork());
                assertEquals(!allowed, machine.powerNotification);
                fixture.press(POWER_SLOT, button, shift);
                assertEquals(allowed, fixture.tile.isAllowedToWork());
            }
        }
        machine.setActiveRecipeMap(machine.maps.get(0));
        ItemStack pending = new ItemStack(Items.gold_ingot, 4096);
        machine.mOutputItems = new ItemStack[]{pending};
        fixture.press(RECIPE_MAP_SLOT, 1, false);
        assertEquals(2, machine.getActiveRecipeMapIndex());
        fixture.press(RECIPE_MAP_SLOT, 0, true);
        assertEquals(0, machine.getActiveRecipeMapIndex());
        fixture.press(RECIPE_MAP_SLOT, 0, false);
        assertEquals(1, machine.getActiveRecipeMapIndex());
        assertSame(pending, machine.mOutputItems[0]);
        assertNull(fixture.handlers.findSyncHandlerNullable("processingControl_" + NEI_SLOT, 0));

        machine.supportedModes = PROTECT_FLUIDS;
        machine.setProcessingModes(0);
        fixture.press(VOIDING_SLOT, 0, false);
        assertEquals(PROTECT_FLUIDS, machine.getProcessingModes());
        fixture.press(VOIDING_SLOT, 1, true);
        assertEquals(0, machine.getProcessingModes());
        for (int control : new int[]{SEPARATION_SLOT, BATCH_SLOT, LOCK_SLOT}) {
            fixture.press(control, 0, false);
            assertEquals(0, machine.getProcessingModes());
        }
        fixture.press(VOIDING_SLOT, 2, false);
        fixture.press(VOIDING_SLOT, -1, false);
        assertEquals(0, machine.getProcessingModes());
        machine.maps = maps(1);
        machine.setActiveRecipeMap(machine.maps.get(0));
        fixture.press(RECIPE_MAP_SLOT, 0, false);
        assertEquals(0, machine.getActiveRecipeMapIndex());
    }

    private static void checkState() throws Exception {
        TestMachine source = new TestMachine();
        source.maps = maps(2);
        Fixture fixture = new Fixture(source);
        source.availableParallel = 4097;
        source.info = new String[]{"Temperature: 700 K", "Fuel: 16000 L"};
        source.setProcessingModes(BATCH | PROTECT_FLUIDS | LOCK_RECIPE);
        source.setActiveRecipeMap(source.maps.get(1));
        source.lEUt = Long.MIN_VALUE + 0x12345678L;
        source.storedEnergy = Long.MAX_VALUE - 33;
        source.energyCapacity = Long.MAX_VALUE - 5;
        GT_ProcessingTestSupport.setField(source, "processingResult", CheckRecipeResult.ITEM_OUTPUT_FULL);
        GT_ProcessingTestSupport.setField(source, "currentParallel", 2048);
        GT_ProcessingTestSupport.setField(source, "currentOperations", 1234567);
        GT_ProcessingTestSupport.setField(source, "mProgresstime", 1048579);
        GT_ProcessingTestSupport.setField(source, "mMaxProgresstime", 2097169);
        fixture.tile.disableWorking();
        fixture.tile.setActive(true);
        fixture.tile.setErrorDisplayID(65);
        ItemStack item = new ItemStack(Items.gold_ingot, 4096);
        item.setStackDisplayName("Pending output with full count");
        FluidStack fluid = new FluidStack(GT_ProcessingTestSupport.fluid("mui2_processing_snapshot"), 123456789);
        fluid.tag = new NBTTagCompound();
        fluid.tag.setString("cycle", "original");
        source.mOutputItems = new ItemStack[]{item, null, new ItemStack(Items.iron_ingot, 129)};
        source.mOutputFluids = new FluidStack[]{fluid};
        PanelSyncManager serverSync = new PanelSyncManager(new ModularSyncManager(false), true);
        ProcessingGuiState server = new ProcessingGuiState(source, serverSync);
        assertTrue(server.isDisplayReady());
        assertFalse(server.getSyncValue().isAllowC2S());

        TestMachine target = new TestMachine();
        target.maps = maps(2);
        GT_ProcessingTestSupport.ProcessingWorld clientWorld = new GT_ProcessingTestSupport.ProcessingWorld();
        clientWorld.isRemote = true;
        new GT_StructureIntegrationTest.TestTile(clientWorld, target, ForgeDirection.NORTH);
        PanelSyncManager clientSync = new PanelSyncManager(new ModularSyncManager(true), true);
        ProcessingGuiState client = new ProcessingGuiState(target, clientSync);
        assertFalse(client.isDisplayReady());
        sendSnapshot(client, new NBTTagCompound());
        NBTTagCompound incomplete = new NBTTagCompound();
        incomplete.setIntArray("state", new int[9]);
        sendSnapshot(client, incomplete);
        assertFalse(client.isDisplayReady());
        transferSnapshot(server, client);
        assertArrayEquals(source.info, (String[]) field(client, "machineInfo"));
        assertTrue(client.isDisplayReady());
        assertEquals(source.getProcessingModes(), field(client, "modes"));
        assertSame(CheckRecipeResult.ITEM_OUTPUT_FULL, field(client, "result"));
        assertEquals(4097, field(client, "availableParallel"));
        assertEquals(2048, field(client, "currentParallel"));
        assertEquals(1234567, field(client, "operations"));
        assertEquals(source.lEUt, field(client, "eut"));
        assertEquals(source.storedEnergy, field(client, "storedEnergy"));
        assertEquals(source.energyCapacity, field(client, "energyCapacity"));
        assertEquals(1, field(client, "activeRecipeMapIndex"));
        assertEquals(1, target.getActiveRecipeMapIndex());
        assertEquals(false, field(client, "allowedToWork"));
        assertEquals(65, field(client, "mDisplayErrorCode"));
        assertEquals(1, field(client, "mActive"));
        assertEquals(1048579, field(client, "mProgressTime"));
        assertEquals(2097169, field(client, "mMaxProgressTime"));
        ItemStack[] items = (ItemStack[]) field(client, "outputItems");
        FluidStack[] fluids = (FluidStack[]) field(client, "outputFluids");
        assertEquals(3, items.length);
        assertEquals(4096, items[0].stackSize);
        assertEquals(item.getDisplayName(), items[0].getDisplayName());
        assertNull(items[1]);
        assertEquals(129, items[2].stackSize);
        assertEquals(123456789, fluids[0].amount);
        assertEquals("original", fluids[0].tag.getString("cycle"));
        item.stackSize = 1;
        fluid.tag.setString("cycle", "changed");
        assertEquals(4096, items[0].stackSize);
        assertEquals("original", fluids[0].tag.getString("cycle"));

        source.setProcessingModes(0);
        source.lEUt = Long.MAX_VALUE;
        source.energyCapacity = 0;
        source.mOutputItems = new ItemStack[0];
        source.mOutputFluids = new FluidStack[0];
        transferSnapshot(server, client);
        assertEquals(0, field(client, "modes"));
        assertEquals(Long.MAX_VALUE, field(client, "eut"));
        assertEquals(0L, field(client, "energyCapacity"));
        assertEquals(0L, field(client, "storedEnergy"));
        assertEquals(0, ((ItemStack[]) field(client, "outputItems")).length);
        assertEquals(0, ((FluidStack[]) field(client, "outputFluids")).length);
    }

    private static void checkServerOnly() throws Exception {
        TestMachine machine = new TestMachine();
        Fixture fixture = new Fixture(machine);
        GenericSyncValue<?, ?> state = fixture.handlers.findSyncHandler("processingState", GenericSyncValue.class);
        assertFalse(state.isAllowC2S());
        NBTTagCompound malicious = (NBTTagCompound) ((NBTTagCompound) state.getValue()).copy();
        int[] values = malicious.getIntArray("state");
        values[0] = ProcessingLogic.ALL_MODES;
        values[5] = 123;
        malicious.setIntArray("state", values);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            ByteBufAdapters.NBT.serialize(packet, malicious);
            fixture.handlers.receiveWidgetUpdate(state.getKey(), false, ValueSyncHandler.SYNC_VALUE, packet);
            assertEquals(0, packet.readerIndex());
            assertEquals(0, machine.getProcessingModes());
            assertEquals(0L, machine.lEUt);
            assertEquals(0, ((NBTTagCompound) state.getValue()).getIntArray("state")[0]);
        } finally {
            packet.release();
        }
    }

    private static void checkAccess() throws Exception {
        TestMachine machine = new TestMachine();
        Fixture fixture = new Fixture(machine);
        fixture.press(BATCH_SLOT, 0, false);
        assertEquals(BATCH, machine.getProcessingModes());
        machine.setProcessingModes(0);
        fixture.player.openContainer = fixture.player.inventoryContainer;
        fixture.press(BATCH_SLOT, 0, false);
        assertEquals(0, machine.getProcessingModes());
        fixture.player.openContainer = fixture.container;
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
        fixture.press(BATCH_SLOT, 0, false);
        assertEquals(0, machine.getProcessingModes());
        fixture.player.setPosition(4.5, 64.5, 7.5);
        fixture.tile.setOwnerName("Another owner");
        GT_ProcessingTestSupport.setField(fixture.tile, "mLockUpgrade", true);
        fixture.press(BATCH_SLOT, 0, false);
        assertEquals(0, machine.getProcessingModes());
        GT_ProcessingTestSupport.setField(fixture.tile, "mLockUpgrade", false);
        assertTrue(fixture.container.canInteractWith(fixture.player));
        GT_StructureIntegrationTest.TestTile replacement = new GT_StructureIntegrationTest.TestTile(
            fixture.world,
            new TestMachine(),
            ForgeDirection.NORTH
        );
        fixture.world.putTile(4, 64, 7, replacement);
        fixture.press(BATCH_SLOT, 0, false);
        assertEquals(0, machine.getProcessingModes());
        fixture.world.putTile(4, 64, 7, fixture.tile);
        machine.setBaseMetaTileEntity(null);
        fixture.press(BATCH_SLOT, 0, false);
        assertEquals(0, machine.getProcessingModes());
        fixture.container.detectAndSendChanges();
        GenericSyncValue<?, ?> state = fixture.handlers.findSyncHandler("processingState", GenericSyncValue.class);
        state.updateCacheFromSource(false);
        assertEquals(0, ((NBTTagCompound) state.getValue()).getIntArray("state").length);
        assertEquals(0, machine.getProcessingModes());
    }

    private static void checkSlotLock() throws Exception {
        TestMachine machine = new TestMachine();
        machine.controller = true;
        Fixture fixture = new Fixture(machine);
        ModularSlot slot = fixture.controllerSlot();
        ItemStack original = new ItemStack(Items.book, 3);
        machine.getInventoryHandler().setStackInSlot(1, original);
        assertFalse(machine.isInventorySlotLocked(1));
        assertTrue(slot.canTakeStack(fixture.player));
        machine.controllerLocked = true;
        assertTrue(machine.isInventorySlotLocked(1));
        assertFalse(machine.isInventorySlotLocked(0));
        assertFalse(slot.canTakeStack(fixture.player));
        assertFalse(slot.isItemValid(new ItemStack(Items.apple)));
        assertNull(machine.getInventoryHandler().extractItem(1, 1, true));
        assertNull(machine.getInventoryHandler().extractItem(1, 1, false));
        assertNull(machine.decrStackSize(1, 1));
        assertNull(fixture.tile.decrStackSize(1, 1));
        machine.getInventoryHandler().setStackInSlot(1, new ItemStack(Items.apple));
        machine.setInventorySlotContents(1, null);
        fixture.tile.setInventorySlotContents(1, null);
        assertSame(original, slot.getStack());
        assertEquals(3, original.stackSize);
        machine.controllerLocked = false;
        assertTrue(slot.canTakeStack(fixture.player));
        assertEquals(1, machine.getInventoryHandler().extractItem(1, 1, false).stackSize);
        assertEquals(2, slot.getStack().stackSize);
    }

    private static List<GT_Recipe_Map> maps(int count) {
        GT_Recipe_Map[] maps = {
            GT_Recipe_Map.sVacuumRecipes, GT_Recipe_Map.sCompressorRecipes, GT_Recipe_Map.sCentrifugeRecipes
        };
        return count == 0 ? Collections.emptyList() : Arrays.asList(Arrays.copyOf(maps, count));
    }

    private static Slot legacyPlayerSlot(GT_Container_ProcessingMachine legacy, Fixture fixture, int index) {
        for (Object entry : legacy.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot.inventory == fixture.player.inventory && slot.getSlotIndex() == index) {
                return slot;
            }
        }
        throw new AssertionError("Missing legacy player slot " + index);
    }

    private static void assertArea(
        Fixture fixture,
        IWidget widget,
        int x,
        int y,
        int width,
        int height
    ) {
        assertEquals(widget.getName(), x, widget.getArea().x - fixture.panel.getArea().x);
        assertEquals(widget.getName(), y, widget.getArea().y - fixture.panel.getArea().y);
        assertEquals(widget.getName(), width, widget.getArea().width);
        assertEquals(widget.getName(), height, widget.getArea().height);
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static Object field(ProcessingGuiState state, String name) throws Exception {
        Field field = ProcessingGuiState.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(state);
    }

    private static void transferSnapshot(ProcessingGuiState server, ProcessingGuiState client) throws Exception {
        server.getSyncValue().updateCacheFromSource(false);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            server.getSyncValue().write(packet);
            client.getSyncValue().readOnClient(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendSnapshot(ProcessingGuiState client, NBTTagCompound snapshot) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            ByteBufAdapters.NBT.serialize(packet, snapshot);
            client.getSyncValue().readOnClient(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static final class Fixture {

        private final MetaTileEntity machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PosGuiData data;
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final ModularContainer container;
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(MetaTileEntity machine) throws Exception {
            this.machine = machine;
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            data = new PosGuiData(player, 4, 64, 7);
            assertTrue(machine.useMui2());
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            player.openContainer = container;
            assertTrue(container.canInteractWith(player));
            collect(panel);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private void resize() {
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private IWidget findWidget(String name) {
            for (IWidget widget : widgets) {
                if (widget.isName(name)) {
                    return widget;
                }
            }
            return null;
        }

        private IWidget widget(String name) {
            IWidget widget = findWidget(name);
            assertNotNull(name, widget);
            return widget;
        }

        private GT_Container_ProcessingMachine legacy() {
            return new GT_Container_ProcessingMachine(player.inventory, tile);
        }

        private ModularSlot playerSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if ("player_inventory".equals(slot.getSlotGroupName()) && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            throw new AssertionError("Missing player slot " + index);
        }

        private ModularSlot controllerSlot() {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == machine.getInventoryHandler() && slot.getSlotIndex() == 1) {
                    return slot;
                }
            }
            throw new AssertionError("Missing controller slot");
        }

        private void press(int control, int button, boolean shift) throws Exception {
            InteractionSyncHandler handler = handlers.findSyncHandler(
                "processingControl_" + control,
                InteractionSyncHandler.class
            );
            assertTrue(handler.isAllowC2S());
            IWidget widget = widget("processing_control_" + control);
            assertTrue(widget instanceof ButtonWidget);
            assertSame(handler, ((ButtonWidget<?>) widget).getSyncHandler());
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                new MouseData(
                    Side.CLIENT,
                    button,
                    shift,
                    false,
                    false
                ).writeToPacket(packet);
                handler.readOnServer(1, packet);
            } finally {
                packet.release();
            }
        }
    }

    private static final class TestMachine extends GT_MetaTileEntity_VacuumFreezer {

        private String[] info = new String[0];
        private boolean powerNotification;
        private int supportedModes = ProcessingLogic.ALL_MODES;
        private List<GT_Recipe_Map> maps = maps(1);
        private boolean nei = true;
        private boolean controller;
        private boolean controllerLocked;
        private int availableParallel = 1;
        private long storedEnergy;
        private long energyCapacity;

        private TestMachine() {
            super("mui2_multiblock_test");
        }

        @Override
        public String[] getProcessingInfo() {
            return info;
        }

        @Override
        public void onProcessingPowerChanged(boolean enabled) {
            powerNotification = enabled;
        }

        @Override
        public int getSupportedProcessingModes() {
            return supportedModes;
        }

        @Override
        public List<GT_Recipe_Map> getRecipeMaps() {
            return maps;
        }

        @Override
        public GT_Recipe_Map getRecipeMapForNEI() {
            return nei ? getRecipeMap() : null;
        }

        @Override
        public boolean hasControllerSlotInGui() {
            return controller;
        }

        @Override
        public boolean isInventorySlotLocked(int slot) {
            return slot == 1 && controllerLocked;
        }

        @Override
        public int getAvailableParallel() {
            return availableParallel;
        }

        @Override
        public long getProcessingStoredEnergy() {
            return storedEnergy;
        }

        @Override
        public long getProcessingEnergyCapacity() {
            return energyCapacity;
        }
    }

    private static final class Viewport extends StaticResizer {

        private final Area area = new Area(0, 0, GUI_WIDTH, GUI_HEIGHT);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Processing multiblock legacy layout";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
