package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.IProvideEnergyCover;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicBatteryBuffer;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.cover.CoverEUMeterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.function.ToLongFunction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;

public class CoverEUMeter extends CoverLegacyData implements IInvertable {

    private long threshold;

    public CoverEUMeter(CoverContext context) {
        super(context);
    }

    public EnergyType getType() {
        return EnergyType.getEnergyType(getVariable() / 2);
    }

    public CoverEUMeter setType(EnergyType type) {
        if (isValid() && type != null) {
            setGuiVariable(type.ordinal() * 2 + (isInverted() ? 1 : 0));
        }
        return this;
    }

    @Override
    public boolean isInverted() {
        return (getVariable() & 1) != 0;
    }

    @Override
    public CoverEUMeter setInverted(boolean inverted) {
        if (isValid()) {
            setGuiVariable(getType().ordinal() * 2 + (inverted ? 1 : 0));
        }
        return this;
    }

    public long getThreshold() {
        return threshold;
    }

    public CoverEUMeter setThresdhold(long threshold) {
        if (isValid()) {
            this.threshold = Math.max(0, Math.min(threshold, getType().getTileEntityEnergyCapacity(getTile())));
            setGuiVariable(getVariable());
        }
        return this;
    }

    public int getNum() {
        return getType().ordinal() * 2 + (isInverted() ? 1 : 0);
    }

    public void setNum(int num) {
        if (isValid()) {
            setGuiVariable(EnergyType.getEnergyType(num / 2).ordinal() * 2 + (num % 2 == 1 ? 1 : 0));
        }
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("typeOrdinal", getType().ordinal());
        tag.setBoolean("inverted", isInverted());
        tag.setLong("threshold", threshold);
        return tag;
    }

    @Override
    protected void readDataFromNbt(NBTBase nbt) {
        if (nbt instanceof NBTTagInt) {
            coverData = ((NBTTagInt) nbt).func_150287_d();
            threshold = 0;
        } else if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            coverData = EnergyType.getEnergyType(tag.getInteger("typeOrdinal")).ordinal() * 2
                    + (tag.getBoolean("inverted") ? 1 : 0);
            threshold = Math.max(0, tag.getLong("threshold"));
        }
    }

    @Override
    protected void writeDataToByteBuf(ByteBuf buffer) {
        buffer.writeInt(getType().ordinal());
        buffer.writeBoolean(isInverted());
        buffer.writeLong(threshold);
    }

    @Override
    public void readDataFromPacket(ByteArrayDataInput input) {
        coverData = EnergyType.getEnergyType(input.readInt()).ordinal() * 2 + (input.readBoolean() ? 1 : 0);
        threshold = Math.max(0, input.readLong());
    }

    @Override
    public void doCoverThings(byte inputRedstone, long timer) {
        ICoverable tile = getTile();
        if (tile == null) {
            return;
        }
        long stored = getType().getTileEntityStoredEnergy(tile);
        long capacity = getType().getTileEntityEnergyCapacity(tile);
        byte signal;
        if (stored == 0) {
            signal = 0;
        } else if (stored >= capacity) {
            signal = 15;
        } else {
            long scaled = stored <= Long.MAX_VALUE / 14 ? 14 * stored / capacity
                    : BigInteger.valueOf(stored).multiply(BigInteger.valueOf(14))
                            .divide(BigInteger.valueOf(capacity)).longValue();
            signal = (byte) (1 + scaled);
        }
        if (isInverted()) {
            signal = (byte) (15 - signal);
        }
        if (threshold > 0 && (isInverted() ? stored >= threshold : stored < threshold)) {
            signal = 0;
        }
        tile.setOutputRedstoneSignal((byte) coverSide.ordinal(), signal);
    }

    @Override
    public boolean isRedstoneSensitive(long timer) {
        return false;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverEUMeterGui(this);
    }

    public enum EnergyType {

        UNIVERSAL_STORAGE("universal", ICoverable::getUniversalEnergyStored, ICoverable::getUniversalEnergyCapacity),
        ELECTRICITY_STORAGE("electricity", ICoverable::getStoredEU, ICoverable::getEUCapacity),
        STEAM_STORAGE("steam", ICoverable::getStoredSteam, ICoverable::getSteamCapacity),
        AVERAGE_ELECTRIC_INPUT("average_in", ICoverable::getAverageElectricInput,
                tile -> tile.getInputVoltage() * tile.getInputAmperage()),
        AVERAGE_ELECTRIC_OUTPUT("average_out", ICoverable::getAverageElectricOutput,
                tile -> tile.getOutputVoltage() * tile.getOutputAmperage()),
        ELECTRICITY_STORAGE_INCLUDING_BATTERIES("eu_stored", tile -> batteryEnergy(tile, 0),
                tile -> batteryEnergy(tile, 1));

        private final String key;
        private final ToLongFunction<ICoverable> stored;
        private final ToLongFunction<ICoverable> capacity;

        EnergyType(String key, ToLongFunction<ICoverable> stored, ToLongFunction<ICoverable> capacity) {
            this.key = "gt.interact.desc.Energy_Detector." + key;
            this.stored = stored;
            this.capacity = capacity;
        }

        public String getTitle() {
            return StatCollector.translateToLocal(key);
        }

        public String getTooltip() {
            return StatCollector.translateToLocal(key + ".tooltip");
        }

        public long getTileEntityStoredEnergy(ICoverable tile) {
            IProvideEnergyCover provider = getEnergyProvider(tile);
            if (provider != null) {
                return provider.getStored();
            }
            return stored.applyAsLong(tile);
        }

        public long getTileEntityEnergyCapacity(ICoverable tile) {
            IProvideEnergyCover provider = getEnergyProvider(tile);
            if (provider != null) {
                return provider.getMaxCapacity();
            }
            return capacity.applyAsLong(tile);
        }

        private IProvideEnergyCover getEnergyProvider(ICoverable tile) {
            if ((this == UNIVERSAL_STORAGE || this == ELECTRICITY_STORAGE_INCLUDING_BATTERIES)
                    && tile instanceof IGregTechTileEntity) {
                IMetaTileEntity machine = ((IGregTechTileEntity) tile).getMetaTileEntity();
                if (machine instanceof IProvideEnergyCover) {
                    return (IProvideEnergyCover) machine;
                }
            }
            return null;
        }

        public EnergyType getNext() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static EnergyType getEnergyType(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : UNIVERSAL_STORAGE;
        }

        private static long batteryEnergy(ICoverable tile, int index) {
            if (tile instanceof IGregTechTileEntity) {
                IMetaTileEntity machine = ((IGregTechTileEntity) tile).getMetaTileEntity();
                if (machine instanceof GT_MetaTileEntity_BasicBatteryBuffer) {
                    return ((GT_MetaTileEntity_BasicBatteryBuffer) machine).getStoredEnergy()[index];
                }
            }
            return index == 0 ? tile.getStoredEU() : tile.getEUCapacity();
        }
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 12;
        if (coverData < 0) {
            coverData = 11;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_031", "Normal Universal Storage"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_032", "Inverted Universal Storage"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_033", "Normal Electricity Storage"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_034", "Inverted Electricity Storage"));
            break;

        case 4:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_035", "Normal Steam Storage"));
            break;

        case 5:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_036", "Inverted Steam Storage"));
            break;

        case 6:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_037", "Normal Average Electric Input"));
            break;

        case 7:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_038", "Inverted Average Electric Input"));
            break;

        case 8:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_039", "Normal Average Electric Output"));
            break;

        case 9:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_040", "Inverted Average Electric Output"));
            break;

        case 10:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_041", "Normal Electricity Storage(Including Batteries)"));
            break;

        case 11:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager.addStringLocalization(
                    "Interaction_DESCRIPTION_Index_042", "Inverted Electricity Storage(Including Batteries)"));
            break;

        }
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
    public int getMinimumTickRate() {
        return 20;
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }
}
