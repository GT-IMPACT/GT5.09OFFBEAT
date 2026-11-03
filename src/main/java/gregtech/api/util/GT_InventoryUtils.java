package gregtech.api.util;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** GTNH inventory compaction, retaining its NBT-sensitive grouping and end-to-front merging. */
public final class GT_InventoryUtils {

    private static final ThreadLocal<Map<StackKey, List<Integer>>> groupsByItem =
        ThreadLocal.withInitial(HashMap::new);

    private GT_InventoryUtils() {
    }

    public static boolean compactInventory(ItemStack[] inventory, int start, int end) {
        List<ItemStack> slots = Arrays.asList(inventory).subList(start, end);
        Map<StackKey, List<Integer>> groups = groupsByItem.get();
        groups.clear();
        for (int index = 0; index < slots.size(); index++) {
            ItemStack stack = slots.get(index);
            if (stack != null) {
                groups.computeIfAbsent(new StackKey(stack), ignored -> new ArrayList<>()).add(index);
            }
        }
        boolean changed = false;
        for (List<Integer> group : groups.values()) {
            int insert = 0;
            int extract = group.size() - 1;
            while (insert < group.size() && insert < extract) {
                ItemStack target = slots.get(group.get(insert));
                int maxStack = target.getMaxStackSize();
                int remaining = maxStack - target.stackSize;
                while (insert < extract) {
                    int sourceIndex = group.get(extract);
                    ItemStack source = slots.get(sourceIndex);
                    int transfer = Math.min(source.stackSize, remaining);
                    source.stackSize -= transfer;
                    target.stackSize += transfer;
                    remaining -= transfer;
                    changed = true;
                    if (source.stackSize <= 0) {
                        slots.set(sourceIndex, null);
                        extract--;
                    }
                    if (target.stackSize >= maxStack) {
                        break;
                    }
                }
                insert++;
            }
        }
        int insert = 0;
        while (insert < slots.size()) {
            if (slots.get(insert) == null) {
                ItemStack stack = null;
                int extract = insert + 1;
                while (extract < slots.size() && (stack = slots.get(extract)) == null) {
                    extract++;
                }
                if (stack == null) {
                    break;
                }
                slots.set(insert, stack);
                slots.set(extract, null);
                changed = true;
            }
            insert++;
        }
        groups.clear();
        return changed;
    }

    /** Replaces GTNH's fastutil custom hash strategy with the same equality on JDK map keys. */
    private static final class StackKey {

        private final ItemStack stack;

        private StackKey(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int hashCode() {
            int hash = stack.getItem().hashCode();
            hash = 31 * hash + stack.getItemDamage();
            return 31 * hash + Objects.hashCode(stack.getTagCompound());
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StackKey && GT_Utility.areStacksEqual(stack, ((StackKey) other).stack);
        }
    }
}
