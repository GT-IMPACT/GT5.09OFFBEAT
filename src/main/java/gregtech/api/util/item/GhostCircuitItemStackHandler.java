package gregtech.api.util.item;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.GT_Utility;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** GTNH phantom inventory view containing one non-transferable configuration circuit. */
public class GhostCircuitItemStackHandler implements IItemHandlerModifiable {

    public static final int NO_CONFIG = -1;
    public static final int MAX_CONFIG = GT_IntegratedCircuit_Item.MAX_CONFIG;

    private final IItemHandlerModifiable inventory;
    private final int circuitSlot;

    public GhostCircuitItemStackHandler(IMetaTileEntity machine) {
        if (!(machine instanceof IConfigurationCircuitSupport)) {
            throw new IllegalArgumentException("Machine does not support virtual configuration circuits");
        }
        IConfigurationCircuitSupport support = (IConfigurationCircuitSupport) machine;
        inventory = machine.getInventoryHandler();
        circuitSlot = support.getCircuitSlot();
        if (!support.allowSelectCircuit() || inventory == null || circuitSlot < 0
            || circuitSlot >= inventory.getSlots()) {
            throw new IllegalArgumentException("Virtual circuit slot is unavailable");
        }
    }

    public boolean hasCircuit() {
        ItemStack stack = inventory.getStackInSlot(circuitSlot);
        return stack != null && isItemValid(0, stack);
    }

    public int getCircuitConfig() {
        return hasCircuit() ? inventory.getStackInSlot(circuitSlot).getItemDamage() : NO_CONFIG;
    }

    public void setCircuitConfig(int config) {
        if (config == NO_CONFIG) {
            inventory.setStackInSlot(circuitSlot, null);
        } else if (config >= 0 && config <= MAX_CONFIG) {
            ItemStack stack = GT_Utility.getIntegratedCircuit(config);
            if (stack == null) {
                throw new IllegalStateException("Integrated circuit item is not registered");
            }
            stack.stackSize = 0;
            inventory.setStackInSlot(circuitSlot, stack);
        } else {
            throw new IllegalArgumentException("Invalid circuit configuration: " + config);
        }
    }

    public static List<ItemStack> getAllConfigurations() {
        List<ItemStack> configurations = new ArrayList<>(MAX_CONFIG + 1);
        for (int config = 0; config <= MAX_CONFIG; config++) {
            ItemStack stack = GT_Utility.getIntegratedCircuit(config);
            if (stack == null) {
                throw new IllegalStateException("Integrated circuit item is not registered");
            }
            stack.stackSize = 1;
            configurations.add(stack);
        }
        return configurations;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlot(slot);
        if (stack == null) {
            inventory.setStackInSlot(circuitSlot, null);
        } else if (isItemValid(slot, stack)) {
            ItemStack phantom = stack.copy();
            phantom.stackSize = 0;
            inventory.setStackInSlot(circuitSlot, phantom);
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        return inventory.getStackInSlot(circuitSlot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlot(slot);
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlot(slot);
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlot(slot);
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlot(slot);
        return stack == null || isCircuit(stack) && stack.getItemDamage() >= 0 && stack.getItemDamage() <= MAX_CONFIG;
    }

    private static boolean isCircuit(ItemStack stack) {
        return stack.getItem() instanceof GT_IntegratedCircuit_Item;
    }

    private static void validateSlot(int slot) {
        if (slot != 0) {
            throw new IndexOutOfBoundsException("Virtual circuit slot index must be zero: " + slot);
        }
    }
}
