package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.modes.DetectionMode;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.gui.modularui.cover.CoverDoesWorkGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;

public class CoverDoesWork extends CoverLegacyData {

    public CoverDoesWork(CoverContext context) {
        super(context);
    }

    public DetectionMode getDetectionMode() {
        if ((getVariable() & 2) != 0) {
            return DetectionMode.MACHINE_IDLE;
        }
        return (getVariable() & 4) != 0 ? DetectionMode.MACHINE_ENABLED : DetectionMode.RECIPE_PROGRESS;
    }

    public void setDetectionMode(DetectionMode mode) {
        if (mode == null) {
            return;
        }
        switch (mode) {
        case MACHINE_IDLE:
            setGuiVariable((getVariable() & ~4) | 2);
            break;
        case MACHINE_ENABLED:
            setGuiVariable((getVariable() & ~2) | 4);
            break;
        default:
            setGuiVariable(getVariable() & ~6);
            break;
        }
    }

    public RedstoneMode getRedstoneMode() {
        return (getVariable() & 1) != 0 ? RedstoneMode.INVERTED : RedstoneMode.NORMAL;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (mode != null) {
            setGuiVariable(mode == RedstoneMode.INVERTED ? getVariable() | 1 : getVariable() & ~1);
        }
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverDoesWorkGui(this);
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        int signal = 0;
        if (tile instanceof IMachineProgress) {
            IMachineProgress machine = (IMachineProgress) tile;
            boolean inverted = (coverData & 1) != 0;
            if ((coverData & 4) != 0) {
                signal = inverted == machine.isAllowedToWork() ? 0 : 15;
            } else if ((coverData & 2) != 0) {
                signal = inverted == (machine.getMaxProgress() == 0) ? 0 : 15;
            } else {
                int scale = machine.getMaxProgress() / 15;
                if (scale > 0 && machine.hasThingsToDo()) {
                    signal = inverted ? 15 - machine.getProgress() / scale : machine.getProgress() / scale;
                } else {
                    signal = inverted ? 15 : 0;
                }
            }
        }
        tile.setOutputRedstoneSignal(side, (byte) signal);
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = Math.floorMod(coverData + (player.isSneaking() ? -1 : 1), 6);
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_018", "Normal"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_019", "Inverted"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_020", "Ready to work"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_021", "Not ready to work"));
            break;

        case 4:
            GT_Utility.sendChatToPlayer(player, StatCollector.translateToLocal("gt.interact.desc.mach_on"));
            break;

        case 5:
            GT_Utility.sendChatToPlayer(player, StatCollector.translateToLocal("gt.interact.desc.mach_off"));
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
        return 5;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
