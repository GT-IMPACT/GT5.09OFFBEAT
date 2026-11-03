package gregtech.api.util.item;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** GTNH single-slot view over a non-transferable item template. */
public class PhantomSingleSlotItemStackHandler implements IItemHandlerModifiable {

    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> setter;

    public PhantomSingleSlotItemStackHandler(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void setStackInSlot(int slot, @Nullable ItemStack stack) {
        if (slot == 0) {
            setter.accept(stack);
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? getter.get() : null;
    }

    @Nullable
    @Override
    public ItemStack insertItem(int slot, @Nullable ItemStack stack, boolean simulate) {
        return null;
    }

    @Nullable
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 0;
    }
}
