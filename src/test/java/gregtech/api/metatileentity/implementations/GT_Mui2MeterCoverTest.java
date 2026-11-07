package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IProvideEnergyCover;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.CoverUIFactory;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_RecipeAdder;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverEUMeter;
import gregtech.common.covers.CoverItemMeter;
import gregtech.common.covers.CoverLiquidMeter;
import gregtech.common.covers.GT_Cover_EUMeter;
import gregtech.common.covers.GT_Cover_ItemMeter;
import gregtech.common.covers.GT_Cover_LiquidMeter;
import gregtech.common.items.GT_MetaGenerated_Item_02;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_PortableTank;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_SuperChest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.GatewayElectricItemManager;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2MeterCoverTest {

    @Test
    public void nativeMeterPacketsControlTheActualThresholdedRedstoneOutput() throws Throwable {
        run("controls");
    }

    @Test
    public void energyMetersReadExternalStorageAndSynchronizeBatteryCapacity() throws Throwable {
        run("energy_storage");
    }

    @Test
    public void meterNbtAndPacketsMigrateLegacyModesAndPreserveThresholds() throws Throwable {
        run("persistence");
    }

    @Test
    public void itemMeterCountsDigitalChestStorageAndRejectsLateGuiPackets() throws Throwable {
        run("special_hosts");
    }

    @Test
    public void itemMeterOpensWithTheRegisteredPlaceholderAndTracksEmptySlots() throws Throwable {
        run("registered_preview");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        Thread.currentThread().setName("Server thread");
        GTGuis.init();
        if (!"registered_preview".equals(scenario)) {
            ItemList.Display_ITS_FREE.set(new ItemStack(Items.paper));
        }
        if ("controls".equals(scenario)) {
            checkEnergyControls();
            checkItemControls();
            checkLiquidControls();
        } else if ("energy_storage".equals(scenario)) {
            checkExternalEnergyStorage();
            checkBatteryCapacitySync();
        } else if ("persistence".equals(scenario)) {
            checkPersistence();
        } else if ("special_hosts".equals(scenario)) {
            checkDigitalChest();
            checkLatePackets();
        } else if ("registered_preview".equals(scenario)) {
            checkRegisteredPreview();
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkRegisteredPreview() throws Exception {
        GregTech_API.sRecipeFile = GregTech_API.sMachineFile;
        GT_Values.RA = new GT_RecipeAdder();
        ItemList.ThermosCan_Empty.set(Items.bucket);
        ItemList.Bottle_Empty.set(Items.glass_bottle);
        ItemList.IC2_Food_Can_Empty.set(Items.bowl);
        ItemList.IC2_Food_Can_Filled.set(Items.bread);
        ItemList.IC2_Food_Can_Spoiled.set(Items.rotten_flesh);
        ItemList.IC2_PlantballCompressed.set(Items.wheat);
        ItemList.Dye_Cocoa.set(Items.dye);
        ItemList.Cell_Empty.set(Items.bucket);
        ItemList.IC2_Fuel_Can_Filled.set((ItemStack) null);
        ItemList.IC2_Fuel_Can_Empty.set((ItemStack) null);
        ItemList.IC2_ForgeHammer.set((ItemStack) null);
        ItemList.IC2_WireCutter.set((ItemStack) null);
        GT_MetaGenerated_Item_02 items = new GT_MetaGenerated_Item_02();
        GT_MetaTileEntity_Hatch_InputBus bus = itemBus();
        Fixture fixture = new Fixture(new GT_Cover_ItemMeter(), bus);
        GenericSyncValue<?, ?> preview = fixture.handlers.findSyncHandler("slot_preview", GenericSyncValue.class);
        ItemStack placeholder = ItemList.Display_ITS_FREE.get(1);
        assertNotNull(placeholder);
        assertSame(items, placeholder.getItem());
        assertEquals(32765, placeholder.getItemDamage());
        assertTrue(items.mEnabledItems.get(765));
        assertFalse(items.mVisibleItems.get(765));
        assertTrue(GT_Utility.areStacksEqual(placeholder, (ItemStack) preview.getValue()));
        fixture.intValue("slot", 0);
        preview.updateCacheFromSource(false);
        assertTrue(GT_Utility.areStacksEqual(placeholder, (ItemStack) preview.getValue()));
        bus.mInventory[0] = new ItemStack(Items.redstone, 12);
        preview.updateCacheFromSource(false);
        assertSame(Items.redstone, ((ItemStack) preview.getValue()).getItem());
        bus.mInventory[0] = null;
        preview.updateCacheFromSource(false);
        assertTrue(GT_Utility.areStacksEqual(placeholder, (ItemStack) preview.getValue()));
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            preview.write(packet);
            preview.read(packet);
            assertTrue(GT_Utility.areStacksEqual(placeholder, (ItemStack) preview.getValue()));
        } finally {
            packet.release();
        }
    }

    private static void checkEnergyControls() throws Exception {
        Fixture fixture = new Fixture(new GT_Cover_EUMeter(), energyBuffer());
        CoverEUMeter meter = (CoverEUMeter) fixture.cover;
        assertEquals(20, meter.getTickRate());
        assertFalse(meter.isRedstoneSensitive(0));
        long[] amounts = {200, 30, 40, 12, 24, 30};
        int[] signals = {3, 5, 8, 3, 2, 5};
        for (CoverEUMeter.EnergyType type : CoverEUMeter.EnergyType.values()) {
            fixture.packet(fixture.handlers.findSyncHandler("energy_type", EnumSyncValue.class),
                packet -> NetworkUtils.writeEnumValue(packet, type));
            assertSame(type, meter.getType());
            fixture.invert(false);
            fixture.longValue("threshold", 0);
            fixture.assertSignal(signals[type.ordinal()]);
            fixture.longValue("threshold", amounts[type.ordinal()] + 1);
            fixture.assertSignal(0);
            fixture.invert(true);
            fixture.assertSignal(15 - signals[type.ordinal()]);
            fixture.longValue("threshold", amounts[type.ordinal()]);
            fixture.assertSignal(0);
        }
        fixture.longValue("threshold", Long.MAX_VALUE);
        assertEquals(100, meter.getThreshold());
        fixture.longValue("threshold", -1);
        assertEquals(0, meter.getThreshold());
        assertTrue(meter.needsUpdate());
    }

    private static void checkExternalEnergyStorage() throws Exception {
        EnergyStorage storage = new EnergyStorage();
        Fixture fixture = new Fixture(new GT_Cover_EUMeter(), storage);
        CoverEUMeter meter = (CoverEUMeter) fixture.cover;
        for (CoverEUMeter.EnergyType type : new CoverEUMeter.EnergyType[] {
                CoverEUMeter.EnergyType.UNIVERSAL_STORAGE,
                CoverEUMeter.EnergyType.ELECTRICITY_STORAGE_INCLUDING_BATTERIES
        }) {
            fixture.packet(fixture.handlers.findSyncHandler("energy_type", EnumSyncValue.class),
                packet -> NetworkUtils.writeEnumValue(packet, type));
            fixture.invert(false);
            fixture.longValue("threshold", 0);
            fixture.assertSignal(8);
            fixture.longValue("threshold", storage.stored + 1);
            assertEquals(storage.stored + 1, meter.getThreshold());
            fixture.assertSignal(0);
            fixture.invert(true);
            fixture.assertSignal(7);
            fixture.longValue("threshold", storage.stored);
            fixture.assertSignal(0);
            fixture.longValue("threshold", Long.MAX_VALUE);
            assertEquals(storage.capacity, meter.getThreshold());
            checkRoundTrip(fixture);
        }
        Fixture client = new Fixture(new GT_Cover_EUMeter(), new EnergyStorage());
        ((CoverEUMeter) client.cover).setType(meter.getType());
        ((EnergyStorage) client.tile.getMetaTileEntity()).capacity = 0;
        checkCapacitySync(fixture, client, storage.capacity, 5_000_000_000L);
        storage.stored = 0;
        fixture.longValue("threshold", 0);
        fixture.invert(false);
        fixture.assertSignal(0);
        storage.stored = storage.capacity;
        fixture.assertSignal(15);
        storage.capacity = storage.stored = 0;
        fixture.assertSignal(0);
        fixture.invert(true);
        fixture.assertSignal(15);
    }

    private static void checkBatteryCapacitySync() throws Exception {
        ElectricItem.rawManager = new ElectricItemManager();
        ElectricItem.manager = new GatewayElectricItemManager();
        GT_MetaTileEntity_BasicBatteryBuffer buffer = energyBuffer();
        Fixture fixture = new Fixture(new GT_Cover_EUMeter(), buffer);
        CoverEUMeter meter = (CoverEUMeter) fixture.cover;
        meter.setType(CoverEUMeter.EnergyType.ELECTRICITY_STORAGE_INCLUDING_BATTERIES);
        ItemStack battery = new ItemStack(new BatteryItem());
        ElectricItem.manager.charge(battery, 2_500_000_000D, 1, true, false);
        buffer.mInventory[0] = battery;
        fixture.longValue("threshold", 3_000_000_000L);
        assertEquals(3_000_000_000L, meter.getThreshold());
        fixture.assertSignal(0);
        fixture.longValue("threshold", 2_000_000_000L);
        fixture.assertSignal(7);
        Fixture client = new Fixture(new GT_Cover_EUMeter(), energyBuffer());
        ((CoverEUMeter) client.cover).setType(meter.getType());
        checkCapacitySync(fixture, client, 5_000_000_100L, 3_000_000_000L);
        buffer.mInventory[0] = null;
        checkCapacitySync(fixture, client, 100, 100);
    }

    @SuppressWarnings("unchecked")
    private static void checkCapacitySync(Fixture server, Fixture client, long expectedCapacity, long threshold)
            throws Exception {
        LongSyncValue source = server.handlers.findSyncHandler("energy_capacity", LongSyncValue.class);
        LongSyncValue target = client.handlers.findSyncHandler("energy_capacity", LongSyncValue.class);
        assertNotNull(source);
        assertNotNull(target);
        source.updateCacheFromSource(false);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            source.write(packet);
            target.read(packet);
        } finally {
            packet.release();
        }
        assertEquals(expectedCapacity, target.getLongValue());
        Field validatorField = TextFieldWidget.class.getDeclaredField("validator");
        validatorField.setAccessible(true);
        for (IWidget widget : client.widgets) {
            if (widget instanceof TextFieldWidget) {
                Function<String, String> validator = (Function<String, String>) validatorField.get(widget);
                assertEquals(Long.toString(threshold), validator.apply(Long.toString(threshold)));
                assertEquals(Long.toString(expectedCapacity), validator.apply(Long.toString(Long.MAX_VALUE)));
                return;
            }
        }
        throw new AssertionError("Missing energy threshold field");
    }

    private static void checkItemControls() throws Exception {
        GT_MetaTileEntity_Hatch_InputBus bus = itemBus();
        bus.mInventory[0] = new ItemStack(Items.redstone, 32);
        bus.mInventory[1] = new ItemStack(Items.ender_pearl, 8);
        Fixture fixture = new Fixture(new GT_Cover_ItemMeter(), bus);
        CoverItemMeter meter = (CoverItemMeter) fixture.cover;
        assertEquals(5, meter.getTickRate());
        assertEquals(-1, meter.getSlot());
        fixture.assertSignal(4);
        fixture.intValue("slot", 0);
        fixture.assertSignal(8);
        fixture.intValue("threshold", 33);
        fixture.assertSignal(0);
        fixture.intValue("threshold", 32);
        fixture.assertSignal(8);
        fixture.invert(true);
        fixture.assertSignal(0);
        fixture.intValue("threshold", 33);
        fixture.assertSignal(7);
        fixture.intValue("slot", 1);
        assertTrue(meter.isInverted());
        fixture.assertSignal(7);
        GenericSyncValue<?, ?> preview = fixture.handlers.findSyncHandler("slot_preview", GenericSyncValue.class);
        assertFalse(preview.isAllowC2S());
        preview.updateCacheFromSource(false);
        assertSame(Items.ender_pearl, ((ItemStack) preview.getValue()).getItem());
        fixture.intValue("slot", Integer.MAX_VALUE);
        assertEquals(bus.getSizeInventory() - 1, meter.getSlot());
        fixture.intValue("slot", Integer.MIN_VALUE);
        assertEquals(-1, meter.getSlot());
        fixture.intValue("threshold", -1);
        assertEquals(0, meter.getThreshold());
    }

    private static void checkLiquidControls() throws Exception {
        GT_MetaTileEntity_PortableTank tank = portableTank();
        tank.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("meter_water"), 32000);
        Fixture fixture = new Fixture(new GT_Cover_LiquidMeter(), tank);
        CoverLiquidMeter meter = (CoverLiquidMeter) fixture.cover;
        assertEquals(5, meter.getTickRate());
        assertFalse(meter.isInverted());
        assertEquals(64000, meter.getMaxCapacity());
        fixture.assertSignal(8);
        fixture.intValue("threshold", 32001);
        fixture.assertSignal(0);
        fixture.invert(true);
        fixture.assertSignal(7);
        fixture.intValue("threshold", 32000);
        fixture.assertSignal(0);
        fixture.intValue("threshold", Integer.MAX_VALUE);
        assertEquals(64000, meter.getThreshold());
        fixture.intValue("threshold", -1);
        assertEquals(0, meter.getThreshold());
        tank.mFluid = null;
        fixture.assertSignal(15);
        fixture.invert(false);
        fixture.assertSignal(0);
        tank.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("meter_water"), 64000);
        fixture.assertSignal(15);
    }

    private static void checkPersistence() throws Exception {
        Fixture energy = new Fixture(new GT_Cover_EUMeter(), energyBuffer());
        readLegacy(energy.cover, 11, 17);
        CoverEUMeter eu = (CoverEUMeter) energy.cover;
        assertSame(CoverEUMeter.EnergyType.ELECTRICITY_STORAGE_INCLUDING_BATTERIES, eu.getType());
        assertTrue(eu.isInverted());
        assertEquals(0, eu.getThreshold());
        assertEquals(37, eu.getTickRate());
        energy.longValue("threshold", 27);
        checkRoundTrip(energy);

        Fixture items = new Fixture(new GT_Cover_ItemMeter(), itemBus());
        CoverItemMeter item = (CoverItemMeter) items.cover;
        readLegacy(item, 1, 17);
        assertEquals(-1, item.getSlot());
        assertTrue(item.isInverted());
        assertEquals(22, item.getTickRate());
        readLegacy(item, 3, 0);
        assertEquals(0, item.getSlot());
        assertFalse(item.isInverted());
        readLegacy(item, 0xC0000004, 0);
        assertEquals(3, item.getSlot());
        assertTrue(item.isInverted());
        items.intValue("threshold", 321);
        checkRoundTrip(items);

        Fixture liquids = new Fixture(new GT_Cover_LiquidMeter(), portableTank());
        CoverLiquidMeter liquid = (CoverLiquidMeter) liquids.cover;
        readLegacy(liquid, 0, 17);
        assertTrue(liquid.isInverted());
        assertEquals(22, liquid.getTickRate());
        liquids.intValue("threshold", 12345);
        checkRoundTrip(liquids);
    }

    private static void readLegacy(Cover cover, int variable, int tickAddition) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("tra", tickAddition);
        tag.setTag("d", new NBTTagInt(variable));
        cover.readFromNbt(tag);
    }

    private static void checkRoundTrip(Fixture fixture) {
        NBTTagCompound saved = fixture.cover.writeToNBT(new NBTTagCompound());
        assertTrue(saved.getTag("d") instanceof NBTTagCompound);
        Cover restored = CoverRegistry.buildCover(fixture.cover.getCoverID(), ForgeDirection.WEST, fixture.tile);
        restored.readFromNbt(saved);
        assertTrue(saved.equals(restored.writeToNBT(new NBTTagCompound())));
        assertEquals(fixture.cover.getLegacyData(), restored.getLegacyData());
        ByteBuf bytes = Unpooled.buffer();
        try {
            fixture.cover.writeToByteBuf(bytes);
            byte[] packet = new byte[bytes.readableBytes()];
            bytes.readBytes(packet);
            Cover received = CoverRegistry.buildCover(fixture.cover.getCoverID(), ForgeDirection.WEST, fixture.tile);
            received.readFromPacket(ByteStreams.newDataInput(packet));
            assertTrue(saved.equals(received.writeToNBT(new NBTTagCompound())));
        } finally {
            bytes.release();
        }
    }

    private static void checkDigitalChest() throws Exception {
        GT_MetaTileEntity_SuperChest chest = new GT_MetaTileEntity_SuperChest("meter_chest", 1, "", null);
        chest.setItemCount(2000);
        chest.mInventory[0] = new ItemStack(Items.redstone, 4);
        chest.mInventory[1] = new ItemStack(Items.redstone, 8);
        Fixture fixture = new Fixture(new GT_Cover_ItemMeter(), chest);
        fixture.intValue("threshold", 2013);
        fixture.assertSignal(0);
        fixture.intValue("threshold", 2012);
        fixture.assertSignal(1);
        fixture.intValue("slot", 0);
        fixture.assertSignal(1);
    }

    private static void checkLatePackets() throws Exception {
        Fixture[] fixtures = {
            new Fixture(new GT_Cover_EUMeter(), energyBuffer()),
            new Fixture(new GT_Cover_ItemMeter(), itemBus()),
            new Fixture(new GT_Cover_LiquidMeter(), portableTank())
        };
        for (Fixture fixture : fixtures) {
            fixture.tile.detachCover(ForgeDirection.WEST);
            assertFalse(fixture.container.canInteractWith(fixture.player));
            NBTTagCompound before = fixture.cover.writeToNBT(new NBTTagCompound());
            fixture.invert(true);
            if (fixture.cover instanceof CoverEUMeter) {
                fixture.longValue("threshold", 10);
                fixture.packet(fixture.handlers.findSyncHandler("energy_type", EnumSyncValue.class),
                    packet -> NetworkUtils.writeEnumValue(packet, CoverEUMeter.EnergyType.STEAM_STORAGE));
            } else {
                fixture.intValue("threshold", 10);
                if (fixture.cover instanceof CoverItemMeter) {
                    fixture.intValue("slot", 0);
                }
            }
            assertTrue(before.equals(fixture.cover.writeToNBT(new NBTTagCompound())));
        }
    }

    private static GT_MetaTileEntity_BasicBatteryBuffer energyBuffer() {
        return new GT_MetaTileEntity_BasicBatteryBuffer(
            "meter_energy",
            1,
            "",
            null,
            1
        );
    }

    private static GT_MetaTileEntity_Hatch_InputBus itemBus() {
        return new GT_MetaTileEntity_Hatch_InputBus("meter_items", 1, "", null);
    }

    private static GT_MetaTileEntity_PortableTank portableTank() {
        return new GT_MetaTileEntity_PortableTank("meter_fluid", 1, "", null);
    }

    private static final class Fixture {

        private final MeterTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final Cover cover;
        private final PanelSyncManager handlers;
        private final ModularContainer container;
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(GT_CoverBehavior behavior, MetaTileEntity machine) throws Exception {
            GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
            tile = new MeterTile(world, machine);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            GregTech_API.registerCoverBehavior(new ItemStack(Items.stick), behavior);
            tile.setCoverIDAtSide((byte) ForgeDirection.WEST.ordinal(),
                GT_Utility.stackToInt(new ItemStack(Items.stick)));
            cover = tile.getCoverAtSide(ForgeDirection.WEST);
            assertTrue(cover.useMui2());
            assertTrue(cover.isValid());
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            CoverGuiData data = new CoverGuiData(
                player,
                cover.getCoverID(),
                4,
                64,
                7,
                ForgeDirection.WEST,
                false
            );
            assertSame(cover, CoverUIFactory.INSTANCE.getGuiHolder(data));
            assertTrue(CoverUIFactory.INSTANCE.canInteractWith(player, data));
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            ModularPanel panel = CoverUIFactory.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            assertEquals(36, container.inventorySlots.size());
            assertTrue(container.canInteractWith(player));
            collect(panel);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private void invert(boolean inverted) throws Exception {
            for (IWidget widget : widgets) {
                if (widget instanceof ToggleButton) {
                    ToggleButton toggle = (ToggleButton) widget;
                    if (toggle.isSynced() && toggle.getSyncHandler() instanceof BooleanSyncValue) {
                        packet(toggle.getSyncHandler(), buffer -> buffer.writeBoolean(inverted));
                        return;
                    }
                }
            }
            throw new AssertionError("Missing inversion toggle");
        }

        private void intValue(String name, int value) throws Exception {
            packet(handlers.findSyncHandler(name, IntSyncValue.class), buffer -> buffer.writeVarIntToBuffer(value));
        }

        private void longValue(String name, long value) throws Exception {
            packet(handlers.findSyncHandler(name, LongSyncValue.class), buffer -> buffer.writeLong(value));
        }

        private void packet(SyncHandler<?> handler, Consumer<PacketBuffer> writer) throws Exception {
            assertNotNull(handler);
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            try {
                writer.accept(buffer);
                handler.readOnServer(ValueSyncHandler.SYNC_VALUE, buffer);
            } finally {
                buffer.release();
            }
        }

        private void assertSignal(int expected) {
            cover.doCoverThings((byte) 0, 20);
            assertEquals(expected, tile.getOutputRedstoneSignal((byte) ForgeDirection.WEST.ordinal()));
        }
    }

    private static final class EnergyStorage extends GT_MetaTileEntity_BasicBatteryBuffer
            implements IProvideEnergyCover {

        private long capacity = 8_000_000_000_000_000_000L;
        private long stored = 4_000_000_000_000_000_000L;

        private EnergyStorage() {
            super("meter_storage", 1, "", null, 1);
        }

        @Override
        public long getStored() {
            return stored;
        }

        @Override
        public long getMaxCapacity() {
            return capacity;
        }
    }

    private static final class BatteryItem extends Item implements IElectricItem {

        private BatteryItem() {
            setMaxStackSize(1);
        }

        @Override
        public boolean canProvideEnergy(ItemStack stack) {
            return true;
        }

        @Override
        public Item getChargedItem(ItemStack stack) {
            return this;
        }

        @Override
        public Item getEmptyItem(ItemStack stack) {
            return this;
        }

        @Override
        public double getMaxCharge(ItemStack stack) {
            return 5_000_000_000D;
        }

        @Override
        public int getTier(ItemStack stack) {
            return 1;
        }

        @Override
        public double getTransferLimit(ItemStack stack) {
            return 32;
        }
    }

    private static final class MeterTile extends GT_StructureIntegrationTest.TestTile {

        private MeterTile(GT_ProcessingTestSupport.ProcessingWorld world, MetaTileEntity machine) {
            super(world, machine, ForgeDirection.NORTH);
        }

        @Override
        public long getUniversalEnergyStored() {
            return 200;
        }

        @Override
        public long getUniversalEnergyCapacity() {
            return 1000;
        }

        @Override
        public long getStoredEU() {
            return 30;
        }

        @Override
        public long getEUCapacity() {
            return 100;
        }

        @Override
        public long getStoredSteam() {
            return 40;
        }

        @Override
        public long getSteamCapacity() {
            return 80;
        }

        @Override
        public long getAverageElectricInput() {
            return 12;
        }

        @Override
        public long getInputVoltage() {
            return 32;
        }

        @Override
        public long getInputAmperage() {
            return 2;
        }

        @Override
        public long getAverageElectricOutput() {
            return 24;
        }

        @Override
        public long getOutputVoltage() {
            return 128;
        }

        @Override
        public long getOutputAmperage() {
            return 2;
        }
    }
}
