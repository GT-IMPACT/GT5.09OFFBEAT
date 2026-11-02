package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_MicrowaveEnergyTransmitter;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Teleporter;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2CoordinateMachineTest {

    @Test
    public void teleporterUsesTheNativeFactoryAndPersistsCoordinatesFromItsFourFields() throws Throwable {
        run("teleporter");
    }

    @Test
    public void microwaveTransmitterUsesTheNativeFactoryAndPersistsCoordinatesFromItsFourFields() throws Throwable {
        run("microwave");
    }

    private void run(String model) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "build", model);
        }
    }

    public static void runScenario(String scenario, String model) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            GTGuis.init();
            GT_MetaTileEntity_BasicTank machine;
            if ("teleporter".equals(model)) {
                machine = new GT_MetaTileEntity_Teleporter("mui2_teleporter", 1, "", null);
                ((GT_MetaTileEntity_Teleporter) machine).mDebug = true;
            } else if ("microwave".equals(model)) {
                machine = new GT_MetaTileEntity_MicrowaveEnergyTransmitter("mui2_microwave", 1, "", null);
                ((GT_MetaTileEntity_MicrowaveEnergyTransmitter) machine).mDebug = true;
            } else {
                throw new AssertionError(model);
            }
            checkContainerAndPersistence(machine);
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkContainerAndPersistence(GT_MetaTileEntity_BasicTank machine) throws Exception {
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        DirtyTile tile = new DirtyTile(world, machine);
        world.putTile(4, 64, 7, tile);
        GT_ProcessingTestSupport.setField(tile, "mTickTimer", 41L);
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        player.setPosition(4.5, 64.5, 7.5);
        PosGuiData data = new PosGuiData(player, 4, 64, 7);
        assertTrue(machine.useMui2());
        assertSame(machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(data));

        ItemStack storageStack = new ItemStack(Items.paper, 3);
        machine.mInventory[2] = storageStack;
        machine.setFillableStack(new FluidStack(GT_ProcessingTestSupport.fluid("mui2_coordinate_fluid"), 1337));
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager panelSync = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        ModularPanel panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, panelSync, settings);
        WidgetTree.collectSyncValues(panelSync, panel);
        ModularContainer container = new ModularContainer();
        container.construct(
            player,
            sync,
            settings,
            panel.getName(),
            data
        );
        assertTrue(container.isInitialized());
        assertTrue(panelSync.isInitialised());
        assertFalse(container.isClient());
        // The legacy holo slots were coordinate buttons, never a physical inventory for these machines.
        assertEquals(36, container.inventorySlots.size());
        List<TextFieldWidget> fields = new ArrayList<>();
        collectFields(panel, fields);
        assertEquals(4, fields.size());

        String[] keys = {"mTargetX", "mTargetY", "mTargetZ", "mTargetD"};
        int[] coordinates = {-30_000_000, Integer.MIN_VALUE, Integer.MAX_VALUE, -1};
        for (int index = 0; index < fields.size(); index++) {
            assertTrue(fields.get(index).getStringValue() instanceof IntSyncValue);
            IntSyncValue fieldSync = (IntSyncValue) fields.get(index).getStringValue();
            assertTrue(fieldSync.isValid());
            assertTrue(fieldSync.isAllowC2S());
            assertSame(panelSync, fieldSync.getSyncManager());
            int previousDirtyCalls = tile.dirtyCalls;
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeVarIntToBuffer(coordinates[index]);
                fieldSync.readOnServer(ValueSyncHandler.SYNC_VALUE, packet);
            } finally {
                packet.release();
            }
            assertEquals(coordinates[index], fieldSync.getIntValue());
            NBTTagCompound written = new NBTTagCompound();
            machine.saveNBTData(written);
            assertEquals(coordinates[index], written.getInteger(keys[index]));
            assertTrue(tile.dirtyCalls > previousDirtyCalls);
        }
        assertSame(storageStack, machine.mInventory[2]);
        assertEquals(3, storageStack.stackSize);
        assertEquals(1337, machine.getFillableStack().amount);

        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        GT_MetaTileEntity_BasicTank restored = (GT_MetaTileEntity_BasicTank) machine.newMetaEntity(null);
        restored.loadNBTData(saved);
        NBTTagCompound rewritten = new NBTTagCompound();
        restored.saveNBTData(rewritten);
        for (int index = 0; index < keys.length; index++) {
            assertEquals(coordinates[index], rewritten.getInteger(keys[index]));
        }
        assertTrue(rewritten.getBoolean("mDebug"));
        assertNotNull(restored.getFillableStack());
        assertSame(machine.getFillableStack().getFluid(), restored.getFillableStack().getFluid());
        assertEquals(1337, restored.getFillableStack().amount);
        assertTrue(container.canInteractWith(player));
        player.setPosition(100, 64, 100);
        assertFalse(container.canInteractWith(player));
    }

    private static void collectFields(IWidget widget, List<TextFieldWidget> fields) {
        if (widget instanceof TextFieldWidget) {
            fields.add((TextFieldWidget) widget);
        }
        for (IWidget child : widget.getChildren()) {
            collectFields(child, fields);
        }
    }

    private static final class DirtyTile extends GT_StructureIntegrationTest.TestTile {

        private int dirtyCalls;

        private DirtyTile(GT_ProcessingTestSupport.ProcessingWorld world, GT_MetaTileEntity_BasicTank machine) {
            super(world, machine, ForgeDirection.NORTH);
        }

        @Override
        public void markDirty() {
            dirtyCalls++;
        }
    }
}
