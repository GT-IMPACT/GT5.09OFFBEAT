package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.HeatingCoil;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.ProcessingOutputs;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static space.impact.structures.structure.StructureUtility.transpose;
import static gregtech.api.enums.Textures.BlockIcons.*;

public class GT_MetaTileEntity_ElectricBlastFurnace
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_ElectricBlastFurnace>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final HintColor BOTTOM_HINT = HintColor.GREEN;
    private static final HintColor RECOVERY_HINT = HintColor.ORANGE;
    private static final HintColor MUFFLER_HINT = HintColor.RED;
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
    private FluidStack[] pollutionFluidStacks = new FluidStack[] { Materials.CarbonDioxide.getGas(1000),
            Materials.CarbonMonoxide.getGas(1000), Materials.SulfurDioxide.getGas(1000) };

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
                .addSubChannel(GTStructureChannels.COIL, "Coil tier from 1 to 10; existing coils determine the tier when completing a structure")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Places maintenance, energy, inputs and outputs in the bottom layer, and the top muffler. Optional top gas outputs are installed manually.")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex,
            boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive) {
                return new ITexture[] { Casings.HEAT_PROOF.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE), TextureFactory.builder()
                                .addIcon(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW).glow().build() };
            }
            return new ITexture[] { Casings.HEAT_PROOF.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW).glow().build() };
        }
        return new ITexture[] { Casings.HEAT_PROOF.getTexture() };
    }

    @Override
    public List<GT_Recipe.GT_Recipe_Map> getRecipeMaps() {
        return Collections.singletonList(GT_Recipe.GT_Recipe_Map.sBlastRecipes);
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(this::getRecipeMap)
                .setOverclock(2, 4).setCalculatorFactory(
                recipe -> new OverclockCalculator().setHeat(recipe.mSpecialValue, mHeatingCapacity, true, true));
    }

    @Override
    protected ProcessingOutputs createProcessingOutputs() {
        int recovery = getGasRecoveryPercentage();
        return new ProcessingOutputs(this)
                .setFluidOutputTransform(fluids -> recoverPollutionGases(fluids, recovery))
                .setFluidOutputFilter((hatch, fluid, index) -> hatch.getBaseMetaTileEntity()
                        .getYCoord() == getBaseMetaTileEntity().getYCoord() + (isPollutionGas(fluid) ? 3 : 0));
    }

    private boolean isPollutionGas(FluidStack fluid) {
        for (FluidStack pollution : pollutionFluidStacks) {
            if (fluid.isFluidEqual(pollution)) {
                return true;
            }
        }
        return false;
    }

    private int getGasRecoveryPercentage() {
        for (GT_MetaTileEntity_Hatch_Muffler hatch : mMufflerHatches) {
            if (isValidMetaTileEntity(hatch)) {
                return 105 - hatch.calculatePollutionReduction(100);
            }
        }
        return 5;
    }

    private FluidStack[] recoverPollutionGases(FluidStack[] fluids, int recovery) {
        if (fluids != null) {
            for (FluidStack fluid : fluids) {
                if (fluid != null && isPollutionGas(fluid)) {
                    long amount = (long) fluid.amount * recovery / 100;
                    if (amount < 0 || amount > Integer.MAX_VALUE) {
                        throw new ProcessingLogic.OutputOverflowException();
                    }
                    fluid.amount = (int) amount;
                }
            }
        }
        return fluids;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        mHeatingCapacity = 0;
        checkedCoil = null;
        bottomOutputs = 0;
        if (!checkPiece(STRUCTURE_PIECE, 1, 3, 0) || checkedCoil == null || !BOTTOM.isSatisfied(this, 0)
                || !EXHAUST.isSatisfied(this, 0)) {
            return false;
        }
        mHeatingCapacity = checkedCoil.getHeatCapacity() + 100 * (GT_Utility.getTier(getTotalInputVoltage()) - 2);
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
                    .addChannel(GTStructureChannels.COIL, 1, 10)
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
        return HeatingCoil.fromTier(GTStructureChannels.getValue(trigger, GTStructureChannels.COIL));
    }

    private enum BlastHatch implements IHatchElement {
        BOTTOM_OUTPUT, MUFFLER;

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
                    if (tUsedMeta >= 12 && tUsedMeta <= 14
                            && aBaseMetaTileEntity.getBlock(xPos, yPos, zPos) == GregTech_API.sBlockCasings1) {
                        aBaseMetaTileEntity.getWorld().setBlock(xPos, yPos, zPos, GregTech_API.sBlockCasings5,
                                tUsedMeta - 12, 3);
                    }
                }
            }
        }
    }

    @Override
    public boolean addOutput(FluidStack fluid) {
        if (fluid == null) {
            return false;
        }
        ProcessingOutputs outputs = createProcessingOutputs();
        return outputs.deliver(null, outputs.prepareFluids(new FluidStack[] { fluid.copy() }), false, true);
    }

    @Override
    public String[] addInfoData() {
        final ArrayList<String> ll = new ArrayList<>();
        ll.add(StatCollector.translateToLocal("GT5U.EBF.heat") + ": " + EnumChatFormatting.GREEN + mHeatingCapacity
                + EnumChatFormatting.RESET + " K");
        final String[] a = new String[ll.size()];
        return ll.toArray(a);
    }
}
