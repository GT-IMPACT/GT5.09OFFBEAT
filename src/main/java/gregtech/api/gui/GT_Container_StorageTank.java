package gregtech.api.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_StorageTank;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.Iterator;

public class GT_Container_StorageTank extends GT_Container_BasicTank {

    public int mContent = 0;
    public boolean OutputFluid = false, mMode = false, mVoidFluidPart = false, mVoidFluidFull = false;

    public GT_Container_StorageTank(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity) {
        super(aInventoryPlayer, aTileEntity);
    }

    @Override
    public void addSlots(InventoryPlayer aInventoryPlayer) {
        addSlotToContainer(new Slot(mTileEntity, 0, 81, 17));
        addSlotToContainer(new GT_Slot_Output(mTileEntity, 1, 81, 44));
        addSlotToContainer(new GT_Slot_Render(mTileEntity, 2, 60, 42));
        addSlotToContainer(new GT_Slot_Holo(mTileEntity, 3, 8, 64, false, true, 1));
        addSlotToContainer(new GT_Slot_Holo(mTileEntity, 4, 26, 64, false, true, 1));
        addSlotToContainer(new GT_Slot_Holo(mTileEntity, 5, 44, 64, false, true, 1));
        addSlotToContainer(new GT_Slot_Holo(mTileEntity, 6, 62, 64, false, true, 1));
    }

    @Override
    public ItemStack slotClick(int aSlotIndex, int aMouseclick, int aShifthold, EntityPlayer aPlayer) {

        if (mTileEntity.getMetaTileEntity() != null) {

            GT_MetaTileEntity_StorageTank mte = ((GT_MetaTileEntity_StorageTank) mTileEntity.getMetaTileEntity());

            if (mte == null) {
                return null;
            }

            if (aSlotIndex == 3) {
                mte.OutputFluid = !mte.OutputFluid;
                if (!mte.OutputFluid) {
                    GT_Utility.sendChatToPlayer(aPlayer, "Fluid Output Disabled");
                } else {
                    GT_Utility.sendChatToPlayer(aPlayer, "Fluid Output Enabled");
                }
                return null;
            }
            if (aSlotIndex == 4) {
                String inBrackets;
                mte.mMode = !mte.mMode;
                if (mte.mMode) {
                    if (mte.mFluid == null) {
                        mte.setLockedFluidName(null);
                        inBrackets = "currently none, will be locked to the next that is put in";
                    } else {
                        mte.setLockedFluidName(
                                mte.getDrainableStack().getUnlocalizedName());
                        inBrackets = mte.getDrainableStack().getLocalizedName();
                    }
                    GT_Utility.sendChatToPlayer(aPlayer, String.format("%s (%s)", "1 specific Fluid", inBrackets));
                } else {
                    GT_Utility.sendChatToPlayer(aPlayer, "Lock Fluid Mode Disabled");
                }
                return null;
            }
            if (aSlotIndex == 5) {
                mte.mVoidFluidPart = !mte.mVoidFluidPart;
                if (!mte.mVoidFluidPart) {
                    GT_Utility.sendChatToPlayer(aPlayer, "Void Part Mode Disabled");
                } else {
                    GT_Utility.sendChatToPlayer(aPlayer, "Void Part Mode Enabled");
                }
                return null;
            }
            if (aSlotIndex == 6) {
                mte.mVoidFluidFull = !mte.mVoidFluidFull;
                if (!mte.mVoidFluidFull) {
                    GT_Utility.sendChatToPlayer(aPlayer, "Void Full Mode Disabled");
                } else {
                    GT_Utility.sendChatToPlayer(aPlayer, "Void Full Mode Enabled");
                }
                return null;
            }
        }
        return super.slotClick(aSlotIndex, aMouseclick, aShifthold, aPlayer);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (mTileEntity.isClientSide() || mTileEntity.getMetaTileEntity() == null) {
            return;
        }

        GT_MetaTileEntity_StorageTank mte = ((GT_MetaTileEntity_StorageTank) mTileEntity.getMetaTileEntity());

        if (mte.mFluid != null) {
            mContent = mte.mFluid.amount;
        } else {
            mContent = 0;
        }
        OutputFluid = mte.OutputFluid;
        mMode = mte.mMode;
        mVoidFluidPart = mte.mVoidFluidPart;
        mVoidFluidFull = mte.mVoidFluidFull;

        for (Object crafter : this.crafters) {
            ICrafting var1 = (ICrafting) crafter;
            var1.sendProgressBarUpdate(this, 100, mContent & 65535);
            var1.sendProgressBarUpdate(this, 101, mContent >>> 16);
            var1.sendProgressBarUpdate(this, 103, OutputFluid ? 1 : 0);
            var1.sendProgressBarUpdate(this, 104, mMode ? 1 : 0);
            var1.sendProgressBarUpdate(this, 105, mVoidFluidPart ? 1 : 0);
            var1.sendProgressBarUpdate(this, 106, mVoidFluidFull ? 1 : 0);
        }
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
            case 103:
                OutputFluid = (par2 != 0);
                break;
            case 104:
                mMode = (par2 != 0);
                break;
            case 105:
                mVoidFluidPart = (par2 != 0);
                break;
            case 106:
                mVoidFluidFull = (par2 != 0);
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
