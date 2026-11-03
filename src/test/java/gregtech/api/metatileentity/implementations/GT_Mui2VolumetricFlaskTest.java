package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.NumberFormat;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.BaseTextFieldWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldHandler;
import com.cleanroommc.modularui.widgets.textfield.TextFieldRenderer;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.items.GT_VolumetricFlask;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GT_Mui2VolumetricFlaskTest {

    @Test
    public void nativePlayerInventoryFactoryKeepsTheReferenceWindowAndNumericField() throws Throwable {
        run("layout");
    }

    @Test
    public void capacityPacketsSaveTheOpenedStackImmediatelyAndRespectCapacityLimits() throws Throwable {
        run("capacity");
    }

    @Test
    public void existingCapacityAndFluidNbtRetainFillDrainAndReloadBehavior() throws Throwable {
        run("fluid");
    }

    @Test
    public void onlyAnEmptyFlaskUsedInAirReachesTheNativeServerOpeningPath() throws Throwable {
        run("routing");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            try {
                runtime.run(getClass(), scenario, "");
            } catch (Throwable failure) {
                failure.getMessage();
                throw failure;
            }
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GT_ProcessingTestSupport.setField(Loader.instance(), "namedMods", new HashMap<String, ModContainer>());
        GTGuis.init();
        String previousName = Thread.currentThread().getName();
        Thread.currentThread().setName("Server thread");
        try (GT_Mui2FluidHatchGuiTest.HeadlessClient ignored = new GT_Mui2FluidHatchGuiTest.HeadlessClient()) {
            Fixture fixture = new Fixture();
            if ("layout".equals(scenario)) {
                checkLayout(fixture);
            } else if ("capacity".equals(scenario)) {
                checkCapacity(fixture);
            } else if ("fluid".equals(scenario)) {
                checkFluid(fixture);
            } else if ("routing".equals(scenario)) {
                checkRouting(fixture);
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            Thread.currentThread().setName(previousName);
        }
    }

    private static void checkLayout(Fixture fixture) throws Exception {
        assertSame(fixture.flask, GuiFactories.playerInventory().getGuiHolder(fixture.data));
        assertEquals("mui:player_inv", GuiFactories.playerInventory().getFactoryName());
        assertSame(fixture.stack, fixture.data.getUsedItemStack());
        assertEquals(150, fixture.panel.getArea().width);
        assertEquals(50, fixture.panel.getArea().height);
        assertEquals("volumetricFlask", fixture.panel.getName());
        // ModularSyncManager always binds the player's inventory for synchronization, without visual slot widgets.
        assertEquals(36, fixture.container.inventorySlots.size());
        boolean[] playerSlots = new boolean[36];
        for (Object entry : fixture.container.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            assertTrue(ModularSlot.isPlayerSlot(slot));
            assertEquals("player_inventory", slot.getSlotGroupName());
            assertEquals(Integer.MIN_VALUE, slot.xDisplayPosition);
            assertEquals(Integer.MIN_VALUE, slot.yDisplayPosition);
            assertFalse(playerSlots[slot.getSlotIndex()]);
            playerSlots[slot.getSlotIndex()] = true;
        }
        assertTrue(fixture.container.canInteractWith(fixture.player));
        assertEquals(80, fixture.field.getArea().width);
        assertTrue(fixture.field.isNumbers());
        assertEquals(5, fixture.field.getTooltip().getShowUpTimer());
        assertTrue(fixture.capacity().isAllowC2S());
        assertEquals(1, validated(fixture, "-20"));
        assertEquals(250, validated(fixture, "250"));
        assertEquals(1000, validated(fixture, "2000"));
        Field handlerField = BaseTextFieldWidget.class.getDeclaredField("handler");
        handlerField.setAccessible(true);
        Object handler = handlerField.get(fixture.field);
        Field maxCharacters = TextFieldHandler.class.getDeclaredField("maxCharacters");
        maxCharacters.setAccessible(true);
        assertEquals(10, maxCharacters.getInt(handler));
        Field rendererField = BaseTextFieldWidget.class.getDeclaredField("renderer");
        rendererField.setAccessible(true);
        Field formatAsInteger = TextFieldRenderer.class.getDeclaredField("formatAsInteger");
        formatAsInteger.setAccessible(true);
        assertTrue(formatAsInteger.getBoolean(rendererField.get(fixture.field)));
        int buttons = 0;
        int titles = 0;
        for (IWidget widget : fixture.widgets) {
            assertFalse(widget instanceof ItemSlot);
            if (widget instanceof ButtonWidget) {
                buttons++;
            }
            if (widget instanceof TextWidget
                && ((TextWidget<?>) widget).getKey().getFormatted().contains("Test Flask")) {
                titles++;
            }
            String details = GT_Mui2InputBusTest.geometry(widget, fixture.panel, 0);
            Area area = widget.getArea();
            assertTrue("Unresolved " + details,
                widget.resizer().isXCalculated() && widget.resizer().isYCalculated()
                    && widget.resizer().isWidthCalculated() && widget.resizer().isHeightCalculated());
            assertTrue(details, area.x >= fixture.panel.getArea().x && area.y >= fixture.panel.getArea().y);
            assertTrue(details, area.ex() <= fixture.panel.getArea().ex() && area.ey() <= fixture.panel.getArea().ey());
        }
        assertEquals("Only the native close button remains", 1, buttons);
        assertEquals(1, titles);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            GuiFactories.playerInventory().writeGuiData(fixture.data, packet);
            PlayerInventoryGuiData decoded = GuiFactories.playerInventory().readGuiData(fixture.player, packet);
            assertSame(InventoryTypes.PLAYER, decoded.getInventoryType());
            assertEquals(3, decoded.getSlotIndex());
            assertSame(fixture.stack, decoded.getUsedItemStack());
            assertEquals(0, packet.readableBytes());
        } finally {
            packet.release();
        }
    }

    private static int validated(Fixture fixture, String input) throws Exception {
        return NumberFormat.AMOUNT_TEXT.format.parse(fixture.field.getValidator().apply(input)).intValue();
    }

    private static void checkCapacity(Fixture fixture) throws Exception {
        ItemStack other = new ItemStack(fixture.flask);
        fixture.flask.setCapacity(other, 17);
        fixture.player.inventory.setInventorySlotContents(0, other);
        fixture.player.inventory.currentItem = 0;
        for (int capacity : new int[] {1, 250, 1000}) {
            fixture.sendCapacity(capacity);
            assertSame(fixture.stack, fixture.player.inventory.getStackInSlot(3));
            assertEquals(capacity, fixture.flask.getCapacity(fixture.stack));
            assertEquals(capacity, fixture.stack.getTagCompound().getInteger("Capacity"));
            assertEquals(17, fixture.flask.getCapacity(other));
        }
        fixture.sendCapacity(333);
        fixture.sendCapacity(0);
        assertEquals(333, fixture.flask.getCapacity(fixture.stack));
        fixture.sendCapacity(-1);
        assertEquals(333, fixture.flask.getCapacity(fixture.stack));
        fixture.sendCapacity(Integer.MAX_VALUE);
        assertEquals(1000, fixture.flask.getCapacity(fixture.stack));
        fixture.capacity().updateCacheFromSource(true);
        assertEquals(1000, fixture.capacity().getIntValue());
        assertEquals("Test Flask", fixture.stack.getDisplayName());
    }

    private static void checkFluid(Fixture fixture) {
        ItemStack legacy = new ItemStack(fixture.flask);
        assertEquals(1000, fixture.flask.getCapacity(legacy));
        assertNull(legacy.getTagCompound());
        fixture.flask.setCapacity(legacy, 0);
        assertNull(legacy.getTagCompound());
        NBTTagCompound old = new NBTTagCompound();
        old.setInteger("Capacity", 333);
        old.setString("legacy", "preserved");
        FluidStack water = new FluidStack(FluidRegistry.WATER, 250);
        water.tag = new NBTTagCompound();
        water.tag.setString("grade", "tagged_water");
        old.setTag("Fluid", water.writeToNBT(new NBTTagCompound()));
        legacy.setTagCompound(old);
        assertEquals(333, fixture.flask.getCapacity(legacy));
        assertEquals(83, fixture.flask.getFreeSpace(legacy));
        FluidStack offered = water.copy();
        offered.amount = 200;
        assertEquals(83, fixture.flask.fill(legacy, offered, false));
        assertEquals(250, fixture.flask.getFluid(legacy).amount);
        assertEquals(83, fixture.flask.fill(legacy, offered, true));
        assertEquals(333, fixture.flask.getFluid(legacy).amount);
        assertEquals(0, fixture.flask.fill(legacy, new FluidStack(FluidRegistry.LAVA, 1), true));
        assertEquals(70, fixture.flask.drain(legacy, 70, false).amount);
        assertEquals(333, fixture.flask.getFluid(legacy).amount);
        assertEquals(70, fixture.flask.drain(legacy, 70, true).amount);
        assertEquals(263, fixture.flask.getFluid(legacy).amount);
        NBTTagCompound saved = new NBTTagCompound();
        legacy.writeToNBT(saved);
        ItemStack restored = ItemStack.loadItemStackFromNBT(saved);
        assertNotNull(restored);
        assertEquals(333, fixture.flask.getCapacity(restored));
        assertEquals(263, fixture.flask.getFluid(restored).amount);
        assertEquals("tagged_water", fixture.flask.getFluid(restored).tag.getString("grade"));
        assertEquals("preserved", restored.getTagCompound().getString("legacy"));
        ItemStack stacked = restored.copy();
        stacked.stackSize = 2;
        assertEquals(0, fixture.flask.fill(stacked, offered, true));
        assertNull(fixture.flask.drain(stacked, 100, true));
        assertEquals(263, fixture.flask.getFluid(stacked).amount);
        assertEquals(263, fixture.flask.drain(restored, 1000, true).amount);
        assertTrue(fixture.flask.isEmpty(restored));
        assertFalse(restored.getTagCompound().hasKey("Fluid"));
        assertEquals(333, fixture.flask.getCapacity(restored));
    }

    private static void checkRouting(Fixture fixture) {
        fixture.world.isRemote = true;
        assertSame(fixture.stack, fixture.flask.onItemRightClick(fixture.stack, fixture.world, fixture.player));
        assertEquals(0, fixture.flask.rayTraces);
        fixture.world.isRemote = false;
        fixture.flask.setFluid(fixture.stack, new FluidStack(FluidRegistry.WATER, 10));
        assertSame(fixture.stack, fixture.flask.onItemRightClick(fixture.stack, fixture.world, fixture.player));
        assertEquals(0, fixture.flask.rayTraces);
        fixture.flask.setFluid(fixture.stack, null);
        fixture.flask.hit = new MovingObjectPosition(0, 0, 0, 1, Vec3.createVectorHelper(0.5, 0.5, 0.5));
        assertSame(fixture.stack, fixture.flask.onItemRightClick(fixture.stack, fixture.world, fixture.player));
        assertEquals(1, fixture.flask.rayTraces);
        fixture.flask.hit = null;
        // The native factory requires EntityPlayerMP; reaching its guard proves this branch uses that factory.
        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> fixture.flask.onItemRightClick(fixture.stack, fixture.world, fixture.player));
        assertTrue(failure.getMessage().contains("Expected server player"));
        assertEquals(2, fixture.flask.rayTraces);
    }

    private static void initializeResizers(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        for (IWidget child : widget.getChildren()) {
            initializeResizers(child, resizer);
        }
    }

    private static final class Fixture {

        private final ControlledFlask flask = new ControlledFlask();
        private final ItemStack stack = new ItemStack(flask);
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final NativePlayer player = new NativePlayer(world);
        private final PlayerInventoryGuiData data;
        private final ModularPanel panel;
        private final ModularContainer container = new ModularContainer();
        private final List<IWidget> widgets = new ArrayList<>();
        private TextFieldWidget field;

        private Fixture() throws Exception {
            stack.setStackDisplayName("Test Flask");
            player.inventory.currentItem = 3;
            player.inventory.setInventorySlotContents(3, stack);
            data = new PlayerInventoryGuiData(player, InventoryTypes.PLAYER, 3);
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            settings.defaultCanInteractWith(GuiFactories.playerInventory(), data);
            panel = GuiFactories.playerInventory().createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(player, sync, settings, panel.getName(), data);
            player.openContainer = container;
            GT_Mui2InputBusTest.prepareLayout(panel, sync);
            collect(panel);
            assertNotNull(field);
            GT_Mui2FluidHatchGuiTest.freezeTranslationCache(panel);
            initializeResizers(panel, new Viewport());
            WidgetTree.resizeInternal(panel.resizer(), false);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            if (widget instanceof TextFieldWidget) {
                assertNull(field);
                field = (TextFieldWidget) widget;
            }
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private IntSyncValue capacity() {
            return (IntSyncValue) field.getStringValue();
        }

        private void sendCapacity(int value) throws Exception {
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeVarIntToBuffer(value);
                capacity().readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
        }
    }

    private static final class ControlledFlask extends GT_VolumetricFlask {

        private MovingObjectPosition hit;
        private int rayTraces;

        private ControlledFlask() {
            super("mui2_flask_test", "Test Flask", 1000);
        }

        @Override
        protected MovingObjectPosition getMovingObjectPositionFromPlayer(
            World world,
            EntityPlayer player,
            boolean fluids
        ) {
            rayTraces++;
            return hit;
        }
    }

    private static final class Viewport extends StaticResizer {

        private final Area area = new Area(0, 0, 600, 600);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Volumetric flask viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }

    private static final class NativePlayer extends GT_StructureIntegrationTest.TestPlayer {

        private NativePlayer(World world) {
            super(world);
        }

        @Override
        public void openGui(
            Object mod,
            int guiId,
            World world,
            int x,
            int y,
            int z
        ) {
            throw new AssertionError("Volumetric flask attempted to open a legacy GUI");
        }
    }
}
