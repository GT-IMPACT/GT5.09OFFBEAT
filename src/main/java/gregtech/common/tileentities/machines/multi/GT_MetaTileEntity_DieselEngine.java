package gregtech.common.tileentities.machines.multi;

import net.minecraft.block.Block;
import gregtech.api.logic.CombustionProcessingLogic;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import net.minecraft.nbt.NBTTagCompound;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import net.minecraftforge.common.util.ForgeDirection;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;


import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.onElementPass;
import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_DieselEngine
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_DieselEngine>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SIDE_HINT = HintColor.GREEN;
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
                .addInfo("Consumes flammable fuel and lubricant; oxygen enables boosted output.")
                .addInfo("Output: 2048 EU/t normally, 6144 EU/t when boosted.")
                .addInfo("Oxygen: 2 L/t; lubricant: 1 L per 72 ticks (2 L when boosted)."));
    }

    protected MultiblockTooltipBuilder engineTooltip(MultiblockTooltipBuilder tooltip) {
        return tooltip.useCompactStructure()
                .addInfo("Pollution: %s per second before muffler reduction.", 20 * getPollutionPerTick(null))
                .beginStructureBlock(3, 3, 4, false)
                .addController("front center")
                .addStructureSection("Components")
                .addOtherStructurePart(getCasingType(), "16+; fill unused casing positions")
                .addOtherStructurePart(getGearboxType(), "exactly 2; directly behind the controller")
                .addOtherStructurePart(getIntakeType(), "exactly 8; around the controller")
                .addAir("8 blocks directly in front of the blocks around the controller")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Maintenance Hatch", "1; around the gearboxes")
                .addHatchInfo("Input Hatches", "2+; around the gearboxes")
                .addHatchInfo("Muffler", "exactly 1; above the rear gearbox, keep exhaust clear")
                .addHatchInfo("Dynamo Hatch", "exactly 1; center of the back wall")
                .addStructureHint("Sides: casings or maintenance/fluid input hatches", SIDE_HINT)
                .addStructureHint("Top rear side: muffler", MUFFLER_HINT)
                .addStructureHint("Back center: dynamo", DYNAMO_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: required hatches in free positions; existing casings are kept.")
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

    protected CombustionProcessingLogic.Settings engineSettings() {
        return new CombustionProcessingLogic.Settings(2048, 4096, 2, 1, 72, 15);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new CombustionProcessingLogic(engineSettings(),
                () -> new CombustionProcessingLogic.State(mRuntime, mEfficiency, boostEu));
    }

    @Override
    public int getSupportedProcessingModes() {
        return 0;
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMapForNEI() {
        return GT_Recipe.GT_Recipe_Map.sDieselFuels;
    }

    @Override
    public boolean checkRecipe(ItemStack stack) {
        boolean started = super.checkRecipe(stack);
        if (!started && mMaxProgresstime == 0
                && getProcessingResult() == CheckRecipeResult.NO_RECIPE) {
            // Running out of fuel resets the engine's warmup, as in the original implementation.
            mEfficiency = 0;
            mEUt = 0;
            lEUt = 0;
            markDirty();
        }
        return started;
    }

    @Override
    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        int previousEfficiency = mEfficiency;
        super.startCycle(result, efficiency);
        CombustionProcessingLogic logic = (CombustionProcessingLogic) getProcessingLogic();
        mEfficiency = previousEfficiency;
        mEfficiencyIncrease = logic.getWarmup();
        boostEu = logic.isBoosted();
        fuelConsumption = logic.getConsumption();
        fuelValue = logic.getFuelValue();
        fuelRemaining = logic.getRemaining();
        markDirty();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setBoolean("engineBoost", boostEu);
        tag.setInteger("engineFuelConsumption", fuelConsumption);
        tag.setInteger("engineFuelValue", fuelValue);
        tag.setInteger("engineFuelRemaining", fuelRemaining);
        tag.setInteger("engineRuntime", mRuntime);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        boostEu = tag.hasKey("engineBoost") ? tag.getBoolean("engineBoost") : mEfficiency > 10000;
        fuelConsumption = tag.getInteger("engineFuelConsumption");
        fuelValue = tag.getInteger("engineFuelValue");
        fuelRemaining = tag.getInteger("engineFuelRemaining");
        mRuntime = tag.getInteger("engineRuntime");
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
                            .hint(getCasingType(), MUFFLER_HINT).build(getMufflerRequirements()))
                    .addElement('D', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DieselEngine.class)
                            .hint(getCasingType(), DYNAMO_HINT).build(getDynamoRequirements()))
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
                    return machine.mInputHatches.size() + machine.mInputBusHatches.size();
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
