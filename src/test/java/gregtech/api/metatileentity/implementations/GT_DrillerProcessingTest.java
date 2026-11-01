package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.ItemList;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.multi.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
public class GT_DrillerProcessingTest {
    private static GameRuntime runtime;
    private final int model;

    public GT_DrillerProcessingTest(int model) {
        this.model = model;
    }

    @Parameterized.Parameters(name = "driller {0}")
    public static Object[] models() {
        return new Object[] { 0, 1, 2, 3, 4 };
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
    public void loweringAutoloadsPipesAndPerformsExactlyOneWorldOperation() throws Throwable {
        run("lowering");
    }

    @Test
    public void allTiersAndMultiAmpHatchesKeepNominalVoltageCalculations() throws Throwable {
        run("powerMatrix");
    }

    @Test
    public void mixedHatchesCheckPowerSeparatelyFromNominalTier() throws Throwable {
        run("mixedHatches");
    }

    @Test
    public void startupReserveAndChangedInputsNeverSpendPipesOnRefusal() throws Throwable {
        run("refusals");
    }

    @Test
    public void protectedReturnedPipesWaitAndSurviveReloadWithoutDuplicates() throws Throwable {
        run("pipeProtection");
    }

    @Test
    public void brokenPipeColumnIsRepairedOnlyWhenAnOperationCommits() throws Throwable {
        run("brokenColumn");
    }

    @Test
    public void activeAndLegacyCyclesResumeWithoutRepeatingWorldChanges() throws Throwable {
        run("reload");
    }

    @Test
    public void guiOnlyOffersSupportedControlsAndKeepsThePipeSlot() throws Throwable {
        run("gui");
    }

    @Test
    public void worldProductionPreservesYieldAndConsumption() throws Throwable {
        run("production");
    }

    @Test
    public void unavailableResourcesAndProtectedOutputsRemainUntouched() throws Throwable {
        run("productionRefusals");
    }

    @Test
    public void productionWaitsOrResumesFromItsSavedPosition() throws Throwable {
        run("productionReload");
    }

    public static void runScenario(String scenario, String model) throws Throwable {
        ic2.core.Ic2Items.miningPipe = new ItemStack(Blocks.fence);
        ic2.core.Ic2Items.miningPipeTip = new ItemStack(Blocks.nether_brick_fence);
        boolean preload = GregTech_API.sPreloadStarted;
        GregTech_API.sPreloadStarted = true;
        try {
            Class.forName(GT_MetaTileEntity_DrillerBase.class.getName());
        } finally {
            GregTech_API.sPreloadStarted = preload;
        }
        ItemList.Circuit_Integrated.set(new ItemStack(Items.comparator));
        Materials.Concrete.mStandardMoltenFluid = fluid("test.driller.concrete");
        GregTech_API.sBlockConcretes = Blocks.stained_hardened_clay;
        try {
            GT_DrillerProcessingTest.class.getDeclaredMethod(scenario, int.class).invoke(null, Integer.parseInt(model));
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void lowering(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        ItemStack pipes = new ItemStack(Blocks.fence, 10);
        s.fixture.items(pipes);
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(0, pipes.stackSize);
        assertEquals(9, s.machine.getStackInSlot(1).stackSize);
        assertSame(Blocks.nether_brick_fence, s.fixture.world.getBlock(0, 63, 1));
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(9, s.machine.getStackInSlot(1).stackSize);
        assertEquals(1, s.machine.getCurrentOperations());
        s.finish();
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(8, s.machine.getStackInSlot(1).stackSize);
        assertSame(Blocks.fence, s.fixture.world.getBlock(0, 63, 1));
        assertSame(Blocks.nether_brick_fence, s.fixture.world.getBlock(0, 62, 1));
    }

    private static void powerMatrix(int model) throws Exception {
        for (int tier = model < 3 ? model + 2 : model == 3 ? 2 : 4; tier < GT_Values.V.length; tier++) {
            for (int amps : new int[] { 0, 1, 4, 16, 64 }) {
                Scenario s = new Scenario(model, tier);
                if (amps != 0) {
                    s.machine.mEnergyHatches.clear();
                    GT_MetaTileEntity_Hatch_EnergyMulti hatch = new GT_MetaTileEntity_Hatch_EnergyMulti("test", tier,
                            amps, "test", null);
                    s.fixture.attach(hatch);
                    hatch.setEUVar(Long.MAX_VALUE);
                    s.machine.mEnergyHatchesMulti.add(hatch);
                }
                s.machine.setInventorySlotContents(1, new ItemStack(Blocks.fence, 2));
                long eut = (model < 3 ? 7L : 6L) * (1L << (tier * 2));
                boolean sufficientPower = eut <= GT_Values.V[tier] * Math.max(1, amps);
                assertEquals("tier=" + tier + " amps=" + amps, sufficientPower, s.machine.checkRecipe(null));
                if (!sufficientPower) {
                    assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, s.machine.getProcessingResult());
                    assertEquals(2, s.machine.getStackInSlot(1).stackSize);
                    assertSame(Blocks.air, s.fixture.world.getBlock(0, 63, 1));
                    continue;
                }
                assertEquals(-eut, s.machine.lEUt);
                assertEquals(model < 3 ? 20 : Math.max(1, 80 / (1 << tier)), s.machine.mMaxProgresstime);
                assertEquals(1, s.machine.getStackInSlot(1).stackSize);
                long before = amps == 0 ? s.machine.mEnergyHatches.get(0).getEUVar()
                        : s.machine.mEnergyHatchesMulti.get(0).getEUVar();
                assertTrue(s.machine.onRunningTick(null));
                long after = amps == 0 ? s.machine.mEnergyHatches.get(0).getEUVar()
                        : s.machine.mEnergyHatchesMulti.get(0).getEUVar();
                assertEquals(-s.machine.lEUt, before - after);
            }
        }
    }

    private static void mixedHatches(int model) throws Exception {
        for (int amps : new int[] { 1, 4, 16, 64 }) {
            Scenario s = new Scenario(model, 4);
            GT_MetaTileEntity_Hatch_EnergyMulti multi = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 3, amps, "test",
                    null);
            s.fixture.attach(multi);
            multi.setEUVar(1000000);
            s.machine.mEnergyHatchesMulti.add(multi);
            s.machine.setInventorySlotContents(1, new ItemStack(Blocks.fence, 2));
            // EV + HV gives nominal tier IV, irrespective of the multi-amp rating.
            long eut = (model < 3 ? 7L : 6L) * 1024;
            boolean powered = eut <= (GT_Values.V[4] + GT_Values.V[3]) / 2 * (2 + amps);
            assertEquals(powered, s.machine.checkRecipe(null));
            if (powered) {
                assertEquals(-eut, s.machine.lEUt);
                assertEquals(model < 3 ? 20 : 2, s.machine.mMaxProgresstime);
            } else {
                assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, s.machine.getProcessingResult());
                assertEquals(2, s.machine.getStackInSlot(1).stackSize);
            }
        }
    }

    private static void refusals(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        ItemStack pipes = new ItemStack(Blocks.fence, 10);
        s.fixture.items(pipes);
        long reserve = 512 + GT_Values.V[4] * 4;
        s.machine.mEnergyHatches.get(0).setEUVar(reserve - 1);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.INSUFFICIENT_STARTUP_ENERGY, s.machine.getProcessingResult());
        assertEquals(10, pipes.stackSize);
        assertNull(s.machine.getStackInSlot(1));
        assertSame(Blocks.air, s.fixture.world.getBlock(0, 63, 1));
        s.machine.mEnergyHatches.get(0).setEUVar(reserve);
        s.state.permissionCallback = () -> pipes.stackSize = 9;
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.INPUT_CHANGED, s.machine.getProcessingResult());
        assertEquals(9, pipes.stackSize);
        assertNull(s.machine.getStackInSlot(1));
        assertSame(Blocks.air, s.fixture.world.getBlock(0, 63, 1));
        s.state.permissionCallback = () -> {
        };
        s.state.allowed = false;
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(2, s.save().getInteger("workState"));
        assertSame(Blocks.air, s.fixture.world.getBlock(0, 63, 1));
        assertEquals(9, s.machine.getStackInSlot(1).stackSize);
    }

    private static void pipeProtection(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        s.column();
        s.state.allowed = false; // Concrete must pick the pipe instead of filling air.
        setField(s.machine, "workState", 2);
        s.machine.setInventorySlotContents(1, new ItemStack(Items.paper));
        GT_MetaTileEntity_Hatch_OutputBus output = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
        s.fixture.attach(output);
        s.machine.mOutputBusses.add(output);
        for (int i = 0; i < output.mInventory.length; i++) {
            output.mInventory[i] = new ItemStack(Items.stick, 64);
        }
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, s.machine.getProcessingResult());
        assertSame(Blocks.nether_brick_fence, s.fixture.world.getBlock(0, 62, 1));
        output.mInventory[0] = null;
        assertTrue(s.machine.checkRecipe(null));
        assertSame(Blocks.air, s.fixture.world.getBlock(0, 62, 1));
        assertSame(Blocks.nether_brick_fence, s.fixture.world.getBlock(0, 63, 1));
        output.mInventory[0] = new ItemStack(Items.stick, 64);
        s.finish();
        assertTrue(s.machine.isWaitingForOutput());
        s.machine.loadNBTData(s.save());
        long energy = s.machine.mEnergyHatches.get(0).getEUVar();
        assertTrue(s.machine.onRunningTick(null));
        assertEquals(energy, s.machine.mEnergyHatches.get(0).getEUVar());
        output.mInventory[0] = null;
        s.finish();
        assertFalse(s.machine.isWaitingForOutput());
        assertEquals(1, output.mInventory[0].stackSize);
        assertTrue(output.mInventory[0].isItemEqual(new ItemStack(Blocks.fence)));
        s.finish();
        assertEquals(1, output.mInventory[0].stackSize);
    }

    private static void brokenColumn(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        s.fixture.world.setBlock(0, 63, 1, Blocks.fence, 0, 3);
        s.fixture.world.setBlock(0, 62, 1, Blocks.bedrock, 0, 3);
        s.machine.setInventorySlotContents(1, new ItemStack(Blocks.fence, 2));
        s.machine.mEnergyHatches.get(0).setEUVar(0);
        assertFalse(s.machine.checkRecipe(null));
        assertSame(Blocks.fence, s.fixture.world.getBlock(0, 63, 1));
        s.machine.mEnergyHatches.get(0).setEUVar(1000000);
        assertTrue(s.machine.checkRecipe(null));
        assertSame(Blocks.nether_brick_fence, s.fixture.world.getBlock(0, 63, 1));
        assertEquals(1, s.save().getInteger("workState"));
        assertEquals(2, s.machine.getStackInSlot(1).stackSize);
    }

    private static void reload(int model) throws Exception {
        for (boolean legacy : new boolean[] { false, true }) {
            Scenario s = new Scenario(model, 4);
            s.machine.setInventorySlotContents(1, new ItemStack(Blocks.fence, 2));
            s.machine.setProcessingModes(s.machine.getSupportedProcessingModes());
            assertTrue(s.machine.checkRecipe(null));
            s.machine.mProgresstime = 1;
            NBTTagCompound tag = s.save();
            if (legacy) {
                tag.removeTag("processing");
            }
            s.machine.loadNBTData(tag);
            assertEquals(1, s.machine.mProgresstime);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(1, s.machine.getStackInSlot(1).stackSize);
            assertSame(Blocks.air, s.fixture.world.getBlock(0, 62, 1));
            if (!legacy) {
                assertEquals(s.machine.getSupportedProcessingModes(), s.machine.getProcessingModes());
            }
            s.finish();
            assertSame(Blocks.air, s.fixture.world.getBlock(0, 62, 1));
        }
    }

    private static void gui(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(s.fixture.world);
        GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory, s.fixture.tile);
        assertEquals(1, gui.getSlotCount());
        assertFalse(gui.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
        assertEquals(ProcessingLogic.PROTECT_ITEMS | (model < 3 ? ProcessingLogic.PROTECT_FLUIDS : 0),
                s.machine.getSupportedProcessingModes());
        s.machine.setProcessingModes(31);
        assertEquals(s.machine.getSupportedProcessingModes(), s.machine.getProcessingModes());
    }

    private static void production(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        s.productionReady();
        if (model < 3) {
            int expected = s.expectedOil();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(999999, s.state.reserve);
            assertEquals(1, s.state.extractions);
            assertEquals(expected, s.machine.mOutputFluids[0].amount);
            s.finish();
            assertEquals(expected, s.output.mFluid.amount);
        } else {
            FluidStack first = Materials.Concrete.getMolten(100);
            FluidStack second = Materials.Concrete.getMolten(100);
            s.fixture.liquid(first);
            s.fixture.liquid(second);
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(56, first.amount + second.amount);
            assertSame(GregTech_API.sBlockConcretes, s.fixture.world.getBlock(0, 61, 1));
            assertEquals(8, s.fixture.world.getBlockMetadata(0, 61, 1));
            assertEquals(15, s.machine.mMaxProgresstime);
        }
    }

    private static void productionRefusals(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        s.productionReady();
        if (model < 3) {
            s.output.mFluid = new FluidStack(fluid("test.driller.oil"), s.output.getCapacity());
            s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, s.machine.getProcessingResult());
            assertEquals(1000000, s.state.reserve);
            assertEquals(0, s.state.extractions);
            assertEquals(1, s.save().getInteger("workState"));
            s.output.mFluid = null;
            s.state.minimumYield = Integer.MAX_VALUE;
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(0, s.state.extractions);
            assertEquals(2, s.save().getInteger("workState"));
            Scenario changed = new Scenario(model, 4);
            changed.productionReady();
            changed.machine.mOutputHatches.clear();
            GT_MetaTileEntity_Hatch_Output_ME output = new GT_MetaTileEntity_Hatch_Output_ME("test", 4, new String[0],
                    null) {
                @Override
                public long getProcessingCapacity(FluidStack fluid) {
                    changed.state.reserve--;
                    return Integer.MAX_VALUE;
                }
            };
            changed.fixture.attach(output);
            changed.machine.mOutputHatches.add(output);
            changed.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            assertFalse(changed.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.INPUT_CHANGED, changed.machine.getProcessingResult());
            assertEquals(0, changed.state.extractions);
            assertEquals(999999, changed.state.reserve);
        } else {
            FluidStack fluid = Materials.Concrete.getMolten(143);
            s.fixture.liquid(fluid);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(143, fluid.amount);
            assertSame(Blocks.air, s.fixture.world.getBlock(0, 61, 1));
            assertEquals(2, s.save().getInteger("workState"));
        }
    }

    private static void productionReload(int model) throws Exception {
        Scenario s = new Scenario(model, 4);
        s.productionReady();
        if (model < 3) {
            MEOutput me = new MEOutput();
            s.machine.mOutputHatches.clear();
            s.fixture.attach(me);
            s.machine.mOutputHatches.add(me);
            s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            assertTrue(s.machine.checkRecipe(null));
            int produced = s.machine.mOutputFluids[0].amount;
            me.capacity = 0;
            s.finish();
            assertTrue(s.machine.isWaitingForOutput());
            s.machine.loadNBTData(s.save());
            s.machine.setProcessingModes(0);
            s.finish();
            assertTrue(s.machine.isWaitingForOutput());
            me.capacity = Integer.MAX_VALUE;
            s.finish();
            s.finish();
            assertEquals(produced, me.stored);
            assertEquals(1, s.state.extractions);
        } else {
            s.fixture.world.setBlock(0, 61, 1, Blocks.stone, 0, 3);
            s.fixture.liquid(Materials.Concrete.getMolten(288));
            assertTrue(s.machine.checkRecipe(null));
            NBTTagCompound saved = s.save();
            int radius = model == 3 ? 16 : 64;
            assertEquals(-radius, saved.getInteger("backfillX"));
            assertEquals(-radius, saved.getInteger("backfillZ"));
            s.machine.loadNBTData(saved);
            assertEquals(-radius, s.save().getInteger("backfillX"));
            assertFalse(s.machine.checkRecipe(null));
            s.finish();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(-radius + 1, s.save().getInteger("backfillZ"));
            assertSame(GregTech_API.sBlockConcretes, s.fixture.world.getBlock(-radius, 62, 1 - radius));
            assertSame(GregTech_API.sBlockConcretes, s.fixture.world.getBlock(-radius, 62, 2 - radius));
        }
    }

    private static final class State {
        int reserve = 1000000;
        int extractions;
        int minimumYield;
        boolean allowed = true;
        Runnable permissionCallback = () -> {
        };

        boolean permit() {
            permissionCallback.run();
            return allowed;
        }
    }

    private static final class Scenario {
        final int model;
        final State state = new State();
        final GT_MetaTileEntity_DrillerBase machine;
        final Fixture fixture;
        GT_MetaTileEntity_Hatch_Output output;

        Scenario(int model, int tier) throws Exception {
            this.model = model;
            machine = machine(model, state);
            fixture = new Fixture(machine, tier);
            fixture.tile.yCoord = 64;
            fixture.tile.disableWorking();
            machine.mEnergyHatches.get(0).setEUVar(Long.MAX_VALUE);
        }

        void column() {
            fixture.world.setBlock(0, 63, 1, Blocks.fence, 0, 3);
            fixture.world.setBlock(0, 62, 1, Blocks.nether_brick_fence, 0, 3);
        }

        void productionReady() throws Exception {
            column();
            setField(machine, "workState", model < 3 ? 1 : 2);
            if (model < 3) {
                fixture.world.setBlock(0, 61, 1, Blocks.bedrock, 0, 3);
                output = new GT_MetaTileEntity_Hatch_Output("test", 4, "test", null);
                fixture.attach(output);
                machine.mOutputHatches.add(output);
            }
        }

        int expectedOil() {
            float speed = .5F + (4 - (model + 2)) * .25F;
            int range = new int[] { 1, 4, 8 }[model];
            return (int) Math.floor(999999.0 * speed / 5000 * ((double) range * 16 * speed * 1.2F));
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
        int capacity = Integer.MAX_VALUE;
        int stored;

        MEOutput() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(FluidStack stack) {
            return capacity;
        }

        @Override
        public int fill(FluidStack stack, boolean commit) {
            int accepted = Math.min(capacity, stack.amount);
            if (commit) {
                capacity -= accepted;
                stored += accepted;
            }
            return accepted;
        }
    }

    private static GT_MetaTileEntity_DrillerBase machine(int model, State state) {
        switch (model) {
        case 0:
            return new GT_MetaTileEntity_OilDrill1("test") {
                @Override
                protected boolean mayBreakBlock(int x, int y, int z) {
                    return state.permit();
                }

                @Override
                protected boolean mayPlaceBlock(int x, int y, int z, Block block, int meta) {
                    return state.permit();
                }

                @Override
                protected OilReserve readOilReserve() {
                    return new OilReserve(new FluidStack(fluid("test.driller.oil"), 1), state.reserve);
                }

                @Override
                protected boolean consumeOilReserve(OilReserve expected) {
                    if (state.reserve != expected.remaining) {
                        return false;
                    }
                    state.reserve--;
                    state.extractions++;
                    return true;
                }

                @Override
                public int getTotalConfigValue() {
                    return state.minimumYield;
                }
            };
        case 1:
            return new GT_MetaTileEntity_OilDrill2("test") {
                @Override
                protected boolean mayBreakBlock(int x, int y, int z) {
                    return state.permit();
                }

                @Override
                protected boolean mayPlaceBlock(int x, int y, int z, Block block, int meta) {
                    return state.permit();
                }

                @Override
                protected OilReserve readOilReserve() {
                    return new OilReserve(new FluidStack(fluid("test.driller.oil"), 1), state.reserve);
                }

                @Override
                protected boolean consumeOilReserve(OilReserve expected) {
                    if (state.reserve != expected.remaining) {
                        return false;
                    }
                    state.reserve--;
                    state.extractions++;
                    return true;
                }

                @Override
                public int getTotalConfigValue() {
                    return state.minimumYield;
                }
            };
        case 2:
            return new GT_MetaTileEntity_OilDrill3("test") {
                @Override
                protected boolean mayBreakBlock(int x, int y, int z) {
                    return state.permit();
                }

                @Override
                protected boolean mayPlaceBlock(int x, int y, int z, Block block, int meta) {
                    return state.permit();
                }

                @Override
                protected OilReserve readOilReserve() {
                    return new OilReserve(new FluidStack(fluid("test.driller.oil"), 1), state.reserve);
                }

                @Override
                protected boolean consumeOilReserve(OilReserve expected) {
                    if (state.reserve != expected.remaining) {
                        return false;
                    }
                    state.reserve--;
                    state.extractions++;
                    return true;
                }

                @Override
                public int getTotalConfigValue() {
                    return state.minimumYield;
                }
            };
        case 3:
            return new GT_MetaTileEntity_ConcreteBackfiller1("test") {
                @Override
                protected boolean mayBreakBlock(int x, int y, int z) {
                    return state.permit();
                }

                @Override
                protected boolean mayPlaceBlock(int x, int y, int z, Block block, int meta) {
                    return state.permit();
                }
            };
        case 4:
            return new GT_MetaTileEntity_ConcreteBackfiller2("test") {
                @Override
                protected boolean mayBreakBlock(int x, int y, int z) {
                    return state.permit();
                }

                @Override
                protected boolean mayPlaceBlock(int x, int y, int z, Block block, int meta) {
                    return state.permit();
                }
            };
        default:
            throw new AssertionError(model);
        }
    }
}
