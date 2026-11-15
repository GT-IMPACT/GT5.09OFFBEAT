package gregtech.common.tools;

import appeng.api.util.DimensionalCoord;
import gregtech.api.enums.GT_Values;
import gregtech.api.graphs.Node;
import gregtech.api.graphs.PowerNode;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.GT_Tool_Item;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Cable;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.common.networkanalyzer.NetworkAnalyzerData;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.AnalyzerModes;
import gregtech.common.networkanalyzer.events.NetworkAnalyzerPlayerTracker;
import gregtech.common.networkanalyzer.net.GTPacketNetworkAnalyzer;
import gregtech.common.networkanalyzer.network.PowerNetworkBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ItemNetworkAnalyzer extends GT_Tool_Item {

    public ItemNetworkAnalyzer() {
        super("NetworkAnalyzer", "Network Analyzer", "", 0, 0, true);
    }

    public static AnalyzerModes getMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        int mode = tag == null ? 0 : tag.getByte("mode") & 0xFF;
        return mode < AnalyzerModes.VALUES.length ? AnalyzerModes.VALUES[mode] : AnalyzerModes.TOPOLOGY;
    }

    public static DimensionalCoord getLocation(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("dim", 10) ? DimensionalCoord.readFromNBT(tag.getCompoundTag("dim")) : null;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static String modeKey(AnalyzerModes mode) {
        return "gt.NetworkAnalyzer.mode." + mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void addAdditionalToolTips(List list, ItemStack stack, EntityPlayer player) {
        list.add(StatCollector.translateToLocal("gt.NetworkAnalyzer.tooltip.description"));
        list.add(StatCollector.translateToLocal("gt.NetworkAnalyzer.tooltip.inspect"));
        list.add(StatCollector.translateToLocal("gt.NetworkAnalyzer.tooltip.switch_mode"));

        String mode = StatCollector.translateToLocal(modeKey(getMode(stack)));
        list.add(StatCollector.translateToLocalFormatted("gt.NetworkAnalyzer.mode", mode));
    }

    @Override
    public boolean onItemUseFirst(
        ItemStack stack,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z,
        int side,
        float hitX,
        float hitY,
        float hitZ
    ) {
        return !world.isRemote && onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean onItemUse(
        ItemStack stack,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z,
        int side,
        float hitX,
        float hitY,
        float hitZ
    ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!isCable(tile) && getMultiblock(tile) == null) {
            return false;
        }
        if (!world.isRemote) {
            DimensionalCoord target = new DimensionalCoord(tile);
            NBTTagCompound location = new NBTTagCompound();
            target.writeToNBT(location);
            getOrCreateTag(stack).setTag("dim", location);
            NetworkAnalyzerPlayerTracker.reset(player);
            player.addChatMessage(new ChatComponentTranslation("gt.NetworkAnalyzer.bound", x, y, z));
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player.isSneaking()) {
            AnalyzerModes[] modes = AnalyzerModes.VALUES;
            AnalyzerModes mode = modes[(getMode(stack).ordinal() + 1) % modes.length];
            getOrCreateTag(stack).setByte("mode", (byte) mode.ordinal());
            NetworkAnalyzerPlayerTracker.reset(player);
            player.addChatMessage(new ChatComponentTranslation(
                "gt.NetworkAnalyzer.set", new ChatComponentTranslation(modeKey(mode))
            ));
        }
        return stack;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        if (!held || world.isRemote || !(entity instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) entity;
        DimensionalCoord target = getLocation(stack);
        if (target == null || target.getDimension() != world.provider.dimensionId
            || !NetworkAnalyzerPlayerTracker.needToUpdate(player, target)) {
            return;
        }

        if (!world.blockExists(target.x, target.y, target.z)) {
            GT_Values.NW.sendToPlayer(new GTPacketNetworkAnalyzer(target, new NetworkAnalyzerData()), player);
            return;
        }
        TileEntity tile = world.getTileEntity(target.x, target.y, target.z);
        GT_MetaTileEntity_MultiBlockBase multiblock = getMultiblock(tile);
        PowerNetworkBuilder builder = new PowerNetworkBuilder();
        if (isCable(tile)) {
            PowerNode node = findNode((BaseMetaPipeEntity) tile);
            if (node != null) {
                builder.addNetwork(node);
            }
        } else if (multiblock != null) {
            collectHatchNetworks(multiblock.mEnergyHatches, builder);
            collectHatchNetworks(multiblock.mDynamoHatches, builder);
            collectHatchNetworks(multiblock.mEnergyHatchesMulti, builder);
            collectHatchNetworks(multiblock.mDynamoHatchesMulti, builder);
        } else {
            stack.getTagCompound().removeTag("dim");
            NetworkAnalyzerPlayerTracker.reset(player);
            GT_Values.NW.sendToPlayer(new GTPacketNetworkAnalyzer(target, new NetworkAnalyzerData()), player);
            player.addChatMessage(new ChatComponentTranslation("gt.NetworkAnalyzer.target_lost"));
            return;
        }
        GT_Values.NW.sendToPlayer(new GTPacketNetworkAnalyzer(target, builder.getData(getMode(stack))), player);
    }

    private static void collectHatchNetworks(
        Iterable<? extends GT_MetaTileEntity_Hatch> hatches,
        PowerNetworkBuilder builder
    ) {
        for (GT_MetaTileEntity_Hatch hatch : hatches) {
            IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            if (base == null || base.isDead()) {
                continue;
            }
            TileEntity neighbor = base.getTileEntityAtSide(base.getFrontFacing());
            if (isCable(neighbor)) {
                PowerNode node = findNode((BaseMetaPipeEntity) neighbor);
                if (node != null) {
                    builder.addNetwork(node);
                }
            }
        }
    }

    private static PowerNode findNode(BaseMetaPipeEntity pipe) {
        Set<BaseMetaPipeEntity> visited = new HashSet<>();
        ArrayDeque<BaseMetaPipeEntity> queue = new ArrayDeque<>();
        queue.add(pipe);
        visited.add(pipe);

        while (!queue.isEmpty()) {
            BaseMetaPipeEntity current = queue.remove();
            Node node = current.getNode();
            if (node instanceof PowerNode) {
                return (PowerNode) node;
            }

            GT_MetaPipeEntity_Cable cable = (GT_MetaPipeEntity_Cable) current.getMetaTileEntity();
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                if (!cable.isConnectedAtSide(side.ordinal())) {
                    continue;
                }
                TileEntity neighbor = current.getTileEntityAtSide((byte) side.ordinal());
                if (isCable(neighbor) && visited.add((BaseMetaPipeEntity) neighbor)) {
                    queue.add((BaseMetaPipeEntity) neighbor);
                }
            }
        }
        return null;
    }

    private static boolean isCable(TileEntity tile) {
        return tile instanceof BaseMetaPipeEntity
            && ((BaseMetaPipeEntity) tile).getMetaTileEntity() instanceof GT_MetaPipeEntity_Cable;
    }

    private static GT_MetaTileEntity_MultiBlockBase getMultiblock(TileEntity tile) {
        if (tile instanceof BaseMetaTileEntity
            && ((BaseMetaTileEntity) tile).getMetaTileEntity() instanceof GT_MetaTileEntity_MultiBlockBase) {
            return (GT_MetaTileEntity_MultiBlockBase) ((BaseMetaTileEntity) tile).getMetaTileEntity();
        }
        return null;
    }
}
