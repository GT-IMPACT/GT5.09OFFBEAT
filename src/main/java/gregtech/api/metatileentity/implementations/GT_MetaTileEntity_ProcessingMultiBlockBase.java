package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.GT_Mod;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.logic.IProcessingOutputSink;
import gregtech.api.logic.ProcessingPersistence;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map_NonGTRecipes;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.gui.modularui.multiblock.MTEProcessingMultiBlockGui;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Opt-in processing base. Existing multiblocks and IMPACT subclasses keep their
 * legacy lifecycle.
 */
public abstract class GT_MetaTileEntity_ProcessingMultiBlockBase<T extends GT_MetaTileEntity_ProcessingMultiBlockBase<T>>
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<T> implements IConfigurationCircuitSupport {

    private static final int CIRCUIT_SLOT = 2;
    private ProcessingLogic processingLogic;
    private int processingModes;
    private int cycleModes;
    private int currentParallel;
    private int currentOperations;
    private boolean waitingForOutput;
    private boolean legacyOutputs;
    private String selectedRecipeMap;
    public long lEUt;
    private CheckRecipeResult processingResult = CheckRecipeResult.NONE;

    protected GT_MetaTileEntity_ProcessingMultiBlockBase(int id, String name, String regionalName) {
        super(id, name, regionalName, CIRCUIT_SLOT + 1);
    }

    protected GT_MetaTileEntity_ProcessingMultiBlockBase(String name) {
        super(name, CIRCUIT_SLOT + 1);
    }

    protected GT_MetaTileEntity_ProcessingMultiBlockBase(int id, String name, String regionalName, int slots) {
        super(id, name, regionalName, slots);
    }

    protected GT_MetaTileEntity_ProcessingMultiBlockBase(String name, int slots) {
        super(name, slots);
    }

    protected abstract ProcessingLogic createProcessingLogic();

    protected IProcessingOutputSink createProcessingOutputs() {
        return new ProcessingOutputs(this);
    }

    /** Opt in only when the machine uses its controller inventory slot. */
    public boolean hasControllerSlotInGui() {
        return !allowSelectCircuit() && usesControllerSlotAsRecipeInput();
    }

    @Override
    public boolean allowSelectCircuit() {
        return false;
    }

    @Override
    public int getCircuitSlot() {
        return CIRCUIT_SLOT;
    }

    @Override
    public int getCircuitSlotX() {
        return MTEProcessingMultiBlockGui.CIRCUIT_SLOT_X;
    }

    @Override
    public int getCircuitSlotY() {
        return MTEProcessingMultiBlockGui.CIRCUIT_SLOT_Y;
    }

    protected final ItemStack getConfigurationCircuit() {
        return allowSelectCircuit() ? mInventory[getCircuitSlot()] : null;
    }

    @Override
    public boolean isValidSlot(int index) {
        return index != getCircuitSlot() && super.isValidSlot(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == getCircuitSlot() && stack != null) {
            if (!(stack.getItem() instanceof GT_IntegratedCircuit_Item)
                || stack.getItemDamage() < 0 || stack.getItemDamage() > GhostCircuitItemStackHandler.MAX_CONFIG) {
                stack = null;
            } else {
                stack = stack.copy();
                stack.stackSize = 0;
            }
        }
        super.setInventorySlotContents(index, stack);
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        return index == getCircuitSlot() ? null : super.decrStackSize(index, amount);
    }

    /**
     * Controller ingredients are shared by all input groups, including separated
     * buses.
     */
    protected boolean usesControllerSlotAsRecipeInput() {
        return false;
    }

    /**
     * Stable machine configuration. The first map is selected for a new controller.
     */
    public List<GT_Recipe_Map> getRecipeMaps() {
        return Collections.emptyList();
    }

    protected GT_Recipe_Map getDefaultRecipeMap() {
        List<GT_Recipe_Map> maps = getRecipeMaps();
        return maps.isEmpty() ? null : maps.get(0);
    }

    @Override
    public GT_Recipe_Map getRecipeMap() {
        List<GT_Recipe_Map> maps = getRecipeMaps();
        if (selectedRecipeMap == null) {
            return getDefaultRecipeMap();
        }
        for (GT_Recipe_Map map : maps) {
            if (map.mUnlocalizedName.equals(selectedRecipeMap)) {
                return map;
            }
        }
        return null;
    }

    public int getActiveRecipeMapIndex() {
        return getRecipeMaps().indexOf(getRecipeMap());
    }

    /**
     * Selects a supported map for the next cycle without touching in-flight
     * outputs. Legacy NBT loading can call this before the tile has a world.
     */
    public boolean setActiveRecipeMap(GT_Recipe_Map map) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.getWorld() != null && base.isClientSide()) {
            return false;
        }
        if (map == null || !getRecipeMaps().contains(map) || map == getRecipeMap()) {
            return false;
        }
        selectedRecipeMap = map.mUnlocalizedName;
        getProcessingLogic().clearRecipeCache();
        getProcessingLogic().clearRecipeLock();
        cashedRecipe = null;
        updateInputRecipeMaps(map);
        if (mMaxProgresstime == 0 && !waitingForOutput) {
            processingResult = CheckRecipeResult.NONE;
        }
        markDirty();
        return true;
    }

    public boolean cycleRecipeMap(boolean backwards) {
        List<GT_Recipe_Map> maps = getRecipeMaps();
        if (maps.size() < 2) {
            return false;
        }
        int current = getActiveRecipeMapIndex();
        int next = current < 0 ? 0 : Math.floorMod(current + (backwards ? -1 : 1), maps.size());
        return setActiveRecipeMap(maps.get(next));
    }

    /** Applies server container state only to the client-side tile. */
    public void syncActiveRecipeMap(int index) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        List<GT_Recipe_Map> maps = getRecipeMaps();
        if (base == null || !base.isClientSide() || index < -1 || index >= maps.size()) {
            return;
        }
        selectedRecipeMap = index < 0 ? "" : maps.get(index).mUnlocalizedName;
    }

    private void updateInputRecipeMaps(GT_Recipe_Map map) {
        for (GT_MetaTileEntity_Hatch_InputBus bus : mInputBusses) {
            if (isValidMetaTileEntity(bus)) {
                bus.mRecipeMap = map;
            }
        }
        for (GT_MetaTileEntity_Hatch_Input hatch : mInputHatches) {
            if (isValidMetaTileEntity(hatch)) {
                hatch.mRecipeMap = map;
            }
        }
        for (GTMTE_BusHatch_Input hatch : mInputBusHatches) {
            if (isValidMetaTileEntity(hatch)) {
                hatch.mRecipeMap = map;
            }
        }
    }

    protected final ProcessingLogic getProcessingLogic() {
        if (processingLogic == null) {
            processingLogic = createProcessingLogic();
        }
        return processingLogic;
    }

    /**
     * IMPACT can supply its current network-dependent allowance here without a GT
     * -> IMPACT dependency.
     */
    public int getMaxParallelRecipes() {
        return 1;
    }

    /**
     * Independent start gate: a disconnected required receiver must not be bypassed
     * by extra parallels.
     */
    protected CheckRecipeResult validateProcessing() {
        return CheckRecipeResult.SUCCESS;
    }

    public CheckRecipeResult getProcessingResult() {
        return processingResult;
    }

    public int getProcessingModes() {
        return processingModes;
    }

    public int getControllerSlotIndex() {
        return 1;
    }

    public boolean isControllerSlotInput() {
        return true;
    }

    public void onProcessingPowerChanged(boolean enabled) {
    }

    public String[] getProcessingInfo() {
        return new String[0];
    }

    public int getSupportedProcessingModes() {
        return ProcessingLogic.ALL_MODES;
    }

    @Override
    public void initDefaultModes(NBTTagCompound tag) {
        if (tag == null || !tag.getCompoundTag("processing").hasKey("modes")) {
            processingModes = getSupportedProcessingModes()
                    & (ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS);
        }
    }

    /** Override for display-only maps with their own registered NEI handler. */
    public GT_Recipe_Map getRecipeMapForNEI() {
        GT_Recipe_Map map = getRecipeMap();
        return map != null && (map.mNEIAllowed || map instanceof GT_Recipe_Map_NonGTRecipes)
                ? map : null;
    }

    public boolean isNEIButtonEnabled() {
        return true;
    }

    public int getAvailableParallel() {
        return getProcessingLogic().getAvailableParallel();
    }

    /** Zero capacity hides the optional internal energy buffer readout. */
    public long getProcessingEnergyCapacity() {
        return 0;
    }

    public long getProcessingStoredEnergy() {
        return 0;
    }

    public int getCurrentParallel() {
        return currentParallel;
    }

    public int getCurrentOperations() {
        return currentOperations;
    }

    @Override
    public boolean isWaitingForOutput() {
        return waitingForOutput;
    }

    public void setProcessingModes(int modes) {
        processingModes = modes & getSupportedProcessingModes();
        if ((processingModes & ProcessingLogic.LOCK_RECIPE) == 0) {
            getProcessingLogic().clearRecipeLock();
        }
        markDirty();
    }

    @Override
    public void markDirty() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.getWorld() != null) {
            markStorageDirty();
        }
    }

    @Override
    public boolean checkRecipe(ItemStack stack) {
        if (mMaxProgresstime > 0 || waitingForOutput) {
            return false;
        }
        if (getProcessingLogic().requiresRecipeMap() && getRecipeMap() == null) {
            processingResult = CheckRecipeResult.RECIPE_MAP_UNAVAILABLE;
            return false;
        }
        processingResult = validateProcessing();
        if (!processingResult.wasSuccessful()) {
            return false;
        }
        int efficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        final ProcessingLogic.Request request;
        try {
            request = new ProcessingLogic.Request(collectProcessingInputs(), getAverageInputVoltage(),
                    getProcessingAmperage(), processingModes, createProcessingOutputs(),
                    bound -> getBaseMetaTileEntity().getRandomNumber(bound), efficiency);
        } catch (ArithmeticException exception) {
            processingResult = CheckRecipeResult.POWER_OVERFLOW;
            return false;
        }
        ProcessingLogic.Result result = getProcessingLogic().process(request);
        processingResult = result.status;
        if (!processingResult.wasSuccessful()) {
            return false;
        }
        startCycle(result, efficiency);
        return true;
    }

    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        legacyOutputs = false;
        lEUt = -result.eut;
        mEUt = (int) Math.max(Integer.MIN_VALUE + 1L, Math.min(Integer.MAX_VALUE, lEUt));
        mMaxProgresstime = result.duration;
        mProgresstime = 0;
        mOutputItems = result.items;
        mOutputFluids = result.fluids;
        currentParallel = result.parallel;
        currentOperations = result.operations;
        cycleModes = processingModes;
        mEfficiency = efficiency;
        mEfficiencyIncrease = 10000;
        cashedRecipe = result.recipe;
        updateSlots();
        markDirty();
    }

    protected List<ProcessingInputs> collectProcessingInputs() {
        List<ItemStack> allItems = getStoredInputs();
        List<FluidStack> fluids = getStoredFluids();
        int controllerSlot = allowSelectCircuit() ? getCircuitSlot()
                : usesControllerSlotAsRecipeInput() ? getControllerSlotIndex() : -1;
        ItemStack controller = getStackInSlot(controllerSlot);
        BooleanSupplier sourceUnchanged = () -> sameReferences(allItems, getStoredInputs())
                && sameReferences(fluids, getStoredFluids())
                && (controllerSlot < 0 || getStackInSlot(controllerSlot) == controller);
        if ((processingModes & ProcessingLogic.SEPARATE_INPUTS) == 0) {
            return Collections.singletonList(
                    new ProcessingInputs(withControllerInput(allItems, controller), fluids, sourceUnchanged));
        }
        List<ProcessingInputs> groups = new ArrayList<>();
        for (GT_MetaTileEntity_Hatch bus : getProcessingInputBuses()) {
            if (!isValidMetaTileEntity(bus)) {
                continue;
            }
            List<ItemStack> items = new ArrayList<>();
            if (bus instanceof GTMTE_BusHatch_Input) {
                items.addAll(GTMTE_BusHatch_Input.getStoredInputs((GTMTE_BusHatch_Input) bus));
            } else {
                for (int i = bus.mInventory.length - 1; i >= 0; i--) {
                    if (bus.mInventory[i] != null) {
                        items.add(bus.mInventory[i]);
                    }
                }
            }
            groups.add(new ProcessingInputs(withControllerInput(items, controller), fluids, sourceUnchanged));
        }
        if (groups.isEmpty()) {
            groups.add(new ProcessingInputs(withControllerInput(Collections.emptyList(), controller), fluids,
                    sourceUnchanged));
        }
        return groups;
    }

    protected List<GT_MetaTileEntity_Hatch> getProcessingInputBuses() {
        List<GT_MetaTileEntity_Hatch> hatches = new ArrayList<>();
        hatches.addAll(mInputBusHatches);
        hatches.addAll(mInputBusses);
        return hatches;
    }

    private static List<ItemStack> withControllerInput(List<ItemStack> inputs, ItemStack controller) {
        if (controller == null) {
            return inputs;
        }
        List<ItemStack> combined = new ArrayList<>(inputs);
        combined.add(controller);
        return combined;
    }

    private static <E> boolean sameReferences(List<E> expected, List<E> current) {
        if (expected.size() != current.size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            if (expected.get(index) != current.get(index)) {
                return false;
            }
        }
        return true;
    }

    protected List<GT_MetaTileEntity_Hatch> getProcessingEnergyHatches() {
        List<GT_MetaTileEntity_Hatch> hatches = new ArrayList<>();
        hatches.addAll(mEnergyHatches);
        hatches.addAll(mEnergyHatchesMulti);
        return hatches;
    }

    protected List<GT_MetaTileEntity_Hatch> getProcessingDynamos() {
        List<GT_MetaTileEntity_Hatch> hatches = new ArrayList<>();
        hatches.addAll(mDynamoHatches);
        hatches.addAll(mDynamoHatchesMulti);
        return hatches;
    }

    /** Sum of nominal voltages; additional amperage does not raise the tier. */
    protected long getTotalInputVoltage() {
        long voltage = 0;
        for (GT_MetaTileEntity_Hatch hatch : getProcessingEnergyHatches()) {
            if (isValidMetaTileEntity(hatch)) {
                voltage = Math.addExact(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
            }
        }
        return voltage;
    }

    @Override
    public long getAverageInputVoltage() {
        long voltage = 0;
        int count = 0;
        for (GT_MetaTileEntity_Hatch hatch : getProcessingEnergyHatches()) {
            if (isValidMetaTileEntity(hatch)) {
                voltage = Math.addExact(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
                count++;
            }
        }
        return count == 0 ? 0 : voltage / count;
    }

    protected long getProcessingAmperage() {
        long amps = 0;
        int ordinary = 0;
        int multi = 0;
        for (GT_MetaTileEntity_Hatch hatch : getProcessingEnergyHatches()) {
            if (!isValidMetaTileEntity(hatch)) {
                continue;
            }
            if (hatch instanceof GT_MetaTileEntity_Hatch_EnergyMulti) {
                multi++;
                amps = Math.addExact(amps, ((GT_MetaTileEntity_Hatch_EnergyMulti) hatch).Amp);
            } else if (hatch instanceof GT_MetaTileEntity_Hatch_Energy) {
                ordinary++;
                GT_MetaTileEntity_Hatch_Energy energy = (GT_MetaTileEntity_Hatch_Energy) hatch;
                amps = Math.addExact(amps,
                        energy.maxEUInput() <= 0 ? 0 : energy.getRatedInputThroughput() / energy.maxEUInput());
            }
        }
        return ordinary == 1 && multi == 0 ? 1 : amps;
    }

    @Override
    public boolean drainEnergyInput(long required) {
        if (required <= 0) {
            return true;
        }
        List<GT_MetaTileEntity_Hatch> hatches = getProcessingEnergyHatches();
        long missing = required;
        for (GT_MetaTileEntity_Hatch hatch : hatches) {
            if (isValidMetaTileEntity(hatch)) {
                missing -= Math.min(missing, Math.max(0, hatch.getBaseMetaTileEntity().getStoredEU()));
            }
        }
        if (missing > 0) {
            return false;
        }
        for (GT_MetaTileEntity_Hatch hatch : hatches) {
            if (isValidMetaTileEntity(hatch)) {
                long amount = Math.min(required, Math.max(0, hatch.getBaseMetaTileEntity().getStoredEU()));
                if (amount > 0) {
                    if (!hatch.getBaseMetaTileEntity().decreaseStoredEnergyUnits(amount, false)) {
                        return false;
                    }
                    required -= amount;
                }
                if (required == 0) {
                    return true;
                }
            }
        }
        return required == 0;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (waitingForOutput) {
            return true;
        }
        try {
            if (lEUt > 0) {
                long generated = Math.addExact(Math.multiplyExact(lEUt / 10000, mEfficiency),
                        lEUt % 10000 * mEfficiency / 10000);
                addEnergyOutput(generated);
                return true;
            }
            long cost = ProcessingLogic.efficiencyAdjustedEUt(-lEUt, mEfficiency);
            if (drainEnergyInput(cost)) {
                return true;
            }
        } catch (ArithmeticException ignored) {
        }
        stopProcessing(CheckRecipeResult.INSUFFICIENT_POWER);
        return false;
    }

    @Override
    public boolean addEnergyOutputMultipleDynamos(long energy, boolean allowMixedVoltage) {
        if (energy <= 0) {
            return true;
        }
        List<GT_MetaTileEntity_Hatch> dynamos = getProcessingDynamos();
        long capacity = 0;
        long firstVoltage = -1;
        for (GT_MetaTileEntity_Hatch hatch : dynamos) {
            if (!isValidMetaTileEntity(hatch)) {
                continue;
            }
            long voltage = hatch.maxEUOutput();
            if (firstVoltage >= 0 && firstVoltage != voltage && !allowMixedVoltage) {
                explodeMultiblock();
                return false;
            }
            firstVoltage = voltage;
            long throughput = Math.multiplyExact(voltage, hatch.maxAmperesOut());
            capacity += Math.min(energy - capacity, throughput);
        }
        if (capacity < energy) {
            explodeMultiblock();
            return false;
        }
        long remaining = energy;
        for (GT_MetaTileEntity_Hatch hatch : dynamos) {
            if (remaining == 0) {
                break;
            }
            if (isValidMetaTileEntity(hatch)) {
                long throughput = Math.multiplyExact(hatch.maxEUOutput(), hatch.maxAmperesOut());
                long offered = Math.min(remaining, throughput);
                IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
                long accepted = Math.min(offered, Math.max(0, tile.getEUCapacity() - tile.getStoredEU()));
                tile.increaseStoredEnergyUnits(accepted, false);
                remaining -= offered;
            }
        }
        return true;
    }

    @Override
    protected void completeRecipe() {
        IProcessingOutputSink outputs = createProcessingOutputs();
        if (legacyOutputs) {
            try {
                mOutputFluids = outputs.prepareFluids(mOutputFluids);
            } catch (ProcessingLogic.OutputOverflowException exception) {
                waitingForOutput = true;
                lEUt = 0;
                mEUt = 0;
                mProgresstime = Math.max(0, mMaxProgresstime - 1);
                processingResult = CheckRecipeResult.OUTPUT_OVERFLOW;
                markDirty();
                return;
            }
            legacyOutputs = false;
        }
        if (!waitingForOutput) {
            awardOutputAchievements();
        }
        waitingForOutput = true;
        lEUt = 0;
        mEUt = 0;
        boolean done = outputs.deliver(mOutputItems, mOutputFluids,
                (cycleModes & ProcessingLogic.PROTECT_ITEMS) != 0, (cycleModes & ProcessingLogic.PROTECT_FLUIDS) != 0);
        markDirty();
        if (!done) {
            mProgresstime = Math.max(0, mMaxProgresstime - 1);
            processingResult = CheckRecipeResult.WAITING_FOR_OUTPUT;
            return;
        }
        waitingForOutput = false;
        mOutputItems = null;
        mOutputFluids = null;
        mProgresstime = 0;
        mMaxProgresstime = 0;
        mEfficiency = Math.max(0, Math.min(mEfficiency + mEfficiencyIncrease,
                getMaxEfficiency(mInventory[1]) - (getIdealStatus() - getRepairStatus()) * 1000));
        mEfficiencyIncrease = 0;
        currentParallel = 0;
        currentOperations = 0;
        processingResult = CheckRecipeResult.NONE;
        if (getBaseMetaTileEntity().isAllowedToWork() && getRepairStatus() > 0) {
            checkRecipe(mInventory[1]);
        }
    }

    private void awardOutputAchievements() {
        if (mOutputItems == null) {
            return;
        }
        for (ItemStack output : mOutputItems) {
            if (output != null) {
                try {
                    IGregTechTileEntity base = getBaseMetaTileEntity();
                    GT_Mod.achievements.issueAchivementHatch(base.getWorld().getPlayerEntityByName(base.getOwnerName()),
                            output);
                } catch (Exception ignored) {
                    // Achievement services must not prevent recipe completion, as in the legacy
                    // base.
                }
            }
        }
    }

    @Override
    public void stopMachine() {
        lEUt = 0;
        // Completed protected output survives structure loss and disabling the
        // controller.
        if (waitingForOutput) {
            mEUt = 0;
            if (getBaseMetaTileEntity().isAllowedToWork()) {
                getBaseMetaTileEntity().disableWorking();
                markDirty();
            }
            return;
        }
        currentParallel = 0;
        currentOperations = 0;
        mOutputFluids = null;
        legacyOutputs = false;
        cycleModes = 0;
        processingResult = CheckRecipeResult.NONE;
        super.stopMachine();
        markDirty();
    }

    protected void stopProcessing(CheckRecipeResult reason) {
        if (reason == CheckRecipeResult.INSUFFICIENT_POWER) {
            stopMachineOnCriticalFailure();
        } else {
            stopMachine();
        }
        processingResult = reason;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setLong("mEUt", lEUt);
        NBTTagCompound processing = new NBTTagCompound();
        processing.setInteger("version", 1);
        processing.setInteger("modes", processingModes);
        processing.setInteger("cycleModes", cycleModes);
        processing.setBoolean("waiting", waitingForOutput);
        processing.setBoolean("legacyOutputs", legacyOutputs);
        processing.setBoolean("ghostCircuit", allowSelectCircuit());
        processing.setInteger("parallel", currentParallel);
        processing.setInteger("operations", currentOperations);
        GT_Recipe_Map map = getRecipeMap();
        if (map != null || selectedRecipeMap != null) {
            processing.setString("recipeMap", map == null ? selectedRecipeMap : map.mUnlocalizedName);
        }
        processing.setTag("lock", getProcessingLogic().saveRecipeLock());
        ProcessingPersistence.writeItems(processing, "items", mOutputItems);
        ProcessingPersistence.writeFluids(processing, "fluids", mOutputFluids);
        tag.setTag("processing", processing);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        NBTTagCompound processing = tag.getCompoundTag("processing");
        if (allowSelectCircuit() && !processing.getBoolean("ghostCircuit")
            && mInventory[getCircuitSlot()] == null) {
            ItemStack legacyCircuit = mInventory[1];
            if (legacyCircuit != null && legacyCircuit.getItem() instanceof GT_IntegratedCircuit_Item
                && legacyCircuit.getItemDamage() >= 0
                && legacyCircuit.getItemDamage() <= GhostCircuitItemStackHandler.MAX_CONFIG) {
                // Keep the original item intact; only copy its configuration into the phantom slot.
                setInventorySlotContents(getCircuitSlot(), legacyCircuit);
            }
        }
        if (getCircuitSlot() >= 0) {
            setInventorySlotContents(getCircuitSlot(), mInventory[getCircuitSlot()]);
        }
        legacyOutputs = !tag.hasKey("processing") || processing.getBoolean("legacyOutputs");
        selectedRecipeMap = processing.hasKey("recipeMap") ? processing.getString("recipeMap") : null;
        getProcessingLogic().clearRecipeCache();
        getProcessingLogic().clearRecipeLock();
        cashedRecipe = null;
        updateInputRecipeMaps(getRecipeMap());
        lEUt = tag.getLong("mEUt");
        mEUt = (int) Math.max(Integer.MIN_VALUE + 1L, Math.min(Integer.MAX_VALUE, lEUt));
        if (!tag.hasKey("processing")) {
            processingModes = 0;
            cycleModes = 0;
            waitingForOutput = false;
            currentParallel = mMaxProgresstime > 0 ? Math.max(1, getLegacyCycleParallel(tag)) : 0;
            currentOperations = currentParallel;
            processingResult = mMaxProgresstime > 0 ? CheckRecipeResult.SUCCESS : CheckRecipeResult.NONE;
            return;
        }
        processingModes = processing.getInteger("modes") & getSupportedProcessingModes();
        cycleModes = processing.getInteger("cycleModes") & ProcessingLogic.ALL_MODES;
        waitingForOutput = processing.getBoolean("waiting");
        currentParallel = processing.getInteger("parallel");
        currentOperations = processing.getInteger("operations");
        getProcessingLogic().loadRecipeLock(processing.getCompoundTag("lock"));
        mOutputItems = ProcessingPersistence.readItems(processing, "items");
        mOutputFluids = ProcessingPersistence.readFluids(processing, "fluids");
        processingResult = waitingForOutput ? CheckRecipeResult.WAITING_FOR_OUTPUT
                : mMaxProgresstime > 0 ? CheckRecipeResult.SUCCESS : CheckRecipeResult.NONE;
    }

    protected int getLegacyCycleParallel(NBTTagCompound tag) {
        return 1;
    }

    @Override
    public long getRunningEUt() {
        return lEUt;
    }

    @Override
    public String[] getInfoData() {
        List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(StatCollector.translateToLocal("gt.processing.parallel") + ": " + currentParallel + " / "
                + getAvailableParallel());
        info.add(StatCollector.translateToLocal(processingResult.translationKey()));
        if (getRecipeMaps().size() > 1) {
            GT_Recipe_Map map = getRecipeMap();
            info.add(StatCollector.translateToLocal("gt.processing.recipe_map") + ": " + StatCollector.translateToLocal(
                    map == null ? CheckRecipeResult.RECIPE_MAP_UNAVAILABLE.translationKey() : map.mUnlocalizedName));
        }
        return info.toArray(new String[0]);
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEProcessingMultiBlockGui(this).build(data, syncManager, settings);
    }
}
