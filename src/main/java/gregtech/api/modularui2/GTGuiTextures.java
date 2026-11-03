package gregtech.api.modularui2;

import static com.cleanroommc.modularui.drawable.UITexture.fullImage;

import java.util.stream.IntStream;

import com.cleanroommc.modularui.drawable.ColorType;
import com.cleanroommc.modularui.drawable.UITexture;
import gregtech.common.modularui2.util.SteamTexture;

/** Core GTNH UI textures. MUI2 resources are isolated from existing machine GUIs. */
public final class GTGuiTextures {

    private GTGuiTextures() {
    }

    public static void init() {
    }

    public static final UITexture TRANSPARENT = UITexture.fullImage("gregtech", "gui/mui2/picture/transparent");

    public static final UITexture BACKGROUND_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/background/singleblock_default")
        .imageSize(176, 166)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_STANDARD)
        .build();

    public static final SteamTexture BACKGROUND_STEAM = SteamTexture.builder()
        .location("gui/mui2/background/%s")
        .imageSize(176, 166)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_STEAM)
        .build();

    public static final UITexture BACKGROUND_FOUNDRY = UITexture.builder()
        .location("gregtech", "gui/mui2/background/foundry_default")
        .imageSize(176, 166)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_FOUNDRY)
        .build();

    public static final UITexture BACKGROUND_NANOCHIP = UITexture.builder()
        .location("gregtech", "gui/mui2/background/nanochip_default")
        .imageSize(198, 203)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_NANOCHIP)
        .build();

    public static final UITexture BACKGROUND_NANOCHIP_LARGE = UITexture.builder()
        .location("gregtech", "gui/mui2/background/nanochip_default_large")
        .imageSize(600, 306)
        .adaptable(4)
        .canApplyTheme()
        .build();

    public static final UITexture BACKGROUND_TITLE_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/tab/title_dark")
        .imageSize(28, 28)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_TITLE_STANDARD)
        .build();

    public static final SteamTexture BACKGROUND_TITLE_STEAM = SteamTexture.builder()
        .location("gui/mui2/tab/title_dark_%s")
        .imageSize(28, 28)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_TITLE_STEAM)
        .build();

    public static final UITexture BACKGROUND_TITLE_FOUNDRY = UITexture.builder()
        .location("gregtech", "gui/mui2/tab/title_foundry")
        .imageSize(28, 28)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_TITLE_FOUNDRY)
        .build();

    public static final UITexture BACKGROUND_TITLE_NANOCHIP = UITexture.builder()
        .location("gregtech", "gui/mui2/tab/title_nanochip")
        .imageSize(28, 28)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_TITLE_NANOCHIP)
        .build();

    public static final UITexture BACKGROUND_POPUP_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/background/popup")
        .imageSize(195, 136)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_POPUP_STANDARD)
        .build();

    public static final SteamTexture BACKGROUND_POPUP_STEAM = SteamTexture.builder()
        .location("gui/mui2/background/popup_%s")
        .imageSize(195, 136)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_POPUP_STEAM)
        .build();

    public static final UITexture BACKGROUND_TERMINAL = UITexture.builder()
        .location("gregtech", "gui/mui2/background/terminal")
        .imageSize(142, 28)
        .adaptable(4)
        .build();

    public static final UITexture BACKGROUND_TEXT_FIELD = UITexture.builder()
        .location("gregtech", "gui/mui2/background/text_field")
        .imageSize(142, 28)
        .adaptable(1)
        .build();

    public static final UITexture FOUNDRY_BACKGROUND_TEXT_FIELD = UITexture.builder()
        .location("gregtech", "gui/mui2/background/popup_foundry")
        .imageSize(195, 136)
        .adaptable(4)
        .name(GTTextureIds.BACKGROUND_POPUP_FOUNDRY)
        .build();

    public static final UITexture NANOCHIP_BACKGROUND_TEXT_FIELD = UITexture.builder()
        .location("gregtech", "gui/mui2/background/popup_nanochip")
        .imageSize(193, 134)
        .adaptable(4)
        .name(GTTextureIds.BACKGROUND_POPUP_NANOCHIP)
        .build();

    public static final UITexture BACKGROUND_NANOCHIP_RULE_POPUP = UITexture.builder()
        .location("gregtech", "gui/mui2/background/nanochip_rule")
        .imageSize(188, 70)
        .adaptable(4)
        .canApplyTheme()
        .build();

    public static final UITexture BACKGROUND_NANOCHIP_NAMEPLATE = UITexture.builder()
        .location("gregtech", "gui/mui2/background/nanochip_nameplate")
        .imageSize(144, 24)
        .build();

    public static final UITexture FOUNDRY_BACKGROUND_CONTRIBUTORS = UITexture.builder()
        .location("gregtech", "gui/mui2/background/contributors_foundry")
        .imageSize(298, 298)
        .adaptable(1)
        .build();

    public static final UITexture BACKGROUND_REDSTONE_SNIFFER = UITexture.builder()
        .location("gregtech", "gui/mui2/background/redstone_sniffer")
        .imageSize(195, 136)
        .adaptable(1)
        .build();

    public static final UITexture BACKGROUND_CHAOS_LOCATOR = UITexture.builder()
        .location("gregtech", "gui/mui2/background/chaos_locator")
        .imageSize(176, 166)
        .adaptable(1)
        .build();

    public static final UITexture BACKGROUND_DRONE_CAMERA = UITexture.builder()
        .location("gregtech", "gui/mui2/background/drone_camera")
        .imageSize(256, 256)
        .adaptable(100, 16, 100, 26)
        .nonOpaque()
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_DRONE_CAMERA)
        .build();

    public static final UITexture BACKGROUND_GRAY_BORDER = UITexture.builder()
        .adaptable(1)
        .canApplyTheme()
        .location("gregtech", "gui/mui2/picture/gray_rectangle")
        .build();

    public static final UITexture OVERLAY_GREGTECH_LOGO = fullImageGT("picture/gt_logo_standard");

    public static final UITexture OVERLAY_NEEDS_CROWBAR = UITexture.fullImage("gregtech", "gui/mui2/icons/needsCrowbar");

    public static final UITexture OVERLAY_NEEDS_HARDHAMMER = UITexture
        .fullImage("gregtech", "gui/mui2/icons/needsHardhammer");

    public static final UITexture OVERLAY_NEEDS_SCREWDRIVER = UITexture
        .fullImage("gregtech", "gui/mui2/icons/needsScrewdriver");

    public static final UITexture OVERLAY_NEEDS_SOFTHAMMER = UITexture
        .fullImage("gregtech", "gui/mui2/icons/needsSofthammer");

    public static final UITexture OVERLAY_NEEDS_SOLDERING = UITexture
        .fullImage("gregtech", "gui/mui2/icons/needsSoldering");

    public static final UITexture OVERLAY_NEEDS_WRENCH = UITexture.fullImage("gregtech", "gui/mui2/icons/needsWrench");

    public static final UITexture OVERLAY_TOO_DAMAGED = UITexture.fullImage("gregtech", "gui/mui2/icons/needsWrench");

    public static final UITexture OVERLAY_POWER_LOSS = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/stalled_electricity")
        .canApplyTheme()
        .fullImage()
        .name(GTTextureIds.PICTURE_ERROR)
        .build();

    public static final UITexture OVERLAY_POWER_LOSS_STEAM = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/stalled_steam")
        .canApplyTheme()
        .fullImage()
        .name(GTTextureIds.PICTURE_ERROR_STEAM)
        .build();

    public static final UITexture OVERLAY_STRUCTURE_INCOMPLETE = UITexture
        .fullImage("gregtech", "gui/mui2/icons/structureIncomplete");

    public static final UITexture OVERLAY_MANUAL_SHUTDOWN = UITexture
        .fullImage("gregtech", "gui/mui2/icons/manualShutdown");

    public static final UITexture OVERLAY_COMPUTATION_LOSS = UITexture
        .fullImage("gregtech", "gui/mui2/icons/stalled_computation");

    public static final UITexture OVERLAY_UNPOWERED = UITexture.fullImage("gregtech", "gui/mui2/icons/unpowered");

    public static final UITexture OVERLAY_ROTOR = UITexture.fullImage("gregtech", "gui/mui2/picture/noRotor");

    public static final UITexture OVERLAY_WIND = UITexture.fullImage("gregtech", "gui/mui2/picture/wind");

    public static final UITexture OVERLAY_NO_MAINTENANCE_ISSUES = UITexture
        .fullImage("gregtech", "gui/mui2/icons/maintenance_none");

    public static final UITexture OVERLAY_SOME_MAINTENANCE_ISSUES = UITexture
        .fullImage("gregtech", "gui/mui2/icons/maintenance_some");

    public static final UITexture OVERLAY_ALL_MAINTENANCE_ISSUES = UITexture
        .fullImage("gregtech", "gui/mui2/icons/maintenance_all");

    public static final UITexture SLOT_ITEM_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_standard")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.SLOT_ITEM_STANDARD)
        .build();

    public static final UITexture SLOT_ITEM_TANK = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_tank")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final SteamTexture SLOT_ITEM_STEAM = SteamTexture.builder()
        .location("gui/mui2/slot/item_%s")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.SLOT_ITEM_STEAM)
        .build();

    public static final UITexture SLOT_ITEM_DARK = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/dark_gray")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_MAINTENANCE = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/maintenance")
        .imageSize(20, 20)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_ITEM_FOUNDRY = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_foundry")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.SLOT_ITEM_FOUNDRY)
        .build();

    public static final UITexture SLOT_ITEM_GOLD = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_gold")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_ITEM_GREEN = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_green")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_ITEM_PURPLE = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_purple")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_ITEM_PRISMATIC = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_prismatic")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_ITEM_NANOCHIP = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/item_nanochip")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.SLOT_ITEM_NANOCHIP)
        .build();

    public static final UITexture SLOT_FLUID_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/fluid_standard")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.SLOT_FLUID_STANDARD)
        .build();

    public static final UITexture SLOT_FLUID_TANK = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/fluid_tank")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final SteamTexture SLOT_FLUID_STEAM = SteamTexture.builder()
        .location("gui/mui2/slot/fluid_%s")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.SLOT_FLUID_STEAM)
        .build();

    public static final UITexture SLOT_FLUID_DARK = UITexture.builder()
        .location("gregtech", "gui/mui2/slot/fluid_dark_gray")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture SLOT_EMPTY = TRANSPARENT;

    public static final UITexture OVERLAY_SLOT_CANISTER = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/canister")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_CANISTER_DARK = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/canister_dark")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_CANISTER_DARK)
        .build();

    public static final SteamTexture OVERLAY_SLOT_CANISTER_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/canister_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_CANISTER_STEAM)
        .build();

    public static final UITexture OVERLAY_SLOT_ARMOR = fullImageColorableGT("overlay_slot/armor");

    public static final UITexture OVERLAY_SLOT_ARMOR_CORE = fullImageColorableGT("overlay_slot/armor_core");

    public static final UITexture OVERLAY_SLOT_ARMOR_FRAME = fullImageColorableGT("overlay_slot/armor_frame");

    public static final UITexture OVERLAY_SLOT_DATA_ORB = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/data_orb")
        .canApplyTheme()
        .fullImage()
        .build();

    public static final UITexture OVERLAY_SLOT_DUST_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/dust")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_DUST_STANDARD)
        .build();

    public static final SteamTexture OVERLAY_SLOT_DUST_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/dust_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_DUST_STEAM)
        .build();

    public static final UITexture OVERLAY_SLOT_INGOT_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/ingot")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_INGOT_STANDARD)
        .build();

    public static final SteamTexture OVERLAY_SLOT_INGOT_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/ingot_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_INGOT_STEAM)
        .build();

    public static final UITexture OVERLAY_SLOT_INT_CIRCUIT = fullImageColorableGT("overlay_slot/int_circuit");

    public static final UITexture OVERLAY_SLOT_FURNACE_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/furnace")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_FURNACE_STANDARD)
        .build();

    public static final SteamTexture OVERLAY_SLOT_FURNACE_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/furnace_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_FURNACE_STEAM)
        .build();

    public static final UITexture OVERLAY_SLOT_IN_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/in")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_IN_STANDARD)
        .build();

    public static final SteamTexture OVERLAY_SLOT_IN_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/in_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_IN_BRONZE, GTTextureIds.OVERLAY_SLOT_IN_STEEL, null)
        .build();

    public static final UITexture OVERLAY_SLOT_OUT_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/out")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_OUT_STANDARD)
        .build();

    public static final SteamTexture OVERLAY_SLOT_OUT_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/out_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_OUT_BRONZE, GTTextureIds.OVERLAY_SLOT_OUT_STEEL, null)
        .build();

    public static final UITexture OVERLAY_SLOT_COAL_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/coal")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_COAL_STANDARD)
        .build();

    public static final SteamTexture OVERLAY_SLOT_COAL_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/coal_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_COAL_BRONZE, GTTextureIds.OVERLAY_SLOT_COAL_STEEL, null)
        .build();

    public static final UITexture OVERLAY_SLOT_BLOCK_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/block")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_BLOCK_STANDARD)
        .build();

    public static final UITexture OVERLAY_SLOT_EXTRUDER_SHAPE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/extruder_shape")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final SteamTexture OVERLAY_SLOT_BLOCK_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/block_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.OVERLAY_SLOT_BLOCK_STEAM)
        .build();

    public static final UITexture OVERLAY_SLOT_FILTER = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/filter")
        .fullImage()
        .canApplyTheme()
        .name("overlay_slot/filter")
        .build();

    public static final UITexture OVERLAY_SLOT_ARROW_ME = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/arrow_me")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_DRAWER = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/drawer")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_DRAWER_SIDE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/drawer_side")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_DRAWER_FRONT = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/drawer_front")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_DRAWER_TRIM = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/drawer_trim")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_MINING_PIPE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/mining_pipe")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_VOID_EXCESS_NONE = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/void_excess_none")
        .build();

    public static final UITexture OVERLAY_BUTTON_VOID_EXCESS_ITEM = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/void_excess_item")
        .build();

    public static final UITexture OVERLAY_BUTTON_VOID_EXCESS_FLUID = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/void_excess_fluid")
        .build();

    public static final UITexture OVERLAY_BUTTON_VOID_EXCESS_ALL = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/void_excess_all")
        .build();

    public static final UITexture OVERLAY_BUTTON_AUTOOUTPUT_FLUID = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/autooutput_fluid")
        .build();

    public static final UITexture OVERLAY_BUTTON_LOCK = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/lock")
        .build();

    public static final UITexture OVERLAY_BUTTON_INPUT_FROM_OUTPUT_SIDE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/input_from_output_side")
        .build();

    public static final UITexture OVERLAY_BUTTON_TANK_VOID_EXCESS = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/tank_void_excess")
        .build();

    public static final UITexture OVERLAY_BUTTON_TANK_VOID_ALL = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/tank_void_all")
        .build();

    public static final UITexture OVERLAY_BUTTON_INPUT_SEPARATION_ON = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/input_separation_on")
        .build();

    public static final UITexture OVERLAY_BUTTON_INPUT_SEPARATION_OFF = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/input_separation_off")
        .build();

    public static final UITexture OVERLAY_BUTTON_RECIPE_UNLOCKED = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/recipe_unlocked")
        .build();

    public static final UITexture OVERLAY_BUTTON_RECIPE_LOCKED = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/recipe_locked")
        .build();

    public static final UITexture OVERLAY_BUTTON_BATCH_MODE_ON = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/batch_mode_on")
        .build();

    public static final UITexture OVERLAY_BUTTON_BATCH_MODE_OFF = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/batch_mode_off")
        .build();

    public static final UITexture OVERLAY_BUTTON_STRUCTURE_UPDATE = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/structure_update");

    public static final UITexture OVERLAY_BUTTON_FORBIDDEN = fullImage("gregtech", "gui/mui2/overlay_button/forbidden");

    public static final UITexture OVERLAY_BUTTON_CHUNK_LOADING = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/chunkloading")
        .build();

    public static final UITexture OVERLAY_BUTTON_CHUNK_LOADING_OFF = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/chunkloading_off")
        .build();

    public static final UITexture OVERLAY_BUTTON_WORK_AREA = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/work_area")
        .build();

    public static final UITexture OVERLAY_BUTTON_SHOW_WORK_AREA = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/show_work_area")
        .build();

    public static final UITexture OVERLAY_BUTTON_REPLACE_COBBLE_ON = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/replace_cobble_on")
        .build();

    public static final UITexture OVERLAY_BUTTON_REPLACE_COBBLE_OFF = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/replace_cobble_off")
        .build();

    public static final UITexture OVERLAY_BUTTON_RETRACT_PIPE = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/retract_pipes")
        .build();

    public static final UITexture OVERLAY_BUTTON_LIQUIDMODE = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/LiquidMode")
        .build();

    public static final UITexture OVERLAY_BUTTON_LIQUIDMODE_OFF = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/LiquidMode_off")
        .build();

    public static final UITexture OVERLAY_BUTTON_LOCKED = UITexture.builder()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/lock_small")
        .build();

    public static final UITexture OVERLAY_BUTTON_PRINT = fullImage("gregtech", "gui/mui2/overlay_button/print");

    public static final UITexture OVERLAY_BUTTON_TRANSPOSE = fullImage("gregtech", "gui/mui2/overlay_button/transpose");

    public static final UITexture OVERLAY_BUTTON_COPY = fullImage("gregtech", "gui/mui2/overlay_button/copy_to_clipboard");

    public static final UITexture OVERLAY_BUTTON_REDSTONESNIFFERLOCATE = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/redstoneSnifferLocate");

    public static final UITexture OVERLAY_BUTTON_MUFFLE_ON = UITexture.builder()
        .fullImage()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/muffle_on")
        .build();

    public static final UITexture OVERLAY_BUTTON_MUFFLE_OFF = UITexture.builder()
        .fullImage()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/muffle_off")
        .build();

    public static final UITexture OVERLAY_BUTTON_SMALL_POWER_SWITCH_ON = UITexture.builder()
        .fullImage()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/small_power_switch_on")
        .build();

    public static final UITexture OVERLAY_BUTTON_SMALL_POWER_SWITCH_OFF = UITexture.builder()
        .fullImage()
        .canApplyTheme()
        .location("gregtech", "gui/mui2/overlay_button/small_power_switch_off")
        .build();

    public static final UITexture OVERLAY_EXPORT = fullImage("gregtech", "gui/mui2/overlay_button/export");

    public static final UITexture OVERLAY_IMPORT = fullImage("gregtech", "gui/mui2/overlay_button/import");

    public static final UITexture OVERLAY_BUTTON_BOUNDING_BOX = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/bounding_box")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_HIGHLIGHT_BLOCK = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/highlight_block");

    public static final UITexture OVERLAY_SLOT_CHARGER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/charger")
        .build();

    public static final UITexture OVERLAY_SLOT_CHARGER_FLUID = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/charger_fluid")
        .build();

    public static final UITexture OVERLAY_SLOT_BEAKER_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/beaker_1")
        .build();

    public static final UITexture OVERLAY_SLOT_BEAKER_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/beaker_2")
        .build();

    public static final UITexture OVERLAY_SLOT_BENDER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/bender")
        .build();

    public static final UITexture OVERLAY_SLOT_BOX = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/box")
        .build();

    public static final UITexture OVERLAY_SLOT_BOXED = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/boxed")
        .build();

    public static final UITexture OVERLAY_SLOT_CANNER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/canner")
        .build();

    public static final UITexture OVERLAY_SLOT_CAULDRON = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/cauldron")
        .build();

    public static final UITexture OVERLAY_SLOT_CENTRIFUGE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/centrifuge")
        .build();

    public static final SteamTexture OVERLAY_SLOT_CENTRIFUGE_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/centrifuge_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_CENTRIFUGE_FLUID = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/centrifuge_fluid")
        .build();

    public static final UITexture OVERLAY_SLOT_COMPRESSOR = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/compressor")
        .build();

    public static final SteamTexture OVERLAY_SLOT_COMPRESSOR_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/compressor_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_CRUSHED_ORE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/crushed_ore")
        .build();

    public static final SteamTexture OVERLAY_SLOT_CRUSHED_ORE_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/crushed_ore_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_CUTTER_SLICED = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/cutter_sliced")
        .build();

    public static final UITexture OVERLAY_SLOT_DATA_STICK = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/data_stick")
        .build();

    public static final UITexture OVERLAY_SLOT_DUST = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/dust")
        .build();

    public static final UITexture OVERLAY_SLOT_EXPLOSIVE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/explosive")
        .build();

    public static final UITexture OVERLAY_SLOT_FURNACE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/furnace")
        .build();

    public static final UITexture OVERLAY_SLOT_GEM = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/gem")
        .build();

    public static final UITexture OVERLAY_SLOT_HAMMER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/hammer")
        .build();

    public static final SteamTexture OVERLAY_SLOT_HAMMER_STEAM = SteamTexture.builder()
        .location("gui/mui2/overlay_slot/hammer_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_SLOT_HEATER_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/heater_1")
        .build();

    public static final UITexture OVERLAY_SLOT_HEATER_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/heater_2")
        .build();

    public static final UITexture OVERLAY_SLOT_IMPLOSION = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/implosion")
        .build();

    public static final UITexture OVERLAY_SLOT_LENS = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/lens")
        .build();

    public static final UITexture OVERLAY_SLOT_MICROSCOPE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/microscope")
        .build();

    public static final UITexture OVERLAY_SLOT_MOLD = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/mold")
        .build();

    public static final UITexture OVERLAY_SLOT_MOLECULAR_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/molecular_1")
        .build();

    public static final UITexture OVERLAY_SLOT_MOLECULAR_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/molecular_2")
        .build();

    public static final UITexture OVERLAY_SLOT_MOLECULAR_3 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/molecular_3")
        .build();

    public static final UITexture OVERLAY_SLOT_PAGE_BLANK = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/page_blank")
        .build();

    public static final UITexture OVERLAY_SLOT_PAGE_PRINTED = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/page_printed")
        .build();

    public static final UITexture OVERLAY_SLOT_PRESS_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/press_1")
        .build();

    public static final UITexture OVERLAY_SLOT_PRESS_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/press_2")
        .build();

    public static final UITexture OVERLAY_SLOT_PRESS_3 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/press_3")
        .build();

    public static final UITexture OVERLAY_SLOT_RECYCLE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/recycle")
        .build();

    public static final UITexture OVERLAY_SLOT_ROD_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/rod_1")
        .build();

    public static final UITexture OVERLAY_SLOT_ROD_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/rod_2")
        .build();

    public static final UITexture OVERLAY_SLOT_SQUARE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/square")
        .build();

    public static final UITexture OVERLAY_SLOT_UUA = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/uua")
        .build();

    public static final UITexture OVERLAY_SLOT_UUM = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/uum")
        .build();

    public static final UITexture OVERLAY_SLOT_VIAL_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/vial_1")
        .build();

    public static final UITexture OVERLAY_SLOT_VIAL_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/vial_2")
        .build();

    public static final UITexture OVERLAY_SLOT_WIREMILL = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/wiremill")
        .build();

    public static final UITexture OVERLAY_SLOT_CIRCUIT = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_slot/circuit")
        .build();

    public static final UITexture[] OVERLAY_SLOTS_NUMBER = IntStream.range(0, 12)
        .mapToObj(
            i -> UITexture.builder()
                .canApplyTheme()
                .fullImage()
                .location("gregtech", "gui/mui2/overlay_slot/number_" + i)
                .build())
        .toArray(UITexture[]::new);

    public static final UITexture OVERLAY_TOOLBOX_WRENCH = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_wrench")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_WIRE_CUTTER = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_wire_cutter")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_SCREWDRIVER = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_screwdriver")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_SOFT_MALLET = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_soft_mallet")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_HARD_HAMMER = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_hard_hammer")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_CROWBAR = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_crowbar")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_SOLDERING_IRON = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_soldering_iron")
        .build();

    public static final UITexture OVERLAY_TOOLBOX_BATTERY = UITexture.builder()
        .location("gregtech", "gui/mui2/external/items/toolbox/toolbox_slot_battery")
        .build();

    public static final UITexture OVERLAY_SLOT_BEE_DRONE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/bee_drone")
        .canApplyTheme()
        .fullImage()
        .build();

    public static final UITexture OVERLAY_SLOT_BEE_QUEEN = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/bee_queen")
        .canApplyTheme()
        .fullImage()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_SLOT_APIARY_UPGRADE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/slot_upgrade")
        .canApplyTheme()
        .fullImage()
        .build();

    public static final UITexture PROGRESSBAR_ARROW_STANDARD = fullImageColorableGT("progressbar/arrow");

    public static final SteamTexture PROGRESSBAR_ARROW_STEAM = SteamTexture.builder()
        .location("gui/mui2/progressbar/arrow_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PROGRESSBAR_ARROW_BBF = fullImageColorableGT("progressbar/arrow_bbf");

    public static final UITexture PROGRESSBAR_WINDMILL_GRINDSTONE = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/windmill_grindstone")
        .fullImage()
        .name(GTTextureIds.PROGRESSBAR_WINDMILL_GRINDSTONE)
        .build();

    public static final UITexture PROGRESSBAR_BOILER_HEAT = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/boiler_heat")
        .fullImage()
        .name(GTTextureIds.PROGRESSBAR_BOILER_HEAT)
        .build();

    public static final UITexture PROGRESSBAR_FUEL_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/fuel")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.PROGRESSBAR_FUEL_STANDARD)
        .build();

    public static final UITexture PROGRESSBAR_NANOCHIP_CALIBRATION = UITexture
        .fullImage("gregtech", "gui/mui2/progressbar/nanochip_calibration");

    public static final SteamTexture PROGRESSBAR_FUEL_STEAM = SteamTexture.builder()
        .location("gui/mui2/progressbar/fuel_%s")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.PROGRESSBAR_FUEL_BRONZE, GTTextureIds.PROGRESSBAR_FUEL_STEEL, null)
        .build();

    public static final UITexture PROGRESSBAR_PURIFICATION_UNIT = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/water_purification_unit")
        .fullImage()
        .build();

    public static final UITexture PROGRESSBAR_ARROW_MULTIPLE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/arrow_multiple")
        .build();

    public static final UITexture PROGRESSBAR_ASSEMBLE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/assemble")
        .build();

    public static final UITexture PROGRESSBAR_ASSEMBLY_LINE_1 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/assemblyline_1")
        .build();

    public static final UITexture PROGRESSBAR_ASSEMBLY_LINE_2 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/assemblyline_2")
        .build();

    public static final UITexture PROGRESSBAR_ASSEMBLY_LINE_3 = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/assemblyline_3")
        .build();

    public static final UITexture PROGRESSBAR_BATH = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/bath")
        .build();

    public static final UITexture PROGRESSBAR_BENDING = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/bending")
        .build();

    public static final UITexture PROGRESSBAR_BOILER_STEAM = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/boiler_steam")
        .build();

    public static final UITexture PROGRESSBAR_BOILER_WATER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/boiler_water")
        .build();

    public static final UITexture PROGRESSBAR_CANNER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/canner")
        .build();

    public static final UITexture PROGRESSBAR_CIRCUIT_ASSEMBLER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/circuit_assembler")
        .build();

    public static final UITexture PROGRESSBAR_COMPRESS = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/compress")
        .build();

    public static final SteamTexture PROGRESSBAR_COMPRESS_STEAM = SteamTexture.builder()
        .location("gui/mui2/progressbar/compress_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PROGRESSBAR_CUT = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/cut")
        .build();

    public static final UITexture PROGRESSBAR_EXTRACT = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/extract")
        .build();

    public static final SteamTexture PROGRESSBAR_EXTRACT_STEAM = SteamTexture.builder()
        .location("gui/mui2/progressbar/extract_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PROGRESSBAR_EXTRUDE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/extrude")
        .build();

    public static final UITexture PROGRESSBAR_FISHING = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/fishing")
        .build();

    public static final UITexture PROGRESSBAR_HAMMER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/hammer")
        .build();

    public static final SteamTexture PROGRESSBAR_HAMMER_STEAM = SteamTexture.builder()
        .location("gui/mui2/progressbar/hammer_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PROGRESSBAR_HAMMER_BASE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/hammer_base")
        .build();

    public static final UITexture PROGRESSBAR_LATHE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/lathe")
        .build();

    public static final UITexture PROGRESSBAR_LATHE_BASE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/lathe_base")
        .build();

    public static final UITexture PROGRESSBAR_MACERATE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/macerate")
        .build();

    public static final SteamTexture PROGRESSBAR_MACERATE_STEAM = SteamTexture.builder()
        .location("gui/mui2/progressbar/macerate_%s")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PROGRESSBAR_MAGNET = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/magnet")
        .build();

    public static final UITexture PROGRESSBAR_MIXER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/mixer")
        .build();

    public static final UITexture PROGRESSBAR_RECYCLE = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/recycle")
        .build();

    public static final UITexture PROGRESSBAR_SIFT = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/sift")
        .build();

    public static final UITexture PROGRESSBAR_STORED_EU = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/stored_eu")
        .build();

    public static final UITexture PROGRESSBAR_WIREMILL = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/wiremill")
        .build();

    public static final UITexture PROGRESSBAR_FLOCCULATION = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/flocculation")
        .build();

    public static final UITexture PROGRESSBAR_CLARIFIER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/clarifier")
        .build();

    public static final UITexture PROGRESSBAR_PH_NEUTRALIZATION = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/phneutralization")
        .build();

    public static final UITexture PROGRESSBAR_OZONATION = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/ozonation")
        .build();

    public static final UITexture PROGRESSBAR_PLASMA_HEATER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/water_plasma_heater")
        .build();

    public static final UITexture PROGRESSBAR_UV_TREATMENT = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/uvtreatment")
        .build();

    public static final UITexture PROGRESSBAR_STEAM_FILL = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/steam_fill")
        .build();

    public static final UITexture PROGRESSBAR_STEAM_FILL_STEEL = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/steam_fill_steel")
        .build();

    public static final UITexture PROGRESSBAR_NAME_REMOVER = UITexture.builder()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/progressbar/name_remover")
        .build();

    public static final UITexture STEAM_GAUGE_BG = UITexture.builder()
        .fullImage()
        .location("gregtech", "gui/mui2/background/steam_dial")
        .build();

    public static final UITexture STEAM_GAUGE_BG_STEEL = UITexture.builder()
        .fullImage()
        .location("gregtech", "gui/mui2/background/steam_dial_steel")
        .name(GTTextureIds.PICTURE_STEAM_GAUGE_STEEL)
        .build();

    public static final UITexture PROGRESSBAR_METER_MINT = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/meter_mint")
        .fullImage()
        .build();

    public static final UITexture PROGRESSBAR_METER_ORANGE = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/meter_orange")
        .fullImage()
        .build();

    public static final UITexture PROGRESSBAR_METER_ROSE = UITexture.builder()
        .location("gregtech", "gui/mui2/progressbar/meter_rose")
        .fullImage()
        .build();

    public static final UITexture BUTTON_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/button/standard")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_STANDARD)
        .build();

    public static final SteamTexture BUTTON_STEAM = SteamTexture.builder()
        .location("gui/mui2/button/%s")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_STEAM)
        .build();

    public static final UITexture BUTTON_STANDARD_PRESSED = UITexture.builder()
        .location("gregtech", "gui/mui2/button/standard_pressed")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_STANDARD_PRESSED)
        .build();

    public static final UITexture BUTTON_STANDARD_DISABLED = UITexture.builder()
        .location("gregtech", "gui/mui2/button/standard_disabled")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final UITexture TOGGLE_BUTTON_STANDARD_DISABLED = UITexture.builder()
        .location("gregtech", "gui/mui2/button/standard_toggle_disabled")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .build();

    public static final SteamTexture BUTTON_STEAM_PRESSED = SteamTexture.builder()
        .location("gui/mui2/button/%s_pressed")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_STEAM_PRESSED)
        .build();

    public static final UITexture BUTTON_FOUNDRY = UITexture.builder()
        .location("gregtech", "gui/mui2/button/foundry")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_FOUNDRY)
        .build();

    public static final UITexture BUTTON_NANOCHIP = UITexture.builder()
        .location("gregtech", "gui/mui2/button/nanochip")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_NANOCHIP)
        .build();

    public static final UITexture BUTTON_FOUNDRY_PRESSED = UITexture.builder()
        .location("gregtech", "gui/mui2/button/foundry_pressed")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_FOUNDRY_PRESSED)
        .build();

    public static final UITexture BUTTON_NANOCHIP_PRESSED = UITexture.builder()
        .location("gregtech", "gui/mui2/button/nanochip_pressed")
        .imageSize(18, 18)
        .adaptable(1)
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_NANOCHIP_PRESSED)
        .build();

    public static final UITexture BUTTON_COVER_TAB_NORMAL_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/tab/cover_normal")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_COVER_TAB_NORMAL_STANDARD)
        .build();

    public static final UITexture BUTTON_COVER_TAB_HIGHLIGHT_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/tab/cover_highlight")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_COVER_TAB_HIGHLIGHT_STANDARD)
        .build();

    public static final UITexture BUTTON_COVER_TAB_DISABLED_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/tab/cover_disabled")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_COVER_TAB_DISABLED_STANDARD)
        .build();

    public static final SteamTexture BUTTON_COVER_TAB_NORMAL_STEAM = SteamTexture.builder()
        .location("gui/mui2/tab/cover_%s_normal")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_COVER_TAB_NORMAL_STEAM)
        .build();

    public static final SteamTexture BUTTON_COVER_TAB_HIGHLIGHT_STEAM = SteamTexture.builder()
        .location("gui/mui2/tab/cover_%s_highlight")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_COVER_TAB_HIGHLIGHT_STEAM)
        .build();

    public static final SteamTexture BUTTON_COVER_TAB_DISABLED_STEAM = SteamTexture.builder()
        .location("gui/mui2/tab/cover_%s_disabled")
        .fullImage()
        .canApplyTheme()
        .name(GTTextureIds.BUTTON_COVER_TAB_DISABLED_STEAM)
        .build();

    public static final UITexture OVERLAY_BUTTON_ALLOW_INPUT = fullImageGT("overlay_button/allow_input");

    public static final UITexture OVERLAY_BUTTON_ALLOW_OUTPUT = fullImageGT("overlay_button/allow_output");

    public static final UITexture OVERLAY_BUTTON_BLACKLIST = fullImageGT("overlay_button/blacklist");

    public static final UITexture OVERLAY_BUTTON_BLOCK_INPUT = fullImageGT("overlay_button/block_input");

    public static final UITexture OVERLAY_BUTTON_BLOCK_OUTPUT = fullImageGT("overlay_button/block_output");

    public static final UITexture OVERLAY_BUTTON_CHECKMARK = fullImageGT("overlay_button/checkmark");

    public static final UITexture OVERLAY_BUTTON_CROSS = fullImageGT("overlay_button/cross");

    public static final UITexture OVERLAY_BUTTON_EXPORT = fullImageGT("overlay_button/export");

    public static final UITexture OVERLAY_BUTTON_HOURGLASS = fullImageGT("overlay_button/hourglass");

    public static final UITexture OVERLAY_BUTTON_IMPORT = fullImageGT("overlay_button/import");

    public static final UITexture OVERLAY_BUTTON_POWER_SWITCH_ON = fullImageGT("overlay_button/power_switch_on");

    public static final UITexture OVERLAY_BUTTON_POWER_SWITCH_OFF = fullImageGT("overlay_button/power_switch_off");

    public static final UITexture OVERLAY_BUTTON_POWER_SWITCH_DISABLED = fullImageGT(
        "overlay_button/power_switch_disabled");

    public static final UITexture OVERLAY_BUTTON_PROGRESS = fullImageGT("overlay_button/progress");

    public static final UITexture OVERLAY_BUTTON_REDSTONE_OFF = fullImageGT("overlay_button/redstone_off");

    public static final UITexture OVERLAY_BUTTON_REDSTONE_ON = fullImageGT("overlay_button/redstone_on");

    public static final UITexture OVERLAY_BUTTON_SCREWDRIVER = fullImageGT("overlay_button/screwdriver");

    public static final UITexture OVERLAY_BUTTON_USE_PROCESSING_STATE = fullImageGT(
        "overlay_button/use_processing_state");

    public static final UITexture OVERLAY_BUTTON_USE_INVERTED_PROCESSING_STATE = fullImageGT(
        "overlay_button/use_inverted_processing_state");

    public static final UITexture OVERLAY_BUTTON_WHITELIST = fullImageGT("overlay_button/whitelist");

    public static final UITexture OVERLAY_BUTTON_GATE_AND = fullImageGT("overlay_button/gate_and");

    public static final UITexture OVERLAY_BUTTON_GATE_NAND = fullImageGT("overlay_button/gate_nand");

    public static final UITexture OVERLAY_BUTTON_GATE_OR = fullImageGT("overlay_button/gate_or");

    public static final UITexture OVERLAY_BUTTON_GATE_NOR = fullImageGT("overlay_button/gate_nor");

    public static final UITexture OVERLAY_BUTTON_ANALOG = fullImageGT("overlay_button/analog");

    public static final UITexture OVERLAY_BUTTON_PLUS_SMALL = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/plus_small");

    public static final UITexture OVERLAY_BUTTON_PLUS_LARGE = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/plus_large");

    public static final UITexture OVERLAY_BUTTON_RESIZE_PANEL = fullImageGT("overlay_button/resize_panel");

    public static final UITexture OVERLAY_BUTTON_ASSEMBLER_MODE = UITexture
        .fullImage("goodgenerator", "gui/mui2/overlay_button/assembler_mode");

    public static final UITexture OVERLAY_BUTTON_PRECISE_MODE = UITexture
        .fullImage("goodgenerator", "gui/mui2/overlay_button/precise_mode");

    public static final UITexture OVERLAY_BUTTON_AUTOOUTPUT_ITEM = UITexture.builder()
        .nonOpaque()
        .canApplyTheme()
        .fullImage()
        .location("gregtech", "gui/mui2/overlay_button/autooutput_item")
        .build();

    public static final UITexture OVERLAY_BUTTON_AUTOPULL_ME = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/auto_pull_me");

    public static final UITexture OVERLAY_BUTTON_AUTOPULL_ME_DISABLED = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/auto_pull_me_disabled");

    public static final UITexture OVERLAY_BUTTON_ONE_STACK_LIMIT = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/one_stack_limit");

    public static final UITexture OVERLAY_BUTTON_LMA_ANIMATION_ON = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/lma_animation_on");

    public static final UITexture OVERLAY_BUTTON_LMA_ANIMATION_OFF = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/lma_animation_off");

    public static final UITexture FOUNDRY_CALCULATOR = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/foundry_calculator")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture TT_OVERLAY_BUTTON_FILTER_NO_DRONE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/disable")
        .imageSize(16, 16)
        .build();

    public static final UITexture OVERLAY_BUTTON_FINITE_MODE_PANEL = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/finite_mode_panel");

    public static final UITexture OVERLAY_BUTTON_FINITE_MODE_ON = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/finite_mode_on");

    public static final UITexture OVERLAY_BUTTON_FINITE_MODE_OFF = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/finite_mode_off");

    public static final UITexture OVERLAY_SLOT_PATTERN_ME = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_slot/pattern_me")
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_PATTERN_OPTIMIZE = UITexture
        .fullImage("gregtech", "gui/mui2/overlay_button/pattern_optimize");

    public static final UITexture OVERLAY_BUTTON_X2 = UITexture.fullImage("gregtech", "gui/mui2/overlay_button/x2");

    public static final UITexture OVERLAY_BUTTON_EMIT_ENERGY = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/emit_energy")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_EMIT_REDSTONE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/emit_redstone")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_INVERT_REDSTONE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/invert_redstone")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_STOCKING_MODE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/stocking_mode")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_SORTING_MODE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/sorting_mode")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_NBT = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/nbt")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_INVERT_FILTER = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/invert_filter")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_CRYOTHEUM_ON = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/mvf_cryo_on")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_PYROTHEUM_ON = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/mebf_pyro_on")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_CRYOTHEUM_OFF = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/mvf_cryo_off")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_PYROTHEUM_OFF = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/mebf_pyro_off")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_SIDE_SELECTION_LEFT = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/side_selection_left")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_SIDE_SELECTION_UP = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/side_selection_up")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_SIDE_SELECTION_RIGHT = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/side_selection_right")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_SIDE_SELECTION_DOWN = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/side_selection_down")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_SIDE_SELECTION_BACK = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/side_selection_back")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_CYCLIC = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/cyclic")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture OVERLAY_BUTTON_SHUFFLE = UITexture.builder()
        .location("gregtech", "gui/mui2/overlay_button/shuffle")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PICTURE_GT_LOGO_STANDARD = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/gt_logo_standard")
        .canApplyTheme()
        .fullImage()
        .name(GTTextureIds.PICTURE_GT_LOGO_STANDARD)
        .build();

    public static final UITexture PICTURE_GT_LOGO_GRAY = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/gt_logo_gray")
        .fullImage()
        .build();

    public static final SteamTexture PICTURE_GT_LOGO_STEAM = SteamTexture.builder()
        .location("gui/mui2/picture/gt_logo_%s")
        .fullImage()
        .name(GTTextureIds.PICTURE_GT_LOGO_STEAM)
        .build();

    public static final UITexture PICTURE_EXOFOUNDRY_LOGO = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_logo")
        .fullImage()
        .name(GTTextureIds.PICTURE_LOGO_EXOFOUNDRY)
        .build();

    public static final UITexture PICTURE_NANOCHIP_LOGO = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/nanochip_logo")
        .fullImage()
        .name(GTTextureIds.PICTURE_LOGO_NANOCHIP)
        .build();

    public static final UITexture PICTURE_NANOCHIP_ARROW = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/nanochip_arrow")
        .fullImage()
        .build();

    public static final UITexture PICTURE_TRANSPARENT = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/transparent")
        .fullImage()
        .build();

    public static final UITexture INFORMATION_SYMBOL = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/information_symbol")
        .fullImage()
        .build();

    public static final UITexture INFORMATION_BUBBLE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/information_bubble")
        .fullImage()
        .build();

    public static final UITexture EXOFOUNDRY_BASE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_base")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_UNSET = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_unset")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_HR = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_heliocast_reinforcement")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture WATER_PURIFICATION_ONLINE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/water_purification_online")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture WATER_PURIFICATION_IDLE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/water_purification_idle")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture WATER_PURIFICATION_OFFLINE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/water_purification_offline")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_HC = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_hypercooler")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_TDS = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_tds")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_EFF_OC = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_efficient_oc")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_ECB = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_extra_casting_basins")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_SLC = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_streamlined_casters")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_PES = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_power_efficient_subsystems")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_PES_EOC = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_pes_eoc_pair")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_ECB_SLC = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_ecb_slc_pair")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_UC_HC = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_uc_hc_pair")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_HR_SELF = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_hr_self_pair")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_PES_EOC_ACTIVE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_pes_eoc_pair_active")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_ECB_SLC_ACTIVE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_ecb_slc_pair_active")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_UC_HC_ACTIVE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_uc_hc_pair_active")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture EXOFOUNDRY_PAIR_HR_SELF_ACTIVE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/exofoundry_hr_self_pair_active")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture PICTURE_INFO = fullImage("gregtech", "gui/mui2/picture/information_round");

    public static final UITexture PICTURE_SLOTS_HOLO_3BY3 = UITexture
        .fullImage("gregtech", "gui/mui2/picture/slots_holo_3by3");

    public static final UITexture PICTURE_ARROW_DOUBLE = UITexture.fullImage("gregtech", "gui/mui2/picture/arrow_double");

    public static final UITexture PICTURE_PLUS_RED = UITexture.fullImage("gregtech", "gui/mui2/picture/plus_red");

    public static final UITexture PICTURE_SCREEN_BLACK = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/screen_black")
        .imageSize(16, 16)
        .canApplyTheme()
        .adaptable(2)
        .build();

    public static final UITexture PICTURE_ARROW_22_RED = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_22_red")
        .canApplyTheme()
        .imageSize(87, 22)
        .adaptable(0, 0, 11, 0)
        .build();

    public static final UITexture PICTURE_ARROW_22_BLUE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_22_blue")
        .canApplyTheme()
        .imageSize(87, 22)
        .adaptable(0, 0, 11, 0)
        .build();

    public static final UITexture PICTURE_ARROW_22_WHITE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_22_white")
        .canApplyTheme()
        .imageSize(87, 22)
        .adaptable(0, 0, 11, 0)
        .build();

    public static final UITexture PICTURE_ARROW_24_RED = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_24_red")
        .canApplyTheme()
        .imageSize(69, 24)
        .adaptable(0, 0, 12, 0)
        .build();

    public static final UITexture PICTURE_ARROW_24_BLUE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_24_blue")
        .canApplyTheme()
        .imageSize(69, 24)
        .adaptable(0, 0, 12, 0)
        .build();

    public static final UITexture PICTURE_ARROW_6_WHITE_PRE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_6_white_pre")
        .canApplyTheme()
        .imageSize(6, 6)
        .adaptable(3, 0, 3, 0)
        .build();

    public static final UITexture PICTURE_ARROW_24_WHITE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/arrow_24_white")
        .canApplyTheme()
        .imageSize(69, 24)
        .adaptable(0, 0, 12, 0)
        .build();

    public static final UITexture PICTURE_SUPER_BUFFER = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/super_buffer")
        .fullImage()
        .canApplyTheme()
        .build();

    public static final UITexture PICTURE_GAUGE = UITexture.builder()
        .location("gregtech", "gui/mui2/picture/gauge.png")
        .fullImage()
        .canApplyTheme()
        .nonOpaque()
        .build();

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_DEFAULT = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_default");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_CHEMBATH = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_chembath");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_WASHPLANT = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_washplant");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_SIMPLEWASHER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_simplewasher");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_PACKAGER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_packager");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_UNPACKAGER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_unpackager");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_SEPARATOR = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_separator");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_POLARIZER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_polarizer");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_LPF_FLUID = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_lpf_fluid");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_LPF_METAL = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_lpf_metal");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_BENDING = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_bending");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_FORMING = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_forming");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_ACCELERATOR = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_lhc_accelerator");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_COLLIDER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_lhc_collider");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_MASS_FABRICATING = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_mass_fabrication");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_DISTILLING = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_distilling");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_DISTILLATION_TOWER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_tower");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_SCANNER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_scanner");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_ARC = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_arc");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_PLASMA_ARC = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_plasma_arc");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_RESEARCH = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_research");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_RECYCLING = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_recycling");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_COMPRESSING = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_compressing");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_SINGULARITY = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_singularity");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_STEAM = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_dehp_steam");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_FORGE = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_forge");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_MACERATOR = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_macerator");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_WASHER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_washer");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_SIFTER = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_sifter");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_THERMAL = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_thermal");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_CENTRIFUGE = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_centrifuge");

    public static final UITexture OVERLAY_BUTTON_MACHINEMODE_IOF_BATH = fullImage(
        "gregtech",
        "gui/mui2/overlay_button/machine_mode_iof_bath");

    public static final UITexture OVERLAY_COKE_OVEN_FLUID_SLOT_GAUGE = fullImageColorableGT(
        "overlay_slot/overlay_coke_oven_fluid_slot_gauge");

    public static final UITexture BACKGROUND_COKE_OVEN_FLUID_SLOT = fullImageColorableGT(
        "overlay_slot/background_coke_oven_fluid_slot.png");

    public static final UITexture BACKGROUND_COKE_OVEN_COAL = fullImageColorableGT("overlay_slot/coke_oven_coal.png");

    public static final UITexture BACKGROUND_COKE_OVEN = UITexture.builder()
        .location("gregtech", "gui/mui2/background/coke_oven")
        .imageSize(176, 166)
        .adaptable(4)
        .canApplyTheme()
        .name(GTTextureIds.BACKGROUND_COKE_OVEN)
        .build();

    public static final UITexture PROGRESSBAR_ARROW = PROGRESSBAR_ARROW_STANDARD;
    public static final UITexture PICTURE_STALLED_ELECTRICITY = OVERLAY_POWER_LOSS;

    private static UITexture fullImageGT(String path) {
        return fullImage("gregtech", "gui/mui2/" + path);
    }

    private static UITexture fullImageColorableGT(String path) {
        return fullImage("gregtech", "gui/mui2/" + path, ColorType.DEFAULT);
    }
}
