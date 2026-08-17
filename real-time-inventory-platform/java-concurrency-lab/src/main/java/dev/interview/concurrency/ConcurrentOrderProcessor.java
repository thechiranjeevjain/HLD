package dev.interview.concurrency;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/** Production-shaped processor: bounded admission, per-client atomicity, observable counters. */
public final class ConcurrentOrderProcessor implements AutoCloseable {
    public record Result(long orderId, boolean accepted, String reason) {}
    private final long limit;
    private final ThreadPoolExecutor executor;
    private final AsyncRiskChecks checks;
    private final ConcurrentHashMap<String, AtomicReference<RiskState>> states = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> exposures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final LongAdder accepted = new LongAdder();
    private final LongAdder rejected = new LongAdder();

    public ConcurrentOrderProcessor(int threads, int queueCapacity, long limit) {
        this.limit = limit;
        this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), Thread.ofPlatform().name("orders-", 0).factory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.checks = new AsyncRiskChecks(executor);
    }

    public CompletableFuture<Result> submit(Order order) {
        sequence.incrementAndGet();
        return checks.validate(order).thenApplyAsync(valid -> valid ? applyAtomically(order) : rejectInvalid(order), executor);
    }

    private Result applyAtomically(Order order) {
        ReentrantLock lock = locks.computeIfAbsent(order.clientId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            AtomicReference<RiskState> ref = states.computeIfAbsent(order.clientId(), ignored -> new AtomicReference<>(RiskState.empty()));
            RiskState before = ref.get();
            long next = Math.addExact(before.netExposureCents(), order.signedNotionalCents());
            if (Math.abs(next) > limit) {
                ref.set(before.rejected()); rejected.increment();
                return new Result(order.id(), false, "exposure limit");
            }
            ref.set(before.accepted(order.signedNotionalCents()));
            exposures.merge(order.clientId(), order.signedNotionalCents(), Math::addExact);
            accepted.increment();
            return new Result(order.id(), true, "accepted");
        } finally { lock.unlock(); }
    }

    private Result rejectInvalid(Order order) { rejected.increment(); return new Result(order.id(), false, "validation"); }
    public RiskState state(String client) { return states.getOrDefault(client, new AtomicReference<>(RiskState.empty())).get(); }
    public Map<String, Long> exposureSnapshot() { return Map.copyOf(exposures); }
    public long submitted() { return sequence.get(); }
    public long accepted() { return accepted.sum(); }
    public long rejected() { return rejected.sum(); }
    public ExecutorMonitor monitor() { return new ExecutorMonitor(executor); }
    public boolean await(Duration timeout) throws InterruptedException { executor.shutdown(); return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS); }
    @Override public void close() { executor.shutdownNow(); }
}
