package gregtech.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.items.GT_Generic_Item;
import gregtech.api.util.*;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;

import static gregtech.GT_Mod.GT_FML_LOGGER;
import static gregtech.api.enums.GT_Values.RES_PATH_ITEM;
import static gregtech.api.util.GT_Utility.ItemNBT.getIntegratedAmount;
import static gregtech.api.util.GT_Utility.ItemNBT.setIntegratedAmount;

public class GT_IntegratedCircuit_Item extends GT_Generic_Item {
    private static final String aTextEmptyRow = "   ";
    protected IIcon[] mIconDamage = new IIcon[25];
    public GT_IntegratedCircuit_Item() {
        super("integrated_circuit", "Programmed Circuit", "");
        setHasSubtypes(true);
        setMaxDamage(0);
        
        ItemList.Circuit_Integrated.set(this);
        
        for (int i = 1; i <= 24; i++) {
            GregTech_API.registerConfigurationCircuit(new ItemStack(this, 0, i));
        }
        
        GT_ModHandler.addShapelessCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 0L), GT_ModHandler.RecipeBits.NOT_REMOVABLE, new Object[]{OrePrefixes.circuit.get(Materials.Basic)});
        long bits = GT_ModHandler.RecipeBits.BUFFERED | GT_ModHandler.RecipeBits.NOT_REMOVABLE;
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 1L), bits, new Object[]{"d  ", " P ", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 2L), bits, new Object[]{" d ", " P ", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 3L), bits, new Object[]{"  d", " P ", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 4L), bits, new Object[]{aTextEmptyRow, " Pd", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 5L), bits, new Object[]{aTextEmptyRow, " P ", "  d", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 6L), bits, new Object[]{aTextEmptyRow, " P ", " d ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 7L), bits, new Object[]{aTextEmptyRow, " P ", "d  ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 8L), bits, new Object[]{aTextEmptyRow, "dP ", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 9L), bits, new Object[]{"P d", aTextEmptyRow, aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 10L), bits, new Object[]{"P  ", "  d", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 11L), bits, new Object[]{"P  ", aTextEmptyRow, "  d", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 12L), bits, new Object[]{"P  ", aTextEmptyRow, " d ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 13L), bits, new Object[]{"  P", aTextEmptyRow, "  d", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 14L), bits, new Object[]{"  P", aTextEmptyRow, " d ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 15L), bits, new Object[]{"  P", aTextEmptyRow, "d  ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 16L), bits, new Object[]{"  P", "d  ", aTextEmptyRow, 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 17L), bits, new Object[]{aTextEmptyRow, aTextEmptyRow, "d P", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 18L), bits, new Object[]{aTextEmptyRow, "d  ", "  P", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 19L), bits, new Object[]{"d  ", aTextEmptyRow, "  P", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 20L), bits, new Object[]{" d ", aTextEmptyRow, "  P", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 21L), bits, new Object[]{"d  ", aTextEmptyRow, "P  ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 22L), bits, new Object[]{" d ", aTextEmptyRow, "P  ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 23L), bits, new Object[]{"  d", aTextEmptyRow, "P  ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
        GT_ModHandler.addCraftingRecipe(ItemList.Circuit_Integrated.getWithDamage(1L, 24L), bits, new Object[]{aTextEmptyRow, "  d", "P  ", 'P', ItemList.Circuit_Integrated.getWildcard(1L)});
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World aWorld, EntityPlayer aPlayer) {
        int toolMode = getIntegratedAmount(stack);
        if (!aPlayer.isSneaking()) {
            ++toolMode;
            if (toolMode > 24) toolMode = 0;
        } else {
            --toolMode;
            if (toolMode < 0) toolMode = 24;
        }
        setIntegratedAmount(stack, toolMode);
        GT_Utility.sendChatToPlayer(aPlayer, GT_LanguageManager.addStringLocalization("gt.behaviour.Integrated", "Configuration") +": " + EnumChatFormatting.YELLOW + toolMode);
        setDamage(stack, toolMode);
        return super.onItemRightClick(stack, aWorld, aPlayer);
    }
    
    private static String getModeString(int aMetaData) {
        switch ((byte) (aMetaData >>> 8)) {
            case 0:
                return "==";
            case 1:
                return "<=";
            case 2:
                return ">=";
            case 3:
                return "<";
            case 4:
                return ">";
        }
        return "";
    }

    private static String getConfigurationString(int aMetaData) {
        return getModeString(aMetaData) + " " + (byte) (aMetaData & 0xFF);
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World aWorld, EntityPlayer aPlayer) {
        int toolMode = getIntegratedAmount(stack);
        if (!aPlayer.isSneaking()) {
            ++toolMode;
            if (toolMode > 24) toolMode = 0;
        } else {
            --toolMode;
            if (toolMode < 0) toolMode = 24;
        }
        setIntegratedAmount(stack, toolMode);
        GT_Utility.sendChatToPlayer(aPlayer, GT_LanguageManager.addStringLocalization("gt.behaviour.Integrated", "Configuration") +": " + EnumChatFormatting.YELLOW + toolMode);
        setDamage(stack, toolMode);
        return super.onItemRightClick(stack, aWorld, aPlayer);
    }

    @Override
    public void addAdditionalToolTips(List aList, ItemStack aStack, EntityPlayer aPlayer) {
        super.addAdditionalToolTips(aList, aStack, aPlayer);
        aList.add("Configuration: " + getDamage(aStack));
        aList.add("Rightclick to set number (+1)");
        aList.add("Rightclick + Shift to set number (-1)");
        aList.add(GT_LanguageManager.addStringLocalization(new StringBuilder().append(getUnlocalizedName()).append(".configuration").toString(), "Configuration: ") + getConfigurationString(getDamage(aStack)));
    }

    public String getUnlocalizedName(ItemStack aStack) {
        return getUnlocalizedName();
    }
    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    public final void getSubItems(Item var1, CreativeTabs aCreativeTab, List aList) {
        aList.add(new ItemStack(this, 1, 0));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister aIconRegister) {
        super.registerIcons(aIconRegister);
        for (int i=0; i < mIconDamage.length; i++) {
            mIconDamage[i] = aIconRegister.registerIcon(RES_PATH_ITEM + (GT_Config.troll ? "troll" : getUnlocalizedName() + "/" + i));
        }
        if (GregTech_API.sPostloadFinished) {
            GT_Log.out.println("GT_Mod: Starting Item Icon Load Phase");
            GT_FML_LOGGER.info("GT_Mod: Starting Item Icon Load Phase");
            GregTech_API.sItemIcons = aIconRegister;
            try {
                for (Runnable tRunnable : GregTech_API.sGTItemIconload) {
                    tRunnable.run();
                }
            } catch (Throwable e) {e.printStackTrace(GT_Log.err);}
            GT_Log.out.println("GT_Mod: Finished Item Icon Load Phase");
            GT_FML_LOGGER.info("GT_Mod: Finished Item Icon Load Phase");
        }
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        byte circuitMode = ((byte) (damage & 0xFF)); // Mask out the MSB Comparison Mode Bits. See: getModeString
        return mIconDamage[circuitMode < mIconDamage.length ? circuitMode : 0];
    }
}
