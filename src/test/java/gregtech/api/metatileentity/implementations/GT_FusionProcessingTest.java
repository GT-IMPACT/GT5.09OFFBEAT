package gregtech.api.metatileentity.implementations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_FusionComputer;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_FusionComputer1;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_FusionComputer2;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_FusionComputer3;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_FusionComputer4;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_FusionComputer5;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GT_FusionProcessingTest {
    private static GameRuntime runtime;
    private final int model;

    public GT_FusionProcessingTest(int model) {
        this.model = model;
    }

    @Parameterized.Parameters(name = "MK {0}")
    public static Object[] models() {
        return new Object[] { 1, 2, 3, 4, 5 };
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
        runtime.run(getClass(), scenario, Integer.toString(model));
    }

    @Test
    public void allFortyTwoReferenceRecipesKeepTheirPowerAndDuration() throws Throwable {
        run("referenceRecipes");
    }

    @Test
    public void startupEnergyBoundaryNeverConsumesInputsOnRefusal() throws Throwable {
        run("startup");
    }

    @Test
    public void capacityBoundaryIsCheckedIndependentlyOfStoredEnergy() throws Throwable {
        run("capacity");
    }

    @Test
    public void continuousReactionDoesNotPayStartupAgain() throws Throwable {
        run("continuous");
    }

    @Test
    public void protectedOutputWaitsWithoutEnergyUseAndSurvivesReload() throws Throwable {
        run("protection");
    }

    @Test
    public void activeAndLegacySavesResumeWithoutRecipeLookupOrStartupCharge() throws Throwable {
        run("reload");
    }

    @Test
    public void batchAddsSequentialOperationsAndKeepsOneParallel() throws Throwable {
        run("batch");
    }

    @Test
    public void lockAndSettingsPersistAcrossReload() throws Throwable {
        run("locking");
    }

    @Test
    public void mixedAndMultiAmpHatchesDoNotBypassReactorChargingLimits() throws Throwable {
        run("hatches");
    }

    @Test
    public void starvationCannotProduceUnpaidProgress() throws Throwable {
        run("starvation");
    }

    @Test
    public void bufferIsRecheckedAfterOutputSimulationBeforeInputCommit() throws Throwable {
        run("changedBuffer");
    }

    @Test
    public void meOutputBackpressureDoesNotLoseOrDuplicatePlasma() throws Throwable {
        run("meOutput");
    }

    @Test
    public void guiHasNoControllerSlotAndSynchronizesInternalBuffer() throws Throwable {
        run("gui");
    }

    public static void runScenario(String scenario, String model) throws Throwable {
        try {
            GT_FusionProcessingTest.class.getDeclaredMethod(scenario, int.class).invoke(null, Integer.parseInt(model));
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        } finally {
            GT_Recipe.GT_Recipe_Map.sFusionRecipes.clear();
        }
    }

    private static void referenceRecipes(int model) throws Exception {
        JsonArray recipes;
        try (InputStreamReader reader = new InputStreamReader(
                GT_FusionProcessingTest.class.getResourceAsStream("/gregtech/fusion-recipes.json"), "UTF-8")) {
            recipes = new JsonParser().parse(reader).getAsJsonArray();
        }
        assertEquals(42, recipes.size());
        for (JsonElement element : recipes) {
            JsonObject record = element.getAsJsonObject();
            Scenario scenario = new Scenario(model, 16);
            int eu = record.get("eut").getAsInt();
            int ticks = record.get("ticks").getAsInt();
            int startup = record.get("startupEU").getAsInt();
            int multiplier = record.getAsJsonArray("multipliers").get(model - 1).getAsInt();
            scenario.recipe(eu, ticks, startup);
            scenario.machine.setEUVar(scenario.machine.maxEUStore());
            String name = record.get("name").getAsString();
            assertEquals(name, multiplier > 0, scenario.machine.checkRecipe(null));
            if (multiplier == 0) {
                scenario.assertInputs(1000);
                continue;
            }
            assertEquals(name, -(long) eu * multiplier, scenario.machine.lEUt);
            assertEquals(name, ticks / multiplier, scenario.machine.mMaxProgresstime);
            assertEquals(name, 1, scenario.machine.getCurrentParallel());
            scenario.assertInputs(990);
        }
    }

    private static void startup(int model) {
        for (int delta : new int[] { -1, 0, 1 }) {
            Scenario scenario = new Scenario(model, 16);
            scenario.recipe(128, 128, 1000000);
            int multiplier = multiplier(model);
            long cost = 1000000 - 128L * multiplier;
            scenario.machine.setEUVar(cost + delta);
            assertEquals(delta >= 0, scenario.machine.checkRecipe(null));
            if (delta < 0) {
                assertEquals(CheckRecipeResult.INSUFFICIENT_STARTUP_ENERGY, scenario.machine.getProcessingResult());
                scenario.assertInputs(1000);
                assertEquals(cost - 1, scenario.machine.getEUVar());
            } else {
                scenario.assertInputs(990);
                assertEquals(delta, scenario.machine.getEUVar());
            }
        }
    }

    private static void capacity(int model) {
        for (int delta : new int[] { -1, 0, 1 }) {
            Scenario scenario = new Scenario(model, 1);
            scenario.recipe(128, 128, (int) scenario.machine.maxEUStore() + delta);
            scenario.machine.setEUVar(scenario.machine.maxEUStore());
            assertEquals(delta <= 0, scenario.machine.checkRecipe(null));
            if (delta > 0) {
                assertEquals(CheckRecipeResult.INSUFFICIENT_STARTUP_CAPACITY, scenario.machine.getProcessingResult());
                scenario.assertInputs(1000);
            }
        }
    }

    private static void continuous(int model) {
        Scenario scenario = new Scenario(model, 16);
        scenario.recipe(128, 128, 1000000);
        scenario.machine.setEUVar(1000000);
        assertTrue(scenario.machine.checkRecipe(null));
        assertTrue(scenario.machine.onRunningTick(null));
        assertEquals(0, scenario.machine.getEUVar());
        scenario.fixture.tile.enableWorking();
        scenario.finish();
        assertTrue(scenario.machine.mMaxProgresstime > 0);
        assertEquals(0, scenario.machine.getEUVar());
        scenario.assertInputs(980);
        scenario.fixture.tile.disableWorking();
        scenario.finish();
        assertFalse(scenario.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.INSUFFICIENT_STARTUP_ENERGY, scenario.machine.getProcessingResult());
        scenario.assertInputs(980);
    }

    private static void protection(int model) {
        Scenario scenario = new Scenario(model, 16);
        scenario.recipe(128, 128, 1000000);
        GT_MetaTileEntity_Hatch_Output output = scenario.output();
        output.mFluid = new FluidStack(scenario.product, output.getCapacity());
        scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        scenario.machine.setEUVar(1000000);
        assertFalse(scenario.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, scenario.machine.getProcessingResult());
        assertEquals(1000000, scenario.machine.getEUVar());
        scenario.assertInputs(1000);
        output.mFluid = null;
        assertTrue(scenario.machine.checkRecipe(null));
        output.mFluid = new FluidStack(scenario.product, output.getCapacity());
        scenario.finish();
        assertTrue(scenario.machine.isWaitingForOutput());
        long energy = scenario.machine.getEUVar();
        scenario.machine.loadNBTData(scenario.save());
        scenario.machine.setProcessingModes(0);
        assertTrue(scenario.machine.onRunningTick(null));
        assertEquals(energy, scenario.machine.getEUVar());
        scenario.finish();
        assertTrue(scenario.machine.isWaitingForOutput());
        output.mFluid = null;
        scenario.finish();
        assertFalse(scenario.machine.isWaitingForOutput());
        assertEquals(10, output.mFluid.amount);
        scenario.finish();
        assertEquals(10, output.mFluid.amount);
        scenario.assertInputs(990);
    }

    private static void reload(int model) {
        for (boolean legacy : new boolean[] { false, true }) {
            Scenario scenario = new Scenario(model, 16);
            scenario.recipe(128, 128, 1000000);
            GT_MetaTileEntity_Hatch_Output output = scenario.output();
            scenario.machine.setEUVar(1000000);
            assertTrue(scenario.machine.checkRecipe(null));
            scenario.machine.mProgresstime = 1;
            long eu = -scenario.machine.lEUt;
            int duration = scenario.machine.mMaxProgresstime;
            NBTTagCompound tag = scenario.save();
            if (legacy) {
                tag.removeTag("processing");
                tag.setInteger("mEUt", (int) eu);
            }
            GT_Recipe.GT_Recipe_Map.sFusionRecipes.clear();
            scenario.machine.loadNBTData(tag);
            assertEquals(-eu, scenario.machine.lEUt);
            assertEquals(duration, scenario.machine.mMaxProgresstime);
            assertEquals(1, scenario.machine.mProgresstime);
            assertFalse(scenario.machine.checkRecipe(null));
            assertTrue(scenario.machine.onRunningTick(null));
            assertEquals(0, scenario.machine.getEUVar());
            scenario.finish();
            assertEquals(10, output.mFluid.amount);
            scenario.assertInputs(990);
        }
    }

    private static void batch(int model) {
        for (boolean enabled : new boolean[] { false, true }) {
            Scenario scenario = new Scenario(model, 16);
            scenario.recipe(128, 128, 1000000);
            scenario.machine.setProcessingModes(enabled ? ProcessingLogic.BATCH : 0);
            scenario.machine.setEUVar(1000000);
            assertTrue(scenario.machine.checkRecipe(null));
            int count = enabled ? multiplier(model) : 1;
            assertEquals(count, scenario.machine.getCurrentOperations());
            assertEquals(1, scenario.machine.getCurrentParallel());
            assertEquals(-128L * multiplier(model), scenario.machine.lEUt);
            assertEquals(128 / multiplier(model) * count, scenario.machine.mMaxProgresstime);
            assertEquals(128L * multiplier(model), scenario.machine.getEUVar());
            scenario.assertInputs(1000 - count * 10);
        }
    }

    private static void locking(int model) {
        Scenario scenario = new Scenario(model, 16);
        scenario.recipe(128, 128, 1000000);
        scenario.machine.setProcessingModes(ProcessingLogic.LOCK_RECIPE);
        scenario.machine.setEUVar(1000000);
        assertTrue(scenario.machine.checkRecipe(null));
        scenario.finish();
        scenario.machine.loadNBTData(scenario.save());
        scenario.recipe(256, 128, 1000000);
        scenario.machine.setEUVar(1000000);
        assertFalse(scenario.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.RECIPE_LOCK_INVALID, scenario.machine.getProcessingResult());
        scenario.assertInputs(990);
        for (int modes = 0; modes <= ProcessingLogic.ALL_MODES; modes++) {
            scenario.machine.setProcessingModes(modes);
            scenario.machine.loadNBTData(scenario.save());
            assertEquals(modes & (ProcessingLogic.BATCH | ProcessingLogic.PROTECT_FLUIDS | ProcessingLogic.LOCK_RECIPE),
                    scenario.machine.getProcessingModes());
        }
    }

    private static void hatches(int model) {
        for (int ordinary : new int[] { 0, 1, 2, 16 }) {
            for (int amps : new int[] { 1, 4, 16, 64 }) {
                Scenario scenario = new Scenario(model, ordinary);
                GT_MetaTileEntity_Hatch_EnergyMulti multi = new GT_MetaTileEntity_Hatch_EnergyMulti("test",
                        scenario.machine.tier(), amps, "", null);
                scenario.fixture.attach(multi);
                scenario.machine.mEnergyHatchesMulti.add(multi);
                multi.setEUVar(1000000);
                scenario.machine.mMachine = true;
                scenario.machine.mStartUpCheck = -1;
                scenario.machine.mUpdate = 1000;
                scenario.machine.setEUVar(0);
                scenario.machine.onPostTick(scenario.fixture.tile, 1);
                assertEquals(ordinary * chargingRate(model), scenario.machine.getEUVar());
                assertEquals(1000000, multi.getEUVar());
                scenario.recipe(128, 128, 1000000);
                scenario.machine.setEUVar(scenario.machine.maxEUStore());
                assertEquals(ordinary > 0, scenario.machine.checkRecipe(null));
            }
        }
    }

    private static void starvation(int model) {
        Scenario scenario = new Scenario(model, 16);
        scenario.recipe(128, 128, 1000000);
        scenario.machine.setEUVar(1000000);
        assertTrue(scenario.machine.checkRecipe(null));
        long eut = -scenario.machine.lEUt;
        scenario.machine.setEUVar(eut - 1);
        assertFalse(scenario.machine.onRunningTick(null));
        assertEquals(eut - 1, scenario.machine.getEUVar());
        assertEquals(0, scenario.machine.mMaxProgresstime);
        assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, scenario.machine.getProcessingResult());
        assertNull(scenario.machine.mOutputFluids);
    }

    private static void changedBuffer(int model) {
        Scenario scenario = new Scenario(model, 16);
        scenario.recipe(128, 128, 1000000);
        GT_MetaTileEntity_Hatch_Output_ME output = new GT_MetaTileEntity_Hatch_Output_ME("test", 4, new String[0],
                null) {
            @Override
            public long getProcessingCapacity(FluidStack fluid) {
                scenario.machine.setEUVar(0);
                return 10000;
            }
        };
        scenario.fixture.attach(output);
        scenario.machine.mOutputHatches.add(output);
        scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        scenario.machine.setEUVar(1000000);
        assertFalse(scenario.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.INPUT_CHANGED, scenario.machine.getProcessingResult());
        scenario.assertInputs(1000);
    }

    private static void meOutput(int model) {
        Scenario scenario = new Scenario(model, 16);
        scenario.recipe(128, 128, 1000000);
        MEOutput output = new MEOutput();
        scenario.fixture.attach(output);
        scenario.machine.mOutputHatches.add(output);
        scenario.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        scenario.machine.setEUVar(1000000);
        assertTrue(scenario.machine.checkRecipe(null));
        output.capacity = 0;
        scenario.finish();
        assertTrue(scenario.machine.isWaitingForOutput());
        output.capacity = 10;
        scenario.finish();
        assertEquals(10, output.stored);
        scenario.finish();
        assertEquals(10, output.stored);
        scenario.assertInputs(990);
    }

    private static void gui(int model) throws Exception {
        Scenario scenario = new Scenario(model, 16);
        scenario.machine.setEUVar(1234567);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                scenario.fixture.world);
        GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory,
                scenario.fixture.tile);
        assertEquals(0, gui.getSlotCount());
        assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
        assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.BATCH_SLOT));
        assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.LOCK_SLOT));
        assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.VOIDING_SLOT));
        assertFalse(gui.isControlVisible(GT_Container_ProcessingMachine.SEPARATION_SLOT));
        assertSame(GT_Recipe.GT_Recipe_Map.sFusionRecipes, gui.getNEIRecipeMap());
        Method snapshot = GT_Container_ProcessingMachine.class.getDeclaredMethod("outputSnapshot");
        snapshot.setAccessible(true);
        NBTTagCompound tag = (NBTTagCompound) snapshot.invoke(gui);
        assertEquals(1234567, tag.getLong("storedEnergy"));
        assertEquals(scenario.machine.maxEUStore(), tag.getLong("energyCapacity"));
        assertEquals(0, scenario.machine.getProcessingModes());
    }

    private static int multiplier(int model) {
        return new int[] { 1, 2, 4, 64, 64 }[model - 1];
    }

    private static long chargingRate(int model) {
        return new long[] { 2048, 4096, 8192, 32768, 131072 }[model - 1];
    }

    private static GT_MetaTileEntity_FusionComputer machine(int model) {
        switch (model) {
        case 1:
            return new GT_MetaTileEntity_FusionComputer1("test");
        case 2:
            return new GT_MetaTileEntity_FusionComputer2("test");
        case 3:
            return new GT_MetaTileEntity_FusionComputer3("test");
        case 4:
            return new GT_MetaTileEntity_FusionComputer4("test");
        case 5:
            return new GT_MetaTileEntity_FusionComputer5("test");
        default:
            throw new IllegalArgumentException(Integer.toString(model));
        }
    }

    private static final class Scenario {
        final GT_MetaTileEntity_FusionComputer machine;
        final Fixture fixture;
        final Fluid first = fluid("shared_fusion_first");
        final Fluid second = fluid("shared_fusion_second");
        final Fluid product = fluid("shared_fusion_product");
        final FluidStack firstInput = new FluidStack(first, 1000);
        final FluidStack secondInput = new FluidStack(second, 1000);

        Scenario(int model, int hatches) {
            GT_Recipe.GT_Recipe_Map.sFusionRecipes.clear();
            machine = machine(model);
            fixture = new Fixture(machine, machine.tier());
            fixture.tile.disableWorking();
            machine.mEnergyHatches.clear();
            for (int i = 0; i < hatches; i++) {
                GT_MetaTileEntity_Hatch_Energy hatch = new GT_MetaTileEntity_Hatch_Energy("test", machine.tier(), "",
                        null);
                fixture.attach(hatch);
                hatch.setEUVar(1000000);
                machine.mEnergyHatches.add(hatch);
            }
            fixture.liquid(firstInput);
            fixture.liquid(secondInput);
        }

        void recipe(int eut, int ticks, int startup) {
            GT_Recipe.GT_Recipe_Map.sFusionRecipes.clear();
            GT_Recipe recipe = GT_Recipe.GT_Recipe_Map.sFusionRecipes.addRecipe(false, new ItemStack[0],
                    new ItemStack[0], null, null,
                    new FluidStack[] { new FluidStack(first, 10), new FluidStack(second, 10) },
                    new FluidStack[] { new FluidStack(product, 10) }, ticks, eut, startup);
            assertNotNull(recipe);
        }

        GT_MetaTileEntity_Hatch_Output output() {
            GT_MetaTileEntity_Hatch_Output output = new GT_MetaTileEntity_Hatch_Output("test", 4, "", null);
            fixture.attach(output);
            machine.mOutputHatches.add(output);
            return output;
        }

        void assertInputs(int amount) {
            assertEquals(amount, firstInput.amount);
            assertEquals(amount, secondInput.amount);
        }

        void finish() {
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }
    }

    private static final class MEOutput extends GT_MetaTileEntity_Hatch_Output_ME {
        int capacity = 10;
        int stored;

        MEOutput() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(FluidStack fluid) {
            return capacity;
        }

        @Override
        public int fill(FluidStack fluid, boolean commit) {
            int accepted = Math.min(capacity, fluid.amount);
            if (commit) {
                stored += accepted;
                capacity -= accepted;
            }
            return accepted;
        }
    }
}
