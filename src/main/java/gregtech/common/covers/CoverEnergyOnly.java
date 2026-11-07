package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;

public class CoverEnergyOnly extends CoverMode {

    public CoverEnergyOnly(CoverContext context) {
        super(context, new String[] { "Allow", "Allow (conditional)", "Disallow (conditional)" });
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + 1) % 3;
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_028", "Allow"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_029", "Allow (conditional)"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_030", "Disallow (conditional)"));
            break;

        }
        markGuiChanged();
    }

    @Override
    public float getBlastProofLevel() {
        return 20.0F;
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return false;
    }

    @Override
    public boolean letsRedstoneGoOut() {
        return false;
    }

    @Override
    public boolean letsEnergyIn() {
        ICoverable tile = getTile();
        if ((coverData > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean letsEnergyOut() {
        ICoverable tile = getTile();
        if ((coverData > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return false;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return false;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return false;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return false;
    }

    @Override
    public boolean manipulatesSidedRedstoneOutput() {
        return false;
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        return false;
    }

    @Override
    public boolean canRemove(boolean forced) {
        return true;
    }

    @Override
    public boolean isGUIClickable() {
        return false;
    }
}
