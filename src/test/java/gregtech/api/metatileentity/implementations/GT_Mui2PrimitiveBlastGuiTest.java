package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.common.modularui2.widget.GTProgressWidget;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BrickedBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BronzeBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_PrimitiveBlastFurnace;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
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

public class GT_Mui2PrimitiveBlastGuiTest {

    @Test
    public void bothFurnacesKeepTheGtnhLayoutNativeSlotsAndRecipeViewerHook() throws Throwable {
        run("layout");
    }

    @Test
    public void nativePickupAndShiftPreserveThreeInputsAndTakeOnlyOutputs() throws Throwable {
        run("inventory");
    }

    @Test
    public void progressSynchronizesIdlePartialCompletionAndReloadedCycles() throws Throwable {
        run("progress");
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
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try (GT_Mui2FluidHatchGuiTest.HeadlessClient ignored = new GT_Mui2FluidHatchGuiTest.HeadlessClient()) {
            for (boolean bronze : new boolean[] {false, true}) {
                Fixture fixture = new Fixture(bronze);
                if ("layout".equals(scenario)) {
                    checkLayout(fixture);
                } else if ("inventory".equals(scenario)) {
                    checkInventory(fixture);
                } else if ("progress".equals(scenario)) {
                    checkProgress(fixture, bronze);
                } else {
                    throw new AssertionError(scenario);
                }
            }
        } finally {
            Thread.currentThread().setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) throws Exception {
        assertTrue(fixture.machine.useMui2());
        assertEquals(176, fixture.panel.getArea().width);
        assertEquals(175, fixture.panel.getArea().height);
        assertEquals(fixture.machine instanceof GT_MetaTileEntity_BronzeBlastFurnace
            ? "gregtech:bronze" : "gregtech:primitive", fixture.machine.getGuiTheme().getId());
        assertEquals(fixture.machine.getGuiTheme().getId(), fixture.panel.getScreen().getThemeOverride());
        assertSame(fixture.machine.getGuiTheme().getMuiTheme(), fixture.panel.getScreen().getCurrentTheme());
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(fixture.container.canInteractWith(fixture.player));
        assertEquals(42, fixture.container.inventorySlots.size());
        int header = CommonWidgets.MACHINE_HEADER_HEIGHT;
        for (int index = 0; index < 6; index++) {
            ModularSlot slot = fixture.machineSlot(index);
            ItemSlot widget = fixture.itemWidget(index);
            assertEquals("item_inv", slot.getSlotGroupName());
            assertEquals(index < 3 ? 34 : 86 + 18 * (index - 3), slot.xDisplayPosition);
            assertEquals(header + (index < 3 ? 16 + 18 * index : 25), slot.yDisplayPosition);
            assertTrue(slot.isCanTake());
            assertEquals(index < 3, slot.isCanPut());
            assertTrue(((IVanillaSlot) widget).handleAsVanillaSlot());
            assertSame(slot, ((IVanillaSlot) widget).getVanillaSlot());
            String expectedTheme = index == 0 || index == 3
                ? GTWidgetThemes.OVERLAY_ITEM_SLOT_INGOT.getFullName()
                : index == 2 ? GTWidgetThemes.OVERLAY_ITEM_SLOT_FURNACE.getFullName()
                    : GTWidgetThemes.OVERLAY_ITEM_SLOT_DUST.getFullName();
            assertEquals(expectedTheme,
                widget.getWidgetTheme(fixture.panel.getScreen().getCurrentTheme()).getKey().getFullName());
        }
        assertEquals(fixture.panel.getArea().x + 58, fixture.progress.getArea().x);
        assertEquals(fixture.panel.getArea().y + 24 + header, fixture.progress.getArea().y);
        assertEquals(20, fixture.progress.getArea().width);
        assertEquals(18, fixture.progress.getArea().height);
        assertEquals(20, fixture.progress.getImageSize());
        Field transferRect = GTProgressWidget.class.getDeclaredField("transferRectId");
        transferRect.setAccessible(true);
        assertEquals(GT_Recipe_Map.sPrimitiveBlastRecipes.mNEIName, transferRect.get(fixture.progress));
        List<IWidget> slots = new ArrayList<>();
        for (IWidget widget : fixture.widgets) {
            if (!(widget instanceof ItemSlot) && !(widget instanceof GTProgressWidget)) {
                continue;
            }
            Area area = widget.getArea();
            String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, 0);
            assertTrue("Unresolved " + details,
                widget.resizer().isXCalculated() && widget.resizer().isYCalculated()
                    && widget.resizer().isWidthCalculated() && widget.resizer().isHeightCalculated());
            assertTrue(details, area.x >= fixture.panel.getArea().x && area.y >= fixture.panel.getArea().y);
            assertTrue(details, area.ex() <= fixture.panel.getArea().ex() && area.ey() <= fixture.panel.getArea().ey());
            for (IWidget previous : slots) {
                assertFalse("Overlapping " + details, area.intersects(previous.getArea()));
            }
            slots.add(widget);
        }
        assertEquals(43, slots.size());
        assertTrue(fixture.machine.onRightclick(fixture.tile, fixture.player));
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkInventory(Fixture fixture) {
        ItemStack input = new ItemStack(Items.apple, 12);
        input.setTagCompound(new NBTTagCompound());
        input.getTagCompound().setString("input", "preserved");
        fixture.player.inventory.setItemStack(input);
        fixture.clickMachine(0, false);
        assertNull(fixture.player.inventory.getItemStack());
        assertEquals(12, fixture.machine.mInventory[0].stackSize);
        assertEquals("preserved", fixture.machine.mInventory[0].getTagCompound().getString("input"));
        fixture.clickMachine(0, false);
        assertNull(fixture.machine.mInventory[0]);
        assertEquals(12, fixture.player.inventory.getItemStack().stackSize);
        fixture.player.inventory.setItemStack(null);
        fixture.player.inventory.setInventorySlotContents(9, new ItemStack(Items.coal, 10));
        fixture.shiftPlayer(9);
        assertNull(fixture.player.inventory.getStackInSlot(9));
        assertSame(Items.coal, fixture.machine.mInventory[0].getItem());
        assertEquals(10, fixture.machine.mInventory[0].stackSize);
        for (int index = 3; index < 6; index++) {
            ItemStack rejected = new ItemStack(Items.gold_ingot, 2);
            fixture.player.inventory.setItemStack(rejected);
            fixture.clickMachine(index, false);
            assertSame(rejected, fixture.player.inventory.getItemStack());
            assertNull(fixture.machine.mInventory[index]);
            ItemStack output = new ItemStack(Items.iron_ingot, index);
            output.setTagCompound(new NBTTagCompound());
            output.getTagCompound().setInteger("output", index);
            fixture.machine.mInventory[index] = output;
            fixture.player.inventory.setItemStack(null);
            fixture.clickMachine(index, false);
            assertNull(fixture.machine.mInventory[index]);
            assertEquals(index, fixture.player.inventory.getItemStack().stackSize);
            assertEquals(index, fixture.player.inventory.getItemStack().getTagCompound().getInteger("output"));
            fixture.player.inventory.setItemStack(null);
            fixture.machine.mInventory[index] = new ItemStack(Items.iron_ingot, index);
            fixture.clickMachine(index, true);
            assertNull(fixture.machine.mInventory[index]);
        }
        int outputCount = 0;
        for (ItemStack stack : fixture.player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == Items.iron_ingot) {
                outputCount += stack.stackSize;
            }
        }
        assertEquals(12, outputCount);
    }

    private static void checkProgress(Fixture fixture, boolean bronze) throws Exception {
        checkSyncedProgress(fixture, 0.0);
        fixture.machine.mMaxProgresstime = 80;
        fixture.machine.mProgresstime = 20;
        checkSyncedProgress(fixture, 0.25);
        NBTTagCompound active = new NBTTagCompound();
        fixture.machine.saveNBTData(active);
        Fixture restored = new Fixture(bronze);
        restored.machine.loadNBTData(active);
        checkSyncedProgress(restored, 0.25);
        fixture.machine.mProgresstime = 80;
        checkSyncedProgress(fixture, 1.0);
        fixture.machine.mProgresstime = 0;
        fixture.machine.mMaxProgresstime = 0;
        checkSyncedProgress(fixture, 0.0);
    }

    private static void checkSyncedProgress(Fixture fixture, double expected) throws Exception {
        DoubleSyncValue server = (DoubleSyncValue) fixture.progress.getDoubleValue();
        server.updateCacheFromSource(true);
        assertEquals(expected, server.getDoubleValue(), 0.0);
        assertEquals((float) expected, fixture.progress.getCurrentProgress(), 0.0F);
        assertFalse(server.isAllowC2S());
        DoubleSyncValue client = new DoubleSyncValue(() -> 0.0);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            server.write(packet);
            client.readOnClient(ValueSyncHandler.SYNC_VALUE, packet);
            assertEquals(expected, client.getDoubleValue(), 0.0);
            assertEquals(0, packet.readableBytes());
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

        private final GT_MetaTileEntity_PrimitiveBlastFurnace machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final NativePlayer player;
        private final PosGuiData data;
        private final ModularPanel panel;
        private final NativeContainer container = new NativeContainer();
        private final List<IWidget> widgets = new ArrayList<>();
        private GTProgressWidget progress;

        private Fixture(boolean bronze) throws Exception {
            machine = bronze ? new GT_MetaTileEntity_BronzeBlastFurnace("mui2_bronze_blast")
                : new GT_MetaTileEntity_BrickedBlastFurnace("mui2_bricked_blast");
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new NativePlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            data = new PosGuiData(player, 4, 64, 7);
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(player, sync, settings, panel.getName(), data);
            player.openContainer = container;
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            panel.getScreen().useTheme(machine.getGuiTheme().getId());
            GT_ProcessingTestSupport.setField(panel, "theme", machine.getGuiTheme().getMuiTheme());
            collect(panel);
            assertNotNull(progress);
            GT_Mui2FluidHatchGuiTest.freezeTranslationCache(panel);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            if (widget instanceof GTProgressWidget) {
                progress = (GTProgressWidget) widget;
            }
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
            return "Primitive blast furnace viewport";
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
            throw new AssertionError("Primitive blast furnace attempted to open a legacy GUI");
        }
    }
}
