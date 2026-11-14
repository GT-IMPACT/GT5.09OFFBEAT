package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Materials;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import static space.impact.structures.structure.StructureUtility.onElementPass;
import static space.impact.structures.structure.StructureUtility.transpose;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.TurbineProcessingLogic;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.items.GT_MetaGenerated_Tool_01;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

public abstract class GT_MetaTileEntity_LargeTurbine extends
        GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_LargeTurbine> implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SIDE_HINT = HintColor.GREEN;
    private static final HintColor DYNAMO_HINT = HintColor.YELLOW;
    private IStructureDefinition<GT_MetaTileEntity_LargeTurbine> structureDefinition;
    private StructureRequirements shell;
    private StructureRequirements dynamo;
    private int casingCount;

    public int baseEff = 0;
    public int optFlow = 0;
    public double realOptFlow = 0;
    protected long storedFluid;
    private long rotorPower;
    private int steamRemainder;
    private boolean fuelAchievement;

    public GT_MetaTileEntity_LargeTurbine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeTurbine(String aName) {
        super(aName);
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return getMaxEfficiency(aStack) > 0;
    }

    public abstract ICasing getCasingType();

    protected boolean requiresOutputHatch() {
        return false;
    }

    protected int getMaxOutputHatches() {
        return Integer.MAX_VALUE;
    }

    protected MultiblockTooltipBuilder turbineTooltip(String machineType) {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType(machineType)
                .addInfo("Insert a turbine rotor into the controller; it cannot be removed or replaced.")
                .beginStructureBlock(3, 3, 4, true)
                .addController("front center")
                .addStructureInfo("Equally facing turbine controllers cannot be 2 blocks apart horizontally.")
                .addStructureSection("Components")
                .addOtherStructurePart(getCasingType(), "24+; fill unused casing positions")
                .addAir("2 blocks between the controller and dynamo")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Maintenance Hatch", "exactly 1; around the 2 central air blocks")
                .addHatchInfo("Input Hatches", "1+; around the 2 central air blocks");
        if (requiresOutputHatch()) {
            tooltip.addHatchInfo("Output Hatches", "1+; around the 2 central air blocks");
        }
        if (getPollutionPerTick(null) > 0) {
            tooltip.addHatchInfo("Muffler", "exactly 1; around the 2 central air blocks, keep exhaust clear");
        }
        return tooltip
                .addHatchInfo("Dynamo Hatch", "exactly 1; center of the back wall")
                .addStructureHint("Around the central air: casings or hatches", SIDE_HINT)
                .addStructureHint("Back center: dynamo", DYNAMO_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: required hatches in free positions; existing casings are kept.");
    }

    private StructureRequirements getShellRequirements() {
        if (shell == null) {
            StructureRequirements.Builder builder = StructureRequirements.builder(getCasingType(), 24)
                    .atLeast(HatchElement.Maintenance, TurbineHatch.INPUT)
                    .atMost(HatchElement.Maintenance, 1)
                    .atMost(TurbineHatch.OUTPUT, getMaxOutputHatches());
            if (requiresOutputHatch()) {
                builder.atLeast(TurbineHatch.OUTPUT);
            }
            if (getPollutionPerTick(null) > 0) {
                builder.atLeast(TurbineHatch.MUFFLER).atMost(TurbineHatch.MUFFLER, 1);
            }
            shell = builder.build();
        }
        return shell;
    }

    private StructureRequirements getDynamoRequirements() {
        if (dynamo == null) {
            dynamo = StructureRequirements.builder(getCasingType(), 0).atLeast(TurbineHatch.DYNAMO).build();
        }
        return dynamo;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        boolean valid = checkPiece(STRUCTURE_PIECE, 1, 1, 0);
        for (byte side = 2; side < 6; side++) {
            IGregTechTileEntity neighbor = base.getIGregTechTileEntityAtSideAndDistance(side, 2);
            if (neighbor != null && neighbor.getFrontFacing() == base.getFrontFacing()
                    && neighbor.getMetaTileEntity() instanceof GT_MetaTileEntity_LargeTurbine) {
                return false;
            }
        }
        return valid && getShellRequirements().isSatisfied(this, casingCount)
                && getDynamoRequirements().isSatisfied(this, 0);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 1, 1, 0, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_LargeTurbine> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_LargeTurbine>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CHC", "CHC", "CCC"},
                            {"C~C", "H-H", "H-H", "CDC"},
                            {"CCC", "CHC", "CHC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', onElementPass(machine -> ++machine.casingCount, getCasingType().asElement()))
                    .addElement('H', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_LargeTurbine.class)
                            .allow(TurbineHatch.OUTPUT, TurbineHatch.MUFFLER)
                            .buildAndChain(getShellRequirements(), machine -> ++machine.casingCount)
                            .withHint(SIDE_HINT))
                    .addElement('D', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_LargeTurbine.class)
                            .hint(getCasingType(), DYNAMO_HINT)
                            .build(getDynamoRequirements()))
                    .build();
        }
        return structureDefinition;
    }

    private enum TurbineHatch implements IHatchElement {
        INPUT, OUTPUT, MUFFLER, DYNAMO;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
            case INPUT:
                return HatchElement.Input.matches(tile);
            case OUTPUT:
                return HatchElement.Output.matches(tile);
            case MUFFLER:
                return tile instanceof GT_MetaTileEntity_Hatch_Muffler;
            case DYNAMO:
                return tile instanceof GT_MetaTileEntity_Hatch_Dynamo
                        || tile instanceof GT_MetaTileEntity_Hatch_DynamoMulti;
            default:
                throw new AssertionError(this);
            }
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            switch (this) {
            case INPUT:
                return tile instanceof GT_MetaTileEntity_Hatch_Input
                        || tile instanceof GTMTE_BusHatch_Input;
            case OUTPUT:
                return tile instanceof GT_MetaTileEntity_Hatch_Output;
            default:
                return matches(tile);
            }
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
            case INPUT:
                return machine.mInputHatches.size()
                        + machine.mInputBusHatches.size();
            case OUTPUT:
                return machine.mOutputHatches.size();
            case MUFFLER:
                return machine.mMufflerHatches.size();
            case DYNAMO:
                return machine.mDynamoHatches.size() + machine.mDynamoHatchesMulti.size();
            default:
                throw new AssertionError(this);
            }
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            switch (this) {
            case INPUT:
                return machine.addInputToMachineList(tile, texture);
            case OUTPUT:
                return machine.addOutputToMachineList(tile, texture);
            case MUFFLER:
                return machine.addMufflerToMachineList(tile, texture);
            case DYNAMO:
                return machine.addDynamoToMachineList(tile, texture);
            default:
                throw new AssertionError(this);
            }
        }

        @Override
        public String getDescription() {
            switch (this) {
            case INPUT:
                return StatCollector.translateToLocal("gt.tooltip.hatch.input_hatch");
            case OUTPUT:
                return StatCollector.translateToLocal("gt.tooltip.hatch.output_hatch");
            case MUFFLER:
                return StatCollector.translateToLocal("gt.tooltip.hatch.muffler");
            case DYNAMO:
                return StatCollector.translateToLocal("gt.tooltip.hatch.dynamo");
            default:
                throw new AssertionError(this);
            }
        }
    }

    public boolean checkguislot = true;
    public String mNameRotorMaterial = "";

    public boolean isRotorLocked() {
        ItemStack rotor = mInventory[1];
        return rotor != null && rotor.getItem() instanceof GT_MetaGenerated_Tool;
    }

    @Override
    public boolean isInventorySlotLocked(int slot) {
        return slot == 1 && isRotorLocked();
    }

    @Override
    public boolean isValidSlot(int index) {
        return !isInventorySlotLocked(index) && super.isValidSlot(index);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setLong("rotorPower", rotorPower);
        aNBT.setInteger("steamRemainder", steamRemainder);
        aNBT.setBoolean("fuelAchievement", fuelAchievement);
        aNBT.setInteger("baseEff", baseEff);
        aNBT.setInteger("optFlow", optFlow);
        aNBT.setBoolean("checkguislot", checkguislot);
        aNBT.setString("mNameRotorMaterial", mNameRotorMaterial);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        baseEff = aNBT.getInteger("baseEff");
        optFlow = aNBT.getInteger("optFlow");
        checkguislot = aNBT.getBoolean("checkguislot");
        mNameRotorMaterial = aNBT.getString("mNameRotorMaterial");
        super.loadNBTData(aNBT);
        rotorPower = Math.max(0, aNBT.hasKey("rotorPower") ? aNBT.getLong("rotorPower") : lEUt);
        steamRemainder = Math.floorMod(aNBT.getInteger("steamRemainder"), 160);
        fuelAchievement = aNBT.getBoolean("fuelAchievement");
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        ItemStack aStack = this.mInventory[1];
        if (aTick % 100 == 0) {
            if (aStack != null && aStack.getItem() instanceof GT_MetaGenerated_Tool && checkguislot) {
                baseEff = GT_Utility.safeInt(
                        (long) ((5F + ((GT_MetaGenerated_Tool) aStack.getItem()).getToolCombatDamage(aStack)) * 1000F));
                optFlow = GT_Utility.safeInt((long) Math.max(Float.MIN_NORMAL,
                        ((GT_MetaGenerated_Tool) aStack.getItem()).getToolStats(aStack).getSpeedMultiplier()
                                * GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mToolSpeed * 50));
                Materials tMaterial = GT_MetaGenerated_Tool.getPrimaryMaterial(aStack);
                this.mNameRotorMaterial = GT_LanguageManager.getTranslation(aStack.getDisplayName())
                        + " | Material Rotor: " + tMaterial.mLocalizedName;
                this.checkguislot = false;
                markDirty();
            }
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new TurbineProcessingLogic(getFuelType(),
                () -> new TurbineProcessingLogic.State(optFlow, baseEff, rotorPower, steamRemainder),
                this::getFuelValue);
    }

    protected abstract TurbineProcessingLogic.FuelType getFuelType();

    public int getFuelValue(FluidStack fluid) {
        return 0;
    }

    @Override
    public boolean hasControllerSlotInGui() {
        return true;
    }

    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.PROTECT_FLUIDS;
    }

    @Override
    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        int previousEfficiency = mEfficiency;
        super.startCycle(result, efficiency);
        mEfficiency = Math.min(previousEfficiency, efficiency);
        mEfficiencyIncrease = 10;
        rotorPower = lEUt;
        TurbineProcessingLogic logic = (TurbineProcessingLogic) getProcessingLogic();
        realOptFlow = logic.getOptimalFlow();
        storedFluid = logic.getStoredFuel();
        steamRemainder = logic.getSteamRemainder();
        if (logic.getConsumedFuel() > 0) {
            awardFuelAchievement();
        }
    }

    private void awardFuelAchievement() {
        if (fuelAchievement || (getFuelType() != TurbineProcessingLogic.FuelType.STEAM
                && getFuelType() != TurbineProcessingLogic.FuelType.SUPERHEATED_STEAM)) {
            return;
        }
        try {
            String name = getFuelType() == TurbineProcessingLogic.FuelType.STEAM ? "muchsteam" : "efficientsteam";
            gregtech.GT_Mod.achievements.issueAchievement(
                    getBaseMetaTileEntity().getWorld().getPlayerEntityByName(getBaseMetaTileEntity().getOwnerName()),
                    name);
            fuelAchievement = true;
        } catch (Exception ignored) {
            // Achievement availability must not interrupt a committed fuel cycle.
        }
    }

    @Override
    public boolean checkRecipe(ItemStack stack) {
        boolean started = super.checkRecipe(stack);
        if (!started && mMaxProgresstime == 0 && !isWaitingForOutput()) {
            rotorPower = 0;
            lEUt = 0;
            mEUt = 0;
            mEfficiency = 0;
            if (optFlow <= 0 || baseEff <= 0) {
                checkguislot = true;
            }
        }
        return started;
    }

    @Override
    public void stopMachine() {
        rotorPower = 0;
        super.stopMachine();
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        if ((aStack != null && aStack.getItem() instanceof GT_MetaGenerated_Tool_01) || !checkguislot) {
            return 10000;
        }
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return true;
    }

    @Override
    public String[] getInfoData() {
        int mPollutionReduction = 0;
        for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                mPollutionReduction = Math.max(tHatch.calculatePollutionReduction(100), mPollutionReduction);
            }
        }

        String tRunning = mMaxProgresstime > 0
                ? EnumChatFormatting.GREEN + StatCollector.translateToLocal("GT5U.turbine.running.true")
                        + EnumChatFormatting.RESET
                : EnumChatFormatting.RED + StatCollector.translateToLocal("GT5U.turbine.running.false")
                        + EnumChatFormatting.RESET;

        String tMaintainance = getIdealStatus() == getRepairStatus()
                ? EnumChatFormatting.GREEN + StatCollector.translateToLocal("GT5U.turbine.maintenance.false")
                        + EnumChatFormatting.RESET
                : EnumChatFormatting.RED + StatCollector.translateToLocal("GT5U.turbine.maintenance.true")
                        + EnumChatFormatting.RESET;

        long storedEnergy = 0;
        long maxEnergy = 0;
        long maxAmp = 0;

        for (GT_MetaTileEntity_Hatch_Dynamo tHatch : mDynamoHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
                maxAmp += tHatch.getBaseMetaTileEntity().getOutputAmperage();
            }
        }

        for (GT_MetaTileEntity_Hatch_DynamoMulti tHatch : mDynamoHatchesMulti) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
                maxAmp += tHatch.getBaseMetaTileEntity().getOutputAmperage();
            }
        }

        String[] ret = new String[] {
                // 8 Lines available for information panels
                tRunning + ": " + EnumChatFormatting.RED + lEUt + EnumChatFormatting.RESET + " EU/t", /* 1 */
                tMaintainance, /* 2 */
                StatCollector.translateToLocal("GT5U.turbine.efficiency") + ": " + EnumChatFormatting.YELLOW
                        + (mEfficiency / 100F) + EnumChatFormatting.RESET + "%", /* 2 */
                StatCollector.translateToLocal("GT5U.multiblock.energy") + ": " + EnumChatFormatting.GREEN
                        + Long.toString(storedEnergy) + EnumChatFormatting.RESET + " EU / " + /* 3 */
                        EnumChatFormatting.YELLOW + Long.toString(maxEnergy) + EnumChatFormatting.RESET + " EU"
                        + EnumChatFormatting.YELLOW + " " + Long.toString(maxAmp) + EnumChatFormatting.RESET + " A",
                StatCollector.translateToLocal("GT5U.turbine.flow") + ": " + EnumChatFormatting.YELLOW
                        + GT_Utility.safeInt((long) realOptFlow) + EnumChatFormatting.RESET + " L/t" + /* 4 */
                        StatCollector.translateToLocal("GT5U.turbine.fuel") + ": " + EnumChatFormatting.GOLD
                        + storedFluid + EnumChatFormatting.RESET + "L", /* 5 */
                StatCollector.translateToLocal("GT5U.multiblock.pollution") + ": " + EnumChatFormatting.GREEN
                        + mPollutionReduction + EnumChatFormatting.RESET + " %", /* 6 */
                mNameRotorMaterial, /* 8 */
                "Turbine Effeciency: " + EnumChatFormatting.GREEN + baseEff / 100 + ".0", /* 9 */

        };

        if (!this.getClass().getName().contains("Steam")) {
            ret[4] = StatCollector.translateToLocal("GT5U.turbine.flow") + ": " + EnumChatFormatting.YELLOW
                    + GT_Utility.safeInt((long) realOptFlow) + EnumChatFormatting.RESET + " L/t";
        }
        return ret;
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

}
