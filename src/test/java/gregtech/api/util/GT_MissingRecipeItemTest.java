package gregtech.api.util;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.enums.GT_Values;
import gregtech.api.GregTech_API;
import gregtech.common.items.ItemDebug;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import org.junit.Test;

import static org.junit.Assert.*;

public class GT_MissingRecipeItemTest {
    @Test
    public void missingItemsRemainVisible() throws Exception {
        // FML and Minecraft static initializers require LaunchWrapper, even in a unit test.
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LaunchClassLoader previousLaunch = Launch.classLoader;
        Map<String, Object> previousBlackboard = Launch.blackboard;
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
        Field side = FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, Side.SERVER);
        Bootstrap.func_151354_b();
        Field mods = Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true);
        mods.set(Loader.instance(), new HashMap<String, ModContainer>());
        GregTech_API.sPreloadStarted = true;
        GT_ModHandler.stopBufferingCraftingRecipes();
        GT_Values.debugRecipes = false;
        assertNull(GT_ModHandler.getModItem("missing_test_mod", "removed", 3));
        assertFalse(GT_ModHandler.addCraftingRecipe(new ItemStack(Items.diamond), new Object[]{"X", 'X', null}));

        GT_Values.debugRecipes = true;
        ItemStack missing = GT_ModHandler.getModItem("missing_test_mod", "removed", 3, 42);
        assertSame(ItemDebug.getInstance(), missing.getItem());
        assertEquals(3, missing.stackSize);
        assertEquals(0, missing.getItemDamage());
        assertEquals("missing_test_mod:removed:42", missing.getTagCompound().getString("MissingItem"));
        assertFalse(GT_Utility.isDebugItem(missing));
        assertSame(missing.getItem(), GT_ModHandler.getModItem("missing_test_mod", "removed", 2).getItem());
        ItemStack replacement = new ItemStack(Items.emerald);
        assertSame(Items.emerald, GT_ModHandler.getModItem("missing_test_mod", "removed", 2, replacement).getItem());
        assertEquals(1, replacement.stackSize);
        assertSame(missing.getItem(), GT_ModHandler.getIC2Item("missing_test_item", 2, 17).getItem());
        assertEquals(0, ItemDebug.missing("catalyst", 0).stackSize);

        GT_Recipe.GT_Recipe_Map map = GT_Recipe.GT_Recipe_Map.sAssemblerRecipes;
        map.clear();
        GT_Recipe recipe = map.addRecipe(false, new ItemStack[]{missing},
            new ItemStack[]{new ItemStack(Items.diamond)}, null, null, null, 20, 8, 0);
        assertNotNull(recipe);
        assertSame(missing.getItem(), recipe.mInputs[0].getItem());
        assertEquals("missing_test_mod:removed:42", recipe.mInputs[0].getTagCompound().getString("MissingItem"));
        assertTrue(GT_ModHandler.addCraftingRecipe(new ItemStack(Items.diamond), new Object[]{"X", 'X', null}));
        List recipes = CraftingManager.getInstance().getRecipeList();
        ShapedOreRecipe shaped = (ShapedOreRecipe) recipes.get(recipes.size() - 1);
        assertSame(missing.getItem(), ((ItemStack) shaped.getInput()[0]).getItem());
        assertTrue(GT_ModHandler.addShapelessCraftingRecipe(new ItemStack(Items.emerald), new Object[]{null}));
        ShapelessOreRecipe shapeless = (ShapelessOreRecipe) recipes.get(recipes.size() - 1);
        assertSame(missing.getItem(), ((ItemStack) shapeless.getInput().get(0)).getItem());
        assertTrue(GT_ModHandler.addCraftingRecipe(null, new Object[]{"X", 'X', new ItemStack(Items.stick)}));
        assertSame(missing.getItem(), ((IRecipe) recipes.get(recipes.size() - 1)).getRecipeOutput().getItem());
        assertTrue(GT_ModHandler.addShapelessCraftingRecipe(null, new Object[]{new ItemStack(Items.stick)}));
        GT_Values.debugRecipes = false;
        assertNull(GT_ModHandler.getModItem("missing_test_mod", "removed", 3, 42));
    }
}
