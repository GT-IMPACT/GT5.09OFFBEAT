package gregtech.common.items.behaviors;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.IItemBehaviour;
import gregtech.api.items.GT_MetaBase_Item;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class Behaviour_NoteBook extends Behaviour_None {

    public static final IItemBehaviour<GT_MetaBase_Item> INSTANCE = new Behaviour_NoteBook();

    protected MetaTileEntity mMetaTileEntity;

    public boolean onItemUseFirst(GT_MetaBase_Item aItem, ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide, float hitX, float hitY, float hitZ) {
        NBTTagCompound tNBT = aStack.getTagCompound();
        if (((aPlayer instanceof EntityPlayerMP)) && aPlayer.isSneaking() && (aItem.canUse(aStack, 20000.0D))) {
            ArrayList<String> tList = new ArrayList();
            if (aItem.use(aStack, GT_Utility.getOnlySensorInfo(tList, aPlayer, aWorld, 1, aX, aY, aZ, aSide, hitX, hitY, hitZ), aPlayer)) {
                int tList_sS=tList.size();
                tNBT.setInteger("dataLinesCount",tList_sS);
                for (int i = 0; i < tList_sS; i++) {
                    tNBT.setString("dataLines" + i, tList.get(i));
                    GT_Utility.sendChatToPlayer(aPlayer, tList.get(i));
                }
            }
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(Integer.valueOf(108)), 1, 1.0F, aX, aY, aZ);
            //aPlayer.openGui("gregtech", 1021, aWorld, aX, aY, aZ);
            return true;
        }

        return false;
    }

    public List<String> getAdditionalToolTips(GT_MetaBase_Item aItem, List<String> aList, ItemStack aStack) {
        String[] mTooltip = new String[] {
                "Your manual assistant",
                EnumChatFormatting.YELLOW + "RightClick Function:",
                "[Applied Energistics 2] Blocks and Parts renamed",
                EnumChatFormatting.YELLOW + "Shift + RightClick Function:",
                "[Gregtech] Controllers and Basic Machines information sensor",
        };

        for (int i = 0; i < mTooltip.length; i++)
            aList.add(mTooltip[i]);

        try {
            NBTTagCompound tNBT = aStack.getTagCompound();
            int lines = tNBT.getInteger("dataLinesCount");
            if(lines<1) throw new Exception();
            aList.add(EnumChatFormatting.RESET + "------------------------");
            aList.add(EnumChatFormatting.RED + "Last sensor information:");
            for (int i = 0; i < lines; i++) {
                aList.add(EnumChatFormatting.RESET+tNBT.getString("dataLines" + i));
            }
        }catch(Exception ignored) {}

        return aList;
    }

}
