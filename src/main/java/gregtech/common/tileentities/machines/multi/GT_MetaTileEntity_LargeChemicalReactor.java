package gregtech.common.tileentities.machines.multi;

import gregtech.GT_Mod;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

import static space.impact.structures.structure.StructureUtility.transpose;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine.isValidForLowGravity;

public class GT_MetaTileEntity_LargeChemicalReactor
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_LargeChemicalReactor>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.CHEMICALLY_INERT, 8)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output)
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
                .addMachineType("Large Chemical Reactor")
                .addInfo("Has the same recipes as the Chemical Reactor")
                .addInfo("Accepts fluids instead of fluid cells")
                .beginStructureBlock(3, 3, 3, false)
                .addController("Front center")
                .addRequirements(SHELL, "Any outer casing position, except the coil")
                .addOtherStructurePart(Casings.PTFE_PIPE, "Center of the structure", 1)
                .addOtherStructurePart(Casings.CUPRONICKEL_COIL,
                        "Any of the five faces adjacent to the PTFE pipe, except the controller", 1)
                .addStructureInfo("At least one cupronickel coil must touch the PTFE pipe; additional cupronickel coils at these five positions are allowed.")
                .addStructureInfo("Construction keeps existing coils, or places one in a free position next to the PTFE pipe.")
                .addStructureInfo("Compatible IMPACT input and energy hatches are also accepted.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

	@Override
	public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive,
								 boolean aRedstone) {
		if (aSide == aFacing) {
			if (aActive) return new ITexture[]{
					Casings.CHEMICALLY_INERT.getTexture(),
					TextureFactory.of(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE),
					TextureFactory.builder().addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE_GLOW).glow().build()};
			return new ITexture[]{
					Casings.CHEMICALLY_INERT.getTexture(),
					TextureFactory.of(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR),
					TextureFactory.builder().addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_GLOW).glow().build()};
		}
		return new ITexture[]{Casings.CHEMICALLY_INERT.getTexture()};
	}
	
	@Override
	public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "LargeChemicalReactor.png");
	}
	
	@Override
	public boolean isCorrectMachinePart(ItemStack aStack) {
		return true;
	}
	
	@Override
	public boolean checkRecipe(ItemStack aStack) {
		ArrayList<ItemStack> tInputList = getStoredInputs();
		int tInputList_sS = tInputList.size();
		for (int i = 0; i < tInputList_sS - 1; i++) {
			for (int j = i + 1; j < tInputList_sS; j++) {
				if (GT_Utility.areStacksEqual(tInputList.get(i), tInputList.get(j))) {
					if (tInputList.get(i).stackSize >= tInputList.get(j).stackSize) {
						tInputList.remove(j--);
						tInputList_sS = tInputList.size();
					} else {
						tInputList.remove(i--);
						tInputList_sS = tInputList.size();
						break;
					}
				}
			}
		}
		tInputList.add(mInventory[1]);
		ItemStack[] inputs = tInputList.toArray(new ItemStack[0]);
		
		ArrayList<FluidStack> tFluidList = getStoredFluids();
		int tFluidList_sS = tFluidList.size();
		for (int i = 0; i < tFluidList_sS - 1; i++) {
			for (int j = i + 1; j < tFluidList_sS; j++) {
				if (GT_Utility.areFluidsEqual(tFluidList.get(i), tFluidList.get(j))) {
					if (tFluidList.get(i).amount >= tFluidList.get(j).amount) {
						tFluidList.remove(j--);
						tFluidList_sS = tFluidList.size();
					} else {
						tFluidList.remove(i--);
						tFluidList_sS = tFluidList.size();
						break;
					}
				}
			}
		}
		FluidStack[] fluids = tFluidList.toArray(new FluidStack[0]);
		
		if (inputs.length > 0 || fluids.length > 0) {
			long voltage = getMaxInputVoltage();
			byte tier = (byte) Math.max(1, GT_Utility.getTier(voltage));
			GT_Recipe recipe = GT_Recipe.GT_Recipe_Map.sMultiblockChemicalRecipes.findRecipe(getBaseMetaTileEntity(), false,
					false, gregtech.api.enums.GT_Values.V[tier], fluids, inputs
			);
			if (recipe != null && recipe.isRecipeInputEqual(true, fluids, inputs)) {
				if (GT_Mod.gregtechproxy.mLowGravProcessing && (recipe.mSpecialValue == -100) && !isValidForLowGravity(recipe, getBaseMetaTileEntity().getWorld().provider.dimensionId)) {
					return false;
				}
				if (recipe.mSpecialValue == -200 && (mCleanroom == null || mCleanroom.mEfficiency == 0)) {
					return false;
				}
				this.mEfficiency         = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
				this.mEfficiencyIncrease = 10000;
				
				this.mEfficiency         = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
				this.mEfficiencyIncrease = 10000;
				calculateOverclockedNessMulti(recipe.mEUt, recipe.mDuration, 1, voltage);
				//In case recipe is too OP for that machine
				if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
					return false;
				if (this.mEUt > 0) {
					this.mEUt = (-this.mEUt);
				}
				this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
				this.mOutputItems     = recipe.mOutputs;
				this.mOutputFluids    = recipe.mFluidOutputs;
				this.updateSlots();
				return true;
			}
		}
		return false;
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
                            .buildAndChain(SHELL, machine -> ++machine.casingCount);
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
