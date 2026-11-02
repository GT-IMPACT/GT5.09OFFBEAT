package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Charger;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_TurboCharger;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2BatteryBufferTest {

    @Test
    public void everyBatteryBufferSizeBuildsThroughTheNativeFactory() throws Throwable {
        run("buffer");
    }

    @Test
    public void everyChargerSizeInheritsTheSameLiveNativeInventory() throws Throwable {
        run("charger");
    }

    @Test
    public void everyTurboChargerSizeInheritsTheSameLiveNativeInventory() throws Throwable {
        run("turbo");
    }

    @Test
    public void nativeSlotsPreserveMachineInsertionValidation() throws Throwable {
        run("validation");
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
            assertTrue(GuiManager.hasFactory("gregtech:mte"));
            if ("validation".equals(scenario)) {
                Fixture fixture = new Fixture(new FilteringBuffer());
                ModularSlot slot = fixture.machineSlot(0);
                assertTrue(slot.isItemValid(new ItemStack(Items.redstone)));
                assertFalse(slot.isItemValid(new ItemStack(Items.paper)));
                ItemStack invalid = new ItemStack(Items.paper);
                assertSame(invalid, fixture.machine.getInventoryHandler().insertItem(0, invalid, false));
                assertNull(slot.getStack());
                return;
            }
            int[] sizes = {1, 4, 9, 16};
            for (int size : sizes) {
                checkInventory(new Fixture(createMachine(scenario, size)), size);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static GT_MetaTileEntity_BasicBatteryBuffer createMachine(String family, int size) {
        if ("buffer".equals(family)) {
            return new GT_MetaTileEntity_BasicBatteryBuffer(
                "mui2_battery_" + size,
                1,
                "",
                null,
                size
            );
        }
        if ("charger".equals(family)) {
            return new GT_MetaTileEntity_Charger(
                "mui2_charger_" + size,
                1,
                "",
                null,
                size
            );
        }
        if ("turbo".equals(family)) {
            return new GT_MetaTileEntity_TurboCharger(
                "mui2_turbo_" + size,
                1,
                "",
                null,
                size
            );
        }
        throw new AssertionError(family);
    }

    private static void checkInventory(Fixture fixture, int size) throws Exception {
        assertTrue(fixture.machine.useMui2());
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.guiData));
        assertTrue(fixture.container.isInitialized());
        assertFalse(fixture.container.isClient());
        assertEquals(size + 36, fixture.container.inventorySlots.size());
        assertEquals(size, fixture.machine.getInventoryHandler().getSlots());
        // GTNH's battery screen omits the top-right controls and keeps the content grid unobstructed.
        assertEquals(0, countToggles(fixture.panel));
        for (int index = 0; index < size; index++) {
            ModularSlot slot = fixture.machineSlot(index);
            assertSame(fixture.machine.getInventoryHandler(), slot.getItemHandler());
            assertEquals(index, slot.getSlotIndex());
            assertEquals(1, slot.getSlotStackLimit());
            assertTrue(slot.isCanPut());
            assertTrue(slot.isCanTake());
            assertTrue(slot.isItemValid(new ItemStack(Items.redstone)));
        }

        ModularSlot firstSlot = fixture.machineSlot(0);
        ItemStack inserted = new ItemStack(Items.redstone, 3);
        ItemStack remainder = fixture.machine.getInventoryHandler().insertItem(0, inserted, false);
        assertNotNull(remainder);
        assertEquals(2, remainder.stackSize);
        assertEquals(1, fixture.machine.mInventory[0].stackSize);
        assertSame(fixture.machine.mInventory[0], firstSlot.getStack());
        assertSame(Items.redstone, fixture.machine.getInventoryHandler().extractItem(0, 1, false).getItem());
        assertNull(fixture.machine.mInventory[0]);
        assertNull(firstSlot.getStack());

        ItemStack replacement = new ItemStack(Items.stick);
        fixture.machine.mInventory[0] = replacement;
        assertSame(replacement, firstSlot.getStack());
        ItemStack guiReplacement = new ItemStack(Items.paper);
        firstSlot.putStack(guiReplacement);
        assertSame(guiReplacement, fixture.machine.mInventory[0]);
        assertTrue(fixture.tile.hasInventoryBeenModified());

        assertTrue(MetaTileEntityGuiHandler.INSTANCE.canInteractWith(fixture.player, fixture.guiData));
        assertTrue(fixture.container.canInteractWith(fixture.player));
        fixture.player.setPosition(100, 64, 100);
        assertFalse(MetaTileEntityGuiHandler.INSTANCE.canInteractWith(fixture.player, fixture.guiData));
        assertFalse(fixture.container.canInteractWith(fixture.player));
        fixture.player.setPosition(4.5, 64.5, 7.5);
        fixture.tile.setOwnerName("Another owner");
        GT_ProcessingTestSupport.setField(fixture.tile, "mLockUpgrade", true);
        assertFalse(MetaTileEntityGuiHandler.INSTANCE.canInteractWith(fixture.player, fixture.guiData));
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static int countToggles(IWidget widget) {
        int count = widget instanceof ToggleButton ? 1 : 0;
        for (IWidget child : widget.getChildren()) {
            count += countToggles(child);
        }
        return count;
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_BasicBatteryBuffer machine;
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PosGuiData guiData;
        private final ModularPanel panel;
        private final ModularContainer container;

        private Fixture(GT_MetaTileEntity_BasicBatteryBuffer machine) throws Exception {
            this.machine = machine;
            GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            assertTrue(tile.isUseableByPlayer(player));
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager panelSync = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            guiData = new PosGuiData(player, 4, 64, 7);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(guiData, panelSync, settings);
            WidgetTree.collectSyncValues(panelSync, panel);
            container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                guiData
            );
            assertTrue(panelSync.isInitialised());
        }

        private ModularSlot machineSlot(int index) {
            ModularSlot found = null;
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if ("item_inv".equals(slot.getSlotGroupName()) && slot.getSlotIndex() == index) {
                    assertNull(found);
                    found = slot;
                }
            }
            assertNotNull(found);
            return found;
        }
    }

    private static final class FilteringBuffer extends GT_MetaTileEntity_BasicBatteryBuffer {

        private FilteringBuffer() {
            super(
                "mui2_filtered_battery",
                1,
                "",
                null,
                1
            );
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return stack != null && stack.getItem() == Items.redstone;
        }
    }
}
