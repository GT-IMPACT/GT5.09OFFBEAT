package gregtech.api.metatileentity.implementations;

import gregtech.api.gui.GT_Container_StorageTank;
import gregtech.api.gui.GT_GUIContainer_StorageTank;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;

public abstract class GT_MetaTileEntity_StorageTank extends GT_MetaTileEntity_BasicTank {

    public boolean OutputFluid = false;

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
        aNBT.setBoolean("OutputFluid", this.OutputFluid);
        super.saveNBTData(aNBT);
    }

    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        this.OutputFluid = aNBT.getBoolean("OutputFluid");
    }
}