# Failure Drills

| Failure                             | Expected behavior                                   | Demo or test                                         |
| ----------------------------------- | --------------------------------------------------- | ---------------------------------------------------- |
| One downstream throws               | Return its typed error plus the other results       | `returnsAndPersistsPartialResultOnTimeoutAndFailure` |
| One downstream exceeds deadline     | Return timeout without waiting for its full latency | same partial-result test                             |
| Transient failure succeeds on retry | Record attempt count and complete aggregate         | `retriesTransientFailureAndPersistsIdempotently`     |
| Client repeats `requestId`          | Return the stored result; do not fan out again      | same idempotency test                                |
| All downstreams take 150 ms         | End-to-end time is near max latency, not sum        | `invokesThreeServicesConcurrently`                   |

## Interview Follow-Ups

1. A downstream becomes slow. Open its circuit after a rolling threshold and keep its concurrency pool isolated.
2. Retries amplify an outage. Retry only transient/idempotent operations, add jitter, respect the remaining deadline, and enforce a retry budget.
3. Persistence fails after calls complete. Return a retryable server error or durably enqueue the aggregate; do not claim success without the required write.
4. Two instances race on one request ID. Use a database unique key and return the winning row.
5. Partial results are unacceptable for a caller. Make response policy explicit per endpoint: fail-fast, best-effort, or cached-stale fallback.
