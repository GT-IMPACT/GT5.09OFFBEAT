package gregtech.api.logic;

import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** A per-attempt snapshot. Only commit() writes to the original stacks. */
public final class ProcessingInputs {
    private final ItemStack[] originals;
    private final ItemStack[] beforeItems;
    private final FluidStack[] originalFluids;
    private final FluidStack[] beforeFluids;
    private final BooleanSupplier sourceUnchanged;
    public final ItemStack[] items;
    public final FluidStack[] fluids;

    public ProcessingInputs(Collection<ItemStack> items, Collection<FluidStack> fluids) {
        this(items, fluids, () -> true);
    }

    public ProcessingInputs(Collection<ItemStack> items, Collection<FluidStack> fluids,
            BooleanSupplier sourceUnchanged) {
        originals = unique(items).toArray(new ItemStack[0]);
        originalFluids = unique(fluids).toArray(new FluidStack[0]);
        beforeItems = copyItems(originals);
        beforeFluids = copyFluids(originalFluids);
        this.items = copyItems(originals);
        this.fluids = copyFluids(originalFluids);
        this.sourceUnchanged = sourceUnchanged;
    }

    private ProcessingInputs(ProcessingInputs source) {
        originals = source.originals;
        originalFluids = source.originalFluids;
        beforeItems = source.beforeItems;
        beforeFluids = source.beforeFluids;
        sourceUnchanged = source.sourceUnchanged;
        items = copyItems(source.items);
        fluids = copyFluids(source.fluids);
    }

    public ProcessingInputs copy() {
        return new ProcessingInputs(this);
    }

    private static <T> List<T> unique(Collection<T> values) {
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
        List<T> result = new ArrayList<>();
        for (T value : values) {
            if (value != null && seen.add(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public static ItemStack[] copyItems(ItemStack[] values) {
        ItemStack[] result = new ItemStack[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] == null ? null : values[i].copy();
        }
        return result;
    }

    public static FluidStack[] copyFluids(FluidStack[] values) {
        FluidStack[] result = new FluidStack[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] == null ? null : values[i].copy();
        }
        return result;
    }

    /**
     * Greedy consumption on this disposable snapshot, including zero-count
     * catalysts.
     */
    public boolean consume(GT_Recipe recipe, int count) {
        for (ItemStack required : recipe.mInputs) {
            if (required == null) {
                continue;
            }
            long left = Math.multiplyExact((long) required.stackSize, count);
            if (left < 0) {
                return false;
            }
            boolean found = false;
            for (ItemStack supplied : items) {
                if (supplied == null || !recipe.matchesProcessingInput(supplied, required)) {
                    continue;
                }
                found = true;
                int take = (int) Math.min(left, Math.max(0, supplied.stackSize));
                supplied.stackSize -= take;
                left -= take;
                if (left == 0) {
                    break;
                }
            }
            if (!found || left != 0) {
                return false;
            }
        }
        for (FluidStack required : recipe.mFluidInputs) {
            if (required == null) {
                continue;
            }
            long left = Math.multiplyExact((long) required.amount, count);
            if (left < 0) {
                return false;
            }
            boolean found = false;
            for (FluidStack supplied : fluids) {
                if (supplied == null || !supplied.isFluidEqual(required)) {
                    continue;
                }
                found = true;
                int take = (int) Math.min(left, Math.max(0, supplied.amount));
                supplied.amount -= take;
                left -= take;
                if (left == 0) {
                    break;
                }
            }
            if (!found || left != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean commit() {
        if (!sourceUnchanged.getAsBoolean()) {
            return false;
        }
        for (int i = 0; i < originals.length; i++) {
            if (!ItemStack.areItemStacksEqual(originals[i], beforeItems[i])) {
                return false;
            }
        }
        for (int i = 0; i < originalFluids.length; i++) {
            if (!originalFluids[i].isFluidStackIdentical(beforeFluids[i])) {
                return false;
            }
        }
        for (int i = 0; i < originals.length; i++) {
            originals[i].stackSize = items[i].stackSize;
        }
        for (int i = 0; i < originalFluids.length; i++) {
            originalFluids[i].amount = fluids[i].amount;
        }
        return true;
    }
}
