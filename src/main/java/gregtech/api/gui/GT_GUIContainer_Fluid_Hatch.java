package gregtech.api.gui;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import gregtech.api.enums.GT_Values;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.net.GT_Packet_OutputHatch;
import gregtech.api.util.GT_Utility;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

public class GT_GUIContainer_Fluid_Hatch extends GT_GUIContainerMetaTile_Machine implements INEIGuiHandler {
	
	private final String mName;
	private Fluid fluid;
	
	public GT_GUIContainer_Fluid_Hatch(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity, String aName) {
		super(new GT_Container_Fluid_Hatch(aInventoryPlayer, aTileEntity), RES_PATH_GUI + "FluidHatch.png");
		mName = aName;
	}
	
	@Override
	public void updateScreen() {
		super.updateScreen();
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
		fontRendererObj.drawString(mName, 8, 6, 4210752);
		if (mContainer != null) {
			GT_Container_Fluid_Hatch hatch = (GT_Container_Fluid_Hatch) mContainer;
			fontRendererObj.drawString("Liquid Amount", 10, 20, 16448255);
			fontRendererObj.drawString(GT_Utility.parseNumberToString(hatch.mContent), 10, 30, 16448255);
			if (hatch.mIDFluid >= 0) {
				fontRendererObj.drawString("Locked Fluid", 101, 20, 16448255);
			}
		}
	}
	
	@Override
	public void drawScreen(int x2, int y2, float par3) {
		super.drawScreen(x2, y2, par3);
		int xx = ((width - xSize) / 2) + 150;
		int yy = ((height - ySize) / 2) + 42;
		List<String> list = new ArrayList<>();
		if (xx < x2 && xx + 16 > x2 && yy < y2 && yy + 16 > y2) {
			list.add("Lock Fluid Mode");
			list.add(EnumChatFormatting.GRAY + "Click with cell filled will block the fluid.");
			list.add(EnumChatFormatting.GRAY + "Click with the same cell or an empty cell");
			list.add(EnumChatFormatting.GRAY + "will remove the fluid lock.");
		}
		GT_Container_Fluid_Hatch hatch = (GT_Container_Fluid_Hatch) mContainer;
		if (!list.isEmpty() && hatch.mIDFluid < 0) {
			drawHoveringText(list, x2, y2, fontRendererObj);
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		super.drawGuiContainerBackgroundLayer(par1, par2, par3);
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
		GT_Container_Fluid_Hatch hatch = (GT_Container_Fluid_Hatch) mContainer;
		if (hatch.mIDFluid >= 0) {
			drawTexturedModalRect(x + 99, y + 17, 8, 17, 69, 43);
		}
	}
	
	@Override
	public VisiblityData modifyVisiblity(GuiContainer guiContainer, VisiblityData visiblityData) {
		return null;
	}
	
	@Override
	public Iterable<Integer> getItemSpawnSlots(GuiContainer guiContainer, ItemStack itemStack) {
		return Collections.emptyList();
	}
	
	@Override
	public List<TaggedInventoryArea> getInventoryAreas(GuiContainer guiContainer) {
		return Collections.emptyList();
	}
	
	@Override
	public boolean handleDragNDrop(GuiContainer guiContainer, int x, int y, ItemStack itemStack, int i2) {
		int xx = ((width - xSize) / 2) + 150;
		int yy = ((height - ySize) / 2) + 42;
		if (xx < x && xx + 16 > x && yy < y && yy + 16 > y) {
			FluidStack fs = GT_Utility.getFluidFromDisplayStack(itemStack);
			if (fs == null) {
				fs = GT_Utility.getFluidForFilledItem(itemStack, true);
			}
			if (fs != null) {
				GT_Values.NW.sendToServer(new GT_Packet_OutputHatch(guiContainer.mc.thePlayer, fs.getFluid()));
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean hideItemPanelSlot(GuiContainer guiContainer, int i, int i1, int i2, int i3) {
		return false;
	}
}