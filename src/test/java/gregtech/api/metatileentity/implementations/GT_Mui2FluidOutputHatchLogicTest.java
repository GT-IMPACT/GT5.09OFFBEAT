package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.modularui2.widget.FluidLockSlotWidget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2FluidOutputHatchLogicTest {

    @Test
    public void modeChangesAndPhantomLocksPreservePhysicalFluidsAndItems() throws Throwable {
        run("modes");
    }

    @Test
    public void physicalFillAndSimulationEnforceModesLocksAndExistingFluidNbt() throws Throwable {
        run("fill");
    }

    @Test
    public void screwdriverAndAutomaticLockNotifyTheSelectingPlayerOnce() throws Throwable {
        run("automatic");
    }

    @Test
    public void filledContainerSelectionDoesNotConsumeOrReplaceTheHeldContainer() throws Throwable {
        run("container");
    }

    @Test
    public void legacyNbtAndDisplaySlotsRemainCompatibleWithThePhantomFilter() throws Throwable {
        run("nbt");
    }

    @Test
    public void containerTicksFillEmptyContainersAndLeaveFilledContainersUntouched() throws Throwable {
        run("containerTicks");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        if ("modes".equals(scenario)) {
            checkModes();
        } else if ("fill".equals(scenario)) {
            checkFill();
        } else if ("automatic".equals(scenario)) {
            checkAutomaticLock();
        } else if ("container".equals(scenario)) {
            checkContainers();
        } else if ("nbt".equals(scenario)) {
            checkNbtAndDisplay();
        } else if ("containerTicks".equals(scenario)) {
            checkContainerTicks();
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkModes() {
        for (byte mode = 0; mode < 10; mode++) {
            Hatch hatch = new Hatch();
            hatch.mFluid = new FluidStack(FluidRegistry.WATER, 17);
            hatch.setMode(mode);
            assertEquals(mode, hatch.getMode());
            assertEquals(mode == 8 || mode == 9, hatch.isFluidLocked());
            assertSame(mode >= 8 ? FluidRegistry.WATER : null, hatch.getLockedFluid());
            assertEquals(17, hatch.getFluidAmount());
            assertTrue(hatch.dirty > 0);
            hatch.lockFluid(false);
            assertNull(hatch.getLockedFluid());
            assertEquals(mode >= 8 ? 0 : mode, hatch.getMode());
        }

        Hatch hatch = new Hatch();
        ItemStack physicalInput = new ItemStack(Items.iron_ingot, 3);
        ItemStack physicalOutput = new ItemStack(Items.gold_ingot, 2);
        hatch.mInventory[0] = physicalInput;
        hatch.mInventory[1] = physicalOutput;
        FluidLockSlotWidget filter = new FluidLockSlotWidget(hatch);
        FluidStack selected = new FluidStack(FluidRegistry.WATER, 1000);
        assertEquals(1, filter.fill(selected, false));
        assertNull(hatch.getLockedFluid());
        assertEquals(0, hatch.getMode());
        assertEquals(1, filter.fill(selected, true));
        assertSame(FluidRegistry.WATER, hatch.getLockedFluid());
        assertEquals(9, hatch.getMode());
        assertNull(hatch.mFluid);
        assertEquals(1000, selected.amount);
        hatch.setMode((byte) 8);
        assertEquals(1, filter.fill(new FluidStack(FluidRegistry.LAVA, 1000), true));
        assertSame(FluidRegistry.LAVA, hatch.getLockedFluid());
        assertEquals("Selecting a fluid preserves the items-and-fluid mode", 8, hatch.getMode());
        filter.drain(1, false);
        assertSame(FluidRegistry.LAVA, hatch.getLockedFluid());
        filter.drain(1, true);
        assertNull(hatch.getLockedFluid());
        assertEquals(0, hatch.getMode());
        assertSame(physicalInput, hatch.mInventory[0]);
        assertSame(physicalOutput, hatch.mInventory[1]);
        assertEquals(3, physicalInput.stackSize);
        assertEquals(2, physicalOutput.stackSize);
        assertNull(hatch.mFluid);
    }

    private static void checkFill() {
        Fluid steam = GT_ProcessingTestSupport.fluid("steam");
        for (byte mode = 0; mode < 8; mode++) {
            Hatch hatch = new Hatch();
            hatch.setMode(mode);
            assertEquals(hatch.outputsSteam(), hatch.isFluidInputAllowed(new FluidStack(steam, 1)));
            assertEquals(hatch.outputsLiquids(), hatch.isFluidInputAllowed(new FluidStack(FluidRegistry.WATER, 1)));
            assertEquals(hatch.outputsSteam() ? 1 : 0, hatch.fill(new FluidStack(steam, 1), false));
            assertEquals(hatch.outputsLiquids() ? 1 : 0,
                    hatch.fill(new FluidStack(FluidRegistry.WATER, 1), false));
            assertNull(hatch.mFluid);
        }
        Hatch hatch = new Hatch();
        hatch.setMode((byte) 9);
        assertTrue(hatch.canStoreFluid(new FluidStack(steam, 1)));
        assertTrue(hatch.canStoreFluid(new FluidStack(FluidRegistry.WATER, 1)));
        hatch.setLockedFluid(FluidRegistry.WATER);
        assertFalse(hatch.isFluidInputAllowed(new FluidStack(FluidRegistry.LAVA, 1)));
        assertEquals(0, hatch.fill(new FluidStack(FluidRegistry.LAVA, 1), true));
        hatch.dirty = 0;
        assertEquals(7, hatch.fill(new FluidStack(FluidRegistry.WATER, 7), false));
        assertEquals(0, hatch.dirty);
        assertNull(hatch.mFluid);
        assertEquals(7, hatch.fill(new FluidStack(FluidRegistry.WATER, 7), true));
        assertEquals(7, hatch.getFluidAmount());
        assertTrue(hatch.dirty > 0);

        hatch.mFluid = taggedWater("a", 7);
        assertFalse(hatch.canStoreFluid(taggedWater("b", 1)));
        assertTrue(hatch.canStoreFluid(taggedWater("a", 1)));
        assertEquals(0, hatch.fill(taggedWater("b", 1), true));
        assertEquals(7, hatch.getFluidAmount());
        hatch.mFluid = null;
        FluidStack large = new FluidStack(FluidRegistry.WATER, hatch.getCapacity() + 5);
        assertEquals(16000, hatch.getCapacity());
        assertEquals(16000, hatch.fill(large, true));
        assertEquals(16000, hatch.getFluidAmount());
        assertEquals(16005, large.amount);
    }

    private static void checkAutomaticLock() {
        Hatch hatch = new Hatch();
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(hatch.world);
        hatch.setMode((byte) 7);
        hatch.onScrewdriverRightClick((byte) 2, player, 0, 0, 0);
        assertEquals(8, hatch.getMode());
        assertNull(hatch.getLockedFluid());
        assertEquals(1, player.messages.size());
        assertEquals("GT5U.chat.hatch.output.items_and_specific_fluid", key(player, 0));
        hatch.fill(new FluidStack(FluidRegistry.WATER, 5), true);
        hatch.onEmptyingContainerWhenEmpty();
        assertSame(FluidRegistry.WATER, hatch.getLockedFluid());
        assertEquals(2, player.messages.size());
        assertEquals("GT5U.chat.hatch.output.fluid.locked_to", key(player, 1));
        hatch.onEmptyingContainerWhenEmpty();
        assertEquals(2, player.messages.size());
        player.setSneaking(true);
        hatch.onScrewdriverRightClick((byte) 2, player, 0, 0, 0);
        assertEquals(7, hatch.getMode());
        assertNull(hatch.getLockedFluid());
        hatch.clickable = false;
        hatch.onScrewdriverRightClick((byte) 2, player, 0, 0, 0);
        assertEquals(7, hatch.getMode());
        assertEquals(3, player.messages.size());
        hatch.clickable = true;
        hatch.setMode((byte) 0);
        hatch.mFluid = null;
        hatch.lockFluid(true);
        hatch.fill(new FluidStack(FluidRegistry.LAVA, 2), true);
        hatch.onEmptyingContainerWhenEmpty();
        assertSame(FluidRegistry.LAVA, hatch.getLockedFluid());
        assertEquals("The previous selecting player is notified only once", 3, player.messages.size());
    }

    private static void checkContainers() {
        FluidContainerRegistry.registerFluidContainer(
            new FluidStack(FluidRegistry.WATER, 1000),
            new ItemStack(Items.water_bucket),
            new ItemStack(Items.bucket)
        );
        FluidContainerRegistry.registerFluidContainer(
            new FluidStack(FluidRegistry.LAVA, 1000),
            new ItemStack(Items.lava_bucket),
            new ItemStack(Items.bucket)
        );
        Hatch hatch = new Hatch();
        IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
        GT_StructureIntegrationTest.TestPlayer player = new GT_StructureIntegrationTest.TestPlayer(hatch.world);
        ItemStack water = new ItemStack(Items.water_bucket, 5);
        player.inventory.mainInventory[0] = water;
        hatch.setMode((byte) 8);
        assertTrue(hatch.onRightclick(tile, player, (byte) 2, 0, 0, 0));
        assertSame(FluidRegistry.WATER, hatch.getLockedFluid());
        assertEquals(8, hatch.getMode());
        assertSame(water, player.inventory.getCurrentItem());
        assertEquals(5, water.stackSize);
        assertNull(hatch.mFluid);
        assertEquals(0, hatch.opened);
        ItemStack lava = new ItemStack(Items.lava_bucket, 4);
        player.inventory.mainInventory[0] = lava;
        hatch.onRightclick(tile, player, (byte) 2, 0, 0, 0);
        assertSame(FluidRegistry.WATER, hatch.getLockedFluid());
        assertEquals("GT5U.chat.hatch.output.fluid.already_locked", key(player, 1));
        assertSame(lava, player.inventory.getCurrentItem());
        assertEquals(4, lava.stackSize);
        assertEquals(0, hatch.opened);
        hatch.setMode((byte) 0);
        hatch.onRightclick(tile, player, (byte) 2, 0, 0, 0);
        assertEquals(1, hatch.opened);
        assertNull(hatch.getLockedFluid());

        ItemStack custom = new ItemStack(new FluidContainer(), 3);
        player.inventory.mainInventory[0] = custom;
        hatch.setMode((byte) 9);
        hatch.onRightclick(tile, player, (byte) 2, 0, 0, 0);
        assertSame(FluidRegistry.LAVA, hatch.getLockedFluid());
        assertSame(custom, player.inventory.getCurrentItem());
        assertEquals(3, custom.stackSize);
        assertNull(hatch.mFluid);
        assertEquals(1, hatch.opened);
    }

    private static void checkNbtAndDisplay() throws Exception {
        Hatch hatch = new Hatch();
        hatch.mMode = 8;
        hatch.mFluid = taggedWater("tank", 7);
        hatch.lockedFluid = new FluidStack(FluidRegistry.LAVA, 0);
        hatch.lockedFluid.tag = new NBTTagCompound();
        hatch.lockedFluid.tag.setString("legacy", "preserved");
        NBTTagCompound saved = new NBTTagCompound();
        hatch.saveNBTData(saved);
        Hatch restored = new Hatch();
        restored.loadNBTData(saved);
        assertEquals(8, restored.getMode());
        assertSame(FluidRegistry.LAVA, restored.getLockedFluid());
        assertEquals("preserved", restored.lockedFluid.tag.getString("legacy"));
        assertEquals(0, restored.lockedFluid.amount);
        assertEquals(7, restored.mFluid.amount);
        assertEquals("tank", restored.mFluid.tag.getString("variant"));
        restored.setLockedFluid(null);
        saved.setString("lockedFluidName", "lava");
        restored.saveNBTData(saved);
        assertFalse(saved.hasKey("lockedFluid"));
        assertFalse(saved.hasKey("lockedFluidName"));
        NBTTagCompound named = new NBTTagCompound();
        named.setByte("mMode", (byte) 9);
        named.setString("lockedFluidName", "water");
        restored.loadNBTData(named);
        assertSame(FluidRegistry.WATER, restored.getLockedFluid());
        named.setString("lockedFluidName", "unknown_missing_fluid");
        restored.loadNBTData(named);
        assertNull(restored.getLockedFluid());

        GT_MetaTileEntity_Hatch_Output legacy = new GT_MetaTileEntity_Hatch_Output("legacy", 1, new String[0], null);
        GT_ProcessingTestSupport.setField(legacy, "mInventory", new ItemStack[3]);
        legacy.mMode = 8;
        legacy.lockedFluid = new FluidStack(FluidRegistry.WATER, 0);
        legacy.doDisplayThings();
        assertEquals(3, legacy.mInventory.length);
        assertEquals(3, legacy.getLockedDisplaySlot());
        ItemList.Display_Fluid.set(Items.diamond);
        GT_ProcessingTestSupport.setField(legacy, "mInventory", new ItemStack[4]);
        ItemStack physical = new ItemStack(Items.iron_ingot, 2);
        legacy.mInventory[0] = physical;
        legacy.doDisplayThings();
        assertNotNull(legacy.mInventory[3]);
        assertSame(physical, legacy.mInventory[0]);
        legacy.lockedFluid = null;
        legacy.doDisplayThings();
        assertNull(legacy.mInventory[3]);
        assertSame(physical, legacy.mInventory[0]);
    }

    private static void checkContainerTicks() {
        ItemList.Bottle_Empty.set(Items.glass_bottle);
        FluidContainerData waterContainer = new FluidContainerData(
            new FluidStack(FluidRegistry.WATER, 1000),
            new ItemStack(Items.water_bucket),
            new ItemStack(Items.bucket)
        );
        FluidContainerRegistry.registerFluidContainer(waterContainer);
        GT_Utility.addFluidContainerData(waterContainer);
        ItemList.Display_Fluid.set(Items.diamond);
        Hatch hatch = new Hatch();
        assertTrue(hatch.doesFillContainers());
        assertFalse(hatch.doesEmptyContainers());
        hatch.mFluid = new FluidStack(FluidRegistry.WATER, 1500);
        ItemStack emptyBuckets = new ItemStack(Items.bucket, 2);
        hatch.mInventory[0] = emptyBuckets;
        hatch.onPreTick(hatch.getBaseMetaTileEntity(), 1);
        assertEquals(500, hatch.getFluidAmount());
        assertSame(Items.water_bucket, hatch.mInventory[1].getItem());
        assertEquals(1, hatch.mInventory[1].stackSize);
        assertSame(emptyBuckets, hatch.mInventory[0]);
        assertEquals(1, emptyBuckets.stackSize);

        ItemStack filledBucket = new ItemStack(Items.water_bucket);
        hatch.mFluid = null;
        hatch.mInventory[0] = filledBucket;
        hatch.mInventory[1] = null;
        hatch.onPreTick(hatch.getBaseMetaTileEntity(), 2);
        assertNull(hatch.mFluid);
        assertSame(filledBucket, hatch.mInventory[0]);
        assertEquals(1, filledBucket.stackSize);
        assertNull(hatch.mInventory[1]);
        assertTrue(hatch.canTankBeFilled());
        assertEquals(4, hatch.fill(new FluidStack(FluidRegistry.WATER, 4), true));
        assertEquals(4, hatch.getFluidAmount());
    }

    private static FluidStack taggedWater(String variant, int amount) {
        FluidStack water = new FluidStack(FluidRegistry.WATER, amount);
        water.tag = new NBTTagCompound();
        water.tag.setString("variant", variant);
        return water;
    }

    private static String key(GT_StructureIntegrationTest.TestPlayer player, int index) {
        return ((ChatComponentTranslation) player.messages.get(index)).getKey();
    }

    private static final class Hatch extends GT_MetaTileEntity_Hatch_Output {

        final GT_StructureIntegrationTest.TestWorld world = new GT_StructureIntegrationTest.TestWorld();
        boolean clickable = true;
        int dirty;
        int opened;

        Hatch() {
            super("fluid_output_logic_test", 1, new String[0], null);
            new GT_StructureIntegrationTest.TestTile(world, this, ForgeDirection.NORTH) {
                @Override
                public GT_CoverBehavior getCoverBehaviorAtSide(byte side) {
                    return new GT_CoverBehavior() {
                        @Override
                        public boolean isGUIClickable(
                            byte side,
                            int coverId,
                            int coverData,
                            ICoverable tile
                        ) {
                            return clickable;
                        }
                    };
                }
            };
        }

        @Override
        protected void markStorageDirty() {
            dirty++;
        }

        @Override
        public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
            opened++;
            return true;
        }

        @Override
        public void doDisplayThings() {
        }
    }

    private static final class FluidContainer extends Item implements IFluidContainerItem {

        @Override
        public FluidStack getFluid(ItemStack container) {
            return new FluidStack(FluidRegistry.LAVA, 500);
        }

        @Override
        public int getCapacity(ItemStack container) {
            return 1000;
        }

        @Override
        public int fill(ItemStack container, FluidStack fluid, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(ItemStack container, int amount, boolean doDrain) {
            return null;
        }
    }
}
