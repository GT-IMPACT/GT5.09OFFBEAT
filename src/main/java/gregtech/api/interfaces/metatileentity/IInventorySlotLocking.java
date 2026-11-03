package gregtech.api.interfaces.metatileentity;

/** Shared slot lock state for inventory access and GUI indicators. */
public interface IInventorySlotLocking {

    default boolean isInventorySlotLocked(int slot) {
        return false;
    }
}
