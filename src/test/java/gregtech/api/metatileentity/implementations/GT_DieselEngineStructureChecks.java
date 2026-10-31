package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DieselEngine;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DieselEngine2;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_DieselEngineStructureChecks {
    static void run(Item hatchItem, HintProxy hints) throws Exception {
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings2 = Blocks.stained_glass;
        GregTech_API.sBlockCasings7 = Blocks.wool;
        for (int i = 0; i < 4; i++) {
            GregTech_API.METATILEENTITIES[32710 + i] = hatch(new char[]{'I', 'U', 'D', 'A'}[i]);
        }
        for (int model = 0; model < 2; model++) {
            for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                    ForgeDirection.WEST, ForgeDirection.EAST}) {
                checkGeometry(model, facing, hints);
                checkSurvival(model, facing, hatchItem, hints);
            }
            checkTooltipAndPersistence(model);
            checkFuelOperation(model);
        }
    }

    private static GT_MetaTileEntity_DieselEngine machine(int model) {
        return model == 0 ? new GT_MetaTileEntity_DieselEngine("multimachine.dieselengine")
                : new GT_MetaTileEntity_DieselEngine2("multimachine.dieselengine2");
    }

    private static MetaTileEntity hatch(char kind) {
        switch (kind) {
            case 'M':
                return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'I':
                return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null) {
                    @Override
                    public void markStorageDirty() {
                        // Real fluid handling without chunk persistence in the isolated test world.
                    }
                };
            case 'U':
                return new GT_MetaTileEntity_Hatch_Muffler("test", 1, "test", null);
            case 'D':
                return new GT_MetaTileEntity_Hatch_Dynamo("test", 9, "test", null);
            case 'A':
                return new GT_MetaTileEntity_Hatch_DynamoMulti("test", 9, 4, "test", null);
            case 'O':
                return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 'B':
                return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 'Q':
                return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 'X':
                return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            default:
                throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(int model, ForgeDirection facing, HintProxy hints) {
        Engine f = new Engine(model, facing);
        assertSame(model == 0 ? Casings.STABLE_TITANIUM : Casings.ROBUST_TUNGSTENSTEEL, f.machine.getCasingType());
        assertSame(model == 0 ? Casings.ENGINE_INTAKE : Casings.TUNGSTENSTEEL_PIPE, f.machine.getIntakeType());
        assertSame(model == 0 ? Casings.TITANIUM_GEARBOX : Casings.TUNGSTENSTEEL_GEARBOX, f.machine.getGearboxType());
        f.machine.construct(trigger(false), false);
        assertEquals("Dedicated ports remain empty", 34, f.world.blocks.size());
        assertEquals(23, f.count(f.machine.getCasingType()));
        assertEquals(8, f.count(f.machine.getIntakeType()));
        assertEquals(2, f.count(f.machine.getGearboxType()));
        assertFalse(f.recheck());
        f.requiredHatches();
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(2, f.machine.mInputHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(1, f.machine.mDynamoHatches.size());
        for (int i = 0; i < 3; i++) {
            f.casing(f.sides.get(i));
            assertFalse("Missing input or maintenance", f.recheck());
            f.putHatch(f.sides.get(i), i == 0 ? 'M' : 'I');
        }
        f.putHatch(f.sides.get(1), 'B');
        assertFalse("Two fluid-capable inputs are required", f.recheck());
        f.putHatch(f.sides.get(1), 'Q');
        f.putHatch(f.sides.get(2), 'X');
        f.putHatch(f.dynamo, 'A');
        assertTrue("IMPACT inputs and dynamo", f.recheck());
        f.putHatch(f.sides.get(3), 'U');
        f.clear(f.muffler);
        assertFalse("An optional side muffler does not replace the dedicated one", f.recheck());
        f.casing(f.muffler);
        assertFalse("A casing cannot replace the dedicated muffler", f.recheck());
        f.putHatch(f.muffler, 'U');
        f.casing(f.sides.get(3));
        f.clear(f.dynamo);
        assertFalse("Dynamo is mandatory", f.recheck());
        f.casing(f.dynamo);
        assertFalse("A casing cannot replace the dynamo", f.recheck());
        f.putHatch(f.dynamo, 'D');
        for (int[] p : f.gears) {
            f.clear(p);
            assertFalse("Both gearboxes are required independently", f.recheck());
            f.put(p, model == 0 ? Casings.STEEL_GEARBOX : Casings.TITANIUM_GEARBOX);
            assertFalse("Correct gearbox metadata", f.recheck());
            f.put(p, f.machine.getGearboxType());
        }
        for (int[] p : f.clearance) {
            f.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
            assertFalse("Every intake needs air in front", f.recheck());
            f.clear(p);
        }
        int[] controllerFront = {facing.offsetX, 64, facing.offsetZ};
        f.world.setBlock(controllerFront[0], controllerFront[1], controllerFront[2], Blocks.bedrock, 0, 0);
        assertTrue("Controller front is not intake clearance", f.recheck());
        f.clear(controllerFront);
        for (int[] p : new int[][]{f.fixed.get(0), f.intakes.get(0)}) {
            f.putHatch(p, 'I');
            assertFalse("Hatches are restricted to marked positions", f.recheck());
            f.put(p, p == f.fixed.get(0) ? f.machine.getCasingType() : f.machine.getIntakeType());
        }
        for (int i = 3; i < 7; i++) {
            f.putHatch(f.sides.get(i), 'O');
        }
        assertEquals(16, f.count(f.machine.getCasingType()));
        assertTrue("All seven side positions may contain hatches", f.recheck());
        f.putHatch(f.fixed.get(0), 'O');
        assertFalse("Fifteen casings / broken fixed frame", f.recheck());
        f.casing(f.fixed.get(0));
        assertTrue(f.recheck());

        hints.hints = 0;
        hints.hintBlocks.clear();
        hints.hintMetadata.clear();
        hints.hintFrames.clear();
        int changes = f.world.changes;
        int inputs = f.machine.mInputHatches.size() + f.machine.mInputBusHatches.size();
        f.world.isRemote = true;
        f.machine.construct(trigger(true), true);
        f.world.isRemote = false;
        assertEquals(43, hints.hints);
        assertEquals(changes, f.world.changes);
        assertEquals(inputs, f.machine.mInputHatches.size() + f.machine.mInputBusHatches.size());
        assertEquals(7, hints.hintFrames.size());
        for (int[] p : f.sides) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(f.machine.getCasingType().getBlock(), hints.hintBlocks.get(key));
            assertSame(HintColor.BLUE, hints.hintFrames.get(key));
        }
        for (int[] p : f.intakes) {
            assertHint(f.machine.getIntakeType(), p, hints);
        }
        for (int[] p : f.gears) {
            assertHint(f.machine.getGearboxType(), p, hints);
        }
        Engine other = new Engine(1 - model, facing);
        other.machine.construct(trigger(false), false);
        assertFalse(other.recheck());
        assertTrue("Variant descriptors and counters are independent", f.recheck());
    }

    private static void assertHint(ICasing casing, int[] p, HintProxy hints) {
        String key = TestWorld.key(p[0], p[1], p[2]);
        assertSame(casing.getBlock(), hints.hintBlocks.get(key));
        assertEquals(casing.getMetadata(), hints.hintMetadata.get(key).intValue());
    }

    private static void checkSurvival(int model, ForgeDirection facing, Item hatchItem, HintProxy hints) {
        Engine f = new Engine(model, facing);
        Source source = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32002),
                new ItemStack(hatchItem, 1, 32004), new ItemStack(hatchItem, 1, 32005),
                new ItemStack(hatchItem, 1, 32711), new ItemStack(hatchItem, 1, model == 0 ? 32712 : 32713),
                stack(f.machine.getCasingType(), 23), stack(f.machine.getIntakeType(), 8), stack(f.machine.getGearboxType(), 2));
        for (int i = 0; i < 50 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int budget = i == 0 ? 1 : 3;
            int built = f.build(trigger(true), budget, source);
            assertTrue(built > 0 && built <= budget);
            assertEquals(built, source.consumed - before);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(35, source.consumed);
        assertEquals("Item-only bus is not auto-selected", 1, source.stacks.get(1).stackSize);
        assertEquals(1, f.machine.mQuadrInputHatches.size());
        assertEquals(1, f.machine.mInputBusHatches.size());
        assertEquals(1, f.machine.mMufflerHatches.size());
        assertEquals(-1, f.build(trigger(true), 100, source));
        assertEquals(35, source.consumed);
        assertEquals(facing.getOpposite().ordinal(), f.world.getTileEntity(f.dynamo[0], f.dynamo[1], f.dynamo[2])
                instanceof TestTile ? ((TestTile) f.world.getTileEntity(f.dynamo[0], f.dynamo[1], f.dynamo[2])).getFrontFacing() : -1);
        for (boolean channel : new boolean[]{false, true}) {
            Engine shell = new Engine(model, facing);
            Source blocks = shell.blocks();
            assertEquals(33, shell.build(trigger(channel), 100, blocks));
            assertEquals(0, shell.build(trigger(channel), 100, blocks));
            assertTrue(shell.world.isAirBlock(shell.dynamo[0], shell.dynamo[1], shell.dynamo[2]));
            assertTrue(shell.world.isAirBlock(shell.muffler[0], shell.muffler[1], shell.muffler[2]));
            for (int i = 0; i < 3; i++) {
                shell.clear(shell.sides.get(i));
            }
            // A pre-existing optional muffler must not suppress auto-placement at the mandatory port.
            shell.putHatch(shell.sides.get(3), 'U');
            Source hatches = new Source(new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 2, 32710),
                    new ItemStack(hatchItem, 1, 32711), new ItemStack(hatchItem, 1, 32712));
            for (int i = 0; i < 6 && !shell.machine.mMachine; i++) {
                shell.build(trigger(true), 100, hatches);
            }
            assertTrue(shell.machine.mMachine);
            assertEquals(5, hatches.consumed);
            assertEquals(2, shell.machine.mMufflerHatches.size());
        }
        Engine disabled = new Engine(model, facing);
        Source items = new Source(new ItemStack(hatchItem, 1, 32711), new ItemStack(hatchItem, 1, 32712),
                stack(disabled.machine.getCasingType(), 23), stack(disabled.machine.getIntakeType(), 8),
                stack(disabled.machine.getGearboxType(), 2));
        assertEquals(33, disabled.build(trigger(false), 100, items));
        assertEquals(1, items.stacks.get(0).stackSize);
        assertEquals(1, items.stacks.get(1).stackSize);
        Engine conflict = new Engine(model, facing);
        for (int[] p : new int[][]{conflict.clearance.get(7), conflict.gears.get(1), conflict.fixed.get(15)}) {
            conflict.world.setBlock(p[0], p[1], p[2], Blocks.bedrock, 0, 0);
        }
        hints.errorPositions.clear();
        int changes = conflict.world.changes;
        Source untouched = conflict.blocks();
        assertEquals(0, conflict.build(trigger(true), 1, untouched));
        assertEquals(3, hints.errorPositions.size());
        assertEquals(0, untouched.queries);
        assertEquals(changes, conflict.world.changes);
        conflict.machine.construct(trigger(false), false);
        assertTrue(conflict.world.changes > changes);
        Engine missing = new Engine(model, facing);
        assertEquals(0, missing.build(trigger(true), 100, new Source()));
        assertFalse(missing.player.messages.isEmpty());
    }

    private static void checkFuelOperation(int model) throws Exception {
        Fluid fuel = new Fluid("diesel_engine_test_fuel_" + model);
        FluidRegistry.registerFluid(fuel);
        if (Materials.Oxygen.mGas == null) {
            Materials.Oxygen.mGas = new Fluid("diesel_engine_test_oxygen");
            FluidRegistry.registerFluid(Materials.Oxygen.mGas);
        }
        if (Materials.Lubricant.mFluid == null) {
            Materials.Lubricant.mFluid = new Fluid("diesel_engine_test_lubricant");
            FluidRegistry.registerFluid(Materials.Lubricant.mFluid);
        }
        ItemStack container = new ItemStack(model == 0 ? Items.apple : Items.carrot);
        GT_Utility.addFluidContainerData(new FluidContainerData(new FluidStack(fuel, 1000), container, new ItemStack(Items.bucket)));
        GT_Recipe recipe = new GT_Recipe(new ItemStack[]{container}, null, null, null, null, null, 1, 0, 100);
        GT_Recipe.GT_Recipe_Map.sDieselFuels.mRecipeList.add(recipe);
        try {
            for (boolean boost : new boolean[]{false, true}) {
                Engine f = new Engine(model, ForgeDirection.NORTH);
                f.machine.construct(trigger(false), false);
                f.requiredHatches();
                GT_MetaTileEntity_Hatch_Input fuelHatch = (GT_MetaTileEntity_Hatch_Input) f.putHatch(f.sides.get(1), 'I');
                GT_MetaTileEntity_Hatch_Input lubricant = (GT_MetaTileEntity_Hatch_Input) f.putHatch(f.sides.get(2), 'I');
                GT_MetaTileEntity_Hatch_Input oxygen = (GT_MetaTileEntity_Hatch_Input) f.putHatch(f.sides.get(3), 'I');
                assertTrue(f.recheck());
                fuelHatch.mFluid = new FluidStack(fuel, 10000);
                lubricant.mFluid = Materials.Lubricant.getFluid(100);
                oxygen.mFluid = boost ? Materials.Oxygen.getGas(100) : null;
                Field boosted = f.machine.getClass().getDeclaredField("boostEu");
                boosted.setAccessible(true);
                boosted.setBoolean(f.machine, boost);
                f.machine.mEfficiency = boost ? 30000 : 10000;
                f.machine.mRuntime = 0;
                assertTrue(f.machine.checkRecipe(null));
                int consumption = (model == 0 ? boost ? 4096 : 2048 : boost ? 10240 : 8192) / 100;
                assertEquals(10000 - consumption, fuelHatch.mFluid.amount);
                assertEquals(100 - (model == 0 ? boost ? 2 : 1 : boost ? 3 : 2), lubricant.mFluid.amount);
                if (boost) {
                    assertEquals(model == 0 ? 98 : 97, oxygen.mFluid.amount);
                }
                assertEquals(model == 0 ? 2048 : 8192, f.machine.mEUt);
                assertEquals(boost ? 30000 : 10000, f.machine.getMaxEfficiency(null));
                assertEquals(model == 0 ? 15 : 40, f.machine.mEfficiencyIncrease);
                int remainingLubricant = lubricant.mFluid.amount;
                f.machine.mRuntime = 1;
                f.machine.mEfficiency = 1000;
                assertTrue(f.machine.checkRecipe(null));
                assertEquals("Startup below 20% produces no energy", 0, f.machine.mEUt);
                assertEquals("Lubricant is not consumed on every tick", remainingLubricant, lubricant.mFluid.amount);
                lubricant.mFluid = null;
                assertFalse("Lubricant is required", f.machine.checkRecipe(null));
            }
        } finally {
            GT_Recipe.GT_Recipe_Map.sDieselFuels.mRecipeList.remove(recipe);
        }
    }

    private static void checkTooltipAndPersistence(int model) throws Exception {
        Engine f = new Engine(model, ForgeDirection.EAST);
        f.machine.mRuntime = 123;
        NBTTagCompound nbt = new NBTTagCompound();
        f.machine.saveNBTData(nbt);
        Engine restored = new Engine(model, ForgeDirection.EAST);
        restored.machine.loadNBTData(nbt);
        assertEquals(123, restored.machine.mRuntime);
        assertEquals(f.machine.getExtendedFacing(), restored.machine.getExtendedFacing());
        assertEquals(model == 0 ? 24 : 60, f.machine.getPollutionPerTick(null));
        for (String language : new String[]{"en_US", "ru_RU"}) {
            try (InputStream stream = GT_DieselEngineStructureChecks.class.getResourceAsStream(
                    "/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getCurrentDescription(false));
            String structure = String.join("\n", f.machine.getCurrentDescription(true));
            String hints = String.join("\n", f.machine.getStructureDescription(trigger(false)));
            assertTrue(info.contains(language.equals("en_US") ? "Combustion Engine Mk" : "Двигатель внутреннего сгорания Mk"));
            assertTrue(info.contains(model == 0 ? "6144" : "24576"));
            assertTrue(structure.contains(language.equals("en_US") ? "3 x 3 x 4" : "3 × 3 × 4"));
            assertTrue(structure.contains("16"));
            assertTrue(structure.contains("gt_hatch"));
            assertFalse(info.contains("gt_hatch"));
            for (EnumChatFormatting color : new EnumChatFormatting[]{EnumChatFormatting.BLUE,
                    EnumChatFormatting.GOLD, EnumChatFormatting.YELLOW}) {
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

    private static final class Engine {
        final TestWorld world = new TestWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_DieselEngine machine;
        final List<int[]> sides = new ArrayList<>();
        final List<int[]> fixed = new ArrayList<>();
        final List<int[]> intakes = new ArrayList<>();
        final List<int[]> gears = new ArrayList<>();
        final List<int[]> clearance = new ArrayList<>();
        final List<int[]> positions = new ArrayList<>();
        int[] dynamo;
        int[] muffler;

        Engine(int model, ForgeDirection facing) {
            machine = machine(model);
            world.putTile(0, 64, 0, new TestTile(world, machine, facing));
            for (int depth = -1; depth < 4; depth++) {
                for (int y = -1; y <= 1; y++) {
                    for (int side = -1; side <= 1; side++) {
                        int[] p = {-facing.offsetX * depth + (facing.offsetX == 0 ? side : 0),
                                64 + y, -facing.offsetZ * depth + (facing.offsetZ == 0 ? side : 0)};
                        if (depth == -1) {
                            if (side != 0 || y != 0) {
                                clearance.add(p);
                            }
                            continue;
                        }
                        if (depth == 0 && side == 0 && y == 0) {
                            continue;
                        }
                        positions.add(p);
                        if (side == 0 && y == 0) {
                            if (depth == 3) {
                                dynamo = p;
                            } else {
                                gears.add(p);
                            }
                        } else if (depth == 0) {
                            intakes.add(p);
                        } else if (depth == 2 && side == 0 && y == 1) {
                            muffler = p;
                        } else if (depth < 3 && (side == 0 || y == 0)) {
                            sides.add(p);
                        } else {
                            fixed.add(p);
                        }
                    }
                }
            }
        }

        void put(int[] p, ICasing casing) {
            world.setBlock(p[0], p[1], p[2], casing.getBlock(), casing.getMetadata(), 0);
        }

        void casing(int[] p) {
            put(p, machine.getCasingType());
        }

        void clear(int[] p) {
            world.setBlockToAir(p[0], p[1], p[2]);
        }

        MetaTileEntity putHatch(int[] p, char kind) {
            MetaTileEntity hatch = hatch(kind);
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch, ForgeDirection.NORTH));
            return hatch;
        }

        void requiredHatches() {
            putHatch(sides.get(0), 'M');
            putHatch(sides.get(1), 'I');
            putHatch(sides.get(2), 'I');
            putHatch(muffler, 'U');
            putHatch(dynamo, 'D');
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
            return new Source(stack(machine.getCasingType(), 23), stack(machine.getIntakeType(), 8), stack(machine.getGearboxType(), 2));
        }

        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }
}
