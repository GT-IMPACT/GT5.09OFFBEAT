package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.gui.GT_Slot_Holo;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.logic.ProcessingPersistence;
import gregtech.api.net.GT_Packet_ProcessingOutputs;
import com.google.common.io.ByteStreams;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.minecraft.inventory.ICrafting;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.Fixture;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.GameRuntime;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.fluid;
import static org.junit.Assert.*;

public class GT_SharedProcessingTest {
    private static GameRuntime runtime;

    @BeforeClass
    public static void openGame() throws Exception {
        runtime = new GameRuntime();
    }

    @AfterClass
    public static void closeGame() throws Exception {
        runtime.close();
    }

    private void run(String scenario) throws Throwable {
        runtime.run(getClass(), scenario, "");
    }

    public static void runScenario(String scenario, String ignored) throws Throwable {
        try {
            GT_SharedProcessingTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    @Test
    public void dynamicLimitIsReadAgainForCachedRecipe() throws Throwable {
        run("dynamicLimit");
    }

    @Test
    public void independentMachineConditionCannotBeBypassed() throws Throwable {
        run("machineCondition");
    }

    @Test
    public void subTickBypassIsAnExplicitMachineSetting() throws Throwable {
        run("subTick");
    }

    @Test
    public void batchExtendsTimeWithoutIncreasingSimultaneousParallel() throws Throwable {
        run("batch");
    }

    @Test
    public void stainlessCoolingBatchRequiresTwoOperationsToFitTheTimeLimit() throws Throwable {
        run("stainlessCoolingBatch");
    }

    @Test
    public void protectedOutputsRejectStartWithoutConsumption() throws Throwable {
        run("protectedOutputs");
    }

    @Test
    public void partialDeliverySurvivesReloadWithoutDuplication() throws Throwable {
        run("pendingOutputs");
    }

    @Test
    public void recipeLockAndModesSurviveReload() throws Throwable {
        run("recipeLock");
    }

    @Test
    public void removedCachedRecipeIsNotReused() throws Throwable {
        run("cacheReload");
    }

    @Test
    public void splitBusesAndCombinedHatchesUseTheSameSnapshotRules() throws Throwable {
        run("inputGroups");
    }

    @Test
    public void longConsumptionDrainsMultipleBuffersAndSurvivesReload() throws Throwable {
        run("longPower");
    }

    @Test
    public void overflowAndCustomValidationDoNotConsumeAnything() throws Throwable {
        run("failedPreparation");
    }

    @Test
    public void inputSnapshotsDeduplicateAliasesAndRejectConcurrentChanges() throws Throwable {
        run("inputTransaction");
    }

    @Test
    public void legacyRunningCycleResumesWithoutReconsumption() throws Throwable {
        run("legacySave");
    }

    @Test
    public void controlsAreServerValidatedHolographicSlots() throws Throwable {
        run("controlSlots");
    }

    @Test
    public void meAdaptersReserveSharedCapacityAndKeepUnacceptedOutput() throws Throwable {
        run("meOutputs");
    }

    @Test
    public void ordinaryAndMultiAmpHatchesProvideNominalAmperage() throws Throwable {
        run("nominalAmperage");
    }

    @Test
    public void largeBatchOutputCountSurvivesNbtWithoutByteTruncation() throws Throwable {
        run("largeOutputSave");
    }

    @Test
    public void chanceBoundaryAndOutputReservationUsePreparedResults() throws Throwable {
        run("outputChances");
    }

    @Test
    public void lockedFluidOutputsAndMeFluidsRespectCapacity() throws Throwable {
        run("fluidAdapters");
    }

    @Test
    public void mapChangesInvalidateCachedRecipe() throws Throwable {
        run("mapChanges");
    }

    @Test
    public void powerAndOutputOverflowDoNotCrossTheConsumptionBoundary() throws Throwable {
        run("overflowBoundary");
    }

    @Test
    public void onlySelectedMapIsSearched() throws Throwable {
        run("selectedMapOnly");
    }

    @Test
    public void switchingMapsResetsCacheAndRecipeLock() throws Throwable {
        run("mapSwitchResetsRecipeState");
    }

    @Test
    public void selectedMapAndInFlightRecipeSurviveReloadIndependently() throws Throwable {
        run("mapSwitchDuringCycle");
    }

    @Test
    public void missingSavedMapDoesNotSilentlySelectAnotherMap() throws Throwable {
        run("missingSavedMap");
    }

    @Test
    public void switchingMapsUpdatesAllInputHatchFilters() throws Throwable {
        run("mapHatchFilters");
    }

    @Test
    public void mapSelectionUsesServerValidatedHolographicSlotAndClientSync() throws Throwable {
        run("mapControlSlot");
    }

    @Test
    public void mapChangeDuringPreparationDoesNotConsumeInputs() throws Throwable {
        run("mapChangeBeforeCommit");
    }

    @Test
    public void switchingMapsPreservesBlockedOutputs() throws Throwable {
        run("mapSwitchWithPendingOutputs");
    }

    private static void dynamicLimit() {
        try (Scenario scenario = new Scenario(3)) {
            scenario.recipe(24, 80, false);
            ItemStack input = scenario.inputs(64, false);
            for (int limit : new int[] { 1, 16, 1 }) {
                scenario.machine.limit = limit;
                assertTrue(scenario.machine.checkRecipe(null));
                assertEquals(limit, scenario.machine.getAvailableParallel());
                assertEquals(limit, scenario.machine.getCurrentParallel());
                int duration = scenario.machine.mMaxProgresstime;
                scenario.machine.limit = 7;
                assertEquals(limit, scenario.machine.getCurrentParallel());
                assertEquals(duration, scenario.machine.mMaxProgresstime);
                scenario.finish();
            }
            assertEquals(46, input.stackSize);
            assertEquals(3, scenario.machine.limitReads);
        }
    }

    @Test
    public void globalMaintenanceBypassPreservesEfficiencyAndRunningCycle() throws Throwable {
        run("globalMaintenanceBypass");
    }

    @Test
    public void machineMaintenanceBypassPreservesEfficiencyAndRunningCycle() throws Throwable {
        run("machineMaintenanceBypass");
    }

    private static void globalMaintenanceBypass() {
        maintenanceBypass(true);
    }

    private static void machineMaintenanceBypass() {
        maintenanceBypass(false);
    }

    private static void maintenanceBypass(boolean global) {
        boolean previousGlobal = GT_MetaTileEntity_MultiBlockBase.disableMaintenance;
        try {
            for (int repairedTools = 0; repairedTools < 64; repairedTools++) {
                try (Scenario scenario = new Scenario(1)) {
                    scenario.recipe(24, 80, false);
                    ItemStack input = scenario.inputs(2, false);
                    GT_MetaTileEntity_Hatch_OutputBus output = scenario.itemOutput();
                    GT_MetaTileEntity_Hatch_Maintenance maintenance = new GT_MetaTileEntity_Hatch_Maintenance("test", 1,
                            "test", null, false);
                    scenario.fixture.attach(maintenance);
                    scenario.machine.mMaintenanceHatches.add(maintenance);
                    GT_MetaTileEntity_MultiBlockBase.disableMaintenance = global;
                    scenario.machine.enabledMaintenance = global;
                    scenario.machine.mWrench = (repairedTools & 1) != 0;
                    scenario.machine.mScrewdriver = (repairedTools & 2) != 0;
                    scenario.machine.mSoftHammer = (repairedTools & 4) != 0;
                    scenario.machine.mHardHammer = (repairedTools & 8) != 0;
                    scenario.machine.mSolderingTool = (repairedTools & 16) != 0;
                    scenario.machine.mCrowbar = (repairedTools & 32) != 0;
                    scenario.machine.mMachine = true;
                    scenario.machine.mStartUpCheck = -1;
                    scenario.machine.mUpdate = 1000;
                    scenario.fixture.tile.enableWorking();

                    scenario.machine.onPostTick(scenario.fixture.tile, 100);
                    assertEquals("Repair mask: " + repairedTools, 6, scenario.machine.getRepairStatus());
                    assertEquals(10000, scenario.machine.mEfficiency);
                    assertEquals(80, scenario.machine.mMaxProgresstime);
                    assertEquals(1, input.stackSize);
                    scenario.fixture.tile.disableWorking();

                    scenario.machine.mRuntime = 1001;
                    scenario.machine.onPostTick(scenario.fixture.tile, 101);
                    assertEquals(1000000 - 24, scenario.machine.mEnergyHatches.get(0).getEUVar());
                    NBTTagCompound saved = scenario.save();
                    for (String tool : new String[] { "mWrench", "mScrewdriver", "mSoftHammer", "mHardHammer",
                            "mSolderingTool", "mCrowbar" }) {
                        saved.setBoolean(tool, false);
                    }
                    scenario.machine.loadNBTData(saved);
                    for (int tick = 102; tick <= 180; tick++) {
                        scenario.machine.onPostTick(scenario.fixture.tile, tick);
                    }
                    assertEquals(6, scenario.machine.getRepairStatus());
                    assertEquals(10000, scenario.machine.mEfficiency);
                    assertEquals(1000000 - 80 * 24, scenario.machine.mEnergyHatches.get(0).getEUVar());
                    assertEquals(0, scenario.machine.mMaxProgresstime);
                    assertEquals(1, output.mInventory[0].stackSize);
                    assertEquals(1, input.stackSize);
                    assertEquals(0, scenario.fixture.tile.getErrorDisplayID() & 63);
                }
            }
        } finally {
            GT_MetaTileEntity_MultiBlockBase.disableMaintenance = previousGlobal;
        }
    }

    private static void machineCondition() {
        try (Scenario scenario = new Scenario(3)) {
            scenario.recipe(24, 1, false);
            ItemStack input = scenario.inputs(64, false);
            scenario.machine.limit = 16;
            scenario.machine.bypass = true;
            scenario.machine.allowed = false;
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.MACHINE_CONDITION, scenario.machine.getProcessingResult());
            assertEquals(64, input.stackSize);
            scenario.machine.allowed = true;
            scenario.machine.limit = 0;
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(64, input.stackSize);
            scenario.machine.limit = 1;
            assertTrue(scenario.machine.checkRecipe(null));
        }
    }

    private static void subTick() {
        for (boolean bypass : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(3)) {
                scenario.recipe(24, 1, false);
                scenario.inputs(64, false);
                scenario.machine.bypass = bypass;
                assertTrue(scenario.machine.checkRecipe(null));
                assertEquals(bypass ? 4 : 1, scenario.machine.getCurrentParallel());
                assertEquals(1, scenario.machine.mMaxProgresstime);
                assertEquals(bypass ? 4 : 1, scenario.machine.mOutputItems[0].stackSize);
            }
        }
    }

    private static void batch() {
        for (int available : new int[] { 1, 3, 128, 200 }) {
            try (Scenario scenario = new Scenario(1)) {
                scenario.recipe(24, 1, false);
                ItemStack input = scenario.inputs(available, false);
                scenario.machine.setProcessingModes(ProcessingLogic.BATCH);
                assertTrue(scenario.machine.checkRecipe(null));
                int operations = Math.min(128, available);
                assertEquals(1, scenario.machine.getCurrentParallel());
                assertEquals(operations, scenario.machine.getCurrentOperations());
                assertEquals(operations, scenario.machine.mMaxProgresstime);
                assertEquals(-24, scenario.machine.lEUt);
                assertEquals(available - operations, input.stackSize);
            }
        }
    }

    private static void stainlessCoolingBatch() {
        for (int tier : new int[] { 3, 4 }) {
            try (Scenario scenario = new Scenario(tier)) {
                int duration = (int) (gregtech.api.enums.Materials.StainlessSteel.getMass() * 3);
                // Use the hot-ingot registration's duration and default freezer EU/t.
                // Vanilla items stand in for the hot and cooled ingot identities.
                GT_Recipe recipe = new GT_Recipe(new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot),
                        duration);
                assertEquals(165, recipe.mDuration);
                assertEquals(120, recipe.mEUt);
                ItemStack input = scenario.inputs(64, false);
                scenario.itemOutput();
                assertTrue(scenario.machine.checkRecipe(null));
                assertEquals(1, scenario.machine.getCurrentOperations());
                int singleDuration = tier == 3 ? 82 : 41;
                long eut = scenario.machine.lEUt;
                assertEquals(singleDuration, scenario.machine.mMaxProgresstime);

                GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                        scenario.fixture.world);
                GT_Container_ProcessingMachine container = new GT_Container_ProcessingMachine(player.inventory,
                        scenario.fixture.tile) {
                    @Override
                    public boolean canInteractWith(EntityPlayer entity) {
                        return true;
                    }
                };
                player.openContainer = container;
                container.slotClick(GT_Container_ProcessingMachine.BATCH_SLOT, 0, 0, player);
                assertEquals(ProcessingLogic.BATCH, scenario.machine.getProcessingModes());
                assertEquals(singleDuration, scenario.machine.mMaxProgresstime);
                assertEquals(1, scenario.machine.getCurrentOperations());
                scenario.finish();

                assertTrue(scenario.machine.checkRecipe(null));
                int expectedOperations = tier == 3 ? 1 : 3;
                assertEquals(1, scenario.machine.getCurrentParallel());
                assertEquals(expectedOperations, scenario.machine.getCurrentOperations());
                assertEquals(singleDuration * expectedOperations, scenario.machine.mMaxProgresstime);
                assertEquals(eut, scenario.machine.lEUt);
                assertEquals(expectedOperations, scenario.machine.mOutputItems[0].stackSize);
                assertEquals(63 - expectedOperations, input.stackSize);
            }
        }
    }

    private static void protectedOutputs() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, 80, true);
            ItemStack input = scenario.inputs(2, true);
            scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, scenario.machine.getProcessingResult());
            assertEquals(2, input.stackSize);
            scenario.itemOutput();
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, scenario.machine.getProcessingResult());
            assertEquals(2, input.stackSize);
            assertEquals(200, scenario.machine.mInputHatches.get(0).mFluid.amount);
            scenario.fluidOutput();
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(1, input.stackSize);
        }
    }

    private static void pendingOutputs() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, 1, true);
            scenario.inputs(5, true);
            scenario.machine.limit = 3;
            GT_MultiblockRecipeProcessingTest.supplyHatches(scenario.fixture, 1, 4, false, false);
            scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS);
            GT_MetaTileEntity_Hatch_OutputBus output = scenario.itemOutput();
            GT_MetaTileEntity_Hatch_Output fluidOutput = scenario.fluidOutput();
            assertTrue(scenario.machine.checkRecipe(null));
            for (int index = 0; index < output.mInventory.length; index++) {
                output.mInventory[index] = new ItemStack(Items.diamond, 64);
            }
            output.mInventory[0] = new ItemStack(Items.gold_ingot, 63);
            fluidOutput.mFluid = new FluidStack(fluid("shared_product"), fluidOutput.getCapacity() - 25);
            fluidOutput.mInventory[1] = new ItemStack(Items.diamond, 64);
            scenario.machine.setProcessingModes(0); // The active cycle keeps its protection settings.
            scenario.finish();
            assertTrue(scenario.machine.isWaitingForOutput());
            assertEquals(64, output.mInventory[0].stackSize);
            assertEquals(2, scenario.machine.mOutputItems[0].stackSize);
            assertEquals(200, scenario.machine.mOutputFluids[0].amount);
            long stored = scenario.machine.mEnergyHatchesMulti.get(0).getEUVar();
            assertTrue(scenario.machine.onRunningTick(null));
            assertEquals(stored, scenario.machine.mEnergyHatchesMulti.get(0).getEUVar());
            scenario.machine.stopMachine();
            NBTTagCompound saved = scenario.save();
            TestFreezer restored = new TestFreezer();
            Fixture fixture = new Fixture(restored, 1);
            fixture.tile.disableWorking();
            restored.loadNBTData(saved);
            assertTrue(restored.isWaitingForOutput());
            restored.mOutputBusses.add(output);
            restored.mOutputHatches.add(fluidOutput);
            output.mInventory[1] = null;
            fluidOutput.mFluid = null;
            restored.completeRecipe();
            assertFalse(restored.isWaitingForOutput());
            assertEquals(2, output.mInventory[1].stackSize);
            assertEquals(200, fluidOutput.mFluid.amount);
            restored.completeRecipe();
            assertEquals(2, output.mInventory[1].stackSize);
            assertEquals(200, fluidOutput.mFluid.amount);
        }
    }

    private static void recipeLock() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, 1, false);
            scenario.inputs(3, false);
            scenario.machine.setProcessingModes(ProcessingLogic.LOCK_RECIPE | ProcessingLogic.BATCH);
            assertTrue(scenario.machine.checkRecipe(null));
            NBTTagCompound saved = scenario.save();
            scenario.machine.loadNBTData(saved);
            assertEquals(ProcessingLogic.LOCK_RECIPE | ProcessingLogic.BATCH, scenario.machine.getProcessingModes());
            scenario.finish();
            scenario.map.clear();
            scenario.recipe(24, 2, false);
            ItemStack input = scenario.inputs(2, false);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.RECIPE_LOCK_INVALID, scenario.machine.getProcessingResult());
            assertEquals(2, input.stackSize);
            scenario.machine.setProcessingModes(0);
            assertTrue(scenario.machine.checkRecipe(null));
        }
    }

    private static void cacheReload() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, 1, false);
            ItemStack input = scenario.inputs(3, false);
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.finish();
            scenario.map.clear();
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(2, input.stackSize);
            scenario.recipe(24, 7, false);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(7, scenario.machine.mMaxProgresstime);
        }
    }

    private static void inputGroups() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe recipe = scenario.recipe(24, 1, false);
            recipe.mInputs[0].stackSize = 2;
            ItemStack first = scenario.inputs(1, false);
            ItemStack second = scenario.inputs(1, false);
            scenario.machine.setProcessingModes(ProcessingLogic.SEPARATE_INPUTS);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(1, first.stackSize);
            assertEquals(1, second.stackSize);
            scenario.machine.setProcessingModes(0);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(0, first.stackSize);
            assertEquals(0, second.stackSize);
            scenario.finish();
            scenario.machine.mInputBusses.clear();
            GTMTE_BusHatch_Input combined = new GTMTE_BusHatch_Input("test", 2, new String[0], null, 32000);
            scenario.fixture.attach(combined);
            combined.disableSort = true;
            combined.mInventory[3] = new ItemStack(Items.iron_ingot, 2);
            scenario.machine.mInputBusHatches.add(combined);
            scenario.machine.setProcessingModes(ProcessingLogic.SEPARATE_INPUTS);
            assertTrue(scenario.machine.checkRecipe(null));
        }
    }

    private static void longPower() {
        try (Scenario scenario = new Scenario(14)) {
            scenario.recipe(1073741824, 80, false);
            scenario.inputs(2, false);
            GT_MultiblockRecipeProcessingTest.supplyHatches(scenario.fixture, 14, 16, false, false);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(-17179869184L, scenario.machine.lEUt);
            scenario.machine.loadNBTData(scenario.save());
            assertEquals(-17179869184L, scenario.machine.lEUt);
            GT_MetaTileEntity_Hatch_EnergyMulti first = scenario.machine.mEnergyHatchesMulti.get(0);
            first.setEUVar(8589934592L);
            GT_MetaTileEntity_Hatch_EnergyMulti second = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 14, 16, "test",
                    null);
            scenario.fixture.attach(second);
            scenario.machine.mEnergyHatchesMulti.add(second);
            second.setEUVar(8589934591L);
            assertFalse(scenario.machine.drainEnergyInput(17179869184L));
            assertEquals(8589934592L, first.getEUVar());
            second.setEUVar(8589934592L);
            assertTrue(scenario.machine.onRunningTick(null));
            assertEquals(0, first.getEUVar());
            assertEquals(0, second.getEUVar());
        }
    }

    private static void failedPreparation() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, Integer.MAX_VALUE, false);
            ItemStack input = scenario.inputs(4, false);
            ProcessingLogic logic = scenario.machine.logic();
            logic.setCalculatorSupplier(() -> new OverclockCalculator().setDurationModifier(2));
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.DURATION_OVERFLOW, scenario.machine.getProcessingResult());
            assertEquals(4, input.stackSize);
            logic.setCalculatorSupplier(() -> new OverclockCalculator().setEUtModifier(Double.MAX_VALUE));
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.POWER_OVERFLOW, scenario.machine.getProcessingResult());
            assertEquals(4, input.stackSize);
            logic.setCalculatorSupplier(() -> new OverclockCalculator().setHeat(1800, 1799, true, true));
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.INSUFFICIENT_HEAT, scenario.machine.getProcessingResult());
            assertEquals(4, input.stackSize);
            logic.setRecipeValidator(recipe -> CheckRecipeResult.MACHINE_CONDITION);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.MACHINE_CONDITION, scenario.machine.getProcessingResult());
            assertEquals(4, input.stackSize);
        }
    }

    private static void inputTransaction() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe recipe = scenario.recipe(24, 1, true);
            ItemStack item = new ItemStack(Items.iron_ingot, 1);
            FluidStack water = new FluidStack(fluid("shared_water"), 100);
            ProcessingInputs snapshot = new ProcessingInputs(Arrays.asList(item, item), Arrays.asList(water, water));
            assertFalse(snapshot.copy().consume(recipe, 2));
            ProcessingInputs consumed = snapshot.copy();
            assertTrue(consumed.consume(recipe, 1));
            water.amount = 99;
            assertFalse(consumed.commit());
            assertEquals(1, item.stackSize);
            assertEquals(99, water.amount);

            ItemStack original = scenario.inputs(4, true);
            ItemStack replacement = original.copy();
            scenario.machine.logic().setRecipeValidator(candidate -> {
                scenario.machine.mInputBusses.get(0).mInventory[0] = replacement;
                return CheckRecipeResult.SUCCESS;
            });
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.INPUT_CHANGED, scenario.machine.getProcessingResult());
            assertEquals(4, original.stackSize);
            assertEquals(4, replacement.stackSize);
            scenario.machine.logic().setRecipeValidator(candidate -> CheckRecipeResult.SUCCESS);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(4, original.stackSize);
            assertEquals(3, replacement.stackSize);
        }
    }

    private static void legacySave() {
        try (Scenario scenario = new Scenario(1)) {
            GT_MetaTileEntity_MultiBlockBase legacyMachine = new GT_LegacyProcessingTestMachine(
                    "legacy");
            new Fixture(legacyMachine, 1);
            legacyMachine.mEUt = -24;
            legacyMachine.mEfficiency = 10000;
            legacyMachine.mMaxProgresstime = 80;
            legacyMachine.mProgresstime = 17;
            legacyMachine.mOutputItems = new ItemStack[] { new ItemStack(Items.gold_ingot, 3) };
            legacyMachine.mOutputFluids = new FluidStack[] { new FluidStack(fluid("shared_product"), 75) };
            NBTTagCompound legacy = new NBTTagCompound();
            legacyMachine.saveNBTData(legacy);
            scenario.machine.loadNBTData(legacy);
            assertEquals(-24, scenario.machine.lEUt);
            assertEquals(80, scenario.machine.mMaxProgresstime);
            assertEquals(17, scenario.machine.mProgresstime);
            assertEquals(0, scenario.machine.getProcessingModes());
            assertFalse(scenario.machine.checkRecipe(null));
            GT_MetaTileEntity_Hatch_OutputBus items = scenario.itemOutput();
            GT_MetaTileEntity_Hatch_Output fluids = scenario.fluidOutput();
            scenario.machine.mMachine = true;
            scenario.machine.mStartUpCheck = -1;
            scenario.machine.mUpdate = 100;
            for (int tick = 1; tick <= 63; tick++) {
                scenario.machine.onPostTick(scenario.fixture.tile, tick);
            }
            assertEquals(3, items.mInventory[0].stackSize);
            assertEquals(75, fluids.mFluid.amount);
            assertEquals(0, scenario.machine.mMaxProgresstime);
            assertEquals(1000000 - 63 * 24, scenario.machine.mEnergyHatches.get(0).getEUVar());
        }
    }

    private static void controlSlots() {
        try (Scenario scenario = new Scenario(1)) {
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                    scenario.fixture.world);
            GT_Container_ProcessingMachine container = new GT_Container_ProcessingMachine(player.inventory,
                    scenario.fixture.tile) {
                @Override
                public boolean canInteractWith(EntityPlayer entity) {
                    return true;
                }
            };
            player.openContainer = container;
            assertEquals(42, container.inventorySlots.size());
            assertEquals(0, container.getSlotCount());
            assertEquals(0, container.getShiftClickSlotCount());
            ItemStack controller = new ItemStack(Items.stick);
            scenario.machine.mInventory[1] = controller;
            int[] expectedModes = { 6, 22, 23, 31 };
            for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
                Slot slot = container.getSlot(slotIndex);
                assertTrue(slot instanceof GT_Slot_Holo);
                assertFalse(slot.isItemValid(new ItemStack(Items.diamond)));
                assertFalse(slot.canTakeStack(player));
                slot.putStack(null); // Empty slot sync must not clear the controller's inventory.
                assertSame(controller, scenario.machine.mInventory[1]);
                container.slotClick(slotIndex, 0, 0, player);
                assertEquals(expectedModes[slotIndex], scenario.machine.getProcessingModes());
                assertEquals(new int[] { 223, 241, 29, 11 }[slotIndex], slot.xDisplayPosition);
                assertEquals(slotIndex == 2 || slotIndex == 3 ? 174 : 156, slot.yDisplayPosition);
            }
            assertEquals(54, container.getSlot(6).xDisplayPosition);
            assertEquals(156, container.getSlot(6).yDisplayPosition);
            assertEquals(214, container.getSlot(33).yDisplayPosition);
            assertEquals(11, container.getSlot(4).xDisplayPosition);
            assertEquals(156, container.getSlot(4).yDisplayPosition);
            assertTrue(container.getSlot(4) instanceof GT_Slot_Holo);
            int[] voidModes = { 29, 27, 25, 31 };
            for (int mode : voidModes) {
                container.slotClick(0, 0, 0, player);
                assertEquals(mode, scenario.machine.getProcessingModes());
            }
            container.slotClick(0, 1, 0, player);
            assertEquals(25, scenario.machine.getProcessingModes());
            container.slotClick(0, 0, 0, player);
            container.slotClick(0, 2, 0, player);
            container.slotClick(0, 0, 5, player);
            assertEquals(31, scenario.machine.getProcessingModes());
            scenario.machine.lEUt = -17179869184L;
            ICrafting listener = (ICrafting) Proxy.newProxyInstance(ICrafting.class.getClassLoader(),
                    new Class<?>[] { ICrafting.class }, (proxy, method, arguments) -> {
                        if (method.getName().equals("sendProgressBarUpdate")) {
                            container.updateProgressBar((Integer) arguments[1],
                                    (short) ((Integer) arguments[2]).intValue());
                        }
                        return null;
                    });
            container.addCraftingToCrafters(listener);
            assertEquals(-17179869184L, container.eut);
            assertEquals(31, container.modes);
            scenario.fixture.tile.enableWorking();
            container.detectAndSendChanges();
            assertTrue(container.allowedToWork);
            scenario.machine.mProgresstime = 6;
            scenario.machine.mMaxProgresstime = 8;
            container.slotClick(4, 0, 0, player);
            assertFalse(scenario.fixture.tile.isAllowedToWork());
            assertFalse(container.allowedToWork);
            assertEquals(6, scenario.machine.mProgresstime);
            assertEquals(8, scenario.machine.mMaxProgresstime);
            container.slotClick(4, 2, 0, player);
            container.slotClick(4, 0, 5, player);
            assertFalse(scenario.fixture.tile.isAllowedToWork());
            container.slotClick(4, 1, 0, player);
            assertTrue(scenario.fixture.tile.isAllowedToWork());
            assertTrue(container.allowedToWork);
            assertEquals(31, scenario.machine.getProcessingModes());
            player.openContainer = player.inventoryContainer;
            container.slotClick(4, 0, 0, player);
            assertTrue(scenario.fixture.tile.isAllowedToWork());
            container.slotClick(0, 0, 0, player);
            assertEquals(31, scenario.machine.getProcessingModes());
        }
    }

    @Test
    public void runningOutputsSyncCountsTagsAndRejectStaleWindows() throws Throwable {
        run("outputDisplaySync");
    }

    @Test
    public void openingGuiWaitsForInitialStateAndOutputs() throws Throwable {
        run("initialGuiSync");
    }

    @Test
    public void processingButtonsSurviveWorldReload() throws Throwable {
        run("processingButtonsReload");
    }

    @Test
    public void processingButtonsMarkAnAlreadySavedChunkForSaving() throws Throwable {
        run("processingButtonsDirtyChunk");
    }

    private static void processingButtonsDirtyChunk() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.useMaps(scenario.map, GT_Recipe_Map.sCompressorRecipes);
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                    scenario.fixture.world);
            GT_Container_ProcessingMachine container = new GT_Container_ProcessingMachine(player.inventory,
                    scenario.fixture.tile) {
                @Override
                public boolean canInteractWith(EntityPlayer entity) {
                    return true;
                }
            };
            player.openContainer = container;
            net.minecraft.world.chunk.Chunk chunk = scenario.fixture.world.getChunkFromBlockCoords(0, 0);
            for (int slot = 0; slot <= GT_Container_ProcessingMachine.RECIPE_MAP_SLOT; slot++) {
                for (int click = 0; click < 2; click++) {
                    chunk.isModified = false;
                    assertFalse(chunk.needsSaving(false));
                    container.slotClick(slot, 0, 0, player);
                    assertEquals("Only settings controls must mark the chunk for saving",
                            slot != GT_Container_ProcessingMachine.NEI_SLOT, chunk.needsSaving(false));
                }
            }
        }
    }

    private static void processingButtonsReload() throws Exception {
        int machineId = 1002;
        gregtech.api.interfaces.metatileentity.IMetaTileEntity previous = GregTech_API.METATILEENTITIES[machineId];
        GregTech_API.METATILEENTITIES[machineId] = new GT_MetaTileEntity_VacuumFreezer("reload test");
        try {
            for (boolean enabled : new boolean[] { false, true }) {
                for (int modes = 0; modes <= ProcessingLogic.ALL_MODES; modes++) {
                    GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
                    gregtech.api.metatileentity.BaseMetaTileEntity tile = new gregtech.api.metatileentity.BaseMetaTileEntity();
                    tile.setWorldObj(world);
                    tile.setInitialValuesAsNBT(null, (short) machineId);
                    GT_MetaTileEntity_VacuumFreezer machine = (GT_MetaTileEntity_VacuumFreezer) tile
                            .getMetaTileEntity();
                    machine.setProcessingModes(modes);
                    if (enabled) {
                        tile.enableWorking();
                    } else {
                        tile.disableWorking();
                    }
                    NBTTagCompound saved = new NBTTagCompound();
                    tile.writeToNBT(saved);
                    assertEquals(modes, saved.getCompoundTag("processing").getInteger("modes"));
                    byte[] bytes = net.minecraft.nbt.CompressedStreamTools.compress(saved);
                    NBTTagCompound loaded = net.minecraft.nbt.CompressedStreamTools.func_152457_a(bytes,
                            net.minecraft.nbt.NBTSizeTracker.field_152451_a);
                    gregtech.api.metatileentity.BaseMetaTileEntity restoredTile = new gregtech.api.metatileentity.BaseMetaTileEntity();
                    restoredTile.setWorldObj(world);
                    restoredTile.readFromNBT(loaded);
                    GT_MetaTileEntity_VacuumFreezer restored = (GT_MetaTileEntity_VacuumFreezer) restoredTile
                            .getMetaTileEntity();
                    assertEquals(modes, restored.getProcessingModes());
                    assertEquals(enabled, restoredTile.isAllowedToWork());

                    GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
                    GT_Container_ProcessingMachine server = new GT_Container_ProcessingMachine(player.inventory,
                            restoredTile);
                    Fixture clientFixture = new Fixture(new TestFreezer(), 1);
                    clientFixture.world.isRemote = true;
                    GT_StructureIntegrationTest.TestPlayer clientPlayer = new GT_StructureIntegrationTest.TestPlayer(
                            clientFixture.world);
                    GT_Container_ProcessingMachine client = new GT_Container_ProcessingMachine(clientPlayer.inventory,
                            clientFixture.tile);
                    ICrafting listener = (ICrafting) Proxy.newProxyInstance(ICrafting.class.getClassLoader(),
                            new Class<?>[] { ICrafting.class }, (proxy, method, arguments) -> {
                                if (method.getName().equals("sendProgressBarUpdate")) {
                                    client.updateProgressBar((Integer) arguments[1],
                                            (short) ((Integer) arguments[2]).intValue());
                                }
                                return null;
                            });
                    server.addCraftingToCrafters(listener);
                    client.receiveOutputs(new NBTTagCompound());
                    assertTrue(client.isDisplayReady());
                    assertEquals(modes, client.modes);
                    assertEquals(enabled, client.allowedToWork);
                }
            }
        } finally {
            GregTech_API.METATILEENTITIES[machineId] = previous;
        }
    }

    private static void initialGuiSync() {
        for (boolean outputsFirst : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(1)) {
                scenario.recipe(24, 20, false);
                scenario.inputs(4, false);
                assertTrue(scenario.machine.checkRecipe(null));
                scenario.machine.mProgresstime = 7;
                scenario.fixture.tile.enableWorking();
                scenario.fixture.tile.setActive(true);
                scenario.fixture.tile.setErrorDisplayID(2);
                scenario.machine.setProcessingModes(ProcessingLogic.BATCH);
                GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                        scenario.fixture.world);
                GT_Container_ProcessingMachine server = new GT_Container_ProcessingMachine(player.inventory,
                        scenario.fixture.tile);
                server.detectAndSendChanges(); // No state changes after the window opens.

                Fixture clientFixture = new Fixture(new TestFreezer(), 1);
                clientFixture.world.isRemote = true;
                GT_StructureIntegrationTest.TestPlayer clientPlayer = new GT_StructureIntegrationTest.TestPlayer(
                        clientFixture.world);
                GT_Container_ProcessingMachine client = new GT_Container_ProcessingMachine(clientPlayer.inventory,
                        clientFixture.tile);
                assertFalse(client.isDisplayReady());
                NBTTagCompound outputs = new NBTTagCompound();
                ProcessingPersistence.writeItems(outputs, "items", scenario.machine.mOutputItems);
                if (outputsFirst) {
                    client.receiveOutputs(outputs);
                    assertFalse(client.isDisplayReady());
                }

                List<int[]> updates = new ArrayList<>();
                ICrafting listener = (ICrafting) Proxy.newProxyInstance(ICrafting.class.getClassLoader(),
                        new Class<?>[] { ICrafting.class }, (proxy, method, arguments) -> {
                            if (method.getName().equals("sendProgressBarUpdate")) {
                                updates.add(new int[] { (Integer) arguments[1], (Integer) arguments[2] });
                            }
                            return null;
                        });
                server.addCraftingToCrafters(listener);
                for (int[] update : updates) {
                    if (update[0] == 117 && !client.isDisplayReady()) {
                        assertEquals(0, client.modes);
                        assertFalse(client.allowedToWork);
                    }
                    client.updateProgressBar(update[0], (short) update[1]);
                }
                assertEquals(outputsFirst, client.isDisplayReady());
                if (!outputsFirst) {
                    client.receiveOutputs(outputs);
                }
                assertTrue(client.isDisplayReady());
                assertEquals(1, client.mActive);
                assertEquals(2, client.mDisplayErrorCode);
                assertEquals(7, client.mProgressTime);
                assertEquals(20, client.mMaxProgressTime);
                assertEquals(ProcessingLogic.BATCH, client.modes);
                assertTrue(client.allowedToWork);
                assertEquals(CheckRecipeResult.SUCCESS, client.result);
                assertEquals(1, client.outputItems.length);
                assertSame(Items.gold_ingot, client.outputItems[0].getItem());
            }
        }
    }

    private static void outputDisplaySync() {
        try (Scenario scenario = new Scenario(1)) {
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                    scenario.fixture.world);
            GT_Container_ProcessingMachine server = new GT_Container_ProcessingMachine(player.inventory,
                    scenario.fixture.tile);
            server.windowId = 7;
            ItemStack item = new ItemStack(Items.iron_ingot, 4096);
            item.setStackDisplayName("Current cycle output");
            FluidStack outputFluid = new FluidStack(fluid("processing_display"), 1000000);
            outputFluid.tag = new NBTTagCompound();
            outputFluid.tag.setString("cycle", "original");
            NBTTagCompound outputs = new NBTTagCompound();
            ProcessingPersistence.writeItems(outputs, "items", new ItemStack[] { item });
            ProcessingPersistence.writeFluids(outputs, "fluids", new FluidStack[] { outputFluid });
            GT_Packet_ProcessingOutputs packet = new GT_Packet_ProcessingOutputs(server, outputs);
            outputs.getCompoundTag("items0").setInteger("Count", 1);
            GT_Packet_ProcessingOutputs decoded = (GT_Packet_ProcessingOutputs) new GT_Packet_ProcessingOutputs()
                    .decode(ByteStreams.newDataInput(packet.encode()));

            Fixture clientFixture = new Fixture(new TestFreezer(), 1);
            clientFixture.world.isRemote = true;
            GT_StructureIntegrationTest.TestPlayer clientPlayer = new GT_StructureIntegrationTest.TestPlayer(
                    clientFixture.world);
            GT_Container_ProcessingMachine client = new GT_Container_ProcessingMachine(clientPlayer.inventory,
                    clientFixture.tile);
            client.windowId = 8;
            decoded.applyTo(client);
            assertEquals(0, client.outputItems.length);
            client.windowId = 7;
            clientFixture.tile.xCoord++;
            decoded.applyTo(client);
            assertEquals(0, client.outputItems.length);
            clientFixture.tile.xCoord--;
            clientFixture.world.provider.dimensionId++;
            decoded.applyTo(client);
            assertEquals(0, client.outputItems.length);
            clientFixture.world.provider.dimensionId--;
            decoded.applyTo(client);
            assertEquals(4096, client.outputItems[0].stackSize);
            assertEquals("Current cycle output", client.outputItems[0].getDisplayName());
            assertEquals(1000000, client.outputFluids[0].amount);
            assertEquals("original", client.outputFluids[0].tag.getString("cycle"));
            decoded.applyTo(server);
            decoded.process(scenario.fixture.world);
            assertEquals(0, server.outputItems.length);

            GT_Packet_ProcessingOutputs empty = new GT_Packet_ProcessingOutputs(server, new NBTTagCompound());
            ((GT_Packet_ProcessingOutputs) new GT_Packet_ProcessingOutputs()
                    .decode(ByteStreams.newDataInput(empty.encode()))).applyTo(client);
            assertEquals(0, client.outputItems.length);
            assertEquals(0, client.outputFluids.length);
        }
    }

    @Test
    public void optionalControllerSlotIsOnTheRightAndShiftClickSkipsControls() throws Throwable {
        run("optionalControllerSlot");
    }

    @Test
    public void chemicalReactorControllerSlotSuppliesNonConsumedRecipeSelector() throws Throwable {
        run("chemicalReactorController");
    }

    private static void chemicalReactorController() throws Exception {
        for (boolean separated : new boolean[] { false, true }) {
            GT_MultiblockRecipeProcessingTest.RecipeFixture f =
                    new GT_MultiblockRecipeProcessingTest.RecipeFixture("LargeChemicalReactor", 1);
            try {
                f.register(24, 80);
                f.recipe.mInputs[1].stackSize = 0;
                f.fill();
                f.machine.mInputBusses.clear();
                f.fixture.items(f.iron);
                GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine =
                        (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) f.machine;
                machine.setProcessingModes(separated ? ProcessingLogic.SEPARATE_INPUTS : 0);
                GT_StructureIntegrationTest.TestPlayer player =
                        new GT_StructureIntegrationTest.TestPlayer(f.fixture.world);
                GT_Container_ProcessingMachine container = (GT_Container_ProcessingMachine)
                        machine.getServerGUI(0, player.inventory, f.fixture.tile);
                assertEquals(1, container.getSlotCount());
                Slot controller = container.getSlot(container.getSlotStartIndex());
                assertFalse(controller instanceof GT_Slot_Holo);
                assertFalse(machine.checkRecipe(null));
                f.assertUnconsumed();
                controller.putStack(f.stick);
                assertSame(f.stick, machine.mInventory[1]);
                assertTrue(machine.checkRecipe(null));
                assertEquals(3, f.stick.stackSize);
                assertEquals(4, f.iron.stackSize);
                assertEquals(200, f.water.amount);
            } finally {
                f.map.clear();
                GregTech_API.sPostloadFinished = f.previousPostload;
            }
        }
    }

    private static void optionalControllerSlot() {
        for (boolean multipleMaps : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(1)) {
                scenario.machine.controllerSlot = true;
                if (multipleMaps) {
                    scenario.useMaps(scenario.map, GT_Recipe_Map.sCompressorRecipes);
                }
                GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                        scenario.fixture.world);
                GT_Container_ProcessingMachine container = new GT_Container_ProcessingMachine(player.inventory,
                        scenario.fixture.tile);
                int controllerIndex = multipleMaps ? 7 : 6;
                assertEquals(controllerIndex + 37, container.inventorySlots.size());
                assertEquals(1, container.getSlotCount());
                assertEquals(1, container.getShiftClickSlotCount());
                assertEquals(controllerIndex, container.getSlotStartIndex());
                assertEquals(controllerIndex, container.getShiftClickStartIndex());
                Slot controller = container.getSlot(controllerIndex);
                assertFalse(controller instanceof GT_Slot_Holo);
                assertEquals(231, controller.xDisplayPosition);
                assertEquals(210, controller.yDisplayPosition);
                scenario.machine.mInventory[1] = new ItemStack(Items.stick, 5);
                assertEquals(5, container.transferStackInSlot(player, controllerIndex).stackSize);
                assertNull(scenario.machine.mInventory[1]);
                int transferred = 0;
                for (ItemStack stack : player.inventory.mainInventory) {
                    if (stack != null && stack.getItem() == Items.stick) {
                        transferred += stack.stackSize;
                    }
                }
                assertEquals(5, transferred);
                assertEquals(0, scenario.machine.getProcessingModes());
            }
        }
    }

    private static void meOutputs() {
        try (Scenario scenario = new Scenario(1)) {
            TestMEOutput output = new TestMEOutput();
            scenario.fixture.attach(output);
            scenario.machine.mOutputBusses.add(output);
            ProcessingOutputs adapter = new ProcessingOutputs(scenario.machine);
            ItemStack[] outputs = { new ItemStack(Items.gold_ingot, 2), new ItemStack(Items.diamond, 2) };
            assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, adapter.canFit(outputs, new FluidStack[0], true, false));
            assertEquals(0, output.stored);
            assertFalse(adapter.deliver(outputs, null, true, false));
            assertEquals(3, output.stored);
            assertEquals(1, outputs[1].stackSize);
            output.stored = 0;
            assertTrue(adapter.deliver(outputs, null, true, false));
            assertEquals(1, output.stored);
        }
    }

    private static void nominalAmperage() {
        try (Scenario scenario = new Scenario(1)) {
            for (int amps : new int[] { 1, 2, 4, 16, 64, 1024 }) {
                scenario.machine.mEnergyHatches.get(0).mAmpers = amps;
                assertEquals(1, scenario.machine.getProcessingAmperage());
                GT_MetaTileEntity_Hatch_EnergyMulti multi = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, amps,
                        "test", null);
                scenario.fixture.attach(multi);
                scenario.machine.mEnergyHatchesMulti.add(multi);
                assertEquals(amps * 2L, scenario.machine.getProcessingAmperage());
                scenario.machine.mEnergyHatchesMulti.clear();
            }
        }
    }

    private static void largeOutputSave() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, 1, false);
            scenario.inputs(200, false);
            scenario.machine.setProcessingModes(ProcessingLogic.BATCH);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(128, scenario.machine.mOutputItems[0].stackSize);
            scenario.machine.loadNBTData(scenario.save());
            assertEquals(128, scenario.machine.mOutputItems[0].stackSize);
            assertEquals(128, scenario.machine.getCurrentOperations());
            scenario.machine.limit = 16;
            scenario.finish();
            GT_MultiblockRecipeProcessingTest.supplyHatches(scenario.fixture, 1, 16, false, false);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(16, scenario.machine.getCurrentParallel());
        }
    }

    private static void outputChances() {
        for (int chance : new int[] { 1, 1359, 1360, 1361, 10000 }) {
            try (Scenario scenario = new Scenario(1)) {
                GT_Recipe recipe = scenario.recipe(24, 1, false);
                recipe.mChances[0] = chance;
                scenario.inputs(1, false);
                scenario.fixture.world.rand.setSeed(0);
                assertTrue(scenario.machine.checkRecipe(null));
                assertEquals(chance > 1360, scenario.machine.mOutputItems[0] != null);
            }
        }
        try (Scenario scenario = new Scenario(1)) {
            scenario.map.addRecipe(false, new ItemStack[] { new ItemStack(Items.iron_ingot) },
                    new ItemStack[] { new ItemStack(Items.gold_ingot, 4), new ItemStack(Items.diamond, 2) },
                    null, new int[] { 10000, 10000 }, null, null, 1, 24, 0);
            scenario.inputs(1, false);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(2, scenario.machine.mOutputItems.length);
            assertEquals(4, scenario.machine.mOutputItems[0].stackSize);
            assertSame(Items.gold_ingot, scenario.machine.mOutputItems[0].getItem());
            assertEquals(2, scenario.machine.mOutputItems[1].stackSize);
            assertSame(Items.diamond, scenario.machine.mOutputItems[1].getItem());
        }
    }

    private static void fluidAdapters() {
        try (Scenario scenario = new Scenario(1)) {
            GT_MetaTileEntity_Hatch_Output hatch = scenario.fluidOutput();
            hatch.mMode = 8;
            hatch.setLockedFluidName(fluid("shared_product").getName());
            ProcessingOutputs outputs = new ProcessingOutputs(scenario.machine);
            assertEquals(CheckRecipeResult.SUCCESS, outputs.canFit(new ItemStack[0],
                    new FluidStack[] { new FluidStack(fluid("shared_product"), 100) }, false, true));
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, outputs.canFit(new ItemStack[0],
                    new FluidStack[] { new FluidStack(fluid("shared_other"), 100) }, false, true));
            scenario.machine.mOutputHatches.clear();
            TestMEFluidOutput me = new TestMEFluidOutput();
            scenario.fixture.attach(me);
            scenario.machine.mOutputHatches.add(me);
            FluidStack[] fluids = { new FluidStack(fluid("shared_product"), 200),
                    new FluidStack(fluid("shared_other"), 200) };
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, outputs.canFit(new ItemStack[0], fluids, false, true));
            assertFalse(outputs.deliver(null, fluids, false, true));
            assertEquals(300, me.stored);
            assertEquals(100, fluids[1].amount);
            me.stored = 0;
            assertTrue(outputs.deliver(null, fluids, false, true));
            assertEquals(100, me.stored);
        }
    }

    private static void mapChanges() {
        try (Scenario scenario = new Scenario(1)) {
            scenario.recipe(24, 1, false);
            ItemStack input = scenario.inputs(3, false);
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.finish();
            scenario.machine.alternateMap = GT_Recipe.GT_Recipe_Map.sCompressorRecipes;
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(2, input.stackSize);
            scenario.machine.alternateMap = null;
            assertTrue(scenario.machine.checkRecipe(null));
        }
    }

    private static void overflowBoundary() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe recipe = scenario.recipe(24, 1, false);
            ItemStack input = scenario.inputs(4, false);
            ProcessingLogic logic = scenario.machine.logic();
            ProcessingLogic.Request excessivePower = new ProcessingLogic.Request(
                    Collections
                            .singletonList(new ProcessingInputs(Collections.singleton(input), Collections.emptyList())),
                    Long.MAX_VALUE, 2, 0, new ProcessingOutputs(scenario.machine), bound -> 0);
            assertEquals(CheckRecipeResult.POWER_OVERFLOW, logic.process(excessivePower).status);
            assertEquals(4, input.stackSize);
            recipe.mOutputs[0].stackSize = -1;
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.OUTPUT_OVERFLOW, scenario.machine.getProcessingResult());
            assertEquals(4, input.stackSize);
            recipe.mOutputs[0].stackSize = Integer.MAX_VALUE;
            scenario.machine.limit = 2;
            GT_MultiblockRecipeProcessingTest.supplyHatches(scenario.fixture, 1, 4, false, false);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.OUTPUT_OVERFLOW, scenario.machine.getProcessingResult());
            assertEquals(4, input.stackSize);
            recipe.mOutputs[0].stackSize = 1;
            scenario.machine.limit = 1;
            ProcessingLogic.Request maintenanceOverflow = new ProcessingLogic.Request(
                    Collections
                            .singletonList(new ProcessingInputs(Collections.singleton(input), Collections.emptyList())),
                    Long.MAX_VALUE, 1, 0, new ProcessingOutputs(scenario.machine), bound -> 0, 1000);
            assertEquals(CheckRecipeResult.POWER_OVERFLOW, logic.process(maintenanceOverflow).status);
            assertEquals(4, input.stackSize);
        }
    }

    private static void selectedMapOnly() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            assertFalse(scenario.machine.setActiveRecipeMap(other));
            scenario.useMaps(scenario.map, other);
            scenario.recipe(24, 80, false);
            scenario.recipe(other, Items.stick, Items.diamond, 20);
            ItemStack stick = new ItemStack(Items.stick, 2);
            scenario.fixture.items(stick);
            assertSame(scenario.map, scenario.machine.getRecipeMap());
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(2, stick.stackSize);
            assertTrue(scenario.machine.setActiveRecipeMap(other));
            assertTrue(scenario.machine.checkRecipe(null));
            assertSame(Items.diamond, scenario.machine.mOutputItems[0].getItem());
            assertEquals(20, scenario.machine.mMaxProgresstime);
            assertEquals(1, stick.stackSize);
            scenario.finish();
            assertTrue(scenario.machine.cycleRecipeMap(false));
            assertSame(scenario.map, scenario.machine.getRecipeMap());
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(1, stick.stackSize);
            assertFalse(scenario.machine.setActiveRecipeMap(null));
            assertFalse(scenario.machine.setActiveRecipeMap(GT_Recipe_Map.sElectrolyzerRecipes));
        }
    }

    private static void mapSwitchResetsRecipeState() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            scenario.recipe(24, 80, false);
            scenario.recipe(other, Items.iron_ingot, Items.diamond, 20);
            ItemStack input = scenario.inputs(4, false);
            scenario.machine.setProcessingModes(ProcessingLogic.LOCK_RECIPE);
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.finish();
            assertEquals(scenario.map.mUnlocalizedName, scenario.machine.logic().saveRecipeLock().getString("map"));
            assertTrue(scenario.machine.setActiveRecipeMap(other));
            assertNull(scenario.machine.cashedRecipe);
            assertFalse(scenario.machine.logic().saveRecipeLock().hasKey("map"));
            assertEquals(ProcessingLogic.LOCK_RECIPE, scenario.machine.getProcessingModes());
            assertTrue(scenario.machine.checkRecipe(null));
            assertSame(Items.diamond, scenario.machine.mOutputItems[0].getItem());
            assertEquals(other.mUnlocalizedName, scenario.machine.logic().saveRecipeLock().getString("map"));
            scenario.finish();
            assertFalse(scenario.machine.setActiveRecipeMap(other));
            assertTrue(scenario.machine.logic().saveRecipeLock().hasKey("map"));
            assertTrue(scenario.machine.setActiveRecipeMap(scenario.map));
            assertTrue(scenario.machine.checkRecipe(null));
            assertSame(Items.gold_ingot, scenario.machine.mOutputItems[0].getItem());
            assertEquals(80, scenario.machine.mMaxProgresstime);
            assertEquals(1, input.stackSize);
        }
    }

    private static void mapSwitchDuringCycle() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            scenario.recipe(24, 80, false);
            scenario.recipe(other, Items.iron_ingot, Items.diamond, 20);
            ItemStack input = scenario.inputs(3, false);
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.machine.mProgresstime = 17;
            assertTrue(scenario.machine.setActiveRecipeMap(other));
            assertEquals(80, scenario.machine.mMaxProgresstime);
            assertEquals(17, scenario.machine.mProgresstime);
            assertEquals(-24, scenario.machine.lEUt);
            assertSame(Items.gold_ingot, scenario.machine.mOutputItems[0].getItem());
            NBTTagCompound saved = scenario.save();
            TestFreezer restored = new TestFreezer();
            // Stable map names, rather than list positions, determine the restored
            // selection.
            restored.supportedMaps = Arrays.asList(other, scenario.map);
            Fixture fixture = new Fixture(restored, 1);
            fixture.tile.disableWorking();
            fixture.items(input);
            restored.loadNBTData(saved);
            assertSame(other, restored.getRecipeMap());
            assertEquals(0, restored.getActiveRecipeMapIndex());
            assertEquals(17, restored.mProgresstime);
            assertEquals(80, restored.mMaxProgresstime);
            assertEquals(2, input.stackSize);
            GT_MetaTileEntity_Hatch_OutputBus output = scenario.itemOutput();
            restored.mOutputBusses.add(output);
            restored.completeRecipe();
            assertSame(Items.gold_ingot, output.mInventory[0].getItem());
            assertEquals(1, output.mInventory[0].stackSize);
            assertTrue(restored.checkRecipe(null));
            assertSame(Items.diamond, restored.mOutputItems[0].getItem());
            assertEquals(20, restored.mMaxProgresstime);
            assertEquals(1, input.stackSize);
        }
    }

    private static void missingSavedMap() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            scenario.recipe(24, 80, false);
            ItemStack input = scenario.inputs(2, false);
            assertTrue(scenario.machine.setActiveRecipeMap(other));
            NBTTagCompound saved = scenario.save();
            scenario.machine.supportedMaps = Collections.singletonList(scenario.map);
            scenario.machine.loadNBTData(saved);
            assertNull(scenario.machine.getRecipeMap());
            assertEquals(-1, scenario.machine.getActiveRecipeMapIndex());
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.RECIPE_MAP_UNAVAILABLE, scenario.machine.getProcessingResult());
            assertEquals(2, input.stackSize);
            assertEquals(other.mUnlocalizedName, scenario.save().getCompoundTag("processing").getString("recipeMap"));
            assertTrue(scenario.machine.setActiveRecipeMap(scenario.map));
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.finish();
            scenario.machine.supportedMaps = Collections.emptyList();
            assertFalse(scenario.machine.cycleRecipeMap(false));
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(1, input.stackSize);
        }
    }

    private static void mapHatchFilters() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            scenario.inputs(2, true);
            GTMTE_BusHatch_Input combined = new GTMTE_BusHatch_Input("test", 2, new String[0], null, 32000);
            scenario.fixture.attach(combined);
            scenario.machine.mInputBusHatches.add(combined);
            GTMTE_Multi_Hatch_Input multi = new GTMTE_Multi_Hatch_Input("test", 4, 2, new String[0], null, 32000);
            scenario.fixture.attach(multi);
            scenario.machine.mQuadrInputHatches.add(multi);
            assertTrue(scenario.machine.setActiveRecipeMap(other));
            assertSame(other, scenario.machine.mInputBusses.get(0).mRecipeMap);
            assertSame(other, scenario.machine.mInputHatches.get(0).mRecipeMap);
            assertSame(other, combined.mRecipeMap);
            assertSame(other, multi.mRecipeMap);
        }
    }

    private static void mapControlSlot() throws Exception {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            String translations = scenario.map.mUnlocalizedName + "=Localized first recipe map\n"
                    + other.mUnlocalizedName + "=Localized second recipe map\n";
            net.minecraft.util.StringTranslate.inject(new java.io.ByteArrayInputStream(
                    translations.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                    scenario.fixture.world);
            GT_Container_ProcessingMachine server = new GT_Container_ProcessingMachine(player.inventory,
                    scenario.fixture.tile) {
                @Override
                public boolean canInteractWith(EntityPlayer entity) {
                    return true;
                }
            };
            player.openContainer = server;
            assertEquals(43, server.inventorySlots.size());
            assertTrue(server.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
            assertSame(scenario.map, server.getNEIRecipeMap());
            assertEquals("Localized first recipe map", server.getNEIRecipeMapName());
            assertEquals(29, server.getSlot(GT_Container_ProcessingMachine.NEI_SLOT).xDisplayPosition);
            assertEquals(156, server.getSlot(GT_Container_ProcessingMachine.NEI_SLOT).yDisplayPosition);
            assertTrue(server.getSlot(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT) instanceof GT_Slot_Holo);
            assertEquals(11, server.getSlot(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT).xDisplayPosition);
            assertEquals(192, server.getSlot(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT).yDisplayPosition);
            server.slotClick(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT, 0, 0, player);
            assertSame(other, scenario.machine.getRecipeMap());
            assertSame(other, server.getNEIRecipeMap());
            assertEquals("Localized second recipe map", server.getNEIRecipeMapName());
            server.slotClick(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT, 1, 0, player);
            assertSame(scenario.map, scenario.machine.getRecipeMap());
            server.slotClick(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT, 1, 1, player);
            assertSame(other, scenario.machine.getRecipeMap());
            assertEquals(0, scenario.machine.getProcessingModes());
            server.slotClick(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT, 0, 5, player);
            server.slotClick(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT, 2, 0, player);
            assertSame(other, scenario.machine.getRecipeMap());
            player.openContainer = player.inventoryContainer;
            server.slotClick(GT_Container_ProcessingMachine.RECIPE_MAP_SLOT, 0, 0, player);
            assertSame(other, scenario.machine.getRecipeMap());
            scenario.machine.syncActiveRecipeMap(0);
            assertSame(other, scenario.machine.getRecipeMap());

            TestFreezer clientMachine = new TestFreezer();
            clientMachine.supportedMaps = scenario.machine.supportedMaps;
            Fixture clientFixture = new Fixture(clientMachine, 1);
            clientFixture.world.isRemote = true;
            GT_StructureIntegrationTest.TestPlayer clientPlayer = new GT_StructureIntegrationTest.TestPlayer(
                    clientFixture.world);
            GT_Container_ProcessingMachine client = new GT_Container_ProcessingMachine(clientPlayer.inventory,
                    clientFixture.tile);
            ICrafting listener = (ICrafting) Proxy.newProxyInstance(ICrafting.class.getClassLoader(),
                    new Class<?>[] { ICrafting.class }, (proxy, method, arguments) -> {
                        if (method.getName().equals("sendProgressBarUpdate")) {
                            client.updateProgressBar((Integer) arguments[1],
                                    (short) ((Integer) arguments[2]).intValue());
                        }
                        return null;
                    });
            server.addCraftingToCrafters(listener);
            assertEquals(1, client.activeRecipeMapIndex);
            assertSame(other, clientMachine.getRecipeMap());
            assertSame(other, client.getNEIRecipeMap());
            assertEquals("Localized second recipe map", client.getNEIRecipeMapName());
            assertFalse(clientMachine.setActiveRecipeMap(scenario.map));
            assertSame(other, clientMachine.getRecipeMap());
            clientPlayer.openContainer = client;
            boolean clientPower = clientFixture.tile.isAllowedToWork();
            client.slotClick(4, 0, 0, clientPlayer);
            assertEquals(clientPower, clientFixture.tile.isAllowedToWork());
            clientMachine.syncActiveRecipeMap(9);
            assertSame(other, clientMachine.getRecipeMap());
            assertTrue(scenario.machine.setActiveRecipeMap(scenario.map));
            server.detectAndSendChanges();
            assertEquals(0, client.activeRecipeMapIndex);
            assertSame(scenario.map, clientMachine.getRecipeMap());
            assertSame(scenario.map, client.getNEIRecipeMap());
            assertEquals("Localized first recipe map", client.getNEIRecipeMapName());
        }
    }

    private static void mapChangeBeforeCommit() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            scenario.recipe(24, 80, false);
            scenario.recipe(other, Items.iron_ingot, Items.diamond, 20);
            ItemStack input = scenario.inputs(2, false);
            scenario.machine.logic().setRecipeValidator(recipe -> {
                scenario.machine.setActiveRecipeMap(other);
                return CheckRecipeResult.SUCCESS;
            });
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.INPUT_CHANGED, scenario.machine.getProcessingResult());
            assertEquals(2, input.stackSize);
            scenario.machine.logic().setRecipeValidator(recipe -> CheckRecipeResult.SUCCESS);
            assertTrue(scenario.machine.checkRecipe(null));
            assertSame(Items.diamond, scenario.machine.mOutputItems[0].getItem());
            assertEquals(1, input.stackSize);
        }
    }

    private static void mapSwitchWithPendingOutputs() {
        try (Scenario scenario = new Scenario(1)) {
            GT_Recipe_Map other = GT_Recipe_Map.sCompressorRecipes;
            scenario.useMaps(scenario.map, other);
            scenario.recipe(24, 1, false);
            scenario.recipe(other, Items.iron_ingot, Items.diamond, 20);
            ItemStack input = scenario.inputs(2, false);
            scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS);
            GT_MetaTileEntity_Hatch_OutputBus output = scenario.itemOutput();
            assertTrue(scenario.machine.checkRecipe(null));
            Arrays.fill(output.mInventory, new ItemStack(Items.stick, 64));
            scenario.finish();
            assertTrue(scenario.machine.isWaitingForOutput());
            assertTrue(scenario.machine.setActiveRecipeMap(other));
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.WAITING_FOR_OUTPUT, scenario.machine.getProcessingResult());
            assertEquals(1, input.stackSize);
            output.mInventory[0] = null;
            scenario.finish();
            assertSame(Items.gold_ingot, output.mInventory[0].getItem());
            assertEquals(1, output.mInventory[0].stackSize);
            output.mInventory[1] = null;
            assertTrue(scenario.machine.checkRecipe(null));
            assertSame(Items.diamond, scenario.machine.mOutputItems[0].getItem());
            assertEquals(0, input.stackSize);
        }
    }

    public static class TestFreezer extends GT_MetaTileEntity_VacuumFreezer {
        int limit = 1;
        int limitReads;
        boolean allowed = true;
        boolean bypass;
        boolean controllerSlot;
        GT_Recipe.GT_Recipe_Map alternateMap;
        List<GT_Recipe_Map> supportedMaps;

        @Override
        public boolean hasControllerSlotInGui() {
            return controllerSlot;
        }

        @Override
        public List<GT_Recipe_Map> getRecipeMaps() {
            return supportedMaps == null ? super.getRecipeMaps() : supportedMaps;
        }

        @Override
        public GT_Recipe.GT_Recipe_Map getRecipeMap() {
            return alternateMap == null ? super.getRecipeMap() : alternateMap;
        }

        TestFreezer() {
            super("shared processing test");
        }

        @Override
        public int getMaxParallelRecipes() {
            limitReads++;
            return limit;
        }

        @Override
        protected CheckRecipeResult validateProcessing() {
            return allowed ? CheckRecipeResult.SUCCESS : CheckRecipeResult.MACHINE_CONDITION;
        }

        @Override
        protected ProcessingLogic createProcessingLogic() {
            return super.createProcessingLogic()
                    .setMaxParallelSupplier(this::getMaxParallelRecipes)
                    .setSubTickParallelBypassesLimit(bypass);
        }

        ProcessingLogic logic() {
            return getProcessingLogic();
        }
    }

    private static final class TestMEOutput extends GT_MetaTileEntity_Hatch_OutputBus_ME {
        int stored;

        TestMEOutput() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(ItemStack stack) {
            return 3 - stored;
        }

        @Override
        public boolean storeAll(ItemStack stack) {
            stored += stack.stackSize;
            stack.stackSize = 0;
            return true;
        }
    }

    private static final class TestMEFluidOutput extends GT_MetaTileEntity_Hatch_Output_ME {
        int stored;

        TestMEFluidOutput() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(FluidStack stack) {
            return 300 - stored;
        }

        @Override
        public int fill(FluidStack stack, boolean doFill) {
            if (doFill) {
                stored += stack.amount;
            }
            return stack.amount;
        }
    }

    private static final class Scenario implements AutoCloseable {
        final TestFreezer machine = new TestFreezer();
        final Fixture fixture;
        final GT_Recipe.GT_Recipe_Map map = machine.getRecipeMap();
        final boolean postload = GregTech_API.sPostloadFinished;
        final List<GT_Recipe_Map> additionalMaps = new ArrayList<>();

        void useMaps(GT_Recipe_Map... maps) {
            for (GT_Recipe_Map recipeMap : maps) {
                if (recipeMap != map && !additionalMaps.contains(recipeMap)) {
                    assertTrue(recipeMap.mRecipeList.isEmpty());
                    additionalMaps.add(recipeMap);
                }
            }
            machine.supportedMaps = Collections.unmodifiableList(Arrays.asList(maps));
        }

        GT_Recipe recipe(GT_Recipe_Map recipeMap, net.minecraft.item.Item input, net.minecraft.item.Item output,
                int duration) {
            GT_Recipe recipe = recipeMap.addRecipe(false, new ItemStack[] { new ItemStack(input) },
                    new ItemStack[] { new ItemStack(output) }, null, new int[] { 10000 }, null, null, duration, 24, 0);
            assertNotNull(recipe);
            return recipe;
        }

        Scenario(int tier) {
            assertTrue(map.mRecipeList.isEmpty());
            fixture = new Fixture(machine, tier);
            fixture.tile.disableWorking();
            GregTech_API.sPostloadFinished = true;
        }

        GT_Recipe recipe(int eut, int duration, boolean fluids) {
            GT_Recipe recipe = map.addRecipe(false, new ItemStack[] { new ItemStack(Items.iron_ingot) },
                    new ItemStack[] { new ItemStack(Items.gold_ingot) }, null, new int[] { 10000 },
                    fluids ? new FluidStack[] { new FluidStack(fluid("shared_water"), 100) } : null,
                    fluids ? new FluidStack[] { new FluidStack(fluid("shared_product"), 75) } : null, duration, eut, 0);
            assertNotNull(recipe);
            return recipe;
        }

        ItemStack inputs(int count, boolean fluids) {
            ItemStack input = new ItemStack(Items.iron_ingot, count);
            fixture.items(input);
            if (fluids) {
                fixture.liquid(new FluidStack(fluid("shared_water"), 100 * count));
            }
            return input;
        }

        GT_MetaTileEntity_Hatch_OutputBus itemOutput() {
            GT_MetaTileEntity_Hatch_OutputBus output = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            fixture.attach(output);
            machine.mOutputBusses.add(output);
            return output;
        }

        GT_MetaTileEntity_Hatch_Output fluidOutput() {
            GT_MetaTileEntity_Hatch_Output output = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);
            fixture.attach(output);
            machine.mOutputHatches.add(output);
            return output;
        }

        void finish() {
            machine.completeRecipe();
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }

        @Override
        public void close() {
            map.clear();
            for (GT_Recipe_Map recipeMap : additionalMaps) {
                recipeMap.clear();
            }
            GregTech_API.sPostloadFinished = postload;
        }
    }
}
