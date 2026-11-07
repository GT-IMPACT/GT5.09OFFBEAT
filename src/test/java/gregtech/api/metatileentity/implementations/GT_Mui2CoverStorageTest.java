package gregtech.api.metatileentity.implementations;

import com.google.common.io.ByteStreams;
import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverFactory;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.metatileentity.CoverableTileEntity;
import gregtech.api.net.GT_Packet;
import gregtech.api.net.GT_Packet_CoverData;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverArm;
import gregtech.common.covers.CoverConveyor;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.CoverEUMeter;
import gregtech.common.covers.CoverFluidfilter;
import gregtech.common.covers.CoverPump;
import gregtech.common.covers.CoverShutter;
import gregtech.common.covers.GT_Cover_ControlsWork;
import gregtech.common.covers.conditions.RedstoneCondition;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2CoverStorageTest {

    @Test
    public void legacyCoversMigrateToIndependentNativeObjectsAndCompoundNbt() throws Throwable {
        run("native");
    }

    @Test
    public void unconvertedCoverDataAndRemovalSurviveTheNewStorage() throws Throwable {
        run("legacy");
    }

    @Test
    public void coverPacketCannotChangeServerState() throws Throwable {
        run("packet");
    }

    @Test
    public void nativeCoverRegistrationsSurviveItemIdRemapping() throws Throwable {
        run("remapping");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        int controllerId = GT_Utility.stackToInt(new ItemStack(Items.stick));
        int legacyId = GT_Utility.stackToInt(new ItemStack(Items.apple));
        GregTech_API.registerCoverBehavior(new ItemStack(Items.stick), new GT_Cover_ControlsWork());
        GregTech_API.registerCoverBehavior(new ItemStack(Items.apple), new GT_CoverBehavior() {
        });
        CoverTile first = new CoverTile(world);
        CoverTile second = new CoverTile(world);
        first.xCoord = -19;
        second.xCoord = -18;

        if ("remapping".equals(scenario)) {
            checkRemapping(first);
        } else if ("native".equals(scenario)) {
            int[] ids = new int[6];
            int[] data = new int[6];
            ids[ForgeDirection.NORTH.ordinal()] = controllerId;
            data[ForgeDirection.NORTH.ordinal()] = 1;
            first.load(new NBTTagCompound(), ids, data);
            Cover cover = first.getCoverAtSide(ForgeDirection.NORTH);
            assertTrue(cover instanceof CoverControlsWork);
            assertSame(RedstoneCondition.DISABLE_WITH_REDSTONE, ((CoverControlsWork) cover).getRedstoneCondition());
            cover.setTickRateAddition(17);
            NBTTagCompound saved = first.save();
            assertEquals(1, saved.getTagList(CoverableTileEntity.COVERS_NBT_KEY, 10).tagCount());
            second.load(saved, new int[6], new int[6]);
            Cover restored = second.getCoverAtSide(ForgeDirection.NORTH);
            assertNotSame(cover, restored);
            assertEquals(cover.getCoverID(), restored.getCoverID());
            assertEquals(17, restored.getTickRateAddition());
            assertEquals(1, restored.getLegacyData());
            ((CoverControlsWork) restored).setRedstoneCondition(RedstoneCondition.DISABLE);
            assertEquals(1, cover.getLegacyData());
            assertEquals(2, restored.getLegacyData());
            assertSame(second, restored.getTile());
        } else if ("legacy".equals(scenario)) {
            int[] ids = new int[6];
            int[] data = new int[6];
            ids[ForgeDirection.EAST.ordinal()] = legacyId;
            data[ForgeDirection.EAST.ordinal()] = 3;
            first.load(new NBTTagCompound(), ids, data);
            Cover cover = first.getCoverAtSide(ForgeDirection.EAST);
            assertEquals(3, cover.getLegacyData());
            assertFalse(cover.useMui2());
            second.load(first.save(), new int[6], new int[6]);
            assertEquals(3, second.getCoverAtSide(ForgeDirection.EAST).getLegacyData());
            ItemStack detached = second.detachCover(ForgeDirection.EAST);
            assertSame(Items.apple, detached.getItem());
            assertFalse(second.hasCoverAtSide(ForgeDirection.EAST));
            second.load(second.save(), ids, data);
            assertFalse(second.hasCoverAtSide(ForgeDirection.EAST));
        } else if ("packet".equals(scenario)) {
            first.setCoverIDAtSide((byte) ForgeDirection.WEST.ordinal(), controllerId);
            CoverControlsWork cover = (CoverControlsWork) first.getCoverAtSide(ForgeDirection.WEST);
            cover.setRedstoneCondition(RedstoneCondition.DISABLE);
            GT_Packet_CoverData packet = new GT_Packet_CoverData(first, cover);
            GT_Packet decoded = new GT_Packet_CoverData().decode(ByteStreams.newDataInput(packet.encode()));
            cover.setRedstoneCondition(RedstoneCondition.ENABLE_WITH_REDSTONE);
            decoded.process(world);
            assertSame(RedstoneCondition.ENABLE_WITH_REDSTONE, cover.getRedstoneCondition());
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static void checkRemapping(CoverTile tile) throws Exception {
        Item item = new Item() {
        };
        Item.itemRegistry.addObject(3014, "cover_test:remapped", item);
        CoverFactory[] factories = {
            context -> new CoverPump(context, 128),
            context -> new CoverConveyor(context, 20, 2),
            context -> new CoverArm(context, 20),
            CoverControlsWork::new,
            CoverFluidfilter::new,
            CoverEUMeter::new,
            CoverShutter::new
        };
        Class<?>[] types = new Class<?>[factories.length];
        for (int index = 0; index < factories.length; index++) {
            ItemStack stack = new ItemStack(item, 1, index);
            CoverRegistry.registerCover(stack, null, factories[index]);
            types[index] = CoverRegistry.buildCover(stack, ForgeDirection.EAST, tile).getClass();
        }
        ItemStack wildcard = new ItemStack(item, 1, 32767);
        CoverRegistry.registerCover(wildcard, null, context -> new CoverPump(context, 512));
        assertEquals(
            CoverPump.class,
            CoverRegistry.buildCover(new ItemStack(item, 1, 100), ForgeDirection.EAST, tile).getClass()
        );

        // FML changes numeric IDs before GT rehashes all item-keyed registries on world load.
        Method remap = Item.itemRegistry.getClass().getDeclaredMethod(
            "addObjectRaw",
            int.class,
            String.class,
            Object.class
        );
        remap.setAccessible(true);
        remap.invoke(Item.itemRegistry, 3015, Item.itemRegistry.getNameForObject(item), item);
        assertEquals(3015, Item.getIdFromItem(item));
        for (Map<GT_ItemStack, ?> mapping : GregTech_API.sItemStackMappings) {
            GT_Utility.reMap(mapping);
        }
        for (int index = 0; index < factories.length; index++) {
            ItemStack stack = new ItemStack(item, 1, index);
            assertTrue("Native registration lost for metadata " + index, CoverRegistry.isNativeCover(stack));
            Cover cover = CoverRegistry.buildCover(stack, ForgeDirection.EAST, tile);
            assertEquals(types[index], cover.getClass());
            tile.attachCover(cover);
            cover.setLegacyData(index);
            CoverTile restored = new CoverTile((GT_ProcessingTestSupport.ProcessingWorld) tile.getWorld());
            restored.load(tile.save(), new int[6], new int[6]);
            assertEquals(types[index], restored.getCoverAtSide(ForgeDirection.EAST).getClass());
            assertEquals(cover.getLegacyData(), restored.getCoverDataAtSide((byte) ForgeDirection.EAST.ordinal()));
        }
        Cover pump = CoverRegistry.buildCover(new ItemStack(item, 1, 100), ForgeDirection.EAST, tile);
        assertEquals(CoverPump.class, pump.getClass());
        assertEquals(1, pump.getTickRate());
    }

    private static final class CoverTile extends GT_StructureIntegrationTest.TestTile {

        private CoverTile(GT_ProcessingTestSupport.ProcessingWorld world) {
            super(world, new GT_MetaTileEntity_Hatch_InputBus("cover_test", 1, "", null), ForgeDirection.NORTH);
        }

        private void load(NBTTagCompound tag, int[] ids, int[] data) {
            readCoverNBT(tag, ids, data);
        }

        private NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            writeCoverNBT(tag, false);
            return tag;
        }
    }
}
