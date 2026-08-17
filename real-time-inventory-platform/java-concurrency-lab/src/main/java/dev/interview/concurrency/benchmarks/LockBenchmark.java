package dev.interview.concurrency.benchmarks;
import java.util.concurrent.TimeUnit; import java.util.concurrent.locks.ReentrantLock; import org.openjdk.jmh.annotations.*;
@BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.SECONDS) @Warmup(iterations=2) @Measurement(iterations=3) @Fork(1)
public class LockBenchmark {
 @State(Scope.Benchmark) public static class S { final Object monitor = new Object(); final ReentrantLock lock = new ReentrantLock(); long value; }
 @Benchmark @Threads(8) public long synchronizedLock(S s) { synchronized(s.monitor) { return ++s.value; } }
 @Benchmark @Threads(8) public long reentrantLock(S s) { s.lock.lock(); try { return ++s.value; } finally { s.lock.unlock(); } }
}
