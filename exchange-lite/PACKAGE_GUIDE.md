# Package Guide

## `io.exchangelite.common.domain`

Immutable market-facing request and report records. These classes validate required fields and normalize markets.

## `io.exchangelite.common.protocol`

Binary frame and payload codecs. This package is the data-plane wire contract.

## `io.exchangelite.common.ipc`

Runtime command contracts and localhost TCP IPC client. This package is the control-plane wire contract.

## `io.exchangelite.common.metrics`

Atomic counters and snapshots.

## `io.exchangelite.engine.core`

Matching domain: order book, risk, market manager, sessions, persistence abstraction.

## `io.exchangelite.engine.network`

Binary TCP and IPC servers. Network code converts bytes to commands and reports but does not own trading rules.

## `io.exchangelite.engine.runtime`

Composition layer. It wires core components, exposes JSON inspection, and owns runtime command behavior.

## `io.exchangelite.sidecar`

HTTP control plane and IPC gateway.

## `io.exchangelite.cli`

Operator command-line client.
