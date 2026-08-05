# ADR-002 Binary Protocol

## Context

Trading traffic needs compact, deterministic framing.

## Problem

JSON over HTTP is easy to inspect but adds parsing overhead, larger payloads, and HTTP concerns to the hot path.

## Requirements

- Versioned frames.
- Correlation ids.
- Bounded payloads.
- Explicit message types.

## Options Considered

- JSON over HTTP.
- JSON over TCP.
- Custom binary frames.
- Protobuf over TCP.

## Pros

Custom frames make byte layout, validation, and allocations explicit.

## Cons

Custom protocols require careful compatibility testing and tooling.

## Decision

Use a small custom binary frame: magic, version, type, length, correlation id, payload.

## Tradeoffs

This is less feature-rich than Protobuf, but it makes protocol mechanics visible for learning and interviews.

## Consequences

Malformed traffic is rejected before domain decoding. Future schema evolution must preserve version compatibility.

## Future Revisions

Add checksums, schema versioning, replay tooling, and generated codecs.
