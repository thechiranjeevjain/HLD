# Production Readiness — Exchange Connectivity Platform

Current target: **I1 — interview-ready**. The simulator proves session-state decisions, not exchange certification. Read the shared [readiness definitions](../docs/READINESS_LEVELS.md).

## Current Evidence

| Area                               | Status                | Evidence                                                                  |
| ---------------------------------- | --------------------- | ------------------------------------------------------------------------- |
| Outbound/inbound sequence recovery | `VERIFIED_LOCAL`      | Sequence checkpoint restoration and resend/duplicate tests                |
| Venue throttling                   | `VERIFIED_LOCAL`      | Deterministic token-bucket test                                           |
| Duplicate order intent             | `VERIFIED_LOCAL`      | `clientOrderId` suppression test                                          |
| Active/standby fencing             | `VERIFIED_LOCAL`      | Standby promotion rejects stale-primary sends                             |
| Disconnect after write             | `VERIFIED_LOCAL`      | Explicit `UNKNOWN` state and reconciliation test/demo                     |
| FIX/OUCH wire implementation       | `DESIGNED_ONLY`       | Protocol enum and session boundary model semantics, not production codecs |
| Venue conformance                  | `EXTERNAL_DEPENDENCY` | Requires exchange certification environment and credentials               |

## Production Gaps

| Workstream          | Production target                                                 | Required proof                                                                   |
| ------------------- | ----------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Protocol engines    | Certified FIX dictionaries and OUCH binary codecs                 | Conformance suites, negative protocol cases, sequence reset/replay certification |
| Networking          | TLS/mTLS, tuned TCP, heartbeat/test-request, reconnect policy     | Packet capture, latency, disconnect, half-open, and network-partition tests      |
| Durability          | Replicated write-ahead raw/business journal                       | Crash-before/after-write tests and exact outbound sequence restoration           |
| Ownership           | Strong lease/fencing integrated with session logon                | Split-brain tests proving only one process can send                              |
| Reconciliation      | Drop-copy/status-query ingestion and operator workflow            | Lost ack, late fill, duplicate fill, missing drop-copy, and aged-unknown drills  |
| Throttling          | Venue-specific limits with cancel priority and emergency capacity | Burst tests, queue-deadline tests, and mass-cancel drills                        |
| Security/compliance | Protected credentials, RBAC, audit, clock synchronization         | Security review, access rotation, audit retention, time-source failure tests     |
| Operations          | Session dashboards, alerts, runbooks, certification-safe releases | Failover game days and venue-coordinated rollback evidence                       |

## Keep Outside the 60-Minute Answer

Do not design the exchange matching engine or list every FIX tag. Focus on session affinity, sender/target sequences, resend and dedupe, throttle policy, fenced failover, and the uncertain-order state. Mention codecs, TLS, certification, and replicated storage as production adapters.

## Promotion Path

1. P0: define venue/session inventory, delivery contract, acknowledgement boundary, RTO/RPO, and certification plan.
2. P1: integrate one real FIX test venue, durable journal, drop copy, and strongly consistent ownership.
3. P2: certify replay/reset behavior and run load, disconnect, split-brain, and failover tests.
4. P3: complete credential security, operational readiness, venue change controls, and controlled rollout.
