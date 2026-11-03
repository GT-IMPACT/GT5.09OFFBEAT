package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.value.sync.IntSyncValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** GTNH enum synchronization using -1 for null, with bounds checked before applying client updates. */
public class NullableEnumSyncValue<E extends Enum<E>> extends IntSyncValue {

    private final E[] values;

    public NullableEnumSyncValue(Class<E> enumClass, Supplier<E> getter, Consumer<E> setter) {
        super(() -> ordinal(getter.get()), setter == null ? null : index -> {
            E[] constants = enumClass.getEnumConstants();
            if (index >= -1 && index < constants.length) {
                setter.accept(index == -1 ? null : constants[index]);
            }
        });
        values = enumClass.getEnumConstants();
    }

    public NullableEnumSyncValue(Class<E> enumClass, Supplier<E> getter) {
        this(enumClass, getter, null);
    }

    public E getEnumValue() {
        int index = getIntValue();
        return index >= 0 && index < values.length ? values[index] : null;
    }

    @Override
    public void setIntValue(int value, boolean setSource, boolean sync) {
        if (value >= -1 && value < values.length) {
            super.setIntValue(value, setSource, sync);
        }
    }

    private static int ordinal(Enum<?> value) {
        return value == null ? -1 : value.ordinal();
    }
}
