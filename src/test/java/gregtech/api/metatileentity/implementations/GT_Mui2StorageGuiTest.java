package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.DynamicKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_DigitalChestBase;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_PortableTank;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_QuantumChest;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_QuantumTank;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_SuperChest;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_SuperTank;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
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

public class GT_Mui2StorageGuiTest {

    @Test
    public void allStorageTanksBindNativeSlotsModesAndLiveFluidStorage() throws Throwable {
        run("tanks");
    }

    @Test
    public void storageTankModePacketsPreserveLockingVoidingAndAutomaticOutput() throws Throwable {
        run("modes");
    }

    @Test
    public void bothDigitalChestsKeepTheirCounterSlotsAndReadOnlyPicture() throws Throwable {
        run("chests");
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
            if ("tanks".equals(scenario) || "modes".equals(scenario)) {
                for (int kind = 0; kind < 3; kind++) {
                    GT_MetaTileEntity_StorageTank tank = tank(kind);
                    Fixture fixture = new Fixture(tank);
                    if ("tanks".equals(scenario)) {
                        checkTankInventory(fixture, tank);
                    } else {
                        checkTankModes(fixture, tank);
                    }
                }
            } else if ("chests".equals(scenario)) {
                checkChest(new GT_MetaTileEntity_SuperChest("mui2_super_chest", 1, "", null));
                checkChest(new GT_MetaTileEntity_QuantumChest("mui2_quantum_chest", 6, "", null));
            } else {
                throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static GT_MetaTileEntity_StorageTank tank(int kind) {
        if (kind == 0) {
            return new GT_MetaTileEntity_PortableTank("mui2_portable_tank", 1, "", null);
        }
        if (kind == 1) {
            return new GT_MetaTileEntity_SuperTank("mui2_super_tank", 1, "", null);
        }
        return new GT_MetaTileEntity_QuantumTank("mui2_quantum_tank", 6, "", null);
    }

    private static void checkTankInventory(Fixture fixture, GT_MetaTileEntity_StorageTank tank) {
        assertEquals(38, fixture.container.inventorySlots.size());
        ModularSlot input = fixture.machineSlot(0);
        ModularSlot output = fixture.machineSlot(1);
        assertTrue(input.isCanPut());
        assertTrue(input.isCanTake());
        assertFalse(output.isCanPut());
        assertTrue(output.isCanTake());
        assertNull(fixture.findMachineSlot(2));
        ItemStack inputStack = new ItemStack(Items.bucket, 3);
        ItemStack display = new ItemStack(Items.book);
        tank.mInventory[0] = inputStack;
        tank.mInventory[2] = display;
        assertSame(inputStack, input.getStack());
        input.putStack(new ItemStack(Items.bucket, 5));
        assertEquals(5, tank.mInventory[0].stackSize);
        assertSame(display, tank.mInventory[2]);

        FluidSlotSyncHandler fluid = fixture.editableFluid();
        Fluid water = GT_ProcessingTestSupport.fluid("mui2_storage_water");
        tank.mFluid = new FluidStack(water, 1234);
        assertSame(tank.mFluid, fluid.getFluidTank().getFluid());
        assertEquals(234, fluid.getFluidTank().drain(234, true).amount);
        assertEquals(1000, tank.getFluidAmount());
        assertEquals(700, fluid.getFluidTank().fill(new FluidStack(water, 700), true));
        assertEquals(1700, tank.getFluidAmount());
        assertTrue(fixture.container.canInteractWith(fixture.player));
        fixture.player.setPosition(100, 64, 100);
        assertFalse(fixture.container.canInteractWith(fixture.player));
    }

    private static void checkTankModes(Fixture fixture, GT_MetaTileEntity_StorageTank tank) throws Exception {
        Fluid water = GT_ProcessingTestSupport.fluid("mui2_mode_water");
        Fluid oil = GT_ProcessingTestSupport.fluid("mui2_mode_oil");
        String[] modes = {"fluidAutoOutput", "fluidLock", "voidOverflow", "voidAll"};
        for (String mode : modes) {
            assertTrue(fixture.handlers.findSyncHandler(mode, BooleanSyncValue.class).isAllowC2S());
            fixture.setMode(mode, true);
        }
        assertTrue(tank.OutputFluid);
        assertTrue(tank.mMode);
        assertTrue(tank.mVoidFluidPart);
        assertTrue(tank.mVoidFluidFull);
        assertNull(tank.getLockedFluidName());
        FluidSlotSyncHandler fluid = fixture.editableFluid();
        fluid.setValue(new FluidStack(water, 1000), true, false);
        assertTrue(water.getUnlocalizedName().equals(tank.getLockedFluidName()));
        assertTrue(fluid.getFilter().test(new FluidStack(water, 1)));
        assertFalse(fluid.getFilter().test(new FluidStack(oil, 1)));
        assertEquals(0, tank.fill(new FluidStack(oil, 100), true));
        StringSyncValue locked = fixture.handlers.findSyncHandler("lockedFluid", StringSyncValue.class);
        assertFalse(locked.isAllowC2S());
        locked.updateCacheFromSource(false);
        assertTrue(water.getUnlocalizedName().equals(locked.getStringValue()));

        NBTTagCompound saved = new NBTTagCompound();
        tank.saveNBTData(saved);
        assertTrue(saved.getBoolean("OutputFluid"));
        assertTrue(saved.getBoolean("mMode"));
        assertTrue(saved.getBoolean("mVoidFluidPart"));
        assertTrue(saved.getBoolean("mVoidFluidFull"));
        tank.OutputFluid = tank.mMode = tank.mVoidFluidPart = tank.mVoidFluidFull = false;
        tank.setLockedFluidName(null);
        tank.loadNBTData(saved);
        assertTrue(tank.OutputFluid && tank.mMode && tank.mVoidFluidPart && tank.mVoidFluidFull);
        assertTrue(water.getUnlocalizedName().equals(tank.getLockedFluidName()));
        fixture.setMode("fluidLock", false);
        assertTrue(fluid.getFilter().test(new FluidStack(oil, 1)));
        tank.mFluid = new FluidStack(oil, 1000);
        fixture.setMode("fluidLock", true);
        assertTrue(oil.getUnlocalizedName().equals(tank.getLockedFluidName()));

        int capacity = tank.getCapacity();
        tank.mFluid = new FluidStack(oil, capacity);
        tank.onPostTick(fixture.tile, 8);
        assertEquals(capacity - capacity * 25L / 100, tank.getFluidAmount());
        assertFalse(tank.mVoidFluidFull);
        fixture.setMode("voidOverflow", false);
        fixture.setMode("voidAll", true);
        tank.onPostTick(fixture.tile, 16);
        assertNull(tank.mFluid);
        assertFalse(tank.mVoidFluidPart);

        fixture.setMode("voidAll", false);
        GT_MetaTileEntity_SuperTank receiver = new GT_MetaTileEntity_SuperTank("mui2_receiver", 1, "", null);
        GT_StructureIntegrationTest.TestTile receiverTile = new GT_StructureIntegrationTest.TestTile(
            fixture.world,
            receiver,
            ForgeDirection.NORTH
        );
        fixture.world.putTile(4, 64, 6, receiverTile);
        GT_ProcessingTestSupport.setField(receiverTile, "mTickTimer", 51L);
        fixture.tile.onAdjacentBlockChange(4, 64, 6);
        assertSame(receiverTile, fixture.tile.getITankContainerAtSide(fixture.tile.getFrontFacing()));
        tank.mFluid = new FluidStack(oil, 2000);
        fixture.setMode("fluidAutoOutput", false);
        tank.onPostTick(fixture.tile, 24);
        assertEquals(2000, tank.getFluidAmount());
        assertEquals(0, receiver.getFluidAmount());
        fixture.setMode("fluidAutoOutput", true);
        tank.onPostTick(fixture.tile, 32);
        assertEquals(0, tank.getFluidAmount());
        assertEquals(2000, receiver.getFluidAmount());
    }

    private static void checkChest(GT_MetaTileEntity_DigitalChestBase chest) throws Exception {
        Fixture fixture = new Fixture(chest);
        assertEquals(38, fixture.container.inventorySlots.size());
        ModularSlot input = fixture.machineSlot(0);
        ModularSlot output = fixture.machineSlot(1);
        ModularSlot picture = fixture.machineSlot(2);
        assertFalse(fixture.container.inventorySlots.contains(picture));
        assertTrue(input.isCanPut() && input.isCanTake());
        assertFalse(output.isCanPut());
        assertTrue(output.isCanTake());
        assertFalse(picture.isCanPut());
        assertFalse(picture.isCanTake());
        checkChestFrame(fixture, 0, 80, 17);
        checkChestFrame(fixture, 1, 80, 53);
        checkChestFrame(fixture, 2, 59, 42);

        chest.setItemCount(123456);
        chest.mInventory[0] = new ItemStack(Items.redstone, 40);
        chest.mInventory[1] = new ItemStack(Items.redstone, 64);
        chest.onPostTick(fixture.tile, 1);
        assertNull(chest.mInventory[0]);
        assertEquals(123496, chest.getItemCount());
        assertEquals(64, output.getStack().stackSize);
        IntSyncValue count = fixture.handlers.findSyncHandler("itemCount", IntSyncValue.class);
        count.updateCacheFromSource(false);
        assertEquals(123496, count.getIntValue());
        boolean foundCounter = false;
        for (IWidget widget : fixture.widgets) {
            if (widget instanceof TextWidget && ((TextWidget<?>) widget).getKey() instanceof DynamicKey) {
                foundCounter |= GT_Utility.parseNumberToString(123496)
                    .equals(EnumChatFormatting.getTextWithoutFormattingCodes(
                        ((TextWidget<?>) widget).getKey().getFormatted()));
            }
        }
        assertTrue(foundCounter);
        ItemStack marker = picture.getStack();
        assertNotNull(marker);
        int pictureAmount = marker.stackSize;
        ItemStack cursor = new ItemStack(Items.redstone, 12);
        fixture.player.inventory.setItemStack(cursor);
        PhantomItemSlot displayWidget = fixture.display();
        for (int mouse = 0; mouse <= 1; mouse++) {
            for (int shift = 0; shift <= 1; shift++) {
                fixture.phantomPacket(displayWidget, PhantomItemSlotSH.SYNC_CLICK, mouse, shift == 1);
                assertSame(marker, chest.mInventory[2]);
                assertEquals(pictureAmount, marker.stackSize);
                assertSame(cursor, fixture.player.inventory.getItemStack());
                assertEquals(12, cursor.stackSize);
            }
        }
        fixture.phantomPacket(displayWidget, PhantomItemSlotSH.SYNC_SCROLL, 1, false);
        picture.putStack(null);
        picture.putStack(new ItemStack(Items.paper));
        assertSame(marker, chest.mInventory[2]);
        assertEquals(pictureAmount, marker.stackSize);
        assertEquals(123496, chest.getItemCount());
        fixture.player.inventory.setItemStack(null);

        ItemStack transferred = new ItemStack(Items.redstone, 5);
        fixture.player.inventory.mainInventory[0] = transferred;
        fixture.container.transferStackInSlot(fixture.player, fixture.playerSlot(0).slotNumber);
        assertEquals(5, input.getStack().stackSize);
        assertNull(fixture.player.inventory.mainInventory[0]);
        ItemStack replacement = new ItemStack(Items.stick, 9);
        chest.mInventory[0] = replacement;
        assertSame(replacement, input.getStack());
        input.putStack(new ItemStack(Items.paper, 4));
        assertEquals(4, chest.mInventory[0].stackSize);
    }

    private static void checkChestFrame(Fixture fixture, int index, int iconX, int iconY) {
        for (IWidget widget : fixture.widgets) {
            if (widget instanceof ItemSlot && ((ItemSlot) widget).isSynced()
                && ((ItemSlot) widget).getSlot() == fixture.machineSlot(index)) {
                ResizeNode resizer = widget.resizer();
                Viewport viewport = new Viewport();
                resizer.initialize(viewport, viewport);
                resizer.initResizing(false);
                assertTrue(resizer.resize(false));
                resizer.preApplyPos();
                assertEquals(iconX - 1, widget.getArea().x);
                assertEquals(iconY - 1 + CommonWidgets.MACHINE_HEADER_HEIGHT, widget.getArea().y);
                return;
            }
        }
        throw new AssertionError("Missing chest slot " + index);
    }

    private static final class Fixture {

        private final MetaTileEntity machine;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final PanelSyncManager handlers;
        private final ModularContainer container;
        private final List<IWidget> widgets = new ArrayList<>();

        private Fixture(MetaTileEntity machine) throws Exception {
            this.machine = machine;
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
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
            assertTrue(container.canInteractWith(player));
            collect(panel);
        }

        private void collect(IWidget widget) {
            widgets.add(widget);
            for (IWidget child : widget.getChildren()) {
                collect(child);
            }
        }

        private ModularSlot findMachineSlot(int index) {
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                    return slot;
                }
            }
            for (IWidget widget : widgets) {
                if (widget instanceof PhantomItemSlot) {
                    ModularSlot slot = ((PhantomItemSlot) widget).getSyncHandler().getSlot();
                    if (slot.getItemHandler() == machine.getInventoryHandler() && slot.getSlotIndex() == index) {
                        return slot;
                    }
                }
            }
            return null;
        }

        private ModularSlot machineSlot(int index) {
            ModularSlot slot = findMachineSlot(index);
            assertNotNull(slot);
            return slot;
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

        private FluidSlotSyncHandler editableFluid() {
            for (IWidget widget : widgets) {
                if (widget instanceof FluidSlot) {
                    FluidSlotSyncHandler handler = ((FluidSlot) widget).getSyncHandler();
                    if (handler.getChangeListener() != null) {
                        return handler;
                    }
                }
            }
            throw new AssertionError("Missing editable tank widget");
        }

        private PhantomItemSlot display() {
            for (IWidget widget : widgets) {
                if (widget instanceof PhantomItemSlot) {
                    return (PhantomItemSlot) widget;
                }
            }
            throw new AssertionError("Missing read-only picture");
        }

        private void setMode(String name, boolean enabled) throws Exception {
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeBoolean(enabled);
                handlers.findSyncHandler(name, BooleanSyncValue.class)
                    .readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
        }

        private void phantomPacket(PhantomItemSlot slot, int id, int mouse, boolean shift) throws Exception {
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                new MouseData(
                    Side.CLIENT,
                    mouse,
                    shift,
                    false,
                    false
                ).writeToPacket(packet);
                slot.getSyncHandler().readOnServer(id, packet);
            } finally {
                packet.release();
            }
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
            return "Storage legacy layout";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }
}
