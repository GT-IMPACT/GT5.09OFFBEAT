package gregtech.api.metatileentity;

import java.util.Arrays;

import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.GT_Values;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.net.GT_Packet_CoverData;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverLegacyBehavior;
import gregtech.common.covers.NativeCoverBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

/** GTNH cover ownership, persistence and lifecycle shared by machines and pipes. */
public abstract class CoverableTileEntity extends BaseTileEntity implements ICoverable {

    public static final String COVERS_NBT_KEY = "gt.covers";
    protected final Cover[] covers = new Cover[6];
    private final GT_CoverBehavior[] compatibilityBehaviors = new GT_CoverBehavior[6];
    private boolean coversUnloaded;
    private boolean coversDestroyed;

    protected CoverableTileEntity() {
        Arrays.fill(covers, CoverRegistry.NO_COVER);
    }

    @Override
    public final Cover getCoverAtSide(ForgeDirection side) {
        return side == null || side == ForgeDirection.UNKNOWN ? CoverRegistry.NO_COVER : covers[side.ordinal()];
    }

    @Override
    public final boolean hasCoverAtSide(ForgeDirection side) {
        return getCoverAtSide(side).getCoverID() != 0;
    }

    @Override
    public final void attachCover(Cover cover) {
        if (cover == null || cover.getSide() == null || cover.getSide() == ForgeDirection.UNKNOWN
            || cover.getTile() != this) {
            throw new IllegalArgumentException("Cover must belong to this tile and a valid side");
        }
        ForgeDirection side = cover.getSide();
        Cover previous = getCoverAtSide(side);
        if (previous == cover) {
            return;
        }
        if (previous.getCoverID() != 0) {
            previous.onCoverRemoval();
        }
        covers[side.ordinal()] = cover;
        compatibilityBehaviors[side.ordinal()] = null;
        cover.setNeedsUpdate(true);
        coversUnloaded = false;
        coversDestroyed = false;
        coverStateChanged(side);
    }

    @Override
    public final ItemStack detachCover(ForgeDirection side) {
        Cover cover = getCoverAtSide(side);
        if (cover.getCoverID() == 0) {
            return null;
        }
        ItemStack item = cover.asItemStack();
        cover.onCoverRemoval();
        covers[side.ordinal()] = CoverRegistry.NO_COVER;
        compatibilityBehaviors[side.ordinal()] = null;
        coverStateChanged(side);
        return item;
    }

    protected final GT_CoverBehavior getCompatibleCoverBehavior(byte side) {
        if (side < 0 || side >= covers.length) {
            return gregtech.api.GregTech_API.sNoBehavior;
        }
        Cover cover = covers[side];
        if (cover.getCoverID() == 0) {
            return gregtech.api.GregTech_API.sNoBehavior;
        }
        if (cover instanceof CoverLegacyBehavior) {
            return ((CoverLegacyBehavior) cover).getBehavior();
        }
        if (compatibilityBehaviors[side] == null) {
            compatibilityBehaviors[side] = new NativeCoverBehavior(cover);
        }
        return compatibilityBehaviors[side];
    }

    protected final void setAttachedCoverID(byte side, int id) {
        if (side < 0 || side >= covers.length) {
            return;
        }
        if (id == 0) {
            detachCover(ForgeDirection.getOrientation(side));
        } else {
            attachCover(CoverRegistry.buildCover(id, ForgeDirection.getOrientation(side), this));
        }
    }

    protected final void setAttachedCoverData(byte side, int value) {
        if (side < 0 || side >= covers.length || covers[side].getCoverID() == 0) {
            return;
        }
        covers[side].setLegacyData(value);
        covers[side].setNeedsUpdate(true);
        syncLegacyCoverSnapshots();
        markDirty();
        markCoverChunkDirty();
    }

    protected final void readCoverNBT(NBTTagCompound nbt, int[] ids, int[] legacyData) {
        Arrays.fill(covers, CoverRegistry.NO_COVER);
        Arrays.fill(compatibilityBehaviors, null);
        if (nbt != null && nbt.hasKey(COVERS_NBT_KEY, 9)) {
            NBTTagList entries = nbt.getTagList(COVERS_NBT_KEY, 10);
            for (int index = 0; index < entries.tagCount(); index++) {
                NBTTagCompound entry = entries.getCompoundTagAt(index);
                int side = entry.getByte("s");
                if (side >= 0 && side < covers.length) {
                    covers[side] = CoverRegistry.buildCoverFromNbt(entry, ForgeDirection.getOrientation(side), this);
                }
            }
        } else {
            for (int side = 0; side < covers.length; side++) {
                int id = side < ids.length ? ids[side] : 0;
                if (id != 0) {
                    Cover cover = CoverRegistry.buildCover(id, ForgeDirection.getOrientation(side), this);
                    cover.setLegacyData(side < legacyData.length ? legacyData[side] : 0);
                    covers[side] = cover;
                }
            }
        }
        coversUnloaded = false;
        coversDestroyed = false;
        syncLegacyCoverSnapshots();
    }

    protected final void writeCoverNBT(NBTTagCompound nbt, boolean isDrop) {
        syncLegacyCoverSnapshots();
        NBTTagList entries = new NBTTagList();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            Cover cover = getCoverAtSide(side);
            if (cover.getCoverID() != 0) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte("s", (byte) side.ordinal());
                CoverRegistry.writeCoverToNbt(cover, entry);
                entries.appendTag(entry);
            }
        }
        if (entries.tagCount() > 0 || !isDrop) {
            nbt.setTag(COVERS_NBT_KEY, entries);
        } else {
            nbt.removeTag(COVERS_NBT_KEY);
        }
    }

    /** Legacy texture packets can change IDs without replacing an unchanged cover's synchronized state. */
    protected final void receiveCoverIDs(int[] ids) {
        for (int side = 0; side < covers.length; side++) {
            if (covers[side].getCoverID() != ids[side]) {
                Cover previous = covers[side];
                if (previous.getCoverID() != 0) {
                    previous.onCoverRemoval();
                }
                covers[side] = CoverRegistry.buildCover(ids[side], ForgeDirection.getOrientation(side), this);
                compatibilityBehaviors[side] = null;
            }
        }
        syncLegacyCoverSnapshots();
    }

    @Override
    public final void updateAttachedCover(int id, ForgeDirection side, NBTTagCompound nbt) {
        Cover cover = getCoverAtSide(side);
        if (worldObj != null && worldObj.isRemote && nbt != null && cover.getCoverID() == id && id != 0) {
            cover.readFromNbt(nbt);
            syncLegacyCoverSnapshots();
            issueTextureUpdate();
        }
    }

    protected final void tickCoverAtSide(byte side, byte redstone, long timer) {
        Cover cover = covers[side];
        int rate = cover.getTickRate();
        if (cover.isValid() && rate > 0 && timer % rate == 0) {
            cover.doCoverThings(cover.isRedstoneSensitive(timer) ? redstone : 0, timer);
            syncLegacyCoverSnapshots();
        }
        if (timer == 11 || cover.needsUpdate()) {
            sendCoverData(cover);
        }
    }

    private void sendCoverData(Cover cover) {
        if (cover.getCoverID() != 0 && worldObj != null && !worldObj.isRemote && GT_Values.NW != null) {
            GT_Values.NW.sendPacketToAllPlayersInRange(worldObj, new GT_Packet_CoverData(this, cover), xCoord, zCoord);
            cover.setNeedsUpdate(false);
        }
    }

    /** Send compound state after the legacy ID snapshot so clients can match every attachment. */
    protected final void sendCoverDataIfNeeded() {
        for (Cover cover : covers) {
            if (cover.getCoverID() != 0 && (cover.useMui2() || cover.isDataNeededOnClient())) {
                sendCoverData(cover);
            }
        }
    }

    protected final void markCoverForUpdate(byte side) {
        if (side >= 0 && side < covers.length) {
            covers[side].setNeedsUpdate(true);
        }
    }

    private void coverStateChanged(ForgeDirection side) {
        syncLegacyCoverSnapshots();
        markDirty();
        markCoverChunkDirty();
        if (worldObj != null) {
            issueCoverUpdate((byte) side.ordinal());
            issueBlockUpdate();
        }
    }

    protected abstract void syncLegacyCoverSnapshots();

    public abstract void issueTextureUpdate();

    private void markCoverChunkDirty() {
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
        }
    }

    @Override
    public void validate() {
        coversUnloaded = false;
        coversDestroyed = false;
        super.validate();
    }

    @Override
    public void onChunkUnload() {
        unloadCovers();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        unloadCovers();
        super.invalidate();
    }

    private void unloadCovers() {
        if (!coversUnloaded) {
            coversUnloaded = true;
            for (Cover cover : covers) {
                if (cover.getCoverID() != 0) {
                    cover.onCoverUnload();
                }
            }
        }
    }

    protected final void destroyCovers() {
        if (!coversDestroyed) {
            coversDestroyed = true;
            for (Cover cover : covers) {
                if (cover.getCoverID() != 0) {
                    cover.onBaseTEDestroyed();
                }
            }
        }
    }

    public final void onBaseTileDestroyed() {
        destroyCovers();
    }
}
