package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.TieredVariant;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuiTheme;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.render.TextureFactory;

import static gregtech.api.enums.Textures.BlockIcons.MACHINE_BRONZEBLASTFURNACE;
import static gregtech.api.enums.Textures.BlockIcons.MACHINE_BRONZEBLASTFURNACE_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.MACHINE_BRONZEBLASTFURNACE_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.MACHINE_BRONZEPLATEDBRICKS;

public class GT_MetaTileEntity_BronzeBlastFurnace
        extends GT_MetaTileEntity_PrimitiveBlastFurnace {
    private static final ITexture[] FACING_SIDE = {TextureFactory.of(MACHINE_BRONZEPLATEDBRICKS)};
    private static final ITexture[] FACING_FRONT = {TextureFactory.of(MACHINE_BRONZEBLASTFURNACE)};
    private static final ITexture[] FACING_ACTIVE = {
            TextureFactory.of(MACHINE_BRONZEBLASTFURNACE_ACTIVE),
            TextureFactory.builder().addIcon(MACHINE_BRONZEBLASTFURNACE_ACTIVE_GLOW).glow().build()
    };

    public GT_MetaTileEntity_BronzeBlastFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_BronzeBlastFurnace(String aName) {
        super(aName);
    }

    @Override
    public GTGuiTheme getGuiTheme() {
        return GTGuiThemes.TIERED_VARIANTS.get(TieredVariant.BRONZE);
    }

	public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
		return new GT_MetaTileEntity_BronzeBlastFurnace(this.mName);
	}

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return aActive ? FACING_ACTIVE : FACING_FRONT;
        }
        return FACING_SIDE;
    }

    @Override
    public ICasing getCasingType() {
        return Casings.BRONZE_PLATED_BRICKS;
    }

    @Override
    public byte getTier()  {
        return 4;
    }

    @Override
    public String getName() {
        return "Bronze Blast Furnace";
    }

    @Override
    public int setPollut(){return 200*4;}
}
