package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DistillationTower;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
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

final class GT_DistillationTowerStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.METATILEENTITIES[32702] = hatch(7);
        GregTech_API.METATILEENTITIES[32703] = hatch(8);
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (int height = 3; height <= 12; height++) {
                checkGeometry(facing, height, hints);
                checkSurvival(facing, height, hatchItem, hints);
            }
            checkInvalidHeights(facing);
        }
        checkFluidRouting();
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
                        // Keep real fluid handling without chunk persistence in the isolated world.
                    }
                };
            default:
                return (MetaTileEntity) GregTech_API.METATILEENTITIES[32000 + kind].newMetaEntity(null);
        }
    }

    private static void checkGeometry(ForgeDirection facing, int height, HintProxy hints) {
        Tower f = new Tower(facing, height);
        f.machine.construct(f.trigger(false), false);
        assertEquals(8 * height + 2, f.world.blocks.size());
        assertFalse("Hatches are required", f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(height - 1, f.machine.mOutputHatches.size());
        assertEquals(1, f.machine.mOutputBusses.size());
        for (int i = 0; i < 4; i++) {
            int[] p = f.layers.get(0).get(i);
            f.casing(p);
            assertFalse("Missing bottom or common requirement " + i, f.recheck());
            f.putHatch(p, new int[]{0, 1, 7, 3}[i]);
        }
        for (int y = 1; y < height; y++) {
            int[] p = f.layers.get(y).get(0);
            f.casing(p);
            assertFalse("Each layer requires its own output: " + y, f.recheck());
            f.putHatch(p, 3);
            assertFalse("Output buses do not substitute fluid outlets", f.recheck());
            f.putHatch(p, 8);
        }
        for (int kind : new int[]{4, 5, 7}) {
            f.putHatch(f.layers.get(0).get(2), kind);
            assertTrue("Fluid and IMPACT inputs", f.recheck());
        }
        f.putHatch(f.layers.get(0).get(2), 2);
        assertFalse("An item-only input bus is insufficient", f.recheck());
        f.putHatch(f.layers.get(0).get(2), 7);
        f.putHatch(f.layers.get(0).get(1), 6);
        assertTrue("Multi-amp energy is accepted", f.recheck());
        int[] upper = f.layers.get(1).get(1);
        f.putHatch(upper, 7);
        assertFalse("Inputs are confined to the bottom", f.recheck());
        f.putHatch(upper, 3);
        assertTrue("Additional upper output buses remain valid", f.recheck());
        f.casing(f.layers.get(0).get(3));
        assertFalse("An upper bus does not replace the bottom output bus", f.recheck());
        f.putHatch(f.layers.get(0).get(3), 3);
        f.casing(upper);
        f.casing(f.layers.get(0).get(0));
        f.putHatch(upper, 0);
        assertTrue("Maintenance can be above the bottom", f.recheck());
        f.casing(upper);
        f.requiredHatches();

        int[] roof = f.center(height - 1);
        f.putHatch(roof, 8);
        assertTrue("Roof center may be a hatch", f.recheck());
        f.casing(roof);
        f.world.setBlockToAir(roof[0], roof[1], roof[2]);
        assertFalse("Roof must close the tower", f.recheck());
        f.casing(roof);
        int[] air = f.center(1);
        f.world.setBlock(air[0], air[1], air[2], Blocks.bedrock, 0, 0);
        assertFalse("Intermediate center must remain empty", f.recheck());
        f.world.setBlockToAir(air[0], air[1], air[2]);

        // The shell permits exactly three extra hatches before reaching the historical casing minimum.
        for (int i = 4; i < 7; i++) {
            f.putHatch(f.layers.get(0).get(i), 2);
        }
        assertEquals(7 * height - 5, f.casingCount());
        assertTrue("Exact casing minimum", f.recheck());
        f.putHatch(f.layers.get(0).get(7), 2);
        assertFalse("One casing below minimum", f.recheck());
        f.casing(f.layers.get(0).get(7));
        assertTrue(f.recheck());

        int changes = f.world.changes;
        int outputs = f.machine.mOutputHatches.size();
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(f.trigger(true), true);
        f.world.isRemote = false;
        assertEquals(9 * height - 1, hints.hints);
        assertEquals(changes, f.world.changes);
        assertEquals(outputs, f.machine.mOutputHatches.size());
        assertEquals(8 * height + 1, hints.hintFrames.size());
        for (int y = 0; y < height; y++) {
            for (int[] p : f.layers.get(y)) {
                String key = TestWorld.key(p[0], p[1], p[2]);
                assertSame(Casings.CLEAN_STAINLESS_STEEL.getBlock(), hints.hintBlocks.get(key));
                assertEquals(1, hints.hintMetadata.get(key).intValue());
                assertSame(y == 0 ? HintColor.BLUE : HintColor.ORANGE, hints.hintFrames.get(key));
            }
        }
        Tower independent = new Tower(facing, height);
        independent.machine.construct(independent.trigger(false), false);
        assertFalse(independent.recheck());
        assertTrue("Counters and layer requirements are independent", f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, int height, Item hatchItem, HintProxy hints) {
        Tower f = new Tower(facing, height);
        ItemStack trigger = f.trigger(true);
        Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32006),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, height - 1, 32703), casings(8 * height + 1));
        for (int i = 0; i < 150 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int budget = i == 0 ? 1 : 3;
            int built = f.build(trigger, budget, source);
            assertTrue("Construction must progress within its budget", built > 0 && built <= budget);
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(8 * height + 1, source.consumed);
        assertEquals(height - 1, f.machine.mOutputHatches.size());
        for (int y = 1; y < height; y++) {
            final int worldY = 64 + y;
            assertEquals(1, f.machine.mOutputHatches.stream()
                    .filter(hatch -> hatch.getBaseMetaTileEntity().getYCoord() == worldY).count());
        }
        for (GT_MetaTileEntity_Hatch_Output hatch : f.machine.mOutputHatches) {
            ForgeDirection outward = ForgeDirection.getOrientation(hatch.getBaseMetaTileEntity().getFrontFacing());
            assertEquals(Blocks.air, f.world.getBlock(hatch.getBaseMetaTileEntity().getXCoord() + outward.offsetX,
                    hatch.getBaseMetaTileEntity().getYCoord() + outward.offsetY,
                    hatch.getBaseMetaTileEntity().getZCoord() + outward.offsetZ));
        }
        assertEquals(-1, f.build(trigger, 10, source));
        assertEquals(8 * height + 1, source.consumed);
        for (boolean channel : new boolean[]{false, true}) {
            Tower shell = new Tower(facing, height);
            Source blocks = new Source(casings(8 * height + 1));
            assertEquals(8 * height + 1, shell.build(shell.trigger(channel), 200, blocks));
            assertEquals(0, shell.build(shell.trigger(channel), 200, blocks));
            assertFalse(shell.machine.mMachine);
        }
        Tower disabled = new Tower(facing, height);
        Source blocksWithHatch = new Source(new ItemStack(hatchItem, 1, 32000), casings(8 * height + 1));
        assertEquals(8 * height + 1, disabled.build(disabled.trigger(false), 200, blocksWithHatch));
        assertEquals(1, blocksWithHatch.stacks.get(0).stackSize);
        Tower missing = new Tower(facing, height);
        Source few = new Source(casings(2));
        assertEquals(2, missing.build(trigger, 200, few));
        assertFalse(missing.player.messages.isEmpty());
        Source rest = new Source(casings(8 * height + 1));
        missing.build(trigger, 200, rest);
        assertEquals(8 * height + 1, few.consumed + rest.consumed);

        Tower conflict = new Tower(facing, height);
        for (int[] p : new int[][]{conflict.center(1), conflict.layers.get(height - 1).get(0),
                conflict.layers.get(0).get(7)}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source untouched = new Source(casings(8 * height + 1));
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger, 1, untouched));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(0, untouched.queries);
        assertEquals(changes, conflict.world.changes);
        conflict.machine.construct(trigger, false);
        assertTrue("Creative builds despite conflicts", conflict.world.changes > changes);
    }

    private static void checkInvalidHeights(ForgeDirection facing) {
        for (int height : new int[]{2, 13}) {
            Tower f = new Tower(facing, height);
            for (List<int[]> layer : f.layers) {
                for (int[] p : layer) {
                    f.casing(p);
                }
            }
            f.requiredHatches();
            assertFalse("Height outside 3..12", f.recheck());
        }
        Tower capped = new Tower(facing, 12);
        capped.machine.construct(new ItemStack(Items.stick, 64), false);
        assertEquals(98, capped.world.blocks.size());
    }

    private static void checkFluidRouting() {
        Tower f = new Tower(ForgeDirection.NORTH, 12);
        f.machine.construct(f.trigger(false), false);
        f.requiredHatches();
        assertTrue(f.recheck());
        for (int i = 0; i < 11; i++) {
            FluidStack fluid = new FluidStack(FluidRegistry.WATER, 100 + i);
            f.machine.addOutput(fluid, i);
            assertEquals("Caller fluid is not modified", 100 + i, fluid.amount);
        }
        for (GT_MetaTileEntity_Hatch_Output hatch : f.machine.mOutputHatches) {
            assertEquals(100 + hatch.getBaseMetaTileEntity().getYCoord() - 65, hatch.mFluid.amount);
        }
        int[] p = f.layers.get(0).get(4);
        f.putHatch(p, 8);
        assertTrue(f.recheck());
        f.machine.addOutput(new FluidStack(FluidRegistry.WATER, 42), 11);
        GT_MetaTileEntity_Hatch_Output bottom = (GT_MetaTileEntity_Hatch_Output)
                ((TestTile) f.world.getTileEntity(p[0], p[1], p[2])).getMetaTileEntity();
        assertNull("No fallback to a bottom fluid hatch", bottom.mFluid);
    }

    private static void checkTooltip() throws Exception {
        GT_MetaTileEntity_DistillationTower machine = new GT_MetaTileEntity_DistillationTower("multimachine.distillationtower");
        assertSame(GT_Recipe.GT_Recipe_Map.sDistillationRecipes, machine.getRecipeMap());
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_DistillationTowerStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hints = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(language.equals("en_US") ? "Distillation Tower" : "Дистилляционная башня"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 3-12 x 3" : "3 × 3-12 × 3"));
            assertTrue(structure.contains("16-79"));
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

    private static ItemStack casings(int count) {
        return new ItemStack(Casings.CLEAN_STAINLESS_STEEL.getBlock(), count, Casings.CLEAN_STAINLESS_STEEL.getMetadata());
    }

    private static final class Tower {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_DistillationTower machine = new GT_MetaTileEntity_DistillationTower("multimachine.distillationtower");
        final List<List<int[]>> layers = new ArrayList<>();
        final ForgeDirection facing;
        final int height;

        Tower(ForgeDirection facing, int height) {
            this.facing = facing;
            this.height = height;
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            for (int y = 0; y < height; y++) {
                List<int[]> layer = new ArrayList<>();
                layers.add(layer);
                for (int x = -facing.offsetX - 1; x <= -facing.offsetX + 1; x++) {
                    for (int z = -facing.offsetZ - 1; z <= -facing.offsetZ + 1; z++) {
                        if (y == 0 && x == 0 && z == 0) {
                            continue;
                        }
                        if (y > 0 && y < height - 1 && x == -facing.offsetX && z == -facing.offsetZ) {
                            continue;
                        }
                        layer.add(new int[]{x, 64 + y, z});
                    }
                }
            }
        }

        int[] center(int y) {
            return new int[]{-facing.offsetX, 64 + y, -facing.offsetZ};
        }

        ItemStack trigger(boolean channel) {
            ItemStack trigger = new ItemStack(Items.stick, height - 2);
            if (channel) {
                ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
            }
            return trigger;
        }

        void casing(int[] p) {
            world.setBlock(p[0], p[1], p[2], Casings.CLEAN_STAINLESS_STEEL.getBlock(), 1, 0);
        }

        void putHatch(int[] p, int kind) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void requiredHatches() {
            for (int i = 0; i < 4; i++) {
                putHatch(layers.get(0).get(i), new int[]{0, 1, 7, 3}[i]);
            }
            for (int y = 1; y < height; y++) {
                putHatch(layers.get(y).get(0), 8);
            }
        }

        int casingCount() {
            int count = 0;
            for (List<int[]> layer : layers) {
                for (int[] p : layer) {
                    if (world.getBlock(p[0], p[1], p[2]) == Casings.CLEAN_STAINLESS_STEEL.getBlock()) {
                        count++;
                    }
                }
            }
            return count;
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
