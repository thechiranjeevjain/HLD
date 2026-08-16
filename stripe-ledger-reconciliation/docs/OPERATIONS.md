# Operations and Observability

Prometheus output is available at `/actuator/prometheus`. Important series include payment creation, webhook outcome, external row outcome, reconciliation run status, shard duration, and retry outcome. Standard HTTP server latency is included by Actuator.

Logs use stable `key=value` fields such as `run_id`, `shard_id`, `payment_id`, `event_id`, `stripe_object_id`, and `idempotency_key`. Micrometer tracing adds `traceId` and `spanId` to request logs.

Suggested alerts:

- mismatch ratio above the merchant's rolling baseline;
- queued/running reconciliation with no completed shard for 15 minutes;
- retry counter spike or fatal shard;
- provider 429 rate above 5% for five minutes;
- oldest pending-dependency webhook over ten minutes;
- quarantine count non-zero;
- adjustment amount/count over policy;
- projection-versus-ledger invariant failure.

The outbox is intentionally inspectable at `/api/outbox`. A production relay claims pending rows, publishes them, and marks publication metadata; consumers remain idempotent.
