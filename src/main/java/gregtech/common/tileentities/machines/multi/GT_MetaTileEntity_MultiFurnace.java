package gregtech.common.tileentities.machines.multi;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.HeatingCoil;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.SmeltingProcessingLogic;
import gregtech.api.gui.GT_GUIContainer_ProcessingMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTStructureChannels;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import net.minecraft.world.World;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import java.util.ArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

public class GT_MetaTileEntity_MultiFurnace
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_MultiFurnace>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor BOTTOM_HINT = HintColor.GREEN;
    private static final HintColor MUFFLER_HINT = HintColor.ORANGE;
    private static final StructureRequirements BOTTOM = StructureRequirements.builder(Casings.HEAT_PROOF, 0)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, FurnaceHatch.INPUT_BUS, FurnaceHatch.OUTPUT_BUS)
            .build();
    private static final StructureRequirements EXHAUST = StructureRequirements.builder(Casings.HEAT_PROOF, 0)
            .atLeast(FurnaceHatch.MUFFLER).build();
    private static IStructureDefinition<GT_MetaTileEntity_MultiFurnace> structureDefinition;
    private HeatingCoil checkedCoil;
    private int mLevel = 0;
    private int mCostDiscount = 1;

    public GT_MetaTileEntity_MultiFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_MultiFurnace(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_MultiFurnace(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Multi Smelter")
                .addInfo("Smelts 8 to 256 items at once, depending on the heating coils")
                .addInfo("Higher coil tiers increase parallel processing or reduce energy consumption")
                .addPollutionAmount(getPollutionPerTick(null) * 20)
                .beginStructureBlock(3, 3, 3, true)
                .addController("Front center of the bottom layer")
                .addCasingInfoMin(Casings.HEAT_PROOF, 8, false)
                .addStructureInfo("8 fixed casings in the top layer; unused hatch positions in the bottom layer also require heat-proof casings")
                .addRequirements(BOTTOM, "Bottom layer, replacing heat-proof casings", BOTTOM_HINT)
                .addRequirements(EXHAUST, "Top center", MUFFLER_HINT)
                .addOtherStructurePart(8, "Heating Coils", "Middle layer, surrounding the empty center")
                .addAir("Center of the middle layer")
                .addStructureInfo("All eight heating coils must have the same tier.")
                .addStructureInfo("Item input and output buses are required; IMPACT combined inputs and multi-amp energy hatches are accepted.")
                .addSubChannel(GTStructureChannels.COIL, "Coil tier from 1 to 10; existing coils determine the tier when completing a structure")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide != aFacing) return new ITexture[]{Casings.HEAT_PROOF.getTexture()};
        if (aActive) return new ITexture[]{
                Casings.HEAT_PROOF.getTexture(),
                TextureFactory.of(OVERLAY_FRONT_MULTI_SMELTER_ACTIVE),
                TextureFactory.builder().addIcon(OVERLAY_FRONT_MULTI_SMELTER_ACTIVE_GLOW).glow().build()};
        return new ITexture[]{
                Casings.HEAT_PROOF.getTexture(),
                TextureFactory.of(OVERLAY_FRONT_MULTI_SMELTER),
                TextureFactory.builder().addIcon(OVERLAY_FRONT_MULTI_SMELTER_GLOW).glow().build()};
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_ProcessingMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "MultiFurnace.png");
    }

    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sFurnaceRecipes;
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new SmeltingProcessingLogic(() -> mLevel, () -> mCostDiscount);
    }

    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.BATCH | ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.SEPARATE_INPUTS;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        checkedCoil = null;
        mLevel = 0;
        mCostDiscount = 1;
        if (!checkPiece(STRUCTURE_PIECE, 1, 2, 0) || checkedCoil == null
                || !BOTTOM.isSatisfied(this, 0) || !EXHAUST.isSatisfied(this, 0)) {
            return false;
        }
        mLevel = checkedCoil.getSmeltingLevel();
        mCostDiscount = checkedCoil.getSmeltingDiscount();
        if (base.isServerSide()) {
            replaceDeprecatedCoils(base);
        }
        return true;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 1, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 1, 2, 0, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_MultiFurnace> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_MultiFurnace>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CMC", "CCC"},
                            {"HHH", "H-H", "HHH"},
                            {"B~B", "BBB", "BBB"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addChannel(GTStructureChannels.COIL, 1, 10)
                    .addElement('C', Casings.HEAT_PROOF.asElement())
                    .addElement('B', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_MultiFurnace.class)
                            .buildAndChain(BOTTOM).withHint(BOTTOM_HINT))
                    .addElement('M', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_MultiFurnace.class)
                            .hint(MUFFLER_HINT).build(EXHAUST))
                    .addElement('H', GTStructureUtility.ofHeatingCoil(
                            GT_MetaTileEntity_MultiFurnace::checkCoil, GT_MetaTileEntity_MultiFurnace::constructionCoil))
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

    /** Pure world lookup, shared by previews, conflict checks and construction. */
    private HeatingCoil constructionCoil(ItemStack trigger) {
        World world = getBaseMetaTileEntity().getWorld();
        IGregTechTileEntity base = getBaseMetaTileEntity();
        ForgeDirection front = getExtendedFacing().getDirection();
        int centerX = base.getXCoord() - front.offsetX;
        int centerZ = base.getZCoord() - front.offsetZ;
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                if (x == centerX && z == centerZ) {
                    continue;
                }
                HeatingCoil coil = HeatingCoil.at(world, x, base.getYCoord() + 1, z);
                if (coil != null) {
                    return coil;
                }
            }
        }
        int tier = GTStructureChannels.getValue(trigger, GTStructureChannels.COIL);
        return HeatingCoil.fromTier(tier);
    }

    private enum FurnaceHatch implements IHatchElement {
        INPUT_BUS,
        OUTPUT_BUS,
        MUFFLER;

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
                tUsedMeta = aBaseMetaTileEntity.getMetaID(xPos, tY + 1, zPos);
                if (tUsedMeta >= 12 && tUsedMeta <= 14 && aBaseMetaTileEntity.getBlock(xPos, tY + 1, zPos) == GregTech_API.sBlockCasings1) {
                    aBaseMetaTileEntity.getWorld().setBlock(xPos, tY + 1, zPos, GregTech_API.sBlockCasings5, tUsedMeta - 12, 3);
                }
            }
        }
    }

    @Override
    public String[] addInfoData() {
        final ArrayList<String> ll = new ArrayList<>();
        ll.add(StatCollector.translateToLocal("GT5U.MS.multismelting") + ": " +
            EnumChatFormatting.GREEN + mLevel * 8 + EnumChatFormatting.RESET
            + " Discount: (EU/t) / " + EnumChatFormatting.GREEN + mCostDiscount
            + EnumChatFormatting.RESET);
        final String[] a = new String[ll.size()];
        return ll.toArray(a);
    }
}
