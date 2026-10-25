package gregtech.api.metatileentity.implementations;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.IterationCounter;
import appeng.util.Platform;

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
    private ItemStack previousCell;
    private IMEInventoryHandler<T> cell;
    private IMEInventoryHandler<T> readableCell;
    private boolean cacheMode;
    private boolean wasActive;
    private int refreshTicks = MIN_REFRESH_TICKS;
    private long lastOutputTick;
    private long lastInputTick = Long.MIN_VALUE;
    private long currentTick;

    public GT_MEOutputStorage(StorageChannel channel, long defaultCapacity, Supplier<ItemStack> cellStack,
            Supplier<AENetworkProxy> proxy, Supplier<BaseActionSource> source, Runnable dirty,
            Consumer<T> returnToCache) {
        this.channel = channel;
        this.defaultCapacity = defaultCapacity;
        this.cellStack = cellStack;
        this.proxy = proxy;
        this.source = source;
        this.dirty = dirty;
        this.returnToCache = returnToCache;
    }

    protected IMEInventoryHandler<T> loadCell(ItemStack stack) {
        return AEApi.instance().registries().cell().getCellInventory(stack, this, channel);
    }

    public boolean isValidCell(ItemStack stack) {
        return stack != null && stack.stackSize == 1 && loadCell(stack) instanceof ICellInventoryHandler;
    }

    public void updateCell() {
        ItemStack stack = cellStack.get();
        if (stack == previousCell) return;
        ItemStack oldCell = previousCell;
        previousCell = stack;
        cell = stack == null ? null : loadCell(stack);
        readableCell = null;
        if (cell != null) {
            MEInventoryHandler<T> readOnly = new MEInventoryHandler<T>(cell, (IAEStackType<T>) cell.getStackType());
            readOnly.setBaseAccess(AccessRestriction.READ);
            readableCell = readOnly;
        }
        updateNetwork(oldCell, stack);
        dirty.run();
    }

    public long getCapacity() {
        updateCell();
        if (cell instanceof ICellInventoryHandler) {
            ICellInventoryHandler<T> handler = (ICellInventoryHandler<T>) cell;
            if (handler.getCellInv() != null) {
                return handler.getCellInv().getStoredItemCount() + handler.getCellInv().getRemainingItemCount();
            }
        }
        return defaultCapacity;
    }

    public boolean canAccept(long buffered) {
        return buffered < getCapacity() || lastInputTick == currentTick;
    }

    public void accepted() {
        lastInputTick = currentTick;
    }

    public void tick(long tick) {
        currentTick = tick;
        updateCell();
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
        if (empty) lastOutputTick = currentTick;
        return !empty && currentTick - lastOutputTick >= refreshTicks;
    }

    public void flushed() {
        lastOutputTick = currentTick;
    }

    public int getRefreshSeconds() { return refreshTicks / 20; }

    public void setRefreshSeconds(int seconds) {
        refreshTicks = (int) Math.max(MIN_REFRESH_TICKS, Math.min(Integer.MAX_VALUE / 20, (long) seconds) * 20);
        dirty.run();
    }

    public boolean isCacheMode() { return cacheMode; }

    public void setCacheMode(boolean enabled) {
        if (cacheMode == enabled) return;
        updateCell();
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

    public boolean canStore(T stack) {
        updateCell();
        return !(cell instanceof ICellInventoryHandler)
                || !((ICellInventoryHandler<T>) cell).isPreformatted() || cell.canAccept(stack);
    }

    public T insert(T stack, IMEInventory<T> network) throws GridAccessException {
        updateCell();
        AENetworkProxy grid = proxy.get();
        long before = stack.getStackSize();
        T remaining = Platform.poweredInsert(grid.getEnergy(), cacheMode && cell != null ? cell : network,
                stack, source.get());
        if (cacheMode && cell != null) {
            long inserted = before - (remaining == null ? 0 : remaining.getStackSize());
            if (inserted > 0) postCellChange(stack, inserted);
        }
        return remaining;
    }

    private void postCellChange(T stack, long amount) {
        AENetworkProxy grid = proxy.get();
        if (grid == null || !grid.isActive()) return;
        try {
            grid.getStorage().postAlterationOfStoredItems(channel,
                    Collections.singletonList(stack.copy().setStackSize(amount)), source.get());
        } catch (GridAccessException ignored) {
            // Storage has already changed; never report an insertion as rejected because notification failed.
        }
    }

    public List<IMEInventoryHandler> getCellArray(StorageChannel requested) {
        updateCell();
        AENetworkProxy grid = proxy.get();
        return cacheMode && requested == channel && readableCell != null && grid != null && grid.isActive()
                ? Collections.<IMEInventoryHandler>singletonList(readableCell) : Collections.<IMEInventoryHandler>emptyList();
    }

    private void updateNetwork(ItemStack oldCell, ItemStack newCell) {
        AENetworkProxy grid = proxy.get();
        if (grid == null) return;
        try {
            grid.getGrid().postEvent(new MENetworkCellArrayUpdate());
            if (cacheMode && grid.isActive() && (oldCell != null || newCell != null)) {
                Platform.postChanges(grid.getStorage(), oldCell, newCell, source.get());
            }
        } catch (GridAccessException ignored) {}
    }

    @Override
    public void saveChanges(IMEInventory inventory) { dirty.run(); }

    public void save(NBTTagCompound nbt) {
        nbt.setBoolean("meCacheMode", cacheMode);
        nbt.setInteger("meRefreshTicks", refreshTicks);
        ItemStack stack = cellStack.get();
        if (stack != null) nbt.setTag("meStorageCell", stack.writeToNBT(new NBTTagCompound()));
        else nbt.removeTag("meStorageCell");
    }

    public void load(NBTTagCompound nbt) {
        cacheMode = nbt.getBoolean("meCacheMode");
        refreshTicks = Math.max(MIN_REFRESH_TICKS, nbt.getInteger("meRefreshTicks"));
        previousCell = null;
        cell = null;
        readableCell = null;
        lastOutputTick = 0;
        lastInputTick = Long.MIN_VALUE;
        currentTick = 0;
        wasActive = false;
    }
}
