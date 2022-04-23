package gregtech;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.commands.RecipesReload;
import gregtech.api.enchants.Enchantment_EnderDamage;
import gregtech.api.enchants.Enchantment_Radioactivity;
import gregtech.api.enums.*;
import gregtech.api.interfaces.internal.IGT_Mod;
import gregtech.api.objects.ItemData;
import gregtech.api.threads.GT_Runnable_MachineBlockUpdate;
import gregtech.api.util.*;
import gregtech.common.GT_DummyWorld;
import gregtech.common.GT_Network;
import gregtech.common.GT_Proxy;
import gregtech.common.GT_RecipeAdder;
import gregtech.common.items.GT_MetaGenerated_Tool_01;
import gregtech.common.items.ItemDebug;
import gregtech.common.items.armor.components.LoadArmorComponents;
import gregtech.common.items.behaviors.Behaviour_DataOrb;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Massfabricator;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_DigitalChestBase;
import gregtech.loaders.load.GT_CoverBehaviorLoader;
import gregtech.loaders.load.GT_FuelLoader;
import gregtech.loaders.load.GT_ItemIterator;
import gregtech.loaders.load.GT_SonictronLoader;
import gregtech.loaders.misc.GT_Achievements;
import gregtech.loaders.misc.GT_Bees;
import gregtech.loaders.misc.GT_CoverLoader;
import gregtech.loaders.postload.*;
import gregtech.loaders.preload.*;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static gregtech.api.enums.GT_Values.MOD_ID_AE;
import static gregtech.api.enums.GT_Values.MOD_ID_FR;

@Mod(modid = "gregtech", name = "GregTech", version = "MC1710", useMetadata = false,
		dependencies = " required-after:IC2;" +
				" after:Forestry;" +
				" after:appliedenergistics2;" +
				" after:BuildCraft|Transport;" +
				" after:BuildCraft|Silicon;" +
				" after:BuildCraft|Factory;" +
				" after:BuildCraft|Energy;" +
				" after:BuildCraft|Core;" +
				" after:BuildCraft|Builders;" +
				" after:GalacticraftCore;" +
				" after:GalacticraftMars;" +
				" after:GalacticraftPlanets;" +
				" after:UndergroundBiomes;" +
				" after:TConstruct;" +
				" after:Translocator;")

public class GT_Mod implements IGT_Mod {
	public static final int VERSION = 509, SUBVERSION = 34;
	public static final int TOTAL_VERSION = calculateTotalGTVersion(VERSION, SUBVERSION);
	public static final int REQUIRED_IC2 = 624;
	public static final Logger GT_FML_LOGGER = LogManager.getLogger("GregTech");
	@Mod.Instance("gregtech")
	public static GT_Mod instance;
	@SidedProxy(modId = "gregtech", clientSide = "gregtech.common.GT_Client", serverSide = "gregtech.common.GT_Server")
	public static GT_Proxy gregtechproxy;
	public static int MAX_IC2 = 2147483647;
	public static GT_Achievements achievements;
	
	static {
		if ((509 != GregTech_API.VERSION) || (509 != GT_ModHandler.VERSION) || (509 != GT_OreDictUnificator.VERSION) || (509 != GT_Recipe.VERSION) || (509 != GT_Utility.VERSION) || (509 != GT_RecipeRegistrator.VERSION) || (509 != Element.VERSION) || (509 != Materials.VERSION) || (509 != OrePrefixes.VERSION)) {
			throw new GT_ItsNotMyFaultException("One of your Mods included GregTech-API Files inside it's download, mention this to the Mod Author, who does this bad thing, and tell him/her to use reflection. I have added a Version check, to prevent Authors from breaking my Mod that way.");
		}
	}
	
	private final String aTextGeneral = "general";
	private final String aTextIC2 = "ic2_";
	public GT_PreLoader_Config initConfig = new GT_PreLoader_Config();
	
	public GT_Mod() {
		try {
			Class.forName("ic2.core.IC2").getField("enableOreDictCircuit").set(null, Boolean.FALSE);
		} catch (Throwable ignored) {
		}
		try {
			Class.forName("ic2.core.IC2").getField("enableCraftingBucket").set(null, Boolean.FALSE);
		} catch (Throwable ignored) {
		}
		try {
			Class.forName("ic2.core.IC2").getField("enableEnergyInStorageBlockItems").set(null, Boolean.FALSE);
		} catch (Throwable ignored) {
		}
		GT_Values.GT              = this;
		GT_Values.DW              = new GT_DummyWorld();
		GT_Values.NW              = new GT_Network();
		GregTech_API.sRecipeAdder = GT_Values.RA = new GT_RecipeAdder();
		
		Textures.BlockIcons.VOID.name();
		Textures.ItemIcons.VOID.name();
	}
	
	public static int calculateTotalGTVersion(int minorVersion) {
		return calculateTotalGTVersion(VERSION, minorVersion);
	}
	
	public static int calculateTotalGTVersion(int majorVersion, int minorVersion) {
		return majorVersion * 1000 + minorVersion;
	}
	
	@Mod.EventHandler
	public void onPreLoad(FMLPreInitializationEvent aEvent) {
		Locale.setDefault(Locale.ENGLISH);
		if (GregTech_API.sPreloadStarted) {
			return;
		}
		try {
			for (Runnable tRunnable : GregTech_API.sBeforeGTPreload) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		
		initConfig.registerFiles(aEvent);
		
		GregTech_API.mIC2Classic   = Loader.isModLoaded("IC2-Classic-Spmod");
		GregTech_API.mTranslocator = Loader.isModLoaded("Translocator");
		GregTech_API.mTConstruct   = Loader.isModLoaded("TConstruct");
		GregTech_API.mGalacticraft = Loader.isModLoaded("GalacticraftCore");
		GregTech_API.mAE2          = Loader.isModLoaded(MOD_ID_AE);
		
		gregtechproxy.onPreLoad();
		initConfig.initSettingConfigs(gregtechproxy);
		
		new Enchantment_EnderDamage();
		new Enchantment_Radioactivity();
		
		Materials.init();
		
		initConfig.save();
		initConfig.initLang(aEvent);
		
		try {
			//remove drop scrapbox
			GT_Utility.getField("ic2.core.item.ItemScrapbox$Drop", "topChance", true, true).set(null, 0);
			((List<?>) GT_Utility.getFieldContent(GT_Utility.getFieldContent("ic2.api.recipe.Recipes", "scrapboxDrops", true, true), "drops", true, true)).clear();
		} catch (Throwable e) {
			if (GT_Values.D1) {
				e.printStackTrace(GT_Log.err);
			}
		}
		GT_ModHandler.addScrapboxDrop(200.0F, GT_ModHandler.getIC2Item("scrap", 1L));
		
		initConfig.initMineTweaker();
		
		new GT_Loader_OreProcessing().run();
		new GT_Loader_OreDictionary().run();
		new GT_Loader_ItemData().run();
		new GT_Loader_Item_Block_And_Fluid().run();
		new GT_Loader_MetaTileEntities().run();
		
		new GT_Loader_CircuitBehaviors().run();
		new GT_CoverBehaviorLoader().run();
		new GT_SonictronLoader().run();
		new GT_SpawnEventHandler();
		
		double ashOutputMultiplier = Math.min(GregTech_API.sOPStuff.get(ConfigCategories.Recipes.gregtechrecipes, "AshOutputMultiplier", 1.0), 1.0);
		if (GregTech_API.sOPStuff.get(ConfigCategories.Recipes.gregtechrecipes, "AshYieldsAlkaliMetalOxides", true)) {
			int[] outputChances = new int[]{(int) (2400 * ashOutputMultiplier), (int) (2100 * ashOutputMultiplier),
					(int) (1200 * ashOutputMultiplier), (int) (500 * ashOutputMultiplier),
					(int) (500 * ashOutputMultiplier), (int) (250 * ashOutputMultiplier)};
			GT_Values.RA.addCentrifugeRecipe(Materials.Ash.getDust(1), GT_Values.NI, GT_Values.NF, GT_Values.NF,
					Materials.Quicklime.getDust(2), Materials.Potash.getDust(1), Materials.Magnesia.getDust(1),
					Materials.PhosphorousPentoxide.getDust(1), Materials.SodaAsh.getDust(1), Materials.BandedIron.getDust(1),
					outputChances, 240, 30
			);
		} else {
			GT_Values.RA.addCentrifugeRecipe(Materials.Ash.getDust(1), GT_Values.NI, GT_Values.NF, GT_Values.NF,
					Materials.Carbon.getDust(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI,
					new int[]{(int) (10000 * ashOutputMultiplier), 0, 0, 0, 0, 0}, 40, 16
			);
		}
		if (gregtechproxy.mSortToTheEnd) {
			try {
				GT_Log.out.println("GT_Mod: Sorting GregTech to the end of the Mod List for further processing.");
				LoadController tLoadController = (LoadController) GT_Utility.getFieldContent(Loader.instance(), "modController", true, true);
				if (tLoadController != null) {
					List<ModContainer> tModList = tLoadController.getActiveModList();
					List<ModContainer> tNewModsList = new ArrayList<>();
					ModContainer tGregTech = null;
					short tModList_sS = (short) tModList.size();
					for (short i = 0; i < tModList_sS; i = (short) (i + 1)) {
						ModContainer tMod = tModList.get(i);
						if (tMod.getModId().equalsIgnoreCase("gregtech")) {
							tGregTech = tMod;
						} else {
							tNewModsList.add(tMod);
						}
					}
					if (tGregTech != null) {
						tNewModsList.add(tGregTech);
					}
					GT_Utility.getField(tLoadController, "activeModList", true, true).set(tLoadController, tNewModsList);
				}
			} catch (Throwable e) {
				if (GT_Values.D1) {
					e.printStackTrace(GT_Log.err);
				}
			}
		}
		GregTech_API.sPreloadFinished = true;
		GT_Log.out.println("GT_Mod: Preload-Phase finished!");
		GT_Log.ore.println("GT_Mod: Preload-Phase finished!");
		try {
			for (Runnable tRunnable : GregTech_API.sAfterGTPreload) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
			GT_Assemblyline_Server.fillMap(aEvent);
		}
		
		ItemDebug.getInstance().registerItem();
	}
	
	@Mod.EventHandler
	public void onLoad(FMLInitializationEvent aEvent) {
		if (GregTech_API.sLoadStarted) {
			return;
		}
		try {
			for (Runnable tRunnable : GregTech_API.sBeforeGTLoad) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		
		if (Loader.isModLoaded(MOD_ID_FR)) {
			new GT_Bees();
		}
		
		//Disable Low Grav regardless of config if Cleanroom is disabled.
		if (!gregtechproxy.mEnableCleanroom) {
			gregtechproxy.mLowGravProcessing         = false;
			gregtechproxy.mLowGravProcessingCircuits = false;
		}
		
		gregtechproxy.onLoad();
		if (gregtechproxy.mSortToTheEnd) {
			new GT_ItemIterator().run();
			gregtechproxy.registerUnificationEntries();
			new GT_FuelLoader().run();
		}
		GregTech_API.sLoadFinished = true;
		GT_Log.out.println("GT_Mod: Load-Phase finished!");
		GT_Log.ore.println("GT_Mod: Load-Phase finished!");
		try {
			for (Runnable tRunnable : GregTech_API.sAfterGTLoad) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
	}
	
	@Mod.EventHandler
	public void onPostLoad(FMLPostInitializationEvent aEvent) {
		if (GregTech_API.sPostloadStarted) {
			return;
		}
		try {
			for (Runnable tRunnable : GregTech_API.sBeforeGTPostload) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		gregtechproxy.onPostLoad();
		if (gregtechproxy.mSortToTheEnd) {
			gregtechproxy.registerUnificationEntries();
		} else {
			new GT_ItemIterator().run();
			gregtechproxy.registerUnificationEntries();
			new GT_FuelLoader().run();
		}
		new GT_ItemMaxStacksizeLoader().run();
		new GT_BlockResistanceLoader().run();
		new GT_RecyclerBlacklistLoader().run();
		new GT_MinableRegistrator().run();
		new GT_MachineRecipeLoader().run();
		new GT_ScrapboxDropLoader().run();
		new GT_CropLoader().run();
		new GT_Worldgenloader().run();
		new GT_CoverLoader().run();
		LoadArmorComponents.init();
		
		GT_RecipeRegistrator.registerUsagesForMaterials(new ItemStack(Blocks.planks, 1), null, false);
		GT_RecipeRegistrator.registerUsagesForMaterials(new ItemStack(Blocks.cobblestone, 1), null, false);
		GT_RecipeRegistrator.registerUsagesForMaterials(new ItemStack(Blocks.stone, 1), null, false);
		GT_RecipeRegistrator.registerUsagesForMaterials(new ItemStack(Items.leather, 1), null, false);
		
		GT_OreDictUnificator.addItemData(GT_ModHandler.getRecipeOutput(null, GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Tin, 1L), null, GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Tin, 1L), null, GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Tin, 1L), null, null, null), new ItemData(Materials.Tin, 10886400L));
		if (!GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.storageblockcrafting, "tile.glowstone", false)) {
			GT_ModHandler.removeRecipe(new ItemStack(Items.glowstone_dust, 1), new ItemStack(Items.glowstone_dust, 1), null, new ItemStack(Items.glowstone_dust, 1), new ItemStack(Items.glowstone_dust, 1));
		}
		GT_ModHandler.removeRecipe(new ItemStack(Blocks.wooden_slab, 1, 0), new ItemStack(Blocks.wooden_slab, 1, 1), new ItemStack(Blocks.wooden_slab, 1, 2));
		GT_ModHandler.addCraftingRecipe(new ItemStack(Blocks.wooden_slab, 6, 0), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"WWW", 'W', new ItemStack(Blocks.planks, 1, 0)});
		
		//Save a copy of these list before activateOreDictHandler(), then loop over them.
		Map<IRecipeInput, RecipeOutput> aMaceratorRecipeList = GT_ModHandler.getMaceratorRecipeList();
		Map<IRecipeInput, RecipeOutput> aCompressorRecipeList = GT_ModHandler.getCompressorRecipeList();
		Map<IRecipeInput, RecipeOutput> aExtractorRecipeList = GT_ModHandler.getExtractorRecipeList();
		Map<IRecipeInput, RecipeOutput> aOreWashingRecipeList = GT_ModHandler.getOreWashingRecipeList();
		Map<IRecipeInput, RecipeOutput> aThermalCentrifugeRecipeList = GT_ModHandler.getThermalCentrifugeRecipeList();
		
		GT_Log.out.println("GT_Mod: Activating OreDictionary Handler, this can take some time, as it scans the whole OreDictionary");
		FMLLog.info("If your Log stops here, you were too impatient. Wait a bit more next time, before killing Minecraft with the Task Manager.");
		gregtechproxy.activateOreDictHandler();
		FMLLog.info("Congratulations, you have been waiting long enough. Have a Cake.");
		GT_Log.out.println("GT_Mod: List of Lists of Tool Recipes: " + GT_ModHandler.sSingleNonBlockDamagableRecipeList_list.toString());
		GT_Log.out.println("GT_Mod: Vanilla Recipe List -> Outputs null or stackSize <=0: " + GT_ModHandler.sVanillaRecipeList_warntOutput.toString());
		GT_Log.out.println("GT_Mod: Single Non Block Damagable Recipe List -> Outputs null or stackSize <=0: " + GT_ModHandler.sSingleNonBlockDamagableRecipeList_warntOutput.toString());
		//GT_Log.out.println("GT_Mod: sRodMaterialList cycles: " + GT_RecipeRegistrator.sRodMaterialList_cycles);
		
		//Add default IC2 recipe to GT
		GT_ModHandler.addIC2RecipesToGT(aMaceratorRecipeList, GT_Recipe.GT_Recipe_Map.sMaceratorRecipes, true, true, true);
		GT_ModHandler.addIC2RecipesToGT(aCompressorRecipeList, GT_Recipe.GT_Recipe_Map.sCompressorRecipes, true, true, true);
		GT_ModHandler.addIC2RecipesToGT(aExtractorRecipeList, GT_Recipe.GT_Recipe_Map.sExtractorRecipes, true, true, true);
		GT_ModHandler.addIC2RecipesToGT(aOreWashingRecipeList, GT_Recipe.GT_Recipe_Map.sOreWasherRecipes, false, true, true);
		GT_ModHandler.addIC2RecipesToGT(aThermalCentrifugeRecipeList, GT_Recipe.GT_Recipe_Map.sThermalCentrifugeRecipes, true, true, true);
		
		if (GT_Values.D1) {
			GT_ModHandler.sSingleNonBlockDamagableRecipeList.forEach(recipe -> GT_Log.out.println("=> " + recipe.getRecipeOutput().getDisplayName()));
		}
		new GT_CraftingRecipeLoader().run();
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, "ic2forgehammer", true)) {
			GT_ModHandler.removeRecipeByOutput(ItemList.IC2_ForgeHammer.getWildcard(1L));
		}
		GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item("machine", 1L));
		GT_ModHandler.addCraftingRecipe(GT_ModHandler.getIC2Item("machine", 1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"RRR", "RwR", "RRR", 'R', OrePrefixes.plate.get(Materials.Iron)});
		ItemStack ISdata0 = new ItemStack(Items.potionitem, 1, 0);
		ItemStack ILdata0 = ItemList.Bottle_Empty.get(1L);
		for (FluidContainerRegistry.FluidContainerData tData : FluidContainerRegistry.getRegisteredFluidContainerData()) {
			if ((tData.filledContainer.getItem() == Items.potionitem) && (tData.filledContainer.getItemDamage() == 0)) {
				GT_Recipe.GT_Recipe_Map.sFluidCannerRecipes.addRecipe(true, new ItemStack[]{ILdata0}, new ItemStack[]{ISdata0}, null, new FluidStack[]{Materials.Water.getFluid(250L)}, null, 4, 1, 0);
				GT_Recipe.GT_Recipe_Map.sFluidCannerRecipes.addRecipe(true, new ItemStack[]{ISdata0}, new ItemStack[]{ILdata0}, null, null, null, 4, 1, 0);
			} else {
				GT_Recipe.GT_Recipe_Map.sFluidCannerRecipes.addRecipe(true, new ItemStack[]{tData.emptyContainer}, new ItemStack[]{tData.filledContainer}, null, new FluidStack[]{tData.fluid}, null, tData.fluid.amount / 62, 1, 0);
				GT_Recipe.GT_Recipe_Map.sFluidCannerRecipes.addRecipe(true, new ItemStack[]{tData.filledContainer}, new ItemStack[]{GT_Utility.getContainerItem(tData.filledContainer, true)}, null, null, new FluidStack[]{tData.fluid}, tData.fluid.amount / 62, 1, 0);
			}
		}
		
		if (Loader.isModLoaded(MOD_ID_FR)) {
			GT_Forestry_Compat.transferCentrifugeRecipes();
			GT_Forestry_Compat.transferSqueezerRecipes();
		}
		if (GregTech_API.mAE2) {
			GT_MetaTileEntity_DigitalChestBase.registerAEIntegration();
		}
		String tName;
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "blastfurnace"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "blockcutter"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "inductionFurnace"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "generator"), false)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "windMill"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "waterMill"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "solarPanel"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "centrifuge"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "electrolyzer"), false)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "compressor"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "electroFurnace"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "extractor"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "macerator"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "recycler"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "metalformer"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "orewashingplant"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "massFabricator"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, aTextIC2 + (tName = "replicator"), true)) {
			GT_ModHandler.removeRecipeByOutput(GT_ModHandler.getIC2Item(tName, 1L));
		}
		if (gregtechproxy.mNerfedVanillaTools) {
			GT_Log.out.println("GT_Mod: Nerfing Vanilla Tool Durability");
			Items.wooden_sword.setMaxDamage(12);
			Items.wooden_pickaxe.setMaxDamage(12);
			Items.wooden_shovel.setMaxDamage(12);
			Items.wooden_axe.setMaxDamage(12);
			Items.wooden_hoe.setMaxDamage(12);
			
			Items.stone_sword.setMaxDamage(48);
			Items.stone_pickaxe.setMaxDamage(48);
			Items.stone_shovel.setMaxDamage(48);
			Items.stone_axe.setMaxDamage(48);
			Items.stone_hoe.setMaxDamage(48);
			
			Items.iron_sword.setMaxDamage(256);
			Items.iron_pickaxe.setMaxDamage(256);
			Items.iron_shovel.setMaxDamage(256);
			Items.iron_axe.setMaxDamage(256);
			Items.iron_hoe.setMaxDamage(256);
			
			Items.golden_sword.setMaxDamage(24);
			Items.golden_pickaxe.setMaxDamage(24);
			Items.golden_shovel.setMaxDamage(24);
			Items.golden_axe.setMaxDamage(24);
			Items.golden_hoe.setMaxDamage(24);
			
			Items.diamond_sword.setMaxDamage(768);
			Items.diamond_pickaxe.setMaxDamage(768);
			Items.diamond_shovel.setMaxDamage(768);
			Items.diamond_axe.setMaxDamage(768);
			Items.diamond_hoe.setMaxDamage(768);
		}
		GT_Log.out.println("GT_Mod: Adding buffered Recipes.");
		GT_ModHandler.stopBufferingCraftingRecipes();
		
		GT_Log.out.println("GT_Mod: Saving Lang File.");
		GT_LanguageManager.sEnglishFile.save();
		GregTech_API.sPostloadFinished = true;
		GT_Log.out.println("GT_Mod: PostLoad-Phase finished!");
		GT_Log.ore.println("GT_Mod: PostLoad-Phase finished!");
		try {
			for (Runnable tRunnable : GregTech_API.sAfterGTPostload) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		GT_Log.out.println("GT_Mod: Adding Fake Recipes for NEI");
		
		if (Loader.isModLoaded(MOD_ID_FR))
			GT_Forestry_Compat.populateFakeNeiRecipes();
		
		if (ItemList.IC2_Crop_Seeds.get(1L) != null) {
			GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, new ItemStack[]{ItemList.IC2_Crop_Seeds.getWildcard(1L)}, new ItemStack[]{ItemList.IC2_Crop_Seeds.getWithName(1L, "Scanned Seeds")}, null, null, null, 160, 8, 0);
		}
		GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, new ItemStack[]{new ItemStack(Items.written_book, 1, 32767)}, new ItemStack[]{ItemList.Tool_DataStick.getWithName(1L, "Scanned Book Data")}, ItemList.Tool_DataStick.getWithName(1L, "Stick to save it to"), null, null, 128, 30, 0);
		GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, new ItemStack[]{new ItemStack(Items.filled_map, 1, 32767)}, new ItemStack[]{ItemList.Tool_DataStick.getWithName(1L, "Scanned Map Data")}, ItemList.Tool_DataStick.getWithName(1L, "Stick to save it to"), null, null, 128, 30, 0);
		GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, new ItemStack[]{ItemList.Tool_DataOrb.getWithName(1L, "Orb to overwrite")}, new ItemStack[]{ItemList.Tool_DataOrb.getWithName(1L, "Copy of the Orb")}, ItemList.Tool_DataOrb.getWithName(0L, "Orb to copy"), null, null, 512, 30, 0);
		GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, new ItemStack[]{ItemList.Tool_DataStick.getWithName(1L, "Stick to overwrite")}, new ItemStack[]{ItemList.Tool_DataStick.getWithName(1L, "Copy of the Stick")}, ItemList.Tool_DataStick.getWithName(0L, "Stick to copy"), null, null, 128, 30, 0);
		GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, new ItemStack[]{ItemList.Tool_DataStick.getWithName(1L, "Raw Prospection Data")}, new ItemStack[]{ItemList.Tool_DataStick.getWithName(1L, "Analyzed Prospection Data")}, null, null, null, 1000, 30, 0);
		
		for (Materials tMaterial : Materials.values()) {
			if ((tMaterial.mElement != null) && (!tMaterial.mElement.mIsIsotope) && (tMaterial.getMass() > 0L)) {
				ItemStack tOutput = ItemList.Tool_DataOrb.get(1L);
				Behaviour_DataOrb.setDataTitle(tOutput, "Elemental-Scan");
				Behaviour_DataOrb.setDataName(tOutput, tMaterial.mElement.name());
				ItemStack tInput = GT_OreDictUnificator.get(OrePrefixes.dust, tMaterial, 1L);
				ItemStack[] ISmat0 = new ItemStack[]{tInput};
				ItemStack[] ISmat1 = new ItemStack[]{tOutput};
				if (tInput != null) {
					GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, ISmat0, ISmat1, ItemList.Tool_DataOrb.get(1L), null, null, (int) (tMaterial.getMass() * 8192L), 30, 0);
					GT_Recipe.GT_Recipe_Map.sReplicatorFakeRecipes.addFakeRecipe(false, null, ISmat0, ISmat1, new FluidStack[]{Materials.UUMatter.getFluid(tMaterial.getMass())}, null, (int) (tMaterial.getMass() * 512L), 30, 0);
				}
				tInput = GT_OreDictUnificator.get(OrePrefixes.cell, tMaterial, 1L);
				ISmat0 = new ItemStack[]{tInput};
				if (tInput != null) {
					GT_Recipe.GT_Recipe_Map.sScannerFakeRecipes.addFakeRecipe(false, ISmat0, ISmat1, ItemList.Tool_DataOrb.get(1L), null, null, (int) (tMaterial.getMass() * 8192L), 30, 0);
					GT_Recipe.GT_Recipe_Map.sReplicatorFakeRecipes.addFakeRecipe(false, null, ISmat0, ISmat1, new FluidStack[]{Materials.UUMatter.getFluid(tMaterial.getMass())}, null, (int) (tMaterial.getMass() * 512L), 30, 0);
				}
			}
			if (tMaterial.mFluid != null) Materials.MATERIALS_FLUID.put(tMaterial.mFluid, tMaterial);
			if (tMaterial.mGas != null) Materials.MATERIALS_FLUID.put(tMaterial.mGas, tMaterial);
			if (tMaterial.mStandardMoltenFluid != null) Materials.MATERIALS_FLUID.put(tMaterial.mStandardMoltenFluid, tMaterial);
			if (tMaterial.mStandardMoltenHot != null) Materials.MATERIALS_FLUID.put(tMaterial.mStandardMoltenHot, tMaterial);
			if (tMaterial.mPlasma != null) Materials.MATERIALS_FLUID.put(tMaterial.mPlasma, tMaterial);
		}
		GT_Recipe.GT_Recipe_Map.sOrganicReplicatorFakeRecipes.addFakeRecipe(false, (new ItemStack[]{ItemList.IC2_Crop_Seeds.getWithName(1, "Instance seeds to duplicate(does not get consumed in progress)")}), (new ItemStack[]{ItemList.IC2_Crop_Seeds.getWithName(1, "Duplicated seeds; Chance equals efficiency")}), null, (new FluidStack[]{Materials.UUMatter.getFluid(1L)}), null, 200, 32, 0);
		if (!GT_MetaTileEntity_Massfabricator.sRequiresUUA) GT_Recipe.GT_Recipe_Map.sMassFabFakeRecipes.addFakeRecipe(false, null, null, null, null, new FluidStack[]{Materials.UUMatter.getFluid(1L)}, GT_MetaTileEntity_Massfabricator.sDurationMultiplier, 256, 0);
		GT_Recipe.GT_Recipe_Map.sMassFabFakeRecipes.addFakeRecipe(false, null, null, null, new FluidStack[]{Materials.UUAmplifier.getFluid(GT_MetaTileEntity_Massfabricator.sUUAperUUM)}, new FluidStack[]{Materials.UUMatter.getFluid(1L)}, GT_MetaTileEntity_Massfabricator.sDurationMultiplier / GT_MetaTileEntity_Massfabricator.sUUASpeedBonus, 256, 0);
		GT_Recipe.GT_Recipe_Map.sRockBreakerFakeRecipes.addFakeRecipe(false, new ItemStack[]{ItemList.Display_ITS_FREE.getWithName(0L, "Place Lava on Side")}, new ItemStack[]{new ItemStack(Blocks.cobblestone, 1)}, null, null, null, 16, 30, 0);
		GT_Recipe.GT_Recipe_Map.sRockBreakerFakeRecipes.addFakeRecipe(false, new ItemStack[]{ItemList.Display_ITS_FREE.getWithName(0L, "Place Lava on Top")}, new ItemStack[]{new ItemStack(Blocks.stone, 1)}, null, null, null, 16, 30, 0);
		GT_Recipe.GT_Recipe_Map.sRockBreakerFakeRecipes.addFakeRecipe(false, new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L)}, new ItemStack[]{new ItemStack(Blocks.obsidian, 1)}, null, null, null, 128, 30, 0);
		
		if (GregTech_API.mOutputRF || GregTech_API.mInputRF) {
			GT_Utility.checkAvailabilities();
			if (!GT_Utility.RF_CHECK) {
				GregTech_API.mOutputRF = false;
				GregTech_API.mInputRF  = false;
			}
		}
		
		addSolidFakeLargeBoilerFuels();
		
		achievements               = new GT_Achievements();
		GT_Recipe.GTppRecipeHelper = true;
		GT_Log.out.println("GT_Mod: Loading finished, deallocating temporary Init Variables.");
		GregTech_API.sBeforeGTPreload  = null;
		GregTech_API.sAfterGTPreload   = null;
		GregTech_API.sBeforeGTLoad     = null;
		GregTech_API.sAfterGTLoad      = null;
		GregTech_API.sBeforeGTPostload = null;
		GregTech_API.sAfterGTPostload  = null;
		
		CreativeTabs mainTab = new CreativeTabs("GTtools") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getIconItemStack() {
				return ItemList.Tool_Cheat.get(1, new ItemStack(Blocks.iron_block, 1));
			}
			
			@SideOnly(Side.CLIENT)
			@Override
			public Item getTabIconItem() {
				return ItemList.Circuit_Integrated.getItem();
			}
			
			@Override
			@SuppressWarnings("unchecked")
			public void displayAllReleventItems(List aList) {
				for (int i = 0; i < 32766; i += 2) {
					if (GT_MetaGenerated_Tool_01.INSTANCE.getToolStats(new ItemStack(GT_MetaGenerated_Tool_01.INSTANCE, 1, i)) != null) {
						ItemStack tStack = new ItemStack(GT_MetaGenerated_Tool_01.INSTANCE, 1, i);
						GT_MetaGenerated_Tool_01.INSTANCE.isItemStackUsable(tStack);
						aList.add(GT_MetaGenerated_Tool_01.INSTANCE.getToolWithStats(i, 1, Materials.Lead, Materials.Lead, null));
						aList.add(GT_MetaGenerated_Tool_01.INSTANCE.getToolWithStats(i, 1, Materials.Nickel, Materials.Nickel, null));
						aList.add(GT_MetaGenerated_Tool_01.INSTANCE.getToolWithStats(i, 1, Materials.Cobalt, Materials.Cobalt, null));
						aList.add(GT_MetaGenerated_Tool_01.INSTANCE.getToolWithStats(i, 1, Materials.Osmium, Materials.Osmium, null));
						aList.add(GT_MetaGenerated_Tool_01.INSTANCE.getToolWithStats(i, 1, Materials.Adamantium, Materials.Adamantium, null));
						aList.add(GT_MetaGenerated_Tool_01.INSTANCE.getToolWithStats(i, 1, Materials.Neutronium, Materials.Neutronium, null));
					}
				}
				super.displayAllReleventItems(aList);
			}
		};
	}
	
	@Mod.EventHandler
	public void onServerAboutToStart(FMLServerAboutToStartEvent aEvent) {
		gregtechproxy.onServerAboutToStart();
	}
	
	@Mod.EventHandler
	@SuppressWarnings("unchecked")
	public void onServerStarting(FMLServerStartingEvent aEvent) {
		try {
			for (Runnable tRunnable : GregTech_API.sBeforeGTServerstart) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		gregtechproxy.onServerStarting();
		aEvent.registerServerCommand(new RecipesReload());
		//Check for more IC2 recipes on ServerStart to also catch MineTweaker additions
		GT_ModHandler.addIC2RecipesToGT(GT_ModHandler.getMaceratorRecipeList(), GT_Recipe.GT_Recipe_Map.sMaceratorRecipes, true, true, true);
		GT_ModHandler.addIC2RecipesToGT(GT_ModHandler.getCompressorRecipeList(), GT_Recipe.GT_Recipe_Map.sCompressorRecipes, true, true, true);
		GT_ModHandler.addIC2RecipesToGT(GT_ModHandler.getExtractorRecipeList(), GT_Recipe.GT_Recipe_Map.sExtractorRecipes, true, true, true);
		GT_ModHandler.addIC2RecipesToGT(GT_ModHandler.getOreWashingRecipeList(), GT_Recipe.GT_Recipe_Map.sOreWasherRecipes, false, true, true);
		GT_ModHandler.addIC2RecipesToGT(GT_ModHandler.getThermalCentrifugeRecipeList(), GT_Recipe.GT_Recipe_Map.sThermalCentrifugeRecipes, true, true, true);
		GT_Log.out.println("GT_Mod: Unificating outputs of all known Recipe Types.");
		ArrayList<ItemStack> tStacks = new ArrayList<>(10000);
		GT_Log.out.println("GT_Mod: IC2 Machines");
		
		Recipes.cannerBottle.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.centrifuge.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.compressor.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.extractor.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.macerator.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.metalformerCutting.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.metalformerExtruding.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.metalformerRolling.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.matterAmplifier.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		Recipes.oreWashing.getRecipes().values()
				.forEach(recipe -> tStacks.addAll(recipe.items));
		
		GT_Log.out.println("GT_Mod: Smelting");
		FurnaceRecipes.smelting().getSmeltingList().values()
				.forEach(obj -> tStacks.add((ItemStack) obj));
		
		if (gregtechproxy.mCraftingUnification) {
			GT_Log.out.println("GT_Mod: Crafting Recipes");
			for (Object tRecipe : CraftingManager.getInstance().getRecipeList()) {
				if ((tRecipe instanceof IRecipe)) {
					tStacks.add(((IRecipe) tRecipe).getRecipeOutput());
				}
			}
		}
		for (ItemStack tOutput : tStacks) {
			if (gregtechproxy.mRegisteredOres.contains(tOutput)) {
				GT_FML_LOGGER.error("GT-ERR-01: @ " + tOutput.getUnlocalizedName() + "   " + tOutput.getDisplayName());
				GT_FML_LOGGER.error("A Recipe used an OreDict Item as Output directly, without copying it before!!! This is a typical CallByReference/CallByValue Error");
				GT_FML_LOGGER.error("Said Item will be renamed to make the invalid Recipe visible, so that you can report it properly.");
				GT_FML_LOGGER.error("Please check all Recipes outputting this Item, and report the Recipes to their Owner.");
				GT_FML_LOGGER.error("The Owner of the ==>RECIPE<==, NOT the Owner of the Item, which has been mentioned above!!!");
				GT_FML_LOGGER.error("And ONLY Recipes which are ==>OUTPUTTING<== the Item, sorry but I don't want failed Bug Reports.");
				GT_FML_LOGGER.error("GregTech just reports this Error to you, so you can report it to the Mod causing the Problem.");
				GT_FML_LOGGER.error("Even though I make that Bug visible, I can not and will not fix that for you, that's for the causing Mod to fix.");
				GT_FML_LOGGER.error("And speaking of failed Reports:");
				GT_FML_LOGGER.error("Both IC2 and GregTech CANNOT be the CAUSE of this Problem, so don't report it to either of them.");
				GT_FML_LOGGER.error("I REPEAT, BOTH, IC2 and GregTech CANNOT be the source of THIS BUG. NO MATTER WHAT.");
				GT_FML_LOGGER.error("Asking in the IC2 Forums, which Mod is causing that, won't help anyone, since it is not possible to determine, which Mod it is.");
				GT_FML_LOGGER.error("If it would be possible, then I would have had added the Mod which is causing it to the Message already. But it is not possible.");
				GT_FML_LOGGER.error("Sorry, but this Error is serious enough to justify this Wall-O-Text and the partially allcapsed Language.");
				GT_FML_LOGGER.error("Also it is a Ban Reason on the IC2-Forums to post this seriously.");
				tOutput.setStackDisplayName("ERROR! PLEASE CHECK YOUR LOG FOR 'GT-ERR-01'!");
			} else {
				GT_OreDictUnificator.setStack(tOutput);
			}
		}
		GregTech_API.mServerStarted = true;
		GT_Log.out.println("GT_Mod: ServerStarting-Phase finished!");
		GT_Log.ore.println("GT_Mod: ServerStarting-Phase finished!");
		try {
			for (Runnable tRunnable : GregTech_API.sAfterGTServerstart) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		// Sets a new Machine Block Update Thread everytime a world is loaded
		GT_Runnable_MachineBlockUpdate.initExecutorService();
	}
	
	@Mod.EventHandler
	public void onServerStarted(FMLServerStartedEvent aEvent) {
		gregtechproxy.onServerStarted();
	}
	
	@Mod.EventHandler
	public void onIDChangingEvent(FMLModIdMappingEvent aEvent) {
		GT_Utility.reInit();
		GT_Recipe.reInit();
		try {
			GregTech_API.sItemStackMappings.forEach(GT_Utility::reMap);
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
	}
	
	@Mod.EventHandler
	public void onServerStopping(FMLServerStoppingEvent aEvent) {
		try {
			for (Runnable tRunnable : GregTech_API.sBeforeGTServerstop) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		gregtechproxy.onServerStopping();
		try {
			if ((GT_Values.D1) || (GT_Log.out != System.out)) {
				GT_Log.out.println("Printing List of all registered Objects inside the OreDictionary, now with free extra Sorting:");
				String[] tList = OreDictionary.getOreNames();
				Arrays.sort(tList);
				for (String tOreName : tList) {
					int tAmount = OreDictionary.getOres(tOreName).size();
					if (tAmount > 0) {
						GT_Log.out.println((tAmount < 10 ? " " : "") + tAmount + "x " + tOreName);
					}
				}
				GT_Log.out.println("Printing List of all registered Objects inside the Fluid Registry, now with free extra Sorting:");
				tList = FluidRegistry.getRegisteredFluids().keySet().toArray(new String[0]);
				Arrays.sort(tList);
				for (String tFluidName : tList) {
					GT_Log.out.println(tFluidName);
				}
				GT_Log.out.println("Outputting all the Names inside the Biomeslist");
				for (int i = 0; i < BiomeGenBase.getBiomeGenArray().length; i++) {
					if (BiomeGenBase.getBiomeGenArray()[i] != null) {
						GT_Log.out.println(BiomeGenBase.getBiomeGenArray()[i].biomeID + " = " + BiomeGenBase.getBiomeGenArray()[i].biomeName);
					}
				}
				GT_Log.out.println("Printing List of generatable Materials");
				for (int i = 0; i < GregTech_API.sGeneratedMaterials.length; i++) {
					if (GregTech_API.sGeneratedMaterials[i] == null) {
						GT_Log.out.println("Index " + i + ":" + null);
					} else {
						GT_Log.out.println("Index " + i + ":" + GregTech_API.sGeneratedMaterials[i]);
					}
				}
				GT_Log.out.println("END GregTech-Debug");
			}
		} catch (Throwable e) {
			if (GT_Values.D1) {
				e.printStackTrace(GT_Log.err);
			}
		}
		try {
			for (Runnable tRunnable : GregTech_API.sAfterGTServerstop) {
				GT_Log.ore.println("REGISTER GT FROM RUNNERS: " + tRunnable.getClass().getCanonicalName());
				tRunnable.run();
			}
		} catch (Throwable e) {
			e.printStackTrace(GT_Log.err);
		}
		//Interrupt IDLE Threads to close down cleanly
		GT_Runnable_MachineBlockUpdate.shutdownExecutorService();
	}
	
	public boolean isServerSide() {
		return gregtechproxy.isServerSide();
	}
	
	public boolean isClientSide() {
		return gregtechproxy.isClientSide();
	}
	
	public boolean isBukkitSide() {
		return gregtechproxy.isBukkitSide();
	}
	
	public EntityPlayer getThePlayer() {
		return gregtechproxy.getThePlayer();
	}
	
	public int addArmor(String aArmorPrefix) {
		return gregtechproxy.addArmor(aArmorPrefix);
	}
	
	public void doSonictronSound(ItemStack aStack, World aWorld, double aX, double aY, double aZ) {
		gregtechproxy.doSonictronSound(aStack, aWorld, aX, aY, aZ);
	}
	
	private void addSolidFakeLargeBoilerFuels() {
		GT_Recipe.GT_Recipe_Map.sLargeBoilerFakeFuels.addSolidRecipes(
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Charcoal, 1),
				GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Charcoal, 1),
				GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1),
				GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Coal, 1),
				GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 1),
				GT_OreDictUnificator.get(OrePrefixes.crushed, Materials.Coal, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lignite, 1),
				GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Lignite, 1),
				GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 1),
				GT_OreDictUnificator.get(OrePrefixes.crushed, Materials.Lignite, 1),
				GT_OreDictUnificator.get(OrePrefixes.log, Materials.Wood, 1),
				GT_OreDictUnificator.get(OrePrefixes.plank, Materials.Wood, 1),
				GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1),
				GT_OreDictUnificator.get(OrePrefixes.slab, Materials.Wood, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Caesium, 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1),
				GT_OreDictUnificator.get(ItemList.Block_SSFUEL.get(1)),
				GT_OreDictUnificator.get(ItemList.Block_MSSFUEL.get(1)),
				GT_OreDictUnificator.get(OrePrefixes.rod, Materials.Blaze, 1)
		);
	}
}
