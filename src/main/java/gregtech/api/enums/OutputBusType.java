package gregtech.api.enums;

/** Buses higher in this enum are checked first when ejecting output. */
public enum OutputBusType {

    Void,
    CompressedFiltered,
    StandardFiltered,
    MECacheFiltered,
    MEFiltered,
    CompressedUnfiltered,
    StandardUnfiltered,
    MECacheUnfiltered,
    MEUnfiltered;

    public static final OutputBusType[] VALUES = values();

    public boolean isFiltered() {
        switch (this) {
            case Void:
            case CompressedFiltered:
            case StandardFiltered:
            case MECacheFiltered:
            case MEFiltered:
                return true;
            default:
                return false;
        }
    }
}
