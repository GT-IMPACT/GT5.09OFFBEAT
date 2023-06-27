package gregtech.api.util;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;

import static gregtech.api.util.GT_ModHandler.addCraftingRecipe;

public class GT_RecipeBuilderCrafting {

    private final ArrayList<Object> shape;
    private final ArrayList<Object> inputs;
    private ItemStack result;

    public static GT_RecipeBuilderCrafting create() {
        return new GT_RecipeBuilderCrafting();
    }

    public GT_RecipeBuilderCrafting() {
        shape = new ArrayList<>();
        inputs = new ArrayList<>();
    }

    public GT_RecipeBuilderCrafting addShape(String... row) {
        shape.addAll(Arrays.asList(row));
        return this;
    }

    public GT_RecipeBuilderCrafting addInput(char shape, Object stack) {
        inputs.add(shape);
        inputs.add(stack);
        return this;
    }

    public void addResult(ItemStack stack) {
        Object[] recipe = new Object[shape.size() + inputs.size()];
        int index = 0;
        for (Object o : shape) recipe[index++] = o;
        for (Object o : inputs) recipe[index++] = o;
        addCraftingRecipe(stack, recipe);
    }
}
