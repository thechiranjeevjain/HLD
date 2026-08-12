package dev.interview.concurrency;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Time-bounded demonstrations: failures become evidence, never a hung JVM. */
public final class FailureDemos {
    private FailureDemos() {}

    public static String lostUpdates(int workers) throws Exception {
        var unsafe = new UnsafeOrderProcessor();
        var read = new CountDownLatch(workers); var write = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(workers)) {
            for (int i = 0; i < workers; i++) pool.submit(() -> unsafe.processWithReadBarrier(sample(1), read, write));
            if (!read.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("workers did not rendezvous");
            write.countDown(); pool.shutdown(); pool.awaitTermination(2, TimeUnit.SECONDS);
        }
        long expected = workers * 1_000L;
        return "race/lost-update expected=" + expected + " actual=" + unsafe.exposure("CLIENT-1") + " processedCounter=" + unsafe.processed();
    }

    public static String hashMapCorruption(int workers, int entries) throws Exception {
        Map<Integer, Integer> map = new HashMap<>();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(workers)) {
            for (int w = 0; w < workers; w++) { int offset = w * entries; pool.submit(() -> { await(start); for (int i = 0; i < entries; i++) map.put(offset + i, i); }); }
            start.countDown(); pool.shutdown(); pool.awaitTermination(3, TimeUnit.SECONDS);
        }
        return "HashMap expectedSize=" + (workers * entries) + " actualSize=" + map.size() + " (result is undefined; rerun if it happened to match)";
    }

    public static boolean deadlockDetected(Duration timeout) throws Exception {
        Object a = new Object(), b = new Object(); var firstLocks = new CountDownLatch(2);
        Thread t1 = Thread.ofPlatform().daemon().start(() -> { synchronized (a) { firstLocks.countDown(); await(firstLocks); synchronized (b) {} } });
        Thread t2 = Thread.ofPlatform().daemon().start(() -> { synchronized (b) { firstLocks.countDown(); await(firstLocks); synchronized (a) {} } });
        long end = System.nanoTime() + timeout.toNanos();
        do { long[] ids = ManagementFactory.getThreadMXBean().findDeadlockedThreads(); if (ids != null && contains(ids, t1.threadId()) && contains(ids, t2.threadId())) return true; Thread.sleep(10); } while (System.nanoTime() < end);
        return false;
    }

    public static boolean starvationDetected() throws Exception {
        ExecutorService oneThread = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> outer = oneThread.submit(() -> { Future<Integer> inner = oneThread.submit(() -> 42); try { inner.get(100, TimeUnit.MILLISECONDS); return false; } catch (TimeoutException expected) { return true; } });
            return outer.get(1, TimeUnit.SECONDS);
        } finally { oneThread.shutdownNow(); }
    }

    public static int unboundedOverloadQueueSize(int tasks) throws Exception {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); var block = new CountDownLatch(1);
        try { pool.submit(() -> await(block)); for (int i = 1; i < tasks; i++) pool.submit(() -> {}); return pool.getQueue().size(); }
        finally { block.countDown(); pool.shutdownNow(); pool.awaitTermination(1, TimeUnit.SECONDS); }
    }

    public static boolean boundedCallerRunsAppliesBackpressure() throws Exception {
        String caller = Thread.currentThread().getName(); var ranOnCaller = new CompletableFuture<String>(); var block = new CountDownLatch(1);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.CallerRunsPolicy());
        try { pool.execute(() -> await(block)); pool.execute(() -> await(block)); pool.execute(() -> ranOnCaller.complete(Thread.currentThread().getName())); return caller.equals(ranOnCaller.get(1, TimeUnit.SECONDS)); }
        finally { block.countDown(); pool.shutdownNow(); }
    }

    public static long contendedLockWaitNanos() throws Exception {
        var lock = new ReentrantLock(); var held = new CountDownLatch(1); var release = new CountDownLatch(1);
        Thread holder = Thread.ofPlatform().start(() -> { lock.lock(); try { held.countDown(); await(release); } finally { lock.unlock(); } });
        held.await(); long start = System.nanoTime(); Thread waiter = Thread.ofPlatform().start(() -> { lock.lock(); try {} finally { lock.unlock(); } });
        Thread.sleep(100); release.countDown(); waiter.join(); holder.join(); return System.nanoTime() - start;
    }

    public static void printAll() throws Exception {
        System.out.println(lostUpdates(16));
        System.out.println(hashMapCorruption(8, 10_000));
        System.out.println("deadlock detected=" + deadlockDetected(Duration.ofSeconds(1)));
        System.out.println("thread starvation detected=" + starvationDetected());
        System.out.println("unbounded overload queued=" + unboundedOverloadQueueSize(10_000));
        System.out.println("CallerRunsPolicy used caller=" + boundedCallerRunsAppliesBackpressure());
        System.out.printf("contended lock waited=%.1f ms%n", contendedLockWaitNanos() / 1_000_000.0);
    }

    private static Order sample(long id) { return new Order(id, "CLIENT-1", "AAPL", Order.Side.BUY, 1, 1_000); }
    private static void await(CountDownLatch latch) { try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private static boolean contains(long[] values, long wanted) { for (long value : values) if (value == wanted) return true; return false; }
}
