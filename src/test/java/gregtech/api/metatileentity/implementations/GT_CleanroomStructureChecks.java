package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Cleanroom;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import ic2.core.block.BlockIC2Door;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_CleanroomStructureChecks {
    private static Block door;

    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockReinforced = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings3 = Blocks.wool;
        GregTech_API.sBlockCasings9 = Blocks.stained_glass;
        // Initialize the real door superclass without IC2's constructor registering a block in an unstarted Forge loader.
        Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
        Constructor<?> doorConstructor = (Constructor<?>) factoryClass
                .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                .invoke(factory, BlockIC2Door.class, BlockDoor.class.getDeclaredConstructor(Material.class));
        doorConstructor.setAccessible(true);
        door = (Block) doorConstructor.newInstance(Material.iron);
        float glass = GT_Values.cleanroomGlass;
        try {
            GT_Values.cleanroomGlass = 35;
            for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                for (int[] size : new int[][]{{1, 4, 1}, {2, 6, 3}, {3, 5, 2}, {7, 15, 7}}) {
                    checkGeometry(new Room(facing, size[0], size[1], size[2]), hints);
                }
                checkSurvival(new Room(facing, 1, 4, 1), hatchItem, hints);
                checkSurvival(new Room(facing, 2, 6, 3), hatchItem, hints);
                checkDoors(facing);
                checkConnections(facing);
            }
            checkSurvival(new Room(ForgeDirection.NORTH, 7, 15, 7), hatchItem, hints);
            for (int height = 4; height <= 15; height++) {
                Room f = new Room(ForgeDirection.NORTH, 2, height, 1);
                f.machine.construct(f.trigger(false), false);
                f.requiredHatches();
                assertTrue("Every supported height", f.recheck());
            }
            checkLimits();
            checkTooltipAndNbt();
        } finally {
            GT_Values.cleanroomGlass = glass;
        }
    }

    private static MetaTileEntity hatch(char kind) {
        switch (kind) {
            case 'M':
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'E':
                return new GT_MetaTileEntity_Hatch_Energy("test", 3, "test", null);
            case 'A':
                return new GT_MetaTileEntity_Hatch_EnergyMulti("test", 3, 4, "test", null);
            case 'H':
                return new GT_MetaTileEntity_BasicHull("test", 1, 1, "test", null);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(Room f, HintProxy hints) {
        int[] interior = f.pos(0, 1, 0);
        f.world.setBlock(interior[0], interior[1], interior[2], Blocks.bedrock, 0, 0);
        f.machine.construct(f.trigger(false), false);
        assertEquals(f.surface.size() + 2, f.world.blocks.size());
        for (int[] p : f.filters) {
            f.assertBlock(p, Casings.FILTER);
        }
        for (int[] p : f.frame) {
            f.assertBlock(p, Casings.PLASCRETE);
        }
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue("Rectangular room with occupied interior", f.recheck());
        f.machine.checkRecipe(null);
        assertEquals(-4, f.machine.mEUt);
        assertEquals(300, f.machine.mEfficiencyIncrease);
        assertEquals(100, f.machine.mMaxProgresstime);
        f.machine.mEfficiency = 10000;
        assertTrue(f.recheck());
        assertEquals(15, f.controller.signal);
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mEnergyHatches.size());
        f.put(f.faces.get(0), Casings.PLASCRETE);
        assertFalse("Maintenance is mandatory", f.recheck());
        assertEquals(0, f.controller.signal);
        f.hatch(f.faces.get(0), 'M');
        f.hatch(f.faces.get(1), 'A');
        assertTrue("Multi-amp energy works", f.recheck());
        f.hatch(f.faces.get(2), 'E');
        assertFalse("Exactly one energy hatch in total", f.recheck());
        f.put(f.faces.get(2), Casings.PLASCRETE);
        f.hatch(f.faces.get(2), 'M');
        assertFalse("Exactly one maintenance hatch", f.recheck());
        f.put(f.faces.get(2), Casings.PLASCRETE);
        for (int[] p : new int[][]{f.frame.get(0), f.frame.get(f.frame.size() - 1), f.faces.get(f.faces.size() - 1)}) {
            f.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 2, 0);
            assertFalse("A matching metadata on the wrong block is insufficient", f.recheck());
            f.put(p, Casings.PLASCRETE);
        }
        if (!f.filters.isEmpty()) {
            int[] p = f.filters.get(0);
            f.world.setBlock(p[0], p[1], p[2], Casings.FILTER.getBlock(), 10, 0);
            assertFalse("Correct filter metadata", f.recheck());
            f.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 11, 0);
            assertFalse("Correct filter block", f.recheck());
            f.put(p, Casings.FILTER);
        }
        assertTrue(f.recheck());
        Room other = new Room(f.facing, f.halfWidth, f.height, f.halfDepth);
        other.machine.construct(other.trigger(false), false);
        assertFalse(other.recheck());
        assertTrue("Counts are not shared", f.recheck());

        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        int changes = f.world.changes;
        f.machine.mEfficiency = 8123;
        f.world.isRemote = true;
        f.machine.construct(f.trigger(true), true);
        f.world.isRemote = false;
        assertEquals(f.surface.size(), hints.hints);
        assertEquals(f.faces.size(), hints.hintFrames.size());
        assertEquals(changes, f.world.changes);
        assertEquals(8123, f.machine.mEfficiency);
        assertEquals(1, f.machine.mEnergyHatchesMulti.size());
        for (int[] p : f.faces) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.PLASCRETE.getBlock(), hints.hintBlocks.get(key));
            assertEquals(2, hints.hintMetadata.get(key).intValue());
            assertSame(HintColor.BLUE, hints.hintFrames.get(key));
        }
        for (int[] p : f.filters) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.FILTER.getBlock(), hints.hintBlocks.get(key));
            assertEquals(11, hints.hintMetadata.get(key).intValue());
            assertFalse(hints.hintFrames.containsKey(key));
        }
    }

    private static void checkDoors(ForgeDirection facing) {
        Room f = new Room(facing, 2, 6, 3);
        f.machine.construct(f.trigger(false), false);
        f.requiredHatches();
        // Test all four wall normals, including the shorter sides of a rectangular room.
        for (int[] local : new int[][]{{0, 4, -3}, {0, 4, 3}, {-2, 4, 0}, {2, 4, 0}}) {
            int[] lower = f.pos(local[0], local[1], local[2]);
            int[] upper = f.pos(local[0], local[1] - 1, local[2]);
            boolean xWall = Math.abs(local[0]) == f.halfWidth ? facing.offsetZ != 0 : facing.offsetX != 0;
            int closedMetadata = xWall ? 0 : 1;
            f.world.setBlock(lower[0], lower[1], lower[2], door, closedMetadata, 0);
            f.world.setBlock(upper[0], upper[1], upper[2], door, 8, 0);
            f.machine.mEfficiency = 10000;
            assertTrue(f.recheck());
            assertEquals("Closed door retains purity", 10000, f.machine.mEfficiency);
            f.world.setBlock(lower[0], lower[1], lower[2], door, closedMetadata | 4, 0);
            assertTrue(f.recheck());
            assertEquals("Open door reduces purity", 9800, f.machine.mEfficiency);
            f.put(upper, Casings.PLASCRETE);
            assertFalse("Orphaned lower door is invalid", f.recheck());
            f.put(lower, Casings.PLASCRETE);
            f.world.setBlock(upper[0], upper[1], upper[2], door, 8, 0);
            assertFalse("Orphaned upper door is invalid", f.recheck());
            f.put(upper, Casings.PLASCRETE);
        }
        for (int i = -1; i <= 1; i++) {
            int[] lower = f.pos(i, 4, -3);
            int[] upper = f.pos(i, 3, -3);
            f.world.setBlock(lower[0], lower[1], lower[2], door, facing.offsetX != 0 ? 0 : 1, 0);
            f.world.setBlock(upper[0], upper[1], upper[2], door, 8, 0);
            assertEquals("At most two complete doors", i < 1, f.recheck());
        }
    }

    private static void checkLimits() {
        Room f = new Room(ForgeDirection.NORTH, 1, 4, 1);
        f.machine.construct(f.trigger(false), false);
        f.requiredHatches();
        for (int i = 2; i < 10; i++) {
            f.put(f.faces.get(i), Casings.CLEANROOM_GLASS);
        }
        assertTrue("Eight glass with 23 plascrete is within floor(23 * .35)", f.recheck());
        f.put(f.faces.get(10), Casings.CLEANROOM_GLASS);
        assertFalse("Nine glass with 22 plascrete exceeds the limit", f.recheck());
        GT_Values.cleanroomGlass = 0;
        for (int[] p : f.faces.subList(2, f.faces.size())) {
            f.put(p, Casings.PLASCRETE);
        }
        assertTrue("A zero glass allowance still permits a solid room", f.recheck());
        f.put(f.faces.get(2), Casings.CLEANROOM_GLASS);
        assertFalse(f.recheck());
        GT_Values.cleanroomGlass = 100;
        for (int[] p : f.faces.subList(2, f.faces.size())) {
            f.put(p, Casings.CLEANROOM_GLASS);
        }
        assertTrue("Minimum 20 plascrete frame", f.recheck());
        f.put(f.frame.get(1), Casings.CLEANROOM_GLASS);
        assertFalse("Nineteen plascrete / broken frame", f.recheck());
        GT_Values.cleanroomGlass = 35;
        Room hulls = new Room(ForgeDirection.NORTH, 2, 6, 3);
        hulls.machine.construct(hulls.trigger(false), false);
        hulls.requiredHatches();
        for (int i = 2; i < 22; i++) {
            hulls.hatch(hulls.faces.get(i), 'H');
        }
        assertTrue("Twenty transfer hulls", hulls.recheck());
        hulls.hatch(hulls.faces.get(22), 'H');
        assertFalse("Not twenty-one hulls", hulls.recheck());
        for (int height : new int[]{3, 16}) {
            Room invalid = new Room(ForgeDirection.NORTH, 1, height, 1);
            invalid.fillShell();
            invalid.requiredHatches();
            assertFalse("Height outside 4..15", invalid.recheck());
        }
        Room tooWide = new Room(ForgeDirection.NORTH, 8, 4, 1);
        tooWide.fillShell();
        tooWide.requiredHatches();
        assertFalse("Width outside 3..15", tooWide.recheck());
    }

    private static void checkConnections(ForgeDirection facing) {
        Room f = new Room(facing, 2, 5, 3);
        GT_MetaTileEntity_BasicMachine_GT_Recipe basic = new GT_MetaTileEntity_BasicMachine_GT_Recipe(
                "test", 1, "test", GT_Recipe.GT_Recipe_Map.sAssemblerRecipes, 1, 1, 1000,
                1, 0, 0, null, "test", "test", "", false, false, 0);
        GT_MetaTileEntity_VacuumFreezer multi = new GT_MetaTileEntity_VacuumFreezer("test");
        f.putMachine(f.pos(0, 1, 0), basic);
        f.putMachine(f.pos(0, 2, 0), multi);
        f.machine.construct(f.trigger(false), false);
        assertNull(basic.mCleanroom);
        assertNull(multi.mCleanroom);
        f.requiredHatches();
        assertTrue(f.recheck());
        assertSame(f.machine, basic.mCleanroom);
        assertSame(f.machine, multi.mCleanroom);
        f.clear(f.faces.get(f.faces.size() - 1));
        assertFalse(f.recheck());
        assertNull("Invalid room detaches basic machines", basic.mCleanroom);
        assertNull("Invalid room detaches multiblocks", multi.mCleanroom);
        f.put(f.faces.get(f.faces.size() - 1), Casings.PLASCRETE);
        assertTrue(f.recheck());
        f.machine.onRemoval();
        assertNull(basic.mCleanroom);
        assertNull(multi.mCleanroom);
    }

    private static void checkSurvival(Room f, Item hatchItem, HintProxy hints) {
        Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                stack(Casings.PLASCRETE, f.surface.size()), stack(Casings.FILTER, f.filters.size()));
        for (int i = 0; i < 200 && !f.machine.mMachine; i++) {
            int consumed = source.consumed;
            int budget = i == 0 ? 1 : 37;
            int placed = f.build(true, budget, source);
            assertTrue(placed > 0 && placed <= budget);
            assertEquals(placed, source.consumed - consumed);
        }
        assertTrue(f.machine.mMachine);
        assertTrue("Detection agrees with the selected construction size", f.recheck());
        assertEquals(f.surface.size(), source.consumed);
        assertEquals(-1, f.build(true, 1000, source));
        assertEquals(f.surface.size(), source.consumed);
        for (GT_MetaTileEntity_Hatch hatch : new GT_MetaTileEntity_Hatch[]{
                f.machine.mMaintenanceHatches.get(0), f.machine.mEnergyHatchesMulti.get(0)}) {
            int x = hatch.getBaseMetaTileEntity().getXCoord();
            int y = hatch.getBaseMetaTileEntity().getYCoord();
            int z = hatch.getBaseMetaTileEntity().getZCoord();
            ForgeDirection direction = ForgeDirection.getOrientation(hatch.getBaseMetaTileEntity().getFrontFacing());
            assertTrue("Hatches face outside the room", f.world.isAirBlock(
                    x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ));
        }
        for (boolean channel : new boolean[]{false, true}) {
            Room shell = new Room(f.facing, f.halfWidth, f.height, f.halfDepth);
            Source blocks = shell.blocks();
            assertEquals(shell.surface.size(), shell.build(channel, 2000, blocks));
            assertEquals(0, shell.build(channel, 2000, blocks));
            assertFalse(shell.machine.mMachine);
        }
        Room disabled = new Room(f.facing, f.halfWidth, f.height, f.halfDepth);
        assertEquals(0, disabled.build(false, 100, new Source(new ItemStack(hatchItem, 1, 32000))));
        assertEquals(1, disabled.world.blocks.size());
        assertFalse(disabled.player.messages.isEmpty());
        Room conflict = new Room(f.facing, f.halfWidth, f.height, f.halfDepth);
        int[][] bad = {conflict.frame.get(0), conflict.faces.get(conflict.faces.size() - 1)};
        for (int[] p : bad) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        Source untouched = conflict.blocks();
        int changes = conflict.world.changes;
        hints.errorPositions.clear();
        assertEquals(0, conflict.build(true, 1, untouched));
        assertEquals(0, untouched.queries);
        assertEquals(changes, conflict.world.changes);
        for (int[] p : bad) {
            assertTrue(hints.errorPositions.contains(TestWorld.key(p[0], p[1], p[2])));
        }
        conflict.machine.construct(conflict.trigger(false), false);
        assertTrue(conflict.world.changes > changes);
        for (int[] p : bad) {
            assertSame(Blocks.bedrock, conflict.world.getBlock(p[0], p[1], p[2]));
        }
    }

    private static void checkTooltipAndNbt() throws Exception {
        Room f = new Room(ForgeDirection.EAST, 2, 6, 3);
        f.machine.mEfficiency = 5432;
        NBTTagCompound nbt = new NBTTagCompound();
        f.machine.saveNBTData(nbt);
        Room loaded = new Room(ForgeDirection.EAST, 2, 6, 3);
        loaded.machine.loadNBTData(nbt);
        assertEquals(5432, loaded.machine.mEfficiency);
        assertEquals(f.machine.getExtendedFacing(), loaded.machine.getExtendedFacing());
        GT_Values.cleanroomGlass = 27.5f;
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_CleanroomStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            String hints = String.join("\n", f.machine.getStructureDescription(f.trigger(false)));
            assertTrue(info.contains(language.equals("en_US") ? "Cleanroom" : "Чистая комната"));
            assertTrue(structure.contains("27.5%"));
            assertTrue(structure.contains("width"));
            assertTrue(structure.contains("depth"));
            assertTrue(structure.contains("height"));
            assertTrue(structure.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            String marker = EnumChatFormatting.BLUE + "■" + EnumChatFormatting.RESET + " - ";
            assertTrue(structure.contains(marker));
            assertTrue(hints.contains(marker));
            assertTrue("Keep the tooltip compact", f.machine.getCurrentDescription(true).length <= 18);
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
            assertFalse(structure.contains("gt.tooltip."));
        }
    }

    private static ItemStack stack(ICasing casing, int count) {
        return new ItemStack(casing.getBlock(), count, casing.getMetadata());
    }

    private static final class Room {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_Cleanroom machine = new GT_MetaTileEntity_Cleanroom("multimachine.cleanroom");
        final ForgeDirection facing;
        final int halfWidth;
        final int height;
        final int halfDepth;
        final List<int[]> surface = new ArrayList<>();
        final List<int[]> frame = new ArrayList<>();
        final List<int[]> faces = new ArrayList<>();
        final List<int[]> filters = new ArrayList<>();
        final RoomTile controller;

        Room(ForgeDirection facing, int halfWidth, int height, int halfDepth) {
            this.facing = facing;
            this.halfWidth = halfWidth;
            this.height = height;
            this.halfDepth = halfDepth;
            controller = new RoomTile(world, machine, facing);
            world.putTile(0, 80, 0, controller);
            for (int b = 0; b < height; b++) {
                for (int a = -halfWidth; a <= halfWidth; a++) {
                    for (int c = -halfDepth; c <= halfDepth; c++) {
                        if (a == 0 && b == 0 && c == 0) {
                            continue;
                        }
                        boolean onWidth = Math.abs(a) == halfWidth;
                        boolean onDepth = Math.abs(c) == halfDepth;
                        if (b != 0 && b != height - 1 && !onWidth && !onDepth) {
                            continue;
                        }
                        int[] p = pos(a, b, c);
                        surface.add(p);
                        if (b == 0 && !onWidth && !onDepth) {
                            filters.add(p);
                        } else if ((onWidth && onDepth) || b == 0) {
                            frame.add(p);
                        } else {
                            faces.add(p);
                        }
                    }
                }
            }
        }

        int[] pos(int a, int b, int c) {
            return new int[]{facing.offsetZ * a - facing.offsetX * c, 80 - b,
                    -facing.offsetX * a - facing.offsetZ * c};
        }

        void put(int[] p, ICasing casing) {
            world.setBlock(p[0], p[1], p[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        void clear(int[] p) {
            world.setBlockToAir(p[0], p[1], p[2]);
        }

        void assertBlock(int[] p, ICasing casing) {
            assertSame(casing.getBlock(), world.getBlock(p[0], p[1], p[2]));
            assertEquals(casing.getMetadata(), world.getBlockMetadata(p[0], p[1], p[2]));
        }

        void putMachine(int[] p, MetaTileEntity machine) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, machine, ForgeDirection.NORTH));
        }

        void hatch(int[] p, char kind) {
            putMachine(p, GT_CleanroomStructureChecks.hatch(kind));
        }

        void requiredHatches() {
            hatch(faces.get(0), 'M');
            hatch(faces.get(1), 'E');
        }

        void fillShell() {
            for (int[] p : surface) {
                put(p, filters.contains(p) ? Casings.FILTER : Casings.PLASCRETE);
            }
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(controller, null);
            return machine.mMachine;
        }

        ItemStack trigger(boolean channel) {
            ItemStack trigger = new ItemStack(Items.stick);
            ChannelDataAccessor.setChannelData(trigger, "width", halfWidth);
            ChannelDataAccessor.setChannelData(trigger, "depth", halfDepth);
            ChannelDataAccessor.setChannelData(trigger, "height", height - 3);
            if (channel) {
                ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
            }
            return trigger;
        }

        Source blocks() {
            return new Source(stack(Casings.PLASCRETE, surface.size()), stack(Casings.FILTER, filters.size()));
        }

        int build(boolean channel, int budget, Source source) {
            return machine.survivalConstruct(trigger(channel), budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }

    private static final class RoomTile extends TestTile {
        byte signal;

        RoomTile(TestWorld world, MetaTileEntity machine, ForgeDirection facing) {
            super(world, machine, facing);
        }

        @Override
        public void setInternalOutputRedstoneSignal(byte side, byte strength) {
            signal = strength;
        }
    }
}
