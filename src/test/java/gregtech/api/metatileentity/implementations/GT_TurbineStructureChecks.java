package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_Steam;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_HPSteam;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_Gas;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_Plasma;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.hint.HintColor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

/** Independent legacy geometry, exercised in the isolated Forge harness without launching the game. */
final class GT_TurbineStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        for (int model = 0; model < 4; model++) {
            for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                checkGeometry(model, facing, hints);
                checkSurvival(model, facing, hatchItem, hints);
            }
            checkTooltipAndPersistence(model);
        }
    }

    private static GT_MetaTileEntity_LargeTurbine machine(int model) {
        switch (model) {
            case 0:
                return new GT_MetaTileEntity_LargeTurbine_Steam("multimachine.largeturbine");
            case 1:
                return new GT_MetaTileEntity_LargeTurbine_HPSteam("multimachine.largehpturbine");
            case 2:
                return new GT_MetaTileEntity_LargeTurbine_Gas("multimachine.largegasturbine");
            case 3:
                return new GT_MetaTileEntity_LargeTurbine_Plasma("multimachine.largeplasmaturbine");
            default:
                throw new AssertionError(model);
        }
    }

    private static MetaTileEntity hatch(char kind) {
        switch (kind) {
            case 'M':
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'I':
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 'O':
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 'U':
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            case 'D':
                return new GT_MetaTileEntity_Hatch_Dynamo("test", 1, "test", null);
            case 'A':
                return new GT_MetaTileEntity_Hatch_DynamoMulti("test", 1, 4, "test", null);
            case 'Q':
                return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 'X':
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            case 'B':
                return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 'E':
                return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(int model, ForgeDirection facing, HintProxy hints) {
        Turbine f = new Turbine(model, facing);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals("32 casings plus controller; rear hatch stays empty", 33, f.world.blocks.size());
        for (int[] p : f.shell) {
            assertSame(f.machine.getCasingType().getBlock(), f.world.getBlock(p[0], p[1], p[2]));
            assertEquals(f.machine.getCasingType().getMetadata(), f.world.getBlockMetadata(p[0], p[1], p[2]));
        }
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mDynamoHatches.size());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        for (int i = 0; i < f.requiredKinds().length; i++) {
            f.putCasing(f.sides.get(i));
            assertFalse("Missing required hatch " + f.requiredKinds()[i], f.recheck());
            f.putHatch(f.sides.get(i), f.requiredKinds()[i]);
            assertTrue(f.recheck());
        }
        f.putHatch(f.sides.get(1), 'B');
        assertFalse("Item buses do not supply turbine fuel", f.recheck());
        for (char input : new char[]{'Q', 'X', 'I'}) {
            f.putHatch(f.sides.get(1), input);
            assertTrue("IMPACT fluid inputs are accepted", f.recheck());
        }
        if (model < 2) {
            f.putHatch(f.sides.get(2), 'E');
            assertFalse("An output bus does not collect steam or water", f.recheck());
            f.putHatch(f.sides.get(2), 'O');
        }
        for (int i = f.requiredKinds().length; i < 8; i++) {
            f.putHatch(f.sides.get(i), i % 2 == 0 ? 'E' : 'U');
        }
        assertTrue("24 fixed casings; legacy optional hatches still accepted", f.recheck());
        int[] fixed = f.fixed.get(0);
        f.putHatch(fixed, 'I');
        assertFalse("23 casings, hatch in a fixed casing position", f.recheck());
        f.putCasing(fixed);
        f.world.setBlock(fixed[0], fixed[1], fixed[2], f.machine.getCasingType().getBlock(), 0, 0);
        assertFalse("Wrong casing metadata", f.recheck());
        f.putCasing(fixed);
        for (int[] air : f.air) {
            f.putCasing(air);
            assertFalse("Both internal center positions must be empty", f.recheck());
            f.world.setBlockToAir(air[0], air[1], air[2]);
        }
        f.putCasing(f.dynamo);
        assertFalse("Rear center cannot be a casing", f.recheck());
        f.putHatch(f.dynamo, 'A');
        assertTrue("IMPACT multi-amp dynamo", f.recheck());
        assertEquals(1, f.machine.mDynamoHatchesMulti.size());
        assertEquals(0, f.machine.mDynamoHatches.size());
        NBTTagCompound hatchTag = new NBTTagCompound();
        f.machine.mDynamoHatchesMulti.get(0).saveNBTData(hatchTag);
        assertEquals("Hatch uses the actual turbine casing texture", f.machine.getCasingType().getTextureId(),
                hatchTag.getByte("mMachineBlock"));

        int[] neighbor = facing.offsetX == 0 ? new int[]{2, 64, 0} : new int[]{0, 64, 2};
        f.world.putTile(neighbor[0], neighbor[1], neighbor[2], new TestTile(f.world, machine(model), facing));
        assertFalse("Preserve the adjacent turbine restriction", f.recheck());
        f.world.putTile(neighbor[0], neighbor[1], neighbor[2],
                new TestTile(f.world, machine(model), facing.getOpposite()));
        assertTrue("Different-facing neighbors remain allowed", f.recheck());
        f.world.setBlockToAir(neighbor[0], neighbor[1], neighbor[2]);

        int changes = f.world.changes;
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(new ItemStack(Items.stick), true);
        f.world.isRemote = false;
        assertEquals(35, hints.hints);
        assertEquals(changes, f.world.changes);
        assertEquals(1, f.machine.mDynamoHatchesMulti.size());
        assertEquals("Only the eight side hatch positions have casing frames", 8, hints.hintFrames.size());
        for (int[] p : f.shell) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(f.machine.getCasingType().getBlock(), hints.hintBlocks.get(key));
            if (f.sides.contains(p)) {
                assertSame(HintColor.BLUE, hints.hintFrames.get(key));
            } else {
                assertFalse("Fixed casings have no hatch markers", hints.hintFrames.containsKey(key));
            }
            assertEquals(f.machine.getCasingType().getMetadata(), hints.hintMetadata.get(key).intValue());
        }
        Turbine independent = new Turbine(model, facing);
        independent.machine.construct(new ItemStack(Items.stick), false);
        assertFalse(independent.recheck());
        assertTrue(f.recheck());
    }

    private static void checkSurvival(int model, ForgeDirection facing, Item hatchItem, HintProxy hints) {
        char[] kinds = {'I', 'O', 'U', 'D', 'A'};
        for (int i = 0; i < kinds.length; i++) {
            GregTech_API.METATILEENTITIES[32300 + i] = hatch(kinds[i]);
        }
        Turbine f = new Turbine(model, facing);
        Source items = new Source(new ItemStack(hatchItem, 1, 32002), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32300),
                new ItemStack(hatchItem, 1, 32301), new ItemStack(hatchItem, 1, 32302),
                new ItemStack(hatchItem, 1, model % 2 == 0 ? 32303 : 32304), f.casingStack(32));
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        assertEquals(1, f.build(trigger, 1, items));
        for (int i = 0; i < 40 && !f.machine.mMachine; i++) {
            int before = items.consumed;
            int built = f.build(trigger, 3, items);
            assertTrue(built > 0 && built <= 3);
            assertEquals(built, items.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(33, items.consumed);
        assertEquals(model < 2 ? 1 : 0, f.machine.mOutputHatches.size());
        assertEquals(model == 2 ? 1 : 0, f.machine.mMufflerHatches.size());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mDynamoHatches.size() + f.machine.mDynamoHatchesMulti.size());
        assertEquals("Never auto-select a bus for fluid fuel", 1, items.stacks.get(0).stackSize);
        assertEquals("Never auto-select an output bus", 1, items.stacks.get(1).stackSize);
        assertEquals(-1, f.build(trigger, 3, items));
        assertEquals(33, items.consumed);
        TestTile dynamo = (TestTile) f.world.getTileEntity(f.dynamo[0], f.dynamo[1], f.dynamo[2]);
        assertEquals("Dynamo faces out through the back", facing.getOpposite().ordinal(), dynamo.getFrontFacing());

        for (boolean channel : new boolean[]{false, true}) {
            Turbine shell = new Turbine(model, facing);
            Source blocks = new Source(shell.casingStack(32));
            ItemStack shellTrigger = channel ? trigger : new ItemStack(Items.stick);
            assertEquals("Missing rear hatch must not stop shell construction", 32, shell.build(shellTrigger, 100, blocks));
            assertEquals(0, shell.build(shellTrigger, 100, blocks));
            assertEquals(32, blocks.consumed);
            assertTrue(shell.world.isAirBlock(shell.dynamo[0], shell.dynamo[1], shell.dynamo[2]));
            assertFalse(shell.machine.mMachine);
            // Existing side casings are preserved; free only the required positions for the next pass.
            for (int i = 0; i < shell.requiredKinds().length; i++) {
                int[] p = shell.sides.get(i);
                shell.world.setBlockToAir(p[0], p[1], p[2]);
            }
            Source hatches = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32300),
                    new ItemStack(hatchItem, 1, 32301), new ItemStack(hatchItem, 1, 32302),
                    new ItemStack(hatchItem, 1, 32303));
            for (int i = 0; i < 6 && !shell.machine.mMachine; i++) {
                shell.build(trigger, 10, hatches);
            }
            assertTrue("Replenish hatches on later passes", shell.machine.mMachine);
            assertEquals(shell.requiredKinds().length + 1, hatches.consumed);
        }
        Turbine disabled = new Turbine(model, facing);
        Source available = new Source(new ItemStack(hatchItem, 1, 32303), disabled.casingStack(32));
        assertEquals(32, disabled.build(new ItemStack(Items.stick), 100, available));
        assertEquals("Channel off leaves the rear dynamo item untouched", 1, available.stacks.get(0).stackSize);

        Turbine conflict = new Turbine(model, facing);
        for (int[] p : new int[][]{conflict.fixed.get(23), conflict.air.get(1), conflict.dynamo}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source blocks = new Source(conflict.casingStack(32));
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger, 1, blocks));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, blocks.queries);
        conflict.machine.construct(trigger, false);
        assertEquals("Creative builds the shell around conflicts", 35, conflict.world.blocks.size());
        Turbine missing = new Turbine(model, facing);
        assertEquals(0, missing.build(trigger, 10, new Source()));
        assertEquals(1, missing.world.blocks.size());
        assertFalse(missing.player.messages.isEmpty());
    }

    private static void checkTooltipAndPersistence(int model) throws Exception {
        Turbine f = new Turbine(model, ForgeDirection.EAST);
        assertEquals(model == 2 ? 15 : 0, f.machine.getPollutionPerTick(null));
        f.machine.baseEff = 1234;
        f.machine.optFlow = 5678;
        f.machine.checkguislot = false;
        f.machine.mNameRotorMaterial = "Saved rotor";
        NBTTagCompound tag = new NBTTagCompound();
        f.machine.saveNBTData(tag);
        // BaseMetaTileEntity restores its front facing before loading the machine NBT.
        Turbine restored = new Turbine(model, ForgeDirection.EAST);
        restored.machine.loadNBTData(tag);
        assertEquals(1234, restored.machine.baseEff);
        assertEquals(5678, restored.machine.optFlow);
        assertFalse(restored.machine.checkguislot);
        assertEquals("Saved rotor", restored.machine.mNameRotorMaterial);
        assertEquals(f.machine.getExtendedFacing(), restored.machine.getExtendedFacing());
        for (String lang : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_TurbineStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + lang + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            String hint = String.join("\n", f.machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(lang.equals("en_US") ? "rotor" : "ротор"));
            assertFalse(info.contains("gt_hatch"));
            assertTrue(structure.contains(lang.equals("en_US") ? "3 x 3 x 4" : "3 × 3 × 4"));
            assertTrue(structure.contains("24"));
            assertTrue(structure.contains("gt_hatch"));
            assertTrue(structure.contains(lang.equals("en_US") ? "Back center" : "По центру сзади"));
            String marker = EnumChatFormatting.YELLOW + "■" + EnumChatFormatting.RESET + " - ";
            assertTrue(structure.contains(marker));
            assertTrue(hint.contains(marker));
            assertTrue(structure.contains(EnumChatFormatting.BLUE + "■" + EnumChatFormatting.RESET + " - "));
            assertEquals("Separate legends for the side positions and rear dynamo", 2,
                    structure.split("■", -1).length - 1);
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
            assertFalse(structure.contains("gt.tooltip."));
        }
    }

    private static final class Turbine {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_LargeTurbine machine;
        final int model;
        final List<int[]> shell = new ArrayList<>();
        final List<int[]> sides = new ArrayList<>();
        final List<int[]> fixed = new ArrayList<>();
        final List<int[]> air = new ArrayList<>();
        int[] dynamo;

        Turbine(int model, ForgeDirection facing) {
            this.model = model;
            machine = machine(model);
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            for (int depth = 0; depth < 4; depth++) {
                for (int y = -1; y <= 1; y++) {
                    for (int side = -1; side <= 1; side++) {
                        int[] p = {-facing.offsetX * depth + (facing.offsetX == 0 ? side : 0),
                                64 + y, -facing.offsetZ * depth + (facing.offsetZ == 0 ? side : 0)};
                        if (side == 0 && y == 0) {
                            if (depth == 3) {
                                dynamo = p;
                            } else if (depth > 0) {
                                air.add(p);
                            }
                            continue;
                        }
                        shell.add(p);
                        if ((side == 0 || y == 0) && (depth == 1 || depth == 2)) {
                            sides.add(p);
                        } else {
                            fixed.add(p);
                        }
                    }
                }
            }
        }

        char[] requiredKinds() {
            return model < 2 ? new char[]{'M', 'I', 'O'} : model == 2 ? new char[]{'M', 'I', 'U'} : new char[]{'M', 'I'};
        }

        void requiredHatches() {
            for (int i = 0; i < requiredKinds().length; i++) {
                putHatch(sides.get(i), requiredKinds()[i]);
            }
            putHatch(dynamo, 'D');
        }

        void putHatch(int[] p, char kind) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void putCasing(int[] p) {
            world.setBlock(p[0], p[1], p[2], machine.getCasingType().getBlock(), machine.getCasingType().getMetadata(), 0);
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(machine.getBaseMetaTileEntity(), null);
            return machine.mMachine;
        }

        ItemStack casingStack(int count) {
            return new ItemStack(machine.getCasingType().getBlock(), count, machine.getCasingType().getMetadata());
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
