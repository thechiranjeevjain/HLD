# Runbook

## Triage

1. Check `docker compose ps` and `/actuator/health`.
2. Search backend JSON/container logs by session ID and correlation ID.
3. Query the session and ordered steps; trust PostgreSQL over WebSocket history.
4. Inspect `stopReason`, failed tool output, token totals, durations, and `cleanupSucceeded`.

Clone failure: validate public GitHub HTTPS URL, branch, DNS, and rate limits. LLM timeout or malformed JSON: retry only if the budget and max-attempt policy permit it. Command timeout: capture partial output, kill the process/container, classify as retryable only for transient infrastructure. Failed build/test: preserve it as a visible tool result; do not call the session successful without explaining it. Backend restart: reconcile non-terminal sessions from checkpoints before leasing work. WebSocket disconnect: UI polls REST and reconnects. Cleanup failure: run `scripts/cleanup.ps1`, alert, and investigate before deleting workspace data.

Docker Desktop must be running. On Windows, ensure Linux containers and socket sharing work. Host Java 21 is optional for Compose but required for direct Gradle tests. Never respond to endpoint protection by adding exclusions.

Metrics: `agent.sessions.completed`, `agent.sessions.failed`, `agent.sandbox.startup`, `agent.tool.latency`, HTTP metrics, JVM metrics. Production alerts: failure ratio, stuck active sessions, sandbox startup p95, LLM latency/cost, cleanup failures, and database saturation.
