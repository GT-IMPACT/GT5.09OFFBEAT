package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.utils.MouseData;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectItemServerActionTest {

    @Test
    public void invalidClientSelectionsCannotReachTheServerCallback() {
        List<Integer> selected = new ArrayList<>();
        SelectItemServerAction action = new SelectItemServerAction((index, mouse) -> selected.add(index));
        action.setSelectionBounds(3, false);

        receive(action, 0, -1, 0);
        receive(action, 0, 3, 0);
        receive(action, 0, Integer.MAX_VALUE, 0);
        receive(action, 0, 1, 2);
        receive(action, 12, 1, 0);
        assertTrue(selected.isEmpty());

        receive(action, 0, 2, 0);
        assertEquals(1, selected.size());
        assertEquals(Integer.valueOf(2), selected.get(0));
    }

    @Test
    public void deselectionRequiresTheServerToEnableIt() {
        List<Integer> selected = new ArrayList<>();
        SelectItemServerAction action = new SelectItemServerAction((index, mouse) -> selected.add(index));
        action.setSelectionBounds(3, true);

        receive(action, 0, -2, 1);
        receive(action, 0, -1, 1);
        assertEquals(1, selected.size());
        assertEquals(Integer.valueOf(-1), selected.get(0));
    }

    private static void receive(
        SelectItemServerAction action,
        int packetId,
        int selection,
        int mouseButton
    ) {
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writeVarIntToBuffer(selection);
            MouseData mouse = new MouseData(
                Side.CLIENT,
                mouseButton,
                false,
                false,
                false
            );
            mouse.writeToPacket(packet);
            action.readOnServer(packetId, packet);
        } finally {
            packet.release();
        }
    }
}
