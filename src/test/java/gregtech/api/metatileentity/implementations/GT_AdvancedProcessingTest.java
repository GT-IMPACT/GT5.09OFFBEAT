package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Cleanroom;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import static gregtech.api.metatileentity.implementations.GT_MultiblockRecipeProcessingTest.*;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

public class GT_AdvancedProcessingTest {
    private static final String[] MACHINES = { "LargeChemicalReactor", "ElectricBlastFurnace", "OilCracker",
            "DistillationTower" };
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
            GT_AdvancedProcessingTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    @Test
    public void controlsAndPowerSurviveReloadWithMachineSpecificControllerSlots() throws Throwable {
        run("controls");
    }

    @Test
    public void legacyCyclesRetainTheirSavedDurationAndEnergy() throws Throwable {
        run("legacyCycles");
    }

    @Test
    public void batchAndRecipeLockSurviveReloadOnEveryMachine() throws Throwable {
        run("batchAndLock");
    }

    @Test
    public void cachedChemicalRecipeRechecksCleanroomBeforeConsumption() throws Throwable {
        run("chemicalEnvironment");
    }

    @Test
    public void cachedBlastRecipeRechecksHeatAndOverclockBonuses() throws Throwable {
        run("blastHeat");
    }

    @Test
    public void towerOutputIndicesSurvivePartialDeliveryAndReload() throws Throwable {
        run("towerOutputs");
    }

    @Test
    public void blastGasRecoveryIsAppliedOnceAndUsesTopOutputsOnly() throws Throwable {
        run("blastOutputs");
    }

    @Test
    public void legacyBlastGasIsRecoveredOnceEvenAfterAnotherSave() throws Throwable {
        run("legacyBlastOutputs");
    }

    @Test
    public void crackerReadsCircuitsFromBusesAndPreservesFluidZones() throws Throwable {
        run("crackerInputs");
    }

    @Test
    public void cachedChemicalRecipeRechecksLowGravity() throws Throwable {
        run("chemicalGravity");
    }

    @Test
    public void towerMeOutputsRespectLevelsAndDoNotDuplicateAfterReload() throws Throwable {
        run("towerMeOutputs");
    }

    @Test
    public void gasRecoveryOverflowRejectsBeforeConsumption() throws Throwable {
        run("gasOverflow");
    }

    private static void controls() throws Throwable {
        for (String model : MACHINES) {
            GT_MigratedProcessingTest.runScenario("controlsReload", model);
        }
    }

    private static void legacyCycles() throws Throwable {
        for (String model : MACHINES) {
            GT_MigratedProcessingTest.runScenario("legacyCycle", model);
        }
    }

    private static void batchAndLock() throws Exception {
        for (String model : MACHINES) {
            try (Scenario scenario = new Scenario(model, 1)) {
                RecipeFixture f = scenario.recipe;
                f.register(24, 40);
                f.fill();
                scenario.machine.setProcessingModes(ProcessingLogic.BATCH | ProcessingLogic.LOCK_RECIPE);
                assertTrue(model, scenario.machine.checkRecipe(null));
                assertEquals(1, scenario.machine.getCurrentParallel());
                assertEquals(3, scenario.machine.getCurrentOperations());
                assertEquals(120, scenario.machine.mMaxProgresstime);
                assertEquals(-24, scenario.machine.lEUt);
                assertEquals(9, scenario.machine.mOutputItems[0].stackSize);
                assertEquals(225, scenario.machine.mOutputFluids[0].amount);
                if (f.iron != null) {
                    assertEquals(0, f.iron.stackSize);
                }
                assertEquals(0, f.water.amount);
                NBTTagCompound saved = scenario.save();
                scenario.machine.loadNBTData(saved);
                assertEquals(3, scenario.machine.getCurrentOperations());
                assertEquals(120, scenario.machine.mMaxProgresstime);
                scenario.machine.completeRecipe();
                f.machine.mInputBusses.clear();
                f.machine.mInputHatches.clear();
                f.fill();
                f.recipe.mDuration++;
                assertFalse(scenario.machine.checkRecipe(null));
                assertEquals(CheckRecipeResult.RECIPE_LOCK_INVALID, scenario.machine.getProcessingResult());
                f.assertUnconsumed();
            }
        }
    }

    private static void chemicalEnvironment() throws Exception {
        boolean lowGravity = GT_Mod.gregtechproxy.mLowGravProcessing;
        try (Scenario scenario = new Scenario("LargeChemicalReactor", 1)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 40);
            f.recipe.mSpecialValue = -200;
            f.fill();
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.CLEANROOM_REQUIRED, scenario.machine.getProcessingResult());
            f.assertUnconsumed();
            GT_MetaTileEntity_Cleanroom room = new GT_MetaTileEntity_Cleanroom("test");
            scenario.machine.mCleanroom = room;
            room.mEfficiency = 1;
            assertTrue(scenario.machine.checkRecipe(null));
            f.assertConsumed();
            scenario.machine.completeRecipe();
            room.mEfficiency = 0;
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.CLEANROOM_REQUIRED, scenario.machine.getProcessingResult());
            assertEquals(4, f.iron.stackSize);
            assertEquals(200, f.water.amount);
            f.recipe.mSpecialValue = -100;
            GT_Mod.gregtechproxy.mLowGravProcessing = false;
            assertTrue("Disabled low-gravity requirement must remain disabled", scenario.machine.checkRecipe(null));
        } finally {
            GT_Mod.gregtechproxy.mLowGravProcessing = lowGravity;
        }
    }

    private static void blastHeat() throws Exception {
        try (Scenario scenario = new Scenario("ElectricBlastFurnace", 3)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 80);
            f.fill();
            setField(scenario.machine, "mHeatingCapacity", 3600);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(-347, scenario.machine.lEUt);
            assertEquals(10, scenario.machine.mMaxProgresstime);
            scenario.machine.completeRecipe();
            setField(scenario.machine, "mHeatingCapacity", 1799);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.INSUFFICIENT_HEAT, scenario.machine.getProcessingResult());
            assertEquals(4, f.iron.stackSize);
            setField(scenario.machine, "mHeatingCapacity", 1800);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(-384, scenario.machine.lEUt);
            assertEquals(20, scenario.machine.mMaxProgresstime);
        }
    }

    @SuppressWarnings("unchecked")
    private static void chemicalGravity() throws Exception {
        Field worldsField = DimensionManager.class.getDeclaredField("worlds");
        worldsField.setAccessible(true);
        Map<Integer, WorldServer> worlds = (Map<Integer, WorldServer>) worldsField.get(null);
        WorldServer previousWorld = worlds.get(0);
        boolean previousGravity = GT_Mod.gregtechproxy.mLowGravProcessing;
        try (Scenario scenario = new Scenario("LargeChemicalReactor", 1)) {
            // Only the provider is read. Do not initialize a Minecraft server in this
            // isolated runtime.
            Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
            Constructor<?> constructor = (Constructor<?>) factoryClass
                    .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                    .invoke(factory, WorldServer.class, Object.class.getDeclaredConstructor());
            constructor.setAccessible(true);
            WorldServer world = (WorldServer) constructor.newInstance();
            setField(world, "provider", new WorldProviderSurface());
            worlds.put(0, world);
            GT_Mod.gregtechproxy.mLowGravProcessing = true;
            RecipeFixture f = scenario.recipe;
            f.register(24, 40);
            f.recipe.mSpecialValue = -100;
            f.fill();
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.LOW_GRAVITY_REQUIRED, scenario.machine.getProcessingResult());
            f.assertUnconsumed();
            setField(world, "provider", new TestMoonProvider());
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.machine.completeRecipe();
            setField(world, "provider", new WorldProviderSurface());
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.LOW_GRAVITY_REQUIRED, scenario.machine.getProcessingResult());
            assertEquals(4, f.iron.stackSize);
            assertEquals(200, f.water.amount);
        } finally {
            GT_Mod.gregtechproxy.mLowGravProcessing = previousGravity;
            if (previousWorld == null) {
                worlds.remove(0);
            } else {
                worlds.put(0, previousWorld);
            }
        }
    }

    public static final class TestMoonProvider extends WorldProviderSurface {
    }

    private static void towerOutputs() throws Exception {
        try (Scenario scenario = new Scenario("DistillationTower", 1)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 4);
            f.recipe.mFluidOutputs = new FluidStack[] { product(75), null, product(125) };
            f.fill();
            GT_MetaTileEntity_Hatch_Output first = scenario.output(1);
            GT_MetaTileEntity_Hatch_Output wrong = scenario.output(2);
            GT_MetaTileEntity_Hatch_Output third = scenario.output(3);
            scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            third.mFluid = product(third.getCapacity());
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, scenario.machine.getProcessingResult());
            f.assertUnconsumed();
            third.mFluid = null;
            assertTrue(scenario.machine.checkRecipe(null));
            third.mFluid = product(third.getCapacity());
            scenario.machine.setProcessingModes(0);
            scenario.machine.completeRecipe();
            assertEquals(75, first.mFluid.amount);
            assertNull(wrong.mFluid);
            assertTrue(scenario.machine.isWaitingForOutput());
            scenario.machine.loadNBTData(scenario.save());
            third.mFluid = null;
            scenario.machine.completeRecipe();
            assertFalse(scenario.machine.isWaitingForOutput());
            assertEquals(75, first.mFluid.amount);
            assertEquals(125, third.mFluid.amount);
            assertNull(wrong.mFluid);
            scenario.machine.completeRecipe();
            assertEquals(125, third.mFluid.amount);
            assertEquals(200, f.water.amount);
        }
    }

    private static void blastOutputs() throws Exception {
        for (int mufflerTier : new int[] { 1, 4, 8 }) {
            try (Scenario scenario = new Scenario("ElectricBlastFurnace", 1)) {
                RecipeFixture f = scenario.recipe;
                f.register(24, 4);
                setField(scenario.machine, "pollutionFluidStacks", new FluidStack[] { gas(1000) });
                f.recipe.mFluidOutputs = new FluidStack[] { product(75), gas(1000) };
                f.fill();
                GT_MetaTileEntity_Hatch_Output bottom = scenario.output(0);
                GT_MetaTileEntity_Hatch_Output top = scenario.output(3);
                GT_MetaTileEntity_Hatch_Muffler muffler = scenario.muffler(mufflerTier);
                int recovered = 10 * (105 - muffler.calculatePollutionReduction(100));
                scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
                top.mFluid = gas(top.getCapacity() - recovered + 1);
                assertFalse(scenario.machine.checkRecipe(null));
                f.assertUnconsumed();
                top.mFluid.amount--;
                assertTrue(scenario.machine.checkRecipe(null));
                assertEquals(recovered, scenario.machine.mOutputFluids[1].amount);
                assertEquals(1000, f.recipe.mFluidOutputs[1].amount);
                top.mFluid.amount++;
                scenario.machine.completeRecipe();
                assertTrue(scenario.machine.isWaitingForOutput());
                assertEquals(75, bottom.mFluid.amount);
                assertEquals(1, scenario.machine.mOutputFluids[1].amount);
                scenario.machine.loadNBTData(scenario.save());
                scenario.machine.mMufflerHatches.clear();
                scenario.muffler(1);
                top.mFluid = null;
                scenario.machine.completeRecipe();
                assertFalse(scenario.machine.isWaitingForOutput());
                assertEquals(1, top.mFluid.amount);
                assertEquals(75, bottom.mFluid.amount);
                scenario.machine.completeRecipe();
                assertEquals(1, top.mFluid.amount);
                assertEquals(4, f.iron.stackSize);
            }
        }
    }

    private static void towerMeOutputs() throws Exception {
        try (Scenario scenario = new Scenario("DistillationTower", 1)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 4);
            f.recipe.mFluidOutputs = new FluidStack[] { product(100), product(125) };
            f.fill();
            TestMEOutput first = new TestMEOutput();
            TestMEOutput second = new TestMEOutput();
            f.fixture.attach(first).yCoord = 1;
            f.fixture.attach(second).yCoord = 2;
            scenario.machine.mOutputHatches.add(first);
            scenario.machine.mOutputHatches.add(second);
            scenario.output(3);
            second.stored = 176;
            scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            assertFalse(scenario.machine.checkRecipe(null));
            f.assertUnconsumed();
            second.stored = 175;
            assertTrue(scenario.machine.checkRecipe(null));
            second.stored = 300;
            scenario.machine.completeRecipe();
            assertTrue(scenario.machine.isWaitingForOutput());
            assertEquals(100, first.stored);
            scenario.machine.loadNBTData(scenario.save());
            second.stored = 0;
            scenario.machine.completeRecipe();
            assertEquals(100, first.stored);
            assertEquals(125, second.stored);
            assertFalse(scenario.machine.isWaitingForOutput());
        }
    }

    private static final class TestMEOutput extends GT_MetaTileEntity_Hatch_Output_ME {
        int stored;

        TestMEOutput() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(FluidStack fluid) {
            return 300 - stored;
        }

        @Override
        public int fill(FluidStack fluid, boolean doFill) {
            int accepted = Math.min(fluid.amount, 300 - stored);
            if (doFill) {
                stored += accepted;
            }
            return accepted;
        }
    }

    private static void gasOverflow() throws Exception {
        try (Scenario scenario = new Scenario("ElectricBlastFurnace", 1)) {
            RecipeFixture f = scenario.recipe;
            f.register(24, 4);
            f.fill();
            f.recipe.mFluidOutputs = new FluidStack[] { gas(Integer.MAX_VALUE) };
            setField(scenario.machine, "pollutionFluidStacks", new FluidStack[] { gas(1000) });
            scenario.muffler(64);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.OUTPUT_OVERFLOW, scenario.machine.getProcessingResult());
            f.assertUnconsumed();

            GT_MetaTileEntity_MultiBlockBase legacy = new GT_LegacyProcessingTestMachine("legacy");
            new Fixture(legacy, 1);
            legacy.mEUt = -24;
            legacy.mMaxProgresstime = 4;
            legacy.mOutputFluids = new FluidStack[] { gas(1000), gas(Integer.MAX_VALUE) };
            NBTTagCompound saved = new NBTTagCompound();
            legacy.saveNBTData(saved);
            scenario.machine.loadNBTData(saved);
            scenario.machine.completeRecipe();
            assertEquals(CheckRecipeResult.OUTPUT_OVERFLOW, scenario.machine.getProcessingResult());
            assertEquals(0, scenario.machine.lEUt);
            assertEquals(1000, scenario.machine.mOutputFluids[0].amount);
            assertEquals(Integer.MAX_VALUE, scenario.machine.mOutputFluids[1].amount);
            scenario.machine.loadNBTData(scenario.save());
            scenario.machine.mMufflerHatches.clear();
            scenario.muffler(1);
            scenario.machine.completeRecipe();
            assertFalse(scenario.machine.isWaitingForOutput());
            assertEquals(6, f.iron.stackSize);
        }
    }

    private static void legacyBlastOutputs() throws Exception {
        try (Scenario scenario = new Scenario("ElectricBlastFurnace", 1)) {
            GT_MetaTileEntity_MultiBlockBase legacy = new GT_LegacyProcessingTestMachine("legacy");
            new Fixture(legacy, 1);
            legacy.mEUt = -24;
            legacy.mMaxProgresstime = 80;
            legacy.mOutputFluids = new FluidStack[] { gas(1000) };
            NBTTagCompound saved = new NBTTagCompound();
            legacy.saveNBTData(saved);
            scenario.machine.loadNBTData(saved);
            scenario.machine.loadNBTData(scenario.save());
            setField(scenario.machine, "pollutionFluidStacks", new FluidStack[] { gas(1000) });
            GT_MetaTileEntity_Hatch_Output top = scenario.output(3);
            GT_MetaTileEntity_Hatch_Muffler muffler = scenario.muffler(4);
            scenario.machine.completeRecipe();
            assertEquals(10 * (105 - muffler.calculatePollutionReduction(100)), top.mFluid.amount);
            int amount = top.mFluid.amount;
            scenario.machine.completeRecipe();
            assertEquals(amount, top.mFluid.amount);
        }
    }

    private static void crackerInputs() throws Exception {
        try (Scenario scenario = new Scenario("OilCracker", 1)) {
            RecipeFixture f = scenario.recipe;
            FluidStack oil = new FluidStack(fluid("cracker_feedstock"), 100);
            FluidStack steam = new FluidStack(fluid("steam"), 100);
            GT_Recipe recipe = f.map.addRecipe(false, new ItemStack[] { new ItemStack(Items.stick, 0) }, null, null,
                    null, new FluidStack[] { oil, steam }, new FluidStack[] { product(75) }, 40, 24, 0);
            assertNotNull(recipe);
            GT_MetaTileEntity_Hatch_Input feedstock = f.fixture.liquid(oil.copy());
            ((TileEntity) feedstock.getBaseMetaTileEntity()).xCoord = 2;
            GT_MetaTileEntity_Hatch_Input reagent = f.fixture.liquid(steam.copy());
            scenario.machine.mInventory[1] = new ItemStack(Items.stick);
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(100, feedstock.mFluid.amount);
            ItemStack circuit = new ItemStack(Items.stick);
            f.fixture.items(circuit);
            feedstock.mFluid = steam.copy();
            reagent.mFluid = oil.copy();
            assertFalse(scenario.machine.checkRecipe(null));
            assertEquals(100, feedstock.mFluid.amount);
            assertEquals(100, reagent.mFluid.amount);
            feedstock.mFluid = oil.copy();
            reagent.mFluid = steam.copy();
            scenario.machine.setProcessingModes(ProcessingLogic.SEPARATE_INPUTS);
            assertTrue(scenario.machine.checkRecipe(null));
            assertEquals(1, circuit.stackSize);
            assertEquals(1, scenario.machine.mInventory[1].stackSize);
            assertEquals(75, scenario.machine.mOutputFluids[0].amount);
        }
    }

    private static FluidStack product(int amount) {
        return new FluidStack(fluid("processing_product"), amount);
    }

    private static FluidStack gas(int amount) {
        return new FluidStack(fluid("processing_pollution"), amount);
    }

    private static final class Scenario implements AutoCloseable {
        final RecipeFixture recipe;
        final GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine;

        Scenario(String model, int tier) throws Exception {
            recipe = new RecipeFixture(model, tier);
            machine = (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) recipe.machine;
            recipe.fixture.tile.disableWorking();
        }

        GT_MetaTileEntity_Hatch_Output output(int level) {
            GT_MetaTileEntity_Hatch_Output hatch = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);
            recipe.fixture.attach(hatch).yCoord = machine.getBaseMetaTileEntity().getYCoord() + level;
            machine.mOutputHatches.add(hatch);
            return hatch;
        }

        GT_MetaTileEntity_Hatch_Muffler muffler(int tier) {
            GT_MetaTileEntity_Hatch_Muffler hatch = new GT_MetaTileEntity_Hatch_Muffler("test", tier, "test", null);
            recipe.fixture.attach(hatch);
            machine.mMufflerHatches.add(hatch);
            return hatch;
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }

        @Override
        public void close() {
            recipe.map.clear();
            GregTech_API.sPostloadFinished = recipe.previousPostload;
        }
    }
}
