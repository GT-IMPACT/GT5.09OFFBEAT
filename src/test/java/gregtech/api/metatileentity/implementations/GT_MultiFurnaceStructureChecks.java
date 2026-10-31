package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiFurnace;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_MultiFurnaceStructureChecks {
    private static final int[] METADATA = {0, 1, 2, 3, 4, 5, 6, 7, 8, 12};
    private static final int[] LEVELS = {1, 2, 4, 8, 16, 16, 16, 16, 16, 32};
    private static final int[] DISCOUNTS = {1, 1, 1, 1, 2, 4, 8, 16, 24, 32};

    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings1 = Blocks.wool;
        GregTech_API.sBlockCasings5 = Blocks.stained_glass;
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (int tier = 0; tier < METADATA.length; tier++) {
                checkGeometry(facing, tier, hints);
                checkSurvival(facing, tier, hatchItem);
            }
            checkConflicts(facing, hints);
            checkLegacy(facing, hints);
        }
        checkTooltip();
    }

    private static MetaTileEntity hatch(int kind) {
        switch (kind) {
            case 0:
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 7:
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 8:
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 9:
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            default:
                return (MetaTileEntity) GregTech_API.METATILEENTITIES[32000 + kind].newMetaEntity(null);
        }
    }

    private static int value(GT_MetaTileEntity_MultiFurnace machine, String name) throws Exception {
        Field field = GT_MetaTileEntity_MultiFurnace.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(machine);
    }

    private static void checkGeometry(ForgeDirection facing, int tier, HintProxy hints) throws Exception {
        Furnace f = new Furnace(facing);
        ItemStack trigger = new ItemStack(Items.stick, tier + 1);
        f.machine.construct(trigger, false);
        assertEquals("Shell and coils, leaving the muffler position empty", 25, f.world.blocks.size());
        assertTrue(f.world.isAirBlock(f.air[0], f.air[1], f.air[2]));
        for (int[] p : f.coils) {
            assertSame(GregTech_API.sBlockCasings5, f.world.getBlock(p[0], p[1], p[2]));
            assertEquals(METADATA[tier], f.world.getBlockMetadata(p[0], p[1], p[2]));
        }
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(LEVELS[tier], value(f.machine, "mLevel"));
        assertEquals(DISCOUNTS[tier], value(f.machine, "mCostDiscount"));
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        for (int kind = 0; kind < 4; kind++) {
            f.put(f.bottom.get(kind), Casings.HEAT_PROOF);
            assertFalse("Missing required bottom hatch " + kind, f.recheck());
            assertEquals(0, value(f.machine, "mLevel"));
            f.putHatch(f.bottom.get(kind), kind);
            assertTrue(f.recheck());
        }
        f.put(f.muffler, Casings.HEAT_PROOF);
        assertFalse("Top center must be a muffler", f.recheck());
        f.putHatch(f.muffler, 9);
        f.putHatch(f.bottom.get(2), 7);
        assertFalse("Fluid input is not an item input", f.recheck());
        f.putHatch(f.bottom.get(2), 4);
        assertFalse("Multi-fluid input is not an item input", f.recheck());
        f.putHatch(f.bottom.get(2), 5);
        f.putHatch(f.bottom.get(1), 6);
        assertTrue("IMPACT combined input and multi-amp energy", f.recheck());
        f.putHatch(f.bottom.get(3), 8);
        assertFalse("Fluid output is not an item output", f.recheck());
        f.requiredHatches();
        f.putHatch(f.top.get(0), 1);
        assertFalse("No energy hatches in the top casing layer", f.recheck());
        f.put(f.top.get(0), Casings.HEAT_PROOF);
        f.putHatch(f.bottom.get(4), 9);
        assertFalse("No mufflers in the bottom layer", f.recheck());
        f.put(f.bottom.get(4), Casings.HEAT_PROOF);
        f.putHatch(f.bottom.get(4), 7);
        f.putHatch(f.bottom.get(5), 8);
        assertTrue("Preserve optional fluid hatches", f.recheck());
        f.put(f.air, Casings.HEAT_PROOF);
        assertFalse("Middle center must be air", f.recheck());
        f.world.setBlockToAir(f.air[0], f.air[1], f.air[2]);
        int[] coil = f.coils.get(7);
        f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, METADATA[(tier + 1) % 10], 0);
        assertFalse("Coil tiers cannot be mixed", f.recheck());
        assertEquals(0, value(f.machine, "mLevel"));
        assertEquals(1, value(f.machine, "mCostDiscount"));
        f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, 9, 0);
        assertFalse("Fusion casing metadata is not a heating coil", f.recheck());
        f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, METADATA[tier], 0);
        assertTrue(f.recheck());

        int changes = f.world.changes;
        int inputs = f.machine.mInputBusses.size();
        resetHints(hints);
        f.world.isRemote = true;
        // Existing coils override a different requested tier without touching machine state.
        f.machine.construct(new ItemStack(Items.stick, (tier + 1) % 10 + 1), true);
        f.world.isRemote = false;
        assertEquals(26, hints.hints);
        assertEquals(8, hints.hintFrames.size());
        assertEquals(changes, f.world.changes);
        assertEquals(inputs, f.machine.mInputBusses.size());
        assertEquals(LEVELS[tier], value(f.machine, "mLevel"));
        for (int[] p : f.bottom) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(HintColor.BLUE, hints.hintFrames.get(key));
            assertSame(Casings.HEAT_PROOF.getBlock(), hints.hintBlocks.get(key));
            assertEquals(11, hints.hintMetadata.get(key).intValue());
        }
        for (int[] p : f.coils) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(GregTech_API.sBlockCasings5, hints.hintBlocks.get(key));
            assertEquals(METADATA[tier], hints.hintMetadata.get(key).intValue());
            assertFalse(hints.hintFrames.containsKey(key));
        }
        for (int[] p : f.top) {
            assertFalse(hints.hintFrames.containsKey(TestWorld.key(p[0], p[1], p[2])));
        }
        Furnace independent = new Furnace(facing);
        independent.machine.construct(trigger, false);
        assertFalse(independent.recheck());
        assertEquals(0, value(independent.machine, "mLevel"));
        assertTrue(f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, int tier, Item hatchItem) throws Exception {
        GregTech_API.METATILEENTITIES[32500] = hatch(9);
        GregTech_API.METATILEENTITIES[32501] = hatch(7);
        GregTech_API.METATILEENTITIES[32502] = hatch(8);
        Furnace f = new Furnace(facing);
        ItemStack trigger = new ItemStack(Items.stick, tier + 1);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        Source source = new Source(new ItemStack(hatchItem, 1, 32501), new ItemStack(hatchItem, 1, 32502),
                new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32500), casingStack(16), coilStack(tier, 8));
        for (int i = 0; i < 35 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int built = f.build(trigger, i == 0 ? 1 : 3, source);
            assertTrue(built > 0 && built <= (i == 0 ? 1 : 3));
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(25, source.consumed);
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mInputBusHatches.size());
        assertEquals(1, f.machine.mEnergyHatchesMulti.size());
        assertEquals(1, f.machine.mOutputBusses.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(1, source.stacks.get(0).stackSize);
        assertEquals(1, source.stacks.get(1).stackSize);
        assertEquals(LEVELS[tier], value(f.machine, "mLevel"));
        assertEquals(DISCOUNTS[tier], value(f.machine, "mCostDiscount"));
        assertEquals(-1, f.build(trigger, 10, source));
        assertEquals(25, source.consumed);
        TestTile muffler = (TestTile) f.world.getTileEntity(f.muffler[0], f.muffler[1], f.muffler[2]);
        assertEquals("Top muffler faces outwards", ForgeDirection.UP.ordinal(), muffler.getFrontFacing());

        for (boolean channel : new boolean[]{false, true}) {
            Furnace shell = new Furnace(facing);
            Source blocks = new Source(casingStack(16), coilStack(tier, 8));
            ItemStack shellTrigger = channel ? trigger : new ItemStack(Items.stick, tier + 1);
            assertEquals("Missing muffler does not prevent building the shell", 24, shell.build(shellTrigger, 100, blocks));
            assertEquals(0, shell.build(shellTrigger, 100, blocks));
            assertEquals(24, blocks.consumed);
            assertFalse(shell.machine.mMachine);
        }
        Furnace partial = new Furnace(facing);
        int[] existing = partial.coils.get(7);
        partial.world.setBlock(existing[0], existing[1], existing[2], GregTech_API.sBlockCasings5, METADATA[tier], 0);
        Source blocks = new Source(casingStack(16), coilStack(tier, 7));
        assertEquals("Installed coils determine construction tier", 23,
                partial.build(new ItemStack(Items.stick, (tier + 1) % 10 + 1), 100, blocks));
        for (int[] p : partial.coils) {
            assertEquals(METADATA[tier], partial.world.getBlockMetadata(p[0], p[1], p[2]));
        }
    }

    private static void checkConflicts(ForgeDirection facing, HintProxy hints) {
        Furnace f = new Furnace(facing);
        for (int i = 0; i < f.coils.size(); i++) {
            int[] p = f.coils.get(i);
            f.world.setBlock(p[0], p[1], p[2], GregTech_API.sBlockCasings5, i < 6 ? 0 : 1, 0);
        }
        f.put(f.air, Casings.HEAT_PROOF);
        f.putHatch(f.top.get(7), 1);
        hints.errorPositions.clear();
        Source source = new Source(casingStack(16), coilStack(0, 8));
        int changes = f.world.changes;
        assertEquals(0, f.build(new ItemStack(Items.stick), 1, source));
        assertEquals("All mixed coils and occupied wrong positions are marked", 4, hints.errorPositions.size());
        assertEquals(changes, f.world.changes);
        assertEquals(0, source.queries);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals("Creative still builds around conflicts", 26, f.world.blocks.size());
        Furnace missing = new Furnace(facing);
        assertEquals(0, missing.build(new ItemStack(Items.stick), 3, new Source()));
        assertFalse(missing.player.messages.isEmpty());
        assertEquals(1, missing.world.blocks.size());
        Furnace clamp = new Furnace(facing);
        clamp.machine.construct(new ItemStack(Items.stick, 64), false);
        for (int[] p : clamp.coils) {
            assertEquals(12, clamp.world.getBlockMetadata(p[0], p[1], p[2]));
        }
    }

    private static void checkLegacy(ForgeDirection facing, HintProxy hints) throws Exception {
        for (int tier = 0; tier < 3; tier++) {
            Furnace f = new Furnace(facing);
            f.machine.construct(new ItemStack(Items.stick, tier + 1), false);
            f.requiredHatches();
            for (int[] p : f.coils) {
                f.world.setBlock(p[0], p[1], p[2], GregTech_API.sBlockCasings1, tier + 12, 0);
            }
            int changes = f.world.changes;
            resetHints(hints);
            f.world.isRemote = true;
            f.machine.construct(new ItemStack(Items.stick, 10), true);
            f.world.isRemote = false;
            assertEquals("Preview must not migrate deprecated coils", changes, f.world.changes);
            for (int[] p : f.coils) {
                assertEquals(tier, hints.hintMetadata.get(TestWorld.key(p[0], p[1], p[2])).intValue());
            }
            Source none = new Source();
            assertEquals("Legacy coils are valid for the construction preflight", -1,
                    f.build(new ItemStack(Items.stick), 1, none));
            assertEquals(0, none.consumed);
            assertEquals(changes + 8, f.world.changes);
            assertEquals(LEVELS[tier], value(f.machine, "mLevel"));
            for (int[] p : f.coils) {
                assertSame(GregTech_API.sBlockCasings5, f.world.getBlock(p[0], p[1], p[2]));
                assertEquals(tier, f.world.getBlockMetadata(p[0], p[1], p[2]));
            }
        }
    }

    private static void resetHints(HintProxy hints) {
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_MultiFurnace machine = new GT_MetaTileEntity_MultiFurnace("multimachine.multifurnace");
        assertSame(GT_Recipe.GT_Recipe_Map.sFurnaceRecipes, machine.getRecipeMap());
        assertEquals(20, machine.getPollutionPerTick(null));
        assertFalse(machine.isFacingValid((byte) 0));
        assertFalse(machine.isFacingValid((byte) 1));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_MultiFurnaceStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hint = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(language.equals("en_US") ? "Multi Smelter" : "Мультиплавильня"));
            assertTrue(info.contains("256"));
            assertTrue(info.contains("400"));
            assertFalse(info.contains("gt_hatch"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 3 x 3" : "3 × 3 × 3"));
            assertTrue(structure.contains(language.equals("en_US") ? "8 Heating Coils" : "8 Нагревательные катушки"));
            assertTrue(structure.contains("gt_hatch"));
            assertTrue(structure.contains("10"));
            for (EnumChatFormatting color : new EnumChatFormatting[]{EnumChatFormatting.BLUE, EnumChatFormatting.GOLD}) {
                String marker = color + "■" + EnumChatFormatting.RESET + " - ";
                assertTrue(structure.contains(marker));
                assertTrue(hint.contains(marker));
            }
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
        }
    }

    private static ItemStack casingStack(int count) {
        return new ItemStack(Casings.HEAT_PROOF.getBlock(), count, Casings.HEAT_PROOF.getMetadata());
    }

    private static ItemStack coilStack(int tier, int count) {
        return new ItemStack(GregTech_API.sBlockCasings5, count, METADATA[tier]);
    }

    private static final class Furnace {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_MultiFurnace machine = new GT_MetaTileEntity_MultiFurnace("multimachine.multifurnace");
        final List<int[]> bottom = new ArrayList<>();
        final List<int[]> coils = new ArrayList<>();
        final List<int[]> top = new ArrayList<>();
        final int[] muffler;
        final int[] air;

        Furnace(ForgeDirection facing) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            muffler = new int[]{-facing.offsetX, 66, -facing.offsetZ};
            air = new int[]{-facing.offsetX, 65, -facing.offsetZ};
            for (int x = -facing.offsetX - 1; x <= -facing.offsetX + 1; x++) {
                for (int z = -facing.offsetZ - 1; z <= -facing.offsetZ + 1; z++) {
                    if (x != 0 || z != 0) {
                        bottom.add(new int[]{x, 64, z});
                    }
                    if (x != air[0] || z != air[2]) {
                        coils.add(new int[]{x, 65, z});
                        top.add(new int[]{x, 66, z});
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
                putHatch(bottom.get(i), i);
            }
            putHatch(muffler, 9);
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
