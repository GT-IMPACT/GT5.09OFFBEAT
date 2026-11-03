package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.modes.BlockMode;
import gregtech.common.covers.modes.FilterDirectionMode;
import gregtech.common.covers.modes.FilterType;
import gregtech.common.gui.modularui.cover.CoverFluidfilterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraftforge.fluids.FluidRegistry;

/** GTNH's typed GUI API over the local fluid ID and three filter bits. */
public class CoverFluidfilter extends CoverLegacyBehavior {

    public CoverFluidfilter(CoverContext context, GT_CoverBehavior behavior) {
        super(context, behavior);
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverFluidfilterGui(this);
    }

    public int getFluidId() {
        return getVariable() >>> 3;
    }

    public CoverFluidfilter setFluidId(int fluidId) {
        if (isValid() && (fluidId == -1
            || fluidId >= 0 && fluidId <= 0x1fffffff && FluidRegistry.getFluid(fluidId) != null)) {
            setGuiVariable(getFilterMode() | (fluidId == -1 ? 0 : fluidId << 3));
        }
        return this;
    }

    public int getFilterMode() {
        return getVariable() & 7;
    }

    public CoverFluidfilter setFilterMode(int filterMode) {
        if (isValid() && filterMode >= 0 && filterMode <= 7) {
            setGuiVariable((getVariable() & ~7) | filterMode);
        }
        return this;
    }

    public FilterDirectionMode getFilterDirection() {
        return (getFilterMode() & 4) == 0 ? FilterDirectionMode.INPUT : FilterDirectionMode.OUTPUT;
    }

    public void setFilterDirection(FilterDirectionMode mode) {
        if (mode != null) {
            setFilterMode(mode == FilterDirectionMode.INPUT ? getFilterMode() & 3 : getFilterMode() | 4);
        }
    }

    public FilterType getFilterType() {
        return (getFilterMode() & 1) == 0 ? FilterType.WHITELIST : FilterType.BLACKLIST;
    }

    public void setFilterType(FilterType type) {
        if (type != null) {
            setFilterMode(type == FilterType.WHITELIST ? getFilterMode() & 6 : getFilterMode() | 1);
        }
    }

    public BlockMode getBlockMode() {
        return (getFilterMode() & 2) == 0 ? BlockMode.BLOCK : BlockMode.ALLOW;
    }

    public void setBlockMode(BlockMode mode) {
        if (mode != null) {
            setFilterMode(mode == BlockMode.BLOCK ? getFilterMode() & 5 : getFilterMode() | 2);
        }
    }
}
