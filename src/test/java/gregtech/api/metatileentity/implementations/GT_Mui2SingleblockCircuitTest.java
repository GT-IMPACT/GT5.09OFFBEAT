package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.modularui2.sync.GhostCircuitSyncHandler;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2SingleblockCircuitTest {

    @Test
    public void selectedZeroAndChangedConfigurationDriveRealRecipesWithoutConsumingTheGhost() throws Throwable {
        run("recipes");
    }

    @Test
    public void ghostCannotReplaceAConsumableCircuit() throws Throwable {
        run("consumable");
    }

    @Test
    public void nativeInventoryPersistenceAndAutomationKeepTheGhostSeparateFromPhysicalSlots() throws Throwable {
        run("inventory");
    }

    @Test
    public void realMui2ContainerBindsTheGhostPopupAndValidatesServerSelections() throws Throwable {
        run("build");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        new GT_IntegratedCircuit_Item();
        GT_Recipe_Map.sAssemblerRecipes.clear();
        if ("recipes".equals(scenario)) {
            checkRecipes();
        } else if ("consumable".equals(scenario)) {
            checkConsumableCircuit();
        } else if ("inventory".equals(scenario)) {
            checkInventory();
        } else if ("build".equals(scenario)) {
            Thread thread = Thread.currentThread();
            String previousName = thread.getName();
            thread.setName("Server thread");
            try {
                checkServerBuild();
            } finally {
                thread.setName(previousName);
            }
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkRecipes() {
        GT_Recipe zero = recipe(0, Items.gold_ingot);
        GT_Recipe twentyFour = recipe(24, Items.iron_ingot);
        Fixture fixture = new Fixture();
        GT_MetaTileEntity_BasicMachine_GT_Recipe machine = fixture.machine;
        ItemStack special = new ItemStack(Items.book, 5);
        machine.setInventorySlotContents(3, special);
        fixture.fillInputs();

        assertEquals(GT_MetaTileEntity_BasicMachine.DID_NOT_FIND_RECIPE, machine.checkRecipe(true));
        assertEquals(3, machine.getInputAt(0).stackSize);
        fixture.ghost.setCircuitConfig(0);
        assertEquals(GT_MetaTileEntity_BasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, machine.checkRecipe(true));
        assertSame(zero, machine.mLastRecipe);
        assertSame(Items.gold_ingot, machine.mOutputItems[0].getItem());
        assertEquals(1, machine.getInputAt(0).stackSize);
        assertEquals(1, machine.getInputAt(1).stackSize);
        assertEquals(0, fixture.ghost.getCircuitConfig());
        assertEquals(0, fixture.ghost.getStackInSlot(0).stackSize);
        assertSame(special, machine.getSpecialSlot());
        assertEquals(5, special.stackSize);

        fixture.fillInputs();
        fixture.ghost.setCircuitConfig(24);
        assertEquals(GT_MetaTileEntity_BasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, machine.checkRecipe(true));
        assertSame(twentyFour, machine.mLastRecipe);
        assertSame(Items.iron_ingot, machine.mOutputItems[0].getItem());
        assertEquals(1, machine.getInputAt(0).stackSize);
        assertEquals(24, fixture.ghost.getCircuitConfig());
        assertEquals(0, fixture.ghost.getStackInSlot(0).stackSize);

        for (int configuration : new int[]{25, 26}) {
            GT_Recipe configured = recipe(configuration, Items.diamond);
            fixture.fillInputs();
            fixture.ghost.setCircuitConfig(configuration);
            assertEquals(GT_MetaTileEntity_BasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, machine.checkRecipe(true));
            assertSame(configured, machine.mLastRecipe);
            assertEquals(configuration, fixture.ghost.getCircuitConfig());
            assertEquals(0, fixture.ghost.getStackInSlot(0).stackSize);
        }

        fixture.fillInputs();
        fixture.ghost.setCircuitConfig(GhostCircuitItemStackHandler.NO_CONFIG);
        assertEquals(GT_MetaTileEntity_BasicMachine.DID_NOT_FIND_RECIPE, machine.checkRecipe(true));
        assertEquals(3, machine.getInputAt(0).stackSize);
        assertEquals(2, machine.getAllInputs().length);
    }

    private static void checkConsumableCircuit() {
        ItemStack requiredCircuit = GT_Utility.getIntegratedCircuit(0);
        requiredCircuit.stackSize = 1;
        assertNotNull(GT_Recipe_Map.sAssemblerRecipes.addRecipe(
            false,
            new ItemStack[]{new ItemStack(Items.apple), requiredCircuit},
            new ItemStack[]{new ItemStack(Items.diamond)},
            null,
            (FluidStack[]) null,
            null,
            20,
            8,
            0
        ));
        Fixture fixture = new Fixture();
        fixture.machine.setInventorySlotContents(4, new ItemStack(Items.apple, 2));
        fixture.ghost.setCircuitConfig(0);
        assertEquals(
            GT_MetaTileEntity_BasicMachine.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
            fixture.machine.checkRecipe(true)
        );
        assertEquals(2, fixture.machine.getInputAt(0).stackSize);
        assertNull(fixture.machine.mOutputItems[0]);
        assertEquals(0, fixture.ghost.getCircuitConfig());
        assertEquals(0, fixture.ghost.getStackInSlot(0).stackSize);

        ItemStack physicalCircuit = requiredCircuit.copy();
        fixture.machine.setInventorySlotContents(5, physicalCircuit);
        assertEquals(
            GT_MetaTileEntity_BasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
            fixture.machine.checkRecipe(true)
        );
        assertEquals(1, fixture.machine.getInputAt(0).stackSize);
        assertEquals(0, physicalCircuit.stackSize);
        assertEquals(0, fixture.ghost.getCircuitConfig());
        assertEquals(0, fixture.ghost.getStackInSlot(0).stackSize);
    }

    private static void checkInventory() {
        recipe(0, Items.gold_ingot);
        Fixture fixture = new Fixture();
        GT_MetaTileEntity_BasicMachine_GT_Recipe machine = fixture.machine;
        assertTrue(machine.useMui2());
        assertTrue(machine.allowSelectCircuit());
        assertEquals(4, machine.getInputSlot());
        assertEquals(6, machine.getOutputSlot());
        assertEquals(8, machine.getCircuitSlot());
        assertEquals(9, machine.mInventory.length);

        machine.setInventorySlotContents(3, new ItemStack(Items.book, 5));
        machine.setInventorySlotContents(4, new ItemStack(Items.apple, 3));
        machine.setInventorySlotContents(6, new ItemStack(Items.gold_ingot, 2));
        fixture.ghost.setCircuitConfig(0);
        byte side = (byte) ForgeDirection.EAST.ordinal();
        assertTrue(machine.allowPutStack(fixture.tile, 5, side, new ItemStack(Items.stick)));
        fixture.ghost.setCircuitConfig(12);
        assertFalse(machine.allowPutStack(fixture.tile, 5, side, new ItemStack(Items.stick)));
        fixture.ghost.setCircuitConfig(0);

        int circuitSlot = machine.getCircuitSlot();
        ItemStack offered = GT_Utility.getIntegratedCircuit(24);
        offered.stackSize = 16;
        assertSame(offered, machine.getInventoryHandler().insertItem(circuitSlot, offered, false));
        assertEquals(16, offered.stackSize);
        assertNull(machine.getInventoryHandler().extractItem(circuitSlot, 1, true));
        assertNull(machine.getInventoryHandler().extractItem(circuitSlot, 1, false));
        assertNull(fixture.tile.decrStackSize(circuitSlot, 1));
        assertEquals(0, machine.getInventoryHandler().getSlotLimit(circuitSlot));
        assertFalse(machine.isValidSlot(circuitSlot));
        assertFalse(fixture.tile.canInsertItem(circuitSlot, offered, side));
        assertFalse(fixture.tile.canExtractItem(circuitSlot, fixture.ghost.getStackInSlot(0), side));
        assertEquals(0, fixture.ghost.getCircuitConfig());
        assertTrue(fixture.tile.hasInventoryBeenModified());

        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_circuit_test");
        IMetaTileEntity previousPrototype = GregTech_API.METATILEENTITIES[32000];
        GregTech_API.METATILEENTITIES[32000] = machine;
        fixture.tile.setMetaTileID((short) 32000);
        try {
            NBTTagCompound saved = new NBTTagCompound();
            fixture.tile.writeToNBT(saved);
            BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
            restoredTile.setWorldObj(fixture.world);
            restoredTile.readFromNBT(saved);
            GT_MetaTileEntity_BasicMachine_GT_Recipe restored =
                (GT_MetaTileEntity_BasicMachine_GT_Recipe) restoredTile.getMetaTileEntity();
            assertNotNull(restored);
            GhostCircuitItemStackHandler restoredGhost = new GhostCircuitItemStackHandler(restored);
            assertEquals(0, restoredGhost.getCircuitConfig());
            assertEquals(0, restoredGhost.getStackInSlot(0).stackSize);
            assertSame(Items.book, restored.getSpecialSlot().getItem());
            assertEquals(5, restored.getSpecialSlot().stackSize);
            assertSame(Items.apple, restored.getInputAt(0).getItem());
            assertEquals(3, restored.getInputAt(0).stackSize);
            assertSame(Items.gold_ingot, restored.getOutputAt(0).getItem());
            assertEquals(2, restored.getOutputAt(0).stackSize);
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previousPrototype;
        }
    }

    private static void checkServerBuild() throws Exception {
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GTGuis.init();
        checkServerBuild(new Fixture(), 62);
        checkServerBuild(new Fixture(7), 80);
    }

    private static void checkServerBuild(Fixture fixture, int controlsY) throws Exception {
        fixture.ghost.setCircuitConfig(0);
        GT_ProcessingTestSupport.setField(fixture.tile, "mTickTimer", 41L);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(fixture.world);
        player.setPosition(4.5, 64.5, 7.5);
        assertTrue(fixture.tile.isUseableByPlayer(player));

        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager panelSync = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        PosGuiData guiData = new PosGuiData(player, 4, 64, 7);
        ModularPanel panel = fixture.machine.buildUI(guiData, panelSync, settings);
        WidgetTree.collectSyncValues(panelSync, panel);
        ModularContainer container = new ModularContainer();
        container.construct(
            player,
            sync,
            settings,
            panel.getName(),
            guiData
        );
        assertTrue(container.isInitialized());
        assertTrue(panelSync.isInitialised());
        GhostCircuitSlotWidget ghostSlot = findCircuitSlot(panel);
        assertNotNull(ghostSlot);
        checkControlAlignment(panel, ghostSlot, fixture.machine, controlsY);
        GhostCircuitSyncHandler circuitSync = ghostSlot.getSyncHandler();
        assertTrue(circuitSync.isValid());
        assertSame(panelSync.findSyncHandler("ghostCircuitIndex"), circuitSync.getIndexSync());
        assertEquals(0, circuitSync.getGhostCircuitHandler().getCircuitConfig());
        panelSync.findPanelHandler("ghostCircuitPanel").openPanel();
        assertTrue(sync.isOpen("circuit_selector"));

        sendSelection(circuitSync, 24);
        assertEquals(24, fixture.ghost.getCircuitConfig());
        sendSelection(circuitSync, 25);
        assertEquals(25, fixture.ghost.getCircuitConfig());
        sendSelection(circuitSync, 26);
        assertEquals(26, fixture.ghost.getCircuitConfig());
        sendSelection(circuitSync, 27);
        assertEquals(26, fixture.ghost.getCircuitConfig());
        sendSelection(circuitSync, -2);
        assertEquals(26, fixture.ghost.getCircuitConfig());
        sendSelection(circuitSync, GhostCircuitItemStackHandler.NO_CONFIG);
        assertFalse(fixture.ghost.hasCircuit());
        sendSelection(circuitSync, 0);
        assertEquals(0, fixture.ghost.getCircuitConfig());
        player.setPosition(100, 64, 100);
        sendSelection(circuitSync, 24);
        assertEquals(0, fixture.ghost.getCircuitConfig());
    }

    private static void sendSelection(GhostCircuitSyncHandler sync, int configuration) throws Exception {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeShort(configuration);
            sync.readOnServer(GhostCircuitSyncHandler.SYNC_CIRCUIT_CONFIG, buffer);
        } finally {
            buffer.release();
        }
    }

    private static void checkControlAlignment(
        ModularPanel panel,
        GhostCircuitSlotWidget ghost,
        GT_MetaTileEntity_BasicMachine machine,
        int controlsY
    ) {
        GT_Mui2RecipeSlotLayoutTest.resizeAreas(ghost);
        assertEquals(151, ghost.getArea().x);
        assertEquals(controlsY + CommonWidgets.MACHINE_HEADER_HEIGHT, ghost.getArea().y);
        assertEquals(18, ghost.getArea().width);
        assertEquals(18, ghost.getArea().height);
        int controls = 0;
        int powerButtons = 0;
        for (IWidget child : panel.getChildren()) {
            if (child instanceof ToggleButton) {
                GT_Mui2RecipeSlotLayoutTest.resizeAreas(child);
                assertEquals(4, child.getArea().y);
                assertEquals(4, 176 - child.getArea().ex());
                powerButtons++;
            }
            if (child.getClass() != ParentWidget.class) {
                continue;
            }
            GT_Mui2RecipeSlotLayoutTest.resizeAreas(child);
            for (IWidget widget : child.getChildren()) {
                if (!(widget instanceof ItemSlot)) {
                    continue;
                }
                ItemSlot slot = (ItemSlot) widget;
                int index = slot.getSlot().getSlotIndex();
                if (slot.getSlot().getItemHandler() == machine.getInventoryHandler()
                    && (index == machine.rechargerSlotStartIndex() || index == 3)) {
                    assertEquals(ghost.getArea().y, slot.getArea().y);
                    controls++;
                }
            }
        }
        assertEquals(2, controls);
        assertEquals(1, powerButtons);
    }

    private static GhostCircuitSlotWidget findCircuitSlot(IWidget widget) {
        if (widget instanceof GhostCircuitSlotWidget) {
            return (GhostCircuitSlotWidget) widget;
        }
        for (IWidget child : widget.getChildren()) {
            GhostCircuitSlotWidget circuit = findCircuitSlot(child);
            if (circuit != null) {
                return circuit;
            }
        }
        return null;
    }

    private static GT_Recipe recipe(int configuration, Item output) {
        GT_Recipe recipe = GT_Recipe_Map.sAssemblerRecipes.addRecipe(
            false,
            new ItemStack[]{new ItemStack(Items.apple, 2), new ItemStack(Items.stick),
                GT_Utility.getIntegratedCircuit(configuration)},
            new ItemStack[]{new ItemStack(output)},
            null,
            (FluidStack[]) null,
            null,
            20,
            8,
            0
        );
        assertNotNull(recipe);
        return recipe;
    }

    private static final class Fixture {

        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_MetaTileEntity_BasicMachine_GT_Recipe machine;
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GhostCircuitItemStackHandler ghost;

        private Fixture() {
            this(2);
        }

        private Fixture(int inputs) {
            machine = new GT_MetaTileEntity_BasicMachine_GT_Recipe(
                "mui2_circuit_test",
                1,
                "",
                GT_Recipe_Map.sAssemblerRecipes,
                inputs,
                1,
                1000,
                1,
                0,
                1,
                null,
                "",
                "",
                "",
                false,
                false,
                0
            );
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            ghost = new GhostCircuitItemStackHandler(machine);
            world.putTile(4, 64, 7, tile);
        }

        private void fillInputs() {
            machine.setInventorySlotContents(4, new ItemStack(Items.apple, 3));
            machine.setInventorySlotContents(5, new ItemStack(Items.stick, 2));
        }
    }
}
