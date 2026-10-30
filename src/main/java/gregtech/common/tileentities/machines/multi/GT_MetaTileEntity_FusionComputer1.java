package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION1;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION1_GLOW;

public class GT_MetaTileEntity_FusionComputer1 extends GT_MetaTileEntity_FusionComputer {

    public GT_MetaTileEntity_FusionComputer1(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 6);
    }

    public GT_MetaTileEntity_FusionComputer1(String aName) {
        super(aName);
    }

    @Override
    public int tier() {
        return 6;
    }

    @Override
    public long maxEUStore() {
        return 160003000L * (Math.min(16, this.mEnergyHatches.size())) / 16L;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_FusionComputer1(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.MACHINE_LUV;
    }

    @Override
    public ICasing getFusionCoilType() {
        return Casings.SUPERCONDUCTING_COIL;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return fusionTooltip("It's over 9000!!!", "2048EU/t and 10mio EU Cap per Energy Hatch");
    }
    
    @Override
    public ITexture getTextureOverlay() {
        return TextureFactory.of(
                TextureFactory.of(OVERLAY_FUSION1),
                TextureFactory.builder().addIcon(OVERLAY_FUSION1_GLOW).glow().build());
    }
    
    @Override
    public int tierOverclock() {
        return 1;
    }

    @Override
    public IIconContainer getIconOverlay() {
        return OVERLAY_FUSION1;
    }

}
