package gregtech.common.tileentities.machines.multi;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import cpw.mods.fml.common.Optional;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.TieredVariant;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuiTheme;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.logic.InventoryProcessingOutputs;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.GTWaila;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.MultiblockTooltipCache;
import gregtech.common.GT_Pollution;
import gregtech.common.gui.modularui.multiblock.MTEBrickedBlastFurnaceGui;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import java.util.Arrays;
import java.util.Collections;

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
    private ProcessingLogic processingLogic;
    private CheckRecipeResult processingResult = CheckRecipeResult.NONE;
    private boolean waitingForOutput;

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
        aNBT.setBoolean("waitingForOutput", waitingForOutput);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            aNBT.removeTag("mOutputItem" + i);
        }
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
        waitingForOutput = mMaxProgresstime > 0 && aNBT.getBoolean("waitingForOutput");
        processingResult = waitingForOutput ? CheckRecipeResult.WAITING_FOR_OUTPUT
                : mMaxProgresstime > 0 ? CheckRecipeResult.SUCCESS : CheckRecipeResult.NONE;
        if (processingLogic != null) {
            processingLogic.clearRecipeCache();
        }
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
    public boolean useMui2() {
        return true;
    }

    @Override
    public GTGuiTheme getGuiTheme() {
        return GTGuiThemes.TIERED_VARIANTS.get(TieredVariant.PRIMITIVE);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEBrickedBlastFurnaceGui(this).build(data, syncManager, settings);
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
                    if (!waitingForOutput && mProgresstime < mMaxProgresstime) {
                        mProgresstime++;
                    }
                    if (mProgresstime >= mMaxProgresstime && completeRecipe()) {
                        GT_Mod.achievements.issueAchievement(
                                aBaseMetaTileEntity.getWorld().getPlayerEntityByName(aBaseMetaTileEntity.getOwnerName()), "steel");
                    }
                } else if (aBaseMetaTileEntity.isAllowedToWork()) {
                    checkRecipe();
                }
            }
            if (this.mMaxProgresstime > 0 && !waitingForOutput && (aTimer % 20L == 0L)) {
                GT_Pollution.addPollution(this.getBaseMetaTileEntity().getWorld(),
                        new ChunkPosition(this.getBaseMetaTileEntity().getXCoord(), this.getBaseMetaTileEntity().getYCoord(),
                                this.getBaseMetaTileEntity().getZCoord()),
                        setPollut());
            }

            aBaseMetaTileEntity.setActive(mMaxProgresstime > 0 && mMachine && !waitingForOutput);
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

    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic()
                .setRecipeMapSupplier(() -> GT_Recipe.GT_Recipe_Map.sPrimitiveBlastRecipes)
                .setPowerRequired(false)
                .setItemOutputLimit(OUTPUT_SLOTS)
                .setCalculatorSupplier(() -> new OverclockCalculator()
                        .setMaxOverclocks(0)
                        .setDurationModifier(1.0 / getTier()));
    }

    protected final ProcessingLogic getProcessingLogic() {
        if (processingLogic == null) {
            processingLogic = createProcessingLogic();
        }
        return processingLogic;
    }

    public CheckRecipeResult getProcessingResult() {
        return processingResult;
    }

    public boolean isWaitingForOutput() {
        return waitingForOutput;
    }

    private InventoryProcessingOutputs processingOutputs() {
        return new InventoryProcessingOutputs(getBaseMetaTileEntity(), INPUT_SLOTS, OUTPUT_SLOTS);
    }

    private boolean completeRecipe() {
        long before = pendingItemCount();
        if (!processingOutputs().deliver(mOutputItems)) {
            boolean changed = !waitingForOutput || before != pendingItemCount();
            waitingForOutput = true;
            processingResult = CheckRecipeResult.WAITING_FOR_OUTPUT;
            if (changed) {
                markStorageDirty();
            }
            return false;
        }
        mOutputItems = null;
        mProgresstime = 0;
        mMaxProgresstime = 0;
        waitingForOutput = false;
        processingResult = CheckRecipeResult.NONE;
        markStorageDirty();
        return true;
    }

    private long pendingItemCount() {
        long count = 0;
        if (mOutputItems != null) {
            for (ItemStack item : mOutputItems) {
                if (item != null) {
                    count += Math.max(0, item.stackSize);
                }
            }
        }
        return count;
    }

    private boolean checkRecipe() {
        if (mMaxProgresstime > 0 || waitingForOutput) {
            return false;
        }
        if (!mMachine) {
            processingResult = CheckRecipeResult.MACHINE_CONDITION;
            return false;
        }
        ItemStack[] originals = Arrays.copyOf(mInventory, INPUT_SLOTS);
        ProcessingInputs inputs = new ProcessingInputs(Arrays.asList(originals), Collections.emptyList(), () -> {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (mInventory[i] != originals[i]) {
                    return false;
                }
            }
            return mMachine;
        });
        ProcessingLogic.Request request = new ProcessingLogic.Request(Collections.singletonList(inputs), 0, 0,
                ProcessingLogic.PROTECT_ITEMS | ProcessingLogic.PROTECT_FLUIDS, processingOutputs(),
                bound -> getBaseMetaTileEntity().getRandomNumber(bound));
        ProcessingLogic.Result result = getProcessingLogic().process(request);
        processingResult = result.status;
        if (!result.status.wasSuccessful()) {
            return false;
        }
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (mInventory[i] != null && mInventory[i].stackSize <= 0) {
                mInventory[i] = null;
            }
        }
        mProgresstime = 0;
        mMaxProgresstime = result.duration;
        mOutputItems = result.items;
        markStorageDirty();
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
