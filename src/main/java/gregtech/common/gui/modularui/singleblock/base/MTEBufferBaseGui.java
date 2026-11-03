package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Buffer;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import net.minecraft.util.StatCollector;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * GTNH buffer controls, using this fork's existing persisted buffer flags.
 * Source: GTNewHorizons/GT5-Unofficial, f0472cfbd44ab9c8d5a27df7e98c64a42eb1c03f.
 */
public class MTEBufferBaseGui<T extends GT_MetaTileEntity_Buffer> extends MTETieredMachineBlockBaseGui<T> {

    public MTEBufferBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("emitEnergy", new BooleanSyncValue(() -> machine.bOutput, value -> {
            machine.bOutput = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
        syncManager.syncValue("emitRedstone", new BooleanSyncValue(() -> machine.bRedstoneIfFull, value -> {
            machine.bRedstoneIfFull = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
        syncManager.syncValue("invertRedstone", new BooleanSyncValue(() -> machine.bInvert, value -> {
            machine.bInvert = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
        syncManager.syncValue("stocking", new BooleanSyncValue(() -> machine.bStockingMode, value -> {
            machine.bStockingMode = value;
            GTGuis.markDirty(machine);
        }).allowC2S());
    }

    @Override
    protected boolean supportsTopRightCornerFlow() {
        return false;
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        Flow row = super.createBottomLeftCornerFlow(panel, syncManager).collapseDisabledChild();
        for (Pair<Boolean, Supplier<IWidget>> button : createButtonList(panel, syncManager)) {
            row.childIf(button.getLeft(), button.getRight());
        }
        return row;
    }

    protected List<Pair<Boolean, Supplier<IWidget>>> createButtonList(
        ModularPanel panel,
        PanelSyncManager syncManager
    ) {
        List<Pair<Boolean, Supplier<IWidget>>> buttons = new ArrayList<>();
        buttons.add(Pair.of(supportsEmitEnergy(),
            () -> createButton(syncManager, "emitEnergy", GTGuiTextures.OVERLAY_BUTTON_EMIT_ENERGY)));
        // This branch has no persisted sorting-mode flag to expose.
        buttons.add(Pair.of(supportsEmitRedstone(),
            () -> createButton(syncManager, "emitRedstone", GTGuiTextures.OVERLAY_BUTTON_EMIT_REDSTONE)));
        buttons.add(Pair.of(supportsInvertRedstone(),
            () -> createButton(syncManager, "invertRedstone", GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)));
        buttons.add(Pair.of(supportsStocking(),
            () -> createButton(syncManager, "stocking", GTGuiTextures.OVERLAY_BUTTON_STOCKING_MODE)));
        return buttons;
    }

    protected boolean supportsEmitEnergy() {
        return true;
    }

    protected boolean supportsEmitRedstone() {
        return true;
    }

    protected boolean supportsInvertRedstone() {
        return true;
    }

    protected boolean supportsStocking() {
        return true;
    }

    protected ToggleButton createButton(PanelSyncManager syncManager, String key, UITexture overlay) {
        return new ToggleButton().value(syncManager.findSyncHandler(key, BooleanSyncValue.class))
            .overlay(overlay).size(SLOT_SIZE)
            .addTooltipLine(StatCollector.translateToLocal("gt.ui.buffer." + key));
    }
}
