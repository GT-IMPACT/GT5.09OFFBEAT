package gregtech.api.util;

import appeng.util.ReadableNumberConverter;
import mcp.mobius.waila.api.SpecialChars;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public abstract class GTWaila {

    public static String getStackListLine(String name, long amount) {
        return name + ": " + EnumChatFormatting.GOLD
                + ReadableNumberConverter.INSTANCE.toWideReadableForm(amount) + EnumChatFormatting.RESET;
    }

    public static String getMachineProgressString(boolean isActive, int maxProgresstime, int progresstime) {
        return getMachineProgressString(isActive, true, maxProgresstime, (long) progresstime);
    }

    public static String getMachineProgressString(boolean isActive, boolean isAllowedToWork, int maxProgresstime,
        int progresstime) {
        return getMachineProgressString(isActive, isAllowedToWork, maxProgresstime, (long) progresstime);
    }

    public static String getMachineProgressString(boolean isActive, long maxProgresstime, long progresstime) {
        return getMachineProgressString(isActive, true, maxProgresstime, progresstime);
    }

    public static String getMachineProgressString(boolean isActive, boolean isAllowedToWork, long maxProgresstime,
        long progresstime) {

        if (!isAllowedToWork) return StatCollector.translateToLocal("GT5U.waila.machine.working_disabled");
        if (!isActive) return StatCollector.translateToLocal("GT5U.waila.machine.idle");

        return SpecialChars.getRenderString("waila.gt.progress", progresstime + "", maxProgresstime + "");
    }
}
