package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Output preparation and capacity checks shared by hatch and controller
 * inventories.
 */
public interface ProcessingOutputAccess {
    CheckRecipeResult canFit(ItemStack[] items, FluidStack[] fluids, boolean protectItems, boolean protectFluids);

    default FluidStack[] prepareFluids(FluidStack[] fluids) {
        return ProcessingInputs.copyFluids(fluids);
    }

    boolean areFluidOutputsEmpty();
}
