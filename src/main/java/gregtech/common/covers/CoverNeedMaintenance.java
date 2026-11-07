package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.modes.MaintenanceAlertCondition;
import gregtech.common.covers.modes.RedstoneMode;
import gregtech.common.gui.modularui.cover.CoverNeedMaintenanceGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

public class CoverNeedMaintenance extends CoverLegacyData {

    public CoverNeedMaintenance(CoverContext context) {
        super(context);
    }

    public MaintenanceAlertCondition getMaintenanceAlertCondition() {
        int mode = getVariable() >>> 1;
        return mode < MaintenanceAlertCondition.values().length ? MaintenanceAlertCondition.values()[mode]
                : MaintenanceAlertCondition.ISSUE_1;
    }

    public void setMaintenanceAlertCondition(MaintenanceAlertCondition condition) {
        if (condition != null) {
            setGuiVariable((getVariable() & 1) | (condition.ordinal() << 1));
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
        return new CoverNeedMaintenanceGui(this);
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        boolean needsRepair = false;
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity tTileEntity = (IGregTechTileEntity) tile;
            IMetaTileEntity mTileEntity = tTileEntity.getMetaTileEntity();
            if (mTileEntity instanceof GT_MetaTileEntity_MultiBlockBase) {
                GT_MetaTileEntity_MultiBlockBase multi = (GT_MetaTileEntity_MultiBlockBase) mTileEntity;
                int ideal = multi.getIdealStatus();
                int real = multi.getRepairStatus();
                ItemStack tRotor = multi.getRealInventory()[1];
                int coverVar = coverData >>> 1;
                if (coverVar < 5) {
                    if (ideal - real > coverVar) {
                        needsRepair = true;
                    }
                } else if (coverVar == 5 || coverVar == 6) {
                    if (isRotor(tRotor)) {
                        long tMax = GT_MetaGenerated_Tool.getToolMaxDamage(tRotor);
                        long tCur = GT_MetaGenerated_Tool.getToolDamage(tRotor);
                        if (coverVar == 5) {
                            needsRepair = (tCur >= tMax * 8 / 10);
                        } else {
                            long mExpectedDamage = Math.round(Math.min(multi.mEUt / multi.damageFactorLow,
                                    Math.pow(multi.mEUt, multi.damageFactorHigh)));
                            needsRepair = tCur + mExpectedDamage * 2 >= tMax;
                        }
                    } else {
                        needsRepair = true;
                    }
                }
            }
        }
        if (coverData % 2 == 0) {
            needsRepair = !needsRepair;
        }
        tile.setOutputRedstoneSignal(side, (byte) (needsRepair ? 0 : 15));
        tile.setOutputRedstoneSignal(GT_Utility.getOppositeSide(side), (byte) (needsRepair ? 0 : 15));
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 14;
        if (coverData < 0) {
            coverData = 13;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_056", "Emit if 1 Maintenance Needed"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_057", "Emit if 1 Maintenance Needed(inverted)"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_058", "Emit if 2 Maintenance Needed"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_059", "Emit if 2 Maintenance Needed(inverted)"));
            break;

        case 4:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_060", "Emit if 3 Maintenance Needed"));
            break;

        case 5:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_061", "Emit if 3 Maintenance Needed(inverted)"));
            break;

        case 6:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_062", "Emit if 4 Maintenance Needed"));
            break;

        case 7:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_063", "Emit if 4 Maintenance Needed(inverted)"));
            break;

        case 8:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_064", "Emit if 5 Maintenance Needed"));
            break;

        case 9:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_065", "Emit if 5 Maintenance Needed(inverted)"));
            break;

        case 10:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_066", "Emit if rotor needs maintenance low accuracy mod"));
            break;

        case 11:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_067", "Emit if rotor needs maintenance low accuracy mod(inverted)"));
            break;

        case 12:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_068", "Emit if rotor needs maintenance high accuracy mod"));
            break;

        case 13:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_069",
                            "Emit if rotor needs maintenance high accuracy mod(inverted)"));
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
        return 60;
    }

    private boolean isRotor(ItemStack aRotor) {
        return !(aRotor == null || !(aRotor.getItem() instanceof GT_MetaGenerated_Tool) || aRotor.getItemDamage() < 170
                || aRotor.getItemDamage() > 176);
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
