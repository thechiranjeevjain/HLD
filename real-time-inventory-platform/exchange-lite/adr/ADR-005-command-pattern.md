# ADR-005 Command Pattern

## Context

Operational APIs need a stable way to invoke runtime actions.

## Problem

Direct sidecar calls into engine internals couple HTTP routes to engine classes and make IPC hard to swap.

## Requirements

- Route HTTP to named commands.
- Keep commands inspectable and testable.
- Support multiple IPC transports.

## Options Considered

- Direct Java method calls.
- Stringly typed handlers per endpoint.
- Typed runtime command registry.

## Pros

The registry centralizes available operations and keeps the sidecar thin.

## Cons

Commands must be curated and versioned.

## Decision

Use `RuntimeCommandType`, `RuntimeCommand`, and `RuntimeCommandRegistry`.

## Tradeoffs

This is a little more ceremony than direct calls, but it is the right control-plane boundary.

## Consequences

Adding a command requires a common enum, registry mapping, sidecar route, CLI mapping, and tests.

## Future Revisions

Add command authorization, audit metadata, idempotency keys, and command schemas.
