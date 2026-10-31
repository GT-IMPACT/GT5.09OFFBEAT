package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HatchElement;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import ic2.core.block.BlockIC2Door;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.alignment.constructable.ISurvivalConstructable;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.AutoPlaceEnvironment;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.IStructureElement;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.structure.StructureDefinition;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static gregtech.api.enums.GT_Values.VN;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.api.structure.StructureUtility.onElementPass;
import static space.impact.api.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_Cleanroom
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_Cleanroom>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "room";
    private static final HintColor HATCH_HINT = HintColor.BLUE;
    private static final StructureRequirements SHELL = StructureRequirements.builder(Casings.PLASCRETE, 20)
            .atLeast(HatchElement.Maintenance, HatchElement.Energy)
            .build();
    private static final Map<String, IStructureDefinition<GT_MetaTileEntity_Cleanroom>> DEFINITIONS = new ConcurrentHashMap<>();
    private final Set<MetaTileEntity> attachedMachines = new HashSet<>();
    private RoomSize structureSize = new RoomSize(1, 4, 1);
    private RoomSize buildingSize;
    private int plascreteCount;
    private int glassCount;
    private int hullCount;
    private int doorCount;
    private boolean openDoor;


    public GT_MetaTileEntity_Cleanroom(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_Cleanroom(String aName) {
        super(aName);
    }

	@Override
	public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
		return new GT_MetaTileEntity_Cleanroom(mName);
	}

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Cleanroom")
                .addInfo("Power: 40 EU/t initially, 4 EU/t at full cleanliness")
                .addInfo("Keep reinforced doors closed to maintain cleanliness")
                .beginVariableStructureBlock(3, 15, 4, 15, 3, 15, true)
                .addController("Roof center")
                .addRequirements(SHELL, "Walls and floor, except the frame", HATCH_HINT)
                .addStructureInfo("Frame: plascrete; roof interior: filter casings")
                .addStructureInfo("Up to 2 reinforced doors and 20 hulls / diode cables")
                .addStructureInfo("Glass: up to %s%% of the plascrete count", GT_Values.cleanroomGlass)
                .addStructureInfo("Exactly one maintenance and one energy hatch")
                .addSubChannel("width", "Width = 2 x value + 1")
                .addSubChannel("depth", "Depth = 2 x value + 1")
                .addSubChannel("height", "Height = value + 3")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Auto-place required hatches")
                .toolTipFinisher();
    }

	@Override
	public boolean checkRecipe(ItemStack aStack) {
    	int tierHatch = 0;
    	for (GT_MetaTileEntity_Hatch_Energy tHatch : mEnergyHatches) tierHatch = tHatch.mTier;
    	for (GT_MetaTileEntity_Hatch_EnergyMulti tHatch : mEnergyHatchesMulti) tierHatch = tHatch.mTier;
		mEfficiencyIncrease = 100 * tierHatch;
		mMaxProgresstime = 100;
		mEUt = -4;
		return true;
	}

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        mUpdate = 100;
        plascreteCount = 0;
        glassCount = 0;
        hullCount = 0;
        doorCount = 0;
        openDoor = false;
        detachMachines();
        RoomSize detected = buildingSize == null ? detectSize() : buildingSize;
        if (detected == null) {
            updateRoomRedstone(false);
            return false;
        }
        structureSize = detected;
        boolean valid = checkPiece(STRUCTURE_PIECE, detected.halfWidth, 0, detected.halfDepth);
        int glassLimit = (int) Math.floor(plascreteCount * (double) GT_Values.cleanroomGlass / 100);
        valid &= SHELL.isSatisfied(this, plascreteCount) && mMaintenanceHatches.size() == 1
                && mEnergyHatches.size() + mEnergyHatchesMulti.size() == 1
                && doorCount <= 2 && hullCount <= 20 && glassCount <= glassLimit;
        if (valid) {
            attachInteriorMachines(detected);
            if (openDoor) {
                mEfficiency = Math.max(0, mEfficiency - 200);
            }
        }
        updateRoomRedstone(valid);
        return valid;
    }

    private RoomSize detectSize() {
        int halfWidth = detectRoofEdge(true);
        int halfDepth = detectRoofEdge(false);
        if (halfWidth == 0 || halfDepth == 0) {
            return null;
        }
        // Follow a solid corner down to the floor, as in the original room detector.
        for (int below = 1; below <= 15; below++) {
            int[] p = worldPosition(halfWidth, below, halfDepth);
            if (!matches(Casings.PLASCRETE, getBaseMetaTileEntity().getWorld(), p[0], p[1], p[2])) {
                return below >= 4 ? new RoomSize(halfWidth, below, halfDepth) : null;
            }
        }
        return null;
    }

    private int detectRoofEdge(boolean width) {
        World world = getBaseMetaTileEntity().getWorld();
        for (int radius = 1; radius <= 7; radius++) {
            int[] p = worldPosition(width ? radius : 0, 0, width ? 0 : radius);
            if (matches(Casings.PLASCRETE, world, p[0], p[1], p[2])) {
                return radius;
            }
            if (!matches(Casings.FILTER, world, p[0], p[1], p[2])) {
                return 0;
            }
        }
        return 0;
    }

    private int[] worldPosition(int a, int b, int c) {
        int[] p = new int[3];
        getExtendedFacing().getWorldOffset(new int[]{a, b, c}, p);
        IGregTechTileEntity base = getBaseMetaTileEntity();
        p[0] += base.getXCoord();
        p[1] += base.getYCoord();
        p[2] += base.getZCoord();
        return p;
    }

    private int[] relativePosition(int x, int y, int z) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        int[] p = new int[3];
        getExtendedFacing().getOffsetABC(
                new int[]{x - base.getXCoord(), y - base.getYCoord(), z - base.getZCoord()}, p);
        return p;
    }

    private static RoomSize constructionSize(ItemStack trigger) {
        int width = Math.max(1, Math.min(7, ChannelDataAccessor.getChannelData(trigger, "width")));
        int depth = Math.max(1, Math.min(7, ChannelDataAccessor.getChannelData(trigger, "depth")));
        int height = Math.max(1, Math.min(12, ChannelDataAccessor.getChannelData(trigger, "height"))) + 3;
        return new RoomSize(width, height, depth);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        structureSize = constructionSize(trigger);
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, structureSize.halfWidth, 0, structureSize.halfDepth);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        buildingSize = constructionSize(trigger);
        structureSize = buildingSize;
        try {
            return survivalBuildPiece(STRUCTURE_PIECE, trigger, buildingSize.halfWidth, 0, buildingSize.halfDepth,
                    elementBudget, environment);
        } finally {
            buildingSize = null;
        }
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_Cleanroom> getStructureDefinition() {
        RoomSize size = structureSize;
        return DEFINITIONS.computeIfAbsent(size.key(), ignored -> {
            String[][] shape = new String[size.height][2 * size.halfDepth + 1];
            for (int b = 0; b < size.height; b++) {
                for (int c = -size.halfDepth; c <= size.halfDepth; c++) {
                    StringBuilder row = new StringBuilder();
                    for (int a = -size.halfWidth; a <= size.halfWidth; a++) {
                        boolean widthEdge = Math.abs(a) == size.halfWidth;
                        boolean depthEdge = Math.abs(c) == size.halfDepth;
                        if (b == 0) {
                            row.append(widthEdge || depthEdge ? 'P' : a == 0 && c == 0 ? '~' : 'F');
                        } else if (widthEdge && depthEdge) {
                            row.append('P');
                        } else if (widthEdge || depthEdge || b == size.height - 1) {
                            row.append('W');
                        } else {
                            // Occupied interior is intentional: machines, pipes and cables are allowed.
                            row.append(' ');
                        }
                    }
                    shape[b][c + size.halfDepth] = row.toString();
                }
            }
            return StructureDefinition.<GT_MetaTileEntity_Cleanroom>builder()
                    .addShape(STRUCTURE_PIECE, transpose(shape))
                    .addChannel("width", 1, 7)
                    .addChannel("depth", 1, 7)
                    .addChannel("height", 1, 12)
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('P', onElementPass(machine -> ++machine.plascreteCount, Casings.PLASCRETE.asElement()))
                    .addElement('F', Casings.FILTER.asElement())
                    .addElement('W', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_Cleanroom.class)
                            .hatchFacing((machine, tile) -> machine.outwardFacing(size, tile))
                            .buildAndChain(SHELL, shellElement(size), machine -> {
                            })
                            .withHint(HATCH_HINT))
                    .build();
        });
    }

    private ForgeDirection outwardFacing(RoomSize size, IGregTechTileEntity tile) {
        int[] local = relativePosition(tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        if (local[1] == size.height - 1) {
            return ForgeDirection.DOWN;
        }
        if (Math.abs(local[0]) == size.halfWidth) {
            ForgeDirection left = getExtendedFacing().getRelativeLeftInWorld();
            return local[0] > 0 ? left : left.getOpposite();
        }
        ForgeDirection back = getExtendedFacing().getRelativeBackInWorld();
        return local[2] > 0 ? back : back.getOpposite();
    }

    private static boolean matches(ICasing casing, World world, int x, int y, int z) {
        return world.blockExists(x, y, z) && world.getBlock(x, y, z) == casing.getBlock()
                && world.getBlockMetadata(x, y, z) == casing.getMetadata();
    }

    private static IStructureElement<GT_MetaTileEntity_Cleanroom> shellElement(RoomSize size) {
        IStructureElement<GT_MetaTileEntity_Cleanroom> casing = Casings.PLASCRETE.asElement();
        return new IStructureElement<GT_MetaTileEntity_Cleanroom>() {
            @Override
            public boolean check(GT_MetaTileEntity_Cleanroom machine, World world, int x, int y, int z) {
                if (matches(Casings.PLASCRETE, world, x, y, z)) {
                    machine.plascreteCount++;
                } else if (matches(Casings.CLEANROOM_GLASS, world, x, y, z)) {
                    machine.glassCount++;
                } else if (isHull(world, x, y, z)) {
                    machine.hullCount++;
                } else if (machine.isDoor(size, world, x, y, z)) {
                    int meta = world.getBlockMetadata(x, y, z);
                    if ((meta & 8) == 0) {
                        machine.doorCount++;
                        int[] local = machine.relativePosition(x, y, z);
                        ForgeDirection normal = Math.abs(local[0]) == size.halfWidth
                                ? machine.getExtendedFacing().getRelativeLeftInWorld()
                                : machine.getExtendedFacing().getRelativeBackInWorld();
                        machine.openDoor |= (normal.offsetX != 0 == ((meta & 1) != 0)) != ((meta & 4) != 0);
                    }
                } else {
                    return false;
                }
                return true;
            }

            @Override
            public boolean couldBeValid(GT_MetaTileEntity_Cleanroom machine, World world,
                                         int x, int y, int z, ItemStack trigger) {
                return matches(Casings.PLASCRETE, world, x, y, z)
                        || matches(Casings.CLEANROOM_GLASS, world, x, y, z)
                        || isHull(world, x, y, z) || machine.isDoor(size, world, x, y, z);
            }

            @Override
            public boolean spawnHint(GT_MetaTileEntity_Cleanroom machine, World world,
                                     int x, int y, int z, ItemStack trigger) {
                return casing.spawnHint(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(GT_MetaTileEntity_Cleanroom machine, World world,
                                      int x, int y, int z, ItemStack trigger) {
                return casing.placeBlock(machine, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(GT_MetaTileEntity_Cleanroom machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return casing.getBlocksToPlace(machine, world, x, y, z, trigger, environment);
            }

            @Override
            public PlaceResult survivalPlaceBlock(GT_MetaTileEntity_Cleanroom machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return casing.survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
            }
        };
    }

    private static boolean isHull(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) tile).getMetaTileEntity() instanceof GT_MetaTileEntity_BasicHull;
    }

    private boolean isDoor(RoomSize size, World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (!(block instanceof BlockIC2Door)) {
            return false;
        }
        int lowerY = (world.getBlockMetadata(x, y, z) & 8) == 0 ? y : y - 1;
        int[] local = relativePosition(x, lowerY, z);
        boolean onWall = Math.abs(local[0]) == size.halfWidth || Math.abs(local[2]) == size.halfDepth;
        return onWall && local[1] >= 2 && local[1] < size.height - 1
                && world.blockExists(x, lowerY, z) && world.blockExists(x, lowerY + 1, z)
                && world.getBlock(x, lowerY, z) == block && world.getBlock(x, lowerY + 1, z) == block
                && (world.getBlockMetadata(x, lowerY, z) & 8) == 0
                && (world.getBlockMetadata(x, lowerY + 1, z) & 8) != 0;
    }

    private void attachInteriorMachines(RoomSize size) {
        for (int a = 1 - size.halfWidth; a < size.halfWidth; a++) {
            for (int c = 1 - size.halfDepth; c < size.halfDepth; c++) {
                for (int b = 1; b < size.height - 1; b++) {
                    int[] p = worldPosition(a, b, c);
                    TileEntity tile = getBaseMetaTileEntity().getWorld().getTileEntity(p[0], p[1], p[2]);
                    if (!(tile instanceof IGregTechTileEntity)) {
                        continue;
                    }
                    IMetaTileEntity machine = ((IGregTechTileEntity) tile).getMetaTileEntity();
                    if (machine instanceof GT_MetaTileEntity_BasicMachine_GT_Recipe) {
                        ((GT_MetaTileEntity_BasicMachine_GT_Recipe) machine).mCleanroom = this;
                        attachedMachines.add((MetaTileEntity) machine);
                    } else if (machine instanceof GT_MetaTileEntity_MultiBlockBase && machine != this) {
                        ((GT_MetaTileEntity_MultiBlockBase) machine).mCleanroom = this;
                        attachedMachines.add((MetaTileEntity) machine);
                    }
                }
            }
        }
    }

    private void detachMachines() {
        for (MetaTileEntity machine : attachedMachines) {
            if (machine instanceof GT_MetaTileEntity_BasicMachine_GT_Recipe) {
                GT_MetaTileEntity_BasicMachine_GT_Recipe basic = (GT_MetaTileEntity_BasicMachine_GT_Recipe) machine;
                if (basic.mCleanroom == this) {
                    basic.mCleanroom = null;
                }
            } else if (machine instanceof GT_MetaTileEntity_MultiBlockBase) {
                GT_MetaTileEntity_MultiBlockBase multi = (GT_MetaTileEntity_MultiBlockBase) machine;
                if (multi.mCleanroom == this) {
                    multi.mCleanroom = null;
                }
            }
        }
        attachedMachines.clear();
    }

    private void updateRoomRedstone(boolean valid) {
        byte signal = valid ? (byte) Math.max(1, 15 * mEfficiency / 10000) : 0;
        for (byte side = 0; side < 6; side++) {
            getBaseMetaTileEntity().setInternalOutputRedstoneSignal(side, signal);
        }
    }

    @Override
    public void onRemoval() {
        detachMachines();
        super.onRemoval();
    }

    private static final class RoomSize {
        private final int halfWidth;
        private final int height;
        private final int halfDepth;

        private RoomSize(int halfWidth, int height, int halfDepth) {
            this.halfWidth = halfWidth;
            this.height = height;
            this.halfDepth = halfDepth;
        }

        private String key() {
            return halfWidth + ":" + height + ":" + halfDepth;
        }
    }

    @Override
    public boolean allowGeneralRedstoneOutput(){
    	return true;
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
		if (aSide == ForgeDirection.DOWN.ordinal() || aSide == ForgeDirection.UP.ordinal()) {
			return new ITexture[]{
					Casings.PLASCRETE.getTexture(),
					aActive ?
							TextureFactory.of(
									TextureFactory.of(OVERLAY_TOP_CLEANROOM_ACTIVE),
									TextureFactory.builder().addIcon(OVERLAY_TOP_CLEANROOM_ACTIVE_GLOW).glow().build()) :
							TextureFactory.of(
									TextureFactory.of(OVERLAY_TOP_CLEANROOM),
									TextureFactory.builder().addIcon(OVERLAY_TOP_CLEANROOM_GLOW).glow().build())
			};
        }
        return new ITexture[]{Casings.PLASCRETE.getTexture()};
    }

	@Override
	public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, this.getLocalName(), "MultiblockDisplay.png");
	}

	@Override
	public GT_Recipe.GT_Recipe_Map getRecipeMap() {
		return null;
	}

	@Override
	public boolean isCorrectMachinePart(ItemStack aStack) {
		return true;
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

	@Override
	public String[] getInfoData() {
		int tierHatch = 0;
		for (GT_MetaTileEntity_Hatch_Energy tHatch : mEnergyHatches) tierHatch = tHatch.mTier;
		for (GT_MetaTileEntity_Hatch_EnergyMulti tHatch : mEnergyHatchesMulti) tierHatch = tHatch.mTier;

		return new String[]{
				"Progress: " + EnumChatFormatting.GREEN + mProgresstime / 20 + EnumChatFormatting.RESET +" s / " + EnumChatFormatting.YELLOW + mMaxProgresstime / 20 + EnumChatFormatting.RESET +" s",
				"Probably uses: " + EnumChatFormatting.RED + Integer.toString(-mEUt) + EnumChatFormatting.RESET + " EU/t",
				"Efficiency Increase" + tierHatch + " % " + "Tier: " + EnumChatFormatting.YELLOW + VN[GT_Utility.getTier(getMaxInputVoltage())] + EnumChatFormatting.RESET,
				"Problems: " + EnumChatFormatting.RED + (getIdealStatus() - getRepairStatus()) + EnumChatFormatting.RESET + " Efficiency: "+ EnumChatFormatting.YELLOW + mEfficiency / 100.0F + EnumChatFormatting.RESET + " %",
		};
	}
}
