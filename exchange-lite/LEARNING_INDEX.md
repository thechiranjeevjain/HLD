# Learning Index

| Concept | Code | Docs | ADR | Interview Topic |
| --- | --- | --- | --- | --- |
| Control plane vs data plane | `engine`, `sidecar`, `cli` | `docs/MILESTONE-001-DESIGN.md`, `SYSTEM_MAP.md` | ADR-001, ADR-004 | Why operational APIs stay out of the hot path |
| Binary protocol | `common.protocol`, `engine.network.BinaryTcpServer` | `REQUEST_LIFECYCLE.md` | ADR-002 | Framing, versioning, correlation ids |
| Order book | `engine.core.OrderBook` | `CLASS_DEPENDENCY_MAP.md` | ADR-003 | Price-time priority and data structures |
| Risk engine | `engine.core.RiskEngine` | `PACKAGE_GUIDE.md` | None | Pre-trade risk checks |
| IPC | `common.ipc`, `engine.network` | `SYSTEM_MAP.md` | ADR-006 | Localhost TCP vs UDS |
| Runtime commands | `engine.runtime.RuntimeCommandRegistry` | `REQUEST_LIFECYCLE.md` | ADR-005 | Command pattern and operational safety |
| Observability | `metrics`, sidecar routes | `docs/operations/README.md` | ADR-008 | Health, metrics, heap, threads |
| Threading | `BinaryTcpServer`, `LocalhostTcpIpcServer`, `OrderBook` | `engine/README.md` | ADR-007 | Synchronization and sharding |
