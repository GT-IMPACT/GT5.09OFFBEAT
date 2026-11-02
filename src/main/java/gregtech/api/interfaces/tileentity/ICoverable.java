package gregtech.api.interfaces.tileentity;

import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.covers.CoverRegistry;
import gregtech.common.covers.Cover;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public interface ICoverable extends IRedstoneTileEntity, IHasInventory, IBasicEnergyContainer {

    default Cover getCoverAtSide(ForgeDirection side) {
        return CoverRegistry.NO_COVER;
    }

    default boolean hasCoverAtSide(ForgeDirection side) {
        return getCoverAtSide(side).getCoverID() != 0;
    }

    default void attachCover(Cover cover) {
        throw new UnsupportedOperationException("This coverable does not own native cover objects");
    }

    default ItemStack detachCover(ForgeDirection side) {
        throw new UnsupportedOperationException("This coverable does not own native cover objects");
    }

    default ItemStack getCoverItemAtSide(ForgeDirection side) {
        return getCoverAtSide(side).asItemStack();
    }

    default void issueCoverUpdate(ForgeDirection side) {
        issueCoverUpdate((byte) side.ordinal());
    }

    default void dropCover(ForgeDirection side, ForgeDirection droppedSide) {
        if (side != null && side != ForgeDirection.UNKNOWN
                && droppedSide != null && droppedSide != ForgeDirection.UNKNOWN) {
            dropCover((byte) side.ordinal(), (byte) droppedSide.ordinal(), true);
        }
    }

    default byte getInternalInputRedstoneSignal(ForgeDirection side) {
        if (side == null || side == ForgeDirection.UNKNOWN) {
            return 0;
        }
        return getInternalInputRedstoneSignal((byte) side.ordinal());
    }

    default void setInternalOutputRedstoneSignal(ForgeDirection side, byte strength) {
        if (side != null && side != ForgeDirection.UNKNOWN) {
            setInternalOutputRedstoneSignal((byte) side.ordinal(), strength);
        }
    }

    /** Render-state updates are accepted only by the client-side attachment. */
    default void updateAttachedCover(int coverID, ForgeDirection side, NBTTagCompound state) {
        Cover cover = getCoverAtSide(side);
        if (state != null && getWorld() != null && isClientSide() && cover.getCoverID() == coverID && coverID != 0) {
            cover.readFromNbt(state);
        }
    }

    public boolean canPlaceCoverIDAtSide(byte aSide, int aID);

    public boolean canPlaceCoverItemAtSide(byte aSide, ItemStack aCover);

    public boolean dropCover(byte aSide, byte aDroppedSide, boolean aForced);

    @Deprecated
    public void setCoverDataAtSide(byte aSide, int aData);

    @Deprecated
    public void setCoverIDAtSide(byte aSide, int aID);

    public void setCoverItemAtSide(byte aSide, ItemStack aCover);

    @Deprecated
    public int getCoverDataAtSide(byte aSide);

    @Deprecated
    public int getCoverIDAtSide(byte aSide);

    public ItemStack getCoverItemAtSide(byte aSide);

    @Deprecated
    public GT_CoverBehavior getCoverBehaviorAtSide(byte aSide);

    /**
     * For use by the regular MetaTileEntities. Returns the Cover Manipulated input Redstone.
     * Don't use this if you are a Cover Behavior. Only for MetaTileEntities.
     */
    public byte getInternalInputRedstoneSignal(byte aSide);

    /**
     * For use by the regular MetaTileEntities. This makes it not conflict with Cover based Redstone Signals.
     * Don't use this if you are a Cover Behavior. Only for MetaTileEntities.
     */
    public void setInternalOutputRedstoneSignal(byte aSide, byte aStrength);

    /**
     * Causes a general Cover Texture update.
     * Sends 6 Integers to Client + causes @issueTextureUpdate()
     */
    public void issueCoverUpdate(byte aSide);

    /**
     * Receiving a packet with cover data.
     */
    @Deprecated
    void receiveCoverData(byte coverSide, int coverID, int coverData);
}
