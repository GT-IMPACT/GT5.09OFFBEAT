package gregtech.api.metatileentity.implementations;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.items.storage.ItemVoidStorageCell;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import gregtech.api.util.GTWaila;
import gregtech.api.util.GT_Utility;
import mcp.mobius.waila.api.IWailaDataAccessor;

/** Cell capacity, local-cell mode and flush timing, adapted from GTNH's ME output provider. */
public class GT_MEOutputStorage<T extends IAEStack<T>> implements ISaveProvider {

    public static final int MIN_REFRESH_TICKS = 40;
    private final StorageChannel channel;
    private final long defaultCapacity;
    private final Supplier<ItemStack> cellStack;
    private final Supplier<AENetworkProxy> proxy;
    private final Supplier<BaseActionSource> source;
    private final Runnable dirty;
    private final Consumer<T> returnToCache;
    private final ToLongFunction<T> bufferedAmount;
    private final List<T> partition = new ArrayList<>();
    private boolean blacklist;
    private boolean voidCell;
    private ItemStack previousCell;
    private IMEInventoryHandler<T> cell;
    private IMEInventoryHandler<T> readableCell;
    private boolean cacheMode;
    private boolean checkMode;
    private int priority;
    private boolean wasActive;
    private int refreshTicks = MIN_REFRESH_TICKS;
    private long lastOutputTick;
    private long lastInputTick = Long.MIN_VALUE;
    private long currentTick;

    public GT_MEOutputStorage(
        StorageChannel channel,
        long defaultCapacity,
        Supplier<ItemStack> cellStack,
        Supplier<AENetworkProxy> proxy,
        Supplier<BaseActionSource> source,
        Runnable dirty,
        Consumer<T> returnToCache
    ) {
        this(
            channel,
            defaultCapacity,
            cellStack,
            proxy,
            source,
            dirty,
            returnToCache,
            stack -> 0
        );
    }

    public GT_MEOutputStorage(
        StorageChannel channel,
        long defaultCapacity,
        Supplier<ItemStack> cellStack,
        Supplier<AENetworkProxy> proxy,
        Supplier<BaseActionSource> source,
        Runnable dirty,
        Consumer<T> returnToCache,
        ToLongFunction<T> bufferedAmount
    ) {
        this.channel = channel;
        this.defaultCapacity = defaultCapacity;
        this.cellStack = cellStack;
        this.proxy = proxy;
        this.source = source;
        this.dirty = dirty;
        this.returnToCache = returnToCache;
        this.bufferedAmount = bufferedAmount;
    }

    protected IMEInventoryHandler<T> loadCell(ItemStack stack) {
        return AEApi.instance().registries().cell().getCellInventory(stack, this, channel);
    }

    public boolean isValidCell(ItemStack stack) {
        if (stack == null || stack.stackSize != 1) {
            return false;
        }
        if (stack.getItem() instanceof ItemVoidStorageCell) {
            return matchesChannel(((ItemVoidStorageCell) stack.getItem()).getStackType());
        }
        if (stack.getItem() instanceof IStorageCell) {
            IStorageCell candidate = (IStorageCell) stack.getItem();
            return matchesChannel(candidate.getStackType()) && candidate.isStorageCell(stack);
        }
        return false;
    }

    private boolean matchesChannel(IAEStackType<?> type) {
        return type == (channel == StorageChannel.ITEMS ? ITEM_STACK_TYPE : FLUID_STACK_TYPE);
    }

    public void onCellChanged() {
        ItemStack stack = cellStack.get();
        if (stack == previousCell) {
            return;
        }
        ItemStack oldCell = previousCell;
        initializeCell();
        if (cacheMode) {
            updateNetwork(oldCell, stack);
        }
        dirty.run();
    }

    public void initializeCell() {
        ItemStack stack = cellStack.get();
        if (stack != previousCell) {
            IMEInventoryHandler<T> loaded = stack == null ? null : loadCell(stack);
            previousCell = stack;
            cell = loaded;
            voidCell = stack != null && stack.getItem() instanceof ItemVoidStorageCell;
            updatePartition(stack);
            updateReadableCell();
        }
    }

    @SuppressWarnings("unchecked")
    private void updatePartition(ItemStack stack) {
        partition.clear();
        blacklist = false;
        if (stack == null || !(stack.getItem() instanceof ICellWorkbenchItem)) {
            return;
        }
        ICellWorkbenchItem workbenchCell = (ICellWorkbenchItem) stack.getItem();
        IInventory upgrades = workbenchCell.getUpgradesInventory(stack);
        for (int slot = 0; slot < upgrades.getSizeInventory(); slot++) {
            ItemStack upgrade = upgrades.getStackInSlot(slot);
            if (upgrade != null && upgrade.getItem() instanceof IUpgradeModule
                    && ((IUpgradeModule) upgrade.getItem()).getType(upgrade) == Upgrades.INVERTER) {
                blacklist = true;
                break;
            }
        }
        IAEStackInventory configured = workbenchCell.getConfigAEInventory(stack);
        for (int slot = 0; slot < configured.getSizeInventory(); slot++) {
            IAEStack<?> filter = configured.getAEStackInSlot(slot);
            if (channel == StorageChannel.ITEMS && filter instanceof IAEItemStack
                    || channel == StorageChannel.FLUIDS && filter instanceof IAEFluidStack) {
                partition.add((T) filter.copy());
            }
        }
    }

    private void updateReadableCell() {
        readableCell = null;
        if (cell != null) {
            MEInventoryHandler<T> readOnly = new MEInventoryHandler<T>(cell, (IAEStackType<T>) cell.getStackType());
            readOnly.setBaseAccess(AccessRestriction.READ);
            readOnly.setPriority(priority);
            readableCell = readOnly;
        }
    }

    public long getCapacity() {
        if (voidCell) {
            return Long.MAX_VALUE;
        }
        if (cell instanceof ICellInventoryHandler) {
            ICellInventoryHandler<T> handler = (ICellInventoryHandler<T>) cell;
            if (handler.getCellInv() != null) {
                return handler.getCellInv().getStoredItemCount() + handler.getCellInv().getRemainingItemCount();
            }
        }
        return defaultCapacity;
    }

    public List<T> getCellContents() {
        List<T> contents = new ArrayList<>();
        if (cell != null) {
            IItemList<T> available = cell.getAvailableItems(
                    (IItemList<T>) cell.getStackType().createPrimitiveList(), IterationCounter.fetchNewId());
            for (T stack : available) {
                contents.add(stack.copy());
            }
        }
        return contents;
    }

    /**
     * Scanner and WAILA follow GTNH's MTEHatchOutputMEBase, including fixes #7854 and #8593.
     * The legacy scanner API accepts translated strings instead of GTNH's encoded translation arguments.
     */
    public String[] getInfoData(List<T> cached, String langBaseKey, Function<T, String> nameGetter) {
        List<String> info = new ArrayList<>();
        AENetworkProxy grid = proxy.get();
        info.add(grid != null && grid.isActive()
                ? StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.bus.online")
                : StatCollector.translateToLocalFormatted("GT5U.infodata.hatch.crafting_input_me.bus.offline", ""));
        String unit = channel == StorageChannel.FLUIDS ? " L" : "";
        info.add(StatCollector.translateToLocalFormatted(langBaseKey + ".cache_capacity",
                EnumChatFormatting.GOLD + GT_Utility.formatNumbers(getCapacity()) + unit + EnumChatFormatting.RESET));
        processInfoData(langBaseKey, nameGetter, cached, info, unit);
        if (cacheMode && cell != null) {
            info.add(StatCollector.translateToLocal("GT5U.waila.hatch.outputme.storage_cache"));
            processInfoData(langBaseKey, nameGetter, getCellContents(), info, unit);
        }
        return info.toArray(new String[0]);
    }

    private void processInfoData(
        String langBaseKey,
        Function<T, String> nameGetter,
        List<T> stacks,
        List<String> info,
        String unit
    ) {
        if (stacks.isEmpty()) {
            info.add(StatCollector.translateToLocal(langBaseKey + ".empty"));
        } else {
            info.add(StatCollector.translateToLocalFormatted(langBaseKey + ".contains", stacks.size()));
            for (int i = 0; i < Math.min(100, stacks.size()); i++) {
                T stack = stacks.get(i);
                info.add(nameGetter.apply(stack) + ": " + EnumChatFormatting.GOLD
                        + GT_Utility.formatNumbers(stack.getStackSize()) + unit + EnumChatFormatting.RESET);
            }
        }
    }

    private void processWailaNBTData(NBTTagCompound tag, String listKey, String countKey, List<T> stacks) {
        tag.setInteger(countKey, stacks.size());
        NBTTagList list = new NBTTagList();
        tag.setTag(listKey, list);
        stacks.sort((a, b) -> Long.compare(b.getStackSize(), a.getStackSize()));
        for (int i = 0; i < Math.min(10, stacks.size()); i++) {
            T stack = stacks.get(i);
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBT(stackTag);
            stackTag.setLong("Amount", stack.getStackSize());
            list.appendTag(stackTag);
        }
    }

    public void getWailaNBTData(NBTTagCompound tag, List<T> cached) {
        tag.setLong("cacheCapacity", getCapacity());
        tag.setInteger("refreshTime", refreshTicks);
        tag.setInteger("ticksToFlush", (int) Math.max(0, lastOutputTick + refreshTicks - currentTick));
        processWailaNBTData(tag, "stacks", "stackCount", cached);
        if (cacheMode && cell != null) {
            processWailaNBTData(tag, "cacheStacks", "cacheCount", getCellContents());
        } else {
            tag.removeTag("cacheStacks");
            tag.removeTag("cacheCount");
        }
    }

    /** Client rendering from GTNH's WailaHelper; names are resolved from the synchronized NBT. */
    public static class WailaHelper {

        private static String getLocalizedName(String prefix, NBTTagCompound tag) {
            if ("fluid".equals(prefix)) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag);
                return fluid == null ? null : fluid.getLocalizedName();
            }
            ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
            return stack == null ? null : stack.getDisplayName();
        }

        private static int processWailaCache(
            String prefix,
            List<String> tooltip,
            String listKey,
            String countKey,
            NBTTagCompound tag,
            boolean showStackCount
        ) {
            NBTTagList stacks = tag.getTagList(listKey, 10);
            int stackCount = tag.getInteger(countKey);
            if (stackCount == 0) {
                tooltip.add(StatCollector.translateToLocal("GT5U.waila.hatch.outputme." + prefix + "_cache_empty"));
                return 0;
            }
            if (showStackCount) {
                tooltip.add(StatCollector.translateToLocalFormatted("GT5U.waila.hatch.outputme."
                        + prefix + "_cache_detail", stackCount, stackCount > 1 ? "s" : ""));
            }
            for (int i = 0; i < stacks.tagCount(); i++) {
                NBTTagCompound stackTag = stacks.getCompoundTagAt(i);
                String name = getLocalizedName(prefix, stackTag);
                if (name != null) {
                    tooltip.add(GTWaila.getStackListLine(name, stackTag.getLong("Amount")));
                }
            }
            if (stackCount > stacks.tagCount()) {
                tooltip.add(StatCollector.translateToLocalFormatted("GT5U.waila.hatch.outputme."
                        + prefix + "_cache_detail.more", stackCount - stacks.tagCount()));
            }
            return stackCount;
        }

        public static boolean hasWailaAdvancedBody(IWailaDataAccessor accessor) {
            return accessor.getNBTData().hasKey("cacheCount");
        }

        public static void getWailaAdvancedBody(String prefix, List<String> tooltip, IWailaDataAccessor accessor) {
            NBTTagCompound tag = accessor.getNBTData();
            if (tag.hasKey("cacheCount")) {
                tooltip.add(StatCollector.translateToLocal("GT5U.waila.hatch.outputme.storage_cache"));
                processWailaCache(prefix, tooltip, "cacheStacks", "cacheCount", tag, true);
            }
        }

        public static void getWailaCacheBody(String prefix, List<String> tooltip, IWailaDataAccessor accessor) {
            NBTTagCompound tag = accessor.getNBTData();
            int stackCount = processWailaCache(prefix, tooltip, "stacks", "stackCount", tag, false);
            int intervalSeconds = tag.getInteger("refreshTime") / 20;
            if (stackCount == 0) {
                tooltip.add(StatCollector.translateToLocalFormatted(
                        "GT5U.waila.hatch.outputme.flush_interval.idle", intervalSeconds));
            } else {
                tooltip.add(StatCollector.translateToLocalFormatted(
                        "GT5U.waila.hatch.outputme.flush_interval",
                        intervalSeconds,
                        tag.getInteger("ticksToFlush") / 20));
            }
        }
    }

    public boolean isVoidCell() {
        return voidCell;
    }

    public boolean canAccept(long buffered) {
        return buffered < getCapacity() || lastInputTick == currentTick;
    }

    public boolean canAccept(long buffered, long amount) {
        // GTNH #7570: Check Mode also enforces the physical buffer limit without a cell.
        return checkMode ? amount <= getCapacity() - buffered : canAccept(buffered);
    }

    public void accepted() {
        lastInputTick = currentTick;
    }

    public void tick(long tick) {
        currentTick = tick;
        if (tick % 20 == 0) {
            AENetworkProxy grid = proxy.get();
            boolean active = grid != null && grid.isActive();
            if (active != wasActive) {
                wasActive = active;
                updateNetwork(null, null);
            }
        }
    }

    public boolean shouldFlush(boolean empty) {
        if (empty) {
            lastOutputTick = currentTick;
        }
        return !empty && currentTick - lastOutputTick >= refreshTicks;
    }

    public void flushed() {
        lastOutputTick = currentTick;
    }

    public int getRefreshSeconds() {
        return refreshTicks / 20;
    }

    public void setRefreshSeconds(int seconds) {
        refreshTicks = (int) Math.max(MIN_REFRESH_TICKS, Math.min(Integer.MAX_VALUE / 20, (long) seconds) * 20);
        dirty.run();
    }

    public boolean isCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(boolean enabled) {
        if (cacheMode == enabled) {
            return;
        }
        if (!enabled && cell != null) {
            IItemList<T> contents = cell.getAvailableItems(
                    (IItemList<T>) cell.getStackType().createPrimitiveList(), IterationCounter.fetchNewId());
            for (T stack : contents) {
                T extracted = cell.extractItems(stack, Actionable.MODULATE, source.get());
                if (extracted != null) {
                    returnToCache.accept(extracted);
                    postCellChange(extracted, -extracted.getStackSize());
                }
            }
        }
        cacheMode = enabled;
        updateNetwork(enabled ? null : cellStack.get(), enabled ? cellStack.get() : null);
        dirty.run();
    }

    public boolean getCheckMode() {
        return checkMode;
    }

    public void setCheckMode(boolean enabled) {
        if (checkMode != enabled) {
            checkMode = enabled;
            dirty.run();
        }
    }

    public boolean shouldCheck() {
        return checkMode && cacheMode && cell != null;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int value) {
        if (priority != value) {
            priority = value;
            updateReadableCell();
            updateNetwork(null, null);
            dirty.run();
        }
    }

    public boolean canStore(T stack) {
        return canStore(stack, 0);
    }

    public boolean canStore(T stack, long reservedSameType) {
        if (shouldCheck()) {
            long buffered = bufferedAmount.applyAsLong(stack);
            long pending = Math.max(0, reservedSameType);
            long amount = stack.getStackSize();
            if (buffered > Long.MAX_VALUE - amount || pending > Long.MAX_VALUE - amount - buffered) {
                return false;
            }
            T input = stack.copy().setStackSize(amount + buffered + pending);
            T remaining = cell.injectItems(input, Actionable.SIMULATE, source.get());
            return remaining == null || remaining.getStackSize() == 0;
        }
        return isAllowed(stack);
    }

    public boolean isFiltered() {
        return !partition.isEmpty();
    }

    public boolean isAllowed(T stack) {
        if (partition.isEmpty()) {
            return true;
        }
        boolean contains = false;
        for (T filter : partition) {
            if (sameType(filter, stack)) {
                contains = true;
                break;
            }
        }
        return blacklist != contains;
    }

    private boolean sameType(T left, T right) {
        if (left instanceof IAEItemStack && right instanceof IAEItemStack) {
            ItemStack leftItem = ((IAEItemStack) left).getItemStack();
            ItemStack rightItem = ((IAEItemStack) right).getItemStack();
            return leftItem.isItemEqual(rightItem) && ItemStack.areItemStackTagsEqual(leftItem, rightItem);
        }
        if (left instanceof IAEFluidStack && right instanceof IAEFluidStack) {
            FluidStack leftFluid = ((IAEFluidStack) left).getFluidStack();
            return leftFluid.isFluidEqual(((IAEFluidStack) right).getFluidStack());
        }
        return false;
    }

    public T insert(T stack, IMEInventory<T> network) throws GridAccessException {
        AENetworkProxy grid = proxy.get();
        long before = stack.getStackSize();
        T remaining = Platform.poweredInsert(grid.getEnergy(), cacheMode && cell != null ? cell : network,
                stack, source.get());
        if (cacheMode && cell != null) {
            long inserted = before - (remaining == null ? 0 : remaining.getStackSize());
            if (inserted > 0) {
                postCellChange(stack, inserted);
            }
        }
        return remaining;
    }

    private void postCellChange(T stack, long amount) {
        AENetworkProxy grid = proxy.get();
        if (grid == null || !grid.isActive()) {
            return;
        }
        try {
            grid.getStorage().postAlterationOfStoredItems(channel,
                    Collections.singletonList(stack.copy().setStackSize(amount)), source.get());
        } catch (GridAccessException ignored) {
            // Storage has already changed; never report an insertion as rejected because notification failed.
        }
    }

    public List<IMEInventoryHandler> getCellArray(StorageChannel requested) {
        AENetworkProxy grid = proxy.get();
        return cacheMode && requested == channel && readableCell != null && grid != null && grid.isActive()
                ? Collections.<IMEInventoryHandler>singletonList(readableCell)
                : Collections.<IMEInventoryHandler>emptyList();
    }

    private void updateNetwork(ItemStack oldCell, ItemStack newCell) {
        AENetworkProxy grid = proxy.get();
        if (grid == null) {
            return;
        }
        try {
            grid.getGrid().postEvent(new MENetworkCellArrayUpdate());
            if (cacheMode && grid.isActive() && (oldCell != null || newCell != null)) {
                Platform.postChanges(grid.getStorage(), oldCell, newCell, source.get());
            }
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public void saveChanges(IMEInventory inventory) {
        dirty.run();
    }

    public void save(NBTTagCompound nbt) {
        nbt.setBoolean("meCacheMode", cacheMode);
        nbt.setBoolean("meCheckMode", checkMode);
        nbt.setInteger("mePriority", priority);
        nbt.setInteger("meRefreshTicks", refreshTicks);
        ItemStack stack = cellStack.get();
        if (stack != null) {
            nbt.setTag("meStorageCell", stack.writeToNBT(new NBTTagCompound()));
        } else {
            nbt.removeTag("meStorageCell");
        }
    }

    public void load(NBTTagCompound nbt) {
        cacheMode = nbt.getBoolean("meCacheMode");
        checkMode = nbt.getBoolean("meCheckMode");
        priority = nbt.getInteger("mePriority");
        refreshTicks = Math.max(MIN_REFRESH_TICKS, nbt.getInteger("meRefreshTicks"));
        previousCell = null;
        cell = null;
        readableCell = null;
        partition.clear();
        blacklist = false;
        voidCell = false;
        lastOutputTick = 0;
        lastInputTick = Long.MIN_VALUE;
        currentTick = 0;
        wasActive = false;
    }
}
