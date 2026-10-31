package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.OrePrefixes;
import gregtech.common.GT_Pollution;
import gregtech.common.GT_Proxy;
import gregtech.common.GT_Server;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Charcoal_Pit;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_CharcoalPitStructureChecks {
    private static Block prefixLog;

    static void run(HintProxy hints) throws Exception {
        GregTech_API.sBlockReinforced = Blocks.stained_hardened_clay;
        OreDictionary.registerOre("logWood", new ItemStack(Blocks.log, 1, OreDictionary.WILDCARD_VALUE));
        OreDictionary.registerOre("logWood", new ItemStack(Blocks.log2, 1, OreDictionary.WILDCARD_VALUE));
        prefixLog = new Block(Material.wood) {};
        prefixLog.setBlockName("structure_test.log");
        prefixLog.setHarvestLevel("axe", 0);
        Block.blockRegistry.addObject(3002, "structure_test:log", prefixLog);
        Item.itemRegistry.addObject(3002, "structure_test:log", new ItemBlock(prefixLog));
        OrePrefixes.log.add(new ItemStack(prefixLog));
        GT_Proxy previousProxy = GT_Mod.gregtechproxy;
        HashMap<ChunkCoordIntPair, int[]> previousPollution = GT_Proxy.dimensionWiseChunkData.get(0);
        try {
            // Pollution needs a server proxy, without running its Forge/event/ore registration constructor.
            Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
            Constructor<?> constructor = (Constructor<?>) factoryClass
                    .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                    .invoke(factory, GT_Server.class, Object.class.getDeclaredConstructor());
            constructor.setAccessible(true);
            GT_Mod.gregtechproxy = (GT_Server) constructor.newInstance();
            GT_Mod.gregtechproxy.mPollution = true;
            GT_Proxy.dimensionWiseChunkData.put(0, new HashMap<>());
            for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                for (int[] size : new int[][]{{0, 1, 0}, {1, 3, 2}, {5, 5, 5}}) {
                    checkGeometry(new Pit(facing, size[0], size[1], size[2]), hints);
                }
                checkSurvival(new Pit(facing, 1, 3, 2), hints);
                checkIrregular(facing);
                checkProcessing(facing);
            }
            checkSurvival(new Pit(ForgeDirection.NORTH, 5, 5, 5), hints);
            checkBounds();
            checkTooltip();
        } finally {
            GT_Mod.gregtechproxy = previousProxy;
            if (previousPollution == null) {
                GT_Proxy.dimensionWiseChunkData.remove(0);
            } else {
                GT_Proxy.dimensionWiseChunkData.put(0, previousPollution);
            }
        }
    }

    private static void checkGeometry(Pit f, HintProxy hints) {
        f.machine.construct(f.trigger(), false);
        assertEquals(f.logs.size() + f.soil.size() + f.bricks.size() + 1, f.world.blocks.size());
        assertTrue(f.recheck());
        assertEquals(6, f.machine.getRepairStatus());
        assertTrue(f.machine.mMaintenanceHatches.isEmpty());
        for (int[] p : f.logs) {
            f.assertBlock(p, Blocks.log, 0);
        }
        for (int[] p : f.soil) {
            f.assertBlock(p, Blocks.dirt, 0);
        }
        for (int[] p : f.bricks) {
            f.assertBlock(p, Blocks.brick_block, 0);
        }
        for (int[] p : new int[][]{f.logs.get(0), f.soil.get(0), f.bricks.get(0)}) {
            Block previous = f.world.getBlock(p[0], p[1], p[2]);
            f.clear(p);
            assertFalse("Logs must be fully sealed and have a brick base", f.recheck());
            f.put(p, Blocks.stone, 0);
            assertFalse("Stone is not logs, soil or bricks", f.recheck());
            f.put(p, previous, 0);
        }
        f.put(f.logs.get(0), prefixLog, 0);
        assertTrue("OrePrefixes fallback accepts an axe-harvested wood log", f.recheck());
        f.put(f.logs.get(0), Blocks.log2, 1);
        f.put(f.soil.get(0), Blocks.grass, 0);
        assertTrue("Mixed logs and grass cover", f.recheck());
        Pit other = new Pit(f.facing, f.halfWidth, f.height, f.halfDepth);
        assertFalse(other.recheck());
        assertTrue(f.recheck());
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        int changes = f.world.changes;
        int pollution = GT_Pollution.getPollution(f.controller);
        f.world.isRemote = true;
        f.machine.construct(f.trigger(), true);
        f.world.isRemote = false;
        assertEquals(f.logs.size() + f.soil.size() + f.bricks.size(), hints.hints);
        assertTrue("No hatch colors on ordinary materials", hints.hintFrames.isEmpty());
        assertEquals(changes, f.world.changes);
        assertEquals(0, f.machine.mMaxProgresstime);
        assertEquals(pollution, GT_Pollution.getPollution(f.controller));
        int[] log = f.logs.get(0);
        String key = TestWorld.key(log[0], log[1], log[2]);
        assertSame(Blocks.log2, hints.hintBlocks.get(key));
        assertEquals(1, hints.hintMetadata.get(key).intValue());
        int[] soil = f.soil.get(0);
        assertSame(Blocks.grass, hints.hintBlocks.get(TestWorld.key(soil[0], soil[1], soil[2])));
    }

    private static void checkSurvival(Pit f, HintProxy hints) {
        f.put(f.logs.get(0), Blocks.log, 3);
        Source source = new Source(new ItemStack(Blocks.stone, 64), new ItemStack(prefixLog, f.logs.size()),
                new ItemStack(Blocks.grass, f.soil.size()), new ItemStack(Blocks.brick_block, f.bricks.size()));
        for (int i = 0; i < 200 && !f.machine.mMachine; i++) {
            int consumed = source.consumed;
            int budget = i == 0 ? 1 : 31;
            int placed = f.build(budget, source);
            assertTrue(placed > 0 && placed <= budget);
            assertEquals(placed, source.consumed - consumed);
        }
        assertTrue(f.machine.mMachine);
        assertTrue(f.recheck());
        int expected = f.logs.size() + f.soil.size() + f.bricks.size() - 1;
        assertEquals(expected, source.consumed);
        assertEquals(64, source.stacks.get(0).stackSize);
        assertEquals(-1, f.build(1000, source));
        assertEquals(expected, source.consumed);
        f.assertBlock(f.logs.get(0), Blocks.log, 3);
        assertEquals("Construction does not ignite the pile", 0, f.machine.mMaxProgresstime);
        Pit empty = new Pit(f.facing, f.halfWidth, f.height, f.halfDepth);
        assertEquals(0, empty.build(10, new Source()));
        assertFalse(empty.player.messages.isEmpty());
        Pit conflict = new Pit(f.facing, f.halfWidth, f.height, f.halfDepth);
        int[][] bad = {conflict.logs.get(conflict.logs.size() - 1), conflict.soil.get(0), conflict.bricks.get(0)};
        for (int[] p : bad) {
            conflict.put(p, Blocks.bedrock, 0);
        }
        Source untouched = conflict.blocks();
        int changes = conflict.world.changes;
        hints.errorPositions.clear();
        assertEquals(0, conflict.build(1, untouched));
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, untouched.queries);
        for (int[] p : bad) {
            assertTrue(hints.errorPositions.contains(TestWorld.key(p[0], p[1], p[2])));
        }
        conflict.machine.construct(conflict.trigger(), false);
        assertTrue(conflict.world.changes > changes);
        for (int[] p : bad) {
            conflict.assertBlock(p, Blocks.bedrock, 0);
        }
    }

    private static void checkIrregular(ForgeDirection facing) {
        Pit f = new Pit(facing, 0, 1, 0);
        List<int[]> logs = new ArrayList<>();
        for (int[] column : new int[][]{{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}}) {
            logs.add(new int[]{column[0], 1, column[1]});
            logs.add(new int[]{column[0], 2, column[1]});
        }
        // The old scanner also permits logs level with the controller, away from its sides.
        logs.add(new int[]{2, 0, 2});
        seal(f, logs);
        assertTrue("L-shaped connected pile with a raised end", f.recheck());
        int pollution = GT_Pollution.getPollution(f.controller);
        assertTrue(f.machine.checkRecipe(null));
        int duration = (int) Math.sqrt(logs.size() * 240000);
        assertEquals(duration, f.machine.mMaxProgresstime);
        assertEquals(pollution + duration * 10, GT_Pollution.getPollution(f.controller));
        int[] unrelated = f.pos(20, 1, 0);
        f.put(unrelated, Blocks.log, 0);
        assertTrue(f.recheck());
        for (int[] p : logs) {
            f.assertBlock(f.pos(p[0], p[1], p[2]), Blocks.log, 0);
        }
        f.machine.mMaxProgresstime = 0; // The base calls checkRecipe again after completing the timer.
        assertFalse(f.machine.checkRecipe(null));
        for (int[] p : logs) {
            f.assertBlock(f.pos(p[0], p[1], p[2]), Casings.BRITTLE_CHARCOAL.getBlock(), 4);
        }
        f.assertBlock(unrelated, Blocks.log, 0);
        assertEquals(pollution + duration * 10, GT_Pollution.getPollution(f.controller));
    }

    private static void seal(Pit f, List<int[]> logs) {
        Set<String> positions = new HashSet<>();
        for (int[] p : logs) {
            positions.add(TestWorld.key(p[0], p[1], p[2]));
            f.put(f.pos(p[0], p[1], p[2]), Blocks.log, 0);
        }
        for (int[] p : logs) {
            for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                int a = p[0] + d[0];
                int b = p[1] + d[1];
                int c = p[2] + d[2];
                if ((a == 0 && b == 0 && c == 0) || positions.contains(TestWorld.key(a, b, c))) {
                    continue;
                }
                f.put(f.pos(a, b, c), d[1] == 1 ? Blocks.brick_block : Blocks.dirt, 0);
            }
        }
    }

    private static void checkBounds() {
        for (int[] size : new int[][]{{6, 1, 0}, {0, 6, 0}, {0, 1, 6}}) {
            Pit invalid = new Pit(ForgeDirection.NORTH, size[0], size[1], size[2]);
            invalid.fill();
            assertFalse("Logs outside the legacy bounds are rejected", invalid.recheck());
        }
        Pit invalid = new Pit(ForgeDirection.NORTH, 1, 2, 1);
        invalid.fill();
        invalid.put(invalid.pos(1, 0, 0), Blocks.log, 0);
        invalid.put(invalid.pos(1, -1, 0), Blocks.dirt, 0);
        assertFalse("Logs cannot touch controller sides", invalid.recheck());
        Pit unloaded = new Pit(ForgeDirection.NORTH, 1, 2, 1);
        unloaded.fill();
        int[] p = unloaded.logs.get(0);
        unloaded.world.unloadedPositions.add(TestWorld.key(p[0], p[1], p[2]));
        assertFalse("Unloaded logs must not be processed", unloaded.recheck());
        assertFalse(unloaded.machine.checkRecipe(null));
        for (int[] log : unloaded.logs) {
            unloaded.assertBlock(log, Blocks.log, 0);
        }
    }

    private static void checkProcessing(ForgeDirection facing) {
        Pit f = new Pit(facing, 1, 2, 1);
        f.fill();
        assertTrue(f.recheck());
        int before = GT_Pollution.getPollution(f.controller);
        assertTrue(f.machine.checkRecipe(null));
        int duration = (int) Math.sqrt(f.logs.size() * 240000);
        assertEquals(duration, f.machine.mMaxProgresstime);
        assertEquals(10000, f.machine.mEfficiency);
        assertEquals(0, f.machine.mEUt);
        assertTrue(f.recheck());
        assertEquals(before + duration * 10, GT_Pollution.getPollution(f.controller));
        for (int[] p : f.logs) {
            f.assertBlock(p, Blocks.log, 0);
        }
        NBTTagCompound nbt = new NBTTagCompound();
        f.machine.saveNBTData(nbt);
        assertTrue(nbt.getBoolean("charcoalPitRunning"));
        Pit restored = new Pit(facing, 1, 2, 1);
        restored.fill();
        restored.machine.loadNBTData(nbt);
        assertTrue(restored.recheck());
        assertEquals(duration, restored.machine.mMaxProgresstime);
        assertEquals(f.machine.getExtendedFacing(), restored.machine.getExtendedFacing());
        restored.machine.mMaxProgresstime = 0;
        assertFalse(restored.machine.checkRecipe(null));
        for (int[] p : restored.logs) {
            restored.assertBlock(p, Casings.BRITTLE_CHARCOAL.getBlock(), 4);
        }
        assertEquals("Reload does not charge pollution twice", before + duration * 10, GT_Pollution.getPollution(f.controller));
        Pit legacy = new Pit(facing, 1, 2, 1);
        legacy.fill();
        nbt.removeTag("charcoalPitRunning");
        legacy.machine.loadNBTData(nbt);
        legacy.machine.mMaxProgresstime = 0;
        assertFalse("Legacy in-flight NBT still finishes the batch", legacy.machine.checkRecipe(null));
        legacy.assertBlock(legacy.logs.get(0), Casings.BRITTLE_CHARCOAL.getBlock(), 4);
        f.clear(f.soil.get(0));
        assertFalse(f.recheck());
        f.controller.enableWorking();
        f.machine.stopMachine();
        assertTrue("Broken or harvested piles keep waiting automatically", f.controller.isAllowedToWork());
        f.put(f.soil.get(0), Blocks.dirt, 0);
        assertTrue(f.recheck());
        assertTrue("Resealing starts a fresh full batch", f.machine.checkRecipe(null));
        f.assertBlock(f.logs.get(0), Blocks.log, 0);
        assertEquals(duration, f.machine.mMaxProgresstime);
        f.clear(f.soil.get(0));
        assertFalse(f.recheck());
        f.controller.disableWorking();
        f.machine.stopMachine();
        assertFalse("Manual disable is preserved", f.controller.isAllowedToWork());
        assertFalse(f.machine.checkRecipe(null));
        f.machine.saveNBTData(nbt);
        assertFalse(nbt.getBoolean("charcoalPitRunning"));
    }

    private static void checkTooltip() throws Exception {
        Pit f = new Pit(ForgeDirection.NORTH, 0, 1, 0);
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_CharcoalPitStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            assertTrue(info.contains(language.equals("en_US") ? "Charcoal Pit" : "Углежогная яма"));
            assertTrue(structure.contains("width"));
            assertTrue(structure.contains("height"));
            assertTrue(structure.contains("depth"));
            assertFalse(structure.contains("gt_hatch"));
            assertTrue("Compact structure tooltip", f.machine.getCurrentDescription(true).length <= 13);
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
            assertFalse(structure.contains("gt.tooltip."));
        }
    }

    private static final class Pit {
        final PitWorld world = new PitWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_Charcoal_Pit machine = new GT_MetaTileEntity_Charcoal_Pit("multimachine.charcoalpile");
        final TestTile controller;
        final ForgeDirection facing;
        final int halfWidth;
        final int height;
        final int halfDepth;
        final List<int[]> logs = new ArrayList<>();
        final List<int[]> soil = new ArrayList<>();
        final List<int[]> bricks = new ArrayList<>();

        Pit(ForgeDirection facing, int halfWidth, int height, int halfDepth) {
            this.facing = facing;
            this.halfWidth = halfWidth;
            this.height = height;
            this.halfDepth = halfDepth;
            controller = new TestTile(world, machine, facing);
            world.putTile(0, 80, 0, controller);
            for (int a = -halfWidth; a <= halfWidth; a++) {
                for (int c = -halfDepth; c <= halfDepth; c++) {
                    bricks.add(pos(a, height + 1, c));
                    if (a != 0 || c != 0) {
                        soil.add(pos(a, 0, c));
                    }
                    for (int b = 1; b <= height; b++) {
                        logs.add(pos(a, b, c));
                    }
                }
            }
            for (int b = 1; b <= height; b++) {
                for (int a = -halfWidth; a <= halfWidth; a++) {
                    soil.add(pos(a, b, -halfDepth - 1));
                    soil.add(pos(a, b, halfDepth + 1));
                }
                for (int c = -halfDepth; c <= halfDepth; c++) {
                    soil.add(pos(-halfWidth - 1, b, c));
                    soil.add(pos(halfWidth + 1, b, c));
                }
            }
        }

        int[] pos(int a, int b, int c) {
            return new int[]{facing.offsetZ * a - facing.offsetX * c, 80 - b,
                    -facing.offsetX * a - facing.offsetZ * c};
        }

        void put(int[] p, Block block, int meta) {
            world.setBlock(p[0], p[1], p[2], block, meta, 0);
        }

        void clear(int[] p) {
            world.setBlockToAir(p[0], p[1], p[2]);
        }

        void assertBlock(int[] p, Block block, int meta) {
            assertSame(block, world.getBlock(p[0], p[1], p[2]));
            assertEquals(meta, world.getBlockMetadata(p[0], p[1], p[2]));
        }

        void fill() {
            for (int[] p : logs) {
                put(p, Blocks.log, 0);
            }
            for (int[] p : soil) {
                put(p, Blocks.dirt, 0);
            }
            for (int[] p : bricks) {
                put(p, Blocks.brick_block, 0);
            }
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(controller, null);
            return machine.mMachine;
        }

        ItemStack trigger() {
            ItemStack trigger = new ItemStack(Items.stick);
            ChannelDataAccessor.setChannelData(trigger, "width", halfWidth + 1);
            ChannelDataAccessor.setChannelData(trigger, "depth", halfDepth + 1);
            ChannelDataAccessor.setChannelData(trigger, "height", height);
            return trigger;
        }

        Source blocks() {
            return new Source(new ItemStack(Blocks.log, logs.size()), new ItemStack(Blocks.dirt, soil.size()),
                    new ItemStack(Blocks.brick_block, bricks.size()));
        }

        int build(int budget, Source source) {
            return machine.survivalConstruct(trigger(), budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }

    private static final class PitWorld extends TestWorld {
        private final Chunk chunk = new Chunk(this, 0, 0);

        @Override
        public Chunk getChunkFromBlockCoords(int x, int z) {
            return chunk;
        }
    }
}
