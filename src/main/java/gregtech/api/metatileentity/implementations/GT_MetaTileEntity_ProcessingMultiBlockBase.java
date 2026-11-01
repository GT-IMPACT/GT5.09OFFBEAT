package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.gui.GT_Container_ProcessingMachine;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.logic.ProcessingPersistence;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import net.minecraft.entity.player.InventoryPlayer;
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
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<T> {
    private ProcessingLogic processingLogic;
    private int processingModes;
    private int cycleModes;
    private int currentParallel;
    private int currentOperations;
    private boolean waitingForOutput;
    private String selectedRecipeMap;
    public long lEUt;
    private CheckRecipeResult processingResult = CheckRecipeResult.NONE;

    protected GT_MetaTileEntity_ProcessingMultiBlockBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GT_MetaTileEntity_ProcessingMultiBlockBase(String name) {
        super(name);
    }

    protected abstract ProcessingLogic createProcessingLogic();

    /** Opt in only when the machine uses its controller inventory slot. */
    public boolean hasControllerSlotInGui() {
        return false;
    }

    /**
     * Stable machine configuration. The first map is selected for a new controller.
     */
    public List<GT_Recipe_Map> getRecipeMaps() {
        return Collections.emptyList();
    }

    @Override
    public GT_Recipe_Map getRecipeMap() {
        List<GT_Recipe_Map> maps = getRecipeMaps();
        if (selectedRecipeMap == null) {
            return maps.isEmpty() ? null : maps.get(0);
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
     * outputs.
     */
    public boolean setActiveRecipeMap(GT_Recipe_Map map) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isClientSide()) {
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
        for (GTMTE_Multi_Hatch_Input hatch : mQuadrInputHatches) {
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

    public int getAvailableParallel() {
        return getProcessingLogic().getAvailableParallel();
    }

    public int getCurrentParallel() {
        return currentParallel;
    }

    public int getCurrentOperations() {
        return currentOperations;
    }

    public boolean isWaitingForOutput() {
        return waitingForOutput;
    }

    public void setProcessingModes(int modes) {
        processingModes = modes & ProcessingLogic.ALL_MODES;
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
        if (getRecipeMap() == null) {
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
                    getProcessingAmperage(), processingModes, new ProcessingOutputs(this),
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

    private void startCycle(ProcessingLogic.Result result, int efficiency) {
        lEUt = -result.eut;
        mEUt = (int) Math.max(Integer.MIN_VALUE + 1L, lEUt);
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
        BooleanSupplier sourceUnchanged = () -> sameReferences(allItems, getStoredInputs())
                && sameReferences(fluids, getStoredFluids());
        if ((processingModes & ProcessingLogic.SEPARATE_INPUTS) == 0) {
            return Collections.singletonList(new ProcessingInputs(allItems, fluids, sourceUnchanged));
        }
        List<ProcessingInputs> groups = new ArrayList<>();
        for (GTMTE_BusHatch_Input bus : mInputBusHatches) {
            if (isValidMetaTileEntity(bus)) {
                groups.add(new ProcessingInputs(GTMTE_BusHatch_Input.getStoredInputs(bus), fluids, sourceUnchanged));
            }
        }
        for (GT_MetaTileEntity_Hatch_InputBus bus : mInputBusses) {
            if (isValidMetaTileEntity(bus)) {
                List<ItemStack> items = new ArrayList<>();
                for (int i = bus.mInventory.length - 1; i >= 0; i--) {
                    if (bus.mInventory[i] != null) {
                        items.add(bus.mInventory[i]);
                    }
                }
                groups.add(new ProcessingInputs(items, fluids, sourceUnchanged));
            }
        }
        if (groups.isEmpty()) {
            groups.add(new ProcessingInputs(Collections.emptyList(), fluids, sourceUnchanged));
        }
        return groups;
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

    protected long getProcessingAmperage() {
        long amps = 0;
        int ordinary = 0;
        int multi = 0;
        for (GT_MetaTileEntity_Hatch_Energy hatch : mEnergyHatches) {
            if (isValidMetaTileEntity(hatch)) {
                ordinary++;
                amps = Math.addExact(amps,
                        hatch.maxEUInput() <= 0 ? 0 : hatch.getRatedInputThroughput() / hatch.maxEUInput());
            }
        }
        for (GT_MetaTileEntity_Hatch_EnergyMulti hatch : mEnergyHatchesMulti) {
            if (isValidMetaTileEntity(hatch)) {
                multi++;
                amps = Math.addExact(amps, hatch.Amp);
            }
        }
        return ordinary == 1 && multi == 0 ? 1 : amps;
    }

    @Override
    public boolean drainEnergyInput(long required) {
        if (required <= 0) {
            return true;
        }
        List<GT_MetaTileEntity_Hatch> hatches = new ArrayList<>();
        hatches.addAll(mEnergyHatches);
        hatches.addAll(mEnergyHatchesMulti);
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
            long cost = ProcessingLogic.efficiencyAdjustedEUt(-lEUt, mEfficiency);
            if (drainEnergyInput(cost)) {
                return true;
            }
        } catch (ArithmeticException ignored) {
        }
        stopMachine();
        processingResult = CheckRecipeResult.INSUFFICIENT_POWER;
        return false;
    }

    @Override
    protected void completeRecipe() {
        if (!waitingForOutput) {
            awardOutputAchievements();
        }
        waitingForOutput = true;
        lEUt = 0;
        mEUt = 0;
        boolean done = new ProcessingOutputs(this).deliver(mOutputItems, mOutputFluids,
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
        if (getBaseMetaTileEntity().isAllowedToWork()) {
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
            return;
        }
        currentParallel = 0;
        currentOperations = 0;
        super.stopMachine();
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
            currentParallel = mMaxProgresstime > 0 ? 1 : 0;
            currentOperations = currentParallel;
            processingResult = mMaxProgresstime > 0 ? CheckRecipeResult.SUCCESS : CheckRecipeResult.NONE;
            return;
        }
        processingModes = processing.getInteger("modes") & ProcessingLogic.ALL_MODES;
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
    public Object getServerGUI(int id, InventoryPlayer player, IGregTechTileEntity base) {
        return new GT_Container_ProcessingMachine(player, base);
    }
}
