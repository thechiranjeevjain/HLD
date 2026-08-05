# ADR-004 TCP vs REST Data Plane

## Context

Trading clients submit orders to the data plane.

## Problem

REST is convenient but brings HTTP parsing, headers, intermediaries, and ambiguous backpressure behavior.

## Requirements

- Stateful sessions.
- Compact request and response frames.
- Predictable tail latency.

## Options Considered

- REST endpoint in the engine.
- WebSocket.
- Binary TCP.

## Pros

Binary TCP gives direct control over framing and session lifecycle.

## Cons

It requires custom client tooling and protocol compatibility tests.

## Decision

Use binary TCP for the data plane.

## Tradeoffs

TCP is less convenient than REST but better represents exchange-style data-plane behavior.

## Consequences

HTTP is reserved for the sidecar control plane.

## Future Revisions

Add TLS, authentication, backpressure, heartbeat timeouts, and reconnect semantics.
