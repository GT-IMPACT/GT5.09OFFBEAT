package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

import gregtech.api.render.TextureFactory;

public class GT_MetaTileEntity_BrickedBlastFurnace extends GT_MetaTileEntity_PrimitiveBlastFurnace{
	private static final ITexture[] FACING_SIDE = {TextureFactory.of(Textures.BlockIcons.MACHINE_CASING_DENSEBRICKS)};
	private static final ITexture[] FACING_FRONT = {TextureFactory.of(Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_INACTIVE)};
	private static final ITexture[] FACING_ACTIVE = {
			TextureFactory.of(Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_ACTIVE),
			TextureFactory.builder().addIcon(Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_ACTIVE_GLOW).glow().build()
	};

    public GT_MetaTileEntity_BrickedBlastFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_BrickedBlastFurnace(String aName) {
        super(aName);
    }
    
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_BrickedBlastFurnace(this.mName);
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return aActive ? FACING_ACTIVE : FACING_FRONT;
        }
        return FACING_SIDE;
    }

    @Override
    public ICasing getCasingType() {
        return Casings.FIREBRICKS;
    }

    @Override
    public byte getTier()  {
        return 1;
    }

	@Override
	public String getName() {
		return "Bricked Blast Furnace";
	}

    @Override
    public int setPollut(){return 200;}
}
