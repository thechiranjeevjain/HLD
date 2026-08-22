# Failure Drills

| Failure                            | Expected invariant                                | Demo or test                                           |
| ---------------------------------- | ------------------------------------------------- | ------------------------------------------------------ |
| Primary dies after journaled send  | Standby resumes at the next outbound sequence     | `suppressesDuplicateAndPersistsSequenceAcrossFailover` |
| Old primary is still alive         | Fencing rejects sends from the stale owner        | same failover test                                     |
| Inbound sequence jumps from 0 to 3 | Do not apply 3; request 1-2                       | `detectsSequenceGapAndIgnoresDuplicateInboundMessages` |
| Venue repeats an execution report  | Ignore the repeated sequence                      | same gap test                                          |
| Socket breaks after write          | Keep order `UNKNOWN`; never blindly resend        | `marksDisconnectAfterWriteAsUnknownUntilReconciled`    |
| Venue message rate is exhausted    | Reject/queue according to policy before wire send | `enforcesVenueThrottle`                                |

## Interview Follow-Ups

1. Kill the active process before and after journal append. Explain why write-ahead ordering changes the recovery result.
2. Partition the lease store from the active gateway. Fail closed because two active FIX sessions can corrupt sequence ownership.
3. Make drop-copy late. Keep capital reserved for `UNKNOWN` orders and alert on age/SLA.
4. Fill the throttle queue. Separate new orders from cancels and reserve emergency cancel capacity.
5. Lose the local state disk. Restore the replicated journal, then reconcile every unresolved order with venue truth.
