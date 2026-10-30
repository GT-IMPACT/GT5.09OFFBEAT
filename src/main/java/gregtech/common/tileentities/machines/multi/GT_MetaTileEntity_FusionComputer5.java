package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.interfaces.ICasing;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION3;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION3_GLOW;

public class GT_MetaTileEntity_FusionComputer5 extends GT_MetaTileEntity_FusionComputer {

    public GT_MetaTileEntity_FusionComputer5(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 6);
    }

    public GT_MetaTileEntity_FusionComputer5(String aName) {
        super(aName);
    }

    @Override
    public int tier() {
        return 10;
    }

    @Override
    public long maxEUStore() {
        return 2100000000L * (Math.min(16, this.mEnergyHatches.size())) / 16L;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_FusionComputer5(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.FUSION_MK_IV;
    }

    @Override
    public ICasing getFusionCoilType() {
        return Casings.SUPERCONDUCTOR_FUSION_COIL_MK_II;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return fusionTooltip("A WHITE DWARF DOWN ON YOUR BASE", "131072EU/t and 132mio EU Cap per Energy Hatch");
    }

    @Override
    public int tierOverclock() {
        return 16;
    }

    @Override
    protected long getEnergyInputPerHatch() {
        return 131072L;
    }

    @Override
    public IIconContainer getIconOverlay() {
        return Textures.BlockIcons.OVERLAY_FUSION3;
    }
    
    @Override
    public ITexture getTextureOverlay() {
        return TextureFactory.of(
                TextureFactory.of(OVERLAY_FUSION3),
                TextureFactory.builder().addIcon(OVERLAY_FUSION3_GLOW).glow().build());
    }
}
