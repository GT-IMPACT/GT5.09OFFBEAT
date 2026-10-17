package gregtech.api.util;

import gregtech.api.GregTech_API;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.objects.ItemData;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

/** Recipe candidates for exact items and their equivalents, prepared during registration. */
public final class GT_RecipeItemRegistry {
    private final Map<GT_ItemStack, Collection<GT_Recipe>> exactRecipes;
    private final Map<GT_ItemStack, Collection<GT_Recipe>> equivalentRecipes = new HashMap<>();
    private final Map<GT_ItemStack, Collection<GT_Recipe>> lookupRecipes = new HashMap<>();

    public GT_RecipeItemRegistry(Map<GT_ItemStack, Collection<GT_Recipe>> exactRecipes) {
        this.exactRecipes = exactRecipes;
        GregTech_API.sItemStackMappings.add(equivalentRecipes);
        GregTech_API.sItemStackMappings.add(lookupRecipes);
    }

    public void register(GT_Recipe recipe) {
        for (ItemStack input : recipe.mInputs) {
            if (input == null) continue;
            register(exactRecipes, new GT_ItemStack(input), recipe);
            ItemData inputData = GT_OreDictUnificator.getAssociation(input);
            if (inputData == null) continue;
            // Register known ore equivalents now; postload reInit handles later associations.
            for (ItemStack equivalent : GT_OreDictUnificator.getOres(inputData.toString())) {
                if (recipe.isCircuitInputEqual(equivalent, input)) {
                    register(equivalentRecipes, new GT_ItemStack(equivalent), recipe);
                    continue;
                }
                ItemData data = GT_OreDictUnificator.getAssociation(equivalent);
                if (data == null || !data.mBlackListed || GT_Utility.areStacksEqual(input, equivalent, true)) continue;
                if (!GT_Utility.areStacksEqual(input, GT_OreDictUnificator.get(false, equivalent), true)) continue;
                register(equivalentRecipes, new GT_ItemStack(equivalent), recipe);
            }
        }
    }

    private void register(Map<GT_ItemStack, Collection<GT_Recipe>> registry, GT_ItemStack key, GT_Recipe recipe) {
        Collection<GT_Recipe> recipes = registry.get(key);
        if (recipes == null) registry.put(key, recipes = new HashSet<GT_Recipe>(1));
        recipes.add(recipe);
        Collection<GT_Recipe> exact = exactRecipes.get(key);
        Collection<GT_Recipe> equivalents = equivalentRecipes.get(key);
        if (exact == null || equivalents == null) {
            lookupRecipes.put(key, exact == null ? equivalents : exact);
        } else {
            Collection<GT_Recipe> combined = new LinkedHashSet<>(exact);
            combined.addAll(equivalents);
            lookupRecipes.put(key, combined);
        }
    }

    public Collection<GT_Recipe> get(GT_ItemStack key) {
        return lookupRecipes.get(key);
    }

    public boolean contains(GT_ItemStack key) {
        return lookupRecipes.containsKey(key);
    }

    public void clear() {
        exactRecipes.clear();
        equivalentRecipes.clear();
        lookupRecipes.clear();
    }
}
