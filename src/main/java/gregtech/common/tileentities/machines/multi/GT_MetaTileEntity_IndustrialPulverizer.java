package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.enums.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import net.minecraft.world.World;
import space.impact.api.alignment.constructable.ISurvivalConstructable;
import space.impact.api.structure.AutoPlaceEnvironment;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.IStructureElement;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.structure.StructureDefinition;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

import static space.impact.api.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_IndustrialPulverizer
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_IndustrialPulverizer>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "main";
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.SOLID_STEEL, 8)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy, HatchElement.Input, HatchElement.Output)
            .build();
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
                .addMachineType("Industrial Pulverizer")
                .addInfo("Processes Macerator, Ore Washer and Chemical Bath recipes")
                .addInfo("Does not lose efficiency when overclocked")
                .beginStructureBlock(3, 3, 3, false)
                .addController("Front center")
                .addRequirements(SHELL, "Any outer casing position, except the pipe")
                .addOtherStructurePart(Casings.STEEL_GEARBOX, "Center of the structure", 1)
                .addOtherStructurePart(Casings.STEEL_PIPE, "Any of the five faces adjacent to the gearbox, except the controller", 1)
                .addStructureInfo("At least one steel pipe casing must touch the gearbox; additional pipes at these five positions are allowed.")
                .addStructureInfo("Construction keeps existing pipes, or places one in a free position next to the gearbox.")
                .addStructureInfo("Compatible IMPACT input and energy hatches are also accepted.")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Automatically place missing GregTech hatches from your inventory")
                .toolTipFinisher();
    }

	@Override
	public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive,
								 boolean aRedstone) {
		if (aSide == aFacing) {
			return new ITexture[]{
					Casings.SOLID_STEEL.getTexture(),
					TextureFactory.of(aActive ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE
							: Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR)};
		}
		return new ITexture[]{Casings.SOLID_STEEL.getTexture()};
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
	public GT_Recipe.GT_Recipe_Map getRecipeMap() {
		return GT_Recipe.GT_Recipe_Map.sIndustrialPulverizerRecipes;
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
		ItemStack[] inputs = tInputList.toArray(new ItemStack[tInputList.size()]);
		
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
		FluidStack[] fluids = tFluidList.toArray(new FluidStack[tFluidList.size()]);
		
		if (inputs.length > 0 || fluids.length > 0) {
			long voltage = getMaxInputVoltage();
			byte tier = (byte) Math.max(1, GT_Utility.getTier(voltage));
			GT_Recipe recipe = getRecipeMap().findRecipe(getBaseMetaTileEntity(), cashedRecipe, false,
					false, gregtech.api.enums.GT_Values.V[tier], fluids, inputs
			);
			if (recipe != null && recipe.isRecipeInputEqual(true, fluids, inputs)) {
				cashedRecipe = recipe;
				this.mEfficiency         = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
				this.mEfficiencyIncrease = 10000;
				
				int EUt = recipe.mEUt;
				int maxProgresstime = recipe.mDuration;
				
				while (EUt <= gregtech.api.enums.GT_Values.V[tier - 1] && maxProgresstime > 2) {
					EUt *= 4;
					maxProgresstime /= 4;
				}
				if (maxProgresstime < 2) {
					maxProgresstime = 2;
					EUt             = recipe.mEUt * recipe.mDuration / 2;
				}
				
				this.mEUt             = -EUt;
				this.mMaxProgresstime = maxProgresstime;
				mOutputItems          = new ItemStack[recipe.mOutputs.length];
				for (int i = 0; i < recipe.mOutputs.length; i++) {
					if (getBaseMetaTileEntity().getRandomNumber(10000) < recipe.getOutputChance(i)) {
						this.mOutputItems[i] = recipe.getOutput(i);
					}
				}
				this.mOutputFluids = recipe.mFluidOutputs;
				this.updateSlots();
				return true;
			}
		}
		return false;
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
                            .buildAndChain(SHELL, machine -> ++machine.casingCount);
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_IndustrialPulverizer>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CPC", "CCC"},
                            {"C~C", "PGP", "CPC"},
                            {"CCC", "CPC", "CCC"}
                    }))
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('C', shell)
                    .addElement('G', Casings.STEEL_GEARBOX.asElement())
                    .addElement('P', pipeOrShell(shell))
                    .build();
        }
        return structureDefinition;
    }

    /** Keep all five legacy pipe locations valid, but automatically build only one pipe. */
    private static IStructureElement<GT_MetaTileEntity_IndustrialPulverizer> pipeOrShell(
            IStructureElement<GT_MetaTileEntity_IndustrialPulverizer> shell) {
        IStructureElement<GT_MetaTileEntity_IndustrialPulverizer> pipe = Casings.STEEL_PIPE.asElement();
        return new IStructureElement<GT_MetaTileEntity_IndustrialPulverizer>() {
            private IStructureElement<GT_MetaTileEntity_IndustrialPulverizer> constructionElement(
                    GT_MetaTileEntity_IndustrialPulverizer machine, World world, int x, int y, int z) {
                if (isPipe(world, x, y, z)) {
                    return pipe;
                }
                int[] selected = machine.findPipePosition(world);
                return selected != null && selected[0] == x && selected[1] == y && selected[2] == z ? pipe : shell;
            }

            @Override
            public boolean check(GT_MetaTileEntity_IndustrialPulverizer machine, World world, int x, int y, int z) {
                if (isPipe(world, x, y, z)) {
                    ++machine.pipeCount;
                    return true;
                }
                return shell.check(machine, world, x, y, z);
            }

            @Override
            public boolean couldBeValid(GT_MetaTileEntity_IndustrialPulverizer machine, World world,
                                         int x, int y, int z, ItemStack trigger) {
                return isPipe(world, x, y, z) || shell.couldBeValid(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(GT_MetaTileEntity_IndustrialPulverizer machine, World world,
                                     int x, int y, int z, ItemStack trigger) {
                return constructionElement(machine, world, x, y, z).spawnHint(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(GT_MetaTileEntity_IndustrialPulverizer machine, World world,
                                      int x, int y, int z, ItemStack trigger) {
                if (couldBeValid(machine, world, x, y, z, trigger)) {
                    return true;
                }
                if (!world.isAirBlock(x, y, z)) {
                    return false;
                }
                return constructionElement(machine, world, x, y, z).placeBlock(machine, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(GT_MetaTileEntity_IndustrialPulverizer machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return constructionElement(machine, world, x, y, z)
                        .getBlocksToPlace(machine, world, x, y, z, trigger, environment);
            }

            @Override
            public PlaceResult survivalPlaceBlock(GT_MetaTileEntity_IndustrialPulverizer machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                if (couldBeValid(machine, world, x, y, z, trigger)) {
                    return PlaceResult.SKIP;
                }
                return constructionElement(machine, world, x, y, z)
                        .survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
            }
        };
    }

    private static boolean isPipe(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == Casings.STEEL_PIPE.getBlock()
                && world.getBlockMetadata(x, y, z) == Casings.STEEL_PIPE.getMetadata();
    }

    /** Read the world rather than check counters: hints must be pure and placement may span several passes. */
    private int[] findPipePosition(World world) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        ForgeDirection front = getExtendedFacing().getDirection();
        int centerX = base.getXCoord() - front.offsetX;
        int centerY = base.getYCoord();
        int centerZ = base.getZCoord() - front.offsetZ;
        int[] selected = null;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (side == front) {
                continue;
            }
            int x = centerX + side.offsetX;
            int y = centerY + side.offsetY;
            int z = centerZ + side.offsetZ;
            if (isPipe(world, x, y, z)) {
                return null;
            }
            if (selected == null && world.isAirBlock(x, y, z)) {
                selected = new int[]{x, y, z};
            }
        }
        // With a closed shell, show which casing to replace manually; never overwrite it during construction.
        return selected == null ? new int[]{centerX, centerY - 1, centerZ} : selected;
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
