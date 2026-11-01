package gregtech.api.util;

import gregtech.api.logic.ProcessingInputs;
import java.util.function.IntPredicate;

/**
 * Bounded searches avoid one inventory scan per parallel or batch operation.
 */
public final class ParallelHelper {
    public static final int MAX_BATCH_SIZE = 128;
    public static final int MAX_BATCH_TICKS = 128;

    private ParallelHelper() {
    }

    public static int maximum(int limit, IntPredicate fits) {
        int low = 0;
        int high = Math.max(0, limit);
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            if (fits.test(middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    public static int byInputs(GT_Recipe recipe, ProcessingInputs inputs, int limit) {
        return maximum(limit, count -> inputs.copy().consume(recipe, count));
    }

    public static int batchLimit(int parallel, int duration, boolean batch) {
        if (!batch || duration >= MAX_BATCH_TICKS) {
            return parallel;
        }
        long operationLimit = (long) parallel * MAX_BATCH_SIZE;
        long durationLimit = (long) parallel * MAX_BATCH_TICKS / duration;
        return (int) Math.min(Integer.MAX_VALUE, Math.min(operationLimit, durationLimit));
    }
}
