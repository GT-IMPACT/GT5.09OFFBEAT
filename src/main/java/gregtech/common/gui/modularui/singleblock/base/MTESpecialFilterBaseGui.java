package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Buffer;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Supplier;

/** GTNH's representation-slot and 3x3 storage layout for specialized filters. */
public abstract class MTESpecialFilterBaseGui<T extends GT_MetaTileEntity_Buffer> extends MTEFilterBaseGui<T> {

    public MTESpecialFilterBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("allowNbt", createAllowNbtSyncValue());
    }

    protected abstract BooleanSyncValue createAllowNbtSyncValue();

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        Flow mainRow = Flow.row().coverChildren().childPadding(1);
        mainRow.child(new Rectangle().asWidget().size(26, 6));
        mainRow.child(createFilterSlot(panel, syncManager));
        mainRow.child(GTGuiTextures.PICTURE_ARROW_24_BLUE.asWidget().size(43, 24));
        mainRow.child(new ItemSlotGridBuilder(machine.inventoryHandler, syncManager).size(3)
            .modularSlotSupplier(MachineModularSlot.supplier(baseMetaTileEntity))
            .build());
        mainRow.child(GTGuiTextures.PICTURE_ARROW_24_RED.asWidget().size(17, 24));
        return super.createContentSection(panel, syncManager).child(mainRow);
    }

    protected ItemSlot createFilterSlot(ModularPanel panel, PanelSyncManager syncManager) {
        ItemSlot itemSlot = createFilterSlotBase(panel, syncManager).slot(createMachineSlot(9));
        itemSlot.tooltipDynamic(tooltip -> tooltip.addStringLines(getEmptyFilterSlotTooltip(panel, syncManager)))
            .background(GTGuiTextures.BUTTON_STANDARD);
        itemSlot.itemTooltip().tooltipBuilder(tooltip -> tooltip.clearText()
            .addStringLines(getFilledFilterSlotTooltip(panel, syncManager)));
        return itemSlot;
    }

    protected abstract ItemSlot createFilterSlotBase(ModularPanel panel, PanelSyncManager syncManager);

    protected abstract List<String> getEmptyFilterSlotTooltip(ModularPanel panel, PanelSyncManager syncManager);

    protected abstract List<String> getFilledFilterSlotTooltip(ModularPanel panel, PanelSyncManager syncManager);

    @Override
    protected List<Pair<Boolean, Supplier<IWidget>>> createButtonList(
        ModularPanel panel,
        PanelSyncManager syncManager
    ) {
        List<Pair<Boolean, Supplier<IWidget>>> buttons = super.createButtonList(panel, syncManager);
        buttons.add(Pair.of(true,
            () -> createButton(syncManager, "allowNbt", GTGuiTextures.OVERLAY_BUTTON_NBT)));
        return buttons;
    }
}
