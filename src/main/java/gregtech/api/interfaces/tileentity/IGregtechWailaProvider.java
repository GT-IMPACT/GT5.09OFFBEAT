package gregtech.api.interfaces.tileentity;

import cpw.mods.fml.common.Optional;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.List;

/**
 * Waila hooks shared by GregTech tile entities and their meta tile entities.
 * Only data explicitly written to the supplied tag is synchronized with the client.
 * Overrides must also use {@code @Optional.Method(modid = "Waila")} to keep Waila optional.
 */
public interface IGregtechWailaProvider {

    @Optional.Method(modid = "Waila")
    default ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Optional.Method(modid = "Waila")
    default void getWailaBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                              IWailaConfigHandler config) {
    }

    @Optional.Method(modid = "Waila")
    default boolean hasWailaAdvancedBody(ItemStack stack, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return false;
    }

    @Optional.Method(modid = "Waila")
    default void getWailaAdvancedBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                                      IWailaConfigHandler config) {
    }

    @Optional.Method(modid = "Waila")
    default void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag,
                                 World world, int x, int y, int z) {
    }
}
