package gregtech.api.enums;

import net.minecraft.block.Block;

import java.util.function.Supplier;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ICasing;

/**
 * Named descriptors for all defined GregTech casing variants and reinforced blocks with casing textures.
 * Reserved metadata without a defined block variant is deliberately omitted.
 * Block references are resolved lazily so this enum is safe to load before block registration.
 *
 * <p>For structures: {@code Casings.FROST_PROOF.asElement()}, or
 * {@code buildHatchAdder(Machine.class).atLeast(...).buildAndChain(Casings.FROST_PROOF)}.
 * Addons should implement {@link ICasing} rather than add entries here.
 */
public enum Casings implements ICasing {
    // sBlockCasings1: texture page 0, starting at 0.
    MACHINE_ULV(BlockSet.CASINGS_1, 0),
    MACHINE_LV(BlockSet.CASINGS_1, 1),
    MACHINE_MV(BlockSet.CASINGS_1, 2),
    MACHINE_HV(BlockSet.CASINGS_1, 3),
    MACHINE_EV(BlockSet.CASINGS_1, 4),
    MACHINE_IV(BlockSet.CASINGS_1, 5),
    MACHINE_LUV(BlockSet.CASINGS_1, 6),
    MACHINE_ZPM(BlockSet.CASINGS_1, 7),
    MACHINE_UV(BlockSet.CASINGS_1, 8),
    MACHINE_UHV(BlockSet.CASINGS_1, 9),
    BRONZE_PLATED_BRICKS(BlockSet.CASINGS_1, 10),
    HEAT_PROOF(BlockSet.CASINGS_1, 11),
    SUPERCONDUCTING_COIL(BlockSet.CASINGS_1, 15),

    // sBlockCasings2: texture page 0, starting at 16.
    SOLID_STEEL(BlockSet.CASINGS_2, 0),
    FROST_PROOF(BlockSet.CASINGS_2, 1),
    BRONZE_GEARBOX(BlockSet.CASINGS_2, 2),
    STEEL_GEARBOX(BlockSet.CASINGS_2, 3),
    TITANIUM_GEARBOX(BlockSet.CASINGS_2, 4),
    /**
     * The historical ItemList name is Casing_Gearbox_TungstenSteel; the actual gearbox is TUNGSTENSTEEL_GEARBOX.
     */
    ASSEMBLY_LINE(BlockSet.CASINGS_2, 5),
    PROCESSOR(BlockSet.CASINGS_2, 6),
    DATA_DRIVE(BlockSet.CASINGS_2, 7),
    CONTAINMENT_FIELD(BlockSet.CASINGS_2, 8),
    ASSEMBLER(BlockSet.CASINGS_2, 9),
    PUMP(BlockSet.CASINGS_2, 10),
    MOTOR(BlockSet.CASINGS_2, 11),
    BRONZE_PIPE(BlockSet.CASINGS_2, 12),
    STEEL_PIPE(BlockSet.CASINGS_2, 13),
    TITANIUM_PIPE(BlockSet.CASINGS_2, 14),
    TUNGSTENSTEEL_PIPE(BlockSet.CASINGS_2, 15),

    // sBlockCasings3: texture page 0, starting at 32.
    YELLOW_STRIPES_A(BlockSet.CASINGS_3, 0),
    YELLOW_STRIPES_B(BlockSet.CASINGS_3, 1),
    RADIOACTIVE_HAZARD_SIGN(BlockSet.CASINGS_3, 2),
    BIO_HAZARD_SIGN(BlockSet.CASINGS_3, 3),
    EXPLOSION_HAZARD_SIGN(BlockSet.CASINGS_3, 4),
    FIRE_HAZARD_SIGN(BlockSet.CASINGS_3, 5),
    ACID_HAZARD_SIGN(BlockSet.CASINGS_3, 6),
    MAGIC_HAZARD_SIGN(BlockSet.CASINGS_3, 7),
    FROST_HAZARD_SIGN(BlockSet.CASINGS_3, 8),
    NOISE_HAZARD_SIGN(BlockSet.CASINGS_3, 9),
    GRATE(BlockSet.CASINGS_3, 10),
    FILTER(BlockSet.CASINGS_3, 11),
    RADIATION_PROOF(BlockSet.CASINGS_3, 12),
    BRONZE_FIREBOX(BlockSet.CASINGS_3, 13),
    STEEL_FIREBOX(BlockSet.CASINGS_3, 14),
    TUNGSTENSTEEL_FIREBOX(BlockSet.CASINGS_3, 15),

    // sBlockCasings4: texture page 0, starting at 48.
    ROBUST_TUNGSTENSTEEL(BlockSet.CASINGS_4, 0),
    CLEAN_STAINLESS_STEEL(BlockSet.CASINGS_4, 1),
    STABLE_TITANIUM(BlockSet.CASINGS_4, 2),
    TITANIUM_FIREBOX(BlockSet.CASINGS_4, 3),
    /**
     * Legacy fusion texture slot; the fusion controller adds a glowing overlay at registration.
     */
    FUSION_GLASS_YELLOW(BlockSet.CASINGS_4, 4),
    /**
     * Legacy fusion glass texture slot.
     */
    FUSION_GLASS(BlockSet.CASINGS_4, 5),
    FUSION_MK_I(BlockSet.CASINGS_4, 6),
    FUSION_COIL(BlockSet.CASINGS_4, 7),
    FUSION_MK_II(BlockSet.CASINGS_4, 8),
    STEEL_TURBINE(BlockSet.CASINGS_4, 9),
    STAINLESS_STEEL_TURBINE(BlockSet.CASINGS_4, 10),
    TITANIUM_TURBINE(BlockSet.CASINGS_4, 11),
    TUNGSTENSTEEL_TURBINE(BlockSet.CASINGS_4, 12),
    ENGINE_INTAKE(BlockSet.CASINGS_4, 13),
    MINING_OSMIRIDIUM(BlockSet.CASINGS_4, 14),
    FIREBRICKS(BlockSet.CASINGS_4, 15),

    // sBlockCasings5: texture page 1, starting at 0.
    CUPRONICKEL_COIL(BlockSet.CASINGS_5, 0),
    KANTHAL_COIL(BlockSet.CASINGS_5, 1),
    NICHROME_COIL(BlockSet.CASINGS_5, 2),
    TUNGSTENSTEEL_COIL(BlockSet.CASINGS_5, 3),
    HSS_G_COIL(BlockSet.CASINGS_5, 4),
    NAQUADAH_COIL(BlockSet.CASINGS_5, 5),
    NAQUADAH_ALLOY_COIL(BlockSet.CASINGS_5, 6),
    ELECTRUM_FLUX_COIL(BlockSet.CASINGS_5, 7),
    DIAMERICIUM_TITANIUM_COIL(BlockSet.CASINGS_5, 8),
    FUSION_MK_III(BlockSet.CASINGS_5, 9),
    FUSION_MK_IV(BlockSet.CASINGS_5, 10),
    SUPERCONDUCTOR_FUSION_COIL_MK_I(BlockSet.CASINGS_5, 11),
    EXTRATERRESTRIAL_HEATING_COIL(BlockSet.CASINGS_5, 12),
    INTERMIX_CHAMBER(BlockSet.CASINGS_5, 13),
    DYSON_RING(BlockSet.CASINGS_5, 14),
    CORE_CHAMBER(BlockSet.CASINGS_5, 15),

    // sBlockCasings6: texture page 1, starting at 96.
    HERMETIC_TIER_0(BlockSet.CASINGS_6, 0),
    HERMETIC_TIER_1(BlockSet.CASINGS_6, 1),
    HERMETIC_TIER_2(BlockSet.CASINGS_6, 2),
    HERMETIC_TIER_3(BlockSet.CASINGS_6, 3),
    HERMETIC_TIER_4(BlockSet.CASINGS_6, 4),
    HERMETIC_TIER_5(BlockSet.CASINGS_6, 5),
    HERMETIC_TIER_6(BlockSet.CASINGS_6, 6),
    HERMETIC_TIER_7(BlockSet.CASINGS_6, 7),
    HERMETIC_TIER_8(BlockSet.CASINGS_6, 8),
    HERMETIC_TIER_9(BlockSet.CASINGS_6, 9),
    HERMETIC_TIER_10(BlockSet.CASINGS_6, 10),
    HERMETIC_TIER_11(BlockSet.CASINGS_6, 11),
    HERMETIC_TIER_12(BlockSet.CASINGS_6, 12),
    HERMETIC_TIER_13(BlockSet.CASINGS_6, 13),
    HERMETIC_TIER_14(BlockSet.CASINGS_6, 14),
    HERMETIC_TIER_15(BlockSet.CASINGS_6, 15),

    // sBlockCasings7: texture page 1, starting at 64.
    ROBUST_NAQUADAH_ALLOY(BlockSet.CASINGS_7, 0),
    TUNGSTENSTEEL_GEARBOX(BlockSet.CASINGS_7, 1),
    ROBUST_NEUTRONIUM(BlockSet.CASINGS_7, 2),
    HIGHSPEED_INTERMIX_CHAMBER(BlockSet.CASINGS_7, 3),
    SUPERCONDUCTOR_DYSON_RING(BlockSet.CASINGS_7, 4),
    SUPERCONDUCTOR_FUSION_COIL_MK_II(BlockSet.CASINGS_7, 5),
    MAGNETIC_COIL(BlockSet.CASINGS_7, 6),
    MACHINE_UEV(BlockSet.CASINGS_7, 10),
    MACHINE_UIV(BlockSet.CASINGS_7, 11),
    MACHINE_UMV(BlockSet.CASINGS_7, 12),
    MACHINE_UXV(BlockSet.CASINGS_7, 13),
    MACHINE_OPV(BlockSet.CASINGS_7, 14),
    MACHINE_MAX(BlockSet.CASINGS_7, 15),

    // sBlockCasings8: texture page 1, starting at 48.
    CHEMICALLY_INERT(BlockSet.CASINGS_8, 0),
    PTFE_PIPE(BlockSet.CASINGS_8, 1),
    /**
     * Legacy casing texture used by the pyrolyse oven (not exposed in the creative inventory).
     */
    PYROLYSE_OVEN(BlockSet.CASINGS_8, 2),
    HSLA(BlockSet.CASINGS_8, 3),
    /**
     * Legacy plascrete texture slot in casings8; the placeable Plascrete Block is PLASCRETE.
     */
    PLASCRETE_LEGACY(BlockSet.CASINGS_8, 4),
    COKE_OVEN_BRICKS(BlockSet.CASINGS_8, 5),

    // sBlockCasings9: texture page 2, starting at 0.
    CLEANROOM_GLASS(BlockSet.CASINGS_9, 0),
    SOLID_FRAME(BlockSet.CASINGS_9, 15),

    // sBlockCasingsNH: texture page 1, starting at 32.
    AIR_FILTER_TURBINE(BlockSet.CASINGS_NH, 0),
    AIR_FILTER_VENT(BlockSet.CASINGS_NH, 1),

    // sBlockReinforced: texture page 0, starting at 80.
    REINFORCED_BRONZEPLATE(BlockSet.REINFORCED, 0),
    REINFORCED_IRIDIUM(BlockSet.REINFORCED, 1),
    PLASCRETE(BlockSet.REINFORCED, 2),
    REINFORCED_TUNGSTENSTEEL(BlockSet.REINFORCED, 3),
    BRITTLE_CHARCOAL(BlockSet.REINFORCED, 4),
    POWDER_BARREL(BlockSet.REINFORCED, 5),
    SOLID_SUPER_FUEL(BlockSet.REINFORCED, 6),
    MAGIC_SOLID_SUPER_FUEL(BlockSet.REINFORCED, 7),
    REINFORCED_STEEL(BlockSet.REINFORCED, 8),
    REINFORCED_TITANIUM(BlockSet.REINFORCED, 9),
    REINFORCED_NAQUADAH(BlockSet.REINFORCED, 10),
    REINFORCED_NEUTRONIUM(BlockSet.REINFORCED, 11);

    private final BlockSet blockSet;
    private final int metadata;

    Casings(BlockSet blockSet, int metadata) {
        this.blockSet = blockSet;
        this.metadata = metadata;
    }

    @Override
    public Block getBlock() {
        return blockSet.block.get();
    }

    @Override
    public int getMetadata() {
        return metadata;
    }

    @Override
    public int getTextureId() {
        return blockSet.textureOffset + metadata;
    }

    /**
     * Mirrors the texture pages registered by GT_Block_Casings* and GT_Block_Reinforced.
     */
    private enum BlockSet {
        CASINGS_1(() -> GregTech_API.sBlockCasings1, 0, 0),
        CASINGS_2(() -> GregTech_API.sBlockCasings2, 0, 16),
        CASINGS_3(() -> GregTech_API.sBlockCasings3, 0, 32),
        CASINGS_4(() -> GregTech_API.sBlockCasings4, 0, 48),
        CASINGS_5(() -> GregTech_API.sBlockCasings5, 1, 0),
        CASINGS_6(() -> GregTech_API.sBlockCasings6, 1, 96),
        CASINGS_7(() -> GregTech_API.sBlockCasings7, 1, 64),
        CASINGS_8(() -> GregTech_API.sBlockCasings8, 1, 48),
        CASINGS_9(() -> GregTech_API.sBlockCasings9, 2, 0),
        CASINGS_NH(() -> GregTech_API.sBlockCasingsNH, 1, 32),
        REINFORCED(() -> GregTech_API.sBlockReinforced, 0, 80);

        private final Supplier<Block> block;
        private final int textureOffset;

        BlockSet(Supplier<Block> block, int page, int firstIndex) {
            this.block = block;
            this.textureOffset = page * 128 + firstIndex;
        }
    }
}
