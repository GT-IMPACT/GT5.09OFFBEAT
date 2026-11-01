package gregtech.api.recipe.check;

/**
 * Stable wire/NBT-independent result codes. Append new values; do not reorder
 * existing values.
 */
public enum CheckRecipeResult {
    NONE, SUCCESS, NO_RECIPE, INSUFFICIENT_VOLTAGE, INSUFFICIENT_POWER, ITEM_OUTPUT_FULL, FLUID_OUTPUT_FULL,
    POWER_OVERFLOW, DURATION_OVERFLOW, OUTPUT_OVERFLOW, INPUT_CHANGED, MACHINE_CONDITION, INSUFFICIENT_HEAT,
    RECIPE_LOCK_INVALID, WAITING_FOR_OUTPUT, RECIPE_MAP_UNAVAILABLE, CLEANROOM_REQUIRED, LOW_GRAVITY_REQUIRED,
    INSUFFICIENT_STARTUP_CAPACITY, INSUFFICIENT_STARTUP_ENERGY;

    public boolean wasSuccessful() {
        return this == SUCCESS;
    }

    public String translationKey() {
        return "gt.processing.result." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static CheckRecipeResult fromOrdinal(int value) {
        return value >= 0 && value < values().length ? values()[value] : NONE;
    }
}
