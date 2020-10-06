package gregtech.loaders.oreprocessing;

import cpw.mods.fml.common.Loader;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import static com.impact.common.item.Core_Items.Core_Items1;

public class ProcessingLog implements gregtech.api.interfaces.IOreRecipeRegistrator {
    public ProcessingLog() {
        OrePrefixes.log.add(this);
    }

    public void registerOre(OrePrefixes aPrefix, Materials aMaterial, String aOreDictName, String aModName, ItemStack aStack) {
        if (aOreDictName.equals("logRubber")) {
            GT_Values.RA.addCentrifugeRecipe(GT_Utility.copyAmount(1L, new Object[]{aStack}), null, null, Materials.Methane.getGas(60L), ItemList.IC2_Resin.get(1L, new Object[0]), GT_ModHandler.getIC2Item("plantBall", 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1L), null, null, null, new int[]{5000, 3750, 2500}, 200, 20);
            GT_ModHandler.addExtractionRecipe(GT_Utility.copyAmount(1L, new Object[]{aStack}), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 1L));
        }

        GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Wood, 2L), gregtech.api.util.GT_ModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS | gregtech.api.util.GT_ModHandler.RecipeBits.BUFFERED, new Object[]{"sLf", 'L', GT_Utility.copyAmount(1L, new Object[]{aStack})});
        GT_Values.RA.addAssemblerRecipe(GT_Utility.copyAmount(1L, new Object[]{aStack}), ItemList.Circuit_Integrated.getWithDamage(0L, 2L, new Object[0]), Materials.SeedOil.getFluid(50L), ItemList.FR_Stick.get(1L, new Object[0]), 16, 8);
        GT_Values.RA.addAssemblerRecipe(GT_Utility.copyAmount(8L, new Object[]{aStack}), ItemList.Circuit_Integrated.getWithDamage(0L, 8L, new Object[0]), Materials.SeedOil.getFluid(250L), ItemList.FR_Casing_Impregnated.get(1L, new Object[0]), 64, 16);
        short aMeta = (short) aStack.getItemDamage();

        if (aMeta == Short.MAX_VALUE) {
            if ((GT_Utility.areStacksEqual(GT_ModHandler.getSmeltingOutput(GT_Utility.copyAmount(1L, new Object[]{aStack}), false, null), new ItemStack(Items.coal, 1, 1)))) {
                addPyrolyeOvenRecipes(aStack);
                if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, "wood2charcoalsmelting", true)) {
                    GT_ModHandler.removeFurnaceSmelting(GT_Utility.copyAmount(1L, new Object[]{aStack}));
                }
            }
            for (int i = 0; i < 32767; i++) {
                if ((GT_Utility.areStacksEqual(GT_ModHandler.getSmeltingOutput(new ItemStack(aStack.getItem(), 1, i), false, null), new ItemStack(Items.coal, 1, 1)))) {
                    addPyrolyeOvenRecipes(aStack);
                    if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, "wood2charcoalsmelting", true)) {
                        GT_ModHandler.removeFurnaceSmelting(new ItemStack(aStack.getItem(), 1, i));
                    }
                }
                ItemStack tStack = GT_ModHandler.getRecipeOutput(new ItemStack[]{new ItemStack(aStack.getItem(), 1, i)});
                if (tStack == null) {
                    if (i >= 16) {
                        break;
                    }
                }
                else {

                    ItemStack tPlanks = GT_Utility.copy(new Object[]{tStack});
                    GT_ModHandler.removeRecipe(new ItemStack[]{new ItemStack(aStack.getItem(), 1, i)});
                    GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(3, new Object[]{tStack}), new Object[]{"s", "L", "L", 'L', new ItemStack(aStack.getItem(), 1, i)});
                    if (Loader.isModLoaded("impact")) {
                        GT_Values.RA.addSawMill(new ItemStack[]{new ItemStack(aStack.getItem(), 8, i)}, new ItemStack[]{new ItemStack(tPlanks.getItem(), 13, i), Core_Items1.getRecipe(39, 5)}, null, 15 * 20, 8, 0);
                        GT_Values.RA.addSawMill(new ItemStack[]{GT_Utility.copyAmount(8, aStack)}, new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 15), Core_Items1.getRecipe(39, 5)}, new FluidStack[]{GT_ModHandler.getWater(250)}, 20 * 20, 8, 1);
                        GT_Values.RA.addSawMill(new ItemStack[]{GT_Utility.copyAmount(8, aStack)}, new ItemStack[]{Core_Items1.getRecipe(39, 21)}, null, 25 * 20, 8, 2);
                    }
                }
            }
        } else {
            if ((GT_Utility.areStacksEqual(GT_ModHandler.getSmeltingOutput(GT_Utility.copyAmount(1L, new Object[]{aStack}), false, null), new ItemStack(Items.coal, 1, 1)))) {
                addPyrolyeOvenRecipes(aStack);
                if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, "wood2charcoalsmelting", true)) {
                    GT_ModHandler.removeFurnaceSmelting(GT_Utility.copyAmount(1L, new Object[]{aStack}));
                }
            }
            ItemStack tStack = GT_ModHandler.getRecipeOutput(new ItemStack[]{GT_Utility.copyAmount(1L, new Object[]{aStack})});
            if (tStack != null) {
                ItemStack tPlanks = GT_Utility.copy(new Object[]{tStack});
                tPlanks.stackSize = (tPlanks.stackSize * 3 / 2);
                GT_ModHandler.removeRecipe(new ItemStack[]{GT_Utility.copyAmount(1L, new Object[]{aStack})});
                GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(3, new Object[]{tStack}), new Object[]{"s", "L", "L", 'L', GT_Utility.copyAmount(1L, new Object[]{aStack})});
            }
        }

        if ((GT_Utility.areStacksEqual(GT_ModHandler.getSmeltingOutput(GT_Utility.copyAmount(1L, new Object[]{aStack}), false, null), new ItemStack(Items.coal, 1, 1)))) {
            addPyrolyeOvenRecipes(aStack);
            if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, "wood2charcoalsmelting", true))
                GT_ModHandler.removeFurnaceSmelting(GT_Utility.copyAmount(1L, new Object[]{aStack}));
        }
    }
    public static void addPyrolyeOvenRecipes(ItemStack logStack){
        GT_Values.RA.addCokeOvenRecipes(GT_Utility.copyAmount(6L, new Object[]{logStack}), Materials.Charcoal.getGems(1), Materials.Creosote.getFluid(100), 90*20);
    }
}
