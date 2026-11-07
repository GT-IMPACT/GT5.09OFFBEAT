package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_Utility;

public class CoverVent extends CoverLegacyData {

    private final int mEfficiency;

    public CoverVent(CoverContext context, int efficiency) {
        super(context);
        this.mEfficiency = efficiency;
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if ((tile instanceof IMachineProgress)) {
            if ((((IMachineProgress) tile).hasThingsToDo()) && (coverData != ((IMachineProgress) tile).getProgress())
                    && (!GT_Utility.hasBlockHitBox(tile.getWorld(), tile.getOffsetX(side, 1), tile.getOffsetY(side, 1),
                            tile.getOffsetZ(side, 1)))) {
                ((IMachineProgress) tile).increaseProgress(this.mEfficiency);
            }
            coverData = ((IMachineProgress) tile).getProgress();
            return;
        }
        coverData = 0;
        return;
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return 100;
    }
}
