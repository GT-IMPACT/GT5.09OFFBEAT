package gregtech.api.modularui2.common;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.modularui2.GTGuiTextures;

import java.util.function.Consumer;

/** Shared GTNH controls. Registered sync values are bound by key instead of registered twice. */
public final class CommonButtons {

    private static final int TOOLTIP_DELAY = 0;

    private CommonButtons() {
    }

    public static ToggleButton createMuffleButton(BooleanSyncValue syncValue) {
        return configureMuffleButton(new ToggleButton().value(syncValue));
    }

    public static ToggleButton createMuffleButton(String syncKey) {
        return configureMuffleButton(new ToggleButton().syncHandler(syncKey));
    }

    private static ToggleButton configureMuffleButton(ToggleButton button) {
        return button
            .tooltip(tooltip -> tooltip.add(IKey.lang("GT5U.machines.muffled")))
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_MUFFLE_ON)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_MUFFLE_OFF)
            .size(12)
            .excludeAreaInRecipeViewer(true);
    }

    public static ToggleButton createPowerSwitchButton(
        BooleanSyncValue syncValue,
        boolean disabled,
        IGregTechTileEntity base
    ) {
        return configurePowerSwitch(new ToggleButton().value(syncValue), disabled, base);
    }

    public static ToggleButton createPowerSwitchButton(
        String syncKey,
        boolean disabled,
        IGregTechTileEntity base
    ) {
        return configurePowerSwitch(new ToggleButton().syncHandler(syncKey), disabled, base);
    }

    private static ToggleButton configurePowerSwitch(
        ToggleButton button,
        boolean disabled,
        IGregTechTileEntity base
    ) {
        return button
            .tooltip(tooltip -> tooltip.add(IKey.lang("GT5U.gui.button.power_switch")))
            .overlay(new DynamicDrawable(() -> {
                if (disabled) {
                    return GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_DISABLED;
                }
                return base.isAllowedToWork()
                    ? GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON
                    : GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_OFF;
            }));
    }

    public static ToggleButton createSmallPowerSwitchButton(String syncKey) {
        return new ToggleButton()
            .syncHandler(syncKey)
            .tooltip(tooltip -> tooltip.add(IKey.lang("GT5U.gui.button.power_switch")))
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_SMALL_POWER_SWITCH_ON)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_SMALL_POWER_SWITCH_OFF)
            .size(12)
            .excludeAreaInRecipeViewer(true);
    }

    public static ButtonWidget<?> panelCloseButton() {
        return ButtonWidget.panelCloseButton();
    }

    public static ToggleButton createToggleButton(BooleanSyncValue syncValue, UITexture overlay, String key) {
        return new ToggleButton()
            .value(syncValue)
            .overlay(overlay)
            .addTooltipLine(IKey.lang(key))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    public static ToggleButton createToggleButtonDynamicTooltip(
        BooleanSyncValue syncValue,
        UITexture overlay,
        Consumer<RichTooltip> tooltipBuilder
    ) {
        return new ToggleButton()
            .value(syncValue)
            .overlay(overlay)
            .tooltipDynamic(tooltipBuilder)
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    public static ToggleButton createToggleButtonDynamicTooltip(
        BooleanSyncValue syncValue,
        UITexture selectedOverlay,
        UITexture unselectedOverlay,
        Consumer<RichTooltip> tooltipBuilder
    ) {
        return new ToggleButton()
            .value(syncValue)
            .overlay(true, selectedOverlay)
            .overlay(false, unselectedOverlay)
            .tooltipDynamic(tooltipBuilder)
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }
}
