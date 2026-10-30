package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler_Bronze;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler_Steel;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler_Titanium;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeBoiler_TungstenSteel;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

/** Uses the isolated Forge harness; no game or client UI is launched. */
final class GT_BoilerStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings1 = Blocks.wool;
        GregTech_API.sBlockCasings2 = Blocks.stained_glass;
        GregTech_API.sBlockCasings3 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings4 = Blocks.carpet;
        for (int model = 0; model < 4; model++) {
            for (ForgeDirection direction : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                checkGeometry(model, direction, hints);
            }
            checkSurvival(model, hatchItem, hints);
            checkTooltipAndRates(model);
        }
    }

    private static GT_MetaTileEntity_LargeBoiler machine(int model) {
        switch (model) {
            case 0:
                return new GT_MetaTileEntity_LargeBoiler_Bronze("multimachine.boiler.bronze");
            case 1:
                return new GT_MetaTileEntity_LargeBoiler_Steel("multimachine.boiler.steel");
            case 2:
                return new GT_MetaTileEntity_LargeBoiler_Titanium("multimachine.boiler.titanium");
            case 3:
                return new GT_MetaTileEntity_LargeBoiler_TungstenSteel("multimachine.boiler.tungstensteel");
            default:
                throw new AssertionError(model);
        }
    }

    private static MetaTileEntity hatch(char kind) {
        switch (kind) {
            case 'M':
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'W':
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 'I':
                return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 'U':
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            case 'O':
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 'B':
                return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            case 'Q':
                return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 'X':
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(int model, ForgeDirection direction, HintProxy hints) {
        Boiler f = new Boiler(model, direction);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals("Legacy 3x5x3 volume including controller", 45, f.world.blocks.size());
        for (Position p : f.positions) {
            ICasing descriptor = f.descriptor(p);
            assertSame(descriptor.getBlock(), f.world.getBlock(p.x, p.y, p.z));
            assertEquals(descriptor.getMetadata(), f.world.getBlockMetadata(p.x, p.y, p.z));
        }
        assertFalse("A shell alone does not satisfy the documented hatch requirements", f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(2, HatchElement.Input.count(f.machine));
        assertEquals(1, f.machine.mOutputHatches.size());

        char[] kinds = {'M', 'W', 'I', 'U', 'O'};
        Position[] required = {f.fireboxes.get(0), f.fireboxes.get(1), f.fireboxes.get(2),
                f.fireboxes.get(3), f.casings.get(0)};
        for (int i = 0; i < kinds.length; i++) {
            f.putCasing(required[i]);
            assertFalse("Missing required hatch " + kinds[i], f.recheck());
            f.putHatch(required[i], kinds[i]);
            assertTrue(f.recheck());
        }
        f.putHatch(f.fireboxes.get(1), 'I');
        assertFalse("Two item buses cannot supply water", f.recheck());
        f.putHatch(f.fireboxes.get(1), 'Q');
        assertTrue("IMPACT multi-fluid input supplies water", f.recheck());
        f.putHatch(f.fireboxes.get(1), 'X');
        assertTrue("IMPACT combined input supplies water", f.recheck());
        f.putHatch(f.fireboxes.get(1), 'W');
        f.putHatch(f.fireboxes.get(2), 'W');
        assertTrue("Two fluid hatches allow water and liquid fuel", f.recheck());
        f.putHatch(f.fireboxes.get(2), 'I');
        f.putHatch(f.casings.get(0), 'B');
        assertFalse("A bus cannot replace a steam outlet", f.recheck());
        f.putHatch(f.casings.get(0), 'O');

        for (int i = 1; i < 9; i++) {
            f.putHatch(f.casings.get(i), 'O');
        }
        assertTrue("24 casings", f.recheck());
        f.putHatch(f.casings.get(9), 'O');
        assertFalse("23 casings", f.recheck());
        f.putCasing(f.casings.get(9));
        f.putHatch(f.fireboxes.get(4), 'I');
        assertTrue("3 fireboxes", f.recheck());
        f.putHatch(f.fireboxes.get(5), 'I');
        assertFalse("2 fireboxes", f.recheck());
        f.putCasing(f.fireboxes.get(5));

        Position pipe = f.pipes.get(0);
        f.world.setBlockToAir(pipe.x, pipe.y, pipe.z);
        assertFalse("The interior contains pipes, not air", f.recheck());
        f.putCasing(pipe);
        Position top = f.topCenter;
        f.world.setBlock(top.x, top.y, top.z, f.machine.getPipeType().getBlock(), f.machine.getPipeType().getMetadata(), 0);
        assertFalse("Top center must be capped", f.recheck());
        f.putCasing(top);
        Position casing = f.casings.get(20);
        f.world.setBlock(casing.x, casing.y, casing.z, f.machine.getCasingType().getBlock(),
                (f.machine.getCasingType().getMetadata() + 1) % 16, 0);
        assertFalse("Wrong casing metadata", f.recheck());
        f.putCasing(casing);
        f.putHatch(f.fireboxes.get(5), 'O');
        assertFalse("Steam outlets cannot replace fireboxes", f.recheck());
        f.putCasing(f.fireboxes.get(5));
        assertTrue(f.recheck());

        int changes = f.world.changes;
        int inputs = HatchElement.Input.count(f.machine);
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(new ItemStack(Items.stick), true);
        f.world.isRemote = false;
        assertEquals(44, hints.hints);
        assertEquals(41, hints.hintFrames.size());
        assertEquals(changes, f.world.changes);
        assertEquals(inputs, HatchElement.Input.count(f.machine));
        for (Position p : f.positions) {
            String key = TestWorld.key(p.x, p.y, p.z);
            ICasing descriptor = f.descriptor(p);
            assertSame("Interchangeable hatch positions retain actual block textures",
                    descriptor.getBlock(), hints.hintBlocks.get(key));
            assertEquals(descriptor.getMetadata(), hints.hintMetadata.get(key).intValue());
            if (p.kind == 'P') {
                assertFalse("Pipes need no hatch frame", hints.hintFrames.containsKey(key));
            } else {
                assertSame(p.kind == 'F' ? HintColor.BLUE : HintColor.ORANGE, hints.hintFrames.get(key));
            }
        }
        Boiler independent = new Boiler(model, direction);
        independent.machine.construct(new ItemStack(Items.stick), false);
        assertFalse("Hatch lists and counters belong to each machine", independent.recheck());
        assertTrue(f.recheck());
    }

    private static void checkSurvival(int model, Item hatchItem, HintProxy hints) {
        GregTech_API.METATILEENTITIES[32200] = hatch('W');
        GregTech_API.METATILEENTITIES[32201] = hatch('U');
        GregTech_API.METATILEENTITIES[32202] = hatch('O');
        Boiler f = new Boiler(model, ForgeDirection.NORTH);
        Source items = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32002),
                new ItemStack(hatchItem, 1, 32200), new ItemStack(hatchItem, 1, 32201),
                new ItemStack(hatchItem, 1, 32202), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(f.machine.getFireboxType().getBlock(), 8, f.machine.getFireboxType().getMetadata()),
                new ItemStack(f.machine.getCasingType().getBlock(), 33, f.machine.getCasingType().getMetadata()),
                new ItemStack(f.machine.getPipeType().getBlock(), 3, f.machine.getPipeType().getMetadata()));
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        assertEquals(1, f.build(trigger, 1, items));
        for (int i = 0; i < 50 && !f.machine.mMachine; i++) {
            int before = items.consumed;
            int built = f.build(trigger, 4, items);
            assertTrue(built > 0 && built <= 4);
            assertEquals(built, items.consumed - before);
        }
        assertTrue("Survival forms model " + model, f.machine.mMachine);
        assertEquals(44, items.consumed);
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(2, HatchElement.Input.count(f.machine));
        assertEquals(1, f.machine.mOutputHatches.size());
        assertEquals("Output bus is not auto-selected for steam", 1, items.stacks.get(5).stackSize);
        assertEquals(-1, f.build(trigger, 4, items));
        assertEquals(44, items.consumed);

        for (boolean channel : new boolean[]{false, true}) {
            Boiler shell = new Boiler(model, ForgeDirection.SOUTH);
            Source blocks = shell.blocks();
            ItemStack shellTrigger = channel ? trigger : new ItemStack(Items.stick);
            assertEquals("Available shell builds even without hatches", 44, shell.build(shellTrigger, 100, blocks));
            assertFalse(shell.machine.mMachine);
            assertEquals(0, shell.build(shellTrigger, 100, blocks));
            assertEquals(44, blocks.consumed);
        }
        Boiler conflict = new Boiler(model, ForgeDirection.WEST);
        for (Position p : new Position[]{conflict.fireboxes.get(7), conflict.casings.get(32), conflict.pipes.get(2)}) {
            conflict.world.setBlock(p.x, p.y, p.z, Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source blocks = conflict.blocks();
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger, 1, blocks));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, blocks.queries);
        conflict.machine.construct(trigger, false);
        assertEquals("Creative still builds around obstructions", 45, conflict.world.blocks.size());

        Boiler missing = new Boiler(model, ForgeDirection.EAST);
        Source empty = new Source();
        assertEquals(0, missing.build(trigger, 3, empty));
        assertEquals(0, empty.consumed);
        assertEquals(1, missing.world.blocks.size());
        assertFalse(missing.player.messages.isEmpty());
    }

    private static void checkTooltipAndRates(int model) throws Exception {
        GT_MetaTileEntity_LargeBoiler machine = machine(model);
        assertEquals(new int[]{400, 600, 800, 1000}[model], machine.getEUt());
        assertEquals(new int[]{16, 12, 8, 4}[model], machine.getEfficiencyIncrease());
        Method boost = machine.getClass().getDeclaredMethod("runtimeBoost", int.class);
        boost.setAccessible(true);
        assertEquals(new int[]{40, 30, 26, 24}[model], boost.invoke(machine, 20));
        assertEquals(12, machine.getPollutionPerTick(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_BoilerStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String primary = String.join("\n", machine.getCurrentDescription(false));
            String secondary = String.join("\n", machine.getCurrentDescription(true));
            assertTrue(primary.contains(language.equals("en_US") ? "Large Boiler" : "Большой котёл"));
            assertTrue(secondary.contains(language.equals("en_US") ? "3 x 5 x 3" : "3 × 5 × 3"));
            assertTrue(secondary.contains("24"));
            assertTrue(secondary.contains("gt_hatch"));
            String inputMarker = EnumChatFormatting.BLUE + "■" + EnumChatFormatting.RESET + " - ";
            String outputMarker = EnumChatFormatting.GOLD + "■" + EnumChatFormatting.RESET + " - ";
            assertTrue(secondary.contains(inputMarker));
            assertTrue(secondary.contains(outputMarker));
            String hints = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(hints.contains(inputMarker));
            assertTrue(hints.contains(outputMarker));
            String water = StatCollector.translateToLocal("gt.tooltip.hatch.boiler_water");
            assertTrue(secondary.contains(EnumChatFormatting.WHITE + water));
            assertTrue(String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick))).contains(water));
            assertFalse(primary.contains("gt.tooltip."));
            assertFalse(secondary.contains("TileEntity_DESCRIPTION_"));
        }
    }

    private static final class Position {
        final int x;
        final int y;
        final int z;
        final char kind;

        Position(int x, int y, int z, char kind) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.kind = kind;
        }
    }

    private static final class Boiler {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_LargeBoiler machine;
        final List<Position> positions = new ArrayList<>();
        final List<Position> fireboxes = new ArrayList<>();
        final List<Position> casings = new ArrayList<>();
        final List<Position> pipes = new ArrayList<>();
        Position topCenter;

        Boiler(int model, ForgeDirection facing) {
            machine = machine(model);
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            // Independent legacy geometry, centered one block behind the controller.
            int centerX = -facing.offsetX;
            int centerZ = -facing.offsetZ;
            for (int y = 0; y <= 4; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = centerX + dx;
                        int z = centerZ + dz;
                        if (y == 0 && x == 0 && z == 0) {
                            continue;
                        }
                        char kind = y == 0 ? 'F' : dx == 0 && dz == 0 && y < 4 ? 'P' : 'C';
                        Position p = new Position(x, y + 64, z, kind);
                        positions.add(p);
                        if (kind == 'F') {
                            fireboxes.add(p);
                        } else if (kind == 'P') {
                            pipes.add(p);
                        } else {
                            casings.add(p);
                        }
                        if (dx == 0 && dz == 0 && y == 4) {
                            topCenter = p;
                        }
                    }
                }
            }
        }

        ICasing descriptor(Position p) {
            switch (p.kind) {
                case 'F':
                    return machine.getFireboxType();
                case 'P':
                    return machine.getPipeType();
                default:
                    return machine.getCasingType();
            }
        }

        void putCasing(Position p) {
            ICasing casing = descriptor(p);
            world.setBlock(p.x, p.y, p.z, casing.getBlock(), casing.getMetadata(), 0);
        }

        void putHatch(Position p, char kind) {
            world.putTile(p.x, p.y, p.z, new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void requiredHatches() {
            putHatch(fireboxes.get(0), 'M');
            putHatch(fireboxes.get(1), 'W');
            putHatch(fireboxes.get(2), 'I');
            putHatch(fireboxes.get(3), 'U');
            putHatch(casings.get(0), 'O');
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(machine.getBaseMetaTileEntity(), null);
            return machine.mMachine;
        }

        Source blocks() {
            return new Source(new ItemStack(machine.getFireboxType().getBlock(), 8, machine.getFireboxType().getMetadata()),
                    new ItemStack(machine.getCasingType().getBlock(), 33, machine.getCasingType().getMetadata()),
                    new ItemStack(machine.getPipeType().getBlock(), 3, machine.getPipeType().getMetadata()));
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
