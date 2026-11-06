package gregtech.common.gui.modularui.multiblock;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;

import static gregtech.api.gui.GT_Container_ProcessingMachine.BATCH_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_WIDTH;
import static gregtech.api.gui.GT_Container_ProcessingMachine.LOCK_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.NEI_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_Y;
import static gregtech.api.gui.GT_Container_ProcessingMachine.POWER_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.RECIPE_MAP_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.SEPARATION_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.VOIDING_SLOT;
import static gregtech.api.logic.ProcessingLogic.BATCH;
import static gregtech.api.logic.ProcessingLogic.LOCK_RECIPE;
import static gregtech.api.logic.ProcessingLogic.PROTECT_FLUIDS;
import static gregtech.api.logic.ProcessingLogic.PROTECT_ITEMS;
import static gregtech.api.logic.ProcessingLogic.SEPARATE_INPUTS;

/** The existing processing window's control positions and server actions. */
public final class ProcessingGuiControls {

    private static final int GUI_BORDER = 4;
    private static final int CONTROL_BANK_WIDTH = 36;
    private static final int LEFT_CONTROL_X = GUI_BORDER
        + (PLAYER_INVENTORY_X - 1 - GUI_BORDER - CONTROL_BANK_WIDTH) / 2 + 1;
    private static final int RIGHT_CONTROL_X = GUI_WIDTH - LEFT_CONTROL_X - 16;
    private static final int[] VOIDING_PROTECTION = {
        PROTECT_ITEMS | PROTECT_FLUIDS, PROTECT_FLUIDS, PROTECT_ITEMS, 0
    };

    private ProcessingGuiControls() {
    }

    public static boolean isControlVisible(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine, int index) {
        int supported = machine.getSupportedProcessingModes();
        switch (index) {
            case VOIDING_SLOT:
                return (supported & (PROTECT_ITEMS | PROTECT_FLUIDS)) != 0;
            case SEPARATION_SLOT:
                return (supported & SEPARATE_INPUTS) != 0;
            case BATCH_SLOT:
                return (supported & BATCH) != 0;
            case LOCK_SLOT:
                return (supported & LOCK_RECIPE) != 0;
            case NEI_SLOT:
                return machine.getRecipeMapForNEI() != null;
            case RECIPE_MAP_SLOT:
                return machine.getRecipeMaps().size() > 1;
            default:
                return index == POWER_SLOT;
        }
    }

    public static int controlX(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine, int index) {
        switch (index) {
            case VOIDING_SLOT:
                return RIGHT_CONTROL_X - (isControlVisible(machine, SEPARATION_SLOT) ? 18 : 9);
            case SEPARATION_SLOT:
                return RIGHT_CONTROL_X;
            case BATCH_SLOT:
            case NEI_SLOT:
                return LEFT_CONTROL_X + 18;
            case POWER_SLOT:
                return LEFT_CONTROL_X
                    + (isControlVisible(machine, NEI_SLOT) || machine.getRecipeMaps().size() > 1 ? 0 : 9);
            default:
                return LEFT_CONTROL_X;
        }
    }

    public static int controlY(int index) {
        if (index == RECIPE_MAP_SLOT) {
            return PLAYER_INVENTORY_Y + 36;
        }
        return PLAYER_INVENTORY_Y + (index == LOCK_SLOT || index == BATCH_SLOT ? 18 : 0);
    }

    public static int voidingIndex(int modes) {
        int protection = modes & (PROTECT_ITEMS | PROTECT_FLUIDS);
        for (int index = 0; index < VOIDING_PROTECTION.length; index++) {
            if (VOIDING_PROTECTION[index] == protection) {
                return index;
            }
        }
        throw new IllegalArgumentException("Invalid output protection: " + protection);
    }

    public static boolean click(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine, int index, int mouseButton) {
        if (machine == null || (mouseButton != 0 && mouseButton != 1)) {
            return false;
        }
        IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        if (base == null || base.isClientSide() || base.isDead() || base.getMetaTileEntity() != machine
            || !isControlVisible(machine, index)) {
            return false;
        }
        int modes = machine.getProcessingModes();
        switch (index) {
            case VOIDING_SLOT:
                if ((machine.getSupportedProcessingModes() & (PROTECT_ITEMS | PROTECT_FLUIDS)) == PROTECT_FLUIDS) {
                    machine.setProcessingModes(modes ^ PROTECT_FLUIDS);
                    break;
                }
                int current = voidingIndex(modes);
                int next = Math.floorMod(current + (mouseButton == 1 ? -1 : 1), VOIDING_PROTECTION.length);
                machine.setProcessingModes((modes & ~(PROTECT_ITEMS | PROTECT_FLUIDS)) | VOIDING_PROTECTION[next]);
                break;
            case SEPARATION_SLOT:
                machine.setProcessingModes(modes ^ SEPARATE_INPUTS);
                break;
            case BATCH_SLOT:
                machine.setProcessingModes(modes ^ BATCH);
                break;
            case LOCK_SLOT:
                machine.setProcessingModes(modes ^ LOCK_RECIPE);
                break;
            case POWER_SLOT:
                if (base.isAllowedToWork()) {
                    base.disableWorking();
                } else {
                    base.enableWorking();
                }
                machine.onProcessingPowerChanged(base.isAllowedToWork());
                machine.markDirty();
                break;
            case RECIPE_MAP_SLOT:
                machine.cycleRecipeMap(mouseButton == 1);
                break;
            default:
                return false;
        }
        return true;
    }
}
