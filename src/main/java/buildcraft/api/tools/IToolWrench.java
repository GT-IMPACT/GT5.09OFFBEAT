package buildcraft.api.tools;

import net.minecraft.entity.player.EntityPlayer;

public interface IToolWrench {
	boolean canWrench(EntityPlayer player, int x, int y, int z);
	void wrenchUsed(EntityPlayer player, int x, int y, int z);
}