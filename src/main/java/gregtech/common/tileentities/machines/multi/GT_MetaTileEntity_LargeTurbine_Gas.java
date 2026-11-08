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
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class GT_MetaTileEntity_LargeTurbine_Gas extends GT_MetaTileEntity_LargeTurbine {

    public GT_MetaTileEntity_LargeTurbine_Gas(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_LargeTurbine_Gas(String aName) {
        super(aName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, byte side, byte facing, byte color,
                                 boolean active, boolean redstone) {
        ITexture surface = getCasingType().getTexture();
        if (side == facing) {
            surface = TextureFactory.of(active
                    ? Textures.BlockIcons.LARGETURBINE_SS_ACTIVE5
                    : Textures.BlockIcons.LARGETURBINE_SS5);
        }
        return new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[1][color + 1], surface};
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return turbineTooltip("Large Gas Turbine")
                .addInfo("Generates EU from gas turbine fuels.")
                .addInfo("Supply the optimal fuel flow shown by the controller for maximum efficiency.")
                .addInfo("Pollution: %s per second before muffler reduction.", getPollutionPerTick(null) * 20)
                .toolTipFinisher();
    }

    @Override
    public GT_Recipe_Map getRecipeMapForNEI() {
        return GT_Recipe_Map.sTurbineFuels;
    }

    @Override
    public int getFuelValue(FluidStack fluid) {
        return TurbineProcessingLogic.findFuelValue(GT_Recipe_Map.sTurbineFuels, fluid);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_LargeTurbine_Gas(mName);
    }

    @Override
    public ICasing getCasingType() {
        return Casings.STAINLESS_STEEL_TURBINE;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 15;
    }

    @Override
    protected TurbineProcessingLogic.FuelType getFuelType() {
        return TurbineProcessingLogic.FuelType.GAS;
    }
}
