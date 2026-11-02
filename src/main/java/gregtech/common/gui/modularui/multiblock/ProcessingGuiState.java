package gregtech.common.gui.modularui.multiblock;

import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingPersistence;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** Publishes the legacy processing state and output snapshot together through MUI2. */
public final class ProcessingGuiState {

    private final GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine;
    private final boolean client;
    private final GenericSyncValue<NBTTagCompound, ?> syncValue;
    private boolean receivedSnapshot;

    int modes;
    CheckRecipeResult result = CheckRecipeResult.NONE;
    int availableParallel = 1;
    int currentParallel;
    int operations;
    int activeRecipeMapIndex;
    boolean allowedToWork;
    long eut;
    long storedEnergy;
    long energyCapacity;
    ItemStack[] outputItems = new ItemStack[0];
    FluidStack[] outputFluids = new FluidStack[0];
    int mDisplayErrorCode;
    int mActive;
    int mProgressTime;
    int mMaxProgressTime;

    public ProcessingGuiState(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine, PanelSyncManager syncManager) {
        this.machine = machine;
        client = syncManager.isClient();
        syncValue = GenericSyncValue.builder(NBTTagCompound.class)
            .getter(this::snapshot)
            .setter(this::receive)
            .adapter(ByteBufAdapters.NBT)
            .build();
        syncManager.syncValue("processingState", syncValue);
    }

    public GenericSyncValue<NBTTagCompound, ?> getSyncValue() {
        return syncValue;
    }

    public boolean isDisplayReady() {
        return !client || receivedSnapshot;
    }

    NBTTagCompound snapshot() {
        NBTTagCompound snapshot = new NBTTagCompound();
        if (client) {
            return snapshot;
        }
        IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        if (base == null || base.isDead() || base.getMetaTileEntity() != machine) {
            return snapshot;
        }
        snapshot.setIntArray("state", new int[]{
            machine.getProcessingModes(), machine.getProcessingResult().ordinal(),
            machine.getAvailableParallel(), machine.getCurrentParallel(), machine.getCurrentOperations(),
            (int) machine.lEUt, (int) (machine.lEUt >>> 32), machine.getActiveRecipeMapIndex(),
            base.isAllowedToWork() ? 1 : 0
        });
        long stored = machine.getProcessingStoredEnergy();
        long capacity = machine.getProcessingEnergyCapacity();
        if (capacity > 0) {
            snapshot.setLong("storedEnergy", stored);
            snapshot.setLong("energyCapacity", capacity);
        }
        ProcessingPersistence.writeItems(snapshot, "items", machine.mOutputItems);
        ProcessingPersistence.writeFluids(snapshot, "fluids", machine.mOutputFluids);
        snapshot.setInteger("error", base.getErrorDisplayID());
        snapshot.setInteger("active", base.isActive() ? 1 : 0);
        snapshot.setInteger("progress", base.getProgress());
        snapshot.setInteger("maxProgress", base.getMaxProgress());
        apply(snapshot);
        return snapshot;
    }

    private void receive(NBTTagCompound snapshot) {
        if (!client || snapshot == null || snapshot.getIntArray("state").length != 9
            || !snapshot.hasKey("itemsSize", 3) || !snapshot.hasKey("fluidsSize", 3)
            || !snapshot.hasKey("error", 3) || !snapshot.hasKey("active", 3)
            || !snapshot.hasKey("progress", 3) || !snapshot.hasKey("maxProgress", 3)) {
            return;
        }
        apply(snapshot);
        if (machine.getRecipeMaps().size() > 1) {
            machine.syncActiveRecipeMap(activeRecipeMapIndex);
        }
        receivedSnapshot = true;
    }

    private void apply(NBTTagCompound snapshot) {
        int[] state = snapshot.getIntArray("state");
        modes = state[0];
        result = CheckRecipeResult.fromOrdinal(state[1]);
        availableParallel = state[2];
        currentParallel = state[3];
        operations = state[4];
        eut = (state[5] & 0xffffffffL) | ((long) state[6] << 32);
        activeRecipeMapIndex = state[7];
        allowedToWork = state[8] != 0;
        storedEnergy = snapshot.getLong("storedEnergy");
        energyCapacity = snapshot.getLong("energyCapacity");
        outputItems = ProcessingPersistence.readItems(snapshot, "items");
        outputFluids = ProcessingPersistence.readFluids(snapshot, "fluids");
        mDisplayErrorCode = snapshot.getInteger("error");
        mActive = snapshot.getInteger("active");
        mProgressTime = snapshot.getInteger("progress");
        mMaxProgressTime = snapshot.getInteger("maxProgress");
    }
}
