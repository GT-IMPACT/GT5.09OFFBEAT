package gregtech.common.items.behaviors;

import gregtech.api.items.GT_MetaBase_Item;
import gregtech.api.util.GT_ModHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IShearable;

import java.util.ArrayList;
import java.util.Random;

public class Behaviour_Knife extends Behaviour_None {
 
	@Override
	public boolean itemInteractionForEntity(GT_MetaBase_Item aItem, ItemStack itemstack, EntityPlayer player, EntityLivingBase entity) {
		if (entity.worldObj.isRemote) {
			return false;
		}
		if (entity instanceof IShearable) {
			IShearable target = (IShearable) entity;
			if (target.isShearable(itemstack, entity.worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ)) {
				ArrayList<ItemStack> drops = target.onSheared(itemstack, entity.worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ,
                        EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack));
                
				Random rand = new Random();
				for (ItemStack stack : drops) {
					EntityItem ent = entity.entityDropItem(stack, 1.0F);
					ent.motionY += rand.nextFloat() * 0.05F;
					ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
					ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
					break;
				}
				GT_ModHandler.damageOrDechargeItem(itemstack, 1, 200, player);
			}
			return true;
		}
		return false;
	}
}