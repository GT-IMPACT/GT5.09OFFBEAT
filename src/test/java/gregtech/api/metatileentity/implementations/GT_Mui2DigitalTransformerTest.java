package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.gui.GT_Container_DigitalTransformer;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Digital_Transformer;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2DigitalTransformerTest {

    @Test
    public void nativeContainerKeepsTheLegacyControlsReadoutsAndPlayerInventory() throws Throwable {
        run("layout");
    }

    @Test
    public void everyButtonAndShiftStepMatchesTheLegacyArithmeticIncludingOverflow() throws Throwable {
        run("actions");
    }

    @Test
    public void serverPacketsRespectDistanceOwnershipAndTheCurrentMachine() throws Throwable {
        run("access");
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
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            GTGuis.init();
            Fixture fixture = new Fixture();
            if ("layout".equals(scenario)) {
                checkLayout(fixture);
            } else if ("actions".equals(scenario)) {
                checkActions(fixture);
            } else if ("access".equals(scenario)) {
                checkAccess(fixture);
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) {
        assertTrue(fixture.machine.useMui2());
        assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
        assertTrue(fixture.container.isInitialized());
        assertFalse(fixture.container.isClient());
        assertEquals(0, fixture.machine.mInventory.length);
        assertEquals(36, fixture.container.inventorySlots.size());
        assertEquals(16, fixture.buttons.size());

        int[] columns = {8, 26, 152, 134};
        for (int action = 0; action < fixture.buttons.size(); action++) {
            ButtonWidget<?> button = fixture.buttons.get(action);
            assertSame(fixture.handlers.findSyncHandler("adjust_" + action), button.getSyncHandler());
            resize(button, new Viewport());
            Area area = button.getArea();
            assertEquals(columns[action / 4], area.x);
            assertEquals(5 + action % 4 * 18 + CommonWidgets.MACHINE_HEADER_HEIGHT, area.y);
            assertEquals(16, area.width);
            assertEquals(16, area.height);
        }

        ItemStack marker = new ItemStack(Items.redstone, 7);
        fixture.player.inventory.mainInventory[0] = marker;
        for (Object entry : fixture.container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            assertText("player_inventory", slot.getSlotGroupName());
            if (slot.getSlotIndex() == 0) {
                assertSame(marker, slot.getStack());
            }
        }

        fixture.machine.EUT = 128;
        fixture.machine.AMP = -2;
        fixture.refreshReadouts();
        String[] expected = {"Digital Trans", "EUT: 128", "TIER: MV", "AMP: -2", "SUM: -256"};
        int[] rows = {8, 24, 32, 40, 48};
        List<TextWidget<?>> texts = fixture.texts();
        assertEquals(expected.length, texts.size());
        for (int index = 0; index < expected.length; index++) {
            TextWidget<?> text = texts.get(index);
            assertText(expected[index], text.getKey().getFormatted());
            resize(text, new Viewport());
            assertEquals(46, text.getArea().x);
            assertEquals(rows[index] + CommonWidgets.MACHINE_HEADER_HEIGHT, text.getArea().y);
        }
    }

    private static void checkActions(Fixture fixture) throws Exception {
        GT_MetaTileEntity_Digital_Transformer reference = machine("legacy_transformer_reference");
        GT_ProcessingTestSupport.ProcessingWorld referenceWorld = new GT_ProcessingTestSupport.ProcessingWorld();
        GT_StructureIntegrationTest.TestTile referenceTile = new GT_StructureIntegrationTest.TestTile(
            referenceWorld,
            reference,
            ForgeDirection.NORTH
        );
        referenceWorld.putTile(4, 64, 7, referenceTile);
        GT_StructureIntegrationTest.TestPlayer referencePlayer = new GT_StructureIntegrationTest.TestPlayer(
            referenceWorld
        );
        GT_Container_DigitalTransformer legacy = new GT_Container_DigitalTransformer(referencePlayer.inventory,
            referenceTile);
        int[][] seeds = {
            {4095, 257}, {-4095, -257}, {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, Integer.MAX_VALUE}
        };
        for (int[] seed : seeds) {
            for (int action = 0; action < 16; action++) {
                for (int shift = 0; shift <= 1; shift++) {
                    for (int mouse = 0; mouse <= 1; mouse++) {
                        fixture.machine.EUT = reference.EUT = seed[0];
                        fixture.machine.AMP = reference.AMP = seed[1];
                        legacy.slotClick(action, mouse, shift, referencePlayer);
                        fixture.press(action, mouse, shift == 1);
                        assertEquals(reference.EUT, fixture.machine.EUT);
                        assertEquals(reference.AMP, fixture.machine.AMP);
                        assertEquals((long) reference.EUT * reference.AMP >= 0, fixture.machine.producing);
                    }
                }
            }
        }
        fixture.machine.EUT = Integer.MIN_VALUE;
        fixture.machine.AMP = -1;
        fixture.refreshReadouts();
        assertText("TIER: ULV", fixture.texts().get(2).getKey().getFormatted());
        assertText("SUM: 2147483648", fixture.texts().get(4).getKey().getFormatted());

        NBTTagCompound saved = new NBTTagCompound();
        fixture.machine.saveNBTData(saved);
        assertTrue(saved.hasKey("eEUT"));
        assertTrue(saved.hasKey("eAMP"));
        fixture.machine.EUT = 0;
        fixture.machine.AMP = 0;
        fixture.machine.loadNBTData(saved);
        assertEquals(Integer.MIN_VALUE, fixture.machine.EUT);
        assertEquals(-1, fixture.machine.AMP);
        assertTrue(fixture.machine.producing);
    }

    private static void checkAccess(Fixture fixture) throws Exception {
        fixture.machine.EUT = 100;
        fixture.press(8, 0, false);
        assertEquals(164, fixture.machine.EUT);
        fixture.player.setPosition(100, 64, 100);
        fixture.press(8, 0, false);
        assertEquals(164, fixture.machine.EUT);
        fixture.player.setPosition(4.5, 64.5, 7.5);
        fixture.tile.setOwnerName("Another owner");
        GT_ProcessingTestSupport.setField(fixture.tile, "mLockUpgrade", true);
        fixture.press(8, 0, false);
        assertEquals(164, fixture.machine.EUT);
        GT_ProcessingTestSupport.setField(fixture.tile, "mLockUpgrade", false);
        fixture.machine.setBaseMetaTileEntity(null);
        fixture.press(8, 0, false);
        assertEquals(164, fixture.machine.EUT);
    }

    private static GT_MetaTileEntity_Digital_Transformer machine(String name) {
        return new GT_MetaTileEntity_Digital_Transformer(name, 1, "", null);
    }

    private static void assertText(String expected, String actual) {
        String visible = EnumChatFormatting.getTextWithoutFormattingCodes(actual);
        assertTrue("Expected [" + expected + "] but got [" + visible + "]", expected.equals(visible));
    }

    private static void resize(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        resizer.initResizing(false);
        assertTrue(resizer.resize(false));
        resizer.preApplyPos();
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_Digital_Transformer machine = machine("mui2_digital_transformer");
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PosGuiData data;
        private final PanelSyncManager handlers;
        private final ModularPanel panel;
        private final ModularContainer container;
        private final List<ButtonWidget<?>> buttons = new ArrayList<>();

        private Fixture() throws Exception {
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            assertTrue(tile.isUseableByPlayer(player));
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            data = new PosGuiData(player, 4, 64, 7);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            for (IWidget child : panel.getChildren()) {
                if (child instanceof ButtonWidget) {
                    buttons.add((ButtonWidget<?>) child);
                }
            }
        }

        private void press(int action, int mouse, boolean shift) throws Exception {
            InteractionSyncHandler handler = (InteractionSyncHandler) buttons.get(action).getSyncHandler();
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                new MouseData(
                    Side.CLIENT,
                    mouse,
                    shift,
                    false,
                    false
                ).writeToPacket(packet);
                handler.readOnServer(1, packet);
            } finally {
                packet.release();
            }
        }

        private void refreshReadouts() {
            handlers.findSyncHandler("eut", IntSyncValue.class).updateCacheFromSource(false);
            handlers.findSyncHandler("amperage", IntSyncValue.class).updateCacheFromSource(false);
        }

        private List<TextWidget<?>> texts() {
            List<TextWidget<?>> texts = new ArrayList<>();
            for (IWidget child : panel.getChildren()) {
                if (child instanceof TextWidget) {
                    texts.add((TextWidget<?>) child);
                }
            }
            return texts;
        }
    }

    private static final class Viewport extends StaticResizer {

        private final Area area = new Area(0, 0, 176, 166 + CommonWidgets.MACHINE_HEADER_HEIGHT);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Digital Transformer legacy layout";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
