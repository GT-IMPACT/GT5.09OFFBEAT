package gregtech.api.interfaces;

/** GTNH opt-in contract for a virtual configuration circuit; physical input slots must not implement this. */
public interface IConfigurationCircuitSupport {

    int getCircuitSlot();

    boolean allowSelectCircuit();

    default int getCircuitSlotX() {
        return 152;
    }

    default int getCircuitSlotY() {
        return 64;
    }
}
