package gregtech.api.interfaces;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/** GTNH data-stick contract; the type identifier distinguishes compatible machine settings. */
public interface IDataCopyable {

    @Nullable
    NBTTagCompound getCopiedData(EntityPlayer player);

    boolean pasteCopiedData(EntityPlayer player, @Nullable NBTTagCompound nbt);

    String getCopiedDataIdentifier(EntityPlayer player);
}
