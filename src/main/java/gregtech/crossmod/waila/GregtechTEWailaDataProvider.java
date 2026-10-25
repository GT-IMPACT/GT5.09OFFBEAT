package gregtech.crossmod.waila;

import gregtech.api.enums.Dyes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.IGregtechWailaProvider;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.common.covers.GT_Cover_Fluidfilter;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

public class GregtechTEWailaDataProvider implements IWailaDataProvider {

    private IGregtechWailaProvider getProvider(IWailaDataAccessor accessor) {
        TileEntity tile = accessor.getTileEntity();
        return tile instanceof IGregtechWailaProvider ? (IGregtechWailaProvider) tile : null;
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        IGregtechWailaProvider provider = getProvider(accessor);
        return provider == null ? null : provider.getWailaStack(accessor, config);
    }

    @Override
    public List<String> getWailaHead(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                                     IWailaConfigHandler config) {
        return tooltip;
    }

    @Override
    public List<String> getWailaBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                                     IWailaConfigHandler config) {
        IGregtechWailaProvider provider = getProvider(accessor);
        if (provider != null) {
            NBTTagCompound tag = accessor.getNBTData();
            if (tag.hasKey("gt_colorization")) {
                tooltip.add(StatCollector.translateToLocalFormatted("GT5U.waila.color", tag.getString("gt_colorization")));
            }
            if (config.getConfig("gregtech.fluidFilter", true)) {
                String key = "filterInfo" + accessor.getSide().ordinal();
                if (tag.hasKey(key)) {
                    tooltip.add(tag.getString(key));
                }
            }
            provider.getWailaBody(stack, tooltip, accessor, config);
        }
        return tooltip;
    }

    @Override
    public boolean hasWailaAdvancedBody(ItemStack stack, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        IGregtechWailaProvider provider = getProvider(accessor);
        return provider != null && provider.hasWailaAdvancedBody(stack, accessor, config);
    }

    @Override
    public List<String> getWailaAdvancedBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                                             IWailaConfigHandler config) {
        IGregtechWailaProvider provider = getProvider(accessor);
        if (provider != null) {
            provider.getWailaAdvancedBody(stack, tooltip, accessor, config);
        }
        return tooltip;
    }

    @Override
    public List<String> getWailaTail(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
                                     IWailaConfigHandler config) {
        return tooltip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag,
                                     World world, int x, int y, int z) {
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            Dyes color = Dyes.get(base.getColorization());
            if (color != Dyes._NULL) {
                tag.setString("gt_colorization", color.mName);
            } else {
                tag.removeTag("gt_colorization");
            }
            if (tile instanceof BaseMetaPipeEntity) {
                for (byte side = 0; side < 6; side++) {
                    String key = "filterInfo" + side;
                    if (base.getCoverBehaviorAtSide(side) instanceof GT_Cover_Fluidfilter) {
                        tag.setString(key, base.getCoverBehaviorAtSide(side).getDescription(side,
                                base.getCoverIDAtSide(side), base.getCoverDataAtSide(side), base));
                    } else {
                        tag.removeTag(key);
                    }
                }
            }
        }
        if (tile instanceof IGregtechWailaProvider) {
            ((IGregtechWailaProvider) tile).getWailaNBTData(player, tile, tag, world, x, y, z);
        }
        return tag;
    }
}
