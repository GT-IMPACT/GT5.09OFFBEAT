package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_Recipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import java.util.ArrayList;

public class GTMTE_Multi_Hatch_Input extends GT_MetaTileEntity_Hatch {

    public final FluidStack[] mFluids;
    public final int mCapacity, mPerFluidAmount;
    public static boolean mIgnoreMap = false;
    public GT_Recipe.GT_Recipe_Map mRecipeMap = null;

    public GTMTE_Multi_Hatch_Input(int aID, String aName, String aNameRegional, int aTier, int aTypeFluids, int aCapacity) {
        super(aID, aName, aNameRegional, aTier, 0, new String[]{
                "Fluid Input for Multiblocks",
                "Types of fluids: " + aTypeFluids,
                "Capacity per fluid: " + aCapacity + "L"
        });

        mPerFluidAmount = aTypeFluids;
        mCapacity = aCapacity;
        mFluids = new FluidStack[mPerFluidAmount];
    }

    public GTMTE_Multi_Hatch_Input(String aName, int aTier, String aDescription, ITexture[][][] aTextures, int aTypeFluids, int aCapacity) {
        super(aName, aTier,0, aDescription, aTextures);
        mPerFluidAmount = aTypeFluids;
        mCapacity = aCapacity;
        mFluids = new FluidStack[mPerFluidAmount];
    }

    public GTMTE_Multi_Hatch_Input(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, int aTypeFluids, int aCapacity) {
        super(aName, aTier, 0, aDescription, aTextures);
        mPerFluidAmount = aTypeFluids;
        mCapacity = aCapacity;
        mFluids = new FluidStack[mPerFluidAmount];
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_IN)};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_IN)};
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GTMTE_Multi_Hatch_Input(mName, mTier, mDescriptionArray, mTextures, mPerFluidAmount, mCapacity);
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return false;
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return false;
    }

    @Override
    public boolean doesFillContainers() {
        return true;
    }

    @Override
    public boolean doesEmptyContainers() {
        return true;
    }

    @Override
    public boolean canTankBeFilled() {
        return true;
    }

    @Override
    public boolean canTankBeEmptied() {
        return true;
    }

    @Override
    public boolean displaysItemStack() {
        return true;
    }

    @Override
    public boolean displaysStackSize() {
        return false;
    }

    public void updateSlots() {
        if (mInventory[getInputSlot()] != null && mInventory[getInputSlot()].stackSize <= 0)
            mInventory[getInputSlot()] = null;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return true;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 1;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        for (int i = 0; i < mPerFluidAmount; i++)
            if (mFluids[i] != null)
                aNBT.setTag("mFluid" + (i == 0 ? "" : i), mFluids[i].writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        for (int i = 0; i < mPerFluidAmount; i++)
            mFluids[i] = FluidStack.loadFluidStackFromNBT(aNBT.getCompoundTag("mFluid" + (i == 0 ? "" : i)));
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public int getCapacity() {
        return mCapacity * mPerFluidAmount;
    }

    @Override
    public FluidTankInfo getInfo() {
        for (FluidStack tFluid : mFluids) {
            if (tFluid != null)
                return new FluidTankInfo(tFluid, mCapacity);
        }
        return new FluidTankInfo(null, mCapacity);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection aSide) {
        if (getCapacity() <= 0 && !getBaseMetaTileEntity().hasSteamEngineUpgrade()) return new FluidTankInfo[]{};
        ArrayList<FluidTankInfo> tList = new ArrayList<>();
        for (FluidStack tFluid : mFluids)
            tList.add(new FluidTankInfo(tFluid, mCapacity));
        return tList.toArray(new FluidTankInfo[mPerFluidAmount]);
    }

    @Override
    public final FluidStack getFluid() {
        for (FluidStack tFluid : mFluids) {
            if (tFluid != null)
                return tFluid;
        }
        return null;
    }

    @Override
    public final int getFluidAmount() {
        int rAmount = 0;
        for (FluidStack tFluid : mFluids) {
            if (tFluid != null)
                rAmount += tFluid.amount;
        }
        return rAmount;
    }

    @Override
    public int getTankPressure() {
        return getFluidAmount() - (getCapacity() / 2);
    }

    @Override
    public int fill(ForgeDirection aSide, FluidStack aFluid, boolean doFill) {
        return fill_default(aSide, aFluid, doFill);
    }

    @Override
    public final int fill_default(ForgeDirection aSide, FluidStack aFluid, boolean doFill) {
        if (aFluid == null || aFluid.getFluid().getID() <= 0) return 0;

        int index = -1;
        for (int i = 0; i < mPerFluidAmount; i++) {
            if (mFluids[i] != null && mFluids[i].isFluidEqual(aFluid)) {
                index = i;
                break;
            } else if ((mFluids[i] == null || mFluids[i].getFluid().getID() <= 0) && index < 0) {
                index = i;
            }
        }

        return fill_default_intoIndex(aSide, aFluid, doFill, index);
    }

    public final int fill_default_intoIndex(ForgeDirection aSide, FluidStack aFluid, boolean doFill, int index) {
        if (index < 0 || index >= mPerFluidAmount) return 0;
        if (aFluid == null || aFluid.getFluid().getID() <= 0) return 0;

        if (mFluids[index] == null || mFluids[index].getFluid().getID() <= 0) {
            if (aFluid.amount * mPerFluidAmount <= getCapacity()) {
                if (doFill) {
                    mFluids[index] = aFluid.copy();
                }
                return aFluid.amount;
            }
            if (doFill) {
                mFluids[index] = aFluid.copy();
                mFluids[index].amount = getCapacity() / mPerFluidAmount;
            }
            return getCapacity() / mPerFluidAmount;
        }

        if (!mFluids[index].isFluidEqual(aFluid)) return 0;

        int space = getCapacity() / mPerFluidAmount - mFluids[index].amount;
        if (aFluid.amount <= space) {
            if (doFill) {
                mFluids[index].amount += aFluid.amount;
            }
            return aFluid.amount;
        }
        if (doFill) {
            mFluids[index].amount = getCapacity() / mPerFluidAmount;
        }
        return space;
    }

    @Override
    public final FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack drained = null;
        for (int i = 0; i < mPerFluidAmount; i++) {
            if ((drained = drainFromIndex(maxDrain, doDrain, i)) != null)
                return drained;
        }
        return null;
    }

    public final FluidStack drainFromIndex(int maxDrain, boolean doDrain, int index) {
        if (index < 0 || index >= mPerFluidAmount) return null;
        if (mFluids[index] == null) return null;
        if (mFluids[index].amount <= 0) {
            mFluids[index] = null;
            return null;
        }

        int used = maxDrain;
        if (mFluids[index].amount < used)
            used = mFluids[index].amount;

        if (doDrain) {
            mFluids[index].amount -= used;
        }

        FluidStack drained = mFluids[index].copy();
        drained.amount = used;

        if (mFluids[index].amount <= 0) {
            mFluids[index] = null;
        }

        return drained;
    }
}
