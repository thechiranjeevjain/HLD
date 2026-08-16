# E-Commerce Backend Interview Guide

## Two-Minute Pitch

This service models a small commerce workflow: inventory is created, an order reserves stock, payment is simulated, and order events are published to Kafka. It is useful for interviews because it exposes consistency boundaries between inventory, orders, payments, and events.

## What To Emphasize

- Inventory reservation is the critical correctness point.
- Order status is a state machine, not just a string field.
- Payment simulation separates order creation from payment outcome.
- Kafka order events let other systems react without being in the checkout transaction.
- Database migrations keep inventory, order, line, and payment tables reproducible.

## Request Flow

1. Admin creates inventory for a SKU.
2. Customer posts an order with requested items.
3. `OrderService` validates inventory and reserves stock.
4. The order and lines are persisted in PostgreSQL.
5. `OrderEventPublisher` publishes order lifecycle messages.
6. Payment endpoint captures or declines a simulated payment and updates status.

## Tradeoffs

| Decision                      | Benefit                         | Cost                                                                       |
| ----------------------------- | ------------------------------- | -------------------------------------------------------------------------- |
| Synchronous stock reservation | Clear correctness in the demo   | Hot SKUs can become contention points                                      |
| Simulated payments            | Easy local workflow             | No real payment provider callbacks                                         |
| Kafka events                  | Decoupled downstream processing | Requires idempotent consumers in real systems                              |
| Single service                | Easy to explain end to end      | Real commerce systems often split catalog, order, payment, and fulfillment |

## FAQ

Q: What prevents overselling?
A: The service must treat reservation as the authoritative write and persist the resulting stock/order state transactionally.

Q: Why publish events after order changes?
A: Other services can react to order creation or payment outcomes without coupling to the checkout request.

Q: What would you add next?
A: idempotency keys, payment webhooks, outbox pattern, inventory locks, cancellation/refund flows, and customer auth.
