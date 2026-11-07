package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingWorldChanges;
import gregtech.api.logic.WorldProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;

import static gregtech.api.enums.GT_Values.VN;
import static gregtech.api.enums.GT_Values.W;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public abstract class GT_MetaTileEntity_DrillerBase
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_DrillerBase>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor BASE_HINT = HintColor.GREEN;
    private IStructureDefinition<GT_MetaTileEntity_DrillerBase> structureDefinition;
    private StructureRequirements baseRequirements;
    private static final ItemStack miningPipe = GT_ModHandler.getIC2Item("miningPipe", 0);
    private static final ItemStack miningPipeTip = GT_ModHandler.getIC2Item("miningPipeTip", 0);
    private static final Block miningPipeBlock = GT_Utility.getBlockFromStack(miningPipe);
    private static final Block miningPipeTipBlock = GT_Utility.getBlockFromStack(miningPipeTip);

    protected boolean isPickingPipes;

    private int xDrill, yDrill, zDrill, xPipe, zPipe, yHead;
    protected int workState = STATE_DOWNWARD;
    protected static final int STATE_DOWNWARD = 0, STATE_AT_BOTTOM = 1, STATE_UPWARD = 2;

    public GT_MetaTileEntity_DrillerBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_DrillerBase(String aName) {
        super(aName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) return new ITexture[]{
                    getCasingType().getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ORE_DRILL_ACTIVE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ORE_DRILL_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    getCasingType().getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ORE_DRILL),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ORE_DRILL_GLOW).glow().build()};
        }
        return new ITexture[]{getCasingType().getTexture()};
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("workState", workState);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        workState = aNBT.getInteger("workState");
        if (aNBT.hasKey("isPickingPipes")) workState = aNBT.getBoolean("isPickingPipes") ? STATE_UPWARD : STATE_DOWNWARD;
    }

    @Override
    public boolean hasControllerSlotInGui() {
        return true;
    }

    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.PROTECT_ITEMS;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new WorldProcessingLogic(this::prepareOperation);
    }

    protected abstract long getDrillingEUt();

    protected abstract int getDrillingDuration();

    /** Retained for subclasses that inspect the drilling cycle's electricity statistics. */
    protected void setElectricityStats() {
        lEUt = -getDrillingEUt();
        mEUt = (int) Math.max(Integer.MIN_VALUE, lEUt);
        mMaxProgresstime = getDrillingDuration();
        mEfficiency = getCurrentEfficiency(null);
        mEfficiencyIncrease = 10000;
    }

    protected int getDrillingTier() {
        return Math.max(0, GT_Utility.getTier(getTotalInputVoltage()));
    }

    private boolean isEnergyEnough() {
        long requiredEnergy = Math.addExact(512L, Math.multiplyExact(getTotalInputVoltage(), 4L));
        for (GT_MetaTileEntity_Hatch_Energy hatch : mEnergyHatches) {
            requiredEnergy -= Math.min(requiredEnergy, Math.max(0, hatch.getEUVar()));
        }
        for (GT_MetaTileEntity_Hatch_EnergyMulti hatch : mEnergyHatchesMulti) {
            requiredEnergy -= Math.min(requiredEnergy, Math.max(0, hatch.getEUVar()));
        }
        return requiredEnergy == 0;
    }

    private WorldProcessingLogic.Operation prepareOperation(ProcessingLogic.Request request) {
        if (!isEnergyEnough()) {
            return WorldProcessingLogic.Operation.failure(CheckRecipeResult.INSUFFICIENT_STARTUP_ENERGY);
        }
        updateCoordinates();
        DrillingPlan plan = new DrillingPlan(request.inputs.get(0).copy());
        checkPipesAndSetYHead(plan);
        plan.loadPipes();
        CheckRecipeResult status;
        switch (workState) {
            case STATE_DOWNWARD:
                status = prepareDownward(plan);
                break;
            case STATE_AT_BOTTOM:
                status = prepareAtBottom(plan);
                break;
            case STATE_UPWARD:
                status = prepareUpward(plan);
                break;
            default:
                status = CheckRecipeResult.MACHINE_CONDITION;
        }
        if (!status.wasSuccessful()) {
            return WorldProcessingLogic.Operation.failure(status);
        }
        return new WorldProcessingLogic.Operation(getDrillingEUt(), getDrillingDuration(),
                plan.items, plan.fluids, plan::commit);
    }

    protected CheckRecipeResult prepareDownward(DrillingPlan plan) {
        switch (plan.lowerPipe(false)) {
            case 2:
                return CheckRecipeResult.NO_RECIPE;
            case 3:
                plan.nextState = STATE_UPWARD;
                break;
            case 1:
                plan.nextState = STATE_AT_BOTTOM;
                break;
            default:
                break;
        }
        return CheckRecipeResult.SUCCESS;
    }

    protected CheckRecipeResult prepareAtBottom(DrillingPlan plan) {
        plan.nextState = plan.lowerPipe(true) == 0 ? STATE_DOWNWARD : STATE_UPWARD;
        return CheckRecipeResult.SUCCESS;
    }

    protected CheckRecipeResult prepareUpward(DrillingPlan plan) {
        if (plan.pickPipe()) {
            return CheckRecipeResult.SUCCESS;
        }
        workState = STATE_DOWNWARD;
        stopMachine();
        return CheckRecipeResult.NO_RECIPE;
    }

    protected boolean reachingVoidOrBedrock() {
        return yHead <= 0 || checkBlockAndMeta(xPipe, yHead - 1, zPipe, Blocks.bedrock, W);
    }

    protected boolean mayBreakBlock(int x, int y, int z) {
        return GT_Utility.eraseBlockByFakePlayer(getFakePlayer(getBaseMetaTileEntity()), x, y, z, true);
    }

    protected boolean mayPlaceBlock(int x, int y, int z, Block block, int meta) {
        return GT_Utility.setBlockByFakePlayer(getFakePlayer(getBaseMetaTileEntity()), x, y, z, block, meta, true);
    }

    /** Inventory and world changes stay provisional until power and output checks have passed. */
    protected final class DrillingPlan {
        public final ProcessingInputs inputs;
        public final ProcessingWorldChanges world = new ProcessingWorldChanges(getBaseMetaTileEntity().getWorld());
        public final int pipeX = xPipe;
        public final int pipeZ = zPipe;
        public final int drillY = yDrill;
        public int headY;
        public int nextState = workState;
        public ItemStack[] items = new ItemStack[0];
        public FluidStack[] fluids = new FluidStack[0];
        public BooleanSupplier commitResource = () -> true;
        public Runnable afterCommit = () -> {};
        private final ItemStack originalController = getStackInSlot(1);
        private final ItemStack beforeController = originalController == null ? null : originalController.copy();
        private ItemStack controller = beforeController == null ? null : beforeController.copy();

        private DrillingPlan(ProcessingInputs inputs) {
            this.inputs = inputs;
        }

        private boolean controllerUnchanged() {
            return originalController == getStackInSlot(1)
                    && ItemStack.areItemStacksEqual(beforeController, getStackInSlot(1));
        }

        private void loadPipes() {
            if (controller != null && (!controller.isItemEqual(miningPipe)
                    || controller.stackSize >= controller.getMaxStackSize())) {
                return;
            }
            for (ItemStack item : inputs.items) {
                if (!item.isItemEqual(miningPipe)) {
                    continue;
                }
                if (controller == null) {
                    controller = GT_Utility.copyAmount(0, miningPipe);
                }
                int count = Math.min(Math.max(0, item.stackSize), controller.getMaxStackSize() - controller.stackSize);
                controller.stackSize += count;
                item.stackSize -= count;
            }
        }

        /** 0: ready, 1: bottom, 2: missing pipes, 3: protected block. */
        public int lowerPipe(boolean simulate) {
            if (controller == null || controller.stackSize <= 0 || !controller.isItemEqual(miningPipe)) {
                return 2;
            }
            if (headY <= 0 || GT_Utility.getBlockHardnessAt(getBaseMetaTileEntity().getWorld(),
                    pipeX, headY - 1, pipeZ) < 0) {
                return 1;
            }
            Block below = getBaseMetaTileEntity().getBlock(pipeX, headY - 1, pipeZ);
            world.observe(pipeX, headY - 1, pipeZ);
            if (!mayBreakBlock(pipeX, headY - 1, pipeZ)
                    || below != miningPipeTipBlock && !mayPlaceBlock(pipeX, headY - 1, pipeZ, miningPipeTipBlock, 0)) {
                return 3;
            }
            if (!simulate) {
                if (below != miningPipeTipBlock) {
                    world.setBlock(pipeX, headY - 1, pipeZ, miningPipeTipBlock, 0);
                }
                if (headY != drillY) {
                    world.setBlock(pipeX, headY, pipeZ, miningPipeBlock, 0);
                }
                if (below != miningPipeBlock && below != miningPipeTipBlock) {
                    controller.stackSize--;
                }
            }
            return 0;
        }

        public boolean pickPipe() {
            if (headY == drillY) {
                return false;
            }
            if (controller == null) {
                controller = GT_Utility.copyAmount(1, miningPipe);
            } else if (controller.isItemEqual(miningPipe) && controller.stackSize < controller.getMaxStackSize()) {
                controller.stackSize++;
            } else {
                items = new ItemStack[]{GT_Utility.copyAmount(1, miningPipe)};
            }
            if (checkBlockAndMeta(pipeX, headY + 1, pipeZ, miningPipeBlock, W)) {
                world.setBlock(pipeX, headY + 1, pipeZ, miningPipeTipBlock, 0);
            }
            world.setBlock(pipeX, headY, pipeZ, Blocks.air, 0);
            return true;
        }

        private boolean commit() {
            if (!isEnergyEnough() || !controllerUnchanged() || !inputs.isUnchanged() || !world.commit()) {
                return false;
            }
            // Block callbacks may have changed an inventory. Roll back before spending anything.
            if (!controllerUnchanged() || !inputs.isUnchanged() || !commitResource.getAsBoolean()) {
                world.rollback();
                return false;
            }
            if (!inputs.commit()) {
                world.rollback();
                return false;
            }
            setInventorySlotContents(1, controller == null || controller.stackSize <= 0 ? null : controller);
            workState = nextState;
            isPickingPipes = workState == STATE_UPWARD;
            afterCommit.run();
            return true;
        }
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        updateCoordinates();
        mDataAccessHatches.clear();
        return checkPiece(STRUCTURE_PIECE, 1, 6, 0) && getBaseRequirements().isSatisfied(this, 0);
    }

    public final StructureRequirements getBaseRequirements() {
        if (baseRequirements == null) {
            baseRequirements = createBaseRequirements()
                    .atLeast(HatchElement.Maintenance, new DrillerEnergy(getMinTier()))
                    .build();
        }
        return baseRequirements;
    }

    protected abstract StructureRequirements.Builder createBaseRequirements();

    protected final MultiblockTooltipBuilder addDrillerStructure(MultiblockTooltipBuilder tooltip) {
        return tooltip.beginStructureBlock(3, 7, 3, false)
                .addController("Front center, bottom layer")
                .addOtherStructurePart(getCasingType(), "Base and central pillar; hatches replace base casings", 11)
                .addOtherStructurePart(GT_OreDictUnificator.get(OrePrefixes.frameGt, getFrameMaterial(), 1),
                        "Beside the pillar and above it", 15)
                .addRequirements(getBaseRequirements(), "Base only", BASE_HINT)
                .addStructureHint("Input Bus / Data Access (optional, base)", BASE_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Places maintenance and sufficiently tiered energy hatches in the drill base, plus the service hatches required by this drill variant.")
                .toolTipFinisher();
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_DrillerBase> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_DrillerBase>builder()
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"   ", " F ", "   "},
                            {"   ", " F ", "   "},
                            {"   ", " F ", "   "},
                            {" F ", "FCF", " F "},
                            {" F ", "FCF", " F "},
                            {" F ", "FCF", " F "},
                            {"H~H", "HHH", "HHH"}
                    }))
                    .addElement('C', getCasingType().asElement())
                    .addElement('F', GTStructureUtility.ofFrame(getFrameMaterial()))
                    .addElement('H', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DrillerBase.class)
                            .allow(HatchElement.Input, HatchElement.Output, DrillerHatch.DATA)
                            .buildAndChain(getBaseRequirements()).withHint(BASE_HINT))
                    .build();
        }
        return structureDefinition;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 1, 6, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int budget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 1, 6, 0, budget, environment);
    }

    private void updateCoordinates() {
        xDrill = getBaseMetaTileEntity().getXCoord();
        yDrill = getBaseMetaTileEntity().getYCoord();
        zDrill = getBaseMetaTileEntity().getZCoord();
        ForgeDirection back = ForgeDirection.getOrientation(getBaseMetaTileEntity().getBackFacing());
        xPipe = xDrill + back.offsetX;
        zPipe = zDrill + back.offsetZ;
    }

    private void checkPipesAndSetYHead(DrillingPlan plan) {
        yHead = yDrill - 1;
        while (yHead >= 0 && checkBlockAndMeta(xPipe, yHead, zPipe, miningPipeBlock, W)) {
            plan.world.observe(xPipe, yHead, zPipe);
            yHead--;
        }
        if (yHead >= 0) {
            plan.world.observe(xPipe, yHead, zPipe);
        }
        if (!checkBlockAndMeta(xPipe, yHead, zPipe, miningPipeTipBlock, W) && ++yHead != yDrill) {
            plan.world.setBlock(xPipe, yHead, zPipe, miningPipeTipBlock, 0);
        }
        if (yHead > 0) {
            plan.world.observe(xPipe, yHead - 1, zPipe);
        }
        plan.headY = yHead;
    }

    private boolean checkBlockAndMeta(int x, int y, int z, Block block, int meta) {
        return (meta == W || getBaseMetaTileEntity().getMetaID(x, y, z) == meta)
                && getBaseMetaTileEntity().getBlock(x, y, z) == block;
    }

    private FakePlayer mFakePlayer = null;

    protected FakePlayer getFakePlayer(IGregTechTileEntity aBaseTile) {
        if (mFakePlayer == null) mFakePlayer = GT_Utility.getFakePlayer(aBaseTile);
        mFakePlayer.setWorld(aBaseTile.getWorld());
        mFakePlayer.setPosition(aBaseTile.getXCoord(), aBaseTile.getYCoord(), aBaseTile.getZCoord());
        return mFakePlayer;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    public abstract ICasing getCasingType();

    protected abstract Materials getFrameMaterial();

    protected abstract int getMinTier();

    public int getTotalConfigValue(){
        int config = 0;
        ArrayList<ItemStack> tCircuitList = getDataItems(1);
        for (ItemStack tCircuit : tCircuitList)
            config += tCircuit.getItemDamage();
        return config;
    }

    public ArrayList<GT_MetaTileEntity_Hatch_DataAccess> mDataAccessHatches = new ArrayList<GT_MetaTileEntity_Hatch_DataAccess>();

    /**
     * @param state using bitmask, 1 for IntegratedCircuit, 2 for DataStick, 4 for DataOrb
     */
    private boolean isCorrectDataItem(ItemStack aStack, int state){
        if ((state & 1) != 0 && ItemList.Circuit_Integrated.isStackEqual(aStack, true, true)) return true;
        if ((state & 2) != 0 && ItemList.Tool_DataStick.isStackEqual(aStack, false, true)) return true;
        if ((state & 4) != 0 && ItemList.Tool_DataOrb.isStackEqual(aStack, false, true)) return true;
        return false;
    }

    /**
     * @param state using bitmask, 1 for IntegratedCircuit, 2 for DataStick, 4 for DataOrb
     */
    public ArrayList<ItemStack> getDataItems(int state) {
        ArrayList<ItemStack> rList = new ArrayList<ItemStack>();
        if (GT_Utility.isStackValid(mInventory[1]) && isCorrectDataItem(mInventory[1], state)) {
            rList.add(mInventory[1]);
        }
        for (GT_MetaTileEntity_Hatch_DataAccess tHatch : mDataAccessHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                for (int i = 0; i < tHatch.getBaseMetaTileEntity().getSizeInventory(); i++) {
                    if (tHatch.getBaseMetaTileEntity().getStackInSlot(i) != null
                            && isCorrectDataItem(tHatch.getBaseMetaTileEntity().getStackInSlot(i), state))
                        rList.add(tHatch.getBaseMetaTileEntity().getStackInSlot(i));
                }
            }
        }
        return rList;
    }

    public boolean addDataAccessToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_DataAccess) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            if (!mDataAccessHatches.contains(aMetaTileEntity)) {
                mDataAccessHatches.add((GT_MetaTileEntity_Hatch_DataAccess) aMetaTileEntity);
            }
            return true;
        }
        return false;
    }

    /** Combined nominal voltage determines the tier; hatch amperage only supplies throughput. */
    private static final class DrillerEnergy implements IHatchElement {
        private final int tier;

        private DrillerEnergy(int tier) {
            this.tier = tier;
        }

        @Override
        public boolean matches(IMetaTileEntity tile) {
            return HatchElement.Energy.matches(tile);
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            if (!matches(tile)) return false;
            long voltage = ((GT_MetaTileEntity_Hatch) tile).maxEUInput();
            return GT_Utility.getTier(voltage) >= tier;
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            long voltage = ((GT_MetaTileEntity_DrillerBase) machine).getTotalInputVoltage();
            return GT_Utility.getTier(voltage) >= tier ? HatchElement.Energy.count(machine) : 0;
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return HatchElement.Energy.add(machine, tile, texture);
        }

        @Override
        public String getDescription() {
            return StatCollector.translateToLocal("gt.tooltip.hatch.energy") + " (" + VN[tier] + "+)";
        }
    }

    protected enum DrillerHatch implements IHatchElement {
        FLUID_INPUT,
        FLUID_OUTPUT,
        ITEM_OUTPUT,
        DATA;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
                case FLUID_INPUT:
                    return tile instanceof GT_MetaTileEntity_Hatch_Input || tile instanceof GTMTE_BusHatch_Input;
                case FLUID_OUTPUT:
                    return tile instanceof GT_MetaTileEntity_Hatch_Output;
                case ITEM_OUTPUT:
                    return tile instanceof GT_MetaTileEntity_Hatch_OutputBus;
                case DATA:
                    return tile instanceof GT_MetaTileEntity_Hatch_DataAccess;
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
                case FLUID_INPUT:
                    return machine.mInputHatches.size() + machine.mInputBusHatches.size();
                case FLUID_OUTPUT:
                    return machine.mOutputHatches.size();
                case ITEM_OUTPUT:
                    return machine.mOutputBusses.size();
                case DATA:
                    return ((GT_MetaTileEntity_DrillerBase) machine).mDataAccessHatches.size();
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            switch (this) {
                case FLUID_INPUT:
                    return machine.addInputToMachineList(tile, texture);
                case FLUID_OUTPUT:
                case ITEM_OUTPUT:
                    return machine.addOutputToMachineList(tile, texture);
                case DATA:
                    return ((GT_MetaTileEntity_DrillerBase) machine).addDataAccessToMachineList(tile, texture);
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public String getDescription() {
            switch (this) {
                case FLUID_INPUT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.input_hatch");
                case FLUID_OUTPUT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.output_hatch");
                case ITEM_OUTPUT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.output_bus");
                case DATA:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.data_access");
                default:
                    throw new AssertionError(this);
            }
        }
    }

}
