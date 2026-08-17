package dev.interview.concurrency.benchmarks;
import java.util.concurrent.TimeUnit; import java.util.concurrent.atomic.*; import org.openjdk.jmh.annotations.*;
@BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.SECONDS) @Warmup(iterations=2) @Measurement(iterations=3) @Fork(1)
public class CounterBenchmark {
 @State(Scope.Benchmark) public static class S { AtomicLong atomic = new AtomicLong(); LongAdder adder = new LongAdder(); }
 @Benchmark @Threads(8) public long atomicLong(S s) { return s.atomic.incrementAndGet(); }
 @Benchmark @Threads(8) public void longAdder(S s) { s.adder.increment(); }
}
