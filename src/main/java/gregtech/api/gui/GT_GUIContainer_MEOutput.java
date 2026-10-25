package gregtech.api.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

@SideOnly(Side.CLIENT)
public class GT_GUIContainer_MEOutput extends GT_GUIContainerMetaTile_Machine {
    private static final ResourceLocation INTERVAL_TEXTURE = new ResourceLocation(RES_PATH_GUI + "Teleporter.png");
    private static final ResourceLocation CACHE_TEXTURE = new ResourceLocation(RES_PATH_GUI + "StorageTank.png");
    private final String title;

    public GT_GUIContainer_MEOutput(InventoryPlayer player, IGregTechTileEntity tile, String title) {
        super(new GT_Container_MEOutput(player, tile), RES_PATH_GUI + "1by1.png");
        this.title = title;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        if (y < 35 || y >= 51) return;
        List<String> tooltip = new ArrayList<String>();
        if (x >= 8 && x < 24) {
            tooltip.add("Cache Mode: " + (((GT_Container_MEOutput) mContainer).cacheMode ? "On" : "Off"));
            tooltip.add("Store outputs in the inserted cell instead of the ME network");
        } else if ((x >= 110 && x < 126) || (x >= 138 && x < 154)) {
            tooltip.add(x < 126 ? "Decrease refresh interval" : "Increase refresh interval");
            tooltip.add("Click: 1 second; Shift-click: 10 seconds");
            tooltip.add("Minimum: 2 seconds");
        }
        if (!tooltip.isEmpty()) drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GT_Container_MEOutput container = (GT_Container_MEOutput) mContainer;
        fontRendererObj.drawString(title, 8, 4, 4210752);
        fontRendererObj.drawString("Capacity: " + GT_Utility.formatNumbers(container.capacity), 8, 18, 4210752);
        fontRendererObj.drawString("Refresh: " + container.refreshSeconds + " s", 8, 58, 4210752);
        fontRendererObj.drawString("Buffered: " + GT_Utility.formatNumbers(container.buffered), 8, 70, 4210752);
        fontRendererObj.drawString(container.cacheMode ? "Cell" : "ME", 29, 39, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        // The same textured +/- slots used by DigitalTransformer.
        mc.renderEngine.bindTexture(INTERVAL_TEXTURE);
        drawTexturedModalRect(guiLeft + 109, guiTop + 34, 25, 4, 18, 18);
        drawTexturedModalRect(guiLeft + 137, guiTop + 34, 133, 4, 18, 18);

        // StorageTank's output toggle, including its separate enabled-state overlay.
        mc.renderEngine.bindTexture(CACHE_TEXTURE);
        drawTexturedModalRect(guiLeft + 7, guiTop + 34, 7, 63, 18, 18);
        if (((GT_Container_MEOutput) mContainer).cacheMode) {
            drawTexturedModalRect(guiLeft + 7, guiTop + 34, 176, 18, 18, 18);
        }
    }
}
