package gregtech.loaders.postload;

import codechicken.nei.api.API;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GT_Log;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_DummyWorld;
import gregtech.common.items.GT_MetaGenerated_Item_03;
import ic2.api.recipe.ILiquidHeatExchangerManager.HeatExchangeProperty;
import ic2.api.recipe.Recipes;
import ic2.core.Ic2Items;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static gregtech.api.enums.GT_Values.RA;

public class GT_MachineRecipeLoader implements Runnable {
    private final static String aTextAE = "appliedenergistics2";
    private final static String aTextAEMM = "item.ItemMultiMaterial";
    private final static String aTextForestry = "Forestry";
    private final static String aTextEBXL = "ExtrabiomesXL";
    private final static String aTextTCGTPage = "gt.research.page.1.";
    private final static Boolean isNEILoaded = Loader.isModLoaded("NotEnoughItems");
    private final MaterialStack[][] mAlloySmelterList = {
            {new MaterialStack(Materials.Tetrahedrite, 3L), new MaterialStack(Materials.Tin, 1L), new MaterialStack(Materials.Bronze, 3L)},
            {new MaterialStack(Materials.Tetrahedrite, 3L), new MaterialStack(Materials.Zinc, 1L), new MaterialStack(Materials.Brass, 3L)},
            {new MaterialStack(Materials.Copper, 3L), new MaterialStack(Materials.Tin, 1L), new MaterialStack(Materials.Bronze, 4L)},
            {new MaterialStack(Materials.Copper, 3L), new MaterialStack(Materials.Zinc, 1L), new MaterialStack(Materials.Brass, 4L)},
            {new MaterialStack(Materials.Copper, 1L), new MaterialStack(Materials.Nickel, 1L), new MaterialStack(Materials.Cupronickel, 2L)},
            {new MaterialStack(Materials.Copper, 1L), new MaterialStack(Materials.Redstone, 4L), new MaterialStack(Materials.RedAlloy, 1L)},
            {new MaterialStack(Materials.AnnealedCopper, 3L), new MaterialStack(Materials.Tin, 1L), new MaterialStack(Materials.Bronze, 4L)},
            {new MaterialStack(Materials.AnnealedCopper, 3L), new MaterialStack(Materials.Zinc, 1L), new MaterialStack(Materials.Brass, 4L)},
            {new MaterialStack(Materials.AnnealedCopper, 1L), new MaterialStack(Materials.Nickel, 1L), new MaterialStack(Materials.Cupronickel, 2L)},
            {new MaterialStack(Materials.AnnealedCopper, 1L), new MaterialStack(Materials.Redstone, 4L), new MaterialStack(Materials.RedAlloy, 1L)},
            {new MaterialStack(Materials.Iron, 1L), new MaterialStack(Materials.Tin, 1L), new MaterialStack(Materials.TinAlloy, 2L)},
            {new MaterialStack(Materials.WroughtIron, 1L), new MaterialStack(Materials.Tin, 1L), new MaterialStack(Materials.TinAlloy, 2L)},
            {new MaterialStack(Materials.Iron, 2L), new MaterialStack(Materials.Nickel, 1L), new MaterialStack(Materials.Invar, 3L)},
            {new MaterialStack(Materials.WroughtIron, 2L), new MaterialStack(Materials.Nickel, 1L), new MaterialStack(Materials.Invar, 3L)},
            {new MaterialStack(Materials.Lead, 4L), new MaterialStack(Materials.Antimony, 1L), new MaterialStack(Materials.BatteryAlloy, 5L)},
            {new MaterialStack(Materials.Gold, 1L), new MaterialStack(Materials.Silver, 1L), new MaterialStack(Materials.Electrum, 2L)},
            {new MaterialStack(Materials.Magnesium, 1L), new MaterialStack(Materials.Aluminium, 2L), new MaterialStack(Materials.Magnalium, 3L)},
            {new MaterialStack(Materials.Silver, 1L), new MaterialStack(Materials.Nikolite, 4L), new MaterialStack(Materials.BlueAlloy, 1L)},
            {new MaterialStack(Materials.Boron, 1L), new MaterialStack(Materials.Glass, 7L), new MaterialStack(Materials.BorosilicateGlass, 8L)}};

    /**
     * Adds new recipes for hatches and busses
     */
    public static void addBusAndHatchRecipes() {
        Materials[] glues = {
                Materials.Glue,
                Materials.Plastic,
                Materials.Polytetrafluoroethylene,
                Materials.Polybenzimidazole
        };

        ItemStack[] chests = {
                new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 0) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 1) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 2) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 3) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 4) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 5) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 6) : new ItemStack(Blocks.chest),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 7) : GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1, 145),
                Loader.isModLoaded("chestup") ? GT_ModHandler.getModItem("chestup", "Blockchestup", 1, 8) : GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1, 146)
        };
        ItemStack[] tanks = {
                GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Empty, 1L),
                ItemList.Large_Fluid_Cell_Steel.get(1L),
                ItemList.Large_Fluid_Cell_Aluminium.get(1L),
                ItemList.Large_Fluid_Cell_StainlessSteel.get(1L),
                ItemList.Large_Fluid_Cell_Titanium.get(1L),
                ItemList.Large_Fluid_Cell_TungstenSteel.get(1L),
                ItemList.Large_Fluid_Cell_Chrome.get(1L),
                ItemList.Large_Fluid_Cell_Iridium.get(1L),
                GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1, 140),
                GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1, 141)
        };

        ItemStack[][] aInputs = new ItemStack[10][3];
        ItemStack[][] aInputs2 = new ItemStack[10][3];
        ItemStack[][] flInputs = new ItemStack[10][3];
        ItemStack[][] flInputs2 = new ItemStack[10][3];

        for (int i = 0; i < 10; i++) {
            aInputs[i] = new ItemStack[]{ItemList.CASINGS[i].get(1), chests[i].copy(), GT_Utility.getIntegratedCircuit(1)};
            aInputs2[i] = new ItemStack[]{ItemList.CASINGS[i].get(1), chests[i].copy(), GT_Utility.getIntegratedCircuit(2)};
            flInputs[i] = new ItemStack[]{ItemList.CASINGS[i].get(1), tanks[i].copy(), GT_Utility.getIntegratedCircuit(1)};
            flInputs2[i] = new ItemStack[]{ItemList.CASINGS[i].get(1), tanks[i].copy(), GT_Utility.getIntegratedCircuit(2)};
        }

        for (int aTier = 0; aTier < 10; aTier++) {

            if (aTier < 2) {
                RA.addAssemblerRecipe(aInputs[aTier], glues[0].getFluid((long) (144 * Math.pow((aTier + 4), aTier))), ItemList.HATCHES_INPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(aInputs2[aTier], glues[0].getFluid((long) (144 * Math.pow((aTier + 4), aTier))), ItemList.HATCHES_OUTPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(flInputs[aTier], glues[0].getFluid((long) (144 * Math.pow((aTier + 4), aTier))), ItemList.HATCHES_INPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(flInputs2[aTier], glues[0].getFluid((long) (144 * Math.pow((aTier + 4), aTier))), ItemList.HATCHES_OUTPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
            }
            if (aTier < 4) {
                RA.addAssemblerRecipe(aInputs[aTier], aTier == 0 ? glues[1].getMolten(72L) : glues[1].getMolten(144L * aTier), ItemList.HATCHES_INPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(aInputs2[aTier], aTier == 0 ? glues[1].getMolten(72L) : glues[1].getMolten(144L * aTier), ItemList.HATCHES_OUTPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(flInputs[aTier], aTier == 0 ? glues[1].getMolten(72L) : glues[1].getMolten(144L * aTier), ItemList.HATCHES_INPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(flInputs2[aTier], aTier == 0 ? glues[1].getMolten(72L) : glues[1].getMolten(144L * aTier), ItemList.HATCHES_OUTPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);

            }
            if (aTier < 7) {
                RA.addAssemblerRecipe(aInputs[aTier], glues[2].getMolten((long) (18 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_INPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(aInputs2[aTier], glues[2].getMolten((long) (18 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_OUTPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(flInputs[aTier], glues[2].getMolten((long) (18 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_INPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
                RA.addAssemblerRecipe(flInputs2[aTier], glues[2].getMolten((long) (18 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_OUTPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);

            }
            RA.addAssemblerRecipe(aInputs[aTier], glues[3].getMolten((long) (2.25 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_INPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
            RA.addAssemblerRecipe(aInputs2[aTier], glues[3].getMolten((long) (2.25 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_OUTPUT_BUS[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
            RA.addAssemblerRecipe(flInputs[aTier], glues[3].getMolten((long) (2.25 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_INPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
            RA.addAssemblerRecipe(flInputs2[aTier], glues[3].getMolten((long) (2.25 * Math.pow(2, (aTier + 1)))), ItemList.HATCHES_OUTPUT[aTier].get(1L), 480, (int) (30 * Math.pow(4, (aTier - 1))), false);
        }
    }

    @Override
    public void run() {
        GT_Log.out.println("GT_Mod: Adding non-OreDict Machine Recipes.");

        try {
            GT_Utility.removeSimpleIC2MachineRecipe(GT_Values.NI, ic2.api.recipe.Recipes.metalformerExtruding.getRecipes(), ItemList.Cell_Empty.get(3L));
            GT_Utility.removeSimpleIC2MachineRecipe(ItemList.IC2_Energium_Dust.get(1L), ic2.api.recipe.Recipes.compressor.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(new ItemStack(Items.gunpowder), ic2.api.recipe.Recipes.extractor.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(new ItemStack(Blocks.wool, 1, 32767), ic2.api.recipe.Recipes.extractor.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(new ItemStack(Blocks.gravel), ic2.api.recipe.Recipes.oreWashing.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(GT_ModHandler.getModItem("IC2", "reactorMOXSimpledepleted", 1L), ic2.api.recipe.Recipes.centrifuge.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(GT_ModHandler.getModItem("IC2", "reactorMOXDualdepleted", 1L), ic2.api.recipe.Recipes.centrifuge.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(GT_ModHandler.getModItem("IC2", "reactorMOXQuaddepleted", 1L), ic2.api.recipe.Recipes.centrifuge.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(GT_ModHandler.getModItem("IC2", "reactorUraniumSimpledepleted", 1L), ic2.api.recipe.Recipes.centrifuge.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(GT_ModHandler.getModItem("IC2", "reactorUraniumDualdepleted", 1L), ic2.api.recipe.Recipes.centrifuge.getRecipes(), GT_Values.NI);
            GT_Utility.removeSimpleIC2MachineRecipe(GT_ModHandler.getModItem("IC2", "reactorUraniumQuaddepleted", 1L), ic2.api.recipe.Recipes.centrifuge.getRecipes(), GT_Values.NI);
        } catch (Throwable e) {
        }
        GT_Utility.removeIC2BottleRecipe(GT_ModHandler.getIC2Item("fuelRod", 1), GT_ModHandler.getIC2Item("UranFuel", 1), ic2.api.recipe.Recipes.cannerBottle.getRecipes(), GT_ModHandler.getIC2Item("reactorUraniumSimple", 1, 1));
        GT_Utility.removeIC2BottleRecipe(GT_ModHandler.getIC2Item("fuelRod", 1), GT_ModHandler.getIC2Item("MOXFuel", 1), ic2.api.recipe.Recipes.cannerBottle.getRecipes(), GT_ModHandler.getIC2Item("reactorMOXSimple", 1, 1));

        RA.addFluidExtractionRecipe(new ItemStack(Items.wheat_seeds, 1, 32767), GT_Values.NI, Materials.SeedOil.getFluid(10), 10000, 32, 2);
        RA.addFluidExtractionRecipe(new ItemStack(Items.melon_seeds, 1, 32767), GT_Values.NI, Materials.SeedOil.getFluid(10), 10000, 32, 2);
        RA.addFluidExtractionRecipe(new ItemStack(Items.pumpkin_seeds, 1, 32767), GT_Values.NI, Materials.SeedOil.getFluid(10), 10000, 32, 2);
        RA.addFluidExtractionRecipe(ItemList.Crop_Drop_Rape.get(1), null, Materials.SeedOil.getFluid(50), 10000, 32, 2);
        try {
            GT_DummyWorld tWorld = (GT_DummyWorld) GT_Values.DW;
            while (tWorld.mRandom.mIterationStep > 0) {
                RA.addFluidExtractionRecipe(GT_Utility.copyAmount(1L, new Object[]{ForgeHooks.getGrassSeed(tWorld)}), GT_Values.NI, Materials.SeedOil.getFluid(10), 10000, 64, 2);
            }
        } catch (Throwable e) {
            GT_Log.out.println("GT_Mod: failed to iterate somehow, maybe it's your Forge Version causing it. But it's not that important\n");
            e.printStackTrace(GT_Log.err);
        }

//        RA.addArcFurnaceRecipe(ItemList.Block_BronzePlate.get(1), new ItemStack[]{ GT_OreDictUnificator.get(OrePrefixes.ingot,Materials.Bronze,4), GT_OreDictUnificator.get(OrePrefixes.dust,Materials.Stone,1)}, null, 160, 96);
//        RA.addArcFurnaceRecipe(ItemList.Block_IridiumTungstensteel.get(1), new ItemStack[]{ GT_OreDictUnificator.get(OrePrefixes.ingot,Materials.Bronze,4), GT_OreDictUnificator.get(OrePrefixes.dust,Materials.Stone,1)}, null, 160, 96);
        RA.addArcFurnaceRecipe(ItemList.Block_TungstenSteelReinforced.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenSteel, 2), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Concrete, 1)}, null, 160, 96);

        //Temporary until circuit overhaul
//        RA.addAlloySmelterRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Rubber, 2), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Copper, 1), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 1), 100, 18);

        RA.addPrinterRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Paper, 1L), FluidRegistry.getFluidStack("squidink", 36), GT_Values.NI, ItemList.Paper_Punch_Card_Empty.get(1L), 100, 2);
        RA.addPrinterRecipe(ItemList.Paper_Punch_Card_Empty.get(1L), FluidRegistry.getFluidStack("squidink", 36), ItemList.Tool_DataStick.getWithName(0L, "With Punch Card Data"), ItemList.Paper_Punch_Card_Encoded.get(1L), 100, 2);
        RA.addPrinterRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Paper, 3L), FluidRegistry.getFluidStack("squidink", 144), ItemList.Tool_DataStick.getWithName(0L, "With Scanned Book Data"), ItemList.Paper_Printed_Pages.get(1L), 400, 2);
        RA.addPrinterRecipe(new ItemStack(Items.map, 1, 32767), FluidRegistry.getFluidStack("squidink", 144), ItemList.Tool_DataStick.getWithName(0L, "With Scanned Map Data"), new ItemStack(Items.filled_map, 1, 0), 400, 2);
        RA.addPrinterRecipe(new ItemStack(Items.book, 1, 32767), FluidRegistry.getFluidStack("squidink", 144), GT_Values.NI, GT_Utility.getWrittenBook("Manual_Printer", ItemList.Book_Written_01.get(1L)), 400, 2);
        for (OrePrefixes tPrefix : Arrays.asList(new OrePrefixes[]{OrePrefixes.dust})) {
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Tin, 6L), GT_OreDictUnificator.get(tPrefix, Materials.Lead, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Antimony, 1L), GT_Utility.getIntegratedCircuit(3), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.SolderingAlloy, 10L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.EnderPearl, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Blaze, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.EnderEye, 1L * tPrefix.mMaterialAmount), (int) (100L * tPrefix.mMaterialAmount / 3628800L), 75);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Gold, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Silver, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Electrum, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Iron, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Invar, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Iron, 4L), GT_OreDictUnificator.get(tPrefix, Materials.Invar, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Manganese, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Chrome, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.StainlessSteel, 9L * tPrefix.mMaterialAmount), (int) (900L * tPrefix.mMaterialAmount / 3628800L), 120);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Iron, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Aluminium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Chrome, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Kanthal, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 120);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Copper, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Zinc, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Brass, 4L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Copper, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Tin, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Bronze, 4L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Copper, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Cupronickel, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 24);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Copper, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Gold, 4L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(3), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.RoseGold, 5L * tPrefix.mMaterialAmount), (int) (500L * tPrefix.mMaterialAmount / 3628800L), 120);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Zinc, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Iron, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.FerriteMixture, 6L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 120);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Copper, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Electrum, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.BlackBronze, 5L * tPrefix.mMaterialAmount), (int) (500L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Bismuth, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Brass, 4L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.BismuthBronze, 5L * tPrefix.mMaterialAmount), (int) (500L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.BlackBronze, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Steel, 3L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.BlackSteel, 5L * tPrefix.mMaterialAmount), (int) (500L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Silver, 1L), GT_OreDictUnificator.get(tPrefix, Materials.BismuthBronze, 1L), GT_OreDictUnificator.get(tPrefix, Materials.BlackSteel, 4L), GT_OreDictUnificator.get(tPrefix, Materials.Steel, 2L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.RedSteel, 8L * tPrefix.mMaterialAmount), (int) (800L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.RoseGold, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Brass, 1L), GT_OreDictUnificator.get(tPrefix, Materials.BlackSteel, 4L), GT_OreDictUnificator.get(tPrefix, Materials.Steel, 2L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.BlueSteel, 8L * tPrefix.mMaterialAmount), (int) (800L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Cobalt, 5L), GT_OreDictUnificator.get(tPrefix, Materials.Chrome, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Molybdenum, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Ultimet, 9L * tPrefix.mMaterialAmount), (int) (900L * tPrefix.mMaterialAmount / 3628800L), 500);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Brass, 7L), GT_OreDictUnificator.get(tPrefix, Materials.Aluminium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Cobalt, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.CobaltBrass, 9L * tPrefix.mMaterialAmount), (int) (900L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Saltpeter, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Sulfur, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Coal, 3L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Gunpowder, 6L * tPrefix.mMaterialAmount), (int) (600L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Saltpeter, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Sulfur, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Charcoal, 3L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Gunpowder, 6L * tPrefix.mMaterialAmount), (int) (600L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Saltpeter, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Sulfur, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Carbon, 3L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Gunpowder, 6L * tPrefix.mMaterialAmount), (int) (600L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Sulfur, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Sodium, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.SodiumSulfide, 3L * tPrefix.mMaterialAmount), (int) (500L * tPrefix.mMaterialAmount / 3628800L), 24);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Gallium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Arsenic, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.GalliumArsenide, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.MeteoricIron, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Steel, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.MeteoricSteel, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 480);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Indium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Gallium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Phosphor, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.IndiumGalliumPhosphide, 3L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Aluminium, 5L), GT_OreDictUnificator.get(tPrefix, Materials.Magnesium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Manganese, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Birmabright, 7L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Titanium, 40L), GT_OreDictUnificator.get(tPrefix, Materials.Aluminium, 6L), GT_OreDictUnificator.get(tPrefix, Materials.Vanadium, 5L), GT_OreDictUnificator.get(tPrefix, Materials.Iron, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Carbon, 1L), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.BT6, 55L * tPrefix.mMaterialAmount), (int) (5500L * tPrefix.mMaterialAmount / 3628800L), 120);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 4L), GT_OreDictUnificator.get(tPrefix, Materials.Chrome, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Nichrome, 5L * tPrefix.mMaterialAmount), (int) (500L * tPrefix.mMaterialAmount / 3628800L), 120);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Osmium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Iridium, 3L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Osmiridium, 4L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 30720);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Niobium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Titanium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.NiobiumTitanium, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 7680);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Vanadium, 3L), GT_OreDictUnificator.get(tPrefix, Materials.Gallium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.VanadiumGallium, 4L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 1920);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Tungsten, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Carbon, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.TungstenCarbide, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 1920);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Tungsten, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Steel, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.TungstenSteel, 2L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 1920);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.TungstenSteel, 5L), GT_OreDictUnificator.get(tPrefix, Materials.Chrome, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Molybdenum, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Vanadium, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.HSSG, 9L * tPrefix.mMaterialAmount), (int) (600L * tPrefix.mMaterialAmount / 3628800L), 1920);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.HSSG, 6L), GT_OreDictUnificator.get(tPrefix, Materials.Cobalt, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Manganese, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Silicon, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.HSSE, 9L * tPrefix.mMaterialAmount), (int) (700L * tPrefix.mMaterialAmount / 3628800L), 4100);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Steel, 7L), GT_OreDictUnificator.get(tPrefix, Materials.Vanadium, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Chrome, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.VanadiumSteel, 9L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 480);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Nickel, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Zinc, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Iron, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.FerriteMixture, 6L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Boron, 1L), GT_OreDictUnificator.get(tPrefix, Materials.Glass, 7L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.BorosilicateGlass, 8L * tPrefix.mMaterialAmount), (int) (200L * tPrefix.mMaterialAmount / 3628800L), 6);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.HSSG, 6L), GT_OreDictUnificator.get(tPrefix, Materials.Iridium, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Osmium, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.HSSS, 9L * tPrefix.mMaterialAmount), (int) (900L * tPrefix.mMaterialAmount / 3628800L), 7680);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Europium, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(3000L), GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Europiumoxide, 5L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 30720);

            RA.addMixerRecipe(GT_OreDictUnificator.get(tPrefix, Materials.Americium, 2L), GT_OreDictUnificator.get(tPrefix, Materials.Titanium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Diamericiumtitanium, 3L * tPrefix.mMaterialAmount), (int) (400L * tPrefix.mMaterialAmount / 3628800L), 122880);

            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Yttrium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Barium, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 3L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(7000), GT_Values.NF, GT_OreDictUnificator.getDust(Materials.YttriumBariumCuprate, 13L * OrePrefixes.dust.mMaterialAmount), (int) (800L * OrePrefixes.dust.mMaterialAmount / 3628800L), 2000);

            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 4L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.RedAlloy, 1L * tPrefix.mMaterialAmount), (int) (100L * tPrefix.mMaterialAmount / 3628800L), 6);

            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silicon, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.RedstoneAlloy, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RedstoneAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silver, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.ConductiveIron, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.ConductiveIron, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BlackSteel, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.EnergeticAlloy, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnergeticAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderEye, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chrome, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.VibrantAlloy, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Steel, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silicon, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.ElectricalSteel, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderPearl, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RedstoneAlloy, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.PulsatingIron, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(new ItemStack(Blocks.soul_sand, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.Soularium, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.ElectricalSteel, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Obsidian, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.getDust(Materials.DarkSteel, 3L * tPrefix.mMaterialAmount), (int) (300L * tPrefix.mMaterialAmount / 3628800L), 6);
        }
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 3L), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Water.getFluid(500L), Materials.Concrete.getMolten(576L), GT_Values.NI, 20, 18);

        RA.addMixerRecipe(new ItemStack(Items.rotten_flesh, 1, 0), new ItemStack(Items.fermented_spider_eye, 1, 0), ItemList.IC2_Scrap.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MeatRaw, 1L), FluidRegistry.getFluidStack("potion.purpledrink", 750), FluidRegistry.getFluidStack("sludge", 1000), ItemList.Food_Chum.get(4L), 128, 24);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wheat, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Water.getFluid(1000L), GT_Values.NF, ItemList.Food_Dough.get(2L), 32, 6);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chili, 1L), ItemList.Food_PotatoChips.get(1L), GT_Values.NI, GT_Values.NI, GT_Values.NF, GT_Values.NF, ItemList.Food_ChiliChips.get(1L), 32, 6);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 5L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ruby, 4L), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, ItemList.IC2_Energium_Dust.get(9L), 600, 120);
        RA.addMixerRecipe(ItemList.IC2_Energium_Dust.get(3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapis, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapotron, 5L), 200, 480);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), new ItemStack(Blocks.brown_mushroom, 1), new ItemStack(Items.spider_eye, 1), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Items.fermented_spider_eye, 1), 100, 6);
        RA.addMixerRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherQuartz, 1L), GT_Values.NI, Materials.Water.getFluid(500L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Fluix, 2L), 20, 18);
        RA.addMixerRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherQuartz, 1L), GT_Values.NI, GT_ModHandler.getDistilledWater(500L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Fluix, 2L), 20, 18);
        RA.addMixerRecipe(ItemList.IC2_Fertilizer.get(1L), new ItemStack(Blocks.dirt, 8, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Water.getFluid(1000L), GT_Values.NF, GT_ModHandler.getModItem(aTextForestry, "soil", 8L, 0), 64, 18);
        RA.addMixerRecipe(ItemList.FR_Fertilizer.get(1L), new ItemStack(Blocks.dirt, 8, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Water.getFluid(1000L), GT_Values.NF, GT_ModHandler.getModItem(aTextForestry, "soil", 8L, 0), 64, 18);
        RA.addMixerRecipe(ItemList.FR_Compost.get(1L), new ItemStack(Blocks.dirt, 8, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Water.getFluid(1000L), GT_Values.NF, GT_ModHandler.getModItem(aTextForestry, "soil", 8L, 0), 64, 18);
        RA.addMixerRecipe(ItemList.FR_Mulch.get(8L), new ItemStack(Blocks.dirt, 8, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Water.getFluid(1000L), GT_Values.NF, GT_ModHandler.getModItem(aTextForestry, "soil", 8L, 0), 64, 18);
        RA.addMixerRecipe(new ItemStack(Blocks.sand, 1, 32767), new ItemStack(Blocks.dirt, 1, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Water.getFluid(250L), GT_Values.NF, GT_ModHandler.getModItem(aTextForestry, "soil", 2L, 1), 16, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.LightFuel, 5L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HeavyFuel, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Fuel, 6L), 16, 120);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.LightFuel, 5L), Materials.Empty.getCells(1), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.HeavyFuel.getFluid(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Fuel, 6L), 16, 120);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HeavyFuel, 1L), Materials.Empty.getCells(5), GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.LightFuel.getFluid(5000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Fuel, 6L), 16, 120);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.LightFuel, 5L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.HeavyFuel.getFluid(1000L), Materials.Fuel.getFluid(6000L), Materials.Empty.getCells(5), 16, 120);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HeavyFuel, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.LightFuel.getFluid(5000L), Materials.Fuel.getFluid(6000L), Materials.Empty.getCells(1), 16, 120);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Water, 5L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L), GT_Values.NI, GT_Values.NI, Materials.Lubricant.getFluid(20), new FluidStack(ItemList.sDrillingFluid, 5000), Materials.Empty.getCells(5), 64, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapis, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Water.getFluid(125), FluidRegistry.getFluidStack("ic2coolant", 125), GT_Values.NI, 256, 75);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapis, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_ModHandler.getDistilledWater(1000), FluidRegistry.getFluidStack("ic2coolant", 1000), GT_Values.NI, 256, 75);
        GT_Values.RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lazurite, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Water.getFluid(125), FluidRegistry.getFluidStack("ic2coolant", 125), GT_Values.NI, 256, 75);
        GT_Values.RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lazurite, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_ModHandler.getDistilledWater(1000), FluidRegistry.getFluidStack("ic2coolant", 1000), GT_Values.NI, 256, 75);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Creosote.getFluid(1000), null, ItemList.SFMixture.get(8), 1600, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Creosote.getFluid(1000), null, ItemList.SFMixture.get(8), 1600, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Caesium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Creosote.getFluid(1000), null, ItemList.SFMixture.get(12), 1600, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Lubricant.getFluid(300), null, ItemList.SFMixture.get(8), 1200, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Lubricant.getFluid(300), null, ItemList.SFMixture.get(8), 1200, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Caesium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Lubricant.getFluid(300), null, ItemList.SFMixture.get(12), 1200, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Glue.getFluid(333), null, ItemList.SFMixture.get(8), 800, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Glue.getFluid(333), null, ItemList.SFMixture.get(8), 800, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Caesium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.Glue.getFluid(333), null, ItemList.SFMixture.get(12), 800, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.McGuffium239.getFluid(10), null, ItemList.SFMixture.get(64), 400, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.McGuffium239.getFluid(10), null, ItemList.SFMixture.get(64), 400, 18);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Caesium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 4L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.McGuffium239.getFluid(10), null, ItemList.SFMixture.get(64), 400, 18);


        RA.addMixerRecipe(ItemList.SFMixture.get(16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderEye, 1L), null, null, Materials.Mercury.getFluid(400), null, ItemList.MSFMixture.get(16), 100, 75);
        RA.addMixerRecipe(ItemList.SFMixture.get(8), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Blaze, 1L), null, null, Materials.Mercury.getFluid(200), null, ItemList.MSFMixture.get(8), 200, 75);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 8), ItemList.MSFMixture.get(48), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(8000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 8), ItemList.MSFMixture.get(32), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(6400), null, ItemList.Block_MSSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 8), ItemList.MSFMixture.get(16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(4000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 8), ItemList.MSFMixture.get(48), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Emerald, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(8000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 8), ItemList.MSFMixture.get(32), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Emerald, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(6400), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 8), ItemList.MSFMixture.get(16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Emerald, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(4000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 8), ItemList.MSFMixture.get(48), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sapphire, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(8000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 8), ItemList.MSFMixture.get(32), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sapphire, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(6400), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 8), ItemList.MSFMixture.get(16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sapphire, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(4000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 8), ItemList.MSFMixture.get(48), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GreenSapphire, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(8000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 8), ItemList.MSFMixture.get(32), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GreenSapphire, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(6400), null, ItemList.Block_MSSFUEL.get(8), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 8), ItemList.MSFMixture.get(16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GreenSapphire, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(1), Materials.NitroFuel.getFluid(4000), null, ItemList.Block_MSSFUEL.get(8), 120, 96);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lanthanum, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Barium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(4000), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Longasssuperconductornameforlvwire, 8L), 600, 24);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cadmium, 5L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(6000), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Pentacadmiummagnesiumhexaoxid, 12L), 1200, 120);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Titanium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Barium, 9L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 10L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(20000), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Titaniumonabariumdecacoppereikosaoxid, 40L), 2400, 120);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 3L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uraniumtriplatinid, 4L), 400, 480);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Vanadium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 3L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Vanadiumtriindinid, 4L), 400, 480);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tin, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Barium, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Titanium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 7L), GT_Utility.getIntegratedCircuit(3), Materials.Oxygen.getGas(14000), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tetraindiumditindibariumtitaniumheptacoppertetrakaidekaoxid, 30L), 2400, 1920);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Palladium, 6L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Osmium, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 13L), 1200, 1920);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Osmiridium, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Europium, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Duranium, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Longasssuperconductornameforuvwire, 10L), 1400, 7680);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Draconium, 6L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadria, 7L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Trinium, 5L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Americium, 6L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tritanium, 5L), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Longasssuperconductornameforuhvwire, 29L), 1600, 30720);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BlackPlutonium, 7L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Oriharukon, 9L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Quantium, 8L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Neutronium, 5L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Infuscolium, 4L), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Longasssuperconductornameforuevwire, 33L), 1800, 122880);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Invar, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Vanadium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Titanium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Molybdenum, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.HSLA, 5), 120, 96);


        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 1), ItemList.SFMixture.get(6), null, null, Materials.NitroFuel.getFluid(1000), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 1), ItemList.SFMixture.get(4), null, null, Materials.NitroFuel.getFluid(800), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 1), ItemList.SFMixture.get(2), null, null, Materials.NitroFuel.getFluid(500), null, ItemList.Block_SSFUEL.get(1), 120, 96);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 1), ItemList.SFMixture.get(6), null, null, Materials.HeavyFuel.getFluid(1500), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 1), ItemList.SFMixture.get(4), null, null, Materials.HeavyFuel.getFluid(1200), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 1), ItemList.SFMixture.get(2), null, null, Materials.HeavyFuel.getFluid(750), null, ItemList.Block_SSFUEL.get(1), 120, 96);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 1), ItemList.SFMixture.get(6), null, null, Materials.LPG.getFluid(1500), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Charcoal, 1), ItemList.SFMixture.get(4), null, null, Materials.LPG.getFluid(1200), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Coal, 1), ItemList.SFMixture.get(2), null, null, Materials.LPG.getFluid(750), null, ItemList.Block_SSFUEL.get(1), 120, 96);

        RA.addMixerRecipe(GregTech_API.getStackofAmountFromOreDict("blockCokeCoal", 1), ItemList.SFMixture.get(1), null, null, Materials.NitroFuel.getFluid(250), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GregTech_API.getStackofAmountFromOreDict("blockCokeCoal", 1), ItemList.SFMixture.get(1), null, null, Materials.HeavyFuel.getFluid(375), null, ItemList.Block_SSFUEL.get(1), 120, 96);
        RA.addMixerRecipe(GregTech_API.getStackofAmountFromOreDict("blockCokeCoal", 1), ItemList.SFMixture.get(1), null, null, Materials.LPG.getFluid(375), null, ItemList.Block_SSFUEL.get(1), 120, 96);

        RA.addExtruderRecipe(ItemList.FR_Wax.get(1L), ItemList.Shape_Extruder_Cell.get(0L), ItemList.FR_WaxCapsule.get(1L), 64, 18);
        RA.addExtruderRecipe(ItemList.FR_RefractoryWax.get(1L), ItemList.Shape_Extruder_Cell.get(0L), ItemList.FR_RefractoryCapsule.get(1L), 128, 18);

        RA.addFluidCannerRecipe(ItemList.Battery_Hull_LV.get(1L), ItemList.IC2_ReBattery.get(1L), Materials.Redstone.getMolten(288L), GT_Values.NF);
        RA.addFluidCannerRecipe(ItemList.Battery_Hull_LV.get(1L), ItemList.Battery_SU_LV_Mercury.getWithCharge(1L, Integer.MAX_VALUE), Materials.Mercury.getFluid(1000L), GT_Values.NF);
        RA.addFluidCannerRecipe(ItemList.Battery_Hull_MV.get(1L), ItemList.Battery_SU_MV_Mercury.getWithCharge(1L, Integer.MAX_VALUE), Materials.Mercury.getFluid(4000L), GT_Values.NF);
        RA.addFluidCannerRecipe(ItemList.Battery_Hull_HV.get(1L), ItemList.Battery_SU_HV_Mercury.getWithCharge(1L, Integer.MAX_VALUE), Materials.Mercury.getFluid(16000L), GT_Values.NF);
        RA.addFluidCannerRecipe(ItemList.Battery_Hull_LV.get(1L), ItemList.Battery_SU_LV_SulfuricAcid.getWithCharge(1L, Integer.MAX_VALUE), Materials.SulfuricAcid.getFluid(1000L), GT_Values.NF);
        RA.addFluidCannerRecipe(ItemList.Battery_Hull_MV.get(1L), ItemList.Battery_SU_MV_SulfuricAcid.getWithCharge(1L, Integer.MAX_VALUE), Materials.SulfuricAcid.getFluid(4000L), GT_Values.NF);
        RA.addFluidCannerRecipe(ItemList.Battery_Hull_HV.get(1L), ItemList.Battery_SU_HV_SulfuricAcid.getWithCharge(1L, Integer.MAX_VALUE), Materials.SulfuricAcid.getFluid(16000L), GT_Values.NF);

        Materials tMaterial = Materials.Iron;

        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Ball.get(0L), Materials.Water.getFluid(250L), new ItemStack(Items.snowball, 1, 0), 20, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Ball.get(0L), GT_ModHandler.getDistilledWater(250L), new ItemStack(Items.snowball, 1, 0), 18, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), Materials.Water.getFluid(1000L), new ItemStack(Blocks.ice, 1, 0), 80, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), GT_ModHandler.getDistilledWater(1000L), new ItemStack(Blocks.ice, 1, 0), 72, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), Materials.Lava.getFluid(1000L), new ItemStack(Blocks.obsidian, 1, 0), 1024, 18);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), Materials.Concrete.getMolten(144L), new ItemStack(GregTech_API.sBlockConcretes, 1, 8), 12, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), Materials.Glowstone.getMolten(576L), new ItemStack(Blocks.glowstone, 1, 0), 12, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), Materials.Glass.getMolten(144L), new ItemStack(Blocks.glass, 1, 0), 12, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Plate.get(0L), Materials.Glass.getMolten(144L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Glass, 1L), 12, 6);

        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Block.get(0L), Materials.ReinforcedGlass.getMolten(144L), GT_ModHandler.getModItem("IC2", "blockAlloyGlass", 1L, 0), 200, 1920);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Plate.get(0L), Materials.ReinforcedGlass.getMolten(144L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.ReinforcedGlass, 1L), 200, 1920);

        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Bottle.get(0L), Materials.Glass.getMolten(144L), ItemList.Bottle_Empty.get(1L), 12, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0L), Materials.Milk.getFluid(250L), ItemList.Food_Cheese.get(1L), 1024, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0L), Materials.Cheese.getMolten(144L), ItemList.Food_Cheese.get(1L), 64, 6);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Anvil.get(0L), Materials.Iron.getMolten(4464L), new ItemStack(Blocks.anvil, 1, 0), 128, 18);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Anvil.get(0L), Materials.WroughtIron.getMolten(4464L), new ItemStack(Blocks.anvil, 1, 0), 128, 18);

        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Plate.get(0L), ItemList.Shape_Mold_Plate.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Casing.get(0L), ItemList.Shape_Mold_Casing.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Gear.get(0L), ItemList.Shape_Mold_Gear.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Credit.get(0L), ItemList.Shape_Mold_Credit.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Bottle.get(0L), ItemList.Shape_Mold_Bottle.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Ingot.get(0L), ItemList.Shape_Mold_Ingot.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Ball.get(0L), ItemList.Shape_Mold_Ball.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Block.get(0L), ItemList.Shape_Mold_Block.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Nugget.get(0L), ItemList.Shape_Mold_Nugget.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Bun.get(0L), ItemList.Shape_Mold_Bun.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Bread.get(0L), ItemList.Shape_Mold_Bread.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Baguette.get(0L), ItemList.Shape_Mold_Baguette.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Cylinder.get(0L), ItemList.Shape_Mold_Cylinder.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Anvil.get(0L), ItemList.Shape_Mold_Anvil.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Name.get(0L), ItemList.Shape_Mold_Name.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Gear_Small.get(0L), ItemList.Shape_Mold_Gear_Small.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Rod.get(0L), ItemList.Shape_Mold_Rod.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Bolt.get(0L), ItemList.Shape_Mold_Bolt.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Round.get(0L), ItemList.Shape_Mold_Round.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Screw.get(0L), ItemList.Shape_Mold_Screw.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Ring.get(0L), ItemList.Shape_Mold_Ring.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Rod_Long.get(0L), ItemList.Shape_Mold_Rod_Long.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Rotor.get(0L), ItemList.Shape_Mold_Rotor.get(1L), 200, 320);
        RA.addFormingPressRecipe(ItemList.Shape_Empty.get(1L), ItemList.Shape_Mold_Turbine_Blade.get(0L), ItemList.Shape_Mold_Turbine_Blade.get(1L), 200, 320);

        RA.addChemicalBathRecipe(ItemList.Food_Raw_Fries.get(1L), Materials.FryingOilHot.getFluid(10L), ItemList.Food_Fries.get(1L), GT_Values.NI, GT_Values.NI, null, 16, 6);
        RA.addChemicalBathRecipe(GT_ModHandler.getIC2Item("dynamite", 1L), Materials.Glue.getFluid(10L), GT_ModHandler.getIC2Item("stickyDynamite", 1L), GT_Values.NI, GT_Values.NI, null, 16, 6);
        RA.addChemicalRecipe(new ItemStack(Items.paper, 1), new ItemStack(Items.string, 1), Materials.Glyceryl.getFluid(500), GT_Values.NF, GT_ModHandler.getIC2Item("dynamite", 1L), 160, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1L), Materials.Concrete.getMolten(144L), GT_ModHandler.getIC2Item("reinforcedStone", 1L), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), Materials.Water.getFluid(125L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.HydratedCoal, 1L), GT_Values.NI, GT_Values.NI, null, 12, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1L), Materials.Water.getFluid(100L), new ItemStack(Items.paper, 1, 0), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Paper, 1L), Materials.Water.getFluid(100L), new ItemStack(Items.paper, 1, 0), GT_Values.NI, GT_Values.NI, null, 100, 6);
        RA.addChemicalBathRecipe(new ItemStack(Items.reeds, 1, 32767), Materials.Water.getFluid(100L), new ItemStack(Items.paper, 1, 0), GT_Values.NI, GT_Values.NI, null, 100, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), GT_ModHandler.getDistilledWater(125L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.HydratedCoal, 1L), GT_Values.NI, GT_Values.NI, null, 12, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1L), GT_ModHandler.getDistilledWater(100L), new ItemStack(Items.paper, 1, 0), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Paper, 1L), GT_ModHandler.getDistilledWater(100L), new ItemStack(Items.paper, 1, 0), GT_Values.NI, GT_Values.NI, null, 100, 6);
        RA.addChemicalBathRecipe(new ItemStack(Items.reeds, 1, 32767), GT_ModHandler.getDistilledWater(100L), new ItemStack(Items.paper, 1, 0), GT_Values.NI, GT_Values.NI, null, 100, 6);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 1), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 2), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 3), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 4), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 5), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 6), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 7), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 8), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 9), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 10), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 11), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 12), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 13), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 14), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.wool, 1, 15), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.wool, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 1), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 2), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 3), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 4), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 5), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 6), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 7), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 8), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 9), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 10), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 11), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 12), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 13), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 14), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.carpet, 1, 15), Materials.Chlorine.getGas(25L), new ItemStack(Blocks.carpet, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.stained_hardened_clay, 1, 32767), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.hardened_clay, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.stained_glass, 1, 32767), Materials.Chlorine.getGas(50L), new ItemStack(Blocks.glass, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        RA.addChemicalBathRecipe(new ItemStack(Blocks.stained_glass_pane, 1, 32767), Materials.Chlorine.getGas(20L), new ItemStack(Blocks.glass_pane, 1, 0), GT_Values.NI, GT_Values.NI, null, 400, 2);
        /*RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 8), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 0), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 9), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 1), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 10), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 2), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 11), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 3), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 12), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 4), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 13), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 5), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 14), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 6), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 15), Materials.Water.getFluid(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 7), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 8), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 0), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 9), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 1), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 10), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 2), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 11), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 3), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 12), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 4), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 13), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 5), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 14), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 6), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(new ItemStack(GregTech_API.sBlockConcretes, 1, 15), GT_ModHandler.getDistilledWater(250L), new ItemStack(GregTech_API.sBlockConcretes, 1, 7), GT_Values.NI, GT_Values.NI, null, 200, 6);*/
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.BlackSteel, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 6L), Materials.ReinforcedGlass.getMolten(144), ItemList.Cleanroom_Glass.get(2L), 200, 75);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.BlackSteel, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 6L), Materials.Concrete.getMolten(144L), ItemList.Block_Plascrete.get(2L), 200, 75);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.HSLA, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 12L), Materials.ReinforcedGlass.getMolten(288), ItemList.Cleanroom_Glass.get(8L), 300, 320);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.HSLA, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 12L), Materials.Concrete.getMolten(288), ItemList.Block_Plascrete.get(8L), 300, 320);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.MaragingSteel250, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 24L), Materials.ReinforcedGlass.getMolten(576), ItemList.Cleanroom_Glass.get(32L), 400, 1200);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.MaragingSteel250, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 24L), Materials.Concrete.getMolten(576), ItemList.Block_Plascrete.get(32L), 400, 1200);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Bronze, 1L), Materials.Concrete.getMolten(144L), ItemList.Block_BronzePlate.get(1L), GT_Values.NI, GT_Values.NI, null, 200, 6);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1L), Materials.Steel.getMolten(288L), ItemList.Block_SteelPlate.get(1L), GT_Values.NI, GT_Values.NI, null, 250, 18);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Titanium, 1L), Materials.Titanium.getMolten(144L), ItemList.Block_TitaniumPlate.get(1L), GT_Values.NI, GT_Values.NI, null, 300, 30);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.TungstenSteel, 1L), Materials.TungstenSteel.getMolten(144L), ItemList.Block_TungstenSteelReinforced.get(1L), GT_Values.NI, GT_Values.NI, null, 350, 75);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Iridium, 1L), Materials.Iridium.getMolten(144L), ItemList.Block_IridiumTungstensteel.get(1L), GT_Values.NI, GT_Values.NI, null, 400, 120);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Naquadah, 1L), Materials.Osmium.getMolten(144L), ItemList.Block_NaquadahPlate.get(1L), GT_Values.NI, GT_Values.NI, null, 450, 320);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1L), Materials.Naquadria.getMolten(144L), ItemList.Block_NeutroniumPlate.get(1L), GT_Values.NI, GT_Values.NI, null, 500, 480);

        RA.addFluidExtractionRecipe(ItemList.Dye_SquidInk.get(1L), GT_Values.NI, FluidRegistry.getFluidStack("squidink", 144), 10000, 128, 6);
        RA.addFluidExtractionRecipe(ItemList.Dye_Indigo.get(1L), GT_Values.NI, FluidRegistry.getFluidStack("indigo", 144), 10000, 128, 6);
        
        RA.addFluidExtractionRecipe(new ItemStack(Items.fish, 1, 0), GT_Values.NI, Materials.FishOil.getFluid(40), 10000, 16, 6);
        RA.addFluidExtractionRecipe(new ItemStack(Items.fish, 1, 1), GT_Values.NI, Materials.FishOil.getFluid(60), 10000, 16, 6);
        RA.addFluidExtractionRecipe(new ItemStack(Items.fish, 1, 2), GT_Values.NI, Materials.FishOil.getFluid(70), 10000, 16, 6);
        RA.addFluidExtractionRecipe(new ItemStack(Items.fish, 1, 3), GT_Values.NI, Materials.FishOil.getFluid(30), 10000, 16, 6);
        RA.addFluidExtractionRecipe(new ItemStack(Items.coal, 1, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 1L), Materials.WoodTar.getFluid(100L), 1000, 30, 18);
        RA.addFluidExtractionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1L), ItemList.IC2_Plantball.get(1L), Materials.Creosote.getFluid(5L), 100, 16, 6);
        RA.addFluidExtractionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.HydratedCoal, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), Materials.Water.getFluid(100L), 10000, 32, 6);
        RA.addFluidExtractionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Monazite, 1L), GT_Values.NI, Materials.Helium.getGas(200L), 10000, 64, 75);

        RA.addFluidExtractionRecipe(GT_ModHandler.getModItem("IC2", "blockAlloyGlass", 1L, 0), GT_Values.NI, Materials.ReinforcedGlass.getMolten(144), 10000, 200, 120);

        RA.addFluidSmelterRecipe(new ItemStack(Items.snowball, 1, 0), GT_Values.NI, Materials.Water.getFluid(250L), 10000, 32, 6);
        RA.addFluidSmelterRecipe(new ItemStack(Blocks.snow, 1, 0), GT_Values.NI, Materials.Water.getFluid(1000L), 10000, 128, 6);
        RA.addFluidSmelterRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ice, 1L), GT_Values.NI, Materials.Ice.getSolid(1000L), 10000, 128, 6);
        RA.addFluidSmelterRecipe(GT_ModHandler.getModItem(aTextForestry, "phosphor", 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphor, 1L), Materials.Lava.getFluid(800L), 1000, 256, 120);

        RA.addAutoclaveRecipe(ItemList.IC2_Energium_Dust.get(9L), GT_ModHandler.getDistilledWater(1000L), ItemList.EnergyCrystal.get(1L), 8000, 1200, 320);
        RA.addAutoclaveRecipe(ItemList.IC2_Energium_Dust.get(9L), Materials.ConductiveIron.getMolten(288), ItemList.EnergyCrystal.get(1L), 10000, 300, 320);
        RA.addAutoclaveRecipe(ItemList.IC2_Energium_Dust.get(9L), Materials.EnergeticAlloy.getMolten(72), ItemList.EnergyCrystal.get(1L), 10000, 150, 192);

        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapotron, 9), GT_ModHandler.getDistilledWater(1000L), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Lapotron, 1), 8000, 1600, 480);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapotron, 9), Materials.EnergeticAlloy.getMolten(144), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Lapotron, 1), 10000, 400, 384);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lapotron, 9), Materials.VibrantAlloy.getMolten(36), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Lapotron, 1), 10000, 200, 320);

        RA.addAutoclaveRecipe(GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 1L, 0), Materials.Water.getFluid(200L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 10), 10000, 2000, 24);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 1L, 600), Materials.Water.getFluid(200L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 11), 10000, 2000, 24);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 1L, 1200), Materials.Water.getFluid(200L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 12), 10000, 2000, 24);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 1L, 0), GT_ModHandler.getDistilledWater(200L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 10), 10000, 1000, 24);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 1L, 600), GT_ModHandler.getDistilledWater(200L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 11), 10000, 1000, 24);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 1L, 1200), GT_ModHandler.getDistilledWater(200L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 12), 10000, 1000, 24);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 64), Materials.Polybenzimidazole.getMolten(72L), GT_ModHandler.getIC2Item("carbonFiber", 128L), 10000, 300, 1920);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 64), Materials.Epoxid.getMolten(144), GT_ModHandler.getIC2Item("carbonFiber", 64L), 10000, 300, 480);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 64), Materials.Polytetrafluoroethylene.getMolten(288), GT_ModHandler.getIC2Item("carbonFiber", 32L), 10000, 400, 120);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 64), Materials.Plastic.getMolten(576), GT_ModHandler.getIC2Item("carbonFiber", 16L), 10000, 600, 30);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NetherStar, 1), Materials.NaquadahEnriched.getMolten(576), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherStar, 1), 3333, 72000, 480);

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Steel, 64), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Zinc, 16), null, ItemList.Component_Filter.get(1), 1600, 18);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_LV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Basic, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_LV.get(1L), 800, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_MV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Good, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_MV.get(1L), 800, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_HV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Advanced, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_HV.get(1L), 800, 18);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Data, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_EV.get(1L), 800, 30);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Elite, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_IV.get(1L), 800, 75);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_LuV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Master, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_LuV.get(1L), 800, 120);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_ZPM.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Ultimate, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_ZPM.get(1L), 800, 480);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Electric_Pump_UV.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Superconductor, 2), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.FluidRegulator_UV.get(1L), 800, 1920);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 2L), OrePrefixes.circuit.get(Materials.Good), 4, GT_Values.NF, ItemList.Schematic.get(1L), 3200, 6);
        RA.addAssemblerRecipe(ItemList.Cover_Shutter.get(1L), OrePrefixes.circuit.get(Materials.Basic), 4, GT_Values.NF, ItemList.FluidFilter.get(1L), 800, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Glass, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit.get(Materials.Basic), 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4)}, GT_Values.NF, ItemList.Cover_Screen.get(1), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 4), GT_OreDictUnificator.get(OrePrefixes.gearGt, Materials.Bronze, 2), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Bronze, 1), GT_Utility.getIntegratedCircuit(2)}, GT_Values.NF, ItemList.Casing_Gearbox_Bronze.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4), GT_OreDictUnificator.get(OrePrefixes.gearGt, Materials.Steel, 2), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1), GT_Utility.getIntegratedCircuit(2)}, GT_Values.NF, ItemList.Casing_Gearbox_Steel.get(2L), 50, 18);
		RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 4), GT_OreDictUnificator.get(OrePrefixes.gearGt, Materials.Titanium, 2), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Titanium, 1), GT_Utility.getIntegratedCircuit(2)}, GT_Values.NF, ItemList.Casing_Gearbox_Titanium.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 4), GT_OreDictUnificator.get(OrePrefixes.gearGt, Materials.TungstenSteel, 2), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.TungstenSteel, 1), GT_Utility.getIntegratedCircuit(2)}, GT_Values.NF, ItemList.Casing_Gearbox_TungstenSteel2.get(2L), 50, 18);


        RA.addCentrifugeRecipe(ItemList.Cell_Empty.get(1), null, Materials.Air.getGas(10000), Materials.Nitrogen.getGas(3900), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1), null, null, null, null, null, null, 1600, 6);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Galena, 3), GT_OreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Sphalerite, 1), Materials.SulfuricAcid.getFluid(4000), new FluidStack(ItemList.sIndiumConcentrate, 8000), null, 60, 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 16), null, new FluidStack(ItemList.sIndiumConcentrate, 32000), new FluidStack(ItemList.sLeadZincSolution, 32000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 1), 200, 1200);
        RA.addElectrolyzerRecipe(null, null, new FluidStack(ItemList.sLeadZincSolution, 8000), Materials.Water.getFluid(2000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silver, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Zinc, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 3), null, null, null, 300, 192);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Pentlandite, 4), null, new FluidStack(ItemList.sNitricAcid, 4000), new FluidStack(ItemList.sNickelSulfate, 36000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.PlatinumGroupSludge, 1), 200, 30);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Chalcopyrite, 4), null, new FluidStack(ItemList.sNitricAcid, 4000), new FluidStack(ItemList.sBlueVitriol, 36000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.PlatinumGroupSludge, 1), 200, 30);
        RA.addElectrolyzerRecipe(ItemList.Cell_Empty.get(1), null, new FluidStack(ItemList.sBlueVitriol, 9000), Materials.SulfuricAcid.getFluid(8000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 1), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1), null, null, null, null, null, 900, 30);
        RA.addElectrolyzerRecipe(ItemList.Cell_Empty.get(1), null, new FluidStack(ItemList.sNickelSulfate, 9000), Materials.SulfuricAcid.getFluid(8000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Nickel, 1), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1), null, null, null, null, null, 900, 30);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.PlatinumGroupSludge, 1), null, null, null, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SiliconDioxide, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Palladium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iridium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Osmium, 1), new int[]{10000, 10000, 10000, 3200, 3000, 2500}, 900, 30);

        RA.addSlicerRecipe(ItemList.Food_Dough_Chocolate.get(1L), ItemList.Shape_Slicer_Flat.get(0L), ItemList.Food_Raw_Cookie.get(4L), 128, 6);
        RA.addSlicerRecipe(ItemList.Food_Baked_Bun.get(1L), ItemList.Shape_Slicer_Flat.get(0L), ItemList.Food_Sliced_Bun.get(2L), 128, 6);
        RA.addSlicerRecipe(ItemList.Food_Baked_Bread.get(1L), ItemList.Shape_Slicer_Flat.get(0L), ItemList.Food_Sliced_Bread.get(2L), 128, 6);
        RA.addSlicerRecipe(ItemList.Food_Baked_Baguette.get(1L), ItemList.Shape_Slicer_Flat.get(0L), ItemList.Food_Sliced_Baguette.get(2L), 128, 6);
//Circuit Recipes!!!
        Object[] o = new Object[0];
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Board_Coated.get(3), new Object[]{" R ","PPP"," R ",'P',GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 1),'R',ItemList.IC2_Resin.get(1)});
        //RA.addAssemblerRecipe(Materials.Wood.getPlates(8), ItemList.IC2_Resin.get(1), Materials.Glue.getFluid(100), ItemList.Circuit_Board_Coated.get(8), 160, 6);
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Board_Phenolic.get(8), new Object[]{"PRP","PPP","PPP",'P',GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1),'R',GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Glue, 1)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Board_Phenolic.get(32), new Object[]{"PRP","PPP","PPP",'P',GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1),'R',GT_OreDictUnificator.get(OrePrefixes.cell, Materials.BisphenolA, 1)});
        //RA.addAssemblerRecipe(Materials.Wood.getDust(1), ItemList.Shape_Mold_Plate.get(0), Materials.Glue.getFluid(100), ItemList.Circuit_Board_Phenolic.get(1), 30, 6);
        //RA.addAssemblerRecipe(Materials.Wood.getDust(1), ItemList.Shape_Mold_Plate.get(0), Materials.BisphenolA.getFluid(100), ItemList.Circuit_Board_Phenolic.get(4), 30, 6);
        //RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 1), Materials.SulfuricAcid.getFluid(125), null, ItemList.Circuit_Board_Plastic.get(1), 500, 10);
        //RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.PolyvinylChloride, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 1), Materials.SulfuricAcid.getFluid(125), null, ItemList.Circuit_Board_Plastic.get(2), 500, 10);
        //RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Polytetrafluoroethylene, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 1), Materials.SulfuricAcid.getFluid(125), null, ItemList.Circuit_Board_Plastic.get(4), 500, 10);
        //RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Epoxid, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 1), Materials.SulfuricAcid.getFluid(125), null, ItemList.Circuit_Board_Epoxy.get(1), 500, 10);
        //RA.addChemicalBathRecipe(ItemList.Circuit_Parts_GlassFiber.get(1), Materials.Epoxid.getMolten(144), Materials.EpoxidFiberReinforced.getPlates(1), GT_Values.NI, GT_Values.NI, null, 240, 18);
        //RA.addChemicalBathRecipe(GT_ModHandler.getIC2Item("carbonFiber", 1),  Materials.Epoxid.getMolten(144), Materials.EpoxidFiberReinforced.getPlates(1), GT_Values.NI, GT_Values.NI, null, 240, 18);
        //RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.EpoxidFiberReinforced, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 1), Materials.SulfuricAcid.getFluid(125), null, ItemList.Circuit_Board_Fiberglass.get(1), 500, 10);
        //RA.addChemicalRecipe(ItemList.Circuit_Board_Fiberglass.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Electrum, 16), Materials.SulfuricAcid.getFluid(250), null, ItemList.Circuit_Board_Multifiberglass.get(1), 100, 480);

        //RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0), Materials.Polytetrafluoroethylene.getMolten(36), ItemList.Circuit_Parts_PetriDish.get(1), 160, 18);
        //RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0), Materials.Polystyrene.getMolten(36), ItemList.Circuit_Parts_PetriDish.get(1), 160, 18);
        //RA.addMixerRecipe(Materials.Sugar.getDust(4), Materials.MeatRaw.getDust(1), Materials.Salt.getDustTiny(1), GT_Values.NI, GT_ModHandler.getDistilledWater(4000), Materials.GrowthMediumRaw.getFluid(4000), GT_Values.NI, 160, 18);
        //RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), Materials.GrowthMediumRaw.getFluid(1000), Materials.GrowthMediumSterilized.getFluid(1000), 60, 24);
        //RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), FluidRegistry.getFluidStack("potion.dragonblood", 1000), Materials.GrowthMediumSterilized.getFluid(1000), 60, 24);
        //RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Board_Multifiberglass.get(1), ItemList.Circuit_Parts_PetriDish.get(1), ItemList.Electric_Pump_LV.get(1,o), ItemList.Sensor_LV.get(1,o)},
        //		OrePrefixes.circuit.get(Materials.Good), 1, Materials.GrowthMediumSterilized.getFluid(250), ItemList.Circuit_Board_Wetware.get(1), 400, 480);

        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Resistor.get(3), new Object[]{" P ","FCF"," P ",'P',new ItemStack(Items.paper),'F',OrePrefixes.wireFine.get(Materials.Copper),'C',OrePrefixes.dust.get(Materials.Coal)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Resistor.get(3), new Object[]{" P ","FCF"," P ",'P',new ItemStack(Items.paper),'F',OrePrefixes.wireFine.get(Materials.Copper),'C',OrePrefixes.dust.get(Materials.Charcoal)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Resistor.get(3), new Object[]{" P ","FCF"," P ",'P',new ItemStack(Items.paper),'F',OrePrefixes.wireFine.get(Materials.Copper),'C',OrePrefixes.dust.get(Materials.Carbon)});
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1),     GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4), ItemList.Circuit_Parts_Resistor.get(12), 160, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Charcoal, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4), ItemList.Circuit_Parts_Resistor.get(12), 160, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1),   GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4), ItemList.Circuit_Parts_Resistor.get(12), 160, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Electrum, 4),Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_ResistorSMD.get(24), 80, 96);
        //RA.addAlloySmelterRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1), ItemList.Shape_Mold_Ball.get(0), ItemList.Circuit_Parts_Glass_Tube.get(1,o), 160, 6);
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Vacuum_Tube.get(1), new Object[]{"PGP","FFF",'G',ItemList.Circuit_Parts_Glass_Tube,'P',new ItemStack(Items.paper),'F',OrePrefixes.wireFine.get(Materials.Copper)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Vacuum_Tube.get(1), new Object[]{"PGP","FFF",'G',ItemList.Circuit_Parts_Glass_Tube,'P',new ItemStack(Items.paper),'F',OrePrefixes.wireGt01.get(Materials.Copper)});
        //RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2), Materials.Paper.getPlates(2)}, GT_Values.NF, ItemList.Circuit_Parts_Vacuum_Tube.get(1), 120, 6);
        //RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Copper, 2), Materials.Paper.getPlates(2)}, GT_Values.NF, ItemList.Circuit_Parts_Vacuum_Tube.get(1), 120, 6);
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Diode.get(1,o), new Object[]{"BG ","WDW","BG ",'B',OrePrefixes.dye.get(Materials.Black),'G',new ItemStack(Blocks.glass_pane),'D',ItemList.Circuit_Silicon_Wafer.get(1),'W',OrePrefixes.wireGt01.get(Materials.Tin)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Diode.get(1,o), new Object[]{"BG ","WDW","BG ",'B',OrePrefixes.dye.get(Materials.Black),'G',new ItemStack(Blocks.glass_pane),'D',ItemList.Circuit_Silicon_Wafer.get(1),'W',OrePrefixes.wireFine.get(Materials.Tin)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Diode.get(4,o), new Object[]{"BG ","WDW","BG ",'B',OrePrefixes.dye.get(Materials.Black),'G',new ItemStack(Blocks.glass_pane),'D',GT_OreDictUnificator.get(OrePrefixes.dustTiny, Materials.Gallium, 1),'W',OrePrefixes.wireGt01.get(Materials.Tin)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Diode.get(4,o), new Object[]{"BG ","WDW","BG ",'B',OrePrefixes.dye.get(Materials.Black),'G',new ItemStack(Blocks.glass_pane),'D',GT_OreDictUnificator.get(OrePrefixes.dustTiny, Materials.Gallium, 1),'W',OrePrefixes.wireFine.get(Materials.Tin)});
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 4), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.Gallium, 1), Materials.Plastic.getMolten(288), ItemList.Circuit_Parts_Diode.get(16), 400, 75);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Platinum, 4), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.Gallium, 1), Materials.Plastic.getMolten(288), ItemList.Circuit_Parts_DiodeSMD.get(32), 400, 120);
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Coil.get(2,o), new Object[]{"WWW","WDW","WWW",'G',new ItemStack(Blocks.glass_pane),'D',OrePrefixes.bolt.get(Materials.Steel),'W',OrePrefixes.wireFine.get(Materials.Copper)});
        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Coil.get(4,o), new Object[]{"WWW","WDW","WWW",'G',new ItemStack(Blocks.glass_pane),'D',OrePrefixes.bolt.get(Materials.NickelZincFerrite),'W',OrePrefixes.wireFine.get(Materials.Copper)});
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 8), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 1), ItemList.Circuit_Parts_Coil.get(2,o), 80, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 8), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.NickelZincFerrite, 1), ItemList.Circuit_Parts_Coil.get(4,o), 80, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 8), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 1), ItemList.Circuit_Parts_Coil.get(2,o), 80, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 8), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.NickelZincFerrite, 1), ItemList.Circuit_Parts_Coil.get(4,o), 80, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Silicon, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Tin, 6),Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_Transistor.get(8), 80, 24);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Gallium, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 6),Materials.Plastic.getMolten(288), ItemList.Circuit_Parts_TransistorSMD.get(32), 80, 96);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 2), ItemList.Circuit_Parts_Capacitor.get(2), 80, 96);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Silicone, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 2),Materials.Plastic.getMolten(72), ItemList.Circuit_Parts_CapacitorSMD.get(32), 120, 120);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.foil, Materials.PolyvinylChloride, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 2),Materials.Plastic.getMolten(72), ItemList.Circuit_Parts_CapacitorSMD.get(32), 100, 120);
        //RA.addExtruderRecipe(Materials.BorosilicateGlass.getIngots(1), ItemList.Shape_Extruder_Wire.get(0), ItemList.Circuit_Parts_GlassFiber.get(8), 160, 96);

        //RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer2.get(1), GT_Utility.copyAmount(0,GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderPearl, 1)), ItemList.Circuit_Wafer_NAND.get(1), 500, 480, true);
        //RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer3.get(1), GT_Utility.copyAmount(0,GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderPearl, 1)), ItemList.Circuit_Wafer_NAND.get(4), 200, 1920, true);
        //RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer2.get(1), GT_Utility.copyAmount(0,GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderEye, 1)), ItemList.Circuit_Wafer_NOR.get(1), 500, 480, true);
        //RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer3.get(1), GT_Utility.copyAmount(0,GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderEye, 1)), ItemList.Circuit_Wafer_NOR.get(4), 200, 1920, true);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_ILC.get(1), ItemList.Circuit_Chip_ILC.get(8,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_Ram.get(1), ItemList.Circuit_Chip_Ram.get(32,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_NAND.get(1), ItemList.Circuit_Chip_NAND.get(32,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_NOR.get(1), ItemList.Circuit_Chip_NOR.get(16,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_CPU.get(1), ItemList.Circuit_Chip_CPU.get(8,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC.get(1), ItemList.Circuit_Chip_SoC.get(10,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC2.get(1), ItemList.Circuit_Chip_SoC2.get(8,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_PIC.get(1), ItemList.Circuit_Chip_PIC.get(4,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_HPIC.get(1), ItemList.Circuit_Chip_HPIC.get(2,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_NanoCPU.get(1), ItemList.Circuit_Chip_NanoCPU.get(7,o), null, 600, 75);
        //RA.addCutterRecipe(ItemList.Circuit_Wafer_QuantumCPU.get(1), ItemList.Circuit_Chip_QuantumCPU.get(5,o), null, 600, 75);
        //RA.addChemicalRecipe(ItemList.Circuit_Wafer_PIC.get(1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 2), Materials.RedAlloy.getMolten(288), null, ItemList.Circuit_Wafer_HPIC.get(1), 1200, 1920);
        //RA.addChemicalRecipe(ItemList.Circuit_Wafer_CPU.get(1), GT_Utility.copyAmount(16, ic2.core.Ic2Items.carbonFiber), Materials.Glowstone.getMolten(576), null, ItemList.Circuit_Wafer_NanoCPU.get(1), 400, 1920);
        //RA.addChemicalRecipe(ItemList.Circuit_Wafer_NanoCPU.get(1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 1), Materials.Radon.getGas(50), null, ItemList.Circuit_Wafer_QuantumCPU.get(1), 600, 1920);
        //RA.addChemicalRecipe(ItemList.Circuit_Wafer_NanoCPU.get(1), ItemList.QuantumEye.get(2), Materials.GalliumArsenide.getMolten(288), null, ItemList.Circuit_Wafer_QuantumCPU.get(1), 400, 1920);

        //RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Emerald, 1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1,o), 1000, 12000, 320, true);
        //RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Olivine, 1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1,o), 1000, 12000, 320, true);
        //RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Emerald, 8), Materials.UUMatter.getFluid(100), ItemList.Tool_DataOrb.get(1,o), 10000, 12000, 320, true);
        //RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Olivine, 8), Materials.UUMatter.getFluid(100), ItemList.Tool_DataOrb.get(1,o), 10000, 12000, 320, true);
        //GT_ModHandler.addShapelessCraftingRecipe(ItemList.Circuit_Parts_RawCrystalChip.get(9,o), new Object[]{ItemList.Circuit_Chip_CrystalCPU.get(1,o)});
        //RA.addBlastRecipe(ItemList.Circuit_Parts_RawCrystalChip.get(1,o), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Emerald, 1), Materials.Helium.getGas(1000), null, ItemList.Circuit_Parts_Crystal_Chip_Elite.get(1,o), null, 900, 480, 5000);
        //RA.addBlastRecipe(ItemList.Circuit_Parts_RawCrystalChip.get(1,o), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Olivine, 1), Materials.Helium.getGas(1000), null, ItemList.Circuit_Parts_Crystal_Chip_Elite.get(1,o), null, 900, 480, 5000);

        //GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Good.get(1,o), new Object[]{"IVC","VDV","CVI",'D',ItemList.Circuit_Parts_Diode.get(1,o),'C',GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.RedAlloy, 1),'V', Ic2Items.electronicCircuit ,'I',ItemList.IC2_Item_Casing_Steel.get(1,o)});

        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0), Materials.Polytetrafluoroethylene.getMolten(36), ItemList.Circuit_Parts_PetriDish.get(1), 160, 18);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0), Materials.Polystyrene.getMolten(36), ItemList.Circuit_Parts_PetriDish.get(1), 160, 18);
        //RA.addMixerRecipe(Materials.Sugar.getDust(4), Materials.MeatRaw.getDust(1), Materials.Salt.getDustTiny(1), GT_Values.NI, GT_ModHandler.getDistilledWater(4000), Materials.GrowthMediumRaw.getFluid(4000), GT_Values.NI, 160, 18);

        RA.addChemicalRecipe(Materials.Plutonium.getDust(1), Materials.Europium.getDust(1), Materials.Radon.getGas(8000L), Materials.GrowthMediumRaw.getFluid(8000L), GT_Values.NI, 300, 7680);
        RA.addChemicalRecipe(Materials.Naquadah.getDust(1), Materials.Americium.getDust(1), Materials.Radon.getGas(32000L), Materials.GrowthMediumRaw.getFluid(32000L), GT_Values.NI, 300, 30720);
        RA.addChemicalRecipe(Materials.Naquadria.getDust(1), Materials.Neutronium.getDust(1), Materials.Radon.getGas(64000L), Materials.GrowthMediumRaw.getFluid(64000L), GT_Values.NI, 600, 122880);


        //RA.addChemicalRecipe(GT_ModHandler.getModItem("GalaxySpace","item.UnknowCrystal",16L), Materials.Osmiridium.getDust(2),  Materials.GrowthMediumSterilized.getFluid(1000L), Materials.BacterialSludge.getFluid(1000L), ItemList.Circuit_Chip_Stemcell.get(64L), GT_Values.NI,600, 30720);
        RA.addChemicalRecipe(ItemList.Circuit_Chip_Stemcell.get(16L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.CosmicNeutronium, 2), Materials.BioMediumSterilized.getFluid(1000L), Materials.Mutagen.getFluid(1000L), ItemList.Circuit_Chip_Biocell.get(16L), GT_Values.NI,1200, 500000);

        RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), Materials.GrowthMediumRaw.getFluid(1000), Materials.GrowthMediumSterilized.getFluid(1000), 100, 7680);
        //RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), FluidRegistry.getFluidStack("potion.dragonblood", 1000), Materials.GrowthMediumSterilized.getFluid(1000), 60, 24);
        //RA.addMixerRecipe(Materials.MysteriousCrystal.getDust(4), Materials.Oriharukon.getDust(4), Materials.BlackPlutonium.getDustTiny(4), GT_ModHandler.getModItem("dreamcraft", "item.TCetiESeaweedExtract", 16L, 0), FluidRegistry.getFluidStack("mutagen", 4000), Materials.BioMediumRaw.getFluid(4000), GT_Values.NI, 160, 275);

        // --- Superconductors
		// --- LV
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforlvwire, 4L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Potin, 2L), ItemList.Electric_Pump_LV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(1000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLV, 4L), 200, 24, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforlvwire, 9L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Steel, 2L), ItemList.Electric_Pump_MV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(2000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLV, 9L), 300, 120, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforlvwire, 20L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.HSLA, 2L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(4000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLV, 20L), 400, 480, true);
		// --- MV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Pentacadmiummagnesiumhexaoxid, 3L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.StainlessSteel, 2L), ItemList.Electric_Pump_MV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(2000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorMV, 3L), 300, 120, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Pentacadmiummagnesiumhexaoxid, 8L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Titanium, 2L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(4000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorMV, 8L), 400, 480, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Pentacadmiummagnesiumhexaoxid, 18L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.MaragingSteel300, 2L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(8000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorMV, 18L), 500, 1920, true);
		// --- HV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Titaniumonabariumdecacoppereikosaoxid, 6L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Titanium, 4L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(4000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorHV, 6L), 400, 240, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Titaniumonabariumdecacoppereikosaoxid, 14L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Inconel690, 4L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(8000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorHV, 14L), 500, 480, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Titaniumonabariumdecacoppereikosaoxid, 30L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.NiobiumTitanium, 4L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(16000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorHV, 30L), 600, 1920, true);
		// --- EV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Uraniumtriplatinid, 9L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.TungstenSteel, 6L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(6000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorEV, 9L), 500, 480, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Uraniumtriplatinid, 20L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Inconel792, 6L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(12000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorEV, 20L), 600, 1920, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Uraniumtriplatinid, 44L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Enderium, 6L), ItemList.Electric_Pump_LuV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(24000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorEV, 44L), 700, 7680, true);
		// --- IV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Vanadiumtriindinid, 12L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.NiobiumTitanium, 8L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(8000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorIV, 12L), 600, 1920, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Vanadiumtriindinid, 28L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Duranium, 8L), ItemList.Electric_Pump_LuV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(16000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorIV, 28L), 700, 7680, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Vanadiumtriindinid, 60L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Infuscolium, 8L), ItemList.Electric_Pump_ZPM.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(32000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorIV, 60L), 800, 30720, true);
		// --- LuV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Tetraindiumditindibariumtitaniumheptacoppertetrakaidekaoxid, 15L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Enderium, 10L), ItemList.Electric_Pump_LuV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(12000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 15L), 700, 7680, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Tetraindiumditindibariumtitaniumheptacoppertetrakaidekaoxid, 31L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Duranium, 10L), ItemList.Electric_Pump_ZPM.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(24000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 31L), 800, 30720, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Tetraindiumditindibariumtitaniumheptacoppertetrakaidekaoxid, 63L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Neutronium, 10L), ItemList.Electric_Pump_UV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(48000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 63L), 900, 122880, true);
		// --- ZPM
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 18L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Naquadah, 12L), ItemList.Electric_Pump_ZPM.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(16000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorZPM, 18L), 800, 30720, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 40L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Infuscolium, 12L), ItemList.Electric_Pump_UV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(32000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorZPM, 40L), 900, 122880, true);
		// --- UV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforuvwire, 21L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Duranium, 14L), ItemList.Electric_Pump_UV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(20000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorUV, 21L), 1000, 122880, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforuvwire, 46L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Neutronium, 14L), ItemList.Electric_Pump_UHV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(40000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorUV, 46L), 2000, 500000, true);
		// --- UHV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforuhvwire, 24L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Infuscolium, 16L), ItemList.Electric_Pump_UHV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(24000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Superconductor, 24L), 1200, 500000, true);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforuhvwire, 56L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.DraconiumAwakened, 16L), ItemList.Electric_Pump_UEV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(48000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Superconductor, 56L), 1800, 2000000, true);
		// --- UEV
		RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Longasssuperconductornameforuevwire, 28L), GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.DraconiumAwakened, 18L), ItemList.Electric_Pump_UEV.get(1L), GT_Utility.getIntegratedCircuit(9)}, Materials.Helium.getGas(26000L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorUEV, 28L), 1200, 1966080, true);
        
        RA.addFormingPressRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 10), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 0L, 13), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 16), 200, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.CertusQuartz, 1L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 0L, 13), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 16), 200, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Diamond, 1L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 0L, 14), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 17), 200, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Gold, 1L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 0L, 15), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 18), 200, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Silicon, 1L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 0L, 19), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 20), 200, 18);

        RA.addFormingPressRecipe(ItemList.Food_Dough_Sugar.get(4L), ItemList.Shape_Mold_Cylinder.get(0L), ItemList.Food_Raw_Cake.get(1L), 384, 6);
        for (Materials tMat : Materials.values()) {
            if (tMat.mStandardMoltenFluid != null && tMat.contains(SubTag.SOLDERING_MATERIAL) && !(GregTech_API.mUseOnlyGoodSolderingMaterials && !tMat.contains(SubTag.SOLDERING_MATERIAL_GOOD))) {
                int tMultiplier = tMat.contains(SubTag.SOLDERING_MATERIAL_GOOD) ? 1 : tMat.contains(SubTag.SOLDERING_MATERIAL_BAD) ? 4 : 2;
                for (ItemStack tPlate : new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 1L)}) {
                    RA.addAssemblerRecipe(new ItemStack(Blocks.lever, 1, 32767), tPlate, tMat.getMolten(144L * tMultiplier / 2L), ItemList.Cover_Controller.get(1L), 800, 18);
                    RA.addAssemblerRecipe(new ItemStack(Blocks.redstone_torch, 1, 32767), tPlate, tMat.getMolten(144L * tMultiplier / 2L), ItemList.Cover_ActivityDetector.get(1L), 800, 18);
                    RA.addAssemblerRecipe(new ItemStack(Blocks.heavy_weighted_pressure_plate, 1, 32767), tPlate, tMat.getMolten(144L * tMultiplier / 2L), ItemList.Cover_FluidDetector.get(1L), 800, 18);
                    RA.addAssemblerRecipe(new ItemStack(Blocks.light_weighted_pressure_plate, 1, 32767), tPlate, tMat.getMolten(144L * tMultiplier / 2L), ItemList.Cover_ItemDetector.get(1L), 800, 18);
                    RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("ecMeter", 1L), tPlate, tMat.getMolten(144L * tMultiplier / 2L), ItemList.Cover_EnergyDetector.get(1L), 800, 18);
                }
            }
        }


        RA.addAssemblerRecipe(new ItemStack(Blocks.redstone_torch, 2, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), Materials.Concrete.getMolten(144L), new ItemStack(Items.repeater, 1, 0), 800, 1);
        RA.addAssemblerRecipe(new ItemStack(Items.leather, 1, 32767), new ItemStack(Items.lead, 1, 32767), Materials.Glue.getFluid(50L), new ItemStack(Items.name_tag, 1, 0), 100, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Paper, 8L), new ItemStack(Items.compass, 1, 32767), GT_Values.NF, new ItemStack(Items.map, 1, 0), 100, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tantalum, 1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Manganese, 1L), Materials.Plastic.getMolten(144L), ItemList.Battery_RE_ULV_Tantalum.get(8L), 100, 6);
        RA.addAssemblerRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 16), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 20), Materials.Redstone.getMolten(144L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 23), 120, 75);
        RA.addAssemblerRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 17), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 20), Materials.Redstone.getMolten(144L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 24), 120, 75);
        RA.addAssemblerRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 18), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 20), Materials.Redstone.getMolten(144L), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 22), 120, 75);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.CertusQuartz, 1L), new ItemStack(Blocks.sand, 1, 32767), GT_Values.NF, GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 2L, 0), 64, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NetherQuartz, 1L), new ItemStack(Blocks.sand, 1, 32767), GT_Values.NF, GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 2L, 600), 64, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Fluix, 1L), new ItemStack(Blocks.sand, 1, 32767), GT_Values.NF, GT_ModHandler.getModItem(aTextAE, "item.ItemCrystalSeed", 2L, 1200), 64, 6);

        if (Loader.isModLoaded(aTextForestry)) {
            RA.addAssemblerRecipe(ItemList.FR_Wax.get(6L), new ItemStack(Items.string, 1, 32767), Materials.Water.getFluid(600L), GT_ModHandler.getModItem(aTextForestry, "candle", 24L, 0), 64, 6);
            RA.addAssemblerRecipe(ItemList.FR_Wax.get(2L), ItemList.FR_Silk.get(1L), Materials.Water.getFluid(200L), GT_ModHandler.getModItem(aTextForestry, "candle", 8L, 0), 16, 6);
            RA.addAssemblerRecipe(ItemList.FR_Silk.get(9L), ItemList.Circuit_Integrated.getWithDamage(0L, 9L), Materials.Water.getFluid(500L), GT_ModHandler.getModItem(aTextForestry, "craftingMaterial", 1L, 3), 64, 6);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem(aTextForestry, "propolis", 5L, 2), ItemList.Circuit_Integrated.getWithDamage(0L, 5L), GT_Values.NF, GT_ModHandler.getModItem(aTextForestry, "craftingMaterial", 1L, 1), 16, 6);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem(aTextForestry, "sturdyMachine", 1L, 0), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Diamond, 4L), Materials.Water.getFluid(5000L), ItemList.FR_Casing_Hardened.get(1L), 64, 30);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Bronze, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), GT_Values.NF, ItemList.FR_Casing_Sturdy.get(1L), 32, 18);
            //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Tin, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 6L), Materials.Water.getFluid(1000L), GT_ModHandler.getModItem(aTextForestry, "chipsets", 1L, 0), 16, 6);
            //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Bronze, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 6L), Materials.Water.getFluid(1000L), GT_ModHandler.getModItem(aTextForestry, "chipsets", 1L, 1), 32, 18);
            //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 6L), Materials.Water.getFluid(1000L), GT_ModHandler.getModItem(aTextForestry, "chipsets", 1L, 2), 48, 24);
            //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.WroughtIron, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 6L), Materials.Water.getFluid(1000L), GT_ModHandler.getModItem(aTextForestry, "chipsets", 1L, 2), 48, 24);
            //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Gold, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 6L), Materials.Water.getFluid(1000L), GT_ModHandler.getModItem(aTextForestry, "chipsets", 1L, 3), 64, 30);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem(aTextForestry, "craftingMaterial", 5L, 1), ItemList.Circuit_Integrated.getWithDamage(0L, 5L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnderPearl, 1L), 64, 6);
        }

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1L), new ItemStack(Blocks.wool, 1, 32767), Materials.Creosote.getFluid(1000L), new ItemStack(Blocks.torch, 6, 0), 400, 1);
        RA.addAssemblerRecipe(new ItemStack(Blocks.piston, 1, 32767), new ItemStack(Items.slime_ball, 1, 32767), GT_Values.NF, new ItemStack(Blocks.sticky_piston, 1, 0), 100, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.piston, 1, 32767), ItemList.IC2_Resin.get(1L), GT_Values.NF, new ItemStack(Blocks.sticky_piston, 1, 0), 100, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.piston, 1, 32767), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Glue.getFluid(100L), new ItemStack(Blocks.sticky_piston, 1, 0), 100, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Rubber, 3L), GT_ModHandler.getIC2Item("carbonMesh", 3L), GT_Utility.getIntegratedCircuit(1)}, Materials.Glue.getFluid(300L), ItemList.Duct_Tape.get(1L), 100, 120);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Silicone, 2L), GT_ModHandler.getIC2Item("carbonMesh", 2L), GT_Utility.getIntegratedCircuit(2)}, Materials.Glue.getFluid(200L), ItemList.Duct_Tape.get(1L), 100, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StyreneButadieneRubber, 1L), GT_ModHandler.getIC2Item("carbonMesh", 1L), GT_Utility.getIntegratedCircuit(3)}, Materials.Glue.getFluid(100L), ItemList.Duct_Tape.get(1L), 100, 1920);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Paper, 3L), new ItemStack(Items.leather, 1, 32767), Materials.Glue.getFluid(20L), new ItemStack(Items.book, 1, 0), 32, 6);
        RA.addAssemblerRecipe(ItemList.Paper_Printed_Pages.get(1L), new ItemStack(Items.leather, 1, 32767), Materials.Glue.getFluid(20L), new ItemStack(Items.written_book, 1, 0), 32, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.itemCasing, Materials.Tin, 4L), new ItemStack(Blocks.glass_pane, 1, 32767), GT_Values.NF, ItemList.Cell_Universal_Fluid.get(1L), 128, 6);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.casingSmall, Materials.Tin, 4), new ItemStack(Blocks.glass_pane, 1, 32767), GT_Values.NF, ItemList.Cell_Universal_Fluid.get(1L), 128, 6);
        RA.addAssemblerRecipe(ItemList.Food_Baked_Cake.get(1L), new ItemStack(Items.egg, 1, 0), Materials.Milk.getFluid(3000L), new ItemStack(Items.cake, 1, 0), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Bun.get(2L), ItemList.Circuit_Integrated.getWithDamage(0L, 2L), GT_Values.NF, ItemList.Food_Sliced_Buns.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Bread.get(2L), ItemList.Circuit_Integrated.getWithDamage(0L, 2L), GT_Values.NF, ItemList.Food_Sliced_Breads.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Baguette.get(2L), ItemList.Circuit_Integrated.getWithDamage(0L, 2L), GT_Values.NF, ItemList.Food_Sliced_Baguettes.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Buns.get(1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), GT_Values.NF, ItemList.Food_Sliced_Bun.get(2L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Breads.get(1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), GT_Values.NF, ItemList.Food_Sliced_Bread.get(2L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Baguettes.get(1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), GT_Values.NF, ItemList.Food_Sliced_Baguette.get(2L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Bun.get(2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MeatCooked, 1L), GT_Values.NF, ItemList.Food_Burger_Meat.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Buns.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MeatCooked, 1L), GT_Values.NF, ItemList.Food_Burger_Meat.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Bun.get(2L), ItemList.Food_Chum.get(1L), GT_Values.NF, ItemList.Food_Burger_Chum.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Buns.get(1L), ItemList.Food_Chum.get(1L), GT_Values.NF, ItemList.Food_Burger_Chum.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Bun.get(2L), ItemList.Food_Sliced_Cheese.get(3L), GT_Values.NF, ItemList.Food_Burger_Cheese.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Sliced_Buns.get(1L), ItemList.Food_Sliced_Cheese.get(3L), GT_Values.NF, ItemList.Food_Burger_Cheese.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Flat_Dough.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MeatCooked, 1L), GT_Values.NF, ItemList.Food_Raw_Pizza_Meat.get(1L), 100, 6);
        RA.addAssemblerRecipe(ItemList.Food_Flat_Dough.get(1L), ItemList.Food_Sliced_Cheese.get(3L), GT_Values.NF, ItemList.Food_Raw_Pizza_Cheese.get(1L), 100, 6);

        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Copper, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 0), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.AnnealedCopper, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 0), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Tin, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 1), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Bronze, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 2), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Iron, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 3), 200, 120);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.WroughtIron, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 3), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Gold, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 4), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Diamond, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 5), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Obsidian, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 6), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Blaze, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 7), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Rubber, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 8), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Emerald, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 9), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Apatite, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 10), 200, 30);
        RA.addCircuitAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Lapis, 2L)}, Materials.Glass.getMolten(576L), GT_ModHandler.getModItem(aTextForestry, "thermionicTubes", 4L, 11), 200, 30);

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2L), new ItemStack(Items.iron_door, 1), ItemList.Cover_Shutter.get(2L), 800, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 2L), new ItemStack(Items.iron_door, 1), ItemList.Cover_Shutter.get(2L), 800, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 2L), new ItemStack(Items.iron_door, 1), ItemList.Cover_Shutter.get(2L), 800, 18);

        //RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new FluidStack(ItemList.sOilExtraHeavy, 10), Materials.OilHeavy.getFluid(15), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.HeavyFuel.getFluid(10L), new FluidStack(ItemList.sToluene, 4), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new FluidStack(ItemList.sToluene, 30), Materials.LightFuel.getFluid(30L), 16, 24, false);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Ball.get(0L), new FluidStack(ItemList.sToluene, 100), ItemList.GelledToluene.get(1), 100, 18);
        RA.addChemicalRecipe(ItemList.GelledToluene.get(4), GT_Values.NI, Materials.SulfuricAcid.getFluid(250), GT_Values.NF, new ItemStack(Blocks.tnt, 1), 200, 24);

        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Ball.get(0L), Materials.ReinforcedGlass.getMolten(288), ItemList.Circuit_Parts_Reinforced_Glass_Tube.get(1), 400, 1920);


        RA.addChemicalRecipe(new ItemStack(Items.sugar, 8), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plastic, 1), new FluidStack(ItemList.sToluene, 800), GT_Values.NF, ItemList.GelledToluene.get(16), 200, 192);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SulfuricAcid, 1), null, null, null, new FluidStack(ItemList.sNitricAcid, 1000), new FluidStack(ItemList.sNitrationMixture, 2000), ItemList.Cell_Empty.get(1), 480, 2);
        RA.addChemicalRecipe(ItemList.GelledToluene.get(4), GT_Values.NI, new FluidStack(ItemList.sNitrationMixture, 200), Materials.DilutedSulfuricAcid.getFluid(200), GT_ModHandler.getIC2Item("industrialTnt", 1L), 80, 480);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(4), Materials.NatruralGas.getGas(16000), Materials.Gas.getGas(16000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.NatruralGas, 16L), GT_Utility.getIntegratedCircuit(4), Materials.Hydrogen.getGas(1000), Materials.HydricSulfide.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Gas, 16L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(4), Materials.SulfuricGas.getGas(16000), Materials.Gas.getGas(16000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SulfuricGas, 16L), GT_Utility.getIntegratedCircuit(4), Materials.Hydrogen.getGas(1000), Materials.HydricSulfide.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Gas, 16L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(4), Materials.SulfuricNaphtha.getFluid(12000), Materials.Naphtha.getFluid(12000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SulfuricNaphtha, 12L), GT_Utility.getIntegratedCircuit(4), Materials.Hydrogen.getGas(1000), Materials.HydricSulfide.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Naphtha, 12L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(4), Materials.SulfuricLightFuel.getFluid(12000), Materials.LightFuel.getFluid(12000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SulfuricLightFuel, 12L), GT_Utility.getIntegratedCircuit(4), Materials.Hydrogen.getGas(1000), Materials.HydricSulfide.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.LightFuel, 12L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(4), Materials.SulfuricHeavyFuel.getFluid(8000), Materials.HeavyFuel.getFluid(8000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SulfuricHeavyFuel, 8L), GT_Utility.getIntegratedCircuit(4), Materials.Hydrogen.getGas(1000), Materials.HydricSulfide.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HeavyFuel, 8L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(4), Materials.SulfuricTar.getFluid(10000), Materials.Tar.getFluid(10000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), 275);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SulfuricTar, 10L), GT_Utility.getIntegratedCircuit(4), Materials.Hydrogen.getGas(1000), Materials.HydricSulfide.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Tar, 10L), 275);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Saltpeter, 8L), null, Materials.Naphtha.getFluid(4608), Materials.Polycaprolactam.getMolten(10368), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Potassium, 1), 640);
        RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Polycaprolactam, 1L), new ItemStack(Items.string, 32), 80, 75);

        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.Creosote.getFluid(3L), Materials.Lubricant.getFluid(1L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.SeedOil.getFluid(4L), Materials.Lubricant.getFluid(1L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.FishOil.getFluid(3L), Materials.Lubricant.getFluid(1L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Biomass.getFluid(40L), Materials.Ethanol.getFluid(12L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 5L), Materials.Biomass.getFluid(40L), Materials.Water.getFluid(12L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 5L), Materials.Water.getFluid(5L), GT_ModHandler.getDistilledWater(5L), 16, 10, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.potatojuice", 2), FluidRegistry.getFluidStack("potion.vodka", 1), 16, 12, true);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.lemonade", 2), FluidRegistry.getFluidStack("potion.alcopops", 1), 16, 12, true);

        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.OilLight.getFluid(300L), Materials.Oil.getFluid(100L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.OilMedium.getFluid(200L), Materials.Oil.getFluid(100L), 16, 24, false);
        RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 4L), Materials.OilHeavy.getFluid(100L), Materials.Oil.getFluid(100L), 16, 24, false);

        RA.addFluidHeaterRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Water.getFluid(6L), Materials.Water.getGas(960L), 30, 30);
        RA.addFluidHeaterRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), GT_ModHandler.getDistilledWater(6L), Materials.Water.getGas(960L), 30, 30);
        RA.addFluidHeaterRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.SeedOil.getFluid(16L), Materials.FryingOilHot.getFluid(16L), 16, 30);
        RA.addFluidHeaterRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.FishOil.getFluid(16L), Materials.FryingOilHot.getFluid(16L), 16, 30);

        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Talc, 1L), FluidRegistry.getFluid("oil"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Soapstone, 1L), FluidRegistry.getFluid("oil"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), FluidRegistry.getFluid("oil"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Talc, 1L), FluidRegistry.getFluid("creosote"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Soapstone, 1L), FluidRegistry.getFluid("creosote"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), FluidRegistry.getFluid("creosote"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Talc, 1L), FluidRegistry.getFluid("seedoil"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Soapstone, 1L), FluidRegistry.getFluid("seedoil"), FluidRegistry.getFluid("lubricant"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), FluidRegistry.getFluid("seedoil"), FluidRegistry.getFluid("lubricant"), false);
        for (Fluid tFluid : new Fluid[]{FluidRegistry.WATER, GT_ModHandler.getDistilledWater(1L).getFluid()}) {
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Milk, 1L), tFluid, FluidRegistry.getFluid("milk"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wheat, 1L), tFluid, FluidRegistry.getFluid("potion.wheatyjuice"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Potassium, 1L), tFluid, FluidRegistry.getFluid("potion.mineralwater"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), tFluid, FluidRegistry.getFluid("potion.mineralwater"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), tFluid, FluidRegistry.getFluid("potion.mineralwater"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 1L), tFluid, FluidRegistry.getFluid("potion.mineralwater"), false);
            //Disabled in favor of a different type of Salt Water
//            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Salt, 1L), tFluid, FluidRegistry.getFluid("potion.saltywater"), true);
//            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RockSalt, 1L), tFluid, FluidRegistry.getFluid("potion.saltywater"), true);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 1L), tFluid, FluidRegistry.getFluid("potion.thick"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Blaze, 1L), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(new ItemStack(Items.magma_cream, 1, 0), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(new ItemStack(Items.fermented_spider_eye, 1, 0), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(new ItemStack(Items.spider_eye, 1, 0), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(new ItemStack(Items.speckled_melon, 1, 0), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(new ItemStack(Items.ghast_tear, 1, 0), tFluid, FluidRegistry.getFluid("potion.mundane"), false);
            RA.addBrewingRecipe(new ItemStack(Items.nether_wart, 1, 0), tFluid, FluidRegistry.getFluid("potion.awkward"), false);
            RA.addBrewingRecipe(new ItemStack(Blocks.red_mushroom, 1, 0), tFluid, FluidRegistry.getFluid("potion.poison"), false);
            RA.addBrewingRecipe(new ItemStack(Items.fish, 1, 3), tFluid, FluidRegistry.getFluid("potion.poison.strong"), true);
            RA.addBrewingRecipe(ItemList.IC2_Grin_Powder.get(1L), tFluid, FluidRegistry.getFluid("potion.poison.strong"), false);
            RA.addBrewingRecipe(new ItemStack(Items.reeds, 1, 0), tFluid, FluidRegistry.getFluid("potion.reedwater"), false);
            RA.addBrewingRecipe(new ItemStack(Items.apple, 1, 0), tFluid, FluidRegistry.getFluid("potion.applejuice"), false);
            RA.addBrewingRecipe(new ItemStack(Items.golden_apple, 1, 0), tFluid, FluidRegistry.getFluid("potion.goldenapplejuice"), true);
            RA.addBrewingRecipe(new ItemStack(Items.golden_apple, 1, 1), tFluid, FluidRegistry.getFluid("potion.idunsapplejuice"), true);
            RA.addBrewingRecipe(ItemList.IC2_Hops.get(1L), tFluid, FluidRegistry.getFluid("potion.hopsjuice"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coffee, 1L), tFluid, FluidRegistry.getFluid("potion.darkcoffee"), false);
            RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chili, 1L), tFluid, FluidRegistry.getFluid("potion.chillysauce"), false);
            RA.addBrewingRecipe(GT_ModHandler.getIC2Item("biochaff", 1), tFluid, Materials.Biomass.mFluid, false);

            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphate, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 3L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(1L), 100);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(1L), 100);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(4L), 400);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphate, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 3L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Apatite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Apatite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(4L), 400);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Apatite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphate, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Apatite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 3L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Apatite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glauconite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glauconite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(4L), 400);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glauconite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphate, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glauconite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 3L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glauconite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            if (Materials.GlauconiteSand.mHasParentMod) {
                RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GlauconiteSand, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
                RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GlauconiteSand, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(4L), 400);
                RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GlauconiteSand, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphate, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(3L), 300);
                RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GlauconiteSand, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 3L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
                RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GlauconiteSand, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new FluidStack(tFluid, 1000), GT_Values.NF, ItemList.IC2_Fertilizer.get(2L), 275);
            }
        }
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chili, 1L), FluidRegistry.getFluid("potion.chillysauce"), FluidRegistry.getFluid("potion.hotsauce"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chili, 1L), FluidRegistry.getFluid("potion.hotsauce"), FluidRegistry.getFluid("potion.diabolosauce"), true);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chili, 1L), FluidRegistry.getFluid("potion.diabolosauce"), FluidRegistry.getFluid("potion.diablosauce"), true);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coffee, 1L), FluidRegistry.getFluid("milk"), FluidRegistry.getFluid("potion.coffee"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cocoa, 1L), FluidRegistry.getFluid("milk"), FluidRegistry.getFluid("potion.darkchocolatemilk"), false);
        RA.addBrewingRecipe(ItemList.IC2_Hops.get(1L), FluidRegistry.getFluid("potion.wheatyjuice"), FluidRegistry.getFluid("potion.wheatyhopsjuice"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wheat, 1L), FluidRegistry.getFluid("potion.hopsjuice"), FluidRegistry.getFluid("potion.wheatyhopsjuice"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.tea"), FluidRegistry.getFluid("potion.sweettea"), true);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.coffee"), FluidRegistry.getFluid("potion.cafeaulait"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.cafeaulait"), FluidRegistry.getFluid("potion.laitaucafe"), true);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.lemonjuice"), FluidRegistry.getFluid("potion.lemonade"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.darkcoffee"), FluidRegistry.getFluid("potion.darkcafeaulait"), true);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.darkchocolatemilk"), FluidRegistry.getFluid("potion.chocolatemilk"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ice, 1L), FluidRegistry.getFluid("potion.tea"), FluidRegistry.getFluid("potion.icetea"), false);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 1L), FluidRegistry.getFluid("potion.lemonade"), FluidRegistry.getFluid("potion.cavejohnsonsgrenadejuice"), true);
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L), FluidRegistry.getFluid("potion.mundane"), FluidRegistry.getFluid("potion.purpledrink"), true);
        RA.addBrewingRecipe(new ItemStack(Items.fermented_spider_eye, 1, 0), FluidRegistry.getFluid("potion.mundane"), FluidRegistry.getFluid("potion.weakness"), false);
        RA.addBrewingRecipe(new ItemStack(Items.fermented_spider_eye, 1, 0), FluidRegistry.getFluid("potion.thick"), FluidRegistry.getFluid("potion.weakness"), false);

        RA.addBrewingRecipe(GT_ModHandler.getModItem(aTextForestry, "mulch", 16L, 0), FluidRegistry.WATER, FluidRegistry.getFluid("biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getModItem(aTextForestry, "fertilizerBio", 16L, 0), FluidRegistry.WATER, FluidRegistry.getFluid("biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getModItem(aTextForestry, "fertilizerCompound", 16L, 0), FluidRegistry.WATER, FluidRegistry.getFluid("biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getModItem(aTextForestry, "mulch", 12L, 0), GT_ModHandler.getDistilledWater(750L).getFluid(), FluidRegistry.getFluid("biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getModItem(aTextForestry, "fertilizerBio", 12L, 0), GT_ModHandler.getDistilledWater(750L).getFluid(), FluidRegistry.getFluid("biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getModItem(aTextForestry, "fertilizerCompound", 12L, 0), GT_ModHandler.getDistilledWater(750L).getFluid(), FluidRegistry.getFluid("biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getIC2Item("biochaff", 1), FluidRegistry.WATER, FluidRegistry.getFluid("ic2biomass"), false);
        RA.addBrewingRecipe(GT_ModHandler.getIC2Item("biochaff", 1), GT_ModHandler.getDistilledWater(750L).getFluid(), FluidRegistry.getFluid("ic2biomass"), false);

        addPotionRecipes("waterbreathing", new ItemStack(Items.fish, 1, 3));
        addPotionRecipes("fireresistance", new ItemStack(Items.magma_cream, 1, 0));
        addPotionRecipes("nightvision", new ItemStack(Items.golden_carrot, 1, 0));
        addPotionRecipes("weakness", new ItemStack(Items.fermented_spider_eye, 1, 0));
        addPotionRecipes("poison", new ItemStack(Items.spider_eye, 1, 0));
        addPotionRecipes("health", new ItemStack(Items.speckled_melon, 1, 0));
        addPotionRecipes("regen", new ItemStack(Items.ghast_tear, 1, 0));
        addPotionRecipes("speed", GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sugar, 1L));
        addPotionRecipes("strength", GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Blaze, 1L));

        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("milk", 50), FluidRegistry.getFluidStack("potion.mundane", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.lemonjuice", 50), FluidRegistry.getFluidStack("potion.limoncello", 25), 1020, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.applejuice", 50), FluidRegistry.getFluidStack("potion.cider", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.goldenapplejuice", 50), FluidRegistry.getFluidStack("potion.goldencider", 25), 1020, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.idunsapplejuice", 50), FluidRegistry.getFluidStack("potion.notchesbrew", 25), 1020, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.reedwater", 50), FluidRegistry.getFluidStack("potion.rum", 25), 1020, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.rum", 50), FluidRegistry.getFluidStack("potion.piratebrew", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.grapejuice", 50), FluidRegistry.getFluidStack("potion.wine", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.wheatyjuice", 50), FluidRegistry.getFluidStack("potion.scotch", 25), 1020, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.scotch", 50), FluidRegistry.getFluidStack("potion.glenmckenner", 10), 2048, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.wheatyhopsjuice", 50), FluidRegistry.getFluidStack("potion.beer", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.hopsjuice", 50), FluidRegistry.getFluidStack("potion.darkbeer", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.darkbeer", 50), FluidRegistry.getFluidStack("potion.dragonblood", 10), 2048, true);

        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.beer", 75), FluidRegistry.getFluidStack("potion.vinegar", 50), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.cider", 75), FluidRegistry.getFluidStack("potion.vinegar", 50), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.goldencider", 75), FluidRegistry.getFluidStack("potion.vinegar", 50), 2048, true);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.rum", 75), FluidRegistry.getFluidStack("potion.vinegar", 50), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.wine", 75), FluidRegistry.getFluidStack("potion.vinegar", 50), 2048, false);

        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.awkward", 50), FluidRegistry.getFluidStack("potion.weakness", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.mundane", 50), FluidRegistry.getFluidStack("potion.weakness", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.thick", 50), FluidRegistry.getFluidStack("potion.weakness", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.poison", 50), FluidRegistry.getFluidStack("potion.damage", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.health", 50), FluidRegistry.getFluidStack("potion.damage", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.waterbreathing", 50), FluidRegistry.getFluidStack("potion.damage", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.nightvision", 50), FluidRegistry.getFluidStack("potion.invisibility", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.fireresistance", 50), FluidRegistry.getFluidStack("potion.slowness", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.speed", 50), FluidRegistry.getFluidStack("potion.slowness", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.strength", 50), FluidRegistry.getFluidStack("potion.weakness", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.regen", 50), FluidRegistry.getFluidStack("potion.poison", 25), 1020, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.poison.strong", 50), FluidRegistry.getFluidStack("potion.damage.strong", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.health.strong", 50), FluidRegistry.getFluidStack("potion.damage.strong", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.speed.strong", 50), FluidRegistry.getFluidStack("potion.slowness.strong", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.strength.strong", 50), FluidRegistry.getFluidStack("potion.weakness.strong", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.nightvision.long", 50), FluidRegistry.getFluidStack("potion.invisibility.long", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.regen.strong", 50), FluidRegistry.getFluidStack("potion.poison.strong", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.poison.long", 50), FluidRegistry.getFluidStack("potion.damage.long", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.waterbreathing.long", 50), FluidRegistry.getFluidStack("potion.damage.long", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.fireresistance.long", 50), FluidRegistry.getFluidStack("potion.slowness.long", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.speed.long", 50), FluidRegistry.getFluidStack("potion.slowness.long", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.strength.long", 50), FluidRegistry.getFluidStack("potion.weakness.long", 10), 2048, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), FluidRegistry.getFluidStack("potion.regen.long", 50), FluidRegistry.getFluidStack("potion.poison.long", 10), 2048, false);

        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_PotatoChips.get(1L), ItemList.Food_PotatoChips.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Potato_On_Stick.get(1L), ItemList.Food_Potato_On_Stick_Roasted.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Bun.get(1L), ItemList.Food_Baked_Bun.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Bread.get(1L), ItemList.Food_Baked_Bread.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Baguette.get(1L), ItemList.Food_Baked_Baguette.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Pizza_Veggie.get(1L), ItemList.Food_Baked_Pizza_Veggie.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Pizza_Cheese.get(1L), ItemList.Food_Baked_Pizza_Cheese.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Pizza_Meat.get(1L), ItemList.Food_Baked_Pizza_Meat.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Baguette.get(1L), ItemList.Food_Baked_Baguette.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Cake.get(1L), ItemList.Food_Baked_Cake.get(1L));
        GT_ModHandler.addSmeltingRecipe(ItemList.Food_Raw_Cookie.get(1L), new ItemStack(Items.cookie, 1));
        GT_ModHandler.addSmeltingRecipe(new ItemStack(Items.slime_ball, 1), ItemList.IC2_Resin.get(1L));

        GT_ModHandler.addExtractionRecipe(new ItemStack(Blocks.bookshelf, 1, 32767), new ItemStack(Items.book, 3, 0));
        GT_ModHandler.addExtractionRecipe(new ItemStack(Items.slime_ball, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 2L));
        GT_ModHandler.addExtractionRecipe(ItemList.IC2_Resin.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 3L));
        GT_ModHandler.addExtractionRecipe(GT_ModHandler.getIC2Item("rubberSapling", 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 1L));
        GT_ModHandler.addExtractionRecipe(GT_ModHandler.getIC2Item("rubberLeaves", 16L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Cell_Air.get(1L), ItemList.Cell_Empty.get(1L));
        if (Loader.isModLoaded(aTextEBXL)) {
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "waterplant1", 1, 0), new ItemStack(Items.dye, 4, 2));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "vines", 1, 0), new ItemStack(Items.dye, 4, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 11), new ItemStack(Items.dye, 4, 11));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 10), new ItemStack(Items.dye, 4, 5));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 9), new ItemStack(Items.dye, 4, 14));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 8), new ItemStack(Items.dye, 4, 14));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 7), new ItemStack(Items.dye, 4, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 6), new ItemStack(Items.dye, 4, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 5), new ItemStack(Items.dye, 4, 11));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 0), new ItemStack(Items.dye, 4, 9));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 4), new ItemStack(Items.dye, 4, 11));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 3), new ItemStack(Items.dye, 4, 13));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower1", 1, 3), new ItemStack(Items.dye, 4, 5));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 2), new ItemStack(Items.dye, 4, 5));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower1", 1, 1), new ItemStack(Items.dye, 4, 12));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 15), new ItemStack(Items.dye, 4, 11));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 14), new ItemStack(Items.dye, 4, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 13), new ItemStack(Items.dye, 4, 9));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 12), new ItemStack(Items.dye, 4, 14));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 11), new ItemStack(Items.dye, 4, 7));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower1", 1, 7), new ItemStack(Items.dye, 4, 7));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower1", 1, 2), new ItemStack(Items.dye, 4, 11));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 13), new ItemStack(Items.dye, 4, 6));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 6), new ItemStack(Items.dye, 4, 12));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 5), new ItemStack(Items.dye, 4, 10));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 2), new ItemStack(Items.dye, 4, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 1), new ItemStack(Items.dye, 4, 9));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 0), new ItemStack(Items.dye, 4, 13));

            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 7), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 0));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 1), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower3", 1, 12), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 4), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 1));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower1", 1, 6), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 2));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 8), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 3));
            GT_ModHandler.addExtractionRecipe(GT_ModHandler.getModItem(aTextEBXL, "flower2", 1, 3), GT_ModHandler.getModItem(aTextEBXL, "extrabiomes.dye", 1, 3));

            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 0), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 1), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 2), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 3), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 4), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 5), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 6), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_1", 4, 7), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_2", 4, 0), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_2", 4, 1), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_2", 4, 2), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_2", 4, 3), ItemList.IC2_Plantball.get(1));
            GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextEBXL, "saplings_2", 4, 4), ItemList.IC2_Plantball.get(1));

        }

        if (Loader.isModLoaded("BiomesOPlenty")) {
            RA.addExtractorRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "flowers", 2L, 3), new ItemStack(Items.glowstone_dust, 1, 0), 300, 2);
            RA.addExtractorRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "mushrooms", 2L, 3), new ItemStack(Items.glowstone_dust, 1, 0), 300, 2);
            RA.addExtractorRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "coral1", 2L, 15), new ItemStack(Items.glowstone_dust, 1, 0), 300, 2);
            RA.addExtractorRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "flowers", 2L, 13), new ItemStack(Items.spider_eye, 1, 0), 300, 2);
            RA.addExtractorRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "mudball", 1L, 0), new ItemStack(Items.clay_ball, 1, 0), 200, 2);
        }

        GT_ModHandler.addCompressionRecipe(ItemList.IC2_Compressed_Coal_Chunk.get(1L), ItemList.IC2_Industrial_Diamond.get(1L));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), GT_ModHandler.getIC2Item("Uran238", 1L));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium235, 1L), GT_ModHandler.getIC2Item("Uran235", 1L));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 1L), GT_ModHandler.getIC2Item("Plutonium", 1L));
        //GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dustTiny, Materials.Uranium235, 1L), GT_ModHandler.getIC2Item("smallUran235", 1L));
        //GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dustTiny, Materials.Plutonium, 1L), GT_ModHandler.getIC2Item("smallPlutonium", 1L));
        GT_ModHandler.addCompressionRecipe(new ItemStack(Blocks.ice, 2, 32767), new ItemStack(Blocks.packed_ice, 1, 0));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ice, 1L), new ItemStack(Blocks.ice, 1, 0));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.CertusQuartz, 4L), GT_ModHandler.getModItem(aTextAE, "tile.BlockQuartz", 1L));
        GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 8L, 10), GT_ModHandler.getModItem(aTextAE, "tile.BlockQuartz", 1L));
        GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 8L, 11), new ItemStack(Blocks.quartz_block, 1, 0));
        GT_ModHandler.addCompressionRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 8L, 12), GT_ModHandler.getModItem(aTextAE, "tile.BlockFluix", 1L));
        GT_ModHandler.addCompressionRecipe(new ItemStack(Items.quartz, 4, 0), new ItemStack(Blocks.quartz_block, 1, 0));
        GT_ModHandler.addCompressionRecipe(new ItemStack(Items.wheat, 9, 0), new ItemStack(Blocks.hay_block, 1, 0));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 4L), new ItemStack(Blocks.glowstone, 1));
        GT_ModHandler.addCompressionRecipe(Materials.Fireclay.getDust(1), ItemList.CompressedFireclay.get(1));

        GameRegistry.addSmelting(ItemList.CompressedFireclay.get(1), ItemList.Firebrick.get(1), 0);

        RA.addCutterRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Graphite, 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Graphite, 9L), GT_Values.NI, 500, 75);
        GT_ModHandler.removeFurnaceSmelting(GT_OreDictUnificator.get(OrePrefixes.ore, Materials.Graphite, 1L));
        GT_ModHandler.addSmeltingRecipe(GT_OreDictUnificator.get(OrePrefixes.ore, Materials.Graphite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Graphite, 1L));
        GT_ModHandler.removeFurnaceSmelting(GT_OreDictUnificator.get(OrePrefixes.oreBlackgranite, Materials.Graphite, 1L));
        GT_ModHandler.addSmeltingRecipe(GT_OreDictUnificator.get(OrePrefixes.oreBlackgranite, Materials.Graphite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Graphite, 1L));
        GT_ModHandler.removeFurnaceSmelting(GT_OreDictUnificator.get(OrePrefixes.oreEndstone, Materials.Graphite, 1L));
        GT_ModHandler.addSmeltingRecipe(GT_OreDictUnificator.get(OrePrefixes.oreEndstone, Materials.Graphite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Graphite, 1L));
        GT_ModHandler.removeFurnaceSmelting(GT_OreDictUnificator.get(OrePrefixes.oreNetherrack, Materials.Graphite, 1L));
        GT_ModHandler.addSmeltingRecipe(GT_OreDictUnificator.get(OrePrefixes.oreNetherrack, Materials.Graphite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Graphite, 1L));
        GT_ModHandler.removeFurnaceSmelting(GT_OreDictUnificator.get(OrePrefixes.oreRedgranite, Materials.Graphite, 1L));
        GT_ModHandler.addSmeltingRecipe(GT_OreDictUnificator.get(OrePrefixes.oreRedgranite, Materials.Graphite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Graphite, 1L));

        GT_ModHandler.addPulverisationRecipe(GT_ModHandler.getModItem(aTextAE, "tile.BlockSkyStone", 1L, 32767), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 1L, 45), GT_Values.NI, 0, false);
        GT_ModHandler.addPulverisationRecipe(GT_ModHandler.getModItem(aTextAE, "tile.BlockSkyChest", 1L, 32767), GT_ModHandler.getModItem(aTextAE, aTextAEMM, 8L, 45), GT_Values.NI, 0, false);
        GT_ModHandler.addPulverisationRecipe(new ItemStack(Items.blaze_rod, 1), new ItemStack(Items.blaze_powder, 3), new ItemStack(Items.blaze_powder, 1), 50, false);
        GT_ModHandler.addPulverisationRecipe(new ItemStack(Items.flint, 2, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 1L), 5, true);
        GT_ModHandler.addPulverisationRecipe(new ItemStack(Blocks.red_mushroom, 1, 32767), ItemList.IC2_Grin_Powder.get(1L));

        RA.addForgeHammerRecipe(new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stonebrick, 1, 2), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.stone, 1, 0), new ItemStack(Blocks.cobblestone, 1, 0), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.cobblestone, 1, 0), new ItemStack(Blocks.gravel, 1, 0), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.gravel, 1, 0), new ItemStack(Blocks.sand, 1, 0), 10, 18);
        RA.addSifterRecipe(new ItemStack(Blocks.gravel, 1, 0), new ItemStack[]{new ItemStack(Items.flint, 1, 0)}, new int[]{10000}, 800, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.sandstone, 1, 32767), new ItemStack(Blocks.sand, 1, 0), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.ice, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ice, 1L), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.packed_ice, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ice, 2L), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.hardened_clay, 1, 0), new ItemStack(Items.clay_ball, 2), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.stained_hardened_clay, 1, 32767), new ItemStack(Items.clay_ball, 2), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.brick_block, 1, 0), new ItemStack(Items.brick, 3, 0), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.nether_brick, 1, 0), new ItemStack(Items.netherbrick, 3, 0), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.stained_glass, 1, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1L), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.glass, 1, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1L), 10, 18);
        RA.addForgeHammerRecipe(GT_ModHandler.getModItem("IC2", "blockAlloyGlass", 1L, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.ReinforcedGlass, 1L), 200, 1920);

        RA.addForgeHammerRecipe(new ItemStack(Blocks.stained_glass_pane, 3, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1L), 10, 18);
        RA.addForgeHammerRecipe(new ItemStack(Blocks.glass_pane, 3, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1L), 10, 18);
        RA.addForgeHammerRecipe(Materials.Brick.getIngots(4), Materials.Brick.getDust(1), 10, 18);
        RA.addForgeHammerRecipe(ItemList.Firebrick.get(1), Materials.Brick.getDust(1), 10, 18);
        RA.addForgeHammerRecipe(ItemList.Casing_Firebricks.get(1), ItemList.Firebrick.get(3), 10, 18);

        GT_ModHandler.addPulverisationRecipe(Materials.Brick.getIngots(4), Materials.Brick.getDust(1));
        GT_ModHandler.addPulverisationRecipe(ItemList.CompressedFireclay.get(4), Materials.Fireclay.getDust(1));
        GT_ModHandler.addPulverisationRecipe(ItemList.Firebrick.get(1), Materials.Brick.getDust(1));
        GT_ModHandler.addPulverisationRecipe(ItemList.Casing_Firebricks.get(1), Materials.Brick.getDust(4));
        GT_ModHandler.addPulverisationRecipe(ItemList.Machine_Bricked_BlastFurnace.get(1), Materials.Brick.getDust(8), Materials.Iron.getDust(1), true);
        GT_ModHandler.addPulverisationRecipe(new ItemStack(Blocks.brick_stairs, 4, 0), Materials.Brick.getDust(6));

        RA.addAmplifier(ItemList.IC2_Scrap.get(9L), 180, 1);
        RA.addAmplifier(ItemList.IC2_Scrapbox.get(1L), 180, 1);

        RA.addBoxingRecipe(ItemList.IC2_Scrap.get(9L), ItemList.Schematic_3by3.get(0L), ItemList.IC2_Scrapbox.get(1L), 16, 1);
        RA.addBoxingRecipe(ItemList.Food_Fries.get(1L), GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Paper, 1L), ItemList.Food_Packaged_Fries.get(1L), 64, 18);
        RA.addBoxingRecipe(ItemList.Food_PotatoChips.get(1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 1L), ItemList.Food_Packaged_PotatoChips.get(1L), 64, 18);
        RA.addBoxingRecipe(ItemList.Food_ChiliChips.get(1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 1L), ItemList.Food_Packaged_ChiliChips.get(1L), 64, 18);

        if (!GregTech_API.mIC2Classic) {
            RA.addFluidExtractionRecipe(GT_ModHandler.getIC2Item("TritiumCell", 1), GT_ModHandler.getIC2Item("fuelRod", 1), Materials.Tritium.getGas(32), 10000, 16, 75);
            RA.addCannerRecipe(GT_ModHandler.getIC2Item("fuelRod", 1), GT_ModHandler.getIC2Item("UranFuel", 1), ItemList.Uraniumcell_1.get(1), null, 30, 18);
            RA.addCannerRecipe(GT_ModHandler.getIC2Item("fuelRod", 1), GT_ModHandler.getIC2Item("MOXFuel", 1), ItemList.Moxcell_1.get(1), null, 30, 18);
        }
        RA.addFusionReactorRecipe(Materials.Lithium.getMolten(16), Materials.Tungsten.getMolten(16), Materials.Iridium.getMolten(16), 32, 32768, 300000000);
        RA.addFusionReactorRecipe(Materials.Deuterium.getGas(125), Materials.Tritium.getGas(125), Materials.Helium.getPlasma(125), 16, 4096, 40000000);  //Mark 1 Cheap //
        RA.addFusionReactorRecipe(Materials.Deuterium.getGas(125), Materials.Helium_3.getGas(125), Materials.Helium.getPlasma(125), 16, 2048, 60000000); //Mark 1 Expensive //
        RA.addFusionReactorRecipe(Materials.Carbon.getMolten(125), Materials.Helium_3.getGas(125), Materials.Oxygen.getPlasma(125), 32, 4096, 80000000); //Mark 1 Expensive //
        RA.addFusionReactorRecipe(Materials.Aluminium.getMolten(16), Materials.Lithium.getMolten(16), Materials.Sulfur.getPlasma(125), 32, 10240, 240000000); //Mark 2 Cheap
        RA.addFusionReactorRecipe(Materials.Beryllium.getMolten(16), Materials.Deuterium.getGas(375), Materials.Nitrogen.getPlasma(175), 16, 16384, 180000000); //Mark 2 Expensive //
        RA.addFusionReactorRecipe(Materials.Silicon.getMolten(16), Materials.Magnesium.getMolten(16), Materials.Iron.getPlasma(125), 32, 8192, 360000000); //Mark 3 Cheap //
        RA.addFusionReactorRecipe(Materials.Potassium.getMolten(16), Materials.Fluorine.getGas(125), Materials.Nickel.getPlasma(125), 16, 32768, 480000000); //Mark 3 Expensive //
        RA.addFusionReactorRecipe(Materials.Beryllium.getMolten(16), Materials.Tungsten.getMolten(16), Materials.Platinum.getMolten(16), 32, 32768, 150000000); //
        RA.addFusionReactorRecipe(Materials.Neodymium.getMolten(16), Materials.Hydrogen.getGas(48), Materials.Europium.getMolten(16), 64, 24576, 150000000); //
        RA.addFusionReactorRecipe(Materials.Lutetium.getMolten(32), Materials.Chrome.getMolten(32), Materials.Americium.getMolten(32), 64, 49152, 200000000); //
        RA.addFusionReactorRecipe(Materials.Plutonium.getMolten(16), Materials.Thorium.getMolten(16), Materials.Naquadah.getMolten(16), 64, 32768, 300000000); //
        RA.addFusionReactorRecipe(Materials.Tungsten.getMolten(48), Materials.Tritanium.getMolten(32), Materials.Infuscolium.getMolten(16), 64, 49152, 400000000); //
        RA.addFusionReactorRecipe(Materials.Americium.getMolten(128), Materials.Naquadria.getMolten(128), Materials.Neutronium.getMolten(32), 60, 98304, 600000000); //
        RA.addFusionReactorRecipe(Materials.Glowstone.getMolten(16), Materials.Helium.getPlasma(4), Materials.Sunnarium.getMolten(16), 32, 7680, 40000000); //Mark 1 Expensive //

        RA.addFusionReactorRecipe(Materials.Tungsten.getMolten(16), Materials.Helium.getGas(16), Materials.Osmium.getMolten(16), 64, 24578, 150000000); //
        RA.addFusionReactorRecipe(Materials.Manganese.getMolten(16), Materials.Hydrogen.getGas(16), Materials.Iron.getMolten(16), 64, 8192, 120000000); //
        RA.addFusionReactorRecipe(Materials.Mercury.getFluid(16), Materials.Magnesium.getMolten(16), Materials.Uranium.getMolten(16), 64, 49152, 240000000); //
        RA.addFusionReactorRecipe(Materials.Gold.getMolten(16), Materials.Aluminium.getMolten(16), Materials.Uranium.getMolten(16), 64, 49152, 240000000); //
        RA.addFusionReactorRecipe(Materials.Uranium.getMolten(16), Materials.Helium.getGas(16), Materials.Plutonium.getMolten(16), 128, 49152, 480000000); //
        RA.addFusionReactorRecipe(Materials.Vanadium.getMolten(16), Materials.Hydrogen.getGas(125), Materials.Chrome.getMolten(16), 64, 24576, 140000000); //
        RA.addFusionReactorRecipe(Materials.Gallium.getMolten(16), Materials.Radon.getGas(125), Materials.Duranium.getMolten(16), 64, 16384, 140000000); //
        RA.addFusionReactorRecipe(Materials.Titanium.getMolten(48), Materials.Duranium.getMolten(32), Materials.Tritanium.getMolten(16), 64, 32768, 200000000); //
        RA.addFusionReactorRecipe(Materials.Gold.getMolten(16), Materials.Mercury.getFluid(16), Materials.Radon.getGas(125), 64, 32768, 200000000); //
        RA.addFusionReactorRecipe(Materials.Tantalum.getMolten(16), Materials.Tritium.getGas(16), Materials.Tungsten.getMolten(16), 16, 24576, 200000000); //
        RA.addFusionReactorRecipe(Materials.Silver.getMolten(144), Materials.Lithium.getMolten(144), Materials.Indium.getMolten(144), 16, 24576, 380000000); //
        RA.addFusionReactorRecipe(Materials.NaquadahEnriched.getMolten(15), Materials.Radon.getGas(125), Materials.Naquadria.getMolten(3), 64, 49152, 400000000); //

        RA.addFusionReactorRecipe(Materials.Magnesium.getMolten(144), Materials.Carbon.getMolten(144), Materials.Argon.getPlasma(125), 32, 24576, 180000000);//FT1+ - utility

        RA.addFusionReactorRecipe(Materials.Copper.getMolten(72), Materials.Tritium.getGas(250), Materials.Zinc.getPlasma(72), 16, 49152, 180000000);//FT2 - farmable
        RA.addFusionReactorRecipe(Materials.Cobalt.getMolten(144), Materials.Silicon.getMolten(144), Materials.Niobium.getPlasma(144), 16, 49152, 200000000);//FT2 - utility
        RA.addFusionReactorRecipe(Materials.Gold.getMolten(144), Materials.Arsenic.getMolten(144), Materials.Silver.getPlasma(144), 16, 49152, 350000000);//FT2+
        RA.addFusionReactorRecipe(Materials.Silver.getMolten(144), Materials.Helium_3.getGas(375), Materials.Tin.getPlasma(144), 16, 49152, 280000000);//FT2
        RA.addFusionReactorRecipe(Materials.Tungsten.getMolten(144), Materials.Carbon.getMolten(144), Materials.Mercury.getPlasma(144), 16, 49152, 300000000);//FT2

        RA.addFusionReactorRecipe(Materials.Tantalum.getMolten(144), Materials.Zinc.getPlasma(72), Materials.Bismuth.getPlasma(144), 16, 98304, 350000000);//FT3 - farmable
        RA.addFusionReactorRecipe(Materials.Iridium.getMolten(144), Materials.Fluorine.getGas(500), Materials.Radon.getPlasma(144), 32, 98304, 450000000);//FT3 - utility
        RA.addFusionReactorRecipe(Materials.Plutonium241.getMolten(144), Materials.Hydrogen.getGas(2000), Materials.Americium.getPlasma(144), 64, 98304, 500000000);//FT3
        //RA.addFusionReactorRecipe(Materials.Neutronium.getMolten(144), Materials.Neutronium.getMolten(144), Materials.Neutronium.getPlasma(72), 64, 130000, 640000000);//FT3+ - yes it is a bit troll XD

        //Ti & O Plasma Recipes
        RA.addFusionReactorRecipe(Materials.Aluminium.getMolten(144), Materials.Fluorine.getGas(144), Materials.Titanium.getPlasma(144), 160, 49152, 100000000);
        RA.addFusionReactorRecipe(Materials.Helium.getPlasma(144), Materials.Lithium.getMolten(144), Materials.Boron.getPlasma(144), 240, 10240, 50000000);

        //MK4
        RA.addFusionReactorRecipe(Materials.Radon.getPlasma(64), Materials.Neutronium.getMolten(64), Materials.Phoenixite.getMolten(32), 80, 196608, 1000000000);//FT4


        GT_ModHandler.removeRecipeByOutput(ItemList.IC2_Fertilizer.get(1L));
        RA.addImplosionRecipe(ItemList.IC2_Compressed_Coal_Chunk.get(1L), 8, ItemList.IC2_Industrial_Diamond.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 1L), new int[]{10000, 1000});
        
        if (Loader.isModLoaded("GalacticraftMars")) {
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("GalacticraftCore", "tile.moonBlock", 1L, 5), null, null, Materials.Helium_3.getGas(33), new ItemStack(Blocks.sand, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Rutile, 1), new int[]{5000, 400, 400, 100, 100, 100}, 400, 6);
            RA.addFluidExtractionRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.mars", 1L, 9), new ItemStack(Blocks.stone, 1), Materials.Iron.getMolten(50), 10000, 250, 18);
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.asteroidsBlock", 1L, 0), null, null, Materials.Nitrogen.getGas(33), new ItemStack(Blocks.sand, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Nickel, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gallium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 1), new int[]{5000, 400, 400, 100, 100, 100}, 400, 6);
        }

        RA.addFluidExtractionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Quartzite, 1L), null, Materials.Glass.getMolten(72), 10000, 600, 28);//(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.SiliconDioxide,1L), GT_OreDictUnificator.get(OrePrefixes.dust,Materials.SiliconDioxide,2L),GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Glass,1L)/** GT_Utility.fillFluidContainer(Materials.Glass.getMolten(1000), ItemList.Cell_Empty.get(1), true, true)**/, 600, 18);

        RA.addDistillationTowerRecipe(Materials.Creosote.getFluid(24L), new FluidStack[]{Materials.Lubricant.getFluid(12L)}, null, 16, 96);
        RA.addDistillationTowerRecipe(Materials.SeedOil.getFluid(32L), new FluidStack[]{Materials.Lubricant.getFluid(12L)}, null, 16, 96);
        RA.addDistillationTowerRecipe(Materials.FishOil.getFluid(24L), new FluidStack[]{Materials.Lubricant.getFluid(12L)}, null, 16, 96);
        RA.addDistillationTowerRecipe(Materials.Biomass.getFluid(2400L), new FluidStack[]{Materials.Ethanol.getFluid(960L), Materials.Water.getFluid(960L)}, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1L), 24, 400);
        RA.addDistillationTowerRecipe(Materials.Water.getFluid(576), new FluidStack[]{GT_ModHandler.getDistilledWater(520L)}, null, 16, 120);


        if (!GregTech_API.mIC2Classic) {
            RA.addDistillationTowerRecipe(new FluidStack(FluidRegistry.getFluid("ic2biomass"), 2000), new FluidStack[]{new FluidStack(FluidRegistry.getFluid("ic2biogas"), 8000), Materials.Water.getFluid(125L)}, ItemList.IC2_Fertilizer.get(1), 200, 480);
            RA.addFuel(GT_ModHandler.getIC2Item("biogasCell", 1L), null, 84, 1);

            RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new FluidStack(FluidRegistry.getFluid("ic2biomass"), 12), new FluidStack(FluidRegistry.getFluid("ic2biogas"), 32), 40, 20, false);
            RA.addDistilleryRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 2L), new FluidStack(FluidRegistry.getFluid("ic2biomass"), 4), Materials.Water.getFluid(2), 80, 30, false);
        }

        RA.addFuel(new ItemStack(Items.golden_apple, 1, 1), new ItemStack(Items.apple, 1), 6400, 5);

        RA.addElectrolyzerRecipe(GT_Values.NI, ItemList.Cell_Empty.get(1L), Materials.Water.getFluid(3000L), Materials.Hydrogen.getGas(2000L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 2000, 30);
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(2), ItemList.Cell_Empty.get(1L), GT_ModHandler.getDistilledWater(3000L), Materials.Hydrogen.getGas(2000L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 1500, 30);
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(3), ItemList.Cell_Empty.get(2L), Materials.Water.getFluid(3000L), Materials.Oxygen.getGas(1000L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 2000, 30);
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(4), ItemList.Cell_Empty.get(2L), GT_ModHandler.getDistilledWater(3000L), Materials.Oxygen.getGas(1000L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 1500, 30);
        RA.addElectrolyzerRecipe(GT_ModHandler.getIC2Item("electrolyzedWaterCell", 3L), 0, GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 2L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 30, 30);
        RA.addElectrolyzerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Water, 1L), 0, GT_ModHandler.getIC2Item("electrolyzedWaterCell", 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, 490, 30);
        RA.addElectrolyzerRecipe(ItemList.Dye_Bonemeal.get(3L), 0, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, 98, 26);
        RA.addElectrolyzerRecipe(new ItemStack(Blocks.sand, 8), 0, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SiliconDioxide, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, 500, 25);
        RA.addElectrolyzerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 7L), GT_Values.NI, Materials.Hydrogen.getGas(7000L), Materials.Oxygen.getGas(4000L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungsten, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 10000, 0, 0, 0, 0}, 120, 1920);
        RA.addElectrolyzerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Scheelite, 7L), GT_Values.NI, Materials.Hydrogen.getGas(7000L), Materials.Oxygen.getGas(4000L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungsten, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 2L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 10000, 0, 0, 0, 0}, 120, 1920);
        //RA.addElectrolyzerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.CarbonDioxide, 4), GT_Values.NI, GT_Values.NF,GT_Values.NF,									   GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 3),   GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1), ItemList.Cell_Empty.get(3), GT_Values.NI, GT_Values.NI, GT_Values.NI,new int[]{10000,10000,10000,0,0,0}, 180, 75);
        RA.addElectrolyzerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Graphite, 1), 0, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 4), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, 100, 75);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NetherQuartz, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), Materials.Water.getFluid(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherQuartz, 3L), 500);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.CertusQuartz, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), Materials.Water.getFluid(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.CertusQuartz, 3L), 500);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Quartzite, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), Materials.Water.getFluid(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Quartzite, 3L), 500);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NetherQuartz, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_ModHandler.getDistilledWater(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherQuartz, 3L), 500);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.CertusQuartz, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_ModHandler.getDistilledWater(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.CertusQuartz, 3L), 500);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Quartzite, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 1L), GT_ModHandler.getDistilledWater(1000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Quartzite, 3L), 500);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 1L), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), 1000);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 1L), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), 1000);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1L), Materials.Oxygen.getGas(3000L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 5L), 500);
        RA.addChemicalRecipe(Materials.Carbon.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(4000L), Materials.Methane.getGas(5000L), GT_Values.NI, 14000);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Carbon.getDust(1), Materials.Empty.getCells(1), Materials.Hydrogen.getGas(4000L), GT_Values.NF, Materials.Methane.getCells(1), GT_Values.NI, 200, 30);
        RA.addChemicalRecipeForBasicMachineOnly(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 1L), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(2000L), GT_ModHandler.getDistilledWater(3000L), ItemList.Cell_Empty.get(1L), GT_Values.NI, 10, 30);
        RA.addChemicalRecipeForBasicMachineOnly(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(500L), GT_ModHandler.getDistilledWater(1500L), ItemList.Cell_Empty.get(1L), GT_Values.NI, 5, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(1)}, new FluidStack[]{Materials.Hydrogen.getGas(2000), Materials.Oxygen.getGas(1000)}, new FluidStack[]{GT_ModHandler.getDistilledWater(1000)}, new ItemStack[]{}, 10, 30);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Rutile, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Carbon, 2L), Materials.Chlorine.getGas(4000L), Materials.Titaniumtetrachloride.getFluid(1000L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.CarbonMonoxide, 2L), 500, 480);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Rutile, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 2L), Materials.Chlorine.getGas(4000L), Materials.Titaniumtetrachloride.getFluid(1000L), GT_Values.NI, 500, 480);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesiumchloride, 1L), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Salt, 2L), 300, 280);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 9L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_Values.NF, Materials.Rubber.getMolten(1296L), GT_Values.NI, 600, 18);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Gold, 8L), new ItemStack(Items.melon, 1, 32767), new ItemStack(Items.speckled_melon, 1, 0), 50);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Gold, 8L), new ItemStack(Items.carrot, 1, 32767), new ItemStack(Items.golden_carrot, 1, 0), 50);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Gold, 8L), new ItemStack(Items.apple, 1, 32767), new ItemStack(Items.golden_apple, 1, 0), 50);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Gold, 8L), new ItemStack(Items.apple, 1, 32767), new ItemStack(Items.golden_apple, 1, 1), 50);
        //RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Blaze, 1L), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnderPearl, 1L), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnderEye, 1L), 50);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Blaze, 1L), new ItemStack(Items.slime_ball, 1, 32767), new ItemStack(Items.magma_cream, 1, 0), 50);

        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnderEye, 1), Materials.Radon.getGas(250), ItemList.QuantumEye.get(1L), null, null, null, 480, 384);
        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherStar, 1), Materials.Radon.getGas(1250), ItemList.QuantumStar.get(1L), null, null, null, 1920, 384);
        RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.NetherStar, 1), Materials.Neutronium.getMolten(288), ItemList.Gravistar.get(1L), 10000, 480, 7680);

        RA.addBenderRecipe(ItemList.IC2_Mixed_Metal_Ingot.get(1L), GT_OreDictUnificator.get(OrePrefixes.plateAlloy, Materials.Advanced, 1L), 100, 6);
        RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tin, 12L), ItemList.Cell_Empty.get(6L), 200, 6);
        RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 12L), ItemList.Cell_Empty.get(12L), 100, 6);
        RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Polytetrafluoroethylene, 12L), ItemList.Cell_Empty.get(48L), 100, 6);
        RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Polybenzimidazole, 12L), ItemList.Cell_Empty.get(60L), 100, 6);
		RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 12L), new ItemStack(Items.bucket, 4, 0), 800, 6);
        RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 12L), new ItemStack(Items.bucket, 4, 0), 800, 6);
        RA.addBenderRecipe(GT_OreDictUnificator.get(OrePrefixes.itemCasing, Materials.StainlessSteel, 1L), ItemList.IC2_Food_Can_Empty.get(1L), 20, 480);
        RA.addPulveriserRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Marble, 1L), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Marble, 1L)}, null, 160, 6);

        RA.addVacuumFreezerRecipe(GT_ModHandler.getIC2Item("reactorCoolantSimple", 1L, 32767), GT_ModHandler.getIC2Item("reactorCoolantSimple", 1L, 1), 100);
        RA.addVacuumFreezerRecipe(GT_ModHandler.getIC2Item("reactorCoolantTriple", 1L, 32767), GT_ModHandler.getIC2Item("reactorCoolantTriple", 1L, 1), 300);
        RA.addVacuumFreezerRecipe(GT_ModHandler.getIC2Item("reactorCoolantSix", 1L, 32767), GT_ModHandler.getIC2Item("reactorCoolantSix", 1L, 1), 1200);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_He_1.getWildcard(1L), ItemList.Reactor_Coolant_He_1.get(1L), 1200);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_He_3.getWildcard(1L), ItemList.Reactor_Coolant_He_3.get(1L), 1800);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_He_6.getWildcard(1L), ItemList.Reactor_Coolant_He_6.get(1L), 3600);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_NaK_1.getWildcard(1L), ItemList.Reactor_Coolant_NaK_1.get(1L), 1200);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_NaK_3.getWildcard(1L), ItemList.Reactor_Coolant_NaK_3.get(1L), 1800);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_NaK_6.getWildcard(1L), ItemList.Reactor_Coolant_NaK_6.get(1L), 3600);
        //New CoolantCell 180-360-540
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_Le_1.getWildcard(1L), ItemList.Reactor_Coolant_Le_1.get(1L), 1200);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_Le_2.getWildcard(1L), ItemList.Reactor_Coolant_Le_2.get(1L), 1800);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_Le_3.getWildcard(1L), ItemList.Reactor_Coolant_Le_3.get(1L), 3600);
        RA.addVacuumFreezerRecipe(ItemList.Reactor_Coolant_Le_6.getWildcard(1L), ItemList.Reactor_Coolant_Le_6.get(1L), 7200);

        RA.addVacuumFreezerRecipe(ItemList.neutroniumHeatCapacitor.getWildcard(1L), ItemList.neutroniumHeatCapacitor.get(1L), 10000000);
        RA.addVacuumFreezerRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Water, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Ice, 1L), 50);
        
        RA.addAlloySmelterRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 3L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Rubber, 3L), 200, 6);

        for (int i = 0; i < 16; i++) {
            RA.addCutterRecipe(new ItemStack(Blocks.stained_glass, 3, i), new ItemStack(Blocks.stained_glass_pane, 8, i), GT_Values.NI, 50, 6);
        }
        RA.addCutterRecipe(new ItemStack(Blocks.glass, 3, 0), new ItemStack(Blocks.glass_pane, 8, 0), GT_Values.NI, 50, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.stone, 1, 0), new ItemStack(Blocks.stone_slab, 2, 0), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.sandstone, 1, 0), new ItemStack(Blocks.stone_slab, 2, 1), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.cobblestone, 1, 0), new ItemStack(Blocks.stone_slab, 2, 3), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.brick_block, 1, 0), new ItemStack(Blocks.stone_slab, 2, 4), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stone_slab, 2, 5), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.nether_brick, 1, 0), new ItemStack(Blocks.stone_slab, 2, 6), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.quartz_block, 1, 32767), new ItemStack(Blocks.stone_slab, 2, 7), GT_Values.NI, 25, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.glowstone, 1, 0), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Glowstone, 4L), GT_Values.NI, 100, 18);

        RA.addCutterRecipe(new ItemStack(Blocks.wooden_slab, 1, 0), ItemList.Plank_Oak.get(2L), GT_Values.NI, 50, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.wooden_slab, 1, 1), ItemList.Plank_Spruce.get(2L), GT_Values.NI, 50, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.wooden_slab, 1, 2), ItemList.Plank_Birch.get(2L), GT_Values.NI, 50, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.wooden_slab, 1, 3), ItemList.Plank_Jungle.get(2L), GT_Values.NI, 50, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.wooden_slab, 1, 4), ItemList.Plank_Acacia.get(2L), GT_Values.NI, 50, 6);
        RA.addCutterRecipe(new ItemStack(Blocks.wooden_slab, 1, 5), ItemList.Plank_DarkOak.get(2L), GT_Values.NI, 50, 6);
        boolean loaded = Loader.isModLoaded(aTextForestry);
        ItemStack[] coverIDs = new ItemStack[]{
                ItemList.Plank_Larch.get(2L),
                ItemList.Plank_Teak.get(2L),
                ItemList.Plank_Acacia_Green.get(2L),
                ItemList.Plank_Lime.get(2L),
                ItemList.Plank_Chestnut.get(2L),
                ItemList.Plank_Wenge.get(2L),
                ItemList.Plank_Baobab.get(2L),
                ItemList.Plank_Sequoia.get(2L),
                ItemList.Plank_Kapok.get(2L),
                ItemList.Plank_Ebony.get(2L),
                ItemList.Plank_Mahagony.get(2L),
                ItemList.Plank_Balsa.get(2L),
                ItemList.Plank_Willow.get(2L),
                ItemList.Plank_Walnut.get(2L),
                ItemList.Plank_Greenheart.get(2L),
                ItemList.Plank_Cherry.get(2L),
                ItemList.Plank_Mahoe.get(2L),
                ItemList.Plank_Poplar.get(2L),
                ItemList.Plank_Palm.get(2L),
                ItemList.Plank_Papaya.get(2L),
                ItemList.Plank_Pine.get(2L),
                ItemList.Plank_Plum.get(2L),
                ItemList.Plank_Maple.get(2L),
                ItemList.Plank_Citrus.get(2L)};
        int i = 0;
        for (ItemStack cover : coverIDs) {
            if (loaded) {
                ItemStack slabWood = GT_ModHandler.getModItem(aTextForestry, "slabs", 1, i);
                ItemStack slabWoodFireproof = GT_ModHandler.getModItem(aTextForestry, "slabsFireproof", 1, i);
                GT_ModHandler.addCraftingRecipe(cover, GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"s ", " P", 'P', slabWood});
                GT_ModHandler.addCraftingRecipe(cover, GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"s ", " P", 'P', slabWoodFireproof});
                RA.addCutterRecipe(slabWood, cover, null, 40, 6);
                RA.addCutterRecipe(slabWoodFireproof, cover, null, 40, 6);
            } else if (isNEILoaded) {
                API.hideItem(cover);
            }
            i++;
        }
        for (int g = 0; g < 16; g++) {
            if (!isNEILoaded) {
                break;
            }
            API.hideItem(new ItemStack(GT_MetaGenerated_Item_03.INSTANCE, 1, g));
        }

        RA.addLatheRecipe(new ItemStack(Blocks.wooden_slab, 1, GT_Values.W), new ItemStack(Items.bowl, 1), null, 50, 6);
        RA.addLatheRecipe(GT_ModHandler.getModItem(aTextForestry, "slabs", 1L, GT_Values.W), new ItemStack(Items.bowl, 1), null, 50, 6);
        RA.addLatheRecipe(GT_ModHandler.getModItem(aTextEBXL, "woodslab", 1L, GT_Values.W), new ItemStack(Items.bowl, 1), null, 50, 6);

        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Cupronickel, 1L), ItemList.Shape_Mold_Credit.get(0L), ItemList.Credit_Greg_Cupronickel.get(4L), 100, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Brass, 1L), ItemList.Shape_Mold_Credit.get(0L), ItemList.Coin_Doge.get(4L), 100, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 1L), ItemList.Shape_Mold_Credit.get(0L), ItemList.Credit_Iron.get(4L), 100, 18);
        RA.addFormingPressRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 1L), ItemList.Shape_Mold_Credit.get(0L), ItemList.Credit_Iron.get(4L), 100, 18);

        if (!GT_Mod.gregtechproxy.mDisableIC2Cables) {
            RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Copper, 1L), GT_ModHandler.getIC2Item("copperCableItem", 3L), 100, 2);
            RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.AnnealedCopper, 1L), GT_ModHandler.getIC2Item("copperCableItem", 3L), 100, 2);
            RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tin, 1L), GT_ModHandler.getIC2Item("tinCableItem", 4L), 150, 1);
            RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 1L), GT_ModHandler.getIC2Item("ironCableItem", 6L), 200, 2);
            RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 1L), GT_ModHandler.getIC2Item("ironCableItem", 6L), 200, 2);
            RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Gold, 1L), GT_ModHandler.getIC2Item("goldCableItem", 6L), 200, 1);
        }
        RA.addWiremillRecipe(GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Graphene, 4L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Graphene, 2L), 400, 2);
        if (!GregTech_API.sRecipeFile.get(ConfigCategories.Recipes.disabledrecipes, "torchesFromCoal", false)) {
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1L), new ItemStack(Items.coal, 1, 32767), new ItemStack(Blocks.torch, 4), 400, 1);
        }
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Gold, 2L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Steel, 1L), new ItemStack(Blocks.light_weighted_pressure_plate, 1), 200, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 2L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Steel, 1L), new ItemStack(Blocks.heavy_weighted_pressure_plate, 1), 200, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 7L), ItemList.Circuit_Integrated.getWithDamage(0L, 7L), new ItemStack(Items.cauldron, 1), 700, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Iron, 1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), GT_ModHandler.getIC2Item("ironFence", 1L), 100, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Iron, 3L), ItemList.Circuit_Integrated.getWithDamage(0L, 3L), new ItemStack(Blocks.iron_bars, 4), 300, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 2L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Steel, 1L), new ItemStack(Blocks.heavy_weighted_pressure_plate, 1), 200, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 7L), ItemList.Circuit_Integrated.getWithDamage(0L, 7L), new ItemStack(Items.cauldron, 1), 700, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.WroughtIron, 1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), GT_ModHandler.getIC2Item("ironFence", 1L), 100, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.WroughtIron, 3L), ItemList.Circuit_Integrated.getWithDamage(0L, 3L), new ItemStack(Blocks.iron_bars, 4), 300, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 2L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Iron, 2L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Blocks.tripwire_hook, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 2L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.WroughtIron, 2L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Blocks.tripwire_hook, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 3L), new ItemStack(Items.string, 3, 32767), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Items.bow, 1), 400, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 3L), ItemList.Component_Minecart_Wheels_Iron.get(2L), new ItemStack(Items.minecart, 1), 500, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 3L), ItemList.Component_Minecart_Wheels_Iron.get(2L), new ItemStack(Items.minecart, 1), 400, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 3L), ItemList.Component_Minecart_Wheels_Steel.get(2L), new ItemStack(Items.minecart, 1), 300, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Iron, 2L), ItemList.Component_Minecart_Wheels_Iron.get(1L), 500, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.WroughtIron, 1L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.WroughtIron, 2L), ItemList.Component_Minecart_Wheels_Iron.get(1L), 400, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Steel, 1L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Steel, 2L), ItemList.Component_Minecart_Wheels_Steel.get(1L), 300, 2);
        RA.addAssemblerRecipe(new ItemStack(Items.minecart, 1), new ItemStack(Blocks.hopper, 1, 32767), new ItemStack(Items.hopper_minecart, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack(Items.minecart, 1), new ItemStack(Blocks.tnt, 1, 32767), new ItemStack(Items.tnt_minecart, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack(Items.minecart, 1), new ItemStack(Blocks.chest, 1, 32767), new ItemStack(Items.chest_minecart, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack(Items.minecart, 1), new ItemStack(Blocks.trapped_chest, 1, 32767), new ItemStack(Items.chest_minecart, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack(Items.minecart, 1), new ItemStack(Blocks.furnace, 1, 32767), new ItemStack(Items.furnace_minecart, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.tripwire_hook, 1), new ItemStack(Blocks.chest, 1, 32767), new ItemStack(Blocks.trapped_chest, 1), 200, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.stone, 1, 0), ItemList.Circuit_Integrated.getWithDamage(0L, 4L), new ItemStack(Blocks.stonebrick, 1, 0), 50, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.sandstone, 1, 0), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new ItemStack(Blocks.sandstone, 1, 2), 50, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.sandstone, 1, 1), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new ItemStack(Blocks.sandstone, 1, 0), 50, 6);
        RA.addAssemblerRecipe(new ItemStack(Blocks.sandstone, 1, 2), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new ItemStack(Blocks.sandstone, 1, 0), 50, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), GT_ModHandler.getIC2Item("machine", 1L), 25, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), ItemList.Casing_ULV.get(1L), 25, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), ItemList.Casing_LV.get(1L), 50, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Birmabright, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), ItemList.Casing_MV.get(1L), 50, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), ItemList.Casing_HV.get(1L), 50, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.BT6, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), ItemList.Casing_EV.get(1L), 50, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.HastelloyC276, 8L), ItemList.Circuit_Integrated.getWithDamage(0L, 8L), ItemList.Casing_IV.get(1L), 50, 18);

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Invar, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Invar, 1L), ItemList.Casing_HeatProof.get(2L), 50, 18);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.Cupronickel, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Bronze, 8L)}, Materials.Tin.getMolten(144L), ItemList.Casing_Coil_Cupronickel.get(1L), 200, 30, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.Kanthal, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 8L)}, Materials.Copper.getMolten(144L), ItemList.Casing_Coil_Kanthal.get(1L), 300, 120, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.Nichrome, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.StainlessSteel, 8L)}, Materials.Aluminium.getMolten(144L), ItemList.Casing_Coil_Nichrome.get(1L), 400, 480, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.TungstenSteel, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.VanadiumSteel, 8L)}, Materials.Nichrome.getMolten(144L), ItemList.Casing_Coil_TungstenSteel.get(1L), 500, 1920, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.HSSG, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.TungstenCarbide, 8L)}, Materials.Tungsten.getMolten(144L), ItemList.Casing_Coil_HSSG.get(1L), 600, 7680, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.Naquadah, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmium, 8L)}, Materials.TungstenSteel.getMolten(144L), ItemList.Casing_Coil_Naquadah.get(1L), 700, 30720, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.NaquadahAlloy, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 8L)}, Materials.Naquadah.getMolten(144L), ItemList.Casing_Coil_NaquadahAlloy.get(1L), 800, 122880, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.ElectrumFlux, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadria, 8L)}, Materials.NaquadahAlloy.getMolten(144L), ItemList.Casing_Coil_ElectrumFlux.get(1L), 900, 500000, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.Diamericiumtitanium, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Infuscolium, 8L)}, Materials.Neutronium.getMolten(144L), ItemList.Casing_Coil_Diamericiumtitanium.get(1L), 1000, 2000000, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.CosmicNeutronium, 1L), GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.DraconiumAwakened, 8L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Quantum, 8L)}, Materials.Phoenixite.getMolten(144L), ItemList.Casing_Coil_Infinity.get(1L), 1200, 8000000, false);

        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_SolidSteel.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Aluminium, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_FrostProof.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.TungstenSteel, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_RobustTungstenSteel.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.NaquadahAlloy, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_RobustNaquadahAlloy.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.StainlessSteel, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_CleanStainlessSteel.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Titanium, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_StableTitanium.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Osmiridium, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_MiningOsmiridium.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Magnalium, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.BlueSteel, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_Turbine.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 6L), ItemList.Casing_Turbine.get(1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_Turbine1.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 6L), ItemList.Casing_Turbine.get(1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_Turbine2.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 6L), ItemList.Casing_Turbine.get(1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_Turbine3.get(2L), 50, 18);
        RA.addAssemblerRecipe(ItemList.Casing_SolidSteel.get(1), GT_Utility.getIntegratedCircuit(6), Materials.Polytetrafluoroethylene.getMolten(216), ItemList.Casing_Chemically_Inert.get(1), 50, 18);
        if (GT_Mod.gregtechproxy.mComponentAssembler) {
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 4L), new ItemStack(Blocks.brick_block, 1), ItemList.Casing_BronzePlatedBricks.get(1L), 60, 6);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 4L), ItemList.Casing_Firebricks.get(1), ItemList.Casing_BronzePlatedBricks.get(1L), 60, 6);

        }
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 6L), new ItemStack(Blocks.brick_block, 1), ItemList.Casing_BronzePlatedBricks.get(2L), 50, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 6L), new ItemStack(Blocks.brick_block, 1), ItemList.Casing_BronzePlatedBricks.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Neutronium, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_RobustNeutronium.get(2L), 50, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.HSLA, 6L), GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.HSLA, 1L), GT_Utility.getIntegratedCircuit(1)}, null, ItemList.Casing_HSLA.get(2L), 50, 18);

        if (GT_Mod.gregtechproxy.mHardMachineCasings) {
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Lead, 2L), ItemList.Casing_ULV.get(1L), Materials.Plastic.getMolten(288), ItemList.Hull_ULV.get(1L), 25, 6);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 2L), ItemList.Casing_LV.get(1L), Materials.Plastic.getMolten(288), ItemList.Hull_LV.get(1L), 50, 30);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 2L), ItemList.Casing_MV.get(1L), Materials.Plastic.getMolten(288), ItemList.Hull_MV.get(1L), 50, 120);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.AnnealedCopper, 2L), ItemList.Casing_MV.get(1L), Materials.Plastic.getMolten(288), ItemList.Hull_MV.get(1L), 50, 120);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Gold, 2L), ItemList.Casing_HV.get(1L), Materials.Plastic.getMolten(288), ItemList.Hull_HV.get(1L), 50, 480);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Aluminium, 2L), ItemList.Casing_EV.get(1L), Materials.Plastic.getMolten(288), ItemList.Hull_EV.get(1L), 50, 1920);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.Tungsten, 2L), ItemList.Casing_IV.get(1L), Materials.Polytetrafluoroethylene.getMolten(288), ItemList.Hull_IV.get(1L), 50, 7680);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.VanadiumGallium, 2L), ItemList.Casing_LuV.get(1L), Materials.Polytetrafluoroethylene.getMolten(288), ItemList.Hull_LuV.get(1L), 50, 30720);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.Naquadah, 2L), ItemList.Casing_ZPM.get(1L), Materials.Polybenzimidazole.getMolten(288), ItemList.Hull_ZPM.get(1L), 50, 122880);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 2L), ItemList.Casing_UV.get(1L), Materials.Polybenzimidazole.getMolten(288), ItemList.Hull_UV.get(1L), 50, 500000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.Europium, 2L), ItemList.Casing_MAX.get(1L), Materials.Polybenzimidazole.getMolten(576), ItemList.Hull_MAX.get(1L), 50, 2000000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.Diamericiumtitanium, 2L), ItemList.Casing_UEV.get(1L), Materials.PerroxPolymer.getMolten(288), ItemList.Hull_UEV.get(1L), 50, 8000000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt08, Materials.Neutronium, 2L), ItemList.Casing_UIV.get(1L), Materials.PerroxPolymer.getMolten(288), ItemList.Hull_UIV.get(1L), 50, 32000000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt08, Materials.Quantium, 2L), ItemList.Casing_UMV.get(1L), Materials.PerroxPolymer.getMolten(288), ItemList.Hull_UMV.get(1L), 50, 128000000);
        } else {
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Lead, 2L), ItemList.Casing_ULV.get(1L), ItemList.Hull_ULV.get(1L), 25, 6);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 2L), ItemList.Casing_LV.get(1L), ItemList.Hull_LV.get(1L), 50, 30);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 2L), ItemList.Casing_MV.get(1L), ItemList.Hull_MV.get(1L), 50, 120);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.AnnealedCopper, 2L), ItemList.Casing_MV.get(1L), ItemList.Hull_MV.get(1L), 50, 120);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Gold, 2L), ItemList.Casing_HV.get(1L), ItemList.Hull_HV.get(1L), 50, 480);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Aluminium, 2L), ItemList.Casing_EV.get(1L), ItemList.Hull_EV.get(1L), 50, 1920);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.Tungsten, 2L), ItemList.Casing_IV.get(1L), ItemList.Hull_IV.get(1L), 50, 7680);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.VanadiumGallium, 2L), ItemList.Casing_LuV.get(1L), ItemList.Hull_LuV.get(1L), 50, 30720);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.Naquadah, 2L), ItemList.Casing_ZPM.get(1L), ItemList.Hull_ZPM.get(1L), 50, 122880);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 2L), ItemList.Casing_UV.get(1L), ItemList.Hull_UV.get(1L), 50, 500000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.Europium, 2L), ItemList.Casing_MAX.get(1L), ItemList.Hull_MAX.get(1L), 50, 2000000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.Diamericiumtitanium, 2L), ItemList.Casing_UEV.get(1L), ItemList.Hull_UEV.get(1L), 50, 8000000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.Neutronium, 2L), ItemList.Casing_UIV.get(1L), ItemList.Hull_UIV.get(1L), 50, 32000000);
            RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt08, Materials.Quantium, 2L), ItemList.Casing_UMV.get(1L), ItemList.Hull_UMV.get(1L), 50, 128000000);
        }

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.BatteryAlloy, 1L), Materials.Plastic.getMolten(144), ItemList.Battery_Hull_LV.get(1L), 800, 1);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 2L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.BatteryAlloy, 3L), Materials.Plastic.getMolten(432), ItemList.Battery_Hull_MV.get(1L), 1600, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.AnnealedCopper, 2L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.BatteryAlloy, 3L), Materials.Plastic.getMolten(432), ItemList.Battery_Hull_MV.get(1L), 1600, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Gold, 4L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.BatteryAlloy, 9L), Materials.Plastic.getMolten(1296), ItemList.Battery_Hull_HV.get(1L), 3200, 6);

        RA.addAssemblerRecipe(new ItemStack(Items.string, 4, 32767), new ItemStack(Items.slime_ball, 1, 32767), new ItemStack(Items.lead, 2), 200, 2);
        RA.addAssemblerRecipe(ItemList.IC2_Compressed_Coal_Ball.get(8L), new ItemStack(Blocks.brick_block, 1), ItemList.IC2_Compressed_Coal_Chunk.get(1L), 400, 6);

        RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("carbonFiber", 2L), ItemList.Circuit_Integrated.getWithDamage(0L, 2L), GT_ModHandler.getIC2Item("carbonMesh", 1L), 800, 2);

        RA.addAssemblerRecipe(ItemList.NC_SensorCard.getWildcard(1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), ItemList.Circuit_Basic.get(3L), 1600, 2);
        
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 5L), new ItemStack(Blocks.chest, 1, 32767), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Blocks.hopper), 800, 2);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 5L), new ItemStack(Blocks.trapped_chest, 1, 32767), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Blocks.hopper), 800, 2);

        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 5L), new ItemStack(Blocks.chest, 1, 32767), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Blocks.hopper), 800, 2);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.WroughtIron, 5L), new ItemStack(Blocks.trapped_chest, 1, 32767), ItemList.Circuit_Integrated.getWithDamage(0L, 1L)}, null, new ItemStack(Blocks.hopper), 800, 2);

        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnderPearl, 1L), new ItemStack(Items.blaze_powder, 1, 0), new ItemStack(Items.ender_eye, 1, 0), 400, 2);
        //RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnderPearl, 6L), new ItemStack(Items.blaze_rod, 1, 0), new ItemStack(Items.ender_eye, 6, 0), 2500, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gear, Materials.CobaltBrass, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1L), ItemList.Component_Sawblade_Diamond.get(1L), 1600, 2);
//        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 5L), new ItemStack(Blocks.tnt, 3, 32767), GT_ModHandler.getIC2Item("industrialTnt", 5L), 800, 2);
//        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 4L), new ItemStack(Blocks.sand, 4, 32767), new ItemStack(Blocks.tnt, 1), 400, 1);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 4L), new ItemStack(Blocks.redstone_lamp, 1), 400, 1);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1L), new ItemStack(Blocks.redstone_torch, 1), 400, 1);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 1L), GT_Utility.getIntegratedCircuit(1)}, Materials.RedAlloy.getMolten(18), new ItemStack(Items.compass, 1), 400, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Gold, 1L), GT_Utility.getIntegratedCircuit(1)}, Materials.RedAlloy.getMolten(18), new ItemStack(Items.clock, 1), 400, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), new ItemStack(Blocks.torch, 2), 400, 1);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), new ItemStack(Blocks.torch, 6), 400, 1);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Wood, 1L), ItemList.IC2_Resin.get(1L), new ItemStack(Blocks.torch, 6), 400, 1);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 8L), new ItemStack(Items.flint, 1), ItemList.IC2_Compressed_Coal_Ball.get(1L), 400, 6);

        if (!GT_Mod.gregtechproxy.mDisableIC2Cables) {
            RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("tinCableItem", 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Rubber, 1L), GT_ModHandler.getIC2Item("insulatedTinCableItem", 1L), 100, 2);
            RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("copperCableItem", 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Rubber, 1L), GT_ModHandler.getIC2Item("insulatedCopperCableItem", 1L), 100, 2);
            RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("goldCableItem", 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Rubber, 2L), GT_ModHandler.getIC2Item("insulatedGoldCableItem", 1L), 200, 2);
            RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("ironCableItem", 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Rubber, 3L), GT_ModHandler.getIC2Item("insulatedIronCableItem", 1L), 300, 2);
        }

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDense, Materials.Neutronium, 8L), GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Iridium, 4L), ItemList.neutroniumHeatCapacitor.get(1L), 100, 120000);

        GT_ModHandler.removeRecipe(new ItemStack[]{new ItemStack(Items.lava_bucket), ItemList.Cell_Empty.get(1L)});
        GT_ModHandler.removeRecipe(new ItemStack[]{new ItemStack(Items.water_bucket), ItemList.Cell_Empty.get(1L)});

        GT_ModHandler.removeFurnaceSmelting(ItemList.IC2_Resin.get(1L));

        RA.addCokeOvenRecipes(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Coal, 1), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.CokeCoal, 1L), Materials.Creosote.getFluid(200), 90 * 20);
        run2();

        GT_Utility.removeSimpleIC2MachineRecipe(new ItemStack(Blocks.cobblestone), GT_ModHandler.getMaceratorRecipeList(), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1L));
        GT_Utility.removeSimpleIC2MachineRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Lapis, 1L), GT_ModHandler.getMaceratorRecipeList(), ItemList.IC2_Plantball.get(1L));
        GT_Utility.removeSimpleIC2MachineRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_ModHandler.getMaceratorRecipeList(), ItemList.IC2_Plantball.get(1L));
        GT_Utility.removeSimpleIC2MachineRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 1L), GT_ModHandler.getMaceratorRecipeList(), ItemList.IC2_Plantball.get(1L));

        for (MaterialStack[] tMats : this.mAlloySmelterList) {
            ItemStack tDust1 = GT_OreDictUnificator.get(OrePrefixes.dust, tMats[0].mMaterial, tMats[0].mAmount);
            ItemStack tDust2 = GT_OreDictUnificator.get(OrePrefixes.dust, tMats[1].mMaterial, tMats[1].mAmount);
            ItemStack tIngot1 = GT_OreDictUnificator.get(OrePrefixes.ingot, tMats[0].mMaterial, tMats[0].mAmount);
            ItemStack tIngot2 = GT_OreDictUnificator.get(OrePrefixes.ingot, tMats[1].mMaterial, tMats[1].mAmount);
            ItemStack tOutputIngot = GT_OreDictUnificator.get(OrePrefixes.ingot, tMats[2].mMaterial, tMats[2].mAmount);
            if (tOutputIngot != GT_Values.NI) {
                GT_ModHandler.addAlloySmelterRecipe(tIngot1, tDust2, tOutputIngot, (int) tMats[2].mAmount * 50, 20, false);
                GT_ModHandler.addAlloySmelterRecipe(tIngot1, tIngot2, tOutputIngot, (int) tMats[2].mAmount * 50, 20, false);
                GT_ModHandler.addAlloySmelterRecipe(tDust1, tIngot2, tOutputIngot, (int) tMats[2].mAmount * 50, 20, false);
                GT_ModHandler.addAlloySmelterRecipe(tDust1, tDust2, tOutputIngot, (int) tMats[2].mAmount * 50, 20, false);
            }
        }

        if (!GregTech_API.mIC2Classic) {
            try {
                Map<String, HeatExchangeProperty> tLiqExchange = ic2.api.recipe.Recipes.liquidCooldownManager.getHeatExchangeProperties();
                Iterator<Map.Entry<String, HeatExchangeProperty>> tIterator = tLiqExchange.entrySet().iterator();
                while (tIterator.hasNext()) {
                    Map.Entry<String, HeatExchangeProperty> tEntry = tIterator.next();
                    if (tEntry.getKey().equals("ic2hotcoolant")) {
                        tIterator.remove();
                        Recipes.liquidCooldownManager.addFluid("ic2hotcoolant", "ic2coolant", 100);
                    }
                }
            } catch (Throwable e) {/*Do nothing*/}

            try {
                Map<String, HeatExchangeProperty> tLiqExchange = ic2.api.recipe.Recipes.liquidHeatupManager.getHeatExchangeProperties();
                Iterator<Map.Entry<String, HeatExchangeProperty>> tIterator = tLiqExchange.entrySet().iterator();
                while (tIterator.hasNext()) {
                    Map.Entry<String, HeatExchangeProperty> tEntry = tIterator.next();
                    if (tEntry.getKey().equals("ic2coolant")) {
                        tIterator.remove();
                        Recipes.liquidHeatupManager.addFluid("ic2coolant", "ic2hotcoolant", 100);
                    }
                }
            } catch (Throwable e) {/*Do nothing*/}
        }
        GT_Utility.removeSimpleIC2MachineRecipe(ItemList.Crop_Drop_BobsYerUncleRanks.get(1L), GT_ModHandler.getExtractorRecipeList(), null);
        GT_Utility.removeSimpleIC2MachineRecipe(ItemList.Crop_Drop_Ferru.get(1L), GT_ModHandler.getExtractorRecipeList(), null);
        GT_Utility.removeSimpleIC2MachineRecipe(ItemList.Crop_Drop_Aurelia.get(1L), GT_ModHandler.getExtractorRecipeList(), null);

        
        RA.addAutoclaveRecipe(Materials.SiliconDioxide.getDust(1), Materials.Water.getFluid(200L), Materials.Quartzite.getGems(1), 750, 2000, 24);
        RA.addAutoclaveRecipe(Materials.SiliconDioxide.getDust(1), GT_ModHandler.getDistilledWater(200L), Materials.Quartzite.getGems(1), 1000, 1500, 24);

        addRecipesApril2017ChemistryUpdate();
        addRecipesMay2017OilRefining();
        addPyrometallurgicalRecipes();
        addPolybenzimidazoleRecipes();
        run3();
    }

    private void run2() {
        RA.addCentrifugeRecipe(new ItemStack(Items.golden_apple, 1, 1), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(4608L), new ItemStack(Items.gold_ingot, 64), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 9216, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.golden_apple, 1, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), new ItemStack(Items.gold_ingot, 7), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 9216, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.golden_carrot, 1, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), new ItemStack(Items.gold_nugget, 6), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 9216, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.speckled_melon, 1, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), new ItemStack(Items.gold_nugget, 6), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 9216, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.mushroom_stew, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), new ItemStack(Items.bowl, 16, 0), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.apple, 32, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.bread, 64, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.porkchop, 12, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.cooked_porkchop, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.beef, 12, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.cooked_beef, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.fish, 12, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.cooked_fished, 16, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.chicken, 12, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.cooked_chicken, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.melon, 64, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.pumpkin, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.rotten_flesh, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.spider_eye, 32, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.carrot, 16, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(ItemList.Food_Raw_Potato.get(16L), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(ItemList.Food_Poisonous_Potato.get(12L), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(ItemList.Food_Baked_Potato.get(24L), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.cookie, 64, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.cake, 8, 0), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.brown_mushroom_block, 12, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.red_mushroom_block, 12, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.brown_mushroom, 32, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.red_mushroom, 32, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Items.nether_wart, 32, 32767), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(GT_ModHandler.getIC2Item("terraWart", 16L), GT_Values.NI, GT_Values.NF, Materials.Methane.getGas(576L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 4608, 5);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.sand, 1, 1), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1L), new ItemStack(Blocks.sand, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{5000, 10, 5000}, 50, 30);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.dirt, 1, 32767), GT_Values.NI, GT_Values.NF, GT_Values.NF, ItemList.IC2_Plantball.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), new ItemStack(Blocks.sand, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{1250, 500, 5000}, 250, 30);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.grass, 1, 32767), GT_Values.NI, GT_Values.NF, GT_Values.NF, ItemList.IC2_Plantball.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), new ItemStack(Blocks.sand, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{2500, 500, 5000}, 250, 30);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.mycelium, 1, 32767), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.brown_mushroom, 1), new ItemStack(Blocks.red_mushroom, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), new ItemStack(Blocks.sand, 1), GT_Values.NI, GT_Values.NI, new int[]{2500, 2500, 500, 5000}, 650, 30);
        RA.addCentrifugeRecipe(ItemList.IC2_Resin.get(1L), GT_Values.NI, GT_Values.NF, Materials.Glue.getFluid(100L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RawRubber, 3L), ItemList.IC2_Plantball.get(1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 1000}, 300, 5);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.DarkAsh, 2), 0, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, 320);
        RA.addCentrifugeRecipe(new ItemStack(Items.magma_cream, 1), 0, new ItemStack(Items.blaze_powder, 1), new ItemStack(Items.slime_ball, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, 500);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium235, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{200, 20}, 800, 320);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium241, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{200, 300}, 1600, 320);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadria, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{200, 50}, 2400, 1200);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadria, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{500, 800}, 2800, 1800);
        RA.addCentrifugeRecipe(GT_Values.NI, GT_Values.NI, Materials.Hydrogen.getGas(160L), Materials.Deuterium.getGas(40L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 160, 20);
        RA.addCentrifugeRecipe(GT_Values.NI, GT_Values.NI, Materials.Deuterium.getGas(160L), Materials.Tritium.getGas(40L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 160, 75);
        RA.addCentrifugeRecipe(GT_Values.NI, GT_Values.NI, Materials.Helium.getGas(80L), Materials.Helium_3.getGas(5L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 160, 75);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 2L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 976, 75);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Endstone, 1L), GT_Values.NI, GT_Values.NF, Materials.Helium.getGas(120L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 1L), new ItemStack(Blocks.sand, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{300, 70, 9000, 0, 0, 0}, 320, 20);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Netherrack, 1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1L), GT_Values.NI, GT_Values.NI, new int[]{600, 2500, 600, 700, 0, 0}, 160, 20);
        RA.addCentrifugeRecipe(new ItemStack(Blocks.soul_sand, 1), GT_Values.NI, GT_Values.NF, Materials.Oil.getFluid(80L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Saltpeter, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1L), new ItemStack(Blocks.sand, 1), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{2000, 200, 9000, 0, 0, 0}, 200, 75);
        RA.addCentrifugeRecipe(GT_Values.NI, GT_Values.NI, Materials.Lava.getFluid(100L), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Copper, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Tin, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Silver, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Tantalum, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 1L), new int[]{2000, 1000, 250, 250, 250, 30}, 80, 75);
        RA.addCentrifugeRecipe(GT_Values.NI, GT_Values.NI, FluidRegistry.getFluidStack("ic2pahoehoelava", 100), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Copper, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Tin, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Silver, 1L), GT_OreDictUnificator.get(OrePrefixes.nugget, Materials.Tantalum, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 1L), new int[]{2000, 1000, 250, 250, 250, 30}, 40, 75);

        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RareEarth, 4L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Neodymium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Yttrium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lanthanum, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cerium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cadmium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Caesium, 1L), new int[]{2500, 2500, 2500, 2500, 2500, 2500}, 64, 20);
        RA.addCentrifugeRecipe(GT_ModHandler.getModItem(aTextAE, aTextAEMM, 4L, 45), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BasalticMineralSand, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Olivine, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Obsidian, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Basalt, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RareEarth, 1L), new int[]{2000, 2000, 2000, 2000, 2000, 2000}, 64, 20);

        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Brick, 19L), ItemList.Cell_Empty.get(12L), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silicon, 3L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 12L), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 10000, 10000}, 400, 120);
        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Fireclay, 19L), ItemList.Cell_Empty.get(12L), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silicon, 3L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 12L), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 10000, 10000}, 200, 120);

        if (Loader.isModLoaded("BiomesOPlenty")) {
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopGrass", 1L, 0), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 800}, 100, 30);
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopDirt", 1L, 0), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 800}, 100, 30);
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopDirt", 1L, 1), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 800}, 100, 30);
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopDirt", 1L, 2), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 800}, 100, 30);
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopDirt", 1L, 3), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 800}, 100, 30);
            //RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopGrass", 1L, 2), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.QuartzSand, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 3300}, 100,30);
            //RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopDirt", 1L, 4), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.QuartzSand, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 3300}, 100,30);
            //RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "newBopDirt", 1L, 5), GT_Values.NI, GT_Values.NF, GT_Values.NF, new ItemStack(Blocks.dirt, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.QuartzSand, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 3300}, 100,30);
            RA.addCentrifugeRecipe(GT_ModHandler.getModItem("BiomesOPlenty", "driedDirt", 1L, 0), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Clay, 1L), new ItemStack(Blocks.sand, 1, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{2400, 5000, 300}, 100, 30);
        }
        RA.addLatheRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.ReinforcedGlass, 1L), GT_OreDictUnificator.get(OrePrefixes.lens, Materials.ReinforcedGlass, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1L), new int[]{10000, 2500}, 400, 18);
        RA.addCutterRecipe(GT_ModHandler.getModItem("IC2", "blockAlloyGlass", 1L, 0), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.ReinforcedGlass, 1L), GT_Values.NI, 1200, 30);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1), ItemList.Cell_Empty.get(3), Materials.HydrochloricAcid.getFluid(3000), Materials.IronIIIChloride.getFluid(1000), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 3), 400, 480);
//      Organic
        //RA.addBrewingRecipeCustom(GT_ModHandler.getModItem("IC2", "itemBiochaff", 16L, 0), Materials.GrowthMediumRaw.getFluid(750), FluidRegistry.getFluidStack("binnie.bacteria", 750), 1200, 480, false);
        RA.addBrewingRecipeCustom(GT_ModHandler.getModItem("IC2", "itemBiochaff", 16L, 0), Materials.GrowthMediumRaw.getFluid(750), Materials.Bacteria.getFluid(750), 1200, 480, false);
        //RA.addBrewingRecipeCustom(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MysteriousCrystal, 1L), FluidRegistry.getFluidStack("binnie.bacteria", 750), FluidRegistry.getFluidStack("bacterialsludge", 750), 800, 275, false);
        RA.addBrewingRecipeCustom(ItemList.IC2_Fertilizer.get(8), Materials.Bacteria.getFluid(750), Materials.BacterialSludge.getFluid(750), 800, 275, false);
        RA.addBrewingRecipeCustom(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L), Materials.BacterialSludge.getFluid(750), Materials.EnrichedBacterialSludge.getFluid(750), 200, 48, false);
        RA.addBrewingRecipeCustom(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium235, 1L), Materials.BacterialSludge.getFluid(7500), Materials.EnrichedBacterialSludge.getFluid(7500), 1000, 48, false);
        RA.addUniversalDistillationRecipe(Materials.EnrichedBacterialSludge.getFluid(10), new FluidStack[]{Materials.Mutagen.getFluid(1L)}, GT_Values.NI, 60, 1920);

        //RA.addBrewingRecipeCustom(GT_ModHandler.getModItem("Genetics", "misc", 6L, 4), FluidRegistry.getFluidStack("water", 750), FluidRegistry.getFluidStack("binnie.growthmedium", 750), 600, 480, false);
        //RA.addBrewingRecipeCustom(GT_ModHandler.getModItem("IC2", "itemBiochaff", 16L, 0), FluidRegistry.getFluidStack("binnie.growthmedium", 750), FluidRegistry.getFluidStack("binnie.bacteria", 750), 1200, 480, false);

        RA.addAutoclaveRecipe(ItemList.Circuit_Parts_RawCrystalParts.get(1L), FluidRegistry.getFluidStack("bacterialsludge", 250), ItemList.Circuit_Parts_RawCrystalChip.get(1L), 6000, 12000, 480);

//      Pumps

        RA.addAssemblylineRecipe(ItemList.Electric_Pump_IV.get(1, new Object() {
        }), 144000, new Object[]{
                ItemList.Electric_Motor_LuV.get(1, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.pipeSmall, Materials.NiobiumTitanium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.HSSS, 2L),
                GT_OreDictUnificator.get(OrePrefixes.screw, Materials.HSSS, 8L),
                new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, (Materials.StyreneButadieneRubber), 4L), GT_OreDictUnificator.get(OrePrefixes.ring, (Materials.Silicone), 4L)},
                GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.HSSS, 2L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 2L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(144),
                Materials.Lubricant.getFluid(250)}, ItemList.Electric_Pump_LuV.get(1), 600, 6000);

        RA.addAssemblylineRecipe(ItemList.Electric_Pump_LuV.get(1, new Object() {
        }), 144000, new Object[]{
                ItemList.Electric_Motor_ZPM.get(1, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Enderium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.screw, Materials.Osmiridium, 8L),
                new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, (Materials.StyreneButadieneRubber), 8L), GT_OreDictUnificator.get(OrePrefixes.ring, (Materials.Silicone), 8L)},
                GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Osmiridium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 2L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(288),
                Materials.Lubricant.getFluid(750)}, ItemList.Electric_Pump_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Electric_Pump_ZPM.get(1, new Object() {
        }), 288000, new Object[]{
                ItemList.Electric_Motor_UV.get(1, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Naquadah, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tritanium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.screw, Materials.Tritanium, 8L),
                new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, (Materials.StyreneButadieneRubber), 16L), GT_OreDictUnificator.get(OrePrefixes.ring, (Materials.Silicone), 16L)},
                GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.NaquadahAlloy, 2L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 2L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(1296),
                Materials.Lubricant.getFluid(2000),
                Materials.Naquadria.getMolten(1296)}, ItemList.Electric_Pump_UV.get(1), 600, 100000);

//        Conveyor

        RA.addAssemblylineRecipe(ItemList.Conveyor_Module_IV.get(1, new Object() {
        }), 144000, new Object[]{
                ItemList.Electric_Motor_LuV.get(2, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.HSSS, 2L),
                GT_OreDictUnificator.get(OrePrefixes.ring, Materials.HSSS, 4L),
                GT_OreDictUnificator.get(OrePrefixes.round, Materials.HSSS, 32L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 2L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(144),
                Materials.Lubricant.getFluid(250),
                Materials.StyreneButadieneRubber.getMolten(1440)}, ItemList.Conveyor_Module_LuV.get(1), 600, 6000);

        RA.addAssemblylineRecipe(ItemList.Conveyor_Module_LuV.get(1, new Object() {
        }), 144000, new ItemStack[]{
                ItemList.Electric_Motor_ZPM.get(2, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Osmiridium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.round, Materials.Osmiridium, 32L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 2L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(288),
                Materials.Lubricant.getFluid(750),
                Materials.StyreneButadieneRubber.getMolten(2880)}, ItemList.Conveyor_Module_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Conveyor_Module_ZPM.get(1, new Object() {
        }), 288000, new ItemStack[]{
                ItemList.Electric_Motor_UV.get(2, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tritanium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Tritanium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.round, Materials.Tritanium, 32L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 2L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(1296),
                Materials.Lubricant.getFluid(2000),
                Materials.StyreneButadieneRubber.getMolten(5760),
                Materials.Naquadria.getMolten(1296)}, ItemList.Conveyor_Module_UV.get(1), 600, 100000);

//        Piston

        RA.addAssemblylineRecipe(ItemList.Electric_Piston_IV.get(1, new Object() {
        }), 144000, new ItemStack[]{
                ItemList.Electric_Motor_LuV.get(1, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.HSSS, 6L),
                GT_OreDictUnificator.get(OrePrefixes.ring, Materials.HSSS, 4L),
                GT_OreDictUnificator.get(OrePrefixes.round, Materials.HSSS, 32L),
                GT_OreDictUnificator.get(OrePrefixes.stick, Materials.HSSS, 4L),
                GT_OreDictUnificator.get(OrePrefixes.gear, Materials.HSSS, 1L),
                GT_OreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.HSSS, 2L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 4L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(144),
                Materials.Lubricant.getFluid(250)}, ItemList.Electric_Piston_LuV.get(1), 600, 6000);


        RA.addAssemblylineRecipe(ItemList.Electric_Piston_LuV.get(1, new Object() {
        }), 144000, new ItemStack[]{
                ItemList.Electric_Motor_ZPM.get(1, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 6L),
                GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Osmiridium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.round, Materials.Osmiridium, 32L),
                GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Osmiridium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.gear, Materials.Osmiridium, 1L),
                GT_OreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.Osmiridium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 4L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(288),
                Materials.Lubricant.getFluid(750)}, ItemList.Electric_Piston_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Electric_Piston_ZPM.get(1, new Object() {
        }), 288000, new ItemStack[]{
                ItemList.Electric_Motor_UV.get(1, new Object() {
                }),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tritanium, 6L),
                GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Tritanium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.round, Materials.Tritanium, 32L),
                GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Tritanium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.gear, Materials.NaquadahAlloy, 1L),
                GT_OreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.NaquadahAlloy, 2L),
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 4L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(1296),
                Materials.Lubricant.getFluid(2000),
                Materials.Naquadria.getMolten(1296)}, ItemList.Electric_Piston_UV.get(1), 600, 100000);

//        RobotArm

        Object o = new Object[0];
        RA.addAssemblylineRecipe(ItemList.Robot_Arm_IV.get(1, new Object() {
        }), 144000, new Object[]{
                GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.HSSS, 4L),
                GT_OreDictUnificator.get(OrePrefixes.gear, Materials.HSSS, 1L),
                GT_OreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.HSSS, 3L),
                ItemList.Electric_Motor_LuV.get(2, new Object() {
                }),
                ItemList.Electric_Piston_LuV.get(1, new Object() {
                }),
                new Object[]{OrePrefixes.circuit.get(Materials.Master), 2},
                new Object[]{OrePrefixes.circuit.get(Materials.Elite), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Data), 8},
                GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 6L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(576),
                Materials.Lubricant.getFluid(250)}, ItemList.Robot_Arm_LuV.get(1), 600, 6000);

        RA.addAssemblylineRecipe(ItemList.Robot_Arm_LuV.get(1, new Object() {
        }), 144000, new Object[]{
                GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Osmiridium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.gear, Materials.Osmiridium, 1L),
                GT_OreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.Osmiridium, 3L),
                ItemList.Electric_Motor_ZPM.get(2, new Object() {
                }),
                ItemList.Electric_Piston_ZPM.get(1, new Object() {
                }),
                new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 2},
                new Object[]{OrePrefixes.circuit.get(Materials.Master), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Elite), 8},
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 6L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(1152),
                Materials.Lubricant.getFluid(750)}, ItemList.Robot_Arm_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Robot_Arm_ZPM.get(1, new Object() {
        }), 288000, new Object[]{
                GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Tritanium, 4L),
                GT_OreDictUnificator.get(OrePrefixes.gear, Materials.NaquadahAlloy, 1L),
                GT_OreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.NaquadahAlloy, 3L),
                ItemList.Electric_Motor_UV.get(2, new Object() {
                }),
                ItemList.Electric_Piston_UV.get(1, new Object() {
                }),
                new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 2},
                new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Master), 8},
                GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 6L)}, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(2304),
                Materials.Lubricant.getFluid(2000),
                Materials.Naquadria.getMolten(1296)}, ItemList.Robot_Arm_UV.get(1), 600, 100000);

//        Emitter

        RA.addAssemblylineRecipe(ItemList.Emitter_IV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.HSSS, 1L),
                        ItemList.Electric_Motor_LuV.get(1, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Osmium, 8L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.GarnetYellow, 1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Master), 4},
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 7L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(576)},
                ItemList.Emitter_LuV.get(1), 600, 6000);

        RA.addAssemblylineRecipe(ItemList.Emitter_LuV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1L),
                        ItemList.Electric_Motor_ZPM.get(1, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Osmiridium, 8L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.GarnetRed, 1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 4},
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 7L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(1152)},
                ItemList.Emitter_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Emitter_ZPM.get(1, new Object() {
                }), 288000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Europium, 1L),
                        ItemList.Electric_Motor_UV.get(1, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.stickLong, Materials.Tritanium, 8L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Amethyst, 1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 4},
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadah, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadah, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadah, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 7L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(2304),
                        Materials.Naquadria.getMolten(1296)},
                ItemList.Emitter_UV.get(1), 600, 100000);

//        Sensor

        RA.addAssemblylineRecipe(ItemList.Sensor_IV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.HSSS, 1L),
                        ItemList.Electric_Motor_LuV.get(1, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmium, 8L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.GarnetYellow, 1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Master), 4},
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 7L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(576)},
                ItemList.Sensor_LuV.get(1), 600, 6000);

        RA.addAssemblylineRecipe(ItemList.Sensor_LuV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1L),
                        ItemList.Electric_Motor_ZPM.get(1, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 8L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.GarnetRed, 1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 4},
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 7L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(1152)},
                ItemList.Sensor_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Sensor_ZPM.get(1, new Object() {
                }), 288000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Europium, 1L),
                        ItemList.Electric_Motor_UV.get(1, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tritanium, 8L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Amethyst, 1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 4},
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadah, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadah, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadah, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 7L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(2304),
                        Materials.Naquadria.getMolten(1296)},
                ItemList.Sensor_UV.get(1), 600, 100000);

//        Field Generator

        RA.addAssemblylineRecipe(ItemList.Field_Generator_IV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.HSSS, 1L),
                        GT_OreDictUnificator.get(OrePrefixes.plate, Materials.HSSS, 6L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.GarnetYellow, 2L),
                        ItemList.Emitter_LuV.get(4, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 4},
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Osmiridium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.YttriumBariumCuprate, 8L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(576)},
                ItemList.Field_Generator_LuV.get(1), 600, 6000);

        RA.addAssemblylineRecipe(ItemList.Field_Generator_LuV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1L),
                        GT_OreDictUnificator.get(OrePrefixes.plate, Materials.NaquadahAlloy, 6L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.GarnetRed, 2L),
                        ItemList.Emitter_ZPM.get(4, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 4},
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Europium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Europium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Europium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Europium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.Naquadah, 8L)}, new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(1152)},
                ItemList.Field_Generator_ZPM.get(1), 600, 24000);

        RA.addAssemblylineRecipe(ItemList.Field_Generator_ZPM.get(1, new Object() {
                }), 288000, new Object[]{
                        GT_OreDictUnificator.get(OrePrefixes.frameGt, Materials.Europium, 1L),
                        GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Tritanium, 6L),
                        GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Amethyst, 2L),
                        ItemList.Emitter_UV.get(4, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Infinite), 4},
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Americium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Americium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Americium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Americium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Americium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Americium, 64L),
                        GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.NaquadahAlloy, 8L)},
                new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(2304),
                        Materials.Naquadria.getMolten(1296)},
                ItemList.Field_Generator_UV.get(1), 600, 100000);

        RA.addAssemblylineRecipe(ItemList.Circuit_Crystalmainframe.get(1L), 72000, new ItemStack[]{
                ItemList.Circuit_Board_Wetware_Extreme.get(1L),
                ItemList.Circuit_Chip_Stemcell.get(16L),
                ItemList.Circuit_Parts_Reinforced_Glass_Tube.get(8L),
                GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Polybenzimidazole, 8L),
                GT_OreDictUnificator.get(OrePrefixes.itemCasing, Materials.Electrum, 16L),
                GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Silicone, 16L),
                GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.HSSE, 8L),
        }, new FluidStack[]{
                GregTech_API.mIC2Classic ? Materials.Water.getFluid(250L) : Materials.GrowthMediumSterilized.getFluid(250L),
                Materials.Radon.getGas(1000L),
                GregTech_API.mIC2Classic ? Materials.Lava.getFluid(1000L) : new FluidStack(FluidRegistry.getFluid("ic2coolant"), 1000)
        }, ItemList.Circuit_Chip_NeuroCPU.get(1L), 600, 80000);

        RA.addAssemblylineRecipe(ItemList.Circuit_Chip_NeuroCPU.get(1L), 144000, new ItemStack[]{
                ItemList.Circuit_Board_Bio_Ultra.get(1L),
                ItemList.Circuit_Chip_Biocell.get(16L),
                ItemList.Circuit_Parts_Reinforced_Glass_Tube.get(16L),
                GT_OreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Polybenzimidazole, 16),
                GT_OreDictUnificator.get(OrePrefixes.itemCasing, Materials.Tungsten, 16),
                GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Silicone, 64),
                GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.HSSS, 32),
        }, new FluidStack[]{
                GregTech_API.mIC2Classic ? Materials.Water.getFluid(500L) : Materials.BioMediumSterilized.getFluid(500L),
                Materials.Radon.getGas(1000L),
                GregTech_API.mIC2Classic ? Materials.Lava.getFluid(2000L) : new FluidStack(FluidRegistry.getFluid("ic2coolant"), 2000)
        }, ItemList.Circuit_Chip_BioCPU.get(1L), 600, 600000);

        RA.addAssemblylineRecipe(GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 1), 144000, new Object[]{
                ItemList.Casing_Coil_Superconductor.get(1),
                new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Master), 8},
                new Object[]{OrePrefixes.circuit.get(Materials.Elite), 16},
                new Object[]{OrePrefixes.circuit.get(Materials.Advanced), 32},
                GT_OreDictUnificator.get(OrePrefixes.plateDense, Materials.Plutonium241, 4L),
                GT_OreDictUnificator.get(OrePrefixes.plateDense, Materials.Osmiridium, 4L),
                ItemList.Field_Generator_IV.get(2),
                ItemList.Circuit_Wafer_UHPIC.get(32),
                GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 32),
        }, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(2880),
                Materials.VanadiumGallium.getMolten(1152L),
        }, ItemList.FusionComputer_LuV.get(1), 800, 30000);

        RA.addAssemblylineRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Europium, 1), 288000, new Object[]{
                ItemList.Casing_Fusion_Coil.get(1),
                new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 8},
                new Object[]{OrePrefixes.circuit.get(Materials.Master), 16},
                new Object[]{OrePrefixes.circuit.get(Materials.Elite), 32},
                GT_OreDictUnificator.get(OrePrefixes.plateDense, Materials.Europium, 4L),
                ItemList.Field_Generator_LuV.get(2),
                ItemList.Circuit_Wafer_NPIC.get(48),
                GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorZPM, 32),
        }, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(2880),
                Materials.NiobiumTitanium.getMolten(1152L),
        }, ItemList.FusionComputer_ZPMV.get(1), 1000, 90000);

        RA.addAssemblylineRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Americium, 1), 432000, new Object[]{
                ItemList.Casing_Fusion_Coil.get(1),
                new Object[]{OrePrefixes.circuit.get(Materials.Infinite), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 8},
                new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 16},
                new Object[]{OrePrefixes.circuit.get(Materials.Master), 32},
                GT_OreDictUnificator.get(OrePrefixes.plateDense, Materials.Americium, 4L),
                ItemList.Field_Generator_ZPM.get(2),
                ItemList.Circuit_Wafer_PPIC.get(64),
                GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.SuperconductorUV, 32),
        }, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(2880),
                Materials.ElectrumFlux.getMolten(1152L),
        }, ItemList.FusionComputer_UV.get(1), 1200, 200000);

        RA.addAssemblylineRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Neutronium, 1), 576000, new Object[]{
                ItemList.Casing_Fusion_Coil2.get(1),
                new Object[]{OrePrefixes.circuit.get(Materials.Bio), 4},
                new Object[]{OrePrefixes.circuit.get(Materials.Infinite), 8},
                new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 16},
                new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 32},
                GT_OreDictUnificator.get(OrePrefixes.plateDense, Materials.Infuscolium, 4L),
                ItemList.Field_Generator_UV.get(2),
                ItemList.Circuit_Wafer_QPIC.get(64),
                GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.Superconductor, 32),
        }, new FluidStack[]{
                Materials.SolderingAlloy.getMolten(2880),
                Materials.Diamericiumtitanium.getMolten(1152L),
        }, ItemList.FusionComputer_UHV.get(1), 1400, 500000);

        if (Loader.isModLoaded("IC2NuclearControl")) {
            RA.addAssemblerRecipe(ItemList.NC_SensorCard.get(1L), GT_Values.NI, GT_ModHandler.getIC2Item("electronicCircuit", 2L), 200, 30);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem("IC2NuclearControl", "ItemMultipleSensorLocationCard", 1L, 0), GT_ModHandler.getModItem("IC2NuclearControl", "ItemMultipleSensorLocationCard", 1L, 0), GT_ModHandler.getIC2Item("electronicCircuit", 1L), 200, 30);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem("IC2NuclearControl", "ItemMultipleSensorLocationCard", 1L, 1), GT_ModHandler.getModItem("IC2NuclearControl", "ItemMultipleSensorLocationCard", 1L, 1), GT_ModHandler.getIC2Item("electronicCircuit", 1L), 200, 30);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem("IC2NuclearControl", "ItemMultipleSensorLocationCard", 1L, 2), GT_ModHandler.getModItem("IC2NuclearControl", "ItemMultipleSensorLocationCard", 1L, 2), GT_ModHandler.getIC2Item("electronicCircuit", 1L), 200, 30);
            RA.addAssemblerRecipe(GT_ModHandler.getModItem("IC2NuclearControl", "RFSensorCard", 1L, 0), GT_ModHandler.getModItem("IC2NuclearControl", "RFSensorCard", 1L, 0), GT_ModHandler.getIC2Item("electronicCircuit", 1L), 200, 30);
        }

        // --- New Batteries
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Aluminium, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.BlueSteel, 2L), Materials.Polytetrafluoroethylene.getMolten(144L),
                ItemList.BatteryHull_EV.get(1L), 100, 480);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Platinum, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RoseGold, 6L), Materials.Polytetrafluoroethylene.getMolten(288L),
                ItemList.BatteryHull_IV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Naquadah, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.RedSteel, 18L), Materials.Polybenzimidazole.getMolten(144L),
                ItemList.BatteryHull_LuV.get(1L), 300, 7680);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.NaquadahAlloy, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Europium, 6L), Materials.Polybenzimidazole.getMolten(288L),
                ItemList.BatteryHull_ZPM.get(1L), 200, 30720);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.ElectrumFlux, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Americium, 18L), Materials.Polybenzimidazole.getMolten(576L),
                ItemList.BatteryHull_UV.get(1L), 300, 122880);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt02, Materials.ElectrumFlux, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Naquadah, 24L), Materials.Polybenzimidazole.getMolten(1152L),
                ItemList.BatteryHull_UHV.get(1L), 100, 500000);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt04, Materials.ElectrumFlux, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.NaquadahEnriched, 36L), Materials.Polybenzimidazole.getMolten(2304L),
                ItemList.BatteryHull_UEV.get(1L), 200, 1000000);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.cableGt08, Materials.ElectrumFlux, 2L),
                GT_OreDictUnificator.get(OrePrefixes.plate, Materials.NaquadahAlloy, 48L), Materials.Polybenzimidazole.getMolten(4608L),
                ItemList.BatteryHull_UIV.get(1L), 300, 2000000);


        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_EV_Full.get(1L), ItemList.BatteryHull_EV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_IV_Full.get(1L), ItemList.BatteryHull_IV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_LuV_Full.get(1L), ItemList.BatteryHull_LuV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_ZPM_Full.get(1L), ItemList.BatteryHull_ZPM.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_UV_Full.get(1L), ItemList.BatteryHull_UV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_UHV_Full.get(1L), ItemList.BatteryHull_UHV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_UEV_Full.get(1L), ItemList.BatteryHull_UEV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.BatteryHull_UIV_Full.get(1L), ItemList.BatteryHull_UIV.get(1L));

        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Vanadium, 4L),
                ItemList.BatteryHull_EV.get(1L),
                ItemList.BatteryHull_EV_Full.get(1L),
                null, 100, 480);

        // EV 8192
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Vanadium, 16L),
                ItemList.BatteryHull_IV.get(1L),
                ItemList.BatteryHull_IV_Full.get(1L),
                null, 150, 1200);

        // LuV 32768
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Vanadium, 32L),
                ItemList.BatteryHull_LuV.get(1L),
                ItemList.BatteryHull_LuV_Full.get(1L),
                null, 200, 1920);

        // ZPM 131072
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadria, 16L),
                ItemList.BatteryHull_ZPM.get(1L),
                ItemList.BatteryHull_ZPM_Full.get(1L),
                null, 250, 4100);

        // UV 524288
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadria, 32L),
                ItemList.BatteryHull_UV.get(1L),
                ItemList.BatteryHull_UV_Full.get(1L),
                null, 300, 7875);

        // UHV 2097152
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Neutronium, 16L),
                ItemList.BatteryHull_UHV.get(1L),
                ItemList.BatteryHull_UHV_Full.get(1L),
                null, 350, 18000);

        // UEV 8388608
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Neutronium, 32L),
                ItemList.BatteryHull_UEV.get(1L),
                ItemList.BatteryHull_UEV_Full.get(1L),
                null, 400, 31440);

        // UIV 33554432
        RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Neutronium, 64L),
                ItemList.BatteryHull_UIV.get(1L),
                ItemList.BatteryHull_UIV_Full.get(1L),
                null, 450, 62880);

        //Energy Crystals
        RA.addCompressorRecipe(ItemList.IC2_Energium_Dust.get(36L), ItemList.EnergyCrystal_LV.get(1L), 1000, 30);
        RA.addCompressorRecipe(ItemList.EnergyCrystal_LV.get(4L), ItemList.EnergyCrystal_MV.get(1L), 1000, 120);
        RA.addCompressorRecipe(ItemList.EnergyCrystal_MV.get(4L), ItemList.EnergyCrystal_HV.get(1L), 1000, 480);
        RA.addCompressorRecipe(ItemList.EnergyCrystal_HV.get(4L), ItemList.EnergyCrystal_EV.get(1L), 1000, 1920);
        GT_ModHandler.addCraftingRecipe(ItemList.EnergyCrystal_IV.get(1L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"PBP", "BCB", "PBP", 'C', OrePrefixes.circuit.get(Materials.Elite), 'B', ItemList.EnergyCrystal_EV.get(1L), 'P', GT_ModHandler.getIC2Item("iridiumPlate", 1L)});

        RA.addExtractorRecipe(ItemList.EnergyCrystal_MV.get(1L), ItemList.EnergyCrystal_LV.get(4L), 100, 120);
        RA.addExtractorRecipe(ItemList.EnergyCrystal_HV.get(1L), ItemList.EnergyCrystal_MV.get(4L), 200, 480);
        RA.addExtractorRecipe(ItemList.EnergyCrystal_EV.get(1L), ItemList.EnergyCrystal_HV.get(4L), 400, 1920);


        //Antimatter
        RA.addAssemblerRecipe(ItemList.Hull_ZPM.get(1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Osmium, 16), Materials.Gallium.getMolten(864L), ItemList.Casing_Dyson_Ring.get(1), 350, 30800, true);

        RA.addMultiblockChemicalRecipe(new ItemStack[]{ItemList.Magnetic_Confinement_Pod.get(1L)}, new FluidStack[]{Materials.Tritanium.getMolten(144), Materials.NaquadriaLiquid.getFluid(1000)}, new FluidStack[]{Materials.Naquadria.getMolten(144)}, new ItemStack[]{ItemList.Magnetic_Confinement_Pod_AntiHydrogen.get(1L)}, 2400, 100000);


        RA.addCannerRecipe(ItemList.Magnetic_Confinement_Pod.get(1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 1L),
                ItemList.Magnetic_Confinement_Pod_Hydrogen.get(1L),
                ItemList.Cell_Empty.get(1L), 200, 480);

        if (Loader.isModLoaded("impact")) {
            RA.addAssemblylineRecipe(ItemList.Generator_Plasma_ZPMV.get(1), 576000, new Object[]{
                    ItemList.Casing_Dyson_Ring.get(1),
                    new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 4},
                    new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 8},
                    new Object[]{OrePrefixes.circuit.get(Materials.Master), 16},
                    new Object[]{OrePrefixes.circuit.get(Materials.Elite), 32},
                    GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 4L),
                    ItemList.Field_Generator_ZPM.get(2),
                    ItemList.Circuit_Wafer_NPIC.get(64),
                    GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.SuperconductorZPM, 32),
            }, new FluidStack[]{Materials.Naquadria.getMolten(2592),
                    Materials.SolderingAlloy.getMolten(2880),
            }, ItemList.Antimatter_Reactor.get(1), 1200, 120000);
        }
        
        // Circuits and Boards
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Circuit_Board_Coated.get(1L), new Object[]{GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 1), ItemList.IC2_Resin.get(1L), ItemList.IC2_Resin.get(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Board_Coated.get(3L), new Object[]{"RRR", "PPP", "RRR", 'P', GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 1), 'R', ItemList.IC2_Resin.get(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Board_Coated_Basic.get(1L), new Object[]{"FFF", "FCF", "FFF", 'C', ItemList.Circuit_Board_Coated.get(1L), 'F', GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Copper, 1)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Good.get(1L), new Object[]{"DAD", "CBC", "PPP", 'D', ItemList.Circuit_Parts_Diode.get(1L), 'C', Ic2Items.electronicCircuit, 'A', GT_OreDictUnificator.get(OrePrefixes.itemCasing, Materials.Steel, 1), 'P', GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Copper, 1), 'B', ItemList.Circuit_Board_Phenolic_Good.get(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Board_Phenolic_Good.get(1L), new Object[]{"FFF", "FCF", "FFF", 'C', ItemList.Circuit_Board_Phenolic.get(1L), 'F', GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.Gold, 1)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Resistor.get(1L), new Object[]{"RPR", "FCF", " P ", 'F', GT_OreDictUnificator.get(OrePrefixes.paper, Materials.Empty, 1), 'P', OrePrefixes.wireFine.get(Materials.Copper), 'C', OrePrefixes.dust.get(Materials.Coal), 'R', ItemList.IC2_Resin.get(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Resistor.get(1L), new Object[]{"RPR", "FCF", " P ", 'F', GT_OreDictUnificator.get(OrePrefixes.paper, Materials.Empty, 1), 'P', OrePrefixes.wireFine.get(Materials.Copper), 'C', OrePrefixes.dust.get(Materials.Carbon), 'R', ItemList.IC2_Resin.get(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Resistor.get(1L), new Object[]{"RPR", "FCF", " P ", 'F', GT_OreDictUnificator.get(OrePrefixes.paper, Materials.Empty, 1), 'P', OrePrefixes.wireFine.get(Materials.Copper), 'C', OrePrefixes.dust.get(Materials.Charcoal), 'R', ItemList.IC2_Resin.get(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Parts_Vacuum_Tube.get(1L), new Object[]{" G ", " F ", "S S", 'G', ItemList.Circuit_Parts_Glass_Tube.get(1L), 'F', OrePrefixes.springSmall.get(Materials.Copper), 'S', OrePrefixes.stick.get(Materials.Steel)});

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 32), Materials.Glue.getFluid(576), ItemList.Circuit_Board_Coated_Basic.get(8), 1600, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 32), Materials.Plastic.getMolten(288), ItemList.Circuit_Board_Coated_Basic.get(16), 1600, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 32), Materials.Polytetrafluoroethylene.getMolten(144), ItemList.Circuit_Board_Coated_Basic.get(16), 1600, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 32), Materials.Epoxid.getMolten(144), ItemList.Circuit_Board_Coated_Basic.get(24), 1600, 6);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 8), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 32), Materials.Polybenzimidazole.getMolten(72L), ItemList.Circuit_Board_Coated_Basic.get(32L), 1600, 6);

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 8), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Glue.getFluid(288), ItemList.Circuit_Board_Phenolic.get(8), 2400, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 8), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.BisphenolA.getFluid(144), ItemList.Circuit_Board_Phenolic.get(16), 2400, 18);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 8), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Epoxid.getMolten(144), ItemList.Circuit_Board_Phenolic.get(24), 2400, 30);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Phenolic.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Gold, 4), Materials.IronIIIChloride.getFluid(100), null, ItemList.Circuit_Board_Phenolic_Good.get(1), 300, 480);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Phenolic.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Gold, 4), Materials.SodiumPersulfate.getFluid(200), null, ItemList.Circuit_Board_Phenolic_Good.get(1), 600, 30);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Plastic, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 4), Materials.SulfuricAcid.getFluid(500), GT_Values.NF, ItemList.Circuit_Board_Plastic.get(1L), 500, 10);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.PolyvinylChloride, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 4), Materials.SulfuricAcid.getFluid(500), GT_Values.NF, ItemList.Circuit_Board_Plastic.get(2L), 500, 10);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Polytetrafluoroethylene, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 4), Materials.SulfuricAcid.getFluid(500), GT_Values.NF, ItemList.Circuit_Board_Plastic.get(4L), 500, 10);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Polybenzimidazole, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 4), Materials.SulfuricAcid.getFluid(500), GT_Values.NF, ItemList.Circuit_Board_Plastic.get(8L), 500, 10);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Plastic.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 6), Materials.IronIIIChloride.getFluid(250), null, ItemList.Circuit_Board_Plastic_Advanced.get(1), 400, 480);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Plastic.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 6), Materials.SodiumPersulfate.getFluid(500), null, ItemList.Circuit_Board_Plastic_Advanced.get(1), 800, 30);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Epoxid, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Gold, 8), Materials.SulfuricAcid.getFluid(500), null, ItemList.Circuit_Board_Epoxy.get(1), 600, 30);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Epoxy.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Electrum, 8), Materials.IronIIIChloride.getFluid(500), null, ItemList.Circuit_Board_Epoxy_Advanced.get(1), 600, 480);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Epoxy.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Electrum, 8), Materials.SodiumPersulfate.getFluid(1000), null, ItemList.Circuit_Board_Epoxy_Advanced.get(1), 1200, 30);

        RA.addChemicalBathRecipe(ItemList.Circuit_Parts_GlassFiber.get(1), Materials.Epoxid.getMolten(144), Materials.EpoxidFiberReinforced.getPlates(1), GT_Values.NI, GT_Values.NI, null, 240, 18);
        RA.addChemicalBathRecipe(GT_ModHandler.getIC2Item("carbonFiber", 1), Materials.Epoxid.getMolten(144), Materials.EpoxidFiberReinforced.getPlates(1), GT_Values.NI, GT_Values.NI, null, 240, 18);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.EpoxidFiberReinforced, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.VibrantAlloy, 1), Materials.SulfuricAcid.getFluid(125), null, ItemList.Circuit_Board_Fiberglass.get(1), 500, 10);

        RA.addChemicalRecipe(ItemList.Circuit_Board_Fiberglass.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.AnnealedCopper, 12), Materials.IronIIIChloride.getFluid(1000), null, ItemList.Circuit_Board_Fiberglass_Advanced.get(1), GT_Values.NI, 225, 7680, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Fiberglass.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.AnnealedCopper, 12), Materials.SodiumPersulfate.getFluid(2000), null, ItemList.Circuit_Board_Fiberglass_Advanced.get(1), GT_Values.NI, 450, 480, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Fiberglass.get(2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 16), Materials.SulfuricAcid.getFluid(500), null, ItemList.Circuit_Board_Multifiberglass.get(1), GT_Values.NI, 600, 480, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Multifiberglass.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 16), Materials.IronIIIChloride.getFluid(2000), null, ItemList.Circuit_Board_Multifiberglass_Elite.get(1), GT_Values.NI, 300, 30720, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Wetware.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.NiobiumTitanium, 32), Materials.IronIIIChloride.getFluid(5000), null, ItemList.Circuit_Board_Wetware_Extreme.get(1), GT_Values.NI, 375, 122880, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Multifiberglass.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 16), Materials.SodiumPersulfate.getFluid(4000), null, ItemList.Circuit_Board_Multifiberglass_Elite.get(1), GT_Values.NI, 600, 1920, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Wetware.get(1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.NiobiumTitanium, 32), Materials.SodiumPersulfate.getFluid(10000), null, ItemList.Circuit_Board_Wetware_Extreme.get(1), GT_Values.NI, 750, 7680, true);

        RA.addChemicalRecipe(ItemList.Circuit_Board_Bio.get(1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Neutronium, 48), Materials.IronIIIChloride.getFluid(7500), GT_Values.NF, ItemList.Circuit_Board_Bio_Ultra.get(1L), GT_Values.NI, 450, 491520, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Bio.get(1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Neutronium, 48), Materials.SodiumPersulfate.getFluid(15000L), GT_Values.NF, ItemList.Circuit_Board_Bio_Ultra.get(1L), GT_Values.NI, 900, 30720, true);

        RA.addChemicalRecipe(ItemList.Circuit_Board_Crystal.get(1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Phoenixite, 48), Materials.IronIIIChloride.getFluid(8000), GT_Values.NF, ItemList.Circuit_Board_Crystal_Extreme.get(1L), GT_Values.NI, 500, 2000000, true);
        RA.addChemicalRecipe(ItemList.Circuit_Board_Crystal.get(1L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Phoenixite, 48), Materials.SodiumPersulfate.getFluid(16000L), GT_Values.NF, ItemList.Circuit_Board_Crystal_Extreme.get(1L), GT_Values.NI, 1000, 122880, true);


        //For ULV Recipe
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(1), GT_OreDictUnificator.get(OrePrefixes.springSmall, Materials.Copper, 1), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 2)}, null, ItemList.Circuit_Parts_Vacuum_Tube.get(2), 160, 6);

        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1), ItemList.IC2_Resin.get(1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2)}, null, ItemList.Circuit_Parts_Resistor.get(1), 100, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1), ItemList.IC2_Resin.get(1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2)}, null, ItemList.Circuit_Parts_Resistor.get(1), 100, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Charcoal, 1), ItemList.IC2_Resin.get(1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2)}, null, ItemList.Circuit_Parts_Resistor.get(1), 100, 6);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(4), GT_OreDictUnificator.get(OrePrefixes.springSmall, Materials.Copper, 4), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 4)}, Materials.RedAlloy.getMolten(72), ItemList.Circuit_Parts_Vacuum_Tube.get(8), 160, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(4), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 4)}, Materials.RedstoneAlloy.getMolten(72), ItemList.Circuit_Parts_Vacuum_Tube.get(12), 160, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(4), GT_OreDictUnificator.get(OrePrefixes.springSmall, Materials.AnnealedCopper, 4), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 4)}, Materials.RedAlloy.getMolten(72), ItemList.Circuit_Parts_Vacuum_Tube.get(12), 160, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Circuit_Parts_Glass_Tube.get(4), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 4), GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Steel, 4)}, Materials.RedstoneAlloy.getMolten(72), ItemList.Circuit_Parts_Vacuum_Tube.get(16), 160, 6);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4)}, Materials.Glue.getFluid(288), ItemList.Circuit_Parts_Resistor.get(4), 320, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4)}, Materials.Glue.getFluid(288), ItemList.Circuit_Parts_Resistor.get(4), 320, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Charcoal, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4)}, Materials.Glue.getFluid(288), ItemList.Circuit_Parts_Resistor.get(4), 320, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Coal, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 4)}, Materials.Glue.getFluid(288), ItemList.Circuit_Parts_Resistor.get(8), 320, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 4)}, Materials.Glue.getFluid(288), ItemList.Circuit_Parts_Resistor.get(8), 320, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Charcoal, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 4)}, Materials.Glue.getFluid(288), ItemList.Circuit_Parts_Resistor.get(8), 320, 18);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Electrum, 4)}, Materials.Plastic.getMolten(288), ItemList.Circuit_Parts_ResistorSMD.get(16), 160, 480);
        RA.addAlloySmelterRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Glass, 1), ItemList.Shape_Mold_Ball.get(0), ItemList.Circuit_Parts_Glass_Tube.get(1), 240, 18);

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GalliumArsenide, 1), Materials.Plastic.getMolten(576), ItemList.Circuit_Parts_Diode.get(16), 800, 30);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GalliumArsenide, 1), Materials.Glass.getMolten(1152), ItemList.Circuit_Parts_Diode.get(8), 800, 30);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 16), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GalliumArsenide, 1), Materials.Plastic.getMolten(576), ItemList.Circuit_Parts_Diode.get(16), 200, 120);

        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4), ItemList.Circuit_Silicon_Wafer.get(1), Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_Diode.get(2), 800, 30);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4), ItemList.Circuit_Silicon_Wafer.get(1), Materials.Glass.getMolten(288), ItemList.Circuit_Parts_Diode.get(1), 800, 30);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 4), ItemList.Circuit_Silicon_Wafer.get(1), Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_Diode.get(4), 200, 120);

        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Platinum, 8), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.GalliumArsenide, 1)}, Materials.Plastic.getMolten(288), ItemList.Circuit_Parts_DiodeSMD.get(32), 200, 480);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Steel, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2), Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_Coil.get(2), 320, 30);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ring, Materials.NickelZincFerrite, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2), Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_Coil.get(4), 320, 30);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Steel, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 2), Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_Coil.get(4), 320, 75);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.ring, Materials.NickelZincFerrite, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 2), Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_Coil.get(8), 320, 75);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Silicon, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Tin, 8), Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_Transistor.get(8), 320, 120);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Gallium, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.AnnealedCopper, 8)}, Materials.Plastic.getMolten(288), ItemList.Circuit_Parts_TransistorSMD.get(16), 160, 480);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Plastic, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 2), Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_Capacitor.get(8), 320, 120);
        //SMD Capacitor
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Silicone, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 2)}, Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_CapacitorSMD.get(16), 160, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.PolyvinylChloride, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Aluminium, 2)}, Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_CapacitorSMD.get(24), 160, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Silicone, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tantalum, 2)}, Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_CapacitorSMD.get(32), 240, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.PolyvinylChloride, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tantalum, 2)}, Materials.Plastic.getMolten(144), ItemList.Circuit_Parts_CapacitorSMD.get(48), 240, 480);

        //SMD Inductor
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Neodymium, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.ConductiveIron, 8)}, Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_InductorSMD.get(24), 320, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Neodymium, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Platinum, 8)}, Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_InductorSMD.get(32), 320, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Neodymium, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Tantalum, 8)}, Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_InductorSMD.get(48), 320, 480);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Neodymium, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Iridium, 8)}, Materials.Plastic.getMolten(36), ItemList.Circuit_Parts_InductorSMD.get(64), 320, 480);

        //Lapotron Crystal
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.Lapotron, 1), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Advanced, 2), GT_Values.NF, ItemList.LapotronCrystal.get(1), 600, 1200);

        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Sapphire, 1), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Lapis, 2), ItemList.Circuit_Parts_Wiring_Elite.get(2)}, Materials.EnergeticAlloy.getMolten(144L), ItemList.LapotronCrystal.get(1), 600, 1200);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.gemFlawless, Materials.Sapphire, 1), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Lapis, 1), ItemList.Circuit_Parts_Wiring_Elite.get(1)}, Materials.VibrantAlloy.getMolten(36L), ItemList.LapotronCrystal.get(1), 300, 1200);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Sapphire, 1), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Lazurite, 2), ItemList.Circuit_Parts_Wiring_Elite.get(2)}, Materials.EnergeticAlloy.getMolten(144L), ItemList.LapotronCrystal.get(1), 600, 1200);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.gemFlawless, Materials.Sapphire, 1), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Lazurite, 1), ItemList.Circuit_Parts_Wiring_Elite.get(1)}, Materials.VibrantAlloy.getMolten(36L), ItemList.LapotronCrystal.get(1), 300, 1200);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Sapphire, 1), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Sodalite, 2), ItemList.Circuit_Parts_Wiring_Elite.get(2)}, Materials.EnergeticAlloy.getMolten(144L), ItemList.LapotronCrystal.get(1), 600, 1200);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.gemFlawless, Materials.Sapphire, 1), GT_OreDictUnificator.get(OrePrefixes.stick, Materials.Sodalite, 1), ItemList.Circuit_Parts_Wiring_Elite.get(1)}, Materials.VibrantAlloy.getMolten(36L), ItemList.LapotronCrystal.get(1), 300, 1200);


        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0), Materials.Polytetrafluoroethylene.getMolten(36), ItemList.Circuit_Parts_PetriDish.get(1), 160, 18);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Cylinder.get(0), Materials.Polystyrene.getMolten(36), ItemList.Circuit_Parts_PetriDish.get(1), 160, 18);

        RA.addExtruderRecipe(Materials.BorosilicateGlass.getIngots(1), ItemList.Shape_Extruder_Wire.get(0), ItemList.Circuit_Parts_GlassFiber.get(8), 160, 96);
        RA.addWiremillRecipe(Materials.BorosilicateGlass.getIngots(1), ItemList.Circuit_Parts_GlassFiber.get(8), 200, 120);


        // RA.addChemicalRecipe(GT_OreDictUnificator.get(Materials.Nitrogen.getCells(3)), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnetite, 0), Materials.Hydrogen.getGas(1000), Materials.Ammonia.getGas(1000), ItemList.Cell_Empty.get(3L), 320, 384);

        if (Loader.isModLoaded("GalacticraftCore")) {
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Emerald, 1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1), 1000, 12000, 320, true);
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Olivine, 1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1), 1000, 12000, 320, true);
        } else {
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Emerald, 1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1), 1000, 12000, 320, true);
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Olivine, 1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1), 1000, 12000, 320, true);
        }

        RA.addForgeHammerRecipe(ItemList.Circuit_Parts_RawCrystalChip.get(1), ItemList.Circuit_Parts_RawCrystalParts.get(9), 100, 480);
        //RA.addAutoclaveRecipe(ItemList.Circuit_Parts_RawCrystalParts.get(1), FluidRegistry.getFluidStack("bacterialsludge", 250), ItemList.Circuit_Parts_RawCrystalChip.get(1L), 6000, 12000, 480);
        //RA.addAutoclaveRecipe(ItemList.Circuit_Parts_RawCrystalParts.get(1), FluidRegistry.getFluidStack("mutagen", 250), ItemList.Circuit_Parts_RawCrystalChip.get(1L), 8000, 12000, 480);
        RA.addAutoclaveRecipe(ItemList.Circuit_Parts_RawCrystalParts.get(1), Materials.Europium.getMolten(16), ItemList.Circuit_Parts_RawCrystalChip.get(1L), 10000, 12000, 480);

        if (Loader.isModLoaded("GalacticraftCore")) {
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Emerald, 12), Materials.Iridium.getMolten(288), ItemList.Tool_DataOrb.get(1), 10000, 12000, 960, true);
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Olivine, 12), Materials.Iridium.getMolten(288), ItemList.Tool_DataOrb.get(1), 10000, 12000, 960, true);
        } else {
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Emerald, 12), Materials.Iridium.getMolten(288), ItemList.Tool_DataOrb.get(1), 10000, 12000, 960, true);
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.Olivine, 12), Materials.Iridium.getMolten(288), ItemList.Tool_DataOrb.get(1), 10000, 12000, 960, true);
        }
        //GT_ModHandler.addShapelessCraftingRecipe(ItemList.Circuit_Parts_RawCrystalChip.get(9), new Object[]{ItemList.Circuit_Chip_CrystalCPU.get(1)});

        RA.addCentrifugeRecipe(ItemList.Cell_Air.get(5L), GT_Values.NI, GT_Values.NF, Materials.Nitrogen.getGas(3900L), Materials.Oxygen.getCells(1), ItemList.Cell_Empty.get(4L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 10000}, 1600, 6);

        RA.addChemicalBathRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MysteriousCrystal, 1), Materials.Naquadria.getMolten(144), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnrichedMysteriousCrystal, 1), null, null, null, 600, 500000);
        if (Loader.isModLoaded("GalacticraftCore")) {
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnrichedMysteriousCrystal, 1), Materials.EnrichedNaquadria.getFluid(288), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnrichedMysteriousCrystal, 1), 8500, 2000, 500000, true);
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MysteriousCrystal, 1), Materials.EnrichedNaquadria.getFluid(288), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.MysteriousCrystal, 1), 10000, 2000, 500000, true);
        } else {
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnrichedMysteriousCrystal, 1), Materials.EnrichedNaquadria.getFluid(288), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.EnrichedMysteriousCrystal, 1), 8500, 2000, 500000, true);
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.MysteriousCrystal, 1), Materials.EnrichedNaquadria.getFluid(288), GT_OreDictUnificator.get(OrePrefixes.gem, Materials.MysteriousCrystal, 1), 10000, 2000, 500000, true);
        }


        //Coil
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_Cupronickel.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Cupronickel, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Bronze, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Tin, 1)}, null, 400, 96);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_Kanthal.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Kanthal, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Aluminium, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.AnnealedCopper, 1)}, null, 800, 96);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_Nichrome.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Nichrome, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.StainlessSteel, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Aluminium, 1)}, null, 1200, 384);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_TungstenSteel.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenSteel, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.VanadiumSteel, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Nichrome, 1)}, null, 1600, 384);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_HSSG.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.HSSG, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenCarbide, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Tungsten, 1)}, null, 2000, 1536);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_Naquadah.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Naquadah, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Osmium, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenSteel, 1)}, null, 2400, 1536);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_NaquadahAlloy.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.NaquadahAlloy, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Osmiridium, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Naquadah, 1)}, null, 2800, 6144);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_ElectrumFlux.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.ElectrumFlux, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Naquadria, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.NaquadahAlloy, 1)}, null, 3200, 24576);
        RA.addArcFurnaceRecipe(ItemList.Casing_Coil_Diamericiumtitanium.get(1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Diamericiumtitanium, 8), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Infuscolium, 2), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Neutronium, 1)}, null, 3200, 24576);

        //New Fuels
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Naquadria.getMolten(4608L), Materials.ElectrumFlux.getMolten(4608L), Materials.Radon.getGas(16000L)}, new FluidStack[]{Materials.EnrichedNaquadria.getFluid(9216L)}, null, 600, 500000);
        RA.addCentrifugeRecipe(GT_Values.NI, GT_Values.NI, Materials.EnrichedNaquadria.getFluid(9216L), Materials.FluidNaquadahFuel.getFluid(4806L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 8L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.ElectrumFlux, 8L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000, 10000}, 600, 2000000);


    }

	/*private void addOldChemicalRecipes() {
		RA.setIsAddingDeprecatedRecipes(true);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Nitrogen, 1L), ItemList.Circuit_Integrated.getWithDamage(0, 1), Materials.Oxygen.getGas(2000L), Materials.NitrogenDioxide.getGas(3000L), ItemList.Cell_Empty.get(1L), 1320);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 2L), ItemList.Circuit_Integrated.getWithDamage(0, 1), Materials.Nitrogen.getGas(1000L), Materials.NitrogenDioxide.getGas(3000L), ItemList.Cell_Empty.get(2L), 1320);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Water.getFluid(2000L), Materials.SulfuricAcid.getFluid(3000L), GT_Values.NI, 1150);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 2L), Materials.Oxygen.getGas(4000L), Materials.SulfuricAcid.getFluid(7000L), ItemList.Cell_Empty.get(2L), 1150);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 4L), Materials.Hydrogen.getGas(2000L), Materials.SulfuricAcid.getFluid(7000L), ItemList.Cell_Empty.get(4L), 1150);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), ItemList.Cell_Water.get(1), null, Materials.SulfuricAcid.getFluid(1500), ItemList.Cell_Empty.get(2), 320);
        RA.addChemicalRecipe(ItemList.Cell_Water.get(1),                               null, new FluidStack(ItemList.sHydricSulfur, 1000), Materials.SulfuricAcid.getFluid(1500), ItemList.Cell_Empty.get(1), 320);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.HydricSulfide, 1L), null, Materials.Water.getFluid(1000),         Materials.SulfuricAcid.getFluid(1500), ItemList.Cell_Empty.get(1), 320);

        RA.addChemicalRecipe(ItemList.Cell_Air.get(2), null, Materials.Naphtha.getFluid(288), Materials.Plastic.getMolten(144), ItemList.Cell_Empty.get(2), 640);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dustTiny, Materials.Titanium, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Oxygen, 16L), Materials.Naphtha.getFluid(1296), Materials.Plastic.getMolten(1296), ItemList.Cell_Empty.get(16), 640);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Naphtha, 3L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.NitrogenDioxide, 1L), new FluidStack(ItemList.sEpichlorhydrin, 144), Materials.Epoxid.getMolten(288), ItemList.Cell_Empty.get(4), 240, 30);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Chlorine, 1L), Materials.LPG.getFluid(432), new FluidStack(ItemList.sEpichlorhydrin, 432), ItemList.Cell_Empty.get(1), 480, 30);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Naphtha, 3L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Fluorine, 1L), new FluidStack(ItemList.sEpichlorhydrin, 432), Materials.Polytetrafluoroethylene.getMolten(432), ItemList.Cell_Empty.get(4), 240, 320);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silicon, 1L), null, new FluidStack(ItemList.sEpichlorhydrin, 144), Materials.Silicone.getMolten(144), GT_Values.NI, 240, 96);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.NitrogenDioxide, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 3L), Materials.Air.getGas(500), new FluidStack(ItemList.sRocketFuel,3000), ItemList.Cell_Water.get(4), 1000, 388);

        RA.addCentrifugeRecipe(GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.Gas.getGas(8000), Materials.LPG.getFluid(4000), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000}, 200, 5);
        RA.addCentrifugeRecipe(ItemList.Cell_Empty.get(4), GT_Values.NI, Materials.Gas.getGas(8000), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.cell, Materials.LPG, 4), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000}, 200, 5);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Fuel, 1L), GT_Values.NI, Materials.Glyceryl.getFluid(250L), Materials.NitroFuel.getFluid(1000L), ItemList.Cell_Empty.get(1L), 320);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Glyceryl, 1L), GT_Values.NI, Materials.Fuel.getFluid(4000L), Materials.NitroFuel.getFluid(4000L), ItemList.Cell_Empty.get(1L), 1000);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.LightFuel, 1L), GT_Values.NI, Materials.Glyceryl.getFluid(250L), Materials.NitroFuel.getFluid(1250L), ItemList.Cell_Empty.get(1L), 320);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Glyceryl, 1L), GT_Values.NI, Materials.LightFuel.getFluid(4000L), Materials.NitroFuel.getFluid(5000L), ItemList.Cell_Empty.get(1L), 1000);

        RA.addMixerRecipe(Materials.Sodium.getDust(2), Materials.Sulfur.getDust(1), GT_Utility.getIntegratedCircuit(2), null, null, null, Materials.SodiumSulfide.getDust(1), 60, 30);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Nitrogen, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1L), Materials.Water.getFluid(2000L), Materials.Glyceryl.getFluid(4000L), ItemList.Cell_Empty.get(1L), 2700);
        RA.setIsAddingDeprecatedRecipes(false);
	}*/

    private void addRecipesApril2017ChemistryUpdate() {
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.CarbonDioxide.getGas(1000), Materials.Oxygen.getGas(2000), Materials.Carbon.getDust(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 300, 120);
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(11), Materials.Empty.getCells(2), Materials.CarbonDioxide.getGas(1000), GT_Values.NF, Materials.Carbon.getDust(1), Materials.Oxygen.getCells(2), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 300, 120);
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(1), GT_Values.NI, Materials.SulfurDioxide.getGas(1000), Materials.Oxygen.getGas(2000), Materials.Sulfur.getDust(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 300, 120);
        RA.addElectrolyzerRecipe(GT_Utility.getIntegratedCircuit(11), Materials.Empty.getCells(2), Materials.SulfurDioxide.getGas(1000), GT_Values.NF, Materials.Sulfur.getDust(1), Materials.Oxygen.getCells(2), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 300, 120);
        RA.addElectrolyzerRecipe(Materials.Salt.getDust(2), GT_Values.NI, GT_Values.NF, Materials.Chlorine.getGas(1000), Materials.Sodium.getDust(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 320, 30);
        RA.addElectrolyzerRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.SaltWater.getFluid(2000), Materials.Chlorine.getGas(1000), Materials.SodiumHydroxide.getDust(1), Materials.Hydrogen.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 720, 30);
        RA.addElectrolyzerRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.SaltWater.getFluid(2000), Materials.Hydrogen.getGas(1000), Materials.SodiumHydroxide.getDust(1), Materials.Chlorine.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 720, 30);
        RA.addElectrolyzerRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.HydrochloricAcid.getFluid(1000), Materials.Chlorine.getGas(1000), Materials.Hydrogen.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 720, 30);
        RA.addElectrolyzerRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.HydrochloricAcid.getFluid(1000), Materials.Hydrogen.getGas(1000), Materials.Chlorine.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 720, 30);
        RA.addElectrolyzerRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), GT_Values.NF, Materials.Chlorine.getGas(1000), Materials.Hydrogen.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 720, 30);
        RA.addElectrolyzerRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), GT_Values.NF, Materials.Hydrogen.getGas(1000), Materials.Chlorine.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 720, 30);

        RA.addUniversalDistillationRecipe(Materials.DilutedHydrochloricAcid.getFluid(2000), new FluidStack[]{Materials.Water.getFluid(1000), Materials.HydrochloricAcid.getFluid(1000)}, GT_Values.NI, 600, 75);

        RA.addChemicalRecipe(Materials.Potassium.getDust(1), Materials.Oxygen.getCells(3), Materials.Nitrogen.getGas(1000), GT_Values.NF, Materials.Saltpeter.getDust(5), Materials.Empty.getCells(3), 180);
        RA.addChemicalRecipe(Materials.Potassium.getDust(1), Materials.Nitrogen.getCells(1), Materials.Oxygen.getGas(3000), GT_Values.NF, Materials.Saltpeter.getDust(5), Materials.Empty.getCells(1), 180);

        RA.addMixerRecipe(Materials.Salt.getDust(2), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Water.getFluid(1000), Materials.SaltWater.getFluid(2000), GT_Values.NI, 40, 6);
        RA.addUniversalDistillationRecipe(Materials.SaltWater.getFluid(2000), new FluidStack[]{GT_ModHandler.getDistilledWater(1000)}, Materials.Salt.getDust(1), 320, 18);

        RA.addUniversalDistillationRecipe(FluidRegistry.getFluidStack("potion.vinegar", 40), new FluidStack[]{Materials.AceticAcid.getFluid(5), Materials.Water.getFluid(35)}, GT_Values.NI, 20, 75);
        RA.addMixerRecipe(Materials.Calcite.getDust(1), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NI, Materials.AceticAcid.getFluid(2000), Materials.CalciumAcetateSolution.getFluid(1000), GT_Values.NI, 240, 18);
        RA.addMixerRecipe(Materials.Calcium.getDust(1), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NI, Materials.AceticAcid.getFluid(2000), Materials.CalciumAcetateSolution.getFluid(1000), GT_Values.NI, 80, 18);
        RA.addMixerRecipe(Materials.Quicklime.getDust(1), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NI, Materials.AceticAcid.getFluid(2000), Materials.CalciumAcetateSolution.getFluid(1000), GT_Values.NI, 80, 18);
        RA.addMixerRecipe(Materials.Calcite.getDust(1), Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(11), GT_Values.NI, Materials.AceticAcid.getFluid(2000), GT_Values.NF, Materials.CalciumAcetateSolution.getCells(1), 240, 18);
        RA.addMixerRecipe(Materials.Calcium.getDust(1), Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(11), GT_Values.NI, Materials.AceticAcid.getFluid(2000), GT_Values.NF, Materials.CalciumAcetateSolution.getCells(1), 80, 18);
        RA.addMixerRecipe(Materials.Quicklime.getDust(1), Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(11), GT_Values.NI, Materials.AceticAcid.getFluid(2000), GT_Values.NF, Materials.CalciumAcetateSolution.getCells(1), 80, 18);
        //GameRegistry.addSmelting(Materials.CalciumAcetateSolution.getCells(1), Materials.Acetone.getCells(1), 0);
        RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), Materials.CalciumAcetateSolution.getFluid(1000), Materials.Acetone.getFluid(1000), 80, 30);
        RA.addUniversalDistillationRecipe(Materials.CalciumAcetateSolution.getFluid(1200), new FluidStack[]{Materials.Acetone.getFluid(1000), Materials.CarbonDioxide.getGas(1000)}, Materials.Quicklime.getDust(1), 80, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Calcite.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.AceticAcid.getFluid(4000)}, new FluidStack[]{Materials.Acetone.getFluid(4000), Materials.CarbonDioxide.getGas(4000)}, null, 400, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Calcium.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.AceticAcid.getFluid(4000)}, new FluidStack[]{Materials.Acetone.getFluid(4000), Materials.CarbonDioxide.getGas(4000)}, null, 400, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Quicklime.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.AceticAcid.getFluid(4000)}, new FluidStack[]{Materials.Acetone.getFluid(4000), Materials.CarbonDioxide.getGas(4000)}, null, 400, 480);

        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Methanol.getFluid(1000), Materials.MethylAcetate.getFluid(1000), Materials.Water.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.AceticAcid.getFluid(1000), Materials.MethylAcetate.getFluid(1000), Materials.Water.getCells(1), 280);
        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Methanol.getFluid(1000), Materials.MethylAcetate.getFluid(1000), Materials.Empty.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.AceticAcid.getFluid(1000), Materials.MethylAcetate.getFluid(1000), Materials.Empty.getCells(1), 280);
        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Methanol.getFluid(1000), Materials.Water.getFluid(1000), Materials.MethylAcetate.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.AceticAcid.getFluid(1000), Materials.Water.getFluid(1000), Materials.MethylAcetate.getCells(1), 280);
        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Methanol.getFluid(1000), GT_Values.NF, Materials.MethylAcetate.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.AceticAcid.getFluid(1000), GT_Values.NF, Materials.MethylAcetate.getCells(1), 280);

        RA.addMixerRecipe(Materials.Acetone.getCells(3), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.PolyvinylAcetate.getFluid(2000), Materials.Glue.getFluid(5000), Materials.Empty.getCells(3), 100, 6);
        RA.addMixerRecipe(Materials.PolyvinylAcetate.getCells(2), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Acetone.getFluid(3000), Materials.Glue.getFluid(5000), Materials.Empty.getCells(2), 100, 6);
        RA.addMixerRecipe(Materials.MethylAcetate.getCells(3), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.PolyvinylAcetate.getFluid(2000), Materials.Glue.getFluid(5000), Materials.Empty.getCells(3), 100, 6);
        RA.addMixerRecipe(Materials.PolyvinylAcetate.getCells(2), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.MethylAcetate.getFluid(3000), Materials.Glue.getFluid(5000), Materials.Empty.getCells(2), 100, 6);

        RA.addChemicalRecipe(Materials.Carbon.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.CarbonMonoxide.getGas(1000), GT_Values.NI, 40, 6);
        RA.addChemicalRecipe(Materials.Coal.getGems(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.CarbonMonoxide.getGas(1000), GT_Values.NI, 80, 6);
        RA.addChemicalRecipe(Materials.Coal.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.CarbonMonoxide.getGas(1000), GT_Values.NI, 80, 6);
        RA.addChemicalRecipe(Materials.Charcoal.getGems(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.CarbonMonoxide.getGas(1000), GT_Values.NI, 80, 6);
        RA.addChemicalRecipe(Materials.Charcoal.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.CarbonMonoxide.getGas(1000), GT_Values.NI, 80, 6);
        RA.addChemicalRecipe(Materials.Carbon.getDust(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(2000), Materials.CarbonDioxide.getGas(1000), GT_Values.NI, 40, 6);
        RA.addChemicalRecipe(Materials.Coal.getGems(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(2000), Materials.CarbonDioxide.getGas(1000), GT_Values.NI, 40, 6);
        RA.addChemicalRecipe(Materials.Coal.getDust(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(2000), Materials.CarbonDioxide.getGas(1000), GT_Values.NI, 40, 6);
        RA.addChemicalRecipe(Materials.Charcoal.getGems(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(2000), Materials.CarbonDioxide.getGas(1000), GT_Values.NI, 40, 6);
        RA.addChemicalRecipe(Materials.Charcoal.getDust(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(2000), Materials.CarbonDioxide.getGas(1000), GT_Values.NI, 40, 6);
        RA.addChemicalRecipe(Materials.Carbon.getDust(1), GT_Values.NI, Materials.CarbonDioxide.getGas(1000), Materials.CarbonMonoxide.getGas(2000), GT_Values.NI, 800);

        RA.addChemicalRecipe(Materials.CarbonMonoxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(4000), Materials.Methanol.getFluid(1000), Materials.Empty.getCells(1), 120, 96);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(4), GT_Utility.getIntegratedCircuit(1), Materials.CarbonMonoxide.getGas(1000), Materials.Methanol.getFluid(1000), Materials.Empty.getCells(4), 120, 96);
        RA.addChemicalRecipe(Materials.CarbonMonoxide.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Hydrogen.getGas(4000), GT_Values.NF, Materials.Methanol.getCells(1), 120, 96);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(4), GT_Utility.getIntegratedCircuit(11), Materials.CarbonMonoxide.getGas(1000), GT_Values.NF, Materials.Methanol.getCells(1), Materials.Empty.getCells(3), 120, 96);
        RA.addChemicalRecipe(Materials.CarbonDioxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(6000), Materials.Methanol.getFluid(1000), Materials.Water.getCells(1), 120, 96);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(6), GT_Utility.getIntegratedCircuit(1), Materials.CarbonDioxide.getGas(1000), Materials.Methanol.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(3), 120, 96);
        RA.addChemicalRecipe(Materials.CarbonDioxide.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Hydrogen.getGas(6000), Materials.Methanol.getFluid(1000), Materials.Empty.getCells(1), 120, 96);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(6), GT_Utility.getIntegratedCircuit(2), Materials.CarbonDioxide.getGas(1000), Materials.Methanol.getFluid(1000), Materials.Empty.getCells(6), 120, 96);
        RA.addChemicalRecipe(Materials.CarbonDioxide.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Hydrogen.getGas(6000), GT_Values.NF, Materials.Methanol.getCells(1), 120, 96);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(6), GT_Utility.getIntegratedCircuit(12), Materials.CarbonDioxide.getGas(1000), GT_Values.NF, Materials.Methanol.getCells(1), Materials.Empty.getCells(5), 120, 96);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Carbon.getDust(1), GT_Utility.getIntegratedCircuit(23)}, new FluidStack[]{Materials.Hydrogen.getGas(4000), Materials.Oxygen.getGas(1000)}, new FluidStack[]{Materials.Methanol.getFluid(1000)}, null, 320, 96);

        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.CarbonMonoxide.getGas(1000), Materials.AceticAcid.getFluid(1000), Materials.Empty.getCells(1), 300);
        RA.addChemicalRecipe(Materials.CarbonMonoxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Methanol.getFluid(1000), Materials.AceticAcid.getFluid(1000), Materials.Empty.getCells(1), 300);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.CarbonMonoxide.getGas(1000), GT_Values.NF, Materials.AceticAcid.getCells(1), 300);
        RA.addChemicalRecipe(Materials.CarbonMonoxide.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Methanol.getFluid(1000), GT_Values.NF, Materials.AceticAcid.getCells(1), 300);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(9), Materials.Oxygen.getGas(2000), Materials.AceticAcid.getFluid(1000), Materials.Empty.getCells(1), 100);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(2), GT_Utility.getIntegratedCircuit(9), Materials.Ethylene.getGas(1000), Materials.AceticAcid.getFluid(1000), Materials.Empty.getCells(2), 100);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(19), Materials.Oxygen.getGas(2000), GT_Values.NF, Materials.AceticAcid.getCells(1), 100);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(2), GT_Utility.getIntegratedCircuit(19), Materials.Ethylene.getGas(1000), GT_Values.NF, Materials.AceticAcid.getCells(1), Materials.Empty.getCells(1), 100);
        //This recipe collides with one for Vinyl Chloride
        //RA.addChemicalRecipeForBasicMachineOnly(Materials.Oxygen.getCells(2), Materials.Ethylene.getCells(1), GT_Values.NF, Materials.AceticAcid.getFluid(1000), Materials.Empty.getCells(3), GT_Values.NI, 100, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Carbon.getDust(2), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Hydrogen.getGas(4000), Materials.Oxygen.getGas(2000)}, new FluidStack[]{Materials.AceticAcid.getFluid(1000)}, null, 480, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.CarbonMonoxide.getGas(2000), Materials.Hydrogen.getGas(4000)}, new FluidStack[]{Materials.AceticAcid.getFluid(1000)}, null, 320, 30);

        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), Materials.Biomass.getFluid(1000), Materials.FermentedBiomass.getFluid(1000), 370, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 1L), new FluidStack(FluidRegistry.getFluid("ic2biomass"), 100), Materials.FermentedBiomass.getFluid(100), 150, false);
        RA.addFermentingRecipe(ItemList.Circuit_Integrated.getWithDamage(0L, 2L), Materials.Biomass.getFluid(100), new FluidStack(FluidRegistry.getFluid("ic2biomass"), 100), 150, false);

        RA.addDistillationTowerRecipe(Materials.FermentedBiomass.getFluid(1000), new FluidStack[]{
                Materials.AceticAcid.getFluid(25), Materials.Water.getFluid(375), Materials.Ethanol.getFluid(150),
                Materials.Methanol.getFluid(150), Materials.Ammonia.getGas(100), Materials.CarbonDioxide.getGas(400),
                Materials.Methane.getGas(600)}, ItemList.IC2_Fertilizer.get(1), 75, 180);
        RA.addDistilleryRecipe(1, Materials.FermentedBiomass.getFluid(1000), Materials.AceticAcid.getFluid(25), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);
        RA.addDistilleryRecipe(2, Materials.FermentedBiomass.getFluid(1000), Materials.Water.getFluid(375), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);
        RA.addDistilleryRecipe(3, Materials.FermentedBiomass.getFluid(1000), Materials.Ethanol.getFluid(150), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);
        RA.addDistilleryRecipe(4, Materials.FermentedBiomass.getFluid(1000), Materials.Methanol.getFluid(150), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);
        RA.addDistilleryRecipe(5, Materials.FermentedBiomass.getFluid(1000), Materials.Ammonia.getGas(100), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);
        RA.addDistilleryRecipe(6, Materials.FermentedBiomass.getFluid(1000), Materials.CarbonDioxide.getGas(400), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);
        RA.addDistilleryRecipe(7, Materials.FermentedBiomass.getFluid(1000), Materials.Methane.getGas(600), ItemList.IC2_Fertilizer.get(1), 1500, 6, false);

        RA.addDistilleryRecipe(17, Materials.FermentedBiomass.getFluid(1000), new FluidStack(FluidRegistry.getFluid("ic2biogas"), 1800), ItemList.IC2_Fertilizer.get(1), 1600, 6, false);
        RA.addDistilleryRecipe(1, Materials.Methane.getGas(1000), new FluidStack(FluidRegistry.getFluid("ic2biogas"), 3000), GT_Values.NI, 160, 6, false);

        RA.addUniversalDistillationRecipe(Materials.CharcoalByproducts.getGas(4000),
                new FluidStack[]{Materials.WoodTar.getFluid(1000), Materials.WoodVinegar.getFluid(1600), Materials.WoodGas.getGas(10000), Materials.Dimethylbenzene.getFluid(400)},
                Materials.Charcoal.getDust(1), 160, 320);
        RA.addUniversalDistillationRecipe(Materials.WoodGas.getGas(1000),
                new FluidStack[]{Materials.CarbonDioxide.getGas(390), Materials.Ethylene.getGas(120), Materials.Methane.getGas(130), Materials.CarbonMonoxide.getGas(240), Materials.Hydrogen.getGas(120)},
                GT_Values.NI, 40, 320);
        RA.addUniversalDistillationRecipe(Materials.WoodVinegar.getFluid(1000),
                new FluidStack[]{Materials.AceticAcid.getFluid(100), Materials.Water.getFluid(500), Materials.Ethanol.getFluid(10), Materials.Methanol.getFluid(300), Materials.Acetone.getFluid(50), Materials.MethylAcetate.getFluid(10)},
                GT_Values.NI, 40, 320);
        RA.addUniversalDistillationRecipe(Materials.WoodTar.getFluid(1000),
                new FluidStack[]{Materials.Creosote.getFluid(250), Materials.Phenol.getFluid(100), Materials.Benzene.getFluid(400), Materials.Toluene.getFluid(100), Materials.Dimethylbenzene.getFluid(150)},
                GT_Values.NI, 40, 320);

        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), Materials.AceticAcid.getCells(1), Materials.Oxygen.getGas(1000), Materials.VinylAcetate.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 180);
        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), Materials.Oxygen.getCells(1), Materials.Ethylene.getGas(1000), Materials.VinylAcetate.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 180);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), Materials.Ethylene.getCells(1), Materials.AceticAcid.getFluid(1000), Materials.VinylAcetate.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 180);

        RA.addDefaultPolymerizationRecipes(Materials.VinylAcetate.mFluid, Materials.VinylAcetate.getCells(1), Materials.PolyvinylAcetate.mFluid);

        RA.addChemicalRecipe(Materials.Ethanol.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.SulfuricAcid.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Ethylene.getCells(1), 1200, 120);
        RA.addChemicalRecipe(Materials.SulfuricAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Ethanol.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Ethylene.getCells(1), 1200, 120);
        RA.addChemicalRecipe(Materials.Ethanol.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.SulfuricAcid.getFluid(1000), Materials.Ethylene.getGas(1000), Materials.DilutedSulfuricAcid.getCells(1), 1200, 120);
        RA.addChemicalRecipe(Materials.SulfuricAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Ethanol.getFluid(1000), Materials.Ethylene.getGas(1000), Materials.DilutedSulfuricAcid.getCells(1), 1200, 120);

        RA.addDefaultPolymerizationRecipes(Materials.Ethylene.mGas, Materials.Ethylene.getCells(1), Materials.Plastic.mStandardMoltenFluid);

        RA.addChemicalRecipe(Materials.Sodium.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Water.getFluid(1000), GT_Values.NF, Materials.SodiumHydroxide.getDust(1), 40, 6);

        RA.addChemicalRecipe(Materials.Chlorine.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.Empty.getCells(1), 60, 6);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorine.getGas(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.Empty.getCells(1), 60, 6);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Hydrogen.getGas(1000), GT_Values.NF, Materials.HydrochloricAcid.getCells(1), 60, 6);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorine.getGas(1000), GT_Values.NF, Materials.HydrochloricAcid.getCells(1), 60, 6);

        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Propene.getGas(1000), Materials.AllylChloride.getFluid(1000), Materials.HydrochloricAcid.getCells(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Propene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorine.getGas(2000), Materials.AllylChloride.getFluid(1000), Materials.HydrochloricAcid.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.Propene.getGas(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.AllylChloride.getCells(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Propene.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorine.getGas(2000), Materials.HydrochloricAcid.getFluid(1000), Materials.AllylChloride.getCells(1), 275);

        RA.addChemicalRecipeForBasicMachineOnly(Materials.Chlorine.getCells(10), Materials.Mercury.getCells(1), Materials.Water.getFluid(10000), Materials.HypochlorousAcid.getFluid(10000), Materials.Empty.getCells(11), GT_Values.NI, 600, 6);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Water.getCells(10), Materials.Mercury.getCells(1), Materials.Chlorine.getGas(10000), Materials.HypochlorousAcid.getFluid(10000), Materials.Empty.getCells(11), GT_Values.NI, 600, 6);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Chlorine.getCells(1), Materials.Water.getCells(1), Materials.Mercury.getFluid(100), Materials.HypochlorousAcid.getFluid(1000), Materials.Empty.getCells(2), GT_Values.NI, 60, 6);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(2)}, new FluidStack[]{Materials.Chlorine.getGas(10000), Materials.Water.getFluid(10000), Materials.Mercury.getFluid(1000)}, new FluidStack[]{Materials.HypochlorousAcid.getFluid(10000)}, null, 600, 6);

        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Water.getFluid(1000), Materials.HypochlorousAcid.getFluid(1000), Materials.DilutedHydrochloricAcid.getCells(1), Materials.Empty.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorine.getGas(2000), Materials.HypochlorousAcid.getFluid(1000), Materials.DilutedHydrochloricAcid.getCells(1), GT_Values.NI, 120);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.Water.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.HypochlorousAcid.getCells(1), Materials.Empty.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorine.getGas(2000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.HypochlorousAcid.getCells(1), GT_Values.NI, 120);

        RA.addChemicalRecipe(Materials.HypochlorousAcid.getCells(1), Materials.SodiumHydroxide.getDust(1), Materials.AllylChloride.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000), Materials.SaltWater.getCells(1), 480);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.AllylChloride.getCells(1), Materials.HypochlorousAcid.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000), Materials.SaltWater.getCells(1), 480);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.HydrochloricAcid.getCells(1), Materials.Glycerol.getCells(1), GT_Values.NF, Materials.Epichlorohydrin.getFluid(1000), Materials.Water.getCells(2), GT_Values.NI, 480, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.SodiumHydroxide.getDust(1), GT_Utility.getIntegratedCircuit(23)}, new FluidStack[]{Materials.Propene.getGas(1000), Materials.Chlorine.getGas(4000), Materials.Water.getFluid(1000)}, new FluidStack[]{Materials.Epichlorohydrin.getFluid(1000), Materials.SaltWater.getFluid(1000), Materials.HydrochloricAcid.getFluid(2000)}, null, 640, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.SodiumHydroxide.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Propene.getGas(1000), Materials.Chlorine.getGas(3000), Materials.Water.getFluid(1000), Materials.Mercury.getFluid(100)}, new FluidStack[]{Materials.Epichlorohydrin.getFluid(1000), Materials.SaltWater.getFluid(1000), Materials.HydrochloricAcid.getFluid(1000)}, null, 640, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.SodiumHydroxide.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Propene.getGas(1000), Materials.Chlorine.getGas(2000), Materials.HypochlorousAcid.getFluid(1000)}, new FluidStack[]{Materials.Epichlorohydrin.getFluid(1000), Materials.SaltWater.getFluid(1000), Materials.HydrochloricAcid.getFluid(1000)}, null, 640, 30);

        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), Materials.Empty.getCells(1), Materials.Glycerol.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000), Materials.Water.getCells(2), 480);
        RA.addChemicalRecipe(Materials.Glycerol.getCells(1), Materials.Empty.getCells(1), Materials.HydrochloricAcid.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000), Materials.Water.getCells(2), 480);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Glycerol.getFluid(1000), Materials.Water.getFluid(2000), Materials.Epichlorohydrin.getCells(1), 480);
        RA.addChemicalRecipe(Materials.Glycerol.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.HydrochloricAcid.getFluid(1000), Materials.Water.getFluid(2000), Materials.Epichlorohydrin.getCells(1), 480);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Glycerol.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000), Materials.Empty.getCells(1), 480);
        RA.addChemicalRecipe(Materials.Glycerol.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.HydrochloricAcid.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000), Materials.Empty.getCells(1), 480);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Glycerol.getFluid(1000), GT_Values.NF, Materials.Epichlorohydrin.getCells(1), 480);
        RA.addChemicalRecipe(Materials.Glycerol.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.HydrochloricAcid.getFluid(1000), GT_Values.NF, Materials.Epichlorohydrin.getCells(1), 480);

        RA.addDistilleryRecipe(2, Materials.HeavyFuel.getFluid(100), Materials.Benzene.getFluid(40), 160, 24, false);
        RA.addDistilleryRecipe(3, Materials.HeavyFuel.getFluid(100), Materials.Phenol.getFluid(25), 160, 24, false);

        RA.addChemicalRecipe(Materials.Apatite.getDust(1), Materials.SulfuricAcid.getCells(5), Materials.Water.getFluid(10000), Materials.PhosphoricAcid.getFluid(3000), Materials.HydrochloricAcid.getCells(1), Materials.Empty.getCells(4), 320);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Phosphor.getDust(4), GT_Values.NI, Materials.Oxygen.getGas(10000), GT_Values.NF, Materials.PhosphorousPentoxide.getDust(1), GT_Values.NI, 40, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Phosphor.getDust(4), GT_Utility.getIntegratedCircuit(1)}, new FluidStack[]{Materials.Oxygen.getGas(10000)}, null, new ItemStack[]{Materials.PhosphorousPentoxide.getDust(1)}, 40, 30);
        RA.addChemicalRecipe(Materials.PhosphorousPentoxide.getDust(1), GT_Values.NI, Materials.Water.getFluid(6000), Materials.PhosphoricAcid.getFluid(4000), GT_Values.NI, 40);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Phosphor.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Oxygen.getGas(2500), Materials.Water.getFluid(1500)}, new FluidStack[]{Materials.PhosphoricAcid.getFluid(1000)}, null, 320, 30);

        RA.addChemicalRecipeForBasicMachineOnly(Materials.Propene.getCells(8), Materials.PhosphoricAcid.getCells(1), Materials.Benzene.getFluid(8000), Materials.Cumene.getFluid(8000), Materials.Empty.getCells(9), GT_Values.NI, 1920, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.PhosphoricAcid.getCells(1), Materials.Benzene.getCells(8), Materials.Propene.getGas(8000), Materials.Cumene.getFluid(8000), Materials.Empty.getCells(9), GT_Values.NI, 1920, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Benzene.getCells(1), Materials.Propene.getCells(1), Materials.PhosphoricAcid.getFluid(125), Materials.Cumene.getFluid(1000), Materials.Empty.getCells(2), GT_Values.NI, 240, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(1)}, new FluidStack[]{Materials.Propene.getGas(8000), Materials.Benzene.getFluid(8000), Materials.PhosphoricAcid.getFluid(1000)}, new FluidStack[]{Materials.Cumene.getFluid(8000)}, null, 1920, 30);

        RA.addChemicalRecipe(Materials.Cumene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.Acetone.getFluid(1000), Materials.Phenol.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Cumene.getFluid(1000), Materials.Acetone.getFluid(1000), Materials.Phenol.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Cumene.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Oxygen.getGas(1000), Materials.Phenol.getFluid(1000), Materials.Acetone.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Cumene.getFluid(1000), Materials.Phenol.getFluid(1000), Materials.Acetone.getCells(1), 275);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Propene.getGas(1000), Materials.Benzene.getFluid(1000), Materials.PhosphoricAcid.getFluid(100), Materials.Oxygen.getGas(1000)}, new FluidStack[]{Materials.Phenol.getFluid(1000), Materials.Acetone.getFluid(1000)}, null, 480, 30);

        RA.addChemicalRecipeForBasicMachineOnly(Materials.Acetone.getCells(1), Materials.Phenol.getCells(2), Materials.HydrochloricAcid.getFluid(1000), Materials.BisphenolA.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(2), 160, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.HydrochloricAcid.getCells(1), Materials.Acetone.getCells(1), Materials.Phenol.getFluid(2000), Materials.BisphenolA.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 160, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Phenol.getCells(2), Materials.HydrochloricAcid.getCells(1), Materials.Acetone.getFluid(1000), Materials.BisphenolA.getFluid(1000), Materials.Water.getCells(1), Materials.Empty.getCells(2), 160, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(1)}, new FluidStack[]{Materials.Acetone.getFluid(1000), Materials.Phenol.getFluid(2000), Materials.HydrochloricAcid.getFluid(1000)}, new FluidStack[]{Materials.BisphenolA.getFluid(1000)}, null, 160, 30);

        RA.addChemicalRecipe(Materials.BisphenolA.getCells(1), Materials.SodiumHydroxide.getDust(1), Materials.Epichlorohydrin.getFluid(1000), Materials.Epoxid.getMolten(1000), Materials.SaltWater.getCells(1), 275);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.Epichlorohydrin.getCells(1), Materials.BisphenolA.getFluid(1000), Materials.Epoxid.getMolten(1000), Materials.SaltWater.getCells(1), 275);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.SodiumHydroxide.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Acetone.getFluid(1000), Materials.Phenol.getFluid(2000), Materials.HydrochloricAcid.getFluid(1000), Materials.Epichlorohydrin.getFluid(1000)}, new FluidStack[]{Materials.Epoxid.getMolten(1000), Materials.SaltWater.getFluid(1000)}, null, 480, 30);

        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.HydrochloricAcid.getFluid(1000), Materials.Chloromethane.getGas(1000), Materials.Water.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Methanol.getFluid(1000), Materials.Chloromethane.getGas(1000), Materials.Water.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.HydrochloricAcid.getFluid(1000), Materials.Water.getFluid(1000), Materials.Chloromethane.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Methanol.getFluid(1000), Materials.Water.getFluid(1000), Materials.Chloromethane.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.HydrochloricAcid.getFluid(1000), Materials.Chloromethane.getGas(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Methanol.getFluid(1000), Materials.Chloromethane.getGas(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Methanol.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.HydrochloricAcid.getFluid(1000), GT_Values.NF, Materials.Chloromethane.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Methanol.getFluid(1000), GT_Values.NF, Materials.Chloromethane.getCells(1), 275);

        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Methane.getGas(1000), Materials.Chloromethane.getGas(1000), Materials.HydrochloricAcid.getCells(1), Materials.Empty.getCells(1), 75);
        RA.addChemicalRecipe(Materials.Methane.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorine.getGas(2000), Materials.Chloromethane.getGas(1000), Materials.HydrochloricAcid.getCells(1), 75);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.Methane.getGas(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.Chloromethane.getCells(1), Materials.Empty.getCells(1), 75);
        RA.addChemicalRecipe(Materials.Methane.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorine.getGas(2000), Materials.HydrochloricAcid.getFluid(1000), Materials.Chloromethane.getCells(1), 75);

        RA.addChemicalRecipe(Materials.Chlorine.getCells(6), GT_Utility.getIntegratedCircuit(3), Materials.Methane.getGas(1000), Materials.Chloroform.getFluid(1000), Materials.HydrochloricAcid.getCells(3), Materials.Empty.getCells(3), 75);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Methane.getCells(1), Materials.Empty.getCells(2), Materials.Chlorine.getGas(6000), Materials.Chloroform.getFluid(1000), Materials.HydrochloricAcid.getCells(3), GT_Values.NI, 80, 30);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(6), GT_Utility.getIntegratedCircuit(13), Materials.Methane.getGas(1000), Materials.HydrochloricAcid.getFluid(3000), Materials.Chloroform.getCells(1), Materials.Empty.getCells(5), 75);
        RA.addChemicalRecipe(Materials.Methane.getCells(1), GT_Utility.getIntegratedCircuit(13), Materials.Chlorine.getGas(6000), Materials.HydrochloricAcid.getFluid(3000), Materials.Chloroform.getCells(1), 75);

        RA.addChemicalRecipe(Materials.Fluorine.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(1000), Materials.HydrofluoricAcid.getFluid(1000), Materials.Empty.getCells(1), 60, 6);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Fluorine.getGas(1000), Materials.HydrofluoricAcid.getFluid(1000), Materials.Empty.getCells(1), 60, 6);
        RA.addChemicalRecipe(Materials.Fluorine.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Hydrogen.getGas(1000), GT_Values.NF, Materials.HydrofluoricAcid.getCells(1), 60, 6);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Fluorine.getGas(1000), GT_Values.NF, Materials.HydrofluoricAcid.getCells(1), 60, 6);

        RA.addChemicalRecipe(Materials.Chloroform.getCells(2), Materials.HydrofluoricAcid.getCells(4), GT_Values.NF, Materials.Tetrafluoroethylene.getGas(1000), Materials.DilutedHydrochloricAcid.getCells(6), 480, 280);
        RA.addChemicalRecipe(Materials.Chloroform.getCells(2), Materials.Empty.getCells(4), Materials.HydrofluoricAcid.getFluid(4000), Materials.Tetrafluoroethylene.getGas(1000), Materials.DilutedHydrochloricAcid.getCells(6), 480, 280);
        RA.addChemicalRecipe(Materials.HydrofluoricAcid.getCells(4), Materials.Empty.getCells(2), Materials.Chloroform.getFluid(2000), Materials.Tetrafluoroethylene.getGas(1000), Materials.DilutedHydrochloricAcid.getCells(6), 480, 280);
        RA.addChemicalRecipe(Materials.HydrofluoricAcid.getCells(4), GT_Utility.getIntegratedCircuit(11), Materials.Chloroform.getFluid(2000), Materials.DilutedHydrochloricAcid.getFluid(6000), Materials.Tetrafluoroethylene.getCells(1), Materials.Empty.getCells(3), 480, 280);
        RA.addChemicalRecipe(Materials.Chloroform.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.HydrofluoricAcid.getFluid(4000), Materials.DilutedHydrochloricAcid.getFluid(6000), Materials.Tetrafluoroethylene.getCells(1), Materials.Empty.getCells(1), 480, 280);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.HydrofluoricAcid.getFluid(4000), Materials.Methane.getGas(2000), Materials.Chlorine.getGas(12000)}, new FluidStack[]{Materials.Tetrafluoroethylene.getGas(1000), Materials.HydrochloricAcid.getFluid(6000), Materials.DilutedHydrochloricAcid.getFluid(6000)}, null, 540, 280);

        RA.addDefaultPolymerizationRecipes(Materials.Tetrafluoroethylene.mGas, Materials.Tetrafluoroethylene.getCells(1), Materials.Polytetrafluoroethylene.mStandardMoltenFluid);

        RA.addChemicalRecipe(Materials.Silicon.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Chloromethane.getGas(2000), Materials.Dimethyldichlorosilane.getFluid(1000), GT_Values.NI, 240, 96);
        //This recipe is redundant:
        //RA.addChemicalRecipe(                   Materials.Silicon.getDust(1), GT_Utility.getIntegratedCircuit(11), Materials.Chloromethane.getGas(2000), GT_Values.NF, Materials.Dimethyldichlorosilane.getCells(1), 240, 96);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Silicon.getDust(1), Materials.Chloromethane.getCells(2), GT_Values.NF, Materials.Dimethyldichlorosilane.getFluid(1000), Materials.Empty.getCells(2), GT_Values.NI, 240, 96);

        RA.addChemicalRecipe(Materials.Dimethyldichlorosilane.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Water.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Polydimethylsiloxane.getDust(3), Materials.Empty.getCells(1), 240, 96);
        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Dimethyldichlorosilane.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Polydimethylsiloxane.getDust(3), Materials.Empty.getCells(1), 240, 96);
        RA.addChemicalRecipe(Materials.Dimethyldichlorosilane.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Water.getFluid(1000), GT_Values.NF, Materials.Polydimethylsiloxane.getDust(3), Materials.DilutedHydrochloricAcid.getCells(1), 240, 96);
        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Dimethyldichlorosilane.getFluid(1000), GT_Values.NF, Materials.Polydimethylsiloxane.getDust(3), Materials.DilutedHydrochloricAcid.getCells(1), 240, 96);
        RA.addChemicalRecipe(Materials.Dimethyldichlorosilane.getCells(1), Materials.Water.getCells(1), GT_Values.NF, Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Polydimethylsiloxane.getDust(3), Materials.Empty.getCells(2), 240, 96);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Silicon.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Methane.getGas(2000), Materials.Chlorine.getGas(4000), Materials.Water.getFluid(1000)}, new FluidStack[]{Materials.HydrochloricAcid.getFluid(2000), Materials.DilutedHydrochloricAcid.getFluid(2000)}, new ItemStack[]{Materials.Polydimethylsiloxane.getDust(3)}, 480, 96);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Silicon.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Methanol.getFluid(2000), Materials.HydrochloricAcid.getFluid(2000)}, new FluidStack[]{Materials.DilutedHydrochloricAcid.getFluid(2000)}, new ItemStack[]{Materials.Polydimethylsiloxane.getDust(3)}, 480, 96);

        RA.addChemicalRecipe(Materials.Polydimethylsiloxane.getDust(9), Materials.Sulfur.getDust(1), GT_Values.NF, Materials.Silicone.getMolten(1296), GT_Values.NI, 1200);

        RA.addChemicalRecipe(Materials.Nitrogen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(3000), Materials.Ammonia.getGas(1000), Materials.Empty.getCells(1), 320, 384);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(3), GT_Utility.getIntegratedCircuit(1), Materials.Nitrogen.getGas(1000), Materials.Ammonia.getGas(1000), Materials.Empty.getCells(3), 320, 384);
        RA.addChemicalRecipe(Materials.Nitrogen.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Hydrogen.getGas(3000), GT_Values.NF, Materials.Ammonia.getCells(1), 320, 384);
        RA.addChemicalRecipe(Materials.Hydrogen.getCells(3), GT_Utility.getIntegratedCircuit(11), Materials.Nitrogen.getGas(1000), GT_Values.NF, Materials.Ammonia.getCells(1), Materials.Empty.getCells(2), 320, 384);

        RA.addChemicalRecipe(Materials.Methanol.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Ammonia.getGas(1000), Materials.Dimethylamine.getGas(1000), Materials.Water.getCells(2), 240, 120);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Ammonia.getCells(1), Materials.Empty.getCells(1), Materials.Methanol.getFluid(2000), Materials.Dimethylamine.getGas(1000), Materials.Water.getCells(2), GT_Values.NI, 240, 120);
        RA.addChemicalRecipe(Materials.Methanol.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.Ammonia.getGas(1000), Materials.Water.getFluid(1000), Materials.Dimethylamine.getCells(1), Materials.Empty.getCells(1), 240, 120);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Methanol.getFluid(2000), Materials.Water.getFluid(1000), Materials.Dimethylamine.getCells(1), 240, 120);
        RA.addChemicalRecipe(Materials.Methanol.getCells(2), GT_Utility.getIntegratedCircuit(2), Materials.Ammonia.getGas(1000), Materials.Dimethylamine.getGas(1000), Materials.Empty.getCells(2), 240, 120);
        RA.addChemicalRecipe(Materials.Methanol.getCells(2), GT_Utility.getIntegratedCircuit(12), Materials.Ammonia.getGas(1000), GT_Values.NF, Materials.Dimethylamine.getCells(1), Materials.Empty.getCells(1), 240, 120);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Methanol.getFluid(2000), GT_Values.NF, Materials.Dimethylamine.getCells(1), 240, 120);

        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.HypochlorousAcid.getFluid(1000), Materials.Chloramine.getFluid(1000), Materials.Water.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HypochlorousAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Ammonia.getGas(1000), Materials.Chloramine.getFluid(1000), Materials.Water.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.HypochlorousAcid.getFluid(1000), Materials.Water.getFluid(1000), Materials.Chloramine.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HypochlorousAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Ammonia.getGas(1000), Materials.Water.getFluid(1000), Materials.Chloramine.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.HypochlorousAcid.getFluid(1000), Materials.Chloramine.getFluid(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HypochlorousAcid.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Ammonia.getGas(1000), Materials.Chloramine.getFluid(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.HypochlorousAcid.getFluid(1000), GT_Values.NF, Materials.Chloramine.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HypochlorousAcid.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Ammonia.getGas(1000), GT_Values.NF, Materials.Chloramine.getCells(1), 275);

        RA.addChemicalRecipe(Materials.Chloramine.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Dimethylamine.getGas(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Dimethylhydrazine.getCells(1), 960, 480);
        RA.addChemicalRecipe(Materials.Dimethylamine.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chloramine.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Dimethylhydrazine.getCells(1), 960, 480);
        RA.addChemicalRecipe(Materials.Chloramine.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Dimethylamine.getGas(1000), Materials.Dimethylhydrazine.getFluid(1000), Materials.DilutedHydrochloricAcid.getCells(1), 960, 480);
        RA.addChemicalRecipe(Materials.Dimethylamine.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chloramine.getFluid(1000), Materials.Dimethylhydrazine.getFluid(1000), Materials.DilutedHydrochloricAcid.getCells(1), 960, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.HypochlorousAcid.getFluid(1000), Materials.Ammonia.getGas(1000), Materials.Methanol.getFluid(2000)}, new FluidStack[]{Materials.Dimethylhydrazine.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Water.getFluid(3000)}, null, 1040, 480);

        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(2), GT_Values.NI, Materials.NitrogenDioxide.getGas(2000), Materials.DinitrogenTetroxide.getGas(1000), GT_Values.NI, 640);
        RA.addChemicalRecipe(Materials.NitrogenDioxide.getCells(2), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, Materials.DinitrogenTetroxide.getGas(1000), Materials.Empty.getCells(2), 640);
        RA.addChemicalRecipe(Materials.NitrogenDioxide.getCells(2), GT_Utility.getIntegratedCircuit(12), GT_Values.NF, GT_Values.NF, Materials.DinitrogenTetroxide.getCells(1), Materials.Empty.getCells(1), 640);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(23)}, new FluidStack[]{Materials.Ammonia.getGas(2000), Materials.Oxygen.getGas(7000)}, new FluidStack[]{Materials.DinitrogenTetroxide.getGas(1000), Materials.Water.getFluid(3000)}, null, 480, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(23)}, new FluidStack[]{Materials.Nitrogen.getGas(2000), Materials.Hydrogen.getGas(6000), Materials.Oxygen.getGas(7000)}, new FluidStack[]{Materials.DinitrogenTetroxide.getGas(1000), Materials.Water.getFluid(3000)}, null, 1100, 480);

        RA.addMixerRecipe(Materials.Dimethylhydrazine.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.DinitrogenTetroxide.getGas(1000), new FluidStack(ItemList.sRocketFuel, 6000), Materials.Empty.getCells(1), 60, 18);
        RA.addMixerRecipe(Materials.DinitrogenTetroxide.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Dimethylhydrazine.getFluid(1000), new FluidStack(ItemList.sRocketFuel, 6000), Materials.Empty.getCells(1), 60, 18);
        RA.addMixerRecipe(Materials.Dimethylhydrazine.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Oxygen.getGas(1000), new FluidStack(ItemList.sRocketFuel, 3000), Materials.Empty.getCells(1), 60, 18);
        RA.addMixerRecipe(Materials.Oxygen.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Dimethylhydrazine.getFluid(1000), new FluidStack(ItemList.sRocketFuel, 3000), Materials.Empty.getCells(1), 60, 18);

        RA.addChemicalRecipe(Materials.Ammonia.getCells(4), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(10000), Materials.Water.getFluid(6000), Materials.NitricOxide.getCells(4), 320);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(10), GT_Utility.getIntegratedCircuit(1), Materials.Ammonia.getGas(4000), Materials.Water.getFluid(6000), Materials.NitricOxide.getCells(4), Materials.Empty.getCells(6), 320);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Ammonia.getCells(4), Materials.Empty.getCells(2), Materials.Oxygen.getGas(10000), Materials.NitricOxide.getGas(4000), Materials.Water.getCells(6), GT_Values.NI, 320, 30);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(10), GT_Utility.getIntegratedCircuit(11), Materials.Ammonia.getGas(4000), Materials.NitricOxide.getGas(4000), Materials.Water.getCells(6), Materials.Empty.getCells(4), 320);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(4), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(10000), GT_Values.NF, Materials.NitricOxide.getCells(4), 320);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(10), GT_Utility.getIntegratedCircuit(2), Materials.Ammonia.getGas(4000), GT_Values.NF, Materials.NitricOxide.getCells(4), Materials.Empty.getCells(6), 320);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(10), GT_Utility.getIntegratedCircuit(12), Materials.Ammonia.getGas(4000), Materials.NitricOxide.getGas(4000), Materials.Empty.getCells(10), 320);


        RA.addChemicalRecipe(Materials.NitricOxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.NitrogenDioxide.getGas(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.NitricOxide.getGas(1000), Materials.NitrogenDioxide.getGas(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.NitricOxide.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Oxygen.getGas(1000), GT_Values.NF, Materials.NitrogenDioxide.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.NitricOxide.getGas(1000), GT_Values.NF, Materials.NitrogenDioxide.getCells(1), 275);

        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.NitrogenDioxide.getGas(3000), Materials.NitricAcid.getFluid(2000), Materials.NitricOxide.getCells(1), 280);
        RA.addChemicalRecipe(Materials.NitrogenDioxide.getCells(3), GT_Utility.getIntegratedCircuit(1), Materials.Water.getFluid(1000), Materials.NitricAcid.getFluid(2000), Materials.NitricOxide.getCells(1), Materials.Empty.getCells(2), 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Water.getCells(1), Materials.Empty.getCells(1), Materials.NitrogenDioxide.getGas(3000), Materials.NitricOxide.getGas(1000), Materials.NitricAcid.getCells(2), GT_Values.NI, 240, 30);
        RA.addChemicalRecipe(Materials.NitrogenDioxide.getCells(3), GT_Utility.getIntegratedCircuit(11), Materials.Water.getFluid(1000), Materials.NitricOxide.getGas(1000), Materials.NitricAcid.getCells(2), Materials.Empty.getCells(1), 280);

        RA.addChemicalRecipeForBasicMachineOnly(Materials.NitrogenDioxide.getCells(2), Materials.Oxygen.getCells(1), Materials.Water.getFluid(1000), Materials.NitricAcid.getFluid(2000), Materials.Empty.getCells(3), GT_Values.NI, 240, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Oxygen.getCells(1), Materials.Water.getCells(1), Materials.NitrogenDioxide.getGas(2000), Materials.NitricAcid.getFluid(2000), Materials.Empty.getCells(2), GT_Values.NI, 240, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Water.getCells(1), Materials.NitrogenDioxide.getCells(2), Materials.Oxygen.getGas(1000), Materials.NitricAcid.getFluid(2000), Materials.Empty.getCells(3), GT_Values.NI, 240, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Hydrogen.getGas(3000), Materials.Nitrogen.getGas(1000), Materials.Oxygen.getGas(4000)}, new FluidStack[]{Materials.NitricAcid.getFluid(1000), Materials.Water.getFluid(1000)}, null, 320, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Ammonia.getGas(1000), Materials.Oxygen.getGas(4000)}, new FluidStack[]{Materials.NitricAcid.getFluid(1000), Materials.Water.getFluid(1000)}, null, 320, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.NitrogenDioxide.getGas(2000), Materials.Oxygen.getGas(1000), Materials.Water.getFluid(1000)}, new FluidStack[]{Materials.NitricAcid.getFluid(2000)}, null, 320, 30);

        RA.addChemicalRecipe(Materials.Sulfur.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Hydrogen.getGas(2000), Materials.HydricSulfide.getGas(1000), GT_Values.NI, 60, 6);
        RA.addChemicalRecipe(Materials.Sulfur.getDust(1), Materials.Empty.getCells(1), Materials.Hydrogen.getGas(2000), GT_Values.NF, Materials.HydricSulfide.getCells(1), 60, 6);

        RA.addChemicalRecipe(Materials.Sulfur.getDust(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(2000), Materials.SulfurDioxide.getGas(1000), GT_Values.NI, 60, 6);

        RA.addChemicalRecipe(Materials.HydricSulfide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1000), Materials.Water.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(3), GT_Utility.getIntegratedCircuit(1), Materials.HydricSulfide.getGas(1000), Materials.SulfurDioxide.getGas(1000), Materials.Water.getCells(1), Materials.Empty.getCells(2), 120);
        RA.addChemicalRecipe(Materials.HydricSulfide.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Oxygen.getGas(3000), Materials.Water.getFluid(1000), Materials.SulfurDioxide.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(3), GT_Utility.getIntegratedCircuit(11), Materials.HydricSulfide.getGas(1000), Materials.Water.getFluid(1000), Materials.SulfurDioxide.getCells(1), Materials.Empty.getCells(2), 120);
        RA.addChemicalRecipe(Materials.HydricSulfide.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1000), Materials.Empty.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(3), GT_Utility.getIntegratedCircuit(2), Materials.HydricSulfide.getGas(1000), Materials.SulfurDioxide.getGas(1000), Materials.Empty.getCells(3), 120);
        RA.addChemicalRecipe(Materials.HydricSulfide.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Oxygen.getGas(3000), GT_Values.NF, Materials.SulfurDioxide.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(3), GT_Utility.getIntegratedCircuit(12), Materials.HydricSulfide.getGas(1000), GT_Values.NF, Materials.SulfurDioxide.getCells(1), Materials.Empty.getCells(2), 120);

        RA.addChemicalRecipe(Materials.SulfurDioxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.HydricSulfide.getGas(2000), Materials.Water.getFluid(2000), Materials.Sulfur.getDust(3), Materials.Empty.getCells(1), 120);
        RA.addChemicalRecipe(Materials.HydricSulfide.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.SulfurDioxide.getGas(1000), Materials.Water.getFluid(2000), Materials.Sulfur.getDust(3), Materials.Empty.getCells(2), 120);
        RA.addChemicalRecipe(Materials.SulfurDioxide.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.HydricSulfide.getGas(2000), GT_Values.NF, Materials.Sulfur.getDust(3), Materials.Empty.getCells(1), 120);
        RA.addChemicalRecipe(Materials.HydricSulfide.getCells(2), GT_Utility.getIntegratedCircuit(2), Materials.SulfurDioxide.getGas(1000), GT_Values.NF, Materials.Sulfur.getDust(3), Materials.Empty.getCells(2), 120);

        //RA.addChemicalRecipe(Materials.Sulfur.getDust(1),         GT_Utility.getIntegratedCircuit(3),  Materials.Oxygen.getGas(3000), Materials.SulfurTrioxide.getGas(1000), GT_Values.NI, 280);
        //RA.addChemicalRecipe(Materials.Sulfur.getDust(1),         Materials.Empty.getCells(1),         Materials.Oxygen.getGas(3000), GT_Values.NF,                          Materials.SulfurTrioxide.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.SulfurDioxide.getGas(1000), Materials.SulfurTrioxide.getGas(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.SulfurDioxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.SulfurTrioxide.getGas(1000), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.SulfurDioxide.getGas(1000), GT_Values.NF, Materials.SulfurTrioxide.getCells(1), 275);
        RA.addChemicalRecipe(Materials.SulfurDioxide.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Oxygen.getGas(1000), GT_Values.NF, Materials.SulfurTrioxide.getCells(1), 275);

        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.SulfurTrioxide.getGas(1000), Materials.SulfuricAcid.getFluid(1000), Materials.Empty.getCells(1), 320, 6);
        RA.addChemicalRecipe(Materials.SulfurTrioxide.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Water.getFluid(1000), Materials.SulfuricAcid.getFluid(1000), Materials.Empty.getCells(1), 320, 6);
        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.SulfurTrioxide.getGas(1000), GT_Values.NF, Materials.SulfuricAcid.getCells(1), 320, 6);
        RA.addChemicalRecipe(Materials.SulfurTrioxide.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Water.getFluid(1000), GT_Values.NF, Materials.SulfuricAcid.getCells(1), 320, 6);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Sulfur.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Oxygen.getGas(3000), Materials.Water.getFluid(1000)}, new FluidStack[]{Materials.SulfuricAcid.getFluid(1000)}, null, 480, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.HydricSulfide.getGas(1000), Materials.Oxygen.getGas(3000)}, new FluidStack[]{Materials.SulfuricAcid.getFluid(1000)}, null, 480, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.SulfurDioxide.getGas(1000), Materials.Oxygen.getGas(1000), Materials.Water.getFluid(1000)}, new FluidStack[]{Materials.SulfuricAcid.getFluid(1000)}, null, 480, 30);

        RA.addUniversalDistillationRecipe(Materials.DilutedSulfuricAcid.getFluid(3000), new FluidStack[]{Materials.SulfuricAcid.getFluid(2000), Materials.Water.getFluid(1000)}, GT_Values.NI, 600, 120);

        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Ethylene.getGas(1000), Materials.VinylChloride.getGas(1000), Materials.HydrochloricAcid.getCells(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorine.getGas(2000), Materials.VinylChloride.getGas(1000), Materials.HydrochloricAcid.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.Ethylene.getGas(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.VinylChloride.getCells(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorine.getGas(2000), Materials.HydrochloricAcid.getFluid(1000), Materials.VinylChloride.getCells(1), 275);

        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), Materials.HydrochloricAcid.getCells(1), Materials.Oxygen.getGas(1000), Materials.VinylChloride.getGas(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.HydrochloricAcid.getCells(1), Materials.Oxygen.getCells(1), Materials.Ethylene.getGas(1000), Materials.VinylChloride.getGas(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), Materials.Ethylene.getCells(1), Materials.HydrochloricAcid.getFluid(1000), Materials.VinylChloride.getGas(1000), Materials.Water.getCells(1), Materials.Empty.getCells(1), 275);

        RA.addDefaultPolymerizationRecipes(Materials.VinylChloride.mGas, Materials.VinylChloride.getCells(1), Materials.PolyvinylChloride.mStandardMoltenFluid);

        RA.addUniversalDistillationRecipe(Materials.Acetone.getFluid(1000), new FluidStack[]{Materials.Ethenone.getGas(1000), Materials.Methane.getGas(1000)}, GT_Values.NI, 80, 640);
        RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), Materials.Acetone.getFluid(1000), Materials.Ethenone.getGas(1000), 160, 275);
        //GameRegistry.addSmelting(Materials.Acetone.getCells(1), Materials.Ethenone.getCells(1), 0);
        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.SulfuricAcid.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Ethenone.getCells(1), 160, 120);
        RA.addChemicalRecipe(Materials.SulfuricAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.AceticAcid.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Ethenone.getCells(1), 160, 120);
        RA.addChemicalRecipe(Materials.AceticAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.SulfuricAcid.getFluid(1000), Materials.Ethenone.getGas(1000), Materials.DilutedSulfuricAcid.getCells(1), 160, 120);
        RA.addChemicalRecipe(Materials.SulfuricAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.AceticAcid.getFluid(1000), Materials.Ethenone.getGas(1000), Materials.DilutedSulfuricAcid.getCells(1), 160, 120);

        RA.addChemicalRecipe(Materials.Ethenone.getCells(1), Materials.Empty.getCells(1), Materials.NitricAcid.getFluid(8000), Materials.Water.getFluid(9000), Materials.Tetranitromethane.getCells(2), 480, 120);
        RA.addChemicalRecipe(Materials.Ethenone.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.NitricAcid.getFluid(8000), Materials.Tetranitromethane.getFluid(2000), Materials.Empty.getCells(1), 480, 120);
        RA.addChemicalRecipe(Materials.NitricAcid.getCells(8), GT_Utility.getIntegratedCircuit(1), Materials.Ethenone.getGas(1000), Materials.Water.getFluid(9000), Materials.Tetranitromethane.getCells(2), Materials.Empty.getCells(6), 480, 120);
        RA.addChemicalRecipe(Materials.NitricAcid.getCells(8), GT_Utility.getIntegratedCircuit(2), Materials.Ethenone.getGas(1000), GT_Values.NF, Materials.Tetranitromethane.getCells(2), Materials.Empty.getCells(6), 480, 120);
        RA.addChemicalRecipe(Materials.NitricAcid.getCells(8), GT_Utility.getIntegratedCircuit(12), Materials.Ethenone.getGas(1000), Materials.Tetranitromethane.getFluid(2000), Materials.Empty.getCells(8), 480, 120);
        RA.addChemicalRecipe(Materials.NitricAcid.getCells(8), Materials.Empty.getCells(1), Materials.Ethenone.getGas(1000), Materials.Tetranitromethane.getFluid(2000), Materials.Water.getCells(9), 480, 120);
        RA.addChemicalRecipe(Materials.Ethenone.getCells(1), Materials.NitricAcid.getCells(8), GT_Values.NF, Materials.Tetranitromethane.getFluid(2000), Materials.Water.getCells(9), 480, 120);

        //RA.addMixerRecipe(Materials.LightFuel.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Tetranitromethane.getFluid(20), Materials.NitroFuel.getFluid(1000), Materials.Empty.getCells(1), 80, 6);
        RA.addMixerRecipe(Materials.Fuel.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Tetranitromethane.getFluid(20), Materials.NitroFuel.getFluid(1000), Materials.Empty.getCells(1), 20, 480);
        RA.addMixerRecipe(Materials.BioDiesel.getCells(1), GT_Values.NI, GT_Values.NI, GT_Values.NI, Materials.Tetranitromethane.getFluid(40), Materials.NitroFuel.getFluid(900), Materials.Empty.getCells(1), 20, 480);

        RA.addChemicalRecipe(Materials.Propene.getCells(1), Materials.Empty.getCells(1), Materials.Ethylene.getGas(1000), Materials.Isoprene.getFluid(1000), Materials.Hydrogen.getCells(2), 120);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), Materials.Empty.getCells(1), Materials.Propene.getGas(1000), Materials.Isoprene.getFluid(1000), Materials.Hydrogen.getCells(2), 120);
        RA.addChemicalRecipe(Materials.Propene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Ethylene.getGas(1000), Materials.Hydrogen.getGas(2000), Materials.Isoprene.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Propene.getGas(1000), Materials.Hydrogen.getGas(2000), Materials.Isoprene.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Propene.getGas(2000), Materials.Isoprene.getFluid(1000), Materials.Methane.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Propene.getCells(2), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, Materials.Isoprene.getFluid(1000), Materials.Methane.getCells(1), Materials.Empty.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Propene.getGas(2000), Materials.Methane.getGas(1000), Materials.Isoprene.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Propene.getCells(2), GT_Utility.getIntegratedCircuit(12), GT_Values.NF, Materials.Methane.getGas(1000), Materials.Isoprene.getCells(1), Materials.Empty.getCells(1), 120);

        RA.addChemicalRecipe(ItemList.Cell_Air.get(1), GT_Utility.getIntegratedCircuit(1), Materials.Isoprene.getFluid(144), GT_Values.NF, Materials.RawRubber.getDust(1), Materials.Empty.getCells(1), 275);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Isoprene.getFluid(288), GT_Values.NF, Materials.RawRubber.getDust(3), Materials.Empty.getCells(2), 320);
        RA.addChemicalRecipe(Materials.Isoprene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Air.getGas(14000), GT_Values.NF, Materials.RawRubber.getDust(7), Materials.Empty.getCells(1), 1120);
        RA.addChemicalRecipe(Materials.Isoprene.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(14000), GT_Values.NF, Materials.RawRubber.getDust(21), Materials.Empty.getCells(2), 2240);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(2)}, new FluidStack[]{Materials.Isoprene.getFluid(1728), Materials.Air.getGas(6000), Materials.Titaniumtetrachloride.getFluid(80)}, null, new ItemStack[]{Materials.RawRubber.getDust(18)}, 640, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(2)}, new FluidStack[]{Materials.Isoprene.getFluid(1728), Materials.Oxygen.getGas(6000), Materials.Titaniumtetrachloride.getFluid(80)}, null, new ItemStack[]{Materials.RawRubber.getDust(24)}, 640, 30);

        RA.addChemicalRecipe(Materials.Benzene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Ethylene.getGas(1000), Materials.Hydrogen.getGas(2000), Materials.Styrene.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Benzene.getFluid(1000), Materials.Hydrogen.getGas(2000), Materials.Styrene.getCells(1), 120);
        RA.addChemicalRecipe(Materials.Benzene.getCells(1), Materials.Empty.getCells(1), Materials.Ethylene.getGas(1000), Materials.Styrene.getFluid(1000), Materials.Hydrogen.getCells(2), 120);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), Materials.Empty.getCells(1), Materials.Benzene.getFluid(1000), Materials.Styrene.getFluid(1000), Materials.Hydrogen.getCells(2), 120);

        RA.addDefaultPolymerizationRecipes(Materials.Styrene.mFluid, Materials.Styrene.getCells(1), Materials.Polystyrene.mStandardMoltenFluid);

        RA.addChemicalRecipeForBasicMachineOnly(Materials.Butadiene.getCells(1), ItemList.Cell_Air.get(5), Materials.Styrene.getFluid(350), GT_Values.NF, Materials.RawStyreneButadieneRubber.getDust(9), Materials.Empty.getCells(6), 160, 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Butadiene.getCells(1), Materials.Oxygen.getCells(5), Materials.Styrene.getFluid(350), GT_Values.NF, Materials.RawStyreneButadieneRubber.getDust(13), Materials.Empty.getCells(6), 160, 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Styrene.getCells(1), ItemList.Cell_Air.get(15), Materials.Butadiene.getGas(3000), GT_Values.NF, Materials.RawStyreneButadieneRubber.getDust(27), Materials.Empty.getCells(16), 480, 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Styrene.getCells(1), Materials.Oxygen.getCells(15), Materials.Butadiene.getGas(3000), GT_Values.NF, Materials.RawStyreneButadieneRubber.getDust(41), Materials.Empty.getCells(16), 480, 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Styrene.getCells(1), Materials.Butadiene.getCells(3), Materials.Air.getGas(15000), GT_Values.NF, Materials.RawStyreneButadieneRubber.getDust(27), Materials.Empty.getCells(4), 480, 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Styrene.getCells(1), Materials.Butadiene.getCells(3), Materials.Oxygen.getGas(15000), GT_Values.NF, Materials.RawStyreneButadieneRubber.getDust(41), Materials.Empty.getCells(4), 480, 280);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(1)}, new FluidStack[]{Materials.Ethylene.getGas(1000), Materials.Benzene.getFluid(1000)}, new FluidStack[]{Materials.Styrene.getFluid(1000), Materials.Hydrogen.getGas(2000)}, null, 120, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(3)}, new FluidStack[]{Materials.Styrene.getFluid(36), Materials.Butadiene.getGas(108), Materials.Air.getGas(2000)}, null, new ItemStack[]{Materials.RawStyreneButadieneRubber.getDust(1)}, 160, 280);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(3)}, new FluidStack[]{Materials.Styrene.getFluid(72), Materials.Butadiene.getGas(216), Materials.Oxygen.getGas(2000)}, null, new ItemStack[]{Materials.RawStyreneButadieneRubber.getDust(3)}, 160, 280);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(4)}, new FluidStack[]{Materials.Styrene.getFluid(1080), Materials.Butadiene.getGas(3240), Materials.Titaniumtetrachloride.getFluid(200), Materials.Air.getGas(30000)}, null, new ItemStack[]{Materials.RawStyreneButadieneRubber.getDust(44), Materials.RawStyreneButadieneRubber.getDust(1)}, 1280, 280);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(4)}, new FluidStack[]{Materials.Styrene.getFluid(540), Materials.Butadiene.getGas(1620), Materials.Titaniumtetrachloride.getFluid(100), Materials.Oxygen.getGas(7500)}, null, new ItemStack[]{Materials.RawStyreneButadieneRubber.getDust(30)}, 640, 280);

        RA.addChemicalRecipe(Materials.RawStyreneButadieneRubber.getDust(9), Materials.Sulfur.getDust(1), GT_Values.NF, Materials.StyreneButadieneRubber.getMolten(1296), GT_Values.NI, 1200);

        RA.addChemicalRecipe(Materials.Benzene.getCells(1), GT_Utility.getIntegratedCircuit(2), Materials.Chlorine.getGas(4000), Materials.HydrochloricAcid.getFluid(2000), Materials.Dichlorobenzene.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(4), GT_Utility.getIntegratedCircuit(2), Materials.Benzene.getFluid(1000), Materials.HydrochloricAcid.getFluid(2000), Materials.Dichlorobenzene.getCells(1), Materials.Empty.getCells(3), 280);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Benzene.getCells(1), Materials.Empty.getCells(1), Materials.Chlorine.getGas(4000), Materials.Dichlorobenzene.getFluid(1000), Materials.HydrochloricAcid.getCells(2), GT_Values.NI, 240, 30);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(4), GT_Utility.getIntegratedCircuit(12), Materials.Benzene.getFluid(1000), Materials.Dichlorobenzene.getFluid(1000), Materials.HydrochloricAcid.getCells(2), Materials.Empty.getCells(2), 280);

        RA.addChemicalRecipe(Materials.SodiumSulfide.getDust(1), ItemList.Cell_Air.get(8), Materials.Dichlorobenzene.getFluid(1000), Materials.PolyphenyleneSulfide.getMolten(1000), Materials.Salt.getDust(2), Materials.Empty.getCells(8), 240, 375);
        RA.addChemicalRecipe(Materials.SodiumSulfide.getDust(1), Materials.Oxygen.getCells(8), Materials.Dichlorobenzene.getFluid(1000), Materials.PolyphenyleneSulfide.getMolten(1500), Materials.Salt.getDust(2), Materials.Empty.getCells(8), 240, 375);

        RA.addChemicalRecipe(Materials.Salt.getDust(2), GT_Utility.getIntegratedCircuit(1), Materials.SulfuricAcid.getFluid(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.SodiumBisulfate.getDust(1), 75);
        RA.addElectrolyzerRecipe(Materials.SodiumBisulfate.getDust(2), Materials.Empty.getCells(2), null, Materials.SodiumPersulfate.getFluid(1000), Materials.Hydrogen.getCells(2), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, null, 600, 30);

        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.Methanol.getCells(9), Materials.SeedOil.getFluid(54000), Materials.BioDiesel.getFluid(54000), Materials.Glycerol.getCells(9), 5400);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.SeedOil.getCells(54), Materials.Methanol.getFluid(9000), Materials.Glycerol.getFluid(9000), Materials.BioDiesel.getCells(54), 5400);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.Methanol.getCells(9), Materials.FishOil.getFluid(54000), Materials.BioDiesel.getFluid(54000), Materials.Glycerol.getCells(9), 5400);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.FishOil.getCells(54), Materials.Methanol.getFluid(9000), Materials.Glycerol.getFluid(9000), Materials.BioDiesel.getCells(54), 5400);

        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.Ethanol.getCells(9), Materials.SeedOil.getFluid(54000), Materials.BioDiesel.getFluid(54000), Materials.Glycerol.getCells(9), 5400);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.SeedOil.getCells(54), Materials.Ethanol.getFluid(9000), Materials.Glycerol.getFluid(9000), Materials.BioDiesel.getCells(54), 5400);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.Ethanol.getCells(9), Materials.FishOil.getFluid(54000), Materials.BioDiesel.getFluid(54000), Materials.Glycerol.getCells(9), 5400);
        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(1), Materials.FishOil.getCells(54), Materials.Ethanol.getFluid(9000), Materials.Glycerol.getFluid(9000), Materials.BioDiesel.getCells(54), 5400);

        RA.addChemicalRecipe(Materials.Glycerol.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.NitrationMixture.getFluid(3000), Materials.DilutedSulfuricAcid.getFluid(3000), Materials.Glyceryl.getCells(1), 180);
        RA.addChemicalRecipe(Materials.NitrationMixture.getCells(3), GT_Utility.getIntegratedCircuit(1), Materials.Glycerol.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(3000), Materials.Glyceryl.getCells(1), Materials.Empty.getCells(2), 180);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Glycerol.getCells(1), Materials.Empty.getCells(2), Materials.NitrationMixture.getFluid(3000), Materials.Glyceryl.getFluid(1000), Materials.DilutedSulfuricAcid.getCells(3), GT_Values.NI, 180, 30);
        RA.addChemicalRecipe(Materials.NitrationMixture.getCells(3), GT_Utility.getIntegratedCircuit(11), Materials.Glycerol.getFluid(1000), Materials.Glyceryl.getFluid(1000), Materials.DilutedSulfuricAcid.getCells(3), 180);

        RA.addChemicalRecipe(Materials.Quicklime.getDust(1), GT_Values.NI, Materials.CarbonDioxide.getGas(1000), GT_Values.NF, Materials.Calcite.getDust(1), 75);
        RA.addChemicalRecipe(Materials.Calcite.getDust(1), GT_Utility.getIntegratedCircuit(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Quicklime.getDust(1), 280);
        RA.addChemicalRecipe(Materials.Magnesia.getDust(1), GT_Values.NI, Materials.CarbonDioxide.getGas(1000), GT_Values.NF, Materials.Magnesite.getDust(1), 75);
        RA.addChemicalRecipe(Materials.Magnesite.getDust(1), GT_Utility.getIntegratedCircuit(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Magnesia.getDust(1), 280);

        RA.addChemicalRecipeForBasicMachineOnly(Materials.Methane.getCells(1), Materials.Empty.getCells(7), GT_ModHandler.getDistilledWater(2000L), Materials.CarbonDioxide.getGas(1000), Materials.Hydrogen.getCells(8), GT_Values.NI, 280, 480);
        RA.addChemicalRecipeForBasicMachineOnly(GT_ModHandler.getModItem("IC2", "itemCellEmpty", 1, 12), Materials.Empty.getCells(6), Materials.Methane.getGas(1000), Materials.CarbonDioxide.getGas(1000), Materials.Hydrogen.getCells(8), GT_Values.NI, 280, 480);
        RA.addChemicalRecipe(Materials.Methane.getCells(1), GT_Utility.getIntegratedCircuit(11), GT_ModHandler.getDistilledWater(2000L), Materials.Hydrogen.getGas(8000), Materials.CarbonDioxide.getCells(1), 280, 480);
        RA.addChemicalRecipe(GT_ModHandler.getModItem("IC2", "itemCellEmpty", 1, 12), GT_Utility.getIntegratedCircuit(11), Materials.Methane.getGas(1000), Materials.Hydrogen.getGas(8000), Materials.CarbonDioxide.getCells(1), Materials.Empty.getCells(1), 280, 480);
        RA.addChemicalRecipe(Materials.Methane.getCells(1), GT_Utility.getIntegratedCircuit(12), GT_ModHandler.getDistilledWater(2000L), Materials.Hydrogen.getGas(8000), Materials.Empty.getCells(1), 280, 480);
        RA.addChemicalRecipe(GT_ModHandler.getModItem("IC2", "itemCellEmpty", 1, 12), GT_Utility.getIntegratedCircuit(12), Materials.Methane.getGas(1000), Materials.Hydrogen.getGas(8000), Materials.Empty.getCells(2), 280, 480);
		RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(12)}, new FluidStack[]{GT_ModHandler.getDistilledWater(2000L), Materials.Methane.getGas(1000)}, new FluidStack[]{Materials.Hydrogen.getGas(8000)}, null, 280, 480);
        
        RA.addChemicalRecipe(Materials.Benzene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorine.getGas(2000), Materials.HydrochloricAcid.getFluid(1000), Materials.Chlorobenzene.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Benzene.getFluid(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.Chlorobenzene.getCells(1), Materials.Empty.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Chlorine.getCells(2), GT_Utility.getIntegratedCircuit(11), Materials.Benzene.getFluid(1000), Materials.Chlorobenzene.getFluid(1000), Materials.HydrochloricAcid.getCells(1), Materials.Empty.getCells(1), 280);

        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorobenzene.getFluid(1000), Materials.Phenol.getFluid(1000), Materials.DilutedHydrochloricAcid.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Chlorobenzene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Water.getFluid(1000), Materials.Phenol.getFluid(1000), Materials.DilutedHydrochloricAcid.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Water.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorobenzene.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Phenol.getCells(1), 280);
        RA.addChemicalRecipe(Materials.Chlorobenzene.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Water.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000), Materials.Phenol.getCells(1), 280);

        RA.addChemicalRecipe(Materials.SodiumHydroxide.getDust(4), GT_Utility.getIntegratedCircuit(1), Materials.Chlorobenzene.getFluid(4000), Materials.Phenol.getFluid(4000), Materials.Salt.getDust(6), 1200);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.SodiumHydroxide.getDust(4), Materials.Empty.getCells(4), Materials.Chlorobenzene.getFluid(4000), GT_Values.NF, Materials.Salt.getDust(6), Materials.Phenol.getCells(4), 960, 30);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.SodiumHydroxide.getDust(4), Materials.Chlorobenzene.getCells(4), GT_Values.NF, GT_Values.NF, Materials.Salt.getDust(6), Materials.Phenol.getCells(4), 960, 30);

        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Benzene.getFluid(1000), Materials.Chlorine.getGas(2000), Materials.Water.getFluid(1000)}, new FluidStack[]{Materials.Phenol.getFluid(1000), Materials.HydrochloricAcid.getFluid(1000), Materials.DilutedHydrochloricAcid.getFluid(1000)}, null, 560, 30);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.SodiumHydroxide.getDust(2), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Benzene.getFluid(2000), Materials.Chlorine.getGas(4000)}, new FluidStack[]{Materials.Phenol.getFluid(2000), Materials.HydrochloricAcid.getFluid(2000)}, new ItemStack[]{Materials.Salt.getDust(3)}, 1120, 30);

        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.LightFuel.getFluid(20000), Materials.HeavyFuel.getFluid(4000)}, new FluidStack[]{Materials.Fuel.getFluid(24000)}, null, 100, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Fuel.getFluid(10000), Materials.Tetranitromethane.getFluid(200)}, new FluidStack[]{Materials.NitroFuel.getFluid(10000)}, null, 100, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.BioDiesel.getFluid(10000), Materials.Tetranitromethane.getFluid(400)}, new FluidStack[]{Materials.NitroFuel.getFluid(9000)}, null, 100, 480);
    }

    private void addRecipesMay2017OilRefining() {

        RA.addUniversalDistillationRecipe(Materials.Gas.getGas(1000), new FluidStack[]{Materials.Butane.getGas(60), Materials.Propane.getGas(70), Materials.Ethane.getGas(100), Materials.Methane.getGas(750), Materials.Helium.getGas(20)}, GT_Values.NI, 240, 120);

        RA.addCentrifugeRecipe(null, null, Materials.Propane.getGas(320), Materials.LPG.getFluid(290), null, null, null, null, null, null, null, 20, 5);
        RA.addCentrifugeRecipe(null, null, Materials.Butane.getGas(320), Materials.LPG.getFluid(370), null, null, null, null, null, null, null, 20, 5);

        RA.addUniversalDistillationRecipe(Materials.Ethylene.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Ethane.getGas(1000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethylene.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(2000)}, null, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethylene.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(2000), Materials.Hydrogen.getGas(2000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethylene.getLightlySteamCracked(1000), new FluidStack[]{Materials.Methane.getGas(1000)}, Materials.Carbon.getDust(1), 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethylene.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Methane.getGas(1000)}, Materials.Carbon.getDust(1), 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethylene.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Methane.getGas(1000)}, Materials.Carbon.getDust(1), 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Ethane.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(2000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethane.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(2000), Materials.Hydrogen.getGas(2000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethane.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(2000), Materials.Hydrogen.getGas(4000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethane.getLightlySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(250), Materials.Methane.getGas(1250)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethane.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(125), Materials.Methane.getGas(1375)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Ethane.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Methane.getGas(1500)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Propene.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Propane.getGas(500), Materials.Ethylene.getGas(500), Materials.Methane.getGas(500)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propene.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Ethane.getGas(1000), Materials.Methane.getGas(1000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propene.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(3000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propene.getLightlySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(1000), Materials.Methane.getGas(500)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propene.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(750), Materials.Methane.getGas(750)}, GT_Values.NI, 180, 120);
        RA.addUniversalDistillationRecipe(Materials.Propene.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Methane.getGas(1500)}, GT_Values.NI, 180, 120);

        RA.addUniversalDistillationRecipe(Materials.Propane.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Ethane.getGas(1000), Materials.Methane.getGas(1000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propane.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(3000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propane.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(3000), Materials.Hydrogen.getGas(2000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propane.getLightlySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(750), Materials.Methane.getGas(1250)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propane.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(500), Materials.Methane.getGas(1500)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Propane.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Ethylene.getGas(250), Materials.Methane.getGas(1750)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Butadiene.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Butene.getGas(667), Materials.Ethylene.getGas(667)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butadiene.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Butane.getGas(223), Materials.Propene.getGas(223), Materials.Ethane.getGas(400), Materials.Ethylene.getGas(445), Materials.Methane.getGas(223)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butadiene.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Propane.getGas(260), Materials.Ethane.getGas(926), Materials.Ethylene.getGas(389), Materials.Methane.getGas(2667)}, GT_Values.NI, 112, 120);
        RA.addUniversalDistillationRecipe(Materials.Butadiene.getLightlySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(750), Materials.Ethylene.getGas(188), Materials.Methane.getGas(188)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butadiene.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(125), Materials.Ethylene.getGas(1125), Materials.Methane.getGas(188)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butadiene.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(125), Materials.Ethylene.getGas(188), Materials.Methane.getGas(1125)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Butene.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Butane.getGas(334), Materials.Propene.getGas(334), Materials.Ethane.getGas(334), Materials.Ethylene.getGas(334), Materials.Methane.getGas(334)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butene.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Propane.getGas(389), Materials.Ethane.getGas(556), Materials.Ethylene.getGas(334), Materials.Methane.getGas(1056)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butene.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Ethane.getGas(1000), Materials.Methane.getGas(2000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butene.getLightlySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(750), Materials.Ethylene.getGas(500), Materials.Methane.getGas(250)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butene.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(200), Materials.Ethylene.getGas(1300), Materials.Methane.getGas(400)}, GT_Values.NI, 192, 120);
        RA.addUniversalDistillationRecipe(Materials.Butene.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(125), Materials.Ethylene.getGas(313), Materials.Methane.getGas(1500)}, Materials.Carbon.getDust(1), 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Butane.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Propane.getGas(667), Materials.Ethane.getGas(667), Materials.Methane.getGas(667)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butane.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Ethane.getGas(1000), Materials.Methane.getGas(2000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butane.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(1000)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butane.getLightlySteamCracked(1000), new FluidStack[]{Materials.Propane.getGas(750), Materials.Ethane.getGas(125), Materials.Ethylene.getGas(125), Materials.Methane.getGas(1063)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butane.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Propane.getGas(125), Materials.Ethane.getGas(750), Materials.Ethylene.getGas(750), Materials.Methane.getGas(438)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Butane.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Propane.getGas(125), Materials.Ethane.getGas(125), Materials.Ethylene.getGas(125), Materials.Methane.getGas(2000)}, Materials.Carbon.getDust(1), 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Gas.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(1400), Materials.Hydrogen.getGas(1340), Materials.Helium.getGas(20)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Gas.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(1400), Materials.Hydrogen.getGas(3340), Materials.Helium.getGas(20)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Gas.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Methane.getGas(1400), Materials.Hydrogen.getGas(4340), Materials.Helium.getGas(20)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Gas.getLightlySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(45), Materials.Ethane.getGas(8), Materials.Ethylene.getGas(85), Materials.Methane.getGas(1026), Materials.Helium.getGas(20)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Gas.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(8), Materials.Ethane.getGas(45), Materials.Ethylene.getGas(92), Materials.Methane.getGas(1018), Materials.Helium.getGas(20)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Gas.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Propene.getGas(8), Materials.Ethane.getGas(8), Materials.Ethylene.getGas(25), Materials.Methane.getGas(1143), Materials.Helium.getGas(20)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Naphtha.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Butane.getGas(800), Materials.Propane.getGas(300), Materials.Ethane.getGas(250), Materials.Methane.getGas(250)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Naphtha.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Butane.getGas(200), Materials.Propane.getGas(1100), Materials.Ethane.getGas(400), Materials.Methane.getGas(400)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Naphtha.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Butane.getGas(125), Materials.Propane.getGas(125), Materials.Ethane.getGas(1500), Materials.Methane.getGas(1500)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Naphtha.getLightlySteamCracked(1000), new FluidStack[]{Materials.HeavyFuel.getFluid(75), Materials.LightFuel.getFluid(150), Materials.Toluene.getFluid(40), Materials.Benzene.getFluid(150), Materials.Butene.getGas(80), Materials.Butadiene.getGas(150), Materials.Propane.getGas(15), Materials.Propene.getGas(200), Materials.Ethane.getGas(35), Materials.Ethylene.getGas(200), Materials.Methane.getGas(200)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Naphtha.getModeratelySteamCracked(1000), new FluidStack[]{Materials.HeavyFuel.getFluid(50), Materials.LightFuel.getFluid(100), Materials.Toluene.getFluid(30), Materials.Benzene.getFluid(125), Materials.Butene.getGas(65), Materials.Butadiene.getGas(100), Materials.Propane.getGas(30), Materials.Propene.getGas(400), Materials.Ethane.getGas(50), Materials.Ethylene.getGas(350), Materials.Methane.getGas(350)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Naphtha.getSeverelySteamCracked(1000), new FluidStack[]{Materials.HeavyFuel.getFluid(25), Materials.LightFuel.getFluid(50), Materials.Toluene.getFluid(20), Materials.Benzene.getFluid(100), Materials.Butene.getGas(50), Materials.Butadiene.getGas(50), Materials.Propane.getGas(15), Materials.Propene.getGas(300), Materials.Ethane.getGas(65), Materials.Ethylene.getGas(500), Materials.Methane.getGas(500)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.LightFuel.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Naphtha.getFluid(800), Materials.Octane.getFluid(100), Materials.Butane.getGas(150), Materials.Propane.getGas(200), Materials.Ethane.getGas(125), Materials.Methane.getGas(125)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.LightFuel.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Naphtha.getFluid(500), Materials.Octane.getFluid(50), Materials.Butane.getGas(200), Materials.Propane.getGas(1100), Materials.Ethane.getGas(400), Materials.Methane.getGas(400)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.LightFuel.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Naphtha.getFluid(200), Materials.Octane.getFluid(20), Materials.Butane.getGas(125), Materials.Propane.getGas(125), Materials.Ethane.getGas(1500), Materials.Methane.getGas(1500)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.LightFuel.getLightlySteamCracked(1000), new FluidStack[]{Materials.HeavyFuel.getFluid(150), Materials.Naphtha.getFluid(400), Materials.Toluene.getFluid(40), Materials.Benzene.getFluid(200), Materials.Butene.getGas(75), Materials.Butadiene.getGas(60), Materials.Propane.getGas(20), Materials.Propene.getGas(150), Materials.Ethane.getGas(10), Materials.Ethylene.getGas(50), Materials.Methane.getGas(50)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.LightFuel.getModeratelySteamCracked(1000), new FluidStack[]{Materials.HeavyFuel.getFluid(100), Materials.Naphtha.getFluid(250), Materials.Toluene.getFluid(50), Materials.Benzene.getFluid(300), Materials.Butene.getGas(90), Materials.Butadiene.getGas(75), Materials.Propane.getGas(35), Materials.Propene.getGas(200), Materials.Ethane.getGas(30), Materials.Ethylene.getGas(150), Materials.Methane.getGas(150)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.LightFuel.getSeverelySteamCracked(1000), new FluidStack[]{Materials.HeavyFuel.getFluid(50), Materials.Naphtha.getFluid(100), Materials.Toluene.getFluid(30), Materials.Benzene.getFluid(150), Materials.Butene.getGas(65), Materials.Butadiene.getGas(50), Materials.Propane.getGas(50), Materials.Propene.getGas(250), Materials.Ethane.getGas(50), Materials.Ethylene.getGas(250), Materials.Methane.getGas(250)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.HeavyFuel.getLightlyHydroCracked(1000), new FluidStack[]{Materials.LightFuel.getFluid(600), Materials.Naphtha.getFluid(100), Materials.Butane.getGas(100), Materials.Propane.getGas(100), Materials.Ethane.getGas(75), Materials.Methane.getGas(75)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.HeavyFuel.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.LightFuel.getFluid(400), Materials.Naphtha.getFluid(400), Materials.Butane.getGas(150), Materials.Propane.getGas(150), Materials.Ethane.getGas(100), Materials.Methane.getGas(100)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.HeavyFuel.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.LightFuel.getFluid(200), Materials.Naphtha.getFluid(250), Materials.Butane.getGas(300), Materials.Propane.getGas(300), Materials.Ethane.getGas(175), Materials.Methane.getGas(175)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.HeavyFuel.getLightlySteamCracked(1000), new FluidStack[]{Materials.LightFuel.getFluid(300), Materials.Naphtha.getFluid(50), Materials.Toluene.getFluid(25), Materials.Benzene.getFluid(125), Materials.Butene.getGas(25), Materials.Butadiene.getGas(15), Materials.Propane.getGas(3), Materials.Propene.getGas(30), Materials.Ethane.getGas(5), Materials.Ethylene.getGas(50), Materials.Methane.getGas(50)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.HeavyFuel.getModeratelySteamCracked(1000), new FluidStack[]{Materials.LightFuel.getFluid(200), Materials.Naphtha.getFluid(200), Materials.Toluene.getFluid(40), Materials.Benzene.getFluid(200), Materials.Butene.getGas(40), Materials.Butadiene.getGas(25), Materials.Propane.getGas(5), Materials.Propene.getGas(50), Materials.Ethane.getGas(7), Materials.Ethylene.getGas(75), Materials.Methane.getGas(75)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.HeavyFuel.getSeverelySteamCracked(1000), new FluidStack[]{Materials.LightFuel.getFluid(100), Materials.Naphtha.getFluid(125), Materials.Toluene.getFluid(80), Materials.Benzene.getFluid(400), Materials.Butene.getGas(80), Materials.Butadiene.getGas(50), Materials.Propane.getGas(10), Materials.Propene.getGas(100), Materials.Ethane.getGas(15), Materials.Ethylene.getGas(150), Materials.Methane.getGas(150)}, GT_Values.NI, 120, 120);

        RA.addUniversalDistillationRecipe(Materials.Tar.getLightlyHydroCracked(1000), new FluidStack[]{Materials.Creosote.getFluid(600), Materials.Naphtha.getFluid(200), Materials.Kerosene.getFluid(300), Materials.Butane.getGas(150), Materials.Propane.getGas(100), Materials.Ethane.getGas(100), Materials.Methane.getGas(150)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Tar.getModeratelyHydroCracked(1000), new FluidStack[]{Materials.Creosote.getFluid(400), Materials.Naphtha.getFluid(500), Materials.Kerosene.getFluid(150), Materials.Butane.getGas(200), Materials.Propane.getGas(800), Materials.Ethane.getGas(300), Materials.Methane.getGas(500)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Tar.getSeverelyHydroCracked(1000), new FluidStack[]{Materials.Creosote.getFluid(200), Materials.Naphtha.getFluid(300), Materials.Kerosene.getFluid(100), Materials.Butane.getGas(125), Materials.Propane.getGas(75), Materials.Ethane.getGas(1200), Materials.Methane.getGas(1750)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Tar.getLightlySteamCracked(1000), new FluidStack[]{Materials.Creosote.getFluid(300), Materials.Naphtha.getFluid(50), Materials.Dimethylbenzene.getFluid(200), Materials.Styrene.getFluid(50), Materials.Butene.getGas(25), Materials.Butadiene.getGas(15), Materials.Propane.getGas(3), Materials.Propene.getGas(30), Materials.Ethane.getGas(5), Materials.Methane.getGas(25)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Tar.getModeratelySteamCracked(1000), new FluidStack[]{Materials.Creosote.getFluid(200), Materials.Naphtha.getFluid(200), Materials.Dimethylbenzene.getFluid(300), Materials.Styrene.getFluid(75), Materials.Butene.getGas(40), Materials.Butadiene.getGas(25), Materials.Propane.getGas(5), Materials.Propene.getGas(50), Materials.Ethane.getGas(7), Materials.Methane.getGas(50)}, GT_Values.NI, 120, 120);
        RA.addUniversalDistillationRecipe(Materials.Tar.getSeverelySteamCracked(1000), new FluidStack[]{Materials.Creosote.getFluid(100), Materials.Naphtha.getFluid(125), Materials.Dimethylbenzene.getFluid(500), Materials.Styrene.getFluid(100), Materials.Butene.getGas(80), Materials.Butadiene.getGas(50), Materials.Propane.getGas(10), Materials.Propene.getGas(100), Materials.Ethane.getGas(15), Materials.Methane.getGas(100)}, GT_Values.NI, 120, 120);

        //Kerosene
        RA.addChemicalRecipe(Materials.Kerosene.getCells(4), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.KeroseneMixture.getFluid(5000), Materials.Empty.getCells(4), 100, 120);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Kerosene.getFluid(4000), Materials.KeroseneMixture.getFluid(5000), Materials.Empty.getCells(1), 100, 120);

        //Fix Recipes
        RA.addExtractorRecipe(GT_ModHandler.getIC2Item("itemCellHydrant", 1L, GT_Values.W), ItemList.Cell_Empty.get(1L), 100, 2);
        RA.addCentrifugeRecipe(GT_ModHandler.getModItem("IC2", "itemCellHydrant", 1L, GT_Values.W), GT_Values.NI, GT_Values.NF, GT_Values.NF, ItemList.Cell_Empty.get(1L), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000}, 100, 2);

        //Recipes for gasoline
        RA.addChemicalRecipe(Materials.Nitrogen.getCells(2), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), Materials.NitrousOxide.getGas(3000), Materials.Empty.getCells(2), 200, 30);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Nitrogen.getGas(2000), Materials.NitrousOxide.getGas(3000), Materials.Empty.getCells(1), 200, 30);
		RA.addChemicalRecipeForBasicMachineOnly(Materials.Ethanol.getCells(1), Materials.Butene.getCells(1), GT_Values.NF, GT_Values.NF, Materials.AntiKnock.getCells(2), GT_Values.NI, 400, 480);
        RA.addMixerRecipe(Materials.Naphtha.getCells(16), Materials.Gas.getCells(2), Materials.Methanol.getCells(1), Materials.Acetone.getCells(1), GT_Values.NF, GT_Values.NF, Materials.GasolineRaw.getCells(20), 100, 480);
        RA.addChemicalRecipe(Materials.GasolineRaw.getCells(10), Materials.Toluene.getCells(1), GT_Values.NF, GT_Values.NF, Materials.GasolineRegular.getCells(11), 10, 480);
        RA.addMixerRecipe(Materials.GasolineRegular.getCells(20), Materials.Octane.getCells(2), Materials.NitrousOxide.getCells(6), Materials.Toluene.getCells(1), Materials.AntiKnock.getFluid(3000L), Materials.GasolinePremium.getFluid(32000L), Materials.Empty.getCells(29), 50, 1920);

        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Naphtha.getFluid(16000), Materials.Gas.getGas(2000), Materials.Methanol.getFluid(1000), Materials.Acetone.getFluid(1000)}, new FluidStack[]{Materials.GasolineRaw.getFluid(20000)}, null, 100, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.GasolineRaw.getFluid(10000), Materials.Toluene.getFluid(1000)}, new FluidStack[]{Materials.GasolineRegular.getFluid(11000)}, null, 10, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.GasolineRegular.getFluid(20000), Materials.Octane.getFluid(2000), Materials.NitrousOxide.getGas(6000), Materials.Toluene.getFluid(1000), Materials.AntiKnock.getFluid(3000L)}, new FluidStack[]{Materials.GasolinePremium.getFluid(32000L)}, null, 50, 1920);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Ethanol.getFluid(1000), Materials.Butene.getGas(1000)}, new FluidStack[]{Materials.AntiKnock.getFluid(2000)}, null, 400, 480);

    }

    public void addPotionRecipes(String aName, ItemStack aItem) {
        //normal
        RA.addBrewingRecipe(aItem, FluidRegistry.getFluid("potion.awkward"), FluidRegistry.getFluid("potion." + aName), false);
        //strong
        RA.addBrewingRecipe(aItem, FluidRegistry.getFluid("potion.thick"), FluidRegistry.getFluid("potion." + aName + ".strong"), false);
        //long
        RA.addBrewingRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), FluidRegistry.getFluid("potion." + aName), FluidRegistry.getFluid("potion." + aName + ".long"), false);
        //splash
        if (!(FluidRegistry.getFluid("potion." + aName) == null || FluidRegistry.getFluid("potion." + aName + ".splash") == null))
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 1L), null, null, null, new FluidStack(FluidRegistry.getFluid("potion." + aName), 750), new FluidStack(FluidRegistry.getFluid("potion." + aName + ".splash"), 750), null, 200, 24);
        //splash strong
        if (!(FluidRegistry.getFluid("potion." + aName + ".strong") == null || FluidRegistry.getFluid("potion." + aName + ".strong.splash") == null))
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 1L), null, null, null, new FluidStack(FluidRegistry.getFluid("potion." + aName + ".strong"), 750), new FluidStack(FluidRegistry.getFluid("potion." + aName + ".strong.splash"), 750), null, 200, 24);
        //splash long
        if (!(FluidRegistry.getFluid("potion." + aName + ".long") == null || FluidRegistry.getFluid("potion." + aName + ".long.splash") == null))
            RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 1L), null, null, null, new FluidStack(FluidRegistry.getFluid("potion." + aName + ".long"), 750), new FluidStack(FluidRegistry.getFluid("potion." + aName + ".long.splash"), 750), null, 200, 24);
    }

    /**
     * Adds recipes related to producing Steel in a Primitive Blast Furnace.
     * Adds recipes related to roasting sulfuric ores and reducing oxidic ores in the Electric Blast Furnace.
     */
    private void addPyrometallurgicalRecipes() {
        RA.addPrimitiveBlastRecipe(Materials.Iron.getIngots(1), GT_Values.NI, 4, Materials.Steel.getIngots(1), GT_Values.NI, 7200);
        RA.addPrimitiveBlastRecipe(Materials.Iron.getDust(1), GT_Values.NI, 4, Materials.Steel.getIngots(1), GT_Values.NI, 7200);
        RA.addPrimitiveBlastRecipe(Materials.Iron.getBlocks(1), GT_Values.NI, 36, Materials.Steel.getIngots(9), GT_Values.NI, 75000);
        RA.addPrimitiveBlastRecipe(Materials.Steel.getDust(1), GT_Values.NI, 2, Materials.Steel.getIngots(1), GT_Values.NI, 7200);
        RA.addPrimitiveBlastRecipe(Materials.Iron.getNuggets(36), GT_Values.NI, 2, Materials.WroughtIron.getNuggets(36), GT_Values.NI, 275);

        //Roasting
        RA.addBlastRecipe(Materials.Tetrahedrite.getDust(3), GT_Values.NI, Materials.Oxygen.getGas(9000), Materials.SulfurDioxide.getGas(6000), Materials.CupricOxide.getDust(3), Materials.AntimonyTrioxide.getDust(1), 120, 120, 1200);
        RA.addBlastRecipe(Materials.Chalcopyrite.getDust(1), Materials.SiliconDioxide.getDust(1), Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(2000), Materials.CupricOxide.getDust(1), Materials.Ferrosilite.getDust(1), 120, 120, 1200);

        RA.addBlastRecipe(Materials.Pyrite.getDust(1), GT_Values.NI, Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(2000), Materials.BandedIron.getDust(1), null, 120, 120, 1200);

        RA.addBlastRecipe(Materials.Pentlandite.getDust(1), GT_Values.NI, Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1000), Materials.Garnierite.getDust(1), null, 120, 120, 1200);

        RA.addBlastRecipe(Materials.Sphalerite.getDust(1), GT_Values.NI, Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1000), Materials.Zincite.getDust(1), null, 120, 120, 1200);

        RA.addBlastRecipe(Materials.Cobaltite.getDust(1), GT_Values.NI, Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1000), Materials.CobaltOxide.getDust(1), Materials.ArsenicTrioxide.getDust(1), 120, 120, 1200);

        RA.addBlastRecipe(Materials.Stibnite.getDust(1), GT_Values.NI, Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1500), Materials.AntimonyTrioxide.getDust(1), null, 120, 120, 1200);

        RA.addBlastRecipe(Materials.Galena.getDust(1), GT_Values.NI, Materials.Oxygen.getGas(3000), Materials.SulfurDioxide.getGas(1000), Materials.Massicot.getDust(1), Materials.Silver.getNuggets(6), 120, 120, 1200);

        //Carbothermic Reduction
        int outputIngotAmount = GT_Mod.gregtechproxy.mMixedOreOnlyYieldsTwoThirdsOfPureOre ? 2 : 3;
        RA.addBlastRecipe(Materials.CupricOxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Copper.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.Malachite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(3000), Materials.Copper.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.AntimonyTrioxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(3000), Materials.Antimony.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.BandedIron.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.Magnetite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.YellowLimonite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.BrownLimonite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.BasalticMineralSand.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.GraniticMineralSand.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.Cassiterite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Tin.getIngots(outputIngotAmount), null, 240, 120, 1200);
        RA.addBlastRecipe(Materials.CassiteriteSand.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Tin.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.Garnierite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Nickel.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.CobaltOxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Cobalt.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.ArsenicTrioxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Arsenic.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.Massicot.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Lead.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.Zincite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Zinc.getIngots(outputIngotAmount), null, 240, 120, 1200);

        RA.addBlastRecipe(Materials.SiliconDioxide.getDust(1), Materials.Carbon.getDust(2), GT_Values.NF, Materials.CarbonMonoxide.getGas(2000), Materials.Silicon.getIngots(1), null, 240, 120, 1200);

        if (GT_Mod.gregtechproxy.mMixedOreOnlyYieldsTwoThirdsOfPureOre) {
            RA.addBlastRecipe(Materials.CupricOxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Copper.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.Malachite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(3000), Materials.Copper.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.AntimonyTrioxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(3000), Materials.Antimony.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.BandedIron.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.Magnetite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.YellowLimonite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.BrownLimonite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.BasalticMineralSand.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.GraniticMineralSand.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Iron.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.Cassiterite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Tin.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.CassiteriteSand.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Tin.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.Garnierite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Nickel.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.CobaltOxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Cobalt.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.ArsenicTrioxide.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Arsenic.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.Massicot.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Lead.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
            RA.addBlastRecipe(Materials.Zincite.getDust(2), Materials.Carbon.getDust(1), GT_Values.NF, Materials.CarbonDioxide.getGas(1000), Materials.Zinc.getIngots(outputIngotAmount), GT_Values.NI, 240, 120, 1200);
        }
    }

    /**
     * Adds recipes related to producing Polybenzimidazole.
     */
    private void addPolybenzimidazoleRecipes() {

        if (Loader.isModLoaded("computronics")) {
            RA.addChemicalRecipe(Materials.ChromiumDioxide.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(1000), GT_Values.NF, Materials.ChromiumTrioxide.getDust(1), 300, 75);
        } else {
            RA.addChemicalRecipe(Materials.Chrome.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Oxygen.getGas(3000), GT_Values.NF, Materials.ChromiumTrioxide.getDust(1), 300, 75);
        }
		RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chrome, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(3), Materials.Oxygen.getGas(3000), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.ChromiumTrioxide, 1), 300, 75);

		//Potassium Dichromate
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.Saltpeter.getDust(2), Materials.ChromiumTrioxide.getDust(2)}, null, new FluidStack[]{Materials.Nitrogen.getGas(2000), Materials.Oxygen.getGas(5000)}, new ItemStack[]{Materials.Potassiumdichromate.getDust(1)}, 100, 480);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.PotassiumNitrade.getDust(2), Materials.ChromiumTrioxide.getDust(2)}, null, new FluidStack[]{Materials.Nitrogen.getGas(2000), Materials.Oxygen.getGas(5000)}, new ItemStack[]{Materials.Potassiumdichromate.getDust(1)}, 100, 480);
        RA.addChemicalRecipe(Materials.Saltpeter.getDust(2), Materials.Chrome.getDust(2), Materials.Oxygen.getGas(1000), Materials.Nitrogen.getGas(1000), Materials.Potassiumdichromate.getDust(1), 100, 480);
        RA.addChemicalRecipe(Materials.PotassiumNitrade.getDust(2), Materials.Chrome.getDust(2), Materials.Oxygen.getGas(1000), Materials.Nitrogen.getGas(1000), Materials.Potassiumdichromate.getDust(1), 100, 480);
        
        //Nitrochlorobenzene
        RA.addChemicalRecipe(Materials.Chlorobenzene.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.NitrationMixture.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Nitrochlorobenzene.getCells(1), 100, 480);
        RA.addChemicalRecipe(Materials.Chlorobenzene.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.NitrationMixture.getFluid(1000), Materials.Nitrochlorobenzene.getFluid(1000), Materials.DilutedSulfuricAcid.getCells(1), 100, 480);
        RA.addChemicalRecipe(Materials.NitrationMixture.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Chlorobenzene.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Nitrochlorobenzene.getCells(1), 100, 480);
        RA.addChemicalRecipe(Materials.NitrationMixture.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Chlorobenzene.getFluid(1000), Materials.Nitrochlorobenzene.getFluid(1000), Materials.DilutedSulfuricAcid.getCells(1), 100, 480);

        //Dimethylbenzene
        RA.addDistilleryRecipe(5, Materials.WoodTar.getFluid(200), Materials.Dimethylbenzene.getFluid(30), 100, 120, false);
        RA.addDistilleryRecipe(5, Materials.CharcoalByproducts.getGas(200), Materials.Dimethylbenzene.getFluid(20), 100, 120, false);

        RA.addChemicalRecipe(Materials.Methane.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Benzene.getFluid(1000), GT_Values.NF, Materials.Dimethylbenzene.getCells(1), 4000, 120);
        RA.addChemicalRecipe(Materials.Benzene.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Methane.getGas(1000), GT_Values.NF, Materials.Dimethylbenzene.getCells(1), 4000, 120);
		RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(1)}, new FluidStack[]{Materials.Benzene.getFluid(1000), Materials.Methane.getGas(1000)}, new FluidStack[]{Materials.Dimethylbenzene.getFluid(1000L)}, null, 4000, 120);
        
		//Phtalic Acid
        RA.addChemicalRecipe(Materials.Dimethylbenzene.getCells(9), Materials.Potassiumdichromate.getDust(1), Materials.Oxygen.getGas(18000), Materials.Water.getFluid(18000), Materials.PhthalicAcid.getCells(9), 900, 1920);
        RA.addChemicalRecipe(Materials.Dimethylbenzene.getCells(9), Materials.Potassiumdichromate.getDust(1), Materials.Oxygen.getGas(18000), Materials.PhthalicAcid.getFluid(9000), Materials.Water.getCells(18), 900, 1920);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(18), Materials.Potassiumdichromate.getDust(1), Materials.Dimethylbenzene.getFluid(9000), Materials.Water.getFluid(18000), Materials.PhthalicAcid.getCells(9), ItemList.Cell_Empty.get(9L), 900, 1920);
        RA.addChemicalRecipe(Materials.Oxygen.getCells(18), Materials.Potassiumdichromate.getDust(1), Materials.Dimethylbenzene.getFluid(9000), Materials.PhthalicAcid.getFluid(18000), Materials.Water.getCells(18), 900, 1920);

        //Dichlorobenzidine
        RA.addChemicalRecipe(Materials.Copper.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Nitrochlorobenzene.getFluid(9000), Materials.Dichlorobenzidine.getFluid(9000), null, 200, 1920);

        //Diphenyl Isophthalate
        RA.addChemicalRecipe(Materials.PhthalicAcid.getCells(1), Materials.SulfuricAcid.getCells(1), Materials.Phenol.getFluid(2000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Diphenylisophthalate.getCells(1), ItemList.Cell_Empty.get(1L), 100, 7680);
        RA.addChemicalRecipe(Materials.PhthalicAcid.getCells(1), Materials.Phenol.getCells(2), Materials.SulfuricAcid.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Diphenylisophthalate.getCells(1), ItemList.Cell_Empty.get(2L), 100, 7680);
        RA.addChemicalRecipe(Materials.SulfuricAcid.getCells(1), Materials.Phenol.getCells(2), Materials.PhthalicAcid.getFluid(1000), Materials.DilutedSulfuricAcid.getFluid(1000), Materials.Diphenylisophthalate.getCells(1), ItemList.Cell_Empty.get(2L), 100, 7680);

        //Diaminobenzidin
        RA.addChemicalRecipe(Materials.Ammonia.getCells(2), Materials.Zinc.getDust(1), Materials.Dichlorobenzidine.getFluid(1000), Materials.HydrochloricAcid.getFluid(2000), Materials.Diaminobenzidin.getCells(1), ItemList.Cell_Empty.get(1L), 100, 7680);
        //RA.addChemicalRecipe(Materials.Dichlorobenzidine.getCells(1),Materials.Zinc.getDust(1), Materials.Ammonia.getGas(2000), Materials.Diaminobenzidin.getFluid(1000), Materials.HydrochloricAcid.getCells(2), 100, 7680);

        //Polybenzimidazole
        RA.addChemicalRecipe(Materials.Diphenylisophthalate.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Diaminobenzidin.getFluid(1000), Materials.Polybenzimidazole.getMolten(1000), Materials.Phenol.getCells(1), 100, 7680);
        RA.addChemicalRecipe(Materials.Diaminobenzidin.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Diphenylisophthalate.getFluid(1000), Materials.Polybenzimidazole.getMolten(1000), Materials.Phenol.getCells(1), 100, 7680);

        ///*
        //---Alternative recipes---
        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(2), GT_Values.NI, Materials.Butene.getGas(1000), Materials.Ethylene.getGas(2000), GT_Values.NI, 860, 1920);
        RA.addChemicalRecipe(Materials.Butene.getCells(1), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, Materials.Ethylene.getGas(2000), Materials.Empty.getCells(1), 860, 1920);
        RA.addChemicalRecipe(Materials.Empty.getCells(2), GT_Utility.getIntegratedCircuit(12), Materials.Butene.getGas(1000), GT_Values.NF, Materials.Ethylene.getCells(2), 860, 1920);
        RA.addChemicalRecipeForBasicMachineOnly(Materials.Butene.getCells(1), Materials.Empty.getCells(1), GT_Values.NF, GT_Values.NF, Materials.Ethylene.getCells(2), GT_Values.NI, 860, 1920);
        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(2), GT_Values.NI, Materials.Butene.getGas(1000), Materials.Ethylene.getGas(1000), GT_Values.NI, 360, 7680);
        RA.addChemicalRecipe(Materials.Butene.getCells(1), GT_Utility.getIntegratedCircuit(2), GT_Values.NF, Materials.Ethylene.getGas(1000), Materials.Empty.getCells(1), 240, 7680);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(12), Materials.Butene.getGas(1000), GT_Values.NF, Materials.Ethylene.getCells(1), 240, 7680);
        RA.addChemicalRecipe(Materials.Butene.getCells(1), GT_Utility.getIntegratedCircuit(12), GT_Values.NF, GT_Values.NF, Materials.Ethylene.getCells(1), 240, 7680);
        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(4), GT_Values.NI, Materials.Ethylene.getGas(2000), Materials.Butene.getGas(1000), GT_Values.NI, 860, 1920);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(2), GT_Utility.getIntegratedCircuit(4), GT_Values.NF, Materials.Butene.getGas(1000), Materials.Empty.getCells(2), 860, 1920);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(14), Materials.Ethylene.getGas(2000), GT_Values.NF, Materials.Butene.getCells(1), 860, 1920);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(2), GT_Utility.getIntegratedCircuit(14), GT_Values.NF, GT_Values.NF, Materials.Butene.getCells(1), Materials.Empty.getCells(1), 860, 1920);
        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(4), GT_Values.NI, Materials.Ethylene.getGas(1000), Materials.Butene.getGas(1000), GT_Values.NI, 240, 7680);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(4), GT_Values.NF, Materials.Butene.getGas(1000), Materials.Empty.getCells(1), 240, 7680);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(14), Materials.Ethylene.getGas(1000), GT_Values.NF, Materials.Butene.getCells(1), 240, 7680);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(14), GT_Values.NF, GT_Values.NF, Materials.Butene.getCells(1), 240, 7680);

        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(3), GT_Values.NI, Materials.Ethylene.getGas(3000), Materials.Propene.getGas(2000), GT_Values.NI, 240, 7680);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(3), GT_Utility.getIntegratedCircuit(3), GT_Values.NF, Materials.Propene.getGas(2000), Materials.Empty.getCells(3), 240, 7680);
        RA.addChemicalRecipe(Materials.Empty.getCells(2), GT_Utility.getIntegratedCircuit(13), Materials.Ethylene.getGas(3000), GT_Values.NF, Materials.Propene.getCells(2), 240, 7680);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(3), GT_Utility.getIntegratedCircuit(13), GT_Values.NF, GT_Values.NF, Materials.Propene.getCells(2), Materials.Empty.getCells(1), 240, 7680);
        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(3), GT_Values.NI, Materials.Ethylene.getGas(1000), Materials.Propene.getGas(1000), GT_Values.NI, 240, 7680);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(3), GT_Values.NF, Materials.Propene.getGas(1000), Materials.Empty.getCells(1), 240, 7680);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(13), Materials.Ethylene.getGas(1000), GT_Values.NF, Materials.Propene.getCells(1), 240, 7680);
        RA.addChemicalRecipe(Materials.Ethylene.getCells(1), GT_Utility.getIntegratedCircuit(13), GT_Values.NF, GT_Values.NF, Materials.Propene.getCells(1), 3240, 7680);

        //RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(8), GT_Values.NI, Materials.Butene.getGas(2000), Materials.Butadiene.getGas(1000), GT_Values.NI, 860, 1920);
        //RA.addChemicalRecipe(Materials.Butene.getCells(2), GT_Utility.getIntegratedCircuit(8), GT_Values.NF, Materials.Butadiene.getGas(1000), Materials.Empty.getCells(2), 860, 1920);
        //RA.addChemicalRecipe(Materials.Empty.getCells(2), GT_Utility.getIntegratedCircuit(10), Materials.Butene.getGas(2000), GT_Values.NF, Materials.Empty.getCells(1), Materials.Butadiene.getCells(1), 860, 1920);
        //RA.addChemicalRecipe(Materials.Butene.getCells(2), GT_Values.NI, GT_Values.NF, GT_Values.NF, Materials.Butadiene.getCells(1), Materials.Empty.getCells(1), 480, 120);
        RA.addChemicalRecipe(GT_Utility.getIntegratedCircuit(8), GT_Values.NI, Materials.Butene.getGas(2000), Materials.Butadiene.getGas(1000), GT_Values.NI, 860, 1920);
        RA.addChemicalRecipe(Materials.Butene.getCells(2), GT_Utility.getIntegratedCircuit(8), GT_Values.NF, Materials.Butadiene.getGas(1000), Materials.Empty.getCells(2), 860, 1920);
        RA.addChemicalRecipe(Materials.Empty.getCells(1), GT_Utility.getIntegratedCircuit(10), Materials.Butene.getGas(2000), GT_Values.NF, Materials.Butadiene.getCells(1), 860, 1920);
        RA.addChemicalRecipe(Materials.Butene.getCells(2), GT_Utility.getIntegratedCircuit(10), GT_Values.NF, GT_Values.NF, Materials.Empty.getCells(1), Materials.Butadiene.getCells(1), 860, 1920);
        
    }

    public void run3() {
        if (GT_Mod.gregtechproxy.mHardRadonRecipe) {
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 8), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 1), Materials.Air.getGas(1000), Materials.Radon.getGas(10), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 8), 12000, 6);
			RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 32), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1), Materials.Air.getGas(1000), Materials.Radon.getGas(100), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 32), 8000, 6);
			RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 64), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Uranium235, 1), Materials.Air.getGas(1000), Materials.Radon.getGas(1000), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 64), 1000, 6);
        } else {
            RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 6), null, null, Materials.Radon.getGas(2), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 6), 12000, 6);
        }

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tin, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Silver, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderiumBase, 4L), 400, 6);
        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Electrum, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahAlloy, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BlueSteel, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RedSteel, 1L), GT_Values.NI, GT_Utility.getIntegratedCircuit(2), Materials.SolderingAlloy.getMolten(144), GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.ElectrumFlux, 4L), 400, 30720);

        RA.addMixerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderiumBase, 2L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BlackSteel, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderPearl, 1L), GT_Values.NI, GT_Values.NI, GT_Utility.getIntegratedCircuit(2), GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Enderium, 4L), 200, 480);


        RA.addMixerRecipe(new ItemStack(Blocks.dirt, 1, 32767), new ItemStack(Items.wheat, 4, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NF, Materials.Water.getFluid(100), GT_ModHandler.getModItem(aTextForestry, "fertilizerBio", 1L, 0), 200, 18);
        RA.addMixerRecipe(new ItemStack(Blocks.dirt, 1, 3), new ItemStack(Items.wheat, 4, 32767), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NF, Materials.Water.getFluid(100), GT_ModHandler.getModItem(aTextForestry, "fertilizerBio", 1L, 0), 200, 18);
        RA.addMixerRecipe(new ItemStack(Blocks.dirt, 1, 32767), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 4L), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NF, Materials.Water.getFluid(100), GT_ModHandler.getModItem(aTextForestry, "fertilizerBio", 1L, 0), 200, 18);
        RA.addMixerRecipe(new ItemStack(Blocks.dirt, 1, 3), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ash, 4L), GT_Utility.getIntegratedCircuit(1), GT_Values.NI, GT_Values.NF, Materials.Water.getFluid(100), GT_ModHandler.getModItem(aTextForestry, "fertilizerBio", 1L, 0), 200, 18);

        // --- Mobloot
        RA.addChemicalRecipe(Materials.Beryllium.getDust(1), Materials.Potassium.getDust(4), Materials.Nitrogen.getGas(5000), null, Materials.EnderPearl.getDust(10), null, 400, 120);
        RA.addChemicalRecipe(Materials.DarkAsh.getDust(1), Materials.Sulfur.getDust(1), null, null, Materials.Blaze.getDust(2), null, 200, 30);
        //RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Ball.get(0L), new FluidStack(FluidRegistry.getFluid("ender"), 250), new ItemStack(Items.ender_pearl,1,0), 100, 30);
		RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.EnderEye, 1L), Materials.SodiumPersulfate.getFluid(100), new ItemStack(Items.ender_eye, 1, 0), 10000, 800, 320);
        
        //ASMD
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Graphene, 2), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Platinum, 16)}, Materials.Polybenzimidazole.getMolten(288L), ItemList.Circuit_Parts_ResistorASMD.get(32L), 80, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 1), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.NiobiumTitanium, 16)}, Materials.Polybenzimidazole.getMolten(576L), ItemList.Circuit_Parts_DiodeASMD.get(64L), 160, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.VanadiumGallium, 2), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.HSSG, 16)}, Materials.Polybenzimidazole.getMolten(576L), ItemList.Circuit_Parts_TransistorASMD.get(64L), 160, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Polybenzimidazole, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.HSSS, 2)}, Materials.Polybenzimidazole.getMolten(576L), ItemList.Circuit_Parts_CapacitorASMD.get(64L), 160, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Naquadah, 1), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.HSSE, 32)}, Materials.Polybenzimidazole.getMolten(576L), ItemList.Circuit_Parts_InductorASMD.get(64L), 160, 1920);

        //XSMD
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Naquadria, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Pikyonium64B, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 1), GT_Utility.getIntegratedCircuit(9)}, Materials.PerroxPolymer.getMolten(144L), ItemList.Circuit_Parts_ResistorXSMD.get(32L), 160, 122880);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tritanium, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Lafium, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 1), GT_Utility.getIntegratedCircuit(9)}, Materials.PerroxPolymer.getMolten(144L), ItemList.Circuit_Parts_DiodeXSMD.get(32L), 160, 122880);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.BlackPlutonium, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.ElectrumFlux, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 1), GT_Utility.getIntegratedCircuit(9)}, Materials.PerroxPolymer.getMolten(144L), ItemList.Circuit_Parts_TransistorXSMD.get(32L), 160, 122880);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Draconium, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.CinobiteA243, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 1), GT_Utility.getIntegratedCircuit(9)}, Materials.PerroxPolymer.getMolten(144L), ItemList.Circuit_Parts_CapacitorXSMD.get(32L), 160, 122880);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Infuscolium, 4), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Oriharukon, 2), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 1), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 1), GT_Utility.getIntegratedCircuit(9)}, Materials.PerroxPolymer.getMolten(144L), ItemList.Circuit_Parts_InductorXSMD.get(32L), 160, 122880);

        //Galacticraft stones
        if (Loader.isModLoaded("GalacticraftCore")) {
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftCore", "tile.moonBlock", 1L, 3), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Moon, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SiliconDioxide, 1)}, new int[]{10000, 1000}, 400, 30);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftCore", "tile.moonBlock", 1L, 4), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Moon, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SiliconDioxide, 1)}, new int[]{10000, 1000}, 400, 30);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftCore", "tile.moonBlock", 1L, 5), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Moon, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SiliconDioxide, 1)}, new int[]{10000, 1000}, 400, 30);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.mars", 1L, 4), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Mars, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BandedIron, 1)}, new int[]{10000, 1000}, 400, 75);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.mars", 1L, 5), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Mars, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BandedIron, 1)}, new int[]{10000, 1000}, 400, 75);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.mars", 1L, 6), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Mars, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BandedIron, 1)}, new int[]{10000, 1000}, 400, 75);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.mars", 1L, 9), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Mars, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BandedIron, 1)}, new int[]{10000, 1000}, 400, 75);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.asteroidsBlock", 1L, 0), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Asteroid, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 1)}, new int[]{10000, 1000, 500}, 400, 120);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.asteroidsBlock", 1L, 1), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Asteroid, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 1)}, new int[]{10000, 1000, 500}, 400, 120);
            RA.addPulveriserRecipe(GT_ModHandler.getModItem("GalacticraftMars", "tile.asteroidsBlock", 1L, 2), new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Asteroid, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 1)}, new int[]{10000, 1000, 500}, 400, 120);

            RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Moon, 1), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SiliconDioxide, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Manganese, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Olivine, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.RareEarth, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 1L), new int[]{1200, 500, 300, 180, 120, 60}, 400, 30);
            RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Mars, 1), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.BandedIron, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ruby, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iridium, 1L), new int[]{1200, 700, 300, 180, 120, 60}, 600, 75);
            RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Asteroid, 1), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Gold, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Titanium, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Ruby, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chrome, 1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Osmium, 1L), new int[]{1200, 700, 300, 180, 120, 40}, 800, 120);
        }

        if (Loader.isModLoaded("impact")) {
            RA.addAssemblylineRecipe(ItemList.MicroTransmitter_UV.get(1L), 576000, new Object[]{
                    ItemList.MicroTransmitter_UV.get(1L),
                    ItemList.Casing_Dyson_Ring.get(8L),
                    ItemList.Casing_Fusion_Coil.get(8L),
                    ItemList.Field_Generator_UV.get(4L),
                    ItemList.Sensor_UV.get(4L),
                    ItemList.Emitter_UV.get(4L),
                    new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 4},
                    GT_OreDictUnificator.get(OrePrefixes.wireGt04, Materials.SuperconductorUV, 16)
            }, new FluidStack[]{
                    Materials.EnrichedNaquadria.getFluid(1296),
                    Materials.Infuscolium.getMolten(1296),
                    new FluidStack(FluidRegistry.getFluid("ic2coolant"), 2000),
                    Materials.Osmiridium.getMolten(1152)
            }, ItemList.Machine_MultiblockTinyWormHole.get(1L), 4000, 500000);
        }
	    
        //Fusion Reactor MKIV & MKV
        //IV
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Fusion_Coil.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Americium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Superconductor, 4), ItemList.Field_Generator_ZPM.get(2L), ItemList.Electric_Pump_ZPM.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 4), ItemList.Neutron_Reflector.get(4L), GT_Utility.getIntegratedCircuit(1)}, Materials.ElectrumFlux.getMolten(864), ItemList.Casing_Fusion_Coil2.get(1L), 200, 500000, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Fusion_Coil.get(4L), GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Neutronium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Infinite, 4), ItemList.Field_Generator_UV.get(2L), ItemList.Electric_Pump_UV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 12), ItemList.Neutron_Reflector.get(8L), GT_Utility.getIntegratedCircuit(1)}, Materials.ElectrumFlux.getMolten(1728), ItemList.Casing_Fusion_Coil2.get(4L), 400, 2000000, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Fusion_Coil.get(16L), GT_OreDictUnificator.get(OrePrefixes.plateTriple, Materials.Phoenixite, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Bio, 4), ItemList.Field_Generator_UHV.get(2L), ItemList.Electric_Pump_UHV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 36), ItemList.Neutron_Reflector.get(12L), GT_Utility.getIntegratedCircuit(1)}, Materials.ElectrumFlux.getMolten(3456), ItemList.Casing_Fusion_Coil2.get(16L), 800, 8000000, true);

        //V
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Fusion_Coil2.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Neutronium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Infinite, 4), ItemList.Field_Generator_UV.get(2L), ItemList.Electric_Pump_UV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 12), ItemList.Neutron_Reflector.get(8L), GT_Utility.getIntegratedCircuit(1)}, Materials.DraconiumAwakened.getMolten(864), ItemList.Casing_Fusion_Coil3.get(1L), 400, 2000000, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Fusion_Coil2.get(4L), GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Phoenixite, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Bio, 4), ItemList.Field_Generator_UHV.get(2L), ItemList.Electric_Pump_UHV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 24), ItemList.Neutron_Reflector.get(12L), GT_Utility.getIntegratedCircuit(1)}, Materials.DraconiumAwakened.getMolten(1728), ItemList.Casing_Fusion_Coil3.get(4L), 800, 8000000, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Fusion_Coil2.get(16L), GT_OreDictUnificator.get(OrePrefixes.plateTriple, Materials.InfinityCatalyst, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Nano, 4), ItemList.Field_Generator_UEV.get(2L), ItemList.Electric_Pump_UEV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 48), ItemList.Neutron_Reflector.get(16L), GT_Utility.getIntegratedCircuit(1)}, Materials.DraconiumAwakened.getMolten(3456), ItemList.Casing_Fusion_Coil3.get(16L), 1200, 32000000, true);


        //Sluice Juice and Sand
        RA.addUniversalDistillationRecipe(Materials.SluiceJuice.getFluid(1000L), new FluidStack[]{GT_ModHandler.getDistilledWater(500L)}, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SluiceSand, 1), 300, 18);

        RA.addCentrifugeRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.SluiceSand, 1), null, null, null, GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Neodymium, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Nickel, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cobalt, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Manganese, 1), new int[]{10000, 500, 500, 500, 500, 500}, 200, 18);
        RA.addCentrifugeRecipe(null, null, Materials.SluiceJuice.getFluid(1000), Materials.Water.getFluid(500L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Tin, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Zinc, 1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Chrome, 1), new int[]{1000, 200, 200, 200, 200, 200}, 40, 75);

        //Stone Dust
        RA.addCentrifugeRecipe(Materials.Stone.getDust(1), GT_Values.NI, GT_Values.NF, GT_Values.NF,
                Materials.Quartzite.getDust(1),Materials.PotassiumFeldspar.getDust(1),Materials.Marble.getDust(1),
                Materials.Biotite.getDust(1), 	Materials.MetalMixture.getDust(1), 		Materials.Sodalite.getDust(1),
                new int[]{2500, 2500, 2000, 1000, 1000, 800}, 480, 120);
        //Metal Mixture
        RA.addCentrifugeRecipe(Materials.MetalMixture.getDust(1), GT_Values.NI, GT_Values.NF, GT_Values.NF,
                Materials.BandedIron.getDust(1), 	Materials.Bauxite.getDust(1), Materials.Pyrolusite.getDust(1),
                Materials.Barite.getDust(1), 		Materials.Chromite.getDust(1), Materials.Ilmenite.getDust(1),
                new int[]{2500, 2500, 2000, 1000, 1000, 800}, 1000, 480);

        //Bioline
        RA.addCentrifugeRecipe(ItemList.PlantMass.get(1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_ModHandler.getModItem("IC2", "itemBiochaff", 1L, 0), GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, GT_Values.NI, new int[]{10000}, 300, 6);
        RA.addPulveriserRecipe(GT_ModHandler.getModItem("IC2", "itemFuelPlantBall", 1L, 0), new ItemStack[]{ItemList.PlantMass.get(1L), ItemList.PlantMass.get(1L), ItemList.PlantMass.get(1L), ItemList.PlantMass.get(1L)}, new int[]{10000, 5000, 3300, 2500}, 200, 2);
        RA.addChemicalBathRecipe(GT_ModHandler.getModItem("IC2", "itemBiochaff", 1L, 0), Materials.Water.getFluid(750L), GT_ModHandler.getModItem("Forestry", "mulch", 8L, 0), GT_ModHandler.getModItem("Forestry", "mulch", 4L, 0), GT_ModHandler.getModItem("Forestry", "mulch", 4L, 0), new int[]{10000, 3300, 2000}, 500, 30);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem("IC2", "itemFuelPlantBall", 16L, 0), Materials.Biomass.getFluid(8), ItemList.RawBioFiber.get(1L), 3300, 200, 20);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem("IC2", "itemFuelPlantBall", 16L, 0), Materials.Fuel.getFluid(8), ItemList.RawBioFiber.get(1L), 9000, 200, 20);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem("IC2", "itemFuelPlantBall", 16L, 0), Materials.NitroFuel.getFluid(8), ItemList.RawBioFiber.get(1L), 10000, 200, 20);
        RA.addAutoclaveRecipe(GT_ModHandler.getModItem("IC2", "itemFuelPlantBall", 16L, 0), Materials.Methanol.getFluid(8), ItemList.RawBioFiber.get(1L), 5000, 200, 20);
        RA.addAssemblerRecipe(ItemList.RawBioFiber.get(2L), GT_Utility.getIntegratedCircuit(1), ItemList.BioOrganicMesh.get(1L), 800, 2);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.block, Materials.Lignite, 1), ItemList.CompressedBioBall.get(8L), ItemList.BioChunk.get(1L), 1200, 75);
        RA.addCompressorRecipe(GT_ModHandler.getModItem("IC2", "itemWeed", 16L), ItemList.IC2_Plantball.get(1L), 300, 2);
        RA.addCompressorRecipe(new ItemStack(Items.apple, 8), ItemList.IC2_Plantball.get(1L), 300, 2);
        RA.addCompressorRecipe(ItemList.BioOrganicMesh.get(1L), ItemList.BioCarbonPlate.get(1L), 300, 2);
        RA.addCompressorRecipe(ItemList.BioBall.get(1L), ItemList.CompressedBioBall.get(1L), 300, 2);
        RA.addBlastRecipe(ItemList.BioCarbonPlate.get(1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_ModHandler.getIC2Item("carbonPlate", 1L), GT_Values.NI, 1000, 120, 1200);
        RA.addBlastRecipe(ItemList.BioChunk.get(1L), GT_Values.NI, GT_Values.NF, GT_Values.NF, GT_ModHandler.getIC2Item("coalChunk", 1L), GT_Values.NI, 1000, 120, 1200);
        RA.addMixerRecipe(GT_ModHandler.getModItem("IC2", "itemFuelPlantBall", 16L, 0), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Flint, 2), GT_Values.NI, GT_Values.NI, GT_Values.NF, GT_Values.NF, ItemList.BioBall.get(1L), 200, 18);

        RA.addFluidCannerRecipe(ItemList.Spray_Empty.get(1L), ItemList.IC2_Spray_WeedEx.get(1L), new FluidStack(ItemList.sHerbicide, 1000), GT_Values.NF);
        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Natrium, 1), GT_ModHandler.getModItem("IC2", "itemWeed", 4L), Materials.SulfuricAcid.getFluid(1000), new FluidStack(ItemList.sHerbicide, 2000), null, 480);

        //===

        RA.addFluidHeaterRecipe(GT_Utility.getIntegratedCircuit(1), Materials.BioMediumRaw.getFluid(200), Materials.BioMediumSterilized.getFluid(200), 20, 30720);
        //RA.addChemicalRecipe(ItemList.Circuit_Chip_Stemcell.get(4L), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.MysteriousCrystal, 1), Materials.BioMediumRaw.getFluid(1000L), Materials.Mutagen.getFluid(250L), ItemList.Circuit_Chip_Biocell.get(1L), 1200, 30720);
        RA.addMixerRecipe(Materials.AlienOrganic.getDust(1), Materials.MysteriousCrystal.getDust(1), Materials.Oriharukon.getDust(1), GT_Utility.getIntegratedCircuit(1), Materials.Mutagen.getFluid(500), Materials.BioMediumRaw.getFluid(1000), Materials.Diamond.getDust(2), 200, 122880);

        //===
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.EnrichedMysteriousCrystal.getDust(1), GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.AlienBiomass.getFluid(9000), Materials.Hydrogen.getGas(1000)}, new FluidStack[]{Materials.FermentedAlienBiomass.getFluid(10000)}, new ItemStack[]{Materials.MysteriousCrystal.getDust(1)}, 7500, 90000);
        RA.addUniversalDistillationRecipe(Materials.FermentedAlienBiomass.getFluid(1000), new FluidStack[]{Materials.Oil.getFluid(200), Materials.Water.getFluid(200), Materials.Ethanol.getFluid(150), Materials.Methanol.getFluid(150), Materials.GrowthMediumRaw.getFluid(75), Materials.GrowthMediumSterilized.getFluid(40), Materials.Bacteria.getFluid(35), Materials.PerroxSuperHeavy.getFluid(65), Materials.PerroxHeavy.getFluid(35), Materials.PerroxLight.getFluid(30), Materials.PerroxSuperLight.getFluid(20)}, Materials.Ash.getDust(1), 200, 275000);
        RA.addCrackingRecipe(24, Materials.PerroxSuperHeavy.getFluid(1000), Materials.Nickel.getPlasma(10), Materials.PerroxSuperHeavyCracked.getGas(1000), 800, 275000);
        RA.addCrackingRecipe(24, Materials.PerroxHeavy.getFluid(1000), Materials.Zinc.getPlasma(10), Materials.PerroxHeavyCracked.getGas(1000), 800, 275000);
        RA.addCrackingRecipe(24, Materials.PerroxLight.getFluid(1000), Materials.Niobium.getPlasma(10), Materials.PerroxLightCracked.getGas(1000), 800, 275000);
        RA.addCrackingRecipe(24, Materials.PerroxSuperLight.getFluid(1000), Materials.Silver.getPlasma(10), Materials.PerroxSuperLightCracked.getGas(1000), 800, 275000);
        RA.addUniversalDistillationRecipe(Materials.PerroxSuperHeavyCracked.getGas(1000), new FluidStack[]{Materials.FermentedBiomass.getFluid(400), Materials.EnrichedBacterialSludge.getFluid(300), Materials.PerroxSuperHeavy.getFluid(100), Materials.Radon.getGas(80), Materials.PerroxHeavy.getFluid(80), Materials.PerroxGas.getGas(40)}, null, 100, 275000);
        RA.addUniversalDistillationRecipe(Materials.PerroxHeavyCracked.getGas(1000), new FluidStack[]{Materials.FermentedBiomass.getFluid(350), Materials.EnrichedBacterialSludge.getFluid(250), Materials.PerroxHeavy.getFluid(120), Materials.Radon.getGas(100), Materials.PerroxLight.getFluid(100), Materials.PerroxGas.getGas(80)}, null, 100, 275000);
        RA.addUniversalDistillationRecipe(Materials.PerroxLightCracked.getGas(1000), new FluidStack[]{Materials.FermentedBiomass.getFluid(330), Materials.EnrichedBacterialSludge.getFluid(220), Materials.PerroxLight.getFluid(150), Materials.Radon.getGas(120), Materials.PerroxSuperLight.getFluid(80), Materials.PerroxGas.getGas(100)}, null, 100, 275000);
        RA.addUniversalDistillationRecipe(Materials.PerroxSuperLightCracked.getGas(1000), new FluidStack[]{Materials.FermentedBiomass.getFluid(300), Materials.EnrichedBacterialSludge.getFluid(200), Materials.PerroxSuperLight.getFluid(200), Materials.Radon.getGas(150), Materials.PerroxGas.getGas(150)}, null, 100, 275000);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.PerroxGas.getGas(1000), Materials.SuperCoolant.getFluid(1000)}, new FluidStack[]{Materials.PerroxGasCoolant.getFluid(1000), new FluidStack(FluidRegistry.getFluid("ic2coolant"), 1000)}, null, 300, 122880);
        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(2)}, new FluidStack[]{Materials.PerroxGasCoolant.getFluid(2160), Materials.Nitrogen.getPlasma(7500L), Materials.Tin.getPlasma(100L)}, new FluidStack[]{Materials.PerroxHot.getFluid(4320)}, null, 200, 500000);
        RA.addFluidSolidifierRecipe(ItemList.Shape_Mold_Ingot.get(0), Materials.PerroxHot.getFluid(144), GT_OreDictUnificator.get(OrePrefixes.ingotHot, Materials.PerroxPolymer, 1L), 100, 500000);
        RA.addVacuumFreezerRecipe(GT_OreDictUnificator.get(OrePrefixes.ingotHot, Materials.PerroxPolymer, 1L), GT_OreDictUnificator.get(OrePrefixes.ingot, Materials.PerroxPolymer, 1L), 200, 122880);

        // --- Liquid Freezer
        RA.addVacuumFreezerRecipe(Materials.Oxygen.getGas(10000), FluidRegistry.getFluidStack("liquidoxygen", 1000), 240, 480);
        RA.addVacuumFreezerRecipe(Materials.Nitrogen.getGas(10000), FluidRegistry.getFluidStack("liquidnitrogen", 1000), 240, 480);
        RA.addVacuumFreezerRecipe(Materials.Air.getGas(10000), Materials.LiquidAir.getFluid(1000), 240, 480);
        RA.addDistillationTowerRecipe(Materials.LiquidAir.getFluid(200000), new FluidStack[]{
                Materials.Nitrogen.getGas(156160), Materials.Oxygen.getGas(41890), Materials.Argon.getGas(1864),
                Materials.CarbonDioxide.getGas(80), Materials.Helium.getGas(9), Materials.Methane.getGas(3),
                Materials.Hydrogen.getGas(1), Materials.NitrousOxide.getGas(1)}, null, 4000, 480);

        if (Loader.isModLoaded("GalacticraftCore")) {
            //LightRadox + Nq -> Enriched Naquadah condensation int aChance, int aDuration, int aEUt, boolean aCleanroom
            RA.addAutoclaveSpaceRecipe(Materials.Naquadah.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.NaquadahEnriched.getDust(3), 10000, 350, 7680, true);

            RA.addAutoclaveSpaceRecipe(Materials.Lead.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.Orichalcum.getDust(3), 10000, 350, 7680, true);

            RA.addAutoclaveSpaceRecipe(Materials.Lapis.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.Lapotron.getDust(3), 10000, 350, 7680, true);

            RA.addAutoclaveSpaceRecipe(Materials.Emerald.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.MysteriousCrystal.getDust(3), 10000, 350, 7680, true);
            RA.addAutoclaveSpaceRecipe(Materials.Olivine.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.MysteriousCrystal.getDust(3), 10000, 350, 7680, true);

            //heavy radox + Nq+ -> Nq*
            RA.addAutoclaveSpaceRecipe(Materials.NaquadahEnriched.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Naquadria.getDust(3), 10000, 350, 122880, true);

            RA.addAutoclaveSpaceRecipe(Materials.Lithium.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Indium.getDust(3), 10000, 350, 122880, true);

            RA.addAutoclaveSpaceRecipe(Materials.Thorium.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Lutetium.getDust(3), 10000, 350, 122880, true);

            RA.addAutoclaveSpaceRecipe(Materials.Plutonium.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Plutonium241.getDust(3), 10000, 350, 122880, true);

        } else {
            //LightRadox + Nq -> Enriched Naquadah condensation int aChance, int aDuration, int aEUt, boolean aCleanroom
            RA.addAutoclaveRecipe(Materials.Naquadah.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.NaquadahEnriched.getDust(3), 10000, 350, 7680, false);

            RA.addAutoclaveRecipe(Materials.Lead.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.Orichalcum.getDust(3), 10000, 350, 7680, false);

            RA.addAutoclaveRecipe(Materials.Lapis.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.Lapotron.getDust(3), 10000, 350, 7680, false);

            RA.addAutoclaveRecipe(Materials.Emerald.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.MysteriousCrystal.getDust(3), 10000, 350, 7680, false);
            RA.addAutoclaveRecipe(Materials.Olivine.getDust(1), Materials.PerroxLight.getFluid(2000), Materials.MysteriousCrystal.getDust(3), 10000, 350, 7680, false);

            //heavy radox + Nq+ -> Nq*
            RA.addAutoclaveRecipe(Materials.NaquadahEnriched.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Naquadria.getDust(3), 10000, 350, 122880, false);

            RA.addAutoclaveRecipe(Materials.Lithium.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Indium.getDust(3), 10000, 350, 122880, false);

            RA.addAutoclaveRecipe(Materials.Thorium.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Lutetium.getDust(3), 10000, 350, 122880, false);

            RA.addAutoclaveRecipe(Materials.Plutonium.getDust(1), Materials.PerroxHeavy.getFluid(4000), Materials.Plutonium241.getDust(3), 10000, 350, 122880, false);

        }
        //super heavy -> heavy radox conversion
        RA.addCentrifugeRecipe(null, null, Materials.PerroxSuperHeavy.getFluid(1000), Materials.PerroxHeavy.getFluid(2000), null, null, null, null, null, null, null, 60000, 500000);

        RA.addCentrifugeRecipe(null, null, Materials.PerroxSuperLight.getFluid(2000), Materials.PerroxLight.getFluid(1000), null, null, null, null, null, null, null, 60000, 500000);

        RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Chlorine.getGas(1000), Materials.PerroxHeavy.getFluid(4000)}, new FluidStack[]{Materials.Fluorine.getGas(3000)}, null, 350, 122880);


        //RA.addMultiblockChemicalRecipe(new ItemStack[]{GT_Utility.getIntegratedCircuit(24)}, new FluidStack[]{Materials.Nitrogen.getGas(2000), new FluidStack(FluidRegistry.getFluid("ic2coolant"), 1000), Materials.Ledox.getMolten(144)},  new FluidStack[]{Materials.SuperCoolant.getFluid(3000)}, null, 700, 122880);

        //NaquadahChemistry
        //1
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.NitricAcid.getFluid(1000), Materials.AmmoniaNitrate.getGas(1000), Materials.Empty.getCells(1), 380, 496);
        RA.addChemicalRecipe(Materials.NitricAcid.getCells(1), GT_Utility.getIntegratedCircuit(1), Materials.Ammonia.getGas(1000), Materials.AmmoniaNitrate.getGas(1000), Materials.Empty.getCells(1), 380, 496);
        RA.addChemicalRecipe(Materials.Ammonia.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.NitricAcid.getFluid(1000), GT_Values.NF, Materials.AmmoniaNitrate.getCells(1), 380, 496);
        RA.addChemicalRecipe(Materials.NitricAcid.getCells(1), GT_Utility.getIntegratedCircuit(11), Materials.Ammonia.getGas(1000), GT_Values.NF, Materials.AmmoniaNitrate.getCells(1), 380, 496);

        RA.addChemicalRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 1), null, Materials.AmmoniaNitrate.getGas(1000), Materials.NaquadahSollution.getFluid(1000), null, 600, 1200);
        RA.addCentrifugeRecipe(null, null, Materials.NaquadahSollution.getFluid(2000), Materials.NaquadahLiquidClear.getFluid(1000), Materials.Platinum.getDust(5), Materials.Iridium.getDust(3), Materials.Naquadah.getDust(1), null, null, null, new int[]{1000, 1000, 1000}, 200, 4100);
        RA.addUniversalDistillationRecipe(Materials.NaquadahLiquidClear.getFluid(1000), new FluidStack[]{Materials.ComplicatedNaquadahSever.getFluid(440), Materials.ComplicatedNaquadahModer.getFluid(320), Materials.ComplicatedNaquadahLight.getFluid(180), Materials.ComplicatedNaquadhaGas.getGas(150)}, null, 90, 7680);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadahSever.getFluid(2000)}, new FluidStack[]{Materials.NaquadahHeavy.getFluid(1000)}, new ItemStack[]{Materials.Iridium.getDust(1)}, new int[]{10000}, 200, 30720);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadahModer.getFluid(2000)}, new FluidStack[]{Materials.NaquadahMedium.getFluid(1000)}, new ItemStack[]{Materials.Iridium.getDust(1)}, new int[]{10000}, 140, 30720);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadahLight.getFluid(2000)}, new FluidStack[]{Materials.NaquadahLight.getFluid(1000)}, new ItemStack[]{Materials.Iridium.getDust(1)}, new int[]{10000}, 90, 30720);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadhaGas.getGas(2000)}, new FluidStack[]{Materials.NaquadhaGas.getGas(1000)}, new ItemStack[]{Materials.Iridium.getDust(1)}, new int[]{10000}, 50, 30720);
        RA.addCrackingRecipe(24, Materials.NaquadahHeavy.getFluid(1000), Materials.Fluorine.getGas(6000), Materials.NaquadahHeavyFluorine.getFluid(2000), 300, 9870);
        RA.addCrackingRecipe(24, Materials.NaquadahMedium.getFluid(1000), Materials.Fluorine.getGas(4000), Materials.NaquadahMediumFluorine.getFluid(1600), 250, 9870);
        RA.addCrackingRecipe(24, Materials.NaquadahLight.getFluid(1000), Materials.Fluorine.getGas(2000), Materials.NaquadahLightFluorine.getFluid(1200), 200, 9870);
        RA.addUniversalDistillationRecipe(Materials.NaquadahHeavyFluorine.getFluid(1000), new FluidStack[]{Materials.Fluorine.getGas(250), Materials.NaquadahHeavyFuel.getFluid(400), Materials.NaquadahMediumFuel.getFluid(200), Materials.NaquadahLightFuel.getFluid(100), Materials.NaquadhaGas.getGas(50)}, null, 160, 16400);
        RA.addUniversalDistillationRecipe(Materials.NaquadahMediumFluorine.getFluid(1000), new FluidStack[]{Materials.Fluorine.getGas(150), Materials.NaquadahHeavyFuel.getFluid(100), Materials.NaquadahMediumFuel.getFluid(400), Materials.NaquadahLightFuel.getFluid(200), Materials.NaquadhaGas.getGas(150)}, null, 140, 16400);
        RA.addUniversalDistillationRecipe(Materials.NaquadahLightFluorine.getFluid(1000), new FluidStack[]{Materials.Fluorine.getGas(50), Materials.NaquadahHeavyFuel.getFluid(50), Materials.NaquadahMediumFuel.getFluid(150), Materials.NaquadahLightFuel.getFluid(400), Materials.NaquadhaGas.getGas(350)}, null, 120, 16400);

        //2
        RA.addMultiblockChemicalRecipe(new ItemStack[]{Materials.NaquadahEnriched.getDust(1)}, new FluidStack[]{Materials.NaquadhaGas.getGas(100), new FluidStack(ItemList.sBlueVitriol, 900)}, new FluidStack[]{Materials.NaquadahESollution.getFluid(1000)}, null, 200, 30720);
        RA.addCentrifugeRecipe(null, null, Materials.NaquadahESollution.getFluid(2000), Materials.NaquadahELiquidClear.getFluid(1000), Materials.Platinum.getDust(5), Materials.Osmium.getDust(3), Materials.NaquadahEnriched.getDust(1), null, null, null, new int[]{1000, 1000, 1000}, 300, 96870);
        RA.addUniversalDistillationRecipe(Materials.NaquadahELiquidClear.getFluid(1000), new FluidStack[]{Materials.ComplicatedNaquadahESever.getFluid(440), Materials.ComplicatedNaquadahEModer.getFluid(320), Materials.ComplicatedNaquadahELight.getFluid(180), Materials.ComplicatedNaquadhaGas.getGas(150)}, null, 140, 75000);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadahESever.getFluid(8000)}, new FluidStack[]{Materials.NaquadahEHeavy.getFluid(4000)}, new ItemStack[]{Materials.Iridium.getDust(2), Materials.Osmium.getDust(3), Materials.Naquadria.getDust(1)}, new int[]{10000, 10000, 10000}, 1200, 122880);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadahEModer.getFluid(8000)}, new FluidStack[]{Materials.NaquadahEMedium.getFluid(4000)}, new ItemStack[]{Materials.Iridium.getDust(2), Materials.Osmium.getDust(3), Materials.Naquadria.getDust(1)}, new int[]{10000, 10000, 10000}, 960, 122880);
        RA.addCyclonRecipe(null, new FluidStack[]{Materials.ComplicatedNaquadahELight.getFluid(8000)}, new FluidStack[]{Materials.NaquadahELight.getFluid(4000)}, new ItemStack[]{Materials.Iridium.getDust(2), Materials.Osmium.getDust(3), Materials.Naquadria.getDust(1)}, new int[]{10000, 10000, 10000}, 760, 122880);
        RA.addCrackingRecipe(24, Materials.NaquadahEHeavy.getFluid(1000), Materials.Radon.getGas(6000), Materials.NaquadahEHeavyRadon.getFluid(2000), 300, 48670);
        RA.addCrackingRecipe(24, Materials.NaquadahEMedium.getFluid(1000), Materials.Radon.getGas(4000), Materials.NaquadahEMediumRadon.getFluid(1600), 250, 48670);
        RA.addCrackingRecipe(24, Materials.NaquadahELight.getFluid(1000), Materials.Radon.getGas(2000), Materials.NaquadahELightRadon.getFluid(1200), 200, 48670);
        RA.addUniversalDistillationRecipe(Materials.NaquadahEHeavyRadon.getFluid(1000), new FluidStack[]{Materials.Radon.getGas(250), Materials.NaquadahEHeavyFuel.getFluid(400), Materials.NaquadahEMediumFuel.getFluid(200), Materials.NaquadahELightFuel.getFluid(100), Materials.NaquadhaGas.getGas(50)}, null, 240, 96870);
        RA.addUniversalDistillationRecipe(Materials.NaquadahEMediumRadon.getFluid(1000), new FluidStack[]{Materials.Radon.getGas(150), Materials.NaquadahEHeavyFuel.getFluid(100), Materials.NaquadahEMediumFuel.getFluid(400), Materials.NaquadahELightFuel.getFluid(200), Materials.NaquadhaGas.getGas(150)}, null, 200, 96870);
        RA.addUniversalDistillationRecipe(Materials.NaquadahELightRadon.getFluid(1000), new FluidStack[]{Materials.Radon.getGas(50), Materials.NaquadahEHeavyFuel.getFluid(50), Materials.NaquadahEMediumFuel.getFluid(150), Materials.NaquadahELightFuel.getFluid(400), Materials.NaquadhaGas.getGas(350)}, null, 160, 96870);

        //3
        RA.addCyclonRecipe(new ItemStack[]{Materials.Naquadria.getDust(1)}, new FluidStack[]{Materials.NitrogenDioxide.getGas(500), Materials.SulfuricAcid.getFluid(500)}, new FluidStack[]{Materials.NaquadriaLiquid.getFluid(1000)}, new ItemStack[]{Materials.Lutetium.getDust(1), Materials.Uranium.getDust(1), Materials.NaquadahEnriched.getDust(1), Materials.Plutonium241.getDust(1)}, new int[]{1000, 1000, 1000, 1000}, 300, 75000);
        RA.addCyclonRecipe(new ItemStack[]{Materials.Europium.getDust(1)}, new FluidStack[]{Materials.NaquadahLightFuel.getFluid(500), Materials.NaquadahELightFuel.getFluid(300), Materials.NaquadriaLiquid.getFluid(200), Materials.Uranium.getMolten(144)}, new FluidStack[]{Materials.HyperFuelI.getFluid(2000)}, new ItemStack[]{Materials.Naquadah.getDust(1)}, new int[]{10000}, 460, 300000);
        RA.addCyclonRecipe(new ItemStack[]{Materials.Americium.getDust(1)}, new FluidStack[]{Materials.NaquadahMediumFuel.getFluid(400), Materials.NaquadahEMediumFuel.getFluid(350), Materials.NaquadriaLiquid.getFluid(250), Materials.Uranium235.getMolten(144)}, new FluidStack[]{Materials.HyperFuelII.getFluid(2000)}, new ItemStack[]{Materials.NaquadahEnriched.getDust(1)}, new int[]{10000}, 520, 500000);
        RA.addCyclonRecipe(new ItemStack[]{Materials.Neutronium.getDust(1)}, new FluidStack[]{Materials.NaquadahHeavyFuel.getFluid(300), Materials.NaquadahEHeavyFuel.getFluid(400), Materials.NaquadriaLiquid.getFluid(300), Materials.Plutonium.getMolten(144)}, new FluidStack[]{Materials.HyperFuelIII.getFluid(2000)}, new ItemStack[]{Materials.Naquadria.getDust(1)}, new int[]{10000}, 580, 180000);


        //EnderIO Fused Quartz and Glass
        if (Loader.isModLoaded("EnderIO")) {
            RA.addAlloySmelterRecipe(Materials.CertusQuartz.getDust(2), Materials.Glass.getDust(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), 500, 30);
            RA.addAlloySmelterRecipe(Materials.NetherQuartz.getDust(2), Materials.Glass.getDust(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), 500, 30);
            RA.addAlloySmelterRecipe(Materials.CertusQuartz.getDust(2), Materials.Quartzite.getDust(2), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), 500, 30);
            RA.addAlloySmelterRecipe(Materials.NetherQuartz.getDust(2), Materials.Quartzite.getDust(2), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), 500, 30);
            RA.addAlloySmelterRecipe(Materials.CertusQuartz.getDust(1), Materials.BorosilicateGlass.getDust(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), 500, 90);
            RA.addAlloySmelterRecipe(Materials.NetherQuartz.getDust(1), Materials.BorosilicateGlass.getDust(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), 500, 90);

            RA.addAlloySmelterRecipe(Materials.Glass.getDust(3), GT_Utility.getIntegratedCircuit(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 1), 500, 30);
            RA.addAlloySmelterRecipe(Materials.Quartzite.getDust(4), GT_Utility.getIntegratedCircuit(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 1), 500, 30);
            RA.addAlloySmelterRecipe(Materials.BorosilicateGlass.getDust(1), GT_Utility.getIntegratedCircuit(1), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 2L, 1), 500, 90);

            RA.addAlloySmelterRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), Materials.Glowstone.getDust(4), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 2), 500, 30);
            RA.addAlloySmelterRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), new ItemStack(Blocks.glowstone), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 2), 500, 30);
            RA.addAlloySmelterRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 1), Materials.Glowstone.getDust(4), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 3), 500, 30);
            RA.addAlloySmelterRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 1), new ItemStack(Blocks.glowstone), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 3), 500, 30);

            for (int i = 0; i < OreDictionary.getOres("dyeBlack").size(); i++) {
                RA.addAlloySmelterRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), OreDictionary.getOres("dyeBlack").get(i).splitStack(4), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 4), 500, 30);
                RA.addAlloySmelterRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 1), OreDictionary.getOres("dyeBlack").get(i).splitStack(4), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 5), 500, 30);
            }

            RA.addChemicalBathRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L), new FluidStack(FluidRegistry.getFluid("dye.chemical.dyeblack"), 72), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 4), GT_Values.NI, GT_Values.NI, new int[]{10000}, 500, 30);
            RA.addChemicalBathRecipe(GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 1), new FluidStack(FluidRegistry.getFluid("dye.chemical.dyeblack"), 72), GT_ModHandler.getModItem("EnderIO", "blockFusedQuartz", 1L, 5), GT_Values.NI, GT_Values.NI, new int[]{10000}, 500, 30);
        }
        
        //New coils
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.IronMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Lead, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.ULV_Coil.get(1L), 200, 6, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.IronMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Steel, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.LV_Coil.get(1L), 200, 30, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Aluminium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.MV_Coil.get(1L), 200, 120, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.EnergeticAlloy, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.HV_Coil.get(1L), 200, 480, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.TungstenSteel, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.EV_Coil.get(1L), 200, 1920, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.NeodymiumMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Iridium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.IV_Coil.get(1L), 200, 7680, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.NeodymiumMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Osmiridium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.LuV_Coil.get(1L), 200, 30720, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.EuropiumoxideMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Europium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.ZPM_Coil.get(1L), 200, 122880, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.EuropiumoxideMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.ElectrumFlux, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.UV_Coil.get(1L), 200, 500000, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.EuropiumoxideMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Infuscolium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.UHV_Coil.get(1L), 200, 2000000, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.EuropiumoxideMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.Neutronium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.UEV_Coil.get(1L), 200, 8000000, false);
        RA.addWireAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.stick, Materials.EuropiumoxideMagnetic, 1L), GT_OreDictUnificator.get(OrePrefixes.wireFine, Materials.BlackPlutonium, 16L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.UIV_Coil.get(1L), 200, 30000000, false);

        //Digital Transformers
        RA.addAssemblerRecipe(new ItemStack[]{GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1L, 12154), ItemList.Cover_Screen.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Titanium, 2L), ItemList.EV_Coil.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Data, 1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Machine_DigitalTransformer_EV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1L, 12155), ItemList.Cover_Screen.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.TungstenSteel, 2L), ItemList.IV_Coil.get(1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Elite, 1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Machine_DigitalTransformer_IV.get(1L), 200, 7680);

        RA.addAssemblylineRecipe(ItemList.Machine_DigitalTransformer_IV.get(1, new Object() {
                }), 72000, new Object[]{
                        GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1L, 12156),
                        ItemList.Cover_Screen.get(1L),
                        GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Chrome, 2L),
                        ItemList.LuV_Coil.get(1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Master), 2}
                },
                new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(1440)},
                ItemList.Machine_DigitalTransformer_LuV.get(1), 400, 30720);

        RA.addAssemblylineRecipe(ItemList.Machine_DigitalTransformer_LuV.get(1, new Object() {
                }), 144000, new Object[]{
                        GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1L, 12157),
                        ItemList.Cover_Screen.get(4L),
                        GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Iridium, 2L),
                        ItemList.ZPM_Coil.get(1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 2}
                },
                new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(2880)},
                ItemList.Machine_DigitalTransformer_ZPM.get(1), 500, 122880);

        RA.addAssemblylineRecipe(ItemList.Machine_DigitalTransformer_ZPM.get(1, new Object() {
                }), 288000, new Object[]{
                        GT_ModHandler.getModItem("gregtech", "gt.blockmachines", 1L, 12158),
                        ItemList.Cover_Screen.get(8L),
                        GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Osmium, 2L),
                        ItemList.UV_Coil.get(1L),
                        new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 2}
                },
                new FluidStack[]{
                        Materials.SolderingAlloy.getMolten(5760)},
                ItemList.Machine_DigitalTransformer_UV.get(1), 600, 500000);

        //Pipes
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.pipeSmall, Materials.TungstenSteel, 1L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(24)}, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.pipeSmall, Materials.Ultimate, 1L), 300, 96);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.TungstenSteel, 1L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(24)}, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Ultimate, 1L), 400, 280);
        RA.addAssemblerRecipe(new ItemStack[]{GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.TungstenSteel, 1L), ItemList.Electric_Pump_IV.get(2L), GT_Utility.getIntegratedCircuit(24)}, GT_Values.NF, GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Ultimate, 1L), 600, 320);

        //Energy Hatches
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_ULV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Lead, 1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Primitive, 2L), ItemList.ULV_Coil.get(2L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Lead, 1L), GT_Utility.getIntegratedCircuit(1)}, Materials.Lubricant.getFluid(2000), ItemList.Hatch_Energy_ULV.get(1L), 200, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_LV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Basic, 2L), ItemList.LV_Coil.get(2L), ItemList.Electric_Pump_LV.get(1L), GT_Utility.getIntegratedCircuit(1)}, Materials.Lubricant.getFluid(2000), ItemList.Hatch_Energy_LV.get(1L), 200, 30);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_MV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Good, 2L), ItemList.MV_Coil.get(2L), ItemList.Electric_Pump_MV.get(1L), GT_Utility.getIntegratedCircuit(1)}, Materials.Lubricant.getFluid(2000), ItemList.Hatch_Energy_MV.get(1L), 200, 120);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_HV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Gold, 1L), ItemList.Circuit_Chip_LPIC.get(2L), ItemList.HV_Coil.get(2L), ItemList.Reactor_Coolant_He_1.get(1L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Energy_HV.get(1L), 200, 480);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Aluminium, 1L), ItemList.Circuit_Chip_PIC.get(2L), ItemList.EV_Coil.get(2L), ItemList.Reactor_Coolant_He_1.get(1L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Energy_EV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorIV, 1L), ItemList.Circuit_Chip_HPIC.get(2L), ItemList.IV_Coil.get(2L), ItemList.Reactor_Coolant_He_3.get(1L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Energy_IV.get(1L), 200, 7680);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_HV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Gold, 1L), ItemList.Circuit_Chip_LPIC.get(2L), ItemList.HV_Coil.get(2L), ItemList.Reactor_Coolant_NaK_1.get(1L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Energy_HV.get(1L), 200, 480);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.cableGt01, Materials.Aluminium, 1L), ItemList.Circuit_Chip_PIC.get(2L), ItemList.EV_Coil.get(2L), ItemList.Reactor_Coolant_NaK_1.get(1L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Energy_EV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorIV, 1L), ItemList.Circuit_Chip_HPIC.get(2L), ItemList.IV_Coil.get(2L), ItemList.Reactor_Coolant_NaK_3.get(1L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Energy_IV.get(1L), 200, 7680);

        //Dynamo Hatches
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_ULV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Lead, 1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Primitive, 2L), ItemList.ULV_Coil.get(2L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Lead, 1L), GT_Utility.getIntegratedCircuit(1)}, Materials.Lubricant.getFluid(2000), ItemList.Hatch_Dynamo_ULV.get(1L), 200, 6);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_LV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Tin, 1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Basic, 2L), ItemList.LV_Coil.get(2L), ItemList.Electric_Pump_LV.get(1L), GT_Utility.getIntegratedCircuit(1)}, Materials.Lubricant.getFluid(2000), ItemList.Hatch_Dynamo_LV.get(1L), 200, 30);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_MV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Copper, 1L), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Good, 2L), ItemList.MV_Coil.get(2L), ItemList.Electric_Pump_MV.get(1L), GT_Utility.getIntegratedCircuit(1)}, Materials.Lubricant.getFluid(2000), ItemList.Hatch_Dynamo_MV.get(1L), 200, 120);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_HV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Gold, 1L), ItemList.Circuit_Chip_LPIC.get(2L), ItemList.HV_Coil.get(2L), ItemList.Reactor_Coolant_He_1.get(1L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Dynamo_HV.get(1L), 200, 480);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Aluminium, 1L), ItemList.Circuit_Chip_PIC.get(2L), ItemList.EV_Coil.get(2L), ItemList.Reactor_Coolant_He_1.get(1L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Dynamo_EV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Vanadiumtriindinid, 1L), ItemList.Circuit_Chip_HPIC.get(2L), ItemList.IV_Coil.get(2L), ItemList.Reactor_Coolant_He_3.get(1L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Dynamo_IV.get(1L), 200, 7680);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_HV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Gold, 1L), ItemList.Circuit_Chip_LPIC.get(2L), ItemList.HV_Coil.get(2L), ItemList.Reactor_Coolant_NaK_1.get(1L), ItemList.Electric_Pump_HV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Dynamo_HV.get(1L), 200, 480);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Aluminium, 1L), ItemList.Circuit_Chip_PIC.get(2L), ItemList.EV_Coil.get(2L), ItemList.Reactor_Coolant_NaK_1.get(1L), ItemList.Electric_Pump_EV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Dynamo_EV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Hull_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Vanadiumtriindinid, 1L), ItemList.Circuit_Chip_HPIC.get(2L), ItemList.IV_Coil.get(2L), ItemList.Reactor_Coolant_NaK_3.get(1L), ItemList.Electric_Pump_IV.get(1L), GT_Utility.getIntegratedCircuit(1)}, GT_Values.NF, ItemList.Hatch_Dynamo_IV.get(1L), 200, 7680);

        //Muffler Hatches
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_HV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.StainlessSteel, 1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.StainlessSteel, 1L), ItemList.Electric_Motor_HV.get(1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_HV.get(1L), 200, 480);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Titanium, 1L), ItemList.Electric_Motor_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Titanium, 1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_EV.get(1L), 200, 1920);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.TungstenSteel, 1L), ItemList.Electric_Motor_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.TungstenSteel, 1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_IV.get(1L), 200, 7680);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_LuV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Enderium, 1L), ItemList.Electric_Motor_LuV.get(1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Iridium, 1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_LuV.get(1L), 200, 30720);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_ZPM.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Naquadah, 1L), ItemList.Electric_Motor_ZPM.get(1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Osmium, 1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_ZPM.get(1L), 200, 122880);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_UV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Duranium, 1L), ItemList.Electric_Motor_UV.get(1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.NaquadahAlloy, 1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_UV.get(1L), 200, 500000);
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_MAX.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Infuscolium, 1L), ItemList.Electric_Motor_UHV.get(1L), GT_OreDictUnificator.get(OrePrefixes.rotor, Materials.Diamericiumtitanium, 1L), GT_Utility.getIntegratedCircuit(3)}, GT_Values.NF, ItemList.Hatch_Muffler_MAX.get(1L), 200, 2000000);

        //Energy Hatches LuV-UIV
        RA.addAssemblylineRecipe(ItemList.Hatch_Energy_IV.get(1, new Object() {
                }), 72000, new Object[]{
                        ItemList.Hull_LuV.get(1L, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 2L),
                        ItemList.Circuit_Chip_UHPIC.get(2L, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Master), 2},
                        ItemList.LuV_Coil.get(2L, new Object() {
                        }),
                        new ItemStack[]{ItemList.Reactor_Coolant_He_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_1.get(1, new Object() {
                        })},
                        new ItemStack[]{ItemList.Reactor_Coolant_He_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_1.get(1, new Object() {
                        })},
                        ItemList.Electric_Pump_LuV.get(1L, new Object() {
                        })},
                new FluidStack[]{
                        new FluidStack(FluidRegistry.getFluid("ic2coolant"), 2000),
                        Materials.SolderingAlloy.getMolten(720)},
                ItemList.Hatch_Energy_LuV.get(1), 400, 30720);


        RA.addAssemblylineRecipe(ItemList.Hatch_Energy_LuV.get(1, new Object() {
                }), 144000, new Object[]{
                        ItemList.Hull_ZPM.get(1L, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorZPM, 2L),
                        ItemList.Circuit_Chip_NPIC.get(2L, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 2},
                        ItemList.ZPM_Coil.get(2L, new Object() {
                        }),
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        ItemList.Electric_Pump_ZPM.get(1L, new Object() {
                        })},
                new FluidStack[]{
                        new FluidStack(FluidRegistry.getFluid("ic2coolant"), 4000),
                        Materials.SolderingAlloy.getMolten(1440)},
                ItemList.Hatch_Energy_ZPM.get(1), 600, 122880);

        RA.addAssemblylineRecipe(ItemList.Hatch_Energy_ZPM.get(1, new Object() {
                }), 288000, new Object[]{
                        ItemList.Hull_UV.get(1L, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorUV, 2L),
                        ItemList.Circuit_Chip_PPIC.get(2L, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 2},
                        ItemList.UV_Coil.get(2L, new Object() {
                        }),
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        ItemList.Electric_Pump_UV.get(1L, new Object() {
                        })},
                new FluidStack[]{
                        new FluidStack(FluidRegistry.getFluid("ic2coolant"), 8000),
                        Materials.SolderingAlloy.getMolten(2880)},
                ItemList.Hatch_Energy_UV.get(1), 800, 500000);


        //Dynamo Hatches LuV-UIV
        RA.addAssemblylineRecipe(ItemList.Hatch_Dynamo_IV.get(1, new Object() {
                }), 72000, new Object[]{
                        ItemList.Hull_LuV.get(1L, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Tetraindiumditindibariumtitaniumheptacoppertetrakaidekaoxid, 2L),
                        ItemList.Circuit_Chip_UHPIC.get(2L, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Master), 2},
                        ItemList.LuV_Coil.get(2L, new Object() {
                        }),
                        new ItemStack[]{ItemList.Reactor_Coolant_He_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_1.get(1, new Object() {
                        })},
                        new ItemStack[]{ItemList.Reactor_Coolant_He_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_3.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_1.get(1, new Object() {
                        })},
                        ItemList.Electric_Pump_LuV.get(1L, new Object() {
                        })},
                new FluidStack[]{
                        new FluidStack(FluidRegistry.getFluid("ic2coolant"), 2000),
                        Materials.SolderingAlloy.getMolten(720)},
                ItemList.Hatch_Dynamo_LuV.get(1), 400, 30720);


        RA.addAssemblylineRecipe(ItemList.Hatch_Dynamo_LuV.get(1, new Object() {
                }), 144000, new Object[]{
                        ItemList.Hull_ZPM.get(1L, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.spring, Materials.Tetranaquadahdiindiumhexaplatiumosminid, 2L),
                        ItemList.Circuit_Chip_NPIC.get(2L, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Ultimate), 2},
                        ItemList.ZPM_Coil.get(2L, new Object() {
                        }),
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        ItemList.Electric_Pump_ZPM.get(1L, new Object() {
                        })},
                new FluidStack[]{
                        new FluidStack(FluidRegistry.getFluid("ic2coolant"), 4000),
                        Materials.SolderingAlloy.getMolten(1440)},
                ItemList.Hatch_Dynamo_ZPM.get(1), 600, 122880);

        RA.addAssemblylineRecipe(ItemList.Hatch_Dynamo_ZPM.get(1, new Object() {
                }), 288000, new Object[]{
                        ItemList.Hull_UV.get(1L, new Object() {
                        }),
                        GT_OreDictUnificator.get(OrePrefixes.spring, Materials.ElectrumFlux, 2L),
                        ItemList.Circuit_Chip_PPIC.get(2L, new Object() {
                        }),
                        new Object[]{OrePrefixes.circuit.get(Materials.Superconductor), 2},
                        ItemList.UV_Coil.get(2L, new Object() {
                        }),
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        new ItemStack[]{ItemList.Reactor_Coolant_He_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_NaK_6.get(1, new Object() {
                        }), ItemList.Reactor_Coolant_Le_2.get(1, new Object() {
                        })},
                        ItemList.Electric_Pump_UV.get(1L, new Object() {
                        })},
                new FluidStack[]{
                        new FluidStack(FluidRegistry.getFluid("ic2coolant"), 8000),
                        Materials.SolderingAlloy.getMolten(2880)},
                ItemList.Hatch_Dynamo_UV.get(1), 800, 500000);

        //END-GAME

        RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer2.get(1), GT_Utility.copyAmount(0, GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderPearl, 1)), ItemList.Circuit_Wafer_NAND.get(1), 900, 480, true);
        RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer3.get(1), GT_Utility.copyAmount(0, GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderPearl, 1)), ItemList.Circuit_Wafer_NAND.get(4), 600, 1920, true);
        RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer2.get(1), GT_Utility.copyAmount(0, GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderEye, 1)), ItemList.Circuit_Wafer_NOR.get(1), 900, 480, true);
        RA.addLaserEngraverRecipe(ItemList.Circuit_Silicon_Wafer3.get(1), GT_Utility.copyAmount(0, GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnderEye, 1)), ItemList.Circuit_Wafer_NOR.get(4), 600, 1920, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_ILC.get(1), ItemList.Circuit_Chip_ILC.get(8), null, 900, 75);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_Ram.get(1), ItemList.Circuit_Chip_Ram.get(32), null, 900, 96);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_NAND.get(1), ItemList.Circuit_Chip_NAND.get(32), null, 900, 280, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_NOR.get(1), ItemList.Circuit_Chip_NOR.get(16), null, 900, 280, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_CPU.get(1), ItemList.Circuit_Chip_CPU.get(8), null, 900, 120, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_Simple_SoC.get(1L), ItemList.Circuit_Chip_Simple_SoC.get(6L), GT_Values.NI, 900, 80, false);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC.get(1), ItemList.Circuit_Chip_SoC.get(6), null, 900, 480, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC2.get(1), ItemList.Circuit_Chip_SoC2.get(6), null, 900, 1020, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC3.get(1), ItemList.Circuit_Chip_SoC3.get(6), null, 900, 1920, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC4.get(1), ItemList.Circuit_Chip_SoC4.get(6), null, 900, 4100, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC5.get(1), ItemList.Circuit_Chip_SoC5.get(6), null, 1000, 2000000, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_SoC6.get(1), ItemList.Circuit_Chip_SoC6.get(6), null, 1000, 8000000, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_ULPIC.get(1L), ItemList.Circuit_Chip_ULPIC.get(6L), GT_Values.NI, 900, 120, false);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_LPIC.get(1L), ItemList.Circuit_Chip_LPIC.get(4L), GT_Values.NI, 900, 480, false);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_PIC.get(1), ItemList.Circuit_Chip_PIC.get(4), null, 900, 1920, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_HPIC.get(1), ItemList.Circuit_Chip_HPIC.get(2), null, 900, 7860, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_UHPIC.get(1), ItemList.Circuit_Chip_UHPIC.get(2), null, 900, 30720, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_NanoCPU.get(1), ItemList.Circuit_Chip_NanoCPU.get(8), null, 900, 480, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_QuantumCPU.get(1), ItemList.Circuit_Chip_QuantumCPU.get(4), null, 900, 1920, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_NPIC.get(1L), ItemList.Circuit_Chip_NPIC.get(2L), GT_Values.NI, 900, 122880, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_PPIC.get(1L), ItemList.Circuit_Chip_PPIC.get(2L), GT_Values.NI, 900, 500000, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_QPIC.get(1L), ItemList.Circuit_Chip_QPIC.get(2L), GT_Values.NI, 900, 2000000, true);
        RA.addCutterRecipe(ItemList.Circuit_Wafer_FPIC.get(1L), ItemList.Circuit_Chip_FPIC.get(2L), GT_Values.NI, 1000, 8000000, true);

        RA.addChemicalRecipe(ItemList.Circuit_Wafer_PIC.get(1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 2), Materials.RedstoneAlloy.getMolten(288), null, ItemList.Circuit_Wafer_HPIC.get(1), GT_Values.NI, 1200, 7860, true);
        RA.addChemicalRecipe(ItemList.Circuit_Wafer_HPIC.get(1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 8), Materials.EnergeticAlloy.getMolten(576), null, ItemList.Circuit_Wafer_UHPIC.get(1), GT_Values.NI, 1200, 30720, true);
        RA.addChemicalRecipe(ItemList.Circuit_Wafer_NPIC.get(1L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 64), Materials.VibrantAlloy.getMolten(1440L), GT_Values.NF, ItemList.Circuit_Wafer_PPIC.get(1L), GT_Values.NI, 1200, 122880, true);

        RA.addChemicalRecipe(ItemList.Circuit_Wafer_CPU.get(1), GT_Utility.copyAmount(16, Ic2Items.carbonFiber), Materials.Glowstone.getMolten(576), null, ItemList.Circuit_Wafer_NanoCPU.get(1), GT_Values.NI, 1200, 1920, true);
        RA.addChemicalRecipe(ItemList.Circuit_Wafer_NanoCPU.get(1), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 1), Materials.Radon.getGas(50), null, ItemList.Circuit_Wafer_QuantumCPU.get(1), GT_Values.NI, 1200, 1920, true);
        RA.addChemicalRecipe(ItemList.Circuit_Wafer_NanoCPU.get(1), ItemList.QuantumEye.get(2), Materials.GalliumArsenide.getMolten(288), null, ItemList.Circuit_Wafer_QuantumCPU.get(1), GT_Values.NI, 900, 1920, true);

        RA.addLaserEngraverRecipe(ItemList.Circuit_Wafer_SoC2.get(1L), GT_OreDictUnificator.get(OrePrefixes.lens, Materials.NetherStar, 1L).copy().splitStack(0), ItemList.Circuit_Chip_RPico.get(1L), 600, 128800, true);

        //Energy Crystals UV-UIV
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.MysteriousCrystal, 1L),
                ItemList.Circuit_Crystalmainframe.get(2L), null,
                ItemList.MysteriousCrystal.get(1L), 300, 500000);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.MysteriousCrystal, 1L),
                ItemList.Circuit_Wetwaresupercomputer.get(2L), null,
                ItemList.MysteriousCrystal.get(1L), 300, 500000);
        RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.gem, Materials.MysteriousCrystal, 1L),
                ItemList.Circuit_Biowarecomputer.get(2L), null,
                ItemList.MysteriousCrystal.get(1L), 300, 500000);

        RA.addLaserEngraverRecipe(ItemList.MysteriousCrystal.get(1L), GT_OreDictUnificator.get(OrePrefixes.lens, Materials.EnrichedMysteriousCrystal, 1L).copy().splitStack(0), ItemList.Circuit_Parts_MECrystal_Chip_Elite.get(3L), 450, 500000, true);

        //Circuits UV-UEV

        if (Loader.isModLoaded("GalacticraftCore")) {
            RA.addAutoclaveSpaceRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.MysteriousCrystal, 1), Materials.Neutronium.getMolten(16), ItemList.Circuit_Parts_RawMCrystalChip.get(1), 1000, 12000, 7680, true);
            RA.addAutoclaveSpaceRecipe(ItemList.Circuit_Parts_RawMCrystalParts.get(1L), FluidRegistry.getFluidStack("mutagen", 250), ItemList.Circuit_Parts_RawMCrystalChip.get(1L), 6000, 12000, 30720, true);
            RA.addAutoclaveSpaceRecipe(ItemList.Circuit_Parts_RawMCrystalParts.get(1L), Materials.BioMediumSterilized.getFluid(250L), ItemList.Circuit_Parts_RawMCrystalChip.get(1L), 9000, 12000, 30720, true);
        } else {
            RA.addAutoclaveRecipe(GT_OreDictUnificator.get(OrePrefixes.gemExquisite, Materials.MysteriousCrystal, 1), Materials.Neutronium.getMolten(16), ItemList.Circuit_Parts_RawMCrystalChip.get(1), 1000, 12000, 7680, true);
            RA.addAutoclaveRecipe(ItemList.Circuit_Parts_RawMCrystalParts.get(1L), FluidRegistry.getFluidStack("mutagen", 250), ItemList.Circuit_Parts_RawMCrystalChip.get(1L), 6000, 12000, 30720, true);
            RA.addAutoclaveRecipe(ItemList.Circuit_Parts_RawMCrystalParts.get(1L), Materials.BioMediumSterilized.getFluid(250L), ItemList.Circuit_Parts_RawMCrystalChip.get(1L), 9000, 12000, 30720, true);
        }
        RA.addForgeHammerRecipe(ItemList.Circuit_Parts_RawMCrystalChip.get(1), ItemList.Circuit_Parts_RawMCrystalParts.get(9), 200, 30720);
        RA.addBlastRecipe(ItemList.Circuit_Parts_RawMCrystalChip.get(1), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.MysteriousCrystal, 1), Materials.Radon.getGas(1000), null, ItemList.Circuit_Parts_MCrystal_Chip_Elite.get(1), null, 900, 122880, 11000);

        //Fusion Coil
        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Coil_Superconductor.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Europium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Master, 4), ItemList.Field_Generator_IV.get(2L), ItemList.Electric_Pump_IV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 4), ItemList.Neutron_Reflector.get(2L), GT_Utility.getIntegratedCircuit(1)}, Materials.YttriumBariumCuprate.getMolten(864), ItemList.Casing_Fusion_Coil.get(1L), 200, 30720, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Coil_Superconductor.get(4L), GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Americium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Ultimate, 8), ItemList.Field_Generator_LuV.get(2L), ItemList.Electric_Pump_LuV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 8), ItemList.Neutron_Reflector.get(4L), GT_Utility.getIntegratedCircuit(2)}, Materials.YttriumBariumCuprate.getMolten(1728), ItemList.Casing_Fusion_Coil.get(4L), 300, 122880, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Coil_Superconductor.get(16L), GT_OreDictUnificator.get(OrePrefixes.plateTriple, Materials.Neutronium, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Superconductor, 12), ItemList.Field_Generator_ZPM.get(2L), ItemList.Electric_Pump_ZPM.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 16), ItemList.Neutron_Reflector.get(8L), GT_Utility.getIntegratedCircuit(3)}, Materials.YttriumBariumCuprate.getMolten(3456), ItemList.Casing_Fusion_Coil.get(16L), 400, 500000, true);

        RA.addAssemblerRecipe(new ItemStack[]{ItemList.Casing_Coil_Superconductor.get(64L), GT_OreDictUnificator.get(OrePrefixes.plateQuadruple, Materials.Phoenixite, 4), GT_OreDictUnificator.get(OrePrefixes.circuit, Materials.Infinite, 16), ItemList.Field_Generator_UV.get(2L), ItemList.Electric_Pump_UV.get(1L), GT_OreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Superconductor, 32), ItemList.Neutron_Reflector.get(16L), GT_Utility.getIntegratedCircuit(4)}, Materials.YttriumBariumCuprate.getMolten(6912), ItemList.Casing_Fusion_Coil.get(64L), 400, 2000000, true);

        addBusAndHatchRecipes();
    }
}
