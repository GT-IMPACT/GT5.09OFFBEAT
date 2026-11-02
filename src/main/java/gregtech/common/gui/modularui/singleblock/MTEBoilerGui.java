package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonButtons;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.GTFluidSlot;
import gregtech.common.modularui2.widget.GTProgressWidget;
import gregtech.common.tileentities.boilers.GT_MetaTileEntity_Boiler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

/** GTNH's three-column boiler layout, retaining this branch's fuel slot and gauge scales. */
public class MTEBoilerGui {

    private final GT_MetaTileEntity_Boiler machine;

    public MTEBoilerGui(GT_MetaTileEntity_Boiler machine) {
        this.machine = machine;
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup("item_inv", 1);
        syncManager.syncValue("powerSwitch", new BooleanSyncValue(
            machine.getBaseMetaTileEntity()::isAllowedToWork,
            enabled -> {
                if (enabled) {
                    machine.getBaseMetaTileEntity().enableWorking();
                } else {
                    machine.getBaseMetaTileEntity().disableWorking();
                }
                GTGuis.markDirty(machine);
            }
        ).allowC2S());
        DoubleSyncValue heat = new DoubleSyncValue(() -> Math.min(
            1,
            Math.max(0, (double) machine.mTemperature / Math.max(1, machine.maxProgresstime() - 10))
        ));
        syncManager.syncValue("heat", heat);
        IWidget waterSlots = Flow.column().coverChildren()
            .child(itemSlot(0, false).widgetTheme(GTWidgetThemes.OVERLAY_ITEM_SLOT_IN))
            .child(new Widget<>().widgetTheme(GTWidgetThemes.PICTURE_CANISTER).size(18))
            .child(itemSlot(1, true).widgetTheme(GTWidgetThemes.OVERLAY_ITEM_SLOT_OUT));
        IWidget indicators = Flow.row().coverChildren().childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(fluidGauge(true))
            .child(fluidGauge(false))
            .child(new GTProgressWidget().value(heat)
                .direction(ProgressWidget.Direction.UP)
                .widgetTheme(GTWidgetThemes.PROGRESSBAR_BOILER_HEAT)
                .size(10, 54));
        IWidget fuelSlots = Flow.column().coverChildren()
            .child(itemSlot(3, true).widgetTheme(GTWidgetThemes.OVERLAY_ITEM_SLOT_DUST))
            .child(new GTProgressWidget().value(new DoubleSyncValue(() -> {
                if (machine.mProcessingEnergy <= 0) {
                    return 0;
                }
                return Math.min(1, Math.max(1d / 14, (double) machine.mProcessingEnergy / 1000));
            }))
                .direction(ProgressWidget.Direction.UP)
                .widgetTheme(GTWidgetThemes.PROGRESSBAR_FUEL)
                .size(14).margin(2))
            .child(itemSlot(2, false).widgetTheme(GTWidgetThemes.OVERLAY_ITEM_SLOT_COAL));
        return GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .doesAddGregTechLogo(false)
            .build()
            .child(Flow.row().horizontalCenter().coverChildren().childPadding(9)
                .top(25 + CommonWidgets.MACHINE_HEADER_HEIGHT)
                .child(waterSlots).child(indicators).child(fuelSlots))
            .child(CommonButtons.createSmallPowerSwitchButton("powerSwitch").top(4).right(4));
    }

    private ItemSlot itemSlot(int index, boolean output) {
        return new ItemSlot().slot(new MachineModularSlot(
            machine.getInventoryHandler(),
            index,
            machine.getBaseMetaTileEntity()
        ).slotGroup("item_inv").accessibility(!output, true));
    }

    private GTFluidSlot fluidGauge(boolean steam) {
        GTFluidSlot gauge = new GTFluidSlot();
        gauge.syncHandler(new FluidSlotSyncHandler(new BoilerGauge(steam))
            .canDrainSlot(false).canFillSlot(false).controlsAmount(false));
        gauge.alwaysShowFull(false).size(10, 54);
        return gauge;
    }

    /** A read-only view of the real tanks; no fluid is created, moved or stored by the gauge. */
    private final class BoilerGauge implements IFluidTank {

        private final boolean steam;

        private BoilerGauge(boolean steam) {
            this.steam = steam;
        }

        @Override
        public FluidStack getFluid() {
            FluidStack stored = steam ? machine.mSteam : machine.mFluid;
            if (stored != null && stored.amount > getCapacity()) {
                FluidStack visible = stored.copy();
                visible.amount = getCapacity();
                return visible;
            }
            return stored;
        }

        @Override
        public int getFluidAmount() {
            FluidStack fluid = getFluid();
            return fluid == null ? 0 : fluid.amount;
        }

        @Override
        public int getCapacity() {
            return steam ? machine.getGuiSteamCapacity() - 100 : 15900;
        }

        @Override
        public FluidTankInfo getInfo() {
            return new FluidTankInfo(getFluid(), getCapacity());
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(int maximum, boolean doDrain) {
            return null;
        }
    }
}
