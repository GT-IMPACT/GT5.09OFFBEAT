package gregtech.api.gui;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;

import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

public class GT_GUIContainer_BusHatch_Input extends GT_GUIContainerMetaTile_Machine {
	
	private final String mName;
	
	public GT_GUIContainer_BusHatch_Input(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity, String aName) {
		super(new GT_Container_BusHatch_Input(aInventoryPlayer, aTileEntity), RES_PATH_GUI + "BusHatch.png");
		mName = aName;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		if (mContainer != null) {
			fontRendererObj.drawString("Fluid Amount", 10, 10, 16448255);
			fontRendererObj.drawString(GT_Utility.parseNumberToString(((GT_Container_BusHatch_Input) mContainer).mFluid), 10, 20, 16448255);
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		super.drawGuiContainerBackgroundLayer(par1, par2, par3);
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}
}
