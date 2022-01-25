package gregtech.loaders.oreprocessing;

import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.interfaces.IOreRecipeRegistrator;

import gregtech.api.objects.GT_StdRenderedTexture;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_RecipeRegistrator;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_Proxy;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static gregtech.api.enums.ConfigCategories.Recipes.harderrecipes;
import static gregtech.api.enums.GT_Values.*;
import static gregtech.api.util.GT_ModHandler.RecipeBits.BUFFERED;
import static gregtech.api.util.GT_ModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS;
import static gregtech.common.GT_Proxy.tBits;

public class ProcessingPlate implements IOreRecipeRegistrator {
	public ProcessingPlate() {
		OrePrefixes.plate.add(this);
		OrePrefixes.plateDouble.add(this);
		OrePrefixes.plateTriple.add(this);
		OrePrefixes.plateQuadruple.add(this);
		OrePrefixes.plateQuintuple.add(this);
		OrePrefixes.plateDense.add(this);
		OrePrefixes.plateAlloy.add(this);
		OrePrefixes.itemCasing.add(this);
	}
	
	public void registerOre(OrePrefixes aPrefix, Materials aMaterial, String aOreDictName, String aModName, ItemStack aStack) {
		boolean aNoSmashing = aMaterial.contains(SubTag.NO_SMASHING);
		boolean aNoWorking = aMaterial.contains(SubTag.NO_WORKING);
		long aMaterialMass = aMaterial.getMass();
		
		switch (aPrefix) {
			case plate:
				GT_ModHandler.removeRecipeByOutput(aStack);
				GT_ModHandler.removeRecipe(aStack);
				
				if (aMaterial.mStandardMoltenFluid != null) {
					if (!(aMaterial == Materials.AnnealedCopper || aMaterial == Materials.WroughtIron)) {
						RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Plate.get(0L), aMaterial.getMolten(144L), GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), 32, 8);
					}
				}
				ItemStack tStack = null;
				switch (aMaterial.mName) {
					case "Iron":
						tStack = new ItemStack(Blocks.iron_block, 1, 0);
						break;
					case "Gold":
						tStack = new ItemStack(Blocks.gold_block, 1, 0);
						break;
					case "Diamond":
						tStack = new ItemStack(Blocks.diamond_block, 1, 0);
						break;
					case "Emerald":
						tStack = new ItemStack(Blocks.emerald_block, 1, 0);
						break;
					case "Lapis":
						tStack = new ItemStack(Blocks.lapis_block, 1, 0);
						break;
					case "Coal":
						tStack = new ItemStack(Blocks.coal_block, 1, 0);
						break;
					case "Redstone":
						tStack = new ItemStack(Blocks.redstone_block, 1, 0);
						break;
					case "Glowstone":
						tStack = new ItemStack(Blocks.glowstone, 1, 0);
						break;
					case "NetherQuartz":
						tStack = new ItemStack(Blocks.quartz_block, 1, 0);
						break;
					case "Obsidian":
						tStack = new ItemStack(Blocks.obsidian, 1, 0);
						break;
					case "Stone":
						tStack = new ItemStack(Blocks.stone, 1, 0);
						break;
					case "GraniteBlack":
						GregTech_API.registerCover(aStack, TextureFactory.of(Textures.BlockIcons.GRANITE_BLACK_SMOOTH), null);
						break;
					case "GraniteRed":
						GregTech_API.registerCover(aStack, TextureFactory.of(Textures.BlockIcons.GRANITE_RED_SMOOTH), null);
						break;
					case "Basalt":
						GregTech_API.registerCover(aStack, TextureFactory.of(Textures.BlockIcons.BASALT_SMOOTH), null);
						break;
					case "Marble":
						GregTech_API.registerCover(aStack, TextureFactory.of(Textures.BlockIcons.MARBLE_SMOOTH), null);
						break;
					case "Concrete":
						GregTech_API.registerCover(aStack, TextureFactory.of(Textures.BlockIcons.CONCRETE_LIGHT_SMOOTH), null);
						break;
					default:
						GregTech_API.registerCover(aStack, TextureFactory.of(aMaterial.mIconSet.mTextures[71], aMaterial.mRGBa, false), null);
				}
				if (tStack != null)
					GregTech_API.registerCover(aStack, TextureFactory.of(Block.getBlockFromItem(tStack.getItem()), tStack.getItemDamage()), null);
				
				if (aMaterial.mFuelPower > 0)
					RA.addFuel(GT_Utility.copyAmount(1L, aStack), null, aMaterial.mFuelPower, aMaterial.mFuelType);
				GT_Utility.removeSimpleIC2MachineRecipe(GT_Utility.copyAmount(9L, aStack), GT_ModHandler.getCompressorRecipeList(), GT_OreDictUnificator.get(OrePrefixes.plateDense, aMaterial, 1L));
				//DISABLED, moved to 3ple plate//GT_Values.RA.addImplosionRecipe(GT_Utility.copyAmount(aMaterial == Materials.MeteoricIron ? 1 : 2, aStack), 2, GT_OreDictUnificator.get(OrePrefixes.compressed, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dustTiny, Materials.DarkAsh, 1L));
				GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.foil, aMaterial, 2L), tBits, new Object[]{"hX", 'X', OrePrefixes.plate.get(aMaterial)});
				
				if (aMaterial == Materials.Paper)
					GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(GregTech_API.sRecipeFile.get(harderrecipes, aStack, true) ? 2L : 3L, aStack), new Object[]{"XXX", 'X', new ItemStack(net.minecraft.init.Items.reeds, 1, 32767)});
				
				if ((aMaterial.mUnificatable) && (aMaterial.mMaterialInto == aMaterial)) {
					if (!aNoSmashing && GregTech_API.sRecipeFile.get(ConfigCategories.Tools.hammerplating, aMaterial.toString(), true)) {
						GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), tBits, new Object[]{"h", "X", "X", 'X', OrePrefixes.ingot.get(aMaterial)});
						GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), tBits, new Object[]{"H", "X", 'H', ToolDictNames.craftingToolForgeHammer, 'X', OrePrefixes.ingot.get(aMaterial)});
						GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), tBits, new Object[]{"h", "X", 'X', OrePrefixes.gem.get(aMaterial)});
						GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), tBits, new Object[]{"H", "X", 'H', ToolDictNames.craftingToolForgeHammer, 'X', OrePrefixes.gem.get(aMaterial)});
						//GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), GT_Proxy.tBits, new Object[]{"h", "X", 'X', OrePrefixes.ingotDouble.get(aMaterial)});
						//GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 2L), GT_Proxy.tBits, new Object[]{"H", "X", 'H', ToolDictNames.craftingToolForgeHammer, 'X', OrePrefixes.ingotDouble.get(aMaterial)});
					}
					if ((aMaterial.contains(SubTag.MORTAR_GRINDABLE)) && (GregTech_API.sRecipeFile.get(ConfigCategories.Tools.mortar, aMaterial.mName, true)))
						GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial, 1L), tBits, new Object[]{"X", "m", 'X', OrePrefixes.plate.get(aMaterial)});
				}
				break;
			case plateDouble:
				GT_ModHandler.removeRecipeByOutput(aStack);
				GregTech_API.registerCover(aStack, TextureFactory.of(aMaterial.mIconSet.mTextures[72], aMaterial.mRGBa, false), null);
				if (!aNoSmashing) {
					RA.addBenderRecipe(GT_Utility.copyAmount(2L, aStack), GT_OreDictUnificator.get(OrePrefixes.plateQuadruple, aMaterial, 1L), (int) Math.max(aMaterialMass * 2L, 1L), 96);
					if (GregTech_API.sRecipeFile.get(ConfigCategories.Tools.hammerdoubleplate, OrePrefixes.plate.get(aMaterial).toString(), true)) {
						Object aPlateStack = OrePrefixes.plate.get(aMaterial);
						GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(1L, aStack), DO_NOT_CHECK_FOR_COLLISIONS | BUFFERED, new Object[]{"I", "B", "h", 'I', aPlateStack, 'B', aPlateStack});
					}
					RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 2L), GT_Utility.copyAmount(1L, aStack), (int) Math.max(aMaterialMass * 2L, 1L), 96);
				} else {
					RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 2L), ItemList.Circuit_Integrated.getWithDamage(0L, 2L), Materials.Glue.getFluid(10L), GT_Utility.copyAmount(1L, aStack), 64, 8);
				}
				break;
			case plateTriple:
				GT_ModHandler.removeRecipeByOutput(aStack);
				GregTech_API.registerCover(aStack, TextureFactory.of(aMaterial.mIconSet.mTextures[73], aMaterial.mRGBa, false), null);
				if (!aNoSmashing) {
					RA.addBenderRecipe(GT_Utility.copyAmount(3L, aStack), GT_OreDictUnificator.get(OrePrefixes.plateDense, aMaterial, 1L), (int) Math.max(aMaterialMass * 3L, 1L), 96);
					if (GregTech_API.sRecipeFile.get(ConfigCategories.Tools.hammertripleplate, OrePrefixes.plate.get(aMaterial).toString(), true)) {
						Object aPlateStack = OrePrefixes.plate.get(aMaterial);
						GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(1L, aStack), DO_NOT_CHECK_FOR_COLLISIONS | BUFFERED, new Object[]{"I", "B", "h", 'I', OrePrefixes.plateDouble.get(aMaterial), 'B', aPlateStack});
					}
					RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 3L), GT_Utility.copyAmount(1L, aStack), (int) Math.max(aMaterialMass * 3L, 1L), 96);
				} else {
					RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 3L), ItemList.Circuit_Integrated.getWithDamage(0L, 3L), Materials.Glue.getFluid(20L), GT_Utility.copyAmount(1L, aStack), 96, 8);
				}
				RA.addImplosionRecipe(GT_Utility.copyAmount(1L, aStack), 2, GT_OreDictUnificator.get(OrePrefixes.compressed, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new int[]{10000, 1000});//added
				break;
			case plateQuadruple:
				GT_ModHandler.removeRecipeByOutput(aStack);
				GregTech_API.registerCover(aStack, TextureFactory.of(aMaterial.mIconSet.mTextures[74], aMaterial.mRGBa, false), null);
				if (!aNoWorking)
					RA.addCNCRecipe(GT_Utility.copyAmount(1L, aStack), GT_OreDictUnificator.get(OrePrefixes.gearGt, aMaterial, 1L), (int) Math.max(aMaterialMass * 2L, 1L), 30);
				if (!aNoSmashing) {
					if (GregTech_API.sRecipeFile.get(ConfigCategories.Tools.hammerquadrupleplate, OrePrefixes.plate.get(aMaterial).toString(), true)) {
						Object aPlateStack = OrePrefixes.plate.get(aMaterial);
						GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(1L, aStack), DO_NOT_CHECK_FOR_COLLISIONS | BUFFERED, new Object[]{"I", "B", "h", 'I', OrePrefixes.plateTriple.get(aMaterial), 'B', aPlateStack});
					}
					RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 4L), GT_Utility.copyAmount(1L, aStack), (int) Math.max(aMaterialMass * 4L, 1L), 96);
				} else {
					RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 4L), ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.Glue.getFluid(30L), GT_Utility.copyAmount(1L, aStack), 128, 8);
				}
				break;
			case plateQuintuple:
				GT_ModHandler.removeRecipeByOutput(aStack);
				GregTech_API.registerCover(aStack, TextureFactory.of(aMaterial.mIconSet.mTextures[75], aMaterial.mRGBa, false), null);
				if (!aNoSmashing) {
					if (GregTech_API.sRecipeFile.get(ConfigCategories.Tools.hammerquintupleplate, OrePrefixes.plate.get(aMaterial).toString(), true)) {
						Object aPlateStack = OrePrefixes.plate.get(aMaterial);
						GT_ModHandler.addCraftingRecipe(GT_Utility.copyAmount(1L, aStack), DO_NOT_CHECK_FOR_COLLISIONS | BUFFERED, new Object[]{"I", "B", "h", 'I', OrePrefixes.plateQuadruple.get(aMaterial), 'B', aPlateStack});
					}
					RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 5L), GT_Utility.copyAmount(1L, aStack), (int) Math.max(aMaterialMass * 5L, 1L), 96);
				} else {
					RA.addAssemblerRecipe(gregtech.api.util.GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 5L), ItemList.Circuit_Integrated.getWithDamage(0L, 5L), Materials.Glue.getFluid(40L), GT_Utility.copyAmount(1L, aStack), 160, 8);
				}
				break;
			case plateDense:
				GT_ModHandler.removeRecipeByOutput(aStack);
				GregTech_API.registerCover(aStack, TextureFactory.of(aMaterial.mIconSet.mTextures[76], aMaterial.mRGBa, false), null);
				if (!aNoSmashing) {
					RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 9L), GT_Utility.copyAmount(1L, aStack), (int) Math.max(aMaterialMass * 9L, 1L), 96);
				}
				break;
			case itemCasing:
				GT_ModHandler.removeRecipeByOutput(aStack);
				if (aMaterial.mStandardMoltenFluid != null) {
					RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Casing.get(0L), aMaterial.getMolten(72L), GT_OreDictUnificator.get(OrePrefixes.itemCasing, aMaterial, 1L), 16, 8);
				}
				if ((aMaterial.mUnificatable) && (aMaterial.mMaterialInto == aMaterial)) {
					if (!aNoSmashing && GregTech_API.sRecipeFile.get(ConfigCategories.Tools.hammerplating, aMaterial.toString(), true)) {
						GT_ModHandler.addCraftingRecipe(GT_OreDictUnificator.get(OrePrefixes.itemCasing, aMaterial, 1L), tBits, new Object[]{"h X", 'X', OrePrefixes.plate.get(aMaterial)});
					}
				}
				RA.addAlloySmelterRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, aMaterial, 2L), ItemList.Shape_Mold_Casing.get(0L), GT_Utility.copyAmount(3L, aStack), 128, 15);
				RA.addCutterRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.itemCasing, aMaterial, 2L), null, (int) Math.max(aMaterial.getMass(), 1L), 16);
				RA.addExtruderRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, aMaterial, 1L), ItemList.Shape_Extruder_Casing.get(0L), GT_OreDictUnificator.get(OrePrefixes.itemCasing, aMaterial, 2L), (int) Math.max(aMaterial.getMass(), 1L), 45);
				GT_RecipeRegistrator.registerReverseFluidSmelting(aStack, aMaterial, aPrefix.mMaterialAmount, null);
				break;
			
			case plateAlloy:
				switch (aOreDictName) {
					case "plateAlloyAdvanced":
						GT_ModHandler.addAlloySmelterRecipe(GT_Utility.copyAmount(1L, aStack), new ItemStack(Blocks.glass, 3, 32767), GT_ModHandler.getIC2Item("reinforcedGlass", 4L), 400, 4, false);
						GT_ModHandler.addAlloySmelterRecipe(GT_Utility.copyAmount(1L, aStack), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 3L), GT_ModHandler.getIC2Item("reinforcedGlass", 4L), 400, 4, false);
					case "plateAlloyIridium":
						GT_ModHandler.removeRecipeByOutput(aStack);
					case "plateIron":
					case "plateCopper":
					case "plateTin":
					case "plateBronze":
					case "plateGold":
					case "plateSteel ":
					case "plateLead":
					case "plateAluminium":
					case "plateStainlessSteel":
					case "plateTitanium":
					case "plateTungsten":
					case "plateTungstenSteel":
					case "plateIridium":
					case "plateChrome":
					case "plateOsmium":
					case "plateNeutronium":
						GT_ModHandler.removeRecipeByOutput(aStack);
				}
				break;
			default:
				break;
		}
	}
}
