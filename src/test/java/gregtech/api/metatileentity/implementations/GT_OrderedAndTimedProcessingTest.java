package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.GT_Values;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.logic.AssemblyLineProcessingLogic;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.common.GT_Pollution;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AirFilter;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AssemblyLine;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Charcoal_Pit;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Cleanroom;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

public class GT_OrderedAndTimedProcessingTest {
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
    public void assemblyBatchBoundariesAndIngredientShortage() throws Throwable {
        run("batch");
    }

    @Test
    public void assemblySubTickNeverAddsParallelOperations() throws Throwable {
        run("subTick");
    }

    @Test
    public void assemblyKeepsEmptyPortAddressesAndAlternatives() throws Throwable {
        run("ordered");
    }

    @Test
    public void assemblyMalformedDataNeverConsumesInputs() throws Throwable {
        run("malformed");
    }

    @Test
    public void assemblyProtectionBoundsBatchAndRetainsCompletedOutput() throws Throwable {
        run("protection");
    }

    @Test
    public void assemblyRecipeLockPersistsAndRequiresTheSameData() throws Throwable {
        run("locking");
    }

    @Test
    public void assemblyActiveAndLegacyCyclesReloadWithoutConsumption() throws Throwable {
        run("assemblyReload");
    }

    @Test
    public void assemblyRejectsChangedDataAndReplacedInputsBeforeCommit() throws Throwable {
        run("changedSource");
    }

    @Test
    public void assemblyPowerOverflowDoesNotConsumeInputs() throws Throwable {
        run("overflow");
    }

    @Test
    public void assemblyMEBackpressureKeepsResultsUntilSpaceReturns() throws Throwable {
        run("meOutput");
    }

    @Test
    public void filterProtectionRefusalDoesNotCleanOrConsume() throws Throwable {
        run("filterProtection");
    }

    @Test
    public void filterActiveReloadDoesNotCleanOrConsumeTwice() throws Throwable {
        run("filterReload");
    }

    @Test
    public void filterPowerShortageDoesNotChangeWorldOrInputs() throws Throwable {
        run("filterNoPower");
    }

    @Test
    public void filterKeepsTwoDistinctInputSelectionAndFixedDuration() throws Throwable {
        run("filterSelection");
    }

    @Test
    public void filterMultiAmpHatchesDoNotChangeFiltrationStrengthOrDuration() throws Throwable {
        run("filterHatches");
    }

    @Test
    public void migratedConsumersKeepTheMaintenanceBypass() throws Throwable {
        run("maintenance");
    }

    @Test
    public void cleanroomGraduallyBuildsPurityAndKeepsItAcrossReload() throws Throwable {
        run("purity");
    }

    @Test
    public void cleanroomConsumesMorePowerAtLowPurity() throws Throwable {
        run("purityPower");
    }

    @Test
    public void allFourPersistOnlySupportedModesAndExposeOnlyNeededSlots() throws Throwable {
        run("controls");
    }

    public static void runScenario(String scenario, String ignored) throws Throwable {
        ItemList.Tool_DataStick.set(new ItemStack(Items.paper));
        ItemList.AdsorptionFilter.set(new ItemStack(Items.paper));
        ItemList.AdsorptionFilterDirty.set(new ItemStack(Items.book));
        boolean pollution = GT_Mod.gregtechproxy.mPollution;
        GT_Mod.gregtechproxy.mPollution = true;
        try {
            GT_OrderedAndTimedProcessingTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        } finally {
            GT_Mod.gregtechproxy.mPollution = pollution;
        }
    }

    private static void batch() {
        for (int ticks : new int[] { 1, 10, 127, 128, 129 }) {
            for (int available : new int[] { 1, 3, 256 }) {
                for (boolean enabled : new boolean[] { false, true }) {
                    Line line = new Line(1, available, ticks);
                    line.machine.setProcessingModes(enabled ? ProcessingLogic.BATCH : 0);
                    int expected = Math.min(available, enabled ? Math.max(1, 128 / ticks) : 1);
                    assertTrue(line.machine.checkRecipe(null));
                    assertEquals(1, line.machine.getCurrentParallel());
                    assertEquals(expected, line.machine.getCurrentOperations());
                    assertEquals(expected * ticks, line.machine.mMaxProgresstime);
                    assertEquals(available - expected, line.iron.stackSize);
                    assertEquals(expected, line.machine.mOutputItems[0].stackSize);
                    assertEquals(-32, line.machine.lEUt);
                }
            }
        }
    }

    private static void subTick() {
        for (boolean batch : new boolean[] { false, true }) {
            Line line = new Line(5, 256, 1);
            line.machine.setProcessingModes(batch ? ProcessingLogic.BATCH : 0);
            assertTrue(line.machine.checkRecipe(null));
            assertEquals(1, line.machine.getCurrentParallel());
            assertEquals(batch ? 128 : 1, line.machine.getCurrentOperations());
            assertEquals(-8192, line.machine.lEUt);
        }
    }

    private static void ordered() {
        Line line = new Line(1, 5, 40);
        line.machine.mInputBusses.clear();
        line.fixture.items();
        line.fixture.items(line.iron);
        line.fixture.liquid(null);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 300);
        line.fixture.liquid(water);
        NBTTagCompound tag = recipe(40);
        tag.removeTag("0");
        tag.setInteger("a1", 2);
        tag.setTag("a1:0", new ItemStack(Items.gold_ingot).writeToNBT(new NBTTagCompound()));
        tag.setTag("a1:1", new ItemStack(Items.iron_ingot, 2).writeToNBT(new NBTTagCompound()));
        tag.setTag("f1", new FluidStack(FluidRegistry.WATER, 100).writeToNBT(new NBTTagCompound()));
        line.machine.mInventory[1] = stick(tag);
        assertTrue(line.machine.checkRecipe(null));
        assertEquals(3, line.iron.stackSize);
        assertEquals(200, water.amount);
        line.machine.stopMachine();
        Collections.swap(line.machine.mInputBusses, 0, 1);
        assertFalse(line.machine.checkRecipe(null));
        assertEquals(3, line.iron.stackSize);
        assertEquals(200, water.amount);
    }

    private static void malformed() {
        for (String field : new String[] { "0", "f0", "output", "time", "eu", "alternatives" }) {
            Line line = new Line(1, 5, 20);
            FluidStack water = new FluidStack(FluidRegistry.WATER, 300);
            line.fixture.liquid(water);
            NBTTagCompound tag = recipe(20);
            tag.setTag("f0", new FluidStack(FluidRegistry.WATER, 100).writeToNBT(new NBTTagCompound()));
            if (field.equals("time") || field.equals("eu")) {
                tag.setInteger(field, -1);
            } else if (field.equals("alternatives")) {
                tag.setInteger("a0", Integer.MAX_VALUE);
            } else {
                tag.setTag(field, new NBTTagCompound());
            }
            line.machine.mInventory[1] = stick(tag);
            assertFalse(field, line.machine.checkRecipe(null));
            assertEquals(field, 5, line.iron.stackSize);
            assertEquals(field, 300, water.amount);
            assertEquals(field, 0, line.machine.mMaxProgresstime);
        }
    }

    private static void protection() {
        Line line = new Line(1, 20, 10);
        GT_MetaTileEntity_Hatch_OutputBus bus = output(line.fixture);
        Arrays.fill(bus.mInventory, new ItemStack(Items.emerald, 64));
        line.machine.setProcessingModes(ProcessingLogic.BATCH | ProcessingLogic.PROTECT_ITEMS);
        assertFalse(line.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, line.machine.getProcessingResult());
        assertEquals(20, line.iron.stackSize);
        bus.mInventory[0] = new ItemStack(Items.emerald, 61);
        assertTrue(line.machine.checkRecipe(null));
        assertEquals(3, line.machine.getCurrentOperations());
        assertEquals(17, line.iron.stackSize);
        bus.mInventory[0].stackSize = 64;
        finish(line.machine);
        assertTrue(line.machine.isWaitingForOutput());
        NBTTagCompound tag = save(line.machine);
        line.machine.loadNBTData(tag);
        // Changing the switch while waiting must not void the protected cycle.
        line.machine.setProcessingModes(0);
        finish(line.machine);
        assertTrue(line.machine.isWaitingForOutput());
        bus.mInventory[0] = null;
        finish(line.machine);
        assertFalse(line.machine.isWaitingForOutput());
        assertEquals(3, bus.mInventory[0].stackSize);
        finish(line.machine);
        assertEquals(3, bus.mInventory[0].stackSize);
        assertEquals(17, line.iron.stackSize);
    }

    private static void locking() {
        Line line = new Line(1, 5, 20);
        line.machine.setProcessingModes(ProcessingLogic.LOCK_RECIPE);
        assertTrue(line.machine.checkRecipe(null));
        finish(line.machine);
        line.machine.loadNBTData(save(line.machine));
        NBTTagCompound other = recipe(30);
        line.machine.mInventory[1] = stick(other);
        assertFalse(line.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.RECIPE_LOCK_INVALID, line.machine.getProcessingResult());
        assertEquals(4, line.iron.stackSize);
        line.machine.mInventory[1] = stick(recipe(20));
        assertTrue(line.machine.checkRecipe(null));
        finish(line.machine);
        line.machine.mInventory[1] = stick(other);
        line.machine.setProcessingModes(0);
        assertTrue(line.machine.checkRecipe(null));
        assertEquals(2, line.iron.stackSize);
    }

    private static void assemblyReload() {
        for (boolean legacy : new boolean[] { false, true }) {
            Line line = new Line(1, 5, 40);
            GT_MetaTileEntity_Hatch_OutputBus bus = output(line.fixture);
            assertTrue(line.machine.checkRecipe(null));
            line.machine.mProgresstime = 17;
            NBTTagCompound tag = save(line.machine);
            if (legacy) {
                tag.removeTag("processing");
            }
            line.machine.loadNBTData(tag);
            assertEquals(17, line.machine.mProgresstime);
            assertEquals(40, line.machine.mMaxProgresstime);
            assertEquals(-32, line.machine.lEUt);
            assertFalse(line.machine.checkRecipe(null));
            assertEquals(4, line.iron.stackSize);
            finish(line.machine);
            assertEquals(1, bus.mInventory[0].stackSize);
            finish(line.machine);
            assertEquals(1, bus.mInventory[0].stackSize);
        }
    }

    private static void changedSource() {
        for (boolean data : new boolean[] { false, true }) {
            Line line = new Line(1, 5, 40);
            List<ItemStack> ports = new ArrayList<>(Collections.singletonList(line.iron));
            ItemStack originalStick = line.machine.mInventory[1];
            int[] reads = { 0 };
            AssemblyLineProcessingLogic logic = new AssemblyLineProcessingLogic(() -> {
                if (++reads[0] == 2) {
                    if (data) {
                        originalStick.getTagCompound().setInteger("time", 41);
                    } else {
                        ports.set(0, line.iron.copy());
                    }
                }
                return ports;
            }, Collections::emptyList, () -> Collections.singletonList(originalStick));
            ProcessingLogic.Result result = logic.process(request(line, 32, 1));
            assertEquals(CheckRecipeResult.INPUT_CHANGED, result.status);
            assertEquals(5, line.iron.stackSize);
            assertEquals(5, ports.get(0).stackSize);
        }
    }

    private static void overflow() {
        Line line = new Line(1, 5, 40);
        AssemblyLineProcessingLogic logic = new AssemblyLineProcessingLogic(() -> Collections.singletonList(line.iron),
                Collections::emptyList, () -> Collections.singletonList(line.machine.mInventory[1]));
        assertEquals(CheckRecipeResult.POWER_OVERFLOW, logic.process(request(line, Long.MAX_VALUE, 2)).status);
        assertEquals(5, line.iron.stackSize);
    }

    private static ProcessingLogic.Request request(Line line, long voltage, long amps) {
        return new ProcessingLogic.Request(
                Collections.singletonList(
                        new ProcessingInputs(Collections.singletonList(line.iron), Collections.emptyList())),
                voltage, amps, 0, new ProcessingOutputs(line.machine), bound -> 0);
    }

    private static void meOutput() {
        Line line = new Line(1, 5, 40);
        TestMEOutput bus = new TestMEOutput();
        line.fixture.attach(bus);
        line.machine.mOutputBusses.add(bus);
        line.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS);
        bus.capacity = 0;
        assertFalse(line.machine.checkRecipe(null));
        assertEquals(5, line.iron.stackSize);
        bus.capacity = 1;
        assertTrue(line.machine.checkRecipe(null));
        bus.capacity = 0;
        finish(line.machine);
        assertTrue(line.machine.isWaitingForOutput());
        assertEquals(0, bus.stored);
        bus.capacity = 1;
        finish(line.machine);
        assertFalse(line.machine.isWaitingForOutput());
        assertEquals(1, bus.stored);
        assertEquals(4, line.iron.stackSize);
    }

    private static void filterProtection() throws Exception {
        Filter filter = new Filter();
        GT_MetaTileEntity_Hatch_OutputBus bus = output(filter.fixture);
        Arrays.fill(bus.mInventory, new ItemStack(Items.book, 64));
        filter.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS);
        assertFalse(filter.machine.checkRecipe(null));
        assertEquals(2, filter.input.stackSize);
        assertEquals(10000, filter.pollution());
        bus.mInventory[0] = null;
        assertTrue(filter.machine.checkRecipe(null));
        assertEquals(1, filter.input.stackSize);
        assertEquals(8800, filter.pollution());
        bus.mInventory[0] = new ItemStack(Items.book, 64);
        finish(filter.machine);
        assertTrue(filter.machine.isWaitingForOutput());
        assertEquals(8800, filter.pollution());
        bus.mInventory[0] = null;
        finish(filter.machine);
        assertEquals(1, bus.mInventory[0].stackSize);
        assertEquals(8800, filter.pollution());
    }

    private static void filterReload() throws Exception {
        for (boolean legacy : new boolean[] { false, true }) {
            Filter filter = new Filter();
            GT_MetaTileEntity_Hatch_OutputBus bus = output(filter.fixture);
            assertTrue(filter.machine.checkRecipe(null));
            NBTTagCompound tag = save(filter.machine);
            if (legacy) {
                tag.removeTag("processing");
                tag.removeTag("airFilterHasPollution");
                tag.removeTag("airFilterReduction");
                tag.removeTag("airFilterToolEfficiency");
            }
            filter.machine.loadNBTData(tag);
            assertFalse(filter.machine.checkRecipe(null));
            assertEquals(1, filter.input.stackSize);
            assertEquals(8800, filter.pollution());
            finish(filter.machine);
            assertEquals(1, bus.mInventory[0].stackSize);
            assertEquals(8800, filter.pollution());
        }
    }

    private static void filterNoPower() throws Exception {
        Filter filter = new Filter();
        filter.machine.mEnergyHatches.clear();
        assertFalse(filter.machine.checkRecipe(null));
        assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, filter.machine.getProcessingResult());
        assertEquals(2, filter.input.stackSize);
        assertEquals(10000, filter.pollution());
    }

    private static void filterSelection() throws Exception {
        Filter filter = new Filter();
        filter.machine.mInputBusses.clear();
        filter.fixture.items(filter.input, new ItemStack(Items.carrot), new ItemStack(Items.apple));
        assertTrue(filter.machine.checkRecipe(null));
        assertEquals(2, filter.input.stackSize);
        assertEquals(9400, filter.pollution());
        assertEquals(200, filter.machine.mMaxProgresstime);
        finish(filter.machine);
        filter.machine.mInputBusses.clear();
        ItemStack smaller = new ItemStack(Items.paper);
        filter.fixture.items(smaller, new ItemStack(Items.apple), filter.input);
        assertTrue(filter.machine.checkRecipe(null));
        assertEquals(1, smaller.stackSize);
        assertEquals(1, filter.input.stackSize);
        assertEquals(8200, filter.pollution());
    }

    private static void purity() {
        GT_MetaTileEntity_Cleanroom room = new GT_MetaTileEntity_Cleanroom("test");
        Fixture fixture = new Fixture(room, 3);
        fixture.tile.disableWorking();
        assertTrue(room.checkRecipe(null));
        assertEquals(0, room.mEfficiency);
        assertEquals(300, room.mEfficiencyIncrease);
        finish(room);
        assertEquals(300, room.mEfficiency);
        assertTrue(room.checkRecipe(null));
        room.mProgresstime = 42;
        room.loadNBTData(save(room));
        assertEquals(300, room.mEfficiency);
        assertEquals(300, room.mEfficiencyIncrease);
        assertEquals(42, room.mProgresstime);
        assertFalse(room.checkRecipe(null));
        finish(room);
        assertEquals(600, room.mEfficiency);
        room.mEfficiency = 9900;
        assertTrue(room.checkRecipe(null));
        finish(room);
        assertEquals(10000, room.mEfficiency);
    }

    private static void filterHatches() throws Exception {
        for (int tier = 0; tier < GT_Values.V.length; tier++) {
            for (int amps : new int[] { 1, 4, 16, 64 }) {
                GT_MetaTileEntity_AirFilter machine = new GT_MetaTileEntity_AirFilter("test");
                Fixture fixture = new Fixture(machine, tier);
                fixture.machine.mEnergyHatches.clear();
                GT_MetaTileEntity_Hatch_EnergyMulti energy = new GT_MetaTileEntity_Hatch_EnergyMulti("test", tier, amps,
                        "", null);
                fixture.attach(energy);
                machine.mEnergyHatchesMulti.add(energy);
                GT_MetaTileEntity_Hatch_Muffler muffler = new GT_MetaTileEntity_Hatch_Muffler("test", 8, "", null);
                fixture.attach(muffler);
                machine.mMufflerHatches.add(muffler);
                setField(machine, "hasPollution", true);
                assertTrue(machine.checkRecipe(null));
                assertEquals(tier == 0 ? -1 : -GT_Values.V[tier] * 3 / 4, machine.lEUt);
                assertEquals(200, machine.mMaxProgresstime);
                NBTTagCompound saved = save(machine);
                assertEquals(Math.min(Math.max(1, tier) * 2, 8) * 300, saved.getInteger("airFilterReduction"));
            }
        }
    }

    private static void maintenance() throws Exception {
        boolean previous = GT_MetaTileEntity_MultiBlockBase.disableMaintenance;
        try {
            for (boolean global : new boolean[] { false, true }) {
                for (GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine : new GT_MetaTileEntity_ProcessingMultiBlockBase<?>[] {
                        new Line(1, 5, 40).machine, new Filter().machine, new GT_MetaTileEntity_Cleanroom("test") }) {
                    if (machine instanceof GT_MetaTileEntity_Cleanroom) {
                        new Fixture(machine, 3);
                        machine.mEfficiency = 10000;
                    }
                    GT_MetaTileEntity_MultiBlockBase.disableMaintenance = global;
                    machine.enabledMaintenance = global;
                    machine.mWrench = machine.mScrewdriver = machine.mSoftHammer = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = false;
                    GT_MetaTileEntity_Hatch_Maintenance hatch = new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "",
                            null, false);
                    new GT_StructureIntegrationTest.TestTile(machine.getBaseMetaTileEntity().getWorld(), hatch,
                            net.minecraftforge.common.util.ForgeDirection.NORTH);
                    machine.mMaintenanceHatches.add(hatch);
                    machine.mMachine = true;
                    machine.mStartUpCheck = -1;
                    machine.mUpdate = 1000;
                    machine.getBaseMetaTileEntity().enableWorking();
                    machine.onPostTick(machine.getBaseMetaTileEntity(), 100);
                    assertEquals(6, machine.getRepairStatus());
                    assertTrue(machine.mMaxProgresstime > 0);
                    assertEquals(10000, machine.mEfficiency);
                    assertTrue(machine.onRunningTick(null));
                }
            }
        } finally {
            GT_MetaTileEntity_MultiBlockBase.disableMaintenance = previous;
        }
    }

    private static void purityPower() {
        GT_MetaTileEntity_Cleanroom room = new GT_MetaTileEntity_Cleanroom("test");
        new Fixture(room, 3);
        for (int purity : new int[] { 0, 1000, 5000, 10000 }) {
            room.stopMachine();
            room.mEfficiency = purity;
            room.mEnergyHatches.get(0).setEUVar(1000);
            assertTrue(room.checkRecipe(null));
            assertTrue(room.onRunningTick(null));
            assertEquals(1000 - 40000 / Math.max(1000, purity), room.mEnergyHatches.get(0).getEUVar());
        }
    }

    private static void controls() {
        for (GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine : new GT_MetaTileEntity_ProcessingMultiBlockBase<?>[] {
                new GT_MetaTileEntity_AssemblyLine("test"), new GT_MetaTileEntity_AirFilter("test"),
                new GT_MetaTileEntity_Cleanroom("test"), new GT_MetaTileEntity_Charcoal_Pit("test") }) {
            Fixture fixture = new Fixture(machine, 3);
            boolean assembly = machine instanceof GT_MetaTileEntity_AssemblyLine;
            boolean filter = machine instanceof GT_MetaTileEntity_AirFilter;
            for (int modes = 0; modes <= ProcessingLogic.ALL_MODES; modes++) {
                machine.setProcessingModes(modes);
                machine.loadNBTData(save(machine));
                assertEquals(modes & machine.getSupportedProcessingModes(), machine.getProcessingModes());
            }
            GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(fixture.world);
            GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory, fixture.tile);
            assertEquals(assembly || filter ? 1 : 0, gui.getSlotCount());
            assertEquals(assembly, gui.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
            assertEquals(assembly, gui.isControlVisible(GT_Container_ProcessingMachine.BATCH_SLOT));
            assertEquals(assembly, gui.isControlVisible(GT_Container_ProcessingMachine.LOCK_SLOT));
            assertEquals(assembly || filter, gui.isControlVisible(GT_Container_ProcessingMachine.VOIDING_SLOT));
            assertFalse(gui.isControlVisible(GT_Container_ProcessingMachine.SEPARATION_SLOT));
            if (assembly) {
                assertSame(GT_Recipe.GT_Recipe_Map.sAssemblylineVisualRecipes, gui.getNEIRecipeMap());
            }
            for (int index = 0; index < GT_Container_ProcessingMachine.CONTROL_COUNT; index++) {
                assertEquals(gui.isControlVisible(index), gui.getSlot(index).xDisplayPosition >= 0);
            }
        }
    }

    private static NBTTagCompound recipe(int ticks) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("eu", 32);
        tag.setInteger("time", ticks);
        tag.setTag("0", new ItemStack(Items.iron_ingot).writeToNBT(new NBTTagCompound()));
        tag.setTag("output", new ItemStack(Items.emerald).writeToNBT(new NBTTagCompound()));
        return tag;
    }

    private static ItemStack stick(NBTTagCompound tag) {
        ItemStack stick = new ItemStack(Items.paper);
        stick.setTagCompound(tag);
        return stick;
    }

    private static NBTTagCompound save(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.saveNBTData(tag);
        return tag;
    }

    private static void finish(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine) {
        machine.completeRecipe();
    }

    private static GT_MetaTileEntity_Hatch_OutputBus output(Fixture fixture) {
        GT_MetaTileEntity_Hatch_OutputBus bus = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "", null);
        fixture.attach(bus);
        fixture.machine.mOutputBusses.add(bus);
        return bus;
    }

    private static final class Line {
        final GT_MetaTileEntity_AssemblyLine machine = new GT_MetaTileEntity_AssemblyLine("test");
        final Fixture fixture;
        final ItemStack iron;

        Line(int tier, int ingredients, int ticks) {
            fixture = new Fixture(machine, tier);
            fixture.tile.disableWorking();
            iron = new ItemStack(Items.iron_ingot, ingredients);
            fixture.items(iron);
            machine.mInventory[1] = stick(recipe(ticks));
        }
    }

    private static final class Filter {
        final GT_MetaTileEntity_AirFilter machine = new GT_MetaTileEntity_AirFilter("test");
        final Fixture fixture = new Fixture(machine, 1);
        final ItemStack input = new ItemStack(Items.paper, 2);

        Filter() throws Exception {
            fixture.tile.disableWorking();
            setField(machine, "hasPollution", true);
            fixture.items(input);
            GT_MetaTileEntity_Hatch_Muffler muffler = new GT_MetaTileEntity_Hatch_Muffler("test", 8, "", null);
            fixture.attach(muffler);
            machine.mMufflerHatches.add(muffler);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    GT_Pollution.addPollution(fixture.world.getChunkFromChunkCoords(x, z),
                            10000 - GT_Pollution.getPollution(fixture.world.getChunkFromChunkCoords(x, z)));
                }
            }
        }

        int pollution() {
            return GT_Pollution.getPollution(fixture.world.getChunkFromChunkCoords(0, 0));
        }
    }

    private static final class TestMEOutput extends GT_MetaTileEntity_Hatch_OutputBus_ME {
        int capacity;
        int stored;

        TestMEOutput() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(ItemStack stack) {
            return capacity;
        }

        @Override
        public boolean storeAll(ItemStack stack) {
            int accepted = Math.min(capacity, stack.stackSize);
            capacity -= accepted;
            stored += accepted;
            stack.stackSize -= accepted;
            return stack.stackSize == 0;
        }
    }
}
