package gregtech.api.metatileentity.implementations;

import gregtech.api.gui.GT_Container_StorageTank;
import gregtech.api.gui.GT_GUIContainer_StorageTank;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public abstract class GT_MetaTileEntity_StorageTank extends GT_MetaTileEntity_BasicTank {

    public boolean OutputFluid = false;
    public String lockedFluidName = null;
    public boolean mMode = false;

    public GT_MetaTileEntity_StorageTank(int aID, String aName, String aNameRegional, int aTier, int aInvSlotCount, String aDescription, ITexture... aTextures) {
        super(aID, aName, aNameRegional, aTier, aInvSlotCount, aDescription, aTextures);
    }

    public GT_MetaTileEntity_StorageTank(String aName, int aTier, int aInvSlotCount, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aInvSlotCount, aDescription, aTextures);
    }

    public GT_MetaTileEntity_StorageTank(String aName, int aTier, int aInvSlotCount, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aInvSlotCount, aDescription, aTextures);
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_StorageTank(aPlayerInventory, aBaseMetaTileEntity, getLocalName());
    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_Container_StorageTank(aPlayerInventory, aBaseMetaTileEntity);
    }

    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("OutputFluid", this.OutputFluid);
        aNBT.setBoolean("mMode", mMode);
        if(lockedFluidName!=null && lockedFluidName.length()!=0) aNBT.setString("lockedFluidName", lockedFluidName);
        else aNBT.removeTag("lockedFluidName");

    }

    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        this.OutputFluid = aNBT.getBoolean("OutputFluid");
        mMode = aNBT.getBoolean("mMode");
        lockedFluidName = aNBT.getString("lockedFluidName");
        lockedFluidName = lockedFluidName.length() == 0 ? null : lockedFluidName;
    }

    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return !mMode || getLockedFluidName() == null || getLockedFluidName().equals(aFluid.getUnlocalizedName());
    }

    public String getLockedFluidName() {
        return lockedFluidName;
    }

    public void setLockedFluidName(String lockedFluidName) {
        this.lockedFluidName = lockedFluidName;
    }

    @Override
    public void onEmptyingContainerWhenEmpty() {
        if (this.lockedFluidName == null && this.mFluid != null) {
            this.setLockedFluidName(this.mFluid.getUnlocalizedName());
        }
    }

    @Override
    public boolean isLiquidOutput(byte aSide) {
        return true;
    }

    @Override
    public boolean isLiquidInput(byte aSide) {
        return aSide != getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aIndex == 1 ;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aIndex == 0;
    }
}