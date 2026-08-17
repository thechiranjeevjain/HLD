# ADR-001 Sidecar Pattern

## Context

The trading engine must keep low-latency binary traffic separate from operational HTTP traffic.

## Problem

If HTTP management endpoints run inside the hot data-plane process, operator traffic, JSON serialization, auth filters, and debugging calls can interfere with matching latency.

## Requirements

- Keep the data plane free of HTTP.
- Allow operators to inspect and command the runtime.
- Preserve a clean security and audit boundary.

## Options Considered

- Embed HTTP directly in the engine.
- Run a separate management sidecar.
- Expose only local shell scripts.

## Pros

Sidecar gives clean ownership, independent HTTP dependencies, and shared-pod localhost reachability.

## Cons

Adds another process, IPC, and failure mode.

## Decision

Use a management sidecar. The sidecar translates REST calls into IPC commands.

## Tradeoffs

The extra hop costs latency, but operator traffic is not in the order path. Operational isolation matters more than raw command latency.

## Consequences

The CLI and Prometheus target the sidecar. Engine code remains focused on trading state and binary sessions.

## Future Revisions

Add sidecar authentication, authorization, audit logging, rate limits, and OpenTelemetry spans.
