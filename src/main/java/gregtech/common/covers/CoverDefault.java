package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;

public class CoverDefault extends CoverMode {

    public CoverDefault(CoverContext context) {
        super(context,
                new String[] { "Blocked", "Redstone", "Energy", "Redstone, Energy", "Fluids", "Redstone, Fluids",
                        "Energy, Fluids", "Redstone, Energy, Fluids", "Items", "Redstone, Items", "Energy, Items",
                        "Redstone, Energy, Items", "Fluids, Items", "Redstone, Fluids, Items", "Energy, Fluids, Items",
                        "Redstone, Energy, Fluids, Items" });
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = ((coverData + 1) & 15);
        GT_Utility.sendChatToPlayer(player,
                ((coverData & 1) != 0
                        ? GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_128", "Redstone ")
                        : "")
                        + ((coverData & 2) != 0 ? GT_LanguageManager
                                .addStringLocalization("Interaction_DESCRIPTION_Index_129", "Energy ") : "")
                        + ((coverData & 4) != 0
                                ? GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_130",
                                        "Fluids ")
                                : "")
                        + ((coverData & 8) != 0
                                ? GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_131",
                                        "Items ")
                                : ""));
        markGuiChanged();
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return (coverData & 1) != 0;
    }

    @Override
    public boolean letsRedstoneGoOut() {
        return (coverData & 1) != 0;
    }

    @Override
    public boolean letsEnergyIn() {
        return (coverData & 2) != 0;
    }

    @Override
    public boolean letsEnergyOut() {
        return (coverData & 2) != 0;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return (coverData & 4) != 0;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return (coverData & 4) != 0;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return (coverData & 8) != 0;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return (coverData & 8) != 0;
    }
}
