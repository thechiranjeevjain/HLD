# API Guide

All monetary values are signed integer minor units. Dates use ISO `YYYY-MM-DD`.

## Create an idempotent payment

```http
POST /api/payments
Idempotency-Key: checkout-order-42
Content-Type: application/json

{"customerId":"cus_42","orderId":"order_42","amount":10000,"currency":"EUR","settlementCurrency":"USD","simulateCaptureFailure":false}
```

Reusing the key with the identical payload returns the stored response. Reusing it with another payload returns `409`.

## Stripe webhook

```json
{"eventId":"evt_42","type":"balance.available","paymentIntentId":"pi_demo_x","stripeObjectId":"txn_42","amount":10000,"currency":"EUR","settlementAmount":10850,"settlementCurrency":"USD","fxRate":"1.085"}
```

Supported event types: `payment_intent.succeeded`, `charge.refunded`, `charge.dispute.created`, `charge.dispute.closed`, `fee.adjusted`, and `balance.available`.

## Import a normalized statement

```http
POST /api/external-imports

{"fileName":"stripe-2026-08-16.json","schemaVersion":"v1","rows":[{"externalId":"txn_1","matchKey":"pi_1","amount":10000,"currency":"USD","status":"AVAILABLE"}]}
```

Good rows are committed; bad rows are visible at `GET /api/external-imports/quarantine`. Replaying the identical file returns `DUPLICATE_FILE`.

## Reconcile

```http
POST /api/reconciliations
{"source":"STRIPE","rangeStart":"2026-08-16","rangeEnd":"2026-08-16"}

POST /api/reconciliations/{id}/run
Idempotency-Key: stripe-close-2026-08-16

GET /api/reconciliations/{id}
```

The run call queues durable shards. Poll the GET resource until `COMPLETED` or `FAILED`.

## Compensating adjustment

```json
{"reconciliationId":"rec_x","reason":"approved processor fee correction","entries":[{"accountId":"processor_fees","currency":"USD","amount":15000,"matchKey":"txn_1"},{"accountId":"reconciliation_suspense","currency":"USD","amount":-15000,"matchKey":"txn_1"}]}
```

Large requests return `PENDING_APPROVAL`; approve with `POST /api/adjustments/{id}/approve` and header `X-Approver: senior-ops`. Approval is replay-safe.

