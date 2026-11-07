package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.cover.CoverArmGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;

public class CoverArm extends CoverLegacyData {

    private static final int EXPORT_MASK = 0x40000000;
    private static final int SLOT_ID_MASK = 0x3FFF;

    public CoverArm(CoverContext context, int tickRate) {
        super(context);
        this.mTickRate = tickRate;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverArmGui(this);
    }

    @Override
    public int getVariable() {
        int variable = super.getVariable();
        // Match the pre-5.09.34.52 conversion in GT_Cover_Arm before interpreting GUI bit fields.
        if ((variable >>> 29) == 0) {
            return CONVERTED_BIT | (((variable + 1) & SLOT_ID_MASK) << 14) | EXPORT_MASK;
        } else if ((variable >>> 29) == 7) {
            return CONVERTED_BIT | Math.min(Math.abs(variable - 1), SLOT_ID_MASK);
        }
        return variable;
    }

    public boolean isExport() {
        return (getVariable() & EXPORT_MASK) != 0;
    }

    public void setExport(boolean export) {
        if (isValid()) {
            setGuiVariable((getVariable() & ~EXPORT_MASK) | (export ? EXPORT_MASK : 0) | CONVERTED_BIT);
        }
    }

    public int getInternalSlotId() {
        return getVariable() & SLOT_ID_MASK;
    }

    public void setInternalSlotId(int slot) {
        if (isValid()) {
            int boundedSlot = Math.max(0, Math.min(slot, Math.min(getTile().getSizeInventory(), SLOT_ID_MASK)));
            setGuiVariable((getVariable() & ~SLOT_ID_MASK) | boundedSlot | CONVERTED_BIT);
        }
    }

    public int getExternalSlotId() {
        return getVariable() >>> 14 & SLOT_ID_MASK;
    }

    public void setExternalSlotId(int slot) {
        if (!isValid()) {
            return;
        }
        ICoverable tile = getTile();
        TileEntity adjacent = tile.getTileEntityAtSide((byte) getSide().ordinal());
        int maximum = adjacent instanceof IInventory ? ((IInventory) adjacent).getSizeInventory() : 0;
        int boundedSlot = Math.max(0, Math.min(slot, Math.min(maximum, SLOT_ID_MASK)));
        setGuiVariable((getVariable() & ~(SLOT_ID_MASK << 14)) | boundedSlot << 14 | CONVERTED_BIT);
    }

    private final int mTickRate;

    private static final int CONVERTED_BIT = 0x80000000;

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if ((((tile instanceof IMachineProgress)) && (!((IMachineProgress) tile).isAllowedToWork()))) {
            return;
        }
        if ((coverData >>> 29) == 0) {
            coverData = CONVERTED_BIT | (((coverData + 1) & SLOT_ID_MASK) << 14) | EXPORT_MASK;
        } else if ((coverData >>> 29) == 7) {
            coverData = CONVERTED_BIT | Math.min(Math.abs(coverData - 1), SLOT_ID_MASK);
        }
        boolean usePower = false;
        if (tile.getUniversalEnergyCapacity() >= 128L) {
            if (tile.isUniversalEnergyStored(256L)) {
                usePower = true;
            } else {
                return;
            }
        }
        TileEntity toTile;
        TileEntity fromTile;
        int toSlot;
        int fromSlot;
        if ((coverData & EXPORT_MASK) > 0) {
            fromTile = (TileEntity) tile;
            toTile = tile.getTileEntityAtSide(side);
            fromSlot = coverData & SLOT_ID_MASK;
            toSlot = (coverData >> 14) & SLOT_ID_MASK;
        } else {
            fromTile = tile.getTileEntityAtSide(side);
            toTile = (TileEntity) tile;
            fromSlot = (coverData >> 14) & SLOT_ID_MASK;
            toSlot = coverData & SLOT_ID_MASK;
        }
        byte movedItems = 0;
        if (fromSlot > 0 && toSlot > 0) {
            if (fromTile instanceof IInventory && toTile instanceof IInventory) {
                movedItems = GT_Utility.moveFromSlotToSlot(
                    (IInventory) fromTile,
                    (IInventory) toTile,
                    fromSlot - 1,
                    toSlot - 1,
                    null,
                    false,
                    (byte) 64,
                    (byte) 1,
                    (byte) 64,
                    (byte) 1
                );
            }
        } else if (toSlot > 0) {
            byte transferSide;
            if ((coverData & EXPORT_MASK) > 0) {
                transferSide = side;
            } else {
                transferSide = GT_Utility.getOppositeSide(side);
            }
            movedItems = GT_Utility.moveOneItemStackIntoSlot(
                fromTile,
                toTile,
                transferSide,
                toSlot - 1,
                null,
                false,
                (byte) 64,
                (byte) 1,
                (byte) 64,
                (byte) 1
            );
        } else if (fromSlot > 0) {
            byte toSide;
            if ((coverData & EXPORT_MASK) > 0) {
                toSide = side;
            } else {
                toSide = GT_Utility.getOppositeSide(side);
            }
            if (fromTile instanceof IInventory) {
                movedItems = GT_Utility.moveFromSlotToSide(
                    (IInventory) fromTile,
                    toTile,
                    fromSlot - 1,
                    toSide,
                    null,
                    false,
                    (byte) 64,
                    (byte) 1,
                    (byte) 64,
                    (byte) 1
                );
            }
        } else {
            byte fromSide;
            byte toSide;
            if ((coverData & EXPORT_MASK) > 0) {
                fromSide = side;
                toSide = GT_Utility.getOppositeSide(side);
            } else {
                fromSide = GT_Utility.getOppositeSide(side);
                toSide = side;
            }
            movedItems = GT_Utility.moveOneItemStack(
                fromTile,
                toTile,
                fromSide,
                toSide,
                null,
                false,
                (byte) 64,
                (byte) 1,
                (byte) 64,
                (byte) 1
            );
        }
        if (usePower) {
            tile.decreaseStoredEnergyUnits(4 * movedItems, true);
        }
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        byte side = (byte) coverSide.ordinal();
        int step = 0;
        if (GT_Utility.getClickedFacingCoords(side, x, y, z)[0] >= 0.5F) {
            step += player.isSneaking() ? 256 : 16;
        } else {
            step -= player.isSneaking() ? 256 : 16;
        }
        coverData = getNewVar(coverData, step);
        sendMessageToPlayer(player, coverData);
        markGuiChanged();
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        int step = (GT_Utility.getClickedFacingCoords(side, x, y, z)[0] >= 0.5F) ? 1 : -1;
        coverData = getNewVar(coverData, step);
        sendMessageToPlayer(player, coverData);
        markGuiChanged();
        return true;
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
        return true;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return true;
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return this.mTickRate;
    }

    private void sendMessageToPlayer(EntityPlayer aPlayer, int var) {
        if ((var & EXPORT_MASK) != 0) {
            GT_Utility.sendChatToPlayer(aPlayer,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_001",
                            "Puts out into adjacent Slot #") + (((var >> 14) & SLOT_ID_MASK) - 1));
        } else {
            GT_Utility.sendChatToPlayer(aPlayer,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_002",
                            "Grabs in for own Slot #") + ((var & SLOT_ID_MASK) - 1));
        }
    }

    private int getNewVar(int var, int step) {
        int intSlot = (var & SLOT_ID_MASK);
        int adjSlot = (var >> 14) & SLOT_ID_MASK;
        if ((var & EXPORT_MASK) == 0) {
            int x = (intSlot + step);
            if (x > SLOT_ID_MASK) {
                return createVar(0, SLOT_ID_MASK, 0);
            } else if (x < 1) {
                return createVar(-step - intSlot + 1, 0, EXPORT_MASK);
            } else {
                return createVar(0, x, 0);
            }
        } else {
            int x = (adjSlot - step);
            if (x > SLOT_ID_MASK) {
                return createVar(SLOT_ID_MASK, 0, EXPORT_MASK);
            } else if (x < 1) {
                return createVar(0, +step - adjSlot + 1, 0);
            } else {
                return createVar(x, 0, EXPORT_MASK);
            }
        }
    }

    private int createVar(int adjSlot, int intSlot, int export) {
        return CONVERTED_BIT | export | ((adjSlot & SLOT_ID_MASK) << 14) | (intSlot & SLOT_ID_MASK);
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
