# System Map

```mermaid
flowchart LR
    Client["Trading client"] -->|"binary TCP"| Binary["BinaryTcpServer"]
    Binary --> Runtime["TradingEngineRuntime"]
    Runtime --> Risk["RiskEngine"]
    Runtime --> Match["MatchingEngine"]
    Match --> Book["OrderBook"]
    Runtime --> Store["PersistenceStore"]
    Runtime --> Metrics["ExchangeMetrics"]
    Operator["Operator"] --> CLI["MarketConsole"]
    CLI -->|"HTTP"| Sidecar["SidecarHttpServer"]
    Sidecar --> Gateway["EngineIpcGateway"]
    Gateway -->|"RuntimeCommand"| Ipc["EngineIpcServer"]
    Ipc --> Registry["RuntimeCommandRegistry"]
    Registry --> Runtime
```

## Component Responsibilities

- `common`: shared contracts that must stay stable across processes.
- `engine`: owns trading state, matching, risk, binary sessions, IPC command execution.
- `sidecar`: owns HTTP, operational translation, and future auth/audit.
- `cli`: human operator interface.
- `docker`, `kubernetes`, `prometheus`, `grafana`: operational packaging.

## Failure Boundaries

- Sidecar crash: trading can continue, but operations are degraded.
- Engine crash: trading and control commands fail; Kubernetes restarts the pod.
- IPC timeout: sidecar returns `502`; alert on sustained failures.
- Binary protocol corruption: reject at frame boundary before domain mutation.
