package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.modes.PlayerDetectionMode;
import gregtech.common.gui.modularui.cover.CoverPlayerDetectorGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fluids.Fluid;

public class CoverPlayerDetector extends CoverLegacyData {

    public CoverPlayerDetector(CoverContext context) {
        super(context);
    }

    public PlayerDetectionMode getPlayerDetectionMode() {
        switch (getVariable()) {
        case 1:
            return PlayerDetectionMode.ONLY_OWNER;
        case 2:
            return PlayerDetectionMode.OTHER_PLAYERS;
        default:
            return PlayerDetectionMode.ANY_PLAYER;
        }
    }

    public void setPlayerDetectionMode(PlayerDetectionMode mode) {
        if (mode == null) {
            return;
        }
        // The existing backend uses 1 for owner and 2 for others, opposite to the old GUI labels.
        switch (mode) {
        case ONLY_OWNER:
            setGuiVariable(1);
            break;
        case OTHER_PLAYERS:
            setGuiVariable(2);
            break;
        default:
            setGuiVariable(0);
            break;
        }
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverPlayerDetectorGui(this);
    }

    private String placer = "";

    private int range = 8;

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        boolean playerDetected = false;
        if (tile instanceof IGregTechTileEntity) {
            if (tile.isUniversalEnergyStored(20)) {
                tile.decreaseStoredEnergyUnits(20, true);
                range = 32;
            } else {
                range = 8;
            }
            placer = ((IGregTechTileEntity) tile).getOwnerName();
        }
        for (Object tObject : tile.getWorld().playerEntities) {
            if ((tObject instanceof EntityPlayerMP)) {
                EntityPlayerMP tEntity = (EntityPlayerMP) tObject;
                int dist = Math.max(1, (int) tEntity.getDistance(tile.getXCoord() + 0.5, tile.getYCoord() + 0.5,
                        tile.getZCoord() + 0.5));
                if (dist < range) {
                    if (coverData == 0) {
                        playerDetected = true;
                        break;
                    }
                    if (tEntity.getDisplayName().equalsIgnoreCase(placer)) {
                        if (coverData == 1) {
                            playerDetected = true;
                            break;
                        }
                    } else if (coverData == 2) {
                        playerDetected = true;
                        break;
                    }
                }
            }
        }
        tile.setOutputRedstoneSignal(side, (byte) (playerDetected ? 15 : 0));
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 3;
        if (coverData < 0) {
            coverData = 2;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_068", "Emit if any Player is close"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_069", "Emit if other Player is close"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_070", "Emit if you are close"));
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
        return 20;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
