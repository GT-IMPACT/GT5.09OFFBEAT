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
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_MultiblockElectrolyzer
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_MultiblockElectrolyzer>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SHELL_HINT = HintColor.GREEN;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.STABLE_TITANIUM, 8)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output).build();
    private static IStructureDefinition<GT_MetaTileEntity_MultiblockElectrolyzer> structureDefinition;
    private int casingCount;
    private int coilCount;

    public GT_MetaTileEntity_MultiblockElectrolyzer(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_MultiblockElectrolyzer(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_MultiblockElectrolyzer(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Multiblock Electrolyzer")
                .addInfo("Processes all Electrolyzer recipes.")
                .beginStructureBlock(3, 3, 3, false)
                .addController("front center")
                .addStructureSection("Components")
                .addOtherStructurePart(SHELL.getCasing(), "8+; fill unused shell positions")
                .addOtherStructurePart(Casings.PUMP, "exactly 1; center of the structure")
                .addOtherStructurePart(Casings.CUPRONICKEL_COIL, "1-5; touching the pump, except toward the controller")
                .addStructureInfo("Construction keeps existing coils or places one in a free position if needed.")
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
            return new ITexture[] { Casings.STABLE_TITANIUM.getTexture(),
                    TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE
                            : Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR) };
        }
        return new ITexture[] { Casings.STABLE_TITANIUM.getTexture() };
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sMultiblockElectrolyzerRecipes);
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
    public IStructureDefinition<GT_MetaTileEntity_MultiblockElectrolyzer> getStructureDefinition() {
        if (structureDefinition == null) {
            IStructureElement<GT_MetaTileEntity_MultiblockElectrolyzer> shell =
                    GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_MultiblockElectrolyzer.class)
                            .autoPlace(HatchElement.InputBus, HatchElement.InputHatch, HatchElement.OutputBus, HatchElement.OutputHatch)
                            .buildAndChain(SHELL, machine -> ++machine.casingCount).withHint(SHELL_HINT);
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_MultiblockElectrolyzer>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CHC", "CCC"},
                            {"C~C", "HPH", "CHC"},
                            {"CCC", "CHC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', shell)
                    .addElement('P', Casings.PUMP.asElement())
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
