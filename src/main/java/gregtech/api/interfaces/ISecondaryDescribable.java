package gregtech.api.interfaces;

/**
 * Already-localized item descriptions. The caller supplies keyboard state; implementations are server-safe.
 */
public interface ISecondaryDescribable {

    boolean hasSecondaryDescription();

    String[] getPrimaryDescription();

    String[] getSecondaryDescription();

    default String[] getCurrentDescription(boolean secondary) {
        return secondary && hasSecondaryDescription() ? getSecondaryDescription() : getPrimaryDescription();
    }
}
