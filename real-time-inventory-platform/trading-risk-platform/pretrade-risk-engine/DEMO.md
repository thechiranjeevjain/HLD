# Ten-step demo

1. `docker compose up --build` starts the engine, management sidecar, Prometheus, and Grafana.
2. The `demo` profile drives `NEW â†’ ADDED â†’ STAGED â†’ COMMITTED` configuration.
3. It uses Java 21 virtual threads to concurrently publish 2,000 orders to `LocalDsfBus`.
4. Watch console `RuntimeView`: both PASS/BLOCK totals and recent decisions are shown.
5. The demo commits config version 2 with a tighter open-buy limit (the same operation is available through JWT-protected `POST /api/config`).
6. A final order crosses the limit and is BLOCKed; transaction rollback leaves open exposure unchanged.
7. `Invoke-RestMethod http://localhost:8091/runtime` inspects the sidecar view.
8. `mvn -pl pretrade-risk-engine -Dtest=PtrArchitectureTest#replayUsesLiveHandlerAndRestoresOnlyTail test` demonstrates snapshot + tail replay through the live `OrderHandler`.
9. The console prints standby takeover; `standbyTakesOverAndFormerPrimaryFailsClosed` proves the former primary cannot continue.
10. Open Prometheus and query `ptr_decisions_total`, `ptr_order_latency_seconds`, `ptr_queue_depth`, `ptr_pool_borrowed`, `ptr_breaches_total`, plus JVM GC/allocation metrics. Grafana can graph the same series.

To simulate process restart manually, stop and start `engine`; the focused recovery test is deterministic and demonstrates the exact state/replay invariant without relying on shell timing.

