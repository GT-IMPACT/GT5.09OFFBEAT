package gregtech.api.metatileentity.implementations;

import java.io.InputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringTranslate;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.Test;

import appeng.api.config.AccessRestriction;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.items.storage.ItemVoidStorageCell;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEItemStack;
import appeng.util.item.AEFluidStack;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OutputBusType;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.metatileentity.IMEOutput;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2MEOutputLogicTest {

    @Test
    public void checkModeSimulatesCellSpaceIncludingBufferedAndReservedAmounts() throws Throwable {
        run("check");
    }

    @Test
    public void priorityAndModesPersistAndExposeAReadOnlyCell() throws Throwable {
        run("priority");
    }

    @Test
    public void copiedSettingsAndReloadsPreserveItemAndFluidBuffers() throws Throwable {
        run("settings");
    }

    @Test
    public void dataStickClicksCopyAndApplyOutputSettings() throws Throwable {
        run("dataStickClicks");
    }

    @Test
    public void screwdriverAndWireCutterFollowTheProviderModesAndSides() throws Throwable {
        run("tools");
    }

    @Test
    public void processingReservesTheSameTypeWithoutChangingStrictTotalCapacity() throws Throwable {
        run("processing");
    }

    @Test
    public void exactPartitionsUseMetadataNbtAndInverterWithoutFuzzyMatching() throws Throwable {
        run("partitions");
    }

    @Test
    public void legacyAndTransactionalOutputUseTheReferenceBusOrder() throws Throwable {
        run("order");
    }

    @Test
    public void fluidOrderPreservesModesLocksPartialRemaindersAndOutputIndices() throws Throwable {
        run("fluidOrder");
    }

    @Test
    public void realProxiesReceiveTheHatchOwnerBeforeCreatingTheirNodes() throws Throwable {
        run("owner");
    }

    @Test
    public void reservationOverloadsPreserveExistingCapacityOverrides() throws Throwable {
        run("capacityOverrides");
    }

    @Test
    public void voidCellsDiscardNewOutputsAndPreserveAlreadyBufferedContents() throws Throwable {
        run("void");
    }

    @Test
    public void offlineBuffersAndCellsRemainVisibleInScannerAndWailaSnapshots() throws Throwable {
        run("display");
    }

    @Test
    public void offlineCheckModeWithoutACellEnforcesCapacityAndKeepsRejectedOutput() throws Throwable {
        run("offlineCapacity");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Field mods = Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true);
        mods.set(Loader.instance(), new HashMap<String, ModContainer>());
        GregTech_API.mAE2 = true;
        if ("check".equals(scenario)) {
            checkSimulation(StorageChannel.ITEMS, IAEItemStack.class);
            checkSimulation(StorageChannel.FLUIDS, IAEFluidStack.class);
        } else if ("priority".equals(scenario)) {
            checkPriority(StorageChannel.ITEMS, IAEItemStack.class);
            checkPriority(StorageChannel.FLUIDS, IAEFluidStack.class);
        } else if ("settings".equals(scenario)) {
            checkSettings();
        } else if ("dataStickClicks".equals(scenario)) {
            checkDataStickClicks();
        } else if ("tools".equals(scenario)) {
            checkTools();
        } else if ("processing".equals(scenario)) {
            checkProcessing();
        } else if ("partitions".equals(scenario)) {
            checkPartitions();
        } else if ("order".equals(scenario)) {
            checkOrder();
        } else if ("fluidOrder".equals(scenario)) {
            checkFluidOrder();
        } else if ("owner".equals(scenario)) {
            checkOwner();
        } else if ("capacityOverrides".equals(scenario)) {
            checkCapacityOverrides();
        } else if ("void".equals(scenario)) {
            checkVoidCells();
        } else if ("display".equals(scenario)) {
            checkDisplay();
        } else if ("offlineCapacity".equals(scenario)) {
            checkOfflineCapacity();
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static <T extends IAEStack<T>> void checkSimulation(StorageChannel channel, Class<T> stackType) {
        CellFixture<T> fixture = new CellFixture<>(channel, stackType);
        fixture.stored = 80;
        fixture.buffered = 12;
        fixture.storage.setCheckMode(true);
        assertFalse(fixture.storage.shouldCheck());
        assertTrue(fixture.storage.canStore(fixture.newStack(1000)));
        fixture.storage.setCacheMode(true);
        assertTrue(fixture.storage.shouldCheck());
        T request = fixture.newStack(8);
        assertTrue(fixture.storage.canStore(request));
        assertEquals(20, fixture.simulated);
        assertEquals(8, request.getStackSize());
        assertEquals("Simulation must never insert into the cell", 80, fixture.stored);
        assertFalse(fixture.storage.canStore(fixture.newStack(9)));
        assertFalse(fixture.storage.canStore(request, 1));
        assertEquals(21, fixture.simulated);
        assertTrue(fixture.storage.canStore(fixture.newStack(4), 4));
        fixture.buffered = Long.MAX_VALUE;
        assertFalse(fixture.storage.canStore(fixture.newStack(1)));
        fixture.buffered = 0;
        fixture.filtered = true;
        assertFalse(fixture.storage.canStore(fixture.newStack(1)));
        fixture.storage.setCheckMode(false);
        assertTrue("A non-workbench cell has no partition filter", fixture.storage.canStore(fixture.newStack(1)));
        fixture.filtered = false;
        assertTrue(fixture.storage.canStore(fixture.newStack(1000)));
        fixture.cellStack = null;
        fixture.storage.setCheckMode(true);
        assertFalse(fixture.storage.shouldCheck());
        assertEquals(1600, fixture.storage.getCapacity());
    }

    private static <T extends IAEStack<T>> void checkPriority(StorageChannel channel, Class<T> stackType) {
        CellFixture<T> fixture = new CellFixture<>(channel, stackType);
        fixture.storage.setCacheMode(true);
        fixture.storage.setCheckMode(true);
        fixture.storage.setPriority(-123);
        fixture.storage.setRefreshSeconds(5);
        fixture.network.active = true;
        IMEInventoryHandler<T> readable = fixture.storage.getCellArray(channel).get(0);
        assertEquals(-123, readable.getPriority());
        assertEquals(AccessRestriction.READ, readable.getAccess());
        T input = fixture.newStack(12);
        assertSame(input, readable.injectItems(input, Actionable.MODULATE, new BaseActionSource()));
        fixture.storage.setPriority(Integer.MAX_VALUE);
        IMEInventoryHandler<T> reprioritized = fixture.storage.getCellArray(channel).get(0);
        assertNotSame(readable, reprioritized);
        assertEquals(Integer.MAX_VALUE, reprioritized.getPriority());
        assertTrue("Priority updates notify the ME grid", fixture.network.events > 0);
        NBTTagCompound saved = new NBTTagCompound();
        fixture.storage.save(saved);
        fixture.network.active = false;
        fixture.storage.load(saved);
        fixture.storage.updateCell();
        fixture.network.active = true;
        assertTrue(fixture.storage.isCacheMode());
        assertTrue(fixture.storage.getCheckMode());
        assertEquals(Integer.MAX_VALUE, fixture.storage.getPriority());
        assertEquals(5, fixture.storage.getRefreshSeconds());
        assertEquals(Integer.MAX_VALUE, fixture.storage.getCellArray(channel).get(0).getPriority());
        fixture.storage.load(new NBTTagCompound());
        assertFalse(fixture.storage.isCacheMode());
        assertFalse(fixture.storage.getCheckMode());
        assertEquals(0, fixture.storage.getPriority());
        assertEquals(2, fixture.storage.getRefreshSeconds());
    }

    private static void checkSettings() throws Exception {
        Bus bus = new Bus();
        bus.storeAll(item("a", 7));
        bus.storeAll(item("b", 11));
        bus.storeAll(new ItemStack(Items.iron_ingot, 13));
        Method itemCount = GT_MetaTileEntity_Hatch_OutputBus_ME.class
                .getDeclaredMethod("getBufferedAmount", IAEItemStack.class);
        itemCount.setAccessible(true);
        assertEquals(7L, itemCount.invoke(bus, nativeItem(item("a", 1))));
        assertEquals(11L, itemCount.invoke(bus, nativeItem(item("b", 1))));
        assertEquals(0L, itemCount.invoke(bus, nativeItem(item("c", 1))));

        Hatch hatch = new Hatch();
        hatch.fill(fluid("a", 7), true);
        hatch.fill(fluid("b", 11), true);
        hatch.fill(new FluidStack(FluidRegistry.LAVA, 13), true);
        Method fluidCount = GT_MetaTileEntity_Hatch_Output_ME.class
                .getDeclaredMethod("getBufferedAmount", IAEFluidStack.class);
        fluidCount.setAccessible(true);
        assertEquals(7L, fluidCount.invoke(hatch, nativeFluid(fluid("a", 1))));
        assertEquals(11L, fluidCount.invoke(hatch, nativeFluid(fluid("b", 1))));
        assertEquals(0L, fluidCount.invoke(hatch, nativeFluid(fluid("c", 1))));

        bus.setConnectsToAllSides(true);
        bus.getMEOutputStorage().setCacheMode(true);
        bus.getMEOutputStorage().setCheckMode(true);
        bus.getMEOutputStorage().setPriority(-77);
        bus.getMEOutputStorage().setRefreshSeconds(5);
        bus.network.colorization = 3;
        NBTTagCompound copied = bus.getCopiedData(null);
        assertEquals("outputBusME", copied.getString("type"));
        assertFalse(copied.hasKey("cachedItems"));
        assertFalse(copied.hasKey("meStorageCell"));
        assertFalse(copied.hasKey("meRefreshTicks"));
        Bus destination = new Bus();
        destination.storeAll(new ItemStack(Items.iron_ingot, 2));
        assertTrue(destination.pasteCopiedData(null, copied));
        assertEquals(2, destination.getMEOutputAmount());
        assertTrue(destination.connectsToAllSides());
        assertTrue(destination.getMEOutputStorage().isCacheMode());
        assertTrue(destination.getMEOutputStorage().getCheckMode());
        assertEquals(-77, destination.getMEOutputStorage().getPriority());
        assertEquals(3, destination.network.colorization);
        assertEquals(2, destination.getMEOutputStorage().getRefreshSeconds());
        assertFalse(destination.pasteCopiedData(null, null));
        assertFalse(hatch.pasteCopiedData(null, copied));
        assertEquals("outputHatchME", hatch.getCopiedDataIdentifier(null));

        NBTTagCompound saved = new NBTTagCompound();
        bus.saveNBTData(saved);
        Bus reloaded = new Bus();
        reloaded.loadNBTData(saved);
        assertEquals(31, reloaded.getMEOutputAmount());
        assertTrue(reloaded.connectsToAllSides());
        assertTrue(reloaded.getMEOutputStorage().getCheckMode());
        assertEquals(-77, reloaded.getMEOutputStorage().getPriority());
        assertEquals(5, reloaded.getMEOutputStorage().getRefreshSeconds());
        assertFalse(reloaded.acceptsItemLock());
        assertTrue(reloaded.useMui2());

        hatch.getMEOutputStorage().setCheckMode(true);
        hatch.getMEOutputStorage().setPriority(42);
        hatch.setConnectsToAllSides(true);
        saved = new NBTTagCompound();
        hatch.saveNBTData(saved);
        Hatch restoredHatch = new Hatch();
        restoredHatch.loadNBTData(saved);
        assertEquals(31, restoredHatch.getMEOutputAmount());
        assertTrue(restoredHatch.connectsToAllSides());
        assertTrue(restoredHatch.getMEOutputStorage().getCheckMode());
        assertEquals(42, restoredHatch.getMEOutputStorage().getPriority());
        assertTrue(restoredHatch.useMui2());
    }

    private static void checkDataStickClicks() throws Exception {
        ItemList.Tool_DataStick.set(new ItemStack(Items.paper));
        DataStickPlayer player = dataStickPlayer();
        ItemStack stick = ItemList.Tool_DataStick.get(1);
        player.inventory.setInventorySlotContents(0, stick);

        Bus sourceBus = new Bus();
        sourceBus.getMEOutputStorage().setCacheMode(true);
        sourceBus.getMEOutputStorage().setCheckMode(true);
        sourceBus.getMEOutputStorage().setPriority(17);
        Bus targetBus = new Bus();
        assertFalse(targetBus.onRightclick(targetBus.getBaseMetaTileEntity(), player));
        assertEquals("GT5U.hatch.outputme.settings.invalid", player.messageKey);
        sourceBus.onLeftclick(sourceBus.getBaseMetaTileEntity(), player);
        assertEquals("GT5U.hatch.outputme.settings.saved", player.messageKey);
        assertTrue(targetBus.onRightclick(targetBus.getBaseMetaTileEntity(), player));
        assertEquals("GT5U.hatch.outputme.settings.loaded", player.messageKey);
        assertTrue(targetBus.getMEOutputStorage().isCacheMode());
        assertTrue(targetBus.getMEOutputStorage().getCheckMode());
        assertEquals(17, targetBus.getMEOutputStorage().getPriority());

        Hatch targetHatch = new Hatch();
        assertFalse(targetHatch.onRightclick(targetHatch.getBaseMetaTileEntity(), player));
        assertEquals("GT5U.hatch.outputme.settings.invalid", player.messageKey);
        Hatch sourceHatch = new Hatch();
        sourceHatch.getMEOutputStorage().setCacheMode(true);
        sourceHatch.getMEOutputStorage().setCheckMode(true);
        sourceHatch.getMEOutputStorage().setPriority(29);
        sourceHatch.onLeftclick(sourceHatch.getBaseMetaTileEntity(), player);
        assertEquals("GT5U.hatch.outputme.settings.saved", player.messageKey);
        assertTrue(targetHatch.onRightclick(targetHatch.getBaseMetaTileEntity(), player));
        assertEquals("GT5U.hatch.outputme.settings.loaded", player.messageKey);
        assertTrue(targetHatch.getMEOutputStorage().isCacheMode());
        assertTrue(targetHatch.getMEOutputStorage().getCheckMode());
        assertEquals(29, targetHatch.getMEOutputStorage().getPriority());

        GT_MetaTileEntity_Hatch_OutputBus source = new GT_MetaTileEntity_Hatch_OutputBus("source", 1, "", null);
        GT_MetaTileEntity_Hatch_OutputBus target = new GT_MetaTileEntity_Hatch_OutputBus("target", 1, "", null);
        assertFalse(target.onRightclick(null, player));
        assertEquals("GT5U.machines.output_bus.invalid", player.messageKey);
        source.setLockedItem(new ItemStack(Items.gold_ingot));
        source.onLeftclick(null, player);
        assertEquals("GT5U.machines.output_bus.saved", player.messageKey);
        assertTrue(target.onRightclick(null, player));
        assertEquals("GT5U.machines.output_bus.loaded", player.messageKey);
        assertTrue(target.isFilteredToItem(new ItemStack(Items.gold_ingot)));
        assertFalse(target.isFilteredToItem(new ItemStack(Items.iron_ingot)));
    }

    private static DataStickPlayer dataStickPlayer() throws Exception {
        // Initialize the player inventory without creating a server or a network connection.
        Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
        Constructor<?> constructor = (Constructor<?>) factoryClass
                .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                .invoke(factory, DataStickPlayer.class, EntityPlayer.class.getConstructor(World.class, GameProfile.class));
        constructor.setAccessible(true);
        return (DataStickPlayer) constructor.newInstance(new GT_StructureIntegrationTest.TestWorld(),
                new GameProfile(UUID.randomUUID(), "DataStickPlayer"));
    }

    private static final class DataStickPlayer extends EntityPlayerMP {

        private String messageKey;

        private DataStickPlayer() {
            super(null, null, null, null);
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            messageKey = ((ChatComponentTranslation) message).getKey();
        }
    }

    private static void checkTools() {
        Bus bus = new Bus();
        Hatch hatch = new Hatch();
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(
                new GT_StructureIntegrationTest.TestWorld());
        checkTools(bus, bus.network, player);
        checkTools(hatch, hatch.network, player);
    }

    private static void checkTools(
        MetaTileEntity output,
        Network network,
        GT_StructureIntegrationTest.TestPlayer player
    ) {
        GT_MEOutputStorage<?> storage = ((IMEOutput) output).getMEOutputStorage();
        player.setSneaking(false);
        output.onScrewdriverRightClick((byte) 2, player, 0, 0, 0);
        assertTrue(storage.isCacheMode());
        assertFalse(storage.getCheckMode());
        player.setSneaking(true);
        output.onScrewdriverRightClick((byte) 2, player, 0, 0, 0);
        assertTrue(storage.isCacheMode());
        assertTrue(storage.getCheckMode());
        assertTrue(output.onWireCutterRightClick((byte) 2, (byte) 2, player, 0, 0, 0));
        assertEquals(6, network.sides.size());
        assertFalse(network.sides.contains(ForgeDirection.UNKNOWN));
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            assertEquals(AECableType.SMART, output.getCableConnectionType(side));
        }
        assertEquals(AECableType.NONE, output.getCableConnectionType(ForgeDirection.UNKNOWN));
        output.onWireCutterRightClick((byte) 2, (byte) 2, player, 0, 0, 0);
        assertEquals(EnumSet.of(ForgeDirection.NORTH), network.sides);
        assertEquals(AECableType.NONE, output.getCableConnectionType(ForgeDirection.SOUTH));
        network.facing = (byte) ForgeDirection.EAST.ordinal();
        output.onFacingChange();
        assertEquals(EnumSet.of(ForgeDirection.EAST), network.sides);
        output.onColorChangeServer((byte) 0);
        assertEquals(AEColor.values()[15], network.color);
        output.onColorChangeServer((byte) -1);
        assertEquals(AEColor.Transparent, network.color);
    }

    private static void checkProcessing() {
        GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("me_reservation_test");
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        Bus bus = new Bus() {
            @Override
            public long getProcessingCapacity(ItemStack stack, long reservedSameType) {
                return stack.stackSize + reservedSameType <= 50 ? 100 : 0;
            }
        };
        Hatch hatch = new Hatch() {
            @Override
            public long getProcessingCapacity(FluidStack stack, long reservedSameType) {
                return stack.amount + reservedSameType <= 50 ? 100 : 0;
            }
        };
        resources.attach(bus);
        resources.attach(hatch);
        machine.mOutputBusses.add(bus);
        machine.mOutputHatches.add(hatch);
        ProcessingOutputs outputs = new ProcessingOutputs(machine);
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL,
                outputs.canFit(new ItemStack[] { item("a", 30), item("a", 30) }, null, true, false));
        assertEquals(CheckRecipeResult.SUCCESS,
                outputs.canFit(new ItemStack[] { item("a", 30), item("b", 30) }, null, true, false));
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL, outputs.canFit(
                new ItemStack[] { item("a", 40), item("b", 40), item("c", 40) }, null, true, false));
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL,
                outputs.canFit(null, new FluidStack[] { fluid("a", 30), fluid("a", 30) }, false, true));
        assertEquals(CheckRecipeResult.SUCCESS,
                outputs.canFit(null, new FluidStack[] { fluid("a", 30), fluid("b", 30) }, false, true));
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL, outputs.canFit(
                null, new FluidStack[] { fluid("a", 40), fluid("b", 40), fluid("c", 40) }, false, true));
        assertEquals(0, bus.getMEOutputAmount());
        assertEquals(0, hatch.getMEOutputAmount());
    }

    private static void checkPartitions() {
        CellFixture<IAEItemStack> items = new CellFixture<>(StorageChannel.ITEMS, IAEItemStack.class);
        PartitionCell itemCell = new PartitionCell();
        ItemStack selected = item("selected", 1);
        selected.setItemDamage(3);
        itemCell.partition = new IAEStack<?>[] { nativeItem(selected), nativeFluid(fluid("ignored", 1)) };
        itemCell.upgrades.setInventorySlotContents(0, new ItemStack(new Upgrade(Upgrades.FUZZY)));
        items.cellStack = new ItemStack(itemCell);
        assertTrue(items.storage.isFiltered());
        assertTrue(items.storage.canStore(nativeItem(selected.copy())));
        ItemStack wrongMeta = selected.copy();
        wrongMeta.setItemDamage(4);
        assertFalse(items.storage.canStore(nativeItem(wrongMeta)));
        ItemStack wrongNbt = selected.copy();
        wrongNbt.getTagCompound().setString("variant", "other");
        assertFalse(items.storage.canStore(nativeItem(wrongNbt)));
        assertFalse(items.storage.canStore(nativeItem(new ItemStack(Items.iron_ingot))));
        itemCell.upgrades.setInventorySlotContents(1, new ItemStack(new Upgrade(Upgrades.INVERTER)));
        items.cellStack = new ItemStack(itemCell);
        assertFalse(items.storage.canStore(nativeItem(selected)));
        assertTrue(items.storage.canStore(nativeItem(wrongMeta)));
        assertTrue(items.storage.canStore(nativeItem(wrongNbt)));
        itemCell.partition = new IAEStack<?>[0];
        items.cellStack = new ItemStack(itemCell);
        assertFalse(items.storage.isFiltered());
        assertTrue("Empty partitions remain unfiltered with Inverter", items.storage.canStore(nativeItem(selected)));
        itemCell.partition = new IAEStack<?>[] { nativeFluid(fluid("ignored", 1)) };
        items.cellStack = new ItemStack(itemCell);
        assertFalse("Wrong-channel partition entries are ignored", items.storage.isFiltered());

        CellFixture<IAEFluidStack> fluids = new CellFixture<>(StorageChannel.FLUIDS, IAEFluidStack.class);
        PartitionCell fluidCell = new PartitionCell();
        fluidCell.partition = new IAEStack<?>[] { nativeFluid(fluid("selected", 1)) };
        fluids.cellStack = new ItemStack(fluidCell);
        assertTrue(fluids.storage.isFiltered());
        assertTrue(fluids.storage.canStore(nativeFluid(fluid("selected", 1))));
        assertFalse(fluids.storage.canStore(nativeFluid(fluid("other", 1))));
        assertFalse(fluids.storage.canStore(nativeFluid(new FluidStack(FluidRegistry.LAVA, 1))));
        fluidCell.upgrades.setInventorySlotContents(0, new ItemStack(new Upgrade(Upgrades.INVERTER)));
        fluids.cellStack = new ItemStack(fluidCell);
        assertFalse(fluids.storage.canStore(nativeFluid(fluid("selected", 1))));
        assertTrue(fluids.storage.canStore(nativeFluid(fluid("other", 1))));
        fluids.cellStack = null;
        assertFalse(fluids.storage.isFiltered());
        assertTrue(fluids.storage.canStore(nativeFluid(fluid("selected", 1))));
    }

    private static void checkOrder() throws Exception {
        GT_MetaTileEntity_Hatch_OutputBus ordinary = new GT_MetaTileEntity_Hatch_OutputBus("normal", 0, "", null);
        assertEquals(OutputBusType.StandardUnfiltered, ordinary.getBusType());
        ordinary.setLockedItem(item("selected", 1));
        assertEquals(OutputBusType.StandardFiltered, ordinary.getBusType());
        Bus classified = new Bus();
        assertEquals(OutputBusType.MEUnfiltered, classified.getBusType());
        classified.getMEOutputStorage().setCacheMode(true);
        assertEquals(OutputBusType.MECacheUnfiltered, classified.getBusType());
        CellFixture<IAEItemStack> storage = new CellFixture<>(StorageChannel.ITEMS, IAEItemStack.class);
        PartitionCell cell = new PartitionCell();
        cell.partition = new IAEStack<?>[] { nativeItem(item("selected", 1)) };
        storage.cellStack = new ItemStack(cell);
        GT_ProcessingTestSupport.setField(classified, "storage", storage.storage);
        assertEquals(OutputBusType.MEFiltered, classified.getBusType());
        assertTrue(classified.isLocked());
        assertFalse(classified.acceptsItemLock());
        storage.storage.setCacheMode(true);
        assertEquals(OutputBusType.MECacheFiltered, classified.getBusType());

        GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("me_order_test");
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        List<OutputBusType> attempts = new ArrayList<>();
        List<OutputBusType> expected = Arrays.asList(
            OutputBusType.StandardFiltered,
            OutputBusType.MECacheFiltered,
            OutputBusType.MEFiltered,
            OutputBusType.StandardUnfiltered,
            OutputBusType.MECacheUnfiltered,
            OutputBusType.MEUnfiltered
        );
        List<OrderedBus> buses = new ArrayList<>();
        for (int index = expected.size() - 1; index >= 0; index--) {
            OrderedBus bus = new OrderedBus(expected.get(index), attempts);
            buses.add(bus);
            resources.attach(bus);
            machine.mOutputBusses.add(bus);
        }
        ProcessingOutputs outputs = new ProcessingOutputs(machine);
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL,
                outputs.canFit(new ItemStack[] { item("selected", 1) }, null, true, false));
        assertEquals(expected, attempts);
        attempts.clear();
        assertFalse(outputs.deliver(new ItemStack[] { item("selected", 1) }, null, true, false));
        assertEquals(expected, attempts);
        attempts.clear();
        assertFalse(machine.addOutput(item("selected", 1)));
        assertEquals(expected, attempts);

        for (OrderedBus bus : buses) {
            bus.matchesFilter = false;
        }
        List<OutputBusType> unfiltered = expected.subList(3, expected.size());
        attempts.clear();
        outputs.canFit(new ItemStack[] { item("other", 1) }, null, true, false);
        assertEquals(unfiltered, attempts);
        attempts.clear();
        machine.addOutput(item("other", 1));
        assertEquals(unfiltered, attempts);
    }

    private static void checkFluidOrder() throws Exception {
        Hatch classified = new Hatch();
        assertEquals(OutputHatchType.MEUnfiltered, classified.getHatchType());
        classified.getMEOutputStorage().setCacheMode(true);
        assertEquals(OutputHatchType.MECacheUnfiltered, classified.getHatchType());
        CellFixture<IAEFluidStack> storage = new CellFixture<>(StorageChannel.FLUIDS, IAEFluidStack.class);
        PartitionCell cell = new PartitionCell();
        cell.partition = new IAEStack<?>[] { nativeFluid(fluid("selected", 1)) };
        storage.cellStack = new ItemStack(cell);
        GT_ProcessingTestSupport.setField(classified, "storage", storage.storage);
        assertEquals(OutputHatchType.MEFiltered, classified.getHatchType());
        storage.storage.setCacheMode(true);
        assertEquals(OutputHatchType.MECacheFiltered, classified.getHatchType());

        GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("me_fluid_order_test");
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        List<OutputHatchType> attempts = new ArrayList<>();
        List<OutputHatchType> expected = Arrays.asList(
            OutputHatchType.StandardFiltered,
            OutputHatchType.MECacheFiltered,
            OutputHatchType.MEFiltered,
            OutputHatchType.StandardUnfiltered,
            OutputHatchType.MECacheUnfiltered,
            OutputHatchType.MEUnfiltered
        );
        List<OrderedHatch> hatches = new ArrayList<>();
        for (int index = expected.size() - 1; index >= 0; index--) {
            OrderedHatch hatch = new OrderedHatch(expected.get(index), attempts);
            resources.attach(hatch);
            hatches.add(hatch);
            machine.mOutputHatches.add(hatch);
        }
        ProcessingOutputs outputs = new ProcessingOutputs(machine);
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL,
                outputs.canFit(null, new FluidStack[] { fluid("selected", 1) }, false, true));
        assertEquals(expected, attempts);
        attempts.clear();
        assertFalse(outputs.deliver(null, new FluidStack[] { fluid("selected", 1) }, false, true));
        assertEquals(expected, attempts);
        attempts.clear();
        assertFalse(machine.addOutput(fluid("selected", 1)));
        assertEquals(expected, attempts);
        for (OrderedHatch hatch : hatches) {
            hatch.matchesFilter = false;
        }
        attempts.clear();
        outputs.canFit(null, new FluidStack[] { fluid("other", 1) }, false, true);
        assertEquals(expected.subList(3, expected.size()), attempts);
        attempts.clear();
        machine.addOutput(fluid("other", 1));
        assertEquals(expected.subList(3, expected.size()), attempts);

        machine.mOutputHatches.clear();
        SmallHatch fallback = new SmallHatch();
        SmallHatch steamOnly = new SmallHatch();
        SmallHatch locked = new SmallHatch();
        SmallHatch liquidsOnly = new SmallHatch();
        steamOnly.mMode = 1;
        locked.mMode = 8;
        locked.lockedFluid = new FluidStack(FluidRegistry.LAVA, 1);
        liquidsOnly.mMode = 4;
        assertEquals(OutputHatchType.StandardUnfiltered, fallback.getHatchType());
        assertEquals(OutputHatchType.StandardFiltered, steamOnly.getHatchType());
        assertEquals(OutputHatchType.StandardFiltered, locked.getHatchType());
        assertEquals(OutputHatchType.StandardFiltered, liquidsOnly.getHatchType());
        for (SmallHatch hatch : Arrays.asList(fallback, steamOnly, locked, liquidsOnly)) {
            resources.attach(hatch);
            machine.mOutputHatches.add(hatch);
        }
        FluidStack requested = new FluidStack(FluidRegistry.WATER, 18);
        assertFalse(machine.addOutput(requested));
        assertEquals(18, requested.amount);
        assertEquals(10, liquidsOnly.getFluidAmount());
        assertEquals(8, fallback.getFluidAmount());
        assertEquals(0, steamOnly.getFluidAmount());
        assertEquals(0, locked.getFluidAmount());
        liquidsOnly.mFluid = null;
        fallback.mFluid = null;
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL,
                outputs.canFit(null, new FluidStack[] { new FluidStack(FluidRegistry.WATER, 21) }, false, true));
        assertEquals(CheckRecipeResult.SUCCESS,
                outputs.canFit(null, new FluidStack[] { requested }, false, true));
        assertEquals(0, liquidsOnly.getFluidAmount());
        assertTrue(outputs.deliver(null, new FluidStack[] { requested }, false, true));
        assertEquals(0, requested.amount);
        assertEquals(10, liquidsOnly.getFluidAmount());
        assertEquals(8, fallback.getFluidAmount());
        liquidsOnly.mFluid = null;
        fallback.mFluid = null;
        FluidStack steam = new FluidStack(GT_ProcessingTestSupport.fluid("steam"), 5);
        assertFalse(machine.addOutput(steam));
        assertEquals(5, steamOnly.getFluidAmount());
        assertEquals(0, liquidsOnly.getFluidAmount());
        steamOnly.mFluid = null;
        assertTrue(outputs.deliver(null, new FluidStack[] { steam.copy() }, false, true));
        assertEquals(5, steamOnly.getFluidAmount());

        outputs.setFluidOutputFilter((hatch, stack, index) -> hatch == fallback && index == 1);
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL,
                outputs.canFit(null, new FluidStack[] { new FluidStack(FluidRegistry.WATER, 5) }, false, true));
        FluidStack[] indexed = { null, new FluidStack(FluidRegistry.WATER, 5) };
        assertEquals(CheckRecipeResult.SUCCESS, outputs.canFit(null, indexed, false, true));
        assertTrue(outputs.deliver(null, indexed, false, true));
        assertEquals(5, fallback.getFluidAmount());
        assertEquals(0, liquidsOnly.getFluidAmount());

        machine.mOutputHatches.clear();
        SmallHatch automaticLock = new SmallHatch();
        automaticLock.mMode = 9;
        resources.attach(automaticLock);
        machine.mOutputHatches.add(automaticLock);
        assertFalse(machine.addOutput(new FluidStack(FluidRegistry.WATER, 5)));
        assertSame(FluidRegistry.WATER, automaticLock.lockedFluid.getFluid());

        Hatch staleLock = new Hatch() {
            @Override
            public boolean isFilteredToFluid(FluidStack stack) {
                return getMEOutputStorage().isAllowed(nativeFluid(stack));
            }
        };
        NBTTagCompound oldData = new NBTTagCompound();
        oldData.setByte("mMode", (byte) 8);
        oldData.setTag("lockedFluid", new FluidStack(FluidRegistry.LAVA, 1).writeToNBT(new NBTTagCompound()));
        staleLock.loadNBTData(oldData);
        GT_ProcessingTestSupport.setField(staleLock, "storage", storage.storage);
        assertTrue(staleLock.isFluidLocked());
        assertTrue(staleLock.isFiltered());
        assertNull(staleLock.getLockedFluidName());
        assertSame(FluidRegistry.LAVA, staleLock.lockedFluid.getFluid());
        machine.mOutputHatches.clear();
        resources.attach(staleLock);
        machine.mOutputHatches.add(staleLock);
        ProcessingOutputs restoredOutputs = new ProcessingOutputs(machine);
        assertEquals(CheckRecipeResult.SUCCESS,
                restoredOutputs.canFit(null, new FluidStack[] { fluid("selected", 5) }, false, true));
        assertTrue(restoredOutputs.deliver(null, new FluidStack[] { fluid("selected", 5) }, false, true));
        assertEquals(5, staleLock.getMEOutputAmount());
        assertFalse(machine.addOutput(fluid("selected", 5)));
        assertEquals(10, staleLock.getMEOutputAmount());
    }

    private static void checkOwner() throws Exception {
        ItemList.Hatch_Output_Bus_ME.set(Items.iron_ingot);
        ItemList.Hatch_Output_ME.set(Items.iron_ingot);
        GT_StructureIntegrationTest.TestWorld world = new GT_StructureIntegrationTest.TestWorld();
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        world.playerEntities.add(player);
        Field owner = AENetworkProxy.class.getDeclaredField("owner");
        owner.setAccessible(true);
        Field node = AENetworkProxy.class.getDeclaredField("node");
        node.setAccessible(true);
        MetaTileEntity[] outputs = {
                new GT_MetaTileEntity_Hatch_OutputBus_ME("real_me_bus", 1, new String[0], null),
                new GT_MetaTileEntity_Hatch_Output_ME("real_me_hatch", 4, new String[0], null) };
        for (MetaTileEntity output : outputs) {
            GT_StructureIntegrationTest.TestTile tile = new GT_StructureIntegrationTest.TestTile(
                    world, output, ForgeDirection.NORTH);
            tile.setOwnerName(player.getCommandSenderName());
            AENetworkProxy proxy = output.getProxy();
            assertSame(player, owner.get(proxy));
            assertNull("Assign ownership before initializing a grid node", node.get(proxy));
            assertSame(proxy, output.getProxy());
        }
        GT_MetaTileEntity_Hatch_OutputBus_ME detached = new GT_MetaTileEntity_Hatch_OutputBus_ME(
                "detached_me_bus", 1, new String[0], null);
        new GT_StructureIntegrationTest.TestTile(null, detached, ForgeDirection.NORTH);
        AENetworkProxy proxy = detached.getProxy();
        assertNull(owner.get(proxy));
        assertNull(node.get(proxy));
    }

    private static void checkCapacityOverrides() {
        long[] capacity = { 5 };
        int[] itemChecks = { 0 };
        int[] fluidChecks = { 0 };
        Bus bus = new Bus() {
            @Override
            public long getProcessingCapacity(ItemStack stack) {
                itemChecks[0]++;
                return capacity[0];
            }
        };
        Hatch hatch = new Hatch() {
            @Override
            public long getProcessingCapacity(FluidStack stack) {
                fluidChecks[0]++;
                return capacity[0];
            }
        };
        GT_MetaTileEntity_VacuumFreezer machine = new GT_MetaTileEntity_VacuumFreezer("me_capacity_override_test");
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        resources.attach(bus);
        resources.attach(hatch);
        machine.mOutputBusses.add(bus);
        machine.mOutputHatches.add(hatch);
        ProcessingOutputs outputs = new ProcessingOutputs(machine);
        assertEquals(CheckRecipeResult.SUCCESS,
                outputs.canFit(new ItemStack[] { item("a", 5) }, new FluidStack[] { fluid("a", 5) }, true, true));
        assertEquals(1, itemChecks[0]);
        assertEquals(1, fluidChecks[0]);
        capacity[0] = 0;
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL,
                outputs.canFit(new ItemStack[] { item("a", 1) }, null, true, false));
        assertEquals(CheckRecipeResult.FLUID_OUTPUT_FULL,
                outputs.canFit(null, new FluidStack[] { fluid("a", 1) }, false, true));
        assertEquals(2, itemChecks[0]);
        assertEquals(2, fluidChecks[0]);
        assertEquals(0, bus.getProcessingCapacity(item("a", 1), 7));
        assertEquals(0, hatch.getProcessingCapacity(fluid("a", 1), 7));
    }

    private static void checkVoidCells() throws Exception {
        ItemVoidStorageCell voidCell = voidCell();
        CellFixture<IAEItemStack> items = new CellFixture<>(StorageChannel.ITEMS, IAEItemStack.class);
        items.cellStack = new ItemStack(voidCell);
        assertTrue(items.storage.isVoidCell());
        assertEquals(Long.MAX_VALUE, items.storage.getCapacity());
        Bus bus = new Bus();
        bus.storeAll(new ItemStack(Items.iron_ingot, 3));
        GT_ProcessingTestSupport.setField(bus, "storage", items.storage);
        ItemStack requestedItems = new ItemStack(Items.gold_ingot, Integer.MAX_VALUE);
        assertTrue(bus.storeAll(requestedItems));
        assertEquals(0, requestedItems.stackSize);
        assertEquals("Inserting a void cell preserves previous output", 3, bus.getMEOutputAmount());
        assertEquals(Long.MAX_VALUE - 3, bus.getProcessingCapacity(new ItemStack(Items.iron_ingot)));
        items.cellStack = null;
        assertFalse(items.storage.isVoidCell());
        bus.storeAll(new ItemStack(Items.iron_ingot, 2));
        assertEquals(5, bus.getMEOutputAmount());

        CellFixture<IAEFluidStack> fluids = new CellFixture<>(StorageChannel.FLUIDS, IAEFluidStack.class);
        fluids.cellStack = new ItemStack(voidCell);
        assertTrue(fluids.storage.isVoidCell());
        assertEquals(Long.MAX_VALUE, fluids.storage.getCapacity());
        Hatch hatch = new Hatch();
        hatch.fill(new FluidStack(FluidRegistry.WATER, 7), true);
        GT_ProcessingTestSupport.setField(hatch, "storage", fluids.storage);
        FluidStack requestedFluid = new FluidStack(FluidRegistry.LAVA, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, hatch.fill(requestedFluid, false));
        assertEquals(7, hatch.getMEOutputAmount());
        assertEquals(Integer.MAX_VALUE, hatch.fill(requestedFluid, true));
        assertEquals(Integer.MAX_VALUE, requestedFluid.amount);
        assertEquals(7, hatch.getMEOutputAmount());
        assertEquals(Long.MAX_VALUE - 7, hatch.getProcessingCapacity(new FluidStack(FluidRegistry.WATER, 1)));
        fluids.cellStack = null;
        hatch.fill(new FluidStack(FluidRegistry.WATER, 2), true);
        assertEquals(9, hatch.getMEOutputAmount());
    }

    private static ItemVoidStorageCell voidCell() throws Exception {
        // AE item registration is outside this fixture; initialize Minecraft Item state without registering features.
        Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
        Constructor<?> constructor = (Constructor<?>) factoryClass
                .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                .invoke(factory, ItemVoidStorageCell.class, Item.class.getDeclaredConstructor());
        constructor.setAccessible(true);
        return (ItemVoidStorageCell) constructor.newInstance();
    }

    private static void checkDisplay() throws Exception {
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        AEConfig.instance = new AEConfig(new File("build/tmp/me-output-display/AppliedEnergistics2.cfg"));
        Launch.classLoader.registerTransformer("appeng.transformer.asm.ASMIntegration");
        LoadController controller = new LoadController(Loader.instance());
        HashBiMap<ModContainer, Object> modObjects = HashBiMap.create();
        ModContainer aeMod = (ModContainer) Proxy.newProxyInstance(GT_Mui2MEOutputLogicTest.class.getClassLoader(),
                new Class<?>[] { ModContainer.class }, (proxy, method, args) ->
                        "getModId".equals(method.getName()) ? "appliedenergistics2"
                                : defaultValue(method.getReturnType()));
        modObjects.put(aeMod, AppEng.instance());
        GT_ProcessingTestSupport.setField(controller, "modObjectList", modObjects);
        GT_ProcessingTestSupport.setField(Loader.instance(), "modController", controller);
        try (InputStream language = GT_Mui2MEOutputLogicTest.class
                .getResourceAsStream("/assets/gregtech/lang/en_US.lang")) {
            StringTranslate.inject(language);
        }
        Bus bus = new Bus();
        Hatch hatch = new Hatch();
        bus.storeAll(new ItemStack(Items.iron_ingot, 192));
        hatch.fill(new FluidStack(FluidRegistry.WATER, 64000), true);
        String itemName = new ItemStack(Items.iron_ingot).getDisplayName();
        String fluidName = new FluidStack(FluidRegistry.WATER, 1).getLocalizedName();
        assertTrue(scanner(bus).contains(itemName + ": 192"));
        assertTrue(scanner(hatch).contains(fluidName + ": 64,000 L"));
        assertTrue(scanner(bus).contains("1,600"));
        assertTrue(scanner(hatch).contains("128,000 L"));
        assertFalse(scanner(bus).contains("Storage Cache:"));
        assertEquals(0, hatch.getCapacity());
        assertEquals(0, hatch.getTankInfo(ForgeDirection.NORTH).length);
        checkWailaSnapshot(bus, 192, false);
        checkWailaSnapshot(hatch, 64000, false);

        NBTTagCompound saved = new NBTTagCompound();
        bus.saveNBTData(saved);
        Bus restoredBus = new Bus();
        restoredBus.loadNBTData(saved);
        assertEquals(scanner(bus), scanner(restoredBus));
        saved = new NBTTagCompound();
        hatch.saveNBTData(saved);
        Hatch restoredHatch = new Hatch();
        restoredHatch.loadNBTData(saved);
        assertEquals(scanner(hatch), scanner(restoredHatch));

        CellFixture<IAEItemStack> items = new CellFixture<>(StorageChannel.ITEMS, IAEItemStack.class);
        CellFixture<IAEFluidStack> fluids = new CellFixture<>(StorageChannel.FLUIDS, IAEFluidStack.class);
        items.contents.add(AEItemStack.create(new ItemStack(Items.gold_ingot)).setStackSize(192));
        fluids.contents.add(AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1)).setStackSize(64000));
        GT_ProcessingTestSupport.setField(bus, "storage", items.storage);
        GT_ProcessingTestSupport.setField(hatch, "storage", fluids.storage);
        assertFalse("A cell outside cache mode is not exposed as output storage",
                scanner(bus).contains("Storage Cache:"));
        items.storage.setCacheMode(true);
        fluids.storage.setCacheMode(true);
        assertTrue(scanner(bus).contains(new ItemStack(Items.gold_ingot).getDisplayName() + ": 192"));
        assertTrue(scanner(hatch).contains(new FluidStack(FluidRegistry.LAVA, 1).getLocalizedName() + ": 64,000 L"));
        assertTrue(scanner(bus).contains("Storage Cache:"));
        assertTrue(scanner(hatch).contains("Storage Cache:"));
        checkWailaSnapshot(bus, 192, true);
        checkWailaSnapshot(hatch, 64000, true);
        assertEquals(192, items.contents.get(0).getStackSize());
        assertEquals(64000, fluids.contents.get(0).getStackSize());
        items.storage.getCellContents().get(0).setStackSize(1);
        assertEquals("Inspection returns copies, never live cell entries", 192, items.contents.get(0).getStackSize());

        items.contents.clear();
        long largeAmount = (long) Integer.MAX_VALUE + 100;
        for (int i = 0; i < 105; i++) {
            items.contents.add(AEItemStack.create(item("display_" + i, 1)).setStackSize(largeAmount + i));
        }
        String[] scanner = bus.getInfoData();
        assertEquals("Scanner limits each section to 100 entries", 106, scanner.length);
        NBTTagCompound tag = new NBTTagCompound();
        bus.getWailaNBTData(null, null, tag, null, 0, 0, 0);
        assertEquals(105, tag.getInteger("cacheCount"));
        assertEquals(10, tag.getTagList("cacheStacks", 10).tagCount());
        assertEquals(largeAmount + 104, tag.getTagList("cacheStacks", 10).getCompoundTagAt(0).getLong("Amount"));
        IWailaDataAccessor accessor = accessor(tag);
        List<String> tooltip = new ArrayList<>();
        bus.getWailaAdvancedBody(null, tooltip, accessor, config());
        assertTrue(String.join("\n", tooltip).contains("95 more"));
        items.network.active = true;
        assertFalse("The network can read the cell while online",
                items.storage.getCellArray(StorageChannel.ITEMS).isEmpty());
        items.network.active = false;
        assertTrue(items.storage.getCellArray(StorageChannel.ITEMS).isEmpty());
        assertTrue("Offline scanner still lists the cell", scanner(bus).contains("105 cached stacks"));
        checkClientLocalization(bus, Items.iron_ingot.getUnlocalizedName() + ".name");
        checkClientLocalization(hatch, "tile.water.name");
    }

    private static void checkClientLocalization(MetaTileEntity machine, String nameKey) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        machine.getWailaNBTData(null, null, tag, null, 0, 0, 0);
        try (InputStream language = new ByteArrayInputStream(
                (nameKey + "=Client localized name\n").getBytes(StandardCharsets.UTF_8))) {
            StringTranslate.inject(language);
        }
        List<String> tooltip = new ArrayList<>();
        machine.getWailaBody(null, tooltip, accessor(tag), config());
        assertTrue("WAILA must resolve names on the client after receiving the NBT: " + nameKey + " " + tooltip,
                String.join("\n", tooltip).contains("Client localized name"));
    }

    private static void checkOfflineCapacity() {
        for (boolean cacheMode : new boolean[] { false, true }) {
            Bus bus = new Bus();
            Hatch hatch = new Hatch();
            bus.getMEOutputStorage().setCacheMode(cacheMode);
            hatch.getMEOutputStorage().setCacheMode(cacheMode);
            bus.getMEOutputStorage().setCheckMode(true);
            hatch.getMEOutputStorage().setCheckMode(true);
            ItemStack tooMany = new ItemStack(Items.iron_ingot, 1601);
            assertFalse(bus.storeAll(tooMany));
            assertEquals(1601, tooMany.stackSize);
            assertEquals(0, bus.getMEOutputAmount());
            assertEquals(0, hatch.fill(new FluidStack(FluidRegistry.WATER, 128001), false));
            assertEquals(0, hatch.fill(new FluidStack(FluidRegistry.WATER, 128001), true));
            assertEquals(0, hatch.getMEOutputAmount());
            assertTrue(bus.storeAll(new ItemStack(Items.iron_ingot, 1599)));
            assertEquals(127999, hatch.fill(new FluidStack(FluidRegistry.WATER, 127999), true));
            assertFalse(bus.storeAll(new ItemStack(Items.gold_ingot, 2)));
            assertEquals(0, hatch.fill(new FluidStack(FluidRegistry.LAVA, 2), true));
            assertTrue(bus.storeAll(new ItemStack(Items.gold_ingot, 1)));
            assertEquals(1, hatch.fill(new FluidStack(FluidRegistry.LAVA, 1), false));
            assertEquals(127999, hatch.getMEOutputAmount());
            assertEquals(1, hatch.fill(new FluidStack(FluidRegistry.LAVA, 1), true));
            assertFalse(bus.storeAll(new ItemStack(Items.gold_ingot, 1)));
            assertEquals(0, hatch.fill(new FluidStack(FluidRegistry.LAVA, 1), true));
            assertEquals(1600, bus.getMEOutputAmount());
            assertEquals(128000, hatch.getMEOutputAmount());

            bus.getMEOutputStorage().setCheckMode(false);
            hatch.getMEOutputStorage().setCheckMode(false);
            assertTrue("The same-tick allowance remains available outside Check Mode",
                    bus.storeAll(new ItemStack(Items.iron_ingot, 64)));
            assertEquals(1000, hatch.fill(new FluidStack(FluidRegistry.WATER, 1000), true));
            bus.onPostTick(bus.getBaseMetaTileEntity(), 1);
            hatch.onPostTick(hatch.getBaseMetaTileEntity(), 1);
            assertFalse(bus.storeAll(new ItemStack(Items.iron_ingot, 1)));
            assertEquals(0, hatch.fill(new FluidStack(FluidRegistry.WATER, 1), true));
            assertEquals(1664, bus.getMEOutputAmount());
            assertEquals(129000, hatch.getMEOutputAmount());
        }
    }

    private static String scanner(MetaTileEntity machine) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", machine.getInfoData()));
    }

    private static void checkWailaSnapshot(MetaTileEntity machine, long amount, boolean hasCell) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.getWailaNBTData(null, null, tag, null, 0, 0, 0);
        assertEquals(1, tag.getInteger("stackCount"));
        assertEquals(amount, tag.getTagList("stacks", 10).getCompoundTagAt(0).getLong("Amount"));
        assertEquals(hasCell, tag.hasKey("cacheCount"));
        if (hasCell) {
            assertEquals(amount, tag.getTagList("cacheStacks", 10).getCompoundTagAt(0).getLong("Amount"));
        }
        IWailaDataAccessor accessor = accessor(tag);
        assertEquals(hasCell, machine.hasWailaAdvancedBody(null, accessor, config()));
        List<String> tooltip = new ArrayList<>();
        machine.getWailaBody(null, tooltip, accessor, config());
        assertFalse(String.join("\n", tooltip).contains("Storage Cache:"));
        tooltip.clear();
        machine.getWailaAdvancedBody(null, tooltip, accessor, config());
        assertEquals(hasCell, String.join("\n", tooltip).contains("Storage Cache:"));
        if (hasCell) {
            String prefix = machine instanceof Bus ? "item" : "fluid";
            String empty = StatCollector.translateToLocal("GT5U.waila.hatch.outputme." + prefix + "_cache_empty");
            assertFalse("Cell names and amounts must deserialize on the client",
                    String.join("\n", tooltip).contains(empty));
            assertEquals(3, tooltip.size());
        }
    }

    private static IWailaDataAccessor accessor(NBTTagCompound tag) {
        return (IWailaDataAccessor) Proxy.newProxyInstance(GT_Mui2MEOutputLogicTest.class.getClassLoader(),
                new Class<?>[] { IWailaDataAccessor.class }, (proxy, method, args) ->
                        "getNBTData".equals(method.getName()) ? tag : defaultValue(method.getReturnType()));
    }

    private static IWailaConfigHandler config() {
        return (IWailaConfigHandler) Proxy.newProxyInstance(GT_Mui2MEOutputLogicTest.class.getClassLoader(),
                new Class<?>[] { IWailaConfigHandler.class }, (proxy, method, args) -> true);
    }

    private static ItemStack item(String variant, int count) {
        ItemStack item = new ItemStack(Items.gold_ingot, count);
        item.setTagCompound(new NBTTagCompound());
        item.getTagCompound().setString("variant", variant);
        return item;
    }

    private static FluidStack fluid(String variant, int count) {
        FluidStack fluid = new FluidStack(FluidRegistry.WATER, count);
        fluid.tag = new NBTTagCompound();
        fluid.tag.setString("variant", variant);
        return fluid;
    }

    private static IAEItemStack nativeItem(ItemStack item) {
        return (IAEItemStack) Proxy.newProxyInstance(GT_Mui2MEOutputLogicTest.class.getClassLoader(),
                new Class<?>[] { IAEItemStack.class }, (proxy, method, args) -> {
                    if ("getItemStack".equals(method.getName())) {
                        return item;
                    }
                    if ("copy".equals(method.getName())) {
                        return nativeItem(item.copy());
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static IAEFluidStack nativeFluid(FluidStack fluid) {
        return (IAEFluidStack) Proxy.newProxyInstance(
            GT_Mui2MEOutputLogicTest.class.getClassLoader(),
            new Class<?>[] { IAEFluidStack.class },
            (proxy, method, args) -> {
                if ("getFluidStack".equals(method.getName())) {
                    return fluid;
                }
                if ("copy".equals(method.getName())) {
                    return nativeFluid(fluid.copy());
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static class Bus extends GT_MetaTileEntity_Hatch_OutputBus_ME {

        final Network network = new Network();

        Bus() {
            super("me_bus_test", 1, new String[0], null);
            setBaseMetaTileEntity(network.host(this));
        }

        @Override
        public AENetworkProxy getProxy() {
            return network;
        }

        @Override
        protected void markStorageDirty() {
        }

    }

    private static class OrderedBus extends Bus {

        private final OutputBusType type;
        private final List<OutputBusType> attempts;
        boolean matchesFilter = true;

        OrderedBus(OutputBusType type, List<OutputBusType> attempts) {
            this.type = type;
            this.attempts = attempts;
        }

        @Override
        public OutputBusType getBusType() {
            return type;
        }

        @Override
        public boolean isFilteredToItem(ItemStack stack) {
            return matchesFilter;
        }

        @Override
        public long getProcessingCapacity(ItemStack stack, long reservedSameType) {
            attempts.add(type);
            return 0;
        }

        @Override
        public boolean storeAll(ItemStack stack) {
            attempts.add(type);
            return false;
        }
    }

    private static class PartitionCell extends Item implements ICellWorkbenchItem {

        final InventoryBasic upgrades = new InventoryBasic("upgrades", false, 2);
        IAEStack<?>[] partition = new IAEStack<?>[0];

        @Override
        public boolean isEditable(ItemStack stack) {
            return true;
        }

        @Override
        public IInventory getUpgradesInventory(ItemStack stack) {
            return upgrades;
        }

        @Override
        public IAEStackInventory getConfigAEInventory(ItemStack stack) {
            return new IAEStackInventory(null, partition.length) {
                @Override
                public IAEStack<?> getAEStackInSlot(int slot) {
                    return partition[slot];
                }
            };
        }

        @Override
        public FuzzyMode getFuzzyMode(ItemStack stack) {
            return FuzzyMode.IGNORE_ALL;
        }

        @Override
        public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
        }
    }

    private static class Upgrade extends Item implements IUpgradeModule {

        private final Upgrades type;

        Upgrade(Upgrades type) {
            this.type = type;
        }

        @Override
        public Upgrades getType(ItemStack stack) {
            return type;
        }
    }

    private static class Hatch extends GT_MetaTileEntity_Hatch_Output_ME {

        final Network network = new Network();

        Hatch() {
            super("me_hatch_test", 4, new String[0], null);
            setBaseMetaTileEntity(network.host(this));
        }

        @Override
        public AENetworkProxy getProxy() {
            return network;
        }

        @Override
        protected void markStorageDirty() {
        }
    }

    private static class OrderedHatch extends Hatch {

        private final OutputHatchType type;
        private final List<OutputHatchType> attempts;
        boolean matchesFilter = true;

        OrderedHatch(OutputHatchType type, List<OutputHatchType> attempts) {
            this.type = type;
            this.attempts = attempts;
        }

        @Override
        public OutputHatchType getHatchType() {
            return type;
        }

        @Override
        public boolean isFilteredToFluid(FluidStack stack) {
            return matchesFilter;
        }

        @Override
        public long getProcessingCapacity(FluidStack stack, long reservedSameType) {
            attempts.add(type);
            return 0;
        }

        @Override
        public int fill(FluidStack stack, boolean doFill) {
            attempts.add(type);
            return 0;
        }
    }

    private static class SmallHatch extends GT_MetaTileEntity_Hatch_Output {

        SmallHatch() {
            super("small_output_test", 1, new String[0], null);
        }

        @Override
        public int getCapacity() {
            return 10;
        }

        @Override
        protected void markStorageDirty() {
        }

        @Override
        public void doDisplayThings() {
        }
    }

    private static class Network extends AENetworkProxy {

        int events;
        boolean active;
        byte facing = (byte) ForgeDirection.NORTH.ordinal();
        byte colorization = -1;
        AEColor color = AEColor.Transparent;
        EnumSet<ForgeDirection> sides = EnumSet.of(ForgeDirection.NORTH);

        Network() {
            super(null, "proxy", new ItemStack(Items.iron_ingot), true);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public IGridNode getNode() {
            return null;
        }

        @Override
        public IStorageGrid getStorage() {
            return (IStorageGrid) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IStorageGrid.class },
                (proxy, method, args) -> defaultValue(method.getReturnType())
            );
        }

        @Override
        public void setValidSides(EnumSet<ForgeDirection> sides) {
            this.sides = EnumSet.copyOf(sides);
        }

        @Override
        public void setColor(AEColor color) {
            this.color = color;
        }

        @Override
        public IGrid getGrid() {
            return (IGrid) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IGrid.class },
                    (proxy, method, args) -> {
                        if ("postEvent".equals(method.getName())) {
                            events++;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
        }

        IGregTechTileEntity host(MetaTileEntity output) {
            return (IGregTechTileEntity) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { IGregTechTileEntity.class }, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getMetaTileEntity":
                                return output;
                            case "isServerSide":
                                return true;
                            case "getFrontFacing":
                                return facing;
                            case "getColorization":
                                return colorization;
                            case "setColorization":
                                colorization = (Byte) args[0];
                                return colorization;
                            default:
                                return defaultValue(method.getReturnType());
                        }
                    });
        }
    }

    private static class CellFixture<T extends IAEStack<T>> {

        final Network network = new Network();
        final Class<T> stackType;
        final GT_MEOutputStorage<T> storage;
        ItemStack cellStack = new ItemStack(Items.diamond);
        long stored;
        long buffered;
        long simulated;
        boolean filtered;
        final List<T> contents = new ArrayList<>();

        @SuppressWarnings("unchecked")
        CellFixture(StorageChannel channel, Class<T> stackType) {
            this.stackType = stackType;
            ICellInventory<T> inventory = (ICellInventory<T>) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { ICellInventory.class }, (proxy, method, args) -> {
                        if ("getStoredItemCount".equals(method.getName())) {
                            return stored;
                        }
                        if ("getRemainingItemCount".equals(method.getName())) {
                            return 100 - stored;
                        }
                        return defaultValue(method.getReturnType());
                    });
            IAEStackType<T> type = (IAEStackType<T>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IAEStackType.class },
                (proxy, method, args) -> "createPrimitiveList".equals(method.getName())
                        ? itemList() : defaultValue(method.getReturnType())
            );
            ICellInventoryHandler<T> handler = (ICellInventoryHandler<T>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { ICellInventoryHandler.class },
                (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getCellInv":
                                return inventory;
                            case "getStackType":
                                return type;
                            case "getAccess":
                                return AccessRestriction.READ_WRITE;
                            case "isPreformatted":
                                return filtered;
                            case "canAccept":
                                return !filtered;
                            case "getAvailableItems":
                                return itemList();
                            case "injectItems":
                                T input = (T) args[0];
                                assertEquals(Actionable.SIMULATE, args[1]);
                                simulated = input.getStackSize();
                                return !filtered && simulated <= 100 - stored ? null : input;
                            default:
                                return defaultValue(method.getReturnType());
                        }
                    });
            storage = new GT_MEOutputStorage<T>(
                channel,
                1600,
                () -> cellStack,
                () -> network,
                BaseActionSource::new,
                () -> {
                },
                stack -> {
                },
                stack -> buffered
            ) {
                @Override
                protected IMEInventoryHandler<T> loadCell(ItemStack stack) {
                    return handler;
                }
            };
        }

        @SuppressWarnings("unchecked")
        private IItemList<T> itemList() {
            return (IItemList<T>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IItemList.class },
                (proxy, method, args) -> "iterator".equals(method.getName()) ? contents.iterator()
                        : defaultValue(method.getReturnType())
            );
        }

        @SuppressWarnings("unchecked")
        T newStack(long initialAmount) {
            long[] amount = { initialAmount };
            return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { stackType },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getStackSize":
                                return amount[0];
                            case "setStackSize":
                                amount[0] = (Long) args[0];
                                return proxy;
                            case "copy":
                                return newStack(amount[0]);
                            default:
                                return defaultValue(method.getReturnType());
                        }
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == float.class) {
            return 0F;
        }
        return null;
    }
}
