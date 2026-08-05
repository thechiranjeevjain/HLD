# ADR-007 Threading Model

## Context

The engine handles TCP sessions, IPC sessions, and per-market matching state.

## Problem

Threading must avoid corrupting order-book state while staying easy to reason about.

## Requirements

- Deterministic per-market behavior.
- Clear failure isolation.
- Compatibility with the current JDK 17 workstation.

## Options Considered

- Thread per connection with synchronized books.
- Virtual thread per connection.
- One event loop per market.
- Disruptor-style ring buffer.

## Pros

Thread per connection plus synchronized books is clear and runnable now.

## Cons

It can contend under heavy load.

## Decision

Use platform worker threads and synchronized order books for milestone 1.

## Tradeoffs

The design favors correctness and explainability over maximum throughput.

## Consequences

Future performance work can replace the worker model without changing public contracts.

## Future Revisions

Move to Java 21 virtual threads or a per-market event loop after benchmark evidence.
