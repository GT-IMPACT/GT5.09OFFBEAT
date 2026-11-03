package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.common.CommonButtons;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;

/** GTNH's input bus grid, sorting and insertion controls, and virtual circuit selector. */
public class MTEHatchInputBusGui extends MTEHatchBaseGui<GT_MetaTileEntity_Hatch_InputBus> {

    public MTEHatchInputBusGui(GT_MetaTileEntity_Hatch_InputBus machine) {
        super(machine);
    }

    protected int getDimension() {
        return Math.max(1, machine.mTier + 1);
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        BooleanSyncValue sorting = new BooleanSyncValue(() -> !machine.disableSort, value -> {
            machine.disableSort = !value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        BooleanSyncValue insertion = new BooleanSyncValue(() -> !machine.disableLimited, value -> {
            machine.disableLimited = !value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        return super.createBottomLeftCornerFlow(panel, syncManager)
            .child(CommonButtons.createToggleButton(sorting, GTGuiTextures.OVERLAY_BUTTON_SORTING_MODE, "GT5U.machines.sorting_mode.tooltip"))
            .child(CommonButtons.createToggleButton(insertion, GTGuiTextures.OVERLAY_BUTTON_ONE_STACK_LIMIT, "GT5U.machines.one_stack_limit.tooltip"));
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomRightCornerFlow(panel, syncManager)
            .child(CommonWidgets.createCircuitSlot(syncManager, machine));
    }

    @Override
    protected int getBasePanelHeight() {
        int dimension = getDimension();
        return super.getBasePanelHeight() + (dimension > 4 ? (dimension - 3) * SLOT_SIZE : 0);
    }

    @Override
    protected int getBasePanelWidth() {
        return super.getBasePanelWidth() + Math.max(0, SLOT_SIZE * (getDimension() - 9));
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager).child(
            new ItemSlotGridBuilder(machine.getInventoryHandler(), syncManager).size(getDimension())
                .modularSlotSupplier((handler, index) -> createMachineSlot(index))
                .build()
                .center());
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return getDimension() <= 4;
    }
}
