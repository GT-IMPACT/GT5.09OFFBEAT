package gregtech.api.covers;

import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/** Immutable attachment context; cover state belongs to the constructed Cover. */
public final class CoverContext {

    private final ItemStack coverItem;
    private final int coverID;
    private final ForgeDirection side;
    private final ICoverable coverable;

    public CoverContext(ItemStack coverItem, ForgeDirection side, ICoverable coverable) {
        this.coverItem = coverItem == null ? null : coverItem.copy();
        this.coverID = GT_Utility.stackToInt(coverItem);
        this.side = side;
        this.coverable = coverable;
    }

    public CoverContext(int coverID, ForgeDirection side, ICoverable coverable) {
        this.coverItem = GT_Utility.intToStack(coverID);
        this.coverID = coverID;
        this.side = side;
        this.coverable = coverable;
    }

    public ItemStack getCoverItem() {
        return coverItem == null ? null : coverItem.copy();
    }

    public int getCoverID() {
        return coverID;
    }

    public ForgeDirection getSide() {
        return side;
    }

    public ICoverable getCoverable() {
        return coverable;
    }
}

