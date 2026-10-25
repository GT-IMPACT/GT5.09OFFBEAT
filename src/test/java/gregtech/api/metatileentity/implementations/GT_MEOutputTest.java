package gregtech.api.metatileentity.implementations;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collections;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.IGridNode;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.me.helpers.AENetworkProxy;
import appeng.core.localization.WailaText;
import appeng.me.helpers.IGridProxyable;
import appeng.me.cache.SecurityCache;
import appeng.me.storage.NetworkInventoryHandler;
import appeng.me.storage.MEPassThrough;
import appeng.me.storage.StorageBusInventoryHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import gregtech.api.gui.GT_Container_MEOutput;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import org.junit.Test;

import static org.junit.Assert.*;

public class GT_MEOutputTest {
    @Test
    public void outputSurvivesNetworkFailuresAndReloads() throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LaunchClassLoader previousLaunch = Launch.classLoader;
        Map<String, Object> previousBlackboard = Launch.blackboard;
        try (LaunchClassLoader loader = new LaunchClassLoader(((URLClassLoader) getClass().getClassLoader()).getURLs())) {
            Launch.classLoader = loader;
            Launch.blackboard = new HashMap<>();
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class)
                    .invoke(null, (Object) new Object[]{"7", "99", "40", "1614", "1.7.10", "9.05",
                            new File("."), new ArrayList<String>()});
            loader.loadClass(getClass().getName()).getMethod("runChecks").invoke(null);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            Launch.classLoader = previousLaunch;
            Launch.blackboard = previousBlackboard;
        }
    }

    public static void runChecks() throws Exception {
        Field side = FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, Side.SERVER);
        Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, new File("."));
        Bootstrap.func_151354_b();
        Field mods = Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true);
        mods.set(Loader.instance(), new HashMap<String, ModContainer>());
        GregTech_API.mAE2 = true;
        checkGuiSync(new Bus());
        checkGuiSync(new Hatch());
        Bus wailaBus = new Bus();
        checkWailaConnection(wailaBus, wailaBus.network);
        Hatch wailaHatch = new Hatch();
        checkWailaConnection(wailaHatch, wailaHatch.network);

        Hatch hatch = new Hatch();
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000000);
        assertEquals(1000000, hatch.fill(water, false));
        assertEquals(0, hatch.getFluidAmount());
        assertEquals(1000000, hatch.fill(water, true));
        water.amount = 1;
        assertEquals(1000000, hatch.getFluidAmount());
        assertEquals(Integer.MAX_VALUE, hatch.fill(new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE), true));
        assertEquals(Integer.MAX_VALUE, hatch.getFluidAmount());
        assertEquals(1000000L + Integer.MAX_VALUE, fluidCount(hatch));
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 1);
        assertEquals("Full offline hatch rejects the next tick's output", 0, hatch.fill(water, true));
        assertEquals(128000, hatch.getCapacity());
        NBTTagCompound nbt = new NBTTagCompound();
        hatch.setItemNBT(nbt);
        Hatch restoredHatch = new Hatch();
        restoredHatch.loadNBTData(nbt);
        restoredHatch.loadNBTData(nbt);
        assertEquals(fluidCount(hatch), fluidCount(restoredHatch));

        Bus bus = new Bus();
        assertFalse(bus.storeAll(null));
        ItemStack items = new ItemStack(Items.iron_ingot, 1000);
        assertTrue(bus.storeAll(items));
        assertEquals(0, items.stackSize);
        bus.onPostTick(bus.getBaseMetaTileEntity(), 20);
        assertEquals(1000, itemCount(bus));
        nbt = new NBTTagCompound();
        bus.setItemNBT(nbt);
        Bus restoredBus = new Bus();
        restoredBus.loadNBTData(nbt);
        restoredBus.loadNBTData(nbt);
        assertEquals(1000, itemCount(restoredBus));
        checkCell(StorageChannel.ITEMS, IAEItemStack.class, 1600);
        checkCell(StorageChannel.FLUIDS, IAEFluidStack.class, 128000);
        checkStorageBus(StorageChannel.ITEMS, IAEItemStack.class);
        checkStorageBus(StorageChannel.FLUIDS, IAEFluidStack.class);
        checkSecuredStorageBus(StorageChannel.ITEMS, IAEItemStack.class);
        checkSecuredStorageBus(StorageChannel.FLUIDS, IAEFluidStack.class);

        // Model partial acceptance and network failures at the insertion boundary.
        checkItemTransfer(bus);
        Hatch transferHatch = new Hatch();
        transferHatch.fill(new FluidStack(FluidRegistry.WATER, 1000000), true);
        checkFluidTransfer(transferHatch);

        Bus large = new Bus();
        large.storeAll(new ItemStack(Items.iron_ingot, Integer.MAX_VALUE));
        large.storeAll(new ItemStack(Items.iron_ingot, 17));
        nbt = new NBTTagCompound();
        large.saveNBTData(nbt);
        restoredBus.loadNBTData(nbt);
        assertEquals((long) Integer.MAX_VALUE + 17, itemCount(restoredBus));
        ItemStack tagged = new ItemStack(Items.iron_ingot, 5);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound().setString("variant", "a");
        large.storeAll(tagged);
        tagged.getTagCompound().setString("variant", "b");
        assertEquals(3, save(large).getTagList("cachedItems", 10).tagCount());
        assertEquals("a", save(large).getTagList("cachedItems", 10).getCompoundTagAt(2)
                .getCompoundTag("tag").getString("variant"));
        restoredBus.loadNBTData(new NBTTagCompound());
        assertEquals(0, itemCount(restoredBus));
        GregTech_API.mAE2 = false;
        assertFalse(bus.storeAll(new ItemStack(Items.iron_ingot)));
        assertEquals(0, hatch.fill(water, true));
    }

    private static void checkGuiSync(MetaTileEntity output) {
        ItemStack cell = new ItemStack(Items.diamond);
        IGregTechTileEntity clientTile = (IGregTechTileEntity) Proxy.newProxyInstance(
                GT_MEOutputTest.class.getClassLoader(), new Class[]{IGregTechTileEntity.class}, (p, m, a) -> {
                    switch (m.getName()) {
                        case "getMetaTileEntity": return output;
                        case "isClientSide": return true;
                        case "getSizeInventory": return output.mInventory.length;
                        case "getStackInSlot": return output.getStackInSlot((Integer) a[0]);
                        case "setInventorySlotContents":
                            output.setInventorySlotContents((Integer) a[0], (ItemStack) a[1]);
                            return null;
                        default: return defaultValue(m.getReturnType());
                    }
                });
        for (int opening = 0; opening < 2; opening++) {
            GT_Container_MEOutput container = new GT_Container_MEOutput(new InventoryPlayer(null), clientTile);
            ItemStack[] windowItems = new ItemStack[container.inventorySlots.size()];
            windowItems[0] = cell.copy();
            container.putStacksInSlots(windowItems);
            assertTrue("Opening the GUI keeps the cell visible", ItemStack.areItemStacksEqual(cell, container.getSlot(0).getStack()));
            for (int control = 1; control <= 3; control++) {
                container.putStackInSlot(control, null);
                assertNull(container.getSlot(control).getStack());
                assertTrue("Control sync must not clear the cell", ItemStack.areItemStacksEqual(cell, container.getSlot(0).getStack()));
                assertNotSame(container.getSlot(0).inventory, container.getSlot(control).inventory);
            }
        }
    }

    private static void checkItemTransfer(Bus bus) {
        bus.network.active = true;
        bus.network.space = 400;
        bus.onPostTick(bus.getBaseMetaTileEntity(), 40);
        assertEquals(600, itemCount(bus));
        assertEquals(400, bus.network.inserted);
        bus.network.space = 1000;
        bus.network.power = false;
        bus.onPostTick(bus.getBaseMetaTileEntity(), 80);
        assertEquals(600, itemCount(bus));
        bus.network.power = true;
        bus.network.unavailable = true;
        bus.onPostTick(bus.getBaseMetaTileEntity(), 120);
        assertEquals(600, itemCount(bus));
        bus.network.unavailable = false;
        bus.network.working = false;
        bus.onPostTick(bus.getBaseMetaTileEntity(), 160);
        assertEquals(600, itemCount(bus));
        bus.network.working = true;
        bus.onPostTick(bus.getBaseMetaTileEntity(), 200);
        bus.onPostTick(bus.getBaseMetaTileEntity(), 240);
        assertEquals(0, itemCount(bus));
        assertEquals(1000, bus.network.inserted);
    }

    private static void checkFluidTransfer(Hatch hatch) {
        hatch.network.active = true;
        hatch.network.space = 200000;
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 40);
        assertEquals(800000, fluidCount(hatch));
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 80);
        assertEquals(800000, fluidCount(hatch));
        hatch.network.space = 1000000;
        hatch.network.power = false;
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 120);
        assertEquals(800000, fluidCount(hatch));
        hatch.network.power = true;
        hatch.network.unavailable = true;
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 160);
        assertEquals(800000, fluidCount(hatch));
        hatch.network.unavailable = false;
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 200);
        hatch.onPostTick(hatch.getBaseMetaTileEntity(), 240);
        assertEquals(0, fluidCount(hatch));
        assertEquals(1000000, hatch.network.inserted);
    }

    private static NBTTagCompound save(GT_MetaTileEntity_Hatch_OutputBus_ME bus) {
        NBTTagCompound nbt = new NBTTagCompound();
        bus.saveNBTData(nbt);
        return nbt;
    }

    private static long itemCount(Bus bus) {
        NBTTagList list = save(bus).getTagList("cachedItems", 10);
        long count = 0;
        for (int i = 0; i < list.tagCount(); i++) count += list.getCompoundTagAt(i).getInteger("cachedCount");
        return count;
    }

    private static long fluidCount(Hatch hatch) {
        NBTTagCompound nbt = new NBTTagCompound();
        hatch.saveNBTData(nbt);
        NBTTagList list = nbt.getTagList("cachedFluids", 10);
        long count = 0;
        for (int i = 0; i < list.tagCount(); i++) count += list.getCompoundTagAt(i).getInteger("Amount");
        return count;
    }

    private static void checkWailaConnection(GT_MetaTileEntity_Hatch hatch, Network network) {
        hatch.idHatch = 7;
        boolean[][] states = {{true, true}, {false, true}, {false, false}};
        String[] expected = {
                WailaText.DeviceOnline.getLocal(EnumChatFormatting.GREEN),
                WailaText.DeviceMissingChannel.getLocal(EnumChatFormatting.RED),
                WailaText.DeviceOffline.getLocal(EnumChatFormatting.RED)
        };
        for (int i = 0; i < states.length; i++) {
            network.active = states[i][0];
            network.power = states[i][1];
            NBTTagCompound tag = new NBTTagCompound();
            hatch.getWailaNBTData(null, null, tag, null, 0, 0, 0);
            assertEquals(network.active, tag.getBoolean("isActive"));
            assertEquals(network.power, tag.getBoolean("isPowered"));
            assertFalse(tag.getBoolean("isBooting"));
            // The client must render the snapshot rather than re-read the local AE proxy.
            network.active = !network.active;
            network.power = !network.power;
            IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
                    GT_MEOutputTest.class.getClassLoader(), new Class[]{IWailaDataAccessor.class},
                    (p, m, a) -> m.getName().equals("getNBTData") ? tag : defaultValue(m.getReturnType()));
            IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
                    GT_MEOutputTest.class.getClassLoader(), new Class[]{IWailaConfigHandler.class},
                    (p, m, a) -> true);
            List<String> tooltip = new ArrayList<>();
            hatch.getWailaBody(null, tooltip, accessor, config);
            assertEquals(3, tooltip.size());
            assertEquals(expected[i], tooltip.get(1));
            assertTrue(tooltip.get(2).contains("ID: 7"));
            config = (IWailaConfigHandler) Proxy.newProxyInstance(
                    GT_MEOutputTest.class.getClassLoader(), new Class[]{IWailaConfigHandler.class},
                    (p, m, a) -> false);
            tooltip.clear();
            hatch.getWailaBody(null, tooltip, accessor, config);
            assertEquals(expected[i], tooltip.get(0));
            assertTrue(tooltip.get(1).contains("ID: 7"));
        }
    }

    private static class Bus extends GT_MetaTileEntity_Hatch_OutputBus_ME {
        final Network network = new Network();
        Bus() {
            super("test", 1, new String[0], new ITexture[0][][]);
            setBaseMetaTileEntity(network.host());
        }
        @Override public AENetworkProxy getProxy() { return network; }
        @Override protected void markStorageDirty() {}
        @Override public int store(ItemStack stack) { return network.insert(stack.stackSize); }
    }

    private static class Hatch extends GT_MetaTileEntity_Hatch_Output_ME {
        final Network network = new Network();
        Hatch() {
            super("test", 4, new String[0], new ITexture[0][][]);
            setBaseMetaTileEntity(network.host());
        }
        @Override public AENetworkProxy getProxy() { return network; }
        @Override protected void markStorageDirty() {}
        @Override protected int store(FluidStack stack) { return network.insert(stack.amount); }
    }

    private static class Network extends AENetworkProxy {
        IGridNode node;
        boolean active, unavailable;
        boolean power = true, working = true;
        long space, inserted;
        Network() { super(null, "proxy", new ItemStack(Items.iron_ingot), true); }
        @Override public boolean isActive() { return active; }
        @Override public boolean isPowered() { return power; }
        @Override public IGridNode getNode() { return node; }
        @Override public void writeToNBT(NBTTagCompound nbt) {}
        @Override public void readFromNBT(NBTTagCompound nbt) {}
        @Override public IGrid getGrid() {
            return (IGrid) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IGrid.class},
                    (p, m, a) -> defaultValue(m.getReturnType()));
        }
        @Override public IEnergyGrid getEnergy() {
            return (IEnergyGrid) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IEnergyGrid.class},
                    (p, m, a) -> m.getName().equals("extractAEPower") ? (power ? (Double) a[0] : 0D)
                            : defaultValue(m.getReturnType()));
        }
        @Override public IStorageGrid getStorage() {
            return (IStorageGrid) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IStorageGrid.class},
                    (p, m, a) -> defaultValue(m.getReturnType()));
        }
        IGregTechTileEntity host() {
            return (IGregTechTileEntity) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{IGregTechTileEntity.class, IGridProxyable.class, IActionHost.class}, (p, m, a) -> {
                        if (m.getName().equals("isServerSide")) return true;
                        if (m.getName().equals("isAllowedToWork")) return working;
                        if (m.getName().equals("getOwnerName")) return "test-owner";
                        if (m.getName().equals("getOwnerUuid")) return new java.util.UUID(0, 42);
                        return defaultValue(m.getReturnType());
                    });
        }
        int insert(int amount) {
            if (!active || !power || unavailable || !working) return amount;
            int accepted = (int) Math.min(amount, space);
            space -= accepted;
            inserted += accepted;
            return amount - accepted;
        }
    }

    private static <T extends IAEStack<T>> void checkCell(StorageChannel channel, Class<T> stackType,
            long defaultCapacity) throws Exception {
        CellFixture<T> fixture = new CellFixture<T>(channel, stackType, defaultCapacity);
        GT_MEOutputStorage<T> storage = fixture.storage;
        assertEquals(defaultCapacity, storage.getCapacity());
        storage.tick(1);
        assertFalse(storage.canAccept(defaultCapacity));
        assertTrue(storage.canAccept(defaultCapacity - 1));
        storage.accepted();
        assertTrue("One complete recipe batch may exceed the limit", storage.canAccept(defaultCapacity + 1));
        storage.tick(2);
        assertFalse(storage.canAccept(defaultCapacity + 1));

        fixture.stack = new ItemStack(Items.diamond);
        fixture.stack.setTagCompound(new NBTTagCompound());
        fixture.stack.getTagCompound().setString("cellContents", "preserved");
        assertTrue(storage.isValidCell(fixture.stack));
        assertFalse(storage.isValidCell(new ItemStack(Items.iron_ingot)));
        assertEquals(1000000, storage.getCapacity());
        fixture.filtered = true;
        assertFalse(storage.canStore(fixture.newStack(1)));
        fixture.filtered = false;
        assertTrue(storage.canStore(fixture.newStack(1)));
        storage.setCacheMode(true);
        fixture.network.active = true;
        assertEquals(1, storage.getCellArray(channel).size());
        IMEInventoryHandler<T> readable = storage.getCellArray(channel).get(0);
        assertEquals(AccessRestriction.READ, readable.getAccess());
        T input = fixture.newStack(100);
        assertSame("Network cannot inject unrelated items into the output cell", input,
                readable.injectItems(input, Actionable.MODULATE, new BaseActionSource()));
        assertNull(storage.insert(input, null));
        assertEquals(100, fixture.stored);
        fixture.network.power = false;
        assertEquals(50, storage.insert(fixture.newStack(50), null).getStackSize());
        assertEquals(100, fixture.stored);
        fixture.network.power = true;
        assertEquals(30, readable.extractItems(fixture.newStack(30), Actionable.MODULATE,
                new BaseActionSource()).getStackSize());
        assertEquals(70, fixture.stored);
        fixture.network.active = false;
        assertTrue(storage.getCellArray(channel).isEmpty());

        storage.setRefreshSeconds(5);
        storage.tick(100);
        assertFalse(storage.shouldFlush(true));
        storage.tick(199);
        assertFalse(storage.shouldFlush(false));
        storage.tick(200);
        assertTrue(storage.shouldFlush(false));
        storage.flushed();
        assertFalse(storage.shouldFlush(false));

        NBTTagCompound nbt = new NBTTagCompound();
        storage.save(nbt);
        storage.setCacheMode(false);
        assertEquals("Disabling cache mode returns the cell's contents to the output buffer", 70, fixture.returned);
        assertEquals(0, fixture.stored);
        storage.load(nbt);
        assertTrue(storage.isCacheMode());
        assertEquals(5, storage.getRefreshSeconds());
        assertEquals("preserved", nbt.getCompoundTag("meStorageCell").getCompoundTag("tag").getString("cellContents"));
        storage.setRefreshSeconds(Integer.MAX_VALUE);
        assertTrue(storage.getRefreshSeconds() > 2);
        storage.setRefreshSeconds(-1);
        assertEquals(2, storage.getRefreshSeconds());
        fixture.stack = null;
        assertEquals(defaultCapacity, storage.getCapacity());
        storage.save(nbt);
        assertFalse(nbt.hasKey("meStorageCell"));

        // The cell drops as a separate inventory item; do not also embed it in the hatch drop.
        Bus bus = new Bus();
        bus.mInventory[0] = new ItemStack(Items.diamond);
        nbt = new NBTTagCompound();
        bus.setItemNBT(nbt);
        assertFalse(nbt.hasKey("meStorageCell"));
        bus.saveNBTData(nbt);
        assertTrue(nbt.hasKey("meStorageCell"));
    }

    private static class CellFixture<T extends IAEStack<T>> {
        final Network network = new Network();
        final Class<T> stackType;
        final GT_MEOutputStorage<T> storage;
        final ICellInventoryHandler<T> handler;
        ItemStack stack;
        long stored, returned;
        boolean filtered;
        BaseActionSource actionSource = new BaseActionSource();

        CellFixture(StorageChannel channel, Class<T> stackType, long defaultCapacity) {
            this.stackType = stackType;
            ICellInventory<T> inventory = (ICellInventory<T>) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{ICellInventory.class}, (p, m, a) -> {
                        if (m.getName().equals("getStoredItemCount")) return stored;
                        if (m.getName().equals("getRemainingItemCount")) return 1000000 - stored;
                        return defaultValue(m.getReturnType());
                    });
            handler = (ICellInventoryHandler<T>) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class[]{ICellInventoryHandler.class}, (p, m, a) -> {
                        switch (m.getName()) {
                            case "getStackType":
                                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IAEStackType.class},
                                        (q, method, args) -> method.getName().equals("createPrimitiveList")
                                                ? itemList() : defaultValue(method.getReturnType()));
                            case "getCellInv": return inventory;
                            case "getAccess": return AccessRestriction.READ_WRITE;
                            case "isPreformatted": return filtered;
                            case "canAccept": return !filtered;
                            case "validForPass": return true;
                            case "getAvailableItems":
                                return itemList();
                            case "injectItems": {
                                T input = (T) a[0];
                                long accepted = Math.min(input.getStackSize(), 1000000 - stored);
                                if (a[1] == Actionable.MODULATE) stored += accepted;
                                return accepted == input.getStackSize() ? null
                                        : input.copy().setStackSize(input.getStackSize() - accepted);
                            }
                            case "extractItems": {
                                long amount = Math.min(((T) a[0]).getStackSize(), stored);
                                if (a[1] == Actionable.MODULATE) stored -= amount;
                                return amount == 0 ? null : newStack(amount);
                            }
                            default: return defaultValue(m.getReturnType());
                        }
                    });
            storage = new GT_MEOutputStorage<T>(channel, defaultCapacity, () -> stack, () -> network,
                    () -> actionSource, () -> {}, input -> returned += input.getStackSize()) {
                @Override protected IMEInventoryHandler<T> loadCell(ItemStack item) {
                    return item.getItem() == Items.diamond ? handler : null;
                }
            };
        }

        private IItemList<T> itemList() {
            return (IItemList<T>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IItemList.class},
                    (q, method, args) -> method.getName().equals("iterator")
                            ? Collections.singletonList(newStack(stored)).iterator()
                            : defaultValue(method.getReturnType()));
        }

        T newStack(long count) {
            long[] amount = {count};
            return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{stackType}, (p, m, a) -> {
                switch (m.getName()) {
                    case "getStackSize": return amount[0];
                    case "setStackSize": amount[0] = (Long) a[0]; return p;
                    case "getAmountPerUnit": return stackType == IAEFluidStack.class ? 1000 : 1;
                    case "copy": return newStack(amount[0]);
                    default: return defaultValue(m.getReturnType());
                }
            });
        }
    }

    private static <T extends IAEStack<T>> void checkStorageBus(StorageChannel channel, Class<T> stackType)
            throws Exception {
        CellFixture<T> target = new CellFixture<T>(channel, stackType, 128000);
        IAEStackType<T> type = (IAEStackType<T>) target.handler.getStackType();
        NetworkInventoryHandler<T> mainNetwork = new NetworkInventoryHandler<T>(type,
                new SecurityCache(target.network.getGrid()));
        mainNetwork.addNewStorage(target.handler);
        MEPassThrough<T> meInterface = new MEPassThrough<T>(mainNetwork, type);
        StorageBusInventoryHandler<T> storageBus = new StorageBusInventoryHandler<T>(meInterface, type,
                () -> AccessRestriction.READ_WRITE);
        NetworkInventoryHandler<T> subnet = new NetworkInventoryHandler<T>(type,
                new SecurityCache(target.network.getGrid()));
        subnet.addNewStorage(storageBus);
        assertNull(target.storage.insert(target.newStack(1000), subnet));
        assertEquals(1000, target.stored);
        target.stored = 999900;
        T remaining = target.storage.insert(target.newStack(1000), subnet);
        assertNotNull(remaining);
        assertEquals(900, remaining.getStackSize());
        assertEquals(1000000, target.stored);
    }

    private static <T extends IAEStack<T>> void checkSecuredStorageBus(StorageChannel channel, Class<T> stackType)
            throws Exception {
        CellFixture<T> target = new CellFixture<T>(channel, stackType, 128000);
        int[] ownerId = {-1};
        SecurityCache[] sourceSecurity = new SecurityCache[1];
        IGrid sourceGrid = (IGrid) Proxy.newProxyInstance(GT_MEOutputTest.class.getClassLoader(),
                new Class[]{IGrid.class}, (p, m, a) -> m.getName().equals("getCache")
                        ? sourceSecurity[0] : defaultValue(m.getReturnType()));
        sourceSecurity[0] = new SecurityCache(sourceGrid);
        IGridNode node = (IGridNode) Proxy.newProxyInstance(GT_MEOutputTest.class.getClassLoader(),
                new Class[]{IGridNode.class}, (p, m, a) -> {
                    if (m.getName().equals("getPlayerID")) return ownerId[0];
                    if (m.getName().equals("setPlayerID")) { ownerId[0] = (Integer) a[0]; return null; }
                    if (m.getName().equals("getGrid")) return sourceGrid;
                    return defaultValue(m.getReturnType());
                });
        IActionHost host = (IActionHost) Proxy.newProxyInstance(GT_MEOutputTest.class.getClassLoader(),
                new Class[]{IActionHost.class}, (p, m, a) -> m.getName().equals("getActionableNode")
                        ? node : defaultValue(m.getReturnType()));
        target.network.node = node;
        target.network.active = true;
        target.actionSource = new MachineSource(host);
        IAEStackType<T> type = (IAEStackType<T>) target.handler.getStackType();
        NetworkInventoryHandler<T> chestSubnet = new NetworkInventoryHandler<T>(type, sourceSecurity[0]);
        chestSubnet.addNewStorage(target.handler);
        assertNull("A chest in the output's own subnet accepts the same anonymous source",
                target.storage.insert(target.newStack(1000), chestSubnet));
        assertEquals(1000, target.stored);
        target.stored = 0;
        SecurityCache mainSecurity = new SecurityCache(target.network.getGrid()) {
            @Override public boolean isAvailable() { return true; }
            @Override public boolean hasPermission(int playerId, SecurityPermissions permission) {
                return playerId == 42;
            }
        };
        NetworkInventoryHandler<T> main = new NetworkInventoryHandler<T>(type, mainSecurity);
        main.addNewStorage(target.handler);
        StorageBusInventoryHandler<T> storageBus = new StorageBusInventoryHandler<T>(
                new MEPassThrough<T>(main, type), type, () -> AccessRestriction.READ_WRITE);
        NetworkInventoryHandler<T> subnet = new NetworkInventoryHandler<T>(type, sourceSecurity[0]);
        subnet.addNewStorage(storageBus);
        T rejected = target.storage.insert(target.newStack(1000), subnet);
        assertNotNull("Anonymous output is rejected by the destination network", rejected);
        assertEquals(1000, rejected.getStackSize());
        assertEquals(0, target.stored);

        // AE2 resolves the player supplied by setOwner during node initialization.
        node.setPlayerID(42);
        assertEquals(42, ownerId[0]);
        assertNull(target.storage.insert(rejected, subnet));
        assertEquals(1000, target.stored);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        return null;
    }
}
