package gregtech.api.metatileentity.implementations;

import appeng.api.config.AccessRestriction;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2MEOutputGuiTest {

    @Test
    public void bothMeOutputsRouteToNativePanelsWithNonOverlappingInventoryAndControls() throws Throwable {
        run("layout");
    }

    @Test
    public void nativeCellSlotsValidateCellsAndSupportPickupShiftAndVanillaNeiRouting() throws Throwable {
        run("cell");
    }

    @Test
    public void modeAndPriorityPacketsUpdateTheProviderAndSurviveMachineNbt() throws Throwable {
        run("settings");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        GT_ProcessingTestSupport.setField(Loader.instance(), "namedMods", new HashMap<String, ModContainer>());
        Launch.classLoader.registerTransformer(
            "cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer"
        );
        FMLServerHandler.instance();
        GTGuis.init();
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try {
            for (boolean fluid : new boolean[]{false, true}) {
                Fixture fixture = new Fixture(fluid);
                if ("layout".equals(scenario)) {
                    checkLayout(fixture);
                } else if ("cell".equals(scenario)) {
                    checkCell(fixture);
                } else if ("settings".equals(scenario)) {
                    checkSettings(fixture);
                } else {
                    throw new AssertionError(scenario);
                }
            }
        } finally {
            Thread.currentThread().setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) {
        assertTrue(fixture.machine.useMui2());
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(MetaTileEntityGuiHandler.INSTANCE.canInteractWith(fixture.player, fixture.data));
        assertEquals(37, fixture.container.inventorySlots.size());
        assertEquals(0, fixture.cell.getSlot().getSlotIndex());
        assertSame(fixture.machine.getInventoryHandler(), fixture.cell.getSlot().getItemHandler());
        int nativeCells = 0;
        int playerSlots = 0;
        for (Object entry : fixture.container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            if (slot.getItemHandler() == fixture.machine.getInventoryHandler()) {
                assertEquals(0, slot.getSlotIndex());
                nativeCells++;
            } else {
                assertEquals("player_inventory", slot.getSlotGroupName());
                playerSlots++;
            }
        }
        assertEquals(1, nativeCells);
        assertEquals(36, playerSlots);
        List<IWidget> controls = new ArrayList<>();
        for (IWidget widget : fixture.widgets) {
            if (widget instanceof ItemSlot || widget.isName("me_check_mode")
                || widget.isName("me_cache_mode") || widget.isName("me_priority")) {
                Area area = widget.getArea();
                String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, 0);
                assertTrue("Unresolved " + details, widget.resizer().isXCalculated()
                    && widget.resizer().isYCalculated() && widget.resizer().isWidthCalculated()
                    && widget.resizer().isHeightCalculated());
                assertTrue("Left " + details, area.x >= fixture.panel.getArea().x);
                assertTrue("Top " + details, area.y >= fixture.panel.getArea().y);
                assertTrue("Right " + details, area.ex() <= fixture.panel.getArea().ex());
                assertTrue("Bottom " + details, area.ey() <= fixture.panel.getArea().ey());
                for (IWidget other : controls) {
                    assertFalse("Intersecting " + details + " with "
                        + GT_Mui2InputBusTest.geometry(other, fixture.panel, 0), area.intersects(other.getArea()));
                }
                controls.add(widget);
            }
        }
        assertEquals(40, controls.size());
    }

    private static void checkCell(Fixture fixture) throws Exception {
        boolean previousAe = GregTech_API.mAE2;
        GregTech_API.mAE2 = true;
        try {
            ItemStack cell = new ItemStack(Items.diamond_sword);
            ItemStack invalid = new ItemStack(Items.paper, 4);
            ModularSlot slot = fixture.cell.getSlot();
            assertTrue(slot.isCanPut());
            assertTrue(slot.isCanTake());
            assertTrue(slot.isItemValid(cell));
            assertFalse(slot.isItemValid(invalid));
            assertSame(invalid, fixture.machine.getInventoryHandler().insertItem(0, invalid, false));
            assertTrue(((IVanillaSlot) fixture.cell).handleAsVanillaSlot());
            assertSame(slot, ((IVanillaSlot) fixture.cell).getVanillaSlot());
            fixture.player.inventory.setItemStack(cell);
            fixture.container.slotClick(slot.slotNumber, 0, 0, fixture.player);
            assertNull(fixture.player.inventory.getItemStack());
            assertSame(Items.diamond_sword, fixture.machine.mInventory[0].getItem());
            assertEquals(1, fixture.machine.mInventory[0].stackSize);
            fixture.container.slotClick(slot.slotNumber, 0, 0, fixture.player);
            assertNull(fixture.machine.mInventory[0]);
            assertSame(Items.diamond_sword, fixture.player.inventory.getItemStack().getItem());
            fixture.player.inventory.setInventorySlotContents(9, fixture.player.inventory.getItemStack());
            fixture.player.inventory.setItemStack(null);
            fixture.container.slotClick(fixture.playerSlot(9).slotNumber, 0, 1, fixture.player);
            assertNull(fixture.player.inventory.getStackInSlot(9));
            assertSame(Items.diamond_sword, fixture.machine.mInventory[0].getItem());
            fixture.container.slotClick(slot.slotNumber, 0, 1, fixture.player);
            assertNull(fixture.machine.mInventory[0]);
            assertEquals(1, playerCells(fixture));
            fixture.player.inventory.setInventorySlotContents(9, invalid);
            fixture.container.slotClick(fixture.playerSlot(9).slotNumber, 0, 1, fixture.player);
            assertNull(fixture.machine.mInventory[0]);
            assertSame(invalid, fixture.player.inventory.getStackInSlot(9));
            assertEquals(4, invalid.stackSize);
        } finally {
            GregTech_API.mAE2 = previousAe;
        }
    }

    private static int playerCells(Fixture fixture) {
        int cells = 0;
        for (int index = 0; index < 36; index++) {
            ItemStack stack = fixture.player.inventory.getStackInSlot(index);
            if (stack != null && stack.getItem() == Items.diamond_sword) {
                cells += stack.stackSize;
            }
        }
        return cells;
    }

    private static void checkSettings(Fixture fixture) throws Exception {
        fixture.priority.getOnUpdateListener().accept(fixture.priority);
        assertFalse(fixture.priority.isEnabled());
        sendBoolean(fixture.check, true);
        assertTrue(fixture.storage.getCheckMode());
        sendBoolean(fixture.cache, true);
        assertTrue(fixture.storage.isCacheMode());
        fixture.priority.getOnUpdateListener().accept(fixture.priority);
        assertTrue(fixture.priority.isEnabled());
        for (int priority : new int[]{Integer.MIN_VALUE, -42, 0, 1, 73, Integer.MAX_VALUE}) {
            sendInt(fixture.priority, priority);
            assertEquals(priority, fixture.storage.getPriority());
        }
        assertEquals(0d, fixture.priority.parse(fixture.priority.getValidator().apply("0")), 0d);
        assertEquals(-42d, fixture.priority.parse(fixture.priority.getValidator().apply("-42")), 0d);
        assertEquals(Integer.MAX_VALUE,
            fixture.priority.parse(fixture.priority.getValidator().apply("2147483648")), 0d);
        boolean previousAe = GregTech_API.mAE2;
        GregTech_API.mAE2 = false;
        try {
            NBTTagCompound saved = new NBTTagCompound();
            fixture.machine.saveNBTData(saved);
            assertTrue(saved.getBoolean("meCheckMode"));
            assertTrue(saved.getBoolean("meCacheMode"));
            assertEquals(Integer.MAX_VALUE, saved.getInteger("mePriority"));
            Fixture restored = new Fixture(fixture.fluid);
            restored.machine.loadNBTData(saved);
            assertTrue(restored.storage.getCheckMode());
            assertTrue(restored.storage.isCacheMode());
            assertEquals(Integer.MAX_VALUE, restored.storage.getPriority());
        } finally {
            GregTech_API.mAE2 = previousAe;
        }
        sendBoolean(fixture.cache, false);
        fixture.priority.getOnUpdateListener().accept(fixture.priority);
        assertFalse(fixture.priority.isEnabled());
        assertEquals(Integer.MAX_VALUE, fixture.storage.getPriority());
    }

    private static void sendBoolean(ToggleButton widget, boolean selected) throws Exception {
        BooleanSyncValue handler = (BooleanSyncValue) widget.getSyncHandler();
        assertTrue(handler.isAllowC2S());
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeBoolean(selected);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendInt(TextFieldWidget widget, int selected) throws Exception {
        IntSyncValue handler = (IntSyncValue) widget.getSyncHandler();
        assertTrue(handler.isAllowC2S());
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeVarIntToBuffer(selected);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
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

        private final boolean fluid;
        private final MetaTileEntity machine;
        private final GT_MEOutputStorage<?> storage;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PosGuiData data;
        private final ModularPanel panel;
        private final NativeContainer container = new NativeContainer();
        private final List<IWidget> widgets = new ArrayList<>();
        private ItemSlot cell;
        private ToggleButton check;
        private ToggleButton cache;
        private TextFieldWidget priority;

        private Fixture(boolean fluid) throws Exception {
            this.fluid = fluid;
            machine = fluid ? new GT_MetaTileEntity_Hatch_Output_ME("me_fluid_gui_test", 4, new String[0], null)
                : new GT_MetaTileEntity_Hatch_OutputBus_ME("me_item_gui_test", 4, new String[0], null);
            storage = fluid ? new CellStorage<IAEFluidStack>(machine, StorageChannel.FLUIDS)
                : new CellStorage<IAEItemStack>(machine, StorageChannel.ITEMS);
            GT_ProcessingTestSupport.setField(machine, "storage", storage);
            assertSame(storage, ((IMEOutput) machine).getMEOutputStorage());
            GT_StructureIntegrationTest.TestTile tile =
                new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            data = new PosGuiData(player, 4, 64, 7);
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            player.openContainer = container;
            collect(panel);
            assertNotNull(cell);
            assertNotNull(check);
            assertNotNull(cache);
            assertNotNull(priority);
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            if (widget.isName("me_storage_cell")) {
                cell = (ItemSlot) widget;
            } else if (widget.isName("me_check_mode")) {
                check = (ToggleButton) widget;
            } else if (widget.isName("me_cache_mode")) {
                cache = (ToggleButton) widget;
            } else if (widget.isName("me_priority")) {
                priority = (TextFieldWidget) widget;
            }
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private ModularSlot playerSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() != machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            throw new AssertionError("No native player slot " + index);
        }
    }

    /** Supplies a test cell for native slot transfers. */
    private static final class CellStorage<T extends IAEStack<T>> extends GT_MEOutputStorage<T> {

        private CellStorage(MetaTileEntity machine, StorageChannel channel) {
            super(
                channel,
                1600,
                () -> machine.getStackInSlot(0),
                () -> null,
                BaseActionSource::new,
                () -> {
                },
                stack -> {
                },
                stack -> 0
            );
        }

        @Override
        public boolean isValidCell(ItemStack stack) {
            return stack != null && stack.stackSize == 1 && stack.getItem() == Items.diamond_sword;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected IMEInventoryHandler<T> loadCell(ItemStack stack) {
            if (stack.getItem() != Items.diamond_sword) {
                return null;
            }
            return (ICellInventoryHandler<T>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ICellInventoryHandler.class},
                (proxy, method, arguments) -> method.getName().equals("getAccess") ? AccessRestriction.READ_WRITE : null
            );
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
            return "ME output layout viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
