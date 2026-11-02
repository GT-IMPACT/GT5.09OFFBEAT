package gregtech.common.modularui2.sync;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class NullableEnumSyncValueTest {

    @Test
    public void invalidClientOrdinalsCannotCorruptTheSourceOrCachedSelection() {
        AtomicReference<Mode> source = new AtomicReference<>(Mode.FIRST);
        NullableEnumSyncValue<Mode> value = new NullableEnumSyncValue<>(Mode.class, source::get, source::set);

        value.setIntValue(Integer.MAX_VALUE, true, false);
        value.setIntValue(-2, true, false);
        assertSame(Mode.FIRST, source.get());
        assertSame(Mode.FIRST, value.getEnumValue());

        value.setIntValue(1, true, false);
        assertSame(Mode.SECOND, source.get());
        assertSame(Mode.SECOND, value.getEnumValue());

        value.setIntValue(-1, true, false);
        assertNull(source.get());
        assertNull(value.getEnumValue());
    }

    private enum Mode {
        FIRST,
        SECOND
    }
}
