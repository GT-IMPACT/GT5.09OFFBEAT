package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import gregtech.api.enums.Dyes;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** GTNH 4 by 4 dye selector with an explicit, bounded number of simultaneous selections. */
public class ColorGridWidget extends Flow {

    private static final EnumChatFormatting[] FORMATTING = {
        EnumChatFormatting.BLACK, EnumChatFormatting.DARK_RED, EnumChatFormatting.DARK_GREEN,
        EnumChatFormatting.GOLD, EnumChatFormatting.DARK_BLUE, EnumChatFormatting.DARK_PURPLE,
        EnumChatFormatting.DARK_AQUA, EnumChatFormatting.GRAY, EnumChatFormatting.DARK_GRAY,
        EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.GREEN, EnumChatFormatting.YELLOW,
        EnumChatFormatting.BLUE, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.GOLD,
        EnumChatFormatting.WHITE
    };

    private final List<Byte> selected = new ArrayList<>();
    private final boolean input;
    private Consumer<List<Byte>> onToggle;
    private int maxSelected = 16;
    private int buttonSize = 9;
    private int buttonPadding = 1;
    private int borderSize = 1;
    private boolean built;

    public ColorGridWidget(boolean input) {
        super(GuiAxis.Y);
        this.input = input;
        coverChildren();
    }

    public ColorGridWidget setInitialSelected(byte value) {
        selected.clear();
        if (value >= 0 && value < 16 && maxSelected > 0) {
            selected.add(value);
        }
        return this;
    }

    public ColorGridWidget setInitialSelected(List<Byte> values) {
        List<Byte> validated = new ArrayList<>();
        if (values != null) {
            for (Byte value : values) {
                if (value == null || value < 0 || value >= 16) {
                    throw new IllegalArgumentException("Dye index must be between 0 and 15");
                }
                if (!validated.contains(value)) {
                    validated.add(value);
                }
            }
        }
        selected.clear();
        selected.addAll(validated);
        trimSelection();
        return this;
    }

    public ColorGridWidget setMaxSelected(int value) {
        if (value < 0 || value > 16) {
            throw new IllegalArgumentException("Selection limit must be between 0 and 16");
        }
        maxSelected = value;
        trimSelection();
        return this;
    }

    public ColorGridWidget setButtonSize(int value) {
        if (value <= borderSize * 2) {
            throw new IllegalArgumentException("Button must fit its border and color swatch");
        }
        buttonSize = value;
        return this;
    }

    public ColorGridWidget setButtonPadding(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Button padding cannot be negative");
        }
        buttonPadding = value;
        return this;
    }

    public ColorGridWidget setBorderSize(int value) {
        if (value < 0 || value * 2 >= buttonSize) {
            throw new IllegalArgumentException("Border must fit within the button");
        }
        borderSize = value;
        return this;
    }

    public ColorGridWidget onButtonToggled(Consumer<List<Byte>> onToggle) {
        this.onToggle = onToggle;
        return this;
    }

    public ColorGridWidget build() {
        createButtonRows();
        return this;
    }

    public void createButtonRows() {
        if (built) {
            throw new IllegalStateException("Color grid was already built");
        }
        for (int row = 0; row < 4; row++) {
            Flow buttons = Flow.row().childPadding(buttonPadding).coverChildren();
            for (int column = 0; column < 4; column++) {
                int index = row * 4 + column;
                short[] rgba = Dyes.VALUES[index].mRGBa;
                int color = 0xff000000 | rgba[0] << 16 | rgba[1] << 8 | rgba[2];
                buttons.child(createToggleButton(color, index));
            }
            child(buttons);
        }
        childPadding(buttonPadding);
        built = true;
    }

    public ToggleButton createToggleButton(int color, int index) {
        if (index < 0 || index >= 16) {
            throw new IllegalArgumentException("Invalid dye index: " + index);
        }
        return new ToggleButton()
            .value(new BoolValue.Dynamic(() -> selected.contains((byte) index), enabled -> {
                selected.remove(Byte.valueOf((byte) index));
                if (enabled) {
                    selected.add((byte) index);
                }
                onToggled();
                if (onToggle != null) {
                    onToggle.accept(getSelected());
                }
            }))
            .background(false, drawButton(false, Color.multiply(color, 0.5f, false)))
            .background(true, drawButton(true, color))
            .hoverOverlay(false, drawButton(false, Color.multiply(color, 0.7f, false)))
            .hoverOverlay(true, drawButton(true, color))
            .tooltipDynamic(tooltip -> {
                tooltip.addLine(StatCollector.translateToLocal("gt.mui2.color." + index));
                if (selected.contains((byte) index)) {
                    tooltip.addLine(StatCollector.translateToLocal("GT5U.tooltip.nac.color_grid.selected"));
                } else {
                    tooltip.addLine(StatCollector.translateToLocal("GT5U.tooltip.nac.color_grid.click_select"));
                }
                if (input) {
                    tooltip.addLine(StatCollector.translateToLocal("GT5U.tooltip.nac.color_grid.wildcard"));
                }
            })
            .size(buttonSize, buttonSize);
    }

    public IDrawable drawButtonBorder(boolean active) {
        return new Rectangle().color(active ? Color.RED.main : Color.BLACK.main);
    }

    public IDrawable drawButton(boolean active, int color) {
        int insideSize = buttonSize - borderSize * 2;
        return IDrawable.of(drawButtonBorder(active), (context, x, y, width, height, theme) -> {
            new Rectangle().color(color).draw(
                context,
                new Area(x + borderSize, y + borderSize, insideSize, insideSize),
                theme
            );
        });
    }

    protected void onToggled() {
        trimSelection();
    }

    private void trimSelection() {
        while (selected.size() > maxSelected) {
            selected.remove(0);
        }
    }

    public String getName(int index) {
        if (index < 0 || index >= selected.size()) {
            return StatCollector.translateToLocal("gt.mui2.color.none");
        }
        return StatCollector.translateToLocal("gt.mui2.color." + selected.get(index));
    }

    public EnumChatFormatting getColorFormatting(int index) {
        return FORMATTING[selected.get(index)];
    }

    public Dyes getDye(int index) {
        return Dyes.VALUES[selected.get(index)];
    }

    public List<Byte> getSelected() {
        return Collections.unmodifiableList(new ArrayList<>(selected));
    }

    public Byte getSelected(int index) {
        return selected.get(index);
    }

    public int getAmountSelected() {
        return selected.size();
    }
}
