package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;

public class CoverRedstoneSignalizer extends CoverMode {

    public CoverRedstoneSignalizer(CoverContext context) {
        super(context, new String[] { "Signal: 0", "Signal: 1", "Signal: 2", "Signal: 3", "Signal: 4", "Signal: 5",
                "Signal: 6", "Signal: 7", "Signal: 8", "Signal: 9", "Signal: 10", "Signal: 11", "Signal: 12",
                "Signal: 13", "Signal: 14", "Signal: 15", "Conditional signal: 0", "Conditional signal: 1",
                "Conditional signal: 2", "Conditional signal: 3", "Conditional signal: 4", "Conditional signal: 5",
                "Conditional signal: 6", "Conditional signal: 7", "Conditional signal: 8", "Conditional signal: 9",
                "Conditional signal: 10", "Conditional signal: 11", "Conditional signal: 12", "Conditional signal: 13",
                "Conditional signal: 14", "Conditional signal: 15", "Inverted conditional signal: 0",
                "Inverted conditional signal: 1", "Inverted conditional signal: 2", "Inverted conditional signal: 3",
                "Inverted conditional signal: 4", "Inverted conditional signal: 5", "Inverted conditional signal: 6",
                "Inverted conditional signal: 7", "Inverted conditional signal: 8", "Inverted conditional signal: 9",
                "Inverted conditional signal: 10", "Inverted conditional signal: 11", "Inverted conditional signal: 12",
                "Inverted conditional signal: 13", "Inverted conditional signal: 14",
                "Inverted conditional signal: 15" });
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + 1) % 48;
        switch (coverData / 16) {
        case 0:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_078", "Signal = ")
                            + (coverData & 15));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_079",
                            "Conditional Signal = ") + (coverData & 15));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_080",
                            "Inverted Conditional Signal = ") + (coverData & 15));
            break;

        }
        markGuiChanged();
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return true;
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
    public byte getRedstoneInput(byte redstone) {
        ICoverable tile = getTile();
        if (coverData < 16) {
            return (byte) (coverData & 15);
        }
        if ((tile instanceof IMachineProgress)) {
            if (((IMachineProgress) tile).isAllowedToWork()) {
                if (coverData / 16 == 1) {
                    return (byte) (coverData & 15);
                }
            } else if (coverData / 16 == 2) {
                return (byte) (coverData & 15);
            }
            return 0;
        }
        return (byte) (coverData & 15);
    }
}
