package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.logic.TurbineProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.Fixture;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.GameRuntime;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.ProcessingWorld;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.multiblock.ProcessingGuiControls;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.fluid;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class GT_TurbineProcessingTest {

    private static GameRuntime runtime;
    private final String model;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] models() {
        return new Object[] { "Steam", "HPSteam", "Gas", "Plasma" };
    }

    public GT_TurbineProcessingTest(String model) {
        this.model = model;
    }

    @BeforeClass
    public static void openGame() throws Exception {
        runtime = new GameRuntime();
    }

    @AfterClass
    public static void closeGame() throws Exception {
        if (runtime != null) {
            runtime.close();
        }
    }

    private void run(String scenario) throws Throwable {
        runtime.run(getClass(), scenario, model);
    }

    @Test
    public void protectedOutputIsTransactionalAndSurvivesReload() throws Throwable {
        run("protectedOutput");
    }

    @Test
    public void meOutputRetriesWithoutRepeatingGeneration() throws Throwable {
        run("meOutput");
    }

    @Test
    public void controlsAndRotorSurviveWorldReload() throws Throwable {
        run("controls");
    }

    @Test
    public void ordinaryAndMultiAmpDynamosAcrossAllTiers() throws Throwable {
        run("dynamos");
    }

    @Test
    public void activeAndLegacyCyclesResumeWithoutSpendingFuelTwice() throws Throwable {
        run("resume");
    }

    @Test
    public void invalidFuelAndMissingRotorDoNotConsumeInputs() throws Throwable {
        run("rejections");
    }

    @Test
    public void fractionalWaterAndPowerRampSurviveReload() throws Throwable {
        run("rampAndRemainder");
    }

    @Test
    public void powerAboveIntegerRangeSurvivesReload() throws Throwable {
        run("largePower");
    }

    @Test
    public void splitHatchesAndSourceChangesAreTransactional() throws Throwable {
        run("snapshots");
    }

    public static void runScenario(String scenario, String model) throws Throwable {
        try {
            GT_TurbineProcessingTest.class.getDeclaredMethod(scenario, String.class).invoke(null, model);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void protectedOutput(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            GT_MetaTileEntity_Hatch_Output output = s.output();
            s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            output.mFluid = s.product(output.getCapacity());
            if (s.outputAmount == 0) {
                assertTrue(s.machine.checkRecipe(null));
                assertEquals(0, s.input.getFluidAmount());
                return;
            }
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, s.machine.getProcessingResult());
            assertEquals(s.optimum, s.input.getFluidAmount());
            output.mFluid = null;
            assertTrue(s.machine.checkRecipe(null));
            output.mFluid = s.product(output.getCapacity() - 1);
            s.machine.setProcessingModes(0);
            s.finish();
            assertTrue(s.machine.isWaitingForOutput());
            assertEquals(s.outputAmount - 1, s.machine.mOutputFluids[0].amount);
            s.machine.loadNBTData(s.save());
            output.mFluid = null;
            assertTrue(s.machine.onRunningTick(null));
            assertEquals(0, s.machine.getRunningEUt());
            s.finish();
            assertFalse(s.machine.isWaitingForOutput());
            assertEquals(s.outputAmount - 1, output.getFluidAmount());
            s.finish();
            assertEquals(s.outputAmount - 1, output.getFluidAmount());
            assertEquals(0, s.input.getFluidAmount());
        }
    }

    private static void meOutput(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            TestME output = new TestME();
            s.fixture.attach(output);
            s.machine.mOutputHatches.add(output);
            s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            output.capacity = s.outputAmount;
            assertTrue(s.machine.checkRecipe(null));
            output.capacity = 0;
            s.finish();
            assertEquals(s.outputAmount > 0, s.machine.isWaitingForOutput());
            output.capacity = s.outputAmount;
            s.finish();
            assertEquals(s.outputAmount, output.stored);
            assertEquals(0, s.input.getFluidAmount());
            s.finish();
            assertEquals(s.outputAmount, output.stored);
        }
    }

    private static void controls(String model) throws Exception {
        boolean hasFluidProtection = !model.equals("Gas");
        int supportedModes = hasFluidProtection ? ProcessingLogic.PROTECT_FLUIDS : 0;
        GT_Recipe.GT_Recipe_Map neiMap = model.equals("Gas") ? GT_Recipe.GT_Recipe_Map.sTurbineFuels
                : model.equals("Plasma") ? GT_Recipe.GT_Recipe_Map.sPlasmaFuels : null;
        boolean hasNei = neiMap != null;
        int id = 1003;
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[id];
        GregTech_API.METATILEENTITIES[id] = machine(model);
        try {
            for (boolean enabled : new boolean[] { false, true }) {
                for (int modes : new int[] { 0, ProcessingLogic.PROTECT_FLUIDS, ProcessingLogic.ALL_MODES }) {
                    ProcessingWorld world = new ProcessingWorld();
                    BaseMetaTileEntity tile = new BaseMetaTileEntity();
                    tile.setWorldObj(world);
                    tile.setInitialValuesAsNBT(null, (short) id);
                    GT_MetaTileEntity_LargeTurbine machine = (GT_MetaTileEntity_LargeTurbine) tile.getMetaTileEntity();
                    machine.optFlow = 1600;
                    machine.baseEff = 10000;
                    machine.checkguislot = false;
                    machine.mNameRotorMaterial = "Test rotor";
                    machine.mInventory[1] = new ItemStack(Items.iron_ingot);
                    machine.setProcessingModes(modes);
                    if (enabled) {
                        tile.enableWorking();
                    } else {
                        tile.disableWorking();
                    }
                    NBTTagCompound tag = new NBTTagCompound();
                    tile.writeToNBT(tag);
                    BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
                    restoredTile.setWorldObj(world);
                    restoredTile.readFromNBT(tag);
                    GT_MetaTileEntity_LargeTurbine restored = (GT_MetaTileEntity_LargeTurbine) restoredTile
                            .getMetaTileEntity();
                    assertEquals(enabled, restoredTile.isAllowedToWork());
                    assertEquals(modes & supportedModes, restored.getProcessingModes());
                    assertEquals(1600, restored.optFlow);
                    assertEquals(10000, restored.baseEff);
                    assertFalse(restored.checkguislot);
                    assertEquals("Test rotor", restored.mNameRotorMaterial);
                    assertEquals(Items.iron_ingot, restored.mInventory[1].getItem());
                    assertSame(neiMap, restored.getRecipeMapForNEI());
                    assertNull(restored.getRecipeMap());
                    assertEquals(hasNei,
                            ProcessingGuiControls.isControlVisible(restored, GT_Container_ProcessingMachine.NEI_SLOT));
                    assertEquals(hasFluidProtection, ProcessingGuiControls.isControlVisible(restored,
                            GT_Container_ProcessingMachine.VOIDING_SLOT));
                    GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
                    GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory,
                            restoredTile) {
                        @Override
                        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer entity) {
                            return true;
                        }
                    };
                    player.openContainer = gui;
                    assertEquals(43, gui.inventorySlots.size());
                    assertEquals(1, gui.getSlotCount());
                    assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.POWER_SLOT));
                    assertEquals(hasFluidProtection, gui.isControlVisible(GT_Container_ProcessingMachine.VOIDING_SLOT));
                    assertEquals(hasNei, gui.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
                    assertEquals(hasNei, gui.getSlot(GT_Container_ProcessingMachine.NEI_SLOT).xDisplayPosition >= 0);
                    for (int slot : new int[] { 1, 2, 3 }) {
                        assertFalse(gui.isControlVisible(slot));
                        assertTrue("Hidden controls must not leave an interactive slot inside the GUI",
                                gui.getSlot(slot).xDisplayPosition < 0);
                        gui.slotClick(slot, 0, 0, player);
                        assertEquals(modes & supportedModes, restored.getProcessingModes());
                    }
                    gui.slotClick(GT_Container_ProcessingMachine.VOIDING_SLOT, 0, 0, player);
                    assertEquals((modes ^ supportedModes) & supportedModes, restored.getProcessingModes());
                    assertEquals(hasNei ? 11 : 20,
                            gui.getSlot(GT_Container_ProcessingMachine.POWER_SLOT).xDisplayPosition);
                    int visibleControls = 0;
                    for (int slot = 0; slot < GT_Container_ProcessingMachine.CONTROL_COUNT; slot++) {
                        if (gui.getSlot(slot).xDisplayPosition >= 0) {
                            visibleControls++;
                        }
                    }
                    assertEquals(1 + (hasFluidProtection ? 1 : 0) + (hasNei ? 1 : 0), visibleControls);
                }
            }
        } finally {
            GregTech_API.METATILEENTITIES[id] = previous;
        }
    }

    private static void dynamos(String model) throws Exception {
        for (int tier = 0; tier < GT_Values.V.length; tier++) {
            for (int amps : new int[] { 0, 1, 4, 64, 1024 }) {
                GT_MetaTileEntity_LargeTurbine machine = machine(model);
                Fixture fixture = new Fixture(machine, 1);
                GT_MetaTileEntity_Hatch dynamo;
                if (amps == 0) {
                    GT_MetaTileEntity_Hatch_Dynamo ordinary = new GT_MetaTileEntity_Hatch_Dynamo("test", tier, "test",
                            null);
                    machine.mDynamoHatches.add(ordinary);
                    dynamo = ordinary;
                } else {
                    GT_MetaTileEntity_Hatch_DynamoMulti multi = new GT_MetaTileEntity_Hatch_DynamoMulti("test", tier,
                            amps, "test", null);
                    machine.mDynamoHatchesMulti.add(multi);
                    dynamo = multi;
                }
                fixture.attach(dynamo);
                long throughput = dynamo.maxEUOutput() * dynamo.maxAmperesOut();
                machine.lEUt = throughput;
                machine.mEfficiency = 10000;
                assertTrue(machine.onRunningTick(null));
                assertEquals(model + " tier=" + tier + " amps=" + amps, throughput, dynamo.getEUVar());
                assertEquals(1000000, machine.mEnergyHatches.get(0).getEUVar());
                dynamo.setEUVar(0);
                machine.mEfficiency = 7500;
                assertTrue(machine.onRunningTick(null));
                assertEquals(throughput * 3 / 4, dynamo.getEUVar());

                GT_MetaTileEntity_Hatch_Dynamo extra = new GT_MetaTileEntity_Hatch_Dynamo("test", 1, "test", null);
                fixture.attach(extra);
                machine.mDynamoHatches.add(extra);
                dynamo.setEUVar(0);
                machine.mEfficiency = 10000;
                machine.lEUt = throughput + extra.maxEUOutput() * extra.maxAmperesOut();
                assertTrue(machine.onRunningTick(null));
                assertEquals(machine.lEUt, dynamo.getEUVar() + extra.getEUVar());
            }
        }
    }

    private static void resume(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.mEfficiency = 7000;
            assertTrue(s.machine.checkRecipe(null));
            long power = s.machine.getRunningEUt();
            NBTTagCompound tag = s.save();
            GT_MetaTileEntity_LargeTurbine restored = machine(model);
            Fixture fixture = new Fixture(restored, 1);
            fixture.tile.disableWorking();
            restored.loadNBTData(tag);
            assertEquals(power, restored.getRunningEUt());
            assertEquals(7000, restored.mEfficiency);
            assertEquals(1, restored.mMaxProgresstime);
            assertFalse(restored.checkRecipe(null));
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) restored).completeRecipe();
            assertEquals(7010, restored.mEfficiency);
            assertEquals(0, s.input.getFluidAmount());

            GT_LegacyProcessingTestMachine legacy = new GT_LegacyProcessingTestMachine("legacy");
            new Fixture(legacy, 1);
            legacy.mEUt = 800;
            legacy.mMaxProgresstime = 1;
            legacy.mEfficiency = 7000;
            legacy.mEfficiencyIncrease = 10;
            tag = new NBTTagCompound();
            legacy.saveNBTData(tag);
            tag.setInteger("baseEff", 10000);
            tag.setInteger("optFlow", 1600);
            tag.setBoolean("checkguislot", false);
            restored.loadNBTData(tag);
            assertEquals(800, restored.getRunningEUt());
            assertFalse(restored.checkRecipe(null));
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) restored).completeRecipe();
            assertEquals(7010, restored.mEfficiency);
            assertEquals(0, s.input.getFluidAmount());
        }
    }

    private static void rejections(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.optFlow = 0;
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.MACHINE_CONDITION, s.machine.getProcessingResult());
            assertEquals(s.optimum, s.input.getFluidAmount());
            s.machine.optFlow = 1600;
            s.input.mFluid = new FluidStack(fluid("turbine_not_fuel"), 2000);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(2000, s.input.getFluidAmount());
            s.input.mFluid = new FluidStack(s.fuel, 0);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(0, s.machine.getRunningEUt());
            if (s.map != null) {
                s.input.mFluid = new FluidStack(s.fuel, s.optimum);
                s.map.clear();
                assertFalse(s.machine.checkRecipe(null));
                assertEquals(s.optimum, s.input.getFluidAmount());
            }
        }
    }

    private static void rampAndRemainder(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.mEfficiency = 1000;
            GT_MetaTileEntity_Hatch_Output output = s.output();
            assertTrue(s.machine.checkRecipe(null));
            long first = s.machine.getRunningEUt();
            s.finish();
            assertEquals(1010, s.machine.mEfficiency);
            s.machine.loadNBTData(s.save());
            s.input.mFluid = new FluidStack(s.fuel, s.optimum);
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(TurbineProcessingLogic.ramp(first, s.target), s.machine.getRunningEUt());
            s.finish();
            s.input.mFluid = null;
            long previous = s.save().getLong("rotorPower");
            boolean running = s.machine.checkRecipe(null);
            assertEquals(TurbineProcessingLogic.ramp(previous, 0) > 0, running);
            assertEquals(Math.max(0, TurbineProcessingLogic.ramp(previous, 0)), s.machine.getRunningEUt());
            if (model.equals("Steam")) {
                s.machine.stopMachine();
                output.mFluid = null;
                for (int cycle = 0; cycle < 8; cycle++) {
                    s.input.mFluid = new FluidStack(s.fuel, 100);
                    assertTrue(s.machine.checkRecipe(null));
                    s.finish();
                    s.machine.loadNBTData(s.save());
                }
                assertEquals(5, output.getFluidAmount());
            }
        }
    }

    private static void largePower(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.optFlow = Integer.MAX_VALUE;
            s.machine.baseEff = Integer.MAX_VALUE;
            s.input.mFluid = new FluidStack(s.fuel, Integer.MAX_VALUE);
            assertTrue(s.machine.checkRecipe(null));
            assertTrue(s.machine.getRunningEUt() > Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, s.machine.mEUt);
            long power = s.machine.getRunningEUt();
            s.machine.loadNBTData(s.save());
            assertEquals(power, s.machine.getRunningEUt());
            if (model.equals("Plasma")) {
                s.machine.stopMachine();
                s.map.mRecipeList.iterator().next().mSpecialValue = Integer.MAX_VALUE;
                s.input.mFluid = new FluidStack(s.fuel, Integer.MAX_VALUE);
                assertTrue(s.machine.checkRecipe(null));
                assertTrue(s.machine.getRunningEUt() > Integer.MAX_VALUE);
            }
        }
    }

    private static void snapshots(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            int first = s.optimum / 2;
            s.input.mFluid.amount = first;
            GT_MetaTileEntity_Hatch_Input second = s.fixture.liquid(new FluidStack(s.fuel, s.optimum - first));
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(0, s.input.getFluidAmount());
            assertEquals(0, second.getFluidAmount());
            s.machine.stopMachine();
            FluidStack original = new FluidStack(s.fuel, s.optimum);
            ProcessingInputs snapshot = new ProcessingInputs(Collections.emptyList(), Arrays.asList(original, original),
                    () -> false);
            ProcessingLogic.Request request = new ProcessingLogic.Request(Collections.singletonList(snapshot), 0, 0, 0,
                    new ProcessingOutputs(s.machine), bound -> 0);
            assertEquals(CheckRecipeResult.INPUT_CHANGED, s.machine.getProcessingLogic().process(request).status);
            assertEquals(s.optimum, original.amount);
        }
    }

    private static GT_MetaTileEntity_LargeTurbine machine(String model) throws Exception {
        return (GT_MetaTileEntity_LargeTurbine) GT_MultiblockRecipeProcessingTest.machine("LargeTurbine_" + model);
    }

    private static final class TestME extends GT_MetaTileEntity_Hatch_Output_ME {
        int capacity;
        int stored;

        TestME() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(FluidStack fluid) {
            return capacity - stored;
        }

        @Override
        public int fill(FluidStack fluid, boolean commit) {
            int accepted = Math.min(fluid.amount, Math.max(0, capacity - stored));
            if (commit) {
                stored += accepted;
            }
            return accepted;
        }
    }

    private static final class Scenario implements AutoCloseable {
        final GT_MetaTileEntity_LargeTurbine machine;
        final Fixture fixture;
        final GT_MetaTileEntity_Hatch_Input input;
        final Fluid fuel;
        final GT_Recipe.GT_Recipe_Map map;
        final int optimum;
        final int outputAmount;
        final long target;
        final String product;

        Scenario(String model) throws Exception {
            fluid("steam");
            fluid("ic2distilledwater");
            fluid("test_coolant");
            if (!FluidRegistry.isFluidRegistered("ic2superheatedsteam")) {
                FluidRegistry.registerFluid(new Fluid("ic2superheatedsteam") {
                    @Override
                    public String getUnlocalizedName(FluidStack fluid) {
                        return "ic2.fluidSuperheatedSteam";
                    }
                });
            }
            boolean gas = model.equals("Gas");
            boolean plasma = model.equals("Plasma");
            boolean steam = model.equals("Steam");
            fuel = fluid(
                    gas ? "turbine_gas" : plasma ? "plasma.test_coolant" : steam ? "steam" : "ic2superheatedsteam");
            map = gas ? GT_Recipe.GT_Recipe_Map.sTurbineFuels : plasma ? GT_Recipe.GT_Recipe_Map.sPlasmaFuels : null;
            if (map != null) {
                ItemStack container = new ItemStack(gas ? Items.apple : Items.carrot);
                GT_Utility.addFluidContainerData(new FluidContainerRegistry.FluidContainerData(
                        new FluidStack(fuel, 1000), container, new ItemStack(Items.bucket)));
                assertNotNull(
                        map.addRecipe(false, new ItemStack[] { container }, null, null, null, null, null, 0, 0, 100));
            }
            optimum = gas ? 16 : plasma ? 641 : 1600;
            outputAmount = gas ? 0 : steam ? 10 : optimum;
            target = gas ? 1600 : plasma ? 64100 : steam ? 800 : 1600;
            product = steam ? "ic2distilledwater" : plasma ? "test_coolant" : "steam";
            machine = machine(model);
            fixture = new Fixture(machine, 1);
            fixture.tile.disableWorking();
            machine.optFlow = 1600;
            machine.baseEff = 10000;
            machine.checkguislot = false;
            setField(machine, "fuelAchievement", true);
            input = fixture.liquid(new FluidStack(fuel, optimum));
        }

        GT_MetaTileEntity_Hatch_Output output() {
            GT_MetaTileEntity_Hatch_Output output = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);
            fixture.attach(output);
            machine.mOutputHatches.add(output);
            return output;
        }

        FluidStack product(int amount) {
            return new FluidStack(fluid(product), amount);
        }

        void finish() {
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }

        @Override
        public void close() {
            if (map != null) {
                map.clear();
            }
        }
    }
}
