package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.AirFilterProcessingLogic;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.metatileentity.implementations.GTMTE_BusHatch_Input;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Log;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import gregtech.common.GT_Pollution;
import gregtech.common.items.GT_MetaGenerated_Tool_01;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import java.util.ArrayList;

import static gregtech.api.enums.GT_Values.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static space.impact.structures.structure.StructureUtility.transpose;

/**
 * Created by danie_000 on 03.10.2016.
 */
public class GT_MetaTileEntity_AirFilter
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_AirFilter>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor BOTTOM_HINT = HintColor.GREEN;
    private static final HintColor MUFFLER_HINT = HintColor.ORANGE;
    private static final StructureRequirements BOTTOM = StructureRequirements
            .builder(Casings.AIR_FILTER_TURBINE, 0)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, FilterHatch.INPUT_BUS, FilterHatch.OUTPUT_BUS)
            .build();
    private static final StructureRequirements MUFFLERS = StructureRequirements
            .builder(Casings.AIR_FILTER_TURBINE, 0)
            .atLeast(FilterHatch.MUFFLER)
            .build();
    private static IStructureDefinition<GT_MetaTileEntity_AirFilter> structureDefinition;
    protected int mPollutionReduction=0;
    protected int baseEff = 2500*4;
    protected boolean hasPollution=false;
    static final GT_Recipe tRecipe= new GT_Recipe(
            new ItemStack[]{ItemList.AdsorptionFilter.get(1L, new Object())},
            new ItemStack[]{ItemList.AdsorptionFilterDirty.get(1L,new Object())},
            null, null, null, null, 1, 1, 0);

    public GT_MetaTileEntity_AirFilter(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_AirFilter(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_AirFilter(mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Electric Air Filter")
                .addInfo("Reduces pollution in the surrounding 3 x 3 chunks")
                .addInfo("Adsorption filters double pollution removal; a turbine in the controller can improve efficiency")
                .addInfo("Maximum useful muffler tier is twice the machine tier")
                .addInfo("Starts cleaning at 10000 total pollution and stops below 1000")
                .beginStructureBlock(3, 4, 3, true)
                .addController("Front center of the bottom layer")
                .addRequirements(BOTTOM, "Any bottom layer casing position", BOTTOM_HINT)
                .addRequirements(MUFFLERS, "Side centers of the two middle layers; 1 to 8 mufflers", MUFFLER_HINT)
                .addOtherStructurePart(Casings.AIR_FILTER_VENT, "Corners of the two middle layers", 8)
                .addOtherStructurePart(Casings.AIR_FILTER_TURBINE, "Entire roof", 9)
                .addAir("Center of both middle layers")
                .addAir("Two full 3 x 3 layers above the roof")
                .addAir("One block outward from all four sides of each middle layer, including the vents")
                .addStructureInfo("Unused bottom hatch and middle muffler positions require Air Filter Turbine Casings.")
                .addStructureInfo("Item input and output buses are required; fluid-only hatches do not satisfy these requirements.")
                .addStructureInfo("IMPACT combined item/fluid inputs and multi-amp energy hatches are accepted.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Places maintenance, energy and filter input/output buses in the base, and the muffler at its dedicated position.")
                .toolTipFinisher();
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[]{Casings.SOLID_STEEL.getTexture(), TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_DIESEL_ENGINE_ACTIVE : Textures.BlockIcons.OVERLAY_FRONT_DIESEL_ENGINE)};
        }
        return new ITexture[]{Casings.SOLID_STEEL.getTexture()};
    }
    
    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new AirFilterProcessingLogic(this::filtrationState, () -> tRecipe);
    }

    @Override
    public int getSupportedProcessingModes() {
        return ProcessingLogic.PROTECT_ITEMS;
    }

    @Override
    public boolean hasControllerSlotInGui() {
        return true;
    }

    private AirFilterProcessingLogic.State filtrationState() {
        long voltage = getTotalInputVoltage();
        int tier = Math.max(1, GT_Utility.getTier(voltage));
        long reduction = 0;
        for (GT_MetaTileEntity_Hatch_Muffler hatch : mMufflerHatches) {
            if (isValidMetaTileEntity(hatch)) {
                reduction += Math.min(tier * 2, hatch.mTier) * 300L;
            }
        }
        ItemStack tool = mInventory[1];
        ItemStack before = tool == null ? null : tool.copy();
        int multiplier = 10000;
        if (isFiltrationTool(tool)) {
            GT_MetaGenerated_Tool item = (GT_MetaGenerated_Tool) tool.getItem();
            multiplier = Math.multiplyExact(GT_Utility.safeInt((long)
                    ((50.0F + 10.0F * item.getToolCombatDamage(tool)) * 100)), 4);
        }
        return new AirFilterProcessingLogic.State(hasPollution, voltage, reduction, multiplier,
                () -> mInventory[1] == tool && ItemStack.areItemStacksEqual(before, tool));
    }

    private boolean isFiltrationTool(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof GT_MetaGenerated_Tool_01)) {
            return false;
        }
        GT_MetaGenerated_Tool tool = (GT_MetaGenerated_Tool) stack.getItem();
        return tool.getToolStats(stack).getSpeedMultiplier() > 0
                && GT_MetaGenerated_Tool.getPrimaryMaterial(stack).mToolSpeed > 0;
    }

    @Override
    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        super.startCycle(result, efficiency);
        AirFilterProcessingLogic logic = (AirFilterProcessingLogic) getProcessingLogic();
        mPollutionReduction = logic.getReduction();
        baseEff = logic.getToolEfficiency();
        if (!hasPollution) {
            return;
        }
        Chunk center = getBaseMetaTileEntity().getWorld().getChunkFromBlockCoords(
                getBaseMetaTileEntity().getXCoord(), getBaseMetaTileEntity().getZCoord());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Chunk chunk = getBaseMetaTileEntity().getWorld().getChunkFromChunkCoords(
                        center.xPosition + x, center.zPosition + z);
                GT_Pollution.addPollution(chunk, -mPollutionReduction);
            }
        }
        ItemStack tool = mInventory[1];
        if (isFiltrationTool(tool)) {
            ((GT_MetaGenerated_Tool) tool.getItem()).doDamage(tool, 50L);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("airFilterReduction", mPollutionReduction);
        tag.setInteger("airFilterToolEfficiency", baseEff);
        tag.setBoolean("airFilterHasPollution", hasPollution);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        mPollutionReduction = tag.getInteger("airFilterReduction");
        baseEff = tag.hasKey("airFilterToolEfficiency") ? tag.getInteger("airFilterToolEfficiency") : 10000;
        hasPollution = tag.getBoolean("airFilterHasPollution");
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        return checkPiece(STRUCTURE_PIECE, 2, 5, 1) && BOTTOM.isSatisfied(this, 0) && MUFFLERS.isSatisfied(this, 0);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, 2, 5, 1);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        return survivalBuildPiece(STRUCTURE_PIECE, trigger, 2, 5, 1, elementBudget, environment);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_AirFilter> getStructureDefinition() {
        if (structureDefinition == null) {
            // Include all mandatory air clearances in the same piece used by survival's conflict scan.
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_AirFilter>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"     ", " --- ", " --- ", " --- ", "     "},
                            {"     ", " --- ", " --- ", " --- ", "     "},
                            {"     ", " CCC ", " CCC ", " CCC ", "     "},
                            {" --- ", "-VMV-", "-M-M-", "-VMV-", " --- "},
                            {" --- ", "-VMV-", "-M-M-", "-VMV-", " --- "},
                            {"     ", " B~B ", " BBB ", " BBB ", "     "}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', Casings.AIR_FILTER_TURBINE.asElement())
                    .addElement('V', Casings.AIR_FILTER_VENT.asElement())
                    .addElement('B', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AirFilter.class)
                            .buildAndChain(BOTTOM)
                            .withHint(BOTTOM_HINT))
                    .addElement('M', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AirFilter.class)
                            .hatchFacing(GT_MetaTileEntity_AirFilter::mufflerFacing)
                            .buildAndChain(MUFFLERS)
                            .withHint(MUFFLER_HINT))
                    .build();
        }
        return structureDefinition;
    }

    private ForgeDirection mufflerFacing(IGregTechTileEntity hatch) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        ForgeDirection back = getExtendedFacing().getDirection().getOpposite();
        int dx = hatch.getXCoord() - base.getXCoord() - back.offsetX;
        int dz = hatch.getZCoord() - base.getZCoord() - back.offsetZ;
        if (dx != 0) {
            return dx < 0 ? ForgeDirection.WEST : ForgeDirection.EAST;
        }
        return dz < 0 ? ForgeDirection.NORTH : ForgeDirection.SOUTH;
    }

    private enum FilterHatch implements IHatchElement {
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

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
            if (mMachine && aTick % 200L == 0L) {
                int pollution = 0;
                Chunk tChunk = getBaseMetaTileEntity().getWorld().getChunkFromBlockCoords(getBaseMetaTileEntity().getXCoord(), getBaseMetaTileEntity().getZCoord());
                int xChunk = tChunk.xPosition;
                int zChunk = tChunk.zPosition;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        tChunk = getBaseMetaTileEntity().getWorld().getChunkFromChunkCoords(xChunk + i, zChunk + j);
                        pollution += GT_Pollution.getPollution(tChunk);
                    }
                }
                //check for pollution
                hasPollution = pollution >= 10000 || (hasPollution && pollution >= 1000);
            }
        } else if (aTick % 200L == 0L) {
            //refresh casing on state change
            int Xstart = aBaseMetaTileEntity.getXCoord();
            int Ystart = aBaseMetaTileEntity.getYCoord();
            int Zstart = aBaseMetaTileEntity.getZCoord();
            final int RANGE = 32;
            try {
                aBaseMetaTileEntity.getWorld().markBlockRangeForRenderUpdate(
                        Xstart - RANGE,
                        Ystart,
                        Zstart - RANGE,
                        Xstart + RANGE,
                        Ystart,
                        Zstart + RANGE);
            } catch (Exception e) {
                GT_Log.out.print(e);
            }
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 0;//set to zero since it will directly affect pollution in chunk
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        try{
            if(aStack.getItem() instanceof GT_MetaGenerated_Tool_01 &&
                    ((GT_MetaGenerated_Tool) aStack.getItem()).getToolStats(aStack).getSpeedMultiplier()>0 &&
                    ((GT_MetaGenerated_Tool) aStack.getItem()).getPrimaryMaterial(aStack).mToolSpeed>0 ) {
                return 10;
            }
        }catch (Exception e){/**/}
        return 0;
    }

    public int getAmountOfOutputs() {
        return 1;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    @Override
    public String[] addInfoData() {
        final ArrayList<String> ll = new ArrayList<>();
        ll.add("Pollution reduction: " + EnumChatFormatting.GREEN + mPollutionReduction
                + EnumChatFormatting.RESET + " gibbl/10s (for Chunk)");
        final String[] a = new String[ll.size()];
        return ll.toArray(a);
    }
}
