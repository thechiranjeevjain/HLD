# Interview Guide

## A strong 90-second opening

“I separate payment orchestration, accounting truth, and processor reconciliation. Requests first acquire a durable idempotency key in the same transaction as the payment and initial journal. Stripe webhooks land in an inbox because delivery is duplicated, reordered, and delayed; event IDs dedupe transport while rank-guarded transitions prevent regression. Money effects are immutable balanced journals, so refunds, disputes, FX, and fees append facts. A transactional balance projection gives fast reads. Daily jobs compare frozen internal and Stripe aggregates using stable object keys, upsert deterministic reason-coded items, and resume per shard. We never claim exactly-once—we make every boundary replay-safe.”

## Walkthrough

1. Click **Reset & run demo**. One normal capture and one capture-timeout are created.
2. Explain that the timeout is ambiguous: the later success webhook advances it and posts the capture exactly once.
3. Inspect a payment. Show payment intent, charge/refund IDs, journals, webhook event ID, and audit correlation.
4. Show balances: these are a projection, not an expensive ledger sum.
5. Show the four durable shards and reconciliation. `INTERNAL_ONLY`, `EXTERNAL_ONLY`, and `AMOUNT_DIFFERENCE` are evidence, not generic “failed” states.
6. Explain that fixes are reviewed compensating entries, never edits.

## Questions and trade-offs

**Why not Redis for idempotency?** A cache can evict and regional caches race. Use the financial system of record's uniqueness boundary. Redis may accelerate reads, never decide ownership.

**What if the first caller times out after commit?** The retry supplies the same key and receives the stored response. A different payload is rejected.

**What if Stripe succeeds but the local transaction fails?** No local success is asserted. The webhook/import later supplies external evidence, producing either the journal or an `EXTERNAL_ONLY` discrepancy. Never synthesize success from a client timeout.

**How do many partial refunds work?** Each Stripe refund ID dedupes one independent refund journal. Sum refunds to derive refundable remainder; enforce it under a payment lock.

**How do FX and fee changes work?** Record order currency, settlement currency, processor fee, and FX clearing legs separately. A later fee adjustment appends a new journal. Reconcile against Stripe balance transaction IDs, not only payment intents.

**Can the balance projection drift?** Yes, so run an invariant job comparing projection balances with ledger sums. Rebuild into a new version and atomically swap; do not mutate ledger history.

**What is the scaling unit?** Merchant + date + currency, or Stripe balance transaction/payment intent where available. Hash keys into bounded jobs; avoid one job per transaction.

**Which scale claims are proven locally?** The database uniqueness and deterministic shard protocols are executable, including 16 concurrent callers and generated datasets up to one million transactions. Geographic latency, failover RPO/RTO, and sustained production throughput require a deployed benchmark and are not claimed.

**Why a database-backed queue?** It keeps this interview demo self-contained and proves leasing, attempts, cursor resume, and replay safety. At higher throughput the same job contract can move to Kafka/SQS while the database remains the result/idempotency authority.

## Invariants

- Every journal sums to zero within one currency.
- Every external money effect has a stable Stripe object ID.
- One idempotency key maps to one request hash and stored response.
- One Stripe event ID is processed at most once locally.
- Payment state rank never decreases.
- Every run item is reproducible from its frozen inputs.
- Every adjustment references a reconciliation and a human-readable reason.
