package gregtech.api.interfaces;

/**
 * Already-localized item descriptions. The caller supplies keyboard state;
 * implementations are server-safe.
 */
public interface ISecondaryDescribable {

    boolean hasSecondaryDescription();

    String[] getPrimaryDescription();

    String[] getSecondaryDescription();

    default boolean hasControlDescription() {
        return false;
    }

    default String[] getControlDescription() {
        return getPrimaryDescription();
    }

    default String[] getCurrentDescription(boolean secondary, boolean controls) {
        if (controls)
            return hasControlDescription() ? getControlDescription() : getPrimaryDescription();
        return getCurrentDescription(secondary);
    }

    default String[] getCurrentDescription(boolean secondary) {
        return secondary && hasSecondaryDescription() ? getSecondaryDescription() : getPrimaryDescription();
    }
}
