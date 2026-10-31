package gregtech.api.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Resource-aware tooltip cache shared by electric and primitive multiblocks. */
public final class MultiblockTooltipCache {
    private static final AtomicLong RESOURCE_GENERATION = new AtomicLong();
    private final Supplier<MultiblockTooltipBuilder> factory;
    private long generation = -1;
    private MultiblockTooltipBuilder tooltip;

    public MultiblockTooltipCache(Supplier<MultiblockTooltipBuilder> factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public MultiblockTooltipBuilder get() {
        long current = RESOURCE_GENERATION.get();
        if (generation != current) {
            tooltip = factory.get();
            if (tooltip != null) tooltip.toolTipFinisher();
            generation = current;
        }
        return tooltip;
    }

    public static void invalidateAll() {
        RESOURCE_GENERATION.incrementAndGet();
    }
}
