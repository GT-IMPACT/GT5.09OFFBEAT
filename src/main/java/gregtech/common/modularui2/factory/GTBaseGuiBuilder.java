package gregtech.common.modularui2.factory;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.modularui2.CoverGuiData;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.covers.Cover;
import gregtech.common.modularui2.widget.CoverTabButton;
import net.minecraftforge.common.util.ForgeDirection;

/** GTNH machine panel template: shared inventory binding, title, cover tabs and logo. */
public final class GTBaseGuiBuilder {

    private final IMetaTileEntity mte;
    private final PosGuiData posGuiData;
    private final PanelSyncManager syncManager;
    private final UISettings uiSettings;

    private int width = 176;
    private int height = 166;
    private boolean bindPlayerInventory = true;
    private boolean addTitle = true;
    private boolean addCoverTabs = true;
    private boolean addGhostCircuitSlot;
    private boolean addGregTechLogo = true;
    private int logoX = 152;
    private int logoY = 63;

    public GTBaseGuiBuilder(IMetaTileEntity mte, PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        this.mte = mte;
        this.posGuiData = data;
        this.syncManager = syncManager;
        this.uiSettings = uiSettings;
        addGhostCircuitSlot = mte instanceof IConfigurationCircuitSupport
            && ((IConfigurationCircuitSupport) mte).allowSelectCircuit();
        addGregTechLogo = !addGhostCircuitSlot;
    }

    public GTBaseGuiBuilder setWidth(int width) {
        if (width < 18) {
            throw new IllegalArgumentException("Panel width must fit an item slot");
        }
        this.width = width;
        return this;
    }

    public GTBaseGuiBuilder setHeight(int height) {
        if (height < 18) {
            throw new IllegalArgumentException("Panel height must fit an item slot");
        }
        this.height = height;
        return this;
    }

    public GTBaseGuiBuilder doesBindPlayerInventory(boolean bindPlayerInventory) {
        this.bindPlayerInventory = bindPlayerInventory;
        return this;
    }

    public GTBaseGuiBuilder doesAddTitle(boolean addTitle) {
        this.addTitle = addTitle;
        return this;
    }

    public GTBaseGuiBuilder doesAddCoverTabs(boolean addCoverTabs) {
        this.addCoverTabs = addCoverTabs;
        return this;
    }

    public GTBaseGuiBuilder doesAddGregTechLogo(boolean addGregTechLogo) {
        this.addGregTechLogo = addGregTechLogo;
        return this;
    }

    public GTBaseGuiBuilder doesAddGhostCircuitSlot(boolean addGhostCircuitSlot) {
        this.addGhostCircuitSlot = addGhostCircuitSlot;
        return this;
    }

    public GTBaseGuiBuilder moveGregtechLogoPos(int x, int y) {
        this.logoX = x;
        this.logoY = y;
        return this;
    }

    public ModularPanel build() {
        int headerHeight = addTitle ? CommonWidgets.MACHINE_HEADER_HEIGHT : 0;
        ModularPanel panel = ModularPanel.defaultPanel(mte.getGuiId(), width, height + headerHeight);
        if (bindPlayerInventory) {
            panel.bindPlayerInventory();
        }
        if (addTitle && NetworkUtils.isClient()) {
            panel.child(CommonWidgets.createMachineTitle(mte, width));
        }
        if (addCoverTabs) {
            panel.child(createCoverTabs());
        }
        if (addGhostCircuitSlot) {
            if (!(mte instanceof IConfigurationCircuitSupport)) {
                throw new IllegalStateException("Virtual circuit slot requires IConfigurationCircuitSupport");
            }
            IConfigurationCircuitSupport support = (IConfigurationCircuitSupport) mte;
            panel.child(CommonWidgets.createCircuitSlot(syncManager, mte)
                .pos(support.getCircuitSlotX() - 1, support.getCircuitSlotY() - 1 + headerHeight));
        }
        if (addGregTechLogo) {
            panel.child(new Widget<>()
                .widgetTheme(GTWidgetThemes.PICTURE_LOGO)
                .size(18)
                .pos(logoX, logoY + headerHeight));
        }
        syncManager.addCloseListener(player -> GTGuis.markDirty(mte));
        return panel;
    }

    private Flow createCoverTabs() {
        Flow column = Flow.column()
            .coverChildren()
            .leftRel(0f, 3, 1f)
            .top(1 + (addTitle ? CommonWidgets.MACHINE_HEADER_HEIGHT : 0))
            .childPadding(2);
        for (byte side = 0; side < 6; side++) {
            ForgeDirection direction = ForgeDirection.getOrientation(side);
            CoverTabButton button = new CoverTabButton(mte.getBaseMetaTileEntity(), direction, syncManager);
            String panelName = "cover_panel_" + side;
            button.setPanel(syncManager.syncedPanel(panelName, true, (panelSync, panelHandler) -> {
                Cover cover = mte.getBaseMetaTileEntity().getCoverAtSide(direction);
                if (!cover.isValid() || !cover.hasCoverGUI() || !cover.useMui2()) {
                    return GTGuis.createPopUpPanel(panelName)
                        .size(176, 58)
                        .child(IKey.lang("gt.mui2.cover_changed").asWidget().pos(8, 20).width(160));
                }
                CoverGuiData data = new CoverGuiData(
                    posGuiData.getPlayer(),
                    cover.getCoverID(),
                    posGuiData.getX(),
                    posGuiData.getY(),
                    posGuiData.getZ(),
                    direction,
                    true
                );
                return cover.buildPopUpUI(
                    data,
                    panelName,
                    panelSync,
                    uiSettings,
                    button
                ).name(panelName);
            }));
            column.child(button);
        }
        uiSettings.getRecipeViewerSettings().addExclusionArea(column);
        return column;
    }
}
