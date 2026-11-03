package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus_ME;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.gui.modularui.cover.CoverItemMeterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_DigitalChestBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;

/** GTNH item detector, retaining the legacy slot/inversion bits for old-world migration. */
public class CoverItemMeter extends CoverLegacyBehavior {

    private static final int SLOT_MASK = 0x03FFFFFF;
    private static final int CONVERTED_BIT = 0x80000000;
    private static final int INVERT_BIT = 0x40000000;

    private int threshold;

    public CoverItemMeter(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
        coverData = CONVERTED_BIT;
        tickRateAddition = 4;
    }

    public int getSlot() {
        return (normalizedVariable() & SLOT_MASK) - 1;
    }

    public CoverItemMeter setSlot(int slot) {
        if (isValid()) {
            int maximum = Math.min(SLOT_MASK - 1, getTile().getSizeInventory() - 1);
            int selected = Math.max(-1, Math.min(slot, maximum));
            setGuiVariable(CONVERTED_BIT | (normalizedVariable() & INVERT_BIT) | (selected + 1));
        }
        return this;
    }

    public boolean isInverted() {
        return (normalizedVariable() & INVERT_BIT) != 0;
    }

    public CoverItemMeter setInverted(boolean inverted) {
        if (isValid()) {
            setGuiVariable(CONVERTED_BIT | (normalizedVariable() & SLOT_MASK) | (inverted ? INVERT_BIT : 0));
        }
        return this;
    }

    public int getThreshold() {
        return threshold;
    }

    public CoverItemMeter setThreshold(int threshold) {
        if (isValid()) {
            this.threshold = Math.max(0, threshold);
            setGuiVariable(getVariable());
        }
        return this;
    }

    private int normalizedVariable() {
        int value = getVariable();
        if ((value & CONVERTED_BIT) != 0) {
            return value;
        }
        if (value == 0) {
            return CONVERTED_BIT;
        }
        if (value == 1) {
            return CONVERTED_BIT | INVERT_BIT;
        }
        return CONVERTED_BIT | Math.min(value - 2, SLOT_MASK);
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("invert", isInverted());
        tag.setInteger("slot", getSlot());
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
            coverData = CONVERTED_BIT | (tag.getBoolean("invert") ? INVERT_BIT : 0)
                | Math.max(0, Math.min(SLOT_MASK, tag.getInteger("slot") + 1));
            threshold = Math.max(0, tag.getInteger("threshold"));
        }
    }

    @Override
    protected void writeDataToByteBuf(ByteBuf buffer) {
        buffer.writeBoolean(isInverted());
        buffer.writeInt(getSlot());
        buffer.writeInt(threshold);
    }

    @Override
    public void readDataFromPacket(ByteArrayDataInput input) {
        boolean inverted = input.readBoolean();
        int slot = input.readInt();
        coverData = CONVERTED_BIT | (inverted ? INVERT_BIT : 0) | Math.max(0, Math.min(SLOT_MASK, slot + 1));
        threshold = Math.max(0, input.readInt());
    }

    public static byte computeSignalBasedOnItems(
        ICoverable tile,
        boolean inverted,
        int threshold,
        int slot,
        int ordinalSide
    ) {
        long maximum = 0;
        long used = 0;
        IMetaTileEntity machine = tile instanceof IGregTechTileEntity
            ? ((IGregTechTileEntity) tile).getMetaTileEntity() : null;
        if (machine instanceof GT_MetaTileEntity_DigitalChestBase) {
            GT_MetaTileEntity_DigitalChestBase chest = (GT_MetaTileEntity_DigitalChestBase) machine;
            maximum = chest.getMaxItemCount();
            used = chest.getProgresstime();
        } else if (machine instanceof GT_MetaTileEntity_Hatch_OutputBus_ME) {
            GT_MetaTileEntity_Hatch_OutputBus_ME bus = (GT_MetaTileEntity_Hatch_OutputBus_ME) machine;
            if (bus.getMEOutputStorage().canAccept(bus.getMEOutputAmount())) {
                maximum = 64;
            }
        } else {
            int[] slots = slot >= 0 ? new int[]{slot} : tile.getAccessibleSlotsFromSide(ordinalSide);
            for (int index : slots) {
                if (index >= 0 && index < tile.getSizeInventory()) {
                    maximum += 64;
                    ItemStack stack = tile.getStackInSlot(index);
                    if (stack != null) {
                        used += ((long) stack.stackSize << 6) / stack.getMaxStackSize();
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
            tile.setOutputRedstoneSignal((byte) coverSide.ordinal(), computeSignalBasedOnItems(
                tile,
                isInverted(),
                threshold,
                getSlot(),
                coverSide.ordinal()
            ));
        }
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        if (isValid()) {
            coverData = normalizedVariable();
            super.onCoverScrewdriverClick(player, x, y, z);
        }
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
        return new CoverItemMeterGui(this);
    }
}
