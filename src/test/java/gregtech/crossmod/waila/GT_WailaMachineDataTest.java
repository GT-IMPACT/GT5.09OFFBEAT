package gregtech.crossmod.waila;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicBatteryBuffer;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Transformer;
import gregtech.api.util.GT_Config;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BrickedBlastFurnace;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ElectricBlastFurnace;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_SuperChest;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.common.config.Configuration;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class GT_WailaMachineDataTest {

    @Test
    public void machineSnapshotsAndConfiguration() throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LaunchClassLoader previousLaunch = Launch.classLoader;
        Map<String, Object> previousBlackboard = Launch.blackboard;
        try (LaunchClassLoader loader = new LaunchClassLoader(((URLClassLoader) getClass().getClassLoader()).getURLs())) {
            Launch.classLoader = loader;
            Launch.blackboard = new HashMap<>();
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class)
                    .invoke(null, (Object) new Object[]{"7", "99", "40", "1614", "1.7.10", "9.05",
                            new File("."), new ArrayList<String>()});
            loader.loadClass(getClass().getName()).getMethod("runChecks").invoke(null);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            Launch.classLoader = previousLaunch;
            Launch.blackboard = previousBlackboard;
        }
    }

    public static void runChecks() throws Exception {
        Field side = FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, Side.SERVER);
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, new File("."));
        Bootstrap.func_151354_b();
        File config = File.createTempFile("waila-machines", ".cfg");
        config.deleteOnExit();
        GregTech_API.sMachineFile = new GT_Config(new Configuration(config));
        checkTransformerModes();
        checkChestSnapshot();
        checkLargeBatteryCapacity();
        checkMultiblockAndHatchData();
    }

    private static void checkTransformerModes() {
        GT_MetaTileEntity_Transformer transformer = new GT_MetaTileEntity_Transformer("test", 1, "test", null);
        attach(transformer, true);
        NBTTagCompound down = snapshot(transformer);
        attach(transformer, false);
        NBTTagCompound up = snapshot(transformer);
        assertEquals(down.getLong("maxEUInput"), up.getLong("maxEUOutput"));
        assertEquals(down.getLong("maxEUOutput"), up.getLong("maxEUInput"));
        // Side classification must use the server snapshot, even after the local mode changed.
        List<String> front = tooltip(transformer, down, 2, "");
        List<String> back = tooltip(transformer, down, 3, "");
        assertNotEquals(front.get(1), back.get(1));
        assertEquals(0, tooltip(transformer, down, 2, "all").size());
    }

    private static void checkChestSnapshot() {
        GT_MetaTileEntity_SuperChest chest = new GT_MetaTileEntity_SuperChest("test", 1, "test", null);
        attach(chest, true);
        chest.mItemStack = new ItemStack(Items.diamond);
        chest.mItemCount = 12345;
        NBTTagCompound tag = snapshot(chest);
        chest.mItemStack = null;
        chest.mItemCount = 0;
        assertTrue(tooltip(chest, tag, 2, "").toString().contains(Items.diamond.getItemStackDisplayName(new ItemStack(Items.diamond))));
        chest.getWailaNBTData(null, null, tag, null, 0, 0, 0);
        assertFalse(tag.hasKey("chestBaseItem"));
    }

    private static void checkLargeBatteryCapacity() {
        GT_MetaTileEntity_BasicBatteryBuffer battery = new GT_MetaTileEntity_BasicBatteryBuffer("test", 1, "test", null, 0) {
            @Override
            public long[] getStoredEnergy() {
                return new long[]{4000000100L, 8000000200L};
            }
        };
        attach(battery, true);
        NBTTagCompound tag = snapshot(battery);
        assertEquals(4000000000L, tag.getLong("nowStorage"));
        assertEquals(8000000000L, tag.getLong("maxStorage"));
        assertEquals(5000000000L, tag.getLong("energyInput"));
        assertEquals(6000000000L, tag.getLong("energyOutput"));
        assertEquals(0, tooltip(battery, tag, 2, "all").size());
    }

    private static void checkMultiblockAndHatchData() {
        GT_MetaTileEntity_ElectricBlastFurnace multi = new GT_MetaTileEntity_ElectricBlastFurnace("test");
        attach(multi, true);
        multi.mProgresstime = 100;
        multi.mMaxProgresstime = 200;
        multi.mEfficiency = 8750;
        NBTTagCompound tag = snapshot(multi);
        assertEquals(87.5F, tag.getFloat("efficiency"), 0.0F);
        assertEquals(200, tag.getInteger("maxProgress"));
        assertTrue(tag.getBoolean("incompleteStructure"));
        assertEquals(0, tooltip(multi, tag, 2, "all").size());
        GT_MetaTileEntity_BrickedBlastFurnace primitive = new GT_MetaTileEntity_BrickedBlastFurnace("test");
        attach(primitive, true);
        assertTrue(snapshot(primitive).getBoolean("incompleteStructurePrimitiveBlastFurnace"));
        primitive.mMachine = true;
        assertFalse(snapshot(primitive).getBoolean("incompleteStructurePrimitiveBlastFurnace"));
        GT_MetaTileEntity_Hatch_Energy hatch = new GT_MetaTileEntity_Hatch_Energy("test", 1, "test", null);
        attach(hatch, true);
        hatch.idHatch = 23;
        assertEquals(23, snapshot(hatch).getInteger("hatchId"));
    }

    private static void attach(MetaTileEntity machine, boolean allowed) {
        IGregTechTileEntity base = (IGregTechTileEntity) Proxy.newProxyInstance(
                GT_WailaMachineDataTest.class.getClassLoader(), new Class<?>[]{IGregTechTileEntity.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setMetaTileEntity": return null;
                        case "isActive": return true;
                        case "isAllowedToWork": return allowed;
                        case "getFrontFacing": return (byte) 2;
                        case "getErrorDisplayID": return 64;
                        case "getStoredEU": return 100L;
                        case "getEUCapacity": return 200L;
                        case "getAverageElectricInput": return 5000000000L;
                        case "getAverageElectricOutput": return 6000000000L;
                        default: throw new AssertionError("Unexpected tile call: " + method);
                    }
                });
        machine.setBaseMetaTileEntity(base);
    }

    private static NBTTagCompound snapshot(MetaTileEntity machine) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.getWailaNBTData(null, null, tag, null, 0, 0, 0);
        return tag;
    }

    private static List<String> tooltip(MetaTileEntity machine, NBTTagCompound tag, int side, String disabled) {
        IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
                GT_WailaMachineDataTest.class.getClassLoader(), new Class<?>[]{IWailaDataAccessor.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getNBTData": return tag;
                        case "getSide": return ForgeDirection.getOrientation(side);
                        default: throw new AssertionError("Unexpected accessor call: " + method);
                    }
                });
        IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
                GT_WailaMachineDataTest.class.getClassLoader(), new Class<?>[]{IWailaConfigHandler.class},
                (proxy, method, args) -> !disabled.equals("all") && !args[0].equals(disabled));
        List<String> result = new ArrayList<>();
        machine.getWailaBody(null, result, accessor, config);
        return result;
    }
}
