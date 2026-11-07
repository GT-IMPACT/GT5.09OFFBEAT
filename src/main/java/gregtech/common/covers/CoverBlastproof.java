package gregtech.common.covers;

import gregtech.api.covers.CoverContext;

public class CoverBlastproof extends CoverLegacyData {

    private final float mLevel;

    public CoverBlastproof(CoverContext context, float level) {
        super(context);
        this.mLevel = level;
    }

    @Override
    public float getBlastProofLevel() {
        return this.mLevel;
    }
}
