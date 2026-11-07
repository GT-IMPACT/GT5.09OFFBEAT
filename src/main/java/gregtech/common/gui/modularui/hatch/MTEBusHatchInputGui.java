package gregtech.common.gui.modularui.hatch;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import gregtech.api.metatileentity.implementations.GTMTE_BusHatch_Input;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonButtons;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.util.BusHatchInputContainer;
import gregtech.common.gui.modularui.util.MachineFluidTank;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.GTFluidSlot;
import net.minecraftforge.fluids.IFluidTank;

/** The existing combined hatch window, using its original texture and slot coordinates. */
public class MTEBusHatchInputGui {

    private static final String INPUT_GROUP = "bus_hatch_input";
    private static final UITexture BACKGROUND = UITexture.builder()
        .location("gregtech", "gui/BusHatch")
        .imageSize(256, 256)
        .subAreaXYWH(0, 0, 176, 166)
        .build();

    private final GTMTE_BusHatch_Input machine;

    public MTEBusHatchInputGui(GTMTE_BusHatch_Input machine) {
        this.machine = machine;
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        settings.customContainer(BusHatchInputContainer::new);
        syncManager.registerSlotGroup(new SlotGroup(INPUT_GROUP, 4).setAllowSorting(false));
        ModularPanel panel = GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .doesAddTitle(false)
            .doesAddGregTechLogo(false)
            .doesAddGhostCircuitSlot(false)
            .doesBindPlayerInventory(false)
            .build()
            .background(BACKGROUND);

        panel.child(itemSlot(machine.getInputSlot()).name("container_input").pos(7, 61));
        for (int index = 1; index <= 16; index++) {
            panel.child(itemSlot(index)
                .name("input_" + index)
                .pos(79 + (index - 1) % 4 * 18, 7 + (index - 1) / 4 * 18));
        }
        panel.child(itemSlot(machine.getOutputSlot()).name("container_output").pos(43, 61));
        panel.child(CommonWidgets.createCircuitSlot(syncManager, machine).name("circuit").pos(61, 61));
        BooleanSyncValue sorting = new BooleanSyncValue(() -> !machine.disableSort, value -> {
            machine.disableSort = !value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        BooleanSyncValue insertion = new BooleanSyncValue(() -> !machine.disableLimited, value -> {
            machine.disableLimited = !value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        panel.child(CommonButtons.createToggleButton(sorting, GTGuiTextures.OVERLAY_BUTTON_SORTING_MODE, "GT5U.machines.sorting_mode.tooltip")
            .name("sorting").size(18).pos(153, 7));
        panel.child(CommonButtons.createToggleButton(insertion, GTGuiTextures.OVERLAY_BUTTON_ONE_STACK_LIMIT, "GT5U.machines.one_stack_limit.tooltip")
            .name("insertion_limit").size(18).pos(153, 25));

        IFluidTank tank = new MachineFluidTank(machine, false);
        panel.child(new GTFluidSlot()
            .name("fluid_display")
            .syncHandler(new FluidSlotSyncHandler(tank)
                .controlsAmount(false)
                .canFillSlot(false)
                .canDrainSlot(false))
            .alwaysShowFull(true)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .disableThemeBackground(true)
            .pos(54, 31));
        panel.child(IKey.str("Fluid Amount").asWidget()
            .name("fluid_amount_label")
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE)
            .color(16448255)
            .pos(10, 10));
        panel.child(IKey.dynamic(() -> GT_Utility.parseNumberToString(tank.getFluidAmount())).asWidget()
            .name("fluid_amount")
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE)
            .color(16448255)
            .pos(10, 20));
        panel.child(SlotGroupWidget.playerInventory((index, slot) -> slot
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .disableThemeBackground(true))
            .pos(7, 83));
        return panel;
    }

    private ItemSlot itemSlot(int index) {
        return new ItemSlot().slot(new MachineModularSlot(
            machine.getInventoryHandler(), index, machine.getBaseMetaTileEntity()
        ).slotGroup(INPUT_GROUP).canPut(index != machine.getOutputSlot()))
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .disableThemeBackground(true);
    }
}
