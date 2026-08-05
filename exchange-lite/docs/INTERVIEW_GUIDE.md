# ExchangeLite Interview Guide

## Two-Minute Pitch

ExchangeLite is a small exchange backend that separates the latency-sensitive data plane from the operational control plane. Clients submit binary TCP orders to the engine. Operators use the sidecar and CLI for health, stats, heap, threads, orders, markets, and shutdown. The engine owns matching, risk, sessions, metrics, and IPC.

## What To Emphasize

- Data-plane trading traffic does not share HTTP routes with operational control.
- The sidecar translates HTTP into IPC commands instead of reaching into engine internals directly.
- The binary protocol makes framing, validation, and latency tradeoffs visible.
- The matching engine demonstrates price-time priority.
- Risk checks happen before orders enter the book.
- The CLI provides a demoable operator workflow.

## Best Reading Order

1. `README.md`
2. `SYSTEM_MAP.md`
3. `REQUEST_LIFECYCLE.md`
4. `CHEATSHEET.md`
5. `adr/`
6. `docs/operations/README.md`

## FAQ

Q: Why split data plane and control plane?
A: Operational HTTP, metrics, and inspection must not contaminate the hot matching path.

Q: Why a binary protocol?
A: It exposes framing, compact encoding, and deterministic parsing. REST is easier, but less representative of latency-sensitive data paths.

Q: What is the first production gap?
A: durable persistence, authentication, TLS, and real benchmark evidence.
