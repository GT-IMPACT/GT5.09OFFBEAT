package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncHandler;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IFMLSidedHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.CoverUIFactory;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverIOBase;
import gregtech.common.covers.GT_Cover_Arm;
import gregtech.common.covers.GT_Cover_Conveyor;
import gregtech.common.covers.GT_Cover_ControlsWork;
import gregtech.common.covers.GT_Cover_DoesWork;
import gregtech.common.covers.GT_Cover_EUMeter;
import gregtech.common.covers.GT_Cover_FluidRegulator;
import gregtech.common.covers.GT_Cover_Fluidfilter;
import gregtech.common.covers.GT_Cover_ItemMeter;
import gregtech.common.covers.GT_Cover_LiquidMeter;
import gregtech.common.covers.GT_Cover_NeedMaintainance;
import gregtech.common.covers.GT_Cover_PlayerDetector;
import gregtech.common.covers.GT_Cover_Pump;
import gregtech.common.covers.GT_Cover_RedstoneReceiverExternal;
import gregtech.common.covers.GT_Cover_RedstoneReceiverInternal;
import gregtech.common.covers.GT_Cover_RedstoneTransmitterExternal;
import gregtech.common.covers.GT_Cover_RedstoneTransmitterInternal;
import gregtech.common.covers.GT_Cover_Shutter;
import gregtech.common.covers.NativeCoverBehavior;
import gregtech.common.covers.modes.BlockMode;
import gregtech.common.covers.modes.MachineProcessingCondition;
import gregtech.common.covers.modes.TransferMode;
import gregtech.common.modularui2.widget.CoverTickRateButton;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_PortableTank;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2CoverRoutingTest {

    @Test
    public void everyConcreteCoverUsesNativeStandaloneAndPopupRouting() throws Throwable {
        run("routing");
    }

    @Test
    public void everyConcreteCoverOpensAndReopensFromMultiblockTabs() throws Throwable {
        run("multiblock_routing");
    }

    @Test
    public void conveyorAndPumpPacketsReachEveryLegacyModeFromEveryInitialMode() throws Throwable {
        run("transfer_modes");
    }

    @Test
    public void transferModePacketsCannotMutateDetachedCoverInstances() throws Throwable {
        run("late_packets");
    }

    @Test
    public void transferCoversKeepGtnhCompactLayoutAndTickButtonInsets() throws Throwable {
        run("tick_button_layout");
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
            ItemList.Display_ITS_FREE.set(new ItemStack(Items.paper));
            if ("routing".equals(scenario)) {
                checkAllRoutes(false);
            } else if ("multiblock_routing".equals(scenario)) {
                checkAllRoutes(true);
            } else if ("transfer_modes".equals(scenario)) {
                checkTransferModes(new GT_Cover_Conveyor(20), 0);
                checkTransferModes(new GT_Cover_Pump(1000), 1);
            } else if ("late_packets".equals(scenario)) {
                checkLatePackets(new GT_Cover_Conveyor(20), 0);
                checkLatePackets(new GT_Cover_Pump(1000), 1);
            } else if ("tick_button_layout".equals(scenario)) {
                FMLCommonHandler commonHandler = FMLCommonHandler.instance();
                Field delegateField = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
                delegateField.setAccessible(true);
                IFMLSidedHandler previousDelegate = (IFMLSidedHandler) delegateField.get(commonHandler);
                try (GT_Mui2FluidHatchGuiTest.HeadlessClient ignored = new GT_Mui2FluidHatchGuiTest.HeadlessClient()) {
                    // Integrated servers build this UI on physical CLIENT too; use actual font widths, not 100.
                    delegateField.set(commonHandler, Proxy.newProxyInstance(
                        IFMLSidedHandler.class.getClassLoader(),
                        new Class<?>[]{IFMLSidedHandler.class},
                        (proxy, method, args) -> "getSide".equals(method.getName())
                            ? Side.CLIENT : method.invoke(previousDelegate, args)
                    ));
                    checkTickButtonLayout(new GT_Cover_Pump(1000), 0);
                    checkTickButtonLayout(new GT_Cover_Conveyor(20), 1);
                } finally {
                    delegateField.set(commonHandler, previousDelegate);
                }
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkTickButtonLayout(GT_CoverBehavior behavior, int metadata) throws Exception {
        Fixture fixture = new Fixture(behavior, metadata);
        for (Panel ui : new Panel[]{fixture.standalone, fixture.popup()}) {
            GT_Mui2InputBusTest.prepareLayout(ui.panel, ui.sync);
            GT_Mui2FluidHatchGuiTest.freezeTranslationCache(ui.panel);
            initializeResizers(ui.panel, new LayoutViewport());
            WidgetTree.resizeInternal(ui.panel.resizer(), false);
            CoverTickRateButton button = findTickButton(ui.panel);
            assertNotNull(button);
            Area bounds = button.getArea();
            Area panel = ui.panel.getArea();
            assertTrue(bounds.x >= panel.x && bounds.ex() <= panel.ex());
            assertTrue(bounds.y >= panel.y && bounds.ey() <= panel.ey());
            assertEquals(0, playerInventoryWidgetCount(ui.panel));
            assertEquals(4, panel.ex() - bounds.ex());
            assertEquals(4, panel.ey() - bounds.ey());
            assertEquals(18, bounds.width);
            assertEquals(18, bounds.height);
            checkTickButtonClearance(ui.panel, button);
        }
    }

    private static CoverTickRateButton findTickButton(IWidget widget) {
        if (widget instanceof CoverTickRateButton) {
            return (CoverTickRateButton) widget;
        }
        for (IWidget child : widget.getChildren()) {
            CoverTickRateButton button = findTickButton(child);
            if (button != null) {
                return button;
            }
        }
        return null;
    }

    private static void checkTickButtonClearance(IWidget widget, CoverTickRateButton button) {
        if (widget == button) {
            return;
        }
        if (widget.getChildren().isEmpty()) {
            assertFalse("Tick button overlaps " + widget, button.getArea().intersects(widget.getArea()));
        }
        for (IWidget child : widget.getChildren()) {
            checkTickButtonClearance(child, button);
        }
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        widget.resizer().initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, widget.resizer());
        }
    }

    private static void checkAllRoutes(boolean multiblock) throws Exception {
        CoverCase[] cases = {
            new CoverCase(new GT_Cover_Conveyor(20), "cover.conveyor"),
            new CoverCase(new GT_Cover_Pump(1000), "cover.pump"),
            new CoverCase(new GT_Cover_Arm(20), "cover.arm"),
            new CoverCase(new GT_Cover_Fluidfilter(), "cover.fluid_filter"),
            new CoverCase(new GT_Cover_FluidRegulator(1000), "cover.fluid_regulator"),
            new CoverCase(new GT_Cover_EUMeter(), "cover.eu_meter"),
            new CoverCase(new GT_Cover_ItemMeter(), "cover.item_meter"),
            new CoverCase(new GT_Cover_LiquidMeter(), "cover.liquid_meter"),
            new CoverCase(new GT_Cover_ControlsWork(), "cover.machine_controller"),
            new CoverCase(new GT_Cover_Shutter(), "cover.shutter"),
            new CoverCase(new GT_Cover_DoesWork(), "cover.activity_detector"),
            new CoverCase(new GT_Cover_NeedMaintainance(), "cover.maintenance"),
            new CoverCase(new GT_Cover_PlayerDetector(), "cover.player_detector"),
            new CoverCase(new GT_Cover_RedstoneTransmitterExternal(), "cover.redstone_wireless"),
            new CoverCase(new GT_Cover_RedstoneTransmitterInternal(), "cover.redstone_wireless"),
            new CoverCase(new GT_Cover_RedstoneReceiverExternal(), "cover.redstone_wireless"),
            new CoverCase(new GT_Cover_RedstoneReceiverInternal(), "cover.redstone_wireless")
        };
        assertEquals(17, cases.length);
        for (int index = 0; index < cases.length; index++) {
            CoverCase coverCase = cases[index];
            String name = coverCase.behavior.getClass().getSimpleName();
            Fixture fixture = new Fixture(coverCase.behavior, index, multiblock);
            assertTrue(name, fixture.cover.hasCoverGUI());
            assertTrue(name, fixture.cover.useMui2());
            assertTrue(name, CoverRegistry.isNativeCover(fixture.item));
            GT_CoverBehavior compatibility = fixture.tile.getCoverBehaviorAtSide((byte) ForgeDirection.WEST.ordinal());
            assertTrue(name, compatibility instanceof NativeCoverBehavior);
            assertNotSame(coverCase.behavior, compatibility);
            assertTrue(compatibility.hasCoverGUI());
            assertTrue(name, (coverCase.guiId + ".WEST").equals(fixture.standalone.panel.getName()));
            assertEquals(name, 36, fixture.standalone.container.inventorySlots.size());
            assertEquals(name, 0, playerInventoryWidgetCount(fixture.standalone.panel));

            Panel popup = fixture.popup();
            assertTrue(name, (coverCase.guiId + ".WEST").equals(popup.panel.getName()));
            assertEquals(name, 36, popup.container.inventorySlots.size());
            assertEquals(name, 0, playerInventoryWidgetCount(popup.panel));
            checkMachineTab(fixture, coverCase.guiId);

            fixture.player.setPosition(20.5, 64.5, 7.5);
            assertFalse(name, fixture.standalone.container.canInteractWith(fixture.player));
            fixture.player.setPosition(4.5, 64.5, 7.5);
            assertTrue(name, fixture.standalone.container.canInteractWith(fixture.player));
            Cover replacement = CoverRegistry.buildCover(fixture.item, ForgeDirection.WEST, fixture.tile);
            fixture.tile.attachCover(replacement);
            assertNotSame(fixture.cover, replacement);
            assertEquals(fixture.cover.getCoverID(), replacement.getCoverID());
            assertTrue(name, CoverUIFactory.INSTANCE.canInteractWith(fixture.player, fixture.data(false)));
            assertFalse(name, fixture.standalone.container.canInteractWith(fixture.player));
            assertTrue(name, fixture.tile.getCoverBehaviorAtSide((byte) ForgeDirection.WEST.ordinal())
                instanceof NativeCoverBehavior);
        }
    }

    private static void checkMachineTab(Fixture fixture, String guiId) {
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager handlers = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        PosGuiData data = new PosGuiData(fixture.player, 4, 64, 7);
        ModularPanel machinePanel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
        WidgetTree.collectSyncValues(handlers, machinePanel);
        ModularContainer container = new ModularContainer();
        container.construct(
            fixture.player,
            sync,
            settings,
            machinePanel.getName(),
            data
        );
        PanelSyncHandler tab = (PanelSyncHandler) handlers.findPanelHandlerNullable("cover_panel_4");
        assertNotNull(tab);
        PanelSyncManager popupHandlers = new PanelSyncManager(sync, false);
        ModularPanel popup = tab.createUI(popupHandlers);
        assertEquals(guiId + ".WEST", popup.getName());
        assertEquals(0, playerInventoryWidgetCount(popup));
        tab.openPanel();
        assertTrue(tab.isPanelOpen());
        tab.closePanelInternal();
        assertFalse(tab.isPanelOpen());
        tab.openPanel();
        assertTrue(tab.isPanelOpen());
    }

    private static int playerInventoryWidgetCount(IWidget widget) {
        int count = 0;
        for (IWidget child : widget.getChildren()) {
            if (widget.isName("player_inventory") && child instanceof ItemSlot) {
                count++;
            } else {
                count += playerInventoryWidgetCount(child);
            }
        }
        return count;
    }

    private static void checkTransferModes(GT_CoverBehavior behavior, int metadata) throws Exception {
        Fixture fixture = new Fixture(behavior, metadata);
        CoverIOBase cover = (CoverIOBase) fixture.cover;
        EnumSyncValue<?, ?> direction = fixture.standalone.handlers.findSyncHandler("io_mode", EnumSyncValue.class);
        EnumSyncValue<?, ?> condition = fixture.standalone.handlers.findSyncHandler(
            "condition_mode",
            EnumSyncValue.class
        );
        EnumSyncValue<?, ?> blocking = fixture.standalone.handlers.findSyncHandler("block_mode", EnumSyncValue.class);
        assertTrue(direction.isAllowC2S());
        assertTrue(condition.isAllowC2S());
        assertTrue(blocking.isAllowC2S());
        for (int initial = 0; initial < 12; initial++) {
            for (int target = 0; target < 12; target++) {
                cover.setLegacyData(initial);
                checkDecodedMode(cover, initial);
                TransferMode targetDirection = TransferMode.values()[target % 2];
                MachineProcessingCondition targetCondition = MachineProcessingCondition.values()[target % 6 / 2];
                BlockMode targetBlocking = target < 6 ? BlockMode.BLOCK : BlockMode.ALLOW;
                if ((initial + target) % 2 == 0) {
                    enumPacket(direction, targetDirection);
                    enumPacket(condition, targetCondition);
                    enumPacket(blocking, targetBlocking);
                } else {
                    enumPacket(blocking, targetBlocking);
                    enumPacket(condition, targetCondition);
                    enumPacket(direction, targetDirection);
                }
                String route = behavior.getClass().getSimpleName() + " " + initial + " -> " + target;
                assertEquals(route, target, cover.getLegacyData());
                checkDecodedMode(cover, target);
                checkLegacyPermissions(fixture, behavior, target);
                if (initial == 0) {
                    checkTransferPersistence(fixture, target);
                }
            }
        }
    }

    private static void checkDecodedMode(CoverIOBase cover, int mode) {
        assertSame(mode % 2 == 0 ? TransferMode.EXPORT : TransferMode.IMPORT, cover.getIOMode());
        assertSame(MachineProcessingCondition.values()[mode % 6 / 2], cover.getMachineProcessingCondition());
        assertSame(mode < 6 ? BlockMode.BLOCK : BlockMode.ALLOW, cover.getBlockMode());
    }

    private static void checkLegacyPermissions(Fixture fixture, GT_CoverBehavior behavior, int mode) {
        byte side = (byte) ForgeDirection.WEST.ordinal();
        int id = fixture.cover.getCoverID();
        assertEquals(behavior.letsItemsIn(
            side,
            id,
            mode,
            0,
            fixture.tile
        ), fixture.cover.letsItemsIn(0));
        assertEquals(behavior.letsItemsOut(
            side,
            id,
            mode,
            0,
            fixture.tile
        ), fixture.cover.letsItemsOut(0));
        assertEquals(behavior.letsFluidIn(
            side,
            id,
            mode,
            null,
            fixture.tile
        ), fixture.cover.letsFluidIn(null));
        assertEquals(behavior.letsFluidOut(
            side,
            id,
            mode,
            null,
            fixture.tile
        ), fixture.cover.letsFluidOut(null));
        assertEquals(behavior.getTickRate(side, id, mode, fixture.tile), fixture.cover.getMinimumTickRate());
    }

    private static void checkTransferPersistence(Fixture fixture, int mode) {
        NBTTagCompound saved = fixture.cover.writeToNBT(new NBTTagCompound());
        assertEquals(mode, ((NBTTagInt) saved.getTag("d")).func_150287_d());
        Cover restored = CoverRegistry.buildCover(fixture.item, ForgeDirection.WEST, fixture.tile);
        restored.readFromNbt(saved);
        assertEquals(mode, restored.getLegacyData());
        assertTrue(saved.equals(restored.writeToNBT(new NBTTagCompound())));
        ByteBuf buffer = Unpooled.buffer();
        try {
            fixture.cover.writeToByteBuf(buffer);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            Cover received = CoverRegistry.buildCover(fixture.item, ForgeDirection.WEST, fixture.tile);
            received.readFromPacket(ByteStreams.newDataInput(bytes));
            assertEquals(mode, received.getLegacyData());
            assertTrue(saved.equals(received.writeToNBT(new NBTTagCompound())));
        } finally {
            buffer.release();
        }
    }

    private static void checkLatePackets(GT_CoverBehavior behavior, int metadata) throws Exception {
        Fixture fixture = new Fixture(behavior, metadata);
        fixture.cover.setLegacyData(7);
        NBTTagCompound saved = fixture.cover.writeToNBT(new NBTTagCompound());
        fixture.tile.detachCover(ForgeDirection.WEST);
        assertFalse(fixture.cover.isValid());
        assertFalse(fixture.standalone.container.canInteractWith(fixture.player));
        enumPacket(fixture.standalone.handlers.findSyncHandler("io_mode", EnumSyncValue.class), TransferMode.EXPORT);
        enumPacket(fixture.standalone.handlers.findSyncHandler("condition_mode", EnumSyncValue.class),
            MachineProcessingCondition.ALWAYS);
        enumPacket(fixture.standalone.handlers.findSyncHandler("block_mode", EnumSyncValue.class), BlockMode.BLOCK);
        assertTrue(saved.equals(fixture.cover.writeToNBT(new NBTTagCompound())));
        Cover replacement = CoverRegistry.buildCover(fixture.item, ForgeDirection.WEST, fixture.tile);
        fixture.tile.attachCover(replacement);
        enumPacket(fixture.standalone.handlers.findSyncHandler("io_mode", EnumSyncValue.class), TransferMode.IMPORT);
        assertEquals(0, replacement.getLegacyData());
        assertEquals(7, fixture.cover.getLegacyData());
    }

    private static <E extends Enum<E>> void enumPacket(EnumSyncValue<?, ?> handler, E value) throws Exception {
        assertNotNull(handler);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            NetworkUtils.writeEnumValue(buffer, value);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, buffer);
        } finally {
            buffer.release();
        }
    }

    private static final class CoverCase {

        private final GT_CoverBehavior behavior;
        private final String guiId;

        private CoverCase(GT_CoverBehavior behavior, String guiId) {
            this.behavior = behavior;
            this.guiId = guiId;
        }
    }

    private static final class Fixture {

        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final ItemStack item;
        private final Cover cover;
        private final Panel standalone;

        private Fixture(GT_CoverBehavior behavior, int metadata) throws Exception {
            this(behavior, metadata, false);
        }

        private Fixture(GT_CoverBehavior behavior, int metadata, boolean multiblock) throws Exception {
            GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
            tile = new GT_StructureIntegrationTest.TestTile(world,
                multiblock ? new GT_MetaTileEntity_VacuumFreezer("routing_multiblock")
                    : new GT_MetaTileEntity_PortableTank("routing_tank", 1, "", null), ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            item = new ItemStack(Items.stick, 1, metadata);
            GregTech_API.registerCoverBehavior(item, behavior);
            cover = CoverRegistry.buildCover(item, ForgeDirection.WEST, tile);
            tile.attachCover(cover);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            assertSame(cover, CoverUIFactory.INSTANCE.getGuiHolder(data(false)));
            assertTrue(CoverUIFactory.INSTANCE.canInteractWith(player, data(false)));
            standalone = new Panel(this, false);
        }

        private CoverGuiData data(boolean popup) {
            return new CoverGuiData(
                player,
                GT_Utility.stackToInt(item),
                4,
                64,
                7,
                ForgeDirection.WEST,
                popup
            );
        }

        private Panel popup() {
            return new Panel(this, true);
        }
    }

    private static final class Panel {

        private final ModularSyncManager sync = new ModularSyncManager(false);
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final ModularContainer container;

        private Panel(Fixture fixture, boolean popup) {
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            CoverGuiData data = fixture.data(popup);
            if (popup) {
                panel = fixture.cover.buildPopUpUI(
                    data,
                    "routing_popup",
                    handlers,
                    settings,
                    new ButtonWidget<>()
                );
            } else {
                panel = CoverUIFactory.INSTANCE.createPanel(data, handlers, settings);
            }
            WidgetTree.collectSyncValues(handlers, panel);
            container = new ModularContainer();
            container.construct(
                fixture.player,
                sync,
                settings,
                panel.getName(),
                data
            );
            assertTrue(handlers.isInitialised());
        }
    }

    private static final class LayoutViewport extends StaticResizer {

        private final Area area = new Area(0, 0, 600, 600);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Cover GUI viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
