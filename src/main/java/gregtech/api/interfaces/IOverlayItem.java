package gregtech.api.interfaces;

import net.minecraft.item.ItemStack;

public interface IOverlayItem {

    public IIconContainer getOverlay(boolean isOverlay, ItemStack aStack);

}
