package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.GTMTE_Multi_Hatch_Input;
import gregtech.api.metatileentity.implementations.GTMTE_BusHatch_Input;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Log;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_HeatExchanger
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_HeatExchanger>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor HOT_HINT = HintColor.RED;
    private static final HintColor COLD_HINT = HintColor.BLUE;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.STABLE_TITANIUM, 20)
            .atLeast(HatchElement.Maintenance, ExchangerHatch.WATER, ExchangerHatch.STEAM).build();
    private static final StructureRequirements HOT_INPUT = StructureRequirements.builder(Casings.STABLE_TITANIUM, 0)
            .atLeast(ExchangerHatch.HOT).build();
    private static final StructureRequirements COLD_OUTPUT = StructureRequirements.builder(Casings.STABLE_TITANIUM, 0)
            .atLeast(ExchangerHatch.COLD).build();
    private static IStructureDefinition<GT_MetaTileEntity_HeatExchanger> structureDefinition;
    private int casingCount;
    public static float penalty_per_config = 0.015f;  // penalize 1.5% efficiency per circuitry level (1-25)

    private GT_MetaTileEntity_Hatch_Input mInputHotFluidHatch;
    private GT_MetaTileEntity_Hatch_Output mOutputColdFluidHatch;
    private boolean superheated = false;
    private int superheated_threshold=0;
    private float water;

    public GT_MetaTileEntity_HeatExchanger(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }
    public GT_MetaTileEntity_HeatExchanger(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Large Heat Exchanger")
                .addInfo("Converts heat from lava or hot coolant into steam using distilled water")
                .addInfo("Sufficient hot fluid produces superheated steam")
                .addInfo("A programmed circuit lowers the superheated-steam threshold at the cost of efficiency")
                .addInfo("Running without distilled water causes an explosion")
                .beginStructureBlock(3, 4, 3, false)
                .addController("Front center of the bottom layer")
                .addRequirements(SHELL, "Any outer casing position")
                .addRequirements(HOT_INPUT, "Bottom center", HOT_HINT)
                .addRequirements(COLD_OUTPUT, "Top center", COLD_HINT)
                .addOtherStructurePart(Casings.TITANIUM_PIPE, "Vertical column between the hot input and cold output", 2)
                .addStructureInfo("The hot input and cold output are separate from the distilled-water and steam hatches.")
                .addStructureInfo("The hot port accepts fluid input hatches, including IMPACT multi-fluid hatches; the cold port requires a fluid output hatch.")
                .addStructureInfo("Compatible IMPACT fluid and combined input hatches can supply distilled water.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        superheated = aNBT.getBoolean("superheated");
        super.loadNBTData(aNBT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setBoolean("superheated", superheated);
        super.saveNBTData(aNBT);
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive)
                return new ITexture[]{
                        Casings.STABLE_TITANIUM.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    Casings.STABLE_TITANIUM.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_HEAT_EXCHANGER),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_GLOW).glow().build()};
        }
        return new ITexture[]{Casings.STABLE_TITANIUM.getTexture()};
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "LargeHeatExchanger.png");
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    public boolean checkRecipe(ItemStack aStack) {
        if (mInputHotFluidHatch.getFluid() == null)
            return true;

        int fluidAmountToConsume = mInputHotFluidHatch.getFluidAmount(); // how much fluid is in hatch

        superheated_threshold = 4000;   // default: must have 4000L per second to generate superheated steam
        float efficiency = 1f;              // default: operate at 100% efficiency with no integrated circuitry
        int shs_reduction_per_config = 150; // reduce threshold 150L/s per circuitry level (1-25)
        float steam_output_multiplier = 20f; // default: multiply output by 4 * 10 (boosted x5)
        float penalty = 0.0f;               // penalty to apply to output based on circuitry level (1-25).
        boolean do_lava = false;

        // Do we have an integrated circuit with a valid configuration?
        if (mInventory[1] != null && mInventory[1].getUnlocalizedName().startsWith("gt.integrated_circuit")) {
            int circuit_config = mInventory[1].getItemDamage();
            if (circuit_config >= 1 && circuit_config <= 25) {
                // If so, apply the penalty and reduced threshold.
                penalty = (circuit_config - 1) * penalty_per_config;
                superheated_threshold -= (shs_reduction_per_config * (circuit_config - 1));
            }
        }
        efficiency -= penalty;

        // If we're working with lava, adjust the threshold and multipliers accordingly.
        if (GT_ModHandler.isLava(mInputHotFluidHatch.getFluid())) {
            steam_output_multiplier /= 5f; // lava is not boosted
            superheated_threshold /= 4f; // unchanged
            do_lava = true;
        } else if (mInputHotFluidHatch.getFluid().isFluidEqual(FluidRegistry.getFluidStack("ic2hotcoolant", 1))) {
            steam_output_multiplier /= 2f; // was boosted x2 on top of x5 -> total x10 -> nerf with this code back to 5x
            superheated_threshold /=5f; // 10x smaller since the Hot Things production in reactor is the same.
        } else {
            // If we're working with neither, fail out
            superheated_threshold=0;
            return false;
        }

        superheated = fluidAmountToConsume >= superheated_threshold; // set the internal superheated flag if we have enough hot fluid.  Used in the onRunningTick method.
        fluidAmountToConsume = Math.min(fluidAmountToConsume, superheated_threshold * 2); // Don't consume too much hot fluid per second
        mInputHotFluidHatch.drain(fluidAmountToConsume, true);
        this.mMaxProgresstime = 20;
        this.mEUt = (int) (fluidAmountToConsume * steam_output_multiplier * efficiency);
        if (do_lava) {
            mOutputColdFluidHatch.fill(FluidRegistry.getFluidStack("ic2pahoehoelava", fluidAmountToConsume), true);
            this.mEfficiencyIncrease = 80;
        } else {
            mOutputColdFluidHatch.fill(FluidRegistry.getFluidStack("ic2coolant", fluidAmountToConsume), true);
            this.mEfficiencyIncrease = 80;
        }
        return true;
    }

    private int useWater(float input) {
        water = water + input;
        int usage = (int) water;
        water = water - usage;
        return usage;
    }

    public boolean onRunningTick(ItemStack aStack) {
        if (this.mEUt > 0) {
            int tGeneratedEU = (int) (this.mEUt * 2L * this.mEfficiency / 10000L); // APPROXIMATELY how much steam to generate.
            if (tGeneratedEU > 0) {

                if (superheated) tGeneratedEU /= 2; // We produce half as much superheated steam if necessary

                int distilledConsumed = useWater(tGeneratedEU / 160f); // how much distilled water to consume
                //tGeneratedEU = distilledConsumed * 160; // EXACTLY how much steam to generate, producing a perfect 1:160 ratio with distilled water consumption

                FluidStack distilledStack = GT_ModHandler.getDistilledWater(distilledConsumed);
                if (depleteInput(distilledStack)) // Consume the distilled water
                {
                    if (superheated) {
                        addOutput(FluidRegistry.getFluidStack("ic2superheatedsteam", tGeneratedEU)); // Generate superheated steam
                    } else {
                        addOutput(GT_ModHandler.getSteam(tGeneratedEU)); // Generate regular steam
                    }
                } else {
                    GT_Log.exp.println(this.mName+" had no more Distilled water!");
                    explodeMultiblock(); // Generate crater
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        mInputHotFluidHatch = null;
        mOutputColdFluidHatch = null;
        return checkPiece(STRUCTURE_PIECE, 1, 3, 0) && SHELL.isSatisfied(this, casingCount)
                && HOT_INPUT.isSatisfied(this, 0) && COLD_OUTPUT.isSatisfied(this, 0);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 1, 3, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 1, 3, 0, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_HeatExchanger> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_HeatExchanger>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "COC", "CCC"},
                            {"CCC", "CPC", "CCC"},
                            {"CCC", "CPC", "CCC"},
                            {"C~C", "CIC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_HeatExchanger.class)
                            .preferForPlacement(tile -> tile instanceof GTMTE_BusHatch_Input)
                            .buildAndChain(SHELL, machine -> ++machine.casingCount))
                    .addElement('P', Casings.TITANIUM_PIPE.asElement())
                    .addElement('I', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_HeatExchanger.class)
                            .hint(HOT_HINT).build(HOT_INPUT))
                    .addElement('O', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_HeatExchanger.class)
                            .hint(COLD_HINT).build(COLD_OUTPUT))
                    .build();
        }
        return structureDefinition;
    }

    private enum ExchangerHatch implements IHatchElement {
        WATER,
        STEAM,
        HOT,
        COLD;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
                case WATER:
                    return HatchElement.Input.matches(tile);
                case STEAM:
                    return HatchElement.Output.matches(tile);
                case HOT:
                    return tile instanceof GT_MetaTileEntity_Hatch_Input;
                case COLD:
                    return tile instanceof GT_MetaTileEntity_Hatch_Output;
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            switch (this) {
                case WATER:
                    return tile instanceof GT_MetaTileEntity_Hatch_Input
                            || tile instanceof GTMTE_Multi_Hatch_Input || tile instanceof GTMTE_BusHatch_Input;
                case STEAM:
                    return tile instanceof GT_MetaTileEntity_Hatch_Output;
                default:
                    return matches(tile);
            }
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
                case WATER:
                    return machine.mInputHatches.size() + machine.mQuadrInputHatches.size()
                            + machine.mInputBusHatches.size();
                case STEAM:
                    return machine.mOutputHatches.size();
                case HOT:
                    return ((GT_MetaTileEntity_HeatExchanger) machine).mInputHotFluidHatch == null ? 0 : 1;
                case COLD:
                    return ((GT_MetaTileEntity_HeatExchanger) machine).mOutputColdFluidHatch == null ? 0 : 1;
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            switch (this) {
                case WATER:
                    return machine.addInputToMachineList(tile, texture);
                case STEAM:
                    return machine.addOutputToMachineList(tile, texture);
                case HOT:
                    return ((GT_MetaTileEntity_HeatExchanger) machine).addHotFluidInputToMachineList(tile, texture);
                case COLD:
                    return ((GT_MetaTileEntity_HeatExchanger) machine).addColdFluidOutputToMachineList(tile, texture);
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public String getDescription() {
            switch (this) {
                case WATER:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.exchanger_water");
                case STEAM:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.exchanger_steam");
                case HOT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.exchanger_hot");
                case COLD:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.exchanger_cold");
                default:
                    throw new AssertionError(this);
            }
        }
    }

    public boolean addColdFluidOutputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Output) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            mOutputColdFluidHatch = (GT_MetaTileEntity_Hatch_Output) aMetaTileEntity;
            return true;
        }
        return false;
    }

    public boolean addHotFluidInputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mRecipeMap = getRecipeMap();
            mInputHotFluidHatch = (GT_MetaTileEntity_Hatch_Input) aMetaTileEntity;
            return true;
        }
        return false;
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_HeatExchanger(this.mName);
    }

    @Override
    public String[] getInfoData() {
        return new String[]{
        		StatCollector.translateToLocal("GT5U.multiblock.Progress")+": "+
                EnumChatFormatting.GREEN + Integer.toString(mProgresstime/20) + EnumChatFormatting.RESET +" s / "+
                        EnumChatFormatting.YELLOW + Integer.toString(mMaxProgresstime/20) + EnumChatFormatting.RESET +" s",
                StatCollector.translateToLocal("GT5U.multiblock.usage")+" "+StatCollector.translateToLocal("GT5U.LHE.steam")+": "+
                        (superheated?EnumChatFormatting.RED:EnumChatFormatting.YELLOW) + Integer.toString(superheated?-2*mEUt:-mEUt) + EnumChatFormatting.RESET + " EU/t",
                StatCollector.translateToLocal("GT5U.multiblock.problems")+": "+
                        EnumChatFormatting.RED+ (getIdealStatus() - getRepairStatus())+EnumChatFormatting.RESET+
                " "+StatCollector.translateToLocal("GT5U.multiblock.efficiency")+": "+
                        EnumChatFormatting.YELLOW+Float.toString(mEfficiency / 100.0F)+EnumChatFormatting.RESET + " %",
                StatCollector.translateToLocal("GT5U.LHE.superheated")+": "+ (superheated?EnumChatFormatting.RED:EnumChatFormatting.BLUE) + superheated + EnumChatFormatting.RESET,
                StatCollector.translateToLocal("GT5U.LHE.superheated")+" "+StatCollector.translateToLocal("GT5U.LHE.threshold")+": "+ EnumChatFormatting.GREEN + superheated_threshold + EnumChatFormatting.RESET
        };
    }
}
