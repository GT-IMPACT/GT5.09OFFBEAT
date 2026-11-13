package gregtech.common.tileentities.machines.multi;

import gregtech.GT_Mod;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static space.impact.structures.structure.StructureUtility.transpose;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine.isValidForLowGravity;

public class GT_MetaTileEntity_LargeChemicalReactor
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_LargeChemicalReactor>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SHELL_HINT = HintColor.GREEN;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.CHEMICALLY_INERT, 8)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output)
            .atMost(HatchElement.Maintenance, 1)
            .atMost(HatchElement.Energy, 4)
            .build();
    private static IStructureDefinition<GT_MetaTileEntity_LargeChemicalReactor> structureDefinition;
    private int casingCount;
    private int coilCount;

    public GT_MetaTileEntity_LargeChemicalReactor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeChemicalReactor(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeChemicalReactor(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Large Chemical Reactor")
                .addInfo("Has the same recipes as the Chemical Reactor.")
                .addInfo("Accepts fluids instead of fluid cells.")
                .beginStructureBlock(3, 3, 3, false)
                .addController("front center")
                .addStructureSection("Components")
                .addOtherStructurePart(SHELL.getCasing(), "8+; fill unused shell positions")
                .addOtherStructurePart(Casings.PTFE_PIPE, "exactly 1; center of the structure")
                .addOtherStructurePart(Casings.CUPRONICKEL_COIL, "1-5; touching the pipe, except toward the controller")
                .addStructureInfo("Construction keeps existing coils or places one in a free position if needed.")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Maintenance Hatch", "exactly 1; shell")
                .addHatchInfo("Energy Hatches", "1-4; shell")
                .addHatchInfo("Input and Output", "1+ bus or hatch each; shell")
                .addStructureHint("Shell: casings or hatches/buses", SHELL_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: available hatches in free positions; existing casings are kept.")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex,
            boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) {
                return new ITexture[] { Casings.CHEMICALLY_INERT.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE), TextureFactory.builder()
                                .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE_GLOW).glow().build() };
            }
            return new ITexture[] { Casings.CHEMICALLY_INERT.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_GLOW).glow().build() };
        }
        return new ITexture[] { Casings.CHEMICALLY_INERT.getTexture() };
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public boolean allowSelectCircuit() {
        return true;
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sMultiblockChemicalRecipes);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(this::getRecipeMap)
                .setOverclock(2, 4)
                .setRecipeValidator(this::validateRecipeEnvironment);
    }

    private CheckRecipeResult validateRecipeEnvironment(GT_Recipe recipe) {
        if (GT_Mod.gregtechproxy.mLowGravProcessing && recipe.mSpecialValue == -100
                && !isValidForLowGravity(recipe, getBaseMetaTileEntity().getWorld().provider.dimensionId)) {
            return CheckRecipeResult.LOW_GRAVITY_REQUIRED;
        }
        if (recipe.mSpecialValue == -200 && (mCleanroom == null || mCleanroom.mEfficiency <= 0)) {
            return CheckRecipeResult.CLEANROOM_REQUIRED;
        }
        return CheckRecipeResult.SUCCESS;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        coilCount = 0;
        return checkPiece(STRUCTURE_PIECE, 1, 1, 0) && coilCount > 0 && SHELL.isSatisfied(this, casingCount);
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
    public IStructureDefinition<GT_MetaTileEntity_LargeChemicalReactor> getStructureDefinition() {
        if (structureDefinition == null) {
            IStructureElement<GT_MetaTileEntity_LargeChemicalReactor> shell =
                    GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_LargeChemicalReactor.class)
                            .autoPlace(HatchElement.InputBus, HatchElement.InputHatch, HatchElement.OutputBus, HatchElement.OutputHatch)
                            .buildAndChain(SHELL, machine -> ++machine.casingCount).withHint(SHELL_HINT);
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_LargeChemicalReactor>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CHC", "CCC"},
                            {"C~C", "HPH", "CHC"},
                            {"CCC", "CHC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', shell)
                    .addElement('P', Casings.PTFE_PIPE.asElement())
                    .addElement('H', GTStructureUtility.ofCasingAroundCenter(
                            Casings.CUPRONICKEL_COIL, machine -> ++machine.coilCount, shell))
                    .build();
        }
        return structureDefinition;
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

}
