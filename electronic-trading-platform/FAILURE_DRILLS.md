# Failure Drills

| Failure                      | Invariant                                               | Demo or test                                          |
| ---------------------------- | ------------------------------------------------------- | ----------------------------------------------------- |
| Stale or missing quote       | Reject before risk reservation and venue send           | `rejectsBeforeConnectivityWhenRiskFails` (limit case) |
| Duplicate client retry       | Return original OMS state; send once                    | `isIdempotentAndReconcilesUncertainVenueOutcome`      |
| Disconnect after venue write | Mark `UNKNOWN`, retain risk, reconcile from venue truth | same uncertainty test                                 |
| Process restart              | Rebuild orders and positions from ordered events        | `recoversOmsAndPositionsFromJournal`                  |
| Risk reject                  | Never call exchange connectivity                        | `rejectsBeforeConnectivityWhenRiskFails`              |

## Interview Follow-Ups

1. OMS database commits but event publication fails. Use a transactional outbox or make the journal the source of truth.
2. Risk instance dies with reservations. Partition ownership plus snapshot/replay restores reservations before the shard becomes ready.
3. Execution arrives before the synchronous acknowledgement. Correlate by venue/client IDs and permit legal out-of-order state transitions.
4. Market data is stale for one venue. Remove that venue from routing and fail closed where a current mark is mandatory.
5. Positions lag executions. Trading remains governed by real-time risk reservations; alert and replay the position consumer without losing order truth.
