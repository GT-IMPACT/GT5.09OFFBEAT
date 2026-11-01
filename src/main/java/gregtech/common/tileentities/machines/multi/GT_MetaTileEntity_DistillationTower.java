package gregtech.common.tileentities.machines.multi;

import gregtech.api.gui.GT_GUIContainer_ProcessingMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import net.minecraft.util.StatCollector;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.StructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_DistillationTower
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_DistillationTower>
        implements ISurvivalConstructable {
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 12;
    private static final HintColor BOTTOM_HINT = HintColor.BLUE;
    private static final HintColor OUTPUT_HINT = HintColor.ORANGE;
    private static final StructureRequirements COMMON = StructureRequirements.builder(Casings.CLEAN_STAINLESS_STEEL, 0)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy).build();
    private static final StructureRequirements BOTTOM_PORTS = StructureRequirements
            .builder(Casings.CLEAN_STAINLESS_STEEL, 0).atLeast(TowerHatch.FLUID_INPUT).build();
    private static final StructureRequirements BOTTOM = withCommonRequirements(BOTTOM_PORTS);
    private static final StructureRequirements[] OUTPUT_LAYERS = createOutputRequirements();
    private static final StructureRequirements[] SHELLS = createShellRequirements();
    private static IStructureDefinition<GT_MetaTileEntity_DistillationTower> structureDefinition;
    private int casingCount;

    private static StructureRequirements withCommonRequirements(StructureRequirements ports) {
        StructureRequirements.Builder builder = StructureRequirements.builder(ports.getCasing(),
                ports.getMinimumCasings());
        COMMON.getHatches().forEach(builder::atLeast);
        ports.getHatches().forEach(builder::atLeast);
        return builder.build();
    }

    private static StructureRequirements[] createOutputRequirements() {
        StructureRequirements[] layers = new StructureRequirements[MAX_HEIGHT];
        for (int y = 1; y < MAX_HEIGHT; y++) {
            layers[y] = StructureRequirements.builder(Casings.CLEAN_STAINLESS_STEEL, 0).atLeast(new LayerOutput(y))
                    .build();
        }
        return layers;
    }

    private static StructureRequirements[] createShellRequirements() {
        StructureRequirements[] shells = new StructureRequirements[MAX_HEIGHT + 1];
        for (int height = MIN_HEIGHT; height <= MAX_HEIGHT; height++) {
            shells[height] = StructureRequirements.builder(Casings.CLEAN_STAINLESS_STEEL, 7 * height - 5).build();
        }
        return shells;
    }

    public GT_MetaTileEntity_DistillationTower(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_DistillationTower(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_DistillationTower(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Distillation Tower")
                .addInfo("Separates fluids into fractions")
                .addInfo("Fluid output height equals its slot number in the NEI recipe, starting one block above the controller")
                .beginVariableStructureBlock(3, 3, MIN_HEIGHT, MAX_HEIGHT, 3, 3, true)
                .addController("Front center of the bottom layer")
                .addCasingInfoRange(Casings.CLEAN_STAINLESS_STEEL, SHELLS[MIN_HEIGHT].getMinimumCasings(), SHELLS[MAX_HEIGHT].getMinimumCasings(), false)
                .addStructureInfo("At least 7 x h - 5 casings are required, where h is the total tower height.")
                .addRequirements(COMMON, "Any casing position")
                .addRequirements(BOTTOM_PORTS, "Bottom layer", BOTTOM_HINT)
                .addRequirements(OUTPUT_LAYERS[1], "Each layer above the bottom, including the roof", OUTPUT_HINT)
                .addAir("Center of every intermediate layer; the bottom and roof are filled")
                .addStructureInfo("Each upper layer needs its own fluid output hatch; output buses do not count toward this requirement.")
                .addStructureInfo("Output buses are optional on any layer; bottom fluid outputs do not replace the required upper outlets.")
                .addStructureInfo("Compatible IMPACT fluid and combined inputs and multi-amp energy hatches are accepted.")
                .addMasterChannel("Total height = channel value + 2, from 3 to 12 blocks")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex,
            boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) {
                return new ITexture[] { Casings.CLEAN_STAINLESS_STEEL.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW).glow().build() };
            }
            return new ITexture[] { Casings.CLEAN_STAINLESS_STEEL.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_DISTILLATION_TOWER),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_GLOW).glow().build() };
        }
        return new ITexture[] { Casings.CLEAN_STAINLESS_STEEL.getTexture() };
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_ProcessingMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(),
                "DistillationTower.png");
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sDistillationRecipes);
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
    protected ProcessingOutputs createProcessingOutputs() {
        return new ProcessingOutputs(this)
                .setFluidOutputFilter((hatch, fluid,
                index) -> hatch.getBaseMetaTileEntity().getYCoord() == getBaseMetaTileEntity().getYCoord() + 1 + index);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        casingCount = 0;
        ForgeDirection back = getExtendedFacing().getDirection().getOpposite();
        int height = MAX_HEIGHT;
        boolean roofFound = false;
        for (int y = 1; y < MAX_HEIGHT; y++) {
            if (!base.getAirOffset(back.offsetX, y, back.offsetZ)) {
                height = y + 1;
                roofFound = true;
                break;
            }
        }
        // Scan the complete candidate even when it is unfinished, to refresh every
        // layer's hatch lists.
        boolean valid = checkPiece(piece(Math.max(MIN_HEIGHT, height)), 1, Math.max(MIN_HEIGHT, height) - 1, 0);
        if (!valid || !roofFound || height < MIN_HEIGHT || !SHELLS[height].isSatisfied(this, casingCount)
                || !BOTTOM.isSatisfied(this, 0)) {
            return false;
        }
        for (int y = 1; y < height; y++) {
            if (!OUTPUT_LAYERS[y].isSatisfied(this, 0)) {
                return false;
            }
        }
        return true;
    }

    private static String piece(int height) {
        return "tower_" + height;
    }

    private static int constructionHeight(ItemStack trigger) {
        return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT - 2, trigger.stackSize) + 2);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        int height = constructionHeight(trigger);
        buildPiece(piece(height), trigger, hintsOnly, 1, height - 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        int height = constructionHeight(trigger);
        return survivalBuildPiece(piece(height), trigger, 1, height - 1, 0, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_DistillationTower> getStructureDefinition() {
        if (structureDefinition == null) {
            StructureDefinition.Builder<GT_MetaTileEntity_DistillationTower> builder = StructureDefinition
                    .<GT_MetaTileEntity_DistillationTower>builder()
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('A', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DistillationTower.class)
                            .allow(HatchElement.Output)
                            .buildAndChain(BOTTOM, machine -> ++machine.casingCount).withHint(BOTTOM_HINT));
            for (int y = 1; y < MAX_HEIGHT; y++) {
                builder.addElement((char) ('A' + y), GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_DistillationTower.class)
                        .allow(TowerHatch.OPTIONAL_OUTPUT_BUS)
                        .buildAndChain(withCommonRequirements(OUTPUT_LAYERS[y]), machine -> ++machine.casingCount)
                        .withHint(OUTPUT_HINT));
            }
            for (int height = MIN_HEIGHT; height <= MAX_HEIGHT; height++) {
                String[][] shape = new String[height][];
                shape[height - 1] = new String[]{"A~A", "AAA", "AAA"};
                for (int y = 1; y < height; y++) {
                    String row = String.valueOf((char) ('A' + y));
                    String fullRow = row + row + row;
                    shape[height - 1 - y] = new String[]{fullRow,
                            y == height - 1 ? fullRow : row + "-" + row, fullRow};
                }
                builder.addShape(piece(height), transpose(shape));
            }
            structureDefinition = builder.build();
        }
        return structureDefinition;
    }

    private enum TowerHatch implements IHatchElement {
        FLUID_INPUT, OPTIONAL_OUTPUT_BUS;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            return this == FLUID_INPUT ? HatchElement.Input.matches(tile)
                    : tile instanceof GT_MetaTileEntity_Hatch_OutputBus;
        }

        @Override
        public boolean matchesForPlacement(IMetaTileEntity tile) {
            return this == FLUID_INPUT
                    ? tile instanceof GT_MetaTileEntity_Hatch_Input || tile instanceof GTMTE_Multi_Hatch_Input
                            || tile instanceof GTMTE_BusHatch_Input
                    : matches(tile);
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            if (this == FLUID_INPUT) {
                return machine.mInputHatches.size() + machine.mQuadrInputHatches.size()
                        + machine.mInputBusHatches.size();
            }
            return machine.mOutputBusses.size();
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return this == FLUID_INPUT ? machine.addInputToMachineList(tile, texture)
                    : machine.addOutputToMachineList(tile, texture);
        }

        @Override
        public String getDescription() {
            return StatCollector.translateToLocal(
                    this == FLUID_INPUT ? "gt.tooltip.hatch.tower_input" : "gt.tooltip.hatch.output_bus");
        }
    }

    /**
     * The same fluid hatch type has an independent requirement on every output
     * level.
     */
    private static final class LayerOutput implements IHatchElement {
        private final int layer;

        private LayerOutput(int layer) {
            this.layer = layer;
        }

        @Override
        public boolean matches(IMetaTileEntity tile) {
            return tile instanceof GT_MetaTileEntity_Hatch_Output;
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            int count = 0;
            int y = machine.getBaseMetaTileEntity().getYCoord() + layer;
            for (GT_MetaTileEntity_Hatch_Output hatch : machine.mOutputHatches) {
                if (hatch.getBaseMetaTileEntity().getYCoord() == y) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return machine.addOutputToMachineList(tile, texture);
        }

        @Override
        public String getDescription() {
            return StatCollector.translateToLocal("gt.tooltip.hatch.tower_output");
        }
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

    public boolean addOutput(FluidStack fluid, int index) {
        if (fluid == null || index < 0) {
            return false;
        }
        FluidStack[] outputs = new FluidStack[index + 1];
        outputs[index] = fluid.copy();
        return createProcessingOutputs().deliver(null, outputs, false, true);
    }

    @Override
    protected void addFluidOutputs(FluidStack[] mOutputFluids2) {
        for (int i = 0; i < mOutputFluids2.length; i++) {
            addOutput(mOutputFluids2[i], i);
        }
    }
}
