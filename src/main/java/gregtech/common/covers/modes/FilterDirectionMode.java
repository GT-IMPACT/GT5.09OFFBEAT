package gregtech.common.covers.modes;

import gregtech.api.interfaces.modularui.IKeyProvider;

public enum FilterDirectionMode implements IKeyProvider {

    INPUT("gt.interact.desc.FluidFilter.FilterInput"),
    OUTPUT("gt.interact.desc.FluidFilter.FilterOutput");

    private final String key;

    FilterDirectionMode(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }
}
