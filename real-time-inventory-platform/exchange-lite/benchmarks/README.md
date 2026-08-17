# Benchmarks

Milestone 1 includes benchmark targets and methodology, not a final performance claim.

## Planned Measurements

- Binary protocol encode/decode latency.
- IPC localhost TCP latency.
- Unix Domain Socket IPC latency on Linux.
- REST sidecar latency.
- Matching throughput by symbol.
- Order processing latency.
- Risk evaluation latency.
- Memory allocation rate.
- Thread utilization.

## Methodology

Use a fixed seed for reproducibility and a separate random live-data mode for broader coverage. Report:

- JDK version.
- CPU and memory.
- OS.
- Warmup duration.
- Measurement duration.
- Percentiles: p50, p90, p99, p99.9.
- Allocation rate.
- GC pauses.

## Current Status

No benchmark numbers are claimed yet. Unit tests validate deterministic behavior, not load capacity.
