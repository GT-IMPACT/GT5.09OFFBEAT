package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.SIPrefix;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.GT_FluidDisplayStackRenderer;
import net.minecraftforge.fluids.FluidStack;

/** MUI2 fluid interactions with the existing GregTech fluid display amount rendering. */
public class GTFluidSlot extends FluidSlot {

    public GTFluidSlot() {
        fluidUnit(UNIT_LITER, SIPrefix.One);
    }

    @Override
    protected boolean displayAmountText() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        FluidStack fluid = getFluidStack();
        if (fluid != null && super.displayAmountText()) {
            GT_FluidDisplayStackRenderer.drawFluidAmount(
                fluid.amount,
                getContentPadding().getLeft(),
                getContentPadding().getTop(),
                getArea().height - getContentPadding().vertical()
            );
        }
        super.drawOverlay(context, widgetTheme);
    }
}
