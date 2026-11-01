package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DieselEngine;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DieselEngine2;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GT_CombustionProcessingTest {
    private static GameRuntime runtime;
    private final String model;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] models() {
        return new Object[] { "DieselEngine", "DieselEngine2" };
    }

    public GT_CombustionProcessingTest(String model) {
        this.model = model;
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

    @Test
    public void missingAndInsufficientLubricantNeverSpendFuelOrOxygen() throws Throwable {
        run("lubricant");
    }

    @Test
    public void boostTransitionsRetainExistingFuelTimingAcrossReload() throws Throwable {
        run("boost");
    }

    @Test
    public void lubricantPeriodAndWarmupSurviveReload() throws Throwable {
        run("warmup");
    }

    @Test
    public void ordinaryAndMultiAmpDynamosAcrossAllTiers() throws Throwable {
        run("dynamos");
    }

    @Test
    public void underpoweredDynamosStillExplode() throws Throwable {
        run("overload");
    }

    @Test
    public void mixedDynamosShareTheGeneratedPower() throws Throwable {
        run("mixedDynamos");
    }

    @Test
    public void legacyCycleCompletesWithoutConsumingAgain() throws Throwable {
        run("legacy");
    }

    @Test
    public void zeroValueFuelAndInvalidInputAreRejected() throws Throwable {
        run("invalid");
    }

    @Test
    public void guiHasOnlyPowerAndNeiWithNoControllerSlot() throws Throwable {
        run("controls");
    }

    public static void runScenario(String scenario, String model) throws Throwable {
        Materials.Oxygen.mGas = fluid("engine_test_oxygen");
        Materials.Lubricant.mFluid = fluid("engine_test_lubricant");
        try {
            GT_CombustionProcessingTest.class.getDeclaredMethod(scenario, String.class).invoke(null, model);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void lubricant(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            for (int amount : new int[] { 0, s.lubeAmount }) {
                s.lube.mFluid = amount == 0 ? null : Materials.Lubricant.getFluid(amount);
                assertFalse(s.machine.checkRecipe(null));
                assertEquals(100000, s.fuel.getFluidAmount());
                assertEquals(1000, s.oxygen.getFluidAmount());
                assertEquals(amount, s.lube.getFluidAmount());
                assertEquals(0, s.machine.mMaxProgresstime);
                assertEquals(CheckRecipeResult.MACHINE_CONDITION, s.machine.getProcessingResult());
            }
            s.lube.mFluid = Materials.Lubricant.getFluid(s.lubeAmount + 1);
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(0, s.lube.getFluidAmount());
            assertEquals(100000 - s.power / 100, s.fuel.getFluidAmount());
        }
    }

    private static void boost(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(100000 - s.power / 100, s.fuel.getFluidAmount());
            assertEquals(30000, s.machine.getMaxEfficiency(null));
            s.machine.loadNBTData(s.save());
            s.finish();
            int before = s.fuel.getFluidAmount();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(before - s.boostedPower / 100, s.fuel.getFluidAmount());
            s.finish();
            s.oxygen.mFluid = null;
            before = s.fuel.getFluidAmount();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(before - s.boostedPower / 100, s.fuel.getFluidAmount());
            assertEquals(10000, s.machine.getMaxEfficiency(null));
            s.finish();
            before = s.fuel.getFluidAmount();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(before - s.power / 100, s.fuel.getFluidAmount());
        }
    }

    private static void warmup(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.mEfficiency = 0;
            s.oxygen.mFluid = null;
            s.machine.mRuntime = s.period - 1;
            int lubricant = s.lube.getFluidAmount();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(0, s.machine.mEfficiency);
            assertEquals(0, s.machine.lEUt);
            assertTrue(s.machine.onRunningTick(null));
            assertEquals(lubricant, s.lube.getFluidAmount());
            s.machine.loadNBTData(s.save());
            assertEquals(s.period - 1, s.machine.mRuntime);
            s.finish();
            assertEquals(s.warmup, s.machine.mEfficiency);
            s.machine.mRuntime++;
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(lubricant - s.lubeAmount, s.lube.getFluidAmount());
        }
    }

    private static void dynamos(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.mEfficiency = 30000;
            long output = s.power * 3L;
            for (int tier = 0; tier < GT_Values.V.length; tier++) {
                for (int amps : new int[] { 1, 4, 64, 1024 }) {
                    if (GT_Values.V[tier] * amps < output) {
                        continue;
                    }
                    s.machine.mDynamoHatches.clear();
                    s.machine.mDynamoHatchesMulti.clear();
                    GT_MetaTileEntity_Hatch hatch;
                    if (amps == 1) {
                        GT_MetaTileEntity_Hatch_Dynamo ordinary = new GT_MetaTileEntity_Hatch_Dynamo("test", tier, "",
                                null);
                        s.machine.mDynamoHatches.add(ordinary);
                        hatch = ordinary;
                    } else {
                        GT_MetaTileEntity_Hatch_DynamoMulti multi = new GT_MetaTileEntity_Hatch_DynamoMulti("test",
                                tier, amps, "", null);
                        s.machine.mDynamoHatchesMulti.add(multi);
                        hatch = multi;
                    }
                    s.fixture.attach(hatch);
                    assertTrue(s.machine.checkRecipe(null));
                    assertTrue(s.machine.onRunningTick(null));
                    assertEquals(model + " tier=" + tier + " amps=" + amps, output, hatch.getEUVar());
                    s.finish();
                }
            }
        }
    }

    private static void overload(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.mEfficiency = 30000;
            for (int tier = 0; tier <= 4; tier++) {
                for (int amps : new int[] { 1, 4, 64 }) {
                    if (GT_Values.V[tier] * amps >= s.power * 3L) {
                        continue;
                    }
                    s.machine.mDynamoHatchesMulti.clear();
                    GT_MetaTileEntity_Hatch_DynamoMulti hatch = new GT_MetaTileEntity_Hatch_DynamoMulti("test", tier,
                            amps, "", null);
                    s.fixture.attach(hatch);
                    s.machine.mDynamoHatchesMulti.add(hatch);
                    s.exploded = false;
                    assertTrue(s.machine.checkRecipe(null));
                    s.machine.onRunningTick(null);
                    assertTrue("Insufficient dynamo throughput must still explode", s.exploded);
                    assertEquals(0, hatch.getEUVar());
                    s.finish();
                }
            }
        }
    }

    private static void mixedDynamos(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.mEfficiency = 30000;
            GT_MetaTileEntity_Hatch_Dynamo ordinary = new GT_MetaTileEntity_Hatch_Dynamo("test", 4, "", null);
            GT_MetaTileEntity_Hatch_DynamoMulti multi = new GT_MetaTileEntity_Hatch_DynamoMulti("test", 3, 64, "", null);
            s.fixture.attach(ordinary);
            s.fixture.attach(multi);
            s.machine.mDynamoHatches.add(ordinary);
            s.machine.mDynamoHatchesMulti.add(multi);
            assertTrue(s.machine.checkRecipe(null));
            assertTrue(s.machine.onRunningTick(null));
            assertFalse(s.exploded);
            assertEquals(2048, ordinary.getEUVar());
            assertEquals(s.power * 3L - 2048, multi.getEUVar());
        }
    }

    private static void legacy(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            GT_LegacyProcessingTestMachine legacy = new GT_LegacyProcessingTestMachine("legacy");
            new Fixture(legacy, 1);
            legacy.mEUt = s.power;
            legacy.mEfficiency = 30000;
            legacy.mEfficiencyIncrease = s.warmup;
            legacy.mMaxProgresstime = 1;
            NBTTagCompound tag = new NBTTagCompound();
            legacy.saveNBTData(tag);
            s.machine.loadNBTData(tag);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(100000, s.fuel.getFluidAmount());
            assertEquals(s.power, s.machine.lEUt);
            assertEquals(30000, s.machine.getMaxEfficiency(null));
            s.finish();
            assertEquals(100000, s.fuel.getFluidAmount());
        }
    }

    private static void invalid(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.recipe.mSpecialValue = 0;
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(100000, s.fuel.getFluidAmount());
            assertEquals(1000, s.oxygen.getFluidAmount());
            s.recipe.mSpecialValue = 100;
            s.fuel.mFluid = new FluidStack(fluid("engine_not_fuel"), 1000);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(1000, s.fuel.getFluidAmount());
        }
    }

    private static void controls(String model) throws Exception {
        try (Scenario s = new Scenario(model)) {
            s.machine.setProcessingModes(ProcessingLogic.ALL_MODES);
            s.machine.loadNBTData(s.save());
            assertEquals(0, s.machine.getProcessingModes());
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(s.fixture.world);
            GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory, s.fixture.tile);
            assertEquals(0, gui.getSlotCount());
            assertEquals(42, gui.inventorySlots.size());
            assertSame(GT_Recipe.GT_Recipe_Map.sDieselFuels, gui.getNEIRecipeMap());
            for (int index = 0; index < GT_Container_ProcessingMachine.CONTROL_COUNT; index++) {
                boolean visible = index == GT_Container_ProcessingMachine.POWER_SLOT
                        || index == GT_Container_ProcessingMachine.NEI_SLOT;
                assertEquals(visible, gui.isControlVisible(index));
                assertEquals(visible, gui.getSlot(index).xDisplayPosition >= 0);
            }
        }
    }

    private static final class Scenario implements AutoCloseable {
        final GT_MetaTileEntity_DieselEngine machine;
        final Fixture fixture;
        final GT_MetaTileEntity_Hatch_Input fuel;
        final GT_MetaTileEntity_Hatch_Input oxygen;
        final GT_MetaTileEntity_Hatch_Input lube;
        final GT_Recipe recipe;
        final int power;
        final int boostedPower;
        final int lubeAmount;
        final int period;
        final int warmup;
        boolean exploded;

        Scenario(String model) {
            boolean mk2 = model.endsWith("2");
            machine = mk2 ? new GT_MetaTileEntity_DieselEngine2("test") {
                @Override
                public void explodeMultiblock() {
                    exploded = true;
                }
            } : new GT_MetaTileEntity_DieselEngine("test") {
                @Override
                public void explodeMultiblock() {
                    exploded = true;
                }
            };
            power = mk2 ? 8192 : 2048;
            boostedPower = mk2 ? 10240 : 4096;
            lubeAmount = mk2 ? 2 : 1;
            period = mk2 ? 80 : 72;
            warmup = mk2 ? 40 : 15;
            fixture = new Fixture(machine, 1);
            fixture.tile.disableWorking();
            machine.mEfficiency = 10000;
            Fluid diesel = fluid("combustion_test_diesel");
            ItemStack container = new ItemStack(Items.apple);
            GT_Utility.addFluidContainerData(new FluidContainerRegistry.FluidContainerData(new FluidStack(diesel, 1000),
                    container, new ItemStack(Items.bucket)));
            recipe = GT_Recipe.GT_Recipe_Map.sDieselFuels.addRecipe(false, new ItemStack[] { container }, null, null,
                    null, null, null, 0, 0, 100);
            assertNotNull(recipe);
            fuel = fixture.liquid(new FluidStack(diesel, 100000));
            oxygen = fixture.liquid(Materials.Oxygen.getGas(1000));
            lube = fixture.liquid(Materials.Lubricant.getFluid(1000));
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }

        void finish() {
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
        }

        @Override
        public void close() {
            GT_Recipe.GT_Recipe_Map.sDieselFuels.clear();
        }
    }
}
