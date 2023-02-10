package crazypants.enderio.api.tool;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface ITool extends IHideFacades {
	boolean canUse(ItemStack var1, EntityPlayer var2, int var3, int var4, int var5);
	void used(ItemStack var1, EntityPlayer var2, int var3, int var4, int var5);
}