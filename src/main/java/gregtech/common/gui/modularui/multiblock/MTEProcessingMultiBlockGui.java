package gregtech.common.gui.modularui.multiblock;

import codechicken.nei.recipe.GuiCraftingRecipe;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROL_COUNT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_Y;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_WIDTH;
import static gregtech.api.gui.GT_Container_ProcessingMachine.NEI_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_Y;

/** GTNH MUI2 synchronization and slots with the existing processing window's fixed layout. */
public final class MTEProcessingMultiBlockGui {

    public static final int CIRCUIT_SLOT_X = (PLAYER_INVENTORY_X - 1 - 18) / 2 + 1;
    public static final int CIRCUIT_SLOT_Y = PLAYER_INVENTORY_Y + 58;

    private final GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine;

    public MTEProcessingMultiBlockGui(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine) {
        this.machine = machine;
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        ProcessingGuiState state = new ProcessingGuiState(machine, syncManager);
        ProcessingMachineDisplay display = new ProcessingMachineDisplay(machine, state);
        ModularPanel panel = GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .setWidth(GUI_WIDTH)
            .setHeight(GUI_HEIGHT)
            .doesAddTitle(false)
            .doesAddCoverTabs(false)
            .doesAddGregTechLogo(false)
            .doesAddGhostCircuitSlot(false)
            .doesBindPlayerInventory(false)
            .build()
            .background(IDrawable.EMPTY)
            .disableHoverBackground()
            .child(display);
        if (machine.hasControllerSlotInGui()) {
            syncManager.registerSlotGroup("controller", 1);
            panel.child(new ItemSlot()
                .name("processing_controller")
                .slot(new MachineModularSlot(machine.getInventoryHandler(), 1, machine.getBaseMetaTileEntity())
                    .slotGroup("controller"))
                .invisible()
                .pos(CONTROLLER_X - 1, CONTROLLER_Y - 1));
        }
        if (machine.allowSelectCircuit()) {
            panel.child(CommonWidgets.createCircuitSlot(syncManager, machine)
                .name("processing_circuit")
                .pos(machine.getCircuitSlotX() - 1, machine.getCircuitSlotY() - 1));
        }
        panel.child(SlotGroupWidget.playerInventory((index, slot) -> slot.invisible())
            .pos(PLAYER_INVENTORY_X - 1, PLAYER_INVENTORY_Y - 1));
        int controls = CONTROL_COUNT + (machine.getRecipeMaps().size() > 1 ? 1 : 0);
        for (int index = 0; index < controls; index++) {
            panel.child(createControl(index, state, display, syncManager));
        }
        return panel;
    }

    private ButtonWidget<?> createControl(
        int index,
        ProcessingGuiState state,
        ProcessingMachineDisplay display,
        PanelSyncManager syncManager
    ) {
        ButtonWidget<?> button = new ProcessingControlButton()
            .name("processing_control_" + index)
            .size(18)
            .left(() -> ProcessingGuiControls.controlX(machine, index) - 1, Unit.Measure.PIXEL)
            .top(ProcessingGuiControls.controlY(index) - 1)
            .invisible()
            .playClickSound(false)
            .setEnabledIf(widget -> ProcessingGuiControls.isControlVisible(machine, index))
            .tooltipBuilder(tooltip -> {
                tooltip.textColor(0xffffffff).titleMargin(2);
                List<String> lines = display.controlTooltip(index);
                for (String line : lines) {
                    tooltip.addLine(line);
                }
            })
            .tooltipAutoUpdate(true);
        if (index == NEI_SLOT) {
            return button.onMousePressed(mouseButton -> {
                GT_Recipe_Map map = machine.getRecipeMapForNEI();
                if (mouseButton == 0 && state.isDisplayReady() && map != null) {
                    GuiCraftingRecipe.openRecipeGui(map.mNEIName);
                }
                return true;
            });
        }
        InteractionSyncHandler click = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (!mouse.isClient() && canInteract(syncManager)
                && ProcessingGuiControls.click(machine, index, mouse.mouseButton)) {
                state.getSyncValue().notifyUpdate();
            }
        });
        syncManager.syncValue("processingControl_" + index, click);
        return button.syncHandler(click);
    }

    private boolean canInteract(PanelSyncManager syncManager) {
        EntityPlayer player = syncManager.getPlayer();
        IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        return player != null && player.openContainer == syncManager.getContainer()
            && base != null && !base.isDead() && base.getMetaTileEntity() == machine
            && syncManager.getContainer().canInteractWith(player);
    }

    private static final class ProcessingControlButton extends ButtonWidget<ProcessingControlButton> {

        @Override
        public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            if (isHovering()) {
                Gui.drawRect(
                    1,
                    1,
                    17,
                    17,
                    0x80ffffff
                );
            }
        }
    }
}
