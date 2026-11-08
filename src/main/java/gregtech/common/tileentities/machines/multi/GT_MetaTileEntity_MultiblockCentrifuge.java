package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Textures;
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
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_MultiblockCentrifuge
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_MultiblockCentrifuge>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SHELL_HINT = HintColor.GREEN;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.CLEAN_STAINLESS_STEEL, 8)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output).build();
    private static IStructureDefinition<GT_MetaTileEntity_MultiblockCentrifuge> structureDefinition;
    private int casingCount;

    public GT_MetaTileEntity_MultiblockCentrifuge(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_MultiblockCentrifuge(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_MultiblockCentrifuge(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Large Centrifuge")
                .addInfo("Processes all Centrifuge recipes.")
                .beginStructureBlock(3, 3, 3, false)
                .addController("front center")
                .addStructureSection("Components")
                .addOtherStructurePart(SHELL.getCasing(), "8+; fill unused shell positions")
                .addOtherStructurePart(Casings.MOTOR, "exactly 1; center of the structure")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Maintenance and Energy Hatches", "1+ each; shell")
                .addHatchInfo("Input and Output", "1+ bus or hatch each; shell")
                .addStructureHint("Shell: casings or hatches/buses", SHELL_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: available hatches in free positions; existing casings are kept.")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex,
            boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[] { Casings.CLEAN_STAINLESS_STEEL.getTexture(),
                    TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE
                            : Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR) };
        }
        return new ITexture[] { Casings.CLEAN_STAINLESS_STEEL.getTexture() };
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sMultiblockCentrifugeRecipes);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(this::getRecipeMap)
                .setOverclock(2, 4);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        return checkPiece(STRUCTURE_PIECE, 1, 1, 0) && SHELL.isSatisfied(this, casingCount);
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
    public IStructureDefinition<GT_MetaTileEntity_MultiblockCentrifuge> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_MultiblockCentrifuge>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CCC", "CCC"},
                            {"C~C", "CMC", "CCC"},
                            {"CCC", "CCC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_MultiblockCentrifuge.class)
                            .autoPlace(HatchElement.InputBus, HatchElement.InputHatch, HatchElement.OutputBus, HatchElement.OutputHatch)
                            .buildAndChain(SHELL, machine -> ++machine.casingCount).withHint(SHELL_HINT))
                    .addElement('M', Casings.MOTOR.asElement())
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
