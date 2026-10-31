package gregtech.api.metatileentity.implementations;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Config;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import gregtech.common.blocks.GT_Item_Casings2;
import gregtech.common.blocks.GT_Item_Machines;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.stats.StatBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;
import space.impact.structures.CommonProxy;
import space.impact.structures.Main;
import space.impact.structures.alignment.IAlignmentLimits;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.alignment.enumerable.ExtendedFacing;
import space.impact.structures.alignment.enumerable.Flip;
import space.impact.structures.alignment.enumerable.Rotation;
import space.impact.structures.hint.HintMarkerKind;
import space.impact.structures.hint.HintColor;
import space.impact.structures.net.AlignmentMessage.AlignmentData;
import space.impact.structures.net.AlignmentMessage.ClientHandler;
import space.impact.structures.structure.IItemSource;
import space.impact.structures.structure.AutoPlaceEnvironment;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/** Exercises the real structure walkers and placement helpers without starting a Minecraft client. */
public class GT_StructureIntegrationTest {
    @Test
    public void tooltipApiLoadsWithoutClientClasses() throws Exception {
        try (URLClassLoader server = new URLClassLoader(((URLClassLoader) getClass().getClassLoader()).getURLs(), null) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("org.lwjgl.")) {
                    throw new ClassNotFoundException("Client dependency on server: " + name);
                }
                return super.loadClass(name, resolve);
            }
        }) {
            for (String name : new String[]{"gregtech.api.interfaces.ICasing",
                    "gregtech.api.interfaces.ISecondaryDescribable", "gregtech.api.util.StructureRequirements",
                    "gregtech.api.metatileentity.implementations.GT_MetaTileEntity_TooltipMultiBlockBase",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_Steam",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_HPSteam",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_Gas",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine_Plasma",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_IndustrialPulverizer",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ImplosionCompressor",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiFurnace",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ElectricBlastFurnace",
                    "gregtech.api.enums.HeatingCoil",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_HeatExchanger",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeChemicalReactor",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiblockCentrifuge",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_MultiblockElectrolyzer",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DistillationTower",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_OilCracker",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DieselEngine",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DieselEngine2",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AssemblyLine",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Cleanroom",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Charcoal_Pit",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BrickedBlastFurnace",
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_BronzeBlastFurnace"}) {
                Class.forName(name, true, server);
            }
            for (String family : new String[]{"OilDrill", "ConcreteBackfiller"}) {
                int variants = family.equals("OilDrill") ? 3 : 2;
                for (int variant = 1; variant <= variants; variant++) {
                    Class.forName("gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_" + family + variant,
                            true, server);
                }
            }
            // Its existing static recipe requires registered adsorption filter items before initialization.
            Class<?> airFilter = Class.forName(
                    "gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AirFilter", false, server);
            assertEquals("gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase",
                    airFilter.getSuperclass().getName());
            Class<?> builder = Class.forName("gregtech.api.util.MultiblockTooltipBuilder", true, server);
            Object tooltip = builder.getConstructor().newInstance();
            builder.getMethod("addInfo", String.class).invoke(tooltip, "Server-safe tooltip");
            assertEquals("Server-safe tooltip", ((String[]) builder.getMethod("getInformation").invoke(tooltip))[0]);
        }
    }

    @Test
    public void structuresAndConstruction() throws Exception {
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

    private static TestMachineItem hatchItem;
    private static HintProxy hintProxy;

    public static void runChecks() throws Exception {
        Field side = FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, Side.SERVER);
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, new File("."));
        Bootstrap.func_151354_b();
        File config = File.createTempFile("structure-test", ".cfg");
        config.deleteOnExit();
        GregTech_API.sMachineFile = new GT_Config(new Configuration(config));
        GregTech_API.sBlockCasings2 = Blocks.wool;
        hintProxy = new HintProxy();
        Field proxy = Main.class.getDeclaredField("proxy");
        proxy.setAccessible(true);
        proxy.set(null, hintProxy);
        Block machineBlock = new Block(Material.iron) {};
        Block.blockRegistry.addObject(3000, "structure_test:machine", machineBlock);
        hatchItem = new TestMachineItem(machineBlock);
        Item.itemRegistry.addObject(3000, "structure_test:machine", hatchItem);
        for (int i = 0; i < 7; i++) GregTech_API.METATILEENTITIES[32000 + i] = hatch(i);
        checkAlignment();
        checkAlignmentPacket();
        checkShapes();
        checkConstruction();
        checkConstructionConflicts();
        checkCasings();
        checkTooltips();
        checkSharedRequirements();
        GT_FusionStructureChecks.run(hatchItem, hintProxy);
        GT_BoilerStructureChecks.run(hatchItem, hintProxy);
        GT_TurbineStructureChecks.run(hatchItem, hintProxy);
        GT_PulverizerStructureChecks.run(hatchItem, hintProxy);
        GT_ImplosionStructureChecks.run(hatchItem, hintProxy);
        GT_MultiFurnaceStructureChecks.run(hatchItem, hintProxy);
        GT_BlastFurnaceStructureChecks.run(hatchItem, hintProxy);
        GT_HeatExchangerStructureChecks.run(hatchItem, hintProxy);
        GT_ChemicalReactorStructureChecks.run(hatchItem, hintProxy);
        GT_CentrifugeStructureChecks.run(hatchItem, hintProxy);
        GT_ElectrolyzerStructureChecks.run(hatchItem, hintProxy);
        GT_DistillationTowerStructureChecks.run(hatchItem, hintProxy);
        GT_OilCrackerStructureChecks.run(hatchItem, hintProxy);
        GT_DieselEngineStructureChecks.run(hatchItem, hintProxy);
        GT_AirFilterStructureChecks.run(hatchItem, hintProxy);
        GT_AssemblyLineStructureChecks.run(hatchItem, hintProxy);
        GT_CleanroomStructureChecks.run(hatchItem, hintProxy);
        GT_CharcoalPitStructureChecks.run(hintProxy);
        GT_DrillerStructureChecks.run(hatchItem, hintProxy);
        GT_PrimitiveBlastStructureChecks.run(hintProxy);
    }

    private static void loadLanguage(String language) throws Exception {
        try (InputStream stream = GT_StructureIntegrationTest.class.getResourceAsStream(
                "/assets/gregtech/lang/" + language + ".lang")) {
            assertTrue("Missing language resource", stream != null);
            StringTranslate.inject(stream);
        }
        GT_MetaTileEntity_TooltipMultiBlockBase.invalidateTooltipCache();
    }

    private static void checkTooltips() throws Exception {
        Block casing = new Block(Material.iron) {}.setBlockName("gt.blockcasings2");
        Block.blockRegistry.addObject(3001, "structure_test:casing", casing);
        Item.itemRegistry.addObject(3001, "structure_test:casing", new GT_Item_Casings2(casing));
        Block previousCasing = GregTech_API.sBlockCasings2;
        GregTech_API.sBlockCasings2 = casing;
        boolean previousPostload = GregTech_API.sPostloadFinished;
        boolean previousEnglishFile = GT_LanguageManager.sUseEnglishFile;
        try {
            loadLanguage("en_US");
            Fixture f = new Fixture(ForgeDirection.NORTH);
            assertSame(f.machine.getTooltip(), f.machine.getTooltip());
            MultiblockTooltipBuilder previousTooltip = f.machine.getTooltip();
            String primary = String.join("\n", f.machine.getCurrentDescription(false));
            String secondary = String.join("\n", f.machine.getCurrentDescription(true));
            assertTrue(primary.contains("Vacuum Freezer"));
            assertTrue(primary.contains("Shift"));
            assertFalse(primary.contains("At least 16"));
            assertTrue(secondary.contains("At least 16"));
            assertTrue(secondary.contains("Frost Proof Machine Casing"));
            assertTrue(secondary.contains("Input Bus or Input Hatch"));
            assertTrue(secondary.contains("3 x 3 x 3"));
            assertTrue(secondary.contains("gt_hatch"));
            assertFalse(secondary.contains("gt.tooltip."));
            assertFalse(secondary.contains("Format error"));
            assertTrue(String.join("\n", f.machine.getStructureDescription(null)).contains("At least 16"));
            assertFalse(String.join("\n", f.machine.getStructureDescription(null)).contains("Hold Shift"));
            String[] copy = f.machine.getPrimaryDescription();
            copy[0] = "mutated";
            assertFalse(f.machine.getPrimaryDescription()[0].equals("mutated"));

            // Exercise the real item tooltip path with conflicting legacy per-index translations.
            GregTech_API.METATILEENTITIES[32010] = f.machine;
            ItemStack controller = new ItemStack(hatchItem, 1, 32010);
            GregTech_API.sPostloadFinished = true;
            GT_LanguageManager.sUseEnglishFile = true;
            GT_LanguageManager.LANGMAP.put("TileEntity_DESCRIPTION_32010_Index_0", "WRONG LEGACY TRANSLATION");
            for (boolean shift : new boolean[]{false, true, false}) {
                hatchItem.secondaryTooltip = shift;
                List<String> lines = new ArrayList<>();
                hatchItem.addInformation(controller, f.player, lines, false);
                String[] expected = f.machine.getCurrentDescription(shift);
                assertEquals(Arrays.asList(expected), lines.subList(0, expected.length));
                assertFalse(lines.contains("WRONG LEGACY TRANSLATION"));
            }

            Freezer legacy = new Freezer() {
                @Override protected MultiblockTooltipBuilder createTooltip() { return null; }
                @Override public String[] getDescription() { return new String[]{"Legacy description"}; }
            };
            new TestTile(f.world, legacy, ForgeDirection.NORTH);
            GregTech_API.METATILEENTITIES[32011] = legacy;
            GT_LanguageManager.LANGMAP.put("TileEntity_DESCRIPTION_32011_Index_0", "LEGACY STILL WORKS");
            List<String> oldLines = new ArrayList<>();
            hatchItem.addInformation(new ItemStack(hatchItem, 1, 32011), f.player, oldLines, false);
            assertTrue(oldLines.contains("LEGACY STILL WORKS"));
            assertFalse(legacy.hasSecondaryDescription());
            assertEquals("Legacy description", legacy.getStructureDescription(null)[0]);

            // Builder text is registered as English and respects GregTech's editable language file.
            GregTech_API.sPostloadFinished = false;
            GT_LanguageManager.sUseEnglishFile = false;
            MultiblockTooltipBuilder automatic = new MultiblockTooltipBuilder("test.automatic")
                    .addInfo("100% original information").addStructureInfo("Original structure information");
            String informationKey = "TileEntity_DESCRIPTION_test.automatic_Information_Index_0";
            String structureKey = "TileEntity_DESCRIPTION_test.automatic_Structure_Index_0";
            assertEquals("100% original information", automatic.getInformation()[0]);
            assertEquals("100% original information", GT_LanguageManager.LANGMAP.get(informationKey));
            assertEquals("Original structure information", GT_LanguageManager.LANGMAP.get(structureKey));
            GT_LanguageManager.LANGMAP.put(informationKey, "Configured information");
            GT_LanguageManager.LANGMAP.put(structureKey, "Configured structure");
            GregTech_API.sPostloadFinished = true;
            GT_LanguageManager.sUseEnglishFile = true;
            MultiblockTooltipBuilder configured = new MultiblockTooltipBuilder("test.automatic")
                    .addInfo("100% original information").addStructureInfo("Original structure information");
            assertEquals("Configured information", configured.getInformation()[0]);
            assertEquals("   Configured structure", configured.getStructureInformation()[0]);

            GT_LanguageManager.sUseEnglishFile = false;
            loadLanguage("ru_RU");
            assertFalse(previousTooltip == f.machine.getTooltip());
            assertTrue(String.join("\n", f.machine.getPrimaryDescription()).contains("Вакуумный морозильник"));
            secondary = String.join("\n", f.machine.getSecondaryDescription());
            assertTrue(secondary.contains("Минимум 16"));
            assertTrue(secondary.contains("Морозостойкий корпус машины"));
            assertTrue(secondary.contains("Входная шина или входной люк"));
            assertFalse(secondary.contains("gt.tooltip."));
            assertFalse(secondary.contains("Format error"));
            assertEquals("Vacuum Freezer", GT_LanguageManager.LANGMAP.get(
                    "TileEntity_DESCRIPTION_multimachine.vacuumfreezer_MachineType_Index_0"));

            MultiblockTooltipBuilder colors = new MultiblockTooltipBuilder()
                    .beginVariableStructureBlock(2, 4, 3, 3, 5, 6, false)
                    .addMaintenanceHatch("bottom", 1)
                    .addEnergyHatch(2, "top", 16)
                    .addStructureHint("Black hatch", HintColor.BLACK).toolTipFinisher();
            assertTrue(String.join("\n", colors.getStructureInformation()).contains("2-4"));
            String[] hints = colors.getStructureHint();
            assertEquals(3, hints.length); // instructions + white + black, not separate duplicate black markers
            assertTrue(String.join("\n", hints).contains("Black hatch"));
            assertEquals(hints.length, colors.toolTipFinisher().getStructureHint().length);
            assertThrows(IllegalArgumentException.class,
                    () -> new MultiblockTooltipBuilder().addStructureHint("bad", 0));
            assertThrows(IllegalStateException.class, () -> colors.addInfo("late mutation"));
        } finally {
            GregTech_API.sBlockCasings2 = previousCasing;
            GregTech_API.METATILEENTITIES[32010] = null;
            GregTech_API.METATILEENTITIES[32011] = null;
            GregTech_API.sPostloadFinished = previousPostload;
            GT_LanguageManager.sUseEnglishFile = previousEnglishFile;
            GT_LanguageManager.LANGMAP.remove("TileEntity_DESCRIPTION_32010_Index_0");
            GT_LanguageManager.LANGMAP.remove("TileEntity_DESCRIPTION_32011_Index_0");
            hatchItem.secondaryTooltip = false;
            loadLanguage("en_US");
        }
    }

    private static void checkSharedRequirements() {
        ICasing addonCasing = new ICasing() {
            @Override public Block getBlock() { return Blocks.wool; }
            @Override public int getMetadata() { return 1; }
            @Override public int getTextureId() { return 17; }
            @Override public String getDisplayName() { return "Addon Casing"; }
        };
        IHatchElement addonInput = new IHatchElement() {
            @Override public boolean matches(IMetaTileEntity tile) { return HatchElement.Input.matches(tile); }
            @Override public int count(GT_MetaTileEntity_MultiBlockBase machine) { return HatchElement.Input.count(machine); }
            @Override public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
                return HatchElement.Input.add(machine, tile, texture);
            }
            @Override public String getDescription() { return "Addon Input"; }
        };
        StructureRequirements.Builder builder = StructureRequirements.builder(addonCasing, 2).atLeast(addonInput);
        StructureRequirements one = builder.build();
        StructureRequirements two = builder.atLeast(addonInput).build();
        assertEquals(Integer.valueOf(1), one.getHatches().get(addonInput));
        assertEquals(Integer.valueOf(2), two.getHatches().get(addonInput));
        assertThrows(UnsupportedOperationException.class, () -> two.getHatches().clear());
        assertThrows(IllegalArgumentException.class, () -> StructureRequirements.builder(addonCasing, -1));
        assertThrows(IllegalArgumentException.class, () -> builder.atLeast(addonInput, 0));
        String tooltip = String.join("\n", new MultiblockTooltipBuilder().addRequirements(two, "shell")
                .getStructureInformation());
        assertTrue(tooltip.contains("Addon Casing"));
        assertTrue(tooltip.contains("Addon Input"));
        assertEquals(2, tooltip.split("At least 2", -1).length - 1);

        Fixture f = new Fixture(ForgeDirection.NORTH);
        f.putHatch(0, 2);
        f.machine.recheck();
        assertTrue(one.isSatisfied(f.machine, 2));
        assertFalse(two.isSatisfied(f.machine, 2));
        assertFalse(one.isSatisfied(f.machine, 1));
        Fixture independent = new Fixture(ForgeDirection.NORTH);
        assertFalse(one.isSatisfied(independent.machine, 2));
        Source items = new Source(new ItemStack(hatchItem, 2, 32002), new ItemStack(Blocks.wool, 3, 1));
        AutoPlaceEnvironment environment = AutoPlaceEnvironment.fromLegacy(items, f.player, f.player::addChatMessage);
        ItemStack trigger = new ItemStack(Items.stick);
        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        int[] casingChecks = {0};
        IStructureElement<Freezer> element = GTStructureUtility.buildHatchAdder(Freezer.class)
                .buildAndChain(two, machine -> casingChecks[0]++);
        assertEquals(IStructureElement.PlaceResult.ACCEPT_STOP,
                element.survivalPlaceBlock(f.machine, f.world, 10, 64, 0, trigger, environment));
        assertEquals(1, items.consumed);
        assertEquals(0, casingChecks[0]);
        assertTrue(element.check(f.machine, f.world, 10, 64, 0));
        assertTrue(two.isSatisfied(f.machine, 2));
        assertEquals(IStructureElement.PlaceResult.SKIP,
                element.survivalPlaceBlock(f.machine, f.world, 10, 64, 0, trigger, environment));
        assertEquals(IStructureElement.PlaceResult.ACCEPT,
                element.survivalPlaceBlock(f.machine, f.world, 11, 64, 0, trigger, environment));
        assertEquals(2, items.consumed);
        assertEquals(1, items.stacks.get(0).stackSize); // no third input hatch
        assertEquals(0, casingChecks[0]);
        assertTrue(element.check(f.machine, f.world, 11, 64, 0));
        assertEquals(1, casingChecks[0]);
        assertFalse(two.isSatisfied(independent.machine, 2));
        StructureRequirements casingsOnly = StructureRequirements.builder(addonCasing, 0).build();
        assertTrue(casingsOnly.isSatisfied(independent.machine, 0));
        assertTrue(GTStructureUtility.buildHatchAdder(Freezer.class).buildAndChain(casingsOnly)
                .check(f.machine, f.world, 11, 64, 0));
    }

    private static void checkCasings() {
        Set<Integer> textureIds = new HashSet<>();
        for (Casings casing : Casings.values()) {
            assertTrue(casing.name(), casing.getMetadata() >= 0 && casing.getMetadata() < 16);
            assertTrue("Duplicate texture ID: " + casing, textureIds.add(casing.getTextureId()));
        }
        assertEquals(17, Casings.FROST_PROOF.getTextureId());
        assertEquals(176, Casings.CHEMICALLY_INERT.getTextureId());
        assertEquals(194, Casings.ROBUST_NEUTRONIUM.getTextureId());
        assertEquals(239, Casings.HERMETIC_TIER_15.getTextureId());
        assertEquals(271, Casings.SOLID_FRAME.getTextureId());
        assertSame(Blocks.wool, Casings.FROST_PROOF.getBlock());
        // Loading the enum must not capture the block registration at that moment.
        GregTech_API.sBlockCasings2 = Blocks.stone;
        try {
            assertSame(Blocks.stone, Casings.FROST_PROOF.getBlock());
        } finally {
            GregTech_API.sBlockCasings2 = Blocks.wool;
        }

        ICasing addonCasing = new ICasing() {
            @Override public Block getBlock() { return Blocks.wool; }
            @Override public int getMetadata() { return 5; }
            @Override public int getTextureId() { return 2 * 128 + 5; }
        };
        ITexture texture = TextureFactory.of(Blocks.wool, 5);
        ITexture previous = Textures.BlockIcons.getCasingTextureForId(addonCasing.getTextureId());
        Textures.BlockIcons.setCasingTextureForId(addonCasing.getTextureId(), texture);
        try {
            assertSame(texture, addonCasing.getTexture());
            Fixture f = new Fixture(ForgeDirection.NORTH);
            int[] checks = {0};
            IStructureElement<Freezer> element = GTStructureUtility.buildHatchAdder(Freezer.class)
                    .atLeast(HatchElement.Input).buildAndChain(addonCasing, machine -> checks[0]++);
            ItemStack trigger = new ItemStack(Items.stick);
            assertFalse(element.check(f.machine, f.world, 10, 64, 0));
            assertTrue(element.placeBlock(f.machine, f.world, 10, 64, 0, trigger));
            assertSame(Blocks.wool, f.world.getBlock(10, 64, 0));
            assertEquals(5, f.world.getBlockMetadata(10, 64, 0));
            assertEquals(0, checks[0]);
            assertTrue(element.couldBeValid(f.machine, f.world, 10, 64, 0, trigger));
            assertEquals(0, checks[0]);
            assertTrue(element.check(f.machine, f.world, 10, 64, 0));
            assertEquals(1, checks[0]);
            f.world.setBlock(10, 64, 0, Blocks.wool, 1, 0);
            assertFalse(addonCasing.asElement().check(f.machine, f.world, 10, 64, 0));

            f.world.isRemote = true;
            int changes = f.world.changes;
            assertTrue(element.spawnHint(f.machine, f.world, 10, 64, 0, trigger));
            assertSame(addonCasing.getBlock(), hintProxy.lastBlock);
            assertEquals(addonCasing.getMetadata(), hintProxy.lastMetadata);
            assertEquals(changes, f.world.changes);
            assertEquals(1, checks[0]);
            assertEquals(0, HatchElement.Input.count(f.machine));
            f.world.isRemote = false;

            GT_MetaTileEntity_Hatch input = (GT_MetaTileEntity_Hatch) hatch(2);
            f.world.putTile(10, 64, 0, new TestTile(f.world, input, ForgeDirection.NORTH));
            assertTrue(element.check(f.machine, f.world, 10, 64, 0));
            assertEquals(1, HatchElement.Input.count(f.machine));
            assertEquals(1, checks[0]);
            assertEquals(2, input.getTexturePage());
            assertEquals(5, input.getUpdateData());
            // The simpler overload accepts the interface too, without an explicit texture index.
            assertTrue(GTStructureUtility.buildHatchAdder(Freezer.class).atLeast(HatchElement.Input)
                    .buildAndChain(addonCasing).couldBeValid(f.machine, f.world, 10, 64, 0, trigger));
        } finally {
            Textures.BlockIcons.setCasingTextureForId(addonCasing.getTextureId(), previous);
        }
    }

    private static void checkAlignmentPacket() throws Exception {
        Fixture server = new Fixture(ForgeDirection.EAST);
        Fixture client = new Fixture(ForgeDirection.NORTH);
        client.world.isRemote = true;
        hintProxy.player = client.player;
        Field instance = Main.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, new Main());
        AlignmentData outgoing =
                new AlignmentData((TestTile) server.machine.getBaseMetaTileEntity());
        ByteBuf buffer = Unpooled.buffer();
        try {
            outgoing.toBytes(buffer);
            AlignmentData incoming =
                    new AlignmentData();
            incoming.fromBytes(buffer);
            new ClientHandler().onMessage(incoming, null);
            assertEquals(server.machine.getExtendedFacing(), client.machine.getExtendedFacing());
            assertEquals(ForgeDirection.EAST.ordinal(), client.machine.getBaseMetaTileEntity().getFrontFacing());
        } finally {
            buffer.release();
            hintProxy.player = null;
        }
    }

    private static void checkAlignment() {
        Freezer machine = new Freezer();
        TestTile base = new TestTile(null, machine, ForgeDirection.NORTH);
        for (byte side = 0; side < 6; side++) assertEquals(side > 1, machine.isFacingValid(side));
        for (ForgeDirection direction : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            base.setFrontFacing((byte) direction.ordinal());
            assertEquals(direction, machine.getExtendedFacing().getDirection());
            machine.setExtendedFacing(ExtendedFacing.of(direction, Rotation.CLOCKWISE, Flip.HORIZONTAL));
            assertEquals(ExtendedFacing.of(direction), machine.getExtendedFacing());
            assertFalse(machine.toolSetRotation(Rotation.CLOCKWISE));
            assertFalse(machine.toolSetFlip(Flip.HORIZONTAL));
        }
        NBTTagCompound oldSave = new NBTTagCompound();
        machine.loadNBTData(oldSave);
        assertEquals(ExtendedFacing.of(ForgeDirection.EAST), machine.getExtendedFacing());
        oldSave.setByte("eRotation", (byte) 1);
        oldSave.setByte("eFlip", (byte) 1);
        machine.loadNBTData(oldSave);
        assertEquals(ExtendedFacing.of(ForgeDirection.EAST), machine.getExtendedFacing());

        Freezer enabled = new Freezer() {
            @Override
            protected IAlignmentLimits getInitialAlignmentLimits() {
                return (direction, rotation, flip) -> direction != ForgeDirection.UNKNOWN && !flip.isVerticallyFliped();
            }
        };
        TestTile enabledBase = new TestTile(null, enabled, ForgeDirection.NORTH);
        ExtendedFacing rotated = ExtendedFacing.of(ForgeDirection.UP, Rotation.CLOCKWISE, Flip.HORIZONTAL);
        enabled.setExtendedFacing(rotated);
        assertEquals(rotated, enabled.getExtendedFacing());
        assertEquals(ForgeDirection.UP.ordinal(), enabledBase.getFrontFacing());
        NBTTagCompound saved = new NBTTagCompound();
        enabled.saveNBTData(saved);
        enabled.loadNBTData(saved);
        assertEquals(rotated, enabled.getExtendedFacing());
        assertFalse(enabled.mMachine);
        assertEquals(1, enabled.mUpdate);
        saved.setByte("eRotation", (byte) -20);
        saved.setByte("eFlip", (byte) 127);
        enabled.loadNBTData(saved);
        assertEquals(ExtendedFacing.of(ForgeDirection.UP), enabled.getExtendedFacing());
    }

    private static void checkShapes() {
        for (ForgeDirection facing : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            Fixture f = new Fixture(facing);
            f.fillShell();
            for (int i = 0; i < 4; i++) f.putHatch(i, i);
            assertTrue("Valid shell facing " + facing, f.machine.recheck());
            assertEquals(1, HatchElement.Input.count(f.machine));
            assertTrue(f.machine.recheck());
            assertEquals("No duplicated hatch entries", 1, HatchElement.Input.count(f.machine));
            for (int i = 0; i < 4; i++) {
                f.putCasing(i);
                assertFalse("Missing hatch " + i, f.machine.recheck());
                f.putHatch(i, i);
            }
            for (int i = 4; i < 9; i++) f.putHatch(i, 1);
            assertTrue("16 casings", f.machine.recheck());
            f.putHatch(9, 1);
            assertFalse("15 casings", f.machine.recheck());
            f.putCasing(9);
            int[] center = f.position(0, 0, 1);
            f.world.setBlock(center[0], center[1], center[2], Blocks.stone, 0, 0);
            assertFalse("Filled center", f.machine.recheck());
            f.machine.construct(new ItemStack(Items.feather), false);
            assertEquals("Creative must not clear occupied center", Blocks.stone,
                    f.world.getBlock(center[0], center[1], center[2]));
            f.world.setBlockToAir(center[0], center[1], center[2]);
            int[] wrong = f.shell.get(10);
            f.world.setBlock(wrong[0], wrong[1], wrong[2], Blocks.wool, 2, 0);
            assertFalse("Wrong casing metadata", f.machine.recheck());
            f.putCasing(10);
            for (int input : new int[]{4, 5}) {
                f.putHatch(2, input);
                f.putHatch(1, 6);
                assertTrue("IMPACT hatch variants", f.machine.recheck());
            }
            f.world.setBlockToAir(f.shell.get(0)[0], f.shell.get(0)[1], f.shell.get(0)[2]);
            assertFalse(f.machine.recheck());
            assertEquals("Scan past missing early blocks", 1, HatchElement.Input.count(f.machine));
        }
    }

    private static void checkConstruction() {
        ItemStack trigger = new ItemStack(Items.feather);
        Fixture creative = new Fixture(ForgeDirection.NORTH);
        creative.putHatch(4, 2);
        creative.machine.construct(trigger, false);
        assertEquals(26, creative.world.blocks.size());
        assertEquals(1, HatchElement.Input.count(creative.machine));
        assertFalse(creative.machine.mMachine);
        creative.world.isRemote = true;
        int changes = creative.world.changes;
        int inputs = HatchElement.Input.count(creative.machine);
        ChannelDataAccessor.setChannelData(trigger, "show_error", 1);
        creative.machine.construct(trigger, true);
        assertEquals(26, hintProxy.hints);
        assertEquals("Shell hints use the casing block and metadata", 25, hintProxy.casingHints);
        assertEquals(changes, creative.world.changes);
        assertEquals(inputs, HatchElement.Input.count(creative.machine));
        creative.machine.construct(trigger, false);
        assertEquals("Client cannot build", changes, creative.world.changes);

        Fixture noChannel = new Fixture(ForgeDirection.NORTH);
        Source casingOnly = new Source(new ItemStack(Blocks.wool, 30, 1));
        assertEquals(0, noChannel.build(trigger, 0, casingOnly));
        assertEquals(0, casingOnly.consumed);
        assertEquals(3, noChannel.build(trigger, 3, casingOnly));
        assertEquals(3, casingOnly.consumed);
        assertEquals(0, HatchElement.Maintenance.count(noChannel.machine));
        noChannel.build(trigger, 50, casingOnly);
        assertEquals(0, noChannel.build(trigger, 50, casingOnly));
        assertFalse("Complete shell without required hatches is not a complete machine", noChannel.machine.mMachine);

        ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
        Fixture f = new Fixture(ForgeDirection.SOUTH);
        // Hatches late in the scan must be found before the first placement.
        f.putHatch(24, 2);
        Source source = new Source(new ItemStack(hatchItem, 5, 32000), new ItemStack(hatchItem, 5, 32001),
                new ItemStack(hatchItem, 5, 32002), new ItemStack(hatchItem, 5, 32003),
                new ItemStack(Blocks.wool, 40, 1));
        for (int i = 0; i < 3; i++) {
            assertEquals("Stop after each hatch", 1, f.build(trigger, 20, source));
            assertEquals(i + 1, source.consumed);
        }
        assertEquals(1, HatchElement.Input.count(f.machine));
        assertEquals(5, source.stacks.get(2).stackSize);
        int rounds = 0;
        while (!f.machine.mMachine && rounds++ < 20) {
            int count = f.build(trigger, 3, source);
            assertTrue(count > 0 && count <= 3);
        }
        assertTrue(f.machine.mMachine);
        assertEquals(24, source.consumed);
        assertEquals(-1, f.build(trigger, 3, source));
        assertEquals(24, source.consumed);
        for (HatchElement category : HatchElement.values()) assertEquals(1, category.count(f.machine));
        for (TestTile tile : f.world.tiles.values()) {
            if (tile.getMetaTileEntity() instanceof Freezer) continue;
            ForgeDirection direction = ForgeDirection.getOrientation(tile.getFrontFacing());
            assertEquals("Hatch faces outside shell", Blocks.air,
                    f.world.getBlock(tile.xCoord + direction.offsetX, tile.yCoord + direction.offsetY,
                            tile.zCoord + direction.offsetZ));
        }

        Fixture missing = new Fixture(ForgeDirection.NORTH);
        Source fallback = new Source(new ItemStack(Blocks.wool, 30, 1));
        int errors = hintProxy.errors;
        assertEquals(5, missing.build(trigger, 5, fallback));
        assertEquals(5, fallback.consumed);
        assertEquals(5, missing.build(trigger, 5, fallback));
        assertEquals(10, fallback.consumed);
        assertEquals(15, missing.build(trigger, 50, fallback));
        assertEquals(25, fallback.consumed);
        assertTrue("Missing hatches do not interrupt shell construction", missing.player.messages.isEmpty());
        assertEquals("Fallback casings are not marked as errors", errors, hintProxy.errors);
        assertFalse(missing.machine.mMachine);
        assertEquals(0, missing.build(trigger, 50, fallback));
        assertEquals(25, fallback.consumed);
        // Only a complete shell with unmet requirements reports that the machine still needs hatches.
        assertFalse(missing.player.messages.isEmpty());
        assertEquals(errors, hintProxy.errors);

        Fixture partial = new Fixture(ForgeDirection.NORTH);
        Source partialItems = new Source(new ItemStack(hatchItem, 3, 32000), new ItemStack(Blocks.wool, 30, 1));
        assertEquals("Available hatch takes priority", 1, partial.build(trigger, 5, partialItems));
        assertEquals(1, HatchElement.Maintenance.count(partial.machine));
        assertEquals("Unavailable remaining categories allow casing placement", 5,
                partial.build(trigger, 5, partialItems));
        assertEquals(2, partialItems.stacks.get(0).stackSize);
        assertEquals(6, partialItems.consumed);
        assertEquals(19, partial.build(trigger, 50, partialItems));
        assertFalse(partial.machine.mMachine);
        assertEquals(errors, hintProxy.errors);

        Fixture noResources = new Fixture(ForgeDirection.NORTH);
        Source none = new Source();
        assertEquals(0, noResources.build(trigger, 5, none));
        assertEquals(0, none.consumed);
        assertFalse(noResources.player.messages.isEmpty());
        assertEquals("Real missing casing still produces an error", errors + 1, hintProxy.errors);

        IStructureElement<Freezer> preview = GTStructureUtility.buildHatchAdder(Freezer.class)
                .atLeast(HatchElement.Input).buildAndChain(Casings.FROST_PROOF);
        AutoPlaceEnvironment environment = AutoPlaceEnvironment.fromLegacy(fallback, missing.player,
                missing.player::addChatMessage);
        assertTrue(preview.getBlocksToPlace(missing.machine, missing.world, 10, 64, 0, trigger, environment)
                .getPredicate().test(new ItemStack(Blocks.wool, 1, 1)));
        assertEquals("Preview must not consume resources", 25, fallback.consumed);
        Source withHatch = new Source(new ItemStack(hatchItem, 1, 32002), new ItemStack(Blocks.wool, 1, 1));
        AutoPlaceEnvironment hatchEnvironment = AutoPlaceEnvironment.fromLegacy(withHatch, missing.player,
                missing.player::addChatMessage);
        IStructureElement.BlocksToPlace choices = preview.getBlocksToPlace(missing.machine, missing.world,
                10, 64, 0, trigger, hatchEnvironment);
        assertTrue(choices.getPredicate().test(new ItemStack(hatchItem, 1, 32002)));
        assertFalse(choices.getPredicate().test(new ItemStack(Blocks.wool, 1, 1)));
        assertEquals(0, withHatch.consumed);
        int[] occupied = missing.shell.get(0);
        missing.world.setBlock(occupied[0], occupied[1], occupied[2], Blocks.stone, 0, 0);
        assertEquals(0, missing.build(trigger, 5, source));
        assertEquals(Blocks.stone, missing.world.getBlock(occupied[0], occupied[1], occupied[2]));
    }

    private static void checkConstructionConflicts() {
        for (ForgeDirection direction : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.EAST}) {
            for (boolean hatchChannel : new boolean[]{false, true}) {
                Fixture f = new Fixture(direction);
                ItemStack trigger = new ItemStack(Items.stick);
                if (hatchChannel) ChannelDataAccessor.setChannelData(trigger, GTStructureUtility.HATCH_CHANNEL, 1);
                // Early empty positions would consume the whole budget before reaching these conflicts.
                int[] wrongMeta = f.shell.get(20);
                int[] obstacle = f.shell.get(24);
                int[] center = f.position(0, 0, 1);
                f.putCasing(5);
                f.putHatch(18, 4);
                f.world.setBlock(wrongMeta[0], wrongMeta[1], wrongMeta[2], Blocks.wool, 2, 0);
                f.world.setBlock(obstacle[0], obstacle[1], obstacle[2], Blocks.stone, 0, 0);
                f.world.setBlock(center[0], center[1], center[2], Blocks.stone, 0, 0);
                f.machine.recheck();
                int checks = f.machine.machineChecks;
                int changes = f.world.changes;
                Source items = new Source(new ItemStack(Blocks.wool, 30, 1), new ItemStack(hatchItem, 1, 32000));
                Set<String> conflicts = new HashSet<>();
                for (int[] p : new int[][]{wrongMeta, obstacle, center}) conflicts.add(TestWorld.key(p[0], p[1], p[2]));

                hintProxy.errorPositions.clear();
                assertEquals(0, f.build(trigger, 1, items));
                assertEquals("All conflicts are marked, even beyond the placement budget", conflicts, hintProxy.errorPositions);
                assertEquals("No placement before the complete conflict scan", changes, f.world.changes);
                assertEquals(0, items.consumed);
                assertEquals("Conflict detection does not query the inventory", 0, items.queries);
                assertEquals("Preflight must not run machine validation", checks, f.machine.machineChecks);
                assertEquals(1, HatchElement.Input.count(f.machine));
                assertEquals(1, f.player.messages.size());
                ChatComponentTranslation message = (ChatComponentTranslation) f.player.messages.get(0);
                assertEquals("gt.structure.obstructed", message.getKey());
                assertEquals(3, message.getFormatArgs()[0]);

                // An active client hologram marks the same conflicts without marking missing casings as errors.
                hintProxy.errorPositions.clear();
                hintProxy.player = f.player;
                f.world.isRemote = true;
                try {
                    f.machine.construct(trigger, true);
                    assertEquals(conflicts, hintProxy.errorPositions);
                    assertEquals(changes, f.world.changes);
                    assertEquals(checks, f.machine.machineChecks);
                    assertEquals(1, HatchElement.Input.count(f.machine));
                } finally {
                    f.world.isRemote = false;
                    hintProxy.player = null;
                }

                for (int[] p : new int[][]{wrongMeta, obstacle, center}) f.world.setBlockToAir(p[0], p[1], p[2]);
                hintProxy.errorPositions.clear();
                assertEquals("Removing conflicts allows the next build", 1, f.build(trigger, 1, items));
                assertEquals(1, items.consumed);
                assertTrue(hintProxy.errorPositions.isEmpty());
            }
        }

        Fixture creative = new Fixture(ForgeDirection.NORTH);
        int[] obstruction = creative.shell.get(24);
        creative.world.setBlock(obstruction[0], obstruction[1], obstruction[2], Blocks.stone, 0, 0);
        hintProxy.errorPositions.clear();
        creative.machine.construct(new ItemStack(Items.stick), false);
        assertEquals("Creative continues building around obstacles", 26, creative.world.blocks.size());
        assertSame(Blocks.stone, creative.world.getBlock(obstruction[0], obstruction[1], obstruction[2]));
        assertTrue(hintProxy.errorPositions.isEmpty());

        Fixture replaceable = new Fixture(ForgeDirection.NORTH);
        int[] first = replaceable.shell.get(0);
        replaceable.world.setBlock(first[0], first[1], first[2], Blocks.tallgrass, 1, 0);
        Source casings = new Source(new ItemStack(Blocks.wool, 2, 1));
        assertEquals("Replaceable vegetation does not block construction", 1,
                replaceable.build(new ItemStack(Items.stick), 1, casings));
        assertSame(Blocks.wool, replaceable.world.getBlock(first[0], first[1], first[2]));
        assertEquals(1, casings.consumed);

        Fixture unloaded = new Fixture(ForgeDirection.NORTH);
        int[] last = unloaded.shell.get(24);
        String unavailable = TestWorld.key(last[0], last[1], last[2]);
        unloaded.world.unloadedPositions.add(unavailable);
        int changes = unloaded.world.changes;
        hintProxy.errorPositions.clear();
        Source items = new Source(new ItemStack(Blocks.wool, 30, 1));
        assertEquals(0, unloaded.build(new ItemStack(Items.stick), 1, items));
        assertEquals(changes, unloaded.world.changes);
        assertEquals(0, items.queries);
        assertEquals(new HashSet<>(Arrays.asList(unavailable)), hintProxy.errorPositions);

        Fixture partial = new Fixture(ForgeDirection.NORTH);
        partial.fillShell();
        for (int i = 0; i < 4; i++) {
            partial.putHatch(20 + i, i);
        }
        assertTrue(partial.machine.recheck());
        int[] early = partial.shell.get(0);
        partial.world.unloadedPositions.add(TestWorld.key(early[0], early[1], early[2]));
        assertFalse("An unloaded required block invalidates an otherwise complete machine", partial.machine.recheck());
        assertEquals("Continue scanning later hatches after an unloaded block", 1, partial.machine.mMaintenanceHatches.size());
        assertEquals(1, partial.machine.mEnergyHatches.size());
        assertEquals(1, partial.machine.mInputBusses.size());
        assertEquals(1, partial.machine.mOutputBusses.size());
        partial.world.unloadedPositions.clear();
        assertTrue(partial.machine.recheck());
    }

    public static class Freezer extends GT_MetaTileEntity_VacuumFreezer {
        int machineChecks;
        public Freezer() { super("multimachine.vacuumfreezer"); }
        @Override public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
            machineChecks++;
            return super.checkMachine(base, stack);
        }
        public boolean recheck() { setupMachine(getBaseMetaTileEntity(), null); return mMachine; }
    }

    private static MetaTileEntity hatch(int kind) {
        switch (kind) {
            case 0: return new GT_MetaTileEntity_Hatch_Maintenance("test", 1, "test", null, false);
            case 1: return new GT_MetaTileEntity_Hatch_Energy("test", 1, "test", null);
            case 2: return new GT_MetaTileEntity_Hatch_InputBus("test", 1, "test", null);
            case 3: return new GT_MetaTileEntity_Hatch_OutputBus("test", 1, "test", null);
            case 4: return new GTMTE_Multi_Hatch_Input("test", 4, 1, new String[]{"test"}, null, 1000);
            case 5: return new GTMTE_BusHatch_Input("test", 1, new String[]{"test"}, null, 1000);
            case 6: return new GT_MetaTileEntity_Hatch_EnergyMulti("test", 1, 4, "test", null);
            default: throw new AssertionError(kind);
        }
    }

    private static class Fixture {
        final TestWorld world = new TestWorld();
        final Freezer machine = new Freezer();
        final TestPlayer player = new TestPlayer(world);
        final List<int[]> shell = new ArrayList<>();
        final ExtendedFacing facing;

        Fixture(ForgeDirection direction) {
            facing = ExtendedFacing.of(direction);
            TestTile controller = new TestTile(world, machine, direction);
            controller.yCoord = 64;
            world.putTile(0, 64, 0, controller);
            // Match the walk order: depth, height (top down), width.
            for (int c = 0; c < 3; c++) for (int b = -1; b <= 1; b++) for (int a = -1; a <= 1; a++) {
                if (a == 0 && b == 0 && (c == 0 || c == 1)) continue;
                shell.add(position(a, b, c));
            }
        }

        int[] position(int a, int b, int c) {
            int[] result = new int[3];
            facing.getWorldOffset(new int[]{a, b, c}, result);
            result[1] += 64;
            return result;
        }

        void fillShell() { for (int i = 0; i < shell.size(); i++) putCasing(i); }
        void putCasing(int i) {
            int[] p = shell.get(i);
            world.setBlock(p[0], p[1], p[2], Blocks.wool, 1, 0);
        }
        void putHatch(int i, int kind) {
            int[] p = shell.get(i);
            TestTile tile = new TestTile(world, hatch(kind), ForgeDirection.NORTH);
            world.putTile(p[0], p[1], p[2], tile);
        }
        int build(ItemStack trigger, int budget, Source source) {
            return machine.survivalConstruct(trigger, budget, ISurvivalBuildEnvironment.create(source, player));
        }
    }

    public static class TestTile extends BaseMetaTileEntity {
        private byte facing;
        TestTile(World world, MetaTileEntity machine, ForgeDirection direction) {
            worldObj = world;
            facing = (byte) direction.ordinal();
            machine.setBaseMetaTileEntity(this);
        }
        @Override public byte getFrontFacing() { return facing; }
        @Override
        public byte getBackFacing() {
            return (byte) ForgeDirection.getOrientation(facing).getOpposite().ordinal();
        }
        @Override public void setFrontFacing(byte side) {
            if (isValidFacing(side)) {
                facing = side;
                ((MetaTileEntity) getMetaTileEntity()).onFacingChange();
            }
        }
        @Override public void issueTextureUpdate() {}
        @Override public void markDirty() {}
    }

    public static class TestWorld extends World {
        final Set<String> unloadedPositions = new HashSet<>();
        final Map<String, Block> blocks = new HashMap<>();
        final Map<String, Integer> metadata = new HashMap<>();
        final Map<String, TestTile> tiles = new HashMap<>();
        int changes;
        TestWorld() {
            super(new SaveHandlerMP(), "structures", new WorldSettings(0, WorldSettings.GameType.SURVIVAL,
                    false, false, WorldType.DEFAULT), new WorldProviderSurface(), new Profiler());
        }
        static String key(int x, int y, int z) { return x + ":" + y + ":" + z; }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public boolean blockExists(int x, int y, int z) { return !unloadedPositions.contains(key(x, y, z)); }
        @Override public Block getBlock(int x, int y, int z) { return blocks.getOrDefault(key(x, y, z), Blocks.air); }
        @Override public int getBlockMetadata(int x, int y, int z) { return metadata.getOrDefault(key(x, y, z), 0); }
        @Override public TileEntity getTileEntity(int x, int y, int z) { return tiles.get(key(x, y, z)); }
        @Override public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
            changes++;
            String key = key(x, y, z);
            tiles.remove(key);
            if (block == Blocks.air) { blocks.remove(key); metadata.remove(key); }
            else { blocks.put(key, block); metadata.put(key, meta); }
            return true;
        }
        void putTile(int x, int y, int z, TestTile tile) {
            setBlock(x, y, z, Blocks.iron_block, 0, 0);
            tile.xCoord = x; tile.yCoord = y; tile.zCoord = z;
            tiles.put(key(x, y, z), tile);
        }
        @Override public boolean canPlaceEntityOnSide(Block block, int x, int y, int z, boolean replace,
                                                     int side, Entity entity, ItemStack stack) {
            return isAirBlock(x, y, z) || getBlock(x, y, z).isReplaceable(this, x, y, z);
        }
        @Override public void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch) {}
    }

    static class TestPlayer extends EntityPlayer {
        final List<IChatComponent> messages = new ArrayList<>();
        TestPlayer(World world) { super(world, new GameProfile(UUID.randomUUID(), "Builder")); }
        @Override public void addChatMessage(IChatComponent message) { messages.add(message); }
        @Override public void addChatComponentMessage(IChatComponent message) { messages.add(message); }
        @Override public boolean canCommandSenderUseCommand(int level, String command) { return true; }
        @Override public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 64, 0);
        }
        @Override public void addStat(StatBase stat, int amount) {}
        @Override public boolean canPlayerEdit(int x, int y, int z, int side, ItemStack stack) { return true; }
    }

    private static class TestMachineItem extends GT_Item_Machines {
        boolean secondaryTooltip;
        TestMachineItem(Block block) { super(block); }
        @Override protected boolean isStructureTooltipRequested() { return secondaryTooltip; }
        @Override public boolean onItemUse(ItemStack stack, EntityPlayer actor, World world, int x, int y, int z,
                                           int side, float hitX, float hitY, float hitZ) {
            int id = stack.getItemDamage();
            MetaTileEntity hatch = id < 32100 ? hatch(id - 32000)
                    : (MetaTileEntity) GregTech_API.METATILEENTITIES[id].newMetaEntity(null);
            ((TestWorld) world).putTile(x, y, z, new TestTile(world, hatch, ForgeDirection.NORTH));
            stack.stackSize--;
            return true;
        }
    }

    static class Source implements IItemSource {
        final List<ItemStack> stacks;
        int consumed;
        int queries;
        Source(ItemStack... stacks) { this.stacks = Arrays.asList(stacks); }
        @Override public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            queries++;
            Map<ItemStack, Integer> result = new LinkedHashMap<>();
            for (ItemStack stack : stacks) {
                if (stack.stackSize <= 0 || !predicate.test(stack)) continue;
                int taken = Math.min(count, stack.stackSize);
                ItemStack copy = stack.copy();
                copy.stackSize = taken;
                result.put(copy, taken);
                if (!simulate) { stack.stackSize -= taken; consumed += taken; }
                count -= taken;
                if (count == 0) break;
            }
            return result;
        }
    }

    static class HintProxy extends CommonProxy {
        int hints;
        int casingHints;
        int errors;
        final Set<String> errorPositions = new HashSet<>();
        final Map<String, Block> hintBlocks = new HashMap<>();
        final Map<String, Integer> hintMetadata = new HashMap<>();
        final Map<String, HintColor> hintFrames = new HashMap<>();
        Block lastBlock;
        int lastMetadata;
        EntityPlayer player;
        @Override public EntityPlayer getCurrentPlayer() { return player; }
        @Override public void enqueueClientTask(Runnable task) { task.run(); }
        @Override public boolean markHintParticleError(EntityPlayer player, World world, int x, int y, int z) {
            errors++;
            errorPositions.add(TestWorld.key(x, y, z));
            return true;
        }
        @Override public void hintParticle(World world, int x, int y, int z, Block block, int meta) {
            hints++;
            String position = TestWorld.key(x, y, z);
            hintBlocks.put(position, block);
            hintMetadata.put(position, meta);
            lastBlock = block;
            lastMetadata = meta;
            if (block == GregTech_API.sBlockCasings2 && meta == 1) casingHints++;
        }
        @Override
        public void hintFrame(World world, int x, int y, int z, HintColor color) {
            String position = TestWorld.key(x, y, z);
            assertTrue("A frame decorates an existing block hint", hintBlocks.containsKey(position));
            assertNull("Only one frame per position", hintFrames.put(position, color));
        }

        @Override public void hintMarker(World world, int x, int y, int z, HintMarkerKind kind) { hints++; }
    }
}
