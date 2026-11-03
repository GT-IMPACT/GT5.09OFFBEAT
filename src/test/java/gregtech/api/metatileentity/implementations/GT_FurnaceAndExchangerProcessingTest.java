package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.ItemList;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.gui.GT_ProcessingOutputSummary;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.Fixture;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.GameRuntime;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_HeatExchanger;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiFurnace;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.fluid;
import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GT_FurnaceAndExchangerProcessingTest {

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

    @Test
    public void furnaceSmeltsMixedInputsAndRespectsBusSeparation() throws Throwable {
        run("mixed");
    }

    @Test
    public void furnaceOverclocksAndMultiAmpPower() throws Throwable {
        run("power");
    }

    @Test
    public void furnaceBatchUsesSequentialOperations() throws Throwable {
        run("batch");
    }

    @Test
    public void furnaceProtectedOutputRetriesAfterReload() throws Throwable {
        run("protectedFurnace");
    }

    @Test
    public void furnaceCoilLimitChangesBetweenCycles() throws Throwable {
        run("coils");
    }

    @Test
    public void furnaceRejectsMissingCoilsAndPowerWithoutConsumingInputs() throws Throwable {
        run("rejections");
    }

    @Test
    public void exchangerColdAndSteamPortsRemainSeparate() throws Throwable {
        run("ports");
    }

    @Test
    public void exchangerProtectedPortsRejectBeforeConsumingHotFluid() throws Throwable {
        run("protectedExchanger");
    }

    @Test
    public void exchangerFinishesThermalCycleWhenOutputFills() throws Throwable {
        run("thermalCycle");
    }

    @Test
    public void exchangerWaterRemainderAndActiveCycleSurviveReload() throws Throwable {
        run("waterRemainder");
    }

    @Test
    public void exchangerDryRunningStillExplodes() throws Throwable {
        run("dry");
    }

    @Test
    public void exchangerInvalidInputDoesNotStartPhantomCycle() throws Throwable {
        run("invalid");
    }

    @Test
    public void guiOnlyShowsApplicableControlsAndExchangerUsesVirtualCircuit() throws Throwable {
        run("controls");
    }

    public static void runScenario(String scenario, String ignored) throws Throwable {
        if (!ItemList.Circuit_Integrated.hasBeenSet()) {
            new GT_IntegratedCircuit_Item();
        }
        for (String name : new String[] { "steam", "ic2distilledwater", "ic2hotcoolant", "ic2coolant",
                "ic2superheatedsteam", "ic2pahoehoelava" }) {
            fluid(name);
        }
        try {
            GT_FurnaceAndExchangerProcessingTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void mixed() throws Exception {
        for (boolean separate : new boolean[] { false, true }) {
            Furnace s = new Furnace(1);
            ItemStack iron = new ItemStack(Blocks.iron_ore, 4);
            ItemStack gold = new ItemStack(Blocks.gold_ore, 4);
            s.fixture.items(iron);
            s.fixture.items(gold);
            s.machine.setProcessingModes(separate ? ProcessingLogic.SEPARATE_INPUTS : 0);
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(separate ? 4 : 8, s.machine.getCurrentOperations());
            assertEquals(separate ? 4 : 0, iron.stackSize + gold.stackSize);
            assertEquals(512, s.machine.mMaxProgresstime);
            List<GT_ProcessingOutputSummary.Entry<ItemStack>> displayed =
                    GT_ProcessingOutputSummary.items(s.machine.mOutputItems);
            assertEquals(separate ? 1 : 2, displayed.size());
            for (GT_ProcessingOutputSummary.Entry<ItemStack> entry : displayed) {
                assertEquals(4, entry.amount);
            }
        }
    }

    private static void power() throws Exception {
        for (int tier = 1; tier <= 5; tier++) {
            Furnace s = new Furnace(tier);
            s.fixture.items(new ItemStack(Blocks.iron_ore, 8));
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(-(4L << (2 * (tier - 1))), s.machine.lEUt);
            assertEquals(512 >> (tier - 1), s.machine.mMaxProgresstime);
        }
        Furnace s = new Furnace(1);
        s.machine.mEnergyHatches.clear();
        GT_MetaTileEntity_Hatch_EnergyMulti hatch = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 16, "", null);
        s.fixture.attach(hatch);
        s.machine.mEnergyHatchesMulti.add(hatch);
        s.fixture.items(new ItemStack(Blocks.iron_ore, 8));
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(-64, s.machine.lEUt);
        assertEquals(128, s.machine.mMaxProgresstime);
    }

    private static void batch() throws Exception {
        Furnace s = new Furnace(5);
        ItemStack ore = new ItemStack(Blocks.iron_ore, 256);
        s.fixture.items(ore);
        s.machine.setProcessingModes(ProcessingLogic.BATCH);
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(8, s.machine.getCurrentParallel());
        assertEquals(32, s.machine.getCurrentOperations());
        assertEquals(128, s.machine.mMaxProgresstime);
        assertEquals(-1024, s.machine.lEUt);
        assertEquals(224, ore.stackSize);
    }

    private static void protectedFurnace() throws Exception {
        Furnace s = new Furnace(1);
        ItemStack ore = new ItemStack(Blocks.iron_ore, 16);
        s.fixture.items(ore);
        GT_MetaTileEntity_Hatch_OutputBus output = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "", null);
        s.fixture.attach(output);
        s.machine.mOutputBusses.add(output);
        Arrays.fill(output.mInventory, new ItemStack(Items.iron_ingot, 64));
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(16, ore.stackSize);
        output.mInventory[0] = null;
        assertTrue(s.machine.checkRecipe(null));
        output.mInventory[0] = new ItemStack(Items.iron_ingot, 64);
        s.finish();
        assertTrue(s.machine.isWaitingForOutput());
        s.machine.loadNBTData(save(s.machine));
        output.mInventory[0] = null;
        s.finish();
        assertFalse(s.machine.isWaitingForOutput());
        assertEquals(8, output.mInventory[0].stackSize);
        assertEquals(8, ore.stackSize);
    }

    private static void coils() throws Exception {
        Furnace s = new Furnace(3);
        ItemStack ore = new ItemStack(Blocks.iron_ore, 64);
        s.fixture.items(ore);
        for (int level : new int[] { 1, 4, 1 }) {
            setField(s.machine, "mLevel", level);
            assertEquals(level * 8, s.machine.getAvailableParallel());
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(level * 8, s.machine.getCurrentOperations());
            s.finish();
        }
        assertEquals(16, ore.stackSize);
    }

    private static void rejections() throws Exception {
        Furnace s = new Furnace(1);
        ItemStack ore = new ItemStack(Blocks.iron_ore, 16);
        s.fixture.items(ore);
        setField(s.machine, "mLevel", 0);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(16, ore.stackSize);
        setField(s.machine, "mLevel", 16);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, s.machine.getProcessingResult());
        assertEquals(16, ore.stackSize);
    }

    private static void ports() throws Exception {
        for (boolean lava : new boolean[] { true, false }) {
            for (int configuration : new int[]{0, 24}) {
                Exchanger s = new Exchanger();
                GhostCircuitItemStackHandler circuit = new GhostCircuitItemStackHandler(s.machine);
                circuit.setCircuitConfig(configuration);
                assertTrue(s.machine.mInventory[s.machine.getCircuitSlot()].getItem()
                    instanceof GT_IntegratedCircuit_Item);
                s.hot.mFluid = lava ? new FluidStack(FluidRegistry.LAVA, 2000)
                    : FluidRegistry.getFluidStack("ic2hotcoolant", 1600);
                int initialHot = s.hot.getFluidAmount();
                int processed = initialHot;
                int expectedHeat = lava ? 8000 : 16000;
                if (configuration == 24) {
                    processed = lava ? 274 : 220;
                    expectedHeat = lava ? 717 : 1440;
                }
                assertTrue(s.machine.checkRecipe(null));
                assertEquals(initialHot - processed, s.hot.getFluidAmount());
                assertEquals(lava ? "ic2pahoehoelava" : "ic2coolant", s.cold.mFluid.getFluid().getName());
                assertEquals(processed, s.cold.getFluidAmount());
                assertEquals(0, s.steam.getFluidAmount());
                assertEquals(configuration, circuit.getCircuitConfig());
                assertEquals(0, circuit.getStackInSlot(0).stackSize);
                s.machine.loadNBTData(save(s.machine));
                assertEquals(configuration, circuit.getCircuitConfig());
                assertTrue(s.machine.onRunningTick(null));
                assertEquals("ic2superheatedsteam", s.steam.mFluid.getFluid().getName());
                assertEquals(expectedHeat, s.steam.getFluidAmount());
                assertEquals(0, s.machine.lEUt);
            }
        }
    }

    private static void protectedExchanger() throws Exception {
        Exchanger s = new Exchanger();
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        s.cold.mFluid = FluidRegistry.getFluidStack("ic2pahoehoelava", 1);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(1000, s.hot.getFluidAmount());
        s.cold.mFluid = null;
        s.steam.mFluid = FluidRegistry.getFluidStack("steam", 1);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(1000, s.hot.getFluidAmount());
        s.steam.mFluid = null;
        assertTrue(s.machine.checkRecipe(null));
    }

    private static void thermalCycle() throws Exception {
        Exchanger s = new Exchanger();
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        assertTrue(s.machine.checkRecipe(null));
        s.steam.mFluid = FluidRegistry.getFluidStack("ic2superheatedsteam", s.steam.getCapacity());
        int water = s.water.getFluidAmount();
        for (int tick = 0; tick < 20; tick++) {
            assertTrue(s.machine.onRunningTick(null));
        }
        assertEquals(water - 500, s.water.getFluidAmount());
        s.finish();
        s.hot.mFluid = new FluidStack(FluidRegistry.LAVA, 1000);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(1000, s.hot.getFluidAmount());
        s.steam.mFluid = null;
        s.cold.mFluid = null;
        assertTrue(s.machine.checkRecipe(null));
    }

    private static void waterRemainder() throws Exception {
        Exchanger s = new Exchanger();
        s.hot.mFluid.amount = 1;
        assertTrue(s.machine.checkRecipe(null));
        int initial = s.water.getFluidAmount();
        for (int tick = 0; tick < 20; tick++) {
            s.machine.loadNBTData(save(s.machine));
            assertTrue(s.machine.onRunningTick(null));
        }
        assertEquals(160, s.steam.getFluidAmount());
        assertEquals(initial - 1, s.water.getFluidAmount());
        assertEquals(4, s.machine.mEUt);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(0, s.hot.getFluidAmount());
    }

    private static void dry() throws Exception {
        Exchanger s = new Exchanger();
        assertTrue(s.machine.checkRecipe(null));
        s.water.mFluid = null;
        s.machine.onRunningTick(null);
        assertTrue(s.machine.exploded);
    }

    private static void invalid() throws Exception {
        Exchanger s = new Exchanger();
        s.hot.mFluid = null;
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(0, s.machine.mMaxProgresstime);
        s.hot.mFluid = new FluidStack(FluidRegistry.WATER, 1000);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(1000, s.hot.getFluidAmount());
    }

    private static void controls() throws Exception {
        Furnace furnace = new Furnace(1);
        Exchanger exchanger = new Exchanger();
        for (GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine : new GT_MetaTileEntity_ProcessingMultiBlockBase<?>[] {
                furnace.machine, exchanger.machine }) {
            boolean thermal = machine == exchanger.machine;
            Fixture fixture = thermal ? exchanger.fixture : furnace.fixture;
            machine.setProcessingModes(ProcessingLogic.ALL_MODES);
            machine.loadNBTData(save(machine));
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(fixture.world);
            GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory, fixture.tile);
            assertEquals(0, gui.getSlotCount());
            assertEquals(thermal, machine.allowSelectCircuit());
            assertFalse(machine.hasControllerSlotInGui());
            assertEquals(!thermal, gui.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
            assertEquals(!thermal, gui.isControlVisible(GT_Container_ProcessingMachine.BATCH_SLOT));
            assertEquals(!thermal, gui.isControlVisible(GT_Container_ProcessingMachine.SEPARATION_SLOT));
            assertFalse(gui.isControlVisible(GT_Container_ProcessingMachine.LOCK_SLOT));
            for (int index = 0; index < GT_Container_ProcessingMachine.CONTROL_COUNT; index++) {
                assertEquals(gui.isControlVisible(index), gui.getSlot(index).xDisplayPosition >= 0);
            }
            assertEquals(machine.getSupportedProcessingModes(), machine.getProcessingModes());
        }
    }

    private static NBTTagCompound save(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.saveNBTData(tag);
        return tag;
    }

    private static final class Furnace {
        final GT_MetaTileEntity_MultiFurnace machine = new GT_MetaTileEntity_MultiFurnace("test");
        final Fixture fixture;

        Furnace(int tier) throws Exception {
            fixture = new Fixture(machine, tier);
            fixture.tile.disableWorking();
            setField(machine, "mLevel", 1);
            setField(machine, "mCostDiscount", 1);
        }

        void finish() {
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
        }
    }

    private static final class RecordingExchanger extends GT_MetaTileEntity_HeatExchanger {
        boolean exploded;

        RecordingExchanger() {
            super("test");
        }

        @Override
        public void explodeMultiblock() {
            exploded = true;
        }
    }

    private static final class Exchanger {
        final RecordingExchanger machine = new RecordingExchanger();
        final Fixture fixture = new Fixture(machine, 1);
        final GT_MetaTileEntity_Hatch_Input hot = new GT_MetaTileEntity_Hatch_Input("hot", 2, "", null);
        final GT_MetaTileEntity_Hatch_Input water;
        final GT_MetaTileEntity_Hatch_Output cold = new GT_MetaTileEntity_Hatch_Output("cold", 2, "", null);
        final GT_MetaTileEntity_Hatch_Output steam = new GT_MetaTileEntity_Hatch_Output("steam", 4, "", null);

        Exchanger() throws Exception {
            fixture.tile.disableWorking();
            fixture.attach(hot);
            fixture.attach(cold);
            fixture.attach(steam);
            setField(machine, "mInputHotFluidHatch", hot);
            setField(machine, "mOutputColdFluidHatch", cold);
            machine.mOutputHatches.add(steam);
            machine.mEfficiency = 10000;
            hot.mFluid = new FluidStack(FluidRegistry.LAVA, 1000);
            water = fixture.liquid(FluidRegistry.getFluidStack("ic2distilledwater", 100000));
        }

        void finish() {
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
        }
    }
}
