package gregtech.common.covers;

import gregtech.api.covers.CoverContext;

public class CoverLens extends CoverLegacyData {

    private final byte mColor;

    public CoverLens(CoverContext context, byte color) {
        super(context);
        this.mColor = color;
    }

    @Override
    public byte getLensColor() {
        return this.mColor;
    }
}
