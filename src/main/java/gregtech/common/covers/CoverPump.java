package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.gui.modularui.cover.base.CoverIOBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

public class CoverPump extends CoverIOBase {

    public CoverPump(CoverContext context, int transferRate) {
        super(context);
        this.mTransferRate = transferRate;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverIOBaseGui(this, "cover.pump");
    }

    private final int mTransferRate;

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if ((coverData % 6 > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork() != coverData % 6 < 4) {
                return;
            }
        }
        if ((tile instanceof IFluidHandler)) {
            IFluidHandler tTank2 = tile.getITankContainerAtSide(side);
            if (tTank2 != null) {
                IFluidHandler tTank1 = (IFluidHandler) tile;
                if (coverData % 2 == 0) {
                    FluidStack tLiquid = tTank1.drain(ForgeDirection.getOrientation(side), this.mTransferRate, false);
                    if (tLiquid != null) {
                        tLiquid = tLiquid.copy();
                        tLiquid.amount = tTank2.fill(ForgeDirection.getOrientation(side).getOpposite(), tLiquid, false);
                        if (tLiquid.amount > 0) {
                            if (((coverData % 2 == 0) || (side != 1)) && ((coverData % 2 != 0) || (side != 0))
                                    && (tile.getUniversalEnergyCapacity() >= Math.min(1, tLiquid.amount / 10))) {
                                if (tile.isUniversalEnergyStored(Math.min(1, tLiquid.amount / 10))) {
                                    tile.decreaseStoredEnergyUnits(Math.min(1, tLiquid.amount / 10), true);
                                    tTank2.fill(ForgeDirection.getOrientation(side).getOpposite(),
                                            tTank1.drain(ForgeDirection.getOrientation(side), tLiquid.amount, true),
                                            true);
                                }
                            } else {
                                tTank2.fill(ForgeDirection.getOrientation(side).getOpposite(),
                                        tTank1.drain(ForgeDirection.getOrientation(side), tLiquid.amount, true), true);
                            }
                        }
                    }
                } else {
                    FluidStack tLiquid = tTank2.drain(ForgeDirection.getOrientation(side).getOpposite(),
                            this.mTransferRate, false);
                    if (tLiquid != null) {
                        tLiquid = tLiquid.copy();
                        tLiquid.amount = tTank1.fill(ForgeDirection.getOrientation(side), tLiquid, false);
                        if (tLiquid.amount > 0) {
                            if (((coverData % 2 == 0) || (side != 1)) && ((coverData % 2 != 0) || (side != 0))
                                    && (tile.getUniversalEnergyCapacity() >= Math.min(1, tLiquid.amount / 10))) {
                                if (tile.isUniversalEnergyStored(Math.min(1, tLiquid.amount / 10))) {
                                    tile.decreaseStoredEnergyUnits(Math.min(1, tLiquid.amount / 10), true);
                                    tTank1.fill(ForgeDirection.getOrientation(side),
                                            tTank2.drain(ForgeDirection.getOrientation(side).getOpposite(),
                                                    tLiquid.amount, true),
                                            true);
                                }
                            } else {
                                tTank1.fill(ForgeDirection.getOrientation(side), tTank2.drain(
                                        ForgeDirection.getOrientation(side).getOpposite(), tLiquid.amount, true), true);
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 12;
        if (coverData < 0) {
            coverData = 11;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_006", "Export"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_007", "Import"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_008", "Export (conditional)"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_009", "Import (conditional)"));
            break;

        case 4:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_010", "Export (invert cond)"));
            break;

        case 5:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_011", "Import (invert cond)"));
            break;

        case 6:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_012", "Export allow Input"));
            break;

        case 7:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_013", "Import allow Output"));
            break;

        case 8:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_014", "Export allow Input (conditional)"));
            break;

        case 9:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_015", "Import allow Output (conditional)"));
            break;

        case 10:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_016", "Export allow Input (invert cond)"));
            break;

        case 11:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_017", "Import allow Output (invert cond)"));
            break;

        }
        markGuiChanged();
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return true;
    }

    @Override
    public boolean letsRedstoneGoOut() {
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
    public boolean letsItemsIn(int slot) {
        return true;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return true;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        ICoverable tile = getTile();
        if ((coverData > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork() != coverData % 6 < 4) {
                return false;
            }
        }
        return (coverData >= 6) || (coverData % 2 != 0);
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        ICoverable tile = getTile();
        if ((coverData > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork() != coverData % 6 < 4) {
                return false;
            }
        }
        return (coverData >= 6) || (coverData % 2 == 0);
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return 1;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
