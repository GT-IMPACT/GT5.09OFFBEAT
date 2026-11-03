package gregtech.api.covers;

import gregtech.api.interfaces.ITexture;

final class CoverRegistration {

    private final CoverFactory factory;
    private final CoverPlacer coverPlacer;
    private final ITexture coverTexture;

    CoverRegistration(CoverFactory factory, CoverPlacer coverPlacer, ITexture coverTexture) {
        this.factory = factory;
        this.coverPlacer = coverPlacer;
        this.coverTexture = coverTexture;
    }

    CoverFactory getFactory() {
        return factory;
    }

    CoverPlacer getCoverPlacer() {
        return coverPlacer;
    }

    ITexture getCoverTexture() {
        return coverTexture;
    }
}
