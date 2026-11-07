package gregtech.api.metatileentity.implementations;

import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.covers.Cover;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2BusHatchInputLogicTest {

    @Test
    public void sortingOnlyCompactsTheSixteenRecipeSlotsAndKeepsNbtVariantsSeparate() throws Throwable {
        run("sorting");
    }

    @Test
    public void containerTicksDecantAndFillWithoutUsingRecipeOrDisplaySlots() throws Throwable {
        run("containers");
    }

    @Test
    public void manualSlotValidationAndAutomationKeepTheirDifferentExistingRules() throws Throwable {
        run("slots");
    }

    @Test
    public void fluidSimulationCapacityAndExistingNbtSettingsSurviveReload() throws Throwable {
        run("storage");
    }

    @Test
    public void combinedAndSeparatedProcessingExcludeContainerAndDisplayItems() throws Throwable {
        run("processing");
    }

    @Test
    public void existingScrewdriverAndSolderingActionsKeepTheirCoverAndSneakRules() throws Throwable {
        run("tools");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        new GT_IntegratedCircuit_Item();
        ItemList.Display_Fluid.set(Items.feather);
        ItemList.Bottle_Empty.set(Items.glass_bottle);
        FluidContainerData water = new FluidContainerData(
            new FluidStack(FluidRegistry.WATER, 1000),
            new ItemStack(Items.water_bucket),
            new ItemStack(Items.bucket)
        );
        FluidContainerRegistry.registerFluidContainer(water);
        GT_Utility.addFluidContainerData(water);
        if ("sorting".equals(scenario)) {
            checkSorting();
        } else if ("containers".equals(scenario)) {
            checkContainers();
        } else if ("slots".equals(scenario)) {
            checkSlots();
        } else if ("storage".equals(scenario)) {
            checkStorage();
        } else if ("processing".equals(scenario)) {
            checkProcessing();
        } else if ("tools".equals(scenario)) {
            checkTools();
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkSorting() {
        Fixture fixture = new Fixture(5000);
        GTMTE_BusHatch_Input hatch = fixture.hatch;
        ItemStack container = item("a", 5);
        ItemStack output = new ItemStack(Items.gold_ingot, 2);
        ItemStack display = item("b", 3);
        fixture.tile.setInventorySlotContents(0, container);
        fixture.tile.setInventorySlotContents(17, output);
        fixture.tile.setInventorySlotContents(18, display);
        fixture.tile.setInventorySlotContents(4, item("a", 40));
        fixture.tile.setInventorySlotContents(8, item("a", 50));
        fixture.tile.setInventorySlotContents(10, item("b", 7));
        fixture.tile.setInventorySlotContents(16, new ItemStack(Items.gold_ingot, 3));
        assertTrue(fixture.tile.hasInventoryBeenModified());
        hatch.onPostTick(fixture.tile, 1);
        assertEquals(64, hatch.mInventory[1].stackSize);
        assertEquals("a", hatch.mInventory[1].getTagCompound().getString("variant"));
        assertEquals(26, hatch.mInventory[2].stackSize);
        assertEquals("a", hatch.mInventory[2].getTagCompound().getString("variant"));
        assertEquals(7, hatch.mInventory[3].stackSize);
        assertEquals("b", hatch.mInventory[3].getTagCompound().getString("variant"));
        assertSame(Items.gold_ingot, hatch.mInventory[4].getItem());
        assertEquals(3, hatch.mInventory[4].stackSize);
        for (int slot = 5; slot <= 16; slot++) {
            assertNull(hatch.mInventory[slot]);
        }
        assertSame(container, hatch.mInventory[0]);
        assertSame(output, hatch.mInventory[17]);
        assertSame(display, hatch.mInventory[18]);

        Fixture disabled = new Fixture(5000);
        disabled.hatch.disableSort = true;
        ItemStack untouched = item("a", 9);
        disabled.tile.setInventorySlotContents(4, untouched);
        disabled.tile.setInventorySlotContents(7, item("a", 0));
        disabled.hatch.onPostTick(disabled.tile, 1);
        assertSame(untouched, disabled.hatch.mInventory[4]);
        assertNull(disabled.hatch.mInventory[1]);
        disabled.hatch.updateSlots();
        assertNull(disabled.hatch.mInventory[7]);
        assertSame(untouched, disabled.hatch.mInventory[4]);
    }

    private static void checkContainers() {
        Fixture fixture = new Fixture(5000);
        GTMTE_BusHatch_Input hatch = fixture.hatch;
        assertEquals(20, hatch.mInventory.length);
        assertEquals(0, hatch.getInputSlot());
        assertEquals(17, hatch.getOutputSlot());
        assertEquals(18, hatch.getStackDisplaySlot());
        assertTrue(hatch.doesEmptyContainers());
        assertTrue(hatch.doesFillContainers());
        ItemStack recipe = item("recipe", 5);
        hatch.mInventory[1] = recipe;
        ItemStack filled = new ItemStack(Items.water_bucket, 2);
        hatch.mInventory[0] = filled;
        hatch.onPreTick(fixture.tile, 1);
        assertEquals(1000, hatch.getFluidAmount());
        assertSame(filled, hatch.mInventory[0]);
        assertEquals(1, filled.stackSize);
        assertSame(Items.bucket, hatch.mInventory[17].getItem());
        assertEquals(1, hatch.mInventory[17].stackSize);
        assertSame(recipe, hatch.mInventory[1]);

        ItemStack empty = new ItemStack(Items.bucket, 2);
        hatch.mInventory[0] = empty;
        hatch.mInventory[17] = null;
        hatch.mFluid = new FluidStack(FluidRegistry.WATER, 1500);
        hatch.onPreTick(fixture.tile, 2);
        assertEquals(500, hatch.getFluidAmount());
        assertSame(empty, hatch.mInventory[0]);
        assertEquals(1, empty.stackSize);
        assertSame(Items.water_bucket, hatch.mInventory[17].getItem());
        assertEquals(1, hatch.mInventory[17].stackSize);
        assertNotNull(hatch.mInventory[18]);
        assertSame(recipe, hatch.mInventory[1]);

        ItemStack blocked = new ItemStack(Items.iron_ingot, 64);
        ItemStack notConsumed = new ItemStack(Items.water_bucket);
        hatch.mInventory[0] = notConsumed;
        hatch.mInventory[17] = blocked;
        hatch.onPreTick(fixture.tile, 3);
        assertEquals(500, hatch.getFluidAmount());
        assertSame(notConsumed, hatch.mInventory[0]);
        assertEquals(1, notConsumed.stackSize);
        assertSame(blocked, hatch.mInventory[17]);
        assertSame(recipe, hatch.mInventory[1]);
    }

    private static void checkSlots() {
        Fixture fixture = new Fixture(5000);
        GTMTE_BusHatch_Input hatch = fixture.hatch;
        ItemStack filled = new ItemStack(new Container(new FluidStack(FluidRegistry.WATER, 500), 1000));
        ItemStack empty = new ItemStack(new Container(null, 1000));
        ItemStack bucket = new ItemStack(Items.water_bucket);
        ItemStack item = new ItemStack(Items.iron_ingot);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            byte side = (byte) direction.ordinal();
            assertTrue(hatch.allowPutStack(fixture.tile, 0, side, filled));
            assertFalse(hatch.allowPutStack(fixture.tile, 0, side, empty));
            assertFalse(hatch.allowPutStack(fixture.tile, 0, side, bucket));
            assertFalse(hatch.allowPutStack(fixture.tile, 0, side, item));
            assertTrue(hatch.allowPutStack(fixture.tile, 1, side, filled));
            assertFalse(hatch.allowPutStack(fixture.tile, 17, side, filled));
            assertFalse(hatch.allowPutStack(fixture.tile, 18, side, filled));
            assertTrue(hatch.allowPullStack(fixture.tile, 17, side, item));
            assertFalse(hatch.allowPullStack(fixture.tile, 0, side, filled));
            assertFalse(hatch.allowPullStack(fixture.tile, 1, side, item));
        }
        hatch.restrictFluid = true;
        assertFalse(hatch.allowPutStack(fixture.tile, 1, (byte) 2, filled));
        assertTrue(hatch.allowPutStack(fixture.tile, 1, (byte) 2, empty));
        assertTrue(hatch.allowPutStack(fixture.tile, 1, (byte) 2, bucket));
        assertTrue(hatch.allowPutStack(fixture.tile, 1, (byte) 2, item));
        for (int slot = 0; slot <= 17; slot++) {
            assertTrue("Manual GUI validation differs from automation in slot " + slot,
                    hatch.isItemValidForSlot(slot, filled));
            assertTrue(hatch.isItemValidForSlot(slot, item));
        }
        assertFalse(hatch.isItemValidForSlot(18, item));
        assertNotNull(GT_Utility.getFluidForFilledItem(bucket, true));
        assertNull(GT_Utility.getFluidForFilledItem(item, true));
    }

    private static void checkStorage() {
        Fixture fixture = new Fixture(5000);
        GTMTE_BusHatch_Input hatch = fixture.hatch;
        FluidStack incoming = water("a", 6000);
        assertEquals(5000, hatch.getCapacity());
        assertEquals(5000, hatch.fill(incoming, false));
        assertNull(hatch.mFluid);
        assertEquals(6000, incoming.amount);
        assertEquals(5000, hatch.fill(incoming, true));
        assertEquals(5000, hatch.getFluidAmount());
        incoming.tag.setString("variant", "changed");
        assertEquals("a", hatch.mFluid.tag.getString("variant"));
        FluidStack simulated = hatch.drain(250, false);
        assertEquals(250, simulated.amount);
        assertEquals(5000, hatch.getFluidAmount());
        simulated.tag.setString("variant", "drained");
        assertEquals("a", hatch.mFluid.tag.getString("variant"));
        assertEquals(0, hatch.fill(water("b", 1), true));
        assertEquals(5000, hatch.drain(6000, true).amount);
        assertNull(hatch.mFluid);
        hatch.disableSort = true;
        hatch.disableLimited = false;
        hatch.restrictFluid = true;
        hatch.fill(water("saved", 3456), true);
        NBTTagCompound saved = new NBTTagCompound();
        hatch.saveNBTData(saved);
        Fixture restored = new Fixture(5000);
        restored.hatch.loadNBTData(saved);
        assertTrue(restored.hatch.disableSort);
        assertTrue(restored.hatch.restrictFluid);
        assertFalse(restored.hatch.disableLimited);
        assertEquals(3456, restored.hatch.getFluidAmount());
        assertEquals("saved", restored.hatch.mFluid.tag.getString("variant"));
        assertEquals(5000, restored.hatch.getCapacity());
        assertEquals(5000, ((GTMTE_BusHatch_Input) hatch.newMetaEntity(null)).getCapacity());
        assertEquals(20, hatch.newMetaEntity(null).mInventory.length);
        restored.hatch.loadNBTData(new NBTTagCompound());
        assertFalse(restored.hatch.disableSort);
        assertFalse(restored.hatch.restrictFluid);
        assertTrue(restored.hatch.disableLimited);
        assertNull(restored.hatch.mFluid);
    }

    private static void checkProcessing() {
        Machine machine = new Machine();
        GT_ProcessingTestSupport.Fixture resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
        GTMTE_BusHatch_Input hatch = hatch(5000);
        resources.attach(hatch);
        hatch.disableSort = true;
        machine.mInputBusHatches.add(hatch);
        ItemStack container = new ItemStack(Items.iron_ingot, 64);
        ItemStack output = new ItemStack(Items.iron_ingot, 64);
        ItemStack display = new ItemStack(Items.iron_ingot, 64);
        hatch.mInventory[0] = container;
        hatch.mInventory[1] = new ItemStack(Items.iron_ingot, 3);
        hatch.mInventory[16] = new ItemStack(Items.iron_ingot, 4);
        hatch.mInventory[17] = output;
        hatch.mInventory[18] = display;
        hatch.mFluid = new FluidStack(FluidRegistry.WATER, 700);
        assertEquals(2, GTMTE_BusHatch_Input.getStoredInputs(hatch).size());
        assertSame(hatch.mInventory[16], GTMTE_BusHatch_Input.getStoredInputs(hatch).get(0));
        assertSame(hatch.mInventory[1], GTMTE_BusHatch_Input.getStoredInputs(hatch).get(1));
        assertEquals(2, machine.getStoredInputs().size());
        assertEquals(1, machine.getStoredFluids().size());
        assertSame(hatch.mFluid, machine.getStoredFluids().get(0));
        GT_Recipe_Map.sChemicalRecipes.clear();
        GT_Recipe recipe = GT_Recipe_Map.sChemicalRecipes.addRecipe(
            false,
            new ItemStack[]{new ItemStack(Items.iron_ingot, 2)},
            new ItemStack[]{new ItemStack(Items.gold_ingot)},
            null,
            null,
            new FluidStack[]{new FluidStack(FluidRegistry.WATER, 100)},
            null,
            20,
            8,
            0
        );
        assertNotNull(recipe);
        ProcessingInputs inputs = machine.inputs().get(0);
        assertEquals(2, inputs.items.length);
        assertTrue(inputs.consume(recipe, 1));
        assertEquals(4, hatch.mInventory[16].stackSize);
        assertEquals(700, hatch.getFluidAmount());
        assertTrue(inputs.commit());
        assertEquals(2, hatch.mInventory[16].stackSize);
        assertEquals(3, hatch.mInventory[1].stackSize);
        assertEquals(600, hatch.getFluidAmount());
        assertSame(container, hatch.mInventory[0]);
        assertSame(output, hatch.mInventory[17]);
        assertSame(display, hatch.mInventory[18]);
        assertEquals(64, container.stackSize);
        assertEquals(64, output.stackSize);
        assertEquals(64, display.stackSize);

        ProcessingInputs changed = machine.inputs().get(0);
        assertTrue(changed.consume(recipe, 1));
        hatch.setInventorySlotContents(1, hatch.mInventory[1].copy());
        assertFalse("Replacing a GUI recipe stack invalidates the pending snapshot", changed.commit());
        assertEquals(600, hatch.getFluidAmount());
        GTMTE_BusHatch_Input second = hatch(5000);
        resources.attach(second);
        second.mInventory[0] = new ItemStack(Items.apple, 64);
        second.mInventory[4] = new ItemStack(Items.gold_ingot, 5);
        second.mInventory[17] = new ItemStack(Items.apple, 64);
        second.mInventory[18] = new ItemStack(Items.apple, 64);
        machine.mInputBusHatches.add(second);
        machine.setProcessingModes(ProcessingLogic.SEPARATE_INPUTS);
        List<ProcessingInputs> separated = machine.inputs();
        assertEquals(2, separated.size());
        assertEquals(2, separated.get(0).items.length);
        assertEquals(1, separated.get(1).items.length);
        assertSame(Items.gold_ingot, separated.get(1).items[0].getItem());
        assertEquals(5, separated.get(1).items[0].stackSize);
        assertEquals(1, separated.get(0).fluids.length);
        assertEquals(1, separated.get(1).fluids.length);

        GhostCircuitItemStackHandler ghost = new GhostCircuitItemStackHandler(hatch);
        ghost.setCircuitConfig(26);
        GT_Recipe_Map.sChemicalRecipes.clear();
        GT_Recipe circuitRecipe = GT_Recipe_Map.sChemicalRecipes.addRecipe(
            false,
            new ItemStack[]{new ItemStack(Items.iron_ingot), GT_Utility.getIntegratedCircuit(26)},
            new ItemStack[]{new ItemStack(Items.diamond)},
            null,
            null,
            new FluidStack[]{new FluidStack(FluidRegistry.WATER, 100)},
            null,
            20,
            8,
            0
        );
        assertNotNull(circuitRecipe);
        for (int modes : new int[]{0, ProcessingLogic.SEPARATE_INPUTS}) {
            machine.setProcessingModes(modes);
            ProcessingInputs withCircuit = machine.inputs().get(0);
            assertTrue(withCircuit.consume(circuitRecipe, 1));
            assertTrue(withCircuit.commit());
            assertEquals(26, ghost.getCircuitConfig());
            assertEquals(0, ghost.getStackInSlot(0).stackSize);
        }
    }

    private static void checkTools() {
        Fixture fixture = new Fixture(5000);
        fixture.player.setSneaking(false);
        fixture.hatch.onScrewdriverRightClick((byte) 2, fixture.player, 0, 0, 0);
        assertFalse(fixture.hatch.disableSort);
        fixture.player.setSneaking(true);
        fixture.hatch.onScrewdriverRightClick((byte) 2, fixture.player, 0, 0, 0);
        assertTrue(fixture.hatch.disableSort);
        ItemStack coverItem = new ItemStack(Items.redstone);
        CoverRegistry.registerCover(
            coverItem,
            null,
            context -> new Cover(context, null),
            CoverRegistry.INTERCEPTS_RIGHT_CLICK_COVER_PLACER
        );
        fixture.tile.attachCover(CoverRegistry.buildCover(coverItem, ForgeDirection.NORTH, fixture.tile));
        fixture.hatch.onScrewdriverRightClick((byte) 2, fixture.player, 0, 0, 0);
        assertTrue(fixture.hatch.disableSort);
        byte back = fixture.tile.getBackFacing();
        fixture.hatch.onScrewdriverRightClick(back, fixture.player, 0, 0, 0);
        assertFalse(fixture.hatch.disableSort);
        assertTrue(fixture.hatch.onSolderingToolRightClick((byte) 2, (byte) 2, fixture.player, 0, 0, 0));
        assertTrue(fixture.hatch.restrictFluid);
        fixture.player.setSneaking(false);
        assertTrue(fixture.hatch.onSolderingToolRightClick(back, back, fixture.player, 0, 0, 0));
        assertFalse(fixture.hatch.restrictFluid);
    }

    private static ItemStack item(String variant, int amount) {
        ItemStack item = new ItemStack(Items.iron_ingot, amount);
        item.setTagCompound(new NBTTagCompound());
        item.getTagCompound().setString("variant", variant);
        return item;
    }

    private static FluidStack water(String variant, int amount) {
        FluidStack fluid = new FluidStack(FluidRegistry.WATER, amount);
        fluid.tag = new NBTTagCompound();
        fluid.tag.setString("variant", variant);
        return fluid;
    }

    private static GTMTE_BusHatch_Input hatch(int capacity) {
        return new GTMTE_BusHatch_Input(
            "combined_input_logic_test",
            4,
            new String[0],
            null,
            capacity
        );
    }

    private static final class Fixture {

        final GTMTE_BusHatch_Input hatch;
        final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        final GT_StructureIntegrationTest.TestTile tile;
        final GT_StructureIntegrationTest.TestPlayer player;

        Fixture(int capacity) {
            hatch = hatch(capacity);
            tile = new GT_StructureIntegrationTest.TestTile(world, hatch, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
        }
    }

    private static final class Machine extends GT_MetaTileEntity_VacuumFreezer {

        Machine() {
            super("combined_input_processing_test");
        }

        List<ProcessingInputs> inputs() {
            return collectProcessingInputs();
        }
    }

    private static final class Container extends Item implements IFluidContainerItem {

        private final FluidStack fluid;
        private final int capacity;

        Container(FluidStack fluid, int capacity) {
            this.fluid = fluid;
            this.capacity = capacity;
        }

        @Override
        public FluidStack getFluid(ItemStack container) {
            return fluid == null ? null : fluid.copy();
        }

        @Override
        public int getCapacity(ItemStack container) {
            return capacity;
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
