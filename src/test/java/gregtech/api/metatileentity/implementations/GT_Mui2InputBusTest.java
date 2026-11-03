package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.AbstractWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.modularui2.sync.GhostCircuitSyncHandler;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
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

public class GT_Mui2InputBusTest {

    @Test
    public void everyTierPreservesInventoryAndMigratesLegacyCapacityWithoutMovingPhysicalItems() throws Throwable {
        run("persistence");
    }

    @Test
    public void sortingUsesGtnhStackOrderPreservesNbtAndNeverMovesOrConsumesTheGhost() throws Throwable {
        run("sorting");
    }

    @Test
    public void screwdriverModesControlRecipeFilteringAndOneStackInsertionWithoutTakingItems() throws Throwable {
        run("insertion");
    }

    @Test
    public void allTierPanelsExposeNativeSlotsAndSyncedControlsWithoutOverlappingTheInventory() throws Throwable {
        run("gui");
    }

    @Test
    public void busCircuitsReachMergedAndSeparateProcessingInputsAsZeroCountSelectors() throws Throwable {
        run("processing");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        new GT_IntegratedCircuit_Item();
        if ("persistence".equals(scenario)) {
            checkPersistence();
        } else if ("sorting".equals(scenario)) {
            checkSorting();
        } else if ("insertion".equals(scenario)) {
            checkInsertion();
        } else if ("processing".equals(scenario)) {
            checkProcessingInputs();
        } else if ("gui".equals(scenario)) {
            Thread thread = Thread.currentThread();
            String previousName = thread.getName();
            thread.setName("Server thread");
            try {
                Launch.classLoader.registerTransformer(
                    "cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer"
                );
                FMLServerHandler.instance();
                GTGuis.init();
                checkGui();
            } finally {
                thread.setName(previousName);
            }
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkPersistence() {
        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_input_bus_test");
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[32000];
        try {
            for (int tier = 0; tier <= 9; tier++) {
                Fixture fixture = new Fixture(tier);
                GT_MetaTileEntity_Hatch_InputBus bus = fixture.bus;
                int capacity = (tier + 1) * (tier + 1);
                assertEquals(capacity + 1, bus.getSizeInventory());
                assertEquals(capacity, bus.getCircuitSlot());
                assertTrue(bus.isValidSlot(capacity - 1));
                assertFalse(bus.isValidSlot(capacity));
                assertTrue(bus.useMui2());
                assertTrue(bus.allowSelectCircuit());
                ItemStack last = new ItemStack(Items.gold_ingot, 17);
                last.setStackDisplayName("Last physical slot " + tier);
                bus.setInventorySlotContents(capacity - 1, last);
                fixture.ghost().setCircuitConfig(tier % 2 == 0 ? 0 : 26);
                bus.disableSort = true;
                bus.disableLimited = false;
                bus.disableFilter = false;
                bus.mRecipeMap = GT_Recipe_Map.sAssemblerRecipes;
                GregTech_API.METATILEENTITIES[32000] = bus;
                fixture.tile.setMetaTileID((short) 32000);
                GT_MetaTileEntity_Hatch_InputBus restored = restore(fixture, save(fixture.tile));
                assertEquals(capacity + 1, restored.getSizeInventory());
                assertPhysicalItem(last, restored.getStackInSlot(capacity - 1));
                GhostCircuitItemStackHandler restoredGhost = new GhostCircuitItemStackHandler(restored);
                assertEquals(tier % 2 == 0 ? 0 : 26, restoredGhost.getCircuitConfig());
                assertEquals(0, restoredGhost.getStackInSlot(0).stackSize);
                assertTrue(restored.disableSort);
                assertFalse(restored.disableLimited);
                assertFalse(restored.disableFilter);
                assertSame(GT_Recipe_Map.sAssemblerRecipes, restored.mRecipeMap);

                int oldCapacity = tier < 1 ? 1 : tier <= 6 ? (tier + 1) * (tier + 1) : 64;
                NBTTagCompound legacy = save(fixture.tile);
                NBTTagList oldInventory = new NBTTagList();
                NBTTagCompound oldLast = new NBTTagCompound();
                last.writeToNBT(oldLast);
                oldLast.setInteger("IntSlot", oldCapacity - 1);
                oldInventory.appendTag(oldLast);
                legacy.setTag("Inventory", oldInventory);
                legacy.removeTag("disableFilter");
                legacy.removeTag("disableLimited");
                legacy.removeTag("recipeMap");
                GT_MetaTileEntity_Hatch_InputBus migrated = restore(fixture, legacy);
                assertPhysicalItem(last, migrated.getStackInSlot(oldCapacity - 1));
                assertEquals(capacity, migrated.getCircuitSlot());
                assertFalse(new GhostCircuitItemStackHandler(migrated).hasCircuit());
                assertTrue(migrated.disableFilter);
                assertTrue(migrated.disableLimited);
                assertNull(migrated.mRecipeMap);
            }
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previous;
        }
    }

    private static void checkSorting() {
        Fixture fixture = new Fixture(2);
        GT_MetaTileEntity_Hatch_InputBus bus = fixture.bus;
        ItemStack[] stacks = {
            null, new ItemStack(Items.iron_ingot, 48), new ItemStack(Items.iron_ingot, 64), null,
            new ItemStack(Items.gold_ingot, 32), new ItemStack(Items.iron_ingot, 32),
            new ItemStack(Items.gold_ingot, 16)
        };
        System.arraycopy(stacks, 0, bus.mInventory, 0, stacks.length);
        for (int configuration : new int[]{0, 26}) {
            fixture.ghost().setCircuitConfig(configuration);
            bus.updateSlots();
            assertStack(bus.getStackInSlot(0), Items.iron_ingot, 64);
            assertStack(bus.getStackInSlot(1), Items.iron_ingot, 64);
            assertStack(bus.getStackInSlot(2), Items.gold_ingot, 48);
            assertStack(bus.getStackInSlot(3), Items.iron_ingot, 16);
            assertNull(bus.getStackInSlot(4));
            assertEquals(configuration, fixture.ghost().getCircuitConfig());
            assertEquals(0, fixture.ghost().getStackInSlot(0).stackSize);
            assertProtectedGhost(fixture);
        }
        for (int index = 0; index < bus.getCircuitSlot(); index++) {
            bus.mInventory[index] = null;
        }
        bus.mInventory[0] = named(60, "red");
        bus.mInventory[1] = named(60, "blue");
        bus.mInventory[2] = named(4, "red");
        bus.mInventory[3] = named(4, "blue");
        bus.updateSlots();
        assertStack(bus.getStackInSlot(0), Items.iron_ingot, 64);
        assertStack(bus.getStackInSlot(1), Items.iron_ingot, 64);
        assertEquals("red", bus.getStackInSlot(0).getDisplayName());
        assertEquals("blue", bus.getStackInSlot(1).getDisplayName());
        assertNull(bus.getStackInSlot(2));
        assertNull(bus.getStackInSlot(3));
        bus.disableSort = true;
        ItemStack fixed = new ItemStack(Items.apple, 5);
        bus.mInventory[7] = fixed;
        bus.mInventory[6] = new ItemStack(Items.stick, 0);
        bus.onPostTick(fixture.tile, 41L);
        assertSame(fixed, bus.getStackInSlot(7));
        assertNull(bus.getStackInSlot(6));
        assertEquals(26, fixture.ghost().getCircuitConfig());
    }

    private static void checkInsertion() {
        Fixture fixture = new Fixture(1);
        GT_MetaTileEntity_Hatch_InputBus bus = fixture.bus;
        byte front = fixture.tile.getFrontFacing();
        byte back = fixture.tile.getBackFacing();
        ItemStack offered = new ItemStack(Items.apple, 16);
        bus.mInventory[0] = new ItemStack(Items.apple, 32);
        assertTrue(bus.disableFilter);
        assertTrue(bus.disableLimited);
        assertFalse(bus.disableSort);
        assertTrue(bus.allowPutStack(fixture.tile, 1, front, offered));
        assertFalse(bus.allowPutStack(fixture.tile, 1, back, offered));
        fixture.player.setSneaking(true);
        screwdriver(fixture);
        assertFalse(bus.disableLimited);
        assertFalse(bus.disableSort);
        assertTrue(bus.allowPutStack(fixture.tile, 0, front, offered));
        assertFalse(bus.allowPutStack(fixture.tile, 1, front, offered));
        assertTrue(bus.allowPutStack(fixture.tile, 1, front, new ItemStack(Items.stick)));
        assertEquals(32, bus.mInventory[0].stackSize);
        assertEquals(16, offered.stackSize);
        screwdriver(fixture);
        assertTrue(bus.disableSort);
        assertTrue(bus.disableLimited);
        screwdriver(fixture);
        assertFalse(bus.disableSort);
        assertTrue(bus.disableLimited);
        fixture.player.setSneaking(false);
        GT_Recipe_Map.sAssemblerRecipes.clear();
        assertNotNull(GT_Recipe_Map.sAssemblerRecipes.addRecipe(
            false,
            new ItemStack[]{new ItemStack(Items.apple)},
            new ItemStack[]{new ItemStack(Items.gold_ingot)},
            null,
            (FluidStack[]) null,
            null,
            20,
            8,
            0
        ));
        bus.mRecipeMap = GT_Recipe_Map.sAssemblerRecipes;
        screwdriver(fixture);
        assertFalse(bus.disableFilter);
        assertTrue(bus.allowPutStack(fixture.tile, 1, front, offered));
        assertFalse(bus.allowPutStack(fixture.tile, 1, front, new ItemStack(Items.diamond)));
        screwdriver(fixture);
        assertTrue(bus.disableFilter);
        assertTrue(bus.allowPutStack(fixture.tile, 1, front, new ItemStack(Items.diamond)));
        assertTrue(bus.allowPullStack(fixture.tile, 0, front, bus.mInventory[0]));
        assertFalse(bus.allowPullStack(fixture.tile, 0, back, bus.mInventory[0]));
        assertEquals(16, offered.stackSize);
        assertEquals(32, bus.mInventory[0].stackSize);
    }

    private static void checkGui() throws Exception {
        for (int tier = 0; tier <= 9; tier++) {
            Fixture fixture = new Fixture(tier);
            fixture.build();
            prepareLayout(fixture.panel, fixture.container.getSyncManager());
            initializeResizers(fixture.panel, new Viewport());
            WidgetTree.resizeInternal(fixture.panel.resizer(), false);
            int dimension = tier + 1;
            assertEquals(176 + Math.max(0, dimension - 9) * 18, fixture.panel.getArea().width);
            assertEquals(175 + (dimension > 4 ? (dimension - 3) * 18 : 0), fixture.panel.getArea().height);
            int playerSlots = 0;
            int machineSlots = 0;
            for (Object entry : fixture.container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == fixture.bus.getInventoryHandler()) {
                    assertTrue(slot.getSlotIndex() < fixture.bus.getCircuitSlot());
                    assertEquals("item_inv", slot.getSlotGroupName());
                    machineSlots++;
                } else {
                    assertEquals("player_inventory", slot.getSlotGroupName());
                    playerSlots++;
                }
            }
            assertEquals(dimension * dimension, machineSlots);
            assertEquals(36, playerSlots);
            assertNotNull(fixture.circuit);
            assertEquals(2, fixture.toggles.size());
            List<IWidget> frames = new ArrayList<>();
            for (IWidget widget : fixture.widgets) {
                if (widget instanceof ItemSlot || widget instanceof ToggleButton) {
                    Area area = widget.getArea();
                    String details = geometry(widget, fixture.panel, tier);
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
                        assertFalse("Intersecting " + details + " with " + geometry(other, fixture.panel, tier),
                            area.intersects(other.getArea()));
                    }
                    frames.add(widget);
                }
            }
            sendBoolean(fixture.toggles.get(0), false);
            assertTrue(fixture.bus.disableSort);
            sendBoolean(fixture.toggles.get(1), true);
            assertFalse(fixture.bus.disableLimited);
            select(fixture, 26);
            assertEquals(26, fixture.ghost().getCircuitConfig());
            assertProtectedGhost(fixture);
        }
    }

    static String geometry(IWidget widget, ModularPanel panel, int tier) {
        ResizeNode parent = widget.resizer().getParent();
        return "tier=" + tier + " widget=" + widget.getClass().getSimpleName() + "#" + widget.getName()
            + " area=" + widget.getArea() + " valid=" + widget.isValid() + " panel=" + panel.getArea()
            + " parent=" + (parent == null ? null : parent.getDebugDisplayName())
            + " parentArea=" + (parent == null ? null : parent.getArea());
    }

    static void prepareLayout(ModularPanel panel, ModularSyncManager sync) throws Exception {
        // Server containers collect sync values without opening client widgets. Scrollable grids still require
        // their real panel and theme before the actual Grid/Flow resizer can run outside a rendered client screen.
        GT_ProcessingTestSupport.setField(panel, "theme", GTGuiThemes.STANDARD.getMuiTheme());
        ModularScreen screen = new ModularScreen("gregtech", panel);
        screen.getScreenArea().width = 600;
        screen.getScreenArea().height = 600;
        GT_ProcessingTestSupport.setField(panel, "screen", screen);
        bindLayoutPanel(panel, panel, null, sync);
    }

    private static void bindLayoutPanel(
        IWidget widget,
        ModularPanel panel,
        IWidget parent,
        ModularSyncManager sync
    ) throws Exception {
        setWidgetField(widget, "valid", true);
        setWidgetField(widget, "panel", panel);
        setWidgetField(widget, "parent", parent);
        setWidgetField(widget, "context", panel.getScreen().getContext());
        if (widget instanceof Widget) {
            ((Widget<?>) widget).initialiseSyncHandler(sync, false);
        }
        for (IWidget child : widget.getChildren()) {
            bindLayoutPanel(child, panel, widget, sync);
        }
    }

    private static void setWidgetField(IWidget widget, String name, Object value) throws Exception {
        Field field = AbstractWidget.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(widget, value);
    }

    private static void checkProcessingInputs() {
        GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("input_bus_circuit_test");
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        GT_MetaTileEntity_Hatch_InputBus first = resources.items(new ItemStack(Items.apple, 3));
        GT_MetaTileEntity_Hatch_InputBus second = resources.items(new ItemStack(Items.stick, 2));
        new GhostCircuitItemStackHandler(first).setCircuitConfig(0);
        new GhostCircuitItemStackHandler(second).setCircuitConfig(26);
        for (boolean separated : new boolean[]{false, true}) {
            machine.setProcessingModes(separated ? ProcessingLogic.SEPARATE_INPUTS : 0);
            List<ProcessingInputs> groups = machine.collectProcessingInputs();
            assertEquals(separated ? 2 : 1, groups.size());
            int selectors = 0;
            for (ProcessingInputs group : groups) {
                int groupSelectors = 0;
                for (ItemStack stack : group.items) {
                    if (stack.getItem() instanceof GT_IntegratedCircuit_Item) {
                        assertEquals(0, stack.stackSize);
                        assertTrue(stack.getItemDamage() == 0 || stack.getItemDamage() == 26);
                        groupSelectors++;
                    }
                }
                assertEquals(separated ? 1 : 2, groupSelectors);
                selectors += groupSelectors;
            }
            assertEquals(2, selectors);
            assertEquals(3, first.getStackInSlot(0).stackSize);
            assertEquals(2, second.getStackInSlot(0).stackSize);
        }
    }

    private static void screwdriver(Fixture fixture) {
        fixture.bus.onScrewdriverRightClick(
            fixture.tile.getFrontFacing(),
            fixture.player,
            0.5f,
            0.5f,
            0.5f
        );
    }

    private static void assertProtectedGhost(Fixture fixture) {
        int index = fixture.bus.getCircuitSlot();
        ItemStack offered = GT_Utility.getIntegratedCircuit(26);
        offered.stackSize = 16;
        assertSame(offered, fixture.bus.getInventoryHandler().insertItem(index, offered, false));
        assertNull(fixture.bus.getInventoryHandler().extractItem(index, 1, true));
        assertNull(fixture.bus.getInventoryHandler().extractItem(index, 1, false));
        assertNull(fixture.tile.decrStackSize(index, 1));
        assertFalse(fixture.bus.allowPutStack(fixture.tile, index, fixture.tile.getFrontFacing(), offered));
        assertFalse(fixture.bus.allowPullStack(fixture.tile, index, fixture.tile.getFrontFacing(), offered));
        assertEquals(0, fixture.bus.getInventoryHandler().getSlotLimit(index));
        assertEquals(16, offered.stackSize);
    }

    private static ItemStack named(int amount, String name) {
        ItemStack stack = new ItemStack(Items.iron_ingot, amount);
        stack.setStackDisplayName(name);
        return stack;
    }

    private static void assertStack(ItemStack stack, Item expected, int amount) {
        assertNotNull(stack);
        assertSame(expected, stack.getItem());
        assertEquals(amount, stack.stackSize);
    }

    private static void assertPhysicalItem(ItemStack expected, ItemStack actual) {
        assertNotNull(actual);
        assertTrue(GT_Utility.areStacksEqual(expected, actual));
        assertEquals(expected.stackSize, actual.stackSize);
    }

    private static NBTTagCompound save(BaseMetaTileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        return tag;
    }

    private static GT_MetaTileEntity_Hatch_InputBus restore(Fixture fixture, NBTTagCompound tag) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(fixture.world);
        tile.readFromNBT(tag);
        return (GT_MetaTileEntity_Hatch_InputBus) tile.getMetaTileEntity();
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

    private static void select(Fixture fixture, int value) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeShort(value);
            fixture.circuit.getSyncHandler().readOnServer(GhostCircuitSyncHandler.SYNC_CIRCUIT_CONFIG, packet);
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

        private final GT_MetaTileEntity_Hatch_InputBus bus;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final List<IWidget> widgets = new ArrayList<>();
        private final List<ToggleButton> toggles = new ArrayList<>();
        private ModularPanel panel;
        private ModularContainer container;
        private GhostCircuitSlotWidget circuit;

        private Fixture(int tier) {
            bus = new GT_MetaTileEntity_Hatch_InputBus("mui2_input_bus", tier, "", null);
            tile = new GT_StructureIntegrationTest.TestTile(world, bus, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
        }

        private GhostCircuitItemStackHandler ghost() {
            return new GhostCircuitItemStackHandler(bus);
        }

        private void build() throws Exception {
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            PosGuiData data = new PosGuiData(player, 4, 64, 7);
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
            if (widget instanceof ToggleButton) {
                toggles.add((ToggleButton) widget);
            }
            if (widget instanceof GhostCircuitSlotWidget) {
                assertNull(circuit);
                circuit = (GhostCircuitSlotWidget) widget;
            }
            for (IWidget child : widget.getChildren()) {
                collect(child);
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
            return "Input bus layout viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
