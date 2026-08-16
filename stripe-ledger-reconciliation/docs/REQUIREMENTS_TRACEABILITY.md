# Requirements Traceability

| Requirement | Executable evidence |
|---|---|
| Append-only ledger | No ledger update/delete path; every correction creates a new journal |
| No silent drops | Webhook inbox retains pending/fatal events; malformed imports enter quarantine |
| Internal vs Stripe statement | Reconciliation worker compares grouped ledger and external rows |
| Replay safety | Unique payment key, event ID, external ID, run key, run/shard key, and run/item key |
| Late/out-of-order events | Pending dependency replay and rank-guarded payment state |
| Deterministic millions/day design | Stable composite key, integer sums, bounded shards, scale generator |
| Create/run/get/adjust APIs | Implemented under `/api` with links and counts |
| Async shard flow | Scheduled worker claims persistent `(run_id, shard_id)` jobs |
| Selective retries | Only `PENDING` and `ERROR_RETRYABLE`; attempts, jitter, cap, `next_run_at` |
| Duplicate processor rows | Same ID/hash is a no-op; changed hash is a conflict |
| Partial-run resume | Completed shards stay complete; cursor is persisted per shard |
| Schema drift | Invalid rows stored with raw payload, row number, and error |
| Auto-adjust safety | Per-currency balance validation, approval threshold, daily amount cap |
| Fast balance | Projection updated in the journal transaction and point-read by indexed key |
| Partial failures | Capture ambiguity, later success, refund limits, disputes/reversals |
| FX and fees | Two balanced FX clearing journals and later fee journal |
| Explain every mismatch | Stable key, internal/external sums, delta, state, reason |
| Support investigation | Payment, possible duplicates, idempotency correlation, webhooks, journals, Stripe IDs |
| Metrics/logs/traces | Prometheus counters/timers, key-value logs, HTTP trace IDs |

Production geography and throughput remain deployment claims: run two app instances against PostgreSQL for the local multi-instance exercise, and use the scale lab before stating measured capacity.

