package gregtech.common.modularui2.widget.builder;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.interfaces.modularui.IKeyProvider;
import gregtech.common.modularui2.sync.LinkedBoolValue;
import gregtech.common.modularui2.widget.SelectButton;

import java.util.Objects;

/** GTNH row or column of radio buttons backed by one enum sync value. */
public class EnumSeriesBuilder<E extends Enum<E>> {

    private final Class<E> enumClass;
    private EnumSyncValue<E, ?> syncValue;
    private IDrawable[] overlays;
    private IKey[] tooltips;

    public EnumSeriesBuilder(Class<E> enumClass) {
        this.enumClass = Objects.requireNonNull(enumClass);
    }

    /** Register the supplied value in the panel sync manager before using the resulting buttons. */
    public EnumSeriesBuilder<E> value(EnumSyncValue<E, ?> syncValue) {
        this.syncValue = Objects.requireNonNull(syncValue);
        return this;
    }

    public EnumSeriesBuilder<E> overlay(IDrawable... overlays) {
        this.overlays = overlays.clone();
        return this;
    }

    public EnumSeriesBuilder<E> overlay(int size, IDrawable... overlays) {
        if (size <= 0) {
            throw new IllegalArgumentException("Overlay size must be positive");
        }
        this.overlays = new IDrawable[overlays.length];
        for (int index = 0; index < overlays.length; index++) {
            this.overlays[index] = Objects.requireNonNull(overlays[index]).asIcon().size(size);
        }
        return this;
    }

    public EnumSeriesBuilder<E> tooltip(IKey... tooltips) {
        this.tooltips = tooltips.clone();
        return this;
    }

    public Flow build(GuiAxis direction) {
        if (syncValue == null) {
            throw new IllegalStateException("Enum sync value is required");
        }
        E[] values = enumClass.getEnumConstants();
        if (overlays != null && overlays.length != values.length
            || tooltips != null && tooltips.length != values.length) {
            throw new IllegalArgumentException("Each enum value must have one overlay and tooltip");
        }
        Flow flow = (direction == GuiAxis.X ? Flow.row() : Flow.column()).childPadding(2).coverChildren();
        for (E value : values) {
            ToggleButton button = new SelectButton().value(LinkedBoolValue.of(syncValue, value)).size(16);
            if (overlays != null) {
                button.overlay(overlays[value.ordinal()]);
            }
            if (tooltips != null) {
                button.addTooltipLine(tooltips[value.ordinal()]);
            } else if (value instanceof IKeyProvider) {
                button.addTooltipLine(IKey.lang(((IKeyProvider) value).getKey()));
            }
            flow.child(button);
        }
        return flow;
    }
}
