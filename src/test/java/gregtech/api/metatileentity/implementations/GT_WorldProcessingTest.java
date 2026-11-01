package gregtech.api.metatileentity.implementations;

import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.logic.ProcessingWorldChanges;
import gregtech.api.logic.WorldProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import static gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport.*;
import static org.junit.Assert.*;

public class GT_WorldProcessingTest {
    private static GameRuntime runtime;

    @BeforeClass
    public static void openGame() throws Exception {
        runtime = new GameRuntime();
    }

    @AfterClass
    public static void closeGame() throws Exception {
        runtime.close();
    }

    @Test
    public void failedBlockPlacementRestoresEarlierChangesAndKeepsInputs() throws Throwable {
        runtime.run(getClass(), "rollback", "");
    }

    @Test
    public void changedObservedBlockRejectsAnOtherwiseValidOperation() throws Throwable {
        runtime.run(getClass(), "changedBlock", "");
    }

    @Test
    public void outputProtectionRejectsBeforeAnyWorldOrInventoryChanges() throws Throwable {
        runtime.run(getClass(), "outputProtection", "");
    }

    @Test
    public void powerAndEfficiencyOverflowRejectBeforeCommit() throws Throwable {
        runtime.run(getClass(), "overflow", "");
    }

    @Test
    public void mergedWritesAndUnchangedBlocksDoNotCauseFalseFailures() throws Throwable {
        runtime.run(getClass(), "mergedWrites", "");
    }

    public static void runScenario(String scenario, String ignored) throws Throwable {
        try {
            GT_WorldProcessingTest.class.getDeclaredMethod(scenario).invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void rollback() {
        SnapshotWorld world = new SnapshotWorld() {
            @Override
            public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
                if (x == 2 && block == Blocks.stone) {
                    return false;
                }
                return super.setBlock(x, y, z, block, meta, flags);
            }
        };
        world.setBlock(1, 64, 0, Blocks.dirt, 2, 3);
        ItemStack original = new ItemStack(Items.stick, 3);
        ProcessingInputs inputs = new ProcessingInputs(Collections.singletonList(original), Collections.emptyList());
        inputs.items[0].stackSize--;
        ProcessingWorldChanges changes = new ProcessingWorldChanges(world);
        changes.setBlock(1, 64, 0, Blocks.stone, 0);
        changes.setBlock(2, 64, 0, Blocks.stone, 0);
        WorldProcessingLogic logic = new WorldProcessingLogic(request -> new WorldProcessingLogic.Operation(7, 20,
                new ItemStack[0], new FluidStack[0], () -> changes.commit() && inputs.commit()));
        assertEquals(CheckRecipeResult.INPUT_CHANGED, logic.process(request(32, 1, 0, 10000)).status);
        assertSame(Blocks.dirt, world.getBlock(1, 64, 0));
        assertEquals(2, world.getBlockMetadata(1, 64, 0));
        assertSame(Blocks.air, world.getBlock(2, 64, 0));
        assertEquals(3, original.stackSize);
    }

    private static void changedBlock() {
        SnapshotWorld world = new SnapshotWorld();
        world.setBlock(1, 64, 0, Blocks.bedrock, 0, 3);
        ProcessingWorldChanges changes = new ProcessingWorldChanges(world);
        changes.observe(1, 64, 0);
        changes.setBlock(2, 64, 0, Blocks.stone, 0);
        world.setBlock(1, 64, 0, Blocks.dirt, 0, 3);
        assertFalse(changes.commit());
        assertSame(Blocks.air, world.getBlock(2, 64, 0));
        assertSame(Blocks.dirt, world.getBlock(1, 64, 0));
    }

    private static void outputProtection() {
        boolean[] committed = { false };
        WorldProcessingLogic logic = new WorldProcessingLogic(request -> new WorldProcessingLogic.Operation(7, 20,
                new ItemStack[] { new ItemStack(Items.stick) }, new FluidStack[0], () -> committed[0] = true));
        assertEquals(CheckRecipeResult.ITEM_OUTPUT_FULL,
                logic.process(request(32, 1, ProcessingLogic.PROTECT_ITEMS, 10000)).status);
        assertFalse(committed[0]);
        assertTrue(logic.process(request(32, 1, 0, 10000)).status.wasSuccessful());
        assertTrue(committed[0]);
    }

    private static void overflow() {
        boolean[] committed = { false };
        WorldProcessingLogic logic = new WorldProcessingLogic(request -> new WorldProcessingLogic.Operation(
                Long.MAX_VALUE, 20, new ItemStack[0], new FluidStack[0], () -> committed[0] = true));
        assertEquals(CheckRecipeResult.POWER_OVERFLOW, logic.process(request(Long.MAX_VALUE, 2, 0, 10000)).status);
        assertEquals(CheckRecipeResult.POWER_OVERFLOW, logic.process(request(Long.MAX_VALUE, 1, 0, 1000)).status);
        assertEquals(CheckRecipeResult.INSUFFICIENT_POWER, logic.process(request(32, 1, 0, 10000)).status);
        assertFalse(committed[0]);
    }

    private static void mergedWrites() {
        SnapshotWorld world = new SnapshotWorld();
        world.setBlock(1, 64, 0, Blocks.dirt, 0, 3);
        ProcessingWorldChanges changes = new ProcessingWorldChanges(world);
        changes.observe(1, 64, 0);
        changes.setBlock(2, 64, 0, Blocks.stone, 0);
        changes.setBlock(2, 64, 0, Blocks.air, 0);
        changes.setBlock(3, 64, 0, Blocks.stone, 0);
        int before = world.changes;
        assertTrue(changes.commit());
        assertEquals(before + 1, world.changes);
        assertSame(Blocks.stone, world.getBlock(3, 64, 0));
        changes.rollback();
        assertSame(Blocks.air, world.getBlock(3, 64, 0));
    }

    private static class SnapshotWorld extends GT_StructureIntegrationTest.TestWorld {
        @Override
        public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
            metadata.put(key(x, y, z), meta);
            return true;
        }
    }

    private static ProcessingLogic.Request request(long voltage, long amperage, int modes, int efficiency) {
        ProcessingInputs inputs = new ProcessingInputs(Collections.emptyList(), Collections.emptyList());
        ProcessingOutputs outputs = new ProcessingOutputs(
                new gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer("test"));
        return new ProcessingLogic.Request(Collections.singletonList(inputs), voltage, amperage, modes, outputs,
                bound -> 0, efficiency);
    }
}
