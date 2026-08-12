package dev.interview.concurrency;

import java.util.concurrent.ThreadPoolExecutor;

/** Cheap operational snapshot; queue growth and active saturation predict latency. */
public final class ExecutorMonitor {
    public record Snapshot(int poolSize, int active, int queued, long completed, long submitted) {}
    private final ThreadPoolExecutor executor;
    public ExecutorMonitor(ThreadPoolExecutor executor) { this.executor = executor; }
    public Snapshot snapshot() { return new Snapshot(executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount(), executor.getTaskCount()); }
    public String report() { return snapshot().toString(); }
}
