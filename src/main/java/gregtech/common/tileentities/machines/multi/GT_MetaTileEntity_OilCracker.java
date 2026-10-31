package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.HeatingCoil;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import net.minecraft.util.StatCollector;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_OilCracker
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_OilCracker>
        implements ISurvivalConstructable {
    private static final String INPUT_LEFT = "input_left";
    private static final String INPUT_RIGHT = "input_right";
    private static final HintColor INPUT_HINT = HintColor.BLUE;
    private static final HintColor REAGENT_HINT = HintColor.PURPLE;
    private static final HintColor OUTPUT_HINT = HintColor.ORANGE;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.CLEAN_STAINLESS_STEEL, 18)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy).build();
    private static final StructureRequirements INPUT = port(CrackerHatch.HYDROCARBON);
    private static final StructureRequirements REAGENT = port(CrackerHatch.REAGENT);
    private static final StructureRequirements OUTPUT = port(CrackerHatch.PRODUCT);
    private static IStructureDefinition<GT_MetaTileEntity_OilCracker> structureDefinition;
    private int casingCount;

    private static StructureRequirements port(CrackerHatch hatch) {
        return StructureRequirements.builder(Casings.CLEAN_STAINLESS_STEEL, 0).atLeast(hatch).build();
    }

    private static StructureRequirements withCommonRequirements(StructureRequirements port) {
        StructureRequirements.Builder builder = StructureRequirements.builder(port.getCasing(), 0);
        SHELL.getHatches().forEach(builder::atLeast);
        port.getHatches().forEach(builder::atLeast);
        return builder.build();
    }

    public GT_MetaTileEntity_OilCracker(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_OilCracker(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Oil Cracking Unit")
                .addInfo("Thermally cracks heavy hydrocarbons into lighter fractions")
                .beginStructureBlock(5, 3, 3, true)
                .addController("Front center")
                .addRequirements(SHELL, "Any casing position")
                .addRequirements(INPUT, "Either outer end face", INPUT_HINT)
                .addRequirements(REAGENT, "Middle casing ring", REAGENT_HINT)
                .addRequirements(OUTPUT, "Outer end face opposite the hydrocarbon input", OUTPUT_HINT)
                .addOtherStructurePart(Casings.CUPRONICKEL_COIL, "Two rings of eight, one on each side of the controller", 16)
                .addAir("Three blocks along the center, between the outer end faces")
                .addStructureInfo("Hydrocarbon inputs and product outputs must occupy opposite ends; both arrangements are accepted.")
                .addStructureInfo("Construction follows existing end inputs or outputs; without them, the preview selects the input end.")
                .addStructureInfo("Steam, hydrogen, radon, fluorine and zinc, nickel, niobium or silver plasma enter through the middle ring; other fluids enter through the hydrocarbon end.")
                .addStructureInfo("Compatible IMPACT multi-fluid and combined inputs and multi-amp energy hatches are accepted.")
                .addStructureInfo("Additional item buses are accepted in their respective input or output zones, but do not replace fluid hatches.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) return new ITexture[]{Casings.CLEAN_STAINLESS_STEEL.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_OIL_CRACKER_ACTIVE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_OIL_CRACKER_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{Casings.CLEAN_STAINLESS_STEEL.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_OIL_CRACKER),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_OIL_CRACKER_GLOW).glow().build()};
        }
        return new ITexture[]{Casings.CLEAN_STAINLESS_STEEL.getTexture()};
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "OilCrackingUnit.png");
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<FluidStack> tInputList = getStoredFluids();
        FluidStack[] tFluidInputs = tInputList.toArray(new FluidStack[0]);
        long tVoltage = getMaxInputVoltage();
        byte tTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));

        GT_Recipe tRecipe = GT_Recipe.GT_Recipe_Map.sCrakingRecipes.findRecipe(
                getBaseMetaTileEntity(), false, gregtech.api.enums.GT_Values.V[tTier], tFluidInputs , mInventory[1]);
        if (tRecipe != null && tRecipe.isRecipeInputEqual(true, tFluidInputs, mInventory[1])) {
            this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            this.mEfficiencyIncrease = 10000;
            calculateOverclockedNessMulti(tRecipe.mEUt, tRecipe.mDuration, 1, tVoltage);
            //In case recipe is too OP for that machine
            if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
                return false;
            if (this.mEUt > 0) {
                this.mEUt = (-this.mEUt);
            }
            this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
            this.mOutputFluids = new FluidStack[]{tRecipe.getFluidOutput(0)};
            return true;
        }
        return false;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        boolean valid = checkPiece(selectedPiece(), 2, 1, 0) && SHELL.isSatisfied(this, casingCount)
                && INPUT.isSatisfied(this, 0) && REAGENT.isSatisfied(this, 0) && OUTPUT.isSatisfied(this, 0);
        if (valid && base.isServerSide()) {
            replaceDeprecatedCoils(base);
        }
        return valid;
    }

    /** Read the existing end hatches without registering them or changing counters during previews. */
    private String selectedPiece() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        int inputSide = 0;
        int outputSide = 0;
        int[] offset = new int[3];
        for (int side : new int[]{-2, 2}) {
            for (int y = -1; y <= 1; y++) {
                for (int depth = 0; depth < 3; depth++) {
                    getExtendedFacing().getWorldOffset(new int[]{side, y, depth}, offset);
                    IGregTechTileEntity tile = base.getIGregTechTileEntityOffset(offset[0], offset[1], offset[2]);
                    if (tile == null) {
                        continue;
                    }
                    if (inputSide == 0 && HatchElement.Input.matches(tile.getMetaTileEntity())) {
                        inputSide = side;
                    }
                    if (outputSide == 0 && HatchElement.Output.matches(tile.getMetaTileEntity())) {
                        outputSide = side;
                    }
                }
            }
        }
        int selected = inputSide != 0 ? inputSide : outputSide != 0 ? -outputSide : -2;
        return selected == -2 ? INPUT_LEFT : INPUT_RIGHT;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(selectedPiece(), trigger, hintsOnly, 2, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(selectedPiece(), trigger, 2, 1, 0, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_OilCracker> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_OilCracker>builder()
                    .addShape(INPUT_LEFT, transpose(new String[][]{
                            {"IHCHO", "IHCHO", "IHCHO"},
                            {"IH~HO", "I---O", "IHCHO"},
                            {"IHCHO", "IHCHO", "IHCHO"}
                    }))
                    .addShape(INPUT_RIGHT, transpose(new String[][]{
                            {"OHCHI", "OHCHI", "OHCHI"},
                            {"OH~HI", "O---I", "OHCHI"},
                            {"OHCHI", "OHCHI", "OHCHI"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('I', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_OilCracker.class)
                            .buildAndChain(withCommonRequirements(INPUT), machine -> ++machine.casingCount)
                            .withHint(INPUT_HINT))
                    .addElement('C', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_OilCracker.class)
                            .buildAndChain(withCommonRequirements(REAGENT), machine -> ++machine.casingCount)
                            .withHint(REAGENT_HINT))
                    .addElement('O', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_OilCracker.class)
                            .buildAndChain(withCommonRequirements(OUTPUT), machine -> ++machine.casingCount)
                            .withHint(OUTPUT_HINT))
                    .addElement('H', GTStructureUtility.ofHeatingCoil(
                            (machine, coil) -> coil == HeatingCoil.CUPRONICKEL,
                            (machine, trigger) -> HeatingCoil.CUPRONICKEL))
                    .build();
        }
        return structureDefinition;
    }

    private enum CrackerHatch implements IHatchElement {
        HYDROCARBON,
        REAGENT,
        PRODUCT;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            return this == PRODUCT ? HatchElement.Output.matches(tile) : HatchElement.Input.matches(tile);
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            return this == PRODUCT ? tile instanceof GT_MetaTileEntity_Hatch_Output
                    : tile instanceof GT_MetaTileEntity_Hatch_Input || tile instanceof GTMTE_BusHatch_Input;
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            if (this == PRODUCT) {
                return machine.mOutputHatches.size();
            }
            GT_MetaTileEntity_OilCracker cracker = (GT_MetaTileEntity_OilCracker) machine;
            int count = 0;
            ArrayList<GT_MetaTileEntity_Hatch> inputs = new ArrayList<>(machine.mInputHatches);
            inputs.addAll(machine.mQuadrInputHatches);
            inputs.addAll(machine.mInputBusHatches);
            for (GT_MetaTileEntity_Hatch input : inputs) {
                if (cracker.isHatchInMiddleRing(input) == (this == REAGENT)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return this == PRODUCT ? machine.addOutputToMachineList(tile, texture)
                    : machine.addInputToMachineList(tile, texture);
        }

        @Override
        public String getDescription() {
            switch (this) {
                case HYDROCARBON:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.cracker_input");
                case REAGENT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.cracker_reagent");
                case PRODUCT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.cracker_output");
                default:
                    throw new AssertionError(this);
            }
        }
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

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_OilCracker(this.mName);
    }

    private void replaceDeprecatedCoils(IGregTechTileEntity aBaseMetaTileEntity) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        int tX = aBaseMetaTileEntity.getXCoord() + xDir;
        int tY = (int) aBaseMetaTileEntity.getYCoord();
        int tZ = aBaseMetaTileEntity.getZCoord() + zDir;
        for (int xPos = tX - 1; xPos <= tX + 1; xPos += (xDir != 0 ? 1 : 2)) {
            for (int yPos = tY - 1; yPos <= tY + 1; yPos++) {
                for (int zPos = tZ - 1; zPos <= tZ + 1; zPos += (xDir != 0 ? 2 : 1)) {
                    if ((yPos == tY) && (xPos == tX || zPos == tZ)) {
                        continue;
                    }
                    if (aBaseMetaTileEntity.getBlock(xPos, yPos, zPos) == GregTech_API.sBlockCasings1 &&
                            aBaseMetaTileEntity.getMetaID(xPos, yPos, zPos) == 12)
                    {
                        aBaseMetaTileEntity.getWorld().setBlock(xPos, yPos, zPos, GregTech_API.sBlockCasings5, 0, 3);
                    }
                }
            }
        }
    }

    @Override
    public ArrayList<FluidStack> getStoredFluids() {
        ArrayList<FluidStack> rList = new ArrayList<FluidStack>();
        ArrayList<GT_MetaTileEntity_Hatch> tHatches = new ArrayList<>(mInputHatches);
        tHatches.addAll(mQuadrInputHatches);
        tHatches.addAll(mInputBusHatches);
        for (GT_MetaTileEntity_Hatch tHatch : tHatches) {
            if (!isValidMetaTileEntity(tHatch)) continue;
            FluidStack[] tFluids;
            if (tHatch instanceof GTMTE_BusHatch_Input) {
                GTMTE_BusHatch_Input combined = (GTMTE_BusHatch_Input) tHatch;
                combined.mRecipeMap = getRecipeMap();
                tFluids = new FluidStack[]{combined.getFillableStack()};
            } else {
                GT_MetaTileEntity_Hatch_Input input = (GT_MetaTileEntity_Hatch_Input) tHatch;
                input.mRecipeMap = getRecipeMap();
                tFluids = input instanceof GTMTE_Multi_Hatch_Input
                        ? ((GTMTE_Multi_Hatch_Input) input).getStoredFluid()
                        : new FluidStack[]{input.getFillableStack()};
            }
            for (FluidStack tStack : tFluids) {
                if (tStack == null) continue;
                if (tStack.isFluidEqual(GT_ModHandler.getSteam(1000)) || tStack.isFluidEqual(Materials.Hydrogen.getGas(1000))
                        || tStack.isFluidEqual(Materials.Radon.getGas(1000)) || tStack.isFluidEqual(Materials.Fluorine.getGas(1000))
                        || tStack.isFluidEqual(Materials.Zinc.getPlasma(1)) || tStack.isFluidEqual(Materials.Nickel.getPlasma(1))
                        || tStack.isFluidEqual(Materials.Niobium.getPlasma(1)) || tStack.isFluidEqual(Materials.Silver.getPlasma(1))) {
                    if (isHatchInMiddleRing(tHatch)) {
                        rList.add(tStack);
                    }
                } else {
                    if (!isHatchInMiddleRing(tHatch)) {
                        rList.add(tStack);
                    }
                }
            }
        }
        return rList;
    }

    private boolean isHatchInMiddleRing(GT_MetaTileEntity_Hatch inputHatch) {
        ForgeDirection front = getExtendedFacing().getDirection();
        IGregTechTileEntity base = getBaseMetaTileEntity();
        IGregTechTileEntity input = inputHatch.getBaseMetaTileEntity();
        return front.offsetX == 0 ? input.getXCoord() == base.getXCoord() : input.getZCoord() == base.getZCoord();
    }
}
