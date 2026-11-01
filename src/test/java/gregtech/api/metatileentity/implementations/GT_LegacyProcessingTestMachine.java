package gregtech.api.metatileentity.implementations;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.item.ItemStack;

/**
 * Stable legacy-base fixture, independent of which production machines have
 * been migrated.
 */
public class GT_LegacyProcessingTestMachine extends GT_MetaTileEntity_MultiBlockBase {
    public GT_LegacyProcessingTestMachine(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new GT_LegacyProcessingTestMachine(mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, byte side, byte facing, byte color, boolean active,
            boolean redstone) {
        return new ITexture[0];
    }

    @Override
    public String[] getDescription() {
        return new String[0];
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack stack) {
        return true;
    }

    @Override
    public boolean checkRecipe(ItemStack stack) {
        return false;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity tile, ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxEfficiency(ItemStack stack) {
        return 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack stack) {
        return 0;
    }

    @Override
    public int getDamageToComponent(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack stack) {
        return false;
    }
}
