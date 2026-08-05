# ADR-003 TreeMap Price Levels

## Context

An order book must quickly locate the best bid and best ask while preserving FIFO order at each price level.

## Problem

Hash maps give fast price lookup but do not naturally expose the best price.

## Requirements

- Best bid and best ask in deterministic order.
- FIFO within each price.
- Simple inspection for learning and debugging.

## Options Considered

- `HashMap` plus heap.
- `TreeMap` price levels.
- Array indexed by price tick.
- Custom skip list.

## Pros

`TreeMap` provides ordered prices and `ArrayDeque` preserves FIFO.

## Cons

Best price operations are O(log n) for updates and can allocate more than specialized structures.

## Decision

Use `TreeMap<Long, ArrayDeque<Order>>` for milestone 1.

## Tradeoffs

Simplicity and explainability win for the first milestone. A production exchange may use preallocated arrays or custom intrusive queues for known tick ranges.

## Consequences

The code is easy to review and deterministic under tests.

## Future Revisions

Benchmark against array price ladders, off-heap structures, and event-loop-owned books.
