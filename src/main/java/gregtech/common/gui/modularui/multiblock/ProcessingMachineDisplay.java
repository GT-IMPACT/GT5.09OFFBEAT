package gregtech.common.gui.modularui.multiblock;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Dyes;
import gregtech.api.gui.GT_ProcessingOutputSummary;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROL_COUNT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_FRAME_SIZE;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_Y;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_WIDTH;
import static gregtech.api.gui.GT_Container_ProcessingMachine.NEI_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_X;
import static gregtech.api.gui.GT_Container_ProcessingMachine.PLAYER_INVENTORY_Y;
import static gregtech.api.gui.GT_Container_ProcessingMachine.POWER_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.RECIPE_MAP_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.SCREEN_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.SCREEN_WIDTH;

/** The existing processing window's textures, text, colors and scroll behavior on an MUI2 widget. */
public class ProcessingMachineDisplay extends Widget<ProcessingMachineDisplay> implements Interactable {

    private static final UITexture BACKGROUND = UITexture.builder()
            .location("gregtech", "gui/background/singleblock_default")
            .imageSize(176, 166)
            .adaptable(4)
            .build();
    private static final UITexture SCREEN = UITexture.builder()
            .location("gregtech", "gui/picture/screen_black")
            .imageSize(16, 16)
            .adaptable(2)
            .build();
    private static final UITexture ITEM_SLOT = texture("slot/item_standard");
    private static final UITexture CONTROLLER_SLOT = UITexture.builder()
            .location("gregtech", "gui/slot/item_standard")
            .imageSize(18, 18)
            .adaptable(1)
            .build();
    private static final UITexture BUTTON = UITexture.builder()
            .location("gregtech", "gui/button/standard")
            .imageSize(18, 18)
            .adaptable(1)
            .build();
    private static final UITexture BUTTON_PRESSED = UITexture.builder()
            .location("gregtech", "gui/button/standard_pressed")
            .imageSize(18, 18)
            .adaptable(1)
            .build();
    private static final UITexture[] VOIDING = { overlay("void_excess_none"), overlay("void_excess_item"),
            overlay("void_excess_fluid"), overlay("void_excess_all") };
    private static final UITexture[] SEPARATION = { overlay("input_separation_off"), overlay("input_separation_on") };
    private static final UITexture[] BATCH = { overlay("batch_mode_off"), overlay("batch_mode_on") };
    private static final UITexture[] LOCK = { overlay("recipe_unlocked"), overlay("recipe_locked") };
    private static final UITexture RECIPE_MAP = overlay("machine_mode_default");
    private static final UITexture NEI = overlay("nei");
    private static final UITexture[] POWER = { overlay("power_switch_off"), overlay("power_switch_on") };
    private static final String[] VOIDING_NAMES = { "none", "items", "fluids", "all" };
    private static final String[] MODE_NAMES = { "separate", "batch", "lock" };
    private static final int[] MODE_FLAGS = { ProcessingLogic.SEPARATE_INPUTS, ProcessingLogic.BATCH,
            ProcessingLogic.LOCK_RECIPE };
    private static final int TEXT_COLOR = 0xfafaff;
    private static final int NAME_COLOR = 0x8999aa;
    private static final int SCREEN_LEFT = 4;
    private static final int SCREEN_TOP = 2;
    private static final int SCREEN_TEXT_X = SCREEN_LEFT + 6;
    private static final int SCREEN_TEXT_Y = SCREEN_TOP + 6;
    private static final int LINE_HEIGHT = 10;
    private static final int VISIBLE_LINES = (SCREEN_HEIGHT + SCREEN_TOP - 2 - SCREEN_TEXT_Y) / LINE_HEIGHT;
    private static final int TEXT_WIDTH = SCREEN_WIDTH - 13;
    private final GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine;
    private final ProcessingGuiState state;
    private int scroll;

    public ProcessingMachineDisplay(GT_MetaTileEntity_ProcessingMultiBlockBase<?> machine, ProcessingGuiState state) {
        this.machine = machine;
        this.state = state;
        name("processing_display");
        size(GUI_WIDTH, GUI_HEIGHT);
        invisible();
    }

    @Override
    public boolean canHoverThrough() {
        return true;
    }

    @Override
    public boolean canClickThrough() {
        return true;
    }

    @Override
    public boolean onMouseScroll(UpOrDown direction, int amount) {
        if (!insideScreen(getContext().getMouseX(), getContext().getMouseY())) {
            return false;
        }
        scroll = Math.max(0, Math.min(Math.max(0,
            screenLines(Minecraft.getMinecraft().fontRenderer).size() - VISIBLE_LINES), scroll - direction.modifier));
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        applyGuiTint();
        BACKGROUND.draw(0, 0, GUI_WIDTH, GUI_HEIGHT);
        SCREEN.draw(SCREEN_LEFT, SCREEN_TOP, SCREEN_WIDTH, SCREEN_HEIGHT);
        drawInventoryFrames();
        drawControls();
        GL11.glColor4f(1, 1, 1, 1);
        drawText(Minecraft.getMinecraft().fontRenderer);
    }

    private void drawInventoryFrames() {
        if (machine.hasControllerSlotInGui()) {
            int padding = (CONTROLLER_FRAME_SIZE - 16) / 2;
            CONTROLLER_SLOT.draw(CONTROLLER_X - padding, CONTROLLER_Y - padding,
                CONTROLLER_FRAME_SIZE, CONTROLLER_FRAME_SIZE);
        }
        for (int row = 0; row < 4; row++) {
            int y = PLAYER_INVENTORY_Y + (row == 3 ? 58 : row * 18);
            for (int column = 0; column < 9; column++) {
                ITEM_SLOT.draw(PLAYER_INVENTORY_X + column * 18 - 1, y - 1, 18, 18);
            }
        }
    }

    private void drawControls() {
        int controls = CONTROL_COUNT + (hasRecipeMapSelector() ? 1 : 0);
        for (int index = 0; index < controls; index++) {
            if (!ProcessingGuiControls.isControlVisible(machine, index)) {
                continue;
            }
            int x = ProcessingGuiControls.controlX(machine, index);
            int y = ProcessingGuiControls.controlY(index);
            if (!state.isDisplayReady()) {
                BUTTON.draw(x - 1, y - 1, 18, 18);
                continue;
            }
            UITexture icon;
            boolean pressed;
            if (index == 0) {
                int mode = ProcessingGuiControls.voidingIndex(state.modes);
                pressed = mode != 0;
                icon = VOIDING[mode];
            } else if (index < POWER_SLOT) {
                pressed = enabled(MODE_FLAGS[index - 1]);
                UITexture[] textures = index == 1 ? SEPARATION : index == 2 ? BATCH : LOCK;
                icon = textures[pressed ? 1 : 0];
            } else if (index == POWER_SLOT) {
                pressed = state.allowedToWork;
                icon = POWER[pressed ? 1 : 0];
            } else if (index == NEI_SLOT) {
                pressed = false;
                icon = NEI;
            } else {
                pressed = false;
                icon = RECIPE_MAP;
            }
            (pressed ? BUTTON_PRESSED : BUTTON).draw(x - 1, y - 1, 18, 18);
            GL11.glColor4f(1, 1, 1, 1);
            icon.draw(x, y, 16, 16);
            applyGuiTint();
        }
    }

    List<String> controlTooltip(int index) {
        List<String> tooltip = new ArrayList<>();
        if (!state.isDisplayReady()) {
            return tooltip;
        }
        if (index == 0) {
            tooltip.add(text("voiding"));
            tooltip.add(text("voiding." + VOIDING_NAMES[ProcessingGuiControls.voidingIndex(state.modes)]));
            tooltip.add(text("recipe_map_switch"));
        } else if (index < POWER_SLOT) {
            tooltip.add(text("mode." + MODE_NAMES[index - 1]) + ": "
                + text(enabled(MODE_FLAGS[index - 1]) ? "on" : "off"));
            tooltip.add(text("mode." + MODE_NAMES[index - 1] + ".description"));
        } else if (index == POWER_SLOT) {
            tooltip.add(text("power") + ": " + text(state.allowedToWork ? "on" : "off"));
        } else if (index == NEI_SLOT) {
            tooltip.add(text("nei"));
            tooltip.add(machine.getRecipeMapForNEI() == null ? ""
                : StatCollector.translateToLocal(machine.getRecipeMapForNEI().mUnlocalizedName));
        } else if (index == RECIPE_MAP_SLOT) {
            tooltip.add(text("recipe_map") + ": " + activeRecipeMapName());
            tooltip.add(text("recipe_map_switch"));
        }
        if (index != POWER_SLOT && index != NEI_SLOT) {
            tooltip.add(text("next_cycle"));
        }
        return tooltip;
    }

    private boolean hasRecipeMapSelector() {
        return machine.getRecipeMaps().size() > 1;
    }

    private String activeRecipeMapName() {
        int index = state.activeRecipeMapIndex;
        String key = index >= 0 && index < machine.getRecipeMaps().size()
            ? machine.getRecipeMaps().get(index).mUnlocalizedName
            : CheckRecipeResult.RECIPE_MAP_UNAVAILABLE.translationKey();
        return StatCollector.translateToLocal(key);
    }

    private boolean insideScreen(int x, int y) {
        return x >= SCREEN_TEXT_X && x < SCREEN_WIDTH
            && y >= SCREEN_TEXT_Y && y < SCREEN_TEXT_Y + VISIBLE_LINES * LINE_HEIGHT;
    }

    private List<ScreenLine> screenLines(FontRenderer fontRendererObj) {
        ProcessingGuiState container = state;
        List<ScreenLine> lines = new ArrayList<>();
        lines.add(new ScreenLine(machine.getLocalName(), NAME_COLOR));
        if (!container.isDisplayReady()) {
            return lines;
        }
        if (hasRecipeMapSelector()) {
            lines.add(new ScreenLine(StatCollector.translateToLocalFormatted("gt.interact.desc.mb.mode",
                    activeRecipeMapName())));
        }
        if ((container.mDisplayErrorCode & 63) != 0) {
            lines.add(new ScreenLine(EnumChatFormatting.RED + text("maintenance.detected")));
            String[] tools = { "wrench", "screwdriver", "soft_hammer", "hard_hammer", "soldering_tool", "crowbar" };
            List<String> requiredTools = new ArrayList<>();
            for (int index = 0; index < tools.length; index++) {
                if ((container.mDisplayErrorCode & (1 << index)) != 0) {
                    requiredTools.add(text("maintenance.tool." + tools[index]));
                }
            }
            lines.add(new ScreenLine(String.join(", ", requiredTools)));
        }
        if ((container.mDisplayErrorCode & 64) != 0) {
            lines.add(new ScreenLine(EnumChatFormatting.RED + trans("138", "Incomplete Structure.")));
        }
        if (container.mDisplayErrorCode == 0) {
            if (container.mActive != 0) {
                lines.add(new ScreenLine(StatCollector.translateToLocal("gt.interact.desc.mb.running")));
            } else if (!container.allowedToWork) {
                lines.add(new ScreenLine(StatCollector.translateToLocal("gt.interact.desc.mb.idle")));
            }
        }
        if (container.result != CheckRecipeResult.NONE) {
            lines.add(new ScreenLine(resultColor(container.result)
                    + StatCollector.translateToLocal(container.result.translationKey())));
        }
        if (container.mMaxProgressTime > 0) {
            lines.add(new ScreenLine(currentProgress(container)));
        }
        if (container.energyCapacity > 0) {
            lines.add(new ScreenLine(StatCollector.translateToLocalFormatted("gt.processing.stored_energy",
                    GT_Utility.formatNumbers(container.storedEnergy))));
            lines.add(new ScreenLine(StatCollector.translateToLocalFormatted("gt.processing.energy_capacity",
                    GT_Utility.formatNumbers(container.energyCapacity))));
        }
        if (container.eut > 0) {
            lines.add(new ScreenLine(EnumChatFormatting.GREEN
                    + StatCollector.translateToLocalFormatted("gt.processing.generation", container.eut)));
        }
        if (container.mMaxProgressTime > 0) {
            for (GT_ProcessingOutputSummary.Entry<ItemStack> item :
                GT_ProcessingOutputSummary.items(container.outputItems)) {
                lines.add(outputLine(item.stack.getDisplayName(), item.amount, false));
            }
            for (GT_ProcessingOutputSummary.Entry<FluidStack> fluid :
                GT_ProcessingOutputSummary.fluids(container.outputFluids)) {
                lines.add(outputLine(fluid.stack.getLocalizedName(), fluid.amount, true));
            }
        }
        List<ScreenLine> wrapped = new ArrayList<>();
        for (ScreenLine line : lines) {
            for (Object part : fontRendererObj.listFormattedStringToWidth(line.text, TEXT_WIDTH)) {
                wrapped.add(new ScreenLine((String) part, line.color));
            }
        }
        return wrapped;
    }

    private static EnumChatFormatting resultColor(CheckRecipeResult result) {
        switch (result) {
        case NONE:
            return EnumChatFormatting.WHITE;
        case SUCCESS:
            return EnumChatFormatting.GREEN;
        case NO_RECIPE:
        case INSUFFICIENT_VOLTAGE:
        case INSUFFICIENT_POWER:
        case INSUFFICIENT_STARTUP_CAPACITY:
        case INSUFFICIENT_STARTUP_ENERGY:
        case ITEM_OUTPUT_FULL:
        case FLUID_OUTPUT_FULL:
        case INPUT_CHANGED:
        case MACHINE_CONDITION:
        case INSUFFICIENT_HEAT:
        case CLEANROOM_REQUIRED:
        case LOW_GRAVITY_REQUIRED:
        case WAITING_FOR_OUTPUT:
            return EnumChatFormatting.YELLOW;
        default:
            return EnumChatFormatting.RED;
        }
    }

    private String currentProgress(ProcessingGuiState container) {
        double percent = Math.round(1000.0 * container.mProgressTime / container.mMaxProgressTime) / 10.0;
        return EnumChatFormatting.WHITE
                + text("in_progress") + " " + String.format(Locale.ROOT, "%.2fs / %.2fs",
                        container.mProgressTime / 20.0, container.mMaxProgressTime / 20.0)
                + " (" + decimal(percent) + "%)";
    }

    private ScreenLine outputLine(String name, long amount, boolean fluid) {
        int duration = state.mMaxProgressTime;
        double perSecond = duration > 0 ? amount * 20.0 / duration : 0;
        String suffix = EnumChatFormatting.WHITE + " x " + EnumChatFormatting.GOLD + GT_Utility.formatNumbers(amount)
                + (fluid ? "L" : "") + EnumChatFormatting.WHITE;
        if (duration > 0) {
            suffix += perSecond > 1 ? " (" + decimal(Math.round(perSecond * 10) / 10.0) + "/s)"
                    : " (" + decimal(Math.round(10 / perSecond) / 10.0) + "s/ea)";
        }
        return new ScreenLine(EnumChatFormatting.AQUA + name + suffix);
    }

    private static String decimal(double number) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(number);
    }

    private void applyGuiTint() {
        if (GregTech_API.sColoredGUI) {
            byte colorIndex = machine.getBaseMetaTileEntity().getColorization();
            Dyes color = colorIndex == -1 ? Dyes.MACHINE_METAL : Dyes.get(colorIndex);
            GL11.glColor4f(color.mRGBa[0] / 255.0f, color.mRGBa[1] / 255.0f, color.mRGBa[2] / 255.0f, 1);
        } else {
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    private boolean enabled(int flag) {
        return (state.modes & flag) != 0;
    }

    private static UITexture texture(String path) {
        return UITexture.fullImage("gregtech", "gui/" + path);
    }

    private static UITexture overlay(String name) {
        return texture("overlay_button/" + name);
    }

    private static String text(String key) {
        return StatCollector.translateToLocal("gt.processing." + key);
    }

    private static String trans(String key, String english) {
        return GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_" + key, english, false);
    }

    private void drawText(FontRenderer fontRendererObj) {
        List<ScreenLine> lines = screenLines(fontRendererObj);
        scroll = Math.min(scroll, Math.max(0, lines.size() - VISIBLE_LINES));
        for (int row = 0; row < VISIBLE_LINES && scroll + row < lines.size(); row++) {
            ScreenLine line = lines.get(scroll + row);
            int y = SCREEN_TEXT_Y + row * LINE_HEIGHT;
            fontRendererObj.drawString(line.text, SCREEN_TEXT_X, y, line.color);
        }
        if (lines.size() > VISIBLE_LINES) {
            int trackHeight = VISIBLE_LINES * LINE_HEIGHT;
            int thumbHeight = Math.max(8, VISIBLE_LINES * trackHeight / lines.size());
            int thumbY = SCREEN_TEXT_Y + scroll * (trackHeight - thumbHeight) / (lines.size() - VISIBLE_LINES);
            Gui.drawRect(
                SCREEN_WIDTH - 2,
                thumbY,
                SCREEN_WIDTH,
                thumbY + thumbHeight,
                0xffffffff
            );
        }
    }

    private static final class ScreenLine {

        private final String text;
        private final int color;

        private ScreenLine(String text) {
            this(text, TEXT_COLOR);
        }

        private ScreenLine(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}
