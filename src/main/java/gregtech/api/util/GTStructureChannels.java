package gregtech.api.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import space.impact.structures.ImpactStructures;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;

/** Shared construction channels for GregTech and its addons. Bounds belong to each structure. */
public final class GTStructureChannels {
    public static final String HATCH = "gt_hatch";
    public static final String WIDTH = "width";
    public static final String DEPTH = "depth";
    public static final String HEIGHT = "height";
    public static final String LENGTH = "length";
    public static final String COIL = "coil";
    public static final String TIER = "tier";
    public static final String VARIANT = "variant";

    private static final Map<String, String> DESCRIPTIONS;

    static {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put(HATCH, "Automatically place missing GregTech hatches from your inventory");
        descriptions.put(WIDTH, "Structure width; see the controller tooltip for the size formula");
        descriptions.put(DEPTH, "Structure depth; see the controller tooltip for the size formula");
        descriptions.put(HEIGHT, "Structure height or vertical layers; see the controller tooltip for the size formula");
        descriptions.put(LENGTH, "Structure length or repeated sections; see the controller tooltip for the size formula");
        descriptions.put(COIL, "Coil tier; existing coils may determine the tier when completing a structure");
        descriptions.put(TIER, "Structure component tier");
        descriptions.put(VARIANT, "Structure variant; see the controller tooltip for the available variants");
        DESCRIPTIONS = Collections.unmodifiableMap(descriptions);
    }

    private GTStructureChannels() {
    }

    public static void registerDescriptions() {
        for (String channel : DESCRIPTIONS.keySet()) {
            ImpactStructures.registerChannelDescription(channel, "gregtech", translationKey(channel));
        }
    }

    private static String translationKey(String channel) {
        return HATCH.equals(channel) ? "gt.structure.hatch_channel" : "gt.structure.channel." + channel;
    }

    public static String getDescription(String channel) {
        String key = translationKey(channel);
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key)
                : DESCRIPTIONS.getOrDefault(channel, channel);
    }

    /** Unset subchannels retain the tool's master value, including legacy stack-size selection. */
    public static int getValue(ItemStack trigger, String channel) {
        return trigger == null ? 1 : ChannelDataAccessor.getChannelData(trigger, channel);
    }
}
