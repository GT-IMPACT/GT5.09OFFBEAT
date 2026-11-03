package gregtech.api.modularui2;

import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.modularui2.factory.GTBaseGuiBuilder;
import gregtech.common.modularui2.panel.GTPopUpPanel;
import net.minecraft.entity.player.EntityPlayer;

/** GTNH's shared entry points for machine panels and GUI factories. */
public final class GTGuis {

    private GTGuis() {
    }

    public static void init() {
        GTGuiTextures.init();
        GTWidgetThemes.init();
        GTGuiTheme.registerThemes();
        registerFactories();
    }

    public static void registerFactories() {
        GuiManager.registerFactory(MetaTileEntityGuiHandler.INSTANCE);
        GuiManager.registerFactory(CoverUIFactory.INSTANCE);
    }

    public static boolean open(EntityPlayer player, IMetaTileEntity machine) {
        return MetaTileEntityGuiHandler.open(player, machine);
    }

    public static GTBaseGuiBuilder mteTemplatePanelBuilder(
            IMetaTileEntity machine,
            PosGuiData data,
            PanelSyncManager syncManager,
            UISettings settings
    ) {
        return new GTBaseGuiBuilder(machine, data, syncManager, settings);
    }

    public static ModularPanel createPopUpPanel(String name) {
        return createPopUpPanel(name, false, false);
    }

    public static ModularPanel createPopUpPanel(
            String name,
            boolean disablePanelsBelow,
            boolean closeOnOutOfBoundsClick
    ) {
        return new GTPopUpPanel(name, disablePanelsBelow, closeOnOutOfBoundsClick);
    }

    public static void markDirty(IMetaTileEntity machine) {
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() == null) {
            return;
        }
        tile.markDirty();
        if (tile.isServerSide()) {
            tile.getWorld().getChunkFromBlockCoords(tile.getXCoord(), tile.getZCoord()).setChunkModified();
        }
    }
}
