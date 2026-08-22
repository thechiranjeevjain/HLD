# Architecture

## High-Level Design

```mermaid
flowchart LR
    C[Client / Algo] --> G[Gateway]
    G --> OMS[OMS]
    OMS --> R[Risk]
    MD[Market Data] --> R
    R --> SOR[Router]
    SOR --> EC[Connectivity]
    EC --> V[Venues]
    V --> EX[Executions]
    EX --> OMS
    EX --> P[Positions / P&L]
    OMS & R & EX --> J[(Event Journal)]
    J --> REC[Recovery]
```

The synchronous path ends only after the platform’s defined acknowledgement boundary. Fills are immutable facts that drive positions, while risk reservations bridge any projection lag.

## Low-Level Design

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant OMS
    participant Risk
    participant Venue
    participant Positions
    Client->>Gateway: NewOrder(clientOrderId)
    Gateway->>OMS: dedupe / create
    OMS->>Risk: check + reserve
    Risk-->>OMS: accepted
    OMS->>Venue: send through session owner
    Venue-->>OMS: fill or uncertain outcome
    OMS->>Positions: immutable fill
    Positions-->>Client: execution/position projection
```

`OmsService` owns lifecycle idempotency, `RiskService` owns reservations, `ConnectivityService` isolates the venue boundary, and the ordered `TradingEvent` journal rebuilds OMS and position projections.
