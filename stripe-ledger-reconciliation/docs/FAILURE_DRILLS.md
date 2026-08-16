# Failure Drills

| Drill | Expected behavior | Evidence |
|---|---|---|
| Send one payment request twice | Same payment ID; one journal | `idempotency_requests`, integration test |
| Reuse key with changed amount | HTTP 409; no second payment | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST` |
| Deliver webhook twice | Second delivery is `DUPLICATE_IGNORED` | `webhook_events.event_id` uniqueness |
| Capture times out, success arrives later | State advances and one capture journal appears | Demo payment `order_1002` |
| Old event arrives after capture | Retained but state cannot regress | rank-guarded SQL update |
| Processor row duplicated | Same payload is a no-op | `external_transactions.external_id` |
| Processor ID reused with changed row | Reject and investigate | `EXTERNAL_ID_REUSED_WITH_DIFFERENT_PAYLOAD` |
| Reconciliation run retried | Identical items, no multiplication | run key + `(run_id, match_key)` key |
| Provider 429 / object-store timeout | Mark only its shard `ERROR_RETRYABLE`; persist attempt, backoff and jitter | `reconciliation_shards` + retry metric |
| Provider schema drift | Keep good rows and quarantine malformed raw rows with a fatal reason | `/api/external-imports/quarantine` |
| Worker dies halfway | Completed shards remain complete; unfinished shards are reclaimable from their cursor | durable shard rows |
| Auto-adjust spikes | Reject the daily amount cap; require named approval above threshold | `adjustment_requests` + outbox |

Recommended alerts: mismatch-rate spike, no run progress for 15 minutes, retry storm, processor 429 rate, pending webhook age, unbalanced-journal invariant failure, and adjustment volume above policy.
