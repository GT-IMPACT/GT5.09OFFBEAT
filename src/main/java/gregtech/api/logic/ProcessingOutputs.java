package gregtech.api.logic;

import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus_ME;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output_ME;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_ModHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import static gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase.isValidMetaTileEntity;

/**
 * Shared simulation and delivery rules. Delivery subtracts only what the
 * destination accepted.
 */
public final class ProcessingOutputs {
    @FunctionalInterface
    public interface FluidOutputFilter {
        boolean accepts(GT_MetaTileEntity_Hatch_Output hatch, FluidStack fluid, int outputIndex);
    }

    private final GT_MetaTileEntity_MultiBlockBase machine;
    private FluidOutputFilter fluidFilter = (hatch, fluid, index) -> true;
    private UnaryOperator<FluidStack[]> fluidTransform = fluids -> fluids;
    private final Map<GT_MetaTileEntity_Hatch, ItemStack[]> slots = new IdentityHashMap<>();
    private final Map<GT_MetaTileEntity_Hatch_Output, FluidStack> tanks = new IdentityHashMap<>();
    private final Map<GT_MetaTileEntity_Hatch, Long> meReserved = new IdentityHashMap<>();

    public ProcessingOutputs(GT_MetaTileEntity_MultiBlockBase machine) {
        this.machine = machine;
    }

    public ProcessingOutputs setFluidOutputFilter(FluidOutputFilter filter) {
        fluidFilter = filter;
        return this;
    }

    public ProcessingOutputs setFluidOutputTransform(UnaryOperator<FluidStack[]> transform) {
        fluidTransform = transform;
        return this;
    }

    /** Applied once to prepared results, never to partially delivered remainders. */
    public FluidStack[] prepareFluids(FluidStack[] fluids) {
        return fluids == null ? null : fluidTransform.apply(ProcessingInputs.copyFluids(fluids));
    }

    public CheckRecipeResult canFit(ItemStack[] items, FluidStack[] fluids, boolean protectItems,
            boolean protectFluids) {
        slots.clear();
        tanks.clear();
        meReserved.clear();
        if (protectItems) {
            for (ItemStack stack : ProcessingInputs.copyItems(items)) {
                if (stack != null) {
                    insertItem(stack, true);
                    if (stack.stackSize > 0) {
                        return CheckRecipeResult.ITEM_OUTPUT_FULL;
                    }
                }
            }
        }
        if (protectFluids) {
            FluidStack[] copies = ProcessingInputs.copyFluids(fluids);
            for (int index = 0; index < copies.length; index++) {
                FluidStack stack = copies[index];
                if (stack != null) {
                    insertFluid(stack, index, true);
                    if (stack.amount > 0) {
                        return CheckRecipeResult.FLUID_OUTPUT_FULL;
                    }
                }
            }
        }
        return CheckRecipeResult.SUCCESS;
    }

    /** Local output tanks must be drained before starting another continuous production cycle. */
    public boolean areFluidOutputsEmpty() {
        for (GT_MetaTileEntity_Hatch_Output hatch : machine.mOutputHatches) {
            if (isValidMetaTileEntity(hatch) && !(hatch instanceof GT_MetaTileEntity_Hatch_Output_ME)
                    && hatch.getFluidAmount() > 0) {
                return false;
            }
        }
        // ME has no local tank to empty; canFit checks the network's available capacity.
        return true;
    }

    public boolean deliver(ItemStack[] items, FluidStack[] fluids, boolean protectItems, boolean protectFluids) {
        boolean complete = true;
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack != null && stack.stackSize > 0) {
                    insertItem(stack, false);
                    if (!protectItems) {
                        stack.stackSize = 0;
                    }
                    complete &= stack.stackSize == 0;
                }
            }
        }
        if (fluids != null) {
            for (int index = 0; index < fluids.length; index++) {
                FluidStack stack = fluids[index];
                if (stack != null && stack.amount > 0) {
                    insertFluid(stack, index, false);
                    if (!protectFluids) {
                        stack.amount = 0;
                    }
                    complete &= stack.amount == 0;
                }
            }
        }
        return complete;
    }

    private void insertItem(ItemStack stack, boolean simulate) {
        for (GT_MetaTileEntity_Hatch_OutputBus bus : machine.mOutputBusses) {
            if (stack.stackSize <= 0) {
                return;
            }
            if (!isValidMetaTileEntity(bus)) {
                continue;
            }
            if (bus instanceof GT_MetaTileEntity_Hatch_OutputBus_ME) {
                GT_MetaTileEntity_Hatch_OutputBus_ME me = (GT_MetaTileEntity_Hatch_OutputBus_ME) bus;
                int amount = (int) Math.min(stack.stackSize, Math.max(0,
                        me.getProcessingCapacity(stack) - (simulate ? meReserved.getOrDefault(me, 0L) : 0)));
                if (amount == 0) {
                    continue;
                }
                if (simulate) {
                    meReserved.put(me, meReserved.getOrDefault(me, 0L) + amount);
                } else {
                    ItemStack part = stack.copy();
                    part.stackSize = amount;
                    if (!me.storeAll(part)) {
                        continue;
                    }
                }
                stack.stackSize -= amount;
            } else {
                insertSlots(bus, stack, simulate, 0, bus.mInventory.length);
            }
        }
        for (GT_MetaTileEntity_Hatch_Output hatch : machine.mOutputHatches) {
            if (stack.stackSize > 0 && isValidMetaTileEntity(hatch) && hatch.outputsItems()
                    && !(hatch instanceof GT_MetaTileEntity_Hatch_Output_ME)) {
                insertSlots(hatch, stack, simulate, 1, 2);
            }
        }
    }

    private void insertSlots(GT_MetaTileEntity_Hatch hatch, ItemStack stack, boolean simulate, int from, int to) {
        ItemStack[] inventory = simulate ? slots.computeIfAbsent(hatch, h -> ProcessingInputs.copyItems(h.mInventory))
                : hatch.mInventory;
        for (int i = from; i < Math.min(to, inventory.length) && stack.stackSize > 0; i++) {
            ItemStack present = inventory[i];
            if (present != null && present.stackSize > 0
                    && (!present.isItemEqual(stack) || !ItemStack.areItemStackTagsEqual(present, stack))) {
                continue;
            }
            int stored = present == null ? 0 : Math.max(0, present.stackSize);
            int room = Math.max(0, Math.min(hatch.getInventoryStackLimit(), stack.getMaxStackSize()) - stored);
            int amount = Math.min(room, stack.stackSize);
            if (amount <= 0) {
                continue;
            }
            ItemStack replacement = stack.copy();
            replacement.stackSize = stored + amount;
            inventory[i] = replacement;
            stack.stackSize -= amount;
            if (!simulate) {
                hatch.getBaseMetaTileEntity().markDirty();
            }
        }
    }

    private void insertFluid(FluidStack stack, int outputIndex, boolean simulate) {
        // Restricted hatches retain their existing priority.
        for (boolean restricted : new boolean[] { true, false }) {
            for (GT_MetaTileEntity_Hatch_Output hatch : machine.mOutputHatches) {
                if (stack.amount <= 0) {
                    return;
                }
                if (!isValidMetaTileEntity(hatch) || (hatch.mMode != 0) != restricted
                        || !fluidFilter.accepts(hatch, stack, outputIndex)) {
                    continue;
                }
                if (GT_ModHandler.isSteam(stack) ? !hatch.outputsSteam() : !hatch.outputsLiquids()) {
                    continue;
                }
                if (hatch.isFluidLocked() && hatch.getLockedFluidName() != null
                        && !hatch.getLockedFluidName().equals(stack.getFluid().getUnlocalizedName())) {
                    continue;
                }
                int amount;
                if (hatch instanceof GT_MetaTileEntity_Hatch_Output_ME) {
                    GT_MetaTileEntity_Hatch_Output_ME me = (GT_MetaTileEntity_Hatch_Output_ME) hatch;
                    amount = (int) Math.min(stack.amount, Math.max(0,
                            me.getProcessingCapacity(stack) - (simulate ? meReserved.getOrDefault(me, 0L) : 0)));
                    if (simulate) {
                        meReserved.put(me, meReserved.getOrDefault(me, 0L) + amount);
                    }
                } else {
                    FluidStack present = simulate && tanks.containsKey(hatch) ? tanks.get(hatch)
                            : hatch.getFillableStack();
                    if (present != null && present.amount > 0 && !present.isFluidEqual(stack)) {
                        continue;
                    }
                    int stored = present == null ? 0 : present.amount;
                    amount = Math.min(stack.amount, Math.max(0, hatch.getCapacity() - stored));
                    amount = Math.min(amount, hatch.fill(stack, false));
                    if (simulate && amount > 0) {
                        FluidStack replacement = stack.copy();
                        replacement.amount = stored + amount;
                        tanks.put(hatch, replacement);
                    }
                }
                if (!simulate && amount > 0) {
                    FluidStack part = stack.copy();
                    part.amount = amount;
                    amount = hatch.fill(part, true);
                    hatch.onEmptyingContainerWhenEmpty();
                    hatch.getBaseMetaTileEntity().markDirty();
                }
                stack.amount -= amount;
            }
        }
    }
}
