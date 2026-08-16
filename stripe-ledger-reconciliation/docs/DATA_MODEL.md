# Data Model and Accounting

```mermaid
erDiagram
  PAYMENTS ||--o{ LEDGER_ENTRIES : ref_id
  PAYMENTS ||--o{ WEBHOOK_EVENTS : payment_intent_id
  RECONCILIATION_RUNS ||--|{ RECONCILIATION_SHARDS : owns
  RECONCILIATION_RUNS ||--o{ RECONCILIATION_ITEMS : produces
  RECONCILIATION_RUNS ||--o{ ADJUSTMENT_REQUESTS : corrects
  EXTERNAL_IMPORTS ||--o{ QUARANTINED_EXTERNAL_ROWS : rejects
  RECONCILIATION_RUNS ||--o{ OUTBOX_EVENTS : emits
```

Ledger signs are from the platform's perspective. Every journal must sum to zero per currency. A EUR-to-USD settlement is not one mixed-currency journal:

| Journal           | Currency | Debit          | Credit            |
| ----------------- | -------- | -------------- | ----------------- |
| FX order clearing | EUR      | FX clearing    | Stripe receivable |
| FX settlement     | USD      | Processor cash | FX clearing       |

The applied rate and Stripe balance transaction are audit metadata. Later fees and rate corrections append their own journals.

The balance projection is derived state and may be rebuilt; ledger entries are accounting truth. An invariant job should compare projection totals with ledger sums before daily close.
