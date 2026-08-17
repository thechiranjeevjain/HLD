# Java Concurrency Lab

A Java 21 interview lab that answers one practical question: **how does a backend process 100,000 concurrent stock-order requests without corrupting state or losing latency control?** It first makes the failures visible, then runs the safe design.

## Architecture

```text
LoadGenerator -> ConcurrentOrderProcessor.submit
                    |
                    +-> bounded ThreadPoolExecutor + CallerRunsPolicy
                    +-> AsyncRiskChecks (three CompletableFuture checks)
                    +-> per-client ReentrantLock
                    +-> AtomicReference<RiskState>
                    +-> ConcurrentHashMap.merge exposure
                    +-> AtomicLong sequence / LongAdder metrics
                    +-> ExecutorMonitor
```

`Order` is immutable input. `RiskState` is an immutable, consistently published snapshot. `AsyncRiskChecks` overlaps independent validation. `ConcurrentOrderProcessor` owns admission, validation, the exposure invariant, and metrics. `LoadGenerator` creates seeded, repeatable multi-client traffic. `ExecutorMonitor` exposes saturation signals. `UnsafeOrderProcessor` and `FailureDemos` are intentionally broken teaching code.

## Run it

Prerequisites: JDK 21 (`java -version`) and Maven 3.9+.

```bash
mvn clean verify
mvn exec:java "-Dexec.args=safe 100000"
mvn exec:java "-Dexec.args=failures"
java -jar target/benchmarks.jar
```

The failure command is safe to run: deadlocked threads are daemon threads, waits have deadlines, and executors are shut down. A concurrent `HashMap` failure is inherently nondeterministic, so its size may occasionally match; the lost-update demo is forced with a barrier and is deterministic.

## The failures, and why each fix works

| Failure | Broken mechanism | Fix | Why it works |
|---|---|---|---|
| Race/lost update | `read -> add -> write` and `long++` interleave | per-client `ReentrantLock`, `AtomicLong`, `LongAdder` | The compound invariant is serialized per client; counters use atomic/striped updates. |
| `HashMap` corruption | concurrent structural mutation has no happens-before guarantee | `ConcurrentHashMap` and `merge` | Safe publication and atomic per-key remapping prevent lost map updates. |
| Deadlock | two threads acquire the same locks in opposite order | one client lock per transaction, acquired once in a `try/finally` | No circular wait; `finally` guarantees release. In multi-lock code, impose a total lock order. |
| Starvation | a pool task blocks waiting for another task submitted to the same saturated pool | compose `CompletableFuture` stages | Composition does not synchronously wait inside a worker; stages become runnable when dependencies finish. |
| Executor overload | an unbounded queue hides overload until memory and latency explode | bounded `ArrayBlockingQueue` + `CallerRunsPolicy` | The producer executes work when saturated, slowing admission and creating backpressure. |
| Lock contention | one global/long-held lock serializes unrelated clients | short per-client `ReentrantLock` critical section | Independent clients proceed concurrently; only invariant check plus update is locked. |

## Utilities: the interview-level model

- `ConcurrentHashMap`: thread-safe shared index. `merge(key, delta, Math::addExact)` makes the per-key read/modify/write atomic. It does not make a multi-object business invariant atomic, hence the client lock.
- `ThreadPoolExecutor`: an explicit concurrency and queue budget. Pool size limits work in flight; queue capacity limits waiting work.
- `CompletableFuture`: describes dependencies without worker-on-worker blocking. Always supply the intended executor; the common pool is not an accidental capacity plan.
- `AtomicLong`: linearizable exact sequence generation. Use it when every returned value matters.
- `LongAdder`: striped high-throughput metrics. `sum()` is observational, not an atomic transaction boundary.
- `AtomicReference`: safely publishes a complete immutable `RiskState`; readers never see a half-updated snapshot.
- `ReentrantLock`: protects the check-then-act exposure invariant and provides explicit `try/finally`, timed lock, interruptible lock, fairness, and diagnostics options.

## Tests

`ConcurrencyFailuresTest` covers deterministic lost updates, exposure-limit safety under concurrent submissions, JVM deadlock detection, nested-executor starvation, and overload/backpressure. Tests assert a property or detected failure rather than relying on timing-only console output.

## JMH

The benchmarks compare `AtomicLong`/`LongAdder`, single-thread `HashMap`/concurrent `ConcurrentHashMap`, and `synchronized`/`ReentrantLock`. The map cases deliberately have different safety contracts: an 8-thread `HashMap` benchmark would be invalid, not a meaningful speed comparison. Treat results as evidence for this machine and workload, not universal rankings.

Run a focused benchmark:

```bash
java -jar target/benchmarks.jar CounterBenchmark -f 1 -wi 3 -i 5
```

## Java Flight Recorder

Record the safe load (PowerShell users should quote the entire JVM option if their shell requires it):

```bash
mvn package
java -XX:StartFlightRecording=filename=orders.jfr,dumponexit=true,settings=profile -cp target/classes dev.interview.concurrency.LoadGenerator safe 100000
jfr summary orders.jfr
jfr print --events jdk.JavaMonitorEnter,jdk.ThreadPark,jdk.CPULoad,jdk.ExecutionSample orders.jfr
```

Open `orders.jfr` in Java Mission Control. Inspect:

- **Blocked threads:** Java Monitor Blocked / `JavaMonitorEnter`; repeated long events identify monitor bottlenecks.
- **Lock contention:** `ThreadPark` and lock views; correlate waiting threads with the holder and stack trace. `ReentrantLock` commonly parks rather than emitting monitor-enter events.
- **Thread states:** the thread timeline distinguishes RUNNABLE, WAITING, TIMED_WAITING, and BLOCKED; a saturated pool plus a growing queue explains latency.
- **CPU hotspots:** Method Profiling / `ExecutionSample`; inspect hot stacks before changing pool size or lock strategy.

For a deliberate contention recording, run the `failures` mode. JFR overhead with `settings=profile` is suitable for a lab but still changes measurements slightly.

## The 80:20 interview explanation

“I separate independent validation from the atomic business transition. A bounded executor defines concurrency and queue budgets, and caller-runs rejection pushes overload back to producers instead of hiding it. CompletableFuture composes checks without nested blocking. For each client, one short lock protects the exposure check and update; the map itself is concurrent, snapshots are immutable and safely published, exact IDs use AtomicLong, and hot metrics use LongAdder. I verify invariants under load, monitor active/queued/completed work, and use JFR to prove where CPU, blocking, and lock time go.”

## Suggested learning order

`Order` -> `RiskState` -> `UnsafeOrderProcessor` -> `FailureDemos` -> `AsyncRiskChecks` -> `ConcurrentOrderProcessor` -> `ExecutorMonitor` -> `LoadGenerator` -> tests -> benchmarks. Work one class at a time; explain its invariant aloud before editing it.
