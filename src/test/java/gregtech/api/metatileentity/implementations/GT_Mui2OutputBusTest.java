package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
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
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.gui.modularui.util.FilterSlot;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2OutputBusTest {

    @Test
    public void filterTemplatesAndCopiedDataPersistWithoutChangingPhysicalItems() throws Throwable {
        run("storage");
    }

    @Test
    public void filteredOutputReservationAndDeliveryUseTheSamePriorityAndNbtRules() throws Throwable {
        run("outputs");
    }

    @Test
    public void everyBusTierBuildsTheReferenceGridAndNativeInventory() throws Throwable {
        run("layout");
    }

    @Test
    public void nativePhantomFilterSamplesNeiItemsAndClearsWithoutTransferringThem() throws Throwable {
        run("filter");
    }

    @Test
    public void partialStorageAndSimulationRespectTheItemsStackLimitAndRemainder() throws Throwable {
        run("partial");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        if ("storage".equals(scenario)) {
            checkStorage();
        } else if ("outputs".equals(scenario)) {
            checkOutputs();
        } else if ("partial".equals(scenario)) {
            checkPartial();
        } else {
            Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
            FMLServerHandler.instance();
            Thread thread = Thread.currentThread();
            String previousName = thread.getName();
            thread.setName("Server thread");
            try {
                GTGuis.init();
                if ("layout".equals(scenario)) {
                    checkLayout();
                } else if ("filter".equals(scenario)) {
                    checkFilter();
                } else {
                    throw new AssertionError(scenario);
                }
            } finally {
                thread.setName(previousName);
            }
        }
    }

    private static void checkStorage() {
        GT_MetaTileEntity_Hatch_OutputBus bus = bus(0);
        ItemStack physical = taggedGold("selected", 37);
        bus.setLockedItem(physical);
        assertNotSame(physical, bus.getLockedItem());
        assertEquals(1, bus.getLockedItem().stackSize);
        assertEquals(37, physical.stackSize);
        physical.getTagCompound().setString("variant", "changed");
        assertEquals("selected", bus.getLockedItem().getTagCompound().getString("variant"));

        NBTTagCompound copied = bus.getCopiedData(null);
        assertEquals("outputBusFilter", copied.getString("type"));
        assertEquals("outputBusFilter", bus.getCopiedDataIdentifier(null));
        GT_MetaTileEntity_Hatch_OutputBus restored = bus(0);
        assertTrue(restored.pasteCopiedData(null, copied));
        assertTrue(restored.isFilteredToItem(taggedGold("selected", 64)));
        assertFalse(restored.pasteCopiedData(null, null));
        assertFalse(restored.pasteCopiedData(null, new NBTTagCompound()));
        assertTrue(restored.isLocked());
        assertFalse(restored.isFilteredToItem(taggedGold("changed", 1)));
        assertFalse(restored.isFilteredToItem(new ItemStack(Items.iron_ingot)));

        NBTTagCompound saved = new NBTTagCompound();
        restored.saveNBTData(saved);
        GT_MetaTileEntity_Hatch_OutputBus reloaded = bus(0);
        reloaded.loadNBTData(saved);
        assertEquals(1, reloaded.getLockedItem().stackSize);
        assertTrue(reloaded.isFilteredToItem(taggedGold("selected", 1)));
        assertNull(reloaded.mInventory[0]);
        restored.clearLock();
        assertTrue(reloaded.pasteCopiedData(null, restored.getCopiedData(null)));
        assertFalse(reloaded.isLocked());
        assertNull(reloaded.getLockedItem());

        bus.clearLock();
        assertTrue(bus.storeAll(new ItemStack(Items.iron_ingot, 4)));
        assertSame(Items.iron_ingot, bus.mInventory[0].getItem());
        GT_MetaTileEntity_Hatch_OutputBus_ME me = new GT_MetaTileEntity_Hatch_OutputBus_ME("me", 1, null, null);
        assertTrue(me.useMui2());
        assertFalse(me.acceptsItemLock());
        assertEquals(1, me.mInventory.length);
    }

    private static void checkOutputs() {
        GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("filtered_output_test");
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        GT_MetaTileEntity_Hatch_OutputBus fallback = bus(0);
        GT_MetaTileEntity_Hatch_OutputBus selected = bus(0);
        resources.attach(fallback);
        resources.attach(selected);
        selected.setLockedItem(taggedGold("selected", 1));
        machine.mOutputBusses.add(fallback);
        machine.mOutputBusses.add(selected);
        ProcessingOutputs outputs = new ProcessingOutputs(machine);
        ItemStack[] products = {taggedGold("selected", 64), new ItemStack(Items.iron_ingot, 64)};
        assertEquals(CheckRecipeResult.SUCCESS, outputs.canFit(products, null, true, false));
        assertNull(fallback.mInventory[0]);
        assertNull(selected.mInventory[0]);
        assertEquals(64, products[0].stackSize);
        assertTrue(outputs.deliver(products, null, true, false));
        assertEquals(0, products[0].stackSize);
        assertEquals(0, products[1].stackSize);
        assertSame(Items.iron_ingot, fallback.mInventory[0].getItem());
        assertTrue(selected.isFilteredToItem(selected.mInventory[0]));

        selected.mInventory[0] = null;
        ItemStack wrongNbt = taggedGold("other", 1);
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL,
            outputs.canFit(new ItemStack[]{wrongNbt}, null, true, false));
        assertFalse(outputs.deliver(new ItemStack[]{wrongNbt}, null, true, false));
        assertEquals(1, wrongNbt.stackSize);
        assertNull(selected.mInventory[0]);
        assertFalse(selected.storeAll(new ItemStack(Items.iron_ingot)));
        assertNull(selected.mInventory[0]);

        fallback.mInventory[0] = null;
        ItemStack legacyGold = taggedGold("selected", 64);
        assertTrue(machine.addOutput(legacyGold));
        assertEquals(64, legacyGold.stackSize);
        assertNull(fallback.mInventory[0]);
        assertTrue(selected.isFilteredToItem(selected.mInventory[0]));
        assertTrue(machine.addOutput(new ItemStack(Items.iron_ingot, 64)));
        assertSame(Items.iron_ingot, fallback.mInventory[0].getItem());
        selected.mInventory[0] = null;
        assertFalse(machine.addOutput(taggedGold("other", 1)));
        assertNull(selected.mInventory[0]);
    }

    private static void checkPartial() {
        GT_MetaTileEntity_Hatch_OutputBus bus = bus(0);
        ItemStack requested = new ItemStack(Items.egg, 40);
        ItemStack simulated = requested.copy();
        assertFalse(bus.storePartial(simulated, true));
        assertEquals(24, simulated.stackSize);
        assertEquals(40, requested.stackSize);
        assertNull(bus.mInventory[0]);
        assertFalse(bus.storeAll(requested));
        assertEquals(24, requested.stackSize);
        assertEquals(16, bus.mInventory[0].stackSize);
        assertEquals(16, bus.getStackSizeLimit(0, requested));
        assertFalse(bus.storeAll(requested));
        assertEquals(24, requested.stackSize);
        assertEquals(16, bus.mInventory[0].stackSize);

        bus.mInventory[0].stackSize = 12;
        ItemStack second = new ItemStack(Items.egg, 8);
        assertFalse(bus.storePartial(second, true));
        assertEquals(4, second.stackSize);
        assertEquals(12, bus.mInventory[0].stackSize);
        assertTrue(bus.storeAll(second));
        assertEquals(0, second.stackSize);
        assertEquals(16, bus.mInventory[0].stackSize);

        bus.mInventory[0].stackSize = 64;
        ItemStack overfull = new ItemStack(Items.egg);
        assertFalse(bus.storePartial(overfull, true));
        assertEquals(1, overfull.stackSize);
        assertEquals(64, bus.mInventory[0].stackSize);
        assertFalse(bus.storeAll(overfull));
        assertEquals(1, overfull.stackSize);
        assertEquals(64, bus.mInventory[0].stackSize);
    }

    private static void checkLayout() throws Exception {
        for (int tier = 0; tier <= 9; tier++) {
            GT_MetaTileEntity_Hatch_OutputBus bus = bus(tier);
            Fixture fixture = new Fixture(bus);
            int dimension = tier + 1;
            int expectedSlots = dimension * dimension;
            assertEquals(expectedSlots, bus.mInventory.length);
            assertEquals(expectedSlots + 36, fixture.container.inventorySlots.size());
            assertEquals(176 + Math.max(0, 18 * (dimension - 9)), fixture.panel.getArea().width);
            assertEquals(175 + (dimension > 4 ? (dimension - 3) * 18 : 0), fixture.panel.getArea().height);
            int slots = 0;
            int filters = 0;
            List<IWidget> frames = new ArrayList<>();
            for (IWidget widget : fixture.widgets) {
                if (widget instanceof FilterSlot) {
                    filters++;
                } else if (widget instanceof ItemSlot) {
                    slots++;
                }
                if (widget instanceof ItemSlot) {
                    Area area = widget.getArea();
                    String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, tier);
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
                        String otherDetails = GT_Mui2InputBusTest.geometry(other, fixture.panel, tier);
                        assertFalse("Intersecting " + details + " with " + otherDetails,
                            area.intersects(other.getArea()));
                    }
                    frames.add(widget);
                }
            }
            assertEquals(expectedSlots + 36, slots);
            assertEquals(1, filters);
        }
    }

    private static void checkFilter() throws Exception {
        GT_MetaTileEntity_Hatch_OutputBus bus = bus(1);
        Fixture fixture = new Fixture(bus);
        FilterSlot filter = null;
        for (IWidget widget : fixture.widgets) {
            if (widget instanceof FilterSlot) {
                filter = (FilterSlot) widget;
            }
        }
        assertNotNull(filter);
        ItemStack cursor = new ItemStack(Items.diamond, 5);
        fixture.player.inventory.setItemStack(cursor);
        ItemStack dragged = taggedGold("selected", 41);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            NetworkUtils.writeItemStack(packet, dragged);
            packet.writeVarIntToBuffer(0);
            filter.getSyncHandler().readOnServer(PhantomItemSlotSH.SYNC_ITEM_SIMPLE, packet);
        } finally {
            packet.release();
        }
        assertEquals(41, dragged.stackSize);
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(5, cursor.stackSize);
        assertEquals(1, bus.getLockedItem().stackSize);
        assertTrue(bus.isFilteredToItem(dragged));
        assertNull(bus.mInventory[0]);

        fixture.player.inventory.setItemStack(null);
        PacketBuffer clear = new PacketBuffer(Unpooled.buffer());
        try {
            new MouseData(Side.SERVER, 0, false, false, false).writeToPacket(clear);
            filter.getSyncHandler().readOnServer(PhantomItemSlotSH.SYNC_CLICK, clear);
        } finally {
            clear.release();
        }
        assertFalse(bus.isLocked());
        assertNull(fixture.player.inventory.getItemStack());
    }

    private static GT_MetaTileEntity_Hatch_OutputBus bus(int tier) {
        return new GT_MetaTileEntity_Hatch_OutputBus("output_bus_test", tier, "", null);
    }

    private static ItemStack taggedGold(String variant, int amount) {
        ItemStack stack = new ItemStack(Items.gold_ingot, amount);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("variant", variant);
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

        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final ModularPanel panel;
        private final ModularContainer container;
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(MetaTileEntity machine) throws Exception {
            GT_StructureIntegrationTest.TestTile tile =
                new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            PosGuiData data = new PosGuiData(player, 4, 64, 7);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container = new ModularContainer();
            container.construct(player, sync, settings, panel.getName(), data);
            player.openContainer = container;
            assertTrue(container.canInteractWith(player));
            collect(panel);
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
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
            return "Output bus layout viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
