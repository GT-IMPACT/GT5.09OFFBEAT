package gregtech.loaders.preload;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.util.GT_Config;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Log;
import gregtech.common.GT_Proxy;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gregtech.GT_Mod.GT_FML_LOGGER;

public class GT_PreLoader_Config {
	private static final String aTextGeneral = "general";
	Configuration tMainConfig;
	
	public void save() {
		GT_Log.out.println("GT_Mod: Saving Main Config");
		tMainConfig.save();
	}
	
	public void registerFiles(FMLPreInitializationEvent aEvent) {
		File tFile = new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "GregTech.cfg");
		tMainConfig = new Configuration(tFile);
		tMainConfig.load();
		tFile                    = new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "IDs.cfg");
		GT_Config.sConfigFileIDs = new Configuration(tFile);
		GT_Config.sConfigFileIDs.load();
		GT_Config.sConfigFileIDs.save();
		
		GregTech_API.sRecipeFile         = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "Recipes.cfg")));
		GregTech_API.sMachineFile        = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "MachineStats.cfg")));
		GregTech_API.sWorldgenFile       = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "WorldGeneration.cfg")));
		GregTech_API.sMaterialProperties = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "MaterialProperties.cfg")));
		GregTech_API.sMaterialComponents = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "MaterialComponents.cfg")));
		GregTech_API.sUnification        = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "Unification.cfg")));
		GregTech_API.sSpecialFile        = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "Other.cfg")));
		GregTech_API.sOPStuff            = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "OverpoweredStuff.cfg")));
		GregTech_API.sModularArmor       = new GT_Config(new Configuration(new File(new File(aEvent.getModConfigurationDirectory(), "GregTech"), "ModularArmor.cfg")));
		GregTech_API.sClientDataFile     = new GT_Config(new Configuration(new File(aEvent.getModConfigurationDirectory().getParentFile(), "GregTech.cfg")));
		
		GT_Log.mLogFile = new File(aEvent.getModConfigurationDirectory().getParentFile(), "logs/GregTech.log");
		if (!GT_Log.mLogFile.exists()) {
			try {
				GT_Log.mLogFile.createNewFile();
			} catch (Throwable ignored) {
			}
		}
		try {
			GT_Log.out = GT_Log.err = new PrintStream(GT_Log.mLogFile);
		} catch (FileNotFoundException ignored) {
		}
		GT_Log.mOreDictLogFile = new File(aEvent.getModConfigurationDirectory().getParentFile(), "logs/OreDict.log");
		if (!GT_Log.mOreDictLogFile.exists()) {
			try {
				GT_Log.mOreDictLogFile.createNewFile();
			} catch (Throwable ignored) {
			}
		}
		if (tMainConfig.get(aTextGeneral, "LoggingExplosions", true).getBoolean(true)) {
			GT_Log.mExplosionLog = new File(aEvent.getModConfigurationDirectory().getParentFile(), "logs/Explosion.log");
			if (!GT_Log.mExplosionLog.exists()) {
				try {
					GT_Log.mExplosionLog.createNewFile();
				} catch (Throwable ignored) {
				}
			}
			try {
				GT_Log.exp = new PrintStream(GT_Log.mExplosionLog);
			} catch (Throwable ignored) {
			}
		}
		if (tMainConfig.get(aTextGeneral, "LoggingPlayerActivity", true).getBoolean(true)) {
			GT_Log.mPlayerActivityLogFile = new File(aEvent.getModConfigurationDirectory().getParentFile(), "logs/PlayerActivity.log");
			if (!GT_Log.mPlayerActivityLogFile.exists()) {
				try {
					GT_Log.mPlayerActivityLogFile.createNewFile();
				} catch (Throwable ignored) {
				}
			}
			try {
				GT_Log.pal = new PrintStream(GT_Log.mPlayerActivityLogFile);
			} catch (Throwable ignored) {
			}
		}
		try {
			List<String> tList = ((GT_Log.LogBuffer) GT_Log.ore).mBufferedOreDictLog;
			GT_Log.ore.println("******************************************************************************");
			GT_Log.ore.println("* This is the complete log of the GT5-Unofficial OreDictionary Handler. It   *");
			GT_Log.ore.println("* processes all OreDictionary entries and can sometimes cause errors. All    *");
			GT_Log.ore.println("* entries and errors are being logged. If you see an error please raise an   *");
			GT_Log.ore.println("* issue at https://github.com/GT-IMPACT/IMPACT-MODPACK/issues.               *");
			GT_Log.ore.println("******************************************************************************");
			tList.forEach(GT_Log.ore::println);
		} catch (Throwable ignored) {
		}
	}
	
	public void initSettingConfigs(GT_Proxy gregtechproxy) {
		GT_Log.out.println("GT_Mod: Setting Configs");
		GT_Values.D1                                         = tMainConfig.get(aTextGeneral, "Debug", false).getBoolean(false);
		GT_Values.D2                                         = tMainConfig.get(aTextGeneral, "Debug2", false).getBoolean(false);
		GT_Values.allow_broken_recipemap                     = tMainConfig.get(aTextGeneral, "debug allow broken recipemap", false).getBoolean(false);
		GT_Values.debugRecipes                               = tMainConfig.get(aTextGeneral, "debugRecipes", false, "Displaying an \"Error\" item in recipes if they are broken or the item is missing [default: false]").getBoolean(false);
		GT_Values.debugCleanroom                             = tMainConfig.get(aTextGeneral, "debugCleanroom", false).getBoolean(false);
		GT_Values.debugDriller                               = tMainConfig.get(aTextGeneral, "debugDriller", false).getBoolean(false);
		GT_Values.debugWorldGen                              = tMainConfig.get(aTextGeneral, "debugWorldGen", false).getBoolean(false);
		GT_Values.debugOrevein                               = tMainConfig.get(aTextGeneral, "debugOrevein", false).getBoolean(false);
		GT_Values.debugSmallOres                             = tMainConfig.get(aTextGeneral, "debugSmallOres", false).getBoolean(false);
		GT_Values.debugStones                                = tMainConfig.get(aTextGeneral, "debugStones", false).getBoolean(false);
		GT_Values.oreveinPercentage                          = tMainConfig.get(aTextGeneral, "oreveinPercentage_100", 100).getInt(100);
		GT_Values.oreveinAttempts                            = tMainConfig.get(aTextGeneral, "oreveinAttempts_64", 64).getInt(64);
		GT_Values.oreveinMaxPlacementAttempts                = tMainConfig.get(aTextGeneral, "oreveinMaxPlacementAttempts_8", 8).getInt(8);
		GT_Values.oreveinPlacerOres                          = tMainConfig.get(aTextGeneral, "oreveinPlacerOres", true).getBoolean(true);
		GT_Values.oreveinPlacerOresMultiplier                = tMainConfig.get(aTextGeneral, "oreveinPlacerOresMultiplier", 2).getInt(2);
		GT_Values.ticksBetweenSounds                         = tMainConfig.get("machines", "TicksBetweenSounds", 30).getInt(30);
		GT_Values.cleanroomGlass                             = (float) tMainConfig.get("machines", "ReinforcedGlassPercentageForCleanroom", 35D).getDouble(35D);
		GregTech_API.TICKS_FOR_LAG_AVERAGING                 = tMainConfig.get(aTextGeneral, "TicksForLagAveragingWithScanner", 25).getInt(25);
		GregTech_API.MILLISECOND_THRESHOLD_UNTIL_LAG_WARNING = tMainConfig.get(aTextGeneral, "MillisecondsPassedInGTTileEntityUntilLagWarning", 100).getInt(100);
		if (tMainConfig.get(aTextGeneral, "disable_STDOUT", false).getBoolean(false)) {
			GT_FML_LOGGER.info("Disableing Console Messages.");
			GT_FML_LOGGER.exit();
			System.out.close();
			System.err.close();
		}
		GregTech_API.sMachineExplosions          = tMainConfig.get("machines", "machines_explosion_damage", true).getBoolean(false);
		GregTech_API.sMachineFlammable           = tMainConfig.get("machines", "machines_flammable", true).getBoolean(false);
		GregTech_API.sMachineNonWrenchExplosions = tMainConfig.get("machines", "explosions_on_nonwrenching", true).getBoolean(false);
		GregTech_API.sMachineWireFire            = tMainConfig.get("machines", "wirefire_on_explosion", true).getBoolean(false);
		GregTech_API.sMachineFireExplosions      = tMainConfig.get("machines", "fire_causes_explosions", true).getBoolean(false);
		GregTech_API.sMachineRainExplosions      = tMainConfig.get("machines", "rain_causes_explosions", true).getBoolean(false);
		GregTech_API.sMachineThunderExplosions   = tMainConfig.get("machines", "lightning_causes_explosions", true).getBoolean(false);
		GregTech_API.sConstantEnergy             = tMainConfig.get("machines", "constant_need_of_energy", true).getBoolean(false);
		GregTech_API.sColoredGUI                 = tMainConfig.get("machines", "colored_guis_when_painted", true).getBoolean(false);
		
		GregTech_API.sUseMachineMetal = tMainConfig.get("machines", "use_machine_metal_tint", true).getBoolean(true);
		
		GregTech_API.sTimber                   = tMainConfig.get(aTextGeneral, "timber_axe", true).getBoolean(true);
		GregTech_API.sDrinksAlwaysDrinkable    = tMainConfig.get(aTextGeneral, "drinks_always_drinkable", false).getBoolean(false);
		GregTech_API.sDoShowAllItemsInCreative = tMainConfig.get(aTextGeneral, "show_all_metaitems_in_creative_and_NEI", false).getBoolean(false);
		GregTech_API.sMultiThreadedSounds      = tMainConfig.get(aTextGeneral, "sound_multi_threading", false).getBoolean(false);
		
		String SBdye0 = "ColorModulation.";
		for (Dyes tDye : Dyes.values()) {
			if ((tDye != Dyes._NULL) && (tDye.mIndex < 0)) {
				String SBdye1 = new StringBuilder(18).append(SBdye0).append(tDye).toString();
				tDye.mRGBa[0] = ((short) Math.min(255, Math.max(0, GregTech_API.sClientDataFile.get(SBdye1, "R", tDye.mRGBa[0]))));
				tDye.mRGBa[1] = ((short) Math.min(255, Math.max(0, GregTech_API.sClientDataFile.get(SBdye1, "G", tDye.mRGBa[1]))));
				tDye.mRGBa[2] = ((short) Math.min(255, Math.max(0, GregTech_API.sClientDataFile.get(SBdye1, "B", tDye.mRGBa[2]))));
			}
		}
		gregtechproxy.mRenderTileAmbientOcclusion = GregTech_API.sClientDataFile.get("render", "TileAmbientOcclusion", true);
		gregtechproxy.mRenderGlowTextures         = GregTech_API.sClientDataFile.get("render", "GlowTextures", true);
		
		gregtechproxy.mMultiBlocksMaxProgressTimeBound       = tMainConfig.get(aTextGeneral, "mMultiBlocksMaxProgressTimeBound", 1).getInt(1);
		gregtechproxy.enableMultiBlocksTickBounds            = tMainConfig.get(aTextGeneral, "enableMultiBlocksTickBounds", false).getBoolean(false);
		gregtechproxy.mMaxEqualEntitiesAtOneSpot             = tMainConfig.get(aTextGeneral, "MaxEqualEntitiesAtOneSpot", 3).getInt(3);
		gregtechproxy.mFlintChance                           = tMainConfig.get(aTextGeneral, "FlintAndSteelChance", 30).getInt(30);
		gregtechproxy.mItemDespawnTime                       = tMainConfig.get(aTextGeneral, "ItemDespawnTime", 6000).getInt(6000);
		gregtechproxy.mNerfStorageBlocks                     = tMainConfig.get(aTextGeneral, "NerfStorageBlocks", true).getBoolean(true);
		gregtechproxy.mAllowSmallBoilerAutomation            = tMainConfig.get(aTextGeneral, "AllowSmallBoilerAutomation", false).getBoolean(false);
		gregtechproxy.mHardMachineCasings                    = tMainConfig.get(aTextGeneral, "HardMachineCasings", true).getBoolean(true);
		gregtechproxy.mDisableVanillaOres                    = tMainConfig.get(aTextGeneral, "DisableVanillaOres", true).getBoolean(true);
		gregtechproxy.mNerfDustCrafting                      = tMainConfig.get(aTextGeneral, "NerfDustCrafting", true).getBoolean(true);
		gregtechproxy.mIncreaseDungeonLoot                   = tMainConfig.get(aTextGeneral, "IncreaseDungeonLoot", true).getBoolean(true);
		gregtechproxy.mAxeWhenAdventure                      = tMainConfig.get(aTextGeneral, "AdventureModeStartingAxe", true).getBoolean(true);
		gregtechproxy.mHardcoreCables                        = tMainConfig.get(aTextGeneral, "HardCoreCableLoss", false).getBoolean(false);
		gregtechproxy.mSurvivalIntoAdventure                 = tMainConfig.get(aTextGeneral, "forceAdventureMode", false).getBoolean(false);
		gregtechproxy.mHungerEffect                          = tMainConfig.get(aTextGeneral, "AFK_Hunger", false).getBoolean(false);
		gregtechproxy.mHardRock                              = tMainConfig.get(aTextGeneral, "harderstone", false).getBoolean(false);
		gregtechproxy.mInventoryUnification                  = tMainConfig.get(aTextGeneral, "InventoryUnification", true).getBoolean(true);
		gregtechproxy.mGTBees                                = tMainConfig.get(aTextGeneral, "GTBees", true).getBoolean(true);
		gregtechproxy.mCraftingUnification                   = tMainConfig.get(aTextGeneral, "CraftingUnification", true).getBoolean(true);
		gregtechproxy.mNerfedWoodPlank                       = tMainConfig.get(aTextGeneral, "WoodNeedsSawForCrafting", true).getBoolean(true);
		gregtechproxy.mNerfedVanillaTools                    = tMainConfig.get(aTextGeneral, "smallerVanillaToolDurability", true).getBoolean(true);
		gregtechproxy.mSortToTheEnd                          = tMainConfig.get(aTextGeneral, "EnsureToBeLoadedLast", true).getBoolean(true);
		gregtechproxy.mDisableIC2Cables                      = tMainConfig.get(aTextGeneral, "DisableIC2Cables", true).getBoolean(true);
		gregtechproxy.mAchievements                          = tMainConfig.get(aTextGeneral, "EnableAchievements", true).getBoolean(true);
		gregtechproxy.mAE2Integration                        = GregTech_API.sSpecialFile.get(ConfigCategories.general, "EnableAE2Integration", GregTech_API.mAE2);
		gregtechproxy.mAE2Tunnel                             = GregTech_API.sSpecialFile.get(ConfigCategories.general, "EnableAE2Tunnel", false);
		gregtechproxy.mNerfedCombs                           = tMainConfig.get(aTextGeneral, "NerfCombs", true).getBoolean(true);
		gregtechproxy.mNerfedCrops                           = tMainConfig.get(aTextGeneral, "NerfCrops", true).getBoolean(true);
		gregtechproxy.mHideUnusedOres                        = tMainConfig.get(aTextGeneral, "HideUnusedOres", true).getBoolean(true);
		gregtechproxy.mHideRecyclingRecipes                  = tMainConfig.get(aTextGeneral, "HideRecyclingRecipes", true).getBoolean(true);
		gregtechproxy.mArcSmeltIntoAnnealed                  = tMainConfig.get(aTextGeneral, "ArcSmeltIntoAnnealedWrought", true).getBoolean(true);
		gregtechproxy.mEnableAllMaterials                    = tMainConfig.get("general", "EnableAllMaterials", false).getBoolean(false);
		gregtechproxy.mEnableAllComponents                   = tMainConfig.get("general", "EnableAllComponents", false).getBoolean(false);
		gregtechproxy.mEnableAllComponentsWithoutIntegration = tMainConfig.get("general", "mEnableAllComponentsWithoutIntegration", true, "No Touch!").getBoolean(true);
		gregtechproxy.mPollution                             = tMainConfig.get("Pollution", "EnablePollution", true).getBoolean(true);
		gregtechproxy.mPollutionSmogLimit                    = tMainConfig.get("Pollution", "SmogLimit", 500000).getInt(500000);
		gregtechproxy.mPollutionPoisonLimit                  = tMainConfig.get("Pollution", "PoisonLimit", 750000).getInt(750000);
		gregtechproxy.mPollutionVegetationLimit              = tMainConfig.get("Pollution", "VegetationLimit", 1000000).getInt(1000000);
		gregtechproxy.mPollutionSourRainLimit                = tMainConfig.get("Pollution", "SourRainLimit", 2000000).getInt(2000000);
		gregtechproxy.mExplosionItemDrop                     = tMainConfig.get("general", "ExplosionItemDrops", false).getBoolean(false);
		gregtechproxy.mAddGTRecipesToIC2Machines             = tMainConfig.get("general", "AddGTRecipesToIC2Machines", true).getBoolean(true);
		gregtechproxy.mUndergroundOil.getConfig(tMainConfig, "undergroundfluid");
		gregtechproxy.mEnableCleanroom           = tMainConfig.get("general", "EnableCleanroom", true).getBoolean(true);
		gregtechproxy.mLowGravProcessing         = Loader.isModLoaded(GT_Values.MOD_ID_GC_CORE) && tMainConfig.get("general", "LowGravProcessing", true).getBoolean(true);
		gregtechproxy.mLowGravProcessingCircuits = Loader.isModLoaded(GT_Values.MOD_ID_GC_CORE) && tMainConfig.get("general", "LowGravProcessingCircuits", false).getBoolean(false);
		gregtechproxy.mComponentAssembler        = tMainConfig.get("general", "ComponentAssembler", false).getBoolean(false);
		
		Calendar now = Calendar.getInstance();
		gregtechproxy.mAprilFool                            = GregTech_API.sSpecialFile.get(ConfigCategories.general, "AprilFool", now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) == 1);
		gregtechproxy.mCropNeedBlock                        = tMainConfig.get("general", "CropNeedBlockBelow", true).getBoolean(true);
		gregtechproxy.mDisableOldChemicalRecipes            = tMainConfig.get("general", "DisableOldChemicalRecipes", false).getBoolean(false);
		gregtechproxy.mMoreComplicatedChemicalRecipes       = tMainConfig.get("general", "MoreComplicatedChemicalRecipes", false).getBoolean(false);
		gregtechproxy.mAMHInteraction                       = tMainConfig.get("general", "AllowAutoMaintenanceHatchInteraction", false).getBoolean(false);
		GregTech_API.mOutputRF                              = GregTech_API.sOPStuff.get(ConfigCategories.general, "OutputRF", true);
		GregTech_API.mInputRF                               = GregTech_API.sOPStuff.get(ConfigCategories.general, "InputRF", false);
		GregTech_API.mEUtoRF                                = GregTech_API.sOPStuff.get(ConfigCategories.general, "100EUtoRF", 360);
		GregTech_API.mRFtoEU                                = GregTech_API.sOPStuff.get(ConfigCategories.general, "100RFtoEU", 20);
		GregTech_API.mRFExplosions                          = GregTech_API.sOPStuff.get(ConfigCategories.general, "RFExplosions", false);
		GregTech_API.meIOLoaded                             = Loader.isModLoaded("EnderIO");
		gregtechproxy.mForceFreeFace                        = GregTech_API.sMachineFile.get(ConfigCategories.machineconfig, "forceFreeFace", false);
		gregtechproxy.mEasierIVPlusCables                   = tMainConfig.get("general", "EasierIVPlusCables", false).getBoolean(false);
		gregtechproxy.mBrickedBlastFurnace                  = tMainConfig.get("general", "BrickedBlastFurnace", true).getBoolean(true);
		gregtechproxy.mMixedOreOnlyYieldsTwoThirdsOfPureOre = tMainConfig.get("general", "MixedOreOnlyYieldsTwoThirdsOfPureOre", false).getBoolean(false);
		gregtechproxy.enableBlackGraniteOres                = GregTech_API.sWorldgenFile.get("general", "enableBlackGraniteOres", gregtechproxy.enableBlackGraniteOres);
		gregtechproxy.enableRedGraniteOres                  = GregTech_API.sWorldgenFile.get("general", "enableRedGraniteOres", gregtechproxy.enableRedGraniteOres);
		gregtechproxy.enableMarbleOres                      = GregTech_API.sWorldgenFile.get("general", "enableMarbleOres", gregtechproxy.enableMarbleOres);
		gregtechproxy.enableBasaltOres                      = GregTech_API.sWorldgenFile.get("general", "enableBasaltOres", gregtechproxy.enableBasaltOres);
		gregtechproxy.enableUBOres                          = GregTech_API.sWorldgenFile.get("general", "enableUBOres", gregtechproxy.enableUBOres);
		gregtechproxy.gt6Pipe                               = tMainConfig.get("general", "GT6StyledPipesConnection", true).getBoolean(true);
		gregtechproxy.gt6Cable                              = tMainConfig.get("general", "GT6StyledWiresConnection", false).getBoolean(false);
		gregtechproxy.gt6Cable                              = tMainConfig.get("general", "GT6StyledWiresConnection", true).getBoolean(true);
		gregtechproxy.ic2EnergySourceCompat                 = tMainConfig.get("general", "Ic2EnergySourceCompat", true).getBoolean(true);
		gregtechproxy.costlyCableConnection                 = tMainConfig.get("general", "CableConnectionRequiresSolderingMaterial", false).getBoolean(false);
		gregtechproxy.mHardRadonRecipe                      = tMainConfig.get("general", "HardRadonRecipe", true).getBoolean(true);
		GT_LanguageManager.i18nPlaceholder                  = tMainConfig.get("general", "EnablePlaceholderForMaterialNamesInLangFile", true).getBoolean(true);
		
		GregTech_API.mUseOnlyGoodSolderingMaterials = GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.harderrecipes, "useonlygoodsolderingmaterials", GregTech_API.mUseOnlyGoodSolderingMaterials);
		
		gregtechproxy.mChangeHarvestLevels = GregTech_API.sMaterialProperties.get("havestLevel", "activateHarvestLevelChange", false);
		if (gregtechproxy.mChangeHarvestLevels) {
			gregtechproxy.mGraniteHavestLevel = GregTech_API.sMaterialProperties.get("havestLevel", "graniteHarvestLevel", 3);
			gregtechproxy.mMaxHarvestLevel    = Math.min(15, GregTech_API.sMaterialProperties.get("havestLevel", "maxLevel", 7));
			for (Materials tMaterial : Materials.values()) {
				if (tMaterial != null && tMaterial.mToolQuality > 0 && tMaterial.mMetaItemSubID < gregtechproxy.mHarvestLevel.length && tMaterial.mMetaItemSubID >= 0) {
					gregtechproxy.mHarvestLevel[tMaterial.mMetaItemSubID] = GregTech_API.sMaterialProperties.get("materialHavestLevel", tMaterial.mDefaultLocalName, tMaterial.mToolQuality);
				}
			}
		}
		
		if (tMainConfig.get("general", "hardermobspawners", true).getBoolean(true)) {
			Blocks.mob_spawner.setHardness(500.0F).setResistance(6000000.0F);
		}
		gregtechproxy.mOnline = tMainConfig.get(aTextGeneral, "online", true).getBoolean(false);
		
		gregtechproxy.mUpgradeCount = Math.min(64, Math.max(1, tMainConfig.get("features", "UpgradeStacksize", 4).getInt()));
		for (OrePrefixes tPrefix : OrePrefixes.values()) {
			if (tPrefix.mIsUsedForOreProcessing) {
				tPrefix.mDefaultStackSize = ((byte) Math.min(64, Math.max(1, tMainConfig.get("features", "MaxOreStackSize", 64).getInt())));
			} else if (tPrefix == OrePrefixes.plank) {
				tPrefix.mDefaultStackSize = ((byte) Math.min(64, Math.max(16, tMainConfig.get("features", "MaxPlankStackSize", 64).getInt())));
			} else if ((tPrefix == OrePrefixes.wood) || (tPrefix == OrePrefixes.treeLeaves) || (tPrefix == OrePrefixes.treeSapling) || (tPrefix == OrePrefixes.log)) {
				tPrefix.mDefaultStackSize = ((byte) Math.min(64, Math.max(16, tMainConfig.get("features", "MaxLogStackSize", 64).getInt())));
			} else if (tPrefix.mIsUsedForBlocks) {
				tPrefix.mDefaultStackSize = ((byte) Math.min(64, Math.max(16, tMainConfig.get("features", "MaxOtherBlockStackSize", 64).getInt())));
			}
		}
	}
	
	public void initLang(FMLPreInitializationEvent aEvent) {
		GT_Log.out.println("GT_Mod: Generating Lang-File");
		GT_LanguageManager.sEnglishFile = new Configuration(new File(aEvent.getModConfigurationDirectory().getParentFile(), "GregTech.lang"));
		GT_LanguageManager.sEnglishFile.load();
		for (Materials aMaterial : Materials.values()) {
			if (aMaterial != null)
				aMaterial.mLocalizedName = GT_LanguageManager.addStringLocalization("Material." + aMaterial.mName.toLowerCase(), aMaterial.mDefaultLocalName);
		}
	}
	
	public void initMineTweaker() {
		List<String> oreTags = new ArrayList<>();
		if (Loader.isModLoaded("MineTweaker3")) {
			File globalDir = new File("scripts");
			if (globalDir.exists()) {
				List<String> scripts = new ArrayList<>();
				for (File file : globalDir.listFiles()) {
					if (file.getName().endsWith(".zs")) {
						try (BufferedReader br = new BufferedReader(new FileReader(file))) {
							String line;
							while ((line = br.readLine()) != null) {
								scripts.add(line);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				String pattern1 = "<";
				String pattern2 = ">";
				
				Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2));
				for (String text : scripts) {
					Matcher m = p.matcher(text);
					while (m.find()) {
						String hit = m.group(1);
						if (hit.startsWith("ore:")) {
							hit = hit.substring(4);
							if (!oreTags.contains(hit)) oreTags.add(hit);
						} else if (hit.startsWith("gregtech:gt.metaitem.0")) {
							hit = hit.substring(22);
							int mIt = Integer.parseInt(hit.substring(0, 1));
							if (mIt > 0) {
								int meta = 0;
								try {
									hit  = hit.substring(2);
									meta = Integer.parseInt(hit);
								} catch (Exception e) {
									GT_FML_LOGGER.info("parseError: " + hit);
								}
								if (meta > 0 && meta < 32000) {
									int prefix = meta / 1000;
									int material = meta % 1000;
									String tag = "";
									String[] tags = new String[]{};
									if (mIt == 1) tags = new String[]{"dustTiny", "dustSmall", "dust", "dustImpure", "dustPure", "crushed", "crushedPurified", "crushedCentrifuged", "gem", "nugget", null, "ingot", "ingotHot", "ingotDouble", "ingotTriple", "ingotQuadruple", "ingotQuintuple", "plate", "plateDouble", "plateTriple", "plateQuadruple", "plateQuintuple", "plateDense", "stick", "lens", "round", "bolt", "screw", "ring", "foil", "cell", "cellPlasma"};
									if (mIt == 2) tags = new String[]{"toolHeadSword", "toolHeadPickaxe", "toolHeadShovel", "toolHeadAxe", "toolHeadHoe", "toolHeadHammer", "toolHeadFile", "toolHeadSaw", "toolHeadDrill", "toolHeadChainsaw", "toolHeadWrench", "toolHeadUniversalSpade", "toolHeadSense", "toolHeadPlow", null, "toolHeadBuzzSaw", "turbineBlade", null, "itemCasing", "wireFine", "gearGtSmall", "rotor", "stickLong", "springSmall", "spring", null, null, "gemChipped", "gemFlawed", "gemFlawless", "gemExquisite", "gearGt"};
									if (mIt == 3) tags = new String[]{"crateGtDust", "crateGtIngot", "crateGtGem", "crateGtPlate"};
									if (tags.length > prefix) tag = tags[prefix];
									if (GregTech_API.sGeneratedMaterials[material] != null) {
										tag += GregTech_API.sGeneratedMaterials[material].mName;
										if (!oreTags.contains(tag)) oreTags.add(tag);
									} else if (material > 0) {
										GT_FML_LOGGER.info("MaterialDisabled: " + material + " " + m.group(1));
									}
								}
							}
						}
					}
				}
			}
		}
		
		String[] preS = new String[]{"dustTiny", "dustSmall", "dust", "dustImpure", "dustPure", "crushed", "crushedPurified", "crushedCentrifuged", "gem", "nugget", "ingot", "ingotHot", "ingotDouble", "ingotTriple", "ingotQuadruple", "ingotQuintuple", "plate", "plateDouble", "plateTriple", "plateQuadruple", "plateQuintuple", "plateDense", "stick", "lens", "round", "bolt", "screw", "ring", "foil", "cell", "cellPlasma", "toolHeadSword", "toolHeadPickaxe", "toolHeadShovel", "toolHeadAxe", "toolHeadHoe", "toolHeadHammer", "toolHeadFile", "toolHeadSaw", "toolHeadDrill", "toolHeadChainsaw", "toolHeadWrench", "toolHeadUniversalSpade", "toolHeadSense", "toolHeadPlow", null, "toolHeadBuzzSaw", "turbineBlade", "wireFine", "gearGtSmall", "rotor", "stickLong", "springSmall", "spring", null, null, "gemChipped", "gemFlawed", "gemFlawless", "gemExquisite", "gearGt", "crateGtDust", "crateGtIngot", "crateGtGem", "crateGtPlate", "itemCasing"};
		
		List<String> mMTTags = new ArrayList<String>();
		for (String test : oreTags) {
			if (StringUtils.startsWithAny(test, preS)) {
				mMTTags.add(test);
				if (GT_Values.D1)
					GT_FML_LOGGER.info("oretag: " + test);
			}
		}
		
		GT_FML_LOGGER.info("reenableMetaItems");
		for (String reEnable : mMTTags) {
			OrePrefixes tPrefix = OrePrefixes.getOrePrefix(reEnable);
			if (tPrefix != null) {
				Materials tName = Materials.get(reEnable.replaceFirst(tPrefix.toString(), ""));
				if (tName != null) {
					tPrefix.mDisabledItems.remove(tName);
					tPrefix.mGeneratedItems.add(tName);
					if (tPrefix == OrePrefixes.screw) {
						OrePrefixes.bolt.mDisabledItems.remove(tName);
						OrePrefixes.bolt.mGeneratedItems.add(tName);
						OrePrefixes.stick.mDisabledItems.remove(tName);
						OrePrefixes.stick.mGeneratedItems.add(tName);
					}
					if (tPrefix == OrePrefixes.round) {
						OrePrefixes.nugget.mDisabledItems.remove(tName);
						OrePrefixes.nugget.mGeneratedItems.add(tName);
					}
					if (tPrefix == OrePrefixes.spring) {
						OrePrefixes.stickLong.mDisabledItems.remove(tName);
						OrePrefixes.stickLong.mGeneratedItems.add(tName);
						OrePrefixes.stick.mDisabledItems.remove(tName);
						OrePrefixes.stick.mGeneratedItems.add(tName);
					}
					if (tPrefix == OrePrefixes.springSmall) {
						OrePrefixes.stick.mDisabledItems.remove(tName);
						OrePrefixes.stick.mGeneratedItems.add(tName);
					}
					if (tPrefix == OrePrefixes.stickLong) {
						OrePrefixes.stick.mDisabledItems.remove(tName);
						OrePrefixes.stick.mGeneratedItems.add(tName);
					}
					if (tPrefix == OrePrefixes.rotor) {
						OrePrefixes.ring.mDisabledItems.remove(tName);
						OrePrefixes.ring.mGeneratedItems.add(tName);
					}
				} else {
					GT_FML_LOGGER.info("noMaterial " + reEnable);
				}
			} else {
				GT_FML_LOGGER.info("noPrefix " + reEnable);
			}
		}
	}
	
}
