package gregtech.api.util;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import space.impact.structures.hint.HintColor;
import space.impact.structures.structure.IChannelDefinition;
import space.impact.structures.structure.IStructureDefinition;

/**
 * Fluent multiblock descriptions. Create after registration and rebuild after a
 * language/resource reload.
 */
public class MultiblockTooltipBuilder {
    private static final Pattern QUANTITY = Pattern.compile("[0-9]+(?:[.,][0-9]+)*(?:[-–xх×][0-9]+(?:[.,][0-9]+)*)*(?:\\+|[xх×])?");
    private static final Pattern COMPACT_QUANTITY =
            Pattern.compile("[0-9]+(?:[.,][0-9]+)*(?:[-–xх×/][0-9]+(?:[.,][0-9]+)*)*(?:\\+|[xх×])?");
    private static final Pattern LARGE_INTEGER = Pattern.compile("(?<![0-9.,])[0-9]{4,}");
    private final List<String> information = new ArrayList<>();
    private final List<String> structure = new ArrayList<>();
    private final List<String> controls = new ArrayList<>();
    private final List<String> hints = new ArrayList<>();
    private final Map<HintColor, LinkedHashSet<String>> coloredHints = new LinkedHashMap<>();
    private final String machineName;
    private final Map<String, Map<String, String>> translatedText = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<String>> channelDescriptions = new LinkedHashMap<>();
    private final Map<String, String> defaultChannelDescriptions = new LinkedHashMap<>();
    private final LinkedHashSet<String> masterChannelDescriptions = new LinkedHashSet<>();
    private boolean structureSections;
    private boolean compactStructure;
    private boolean finished;

    /**
     * Formatting-only builder for callers that already supply localized strings.
     */
    public MultiblockTooltipBuilder() {
        machineName = null;
    }

    /**
     * Automatically registers English text under separate per-machine translation
     * keys for each section.
     */
    public MultiblockTooltipBuilder(String machineName) {
        this.machineName = Objects.requireNonNull(machineName);
        if (machineName.isEmpty())
            throw new IllegalArgumentException("Empty machine name");
    }

    private String text(String section, String english) {
        Objects.requireNonNull(english);
        if (machineName == null)
            return english;
        Map<String, String> entries = translatedText.computeIfAbsent(section, key -> new LinkedHashMap<>());
        return entries.computeIfAbsent(english, value -> translate(
                "TileEntity_DESCRIPTION_" + machineName + "_" + section + "_Index_" + entries.size(), value));
    }

    private static String translate(String key, String english, Object... args) {
        String translated = GT_LanguageManager.addStringLocalization(key, english, !GregTech_API.sPostloadFinished);
        if (!GT_LanguageManager.sUseEnglishFile && StatCollector.canTranslate(key)) {
            translated = StatCollector.translateToLocal(key);
        }
        return args.length == 0 ? translated : String.format(translated, args);
    }

    private static String tr(String key, Object... args) {
        return translate("gt.tooltip." + key, englishTemplate(key), args);
    }

    private static String englishTemplate(String key) {
        switch (key) {
        case "machine_type":
            return "Machine Type";
        case "dimensions":
            return "Dimensions (WxHxD): %s x %s x %s";
        case "hollow":
            return "Hollow";
        case "structure":
            return "Structure";
        case "controller":
            return "Controller";
        case "tiered":
            return "Tiered";
        case "minimum":
            return "At least %s";
        case "exactly":
            return "Exactly %s";
        case "optional_maximum":
            return "Optional, up to %s";
        case "legend":
            return "Hologram";
        case "air":
            return "Required air";
        case "hold_shift":
            return "Hold %s to display structure requirements";
        case "hold_ctrl":
            return "Hold %s to display machine controls";
        case "projector":
            return "Use the ImpactStructures hologram projector on the controller to show the structure; sneak-use to build.";
        case "projector_show":
            return "Use the ImpactStructures projector on the controller to show the structure.";
        case "projector_build":
            return "Sneak-use to build.";
        case "pollution":
            return "Produces %s pollution per second";
        case "channels":
            return "Structure channels";
        case "master_channel_label":
            return "Projector stack size";
        case "master_channel":
            return "Projector stack size determines: %s";
        case "hatch.energy":
            return "Energy Hatch";
        case "hatch.dynamo":
            return "Dynamo Hatch";
        case "hatch.maintenance":
            return "Maintenance Hatch";
        case "hatch.muffler":
            return "Muffler Hatch";
        case "hatch.input_bus":
            return "Input Bus";
        case "hatch.input_hatch":
            return "Input Hatch";
        case "hatch.output_bus":
            return "Output Bus";
        case "hatch.output_hatch":
            return "Output Hatch";
        default:
            throw new IllegalArgumentException("Unknown tooltip template: " + key);
        }
    }

    private void checkOpen() {
        if (finished)
            throw new IllegalStateException("Tooltip is already finished");
    }

    private MultiblockTooltipBuilder line(List<String> section, String text) {
        checkOpen();
        section.add(Objects.requireNonNull(text));
        return this;
    }

    private static String quantity(String amount) {
        return EnumChatFormatting.GOLD + amount + EnumChatFormatting.WHITE;
    }

    private String formula(String expression) {
        return EnumChatFormatting.GOLD + text("Formula", expression) + EnumChatFormatting.GRAY;
    }

    private static boolean isWordCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static String groupThousands(String amount) {
        Matcher matcher = LARGE_INTEGER.matcher(amount);
        StringBuilder result = new StringBuilder(amount.length());
        int previousEnd = 0;
        while (matcher.find()) {
            result.append(amount, previousEnd, matcher.start());
            String integer = matcher.group();
            try {
                result.append(GT_Utility.formatNumbers(Long.parseLong(integer)));
            } catch (NumberFormatException e) {
                result.append(integer);
            }
            previousEnd = matcher.end();
        }
        return result.append(amount, previousEnd, amount.length()).toString();
    }

    /** Format and highlight quantities after localization, preserving surrounding text formatting. */
    private String quantities(String text, EnumChatFormatting defaultColor) {
        StringBuilder result = new StringBuilder(text.length());
        String formatting = defaultColor.toString();
        Matcher matcher = (compactStructure ? COMPACT_QUANTITY : QUANTITY).matcher(text);
        char previous = ' ';
        for (int index = 0; index < text.length();) {
            char character = text.charAt(index);
            if (character == '\u00a7' && index + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(index + 1));
                String control = text.substring(index, index + 2);
                if ("0123456789abcdefr".indexOf(code) >= 0) {
                    formatting = control;
                } else if ("klmno".indexOf(code) >= 0) {
                    formatting += control;
                }
                result.append(control);
                index += 2;
                continue;
            }
            if (Character.isDigit(character) && !isWordCharacter(previous)
                    && matcher.region(index, text.length()).lookingAt()
                    && (matcher.end() == text.length() || !isWordCharacter(text.charAt(matcher.end())))) {
                String amount = compactStructure ? groupThousands(matcher.group()) : matcher.group();
                result.append(EnumChatFormatting.GOLD).append(amount).append(formatting);
                previous = text.charAt(matcher.end() - 1);
                index = matcher.end();
            } else {
                result.append(character);
                previous = character;
                index++;
            }
        }
        return result.toString();
    }

    public MultiblockTooltipBuilder addMachineType(String machine) {
        return line(information, tr("machine_type") + ": " + EnumChatFormatting.YELLOW
                + text("MachineType", machine) + EnumChatFormatting.RESET);
    }

    public MultiblockTooltipBuilder addInfo(String info) {
        return line(information, text("Information", info));
    }

    public MultiblockTooltipBuilder addInfo(String info, Object... arguments) {
        return line(information, String.format(Locale.ROOT, text("Information", info), arguments));
    }

    /** Compact layout is opt-in so existing machine and addon tooltips retain their layout. */
    public MultiblockTooltipBuilder useCompactStructure() {
        checkOpen();
        compactStructure = true;
        return this;
    }

    public MultiblockTooltipBuilder addControlInfo(String info) {
        return line(controls, text("Controls", info));
    }

    public boolean hasControlInfo() {
        return !controls.isEmpty();
    }

    public String[] getControlInfo() {
        toolTipFinisher();
        return controls.toArray(new String[0]);
    }

    public MultiblockTooltipBuilder addInfoAll(String... lines) {
        for (String text : lines)
            addInfo(text);
        return this;
    }

    public MultiblockTooltipBuilder addSeparator() {
        return addSeparator(EnumChatFormatting.GRAY, 30);
    }

    public MultiblockTooltipBuilder addSeparator(EnumChatFormatting color) {
        return addSeparator(color, 30);
    }

    public MultiblockTooltipBuilder addSeparator(EnumChatFormatting color, int length) {
        return line(information, separator(color, length));
    }

    private static String separator(EnumChatFormatting color, int length) {
        if (length < 0)
            throw new IllegalArgumentException("Negative separator length");
        StringBuilder text = new StringBuilder(color.toString());
        for (int i = 0; i < length; i++)
            text.append('-');
        return text.toString();
    }

    public MultiblockTooltipBuilder addPollutionAmount(int pollution) {
        return pollution == 0 ? this : line(information, tr("pollution", pollution));
    }

    public MultiblockTooltipBuilder beginStructureBlock(int width, int height, int depth, boolean hollow) {
        return beginVariableStructureBlock(width, width, height, height, depth, depth, hollow);
    }

    public MultiblockTooltipBuilder beginVariableStructureBlock(int minWidth, int maxWidth, int minHeight,
            int maxHeight, int minDepth, int maxDepth, boolean hollow) {
        line(structure, tr("dimensions", range(minWidth, maxWidth), range(minHeight, maxHeight),
                range(minDepth, maxDepth)) + (hollow && !compactStructure ? " (" + tr("hollow") + ")" : ""));
        if (compactStructure) {
            return this;
        }
        return line(structure, EnumChatFormatting.WHITE + tr("structure") + ":");
    }

    private static String range(int min, int max) {
        if (min < 0 || max < min)
            throw new IllegalArgumentException("Invalid range");
        return min == max ? Integer.toString(min) : min + "-" + max;
    }

    public MultiblockTooltipBuilder addController(String location) {
        if (compactStructure) {
            return line(structure, EnumChatFormatting.WHITE + tr("controller") + ": "
                    + EnumChatFormatting.GRAY + text("Location", location));
        }
        return part(tr("controller"), text("Location", location));
    }

    /** Opts into section headings without changing legacy tooltip layouts. */
    public MultiblockTooltipBuilder addStructureSection(String title) {
        checkOpen();
        structureSections = true;
        return sectionHeading(structure, text("StructureSection", title));
    }

    private MultiblockTooltipBuilder sectionHeading(List<String> destination, String title) {
        return line(destination, EnumChatFormatting.YELLOW + title + EnumChatFormatting.RESET);
    }

    public MultiblockTooltipBuilder addCasing(String count, String name, boolean tiered) {
        return casing(count, text("Casing", name), tiered);
    }

    private MultiblockTooltipBuilder casing(String count, String name, boolean tiered) {
        String amount = count.isEmpty() ? "" : EnumChatFormatting.GOLD + count + " ";
        return line(structure, "   " + amount + EnumChatFormatting.WHITE + name
                + (tiered ? " (" + tr("tiered") + ")" : ""));
    }

    public MultiblockTooltipBuilder addCasing(String count, ICasing casing, boolean tiered) {
        return casing(count, casing.getDisplayName(), tiered);
    }

    public MultiblockTooltipBuilder addCasingInfo(String count, String name, boolean tiered) {
        return addCasing(count, name, tiered);
    }

    public MultiblockTooltipBuilder addCasingInfoExactly(String name, int count, boolean tiered) {
        return addCasing(Integer.toString(count), name, tiered);
    }

    public MultiblockTooltipBuilder addCasingInfoExactly(ICasing casing, int count, boolean tiered) {
        return casing(Integer.toString(count), casing.getDisplayName(), tiered);
    }

    public MultiblockTooltipBuilder addCasingInfoMin(String name, int minimum, boolean tiered) {
        return addCasing(tr("minimum", minimum), name, tiered);
    }

    public MultiblockTooltipBuilder addCasingInfoMin(ICasing casing, int minimum, boolean tiered) {
        return casing(tr("minimum", minimum), casing.getDisplayName(), tiered);
    }

    public MultiblockTooltipBuilder addCasingInfoRange(String name, int min, int max, boolean tiered) {
        return addCasing(range(min, max), name, tiered);
    }

    public MultiblockTooltipBuilder addCasingInfoRange(ICasing casing, int min, int max, boolean tiered) {
        return casing(range(min, max), casing.getDisplayName(), tiered);
    }

    /**
     * Renders the same minimums used by machine validation and survival hatch
     * selection.
     */
    public MultiblockTooltipBuilder addRequirements(StructureRequirements requirements, String location) {
        return requirements(requirements, location, null);
    }

    private MultiblockTooltipBuilder requirements(StructureRequirements requirements, String location,
            HintColor color) {
        int start = structure.size();
        if (requirements.getMinimumCasings() > 0) {
            addCasingInfoMin(requirements.getCasing(), requirements.getMinimumCasings(), false);
        }
        for (Map.Entry<IHatchElement, Integer> entry : requirements.getHatches().entrySet()) {
            addHatch(entry.getKey(), entry.getValue(), location);
            if (color != null) {
                coloredHint(entry.getKey().getDescription(), color);
            }
        }
        hints.addAll(structure.subList(start, structure.size()));
        return this;
    }

    /**
     * Adds the hatch group's hologram color to the structure and construction
     * legend.
     */
    public MultiblockTooltipBuilder addRequirements(StructureRequirements requirements, String location,
            HintColor color) {
        return requirements(requirements, location, Objects.requireNonNull(color));
    }

    public MultiblockTooltipBuilder addHatch(IHatchElement hatch, int minimum, String location) {
        return hatch(hatch.getDescription(), minimum, location);
    }

    public MultiblockTooltipBuilder addHatch(String name, int minimum, String location) {
        return hatch(text("Hatch", name), minimum, location);
    }

    public MultiblockTooltipBuilder addHatchInfo(String name, String requirement) {
        part(text("Hatch", name), text("Location", requirement));
        return line(hints, structure.get(structure.size() - 1));
    }

    public MultiblockTooltipBuilder addHatchExactly(IHatchElement hatch, int count, String location, HintColor color) {
        return hatchRequirement(hatch, tr("exactly", count), location, color);
    }

    public MultiblockTooltipBuilder addOptionalHatch(
            IHatchElement hatch,
            int maximum,
            String location,
            HintColor color
    ) {
        return hatchRequirement(hatch, tr("optional_maximum", maximum), location, color);
    }

    private MultiblockTooltipBuilder hatchRequirement(
            IHatchElement hatch,
            String amount,
            String location,
            HintColor color
    ) {
        String name = hatch.getDescription();
        part(name, amount + "; " + text("Location", location));
        line(hints, structure.get(structure.size() - 1));
        coloredHint(name, color);
        return this;
    }

    private MultiblockTooltipBuilder hatch(String name, int minimum, String location, int... dots) {
        return part(name, tr("minimum", minimum) + "; " + text("Location", location), dots);
    }

    public MultiblockTooltipBuilder addOtherStructurePart(String name, String location, int... dots) {
        return part(text("Part", name), text("Location", location), dots);
    }

    private MultiblockTooltipBuilder part(String name, String location, int... dots) {
        line(structure, "   " + EnumChatFormatting.WHITE + name + ": " + EnumChatFormatting.GRAY + location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addOtherStructurePart(int count, String name, String location, int... dots) {
        return part(quantity(Integer.toString(count)) + " " + text("Part", name), text("Location", location), dots);
    }

    public MultiblockTooltipBuilder addOtherStructurePartExactly(int count, String name, String location) {
        return part(text("Part", name), tr("exactly", count) + "; " + text("Location", location));
    }

    public MultiblockTooltipBuilder addOtherStructurePart(ICasing casing, String location, int count) {
        return part(quantity(Integer.toString(count)) + " " + casing.getDisplayName(), text("Location", location));
    }

    public MultiblockTooltipBuilder addOtherStructurePart(ICasing casing, String location) {
        return part(casing.getDisplayName(), text("Location", location));
    }

    public MultiblockTooltipBuilder addStructurePartInfo(ICasing casing, String location, Object... arguments) {
        return part(casing.getDisplayName(), String.format(Locale.ROOT, text("Location", location), arguments));
    }

    public MultiblockTooltipBuilder addOtherStructurePart(ICasing casing, String location, String expression) {
        String description = String.format(Locale.ROOT, text("Location", location), formula(expression));
        return part(casing.getDisplayName(), description);
    }

    public MultiblockTooltipBuilder addOtherStructurePart(ItemStack stack, String location) {
        return part(stack.getDisplayName(), text("Location", location));
    }

    public MultiblockTooltipBuilder addOtherStructurePart(ItemStack stack, String location, int count) {
        return part(quantity(Integer.toString(count)) + " " + stack.getDisplayName(), text("Location", location));
    }

    public MultiblockTooltipBuilder addAir(String location) {
        String translatedLocation = text("Location", location);
        line(hints, tr("air") + ": " + translatedLocation);
        return part(tr("air"), translatedLocation);
    }

    public MultiblockTooltipBuilder addAir(int count, String location) {
        String requirement = tr("exactly", count) + "; " + text("Location", location);
        line(hints, tr("air") + ": " + requirement);
        return part(tr("air"), requirement);
    }

    public MultiblockTooltipBuilder addStructureInfo(String info) {
        return line(structure, "   " + EnumChatFormatting.GRAY + text("Structure", info));
    }

    /** Format configuration values after translating the stable template. */
    public MultiblockTooltipBuilder addStructureInfo(String info, Object... arguments) {
        return line(structure, "   " + EnumChatFormatting.GRAY + String.format(Locale.ROOT, text("Structure", info), arguments));
    }

    public MultiblockTooltipBuilder addStructureFooter(String info) {
        return line(structure, text("Footer", info));
    }

    public MultiblockTooltipBuilder addStructureInfoSeparator() {
        return line(structure, separator(EnumChatFormatting.GRAY, 30));
    }

    /** Repeated calls for one channel add detail rows under the same label. */
    public MultiblockTooltipBuilder addSubChannel(String channel, String description) {
        checkOpen();
        channelDescriptions.computeIfAbsent(Objects.requireNonNull(channel), key -> new LinkedHashSet<>())
                .add(text("Channel", description));
        return this;
    }

    public MultiblockTooltipBuilder addSubChannel(String channel, String description, String expression) {
        checkOpen();
        channelDescriptions.computeIfAbsent(Objects.requireNonNull(channel), key -> new LinkedHashSet<>())
                .add(String.format(Locale.ROOT, text("Channel", description), formula(expression)));
        return this;
    }

    /** Explicit machine descriptions take precedence over shared channel descriptions. */
    public MultiblockTooltipBuilder addStructureChannels(IStructureDefinition<?> definition) {
        checkOpen();
        for (IChannelDefinition channel : definition.getChannelDefinitions()) {
            String range = channel.getMaximumValue() == Integer.MAX_VALUE
                    ? channel.getMinimumValue() + "+"
                    : channel.getMinimumValue() + "-" + channel.getMaximumValue();
            defaultChannelDescriptions.putIfAbsent(channel.getChannel(),
                    GTStructureChannels.getDescription(channel.getChannel()) + " (" + range + ")");
        }
        return this;
    }

    public MultiblockTooltipBuilder addMasterChannel(String effect) {
        checkOpen();
        masterChannelDescriptions.add(text("Channel", effect));
        return this;
    }

    private void appendChannelSection(List<String> destination) {
        if (channelDescriptions.isEmpty() && masterChannelDescriptions.isEmpty()) return;
        sectionHeading(destination, tr("channels"));
        if (!masterChannelDescriptions.isEmpty()) {
            line(destination, "   " + EnumChatFormatting.AQUA + tr("master_channel_label") + EnumChatFormatting.RESET);
            for (String description : masterChannelDescriptions)
                line(destination, "      " + EnumChatFormatting.GRAY + description);
        }
        for (Map.Entry<String, LinkedHashSet<String>> channel : channelDescriptions.entrySet()) {
            String identifier = compactStructure ? ""
                    : EnumChatFormatting.DARK_GRAY + " (" + channel.getKey() + ")";
            line(destination, "   " + EnumChatFormatting.AQUA + GTStructureChannels.getLabel(channel.getKey())
                    + identifier + EnumChatFormatting.RESET);
            for (String description : channel.getValue())
                line(destination, "      " + EnumChatFormatting.GRAY + description);
        }
    }

    /**
     * GTNH-compatible key-based hint; numeric dots 1..16 map to ImpactStructures
     * colors 0..15.
     */
    public MultiblockTooltipBuilder addStructureHint(String nameKey, int... dots) {
        String name = StatCollector.canTranslate(nameKey) ? StatCollector.translateToLocal(nameKey)
                : text("Hint", nameKey);
        return dots.length == 0 ? line(hints, name) : hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addStructureHint(String name, HintColor color) {
        checkOpen();
        coloredHint(text("Hint", name), color);
        return this;
    }

    private void coloredHint(String name, HintColor color) {
        checkOpen();
        coloredHints.computeIfAbsent(Objects.requireNonNull(color), key -> new LinkedHashSet<>()).add(name);
    }

    private MultiblockTooltipBuilder hintNames(String name, int... dots) {
        for (int dot : dots)
            coloredHint(name, HintColor.fromMetadata(dot - 1));
        return this;
    }

    public MultiblockTooltipBuilder addEnergyHatch(String location, int... dots) {
        return addEnergyHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addEnergyHatch(int count, String location, int... dots) {
        String name = tr("hatch.energy");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addDynamoHatch(String location, int... dots) {
        return addDynamoHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addDynamoHatch(int count, String location, int... dots) {
        String name = tr("hatch.dynamo");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addMaintenanceHatch(String location, int... dots) {
        return addMaintenanceHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addMaintenanceHatch(int count, String location, int... dots) {
        String name = tr("hatch.maintenance");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addMufflerHatch(String location, int... dots) {
        return addMufflerHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addMufflerHatch(int count, String location, int... dots) {
        String name = tr("hatch.muffler");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addInputBus(String location, int... dots) {
        return addInputBus(1, location, dots);
    }

    public MultiblockTooltipBuilder addInputBus(int count, String location, int... dots) {
        String name = tr("hatch.input_bus");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addInputHatch(String location, int... dots) {
        return addInputHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addInputHatch(int count, String location, int... dots) {
        String name = tr("hatch.input_hatch");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addOutputBus(String location, int... dots) {
        return addOutputBus(1, location, dots);
    }

    public MultiblockTooltipBuilder addOutputBus(int count, String location, int... dots) {
        String name = tr("hatch.output_bus");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder addOutputHatch(String location, int... dots) {
        return addOutputHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addOutputHatch(int count, String location, int... dots) {
        String name = tr("hatch.output_hatch");
        return hatch(name, count, location, dots);
    }

    public MultiblockTooltipBuilder toolTipFinisher() {
        if (finished)
            return this;
        normalizeSeparators(information);
        if (!compactStructure && !information.isEmpty() && !isSeparator(information.get(information.size() - 1))) {
            addSeparator();
        }
        String shiftKey = EnumChatFormatting.YELLOW + "Shift" + EnumChatFormatting.GRAY;
        line(information, EnumChatFormatting.GRAY + tr("hold_shift", shiftKey));
        if (!controls.isEmpty()) {
            String ctrlKey = EnumChatFormatting.YELLOW + "Ctrl" + EnumChatFormatting.GRAY;
            line(information, EnumChatFormatting.GRAY + tr("hold_ctrl", ctrlKey));
        }
        if (structureSections && (compactStructure || !coloredHints.isEmpty())) {
            sectionHeading(structure, tr("legend"));
            sectionHeading(hints, tr("legend"));
        }
        if (compactStructure) {
            line(structure, "   " + tr("projector_show"));
            line(structure, "   " + tr("projector_build"));
            line(hints, "   " + tr("projector_show"));
            line(hints, "   " + tr("projector_build"));
        } else {
            line(structure, "   " + tr("projector"));
            line(hints, "   " + tr("projector"));
        }
        for (Map.Entry<HintColor, LinkedHashSet<String>> entry : coloredHints.entrySet()) {
            HintColor color = entry.getKey();
            String legend = "   " + GTStructureUtility.formatHintText(color, "■")
                    + (compactStructure ? " " : " - ") + String.join(", ", entry.getValue());
            line(hints, legend);
            line(structure, legend);
        }
        for (Map.Entry<String, String> channel : defaultChannelDescriptions.entrySet()) {
            if (!channelDescriptions.containsKey(channel.getKey()))
                addSubChannel(channel.getKey(), channel.getValue());
        }
        appendChannelSection(structure);
        appendChannelSection(hints);
        highlightQuantities(information);
        highlightQuantities(structure);
        highlightQuantities(controls);
        highlightQuantities(hints);
        finished = true;
        return this;
    }

    private static boolean isSeparator(String line) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(line).matches("-{3,}");
    }

    private static void normalizeSeparators(List<String> lines) {
        for (int i = lines.size() - 1; i > 0; i--)
            if (isSeparator(lines.get(i)) && isSeparator(lines.get(i - 1)))
                lines.remove(i);
    }

    private void highlightQuantities(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, quantities(lines.get(i), EnumChatFormatting.GRAY));
        }
    }

    /**
     * Retained for source compatibility with older GTNH call sites.
     */
    public MultiblockTooltipBuilder toolTipFinisher(String... ignoredAuthors) {
        return toolTipFinisher();
    }

    public String[] getInformation() {
        toolTipFinisher();
        return information.toArray(new String[0]);
    }

    public String[] getStructureInformation() {
        toolTipFinisher();
        return structure.toArray(new String[0]);
    }

    public String[] getStructureHint() {
        toolTipFinisher();
        return hints.toArray(new String[0]);
    }
}
