package gregtech.api.metatileentity.implementations;

import com.google.common.io.ByteStreams;
import gregtech.api.GregTech_API;
import gregtech.api.metatileentity.CoverableTileEntity;
import gregtech.api.net.GT_Packet;
import gregtech.api.net.GT_Packet_CoverData;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.GT_Cover_ControlsWork;
import gregtech.common.covers.GT_Cover_Shutter;
import gregtech.common.covers.conditions.RedstoneCondition;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

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

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        int controllerId = GT_Utility.stackToInt(new ItemStack(Items.stick));
        int shutterId = GT_Utility.stackToInt(new ItemStack(Items.apple));
        GregTech_API.registerCoverBehavior(new ItemStack(Items.stick), new GT_Cover_ControlsWork());
        GregTech_API.registerCoverBehavior(new ItemStack(Items.apple), new GT_Cover_Shutter());
        CoverTile first = new CoverTile(world);
        CoverTile second = new CoverTile(world);
        first.xCoord = -19;
        second.xCoord = -18;

        if ("native".equals(scenario)) {
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
            ids[ForgeDirection.EAST.ordinal()] = shutterId;
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
