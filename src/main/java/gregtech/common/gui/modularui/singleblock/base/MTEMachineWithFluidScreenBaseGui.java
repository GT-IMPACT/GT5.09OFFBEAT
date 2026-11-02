package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_TieredMachineBlock;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.modularui2.widget.GTFluidSlot;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import java.util.function.Predicate;

/**
 * GTNH fluid screen and container I/O column shared by tanks and generators.
 * Source: GTNewHorizons/GT5-Unofficial, f0472cfbd44ab9c8d5a27df7e98c64a42eb1c03f.
 */
public class MTEMachineWithFluidScreenBaseGui<T extends GT_MetaTileEntity_TieredMachineBlock>
    extends MTETieredMachineBlockBaseGui<T> {

    protected static final int FLUID_SCREEN_WIDTH = 71;
    protected static final int FLUID_SCREEN_HEIGHT = 54;

    public MTEMachineWithFluidScreenBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.registerSlotGroup("fluid_inv", 1);
    }

    protected boolean supportsFluidScreen() {
        return true;
    }

    protected boolean supportsFluidIOColumn() {
        return true;
    }

    protected Predicate<FluidStack> getFluidSlotFilter() {
        return fluid -> true;
    }

    protected boolean canFillFluidSlot() {
        return true;
    }

    protected boolean canDrainFluidSlot() {
        return true;
    }

    protected ParentWidget<?> createScreen(ModularPanel panel, PanelSyncManager syncManager, IFluidTank tank) {
        ParentWidget<?> screen = CommonWidgets.createFluidScreen(FLUID_SCREEN_WIDTH, FLUID_SCREEN_HEIGHT);
        Flow text = Flow.column().coverChildren().childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(IKey.lang("gt.ui.tank.amount").asWidget().widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE))
            .child(IKey.dynamic(() -> GT_Utility.formatNumbers(tank.getFluidAmount()))
                .asWidget().widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE));
        return screen.child(text).child(createFluidSlot(panel, syncManager, tank));
    }

    protected FluidSlot createFluidSlot(ModularPanel panel, PanelSyncManager syncManager, IFluidTank tank) {
        return new GTFluidSlot().syncHandler(new FluidSlotSyncHandler(tank).filter(getFluidSlotFilter())
            .canFillSlot(canFillFluidSlot()).canDrainSlot(canDrainFluidSlot()))
            .bottomRel(0).rightRel(0).background(GTGuiTextures.SLOT_FLUID_TANK);
    }

    protected Flow createIO(
        ModularPanel panel,
        PanelSyncManager syncManager,
        int inputSlot,
        int outputSlot,
        IFluidTank tank
    ) {
        return Flow.column().coverChildrenWidth().height(FLUID_SCREEN_HEIGHT)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(createInputSlot(panel, syncManager, inputSlot))
            .child(new GTFluidSlot().size(16)
                .syncHandler(new FluidSlotSyncHandler(tank)
                    .controlsAmount(false).canDrainSlot(false).canFillSlot(false))
                .alwaysShowFull(false).background().hoverBackground().overlay(GTGuiTextures.PICTURE_GAUGE))
            .child(createOutputSlot(panel, syncManager, outputSlot));
    }

    protected ItemSlot createInputSlot(ModularPanel panel, PanelSyncManager syncManager, int index) {
        return new ItemSlot().slot(createMachineSlot(index).slotGroup("fluid_inv"))
            .backgroundOverlay(GTGuiTextures.OVERLAY_SLOT_IN_STANDARD);
    }

    protected ItemSlot createOutputSlot(ModularPanel panel, PanelSyncManager syncManager, int index) {
        return new ItemSlot().slot(createMachineSlot(index).slotGroup("fluid_inv").canPut(false))
            .backgroundOverlay(GTGuiTextures.OVERLAY_SLOT_OUT_STANDARD);
    }
}
