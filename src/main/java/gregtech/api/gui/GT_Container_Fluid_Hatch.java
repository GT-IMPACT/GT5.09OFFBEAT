package gregtech.api.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

@Deprecated
public class GT_Container_Fluid_Hatch extends GT_ContainerMetaTile_Machine {
	
	public int mContent = 0, mIDFluid = -1;
	
	public GT_Container_Fluid_Hatch(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity) {
		super(aInventoryPlayer, aTileEntity);
	}
	
	@Override
	public void addSlots(InventoryPlayer aInventoryPlayer) {
		addSlotToContainer(new Slot(mTileEntity, 0, 80, 18));
		addSlotToContainer(new GT_Slot_Output(mTileEntity, 1, 80, 43));
		addSlotToContainer(new GT_Slot_Render(mTileEntity, 2, 59, 42));
		addSlotToContainer(new GT_Slot_Render(mTileEntity, 3, 150, 42));
	}
	
	@Override
	public ItemStack slotClick(int aSlotIndex, int aMouseclick, int aShifthold, EntityPlayer aPlayer) {
//		GT_MetaTileEntity_Hatch_Output output = (GT_MetaTileEntity_Hatch_Output) mTileEntity.getMetaTileEntity();
//		if (aSlotIndex == 3) {
//			ItemStack tStackHeld = aPlayer.inventory.getItemStack();
//			if (tStackHeld != null) {
//				FluidStack tFluidHeld = GT_Utility.getFluidForFilledItem(tStackHeld, true);
//				if (tFluidHeld != null && !tFluidHeld.getUnlocalizedName().equals(output.getLockedFluidName())) {
//					output.setLockedFluidName(tFluidHeld.getUnlocalizedName());
//					output.lockedFluid = new FluidStack(tFluidHeld, 0);
//					GT_Utility.sendChatToPlayer(aPlayer, String.format(trans("151.4", "Sucessfully locked Fluid to %s"), tFluidHeld.getLocalizedName()));
//					output.mMode = 9;
//				}
//			} else {
//				output.mMode = 0;
//				output.setLockedFluid(null);
//				GT_Utility.sendChatToPlayer(aPlayer, "Clear locked Fluid");
//			}
//			output.doDisplayThings();
//		}
		return super.slotClick(aSlotIndex, aMouseclick, aShifthold, aPlayer);
	}
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
//		if (mTileEntity.isClientSide() || mTileEntity.getMetaTileEntity() == null) return;
//		GT_MetaTileEntity_Hatch_Output output = (GT_MetaTileEntity_Hatch_Output) mTileEntity.getMetaTileEntity();
//
//
//		if (output.mFluid != null) {
//			mContent = output.mFluid.amount;
//		} else {
//			mContent = 0;
//		}
//
//		if (output.lockedFluid != null) {
//			mIDFluid = output.lockedFluid.getFluidID();
//		} else {
//			mIDFluid = -1;
//		}
//
//		for (Object crafter : this.crafters) {
//			ICrafting var1 = (ICrafting) crafter;
//			var1.sendProgressBarUpdate(this, 100, mContent & 65535);
//			var1.sendProgressBarUpdate(this, 101, mContent >>> 16);
//			var1.sendProgressBarUpdate(this, 102, mIDFluid & 65535);
//			var1.sendProgressBarUpdate(this, 103, mIDFluid >>> 16);
//		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int par1, int par2) {
		super.updateProgressBar(par1, par2);
		switch (par1) {
			case 100:
				mContent = mContent & -65536 | par2;
				break;
			case 101:
				mContent = mContent & 65535 | par2 << 16;
				break;
			case 102:
				mIDFluid = mIDFluid & -65536 | par2;
				break;
			case 103:
				mIDFluid = mIDFluid & 65535 | par2 << 16;
				break;
		}
	}
	
	@Override
	public int getSlotCount() {
		return 2;
	}
	
	@Override
	public int getShiftClickSlotCount() {
		return 1;
	}
}
