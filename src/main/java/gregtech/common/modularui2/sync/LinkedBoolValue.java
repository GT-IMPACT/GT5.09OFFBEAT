package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;

/** GTNH linked toggle values backed by a single synchronized enum, integer or boolean. */
public final class LinkedBoolValue {

    private LinkedBoolValue() {
    }

    public static <T extends Enum<T>> BoolValue.Dynamic of(EnumSyncValue<T, ?> syncValue, T value) {
        return new BoolValue.Dynamic(() -> syncValue.getValue() == value, enabled -> {
            if (syncValue.getValue() != value) {
                syncValue.setValue(value);
            }
        });
    }

    public static BoolValue.Dynamic of(IntSyncValue syncValue, int value) {
        return new BoolValue.Dynamic(
            () -> syncValue.getIntValue() == value,
            enabled -> syncValue.setValue(value, true, syncValue.getIntValue() != value)
        );
    }

    public static BoolValue.Dynamic of(BooleanSyncValue syncValue, boolean value) {
        return new BoolValue.Dynamic(
            () -> syncValue.getValue() == value,
            enabled -> syncValue.setValue(value, true, syncValue.getValue() != value)
        );
    }
}
