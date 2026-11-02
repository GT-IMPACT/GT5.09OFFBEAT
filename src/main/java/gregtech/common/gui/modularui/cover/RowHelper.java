package gregtech.common.gui.modularui.cover;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.covers.IInvertable;
import net.minecraft.util.StatCollector;

public final class RowHelper {

    private RowHelper() {
    }

    public static Flow makeInvertRedstoneRow(IInvertable cover) {
        BooleanSyncValue inverted = new BooleanSyncValue(cover::isInverted, cover::setInverted).allowC2S();
        return Flow.row().name("invert_redstone")
            .child(new ToggleButton().value(inverted)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                .size(16))
            .child(IKey.dynamic(() -> inverted.getValue()
                ? StatCollector.translateToLocal("gt.interact.desc.inverted")
                : StatCollector.translateToLocal("gt.interact.desc.normal")).asWidget());
    }
}
