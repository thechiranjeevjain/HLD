# 40–60 Minute Interview Guide — Pre-Trade Risk Platform (HLD 26)

This timed guide complements the code-focused [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md) and runnable [DEMO.md](DEMO.md).

## Expected Board at the End

Show client/order gateway → partition router → single-writer risk shards, with in-memory limits/positions/reservations, versioned control-plane configuration, immutable journal + snapshots, a fenced active/standby owner, market-data freshness, and observability. Put **no database on the synchronous check path** in a box.

## Timed Plan

| Time      | Output                                                                               |
| --------- | ------------------------------------------------------------------------------------ |
| 0–5 min   | Clarify products, limits, latency, consistency, update frequency, and failure policy |
| 5–10 min  | Estimate order rate, entities, hot-state size, journal rate, and SLO/RTO             |
| 10–20 min | Draw data plane, control plane, state distribution, journal/snapshot, and HA         |
| 20–32 min | Walk check + atomic reservation and rejection paths                                  |
| 32–42 min | Deep dive dynamic-limit rollout and snapshot + replay recovery                       |
| 42–50 min | Partitioning, failover/fencing, backpressure, kill switches                          |
| 50–60 min | Observability, security, trade-offs, runnable proof, recap                           |

## Scope and Assumptions

Ask whether checks are per order/account/desk/firm, which asset classes and currencies exist, whether positions and open orders must be combined, how fresh marks must be, whether limit changes are intraday, and whether uncertainty fails open or closed.

Assume 250k checks/s peak per region, p99 under 500 μs inside the risk service, one million active risk entities, 2x burst headroom, no accepted order without reserved capacity, and shard recovery under 30 seconds. Use fixed-point integer money/quantity on the hot path.

## Requirements and Invariants

Functional: validate orders, check order size/notional/position/open buy/open sell/loss limits, atomically reserve capacity, release/convert reservations on lifecycle events, apply versioned limit updates, support kill switches, recover, fail over, audit, and explain rejection reasons.

Non-functional: deterministic low tail latency, no remote database dependency on the hot path, horizontal scale, strong ownership of mutable state, bounded recovery, high availability, and safe configuration changes.

Invariants:

- Check and reserve are one serialized transition.
- A limit version becomes visible atomically—never a partial batch.
- Exactly one fenced owner mutates a partition.
- Snapshot offset and journal replay order are explicit.
- Missing/stale required state fails closed.

## Capacity Sketch

- 250k checks/s × roughly 500 bytes of command/audit data ≈ 125 MB/s before replication.
- One million compact exposure records at a few hundred bytes each fits in memory with headroom; avoid per-order object churn.
- Partition by risk group/account so all limits required for one decision are co-located.
- Snapshot frequently enough that `tail events / replay rate < RTO`; do not snapshot on every check.
- Keep database/object storage for control-plane authoring, snapshots, audit/archive, and analytics—not synchronous evaluation.

## Architecture Walkthrough

1. Gateway authenticates and routes by risk-group partition.
2. One event loop owns that partition’s limits, positions, P&L, and open-order reservations.
3. It verifies lease/fencing and kill-switch state.
4. It reads the current immutable, committed limit snapshot and fresh market mark.
5. It evaluates projected exposure including the candidate order.
6. If all checks pass, it reserves exposure in the same transition and returns `ACCEPT`; otherwise it returns deterministic breach reasons.
7. The transition is journaled asynchronously or synchronously according to the acknowledged-durability contract.
8. Execution/cancel/reject events convert or release reservations through the same owner.

The control plane writes limit versions, validates complete batches, stages them, and atomically publishes `COMMITTED(version)`. Risk shards observe monotonically increasing committed versions.

## Recovery and HA

- Snapshot contains state, last applied journal sequence, and config version.
- Restore snapshot, replay only the tail through the same live handler, compare invariants, then advertise ready.
- Active/standby per shard uses an expiring lease and monotonically increasing fencing token.
- A node that cannot prove ownership rejects checks; safety wins over availability.
- Partition movement occurs at a journal barrier with snapshot/offset transfer.
- Reconcile reservations against OMS/execution truth after crash or DR.

## Observability and Security

Track check latency p50/p99/p999, checks/s, rejection reasons, queue depth, market-data/config age, config version/skew, exposure utilization, journal/snapshot/replay lag, pool/allocation/GC behavior, lease/failover changes, and kill-switch actions.

Use service identity, RBAC plus per-entity ACLs, maker-checker limit approval, signed/versioned configuration, encrypted data, tamper-evident audit, and clock synchronization.

## Trade-Offs

- Single-writer shards simplify atomicity and recovery but one hot risk group is bounded by one owner/core; isolate or hierarchically split only with explicit aggregate coordination.
- Synchronous journal replication improves durability but costs latency.
- Eventual configuration distribution is fast, but only complete committed versions may become active.
- Fixed-point arrays reduce allocation but are less flexible than domain-rich objects.
- Fail-closed protects capital but needs operational fallbacks and rapid recovery.

## Runnable Proof

From `trading-risk-platform` run:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot' # or any installed JDK 21+
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -pl pretrade-risk-engine -am test
mvn -pl pretrade-risk-engine spring-boot:run -Dspring-boot.run.profiles=demo
```

The module proves serialized check/reserve, versioned config, snapshot-tail replay through the live handler, lease loss/failover, FIX parsing, kill switch, market-data freshness, audit, and metrics. Its in-process bus/journal/lease/config distribution are explicit substitutes for production infrastructure.
