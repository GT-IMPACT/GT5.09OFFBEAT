package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidHandler;

public class CoverDrain extends CoverMode {

    public CoverDrain(CoverContext context) {
        super(context, new String[] { "Import", "Import (conditional)", "Import (invert cond)", "Keep Liquids Away",
                "Keep Liquids Away (conditional)", "Keep Liquids Away (invert cond)" });
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if ((coverData % 3 > 1) && ((tile instanceof IMachineProgress))) {
            if (((IMachineProgress) tile).isAllowedToWork() != coverData % 3 < 2) {
                return;
            }
        }
        if (side != 6) {
            Block tBlock = tile.getBlockAtSide(side);
            if ((coverData < 3) && ((tile instanceof IFluidHandler))) {
                if ((side == 1) && (tile.getWorld().isRaining())
                        && (tile.getWorld().getPrecipitationHeight(tile.getXCoord(), tile.getZCoord()) - 2 < tile
                                .getYCoord())) {
                    int tAmount = (int) (tile.getBiome().rainfall * 10.0F);
                    if (tAmount > 0) {
                        ((IFluidHandler) tile).fill(ForgeDirection.getOrientation(side),
                                Materials.Water.getFluid(tile.getWorld().isThundering() ? tAmount * 2 : tAmount), true);
                    }
                }
                FluidStack tLiquid = null;
                if (tBlock != null) {
                    if (((tBlock == Blocks.water) || (tBlock == Blocks.flowing_water))
                            && (tile.getMetaIDAtSide(side) == 0)) {
                        tLiquid = Materials.Water.getFluid(1000L);
                    } else if (((tBlock == Blocks.lava) || (tBlock == Blocks.flowing_lava))
                            && (tile.getMetaIDAtSide(side) == 0)) {
                        tLiquid = Materials.Lava.getFluid(1000L);
                    } else if ((tBlock instanceof IFluidBlock)) {
                        tLiquid = ((IFluidBlock) tBlock).drain(tile.getWorld(), tile.getOffsetX(side, 1),
                                tile.getOffsetY(side, 1), tile.getOffsetZ(side, 1), false);
                    }
                    if ((tLiquid != null) && (tLiquid.getFluid() != null)
                            && ((side > 1) || ((side == 0) && (tLiquid.getFluid().getDensity() <= 0))
                                    || ((side == 1) && (tLiquid.getFluid().getDensity() >= 0)))
                            && (((IFluidHandler) tile).fill(ForgeDirection.getOrientation(side), tLiquid,
                                    false) == tLiquid.amount)) {
                        ((IFluidHandler) tile).fill(ForgeDirection.getOrientation(side), tLiquid, true);
                        tile.getWorld().setBlockToAir(tile.getXCoord() + ForgeDirection.getOrientation(side).offsetX,
                                tile.getYCoord() + ForgeDirection.getOrientation(side).offsetY,
                                tile.getZCoord() + ForgeDirection.getOrientation(side).offsetZ);
                    }
                }
            }
            if ((coverData >= 3) && (tBlock != null)
                    && ((tBlock == Blocks.lava) || (tBlock == Blocks.flowing_lava) || (tBlock == Blocks.water)
                            || (tBlock == Blocks.flowing_water) || ((tBlock instanceof IFluidBlock)))) {
                tile.getWorld().setBlock(
                    tile.getOffsetX(side, 1),
                    tile.getOffsetY(side, 1),
                    tile.getOffsetZ(side, 1),
                    Blocks.air,
                    0,
                    0
                );
            }
        }
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        coverData = (coverData + (player.isSneaking() ? -1 : 1)) % 6;
        if (coverData < 0) {
            coverData = 5;
        }
        switch (coverData) {
        case 0:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_022", "Import"));
            break;

        case 1:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_023", "Import (conditional)"));
            break;

        case 2:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_024", "Import (invert cond)"));
            break;

        case 3:
            GT_Utility.sendChatToPlayer(player,
                    GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_025", "Keep Liquids Away"));
            break;

        case 4:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_026", "Keep Liquids Away (conditional)"));
            break;

        case 5:
            GT_Utility.sendChatToPlayer(player, GT_LanguageManager
                    .addStringLocalization("Interaction_DESCRIPTION_Index_027", "Keep Liquids Away (invert cond)"));
            break;

        }
        markGuiChanged();
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        ICoverable tile = getTile();
        return !(tile instanceof IMachineProgress) || ((IMachineProgress) tile).isAllowedToWork() == coverData < 2;
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return coverData < 3 ? 50 : 1;
    }
}
