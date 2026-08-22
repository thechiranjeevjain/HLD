# Architecture

## High-Level Design

```mermaid
flowchart LR
    OMS[OMS / Router] --> VR[Venue Router]
    VR --> F[FIX Session Shards]
    VR --> O[OUCH Session Shards]
    F & O --> V[Exchanges]
    F & O --> J[(Replicated Journal)]
    L[(Lease + Fencing)] --> F & O
    V --> D[Drop Copy]
    D --> R[Reconciler]
    R --> OMS
```

Sessions are the ordering and scaling boundary. The platform is active/active across many venue sessions, while each individual session has one fenced writer and a standby.

## Low-Level Design

```mermaid
stateDiagram-v2
    [*] --> RECEIVED
    RECEIVED --> SENT: journal + socket write
    SENT --> ACKNOWLEDGED: venue ack
    SENT --> REJECTED: venue reject
    SENT --> UNKNOWN: disconnect after write
    UNKNOWN --> ACKNOWLEDGED: drop copy / status says accepted
    UNKNOWN --> REJECTED: venue confirms absent/rejected
    ACKNOWLEDGED --> [*]
    REJECTED --> [*]
```

`VenueSession` owns sequences and order state, `TokenBucket` owns rate capacity, and `FencingLease` provides monotonically increasing ownership. A durable implementation replaces the list journal and in-process lease without changing these boundaries.
