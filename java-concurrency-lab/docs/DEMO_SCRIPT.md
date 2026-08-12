# Java Concurrency Lab Demo Script

## Verify

Use a JDK that supports `--release 21`. On this machine, OpenJDK 25 is available locally:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\java-concurrency-lab
$jdk = "C:\Users\Chiranjeev Jain\.jdks\openjdk-25"
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
mvn test
```

Expected result: `ConcurrencyFailuresTest` runs 5 tests with no failures.

## Run The Safe Load

```powershell
mvn exec:java "-Dexec.args=safe 100000"
```

Point out:

1. 100,000 submitted orders.
2. Accepted and rejected counts.
3. Throughput on this machine.
4. Executor active, queued, completed, and submitted work.
5. `exposure-limit-invariant=true`.

## Run The Failure Demos

```powershell
mvn exec:java "-Dexec.args=failures"
```

Point out:

1. Lost update: expected count is much higher than the actual count.
2. Unsafe `HashMap`: expected size differs from actual size.
3. Deadlock detection is true.
4. Thread starvation detection is true.
5. Unbounded overload queues too much work.
6. `CallerRunsPolicy` makes the caller execute work under saturation.
7. Contended lock reports measurable wait time.

The failure command may print Maven warnings about lingering threads because some demos intentionally create stuck daemon-style work. Treat that as part of the teaching signal, not the safe design.

## Optional Benchmark/JFR

```powershell
mvn package
java -jar target\benchmarks.jar CounterBenchmark -f 1 -wi 3 -i 5
java -XX:StartFlightRecording=filename=orders.jfr,dumponexit=true,settings=profile -cp target\classes dev.interview.concurrency.LoadGenerator safe 100000
jfr summary orders.jfr
```

## Interview Close

Say: concurrency correctness comes from choosing the right boundary. Concurrent collections protect their own internals, but business invariants still need an explicit atomic transition. The safe design keeps that transition short, makes executor capacity visible, and uses tests plus load output to prove the invariant holds.
