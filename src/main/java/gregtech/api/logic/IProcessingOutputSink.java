package gregtech.api.logic;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Delivery updates the pending arrays to contain only unaccepted results. */
public interface IProcessingOutputSink extends ProcessingOutputAccess {

    boolean deliver(ItemStack[] items, FluidStack[] fluids, boolean protectItems, boolean protectFluids);
}
