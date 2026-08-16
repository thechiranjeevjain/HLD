# MiniKV — Deep Interviewer Grilling (Causal, Failure-First)

This document simulates a **real senior interview**, not trivia.
Each question follows the structure:

naive idea → why it seems reasonable → what breaks → how MiniKV fixes it

---

## Q1: Why do we need a Write-Ahead Log (WAL)?

### Why not just write to memory?

**Naive idea**
Memory is fast. Disk is slow. Let’s just store data in memory.

**Why this feels reasonable**

- Crashes feel rare
- Restarting the app feels acceptable
- Most demos “work fine”

**What actually breaks**
Timeline:

1. Client calls `put("a", "1")`
2. Value is written to HashMap
3. System ACKs success
4. Process crashes (power loss / kill -9)

After restart:

- Memory is empty
- Client believes data was saved
- The system has **lied**

**Why this is unacceptable**
Users cannot distinguish:

- “never written”
- “written but lost”

In payments, this is catastrophic.

**Why WAL fixes this**

- Intent is written to disk _before_ memory mutation
- Disk becomes the recovery source of truth
- Restart replays intent deterministically

**Core lesson**

> Never acknowledge something you cannot recover.

---

## Q2: Why must WAL append happen BEFORE memory mutation?

**Alternative design**

1. Write to memory
2. Append to WAL later

**Why people attempt this**

- “Crash window is small”
- “Logging can happen eventually”

**Failure timeline**

1. Write hits memory
2. ACK returned
3. Crash occurs before WAL append

After restart:

- WAL has no record
- Memory is gone
- Acknowledged data is lost

**Key insight**
A WAL that comes _after_ memory gives false confidence.
It is worse than having no WAL at all.

---

## Q3: Why not use ConcurrentHashMap and avoid locking?

**Naive thought**
ConcurrentHashMap is thread-safe → problem solved.

**Why this fails**
Thread safety ≠ atomicity across components.

One logical write spans:

- WAL append
- store update
- cache update

Without a shared lock:

- WAL may contain data not in store
- Cache may contain data not in WAL
- Readers observe impossible states

**Why MiniKV uses coarse locking**

- One logical operation = one atomic critical section
- Correctness is visible and defendable

**Trade-off**
Lower throughput in exchange for correctness confidence.

---

## Q4: TTL seems imprecise. Isn’t that a bug?

**What perfect TTL requires**

- Per-key timers OR
- Priority queue ordered by expiry

**Why that’s avoided**

- Timer storms
- Complex concurrency
- Hard-to-debug races

**What MiniKV guarantees instead**

- Expired keys are never returned
- Cleanup is eventual

**Important distinction**
Precision is not correctness.

Many real systems accept TTL drift under load.

---

## Q5: Why skip corrupted WAL entries instead of crashing?

**Naive expectation**
Corruption = fail fast.

**Why this is dangerous**

- One bad byte bricks the entire system
- All valid data becomes inaccessible

**Chosen strategy**

- Skip corrupted entries
- Preserve remaining history

**Trade-off**
Possible loss of corrupted record vs total outage.

Production systems usually choose availability.

---

## Q6: What is the biggest weakness of this design?

**Answer**
Single-node durability.

Disk failure = total data loss.

**Why this is accepted**
This project teaches foundations.
Replication builds _on top of_ correct single-node logic.

You cannot distribute a broken core.

---

## Q7: What would you improve first in production?

Correct order:

1. WAL segmentation
2. Checksums for corruption detection
3. Crash-safe compaction

Incorrect answers:

- “Microservices”
- “Kubernetes”

Reliability is built bottom-up.

---

## Q8: If we remove the cache, what breaks?

Nothing correctness-wise.

Only performance degrades.

**Senior signal**
Knowing which components are optional.
