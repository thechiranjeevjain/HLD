# Glossary

- Ask: sell order resting on the book.
- Bid: buy order resting on the book.
- Control plane: operational APIs, inspection, health, config, metrics, and runtime commands.
- Data plane: latency-sensitive client trading path.
- IPC: inter-process communication between sidecar and engine.
- Limit order: order with a maximum buy price or minimum sell price.
- Market order: order that consumes available liquidity and never rests.
- Price-time priority: better price first, then older order first.
- Resting order: open order waiting in the book.
- Sidecar: companion process/container that owns management APIs.
- Tick: integer price unit used to avoid floating point money errors.
- UDS: Unix Domain Socket, a same-host IPC transport.
