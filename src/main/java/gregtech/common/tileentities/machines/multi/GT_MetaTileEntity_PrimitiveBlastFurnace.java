package gregtech.common.tileentities.machines.multi;

import cpw.mods.fml.common.Optional;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.GTWaila;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.MultiblockTooltipCache;
import gregtech.common.GT_Pollution;
import gregtech.common.gui.GT_Container_PrimitiveBlastFurnace;
import gregtech.common.gui.GT_GUIContainer_PrimitiveBlastFurnace;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.ImpactStructures;
import space.impact.structures.Main;
import space.impact.structures.alignment.IAlignment;
import space.impact.structures.alignment.IAlignmentLimits;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.alignment.enumerable.ExtendedFacing;
import space.impact.structures.structure.AutoPlaceEnvironment;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.IStructureWalker;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import space.impact.structures.structure.StructureUtility;

import java.util.List;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;
import static space.impact.structures.structure.StructureUtility.transpose;

public abstract class GT_MetaTileEntity_PrimitiveBlastFurnace extends MetaTileEntity
        implements IAlignment, ISurvivalConstructable, ISecondaryDescribable {
    private static final String STRUCTURE_PIECE = "main";
    private final MultiblockTooltipCache tooltipCache = new MultiblockTooltipCache(this::createTooltip);
    private IStructureDefinition<GT_MetaTileEntity_PrimitiveBlastFurnace> structureDefinition;
    public static final int INPUT_SLOTS = 3, OUTPUT_SLOTS = 3;

    public int mMaxProgresstime = 0;
    public int mUpdate = 5;
    public int mProgresstime = 0;
    public boolean mMachine = false;

    public ItemStack[] mOutputItems = new ItemStack[OUTPUT_SLOTS];

    @Deprecated
    public ItemStack mOutputItem1;
    @Deprecated
    public ItemStack mOutputItem2;

    public GT_MetaTileEntity_PrimitiveBlastFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, INPUT_SLOTS + OUTPUT_SLOTS);
    }

    public GT_MetaTileEntity_PrimitiveBlastFurnace(String aName) {
        super(aName, INPUT_SLOTS + OUTPUT_SLOTS);
    }

    @Override
    public boolean isSteampowered() {
        return false;
    }

    @Override
    public boolean isElectric() {
        return false;
    }

    @Override
    public boolean isPneumatic() {
        return false;
    }

    @Override
    public boolean isEnetInput() {
        return false;
    }

    @Override
    public boolean isEnetOutput() {
        return false;
    }

    @Override
    public boolean isInputFacing(byte aSide) {
        return false;
    }

    @Override
    public boolean isOutputFacing(byte aSide) {
        return false;
    }

    @Override
    public boolean isTeleporterCompatible() {
        return false;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return aFacing >= 2 && aFacing <= 5;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public int getProgresstime() {
        return this.mProgresstime;
    }

    @Override
    public int maxProgresstime() {
        return this.mMaxProgresstime;
    }

    @Override
    public int increaseProgress(int aProgress) {
        this.mProgresstime += aProgress;
        return this.mMaxProgresstime - this.mProgresstime;
    }

    @Override
    public boolean allowCoverOnSide(byte aSide, GT_ItemStack aCoverID) {
        return (GregTech_API.getCoverBehavior(aCoverID.toStack()).isSimpleCover()) && (super.allowCoverOnSide(aSide, aCoverID));
    }

	@Override
    public abstract MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity);

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("mProgresstime", this.mProgresstime);
        aNBT.setInteger("mMaxProgresstime", this.mMaxProgresstime);
        if (this.mOutputItems != null) {
            for (int i = 0; i < mOutputItems.length; i++) {
                if (this.mOutputItems[i] != null) {
                    NBTTagCompound tNBT = new NBTTagCompound();
                    this.mOutputItems[i].writeToNBT(tNBT);
                    aNBT.setTag("mOutputItem" + i, tNBT);
                }
            }
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        this.mUpdate = 5;
        this.mProgresstime = aNBT.getInteger("mProgresstime");
        this.mMaxProgresstime = aNBT.getInteger("mMaxProgresstime");
        this.mOutputItems = new ItemStack[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.mOutputItems[i] = GT_Utility.loadItem(aNBT, "mOutputItem" + i);
        }
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        }
        aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_Container_PrimitiveBlastFurnace(aPlayerInventory, aBaseMetaTileEntity);
    }

	@Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
		return new GT_GUIContainer_PrimitiveBlastFurnace(aPlayerInventory, aBaseMetaTileEntity, getName(), GT_Recipe.GT_Recipe_Map.sPrimitiveBlastRecipes.mNEIName);
	}

    public abstract ICasing getCasingType();

    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder(mName)
                .addMachineType(getName())
                .addInfo("Steelmaking and pyrometallurgy")
                .addInfo("No power or hatches required");
        if (getTier() == 4) {
            tooltip.addInfo("Processes recipes four times faster");
        }
        return tooltip.addPollutionAmount(setPollut())
                .beginStructureBlock(3, 4, 3, true)
                .addController("Front center of the second layer")
                .addCasingInfoExactly(getCasingType(), 32, false)
                .addAir("Central shaft, open at the top; working lava is allowed")
                .toolTipFinisher();
    }

    @Override
    public final boolean hasSecondaryDescription() {
        return true;
    }

    @Override
    public String[] getDescription() {
        return getPrimaryDescription();
    }

    @Override
    public final String[] getPrimaryDescription() {
        return tooltipCache.get().getInformation();
    }

    @Override
    public final String[] getSecondaryDescription() {
        return tooltipCache.get().getStructureInformation();
    }

    @Override
    public String[] getStructureDescription(ItemStack trigger) {
        return tooltipCache.get().getStructureHint();
    }

    // Only the controller's horizontal facing is used, so the existing GT save and sync data are sufficient.
    @Override
    public final ExtendedFacing getExtendedFacing() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return ExtendedFacing.of(base != null && isFacingValid(base.getFrontFacing())
                ? ForgeDirection.getOrientation(base.getFrontFacing()) : ForgeDirection.NORTH);
    }

    @Override
    public final IAlignmentLimits getAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && direction != ForgeDirection.UNKNOWN
                && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public final void setExtendedFacing(ExtendedFacing alignment) {
        if (alignment == null || !isNewExtendedFacingValid(alignment) || getBaseMetaTileEntity() == null) return;
        byte side = (byte) alignment.getDirection().ordinal();
        if (getBaseMetaTileEntity().getFrontFacing() != side) {
            getBaseMetaTileEntity().setFrontFacing(side);
        }
    }

    @Override
    public void onFirstTick(IGregTechTileEntity base) {
        super.onFirstTick(base);
        if (base.isServerSide() && !isFacingValid(base.getFrontFacing())) {
            base.setFrontFacing((byte) ForgeDirection.NORTH.ordinal());
        }
    }

    @Override
    public void onFacingChange() {
        super.onFacingChange();
        mMachine = false;
        mUpdate = 1;
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_PrimitiveBlastFurnace> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<GT_MetaTileEntity_PrimitiveBlastFurnace>builder()
                    .addShape(STRUCTURE_PIECE, transpose(new String[][]{
                            {"CCC", "CLC", "CCC"},
                            {"CCC", "CLC", "CCC"},
                            {"C~C", "CLC", "CCC"},
                            {"CCC", "CCC", "CCC"}
                    }))
                    .addElement('C', getCasingType().asElement())
                    .addElement('L', shaft())
                    .build();
        }
        return structureDefinition;
    }

    private static IStructureElement<GT_MetaTileEntity_PrimitiveBlastFurnace> shaft() {
        return new IStructureElement<GT_MetaTileEntity_PrimitiveBlastFurnace>() {
            @Override
            public boolean check(GT_MetaTileEntity_PrimitiveBlastFurnace machine, World world, int x, int y, int z) {
                Block block = world.getBlock(x, y, z);
                return world.isAirBlock(x, y, z) || block == Blocks.lava || block == Blocks.flowing_lava;
            }

            @Override
            public boolean couldBeValid(GT_MetaTileEntity_PrimitiveBlastFurnace machine, World world,
                                         int x, int y, int z, ItemStack trigger) {
                return check(machine, world, x, y, z);
            }

            @Override
            public boolean spawnHint(GT_MetaTileEntity_PrimitiveBlastFurnace machine, World world,
                                     int x, int y, int z, ItemStack trigger) {
                return StructureUtility.isAir().spawnHint(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(GT_MetaTileEntity_PrimitiveBlastFurnace machine, World world,
                                      int x, int y, int z, ItemStack trigger) {
                return check(machine, world, x, y, z);
            }

            @Override
            public PlaceResult survivalPlaceBlock(GT_MetaTileEntity_PrimitiveBlastFurnace machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return check(machine, world, x, y, z) ? PlaceResult.SKIP : PlaceResult.REJECT;
            }
        };
    }

    private void visitStructure(IStructureWalker<GT_MetaTileEntity_PrimitiveBlastFurnace> walker) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        getStructureDefinition().iterate(STRUCTURE_PIECE, base.getWorld(), getExtendedFacing(),
                base.getXCoord(), base.getYCoord(), base.getZCoord(), 1, 2, 0,
                IStructureWalker.ignoreBlockUnloaded(walker));
    }

    public boolean checkMachine() {
        boolean[] valid = {true};
        visitStructure((element, world, x, y, z, a, b, c) -> {
            if (!world.blockExists(x, y, z) || !element.check(this, world, x, y, z)) valid[0] = false;
            return true;
        });
        return valid[0];
    }

    private int findConstructionConflicts(ItemStack trigger, EntityPlayer actor) {
        int[] conflicts = {0};
        visitStructure((element, world, x, y, z, a, b, c) -> {
            if (!world.blockExists(x, y, z)
                    || (!element.couldBeValid(this, world, x, y, z, trigger) && !world.isAirBlock(x, y, z))) {
                conflicts[0]++;
                if (actor != null) ImpactStructures.markHintParticleError(actor, world, x, y, z);
            }
            return true;
        });
        return conflicts[0];
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base.isClientSide() != hintsOnly) return;
        if (hintsOnly) {
            getStructureDefinition().hints(this, trigger, STRUCTURE_PIECE, base.getWorld(), getExtendedFacing(),
                    base.getXCoord(), base.getYCoord(), base.getZCoord(), 1, 2, 0);
            EntityPlayer player = Main.getCurrentPlayer();
            if (player != null && !player.capabilities.isCreativeMode) findConstructionConflicts(trigger, player);
            return;
        }
        visitStructure((element, world, x, y, z, a, b, c) -> {
            if (world.blockExists(x, y, z) && !element.couldBeValid(this, world, x, y, z, trigger)
                    && world.isAirBlock(x, y, z)) {
                element.placeBlock(this, world, x, y, z, trigger);
            }
            return true;
        });
        mMachine = checkMachine();
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int budget, ISurvivalBuildEnvironment environment) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (!base.isServerSide() || budget <= 0) return 0;
        int conflicts = findConstructionConflicts(trigger, environment.getActor());
        if (conflicts > 0) {
            environment.getActor().addChatComponentMessage(new ChatComponentTranslation("gt.structure.obstructed", conflicts));
            return 0;
        }
        mMachine = checkMachine();
        if (mMachine) return -1;
        int built = getStructureDefinition().survivalBuild(this, trigger, STRUCTURE_PIECE, base.getWorld(),
                getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), 1, 2, 0,
                budget, environment, false);
        mMachine = checkMachine();
        return built;
    }

    @Override
    public void onMachineBlockUpdate() {
        this.mUpdate = 5;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        if ((aBaseMetaTileEntity.isClientSide()) && (aBaseMetaTileEntity.isActive())) {
            aBaseMetaTileEntity.getWorld().spawnParticle("largesmoke",
                    aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1) + XSTR_INSTANCE.nextFloat(),
                    aBaseMetaTileEntity.getOffsetY(aBaseMetaTileEntity.getBackFacing(), 1),
                    aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1) + XSTR_INSTANCE.nextFloat(), 0.0D, 0.3D, 0.0D);
        }
        if (aBaseMetaTileEntity.isServerSide()) {
            if (this.mUpdate-- == 0) {
                this.mMachine = checkMachine();
            }
            if (this.mMachine) {
                if (this.mMaxProgresstime > 0) {
                    if (++this.mProgresstime >= this.mMaxProgresstime) {
                        addOutputProducts();
                        this.mOutputItems = null;
                        this.mProgresstime = 0;
                        this.mMaxProgresstime = 0;
                        GT_Mod.achievements.issueAchievement(
                                aBaseMetaTileEntity.getWorld().getPlayerEntityByName(aBaseMetaTileEntity.getOwnerName()), "steel");
                    }
                } else if (aBaseMetaTileEntity.isAllowedToWork()) {
                    checkRecipe();
                }
            }
            if (this.mMaxProgresstime > 0 && (aTimer % 20L == 0L)) {
                GT_Pollution.addPollution(this.getBaseMetaTileEntity().getWorld(),
                        new ChunkPosition(this.getBaseMetaTileEntity().getXCoord(), this.getBaseMetaTileEntity().getYCoord(),
                                this.getBaseMetaTileEntity().getZCoord()),
                        setPollut());
            }

            aBaseMetaTileEntity.setActive((this.mMaxProgresstime > 0) && (this.mMachine));
            if (aBaseMetaTileEntity.isActive()) {
                if (aBaseMetaTileEntity.getAir(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                        aBaseMetaTileEntity.getYCoord(), aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1))) {
                    aBaseMetaTileEntity.getWorld().setBlock(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                            aBaseMetaTileEntity.getYCoord(), aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1),
                            Blocks.lava, 1, 2);
                    this.mUpdate = 1;
                }
                if (aBaseMetaTileEntity.getAir(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                        aBaseMetaTileEntity.getYCoord() + 1, aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1))) {
                    aBaseMetaTileEntity.getWorld().setBlock(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                            aBaseMetaTileEntity.getYCoord() + 1, aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1),
                            Blocks.lava, 1, 2);
                    this.mUpdate = 1;
                }
            } else {
                if (aBaseMetaTileEntity.getBlock(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                        aBaseMetaTileEntity.getYCoord(),
                        aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1)) == Blocks.lava) {
                    aBaseMetaTileEntity.getWorld().setBlock(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                            aBaseMetaTileEntity.getYCoord(), aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1),
                            Blocks.air, 0, 2);
                    this.mUpdate = 1;
                }
                if (aBaseMetaTileEntity.getBlock(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                        aBaseMetaTileEntity.getYCoord() + 1,
                        aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1)) == Blocks.lava) {
                    aBaseMetaTileEntity.getWorld().setBlock(aBaseMetaTileEntity.getOffsetX(aBaseMetaTileEntity.getBackFacing(), 1),
                            aBaseMetaTileEntity.getYCoord() + 1, aBaseMetaTileEntity.getOffsetZ(aBaseMetaTileEntity.getBackFacing(), 1),
                            Blocks.air, 0, 2);
                    this.mUpdate = 1;
                }
            }
        }
    }

    public abstract int setPollut();

    private void addOutputProducts() {
        if (this.mOutputItems == null) {
            return;
        }
        int limit = Math.min(mOutputItems.length, OUTPUT_SLOTS);
        for (int i = 0; i < limit; i++) {
            int absi = INPUT_SLOTS + i;
            if (this.mInventory[absi] == null) {
                this.mInventory[absi] = GT_Utility.copy(this.mOutputItems[i]);
            } else if (GT_Utility.areStacksEqual(this.mInventory[absi], this.mOutputItems[i])) {
                this.mInventory[absi].stackSize = Math.min(this.mInventory[absi].getMaxStackSize(),
                        this.mInventory[absi].stackSize + this.mOutputItems[i].stackSize);
            }
        }
    }

    private boolean spaceForOutput(ItemStack outputStack, int relativeOutputSlot) {
        int absoluteSlot = relativeOutputSlot + INPUT_SLOTS;
        if (this.mInventory[absoluteSlot] == null || outputStack == null) {
            return true;
        }
        return ((this.mInventory[absoluteSlot].stackSize + outputStack.stackSize <= this.mInventory[absoluteSlot].getMaxStackSize()) && (GT_Utility.areStacksEqual(this.mInventory[absoluteSlot], outputStack)));
    }

    private boolean checkRecipe() {
        if (!this.mMachine) {
            return false;
        }
        ItemStack[] inputs = new ItemStack[INPUT_SLOTS];
        System.arraycopy(mInventory, 0, inputs, 0, INPUT_SLOTS);
        GT_Recipe recipe = GT_Recipe.GT_Recipe_Map.sPrimitiveBlastRecipes.findRecipe(getBaseMetaTileEntity(), false, 0, null, inputs);
        if (recipe == null) {
            this.mOutputItems = null;
            return false;
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (!spaceForOutput(recipe.getOutput(i), i)) {
                this.mOutputItems = null;
                return false;
            }
        }

        if (!recipe.isRecipeInputEqual(true, null, inputs)) {
            this.mOutputItems = null;
            return false;
        }
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (mInventory[i] != null && mInventory[i].stackSize == 0) {
                mInventory[i] = null;
            }
        }

        this.mMaxProgresstime = recipe.mDuration/getTier();
        this.mOutputItems = recipe.mOutputs;
        return true;
    }

    @Override
    public boolean isGivingInformation() {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aIndex > INPUT_SLOTS;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return !GT_Utility.areStacksEqual(aStack, this.mInventory[0]);
    }

    @Override
    public byte getTileEntityBaseType() {
        return 0;
    }

    public abstract byte getTier();

    public abstract String getName();

    @Override
    @Optional.Method(modid = "Waila")
    public void getWailaBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                             IWailaConfigHandler config) {
        super.getWailaBody(stack, tooltip, accessor, config);
        if (!config.getConfig("gregtech.multiblock", true)) {
            return;
        }
        NBTTagCompound tag = accessor.getNBTData();
        if (tag.getBoolean("incompleteStructurePrimitiveBlastFurnace")) {
            tooltip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("GT5U.waila.multiblock.incomplete_structure")
                    + EnumChatFormatting.RESET);
        }
        tooltip.add(GTWaila.getMachineProgressString(tag.getBoolean("isActive"),
                tag.getBoolean("isAllowedToWork"), tag.getInteger("maxProgressPrimitiveBlastFurnace"),
                tag.getInteger("progressPrimitiveBlastFurnace")));
    }

    @Override
    @Optional.Method(modid = "Waila")
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag,
                                World world, int x, int y, int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("progressPrimitiveBlastFurnace", mProgresstime);
        tag.setInteger("maxProgressPrimitiveBlastFurnace", mMaxProgresstime);
        tag.setBoolean("incompleteStructurePrimitiveBlastFurnace", !mMachine);
    }
}
