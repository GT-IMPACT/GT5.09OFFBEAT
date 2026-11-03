package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
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

public class GT_Mui2MaintenanceHatchTest {

    @Test
    public void bothHatchesUseNativeWindowsAndTheGtnhCenteredRepairTargets() throws Throwable {
        run("layout");
    }

    @Test
    public void manualRepairUsesTheActualCursorToolAndConsumesTapeWithoutStoringItems() throws Throwable {
        run("manual");
    }

    @Test
    public void automaticInventorySupportsNativePickupAndShiftWhileRespectingInteractionConfig() throws Throwable {
        run("inventory");
    }

    @Test
    public void automaticRepairPreservesTheOriginalConsumableAmountsAndInventoryNbt() throws Throwable {
        run("auto");
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
        GT_Mod.gregtechproxy.mAMHInteraction = true;
        ItemList.Duct_Tape.set(Items.paper);
        GT_OreDictUnificator.registerOre("craftingDuctTape", new ItemStack(Items.paper));
        GT_OreDictUnificator.set(OrePrefixes.cell, Materials.Lubricant, new ItemStack(Items.slime_ball));
        GT_OreDictUnificator.set(OrePrefixes.screw, Materials.Steel, new ItemStack(Items.iron_ingot));
        GT_OreDictUnificator.set(OrePrefixes.circuit, Materials.Advanced, new ItemStack(Items.redstone));
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try (GT_Mui2FluidHatchGuiTest.HeadlessClient ignored = new GT_Mui2FluidHatchGuiTest.HeadlessClient()) {
            if ("layout".equals(scenario)) {
                checkLayout(new Fixture(false));
                checkLayout(new Fixture(true));
            } else if ("manual".equals(scenario)) {
                checkManual(new Fixture(false));
            } else if ("inventory".equals(scenario)) {
                checkInventory(new Fixture(true));
            } else if ("auto".equals(scenario)) {
                checkAutomaticRepair(new Fixture(true));
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
        assertEquals(175, fixture.panel.getArea().height);
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(fixture.container.canInteractWith(fixture.player));
        assertNull(fixture.handlers.findPanelHandlerNullable("ghostCircuitPanel"));
        assertEquals(fixture.machine.mAuto ? 40 : 36, fixture.container.inventorySlots.size());
        assertNotNull(fixture.widget("maintenance_help"));
        List<IWidget> slots = new ArrayList<>();
        for (IWidget widget : fixture.widgets) {
            assertFalse(widget instanceof GhostCircuitSlotWidget);
            assertFalse(widget instanceof ToggleButton);
            if (!(widget instanceof ItemSlot)) {
                continue;
            }
            String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, 0);
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
        assertEquals(fixture.machine.mAuto ? 40 : 37, slots.size());
        if (fixture.machine.mAuto) {
            assertNull(fixture.findWidget("maintenance_slot"));
            ModularSlot first = fixture.machineSlot(0);
            for (int index = 0; index < 4; index++) {
                ModularSlot slot = fixture.machineSlot(index);
                assertEquals(first.xDisplayPosition + 18 * (index % 2), slot.xDisplayPosition);
                assertEquals(first.yDisplayPosition + 18 * (index / 2), slot.yDisplayPosition);
                assertTrue(slot.isCanPut() && slot.isCanTake());
            }
            assertEquals(176, first.xDisplayPosition * 2 + 34);
        } else {
            PhantomItemSlot target = fixture.target();
            IWidget background = fixture.widget("maintenance_background");
            assertEquals(25, target.getArea().width);
            assertEquals(25, target.getArea().height);
            assertEquals(20, background.getArea().width);
            assertEquals(20, background.getArea().height);
            assertTrue(Math.abs(target.getArea().x * 2 + target.getArea().width
                - background.getArea().x * 2 - background.getArea().width) <= 1);
            assertTrue(Math.abs(target.getArea().y * 2 + target.getArea().height
                - background.getArea().y * 2 - background.getArea().height) <= 1);
            assertFalse(target.handleAsVanillaSlot());
            assertEquals(-1, target.getSyncHandler().getSlot().slotNumber);
            assertNull(fixture.machineSlotOrNull(0));
        }
        fixture.player.inventory.setInventorySlotContents(fixture.player.inventory.currentItem, null);
        assertTrue(fixture.machine.onRightclick(fixture.tile, fixture.player,
            fixture.tile.getFrontFacing(), 0.5F, 0.5F, 0.5F));
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkManual(Fixture fixture) throws Exception {
        ItemStack tool = new ItemStack(Items.iron_pickaxe);
        assertTrue(GregTech_API.registerWrench(tool));
        fixture.player.inventory.setItemStack(tool);
        fixture.clickTarget(0);
        assertTrue(fixture.machine.mWrench);
        assertFalse(fixture.machine.mScrewdriver);
        assertEquals(1, fixture.player.inventory.getItemStack().getItemDamage());
        assertSame(Items.iron_pickaxe, fixture.player.inventory.getItemStack().getItem());
        assertNull(fixture.machine.mInventory[0]);
        fixture.clickTarget(0);
        assertEquals("An already repaired issue must not wear its tool again", 1,
            fixture.player.inventory.getItemStack().getItemDamage());

        ItemStack ordinary = new ItemStack(Items.apple, 6);
        fixture.player.inventory.setItemStack(ordinary);
        fixture.clickTarget(0);
        assertSame(ordinary, fixture.player.inventory.getItemStack());
        assertEquals(6, ordinary.stackSize);
        assertNull(fixture.machine.mInventory[0]);
        ItemStack dragged = new ItemStack(Items.paper, 8);
        assertFalse(fixture.target().handleDragAndDrop(dragged, 0));
        assertEquals(8, dragged.stackSize);
        assertFalse(fixture.machine.mScrewdriver);

        fixture.tile.setActive(true);
        fixture.player.inventory.setItemStack(new ItemStack(Items.paper, 2));
        fixture.clickTarget(1);
        assertAllRepaired(fixture.machine);
        assertEquals(1, fixture.player.inventory.getItemStack().stackSize);
        assertFalse(fixture.tile.isActive());
        assertNull(fixture.machine.mInventory[0]);
        fixture.clickTarget(0);
        assertNull(fixture.player.inventory.getItemStack());
        fixture.clickTarget(0);
        assertNull(fixture.machine.mInventory[0]);
        for (int index = 0; index < 36; index++) {
            assertNull(fixture.player.inventory.getStackInSlot(index));
        }

        Fixture direct = new Fixture(false);
        direct.tile.setActive(true);
        direct.player.inventory.setInventorySlotContents(direct.player.inventory.currentItem,
            new ItemStack(Items.paper, 2));
        assertFalse(direct.machine.onRightclick(direct.tile, direct.player,
            direct.tile.getBackFacing(), 0.5F, 0.5F, 0.5F));
        assertFalse(direct.machine.mWrench);
        assertStack(direct.player.getCurrentEquippedItem(), Items.paper, 2);
        assertTrue(direct.machine.onRightclick(direct.tile, direct.player,
            direct.tile.getFrontFacing(), 0.5F, 0.5F, 0.5F));
        assertAllRepaired(direct.machine);
        assertFalse(direct.tile.isActive());
        assertStack(direct.player.getCurrentEquippedItem(), Items.paper, 1);
        assertTrue(direct.machine.onRightclick(direct.tile, direct.player,
            direct.tile.getFrontFacing(), 0.5F, 0.5F, 0.5F));
        assertNull(direct.player.getCurrentEquippedItem());
        assertNull(direct.machine.mInventory[0]);
    }

    private static void checkInventory(Fixture fixture) {
        fixture.shiftFromPlayer(new ItemStack(Items.paper, 12));
        assertStack(fixture.machine.mInventory[0], Items.paper, 12);
        fixture.player.inventory.setItemStack(new ItemStack(Items.slime_ball, 5));
        fixture.container.slotClick(fixture.machineSlot(1).slotNumber, 0, 0, fixture.player);
        assertNull(fixture.player.inventory.getItemStack());
        assertStack(fixture.machine.mInventory[1], Items.slime_ball, 5);
        fixture.container.slotClick(fixture.machineSlot(1).slotNumber, 0, 0, fixture.player);
        assertNull(fixture.machine.mInventory[1]);
        assertStack(fixture.player.inventory.getItemStack(), Items.slime_ball, 5);
        fixture.player.inventory.setItemStack(new ItemStack(Items.apple, 3));
        ItemStack arbitrary = fixture.player.inventory.getItemStack();
        fixture.container.slotClick(fixture.machineSlot(2).slotNumber, 0, 0, fixture.player);
        assertSame(arbitrary, fixture.player.inventory.getItemStack());
        assertNull(fixture.machine.mInventory[2]);
        fixture.player.inventory.setItemStack(null);
        fixture.container.slotClick(fixture.machineSlot(0).slotNumber, 0, 1, fixture.player);
        assertNull(fixture.machine.mInventory[0]);
        int tape = 0;
        for (ItemStack stack : fixture.player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == Items.paper) {
                tape += stack.stackSize;
            }
        }
        assertEquals(12, tape);
        GT_Mod.gregtechproxy.mAMHInteraction = false;
        try {
            ItemStack rejected = new ItemStack(Items.paper, 3);
            fixture.player.inventory.setItemStack(rejected);
            fixture.container.slotClick(fixture.machineSlot(0).slotNumber, 0, 0, fixture.player);
            assertSame(rejected, fixture.player.inventory.getItemStack());
            assertNull(fixture.machine.mInventory[0]);
            assertFalse(fixture.machine.allowPutStack(fixture.tile, 0, fixture.tile.getFrontFacing(), rejected));
            assertFalse(fixture.machine.allowPullStack(fixture.tile, 0, fixture.tile.getFrontFacing(), rejected));
        } finally {
            GT_Mod.gregtechproxy.mAMHInteraction = true;
        }
    }

    private static void checkAutomaticRepair(Fixture fixture) {
        ItemStack[] supplies = {
            new ItemStack(Items.paper, 8),
            new ItemStack(Items.slime_ball, 4),
            new ItemStack(Items.iron_ingot, 8),
            new ItemStack(Items.redstone, 4)
        };
        for (int index = 0; index < supplies.length; index++) {
            fixture.machine.mInventory[index] = supplies[index];
        }
        assertTrue(fixture.machine.isRecipeInputEqual(false));
        for (int index = 0; index < supplies.length; index++) {
            assertSame(supplies[index], fixture.machine.mInventory[index]);
            assertEquals(index % 2 == 0 ? 8 : 4, supplies[index].stackSize);
        }
        NBTTagCompound saved = fixture.machine.getInventoryHandler().serializeNBT();
        assertTrue(fixture.machine.autoMaintainance());
        assertAllRepaired(fixture.machine);
        assertStack(fixture.machine.mInventory[0], Items.paper, 4);
        assertStack(fixture.machine.mInventory[1], Items.slime_ball, 2);
        assertStack(fixture.machine.mInventory[2], Items.iron_ingot, 4);
        assertStack(fixture.machine.mInventory[3], Items.redstone, 2);
        assertTrue(fixture.machine.autoMaintainance());
        for (ItemStack stack : fixture.machine.mInventory) {
            assertNull(stack);
        }
        assertFalse(fixture.machine.autoMaintainance());
        fixture.machine.getInventoryHandler().deserializeNBT(saved);
        assertStack(fixture.machine.mInventory[0], Items.paper, 8);
        assertStack(fixture.machine.mInventory[1], Items.slime_ball, 4);
        assertStack(fixture.machine.mInventory[2], Items.iron_ingot, 8);
        assertStack(fixture.machine.mInventory[3], Items.redstone, 4);
        fixture.machine.mInventory[3].stackSize = 1;
        assertFalse(fixture.machine.autoMaintainance());
        assertEquals(8, fixture.machine.mInventory[0].stackSize);
        assertEquals(4, fixture.machine.mInventory[1].stackSize);
        assertEquals(8, fixture.machine.mInventory[2].stackSize);
        assertEquals(1, fixture.machine.mInventory[3].stackSize);
    }

    private static void assertAllRepaired(GT_MetaTileEntity_Hatch_Maintenance machine) {
        assertTrue(machine.mWrench && machine.mScrewdriver && machine.mSoftHammer
            && machine.mHardHammer && machine.mSolderingTool && machine.mCrowbar);
    }

    private static void assertStack(ItemStack stack, Item expected, int count) {
        assertNotNull(stack);
        assertSame(expected, stack.getItem());
        assertEquals(count, stack.stackSize);
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_Hatch_Maintenance machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final NativePlayer player;
        private final PosGuiData data;
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final NativeContainer container = new NativeContainer();
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(boolean auto) throws Exception {
            machine = new GT_MetaTileEntity_Hatch_Maintenance("mui2_maintenance", 1, new String[0], null, auto);
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
            assertNotNull("Missing " + name, widget);
            return widget;
        }

        private PhantomItemSlot target() {
            return (PhantomItemSlot) widget("maintenance_slot");
        }

        private ModularSlot machineSlotOrNull(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            return null;
        }

        private ModularSlot machineSlot(int index) {
            ModularSlot slot = machineSlotOrNull(index);
            assertNotNull(slot);
            return slot;
        }

        private void shiftFromPlayer(ItemStack stack) {
            player.inventory.setInventorySlotContents(9, stack);
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (ModularSlot.isPlayerSlot(slot) && slot.getSlotIndex() == 9) {
                    container.slotClick(slot.slotNumber, 0, 1, player);
                    assertNull(player.inventory.getStackInSlot(9));
                    return;
                }
            }
            throw new AssertionError("Missing player slot 9");
        }

        private void clickTarget(int button) throws Exception {
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                new MouseData(Side.SERVER, button, false, false, false).writeToPacket(packet);
                target().getSyncHandler().readOnServer(PhantomItemSlotSH.SYNC_CLICK, packet);
            } finally {
                packet.release();
            }
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
            return "Maintenance hatch viewport";
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
            throw new AssertionError("Maintenance hatch attempted to open a legacy GUI");
        }
    }
}
