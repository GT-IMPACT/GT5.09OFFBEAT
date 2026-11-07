package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.gui.modularui.cover.base.CoverIOBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;

public class CoverConveyor extends CoverIOBase {

    public CoverConveyor(CoverContext context, int tickRate, int maxStacks) {
        super(context);
        this.mTickRate = tickRate;
        this.mMaxStacks = maxStacks;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverIOBaseGui(this, "cover.conveyor");
    }

    private final int mTickRate;

    private final int mMaxStacks;

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if ((coverData % 6 > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork() != coverData % 6 < 4) {
                return;
            }
        }
        TileEntity tTileEntity = tile.getTileEntityAtSide(side);
        Object fromEntity = coverData % 2 == 0 ? tile : tTileEntity;
        Object toEntity = coverData % 2 != 0 ? tile : tTileEntity;
        byte fromSide = coverData % 2 != 0 ? GT_Utility.getOppositeSide(side) : side;
        byte toSide = coverData % 2 == 0 ? GT_Utility.getOppositeSide(side) : side;
        boolean costsEnergy = ((coverData % 2 == 0) || (side != 1)) && ((coverData % 2 != 0) || (side != 0))
                && (tile.getUniversalEnergyCapacity() >= 128L);
        byte moved;
        for (int i = 0; i < this.mMaxStacks; i++) {
            if ((costsEnergy && !tile.isUniversalEnergyStored(256L))) {
                break;
            }
            moved = GT_Utility.moveOneItemStack(
                fromEntity,
                toEntity,
                fromSide,
                toSide,
                null,
                false,
                (byte) 64,
                (byte) 1,
                (byte) 64,
                (byte) 1
            );
            if (moved == 0) {
                break;
            }
            if (costsEnergy) {
                tile.decreaseStoredEnergyUnits(4 * moved, true);
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
    public boolean letsFluidIn(Fluid fluid) {
        return true;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return true;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return (coverData >= 6) || (coverData % 2 != 0);
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return (coverData >= 6) || (coverData % 2 == 0);
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return this.mTickRate;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
