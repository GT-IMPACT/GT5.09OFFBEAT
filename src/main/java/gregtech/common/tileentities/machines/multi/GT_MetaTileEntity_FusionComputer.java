package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GTMTE_Multi_Hatch_Input;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public abstract class GT_MetaTileEntity_FusionComputer
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_FusionComputer>
        implements ISurvivalConstructable {

    private static final String STRUCTURE_PIECE = "main";
    // The controller faces the ring center, five blocks ahead, as in the original structure.
    private static final int OFFSET_X = 7;
    private static final int OFFSET_Y = 1;
    private static final int OFFSET_Z = 12;
    private IStructureDefinition<GT_MetaTileEntity_FusionComputer> structureDefinition;
    private final Map<FusionHatch, StructureRequirements> requirements = new EnumMap<>(FusionHatch.class);

    public GT_Recipe mLastRecipe;
    public int mEUStore;
    private boolean continuingReaction;
    private long startupEnergy;

    public GT_MetaTileEntity_FusionComputer(int aID, String aName, String aNameRegional, int tier) {
        super(aID, aName, aNameRegional);
        enabledMaintenance = false;
    }

    public GT_MetaTileEntity_FusionComputer(String aName) {
        super(aName);
        enabledMaintenance = false;
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sFusionRecipes;
    }

    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.BATCH | ProcessingLogic.PROTECT_FLUIDS | ProcessingLogic.LOCK_RECIPE;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(this::getRecipeMap)
                .setRecipeValidator(this::validateFusionRecipe)
                .setCalculatorFactory(recipe -> {
                    int multiplier = overclock(recipe.mSpecialValue, recipe.mDuration, recipe.mEUt);
                    return new OverclockCalculator().setMaxOverclocks(0)
                            .setEUtModifier(multiplier).setDurationModifier(1.0 / multiplier);
                });
    }

    private CheckRecipeResult validateFusionRecipe(GT_Recipe recipe) {
        if (recipe.mSpecialValue < 0 || recipe.mSpecialValue > maxEUStore()) {
            return CheckRecipeResult.INSUFFICIENT_STARTUP_CAPACITY;
        }
        int multiplier = overclock(recipe.mSpecialValue, recipe.mDuration, recipe.mEUt);
        if (multiplier == 0) {
            return CheckRecipeResult.INSUFFICIENT_POWER;
        }
        long startup = Math.max(0, recipe.mSpecialValue - (long) recipe.mEUt * multiplier);
        if (!continuingReaction && getBaseMetaTileEntity().getStoredEU() < startup) {
            return CheckRecipeResult.INSUFFICIENT_STARTUP_ENERGY;
        }
        startupEnergy = continuingReaction ? 0 : startup;
        return CheckRecipeResult.SUCCESS;
    }

    @Override
    protected List<ProcessingInputs> collectProcessingInputs() {
        List<ProcessingInputs> guarded = new ArrayList<>();
        for (ProcessingInputs inputs : super.collectProcessingInputs()) {
            guarded.add(inputs.withCommitGuard(() -> getBaseMetaTileEntity().getStoredEU() >= startupEnergy));
        }
        return guarded;
    }

    @Override
    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        if (!continuingReaction) {
            long startup = Math.max(0, result.recipe.mSpecialValue - result.eut);
            getBaseMetaTileEntity().decreaseStoredEnergyUnits(startup, false);
        }
        super.startCycle(result, 10000);
        mLastRecipe = result.recipe;
        mEUStore = (int) getBaseMetaTileEntity().getStoredEU();
        mRunningOnLoad = false;
        turnCasingActive(true);
    }

    @Override
    protected void completeRecipe() {
        continuingReaction = true;
        try {
            super.completeRecipe();
        } finally {
            continuingReaction = false;
            if (mMaxProgresstime == 0 && !isWaitingForOutput()) {
                mLastRecipe = null;
            }
            turnCasingActive(mMaxProgresstime > 0 && !isWaitingForOutput());
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (!tag.hasKey("processing")) {
            lEUt = -Math.abs((long) tag.getInteger("mEUt"));
            mEUt = (int) lEUt;
        }
        continuingReaction = false;
        mLastRecipe = null;
    }

    public abstract int tier();

    @Override
    public long getProcessingEnergyCapacity() {
        return maxEUStore();
    }

    @Override
    public long getProcessingStoredEnergy() {
        return getBaseMetaTileEntity().getStoredEU();
    }

    @Override
    public abstract long maxEUStore();

    public abstract MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity);

    public boolean allowCoverOnSide(byte aSide, GT_ItemStack aStack) {

        return aSide != getBaseMetaTileEntity().getFrontFacing();
    }

    static {
        Textures.BlockIcons.setCasingTextureForId(Casings.FUSION_GLASS_YELLOW.getTextureId(),
                TextureFactory.of(
                        TextureFactory.of(MACHINE_CASING_FUSION_GLASS_YELLOW),
                        TextureFactory.builder().addIcon(MACHINE_CASING_FUSION_GLASS_YELLOW_GLOW).glow().build()
                ));
    }

    public abstract ICasing getCasingType();

    public abstract ICasing getFusionCoilType();

    // Keep the block/meta accessors for integrations using the previous API.
    public Block getCasing() {
        return getCasingType().getBlock();
    }

    public int getCasingMeta() {
        return getCasingType().getMetadata();
    }

    public Block getFusionCoil() {
        return getFusionCoilType().getBlock();
    }

    public int getFusionCoilMeta() {
        return getFusionCoilType().getMetadata();
    }

    private enum FusionHatch {
        ENERGY(HatchElement.Energy, GT_MetaTileEntity_Hatch_Energy.class, "energy", 1, HintColor.YELLOW),
        INPUT(HatchElement.Input, GT_MetaTileEntity_Hatch_Input.class, "input_hatch", 2, HintColor.BLUE),
        OUTPUT(HatchElement.Output, GT_MetaTileEntity_Hatch_Output.class, "output_hatch", 1, HintColor.RED);

        final HatchElement category;
        final Class<? extends GT_MetaTileEntity_Hatch> type;
        final String translation;
        final int minimum;
        final HintColor hintColor;

        FusionHatch(HatchElement category, Class<? extends GT_MetaTileEntity_Hatch> type,
                    String translation, int minimum, HintColor hintColor) {
            this.category = category;
            this.type = type;
            this.translation = translation;
            this.minimum = minimum;
            this.hintColor = hintColor;
        }
    }

    private StructureRequirements requirements(FusionHatch kind) {
        return requirements.computeIfAbsent(kind, key -> {
            int minimumTier = tier();
            IHatchElement hatch = new IHatchElement() {
                @Override
                public boolean matches(IMetaTileEntity tile) {
                    // Retain optional buses and IMPACT hatches accepted by the legacy structure.
                    return key.category.matches(tile) && (!key.type.isInstance(tile)
                            || ((GT_MetaTileEntity_Hatch) tile).mTier >= minimumTier);
                }

                @Override
                public boolean matchesForPlacement(IMetaTileEntity tile) {
                    return key.type.isInstance(tile) && ((GT_MetaTileEntity_Hatch) tile).mTier >= minimumTier;
                }

                @Override
                public int count(GT_MetaTileEntity_MultiBlockBase machine) {
                    // Optional legacy variants do not replace the mandatory standard hatches.
                    switch (key) {
                        case ENERGY:
                            return machine.mEnergyHatches.size();
                        case INPUT:
                            int standardInputs = 0;
                            for (GT_MetaTileEntity_Hatch_Input input : machine.mInputHatches) {
                                if (!(input instanceof GTMTE_Multi_Hatch_Input)) {
                                    standardInputs++;
                                }
                            }
                            return standardInputs;
                        case OUTPUT:
                            return machine.mOutputHatches.size();
                        default:
                            throw new AssertionError(key);
                    }
                }

                @Override
                public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
                    return key.category.add(machine, tile, Casings.FUSION_GLASS.getTextureId());
                }

                @Override
                public String getDescription() {
                    return StatCollector.translateToLocal("gt.tooltip.hatch." + key.translation);
                }
            };
            return StructureRequirements.builder(getCasingType(), 0).atLeast(hatch, key.minimum).build();
        });
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        if (!checkPiece(STRUCTURE_PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z)) {
            return false;
        }
        for (FusionHatch kind : FusionHatch.values()) {
            if (!requirements(kind).isSatisfied(this, 0)) {
                return false;
            }
        }
        mWrench = true;
        mScrewdriver = true;
        mSoftHammer = true;
        mHardHammer = true;
        mSolderingTool = true;
        mCrowbar = true;
        return true;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, OFFSET_X, OFFSET_Y, OFFSET_Z, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_FusionComputer> getStructureDefinition() {
        if (structureDefinition == null) {
            // Spaces are deliberately unconstrained: the old reactor did not require air outside its ring.
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_FusionComputer>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {
                                    "               ",
                                    "      ICI      ",
                                    "    CC   CC    ",
                                    "   C       C   ",
                                    "  C         C  ",
                                    "  C         C  ",
                                    " I           I ",
                                    " C           C ",
                                    " I           I ",
                                    "  C         C  ",
                                    "  C         C  ",
                                    "   C       C   ",
                                    "    CC   CC    ",
                                    "      ICI      ",
                                    "               "
                            },
                            {
                                    "      OCO      ",
                                    "    CCKKKCC    ",
                                    "   EKKOCOKKE   ",
                                    "  EKEC   CEKE  ",
                                    " CKE       EKC ",
                                    " CKC       CKC ",
                                    "OKO         OKO",
                                    "CKC         CKC",
                                    "OKO         OKO",
                                    " CKC       CKC ",
                                    " CKE       EKC ",
                                    "  EKEC   CEKE  ",
                                    "   EKKO~OKKE   ",
                                    "    CCKKKCC    ",
                                    "      OCO      "
                            },
                            {
                                    "               ",
                                    "      ICI      ",
                                    "    CC   CC    ",
                                    "   C       C   ",
                                    "  C         C  ",
                                    "  C         C  ",
                                    " I           I ",
                                    " C           C ",
                                    " I           I ",
                                    "  C         C  ",
                                    "  C         C  ",
                                    "   C       C   ",
                                    "    CC   CC    ",
                                    "      ICI      ",
                                    "               "
                            }
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', getCasingType().asElement())
                    .addElement('K', getFusionCoilType().asElement())
                    .addElement('E', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_FusionComputer.class)
                            .buildAndChain(requirements(FusionHatch.ENERGY))
                            .withHint(FusionHatch.ENERGY.hintColor))
                    .addElement('I', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_FusionComputer.class)
                            .buildAndChain(requirements(FusionHatch.INPUT))
                            .withHint(FusionHatch.INPUT.hintColor))
                    .addElement('O', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_FusionComputer.class)
                            .buildAndChain(requirements(FusionHatch.OUTPUT))
                            .withHint(FusionHatch.OUTPUT.hintColor))
                    .build();
        }
        return structureDefinition;
    }

    protected MultiblockTooltipBuilder fusionTooltip(String description, String energyInfo) {
        int requiredHatches = 0;
        for (FusionHatch kind : FusionHatch.values()) {
            for (int minimum : requirements(kind).getHatches().values()) {
                requiredHatches += minimum;
            }
        }
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Fusion Reactor")
                .addInfo(description)
                .addInfo(energyInfo)
                .beginStructureBlock(15, 3, 15, false)
                .addController("Inner edge of the ring, facing the center, five blocks away")
                .addCasingInfoRange(getCasingType(), 79, 127 - requiredHatches, false)
                .addOtherStructurePart(getFusionCoilType(), "Middle layer of the ring", 32)
                .addRequirements(requirements(FusionHatch.ENERGY), "16 diagonal positions in the middle layer",
                        FusionHatch.ENERGY.hintColor)
                .addRequirements(requirements(FusionHatch.INPUT), "16 cardinal positions in the top and bottom layers",
                        FusionHatch.INPUT.hintColor)
                .addRequirements(requirements(FusionHatch.OUTPUT), "16 cardinal positions in the middle layer",
                        FusionHatch.OUTPUT.hintColor)
                .addStructureInfo("Required hatches must be " + GT_Values.VN[tier()] + " or better.")
                .addStructureInfo("Buses and special IMPACT hatches do not replace the required standard hatches; no maintenance is needed.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Places reactor-tier energy injectors and fluid injectors/extractors at their dedicated ring positions.")
                .toolTipFinisher();
    }

    protected MultiblockTooltipBuilder compactFusionTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Fusion Reactor")
                .addInfo("Fuses fluids into new materials.")
                .addInfo("Energy input: %s EU/t per Energy Hatch.", getEnergyInputPerHatch())
                .addInfo("Charge the energy buffer to the recipe's startup requirement.");
        if (tierOverclock() > 1) {
            tooltip.addInfo("Lower startup energy allows more overclocks, if power allows.");
        }
        return tooltip
                .beginStructureBlock(15, 3, 15, false)
                .addController("inner edge of the middle layer, 5 blocks from the center, facing inward")
                .addStructureSection("Components")
                .addOtherStructurePart(getCasingType(), "79+; fill unused ring positions")
                .addOtherStructurePart(getFusionCoilType(), "exactly 32; middle layer of the ring")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Energy Hatches", "1-16; middle-layer diagonals")
                .addHatchInfo("Input Hatches", "2-16; top and bottom of the ring")
                .addHatchInfo("Output Hatches", "1-16; straight sides of the middle layer")
                .addStructureInfo("Required hatches: standard %s or higher; no maintenance.", GT_Values.VN[tier()])
                .addStructureHint("Middle diagonals: casings or energy hatches", FusionHatch.ENERGY.hintColor)
                .addStructureHint("Top and bottom: casings or input hatches", FusionHatch.INPUT.hintColor)
                .addStructureHint("Middle sides: casings or output hatches", FusionHatch.OUTPUT.hintColor)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: required hatches in free positions; existing casings are kept.");
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) return new ITexture[]{TextureFactory.of(MACHINE_CASING_FUSION_GLASS), getTextureOverlay()};
        if (aActive) return new ITexture[]{Casings.FUSION_GLASS_YELLOW.getTexture()};
        return new ITexture[]{TextureFactory.of(MACHINE_CASING_FUSION_GLASS)};
    }

    /**
     * @return The list of textures overlay
     */
    public abstract ITexture getTextureOverlay();

    public abstract IIconContainer getIconOverlay();

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    public int overclock(int mStartEnergy) {
        if (tierOverclock() == 1) {
            return 1;
        }
        if (tierOverclock() == 2) {
            return mStartEnergy < 160_000_000 ? 2 : 1;
        }
        if (this.tierOverclock() == 4) {
            return (mStartEnergy < 160_000_000 ? 4 : (mStartEnergy < 320_000_000 ? 2 : 1));
        }
        if (this.tierOverclock() == 8) {
            // MK IV and V use perfect overclocks: 4x EU/t and 1/4 duration per step.
            return (mStartEnergy < 160_000_000) ? 64 : ((mStartEnergy < 320_000_000) ? 16 : (mStartEnergy < 640_000_000) ? 4 : 1);
        }
        return (mStartEnergy < 160_000_000) ? 256 : ((mStartEnergy < 320_000_000) ? 64 : (mStartEnergy < 640_000_000) ? 16 : (mStartEnergy < 1_280_000_000) ? 4 : 1);
    }

    protected int overclock(int startEnergy, int duration, int eut) {
        int energyHatches = 0;
        for (GT_MetaTileEntity_Hatch_Energy hatch : mEnergyHatches) {
            if (isValidMetaTileEntity(hatch)) energyHatches++;
        }
        long energyInput = getEnergyInputPerHatch() * Math.min(16, energyHatches);
        if (eut <= 0 || eut > energyInput || duration < 1) return 0;

        int multiplier = overclock(startEnergy);
        int step = tierOverclock() >= 8 ? 4 : 2;
        // Leave charging headroom when overclocking; an unclocked recipe may use the full input.
        while (multiplier > 1 && ((long) eut * multiplier >= energyInput || multiplier > duration)) {
            multiplier /= step;
        }
        return multiplier;
    }

    public abstract int tierOverclock();

    protected long getEnergyInputPerHatch() {
        return 2048L * tierOverclock();
    }

    public boolean turnCasingActive(boolean status) {
        if (this.mEnergyHatches != null) {
            for (GT_MetaTileEntity_Hatch_Energy hatch : this.mEnergyHatches) {
                hatch.updateTexture(status ? Casings.FUSION_GLASS_YELLOW.getTextureId() : Casings.FUSION_GLASS.getTextureId());
            }
        }
        if (this.mOutputHatches != null) {
            for (GT_MetaTileEntity_Hatch_Output hatch : this.mOutputHatches) {
                hatch.updateTexture(status ? Casings.FUSION_GLASS_YELLOW.getTextureId() : Casings.FUSION_GLASS.getTextureId());
            }
        }
        if (this.mInputHatches != null) {
            for (GT_MetaTileEntity_Hatch_Input hatch : this.mInputHatches) {
                hatch.updateTexture(status ? Casings.FUSION_GLASS_YELLOW.getTextureId() : Casings.FUSION_GLASS.getTextureId());
            }
        }
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        if (base.isServerSide()) {
            if (mRunningOnLoad) {
                setupMachine(base, mInventory[1]);
                mRunningOnLoad = false;
            }
            if (mMachine && mStartUpCheck < 0) {
                chargeReactorBuffer(base);
            }
        }
        super.onPostTick(base, tick);
        if (base.isServerSide()) {
            mEUStore = (int) base.getStoredEU();
            turnCasingActive(mMaxProgresstime > 0 && !isWaitingForOutput());
        }
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (isWaitingForOutput()) {
            return true;
        }
        long required = -lEUt;
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (required >= 0 && base.getStoredEU() >= required
                && base.decreaseStoredEnergyUnits(required, false)) {
            return true;
        }
        stopProcessing(CheckRecipeResult.INSUFFICIENT_POWER);
        return false;
    }

    @Override
    public void stopMachine() {
        if (!isWaitingForOutput()) {
            mLastRecipe = null;
            mOutputFluids = null;
        }
        super.stopMachine();
        turnCasingActive(false);
    }

    private void chargeReactorBuffer(IGregTechTileEntity base) {
        long input = getEnergyInputPerHatch();
        for (GT_MetaTileEntity_Hatch_Energy hatch : mEnergyHatches) {
            if (isValidMetaTileEntity(hatch) && base.getStoredEU() <= maxEUStore() - input
                    && hatch.getBaseMetaTileEntity().decreaseStoredEnergyUnits(input, false)) {
                base.increaseStoredEnergyUnits(input, true);
            }
        }
    }

    @Override
    public boolean drainEnergyInput(long aEU) {
        return false;
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

    @Override
    public String[] getInfoData() {
        String tier = "";
        switch (tier()) {
            case 6:
                tier = EnumChatFormatting.RED + "I" + EnumChatFormatting.RESET;
                break;
            case 7:
                tier = EnumChatFormatting.YELLOW + "II" + EnumChatFormatting.RESET;
                break;
            case 8:
                tier = EnumChatFormatting.GRAY + "III" + EnumChatFormatting.RESET;
                break;
            case 9:
                tier = "IV";
                break;
            case 10:
                tier = "V";
                break;
            default:
                break;
        }
        float plasmaOut = 0;
        int powerRequired = 0;
        if (this.mLastRecipe != null) {
            powerRequired = this.mLastRecipe.mEUt;
            if (this.mLastRecipe.getFluidOutput(0) != null) {
                plasmaOut = (float) this.mLastRecipe.getFluidOutput(0).amount / (float) this.mLastRecipe.mDuration;
            }
        }

        return new String[]{
                EnumChatFormatting.BLUE + "Fusion Reactor MK " + EnumChatFormatting.RESET + tier,
                StatCollector.translateToLocal("GT5U.fusion.req") + ": " + EnumChatFormatting.RED + (-lEUt) + EnumChatFormatting.RESET + " EU/t",
                StatCollector.translateToLocal("GT5U.multiblock.energy") + ": " + EnumChatFormatting.GREEN + mEUStore + EnumChatFormatting.RESET + " EU / " + EnumChatFormatting.YELLOW + maxEUStore() + EnumChatFormatting.RESET + " EU",
                StatCollector.translateToLocal("GT5U.fusion.plasma") + ": " + EnumChatFormatting.YELLOW + plasmaOut + EnumChatFormatting.RESET + " L/t"};
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }
}
