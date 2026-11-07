package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

public class CoverSolarPanel extends CoverLegacyData {

    private final int mVoltage;

    public CoverSolarPanel(CoverContext context, int voltage) {
        super(context);
        this.mVoltage = voltage;
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if (side != 1) {
            coverData = 0;
            return;
        }
        int coverState = coverData & 3;
        int coverNum = coverData >> 2;
        if (timer % 100L == 0L) {
            if (tile.getWorld().isThundering()) {
                coverData = tile.getBiome().rainfall > 0.0F && tile.getSkyAtSide(side) ? Math.min(20, coverNum) << 2
                        : coverNum << 2;
                return;
            } else {
                if (tile.getWorld().isRaining() && tile.getBiome().rainfall > 0.0F) {
                    if (tile.getSkyAtSide(side)) {
                        coverNum = Math.min(30, coverNum);
                    }
                    if (tile.getWorld().skylightSubtracted >= 4) {
                        if (tile.getWorld().isDaytime()) {
                            coverState = 2;
                        } else {
                            coverData = coverNum << 2;
                            return;
                        }
                    }
                } else {
                    if (tile.getWorld().isDaytime()) {
                        coverState = 1;
                    } else {
                        coverState = 2;
                    }
                }
            }
        }
        if (coverState == 1 || (coverState == 2 && timer % 8L == 0L)) {
            tile.injectEnergyUnits((byte) 6, ((100L - (long) coverNum) * ((long) this.mVoltage)) / 100L, 1L);
        }
        if (timer % 28800L == 0L && coverNum < 100 && (coverNum > 10 || XSTR_INSTANCE.nextInt(3) == 2)) {
            coverNum++;
        }
        coverData = coverState + (coverNum << 2);
        return;
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if (player.capabilities.isCreativeMode) {
            GT_Utility.sendChatToPlayer(player, "Cleaned solar panel from " + (coverData >> 2) + "% dirt");
            coverData &= 3;
            markGuiChanged();
            return true;
        }
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack is = player.inventory.mainInventory[i];
            if (is == null) {
                continue;
            }
            if (is.getUnlocalizedName().equals(new ItemStack(Items.water_bucket).getUnlocalizedName())) {
                player.inventory.mainInventory[i] = new ItemStack(Items.bucket);
                if (player.inventoryContainer != null) {
                    player.inventoryContainer.detectAndSendChanges();
                }
                GT_Utility.sendChatToPlayer(player, "Cleaned solar panel from " + (coverData >> 2) + "% dirt");
                coverData &= 3;
                markGuiChanged();
                return true;
            }
        }
        GT_Utility.sendChatToPlayer(player, "You need water bucket in inventory to clean the panel.");
        return false;
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return 1;
    }
}
