# Class Dependency Map

```mermaid
classDiagram
    TradingEngineRuntime --> RiskEngine
    TradingEngineRuntime --> MatchingEngine
    TradingEngineRuntime --> SessionManager
    TradingEngineRuntime --> PersistenceStore
    TradingEngineRuntime --> ExchangeMetrics
    MatchingEngine --> OrderBook
    OrderBook --> Order
    OrderBook --> ExecutionReport
    BinaryTcpServer --> BinaryProtocol
    BinaryTcpServer --> TradingEngineRuntime
    RuntimeCommandRegistry --> TradingEngineRuntime
    SidecarHttpServer --> IpcGateway
    EngineIpcGateway --> LocalhostTcpIpcClient
    MarketConsole --> SidecarHttpServer
```

Important relationships:

- `RuntimeCommandRegistry` is the command boundary between IPC and engine runtime.
- `OrderBook` owns open orders and price levels. No sidecar or CLI class reaches it.
- `BinaryProtocol` is shared by client-facing data-plane code and tests.
- `ExchangeMetrics` is updated by runtime and network paths, then exposed through runtime command responses.
