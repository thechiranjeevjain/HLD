# Security

## Current State

Milestone 1 validates boundaries but does not claim production security. The sidecar is the correct place to add authentication, authorization, rate limiting, and audit logging. The engine data plane currently trusts local deployment configuration and validates message structure and domain fields.

## Authentication Strategy

- Sidecar: require operator identity through mTLS, OAuth2, or a signed internal token.
- Data plane: require client certificate or session login before accepting order flow.
- IPC: use Unix Domain Sockets with filesystem permissions where possible, or localhost TCP with mTLS if TCP is exposed beyond loopback.

## Authorization Strategy

- Separate read-only inspection commands from mutating runtime commands.
- Restrict `SHUTDOWN`, `RELOAD_CONFIG`, and future market-state commands to privileged roles.
- Attach operator identity and command id to audit logs.

## Secrets Management

- Kubernetes secrets are represented by `kubernetes/secret.example.yaml`.
- Real secrets must be injected by a secret manager, not committed.
- Rotate operator tokens and TLS material.

## TLS Considerations

- Data TCP should add TLS before non-local use.
- Sidecar HTTP should run behind mTLS or service mesh policy.
- IPC over UDS avoids network TLS but still requires filesystem permission hardening.

## Input Validation

- Binary frames validate magic, version, type, length, and payload size.
- Domain records validate required fields, positive prices, and positive quantities.
- Risk checks reject excessive quantity and notional.

## Attack Surface

- Sidecar REST endpoints.
- Binary TCP data port.
- IPC port or socket.
- Container environment variables.
- Kubernetes service exposure.

## Future Improvements

- Add mTLS.
- Add command authorization.
- Add audit log sink.
- Add request rate limits.
- Add protocol fuzz tests.
- Add dependency scanning and container image scanning.
