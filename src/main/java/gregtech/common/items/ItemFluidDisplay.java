package gregtech.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import gregtech.api.items.GT_Generic_Item;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** GTNH item type and tooltip API used by ModularUI2 and NEI. */
public abstract class ItemFluidDisplay extends GT_Generic_Item {

    protected ItemFluidDisplay() {
        super("GregTech_FluidDisplay", "Fluid Display", null);
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
