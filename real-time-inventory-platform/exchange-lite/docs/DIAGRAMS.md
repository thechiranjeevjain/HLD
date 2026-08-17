# ExchangeLite Diagrams

This file gives the fastest diagram entry point. Deeper maps are in `SYSTEM_MAP.md`, `CLASS_DEPENDENCY_MAP.md`, `REQUEST_LIFECYCLE.md`, and the ADRs.

## System View

```mermaid
flowchart LR
    Client["Trading client"] -->|"binary TCP"| Engine["Trading Engine"]
    Engine --> Risk["Risk Engine"]
    Engine --> Book["Order Book"]
    Engine --> Metrics["Metrics"]
    CLI["Operator CLI"] -->|"HTTP"| Sidecar["Management Sidecar"]
    Sidecar -->|"IPC"| IPC["Engine IPC Server"]
    IPC --> Engine
    Prom["Prometheus"] --> Sidecar
```

## Order Flow

```mermaid
sequenceDiagram
    participant C as Trading client
    participant TCP as Binary TCP server
    participant E as Engine runtime
    participant R as RiskEngine
    participant B as OrderBook
    C->>TCP: framed order
    TCP->>E: decoded request
    E->>R: risk check
    alt rejected
        E-->>TCP: reject report
    else accepted
        E->>B: match or rest order
        B-->>E: execution reports
        E-->>TCP: reports
    end
    TCP-->>C: encoded response
```

## Control Plane Flow

```mermaid
flowchart LR
    CLI["mc command"] --> Sidecar["HTTP sidecar"]
    Sidecar --> Gateway["IpcGateway"]
    Gateway --> Registry["RuntimeCommandRegistry"]
    Registry --> Engine["TradingEngineRuntime"]
    Engine --> Response["stats / health / snapshot"]
```
