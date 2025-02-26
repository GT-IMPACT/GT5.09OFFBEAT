package gregtech.common.items;

import cpw.mods.fml.common.Loader;
import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.interfaces.IItemBehaviour;
import gregtech.api.interfaces.ITexture;
import gregtech.api.items.GT_MetaBase_Item;
import gregtech.api.items.GT_MetaGenerated_Item_X32;
import gregtech.api.objects.GT_MultiTexture;

import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.*;
import gregtech.common.covers.*;
import gregtech.common.items.behaviors.*;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

import java.util.Collection;
import java.util.List;

public class GT_MetaGenerated_Item_01 extends GT_MetaGenerated_Item_X32 {
    private final static String aTextStick = " S ";
    private final static String aTextFeather = "F  ";
    private final static String aTextEmptyRow = "   ";
    private final static String aTextShape = " P ";
    public static GT_MetaGenerated_Item_01 INSTANCE;
    private final String mToolTipPurify = GT_LanguageManager.addStringLocalization("metaitem.01.tooltip.purify", "Throw into Cauldron to get clean Dust");

    public GT_MetaGenerated_Item_01() {
        super("metaitem.01", OrePrefixes.dustTiny, OrePrefixes.dustSmall, OrePrefixes.dust, OrePrefixes.dustImpure, OrePrefixes.dustPure, OrePrefixes.crushed, OrePrefixes.crushedPurified, OrePrefixes.crushedCentrifuged, OrePrefixes.gem, OrePrefixes.nugget, null, OrePrefixes.ingot, OrePrefixes.ingotHot, OrePrefixes.ingotDouble, OrePrefixes.ingotTriple, OrePrefixes.ingotQuadruple, OrePrefixes.ingotQuintuple, OrePrefixes.plate, OrePrefixes.plateDouble, OrePrefixes.plateTriple, OrePrefixes.plateQuadruple, OrePrefixes.plateQuintuple, OrePrefixes.plateDense, OrePrefixes.stick, OrePrefixes.lens, OrePrefixes.round, OrePrefixes.bolt, OrePrefixes.screw, OrePrefixes.ring, OrePrefixes.foil, OrePrefixes.cell, OrePrefixes.cellPlasma);
        INSTANCE = this;

        int tLastID = 0;

        setBurnValue(17000 + Materials.Wood.mMetaItemSubID, 1600);
        GT_OreDictUnificator.addToBlacklist(new ItemStack(this, 1, 17000 + Materials.Wood.mMetaItemSubID));
        GT_ModHandler.addCompressionRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1L), new ItemStack(this, 1, 17000 + Materials.Wood.mMetaItemSubID));
        GregTech_API.registerCover(new ItemStack(this, 1, 17000 + Materials.Wood.mMetaItemSubID), TextureFactory.of(Textures.BlockIcons.COVER_WOOD_PLATE), null);

        ItemStack tStack = new ItemStack(this, 1, 17000 + Materials.Wood.mMetaItemSubID);
        tStack.setStackDisplayName("The holy Planks of Sengir");
        GT_Utility.ItemNBT.addEnchantment(tStack, Enchantment.smite, 10);
        GT_ModHandler.addCraftingRecipe(tStack, GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"XXX", "XDX", "XXX", 'X', OrePrefixes.gem.get(Materials.NetherStar), 'D', new ItemStack(Blocks.dragon_egg, 1, 32767)});

        ItemList.Credit_Greg_Copper.set(addItem(tLastID = 0, "Copper GT Credit", "0.125 Credits"));
        ItemList.Credit_Greg_Cupronickel.set(addItem(tLastID = 1, "Cupronickel GT Credit", "1 Credit", new ItemData(Materials.Cupronickel, 907200L)));
        ItemList.Credit_Greg_Silver.set(addItem(tLastID = 2, "Silver GT Credit", "8 Credits"));
        ItemList.Credit_Greg_Gold.set(addItem(tLastID = 3, "Gold GT Credit", "64 Credits"));
        ItemList.Credit_Greg_Platinum.set(addItem(tLastID = 4, "Platinum GT Credit", "512 Credits"));
        ItemList.Credit_Greg_Osmium.set(addItem(tLastID = 5, "Osmium GT Credit", "4096 Credits"));
        ItemList.Credit_Greg_Naquadah.set(addItem(tLastID = 6, "Naquadah GT Credit", "32768 Credits"));
        ItemList.Credit_Greg_Neutronium.set(addItem(tLastID = 7, "Neutronium GT Credit", "262144 Credits"));
        ItemList.Coin_Gold_Ancient.set(addItem(tLastID = 8, "Ancient Gold Coin", "Found in ancient Ruins", new ItemData(Materials.Gold, 907200L)));
        ItemList.Coin_Doge.set(addItem(tLastID = 9, "Doge Coin", "wow much coin how money so crypto plz mine v rich very currency wow", new ItemData(Materials.Brass, 907200L)));
        ItemList.Coin_Chocolate.set(addItem(tLastID = 10, "Chocolate Coin", "Wrapped in Gold", new ItemData(Materials.Gold, OrePrefixes.foil.mMaterialAmount), new GT_FoodStat(1, 0.1F, EnumAction.eat, GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Gold, 1L), true, false, false, Potion.moveSpeed.id, 200, 1, 100)));
        ItemList.Credit_Copper.set(addItem(tLastID = 11, "Industrial Copper Credit", "0.125 Credits"));

        ItemList.Credit_Silver.set(addItem(tLastID = 13, "Industrial Silver Credit", "8 Credits"));
        ItemList.Credit_Gold.set(addItem(tLastID = 14, "Industrial Gold Credit", "64 Credits"));
        ItemList.Credit_Platinum.set(addItem(tLastID = 15, "Industrial Platinum Credit", "512 Credits"));
        ItemList.Credit_Osmium.set(addItem(tLastID = 16, "Industrial Osmium Credit", "4096 Credits"));

        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Coin_Chocolate.get(1L), new Object[]{OrePrefixes.dust.get(Materials.Cocoa), OrePrefixes.dust.get(Materials.Milk), OrePrefixes.dust.get(Materials.Sugar), OrePrefixes.foil.get(Materials.Gold)});

        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Copper.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Iron});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Iron.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Silver});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Silver.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Gold});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Gold.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Platinum});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Platinum.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Osmium});

        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Iron.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Copper, ItemList.Credit_Copper, ItemList.Credit_Copper, ItemList.Credit_Copper, ItemList.Credit_Copper, ItemList.Credit_Copper, ItemList.Credit_Copper, ItemList.Credit_Copper});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Silver.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Iron, ItemList.Credit_Iron, ItemList.Credit_Iron, ItemList.Credit_Iron, ItemList.Credit_Iron, ItemList.Credit_Iron, ItemList.Credit_Iron, ItemList.Credit_Iron});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Gold.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Silver, ItemList.Credit_Silver, ItemList.Credit_Silver, ItemList.Credit_Silver, ItemList.Credit_Silver, ItemList.Credit_Silver, ItemList.Credit_Silver, ItemList.Credit_Silver});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Platinum.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Gold, ItemList.Credit_Gold, ItemList.Credit_Gold, ItemList.Credit_Gold, ItemList.Credit_Gold, ItemList.Credit_Gold, ItemList.Credit_Gold, ItemList.Credit_Gold});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Osmium.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Platinum, ItemList.Credit_Platinum, ItemList.Credit_Platinum, ItemList.Credit_Platinum, ItemList.Credit_Platinum, ItemList.Credit_Platinum, ItemList.Credit_Platinum, ItemList.Credit_Platinum});

        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Copper.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Cupronickel});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Cupronickel.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Silver});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Silver.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Gold});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Gold.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Platinum});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Platinum.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Osmium});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Osmium.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Naquadah});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Naquadah.get(8L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Neutronium});

        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Cupronickel.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper, ItemList.Credit_Greg_Copper});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Silver.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel, ItemList.Credit_Greg_Cupronickel});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Gold.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver, ItemList.Credit_Greg_Silver});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Platinum.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold, ItemList.Credit_Greg_Gold});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Osmium.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum, ItemList.Credit_Greg_Platinum});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Naquadah.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium, ItemList.Credit_Greg_Osmium});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Credit_Greg_Neutronium.get(1L), GT_ModHandler.RecipeBits.KEEPNBT | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah, ItemList.Credit_Greg_Naquadah});

        ItemList.Component_Minecart_Wheels_Iron.set(addItem(tLastID = 100, "Iron Minecart Wheels", "To get things rolling"));
        ItemList.Component_Minecart_Wheels_Steel.set(addItem(tLastID = 101, "Steel Minecart Wheels", "To get things rolling"));

        GT_ModHandler.addCraftingRecipe(ItemList.Component_Minecart_Wheels_Iron.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" h ", "RSR", " w ", 'R', OrePrefixes.ring.get(Materials.Iron), 'S', OrePrefixes.stick.get(Materials.Iron)});
        GT_ModHandler.addCraftingRecipe(ItemList.Component_Minecart_Wheels_Steel.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" h ", "RSR", " w ", 'R', OrePrefixes.ring.get(Materials.Steel), 'S', OrePrefixes.stick.get(Materials.Steel)});

        ItemList.CompressedFireclay.set(addItem(tLastID = 110, "Compressed Fireclay", "Brick-shaped"));
        ItemList.Firebrick.set(addItem(tLastID = 111, "Firebrick", "Heat resistant"));

        ItemList.Shape_Empty.set(addItem(tLastID = 300, "Empty Shape Plate", "Raw Plate to make Molds and Extruder Shapes"));


        ItemList.Shape_Mold_Plate.set(addItem(tLastID = 301, "Mold (Plate)", "Mold for making Plates"));
        ItemList.Shape_Mold_Casing.set(addItem(tLastID = 302, "Mold (Casing)", "Mold for making Item Casings"));
        ItemList.Shape_Mold_Gear.set(addItem(tLastID = 303, "Mold (Gear)", "Mold for making Gears"));
        ItemList.Shape_Mold_Credit.set(addItem(tLastID = 304, "Mold (Coinage)", "Secure Mold for making Coins (Don't lose it!)"));
        ItemList.Shape_Mold_Bottle.set(addItem(tLastID = 305, "Mold (Bottle)", "Mold for making Bottles"));
        ItemList.Shape_Mold_Ingot.set(addItem(tLastID = 306, "Mold (Ingot)", "Mold for making Ingots"));
        ItemList.Shape_Mold_Ball.set(addItem(tLastID = 307, "Mold (Ball)", "Mold for making Balls"));
        ItemList.Shape_Mold_Block.set(addItem(tLastID = 308, "Mold (Block)", "Mold for making Blocks"));
        ItemList.Shape_Mold_Nugget.set(addItem(tLastID = 309, "Mold (Nuggets)", "Mold for making Nuggets"));
        ItemList.Shape_Mold_Bun.set(addItem(tLastID = 310, "Mold (Buns)", "Mold for shaping Buns"));
        ItemList.Shape_Mold_Bread.set(addItem(tLastID = 311, "Mold (Bread)", "Mold for shaping Breads"));
        ItemList.Shape_Mold_Baguette.set(addItem(tLastID = 312, "Mold (Baguette)", "Mold for shaping Baguettes"));
        ItemList.Shape_Mold_Cylinder.set(addItem(tLastID = 313, "Mold (Cylinder)", "Mold for shaping Cylinders"));
        ItemList.Shape_Mold_Anvil.set(addItem(tLastID = 314, "Mold (Anvil)", "Mold for shaping Anvils"));
        ItemList.Shape_Mold_Name.set(addItem(tLastID = 315, "Mold (Name)", "Mold for naming Items (rename Mold with Anvil)"));
        ItemList.Shape_Mold_Gear_Small.set(addItem(tLastID = 317, "Mold (Small Gear)", "Mold for making small Gears"));
        ItemList.Shape_Mold_Rod.set(addItem(tLastID = 318, "Mold (Rod)", "Mold for making Rods"));
        ItemList.Shape_Mold_Bolt.set(addItem(tLastID = 319, "Mold (Bolt)", "Mold for making Bolts"));
        ItemList.Shape_Mold_Round.set(addItem(tLastID = 320, "Mold (Round)", "Mold for making Rounds"));
        ItemList.Shape_Mold_Screw.set(addItem(tLastID = 321, "Mold (Screw)", "Mold for making Screws"));
        ItemList.Shape_Mold_Ring.set(addItem(tLastID = 322, "Mold (Ring)", "Mold for making Rings"));
        ItemList.Shape_Mold_Rod_Long.set(addItem(tLastID = 323, "Mold (Long Rod)", "Mold for making Long Rods"));
        ItemList.Shape_Mold_Rotor.set(addItem(tLastID = 324, "Mold (Rotor)", "Mold for making a Rotor"));
        ItemList.Shape_Mold_Turbine_Blade.set(addItem(tLastID = 325, "Mold (Turbine Blade)", "Mold for making a Turbine Blade"));


        GT_ModHandler.removeRecipe(new ItemStack(Blocks.glass), null, new ItemStack(Blocks.glass), null, new ItemStack(Blocks.glass));

        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Empty.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"hf", "PP", "PP", 'P', "plateSteel"});

        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Credit.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"h  ", aTextShape, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Plate.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" h ", aTextShape, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Casing.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  h", aTextShape, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Gear.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, " Ph", aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Bottle.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextShape, "  h", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Ingot.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextShape, " h ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Ball.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextShape, "h  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Block.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, "hP ", aTextEmptyRow, 'P', ItemList.Shape_Empty});

        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Nugget.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P h", aTextEmptyRow, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Bun.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P  ", "  h", aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Bread.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P  ", aTextEmptyRow, "  h", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Baguette.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P  ", aTextEmptyRow, " h ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Cylinder.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", aTextEmptyRow, "  h", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Anvil.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", aTextEmptyRow, " h ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Name.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", aTextEmptyRow, "h  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Gear_Small.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextEmptyRow, "h P", 'P', ItemList.Shape_Empty});

        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Rod.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" h ", " P ", " F ", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Bolt.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" F", "hP", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Round.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"h ", "FP", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Screw.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"h P", "  F", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Ring.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  F", "h P", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Rod_Long.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"hPF", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Rotor.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"h  ", " P ", "  F", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Mold_Turbine_Blade.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"FPh", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});

        ItemList.Shape_Extruder_Plate.set(addItem(tLastID = 350, "Extruder Shape (Plate)", "Extruder Shape for making Plates"));
        ItemList.Shape_Extruder_Rod.set(addItem(tLastID = 351, "Extruder Shape (Rod)", "Extruder Shape for making Rods"));
        ItemList.Shape_Extruder_Bolt.set(addItem(tLastID = 352, "Extruder Shape (Bolt)", "Extruder Shape for making Bolts"));
        ItemList.Shape_Extruder_Ring.set(addItem(tLastID = 353, "Extruder Shape (Ring)", "Extruder Shape for making Rings"));
        ItemList.Shape_Extruder_Cell.set(addItem(tLastID = 354, "Extruder Shape (Cell)", "Extruder Shape for making Cells"));
        ItemList.Shape_Extruder_Ingot.set(addItem(tLastID = 355, "Extruder Shape (Ingot)", "Extruder Shape for, wait, can't we just use a Furnace?"));
        ItemList.Shape_Extruder_Wire.set(addItem(tLastID = 356, "Extruder Shape (Wire)", "Extruder Shape for making Wires"));
        ItemList.Shape_Extruder_Casing.set(addItem(tLastID = 357, "Extruder Shape (Casing)", "Extruder Shape for making Item Casings"));
        ItemList.Shape_Extruder_Pipe_Tiny.set(addItem(tLastID = 358, "Extruder Shape (Tiny Pipe)", "Extruder Shape for making tiny Pipes"));
        ItemList.Shape_Extruder_Pipe_Small.set(addItem(tLastID = 359, "Extruder Shape (Small Pipe)", "Extruder Shape for making small Pipes"));
        ItemList.Shape_Extruder_Pipe_Medium.set(addItem(tLastID = 360, "Extruder Shape (Normal Pipe)", "Extruder Shape for making Pipes"));
        ItemList.Shape_Extruder_Pipe_Large.set(addItem(tLastID = 361, "Extruder Shape (Large Pipe)", "Extruder Shape for making large Pipes"));
        ItemList.Shape_Extruder_Pipe_Huge.set(addItem(tLastID = 362, "Extruder Shape (Huge Pipe)", "Extruder Shape for making full Block Pipes"));
        ItemList.Shape_Extruder_Block.set(addItem(tLastID = 363, "Extruder Shape (Block)", "Extruder Shape for making Blocks"));
        ItemList.Shape_Extruder_Sword.set(addItem(tLastID = 364, "Extruder Shape (Sword Blade)", "Extruder Shape for making Swords"));
        ItemList.Shape_Extruder_Pickaxe.set(addItem(tLastID = 365, "Extruder Shape (Pickaxe Head)", "Extruder Shape for making Pickaxes"));
        ItemList.Shape_Extruder_Shovel.set(addItem(tLastID = 366, "Extruder Shape (Shovel Head)", "Extruder Shape for making Shovels"));
        ItemList.Shape_Extruder_Axe.set(addItem(tLastID = 367, "Extruder Shape (Axe Head)", "Extruder Shape for making Axes"));
        ItemList.Shape_Extruder_Hoe.set(addItem(tLastID = 368, "Extruder Shape (Hoe Head)", "Extruder Shape for making Hoes"));
        ItemList.Shape_Extruder_Hammer.set(addItem(tLastID = 369, "Extruder Shape (Hammer Head)", "Extruder Shape for making Hammers"));
        ItemList.Shape_Extruder_File.set(addItem(tLastID = 370, "Extruder Shape (File Head)", "Extruder Shape for making Files"));
        ItemList.Shape_Extruder_Saw.set(addItem(tLastID = 371, "Extruder Shape (Saw Blade)", "Extruder Shape for making Saws"));
        ItemList.Shape_Extruder_Gear.set(addItem(tLastID = 372, "Extruder Shape (Gear)", "Extruder Shape for making Gears"));
        ItemList.Shape_Extruder_Bottle.set(addItem(tLastID = 373, "Extruder Shape (Bottle)", "Extruder Shape for making Bottles"));
        ItemList.Shape_Extruder_Gear_Small.set(addItem(tLastID = 374, "Extruder Shape (Small Gear)", "Extruder Shape for making Small Gears"));
        ItemList.Shape_Extruder_Rotor.set(addItem(tLastID = 375, "Extruder Shape (Rotor)", "Extruder Shape for a Rotor"));
        ItemList.Shape_Extruder_Turbine_Blade.set(addItem(tLastID = 376, "Extruder Shape (Turbine Blade)", "Extruder Shape for a Turbine Blade"));
        ItemList.Shape_Extruder_Shaft.set(addItem(tLastID = 377, "Extruder Shape (Shaft)", "Extruder Shape for a Shaft"));


        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Bolt.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"x  ", aTextShape, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Cell.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" x ", aTextShape, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Ingot.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  x", aTextShape, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Ring.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, " Px", aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Rod.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextShape, "  x", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Wire.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextShape, " x ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Casing.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextShape, "x  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Plate.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, "xP ", aTextEmptyRow, 'P', ItemList.Shape_Empty});

        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Block.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P x", aTextEmptyRow, aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Pipe_Small.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P  ", "  x", aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Pipe_Large.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P  ", aTextEmptyRow, "  x", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Pipe_Medium.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"P  ", aTextEmptyRow, " x ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Sword.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", aTextEmptyRow, "  x", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Pickaxe.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", aTextEmptyRow, " x ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Shovel.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", aTextEmptyRow, "x  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Axe.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  P", "x  ", aTextEmptyRow, 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Hoe.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, aTextEmptyRow, "x P", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Hammer.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, "x  ", "  P", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_File.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"x  ", aTextEmptyRow, "  P", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Saw.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" x ", aTextEmptyRow, "  P", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Gear.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"x  ", aTextEmptyRow, "P  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Gear_Small.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, "x  ", "P  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Pipe_Tiny.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" x ", aTextEmptyRow, "P  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Pipe_Huge.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"  x", aTextEmptyRow, "P  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Bottle.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{aTextEmptyRow, "  x", "P  ", 'P', ItemList.Shape_Empty});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Rotor.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"x", "P", "F", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Turbine_Blade.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"FPx", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Extruder_Shaft.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"xPF", 'P', ItemList.Shape_Empty, 'F', ToolDictNames.craftingToolFile});


        ItemList.Shape_Slicer_Flat.set(addItem(tLastID = 398, "Slicer Blade (Flat)", "Slicer Blade for cutting Flat"));
        ItemList.Shape_Slicer_Stripes.set(addItem(tLastID = 399, "Slicer Blade (Stripes)", "Slicer Blade for cutting Stripes"));

        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Slicer_Flat.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"hXS", aTextShape, "fXd", 'P', ItemList.Shape_Extruder_Block, 'X', OrePrefixes.plate.get(Materials.StainlessSteel), 'S', OrePrefixes.screw.get(Materials.StainlessSteel)});
        GT_ModHandler.addCraftingRecipe(ItemList.Shape_Slicer_Stripes.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"hXS", "XPX", "fXd", 'P', ItemList.Shape_Extruder_Block, 'X', OrePrefixes.plate.get(Materials.StainlessSteel), 'S', OrePrefixes.screw.get(Materials.StainlessSteel)});

        ItemList.Fuel_Can_Plastic_Empty.set(addItem(tLastID = 400, "Empty Plastic Fuel Can", "Used to store Fuels", new ItemData(Materials.Plastic, OrePrefixes.plate.mMaterialAmount * 1L)));
        ItemList.Fuel_Can_Plastic_Filled.set(addItem(tLastID = 401, "Plastic Fuel Can", "Burns well in Diesel Generators", new ItemData(Materials.Plastic, OrePrefixes.plate.mMaterialAmount * 1L)));

        GT_ModHandler.addCraftingRecipe(ItemList.Fuel_Can_Plastic_Empty.get(7L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{" PP", "P P", "PPP", 'P', OrePrefixes.plate.get(Materials.Plastic)});

        ItemList.Spray_Empty.set(addItem(tLastID = 402, "Empty Spray Can", "Used for making Sprays", new ItemData(Materials.Tin, OrePrefixes.plate.mMaterialAmount * 2L, Materials.Redstone, OrePrefixes.dust.mMaterialAmount)));

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1L), GT_OreDictUnificator.get(OrePrefixes.cell, Materials.Empty, 1L), ItemList.Spray_Empty.get(1L), 800, 1);

        ItemList.Crate_Empty.set(addItem(tLastID = 403, "Empty Crate", "To Package lots of Material", new ItemData(Materials.Wood, 3628800L, Materials.Iron, OrePrefixes.screw.mMaterialAmount)));

        GT_ModHandler.addCraftingRecipe(ItemList.Crate_Empty.get(4L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"SWS", "WdW", "SWS", 'W', OrePrefixes.plank.get(Materials.Wood), 'S', OrePrefixes.screw.get(Materials.Iron)});
        GT_ModHandler.addCraftingRecipe(ItemList.Crate_Empty.get(4L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"SWS", "WdW", "SWS", 'W', OrePrefixes.plank.get(Materials.Wood), 'S', OrePrefixes.screw.get(Materials.Steel)});

        ItemList.ThermosCan_Empty.set(addItem(tLastID = 404, "Empty Thermos Can", "Keeping hot things hot and cold things cold", new ItemData(Materials.Aluminium, OrePrefixes.plate.mMaterialAmount * 1L + 2L * OrePrefixes.ring.mMaterialAmount)));

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Aluminium, 1L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Silver, 2L), ItemList.ThermosCan_Empty.get(1L), 800, 1);

        ItemList.Large_Fluid_Cell_Steel.set(addItem(tLastID = 405, "Large Steel Fluid Cell", "", new ItemData(Materials.Steel, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Steel, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 8000L, 64L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Steel, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Steel, 4L), ItemList.Large_Fluid_Cell_Steel.get(1L), 200, 30);

        ItemList.Large_Fluid_Cell_TungstenSteel.set(addItem(tLastID = 406, "Large Tungstensteel Fluid Cell", "", new ItemData(Materials.TungstenSteel, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.TungstenSteel, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 512000L, 32L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.TungstenSteel, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.TungstenSteel, 4L), ItemList.Large_Fluid_Cell_TungstenSteel.get(1L), 200, 480);

        ItemList.Large_Fluid_Cell_Aluminium.set(addItem(tLastID = 407, "Large Aluminium Fluid Cell", "", new ItemData(Materials.Aluminium, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Aluminium, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 32000L, 64L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Aluminium, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Aluminium, 4L), ItemList.Large_Fluid_Cell_Aluminium.get(1L), 200, 64);

        ItemList.Large_Fluid_Cell_StainlessSteel.set(addItem(tLastID = 408, "Large StainlessSteel Fluid Cell", "", new ItemData(Materials.StainlessSteel, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.StainlessSteel, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 64000L, 64L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.StainlessSteel, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.StainlessSteel, 4L), ItemList.Large_Fluid_Cell_StainlessSteel.get(1L), 200, 120);

        ItemList.Large_Fluid_Cell_Titanium.set(addItem(tLastID = 409, "Large Titanium Fluid Cell", "", new ItemData(Materials.Titanium, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Titanium, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 128000L, 64L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Titanium, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Titanium, 4L), ItemList.Large_Fluid_Cell_Titanium.get(1L), 200, 256);

        ItemList.Large_Fluid_Cell_Chrome.set(addItem(tLastID = 410, "Large Chrome Fluid Cell", "", new ItemData(Materials.Chrome, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Chrome, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 2048000L, 8L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Chrome, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Chrome, 4L), ItemList.Large_Fluid_Cell_Chrome.get(1L), 200, 1024);

        ItemList.Large_Fluid_Cell_Iridium.set(addItem(tLastID = 411, "Large Iridium Fluid Cell", "", new ItemData(Materials.Iridium, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Iridium, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 8192000L, 2L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Iridium, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Iridium, 4L), ItemList.Large_Fluid_Cell_Iridium.get(1L), 200, 1920);

        ItemList.Large_Fluid_Cell_Osmium.set(addItem(tLastID = 412, "Large Osmium Fluid Cell", "", new ItemData(Materials.Osmium, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Osmium, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 32768000L, 1L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Osmium, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Osmium, 4L), ItemList.Large_Fluid_Cell_Osmium.get(1L), 200, 4096);

        ItemList.Large_Fluid_Cell_Neutronium.set(addItem(tLastID = 413, "Large Neutronium Fluid Cell", "", new ItemData(Materials.Neutronium, OrePrefixes.plateDouble.mMaterialAmount * 4L, new MaterialStack(Materials.Neutronium, OrePrefixes.ring.mMaterialAmount * 4L))));
        setFluidContainerStats(32000 + tLastID, 131072000L, 1L);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Neutronium, 4L), GT_OreDictUnificator.get(OrePrefixes.ring, Materials.Neutronium, 4L), ItemList.Large_Fluid_Cell_Neutronium.get(1L), 400, 16384);

        for (int i = 0; i < 16; i++) {
            ItemList.SPRAY_CAN_DYES[i].set(addItem(tLastID = 430 + 2 * i, "Spray Can (" + Dyes.get(i).mName + ")", "Full"));
            ItemList.SPRAY_CAN_DYES_USED[i].set(addItem(tLastID + 1, "Spray Can (" + Dyes.get(i).mName + ")", "Used", SubTag.INVISIBLE));
            IItemBehaviour<GT_MetaBase_Item> tBehaviour = new Behaviour_Spray_Color(ItemList.Spray_Empty.get(1L), ItemList.SPRAY_CAN_DYES_USED[i].get(1L), ItemList.SPRAY_CAN_DYES[i].get(1L), 512L, i);
            addItemBehavior(32000 + tLastID, tBehaviour);
            addItemBehavior(32001 + tLastID, tBehaviour);
        }
        ItemList.Tool_Matches.set(addItem(tLastID = 471, "Match", ""));
        ItemList.Tool_MatchBox_Used.set(addItem(tLastID = 472, "Match Box", "This is not a Car", SubTag.INVISIBLE));
        ItemList.Tool_MatchBox_Full.set(addItem(tLastID = 473, "Match Box (Full)", "This is not a Car"));

        IItemBehaviour<GT_MetaBase_Item> tBehaviour = new Behaviour_Lighter(null, ItemList.Tool_Matches.get(1L), ItemList.Tool_Matches.get(1L), 1L);
        addItemBehavior(32471, tBehaviour);
        tBehaviour = new Behaviour_Lighter(null, ItemList.Tool_MatchBox_Used.get(1L), ItemList.Tool_MatchBox_Full.get(1L), 16L);
        addItemBehavior(32472, tBehaviour);
        addItemBehavior(32473, tBehaviour);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Wood, 1L), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.Phosphor, 1L), ItemList.Tool_Matches.get(1L), 16, 16);
        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Wood, 1L), GT_OreDictUnificator.get(OrePrefixes.dustSmall, Materials.Phosphorus, 1L), ItemList.Tool_Matches.get(1L), 16, 16);
        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Wood, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphor, 1L), ItemList.Tool_Matches.get(4L), 64, 16);
        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.bolt, Materials.Wood, 4L), GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 1L), ItemList.Tool_Matches.get(4L), 64, 16);
        GT_Values.RA.addBoxingRecipe(ItemList.Tool_Matches.get(16L), GT_OreDictUnificator.get(OrePrefixes.plateDouble, Materials.Paper, 1L), ItemList.Tool_MatchBox_Full.get(1L), 64, 16);
        GT_Values.RA.addUnboxingRecipe(ItemList.Tool_MatchBox_Full.get(1L), ItemList.Tool_Matches.get(16L), null, 32, 16);

        ItemList.Tool_Lighter_Invar_Empty.set(addItem(tLastID = 474, "Lighter (Empty)", "", new ItemData(Materials.Invar, OrePrefixes.plate.mMaterialAmount * 2L)));
        ItemList.Tool_Lighter_Invar_Used.set(addItem(tLastID = 475, "Lighter", "", new ItemData(Materials.Invar, OrePrefixes.plate.mMaterialAmount * 2L), SubTag.INVISIBLE));
        ItemList.Tool_Lighter_Invar_Full.set(addItem(tLastID = 476, "Lighter (Full)", "", new ItemData(Materials.Invar, OrePrefixes.plate.mMaterialAmount * 2L)));

        tBehaviour = new Behaviour_Lighter(ItemList.Tool_Lighter_Invar_Empty.get(1L), ItemList.Tool_Lighter_Invar_Used.get(1L), ItemList.Tool_Lighter_Invar_Full.get(1L), 100L);
        addItemBehavior(32475, tBehaviour);
        addItemBehavior(32476, tBehaviour);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Invar, 2L), new ItemStack(Items.flint, 1), ItemList.Tool_Lighter_Invar_Empty.get(1L), 256, 16);

        ItemList.Tool_Lighter_Platinum_Empty.set(addItem(tLastID = 477, "Platinum Lighter (Empty)", "A known Prank Master is engraved on it", new ItemData(Materials.Platinum, OrePrefixes.plate.mMaterialAmount * 2L)));
        ItemList.Tool_Lighter_Platinum_Used.set(addItem(tLastID = 478, "Platinum Lighter", "A known Prank Master is engraved on it", new ItemData(Materials.Platinum, OrePrefixes.plate.mMaterialAmount * 2L), SubTag.INVISIBLE));
        ItemList.Tool_Lighter_Platinum_Full.set(addItem(tLastID = 479, "Platinum Lighter (Full)", "A known Prank Master is engraved on it", new ItemData(Materials.Platinum, OrePrefixes.plate.mMaterialAmount * 2L)));

        tBehaviour = new Behaviour_Lighter(ItemList.Tool_Lighter_Platinum_Empty.get(1L), ItemList.Tool_Lighter_Platinum_Used.get(1L), ItemList.Tool_Lighter_Platinum_Full.get(1L), 1000L);
        addItemBehavior(32478, tBehaviour);
        addItemBehavior(32479, tBehaviour);

        GT_Values.RA.addAssemblerRecipe(GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Platinum, 2L), new ItemStack(Items.flint, 1), ItemList.Tool_Lighter_Platinum_Empty.get(1L), 256, 256);

        ItemList.Ingot_Heavy1.set(addItem(tLastID = 462, "Heavy Duty Alloy Ingot", "Used to make Heavy Duty Plates"));
        ItemList.Ingot_Heavy2.set(addItem(tLastID = 463, "Heavy Duty Alloy Ingot T2", "Used to make Heavy Duty Plates T2"));
        ItemList.Ingot_Heavy3.set(addItem(tLastID = 464, "Heavy Duty Alloy Ingot T3", "Used to make Heavy Duty Plates T3"));
        
        ItemList.Ingot_IridiumAlloy.set(addItem(tLastID = 480, "Iridium Alloy Ingot", "Used to make Iridium Plates"));

        ItemList.Paper_Printed_Pages.set(addItem(tLastID = 481, "Printed Pages", "Used to make written Books", new ItemData(Materials.Paper, 10886400L), new Behaviour_PrintedPages()));
        ItemList.Paper_Magic_Empty.set(addItem(tLastID = 482, "Magic Paper", "", SubTag.INVISIBLE, new ItemData(Materials.Paper, 3628800L)));
        ItemList.Paper_Magic_Page.set(addItem(tLastID = 483, "Enchanted Page", "", SubTag.INVISIBLE, new ItemData(Materials.Paper, 3628800L)));
        ItemList.Paper_Magic_Pages.set(addItem(tLastID = 484, "Enchanted Pages", "", SubTag.INVISIBLE, new ItemData(Materials.Paper, 10886400L)));
        ItemList.Paper_Punch_Card_Empty.set(addItem(tLastID = 485, "Punch Card", "", SubTag.INVISIBLE, new ItemData(Materials.Paper, 7257600L)));
        ItemList.Paper_Punch_Card_Encoded.set(addItem(tLastID = 486, "Punched Card", "", SubTag.INVISIBLE, new ItemData(Materials.Paper, 7257600L)));
        ItemList.Book_Written_01.set(addItem(tLastID = 487, "Book", "", new ItemData(Materials.Paper, 10886400L), "bookWritten", OreDictNames.craftingBook, new Behaviour_WrittenBook()));
        ItemList.Book_Written_02.set(addItem(tLastID = 488, "Book", "", new ItemData(Materials.Paper, 10886400L), "bookWritten", OreDictNames.craftingBook, new Behaviour_WrittenBook()));
        ItemList.Book_Written_03.set(addItem(tLastID = 489, "Book", "", new ItemData(Materials.Paper, 10886400L), "bookWritten", OreDictNames.craftingBook, new Behaviour_WrittenBook()));

        ItemList.Schematic.set(addItem(tLastID = 490, "Schematic", "EMPTY", new ItemData(Materials.StainlessSteel, 7257600L)));
        ItemList.Schematic_Crafting.set(addItem(tLastID = 491, "Schematic (Crafting)", "Crafts the Programmed Recipe", new ItemData(Materials.StainlessSteel, 7257600L)));
        ItemList.Schematic_1by1.set(addItem(tLastID = 495, "Schematic (1x1)", "Crafts 1 Items as 1x1 (use in Packager)", new ItemData(Materials.StainlessSteel, 7257600L)));
        ItemList.Schematic_2by2.set(addItem(tLastID = 496, "Schematic (2x2)", "Crafts 4 Items as 2x2 (use in Packager)", new ItemData(Materials.StainlessSteel, 7257600L)));
        ItemList.Schematic_3by3.set(addItem(tLastID = 497, "Schematic (3x3)", "Crafts 9 Items as 3x3 (use in Packager)", new ItemData(Materials.StainlessSteel, 7257600L)));
        ItemList.Schematic_Dust.set(addItem(tLastID = 498, "Schematic (Dusts)", "Combines Dusts (use in Packager)", new ItemData(Materials.StainlessSteel, 7257600L)));

        GT_ModHandler.addCraftingRecipe(ItemList.Schematic_1by1.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"d  ", aTextShape, aTextEmptyRow, 'P', ItemList.Schematic});
        GT_ModHandler.addCraftingRecipe(ItemList.Schematic_2by2.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{" d ", aTextShape, aTextEmptyRow, 'P', ItemList.Schematic});
        GT_ModHandler.addCraftingRecipe(ItemList.Schematic_3by3.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"  d", aTextShape, aTextEmptyRow, 'P', ItemList.Schematic});
        GT_ModHandler.addCraftingRecipe(ItemList.Schematic_Dust.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{aTextEmptyRow, aTextShape, "  d", 'P', ItemList.Schematic});

        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Schematic.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Schematic_Crafting});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Schematic.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Schematic_1by1});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Schematic.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Schematic_2by2});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Schematic.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Schematic_3by3});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Schematic.get(1L), GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Schematic_Dust});

        ItemList.Battery_Hull_LV.set(addItem(tLastID = 500, "Small Battery Hull", "An empty LV Battery Hull", new ItemData(Materials.BatteryAlloy, OrePrefixes.plate.mMaterialAmount * 1L)));
        ItemList.Battery_Hull_MV.set(addItem(tLastID = 501, "Medium Battery Hull", "An empty MV Battery Hull", new ItemData(Materials.BatteryAlloy, OrePrefixes.plate.mMaterialAmount * 3L)));
        ItemList.Battery_Hull_HV.set(addItem(tLastID = 502, "Large Battery Hull", "An empty HV Battery Hull", new ItemData(Materials.BatteryAlloy, OrePrefixes.plate.mMaterialAmount * 9L)));

        GT_ModHandler.addCraftingRecipe(ItemList.Battery_Hull_LV.get(1L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"C", "P", "P", 'P', OrePrefixes.plate.get(Materials.BatteryAlloy), 'C', OreDictNames.craftingWireTin});
        GT_ModHandler.addCraftingRecipe(ItemList.Battery_Hull_MV.get(1L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{"C C", "PPP", "PPP", 'P', OrePrefixes.plate.get(Materials.BatteryAlloy), 'C', OreDictNames.craftingWireCopper});

        ItemList.Battery_RE_ULV_Tantalum.set(addItem(tLastID = 499, "Tantalum Capacitor", "Reusable"));
        setElectricStats(32000 + tLastID, 1000L, GT_Values.V[0], 0L, -3L, false);

        ItemList.Battery_SU_LV_SulfuricAcid.set(addItem(tLastID = 510, "Small Acid Battery", "Single Use"));
        setElectricStats(32000 + tLastID, 18000L, GT_Values.V[1], 1L, -2L, true);
        ItemList.Battery_SU_LV_Mercury.set(addItem(tLastID = 511, "Small Mercury Battery", "Single Use"));
        setElectricStats(32000 + tLastID, 32000L, GT_Values.V[1], 1L, -2L, true);

        ItemList.Battery_RE_LV_Cadmium.set(addItem(tLastID = 517, "Small Cadmium Battery", "Reusable", "calclavia:ADVANCED_BATTERY"));
        setElectricStats(32000 + tLastID, 75000L, GT_Values.V[1], 1L, -3L, true);
        ItemList.Battery_RE_LV_Lithium.set(addItem(tLastID = 518, "Small Lithium Battery", "Reusable", "calclavia:ADVANCED_BATTERY"));
        setElectricStats(32000 + tLastID, 100000L, GT_Values.V[1], 1L, -3L, true);
        ItemList.Battery_RE_LV_Sodium.set(addItem(tLastID = 519, "Small Sodium Battery", "Reusable", "calclavia:ADVANCED_BATTERY"));
        setElectricStats(32000 + tLastID, 50000L, GT_Values.V[1], 1L, -3L, true);

        ItemList.Battery_SU_MV_SulfuricAcid.set(addItem(tLastID = 520, "Medium Acid Battery", "Single Use"));
        setElectricStats(32000 + tLastID, 72000L, GT_Values.V[2], 2L, -2L, true);
        ItemList.Battery_SU_MV_Mercury.set(addItem(tLastID = 521, "Medium Mercury Battery", "Single Use"));
        setElectricStats(32000 + tLastID, 128000L, GT_Values.V[2], 2L, -2L, true);

        ItemList.Battery_RE_MV_Cadmium.set(addItem(tLastID = 527, "Medium Cadmium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 300000L, GT_Values.V[2], 2L, -3L, true);
        ItemList.Battery_RE_MV_Lithium.set(addItem(tLastID = 528, "Medium Lithium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 400000L, GT_Values.V[2], 2L, -3L, true);
        ItemList.Battery_RE_MV_Sodium.set(addItem(tLastID = 529, "Medium Sodium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 200000L, GT_Values.V[2], 2L, -3L, true);

        ItemList.Battery_SU_HV_SulfuricAcid.set(addItem(tLastID = 530, "Large Acid Battery", "Single Use"));
        setElectricStats(32000 + tLastID, 288000L, GT_Values.V[3], 3L, -2L, true);
        ItemList.Battery_SU_HV_Mercury.set(addItem(tLastID = 531, "Large Mercury Battery", "Single Use"));
        setElectricStats(32000 + tLastID, 512000L, GT_Values.V[3], 3L, -2L, true);

        ItemList.Battery_RE_HV_Cadmium.set(addItem(tLastID = 537, "Large Cadmium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 1200000L, GT_Values.V[3], 3L, -3L, true);
        ItemList.Battery_RE_HV_Lithium.set(addItem(tLastID = 538, "Large Lithium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 1600000L, GT_Values.V[3], 3L, -3L, true);
        ItemList.Battery_RE_HV_Sodium.set(addItem(tLastID = 539, "Large Sodium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 800000L, GT_Values.V[3], 3L, -3L, true);

        GT_ModHandler.addExtractionRecipe(ItemList.Battery_SU_LV_SulfuricAcid.get(1L), ItemList.Battery_Hull_LV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_SU_LV_Mercury.get(1L), ItemList.Battery_Hull_LV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_SU_MV_SulfuricAcid.get(1L), ItemList.Battery_Hull_MV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_SU_MV_Mercury.get(1L), ItemList.Battery_Hull_MV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_SU_HV_SulfuricAcid.get(1L), ItemList.Battery_Hull_HV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_SU_HV_Mercury.get(1L), ItemList.Battery_Hull_HV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_LV_Cadmium.get(1L), ItemList.Battery_Hull_LV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_LV_Lithium.get(1L), ItemList.Battery_Hull_LV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_LV_Sodium.get(1L), ItemList.Battery_Hull_LV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_MV_Cadmium.get(1L), ItemList.Battery_Hull_MV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_MV_Lithium.get(1L), ItemList.Battery_Hull_MV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_MV_Sodium.get(1L), ItemList.Battery_Hull_MV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_HV_Cadmium.get(1L), ItemList.Battery_Hull_HV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_HV_Lithium.get(1L), ItemList.Battery_Hull_HV.get(1L));
        GT_ModHandler.addExtractionRecipe(ItemList.Battery_RE_HV_Sodium.get(1L), ItemList.Battery_Hull_HV.get(1L));

        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cadmium, 2L), ItemList.Battery_Hull_LV.get(1L), ItemList.Battery_RE_LV_Cadmium.get(1L), null, 100, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 2L), ItemList.Battery_Hull_LV.get(1L), ItemList.Battery_RE_LV_Lithium.get(1L), null, 100, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 2L), ItemList.Battery_Hull_LV.get(1L), ItemList.Battery_RE_LV_Sodium.get(1L), null, 100, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cadmium, 8L), ItemList.Battery_Hull_MV.get(1L), ItemList.Battery_RE_MV_Cadmium.get(1L), null, 400, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 8L), ItemList.Battery_Hull_MV.get(1L), ItemList.Battery_RE_MV_Lithium.get(1L), null, 400, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 8L), ItemList.Battery_Hull_MV.get(1L), ItemList.Battery_RE_MV_Sodium.get(1L), null, 400, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Cadmium, 32L), ItemList.Battery_Hull_HV.get(1L), ItemList.Battery_RE_HV_Cadmium.get(1L), null, 1600, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Lithium, 32L), ItemList.Battery_Hull_HV.get(1L), ItemList.Battery_RE_HV_Lithium.get(1L), null, 1600, 2);
        GT_Values.RA.addCannerRecipe(GT_OreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 32L), ItemList.Battery_Hull_HV.get(1L), ItemList.Battery_RE_HV_Sodium.get(1L), null, 1600, 2);

        ItemList.Energy_LapotronicOrb.set(addItem(tLastID = 597, "Lapotronic Energy Orb", "Reusable battery", OrePrefixes.battery.get(Materials.Ultimate)));
        setElectricStats(32000 + tLastID, 250000000L, GT_Values.V[5], 5L, -3L, true);

        ItemList.ZPM.set(addItem(tLastID = 598, "Zero Point Module", "Single use battery"));
        setElectricStats(32000 + tLastID, 2000000000000L, GT_Values.V[7], 7L, -2L, true);

        ItemList.Energy_LapotronicOrb2.set(addItem(tLastID = 599, "Lapotronic Energy Orb Cluster", "Reusable battery", OrePrefixes.battery.get(Materials.Ultimate)));
        setElectricStats(32000 + tLastID, 1000000000L, GT_Values.V[6], 6L, -3L, true);


        ItemList.Energy_Module.set(addItem(tLastID = 736, "Energy Module", "Reusable battery", OrePrefixes.battery.get(Materials.Ultimate)));
        setElectricStats(32000 + tLastID, 10000000000L, GT_Values.V[7], 7L, -3L, true);

        ItemList.Energy_Cluster.set(addItem(tLastID = 737, "Energy Cluster", "Reusable battery"));
        setElectricStats(32000 + tLastID, 100000000000L, GT_Values.V[8], 8L, -3L, true);

        ItemList.Electric_Motor_LV.set(addItem(tLastID = 600, "LV Electric Motor", "LV Motor"));
        ItemList.Electric_Motor_MV.set(addItem(tLastID = 601, "MV Electric Motor", "MV Motor"));
        ItemList.Electric_Motor_HV.set(addItem(tLastID = 602, "HV Electric Motor", "HV Motor"));
        ItemList.Electric_Motor_EV.set(addItem(tLastID = 603, "EV Electric Motor", "EV Motor"));
        ItemList.Electric_Motor_IV.set(addItem(tLastID = 604, "IV Electric Motor", "IV Motor"));
        ItemList.Electric_Motor_LuV.set(addItem(tLastID = 606, "LuV Electric Motor", "LuV Motor"));
        ItemList.Electric_Motor_ZPM.set(addItem(tLastID = 607, "ZPM Electric Motor", "ZPM Motor"));
        ItemList.Electric_Motor_UV.set(addItem(tLastID = 608, "UV Electric Motor", "UV Motor"));
        ItemList.Electric_Motor_UHV.set(addItem(tLastID = 609, "UHV Electric Motor", "UHV Motor"));
        ItemList.Electric_Motor_UEV.set(addItem(tLastID = 624, "UEV Electric Motor", "UEV Motor"));


        ItemList.Electric_Pump_LV.set(addItem(tLastID = 610, "LV Electric Pump", "2560 L/sec (as Cover)"));
        ItemList.Electric_Pump_MV.set(addItem(tLastID = 611, "MV Electric Pump", "10240 L/sec (as Cover)"));
        ItemList.Electric_Pump_HV.set(addItem(tLastID = 612, "HV Electric Pump", "40.960 L/sec (as Cover)"));
        ItemList.Electric_Pump_EV.set(addItem(tLastID = 613, "EV Electric Pump", "163.840 L/sec (as Cover)"));
        ItemList.Electric_Pump_IV.set(addItem(tLastID = 614, "IV Electric Pump", "655.360 L/sec (as Cover)"));
        ItemList.Electric_Pump_LuV.set(addItem(tLastID = 620, "LuV Electric Pump", "2.621.440 L/sec (as Cover)"));
        ItemList.Electric_Pump_ZPM.set(addItem(tLastID = 621, "ZPM Electric Pump", "10.485.760 L/sec (as Cover)"));
        ItemList.Electric_Pump_UV.set(addItem(tLastID = 622, "UV Electric Pump", "41.943.040 L/sec (as Cover)"));
        ItemList.Electric_Pump_UHV.set(addItem(tLastID = 623, "UHV Electric Pump", "(not Cover)"));
        ItemList.Electric_Pump_UEV.set(addItem(tLastID = 625, "UEV Electric Pump", "(not Cover)"));

        GregTech_API.registerCover(ItemList.Electric_Pump_LV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(128));
        GregTech_API.registerCover(ItemList.Electric_Pump_MV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(512));
        GregTech_API.registerCover(ItemList.Electric_Pump_HV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[3][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(2048));
        GregTech_API.registerCover(ItemList.Electric_Pump_EV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[4][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(8192));
        GregTech_API.registerCover(ItemList.Electric_Pump_IV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[5][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(32768));
        GregTech_API.registerCover(ItemList.Electric_Pump_LuV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[6][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(131072));
        GregTech_API.registerCover(ItemList.Electric_Pump_ZPM.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[7][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(524288));
        GregTech_API.registerCover(ItemList.Electric_Pump_UV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[8][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_Pump(2097152));

        ItemList.FluidRegulator_LV.set(addItem(tLastID = 615, "LV Fluid Regulator", "Configuable up to 640 L/sec (as Cover)/n Rightclick/Screwdriver-rightclick/Shift-screwdriver-rightclick/n to adjust the pump speed by 1/16/256 L/sec per click"));
        ItemList.FluidRegulator_MV.set(addItem(tLastID = 616, "MV Fluid Regulator", "Configuable up to 2.560 L/sec (as Cover)/n Rightclick/Screwdriver-rightclick/Shift-screwdriver-rightclick/n to adjust the pump speed by 1/16/256 L/sec per click"));
        ItemList.FluidRegulator_HV.set(addItem(tLastID = 617, "HV Fluid Regulator", "Configuable up to 10.240 L/sec (as Cover)/n Rightclick/Screwdriver-rightclick/Shift-screwdriver-rightclick/n to adjust the pump speed by 1/16/256 L/sec per click"));
        ItemList.FluidRegulator_EV.set(addItem(tLastID = 618, "EV Fluid Regulator", "Configuable up to 40.960 L/sec (as Cover)/n Rightclick/Screwdriver-rightclick/Shift-screwdriver-rightclick/n to adjust the pump speed by 1/16/256 L/sec per click"));
        ItemList.FluidRegulator_IV.set(addItem(tLastID = 619, "IV Fluid Regulator", "Configuable up to 163.840 L/sec (as Cover)/n Rightclick/Screwdriver-rightclick/Shift-screwdriver-rightclick/n to adjust the pump speed by 1/16/256 L/sec per click"));

        GregTech_API.registerCover(ItemList.FluidRegulator_LV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(32));
        GregTech_API.registerCover(ItemList.FluidRegulator_MV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(128));
        GregTech_API.registerCover(ItemList.FluidRegulator_HV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[3][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(512));
        GregTech_API.registerCover(ItemList.FluidRegulator_EV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[4][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(2048));
        GregTech_API.registerCover(ItemList.FluidRegulator_IV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[5][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(8192));

        ItemList.FluidFilter.set(addItem(tLastID = 635, "Fluid Filter", "Set with Fluid Container to only accept one Fluid Type"));
        GregTech_API.registerCover(ItemList.FluidFilter.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_SHUTTER)), new GT_Cover_Fluidfilter());

        /**ItemList.Rotor_LV.set(addItem(tLastID = 620, "Tin Rotor", "", new Object[] { OrePrefixes.rotor.get(Materials.Tin), new TC_Aspects.TC_AspectStack(TC_Aspects.METALLUM, 1L), new TC_Aspects.TC_AspectStack(TC_Aspects.MOTUS, 1L) }));
         ItemList.Rotor_MV.set(addItem(tLastID = 621, "Bronze Rotor", "", new Object[] { OrePrefixes.rotor.get(Materials.Bronze), new TC_Aspects.TC_AspectStack(TC_Aspects.METALLUM, 2L), new TC_Aspects.TC_AspectStack(TC_Aspects.MOTUS, 2L) }));
         ItemList.Rotor_HV.set(addItem(tLastID = 622, "Steel Rotor", "", new Object[] { OrePrefixes.rotor.get(Materials.Steel), new TC_Aspects.TC_AspectStack(TC_Aspects.METALLUM, 4L), new TC_Aspects.TC_AspectStack(TC_Aspects.MOTUS, 4L) }));
         ItemList.Rotor_EV.set(addItem(tLastID = 623, "Stainless Steel Rotor", "", new Object[] { OrePrefixes.rotor.get(Materials.StainlessSteel), new TC_Aspects.TC_AspectStack(TC_Aspects.METALLUM, 8L), new TC_Aspects.TC_AspectStack(TC_Aspects.MOTUS, 8L) }));
         ItemList.Rotor_IV.set(addItem(tLastID = 624, "Tungstensteel Rotor", "", new Object[] { OrePrefixes.rotor.get(Materials.TungstenSteel), new TC_Aspects.TC_AspectStack(TC_Aspects.METALLUM, 16L), new TC_Aspects.TC_AspectStack(TC_Aspects.MOTUS, 16L) }));
         ItemList.Rotor_LuV.set(ItemList.Rotor_IV.get(1L));
         ItemList.Rotor_ZPM.set(ItemList.Rotor_LuV.get(1L));
         ItemList.Rotor_UV.set(ItemList.Rotor_ZPM.get(1L));**/

        ItemList.Conveyor_Module_LV.set(addItem(tLastID = 630, "LV Conveyor Module", "1 Stack every 20 secs (as Cover)"));
        ItemList.Conveyor_Module_MV.set(addItem(tLastID = 631, "MV Conveyor Module", "1 Stack every 5 secs (as Cover)"));
        ItemList.Conveyor_Module_HV.set(addItem(tLastID = 632, "HV Conveyor Module", "1 Stack every 1 sec (as Cover)"));
        ItemList.Conveyor_Module_EV.set(addItem(tLastID = 633, "EV Conveyor Module", "1 Stack every 1/5 sec (as Cover)"));
        ItemList.Conveyor_Module_IV.set(addItem(tLastID = 634, "IV Conveyor Module", "1 Stack every 1/20 sec (as Cover)"));
        ItemList.Conveyor_Module_LuV.set(addItem(tLastID = 636, "LuV Conveyor Module", "2 Stack every 1/20 sec (as Cover)"));
        ItemList.Conveyor_Module_ZPM.set(addItem(tLastID = 637, "ZPM Conveyor Module", "3 Stack every 1/20 sec (as Cover)"));
        ItemList.Conveyor_Module_UV.set(addItem(tLastID = 638, "UV Conveyor Module", "4 Stack every 1/20 sec (as Cover)"));
        ItemList.Conveyor_Module_UHV.set(addItem(tLastID = 639, "UHV Conveyor Module", "5 Stack every 1/20 sec (as Cover)"));
        ItemList.Conveyor_Module_UEV.set(addItem(tLastID = 626, "UEV Conveyor Module", "6 Stack every 1/20 sec (as Cover)"));

        GregTech_API.registerCover(ItemList.Conveyor_Module_LV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(400));
        GregTech_API.registerCover(ItemList.Conveyor_Module_MV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(100));
        GregTech_API.registerCover(ItemList.Conveyor_Module_HV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[3][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(20));
        GregTech_API.registerCover(ItemList.Conveyor_Module_EV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[4][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(4));
        GregTech_API.registerCover(ItemList.Conveyor_Module_IV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[5][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(1));
        GregTech_API.registerCover(ItemList.Conveyor_Module_LuV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[6][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(1, (byte) 2));
        GregTech_API.registerCover(ItemList.Conveyor_Module_ZPM.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[7][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(1, (byte) 3));
        GregTech_API.registerCover(ItemList.Conveyor_Module_UV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[8][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(1, (byte) 4));
        GregTech_API.registerCover(ItemList.Conveyor_Module_UHV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[9][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(1, (byte) 5));
        GregTech_API.registerCover(ItemList.Conveyor_Module_UEV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[9][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONVEYOR)), new GT_Cover_Conveyor(1, (byte) 6));

        ItemList.Electric_Piston_LV.set(addItem(tLastID = 640, "LV Electric Piston", ""));
        ItemList.Electric_Piston_MV.set(addItem(tLastID = 641, "MV Electric Piston", ""));
        ItemList.Electric_Piston_HV.set(addItem(tLastID = 642, "HV Electric Piston", ""));
        ItemList.Electric_Piston_EV.set(addItem(tLastID = 643, "EV Electric Piston", ""));
        ItemList.Electric_Piston_IV.set(addItem(tLastID = 644, "IV Electric Piston", ""));
        ItemList.Electric_Piston_LuV.set(addItem(tLastID = 645, "LuV Electric Piston", ""));
        ItemList.Electric_Piston_ZPM.set(addItem(tLastID = 646, "ZPM Electric Piston", ""));
        ItemList.Electric_Piston_UV.set(addItem(tLastID = 647, "UV Electric Piston", ""));
        ItemList.Electric_Piston_UHV.set(addItem(tLastID = 648, "UHV Electric Piston", ""));
        ItemList.Electric_Piston_UEV.set(addItem(tLastID = 649, "UEV Electric Piston", ""));

        ItemList.Robot_Arm_LV.set(addItem(tLastID = 650, "LV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_MV.set(addItem(tLastID = 651, "MV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_HV.set(addItem(tLastID = 652, "HV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_EV.set(addItem(tLastID = 653, "EV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_IV.set(addItem(tLastID = 654, "IV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_LuV.set(addItem(tLastID = 655, "LuV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_ZPM.set(addItem(tLastID = 656, "ZPM Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_UV.set(addItem(tLastID = 657, "UV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_UHV.set(addItem(tLastID = 658, "UHV Robot Arm", "Inserts into specific Slots (as Cover)"));
        ItemList.Robot_Arm_UEV.set(addItem(tLastID = 659, "UEV Robot Arm", "Inserts into specific Slots (as Cover)"));

        ItemList.Steam_Valve_LV.set(addItem(tLastID = 660, "LV Steam Regulator", "Configuable up to 20.480 L/sec (as Cover)"));
        ItemList.Steam_Valve_MV.set(addItem(tLastID = 661, "MV Steam Regulator", "Configuable up to 81.920 L/sec (as Cover)"));
        ItemList.Steam_Valve_HV.set(addItem(tLastID = 662, "HV Steam Regulator", "Configuable up to 327.680 L/sec (as Cover)"));
        ItemList.Steam_Valve_EV.set(addItem(tLastID = 663, "EV Steam Regulator", "Configuable up to 1.310.720 L/sec (as Cover)"));
        ItemList.Steam_Valve_IV.set(addItem(tLastID = 664, "IV Steam Regulator", "Configuable up to 5.242.880 L/sec (as Cover)"));

        GregTech_API.registerCover(ItemList.Steam_Valve_LV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(1024, true));
        GregTech_API.registerCover(ItemList.Steam_Valve_MV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(4096, true));
        GregTech_API.registerCover(ItemList.Steam_Valve_HV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[3][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(16384, true));
        GregTech_API.registerCover(ItemList.Steam_Valve_EV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[4][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(65536, true));
        GregTech_API.registerCover(ItemList.Steam_Valve_IV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[5][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_PUMP)), new GT_Cover_FluidRegulator(262144, true));

        GregTech_API.registerCover(ItemList.Robot_Arm_LV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(400));
        GregTech_API.registerCover(ItemList.Robot_Arm_MV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(100));
        GregTech_API.registerCover(ItemList.Robot_Arm_HV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[3][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(20));
        GregTech_API.registerCover(ItemList.Robot_Arm_EV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[4][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(4));
        GregTech_API.registerCover(ItemList.Robot_Arm_IV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[5][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(1));
        GregTech_API.registerCover(ItemList.Robot_Arm_LuV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[6][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(1));
        GregTech_API.registerCover(ItemList.Robot_Arm_ZPM.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[7][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(1));
        GregTech_API.registerCover(ItemList.Robot_Arm_UV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[8][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(1));
        GregTech_API.registerCover(ItemList.Robot_Arm_UHV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[9][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(1));
        GregTech_API.registerCover(ItemList.Robot_Arm_UEV.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[9][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ARM)), new GT_Cover_Arm(1));

        ItemList.Emitter_LV.set(addItem(tLastID = 680, "LV Emitter", ""));
        ItemList.Emitter_MV.set(addItem(tLastID = 681, "MV Emitter", ""));
        ItemList.Emitter_HV.set(addItem(tLastID = 682, "HV Emitter", ""));
        ItemList.Emitter_EV.set(addItem(tLastID = 683, "EV Emitter", ""));
        ItemList.Emitter_IV.set(addItem(tLastID = 684, "IV Emitter", ""));
        ItemList.Emitter_LuV.set(addItem(tLastID = 685, "LuV Emitter", ""));
        ItemList.Emitter_ZPM.set(addItem(tLastID = 686, "ZPM Emitter", ""));
        ItemList.Emitter_UV.set(addItem(tLastID = 687, "UV Emitter", ""));
        ItemList.Emitter_UHV.set(addItem(tLastID = 688, "UHV Emitter", ""));
        ItemList.Emitter_UEV.set(addItem(tLastID = 689, "UEV Emitter", ""));

        ItemList.Field_Generator_LV.set(addItem(tLastID = 670, "LV Field Generator", ""));
        ItemList.Field_Generator_MV.set(addItem(tLastID = 671, "MV Field Generator", ""));
        ItemList.Field_Generator_HV.set(addItem(tLastID = 672, "HV Field Generator", ""));
        ItemList.Field_Generator_EV.set(addItem(tLastID = 673, "EV Field Generator", ""));
        ItemList.Field_Generator_IV.set(addItem(tLastID = 674, "IV Field Generator", ""));
        ItemList.Field_Generator_LuV.set(addItem(tLastID = 675, "LuV Field Generator", ""));
        ItemList.Field_Generator_ZPM.set(addItem(tLastID = 676, "ZPM Field Generator", ""));
        ItemList.Field_Generator_UV.set(addItem(tLastID = 677, "UV Field Generator", ""));
        ItemList.Field_Generator_UHV.set(addItem(tLastID = 678, "UHV Field Generator", ""));
        ItemList.Field_Generator_UEV.set(addItem(tLastID = 679, "UEV Field Generator", ""));

        ItemList.Sensor_LV.set(addItem(tLastID = 690, "LV Sensor", ""));
        ItemList.Sensor_MV.set(addItem(tLastID = 691, "MV Sensor", ""));
        ItemList.Sensor_HV.set(addItem(tLastID = 692, "HV Sensor", ""));
        ItemList.Sensor_EV.set(addItem(tLastID = 693, "EV Sensor", ""));
        ItemList.Sensor_IV.set(addItem(tLastID = 694, "IV Sensor", ""));
        ItemList.Sensor_LuV.set(addItem(tLastID = 695, "LuV Sensor", ""));
        ItemList.Sensor_ZPM.set(addItem(tLastID = 696, "ZPM Sensor", ""));
        ItemList.Sensor_UV.set(addItem(tLastID = 697, "UV Sensor", ""));
        ItemList.Sensor_UHV.set(addItem(tLastID = 698, "UHV Sensor", ""));
        ItemList.Sensor_UEV.set(addItem(tLastID = 699, "UEV Sensor", ""));

        ItemList.Circuit_Primitive.set(addItem(tLastID = 700, "Vacuum Tube", "A very simple Circuit (ULV)", OrePrefixes.circuit.get(Materials.Primitive), SubTag.NO_UNIFICATION));
        ItemList.Circuit_Parts_Vacuum_Tube.set(ItemList.Circuit_Primitive.get(1));
        ItemList.Circuit_Basic.set(addItem(tLastID = 701, "Integrated Logic Circuit", "A Basic Circuit (LV)", OrePrefixes.circuit.get(Materials.Basic), SubTag.NO_UNIFICATION));
        ItemList.Circuit_Good.set(addItem(tLastID = 702, "Good Electronic Circuit", "A Good Circuit (MV)", OrePrefixes.circuit.get(Materials.Good), SubTag.NO_UNIFICATION));
        ItemList.Circuit_Advanced.set(addItem(tLastID = 703, "Processor Assembly", "An Advanced Circuit (HV)", OrePrefixes.circuit.get(Materials.Advanced), SubTag.NO_UNIFICATION));
        ItemList.Circuit_Computer.set(ItemList.Circuit_Advanced.get(1));
        ItemList.Circuit_Data.set(addItem(tLastID = 704, "Workstation", "An Extreme Circuit (EV)", OrePrefixes.circuit.get(Materials.Data), SubTag.NO_UNIFICATION));
        ItemList.Circuit_Elite.set(addItem(tLastID = 705, "Mainframe", "An Elite Circuit (IV)", OrePrefixes.circuit.get(Materials.Elite), SubTag.NO_UNIFICATION));
        ItemList.Circuit_Master.set(addItem(tLastID = 706, "Nanoprocessor Mainframe", "A Master Circuit (LuV)", OrePrefixes.circuit.get(Materials.Master), SubTag.NO_UNIFICATION));
        ItemList.Tool_DataOrb.set(addItem(tLastID = 707, "Data Orb", "A High Capacity Data Storage", SubTag.NO_UNIFICATION, new Behaviour_DataOrb()));
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Tool_DataOrb.get(1L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Tool_DataOrb.get(1L)});
        ItemList.Tool_DataStick.set(addItem(tLastID = 708, "Data Stick", "A Low Capacity Data Storage", SubTag.NO_UNIFICATION, new Behaviour_DataStick()));
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Tool_DataStick.get(1L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{ItemList.Tool_DataStick.get(1L)});

        ItemList.Circuit_Board_Basic.set(addItem(tLastID = 710, "Coated Circuit Board", "A Basic Board"));
        ItemList.Circuit_Board_Coated.set(ItemList.Circuit_Board_Basic.get(1));
        ItemList.Circuit_Board_Advanced.set(addItem(tLastID = 711, "Epoxy Circuit Board", "An Advanced Board"));
        ItemList.Circuit_Board_Epoxy.set(ItemList.Circuit_Board_Advanced.get(1));
        ItemList.Circuit_Board_Elite.set(addItem(tLastID = 712, "Multilayer Fiber-Reinforced Circuit Board", "An Elite Board"));
        ItemList.Circuit_Board_Multifiberglass.set(ItemList.Circuit_Board_Elite.get(1));
        ItemList.Circuit_Parts_Crystal_Chip_Elite.set(addItem(tLastID = 713, "Engraved Crystal Chip", "Needed for Circuits"));
        ItemList.Circuit_Parts_Crystal_Chip_Master.set(addItem(tLastID = 714, "Engraved Lapotron Chip", "Needed for Circuits"));
        ItemList.Circuit_Parts_Advanced.set(addItem(tLastID = 715, "Diode", "Basic Electronic Component"));
        ItemList.Circuit_Parts_Diode.set(ItemList.Circuit_Parts_Advanced.get(1));
        ItemList.Circuit_Parts_Wiring_Basic.set(addItem(tLastID = 716, "Resistor", "Basic Electronic Component"));
        ItemList.Circuit_Parts_Resistor.set(ItemList.Circuit_Parts_Wiring_Basic.get(1));
        ItemList.Circuit_Parts_Wiring_Advanced.set(addItem(tLastID = 717, "Transistor", "Basic Electronic Component"));
        ItemList.Circuit_Parts_Transistor.set(ItemList.Circuit_Parts_Wiring_Advanced.get(1));
        ItemList.Circuit_Parts_Wiring_Elite.set(addItem(tLastID = 718, "Capacitor", "Electronic Component"));
        ItemList.Circuit_Parts_Capacitor.set(ItemList.Circuit_Parts_Wiring_Elite.get(1));
        ItemList.Empty_Board_Basic.set(addItem(tLastID = 719, "Phenolic Circuit Board", "A Good Board"));
        ItemList.Circuit_Board_Phenolic.set(ItemList.Empty_Board_Basic.get(1));
        ItemList.Empty_Board_Elite.set(addItem(tLastID = 720, "Fiber-Reinforced Circuit Board", "An Extreme Board"));
        ItemList.Circuit_Board_Fiberglass.set(ItemList.Empty_Board_Elite.get(1));


        ItemList.Component_Sawblade_Diamond.set(addItem(tLastID = 721, "Diamond Sawblade", "", OreDictNames.craftingDiamondBlade));
        ItemList.Component_Grinder_Diamond.set(addItem(tLastID = 722, "Diamond Grinding Head", "", OreDictNames.craftingGrinder));
        ItemList.Component_Grinder_Tungsten.set(addItem(tLastID = 723, "Tungsten Grinding Head", "", OreDictNames.craftingGrinder));

        ItemList.QuantumEye.set(addItem(tLastID = 724, "Quantum Eye", "Improved Ender Eye"));
        ItemList.QuantumStar.set(addItem(tLastID = 725, "Quantum Star", "Improved Nether Star"));
        ItemList.Gravistar.set(addItem(tLastID = 726, "Gravi Star", "Ultimate Nether Star"));

        GT_ModHandler.addCraftingRecipe(ItemList.Component_Sawblade_Diamond.get(1L), GT_ModHandler.RecipeBits.DISMANTLEABLE | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{" D ", "DGD", " D ", 'D', OrePrefixes.dustSmall.get(Materials.Diamond), 'G', OrePrefixes.gearGt.get(Materials.CobaltBrass)});
        GT_ModHandler.addCraftingRecipe(ItemList.Component_Grinder_Diamond.get(1L), GT_ModHandler.RecipeBits.DISMANTLEABLE | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"DSD", "SIS", "DSD", 'I', OrePrefixes.gem.get(Materials.Diamond), 'D', OrePrefixes.dust.get(Materials.Diamond), 'S', OrePrefixes.plateDouble.get(Materials.Steel)});
        GT_ModHandler.addCraftingRecipe(ItemList.Component_Grinder_Tungsten.get(1L), GT_ModHandler.RecipeBits.DISMANTLEABLE | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"TST", "SIS", "TST", 'I', OreDictNames.craftingIndustrialDiamond, 'T', OrePrefixes.plate.get(Materials.Tungsten), 'S', OrePrefixes.plateDouble.get(Materials.Steel)});

        ItemList.Upgrade_Muffler.set(addItem(tLastID = 727, "Muffler Upgrade", "Makes Machines silent"));
        ItemList.Upgrade_Lock.set(addItem(tLastID = 728, "Lock Upgrade", "Protects your Machines"));

        ItemList.Component_Filter.set(addItem(tLastID = 729, "Item Filter", "", new ItemData(Materials.Zinc, OrePrefixes.foil.mMaterialAmount * 16L), OreDictNames.craftingFilter));

        GT_Values.RA.addAssemblerRecipe(GT_ModHandler.getIC2Item("carbonMesh", 4L), GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Zinc, 16L), Materials.Plastic.getMolten(144), ItemList.Component_Filter.get(1L), 1600, 32);

        ItemList.Cover_Controller.set(addItem(tLastID = 730, "Machine Controller", "Turns Machines ON/OFF (as Cover)"));
        ItemList.Cover_ActivityDetector.set(addItem(tLastID = 731, "Activity Detector", "Gives out Activity as Redstone (as Cover)"));
        ItemList.Cover_FluidDetector.set(addItem(tLastID = 732, "Fluid Detector", "Gives out Fluid Amount as Redstone (as Cover)"));
        ItemList.Cover_ItemDetector.set(addItem(tLastID = 733, "Item Detector", "Gives out Item Amount as Redstone (as Cover)"));
        ItemList.Cover_EnergyDetector.set(addItem(tLastID = 734, "Energy Detector", "Gives out Energy Amount as Redstone (as Cover)"));
        ItemList.Cover_PlayerDetector.set(addItem(tLastID = 735, "Player Detector", "Gives out close Players as Redstone (as Cover)"));
        GT_Values.RA.addAssemblerRecipe(ItemList.Sensor_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 1L), ItemList.Cover_PlayerDetector.get(1L), 3200, 128);

        GregTech_API.registerCover(ItemList.Cover_Controller.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CONTROLLER)), new GT_Cover_ControlsWork());
        GregTech_API.registerCover(ItemList.Cover_ActivityDetector.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ACTIVITYDETECTOR)), new GT_Cover_DoesWork());
        GregTech_API.registerCover(ItemList.Cover_FluidDetector.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_FLUIDDETECTOR)), new GT_Cover_LiquidMeter());
        GregTech_API.registerCover(ItemList.Cover_ItemDetector.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ITEMDETECTOR)), new GT_Cover_ItemMeter());
        GregTech_API.registerCover(ItemList.Cover_EnergyDetector.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ENERGYDETECTOR)), new GT_Cover_EUMeter());
        GregTech_API.registerCover(ItemList.Cover_PlayerDetector.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ACTIVITYDETECTOR)), new GT_Cover_PlayerDetector());

        ItemList.Cover_Screen.set(addItem(tLastID = 740, "Computer Monitor", "Displays Data (as Cover)"));
        ItemList.Cover_Crafting.set(addItem(tLastID = 744, "Crafting Table Cover", "Better than a wooden Workbench (as Cover)"));
        ItemList.Cover_Drain.set(addItem(tLastID = 745, "Drain", "Absorbs Fluids and collects Rain (as Cover)"));

        ItemList.Cover_Shutter.set(addItem(tLastID = 749, "Shutter Module", "Blocks Inventory/Tank Side. Usage together with Machine Controller (as Cover)"));

        GregTech_API.registerCover(ItemList.Cover_Screen.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_SCREEN)), new GT_Cover_Screen());
        GregTech_API.registerCover(ItemList.Cover_Crafting.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_CRAFTING)), new GT_Cover_Crafting());
        GregTech_API.registerCover(ItemList.Cover_Drain.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[0][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_DRAIN)), new GT_Cover_Drain());
        GregTech_API.registerCover(ItemList.Cover_Shutter.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[1][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_SHUTTER)), new GT_Cover_Shutter());

        ItemList.Cover_SolarPanel.set(addItem(tLastID = 750, "Solar Panel", "May the Sun be with you (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_8V.set(addItem(tLastID = 751, "Solar Panel (8V)", "8 Volt Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_LV.set(addItem(tLastID = 752, "Solar Panel (LV)", "Low Voltage Solar Panel (Needs cleaning with right click (as Cover)"));
        ItemList.Cover_SolarPanel_MV.set(addItem(tLastID = 753, "Solar Panel (MV)", "Medium Voltage Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_HV.set(addItem(tLastID = 754, "Solar Panel (HV)", "High Voltage Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_EV.set(addItem(tLastID = 755, "Solar Panel (EV)", "Extreme Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_IV.set(addItem(tLastID = 756, "Solar Panel (IV)", "Insane Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_LuV.set(addItem(tLastID = 757, "Solar Panel (LuV)", "Ludicrous Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_ZPM.set(addItem(tLastID = 758, "Solar Panel (ZPM)", "ZPM Voltage Solar Panel (Needs cleaning with right click) (as Cover)"));
        ItemList.Cover_SolarPanel_UV.set(addItem(tLastID = 759, "Solar Panel (UV)", "Ultimate Solar Panel (Needs cleaning with right click) (as Cover)"));

        GregTech_API.registerCover(ItemList.Cover_SolarPanel.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL), new GT_Cover_SolarPanel(1));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_8V.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_8V), new GT_Cover_SolarPanel(8));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_LV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_LV), new GT_Cover_SolarPanel(32));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_MV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_MV), new GT_Cover_SolarPanel(128));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_HV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_HV), new GT_Cover_SolarPanel(512));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_EV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_EV), new GT_Cover_SolarPanel(2048));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_IV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_IV), new GT_Cover_SolarPanel(8192));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_LuV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_LuV), new GT_Cover_SolarPanel(32768));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_ZPM.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_ZPM), new GT_Cover_SolarPanel(131072));
        GregTech_API.registerCover(ItemList.Cover_SolarPanel_UV.get(1L), TextureFactory.of(Textures.BlockIcons.SOLARPANEL_UV), new GT_Cover_SolarPanel(524288));

        ItemList.Tool_Sonictron.set(addItem(tLastID = 760, "Sonictron", "Bring your Music with you", Behaviour_Sonictron.INSTANCE));
        ItemList.Tool_Cheat.set(addItem(tLastID = 761, "Debug Scanner", "Also an Infinite Energy Source", Behaviour_Scanner.INSTANCE));
        setElectricStats(32000 + tLastID, -2000000000L, 1000000000L, -1L, -3L, false);
        ItemList.Tool_Scanner.set(addItem(tLastID = 762, "Portable Scanner", "Tricorder", Behaviour_Scanner.INSTANCE));
        setElectricStats(32000 + tLastID, 400000L, GT_Values.V[2], 2L, -1L, false);
        GT_ModHandler.addCraftingRecipe(ItemList.Tool_Scanner.get(1L), GT_ModHandler.RecipeBits.DISMANTLEABLE | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"EPR", "CSC", "PBP", 'C', OrePrefixes.circuit.get(Materials.Advanced), 'P', OrePrefixes.plate.get(Materials.Aluminium), 'E', ItemList.Emitter_MV, 'R', ItemList.Sensor_MV, 'S', ItemList.Cover_Screen, 'B', ItemList.Battery_RE_MV_Lithium});
        ItemList.NC_SensorKit.set(addItem(tLastID = 763, "GregTech Sensor Kit", "", new Behaviour_SensorKit()));
        ItemList.Duct_Tape.set(addItem(tLastID = 764, "BrainTech Aerospace Advanced Reinforced Duct Tape FAL-84", "If you can't fix it with this, use more of it!", OreDictNames.craftingDuctTape));
        ItemList.McGuffium_239.set(addItem(tLastID = 765, "Mc Guffium 239", "42% better than Phlebotnium"));

        GT_Values.RA.addAssemblerRecipe(ItemList.Sensor_LV.get(1L), ItemList.Emitter_LV.get(1L), ItemList.NC_SensorKit.get(1L), 1600, 2);

        ItemList.Cover_RedstoneTransmitterExternal.set(addItem(tLastID = 741, "Redstone Transmitter (Out)", "Transfers Redstonesignals wireless (as Cover)"));
        ItemList.Cover_RedstoneTransmitterInternal.set(addItem(tLastID = 742, "Redstone Transmitter (In)", "Transfers Redstonesignals wireless (as Cover)"));
        ItemList.Cover_RedstoneReceiverExternal.set(addItem(tLastID = 746, "Redstone Receiver (Out)", "Transfers Redstonesignals wireless (as Cover)"));
        ItemList.Cover_RedstoneReceiverInternal.set(addItem(tLastID = 747, "Redstone Receiver (In)", "Transfers Redstonesignals wireless (as Cover)"));

        GregTech_API.registerCover(ItemList.Cover_RedstoneTransmitterExternal.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ACTIVITYDETECTOR)), new GT_Cover_RedstoneTransmitterExternal());
        GregTech_API.registerCover(ItemList.Cover_RedstoneTransmitterInternal.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ACTIVITYDETECTOR)), new GT_Cover_RedstoneTransmitterInternal());
        GregTech_API.registerCover(ItemList.Cover_RedstoneReceiverExternal.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_FLUIDDETECTOR)), new GT_Cover_RedstoneReceiverExternal());
        GregTech_API.registerCover(ItemList.Cover_RedstoneReceiverInternal.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_FLUIDDETECTOR)), new GT_Cover_RedstoneReceiverInternal());

        GT_Values.RA.addAssemblerRecipe(ItemList.Emitter_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 1L), ItemList.Cover_RedstoneTransmitterExternal.get(1L), 3200, 128);
        GT_Values.RA.addAssemblerRecipe(ItemList.Sensor_EV.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 1L), ItemList.Cover_RedstoneReceiverExternal.get(1L), 3200, 128);
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Cover_RedstoneTransmitterInternal.get(1L), new Object[]{ItemList.Cover_RedstoneTransmitterExternal.get(1L)});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Cover_RedstoneReceiverInternal.get(1L), new Object[]{ItemList.Cover_RedstoneReceiverExternal.get(1L)});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Cover_RedstoneTransmitterExternal.get(1L), new Object[]{ItemList.Cover_RedstoneTransmitterInternal.get(1L)});
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Cover_RedstoneReceiverExternal.get(1L), new Object[]{ItemList.Cover_RedstoneReceiverInternal.get(1L)});

        ItemList.Cover_NeedsMaintainance.set(addItem(tLastID = 748, "Needs Maintenance Cover", "Attach to Multiblock Controller. Emits Redstone Signal if needs Maintenance"));
        GregTech_API.registerCover(ItemList.Cover_NeedsMaintainance.get(1L), new GT_MultiTexture(Textures.BlockIcons.MACHINE_CASINGS[2][0], TextureFactory.of(Textures.BlockIcons.OVERLAY_ACTIVITYDETECTOR)), new GT_Cover_NeedMaintainance());
        GT_Values.RA.addAssemblerRecipe(ItemList.Emitter_MV.get(1L), GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 1L), ItemList.Cover_NeedsMaintainance.get(1L), 600, 24);

        ItemList.Energy_Module.set(addItem(tLastID = 736, "Energy Module", "Reusable battery", OrePrefixes.battery.get(Materials.Ultimate)));
        setElectricStats(32000 + tLastID, 10000000000L, GT_Values.V[7], 7L, -3L, true);

        ItemList.Energy_Cluster.set(addItem(tLastID = 737, "Energy Cluster", "Reusable battery"));
        setElectricStats(32000 + tLastID, 100000000000L, GT_Values.V[8], 8L, -3L, true);

        ItemList.ZPM2.set(addItem(tLastID = 605, "Ultimate Battery", "Fill this to win minecraft"));
        setElectricStats(32000 + tLastID, Long.MAX_VALUE, GT_Values.V[9], 9L, -3L, true);

        ItemList.ZPM3.set(addItem(tLastID = 738, "Really Ultimate Battery", "Fill this to be way older"));
        setElectricStats(32000 + tLastID, Long.MAX_VALUE, GT_Values.V[12], 12L, -3L, true);

        ItemList.BatteryHull_EV.set(addItem(tLastID = 503, "Small Vanadium Battery (Empty)", "An empty EV Battery Container", new ItemData(Materials.BlueSteel, OrePrefixes.plate.mMaterialAmount * 2L)));
        ItemList.BatteryHull_IV.set(addItem(tLastID = 504, "Medium Vanadium Battery (Empty)", "An empty IV Battery Container", new ItemData(Materials.RoseGold, OrePrefixes.plate.mMaterialAmount * 6L)));
        ItemList.BatteryHull_LuV.set(addItem(tLastID = 505, "Large Vanadium Battery (Empty)", "An empty LuV Battery Container", new ItemData(Materials.RedSteel, OrePrefixes.plate.mMaterialAmount * 18L)));
        ItemList.BatteryHull_ZPM.set(addItem(tLastID = 506, "Medium Naquadria Battery (Empty)", "An empty ZPM Energy Storage", new ItemData(Materials.Europium, OrePrefixes.plate.mMaterialAmount * 6L)));
        ItemList.BatteryHull_UV.set(addItem(tLastID = 507, "Large Naquadria Battery (Empty)", "An empty UV Energy Storage", new ItemData(Materials.Americium, OrePrefixes.plate.mMaterialAmount * 18L)));
        ItemList.BatteryHull_UHV.set(addItem(tLastID = 561, "Small Neutronium Battery (Empty)", "An empty UHV Energy Storage", new ItemData(Materials.Naquadah, OrePrefixes.plate.mMaterialAmount * 24L)));
        ItemList.BatteryHull_UEV.set(addItem(tLastID = 562, "Medium Neutronium Battery (Empty)", "An empty UEV Energy Storage", new ItemData(Materials.NaquadahEnriched, OrePrefixes.plate.mMaterialAmount * 36L)));
        ItemList.BatteryHull_UIV.set(addItem(tLastID = 563, "Large Neutronium Battery (Empty)", "An empty UIV Energy Storage", new ItemData(Materials.NaquadahAlloy, OrePrefixes.plate.mMaterialAmount * 48L)));

        ItemList.BatteryHull_EV_Full.set(addItem(tLastID = 540, "Small Vanadium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 6400000L, GT_Values.V[4], 4L, -3L, true);
        ItemList.BatteryHull_IV_Full.set(addItem(tLastID = 545, "Medium Vanadium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 25600000L, GT_Values.V[5], 5L, -3L, true);
        ItemList.BatteryHull_LuV_Full.set(addItem(tLastID = 550, "Large Vanadium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 102400000L, GT_Values.V[6], 6L, -3L, true);
        ItemList.BatteryHull_ZPM_Full.set(addItem(tLastID = 555, "Medium Naquadria Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 409600000L, GT_Values.V[7], 7L, -3L, true);
        ItemList.BatteryHull_UV_Full.set(addItem(tLastID = 560, "Large Naquadria Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 1638400000L, GT_Values.V[8], 8L, -3L, true);
        ItemList.BatteryHull_UHV_Full.set(addItem(tLastID = 570, "Small Neutronium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 6553600000L, GT_Values.V[9], 9L, -3L, true);
        ItemList.BatteryHull_UEV_Full.set(addItem(tLastID = 575, "Medium Neutronium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 26214400000L, GT_Values.V[10], 10L, -3L, true);
        ItemList.BatteryHull_UIV_Full.set(addItem(tLastID = 580, "Large Neutronium Battery", "Reusable"));
        setElectricStats(32000 + tLastID, 104857600000L, GT_Values.V[11], 11L, -3L, true);

        //Antimatter
        ItemList.Magnetic_Confinement_Pod.set(addItem(tLastID = 594, "Magnetic Confinement Pod", "For contains very dangerous material in antigravitation field!"));
        ItemList.Magnetic_Confinement_Pod_AntiHydrogen.set(addItem(tLastID = 595, "AntiHydrogen Pod", "Antimatter Reactor Fuel. AntiWater?! @.@"));
        ItemList.Magnetic_Confinement_Pod_Hydrogen.set(addItem(tLastID = 596, "Hydrogen Pod", "Antimatter Reactor Fuel"));
        GT_ModHandler.addCraftingRecipe(ItemList.Magnetic_Confinement_Pod.get(1L), GT_ModHandler.RecipeBits.DISMANTLEABLE | GT_ModHandler.RecipeBits.NOT_REMOVABLE | GT_ModHandler.RecipeBits.REVERSIBLE, new Object[]{"PCP", "F F", "PBP", 'C', OrePrefixes.circuit.get(Materials.Master), 'P', OrePrefixes.plate.get(Materials.NaquadahAlloy), 'F', ItemList.Field_Generator_ZPM, 'B', ItemList.Energy_LapotronicOrb2});


    }

    @Override
    public boolean onEntityItemUpdate(EntityItem aItemEntity) {
        int aDamage = aItemEntity.getEntityItem().getItemDamage();
        if ((aDamage < 32000) && (aDamage >= 0) && (!aItemEntity.worldObj.isRemote)) {
            Materials aMaterial = GregTech_API.sGeneratedMaterials[(aDamage % 1000)];
            if ((aMaterial != null) && (aMaterial != Materials.Empty) && (aMaterial != Materials._NULL)) {
                int tX = MathHelper.floor_double(aItemEntity.posX);
                int tY = MathHelper.floor_double(aItemEntity.posY);
                int tZ = MathHelper.floor_double(aItemEntity.posZ);
                OrePrefixes aPrefix = this.mGeneratedPrefixList[(aDamage / 1000)];
                if ((aPrefix == OrePrefixes.dustImpure) || (aPrefix == OrePrefixes.dustPure)) {
                    Block tBlock = aItemEntity.worldObj.getBlock(tX, tY, tZ);
                    byte tMetaData = (byte) aItemEntity.worldObj.getBlockMetadata(tX, tY, tZ);
                    if ((tBlock == Blocks.cauldron) && (tMetaData > 0)) {
                        aItemEntity.setEntityItemStack(GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial, aItemEntity.getEntityItem().stackSize));
                        aItemEntity.worldObj.setBlockMetadataWithNotify(tX, tY, tZ, tMetaData - 1, 3);
                        return true;
                    }
                } else if (aPrefix == OrePrefixes.crushed) {
                    Block tBlock = aItemEntity.worldObj.getBlock(tX, tY, tZ);
                    byte tMetaData = (byte) aItemEntity.worldObj.getBlockMetadata(tX, tY, tZ);
                    if ((tBlock == Blocks.cauldron) && (tMetaData > 0)) {
                        aItemEntity.setEntityItemStack(GT_OreDictUnificator.get(OrePrefixes.crushedPurified, aMaterial, aItemEntity.getEntityItem().stackSize));
                        aItemEntity.worldObj.setBlockMetadataWithNotify(tX, tY, tZ, tMetaData - 1, 3);
                        return true;
                    }
                } else if (aPrefix == OrePrefixes.dust && aMaterial == Materials.Wheat) {
                    Block tBlock = aItemEntity.worldObj.getBlock(tX, tY, tZ);
                    byte tMetaData = (byte) aItemEntity.worldObj.getBlockMetadata(tX, tY, tZ);
                    if ((tBlock == Blocks.cauldron) && (tMetaData > 0)) {
                        aItemEntity.setEntityItemStack(ItemList.Food_Dough.get(aItemEntity.getEntityItem().stackSize));
                        aItemEntity.worldObj.setBlockMetadataWithNotify(tX, tY, tZ, tMetaData - 1, 3);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalToolTips(List aList, ItemStack aStack, EntityPlayer aPlayer) {
        super.addAdditionalToolTips(aList, aStack, aPlayer);
        int aDamage = aStack.getItemDamage();
        if ((aDamage < 32000) && (aDamage >= 0)) {
            Materials aMaterial = GregTech_API.sGeneratedMaterials[(aDamage % 1000)];
            if ((aMaterial != null) && (aMaterial != Materials.Empty) && (aMaterial != Materials._NULL)) {
                OrePrefixes aPrefix = this.mGeneratedPrefixList[(aDamage / 1000)];
                if ((aPrefix == OrePrefixes.dustImpure) || (aPrefix == OrePrefixes.dustPure)) {
                    aList.add(this.mToolTipPurify);
                }
            }
        }
    }

    public boolean isPlasmaCellUsed(OrePrefixes aPrefix, Materials aMaterial) {
        Collection<GT_Recipe> fusionRecipes = GT_Recipe.GT_Recipe_Map.sFusionRecipes.mRecipeList;
        if (aPrefix == OrePrefixes.cellPlasma && aMaterial.getPlasma(1L) != null) { //Materials has a plasma fluid
            for (GT_Recipe recipe : fusionRecipes) { //Loop through fusion recipes
                if (recipe.getFluidOutput(0) != null) { //Make sure fluid output can't be null (not sure if possible)
                    if (recipe.getFluidOutput(0).isFluidEqual(aMaterial.getPlasma(1L)))
                        return true; //Fusion recipe output matches current plasma cell fluid
                }
            }
        }
        return false;
    }

    @Override
    public boolean doesShowInCreative(OrePrefixes aPrefix, Materials aMaterial, boolean aDoShowAllItems) {
        return (aDoShowAllItems) || (((aPrefix != OrePrefixes.gem) || (!aMaterial.mName.startsWith("Infused"))) && (aPrefix != OrePrefixes.dustTiny) && (aPrefix != OrePrefixes.dustSmall) && (aPrefix != OrePrefixes.dustImpure) && (aPrefix != OrePrefixes.dustPure) && (aPrefix != OrePrefixes.crushedPurified) && (aPrefix != OrePrefixes.crushedCentrifuged) && (aPrefix != OrePrefixes.ingotHot) && !(aPrefix == OrePrefixes.cellPlasma && !isPlasmaCellUsed(aPrefix, aMaterial)));
    }

    @Override
    public ItemStack getContainerItem(ItemStack aStack) {
        int aDamage = aStack.getItemDamage();
        if ((aDamage >= 32430) && (aDamage <= 32461)) {
            return ItemList.Spray_Empty.get(1L);
        }
        if ((aDamage == 32479) || (aDamage == 32476)) {
            return new ItemStack(this, 1, aDamage - 2);
        }
        if (aDamage == 32401) {
            return new ItemStack(this, 1, aDamage - 1);
        }
        return super.getContainerItem(aStack);
    }

    @Override
    public boolean doesMaterialAllowGeneration(OrePrefixes aPrefix, Materials aMaterial) {
        return (super.doesMaterialAllowGeneration(aPrefix, aMaterial));
    }
}
