package gregtech.api.threads;

import java.util.concurrent.locks.ReentrantLock;

/** Shutdown cleanup on the tick owner, never a transfer or replacement of the shared lock. */
public final class GT_TickLock {
    private GT_TickLock() {}

    public static int releaseOwnedLock(ReentrantLock lock) {
        int released = 0;
        // A failed START event can skip END. Only the owning server thread can
        // release its holds; a worker or a new server must never unlock this lock.
        while (lock.isHeldByCurrentThread()) {
            lock.unlock();
            released++;
        }
        return released;
    }
}
