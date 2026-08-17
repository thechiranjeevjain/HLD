# ADR-006 IPC Strategy

## Context

The sidecar and engine run in the same deployment unit.

## Problem

The sidecar needs to inspect and command the engine without sharing memory or bypassing operational boundaries.

## Requirements

- Portable local development.
- Low latency same-host operation.
- Transport abstraction.

## Options Considered

- Localhost TCP.
- Unix Domain Socket.
- Shared memory.
- HTTP from sidecar to engine.

## Pros

Localhost TCP is portable. Unix Domain Sockets reduce network stack exposure and are common for same-host IPC on Unix-like systems.

## Cons

UDS portability is weaker on Windows. TCP has a larger attack surface if misconfigured.

## Decision

Implement both localhost TCP and UDS behind `EngineIpcServer`.

## Tradeoffs

Milestone 1 starts with localhost TCP by default because it works on this Windows workstation.

## Consequences

Production deployments can switch transport without changing sidecar routes.

## Future Revisions

Add client-side UDS support, mTLS for TCP, and benchmarks comparing IPC latency.
