package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AirFilter;
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
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_AirFilterStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasingsNH = Blocks.wool;
        ItemList.AdsorptionFilter.set(new ItemStack(Items.paper));
        ItemList.AdsorptionFilterDirty.set(new ItemStack(Items.map));
        GregTech_API.METATILEENTITIES[32720] = hatch('U');
        GregTech_API.METATILEENTITIES[32721] = hatch('I');
        GregTech_API.METATILEENTITIES[32722] = hatch('O');
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            checkGeometry(facing, hints);
            checkSurvival(facing, hatchItem, hints);
        }
        checkTooltip();
    }

    private static MetaTileEntity hatch(char kind) {
        switch (kind) {
            case 'M':
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'E':
                return new GT_MetaTileEntity_Hatch_Energy("test", 1, "test", null);
            case 'B':
                return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 'Q':
                return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            case 'U':
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            case 'I':
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 'O':
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 'X':
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            case 'A':
                return new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(ForgeDirection facing, HintProxy hints) {
        Filter f = new Filter(facing);
        f.machine.construct(trigger(false), false);
        assertEquals(34, f.world.blocks.size());
        for (int[] p : f.vents) {
            assertEquals(1, f.world.getBlockMetadata(p[0], p[1], p[2]));
        }
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        for (int i = 0; i < 4; i++) {
            f.put(f.bottom.get(i), Casings.AIR_FILTER_TURBINE);
            assertFalse("Each bottom category is required", f.recheck());
            f.putHatch(f.bottom.get(i), "MEBQ".charAt(i));
        }
        f.put(f.mufflers.get(0), Casings.AIR_FILTER_TURBINE);
        assertFalse("At least one muffler", f.recheck());
        for (int i = 0; i < 8; i++) {
            f.putHatch(f.mufflers.get(i), 'U');
            assertTrue(f.recheck());
            assertEquals(i + 1, f.machine.mMufflerHatches.size());
        }
        assertTrue(f.recheck());
        assertEquals("Rechecking does not duplicate hatches", 8, f.machine.mMufflerHatches.size());
        f.putHatch(f.bottom.get(2), 'I');
        assertFalse("Fluid input cannot replace the item bus", f.recheck());
        f.putHatch(f.bottom.get(2), 'X');
        f.putHatch(f.bottom.get(1), 'A');
        assertTrue("IMPACT combined input and multi-amp energy", f.recheck());
        f.putHatch(f.bottom.get(3), 'O');
        assertFalse("Fluid output cannot replace the item bus", f.recheck());
        f.putHatch(f.bottom.get(3), 'Q');
        f.putHatch(f.bottom.get(4), 'I');
        f.putHatch(f.bottom.get(5), 'O');
        assertTrue("Optional existing fluid hatches remain accepted", f.recheck());
        f.putHatch(f.bottom.get(6), 'U');
        assertFalse("Mufflers cannot be placed in the bottom", f.recheck());
        f.put(f.bottom.get(6), Casings.AIR_FILTER_TURBINE);
        f.putHatch(f.mufflers.get(7), 'E');
        assertFalse("Middle positions accept only mufflers", f.recheck());
        f.putHatch(f.mufflers.get(7), 'U');
        for (int[] p : f.vents) {
            f.put(p, Casings.AIR_FILTER_TURBINE);
            assertFalse("All eight vent casings are required", f.recheck());
            f.put(p, Casings.AIR_FILTER_VENT);
        }
        for (int[] p : f.roof) {
            f.putHatch(p, 'U');
            assertFalse("The roof must be solid casings", f.recheck());
            f.put(p, Casings.AIR_FILTER_TURBINE);
        }
        for (int[] p : f.air) {
            f.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
            assertFalse("Internal air and every side/top clearance are mandatory", f.recheck());
            f.clear(p);
        }
        assertTrue(f.recheck());
        Filter other = new Filter(facing);
        other.machine.construct(trigger(false), false);
        assertFalse("Hatch counters are not shared", other.recheck());
        assertTrue(f.recheck());

        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        int changes = f.world.changes;
        f.world.isRemote = true;
        f.machine.construct(trigger(true), true);
        f.world.isRemote = false;
        assertEquals(77, hints.hints);
        assertEquals(16, hints.hintFrames.size());
        assertEquals(changes, f.world.changes);
        assertEquals(8, f.machine.mMufflerHatches.size());
        for (int[] p : f.bottom) {
            assertHint(p, Casings.AIR_FILTER_TURBINE, HintColor.BLUE, hints);
        }
        for (int[] p : f.mufflers) {
            assertHint(p, Casings.AIR_FILTER_TURBINE, HintColor.ORANGE, hints);
        }
        for (int[] p : f.vents) {
            assertHint(p, Casings.AIR_FILTER_VENT, null, hints);
        }
        assertTrue(f.machine.checkRecipe(null));
        assertEquals("Idle energy remains 32 EU/t", -32, f.machine.mEUt);
        assertEquals(200, f.machine.mMaxProgresstime);
        assertEquals(0, f.machine.getPollutionPerTick(null));
    }

    private static void assertHint(int[] p, ICasing casing, HintColor color, HintProxy hints) {
        String key = TestWorld.key(p[0], p[1], p[2]);
        assertSame(casing.getBlock(), hints.hintBlocks.get(key));
        assertEquals(casing.getMetadata(), hints.hintMetadata.get(key).intValue());
        assertSame(color, hints.hintFrames.get(key));
    }

    private static void checkSurvival(ForgeDirection facing, Item hatchItem, HintProxy hints) {
        Filter f = new Filter(facing);
        Source source = new Source(new ItemStack(hatchItem, 1, 32721), new ItemStack(hatchItem, 1, 32722),
                new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32720), stack(Casings.AIR_FILTER_TURBINE, 25),
                stack(Casings.AIR_FILTER_VENT, 8));
        for (int i = 0; i < 40 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int budget = i == 0 ? 1 : 3;
            int placed = f.build(trigger(true), budget, source);
            assertTrue(placed > 0 && placed <= budget);
            assertEquals(placed, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(33, source.consumed);
        assertEquals("Fluid-only input was not selected", 1, source.stacks.get(0).stackSize);
        assertEquals("Fluid-only output was not selected", 1, source.stacks.get(1).stackSize);
        assertEquals(1, f.machine.mInputBusHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(-1, f.build(trigger(true), 100, source));
        assertEquals(33, source.consumed);

        for (boolean channel : new boolean[]{false, true}) {
            Filter shell = new Filter(facing);
            Source blocks = shell.blocks();
            assertEquals("Missing hatches do not prevent casing construction", 33,
                    shell.build(trigger(channel), 100, blocks));
            assertEquals(0, shell.build(trigger(channel), 100, blocks));
            assertEquals(33, blocks.consumed);
            assertFalse(shell.machine.mMachine);
        }
        Filter disabled = new Filter(facing);
        Source disabledSource = new Source(new ItemStack(hatchItem, 1, 32720),
                new ItemStack(hatchItem, 1, 32000), stack(Casings.AIR_FILTER_TURBINE, 25),
                stack(Casings.AIR_FILTER_VENT, 8));
        assertEquals(33, disabled.build(trigger(false), 100, disabledSource));
        assertEquals(1, disabledSource.stacks.get(0).stackSize);
        assertEquals(1, disabledSource.stacks.get(1).stackSize);
        Filter empty = new Filter(facing);
        assertEquals(0, empty.build(trigger(true), 10, new Source()));
        assertFalse("Missing resources are reported", empty.player.messages.isEmpty());

        // Exercise every muffler position: external air belongs to the piece, but facing must still point outward.
        for (int i = 0; i < 8; i++) {
            Filter port = new Filter(facing);
            port.machine.construct(trigger(false), false);
            port.requiredHatches();
            port.put(port.mufflers.get(0), Casings.AIR_FILTER_TURBINE);
            int[] p = port.mufflers.get(i);
            port.clear(p);
            Source muffler = new Source(new ItemStack(hatchItem, 1, 32720));
            assertEquals(1, port.build(trigger(true), 10, muffler));
            assertTrue(port.recheck());
            int dx = p[0] + facing.offsetX;
            int dz = p[2] + facing.offsetZ;
            ForgeDirection outward = dx != 0 ? (dx < 0 ? ForgeDirection.WEST : ForgeDirection.EAST)
                    : (dz < 0 ? ForgeDirection.NORTH : ForgeDirection.SOUTH);
            assertEquals(outward.ordinal(), ((TestTile) port.world.getTileEntity(p[0], p[1], p[2])).getFrontFacing());
            assertEquals(-1, port.build(trigger(true), 10, muffler));
            assertEquals(1, muffler.consumed);
        }

        Filter conflict = new Filter(facing);
        List<int[]> blocked = new ArrayList<>(conflict.air);
        blocked.add(conflict.vents.get(0));
        blocked.add(conflict.bottom.get(7));
        for (int[] p : blocked) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        Source untouched = conflict.blocks();
        int changes = conflict.world.changes;
        hints.errorPositions.clear();
        assertEquals(0, conflict.build(trigger(true), 1, untouched));
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, untouched.queries);
        for (int[] p : blocked) {
            assertTrue(hints.errorPositions.contains(TestWorld.key(p[0], p[1], p[2])));
        }
        conflict.machine.construct(trigger(false), false);
        assertTrue("Creative construction bypasses the preflight", conflict.world.changes > changes);
        for (int[] p : blocked) {
            assertSame(Blocks.bedrock, conflict.world.getBlock(p[0], p[1], p[2]));
        }
    }

    private static void checkTooltip() throws Exception {
        Filter f = new Filter(ForgeDirection.NORTH);
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_AirFilterStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            String hints = String.join("\n", f.machine.getStructureDescription(trigger(false)));
            assertTrue(info.contains(language.equals("en_US") ? "Electric Air Filter" : "Электрический воздушный фильтр"));
            assertTrue(info.contains("10000"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 4 x 3" : "3 × 4 × 3"));
            assertTrue(structure.contains(language.equals("en_US") ? "Two full" : "Два полных"));
            assertTrue(structure.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            for (EnumChatFormatting color : new EnumChatFormatting[]{EnumChatFormatting.BLUE, EnumChatFormatting.GOLD}) {
                String marker = color + "■" + EnumChatFormatting.RESET + " - ";
                assertTrue(structure.contains(marker));
                assertTrue(hints.contains(marker));
            }
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

    private static final class Filter {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_AirFilter machine = new GT_MetaTileEntity_AirFilter("multimachine.airfilter");
        final List<int[]> bottom = new ArrayList<>();
        final List<int[]> roof = new ArrayList<>();
        final List<int[]> vents = new ArrayList<>();
        final List<int[]> mufflers = new ArrayList<>();
        final List<int[]> air = new ArrayList<>();

        Filter(ForgeDirection facing) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            // Independent coordinates derived from the original structure, not the new shape strings.
            for (int y = 0; y < 6; y++) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        int[] p = {x - facing.offsetX, 64 + y, z - facing.offsetZ};
                        if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                            if ((y == 1 || y == 2) && Math.abs(x) != Math.abs(z)) {
                                air.add(p);
                            }
                        } else if (y >= 4 || ((y == 1 || y == 2) && x == 0 && z == 0)) {
                            air.add(p);
                        } else if (y == 3) {
                            roof.add(p);
                        } else if (y == 0) {
                            if (p[0] != 0 || p[2] != 0) {
                                bottom.add(p);
                            }
                        } else if (x != 0 && z != 0) {
                            vents.add(p);
                        } else {
                            mufflers.add(p);
                        }
                    }
                }
            }
        }

        void put(int[] p, ICasing casing) {
            world.setBlock(p[0], p[1], p[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        void clear(int[] p) {
            world.setBlockToAir(p[0], p[1], p[2]);
        }

        void putHatch(int[] p, char kind) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void requiredHatches() {
            for (int i = 0; i < 4; i++) {
                putHatch(bottom.get(i), "MEBQ".charAt(i));
            }
            putHatch(mufflers.get(0), 'U');
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(machine.getBaseMetaTileEntity(), null);
            return machine.mMachine;
        }

        Source blocks() {
            return new Source(stack(Casings.AIR_FILTER_TURBINE, 25), stack(Casings.AIR_FILTER_VENT, 8));
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
