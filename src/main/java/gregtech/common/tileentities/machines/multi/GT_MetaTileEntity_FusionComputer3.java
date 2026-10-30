package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;

import static gregtech.api.enums.Textures.BlockIcons.*;

public class GT_MetaTileEntity_FusionComputer3 extends GT_MetaTileEntity_FusionComputer {

    public GT_MetaTileEntity_FusionComputer3(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 6);
    }

    public GT_MetaTileEntity_FusionComputer3(String aName) {
        super(aName);
    }

    @Override
    public int tier() {
        return 8;
    }

    @Override
    public long maxEUStore() {
        return 640010000L * (Math.min(16, this.mEnergyHatches.size())) / 16L;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_FusionComputer3(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.FUSION_MK_II;
    }

    @Override
    public ICasing getFusionCoilType() {
        return Casings.FUSION_COIL;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return fusionTooltip("A SUN DOWN ON EARTH", "8192EU/t and 40mio EU Cap per Energy Hatch");
    }

    @Override
    public int tierOverclock() {
        return 4;
    }

    @Override
    public IIconContainer getIconOverlay() {
        return OVERLAY_FUSION3;
    }
    
    @Override
    public ITexture getTextureOverlay() {
        return TextureFactory.of(
                TextureFactory.of(OVERLAY_FUSION3),
                TextureFactory.builder().addIcon(OVERLAY_FUSION3_GLOW).glow().build());
    }
}

