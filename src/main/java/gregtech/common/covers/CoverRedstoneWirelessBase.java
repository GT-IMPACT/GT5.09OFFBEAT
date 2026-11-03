package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.cover.CoverRedstoneWirelessBaseGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.StatCollector;

/** GTNH wireless channels and private-channel flag, retaining all four local transmitter/receiver behaviors. */
public class CoverRedstoneWirelessBase extends CoverLegacyBehavior {

    public static final int MAX_CHANNEL = 65535;
    public static final int CHANNEL_MASK = 0x0000FFFF;
    public static final int CHECKBOX_MASK = 0x00010000;

    private int frequency;
    private boolean privateChannel;

    public CoverRedstoneWirelessBase(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        int bounded = Math.max(0, Math.min(MAX_CHANNEL, frequency));
        if (isValid() && this.frequency != bounded) {
            processCoverData(bounded, privateChannel);
            markGuiChanged();
        }
    }

    public boolean isPrivateChannel() {
        return privateChannel;
    }

    public void setPrivateChannel(boolean privateChannel) {
        if (isValid() && this.privateChannel != privateChannel) {
            processCoverData(frequency, privateChannel);
            markGuiChanged();
        }
    }

    @Override
    public int getLegacyData() {
        return frequency | (privateChannel ? CHECKBOX_MASK : 0);
    }

    @Override
    public int getVariable() {
        return getLegacyData();
    }

    @Override
    public CoverLegacyData setVariable(int value) {
        setLegacyData(value);
        return this;
    }

    @Override
    public void setLegacyData(int value) {
        processCoverData(value & CHANNEL_MASK, (value & CHECKBOX_MASK) != 0);
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("frequency", frequency);
        tag.setBoolean("privateChannel", privateChannel);
        return tag;
    }

    @Override
    protected void readDataFromNbt(NBTBase nbt) {
        if (nbt instanceof NBTTagInt) {
            setLegacyData(((NBTTagInt) nbt).func_150287_d());
        } else if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            processCoverData(tag.getInteger("frequency"), tag.getBoolean("privateChannel"));
        }
    }

    @Override
    public void readDataFromPacket(ByteArrayDataInput input) {
        processCoverData(input.readInt(), input.readBoolean());
    }

    @Override
    protected void writeDataToByteBuf(ByteBuf buffer) {
        buffer.writeInt(frequency);
        buffer.writeBoolean(privateChannel);
    }

    private void processCoverData(int frequency, boolean privateChannel) {
        if (this.frequency == frequency && this.privateChannel == privateChannel) {
            return;
        }
        GregTech_API.sWirelessRedstone.remove(getMapFrequency());
        this.frequency = frequency;
        this.privateChannel = privateChannel;
        coverData = getLegacyData();
        ICoverable tile = getTile();
        if (tile != null) {
            GregTech_API.sWirelessRedstone.put(
                getMapFrequency(),
                tile.getOutputRedstoneSignal((byte) coverSide.ordinal())
            );
        }
    }

    private int getMapFrequency() {
        return frequency + (privateChannel ? MAX_CHANNEL + 1 : 0);
    }

    private byte getWirelessSignal() {
        Byte signal = GregTech_API.sWirelessRedstone.get(getMapFrequency());
        return signal == null ? 0 : signal;
    }

    private boolean isTransmitter() {
        return getBehavior() instanceof GT_Cover_RedstoneTransmitterExternal
            || getBehavior() instanceof GT_Cover_RedstoneTransmitterInternal;
    }

    @Override
    public void doCoverThings(byte inputRedstone, long timer) {
        ICoverable tile = getTile();
        if (tile == null) {
            return;
        }
        if (getBehavior() instanceof GT_Cover_RedstoneTransmitterExternal) {
            GregTech_API.sWirelessRedstone.put(getMapFrequency(), inputRedstone);
        } else if (getBehavior() instanceof GT_Cover_RedstoneTransmitterInternal) {
            GregTech_API.sWirelessRedstone.put(
                getMapFrequency(),
                tile.getOutputRedstoneSignal((byte) coverSide.ordinal())
            );
        } else if (getBehavior() instanceof GT_Cover_RedstoneReceiverExternal) {
            tile.setOutputRedstoneSignal((byte) coverSide.ordinal(), getWirelessSignal());
        } else if (getBehavior() instanceof GT_Cover_RedstoneReceiverInternal && tile instanceof IMachineProgress) {
            IMachineProgress machine = (IMachineProgress) tile;
            if (getWirelessSignal() > 0) {
                machine.enableWorking();
            } else {
                machine.disableWorking();
            }
            machine.setWorkDataValue(inputRedstone);
        }
    }

    @Override
    public byte getRedstoneInput(byte input) {
        return getBehavior() instanceof GT_Cover_RedstoneReceiverInternal ? getWirelessSignal()
            : super.getRedstoneInput(input);
    }

    @Override
    public boolean isRedstoneSensitive(long timer) {
        return !(getBehavior() instanceof GT_Cover_RedstoneTransmitterInternal
            || getBehavior() instanceof GT_Cover_RedstoneReceiverExternal);
    }

    @Override
    public boolean canRemove(boolean forced) {
        return true;
    }

    @Override
    public void onCoverRemoval() {
        if (isTransmitter()) {
            GregTech_API.sWirelessRedstone.remove(getMapFrequency());
        }
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        if ((x > 0.375D && x < 0.625D) || (coverSide.offsetX != 0 && y > 0.375D && y < 0.625D)) {
            setFrequency(GT_Utility.stackToInt(player.inventory.getCurrentItem()) & CHANNEL_MASK);
            GT_Utility.sendChatToPlayer(player, getDescription());
            return true;
        }
        return false;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        if ((x > 0.375D && x < 0.625D) || coverSide.offsetX == 0
            || (y > 0.375D && y < 0.625D) || z <= 0.375D || z >= 0.625D) {
            float[] coordinates = GT_Utility.getClickedFacingCoords((byte) coverSide.ordinal(), x, y, z);
            int adjustment;
            switch ((int) (coordinates[0] * 2) + 2 * (int) (coordinates[1] * 2)) {
                case 0:
                    adjustment = -32;
                    break;
                case 1:
                    adjustment = 32;
                    break;
                case 2:
                    adjustment = -1024;
                    break;
                case 3:
                    adjustment = 1024;
                    break;
                default:
                    adjustment = 0;
                    break;
            }
            setFrequency(frequency + adjustment);
        }
        GT_Utility.sendChatToPlayer(player, getDescription());
    }

    @Override
    public String getDescription() {
        return StatCollector.translateToLocalFormatted("gt.interact.desc.freq_format", frequency, privateChannel);
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverRedstoneWirelessBaseGui(this);
    }
}
