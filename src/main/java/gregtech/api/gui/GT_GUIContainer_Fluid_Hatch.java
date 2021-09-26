package gregtech.api.gui;

import gregtech.api.gui.widgets.GT_GuiIcon;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Utility;
import gregtech.common.items.GT_FluidDisplayItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

public class GT_GUIContainer_Fluid_Hatch extends GT_GUIContainerMetaTile_Machine {

    private final String mName;


    public GT_GUIContainer_Fluid_Hatch(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity, String aName) {
        super(new GT_Container_Fluid_Hatch(aInventoryPlayer, aTileEntity), RES_PATH_GUI + "FluidHatch.png");
        mName = aName;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
        fontRendererObj.drawString(mName, 8, 6, 4210752);
        if (mContainer != null) {
            fontRendererObj.drawString("Fluid Amount", 10, 20, 16448255);
            GT_Container_Fluid_Hatch hatch = (GT_Container_Fluid_Hatch) mContainer;
            if (hatch.mIDFluid >= 0)
                fontRendererObj.drawString("Locked Fluid", 101, 20, 16448255);
            fontRendererObj.drawString(GT_Utility.parseNumberToString(((GT_Container_Fluid_Hatch) mContainer).mContent), 10, 30, 16448255);
        }
    }

    @Override
    public void drawScreen(int x2, int y2, float par3) {
        super.drawScreen(x2, y2, par3);
        int xStart = (width - xSize) / 2;
        int yStart = (height - ySize) / 2;
        int x = x2 - xStart;
        int y = y2 - yStart + 5;
        List<String> list = new ArrayList<>();
        if (y >= 41 && y <= 59) {
            if (x >= 149 && x <= 167) {
                list.add("Lock Fluid Mode");
                list.add(EnumChatFormatting.GRAY + "Click with cell filled will block the fluid.");
                list.add(EnumChatFormatting.GRAY + "Click with the same cell or an empty cell");
                list.add(EnumChatFormatting.GRAY + "will remove the fluid lock.");
            }
        }
        GT_Container_Fluid_Hatch hatch = (GT_Container_Fluid_Hatch) mContainer;
        if (!list.isEmpty() && hatch.mIDFluid < 0)
            drawHoveringText(list, x2, y2, fontRendererObj);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        super.drawGuiContainerBackgroundLayer(par1, par2, par3);
        GT_Container_Fluid_Hatch hatch = (GT_Container_Fluid_Hatch) mContainer;
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        if (hatch.mIDFluid >= 0) {
            drawTexturedModalRect(x + 99, y + 17, 8, 17, 69, 43);
        }
    }
}
