package gregtech.api.metatileentity.implementations;

import cpw.mods.fml.server.FMLServerHandler;
import java.lang.reflect.Proxy;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.conditions.RedstoneCondition;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;
import gregtech.common.gui.modularui.util.MachineFluidTank;
import gregtech.common.modularui2.widget.GTFluidSlot;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2BasicMachineTest {

    @Test
    public void fluidSlotsUseLiveMachineStorageAndConserveFluids() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "tanks", "");
        }
    }

    @Test
    public void sharedTanksStaySharedAndIdleProgressIsFinite() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "shared", "");
        }
    }

    @Test
    public void nativeControllerCoverPreservesLegacyRedstoneAndRemovalBehavior() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "cover", "");
        }
    }

    @Test
    public void serverBuildInitializesThemesAndRealMachineAndCoverContainers() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "build", "");
        }
    }

    public static void runScenario(String scenario, String model) {
        if ("build".equals(scenario)) {
            Thread serverThread = Thread.currentThread();
            String previousName = serverThread.getName();
            serverThread.setName("Server thread");
            try {
                checkServerBuild();
            } finally {
                serverThread.setName(previousName);
            }
            return;
        }
        if ("cover".equals(scenario)) {
            checkControllerCover();
            return;
        }
        Fluid water = GT_ProcessingTestSupport.fluid("mui2_test_water");
        Fluid oil = GT_ProcessingTestSupport.fluid("mui2_test_oil");
        if ("tanks".equals(scenario)) {
            TestMachine machine = new TestMachine(false);
            MachineFluidTank input = new MachineFluidTank(machine, false);
            MachineFluidTank output = new MachineFluidTank(machine, true);
            FluidStack offered = new FluidStack(water, 1500);

            assertEquals(1000, input.fill(offered, false));
            assertNull(machine.getFillableStack());
            assertEquals(1000, input.fill(offered, true));
            assertEquals(1500, offered.amount);
            assertEquals(1000, machine.getFillableStack().amount);
            assertNull(machine.getDrainableStack());
            assertEquals(0, input.fill(new FluidStack(oil, 10), true));
            assertEquals(0, input.fill(new FluidStack(water, 10), true));

            assertEquals(250, input.drain(250, false).amount);
            assertEquals(1000, input.getFluidAmount());
            FluidStack transferred = input.drain(250, true);
            assertEquals(250, output.fill(transferred, true));
            assertEquals(1000, input.getFluidAmount() + output.getFluidAmount());
            assertEquals(750, machine.getFillableStack().amount);
            assertEquals(250, machine.getDrainableStack().amount);

            machine.setFillableStack(new FluidStack(oil, 700));
            assertSame(machine.getFillableStack(), input.getFluid());
            assertEquals(700, input.drain(Integer.MAX_VALUE, true).amount);
            assertNull(machine.getFillableStack());
            assertNull(input.drain(1, true));
        } else {
            TestMachine machine = new TestMachine(true);
            MachineFluidTank input = new MachineFluidTank(machine, false);
            MachineFluidTank output = new MachineFluidTank(machine, true);
            assertEquals(600, input.fill(new FluidStack(water, 600), true));
            assertSame(input.getFluid(), output.getFluid());
            assertEquals(200, output.drain(200, true).amount);
            assertEquals(400, input.getFluidAmount());
            assertEquals(400, output.getFluidAmount());

            InspectableGui gui = new InspectableGui(machine);
            assertFalse(Double.isNaN(gui.progress()));
            assertEquals(0, gui.progress(), 0);
            machine.mMaxProgresstime = 100;
            machine.mProgresstime = 50;
            assertEquals(0.5, gui.progress(), 0);
            machine.mProgresstime = -100;
            assertEquals(0, gui.progress(), 0);
            machine.mProgresstime = 200;
            assertEquals(1, gui.progress(), 0);
            assertFalse(machine.useMui2());
        }
    }

    private static void checkServerBuild() {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        assertFalse(world.isRemote);
        GTGuis.init();
        assertTrue(GuiManager.hasFactory("gregtech:mte"));
        assertTrue(GuiManager.hasFactory("gregtech:cover"));
        assertFalse(IThemeApi.get().getJavaDefaultThemes(GTGuiThemes.STANDARD.getId()).isEmpty());
        assertFalse(IThemeApi.get().getJavaDefaultThemes(GTGuiThemes.COVER.getId()).isEmpty());

        TestMachine machine = new TestMachine(false);
        ItemStack firstInputStack = new ItemStack(Items.apple, 2);
        ItemStack secondInputStack = new ItemStack(Items.stick, 3);
        machine.mInventory[machine.getInputSlot()] = firstInputStack;
        machine.mInventory[machine.getInputSlot() + 1] = secondInputStack;
        GT_StructureIntegrationTest.TestTile tile = new GT_StructureIntegrationTest.TestTile(
            world,
            machine,
            ForgeDirection.NORTH
        );
        world.putTile(4, 64, 7, tile);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        player.setPosition(4.5, 64.5, 7.5);
        CoverControlsWork cover = new CoverControlsWork(
            new CoverContext(new ItemStack(Items.stick), ForgeDirection.WEST, tile)
        );
        tile.attachCover(cover);
        assertTrue(cover.isValid());

        ModularSyncManager machineSync = new ModularSyncManager(false);
        PanelSyncManager machinePanelSync = new PanelSyncManager(machineSync, true);
        UISettings machineSettings = new UISettings(RecipeViewerSettings.DUMMY);
        PosGuiData machineData = new PosGuiData(player, tile.xCoord, tile.yCoord, tile.zCoord);
        ModularPanel machinePanel = machine.buildUI(machineData, machinePanelSync, machineSettings);
        WidgetTree.collectSyncValues(machinePanelSync, machinePanel);
        ModularContainer machineContainer = new ModularContainer();
        machineContainer.construct(
            player,
            machineSync,
            machineSettings,
            machinePanel.getName(),
            machineData
        );
        assertTrue(machinePanelSync.isInitialised());
        assertTrue(machineContainer.isInitialized());
        assertFalse(machineContainer.isClient());
        assertEquals(42, machineContainer.inventorySlots.size());
        assertNotNull(machinePanelSync.findSyncHandler("fluidAutoOutput"));
        assertNotNull(machinePanelSync.findSyncHandler("itemAutoOutput"));
        assertNotNull(machinePanelSync.findSyncHandler("powerSwitch"));
        boolean previousFluidUnits = NumberFormatConfig.useForgeFluidMillibuckets;
        try {
            NumberFormatConfig.useForgeFluidMillibuckets = true;
            assertEquals(2, checkFluidCounters(machinePanel));
        } finally {
            NumberFormatConfig.useForgeFluidMillibuckets = previousFluidUnits;
        }

        int inputSlots = 0;
        int outputSlots = 0;
        ModularSlot firstInput = null;
        for (Object entry : machineContainer.inventorySlots) {
            ModularSlot slot = (ModularSlot) entry;
            if ("item_inv".equals(slot.getSlotGroupName())) {
                if (slot.isCanPut()) {
                    inputSlots++;
                    firstInput = slot;
                } else {
                    outputSlots++;
                }
            }
        }
        assertEquals(2, inputSlots);
        assertEquals(2, outputSlots);
        assertNotNull(firstInput);
        assertSame(machine.getInventoryHandler(), firstInput.getItemHandler());
        assertTrue(firstInput.getStack() == firstInputStack || firstInput.getStack() == secondInputStack);
        int liveIndex = machine.getInputSlot() + (firstInput.getStack() == firstInputStack ? 0 : 1);
        ItemStack replacement = new ItemStack(Items.apple, 8);
        machine.mInventory[liveIndex] = replacement;
        assertSame(replacement, firstInput.getStack());

        CoverGuiData coverData = new CoverGuiData(
            player,
            cover.getCoverID(),
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            ForgeDirection.WEST,
            false
        );
        ModularSyncManager coverSync = new ModularSyncManager(false);
        PanelSyncManager coverPanelSync = new PanelSyncManager(coverSync, true);
        UISettings coverSettings = new UISettings(RecipeViewerSettings.DUMMY);
        ModularPanel coverPanel = cover.buildUI(coverData, coverPanelSync, coverSettings);
        WidgetTree.collectSyncValues(coverPanelSync, coverPanel);
        ModularContainer coverContainer = new ModularContainer();
        coverContainer.construct(
            player,
            coverSync,
            coverSettings,
            coverPanel.getName(),
            coverData
        );
        assertTrue(coverPanelSync.isInitialised());
        assertFalse(coverContainer.isClient());
        assertEquals(36, coverContainer.inventorySlots.size());
        assertNotNull(coverPanelSync.findSyncHandler("condition_mode"));
    }

    private static int checkFluidCounters(IWidget widget) {
        int count = 0;
        if (widget instanceof FluidSlot) {
            assertTrue(widget instanceof GTFluidSlot);
            FluidSlot slot = (FluidSlot) widget;
            assertEquals("L", slot.getBaseUnit());
            assertEquals("L", slot.getUnit());
            assertEquals(24_000, slot.getBaseUnitAmount(24_000), 0);
            count++;
        }
        for (IWidget child : widget.getChildren()) {
            count += checkFluidCounters(child);
        }
        return count;
    }

    private static void checkControllerCover() {
        boolean[] allowed = {true};
        byte[] workData = {0};
        ICoverable tile = (ICoverable) Proxy.newProxyInstance(
            ICoverable.class.getClassLoader(),
            new Class<?>[] {ICoverable.class, IMachineProgress.class},
            (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "enableWorking":
                        allowed[0] = true;
                        return null;
                    case "disableWorking":
                        allowed[0] = false;
                        return null;
                    case "setWorkDataValue":
                        workData[0] = (Byte) arguments[0];
                        return null;
                    case "getWorld":
                        return null;
                    default:
                        throw new AssertionError("Unexpected controller-cover query: " + method);
                }
            }
        );
        CoverControlsWork cover = new CoverControlsWork(new CoverContext(0, ForgeDirection.NORTH, tile), null);
        for (int mode = 0; mode < 3; mode++) {
            cover.setLegacyData(mode);
            assertTrue(cover.isRedstoneSensitive(0));
            for (byte signal : new byte[] {0, 15}) {
                cover.doCoverThings(signal, 0);
                assertEquals(mode != 2 && (signal > 0) == (mode == 0), allowed[0]);
                assertEquals(signal, workData[0]);
            }
        }
        cover.setLegacyData(Integer.MAX_VALUE);
        assertSame(RedstoneCondition.DISABLE, cover.getRedstoneCondition());
        cover.doCoverThings((byte) 15, 0);
        assertFalse(allowed[0]);
        cover.onCoverRemoval();
        assertTrue(allowed[0]);
        assertEquals(0, workData[0]);
    }

    private static final class InspectableGui extends MTEBasicMachineBaseGui<TestMachine> {

        private InspectableGui(TestMachine machine) {
            super(machine, true, true);
        }

        private double progress() {
            return getProgress();
        }
    }

    private static final class TestMachine extends GT_MetaTileEntity_BasicMachine {

        private final boolean shared;

        private TestMachine(boolean shared) {
            super(
                "mui2_test",
                1,
                1,
                "",
                null,
                2,
                2,
                "",
                ""
            );
            this.shared = shared;
        }

        @Override
        public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new TestMachine(shared);
        }

        @Override
        public int getCapacity() {
            return 1000;
        }

        @Override
        public FluidStack getFillableStack() {
            return shared ? getDrainableStack() : super.getFillableStack();
        }

        @Override
        public FluidStack setFillableStack(FluidStack fluid) {
            return shared ? setDrainableStack(fluid) : super.setFillableStack(fluid);
        }
    }
}
