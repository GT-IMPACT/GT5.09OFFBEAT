package gregtech.api.metatileentity.implementations;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Cleanroom;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_HeatExchanger;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

public class GT_ProcessingLifecycleTest {
    private static GameRuntime runtime;

    @BeforeClass
    public static void openGame() throws Exception {
        runtime = new GameRuntime();
    }

    @AfterClass
    public static void closeGame() throws Exception {
        runtime.close();
    }

    @Test
    public void waitingTicksDoNotRunMaintenancePollutionOrConsumePower() throws Throwable {
        runtime.run(getClass(), "waitingTick", "");
    }

    @Test
    public void completedProductsCanLeaveAfterMaintenanceToolsBreak() throws Throwable {
        runtime.run(getClass(), "brokenMaintenance", "");
    }

    @Test
    public void abortingAnUnfinishedCycleClearsItemAndFluidProductsFromSave() throws Throwable {
        runtime.run(getClass(), "abort", "");
    }

    @Test
    public void stoppingWhileWaitingDisablesRestartAndRetainsSavedProducts() throws Throwable {
        runtime.run(getClass(), "stopWaiting", "");
    }

    @Test
    public void placementEnablesOnlySupportedProtectionWithoutOverwritingSavedModes() throws Throwable {
        runtime.run(getClass(), "placementProtection", "");
    }

    public static void runScenario(String scenario, String ignored) throws Throwable {
        try {
            GT_ProcessingLifecycleTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        } finally {
            GT_Recipe.GT_Recipe_Map.sVacuumRecipes.clear();
        }
    }

    private static void waitingTick() {
        Scenario s = new Scenario();
        assertTrue(s.machine.checkRecipe(null));
        s.tick();
        assertEquals(1, s.machine.maintenanceChecks);
        assertEquals(1, s.machine.pollutionCalls);
        assertTrue(s.fixture.tile.isActive());
        s.blockAndFinish();
        long energy = s.machine.mEnergyHatches.get(0).getEUVar();
        for (int i = 0; i < 5; i++) {
            s.tick();
        }
        assertEquals(1, s.machine.maintenanceChecks);
        assertEquals(1, s.machine.pollutionCalls);
        assertEquals(energy, s.machine.mEnergyHatches.get(0).getEUVar());
        assertFalse(s.fixture.tile.isActive());
        assertTrue(s.machine.isWaitingForOutput());
        s.output.mFluid = null;
        s.tick();
        assertFalse(s.machine.isWaitingForOutput());
        assertEquals(75, s.output.mFluid.amount);
        s.tick();
        assertEquals(75, s.output.mFluid.amount);
    }

    private static void brokenMaintenance() {
        Scenario s = new Scenario();
        ItemStack input = s.machine.mInputBusses.get(0).mInventory[0];
        input.stackSize = 2;
        assertTrue(s.machine.checkRecipe(null));
        s.blockAndFinish();
        s.fixture.tile.enableWorking();
        s.machine.mWrench = s.machine.mScrewdriver = s.machine.mSoftHammer = s.machine.mHardHammer = s.machine.mSolderingTool = s.machine.mCrowbar = false;
        s.output.mFluid = null;
        s.tick();
        assertFalse(s.machine.isWaitingForOutput());
        assertEquals(75, s.output.mFluid.amount);
        assertEquals(0, s.machine.maintenanceChecks);
        assertEquals(0, s.machine.pollutionCalls);
        assertEquals(1, input.stackSize);
        assertEquals(0, s.machine.mMaxProgresstime);
    }

    private static void abort() {
        Scenario s = new Scenario();
        assertTrue(s.machine.checkRecipe(null));
        assertNotNull(s.machine.mOutputFluids);
        s.machine.stopMachine();
        assertNull(s.machine.mOutputItems);
        assertNull(s.machine.mOutputFluids);
        assertEquals(CheckRecipeResult.NONE, s.machine.getProcessingResult());
        assertEquals(0, s.machine.mMaxProgresstime);
        assertEquals(0, s.machine.lEUt);
        NBTTagCompound saved = s.save();
        s.machine.loadNBTData(saved);
        assertEquals(0, s.machine.mOutputItems.length);
        assertEquals(0, s.machine.mOutputFluids.length);
        s.tick();
        assertNull(s.output.mFluid);
    }

    private static void stopWaiting() {
        Scenario s = new Scenario();
        assertTrue(s.machine.checkRecipe(null));
        s.blockAndFinish();
        s.fixture.tile.enableWorking();
        s.machine.stopMachine();
        assertFalse(s.fixture.tile.isAllowedToWork());
        assertTrue(s.machine.isWaitingForOutput());
        s.machine.loadNBTData(s.save());
        s.machine.mMachine = false;
        s.tick();
        assertEquals(75, s.machine.mOutputFluids[0].amount);
        s.machine.mMachine = true;
        s.output.mFluid = null;
        s.tick();
        assertEquals(75, s.output.mFluid.amount);
        assertFalse(s.fixture.tile.isAllowedToWork());
        assertEquals(0, s.machine.mMaxProgresstime);
    }

    private static void placementProtection() {
        TestFreezer freezer = new TestFreezer();
        Fixture fixture = new Fixture(freezer, 1);
        freezer.initDefaultModes(null);
        assertEquals(ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS, freezer.getProcessingModes());

        NBTTagCompound itemTag = new NBTTagCompound();
        itemTag.setTag("GT.CraftingComponents", new NBTTagCompound());
        GT_MetaTileEntity_HeatExchanger exchanger = new GT_MetaTileEntity_HeatExchanger("test");
        fixture.attach(exchanger);
        exchanger.loadNBTData(itemTag);
        exchanger.initDefaultModes(itemTag);
        assertEquals(ProcessingLogic.PROTECT_FLUIDS, exchanger.getProcessingModes());

        GT_MetaTileEntity_Cleanroom cleanroom = new GT_MetaTileEntity_Cleanroom("test");
        cleanroom.initDefaultModes(null);
        assertEquals(0, cleanroom.getProcessingModes());

        NBTTagCompound saved = new NBTTagCompound();
        freezer.setProcessingModes(0);
        freezer.saveNBTData(saved);
        TestFreezer restored = new TestFreezer();
        fixture.attach(restored);
        restored.loadNBTData(saved);
        restored.initDefaultModes(saved);
        assertEquals(0, restored.getProcessingModes());
    }

    private static final class TestFreezer extends GT_MetaTileEntity_VacuumFreezer {
        int maintenanceChecks;
        int pollutionCalls;

        TestFreezer() {
            super("test");
        }

        @Override
        public boolean doRandomMaintenanceDamage() {
            maintenanceChecks++;
            return true;
        }

        @Override
        public boolean polluteEnvironment(int pollution) {
            pollutionCalls++;
            return true;
        }
    }

    private static final class Scenario {
        final TestFreezer machine = new TestFreezer();
        final Fixture fixture = new Fixture(machine, 1);
        final GT_MetaTileEntity_Hatch_Output output = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);

        Scenario() {
            fixture.tile.disableWorking();
            fixture.attach(output);
            machine.mOutputHatches.add(output);
            machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            fixture.items(new ItemStack(Items.iron_ingot));
            assertNotNull(machine.getRecipeMap().addRecipe(false, new ItemStack[] { new ItemStack(Items.iron_ingot) },
                    new ItemStack[] { new ItemStack(Items.gold_ingot) }, null, null, null,
                    new FluidStack[] { new FluidStack(fluid("lifecycle.product"), 75) }, 20, 24, 0));
            machine.mMachine = true;
        }

        void blockAndFinish() {
            output.mFluid = new FluidStack(fluid("lifecycle.product"), output.getCapacity());
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
            assertTrue(machine.isWaitingForOutput());
        }

        void tick() {
            machine.mUpdate = -1;
            machine.mStartUpCheck = -1;
            machine.onPostTick(fixture.tile, 1);
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }
    }
}
