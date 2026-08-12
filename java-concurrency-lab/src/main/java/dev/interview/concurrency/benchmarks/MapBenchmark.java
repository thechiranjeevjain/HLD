package dev.interview.concurrency.benchmarks;
import java.util.*; import java.util.concurrent.*; import java.util.concurrent.TimeUnit; import org.openjdk.jmh.annotations.*;
@BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.SECONDS) @Warmup(iterations=2) @Measurement(iterations=3) @Fork(1)
public class MapBenchmark {
 @State(Scope.Benchmark) public static class S { Map<Integer,Long> hash = new HashMap<>(); ConcurrentHashMap<Integer,Long> concurrent = new ConcurrentHashMap<>(); }
 @Benchmark @Threads(1) public long hashMapSingleThread(S s) { return s.hash.merge(1, 1L, Long::sum); }
 @Benchmark @Threads(8) public long concurrentHashMap(S s) { return s.concurrent.merge(1, 1L, Long::sum); }
}
