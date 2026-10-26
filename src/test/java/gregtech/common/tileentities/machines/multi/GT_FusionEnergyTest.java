package gregtech.common.tileentities.machines.multi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy;
import gregtech.api.util.GT_Config;
import gregtech.api.util.GT_Recipe;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;

public class GT_FusionEnergyTest {
    @Test
    public void fusionEnergyTransferAndStarvation() throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LaunchClassLoader previousLaunch = Launch.classLoader;
        java.util.Map<String, Object> previousBlackboard = Launch.blackboard;
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
        Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, new File("."));
        Bootstrap.func_151354_b();
        File config = File.createTempFile("fusion-energy", ".cfg");
        config.deleteOnExit();
        GregTech_API.sMachineFile = new GT_Config(new Configuration(config));

        GT_MetaTileEntity_FusionComputer[] reactors = {
            new GT_MetaTileEntity_FusionComputer1("test1"),
            new GT_MetaTileEntity_FusionComputer2("test2"),
            new GT_MetaTileEntity_FusionComputer3("test3"),
            new GT_MetaTileEntity_FusionComputer4("test4"),
            new GT_MetaTileEntity_FusionComputer5("test5")
        };
        long[] inputs = {2048, 4096, 8192, 32768, 131072};
        for (int i = 0; i < reactors.length; i++) {
            GT_MetaTileEntity_FusionComputer reactor = reactors[i];
            EnergyTile tile = prepare(reactor, 1, 1000000);
            reactor.onPostTick(tile.entity, 1);
            assertEquals("Input for MK " + (i + 1), 1000000 + inputs[i], tile.energy);
            assertEquals(tile.energy, reactor.mEUStore);

            tile.energy = reactor.maxEUStore() - inputs[i];
            reactor.onPostTick(tile.entity, 2);
            assertEquals("Can fill buffer exactly", reactor.maxEUStore(), tile.energy);
        }

        GT_MetaTileEntity_FusionComputer reactor = reactors[4];
        EnergyTile tile = prepare(reactor, 16, 1000000);
        reactor.mMaxProgresstime = 20;
        reactor.mEUt = 786432;
        reactor.mEUStore = 0; // A stale snapshot must not stop a powered reactor.
        reactor.onPostTick(tile.entity, 1);
        assertEquals(1000000 + 16 * 131072 - 786432, tile.energy);
        assertEquals(1, reactor.mProgresstime);

        for (GT_MetaTileEntity_Hatch_Energy hatch : reactor.mEnergyHatches) {
            hatch.getBaseMetaTileEntity().decreaseStoredEnergyUnits(10000000, true);
        }
        tile.energy = 786431;
        reactor.mEUStore = 1000000; // A stale snapshot must not allow unpaid progress.
        reactor.onPostTick(tile.entity, 2);
        assertEquals(0, reactor.mMaxProgresstime);
        assertEquals(786431, tile.energy);
        assertEquals(tile.energy, reactor.mEUStore);

        checkRecipeSnapshot(reactors);
        checkOverclockLimits(reactors[4]);
        checkRecipeInputs();
    }

    private static void checkRecipeSnapshot(GT_MetaTileEntity_FusionComputer[] reactors) throws Exception {
        // Base values exported from all 42 live sFusionRecipes through ForgeMCP.
        JsonArray recipes;
        try (InputStreamReader reader = new InputStreamReader(
                GT_FusionEnergyTest.class.getResourceAsStream("/gregtech/fusion-recipes.json"), "UTF-8")) {
            recipes = new JsonParser().parse(reader).getAsJsonArray();
        }
        assertEquals(42, recipes.size());
        int combinations = 0;
        for (int tier = 0; tier < reactors.length; tier++) {
            GT_MetaTileEntity_FusionComputer reactor = reactors[tier];
            prepare(reactor, 16, 0);
            for (JsonElement element : recipes) {
                JsonObject recipe = element.getAsJsonObject();
                int start = recipe.get("startupEU").getAsInt();
                int expected = recipe.getAsJsonArray("multipliers").get(tier).getAsInt();
                if (start > reactor.maxEUStore()) {
                    assertEquals(0, expected);
                    continue;
                }
                combinations++;
                int eut = recipe.get("eut").getAsInt();
                int duration = recipe.get("ticks").getAsInt();
                int actual = reactor.overclock(start, duration, eut);
                assertEquals("MK " + (tier + 1) + ": " + recipe.get("name").getAsString(), expected, actual);
                if (actual > 0) {
                    assertTrue((long) eut * actual <= 16 * reactor.getEnergyInputPerHatch());
                    if (actual > 1) assertTrue((long) eut * actual < 16 * reactor.getEnergyInputPerHatch());
                    assertTrue(duration >= actual);
                }
            }
        }
        assertEquals(162, combinations);
    }

    private static void checkOverclockLimits(GT_MetaTileEntity_FusionComputer reactor) {
        prepare(reactor, 16, 0);
        assertEquals(256, reactor.overclock(40000000, 256, 8191));
        assertEquals(64, reactor.overclock(40000000, 256, 8192)); // Leave headroom at equality.
        assertEquals(64, reactor.overclock(40000000, 256, 8193));
        assertEquals(0, reactor.overclock(40000000, 256, Integer.MAX_VALUE));
        assertEquals(1, reactor.overclock(40000000, 1, 2048));
        assertEquals(1, reactor.overclock(40000000, 3, 2048));
        assertEquals(4, reactor.overclock(40000000, 4, 2048));
        assertEquals(16, reactor.overclock(40000000, 32, 2048));
        assertEquals(64, reactor.overclock(40000000, 64, 2048));
        assertEquals(4, reactor.overclock(1000000000, 80, 196608)); // Phoenixite.

        prepare(reactor, 1, 0);
        assertEquals(4, reactor.overclock(40000000, 64, 24576));
        assertEquals(1, reactor.overclock(40000000, 64, 32768));
        assertEquals(1, reactor.overclock(40000000, 64, 131072)); // Base recipe still fits exactly.
        assertEquals(0, reactor.overclock(40000000, 64, 131073));
        reactor.mEnergyHatches.add(new GT_MetaTileEntity_Hatch_Energy("unattached", reactor.tier(), "", null));
        assertEquals(0, reactor.overclock(40000000, 80, 196608));
        prepare(reactor, 0, 0);
        assertEquals(0, reactor.overclock(40000000, 64, 2048));
    }

    private static void checkRecipeInputs() {
        TestFusionComputer reactor = new TestFusionComputer();
        prepare(reactor, 1, 0);
        Fluid first = new Fluid("fusion_test_first");
        Fluid second = new Fluid("fusion_test_second");
        Fluid output = new Fluid("fusion_test_output");
        FluidRegistry.registerFluid(first);
        FluidRegistry.registerFluid(second);
        FluidRegistry.registerFluid(output);
        reactor.fluids.add(new FluidStack(first, 32));
        reactor.fluids.add(new FluidStack(second, 64));
        GT_Recipe recipe = new GT_Recipe(null, null, null, null,
            new FluidStack[]{new FluidStack(first, 16), new FluidStack(second, 32)},
            new FluidStack[]{new FluidStack(output, 17)}, 32, 196608, 40000000);
        GT_Recipe.GT_Recipe_Map.sFusionRecipes.addRecipe(recipe);
        reactor.mLastRecipe = recipe;
        assertFalse(reactor.checkRecipe(null));
        assertEquals(32, reactor.fluids.get(0).amount);
        assertEquals(64, reactor.fluids.get(1).amount);

        recipe.mEUt = 32768;
        reactor.mLastRecipe = recipe;
        assertTrue(reactor.checkRecipe(null));
        assertEquals(32768, reactor.mEUt);
        assertEquals(32, reactor.mMaxProgresstime);
        assertEquals(16, reactor.fluids.get(0).amount);
        assertEquals(32, reactor.fluids.get(1).amount);
    }

    private static class TestFusionComputer extends GT_MetaTileEntity_FusionComputer5 {
        final ArrayList<FluidStack> fluids = new ArrayList<>();

        TestFusionComputer() { super("recipe_test"); }

        @Override
        public ArrayList<FluidStack> getStoredFluids() { return new ArrayList<>(fluids); }

        @Override
        public void updateSlots() {}

        @Override
        public boolean turnCasingActive(boolean active) { return true; }
    }

    private static EnergyTile prepare(GT_MetaTileEntity_FusionComputer reactor, int hatches, long energy) {
        reactor.mEnergyHatches.clear();
        for (int i = 0; i < hatches; i++) {
            GT_MetaTileEntity_Hatch_Energy hatch = new GT_MetaTileEntity_Hatch_Energy("test", reactor.tier(), "", null);
            new EnergyTile(hatch, 10000000);
            reactor.mEnergyHatches.add(hatch);
        }
        reactor.mMachine = true;
        reactor.mUpdate = 100;
        reactor.mStartUpCheck = -1;
        reactor.updateRepairStatus();
        return new EnergyTile(reactor, energy);
    }

    private static class EnergyTile {
        long energy;
        final IGregTechTileEntity entity;

        EnergyTile(MetaTileEntity machine, long stored) {
            energy = stored;
            entity = (IGregTechTileEntity) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{IGregTechTileEntity.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMetaTileEntity": return machine;
                        case "isServerSide": return true;
                        case "getStoredEU": return energy;
                        case "increaseStoredEnergyUnits": energy += (Long) args[0]; return true;
                        case "decreaseStoredEnergyUnits":
                            long amount = (Long) args[0];
                            if (energy < amount && !(Boolean) args[1]) return false;
                            energy = Math.max(0, energy - amount);
                            return true;
                        default:
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                            if (method.getReturnType() == long.class) return 0L;
                            return null;
                    }
                });
            machine.setBaseMetaTileEntity(entity);
        }
    }
}
