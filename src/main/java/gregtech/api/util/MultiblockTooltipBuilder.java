package gregtech.api.util;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.*;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import space.impact.api.hint.HintColor;

/**
 * Fluent multiblock descriptions. Create after registration and rebuild after a language/resource reload.
 */
public class MultiblockTooltipBuilder {
    private final List<String> information = new ArrayList<>();
    private final List<String> structure = new ArrayList<>();
    private final List<String> hints = new ArrayList<>();
    private final Map<HintColor, LinkedHashSet<String>> coloredHints = new LinkedHashMap<>();
    private final String machineName;
    private final Map<String, Map<String, String>> translatedText = new LinkedHashMap<>();
    private boolean finished;

    /**
     * Formatting-only builder for callers that already supply localized strings.
     */
    public MultiblockTooltipBuilder() {
        machineName = null;
    }

    /**
     * Automatically registers English text under separate per-machine translation keys for each section.
     */
    public MultiblockTooltipBuilder(String machineName) {
        this.machineName = Objects.requireNonNull(machineName);
        if (machineName.isEmpty()) throw new IllegalArgumentException("Empty machine name");
    }

    private String text(String section, String english) {
        Objects.requireNonNull(english);
        if (machineName == null) return english;
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
            case "air":
                return "Required air";
            case "hold_shift":
                return "Hold Shift to display structure requirements";
            case "projector":
                return "Use the ImpactStructures hologram projector on the controller to show the structure; sneak-use to build.";
            case "pollution":
                return "Produces %s pollution per second";
            case "channel":
                return "Channel %s: %s";
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
        if (finished) throw new IllegalStateException("Tooltip is already finished");
    }

    private MultiblockTooltipBuilder line(List<String> section, String text) {
        checkOpen();
        section.add(Objects.requireNonNull(text));
        return this;
    }

    public MultiblockTooltipBuilder addMachineType(String machine) {
        return line(information, tr("machine_type") + ": " + EnumChatFormatting.YELLOW
                + text("MachineType", machine) + EnumChatFormatting.RESET);
    }

    public MultiblockTooltipBuilder addInfo(String info) {
        return line(information, text("Information", info));
    }

    public MultiblockTooltipBuilder addInfoAll(String... lines) {
        for (String text : lines) addInfo(text);
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
        if (length < 0) throw new IllegalArgumentException("Negative separator length");
        StringBuilder text = new StringBuilder(color.toString());
        for (int i = 0; i < length; i++) text.append('-');
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
                range(minDepth, maxDepth)) + (hollow ? " (" + tr("hollow") + ")" : ""));
        return line(structure, EnumChatFormatting.WHITE + tr("structure") + ":");
    }

    private static String range(int min, int max) {
        if (min < 0 || max < min) throw new IllegalArgumentException("Invalid range");
        return min == max ? Integer.toString(min) : min + "-" + max;
    }

    public MultiblockTooltipBuilder addController(String location) {
        return part(tr("controller"), text("Location", location));
    }

    public MultiblockTooltipBuilder addCasing(String count, String name, boolean tiered) {
        return casing(count, text("Casing", name), tiered);
    }

    private MultiblockTooltipBuilder casing(String count, String name, boolean tiered) {
        return line(structure, "   " + EnumChatFormatting.GOLD + count + " " + EnumChatFormatting.WHITE + name
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
     * Renders the same minimums used by machine validation and survival hatch selection.
     */
    public MultiblockTooltipBuilder addRequirements(StructureRequirements requirements, String location) {
        int start = structure.size();
        addCasingInfoMin(requirements.getCasing(), requirements.getMinimumCasings(), false);
        for (Map.Entry<IHatchElement, Integer> entry : requirements.getHatches().entrySet()) {
            addHatch(entry.getKey(), entry.getValue(), location);
        }
        hints.addAll(structure.subList(start, structure.size()));
        return this;
    }

    public MultiblockTooltipBuilder addHatch(IHatchElement hatch, int minimum, String location) {
        return hatch(hatch.getDescription(), minimum, location);
    }

    public MultiblockTooltipBuilder addHatch(String name, int minimum, String location) {
        return hatch(text("Hatch", name), minimum, location);
    }

    private MultiblockTooltipBuilder hatch(String name, int minimum, String location) {
        return part(name, tr("minimum", minimum) + "; " + text("Location", location));
    }

    public MultiblockTooltipBuilder addOtherStructurePart(String name, String location, int... dots) {
        return part(text("Part", name), text("Location", location), dots);
    }

    private MultiblockTooltipBuilder part(String name, String location, int... dots) {
        line(structure, "   " + EnumChatFormatting.WHITE + name + ": " + EnumChatFormatting.GRAY + location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addOtherStructurePart(int count, String name, String location, int... dots) {
        return part(count + " " + text("Part", name), text("Location", location), dots);
    }

    public MultiblockTooltipBuilder addOtherStructurePart(ICasing casing, String location, int count) {
        return part(count + " " + casing.getDisplayName(), text("Location", location));
    }

    public MultiblockTooltipBuilder addAir(String location) {
        String translatedLocation = text("Location", location);
        line(hints, tr("air") + ": " + translatedLocation);
        return part(tr("air"), translatedLocation);
    }

    public MultiblockTooltipBuilder addStructureInfo(String info) {
        return line(structure, "   " + text("Structure", info));
    }

    public MultiblockTooltipBuilder addStructureFooter(String info) {
        return line(structure, text("Footer", info));
    }

    public MultiblockTooltipBuilder addStructureInfoSeparator() {
        return line(structure, separator(EnumChatFormatting.GRAY, 30));
    }

    public MultiblockTooltipBuilder addSubChannel(String channel, String description) {
        String text = tr("channel", channel, text("Channel", description));
        line(hints, text);
        return line(structure, "   " + text);
    }

    public MultiblockTooltipBuilder addMasterChannel(String effect) {
        String text = tr("master_channel", text("Channel", effect));
        line(hints, text);
        return line(structure, "   " + text);
    }

    /**
     * GTNH-compatible key-based hint; numeric dots 1..16 map to ImpactStructures colors 0..15.
     */
    public MultiblockTooltipBuilder addStructureHint(String nameKey, int... dots) {
        String name = StatCollector.canTranslate(nameKey) ? StatCollector.translateToLocal(nameKey) : text("Hint", nameKey);
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
        for (int dot : dots) coloredHint(name, HintColor.fromMetadata(dot - 1));
        return this;
    }

    public MultiblockTooltipBuilder addEnergyHatch(String location, int... dots) {
        return addEnergyHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addEnergyHatch(int count, String location, int... dots) {
        String name = tr("hatch.energy");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addDynamoHatch(String location, int... dots) {
        return addDynamoHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addDynamoHatch(int count, String location, int... dots) {
        String name = tr("hatch.dynamo");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addMaintenanceHatch(String location, int... dots) {
        return addMaintenanceHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addMaintenanceHatch(int count, String location, int... dots) {
        String name = tr("hatch.maintenance");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addMufflerHatch(String location, int... dots) {
        return addMufflerHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addMufflerHatch(int count, String location, int... dots) {
        String name = tr("hatch.muffler");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addInputBus(String location, int... dots) {
        return addInputBus(1, location, dots);
    }

    public MultiblockTooltipBuilder addInputBus(int count, String location, int... dots) {
        String name = tr("hatch.input_bus");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addInputHatch(String location, int... dots) {
        return addInputHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addInputHatch(int count, String location, int... dots) {
        String name = tr("hatch.input_hatch");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addOutputBus(String location, int... dots) {
        return addOutputBus(1, location, dots);
    }

    public MultiblockTooltipBuilder addOutputBus(int count, String location, int... dots) {
        String name = tr("hatch.output_bus");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder addOutputHatch(String location, int... dots) {
        return addOutputHatch(1, location, dots);
    }

    public MultiblockTooltipBuilder addOutputHatch(int count, String location, int... dots) {
        String name = tr("hatch.output_hatch");
        hatch(name, count, location);
        return hintNames(name, dots);
    }

    public MultiblockTooltipBuilder toolTipFinisher() {
        if (finished) return this;
        line(information, EnumChatFormatting.GRAY + tr("hold_shift"));
        line(structure, tr("projector"));
        line(hints, tr("projector"));
        for (Map.Entry<HintColor, LinkedHashSet<String>> entry : coloredHints.entrySet()) {
            line(hints, StatCollector.translateToLocal("impactapi.blockhint." + entry.getKey().getMetadata()
                    + ".name") + ": " + String.join(", ", entry.getValue()));
        }
        finished = true;
        return this;
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
