# Interview Guide

## The mental model

An order is converted into a pooled `Exposure`, keyed by `CompositeIdentity`, then evaluated across an `ExposureGroup` DAG. Each group owns a `RiskCheckGroup`; each `RiskCheck` composes a calculation with validation. A visit token ensures a shared DAG node runs once. The state transaction applies the projected exposure first, checks projected state, and either commits or rolls it back. Thus a rejected order has zero lasting mutation.

## File-by-file

| File | What to explain |
|---|---|
| `ptr/PtrCore.java` | Domain chain, primitive state arrays, transaction, pool/reference count, DAG visit token, Strategy-based checks, DSF bus, registry, and thin handler. This is the latency-sensitive data plane. |
| `ptr/ControlPlane.java` | Lifecycle, authoritative writer, monotonic versions, event idempotency, observable cache, listener-maintained limit store, ACL/ACE delegation, lease election, split-brain guard. |
| `ptr/Recovery.java` | Snapshot sequence plus journal tail; recovery deliberately invokes the live `InputHandler`. |
| `ptr/PtrRuntime.java` | Spring constructor composition, metrics, registry wiring, journal-before-publish ingress, recent decision view. |
| `ApiController.java` | Config/operations REST only. The absence of an order endpoint is an architectural decision. |
| `SecurityConfiguration.java` | JWT authentication and method-level role checks; entity authorization remains in `AclService`. |
| `DemoScenario.java` | Executable narrative: lifecycle, concurrent publishers, limit change, breach, failover, metrics. |
| `PtrArchitectureTest.java` | Executable invariants: rollback, pool lifecycle, partial/lost config, duplicate commit, replay, delegation, failover. |
| `Dockerfile` / `docker-compose.yml` | Reproducible Java 21 runtime and a local four-service demo. |
| `sidecar/*` | NFF-like process boundary: exposes management state without putting management work on the owner loop. |
| `k8s/deployment.yaml` | Main container plus sidecar in one pod and a sidecar-only service. |
| `observability/prometheus.yml` | Pull-based metrics wiring. JVM binders provide GC/allocation metrics. |

## Requirement â†’ constraint â†’ decision â†’ benefit â†’ trade-off

| Requirement | Constraint | Decision | Benefit | Trade-off |
|---|---|---|---|---|
| Low-latency consistent exposure | Locks add contention and make ordering subtle | One owner event loop | Deterministic mutation with no hot locks | One partition has a throughput ceiling; shard by identity in production |
| Rejection leaves no state | Checks need projected values | Apply in `Transaction`, then commit/rollback | Simple atomic invariant | Undo logic must cover every mutation |
| Shared exposure graph | DAG may converge | Integer visit token per traversal | O(nodes + edges), no per-order `Set` allocation | Token wraparound needs rare array clear |
| Low allocation | GC tail latency matters | Fixed-point longs, arrays, pooled Exposure | Compact cache-friendly working set | Less expressive than rich objects; pool misuse is dangerous |
| Multiple consumers | Exposure lifetime crosses components | Atomic reference count | Explicit safe recycling | Atomic operations cost more than thread-confined counters |
| Dynamic config | Half-applied limits are unsafe | NEW/ADDED/STAGED/COMMITTED; hot store sees only COMMITTED | Atomic activation and auditability | Slower operational workflow |
| Exactly-once is unrealistic | Delivery can duplicate/reorder | Event ID dedupe plus monotonic version | Idempotent duplicate and stale-event rejection | Dedupe retention must be bounded in production |
| Fast config lookup | Cache scans would pollute hot path | Observer-maintained secondary store | One volatile read | Listener correctness becomes critical |
| Recovery matches live behavior | Separate replay code drifts | Replay through same handler | One transition implementation | Handler must be deterministic and side effects controlled |
| HA without split brain | Two primaries corrupt state | Lease plus `assertPrimary` fail-fast | Safety over availability | Pauses when lease service is unavailable |
| Fine-grained security | Roles alone are too broad | JWT/RBAC at API plus entity ACL/ACE | Coarse and fine authorization | More policy administration |
| Operability without jitter | Management queries can allocate/lock | Sidecar reads immutable runtime view | Fault/resource isolation | Snapshot can be slightly stale |

## Exact explanations

### 30 seconds

â€œThis is a Java 21 pre-trade risk simulator with a deliberate data-plane/control-plane split. Concurrent publishers put orders on a DSF-like bus, but one owner thread mutates primitive exposure arrays. It creates a pooled Exposure, traverses a visit-token-deduplicated group DAG, runs OpenBuy, OpenSell, order value, price deviation, and rate checks, then atomically commits or rolls back projected state. Configuration is versioned and only COMMITTED data reaches the hot store. Snapshot/replay, lease failover, JWT plus ACLs, a management sidecar, and percentile metrics make the demo operational rather than CRUD.â€

### 2 minutes

â€œStart at ingress. There is intentionally no order REST endpoint: `PtrRuntime.submit` represents a binary/event-bus gateway. Many publishers can enqueue concurrently. `LocalDsfBus` delivers orders to one `OrderHandler`, which claims ownership of mutable `RiskState`; arrays store open buy/sell by identity. The handler borrows a reference-counted Exposure, begins a transaction by applying projected quantity, and walks the ExposureGroup DAG. The root references a shared leaf twice in the demo, proving the visit token prevents duplicate checks without allocating a HashSet. Checks are calculator/validator strategies. A failure rolls back before the object returns to its pool; success commits.

The control plane is separate. A single named writer accepts monotonic versions and deduplicates event IDs. Config progresses through NEW, ADDED, STAGED, and COMMITTED. A ListenableCache notifies a listener-built store; only COMMITTED limits become one-read hot-path state. Recovery restores a sequence-tagged snapshot and sends journal tail events through the exact live handler. HA uses an expiring lease; every primary-only action should assert the lease and fail closed if ownership is lost. JWT roles protect operations while ACL/ACE handles per-entity permissions and constrained delegation. Micrometer emits throughput, P50/P99/P999 latency, queue, breach, pool, JVM GC, and allocation metrics; the sidecar exposes runtime state separately.â€

### 5 minutes

Use the two-minute version, then add: â€œThe core invariant is serialized state transitions, not merely a concurrent map. Lock-free reads alone cannot make check-plus-reserve atomic. The transaction deliberately evaluates projected state so OpenBuy/OpenSell limits include the candidate order. Fixed-point longs avoid `BigDecimal` allocation in the hot path. The pool has an `AtomicInteger` because an Exposure may be shared outside its owner thread, whereas state arrays require no atomics because they are thread-confined.

For failure handling, journal append precedes bus publish. A real implementation would durably replicate the journal and checkpoint snapshots; here those interfaces are local and deterministic. A snapshot records its last included sequence, so replay applies only events after that point. Duplicate configuration commit is harmless because event IDs are deduped; a lower/equal version under a new event ID is rejected, covering reordered delivery. The lease is the simulated etcd boundary. Expiry lets the standby campaign, and the old primaryâ€™s assertion fails, choosing consistency over availability.

For security, authentication and coarse roles live at the HTTP edge, while `AclService` answers whether a principal can view, edit, or delegate on an entity. Delegation itself requires DELEGATE on that entity and records the delegator. The demo keeps infrastructure intentionally local: DSF is an in-memory bounded queue, RDM is a versioned observable cache, etcd is a synchronized lease store, and the NFF is a tiny sidecar. Those are simplifications; the interfaces and safety invariants are the parts intended to transfer to a real PTR environment.â€

## Top 30 interviewer questions and ideal answers

1. **Why single-thread mutable state?** It makes check-plus-reserve a serial state machine, eliminating lock races and improving tail predictability.
2. **Does that limit scale?** Per partition, yes. Scale by consistent-hash sharding CompositeIdentity while preserving one owner per shard.
3. **Why not ConcurrentHashMap?** It protects individual operations, not the multi-field projected-check-and-reserve invariant.
4. **How is rollback guaranteed?** `Transaction` is `AutoCloseable`; any non-commit path subtracts the exact projected delta.
5. **What about an exception mid-check?** The handler fails closed and try-with-resources rolls back and releases the Exposure.
6. **Why fixed-point longs?** Deterministic decimal semantics with no hot-path decimal allocation; overflow uses checked multiplication.
7. **Why pool objects in modern Java?** To demonstrate tail-latency control for a known, frequently allocated shape; measure before using it in production.
8. **Why AtomicInteger refcounts?** Exposure may cross consumers; atomic retain/release makes ownership explicit. Thread-confined state does not need atomics.
9. **What prevents use-after-free?** Retain before sharing, release after use, and lifecycle checks that throw on retain-after-release/double-release.
10. **How does DAG dedupe work?** Each traversal increments a token; nodes store the last token visited, so converging paths skip repeat evaluation.
11. **What happens on token overflow?** Zero triggers an array clear and restarts at a nonzero token.
12. **Why calculator plus validator?** Calculation and policy comparison vary independently and are testable Strategy components.
13. **How is order rate measured?** A per-identity primitive one-second window; production may use ring buckets for smoother windows.
14. **Why no order REST API?** HTTP serialization and request threads are inappropriate for the latency path; REST is only control/operations.
15. **How is backpressure handled?** The bus is bounded and rejects when full; production ingress should apply protocol-level throttling/fail-closed behavior.
16. **How are config partial updates prevented?** Only COMMITTED objects reach the listener-built hot store.
17. **How are duplicates handled?** The authoritative writer deduplicates event IDs; duplicate COMMITTED delivery returns existing state.
18. **How are reordered updates handled?** Versions must strictly increase for a new event ID, so stale updates fail.
19. **Why one writer?** It prevents conflicting ordering authorities; replicas distribute committed events but do not invent versions.
20. **What does ListenableCache buy?** Observer callbacks incrementally maintain indexes instead of scanning config per order.
21. **How do you recover?** Restore snapshot arrays at sequence N, then replay journal events greater than N through `OrderHandler`.
22. **How do you avoid replay side effects?** Separate deterministic state application from external sinks or suppress/identify already-published outputs.
23. **Can snapshot and journal leave a gap?** The sequence boundary is captured with the snapshot; durable production storage would atomically checkpoint it.
24. **How is leader election safe?** An expiring lease defines authority, and the node asserts the current store leader before primary work.
25. **What if lease renewal fails?** Fail fast and stop decisions; the design prefers no decisions to inconsistent double-primary decisions.
26. **RBAC versus ACL?** RBAC grants operational capability; ACL/ACE narrows it to a desk/account/entity and supports delegation.
27. **How is delegation constrained?** The actor must already hold DELEGATE on that exact entity, and the ACE records who delegated.
28. **Why a sidecar?** It isolates management polling/serialization and provides a separately exposed operational surface.
29. **Which metrics matter first?** Throughput, P99/P999, queue depth, blocks/breaches, pool exhaustion, allocation rate, and GC pauses.
30. **What would you productionize next?** Durable replicated journal/snapshots, multi-shard routing, real etcd leases with fencing tokens, bounded dedupe, signed key rotation, chaos/load testing, and SLO dashboards.

## Real PTR concepts versus simulation

| Faithful concept | Local simplification |
|---|---|
| Ordered message-driven risk evaluation | Bounded in-memory queue instead of proprietary DSF |
| Composite identities and exposure-group DAG | Integer account identity and a two-node example DAG |
| Transactional projected exposure | In-memory delta undo rather than proprietary transaction framework |
| Staged/committed reference data | Local ListenableCache instead of RDM/distributed config |
| Snapshot plus ordered replay | In-memory snapshot/journal rather than replicated durable storage |
| Active/standby lease and fail-fast guard | Synchronized local lease instead of etcd and fencing tokens |
| RBAC plus entity ACL/ACE/delegation | In-memory ACEs; JWT uses a demo symmetric key |
| NFF-style management boundary | Tiny Python sidecar polling an internal endpoint |
| Percentile and JVM telemetry | Micrometer/Prometheus rather than enterprise telemetry stack |

The project is architecture-faithful at the boundaries and invariants, not a claim that the local implementations reproduce proprietary PTR infrastructure.

