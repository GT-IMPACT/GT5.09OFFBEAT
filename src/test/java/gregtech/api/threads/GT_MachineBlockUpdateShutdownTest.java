package gregtech.api.threads;

import gregtech.common.GT_Proxy;
import net.minecraft.util.ChunkCoordinates;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

public class GT_MachineBlockUpdateShutdownTest {
    @Test
    public void shutdownCancelsActiveAndQueuedUpdates() throws Exception {
        ExecutorService previous = GT_Runnable_MachineBlockUpdate.EXECUTOR_SERVICE;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean queuedRan = new AtomicBoolean();
        try {
            GT_Runnable_MachineBlockUpdate.EXECUTOR_SERVICE = executor;
            executor.submit(() -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException expected) {
                    interrupted.set(true);
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            GT_Runnable_MachineBlockUpdate.submitUpdate(() -> queuedRan.set(true));
            GT_Runnable_MachineBlockUpdate.shutdownExecutorService();
            assertTrue(executor.isTerminated());
            assertTrue(interrupted.get());
            assertFalse(queuedRan.get());
            // Late updates and repeated cleanup must be harmless.
            GT_Runnable_MachineBlockUpdate.submitUpdate(() -> queuedRan.set(true));
            GT_Runnable_MachineBlockUpdate.shutdownExecutorService();
            assertFalse(queuedRan.get());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            GT_Runnable_MachineBlockUpdate.EXECUTOR_SERVICE = previous;
        }
    }

    @Test
    public void machineAndCableUpdatesCanBeInterruptedWhileWaitingForTickLock() throws Exception {
        assertInterruptible(new GT_Runnable_MachineBlockUpdate(null, new ChunkCoordinates(0, 0, 0)));
        assertInterruptible(new GT_Runnable_Cable_Update(null, new ChunkCoordinates(0, 0, 0)));
    }

    private void assertInterruptible(Runnable update) throws Exception {
        ReentrantLock lock = GT_Proxy.TICK_LOCK;
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            update.run();
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        worker.setDaemon(true);
        lock.lock();
        try {
            worker.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!lock.hasQueuedThread(worker) && worker.isAlive() && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertTrue("Worker should be waiting for the tick lock", lock.hasQueuedThread(worker));
            worker.interrupt();
            worker.join(5000);
            assertFalse("Cancellation must not wait for the server to release the lock", worker.isAlive());
            assertTrue(interrupted.get());
            assertTrue(lock.isHeldByCurrentThread());
        } finally {
            worker.interrupt();
            lock.unlock();
            worker.join(5000);
        }
    }
}
