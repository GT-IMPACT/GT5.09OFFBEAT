package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.util.MachineItemHandler;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.GTFluidSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

/** The existing standalone pump and water generator window, with its original container slots. */
public class MTEBasicTankGui {

    private static final UITexture BACKGROUND = UITexture.builder()
        .location("gregtech", "gui/BasicTank")
        .imageSize(256, 256)
        .subAreaXYWH(0, 4, 176, 162)
        .canApplyTheme()
        .build();

    private final GT_MetaTileEntity_BasicTank machine;
    private final MachineItemHandler inventory;

    public MTEBasicTankGui(GT_MetaTileEntity_BasicTank machine) {
        this.machine = machine;
        inventory = new MachineItemHandler(machine.mInventory, machine) {

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // GT_Container_BasicTank uses a plain Slot here, independently of automation rules.
                return slot == 0 || super.isItemValid(slot, stack);
            }
        };
    }

    public ModularPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup("input", 1);
        syncManager.registerSlotGroup("output", 1, false);
        IntSyncValue amount = new IntSyncValue(() -> machine.mFluid == null ? 0 : machine.mFluid.amount);
        syncManager.syncValue("fluidAmount", amount);
        GTFluidSlot display = new GTFluidSlot();
        display.syncHandler(new FluidSlotSyncHandler(new TankDisplay())
            .canDrainSlot(false).canFillSlot(false).controlsAmount(false));
        display.alwaysShowFull(true).disableThemeBackground(true);
        int header = CommonWidgets.MACHINE_HEADER_HEIGHT;
        return GTGuis.mteTemplatePanelBuilder(machine, data, syncManager, settings)
            .setWidth(176)
            .setHeight(166)
            .doesBindPlayerInventory(false)
            .doesAddGregTechLogo(false)
            .doesAddGhostCircuitSlot(false)
            .build()
            .child(BACKGROUND.asWidget().pos(0, header + 4).size(176, 162))
            .child(new ItemSlot().slot(new MachineModularSlot(inventory, 0, machine.getBaseMetaTileEntity())
                .slotGroup("input"))
                .disableThemeBackground(true).pos(79, 16 + header))
            .child(new ItemSlot().slot(new MachineModularSlot(inventory, 1, machine.getBaseMetaTileEntity())
                .canPut(false).slotGroup("output"))
                .disableThemeBackground(true).pos(79, 52 + header))
            .child(display.pos(58, 41 + header).size(18))
            .child(IKey.str("Liquid Amount").asWidget()
                .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE).pos(10, 20 + header))
            .child(IKey.dynamic(() -> GT_Utility.parseNumberToString(amount.getIntValue())).asWidget()
                .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE).pos(10, 30 + header))
            .child(IKey.lang("container.inventory").asWidget().pos(8, 72 + header))
            .child(SlotGroupWidget.playerInventory((index, slot) -> slot.disableThemeBackground(true).size(18))
                .pos(7, 83 + header));
    }

    /** A live view of the real tank that cannot fill containers or change stored fluid. */
    private final class TankDisplay implements IFluidTank {

        @Override
        public FluidStack getFluid() {
            return machine.mFluid;
        }

        @Override
        public int getFluidAmount() {
            return machine.mFluid == null ? 0 : machine.mFluid.amount;
        }

        @Override
        public int getCapacity() {
            return machine.getCapacity();
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
