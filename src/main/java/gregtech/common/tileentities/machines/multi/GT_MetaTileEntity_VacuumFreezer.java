package gregtech.common.tileentities.machines.multi;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HatchElement;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.*;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

public class GT_MetaTileEntity_VacuumFreezer
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_VacuumFreezer>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.FROST_PROOF, 16)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output).build();
    private static IStructureDefinition<GT_MetaTileEntity_VacuumFreezer> structureDefinition;
    private int casingCount;

    public GT_MetaTileEntity_VacuumFreezer(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_VacuumFreezer(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_VacuumFreezer(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Vacuum Freezer")
                .addInfo("Super cools hot ingots and cells")
                .beginStructureBlock(3, 3, 3, true)
                .addController("Front face, centered")
                .addRequirements(SHELL, "Any casing position")
                .addAir("Center of the structure")
                .addStructureInfo("Compatible IMPACT input and energy hatches are also accepted.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        ITexture[] rTexture;
        if (aSide == aFacing) {
            if (aActive) {
                rTexture = new ITexture[]{
                        Casings.FROST_PROOF.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_VACUUM_FREEZER_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_VACUUM_FREEZER_ACTIVE_GLOW).glow().build()};
            } else {
                rTexture = new ITexture[]{
                        Casings.FROST_PROOF.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_VACUUM_FREEZER),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_VACUUM_FREEZER_GLOW).glow().build()};
            }
        } else {
            rTexture = new ITexture[]{Casings.FROST_PROOF.getTexture()};
        }
        return rTexture;
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "VacuumFreezer.png");
    }

    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sVacuumRecipes;
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }


    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<ItemStack> tInputList = getStoredInputs();
        ArrayList<FluidStack> tFluidList = getStoredFluids();
        FluidStack[] tFluids = tFluidList.toArray(new FluidStack[tFluidList.size()]);
        ItemStack[] tInput = tInputList.toArray(new ItemStack[tInputList.size()]);
        if (tInputList.size() > 0 || tFluids.length > 0) {
            long tVoltage = getMaxInputVoltage();
            byte tTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));
            GT_Recipe tRecipe = getRecipeMap().findRecipe(getBaseMetaTileEntity(), cashedRecipe,
                    false, false, GT_Values.V[tTier], tFluids, tInput);
            if (tRecipe != null) {
                if (tRecipe.isRecipeInputEqual(true, tFluids, tInput)) {
                    cashedRecipe = tRecipe;
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
                    this.mOutputItems = new ItemStack[]{tRecipe.getOutput(0)};
                    this.mOutputFluids = tRecipe.mFluidOutputs;
                    updateSlots();
                    return true;
                }
            }
        }
        return false;
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
    public IStructureDefinition<GT_MetaTileEntity_VacuumFreezer> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_VacuumFreezer>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CCC", "CCC"},
                            {"C~C", "C-C", "CCC"},
                            {"CCC", "CCC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_VacuumFreezer.class)
                            .buildAndChain(SHELL, machine -> ++machine.casingCount))
                    .build();
        }
        return structureDefinition;
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
}
