package gregtech.api.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.common.blocks.GT_Item_Machines;
import space.impact.api.structure.AutoPlaceEnvironment;

public final class GTStructureUtility {
    public static final String HATCH_CHANNEL = "gt_hatch";

    private GTStructureUtility() {
    }

    public static <T extends GT_MetaTileEntity_MultiBlockBase> HatchElementBuilder<T> buildHatchAdder(Class<T> type) {
        return new HatchElementBuilder<>();
    }

    public static IMetaTileEntity getMetaTileEntity(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof GT_Item_Machines)) return null;
        int id = stack.getItemDamage();
        return id > 0 && id < GregTech_API.METATILEENTITIES.length ? GregTech_API.METATILEENTITIES[id] : null;
    }

    static void orientHatch(IGregTechTileEntity tile, AutoPlaceEnvironment environment) {
        if (environment.getAPILevel() != AutoPlaceEnvironment.APILevel.V2) return;
        ForgeDirection selected = null;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (environment.isContainedInPiece(direction.offsetX, -direction.offsetY, direction.offsetZ)) continue;
            ForgeDirection facing = environment.getFacing().getWorldDirection(
                    direction.offsetY == 0 ? direction : direction.getOpposite());
            if (!tile.isValidFacing((byte) facing.ordinal())) continue;
            selected = facing;
            if (facing.offsetY == 0) break;
        }
        if (selected != null) tile.setFrontFacing((byte) selected.ordinal());
    }
}
