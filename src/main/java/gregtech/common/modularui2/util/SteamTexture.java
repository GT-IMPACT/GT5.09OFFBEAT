package gregtech.common.modularui2.util;

import com.cleanroommc.modularui.drawable.UITexture;

import gregtech.api.enums.TieredVariant;

/**
 * Registers {@link UITexture}s for bronze, steel and primitive variants, all at once.
 */
public class SteamTexture {

    private final UITexture bronzeTexture;
    private final UITexture steelTexture;
    private final UITexture primitiveTexture;

    private SteamTexture(UITexture bronzeTexture, UITexture steelTexture, UITexture primitiveTexture) {
        this.bronzeTexture = bronzeTexture;
        this.steelTexture = steelTexture;
        this.primitiveTexture = primitiveTexture;
    }

    public UITexture get(TieredVariant variant) {
        switch (variant) {
            case BRONZE:
                return bronzeTexture;
            case STEEL:
                return steelTexture;
            case PRIMITIVE:
                return primitiveTexture;
            default:
                return null;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final SteamTexture NONE = new SteamTexture(null, null, null);

    public static class Builder {

        private Builder() {
        }

        private final UITexture.Builder bronzeBuilder = UITexture.builder();
        private final UITexture.Builder steelBuilder = UITexture.builder();
        private final UITexture.Builder primitiveBuilder = UITexture.builder();

        public Builder location(String pathPlaceholder) {
            bronzeBuilder.location("gregtech", String.format(pathPlaceholder, TieredVariant.BRONZE));
            steelBuilder.location("gregtech", String.format(pathPlaceholder, TieredVariant.STEEL));
            primitiveBuilder.location("gregtech", String.format(pathPlaceholder, TieredVariant.PRIMITIVE));
            return this;
        }

        public Builder imageSize(int w, int h) {
            bronzeBuilder.imageSize(w, h);
            steelBuilder.imageSize(w, h);
            primitiveBuilder.imageSize(w, h);
            return this;
        }

        public Builder fullImage() {
            bronzeBuilder.fullImage();
            steelBuilder.fullImage();
            primitiveBuilder.fullImage();
            return this;
        }

        public Builder adaptable(int border) {
            bronzeBuilder.adaptable(border);
            steelBuilder.adaptable(border);
            primitiveBuilder.adaptable(border);
            return this;
        }

        public Builder canApplyTheme() {
            bronzeBuilder.canApplyTheme();
            steelBuilder.canApplyTheme();
            primitiveBuilder.canApplyTheme();
            return this;
        }

        public Builder name(String bronzeName, String steelName, String primitiveName) {
            bronzeBuilder.name(bronzeName);
            steelBuilder.name(steelName);
            primitiveBuilder.name(primitiveName);
            return this;
        }

        public Builder name(String namePlaceholder) {
            bronzeBuilder.name(String.format(namePlaceholder, TieredVariant.BRONZE));
            steelBuilder.name(String.format(namePlaceholder, TieredVariant.STEEL));
            primitiveBuilder.name(String.format(namePlaceholder, TieredVariant.PRIMITIVE));
            return this;
        }

        public SteamTexture build() {
            return new SteamTexture(bronzeBuilder.build(), steelBuilder.build(), primitiveBuilder.build());
        }
    }
}

