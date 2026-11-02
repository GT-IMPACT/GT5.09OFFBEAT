package gregtech.common.covers.conditions;

public enum RedstoneCondition {

    ENABLE_WITH_REDSTONE("gt.ui.cover.work.enable_with_redstone"),
    DISABLE_WITH_REDSTONE("gt.ui.cover.work.disable_with_redstone"),
    DISABLE("gt.ui.cover.work.disabled");

    private final String translationKey;

    RedstoneCondition(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static RedstoneCondition fromLegacyData(int value) {
        if (value < 0 || value >= values().length) {
            return DISABLE;
        }
        return values()[value];
    }
}
