package gregtech.api.threads;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.Assert.*;

public class GT_TickLockTest {
    @Test public void shutdownReleasesEveryOwnedHoldAndUnblocksWorker() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch done = new CountDownLatch(1);
        lock.lock(); lock.lock();
        Thread worker = new Thread(() -> {
            lock.lock();
            try { done.countDown(); } finally { lock.unlock(); }
        });
        worker.setDaemon(true);
        try {
            worker.start();
            assertEquals(2, GT_TickLock.releaseOwnedLock(lock));
            assertTrue(done.await(5, TimeUnit.SECONDS));
            worker.join(5000);
            assertFalse(worker.isAlive());
            assertEquals(0, GT_TickLock.releaseOwnedLock(lock));
        } finally { while (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    @Test public void cleanupDoesNotReleaseAnotherThreadsLock() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
        Thread owner = new Thread(() -> {
            lock.lock();
            try { held.countDown(); release.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { lock.unlock(); }
        });
        owner.setDaemon(true);
        try {
            owner.start();
            assertTrue(held.await(5, TimeUnit.SECONDS));
            assertEquals(0, GT_TickLock.releaseOwnedLock(lock));
            assertTrue(lock.isLocked());
            assertFalse(lock.tryLock());
        } finally { release.countDown(); owner.join(5000); }
    }

    @Test public void repeatedFailedServersCanReuseSameLockInOneJvm() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        AtomicInteger completed = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            Thread server = new Thread(() -> {
                lock.lock();
                try { throw new IllegalStateException("tick START failed before END"); }
                catch (IllegalStateException expected) { }
                finally { GT_TickLock.releaseOwnedLock(lock); }
                completed.incrementAndGet();
            });
            server.setDaemon(true);
            server.start(); server.join(5000);
            assertFalse(server.isAlive());
            assertFalse(lock.isLocked());
        }
        assertEquals(3, completed.get());
    }
}
