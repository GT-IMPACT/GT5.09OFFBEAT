package gregtech.common.networkanalyzer.events;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.util.DimensionalCoord;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

public class NetworkAnalyzerPlayerTracker {

    private static final long UPDATE_INTERVAL_TICKS = 30L;

    private static class Entry {

        private final DimensionalCoord coord;
        private final long last;

        public Entry(DimensionalCoord coord, long last) {
            this.coord = coord;
            this.last = last;
        }
    }

    private static final Map<EntityPlayer, Entry> entries = new HashMap<>();

    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new NetworkAnalyzerPlayerTracker());
    }

    public static void clear() {
        entries.clear();
    }

    public static boolean needToUpdate(EntityPlayer player, DimensionalCoord coord) {
        final long now = player.worldObj.getTotalWorldTime();

        final Entry last = entries.get(player);
        if (last != null && Math.abs(now - last.last) < UPDATE_INTERVAL_TICKS && last.coord.isEqual(coord)) {
            return false;
        }
        entries.put(player, new Entry(coord, now));
        return true;
    }

    public static void reset(EntityPlayer player) {
        entries.remove(player);
    }

    @SubscribeEvent
    public void handlePlayerLogout(PlayerLoggedOutEvent ev) {
        reset(ev.player);
    }

    @SubscribeEvent
    public void handlePlayerChangedDimension(PlayerChangedDimensionEvent ev) {
        reset(ev.player);
    }

    @SubscribeEvent
    public void handlePlayerRespawn(PlayerRespawnEvent ev) {
        reset(ev.player);
    }

}
