package gregtech.api.enums;

/** Hatches higher in this enum are checked first when ejecting fluid. */
public enum OutputHatchType {

    Void,
    StandardFiltered,
    MECacheFiltered,
    MEFiltered,
    StandardUnfiltered,
    MECacheUnfiltered,
    MEUnfiltered;

    public static final OutputHatchType[] VALUES = values();

    public boolean isFiltered() {
        switch (this) {
            case Void:
            case StandardFiltered:
            case MECacheFiltered:
            case MEFiltered:
                return true;
            default:
                return false;
        }
    }
}
