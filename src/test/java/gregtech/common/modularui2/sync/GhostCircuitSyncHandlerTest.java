package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GhostCircuitSyncHandlerTest {

    @Test
    public void configurationZeroPersistsAndPhantomInventoryCannotTransferItems() throws Throwable {
        run("storage");
    }

    @Test
    public void cursorSamplingCyclingAndUnboundPacketsPreserveServerSelection() throws Throwable {
        run("interaction");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        GT_IntegratedCircuit_Item circuit = new GT_IntegratedCircuit_Item();
        ItemStackHandler inventory = new ItemStackHandler(4);
        ItemStack physicalInput = new ItemStack(Items.redstone, 5);
        inventory.setStackInSlot(0, physicalInput);
        IMetaTileEntity machine = machine(inventory);
        GhostCircuitItemStackHandler phantom = new GhostCircuitItemStackHandler(machine);
        if ("storage".equals(scenario)) {
            checkStorage(circuit, inventory, phantom);
        } else if ("interaction".equals(scenario)) {
            checkInteraction(circuit, machine, phantom);
        } else {
            throw new AssertionError(scenario);
        }
        assertSame(physicalInput, inventory.getStackInSlot(0));
        assertEquals(5, physicalInput.stackSize);
    }

    private static IMetaTileEntity machine(ItemStackHandler inventory) {
        return (IMetaTileEntity) Proxy.newProxyInstance(
            IMetaTileEntity.class.getClassLoader(),
            new Class<?>[] {IMetaTileEntity.class, IConfigurationCircuitSupport.class},
            (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "getInventoryHandler":
                        return inventory;
                    case "getCircuitSlot":
                        return 2;
                    case "allowSelectCircuit":
                        return true;
                    case "getBaseMetaTileEntity":
                        return null;
                    default:
                        throw new AssertionError("Unexpected phantom-circuit query: " + method);
                }
            }
        );
    }

    private static void checkStorage(
        GT_IntegratedCircuit_Item circuit,
        ItemStackHandler inventory,
        GhostCircuitItemStackHandler phantom
    ) {
        assertEquals(GhostCircuitItemStackHandler.NO_CONFIG, phantom.getCircuitConfig());
        phantom.setCircuitConfig(0);
        assertTrue(phantom.hasCircuit());
        assertEquals(0, phantom.getCircuitConfig());
        assertEquals(0, inventory.getStackInSlot(2).stackSize);
        NBTTagCompound saved = inventory.serializeNBT();
        ItemStackHandler restoredInventory = new ItemStackHandler(4);
        restoredInventory.deserializeNBT(saved);
        GhostCircuitItemStackHandler restored = new GhostCircuitItemStackHandler(machine(restoredInventory));
        assertTrue(restored.hasCircuit());
        assertEquals(0, restored.getCircuitConfig());
        assertEquals(0, restored.getStackInSlot(0).stackSize);

        List<ItemStack> configurations = GhostCircuitItemStackHandler.getAllConfigurations();
        assertEquals(25, configurations.size());
        for (int index = 0; index < configurations.size(); index++) {
            assertSame(circuit, configurations.get(index).getItem());
            assertEquals(index, configurations.get(index).getItemDamage());
            assertEquals(1, configurations.get(index).stackSize);
        }

        ItemStack cursor = new ItemStack(circuit, 12, 7);
        phantom.setStackInSlot(0, cursor);
        assertNotSame(cursor, phantom.getStackInSlot(0));
        assertEquals(12, cursor.stackSize);
        assertEquals(7, phantom.getCircuitConfig());
        assertEquals(0, phantom.getSlotLimit(0));
        assertSame(cursor, phantom.insertItem(0, cursor, true));
        assertSame(cursor, phantom.insertItem(0, cursor, false));
        assertNull(phantom.extractItem(0, Integer.MAX_VALUE, true));
        assertNull(phantom.extractItem(0, Integer.MAX_VALUE, false));
        assertEquals(7, phantom.getCircuitConfig());
        assertFalse(phantom.isItemValid(0, new ItemStack(Items.stick)));
        assertFalse(phantom.isItemValid(0, new ItemStack(circuit, 1, 25)));
        phantom.setStackInSlot(0, new ItemStack(circuit, 1, 25));
        assertEquals(7, phantom.getCircuitConfig());
        assertThrows(IllegalArgumentException.class, () -> phantom.setCircuitConfig(25));
        assertThrows(IllegalArgumentException.class, () -> phantom.setCircuitConfig(-2));
        phantom.setCircuitConfig(GhostCircuitItemStackHandler.NO_CONFIG);
        assertFalse(phantom.hasCircuit());
        assertNull(phantom.getStackInSlot(0));
    }

    private static void checkInteraction(
        GT_IntegratedCircuit_Item circuit,
        IMetaTileEntity machine,
        GhostCircuitItemStackHandler phantom
    ) throws Exception {
        InspectableSync sync = new InspectableSync(new ModularSlot(phantom, 0), machine);
        ItemStack cursor = new ItemStack(circuit, 11, 0);
        sync.click(0, false, cursor);
        assertEquals(0, phantom.getCircuitConfig());
        assertEquals(11, cursor.stackSize);
        assertEquals(0, cursor.getItemDamage());
        assertNotSame(cursor, phantom.getStackInSlot(0));
        assertEquals(0, phantom.getStackInSlot(0).stackSize);
        sync.click(1, true, cursor);
        assertEquals(GhostCircuitItemStackHandler.NO_CONFIG, phantom.getCircuitConfig());
        assertEquals(11, cursor.stackSize);

        sync.click(0, false, null);
        assertEquals(0, phantom.getCircuitConfig());
        sync.click(0, false, null);
        assertEquals(1, phantom.getCircuitConfig());
        sync.click(1, false, null);
        assertEquals(0, phantom.getCircuitConfig());
        sync.click(1, false, null);
        assertEquals(GhostCircuitItemStackHandler.NO_CONFIG, phantom.getCircuitConfig());
        sync.scroll(-1000);
        assertEquals(24, phantom.getCircuitConfig());
        sync.click(0, false, null);
        assertEquals(GhostCircuitItemStackHandler.NO_CONFIG, phantom.getCircuitConfig());
        sync.scroll(Integer.MAX_VALUE);
        assertEquals(0, phantom.getCircuitConfig());
        sync.click(2, false, new ItemStack(circuit, 1, 8));
        assertEquals(0, phantom.getCircuitConfig());
        sync.click(0, false, new ItemStack(circuit, 1, 25));
        assertEquals(0, phantom.getCircuitConfig());

        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeShort(12);
            sync.readOnServer(GhostCircuitSyncHandler.SYNC_CIRCUIT_CONFIG, packet);
            assertEquals(0, phantom.getCircuitConfig());
        } finally {
            packet.release();
        }
        assertTrue(sync.syncRequests > 0);
    }

    private static final class InspectableSync extends GhostCircuitSyncHandler {

        private int syncRequests;

        private InspectableSync(ModularSlot slot, IMetaTileEntity machine) {
            super(slot, machine);
        }

        private void click(int button, boolean shift, ItemStack cursor) {
            phantomClick(new MouseData(
                Side.SERVER,
                button,
                shift,
                false,
                false
            ), cursor);
        }

        private void scroll(int direction) {
            phantomScroll(new MouseData(
                Side.SERVER,
                direction,
                false,
                false,
                false
            ));
        }

        @Override
        public void forceSyncItem() {
            // The test observes sync requests; no remote player or network channel is created.
            syncRequests++;
        }
    }
}
