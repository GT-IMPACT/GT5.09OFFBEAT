package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;

public class CoverScreen extends CoverLegacyData {

    public CoverScreen(CoverContext context) {
        super(context);
    }

    @Override
    public float getBlastProofLevel() {
        return 20.0F;
    }

    @Override
    public boolean letsRedstoneGoIn() {
        return false;
    }

    @Override
    public boolean letsRedstoneGoOut() {
        return false;
    }

    @Override
    public boolean letsEnergyIn() {
        return false;
    }

    @Override
    public boolean letsEnergyOut() {
        return false;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        return false;
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        return false;
    }

    @Override
    public boolean letsItemsIn(int slot) {
        return false;
    }

    @Override
    public boolean letsItemsOut(int slot) {
        return false;
    }

    @Override
    public boolean manipulatesSidedRedstoneOutput() {
        return false;
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        return false;
    }

    @Override
    public boolean canRemove(boolean forced) {
        return true;
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        coverData = 0;
        return;
    }
}
