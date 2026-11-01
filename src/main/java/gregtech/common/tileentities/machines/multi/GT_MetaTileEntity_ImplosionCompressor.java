package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.gui.GT_GUIContainer_ProcessingMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import net.minecraft.util.StatCollector;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.ofChain;
import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_ImplosionCompressor
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_ImplosionCompressor>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.SOLID_STEEL, 16)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, ImplosionHatch.INPUT_BUS, ImplosionHatch.OUTPUT_BUS,
                    ImplosionHatch.MUFFLER)
            .build();
    private static IStructureDefinition<GT_MetaTileEntity_ImplosionCompressor> structureDefinition;
    private int casingCount;

    public GT_MetaTileEntity_ImplosionCompressor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_ImplosionCompressor(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_ImplosionCompressor(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Implosion Compressor")
                .addInfo("Compresses materials using explosives")
                .addPollutionAmount(getPollutionPerTick(null) * 20)
                .beginStructureBlock(3, 3, 3, true)
                .addController("Front center")
                .addRequirements(SHELL, "Any casing position")
                .addAir("Center of the structure")
                .addStructureInfo("Explosion warning signs may replace solid steel casings and count toward the same minimum.")
                .addStructureInfo("Item input and output buses are required; fluid-only hatches do not satisfy these requirements.")
                .addStructureInfo("IMPACT combined item/fluid inputs and multi-amp energy hatches are accepted.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex,
            boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) {
                return new ITexture[] { Casings.SOLID_STEEL.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_IMPLOSION_COMPRESSOR_ACTIVE), TextureFactory.builder()
                                .addIcon(OVERLAY_FRONT_IMPLOSION_COMPRESSOR_ACTIVE_GLOW).glow().build() };
            }
            return new ITexture[] { Casings.SOLID_STEEL.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_IMPLOSION_COMPRESSOR),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_IMPLOSION_COMPRESSOR_GLOW).glow().build() };
        }
        return new ITexture[] { Casings.SOLID_STEEL.getTexture() };
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_ProcessingMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(),
                "ImplosionCompressor.png");
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sImplosionRecipes);
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(this::getRecipeMap)
                .setOverclock(2, 4);
    }

    @Override
    public boolean checkRecipe(ItemStack stack) {
        if (!super.checkRecipe(stack)) {
            return false;
        }
        sendLoopStart((byte) 20);
        return true;
    }

    public void startSoundLoop(byte aIndex, double aX, double aY, double aZ) {
        super.startSoundLoop(aIndex, aX, aY, aZ);
        if (aIndex == 20) {
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(5), 10, 1.0F, aX, aY, aZ);
        }
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
    public IStructureDefinition<GT_MetaTileEntity_ImplosionCompressor> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_ImplosionCompressor>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CCC", "CCC"},
                            {"C~C", "C-C", "CCC"},
                            {"CCC", "CCC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_ImplosionCompressor.class)
                            .buildAndChain(SHELL, ofChain(Casings.SOLID_STEEL.asElement(),
                                    Casings.EXPLOSION_HAZARD_SIGN.asElement()), machine -> ++machine.casingCount))
                    .build();
        }
        return structureDefinition;
    }

    private enum ImplosionHatch implements IHatchElement {
        INPUT_BUS, OUTPUT_BUS, MUFFLER;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
            case INPUT_BUS:
                return HatchElement.Input.matches(tile);
            case OUTPUT_BUS:
                return HatchElement.Output.matches(tile);
            case MUFFLER:
                return tile instanceof GT_MetaTileEntity_Hatch_Muffler;
            default:
                throw new AssertionError(this);
            }
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            switch (this) {
            case INPUT_BUS:
                return tile instanceof GT_MetaTileEntity_Hatch_InputBus || tile instanceof GTMTE_BusHatch_Input;
            case OUTPUT_BUS:
                return tile instanceof GT_MetaTileEntity_Hatch_OutputBus;
            default:
                return matches(tile);
            }
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
            case INPUT_BUS:
                return machine.mInputBusses.size() + machine.mInputBusHatches.size();
            case OUTPUT_BUS:
                return machine.mOutputBusses.size();
            case MUFFLER:
                return machine.mMufflerHatches.size();
            default:
                throw new AssertionError(this);
            }
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            switch (this) {
            case INPUT_BUS:
                return machine.addInputToMachineList(tile, texture);
            case OUTPUT_BUS:
                return machine.addOutputToMachineList(tile, texture);
            case MUFFLER:
                return machine.addMufflerToMachineList(tile, texture);
            default:
                throw new AssertionError(this);
            }
        }

        @Override
        public String getDescription() {
            switch (this) {
            case INPUT_BUS:
                return StatCollector.translateToLocal("gt.tooltip.hatch.input_bus");
            case OUTPUT_BUS:
                return StatCollector.translateToLocal("gt.tooltip.hatch.output_bus");
            case MUFFLER:
                return StatCollector.translateToLocal("gt.tooltip.hatch.muffler");
            default:
                throw new AssertionError(this);
            }
        }
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    public int getPollutionPerTick(ItemStack aStack) {
        return 500;
    }

    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }
}
