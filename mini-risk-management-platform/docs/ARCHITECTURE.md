# Architecture

## System Goal

The platform accepts an order, checks it against pre-trade risk limits, persists the decision, emits an event, updates exposure history, and notifies operators.

The important learning point is service ownership:

- `order-service` owns the order.
- `risk-service` owns the decision.
- `history-service` owns exposure history.
- `notification-service` owns notification side effects.
- `api-gateway` owns external entry and hides internal service names from clients.

## Request Flow

1. Client sends `POST /api/orders` to `api-gateway`.
2. Gateway forwards to `order-service`.
3. `order-service` validates input and creates an order id.
4. `order-service` calls `risk-service`.
5. `risk-service` reads limits from Redis or PostgreSQL.
6. `risk-service` calls `history-service` for current exposure.
7. `risk-service` returns `ACCEPT` or `REJECT`.
8. `order-service` stores the order and publishes an `OrderEvent`.
9. `history-service` consumes accepted events and updates exposure history.
10. `notification-service` consumes all order events and logs a simulated email.

## Risk Rules

The simplified risk engine checks:

- Order quantity limit: a single order cannot exceed `max_order_quantity`.
- Position limit: projected absolute position cannot exceed `max_position_quantity`.
- Daily exposure limit: current daily exposure plus order notional cannot exceed `max_daily_exposure`.

The service fails closed if it cannot read history. In financial risk systems, allowing unsafe orders during dependency failure is often worse than rejecting safe orders.

## Data Ownership

| Service | Database | Tables | Reason |
| --- | --- | --- | --- |
| `order-service` | `orders` | `orders` | Order intake and final decision audit trail. |
| `risk-service` | `risk` | `risk_limits` | Risk configuration is owned by risk. |
| `history-service` | `history` | `exposure_events` | Exposure materialization from accepted orders. |

Each service owns its schema. Other services call APIs or consume events instead of reading foreign tables directly.

## Consistency Model

Order persistence is synchronous. Exposure history is asynchronous.

That means immediately after an accepted order, `GET /api/orders/{id}` shows the order, while `GET /api/exposures/{clientId}/{symbol}` may lag briefly until `history-service` consumes the Kafka event.

Production tradeoff:

- Synchronous updates provide stronger read-after-write consistency but tighter coupling and higher latency.
- Asynchronous events improve decoupling and resilience but require idempotency, lag monitoring, and reconciliation.

## Failure Policy

| Failure | Behavior |
| --- | --- |
| `risk-service` unavailable | `order-service` rejects the order with a fail-closed reason. |
| `history-service` unavailable during risk check | `risk-service` rejects the order. |
| Kafka unavailable after DB save | The current implementation can lose the event. The production guide explains the outbox pattern fix. |
| Redis unavailable | `risk-service` logs cache failure and falls back to PostgreSQL. |
| PostgreSQL unavailable | Affected service becomes unready and request paths fail. |

## Production Gaps Kept Visible

The code intentionally leaves some senior-level discussion points visible:

- No distributed transaction between order DB and Kafka. Discuss outbox pattern.
- Single-node Kafka/PostgreSQL in local lab. Discuss production replication.
- Simple gateway. Discuss authentication, rate limits, circuit breakers, request IDs, and WAF.
- No schema registry. Discuss event compatibility and versioning.
- No real email provider. Discuss retry queues and dead-letter topics.

