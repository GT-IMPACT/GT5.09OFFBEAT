package gregtech.common.tileentities.machines.basic;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_Container_BasicTank;
import gregtech.api.gui.GT_GUIContainer_BasicTank;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import gregtech.api.objects.GT_RenderedTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class GT_MetaTileEntity_AirGenerator extends GT_MetaTileEntity_BasicTank {



    public GT_MetaTileEntity_AirGenerator(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, 3, "Condense " + 100 * (1 << aTier - 1) * (1 << aTier - 1)  + " L per tick of Air.");
    }

    public GT_MetaTileEntity_AirGenerator(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 3, "Condense " + 100 * (1 << aTier - 1) * (1 << aTier - 1)  + " L per tick of Air.", aTextures);
    }

    public GT_MetaTileEntity_AirGenerator(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 3, "Condense " + 100 * (1 << aTier - 1) * (1 << aTier - 1)  + " L per tick of Air.", aTextures);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_AirGenerator(this.mName, this.mTier, this.mDescriptionArray, this.mTextures);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[mTier][aColorIndex + 1], (aSide == 0 || aSide == 1) ? new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT) : new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_DRAIN)};
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] aTextures) {
        ITexture[][][] rTextures = new ITexture[10][17][];
        for (byte i = -1; i < 16; i++) {
            rTextures[0][i + 1] = getFront(i);
            rTextures[1][i + 1] = getBack(i);
            rTextures[2][i + 1] = getBottom(i);
            rTextures[3][i + 1] = getTop(i);
            rTextures[4][i + 1] = getSides(i);
            rTextures[5][i + 1] = getFrontActive(i);
            rTextures[6][i + 1] = getBackActive(i);
            rTextures[7][i + 1] = getBottomActive(i);
            rTextures[8][i + 1] = getTopActive(i);
            rTextures[9][i + 1] = getSidesActive(i);
        }
        return rTextures;
    }

    public ITexture[] getFront(byte aColor) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[mTier][aColor + 1]};
    }

    public ITexture[] getBack(byte aColor) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[mTier][aColor + 1]};
    }

    public ITexture[] getBottom(byte aColor) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[mTier][aColor + 1]};
    }

    public ITexture[] getTop(byte aColor) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[mTier][aColor + 1]};
    }

    public ITexture[] getSides(byte aColor) {
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[mTier][aColor + 1]};
    }

    public ITexture[] getFrontActive(byte aColor) {
        return getFront(aColor);
    }

    public ITexture[] getBackActive(byte aColor) {
        return getBack(aColor);
    }

    public ITexture[] getBottomActive(byte aColor) {
        return getBottom(aColor);
    }

    public ITexture[] getTopActive(byte aColor) {
        return getTop(aColor);
    }

    public ITexture[] getSidesActive(byte aColor) {
        return getSides(aColor);
    }

    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (this.getBaseMetaTileEntity().isServerSide()) {
            if (this.getBaseMetaTileEntity().isAllowedToWork()) {
                if (this.getFluidAmount() + this.generateAirAmount() <= this.getCapacity() && this.getBaseMetaTileEntity().decreaseStoredEnergyUnits(GT_Values.V[this.mTier], false)) {
                    if (this.mFluid != null && this.mFluid.getFluidID() != Materials.Air.getGas(1L).getFluidID()) {
                        this.mFluid = null;
                    }

                    this.fill(Materials.Air.getGas(this.generateAirAmount()), true);
                }

                this.getBaseMetaTileEntity().setActive(true);
            } else {
                this.getBaseMetaTileEntity().setActive(false);
            }


            IFluidHandler tTileEntity = aBaseMetaTileEntity.getITankContainerAtSide(aBaseMetaTileEntity.getFrontFacing());
            if (tTileEntity != null) {


                FluidTankInfo[] inf = tTileEntity.getTankInfo(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()));
                FluidTankInfo[] var7 = inf;
                int var8 = inf.length;

                for (int var9 = 0; var9 < var8; ++var9) {
                    FluidTankInfo info = var7[var9];
                    if (info != null && (info.fluid == null || info.fluid.getFluidID() < 0 || info.fluid.getFluidID() == Materials.Air.getFluid(1L).getFluidID())) {

                        for (boolean temp = true; temp && mFluid != null; ) {
                            temp = false;
                            FluidStack tDrained = aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()), Math.max(1, mFluid.amount), false);
                            if (tDrained != null) {
                                int tFilledAmount = tTileEntity.fill(ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()), tDrained, false);
                                if (tFilledAmount > 0) {
                                    temp = true;
                                    tTileEntity.fill(ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()), aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()), tFilledAmount, true), true);
                                }
                            }
                        }

//                            if (this.getFluidAmount() >= amount && tTileEntity.fill(ForgeDirection.getOrientation(tSide).getOpposite(), this.drain(amount, false), false) > 0) {
//                                tTileEntity.fill(ForgeDirection.getOrientation(tSide).getOpposite(), this.drain(amount, true), true);
//                            }
//                            break;
                    }
                }
            }
        }

    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_Container_BasicTank(aPlayerInventory, aBaseMetaTileEntity);
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_BasicTank(aPlayerInventory, aBaseMetaTileEntity, getLocalName());
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 1;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 0;
    }

    public boolean isLiquidOutput(byte aSide) {
        return true;
    }

    private int generateAirAmount() {
        return (100 * (1 << this.mTier - 1) * (1 << this.mTier - 1));
    }

    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        } else {
            aBaseMetaTileEntity.openGUI(aPlayer);
            return true;
        }
    }

    public boolean isSimpleMachine() {
        return true;
    }

    public boolean isElectric() {
        return true;
    }

    public boolean isEnetInput() {
        return true;
    }

    public long getMinimumStoredEU() {
        return GT_Values.V[this.mTier] * 16L;
    }

    public long maxEUStore() {
        return GT_Values.V[this.mTier] * 64L;
    }

    public long maxEUInput() {
        return GT_Values.V[this.mTier];
    }

    public long maxSteamStore() {
        return this.maxEUStore();
    }

    public long maxAmperesIn() {
        return 2L;
    }

    public int getStackDisplaySlot() {
        return 2;
    }

    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    public boolean isInputFacing(byte aSide) {
        return true;
    }

    public boolean isOutputFacing(byte aSide) {
        return true;
    }

    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    public int getCapacity() {
        return 100 * (1 << this.mTier - 1) * (1 << this.mTier - 1) * 20;
    }

    public int getTankPressure() {
        return 100;
    }

    public boolean isFluidChangingAllowed() {
        return false;
    }

    public boolean doesFillContainers() {
        return true;
    }

    public boolean doesEmptyContainers() {
        return true;
    }

    public boolean canTankBeFilled() {
        return true;
    }

    public boolean canTankBeEmptied() {
        return true;
    }

    public boolean displaysItemStack() {
        return true;
    }

    public boolean displaysStackSize() {
        return false;
    }

}