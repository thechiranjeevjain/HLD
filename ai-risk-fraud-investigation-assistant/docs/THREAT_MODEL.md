# Threat model

| Threat | Control |
| --- | --- |
| Stolen analyst credential | External IdP/MFA in production, short sessions, RBAC, audit |
| Analyst privilege escalation | Method/path authorization and server-side role checks |
| Prompt injection in evidence or policy | Treat retrieved text as data, fixed system policy, tool allowlist, structured schema |
| LLM exfiltrates PII | Mask prompts/tool results, minimize context, private endpoint, retention controls |
| LLM blocks a customer | Architecturally impossible: recommendations only; senior human approval required |
| Duplicate/replayed events | Unique keys, idempotent consumer transaction, audit |
| Audit tampering | Append-only permissions, export to immutable object storage/SIEM |
| Supply-chain compromise | Locked dependencies, CI scans, ECR scanning, signed images as next step |

Trust boundaries are browser/API, application/data stores, Kafka consumers, and external model provider. Secrets belong in a secret manager, never source control.
