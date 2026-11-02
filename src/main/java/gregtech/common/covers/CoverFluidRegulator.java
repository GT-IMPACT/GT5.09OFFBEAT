package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.covers.CoverContext;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.modes.MachineProcessingCondition;
import gregtech.common.covers.modes.TransferMode;
import gregtech.common.gui.modularui.cover.CoverFluidRegulatorGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

/** GTNH's native regulator, retaining existing regulator tiers, steam filtering and legacy int migration. */
public class CoverFluidRegulator extends Cover {

    public static final int TICK_RATE_MIN = 1;
    public static final int TICK_RATE_MAX = 2048;

    private final GT_Cover_FluidRegulator behavior;
    private final int transferRate;
    private boolean allowFluid;
    private int tickRate = TICK_RATE_MIN;
    private int speed;
    private Conditional condition = Conditional.ALWAYS;

    public CoverFluidRegulator(CoverContext context, GT_CoverBehavior behavior) {
        super(context, CoverRegistry.getCoverTexture(context.getCoverItem()));
        if (!(behavior instanceof GT_Cover_FluidRegulator)) {
            throw new IllegalArgumentException("Fluid regulator behavior is required");
        }
        this.behavior = (GT_Cover_FluidRegulator) behavior;
        transferRate = this.behavior.mTransferRate;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverFluidRegulatorGui(this);
    }

    public int getTransferRate() {
        return transferRate;
    }

    public int getTickRateForUi() {
        return tickRate;
    }

    public CoverFluidRegulator setTickRateForUi(int tickRate) {
        if (isValid()) {
            int minimumForSpeed = transferRate > 0
                ? (int) ((Math.abs((long) speed) + transferRate - 1) / transferRate) : TICK_RATE_MIN;
            this.tickRate = Math.min(TICK_RATE_MAX, Math.max(TICK_RATE_MIN, Math.max(tickRate, minimumForSpeed)));
            markGuiChanged();
        }
        return this;
    }

    public TransferMode getIOMode() {
        return speed >= 0 ? TransferMode.EXPORT : TransferMode.IMPORT;
    }

    public void setIOMode(TransferMode mode) {
        if (isValid() && mode != null && mode != getIOMode()) {
            speed = -speed;
            markGuiChanged();
        }
    }

    public int getSpeed() {
        return speed;
    }

    public CoverFluidRegulator setSpeed(int speed) {
        if (isValid()) {
            this.speed = Math.max(getMinSpeed(), Math.min(getMaxSpeed(), speed));
            markGuiChanged();
        }
        return this;
    }

    public MachineProcessingCondition getMachineProcessingCondition() {
        if (condition == Conditional.CONDITIONAL) {
            return MachineProcessingCondition.CONDITIONAL;
        }
        return condition == Conditional.INVERTED
            ? MachineProcessingCondition.INVERTED : MachineProcessingCondition.ALWAYS;
    }

    public void setMachineProcessingCondition(MachineProcessingCondition mode) {
        if (mode != null) {
            setCondition(mode == MachineProcessingCondition.ALWAYS ? Conditional.ALWAYS
                : mode == MachineProcessingCondition.CONDITIONAL ? Conditional.CONDITIONAL : Conditional.INVERTED);
        }
    }

    public Conditional getCondition() {
        return condition;
    }

    public CoverFluidRegulator setCondition(Conditional condition) {
        if (isValid() && condition != null) {
            this.condition = condition;
            markGuiChanged();
        }
        return this;
    }

    @Override
    public int getLegacyData() {
        return speed;
    }

    @Override
    public void setLegacyData(int value) {
        speed = Math.max(getMinSpeed(), Math.min(getMaxSpeed(), value));
    }

    @Override
    protected void readDataFromNbt(NBTBase nbt) {
        if (nbt instanceof NBTTagInt) {
            tickRate = TICK_RATE_MIN;
            condition = Conditional.ALWAYS;
            setLegacyData(((NBTTagInt) nbt).func_150287_d());
        } else if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            readState(tag.getInteger("mTickRate"), tag.getInteger("mSpeed"), tag.getByte("mCondition"));
        }
    }

    private void readState(int tickRate, int speed, int condition) {
        this.tickRate = Math.min(TICK_RATE_MAX, Math.max(TICK_RATE_MIN, tickRate));
        this.speed = Math.max(getMinSpeed(), Math.min(getMaxSpeed(), speed));
        this.condition = condition >= 0 && condition < Conditional.VALUES.length
            ? Conditional.VALUES[condition] : Conditional.ALWAYS;
    }

    @Override
    public void readDataFromPacket(ByteArrayDataInput data) {
        readState(data.readInt(), data.readInt(), data.readUnsignedByte());
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("mSpeed", speed);
        tag.setInteger("mTickRate", tickRate);
        tag.setByte("mCondition", (byte) condition.ordinal());
        return tag;
    }

    @Override
    protected void writeDataToByteBuf(ByteBuf buffer) {
        buffer.writeInt(tickRate).writeInt(speed).writeByte(condition.ordinal());
    }

    @Override
    public boolean isRedstoneSensitive(long timer) {
        return condition.isRedstoneSensitive();
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        if (!isValid() || speed == 0 || !condition.isAllowedToWork(coverSide, coverID, tile)
            || !(tile instanceof IFluidHandler)) {
            return;
        }
        IFluidHandler source = speed > 0 ? (IFluidHandler) tile
            : tile.getITankContainerAtSide((byte) coverSide.ordinal());
        IFluidHandler destination = speed > 0 ? tile.getITankContainerAtSide((byte) coverSide.ordinal())
            : (IFluidHandler) tile;
        ForgeDirection drainSide = speed > 0 ? coverSide : coverSide.getOpposite();
        if (source != null && destination != null) {
            allowFluid = true;
            try {
                moveFluid(source, destination, drainSide, Math.abs(speed));
            } finally {
                allowFluid = false;
            }
        }
    }

    /** GTNH GTUtility.moveFluid, kept beside the only caller in this branch. */
    private void moveFluid(IFluidHandler source, IFluidHandler destination, ForgeDirection drainSide, int maximum) {
        if (maximum <= 0) {
            return;
        }
        FluidStack liquid = source.drain(drainSide, maximum, false);
        if (liquid == null) {
            return;
        }
        liquid = liquid.copy();
        liquid.amount = destination.fill(drainSide.getOpposite(), liquid, false);
        if (liquid.amount > 0 && behavior.canTransferFluid(liquid)) {
            destination.fill(drainSide.getOpposite(), source.drain(drainSide, liquid.amount, true), true);
        }
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        int step = player.isSneaking() ? 256 : 16;
        setSpeed((int) Math.max(getMinSpeed(), Math.min(getMaxSpeed(), (long) speed
            + (faceClickedOnRightSide(x, y, z) ? step : -step))));
        player.addChatMessage(getScrewdriverClickChat());
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        if (!isValid()) {
            return false;
        }
        setSpeed((int) Math.max(getMinSpeed(), Math.min(getMaxSpeed(), (long) speed
            + (faceClickedOnRightSide(x, y, z) ? 1 : -1))));
        player.addChatMessage(getScrewdriverClickChat());
        return true;
    }

    private IChatComponent getScrewdriverClickChat() {
        if (Math.abs(speed) == getMaxSpeed()) {
            return new ChatComponentTranslation("GT5U.chat.cover.fluid_regulator.max");
        }
        if (tickRate == 1) {
            return new ChatComponentTranslation("GT5U.chat.cover.fluid_regulator.rate_1", speed, speed * 20);
        }
        return new ChatComponentTranslation(
            "GT5U.chat.cover.fluid_regulator.speed",
            speed,
            tickRate,
            String.format("%.2f", speed * 20d / tickRate)
        );
    }

    public int getMaxSpeed() {
        return (int) Math.min(Integer.MAX_VALUE, (long) transferRate * tickRate);
    }

    public int getMinSpeed() {
        return -getMaxSpeed();
    }

    private boolean faceClickedOnRightSide(float x, float y, float z) {
        return GT_Utility.getClickedFacingCoords((byte) coverSide.ordinal(), x, y, z)[0] >= 0.5F;
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return true;
    }

    @Override
    public boolean letsRedstoneGoOut() {
        return true;
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
    public boolean letsItemsIn(int slot) {
        return true;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return true;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return allowFluid;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return allowFluid;
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return tickRate;
    }

    @Override
    public boolean allowsTickRateAddition() {
        return false;
    }

    public enum Conditional {

        ALWAYS(false) {

            @Override
            boolean isAllowedToWork(ForgeDirection coverSide, int coverID, ICoverable tile) {
                return true;
            }
        },
        CONDITIONAL(true) {

            @Override
            boolean isAllowedToWork(ForgeDirection coverSide, int coverID, ICoverable tile) {
                return !(tile instanceof IMachineProgress) || ((IMachineProgress) tile).isAllowedToWork();
            }
        },
        INVERTED(true) {

            @Override
            boolean isAllowedToWork(ForgeDirection coverSide, int coverID, ICoverable tile) {
                return !(tile instanceof IMachineProgress) || !((IMachineProgress) tile).isAllowedToWork();
            }
        };

        private static final Conditional[] VALUES = values();
        private final boolean redstoneSensitive;

        Conditional(boolean redstoneSensitive) {
            this.redstoneSensitive = redstoneSensitive;
        }

        abstract boolean isAllowedToWork(ForgeDirection coverSide, int coverID, ICoverable tile);

        boolean isRedstoneSensitive() {
            return redstoneSensitive;
        }
    }
}
