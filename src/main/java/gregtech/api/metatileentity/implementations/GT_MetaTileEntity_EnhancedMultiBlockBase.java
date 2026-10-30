package gregtech.api.metatileentity.implementations;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.NetworkRegistry;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import space.impact.api.ImpactAPI;
import space.impact.api.Main;
import space.impact.api.alignment.IAlignment;
import space.impact.api.alignment.IAlignmentLimits;
import space.impact.api.alignment.IAlignmentProvider;
import space.impact.api.alignment.constructable.IConstructable;
import space.impact.api.alignment.enumerable.ExtendedFacing;
import space.impact.api.alignment.enumerable.Flip;
import space.impact.api.alignment.enumerable.Rotation;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.IStructureWalker;
import space.impact.api.structure.ISurvivalBuildEnvironment;

/**
 * Declarative structures and construction, following GTNH's enhanced multiblock API.
 */
public abstract class GT_MetaTileEntity_EnhancedMultiBlockBase<T extends GT_MetaTileEntity_EnhancedMultiBlockBase<T>>
        extends GT_MetaTileEntity_TooltipMultiBlockBase implements IAlignment, IConstructable {

    private ExtendedFacing extendedFacing;

    protected GT_MetaTileEntity_EnhancedMultiBlockBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GT_MetaTileEntity_EnhancedMultiBlockBase(String name) {
        super(name);
    }

    /**
     * Override in a machine to opt into vertical facing, rotation or reflection.
     */
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && direction != ForgeDirection.UNKNOWN
                && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public final IAlignmentLimits getAlignmentLimits() {
        return getInitialAlignmentLimits();
    }

    @Override
    public final boolean isFacingValid(byte side) {
        return side >= 0 && side < 6 && canSetToDirectionAny(ForgeDirection.getOrientation(side));
    }

    @Override
    public final ExtendedFacing getExtendedFacing() {
        if (extendedFacing == null) {
            IGregTechTileEntity base = getBaseMetaTileEntity();
            extendedFacing = correctAlignment(ExtendedFacing.of(base == null ? ForgeDirection.NORTH
                    : ForgeDirection.getOrientation(base.getFrontFacing())));
        }
        return extendedFacing;
    }

    private ExtendedFacing correctAlignment(ExtendedFacing facing) {
        if (isNewExtendedFacingValid(facing)) return facing;
        ExtendedFacing unrotated = ExtendedFacing.of(facing.getDirection());
        if (isNewExtendedFacingValid(unrotated)) return unrotated;
        for (ExtendedFacing candidate : ExtendedFacing.VALUES) {
            if (isNewExtendedFacingValid(candidate)) return candidate;
        }
        throw new IllegalStateException("No permitted alignment for " + mName);
    }

    @Override
    public final void setExtendedFacing(ExtendedFacing facing) {
        if (facing == null || !isNewExtendedFacingValid(facing) || facing == extendedFacing) return;
        extendedFacing = facing;
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null) return;
        if (base.getFrontFacing() != facing.getDirection().ordinal()) {
            base.setFrontFacing((byte) facing.getDirection().ordinal());
        }
        mMachine = false;
        onMachineBlockUpdate();
        mUpdate = 1;
        if (base.getWorld() == null) return;
        if (base.isServerSide()) {
            base.markDirty();
            ImpactAPI.sendAlignment((IAlignmentProvider) base, new NetworkRegistry.TargetPoint(
                    base.getWorld().provider.dimensionId, base.getXCoord(), base.getYCoord(), base.getZCoord(), 128));
        } else {
            base.issueTextureUpdate();
        }
    }

    @Override
    public void onFacingChange() {
        super.onFacingChange();
        ExtendedFacing previous = extendedFacing == null ? ExtendedFacing.DEFAULT : extendedFacing;
        setExtendedFacing(correctAlignment(ExtendedFacing.of(
                ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing()),
                previous.getRotation(), previous.getFlip())));
    }

    @Override
    public boolean onWrenchRightClick(byte side, byte wrenchingSide, EntityPlayer player, float x, float y, float z) {
        if (wrenchingSide != getBaseMetaTileEntity().getFrontFacing()) {
            return super.onWrenchRightClick(side, wrenchingSide, player, x, y, z);
        }
        if (player.isSneaking()) {
            return isFlipChangeAllowed() && toolSetFlip(getFlip().isHorizontallyFlipped() ? Flip.NONE : Flip.HORIZONTAL);
        }
        return isRotationChangeAllowed() && toolSetRotation(null);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setByte("eRotation", (byte) getExtendedFacing().getRotation().getIndex());
        tag.setByte("eFlip", (byte) getExtendedFacing().getFlip().getIndex());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        int rotation = tag.getByte("eRotation");
        int flip = tag.getByte("eFlip");
        extendedFacing = correctAlignment(ExtendedFacing.of(
                ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing()),
                rotation >= 0 && rotation < Rotation.VALUES.length ? Rotation.byIndex(rotation) : Rotation.NORMAL,
                flip >= 0 && flip < Flip.VALUES.length ? Flip.byIndex(flip) : Flip.NONE));
    }

    @Override
    public void onFirstTick(IGregTechTileEntity base) {
        super.onFirstTick(base);
        ExtendedFacing facing = getExtendedFacing();
        if (base.isServerSide()) {
            if (base.getFrontFacing() != facing.getDirection().ordinal()) {
                base.setFrontFacing((byte) facing.getDirection().ordinal());
            }
        } else {
            ImpactAPI.queryAlignment((IAlignmentProvider) base);
        }
    }

    @Override
    public abstract IStructureDefinition<T> getStructureDefinition();

    @Override
    public String[] getStructureDescription(ItemStack trigger) {
        return getTooltip() == null ? getDescription() : getTooltip().getStructureHint();
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /**
     * Visit the entire piece, including after errors, so incomplete machines still discover all their hatches.
     */
    protected final boolean checkPiece(String piece, int horizontal, int vertical, int depth) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        boolean[] valid = {true};
        getStructureDefinition().iterate(piece, base.getWorld(), getExtendedFacing(),
                base.getXCoord(), base.getYCoord(), base.getZCoord(), horizontal, vertical, depth,
                (element, world, x, y, z, a, b, c) -> {
                    if (!world.blockExists(x, y, z) || !element.check(self(), world, x, y, z)) valid[0] = false;
                    return true;
                });
        return valid[0];
    }

    protected final boolean buildPiece(String piece, ItemStack trigger, boolean hintsOnly,
                                       int horizontal, int vertical, int depth) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base.isClientSide() != hintsOnly) return false;
        if (hintsOnly) {
            boolean hinted = getStructureDefinition().hints(self(), trigger, piece, base.getWorld(), getExtendedFacing(),
                    base.getXCoord(), base.getYCoord(), base.getZCoord(), horizontal, vertical, depth);
            // Mark all survival obstructions on the freshly generated hologram, without requiring show_error.
            EntityPlayer player = Main.getCurrentPlayer();
            if (player != null && !player.capabilities.isCreativeMode) {
                findConstructionConflicts(piece, trigger, horizontal, vertical, depth, player);
            }
            return hinted;
        }
        boolean[] complete = {true};
        getStructureDefinition().iterate(piece, base.getWorld(), getExtendedFacing(),
                base.getXCoord(), base.getYCoord(), base.getZCoord(), horizontal, vertical, depth,
                (element, world, x, y, z, a, b, c) -> {
                    if (!world.blockExists(x, y, z)) complete[0] = false;
                    else if (!element.couldBeValid(self(), world, x, y, z, trigger)) {
                        if (!world.isAirBlock(x, y, z) || !element.placeBlock(self(), world, x, y, z, trigger)) {
                            complete[0] = false;
                        }
                    }
                    return true;
                });
        setupMachine(base, mInventory[1]);
        return complete[0];
    }

    /**
     * Read-only scan of the entire piece, independent of placement budget and available items.
     * Never call check() here: it can register hatches or increment a machine's casing counters.
     */
    private int findConstructionConflicts(String piece, ItemStack trigger, int horizontal, int vertical, int depth,
                                          EntityPlayer actor) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        int[] conflicts = {0};
        getStructureDefinition().iterate(piece, base.getWorld(), getExtendedFacing(),
                base.getXCoord(), base.getYCoord(), base.getZCoord(), horizontal, vertical, depth,
                IStructureWalker.<T>ignoreBlockUnloaded((element, world, x, y, z, a, b, c) -> {
                    boolean blocked = !world.blockExists(x, y, z);
                    if (!blocked && !element.couldBeValid(self(), world, x, y, z, trigger)) {
                        blocked = !ImpactAPI.isBlockTriviallyReplaceable(world, x, y, z, actor);
                    }
                    if (blocked) {
                        conflicts[0]++;
                        if (actor != null) ImpactAPI.markHintParticleError(actor, world, x, y, z);
                    }
                    return true;
                }));
        return conflicts[0];
    }

    protected final int survivalBuildPiece(String piece, ItemStack trigger, int horizontal, int vertical, int depth,
                                           int budget, ISurvivalBuildEnvironment environment) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (!base.isServerSide() || budget <= 0) return 0;
        int conflicts = findConstructionConflicts(piece, trigger, horizontal, vertical, depth,
                environment.getActor());
        if (conflicts > 0) {
            environment.getActor().addChatComponentMessage(
                    new ChatComponentTranslation("gt.structure.obstructed", conflicts));
            return 0;
        }
        setupMachine(base, mInventory[1]);
        if (mMachine) return -1;
        int built = getStructureDefinition().survivalBuild(self(), trigger, piece, base.getWorld(), getExtendedFacing(),
                base.getXCoord(), base.getYCoord(), base.getZCoord(), horizontal, vertical, depth,
                budget, environment, false);
        setupMachine(base, mInventory[1]);
        if (built == -1 && !mMachine) {
            environment.getActor().addChatComponentMessage(
                    new ChatComponentTranslation("gt.structure.incomplete"));
            return 0;
        }
        return built;
    }
}
