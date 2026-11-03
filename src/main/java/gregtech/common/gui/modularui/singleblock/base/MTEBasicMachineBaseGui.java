package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.common.CommonButtons;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.util.MachineFluidTank;
import gregtech.common.modularui2.widget.GTFluidSlot;
import gregtech.common.modularui2.widget.GTProgressWidget;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

/**
 * Reusable singleblock recipe UI adapted from GTNH's MTEBasicMachineBaseGui.
 * Source: GTNewHorizons/GT5-Unofficial, f0472cfbd44ab9c8d5a27df7e98c64a42eb1c03f.
 * Item and fluid slots are live views of the existing machine, including shared tanks.
 */
public class MTEBasicMachineBaseGui<T extends GT_MetaTileEntity_BasicMachine>
    extends MTETieredMachineBlockBaseGui<T> {

    protected final boolean hasInputFluidSlot;
    protected final boolean hasOutputFluidSlot;
    private int progressDirection;
    private int progressSegments = 1;
    private UITexture progressTexture;

    public MTEBasicMachineBaseGui(T machine, boolean hasInputFluidSlot, boolean hasOutputFluidSlot) {
        super(machine);
        this.hasInputFluidSlot = hasInputFluidSlot && !machine.isSteampowered() && machine.getCapacity() > 0;
        this.hasOutputFluidSlot = hasOutputFluidSlot && !machine.isSteampowered() && machine.getCapacity() > 0;
    }

    public MTEBasicMachineBaseGui<T> progressDirection(int direction) {
        progressDirection = Math.floorMod(direction, 8);
        return this;
    }

    public MTEBasicMachineBaseGui<T> progressSegments(int segments) {
        progressSegments = Math.max(1, segments);
        return this;
    }

    public MTEBasicMachineBaseGui<T> progressTexture(UITexture texture) {
        progressTexture = texture;
        return this;
    }

    @Override
    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        registerSyncValues(syncManager);
        int additionalHeight = Math.max(0, getRecipeRows() - 2) * SLOT_SIZE;
        int controlsY = 62 + additionalHeight;
        ModularPanel panel = GTGuis.mteTemplatePanelBuilder(machine, guiData, syncManager, uiSettings)
            .setWidth(176)
            .setHeight(166 + additionalHeight)
            .doesAddGregTechLogo(false)
            .build();

        ParentWidget<?> content = new ParentWidget<>()
            .pos(0, CommonWidgets.MACHINE_HEADER_HEIGHT)
            .size(176, 166 + additionalHeight);
        content.child(createItemInputSlots(syncManager))
            .child(createProgressBar(syncManager))
            .child(createItemOutputSlots(syncManager))
            .child(createErrorWidget(syncManager).pos(79, controlsY - SLOT_SIZE - 1));
        if (!machine.isSteampowered()) {
            content.child(createAutoOutputButton(syncManager, false).pos(7, controlsY))
                .child(createAutoOutputButton(syncManager, true).pos(25, controlsY))
                .child(createChargerSlot().pos(79, controlsY))
                .child(createSpecialSlot().pos(124, controlsY));
        }
        panel.child(content)
            .child(CommonButtons.createSmallPowerSwitchButton("powerSwitch").top(4).right(4));

        if (hasInputFluidSlot) {
            content.child(createFluidInputSlot().pos(52, controlsY));
        }
        if (hasOutputFluidSlot) {
            content.child(createFluidOutputSlot().pos(106, controlsY));
        }
        return panel;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.registerSlotGroup("item_inv", 3);
        syncManager.syncValue("itemAutoOutput", new BooleanSyncValue(() -> machine.mItemTransfer, value -> {
            machine.mItemTransfer = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
        syncManager.syncValue("fluidAutoOutput", new BooleanSyncValue(() -> machine.mFluidTransfer, value -> {
            machine.mFluidTransfer = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
        syncManager.syncValue("stuttering", new BooleanSyncValue(() -> machine.mStuttering));
        syncManager.syncValue("fluidSwap", new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (!syncManager.isClient() && mouse.shift && (mouse.mouseButton == 0 || mouse.mouseButton == 1)) {
                FluidStack input = machine.getFillableStack();
                FluidStack output = machine.getDrainableStack();
                machine.setFillableStack(output);
                machine.setDrainableStack(input);
                GTGuis.markDirty(machine);
            }
        }));
    }

    protected ParentWidget<?> createItemInputSlots(PanelSyncManager syncManager) {
        return createItemSlots(machine.getInputSlot(), machine.mInputSlotCount, false).pos(16, 15);
    }

    protected ParentWidget<?> createItemOutputSlots(PanelSyncManager syncManager) {
        return createItemSlots(machine.getOutputSlot(), machine.mOutputItems.length, true).pos(106, 15);
    }

    protected ParentWidget<?> createItemSlots(int firstSlot, int count, boolean output) {
        int columns = count == 4 ? 2 : 3;
        int rows = (count + columns - 1) / columns;
        int gridRows = Math.max(2, getRecipeRows());
        ParentWidget<?> group = new ParentWidget<>().size(3 * SLOT_SIZE, gridRows * SLOT_SIZE);
        for (int index = 0; index < count; index++) {
            int row = index / columns;
            int column = index % columns;
            int rowSize = Math.min(columns, count - row * columns);
            if (!output && count == 5) {
                row = index < 2 ? 0 : 1;
                column = index < 2 ? index : index - 2;
                rowSize = row == 0 ? 2 : 3;
            }
            int x = output ? column * SLOT_SIZE : (3 - rowSize + column) * SLOT_SIZE;
            int y = (gridRows - rows) * SLOT_SIZE / 2 + row * SLOT_SIZE;
            ModularSlot slot = createMachineSlot(firstSlot + index).slotGroup("item_inv");
            if (output) {
                slot.accessibility(false, true);
            }
            group.child(new ItemSlot().slot(slot).pos(x, y));
        }
        return group;
    }

    protected int getRecipeRows() {
        return (Math.max(machine.mInputSlotCount, machine.mOutputItems.length) + 2) / 3;
    }

    protected ItemSlot createChargerSlot() {
        return new ItemSlot().slot(createMachineSlot(machine.rechargerSlotStartIndex()).slotGroup("special"))
            .backgroundOverlay(GTGuiTextures.OVERLAY_SLOT_CHARGER)
            .addTooltipLine(text("charger"));
    }

    protected ItemSlot createSpecialSlot() {
        return new ItemSlot().slot(createMachineSlot(3).slotGroup("special"))
            .addTooltipLine(text("special"));
    }

    protected FluidSlot createFluidInputSlot() {
        return new GTFluidSlot().syncHandler(
            new FluidSlotSyncHandler(new MachineFluidTank(machine, false)).filter(machine::isFluidInputAllowed));
    }

    protected FluidSlot createFluidOutputSlot() {
        return new GTFluidSlot().syncHandler(
            new FluidSlotSyncHandler(new MachineFluidTank(machine, true)).canFillSlot(false));
    }

    protected ProgressWidget createProgressBar(PanelSyncManager syncManager) {
        GT_Recipe_Map map = machine.getRecipeList();
        String recipeId = machine.mNEIName;
        if (!GT_Utility.isStringValid(recipeId)) {
            recipeId = map == null ? null : map.mNEIName;
        }
        int additionalHeight = Math.max(0, getRecipeRows() - 2) * SLOT_SIZE;
        return new GTProgressWidget().neiTransferRect(recipeId, new Object[0], text("recipes"))
            .value(new DoubleSyncValue(this::getProgress))
            .texture(getProgressTexture(), 20)
            .direction(getProgressDirection())
            .size(20, 18)
            .pos(78, 24 + additionalHeight / 2);
    }

    protected UITexture getProgressTexture() {
        return progressTexture == null ? GTGuiTextures.PROGRESSBAR_ARROW : progressTexture;
    }

    protected ProgressWidget.Direction getProgressDirection() {
        switch (progressDirection % 4) {
            case 1:
                return ProgressWidget.Direction.LEFT;
            case 2:
                return ProgressWidget.Direction.DOWN;
            case 3:
                return ProgressWidget.Direction.UP;
            default:
                return ProgressWidget.Direction.RIGHT;
        }
    }

    protected double getProgress() {
        if (machine.maxProgresstime() <= 0) {
            return 0;
        }
        double progress = Math.min(1, Math.max(0, (double) machine.getProgresstime() / machine.maxProgresstime()));
        double phase = progress * progressSegments;
        if (progress < 1) {
            phase -= Math.floor(phase);
        } else {
            phase = 1;
        }
        return progressDirection >= 4 ? 1 - phase : phase;
    }

    protected Widget<?> createErrorWidget(PanelSyncManager syncManager) {
        BooleanSyncValue stuttering = syncManager.findSyncHandler("stuttering", BooleanSyncValue.class);
        return GTGuiTextures.OVERLAY_POWER_LOSS.asWidget().size(SLOT_SIZE)
            .setEnabledIf(widget -> stuttering.getBoolValue())
            .addTooltipLine(text("stalled"));
    }

    protected ToggleButton createAutoOutputButton(PanelSyncManager syncManager, boolean items) {
        String key = items ? "itemAutoOutput" : "fluidAutoOutput";
        UITexture overlay = items
            ? GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_ITEM
            : GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_FLUID;
        InteractionSyncHandler fluidSwap = syncManager.findSyncHandler("fluidSwap", InteractionSyncHandler.class);
        ToggleButton button = new ToggleButton() {

            @Override
            public Result onMousePressed(int mouseButton) {
                if (mouseButton != 0 && mouseButton != 1) {
                    return Result.IGNORE;
                }
                if (!items && Interactable.hasShiftDown()) {
                    fluidSwap.onMousePressed(mouseButton);
                    return Result.SUCCESS;
                }
                return super.onMousePressed(mouseButton);
            }
        }.value(syncManager.findSyncHandler(key, BooleanSyncValue.class))
            .overlay(overlay)
            .size(SLOT_SIZE)
            .addTooltipLine(text(items ? "auto_items" : "auto_fluids"));
        if (!items) {
            button.addTooltipLine(text("swap_fluids"));
        }
        return button;
    }

    private static String text(String key) {
        return StatCollector.translateToLocal("gt.ui.singleblock." + key);
    }
}
