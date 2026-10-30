package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringTranslate;
import net.minecraft.util.StatCollector;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.ImpactAPI;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

/** Runs inside the existing isolated Forge test harness, without launching Minecraft. */
final class GT_FusionStructureChecks {
    private static final List<Position> LEGACY = new ArrayList<>();

    static void run(Item hatchItem, HintProxy hints) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                GT_FusionStructureChecks.class.getResourceAsStream("/gregtech/fusion-structure.csv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",");
                LEGACY.add(new Position(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]), fields[3].charAt(0)));
            }
        }
        GregTech_API.sBlockCasings1 = Blocks.wool;
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings5 = Blocks.stained_glass;
        GregTech_API.sBlockCasings7 = Blocks.carpet;
        for (int model = 1; model <= 5; model++) {
            for (ForgeDirection direction : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                checkLegacyGeometry(model, direction, hints);
            }
            checkSurvival(model, hatchItem, hints);
            checkTooltip(model);
        }
    }

    private static GT_MetaTileEntity_FusionComputer machine(int model) {
        String name = "fusioncomputer.tier." + (model == 5 ? "10" : "0" + (model + 5));
        switch (model) {
            case 1:
                return new GT_MetaTileEntity_FusionComputer1(name);
            case 2:
                return new GT_MetaTileEntity_FusionComputer2(name);
            case 3:
                return new GT_MetaTileEntity_FusionComputer3(name);
            case 4:
                return new GT_MetaTileEntity_FusionComputer4(name);
            case 5:
                return new GT_MetaTileEntity_FusionComputer5(name);
            default:
                throw new AssertionError(model);
        }
    }

    private static MetaTileEntity hatch(char kind, int tier) {
        switch (kind) {
            case 'E':
                return new GT_MetaTileEntity_Hatch_Energy("fusion_test", tier, "test", null);
            case 'I':
                return new GT_MetaTileEntity_Hatch_Input("fusion_test", tier, "test", null);
            case 'O':
                return new GT_MetaTileEntity_Hatch_Output("fusion_test", tier, "test", null);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkLegacyGeometry(int model, ForgeDirection direction, HintProxy hints) {
        Reactor f = new Reactor(model, direction);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals("Exact legacy shell, model " + model + " facing " + direction, 160, f.world.blocks.size());
        for (Position p : LEGACY) {
            if (p.kind != '~') {
                f.assertCasing(p);
            }
        }
        assertFalse("Casings alone cannot form a reactor", f.recheck());
        Position energy = f.positions('E').get(0);
        Position input = f.positions('I').get(0);
        Position output = f.positions('O').get(0);
        f.putHatch(energy, hatch('E', f.machine.tier()));
        f.putHatch(input, hatch('I', f.machine.tier()));
        f.putHatch(f.positions('I').get(1), hatch('I', f.machine.tier()));
        f.putHatch(output, hatch('O', f.machine.tier()));
        assertTrue("Legacy valid reactor", f.recheck());
        f.putHatch(f.positions('I').get(2), new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null));
        f.putHatch(f.positions('O').get(1), new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null));
        f.putHatch(f.positions('E').get(1), new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null));
        assertTrue("Legacy optional hatches remain compatible", f.recheck());
        assertEquals(1, f.machine.mEnergyHatches.size());
        assertEquals(2, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mOutputHatches.size());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mEnergyHatches.size());
        assertEquals(2, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mOutputHatches.size());
        for (Position p : new Position[]{energy, input, output}) {
            f.putCasing(p);
            assertFalse("Missing category " + p.kind, f.recheck());
            f.putHatch(p, hatch(p.kind, f.machine.tier() - 1));
            assertFalse("Insufficient tier for " + p.kind, f.recheck());
            f.putHatch(p, hatch(p.kind, f.machine.tier() + 1));
            assertTrue("Higher tiers remain accepted", f.recheck());
        }
        f.putHatch(energy, hatch('I', f.machine.tier()));
        assertFalse("Hatch positions are category-specific", f.recheck());
        f.putHatch(energy, hatch('E', f.machine.tier()));
        f.putHatch(input, new GT_MetaTileEntity_Hatch_InputBus("test", f.machine.tier(), "test", null));
        assertFalse("An item bus cannot supply a fusion fluid input", f.recheck());
        f.putHatch(input, hatch('I', f.machine.tier()));
        Position coil = f.positions('K').get(0);
        int[] cp = f.coords(coil);
        f.world.setBlock(cp[0], cp[1], cp[2], Blocks.stone, 0, 0);
        assertFalse("Wrong coil", f.recheck());
        f.putCasing(coil);
        Position casing = f.positions('C').get(0);
        cp = f.coords(casing);
        f.world.setBlock(cp[0], cp[1], cp[2], f.machine.getCasing(), (f.machine.getCasingMeta() + 1) % 16, 0);
        assertFalse("Wrong casing metadata", f.recheck());
        f.putCasing(casing);
        int[] center = f.coords(new Position(0, 0, 0, ' '));
        f.world.setBlock(center[0], center[1], center[2], Blocks.stone, 0, 0);
        assertTrue("Unused center remains unconstrained, as before", f.recheck());
        int changes = f.world.changes;
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        f.world.isRemote = true;
        f.machine.construct(new ItemStack(Items.stick), true);
        f.world.isRemote = false;
        assertEquals(159, hints.hints);
        for (Position p : LEGACY) {
            if (p.kind == '~') {
                continue;
            }
            int[] at = f.coords(p);
            String key = TestWorld.key(at[0], at[1], at[2]);
            if (p.kind == 'E' || p.kind == 'I' || p.kind == 'O') {
                HintColor color = p.kind == 'E' ? HintColor.YELLOW : p.kind == 'I' ? HintColor.BLUE : HintColor.ORANGE;
                assertSame("Hatch positions use colored hint blocks", ImpactAPI.getBlockHint(), hints.hintBlocks.get(key));
                assertEquals(color.getMetadata(), hints.hintMetadata.get(key).intValue());
            } else {
                ICasing descriptor = p.kind == 'K' ? f.machine.getFusionCoilType() : f.machine.getCasingType();
                assertSame("Casings and coils keep their actual textures", descriptor.getBlock(), hints.hintBlocks.get(key));
                assertEquals(descriptor.getMetadata(), hints.hintMetadata.get(key).intValue());
            }
        }
        assertEquals(changes, f.world.changes);
        assertEquals(2, f.machine.mInputHatches.size());
        f.putCasing(input);
        f.machine.mRunningOnLoad = true;
        f.machine.mStartUpCheck = 100;
        f.machine.mUpdate = 100;
        f.machine.onPostTick(f.machine.getBaseMetaTileEntity(), 1);
        f.machine.onPostTick(f.machine.getBaseMetaTileEntity(), 2);
        assertFalse("Repeated load checks must not count a single fluid input twice", f.machine.mMachine);
        assertEquals(1, f.machine.mInputHatches.size());
    }

    private static void checkSurvival(int model, Item item, HintProxy hints) {
        Reactor f = new Reactor(model, ForgeDirection.NORTH);
        int baseId = 32100 + model * 10;
        for (int i = 0; i < 3; i++) {
            GregTech_API.METATILEENTITIES[baseId + i] = hatch("EIO".charAt(i), f.machine.tier());
        }
        GregTech_API.METATILEENTITIES[baseId + 3] = hatch('E', f.machine.tier() - 1);
        GregTech_API.METATILEENTITIES[baseId + 4] = new GT_MetaTileEntity_Hatch_InputBus("test", f.machine.tier(), "test", null);
        Source source = new Source(new ItemStack(item, 1, baseId + 3), new ItemStack(item, 1, baseId),
                new ItemStack(item, 2, baseId + 1), new ItemStack(item, 1, baseId + 2),
                new ItemStack(item, 1, baseId + 4),
                new ItemStack(f.machine.getCasing(), 200, f.machine.getCasingMeta()),
                new ItemStack(f.machine.getFusionCoil(), 32, f.machine.getFusionCoilMeta()));
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        assertEquals(1, f.build(trigger, 1, source));
        assertEquals(1, source.consumed);
        for (int i = 0; i < 100 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int result = f.build(trigger, 7, source);
            assertTrue(result >= 0 && result <= 7);
            assertEquals(result, source.consumed - before);
        }
        assertTrue("Survival completes model " + model, f.machine.mMachine);
        assertEquals(159, source.consumed);
        assertEquals("Under-tier item not consumed", 1, source.stacks.get(0).stackSize);
        assertEquals("Optional bus cannot satisfy the required fluid input", 1, source.stacks.get(4).stackSize);
        assertEquals(1, f.machine.mEnergyHatches.size());
        assertEquals(2, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mOutputHatches.size());
        assertEquals(-1, f.build(trigger, 7, source));
        assertEquals(159, source.consumed);

        Reactor shell = new Reactor(model, ForgeDirection.EAST);
        Source casings = new Source(new ItemStack(shell.machine.getCasing(), 127, shell.machine.getCasingMeta()),
                new ItemStack(shell.machine.getFusionCoil(), 32, shell.machine.getFusionCoilMeta()),
                new ItemStack(item, 1, baseId));
        assertEquals(159, shell.build(new ItemStack(Items.stick), 200, casings));
        assertEquals(1, casings.stacks.get(2).stackSize);
        assertFalse(shell.machine.mMachine);
        Reactor fallback = new Reactor(model, ForgeDirection.SOUTH);
        Source noHatches = new Source(new ItemStack(fallback.machine.getCasing(), 127, fallback.machine.getCasingMeta()),
                new ItemStack(fallback.machine.getFusionCoil(), 32, fallback.machine.getFusionCoilMeta()));
        assertEquals("Enabled hatch channel still builds available casings", 159, fallback.build(trigger, 200, noHatches));
        assertEquals(0, fallback.build(trigger, 200, noHatches));
        assertFalse(fallback.machine.mMachine);

        Reactor obstructed = new Reactor(model, ForgeDirection.WEST);
        for (char kind : new char[]{'C', 'K', 'E'}) {
            int[] p = obstructed.coords(obstructed.positions(kind).get(0));
            obstructed.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        int before = obstructed.world.changes;
        hints.errorPositions.clear();
        Source empty = new Source();
        assertEquals(0, obstructed.build(trigger, 1, empty));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(before, obstructed.world.changes);
        assertEquals(0, empty.queries);
        Reactor missing = new Reactor(model, ForgeDirection.NORTH);
        assertEquals(0, missing.build(trigger, 5, empty));
        assertEquals(0, empty.consumed);
        assertFalse(missing.player.messages.isEmpty());
    }

    private static void checkTooltip(int model) throws Exception {
        GT_MetaTileEntity_FusionComputer machine = machine(model);
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_FusionStructureChecks.class.getResourceAsStream("/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String primary = String.join("\n", machine.getCurrentDescription(false));
            String secondary = String.join("\n", machine.getCurrentDescription(true));
            assertTrue(primary.contains(language.equals("en_US") ? "Fusion Reactor" : "Термоядерный реактор"));
            assertFalse(primary.contains("15 x 3 x 15"));
            assertTrue(secondary.contains(language.equals("en_US") ? "15 x 3 x 15" : "15 × 3 × 15"));
            assertTrue(secondary.contains("79-123"));
            assertTrue(secondary.contains("gt_hatch"));
            assertFalse(secondary.contains("At least 0"));
            assertTrue(String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick))).contains("gt_hatch"));
            EnumChatFormatting[] textColors = {EnumChatFormatting.YELLOW, EnumChatFormatting.BLUE, EnumChatFormatting.GOLD};
            String[] categories = {"energy", "input_hatch", "output_hatch"};
            for (int i = 0; i < textColors.length; i++) {
                String name = StatCollector.translateToLocal("gt.tooltip.hatch." + categories[i]);
                String legend = textColors[i] + "■" + EnumChatFormatting.RESET + " - " + name;
                assertTrue("Hatch names keep ordinary formatting",
                        secondary.contains("   " + EnumChatFormatting.WHITE + name + ":"));
                assertTrue("Shift tooltip explains hatch colors", secondary.contains(legend));
                assertTrue("Construction hints explain the same colors",
                        String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick))).contains(legend));
            }
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

    private static final class Reactor {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_FusionComputer machine;
        final ForgeDirection direction;

        Reactor(int model, ForgeDirection direction) {
            this.direction = direction;
            machine = machine(model);
            world.putTile(0, 64, 0, new TestTile(world, machine, direction));
        }

        int[] coords(Position p) {
            // Independent legacy world coordinates: symmetric ring centered five blocks in front.
            int x = p.x;
            int z = p.z;
            if (direction == ForgeDirection.SOUTH) {
                x = -p.x;
                z = -p.z;
            }
            if (direction == ForgeDirection.WEST) {
                x = p.z;
                z = -p.x;
            }
            if (direction == ForgeDirection.EAST) {
                x = -p.z;
                z = p.x;
            }
            return new int[]{x + direction.offsetX * 5, p.y + 64, z + direction.offsetZ * 5};
        }

        List<Position> positions(char kind) {
            List<Position> found = new ArrayList<>();
            for (Position p : LEGACY) {
                if (p.kind == kind) {
                    found.add(p);
                }
            }
            return found;
        }

        void putCasing(Position p) {
            int[] at = coords(p);
            ICasing casing = p.kind == 'K' ? machine.getFusionCoilType() : machine.getCasingType();
            world.setBlock(at[0], at[1], at[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        void assertCasing(Position p) {
            int[] at = coords(p);
            ICasing casing = p.kind == 'K' ? machine.getFusionCoilType() : machine.getCasingType();
            assertSame(casing.getBlock(), world.getBlock(at[0], at[1], at[2]));
            assertEquals(casing.getMetadata(), world.getBlockMetadata(at[0], at[1], at[2]));
        }

        void putHatch(Position p, MetaTileEntity hatch) {
            int[] at = coords(p);
            world.putTile(at[0], at[1], at[2], new TestTile(world, hatch, ForgeDirection.NORTH));
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
