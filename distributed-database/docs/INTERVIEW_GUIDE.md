# Distributed Database Interview Guide

## Two-Minute Pitch

This is an interview-sized distributed key-value database. Three TCP nodes use leader forwarding, consistent hashing, replication, read/write quorum, WAL persistence, and recovery. It deliberately keeps the mechanics visible instead of hiding them behind a database library.

## What To Emphasize

- A follower can accept a write request but forwards it to the current leader.
- Consistent hashing maps each key to a replica set.
- The leader writes to replicas and waits for write quorum.
- Reads query replicas, pick the newest version, and can repair stale replicas.
- Recovery pulls missed records after a node restarts.

## Request Flow

1. Client sends `PUT`, `GET`, `DELETE`, `STATUS`, `RING`, or `RECOVER` over TCP.
2. Command is parsed by `Codec` and handled by `CommandHandler`.
3. Writes go to the leader and then to the key's replica set.
4. Quorum decides success or failure.
5. WAL protects each node across restart.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| TCP text commands | Very easy to demo and debug | Not a production protocol |
| First-live-node leader | Simple leader behavior | Not consensus-safe |
| Quorum reads/writes | Teaches consistency tradeoffs | More latency than single-node reads |
| WAL per node | Restart recovery | No compaction or snapshotting yet |

## FAQ

Q: Is this CP or AP?
A: It is a learning system. Quorum makes some failures visible, but leader election is not consensus-grade, so do not overclaim production consistency.

Q: Why use consistent hashing?
A: It lets key ownership be reasoned from the ring rather than hardcoded node choices.

Q: What would you add next?
A: membership changes, Raft-style consensus, snapshots, compaction, anti-entropy repair, TLS, and authentication.
