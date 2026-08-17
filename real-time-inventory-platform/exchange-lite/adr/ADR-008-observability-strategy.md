# ADR-008 Observability Strategy

## Context

Operators need health, runtime stats, heap, thread, and metrics visibility.

## Problem

Without a control-plane metrics and inspection surface, incidents become guesswork.

## Requirements

- Health checks.
- Runtime counters.
- Heap and thread inspection.
- Prometheus scrape point.

## Options Considered

- JMX only.
- Engine HTTP endpoints.
- Sidecar REST and metrics endpoints.

## Pros

Sidecar endpoints keep observability off the data plane and fit Kubernetes probes.

## Cons

Metrics depend on IPC availability.

## Decision

Expose inspection through sidecar routes backed by runtime IPC commands.

## Tradeoffs

If IPC fails, sidecar health reflects operational degradation even if matching still runs.

## Consequences

Prometheus scrapes the sidecar, not the engine.

## Future Revisions

Add Micrometer, histograms, exemplars, OpenTelemetry spans, and alert rules.
