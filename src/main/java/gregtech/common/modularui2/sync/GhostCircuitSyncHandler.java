package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

/** GTNH virtual circuit synchronization with bounded client selections and server-owned inventory state. */
public class GhostCircuitSyncHandler extends PhantomItemSlotSH {

    public static final int SYNC_CIRCUIT_CONFIG = 10;

    private final IMetaTileEntity machine;
    private final IntSyncValue indexSync;

    public GhostCircuitSyncHandler(ModularSlot slot, IMetaTileEntity machine) {
        super(slot);
        this.machine = machine;
        GhostCircuitItemStackHandler handler = getGhostCircuitHandler();
        indexSync = new IntSyncValue(handler::getCircuitConfig, index -> {
            if (index >= GhostCircuitItemStackHandler.NO_CONFIG && index <= GhostCircuitItemStackHandler.MAX_CONFIG) {
                handler.setCircuitConfig(index);
            }
        });
        indexSync.allowC2S(false);
    }

    public void registerIndexSync(PanelSyncManager syncManager, String key) {
        syncManager.syncValue(key, indexSync);
    }

    @Override
    protected void phantomClick(MouseData mouseData, ItemStack cursorStack) {
        if (mouseData.mouseButton != 0 && mouseData.mouseButton != 1) {
            return;
        }
        if (mouseData.mouseButton == 1 && mouseData.shift) {
            setCircuitConfig(GhostCircuitItemStackHandler.NO_CONFIG);
        } else if (cursorStack != null && cursorStack.getItem() instanceof GT_IntegratedCircuit_Item) {
            setCircuitConfig(cursorStack.getItemDamage());
        } else if (mouseData.mouseButton == 0) {
            setCircuitConfig(getNextCircuitConfig(1));
        } else {
            setCircuitConfig(getNextCircuitConfig(-1));
        }
    }

    @Override
    protected void phantomScroll(MouseData mouseData) {
        if (mouseData.mouseButton != 0) {
            setCircuitConfig(getNextCircuitConfig(Integer.signum(mouseData.mouseButton)));
        }
    }

    @Override
    public void readOnServer(int id, PacketBuffer buffer) throws IOException {
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        if (!isValid() || tile == null || tile.getMetaTileEntity() != machine
            || !tile.isUseableByPlayer(getSyncManager().getPlayer())) {
            return;
        }
        if (id == SYNC_CIRCUIT_CONFIG) {
            setCircuitConfig(buffer.readShort());
        } else {
            super.readOnServer(id, buffer);
        }
    }

    private void setCircuitConfig(int config) {
        if (config != GhostCircuitItemStackHandler.NO_CONFIG
            && (config < 0 || config > GhostCircuitItemStackHandler.MAX_CONFIG)) {
            return;
        }
        GhostCircuitItemStackHandler handler = getGhostCircuitHandler();
        if (handler.getCircuitConfig() != config) {
            handler.setCircuitConfig(config);
            forceSyncItem();
        }
    }

    private int getNextCircuitConfig(int delta) {
        int current = getGhostCircuitHandler().getCircuitConfig();
        if (current == GhostCircuitItemStackHandler.NO_CONFIG) {
            return delta > 0 ? 0 : GhostCircuitItemStackHandler.MAX_CONFIG;
        }
        int next = current + delta;
        return next < 0 || next > GhostCircuitItemStackHandler.MAX_CONFIG
            ? GhostCircuitItemStackHandler.NO_CONFIG : next;
    }

    public GhostCircuitItemStackHandler getGhostCircuitHandler() {
        IItemHandler handler = getSlot().getItemHandler();
        if (!(handler instanceof GhostCircuitItemStackHandler)) {
            throw new IllegalStateException("Virtual circuit sync requires GhostCircuitItemStackHandler");
        }
        return (GhostCircuitItemStackHandler) handler;
    }

    public IntSyncValue getIndexSync() {
        return indexSync;
    }
}
