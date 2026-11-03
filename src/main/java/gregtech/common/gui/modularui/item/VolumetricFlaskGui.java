package gregtech.common.gui.modularui.item;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import gregtech.api.util.GT_Utility;
import gregtech.common.items.GT_VolumetricFlask;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/** GTNH's compact volume editor, bound to the flask's player inventory slot. */
public class VolumetricFlaskGui {

    private final PlayerInventoryGuiData data;
    private final GT_VolumetricFlask flask;

    public VolumetricFlaskGui(PlayerInventoryGuiData data) {
        this.data = data;
        ItemStack stack = data.getUsedItemStack();
        if (stack == null || !(stack.getItem() instanceof GT_VolumetricFlask)) {
            throw new IllegalArgumentException("Tried to open the volumetric flask GUI without a flask");
        }
        flask = (GT_VolumetricFlask) stack.getItem();
    }

    public ModularPanel build() {
        IntSyncValue capacity = new IntSyncValue(
            () -> flask.getCapacity(data.getUsedItemStack()),
            value -> flask.setCapacity(data.getUsedItemStack(), value)
        ).allowC2S();
        ModularPanel panel = new ModularPanel("volumetricFlask").size(150, 50)
            .child(ButtonWidget.panelCloseButton());
        Flow mainColumn = Flow.col()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(10)
            .marginTop(5)
            .marginLeft(5);
        mainColumn.child(IKey.str(data.getUsedItemStack().getDisplayName()).asWidget());
        Flow textRow = Flow.row()
            .coverChildren()
            .childPadding(2);
        int maxCapacity = flask.getMaxCapacity();
        textRow.child(new TextFieldWidget()
            .name("flask_capacity")
            .width(80)
            .value(capacity)
            .formatAsInteger(true)
            .numbersInt(1, maxCapacity)
            .setMaxLength(10)
            .tooltipShowUpTimer(5)
            .tooltip(tooltip -> tooltip.addLine(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC
                + StatCollector.translateToLocalFormatted(
                    "GT5U.gui.text.volumetric_flask.max_capacity", GT_Utility.formatNumbers(maxCapacity)
                ))));
        textRow.child(IKey.lang("GT5U.gui.text.volumetric_flask.capacity").asWidget());
        mainColumn.child(textRow);
        panel.child(mainColumn);
        return panel;
    }
}
