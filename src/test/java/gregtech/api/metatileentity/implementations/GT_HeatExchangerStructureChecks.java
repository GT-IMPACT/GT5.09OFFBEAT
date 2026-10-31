package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_HeatExchanger;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_HeatExchangerStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings4 = Blocks.wool;
        GregTech_API.sBlockCasings2 = Blocks.stained_glass;
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            checkGeometry(facing, hints);
            checkSurvival(facing, hatchItem, hints);
        }
        checkThermalOperation();
        checkTooltip();
    }

    private static MetaTileEntity hatch(int kind) {
        switch (kind) {
            case 0:
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 7:
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null) {
                    @Override
                    public void markStorageDirty() {
                        // No chunk persistence in the isolated world; keep real fluid handling.
                    }
                };
            case 8:
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null) {
                    @Override
                    public void markStorageDirty() {
                    }
                };
            default:
                return (MetaTileEntity) GregTech_API.METATILEENTITIES[32000 + kind].newMetaEntity(null);
        }
    }

    private static Object field(GT_MetaTileEntity_HeatExchanger machine, String name) throws Exception {
        Field field = GT_MetaTileEntity_HeatExchanger.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(machine);
    }

    private static void checkGeometry(ForgeDirection facing, HintProxy hints) throws Exception {
        Exchanger f = new Exchanger(facing);
        f.machine.construct(new ItemStack(Items.stick), false);
        assertEquals(34, f.world.blocks.size());
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals("Only the water input belongs to normal input lists", 1, f.machine.mInputHatches.size());
        assertEquals("Only the steam output belongs to normal output lists", 1, f.machine.mOutputHatches.size());
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        for (int i = 0; i < 3; i++) {
            f.putCasing(f.shell.get(i));
            assertFalse("Hot/cold ports do not replace a missing shell hatch " + i, f.recheck());
            f.putHatch(f.shell.get(i), new int[]{0, 7, 8}[i]);
        }
        for (int kind : new int[]{2, 5}) {
            f.putHatch(f.hot, kind);
            assertFalse("Hot port requires the fluid input API", f.recheck());
            assertNull(field(f.machine, "mInputHotFluidHatch"));
        }
        f.putHatch(f.hot, 4);
        assertTrue("Legacy IMPACT multi-fluid hot inputs remain accepted", f.recheck());
        f.putHatch(f.hot, 7);
        f.putHatch(f.cold, 3);
        assertFalse("Cold port cannot be an item bus", f.recheck());
        assertNull(field(f.machine, "mOutputColdFluidHatch"));
        f.putHatch(f.cold, 8);
        f.putHatch(f.shell.get(1), 2);
        assertFalse("An item bus cannot supply water", f.recheck());
        for (int water : new int[]{4, 5, 7}) {
            f.putHatch(f.shell.get(1), water);
            assertTrue("IMPACT fluid and combined inputs can supply water", f.recheck());
        }
        f.putHatch(f.shell.get(2), 3);
        assertFalse("An item bus cannot output steam", f.recheck());
        f.putHatch(f.shell.get(2), 8);
        for (int i = 3; i <= 10; i++) {
            f.putHatch(f.shell.get(i), i % 2 == 0 ? 2 : 3);
        }
        assertTrue("20 casings and legacy optional item buses", f.recheck());
        f.putHatch(f.shell.get(11), 7);
        assertFalse("19 casings", f.recheck());
        f.putCasing(f.shell.get(11));
        f.putHatch(f.shell.get(30), 1);
        assertFalse("Unrelated machine blocks cannot be ignored as controllers", f.recheck());
        f.putCasing(f.shell.get(30));
        for (int[] p : f.pipes) {
            f.world.setBlockToAir(p[0], p[1], p[2]);
            assertFalse("Both titanium pipe blocks are required", f.recheck());
            f.world.setBlock(p[0], p[1], p[2], Casings.TITANIUM_PIPE.getBlock(), 13, 0);
            assertFalse("Wrong pipe metadata", f.recheck());
            f.putPipe(p);
        }
        assertTrue(f.recheck());
        int changes = f.world.changes;
        Object hot = field(f.machine, "mInputHotFluidHatch");
        Object cold = field(f.machine, "mOutputColdFluidHatch");
        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        f.world.isRemote = true;
        f.machine.construct(new ItemStack(Items.stick), true);
        f.world.isRemote = false;
        assertEquals(35, hints.hints);
        assertEquals(changes, f.world.changes);
        assertSame(hot, field(f.machine, "mInputHotFluidHatch"));
        assertSame(cold, field(f.machine, "mOutputColdFluidHatch"));
        assertTrue("No frames on unrestricted shell hatch positions", hints.hintFrames.isEmpty());
        for (int[] p : f.shell) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.STABLE_TITANIUM.getBlock(), hints.hintBlocks.get(key));
            assertEquals(2, hints.hintMetadata.get(key).intValue());
        }
        for (int[] p : f.pipes) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(Casings.TITANIUM_PIPE.getBlock(), hints.hintBlocks.get(key));
            assertEquals(14, hints.hintMetadata.get(key).intValue());
        }
        Exchanger independent = new Exchanger(facing);
        independent.machine.construct(new ItemStack(Items.stick), false);
        assertFalse(independent.recheck());
        assertNull(field(independent.machine, "mInputHotFluidHatch"));
        assertTrue(f.recheck());
    }

    private static void checkSurvival(ForgeDirection facing, Item hatchItem, HintProxy hints) {
        GregTech_API.METATILEENTITIES[32700] = hatch(7);
        GregTech_API.METATILEENTITIES[32701] = hatch(8);
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        Exchanger f = new Exchanger(facing);
        Source source = new Source(new ItemStack(hatchItem, 1, 32002), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32700), new ItemStack(hatchItem, 1, 32000),
                new ItemStack(hatchItem, 1, 32005), new ItemStack(hatchItem, 2, 32701), casingStack(31), pipeStack());
        for (int i = 0; i < 45 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int built = f.build(trigger, i == 0 ? 1 : 3, source);
            assertTrue(built > 0 && built <= (i == 0 ? 1 : 3));
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(35, source.consumed);
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals("Prefer the combined water input and preserve the ordinary hatch for hot fluid",
                1, f.machine.mInputBusHatches.size());
        assertEquals(0, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mOutputHatches.size());
        assertEquals(1, source.stacks.get(0).stackSize);
        assertEquals(1, source.stacks.get(1).stackSize);
        assertEquals(-1, f.build(trigger, 10, source));
        assertEquals(35, source.consumed);
        TestTile hot = (TestTile) f.world.getTileEntity(f.hot[0], f.hot[1], f.hot[2]);
        TestTile cold = (TestTile) f.world.getTileEntity(f.cold[0], f.cold[1], f.cold[2]);
        assertEquals(ForgeDirection.DOWN.ordinal(), hot.getFrontFacing());
        assertEquals(ForgeDirection.UP.ordinal(), cold.getFrontFacing());
        for (boolean channel : new boolean[]{false, true}) {
            Exchanger shell = new Exchanger(facing);
            Source blocks = new Source(casingStack(31), pipeStack());
            assertEquals("Missing dedicated ports must not stop the shell", 33,
                    shell.build(channel ? trigger : new ItemStack(Items.stick), 100, blocks));
            assertEquals(0, shell.build(trigger, 100, blocks));
            assertEquals(33, blocks.consumed);
            assertFalse(shell.machine.mMachine);
            for (int i = 0; i < 3; i++) {
                int[] p = shell.shell.get(i);
                shell.world.setBlockToAir(p[0], p[1], p[2]);
            }
            Source hatches = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 2, 32700),
                    new ItemStack(hatchItem, 2, 32701));
            for (int i = 0; i < 5; i++) {
                assertEquals(1, shell.build(trigger, 10, hatches));
            }
            assertTrue("Install all ports on later passes", shell.machine.mMachine);
            assertEquals(5, hatches.consumed);
        }
        Exchanger conflict = new Exchanger(facing);
        for (int[] p : new int[][]{conflict.hot, conflict.cold, conflict.pipes.get(1)}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        Source blocks = new Source(casingStack(31), pipeStack());
        int changes = conflict.world.changes;
        assertEquals(0, conflict.build(trigger, 1, blocks));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(changes, conflict.world.changes);
        assertEquals(0, blocks.queries);
        conflict.machine.construct(trigger, false);
        assertEquals(36, conflict.world.blocks.size());
        Exchanger missing = new Exchanger(facing);
        assertEquals(0, missing.build(trigger, 10, new Source()));
        assertFalse(missing.player.messages.isEmpty());
    }

    private static void checkThermalOperation() throws Exception {
        for (String name : new String[]{"ic2hotcoolant", "ic2coolant", "ic2pahoehoelava", "ic2distilledwater",
                "ic2superheatedsteam", "steam"}) {
            if (FluidRegistry.getFluid(name) == null) {
                FluidRegistry.registerFluid(new Fluid(name));
            }
        }
        for (boolean lava : new boolean[]{true, false}) {
            int threshold = lava ? 1000 : 800;
            for (int amount : new int[]{threshold / 2, threshold, threshold * 3}) {
                Exchanger f = new Exchanger(ForgeDirection.NORTH);
                f.machine.construct(new ItemStack(Items.stick), false);
                f.requiredHatches();
                assertTrue(f.recheck());
                GT_MetaTileEntity_Hatch_Input hot = (GT_MetaTileEntity_Hatch_Input) field(f.machine, "mInputHotFluidHatch");
                GT_MetaTileEntity_Hatch_Output cold = (GT_MetaTileEntity_Hatch_Output) field(f.machine, "mOutputColdFluidHatch");
                GT_MetaTileEntity_Hatch_Input water = f.machine.mInputHatches.get(0);
                GT_MetaTileEntity_Hatch_Output steam = f.machine.mOutputHatches.get(0);
                hot.mFluid = FluidRegistry.getFluidStack(lava ? "lava" : "ic2hotcoolant", amount);
                water.mFluid = FluidRegistry.getFluidStack("ic2distilledwater", 10000);
                f.machine.mEfficiency = 10000;
                assertTrue(f.machine.checkRecipe(null));
                int consumed = Math.min(amount, threshold * 2);
                int energy = consumed * (lava ? 4 : 10);
                boolean superheated = amount >= threshold;
                assertEquals(energy, f.machine.mEUt);
                assertEquals(20, f.machine.mMaxProgresstime);
                assertEquals(threshold, field(f.machine, "superheated_threshold"));
                assertEquals(superheated, field(f.machine, "superheated"));
                assertEquals(amount - consumed, hot.getFluidAmount());
                assertEquals(consumed, cold.mFluid.amount);
                assertSame(FluidRegistry.getFluid(lava ? "ic2pahoehoelava" : "ic2coolant"), cold.mFluid.getFluid());
                assertTrue(f.machine.onRunningTick(null));
                int produced = energy * (superheated ? 1 : 2);
                assertEquals(produced, steam.mFluid.amount);
                assertSame(FluidRegistry.getFluid(superheated ? "ic2superheatedsteam" : "steam"), steam.mFluid.getFluid());
                assertEquals(10000 - produced / 160, water.getFluidAmount());
                assertEquals("Cold output remains isolated from steam output", consumed, cold.mFluid.amount);
                NBTTagCompound tag = new NBTTagCompound();
                f.machine.saveNBTData(tag);
                Exchanger restored = new Exchanger(ForgeDirection.NORTH);
                restored.machine.loadNBTData(tag);
                assertEquals(superheated, field(restored.machine, "superheated"));
                water.mFluid = null;
                f.machine.onRunningTick(null);
                assertTrue("Original dry-running explosion behavior", f.machine.exploded);
            }
        }
        Exchanger circuit = new Exchanger(ForgeDirection.NORTH);
        circuit.machine.construct(new ItemStack(Items.stick), false);
        circuit.requiredHatches();
        assertTrue(circuit.recheck());
        Item integratedCircuit = new Item() {
            @Override
            public String getUnlocalizedName(ItemStack stack) {
                return "gt.integrated_circuit";
            }
        };
        circuit.machine.mInventory[1] = new ItemStack(integratedCircuit, 1, 25);
        GT_MetaTileEntity_Hatch_Input hot = (GT_MetaTileEntity_Hatch_Input) field(circuit.machine, "mInputHotFluidHatch");
        hot.mFluid = FluidRegistry.getFluidStack("lava", 100);
        assertTrue(circuit.machine.checkRecipe(null));
        assertEquals(100, field(circuit.machine, "superheated_threshold"));
        assertEquals(256, circuit.machine.mEUt);
        hot.mFluid = FluidRegistry.getFluidStack("water", 100);
        assertFalse("Unrelated fluid is not a heat source", circuit.machine.checkRecipe(null));
        assertEquals(0, field(circuit.machine, "superheated_threshold"));
    }

    private static void checkTooltip() throws Exception {
        TestExchanger machine = new TestExchanger();
        assertEquals(0, machine.getPollutionPerTick(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_HeatExchangerStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", machine.getCurrentDescription(false));
            String structure = String.join("\n", machine.getCurrentDescription(true));
            String hint = String.join("\n", machine.getStructureDescription(new ItemStack(Items.stick)));
            assertTrue(info.contains(language.equals("en_US") ? "Large Heat Exchanger" : "Большой теплообменник"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 4 x 3" : "3 × 4 × 3"));
            assertTrue(structure.contains("20"));
            assertTrue(structure.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            for (EnumChatFormatting color : new EnumChatFormatting[]{EnumChatFormatting.RED, EnumChatFormatting.BLUE}) {
                String marker = color + "■" + EnumChatFormatting.RESET + " - ";
                assertTrue(structure.contains(marker));
                assertTrue(hint.contains(marker));
            }
            assertEquals(2, structure.split("■", -1).length - 1);
            assertFalse(structure.contains("gt.tooltip."));
            assertFalse(structure.contains("TileEntity_DESCRIPTION_"));
        }
    }

    private static ItemStack casingStack(int count) {
        return new ItemStack(Casings.STABLE_TITANIUM.getBlock(), count, Casings.STABLE_TITANIUM.getMetadata());
    }

    private static ItemStack pipeStack() {
        return new ItemStack(Casings.TITANIUM_PIPE.getBlock(), 2, Casings.TITANIUM_PIPE.getMetadata());
    }

    private static final class TestExchanger extends GT_MetaTileEntity_HeatExchanger {
        boolean exploded;

        TestExchanger() {
            super("multimachine.heatexchanger");
        }

        @Override
        public void explodeMultiblock() {
            exploded = true;
        }
    }

    private static final class Exchanger {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final TestExchanger machine = new TestExchanger();
        final List<int[]> shell = new ArrayList<>();
        final List<int[]> pipes = new ArrayList<>();
        final int[] hot;
        final int[] cold;

        Exchanger(ForgeDirection facing) {
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            hot = new int[]{-facing.offsetX, 64, -facing.offsetZ};
            cold = new int[]{-facing.offsetX, 67, -facing.offsetZ};
            for (int x = hot[0] - 1; x <= hot[0] + 1; x++) {
                for (int z = hot[2] - 1; z <= hot[2] + 1; z++) {
                    for (int y = 64; y <= 67; y++) {
                        if (x == hot[0] && z == hot[2]) {
                            if (y == 65 || y == 66) {
                                pipes.add(new int[]{x, y, z});
                            }
                        } else if (x != 0 || y != 64 || z != 0) {
                            shell.add(new int[]{x, y, z});
                        }
                    }
                }
            }
        }

        void putCasing(int[] p) {
            world.setBlock(p[0], p[1], p[2], Casings.STABLE_TITANIUM.getBlock(), Casings.STABLE_TITANIUM.getMetadata(), 0);
        }

        void putPipe(int[] p) {
            world.setBlock(p[0], p[1], p[2], Casings.TITANIUM_PIPE.getBlock(), Casings.TITANIUM_PIPE.getMetadata(), 0);
        }

        void putHatch(int[] p, int kind) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind), ForgeDirection.NORTH));
        }

        void requiredHatches() {
            putHatch(shell.get(0), 0);
            putHatch(shell.get(1), 7);
            putHatch(shell.get(2), 8);
            putHatch(hot, 7);
            putHatch(cold, 8);
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
