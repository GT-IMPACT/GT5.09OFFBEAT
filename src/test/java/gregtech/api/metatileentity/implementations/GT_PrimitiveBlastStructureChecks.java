package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BrickedBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BronzeBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_PrimitiveBlastFurnace;
import gregtech.loaders.misc.GT_Achievements;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.alignment.enumerable.ExtendedFacing;
import space.impact.structures.alignment.enumerable.Flip;
import space.impact.structures.alignment.enumerable.Rotation;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_PrimitiveBlastStructureChecks {
    static void run(HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings1 = Blocks.stained_glass;
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        for (boolean bronze : new boolean[]{false, true}) {
            for (ForgeDirection direction : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                checkGeometry(bronze, direction, hints);
                checkSurvival(bronze, direction, hints);
            }
            checkTooltip(bronze);
            checkProcessing(bronze);
        }
    }

    private static void checkGeometry(boolean bronze, ForgeDirection direction, HintProxy hints) {
        Furnace f = new Furnace(bronze, direction);
        assertEquals(6, f.machine.mInventory.length);
        assertSame(bronze ? Casings.BRONZE_PLATED_BRICKS : Casings.FIREBRICKS, f.machine.getCasingType());
        assertSame(f.machine, f.controller.getAlignment());
        assertSame(f.machine, f.controller.getConstructable());
        f.machine.construct(trigger(), false);
        assertEquals(33, f.world.blocks.size());
        assertTrue(f.machine.checkMachine());
        for (int[] p : f.shell) {
            assertSame(f.machine.getCasingType().getBlock(), f.world.getBlock(p[0], p[1], p[2]));
            assertEquals(f.machine.getCasingType().getMetadata(), f.world.getBlockMetadata(p[0], p[1], p[2]));
            f.put(p, Blocks.air, 0);
            assertFalse("Every one of the 32 casings is mandatory", f.machine.checkMachine());
            f.put(p, f.machine.getCasingType().getBlock(), (f.machine.getCasingType().getMetadata() + 1) % 16);
            assertFalse("Wrong casing metadata", f.machine.checkMachine());
            f.casing(p);
        }
        for (int[] p : f.shaft) {
            assertTrue(f.world.isAirBlock(p[0], p[1], p[2]));
            for (Block valid : new Block[]{Blocks.lava, Blocks.flowing_lava}) {
                f.put(p, valid, 3);
                assertTrue("Working lava remains valid", f.machine.checkMachine());
            }
            for (Block invalid : new Block[]{Blocks.water, Blocks.stone}) {
                f.put(p, invalid, 0);
                assertFalse("The central shaft must stay open", f.machine.checkMachine());
            }
            f.put(p, Blocks.air, 0);
        }
        int[] unloaded = f.shell.get(0);
        f.world.unloadedPositions.add(TestWorld.key(unloaded[0], unloaded[1], unloaded[2]));
        assertFalse(f.machine.checkMachine());
        f.world.unloadedPositions.clear();
        assertTrue(f.machine.checkMachine());
        f.machine.mMaxProgresstime = 40;
        f.machine.mProgresstime = 12;
        f.machine.mInventory[0] = new ItemStack(Items.iron_ingot, 5);
        f.put(f.shaft.get(0), Blocks.lava, 1);
        hints.hints = 0;
        hints.hintBlocks.clear(); hints.hintMetadata.clear(); hints.hintFrames.clear();
        int changes = f.world.changes;
        f.world.isRemote = true;
        f.machine.construct(trigger(), true);
        f.world.isRemote = false;
        assertEquals("32 textured casings and three air markers", 35, hints.hints);
        assertEquals(32, hints.hintBlocks.size());
        assertTrue("No hatch colors on ordinary casings", hints.hintFrames.isEmpty());
        assertEquals(changes, f.world.changes);
        assertEquals(12, f.machine.mProgresstime);
        assertEquals(40, f.machine.mMaxProgresstime);
        assertEquals(5, f.machine.mInventory[0].stackSize);
        assertSame(Blocks.lava, f.world.getBlock(f.shaft.get(0)[0], 64, f.shaft.get(0)[2]));
        for (ExtendedFacing facing : ExtendedFacing.VALUES) {
            boolean valid = facing.getDirection().offsetY == 0 && facing.getDirection() != ForgeDirection.UNKNOWN
                    && facing.getRotation() == Rotation.NORMAL && facing.getFlip() == Flip.NONE;
            assertEquals(valid, f.machine.isNewExtendedFacingValid(facing));
            if (!valid) {
                f.machine.setExtendedFacing(facing);
                assertEquals(direction, f.machine.getExtendedFacing().getDirection());
            }
        }
        assertFalse(f.machine.isFacingValid((byte) 6));
        f.machine.mMachine = true;
        f.machine.setExtendedFacing(ExtendedFacing.of(direction.getOpposite()));
        assertFalse(f.machine.mMachine);
        assertEquals(1, f.machine.mUpdate);
        assertEquals(direction.getOpposite().ordinal(), f.controller.getFrontFacing());
        assertEquals(12, f.machine.mProgresstime);
    }

    private static void checkSurvival(boolean bronze, ForgeDirection direction, HintProxy hints) {
        Furnace f = new Furnace(bronze, direction);
        f.casing(f.shell.get(0));
        Source source = f.source(31);
        for (int i = 0; i < 32 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int budget = i % 4 + 1;
            int built = f.build(budget, source);
            assertTrue(built > 0 && built <= budget);
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(31, source.consumed);
        assertEquals(-1, f.build(100, source));
        assertEquals(31, source.consumed);
        Furnace shortSupply = new Furnace(bronze, direction);
        Source few = shortSupply.source(2);
        assertEquals(2, shortSupply.build(100, few));
        assertFalse(shortSupply.machine.mMachine);
        assertFalse(shortSupply.player.messages.isEmpty());
        assertEquals(0, shortSupply.build(100, few));
        assertEquals(2, few.consumed);
        assertEquals(30, shortSupply.build(100, shortSupply.source(30)));
        assertTrue(shortSupply.machine.mMachine);
        Furnace blocked = new Furnace(bronze, direction);
        int[] corner = blocked.shell.get(0);
        int[] center = blocked.shaft.get(0);
        int[] top = blocked.shaft.get(2);
        blocked.put(corner, Blocks.bedrock, 0);
        blocked.put(center, Blocks.tallgrass, 1);
        blocked.put(top, Blocks.stone, 0);
        int changes = blocked.world.changes;
        Source untouched = blocked.source(32);
        hints.errorPositions.clear();
        assertEquals(0, blocked.build(1, untouched));
        assertEquals(changes, blocked.world.changes);
        assertEquals(0, untouched.queries);
        Set<String> errors = new HashSet<>();
        for (int[] p : new int[][]{corner, center, top}) errors.add(TestWorld.key(p[0], p[1], p[2]));
        assertEquals(errors, hints.errorPositions);
        hints.errorPositions.clear();
        hints.player = blocked.player;
        blocked.world.isRemote = true;
        try {
            blocked.machine.construct(trigger(), true);
        } finally {
            blocked.world.isRemote = false;
            hints.player = null;
        }
        assertEquals(errors, hints.errorPositions);
        assertEquals(changes, blocked.world.changes);
        blocked.machine.construct(trigger(), false);
        assertTrue(blocked.world.changes > changes);
        assertSame(Blocks.bedrock, blocked.world.getBlock(corner[0], corner[1], corner[2]));
        assertSame(Blocks.tallgrass, blocked.world.getBlock(center[0], center[1], center[2]));
        assertFalse(blocked.machine.checkMachine());
        Furnace unloaded = new Furnace(bronze, direction);
        int[] unavailable = unloaded.shell.get(31);
        unloaded.world.unloadedPositions.add(TestWorld.key(unavailable[0], unavailable[1], unavailable[2]));
        Source resources = unloaded.source(32);
        assertEquals(0, unloaded.build(100, resources));
        assertEquals(0, resources.queries);
        assertEquals(1, unloaded.world.blocks.size());
        assertEquals(0, f.build(0, f.source(32)));
    }

    private static void checkTooltip(boolean bronze) throws Exception {
        Furnace f = new Furnace(bronze, ForgeDirection.NORTH);
        String english = null;
        for (String language : new String[]{"en_US", "ru_RU", "en_US"}) {
            try (InputStream stream = GT_PrimitiveBlastStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            assertTrue(f.machine.hasSecondaryDescription());
            assertFalse(info.contains("TileEntity_DESCRIPTION"));
            assertFalse(structure.contains("gt.tooltip"));
            assertTrue(structure.contains("32"));
            assertFalse(structure.contains("gt_hatch"));
            assertTrue(f.machine.getSecondaryDescription().length <= 7);
            assertFalse(info.equals(structure));
            assertTrue(String.join("\n", f.machine.getStructureDescription(trigger())).contains("ImpactStructures"));
            assertTrue(info.contains(bronze ? "800" : "200"));
            if (language.equals("en_US")) {
                if (english == null) english = info; else assertEquals(english, info);
            } else assertNotEquals(english, info);
        }
    }

    private static void checkProcessing(boolean bronze) throws Exception {
        GT_Recipe.GT_Recipe_Map map = GT_Recipe.GT_Recipe_Map.sPrimitiveBlastRecipes;
        ItemStack[] inputs = {new ItemStack(Items.apple), new ItemStack(Items.coal), new ItemStack(Items.flint)};
        ItemStack[] outputs = {new ItemStack(Items.iron_ingot), new ItemStack(Items.brick), new ItemStack(Items.gold_nugget)};
        if (map.findRecipe(null, false, 0, null, inputs) == null) {
            assertNotNull(map.addRecipe(false, inputs, outputs, null, null, null, null, 80, 0, 0));
        }
        Furnace f = new Furnace(bronze, ForgeDirection.EAST);
        f.machine.construct(trigger(), false);
        for (int i = 0; i < 3; i++) f.machine.mInventory[i] = inputs[i].copy();
        f.machine.mInventory[3] = new ItemStack(Items.diamond);
        Method recipe = GT_MetaTileEntity_PrimitiveBlastFurnace.class.getDeclaredMethod("checkRecipe");
        recipe.setAccessible(true);
        assertFalse("Blocked output cannot consume inputs", (boolean) recipe.invoke(f.machine));
        assertEquals(1, f.machine.mInventory[0].stackSize);
        f.machine.mInventory[3] = null;
        assertTrue((boolean) recipe.invoke(f.machine));
        assertEquals(bronze ? 20 : 80, f.machine.mMaxProgresstime);
        for (int i = 0; i < 3; i++) assertNull(f.machine.mInventory[i]);
        f.machine.onPostTick(f.controller, 1);
        assertEquals(1, f.machine.mProgresstime);
        for (int i = 0; i < 2; i++) {
            int[] p = f.shaft.get(i);
            assertSame(Blocks.lava, f.world.getBlock(p[0], p[1], p[2]));
        }
        assertTrue(f.machine.checkMachine());
        f.put(f.shell.get(0), Blocks.air, 0);
        f.machine.mUpdate = 0;
        f.machine.onPostTick(f.controller, 1);
        assertFalse(f.machine.mMachine);
        assertEquals("Broken structure pauses the existing recipe", 1, f.machine.mProgresstime);
        f.casing(f.shell.get(0));
        f.machine.mUpdate = 0;
        f.machine.onPostTick(f.controller, 1);
        assertTrue(f.machine.mMachine);
        assertEquals(2, f.machine.mProgresstime);
        NBTTagCompound tag = new NBTTagCompound();
        f.machine.saveNBTData(tag);
        Furnace restored = new Furnace(bronze, ForgeDirection.EAST);
        restored.machine.construct(trigger(), false);
        restored.machine.loadNBTData(tag);
        assertEquals(2, restored.machine.mProgresstime);
        assertEquals(f.machine.mMaxProgresstime, restored.machine.mMaxProgresstime);
        assertEquals(ForgeDirection.EAST, restored.machine.getExtendedFacing().getDirection());
        GT_Achievements previous = GT_Mod.achievements;
        try {
            Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
            Constructor<?> constructor = (Constructor<?>) factoryClass
                    .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                    .invoke(factory, GT_Achievements.class, Object.class.getDeclaredConstructor());
            constructor.setAccessible(true);
            GT_Mod.achievements = (GT_Achievements) constructor.newInstance();
            for (int i = 0; i < 80 && restored.machine.mMaxProgresstime > 0; i++) {
                restored.machine.onPostTick(restored.controller, 1);
            }
        } finally {
            GT_Mod.achievements = previous;
        }
        assertEquals(0, restored.machine.mMaxProgresstime);
        assertEquals(0, restored.machine.mProgresstime);
        for (int i = 0; i < 3; i++) {
            assertTrue(outputs[i].isItemEqual(restored.machine.mInventory[i + 3]));
            assertEquals(1, restored.machine.mInventory[i + 3].stackSize);
        }
        for (int[] p : restored.shaft) assertTrue(restored.world.isAirBlock(p[0], p[1], p[2]));
        assertEquals(6, restored.machine.mInventory.length);
    }

    private static ItemStack trigger() {
        return new ItemStack(Items.stick);
    }

    private static final class Furnace {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_PrimitiveBlastFurnace machine;
        final TestTile controller;
        final ForgeDirection facing;
        final List<int[]> shell = new ArrayList<>();
        final List<int[]> shaft = new ArrayList<>();

        Furnace(boolean bronze, ForgeDirection facing) {
            this.facing = facing;
            machine = bronze ? new GT_MetaTileEntity_BronzeBlastFurnace("bronzemachine.blastfurnace")
                    : new GT_MetaTileEntity_BrickedBlastFurnace("multimachine.brickedblastfurnace");
            controller = new TestTile(world, machine, facing);
            world.putTile(0, 64, 0, controller);
            for (int c = 0; c < 3; c++) for (int b = -2; b <= 1; b++) for (int a = -1; a <= 1; a++) {
                if (a == 0 && b == 0 && c == 0) continue;
                if (a == 0 && c == 1 && b <= 0) continue;
                shell.add(pos(a, b, c));
            }
            for (int b = 0; b >= -2; b--) shaft.add(pos(0, b, 1));
            assertEquals(32, shell.size());
        }

        int[] pos(int a, int b, int c) {
            return new int[]{facing.offsetZ * a - facing.offsetX * c, 64 - b,
                    -facing.offsetX * a - facing.offsetZ * c};
        }

        void put(int[] p, Block block, int meta) {
            world.setBlock(p[0], p[1], p[2], block, meta, 0);
        }

        void casing(int[] p) {
            put(p, machine.getCasingType().getBlock(), machine.getCasingType().getMetadata());
        }

        Source source(int count) {
            return new Source(new ItemStack(machine.getCasingType().getBlock(), count, machine.getCasingType().getMetadata()));
        }

        int build(int budget, Source source) {
            return machine.survivalConstruct(trigger(), budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
