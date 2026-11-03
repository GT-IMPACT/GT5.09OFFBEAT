package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/** Compatibility for existing addon behaviors; persistent data is owned by this cover object. */
@Deprecated
public class CoverLegacyBehavior extends CoverLegacyData {

    private final GT_CoverBehavior behavior;

    public CoverLegacyBehavior(CoverContext context, GT_CoverBehavior behavior) {
        super(context);
        this.behavior = behavior;
    }

    public GT_CoverBehavior getBehavior() {
        return behavior;
    }

    protected final void setGuiVariable(int value) {
        if (isValid()) {
            coverData = value;
            markGuiChanged();
        }
    }

    @Override
    protected NBTBase saveDataToNbt() {
        if (behavior instanceof GT_Cover_Fluidfilter) {
            NBTTagCompound state = new NBTTagCompound();
            state.setInteger("legacy", coverData);
            Fluid fluid = FluidRegistry.getFluid(coverData >>> 3);
            if (fluid != null) {
                state.setString("fluid", FluidRegistry.getFluidName(fluid));
            }
            return state;
        }
        return super.saveDataToNbt();
    }

    @Override
    protected void readDataFromNbt(NBTBase nbt) {
        if (behavior instanceof GT_Cover_Fluidfilter && nbt instanceof NBTTagCompound) {
            NBTTagCompound state = (NBTTagCompound) nbt;
            coverData = state.getInteger("legacy");
            if (state.hasKey("fluid")) {
                coverData = (coverData & 7) | (FluidRegistry.getFluidID(state.getString("fluid")) << 3);
            }
        } else {
            super.readDataFromNbt(nbt);
        }
    }

    private byte side() {
        return (byte) coverSide.ordinal();
    }

    @Override
    public boolean useMui2() {
        return false;
    }

    @Override
    public boolean hasCoverGUI() {
        return behavior.hasCoverGUI();
    }

    @Override
    public boolean openGUI(EntityPlayer player) {
        if (useMui2()) {
            return super.openGUI(player);
        }
        return isValid() && behavior.onCoverShiftRightclick(
            side(),
            coverID,
            coverData,
            getTile(),
            player
        );
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        coverData = behavior.doCoverThings(
            side(),
            redstone,
            coverID,
            coverData,
            getTile(),
            timer
        );
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = behavior.onCoverScrewdriverclick(
            side(),
            coverID,
            coverData,
            getTile(),
            player,
            x,
            y,
            z
        );
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        return behavior.onCoverRightclick(
            side(),
            coverID,
            coverData,
            getTile(),
            player,
            x,
            y,
            z
        );
    }

    @Override
    public boolean canRemove(boolean forced) {
        return behavior.onCoverRemoval(
            side(),
            coverID,
            coverData,
            getTile(),
            forced
        );
    }

    @Override
    public int getMinimumTickRate() {
        return behavior.getTickRate(side(), coverID, coverData, getTile());
    }

    @Override
    public byte getRedstoneInput(byte input) {
        return behavior.getRedstoneInput(
            side(),
            input,
            coverID,
            coverData,
            getTile()
        );
    }

    @Override
    public boolean isGUIClickable() {
        return behavior.isGUIClickable(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return behavior.letsRedstoneGoIn(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsRedstoneGoOut() {
        return behavior.letsRedstoneGoOut(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsFibreGoIn() {
        return behavior.letsFibreGoIn(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsFibreGoOut() {
        return behavior.letsFibreGoOut(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsEnergyIn() {
        return behavior.letsEnergyIn(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsEnergyOut() {
        return behavior.letsEnergyOut(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return behavior.letsFluidIn(
            side(),
            coverID,
            coverData,
            fluid,
            getTile()
        );
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return behavior.letsFluidOut(
            side(),
            coverID,
            coverData,
            fluid,
            getTile()
        );
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return behavior.letsItemsIn(
            side(),
            coverID,
            coverData,
            slot,
            getTile()
        );
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return behavior.letsItemsOut(
            side(),
            coverID,
            coverData,
            slot,
            getTile()
        );
    }

    @Override
    public boolean manipulatesSidedRedstoneOutput() {
        return behavior.manipulatesSidedRedstoneOutput(side(), coverID, coverData, getTile());
    }

    @Override
    public boolean alwaysLookConnected() {
        return behavior.alwaysLookConnected(side(), coverID, coverData, getTile());
    }

    @Override
    public byte getLensColor() {
        return behavior.getLensColor(side(), coverID, coverData, getTile());
    }

    @Override
    public float getBlastProofLevel() {
        return behavior.getBlastProofLevel(side(), coverID, coverData, getTile());
    }

    @Override
    public String getDescription() {
        return behavior.getDescription(side(), coverID, coverData, getTile());
    }

}

