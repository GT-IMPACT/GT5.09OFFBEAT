package gregtech.api.metatileentity.implementations;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.logic.BoilerProcessingLogic;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
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

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GT_BoilerProcessingTest {
    private static GameRuntime runtime;
    private static boolean fuelHandlerRegistered;
    private final String model;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] models() {
        return new Object[] { "Bronze", "Steel", "Titanium", "TungstenSteel" };
    }

    public GT_BoilerProcessingTest(String model) {
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
    public void protectionWaitsForEmptyHatchesOnlyBetweenBurns() throws Throwable {
        run("protection");
    }

    @Test
    public void enablingProtectionDuringBurnDoesNotFreezeCombustion() throws Throwable {
        run("toggleDuringBurn");
    }

    @Test
    public void meDisconnectDoesNotPauseBurnAndBlocksNextFuel() throws Throwable {
        run("meOutput");
    }

    @Test
    public void protectedFuelContainersWaitOnlyAfterTheBurnAndSurviveReload() throws Throwable {
        run("containers");
    }

    @Test
    public void controlsAndCircuitSurviveWorldReload() throws Throwable {
        run("controls");
    }

    @Test
    public void liquidFuelPriorityDurationsAndOverflowAreTransactional() throws Throwable {
        run("liquidFuels");
    }

    @Test
    public void fuelAndThrottleRemaindersSurviveReload() throws Throwable {
        run("remainders");
    }

    @Test
    public void activeAndLegacyBurnsResumeWithoutSpendingFuelTwice() throws Throwable {
        run("resume");
    }

    @Test
    public void longSolidFuelWarmupSurvivesReloadAndCircuitChanges() throws Throwable {
        run("warmup");
    }

    @Test
    public void changedInputAndRejectedFuelLeaveInputsUntouched() throws Throwable {
        run("snapshots");
    }

    public static void runScenario(String scenario, String model) throws Throwable {
        ItemList.Cell_Empty.set(new ItemStack(Items.bucket));
        ItemList.IC2_Fuel_Can_Filled.set(new ItemStack(Items.lava_bucket));
        ItemList.IC2_ForgeHammer.set(new ItemStack(Items.iron_pickaxe));
        ItemList.IC2_WireCutter.set(new ItemStack(Items.shears));
        Materials.Water.mFluid = FluidRegistry.WATER;
        fluid("steam");
        fluid("ic2distilledwater");
        if (!fuelHandlerRegistered) {
            GameRegistry.registerFuelHandler(stack -> stack.getItem() == Items.paper && stack.hasTagCompound()
                    ? stack.getTagCompound().getInteger("testBurnTime")
                    : 0);
            fuelHandlerRegistered = true;
        }
        try {
            GT_BoilerProcessingTest.class.getDeclaredMethod(scenario, String.class).invoke(null, model);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void protection(String model) throws Exception {
        Scenario s = new Scenario(model);
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        s.output.mFluid = steam(1);
        s.machine.mEfficiency = 0;
        assertFalse("Even a cold boiler must wait for the previous steam to drain", s.machine.checkRecipe(null));
        assertEquals(4, s.fuel.stackSize);
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, s.machine.getProcessingResult());
        s.output.mFluid = null;
        s.machine.mEfficiency = 10000;
        assertTrue(s.machine.checkRecipe(null));
        s.output.mFluid = steam(s.output.getCapacity());
        int duration = s.machine.mMaxProgresstime;
        for (int tick = 0; tick < duration; tick++) {
            assertTrue("Combustion must continue with a full protected hatch", s.machine.onRunningTick(null));
            s.machine.mProgresstime++;
        }
        assertEquals(100000 - duration * s.waterPerTick(), s.water.getFluidAmount());
        assertEquals(s.output.getCapacity(), s.output.getFluidAmount());
        s.finish();
        assertEquals(0, s.machine.mMaxProgresstime);
        assertFalse(s.machine.isWaitingForOutput());
        s.machine.loadNBTData(s.save());
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(3, s.fuel.stackSize);
        s.output.mFluid.amount = 1;
        assertFalse("Partly drained is not empty", s.machine.checkRecipe(null));
        s.output.mFluid = null;
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(2, s.fuel.stackSize);
        assertEquals(0, s.machine.getRunningEUt());
    }

    private static void toggleDuringBurn(String model) throws Exception {
        Scenario s = new Scenario(model);
        s.output.mFluid = steam(s.output.getCapacity());
        assertTrue(s.machine.checkRecipe(null));
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        assertTrue(s.machine.onRunningTick(null));
        assertEquals(100000 - s.waterPerTick(), s.water.getFluidAmount());
        s.finish();
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(3, s.fuel.stackSize);
        s.machine.setProcessingModes(0);
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(2, s.fuel.stackSize);
    }

    private static void meOutput(String model) throws Exception {
        Scenario s = new Scenario(model);
        s.machine.mOutputHatches.clear();
        TestME output = new TestME();
        s.fixture.attach(output);
        s.machine.mOutputHatches.add(output);
        s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(4, s.fuel.stackSize);
        output.capacity = s.machine.getEUt() * 2;
        assertTrue(s.machine.checkRecipe(null));
        output.capacity = 0;
        assertTrue(s.machine.onRunningTick(null));
        assertEquals(100000 - s.waterPerTick(), s.water.getFluidAmount());
        assertEquals(0, output.stored);
        s.finish();
        assertFalse(s.machine.checkRecipe(null));
        output.capacity = s.machine.getEUt() * 2;
        assertTrue(s.machine.checkRecipe(null));
        assertTrue(s.machine.onRunningTick(null));
        assertEquals(s.machine.getEUt() * 2, output.stored);
        assertEquals(2, s.fuel.stackSize);
    }

    private static void containers(String model) throws Exception {
        Item previous = Items.paper.getContainerItem();
        Items.paper.setContainerItem(Items.bucket);
        try {
            Scenario s = new Scenario(model);
            s.machine.mInputBusses.clear();
            ItemStack fuel = fuel(1600, 2);
            s.fixture.items(fuel);
            GT_MetaTileEntity_Hatch_OutputBus bus = new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            s.fixture.attach(bus);
            s.machine.mOutputBusses.add(bus);
            s.machine.setProcessingModes(ProcessingLogic.PROTECT_ITEMS);
            s.output.mInventory[1] = new ItemStack(Items.iron_ingot, 64);
            Arrays.fill(bus.mInventory, new ItemStack(Items.iron_ingot, 64));
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, s.machine.getProcessingResult());
            assertEquals(2, fuel.stackSize);
            Arrays.fill(bus.mInventory, null);
            assertTrue(s.machine.checkRecipe(null));
            Arrays.fill(bus.mInventory, new ItemStack(Items.iron_ingot, 64));
            assertTrue(s.machine.onRunningTick(null));
            assertEquals(100000 - s.waterPerTick(), s.water.getFluidAmount());
            s.finish();
            assertTrue(s.machine.isWaitingForOutput());
            assertEquals(1, s.machine.mOutputItems[0].stackSize);
            s.machine.loadNBTData(s.save());
            Arrays.fill(bus.mInventory, null);
            int water = s.water.getFluidAmount();
            assertTrue(s.machine.onRunningTick(null));
            s.finish();
            assertEquals(water, s.water.getFluidAmount());
            assertFalse(s.machine.isWaitingForOutput());
            int buckets = 0;
            for (ItemStack item : bus.mInventory) {
                if (item != null && item.getItem() == Items.bucket) {
                    buckets += item.stackSize;
                }
            }
            assertEquals(1, buckets);
            assertEquals(1, fuel.stackSize);
        } finally {
            Items.paper.setContainerItem(previous);
        }
    }

    private static void controls(String model) throws Exception {
        int id = 1004;
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[id];
        GregTech_API.METATILEENTITIES[id] = machine(model);
        try {
            for (boolean enabled : new boolean[] { false, true }) {
                for (int modes : new int[] { 0, ProcessingLogic.PROTECT_ITEMS, ProcessingLogic.PROTECT_FLUIDS,
                        ProcessingLogic.ALL_MODES }) {
                    ProcessingWorld world = new ProcessingWorld();
                    BaseMetaTileEntity tile = new BaseMetaTileEntity();
                    tile.setWorldObj(world);
                    tile.setInitialValuesAsNBT(null, (short) id);
                    GT_MetaTileEntity_LargeBoiler boiler = (GT_MetaTileEntity_LargeBoiler) tile.getMetaTileEntity();
                    boiler.mInventory[1] = new ItemStack(Items.paper, 1, 7);
                    boiler.setProcessingModes(modes);
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
                    GT_MetaTileEntity_LargeBoiler restored = (GT_MetaTileEntity_LargeBoiler) restoredTile
                            .getMetaTileEntity();
                    assertEquals(enabled, restoredTile.isAllowedToWork());
                    assertEquals(modes & (ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS),
                            restored.getProcessingModes());
                    assertEquals(7, restored.mInventory[1].getItemDamage());
                    GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
                    GT_Container_ProcessingMachine gui = new GT_Container_ProcessingMachine(player.inventory,
                            restoredTile);
                    assertEquals(43, gui.inventorySlots.size());
                    assertEquals(1, gui.getSlotCount());
                    for (int slot : new int[] { GT_Container_ProcessingMachine.BATCH_SLOT,
                            GT_Container_ProcessingMachine.LOCK_SLOT,
                            GT_Container_ProcessingMachine.SEPARATION_SLOT }) {
                        assertFalse(gui.isControlVisible(slot));
                        assertTrue(gui.getSlot(slot).xDisplayPosition < 0);
                    }
                    assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.NEI_SLOT));
                    assertTrue(gui.isControlVisible(GT_Container_ProcessingMachine.VOIDING_SLOT));
                    assertSame(GT_Recipe.GT_Recipe_Map.sLargeBoilerFakeFuels, gui.getNEIRecipeMap());
                    assertEquals(11, gui.getSlot(GT_Container_ProcessingMachine.POWER_SLOT).xDisplayPosition);
                    assertEquals(29, gui.getSlot(GT_Container_ProcessingMachine.NEI_SLOT).xDisplayPosition);
                    assertNull("Display-only fuel map must not filter water hatches", restored.getRecipeMap());
                }
            }
        } finally {
            GregTech_API.METATILEENTITIES[id] = previous;
        }
    }

    private static void liquidFuels(String model) throws Exception {
        GT_Recipe.GT_Recipe_Map dieselMap = GT_Recipe.GT_Recipe_Map.sDieselFuels;
        GT_Recipe.GT_Recipe_Map denseMap = GT_Recipe.GT_Recipe_Map.sDenseLiquidFuels;
        try {
            Fluid diesel = registerFuel(dieselMap, Items.apple, "boiler_test_diesel", 100);
            Fluid dense = registerFuel(denseMap, Items.carrot, "boiler_test_dense", 100);
            Scenario s = new Scenario(model);
            GT_MetaTileEntity_Hatch_Input dieselInput = s.fixture.liquid(new FluidStack(diesel, 1000));
            GT_MetaTileEntity_Hatch_Input denseInput = s.fixture.liquid(new FluidStack(dense, 1000));
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(0, dieselInput.getFluidAmount());
            assertEquals(1000, denseInput.getFluidAmount());
            assertEquals(4, s.fuel.stackSize);
            assertEquals(boost(model, 50), s.machine.mMaxProgresstime);
            assertEquals(Math.min(10000, boost(model, 50) * s.machine.getEfficiencyIncrease() * 4),
                    s.machine.mEfficiencyIncrease);
            s.finish();
            assertTrue(s.machine.checkRecipe(null));
            assertEquals(0, denseInput.getFluidAmount());
            assertEquals(boost(model, 200), s.machine.mMaxProgresstime);
            s.finish();
            denseMap.mRecipeList.iterator().next().mSpecialValue = Integer.MAX_VALUE;
            denseInput.mFluid = new FluidStack(dense, 1000);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(CheckRecipeResult.POWER_OVERFLOW, s.machine.getProcessingResult());
            assertEquals(1000, denseInput.getFluidAmount());
            assertEquals(4, s.fuel.stackSize);
            denseInput.mFluid.amount = 999;
            assertTrue("Insufficient liquid fuel must leave it alone and allow solid fuel",
                    s.machine.checkRecipe(null));
            assertEquals(999, denseInput.getFluidAmount());
            assertEquals(3, s.fuel.stackSize);
        } finally {
            dieselMap.clear();
            denseMap.clear();
        }
    }

    private static void remainders(String model) throws Exception {
        Scenario s = new Scenario(model);
        ItemStack fuel = fuel(100, 8);
        s.machine.mInputBusses.clear();
        s.fixture.items(fuel);
        s.machine.mInventory[1] = circuit(7);
        int adjustedHeat = Math.max(25, s.machine.getEUt() - 175);
        long expectedHeat = 0;
        long burntTicks = 0;
        for (int index = 0; index < 8; index++) {
            assertTrue(s.machine.checkRecipe(null));
            int rawFuelTicks = (index % 4 == 3) ? 2 : 1;
            expectedHeat += boost(model, rawFuelTicks) * s.machine.getEUt();
            burntTicks += s.machine.mMaxProgresstime;
            NBTTagCompound saved = s.save();
            assertEquals((index + 1) * 20 % 80, saved.getCompoundTag("boiler").getInteger("fuelRemainder"));
            assertEquals(expectedHeat,
                    burntTicks * adjustedHeat + saved.getCompoundTag("boiler").getInteger("heatRemainder"));
            s.machine.loadNBTData(saved);
            s.finish();
        }
        assertEquals(0, fuel.stackSize);
    }

    private static void resume(String model) throws Exception {
        for (boolean legacy : new boolean[] { false, true }) {
            Scenario s = new Scenario(model);
            s.machine.mInventory[1] = circuit(3);
            s.machine.setProcessingModes(ProcessingLogic.PROTECT_FLUIDS);
            assertTrue(s.machine.checkRecipe(null));
            s.machine.mProgresstime = 2;
            int heat = s.machine.mEUt;
            int duration = s.machine.mMaxProgresstime;
            NBTTagCompound saved = s.save();
            if (legacy) {
                saved.removeTag("processing");
                saved.removeTag("boiler");
                saved.setInteger("mEUt", heat);
            }
            s.machine.loadNBTData(saved);
            assertEquals(heat, s.machine.mEUt);
            assertEquals(0, s.machine.getRunningEUt());
            assertEquals(duration, s.machine.mMaxProgresstime);
            assertEquals(2, s.machine.mProgresstime);
            assertFalse(s.machine.checkRecipe(null));
            assertEquals(3, s.fuel.stackSize);
            assertTrue(s.machine.onRunningTick(null));
            assertEquals(heat * 2, s.output.getFluidAmount());
            assertEquals(100000 - (heat * 2 + 160) / 160, s.water.getFluidAmount());
        }
    }

    private static void warmup(String model) throws Exception {
        Scenario s = new Scenario(model);
        s.machine.mInputBusses.clear();
        ItemStack fuel = fuel(160000, 2);
        s.fixture.items(fuel);
        s.machine.mEfficiency = 0;
        s.machine.mInventory[1] = circuit(1);
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(0, s.machine.mEfficiency);
        assertEquals(0, s.machine.mEfficiencyIncrease);
        int heat = s.machine.getEUt() - 25;
        assertEquals(heat, s.machine.mEUt);
        s.machine.mInventory[1] = circuit(25);
        for (int tick = 1; tick <= 10; tick++) {
            s.machine.loadNBTData(s.save());
            assertTrue(s.machine.onRunningTick(null));
            assertEquals(tick * 20, s.machine.mEfficiency);
            assertEquals(heat, s.machine.mEUt);
            assertEquals(1, fuel.stackSize);
        }
        s.finish();
        assertTrue(s.machine.checkRecipe(null));
        assertEquals(Math.max(25, s.machine.getEUt() - 625), s.machine.mEUt);
    }

    private static void snapshots(String model) throws Exception {
        Scenario s = new Scenario(model);
        BoilerProcessingLogic logic = new BoilerProcessingLogic(
                () -> new BoilerProcessingLogic.State(s.machine.getEUt(), s.machine.getEfficiencyIncrease(), 10000,
                        10000, 0, 0, 0),
                value -> boost(model, value));
        ItemStack coal = new ItemStack(Items.coal, 2);
        ProcessingInputs inputs = new ProcessingInputs(Collections.singletonList(coal), Collections.emptyList(),
                () -> false);
        ProcessingLogic.Result result = logic.process(new ProcessingLogic.Request(Collections.singletonList(inputs), 0,
                0, 0, new ProcessingOutputs(s.machine), bound -> 0));
        assertEquals(CheckRecipeResult.INPUT_CHANGED, result.status);
        assertEquals(2, coal.stackSize);
        assertNull(logic.getBurn());
        s.machine.mInputBusses.clear();
        ItemStack invalid = new ItemStack(Items.iron_ingot, 4);
        s.fixture.items(invalid);
        assertFalse(s.machine.checkRecipe(null));
        assertEquals(4, invalid.stackSize);
        assertEquals(CheckRecipeResult.NO_RECIPE, s.machine.getProcessingResult());
        FluidStack water = new FluidStack(FluidRegistry.WATER, 2);
        ProcessingInputs mixed = new ProcessingInputs(Collections.emptyList(),
                Arrays.asList(water, new FluidStack(fluid("ic2distilledwater"), 3)));
        assertFalse(mixed.consumeFluid(new FluidStack(FluidRegistry.WATER, 3)));
        assertEquals(2, mixed.fluids[0].amount);
        assertTrue(mixed.consumeFluid(new FluidStack(fluid("ic2distilledwater"), 3)));
        assertTrue(mixed.commit());
        assertEquals(2, water.amount);
    }

    private static Fluid registerFuel(GT_Recipe.GT_Recipe_Map map, Item item, String name, int value) {
        Fluid fluid = fluid(name);
        ItemStack container = new ItemStack(item);
        GT_Utility.addFluidContainerData(new FluidContainerRegistry.FluidContainerData(new FluidStack(fluid, 1000),
                container, new ItemStack(Items.bucket)));
        assertNotNull(map.addRecipe(false, new ItemStack[] { container }, null, null, null, null, null, 0, 0, value));
        return fluid;
    }

    private static long boost(String model, long time) {
        int ratio = new int[] { 200, 150, 130, 120 }[Arrays.asList(models()).indexOf(model)];
        return Math.multiplyExact(time, ratio) / 100;
    }

    private static ItemStack circuit(int configuration) {
        return new ItemStack(new Item() {
            @Override
            public String getUnlocalizedName(ItemStack stack) {
                return "gt.integrated_circuit";
            }
        }, 1, configuration);
    }

    private static ItemStack fuel(int burnTime, int count) {
        ItemStack fuel = new ItemStack(Items.paper, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("testBurnTime", burnTime);
        fuel.setTagCompound(tag);
        return fuel;
    }

    private static FluidStack steam(int amount) {
        return new FluidStack(fluid("steam"), amount);
    }

    private static GT_MetaTileEntity_LargeBoiler machine(String model) throws Exception {
        return (GT_MetaTileEntity_LargeBoiler) GT_MultiblockRecipeProcessingTest.machine("LargeBoiler_" + model);
    }

    private static final class Scenario {
        final GT_MetaTileEntity_LargeBoiler machine;
        final Fixture fixture;
        final ItemStack fuel = new ItemStack(Items.coal, 4);
        final GT_MetaTileEntity_Hatch_Input water;
        final GT_MetaTileEntity_Hatch_Output output;

        Scenario(String model) throws Exception {
            machine = machine(model);
            fixture = new Fixture(machine, 1);
            fixture.tile.disableWorking();
            machine.mEnergyHatches.clear();
            fixture.items(fuel);
            water = fixture.liquid(new FluidStack(FluidRegistry.WATER, 100000));
            output = new GT_MetaTileEntity_Hatch_Output("test", 2, "test", null);
            fixture.attach(output);
            machine.mOutputHatches.add(output);
            machine.mEfficiency = 10000;
        }

        int waterPerTick() {
            return (machine.getEUt() * 2 + 160) / 160;
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }

        void finish() {
            ((GT_MetaTileEntity_ProcessingMultiBlockBase<?>) machine).completeRecipe();
        }
    }

    private static final class TestME extends GT_MetaTileEntity_Hatch_Output_ME {
        int capacity;
        int stored;

        TestME() {
            super("test", 4, new String[0], null);
        }

        @Override
        public long getProcessingCapacity(FluidStack fluid) {
            return Math.max(0, capacity - stored);
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
}
