package gregtech.api.gui;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

public class GT_GUIContainer_StorageTank extends GT_GUIContainerMetaTile_Machine {

    private final String mName;

    public GT_GUIContainer_StorageTank(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity, String aName) {
        super(new GT_Container_StorageTank(aInventoryPlayer, aTileEntity), RES_PATH_GUI + "StorageTank.png");
        mName = aName;
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        super.drawScreen(par1, par2, par3);
        drawTooltip(par1, par2);
    }

    private void drawTooltip(int x2, int y2) {
        int xStart = (width - xSize) / 2;
        int yStart = (height - ySize) / 2;
        int x = x2 - xStart;
        int y = y2 - yStart + 5;
        List<String> list = new ArrayList<String>();
        if (y >= 68 && y <= 84) {
            if (x >= 8 && x <= 24) {
                list.add("Fluid Auto-Output");
            }
        }
        if (!list.isEmpty())
            drawHoveringText(list, x2, y2, fontRendererObj);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        fontRendererObj.drawString(mName, 8, 6, 4210752);
        if (mContainer != null) {
            fontRendererObj.drawString("Liquid Amount", 10, 20, 16448255);
            fontRendererObj.drawString(GT_Utility.parseNumberToString(((GT_Container_BasicTank) mContainer).mContent), 10, 30, 16448255);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        super.drawGuiContainerBackgroundLayer(par1, par2, par3);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        if (mContainer != null) {
            if (((GT_Container_StorageTank) mContainer).OutputFluid)
                drawTexturedModalRect(x + 7, y + 63, 176, 18, 18, 18);
        }
    }
}
