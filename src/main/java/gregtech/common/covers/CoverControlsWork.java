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
 * Machine controller with GTNH's Safe Mode and redstone work data.
 */
public class CoverControlsWork extends Cover {

    private static final int SAFE_MODE_OFFSET = 3;

    private int mode;
    private boolean handledShutdown;

    public CoverControlsWork(CoverContext context) {
        this(context, null);
    }

    public CoverControlsWork(CoverContext context, ITexture texture) {
        super(context, texture);
    }

    public RedstoneCondition getRedstoneCondition() {
        return RedstoneCondition.fromLegacyData(mode % SAFE_MODE_OFFSET);
    }

    public void setRedstoneCondition(RedstoneCondition condition) {
        RedstoneCondition selected = condition == null ? RedstoneCondition.DISABLE : condition;
        setLegacyData(selected.ordinal() + (isSafeMode() ? SAFE_MODE_OFFSET : 0));
    }

    public boolean isSafeMode() {
        return mode >= SAFE_MODE_OFFSET;
    }

    public void setSafeMode(boolean enabled) {
        setLegacyData(getRedstoneCondition().ordinal() + (enabled ? SAFE_MODE_OFFSET : 0));
    }

    @Override
    public int getLegacyData() {
        return mode;
    }

    @Override
    public void setLegacyData(int value) {
        int selected = value >= 0 && value < 2 * SAFE_MODE_OFFSET ? value : RedstoneCondition.DISABLE.ordinal();
        if (mode == selected) {
            return;
        }
        mode = selected;
        setNeedsUpdate(true);
        ICoverable tile = getTile();
        if (tile != null && tile.getWorld() != null && tile.isServerSide()) {
            tile.markDirty();
            tile.getWorld().getChunkFromBlockCoords(tile.getXCoord(), tile.getZCoord()).setChunkModified();
        }
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
        RedstoneCondition condition = getRedstoneCondition();
        if (isSafeMode() && condition != RedstoneCondition.DISABLE
            && machine.wasShutdownByCriticalFailure() && !handledShutdown) {
            handledShutdown = true;
            setLegacyData(RedstoneCondition.DISABLE.ordinal());
            machine.setWorkDataValue(inputRedstone);
            return;
        }
        boolean enabled = condition != RedstoneCondition.DISABLE
            && (inputRedstone > 0) == (condition == RedstoneCondition.ENABLE_WITH_REDSTONE);
        if (enabled != machine.isAllowedToWork()) {
            if (enabled) {
                machine.enableWorking();
                handledShutdown = false;
            } else {
                machine.disableWorking();
            }
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
        int next = (getLegacyData() + (player.isSneaking() ? -1 : 1)) % 5;
        if (next < 0) {
            next = RedstoneCondition.DISABLE.ordinal();
        }
        setLegacyData(next);
        GT_Utility.sendChatToPlayer(player, getDescription());
    }

    @Override
    public String getDescription() {
        String description = StatCollector.translateToLocal(getRedstoneCondition().getTranslationKey());
        return isSafeMode() ? description + " (" + StatCollector.translateToLocal("gt.ui.cover.work.safe_mode") + ")"
            : description;
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
