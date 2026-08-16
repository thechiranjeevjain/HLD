# Stripe Ledger Reconciliation Lab

An interview-ready, runnable system for idempotent payments, immutable double-entry accounting, at-least-once Stripe-style webhooks, fast balances, asynchronous sharded reconciliation, guarded adjustments, and deterministic failure simulation.

## Run it

Requirements: Java 21, Maven, Node/npm.

```bat
scripts\start-demo.cmd
```

Open `http://localhost:8095`, click **Reset & run demo**, then inspect either payment. The demo persists data in `.data/ledger.mv.db`, so idempotency survives restarts.

Validation:

```bat
scripts\test-all.cmd
```

## What is actually implemented

- Database-enforced idempotency with a unique key, canonical request hash, and stored response. Same key + same request returns the original result; same key + changed request returns HTTP 409.
- Immutable, balanced journals. Captures, refunds, disputes, fees, and adjustments append compensating entries; no ledger row is updated.
- At-least-once webhook inbox deduped by Stripe event ID. Unknown dependencies are retained as `PENDING_DEPENDENCY`; stale state transitions cannot move a payment backward.
- Orthogonal money events support many partial refunds, later disputes, and fee adjustments without overloading one payment status.
- Transactionally maintained `balance_projection`, avoiding a sum over the full ledger on every read.
- External rows imported once and deduped by `external_id`; changed payloads under the same ID are rejected.
- Deterministic reconciliation on stable Stripe object keys with `MATCHED` / `MISMATCHED` items and explicit reason codes.
- Idempotent reconciliation run key and upserted items, so a replay does not multiply results.
- Support explainer joining the payment, Stripe IDs, webhooks, journal IDs, and audit events.
- Four durable reconciliation shards claimed by a scheduled worker, with cursors, attempts, retry timestamps, capped exponential backoff, deterministic jitter, and fatal exhaustion.
- Transactional discrepancy outbox, schema-drift quarantine, immutable import manifests, approval-gated adjustments, daily adjustment limits, FX clearing legs, refund limits, and dispute reversals.
- Prometheus metrics, trace IDs in logs, deterministic scale data generation up to one million transactions, and concurrent idempotency verification.

## Core API

```text
POST /api/payments                         Idempotency-Key header required
POST /api/webhooks/stripe
GET  /api/support/payments/{paymentId}
GET  /api/ledger
GET  /api/balances
POST /api/external-transactions
POST /api/external-imports                    normalized file + quarantine flow
POST /api/reconciliations
POST /api/reconciliations/{id}/run         Idempotency-Key header required
GET  /api/reconciliations/{id}
POST /api/adjustments
POST /api/adjustments/{id}/approve            X-Approver header required
GET  /api/outbox
POST /api/demo/generate-scale                 up to 1,000,000 rows
POST /api/demo/reset-and-seed
```

Amounts are integer minor units. `10000 USD` means `$100.00`; floating point is never used for money.

## Deliberate scope boundaries

The default profile uses file-backed H2 for a zero-setup interview demo. Docker Compose switches to PostgreSQL. A real deployment would additionally use Stripe signature verification, a queue/outbox relay, row-level tenant security, secrets management, Kafka/PubSub, object storage for raw statements, and partitioned tables. Stripe itself is represented through stable external IDs and normalized balance transactions: no Stripe credentials are needed.

Docker files are provided, but container execution is not required for the local demo.

## Documentation map

- [Architecture and design decisions](docs/ARCHITECTURE.md)
- [Requirement-by-requirement evidence](docs/REQUIREMENTS_TRACEABILITY.md)
- [API contracts and examples](docs/API.md)
- [Data model and accounting invariants](docs/DATA_MODEL.md)
- [Interview walkthrough and follow-up answers](docs/INTERVIEW_GUIDE.md)
- [Failure injection drills](docs/FAILURE_DRILLS.md)
- [Operations, metrics, logs, tracing, and alerts](docs/OPERATIONS.md)
- [Local scale and multi-instance learning guide](docs/LOCAL_SCALE_LAB.md)
