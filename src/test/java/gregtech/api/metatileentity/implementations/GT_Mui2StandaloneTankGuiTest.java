package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.DynamicKey;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.gui.GT_Container_BasicTank;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Pump;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_WaterGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
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

public class GT_Mui2StandaloneTankGuiTest {

    @Test
    public void pumpKeepsItsContainerLayoutNativeInventoryFluidAndResumeState() throws Throwable {
        run("pump");
    }

    @Test
    public void waterGeneratorKeepsItsContainerAndReadOnlyTankPresentation() throws Throwable {
        run("water");
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
            assertSame(MetaTileEntityGuiHandler.INSTANCE, GuiManager.getFactory("gregtech:mte"));
            if ("pump".equals(scenario)) {
                GT_MetaTileEntity_Pump pump = new GT_MetaTileEntity_Pump("mui2_pump", 2, "", null);
                pump.mPumpList.add(new ChunkPosition(5, 63, 7));
                pump.mPrimaryPumpedBlock = Blocks.water;
                pump.mSecondaryPumpedBlock = Blocks.flowing_water;
                pump.mPumpTimer = 47;
                pump.mPumpCountBelow = 3;
                Fixture fixture = new Fixture(pump);
                fixture.checkWindow();
                assertEquals(1, pump.mPumpList.size());
                assertFalse(pump.wasPumping);
                assertEquals(47, pump.mPumpTimer);
                assertEquals(3, pump.mPumpCountBelow);
                checkPumpPersistence(fixture, pump);
            } else if ("water".equals(scenario)) {
                Fixture fixture = new Fixture(new GT_MetaTileEntity_WaterGenerator("mui2_water", 2, "", null));
                fixture.checkWindow();
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkPumpPersistence(Fixture fixture, GT_MetaTileEntity_Pump pump) {
        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_pump_test");
        IMetaTileEntity previousPrototype = GregTech_API.METATILEENTITIES[32000];
        GregTech_API.METATILEENTITIES[32000] = pump;
        fixture.tile.setMetaTileID((short) 32000);
        try {
            NBTTagCompound saved = new NBTTagCompound();
            fixture.tile.writeToNBT(saved);
            BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
            restoredTile.setWorldObj(fixture.world);
            restoredTile.readFromNBT(saved);
            GT_MetaTileEntity_Pump restored = (GT_MetaTileEntity_Pump) restoredTile.getMetaTileEntity();
            assertNotNull(restored);
            assertTrue(restored.useMui2());
            assertEquals(3, restored.mInventory.length);
            assertSame(Items.paper, restored.mInventory[0].getItem());
            assertEquals(5, restored.mInventory[0].stackSize);
            assertNull(restored.mInventory[1]);
            assertSame(Items.book, restored.mInventory[2].getItem());
            assertEquals(3, restored.mInventory[2].stackSize);
            assertTrue(restored.mFluid.isFluidEqual(pump.mFluid));
            assertEquals(pump.mFluid.amount, restored.mFluid.amount);
            assertEquals(pump.getCapacity(), restored.getCapacity());
            assertTrue(restored.wasPumping);
            assertTrue(restored.mPumpList.isEmpty());
            assertSame(Blocks.water, restored.mPrimaryPumpedBlock);
            assertSame(Blocks.flowing_water, restored.mSecondaryPumpedBlock);
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previousPrototype;
        }
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_BasicTank machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final NoLegacyGuiPlayer player;
        private final PanelSyncManager handlers;
        private final ModularContainer container;
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(GT_MetaTileEntity_BasicTank machine) throws Exception {
            this.machine = machine;
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new NoLegacyGuiPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
            machine.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("mui2_standalone_tank"), 1234);
            machine.mInventory[0] = new ItemStack(Items.stick, 2);
            machine.mInventory[1] = new ItemStack(Items.bucket, 4);
            machine.mInventory[2] = new ItemStack(Items.book, 3);
            assertTrue(machine.useMui2());
            ModularSyncManager sync = new ModularSyncManager(false);
            handlers = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            PosGuiData data = new PosGuiData(player, 4, 64, 7);
            assertSame(machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(data));
            ModularPanel panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
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
            assertNull(handlers.findSyncHandlerNullable("powerSwitch", 0));
            assertNull(handlers.findPanelHandlerNullable("ghostCircuitPanel"));
            collect(panel);
        }

        private void checkWindow() {
            GT_Container_BasicTank legacy = (GT_Container_BasicTank) machine.getServerGUI(0, player.inventory, tile);
            assertEquals(39, legacy.inventorySlots.size());
            assertEquals(38, container.inventorySlots.size());
            ModularSlot input = machineSlot(0);
            ModularSlot output = machineSlot(1);
            assertTrue(input.isCanPut());
            assertTrue(input.isCanTake());
            assertFalse(output.isCanPut());
            assertTrue(output.isCanTake());
            assertNull(machineSlot(2));
            for (int index = 0; index < 2; index++) {
                Slot old = (Slot) legacy.inventorySlots.get(index);
                ModularSlot slot = machineSlot(index);
                assertSame(old.getStack(), slot.getStack());
                assertEquals(old.getSlotStackLimit(), slot.getSlotStackLimit());
                assertEquals(old.isItemValid(new ItemStack(Items.paper)), slot.isItemValid(new ItemStack(Items.paper)));
                checkPosition(slotWidget(index), old.xDisplayPosition - 1, old.yDisplayPosition - 1);
            }

            FluidSlot display = null;
            for (IWidget widget : widgets) {
                if (widget instanceof FluidSlot) {
                    assertNull(display);
                    display = (FluidSlot) widget;
                }
            }
            assertNotNull(display);
            checkPosition(display, 58, 41);
            assertSame(machine.mFluid, display.getFluidTank().getFluid());
            assertEquals(machine.getCapacity(), display.getFluidTank().getCapacity());
            assertEquals(0, display.getFluidTank().fill(machine.mFluid.copy(), true));
            assertNull(display.getFluidTank().drain(1000, true));
            assertFalse(display.getSyncHandler().canFillSlot());
            assertFalse(display.getSyncHandler().canDrainSlot());
            assertFalse(display.getSyncHandler().controlsAmount());
            ItemStack displayMarker = machine.mInventory[2];
            for (int amount : new int[]{0, 123456, Integer.MAX_VALUE}) {
                machine.mFluid.amount = amount;
                checkCounter(amount);
                assertSame(machine.mFluid, display.getFluidTank().getFluid());
                assertSame(displayMarker, machine.mInventory[2]);
            }
            machine.mFluid = null;
            checkCounter(0);
            assertNull(display.getFluidTank().getFluid());
            machine.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("mui2_standalone_tank"), 5432);
            checkCounter(5432);
            assertSame(machine.mFluid, display.getFluidTank().getFluid());

            ItemStack replacement = new ItemStack(Items.apple, 3);
            input.putStack(replacement);
            assertSame(replacement, machine.mInventory[0]);
            assertSame(replacement, ((Slot) legacy.inventorySlots.get(0)).getStack());
            assertTrue(tile.hasInventoryBeenModified());
            input.putStack(null);
            player.inventory.mainInventory[9] = new ItemStack(Items.paper, 5);
            assertNotNull(container.transferStackInSlot(player, playerSlot(9).slotNumber));
            assertNull(player.inventory.mainInventory[9]);
            assertSame(Items.paper, machine.mInventory[0].getItem());
            assertEquals(5, machine.mInventory[0].stackSize);
            assertNotNull(container.transferStackInSlot(player, output.slotNumber));
            assertNull(machine.mInventory[1]);
            assertSame(displayMarker, machine.mInventory[2]);
            assertEquals(5432, machine.mFluid.amount);
            checkRightClicks();
        }

        private void checkCounter(int expected) {
            IntSyncValue amount = handlers.findSyncHandler("fluidAmount", IntSyncValue.class);
            amount.updateCacheFromSource(false);
            assertEquals(expected, amount.getIntValue());
            boolean foundCounter = false;
            for (IWidget widget : widgets) {
                if (widget instanceof TextWidget && ((TextWidget<?>) widget).getKey() instanceof DynamicKey) {
                    String visible = EnumChatFormatting.getTextWithoutFormattingCodes(
                        ((TextWidget<?>) widget).getKey().getFormatted()
                    );
                    foundCounter |= GT_Utility.parseNumberToString(expected).equals(visible);
                }
            }
            assertTrue(foundCounter);
        }

        private void checkRightClicks() {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            player.inventory.setItemStack(null);
            assertNull(player.getCurrentEquippedItem());
            for (byte side = 0; side < 6; side++) {
                assertTrue(tile.onRightclick(
                    player,
                    side,
                    0.5F,
                    0.5F,
                    0.5F
                ));
            }
            world.isRemote = true;
            try {
                assertTrue(machine.onRightclick(tile, player));
                assertTrue(GTGuis.open(player, machine));
            } finally {
                world.isRemote = false;
            }
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private ModularSlot machineSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getSlotIndex() == index && ("input".equals(slot.getSlotGroupName())
                    || "output".equals(slot.getSlotGroupName()))) {
                    return slot;
                }
            }
            return null;
        }

        private ItemSlot slotWidget(int index) {
            for (IWidget widget : widgets) {
                if (widget instanceof ItemSlot && ((ItemSlot) widget).isSynced()
                    && ((ItemSlot) widget).getSlot() == machineSlot(index)) {
                    return (ItemSlot) widget;
                }
            }
            throw new AssertionError("Missing machine slot " + index);
        }

        private ModularSlot playerSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if ("player_inventory".equals(slot.getSlotGroupName()) && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            throw new AssertionError("Missing player slot " + index);
        }

        private void checkPosition(IWidget widget, int x, int y) {
            Viewport viewport = new Viewport();
            ResizeNode resizer = widget.resizer();
            resizer.initialize(viewport, viewport);
            resizer.initResizing(false);
            assertTrue(resizer.resize(false));
            resizer.preApplyPos();
            assertEquals(x, widget.getArea().x);
            assertEquals(y + CommonWidgets.MACHINE_HEADER_HEIGHT, widget.getArea().y);
            assertEquals(18, widget.getArea().width);
            assertEquals(18, widget.getArea().height);
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
            return "Standalone tank legacy layout";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }

    private static final class NoLegacyGuiPlayer extends GT_StructureIntegrationTest.TestPlayer {

        private NoLegacyGuiPlayer(World world) {
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
            throw new AssertionError("A migrated standalone tank attempted to open its legacy GUI");
        }
    }
}
