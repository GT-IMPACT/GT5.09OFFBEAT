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

public class GT_MetaTileEntity_FusionComputer2 extends GT_MetaTileEntity_FusionComputer {

    public GT_MetaTileEntity_FusionComputer2(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 6);
    }

    public GT_MetaTileEntity_FusionComputer2(String aName) {
        super(aName);
    }

    @Override
    public int tier() {
        return 7;
    }

    @Override
    public long maxEUStore() {
        return 320006000L * (Math.min(16, this.mEnergyHatches.size())) / 16L;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_FusionComputer2(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.FUSION_MK_I;
    }

    @Override
    public ICasing getFusionCoilType() {
        return Casings.FUSION_COIL;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return fusionTooltip("It's over 9000!!!", "4096EU/t and 20mio EU Cap per Energy Hatch");
    }
    
    

    @Override
    public int tierOverclock() {
        return 2;
    }

    @Override
    public IIconContainer getIconOverlay() {
        return OVERLAY_FUSION2;
    }
    
    @Override
    public ITexture getTextureOverlay() {
        return TextureFactory.of(
                TextureFactory.of(OVERLAY_FUSION2),
                TextureFactory.builder().addIcon(OVERLAY_FUSION2_GLOW).glow().build());
    }
}
