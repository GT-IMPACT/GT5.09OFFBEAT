package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.modularui2.factory.SelectItemGuiBuilder;
import gregtech.common.modularui2.sync.GhostCircuitSyncHandler;

/** GTNH virtual circuit slot with scrolling, cursor sampling and a synchronized selection popup. */
public class GhostCircuitSlotWidget extends PhantomItemSlot {

    private static final String GUI_ID = "circuit_selector";

    private final IMetaTileEntity machine;
    private final IPanelHandler selectorPanel;
    private final PanelSyncManager syncManager;
    private GhostCircuitSyncHandler circuitSyncHandler;

    public GhostCircuitSlotWidget(IMetaTileEntity machine, PanelSyncManager syncManager) {
        this.machine = machine;
        this.syncManager = syncManager;
        tooltipBuilder(this::buildCircuitTooltip);
        tooltipAutoUpdate(true);
        selectorPanel = buildSelectorPanel();
        slot(new ModularSlot(new GhostCircuitItemStackHandler(machine), 0));
    }

    @Override
    public IDrawable getCurrentBackground(WidgetThemeEntry<?> widgetTheme) {
        return new DrawableStack(super.getCurrentBackground(widgetTheme), GTGuiTextures.OVERLAY_SLOT_INT_CIRCUIT);
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (mouseButton != 0 && mouseButton != 1) {
            return Result.IGNORE;
        }
        if (!selectorPanel.isPanelOpen()) {
            if (mouseButton == 0 && Interactable.hasShiftDown()) {
                selectorPanel.openPanel();
            } else {
                MouseData mouse = MouseData.create(mouseButton);
                getSyncHandler().syncToServer(PhantomItemSlotSH.SYNC_CLICK, mouse::writeToPacket);
            }
        }
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseScroll(UpOrDown direction, int amount) {
        if (!selectorPanel.isPanelOpen()) {
            MouseData mouse = MouseData.create(direction.modifier);
            getSyncHandler().syncToServer(PhantomItemSlotSH.SYNC_SCROLL, mouse::writeToPacket);
        }
        return true;
    }

    @Override
    public PhantomItemSlot slot(ModularSlot slot) {
        if (circuitSyncHandler != null) {
            throw new IllegalStateException("Virtual circuit slots are bound to their owning machine at construction");
        }
        circuitSyncHandler = new GhostCircuitSyncHandler(slot, machine);
        setSyncOrValue(circuitSyncHandler);
        circuitSyncHandler.registerIndexSync(syncManager, "ghostCircuitIndex");
        return this;
    }

    private void buildCircuitTooltip(RichTooltip tooltip) {
        int config = getSyncHandler().getGhostCircuitHandler().getCircuitConfig();
        String configuration = config == GhostCircuitItemStackHandler.NO_CONFIG
            ? IKey.lang("GT5U.machines.select_circuit.tooltip.configuration.none").get()
            : String.valueOf(config);
        tooltip.clearText()
            .addLine(IKey.lang("GT5U.machines.select_circuit.tooltip"))
            .spaceLine(2)
            .addLine(IKey.lang("GT5U.machines.select_circuit.tooltip.configuration", configuration))
            .addLine(IKey.lang("GT5U.machines.select_circuit.tooltip.1"))
            .addLine(IKey.lang("GT5U.machines.select_circuit.tooltip.2"))
            .addLine(IKey.lang("GT5U.machines.select_circuit.tooltip.3"));
    }

    private IPanelHandler buildSelectorPanel() {
        return syncManager.syncedPanel("ghostCircuitPanel", true, (panelSync, panelHandler) -> {
            ModularPanel panel = GTGuis.createPopUpPanel(GUI_ID);
            return new SelectItemGuiBuilder(panel, GhostCircuitItemStackHandler.getAllConfigurations())
                .setHeaderItem(machine.getStackForm(1))
                .setTitle(IKey.lang("GT5U.machines.select_circuit"))
                .setSelectedSyncHandler(circuitSyncHandler.getIndexSync())
                .setOnSelectedClientAction((selected, mouse) -> {
                    getSyncHandler().syncToServer(GhostCircuitSyncHandler.SYNC_CIRCUIT_CONFIG, buffer -> {
                        buffer.writeShort(selected);
                    });
                    if (mouse.shift) {
                        panel.closeIfOpen();
                    }
                })
                .setCurrentItemSlotOverlay(GTGuiTextures.OVERLAY_SLOT_INT_CIRCUIT)
                .setAllowDeselected(true)
                .build();
        });
    }

    @Override
    public GhostCircuitSyncHandler getSyncHandler() {
        return circuitSyncHandler;
    }
}
