package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.CoverUIFactory;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverArm;
import gregtech.common.covers.CoverFluidRegulator;
import gregtech.common.covers.CoverFluidfilter;
import gregtech.common.covers.GT_Cover_Arm;
import gregtech.common.covers.GT_Cover_FluidRegulator;
import gregtech.common.covers.GT_Cover_Fluidfilter;
import gregtech.common.covers.modes.BlockMode;
import gregtech.common.covers.modes.FilterDirectionMode;
import gregtech.common.covers.modes.FilterType;
import gregtech.common.covers.modes.MachineProcessingCondition;
import gregtech.common.covers.modes.TransferMode;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_QuantumTank;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2ArmFluidCoverTest {

    @Test
    public void nativeArmFieldsPreservePackedSlotsLegacyActionsAndDetachedCoverSafety() throws Throwable {
        run("arm");
    }

    @Test
    public void armGuiNormalizesOldSlotDataBeforeEditingWithoutRewritingUnreadLegacyNbt() throws Throwable {
        run("arm_migration");
    }

    @Test
    public void nativeFluidFilterModesAndPhantomSelectionRetainTheLegacyFluidRules() throws Throwable {
        run("filter");
    }

    @Test
    public void nativeRegulatorControlsChangeRealTransferIntervalsConditionsAndDirection() throws Throwable {
        run("regulator");
    }

    @Test
    public void steamRegulatorRetainsItsFluidRestrictionAndMigratesOldIntegerData() throws Throwable {
        run("steam");
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
            if ("arm".equals(scenario)) {
                checkArm();
            } else if ("arm_migration".equals(scenario)) {
                checkArmMigration();
            } else if ("filter".equals(scenario)) {
                checkFilter();
            } else if ("regulator".equals(scenario)) {
                checkRegulator();
            } else if ("steam".equals(scenario)) {
                checkSteam();
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkArm() throws Exception {
        Fixture fixture = new Fixture(new GT_Cover_Arm(20));
        assertTrue(fixture.cover instanceof CoverArm);
        CoverArm arm = (CoverArm) fixture.cover;
        arm.setVariable(0x90000000 | 2 << 14 | 1);
        fixture.build();
        assertEquals(2, fixture.fields.size());
        sendBoolean(fixture.handlers.findSyncHandler("isExport", BooleanSyncValue.class), true);
        assertTrue(arm.isExport());
        assertEquals(0x10000000, arm.getVariable() & 0x10000000);
        sendInt(fixture.field(0), 1);
        sendInt(fixture.field(1), 0);
        assertEquals(2, arm.getInternalSlotId());
        assertEquals(1, arm.getExternalSlotId());
        sendInt(fixture.field(0), -1);
        assertEquals(0, arm.getInternalSlotId());
        sendInt(fixture.field(1), 100000);
        assertEquals(fixture.adjacent.getSizeInventory(), arm.getExternalSlotId());
        int prior = arm.getVariable();
        int expected = new GT_Cover_Arm(20).onCoverScrewdriverclick(
            (byte) ForgeDirection.EAST.ordinal(),
            arm.getCoverID(),
            prior,
            fixture.tile,
            fixture.player,
            0.75F,
            0.5F,
            0.75F
        );
        arm.onCoverScrewdriverClick(fixture.player, 0.75F, 0.5F, 0.75F);
        assertEquals(expected, arm.getVariable());
        NBTTagCompound nbt = new NBTTagCompound();
        CoverRegistry.writeCoverToNbt(arm, nbt);
        Cover restored = CoverRegistry.buildCoverFromNbt(nbt, ForgeDirection.EAST, fixture.tile);
        assertTrue(restored instanceof CoverArm);
        assertEquals(arm.getVariable(), restored.getLegacyData());
        fixture.tile.detachCover(ForgeDirection.EAST);
        int detached = arm.getVariable();
        sendInt(fixture.field(0), 0);
        arm.setExport(!arm.isExport());
        assertEquals(detached, arm.getVariable());
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkArmMigration() throws Exception {
        Fixture fixture = new Fixture(new GT_Cover_Arm(20));
        CoverArm arm = (CoverArm) fixture.cover;
        fixture.build();
        int[] rawValues = {0, 12, -1, -12, 0xc0000000 | 2 << 14 | 3, 0x80000000 | 3 << 14 | 2};
        boolean[] exportModes = {true, true, false, false, true, false};
        int[] internalSlots = {0, 0, 2, 13, 3, 2};
        int[] externalSlots = {1, 13, 0, 0, 2, 3};
        for (int index = 0; index < rawValues.length; index++) {
            int raw = rawValues[index];
            arm.setLegacyData(raw);
            assertEquals(exportModes[index], arm.isExport());
            assertEquals(internalSlots[index], arm.getInternalSlotId());
            assertEquals(externalSlots[index], arm.getExternalSlotId());
            assertEquals(raw, arm.getLegacyData());
            NBTTagCompound saved = new NBTTagCompound();
            CoverRegistry.writeCoverToNbt(arm, saved);
            assertEquals(raw, ((NBTTagInt) saved.getTag("d")).func_150287_d());

            sendBoolean(fixture.handlers.findSyncHandler("isExport", BooleanSyncValue.class), !exportModes[index]);
            assertEquals(!exportModes[index], arm.isExport());
            assertEquals(internalSlots[index], arm.getInternalSlotId());
            assertEquals(externalSlots[index], arm.getExternalSlotId());

            arm.setLegacyData(raw);
            sendInt(fixture.field(0), 0);
            assertEquals(exportModes[index], arm.isExport());
            assertEquals(1, arm.getInternalSlotId());
            assertEquals(externalSlots[index], arm.getExternalSlotId());

            arm.setLegacyData(raw);
            sendInt(fixture.field(1), 1);
            assertEquals(exportModes[index], arm.isExport());
            assertEquals(internalSlots[index], arm.getInternalSlotId());
            assertEquals(2, arm.getExternalSlotId());
        }
    }

    private static void checkFilter() throws Exception {
        GT_Cover_Fluidfilter old = new GT_Cover_Fluidfilter();
        Fixture fixture = new Fixture(old);
        assertTrue(fixture.cover instanceof CoverFluidfilter);
        CoverFluidfilter filter = (CoverFluidfilter) fixture.cover;
        fixture.build();
        Fluid water = GT_ProcessingTestSupport.fluid("cover_filter_water");
        Fluid other = GT_ProcessingTestSupport.fluid("cover_filter_other");
        FluidSlot slot = findFluidSlot(fixture.panel);
        assertNotNull(slot);
        FluidSlotSyncHandler fluidHandler = slot.getSyncHandler();
        ItemStack cursor = new ItemStack(Items.stick, 4);
        fixture.player.inventory.setItemStack(cursor);
        fluidHandler.setValue(new FluidStack(water, 1), true, false);
        assertEquals(water.getID(), filter.getFluidId());
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(4, cursor.stackSize);
        assertNull(fixture.machine.mFluid);
        sendEnum(fixture.handlers, "io_mode", FilterDirectionMode.OUTPUT);
        sendEnum(fixture.handlers, "filter_type", FilterType.BLACKLIST);
        sendEnum(fixture.handlers, "block_mode", BlockMode.ALLOW);
        assertEquals(7, filter.getFilterMode());
        for (int mode = 0; mode < 8; mode++) {
            filter.setFilterMode(mode);
            assertEquals(water.getID(), filter.getFluidId());
            for (Fluid fluid : new Fluid[]{water, other}) {
                assertEquals(old.letsFluidIn(
                    (byte) 5,
                    filter.getCoverID(),
                    filter.getVariable(),
                    fluid,
                    fixture.tile
                ), filter.letsFluidIn(fluid));
                assertEquals(old.letsFluidOut(
                    (byte) 5,
                    filter.getCoverID(),
                    filter.getVariable(),
                    fluid,
                    fixture.tile
                ), filter.letsFluidOut(fluid));
            }
        }
        NBTTagCompound nbt = new NBTTagCompound();
        CoverRegistry.writeCoverToNbt(filter, nbt);
        CoverFluidfilter restored = (CoverFluidfilter) CoverRegistry.buildCoverFromNbt(
            nbt, ForgeDirection.EAST, fixture.tile);
        assertEquals(filter.getVariable(), restored.getVariable());
        fixture.tile.detachCover(ForgeDirection.EAST);
        int detached = filter.getVariable();
        filter.setFluidId(-1);
        sendEnum(fixture.handlers, "filter_type", FilterType.WHITELIST);
        assertEquals(detached, filter.getVariable());
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkRegulator() throws Exception {
        Fixture fixture = new Fixture(new GT_Cover_FluidRegulator(100));
        assertTrue(fixture.cover instanceof CoverFluidRegulator);
        CoverFluidRegulator regulator = (CoverFluidRegulator) fixture.cover;
        fixture.build();
        assertEquals(2, fixture.fields.size());
        sendInt(fixture.field(1), 3);
        sendInt(fixture.field(0), 150);
        assertEquals(3, regulator.getTickRate());
        assertEquals(300, regulator.getMaxSpeed());
        assertFalse(regulator.allowsTickRateAddition());
        Fluid fluid = GT_ProcessingTestSupport.fluid("cover_regulator_fluid");
        assertSame(fixture.adjacentTile,
            fixture.tile.getITankContainerAtSide((byte) ForgeDirection.EAST.ordinal()));
        assertEquals(150, fixture.adjacentTile.fill(ForgeDirection.WEST, new FluidStack(fluid, 150), false));
        fixture.machine.mFluid = new FluidStack(fluid, 1000);
        sendEnum(fixture.handlers, "condition_mode", MachineProcessingCondition.CONDITIONAL);
        fixture.tile.disableWorking();
        fixture.tile.tick(1);
        fixture.tile.tick(2);
        fixture.tile.tick(3);
        assertEquals(1000, fixture.machine.mFluid.amount);
        assertNull(fixture.adjacent.mFluid);
        fixture.tile.enableWorking();
        fixture.tile.tick(6);
        assertEquals(850, fixture.machine.mFluid.amount);
        assertEquals(150, fixture.adjacent.mFluid.amount);
        assertFalse(regulator.letsFluidOut(fluid));
        sendEnum(fixture.handlers, "condition_mode", MachineProcessingCondition.INVERTED);
        fixture.tile.tick(9);
        assertEquals(850, fixture.machine.mFluid.amount);
        fixture.tile.disableWorking();
        fixture.tile.tick(12);
        assertEquals(700, fixture.machine.mFluid.amount);
        sendEnum(fixture.handlers, "condition_mode", MachineProcessingCondition.ALWAYS);
        sendEnum(fixture.handlers, "io_mode", TransferMode.IMPORT);
        fixture.tile.tick(15);
        assertEquals(850, fixture.machine.mFluid.amount);
        assertEquals(150, fixture.adjacent.mFluid.amount);
        sendInt(fixture.field(1), 0);
        assertEquals(2, regulator.getTickRateForUi());
        sendInt(fixture.field(0), Integer.MIN_VALUE);
        assertEquals(-200, regulator.getSpeed());
        sendInt(fixture.field(0), Integer.MAX_VALUE);
        assertEquals(200, regulator.getSpeed());
        NBTTagCompound nbt = new NBTTagCompound();
        CoverRegistry.writeCoverToNbt(regulator, nbt);
        CoverFluidRegulator restored = (CoverFluidRegulator) CoverRegistry.buildCoverFromNbt(
            nbt, ForgeDirection.EAST, fixture.tile);
        assertEquals(regulator.getSpeed(), restored.getSpeed());
        assertEquals(regulator.getTickRateForUi(), restored.getTickRateForUi());
        assertSame(regulator.getCondition(), restored.getCondition());
        fixture.tile.detachCover(ForgeDirection.EAST);
        regulator.setSpeed(0);
        regulator.setTickRateForUi(2000);
        regulator.setCondition(CoverFluidRegulator.Conditional.INVERTED);
        assertEquals(200, regulator.getSpeed());
        assertEquals(2, regulator.getTickRateForUi());
    }

    private static void checkSteam() throws Exception {
        Fixture fixture = new Fixture(new GT_Cover_FluidRegulator(100, true));
        CoverFluidRegulator regulator = (CoverFluidRegulator) fixture.cover;
        NBTTagCompound old = new NBTTagCompound();
        old.setTag("d", new NBTTagInt(-25));
        regulator.readFromNbt(old);
        assertEquals(-25, regulator.getSpeed());
        assertEquals(1, regulator.getTickRateForUi());
        assertSame(CoverFluidRegulator.Conditional.ALWAYS, regulator.getCondition());
        regulator.setSpeed(100);
        fixture.machine.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("cover_not_steam"), 1000);
        fixture.tile.tick(1);
        assertNull(fixture.adjacent.mFluid);
        fixture.machine.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("steam"), 1000);
        fixture.tile.tick(2);
        assertEquals(900, fixture.machine.mFluid.amount);
        assertEquals(100, fixture.adjacent.mFluid.amount);
        assertTrue(fixture.tile.dirtyCalls > 0);
    }

    private static void sendInt(IntSyncValue value, int selected) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeVarIntToBuffer(selected);
            value.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendBoolean(BooleanSyncValue value, boolean selected) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeBoolean(selected);
            value.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static <E extends Enum<E>> void sendEnum(
        PanelSyncManager handlers,
        String key,
        E selected
    ) throws Exception {
        EnumSyncValue<?, ?> value = handlers.findSyncHandler(key, EnumSyncValue.class);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            NetworkUtils.writeEnumValue(packet, selected);
            value.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static FluidSlot findFluidSlot(IWidget widget) {
        if (widget instanceof FluidSlot) {
            return (FluidSlot) widget;
        }
        for (IWidget child : widget.getChildren()) {
            FluidSlot found = findFluidSlot(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void collectFields(IWidget widget, List<TextFieldWidget> fields) {
        if (widget instanceof TextFieldWidget) {
            fields.add((TextFieldWidget) widget);
        }
        for (IWidget child : widget.getChildren()) {
            collectFields(child, fields);
        }
    }

    private static final class Fixture {

        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_MetaTileEntity_QuantumTank machine =
            new GT_MetaTileEntity_QuantumTank("cover_host", 6, "", null);
        private final GT_MetaTileEntity_QuantumTank adjacent =
            new GT_MetaTileEntity_QuantumTank("cover_adjacent", 6, "", null);
        private final CoverTile tile = new CoverTile(world, machine);
        private final CoverTile adjacentTile = new CoverTile(world, adjacent);
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final Cover cover;
        private final List<TextFieldWidget> fields = new ArrayList<>();
        private PanelSyncManager handlers;
        private ModularPanel panel;
        private ModularContainer container;

        private Fixture(GT_CoverBehavior behavior) throws Exception {
            assertTrue(machine.getCapacity() > 0);
            assertTrue(adjacent.getCapacity() > 0);
            GregTech_API.registerCoverBehavior(new ItemStack(Items.stick), behavior);
            world.putTile(4, 64, 7, tile);
            world.putTile(5, 64, 7, adjacentTile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 41L);
            GT_ProcessingTestSupport.setField(adjacentTile, "mTickTimer", 41L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            tile.setCoverIDAtSide(
                (byte) ForgeDirection.EAST.ordinal(),
                GT_Utility.stackToInt(new ItemStack(Items.stick))
            );
            cover = tile.getCoverAtSide(ForgeDirection.EAST);
            assertTrue(cover.useMui2());
        }

        private void build() {
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            CoverGuiData data = new CoverGuiData(
                player,
                cover.getCoverID(),
                4,
                64,
                7,
                ForgeDirection.EAST,
                false
            );
            panel = CoverUIFactory.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            collectFields(panel, fields);
            assertTrue(container.isInitialized());
            assertEquals(36, container.inventorySlots.size());
            assertTrue(container.canInteractWith(player));
        }

        private IntSyncValue field(int index) {
            return (IntSyncValue) fields.get(index).getStringValue();
        }
    }

    private static final class CoverTile extends GT_StructureIntegrationTest.TestTile {

        private int dirtyCalls;

        private CoverTile(GT_ProcessingTestSupport.ProcessingWorld world, MetaTileEntity machine) {
            super(world, machine, ForgeDirection.NORTH);
        }

        private void tick(long timer) {
            tickCoverAtSide((byte) ForgeDirection.EAST.ordinal(), (byte) 0, timer);
        }

        @Override
        public void markDirty() {
            dirtyCalls++;
        }
    }
}
