package gregtech.api.logic;

import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** Saves operation counts without vanilla ItemStack NBT's signed byte limit. */
public final class ProcessingPersistence {
    private ProcessingPersistence() {
    }

    public static NBTTagCompound recipeIdentity(GT_Recipe.GT_Recipe_Map map, GT_Recipe recipe) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("map", map.mUnlocalizedName);
        tag.setInteger("eu", recipe.mEUt);
        tag.setInteger("duration", recipe.mDuration);
        tag.setInteger("special", recipe.mSpecialValue);
        writeItems(tag, "in", recipe.mInputs);
        writeItems(tag, "out", recipe.mOutputs);
        writeFluids(tag, "fin", recipe.mFluidInputs);
        writeFluids(tag, "fout", recipe.mFluidOutputs);
        tag.setIntArray("chances", recipe.mChances);
        return tag;
    }

    public static void writeItems(NBTTagCompound tag, String key, ItemStack[] items) {
        tag.setInteger(key + "Size", items == null ? 0 : items.length);
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    NBTTagCompound item = items[i].writeToNBT(new NBTTagCompound());
                    item.setInteger("Count", items[i].stackSize);
                    tag.setTag(key + i, item);
                }
            }
        }
    }

    public static void writeFluids(NBTTagCompound tag, String key, FluidStack[] fluids) {
        tag.setInteger(key + "Size", fluids == null ? 0 : fluids.length);
        if (fluids != null) {
            for (int i = 0; i < fluids.length; i++) {
                if (fluids[i] != null) {
                    tag.setTag(key + i, fluids[i].writeToNBT(new NBTTagCompound()));
                }
            }
        }
    }

    public static ItemStack[] readItems(NBTTagCompound tag, String key) {
        ItemStack[] items = new ItemStack[tag.getInteger(key + "Size")];
        for (int i = 0; i < items.length; i++) {
            if (tag.hasKey(key + i)) {
                NBTTagCompound item = tag.getCompoundTag(key + i);
                items[i] = ItemStack.loadItemStackFromNBT(item);
                if (items[i] != null) {
                    items[i].stackSize = item.getInteger("Count");
                }
            }
        }
        return items;
    }

    public static FluidStack[] readFluids(NBTTagCompound tag, String key) {
        FluidStack[] fluids = new FluidStack[tag.getInteger(key + "Size")];
        for (int i = 0; i < fluids.length; i++) {
            if (tag.hasKey(key + i)) {
                fluids[i] = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag(key + i));
            }
        }
        return fluids;
    }
}
