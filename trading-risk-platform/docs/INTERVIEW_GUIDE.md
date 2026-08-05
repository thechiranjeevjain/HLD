# Trading Risk Platform Interview Guide

## Two-Minute Pitch

This project demonstrates pre-trade risk from two angles. The microservice path shows order, risk, history, notification, gateway, and PostgreSQL boundaries. The standalone pre-trade engine is the live demo: it accepts JSON and FIX orders, performs fail-closed risk checks, reserves exposure atomically, supports kill switches and circuit breakers, records audit events, and shows PnL.

## What To Emphasize

- Risk checks must happen before accepting an order.
- Atomic check plus reservation prevents race conditions.
- Kill switches and circuit breakers sit at the front of the hot path.
- Fail-closed behavior is safer than accepting orders with stale risk state.
- Audit events make decisions explainable after the fact.
- The microservice version demonstrates service boundaries; the standalone engine demonstrates the hot path.

## Request Flow

1. Client submits an order through JSON or FIX.
2. Input is normalized into an internal order model.
3. Kill switch and circuit breaker checks run first.
4. Limit, symbol, quantity, notional, market-data freshness, and exposure checks run.
5. Accepted orders reserve notional under an account lock.
6. The audit trail records the decision and reasons.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Standalone pre-trade engine | Fast, reliable interview demo | In-memory state is not durable |
| Microservice version | Shows production boundaries | Heavier local runtime |
| Fail-closed checks | Safer for risk | Can reject valid flow during dependency issues |
| Account-level lock | Prevents double reservation | Limits parallelism for one account |

## FAQ

Q: Why do kill switches run first?
A: They must reject immediately during emergency controls and should not depend on downstream state.

Q: Why is atomic reservation important?
A: Two individually valid orders can exceed the account limit if they check stale exposure concurrently.

Q: What would you add next?
A: persistent event log, replay, market-data feed integration, account hierarchy, auth, metrics alerts, and load testing.
