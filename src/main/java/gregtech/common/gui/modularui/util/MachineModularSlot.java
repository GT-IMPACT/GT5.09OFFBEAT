package gregtech.common.gui.modularui.util;

import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.modularui2.GTGuis;

import java.util.function.BiFunction;

/** GTNH slot adapter that wakes machine processing when players change an inventory stack. */
public class MachineModularSlot extends ModularSlot {

    public MachineModularSlot(IItemHandler itemHandler, int index, IGregTechTileEntity base) {
        super(itemHandler, index);
        changeListener((stack, changed, client, initial) -> {
            if (!client && !initial) {
                base.setInventorySlotContents(index, stack);
                GTGuis.markDirty(base.getMetaTileEntity());
            }
        });
    }

    public static BiFunction<IItemHandler, Integer, MachineModularSlot> supplier(IGregTechTileEntity base) {
        return (itemHandler, index) -> new MachineModularSlot(itemHandler, index, base);
    }
}
