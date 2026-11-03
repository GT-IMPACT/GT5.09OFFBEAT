package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.ByteSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_ChestBuffer;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_Filter;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_ItemDistributor;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_Regulator;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_SuperBuffer;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_TypeFilter;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2AutomationTest {

    @Test
    public void allSixAutomationWindowsUseTheFactoryAndExposeOnlyTheirOwnPhysicalSlots() throws Throwable {
        run("build");
    }

    @Test
    public void filterSamplesUseTheLiveTemplateSlotsWithoutConsumingTheCursor() throws Throwable {
        run("filter");
    }

    @Test
    public void regulatorKeepsNineCountedTemplatesNineTargetsAndTheNativeChargerSlot() throws Throwable {
        run("regulator");
    }

    @Test
    public void distributorSidePopupEditsThePersistedSideCountsAndRetainsScrewdriverControl() throws Throwable {
        run("distributor");
    }

    @Test
    public void typeFilterIconDelegatesToItsExistingPrefixCycleAndPreservesTheCursor() throws Throwable {
        run("type");
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
            if ("build".equals(scenario)) {
                checkAllContainers();
            } else if ("filter".equals(scenario)) {
                checkFilter();
            } else if ("regulator".equals(scenario)) {
                checkRegulator();
            } else if ("distributor".equals(scenario)) {
                checkDistributor();
            } else if ("type".equals(scenario)) {
                checkTypeFilter();
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkAllContainers() throws Exception {
        String[] families = {"ChestBuffer", "Filter", "ItemDistributor", "Regulator", "SuperBuffer", "TypeFilter"};
        int[] physicalCounts = {27, 9, 27, 10, 0, 9};
        int[] phantomCounts = {0, 9, 0, 9, 0, 1};
        int[] toggles = {4, 5, 3, 1, 4, 5};
        for (int family = 0; family < families.length; family++) {
            Fixture fixture = new Fixture(createMachine(families[family]));
            assertTrue(fixture.machine.useMui2());
            assertSame(fixture.machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(fixture.data));
            assertTrue(fixture.container.isInitialized());
            assertFalse(fixture.container.isClient());
            assertEquals(36 + physicalCounts[family], fixture.container.inventorySlots.size());
            assertEquals(phantomCounts[family], fixture.phantomSlots().size());
            assertEquals(toggles[family], countToggles(fixture.panel));
            boolean[] visible = new boolean[fixture.machine.mInventory.length];
            int count = 0;
            for (Object entry : fixture.container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() != fixture.machine.inventoryHandler) {
                    continue;
                }
                int index = slot.getSlotIndex();
                assertFalse(visible[index]);
                visible[index] = true;
                assertTrue(slot.isCanTake());
                count++;
                ItemStack stack = new ItemStack(Items.stick, index + 1);
                fixture.machine.mInventory[index] = stack;
                assertSame(stack, slot.getStack());
                ItemStack replacement = new ItemStack(Items.paper, 2);
                slot.putStack(replacement);
                assertSame(replacement, fixture.machine.mInventory[index]);
            }
            assertEquals(physicalCounts[family], count);
            if ("Regulator".equals(families[family])) {
                assertTrue(visible[19]);
                assertFalse(visible[18]);
            }
            if ("TypeFilter".equals(families[family])) {
                assertFalse(visible[9]);
            }
            for (int index = 0; index < Math.min(9, physicalCounts[family]); index++) {
                assertTrue(visible[index]);
            }
            assertTrue(fixture.container.canInteractWith(fixture.player));
            fixture.player.setPosition(100, 64, 100);
            assertFalse(fixture.container.canInteractWith(fixture.player));
            fixture.player.setPosition(4.5, 64.5, 7.5);

            setBoolean(fixture.panelSync, "emitEnergy", true);
            NBTTagCompound saved = new NBTTagCompound();
            fixture.machine.saveNBTData(saved);
            assertTrue(saved.getBoolean("bOutput"));
            assertTrue(fixture.machine.bOutput);
            assertTrue(fixture.tile.dirtyCalls > 0);
            if ("ChestBuffer".equals(families[family]) || "SuperBuffer".equals(families[family])) {
                setBoolean(fixture.panelSync, "emitRedstone", true);
                setBoolean(fixture.panelSync, "invertRedstone", true);
                setBoolean(fixture.panelSync, "stocking", true);
                assertTrue(fixture.machine.bRedstoneIfFull);
                assertTrue(fixture.machine.bInvert);
                assertTrue(fixture.machine.bStockingMode);
            }
        }
    }

    private static void checkFilter() throws Exception {
        GT_MetaTileEntity_Filter machine = (GT_MetaTileEntity_Filter) createMachine("Filter");
        Fixture fixture = new Fixture(machine);
        List<PhantomItemSlot> samples = fixture.phantomSlots();
        ItemStack cursor = new ItemStack(Items.stick, 37);
        fixture.player.inventory.setItemStack(cursor);
        for (int index = 0; index < samples.size(); index++) {
            PhantomItemSlot sample = samples.get(index);
            assertEquals(index + 9, sample.getSlot().getSlotIndex());
            click(sample, 0, false);
            assertSame(Items.stick, machine.mInventory[index + 9].getItem());
            assertEquals(1, machine.mInventory[index + 9].stackSize);
            assertSame(cursor, fixture.player.inventory.getItemStack());
            assertEquals(37, cursor.stackSize);
            assertFalse(machine.isValidSlot(index + 9));
        }
        fixture.player.inventory.setItemStack(null);
        click(samples.get(0), 1, false);
        assertEquals(1, machine.mInventory[9].stackSize);
        click(samples.get(0), 0, false);
        assertNull(machine.mInventory[9]);
        setBoolean(fixture.panelSync, "invertFilter", true);
        setBoolean(fixture.panelSync, "ignoreNbt", true);
        assertTrue(machine.bInvertFilter);
        assertTrue(machine.bIgnoreNBT);
        assertFalse(machine.bStockingMode);
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        GT_MetaTileEntity_Filter restored = (GT_MetaTileEntity_Filter) machine.newMetaEntity(null);
        restored.loadNBTData(saved);
        assertTrue(restored.bInvertFilter);
        assertTrue(restored.bIgnoreNBT);
    }

    private static void checkRegulator() throws Exception {
        GT_MetaTileEntity_Regulator machine = (GT_MetaTileEntity_Regulator) createMachine("Regulator");
        Fixture fixture = new Fixture(machine);
        List<PhantomItemSlot> samples = fixture.phantomSlots();
        ItemStack cursor = new ItemStack(Items.stick, 16);
        fixture.player.inventory.setItemStack(cursor);
        for (int index = 0; index < 9; index++) {
            assertEquals(index + 9, samples.get(index).getSlot().getSlotIndex());
            click(samples.get(index), 0, false);
            assertEquals(16, machine.mInventory[index + 9].stackSize);
            IntSyncValue target = fixture.panelSync.findSyncHandler("targetSlot", index, IntSyncValue.class);
            sendInt(target, index * 10);
            assertEquals(index * 10, machine.mTargetSlots[index]);
        }
        assertEquals(16, cursor.stackSize);
        assertSame(cursor, fixture.player.inventory.getItemStack());
        fixture.player.inventory.setItemStack(null);
        click(samples.get(0), 0, true);
        assertEquals(8, machine.mInventory[9].stackSize);
        click(samples.get(0), 1, false);
        assertEquals(9, machine.mInventory[9].stackSize);
        click(samples.get(0), 0, true);
        click(samples.get(0), 0, false);
        assertNull(machine.mInventory[9]);
        sendInt(fixture.panelSync.findSyncHandler("targetSlot", 0, IntSyncValue.class), -10);
        assertEquals(0, machine.mTargetSlots[0]);
        sendInt(fixture.panelSync.findSyncHandler("targetSlot", 8, IntSyncValue.class), 1000);
        assertEquals(99, machine.mTargetSlots[8]);
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        GT_MetaTileEntity_Regulator restored = (GT_MetaTileEntity_Regulator) machine.newMetaEntity(null);
        restored.loadNBTData(saved);
        assertEquals(9, restored.mTargetSlots.length);
        for (int index = 0; index < 9; index++) {
            assertEquals(machine.mTargetSlots[index], restored.mTargetSlots[index]);
        }
    }

    private static void checkDistributor() throws Exception {
        GT_MetaTileEntity_ItemDistributor machine =
            (GT_MetaTileEntity_ItemDistributor) createMachine("ItemDistributor");
        Fixture fixture = new Fixture(machine);
        assertNotNull(fixture.panelSync.findPanelHandler("sideConfigPanel"));
        fixture.panelSync.findPanelHandler("sideConfigPanel").openPanel();
        assertTrue(fixture.sync.isOpen("sideConfigPanel"));
        PanelSyncManager configSync = fixture.sync.getPanelSyncManager("sideConfigPanel");
        for (int index = 0; index < 6; index++) {
            ByteSyncValue value = configSync.findSyncHandler(
                ModularSyncManager.AUTO_SYNC_PREFIX + "sideConfigPanel", index, ByteSyncValue.class);
            assertTrue(value.isAllowC2S());
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeByte(127 - index);
                value.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
            assertEquals(127 - index, machine.getItemsPerSide(index));
        }
        machine.setItemsPerSide(0, (byte) -1);
        assertEquals(127, machine.getItemsPerSide(0));
        machine.onScrewdriverRightClick(
            (byte) 0,
            fixture.player,
            0,
            0,
            0
        );
        assertEquals(0, machine.getItemsPerSide(0));
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        GT_MetaTileEntity_ItemDistributor restored = (GT_MetaTileEntity_ItemDistributor) machine.newMetaEntity(null);
        restored.loadNBTData(saved);
        for (int index = 0; index < 6; index++) {
            assertEquals(machine.getItemsPerSide(index), saved.getByteArray("mItemsPerSide")[index]);
            assertEquals(machine.getItemsPerSide(index), restored.getItemsPerSide(index));
        }
        assertTrue(fixture.tile.dirtyCalls >= 6);
    }

    private static void checkTypeFilter() throws Exception {
        OrePrefixes.ore.mPrefixedItems.add(new ItemStack(Items.coal));
        OrePrefixes.dust.mPrefixedItems.add(new ItemStack(Items.redstone));
        GT_MetaTileEntity_TypeFilter machine = (GT_MetaTileEntity_TypeFilter) createMachine("TypeFilter");
        Fixture fixture = new Fixture(machine);
        GT_MetaTileEntity_TypeFilter legacy = (GT_MetaTileEntity_TypeFilter) createMachine("TypeFilter");
        new GT_StructureIntegrationTest.TestTile(fixture.world, legacy, ForgeDirection.NORTH);
        ItemStack cursor = new ItemStack(Items.stick, 24);
        fixture.player.inventory.setItemStack(cursor);
        PhantomItemSlot icon = fixture.phantomSlots().get(0);
        assertEquals(9, icon.getSlot().getSlotIndex());
        legacy.clickTypeIcon(false);
        click(icon, 0, false);
        assertSame(legacy.mPrefix, machine.mPrefix);
        assertEquals(-1, machine.mRotationIndex);
        legacy.clickTypeIcon(true);
        click(icon, 1, false);
        assertSame(legacy.mPrefix, machine.mPrefix);
        assertSame(cursor, fixture.player.inventory.getItemStack());
        assertEquals(24, cursor.stackSize);
        setBoolean(fixture.panelSync, "invertFilter", true);
        setBoolean(fixture.panelSync, "allowNbt", true);
        assertTrue(machine.bInvertFilter);
        assertTrue(machine.bNBTAllowed);
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        GT_MetaTileEntity_TypeFilter restored = (GT_MetaTileEntity_TypeFilter) machine.newMetaEntity(null);
        restored.loadNBTData(saved);
        assertSame(machine.mPrefix, restored.mPrefix);
        assertTrue(restored.bInvertFilter);
        assertTrue(restored.bNBTAllowed);
    }

    private static GT_MetaTileEntity_Buffer createMachine(String family) {
        if ("ChestBuffer".equals(family)) {
            return new GT_MetaTileEntity_ChestBuffer(
                "mui2_chest_buffer",
                1,
                28,
                "",
                null
            );
        }
        if ("Filter".equals(family)) {
            return new GT_MetaTileEntity_Filter(
                "mui2_filter",
                1,
                19,
                "",
                null
            );
        }
        if ("ItemDistributor".equals(family)) {
            return new GT_MetaTileEntity_ItemDistributor(
                "mui2_distributor",
                1,
                28,
                "",
                null
            );
        }
        if ("Regulator".equals(family)) {
            return new GT_MetaTileEntity_Regulator(
                "mui2_regulator",
                1,
                20,
                "",
                null
            );
        }
        if ("SuperBuffer".equals(family)) {
            return new GT_MetaTileEntity_SuperBuffer(
                "mui2_super_buffer",
                1,
                257,
                "",
                null
            );
        }
        if ("TypeFilter".equals(family)) {
            return new GT_MetaTileEntity_TypeFilter(
                "mui2_type_filter",
                1,
                11,
                "",
                null
            );
        }
        throw new AssertionError(family);
    }

    private static void setBoolean(PanelSyncManager syncManager, String key, boolean value) throws Exception {
        BooleanSyncValue handler = syncManager.findSyncHandler(key, BooleanSyncValue.class);
        assertTrue(handler.isAllowC2S());
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeBoolean(value);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void sendInt(IntSyncValue handler, int value) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeVarIntToBuffer(value);
            handler.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
        } finally {
            packet.release();
        }
    }

    private static void click(PhantomItemSlot slot, int button, boolean shift) throws Exception {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            new MouseData(
                Side.SERVER,
                button,
                shift,
                false,
                false
            ).writeToPacket(packet);
            slot.getSyncHandler().readOnServer(PhantomItemSlotSH.SYNC_CLICK, packet);
        } finally {
            packet.release();
        }
    }

    private static int countToggles(IWidget widget) {
        int count = widget instanceof ToggleButton ? 1 : 0;
        for (IWidget child : widget.getChildren()) {
            count += countToggles(child);
        }
        return count;
    }

    private static void collectPhantoms(IWidget widget, List<PhantomItemSlot> slots) {
        if (widget instanceof PhantomItemSlot) {
            slots.add((PhantomItemSlot) widget);
        }
        for (IWidget child : widget.getChildren()) {
            collectPhantoms(child, slots);
        }
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_Buffer machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final DirtyTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PosGuiData data;
        private final ModularSyncManager sync;
        private final PanelSyncManager panelSync;
        private final ModularPanel panel;
        private final ModularContainer container;

        private Fixture(GT_MetaTileEntity_Buffer machine) throws Exception {
            this.machine = machine;
            tile = new DirtyTile(world, machine);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 41L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            data = new PosGuiData(player, 4, 64, 7);
            sync = new ModularSyncManager(false);
            panelSync = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, panelSync, settings);
            WidgetTree.collectSyncValues(panelSync, panel);
            container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            assertTrue(panelSync.isInitialised());
        }

        private List<PhantomItemSlot> phantomSlots() {
            List<PhantomItemSlot> phantoms = new ArrayList<>();
            collectPhantoms(panel, phantoms);
            return phantoms;
        }
    }

    private static final class DirtyTile extends GT_StructureIntegrationTest.TestTile {

        private int dirtyCalls;

        private DirtyTile(GT_ProcessingTestSupport.ProcessingWorld world, GT_MetaTileEntity_Buffer machine) {
            super(world, machine, ForgeDirection.NORTH);
        }

        @Override
        public void markDirty() {
            dirtyCalls++;
        }
    }
}
