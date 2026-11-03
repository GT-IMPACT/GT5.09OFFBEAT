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
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.interfaces.ITexture;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GT_Mui2CustomBasicMachineTest {

    private static final String[] MODELS = {
        "AirCondenser",
        "Boxinator",
        "Disassembler",
        "Massfabricator",
        "Miner",
        "OrganicReplicator",
        "PotionBrewer",
        "Printer",
        "Replicator",
        "RockBreaker",
        "Scanner"
    };

    @Test
    public void customMachinesUseTheRegisteredFactoryAndKeepEveryPhysicalRecipeSlot() throws Throwable {
        run("build");
    }

    @Test
    public void prospectorKeepsItsSeparateInteractionContract() throws Throwable {
        run("legacy");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        if ("legacy".equals(scenario)) {
            checkLegacyExceptions();
            return;
        }
        if (!"build".equals(scenario)) {
            throw new AssertionError(scenario);
        }
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
            FMLServerHandler.instance();
            GTGuis.init();
            assertSame(MetaTileEntityGuiHandler.INSTANCE, GuiManager.getFactory("gregtech:mte"));
            int[] inputCounts = {0, 2, 1, 1, 2, 1, 1, 2, 1, 1, 1};
            int[] outputCounts = {0, 1, 9, 1, 2, 1, 0, 2, 1, 1, 1};
            for (int index = 0; index < MODELS.length; index++) {
                checkContainer(MODELS[index], inputCounts[index], outputCounts[index]);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkContainer(String model, int inputs, int outputs) throws Exception {
        GT_MetaTileEntity_BasicMachine machine = createMachine(model);
        Fixture fixture = new Fixture(machine);
        assertTrue(model, machine.useMui2());
        assertFalse(model, machine.allowSelectCircuit());
        assertEquals(model, inputs, machine.mInputSlotCount);
        assertEquals(model, outputs, machine.mOutputItems.length);
        assertEquals(4, machine.getInputSlot());
        assertEquals(4 + inputs, machine.getOutputSlot());
        assertEquals(6 + inputs + outputs, machine.mInventory.length);
        assertEquals(machine.mInventory.length - 1, machine.getCircuitSlot());
        assertEquals(inputs, machine.getAllInputs().length);
        assertThrows(IllegalArgumentException.class, () -> new GhostCircuitItemStackHandler(machine));

        ItemStack special = new ItemStack(Items.book, 3);
        machine.setInventorySlotContents(3, special);
        for (int index = 0; index < inputs; index++) {
            machine.setInventorySlotContents(4 + index, new ItemStack(Items.apple, index + 1));
        }
        for (int index = 0; index < outputs; index++) {
            machine.setInventorySlotContents(4 + inputs + index, new ItemStack(Items.stick, index + 1));
        }

        MetaTileEntityGuiHandler factory = MetaTileEntityGuiHandler.INSTANCE;
        PosGuiData guiData = new PosGuiData(fixture.player, 4, 64, 7);
        assertSame(machine, factory.getGuiHolder(guiData));
        assertTrue(factory.canInteractWith(fixture.player, guiData));
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager panelSync = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        ModularPanel panel = factory.createPanel(guiData, panelSync, settings);
        WidgetTree.collectSyncValues(panelSync, panel);
        ModularContainer container = new ModularContainer();
        container.construct(
            fixture.player,
            sync,
            settings,
            panel.getName(),
            guiData
        );
        assertTrue(container.isInitialized());
        assertFalse(container.isClient());
        assertTrue(container.canInteractWith(fixture.player));
        assertEquals(model, 38 + inputs + outputs, container.inventorySlots.size());
        assertNotNull(panelSync.findSyncHandler("powerSwitch"));
        assertNotNull(panelSync.findSyncHandler("fluidAutoOutput"));
        assertNotNull(panelSync.findSyncHandler("itemAutoOutput"));
        assertNull(panelSync.findPanelHandlerNullable("ghostCircuitPanel"));
        assertFalse(containsGhostSlot(panel));

        boolean[] visible = new boolean[machine.mInventory.length];
        ModularSlot specialSlot = null;
        for (Object entry : container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            if (slot.getItemHandler() != machine.getInventoryHandler()) {
                continue;
            }
            int nativeIndex = slot.getSlotIndex();
            assertFalse(model + " duplicates native slot " + nativeIndex, visible[nativeIndex]);
            visible[nativeIndex] = true;
            assertSame(machine.mInventory[nativeIndex], slot.getStack());
            assertTrue(slot.isCanTake());
            if (nativeIndex == 3) {
                specialSlot = slot;
            } else if (nativeIndex >= machine.getOutputSlot()) {
                assertFalse(slot.isCanPut());
            } else {
                assertTrue(slot.isCanPut());
            }
        }
        assertTrue(visible[machine.rechargerSlotStartIndex()]);
        assertNotNull(specialSlot);
        assertTrue(specialSlot.isCanPut());
        assertSame(special, specialSlot.getStack());
        assertSame(special, machine.getSpecialSlot());
        assertFalse(visible[0]);
        assertFalse(visible[2]);
        assertFalse(visible[machine.getCircuitSlot() - 1]);
        assertFalse(visible[machine.getCircuitSlot()]);
        for (int index = 0; index < inputs; index++) {
            assertTrue(visible[4 + index]);
            assertSame(machine.getInputAt(index), machine.getAllInputs()[index]);
        }
        for (int index = 0; index < outputs; index++) {
            assertTrue(visible[4 + inputs + index]);
            assertSame(machine.mInventory[4 + inputs + index], machine.getOutputAt(index));
        }

        ItemStack replacement = new ItemStack(Items.paper, 2);
        specialSlot.putStack(replacement);
        assertSame(replacement, machine.getSpecialSlot());
        assertTrue(fixture.tile.hasInventoryBeenModified());
        fixture.world.tiles.clear();
        assertFalse(container.canInteractWith(fixture.player));
    }

    private static boolean containsGhostSlot(IWidget widget) {
        if (widget instanceof GhostCircuitSlotWidget) {
            return true;
        }
        for (IWidget child : widget.getChildren()) {
            if (containsGhostSlot(child)) {
                return true;
            }
        }
        return false;
    }

    private static void checkLegacyExceptions() throws Exception {
        GT_MetaTileEntity_BasicMachine machine = createMachine("SeismicProspector");
        Fixture fixture = new Fixture(machine);
        assertFalse(machine.useMui2());
        assertFalse(machine.allowSelectCircuit());
        assertThrows(IllegalStateException.class, () -> MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(
            new PosGuiData(fixture.player, 4, 64, 7)
        ));
    }

    private static GT_MetaTileEntity_BasicMachine createMachine(String model) throws Exception {
        Class<?> type = Class.forName("gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_" + model);
        Class<?> descriptionType = "Printer".equals(model) ? String.class : String[].class;
        Constructor<?> constructor = type.getConstructor(
            String.class,
            int.class,
            descriptionType,
            ITexture[][][].class,
            String.class,
            String.class
        );
        Object description = descriptionType == String.class ? "" : new String[]{""};
        return (GT_MetaTileEntity_BasicMachine) constructor.newInstance(
            "mui2_custom_" + model,
            1,
            description,
            null,
            "",
            ""
        );
    }

    private static final class Fixture {

        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;

        private Fixture(GT_MetaTileEntity_BasicMachine machine) throws Exception {
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
        }
    }
}
