# Threat model

| Threat | Control |
| --- | --- |
| Stolen analyst credential | External IdP/MFA in production, short sessions, RBAC, audit |
| Analyst privilege escalation | Method/path authorization and server-side role checks |
| Direct prompt injection | Inspect untrusted input before orchestration; return 403 and audit `PROMPT_INJECTION_BLOCKED` |
| Prompt injection in evidence or policy | Inspect retrieved text as untrusted data; never promote it to instructions |
| Unauthorized model tool request | Exact-name read-tool allowlist; deny and audit every unknown or write-capable tool |
| LLM exfiltrates secrets or PII | Mask tool results and audit details; reject secret-bearing output at the DLP boundary |
| LLM blocks a customer | Architecturally impossible: recommendations only; senior human approval required |
| Duplicate/replayed events | Unique keys, idempotent consumer transaction, audit |
| Denial audit lost during transaction rollback | Security audit uses an independent transaction; production exports append-only records to immutable storage/SIEM |
| Supply-chain compromise | Locked dependencies, CI scans, ECR scanning, signed images as next step |

Trust boundaries are browser/API, application/data stores, Kafka consumers, and external model provider. Secrets belong in a secret manager, never source control.

The executable evidence for the four model-facing threats is `AdversarialAgentSecurityTest`; payloads and expected audit events are catalogued in [ADVERSARIAL_SECURITY.md](ADVERSARIAL_SECURITY.md).
