package gregtech.common.tileentities.boilers;

import gregtech.api.enums.Dyes;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Log;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.common.GT_Pollution;
import gregtech.common.gui.GT_Container_Boiler;
import gregtech.common.gui.GT_GUIContainer_Boiler;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import static gregtech.api.enums.Textures.BlockIcons.*;

public class GT_MetaTileEntity_Boiler_Lava
        extends GT_MetaTileEntity_Boiler {
    public GT_MetaTileEntity_Boiler_Lava(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, new String[]{
                "A Boiler running off Lava or Creosote",
                "Produces 600L of Steam per second",
                "Causes 20 Pollution per second"});
    }

    public GT_MetaTileEntity_Boiler_Lava(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Boiler_Lava(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    public ITexture[][][] getTextureSet(ITexture[] aTextures) {
        ITexture[][][] rTextures = new ITexture[5][17][];
        final ITexture[]
                texBottom = {TextureFactory.of(MACHINE_STEELBRICKS_BOTTOM)},
                texTop = {TextureFactory.of(MACHINE_STEELBRICKS_TOP), TextureFactory.of(OVERLAY_PIPE)},
                texSide = {TextureFactory.of(MACHINE_STEELBRICKS_SIDE), TextureFactory.of(OVERLAY_PIPE)},
                texFront = {
                        TextureFactory.of(MACHINE_STEELBRICKS_SIDE),
                        TextureFactory.of(BOILER_LAVA_FRONT)},
                texFrontActive = {
                        TextureFactory.of(MACHINE_STEELBRICKS_SIDE),
                        TextureFactory.of(BOILER_LAVA_FRONT_ACTIVE),
                        TextureFactory.builder().addIcon(BOILER_LAVA_FRONT_ACTIVE_GLOW).glow().build()};
        for (byte i = 0; i < 17; i++) {
            rTextures[0][i] = texBottom;
            rTextures[1][i] = texTop;
            rTextures[2][i] = texSide;
            rTextures[3][i] = texFront;
            rTextures[4][i] = texFrontActive;
        }
        return rTextures;
    }

    public int maxProgresstime() {
        return 1000;
    }

    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_Container_Boiler(aPlayerInventory, aBaseMetaTileEntity, 32000);
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_Boiler(aPlayerInventory, aBaseMetaTileEntity, "SteelBoiler.png", 32000);
    }

    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Boiler_Lava(this.mName, this.mTier, this.mDescriptionArray, this.mTextures);
    }

    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if ((aBaseMetaTileEntity.isServerSide()) && (aTick > 20L)) {
            if (this.mTemperature <= 20) {
                this.mTemperature = 20;
                this.mLossTimer = 0;
            }
            if (++this.mLossTimer > 20) {
                this.mTemperature -= 1;
                this.mLossTimer = 0;
            }
            for (byte i = 1; (this.mSteam != null) && (i < 6); i = (byte) (i + 1)) {
                if (i != aBaseMetaTileEntity.getFrontFacing()) {
                    IFluidHandler tTileEntity = aBaseMetaTileEntity.getITankContainerAtSide(i);
                    if (tTileEntity != null) {
                        FluidStack tDrained = aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(i), Math.max(1, this.mSteam.amount / 2), false);
                        if (tDrained != null) {
                            int tFilledAmount = tTileEntity.fill(ForgeDirection.getOrientation(i).getOpposite(), tDrained, false);
                            if (tFilledAmount > 0) {
                                tTileEntity.fill(ForgeDirection.getOrientation(i).getOpposite(), aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(i), tFilledAmount, true), true);
                            }
                        }
                    }
                }
            }
            if (aTick % 10L == 0L) {
                if (this.mTemperature > 100) {
                    if ((this.mFluid == null) || (!GT_ModHandler.isWater(this.mFluid)) || (this.mFluid.amount <= 0)) {
                        this.mHadNoWater = true;
                    } else {
                        if (this.mHadNoWater) {
                            GT_Log.exp.println("Boiler "+this.mName+" had no Water!");
                            aBaseMetaTileEntity.doExplosion(2048L);
                            return;
                        }
                        this.mFluid.amount -= 1;
                        if (this.mSteam == null) {
                            this.mSteam = GT_ModHandler.getSteam(300L);
                        } else if (GT_ModHandler.isSteam(this.mSteam)) {
                            this.mSteam.amount += 300;
                        } else {
                            this.mSteam = GT_ModHandler.getSteam(300L);
                        }
                    }
                } else {
                    this.mHadNoWater = false;
                }
            }
            if ((this.mSteam != null) &&
                    (this.mSteam.amount > 32000)) {
                sendSound((byte) 1);
                this.mSteam.amount = 24000;
            }
            if ((this.mProcessingEnergy <= 0) && (aBaseMetaTileEntity.isAllowedToWork()) &&
                    (GT_OreDictUnificator.isItemStackInstanceOf(this.mInventory[2], OrePrefixes.bucket.get(Materials.Lava)))) {
                this.mProcessingEnergy += 1000;
                aBaseMetaTileEntity.decrStackSize(2, 1);
                aBaseMetaTileEntity.addStackToSlot(3, GT_OreDictUnificator.get(OrePrefixes.bucket, Materials.Empty, 1L));
            }
            if ((this.mTemperature < 1000) && (this.mProcessingEnergy > 0) && (aTick % 8L == 0L)) {
                this.mProcessingEnergy -= 3;
                this.mTemperature += 1;
            }

            if (this.mProcessingEnergy > 0 && (aTick % 20L == 0L)) {
                GT_Pollution.addPollution(getBaseMetaTileEntity(), 20);
            }
            aBaseMetaTileEntity.setActive(this.mProcessingEnergy > 0);
        }
    }

    public final int fill(FluidStack aFluid, boolean doFill) {
         if (((GT_ModHandler.isLava(aFluid)) || (GT_ModHandler.isCreosote(aFluid))) && (this.mProcessingEnergy < 50)) {
            int tFilledAmount = Math.min(50, aFluid.amount);
            if (doFill) {
                this.mProcessingEnergy += tFilledAmount;
            }
            return tFilledAmount;
        }
        return super.fill(aFluid, doFill);
    }

    public int getCapacity() {
        return 32000;
    }

}
