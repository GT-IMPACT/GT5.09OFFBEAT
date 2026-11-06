package gregtech.api.metatileentity.implementations;

import gregtech.api.logic.FuelProcessingLogic;
import gregtech.api.logic.IProcessingFluidOutput;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.fluid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GT_FuelProcessingTest {

    private static GT_ProcessingTestSupport.GameRuntime runtime;

    @BeforeClass
    public static void openGame() throws Exception {
        runtime = new GT_ProcessingTestSupport.GameRuntime();
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
            GT_FuelProcessingTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    @Test
    public void missingAuxiliaryFluidDoesNotBurnFuel() throws Throwable {
        run("missingAuxiliary");
    }

    @Test
    public void fullProtectedOutputDoesNotBurnEitherFluid() throws Throwable {
        run("fullOutput");
    }

    @Test
    public void successfulBurnConsumesFuelAndOxygenExactlyOnce() throws Throwable {
        run("successfulBurn");
    }

    @Test
    public void generationOverflowDoesNotConsumeAnyInput() throws Throwable {
        run("generationOverflow");
    }

    @Test
    public void changedInputRejectsTheBurnWithoutConsumingOtherInputs() throws Throwable {
        run("changedInput");
    }

    @Test
    public void recipeFuelConsumesItemsAndFluidsTogetherWithGuaranteedProducts() throws Throwable {
        run("recipeFuel");
    }

    @Test
    public void customTankSimulationReservesSpaceAcrossProductsWithoutWriting() throws Throwable {
        run("customTank");
    }

    private static void missingAuxiliary() {
        try (Burn burn = new Burn()) {
            burn.oxygen.amount = 49;
            ProcessingLogic.Result result = burn.process(0, 10000, true);
            assertEquals(CheckRecipeResult.MACHINE_CONDITION, result.status);
            assertEquals(200, burn.fuel.amount);
            assertEquals(49, burn.oxygen.amount);
            assertEquals(0, burn.logic.getConsumption());
        }
    }

    private static void fullOutput() {
        try (Burn burn = new Burn()) {
            ProcessingLogic.Result result = burn.process(ProcessingLogic.PROTECT_FLUIDS, 10000, true);
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, result.status);
            assertEquals(200, burn.fuel.amount);
            assertEquals(100, burn.oxygen.amount);
        }
    }

    private static void successfulBurn() {
        try (Burn burn = new Burn()) {
            ProcessingLogic.Result result = burn.process(0, 10000, true);
            assertEquals(CheckRecipeResult.SUCCESS, result.status);
            assertEquals(-128, result.eut);
            assertEquals(20, result.duration);
            assertEquals(75, result.fluids[0].amount);
            assertEquals(100, burn.fuel.amount);
            assertEquals(50, burn.oxygen.amount);
            assertEquals(100, burn.logic.getConsumption());
            assertEquals(1, burn.logic.getCurrentRecipe().mInputs[0].stackSize);
        }
    }

    private static void generationOverflow() {
        try (Burn burn = new Burn()) {
            burn.generation = Long.MAX_VALUE;
            ProcessingLogic.Result result = burn.process(0, Integer.MAX_VALUE, true);
            assertEquals(CheckRecipeResult.POWER_OVERFLOW, result.status);
            assertEquals(200, burn.fuel.amount);
            assertEquals(100, burn.oxygen.amount);
        }
    }

    private static void changedInput() {
        try (Burn burn = new Burn()) {
            ProcessingLogic.Result result = burn.process(0, 10000, false);
            assertEquals(CheckRecipeResult.INPUT_CHANGED, result.status);
            assertEquals(200, burn.fuel.amount);
            assertEquals(100, burn.oxygen.amount);
        }
    }

    private static void recipeFuel() {
        try (Burn burn = new Burn()) {
            ItemStack item = new ItemStack(Items.iron_ingot, 2);
            GT_Recipe recipe = burn.map.mRecipeList.iterator().next();
            recipe.mFluidInputs = new FluidStack[] { burn.fuel.copy() };
            recipe.mFluidInputs[0].amount = 100;
            recipe.mChances[0] = 0;
            FuelProcessingLogic logic = new FuelProcessingLogic(() -> burn.map,
                    fuel -> new FuelProcessingLogic.FuelCycle(fuel, 2097152, 36000));
            ProcessingLogic.Result result = logic.process(new ProcessingLogic.Request(
                    Collections.singletonList(new ProcessingInputs(Collections.singletonList(item),
                            Collections.singletonList(burn.fuel))), 0, 0, 0, burn.outputs, bound -> 9999));
            assertEquals(CheckRecipeResult.SUCCESS, result.status);
            assertEquals(1, item.stackSize);
            assertEquals(100, burn.fuel.amount);
            assertEquals(1, result.items[0].stackSize);
            assertEquals(Items.gold_ingot, result.items[0].getItem());
            assertEquals(-2097152L, result.eut);
        }
    }

    private static void customTank() {
        try (Burn burn = new Burn()) {
            LocalTank tank = new LocalTank();
            ProcessingOutputs outputs = new ProcessingOutputs(burn.machine).addFluidOutput(tank);
            FluidStack first = product(60);
            FluidStack second = product(60);
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL,
                    outputs.canFit(new ItemStack[0], new FluidStack[] { first, second }, false, true));
            assertEquals(0, tank.amount);
            assertEquals(60, first.amount);
            second.amount = 40;
            assertEquals(CheckRecipeResult.SUCCESS,
                    outputs.canFit(new ItemStack[0], new FluidStack[] { first, second }, false, true));
            assertEquals(0, tank.amount);
            assertTrue(outputs.deliver(null, new FluidStack[] { first, second }, false, true));
            assertEquals(100, tank.amount);
            FluidStack pending = product(50);
            assertFalse(outputs.deliver(null, new FluidStack[] { pending }, false, true));
            assertEquals(50, pending.amount);
            tank.amount = 70;
            assertFalse(outputs.deliver(null, new FluidStack[] { pending }, false, true));
            assertEquals(20, pending.amount);
            tank.amount = 0;
            assertTrue(outputs.deliver(null, new FluidStack[] { pending }, false, true));
            assertEquals(20, tank.amount);
            assertEquals(0, pending.amount);
        }
    }

    private static FluidStack product(int amount) {
        return new FluidStack(fluid("fuel_test_product"), amount);
    }

    private static final class Burn implements AutoCloseable {

        private final GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("fuel-test");
        private final GT_Recipe.GT_Recipe_Map map = machine.getRecipeMap();
        private final ProcessingOutputs outputs = new ProcessingOutputs(machine);
        private final FluidStack fuel = new FluidStack(fluid("fuel_test_input"), 200);
        private final FluidStack oxygen = new FluidStack(fluid("fuel_test_oxygen"), 100);
        private long generation = 128;
        private final FuelProcessingLogic logic;

        private Burn() {
            new GT_ProcessingTestSupport.Fixture(machine, 1);
            map.addRecipe(false, new ItemStack[] { new ItemStack(Items.iron_ingot) },
                    new ItemStack[] { new ItemStack(Items.gold_ingot) }, null, new int[] { 10000 },
                    null, null, 20, 24, 0);
            logic = new FuelProcessingLogic(() -> map, recipe -> new FuelProcessingLogic.FuelCycle(
                    generation, 20, new FluidStack(fuel.getFluid(), 100),
                    new FluidStack[] { new FluidStack(oxygen.getFluid(), 50) },
                    new FluidStack[] { product(75) }));
        }

        private ProcessingLogic.Result process(int modes, int efficiency, boolean unchanged) {
            ProcessingInputs inputs = new ProcessingInputs(Collections.emptyList(), Arrays.asList(fuel, oxygen),
                    () -> unchanged);
            return logic.process(new ProcessingLogic.Request(Collections.singletonList(inputs), 0, 0, modes,
                    outputs, bound -> 0, efficiency));
        }

        @Override
        public void close() {
            map.clear();
        }
    }

    private static final class LocalTank implements IProcessingFluidOutput {

        private int amount;

        @Override
        public FluidStack getStoredFluid() {
            return amount == 0 ? null : product(amount);
        }

        @Override
        public int getCapacity() {
            return 100;
        }

        @Override
        public boolean accepts(FluidStack fluid) {
            return fluid.isFluidEqual(product(1));
        }

        @Override
        public boolean isRestricted() {
            return false;
        }

        @Override
        public int fill(FluidStack fluid, boolean execute) {
            int accepted = Math.min(fluid.amount, 100 - amount);
            if (execute) {
                amount += accepted;
            }
            return accepted;
        }
    }
}
