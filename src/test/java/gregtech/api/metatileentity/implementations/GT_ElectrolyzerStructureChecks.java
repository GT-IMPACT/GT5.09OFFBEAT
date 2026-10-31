package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiblockElectrolyzer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_ElectrolyzerStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings2 = Blocks.wool;
        GregTech_API.sBlockCasings5 = Blocks.stained_glass;
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (int coil = 0; coil < 5; coil++) {
                checkGeometry(facing, coil, hints);
            }
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

    private static void checkGeometry(ForgeDirection facing, int coilIndex, HintProxy hints) {
        Electrolyzer f = new Electrolyzer(facing);
        int[] coil = f.coilPositions.get(coilIndex);
        f.put(coil, Casings.CUPRONICKEL_COIL);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals(27, f.world.blocks.size());
        assertEquals("Existing coil location is preserved without extra coils", 1, f.count(Casings.CUPRONICKEL_COIL));
        assertEquals(24, f.count(Casings.STABLE_TITANIUM));
        assertEquals(1, f.count(Casings.PUMP));
        assertFalse("The documented hatches are required", f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mEnergyHatches.size());
        assertEquals(1, f.machine.mInputBusses.size());
        assertEquals(1, f.machine.mOutputBusses.size());
        for (int kind = 0; kind < 4; kind++) {
            f.put(f.ordinary.get(kind), Casings.STABLE_TITANIUM);
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
        f.put(coil, Casings.STABLE_TITANIUM);
        assertFalse("At least one coil is required", f.recheck());
        for (int tier = 1; tier < 10; tier++) {
            f.world.setBlock(coil[0], coil[1], coil[2], Casings.CUPRONICKEL_COIL.getBlock(), tier, 0);
            assertFalse("Only cupronickel coils are accepted", f.recheck());
        }
        f.put(coil, Casings.CUPRONICKEL_COIL);
        f.world.setBlockToAir(f.center[0], f.center[1], f.center[2]);
        assertFalse("Center contains a pump, not air", f.recheck());
        f.put(f.center, Casings.MOTOR);
        assertFalse("A motor cannot replace the pump", f.recheck());
        f.putHatch(f.center, 2);
        assertFalse("The center cannot contain a hatch", f.recheck());
        f.put(f.center, Casings.PUMP);
        f.put(f.ordinary.get(4), Casings.CUPRONICKEL_COIL);
        assertFalse("A coil on an edge or corner is invalid", f.recheck());
        f.put(f.ordinary.get(4), Casings.STABLE_TITANIUM);
        for (int[] adjacent : f.coilPositions) {
            f.put(adjacent, Casings.CUPRONICKEL_COIL);
        }
        assertTrue("Legacy structures may contain all five coils", f.recheck());
        for (int[] adjacent : f.coilPositions) {
            if (adjacent != coil) {
                f.put(adjacent, Casings.STABLE_TITANIUM);
            }
        }
        for (int i = 4; i < 16; i++) {
            f.putHatch(f.ordinary.get(i), 2);
        }
        assertEquals(8, f.count(Casings.STABLE_TITANIUM));
        assertTrue("Eight casings", f.recheck());
        f.putHatch(f.ordinary.get(16), 2);
        assertFalse("Seven casings", f.recheck());
        f.put(f.ordinary.get(16), Casings.STABLE_TITANIUM);
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
            ICasing expected = p == f.center ? Casings.PUMP
                    : p == coil ? Casings.CUPRONICKEL_COIL : Casings.STABLE_TITANIUM;
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(expected.getBlock(), hints.hintBlocks.get(key));
            assertEquals(expected.getMetadata(), hints.hintMetadata.get(key).intValue());
        }
        Electrolyzer independent = new Electrolyzer(facing);
        independent.machine.construct(new ItemStack(Items.stick), false);
        assertFalse("Counters and hatch lists are not shared", independent.recheck());
        assertTrue(f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, Item hatchItem, HintProxy hints) {
        Electrolyzer f = new Electrolyzer(facing);
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(trigger, true);
        f.world.isRemote = false;
        assertEquals("Hints on an empty site do not place blocks", 1, f.world.blocks.size());
        assertEquals("Preview selects exactly one coil", 1,
                hints.hintBlocks.values().stream().filter(block -> block == Casings.CUPRONICKEL_COIL.getBlock()).count());
        String coilHint = hints.hintBlocks.entrySet().stream()
                .filter(entry -> entry.getValue() == Casings.CUPRONICKEL_COIL.getBlock()).findFirst().get().getKey();
        Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32003),
                stack(Casings.STABLE_TITANIUM, 24), stack(Casings.PUMP, 1), stack(Casings.CUPRONICKEL_COIL, 5));
        for (int i = 0; i < 35 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int built = f.build(trigger, i == 0 ? 1 : 3, source);
            assertTrue(built > 0 && built <= (i == 0 ? 1 : 3));
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(26, source.consumed);
        assertEquals(1, f.count(Casings.CUPRONICKEL_COIL));
        assertEquals(4, source.stacks.get(6).stackSize);
        for (int[] p : f.coilPositions) {
            if (f.world.getBlock(p[0], p[1], p[2]) == Casings.CUPRONICKEL_COIL.getBlock()) {
                assertEquals("Construction follows the preview", coilHint, TestWorld.key(p[0], p[1], p[2]));
            }
        }
        assertEquals(1, f.machine.mInputBusHatches.size());
        assertEquals(1, f.machine.mEnergyHatchesMulti.size());
        assertEquals(-1, f.build(trigger, 10, source));
        assertEquals(26, source.consumed);

        for (boolean channel : new boolean[]{false, true}) {
            Electrolyzer shell = new Electrolyzer(facing);
            Source blocks = shell.blocks();
            ItemStack shellTrigger = channel ? trigger : new ItemStack(Items.stick);
            assertEquals(26, shell.build(shellTrigger, 100, blocks));
            assertEquals(0, shell.build(shellTrigger, 100, blocks));
            assertEquals(26, blocks.consumed);
            assertEquals(1, shell.count(Casings.CUPRONICKEL_COIL));
            assertEquals(24, shell.count(Casings.STABLE_TITANIUM));
            assertFalse(shell.machine.mMachine);
        }
        Electrolyzer disabled = new Electrolyzer(facing);
        Source withHatches = new Source(new ItemStack(hatchItem, 1, 32000), stack(Casings.STABLE_TITANIUM, 24),
                stack(Casings.PUMP, 1), stack(Casings.CUPRONICKEL_COIL, 1));
        assertEquals(26, disabled.build(new ItemStack(Items.stick), 100, withHatches));
        assertEquals(1, withHatches.stacks.get(0).stackSize);

        // If four neighboring positions are occupied, construction must use the fifth, regardless of direction.
        for (int selected = 0; selected < 5; selected++) {
            Electrolyzer partial = new Electrolyzer(facing);
            for (int i = 0; i < 5; i++) {
                if (i != selected) {
                    partial.putHatch(partial.coilPositions.get(i), i % 4);
                }
            }
            Source blocks = partial.blocks();
            partial.build(trigger, 100, blocks);
            int[] coil = partial.coilPositions.get(selected);
            assertSame(Casings.CUPRONICKEL_COIL.getBlock(), partial.world.getBlock(coil[0], coil[1], coil[2]));
            assertEquals(0, partial.world.getBlockMetadata(coil[0], coil[1], coil[2]));
            assertEquals(1, partial.count(Casings.CUPRONICKEL_COIL));
            assertEquals(22, blocks.consumed);
        }

        Electrolyzer conflict = new Electrolyzer(facing);
        for (int[] p : new int[][]{conflict.center, conflict.ordinary.get(19)}) {
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
        Electrolyzer missing = new Electrolyzer(facing);
        Source incomplete = new Source(stack(Casings.STABLE_TITANIUM, 24), stack(Casings.PUMP, 1));
        missing.build(trigger, 100, incomplete);
        assertFalse(missing.machine.mMachine);
        assertFalse(missing.player.messages.isEmpty());
        assertEquals(0, missing.count(Casings.CUPRONICKEL_COIL));
        Source replenished = missing.blocks();
        missing.build(trigger, 100, replenished);
        assertEquals(1, missing.count(Casings.CUPRONICKEL_COIL));
        assertEquals(26, incomplete.consumed + replenished.consumed);
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_MultiblockElectrolyzer machine = new GT_MetaTileEntity_MultiblockElectrolyzer(
                "multimachine.multielectrolyzer");
        assertSame(GT_Recipe.GT_Recipe_Map.sMultiblockElectrolyzerRecipes, machine.getRecipeMap());
        assertEquals(10000, machine.getMaxEfficiency(null));
        assertEquals(0, machine.getPollutionPerTick(null));
        assertFalse(machine.explodesOnComponentBreak(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_ElectrolyzerStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hint = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(language.equals("en_US") ? "Multiblock Electrolyzer" : "Мультиблочный электролизёр"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 3 x 3" : "3 × 3 × 3"));
            assertTrue(structure.contains(language.equals("en_US") ? "five faces" : "пяти сторон"));
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

    private static final class Electrolyzer {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_MultiblockElectrolyzer machine =
                new GT_MetaTileEntity_MultiblockElectrolyzer("multimachine.multielectrolyzer");
        final List<int[]> positions = new ArrayList<>();
        final List<int[]> ordinary = new ArrayList<>();
        final List<int[]> coilPositions = new ArrayList<>();
        int[] center;

        Electrolyzer(ForgeDirection facing) {
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
                        } else if (centers == 2) {
                            coilPositions.add(p);
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
            return new Source(stack(Casings.STABLE_TITANIUM, 24), stack(Casings.PUMP, 1), stack(Casings.CUPRONICKEL_COIL, 1));
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
