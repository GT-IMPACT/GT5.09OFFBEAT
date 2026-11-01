package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.logic.TurbineProcessingLogic;
import gregtech.api.interfaces.ICasing;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

import gregtech.api.render.TextureFactory;
import net.minecraft.item.ItemStack;

public class GT_MetaTileEntity_LargeTurbine_Steam extends GT_MetaTileEntity_LargeTurbine {

    public GT_MetaTileEntity_LargeTurbine_Steam(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeTurbine_Steam(String aName) {
        super(aName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, byte side, byte facing, byte color,
                                 boolean active, boolean redstone) {
        ITexture surface = getCasingType().getTexture();
        if (side == facing) {
            surface = TextureFactory.of(active
                    ? Textures.BlockIcons.LARGETURBINE_ST_ACTIVE5
                    : Textures.BlockIcons.LARGETURBINE_ST5);
        }
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[1][color + 1], surface};
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return turbineTooltip("Large Steam Turbine")
                .addInfo("Steam input; distilled water output.")
                .addInfo("Output depends on the rotor and fitting.")
                .addInfo("Use a screwdriver to adjust the turbine fitting.")
                .toolTipFinisher();
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeTurbine_Steam(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.STEEL_TURBINE;
    }

    @Override
    protected boolean requiresOutputHatch() {
        return true;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    @Override
    protected TurbineProcessingLogic.FuelType getFuelType() {
        return TurbineProcessingLogic.FuelType.STEAM;
    }
}
