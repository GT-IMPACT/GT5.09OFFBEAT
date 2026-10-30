package gregtech.common.tileentities.machines.multi;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.api.structure.StructureUtility.transpose;

import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import gregtech.GT_Mod;
import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_Container_MultiMachine;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.*;
import gregtech.common.gui.GT_GUIContainer_FusionReactor;
import space.impact.api.alignment.constructable.ISurvivalConstructable;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.structure.StructureDefinition;

public abstract class GT_MetaTileEntity_FusionComputer
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_FusionComputer>
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

    public GT_MetaTileEntity_FusionComputer(int aID, String aName, String aNameRegional, int tier) {
        super(aID, aName, aNameRegional);
        enabledMaintenance = false;
    }

    public GT_MetaTileEntity_FusionComputer(String aName) {
        super(aName);
        enabledMaintenance = false;
    }

    public abstract int tier();

    @Override
    public abstract long maxEUStore();

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_Container_MultiMachine(aPlayerInventory, aBaseMetaTileEntity);
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_FusionReactor(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "FusionComputer.png", GT_Recipe.GT_Recipe_Map.sFusionRecipes.mNEIName);
    }

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
        OUTPUT(HatchElement.Output, GT_MetaTileEntity_Hatch_Output.class, "output_hatch", 1, HintColor.ORANGE);

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
                            return machine.mInputHatches.size();
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
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
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

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<FluidStack> tFluidList = getStoredFluids();
        int tFluidList_sS = tFluidList.size();
        for (int i = 0; i < tFluidList_sS - 1; i++) {
            for (int j = i + 1; j < tFluidList_sS; j++) {
                if (GT_Utility.areFluidsEqual(tFluidList.get(i), tFluidList.get(j))) {
                    if ((tFluidList.get(i)).amount >= (tFluidList.get(j)).amount) {
                        tFluidList.remove(j--);
                        tFluidList_sS = tFluidList.size();
                    } else {
                        tFluidList.remove(i--);
                        tFluidList_sS = tFluidList.size();
                        break;
                    }
                }
            }
        }

        if (tFluidList.size() > 1) {
            FluidStack[] tFluids = tFluidList.toArray(new FluidStack[tFluidList.size()]);
            GT_Recipe tRecipe = GT_Recipe.GT_Recipe_Map.sFusionRecipes.findRecipe(this.getBaseMetaTileEntity(), this.mLastRecipe, false, GT_Values.V[15], tFluids);

            if (tRecipe == null || maxEUStore() < tRecipe.mSpecialValue) {
                turnCasingActive(false);
                this.mLastRecipe = null;
                return false;
            }
            int overclock = overclock(tRecipe.mSpecialValue, tRecipe.mDuration, tRecipe.mEUt);
            if (overclock == 0) {
                turnCasingActive(false);
                this.mLastRecipe = null;
                return false;
            }
            if (mRunningOnLoad || tRecipe.isRecipeInputEqual(true, tFluids)) {
                updateSlots();
                this.mLastRecipe = tRecipe;
                this.mEUt = this.mLastRecipe.mEUt * overclock;
                this.mMaxProgresstime = this.mLastRecipe.mDuration / overclock;
                this.mEfficiencyIncrease = 10000;
                this.mOutputFluids = this.mLastRecipe.mFluidOutputs;
                turnCasingActive(true);
                mRunningOnLoad = false;
                mWrench = true;
                mScrewdriver = true;
                mSoftHammer = true;
                mHardHammer = true;
                mSolderingTool = true;
                mCrowbar = true;
                return true;
            }
        }
        return false;
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
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
            if (mEfficiency < 0)
                mEfficiency = 0;
            if (mRunningOnLoad) {
                setupMachine(aBaseMetaTileEntity, mInventory[1]);
                if (mMachine) {
                    this.mEUStore = (int) aBaseMetaTileEntity.getStoredEU();
                    checkRecipe(mInventory[1]);
                }
            }
            if (--mUpdate == 0 || --mStartUpCheck == 0) {
                setupMachine(aBaseMetaTileEntity, mInventory[1]);
            }
            if (mStartUpCheck < 0) {
                if (mMachine) {
                    if (this.mEnergyHatches != null) {
                        long energyInput = getEnergyInputPerHatch();
                        for (GT_MetaTileEntity_Hatch_Energy tHatch : mEnergyHatches)
                            if (isValidMetaTileEntity(tHatch)) {
                                if (aBaseMetaTileEntity.getStoredEU() + energyInput <= maxEUStore()
                                        && tHatch.getBaseMetaTileEntity().decreaseStoredEnergyUnits(energyInput, false)) {
                                    aBaseMetaTileEntity.increaseStoredEnergyUnits(energyInput, true);
                                }
                            }
                    }
                    if (getRepairStatus() > 0) {
                        if (mMaxProgresstime > 0) {
                            if (aBaseMetaTileEntity.getStoredEU() < mEUt
                                    || !aBaseMetaTileEntity.decreaseStoredEnergyUnits(mEUt, false)) {
                                stopMachine();
                            }
                            if (mMaxProgresstime > 0 && ++mProgresstime >= mMaxProgresstime) {
                                if (mOutputItems != null)
                                    for (ItemStack tStack : mOutputItems) if (tStack != null) addOutput(tStack);
                                if (mOutputFluids != null)
                                    for (FluidStack tStack : mOutputFluids) if (tStack != null) addOutput(tStack);
                                mEfficiency = Math.max(0, Math.min(mEfficiency + mEfficiencyIncrease, getMaxEfficiency(mInventory[1]) - ((getIdealStatus() - getRepairStatus()) * 1000)));
                                mOutputItems = null;
                                mProgresstime = 0;
                                mMaxProgresstime = 0;
                                mEfficiencyIncrease = 0;
                                if (mOutputFluids != null && mOutputFluids.length > 0) {
                                    try {
                                        GT_Mod.achievements.issueAchivementHatchFluid(aBaseMetaTileEntity.getWorld().getPlayerEntityByName(aBaseMetaTileEntity.getOwnerName()), mOutputFluids[0]);
                                    } catch (Exception e) {
                                    }
                                }
                                this.mEUStore = (int) aBaseMetaTileEntity.getStoredEU();
                                if (aBaseMetaTileEntity.isAllowedToWork())
                                    checkRecipe(mInventory[1]);
                            }
                        } else {
                            if (aTick % 100 == 0 || aBaseMetaTileEntity.hasWorkJustBeenEnabled() || aBaseMetaTileEntity.hasInventoryBeenModified()) {
                                turnCasingActive(mMaxProgresstime > 0);
                                if (aBaseMetaTileEntity.isAllowedToWork()) {
                                    this.mEUStore = (int) aBaseMetaTileEntity.getStoredEU();
                                    if (checkRecipe(mInventory[1])) {
                                        if (this.mEUStore < this.mLastRecipe.mSpecialValue - this.mEUt) {
                                            mMaxProgresstime = 0;
                                            turnCasingActive(false);
                                        }
                                        aBaseMetaTileEntity.decreaseStoredEnergyUnits(this.mLastRecipe.mSpecialValue - this.mEUt, true);
                                    }
                                }
                                if (mMaxProgresstime <= 0)
                                    mEfficiency = Math.max(0, mEfficiency - 1000);
                            }
                        }
                    } else {
                        this.mLastRecipe = null;
                        stopMachine();
                    }
                } else {
                    turnCasingActive(false);
                    this.mLastRecipe = null;
                    stopMachine();
                }
            }
            aBaseMetaTileEntity.setErrorDisplayID((aBaseMetaTileEntity.getErrorDisplayID() & ~127) | (mWrench ? 0 : 1) | (mScrewdriver ? 0 : 2) | (mSoftHammer ? 0 : 4) | (mHardHammer ? 0 : 8)
                    | (mSolderingTool ? 0 : 16) | (mCrowbar ? 0 : 32) | (mMachine ? 0 : 64));
            this.mEUStore = (int) aBaseMetaTileEntity.getStoredEU();
            aBaseMetaTileEntity.setActive(mMaxProgresstime > 0);
        }
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (mEUt < 0) {
            if (!drainEnergyInput(((long) -mEUt * 10000) / Math.max(1000, mEfficiency))) {
                this.mLastRecipe = null;
                stopMachine();
                return false;
            }
        }
        if (this.mEUStore <= 0) {
            this.mLastRecipe = null;
            stopMachine();
            return false;
        }
        return true;
    }

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
                StatCollector.translateToLocal("GT5U.fusion.req") + ": " + EnumChatFormatting.RED + mEUt + EnumChatFormatting.RESET + " EU/t",
                StatCollector.translateToLocal("GT5U.multiblock.energy") + ": " + EnumChatFormatting.GREEN + mEUStore + EnumChatFormatting.RESET + " EU / " + EnumChatFormatting.YELLOW + maxEUStore() + EnumChatFormatting.RESET + " EU",
                StatCollector.translateToLocal("GT5U.fusion.plasma") + ": " + EnumChatFormatting.YELLOW + plasmaOut + EnumChatFormatting.RESET + " L/t"};
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }
}
