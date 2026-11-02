package gregtech.common.modularui2.sync;

import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;

public final class Predicates {

    private Predicates() {
    }

    public static boolean isPositive(SyncHandler<?> syncHandler) {
        return syncHandler instanceof IntSyncValue && ((IntSyncValue) syncHandler).getIntValue() > 0;
    }
}
