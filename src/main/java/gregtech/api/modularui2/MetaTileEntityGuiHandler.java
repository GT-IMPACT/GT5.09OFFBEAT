package gregtech.api.modularui2;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

/** Adapted from GTNH's MetaTileEntityGuiHandler; retains this branch's ownership checks. */
public final class MetaTileEntityGuiHandler extends AbstractUIFactory<PosGuiData> {

    public static final MetaTileEntityGuiHandler INSTANCE = new MetaTileEntityGuiHandler();
    public static final int MAX_INTERACTION_DISTANCE = 64;

    private MetaTileEntityGuiHandler() {
        super("gregtech:mte");
    }

    public static boolean open(EntityPlayer player, IMetaTileEntity machine) {
        if (player == null || machine == null || player instanceof FakePlayer || !machine.useMui2()) {
            return false;
        }
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() == null || player.worldObj != tile.getWorld()) {
            return false;
        }
        if (tile.isClientSide()) {
            return true;
        }
        if (!(player instanceof EntityPlayerMP) || !tile.isUseableByPlayer(player)) {
            return false;
        }
        PosGuiData data = new PosGuiData(player, tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        GuiManager.open(INSTANCE, data, (EntityPlayerMP) player);
        return true;
    }

    @Override
    public IGuiHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        TileEntity tile = data.getTileEntity();
        if (tile instanceof IGregTechTileEntity) {
            IMetaTileEntity machine = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (machine != null && machine.useMui2()) {
                return machine;
            }
        }
        throw new IllegalStateException("No MUI2 machine at " + data.getX() + ", " + data.getY() + ", " + data.getZ());
    }

    @Override
    public ModularPanel createPanel(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        IMetaTileEntity machine = (IMetaTileEntity) getGuiHolder(data);
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        ModularPanel panel = machine.buildUI(data, syncManager, settings);
        syncManager.addOpenListener(player -> {
            if (!syncManager.isClient()) {
                tile.openInventory();
            }
        });
        syncManager.addCloseListener(player -> {
            if (!syncManager.isClient()) {
                tile.closeInventory();
                GTGuis.markDirty(machine);
            }
        });
        // A replacement block must never inherit handlers that still point to the removed machine.
        settings.canInteractWith(player -> canInteractWith(player, data)
                && data.getTileEntity() == tile && tile.getMetaTileEntity() == machine);
        return panel;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, PosGuiData data) {
        if (player == null || player instanceof FakePlayer || !super.canInteractWith(player, data)
                || player.worldObj != data.getWorld() || data.getSquaredDistance(player) >= MAX_INTERACTION_DISTANCE) {
            return false;
        }
        TileEntity tile = data.getTileEntity();
        if (!(tile instanceof IGregTechTileEntity)) {
            return false;
        }
        IGregTechTileEntity machineTile = (IGregTechTileEntity) tile;
        IMetaTileEntity machine = machineTile.getMetaTileEntity();
        return machine != null && machine.useMui2() && !tile.isInvalid()
                && machineTile.isUseableByPlayer(player);
    }

    @Override
    public void writeGuiData(PosGuiData data, PacketBuffer buffer) {
        buffer.writeVarIntToBuffer(data.getX());
        buffer.writeVarIntToBuffer(data.getY());
        buffer.writeVarIntToBuffer(data.getZ());
    }

    @Override
    public PosGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new PosGuiData(
                player,
                buffer.readVarIntFromBuffer(),
                buffer.readVarIntFromBuffer(),
                buffer.readVarIntFromBuffer()
        );
    }
}
