package gregtech.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** GTNH fluid tooltip hook called directly by ModularUI2's fluid widgets. */
public final class ItemFluidDisplay {

    private ItemFluidDisplay() {
    }

    @SideOnly(Side.CLIENT)
    public static void addTooltipForFluid(Fluid fluid, List<String> tooltip) {
        if (fluid == null) {
            return;
        }
        Materials material = Materials.MATERIALS_FLUID.get(fluid);
        String formula = material == null
                ? GT_FluidDisplayItem.getChemicalFormula(new FluidStack(fluid, 1))
                : material.mChemicalFormula;
        if (formula != null && !formula.isEmpty() && !"?".equals(formula)) {
            tooltip.add(EnumChatFormatting.YELLOW + formula + EnumChatFormatting.RESET);
        }
    }
}
