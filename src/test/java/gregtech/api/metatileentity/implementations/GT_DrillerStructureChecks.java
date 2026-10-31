package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.ICasing;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.common.blocks.GT_Item_Machines;
import gregtech.common.tileentities.machines.multi.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.Main;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_DrillerStructureChecks {
    private static final Materials[] MATERIALS = {Materials.Steel, Materials.Titanium, Materials.TungstenSteel};
    private static final Casings[] CASINGS = {Casings.SOLID_STEEL, Casings.STABLE_TITANIUM,
            Casings.ROBUST_TUNGSTENSTEEL};
    private static final int[] MATERIAL_INDEX = {0, 1, 2, 0, 1};
    private static final int[] TIERS = {2, 3, 4, 2, 4};
    private static Item frameItem;

    static void run(Item hatchItem, HintProxy oldHints) throws Exception {
        GregTech_API.sBlockCasings2 = Blocks.stained_glass;
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay;
        GregTech_API.sBlockCasings7 = Blocks.wool;
        // The production base resolves its IC2 pipes when the class is initialized.
        ic2.core.Ic2Items.miningPipe = new ItemStack(Blocks.fence);
        ic2.core.Ic2Items.miningPipeTip = new ItemStack(Blocks.nether_brick_fence);
        boolean preload = GregTech_API.sPreloadStarted;
        GregTech_API.sPreloadStarted = true;
        Class.forName(GT_MetaTileEntity_DrillerBase.class.getName());
        GregTech_API.sPreloadStarted = preload;
        Block frameBlock = new Block(Material.iron) {};
        Block.blockRegistry.addObject(3003, "test.driller_frame", frameBlock);
        frameItem = new FrameItem(frameBlock);
        Item.itemRegistry.addObject(3003, "test.driller_frame", frameItem);
        for (int i = 0; i < MATERIALS.length; i++) {
            GT_OreDictUnificator.set(OrePrefixes.frameGt, MATERIALS[i], new ItemStack(frameItem, 1, i), true, true);
        }
        for (int tier = 0; tier <= 6; tier++) {
            GregTech_API.METATILEENTITIES[32520 + tier] = hatch('E', tier);
        }
        GregTech_API.METATILEENTITIES[32530] = hatch('Q', 4);
        GregTech_API.METATILEENTITIES[32531] = hatch('X', 4);
        GregTech_API.METATILEENTITIES[32532] = hatch('A', 6);
        GregTech_API.METATILEENTITIES[32533] = hatch('O', 1);
        DrillerHints hints = new DrillerHints();
        Field proxy = Main.class.getDeclaredField("proxy");
        proxy.setAccessible(true);
        proxy.set(null, hints);
        try {
            for (int model = 0; model < TIERS.length; model++) {
                for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                        ForgeDirection.WEST, ForgeDirection.EAST}) {
                    checkGeometry(model, facing, hints);
                    checkSurvival(model, facing, hatchItem, hints);
                }
                checkTooltipAndState(model);
            }
            checkEnergyAndCircuits();
        } finally {
            proxy.set(null, oldHints);
        }
    }

    private static GT_MetaTileEntity_DrillerBase machine(int model) {
        switch (model) {
            case 0: return new GT_MetaTileEntity_OilDrill1("multimachine.oildrill1");
            case 1: return new GT_MetaTileEntity_OilDrill2("multimachine.oildrill2");
            case 2: return new GT_MetaTileEntity_OilDrill3("multimachine.oildrill3");
            case 3: return new GT_MetaTileEntity_ConcreteBackfiller1("multimachine.concretebackfiller1");
            case 4: return new GT_MetaTileEntity_ConcreteBackfiller2("multimachine.concretebackfiller3");
            default: throw new AssertionError(model);
        }
    }

    private static MetaTileEntity hatch(char kind, int tier) {
        switch (kind) {
            case 'M': return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 'E': return new GT_MetaTileEntity_Hatch_Energy("test", tier, "test", null);
            case 'A': return new GT_MetaTileEntity_Hatch_EnergyMulti("test", tier, 4, "test", null);
            case 'I': return new GT_MetaTileEntity_Hatch_Input("test", 1, "test", null);
            case 'O': return new GT_MetaTileEntity_Hatch_Output("test", 1, "test", null);
            case 'B': return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            case 'Q': return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 'X': return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            case 'D': return new GT_MetaTileEntity_Hatch_DataAccess("test", 1, "test", null);
            default: throw new AssertionError(kind);
        }
    }

    private static void checkGeometry(int model, ForgeDirection facing, DrillerHints hints) throws Exception {
        Rig f = new Rig(model, facing);
        f.machine.construct(trigger(false), false);
        assertEquals(27, f.world.blocks.size());
        assertEquals(15, f.world.frames.size());
        assertSame(CASINGS[MATERIAL_INDEX[model]], f.machine.getCasingType());
        assertFalse(f.recheck());
        for (int[] p : f.fixed) f.assertCasing(p);
        for (int[] p : f.base) f.assertCasing(p);
        f.required();
        assertTrue(f.recheck());
        for (int i = 0; i < f.requiredKinds().length; i++) {
            f.casing(f.base.get(i));
            assertFalse("Missing mandatory hatch", f.recheck());
            f.putHatch(f.base.get(i), f.requiredKinds()[i], TIERS[model]);
        }
        f.putHatch(f.base.get(1), 'E', TIERS[model] - 1);
        assertFalse("Below minimum input voltage", f.recheck());
        f.putHatch(f.base.get(1), 'A', TIERS[model]);
        assertTrue("Multi-amp energy works", f.recheck());
        if (model >= 3) {
            for (char kind : new char[]{'Q', 'X'}) {
                f.putHatch(f.base.get(2), kind, 4);
                assertTrue("Fluid-capable IMPACT inputs", f.recheck());
            }
            f.putHatch(f.base.get(2), 'B', 1);
            assertFalse("An item bus cannot supply concrete", f.recheck());
            f.putHatch(f.base.get(2), 'I', 1);
        }
        f.putHatch(f.base.get(7), 'D', 1);
        assertTrue(f.recheck());
        assertTrue(f.recheck());
        assertEquals(1, f.machine.mDataAccessHatches.size());
        NBTTagCompound hatchTag = new NBTTagCompound();
        f.machine.mDataAccessHatches.get(0).saveNBTData(hatchTag);
        assertEquals(f.machine.getCasingType().getTextureId(),
                (hatchTag.getByte("mMachineBlock") & 127) + 128 * hatchTag.getByte("mTexturePage"));
        f.casing(f.base.get(7));
        assertTrue(f.recheck());
        assertTrue(f.machine.mDataAccessHatches.isEmpty());
        int[] column = f.fixed.get(0);
        f.putHatch(column, 'D', 1);
        assertFalse("Hatches are restricted to the base", f.recheck());
        f.casing(column);
        int[] frame = f.framePositions.get(0);
        f.world.putFrame(frame, MATERIALS[(MATERIAL_INDEX[model] + 1) % MATERIALS.length]);
        assertFalse("Different frame material is not accepted", f.recheck());
        f.world.putFrame(frame, MATERIALS[MATERIAL_INDEX[model]]);
        assertTrue(f.recheck());
        // Unspecified corners and the drilling column below the base remain outside the structure.
        int[] corner = f.pos(-1, -5, 0);
        f.world.setBlock(corner[0], corner[1], corner[2], Blocks.bedrock, 0, 0);
        int[] pipe = f.pos(0, 1, 1);
        f.world.setBlock(pipe[0], pipe[1], pipe[2], Blocks.fence, 0, 0);
        assertTrue(f.recheck());
        hints.reset();
        int changes = f.world.changes;
        int maintenance = f.machine.mMaintenanceHatches.size();
        f.world.isRemote = true;
        f.machine.construct(trigger(true), true);
        f.world.isRemote = false;
        assertEquals(26, hints.hints);
        assertEquals(8, hints.hintFrames.size());
        assertEquals(15, hints.frameTints.size());
        for (int[] p : f.base) {
            String key = TestWorld.key(p[0], p[1], p[2]);
            assertSame(f.machine.getCasingType().getBlock(), hints.hintBlocks.get(key));
            assertSame(HintColor.BLUE, hints.hintFrames.get(key));
        }
        for (short[] tint : hints.frameTints.values()) assertArrayEquals(MATERIALS[MATERIAL_INDEX[model]].mRGBa, tint);
        assertEquals(changes, f.world.changes);
        assertEquals(maintenance, f.machine.mMaintenanceHatches.size());
        Rig other = new Rig((model + 1) % TIERS.length, facing);
        other.machine.construct(trigger(false), false);
        assertFalse(other.recheck());
        assertTrue(f.recheck());
        assertFalse("No extended rotations", f.machine.isNewExtendedFacingValid(
                space.impact.structures.alignment.enumerable.ExtendedFacing.of(facing,
                        space.impact.structures.alignment.enumerable.Rotation.CLOCKWISE,
                        space.impact.structures.alignment.enumerable.Flip.NONE)));
    }

    private static void checkSurvival(int model, ForgeDirection facing, Item hatchItem, DrillerHints hints) {
        Rig f = new Rig(model, facing);
        Source source = new Source(new ItemStack(hatchItem, 1, 32520), new ItemStack(hatchItem, 1, 32520 + TIERS[model]),
                new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32003),
                new ItemStack(hatchItem, 1, 32530), new ItemStack(hatchItem, 1, 32533),
                stack(f.machine.getCasingType(), 11), new ItemStack(frameItem, 15, MATERIAL_INDEX[model]));
        for (int i = 0; i < 35 && !f.machine.mMachine; i++) {
            int before = source.consumed;
            int budget = i % 3 + 1;
            int built = f.build(true, budget, source);
            assertTrue("Bounded positive progress: model=" + model + ", pass=" + i + ", built=" + built
                    + ", consumed=" + source.consumed + ", messages=" + f.player.messages, built > 0 && built <= budget);
            assertEquals(built, source.consumed - before);
        }
        assertTrue("All variants can be built", f.machine.mMachine);
        assertEquals(26, source.consumed);
        assertEquals("Low-tier hatch not selected", 1, source.stacks.get(0).stackSize);
        assertEquals(1, f.machine.mMaintenanceHatches.size());
        assertEquals(1, f.machine.mEnergyHatches.size());
        assertEquals(-1, f.build(true, 100, source));
        assertEquals(26, source.consumed);
        for (GT_MetaTileEntity_Hatch_Energy hatch : f.machine.mEnergyHatches) {
            int side = hatch.getBaseMetaTileEntity().getFrontFacing();
            ForgeDirection outward = ForgeDirection.getOrientation(side);
            int x = hatch.getBaseMetaTileEntity().getXCoord() + outward.offsetX;
            int y = hatch.getBaseMetaTileEntity().getYCoord() + outward.offsetY;
            int z = hatch.getBaseMetaTileEntity().getZCoord() + outward.offsetZ;
            assertTrue("Hatch faces outside the tower", f.world.isAirBlock(x, y, z));
        }
        for (boolean channel : new boolean[]{false, true}) {
            Rig shell = new Rig(model, facing);
            Source blocks = channel ? shell.blocks() : new Source(
                    new ItemStack(hatchItem, 1, 32000), new ItemStack(hatchItem, 1, 32520 + TIERS[model]),
                    stack(shell.machine.getCasingType(), 11), new ItemStack(frameItem, 15, MATERIAL_INDEX[model]));
            assertEquals(26, shell.build(channel, 100, blocks));
            if (!channel) {
                assertEquals("Channel off: maintenance item is untouched", 1, blocks.stacks.get(0).stackSize);
                assertEquals("Channel off: energy item is untouched", 1, blocks.stacks.get(1).stackSize);
            }
            assertEquals(0, shell.build(channel, 100, blocks));
            assertEquals(26, blocks.consumed);
            assertFalse(shell.recheck());
            assertTrue(shell.machine.mEnergyHatches.isEmpty());
        }
        Rig blocked = new Rig(model, facing);
        int[] obstruction = blocked.framePositions.get(0);
        int[] obstruction2 = blocked.base.get(7);
        blocked.world.putFrame(obstruction, MATERIALS[(MATERIAL_INDEX[model] + 1) % MATERIALS.length]);
        blocked.world.setBlock(obstruction2[0], obstruction2[1], obstruction2[2], Blocks.bedrock, 0, 0);
        Source blocks = blocked.blocks();
        hints.reset();
        int changes = blocked.world.changes;
        assertEquals(0, blocked.build(true, 1, blocks));
        assertEquals(changes, blocked.world.changes);
        assertEquals(0, blocks.queries);
        assertEquals(2, hints.errorPositions.size());
        blocked.machine.construct(trigger(false), false);
        assertTrue("Creative builds the unobstructed part", blocked.world.changes > changes);
        assertSame(Blocks.bedrock, blocked.world.getBlock(obstruction2[0], obstruction2[1], obstruction2[2]));
        Rig missing = new Rig(model, facing);
        assertEquals(0, missing.build(true, 10, new Source()));
        assertFalse(missing.player.messages.isEmpty());
        assertEquals(1, missing.world.blocks.size());
        assertEquals(0, missing.build(true, 0, missing.blocks()));
    }

    private static void checkTooltipAndState(int model) throws Exception {
        Rig f = new Rig(model, ForgeDirection.WEST);
        String english = null;
        for (String language : new String[]{"en_US", "ru_RU", "en_US"}) {
            try (InputStream stream = GT_DrillerStructureChecks.class.getResourceAsStream("/assets/gregtech/lang/" + language + ".lang")) {
                StringTranslate.inject(stream);
            }
            GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
            String info = String.join("\n", f.machine.getPrimaryDescription());
            String structure = String.join("\n", f.machine.getSecondaryDescription());
            assertFalse(info.contains("TileEntity_DESCRIPTION"));
            assertFalse(structure.contains("gt.tooltip"));
            assertTrue(structure.contains(language.equals("ru_RU") ? "3 × 7 × 3" : "3 x 7 x 3"));
            assertTrue(structure.contains("gt_hatch"));
            assertTrue(structure.contains("\u00a79\u25a0\u00a7r - "));
            assertTrue("Compact structure tooltip", f.machine.getSecondaryDescription().length <= 15);
            assertTrue("Compact information tooltip", f.machine.getPrimaryDescription().length <= 6);
            if (language.equals("en_US")) {
                if (english == null) english = info; else assertEquals(english, info);
            } else assertNotEquals(english, info);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("workState", 2);
        f.machine.loadNBTData(tag);
        NBTTagCompound saved = new NBTTagCompound();
        f.machine.saveNBTData(saved);
        assertEquals(2, saved.getInteger("workState"));
        assertEquals(ForgeDirection.WEST, f.machine.getExtendedFacing().getDirection());
        tag = new NBTTagCompound();
        tag.setBoolean("isPickingPipes", true);
        f.machine.loadNBTData(tag);
        f.machine.saveNBTData(saved);
        assertEquals(2, saved.getInteger("workState"));
        assertTrue(f.machine.newMetaEntity(null) instanceof GT_MetaTileEntity_DrillerBase);
    }

    private static void checkEnergyAndCircuits() throws Exception {
        Rig f = new Rig(4, ForgeDirection.NORTH);
        f.machine.construct(trigger(false), false);
        f.required();
        f.putHatch(f.base.get(1), 'A', 4);
        f.putHatch(f.base.get(7), 'D', 1);
        assertTrue(f.recheck());
        Method energy = GT_MetaTileEntity_DrillerBase.class.getDeclaredMethod("isEnergyEnough");
        energy.setAccessible(true);
        assertFalse((boolean) energy.invoke(f.machine));
        f.machine.mEnergyHatchesMulti.get(0).setEUVar(10000000);
        assertTrue("Multi-amp hatches contribute stored energy", (boolean) energy.invoke(f.machine));
        ItemList.Circuit_Integrated.set(new ItemStack(Items.comparator));
        GT_MetaTileEntity_Hatch_DataAccess data = f.machine.mDataAccessHatches.get(0);
        data.mInventory[0] = new ItemStack(Items.comparator, 1, 7);
        data.mInventory[1] = new ItemStack(Items.comparator, 1, 9);
        assertEquals(16, f.machine.getTotalConfigValue());
        assertTrue(f.recheck());
        assertEquals("Circuits are not duplicated by rechecking", 16, f.machine.getTotalConfigValue());
        Method radius = GT_MetaTileEntity_ConcreteBackfiller2.class.getDeclaredMethod("getRadius");
        radius.setAccessible(true);
        assertEquals(32, radius.invoke(f.machine));
        f.casing(f.base.get(7));
        assertTrue(f.recheck());
        assertEquals(64, radius.invoke(f.machine));
    }

    private static ItemStack stack(ICasing casing, int count) {
        return new ItemStack(casing.getBlock(), count, casing.getMetadata());
    }

    private static ItemStack trigger(boolean hatches) {
        ItemStack trigger = new ItemStack(Items.stick);
        if (hatches) ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        return trigger;
    }

    private static final class Rig {
        final int model;
        final ForgeDirection facing;
        final RigWorld world = new RigWorld();
        final TestPlayer player = new TestPlayer(world);
        final GT_MetaTileEntity_DrillerBase machine;
        final TestTile controller;
        final List<int[]> base = new ArrayList<>();
        final List<int[]> fixed = new ArrayList<>();
        final List<int[]> framePositions = new ArrayList<>();

        Rig(int model, ForgeDirection facing) {
            this.model = model;
            this.facing = facing;
            machine = machine(model);
            controller = new TestTile(world, machine, facing);
            world.putTile(0, 64, 0, controller);
            for (int c = 0; c < 3; c++) for (int a = -1; a <= 1; a++) {
                if (a != 0 || c != 0) base.add(pos(a, 0, c));
            }
            for (int height = 1; height <= 3; height++) {
                fixed.add(pos(0, -height, 1));
                framePositions.add(pos(-1, -height, 1));
                framePositions.add(pos(1, -height, 1));
                framePositions.add(pos(0, -height, 0));
                framePositions.add(pos(0, -height, 2));
                framePositions.add(pos(0, -height - 3, 1));
            }
        }

        int[] pos(int a, int b, int c) {
            return new int[]{facing.offsetZ * a - facing.offsetX * c, 64 - b,
                    -facing.offsetX * a - facing.offsetZ * c};
        }

        void casing(int[] p) {
            ICasing c = machine.getCasingType();
            world.setBlock(p[0], p[1], p[2], c.getBlock(), c.getMetadata(), 0);
        }

        void assertCasing(int[] p) {
            assertSame(machine.getCasingType().getBlock(), world.getBlock(p[0], p[1], p[2]));
            assertEquals(machine.getCasingType().getMetadata(), world.getBlockMetadata(p[0], p[1], p[2]));
        }

        char[] requiredKinds() {
            return model < 3 ? new char[]{'M', 'E', 'O'} : new char[]{'M', 'E', 'I'};
        }

        void required() {
            char[] kinds = requiredKinds();
            for (int i = 0; i < kinds.length; i++) putHatch(base.get(i), kinds[i], TIERS[model]);
        }

        void putHatch(int[] p, char kind, int tier) {
            world.putTile(p[0], p[1], p[2], new TestTile(world, hatch(kind, tier), ForgeDirection.NORTH));
        }

        boolean recheck() {
            ((GT_MetaTileEntity_MultiBlockBase) machine).setupMachine(controller, null);
            return machine.mMachine;
        }

        Source blocks() {
            return new Source(stack(machine.getCasingType(), 11), new ItemStack(frameItem, 15, MATERIAL_INDEX[model]));
        }

        int build(boolean channel, int budget, Source source) {
            return machine.survivalConstruct(trigger(channel), budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }

    private static final class RigWorld extends TestWorld {
        final Map<String, BaseMetaPipeEntity> frames = new HashMap<>();

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            BaseMetaPipeEntity frame = frames.get(key(x, y, z));
            return frame == null ? super.getTileEntity(x, y, z) : frame;
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
            frames.remove(key(x, y, z));
            return super.setBlock(x, y, z, block, meta, flags);
        }

        void putFrame(int[] p, Materials material) {
            GT_MetaPipeEntity_Frame frame = new GT_MetaPipeEntity_Frame("test.frame", material);
            setBlock(p[0], p[1], p[2], Blocks.iron_block, frame.getTileEntityBaseType(), 0);
            BaseMetaPipeEntity tile = new BaseMetaPipeEntity() {
                @Override public void markDirty() {}
            };
            tile.setWorldObj(this);
            tile.xCoord = p[0]; tile.yCoord = p[1]; tile.zCoord = p[2];
            frame.setBaseMetaTileEntity(tile);
            frames.put(key(p[0], p[1], p[2]), tile);
        }
    }

    private static final class FrameItem extends GT_Item_Machines {
        FrameItem(Block block) { super(block); }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return MATERIALS[stack.getItemDamage()].mName + " Frame Box";
        }

        @Override
        public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                                    int side, float hitX, float hitY, float hitZ, int meta) {
            ((RigWorld) world).putFrame(new int[]{x, y, z}, MATERIALS[stack.getItemDamage()]);
            return true;
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                                 int side, float hitX, float hitY, float hitZ) {
            placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, stack.getItemDamage());
            stack.stackSize--;
            return true;
        }
    }

    private static final class DrillerHints extends HintProxy {
        final Map<String, short[]> frameTints = new HashMap<>();

        @Override
        public void hintParticleTinted(World world, int x, int y, int z, IIcon[] icons, short[] tint,
                                       ItemStack displayStack) {
            hints++;
            assertEquals(6, icons.length);
            assertNotNull("Frame hints carry the item used for their localized name", displayStack);
            assertSame(frameItem, displayStack.getItem());
            assertEquals(1, displayStack.stackSize);
            assertArrayEquals(MATERIALS[displayStack.getItemDamage()].mRGBa, tint);
            frameTints.put(TestWorld.key(x, y, z), tint.clone());
        }

        void reset() {
            hints = 0;
            hintBlocks.clear(); hintMetadata.clear(); hintFrames.clear(); errorPositions.clear(); frameTints.clear();
        }
    }
}
