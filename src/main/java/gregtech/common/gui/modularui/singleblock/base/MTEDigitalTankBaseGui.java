package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_StorageTank;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GT_Utility;
import gregtech.common.modularui2.widget.GTFluidSlot;
import net.minecraftforge.fluids.IFluidTank;

/** GTNH's digital tank layout, bound to this fork's four existing tank modes. */
public class MTEDigitalTankBaseGui<T extends GT_MetaTileEntity_StorageTank> extends MTEBasicTankBaseGui<T> {

    public MTEDigitalTankBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected boolean supportsTopRightCornerFlow() {
        return false;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("fluidAutoOutput", new BooleanSyncValue(() -> machine.OutputFluid, value -> {
            machine.OutputFluid = value;
            if (!syncManager.isClient()) {
                GT_Utility.sendChatToPlayer(syncManager.getPlayer(), value
                    ? "Fluid Output Enabled" : "Fluid Output Disabled");
                GTGuis.markDirty(machine);
            }
        }).allowC2S());
        syncManager.syncValue("fluidLock", new BooleanSyncValue(() -> machine.mMode, value -> {
            machine.mMode = value;
            if (!syncManager.isClient()) {
                if (value) {
                    machine.setLockedFluidName(machine.mFluid == null ? null : machine.mFluid.getUnlocalizedName());
                    String fluidName = machine.mFluid == null
                        ? "currently none, will be locked to the next that is put in"
                        : machine.mFluid.getLocalizedName();
                    GT_Utility.sendChatToPlayer(syncManager.getPlayer(), "1 specific Fluid (" + fluidName + ")");
                } else {
                    GT_Utility.sendChatToPlayer(syncManager.getPlayer(), "Lock Fluid Mode Disabled");
                }
                GTGuis.markDirty(machine);
            }
        }).allowC2S());
        syncManager.syncValue("voidOverflow", new BooleanSyncValue(() -> machine.mVoidFluidPart, value -> {
            machine.mVoidFluidPart = value;
            if (!syncManager.isClient()) {
                GT_Utility.sendChatToPlayer(syncManager.getPlayer(), value
                    ? "Void Part Mode Enabled" : "Void Part Mode Disabled");
                GTGuis.markDirty(machine);
            }
        }).allowC2S());
        syncManager.syncValue("voidAll", new BooleanSyncValue(() -> machine.mVoidFluidFull, value -> {
            machine.mVoidFluidFull = value;
            if (!syncManager.isClient()) {
                GT_Utility.sendChatToPlayer(syncManager.getPlayer(), value
                    ? "Void Full Mode Enabled" : "Void Full Mode Disabled");
                GTGuis.markDirty(machine);
            }
        }).allowC2S());
        syncManager.syncValue("lockedFluid", new StringSyncValue(
            () -> machine.getLockedFluidName() == null ? "" : machine.getLockedFluidName(),
            value -> machine.setLockedFluidName(value.isEmpty() ? null : value)));
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomLeftCornerFlow(panel, syncManager)
            .child(new ToggleButton()
                .value(syncManager.findSyncHandler("fluidAutoOutput", BooleanSyncValue.class))
                .overlay(GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_FLUID)
                .addTooltipLine("Fluid Auto-Output"))
            .child(new ToggleButton()
                .value(syncManager.findSyncHandler("fluidLock", BooleanSyncValue.class))
                .overlay(GTGuiTextures.OVERLAY_BUTTON_LOCK)
                .addTooltipLine("Lock Fluid Mode")
                .addTooltipLine("First you need to a fill this tank fluid, then press the button")
                .addTooltipLine("No liquids will be poured in here except this one"));
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        Flow buttons = Flow.row().coverChildren().marginRight(SLOT_SIZE)
            .child(new ToggleButton()
                .value(syncManager.findSyncHandler("voidOverflow", BooleanSyncValue.class))
                .overlay(GTGuiTextures.OVERLAY_BUTTON_TANK_VOID_EXCESS)
                .addTooltipLine("Void Part Mode")
                .addTooltipLine("Fluid is part (25%) removing if tank full"))
            .child(new ToggleButton()
                .value(syncManager.findSyncHandler("voidAll", BooleanSyncValue.class))
                .overlay(GTGuiTextures.OVERLAY_BUTTON_TANK_VOID_ALL)
                .addTooltipLine("Void Full Mode")
                .addTooltipLine("Fluid is completely removing"));
        return super.createBottomRightCornerFlow(panel, syncManager).child(buttons);
    }

    @Override
    protected FluidSlot createFluidSlot(ModularPanel panel, PanelSyncManager syncManager, IFluidTank tank) {
        FluidSlotSyncHandler fluidSync = new FluidSlotSyncHandler(tank).filter(machine::isFluidInputAllowed);
        fluidSync.setChangeListener(() -> {
            if (!syncManager.isClient()) {
                machine.onEmptyingContainerWhenEmpty();
                GTGuis.markDirty(machine);
            }
        });
        return new GTFluidSlot().syncHandler(fluidSync)
            .bottomRel(0).rightRel(0).background(GTGuiTextures.SLOT_FLUID_TANK);
    }
}
