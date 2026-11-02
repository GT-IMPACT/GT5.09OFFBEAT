package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.conditions.RedstoneCondition;
import gregtech.common.gui.modularui.cover.CoverControlsWorkGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;

/**
 * Native GTNH cover object, preserving this fork's three machine-controller modes and work data.
 * Source: GTNewHorizons/GT5-Unofficial, f0472cfbd44ab9c8d5a27df7e98c64a42eb1c03f.
 */
public class CoverControlsWork extends Cover {

    private RedstoneCondition condition = RedstoneCondition.ENABLE_WITH_REDSTONE;

    public CoverControlsWork(CoverContext context) {
        this(context, null);
    }

    public CoverControlsWork(CoverContext context, ITexture texture) {
        super(context, texture);
    }

    public RedstoneCondition getRedstoneCondition() {
        return condition;
    }

    public void setRedstoneCondition(RedstoneCondition condition) {
        RedstoneCondition selected = condition == null ? RedstoneCondition.DISABLE : condition;
        if (this.condition == selected) {
            return;
        }
        this.condition = selected;
        setNeedsUpdate(true);
        ICoverable tile = getTile();
        if (tile != null && tile.getWorld() != null && tile.isServerSide()) {
            tile.markDirty();
            tile.getWorld().getChunkFromBlockCoords(tile.getXCoord(), tile.getZCoord()).setChunkModified();
        }
    }

    @Override
    public int getLegacyData() {
        return condition.ordinal();
    }

    @Override
    public void setLegacyData(int value) {
        setRedstoneCondition(RedstoneCondition.fromLegacyData(value));
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("mode", getLegacyData());
        return tag;
    }

    @Override
    protected void readDataFromNbt(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            setLegacyData(((NBTTagCompound) tag).getInteger("mode"));
        } else if (tag instanceof NBTTagInt) {
            setLegacyData(((NBTTagInt) tag).func_150287_d());
        }
    }

    @Override
    protected void writeDataToByteBuf(ByteBuf buffer) {
        buffer.writeInt(getLegacyData());
    }

    @Override
    protected void readDataFromPacket(ByteArrayDataInput input) {
        setLegacyData(input.readInt());
    }

    @Override
    public void doCoverThings(byte inputRedstone, long timer) {
        ICoverable tile = getTile();
        if (!(tile instanceof IMachineProgress)) {
            return;
        }
        IMachineProgress machine = (IMachineProgress) tile;
        boolean enabled = condition != RedstoneCondition.DISABLE
            && (inputRedstone > 0) == (condition == RedstoneCondition.ENABLE_WITH_REDSTONE);
        if (enabled) {
            machine.enableWorking();
        } else {
            machine.disableWorking();
        }
        machine.setWorkDataValue(inputRedstone);
    }

    @Override
    public boolean isRedstoneSensitive(long timer) {
        // Work data retains the incoming signal even when this cover disables the machine.
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
    public int getMinimumTickRate() {
        return 1;
    }

    @Override
    public void onCoverRemoval() {
        ICoverable tile = getTile();
        if (tile instanceof IMachineProgress) {
            IMachineProgress machine = (IMachineProgress) tile;
            machine.enableWorking();
            machine.setWorkDataValue((byte) 0);
        }
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        int next = Math.floorMod(getLegacyData() + (player.isSneaking() ? -1 : 1), 3);
        setLegacyData(next);
        GT_Utility.sendChatToPlayer(player, getDescription());
    }

    @Override
    public String getDescription() {
        return StatCollector.translateToLocal(condition.getTranslationKey());
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverControlsWorkGui(this);
    }
}
