package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_ModHandler;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_OilCracker;
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
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_OilCrackerStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings1 = Blocks.wool;
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings5 = Blocks.stained_glass;
        GregTech_API.METATILEENTITIES[32704] = hatch(7);
        GregTech_API.METATILEENTITIES[32705] = hatch(8);
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (boolean reversed : new boolean[]{false, true}) {
                checkGeometry(facing, reversed, hints);
                checkSurvival(facing, reversed, hatchItem, hints);
                checkFluidSeparation(facing, reversed);
            }
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
            default:
                return (MetaTileEntity) GregTech_API.METATILEENTITIES[32000 + kind].newMetaEntity(null);
        }
    }

    private static void checkGeometry(ForgeDirection facing, boolean reversed, HintProxy hints) {
        Cracker f = new Cracker(facing, reversed);
        f.machine.construct(trigger(false), false);
        assertEquals(42, f.world.blocks.size());
        assertEquals(25, f.count(Casings.CLEAN_STAINLESS_STEEL));
        assertEquals(16, f.count(Casings.CUPRONICKEL_COIL));
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue("Both opposite-side arrangements must form", f.recheck());
        assertTrue(f.recheck());
        assertEquals(2, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mOutputHatches.size());
        int[][] required = {f.input.get(0), f.middle.get(0), f.output.get(0), f.middle.get(1), f.middle.get(2)};
        int[] kinds = {7, 7, 8, 0, 1};
        for (int i = 0; i < required.length; i++) {
            f.put(required[i], Casings.CLEAN_STAINLESS_STEEL);
            assertFalse("Missing required category " + i, f.recheck());
            f.putHatch(required[i], kinds[i]);
        }
        f.putHatch(f.input.get(0), 2);
        assertFalse("Item-only bus cannot supply hydrocarbons", f.recheck());
        f.putHatch(f.input.get(0), 7);
        f.putHatch(f.middle.get(0), 2);
        assertFalse("Item-only bus cannot supply reagents", f.recheck());
        f.putHatch(f.middle.get(0), 7);
        f.putHatch(f.output.get(0), 3);
        assertFalse("Item-only output bus cannot drain product", f.recheck());
        f.putHatch(f.output.get(0), 8);
        for (int kind : new int[]{4, 5, 7}) {
            f.putHatch(f.input.get(0), kind);
            f.putHatch(f.middle.get(0), kind);
            assertTrue("IMPACT inputs count separately by position", f.recheck());
        }
        f.putHatch(f.middle.get(2), 6);
        assertTrue(f.recheck());
        for (int[] p : new int[][]{f.input.get(1), f.middle.get(3)}) {
            f.putHatch(p, 8);
            assertFalse("Product output in the wrong zone", f.recheck());
            f.put(p, Casings.CLEAN_STAINLESS_STEEL);
        }
        f.putHatch(f.output.get(1), 7);
        assertFalse("Inputs on both ends are invalid", f.recheck());
        f.put(f.output.get(1), Casings.CLEAN_STAINLESS_STEEL);
        for (int[] p : f.air) {
            f.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
            assertFalse("All three interior positions must be empty", f.recheck());
            f.world.setBlockToAir(p[0], p[1], p[2]);
        }
        int[] coil = f.coils.get(0);
        for (int tier = 1; tier < 10; tier++) {
            f.world.setBlock(coil[0], coil[1], coil[2], GregTech_API.sBlockCasings5, tier, 0);
            assertFalse("Only cupronickel coils are accepted", f.recheck());
        }
        f.put(coil, Casings.CUPRONICKEL_COIL);
        f.putHatch(f.input.get(1), 2);
        f.putHatch(f.output.get(1), 3);
        assertEquals(18, f.count(Casings.CLEAN_STAINLESS_STEEL));
        assertTrue("Exactly eighteen casings", f.recheck());
        f.putHatch(f.input.get(2), 2);
        assertFalse("Seventeen casings", f.recheck());
        f.put(f.input.get(2), Casings.CLEAN_STAINLESS_STEEL);
        assertTrue(f.recheck());

        // Deprecated coils are accepted by pure probes; conversion happens only on a valid server check.
        for (int[] p : f.coils) {
            f.world.setBlock(p[0], p[1], p[2], GregTech_API.sBlockCasings1, 12, 0);
        }
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        int changes = f.world.changes;
        int inputs = f.machine.mInputHatches.size();
        f.world.isRemote = true;
        f.machine.construct(trigger(true), true);
        f.world.isRemote = false;
        assertEquals(44, hints.hints);
        assertEquals(changes, f.world.changes);
        assertEquals(inputs, f.machine.mInputHatches.size());
        assertEquals(25, hints.hintFrames.size());
        assertHintGroup(f.input, HintColor.BLUE, hints);
        assertHintGroup(f.middle, HintColor.PURPLE, hints);
        assertHintGroup(f.output, HintColor.ORANGE, hints);
        for (int[] p : f.coils) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.CUPRONICKEL_COIL.getBlock(), hints.hintBlocks.get(key));
            assertEquals(0, hints.hintMetadata.get(key).intValue());
            assertFalse(hints.hintFrames.containsKey(key));
        }
        assertTrue(f.recheck());
        assertEquals(16, f.count(Casings.CUPRONICKEL_COIL));
        Cracker independent = new Cracker(facing, reversed);
        independent.machine.construct(trigger(false), false);
        assertFalse(independent.recheck());
        assertTrue(f.recheck());
    }

    private static void assertHintGroup(List<int[]> positions, HintColor color, HintProxy hints) {
        for (int[] p : positions) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.CLEAN_STAINLESS_STEEL.getBlock(), hints.hintBlocks.get(key));
            assertEquals(1, hints.hintMetadata.get(key).intValue());
            assertSame(color, hints.hintFrames.get(key));
        }
    }

    private static void checkSurvival(ForgeDirection facing, boolean reversed, Item hatchItem, HintProxy hints) {
        for (boolean existingInput : new boolean[]{true, false}) {
            Cracker f = new Cracker(facing, reversed);
            // Either an existing input or an existing output selects the desired arrangement.
            f.putHatch(existingInput ? f.input.get(0) : f.output.get(0), existingInput ? 7 : 8);
            Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                    new ItemStack(hatchItem, existingInput ? 1 : 2, 32005),
                    new ItemStack(hatchItem, existingInput ? 1 : 0, 32705),
                    stack(Casings.CLEAN_STAINLESS_STEEL, 25), stack(Casings.CUPRONICKEL_COIL, 16));
            for (int i = 0; i < 60 && !f.machine.mMachine; i++) {
                int before = source.consumed;
                int budget = i == 0 ? 1 : 3;
                int built = f.build(trigger(true), budget, source);
                assertTrue("Construction progresses within budget", built > 0 && built <= budget);
                assertEquals(built, source.consumed - before);
            }
            assertTrue(f.machine.mMachine);
            assertEquals(40, source.consumed);
            assertEquals(2, f.machine.mInputHatches.size() + f.machine.mInputBusHatches.size());
            assertEquals(1, f.machine.mOutputHatches.size());
            assertEquals(1, f.machine.mEnergyHatchesMulti.size());
            assertEquals(-1, f.build(trigger(true), 100, source));
            assertEquals(40, source.consumed);
            // Existing player-placed hatches keep their facing; only automatic placement orients new ones.
            if (existingInput) {
                for (GT_MetaTileEntity_Hatch_Output hatch : f.machine.mOutputHatches) {
                    ForgeDirection out = ForgeDirection.getOrientation(hatch.getBaseMetaTileEntity().getFrontFacing());
                    assertSame(Blocks.air, f.world.getBlock(hatch.getBaseMetaTileEntity().getXCoord() + out.offsetX,
                            hatch.getBaseMetaTileEntity().getYCoord() + out.offsetY,
                            hatch.getBaseMetaTileEntity().getZCoord() + out.offsetZ));
                }
            }
        }
        Cracker empty = new Cracker(facing, reversed);
        Source complete = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32001),
                new ItemStack(hatchItem, 2, 32704), new ItemStack(hatchItem, 1, 32705),
                stack(Casings.CLEAN_STAINLESS_STEEL, 25), stack(Casings.CUPRONICKEL_COIL, 16));
        for (int i = 0; i < 10 && !empty.machine.mMachine; i++) {
            empty.build(trigger(true), 100, complete);
        }
        assertTrue("An empty site selects a consistent arrangement", empty.machine.mMachine);
        assertEquals(41, complete.consumed);
        for (boolean channel : new boolean[]{false, true}) {
            Cracker shell = new Cracker(facing, reversed);
            Source blocks = shell.blocks();
            assertEquals(41, shell.build(trigger(channel), 100, blocks));
            assertEquals(0, shell.build(trigger(channel), 100, blocks));
            assertEquals(41, blocks.consumed);
        }
        Cracker disabled = new Cracker(facing, reversed);
        Source withHatch = new Source(new ItemStack(hatchItem, 1, 32000),
                stack(Casings.CLEAN_STAINLESS_STEEL, 25), stack(Casings.CUPRONICKEL_COIL, 16));
        assertEquals(41, disabled.build(trigger(false), 100, withHatch));
        assertEquals(1, withHatch.stacks.get(0).stackSize);
        Cracker missing = new Cracker(facing, reversed);
        Source shortSupply = new Source(stack(Casings.CLEAN_STAINLESS_STEEL, 25));
        missing.build(trigger(true), 100, shortSupply);
        assertFalse(missing.player.messages.isEmpty());
        Source rest = missing.blocks();
        missing.build(trigger(true), 100, rest);
        assertEquals(41, shortSupply.consumed + rest.consumed);
        Cracker conflict = new Cracker(facing, reversed);
        for (int[] p : new int[][]{conflict.air.get(0), conflict.coils.get(15), conflict.output.get(8)}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source untouched = conflict.blocks();
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger(true), 1, untouched));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, untouched.queries);
        conflict.machine.construct(trigger(false), false);
        assertTrue(conflict.world.changes > changes);
    }

    private static void checkFluidSeparation(ForgeDirection facing, boolean reversed) {
        List<Fluid> reagents = new ArrayList<>();
        reagents.add(GT_ModHandler.getSteam(1).getFluid());
        for (Materials material : new Materials[]{Materials.Hydrogen, Materials.Radon, Materials.Fluorine}) {
            if (material.mGas == null) {
                material.mGas = new Fluid("cracker_test_gas_" + material.mName);
                FluidRegistry.registerFluid(material.mGas);
            }
            reagents.add(material.mGas);
        }
        for (Materials material : new Materials[]{Materials.Zinc, Materials.Nickel, Materials.Niobium, Materials.Silver}) {
            if (material.mPlasma == null) {
                material.mPlasma = new Fluid("cracker_test_plasma_" + material.mName);
                FluidRegistry.registerFluid(material.mPlasma);
            }
            reagents.add(material.mPlasma);
        }
        for (int kind : new int[]{7, 4, 5}) {
            Cracker f = new Cracker(facing, reversed);
            f.machine.construct(trigger(false), false);
            f.requiredHatches();
            GT_MetaTileEntity_Hatch input = f.putHatch(f.input.get(0), kind);
            GT_MetaTileEntity_Hatch middle = f.putHatch(f.middle.get(0), kind);
            assertTrue(f.recheck());
            for (Fluid reagent : reagents) {
                FluidStack oil = new FluidStack(FluidRegistry.LAVA, 500);
                FluidStack steam = new FluidStack(reagent, 100);
                fluid(input, oil);
                fluid(middle, steam);
                List<FluidStack> stored = f.machine.getStoredFluids();
                assertEquals(2, stored.size());
                assertTrue(stored.contains(oil));
                assertTrue(stored.contains(steam));
                fluid(input, steam);
                fluid(middle, oil);
                assertTrue("Both fluids in the wrong zones are ignored", f.machine.getStoredFluids().isEmpty());
            }
        }
    }

    private static void fluid(GT_MetaTileEntity_Hatch hatch, FluidStack fluid) {
        if (hatch instanceof GTMTE_Multi_Hatch_Input) {
            ((GTMTE_Multi_Hatch_Input) hatch).getStoredFluid()[0] = fluid;
        } else {
            hatch.mFluid = fluid;
        }
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_OilCracker machine = new GT_MetaTileEntity_OilCracker("multimachine.cracker");
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_OilCrackerStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hints = String.join("\n", machine.getStructureDescription(trigger(false)));
            assertTrue(info.contains(language.equals("en_US") ? "Oil Cracking Unit" : "Установка крекинга"));
            assertTrue(structure.contains(language.equals("en_US") ? "5 x 3 x 3" : "5 × 3 × 3"));
            assertTrue(structure.contains("18"));
            assertTrue(structure.contains("16"));
            assertTrue(structure.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            for (EnumChatFormatting color : new EnumChatFormatting[]{EnumChatFormatting.BLUE,
                    EnumChatFormatting.DARK_PURPLE, EnumChatFormatting.GOLD}) {
                String marker = color + "■" + EnumChatFormatting.RESET + " - ";
                assertTrue(structure.contains(marker));
                assertTrue(hints.contains(marker));
            }
            assertEquals(3, structure.split("■", -1).length - 1);
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
            assertFalse(structure.contains("gt.tooltip."));
        }
    }

    private static ItemStack trigger(boolean channel) {
        ItemStack trigger = new ItemStack(Items.stick);
        if (channel) {
            ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        }
        return trigger;
    }

    private static ItemStack stack(ICasing casing, int count) {
        return new ItemStack(casing.getBlock(), count, casing.getMetadata());
    }

    private static final class Cracker {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_OilCracker machine = new GT_MetaTileEntity_OilCracker("multimachine.cracker");
        final List<int[]> input = new ArrayList<>();
        final List<int[]> output = new ArrayList<>();
        final List<int[]> middle = new ArrayList<>();
        final List<int[]> coils = new ArrayList<>();
        final List<int[]> air = new ArrayList<>();
        final List<int[]> positions = new ArrayList<>();

        Cracker(ForgeDirection facing, boolean reversed) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            // Reconstruct the original world-coordinate loops independently of the declarative shape.
            for (int along = -1; along <= 1; along++) {
                for (int y = -1; y <= 1; y++) {
                    for (int across = -2; across <= 2; across++) {
                        int x = -facing.offsetX + (facing.offsetX == 0 ? across : along);
                        int z = -facing.offsetZ + (facing.offsetX == 0 ? along : across);
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        int[] p = {x, 64 + y, z};
                        positions.add(p);
                        if (y == 0 && along == 0 && Math.abs(across) <= 1) {
                            air.add(p);
                        } else if (Math.abs(across) == 1) {
                            coils.add(p);
                        } else if (across == 0) {
                            middle.add(p);
                        } else if ((across < 0) != reversed) {
                            input.add(p);
                        } else {
                            output.add(p);
                        }
                    }
                }
            }
        }

        void put(int[] p, ICasing casing) {
            world.setBlock(p[0], p[1], p[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        GT_MetaTileEntity_Hatch putHatch(int[] p, int kind) {
            MetaTileEntity hatch = hatch(kind);
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch, ForgeDirection.NORTH));
            return (GT_MetaTileEntity_Hatch) hatch;
        }

        void requiredHatches() {
            putHatch(input.get(0), 7);
            putHatch(middle.get(0), 7);
            putHatch(output.get(0), 8);
            putHatch(middle.get(1), 0);
            putHatch(middle.get(2), 1);
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
            return new Source(stack(Casings.CLEAN_STAINLESS_STEEL, 25), stack(Casings.CUPRONICKEL_COIL, 16));
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
