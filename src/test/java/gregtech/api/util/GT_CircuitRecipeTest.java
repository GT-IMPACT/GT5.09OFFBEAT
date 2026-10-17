package gregtech.api.util;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.oredict.OreDictionary;
import org.junit.Test;

import static org.junit.Assert.*;

public class GT_CircuitRecipeTest {
    @Test
    public void circuitAlternativesShareOneRecipe() throws Exception {
        // FML and Minecraft static initializers require LaunchWrapper, even in a unit test.
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LaunchClassLoader previousLaunch = Launch.classLoader;
        java.util.Map<String, Object> previousBlackboard = Launch.blackboard;
        try (LaunchClassLoader loader = new LaunchClassLoader(((URLClassLoader) getClass().getClassLoader()).getURLs())) {
            Launch.classLoader = loader;
            Launch.blackboard = new HashMap<>();
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class)
                .invoke(null, (Object) new Object[]{"7", "99", "40", "1614", "1.7.10", "9.05",
                    new File("."), new ArrayList<String>()});
            loader.loadClass(getClass().getName()).getMethod("runChecks").invoke(null);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            Launch.classLoader = previousLaunch;
            Launch.blackboard = previousBlackboard;
        }
    }

    public static void runChecks() throws Exception {
        Field side = cpw.mods.fml.relauncher.FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, cpw.mods.fml.relauncher.Side.SERVER);
        Bootstrap.func_151354_b();
        Field mods = Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true);
        mods.set(Loader.instance(), new HashMap<String, ModContainer>());
        ItemStack first = new ItemStack(Items.diamond);
        ItemStack second = new ItemStack(Items.emerald);
        ItemStack otherTier = new ItemStack(Items.quartz);
        circuit(first, Materials.Basic);
        circuit(second, Materials.Basic);
        circuit(otherTier, Materials.Good);
        GregTech_API.sUnificationEntriesRegistered = true;

        GT_Recipe.GT_Recipe_Map map = GT_Recipe.GT_Recipe_Map.sAssemblerRecipes;
        map.clear();
        ItemStack[] suppliedInputs = {GT_Utility.copyAmount(2, second)};
        GT_Recipe recipe = map.addRecipeWithCircuitOreDict(false, suppliedInputs,
            new ItemStack[]{otherTier}, null, 1200, 500000, -200);
        assertNotNull(recipe);
        assertEquals(1, map.mRecipeList.size());
        assertTrue(ItemStack.areItemStacksEqual(otherTier, recipe.mOutputs[0]));
        assertSame(recipe, map.findRecipe(null, false, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, first)));
        assertSame(recipe, map.findRecipe(null, false, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, second)));
        assertSame(recipe, map.findRecipe(null, true, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, first)));
        assertNull(map.findRecipe(null, false, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, otherTier)));
        assertTrue(map.containsInput(first));
        assertTrue(map.containsInput(second));
        assertFalse(map.containsInput(otherTier));
        assertEquals(2, suppliedInputs[0].stackSize);
        assertSame(second.getItem(), suppliedInputs[0].getItem());

        assertFalse(recipe.isRecipeInputEqual(false, false, null, first));
        assertEquals(1, first.stackSize);
        ItemStack a = GT_Utility.copyAmount(1, first);
        ItemStack b = GT_Utility.copyAmount(2, second);
        assertTrue(recipe.isRecipeInputEqual(true, false, null, a, b));
        assertEquals(0, a.stackSize);
        assertEquals(1, b.stackSize);
        assertTrue(recipe.copy().isRecipeInputEqual(false, false, null, GT_Utility.copyAmount(2, first)));
        Object display = recipe.getInputAlternatives(0);
        assertTrue(display instanceof ItemStack[]);
        assertEquals(2, ((ItemStack[]) display).length);
        for (ItemStack alternative : (ItemStack[]) display) assertEquals(2, alternative.stackSize);

        assertNull(map.addRecipeWithCircuitOreDict(false, new ItemStack[]{GT_Utility.copyAmount(2, first)},
            new ItemStack[]{otherTier}, null, 1200, 500000, -200));
        assertEquals(1, map.mRecipeList.size());
        map.reInit();
        assertSame(recipe, map.findRecipe(null, false, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, first)));
        assertEquals(1, map.mRecipeList.size());

        // A later ore registration becomes reachable after the normal postload rebuild.
        ItemStack late = new ItemStack(Items.coal);
        circuit(late, Materials.Basic);
        map.reInit();
        assertSame(recipe, map.findRecipe(null, false, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, late)));
        assertEquals(3, ((ItemStack[]) recipe.getInputAlternatives(0)).length);

        map.clear();
        net.minecraftforge.fluids.FluidStack water = new net.minecraftforge.fluids.FluidStack(
            net.minecraftforge.fluids.FluidRegistry.WATER, 576);
        GT_Recipe mixed = map.addRecipeWithCircuitOreDict(false,
            new ItemStack[]{GT_Utility.copyAmount(2, second), otherTier}, new ItemStack[]{new ItemStack(Items.stick)},
            new net.minecraftforge.fluids.FluidStack[]{water}, 1200, 500000, -200);
        assertNotNull(mixed);
        assertEquals(1, map.mRecipeList.size());
        assertEquals(-200, mixed.mSpecialValue);
        assertSame(mixed, map.findRecipe(null, false, Long.MAX_VALUE,
            new net.minecraftforge.fluids.FluidStack[]{water}, GT_Utility.copyAmount(2, first), otherTier));
        assertNull(map.findRecipe(null, false, Long.MAX_VALUE, null, GT_Utility.copyAmount(2, first), otherTier));
        assertFalse(mixed.isRecipeInputEqual(false, false,
            new net.minecraftforge.fluids.FluidStack[]{water}, GT_Utility.copyAmount(3, first)));

        map.clear();
        GT_Recipe exact = map.addRecipe(false, new ItemStack[]{GT_Utility.copyAmount(2, second)},
            new ItemStack[]{first}, null, (net.minecraftforge.fluids.FluidStack[]) null, null, 1200, 500000, 0);
        assertNotNull(exact);
        assertFalse(exact.isRecipeInputEqual(false, false, null, GT_Utility.copyAmount(2, first)));
        assertTrue(exact.isRecipeInputEqual(false, false, null, GT_Utility.copyAmount(2, second)));
        assertTrue(exact.getInputAlternatives(0) instanceof ItemStack);
        assertTrue(ItemStack.areItemStacksEqual(first, exact.mOutputs[0]));
        map.clear();
    }

    private static void circuit(ItemStack stack, Materials tier) {
        GT_OreDictUnificator.addToBlacklist(stack);
        GT_OreDictUnificator.addAssociation(OrePrefixes.circuit, tier, stack, true);
        OreDictionary.registerOre(OrePrefixes.circuit.get(tier).toString(), stack);
    }
}
