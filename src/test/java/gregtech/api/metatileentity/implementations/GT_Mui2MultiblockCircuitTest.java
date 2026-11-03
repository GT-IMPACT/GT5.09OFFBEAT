package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.logic.HeatExchangeProcessingLogic;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.modularui2.sync.GhostCircuitSyncHandler;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_WIDTH;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_Y;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2MultiblockCircuitTest {

    private static final String[] CIRCUIT_MACHINES = {
        "OilCracker", "LargeChemicalReactor", "HeatExchanger", "LargeBoiler_Bronze", "LargeBoiler_Steel",
        "LargeBoiler_Titanium", "LargeBoiler_TungstenSteel"
    };
    private static final String[] ORDINARY_MACHINES = {
        "VacuumFreezer", "ElectricBlastFurnace", "MultiFurnace", "ImplosionCompressor", "DistillationTower",
        "Cleanroom", "IndustrialPulverizer", "MultiblockCentrifuge", "MultiblockElectrolyzer", "Charcoal_Pit",
        "DieselEngine", "DieselEngine2", "FusionComputer1", "FusionComputer2", "FusionComputer3", "FusionComputer4",
        "FusionComputer5"
    };
    private static final String[] CONTROLLER_MACHINES = {
        "AirFilter", "AssemblyLine", "LargeTurbine_Gas", "LargeTurbine_HPSteam", "LargeTurbine_Plasma",
        "LargeTurbine_Steam", "OilDrill1", "OilDrill2", "OilDrill3", "ConcreteBackfiller1", "ConcreteBackfiller2"
    };

    @Test
    public void onlyConfiguredControllersBindTheSharedCircuitWidgetAndSelector() throws Throwable {
        run("routing");
    }

    @Test
    public void realSelectionsRemainBoundedAndCannotMovePhysicalItems() throws Throwable {
        run("packets");
    }

    @Test
    public void selectedZeroDrivesCombinedAndSeparatedRecipesWithoutSupplyingConsumables() throws Throwable {
        run("recipes");
    }

    @Test
    public void oldInventoriesMigrateValidSelectionsOnceAndNeverResurrectClearedCircuits() throws Throwable {
        run("migration");
    }

    @Test
    public void exchangerAndAllBoilersReadTheSelectedGhostInsteadOfTheOldPhysicalSlot() throws Throwable {
        run("thermal");
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
        new GT_IntegratedCircuit_Item();
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            GTGuis.init();
            switch (scenario) {
                case "routing":
                    prepareRoutingFixtures();
                    checkRouting();
                    break;
                case "packets":
                    checkPackets();
                    break;
                case "recipes":
                    checkRecipes();
                    break;
                case "migration":
                    checkMigration();
                    break;
                case "thermal":
                    checkThermal();
                    break;
                default:
                    throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void prepareRoutingFixtures() throws ClassNotFoundException {
        ItemList.AdsorptionFilter.set(new ItemStack(Items.paper));
        ItemList.AdsorptionFilterDirty.set(new ItemStack(Items.book));
        ic2.core.Ic2Items.miningPipe = new ItemStack(Blocks.fence);
        ic2.core.Ic2Items.miningPipeTip = new ItemStack(Blocks.nether_brick_fence);
        boolean previousPreload = GregTech_API.sPreloadStarted;
        GregTech_API.sPreloadStarted = true;
        try {
            Class.forName("gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DrillerBase");
        } finally {
            GregTech_API.sPreloadStarted = previousPreload;
        }
        Materials.Concrete.mStandardMoltenFluid = GT_ProcessingTestSupport.fluid("mui2_driller_concrete");
        GregTech_API.sBlockConcretes = Blocks.stained_hardened_clay;
    }

    private static void checkRouting() throws Exception {
        for (String model : CIRCUIT_MACHINES) {
            Fixture fixture = new Fixture(model);
            assertTrue(model, fixture.machine.allowSelectCircuit());
            assertEquals(3, fixture.machine.mInventory.length);
            assertEquals(2, fixture.machine.getCircuitSlot());
            assertFalse(fixture.machine.hasControllerSlotInGui());
            assertEquals(36, fixture.container.inventorySlots.size());
            assertSame(GhostCircuitSlotWidget.class, fixture.circuit.getClass());
            fixture.resize();
            assertEquals(17, fixture.circuit.getArea().x - fixture.panel.getArea().x);
            assertEquals(213, fixture.circuit.getArea().y - fixture.panel.getArea().y);
            assertEquals(18, fixture.circuit.getArea().width);
            assertEquals(18, fixture.circuit.getArea().height);
            assertEquals(18, fixture.machine.getCircuitSlotX());
            assertEquals(214, fixture.machine.getCircuitSlotY());
            checkNativePlayerInventory(fixture);
            GhostCircuitSyncHandler handler = fixture.circuit.getSyncHandler();
            assertSame(fixture.handlers.findSyncHandler("ghostCircuitIndex"), handler.getIndexSync());
            assertFalse(handler.getIndexSync().isAllowC2S());
            assertSame(fixture.machine.getInventoryHandler().getStackInSlot(2),
                handler.getGhostCircuitHandler().getStackInSlot(0));
            PanelSyncHandler popup = (PanelSyncHandler) fixture.handlers.findPanelHandler("ghostCircuitPanel");
            ModularPanel selector = popup.createUI(new PanelSyncManager(fixture.sync, false));
            assertEquals("circuit_selector", selector.getName());
            List<IWidget> choices = new ArrayList<>();
            collect(selector, choices);
            int configurations = 0;
            int selectionSlots = 0;
            for (IWidget widget : choices) {
                if (widget instanceof SlotLikeButtonWidget) {
                    selectionSlots++;
                    ItemStack stack = ((SlotLikeButtonWidget) widget).getStackForRecipeViewer();
                    if (stack != null) {
                        assertTrue(stack.getItem() instanceof GT_IntegratedCircuit_Item);
                        assertEquals(configurations++, stack.getItemDamage());
                    }
                }
            }
            assertEquals(27, configurations);
            assertEquals(28, selectionSlots);
            popup.openPanel();
            assertTrue(fixture.sync.isOpen("circuit_selector"));
        }
        for (String model : ORDINARY_MACHINES) {
            checkOrdinaryRouting(model, false);
        }
        for (String model : CONTROLLER_MACHINES) {
            checkOrdinaryRouting(model, true);
        }
    }

    private static void checkOrdinaryRouting(String model, boolean controller) throws Exception {
        Fixture fixture = new Fixture(model);
        assertFalse(model, fixture.machine.allowSelectCircuit());
        assertEquals(model, controller, fixture.machine.hasControllerSlotInGui());
        assertNull(fixture.circuit);
        assertNull(fixture.handlers.findPanelHandlerNullable("ghostCircuitPanel"));
        assertNull(fixture.handlers.findSyncHandlerNullable("ghostCircuitIndex", 0));
        fixture.resize();
        checkNativePlayerInventory(fixture);
    }

    private static void checkNativePlayerInventory(Fixture fixture) {
        assertEquals(GUI_WIDTH, fixture.panel.getArea().width);
        assertEquals(GUI_HEIGHT, fixture.panel.getArea().height);
        boolean controller = fixture.machine.hasControllerSlotInGui();
        assertEquals(controller ? 37 : 36, fixture.container.inventorySlots.size());
        int controllerSlots = 0;
        int playerSlots = 0;
        for (Object entry : fixture.container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            if ("controller".equals(slot.getSlotGroupName())) {
                assertSame(fixture.machine.getInventoryHandler(), slot.getItemHandler());
                assertEquals(1, slot.getSlotIndex());
                controllerSlots++;
                continue;
            }
            assertEquals("player_inventory", slot.getSlotGroupName());
            assertFalse(slot.getItemHandler() == fixture.machine.getInventoryHandler());
            playerSlots++;
        }
        assertEquals(controller ? 1 : 0, controllerSlots);
        assertEquals(36, playerSlots);
        int widgets = 0;
        for (IWidget widget : fixture.widgets) {
            if (widget.getName() == null || !widget.getName().startsWith("slot_")) {
                continue;
            }
            int index = Integer.parseInt(widget.getName().substring("slot_".length()));
            int row = index < 9 ? 3 : index / 9 - 1;
            int x = PLAYER_INVENTORY_X - 1 + index % 9 * 18;
            int y = PLAYER_INVENTORY_Y - 1 + (row == 3 ? 58 : row * 18);
            assertEquals(x, widget.getArea().x - fixture.panel.getArea().x);
            assertEquals(y, widget.getArea().y - fixture.panel.getArea().y);
            assertEquals(18, widget.getArea().width);
            assertEquals(18, widget.getArea().height);
            widgets++;
        }
        assertEquals(36, widgets);
    }

    private static void checkPackets() throws Exception {
        for (String model : CIRCUIT_MACHINES) {
            Fixture fixture = new Fixture(model);
            GhostCircuitItemStackHandler ghost = fixture.ghost();
            ItemStack physical = GT_Utility.getIntegratedCircuit(7);
            physical.stackSize = 3;
            fixture.machine.mInventory[1] = physical;
            ItemStack cursor = new ItemStack(Items.diamond, 5);
            fixture.player.inventory.setItemStack(cursor);
            fixture.select(0);
            assertEquals(0, ghost.getCircuitConfig());
            assertEquals(0, fixture.machine.getConfigurationCircuit().stackSize);
            fixture.select(24);
            assertEquals(24, ghost.getCircuitConfig());
            fixture.select(25);
            assertEquals(25, ghost.getCircuitConfig());
            fixture.select(26);
            assertEquals(26, ghost.getCircuitConfig());
            fixture.select(27);
            fixture.select(-2);
            assertEquals(26, ghost.getCircuitConfig());
            assertSame(cursor, fixture.player.inventory.getItemStack());
            assertEquals(5, cursor.stackSize);
            assertSame(physical, fixture.machine.mInventory[1]);
            assertEquals(3, physical.stackSize);
            int slot = fixture.machine.getCircuitSlot();
            ItemStack offered = GT_Utility.getIntegratedCircuit(0);
            offered.stackSize = 16;
            assertSame(offered, fixture.machine.getInventoryHandler().insertItem(slot, offered, false));
            assertNull(fixture.machine.getInventoryHandler().extractItem(slot, 1, true));
            assertNull(fixture.machine.getInventoryHandler().extractItem(slot, 1, false));
            assertNull(fixture.tile.decrStackSize(slot, 1));
            assertEquals(0, fixture.machine.getInventoryHandler().getSlotLimit(slot));
            assertFalse(fixture.machine.isValidSlot(slot));
            byte side = (byte) ForgeDirection.EAST.ordinal();
            assertFalse(fixture.tile.canInsertItem(slot, offered, side));
            assertFalse(fixture.tile.canExtractItem(slot, ghost.getStackInSlot(0), side));
            fixture.select(GhostCircuitItemStackHandler.NO_CONFIG);
            assertFalse(ghost.hasCircuit());
            assertNull(fixture.machine.getConfigurationCircuit());
            fixture.select(0);
            fixture.player.setPosition(100, 64, 100);
            fixture.select(24);
            assertEquals(0, ghost.getCircuitConfig());
        }
    }

    private static void checkRecipes() throws Exception {
        boolean previousPostload = GregTech_API.sPostloadFinished;
        GregTech_API.sPostloadFinished = true;
        try {
            for (String model : new String[]{"OilCracker", "LargeChemicalReactor"}) {
                for (boolean separated : new boolean[]{false, true}) {
                    checkRecipe(model, separated, false);
                    checkRecipe(model, separated, true);
                }
            }
        } finally {
            GregTech_API.sPostloadFinished = previousPostload;
        }
    }

    private static void checkRecipe(String model, boolean separated, boolean consumable) throws Exception {
        Fixture fixture = new Fixture(model);
        GT_Recipe_Map map = fixture.machine.getRecipeMap();
        map.clear();
        try {
            ItemStack circuit = GT_Utility.getIntegratedCircuit(0);
            circuit.stackSize = consumable ? 1 : 0;
            GT_Recipe recipe = map.addRecipe(
                false,
                new ItemStack[]{new ItemStack(Items.apple, 2), circuit},
                new ItemStack[]{new ItemStack(Items.gold_ingot)},
                null,
                null,
                new FluidStack[]{
                    new FluidStack(GT_ProcessingTestSupport.fluid("mui2_multiblock_feed"), 100),
                    new FluidStack(GT_ProcessingTestSupport.fluid("mui2_multiblock_reagent"), 50)
                },
                null,
                80,
                24,
                0
            );
            assertNotNull(recipe);
            ItemStack firstApples = new ItemStack(Items.apple, 4);
            ItemStack secondApples = new ItemStack(Items.apple, 4);
            GT_MetaTileEntity_Hatch_InputBus firstBus = fixture.resources.items(firstApples);
            fixture.resources.items(secondApples);
            fixture.resources.liquid(new FluidStack(GT_ProcessingTestSupport.fluid("mui2_multiblock_feed"), 300));
            fixture.resources.liquid(new FluidStack(GT_ProcessingTestSupport.fluid("mui2_multiblock_reagent"), 150));
            ItemStack physical = GT_Utility.getIntegratedCircuit(0);
            physical.stackSize = 3;
            fixture.machine.mInventory[1] = physical;
            fixture.machine.setProcessingModes(separated ? ProcessingLogic.SEPARATE_INPUTS : 0);
            assertFalse(
                "The preserved controller item must not act as a selected circuit",
                fixture.machine.checkRecipe(null)
            );
            fixture.select(7);
            assertFalse(fixture.machine.checkRecipe(null));
            fixture.select(0);
            List<ProcessingInputs> groups = fixture.machine.collectProcessingInputs();
            assertEquals(separated ? 2 : 1, groups.size());
            for (ProcessingInputs group : groups) {
                int selectors = 0;
                for (ItemStack stack : group.items) {
                    if (stack.getItem() instanceof GT_IntegratedCircuit_Item) {
                        assertEquals(0, stack.getItemDamage());
                        assertEquals(0, stack.stackSize);
                        selectors++;
                    }
                }
                assertEquals(1, selectors);
            }
            if (consumable) {
                assertFalse("A zero-count ghost cannot supply a consumed circuit", fixture.machine.checkRecipe(null));
                assertEquals(4, firstApples.stackSize);
                assertEquals(4, secondApples.stackSize);
                ItemStack supplied = GT_Utility.getIntegratedCircuit(0);
                supplied.stackSize = 2;
                firstBus.mInventory[1] = supplied;
                assertTrue(fixture.machine.checkRecipe(null));
                assertEquals(1, supplied.stackSize);
            } else {
                assertTrue(fixture.machine.checkRecipe(null));
            }
            assertEquals(6, firstApples.stackSize + secondApples.stackSize);
            assertEquals(3, physical.stackSize);
            assertSame(physical, fixture.machine.mInventory[1]);
            assertEquals(0, fixture.ghost().getCircuitConfig());
            assertEquals(0, fixture.machine.getConfigurationCircuit().stackSize);
        } finally {
            map.clear();
        }
    }

    private static void checkMigration() throws Exception {
        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_multiblock_circuit_test");
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[32000];
        try {
            for (String model : CIRCUIT_MACHINES) {
                for (int configuration : new int[]{0, 24, 25, 26, 27}) {
                    Fixture fixture = new Fixture(model);
                    ItemStack physical = GT_Utility.getIntegratedCircuit(configuration);
                    physical.stackSize = 3;
                    physical.setStackDisplayName("Preserved physical circuit " + configuration);
                    fixture.machine.mInventory[1] = physical;
                    GregTech_API.METATILEENTITIES[32000] = fixture.machine;
                    fixture.tile.setMetaTileID((short) 32000);
                    NBTTagCompound legacy = save(fixture.tile);
                    legacy.getCompoundTag("processing").removeTag("ghostCircuit");
                    BaseMetaTileEntity migratedTile = restore(fixture, legacy);
                    GT_MetaTileEntity_ProcessingMultiBlockBase<?> migrated = machine(migratedTile);
                    GhostCircuitItemStackHandler ghost = new GhostCircuitItemStackHandler(migrated);
                    assertEquals(configuration <= 26 ? configuration : GhostCircuitItemStackHandler.NO_CONFIG,
                        ghost.getCircuitConfig());
                    assertPhysicalCircuit(physical, migrated.mInventory[1]);
                    ghost.setCircuitConfig(26);
                    BaseMetaTileEntity selectedTile = restore(fixture, save(migratedTile));
                    GT_MetaTileEntity_ProcessingMultiBlockBase<?> selected = machine(selectedTile);
                    GhostCircuitItemStackHandler selectedGhost = new GhostCircuitItemStackHandler(selected);
                    assertEquals(26, selectedGhost.getCircuitConfig());
                    assertEquals(0, selectedGhost.getStackInSlot(0).stackSize);
                    assertPhysicalCircuit(physical, selected.mInventory[1]);
                    selectedGhost.setCircuitConfig(GhostCircuitItemStackHandler.NO_CONFIG);
                    NBTTagCompound cleared = save(selectedTile);
                    assertTrue(cleared.getCompoundTag("processing").getBoolean("ghostCircuit"));
                    GT_MetaTileEntity_ProcessingMultiBlockBase<?> reloaded = machine(restore(fixture, cleared));
                    assertFalse(new GhostCircuitItemStackHandler(reloaded).hasCircuit());
                    assertPhysicalCircuit(physical, reloaded.mInventory[1]);
                }
            }
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previous;
        }
    }

    private static NBTTagCompound save(BaseMetaTileEntity tile) {
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        return saved;
    }

    private static BaseMetaTileEntity restore(Fixture fixture, NBTTagCompound saved) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(fixture.resources.world);
        tile.readFromNBT(saved);
        assertNotNull(tile.getMetaTileEntity());
        return tile;
    }

    private static GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine(BaseMetaTileEntity tile) {
        return (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) tile.getMetaTileEntity();
    }

    private static void assertPhysicalCircuit(ItemStack expected, ItemStack actual) {
        assertNotNull(actual);
        assertSame(expected.getItem(), actual.getItem());
        assertEquals(expected.getItemDamage(), actual.getItemDamage());
        assertEquals(3, actual.stackSize);
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
    }

    private static void checkThermal() throws Exception {
        ItemList.Cell_Empty.set(new ItemStack(Items.bucket));
        ItemList.IC2_Fuel_Can_Filled.set(new ItemStack(Items.lava_bucket));
        ItemList.IC2_ForgeHammer.set(new ItemStack(Items.iron_pickaxe));
        ItemList.IC2_WireCutter.set(new ItemStack(Items.shears));
        Materials.Water.mFluid = FluidRegistry.WATER;
        for (String fluid : new String[]{"steam", "ic2distilledwater", "ic2hotcoolant", "ic2coolant",
            "ic2superheatedsteam", "ic2pahoehoelava"}) {
            GT_ProcessingTestSupport.fluid(fluid);
        }
        for (int[] expected : new int[][]{{0, 1000, 8000, 2000}, {24, 137, 717, 274},
            {25, 100, 512, 200}, {26, 62, 310, 124}}) {
            int configuration = expected[0];
            Fixture fixture = new Fixture("HeatExchanger");
            GT_MetaTileEntity_Hatch_Input hot = new GT_MetaTileEntity_Hatch_Input("hot", 2, "", null);
            GT_MetaTileEntity_Hatch_Output cold = new GT_MetaTileEntity_Hatch_Output("cold", 2, "", null);
            GT_MetaTileEntity_Hatch_Output steam = new GT_MetaTileEntity_Hatch_Output("steam", 4, "", null);
            fixture.resources.attach(hot);
            fixture.resources.attach(cold);
            fixture.resources.attach(steam);
            GT_ProcessingTestSupport.setField(fixture.machine, "mInputHotFluidHatch", hot);
            GT_ProcessingTestSupport.setField(fixture.machine, "mOutputColdFluidHatch", cold);
            fixture.machine.mOutputHatches.add(steam);
            hot.mFluid = new FluidStack(FluidRegistry.LAVA, 2000);
            fixture.machine.mInventory[1] = GT_Utility.getIntegratedCircuit(configuration == 0 ? 24 : 0);
            fixture.select(configuration);
            assertTrue(fixture.machine.checkRecipe(null));
            HeatExchangeProcessingLogic logic = (HeatExchangeProcessingLogic) fixture.machine.getProcessingLogic();
            assertEquals(expected[1], logic.getThreshold());
            assertEquals(expected[2], logic.getHeat());
            assertEquals(expected[3], cold.getFluidAmount());
            assertEquals(configuration, fixture.ghost().getCircuitConfig());
        }
        for (String model : CIRCUIT_MACHINES) {
            if (!model.startsWith("LargeBoiler_")) {
                continue;
            }
            for (int configuration : new int[]{0, 24, 25, 26}) {
                Fixture fixture = new Fixture(model);
                GT_MetaTileEntity_LargeBoiler boiler = (GT_MetaTileEntity_LargeBoiler) fixture.machine;
                fixture.machine.mEnergyHatches.clear();
                ItemStack coal = new ItemStack(Items.coal, 2);
                fixture.resources.items(coal);
                fixture.machine.mEfficiency = 10000;
                fixture.machine.mInventory[1] = GT_Utility.getIntegratedCircuit(configuration == 0 ? 24 : 0);
                fixture.select(configuration);
                assertTrue(fixture.machine.checkRecipe(null));
                assertEquals(Math.max(25, boiler.getEUt() - configuration * 25), fixture.machine.mEUt);
                assertEquals(1, coal.stackSize);
                assertEquals(configuration, fixture.ghost().getCircuitConfig());
            }
        }
    }

    private static void collect(IWidget widget, List<IWidget> widgets) {
        widgets.add(widget);
        for (IWidget child : widget.getChildren()) {
            collect(child, widgets);
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

        private final GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine;
        private final GT_ProcessingTestSupport.Fixture resources;
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final ModularSyncManager sync = new ModularSyncManager(false);
        private final PanelSyncManager handlers = new PanelSyncManager(sync, true);
        private final ModularPanel panel;
        private final ModularContainer container = new ModularContainer();
        private final List<IWidget> widgets = new ArrayList<>();
        private final GhostCircuitSlotWidget circuit;

        private Fixture(String model) throws Exception {
            machine = (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) GT_MultiblockRecipeProcessingTest.machine(model);
            resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
            tile = resources.tile;
            resources.world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            tile.disableWorking();
            player = new GT_StructureIntegrationTest.TestPlayer(resources.world);
            player.setPosition(4.5, 64.5, 7.5);
            PosGuiData guiData = new PosGuiData(player, 4, 64, 7);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(guiData, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                guiData
            );
            player.openContainer = container;
            collect(panel, widgets);
            GhostCircuitSlotWidget found = null;
            for (IWidget widget : widgets) {
                if (widget instanceof GhostCircuitSlotWidget) {
                    assertNull(found);
                    found = (GhostCircuitSlotWidget) widget;
                }
            }
            circuit = found;
        }

        private GhostCircuitItemStackHandler ghost() {
            assertNotNull(circuit);
            return circuit.getSyncHandler().getGhostCircuitHandler();
        }

        private void resize() {
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void select(int configuration) throws Exception {
            assertNotNull(circuit);
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeShort(configuration);
                circuit.getSyncHandler().readOnServer(GhostCircuitSyncHandler.SYNC_CIRCUIT_CONFIG, packet);
            } finally {
                packet.release();
            }
        }
    }

    private static final class Viewport extends StaticResizer {

        private final Area area = new Area(0, 0, GUI_WIDTH, GUI_HEIGHT);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Multiblock configuration circuit layout";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
