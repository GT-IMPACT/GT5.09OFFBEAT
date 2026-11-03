package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
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
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AssemblyLine;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
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

public class GT_Mui2DataAccessHatchTest {

    @Test
    public void allRegisteredTiersUseTheGtnhGridAndCircuitSlotOverlay() throws Throwable {
        run("layout");
    }

    @Test
    public void nativePickupAndShiftConserveSticksAndRejectOtherDataItems() throws Throwable {
        run("inventory");
    }

    @Test
    public void tierAndActivityAutomationRulesKeepTheirFourTickRefreshTimeout() throws Throwable {
        run("automation");
    }

    @Test
    public void oldNbtRetainsForbiddenItemsForReadersAndNativeExtraction() throws Throwable {
        run("persistence");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            try {
                runtime.run(getClass(), scenario, "");
            } catch (Throwable failure) {
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
        ItemList.Tool_DataStick.set(new ItemStack(Items.paper, 1, 1));
        ItemList.Tool_DataOrb.set(new ItemStack(Items.paper, 1, 2));
        new GT_IntegratedCircuit_Item();
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try (GT_Mui2FluidHatchGuiTest.HeadlessClient ignored = new GT_Mui2FluidHatchGuiTest.HeadlessClient()) {
            if ("persistence".equals(scenario)) {
                checkPersistence();
            } else {
                for (int tier : new int[] {4, 6, 8}) {
                    Fixture fixture = new Fixture(tier);
                    if ("layout".equals(scenario)) {
                        checkLayout(fixture);
                    } else if ("inventory".equals(scenario)) {
                        checkInventory(fixture);
                    } else if ("automation".equals(scenario)) {
                        checkAutomation(fixture);
                    } else {
                        throw new AssertionError(scenario);
                    }
                }
            }
        } finally {
            Thread.currentThread().setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) {
        int dimension = fixture.machine.mTier == 4 ? 2 : 4;
        int physicalSlots = dimension * dimension;
        assertTrue(fixture.machine.useMui2());
        assertEquals(176, fixture.panel.getArea().width);
        assertEquals(175, fixture.panel.getArea().height);
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(fixture.container.canInteractWith(fixture.player));
        assertEquals(physicalSlots + 36, fixture.container.inventorySlots.size());
        assertNull(fixture.handlers.findPanelHandlerNullable("ghostCircuitPanel"));
        ModularSlot first = fixture.machineSlot(0);
        assertEquals(176, first.xDisplayPosition * 2 + 18 * (dimension - 1) + 16);
        for (int index = 0; index < physicalSlots; index++) {
            ModularSlot slot = fixture.machineSlot(index);
            assertEquals("data_inv", slot.getSlotGroupName());
            assertEquals(first.xDisplayPosition + 18 * (index % dimension), slot.xDisplayPosition);
            assertEquals(first.yDisplayPosition + 18 * (index / dimension), slot.yDisplayPosition);
            assertEquals(1, slot.getSlotStackLimit());
            ItemSlot widget = fixture.itemWidget(index);
            assertSame(GTGuiTextures.OVERLAY_SLOT_CIRCUIT, widget.getBackground());
            assertTrue(((IVanillaSlot) widget).handleAsVanillaSlot());
            assertSame(slot, ((IVanillaSlot) widget).getVanillaSlot());
        }
        List<IWidget> slots = new ArrayList<>();
        for (IWidget widget : fixture.widgets) {
            assertFalse(widget instanceof GhostCircuitSlotWidget);
            assertFalse(widget instanceof ToggleButton);
            if (!(widget instanceof ItemSlot)) {
                continue;
            }
            String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, fixture.machine.mTier);
            assertTrue("Unresolved " + details,
                widget.resizer().isXCalculated() && widget.resizer().isYCalculated()
                    && widget.resizer().isWidthCalculated() && widget.resizer().isHeightCalculated());
            Area area = widget.getArea();
            assertTrue(details, area.x >= fixture.panel.getArea().x && area.y >= fixture.panel.getArea().y);
            assertTrue(details, area.ex() <= fixture.panel.getArea().ex() && area.ey() <= fixture.panel.getArea().ey());
            for (IWidget previous : slots) {
                assertFalse("Overlapping " + details, area.intersects(previous.getArea()));
            }
            slots.add(widget);
        }
        assertEquals(physicalSlots + 36, slots.size());
        assertTrue(fixture.machine.onRightclick(fixture.tile, fixture.player));
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkInventory(Fixture fixture) {
        ItemStack offered = stick(3, "tagged_recipe");
        fixture.player.inventory.setItemStack(offered);
        fixture.clickMachine(0, false);
        assertEquals(1, fixture.machine.mInventory[0].stackSize);
        assertEquals("tagged_recipe", fixture.machine.mInventory[0].getTagCompound().getString("recipe"));
        assertEquals(2, fixture.player.inventory.getItemStack().stackSize);
        assertEquals(3, fixture.countSticks());
        fixture.player.inventory.setInventorySlotContents(10, fixture.player.inventory.getItemStack());
        fixture.player.inventory.setItemStack(null);
        fixture.clickMachine(0, false);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals("tagged_recipe", fixture.player.inventory.getItemStack().getTagCompound().getString("recipe"));
        assertEquals(3, fixture.countSticks());
        fixture.clickMachine(0, false);
        assertNull(fixture.player.inventory.getItemStack());
        fixture.clickMachine(0, true);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals(3, fixture.countSticks());
        ItemStack[] rejected = {
            ItemList.Tool_DataOrb.get(1),
            ItemList.Circuit_Integrated.get(1),
            new ItemStack(Items.paper, 1, 3),
            new ItemStack(Items.apple)
        };
        for (ItemStack stack : rejected) {
            fixture.player.inventory.setItemStack(stack);
            fixture.clickMachine(0, false);
            assertSame(stack, fixture.player.inventory.getItemStack());
            assertNull(fixture.machine.mInventory[0]);
            assertFalse(fixture.machine.isItemValidForSlot(0, stack));
        }
        fixture.player.inventory.setItemStack(null);
        for (int index = 0; index < 36; index++) {
            fixture.player.inventory.setInventorySlotContents(index, null);
        }
        fixture.player.inventory.setInventorySlotContents(9, stick(6, "shift_recipe"));
        fixture.shiftPlayer(9);
        int inserted = Math.min(6, fixture.machine.mInventory.length);
        for (int index = 0; index < inserted; index++) {
            assertEquals(1, fixture.machine.mInventory[index].stackSize);
            assertEquals("shift_recipe", fixture.machine.mInventory[index].getTagCompound().getString("recipe"));
        }
        ItemStack remaining = fixture.player.inventory.getStackInSlot(9);
        if (inserted == 6) {
            assertNull(remaining);
        } else {
            assertNotNull(remaining);
            assertEquals(6 - inserted, remaining.stackSize);
        }
        assertEquals(6, fixture.countSticks());
        fixture.machine.setActive(true);
        fixture.clickMachine(0, true);
        assertNull("Active hatch still permits manual GUI extraction", fixture.machine.mInventory[0]);
        assertEquals(6, fixture.countSticks());
    }

    private static void checkAutomation(Fixture fixture) {
        ItemStack data = stick(1, "automation_recipe");
        boolean supportsAutomation = fixture.machine.mTier >= 8;
        for (byte side = 0; side < 6; side++) {
            assertEquals(supportsAutomation, fixture.machine.allowPutStack(fixture.tile, 0, side, data));
            assertEquals(supportsAutomation, fixture.machine.allowPullStack(fixture.tile, 0, side, data));
        }
        fixture.machine.setActive(true);
        for (int tick = 1; tick <= 3; tick++) {
            fixture.machine.onPostTick(fixture.tile, tick);
            assertTrue(fixture.tile.isActive());
            assertFalse(fixture.machine.allowPutStack(fixture.tile, 0, (byte) 2, data));
            assertFalse(fixture.machine.allowPullStack(fixture.tile, 0, (byte) 2, data));
        }
        fixture.machine.setActive(true);
        for (int tick = 4; tick <= 6; tick++) {
            fixture.machine.onPostTick(fixture.tile, tick);
            assertTrue("Controller activity refreshes the four-tick hold", fixture.tile.isActive());
        }
        fixture.machine.onPostTick(fixture.tile, 7);
        assertFalse(fixture.tile.isActive());
        assertEquals(supportsAutomation, fixture.machine.allowPutStack(fixture.tile, 0, (byte) 2, data));
        assertEquals(supportsAutomation, fixture.machine.allowPullStack(fixture.tile, 0, (byte) 2, data));
        fixture.machine.setActive(true);
        fixture.machine.setActive(false);
        assertFalse(fixture.tile.isActive());
        assertEquals(supportsAutomation, fixture.machine.allowPutStack(fixture.tile, 0, (byte) 2, data));
    }

    private static void checkPersistence() throws Exception {
        Fixture source = new Fixture(8);
        source.machine.mInventory[0] = stick(1, "persisted_recipe");
        source.machine.mInventory[1] = ItemList.Tool_DataOrb.get(1);
        source.machine.mInventory[1].setTagCompound(new NBTTagCompound());
        source.machine.mInventory[1].getTagCompound().setString("legacy", "orb_payload");
        source.machine.mInventory[2] = ItemList.Circuit_Integrated.get(1);
        source.machine.mInventory[3] = new ItemStack(Items.apple);
        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_data_access_test");
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[32000];
        GregTech_API.METATILEENTITIES[32000] = source.machine;
        source.tile.setMetaTileID((short) 32000);
        GT_MetaTileEntity_Hatch_DataAccess restored;
        try {
            NBTTagCompound saved = new NBTTagCompound();
            source.tile.writeToNBT(saved);
            BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
            restoredTile.setWorldObj(source.world);
            restoredTile.readFromNBT(saved);
            restored = (GT_MetaTileEntity_Hatch_DataAccess) restoredTile.getMetaTileEntity();
            assertNotNull(restored);
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previous;
        }
        Fixture fixture = new Fixture(restored);
        assertTrue(restored.useMui2());
        assertEquals(16, restored.mInventory.length);
        assertEquals("persisted_recipe", restored.mInventory[0].getTagCompound().getString("recipe"));
        assertEquals("orb_payload", restored.mInventory[1].getTagCompound().getString("legacy"));
        assertTrue(ItemList.Circuit_Integrated.isStackEqual(restored.mInventory[2], true, true));
        assertSame(Items.apple, restored.mInventory[3].getItem());
        GT_MetaTileEntity_AssemblyLine line = new GT_MetaTileEntity_AssemblyLine("mui2_data_access_reader");
        line.mDataAccessHatches.add(restored);
        List<ItemStack> sticks = line.getDataItems(2);
        assertEquals(1, sticks.size());
        assertSame(restored.mInventory[0], sticks.get(0));
        List<ItemStack> data = line.getDataItems(7);
        assertEquals(3, data.size());
        for (int index = 0; index < 3; index++) {
            assertSame(restored.mInventory[index], data.get(index));
        }
        sticks.get(0).getTagCompound().setString("reader", "live_reference");
        assertEquals("live_reference", restored.mInventory[0].getTagCompound().getString("reader"));
        for (int index = 1; index <= 3; index++) {
            assertFalse(restored.isItemValidForSlot(index, restored.mInventory[index]));
            // Native pickup splits the live inventory stack and reduces that object to zero.
            ItemStack old = restored.mInventory[index].copy();
            fixture.clickMachine(index, false);
            assertNull(restored.mInventory[index]);
            assertTrue(ItemStack.areItemStacksEqual(old, fixture.player.inventory.getItemStack()));
            fixture.clickMachine(4, false);
            assertNull(restored.mInventory[4]);
            assertTrue(ItemStack.areItemStacksEqual(old, fixture.player.inventory.getItemStack()));
            fixture.player.inventory.setInventorySlotContents(10 + index, fixture.player.inventory.getItemStack());
            fixture.player.inventory.setItemStack(null);
        }
        assertSame(Items.paper, fixture.player.inventory.getStackInSlot(11).getItem());
        assertEquals(2, fixture.player.inventory.getStackInSlot(11).getItemDamage());
        assertEquals("orb_payload", fixture.player.inventory.getStackInSlot(11).getTagCompound().getString("legacy"));
    }

    private static ItemStack stick(int count, String recipe) {
        ItemStack stack = ItemList.Tool_DataStick.get(count);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("recipe", recipe);
        return stack;
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_Hatch_DataAccess machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final NativePlayer player;
        private final PosGuiData data;
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final NativeContainer container = new NativeContainer();
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(int tier) throws Exception {
            this(new GT_MetaTileEntity_Hatch_DataAccess("mui2_data_access", tier, new String[0], null));
        }

        private Fixture(GT_MetaTileEntity_Hatch_DataAccess machine) throws Exception {
            this.machine = machine;
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
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(player, sync, settings, panel.getName(), data);
            player.openContainer = container;
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            collect(panel);
            GT_Mui2FluidHatchGuiTest.freezeTranslationCache(panel);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private ModularSlot machineSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            throw new AssertionError("Missing machine slot " + index);
        }

        private ItemSlot itemWidget(int index) {
            ModularSlot slot = machineSlot(index);
            for (IWidget widget : widgets) {
                if (widget instanceof ItemSlot && ((ItemSlot) widget).getSlot() == slot) {
                    return (ItemSlot) widget;
                }
            }
            throw new AssertionError("Missing item widget " + index);
        }

        private void clickMachine(int index, boolean shift) {
            container.slotClick(machineSlot(index).slotNumber, 0, shift ? 1 : 0, player);
        }

        private void shiftPlayer(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (ModularSlot.isPlayerSlot(slot) && slot.getSlotIndex() == index) {
                    container.slotClick(slot.slotNumber, 0, 1, player);
                    return;
                }
            }
            throw new AssertionError("Missing player slot " + index);
        }

        private int countSticks() {
            int count = 0;
            for (ItemStack stack : machine.mInventory) {
                count += countStick(stack);
            }
            for (ItemStack stack : player.inventory.mainInventory) {
                count += countStick(stack);
            }
            return count + countStick(player.inventory.getItemStack());
        }

        private int countStick(ItemStack stack) {
            return ItemList.Tool_DataStick.isStackEqual(stack, false, true) ? stack.stackSize : 0;
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
            return "Data access hatch viewport";
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
            throw new AssertionError("Data access hatch attempted to open a legacy GUI");
        }
    }
}
