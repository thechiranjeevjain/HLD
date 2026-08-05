# MiniKV — Trade-offs (Explicit, Causal)

Trade-offs are not compromises.
They are **deliberate sacrifices** to protect invariants.

---

## Trade-off 1: WAL + Memory vs Direct Disk Writes

**Alternative**
Write each key directly to disk.

**Why it sounds good**
Data always lives on disk.

**What breaks**
- Partial writes
- Corrupted files
- Crash mid-write leaves unreadable state

**Why WAL wins**
- Sequential writes are safer
- Intent-first design
- Deterministic recovery

**Sacrifice**
More steps, more code.

**Gain**
Strong durability.

---

## Trade-off 2: Coarse Locking vs Fine-Grained Concurrency

**Alternative**
Per-key locks, lock-free reads.

**Why it sounds good**
High throughput.

**What breaks**
- WAL, store, cache divergence
- Impossible intermediate states

**Why coarse locking**
- Atomicity across components
- Simple reasoning

**Sacrifice**
Throughput.

**Gain**
Correctness you can defend.

---

## Trade-off 3: Lazy TTL vs Precise Expiration

**Alternative**
Timers or expiry heaps.

**Why it sounds good**
Precise TTL.

**What breaks**
- Timer storms
- Race conditions
- Complex synchronization

**Why lazy TTL**
- Expired keys never returned
- Cleanup is eventual

**Sacrifice**
TTL drift.

**Gain**
Simplicity and correctness.

---

## Trade-off 4: Skip Corruption vs Fail Fast

**Alternative**
Crash on corruption.

**What breaks**
- Full outage
- Data loss cascade

**Why skip**
- Partial recovery is better than none

**Sacrifice**
One corrupted record.

**Gain**
System availability.

---

## Trade-off 5: Pause-the-World Compaction

**Alternative**
Concurrent compaction.

**What breaks**
- Crash during rewrite corrupts state
- Extremely complex logic

**Why pause**
- No partial states
- Easy to reason about

**Sacrifice**
Temporary write stall.

**Gain**
Strong correctness during compaction.

---

## Trade-off 6: Single Node vs Distributed

**Alternative**
Replication + consensus.

**Why it’s avoided here**
Distributed systems amplify bad local design.

**Sacrifice**
Disk failure loses data.

**Gain**
Foundational understanding.

---

## Meta Principle

MiniKV consistently chooses:
- Correctness over speed
- Visibility over abstraction
- Recoverability over convenience

These choices are **not universal**.
They are correct for learning how systems fail and recover.
