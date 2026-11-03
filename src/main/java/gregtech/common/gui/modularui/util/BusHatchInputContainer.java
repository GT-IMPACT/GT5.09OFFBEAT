package gregtech.common.gui.modularui.util;

import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Keeps the combined hatch's legacy shift destinations while MUI2 performs the actual transfer. */
public class BusHatchInputContainer extends ModularContainer {

    private List<ModularSlot> transferTargets;

    @Override
    protected ItemStack transferItem(ModularSlot fromSlot, ItemStack fromStack) {
        boolean fromPlayer = ModularSlot.isPlayerSlot(fromSlot);
        boolean filledContainer = fromPlayer && GT_Utility.getFluidForFilledItem(fromStack, true) != null;
        List<ModularSlot> targets = new ArrayList<>();
        for (ModularSlot slot : super.getShiftClickSlots()) {
            if (ModularSlot.isPlayerSlot(slot) == fromPlayer) {
                continue;
            }
            if (fromPlayer && slot.getSlotIndex() == 0 && !filledContainer) {
                continue;
            }
            targets.add(slot);
        }
        if (!fromPlayer) {
            // Vanilla's reverse inventory range fills the hotbar right to left, then the main inventory.
            targets.sort(Comparator.comparingInt((ModularSlot slot) -> slot.getSlotIndex() < 9 ? 0 : 1)
                .thenComparing(Comparator.comparingInt(ModularSlot::getSlotIndex).reversed()));
        }
        transferTargets = Collections.unmodifiableList(targets);
        try {
            return super.transferItem(fromSlot, fromStack);
        } finally {
            transferTargets = null;
        }
    }

    @Override
    public List<ModularSlot> getShiftClickSlots() {
        return transferTargets == null ? super.getShiftClickSlots() : transferTargets;
    }
}
