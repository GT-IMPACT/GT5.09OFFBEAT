package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
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
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.gui.GT_Container_1by1;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_CuringOven;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2CuringOvenTest {

    @Test
    public void nativeFactoryKeepsTheLegacyOneSlotLayoutValidationAndLiveInventory() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "build", "");
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
            checkContainer();
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkContainer() throws Exception {
        GT_MetaTileEntity_CuringOven machine = new GT_MetaTileEntity_CuringOven(
            "mui2_curing_oven",
            1,
            "",
            null,
            "",
            ""
        );
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        DirtyTile tile = new DirtyTile(world, machine);
        world.putTile(4, 64, 7, tile);
        GT_ProcessingTestSupport.setField(tile, "mTickTimer", 41L);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        player.setPosition(4.5, 64.5, 7.5);
        ItemStack stored = new ItemStack(Items.stick, 3);
        machine.mInventory[0] = stored;
        player.inventory.setInventorySlotContents(9, new ItemStack(Items.apple, 2));
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.paper, 4));
        GT_Container_1by1 legacy = new GT_Container_1by1(player.inventory, tile);
        assertEquals(37, legacy.inventorySlots.size());
        Slot legacySlot = (Slot) legacy.inventorySlots.get(0);
        assertEquals(0, legacySlot.getSlotIndex());
        assertEquals(80, legacySlot.xDisplayPosition);
        assertEquals(35, legacySlot.yDisplayPosition);
        assertSame(stored, legacySlot.getStack());

        PosGuiData data = new PosGuiData(player, 4, 64, 7);
        assertTrue(machine.useMui2());
        assertFalse(machine.allowSelectCircuit());
        assertSame(machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(data));
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager panelSync = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        ModularPanel panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, panelSync, settings);
        WidgetTree.collectSyncValues(panelSync, panel);
        ModularContainer container = new ModularContainer();
        container.construct(
            player,
            sync,
            settings,
            panel.getName(),
            data
        );
        assertTrue(container.isInitialized());
        assertTrue(panelSync.isInitialised());
        assertFalse(container.isClient());
        assertEquals(37, container.inventorySlots.size());
        assertNull(panelSync.findSyncHandlerNullable("powerSwitch", 0));
        assertNull(panelSync.findSyncHandlerNullable("itemAutoOutput", 0));
        assertNull(panelSync.findSyncHandlerNullable("fluidAutoOutput", 0));
        assertNull(panelSync.findPanelHandlerNullable("ghostCircuitPanel"));

        List<ItemSlot> slots = new ArrayList<>();
        collectSlotsAndRejectExtraControls(panel, slots);
        assertEquals(37, slots.size());
        initializeResizers(panel, new StaticResizer() {

            private final Area area = new Area(0, 0, 176, 220);

            @Override
            public Area getArea() {
                return area;
            }

            @Override
            public String getDebugDisplayName() {
                return "Curing oven viewport";
            }

            @Override
            public String toString() {
                return getDebugDisplayName();
            }
        });
        WidgetTree.resizeInternal(panel.resizer(), false);
        assertEquals(176, panel.getArea().width);
        assertEquals(166 + CommonWidgets.MACHINE_HEADER_HEIGHT, panel.getArea().height);
        ModularSlot nativeSlot = null;
        int machineSlotCount = 0;
        for (ItemSlot widget : slots) {
            ModularSlot slot = widget.isSynced() ? widget.getSlot()
                : findNativePlayerSlot(container, Integer.parseInt(widget.getName().substring("slot_".length())));
            Slot expected;
            if ("item_inv".equals(slot.getSlotGroupName())) {
                expected = legacySlot;
                nativeSlot = slot;
                machineSlotCount++;
                assertEquals(0, slot.getSlotIndex());
            } else {
                expected = findPlayerSlot(legacy, slot.getSlotIndex());
            }
            assertSame(expected.getStack(), slot.getStack());
            assertEquals(expected.xDisplayPosition, widget.getArea().x - panel.getArea().x + 1);
            assertEquals(
                expected.yDisplayPosition + CommonWidgets.MACHINE_HEADER_HEIGHT,
                widget.getArea().y - panel.getArea().y + 1
            );
        }
        assertEquals(1, machineSlotCount);
        assertNotNull(nativeSlot);
        assertTrue(nativeSlot.canTakeStack(player));
        assertEquals(legacySlot.getSlotStackLimit(), nativeSlot.getSlotStackLimit());
        assertFalse(machine.isValidSlot(0));
        assertTrue(legacySlot.isItemValid(new ItemStack(Items.stick)));
        assertEquals(
            legacySlot.isItemValid(new ItemStack(Items.stick)),
            nativeSlot.isItemValid(new ItemStack(Items.stick))
        );
        ItemStack rejected = new ItemStack(Items.apple);
        assertSame(rejected, machine.getInventoryHandler().insertItem(0, rejected, false));
        assertTrue(nativeSlot.isItemValid(rejected));
        assertSame(stored, nativeSlot.getStack());

        int dirtyBeforeExtraction = tile.dirtyCalls;
        ItemStack extracted = nativeSlot.decrStackSize(1);
        assertSame(Items.stick, extracted.getItem());
        assertEquals(1, extracted.stackSize);
        assertEquals(2, machine.mInventory[0].stackSize);
        assertSame(machine.mInventory[0], nativeSlot.getStack());
        assertSame(machine.mInventory[0], legacySlot.getStack());
        assertTrue(tile.dirtyCalls > dirtyBeforeExtraction);
        ItemStack replacement = new ItemStack(Items.paper, 2);
        int dirtyBeforeReplacement = tile.dirtyCalls;
        nativeSlot.putStack(replacement);
        assertSame(replacement, machine.mInventory[0]);
        assertSame(replacement, legacySlot.getStack());
        assertTrue(tile.dirtyCalls > dirtyBeforeReplacement);
        assertTrue(tile.hasInventoryBeenModified());

        nativeSlot.putStack(null);
        ModularSlot playerInput = findNativePlayerSlot(container, 9);
        assertNotNull(container.transferStackInSlot(player, playerInput.slotNumber));
        assertSame(Items.apple, machine.mInventory[0].getItem());
        assertEquals(2, machine.mInventory[0].stackSize);
        assertNull(player.inventory.getStackInSlot(9));

        assertTrue(container.canInteractWith(player));
        player.setPosition(100, 64, 100);
        assertFalse(container.canInteractWith(player));
        player.setPosition(4.5, 64.5, 7.5);
        tile.setOwnerName("Another owner");
        GT_ProcessingTestSupport.setField(tile, "mLockUpgrade", true);
        assertFalse(container.canInteractWith(player));
        GT_ProcessingTestSupport.setField(tile, "mLockUpgrade", false);
        assertTrue(container.canInteractWith(player));
        world.tiles.clear();
        assertFalse(container.canInteractWith(player));
    }

    private static Slot findPlayerSlot(GT_Container_1by1 legacy, int index) {
        for (int slot = 1; slot < legacy.inventorySlots.size(); slot++) {
            Slot candidate = (Slot) legacy.inventorySlots.get(slot);
            if (candidate.getSlotIndex() == index) {
                return candidate;
            }
        }
        throw new AssertionError("Missing legacy player slot " + index);
    }

    private static ModularSlot findNativePlayerSlot(ModularContainer container, int index) {
        for (Object entry : container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            if ("player_inventory".equals(slot.getSlotGroupName()) && slot.getSlotIndex() == index) {
                return slot;
            }
        }
        throw new AssertionError("Missing native player slot " + index);
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static void collectSlotsAndRejectExtraControls(IWidget widget, List<ItemSlot> slots) {
        assertFalse(widget instanceof ToggleButton);
        assertFalse(widget instanceof FluidSlot);
        assertFalse(widget instanceof GhostCircuitSlotWidget);
        assertFalse(widget instanceof ProgressWidget);
        if (widget instanceof ItemSlot) {
            slots.add((ItemSlot) widget);
        }
        for (IWidget child : widget.getChildren()) {
            collectSlotsAndRejectExtraControls(child, slots);
        }
    }

    private static final class DirtyTile extends GT_StructureIntegrationTest.TestTile {

        private int dirtyCalls;

        private DirtyTile(GT_ProcessingTestSupport.ProcessingWorld world, GT_MetaTileEntity_CuringOven machine) {
            super(world, machine, ForgeDirection.NORTH);
        }

        @Override
        public void markDirty() {
            dirtyCalls++;
        }
    }
}
