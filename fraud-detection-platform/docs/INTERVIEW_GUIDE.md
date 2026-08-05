# Fraud Detection Platform Interview Guide

## Two-Minute Pitch

This service scores transaction events using deterministic rules, Redis velocity checks, Kafka ingestion, and PostgreSQL audit storage. It is a good backend interview project because fraud systems must make fast decisions while preserving evidence for investigation.

## What To Emphasize

- The same transaction schema can arrive through HTTP or Kafka.
- `transactionId` is the idempotency boundary.
- Velocity checks catch repeated behavior across time windows.
- Rule output is stored so a decision can be explained later.
- The design favors explainability over black-box scoring.

## Request Flow

1. Transaction arrives through `POST /api/events/transactions` or Kafka.
2. `FraudScoringService` normalizes and scores the event.
3. `VelocityService` checks short-window activity in Redis.
4. `FraudRuleEngine` emits rule evaluations and a risk level.
5. PostgreSQL stores the decision for audit and lookup.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Rule engine | Easy to explain and test | Less adaptive than ML |
| Redis velocity state | Fast repeated-activity checks | Requires expiry and outage policy |
| PostgreSQL audit | Decisions are explainable later | Additional write latency |
| Kafka listener | Async ingestion path | Requires idempotency and replay handling |

## FAQ

Q: Why rules instead of ML?
A: Rules are transparent and deterministic, which is useful for learning, audits, and interviews. ML can be a future scoring input.

Q: What happens if the same transaction arrives twice?
A: The design should treat `transactionId` as the idempotency key so duplicates do not create conflicting decisions.

Q: What would you add next?
A: model scoring, feature store, alert queues, case management, replay tools, and decision versioning.
