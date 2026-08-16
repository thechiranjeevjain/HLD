# Message Queue Lab Interview Guide

## Two-Minute Pitch

This lab implements a simple in-memory message queue with producer append, offset assignment, a delivery loop, retries, acknowledgments, and dead-letter handling. It demonstrates that sending a message is not the same as completing work.

## What To Emphasize

- Offsets create ordering, not correctness by themselves.
- At-least-once delivery means duplicates are normal.
- Acknowledgment means stop retrying, not exactly-once execution.
- Poison messages must be isolated or they block progress.
- Business handlers need idempotency for safe retries.

## Tradeoffs

| Decision               | Benefit                    | Cost                   |
| ---------------------- | -------------------------- | ---------------------- |
| In-memory log          | Mechanics are easy to see  | No crash recovery      |
| Single delivery thread | Deterministic retry order  | Low throughput         |
| Fixed retry delay      | Simple behavior            | No exponential backoff |
| DLQ after max attempts | Prevents infinite blocking | Needs operator review  |

## FAQ

Q: Is this exactly-once?
A: No. It is at-least-once. Exactly-once requires coordination between message state and side effects.

Q: Why keep acknowledged messages until compaction?
A: It makes log retention and compaction visible as separate concerns.

Q: What would you add next?
A: persistent log, consumer groups, visibility timeouts, idempotency keys, backoff policy, and metrics.
