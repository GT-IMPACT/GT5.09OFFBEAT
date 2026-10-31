package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HeatingCoil;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ElectricBlastFurnace;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_BlastFurnaceStructureChecks {
    private static final int[] METADATA = {0, 1, 2, 3, 4, 5, 6, 7, 8, 12};
    private static final int[] HEAT = {1801, 2701, 3601, 4501, 5401, 7201, 9001, 10801, 12601, 19801};

    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings1 = Blocks.wool;
        GregTech_API.sBlockCasings5 = Blocks.stained_glass;
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (int tier = 0; tier < METADATA.length; tier++) {
                checkGeometry(facing, tier, hints);
                checkSurvival(facing, tier, hatchItem);
            }
            checkConflictsAndLegacy(facing, hints);
        }
        checkHeatAndOutputs();
        checkTooltip();
    }

    private static MetaTileEntity hatch(int kind) {
        switch (kind) {
            case 0:
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 7:
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 8:
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null) {
                    @Override
                    public void markStorageDirty() {
                        // The isolated world has no chunk storage; keep the real tank fill implementation.
                    }
                };
            case 9:
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            default:
                return (MetaTileEntity) GregTech_API.METATILEENTITIES[32000 + kind].newMetaEntity(null);
        }
    }

    private static int heat(GT_MetaTileEntity_ElectricBlastFurnace machine) throws Exception {
        Field field = GT_MetaTileEntity_ElectricBlastFurnace.class.getDeclaredField("mHeatingCapacity");
        field.setAccessible(true);
        return field.getInt(machine);
    }

    private static void checkGeometry(ForgeDirection facing, int tier, HintProxy hints) throws Exception {
        Furnace f = new Furnace(facing);
        f.machine.construct(new ItemStack(Items.stick, tier + 1), false);
        assertEquals(33, f.world.blocks.size());
        assertFalse(f.recheck());
        for (int[] p : f.coils) {
            assertSame(GregTech_API.sBlockCasings5, f.world.getBlock(p[0], p[1], p[2]));
            assertEquals(METADATA[tier], f.world.getBlockMetadata(p[0], p[1], p[2]));
        }
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals("LV energy preserves the -100 K correction", HEAT[tier] - 100, heat(f.machine));
        assertEquals(1, f.machine.mOutputBusses.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        for (int kind = 0; kind < 4; kind++) {
            f.putCasing(f.bottom.get(kind));
            assertFalse("Missing bottom category " + kind, f.recheck());
            assertEquals(0, heat(f.machine));
            f.putHatch(f.bottom.get(kind), kind);
        }
        f.putCasing(f.muffler);
        assertFalse("Required muffler at top center", f.recheck());
        f.putHatch(f.muffler, 9);
        f.putHatch(f.top.get(0), 8);
        f.putCasing(f.bottom.get(3));
        assertFalse("Top gas output must not satisfy the bottom output requirement", f.recheck());
        f.putHatch(f.top.get(1), 3);
        assertFalse("Neither does a legacy top output bus", f.recheck());
        f.putHatch(f.bottom.get(3), 8);
        assertTrue("Fluid output at the bottom is allowed", f.recheck());
        for (int input : new int[]{4, 5, 7, 2}) {
            f.putHatch(f.bottom.get(2), input);
            assertTrue("Input types, including IMPACT, remain supported", f.recheck());
        }
        f.putHatch(f.bottom.get(1), 6);
        assertTrue(f.recheck());
        assertEquals("Multi-amp voltage affects heat as before", HEAT[tier], heat(f.machine));
        f.requiredHatches();
        f.putHatch(f.top.get(2), 1);
        assertFalse("Top accepts outputs only", f.recheck());
        f.putCasing(f.top.get(2));
        f.putHatch(f.bottom.get(4), 9);
        assertFalse("Muffler cannot go in the bottom layer", f.recheck());
        f.putCasing(f.bottom.get(4));
        for (int[] p : f.air) {
            f.putCasing(p);
            assertFalse("Both internal centers must remain air", f.recheck());
            f.world.setBlockToAir(p[0], p[1], p[2]);
        }
        int[] coil = f.coils.get(15);
        f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, METADATA[(tier + 1) % 10], 0);
        assertFalse("Mixed rings must not form", f.recheck());
        assertEquals(0, heat(f.machine));
        f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, 11, 0);
        assertFalse("Fusion coil is not a heating coil", f.recheck());
        f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, METADATA[tier], 0);
        assertTrue(f.recheck());

        int changes = f.world.changes;
        int outputs = f.machine.mOutputHatches.size();
        resetHints(hints);
        f.world.isRemote = true;
        f.machine.construct(new ItemStack(Items.stick, (tier + 1) % 10 + 1), true);
        f.world.isRemote = false;
        assertEquals(35, hints.hints);
        assertEquals(16, hints.hintFrames.size());
        assertEquals(changes, f.world.changes);
        assertEquals(outputs, f.machine.mOutputHatches.size());
        assertEquals(HEAT[tier] - 100, heat(f.machine));
        for (int[] p : f.bottom) {
            assertSame(HintColor.BLUE, hints.hintFrames.get(TestWorld.key(p[0], p[1], p[2])));
        }
        for (int[] p : f.top) {
            assertSame(HintColor.YELLOW, hints.hintFrames.get(TestWorld.key(p[0], p[1], p[2])));
        }
        for (int[] p : f.coils) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(GregTech_API.sBlockCasings5, hints.hintBlocks.get(key));
            assertEquals(METADATA[tier], hints.hintMetadata.get(key).intValue());
            assertFalse(hints.hintFrames.containsKey(key));
        }
        Furnace independent = new Furnace(facing);
        independent.machine.construct(new ItemStack(Items.stick, tier + 1), false);
        assertFalse(independent.recheck());
        assertEquals(0, heat(independent.machine));
        assertTrue(f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, int tier, Item hatchItem) throws Exception {
        GregTech_API.METATILEENTITIES[32600] = hatch(8);
        GregTech_API.METATILEENTITIES[32601] = hatch(9);
        Furnace f = new Furnace(facing);
        ItemStack trigger = new ItemStack(Items.stick, tier + 1);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        // Existing top output is deliberately encountered before bottom positions in the structure walk.
        f.putHatch(f.top.get(0), 8);
        Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32601), new ItemStack(hatchItem, 1, 32600), casingStack(16), coilStack(tier, 16));
        for (int i = 0; i < 45 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int built = f.build(trigger, i == 0 ? 1 : 3, source);
            assertTrue(built > 0 && built <= (i == 0 ? 1 : 3));
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(32, source.consumed);
        assertEquals(1, f.machine.mOutputBusses.size());
        assertEquals(1, f.machine.mOutputHatches.size());
        assertEquals("Optional gas outputs are not auto-installed", 1, source.stacks.get(5).stackSize);
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mInputBusHatches.size());
        assertEquals(1, f.machine.mEnergyHatchesMulti.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(HEAT[tier], heat(f.machine));
        assertEquals(-1, f.build(trigger, 10, source));
        assertEquals(32, source.consumed);
        for (boolean channel : new boolean[]{false, true}) {
            Furnace shell = new Furnace(facing);
            Source blocks = new Source(casingStack(16), coilStack(tier, 16));
            assertEquals(32, shell.build(channel ? trigger : new ItemStack(Items.stick, tier + 1), 100, blocks));
            assertEquals(0, shell.build(trigger, 100, blocks));
            assertEquals(32, blocks.consumed);
            assertFalse(shell.machine.mMachine);
        }
        Furnace partial = new Furnace(facing);
        int[] existing = partial.coils.get(15);
        partial.world.setBlock(existing[0], existing[1], existing[2], GregTech_API.sBlockCasings5, METADATA[tier], 0);
        Source blocks = new Source(casingStack(16), coilStack(tier, 15));
        assertEquals(31, partial.build(new ItemStack(Items.stick, (tier + 1) % 10 + 1), 100, blocks));
        for (int[] p : partial.coils) {
            assertEquals(METADATA[tier], partial.world.getBlockMetadata(p[0], p[1], p[2]));
        }
    }

    private static void checkConflictsAndLegacy(ForgeDirection facing, HintProxy hints) {
        Furnace conflict = new Furnace(facing);
        for (int i = 0; i < conflict.coils.size(); i++) {
            int[] p = conflict.coils.get(i);
            conflict.world.setBlock(p[0], p[1], p[2], GregTech_API.sBlockCasings5, i < 14 ? 0 : 1, 0);
        }
        conflict.putCasing(conflict.air.get(1));
        conflict.putHatch(conflict.top.get(7), 1);
        hints.errorPositions.clear();
        Source blocks = new Source(casingStack(16), coilStack(0, 16));
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(new ItemStack(Items.stick), 1, blocks));
        assertEquals(4, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, blocks.queries);
        conflict.machine.construct(new ItemStack(Items.stick), false);
        assertEquals(34, conflict.world.blocks.size());
        for (int tier = 0; tier < 3; tier++) {
            Furnace f = new Furnace(facing);
            f.machine.construct(new ItemStack(Items.stick, tier + 1), false);
            f.requiredHatches();
            for (int[] p : f.coils) {
                f.world.setBlock(p[0], p[1], p[2], GregTech_API.sBlockCasings1, tier + 12, 0);
            }
            changes = f.world.changes;
            resetHints(hints);
            f.world.isRemote = true;
            f.machine.construct(new ItemStack(Items.stick, 10), true);
            f.world.isRemote = false;
            assertEquals(changes, f.world.changes);
            Source none = new Source();
            assertEquals(-1, f.build(new ItemStack(Items.stick), 1, none));
            assertEquals(0, none.consumed);
            assertEquals(changes + 16, f.world.changes);
            for (int[] p : f.coils) {
                assertSame(GregTech_API.sBlockCasings5, f.world.getBlock(p[0], p[1], p[2]));
                assertEquals(tier, f.world.getBlockMetadata(p[0], p[1], p[2]));
            }
        }
    }

    private static void checkHeatAndOutputs() throws Exception {
        assertSame(HeatingCoil.CUPRONICKEL, HeatingCoil.fromTier(Integer.MIN_VALUE));
        assertSame(HeatingCoil.EXTRATERRESTRIAL, HeatingCoil.fromTier(Integer.MAX_VALUE));
        Furnace f = new Furnace(ForgeDirection.NORTH);
        f.machine.construct(new ItemStack(Items.stick), false);
        f.requiredHatches();
        for (int tier = 1; tier <= 4; tier++) {
            int[] p = f.bottom.get(1);
            f.world.putTile(p[0], p[1], p[2], new TestTile(f.world,
                    new GT_MetaTileEntity_Hatch_Energy("test", tier, "test", null), ForgeDirection.NORTH));
            assertTrue(f.recheck());
            assertEquals(1801 + 100 * (tier - 2), heat(f.machine));
        }
        Method overclock = GT_MetaTileEntity_ElectricBlastFurnace.class.getDeclaredMethod(
                "calculateOverclockednessEBF", int.class, int.class, long.class);
        overclock.setAccessible(true);
        assertEquals((byte) 1, overclock.invoke(f.machine, 120, 200, GT_Values.V[3]));
        assertEquals(480, f.machine.mEUt);
        assertEquals(100, f.machine.mMaxProgresstime);

        Fluid[] gases = {new Fluid("ebf_test_co2"), new Fluid("ebf_test_co"), new Fluid("ebf_test_so2")};
        for (Fluid gas : gases) {
            FluidRegistry.registerFluid(gas);
        }
        Field pollutants = GT_MetaTileEntity_ElectricBlastFurnace.class.getDeclaredField("pollutionFluidStacks");
        pollutants.setAccessible(true);
        pollutants.set(f.machine, new FluidStack[]{new FluidStack(gases[0], 1000),
                new FluidStack(gases[1], 1000), new FluidStack(gases[2], 1000)});
        f.putHatch(f.bottom.get(3), 8);
        f.putHatch(f.top.get(0), 8);
        assertTrue(f.recheck());
        GT_MetaTileEntity_Hatch_Output bottom = (GT_MetaTileEntity_Hatch_Output)
                ((TestTile) f.world.getTileEntity(f.bottom.get(3)[0], 64, f.bottom.get(3)[2])).getMetaTileEntity();
        GT_MetaTileEntity_Hatch_Output top = (GT_MetaTileEntity_Hatch_Output)
                ((TestTile) f.world.getTileEntity(f.top.get(0)[0], 67, f.top.get(0)[2])).getMetaTileEntity();
        for (int mufflerTier : new int[]{1, 4}) {
            GT_MetaTileEntity_Hatch_Muffler muffler = new GT_MetaTileEntity_Hatch_Muffler("test", mufflerTier, "test", null);
            f.world.putTile(f.muffler[0], f.muffler[1], f.muffler[2], new TestTile(f.world, muffler, ForgeDirection.UP));
            assertTrue(f.recheck());
            for (Fluid gas : gases) {
                bottom.mFluid = null;
                top.mFluid = null;
                assertTrue(f.machine.addOutput(new FluidStack(gas, 1000)));
                assertNull("Pollutant must not go into a bottom output", bottom.mFluid);
                assertSame(gas, top.mFluid.getFluid());
                assertEquals(1000 * (105 - muffler.calculatePollutionReduction(100)) / 100, top.mFluid.amount);
            }
        }
        top.mFluid = null;
        assertTrue(f.machine.addOutput(new FluidStack(FluidRegistry.WATER, 1000)));
        assertNull("Ordinary recipe fluid must not go to the top", top.mFluid);
        assertEquals(1000, bottom.mFluid.amount);
    }

    private static void resetHints(HintProxy hints) {
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_ElectricBlastFurnace machine = new GT_MetaTileEntity_ElectricBlastFurnace("multimachine.blastfurnace");
        assertSame(GT_Recipe.GT_Recipe_Map.sBlastRecipes, machine.getRecipeMap());
        assertEquals(20, machine.getPollutionPerTick(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_BlastFurnaceStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hint = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(language.equals("en_US") ? "Electric Blast Furnace" : "Электрическая доменная печь"));
            assertTrue(info.contains("900"));
            assertTrue(info.contains("1800"));
            assertFalse(info.contains("gt_hatch"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 4 x 3" : "3 × 4 × 3"));
            assertTrue(structure.contains(language.equals("en_US") ? "16 Heating Coils" : "16 Нагревательные катушки"));
            assertTrue(structure.contains("gt_hatch"));
            for (EnumChatFormatting color : new EnumChatFormatting[]{EnumChatFormatting.BLUE,
                    EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD}) {
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
        final GT_MetaTileEntity_ElectricBlastFurnace machine = new GT_MetaTileEntity_ElectricBlastFurnace("multimachine.blastfurnace");
        final List<int[]> bottom = new ArrayList<>();
        final List<int[]> coils = new ArrayList<>();
        final List<int[]> top = new ArrayList<>();
        final List<int[]> air = new ArrayList<>();
        final int[] muffler;

        Furnace(ForgeDirection facing) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            muffler = new int[]{-facing.offsetX, 67, -facing.offsetZ};
            for (int y = 65; y <= 66; y++) {
                air.add(new int[]{-facing.offsetX, y, -facing.offsetZ});
            }
            for (int x = -facing.offsetX - 1; x <= -facing.offsetX + 1; x++) {
                for (int z = -facing.offsetZ - 1; z <= -facing.offsetZ + 1; z++) {
                    if (x != 0 || z != 0) {
                        bottom.add(new int[]{x, 64, z});
                    }
                    if (x != muffler[0] || z != muffler[2]) {
                        coils.add(new int[]{x, 65, z});
                        coils.add(new int[]{x, 66, z});
                        top.add(new int[]{x, 67, z});
                    }
                }
            }
        }

        void putCasing(int[] p) {
            world.setBlock(p[0], p[1], p[2], Casings.HEAT_PROOF.getBlock(), Casings.HEAT_PROOF.getMetadata(), 0);
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
