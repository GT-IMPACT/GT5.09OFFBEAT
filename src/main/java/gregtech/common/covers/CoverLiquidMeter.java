package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.cover.CoverLiquidMeterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class CoverLiquidMeter extends CoverLegacyData implements IInvertable {

    private int threshold;

    public CoverLiquidMeter(CoverContext context) {
        super(context);
        coverData = 1;
        tickRateAddition = 4;
    }

    @Override
    public boolean isInverted() {
        return getVariable() == 0;
    }

    @Override
    public CoverLiquidMeter setInverted(boolean inverted) {
        if (isValid()) {
            setGuiVariable(inverted ? 0 : 1);
        }
        return this;
    }

    public int getThreshold() {
        return threshold;
    }

    public CoverLiquidMeter setThreshold(int threshold) {
        if (isValid()) {
            this.threshold = Math.max(0, Math.min(threshold, getMaxCapacity()));
            setGuiVariable(getVariable());
        }
        return this;
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("invert", isInverted());
        tag.setInteger("threshold", threshold);
        return tag;
    }

    @Override
    protected void readDataFromNbt(NBTBase nbt) {
        if (nbt instanceof NBTTagInt) {
            coverData = ((NBTTagInt) nbt).func_150287_d();
            threshold = 0;
            setTickRateAddition(getTickRateAddition() + 4);
        } else if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            coverData = tag.getBoolean("invert") ? 0 : 1;
            threshold = Math.max(0, tag.getInteger("threshold"));
        }
    }

    @Override
    protected void writeDataToByteBuf(ByteBuf buffer) {
        buffer.writeBoolean(isInverted());
        buffer.writeInt(threshold);
    }

    @Override
    public void readDataFromPacket(ByteArrayDataInput input) {
        coverData = input.readBoolean() ? 0 : 1;
        threshold = Math.max(0, input.readInt());
    }

    public static byte computeSignalBasedOnFluid(ICoverable tile, boolean inverted, int threshold) {
        if (!(tile instanceof IFluidHandler)) {
            return 0;
        }
        FluidTankInfo[] tanks = ((IFluidHandler) tile).getTankInfo(ForgeDirection.UNKNOWN);
        long maximum = 0;
        long used = 0;
        if (tanks != null) {
            for (FluidTankInfo tank : tanks) {
                if (tank != null) {
                    maximum += tank.capacity;
                    if (tank.fluid != null) {
                        used += tank.fluid.amount;
                    }
                }
            }
        }
        return GTUtility.convertRatioToRedstone(used, maximum, threshold, inverted);
    }

    @Override
    public void doCoverThings(byte inputRedstone, long timer) {
        ICoverable tile = getTile();
        if (tile != null) {
            tile.setOutputRedstoneSignal((byte) coverSide.ordinal(),
                    computeSignalBasedOnFluid(tile, isInverted(), threshold));
        }
    }

    public int getMaxCapacity() {
        ICoverable tile = getTile();
        if (tile == null || tile.isDead() || !(tile instanceof IFluidHandler)) {
            return 0;
        }
        long capacity = 0;
        FluidTankInfo[] tanks = ((IFluidHandler) tile).getTankInfo(ForgeDirection.UNKNOWN);
        if (tanks != null) {
            for (FluidTankInfo tank : tanks) {
                if (tank != null) {
                    capacity += tank.capacity;
                }
            }
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, capacity));
    }

    @Override
    public int getMinimumTickRate() {
        return 1;
    }

    @Override
    public int getDefaultTickRate() {
        return 5;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverLiquidMeterGui(this, getMaxCapacity());
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        if (coverData == 0) {
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_055", "Normal"));
        } else {
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_054", "Inverted"));
        }
        coverData = coverData == 0 ? 1 : 0;
        markGuiChanged();
    }

    @Override
    public boolean letsEnergyIn() {
        return true;
    }

    @Override
    public boolean letsEnergyOut() {
        return true;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return true;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return true;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return true;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return true;
    }

    @Override
    public boolean manipulatesSidedRedstoneOutput() {
        return true;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
