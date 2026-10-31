package gregtech.api.enums;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ICasing;
import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Heating coil tiers shared by furnace structures, construction and operating parameters. */
public enum HeatingCoil implements ICasing {
    CUPRONICKEL(Casings.CUPRONICKEL_COIL, 1801, 1, 1),
    KANTHAL(Casings.KANTHAL_COIL, 2701, 2, 1),
    NICHROME(Casings.NICHROME_COIL, 3601, 4, 1),
    TUNGSTENSTEEL(Casings.TUNGSTENSTEEL_COIL, 4501, 8, 1),
    HSS_G(Casings.HSS_G_COIL, 5401, 16, 2),
    NAQUADAH(Casings.NAQUADAH_COIL, 7201, 16, 4),
    NAQUADAH_ALLOY(Casings.NAQUADAH_ALLOY_COIL, 9001, 16, 8),
    ELECTRUM_FLUX(Casings.ELECTRUM_FLUX_COIL, 10801, 16, 16),
    DIAMERICIUM_TITANIUM(Casings.DIAMERICIUM_TITANIUM_COIL, 12601, 16, 24),
    EXTRATERRESTRIAL(Casings.EXTRATERRESTRIAL_HEATING_COIL, 19801, 32, 32);

    private final ICasing casing;
    private final int heatCapacity;
    private final int smeltingLevel;
    private final int smeltingDiscount;

    HeatingCoil(ICasing casing, int heatCapacity, int smeltingLevel, int smeltingDiscount) {
        this.casing = casing;
        this.heatCapacity = heatCapacity;
        this.smeltingLevel = smeltingLevel;
        this.smeltingDiscount = smeltingDiscount;
    }

    @Override
    public Block getBlock() {
        return casing.getBlock();
    }

    @Override
    public int getMetadata() {
        return casing.getMetadata();
    }

    @Override
    public int getTextureId() {
        return casing.getTextureId();
    }

    public int getHeatCapacity() {
        return heatCapacity;
    }

    public int getSmeltingLevel() {
        return smeltingLevel;
    }

    public int getSmeltingDiscount() {
        return smeltingDiscount;
    }

    /** One-based projector tier; values outside the supported range select the nearest tier. */
    public static HeatingCoil fromTier(int tier) {
        HeatingCoil[] tiers = values();
        return tiers[Math.max(1, Math.min(tiers.length, tier)) - 1];
    }

    /** Recognize old coil blocks without mutating the world during previews and conflict checks. */
    public static HeatingCoil at(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);
        if (block == GregTech_API.sBlockCasings1 && metadata >= 12 && metadata <= 14) {
            return values()[metadata - 12];
        }
        for (HeatingCoil coil : values()) {
            if (block == coil.getBlock() && metadata == coil.getMetadata()) {
                return coil;
            }
        }
        return null;
    }
}
