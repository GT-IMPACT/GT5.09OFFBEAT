package gregtech.api.gui;

import gregtech.api.enums.GT_Values;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingPersistence;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.net.GT_Packet_ProcessingOutputs;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

import static gregtech.api.logic.ProcessingLogic.BATCH;
import static gregtech.api.logic.ProcessingLogic.LOCK_RECIPE;
import static gregtech.api.logic.ProcessingLogic.PROTECT_FLUIDS;
import static gregtech.api.logic.ProcessingLogic.PROTECT_ITEMS;
import static gregtech.api.logic.ProcessingLogic.SEPARATE_INPUTS;

/**
 * Processing controls use holographic slots and server-side slotClick handling.
 */
public class GT_Container_ProcessingMachine extends GT_Container_MultiMachine {
    public static final int CONTROL_COUNT = 6;
    public static final int VOIDING_SLOT = 0;
    public static final int SEPARATION_SLOT = 1;
    public static final int BATCH_SLOT = 2;
    public static final int LOCK_SLOT = 3;
    public static final int POWER_SLOT = 4;
    public static final int NEI_SLOT = 5;
    public static final int RECIPE_MAP_SLOT = 6;
    // The black interior is 256 x 144 (16:9), with a two-pixel frame.
    public static final int SCREEN_WIDTH = 260;
    public static final int SCREEN_HEIGHT = 148;
    public static final int GUI_WIDTH = SCREEN_WIDTH + 8;
    public static final int PLAYER_INVENTORY_X = (GUI_WIDTH - 162) / 2 + 1;
    public static final int PLAYER_INVENTORY_Y = SCREEN_HEIGHT + 8;
    public static final int GUI_HEIGHT = PLAYER_INVENTORY_Y + 82;
    private static final int GUI_BORDER = 4;
    private static final int CONTROL_BANK_WIDTH = 36;
    private static final int LEFT_CONTROL_X = GUI_BORDER
            + (PLAYER_INVENTORY_X - 1 - GUI_BORDER - CONTROL_BANK_WIDTH) / 2 + 1;
    private static final int RIGHT_CONTROL_X = GUI_WIDTH - LEFT_CONTROL_X - 16;
    public static final int CONTROLLER_FRAME_SIZE = 26;
    public static final int CONTROLLER_X = (PLAYER_INVENTORY_X - 1 + 162 + GUI_WIDTH - GUI_BORDER - 16) / 2;
    public static final int CONTROLLER_Y = PLAYER_INVENTORY_Y + 58 + 17 - (CONTROLLER_FRAME_SIZE + 16) / 2;
    private static final int[] VOIDING_PROTECTION = { PROTECT_ITEMS | PROTECT_FLUIDS, PROTECT_FLUIDS, PROTECT_ITEMS,
            0 };

    public int modes;
    public int availableParallel = 1;
    public int currentParallel;
    public int operations;
    public int activeRecipeMapIndex;
    public boolean allowedToWork;
    public long eut;
    public CheckRecipeResult result = CheckRecipeResult.NONE;
    public ItemStack[] outputItems = new ItemStack[0];
    public FluidStack[] outputFluids = new FluidStack[0];
    private int[] previous;
    private NBTTagCompound previousOutputs;
    private final int[] received = new int[9];
    private boolean receivedState;
    private boolean receivedOutputs;

    public GT_Container_ProcessingMachine(InventoryPlayer player, IGregTechTileEntity tile) {
        super(player, tile);
    }

    @Override
    public void addSlots(InventoryPlayer player) {
        int controlCount = CONTROL_COUNT + (hasRecipeMapSelector() ? 1 : 0);
        IInventory controls = new InventoryBasic("Processing controls", false, controlCount);
        for (int index = 0; index < controlCount; index++) {
            int x = isControlVisible(index) ? controlX(index) : -10000;
            addSlotToContainer(new GT_Slot_Holo(controls, index, x, controlY(index), false, false, 1));
        }
        if (machine().hasControllerSlotInGui()) {
            addSlotToContainer(new Slot(mTileEntity, 1, CONTROLLER_X, CONTROLLER_Y));
        }
    }

    public boolean isControlVisible(int slot) {
        int supported = machine().getSupportedProcessingModes();
        switch (slot) {
        case VOIDING_SLOT:
            return (supported & (PROTECT_ITEMS | PROTECT_FLUIDS)) != 0;
        case SEPARATION_SLOT:
            return (supported & SEPARATE_INPUTS) != 0;
        case BATCH_SLOT:
            return (supported & BATCH) != 0;
        case LOCK_SLOT:
            return (supported & LOCK_RECIPE) != 0;
        case NEI_SLOT:
            return getNEIRecipeMap() != null;
        case RECIPE_MAP_SLOT:
            return hasRecipeMapSelector();
        default:
            return slot == POWER_SLOT;
        }
    }

    public void updateControlSlotPositions() {
        int controlCount = CONTROL_COUNT + (hasRecipeMapSelector() ? 1 : 0);
        for (int index = 0; index < controlCount; index++) {
            Slot slot = getSlot(index);
            slot.xDisplayPosition = isControlVisible(index) ? controlX(index) : -10000;
            slot.yDisplayPosition = controlY(index);
        }
    }

    private int controlX(int slot) {
        switch (slot) {
        case VOIDING_SLOT:
            return RIGHT_CONTROL_X - (isControlVisible(SEPARATION_SLOT) ? 18 : 9);
        case SEPARATION_SLOT:
            return RIGHT_CONTROL_X;
        case BATCH_SLOT:
        case NEI_SLOT:
            return LEFT_CONTROL_X + 18;
        case POWER_SLOT:
            return LEFT_CONTROL_X + (isControlVisible(NEI_SLOT) || hasRecipeMapSelector() ? 0 : 9);
        default:
            return LEFT_CONTROL_X;
        }
    }

    private static int controlY(int slot) {
        if (slot == RECIPE_MAP_SLOT) {
            return PLAYER_INVENTORY_Y + 36;
        }
        return PLAYER_INVENTORY_Y + (slot == LOCK_SLOT || slot == BATCH_SLOT ? 18 : 0);
    }

    @Override
    public int getSlotCount() {
        return machine().hasControllerSlotInGui() ? 1 : 0;
    }

    @Override
    public int getShiftClickSlotCount() {
        return getSlotCount();
    }

    @Override
    public int getSlotStartIndex() {
        return CONTROL_COUNT + (hasRecipeMapSelector() ? 1 : 0);
    }

    @Override
    protected void bindPlayerInventory(InventoryPlayer player) {
        bindPlayerInventory(player, PLAYER_INVENTORY_X - 8, PLAYER_INVENTORY_Y - 84);
    }

    @Override
    public ItemStack slotClick(int slot, int mouseButton, int clickMode, EntityPlayer player) {
        int controlCount = CONTROL_COUNT + (hasRecipeMapSelector() ? 1 : 0);
        if (slot < 0 || slot >= controlCount) {
            return super.slotClick(slot, mouseButton, clickMode, player);
        }
        if (mTileEntity.isClientSide() || player.openContainer != this || !canInteractWith(player)
                || (mouseButton != 0 && mouseButton != 1) || (clickMode != 0 && clickMode != 1)) {
            return null;
        }
        if (!isControlVisible(slot)) {
            return null;
        }
        GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine = machine();
        int modes = machine.getProcessingModes();
        switch (slot) {
        case VOIDING_SLOT:
            if ((machine.getSupportedProcessingModes() & (PROTECT_ITEMS | PROTECT_FLUIDS)) == PROTECT_FLUIDS) {
                machine.setProcessingModes(modes ^ PROTECT_FLUIDS);
                break;
            }
            int index = voidingIndex(modes);
            int next = Math.floorMod(index + (mouseButton == 1 ? -1 : 1), VOIDING_PROTECTION.length);
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
            if (mTileEntity.isAllowedToWork()) {
                mTileEntity.disableWorking();
            } else {
                mTileEntity.enableWorking();
            }
            machine.markDirty();
            break;
        case RECIPE_MAP_SLOT:
            machine.cycleRecipeMap(mouseButton == 1);
            break;
        }
        detectAndSendChanges();
        return null;
    }

    public int getVoidingModeIndex() {
        return voidingIndex(modes);
    }

    private static int voidingIndex(int modes) {
        int protection = modes & (PROTECT_ITEMS | PROTECT_FLUIDS);
        for (int index = 0; index < VOIDING_PROTECTION.length; index++) {
            if (VOIDING_PROTECTION[index] == protection) {
                return index;
            }
        }
        throw new IllegalArgumentException("Invalid output protection: " + protection);
    }

    private GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine() {
        return (GT_MetaTileEntity_ProcessingMultiBlockBase<?>) mTileEntity.getMetaTileEntity();
    }

    public GT_Recipe_Map getNEIRecipeMap() {
        return machine().getRecipeMapForNEI();
    }

    public String getNEIRecipeMapName() {
        GT_Recipe_Map map = getNEIRecipeMap();
        return map == null ? "" : StatCollector.translateToLocal(map.mUnlocalizedName);
    }

    public boolean hasRecipeMapSelector() {
        return machine().getRecipeMaps().size() > 1;
    }

    public String getActiveRecipeMapName() {
        List<GT_Recipe_Map> maps = machine().getRecipeMaps();
        String key = activeRecipeMapIndex >= 0 && activeRecipeMapIndex < maps.size()
                ? maps.get(activeRecipeMapIndex).mUnlocalizedName
                : CheckRecipeResult.RECIPE_MAP_UNAVAILABLE.translationKey();
        return StatCollector.translateToLocal(key);
    }

    private int[] state() {
        GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine = machine();
        return new int[] { machine.getProcessingModes(), machine.getProcessingResult().ordinal(),
                machine.getAvailableParallel(), machine.getCurrentParallel(), machine.getCurrentOperations(),
                (int) machine.lEUt, (int) (machine.lEUt >>> 32), machine.getActiveRecipeMapIndex(),
                mTileEntity.isAllowedToWork() ? 1 : 0 };
    }

    private void send(ICrafting player, int[] values) {
        for (int i = 0; i < values.length; i++) {
            player.sendProgressBarUpdate(this, 100 + i * 2, values[i] & 0xffff);
            player.sendProgressBarUpdate(this, 101 + i * 2, values[i] >>> 16);
        }
    }

    @Override
    public void addCraftingToCrafters(ICrafting player) {
        super.addCraftingToCrafters(player);
        if (!mTileEntity.isClientSide()) {
            // The legacy container normally sends only changed values. Opening a
            // window must also send unchanged activity, errors and progress.
            player.sendProgressBarUpdate(this, 6, mTileEntity.getErrorDisplayID());
            int progress = mTileEntity.getProgress();
            player.sendProgressBarUpdate(this, 11, progress & 0xffff);
            player.sendProgressBarUpdate(this, 12, progress >>> 16);
            int duration = mTileEntity.getMaxProgress();
            player.sendProgressBarUpdate(this, 13, duration & 0xffff);
            player.sendProgressBarUpdate(this, 14, duration >>> 16);
            player.sendProgressBarUpdate(this, 16, mTileEntity.isActive() ? 1 : 0);
            send(player, state());
            sendOutputs(player, outputSnapshot());
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(mTileEntity.getMetaTileEntity() instanceof GT_MetaTileEntity_ProcessingMultiBlockBase)) {
            return;
        }
        updateControlSlotPositions();
        if (mTileEntity.isClientSide()) {
            return;
        }
        int[] values = state();
        if (!Arrays.equals(previous, values)) {
            for (Object player : crafters) {
                send((ICrafting) player, values);
            }
            previous = values;
        }
        NBTTagCompound outputs = outputSnapshot();
        if (!outputs.equals(previousOutputs)) {
            for (Object player : crafters) {
                sendOutputs((ICrafting) player, outputs);
            }
            previousOutputs = outputs;
        }
    }

    private NBTTagCompound outputSnapshot() {
        NBTTagCompound outputs = new NBTTagCompound();
        ProcessingPersistence.writeItems(outputs, "items", machine().mOutputItems);
        ProcessingPersistence.writeFluids(outputs, "fluids", machine().mOutputFluids);
        return outputs;
    }

    private void sendOutputs(ICrafting listener, NBTTagCompound outputs) {
        if (listener instanceof EntityPlayerMP) {
            GT_Values.NW.sendToPlayer(new GT_Packet_ProcessingOutputs(this, outputs), (EntityPlayerMP) listener);
        }
    }

    public void receiveOutputs(NBTTagCompound outputs) {
        if (mTileEntity.isClientSide()) {
            outputItems = ProcessingPersistence.readItems(outputs, "items");
            outputFluids = ProcessingPersistence.readFluids(outputs, "fluids");
            receivedOutputs = true;
        }
    }

    public boolean isDisplayReady() {
        return !mTileEntity.isClientSide() || (receivedState && receivedOutputs);
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id < 100 || id >= 118) {
            super.updateProgressBar(id, value);
            return;
        }
        int index = (id - 100) / 2;
        received[index] = (id & 1) == 0 ? (received[index] & 0xffff0000) | (value & 0xffff)
                : (received[index] & 0xffff) | (value << 16);
        if (id != 117) {
            return;
        }
        // Publish modes, result and power together, after the last word arrives.
        modes = received[0];
        result = CheckRecipeResult.fromOrdinal(received[1]);
        availableParallel = received[2];
        currentParallel = received[3];
        operations = received[4];
        eut = (received[5] & 0xffffffffL) | ((long) received[6] << 32);
        activeRecipeMapIndex = received[7];
        allowedToWork = received[8] != 0;
        if (hasRecipeMapSelector()) {
            machine().syncActiveRecipeMap(activeRecipeMapIndex);
        }
        updateControlSlotPositions();
        receivedState = true;
    }
}
