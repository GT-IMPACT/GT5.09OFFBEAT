package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import gregtech.common.modularui2.factory.SelectItemGuiBuilder;
import net.minecraft.network.PacketBuffer;

/** Server selection action registered in the parent panel before the selector is opened. */
public class SelectItemServerAction extends SyncHandler<SelectItemServerAction> {

    private final SelectItemGuiBuilder.IOnSelectedAction onSelected;
    private int choiceCount;
    private boolean allowDeselected;

    public SelectItemServerAction(SelectItemGuiBuilder.IOnSelectedAction onSelected) {
        this.onSelected = onSelected;
        allowC2S();
    }

    public void setSelectionBounds(int choiceCount, boolean allowDeselected) {
        if (choiceCount < 1) {
            throw new IllegalArgumentException("Selection must contain at least one choice");
        }
        this.choiceCount = choiceCount;
        this.allowDeselected = allowDeselected;
    }

    public void send(int selected, MouseData mouseData) {
        syncToServer(0, buffer -> {
            buffer.writeVarIntToBuffer(selected);
            mouseData.writeToPacket(buffer);
        });
    }

    @Override
    public void readOnClient(int id, PacketBuffer buffer) {
    }

    @Override
    public void readOnServer(int id, PacketBuffer buffer) {
        if (id != 0) {
            return;
        }
        int selected = buffer.readVarIntFromBuffer();
        MouseData mouseData = MouseData.readPacket(buffer);
        boolean validSelection = selected >= 0 && selected < choiceCount
            || allowDeselected && selected == SelectItemGuiBuilder.DESELECTED;
        if (validSelection && (mouseData.mouseButton == 0 || mouseData.mouseButton == 1)) {
            onSelected.accept(selected, mouseData);
        }
    }
}
