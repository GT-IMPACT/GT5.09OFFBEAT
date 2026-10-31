package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.HeatingCoil;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GTMTE_BusHatch_Input;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import net.minecraft.world.World;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

import static gregtech.api.enums.GT_Values.V;
import static space.impact.structures.structure.StructureUtility.transpose;
import static gregtech.api.enums.Textures.BlockIcons.*;

public class GT_MetaTileEntity_ElectricBlastFurnace
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_ElectricBlastFurnace>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor BOTTOM_HINT = HintColor.BLUE;
    private static final HintColor RECOVERY_HINT = HintColor.YELLOW;
    private static final HintColor MUFFLER_HINT = HintColor.ORANGE;
    private static final StructureRequirements BOTTOM = StructureRequirements.builder(Casings.HEAT_PROOF, 0)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, BlastHatch.BOTTOM_OUTPUT)
            .build();
    private static final StructureRequirements TOP = StructureRequirements.builder(Casings.HEAT_PROOF, 0).build();
    private static final StructureRequirements EXHAUST = StructureRequirements.builder(Casings.HEAT_PROOF, 0)
            .atLeast(BlastHatch.MUFFLER).build();
    private static IStructureDefinition<GT_MetaTileEntity_ElectricBlastFurnace> structureDefinition;
    private HeatingCoil checkedCoil;
    private int bottomOutputs;
    private int mHeatingCapacity = 0;
    private int controllerY;
    private FluidStack[] pollutionFluidStacks = new FluidStack[]{Materials.CarbonDioxide.getGas(1000),
            Materials.CarbonMonoxide.getGas(1000), Materials.SulfurDioxide.getGas(1000)};
    

    public GT_MetaTileEntity_ElectricBlastFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_ElectricBlastFurnace(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_ElectricBlastFurnace(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Electric Blast Furnace")
                .addInfo("Each 900 K above the recipe requirement multiplies EU/t by 0.95")
                .addInfo("Each 1800 K above the recipe requirement upgrades one overclock")
                .addInfo("Upgraded overclocks reduce recipe time to 25% and increase EU/t to 400%")
                .addInfo("Optional top output hatches recover CO2, CO and SO2; recovery depends on muffler tier")
                .addPollutionAmount(getPollutionPerTick(null) * 20)
                .beginStructureBlock(3, 4, 3, true)
                .addController("Front center of the bottom layer")
                .addRequirements(BOTTOM, "Bottom layer, replacing heat-proof casings", BOTTOM_HINT)
                .addRequirements(EXHAUST, "Top center", MUFFLER_HINT)
                .addOtherStructurePart("Optional gas Output Hatch", "Top layer, replacing heat-proof casings")
                .addStructureHint("Optional gas Output Hatch", RECOVERY_HINT)
                .addOtherStructurePart(16, "Heating Coils", "Two middle layers, surrounding the empty center")
                .addAir("Centers of both middle layers")
                .addStructureInfo("All sixteen heating coils must have the same tier.")
                .addStructureInfo("Top gas output hatches do not replace the required bottom output hatch or bus.")
                .addStructureInfo("Fill unused bottom and top positions with heat-proof casings.")
                .addStructureInfo("Compatible IMPACT input and energy hatches are also accepted.")
                .addMasterChannel("Coil tier from 1 to 10; existing coils determine the tier when completing a structure")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive)
                return new ITexture[]{
                        Casings.HEAT_PROOF.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    Casings.HEAT_PROOF.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW).glow().build()};
        }
        return new ITexture[]{Casings.HEAT_PROOF.getTexture()};
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "ElectricBlastFurnace.png");
    }

    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sBlastRecipes;
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    public boolean checkRecipe(ItemStack aStack) {

        ArrayList<ArrayList<ItemStack>> tBusInputs = new ArrayList<>();
        FluidStack[] tFluids = getStoredFluids().toArray(new FluidStack[]{});

        for (GT_MetaTileEntity_Hatch_InputBus tBus : mInputBusses) {
            ArrayList<ItemStack> tBusItems = new ArrayList<ItemStack>();
            tBus.mRecipeMap = getRecipeMap();
            if (isValidMetaTileEntity(tBus)) {
                for (int i = tBus.getBaseMetaTileEntity().getSizeInventory() - 1; i >= 0; i--) {
                    if (tBus.getBaseMetaTileEntity().getStackInSlot(i) != null)
                        tBusItems.add(tBus.getBaseMetaTileEntity().getStackInSlot(i));
                }
            }
            tBusInputs.add(tBusItems);
        }

        for (GTMTE_BusHatch_Input tBus : mInputBusHatches) {
            tBus.mRecipeMap = getRecipeMap();
            if (!isValidMetaTileEntity(tBus)) continue;
            tBusInputs.add(GTMTE_BusHatch_Input.getStoredInputs(tBus));
        }

        for (ArrayList<ItemStack> tBusItems : tBusInputs) {
            if (!tBusItems.isEmpty()) {
                ItemStack[] tInputs = tBusItems.toArray(new ItemStack[]{});
                long tVoltage = getMaxInputVoltage();
                byte tTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));
                GT_Recipe tRecipe = getRecipeMap().findRecipe(getBaseMetaTileEntity(), cashedRecipe, false, gregtech.api.enums.GT_Values.V[tTier], tFluids, tInputs);
                if ((tRecipe != null) && (this.mHeatingCapacity >= tRecipe.mSpecialValue) && (tRecipe.isRecipeInputEqual(true, tFluids, tInputs))) {
                    cashedRecipe = tRecipe;
                    this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
                    this.mEfficiencyIncrease = 10000;
                    int tHeatCapacityDivTiers = (mHeatingCapacity - tRecipe.mSpecialValue) / 900;
                    byte overclockCount = calculateOverclockednessEBF(tRecipe.mEUt, tRecipe.mDuration, tVoltage);
                    //In case recipe is too OP for that machine
                    if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
                        return false;
                    if (this.mEUt > 0) {
                        this.mEUt = (-this.mEUt);
                    }
                    if (tHeatCapacityDivTiers > 0) {
                        this.mEUt = (int) (this.mEUt * (Math.pow(0.95, tHeatCapacityDivTiers)));
                        this.mMaxProgresstime >>= Math.min(tHeatCapacityDivTiers / 2, overclockCount);//extra free overclocking if possible
                        if (this.mMaxProgresstime < 1) this.mMaxProgresstime = 1;//no eu efficiency correction
                    }
                    this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
                    this.mOutputItems = new ItemStack[]{tRecipe.getOutput(0), tRecipe.getOutput(1)};
                    this.mOutputFluids = new FluidStack[]{tRecipe.getFluidOutput(0)};
                    updateSlots();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calcualtes overclocked ness using long integers
     * @param aEUt          - recipe EUt
     * @param aDuration     - recipe Duration
     */
    protected byte calculateOverclockednessEBF(int aEUt, int aDuration, long maxInputVoltage) {
        byte mTier=(byte)Math.max(0,GT_Utility.getTier(maxInputVoltage)), timesOverclocked=0;
        if(mTier==0){
            //Long time calculation
            long xMaxProgresstime = ((long)aDuration)<<1;
            if(xMaxProgresstime>Integer.MAX_VALUE-1){
                //make impossible if too long
                mEUt=Integer.MAX_VALUE-1;
                mMaxProgresstime=Integer.MAX_VALUE-1;
            }else{
                mEUt=aEUt>>2;
                mMaxProgresstime=(int)xMaxProgresstime;
            }
            //return 0;
        }else{
            //Long EUt calculation
            long xEUt=aEUt;
            //Isnt too low EUt check?
            long tempEUt = xEUt<V[1] ? V[1] : xEUt;

            mMaxProgresstime = aDuration;

            while (tempEUt <= V[mTier -1]) {
                tempEUt<<=2;//this actually controls overclocking
                //xEUt *= 4;//this is effect of everclocking
                mMaxProgresstime>>=1;//this is effect of overclocking
                xEUt = mMaxProgresstime==0 ? xEUt>>1 : xEUt<<2;//U know, if the time is less than 1 tick make the machine use less power
                timesOverclocked++;
            }
            if(xEUt>Integer.MAX_VALUE-1){
                mEUt = Integer.MAX_VALUE-1;
                mMaxProgresstime = Integer.MAX_VALUE-1;
            }else{
                mEUt = (int)xEUt;
                if(mEUt==0)
                    mEUt = 1;
                if(mMaxProgresstime==0)
                    mMaxProgresstime = 1;//set time to 1 tick
            }
        }
        return timesOverclocked;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        controllerY = base.getYCoord();
        mHeatingCapacity = 0;
        checkedCoil = null;
        bottomOutputs = 0;
        if (!checkPiece(STRUCTURE_PIECE, 1, 3, 0) || checkedCoil == null
                || !BOTTOM.isSatisfied(this, 0) || !EXHAUST.isSatisfied(this, 0)) {
            return false;
        }
        mHeatingCapacity = checkedCoil.getHeatCapacity() + 100 * (GT_Utility.getTier(getMaxInputVoltage()) - 2);
        if (base.isServerSide()) {
            replaceDeprecatedCoils(base);
        }
        return true;
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
    public IStructureDefinition<GT_MetaTileEntity_ElectricBlastFurnace> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_ElectricBlastFurnace>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"TTT", "TMT", "TTT"},
                            {"HHH", "H-H", "HHH"},
                            {"HHH", "H-H", "HHH"},
                            {"B~B", "BBB", "BBB"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('B', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_ElectricBlastFurnace.class)
                            .buildAndChain(BOTTOM).withHint(BOTTOM_HINT))
                    .addElement('T', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_ElectricBlastFurnace.class)
                            .allow(HatchElement.Output).buildAndChain(TOP).withHint(RECOVERY_HINT))
                    .addElement('M', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_ElectricBlastFurnace.class)
                            .hint(MUFFLER_HINT).build(EXHAUST))
                    .addElement('H', GTStructureUtility.ofHeatingCoil(
                            GT_MetaTileEntity_ElectricBlastFurnace::checkCoil,
                            GT_MetaTileEntity_ElectricBlastFurnace::constructionCoil))
                    .build();
        }
        return structureDefinition;
    }

    private boolean checkCoil(HeatingCoil coil) {
        if (checkedCoil == null) {
            checkedCoil = coil;
        }
        return checkedCoil == coil;
    }

    private HeatingCoil constructionCoil(ItemStack trigger) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        World world = base.getWorld();
        ForgeDirection front = getExtendedFacing().getDirection();
        int centerX = base.getXCoord() - front.offsetX;
        int centerZ = base.getZCoord() - front.offsetZ;
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                if (x == centerX && z == centerZ) {
                    continue;
                }
                for (int y = base.getYCoord() + 1; y <= base.getYCoord() + 2; y++) {
                    HeatingCoil coil = HeatingCoil.at(world, x, y, z);
                    if (coil != null) {
                        return coil;
                    }
                }
            }
        }
        return HeatingCoil.fromTier(trigger == null ? 1 : trigger.stackSize);
    }

    private enum BlastHatch implements IHatchElement {
        BOTTOM_OUTPUT,
        MUFFLER;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            return this == BOTTOM_OUTPUT ? HatchElement.Output.matches(tile)
                    : tile instanceof GT_MetaTileEntity_Hatch_Muffler;
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            return this == BOTTOM_OUTPUT ? ((GT_MetaTileEntity_ElectricBlastFurnace) machine).bottomOutputs
                    : machine.mMufflerHatches.size();
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            if (this == MUFFLER) {
                return machine.addMufflerToMachineList(tile, texture);
            }
            if (!machine.addOutputToMachineList(tile, texture)) {
                return false;
            }
            ++((GT_MetaTileEntity_ElectricBlastFurnace) machine).bottomOutputs;
            return true;
        }

        @Override
        public String getDescription() {
            return this == BOTTOM_OUTPUT ? HatchElement.Output.getDescription()
                    : StatCollector.translateToLocal("gt.tooltip.hatch.muffler");
        }
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    public int getPollutionPerTick(ItemStack aStack) {
        return 20;
    }

    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    private void replaceDeprecatedCoils(IGregTechTileEntity aBaseMetaTileEntity) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        int tX = aBaseMetaTileEntity.getXCoord() + xDir;
        int tY = (int) aBaseMetaTileEntity.getYCoord();
        int tZ = aBaseMetaTileEntity.getZCoord() + zDir;
        int tUsedMeta;
        for (int xPos = tX - 1; xPos <= tX + 1; xPos++) {
            for (int zPos = tZ - 1; zPos <= tZ + 1; zPos++) {
                if ((xPos == tX) && (zPos == tZ)) {
                    continue;
                }
                for (int yPos = tY + 1; yPos <= tY + 2; yPos++) {
                    tUsedMeta = aBaseMetaTileEntity.getMetaID(xPos, yPos, zPos);
                    if (tUsedMeta >= 12 && tUsedMeta <= 14 && aBaseMetaTileEntity.getBlock(xPos, yPos, zPos) == GregTech_API.sBlockCasings1) {
                        aBaseMetaTileEntity.getWorld().setBlock(xPos, yPos, zPos, GregTech_API.sBlockCasings5, tUsedMeta - 12, 3);
                    }
                }
            }
        }
    }
    @Override
     public boolean addOutput(FluidStack aLiquid) {
        if (aLiquid == null) return false;
        int targetHeight;
        FluidStack tLiquid = aLiquid.copy();
        boolean isOutputPollution = false;
        for (FluidStack pollutionFluidStack : pollutionFluidStacks) {
            if (tLiquid.isFluidEqual(pollutionFluidStack)) {
                isOutputPollution = true;
                break;
            }
        }
        if (isOutputPollution) {
            targetHeight = this.controllerY + 3;
            int pollutionReduction = 0;
            for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches) {
                if (isValidMetaTileEntity(tHatch)) {
                    pollutionReduction = 100 - tHatch.calculatePollutionReduction(100);
                    break;
                }
            }
            tLiquid.amount = tLiquid.amount * (pollutionReduction + 5) / 100;
        } else {
            targetHeight = this.controllerY;
        }
        for (GT_MetaTileEntity_Hatch_Output tHatch : mOutputHatches) {
            if (isValidMetaTileEntity(tHatch) && GT_ModHandler.isSteam(aLiquid) ? tHatch.outputsSteam() : tHatch.outputsLiquids()) {
                if (tHatch.getBaseMetaTileEntity().getYCoord() == targetHeight) {
                    int tAmount = tHatch.fill(tLiquid, false);
                    if (tAmount >= tLiquid.amount) {
                        return tHatch.fill(tLiquid, true) >= tLiquid.amount;
                    } else if (tAmount > 0) {
                        tLiquid.amount = tLiquid.amount - tHatch.fill(tLiquid, true);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String[] addInfoData() {
        final ArrayList<String> ll = new ArrayList<>();
        ll.add(StatCollector.translateToLocal("GT5U.EBF.heat") + ": " +
            EnumChatFormatting.GREEN + mHeatingCapacity + EnumChatFormatting.RESET + " K");
        final String[] a = new String[ll.size()];
        return ll.toArray(a);
    }
}
