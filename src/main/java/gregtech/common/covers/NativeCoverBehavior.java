package gregtech.common.covers;

import gregtech.api.covers.CoverRegistry;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.GT_CoverBehavior;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

/** Deprecated method-shaped view for addons that still call GT_CoverBehavior. */
@Deprecated
public final class NativeCoverBehavior extends GT_CoverBehavior {

    private final Cover cover;

    public NativeCoverBehavior(Cover cover) {
        this.cover = cover;
    }

    @Override
    public boolean hasCoverGUI() {
        return cover.hasCoverGUI();
    }

    @Override
    public int doCoverThings(
        byte side,
        byte redstone,
        int id,
        int variable,
        ICoverable tile,
        long timer
    ) {
        cover.doCoverThings(redstone, timer);
        return cover.getLegacyData();
    }

    @Override
    public int onCoverScrewdriverclick(
        byte side,
        int id,
        int variable,
        ICoverable tile,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        cover.onCoverScrewdriverClick(player, x, y, z);
        return cover.getLegacyData();
    }

    @Override
    public boolean onCoverShiftRightclick(
        byte side,
        int id,
        int variable,
        ICoverable tile,
        EntityPlayer player
    ) {
        return cover.openGUI(player);
    }

    @Override
    public boolean onCoverRightclick(
        byte side,
        int id,
        int variable,
        ICoverable tile,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        return cover.onCoverRightClick(player, x, y, z);
    }

    @Override
    public boolean onCoverRemoval(
        byte side,
        int id,
        int variable,
        ICoverable tile,
        boolean forced
    ) {
        return cover.canRemove(forced);
    }

    @Override
    public int getTickRate(byte side, int id, int variable, ICoverable tile) {
        return cover.getTickRate();
    }

    @Override
    public boolean isCoverPlaceable(byte side, GT_ItemStack item, ICoverable tile) {
        return CoverRegistry.getCoverPlacer(item.toStack()).isCoverPlaceable(
            net.minecraftforge.common.util.ForgeDirection.getOrientation(side), item.toStack(), tile);
    }

    @Override
    public boolean isSimpleCover() {
        return CoverRegistry.getCoverPlacer(cover.asItemStack()).allowOnPrimitiveBlock();
    }

    @Override
    public ItemStack getDrop(byte side, int id, int variable, ICoverable tile) {
        return cover.asItemStack();
    }

    @Override
    public byte getRedstoneInput(
        byte side,
        byte input,
        int id,
        int variable,
        ICoverable tile
    ) {
        return cover.getRedstoneInput(input);
    }

    @Override
    public boolean letsRedstoneGoIn(byte side, int id, int variable, ICoverable tile) {
        return cover.letsRedstoneGoIn();
    }

    @Override
    public boolean letsRedstoneGoOut(byte side, int id, int variable, ICoverable tile) {
        return cover.letsRedstoneGoOut();
    }

    @Override
    public boolean letsFibreGoIn(byte side, int id, int variable, ICoverable tile) {
        return cover.letsFibreGoIn();
    }

    @Override
    public boolean letsFibreGoOut(byte side, int id, int variable, ICoverable tile) {
        return cover.letsFibreGoOut();
    }

    @Override
    public boolean letsEnergyIn(byte side, int id, int variable, ICoverable tile) {
        return cover.letsEnergyIn();
    }

    @Override
    public boolean letsEnergyOut(byte side, int id, int variable, ICoverable tile) {
        return cover.letsEnergyOut();
    }

    @Override
    public boolean letsFluidIn(
        byte side,
        int id,
        int variable,
        Fluid fluid,
        ICoverable tile
    ) {
        return cover.letsFluidIn(fluid);
    }

    @Override
    public boolean letsFluidOut(
        byte side,
        int id,
        int variable,
        Fluid fluid,
        ICoverable tile
    ) {
        return cover.letsFluidOut(fluid);
    }

    @Override
    public boolean letsItemsIn(
        byte side,
        int id,
        int variable,
        int slot,
        ICoverable tile
    ) {
        return cover.letsItemsIn(slot);
    }

    @Override
    public boolean letsItemsOut(
        byte side,
        int id,
        int variable,
        int slot,
        ICoverable tile
    ) {
        return cover.letsItemsOut(slot);
    }

    @Override
    public boolean isGUIClickable(byte side, int id, int variable, ICoverable tile) {
        return cover.isGUIClickable();
    }

    @Override
    public boolean manipulatesSidedRedstoneOutput(byte side, int id, int variable, ICoverable tile) {
        return cover.manipulatesSidedRedstoneOutput();
    }

    @Override
    public boolean alwaysLookConnected(byte side, int id, int variable, ICoverable tile) {
        return cover.alwaysLookConnected();
    }

    @Override
    public byte getLensColor(byte side, int id, int variable, ICoverable tile) {
        return cover.getLensColor();
    }

    @Override
    public float getBlastProofLevel(byte side, int id, int variable, ICoverable tile) {
        return cover.getBlastProofLevel();
    }

    @Override
    public String getDescription(byte side, int id, int variable, ICoverable tile) {
        return cover.getDescription();
    }

}

