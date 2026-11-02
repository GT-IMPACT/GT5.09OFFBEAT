package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.covers.Cover;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

/** GTNH cover tab opening native synchronized popups through the shared cover object API. */
public class CoverTabButton extends ButtonWidget<CoverTabButton> {

    private final ICoverable coverable;
    private final ForgeDirection side;
    private IPanelHandler panel;

    public CoverTabButton(ICoverable coverable, ForgeDirection side, PanelSyncManager syncManager) {
        if (side == null || side == ForgeDirection.UNKNOWN) {
            throw new IllegalArgumentException("A cover tab needs a valid side");
        }
        this.coverable = coverable;
        this.side = side;
        String actionKey = "open_cover_" + side.ordinal();
        syncManager.registerSyncedAction(actionKey, false, true, packet -> {
            int expectedCover = packet.readInt();
            Cover cover = coverable.getCoverAtSide(side);
            if (coverable.isUseableByPlayer(syncManager.getPlayer()) && cover.isValid()
                && cover.getCoverID() == expectedCover && cover.hasCoverGUI() && !cover.useMui2()) {
                cover.openGUI(syncManager.getPlayer());
            }
        });
        setEnabledIf(widget -> coverable.getCoverAtSide(side).isValid())
            .onMousePressed(mouseButton -> {
                Cover cover = coverable.getCoverAtSide(side);
                if ((mouseButton != 0 && mouseButton != 1) || !cover.hasCoverGUI()) {
                    return false;
                }
                if (cover.useMui2()) {
                    togglePanel();
                } else {
                    syncManager.callSyncedAction(actionKey, packet -> packet.writeInt(cover.getCoverID()));
                }
                return true;
            })
            .overlay(new DynamicDrawable(() -> new ItemDrawable(coverable.getCoverAtSide(side).asItemStack())
                .asIcon()
                .marginLeft(2)
                .marginTop(1)))
            .tooltipBuilder(this::buildTooltip)
            .tooltipAutoUpdate(true)
            .size(20, 20);
    }

    public void setPanel(IPanelHandler panel) {
        this.panel = panel;
    }

    private void togglePanel() {
        if (panel == null) {
            return;
        }
        if (panel.isPanelOpen()) {
            panel.closePanel();
        } else {
            panel.openPanel();
        }
    }

    private void buildTooltip(RichTooltip tooltip) {
        Cover cover = coverable.getCoverAtSide(side);
        ItemStack stack = cover.asItemStack();
        if (stack == null) {
            return;
        }
        String sideFormatting = cover.hasCoverGUI()
            ? EnumChatFormatting.GRAY.toString() + EnumChatFormatting.UNDERLINE
            : EnumChatFormatting.DARK_GRAY.toString();
        tooltip.add(sideFormatting
            + StatCollector.translateToLocal("gt.mui2.side." + side.ordinal())
            + EnumChatFormatting.RESET + ": " + stack.getDisplayName());
        tooltip.newLine().addStringLines(cover.getAdditionalTooltip());
    }

    @Override
    public WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getWidgetTheme(coverable.getCoverAtSide(side).hasCoverGUI()
            ? GTWidgetThemes.BUTTON_COVER_TAB_ENABLED
            : GTWidgetThemes.BUTTON_COVER_TAB_DISABLED);
    }
}
