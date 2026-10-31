package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

/** Characterization tests: expected values describe this fork, including legacy quirks. */
@RunWith(Parameterized.class)
public class GT_MultiblockRecipeProcessingTest {
    private static GameRuntime runtime;
    private final String model;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> machines() {
        return Arrays.asList(new Object[][]{{"VacuumFreezer"}, {"MultiblockCentrifuge"},
                {"MultiblockElectrolyzer"}, {"LargeChemicalReactor"}, {"IndustrialPulverizer"},
                {"ImplosionCompressor"}, {"ElectricBlastFurnace"}, {"OilCracker"}, {"DistillationTower"}});
    }

    public GT_MultiblockRecipeProcessingTest(String model) { this.model = model; }
    @BeforeClass public static void openGame() throws Exception { runtime = new GameRuntime(); }
    @AfterClass public static void closeGame() throws Exception { if (runtime != null) runtime.close(); }
    private void run(String scenario) throws Throwable { runtime.run(getClass(), scenario, model); }

    @Test public void successfulRecipeConsumesExactlyOneBatch() throws Throwable { run("success"); }
    @Test public void emptyInputsDoNotStart() throws Throwable { run("empty"); }
    @Test public void missingIngredientDoesNotConsumeOtherInputs() throws Throwable { run("shortage"); }
    @Test public void wrongIngredientDoesNotStart() throws Throwable { run("wrong"); }
    @Test public void voltageBoundaryPreservesMachineSpecificBehavior() throws Throwable { run("voltage"); }
    @Test public void overclockPreservesDurationAndEnergy() throws Throwable { run("overclock"); }
    @Test public void outputSelectionAndChanceAreMachineSpecific() throws Throwable { run("outputs"); }
    @Test public void runningRecipeSurvivesNbtRoundTrip() throws Throwable { run("nbt"); }
    @Test public void nextRecipeDoesNotReuseMismatchedCache() throws Throwable { run("cache"); }
    @Test public void allVoltageTiersAndOneTickRecipes() throws Throwable { run("tiers"); }
    @Test public void multiAmpAndMixedEnergyHatches() throws Throwable { run("amps"); }
    @Test public void voltageImmediatelyBelowAtAndAboveEveryTier() throws Throwable { run("boundaries"); }
    @Test public void recipeEUtImmediatelyBelowAtAndAboveEveryTier() throws Throwable { run("recipeBoundaries"); }
    @Test public void disabledRecipeDoesNotStartOrConsumeInputs() throws Throwable { run("disabled"); }
    @Test public void amperageCannotUnlockRecipesMoreThanOneTierAboveHatches() throws Throwable { run("tierLimit"); }
    @Test public void mixedHatchesUseUnweightedVoltageForRecipeLimit() throws Throwable { run("mixedTierLimit"); }
    @Test public void cachedRecipeIsRejectedAfterVoltageDowngrade() throws Throwable { run("cachedTierLimit"); }

    public static void runScenario(String scenario, String model) throws Exception {
        if (scenario.equals("tierLimit") || scenario.equals("mixedTierLimit")) {
            checkRecipeTierLimit(model, scenario.equals("mixedTierLimit"));
            return;
        }
        if (scenario.equals("cachedTierLimit")) {
            RecipeFixture f = new RecipeFixture(model, 3);
            try {
                f.register(480, 1); f.fill();
                assertTrue(f.machine.checkRecipe(null));
                f.machine.mInputBusses.clear();
                f.machine.mInputHatches.clear();
                f.machine.mMaxProgresstime = 0;
                f.machine.mEUt = 0;
                f.fill();
                supplyHatches(f.fixture, 1, 16, false, false);
                assertFalse("Cached HV recipe must not run on LV 16A", f.machine.checkRecipe(null));
                f.assertUnconsumed();
            } finally { f.map.clear(); GregTech_API.sPostloadFinished = f.previousPostload; }
            return;
        }
        if (scenario.equals("recipeBoundaries")) {
            checkRecipeBoundaries(model);
            return;
        }
        if (scenario.equals("tiers") || scenario.equals("amps") || scenario.equals("boundaries")) {
            checkPowerMatrix(model, scenario);
            return;
        }
        RecipeFixture f = new RecipeFixture(model, scenario.equals("overclock") ? 2 : 1);
        try {
            switch (scenario) {
                case "disabled":
                    f.register(24, 80); f.fill(); f.recipe.mEnabled = false;
                    assertFalse(f.machine.checkRecipe(null)); f.assertUnconsumed();
                    return;
                case "empty":
                    f.register(24, 80);
                    assertFalse(f.machine.checkRecipe(null));
                    assertEquals(0, f.machine.mMaxProgresstime);
                    return;
                case "voltage":
                    f.register(33, 80);
                    f.fill();
                    boolean implosion = model.equals("ImplosionCompressor");
                    assertEquals("Implosion searches without a voltage ceiling", implosion,
                            f.machine.checkRecipe(null));
                    if (!implosion) f.assertUnconsumed();
                    else assertEquals(-33, f.machine.mEUt);
                    return;
                case "shortage":
                    f.register(24, 80);
                    f.fill();
                    if (f.fluidOnly()) f.water.amount = 99;
                    else f.iron.stackSize = 1;
                    assertFalse(f.machine.checkRecipe(null));
                    assertEquals(f.fluidOnly() ? 99 : 300, f.water == null ? 300 : f.water.amount);
                    if (f.iron != null) assertEquals(1, f.iron.stackSize);
                    assertEquals(0, f.machine.mMaxProgresstime);
                    return;
                case "wrong":
                    f.register(24, 80);
                    f.fill();
                    if (f.fluidOnly()) f.machine.mInputHatches.get(0).mFluid = new FluidStack(fluid("wrong"), 300);
                    else f.machine.mInputBusses.get(0).mInventory[0] = new ItemStack(Items.diamond, 6);
                    assertFalse(f.machine.checkRecipe(null));
                    assertEquals(0, f.machine.mMaxProgresstime);
                    return;
                default:
                    f.register(24, 80);
                    f.fill();
                    assertTrue(model, f.machine.checkRecipe(null));
                    f.assertConsumed();
                    assertEquals(scenario.equals("overclock") ? -96 : -24, f.machine.mEUt);
                    assertEquals(scenario.equals("overclock") ? (model.equals("IndustrialPulverizer") ? 20 : 40) : 80,
                            f.machine.mMaxProgresstime);
                    assertEquals(10000, f.machine.mEfficiency);
                    assertEquals(10000, f.machine.mEfficiencyIncrease);
                    if (scenario.equals("outputs") || scenario.equals("success")) f.assertOutputs();
                    if (scenario.equals("nbt")) f.assertSaved();
                    if (scenario.equals("cache")) {
                        f.machine.mInputBusses.clear();
                        f.machine.mInputHatches.clear();
                        f.machine.mInventory[1] = null;
                        f.fixture.items(new ItemStack(Items.diamond, 8));
                        f.fixture.liquid(new FluidStack(fluid("wrong"), 300));
                        assertFalse("Cached recipe must still match the next input", f.machine.checkRecipe(null));
                    }
            }
        } finally {
            f.map.clear();
            GregTech_API.sPostloadFinished = f.previousPostload;
        }
    }

    private static void checkRecipeTierLimit(String model, boolean mixed) throws Exception {
        for (int tier = 0; tier <= 12; tier++) for (boolean ordinary : new boolean[]{false, true})
            for (int amps : mixed ? new int[]{16, 64} : new int[]{4, 16, 64}) for (int delta : new int[]{-1, 0, 1}) {
                // Mixed configuration has one multiamp hatch plus one ordinary hatch a tier above.
                if (mixed && ordinary) continue;
                long voltage = GT_Values.V[tier];
                if (voltage * amps + (mixed ? GT_Values.V[tier + 1] : 0) > GT_Values.V[15]) continue;
                long limit = mixed ? (voltage + GT_Values.V[tier + 1]) / 2 * 4 : voltage * 4;
                RecipeFixture f = new RecipeFixture(model, tier);
                try {
                    supplyHatches(f.fixture, tier, amps, ordinary, mixed);
                    f.register((int) limit + delta, 1); f.fill();
                    String context = model + " tier=" + tier + " amps/count=" + amps
                            + " ordinary=" + ordinary + " mixed=" + mixed + " delta=" + delta;
                    assertEquals(context, delta <= 0, f.machine.checkRecipe(null));
                    if (delta <= 0) f.assertConsumed();
                    else f.assertUnconsumed();
                } finally { f.map.clear(); GregTech_API.sPostloadFinished = f.previousPostload; }
            }
    }

    static void supplyHatches(Fixture f, int tier, int amps, boolean ordinary, boolean mixed) {
        f.machine.mEnergyHatches.clear();
        f.machine.mEnergyHatchesMulti.clear();
        if (ordinary) {
            for (int i = 0; i < amps; i++) {
                GT_MetaTileEntity_Hatch_Energy hatch = new GT_MetaTileEntity_Hatch_Energy("test", tier, "test", null);
                f.attach(hatch); f.machine.mEnergyHatches.add(hatch);
            }
        } else {
            GT_MetaTileEntity_Hatch_EnergyMulti hatch = new GT_MetaTileEntity_Hatch_EnergyMulti("test", tier, amps, "test", null);
            f.attach(hatch); f.machine.mEnergyHatchesMulti.add(hatch);
        }
        if (mixed) {
            GT_MetaTileEntity_Hatch_Energy hatch = new GT_MetaTileEntity_Hatch_Energy("test", tier + 1, "test", null);
            f.attach(hatch); f.machine.mEnergyHatches.add(hatch);
        }
    }

    private static void checkRecipeBoundaries(String model) throws Exception {
        for (int tier = 0; tier < GT_Values.V.length; tier++) for (int delta : new int[]{-1, 0, 1}) {
            int eut = (int) GT_Values.V[tier] + delta;
            RecipeFixture f = new RecipeFixture(model, tier);
            try {
                f.register(eut, 1); f.fill();
                boolean accepted = model.equals("ImplosionCompressor")
                        || (tier == 0 && !model.equals("DistillationTower")) || delta <= 0;
                String context = model + " tier=" + tier + " recipeEUt=" + eut;
                assertEquals(context, accepted, f.machine.checkRecipe(null));
                if (!accepted) { f.assertUnconsumed(); continue; }
                int expected = model.equals("IndustrialPulverizer") ? eut / 2 : tier == 0 ? eut / 4 : eut;
                assertEquals(context, -expected, f.machine.mEUt);
                assertEquals(context, model.equals("IndustrialPulverizer") || tier == 0 ? 2 : 1, f.machine.mMaxProgresstime);
                f.assertConsumed();
            } finally { f.map.clear(); GregTech_API.sPostloadFinished = f.previousPostload; }
        }
    }

    // Frozen expectations for a 24 EU/t, 80-tick recipe. The falling high-tier EU/t
    // values intentionally preserve the current sub-tick behavior.
    private static final int[] ENERGY = {6, 24, 96, 384, 1536, 6144, 24576, 98304,
            49152, 24576, 12288, 6144, 3072, 1536, 768, 768};
    private static final int[] DURATION = {160, 80, 40, 20, 10, 5, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private static final int[] ONE_TICK_ENERGY = {6, 24, 12, 6, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    private static void checkPowerMatrix(String model, String scenario) throws Exception {
        for (int tier = 0; tier < GT_Values.V.length; tier++) {
            int[] variants = scenario.equals("amps") ? new int[]{1, 2, 4, 8, 16, 32, 64, 256, 1024}
                    : scenario.equals("boundaries") ? new int[]{-1, 0, 1} : new int[]{80, 1};
            for (int variant : variants) {
                for (int mixed = 0; mixed < (scenario.equals("amps") ? 2 : 1); mixed++) {
                    long voltage = scenario.equals("amps") ? GT_Values.V[tier] * variant + (mixed == 1 ? 32 : 0)
                            : GT_Values.V[tier] + (scenario.equals("boundaries") ? variant : 0);
                    if (voltage > GT_Values.V[15]) continue; // No recipe-search tier exists above MAX.
                    int effectiveTier = 0;
                    while (voltage > GT_Values.V[effectiveTier]) effectiveTier++;
                    int duration = scenario.equals("tiers") ? variant : 80;
                    RecipeFixture f = new RecipeFixture(model, tier);
                    try {
                        if (scenario.equals("amps")) {
                            f.machine.mEnergyHatches.clear();
                            GT_MetaTileEntity_Hatch_EnergyMulti hatch = new GT_MetaTileEntity_Hatch_EnergyMulti(
                                    "test", tier, variant, "test", null);
                            f.fixture.attach(hatch);
                            f.machine.mEnergyHatchesMulti.add(hatch);
                            if (mixed == 1) {
                                GT_MetaTileEntity_Hatch_Energy ordinary = new GT_MetaTileEntity_Hatch_Energy(
                                        "test", 1, "test", null);
                                f.fixture.attach(ordinary);
                                f.machine.mEnergyHatches.add(ordinary);
                            }
                        } else if (scenario.equals("boundaries")) {
                            // Probe the voltage-to-tier boundary independently of fixed hatch tiers.
                            final long supplied = voltage;
                            new GT_StructureIntegrationTest.TestTile(f.fixture.world, f.machine.mEnergyHatches.get(0),
                                    net.minecraftforge.common.util.ForgeDirection.NORTH) {
                                @Override public long getInputVoltage() { return supplied; }
                            };
                        }
                        String context = model + ", voltage=" + voltage + ", variant=" + variant + ", mixed=" + mixed;
                        assertEquals(context, voltage, f.machine.getMaxInputVoltage());
                        f.register(24, duration);
                        f.fill();
                        if (model.equals("DistillationTower") && effectiveTier == 0) {
                            assertFalse(context, f.machine.checkRecipe(null));
                            f.assertUnconsumed();
                            continue;
                        }
                        assertTrue(context, f.machine.checkRecipe(null));
                        int expectedEnergy = duration == 1 ? ONE_TICK_ENERGY[effectiveTier] : ENERGY[effectiveTier];
                        int expectedDuration = duration == 1 ? (effectiveTier == 0 ? 2 : 1) : DURATION[effectiveTier];
                        if (model.equals("IndustrialPulverizer")) {
                            expectedEnergy = duration == 1 ? 12 : effectiveTier <= 1 ? 24
                                    : effectiveTier == 2 ? 96 : effectiveTier == 3 ? 384 : 960;
                            expectedDuration = duration == 1 ? 2 : effectiveTier <= 1 ? 80
                                    : effectiveTier == 2 ? 20 : effectiveTier == 3 ? 5 : 2;
                        }
                        assertEquals(context + " EU/t", -expectedEnergy, f.machine.mEUt);
                        assertEquals(context + " ticks", expectedDuration, f.machine.mMaxProgresstime);
                        f.assertConsumed();
                    } finally {
                        f.map.clear();
                        GregTech_API.sPostloadFinished = f.previousPostload;
                    }
                }
            }
        }
    }

    static GT_MetaTileEntity_MultiBlockBase machine(String name) throws Exception {
        return (GT_MetaTileEntity_MultiBlockBase) Class.forName(
                "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_" + name)
                .getConstructor(String.class).newInstance("processing-test");
    }

    static final class RecipeFixture {
        final String model;
        final GT_MetaTileEntity_MultiBlockBase machine;
        final Fixture fixture;
        final GT_Recipe.GT_Recipe_Map map;
        final boolean previousPostload = GregTech_API.sPostloadFinished;
        ItemStack iron;
        ItemStack stick;
        FluidStack water;
        GT_Recipe recipe;

        RecipeFixture(String model, int tier) throws Exception {
            this.model = model;
            machine = machine(model);
            fixture = new Fixture(machine, tier);
            map = model.equals("MultiblockCentrifuge") ? GT_Recipe.GT_Recipe_Map.sMultiblockCentrifugeRecipes
                    : model.equals("LargeChemicalReactor") ? GT_Recipe.GT_Recipe_Map.sMultiblockChemicalRecipes
                    : model.equals("OilCracker") ? GT_Recipe.GT_Recipe_Map.sCrakingRecipes : machine.getRecipeMap();
            assertTrue("Test classloader must start with an empty map", map.mRecipeList.isEmpty());
            GregTech_API.sPostloadFinished = true;
            if (model.equals("ElectricBlastFurnace")) setField(machine, "mHeatingCapacity", 1800);
        }

        boolean fluidOnly() { return model.equals("OilCracker") || model.equals("DistillationTower"); }
        boolean implosion() { return model.equals("ImplosionCompressor"); }

        void register(int eut, int duration) {
            ItemStack[] inputs = model.equals("DistillationTower") ? null : model.equals("OilCracker")
                    ? new ItemStack[]{new ItemStack(Items.stick, 0)}
                    : new ItemStack[]{new ItemStack(Items.iron_ingot, 2), new ItemStack(Items.stick, 1)};
            FluidStack[] fluids = implosion() ? null : new FluidStack[]{
                    new FluidStack(fluid("processing_water"), 100), new FluidStack(fluid("processing_reagent"), 50)};
            recipe = map.addRecipe(false, inputs,
                    new ItemStack[]{new ItemStack(Items.gold_ingot, 3), new ItemStack(Items.flint, 2)}, null,
                    new int[]{10000, 1}, fluids,
                    implosion() ? null : new FluidStack[]{new FluidStack(fluid("processing_product"), 75)},
                    duration, eut, model.equals("ElectricBlastFurnace") ? 1800 : 0);
            assertNotNull("Recipe registration", recipe);
        }

        void fill() {
            if (!fluidOnly()) {
                iron = new ItemStack(Items.iron_ingot, 6);
                stick = new ItemStack(Items.stick, 3);
                fixture.items(iron, stick);
            } else if (model.equals("OilCracker")) {
                stick = new ItemStack(Items.stick);
                machine.mInventory[1] = stick;
            }
            if (!implosion()) {
                water = new FluidStack(fluid("processing_water"), 300);
                fixture.liquid(water);
                fixture.liquid(new FluidStack(fluid("processing_reagent"), 150));
                if (model.equals("OilCracker")) {
                    // Ordinary feedstocks belong to the outer rings, not the reagent ring.
                    for (GT_MetaTileEntity_Hatch_Input hatch : machine.mInputHatches)
                        ((net.minecraft.tileentity.TileEntity) hatch.getBaseMetaTileEntity()).xCoord = 2;
                }
            }
        }

        void assertConsumed() {
            if (iron != null) assertEquals(4, iron.stackSize);
            if (stick != null) assertEquals(model.equals("OilCracker") ? 1 : 2, stick.stackSize);
            if (water != null) {
                assertEquals(200, water.amount);
                assertEquals(100, machine.mInputHatches.get(1).mFluid.amount);
            }
            // Starting a recipe prepares outputs; it does not deliver them yet.
            assertEquals(0, machine.mProgresstime);
        }

        void assertUnconsumed() {
            if (iron != null) assertEquals(6, iron.stackSize);
            if (water != null) assertEquals(300, water.amount);
            assertEquals(0, machine.mMaxProgresstime);
        }

        void assertOutputs() {
            if (model.equals("OilCracker")) assertNull(machine.mOutputItems);
            else {
                assertEquals(model.equals("VacuumFreezer") || model.equals("DistillationTower") ? 1 : 2,
                        machine.mOutputItems.length);
                assertTrue(machine.mOutputItems[0].isItemEqual(new ItemStack(Items.gold_ingot)));
                assertEquals(3, machine.mOutputItems[0].stackSize);
                if (machine.mOutputItems.length == 2) {
                    if (model.equals("ElectricBlastFurnace") || model.equals("LargeChemicalReactor"))
                        assertEquals("These machines ignore output chances", 2, machine.mOutputItems[1].stackSize);
                    else assertNull("Seeded rare output must miss", machine.mOutputItems[1]);
                }
            }
            if (!implosion()) {
                assertEquals(1, machine.mOutputFluids.length);
                assertEquals(75, machine.mOutputFluids[0].amount);
                assertSame(fluid("processing_product"), machine.mOutputFluids[0].getFluid());
            }
        }

        void assertSaved() throws Exception {
            machine.mProgresstime = 17;
            machine.mPollution = 42;
            machine.mRuntime = 123;
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            GT_MetaTileEntity_MultiBlockBase restored = machine(model);
            new Fixture(restored, 1);
            restored.loadNBTData(tag);
            assertEquals(-24, restored.mEUt);
            assertEquals(17, restored.mProgresstime);
            assertEquals(80, restored.mMaxProgresstime);
            assertEquals(10000, restored.mEfficiency);
            assertEquals(10000, restored.mEfficiencyIncrease);
            assertEquals(42, restored.mPollution);
            assertEquals(123, restored.mRuntime);
            if (machine.mOutputItems != null) {
                assertEquals(machine.mOutputItems.length, restored.mOutputItems.length);
                for (int i = 0; i < machine.mOutputItems.length; i++)
                    assertTrue(ItemStack.areItemStacksEqual(machine.mOutputItems[i], restored.mOutputItems[i]));
            }
            if (machine.mOutputFluids != null) {
                assertEquals(machine.mOutputFluids.length, restored.mOutputFluids.length);
                for (int i = 0; i < machine.mOutputFluids.length; i++)
                    assertTrue(machine.mOutputFluids[i].isFluidStackIdentical(restored.mOutputFluids[i]));
            }
        }
    }
}
