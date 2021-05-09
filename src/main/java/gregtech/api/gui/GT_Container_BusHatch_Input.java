package gregtech.api.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;

public class GT_Container_BusHatch_Input extends GT_ContainerMetaTile_Machine {
	
	public int mFluid = 0;
	
	public GT_Container_BusHatch_Input(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity) {
		super(aInventoryPlayer, aTileEntity);
	}
	
	@Override
	public void addSlots(InventoryPlayer aInventoryPlayer) {
		int count = 0;
		addSlotToContainer(new Slot(mTileEntity, count++, 8, 62));
		
		for (int yCount = 0; yCount < 4; yCount++) for (int xCount = 0; xCount < 4; xCount++) {
			addSlotToContainer(new Slot(mTileEntity, count++, 80 + 18 * xCount, 8 + 18 * yCount));
		}
		
		addSlotToContainer(new GT_Slot_Output(mTileEntity, count++, 44, 62));
		addSlotToContainer(new GT_Slot_Render(mTileEntity, count, 55, 32));
	}
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (mTileEntity.isClientSide() || mTileEntity.getMetaTileEntity() == null) return;
		if (((GT_MetaTileEntity_BasicTank) mTileEntity.getMetaTileEntity()).mFluid != null)
			mFluid = ((GT_MetaTileEntity_BasicTank) mTileEntity.getMetaTileEntity()).mFluid.amount;
		else
			mFluid = 0;
		for (Object crafter : this.crafters) {
			ICrafting var1 = (ICrafting) crafter;
			var1.sendProgressBarUpdate(this, 100, mFluid & 65535);
			var1.sendProgressBarUpdate(this, 101, mFluid >>> 16);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int par1, int par2) {
		super.updateProgressBar(par1, par2);
		switch (par1) {
			case 100:
				mFluid = mFluid & -65536 | par2;
				break;
			case 101:
				mFluid = mFluid & 65535 | par2 << 16;
				break;
		}
	}
	
	@Override
	public int getSlotCount() {
		return 18;
	}
	
	@Override
	public int getShiftClickSlotCount() {
		return 17;
	}
}