package gregtech.api.gui;

import codechicken.nei.recipe.GuiCraftingRecipe;
import com.gtnewhorizons.modularui.api.drawable.AdaptableUITexture;
import com.gtnewhorizons.modularui.api.drawable.UITexture;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Dyes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROL_COUNT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.CONTROLLER_FRAME_SIZE;
import static gregtech.api.gui.GT_Container_ProcessingMachine.POWER_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.NEI_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.RECIPE_MAP_SLOT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_WIDTH;
import static gregtech.api.gui.GT_Container_ProcessingMachine.GUI_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.SCREEN_HEIGHT;
import static gregtech.api.gui.GT_Container_ProcessingMachine.SCREEN_WIDTH;

/** GTNH GregTech controls, rendered over server-validated holographic slots. */
public class GT_GUIContainer_ProcessingMachine extends GT_GUIContainer_MultiMachine {
    private static final UITexture BACKGROUND = AdaptableUITexture.of("gregtech", "gui/background/singleblock_default",
            176, 166, 4);
    private static final UITexture SCREEN = AdaptableUITexture.of("gregtech", "gui/picture/screen_black", 16, 16, 2);
    private static final UITexture ITEM_SLOT = texture("slot/item_standard");
    private static final UITexture CONTROLLER_SLOT = AdaptableUITexture.of("gregtech", "gui/slot/item_standard",
            18, 18, 1);
    private static final UITexture BUTTON = AdaptableUITexture.of("gregtech", "gui/button/standard", 18, 18, 1);
    private static final UITexture BUTTON_PRESSED = AdaptableUITexture.of("gregtech", "gui/button/standard_pressed", 18,
            18, 1);
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
    private int scroll;

    public GT_GUIContainer_ProcessingMachine(InventoryPlayer player, IGregTechTileEntity base, String name,
            String texture) {
        super(new GT_Container_ProcessingMachine(player, base), name, texture);
        xSize = GUI_WIDTH;
        ySize = GUI_HEIGHT;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int x = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        if (wheel != 0 && insideScreen(x, y)) {
            scroll = Math.max(0,
                    Math.min(Math.max(0, screenLines().size() - VISIBLE_LINES), scroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        GT_Container_ProcessingMachine container = processingContainer();
        container.updateControlSlotPositions();
        Slot slot = container.getSlot(NEI_SLOT);
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        if (container.isControlVisible(NEI_SLOT)
                && x >= slot.xDisplayPosition - 1 && x < slot.xDisplayPosition + 17
                && y >= slot.yDisplayPosition - 1 && y < slot.yDisplayPosition + 17) {
            GT_Recipe_Map map = container.getNEIRecipeMap();
            if (button == 0 && container.isDisplayReady() && map != null) {
                GuiCraftingRecipe.openRecipeGui(map.mNEIName);
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        GT_Container_ProcessingMachine container = processingContainer();
        if (!container.isDisplayReady()) {
            return;
        }
        List<String> tooltip = new ArrayList<>();
        int controls = CONTROL_COUNT + (container.hasRecipeMapSelector() ? 1 : 0);
        for (int index = 0; index < controls; index++) {
            if (!container.isControlVisible(index)) {
                continue;
            }
            Slot slot = container.getSlot(index);
            if (x >= slot.xDisplayPosition && x < slot.xDisplayPosition + 16 && y >= slot.yDisplayPosition
                    && y < slot.yDisplayPosition + 16) {
                if (index == 0) {
                    tooltip.add(text("voiding"));
                    tooltip.add(text("voiding." + VOIDING_NAMES[container.getVoidingModeIndex()]));
                    tooltip.add(text("recipe_map_switch"));
                } else if (index < POWER_SLOT) {
                    tooltip.add(text("mode." + MODE_NAMES[index - 1]) + ": "
                            + text(enabled(MODE_FLAGS[index - 1]) ? "on" : "off"));
                    tooltip.add(text("mode." + MODE_NAMES[index - 1] + ".description"));
                } else if (index == POWER_SLOT) {
                    tooltip.add(text("power") + ": " + text(container.allowedToWork ? "on" : "off"));
                } else if (index == NEI_SLOT) {
                    tooltip.add(text("nei"));
                    tooltip.add(container.getNEIRecipeMapName());
                } else if (index == RECIPE_MAP_SLOT) {
                    tooltip.add(text("recipe_map") + ": " + container.getActiveRecipeMapName());
                    tooltip.add(text("recipe_map_switch"));
                }
                if (index != POWER_SLOT && index != NEI_SLOT) {
                    tooltip.add(text("next_cycle"));
                }
            }
        }
        if (!tooltip.isEmpty()) {
            drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
        }
    }

    private boolean insideScreen(int x, int y) {
        return x >= SCREEN_TEXT_X && x < SCREEN_WIDTH && y >= SCREEN_TEXT_Y && y < SCREEN_TEXT_Y + VISIBLE_LINES * LINE_HEIGHT;
    }

    private List<ScreenLine> screenLines() {
        GT_Container_ProcessingMachine container = processingContainer();
        List<ScreenLine> lines = new ArrayList<>();
        lines.add(new ScreenLine(mName, NAME_COLOR));
        if (!container.isDisplayReady()) {
            return lines;
        }
        if (container.hasRecipeMapSelector()) {
            lines.add(new ScreenLine(StatCollector.translateToLocalFormatted("gt.interact.desc.mb.mode",
                    container.getActiveRecipeMapName())));
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
        if (container.eut > 0) {
            lines.add(new ScreenLine(EnumChatFormatting.GREEN
                    + StatCollector.translateToLocalFormatted("gt.processing.generation", container.eut)));
        }
        List<ItemStack> items = new ArrayList<>(Arrays.asList(container.outputItems));
        items.removeIf(item -> item == null || item.stackSize <= 0);
        items.sort(Comparator.comparingInt((ItemStack item) -> item.stackSize).reversed());
        for (ItemStack item : items) {
            lines.add(outputLine(item.getDisplayName(), item.stackSize, false));
        }
        List<FluidStack> fluids = new ArrayList<>(Arrays.asList(container.outputFluids));
        fluids.removeIf(fluid -> fluid == null || fluid.amount <= 0);
        fluids.sort(Comparator.comparingInt((FluidStack fluid) -> fluid.amount).reversed());
        for (FluidStack fluid : fluids) {
            lines.add(outputLine(fluid.getLocalizedName(), fluid.amount, true));
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

    /** Progress format from the GTNH reference screenshot (PR #2252). */
    private String currentProgress(GT_Container_ProcessingMachine container) {
        double percent = Math.round(1000.0 * container.mProgressTime / container.mMaxProgressTime) / 10.0;
        return EnumChatFormatting.WHITE
                + text("in_progress") + " " + String.format(Locale.ROOT, "%.2fs / %.2fs",
                        container.mProgressTime / 20.0, container.mMaxProgressTime / 20.0)
                + " (" + decimal(percent) + "%)";
    }

    /** Aqua name, gold count and white production rate, as in GTNH PR #2252. */
    private ScreenLine outputLine(String name, long amount, boolean fluid) {
        int duration = processingContainer().mMaxProgressTime;
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

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        List<ScreenLine> lines = screenLines();
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
            drawRect(SCREEN_WIDTH - 2, thumbY, SCREEN_WIDTH, thumbY + thumbHeight, 0xffffffff);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        applyGuiTint();
        BACKGROUND.draw(guiLeft, guiTop, xSize, ySize);
        SCREEN.draw(guiLeft + SCREEN_LEFT, guiTop + SCREEN_TOP, SCREEN_WIDTH, SCREEN_HEIGHT);
        GT_Container_ProcessingMachine container = processingContainer();
        int controls = CONTROL_COUNT + (container.hasRecipeMapSelector() ? 1 : 0);
        container.updateControlSlotPositions();
        for (int index = 0; index < container.inventorySlots.size(); index++) {
            if (index >= controls) {
                Slot slot = container.getSlot(index);
                if (index == controls && container.getSlotCount() > 0) {
                    int padding = (CONTROLLER_FRAME_SIZE - 16) / 2;
                    CONTROLLER_SLOT.draw(guiLeft + slot.xDisplayPosition - padding,
                            guiTop + slot.yDisplayPosition - padding, CONTROLLER_FRAME_SIZE, CONTROLLER_FRAME_SIZE);
                } else {
                    ITEM_SLOT.draw(guiLeft + slot.xDisplayPosition - 1, guiTop + slot.yDisplayPosition - 1, 18, 18);
                }
            }
        }
        for (int index = 0; index < controls; index++) {
            if (!container.isControlVisible(index)) {
                continue;
            }
            Slot slot = container.getSlot(index);
            int x = guiLeft + slot.xDisplayPosition;
            int y = guiTop + slot.yDisplayPosition;
            if (!container.isDisplayReady()) {
                BUTTON.draw(x - 1, y - 1, 18, 18);
                continue;
            }
            UITexture icon;
            boolean pressed;
            if (index == 0) {
                int mode = container.getVoidingModeIndex();
                pressed = mode != 0;
                icon = VOIDING[mode];
            } else if (index < POWER_SLOT) {
                pressed = enabled(MODE_FLAGS[index - 1]);
                UITexture[] textures = index == 1 ? SEPARATION : index == 2 ? BATCH : LOCK;
                icon = textures[pressed ? 1 : 0];
            } else if (index == POWER_SLOT) {
                pressed = container.allowedToWork;
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
        GL11.glColor4f(1, 1, 1, 1);
    }

    private void applyGuiTint() {
        if (GregTech_API.sColoredGUI) {
            byte colorIndex = mContainer.mTileEntity.getColorization();
            Dyes color = colorIndex == -1 ? Dyes.MACHINE_METAL : Dyes.get(colorIndex);
            GL11.glColor4f(color.mRGBa[0] / 255.0f, color.mRGBa[1] / 255.0f, color.mRGBa[2] / 255.0f, 1);
        } else {
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    private boolean enabled(int flag) {
        return (processingContainer().modes & flag) != 0;
    }

    private GT_Container_ProcessingMachine processingContainer() {
        return (GT_Container_ProcessingMachine) mContainer;
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
