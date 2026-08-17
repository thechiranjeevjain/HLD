# Milestone 001 Principal Engineer Review

## Architecture

The most important boundary is correct: trading data-plane traffic is binary TCP and operational traffic goes through the sidecar and IPC. The sidecar does not construct engine internals, which keeps the control plane replaceable.

## Readability

The module split is clear. Shared contracts live in `common`, core matching state lives in `engine.core`, network adapters live in `engine.network`, and composition lives in `engine.runtime`.

## Maintainability

The command registry makes new operational APIs explicit. The main maintainability risk is hand-written JSON. It is acceptable in milestone 1 because payloads are controlled, but should be replaced with a structured JSON library before feature growth.

## Scalability

The synchronized order book is deterministic and easy to reason about. It will bottleneck for hot symbols. The next scalable step is one book owner per market, either via a dedicated event loop, actor, or virtual-thread-friendly queue.

## Performance

The protocol is bounded and compact. Cached platform threads are compatible with JDK 17 but not the final target. Move to Java 21 virtual threads or event loops after benchmark evidence.

## Naming

Names are mostly direct and interview-friendly. `TradingEngineRuntime` is intentionally a composition root, not a domain object.

## Coupling And Cohesion

The sidecar is well decoupled from engine internals. `TradingEngineRuntime` has several responsibilities but is acceptable as the runtime facade. If it grows, split inspection JSON, command handling, and lifecycle management.

## Testing

Covered:

- Binary protocol round trips and invalid magic.
- Order resting, matching, residuals, and cancel.
- Risk quantity and notional rejections.
- Runtime command registry.
- Sidecar route translation.
- Real sidecar-to-engine localhost TCP IPC integration.

Missing:

- Binary TCP server end-to-end order submission test.
- Concurrency tests for multi-session order flow.
- Fuzz tests for malformed frames.
- Load and benchmark harness.

## Operational Readiness

Docker, Compose, Kubernetes, Prometheus, Grafana, and runbooks exist. The deployment is suitable for local and interview demonstration, not production trading, because durable persistence, auth, TLS, and alert rules are not complete.

## Technical Debt

- Java 17 local validation instead of Java 21.
- Maven fallback in addition to intended Gradle build.
- Hand-written JSON.
- In-memory persistence.
- No real Micrometer registry yet; Prometheus text is implemented directly.

## Recommended Next Improvements

1. Install JDK 21 and add a Gradle wrapper.
2. Add an end-to-end binary TCP client integration test.
3. Add structured JSON serialization.
4. Add durable journal persistence and replay.
5. Add Micrometer registry integration and histogram metrics.
6. Add auth and audit to sidecar commands.
