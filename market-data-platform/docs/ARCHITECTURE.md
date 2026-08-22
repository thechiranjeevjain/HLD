# Architecture

## High-Level Design

```mermaid
flowchart LR
    AB[Multicast A/B] --> FH[Feed Handlers]
    FH --> SQ[Sequencer + Gap Repair]
    RP[Replay / Snapshot] --> SQ
    SQ --> N[Normalizer]
    N --> SP[Symbol Partitioner]
    SP --> BS[Book Shards]
    BS --> L[(Normalized Log)]
    BS --> FO[Fan-Out Tier]
    FO --> ST[Strategies]
    FO --> UI[UI / Analytics]
```

Venue/channel order is repaired before normalization. After normalization, the stream is repartitioned so one shard owns each symbol book.

## Low-Level Design

```mermaid
sequenceDiagram
    participant Feed
    participant Sequencer
    participant Replay
    participant Book
    participant FanOut
    Feed->>Sequencer: packet seq=3 (expected=2)
    Sequencer->>Sequencer: buffer seq=3
    Sequencer->>Replay: request seq=2
    Replay-->>Sequencer: packet seq=2
    Sequencer->>Book: normalized seq=2
    Sequencer->>Book: normalized seq=3
    Book->>FanOut: snapshot/update
    FanOut-->>FanOut: conflate or disconnect slow client
```

`SequencedFeedHandler` emits only a contiguous prefix, `OrderBook` owns order and price-level state, and every `Subscriber` has an independent bounded queue and overflow policy.
