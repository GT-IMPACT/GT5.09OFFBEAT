package gregtech.common.tileentities.machines.multi;

import gregtech.GT_Mod;
import gregtech.api.enums.Materials;
import gregtech.api.enums.HatchElement;
import gregtech.api.logic.BoilerProcessingLogic;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.*;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import java.util.Locale;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public abstract class GT_MetaTileEntity_LargeBoiler
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_LargeBoiler>
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
        int steamPerSecond = getEUt() * 40;
        long steamPerCoal = getEUt() * 2L * runtimeBoost(20);
        int minimumSteamPerSecond = Math.max(25, getEUt() - 25 * GT_IntegratedCircuit_Item.MAX_CONFIG) * 40;

        return new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Large Boiler")
                .addInfo("Burns solid or liquid fuel to produce steam from water or distilled water.")
                .addInfo("At full heat without a circuit: %s L/s; %s L of steam per Coal.", steamPerSecond, steamPerCoal)
                .addInfo("Steam output increases as the boiler heats up.")
                .addInfo("Circuit: -1000 L/s per setting; minimum %s L/s.", minimumSteamPerSecond)
                .addInfo("Diesel fuels have 1/4 efficiency.")
                .addInfo("Running out of water causes an explosion.")
                .addInfo("Pollution: %s per second before muffler reduction.", 20 * getPollutionPerTick(null))
                .beginStructureBlock(3, 5, 3, false)
                .addController("front center of the bottom layer")
                .addStructureSection("Components")
                .addOtherStructurePart(getCasingType(), "24+; fill unused positions in the upper 4 layers")
                .addOtherStructurePart(getFireboxType(), "3+; fill unused positions in the bottom layer")
                .addOtherStructurePart(getPipeType(), "exactly 3; center column above the bottom layer, below the roof")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Maintenance Hatch", "exactly 1; bottom layer")
                .addHatchInfo("Input Hatches or Buses", "2+ total, including 1+ hatch for water; bottom layer")
                .addHatchInfo("Output Hatches", "1+; upper 4 layers (steam)")
                .addHatchInfo("Muffler", "exactly 1; bottom layer, keep exhaust clear")
                .addStructureHint("Bottom: fireboxes or hatches/buses", FIREBOX_HINT)
                .addStructureHint("Upper layers: casings or output hatches", OUTPUT_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: required hatches in free positions; existing casings are kept.")
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
                    .atMost(HatchElement.Maintenance, 1)
                    .atMost(BoilerHatch.MUFFLER, 1)
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
                    return machine.mInputHatches.size()
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

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new BoilerProcessingLogic(() -> new BoilerProcessingLogic.State(getEUt(), getEfficiencyIncrease(),
                mEfficiency, maximumEfficiency(), circuitConfiguration(), excessFuel, excessProjectedEU),
                this::runtimeBoost);
    }

    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS;
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMapForNEI() {
        return GT_Recipe.GT_Recipe_Map.sLargeBoilerFakeFuels;
    }

    @Override
    public boolean allowSelectCircuit() {
        return true;
    }

    private int maximumEfficiency() {
        return getMaxEfficiency(mInventory[1]) - (getIdealStatus() - getRepairStatus()) * 1000;
    }

    private int circuitConfiguration() {
        ItemStack circuit = getConfigurationCircuit();
        if (circuit == null || !circuit.getUnlocalizedName().startsWith("gt.integrated_circuit")) {
            return 0;
        }
        int configuration = circuit.getItemDamage();
        return configuration >= 1 && configuration <= GT_IntegratedCircuit_Item.MAX_CONFIG ? configuration : 0;
    }

    @Override
    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        int previousEfficiency = mEfficiency;
        super.startCycle(result, efficiency);
        BoilerProcessingLogic.Burn burn = ((BoilerProcessingLogic) getProcessingLogic()).getBurn();
        // mEUt is the legacy thermal rate; lEUt stays zero because boilers produce no electricity.
        mEUt = burn.heat;
        mEfficiency = previousEfficiency;
        mEfficiencyIncrease = burn.efficiencyIncrease;
        mSuperEfficencyIncrease = burn.tickEfficiencyIncrease;
        integratedCircuitConfig = burn.circuit;
        excessFuel = burn.fuelRemainder;
        excessProjectedEU = burn.heatRemainder;
        markDirty();
    }

    abstract long runtimeBoost(long time);

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (mEUt <= 0) {
            return true;
        }
        if (mSuperEfficencyIncrease > 0) {
            mEfficiency = Math.max(0, Math.min(mEfficiency + mSuperEfficencyIncrease, maximumEfficiency()));
        }
        int steam = Math.toIntExact(mEUt * 2L * mEfficiency / 10000);
        if (steam > 0) {
            // Preserve the existing water rounding. A full output never pauses combustion.
            long water = (steam + 160L) / 160;
            if (depleteInput(Materials.Water.getFluid(water))
                    || depleteInput(GT_ModHandler.getDistilledWater(water))) {
                addOutput(GT_ModHandler.getSteam(steam));
            } else {
                GT_Log.exp.println("Boiler " + mName + " had no Water!");
                explodeMultiblock();
            }
        }
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setLong("mEUt", mEUt);
        NBTTagCompound boiler = new NBTTagCompound();
        boiler.setInteger("tickWarmup", mSuperEfficencyIncrease);
        boiler.setInteger("circuit", integratedCircuitConfig);
        boiler.setInteger("fuelRemainder", excessFuel);
        boiler.setInteger("heatRemainder", excessProjectedEU);
        boiler.setBoolean("firstRun", firstRun);
        tag.setTag("boiler", boiler);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        lEUt = 0;
        NBTTagCompound boiler = tag.getCompoundTag("boiler");
        mSuperEfficencyIncrease = boiler.getInteger("tickWarmup");
        integratedCircuitConfig = boiler.getInteger("circuit");
        excessFuel = boiler.getInteger("fuelRemainder");
        excessProjectedEU = boiler.getInteger("heatRemainder");
        firstRun = !tag.hasKey("boiler") || boiler.getBoolean("firstRun");
        if (!tag.hasKey("boiler") && mMaxProgresstime > 0) {
            // Old saves contain the thermal rate and duration, but no boiler-specific state.
            integratedCircuitConfig = Math.max(0, Math.min(25, (getEUt() - mEUt) / 25));
            mSuperEfficencyIncrease = mEfficiencyIncrease == 0 ? 20 : 0;
        }
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

}
