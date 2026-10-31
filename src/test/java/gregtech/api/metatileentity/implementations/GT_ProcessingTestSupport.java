package gregtech.api.metatileentity.implementations;

import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import gregtech.GT_Mod;
import gregtech.common.GT_Server;
import gregtech.api.enums.GT_Values;
import gregtech.api.net.IGT_NetworkHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GT_Config;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Test-only Minecraft bootstrap and real hatch fixtures; no recipe logic is replaced. */
public final class GT_ProcessingTestSupport {
    private GT_ProcessingTestSupport() {}

    public static final class GameRuntime implements AutoCloseable {
        private final ClassLoader previousContext = Thread.currentThread().getContextClassLoader();
        private final LaunchClassLoader previousLaunch = Launch.classLoader;
        private final Map<String, Object> previousBlackboard = Launch.blackboard;
        private final LaunchClassLoader loader;

        public GameRuntime() throws Exception {
            loader = new LaunchClassLoader(((URLClassLoader) getClass().getClassLoader()).getURLs());
            try {
                Launch.classLoader = loader;
                Launch.blackboard = new HashMap<>();
                Thread.currentThread().setContextClassLoader(loader);
                loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class)
                        .invoke(null, (Object) new Object[]{"7", "99", "40", "1614", "1.7.10", "9.05",
                                new File("."), new ArrayList<String>()});
                loader.loadClass(GT_ProcessingTestSupport.class.getName()).getMethod("bootstrap").invoke(null);
            } catch (Exception e) {
                close();
                throw e;
            }
        }

        public void run(Class<?> suite, String scenario, String model) throws Throwable {
            try {
                loader.loadClass(suite.getName()).getMethod("runScenario", String.class, String.class)
                        .invoke(null, scenario, model);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        @Override
        public void close() throws Exception {
            try {
                loader.close();
            } finally {
                Thread.currentThread().setContextClassLoader(previousContext);
                Launch.classLoader = previousLaunch;
                Launch.blackboard = previousBlackboard;
            }
        }
    }

    public static void bootstrap() throws Exception {
        setStatic(FMLRelaunchLog.class, "side", Side.SERVER);
        setStatic(FMLInjectionData.class, "minecraftHome", new File("."));
        Bootstrap.func_151354_b();
        File config = File.createTempFile("processing-test", ".cfg");
        config.deleteOnExit();
        GregTech_API.sMachineFile = new GT_Config(new Configuration(config));
        // The real proxy constructor registers Forge services. Only its configuration fields
        // are needed here; classloader isolation keeps this test instance out of other suites.
        Class<?> factoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
        Constructor<?> constructor = (Constructor<?>) factoryClass
                .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                .invoke(factory, GT_Server.class, Object.class.getDeclaredConstructor());
        constructor.setAccessible(true);
        GT_Mod.gregtechproxy = (GT_Server) constructor.newInstance();
        // Network delivery is outside the processing contract; no Minecraft server is running.
        GT_Values.NW = (IGT_NetworkHandler) Proxy.newProxyInstance(IGT_NetworkHandler.class.getClassLoader(),
                new Class<?>[]{IGT_NetworkHandler.class}, (proxy, method, args) -> {
                    if (method.getReturnType() == void.class) return null;
                    throw new AssertionError("Unexpected network query: " + method);
                });
    }

    private static void setStatic(Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    public static void setField(Object instance, String name, Object value) throws Exception {
        for (Class<?> owner = instance.getClass(); owner != null; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // State can be declared by the machine's base class.
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static Fluid fluid(String name) {
        if (!FluidRegistry.isFluidRegistered(name)) FluidRegistry.registerFluid(new Fluid(name));
        return FluidRegistry.getFluid(name);
    }

    public static final class ProcessingWorld extends GT_StructureIntegrationTest.TestWorld {
        private final Map<String, Chunk> chunks = new HashMap<>();

        @Override
        public Chunk getChunkFromChunkCoords(int x, int z) {
            return chunks.computeIfAbsent(x + ":" + z, ignored -> new Chunk(this, x, z));
        }
    }

    public static final class Fixture {
        public final ProcessingWorld world = new ProcessingWorld();
        public final GT_MetaTileEntity_MultiBlockBase machine;
        public final GT_StructureIntegrationTest.TestTile tile;

        public Fixture(GT_MetaTileEntity_MultiBlockBase machine, int tier) {
            this.machine = machine;
            tile = attach(machine);
            machine.mWrench = machine.mScrewdriver = machine.mSoftHammer = machine.mHardHammer =
                    machine.mSolderingTool = machine.mCrowbar = true;
            GT_MetaTileEntity_Hatch_Energy energy = new GT_MetaTileEntity_Hatch_Energy("test", tier, "test", null);
            attach(energy);
            energy.setEUVar(1000000);
            machine.mEnergyHatches.add(energy);
            world.rand.setSeed(0);
        }

        public GT_StructureIntegrationTest.TestTile attach(MetaTileEntity entity) {
            return new GT_StructureIntegrationTest.TestTile(world, entity, ForgeDirection.NORTH);
        }

        public GT_MetaTileEntity_Hatch_InputBus items(ItemStack... stacks) {
            GT_MetaTileEntity_Hatch_InputBus bus = new GT_MetaTileEntity_Hatch_InputBus("test", 2, "test", null);
            attach(bus);
            bus.disableSort = true;
            System.arraycopy(stacks, 0, bus.mInventory, 0, stacks.length);
            machine.mInputBusses.add(bus);
            return bus;
        }

        public GT_MetaTileEntity_Hatch_Input liquid(FluidStack stack) {
            GT_MetaTileEntity_Hatch_Input hatch = new GT_MetaTileEntity_Hatch_Input("test", 2, "test", null);
            attach(hatch);
            hatch.mFluid = stack;
            machine.mInputHatches.add(hatch);
            return hatch;
        }
    }
}
