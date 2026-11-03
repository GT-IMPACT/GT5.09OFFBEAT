package gregtech.api.interfaces.metatileentity;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/** GTNH contract for a copied item template used as a machine output filter. */
public interface IItemLockable {

    void setLockedItem(@Nullable ItemStack itemStack);

    @Nullable
    ItemStack getLockedItem();

    void clearLock();

    boolean isLocked();

    default boolean acceptsItemLock() {
        return false;
    }
}
