package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.gui.GT_Slot_Holo;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static gregtech.api.metatileentity.implementations.GT_MultiblockRecipeProcessingTest.*;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

/**
 * Machine integration checks for the four controllers migrated after the
 * freezer.
 */
@RunWith(Parameterized.class)
public class GT_MigratedProcessingTest {
    private static GameRuntime runtime;
    private final String model;

    public GT_MigratedProcessingTest(String model) {
        this.model = model;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> machines() {
        return Arrays.asList(new Object[][] { { "MultiblockCentrifuge" }, { "MultiblockElectrolyzer" },
                { "IndustrialPulverizer" }, { "ImplosionCompressor" } });
    }

    @BeforeClass
    public static void openGame() throws Exception {
        runtime = new GameRuntime();
    }

    @AfterClass
    public static void closeGame() throws Exception {
        runtime.close();
    }

    private void run(String scenario) throws Throwable {
        runtime.run(getClass(), scenario, model);
    }

    public static void runScenario(String scenario, String model) throws Throwable {
        try {
            GT_MigratedProcessingTest.class.getDeclaredMethod(scenario, String.class).invoke(null, model);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    @Test
    public void batchStartsOneCycleAndPreservesMachineSound() throws Throwable {
        run("batchAndSound");
    }

    @Test
    public void protectedOutputsRejectWithoutConsumptionAndSurviveReload() throws Throwable {
        run("protectedOutputs");
    }

    @Test
    public void controllerInventoryIsIgnoredWithOrdinaryCombinedAndSeparatedInputs() throws Throwable {
        run("controllerInputs");
    }

    @Test
    public void controllerChangesDoNotAffectRecipesUsingInputBuses() throws Throwable {
        run("controllerTransaction");
    }

    @Test
    public void allModesAndPowerStateSurviveWorldReloadWithCorrectSlots() throws Throwable {
        run("controlsReload");
    }

    @Test
    public void legacyCycleFinishesOnceWithSavedEnergyAndDuration() throws Throwable {
        run("legacyCycle");
    }

    @Test
    public void normalAndPerfectOverclockReachOneTickWithoutExtraParallel() throws Throwable {
        run("oneTick");
    }

    @Test
    public void maintenanceBypassStillWorks() throws Throwable {
        run("maintenanceBypass");
    }

    private static void batchAndSound(String model) throws Exception {
        try (Scenario scenario = new Scenario(model, 1)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 40);
            f.fill();
            scenario.machine.setProcessingModes(ProcessingLogic.BATCH);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(1, scenario.machine.getCurrentParallel());
            assertEquals(3, scenario.machine.getCurrentOperations());
            assertEquals(120, scenario.machine.mMaxProgresstime);
            assertEquals(-24, scenario.machine.lEUt);
            assertEquals(0, f.iron.stackSize);
            assertEquals(0, f.stick.stackSize);
            assertEquals(9, scenario.machine.mOutputItems[0].stackSize);
            assertEquals(f.implosion() ? 1 : 0, scenario.machine.mSoundRequests);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(f.implosion() ? 1 : 0, scenario.machine.mSoundRequests);
        }
    }

    private static void protectedOutputs(String model) throws Exception {
        try (Scenario scenario = new Scenario(model, 1)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 3);
            f.recipe.mChances[1] = 10000;
            f.fill();
            GT_MetaTileEntity_Hatch_OutputBus items = scenario.itemOutput();
            GT_MetaTileEntity_Hatch_Output fluids = scenario.fluidOutput();
            fluids.mInventory[1] = new ItemStack(Items.diamond, 64);
            scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS);
            Arrays.fill(items.mInventory, new ItemStack(Items.diamond, 64));
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, scenario.machine.getProcessingResult());
            f.assertUnconsumed();
            Arrays.fill(items.mInventory, null);
            if (!f.implosion()) {
                fluids.mFluid = new FluidStack(fluid("processing_product"), fluids.getCapacity());
                assertFalse(scenario.machine.checkRecipe(null));
                assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, scenario.machine.getProcessingResult());
                f.assertUnconsumed();
                fluids.mFluid = null;
            }
            assertTrue(scenario.machine.checkRecipe(null));
            f.assertConsumed();
            Arrays.fill(items.mInventory, new ItemStack(Items.diamond, 64));
            f.fixture.tile.disableWorking();
            scenario.machine.completeRecipe();
            assertTrue(scenario.machine.isWaitingForOutput());
            assertEquals(0, scenario.machine.lEUt);
            NBTTagCompound saved = scenario.save();
            GT_MetaTileEntity_ProcessingMultiBlockBase<?> restored = sharedMachine(model);
            Fixture restoredFixture = new Fixture(restored, 1);
            restoredFixture.tile.disableWorking();
            restored.mOutputBusses.add(items);
            restored.mOutputHatches.add(fluids);
            restored.loadNBTData(saved);
            assertTrue(restored.isWaitingForOutput());
            Arrays.fill(items.mInventory, null);
            restored.completeRecipe();
            assertFalse(restored.isWaitingForOutput());
            assertEquals(3, itemCount(items, Items.gold_ingot));
            assertEquals(2, itemCount(items, Items.flint));
            if (!f.implosion()) {
                assertEquals(75, fluids.mFluid.amount);
            }
            restored.completeRecipe();
            assertEquals(3, itemCount(items, Items.gold_ingot));
            assertEquals(2, itemCount(items, Items.flint));
            assertEquals(4, f.iron.stackSize);
            assertEquals(2, f.stick.stackSize);
            if (!f.implosion()) {
                assertEquals(200, f.water.amount);
            }
        }
    }

    private static void controllerInputs(String model) throws Exception {
        for (boolean separated : new boolean[] { false, true }) {
            for (boolean combined : new boolean[] { false, true }) {
                try (Scenario scenario = new Scenario(model, 1)) {
                    RecipeFixture f = scenario.recipe;
                    f.register(24, 80);
                    f.fill();
                    f.machine.mInputBusses.clear();
                    if (combined) {
                        GTMTE_BusHatch_Input bus = new GTMTE_BusHatch_Input("test", 2, new String[0], null, 32000);
                        f.fixture.attach(bus);
                        bus.mInventory[3] = f.iron;
                        f.machine.mInputBusHatches.add(bus);
                    } else {
                        f.fixture.items(f.iron);
                    }
                    scenario.machine.mInventory[1] = f.stick;
                    scenario.machine.setProcessingModes(separated ? ProcessingLogic.SEPARATE_INPUTS : 0);
                    assertFalse(scenario.machine.hasControllerSlotInGui());
                    assertFalse("separated=" + separated + ", combined=" + combined,
                            scenario.machine.checkRecipe(null));
                    f.assertUnconsumed();
                    assertEquals(3, f.stick.stackSize);
                }
            }
        }
    }

    private static void controllerTransaction(String model) throws Exception {
        for (boolean separated : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(model, 1)) {
                RecipeFixture f = scenario.recipe;
                f.register(24, 80);
                f.fill();
                ItemStack controller = new ItemStack(Items.stick, 3);
                ItemStack replacement = controller.copy();
                scenario.machine.mInventory[1] = controller;
                scenario.machine.setProcessingModes(separated ? ProcessingLogic.SEPARATE_INPUTS : 0);
                scenario.machine.getProcessingLogic().setRecipeValidator(recipe -> {
                    scenario.machine.mInventory[1] = replacement;
                    return CheckRecipeResult.SUCCESS;
                });
                assertTrue(scenario.machine.checkRecipe(null));
                assertEquals(3, controller.stackSize);
                assertEquals(3, replacement.stackSize);
                f.assertConsumed();
            }
        }
    }

    private static void controlsReload(String model) throws Exception {
        int machineId = 1003;
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[machineId];
        GregTech_API.METATILEENTITIES[machineId] = sharedMachine(model);
        try {
            for (boolean enabled : new boolean[] { false, true }) {
                for (int modes = 0; modes <= ProcessingLogic.ALL_MODES; modes++) {
                    ProcessingWorld world = new ProcessingWorld();
                    BaseMetaTileEntity tile = new BaseMetaTileEntity();
                    tile.setWorldObj(world);
                    tile.setInitialValuesAsNBT(null, (short) machineId);
                    GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine = (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) tile
                            .getMetaTileEntity();
                    machine.setProcessingModes(modes);
                    machine.mInventory[1] = new ItemStack(Items.stick, 3);
                    if (enabled) {
                        tile.enableWorking();
                    } else {
                        tile.disableWorking();
                    }
                    NBTTagCompound saved = new NBTTagCompound();
                    tile.writeToNBT(saved);
                    BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
                    restoredTile.setWorldObj(world);
                    restoredTile.readFromNBT(CompressedStreamTools.func_152457_a(CompressedStreamTools.compress(saved),
                            NBTSizeTracker.field_152451_a));
                    GT_MetaTileEntity_ProcessingMultiBlockBase<?> restored = (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) restoredTile
                            .getMetaTileEntity();
                    assertEquals(machine.getClass(), restored.getClass());
                    assertEquals(modes, restored.getProcessingModes());
                    assertEquals(enabled, restoredTile.isAllowedToWork());
                    assertEquals(3, restored.mInventory[1].stackSize);
                    assertSame(machine.getRecipeMap(), restored.getRecipeMap());
                    GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
                    GT_Container_ProcessingMachine container = (GT_Container_ProcessingMachine) restored.getServerGUI(0,
                            player.inventory, restoredTile);
                    for (int index = 0; index < GT_Container_ProcessingMachine.CONTROL_COUNT; index++) {
                        assertTrue(container.getSlot(index) instanceof GT_Slot_Holo);
                    }
                    boolean supportsController = model.equals("LargeChemicalReactor");
                    int controllerSlots = supportsController ? 1 : 0;
                    assertEquals(model, supportsController, restored.hasControllerSlotInGui());
                    assertEquals(42 + controllerSlots, container.inventorySlots.size());
                    assertEquals(controllerSlots, container.getSlotCount());
                    assertEquals(controllerSlots, container.getShiftClickSlotCount());
                    if (supportsController) {
                        assertFalse(container.getSlot(container.getSlotStartIndex()) instanceof GT_Slot_Holo);
                        assertSame(
                                restored.mInventory[1],
                                container.getSlot(container.getSlotStartIndex()).getStack()
                        );
                    }
                }
            }
        } finally {
            GregTech_API.METATILEENTITIES[machineId] = previous;
        }
    }

    private static void legacyCycle(String model) throws Exception {
        try (Scenario scenario = new Scenario(model, 1)) {
            GT_MetaTileEntity_MultiBlockBase legacy = new GT_LegacyProcessingTestMachine("legacy");
            new Fixture(legacy, 1);
            legacy.mEUt = -96;
            legacy.mEfficiency = 10000;
            legacy.mMaxProgresstime = 20;
            legacy.mProgresstime = 17;
            legacy.mOutputItems = new ItemStack[] { new ItemStack(Items.gold_ingot, 3) };
            NBTTagCompound saved = new NBTTagCompound();
            legacy.saveNBTData(saved);
            assertFalse(saved.hasKey("processing"));
            scenario.machine.loadNBTData(saved);
            ItemStack input = new ItemStack(Items.iron_ingot, 6);
            scenario.recipe.fixture.items(input);
            GT_MetaTileEntity_Hatch_OutputBus output = scenario.itemOutput();
            scenario.prepareTicks();
            assertEquals(-96, scenario.machine.lEUt);
            assertEquals(20, scenario.machine.mMaxProgresstime);
            assertEquals(17, scenario.machine.mProgresstime);
            assertFalse(scenario.machine.checkRecipe(null));
            for (int tick = 1; tick <= 4; tick++) {
                scenario.machine.onPostTick(scenario.recipe.fixture.tile, tick);
            }
            assertEquals(0, scenario.machine.mMaxProgresstime);
            assertEquals(3, itemCount(output, Items.gold_ingot));
            assertEquals(1000000 - 3 * 96, scenario.machine.mEnergyHatches.get(0).getEUVar());
            assertEquals(6, input.stackSize);
        }
    }

    private static void oneTick(String model) throws Exception {
        try (Scenario scenario = new Scenario(model, 4)) {
            scenario.recipe.register(24, 1);
            scenario.recipe.fill();
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(1, scenario.machine.mMaxProgresstime);
            assertEquals(-1536, scenario.machine.lEUt);
            assertEquals(1, scenario.machine.getCurrentOperations());
            assertEquals(1, scenario.machine.getCurrentParallel());
            scenario.recipe.assertConsumed();
        }
    }

    private static void maintenanceBypass(String model) throws Exception {
        boolean previous = GT_MetaTileEntity_MultiBlockBase.disableMaintenance;
        try {
            for (boolean global : new boolean[] { false, true }) {
                try (Scenario scenario = new Scenario(model, 1)) {
                    scenario.recipe.register(24, 80);
                    scenario.recipe.fill();
                    GT_MetaTileEntity_Hatch_Maintenance maintenance = new GT_MetaTileEntity_Hatch_Maintenance("test", 1,
                            "test", null, false);
                    scenario.recipe.fixture.attach(maintenance);
                    scenario.machine.mMaintenanceHatches.add(maintenance);
                    GT_MetaTileEntity_MultiBlockBase.disableMaintenance = global;
                    scenario.machine.enabledMaintenance = global;
                    scenario.machine.mWrench = scenario.machine.mScrewdriver = scenario.machine.mSoftHammer = scenario.machine.mHardHammer = scenario.machine.mSolderingTool = scenario.machine.mCrowbar = false;
                    scenario.prepareTicks();
                    scenario.recipe.fixture.tile.enableWorking();
                    scenario.machine.onPostTick(scenario.recipe.fixture.tile, 100);
                    assertEquals(6, scenario.machine.getRepairStatus());
                    assertEquals(10000, scenario.machine.mEfficiency);
                    scenario.recipe.assertConsumed();
                }
            }
        } finally {
            GT_MetaTileEntity_MultiBlockBase.disableMaintenance = previous;
        }
    }

    private static GT_MetaTileEntity_ProcessingMultiBlockBase<?> sharedMachine(String model) throws Exception {
        return (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine(model);
    }

    private static int itemCount(GT_MetaTileEntity_Hatch_OutputBus bus, net.minecraft.item.Item item) {
        int count = 0;
        for (ItemStack stack : bus.mInventory) {
            if (stack != null && stack.getItem() == item) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private static final class Scenario implements AutoCloseable {
        final RecipeFixture recipe;
        final GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine;

        Scenario(String model, int tier) throws Exception {
            recipe = new RecipeFixture(model, tier);
            machine = (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) recipe.machine;
        }

        GT_MetaTileEntity_Hatch_OutputBus itemOutput() {
            GT_MetaTileEntity_Hatch_OutputBus bus = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            recipe.fixture.attach(bus);
            machine.mOutputBusses.add(bus);
            return bus;
        }

        GT_MetaTileEntity_Hatch_Output fluidOutput() {
            GT_MetaTileEntity_Hatch_Output hatch = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);
            recipe.fixture.attach(hatch);
            machine.mOutputHatches.add(hatch);
            return hatch;
        }

        NBTTagCompound save() {
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            return saved;
        }

        void prepareTicks() {
            machine.mMachine = true;
            machine.mStartUpCheck = -1;
            machine.mUpdate = 1000;
            recipe.fixture.tile.disableWorking();
        }

        @Override
        public void close() {
            recipe.map.clear();
            GregTech_API.sPostloadFinished = recipe.previousPostload;
        }
    }
}
