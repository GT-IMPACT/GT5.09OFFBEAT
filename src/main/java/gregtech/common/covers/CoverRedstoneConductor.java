package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;

public class CoverRedstoneConductor extends CoverMode {

    public CoverRedstoneConductor(CoverContext context) {
        super(context, new String[] { "Strongest input", "Bottom input", "Top input", "North input", "South input",
                "West input", "East input" });
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if (coverData == 0) {
            tile.setOutputRedstoneSignal(side, tile.getStrongestRedstone());
        } else if (coverData < 7) {
            tile.setOutputRedstoneSignal(side, tile.getInternalInputRedstoneSignal((byte) (coverData - 1)));
        }
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 7;
        if (coverData < 0) {
            coverData = 6;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_071", "Conducts strongest Input"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_072", "Conducts from bottom Input"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_073", "Conducts from top Input"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_074", "Conducts from north Input"));
            break;

        case 4:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_075", "Conducts from south Input"));
            break;

        case 5:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_076", "Conducts from west Input"));
            break;

        case 6:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_077", "Conducts from east Input"));
            break;

        }
        markGuiChanged();
    }

    @Override
    public boolean letsEnergyIn() {
        return true;
    }

    @Override
    public boolean letsEnergyOut() {
        return true;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return true;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return true;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return true;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return true;
    }

    @Override
    public boolean manipulatesSidedRedstoneOutput() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return 1;
    }
}
