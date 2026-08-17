# Failure handling

- Duplicate HTTP or Kafka input: unique transaction and idempotency keys return the original case; consumers must claim their key in the same transaction as side effects.
- Out-of-order input: compare aggregate version and event time; defer events with unmet prerequisites to a bounded retry topic.
- Invalid Kafka payload: schema validation, structured error, then dead-letter after three attempts.
- Redis failure: bypass cache/rate metadata and use PostgreSQL-backed correctness; alert on degraded latency.
- LLM timeout/tool failure/invalid output: bounded timeout, at most two retries for transient errors, persist failed call, return a deterministic-only investigation requiring human review.
- Concurrent case edits: JPA optimistic version rejects stale approval.
- Prompt injection: policy/customer text is untrusted data, never instructions; tools are code allowlisted and read-only; outputs are schema validated.
- Sensitive action: no LLM execution path exists; senior/admin authorization and rationale are mandatory.
- Outbox: retry pending rows with exponential backoff; publish at least once; consumers remain idempotent.
