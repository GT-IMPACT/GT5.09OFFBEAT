package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.CoverUIFactory;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GT_Mui2FoundationTest {

    @Test
    public void inventoryKeepsMachineStorageValidationAndSimulation() throws Throwable {
        run("inventory");
    }

    @Test
    public void machineFactoryChecksAccessDistanceAndRemovedTiles() throws Throwable {
        run("access");
    }

    @Test
    public void guiPacketsRoundTripNegativeCoordinatesAndCoverSide() throws Throwable {
        run("packets");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        InventoryMachine machine = new InventoryMachine();
        GT_StructureIntegrationTest.TestTile tile = new GT_StructureIntegrationTest.TestTile(
                world,
                machine,
                ForgeDirection.NORTH
        );
        world.putTile(-21, 64, 37, tile);
        GT_ProcessingTestSupport.setField(tile, "mTickTimer", 41L);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        player.setPosition(-20.5, 64.5, 37.5);
        if ("inventory".equals(scenario)) {
            checkInventory(machine, tile, world);
        } else if ("access".equals(scenario)) {
            checkAccess(machine, tile, player, world);
        } else if ("packets".equals(scenario)) {
            checkPackets(player);
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkInventory(
            InventoryMachine machine,
            GT_StructureIntegrationTest.TestTile tile,
            GT_ProcessingTestSupport.ProcessingWorld world
    ) {
        ItemStackHandler inventory = machine.getInventoryHandler();
        ItemStack offered = new ItemStack(Items.apple, 20);
        assertEquals(4, inventory.insertItem(0, offered, true).stackSize);
        assertNull(machine.mInventory[0]);
        assertEquals(4, inventory.insertItem(0, offered, false).stackSize);
        assertEquals(16, machine.mInventory[0].stackSize);
        assertEquals(20, offered.stackSize);
        assertSame(machine.mInventory[0], inventory.getStackInSlot(0));
        assertTrue(tile.hasInventoryBeenModified());
        assertTrue(world.getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified);

        ItemStack rejected = new ItemStack(Items.stick);
        assertSame(rejected, inventory.insertItem(1, rejected, false));
        assertNull(machine.mInventory[1]);
        assertNull(inventory.extractItem(0, -1, false));
        assertEquals(16, machine.mInventory[0].stackSize);
        assertEquals(8, inventory.extractItem(0, 8, true).stackSize);
        assertEquals(16, machine.mInventory[0].stackSize);

        NBTTagCompound saved = inventory.serializeNBT();
        inventory.extractItem(0, 8, false);
        assertEquals(8, machine.mInventory[0].stackSize);
        inventory.deserializeNBT(saved);
        assertSame(machine.mInventory[0], inventory.getStackInSlot(0));
        assertEquals(16, machine.mInventory[0].stackSize);
        machine.mInventory[0] = new ItemStack(Items.apple, 3);
        assertEquals(3, inventory.getStackInSlot(0).stackSize);
        assertThrows(IllegalArgumentException.class, () -> inventory.setSize(inventory.getSlots() + 1));
    }

    private static void checkAccess(
            InventoryMachine machine,
            GT_StructureIntegrationTest.TestTile tile,
            GT_StructureIntegrationTest.TestPlayer player,
            GT_ProcessingTestSupport.ProcessingWorld world
    ) {
        PosGuiData data = new PosGuiData(player, tile.xCoord, tile.yCoord, tile.zCoord);
        MetaTileEntityGuiHandler factory = MetaTileEntityGuiHandler.INSTANCE;
        assertSame(machine, factory.getGuiHolder(data));
        assertTrue(factory.canInteractWith(player, data));
        machine.accessAllowed = false;
        assertFalse(factory.canInteractWith(player, data));
        machine.accessAllowed = true;
        player.setPosition(tile.xCoord + 8.5, tile.yCoord + 0.5, tile.zCoord + 0.5);
        assertFalse(factory.canInteractWith(player, data));
        player.setPosition(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5);
        assertTrue(factory.canInteractWith(player, data));
        world.tiles.clear();
        assertFalse(factory.canInteractWith(player, data));
        assertThrows(IllegalStateException.class, () -> factory.getGuiHolder(data));
        assertFalse(new GT_MetaTileEntity_Hatch_InputBus("legacy", 1, "", null).useMui2());
    }

    private static void checkPackets(GT_StructureIntegrationTest.TestPlayer player) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            PosGuiData machineData = new PosGuiData(player, -30000000, 255, 29999999);
            MetaTileEntityGuiHandler.INSTANCE.writeGuiData(machineData, buffer);
            PosGuiData machineCopy = MetaTileEntityGuiHandler.INSTANCE.readGuiData(player, buffer);
            assertEquals(machineData.getX(), machineCopy.getX());
            assertEquals(machineData.getY(), machineCopy.getY());
            assertEquals(machineData.getZ(), machineCopy.getZ());
            assertEquals(0, buffer.readableBytes());
            CoverGuiData coverData = new CoverGuiData(
                    player,
                    0x7fff0042,
                    -2048,
                    128,
                    1048576,
                    ForgeDirection.WEST,
                    false
            );
            CoverUIFactory.INSTANCE.writeGuiData(coverData, buffer);
            CoverGuiData coverCopy = CoverUIFactory.INSTANCE.readGuiData(player, buffer);
            assertEquals(coverData.getCoverID(), coverCopy.getCoverID());
            assertEquals(coverData.getX(), coverCopy.getX());
            assertEquals(coverData.getY(), coverCopy.getY());
            assertEquals(coverData.getZ(), coverCopy.getZ());
            assertSame(ForgeDirection.WEST, coverCopy.getSide());
            assertFalse(coverCopy.isPopUp());
            assertEquals(0, buffer.readableBytes());
            assertNotNull(coverCopy.getPlayer());
        } finally {
            buffer.release();
        }
    }

    private static final class InventoryMachine extends GT_MetaTileEntity_Hatch_InputBus {

        private boolean accessAllowed = true;

        private InventoryMachine() {
            super("mui2_inventory", 1, "", null);
        }

        @Override
        public int getInventoryStackLimit() {
            return 16;
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return stack != null && stack.getItem() == Items.apple;
        }

        @Override
        public boolean isAccessAllowed(net.minecraft.entity.player.EntityPlayer player) {
            return accessAllowed;
        }

        @Override
        public boolean useMui2() {
            return true;
        }
    }
}
