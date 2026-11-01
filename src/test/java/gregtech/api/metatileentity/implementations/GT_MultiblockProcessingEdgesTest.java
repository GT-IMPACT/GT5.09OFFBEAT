package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.common.tileentities.machines.multi.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static gregtech.api.metatileentity.implementations.GT_MultiblockRecipeProcessingTest.*;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

public class GT_MultiblockProcessingEdgesTest {
    private static GameRuntime runtime;
    @BeforeClass public static void openGame() throws Exception { runtime = new GameRuntime(); }
    @AfterClass public static void closeGame() throws Exception { if (runtime != null) runtime.close(); }
    private void run(String scenario) throws Throwable { runtime.run(getClass(), scenario, ""); }

    @Test public void splitItemStacksPreservePerMachineRules() throws Throwable { run("splitItems"); }
    @Test public void splitFluidStacksPreservePerMachineRules() throws Throwable { run("splitFluids"); }
    @Test public void controllerSlotIsOnlyUsedBySupportingMachines() throws Throwable { run("controller"); }
    @Test public void blastFurnaceHeatThresholdAndDiscounts() throws Throwable { run("heat"); }
    @Test public void chemicalCleanroomRejectionDoesNotConsumeInputs() throws Throwable { run("cleanroom"); }
    @Test public void overflowFailureCurrentlyConsumesInputs() throws Throwable { run("overflow"); }
    @Test public void fullOutputDoesNotPreventLegacyRecipeStart() throws Throwable { run("fullOutput"); }
    @Test public void ordinaryAndMultiAmpEnergyDrainBoundaries() throws Throwable { run("energyDrain"); }
    @Test public void drainDoesNotCombinePartialBuffers() throws Throwable { run("splitEnergy"); }
    @Test public void multiAmpDynamosAndMixedOutputs() throws Throwable { run("dynamos"); }
    @Test public void multiFurnaceCoilLevelsAndDiscounts() throws Throwable { run("multiFurnace"); }
    @Test public void ordinaryHatchAmperageDoesNotMultiplyLegacySearchVoltage() throws Throwable { run("ordinaryAmps"); }
    @Test public void shortAndOddRecipeDurations() throws Throwable { run("shortDurations"); }
    @Test public void chanceBoundaryUsesStrictComparison() throws Throwable { run("chances"); }
    @Test public void completeCycleDeliversOutputsOnceAndConsumesEnergyPerTick() throws Throwable { run("completeCycle"); }
    @Test public void maintenanceChangesTickEnergyCost() throws Throwable { run("maintenance"); }
    @Test public void multiAmpDynamoIntegerOverflowIsCharacterized() throws Throwable { run("dynamoOverflow"); }
    @Test public void ulvDurationOverflowDoesNotWrapNegative() throws Throwable { run("durationOverflow"); }
    @Test public void voltageLimitHandlesEmptyInvalidAndHighestTierHatches() throws Throwable { run("recipeVoltageLimit"); }

    public static void runScenario(String scenario, String ignored) throws Throwable {
        try {
            GT_MultiblockProcessingEdgesTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static void recipeVoltageLimit() {
        GT_MetaTileEntity_MultiBlockBase machine = new GT_MetaTileEntity_VacuumFreezer("test");
        Fixture f = new Fixture(machine, 1);
        for (int tier = 0; tier < GT_Values.V.length; tier++) {
            supplyHatches(f, tier, 1024, false, false);
            long voltage = GT_Values.V[tier];
            assertEquals(voltage, machine.getAverageInputVoltage());
            assertTrue(machine.isRecipeVoltageAllowed(voltage * 4L));
            assertFalse(machine.isRecipeVoltageAllowed(voltage * 4L + 1));
        }
        supplyHatches(f, 1, 16, false, false);
        // An unattached high-tier hatch must not raise the limit or dilute the average.
        machine.mEnergyHatches.add(new GT_MetaTileEntity_Hatch_Energy("invalid", 15, "test", null));
        assertEquals(32, machine.getAverageInputVoltage());
        assertTrue(machine.isRecipeVoltageAllowed(120));
        assertFalse(machine.isRecipeVoltageAllowed(480));
        machine.mEnergyHatchesMulti.clear();
        assertEquals(0, machine.getAverageInputVoltage());
        assertFalse(machine.isRecipeVoltageAllowed(1));
    }

    private interface Check { void run(RecipeFixture f) throws Exception; }
    private static void recipe(String model, int tier, Check check) throws Exception {
        RecipeFixture f = new RecipeFixture(model, tier);
        try { check.run(f); }
        finally { f.map.clear(); GregTech_API.sPostloadFinished = f.previousPostload; }
    }

    private static final String[] ITEM_MACHINES = {"VacuumFreezer", "MultiblockCentrifuge",
            "MultiblockElectrolyzer", "LargeChemicalReactor", "IndustrialPulverizer", "ImplosionCompressor",
            "ElectricBlastFurnace"};

    private static void splitItems() throws Exception {
        for (String model : ITEM_MACHINES) recipe(model, 1, f -> {
            f.register(24, 80); f.fill();
            f.machine.mInputBusses.clear();
            ItemStack first = new ItemStack(Items.iron_ingot), second = first.copy();
            f.fixture.items(first, new ItemStack(Items.stick));
            f.fixture.items(second);
            boolean combines = f.usesSharedProcessing();
            assertEquals(model, combines, f.machine.checkRecipe(null));
            assertEquals(model, combines ? 0 : 1, first.stackSize);
            assertEquals(model, combines ? 0 : 1, second.stackSize);
        });
    }

    private static void splitFluids() throws Exception {
        for (String model : new String[]{"VacuumFreezer", "MultiblockCentrifuge", "MultiblockElectrolyzer",
                "LargeChemicalReactor", "IndustrialPulverizer", "ElectricBlastFurnace", "DistillationTower"}) {
            recipe(model, 1, f -> {
                f.register(24, 80); f.fill();
                f.water.amount = 50;
                FluidStack second = f.water.copy();
                f.fixture.liquid(second);
                boolean combines = f.usesSharedProcessing() || model.equals("ElectricBlastFurnace")
                        || model.equals("DistillationTower");
                assertEquals(model, combines, f.machine.checkRecipe(null));
                assertEquals(model, combines ? 0 : 50, f.water.amount);
                assertEquals(model, combines ? 0 : 50, second.amount);
            });
        }
    }

    private static void controller() throws Exception {
        for (String model : ITEM_MACHINES) {
            recipe(model, 1, f -> {
                f.register(24, 80);
                f.fill();
                f.machine.mInputBusses.get(0).mInventory[1] = null;
                f.machine.mInventory[1] = f.stick;

                boolean supported = model.equals("LargeChemicalReactor");
                assertEquals(model, supported, f.machine.checkRecipe(null));
                assertEquals(model, supported ? 2 : 3, f.stick.stackSize);
                if (supported) {
                    f.assertConsumed();
                } else {
                    f.assertUnconsumed();
                }
            });
        }
    }

    private static void heat() throws Exception {
        int[][] rows = {{1799, 0, 0}, {1800, -384, 20}, {2699, -384, 20}, {2700, -365, 20},
                {3599, -365, 20}, {3600, -347, 10}, {5400, -313, 5}};
        for (int[] row : rows) recipe("ElectricBlastFurnace", 3, f -> {
            f.register(24, 80); f.fill();
            setField(f.machine, "mHeatingCapacity", row[0]);
            assertEquals("heat=" + row[0], row[0] >= 1800, f.machine.checkRecipe(null));
            assertEquals("heat=" + row[0], row[1], f.machine.mEUt);
            assertEquals("heat=" + row[0], row[2], f.machine.mMaxProgresstime);
            if (row[0] < 1800) f.assertUnconsumed();
            else f.assertConsumed();
        });
    }

    private static void cleanroom() throws Exception {
        for (int cleanliness : new int[]{-1, 0, 1, 10000}) recipe("LargeChemicalReactor", 1, f -> {
            f.register(24, 80); f.recipe.mSpecialValue = -200; f.fill();
            if (cleanliness >= 0) {
                GT_MetaTileEntity_Cleanroom room = new GT_MetaTileEntity_Cleanroom("test");
                room.mEfficiency = cleanliness;
                setField(f.machine, "mCleanroom", room);
            }
            assertEquals(cleanliness > 0, f.machine.checkRecipe(null));
            if (cleanliness > 0) f.assertConsumed();
            else f.assertUnconsumed();
            assertEquals(cleanliness > 0 ? 80 : 0, f.machine.mMaxProgresstime);
        });
    }

    private static void overflow() throws Exception {
        for (String model : new String[]{"VacuumFreezer", "MultiblockCentrifuge", "MultiblockElectrolyzer",
                "LargeChemicalReactor", "ImplosionCompressor", "ElectricBlastFurnace", "DistillationTower", "OilCracker"}) {
            recipe(model, 15, f -> {
                f.register(1073741824, 80); f.fill();
                if (f.usesSharedProcessing()) {
                    assertTrue(f.machine.checkRecipe(null));
                    assertEquals(-1073741824L, ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) f.machine).lEUt);
                    assertEquals(80, f.machine.mMaxProgresstime);
                    f.assertConsumed();
                    return;
                }
                assertFalse(model, f.machine.checkRecipe(null));
                assertEquals(Integer.MAX_VALUE - 1, f.machine.mEUt);
                assertEquals(Integer.MAX_VALUE - 1, f.machine.mMaxProgresstime);
                f.assertConsumed();
            });
        }
    }

    private static void fullOutput() throws Exception {
        for (String model : ITEM_MACHINES) recipe(model, 1, f -> {
            f.register(24, 80); f.fill();
            GT_MetaTileEntity_Hatch_OutputBus output = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            f.fixture.attach(output);
            for (int i = 0; i < output.mInventory.length; i++) output.mInventory[i] = new ItemStack(Items.diamond, 64);
            f.machine.mOutputBusses.add(output);
            assertTrue(model, f.machine.checkRecipe(null));
            f.assertConsumed();
            for (ItemStack stack : output.mInventory) assertEquals(64, stack.stackSize);
        });
    }

    private static void energyDrain() {
        for (boolean multi : new boolean[]{false, true}) for (int stored : new int[]{23, 24, 25}) {
            Fixture f = new Fixture(new GT_MetaTileEntity_VacuumFreezer("test"), 1);
            GT_MetaTileEntity_Hatch hatch;
            if (multi) {
                f.machine.mEnergyHatches.clear();
                GT_MetaTileEntity_Hatch_EnergyMulti energy = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 16, "test", null);
                f.attach(energy); f.machine.mEnergyHatchesMulti.add(energy); hatch = energy;
            } else hatch = f.machine.mEnergyHatches.get(0);
            hatch.setEUVar(stored);
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) f.machine).lEUt = -24;
            f.machine.mEUt = -24; f.machine.mEfficiency = 10000; f.machine.mMaxProgresstime = 80;
            assertEquals(stored >= 24, f.machine.onRunningTick(null));
            assertEquals(stored >= 24 ? stored - 24 : stored, hatch.getEUVar());
            assertEquals(stored >= 24 ? 80 : 0, f.machine.mMaxProgresstime);
        }
    }

    private static void splitEnergy() {
        Fixture f = new Fixture(new GT_LegacyProcessingTestMachine("test"), 1);
        GT_MetaTileEntity_Hatch_Energy ordinary = f.machine.mEnergyHatches.get(0);
        GT_MetaTileEntity_Hatch_EnergyMulti multi = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
        f.attach(multi); f.machine.mEnergyHatchesMulti.add(multi);
        ordinary.setEUVar(12); multi.setEUVar(12);
        assertFalse("The legacy drain requires one hatch to supply the whole tick", f.machine.drainEnergyInput(24));
        assertEquals(12, ordinary.getEUVar()); assertEquals(12, multi.getEUVar());
        multi.setEUVar(24);
        assertTrue(f.machine.drainEnergyInput(24));
        assertEquals(12, ordinary.getEUVar()); assertEquals(0, multi.getEUVar());
    }

    public static final class ExplosionRecorder extends GT_LegacyProcessingTestMachine {
        int explosions;
        public ExplosionRecorder() { super("test"); }
        @Override public void explodeMultiblock() { explosions++; }
    }

    private static void dynamos() {
        for (int tier = 0; tier <= 13; tier++) for (int amps : new int[]{1, 2, 4, 8, 16, 32, 64}) {
            long capacity = GT_Values.V[tier] * amps;
            // The legacy implementation accumulates injected EU in an int.
            if (capacity > Integer.MAX_VALUE) continue;
            for (int delta : new int[]{-1, 0, 1}) {
                ExplosionRecorder machine = new ExplosionRecorder();
                Fixture f = new Fixture(machine, 1);
                GT_MetaTileEntity_Hatch_DynamoMulti dynamo = new GT_MetaTileEntity_Hatch_DynamoMulti("test", tier, amps, "test", null);
                f.attach(dynamo); machine.mDynamoHatchesMulti.add(dynamo);
                boolean success = machine.addEnergyOutput(capacity + delta);
                assertEquals("tier=" + tier + ", amps=" + amps + ", delta=" + delta, delta <= 0, success);
                assertEquals(delta > 0 ? 1 : 0, machine.explosions);
                assertEquals(delta > 0 ? 0 : capacity + delta, dynamo.getEUVar());
            }
        }
        ExplosionRecorder machine = new ExplosionRecorder();
        Fixture f = new Fixture(machine, 1);
        GT_MetaTileEntity_Hatch_Dynamo normal = new GT_MetaTileEntity_Hatch_Dynamo("test", 1, "test", null, 1);
        GT_MetaTileEntity_Hatch_DynamoMulti multi = new GT_MetaTileEntity_Hatch_DynamoMulti("test", 2, 4, "test", null);
        f.attach(normal); f.attach(multi);
        machine.mDynamoHatches.add(normal); machine.mDynamoHatchesMulti.add(multi);
        assertFalse(machine.addEnergyOutputMultipleDynamos(100, false));
        assertEquals(1, machine.explosions);
        assertEquals(0, normal.getEUVar()); assertEquals(0, multi.getEUVar());
        assertTrue(machine.addEnergyOutputMultipleDynamos(100, true));
        assertEquals(32, normal.getEUVar()); assertEquals(68, multi.getEUVar());
    }

    private static void multiFurnace() throws Exception {
        for (int level : new int[]{1, 2, 4, 8, 16}) for (int discount : new int[]{1, 2, 4}) {
            GT_MetaTileEntity_MultiFurnace machine = new GT_MetaTileEntity_MultiFurnace("test");
            Fixture f = new Fixture(machine, 1);
            setField(machine, "mLevel", level); setField(machine, "mCostDiscount", discount);
            ItemStack ore = new ItemStack(net.minecraft.init.Blocks.iron_ore, 256);
            f.items(ore);
            if (4 * level / discount > 32) {
                assertFalse("One LV hatch cannot power this coil load", machine.checkRecipe(null));
                assertEquals(256, ore.stackSize);
                continue;
            }
            assertTrue(machine.checkRecipe(null));
            assertEquals(256 - 8 * level, ore.stackSize);
            assertEquals(8 * level, machine.mOutputItems.length);
            for (ItemStack output : machine.mOutputItems) {
                assertSame(Items.iron_ingot, output.getItem()); assertEquals(1, output.stackSize);
            }
            assertEquals(-4 * level / discount, machine.mEUt);
            assertEquals(512, machine.mMaxProgresstime);
        }
        GT_MetaTileEntity_MultiFurnace machine = new GT_MetaTileEntity_MultiFurnace("test");
        Fixture f = new Fixture(machine, 1);
        setField(machine, "mLevel", 1);
        f.items(new ItemStack(Items.stick));
        assertFalse("Non-smeltable inputs must not start a cycle", machine.checkRecipe(null));
        assertEquals(0, machine.mMaxProgresstime);
    }

    private static void ordinaryAmps() {
        for (int amps : new int[]{1, 2, 4, 16, 64}) {
            Fixture f = new Fixture(new GT_MetaTileEntity_VacuumFreezer("test"), 1);
            f.machine.mEnergyHatches.get(0).mAmpers = amps;
            assertEquals(32, f.machine.getMaxInputVoltage());
            GT_MetaTileEntity_Hatch_Energy second = new GT_MetaTileEntity_Hatch_Energy("test", 2, "test", null);
            f.attach(second); f.machine.mEnergyHatches.add(second);
            assertEquals(160, f.machine.getMaxInputVoltage());
        }
    }

    private static void shortDurations() throws Exception {
        int[][] standard = {{1, 12, 1}, {2, 96, 1}, {3, 96, 1}, {79, 96, 39}, {80, 96, 40}, {81, 96, 40}};
        int[][] pulverizer = {{1, 96, 1}, {2, 96, 1}, {3, 96, 1}, {79, 96, 19}, {80, 96, 20}, {81, 96, 20}};
        for (String model : ITEM_MACHINES) for (int[] row : model.equals("IndustrialPulverizer") ? pulverizer : standard) {
            recipe(model, 2, f -> {
                f.register(24, row[0]); f.fill();
                assertTrue(model + " duration=" + row[0], f.machine.checkRecipe(null));
                assertEquals(f.usesSharedProcessing() ? -96 : -row[1], f.machine.mEUt);
                assertEquals(row[2], f.machine.mMaxProgresstime);
            });
        }
    }

    private static void chances() throws Exception {
        for (String model : new String[]{"MultiblockCentrifuge", "MultiblockElectrolyzer", "IndustrialPulverizer", "ImplosionCompressor"})
            for (int chance : new int[]{1, 1359, 1360, 1361, 10000}) recipe(model, 1, f -> {
                f.register(24, 80); f.fill();
                // Java Random seed 0 produces 1360 for the first nextInt(10000).
                f.recipe.mChances[0] = chance;
                f.fixture.world.rand.setSeed(0);
                assertTrue(f.machine.checkRecipe(null));
                assertEquals(model + " chance=" + chance, chance > 1360, f.machine.mOutputItems[0] != null);
            });
    }

    private static void completeCycle() throws Exception {
        recipe("VacuumFreezer", 1, f -> {
            f.register(24, 3); f.fill();
            GT_MetaTileEntity_Hatch_OutputBus bus = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            f.fixture.attach(bus); f.machine.mOutputBusses.add(bus);
            GT_MetaTileEntity_Hatch_Output liquid = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);
            f.fixture.attach(liquid); f.machine.mOutputHatches.add(liquid);
            assertTrue(f.machine.checkRecipe(null));
            f.machine.mMachine = true; f.machine.mStartUpCheck = -1; f.machine.mUpdate = 100;
            f.fixture.tile.disableWorking(); // Finish the in-flight batch without automatically starting another.
            GT_MetaTileEntity_Hatch_Energy energy = f.machine.mEnergyHatches.get(0);
            energy.setEUVar(1000);
            for (int tick = 1; tick <= 3; tick++) {
                f.machine.onPostTick(f.fixture.tile, tick);
                assertEquals(1000 - tick * 24, energy.getEUVar());
                if (tick < 3) { assertNull(bus.mInventory[0]); assertNull(liquid.mFluid); }
            }
            assertEquals(0, f.machine.mMaxProgresstime); assertEquals(0, f.machine.mProgresstime);
            assertSame(Items.gold_ingot, bus.mInventory[0].getItem()); assertEquals(3, bus.mInventory[0].stackSize);
            assertEquals(75, liquid.mFluid.amount);
            f.machine.onPostTick(f.fixture.tile, 4);
            assertEquals(3, bus.mInventory[0].stackSize); assertEquals(75, liquid.mFluid.amount);
            assertEquals(928, energy.getEUVar());
            assertEquals(4, f.iron.stackSize);
        });
    }

    private static void maintenance() throws Exception {
        for (int missing = 0; missing <= 6; missing++) {
            final int count = missing;
            recipe("VacuumFreezer", 1, f -> {
                String[] tools = {"mWrench", "mScrewdriver", "mSoftHammer", "mHardHammer", "mSolderingTool", "mCrowbar"};
                for (int i = 0; i < count; i++) setField(f.machine, tools[i], false);
                f.register(24, 80); f.fill();
                assertTrue(f.machine.checkRecipe(null));
                int efficiency = 10000 - 1000 * count;
                assertEquals(efficiency, f.machine.mEfficiency);
                f.machine.mEnergyHatches.get(0).setEUVar(1000);
                assertTrue(f.machine.onRunningTick(null));
                assertEquals(1000 - 240000 / efficiency, f.machine.mEnergyHatches.get(0).getEUVar());
            });
        }
    }

    private static void dynamoOverflow() {
        long[][] rows = {{2147483647L, 1}, {2147483648L, 0}, {4294967295L, 0},
                {4294967296L, 0}, {4294967297L, 1}};
        for (long[] row : rows) {
            ExplosionRecorder machine = new ExplosionRecorder();
            Fixture f = new Fixture(machine, 1);
            GT_MetaTileEntity_Hatch_DynamoMulti dynamo = new GT_MetaTileEntity_Hatch_DynamoMulti("test", 13, 16, "test", null);
            f.attach(dynamo); machine.mDynamoHatchesMulti.add(dynamo);
            // Current bug: the return flag wraps with the int counter although EU was delivered.
            assertEquals("requested=" + row[0], row[1] == 1, machine.addEnergyOutput(row[0]));
            assertEquals(row[0], dynamo.getEUVar());
            assertEquals(0, machine.explosions);
        }
    }

    private static void durationOverflow() throws Exception {
        for (String model : new String[]{"VacuumFreezer", "ElectricBlastFurnace"})
            for (int duration : new int[]{1073741823, 1073741824, Integer.MAX_VALUE}) recipe(model, 0, f -> {
                f.register(4, duration); f.fill();
                if (f.usesSharedProcessing()) {
                    assertTrue(f.machine.checkRecipe(null));
                    assertEquals(-4, f.machine.mEUt);
                    assertEquals(duration, f.machine.mMaxProgresstime);
                    f.assertConsumed();
                    return;
                }
                boolean success = duration == 1073741823;
                assertEquals(success, f.machine.checkRecipe(null));
                assertEquals(success ? -1 : Integer.MAX_VALUE - 1, f.machine.mEUt);
                assertEquals(Integer.MAX_VALUE - 1, f.machine.mMaxProgresstime);
                f.assertConsumed();
            });
    }
}
