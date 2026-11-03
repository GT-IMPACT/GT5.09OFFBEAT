package gregtech.common.gui.modularui.singleblock.base;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Buffer;
import gregtech.api.modularui2.GTGuiTextures;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Supplier;

/** GTNH filter controls without changing the local machines' backend inheritance. */
public abstract class MTEFilterBaseGui<T extends GT_MetaTileEntity_Buffer> extends MTEBufferBaseGui<T> {

    public MTEFilterBaseGui(T machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("invertFilter", createInvertFilterSyncValue());
    }

    protected abstract BooleanSyncValue createInvertFilterSyncValue();

    @Override
    protected List<Pair<Boolean, Supplier<IWidget>>> createButtonList(
        ModularPanel panel,
        PanelSyncManager syncManager
    ) {
        List<Pair<Boolean, Supplier<IWidget>>> buttons = super.createButtonList(panel, syncManager);
        buttons.add(Pair.of(true,
            () -> createButton(syncManager, "invertFilter", GTGuiTextures.OVERLAY_BUTTON_INVERT_FILTER)));
        return buttons;
    }
}
