# Request Lifecycle

## New Order

1. Client encodes `OrderRequest` using `BinaryProtocol.encodeOrderRequest`.
2. Client wraps payload in a `FramedMessage` with type `NEW_ORDER`.
3. `BinaryTcpServer` reads the fixed header and bounded payload.
4. `BinaryProtocol.decodeFrame` validates magic, version, message type, length, and correlation id.
5. `TradingEngineRuntime.submitOrder` checks market state.
6. `RiskEngine.evaluate` enforces max quantity, max notional, and blocked account checks.
7. `MatchingEngine.submit` routes to the market `OrderBook`.
8. `OrderBook.submit` matches against the best opposite price, FIFO within each price.
9. Runtime records metrics and appends the execution report to `PersistenceStore`.
10. Server returns an `EXECUTION_REPORT` frame with JSON report payload.

## Operator Stats

1. Operator runs `mc stats`.
2. CLI sends `GET /stats` to the sidecar.
3. `SidecarHttpServer` maps route to `RuntimeCommandType.STATS`.
4. `EngineIpcGateway` sends a line-oriented IPC command to the engine.
5. `RuntimeCommandRegistry` returns `TradingEngineRuntime.statsJson`.
6. Sidecar returns the runtime response body as HTTP JSON.

## Failure Handling

- Invalid trading frame: return `REJECT` frame.
- Risk failure: return `ExecutionReport` with `REJECTED`.
- Engine IPC unavailable: sidecar returns `502`.
- Unknown sidecar route: sidecar returns `404`.
