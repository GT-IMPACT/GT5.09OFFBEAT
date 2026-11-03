package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.gui.modularui.cover.CoverArmGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;

/** Typed MUI2 access to the existing arm's packed slot and direction data. */
public class CoverArm extends CoverLegacyBehavior {

    public CoverArm(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
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
            return GT_Cover_Arm.CONVERTED_BIT | (((variable + 1) & GT_Cover_Arm.SLOT_ID_MASK) << 14)
                | GT_Cover_Arm.EXPORT_MASK;
        } else if ((variable >>> 29) == 7) {
            return GT_Cover_Arm.CONVERTED_BIT | Math.min(Math.abs(variable - 1), GT_Cover_Arm.SLOT_ID_MASK);
        }
        return variable;
    }

    public boolean isExport() {
        return (getVariable() & GT_Cover_Arm.EXPORT_MASK) != 0;
    }

    public void setExport(boolean export) {
        if (isValid()) {
            setGuiVariable((getVariable() & ~GT_Cover_Arm.EXPORT_MASK)
                | (export ? GT_Cover_Arm.EXPORT_MASK : 0) | GT_Cover_Arm.CONVERTED_BIT);
        }
    }

    public int getInternalSlotId() {
        return getVariable() & GT_Cover_Arm.SLOT_ID_MASK;
    }

    public void setInternalSlotId(int slot) {
        if (isValid()) {
            int boundedSlot = Math.max(0, Math.min(slot,
                Math.min(getTile().getSizeInventory(), GT_Cover_Arm.SLOT_ID_MASK)));
            setGuiVariable((getVariable() & ~GT_Cover_Arm.SLOT_ID_MASK)
                | boundedSlot | GT_Cover_Arm.CONVERTED_BIT);
        }
    }

    public int getExternalSlotId() {
        return getVariable() >>> 14 & GT_Cover_Arm.SLOT_ID_MASK;
    }

    public void setExternalSlotId(int slot) {
        if (!isValid()) {
            return;
        }
        ICoverable tile = getTile();
        TileEntity adjacent = tile.getTileEntityAtSide((byte) getSide().ordinal());
        int maximum = adjacent instanceof IInventory ? ((IInventory) adjacent).getSizeInventory() : 0;
        int boundedSlot = Math.max(0, Math.min(slot, Math.min(maximum, GT_Cover_Arm.SLOT_ID_MASK)));
        setGuiVariable((getVariable() & ~(GT_Cover_Arm.SLOT_ID_MASK << 14))
            | boundedSlot << 14 | GT_Cover_Arm.CONVERTED_BIT);
    }
}
