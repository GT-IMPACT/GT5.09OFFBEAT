package gregtech.api.gui;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Display totals without changing the stacks used for output delivery. */
public final class GT_ProcessingOutputSummary {
    private GT_ProcessingOutputSummary() {}

    public static List<Entry<ItemStack>> items(ItemStack[] stacks) {
        return summarize(stacks,
                stack -> Arrays.asList(stack.getItem(), stack.getItemDamage(), stack.getTagCompound()),
                stack -> stack.stackSize);
    }

    public static List<Entry<FluidStack>> fluids(FluidStack[] stacks) {
        return summarize(stacks, stack -> Arrays.asList(stack.getFluid(), stack.tag), stack -> stack.amount);
    }

    private static <T> List<Entry<T>> summarize(T[] stacks, Function<T, ?> key, ToLongFunction<T> amount) {
        Map<Object, Entry<T>> totals = new LinkedHashMap<>();
        if (stacks != null) {
            for (T stack : stacks) {
                if (stack == null || amount.applyAsLong(stack) <= 0) {
                    continue;
                }
                Object identity = key.apply(stack);
                Entry<T> previous = totals.get(identity);
                totals.put(identity, new Entry<>(previous == null ? stack : previous.stack,
                        amount.applyAsLong(stack) + (previous == null ? 0 : previous.amount)));
            }
        }
        List<Entry<T>> result = new ArrayList<>(totals.values());
        result.sort(Comparator.comparingLong((Entry<T> entry) -> entry.amount).reversed());
        return result;
    }

    public static final class Entry<T> {
        public final T stack;
        public final long amount;

        private Entry(T stack, long amount) {
            this.stack = stack;
            this.amount = amount;
        }
    }
}
