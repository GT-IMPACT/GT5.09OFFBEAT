package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.CoverUIFactory;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.objects.GT_Cover_Default;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverDoesWork;
import gregtech.common.covers.CoverLegacyBehavior;
import gregtech.common.covers.CoverMode;
import gregtech.common.covers.CoverNeedMaintenance;
import gregtech.common.covers.CoverPlayerDetector;
import gregtech.common.covers.CoverRedstoneWirelessBase;
import gregtech.common.covers.CoverShutter;
import gregtech.common.covers.GT_Cover_Blastproof;
import gregtech.common.covers.GT_Cover_Crafting;
import gregtech.common.covers.GT_Cover_DoesWork;
import gregtech.common.covers.GT_Cover_Drain;
import gregtech.common.covers.GT_Cover_EnergyOnly;
import gregtech.common.covers.GT_Cover_Lens;
import gregtech.common.covers.GT_Cover_NeedMaintainance;
import gregtech.common.covers.GT_Cover_PlayerDetector;
import gregtech.common.covers.GT_Cover_RedstoneConductor;
import gregtech.common.covers.GT_Cover_RedstoneReceiverExternal;
import gregtech.common.covers.GT_Cover_RedstoneReceiverInternal;
import gregtech.common.covers.GT_Cover_RedstoneSignalizer;
import gregtech.common.covers.GT_Cover_RedstoneTransmitterExternal;
import gregtech.common.covers.GT_Cover_RedstoneTransmitterInternal;
import gregtech.common.covers.GT_Cover_Screen;
import gregtech.common.covers.GT_Cover_Shutter;
import gregtech.common.covers.GT_Cover_SolarPanel;
import gregtech.common.covers.GT_Cover_Vent;
import gregtech.common.covers.modes.DetectionMode;
import gregtech.common.covers.modes.MaintenanceAlertCondition;
import gregtech.common.covers.modes.PlayerDetectionMode;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.covers.modes.ShutterMode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2LogicCoverTest {

    @Test
    public void passiveAndTickingCoversKeepParametersStateAndPlacementIdentity() throws Throwable {
        run("passive");
    }

    @Test
    public void remainingConfigurableCoversUseNativeModesAndKeepTheirLegacyPermissions() throws Throwable {
        run("remaining");
    }

    @Test
    public void shutterMaintenanceAndPlayerControlsKeepTheirNativeBackingAndNbt() throws Throwable {
        run("logic");
    }

    @Test
    public void activityDetectorAddsEnabledDetectionAndKeepsAllLegacyProgressAndIdleModes() throws Throwable {
        run("activity");
    }

    @Test
    public void wirelessCoversMigrateSignedLegacyChannelsAndUseSeparatePublicAndPrivateSignals() throws Throwable {
        run("wireless");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
            Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
            FMLServerHandler.instance();
            GTGuis.init();
            assertSame(CoverUIFactory.INSTANCE, GuiManager.getFactory("gregtech:cover"));
            switch (scenario) {
                case "passive":
                    checkPassive();
                    break;
                case "remaining":
                    checkRemaining();
                    break;
                case "logic":
                    checkLogic();
                    break;
                case "activity":
                    checkActivity();
                    break;
                case "wireless":
                    checkWireless();
                    break;
                default:
                    throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkPassive() throws Exception {
        GT_CoverBehavior[] behaviors = {
            new GT_Cover_Blastproof(42.0F),
            new GT_Cover_Crafting(),
            new GT_Cover_Lens((byte) 7),
            new GT_Cover_Screen(),
            new GT_Cover_SolarPanel(8192),
            new GT_Cover_Vent(3)
        };
        Item[] items = {Items.apple, Items.stick, Items.paper, Items.coal, Items.quartz, Items.diamond};
        for (int index = 0; index < behaviors.length; index++) {
            Fixture fixture = new Fixture(items[index], behaviors[index], false);
            assertFalse(fixture.cover instanceof CoverLegacyBehavior);
            Cover cover = fixture.cover;
            GT_CoverBehavior legacy = behaviors[index];
            byte side = (byte) ForgeDirection.NORTH.ordinal();
            assertEquals(legacy.getBlastProofLevel(side, 1, 0, fixture.tile), cover.getBlastProofLevel(), 0.0F);
            assertEquals(legacy.getLensColor(side, 1, 0, fixture.tile), cover.getLensColor());
            assertEquals(legacy.alwaysLookConnected(side, 1, 0, fixture.tile), cover.alwaysLookConnected());
            assertEquals(legacy.getTickRate(side, 1, 0, fixture.tile), cover.getMinimumTickRate());
            for (int value : new int[]{0, 1, 99, Integer.MIN_VALUE}) {
                cover.setLegacyData(value);
                cover.doCoverThings((byte) 0, 101);
                assertEquals(legacy.doCoverThings(side, (byte) 0, 1, value, fixture.tile, 101), cover.getLegacyData());
            }
            fixture.checkNbt();
        }
        assertFalse(CoverRegistry.isNativeCover(new ItemStack(Items.flint)));
    }

    private static void checkRemaining() throws Exception {
        GT_CoverBehavior[] behaviors = {
            new GT_Cover_Drain(),
            new GT_Cover_EnergyOnly(),
            new GT_Cover_RedstoneConductor(),
            new GT_Cover_RedstoneSignalizer(),
            new GT_Cover_Default()
        };
        Item[] items = {Items.apple, Items.stick, Items.paper, Items.coal, Items.quartz};
        for (int index = 0; index < behaviors.length; index++) {
            Fixture fixture = new Fixture(items[index], behaviors[index]);
            assertFalse(fixture.cover instanceof CoverLegacyBehavior);
            CoverMode cover = (CoverMode) fixture.cover;
            for (int mode = 0; mode < cover.getModeCount(); mode++) {
                fixture.sendInt("mode", mode);
                assertEquals(mode, cover.getLegacyData());
                for (boolean enabled : new boolean[]{false, true}) {
                    if (enabled) {
                        fixture.tile.enableWorking();
                    } else {
                        fixture.tile.disableWorking();
                    }
                    byte side = (byte) ForgeDirection.NORTH.ordinal();
                    GT_CoverBehavior legacy = behaviors[index];
                    assertEquals(legacy.letsRedstoneGoIn(side, 1, mode, fixture.tile), cover.letsRedstoneGoIn());
                    assertEquals(legacy.letsRedstoneGoOut(side, 1, mode, fixture.tile), cover.letsRedstoneGoOut());
                    assertEquals(legacy.letsEnergyIn(side, 1, mode, fixture.tile), cover.letsEnergyIn());
                    assertEquals(legacy.letsEnergyOut(side, 1, mode, fixture.tile), cover.letsEnergyOut());
                    assertEquals(legacy.letsItemsIn(side, 1, mode, 0, fixture.tile), cover.letsItemsIn(0));
                    assertEquals(legacy.letsItemsOut(side, 1, mode, 0, fixture.tile), cover.letsItemsOut(0));
                    assertEquals(legacy.letsFluidIn(side, 1, mode, null, fixture.tile), cover.letsFluidIn(null));
                    assertEquals(legacy.letsFluidOut(side, 1, mode, null, fixture.tile), cover.letsFluidOut(null));
                    assertEquals(legacy.getTickRate(side, 1, mode, fixture.tile), cover.getMinimumTickRate());
                    assertEquals(legacy.getRedstoneInput(side, (byte) 9, 1, mode, fixture.tile),
                        cover.getRedstoneInput((byte) 9));
                }
            }
            fixture.checkNbt();
            int setting = cover.getLegacyData();
            fixture.tile.detachCover(ForgeDirection.NORTH);
            fixture.sendInt("mode", 0);
            assertEquals(setting, cover.getLegacyData());
        }
    }

    private static void checkLogic() throws Exception {
        Fixture shutterFixture = new Fixture(Items.apple, new GT_Cover_Shutter());
        CoverShutter shutter = (CoverShutter) shutterFixture.cover;
        for (ShutterMode mode : ShutterMode.values()) {
            shutterFixture.select("mode", mode);
            assertEquals(mode.ordinal(), shutter.getLegacyData());
            assertSame(mode, shutter.getShutterMode());
        }
        shutterFixture.select("mode", ShutterMode.OPEN_IF_ENABLED);
        shutterFixture.tile.enableWorking();
        assertTrue(shutter.letsItemsIn(0));
        assertTrue(shutter.letsEnergyOut());
        shutterFixture.tile.disableWorking();
        assertFalse(shutter.letsItemsIn(0));
        shutterFixture.select("mode", ShutterMode.ONLY_OUTPUT);
        assertFalse(shutter.letsItemsIn(0));
        assertTrue(shutter.letsItemsOut(0));
        shutterFixture.checkNbt();
        shutterFixture.tile.detachCover(ForgeDirection.NORTH);
        shutterFixture.select("mode", ShutterMode.ONLY_INPUT);
        assertEquals(ShutterMode.ONLY_OUTPUT.ordinal(), shutter.getLegacyData());
        assertFalse(shutterFixture.container.canInteractWith(shutterFixture.player));

        Fixture maintenanceFixture = new Fixture(Items.stick, new GT_Cover_NeedMaintainance());
        CoverNeedMaintenance maintenance = (CoverNeedMaintenance) maintenanceFixture.cover;
        maintenanceFixture.select("redstone_mode", RedstoneMode.INVERTED);
        for (MaintenanceAlertCondition condition : MaintenanceAlertCondition.values()) {
            maintenanceFixture.select("threshold", condition);
            assertSame(condition, maintenance.getMaintenanceAlertCondition());
            assertEquals((condition.ordinal() << 1) | 1, maintenance.getLegacyData());
        }
        maintenanceFixture.select("redstone_mode", RedstoneMode.NORMAL);
        assertEquals(12, maintenance.getLegacyData());
        maintenanceFixture.checkNbt();

        Fixture playerFixture = new Fixture(Items.paper, new GT_Cover_PlayerDetector());
        CoverPlayerDetector detector = (CoverPlayerDetector) playerFixture.cover;
        playerFixture.select("mode", PlayerDetectionMode.OTHER_PLAYERS);
        assertEquals(2, detector.getLegacyData());
        playerFixture.select("mode", PlayerDetectionMode.ONLY_OWNER);
        assertEquals(1, detector.getLegacyData());
        detector.setLegacyData(2);
        assertSame(PlayerDetectionMode.OTHER_PLAYERS, detector.getPlayerDetectionMode());
        playerFixture.checkNbt();
        assertTrue(playerFixture.tile.dirtyCalls > 0);
        assertTrue(playerFixture.world.getChunkFromBlockCoords(4, 7).isModified);
    }

    private static void checkActivity() throws Exception {
        Fixture fixture = new Fixture(Items.redstone, new GT_Cover_DoesWork());
        CoverDoesWork detector = (CoverDoesWork) fixture.cover;
        fixture.tile.maxProgress = 150;
        fixture.tile.progress = 60;
        fixture.tile.hasRecipe = true;
        for (int legacy = 0; legacy < 4; legacy++) {
            detector.setLegacyData(legacy);
            detector.doCoverThings((byte) 0, 5);
            assertEquals(legacy, detector.getLegacyData());
            int expected = legacy == 0 ? 6 : legacy == 1 ? 9 : legacy == 2 ? 0 : 15;
            assertEquals(expected, fixture.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
        }
        fixture.select("detection_mode", DetectionMode.MACHINE_ENABLED);
        fixture.select("redstone_mode", RedstoneMode.NORMAL);
        assertEquals(4, detector.getLegacyData());
        fixture.tile.enableWorking();
        detector.doCoverThings((byte) 0, 10);
        assertEquals(15, fixture.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
        fixture.tile.disableWorking();
        detector.doCoverThings((byte) 0, 15);
        assertEquals(0, fixture.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
        fixture.select("redstone_mode", RedstoneMode.INVERTED);
        detector.doCoverThings((byte) 0, 20);
        assertEquals(5, detector.getLegacyData());
        assertEquals(15, fixture.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
        fixture.checkNbt();
        detector.onCoverScrewdriverClick(fixture.player, 0.5F, 0.5F, 0.5F);
        assertEquals(0, detector.getLegacyData());
        fixture.select("detection_mode", DetectionMode.MACHINE_IDLE);
        fixture.tile.maxProgress = 0;
        detector.doCoverThings((byte) 0, 25);
        assertEquals(15, fixture.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
    }

    private static void checkWireless() throws Exception {
        Fixture externalTransmitter = new Fixture(Items.coal, new GT_Cover_RedstoneTransmitterExternal());
        Fixture internalTransmitter = new Fixture(Items.diamond, new GT_Cover_RedstoneTransmitterInternal());
        Fixture externalReceiver = new Fixture(Items.emerald, new GT_Cover_RedstoneReceiverExternal());
        Fixture internalReceiver = new Fixture(Items.quartz, new GT_Cover_RedstoneReceiverInternal());
        Fixture[] fixtures = {externalTransmitter, internalTransmitter, externalReceiver, internalReceiver};
        for (Fixture fixture : fixtures) {
            CoverRedstoneWirelessBase cover = (CoverRedstoneWirelessBase) fixture.cover;
            // GTNH's legacy migration: low 16 bits become frequency, bit 16 becomes privacy.
            // All other bits of old signed channels are deliberately discarded.
            for (int legacy : new int[]{0, -1, Integer.MIN_VALUE, 0x1234ABCD}) {
                NBTTagCompound old = new NBTTagCompound();
                old.setTag("d", new NBTTagInt(legacy));
                cover.readFromNbt(old);
                assertEquals(legacy & 0xFFFF, cover.getFrequency());
                assertEquals((legacy & 0x10000) != 0, cover.isPrivateChannel());
            }
            CoverTile migratedHost = new CoverTile(fixture.world);
            migratedHost.loadLegacy(cover.getCoverID(), -1);
            CoverRedstoneWirelessBase migrated =
                (CoverRedstoneWirelessBase) migratedHost.getCoverAtSide(ForgeDirection.NORTH);
            assertEquals(65535, migrated.getFrequency());
            assertTrue(migrated.isPrivateChannel());
            fixture.sendInt("frequency", 65536);
            assertEquals(65535, cover.getFrequency());
            fixture.sendInt("frequency", -42);
            assertEquals(0, cover.getFrequency());
            fixture.sendInt("frequency", 42);
            fixture.sendBoolean("private_channel", true);
            assertEquals(65536 + 42, cover.getLegacyData());
            fixture.checkNbt();
            checkWirelessPacket(cover, fixture);
        }
        externalTransmitter.sendBoolean("private_channel", false);
        externalReceiver.sendBoolean("private_channel", false);
        externalTransmitter.cover.doCoverThings((byte) 11, 1);
        internalTransmitter.tile.setOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal(), (byte) 7);
        internalTransmitter.cover.doCoverThings((byte) 0, 1);
        assertEquals(Byte.valueOf((byte) 11), GregTech_API.sWirelessRedstone.get(42));
        assertEquals(Byte.valueOf((byte) 7), GregTech_API.sWirelessRedstone.get(65536 + 42));
        externalReceiver.cover.doCoverThings((byte) 0, 1);
        assertEquals(11, externalReceiver.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
        externalReceiver.sendBoolean("private_channel", true);
        internalTransmitter.cover.doCoverThings((byte) 0, 2);
        externalReceiver.cover.doCoverThings((byte) 0, 2);
        assertEquals(7, externalReceiver.tile.getOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal()));
        internalReceiver.cover.doCoverThings((byte) 9, 2);
        assertTrue(internalReceiver.tile.isAllowedToWork());
        assertEquals(9, internalReceiver.tile.getWorkDataValue());
        assertEquals(7, internalReceiver.cover.getRedstoneInput((byte) 0));
        internalTransmitter.tile.setOutputRedstoneSignal((byte) ForgeDirection.NORTH.ordinal(), (byte) 0);
        internalTransmitter.cover.doCoverThings((byte) 0, 3);
        internalReceiver.cover.doCoverThings((byte) 4, 3);
        assertFalse(internalReceiver.tile.isAllowedToWork());
        assertEquals(4, internalReceiver.tile.getWorkDataValue());
        externalTransmitter.sendInt("frequency", 43);
        assertFalse(GregTech_API.sWirelessRedstone.containsKey(42));
        externalTransmitter.cover.doCoverThings((byte) 15, 4);
        assertEquals(Byte.valueOf((byte) 15), GregTech_API.sWirelessRedstone.get(43));
        externalTransmitter.tile.detachCover(ForgeDirection.NORTH);
        assertFalse(GregTech_API.sWirelessRedstone.containsKey(43));
        externalTransmitter.sendInt("frequency", 44);
        assertEquals(43, ((CoverRedstoneWirelessBase) externalTransmitter.cover).getFrequency());
    }

    private static void checkWirelessPacket(CoverRedstoneWirelessBase cover, Fixture fixture) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            cover.writeToByteBuf(buffer);
            assertEquals(9, buffer.readableBytes());
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            CoverTile host = new CoverTile(fixture.world);
            CoverRedstoneWirelessBase copy = (CoverRedstoneWirelessBase) CoverRegistry.buildCover(
                cover.getCoverID(),
                ForgeDirection.NORTH,
                host
            );
            copy.readFromPacket(ByteStreams.newDataInput(bytes));
            assertEquals(cover.getFrequency(), copy.getFrequency());
            assertEquals(cover.isPrivateChannel(), copy.isPrivateChannel());
            assertEquals(cover.getTickRateAddition(), copy.getTickRateAddition());
        } finally {
            buffer.release();
        }
    }

    private static final class Fixture {

        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final CoverTile tile = new CoverTile(world);
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final Cover cover;
        private final PanelSyncManager handlers;
        private final ModularContainer container;

        private Fixture(Item item, GT_CoverBehavior behavior) throws Exception {
            this(item, behavior, true);
        }

        private Fixture(Item item, GT_CoverBehavior behavior, boolean gui) throws Exception {
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            ItemStack coverItem = new ItemStack(item);
            GregTech_API.registerCoverBehavior(coverItem, behavior);
            int id = GT_Utility.stackToInt(coverItem);
            tile.setCoverIDAtSide((byte) ForgeDirection.NORTH.ordinal(), id);
            cover = tile.getCoverAtSide(ForgeDirection.NORTH);
            assertTrue(cover.useMui2());
            if (!gui) {
                handlers = null;
                container = null;
                return;
            }
            assertTrue(cover.hasCoverGUI());
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            CoverGuiData data = new CoverGuiData(
                player,
                id,
                4,
                64,
                7,
                ForgeDirection.NORTH,
                false
            );
            assertSame(cover, CoverUIFactory.INSTANCE.getGuiHolder(data));
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
            assertTrue(container.isInitialized());
            assertTrue(container.canInteractWith(player));
            assertEquals(36, container.inventorySlots.size());
            tile.dirtyCalls = 0;
        }

        private void select(String key, Enum<?> value) throws Exception {
            EnumSyncValue<?, ?> handler = handlers.findSyncHandler(key, EnumSyncValue.class);
            assertTrue(handler.isAllowC2S());
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                NetworkUtils.writeEnumValue(packet, value);
                handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
        }

        private void sendInt(String key, int value) throws Exception {
            IntSyncValue handler = handlers.findSyncHandler(key, IntSyncValue.class);
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeVarIntToBuffer(value);
                handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
        }

        private void sendBoolean(String key, boolean value) throws Exception {
            BooleanSyncValue handler = handlers.findSyncHandler(key, BooleanSyncValue.class);
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeBoolean(value);
                handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
        }

        private void checkNbt() {
            cover.setTickRateAddition(17);
            NBTTagCompound saved = tile.save();
            CoverTile restored = new CoverTile(world);
            restored.load(saved);
            Cover copy = restored.getCoverAtSide(ForgeDirection.NORTH);
            assertNotSame(cover, copy);
            assertSame(cover.getClass(), copy.getClass());
            assertSame(restored, copy.getTile());
            assertEquals(cover.getLegacyData(), copy.getLegacyData());
            assertEquals(17, copy.getTickRateAddition());
            if (cover instanceof CoverRedstoneWirelessBase) {
                NBTTagCompound state = copy.writeToNBT(new NBTTagCompound());
                assertTrue(state.getTag("d") instanceof NBTTagCompound);
                NBTTagCompound wireless = state.getCompoundTag("d");
                assertEquals(((CoverRedstoneWirelessBase) cover).getFrequency(), wireless.getInteger("frequency"));
                assertTrue(wireless.getBoolean("privateChannel"));
            } else {
                assertTrue(copy.writeToNBT(new NBTTagCompound()).getTag("d") instanceof NBTTagInt);
            }
        }
    }

    private static final class CoverTile extends GT_StructureIntegrationTest.TestTile {

        private int dirtyCalls;
        private int progress;
        private int maxProgress;
        private boolean hasRecipe;

        private CoverTile(GT_ProcessingTestSupport.ProcessingWorld world) {
            super(world, new GT_MetaTileEntity_Hatch_InputBus("logic_cover_test", 1, "", null), ForgeDirection.NORTH);
        }

        @Override
        public void markDirty() {
            dirtyCalls++;
        }

        @Override
        public int getProgress() {
            return progress;
        }

        @Override
        public int getMaxProgress() {
            return maxProgress;
        }

        @Override
        public boolean hasThingsToDo() {
            return hasRecipe;
        }

        private NBTTagCompound save() {
            NBTTagCompound saved = new NBTTagCompound();
            writeCoverNBT(saved, false);
            return saved;
        }

        private void load(NBTTagCompound saved) {
            readCoverNBT(saved, new int[6], new int[6]);
        }

        private void loadLegacy(int id, int value) {
            int[] ids = new int[6];
            int[] values = new int[6];
            ids[ForgeDirection.NORTH.ordinal()] = id;
            values[ForgeDirection.NORTH.ordinal()] = value;
            readCoverNBT(new NBTTagCompound(), ids, values);
        }
    }
}
