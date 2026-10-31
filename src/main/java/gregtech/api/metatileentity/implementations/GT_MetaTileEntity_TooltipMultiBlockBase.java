package gregtech.api.metatileentity.implementations;

import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.MultiblockTooltipCache;

/**
 * Optional tooltip support, following GTNH's tooltip multiblock base without client dependencies.
 */
public abstract class GT_MetaTileEntity_TooltipMultiBlockBase extends GT_MetaTileEntity_MultiBlockBase
        implements ISecondaryDescribable {
    private final MultiblockTooltipCache tooltipCache = new MultiblockTooltipCache(this::createTooltip);

    protected GT_MetaTileEntity_TooltipMultiBlockBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GT_MetaTileEntity_TooltipMultiBlockBase(String name) {
        super(name);
    }

    /**
     * Return null to keep a subclass's legacy getDescription implementation.
     */
    protected MultiblockTooltipBuilder createTooltip() {
        return null;
    }

    protected final MultiblockTooltipBuilder getTooltip() {
        return tooltipCache.get();
    }

    /**
     * Called by the client resource listener. No registry of machine instances is retained.
     */
    public static void invalidateTooltipCache() {
        MultiblockTooltipCache.invalidateAll();
    }

    @Override
    public final boolean hasSecondaryDescription() {
        return getTooltip() != null;
    }

    @Override
    public String[] getDescription() {
        MultiblockTooltipBuilder tooltip = getTooltip();
        return tooltip == null ? new String[0] : tooltip.getInformation();
    }

    @Override
    public final String[] getPrimaryDescription() {
        MultiblockTooltipBuilder tooltip = getTooltip();
        return tooltip == null ? getDescription() : tooltip.getInformation();
    }

    @Override
    public final String[] getSecondaryDescription() {
        MultiblockTooltipBuilder tooltip = getTooltip();
        return tooltip == null ? getDescription() : tooltip.getStructureInformation();
    }
}
