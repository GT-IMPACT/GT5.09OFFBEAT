package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_IndustrialPulverizer
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_IndustrialPulverizer>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor SHELL_HINT = HintColor.GREEN;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.SOLID_STEEL, 8)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output).build();
    private static IStructureDefinition<GT_MetaTileEntity_IndustrialPulverizer> structureDefinition;
    private int casingCount;
    private int pipeCount;

    public GT_MetaTileEntity_IndustrialPulverizer(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_IndustrialPulverizer(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_IndustrialPulverizer(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Industrial Pulverizer")
                .addInfo("Processes Macerator, Ore Washer and Chemical Bath recipes.")
                .addInfo("Does not lose efficiency when overclocked.")
                .beginStructureBlock(3, 3, 3, false)
                .addController("front center")
                .addStructureSection("Components")
                .addOtherStructurePart(SHELL.getCasing(), "8+; fill unused shell positions")
                .addOtherStructurePart(Casings.STEEL_GEARBOX, "exactly 1; center of the structure")
                .addOtherStructurePart(Casings.STEEL_PIPE, "1-5; touching the gearbox, except toward the controller")
                .addStructureInfo("Construction keeps existing pipes or places one in a free position if needed.")
                .addStructureSection("Hatches and buses")
                .addHatchInfo("Maintenance and Energy Hatches", "1+ each; shell")
                .addHatchInfo("Input and Output", "1+ bus or hatch each; shell")
                .addStructureHint("Shell: casings or hatches/buses", SHELL_HINT)
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "1: required hatches in free positions; existing casings are kept.")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex,
            boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[] { Casings.SOLID_STEEL.getTexture(),
                    TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE
                            : Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR) };
        }
        return new ITexture[] { Casings.SOLID_STEEL.getTexture() };
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sIndustrialPulverizerRecipes);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(this::getRecipeMap)
                .setOverclock(4, 4);
    }

    public void startSoundLoop(byte aIndex, double aX, double aY, double aZ) {
        super.startSoundLoop(aIndex, aX, aY, aZ);
        if (aIndex == 1) {
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(Integer.valueOf(201)), 10, 1.0F, aX, aY, aZ);
        }
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        pipeCount = 0;
        return checkPiece(STRUCTURE_PIECE, 1, 1, 0) && pipeCount > 0 && SHELL.isSatisfied(this, casingCount);
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
    public IStructureDefinition<GT_MetaTileEntity_IndustrialPulverizer> getStructureDefinition() {
        if (structureDefinition == null) {
            IStructureElement<GT_MetaTileEntity_IndustrialPulverizer> shell =
                    GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_IndustrialPulverizer.class)
                            .buildAndChain(SHELL, machine -> ++machine.casingCount).withHint(SHELL_HINT);
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_IndustrialPulverizer>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CPC", "CCC"},
                            {"C~C", "PGP", "CPC"},
                            {"CCC", "CPC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', shell)
                    .addElement('G', Casings.STEEL_GEARBOX.asElement())
                    .addElement('P', GTStructureUtility.ofCasingAroundCenter(
                            Casings.STEEL_PIPE, machine -> ++machine.pipeCount, shell))
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
