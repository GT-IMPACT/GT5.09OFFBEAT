package gregtech.api.metatileentity.implementations;

import com.impact.mods.gregtech.tileentities.hatches.lasers.GTMTE_LaserEnergy_In;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AssemblyLine;
import ic2.core.Ic2Items;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_AssemblyLineStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings2 = Blocks.wool;
        GregTech_API.sBlockCasings3 = Blocks.stained_hardened_clay;
        Ic2Items.reinforcedGlass = new ItemStack(Blocks.glass);
        ItemList.Tool_DataStick.set(new ItemStack(Items.paper));
        GregTech_API.METATILEENTITIES[32730] = hatch('F');
        GregTech_API.METATILEENTITIES[32731] = hatch('L');
        GregTech_API.METATILEENTITIES[32732] = hatch('D');
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (int length = 5; length <= 16; length++) {
                Line f = new Line(facing, length);
                f.machine.construct(f.trigger(false), false);
                assertEquals(9 * length, f.world.blocks.size());
                f.requiredHatches();
                assertTrue("Every supported length and facing", f.recheck());
                assertEquals(length - 1, f.machine.mInputBusses.size());
                for (int slice = 0; slice < length - 1; slice++) {
                    assertSame(f.tile(slice, -2, 1).getMetaTileEntity(), f.machine.mInputBusses.get(slice));
                }
                if (length == 5 || length == 16) {
                    checkGeometry(f, hints);
                    checkSurvival(facing, length, hatchItem, hints);
                }
            }
            checkRecipe(facing);
        }
        checkTooltipAndNbt();
    }

    private static MetaTileEntity hatch(char kind) {
        switch (kind) {
            case 'M':
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'E':
                return new GT_MetaTileEntity_Hatch_Energy("test", 1, "test", null);
            case 'A':
                return new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
            case 'L':
                return new GTMTE_LaserEnergy_In("test", 1, 4, "test", null);
            case 'I':
                return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 'O':
                return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            case 'D':
                return new GT_MetaTileEntity_Hatch_DataAccess("test", 4, "test", null);
            case 'F':
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null) {
                    @Override
                    public void markStorageDirty() {
                    }
                };
            case 'Q':
                return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 'X':
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(Line f, HintProxy hints) {
        f.put(0, -2, 0, Casings.SOLID_STEEL);
        assertFalse("Maintenance is required", f.recheck());
        f.hatch(0, -2, 0, 'M');
        f.put(0, 1, 1, Casings.SOLID_STEEL);
        assertFalse("Power is required", f.recheck());
        f.hatch(0, 1, 1, 'A');
        assertTrue("One multi-amp energy hatch", f.recheck());
        f.hatch(1, 1, 1, 'E');
        assertFalse("No mixed power types", f.recheck());
        f.hatch(1, 1, 1, 'A');
        assertFalse("Only one multi-amp hatch", f.recheck());
        f.put(1, 1, 1, Casings.SOLID_STEEL);
        f.hatch(0, 1, 1, 'L');
        assertFalse("Lasers are forbidden", f.recheck());
        f.hatch(0, 1, 1, 'E');
        if (f.length == 16) {
            for (int i = 1; i < 8; i++) {
                f.hatch(i, 1, 1, 'E');
            }
            assertTrue("Eight basic hatches", f.recheck());
            f.hatch(8, 1, 1, 'E');
            assertFalse("Not nine basic hatches", f.recheck());
            for (int i = 1; i <= 8; i++) {
                f.put(i, 1, 1, Casings.SOLID_STEEL);
            }
        }
        for (int slice = 0; slice < f.length; slice++) {
            f.put(slice, -2, 1, Casings.SOLID_STEEL);
            assertFalse("Every dedicated bus is mandatory", f.recheck());
            f.hatch(slice, -2, 1, slice == f.length - 1 ? 'O' : 'I');
        }
        for (int slice : new int[]{0, 1, f.length - 1}) {
            for (int y : new int[]{-1, 0}) {
                f.clear(slice, y, 1);
                assertFalse("Internal blocks are required, including the first slice", f.recheck());
                f.put(slice, y, 1, y == -1 ? Casings.ASSEMBLY_LINE : Casings.ASSEMBLER);
            }
        }
        f.hatch(1, 0, 0, 'D');
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mDataAccessHatches.size());
        f.hatch(2, 0, 0, 'D');
        assertFalse("Data hatch only next to controller", f.recheck());
        f.put(2, 0, 0, Casings.GRATE);
        f.put(1, 0, 0, Casings.GRATE);
        assertTrue(f.recheck());
        assertTrue("Removed data hatches are cleared", f.machine.mDataAccessHatches.isEmpty());
        f.hatch(1, -2, 0, 'I');
        assertFalse("Item buses cannot change input order by occupying bottom sides", f.recheck());
        f.hatch(1, -2, 0, 'Q');
        assertFalse("Multi-fluid hatch is incompatible with ordered single-fluid recipes", f.recheck());
        f.hatch(1, -2, 0, 'X');
        assertFalse("Combined inputs do not participate in data-stick recipes", f.recheck());
        f.hatch(1, -2, 0, 'F');
        f.hatch(0, -2, 2, 'F');
        assertTrue(f.recheck());
        assertSame(f.tile(0, -2, 2).getMetaTileEntity(), f.machine.mInputHatches.get(0));
        assertSame(f.tile(1, -2, 0).getMetaTileEntity(), f.machine.mInputHatches.get(1));
        for (int depth : new int[]{0, 2}) {
            f.block(f.length - 1, 1, depth, Blocks.bedrock, 0);
            assertFalse("Top sides remain air", f.recheck());
            f.clear(f.length - 1, 1, depth);
            f.block(f.length - 1, -1, depth, Blocks.glass, 5);
            assertTrue("Glass metadata remains unrestricted", f.recheck());
            f.block(f.length - 1, -1, depth, Blocks.stained_glass, 0);
            assertFalse("Correct reinforced glass block", f.recheck());
            f.block(f.length - 1, -1, depth, Blocks.glass, 0);
        }
        f.hatch(3, -2, 1, 'O');
        assertFalse("Fewer than five slices is invalid", f.recheck());
        f.hatch(3, -2, 1, 'I');
        if (f.length == 16) {
            f.hatch(15, -2, 1, 'I');
            f.hatch(16, -2, 1, 'O');
            assertFalse("A seventeenth slice cannot terminate the line", f.recheck());
            f.clear(16, -2, 1);
            f.hatch(15, -2, 1, 'O');
        }
        assertTrue(f.recheck());
        Line other = new Line(f.facing, f.length);
        other.machine.construct(other.trigger(false), false);
        assertFalse("Lists are per machine", other.recheck());
        assertTrue(f.recheck());

        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        int changes = f.world.changes;
        int inputs = f.machine.mInputBusses.size();
        f.world.isRemote = true;
        f.machine.construct(f.trigger(true), true);
        f.world.isRemote = false;
        assertEquals(12 * f.length - 1, hints.hints);
        assertEquals(3 * f.length + 1, hints.hintFrames.size());
        assertEquals(changes, f.world.changes);
        assertEquals(inputs, f.machine.mInputBusses.size());
        for (int slice = 0; slice < f.length; slice++) {
            f.assertHint(slice, -2, 0, Casings.SOLID_STEEL, HintColor.BLUE, hints);
            f.assertHint(slice, -2, 2, Casings.SOLID_STEEL, HintColor.BLUE, hints);
            f.assertHint(slice, 1, 1, Casings.SOLID_STEEL, HintColor.YELLOW, hints);
        }
        f.assertHint(1, 0, 0, Casings.GRATE, HintColor.PURPLE, hints);
    }

    private static void checkSurvival(ForgeDirection facing, int length, Item hatchItem, HintProxy hints) {
        Line f = new Line(facing, length);
        Source source = new Source(new ItemStack(hatchItem, 1, 32731), new ItemStack(hatchItem, 1, 32004),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32730),
                new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, length - 1, 32002), new ItemStack(hatchItem, 1, 32003),
                stack(Casings.SOLID_STEEL, 3 * length), stack(Casings.ASSEMBLY_LINE, length),
                stack(Casings.ASSEMBLER, length), stack(Casings.GRATE, 2 * length - 1),
                new ItemStack(Blocks.glass, 2 * length));
        for (int i = 0; i < 100 && !f.machine.mMachine; i++) {
            int consumed = source.consumed;
            int budget = i % 2 == 0 ? 1 : 7;
            int placed = f.build(true, budget, source);
            assertTrue(placed > 0 && placed <= budget);
            assertEquals(placed, source.consumed - consumed);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(10 * length - 1, source.consumed);
        for (int i = 0; i < 4; i++) {
            assertEquals("Forbidden or optional hatch is not auto-selected", 1, source.stacks.get(i).stackSize);
        }
        assertEquals(1, f.machine.mEnergyHatchesMulti.size());
        assertEquals(length - 1, f.machine.mInputBusses.size());
        assertEquals(-1, f.build(true, 100, source));
        assertEquals(10 * length - 1, source.consumed);
        for (boolean channel : new boolean[]{false, true}) {
            Line shell = new Line(facing, length);
            Source blocks = shell.blocks();
            assertEquals(9 * length - 1, shell.build(channel, 1000, blocks));
            assertEquals(0, shell.build(channel, 1000, blocks));
            assertFalse(shell.machine.mMachine);
            for (int i = 0; i < length; i++) {
                int[] p = shell.pos(i, -2, 1);
                assertTrue(shell.world.isAirBlock(p[0], p[1], p[2]));
            }
        }
        Line disabled = new Line(facing, length);
        assertEquals(0, disabled.build(false, 100, new Source(new ItemStack(hatchItem, 16, 32002))));
        assertEquals(1, disabled.world.blocks.size());
        assertFalse(disabled.player.messages.isEmpty());
        Line conflict = new Line(facing, length);
        int[][] positions = {conflict.pos(length - 1, 1, 2), conflict.pos(0, -1, 1), conflict.pos(length - 1, -2, 1)};
        for (int[] p : positions) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        Source untouched = conflict.blocks();
        int changes = conflict.world.changes;
        hints.errorPositions.clear();
        assertEquals(0, conflict.build(true, 1, untouched));
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, untouched.queries);
        for (int[] p : positions) {
            assertTrue(hints.errorPositions.contains(TestWorld.key(p[0], p[1], p[2])));
        }
        conflict.machine.construct(conflict.trigger(false), false);
        assertTrue(conflict.world.changes > changes);
        for (int[] p : positions) {
            assertSame(Blocks.bedrock, conflict.world.getBlock(p[0], p[1], p[2]));
        }
    }

    private static void checkRecipe(ForgeDirection facing) {
        Line f = new Line(facing, 5);
        f.machine.construct(f.trigger(false), false);
        f.requiredHatches();
        f.hatch(0, -2, 2, 'F');
        f.hatch(1, -2, 0, 'F');
        f.hatch(1, -2, 2, 'F');
        f.hatch(2, -2, 0, 'F');
        assertTrue(f.recheck());
        Item[] ingredients = {Items.apple, Items.carrot, Items.diamond, Items.iron_ingot};
        NBTTagCompound tag = recipe();
        for (int i = 0; i < 4; i++) {
            f.machine.mInputBusses.get(i).mInventory[0] = new ItemStack(ingredients[i], 8);
            tag.setTag("" + i, new ItemStack(ingredients[i], i + 1).writeToNBT(new NBTTagCompound()));
            f.machine.mInputHatches.get(i).mFluid = new FluidStack(i % 2 == 0 ? FluidRegistry.WATER : FluidRegistry.LAVA, 1000);
            tag.setTag("f" + i, new FluidStack(i % 2 == 0 ? FluidRegistry.WATER : FluidRegistry.LAVA, (i + 1) * 100)
                    .writeToNBT(new NBTTagCompound()));
        }
        f.machine.mInventory[1] = stick(tag);
        assertTrue("Ordered recipe works in all four orientations", f.machine.checkRecipe(null));
        for (int i = 0; i < 4; i++) {
            assertEquals(7 - i, f.machine.mInputBusses.get(i).mInventory[0].stackSize);
            assertEquals(1000 - (i + 1) * 100, f.machine.mInputHatches.get(i).mFluid.amount);
        }
        assertEquals(-8, f.machine.mEUt);
        assertEquals(40, f.machine.mMaxProgresstime);
        assertSame(Items.emerald, f.machine.mOutputItems[0].getItem());
        NBTTagCompound tooLong = recipe();
        tooLong.setTag("14", new ItemStack(Items.apple).writeToNBT(new NBTTagCompound()));
        f.machine.mInventory[1] = stick(tooLong);
        assertFalse("Long recipes are rejected without indexing past the buses", f.machine.checkRecipe(null));
        for (int[] p : new int[][]{f.pos(0, -2, 2), f.pos(1, -2, 0), f.pos(1, -2, 2), f.pos(2, -2, 0)}) {
            f.world.setBlock(p[0], p[1], p[2], Casings.SOLID_STEEL.getBlock(), 0, 0);
        }
        assertTrue(f.recheck());
        NBTTagCompound missingFluid = recipe();
        missingFluid.setTag("f3", new FluidStack(FluidRegistry.WATER, 100).writeToNBT(new NBTTagCompound()));
        f.machine.mInventory[1] = stick(missingFluid);
        assertFalse("Missing optional fluid ports do not crash", f.machine.checkRecipe(null));

        NBTTagCompound failed = recipe();
        failed.setTag("0", new ItemStack(Items.apple).writeToNBT(new NBTTagCompound()));
        failed.setTag("1", new ItemStack(Items.stick).writeToNBT(new NBTTagCompound()));
        f.machine.mInventory[1] = stick(failed);
        GT_MetaTileEntity_Hatch_DataAccess data = (GT_MetaTileEntity_Hatch_DataAccess) f.hatch(1, 0, 0, 'D');
        NBTTagCompound succeeds = recipe();
        succeeds.setTag("2", new ItemStack(Items.diamond).writeToNBT(new NBTTagCompound()));
        data.mInventory[0] = stick(succeeds);
        assertTrue(f.recheck());
        int apples = f.machine.mInputBusses.get(0).mInventory[0].stackSize;
        int diamonds = f.machine.mInputBusses.get(2).mInventory[0].stackSize;
        assertTrue(f.machine.checkRecipe(null));
        assertEquals("Failed candidate does not leak item consumption", apples, f.machine.mInputBusses.get(0).mInventory[0].stackSize);
        assertEquals(diamonds - 1, f.machine.mInputBusses.get(2).mInventory[0].stackSize);
    }

    private static NBTTagCompound recipe() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("output", new ItemStack(Items.emerald).writeToNBT(new NBTTagCompound()));
        tag.setInteger("time", 40);
        tag.setInteger("eu", 8);
        return tag;
    }

    private static ItemStack stick(NBTTagCompound tag) {
        ItemStack stack = new ItemStack(Items.paper);
        stack.setTagCompound(tag);
        return stack;
    }

    private static void checkTooltipAndNbt() throws Exception {
        Line f = new Line(ForgeDirection.EAST, 5);
        NBTTagCompound nbt = new NBTTagCompound();
        f.machine.saveNBTData(nbt);
        Line restored = new Line(ForgeDirection.EAST, 5);
        restored.machine.loadNBTData(nbt);
        assertEquals(f.machine.getExtendedFacing(), restored.machine.getExtendedFacing());
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_AssemblyLineStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            String hints = String.join("\n", f.machine.getStructureDescription(f.trigger(false)));
            assertTrue(info.contains(language.equals("en_US") ? "Assembly Line" : "Сборочная линия"));
            assertTrue(structure.contains(language.equals("en_US") ? "no mixing or lasers" : "без смешивания и лазеров"));
            assertTrue(structure.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            for (HintColor color : new HintColor[]{HintColor.BLUE, HintColor.YELLOW, HintColor.ORANGE,
                    HintColor.GREEN, HintColor.PURPLE}) {
                String marker = GTStructureUtility.getHintTextColor(color) + "■" + EnumChatFormatting.RESET + " - ";
                assertTrue(structure.contains(marker));
                assertTrue(hints.contains(marker));
            }
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
            assertFalse(structure.contains("gt.tooltip."));
        }
    }

    private static ItemStack stack(ICasing casing, int count) {
        return new ItemStack(casing.getBlock(), count, casing.getMetadata());
    }

    private static final class Line {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_AssemblyLine machine = new GT_MetaTileEntity_AssemblyLine("multimachine.assemblyline");
        final ForgeDirection facing;
        final int length;

        Line(ForgeDirection facing, int length) {
            this.facing = facing;
            this.length = length;
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
        }

        int[] pos(int slice, int height, int depth) {
            return new int[]{facing.offsetZ * slice - facing.offsetX * depth, 64 + height,
                    -facing.offsetX * slice - facing.offsetZ * depth};
        }

        TestTile tile(int slice, int height, int depth) {
            int[] p = pos(slice, height, depth);
            return (TestTile) world.getTileEntity(p[0], p[1], p[2]);
        }

        void block(int slice, int height, int depth, net.minecraft.block.Block block, int metadata) {
            int[] p = pos(slice, height, depth);
            world.setBlock(p[0], p[1], p[2], block, metadata, 0);
        }

        void put(int slice, int height, int depth, ICasing casing) {
            block(slice, height, depth, casing.getBlock(), casing.getMetadata());
        }

        void clear(int slice, int height, int depth) {
            block(slice, height, depth, Blocks.air, 0);
        }

        MetaTileEntity hatch(int slice, int height, int depth, char kind) {
            MetaTileEntity hatch = GT_AssemblyLineStructureChecks.hatch(kind);
            int[] p = pos(slice, height, depth);
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch, ForgeDirection.NORTH));
            return hatch;
        }

        void requiredHatches() {
            hatch(0, -2, 0, 'M');
            hatch(0, 1, 1, 'E');
            for (int slice = 0; slice < length; slice++) {
                hatch(slice, -2, 1, slice == length - 1 ? 'O' : 'I');
            }
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(machine.getBaseMetaTileEntity(), null);
            return machine.mMachine;
        }

        ItemStack trigger(boolean channel) {
            ItemStack trigger = new ItemStack(Items.stick, length - 4);
            if (channel) {
                ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
            }
            return trigger;
        }

        Source blocks() {
            return new Source(stack(Casings.SOLID_STEEL, 3 * length), stack(Casings.ASSEMBLY_LINE, length),
                    stack(Casings.ASSEMBLER, length), stack(Casings.GRATE, 2 * length - 1),
                    new ItemStack(Blocks.glass, 2 * length));
        }

        int build(boolean channel, int budget, Source source) {
            return machine.survivalConstruct(trigger(channel), budget, ISurvivalBuildEnvironment.create(source, player));
        }

        void assertHint(int slice, int height, int depth, ICasing casing, HintColor color, HintProxy hints) {
            int[] p = pos(slice, height, depth);
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(casing.getBlock(), hints.hintBlocks.get(key));
            assertEquals(casing.getMetadata(), hints.hintMetadata.get(key).intValue());
            assertSame(color, hints.hintFrames.get(key));
        }
    }
}
