package gregtech.common.tileentities.machines.multi;

import gregtech.GT_Mod;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.HatchElement;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.*;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Locale;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public abstract class GT_MetaTileEntity_LargeBoiler
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_LargeBoiler>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor FIREBOX_HINT = HintColor.BLUE;
    private static final HintColor OUTPUT_HINT = HintColor.ORANGE;
    private IStructureDefinition<GT_MetaTileEntity_LargeBoiler> structureDefinition;
    private StructureRequirements fireboxes;
    private StructureRequirements shell;
    private int casingCount;
    private int fireboxCount;

    private boolean firstRun = true;
    private int mSuperEfficencyIncrease = 0;
    private int integratedCircuitConfig = 0; //Steam output is reduced by 1000L per config
    private int excessFuel = 0; //Eliminate rounding errors for fuels that burn half items
    private int excessProjectedEU = 0; //Eliminate rounding errors from throttling the boiler

    public GT_MetaTileEntity_LargeBoiler(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeBoiler(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Large Boiler")
                .addInfo("Produces " + (getEUt() * 40) * (runtimeBoost(20) / 20f) + "L of Steam with 1 Coal at " + getEUt() * 40 + "L/s")
                .addInfo("A programmed circuit in the main block throttles the boiler (-1000L/s per config)")
                .addInfo(String.format(Locale.ROOT, "Diesel fuels have 1/4 efficiency - Takes %.2f seconds to heat up", 500.0 / getEfficiencyIncrease()))
                .addPollutionAmount(20 * getPollutionPerTick(null))
                .beginStructureBlock(3, 5, 3, false)
                .addController("Front center of the bottom firebox layer")
                .addRequirements(getFireboxRequirements(), "Bottom layer, replacing fireboxes", FIREBOX_HINT)
                .addRequirements(getShellRequirements(), "Upper four layers, replacing casings", OUTPUT_HINT)
                .addOtherStructurePart(getPipeType(), "Vertical column in the center, above the bottom layer", 3)
                .addStructureInfo("Two inputs are required: a water hatch and a fuel hatch or bus.")
                .addStructureInfo("Compatible IMPACT fluid and combined input hatches are accepted.")
                .addStructureInfo("The top center is a casing or output hatch, not a pipe or air.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    public abstract ICasing getCasingType();

    public abstract ICasing getPipeType();

    public abstract ICasing getFireboxType();

    private StructureRequirements getFireboxRequirements() {
        if (fireboxes == null) {
            fireboxes = StructureRequirements.builder(getFireboxType(), 3)
                    .atLeast(HatchElement.Maintenance)
                    .atLeast(BoilerHatch.WATER)
                    .atLeast(BoilerHatch.INPUT, 2)
                    .atLeast(BoilerHatch.MUFFLER)
                    .build();
        }
        return fireboxes;
    }

    private StructureRequirements getShellRequirements() {
        if (shell == null) {
            shell = StructureRequirements.builder(getCasingType(), 24)
                    .atLeast(BoilerHatch.STEAM)
                    .build();
        }
        return shell;
    }

    private enum BoilerHatch implements IHatchElement {
        WATER,
        INPUT,
        STEAM,
        MUFFLER;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
                case WATER:
                    return tile instanceof GT_MetaTileEntity_Hatch_Input
                            || tile instanceof GTMTE_Multi_Hatch_Input
                            || tile instanceof GTMTE_BusHatch_Input;
                case INPUT:
                    return HatchElement.Input.matches(tile);
                case STEAM:
                    // Preserve optional output buses, but only fluid hatches count as steam outputs.
                    return HatchElement.Output.matches(tile);
                case MUFFLER:
                    return tile instanceof GT_MetaTileEntity_Hatch_Muffler;
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            if (this == STEAM) {
                return tile instanceof GT_MetaTileEntity_Hatch_Output;
            }
            return matches(tile);
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
                case WATER:
                    return machine.mInputHatches.size() + machine.mQuadrInputHatches.size()
                            + machine.mInputBusHatches.size();
                case INPUT:
                    return HatchElement.Input.count(machine);
                case STEAM:
                    return machine.mOutputHatches.size();
                case MUFFLER:
                    return machine.mMufflerHatches.size();
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            switch (this) {
                case WATER:
                case INPUT:
                    return machine.addInputToMachineList(tile, texture);
                case STEAM:
                    return machine.addOutputToMachineList(tile, texture);
                case MUFFLER:
                    return machine.addMufflerToMachineList(tile, texture);
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public String getDescription() {
            return StatCollector.translateToLocal("gt.tooltip.hatch.boiler_" + name().toLowerCase(Locale.ROOT));
        }
    }

    public abstract int getEUt();

    public abstract int getEfficiencyIncrease();
    
    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) return new ITexture[]{
                    getCasingType().getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_LARGE_BOILER_ACTIVE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_LARGE_BOILER_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    getCasingType().getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_LARGE_BOILER),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_LARGE_BOILER_GLOW).glow().build()};
        }
        return new ITexture[]{getCasingType().getTexture()};
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "LargeBoiler.png");
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    public boolean checkRecipe(ItemStack aStack) {
        //Do we have an integrated circuit with a valid configuration?
        if (mInventory[1] != null && mInventory[1].getUnlocalizedName().startsWith("gt.integrated_circuit")) {
            int circuit_config = mInventory[1].getItemDamage();
            if (circuit_config >= 1 && circuit_config <= 25) {
                // If so, overwrite the current config
                this.integratedCircuitConfig = circuit_config;
            }
        } else {
            //If not, set the config to zero
            this.integratedCircuitConfig = 0;
        }

        this.mSuperEfficencyIncrease = 0;
        for (GT_Recipe tRecipe : GT_Recipe.GT_Recipe_Map.sDieselFuels.mRecipeList) {
            FluidStack tFluid = GT_Utility.getFluidForFilledItem(tRecipe.getRepresentativeInput(0), true);
            if (tFluid != null && tRecipe.mSpecialValue > 1) {
                tFluid.amount = 1000;
                if (depleteInput(tFluid)) {
                    this.mMaxProgresstime = adjustBurnTimeForConfig(runtimeBoost(tRecipe.mSpecialValue / 2));
                    this.mEUt = adjustEUtForConfig(getEUt());
                    this.mEfficiencyIncrease = this.mMaxProgresstime * getEfficiencyIncrease() * 4;
                    return true;
                }
            }
        }
        for (GT_Recipe tRecipe : GT_Recipe.GT_Recipe_Map.sDenseLiquidFuels.mRecipeList) {
            FluidStack tFluid = GT_Utility.getFluidForFilledItem(tRecipe.getRepresentativeInput(0), true);
            if (tFluid != null) {
                tFluid.amount = 1000;
                if (depleteInput(tFluid)) {
                    this.mMaxProgresstime = adjustBurnTimeForConfig(Math.max(1, runtimeBoost(tRecipe.mSpecialValue * 2)));
                    this.mEUt = adjustEUtForConfig(getEUt());
                    this.mEfficiencyIncrease = this.mMaxProgresstime * getEfficiencyIncrease();
                    return true;
                }
            }
        }
        ArrayList<ItemStack> tInputList = getStoredInputs();
        if (!tInputList.isEmpty()) {
            for (ItemStack tInput : tInputList) {
                if (tInput != GT_OreDictUnificator.get(OrePrefixes.bucket, Materials.Lava, 1)){
                    if (GT_Utility.getFluidForFilledItem(tInput, true) == null && (this.mMaxProgresstime = GT_ModHandler.getFuelValue(tInput) / 80) > 0) {
                        this.excessFuel += GT_ModHandler.getFuelValue(tInput) % 80;
                        this.mMaxProgresstime += this.excessFuel / 80;
                        this.excessFuel %= 80;
                        this.mMaxProgresstime = adjustBurnTimeForConfig(runtimeBoost(this.mMaxProgresstime));
                        this.mEUt = adjustEUtForConfig(getEUt());
                        this.mEfficiencyIncrease = this.mMaxProgresstime * getEfficiencyIncrease();
                        this.mOutputItems = new ItemStack[]{GT_Utility.getContainerItem(tInput, true)};
                        tInput.stackSize -= 1;
                        updateSlots();
                        if (this.mEfficiencyIncrease > 5000) {
                            this.mEfficiencyIncrease = 0;
                            this.mSuperEfficencyIncrease = 20;
                        }
                        return true;
                    }
                }
            }
        }
        this.mMaxProgresstime = 0;
        this.mEUt = 0;
        return false;
    }

    abstract int runtimeBoost(int mTime);

    public boolean onRunningTick(ItemStack aStack) {
        if (this.mEUt > 0) {
            if (this.mSuperEfficencyIncrease > 0)
                mEfficiency = Math.max(0, Math.min(mEfficiency + mSuperEfficencyIncrease, getMaxEfficiency(mInventory[1]) - ((getIdealStatus() - getRepairStatus()) * 1000)));
            int tGeneratedEU = (int) (this.mEUt * 2L * this.mEfficiency / 10000L);
            if (tGeneratedEU > 0) {
                long amount = (tGeneratedEU + 160) / 160;
                if (depleteInput(Materials.Water.getFluid(amount)) || depleteInput(GT_ModHandler.getDistilledWater(amount))) {
                    addOutput(GT_ModHandler.getSteam(tGeneratedEU));
                } else {
                    GT_Log.exp.println("Boiler "+this.mName+" had no Water!");
                    explodeMultiblock();
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (mProgresstime > 0 && firstRun) {
            firstRun = false;
            GT_Mod.achievements.issueAchievement(aBaseMetaTileEntity.getWorld().getPlayerEntityByName(aBaseMetaTileEntity.getOwnerName()), "extremepressure");
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        fireboxCount = 0;
        return checkPiece(STRUCTURE_PIECE, 1, 4, 0)
                && getFireboxRequirements().isSatisfied(this, fireboxCount)
                && getShellRequirements().isSatisfied(this, casingCount);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 1, 4, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 1, 4, 0, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_LargeBoiler> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_LargeBoiler>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CCC", "CCC"},
                            {"CCC", "CPC", "CCC"},
                            {"CCC", "CPC", "CCC"},
                            {"CCC", "CPC", "CCC"},
                            {"F~F", "FFF", "FFF"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_LargeBoiler.class)
                            .buildAndChain(getShellRequirements(), machine -> ++machine.casingCount)
                            .withHint(OUTPUT_HINT))
                    .addElement('F', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_LargeBoiler.class)
                            .buildAndChain(getFireboxRequirements(), machine -> ++machine.fireboxCount)
                            .withHint(FIREBOX_HINT))
                    .addElement('P', getPipeType().asElement())
                    .build();
        }
        return structureDefinition;
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    public int getPollutionPerTick(ItemStack aStack) {
        int adjustedEUOutput = Math.max(25, getEUt() - 25 * integratedCircuitConfig);
        return Math.max(1, 12 * adjustedEUOutput / getEUt());
    }

    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    private int adjustEUtForConfig(int rawEUt) {
        int adjustedSteamOutput = rawEUt - 25 * integratedCircuitConfig;
        return Math.max(adjustedSteamOutput, 25);
    }

    private int adjustBurnTimeForConfig(int rawBurnTime) {
        if (mEfficiency < getMaxEfficiency(mInventory[1]) - ((getIdealStatus() - getRepairStatus()) * 1000)) {
            return rawBurnTime;
        }
        int adjustedEUt = Math.max(25, getEUt() - 25 * integratedCircuitConfig);
        int adjustedBurnTime = rawBurnTime * getEUt() / adjustedEUt;
        this.excessProjectedEU += getEUt() * rawBurnTime - adjustedEUt * adjustedBurnTime;
        adjustedBurnTime += this.excessProjectedEU / adjustedEUt;
        this.excessProjectedEU %= adjustedEUt;
        return adjustedBurnTime;
    }
}
