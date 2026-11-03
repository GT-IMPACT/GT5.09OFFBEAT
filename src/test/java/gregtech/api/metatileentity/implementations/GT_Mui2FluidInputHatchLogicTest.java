package gregtech.api.metatileentity.implementations;

import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.Cover;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2FluidInputHatchLogicTest {

    @Test
    public void recipeFilterPreservesExistingFluidAndSimulationNeverChangesTheTank() throws Throwable {
        run("fluids");
    }

    @Test
    public void frontContainerSlotsDecantAcceptedFluidsButDoNotAutofillEmptyContainers() throws Throwable {
        run("containers");
    }

    @Test
    public void filterAndRecipeMapPersistAndScrewdriverRespectsTheClickedCover() throws Throwable {
        run("persistence");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        ItemList.Display_Fluid.set(new ItemStack(Items.feather));
        Fluid accepted = GT_ProcessingTestSupport.fluid("mui2_hatch_accepted");
        Fluid other = GT_ProcessingTestSupport.fluid("mui2_hatch_other");
        GT_Recipe_Map.sChemicalRecipes.clear();
        assertNotNull(GT_Recipe_Map.sChemicalRecipes.addRecipe(
            false,
            new ItemStack[]{new ItemStack(Items.apple)},
            new ItemStack[]{new ItemStack(Items.gold_ingot)},
            null,
            null,
            new FluidStack[]{new FluidStack(accepted, 100)},
            null,
            20,
            8,
            0
        ));
        GT_Utility.addFluidContainerData(new FluidContainerData(
            new FluidStack(accepted, 1000), new ItemStack(Items.water_bucket), new ItemStack(Items.bucket)
        ));
        GT_Utility.addFluidContainerData(new FluidContainerData(
            new FluidStack(other, 1000), new ItemStack(Items.lava_bucket), new ItemStack(Items.bucket)
        ));
        if ("fluids".equals(scenario)) {
            checkFluids(accepted, other);
        } else if ("containers".equals(scenario)) {
            checkContainers(accepted, other);
        } else if ("persistence".equals(scenario)) {
            checkPersistenceAndTools(accepted, other);
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkFluids(Fluid accepted, Fluid other) {
        for (int tier = 0; tier <= 9; tier++) {
            assertEquals(8000 * (1 << tier), new Fixture(tier).hatch.getCapacity());
        }
        Fixture fixture = new Fixture(1);
        GT_MetaTileEntity_Hatch_Input hatch = fixture.hatch;
        assertTrue(hatch.disableFilter);
        assertEquals(1300, hatch.fill(new FluidStack(other, 1300), true));
        hatch.mRecipeMap = GT_Recipe_Map.sChemicalRecipes;
        hatch.disableFilter = false;
        FluidStack rejected = new FluidStack(other, 900);
        assertFalse(hatch.isFluidInputAllowed(rejected));
        assertEquals(0, hatch.fill(rejected, false));
        assertEquals(0, hatch.fill(rejected, true));
        assertSame(other, hatch.getFluid().getFluid());
        assertEquals(1300, hatch.getFluidAmount());
        assertEquals(900, rejected.amount);
        assertEquals(700, hatch.drain(700, false).amount);
        assertEquals(1300, hatch.getFluidAmount());
        assertEquals(1300, hatch.drain(2000, true).amount);
        assertNull(hatch.getFluid());

        FluidStack incoming = new FluidStack(accepted, hatch.getCapacity() + 700);
        assertEquals(hatch.getCapacity(), hatch.fill(incoming, false));
        assertNull(hatch.getFluid());
        assertEquals(hatch.getCapacity() + 700, incoming.amount);
        assertEquals(hatch.getCapacity(), hatch.fill(incoming, true));
        assertEquals(hatch.getCapacity(), hatch.getFluidAmount());
        assertEquals(0, hatch.fill(new FluidStack(accepted, 100), true));
        assertEquals(hatch.getCapacity() + 700, incoming.amount);
        FluidStack simulated = hatch.drain(250, false);
        assertSame(accepted, simulated.getFluid());
        assertEquals(250, simulated.amount);
        assertEquals(hatch.getCapacity(), hatch.getFluidAmount());
        assertTrue(hatch.canTankBeEmptied());
        assertEquals(hatch.getCapacity(), hatch.drain(hatch.getCapacity(), true).amount);
        hatch.mRecipeMap = null;
        assertTrue(hatch.isFluidInputAllowed(new FluidStack(other, 50)));
        assertEquals(50, hatch.fill(new FluidStack(other, 50), true));

        // The legacy public statics were never per-hatch locks and must not couple different hatches.
        GT_MetaTileEntity_Hatch_Input.mIgnoreMap = true;
        GT_MetaTileEntity_Hatch_Input.mLockedFluid = other;
        Fixture second = new Fixture(1);
        second.hatch.mRecipeMap = GT_Recipe_Map.sChemicalRecipes;
        second.hatch.disableFilter = false;
        assertEquals(100, second.hatch.fill(new FluidStack(accepted, 100), true));
        assertEquals(0, second.hatch.fill(new FluidStack(other, 100), true));
        assertEquals(50, hatch.getFluidAmount());
    }

    private static void checkContainers(Fluid accepted, Fluid other) {
        Fixture fixture = new Fixture(1);
        GT_MetaTileEntity_Hatch_Input hatch = fixture.hatch;
        hatch.mRecipeMap = GT_Recipe_Map.sChemicalRecipes;
        hatch.disableFilter = false;
        ItemStack filled = new ItemStack(Items.water_bucket, 2);
        ItemStack rejected = new ItemStack(Items.lava_bucket, 2);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            byte index = (byte) side.ordinal();
            boolean front = index == fixture.tile.getFrontFacing();
            assertEquals(front, hatch.allowPutStack(fixture.tile, 0, index, filled));
            assertEquals(front, hatch.allowPutStack(fixture.tile, 0, index, new ItemStack(Items.apple)));
            assertFalse(hatch.allowPutStack(fixture.tile, 0, index, rejected));
            assertFalse(hatch.allowPutStack(fixture.tile, 0, index, new ItemStack(Items.diamond)));
            assertFalse(hatch.allowPutStack(fixture.tile, 1, index, filled));
            assertEquals(front, hatch.allowPullStack(fixture.tile, 1, index, new ItemStack(Items.bucket)));
            assertFalse(hatch.allowPullStack(fixture.tile, 0, index, filled));
        }
        assertEquals(2, filled.stackSize);
        assertEquals(2, rejected.stackSize);
        assertFalse(hatch.doesFillContainers());
        assertTrue(hatch.doesEmptyContainers());
        hatch.fill(new FluidStack(accepted, 5000), true);
        ItemStack empty = new ItemStack(Items.bucket, 3);
        hatch.setInventorySlotContents(0, empty);
        hatch.onPreTick(fixture.tile, 51L);
        assertSame(empty, hatch.getStackInSlot(0));
        assertEquals(3, empty.stackSize);
        assertNull(hatch.getStackInSlot(1));
        assertEquals(5000, hatch.getFluidAmount());

        hatch.setInventorySlotContents(0, filled);
        hatch.onPreTick(fixture.tile, 52L);
        assertEquals(1, filled.stackSize);
        assertSame(Items.bucket, hatch.getStackInSlot(1).getItem());
        assertEquals(1, hatch.getStackInSlot(1).stackSize);
        assertEquals(6000, hatch.getFluidAmount());
        hatch.setInventorySlotContents(0, rejected);
        hatch.onPreTick(fixture.tile, 53L);
        assertSame(rejected, hatch.getStackInSlot(0));
        assertEquals(2, rejected.stackSize);
        assertEquals(6000, hatch.getFluidAmount());
        assertEquals(1, hatch.getStackInSlot(1).stackSize);
        hatch.setFillableStack(new FluidStack(accepted, hatch.getCapacity() - 500));
        ItemStack tooLarge = new ItemStack(Items.water_bucket);
        hatch.setInventorySlotContents(0, tooLarge);
        hatch.onPreTick(fixture.tile, 54L);
        assertSame(tooLarge, hatch.getStackInSlot(0));
        assertEquals(1, tooLarge.stackSize);
        assertEquals(hatch.getCapacity() - 500, hatch.getFluidAmount());
        assertEquals(1, hatch.getStackInSlot(1).stackSize);
        assertSame(accepted, hatch.getFluid().getFluid());
        assertFalse(hatch.getFluid().isFluidEqual(new FluidStack(other, 1)));
    }

    private static void checkPersistenceAndTools(Fluid accepted, Fluid other) {
        Fixture fixture = new Fixture(2);
        GT_MetaTileEntity_Hatch_Input hatch = fixture.hatch;
        hatch.mRecipeMap = GT_Recipe_Map.sChemicalRecipes;
        hatch.disableFilter = false;
        hatch.fill(new FluidStack(accepted, 3456), true);
        NBTTagCompound saved = new NBTTagCompound();
        hatch.saveNBTData(saved);
        assertFalse(saved.getBoolean("disableFilter"));
        assertEquals(GT_Recipe_Map.sChemicalRecipes.mUnlocalizedName, saved.getString("recipeMap"));
        Fixture restored = new Fixture(2);
        restored.hatch.loadNBTData(saved);
        assertFalse(restored.hatch.disableFilter);
        assertSame(GT_Recipe_Map.sChemicalRecipes, restored.hatch.mRecipeMap);
        assertSame(accepted, restored.hatch.getFluid().getFluid());
        assertEquals(3456, restored.hatch.getFluidAmount());
        restored.hatch.drain(456, true);
        assertEquals(3000, restored.hatch.getFluidAmount());
        assertEquals(3456, hatch.getFluidAmount());
        NBTTagCompound legacy = (NBTTagCompound) saved.copy();
        legacy.removeTag("disableFilter");
        legacy.removeTag("recipeMap");
        restored.hatch.loadNBTData(legacy);
        assertTrue(restored.hatch.disableFilter);
        assertNull(restored.hatch.mRecipeMap);
        assertEquals(3456, restored.hatch.getFluidAmount());
        NBTTagCompound unknownMap = (NBTTagCompound) saved.copy();
        unknownMap.setString("recipeMap", "missing.recipe.map");
        restored.hatch.loadNBTData(unknownMap);
        assertFalse(restored.hatch.disableFilter);
        assertNull(restored.hatch.mRecipeMap);
        assertTrue(restored.hatch.isFluidInputAllowed(new FluidStack(other, 100)));

        fixture.player.setSneaking(false);
        screwdriver(fixture, fixture.tile.getFrontFacing());
        assertTrue(hatch.disableFilter);
        fixture.player.setSneaking(true);
        screwdriver(fixture, fixture.tile.getFrontFacing());
        assertFalse(hatch.disableFilter);
        ItemStack coverItem = new ItemStack(Items.redstone);
        CoverRegistry.registerCover(
            coverItem,
            null,
            context -> new Cover(context, null),
            CoverRegistry.INTERCEPTS_RIGHT_CLICK_COVER_PLACER
        );
        fixture.tile.attachCover(CoverRegistry.buildCover(coverItem, ForgeDirection.NORTH, fixture.tile));
        screwdriver(fixture, fixture.tile.getFrontFacing());
        assertFalse(hatch.disableFilter);
        screwdriver(fixture, fixture.tile.getBackFacing());
        assertTrue(hatch.disableFilter);
        assertSame(GT_Recipe_Map.sChemicalRecipes, hatch.mRecipeMap);
        assertEquals(3456, hatch.getFluidAmount());
    }

    private static void screwdriver(Fixture fixture, byte side) {
        fixture.hatch.onScrewdriverRightClick(side, fixture.player, 0.5f, 0.5f, 0.5f);
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_Hatch_Input hatch;
        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;

        private Fixture(int tier) {
            hatch = new GT_MetaTileEntity_Hatch_Input("fluid_input_logic_test", tier, "", null);
            tile = new GT_StructureIntegrationTest.TestTile(world, hatch, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            player = new GT_StructureIntegrationTest.TestPlayer(world);
        }
    }
}
