# Production Guide

## Production Readiness Themes

The project is intentionally a learning platform, not a certified trading system. Use it to explain what is present and what a real company would harden.

## Reliability

Current:

- Health probes on every service.
- Timeouts on downstream HTTP clients.
- Fail-closed risk policy.
- Idempotent history consumer using `order_id`.
- Environment-driven configuration.

Production upgrades:

- Add circuit breakers with budgeted retries.
- Add Kafka dead-letter topics.
- Add outbox pattern for order DB plus Kafka publish consistency.
- Add consumer lag alerts.
- Add reconciliation jobs between orders and exposure events.
- Add graceful shutdown and readiness drain delay.

## Data Consistency

The biggest deliberate gap is this sequence:

1. `order-service` saves the order.
2. `order-service` publishes a Kafka event.

If the DB commit succeeds and Kafka publish fails, history and notification miss the event.

Production fix: transactional outbox.

```text
orders transaction:
  insert orders
  insert outbox_events

publisher loop:
  read unpublished outbox events
  publish to Kafka
  mark published
```

This converts a cross-system transaction into local durability plus retry.

## Security

Current:

- Non-root containers.
- Secrets separated from ConfigMaps.
- No credentials hardcoded in Java code.

Production upgrades:

- TLS everywhere.
- Authentication and authorization at gateway.
- mTLS between services.
- External secret manager.
- NetworkPolicies.
- Image signing and vulnerability scanning.
- Audit logs for risk-limit changes.

## Observability

Current:

- Spring Boot Actuator health and Prometheus metrics.
- Prometheus scrape config.
- Grafana starter dashboard.
- Structured enough logs for service behavior.

Production upgrades:

- Correlation IDs across gateway, order, risk, and history.
- Distributed tracing with OpenTelemetry.
- RED metrics: rate, errors, duration.
- USE metrics: utilization, saturation, errors.
- Kafka consumer lag dashboard.
- Database pool and slow query dashboards.
- Alert runbooks linked from alert definitions.

## Scalability

Current:

- Stateless services can scale horizontally.
- Kafka topic has three partitions.
- Kubernetes HPAs are defined.

Production upgrades:

- Partition order events by client or account based on ordering requirement.
- Separate read and write paths for exposure queries.
- Cache hot risk limits with explicit invalidation.
- Use connection pool sizing per service.
- Split heavy aggregation from request-time risk checks.

## Operational Best Practices

- Every alert needs a runbook.
- Every dashboard needs an owner.
- Every dependency needs timeout, retry, and fallback policy.
- Every async consumer needs idempotency.
- Every schema change needs migration and rollback thinking.
- Every deploy needs a health gate and rollback command.
- Every incident should produce a test, dashboard, or runbook improvement.
