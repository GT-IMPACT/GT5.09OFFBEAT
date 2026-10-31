package gregtech.common.tileentities.machines.multi;

import net.minecraft.block.Block;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.api.alignment.constructable.ISurvivalConstructable;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.structure.StructureDefinition;
import net.minecraftforge.common.util.ForgeDirection;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.api.structure.StructureUtility.onElementPass;
import static space.impact.api.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_DieselEngine
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_DieselEngine>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SIDE_HINT = HintColor.BLUE;
    private static final HintColor MUFFLER_HINT = HintColor.ORANGE;
    private static final HintColor DYNAMO_HINT = HintColor.YELLOW;
    private IStructureDefinition<GT_MetaTileEntity_DieselEngine> structureDefinition;
    private StructureRequirements shell;
    private StructureRequirements muffler;
    private StructureRequirements dynamo;
    private int casingCount;
    protected int fuelConsumption = 0;
    protected int fuelValue = 0;
    protected int fuelRemaining = 0;
    protected boolean boostEu = false;

    public GT_MetaTileEntity_DieselEngine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_DieselEngine(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        try {
            if (getClass().getMethod("getDescription").getDeclaringClass() != GT_MetaTileEntity_TooltipMultiBlockBase.class) {
                return null;
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return engineTooltip(new MultiblockTooltipBuilder(mName)
                .addMachineType("Combustion Engine Mk1")
                .addInfo("Consumes flammable fuel and lubricant; oxygen enables boosted output")
                .addInfo("Output: 2048 EU/t normally, 6144 EU/t when boosted")
                .addInfo("Oxygen: 2 L/t; lubricant: 1 L per 72 ticks (2 L when boosted)"));
    }

    protected MultiblockTooltipBuilder engineTooltip(MultiblockTooltipBuilder tooltip) {
        return tooltip.addPollutionAmount(20 * getPollutionPerTick(null))
                .beginStructureBlock(3, 3, 4, false)
                .addController("Front center")
                .addRequirements(getShellRequirements(), "Side centers beside either internal block, except the dedicated muffler", SIDE_HINT)
                .addRequirements(getMufflerRequirements(), "Top center beside the rear internal block", MUFFLER_HINT)
                .addRequirements(getDynamoRequirements(), "Back center", DYNAMO_HINT)
                .addOtherStructurePart(getGearboxType(), "Two central positions behind the controller", 2)
                .addOtherStructurePart(getIntakeType(), "Eight positions surrounding the controller", 8)
                .addAir("Eight blocks directly in front of the intake ring")
                .addStructureInfo("The seven marked side positions accept hatches or casings; corners and the rear frame require casings.")
                .addStructureInfo("Two fluid-capable input hatches and one maintenance hatch are required; item-only buses do not replace fluid inputs.")
                .addStructureInfo("Additional output hatches, buses and mufflers are accepted at the marked side positions.")
                .addStructureInfo("Compatible IMPACT multi-fluid and combined inputs and multi-amp dynamo hatches are accepted.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    public ICasing getCasingType() {
        return legacyCasing(getCasingBlock(), getCasingMeta());
    }

    public ICasing getIntakeType() {
        return legacyCasing(getIntakeBlock(), getIntakeMeta());
    }

    public ICasing getGearboxType() {
        return legacyCasing(getGearboxBlock(), getGearboxMeta());
    }

    private static ICasing legacyCasing(Block block, int metadata) {
        for (Casings casing : Casings.values()) {
            if (casing.getBlock() == block && casing.getMetadata() == metadata) {
                return casing;
            }
        }
        throw new IllegalArgumentException("Unregistered legacy engine casing: " + block + ":" + metadata);
    }

    // Retained for subclasses that still override the legacy block/metadata API.
    @Deprecated
    public Block getCasingBlock() {
        return Casings.STABLE_TITANIUM.getBlock();
    }

    @Deprecated
    public byte getCasingMeta() {
        return (byte) Casings.STABLE_TITANIUM.getMetadata();
    }

    @Deprecated
    public Block getIntakeBlock() {
        return Casings.ENGINE_INTAKE.getBlock();
    }

    @Deprecated
    public byte getIntakeMeta() {
        return (byte) Casings.ENGINE_INTAKE.getMetadata();
    }

    @Deprecated
    public Block getGearboxBlock() {
        return Casings.TITANIUM_GEARBOX.getBlock();
    }

    @Deprecated
    public byte getGearboxMeta() {
        return (byte) Casings.TITANIUM_GEARBOX.getMetadata();
    }

    @Deprecated
    public byte getCasingTextureIndex() {
        return (byte) Casings.STABLE_TITANIUM.getTextureId();
    }

    private StructureRequirements getShellRequirements() {
        if (shell == null) {
            shell = StructureRequirements.builder(getCasingType(), 16)
                    .atLeast(HatchElement.Maintenance).atLeast(EngineHatch.INPUT, 2).build();
        }
        return shell;
    }

    private StructureRequirements getMufflerRequirements() {
        if (muffler == null) {
            muffler = StructureRequirements.builder(getCasingType(), 0).atLeast(EngineHatch.MUFFLER).build();
        }
        return muffler;
    }

    private StructureRequirements getDynamoRequirements() {
        if (dynamo == null) {
            dynamo = StructureRequirements.builder(getCasingType(), 0).atLeast(EngineHatch.DYNAMO).build();
        }
        return dynamo;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) return new ITexture[]{
                    getCasingType().getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_DIESEL_ENGINE_ACTIVE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_DIESEL_ENGINE_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    getCasingType().getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_DIESEL_ENGINE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_DIESEL_ENGINE_GLOW).glow().build()};
        }
        return new ITexture[]{getCasingType().getTexture()};
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return getMaxEfficiency(aStack) > 0;
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "LargeDieselEngine.png");
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<FluidStack> tFluids = getStoredFluids();
        Collection<GT_Recipe> tRecipeList = GT_Recipe.GT_Recipe_Map.sDieselFuels.mRecipeList;

        if (tFluids.size() > 0 && tRecipeList != null) { //Does input hatch have a diesel fuel?
            for (FluidStack hatchFluid1 : tFluids) { //Loops through hatches
                for (GT_Recipe aFuel : tRecipeList) { //Loops through diesel fuel recipes
                    FluidStack tLiquid;
                    if ((tLiquid = GT_Utility.getFluidForFilledItem(aFuel.getRepresentativeInput(0), true)) != null) { //Create fluidstack from current recipe
                        if (hatchFluid1.isFluidEqual(tLiquid)) { //Has a diesel fluid
                            fuelConsumption = tLiquid.amount = boostEu ? (4096 / aFuel.mSpecialValue) : (2048 / aFuel.mSpecialValue); //Calc fuel consumption
                            fuelConsumption = tLiquid.amount = Math.max(tLiquid.amount, 1);
                            if (depleteInput(tLiquid)) { //Deplete that amount
                                boostEu = depleteInput(Materials.Oxygen.getGas(2L));

                                if (tFluids.contains(Materials.Lubricant.getFluid(1L))) { //Has lubricant?
                                    //Deplete Lubricant. 1000L should = 1 hour of runtime (if baseEU = 2048)
                                    if (mRuntime % 72 == 0 || mRuntime == 0)
                                        depleteInput(Materials.Lubricant.getFluid(boostEu ? 2 : 1));
                                } else return false;

                                fuelValue = aFuel.mSpecialValue;
                                fuelRemaining = hatchFluid1.amount; //Record available fuel
                                this.mEUt = mEfficiency < 2000 ? 0 : 2048; //Output 0 if startup is less than 20%
                                this.mProgresstime = 1;
                                this.mMaxProgresstime = 1;
                                this.mEfficiencyIncrease = 15;
                                if (this.mDynamoHatches.size() > 0) {
                                    for (GT_MetaTileEntity_Hatch_Dynamo hatch : mDynamoHatches) {
                                        if ((hatch.getBaseMetaTileEntity().getOutputVoltage() * hatch.mAmpers) < ((long) mEUt * mEfficiency / 10000L)) {
                                            explodeMultiblock();
                                        }
                                    }
                                }
                                if (this.mDynamoHatchesMulti.size() > 0) {
                                    for (GT_MetaTileEntity_Hatch_DynamoMulti hatch : mDynamoHatchesMulti) {
                                        if ((hatch.getBaseMetaTileEntity().getOutputVoltage() * hatch.Amp) < ((long) mEUt * mEfficiency / 10000L)) {
                                            explodeMultiblock();
                                        }
                                    }
                                }
                                return true;
                            }
                        }
                    }
                }
            }
        }
        this.mEUt = 0;
        this.mEfficiency = 0;
        return false;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        return checkPiece(STRUCTURE_PIECE, 1, 1, 1) && getShellRequirements().isSatisfied(this, casingCount)
                && getMufflerRequirements().isSatisfied(this, 0) && getDynamoRequirements().isSatisfied(this, 0);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 1, 1, 1);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 1, 1, 1, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_DieselEngine> getStructureDefinition() {
        if (structureDefinition == null) {
            // The first depth plane describes intake clearance; its center is deliberately unconstrained.
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_DieselEngine>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"---", "III", "CHC", "CMC", "CCC"},
                            {"- -", "I~I", "HGH", "HGH", "CDC"},
                            {"---", "III", "CHC", "CHC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', onElementPass(machine -> ++machine.casingCount, getCasingType().asElement()))
                    .addElement('I', getIntakeType().asElement())
                    .addElement('G', getGearboxType().asElement())
                    .addElement('H', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DieselEngine.class)
                            .allow(HatchElement.Output, EngineHatch.EXTRA_MUFFLER)
                            .buildAndChain(getShellRequirements(), machine -> ++machine.casingCount).withHint(SIDE_HINT))
                    .addElement('M', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DieselEngine.class)
                            .hint(MUFFLER_HINT).build(getMufflerRequirements()))
                    .addElement('D', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DieselEngine.class)
                            .hint(DYNAMO_HINT).build(getDynamoRequirements()))
                    .build();
        }
        return structureDefinition;
    }

    private enum EngineHatch implements IHatchElement {
        INPUT,
        MUFFLER,
        EXTRA_MUFFLER,
        DYNAMO;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
                case INPUT:
                    return HatchElement.Input.matches(tile);
                case MUFFLER:
                case EXTRA_MUFFLER:
                    return tile instanceof GT_MetaTileEntity_Hatch_Muffler;
                case DYNAMO:
                    return tile instanceof GT_MetaTileEntity_Hatch_Dynamo || tile instanceof GT_MetaTileEntity_Hatch_DynamoMulti;
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            return this == INPUT ? tile instanceof GT_MetaTileEntity_Hatch_Input
                    || tile instanceof GTMTE_BusHatch_Input : matches(tile);
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
                case INPUT:
                    return machine.mInputHatches.size() + machine.mQuadrInputHatches.size() + machine.mInputBusHatches.size();
                case MUFFLER:
                    IGregTechTileEntity base = machine.getBaseMetaTileEntity();
                    ForgeDirection back = ForgeDirection.getOrientation(base.getBackFacing());
                    for (GT_MetaTileEntity_Hatch_Muffler hatch : machine.mMufflerHatches) {
                        IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
                        if (tile.getXCoord() == base.getXCoord() + 2 * back.offsetX
                                && tile.getYCoord() == base.getYCoord() + 1
                                && tile.getZCoord() == base.getZCoord() + 2 * back.offsetZ) {
                            return 1;
                        }
                    }
                    return 0;
                case EXTRA_MUFFLER:
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
                case MUFFLER:
                case EXTRA_MUFFLER:
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
                case MUFFLER:
                case EXTRA_MUFFLER:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.muffler");
                case DYNAMO:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.dynamo");
                default:
                    throw new AssertionError(this);
            }
        }
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_DieselEngine(this.mName);
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 1;
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return boostEu ? 30000 : 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 24;
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

        long storedEnergy = 0;
        long maxEnergy = 0;
        for (GT_MetaTileEntity_Hatch_Dynamo tHatch : mDynamoHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }

        for (GT_MetaTileEntity_Hatch_DynamoMulti tHatch : mDynamoHatchesMulti) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }
        return new String[]{
                EnumChatFormatting.BLUE + "Diesel Engine" + EnumChatFormatting.RESET,
                StatCollector.translateToLocal("GT5U.multiblock.energy") + ": " +
                        EnumChatFormatting.GREEN + Long.toString(storedEnergy) + EnumChatFormatting.RESET + " EU / " +
                        EnumChatFormatting.YELLOW + Long.toString(maxEnergy) + EnumChatFormatting.RESET + " EU",
                StatCollector.translateToLocal("GT5U.engine.output") + ": " + EnumChatFormatting.GREEN + (mEUt * mEfficiency / 10000) + EnumChatFormatting.RESET + " EU/t",
                StatCollector.translateToLocal("GT5U.engine.consumption") + ": " + EnumChatFormatting.YELLOW + fuelConsumption + EnumChatFormatting.RESET + " L/t",
                StatCollector.translateToLocal("GT5U.engine.value") + ": " + EnumChatFormatting.YELLOW + fuelValue + EnumChatFormatting.RESET + " EU/L",
                StatCollector.translateToLocal("GT5U.turbine.fuel") + ": " + EnumChatFormatting.GOLD + fuelRemaining + EnumChatFormatting.RESET + " L",
                StatCollector.translateToLocal("GT5U.engine.efficiency") + ": " + EnumChatFormatting.YELLOW + (mEfficiency / 100F) + EnumChatFormatting.YELLOW + " %",
                StatCollector.translateToLocal("GT5U.multiblock.pollution") + ": " + EnumChatFormatting.GREEN + mPollutionReduction + EnumChatFormatting.RESET + " %"

        };
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }
}
