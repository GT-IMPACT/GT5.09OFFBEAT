package gregtech.common.covers.modes;

import gregtech.api.interfaces.modularui.IKeyProvider;

public enum TransferMode implements IKeyProvider {

    EXPORT("gt.interact.desc.export"),
    IMPORT("gt.interact.desc.import");

    private final String key;

    TransferMode(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }
}
