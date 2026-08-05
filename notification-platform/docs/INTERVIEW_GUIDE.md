# Notification Platform Interview Guide

## Two-Minute Pitch

This service models a notification pipeline with multiple channels, retry backoff, manual retry, and dead-letter records. It is useful for backend interviews because notification delivery is naturally unreliable and forces explicit failure handling.

## What To Emphasize

- API creation is separate from actual delivery.
- Email, SMS, and push are routed through channel-specific gateways.
- Failures retry with bounded attempts.
- Dead-letter records preserve failed work for inspection and recovery.
- Manual retry is an operator workflow, not a hidden automatic loop.

## Request Flow

1. Client posts a notification request.
2. `NotificationService` persists the notification as pending.
3. Scheduled worker picks eligible notifications.
4. `DeliveryRouter` selects email, SMS, or push gateway.
5. Success marks the notification delivered.
6. Repeated failure creates a dead-letter record.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Scheduled worker | Simple local asynchronous behavior | Polling delay and less precise scheduling |
| Channel router | Clear extension point for new channels | Each gateway needs channel-specific failure mapping |
| DLQ table | Inspectable failure history | Operators must decide when and how to replay |
| Simulated gateways | Deterministic local tests | No real provider reliability behavior |

## FAQ

Q: Why not deliver synchronously in the API request?
A: Provider latency and failures should not hold the client request open. Persisting first lets the system retry.

Q: Why is idempotency important?
A: Retries can duplicate provider calls. Real delivery should include dedupe keys or provider idempotency tokens.

Q: What would you add next?
A: exponential backoff, provider webhooks, templates, user preferences, idempotency keys, and metrics per channel.
