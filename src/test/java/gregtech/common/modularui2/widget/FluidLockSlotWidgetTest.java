package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.interfaces.metatileentity.IFluidLockableMui2;
import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FluidLockSlotWidgetTest {

    @Test
    public void fluidFilterSamplesCursorWithoutConsumingFluidAndSimulationDoesNotChangeTheLock() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(FluidLockSlotWidgetTest.class, "fluid-lock", "");
        }
    }

    public static void runScenario(String scenario, String model) {
        FilterMachine machine = new FilterMachine();
        FluidLockSlotWidget slot = new FluidLockSlotWidget(machine);
        assertNotNull(slot.getSyncHandler());
        assertTrue(slot.getSyncHandler().isPhantom());
        assertFalse(slot.getSyncHandler().controlsAmount());
        assertSame(slot, slot.getSyncHandler().getFluidTank());

        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        assertEquals(1, slot.fill(water, false));
        assertNull(machine.getLockedFluid());
        assertFalse(machine.isFluidLocked());
        assertEquals(1, slot.fill(water, true));
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertTrue(machine.isFluidLocked());
        assertEquals(1000, water.amount);
        assertEquals(0, slot.fill(new FluidStack(FluidRegistry.LAVA, 1000), true));
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        slot.getSyncHandler().setValue(new FluidStack(FluidRegistry.LAVA, 1000), true, false);
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());

        slot.drain(1, false);
        slot.drain(0, true);
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        slot.drain(1, true);
        assertNull(machine.getLockedFluid());
        assertFalse(machine.isFluidLocked());

        FluidContainerRegistry.registerFluidContainer(
            new FluidStack(FluidRegistry.WATER, 1000),
            new ItemStack(Items.water_bucket),
            new ItemStack(Items.bucket)
        );
        TestFluidSlotSyncHandler handler = new TestFluidSlotSyncHandler(slot);
        slot.syncHandler(handler);
        ItemStack cursor = new ItemStack(Items.water_bucket, 5);
        handler.click(cursor);
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
        assertTrue(machine.isFluidLocked());
        assertEquals(5, cursor.stackSize);
        assertSame(Items.water_bucket, cursor.getItem());
        handler.click(new ItemStack(Items.lava_bucket));
        assertSame(FluidRegistry.WATER, machine.getLockedFluid());
    }

    private static final class TestFluidSlotSyncHandler extends FluidSlotSyncHandler {

        private TestFluidSlotSyncHandler(IFluidTank tank) {
            super(tank);
        }

        private void click(ItemStack cursor) {
            tryClickPhantom(new MouseData(
                Side.SERVER,
                0,
                false,
                false,
                false
            ), cursor);
        }
    }

    private static final class FilterMachine implements IFluidLockableMui2 {

        private Fluid lockedFluid;
        private boolean locked;

        @Override
        public void setLockedFluid(Fluid fluid) {
            lockedFluid = fluid;
        }

        @Override
        public Fluid getLockedFluid() {
            return lockedFluid;
        }

        @Override
        public void lockFluid(boolean lock) {
            locked = lock;
        }

        @Override
        public boolean isFluidLocked() {
            return locked;
        }

        @Override
        public boolean acceptsFluidLock(Fluid fluid) {
            return fluid == FluidRegistry.WATER;
        }
    }
}
