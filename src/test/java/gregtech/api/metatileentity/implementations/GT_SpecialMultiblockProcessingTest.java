package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.ItemList;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.GT_Mod;
import gregtech.common.GT_Pollution;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.tileentities.machines.multi.*;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

public class GT_SpecialMultiblockProcessingTest {
    private static GameRuntime runtime;
    @BeforeClass public static void openGame() throws Exception { runtime = new GameRuntime(); }
    @AfterClass public static void closeGame() throws Exception { if (runtime != null) runtime.close(); }
    private void run(String scenario, String model) throws Throwable { runtime.run(getClass(), scenario, model); }

    @Test public void bronzeBoilerFuelAndCircuitMatrix() throws Throwable { run("boiler", "Bronze"); }
    @Test public void steelBoilerFuelAndCircuitMatrix() throws Throwable { run("boiler", "Steel"); }
    @Test public void titaniumBoilerFuelAndCircuitMatrix() throws Throwable { run("boiler", "Titanium"); }
    @Test public void tungstenBoilerFuelAndCircuitMatrix() throws Throwable { run("boiler", "TungstenSteel"); }
    @Test public void steamTurbineFlowAndRamp() throws Throwable { run("turbine", "Steam"); }
    @Test public void superheatedTurbineFlowAndRamp() throws Throwable { run("turbine", "HPSteam"); }
    @Test public void gasTurbineFlowAndRamp() throws Throwable { run("turbine", "Gas"); }
    @Test public void plasmaTurbineFlowAndRamp() throws Throwable { run("turbine", "Plasma"); }
    @Test public void oilDrillOneEnergyAndStates() throws Throwable { run("driller", "OilDrill1"); }
    @Test public void oilDrillTwoEnergyAndStates() throws Throwable { run("driller", "OilDrill2"); }
    @Test public void oilDrillThreeEnergyAndStates() throws Throwable { run("driller", "OilDrill3"); }
    @Test public void concreteBackfillerOneEnergyAndStates() throws Throwable { run("driller", "ConcreteBackfiller1"); }
    @Test public void concreteBackfillerTwoEnergyAndStates() throws Throwable { run("driller", "ConcreteBackfiller2"); }
    @Test public void cleanroomAllHatchTiersAndMultiAmpVariants() throws Throwable { run("cleanroom", ""); }
    @Test public void airFilterIdlePollutedAndConsumableModes() throws Throwable { run("airFilter", ""); }
    @Test public void assemblyLineVoltageAndMultiAmpMatrix() throws Throwable { run("assemblyLine", ""); }
    @Test public void assemblyLineCannotSkipMultipleVoltageTiers() throws Throwable { run("assemblyTierLimit", ""); }

    public static void runScenario(String scenario, String model) throws Throwable {
        try {
            GT_SpecialMultiblockProcessingTest.class.getDeclaredMethod(scenario, String.class).invoke(null, model);
        } catch (InvocationTargetException e) { throw e.getCause(); }
    }

    private static void boiler(String model) throws Exception {
        if (!ItemList.Circuit_Integrated.hasBeenSet()) {
            new GT_IntegratedCircuit_Item();
        }
        ItemList.Cell_Empty.set(new ItemStack(Items.bucket));
        ItemList.IC2_Fuel_Can_Filled.set(new ItemStack(Items.lava_bucket));
        ItemList.IC2_ForgeHammer.set(new ItemStack(Items.iron_pickaxe));
        ItemList.IC2_WireCutter.set(new ItemStack(Items.shears));
        int index = java.util.Arrays.asList("Bronze", "Steel", "Titanium", "TungstenSteel").indexOf(model);
        int rawEnergy = new int[]{400, 600, 800, 1000}[index];
        int rawDuration = new int[]{40, 30, 26, 24}[index]; // One vanilla coal, 1600 furnace ticks.
        int efficiencyIncrease = new int[]{16, 12, 8, 4}[index];
        for (int config = 0; config <= GhostCircuitItemStackHandler.MAX_CONFIG; config++) {
            for (boolean warmedUp : new boolean[]{false, true}) {
                GT_MetaTileEntity_LargeBoiler machine = (GT_MetaTileEntity_LargeBoiler)
                    GT_MultiblockRecipeProcessingTest.machine("LargeBoiler_" + model);
                Fixture f = new Fixture(machine, 1);
                ItemStack coal = new ItemStack(Items.coal, 2);
                f.items(coal);
                new GhostCircuitItemStackHandler(machine).setCircuitConfig(config);
                machine.mEfficiency = warmedUp ? 10000 : 0;
                assertTrue(model + " circuit=" + config, machine.checkRecipe(null));
                int energy = Math.max(25, rawEnergy - 25 * config);
                int duration = warmedUp ? rawDuration * rawEnergy / energy : rawDuration;
                assertEquals(1, coal.stackSize);
                assertEquals(energy, machine.mEUt);
                assertEquals(duration, machine.mMaxProgresstime);
                assertEquals(duration * efficiencyIncrease > 5000 ? 0 : duration * efficiencyIncrease,
                    machine.mEfficiencyIncrease);
                NBTTagCompound tag = new NBTTagCompound();
                machine.saveNBTData(tag);
                GT_MetaTileEntity_LargeBoiler restored = (GT_MetaTileEntity_LargeBoiler)
                    GT_MultiblockRecipeProcessingTest.machine("LargeBoiler_" + model);
                new Fixture(restored, 1);
                restored.loadNBTData(tag);
                assertEquals(energy, restored.mEUt);
                assertEquals(duration, restored.mMaxProgresstime);
            }
        }
        GT_MetaTileEntity_LargeBoiler machine = (GT_MetaTileEntity_LargeBoiler)
                GT_MultiblockRecipeProcessingTest.machine("LargeBoiler_" + model);
        Fixture f = new Fixture(machine, 1);
        assertFalse(machine.checkRecipe(null));
        assertEquals(0, machine.mEUt);
        Materials.Water.mFluid = FluidRegistry.WATER;
        fluid("steam"); fluid("ic2distilledwater");
        GT_MetaTileEntity_Hatch_Input water = f.liquid(new FluidStack(FluidRegistry.WATER, 1000));
        GT_MetaTileEntity_Hatch_Output output = output(f);
        machine.mEUt = rawEnergy; machine.mEfficiency = 10000;
        assertTrue(machine.onRunningTick(null));
        assertEquals(rawEnergy * 2, output.mFluid.amount);
        assertEquals(1000 - (rawEnergy * 2 + 160) / 160, water.mFluid.amount);
    }

    private static GT_MetaTileEntity_Hatch_Output output(Fixture f) {
        GT_MetaTileEntity_Hatch_Output output = new GT_MetaTileEntity_Hatch_Output("test", 8, "test", null);
        f.attach(output); f.machine.mOutputHatches.add(output);
        return output;
    }

    private static void turbine(String model) throws Exception {
        boolean gas = model.equals("Gas"), plasma = model.equals("Plasma"), steam = model.equals("Steam");
        fluid("steam"); fluid("ic2distilledwater"); fluid("test_coolant");
        if (!FluidRegistry.isFluidRegistered("ic2superheatedsteam")) {
            FluidRegistry.registerFluid(new Fluid("ic2superheatedsteam") {
                @Override public String getUnlocalizedName(FluidStack stack) { return "ic2.fluidSuperheatedSteam"; }
            });
        }
        Fluid fuel = fluid(gas ? "processing_gas" : plasma ? "plasma.test_coolant" : steam ? "steam" : "ic2superheatedsteam");
        GT_Recipe.GT_Recipe_Map fuels = gas ? GT_Recipe.GT_Recipe_Map.sTurbineFuels : GT_Recipe.GT_Recipe_Map.sPlasmaFuels;
        if (gas || plasma) {
            ItemStack container = new ItemStack(gas ? Items.apple : Items.carrot);
            GT_Utility.addFluidContainerData(new FluidContainerRegistry.FluidContainerData(
                    new FluidStack(fuel, 1000), container, new ItemStack(Items.bucket)));
            assertNotNull(fuels.addRecipe(false, new ItemStack[]{container}, null, null, null, null, null, 0, 0, 100));
        }
        try {
            int optimum = gas ? 16 : plasma ? 641 : 1600;
            int[] amounts = {0, optimum / 2, optimum, optimum * 5 / 4, optimum * 2};
            int[] expectedPower = gas ? new int[]{0, 400, 1600, 1500, 1500}
                    : plasma ? new int[]{0, 15975, 64100, 64100, 64100}
                    : steam ? new int[]{0, 200, 800, 800, 800} : new int[]{0, 400, 1600, 1600, 1600};
            for (int i = 0; i < amounts.length; i++) {
                GT_MetaTileEntity_LargeTurbine machine = (GT_MetaTileEntity_LargeTurbine)
                        GT_MultiblockRecipeProcessingTest.machine("LargeTurbine_" + model);
                Fixture f = new Fixture(machine, 1);
                setField(machine, "fuelAchievement", true);
                machine.optFlow = 1600; machine.baseEff = 10000;
                GT_MetaTileEntity_Hatch_Input input = f.liquid(new FluidStack(fuel, amounts[i]));
                GT_MetaTileEntity_Hatch_Output output = output(f);
                setField(machine, "rotorPower", (long) expectedPower[i]);
                assertEquals(model + " input=" + amounts[i], amounts[i] > 0, machine.checkRecipe(null));
                assertEquals(expectedPower[i], machine.getRunningEUt());
                int consumed = Math.min(amounts[i], optimum * 5 / 4);
                assertEquals(amounts[i] - consumed, input.getFluidAmount());
                assertEquals("Outputs stay with the active cycle", 0, output.getFluidAmount());
                if (consumed > 0 && !gas) {
                    assertEquals(steam ? consumed / 160 : consumed, machine.mOutputFluids[0].amount);
                    assertSame(fluid(steam ? "ic2distilledwater" : plasma ? "test_coolant" : "steam"),
                            machine.mOutputFluids[0].getFluid());
                }
                machine.stopMachine();
                input.mFluid = new FluidStack(fuel, optimum);
                assertTrue(machine.checkRecipe(null));
                assertEquals("Startup ramp", Math.max(10, expectedPower[2] / 100), machine.getRunningEUt());
                assertEquals(1, machine.mMaxProgresstime);
                assertEquals(10, machine.mEfficiencyIncrease);
                machine.stopMachine();
                input.mFluid = null;
                setField(machine, "rotorPower", 9L);
                assertFalse(machine.checkRecipe(null));
                assertEquals(0, machine.mEUt);
                assertEquals(0, machine.mEfficiency);
            }
        } finally { if (gas || plasma) fuels.clear(); }
    }

    private static void driller(String model) throws Exception {
        boolean oil = model.startsWith("Oil");
        int[] downward = {40, 40, 20, 10, 5, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        int[] upward = {120, 120, 60, 30, 15, 7, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        Method stats = GT_MetaTileEntity_DrillerBase.class.getDeclaredMethod("setElectricityStats");
        stats.setAccessible(true);
        Method enough = GT_MetaTileEntity_DrillerBase.class.getDeclaredMethod("isEnergyEnough");
        enough.setAccessible(true);
        for (int tier = 0; tier < GT_Values.V.length; tier++) for (int state = 0; state < 3; state++) {
            GT_MetaTileEntity_DrillerBase machine = (GT_MetaTileEntity_DrillerBase)
                    GT_MultiblockRecipeProcessingTest.machine(model);
            Fixture f = new Fixture(machine, tier);
            setField(machine, "workState", state);
            stats.invoke(machine);
            long expectedEUt = oil ? -(7L << (tier * 2)) : -(6L << (Math.max(1, tier) * 2));
            assertEquals(model + " tier=" + tier, expectedEUt, machine.lEUt);
            assertEquals((int) Math.max(Integer.MIN_VALUE, expectedEUt), machine.mEUt);
            assertEquals(oil ? 20 : state == 2 ? upward[tier] : downward[tier], machine.mMaxProgresstime);
            assertEquals(10000, machine.mEfficiency);
            long required = 512 + GT_Values.V[tier] * 4;
            f.machine.mEnergyHatches.get(0).setEUVar(required - 1);
            assertFalse((boolean) enough.invoke(machine));
            f.machine.mEnergyHatches.get(0).setEUVar(required);
            assertTrue((boolean) enough.invoke(machine));
            NBTTagCompound tag = new NBTTagCompound(); machine.saveNBTData(tag);
            assertEquals(state, tag.getInteger("workState"));
        }
        // Unlike running-tick drain, a driller's startup reserve combines buffers.
        GT_MetaTileEntity_DrillerBase machine = (GT_MetaTileEntity_DrillerBase)
                GT_MultiblockRecipeProcessingTest.machine(model);
        Fixture f = new Fixture(machine, 1);
        GT_MetaTileEntity_Hatch_EnergyMulti multi = new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
        f.attach(multi); machine.mEnergyHatchesMulti.add(multi);
        f.machine.mEnergyHatches.get(0).setEUVar(384); multi.setEUVar(383);
        assertEquals(160, machine.getMaxInputVoltage());
        assertFalse((boolean) enough.invoke(machine));
        multi.setEUVar(384);
        assertTrue((boolean) enough.invoke(machine));
    }

    private static void cleanroom(String ignored) {
        for (int tier = 0; tier < GT_Values.V.length; tier++) for (int amps : new int[]{0, 1, 4, 16, 64}) {
            GT_MetaTileEntity_Cleanroom machine = new GT_MetaTileEntity_Cleanroom("test");
            Fixture f = new Fixture(machine, tier);
            if (amps != 0) {
                machine.mEnergyHatches.clear();
                GT_MetaTileEntity_Hatch_EnergyMulti hatch = new GT_MetaTileEntity_Hatch_EnergyMulti("test", tier, amps, "test", null);
                f.attach(hatch); machine.mEnergyHatchesMulti.add(hatch);
            }
            assertTrue(machine.checkRecipe(null));
            assertEquals(-4, machine.mEUt);
            assertEquals(100, machine.mMaxProgresstime);
            assertEquals("Cleaning rate depends on hatch tier, not amps", tier * 100, machine.mEfficiencyIncrease);
        }
    }

    private static void airFilter(String ignored) throws Exception {
        ItemList.AdsorptionFilter.set(new ItemStack(Items.paper));
        ItemList.AdsorptionFilterDirty.set(new ItemStack(Items.book));
        boolean previousPollution = GT_Mod.gregtechproxy.mPollution;
        GT_Mod.gregtechproxy.mPollution = true;
        try {
            for (int tier = 0; tier <= 13; tier++) for (boolean polluted : new boolean[]{false, true})
                for (boolean consumable : new boolean[]{false, true}) {
                    GT_MetaTileEntity_AirFilter machine = new GT_MetaTileEntity_AirFilter("test");
                    Fixture f = new Fixture(machine, tier);
                    setField(machine, "hasPollution", polluted);
                    GT_MetaTileEntity_Hatch_Muffler muffler = new GT_MetaTileEntity_Hatch_Muffler("test", 8, "test", null);
                    f.attach(muffler); machine.mMufflerHatches.add(muffler);
                    ItemStack filter = new ItemStack(Items.paper, 2);
                    if (consumable) f.items(filter);
                    for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                        net.minecraft.world.chunk.Chunk chunk = f.world.getChunkFromChunkCoords(x, z);
                        GT_Pollution.addPollution(chunk, 10000 - GT_Pollution.getPollution(chunk));
                    }
                    if (tier == 0 && !polluted) {
                        assertFalse("An 8 EU/t supply cannot run the 32 EU/t idle cycle", machine.checkRecipe(null));
                        assertEquals(2, filter.stackSize);
                        assertEquals(10000, GT_Pollution.getPollution(f.world.getChunkFromChunkCoords(0, 0)));
                        continue;
                    }
                    assertTrue(machine.checkRecipe(null));
                    assertEquals(200, machine.mMaxProgresstime);
                    assertEquals(polluted ? tier == 0 ? -1 : -(int) (GT_Values.V[tier] * 3 / 4) : -32, machine.mEUt);
                    int reduction = polluted ? Math.min(Math.max(1, tier) * 2, 8) * 300 * (consumable ? 2 : 1) : 0;
                    for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                        assertEquals(10000 - reduction, GT_Pollution.getPollution(f.world.getChunkFromChunkCoords(x, z)));
                    assertEquals(polluted && consumable ? 1 : 2, filter.stackSize);
                    if (polluted && consumable) assertSame(Items.book, machine.mOutputItems[0].getItem());
                    else assertEquals(0, machine.mOutputItems.length);
                }
        } finally { GT_Mod.gregtechproxy.mPollution = previousPollution; }
    }

    private static void assemblyTierLimit(String ignored) {
        ItemList.Tool_DataStick.set(new ItemStack(Items.paper));
        for (int tier = 0; tier <= 11; tier++) for (boolean ordinary : new boolean[]{false, true})
            for (boolean mixed : new boolean[]{false, true}) for (int delta : new int[]{-1, 0, 1}) {
                if (ordinary && mixed) continue;
                GT_MetaTileEntity_AssemblyLine machine = new GT_MetaTileEntity_AssemblyLine("test");
                Fixture f = new Fixture(machine, tier);
                GT_MultiblockRecipeProcessingTest.supplyHatches(f, tier, 16, ordinary, mixed);
                long voltage = GT_Values.V[tier];
                long limit = mixed ? (voltage + GT_Values.V[tier + 1]) / 2 * 4 : voltage * 4;
                ItemStack ingredient = new ItemStack(Items.iron_ingot, 3);
                FluidStack liquid = new FluidStack(fluid("assembly_voltage_test"), 300);
                f.items(ingredient); f.liquid(liquid);
                NBTTagCompound recipe = new NBTTagCompound();
                recipe.setTag("0", new ItemStack(Items.iron_ingot, 2).writeToNBT(new NBTTagCompound()));
                recipe.setTag("f0", new FluidStack(liquid.getFluid(), 100).writeToNBT(new NBTTagCompound()));
                recipe.setTag("output", new ItemStack(Items.emerald).writeToNBT(new NBTTagCompound()));
                recipe.setInteger("time", 1); recipe.setInteger("eu", (int) limit + delta);
                machine.mInventory[1] = new ItemStack(Items.paper);
                machine.mInventory[1].setTagCompound(recipe);
                String context = "tier=" + tier + " ordinary=" + ordinary + " mixed=" + mixed + " delta=" + delta;
                assertEquals(context, delta <= 0, machine.checkRecipe(null));
                assertEquals(context, delta <= 0 ? 1 : 3, ingredient.stackSize);
                assertEquals(context, delta <= 0 ? 200 : 300, liquid.amount);
                if (delta > 0) {
                    assertEquals(context, 0, machine.mMaxProgresstime);
                    assertNull(context, machine.mOutputItems);
                }
            }
    }

    private static void assemblyLine(String ignored) throws Exception {
        ItemList.Tool_DataStick.set(new ItemStack(Items.paper));
        for (int tier = 0; tier < GT_Values.V.length; tier++) for (int amps : new int[]{0, 1, 4, 16, 64})
            for (int eut : new int[]{8, 24}) {
                long voltage = GT_Values.V[tier] * (amps == 0 ? 1 : amps);

                GT_MetaTileEntity_AssemblyLine machine = new GT_MetaTileEntity_AssemblyLine("test");
                Fixture f = new Fixture(machine, tier);
                if (amps != 0) {
                    machine.mEnergyHatches.clear();
                    GT_MetaTileEntity_Hatch_EnergyMulti hatch = new GT_MetaTileEntity_Hatch_EnergyMulti("test", tier, amps, "test", null);
                    f.attach(hatch); machine.mEnergyHatchesMulti.add(hatch);
                }
                ItemStack ingredient = new ItemStack(Items.iron_ingot, 3);
                f.items(ingredient);
                NBTTagCompound recipe = new NBTTagCompound();
                recipe.setTag("0", new ItemStack(Items.iron_ingot, 2).writeToNBT(new NBTTagCompound()));
                recipe.setTag("output", new ItemStack(Items.emerald).writeToNBT(new NBTTagCompound()));
                recipe.setInteger("time", 80); recipe.setInteger("eu", eut);
                machine.mInventory[1] = new ItemStack(Items.paper);
                machine.mInventory[1].setTagCompound(recipe);
                String context = "tier=" + tier + " amps=" + amps + " eut=" + eut;
                if (eut > voltage) {
                    assertFalse(context, machine.checkRecipe(null));
                    assertEquals(3, ingredient.stackSize);
                    continue;
                }
                assertTrue(context, machine.checkRecipe(null));
                // Shared 4x EU / 2x speed overclock, with long power and a strict one-operation limit.
                long expectedEnergy = eut;
                double duration = 80;
                for (long control = 32; control <= voltage / 4; control *= 4) {
                    expectedEnergy *= 4;
                    duration /= 2;
                }
                assertEquals(context, -expectedEnergy, machine.lEUt);
                assertEquals(context, Math.max(1, (int) duration), machine.mMaxProgresstime);
                assertEquals(1, machine.getCurrentParallel());
                assertEquals(1, ingredient.stackSize);
                assertSame(Items.emerald, machine.mOutputItems[0].getItem());
            }
    }
}
