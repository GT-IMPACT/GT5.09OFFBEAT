package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiblockCentrifuge;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_CentrifugeStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings2 = Blocks.stained_glass;
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            checkGeometry(facing, hints);
            checkSurvival(facing, hatchItem, hints);
        }
        checkTooltip();
    }

    private static MetaTileEntity hatch(int kind) {
        switch (kind) {
            case 0:
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 1:
                return new GT_MetaTileEntity_Hatch_Energy("test", 1, "test", null);
            case 2:
                return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 3:
                return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            case 4:
                return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 5:
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            case 6:
                return new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
            case 7:
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 8:
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(ForgeDirection facing, HintProxy hints) {
        Centrifuge f = new Centrifuge(facing);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals(27, f.world.blocks.size());
        assertEquals(25, f.count(Casings.CLEAN_STAINLESS_STEEL));
        assertEquals(1, f.count(Casings.MOTOR));
        assertFalse("The documented hatches are required", f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mEnergyHatches.size());
        assertEquals(1, f.machine.mInputBusses.size());
        assertEquals(1, f.machine.mOutputBusses.size());
        for (int kind = 0; kind < 4; kind++) {
            f.put(f.ordinary.get(kind), Casings.CLEAN_STAINLESS_STEEL);
            assertFalse("Missing category " + kind, f.recheck());
            f.putHatch(f.ordinary.get(kind), kind);
            assertTrue(f.recheck());
        }
        for (int input : new int[]{4, 5, 7, 2}) {
            f.putHatch(f.ordinary.get(2), input);
            assertTrue("Bus, fluid and IMPACT inputs", f.recheck());
        }
        f.putHatch(f.ordinary.get(1), 6);
        f.putHatch(f.ordinary.get(3), 8);
        assertTrue("IMPACT energy and fluid output", f.recheck());
        f.requiredHatches();
        for (int[] position : f.ordinary.subList(4, f.ordinary.size())) {
            f.putHatch(position, 2);
            assertTrue("Every shell position accepts hatches", f.recheck());
            f.put(position, Casings.CLEAN_STAINLESS_STEEL);
        }
        f.world.setBlock(f.center[0], f.center[1], f.center[2], Casings.MOTOR.getBlock(), 10, 0);
        assertFalse("The motor metadata must match", f.recheck());
        f.world.setBlockToAir(f.center[0], f.center[1], f.center[2]);
        assertFalse("The center must contain a motor", f.recheck());
        f.put(f.center, Casings.CLEAN_STAINLESS_STEEL);
        assertFalse("A shell casing cannot replace the motor", f.recheck());
        f.putHatch(f.center, 1);
        assertFalse("Hatches cannot replace the motor", f.recheck());
        f.put(f.center, Casings.MOTOR);
        f.put(f.ordinary.get(4), Casings.MOTOR);
        assertFalse("A motor cannot replace shell casings", f.recheck());
        f.put(f.ordinary.get(4), Casings.CLEAN_STAINLESS_STEEL);
        for (int i = 4; i < 17; i++) {
            f.putHatch(f.ordinary.get(i), 2);
        }
        assertEquals(8, f.count(Casings.CLEAN_STAINLESS_STEEL));
        assertTrue("Eight casings", f.recheck());
        f.putHatch(f.ordinary.get(17), 2);
        assertFalse("Seven casings", f.recheck());
        f.put(f.ordinary.get(17), Casings.CLEAN_STAINLESS_STEEL);
        assertTrue(f.recheck());

        int changes = f.world.changes;
        int inputs = f.machine.mInputBusses.size();
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(new ItemStack(Items.stick), true);
        f.world.isRemote = false;
        assertEquals(26, hints.hints);
        assertEquals(changes, f.world.changes);
        assertEquals(inputs, f.machine.mInputBusses.size());
        assertTrue("Hatches can go anywhere in the casing shell: no color restriction", hints.hintFrames.isEmpty());
        for (int[] p : f.positions) {
            ICasing expected = p == f.center ? Casings.MOTOR : Casings.CLEAN_STAINLESS_STEEL;
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(expected.getBlock(), hints.hintBlocks.get(key));
            assertEquals(expected.getMetadata(), hints.hintMetadata.get(key).intValue());
        }
        Centrifuge independent = new Centrifuge(facing);
        independent.machine.construct(new ItemStack(Items.stick), false);
        assertFalse("Counters and hatch lists are not shared", independent.recheck());
        assertTrue(f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, Item hatchItem, HintProxy hints) {
        Centrifuge f = new Centrifuge(facing);
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(trigger, true);
        f.world.isRemote = false;
        assertEquals("Hints on an empty site do not place blocks", 1, f.world.blocks.size());
        assertEquals("Preview contains one motor", 1,
                hints.hintBlocks.values().stream().filter(block -> block == Casings.MOTOR.getBlock()).count());
        Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32003),
                stack(Casings.CLEAN_STAINLESS_STEEL, 25), stack(Casings.MOTOR, 1));
        for (int i = 0; i < 35 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int built = f.build(trigger, i == 0 ? 1 : 3, source);
            assertTrue(built > 0 && built <= (i == 0 ? 1 : 3));
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(26, source.consumed);
        assertEquals(1, f.count(Casings.MOTOR));
        assertEquals(21, f.count(Casings.CLEAN_STAINLESS_STEEL));
        assertEquals(4, source.stacks.get(4).stackSize);
        assertEquals(1, f.machine.mInputBusHatches.size());
        assertEquals(1, f.machine.mEnergyHatchesMulti.size());
        assertEquals(-1, f.build(trigger, 10, source));
        assertEquals(26, source.consumed);

        for (boolean channel : new boolean[]{false, true}) {
            Centrifuge shell = new Centrifuge(facing);
            Source blocks = shell.blocks();
            ItemStack shellTrigger = channel ? trigger : new ItemStack(Items.stick);
            assertEquals(26, shell.build(shellTrigger, 100, blocks));
            assertEquals(0, shell.build(shellTrigger, 100, blocks));
            assertEquals(26, blocks.consumed);
            assertEquals(1, shell.count(Casings.MOTOR));
            assertEquals(25, shell.count(Casings.CLEAN_STAINLESS_STEEL));
            assertFalse(shell.machine.mMachine);
        }
        Centrifuge disabled = new Centrifuge(facing);
        Source withHatches = new Source(new ItemStack(hatchItem, 1, 32000), stack(Casings.CLEAN_STAINLESS_STEEL, 25),
                stack(Casings.MOTOR, 1));
        assertEquals(26, disabled.build(new ItemStack(Items.stick), 100, withHatches));
        assertEquals(1, withHatches.stacks.get(0).stackSize);

        Centrifuge conflict = new Centrifuge(facing);
        for (int[] p : new int[][]{conflict.center, conflict.ordinary.get(24)}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source blocks = conflict.blocks();
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger, 1, blocks));
        assertEquals(2, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, blocks.queries);
        conflict.machine.construct(trigger, false);
        assertEquals("Creative builds around conflicts", 27, conflict.world.blocks.size());
        Centrifuge missing = new Centrifuge(facing);
        Source incomplete = new Source(stack(Casings.CLEAN_STAINLESS_STEEL, 25));
        missing.build(trigger, 100, incomplete);
        assertFalse(missing.machine.mMachine);
        assertFalse(missing.player.messages.isEmpty());
        assertEquals(0, missing.count(Casings.MOTOR));
        Source replenished = missing.blocks();
        missing.build(trigger, 100, replenished);
        assertEquals(1, missing.count(Casings.MOTOR));
        assertEquals(26, incomplete.consumed + replenished.consumed);
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_MultiblockCentrifuge machine = new GT_MetaTileEntity_MultiblockCentrifuge(
                "multimachine.multicentrifuge");
        assertEquals(10000, machine.getMaxEfficiency(null));
        assertEquals(0, machine.getPollutionPerTick(null));
        assertFalse(machine.explodesOnComponentBreak(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_CentrifugeStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hint = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(language.equals("en_US") ? "Large Centrifuge" : "Большая центрифуга"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 3 x 3" : "3 × 3 × 3"));
            assertTrue(structure.contains("8"));
            assertTrue(structure.contains("gt_hatch"));
            assertTrue(hint.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            assertFalse(structure.contains("■"));
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
            assertFalse(structure.contains("gt.tooltip."));
        }
    }

    private static ItemStack stack(ICasing casing, int count) {
        return new ItemStack(casing.getBlock(), count, casing.getMetadata());
    }

    private static final class Centrifuge {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_MultiblockCentrifuge machine =
                new GT_MetaTileEntity_MultiblockCentrifuge("multimachine.multicentrifuge");
        final List<int[]> positions = new ArrayList<>();
        final List<int[]> ordinary = new ArrayList<>();
        int[] center;

        Centrifuge(ForgeDirection facing) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            // Match the old world-coordinate cube, independently of the declarative shape.
            for (int x = -facing.offsetX - 1; x <= -facing.offsetX + 1; x++) {
                for (int z = -facing.offsetZ - 1; z <= -facing.offsetZ + 1; z++) {
                    for (int y = -1; y <= 1; y++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        int[] p = {x, y + 64, z};
                        positions.add(p);
                        int centers = (x == -facing.offsetX ? 1 : 0) + (y == 0 ? 1 : 0)
                                + (z == -facing.offsetZ ? 1 : 0);
                        if (centers == 3) {
                            center = p;
                        } else {
                            ordinary.add(p);
                        }
                    }
                }
            }
        }

        void put(int[] p, ICasing casing) {
            world.setBlock(p[0], p[1], p[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        void putHatch(int[] p, int kind) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void requiredHatches() {
            for (int i = 0; i < 4; i++) {
                putHatch(ordinary.get(i), i);
            }
        }

        int count(ICasing casing) {
            int count = 0;
            for (int[] p : positions) {
                if (world.getBlock(p[0], p[1], p[2]) == casing.getBlock()
                        && world.getBlockMetadata(p[0], p[1], p[2]) == casing.getMetadata()) {
                    count++;
                }
            }
            return count;
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(machine.getBaseMetaTileEntity(), null);
            return machine.mMachine;
        }

        Source blocks() {
            return new Source(stack(Casings.CLEAN_STAINLESS_STEEL, 25), stack(Casings.MOTOR, 1));
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
