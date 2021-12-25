package gregtech.loaders.oreprocessing;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;

public class ProcessingDirty implements gregtech.api.interfaces.IOreRecipeRegistrator {
	public ProcessingDirty() {
		OrePrefixes.clump.add(this);
		OrePrefixes.shard.add(this);
		OrePrefixes.crushed.add(this);
		OrePrefixes.dirtyGravel.add(this);
	}
	
	public void registerOre(OrePrefixes aPrefix, Materials aMaterial, String aOreDictName, String aModName, net.minecraft.item.ItemStack aStack) {
		GT_Values.RA.addForgeHammerRecipe(GT_Utility.copyAmount(1L, aStack), GT_OreDictUnificator.get(OrePrefixes.dustImpure, aMaterial.mMacerateInto, 1L), 10, 16);
		GT_ModHandler.addPulverisationRecipe(GT_Utility.copyAmount(1L, aStack), GT_OreDictUnificator.get(OrePrefixes.dustImpure, aMaterial.mMacerateInto, GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial.mMacerateInto, 1L), 1L), GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1L), 10, false);
		GT_Values.RA.addOreWasherRecipe(GT_Utility.copyAmount(1L, aStack), new ItemStack[]{GT_OreDictUnificator.get(aPrefix == OrePrefixes.crushed ? OrePrefixes.crushedPurified : OrePrefixes.dustPure, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L)}, Materials.Water.getFluid(1000L), new int[]{10000, 1000, 10000}, 500, 16);
		GT_Values.RA.addThermalCentrifugeRecipe(GT_Utility.copyAmount(1L, aStack), new ItemStack[]{GT_OreDictUnificator.get(aPrefix == OrePrefixes.crushed ? OrePrefixes.crushedCentrifuged : OrePrefixes.dust, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L)}, new int[]{10000, 1000, 10000}, (int) Math.min(5000L, Math.abs(aMaterial.getMass() * 20L)), 48);
		
		chemicalBath(aPrefix, aMaterial, aStack, aMaterial);
		for (Materials tMaterial : aMaterial.mOreByProducts) {
			chemicalBath(aPrefix, aMaterial, aStack, tMaterial);
		}
		
		//Flotation Unit
		GT_Values.RA.addFlotationUnitRecipe(
				GT_Utility.copyAmount(1, aStack), Materials.Water.getFluid(1000L),
				Materials.SluiceJuice.getFluid(400L),
				GT_OreDictUnificator.get(OrePrefixes.dustPure, aMaterial.mMacerateInto, 2),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1),
				null, new int[]{10000, 2500, 2000, 1000, 10000}, 150, 480
		);
		GT_Values.RA.addFlotationUnitRecipe(
				GT_Utility.copyAmount(1, aStack), Materials.Creosote.getFluid(1600L),
				Materials.SluiceJuice.getFluid(1000L),
				GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial.mMacerateInto, 3),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(2, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1),
				new int[]{10000, 10000, 5000, 2500, 2000, 10000}, 300, 1024
		);
		GT_Values.RA.addFlotationUnitRecipe(
				GT_Utility.copyAmount(1, aStack), Materials.OilHeavy.getFluid(1600L),
				Materials.SluiceJuice.getFluid(1000L),
				GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial.mMacerateInto, 3),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(2, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1),
				new int[]{10000, 10000, 5000, 2500, 2000, 10000}, 360, 1024
		);
		
		GT_Values.RA.addFlotationUnitRecipe(
				GT_Utility.copyAmount(1, aStack), Materials.NitricAcid.getFluid(3000L),
				Materials.SluiceJuice.getFluid(2000L),
				GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial.mMacerateInto, 4),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 2),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(0, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(2, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
				GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1),
				new int[]{10000, 10000, 10000, 8000, 5000, 10000}, 440, 4096
		);
		
		if (aMaterial.contains(SubTag.WASHING_MERCURY))
			GT_Values.RA.addFlotationUnitRecipe(
					GT_Utility.copyAmount(1, aStack), Materials.Mercury.getFluid(1200L), Materials.SluiceJuice.getFluid(500L),
					GT_OreDictUnificator.get(OrePrefixes.dustPure, aMaterial.mMacerateInto, 4L),
					GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial.mMacerateInto, 2L),
					GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 2L),
					GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(2, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 2L),
					GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L),
					null, new int[]{10000, 8000, 2000, 1000, 10000}, 200, 480
			);
		flotationSodiumPersulfate(aMaterial, aStack, aMaterial);
		for (Materials tMaterial : aMaterial.mOreByProducts) {
			if (tMaterial.contains(SubTag.WASHING_MERCURY))
				GT_Values.RA.addFlotationUnitRecipe(
						GT_Utility.copyAmount(1, aStack), Materials.Mercury.getFluid(1200L),
						Materials.SluiceJuice.getFluid(500L),
						GT_OreDictUnificator.get(OrePrefixes.dustPure, aMaterial.mMacerateInto, 2),
						GT_OreDictUnificator.get(OrePrefixes.dust, tMaterial.mMacerateInto, 1),
						GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
						GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(2, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
						GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1),
						null, new int[]{10000, 8000, 2000, 1000, 10000}, 200, 480
				);
			flotationSodiumPersulfate(aMaterial, aStack, tMaterial);
		}
	}
	
	private void flotationSodiumPersulfate(Materials aMaterial, ItemStack aStack, Materials tMaterial) {
		if (tMaterial.contains(SubTag.WASHING_SODIUMPERSULFATE))
			GT_Values.RA.addFlotationUnitRecipe(
					GT_Utility.copyAmount(1, aStack), Materials.SodiumPersulfate.getFluid(1200L),
					Materials.SluiceJuice.getFluid(500L),
					GT_OreDictUnificator.get(OrePrefixes.dustPure, aMaterial.mMacerateInto, 2),
					GT_OreDictUnificator.get(OrePrefixes.dust, tMaterial.mMacerateInto, 1),
					GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
					GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(2, aMaterial.mMacerateInto, aMaterial.mOreByProducts), 1),
					GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1),
					null, new int[]{10000, 8000, 2000, 1000, 10000}, 200, 480
			);
	}
	
	private void chemicalBath(OrePrefixes aPrefix, Materials aMaterial, ItemStack aStack, Materials tMaterial) {
		if (tMaterial.contains(SubTag.WASHING_MERCURY))
			GT_Values.RA.addChemicalBathRecipe(GT_Utility.copyAmount(1L, aStack), Materials.Mercury.getFluid(1000L), GT_OreDictUnificator.get(aPrefix == OrePrefixes.crushed ? OrePrefixes.crushedPurified : OrePrefixes.dustPure, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, tMaterial.mMacerateInto, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L), new int[]{10000, 7000, 4000}, 800, 8);
		if (tMaterial.contains(SubTag.WASHING_SODIUMPERSULFATE))
			GT_Values.RA.addChemicalBathRecipe(GT_Utility.copyAmount(1L, aStack), Materials.SodiumPersulfate.getFluid(1000L), GT_OreDictUnificator.get(aPrefix == OrePrefixes.crushed ? OrePrefixes.crushedPurified : OrePrefixes.dustPure, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, tMaterial.mMacerateInto, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L), new int[]{10000, 7000, 4000}, 800, 8);
	}
	
}