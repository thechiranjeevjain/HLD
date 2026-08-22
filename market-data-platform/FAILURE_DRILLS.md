# Failure Drills

| Failure                                  | Expected behavior                                 | Demo or test                           |
| ---------------------------------------- | ------------------------------------------------- | -------------------------------------- |
| Packet 2 is missing and packet 3 arrives | Buffer 3, recover 2, publish 2 then 3             | `repairsGapBeforeApplyingLaterPacket`  |
| Duplicate packet arrives                 | Drop it by sequence before book mutation          | feed-handler duplicate counter         |
| Reduction crosses zero                   | Remove the order, never publish negative quantity | `reconstructsPriceLevelsAndReductions` |
| UI stops reading                         | Conflate to the newest snapshot                   | `isolatesSlowConsumersByPolicy`        |
| Raw client stops reading                 | Disconnect it rather than block the shard         | same slow-consumer test                |

## Interview Follow-Ups

1. Lose both A and B copies of a sequence. Pause the symbol/channel, request replay, and use a snapshot plus incremental catch-up if the replay window expired.
2. Restart a book shard. Load its latest snapshot and replay the normalized log from the snapshot offset before advertising readiness.
3. Detect a crossed book. Quarantine the affected symbol, compare A/B feeds, and expose data-quality metrics instead of silently repairing it.
4. Overload fan-out. Sample or conflate display clients; never let them exert backpressure on feed ingestion.
5. Rebalance symbols. Move at a sequence barrier and transfer snapshot plus next offset so one owner applies each event.
