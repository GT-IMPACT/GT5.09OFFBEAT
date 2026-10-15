package gregtech.loaders.postload;

import gregtech.api.util.GT_OreDictUnificator;
import ic2.core.AdvRecipe;
import ic2.core.AdvShapelessRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.List;

/** Uses preferred ingots and replaces redundant IC2 crafting ingredients with unified materials. */
public class GT_CraftingInputLoader implements Runnable {
    @Override
    public void run() {
        for (Object recipe : CraftingManager.getInstance().getRecipeList()) {
            if (recipe instanceof ShapedOreRecipe) {
                Object[] inputs = ((ShapedOreRecipe) recipe).getInput();
                for (int i = 0; i < inputs.length; i++) {
                    inputs[i] = unifyInput(inputs[i]);
                }
            } else if (recipe instanceof ShapelessOreRecipe) {
                unifyInputs(((ShapelessOreRecipe) recipe).getInput());
            } else if (recipe instanceof ShapedRecipes) {
                ItemStack[] inputs = ((ShapedRecipes) recipe).recipeItems;
                for (int i = 0; i < inputs.length; i++) {
                    inputs[i] = GT_OreDictUnificator.getPreferredRecipeInput(inputs[i]);
                }
            } else if (recipe instanceof ShapelessRecipes) {
                unifyInputs(((ShapelessRecipes) recipe).recipeItems);
            } else if (recipe instanceof AdvRecipe) {
                unifyIC2Inputs(((AdvRecipe) recipe).input);
                unifyIC2Inputs(((AdvRecipe) recipe).inputMirrored);
            } else if (recipe instanceof AdvShapelessRecipe) {
                unifyIC2Inputs(((AdvShapelessRecipe) recipe).input);
            }
        }
    }

    private static void unifyIC2Inputs(Object[] inputs) {
        if (inputs == null) return;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] == null) continue;
            List<ItemStack> alternatives = AdvRecipe.expand(inputs[i]);
            Object unified = unifyInput(alternatives);
            if (unified != alternatives) inputs[i] = unified;
        }
    }

    private static void unifyInputs(List inputs) {
        for (int i = 0; i < inputs.size(); i++) {
            inputs.set(i, unifyInput(inputs.get(i)));
        }
    }

    private static Object unifyInput(Object input) {
        if (input instanceof ItemStack) return GT_OreDictUnificator.getPreferredRecipeInput((ItemStack) input);
        if (!(input instanceof List)) return input;

        List<?> alternatives = (List<?>) input;
        // Forge recipes can share the actual OreDictionary list. Keep changes local to crafting.
        List<Object> unified = new ArrayList<Object>(alternatives.size());
        boolean changed = false;
        for (Object alternative : alternatives) {
            Object replacement = alternative instanceof ItemStack
                ? GT_OreDictUnificator.getPreferredRecipeInput((ItemStack) alternative) : alternative;
            changed |= replacement != alternative;
            unified.add(replacement);
        }
        if (!changed) return input;

        for (int i = unified.size() - 1; i >= 0; i--) {
            if (!(unified.get(i) instanceof ItemStack)) continue;
            for (int j = 0; j < i; j++) {
                if (unified.get(j) instanceof ItemStack
                    && ItemStack.areItemStacksEqual((ItemStack) unified.get(i), (ItemStack) unified.get(j))) {
                    unified.remove(i);
                    break;
                }
            }
        }
        return unified;
    }

}
