# Operations

## Deployment Guide

Build locally:

```powershell
mvn test
mvn package
```

Run engine, sidecar, and CLI as separate processes. In Kubernetes, run engine and sidecar in the same pod so the sidecar can reach engine IPC over localhost.

## Runbook

Check health:

```powershell
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole health
```

Inspect core runtime:

```powershell
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole stats
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole markets
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole threads
```

Graceful shutdown:

```powershell
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole shutdown
```

## Incident Response Guide

| Incident           | Detection                                   | Mitigation                         | Long-Term Fix                                      |
| ------------------ | ------------------------------------------- | ---------------------------------- | -------------------------------------------------- |
| Engine crash       | Sidecar `502`, pod restart, missing metrics | Restart pod, inspect previous logs | Add durable journal and crash replay               |
| Sidecar crash      | HTTP health fails, engine still accepts TCP | Restart sidecar/pod                | Add separate sidecar liveness and better readiness |
| IPC timeout        | Sidecar returns `502`                       | Check IPC port/socket, thread dump | Add timeout histograms and circuit breaker         |
| Message corruption | Data server returns `REJECT`                | Inspect client encoder             | Add packet capture and protocol conformance suite  |
| Memory pressure    | Heap route and GC metrics rise              | Reduce load, restart if needed     | Add bounded queues and allocation profiling        |
| Deadlock           | Thread dump shows blocked book locks        | Drain traffic and restart          | Move to event-loop-owned books                     |
| Slow consumers     | TCP sessions rise, latency rises            | Disconnect bad clients             | Add write timeouts and backpressure                |

## Monitoring Guide

Prometheus target:

```text
http://sidecar:8080/metrics
```

Core metrics:

- `exchange_orders_accepted_total`
- `exchange_orders_rejected_total`
- `exchange_trades_executed_total`
- `exchange_matching_latency_nanos_total`

Suggested alerts:

- Sidecar health not `UP` for 2 minutes.
- IPC `502` responses above threshold.
- Rejected orders spike above baseline.
- Heap used above 85 percent for 5 minutes.
- Thread count grows monotonically for 10 minutes.

## Debugging Guide

1. Check `/health`.
2. Check `/stats`.
3. Check `/threads` for blocked or runaway worker threads.
4. Check `/heap` for memory pressure.
5. Check `/orders` for unexpected resting state.
6. Reproduce with binary protocol tests before blaming matching logic.

## Performance Tuning Guide

- Shard markets so each hot symbol has isolated locking.
- Replace cached platform workers with virtual threads or event loops after Java 21 validation.
- Preallocate protocol buffers on hot sessions.
- Record percentile histograms rather than only total latency.
- Benchmark order book alternatives before replacing `TreeMap`.

## Kubernetes Scaling Discussion

The current deployment uses one engine plus one sidecar in a shared pod. This is realistic for a single matching partition. Horizontal scaling requires partitioning markets across pods because two active engines cannot own the same order book without a consensus or routing layer.

Recommended production direction:

- Use a StatefulSet when each pod owns stable market partitions and durable volumes.
- Use a Deployment when the engine is stateless or only serves demo traffic.
- Route market symbols to the owning pod through a gateway.
- Avoid multiple writable replicas for the same market unless a leader election and replay model exists.

Rolling updates must drain data-plane sessions before terminating the old pod. Sidecar readiness should fail before data-plane shutdown so operators stop routing new traffic first.

## Upgrade Guide

1. Add a new binary protocol version.
2. Keep old decoders until all clients migrate.
3. Add command versioning for sidecar IPC.
4. Roll sidecar first when adding read-only routes.
5. Roll engine first when adding command handlers required by sidecar.

## Disaster Recovery Guide

Milestone 1 persistence is in memory. A production release must add a durable journal before claiming crash recovery. Recovery design should:

- Persist accepted orders and executions before acknowledging.
- Replay journal into order books.
- Check sequence monotonicity.
- Emit recovery metrics and audit logs.

## Developer Onboarding Guide

Read `READING_ORDER.md`, run `mvn test`, then inspect `OrderBookTest` and `RuntimeCommandRegistryTest`. After that, run the engine and sidecar locally and query `mc stats`.
