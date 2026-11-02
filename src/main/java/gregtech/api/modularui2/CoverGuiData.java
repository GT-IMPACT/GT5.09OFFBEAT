package gregtech.api.modularui2;

import com.cleanroommc.modularui.factory.SidedPosGuiData;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.common.covers.Cover;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/** GTNH context shared by standalone cover windows and cover popups. */
public final class CoverGuiData extends SidedPosGuiData {

    private final int coverID;
    private final boolean popup;

    public CoverGuiData(
            EntityPlayer player,
            int coverID,
            int x,
            int y,
            int z,
            ForgeDirection side,
            boolean popup
    ) {
        super(player, x, y, z, side);
        if (side == null || side == ForgeDirection.UNKNOWN) {
            throw new IllegalArgumentException("A cover must have a valid side");
        }
        this.coverID = coverID;
        this.popup = popup;
    }

    public int getCoverID() {
        return coverID;
    }

    public ICoverable getCoverable() {
        TileEntity tile = getTileEntity();
        if (!(tile instanceof ICoverable)) {
            throw new IllegalStateException("Cover host is no longer present");
        }
        return (ICoverable) tile;
    }

    public Cover getCover() {
        Cover cover = getCoverable().getCoverAtSide(getSide());
        if (cover.getCoverID() != coverID) {
            throw new IllegalStateException("Cover was replaced while its window was open");
        }
        return cover;
    }

    public boolean isPopUp() {
        return popup;
    }
}
