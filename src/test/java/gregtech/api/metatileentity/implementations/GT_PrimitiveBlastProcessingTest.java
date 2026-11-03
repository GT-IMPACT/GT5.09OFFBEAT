package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.logic.InventoryProcessingOutputs;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Recipe;
import gregtech.common.GT_RecipeAdder;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BrickedBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BronzeBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_PrimitiveBlastFurnace;
import gregtech.loaders.misc.GT_Achievements;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GT_PrimitiveBlastProcessingTest {
    private static GameRuntime runtime;
    private static Item referenceItem;
    private final boolean bronze;

    public GT_PrimitiveBlastProcessingTest(boolean bronze) {
        this.bronze = bronze;
    }

    @Parameterized.Parameters(name = "bronze={0}")
    public static Object[] models() {
        return new Object[] { false, true };
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
        runtime.run(getClass(), scenario, Boolean.toString(bronze));
    }

    @Test
    public void allRegisteredFuelVariantsKeepInputsOutputsAndDuration() throws Throwable {
        run("referenceRecipes");
    }

    @Test
    public void shortAndMaximumDurationCyclesKeepTheirSpeedAndOneOperation() throws Throwable {
        run("durations");
    }

    @Test
    public void eachOutputSlotChecksTypeNbtAndExactCapacityBeforeConsumption() throws Throwable {
        run("outputCapacity");
    }

    @Test
    public void missingInputsAndStructureLeaveEveryStackUntouched() throws Throwable {
        run("refusals");
    }

    @Test
    public void fullOutputsAtCompletionRetainRemaindersAcrossReload() throws Throwable {
        run("waiting");
    }

    @Test
    public void activeLegacySaveCompletesWithoutSpendingInputsAgain() throws Throwable {
        run("legacySave");
    }

    @Test
    public void cachedRecipeStillChecksOutputsAndRecipeRemoval() throws Throwable {
        run("cache");
    }

    @Test
    public void replacingInputDuringOutputSimulationRejectsTheCommit() throws Throwable {
        run("inputChanged");
    }

    @Test
    public void poweredAndFluidRecipesCannotRunWithoutTheirResources() throws Throwable {
        run("unsupportedResources");
    }

    @Test
    public void poweredMachinesStillRequirePowerForZeroEuRecipes() throws Throwable {
        run("powerPolicy");
    }

    @Test
    public void sixWorkingSlotsKeepLiveInventoryHandlerBindings() throws Throwable {
        run("inventory");
    }

    @Test
    public void clearingPendingOutputsAlsoClearsReusedSaveTags() throws Throwable {
        run("staleSave");
    }

    public static void runScenario(String scenario, String bronze) throws Throwable {
        if (GT_Mod.achievements == null) {
            Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
            Constructor<?> constructor = (Constructor<?>) factoryClass
                    .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                    .invoke(factory, GT_Achievements.class, Object.class.getDeclaredConstructor());
            constructor.setAccessible(true);
            GT_Mod.achievements = (GT_Achievements) constructor.newInstance();
        }
        try {
            GT_PrimitiveBlastProcessingTest.class.getDeclaredMethod(scenario, boolean.class).invoke(null,
                    Boolean.parseBoolean(bronze));
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        } finally {
            map().clear();
        }
    }

    private static GT_Recipe.GT_Recipe_Map map() {
        return GT_Recipe.GT_Recipe_Map.sPrimitiveBlastRecipes;
    }

    private static void registerReferenceMaterials() {
        if (referenceItem != null) {
            return;
        }
        referenceItem = new Item().setHasSubtypes(true).setUnlocalizedName("primitive.reference");
        Item.itemRegistry.addObject(3011, "test.primitive.material", referenceItem);
        int meta = 0;
        for (Materials material : new Materials[] { Materials.Coal, Materials.Charcoal, Materials.CokeCoal }) {
            for (OrePrefixes prefix : new OrePrefixes[] { OrePrefixes.gem, OrePrefixes.dust, OrePrefixes.block }) {
                GT_OreDictUnificator.set(prefix, material, new ItemStack(referenceItem, 1, meta++));
            }
        }
        for (Materials material : new Materials[] { Materials.DarkAsh, Materials.Ash }) {
            for (OrePrefixes prefix : new OrePrefixes[] { OrePrefixes.dustTiny, OrePrefixes.dust }) {
                GT_OreDictUnificator.set(prefix, material, new ItemStack(referenceItem, 1, meta++));
            }
        }
    }

    private static void referenceRecipes(boolean bronze) throws Exception {
        registerReferenceMaterials();
        GregTech_API.sRecipeFile = GregTech_API.sMachineFile;
        GT_RecipeAdder adder = new GT_RecipeAdder();
        int[] inputAmounts = { 1, 1, 1, 1, 36 };
        int[] outputAmounts = { 1, 1, 9, 1, 36 };
        int[] fuelAmounts = { 4, 4, 36, 2, 2 };
        int[] durations = { 7200, 7200, 75000, 7200, 275 };
        for (int i = 0; i < durations.length; i++) {
            assertTrue(adder.addPrimitiveBlastRecipe(new ItemStack(referenceItem, inputAmounts[i], 20 + i), null,
                    fuelAmounts[i], new ItemStack(referenceItem, outputAmounts[i], 30 + i), null, durations[i]));
        }
        ArrayList<GT_Recipe> recipes = new ArrayList<>(map().mRecipeList);
        assertEquals(34, recipes.size());
        for (GT_Recipe recipe : recipes) {
            map().clear();
            map().addRecipe(recipe);
            Furnace furnace = new Furnace(bronze);
            ItemStack[] before = ProcessingInputs.copyItems(recipe.mInputs);
            furnace.inputs(recipe.mInputs);
            assertTrue(furnace.start());
            assertEquals(Math.max(1, recipe.mDuration / (bronze ? 4 : 1)), furnace.machine.mMaxProgresstime);
            for (int i = 0; i < 3; i++) {
                assertNull(furnace.machine.mInventory[i]);
            }
            furnace.finish();
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                assertTrue(ItemStack.areItemStacksEqual(recipe.mOutputs[i], furnace.machine.mInventory[3 + i]));
            }
            for (int i = 0; i < before.length; i++) {
                assertTrue(ItemStack.areItemStacksEqual(before[i], recipe.mInputs[i]));
            }
        }
    }

    private static void durations(boolean bronze) throws Exception {
        for (int duration : new int[] { 1, 2, 3, 4, 5, 7, 8, 127, 128, 129, 275, 7200, Integer.MAX_VALUE }) {
            map().clear();
            Furnace furnace = new Furnace(bronze);
            GT_Recipe recipe = recipe(duration, 0);
            furnace.inputs(recipe.mInputs);
            furnace.machine.mInventory[0].stackSize = 64;
            assertTrue(furnace.start());
            assertEquals(Math.max(1, duration / (bronze ? 4 : 1)), furnace.machine.mMaxProgresstime);
            assertEquals(62, furnace.machine.mInventory[0].stackSize);
            assertFalse(furnace.start());
            furnace.finish();
            assertEquals(2, furnace.machine.mInventory[3].stackSize);
            assertEquals(2, recipe.mOutputs[0].stackSize);
        }
    }

    private static void outputCapacity(boolean bronze) throws Exception {
        for (int slot = 0; slot < 3; slot++) {
            for (int free : new int[] { 1, 2 }) {
                map().clear();
                Furnace furnace = new Furnace(bronze);
                GT_Recipe recipe = recipe(80, 0);
                furnace.inputs(recipe.mInputs);
                ItemStack stored = recipe.mOutputs[slot].copy();
                stored.stackSize = 64 - free;
                furnace.machine.mInventory[slot + 3] = stored;
                assertEquals(free >= 2, furnace.start());
                if (free == 1) {
                    assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, furnace.machine.getProcessingResult());
                    furnace.assertInputs(recipe.mInputs);
                }
            }
            map().clear();
            Furnace furnace = new Furnace(bronze);
            GT_Recipe recipe = recipe(80, 0);
            furnace.inputs(recipe.mInputs);
            furnace.machine.mInventory[slot + 3] = new ItemStack(Items.diamond);
            assertFalse(furnace.start());
            furnace.assertInputs(recipe.mInputs);
            ItemStack tagged = recipe.mOutputs[slot].copy();
            tagged.stackTagCompound = new NBTTagCompound();
            tagged.stackTagCompound.setString("test", "different");
            furnace.machine.mInventory[slot + 3] = tagged;
            assertFalse(furnace.start());
            furnace.assertInputs(recipe.mInputs);
        }
    }

    private static void refusals(boolean bronze) throws Exception {
        GT_Recipe recipe = recipe(80, 0);
        Furnace furnace = new Furnace(bronze);
        furnace.inputs(recipe.mInputs);
        furnace.machine.mInventory[1].stackSize--;
        ItemStack[] before = Arrays.copyOf(furnace.machine.mInventory, 3);
        assertFalse(furnace.start());
        furnace.assertInputs(before);
        furnace.inputs(recipe.mInputs);
        furnace.machine.mMachine = false;
        assertFalse(furnace.start());
        assertEquals(CheckRecipeResult.MACHINE_CONDITION, furnace.machine.getProcessingResult());
        furnace.assertInputs(recipe.mInputs);
    }

    private static void waiting(boolean bronze) throws Exception {
        GT_Recipe recipe = recipe(80, 0);
        Furnace furnace = new Furnace(bronze);
        furnace.inputs(recipe.mInputs);
        assertTrue(furnace.start());
        furnace.machine.mInventory[3] = new ItemStack(Items.iron_ingot, 63);
        furnace.machine.mInventory[4] = new ItemStack(Items.diamond, 64);
        furnace.finish();
        assertTrue(furnace.machine.isWaitingForOutput());
        assertEquals(CheckRecipeResult.WAITING_FOR_OUTPUT, furnace.machine.getProcessingResult());
        assertFalse(furnace.tile.isActive());
        assertEquals(64, furnace.machine.mInventory[3].stackSize);
        assertEquals(1, furnace.machine.mOutputItems[0].stackSize);
        assertEquals(2, furnace.machine.mInventory[5].stackSize);
        assertFalse(furnace.start());
        Furnace restored = new Furnace(bronze);
        System.arraycopy(ProcessingInputs.copyItems(furnace.machine.mInventory), 0, restored.machine.mInventory, 0, 6);
        restored.machine.loadNBTData(furnace.save());
        restored.finish();
        assertTrue(restored.machine.isWaitingForOutput());
        assertEquals(2, restored.machine.mInventory[5].stackSize);
        restored.machine.mInventory[3] = null;
        restored.machine.mInventory[4] = null;
        restored.finish();
        assertFalse(restored.machine.isWaitingForOutput());
        assertEquals(1, restored.machine.mInventory[3].stackSize);
        assertEquals(2, restored.machine.mInventory[4].stackSize);
        assertEquals(2, restored.machine.mInventory[5].stackSize);
        restored.tick();
        assertEquals(1, restored.machine.mInventory[3].stackSize);
        assertEquals(2, recipe.mOutputs[0].stackSize);
    }

    private static void legacySave(boolean bronze) throws Exception {
        GT_Recipe recipe = recipe(80, 0);
        Furnace furnace = new Furnace(bronze);
        furnace.inputs(recipe.mInputs);
        assertTrue(furnace.start());
        furnace.tick();
        NBTTagCompound old = furnace.save();
        old.removeTag("waitingForOutput");
        Furnace restored = new Furnace(bronze);
        restored.inputs(recipe.mInputs);
        restored.machine.loadNBTData(old);
        assertEquals(1, restored.machine.mProgresstime);
        assertFalse(restored.start());
        map().clear();
        restored.finish();
        restored.assertInputs(recipe.mInputs);
        assertEquals(2, restored.machine.mInventory[3].stackSize);
    }

    private static void cache(boolean bronze) throws Exception {
        GT_Recipe recipe = recipe(80, 0);
        Furnace furnace = new Furnace(bronze);
        furnace.inputs(recipe.mInputs);
        assertTrue(furnace.start());
        furnace.finish();
        furnace.inputs(recipe.mInputs);
        furnace.machine.mInventory[3].stackSize = 64;
        assertFalse(furnace.start());
        furnace.assertInputs(recipe.mInputs);
        furnace.machine.mInventory[3] = null;
        map().clear();
        assertFalse(furnace.start());
        assertEquals(CheckRecipeResult.NO_RECIPE, furnace.machine.getProcessingResult());
        furnace.assertInputs(recipe.mInputs);
    }

    private static void inputChanged(boolean bronze) throws Exception {
        GT_Recipe recipe = recipe(80, 0);
        Furnace furnace = new Furnace(bronze);
        furnace.inputs(recipe.mInputs);
        new GT_StructureIntegrationTest.TestTile(furnace.world, furnace.machine, ForgeDirection.NORTH) {
            @Override
            public ItemStack getStackInSlot(int index) {
                if (index == 3) {
                    furnace.machine.mInventory[0] = new ItemStack(Items.diamond);
                }
                return super.getStackInSlot(index);
            }
        };
        assertFalse(furnace.start());
        assertEquals(CheckRecipeResult.INPUT_CHANGED, furnace.machine.getProcessingResult());
        assertEquals(1, furnace.machine.mInventory[0].stackSize);
        assertEquals(2, furnace.machine.mInventory[1].stackSize);
    }

    private static void unsupportedResources(boolean bronze) throws Exception {
        Furnace furnace = new Furnace(bronze);
        GT_Recipe powered = recipe(80, 1);
        furnace.inputs(powered.mInputs);
        assertFalse(furnace.start());
        assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, furnace.machine.getProcessingResult());
        furnace.assertInputs(powered.mInputs);
        map().clear();
        GT_Recipe fluid = recipe(80, 0);
        fluid.mFluidOutputs = new FluidStack[] { new FluidStack(fluid("test.primitive.output"), 1) };
        assertFalse(furnace.start());
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, furnace.machine.getProcessingResult());
        furnace.assertInputs(fluid.mInputs);
    }

    private static void powerPolicy(boolean bronze) {
        GT_Recipe recipe = recipe(80, 0);
        Furnace furnace = new Furnace(bronze);
        furnace.inputs(recipe.mInputs);
        ProcessingInputs inputs = new ProcessingInputs(Arrays.asList(Arrays.copyOf(furnace.machine.mInventory, 3)),
                Collections.emptyList());
        ProcessingLogic logic = new ProcessingLogic().setRecipeMapSupplier(GT_PrimitiveBlastProcessingTest::map);
        ProcessingLogic.Request request = new ProcessingLogic.Request(Collections.singletonList(inputs), 0, 0,
                ProcessingLogic.PROTECT_ITEMS, new InventoryProcessingOutputs(furnace.tile, 3, 3), bound -> 0);
        assertEquals(CheckRecipeResult.INSUFFICIENT_VOLTAGE, logic.process(request).status);
        furnace.assertInputs(recipe.mInputs);
    }

    private static void inventory(boolean bronze) {
        Furnace furnace = new Furnace(bronze);
        assertEquals(6, furnace.machine.mInventory.length);
        assertEquals(6, furnace.machine.getInventoryHandler().getSlots());
        for (int i = 0; i < 6; i++) {
            ItemStack stack = new ItemStack(i < 3 ? Items.apple : Items.iron_ingot, i + 1);
            furnace.machine.getInventoryHandler().setStackInSlot(i, stack);
            assertSame(stack, furnace.machine.mInventory[i]);
            assertSame(stack, furnace.machine.getInventoryHandler().getStackInSlot(i));
        }
        assertEquals(2, furnace.machine.getInventoryHandler().extractItem(3, 2, true).stackSize);
        assertEquals(4, furnace.machine.mInventory[3].stackSize);
        assertEquals(2, furnace.machine.getInventoryHandler().extractItem(3, 2, false).stackSize);
        assertEquals(2, furnace.machine.mInventory[3].stackSize);
        assertSame(furnace.machine.mInventory[3], furnace.machine.getInventoryHandler().getStackInSlot(3));
    }

    private static void staleSave(boolean bronze) throws Exception {
        Furnace furnace = new Furnace(bronze);
        GT_Recipe recipe = recipe(80, 0);
        furnace.inputs(recipe.mInputs);
        assertTrue(furnace.start());
        NBTTagCompound tag = furnace.save();
        assertTrue(tag.hasKey("mOutputItem0"));
        furnace.finish();
        furnace.machine.saveNBTData(tag);
        assertFalse(tag.hasKey("mOutputItem0"));
        assertFalse(tag.hasKey("mOutputItem1"));
        assertFalse(tag.hasKey("mOutputItem2"));
        assertEquals(0, tag.getInteger("mMaxProgresstime"));
    }

    private static GT_Recipe recipe(int duration, int eut) {
        GT_Recipe recipe = map().addRecipe(false,
                new ItemStack[] { new ItemStack(Items.apple, 2), new ItemStack(Items.coal, 2),
                        new ItemStack(Items.flint, 2) },
                new ItemStack[] { new ItemStack(Items.iron_ingot, 2), new ItemStack(Items.brick, 2),
                        new ItemStack(Items.gold_nugget, 2) },
                null, null, null, null, duration, eut, 0);
        assertNotNull(recipe);
        return recipe;
    }

    private static final class Furnace {
        final ProcessingWorld world = new ProcessingWorld();
        final GT_MetaTileEntity_PrimitiveBlastFurnace machine;
        final GT_StructureIntegrationTest.TestTile tile;

        Furnace(boolean bronze) {
            machine = bronze ? new GT_MetaTileEntity_BronzeBlastFurnace("test")
                    : new GT_MetaTileEntity_BrickedBlastFurnace("test");
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            tile.yCoord = 64;
            tile.disableWorking();
            machine.mMachine = true;
        }

        void inputs(ItemStack[] items) {
            Arrays.fill(machine.mInventory, 0, 3, null);
            for (int i = 0; i < items.length; i++) {
                machine.mInventory[i] = items[i] == null ? null : items[i].copy();
            }
        }

        void assertInputs(ItemStack[] items) {
            for (int i = 0; i < items.length; i++) {
                assertTrue("input " + i, ItemStack.areItemStacksEqual(items[i], machine.mInventory[i]));
            }
        }

        boolean start() throws Exception {
            Method method = GT_MetaTileEntity_PrimitiveBlastFurnace.class.getDeclaredMethod("checkRecipe");
            method.setAccessible(true);
            return (boolean) method.invoke(machine);
        }

        void tick() {
            machine.mUpdate = -1;
            machine.onPostTick(tile, 1);
        }

        void finish() {
            machine.mProgresstime = machine.mMaxProgresstime;
            tick();
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            return tag;
        }
    }
}
