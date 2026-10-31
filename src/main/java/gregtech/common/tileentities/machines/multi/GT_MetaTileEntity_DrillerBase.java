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

import space.impact.api.alignment.constructable.ISurvivalConstructable;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.structure.StructureDefinition;

import java.util.ArrayList;

import static gregtech.api.enums.GT_Values.VN;
import static gregtech.api.enums.GT_Values.W;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.api.structure.StructureUtility.transpose;

public abstract class GT_MetaTileEntity_DrillerBase
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_DrillerBase>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor BASE_HINT = HintColor.BLUE;
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

    protected boolean tryPickPipe() {
        if (yHead == yDrill)
            return isPickingPipes = false;
        if (tryOutputPipe()){
            if (checkBlockAndMeta(xPipe, yHead + 1, zPipe, miningPipeBlock, W))
                getBaseMetaTileEntity().getWorld().setBlock(xPipe, yHead + 1, zPipe, miningPipeTipBlock);
            getBaseMetaTileEntity().getWorld().setBlockToAir(xPipe, yHead, zPipe);
            return isPickingPipes = true;
        }
        return isPickingPipes = false;
    }

    /**
     * Added for compability reasons
     * @return true if the state is 0 false otherwise.
     */
    protected boolean tryLowerPipe() {
        return tryLowerPipeState(false) == 0;
    }


    /**
     * @return 0 for succeeded, 1 for invalid block, 2 for not having mining pipes, 3 for event canceled.
     */
    protected int tryLowerPipeState() {
        return tryLowerPipeState(false);
    }

    /**
     * @return 0 for succeeded, 1 for invalid block, 2 for not having mining pipes, 3 for event canceled.
     */
    protected int tryLowerPipeState(boolean isSimulating) {
        if (!isHasMiningPipes()) return 2;
        switch (canLowerPipe()) {
            case 1: return 1;
            case 2: return 3;
        }

        Block block = getBaseMetaTileEntity().getBlock(xPipe, yHead - 1, zPipe);
        if (block != miningPipeTipBlock && !GT_Utility.setBlockByFakePlayer(getFakePlayer(getBaseMetaTileEntity()),
                xPipe, yHead - 1, zPipe, miningPipeTipBlock, 0, isSimulating)) {
            return 3;
        }
        if (!isSimulating) {
            if (yHead != yDrill) {
                getBaseMetaTileEntity().getWorld().setBlock(xPipe, yHead, zPipe, miningPipeBlock);
            }
            if (block != miningPipeBlock && block != miningPipeTipBlock) {
                getBaseMetaTileEntity().decrStackSize(1, 1);
            }
        }

        return 0;
    }

    private void putMiningPipesFromInputsInController() {
        int maxPipes = miningPipe.getMaxStackSize();
        if (isHasMiningPipes(maxPipes)) return;

        ItemStack pipes = getStackInSlot(1);
        if(pipes != null && !pipes.isItemEqual(miningPipe)) return;
        for (ItemStack storedItem : getStoredInputs()) {
            if (!storedItem.isItemEqual(miningPipe)) continue;

            if (pipes == null) {
                setInventorySlotContents(1, GT_Utility.copy(miningPipe));
                pipes = getStackInSlot(1);
            }

            if (pipes.stackSize == maxPipes) break;

            int needPipes = maxPipes - pipes.stackSize;
            int transferPipes = storedItem.stackSize < needPipes ? storedItem.stackSize : needPipes;

            pipes.stackSize += transferPipes;
            storedItem.stackSize -= transferPipes;
        }
        updateSlots();
    }

    private boolean tryOutputPipe(){
        if (!getBaseMetaTileEntity().addStackToSlot(1, GT_Utility.copyAmount(1, miningPipe)))
            mOutputItems = new ItemStack[] {GT_Utility.copyAmount(1, miningPipe)};
        return true;
    }

    /**
     * @return 0 for available, 1 for invalid block, 2 for event canceled.
     */
    protected int canLowerPipe(){
        IGregTechTileEntity aBaseTile = getBaseMetaTileEntity();
        if (yHead > 0 && GT_Utility.getBlockHardnessAt(aBaseTile.getWorld(), xPipe, yHead - 1, zPipe) >= 0) {
            return GT_Utility.eraseBlockByFakePlayer(getFakePlayer(aBaseTile), xPipe, yHead - 1, zPipe, true) ? 0 : 2;
        }
        return 1;
    }

    protected boolean reachingVoidOrBedrock() {
        return yHead <= 0 || checkBlockAndMeta(xPipe, yHead - 1, zPipe, Blocks.bedrock, W);
    }

    private boolean isHasMiningPipes() {
        return isHasMiningPipes(1);
    }

    private boolean isHasMiningPipes(int minCount) {
        ItemStack pipe = getStackInSlot(1);
        return pipe != null && pipe.stackSize > minCount - 1 && pipe.isItemEqual(miningPipe);
    }

    /**
     * Readded for compability
     * @return if no pipes are present
     */
    protected boolean waitForPipes(){
        return !isHasMiningPipes();
    }

    private boolean isEnergyEnough() {
        long requiredEnergy = 512 + getMaxInputVoltage() * 4;
        for (GT_MetaTileEntity_Hatch_Energy energyHatch : mEnergyHatches) {
            requiredEnergy -= energyHatch.getEUVar();
            if (requiredEnergy <= 0) return true;
        }
        for (GT_MetaTileEntity_Hatch_EnergyMulti energyHatch : mEnergyHatchesMulti) {
            requiredEnergy -= energyHatch.getEUVar();
            if (requiredEnergy <= 0) return true;
        }
        return false;
    }

    protected boolean workingDownward(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe, int yHead, int oldYHead) {
        switch (tryLowerPipeState()) {
            case 2: mMaxProgresstime = 0; return false;
            case 3: workState = STATE_UPWARD; return true;
            case 1: workState = STATE_AT_BOTTOM; return true;
            default: return true;
        }
    }

    protected boolean workingAtBottom(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe, int yHead, int oldYHead) {
        switch (tryLowerPipeState(true)) {
            case 0: workState = STATE_DOWNWARD; return true;
            default: workState = STATE_UPWARD; return true;
        }
    }

    protected boolean workingUpward(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe, int yHead, int oldYHead) {
        if (tryPickPipe()) {
            return true;
        } else {
            workState = STATE_DOWNWARD;
            stopMachine();
            return false;
        }
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        //Public pipe actions
        updateCoordinates();
        setElectricityStats();
        int oldYHead = yHead;
        if (!checkPipesAndSetYHead() || !isEnergyEnough()) {
            stopMachine();
            return false;
        }
        putMiningPipesFromInputsInController();
        switch (workState) {
            case STATE_DOWNWARD:
                return workingDownward(aStack, xDrill, yDrill, zDrill, xPipe, zPipe, yHead, oldYHead);
            case STATE_AT_BOTTOM:
                return workingAtBottom(aStack, xDrill, yDrill, zDrill, xPipe, zPipe, yHead, oldYHead);
            case STATE_UPWARD:
                return workingUpward(aStack, xDrill, yDrill, zDrill, xPipe, zPipe, yHead, oldYHead);
            default:
                return false;
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
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Auto-place required hatches")
                .toolTipFinisher();
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_DrillerBase> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_DrillerBase>builder()
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

    private boolean checkPipesAndSetYHead() {
        yHead = yDrill - 1;
        while (checkBlockAndMeta(xPipe, yHead, zPipe, miningPipeBlock, W)) yHead--; //skip pipes
        //is pipe tip OR is controller layer
        if (checkBlockAndMeta(xPipe, yHead, zPipe, miningPipeTipBlock, W) || ++yHead == yDrill) return true;
        //pipe column is broken - try fix
        getBaseMetaTileEntity().getWorld().setBlock(xPipe, yHead, zPipe, miningPipeTipBlock);
        return true;
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

    protected abstract void setElectricityStats();

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

    /** Keep the legacy combined input-voltage check, but select a sufficient hatch during construction. */
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
            if (tile instanceof GT_MetaTileEntity_Hatch_EnergyMulti) {
                voltage *= ((GT_MetaTileEntity_Hatch_EnergyMulti) tile).Amp;
            }
            return GT_Utility.getTier(voltage) >= tier;
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            return GT_Utility.getTier(machine.getMaxInputVoltage()) >= tier ? HatchElement.Energy.count(machine) : 0;
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
                    return machine.mInputHatches.size() + machine.mQuadrInputHatches.size() + machine.mInputBusHatches.size();
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
