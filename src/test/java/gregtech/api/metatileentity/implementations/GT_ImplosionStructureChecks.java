package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ImplosionCompressor;
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

final class GT_ImplosionStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings2 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings3 = Blocks.stained_glass;
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (int mode = 0; mode < 3; mode++) {
                checkGeometry(facing, mode, hints);
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
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            case 5:
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            case 6:
                return new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
            case 7:
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 8:
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 9:
                return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(ForgeDirection facing, int mode, HintProxy hints) {
        Implosion f = new Implosion(facing);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals(26, f.world.blocks.size());
        for (int i = 0; i < f.shell.size(); i++) {
            int[] p = f.shell.get(i);
            assertEquals(0, f.world.getBlockMetadata(p[0], p[1], p[2]));
            assertSame(Casings.SOLID_STEEL.getBlock(), f.world.getBlock(p[0], p[1], p[2]));
            f.putCasing(i, mode);
        }
        assertTrue(f.world.isAirBlock(f.center[0], f.center[1], f.center[2]));
        assertFalse("Required hatches are missing", f.recheck());
        f.requiredHatches();
        assertTrue("Steel, warning signs and mixed shells are valid", f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        for (int kind = 0; kind < 5; kind++) {
            f.putCasing(kind, mode);
            assertFalse("Missing hatch category " + kind, f.recheck());
            f.putHatch(kind, kind);
            assertTrue(f.recheck());
        }
        for (int input : new int[]{7, 9}) {
            f.putHatch(2, input);
            assertFalse("Fluid-only inputs do not satisfy the item input requirement", f.recheck());
        }
        f.putHatch(2, 5);
        f.putHatch(1, 6);
        assertTrue("IMPACT combined input and multi-amp energy", f.recheck());
        f.putHatch(3, 8);
        assertFalse("Fluid output does not replace the output bus", f.recheck());
        f.requiredHatches();
        f.putHatch(5, 7);
        f.putHatch(6, 8);
        f.putHatch(7, 9);
        assertTrue("Preserve optional fluid hatches in legacy structures", f.recheck());
        f.putHatch(8, 2);
        assertTrue("16 casing blocks, including signs", f.recheck());
        f.putHatch(9, 2);
        assertFalse("15 casing blocks", f.recheck());
        f.putCasing(9, mode);
        f.world.setBlock(f.center[0], f.center[1], f.center[2], Blocks.stone, 0, 0);
        assertFalse("Center must remain air", f.recheck());
        f.world.setBlockToAir(f.center[0], f.center[1], f.center[2]);
        int[] invalid = f.shell.get(24);
        f.world.setBlock(invalid[0], invalid[1], invalid[2], Casings.EXPLOSION_HAZARD_SIGN.getBlock(), 5, 0);
        assertFalse("Wrong sign metadata", f.recheck());
        f.putCasing(24, mode);
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
        assertTrue("Hatches are allowed throughout the shell", hints.hintFrames.isEmpty());
        for (int[] p : f.shell) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.SOLID_STEEL.getBlock(), hints.hintBlocks.get(key));
            assertEquals(0, hints.hintMetadata.get(key).intValue());
        }
        Implosion independent = new Implosion(facing);
        independent.machine.construct(new ItemStack(Items.stick), false);
        assertFalse(independent.recheck());
        assertTrue(f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, Item hatchItem, HintProxy hints) {
        GregTech_API.METATILEENTITIES[32400] = hatch(4);
        GregTech_API.METATILEENTITIES[32401] = hatch(7);
        GregTech_API.METATILEENTITIES[32402] = hatch(8);
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        for (ICasing casing : new ICasing[]{Casings.SOLID_STEEL, Casings.EXPLOSION_HAZARD_SIGN}) {
            Implosion f = new Implosion(facing);
            Source source = new Source(new ItemStack(hatchItem, 1, 32401), new ItemStack(hatchItem, 1, 32402),
                    new ItemStack(hatchItem, 1, 32004), new ItemStack(hatchItem, 1, 32000),
                    new ItemStack(hatchItem, 1, 32006), new ItemStack(hatchItem, 1, 32005),
                    new ItemStack(hatchItem, 1, 32003), new ItemStack(hatchItem, 1, 32400), stack(casing, 25));
            for (int i = 0; i < 35 && !f.machine.mMachine; i++) {
                int before = source.consumed;
                int built = f.build(trigger, i == 0 ? 1 : 3, source);
                assertTrue(built > 0 && built <= (i == 0 ? 1 : 3));
                assertEquals(built, source.consumed - before);
            }
            assertTrue(f.machine.mMachine);
            assertEquals(25, source.consumed);
            assertEquals(1, f.machine.mInputBusHatches.size());
            assertEquals(1, f.machine.mEnergyHatchesMulti.size());
            assertEquals(1, f.machine.mMufflerHatches.size());
            assertEquals(1, f.machine.mMaintenanceHatches.size());
            assertEquals(1, f.machine.mOutputBusses.size());
            for (int i = 0; i < 3; i++) {
                assertEquals("No fluid-only hatch is auto-selected", 1, source.stacks.get(i).stackSize);
            }
            assertEquals(-1, f.build(trigger, 10, source));
            assertEquals(25, source.consumed);
        }
        for (boolean channel : new boolean[]{false, true}) {
            Implosion shell = new Implosion(facing);
            Source blocks = new Source(stack(Casings.EXPLOSION_HAZARD_SIGN, 25));
            ItemStack shellTrigger = channel ? trigger : new ItemStack(Items.stick);
            assertEquals("Signs alone can build the shell", 25, shell.build(shellTrigger, 100, blocks));
            assertEquals(0, shell.build(shellTrigger, 100, blocks));
            assertEquals(25, blocks.consumed);
            assertFalse(shell.machine.mMachine);
        }
        Implosion disabled = new Implosion(facing);
        Source available = new Source(new ItemStack(hatchItem, 1, 32400), stack(Casings.SOLID_STEEL, 10),
                stack(Casings.EXPLOSION_HAZARD_SIGN, 15));
        assertEquals("Mix both casing inventories", 25, disabled.build(new ItemStack(Items.stick), 100, available));
        assertEquals(1, available.stacks.get(0).stackSize);
        assertEquals(0, available.stacks.get(1).stackSize);
        assertEquals(0, available.stacks.get(2).stackSize);
        Source untouched = new Source(stack(Casings.SOLID_STEEL, 25));
        assertEquals(0, disabled.build(trigger, 100, untouched));
        assertEquals("Valid signs are skipped instead of replaced", 0, untouched.consumed);
        for (int i = 0; i < 5; i++) {
            int[] p = disabled.shell.get(i);
            disabled.world.setBlockToAir(p[0], p[1], p[2]);
        }
        Source laterHatches = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32001),
                new ItemStack(hatchItem, 1, 32002), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32400));
        for (int i = 0; i < 5; i++) {
            assertEquals(1, disabled.build(trigger, 10, laterHatches));
        }
        assertTrue(disabled.machine.mMachine);
        assertEquals(5, laterHatches.consumed);

        Implosion conflict = new Implosion(facing);
        for (int[] p : new int[][]{conflict.center, conflict.shell.get(24)}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source blocks = new Source(stack(Casings.SOLID_STEEL, 25));
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger, 1, blocks));
        assertEquals(2, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, blocks.queries);
        conflict.machine.construct(trigger, false);
        assertEquals(27, conflict.world.blocks.size());
        Implosion missing = new Implosion(facing);
        Source empty = new Source();
        assertEquals(0, missing.build(trigger, 10, empty));
        assertEquals(0, empty.consumed);
        assertFalse(missing.player.messages.isEmpty());
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_ImplosionCompressor machine = new GT_MetaTileEntity_ImplosionCompressor(
                "multimachine.implosioncompressor");
        assertSame(GT_Recipe.GT_Recipe_Map.sImplosionRecipes, machine.getRecipeMap());
        assertEquals(500, machine.getPollutionPerTick(null));
        assertEquals(10000, machine.getMaxEfficiency(null));
        assertFalse(machine.isFacingValid((byte) 0));
        assertFalse(machine.isFacingValid((byte) 1));
        assertFalse(machine.explodesOnComponentBreak(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_ImplosionStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            assertTrue(info.contains(language.equals("en_US") ? "Implosion Compressor" : "Имплозивный компрессор"));
            assertTrue(info.contains("10000"));
            assertFalse(info.contains("gt_hatch"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 3 x 3" : "3 × 3 × 3"));
            assertTrue(structure.contains(language.equals("en_US") ? "warning signs" : "Знаки взрывоопасности"));
            assertTrue(structure.contains("16"));
            assertTrue(structure.contains("gt_hatch"));
            assertTrue(String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick))).contains("gt_hatch"));
            assertFalse(structure.contains("■"));
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
        }
    }

    private static ItemStack stack(ICasing casing, int count) {
        return new ItemStack(casing.getBlock(), count, casing.getMetadata());
    }

    private static final class Implosion {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_ImplosionCompressor machine =
                new GT_MetaTileEntity_ImplosionCompressor("multimachine.implosioncompressor");
        final List<int[]> shell = new ArrayList<>();
        final int[] center;

        Implosion(ForgeDirection facing) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            center = new int[]{-facing.offsetX, 64, -facing.offsetZ};
            for (int x = center[0] - 1; x <= center[0] + 1; x++) {
                for (int z = center[2] - 1; z <= center[2] + 1; z++) {
                    for (int y = 63; y <= 65; y++) {
                        if (y == 64 && ((x == 0 && z == 0) || (x == center[0] && z == center[2]))) {
                            continue;
                        }
                        shell.add(new int[]{x, y, z});
                    }
                }
            }
        }

        void putCasing(int index, int mode) {
            ICasing casing = mode == 1 || (mode == 2 && index % 2 == 0)
                    ? Casings.EXPLOSION_HAZARD_SIGN : Casings.SOLID_STEEL;
            int[] p = shell.get(index);
            world.setBlock(p[0], p[1], p[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        void putHatch(int index, int kind) {
            int[] p = shell.get(index);
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void requiredHatches() {
            for (int i = 0; i < 5; i++) {
                putHatch(i, i);
            }
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(machine.getBaseMetaTileEntity(), null);
            return machine.mMachine;
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
