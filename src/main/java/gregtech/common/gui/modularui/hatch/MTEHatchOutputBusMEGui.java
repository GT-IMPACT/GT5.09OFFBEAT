package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.metatileentity.implementations.GT_MEOutputStorage;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus_ME;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

/** GTNH's ME output controls, bound to the existing cell and buffered-output provider. */
public class MTEHatchOutputBusMEGui extends MTEHatchBaseGui<GT_MetaTileEntity_Hatch_OutputBus_ME> {

    public MTEHatchOutputBusMEGui(GT_MetaTileEntity_Hatch_OutputBus_ME hatch) {
        super(hatch);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        GT_MEOutputStorage<?> provider = machine.getMEOutputStorage();
        IntSyncValue prioritySyncer = new IntSyncValue(provider::getPriority, provider::setPriority).allowC2S();
        BooleanSyncValue isCaching = new BooleanSyncValue(provider::isCacheMode, provider::setCacheMode).allowC2S();
        BooleanSyncValue isChecking = new BooleanSyncValue(provider::getCheckMode, provider::setCheckMode).allowC2S();

        Flow mainRow = Flow.row()
            .coverChildren()
            .verticalCenter();

        // drive slot
        mainRow.child(new ItemSlot().name("me_storage_cell")
            .slot(createMachineSlot(0).singletonSlotGroup()));

        // check mode toggle
        mainRow.child(
            new ToggleButton()
                .name("me_check_mode")
                .value(isChecking)
                .overlay(GuiTextures.SEARCH)
                .addTooltipLine(IKey.lang("GT5U.hatch.outputme.toggle_checking")));

        // caching mode toggle
        mainRow.child(
            new ToggleButton()
                .name("me_cache_mode")
                .value(isCaching)
                .overlay(GuiTextures.FOLDER)
                .addTooltipLine(IKey.lang("GT5U.hatch.outputme.toggle_caching")));

        // priority input text field
        mainRow.child(
            new TextFieldWidget()
                .name("me_priority")
                .size(75, 14)
                .formatAsInteger(true)
                .value(prioritySyncer)
                .numbersInt(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setMaxLength(10)
                .tooltip(t -> t.addLine(IKey.lang("gui.appliedenergistics2.Priority")))
                .setEnabledIf(t -> isCaching.getBoolValue())
                .marginLeft(5));

        return super.createContentSection(panel, syncManager).child(mainRow);
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return true;
    }
}
