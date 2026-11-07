package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.modes.ShutterMode;
import gregtech.common.gui.modularui.cover.CoverShutterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;

public class CoverShutter extends CoverLegacyData {

    public CoverShutter(CoverContext context) {
        super(context);
    }

    public ShutterMode getShutterMode() {
        int mode = getVariable();
        return mode >= 0 && mode < ShutterMode.values().length ? ShutterMode.values()[mode]
                : ShutterMode.OPEN_IF_ENABLED;
    }

    public void setShutterMode(ShutterMode mode) {
        if (mode != null) {
            setGuiVariable(mode.ordinal());
        }
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverShutterGui(this);
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 4;
        if (coverData < 0) {
            coverData = 3;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_082", "Open if work enabled"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_083", "Open if work disabled"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_084", "Only Output allowed"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_085", "Only Input allowed"));
            break;

        }
        markGuiChanged();
    }

    @Override
    public boolean letsRedstoneGoIn() {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 3
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsRedstoneGoOut() {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 2
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsEnergyIn() {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 3
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsEnergyOut() {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 2
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 3
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 2
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 3
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        ICoverable tile = getTile();
        return coverData >= 2 ? coverData == 2
                : (tile instanceof IMachineProgress)
                        ? ((IMachineProgress) tile).isAllowedToWork() ? coverData % 2 == 0 : coverData % 2 != 0
                        : true;
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return 0;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
