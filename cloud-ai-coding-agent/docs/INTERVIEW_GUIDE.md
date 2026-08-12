# Cloud AI Coding Agent Interview Guide

## Two-Minute Pitch

This project is an interview-sized cloud coding agent. A React UI creates an agent session, a Spring Boot backend stores durable session state, a provider-neutral LLM boundary plans work, and tool calls run inside disposable non-root Docker sandboxes. WebSockets stream progress, while PostgreSQL remains the source of truth.

## What To Emphasize

- The agent loop is auditable: plan, tool call, result, diff, and status are persisted.
- The LLM is behind an adapter, so fake and OpenAI-backed planners share the same boundary.
- The sandbox does not receive the host Docker socket.
- Cancellation and retry are explicit session operations.
- WebSocket events are transient; durable state is read back from the API.
- GitHub PR creation is intentionally an adapter seam, not fake production behavior.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Modular monolith | Easy local demo and clear boundaries | Not independently scalable by component |
| Fake planner by default | Repeatable safe demo | Does not prove general coding intelligence |
| Docker sandbox | Stronger execution isolation than local shell | Requires Docker Desktop locally |
| PostgreSQL session store | Recoverable session history | More setup than in-memory state |
| WebSocket streaming | Good operator experience | Needs replay from database after disconnect |

## FAQ

Q: What is the backend interview story?
A: workflow orchestration, durable state, sandboxing, streaming updates, cancellation, retries, and provider-neutral AI integration.

Q: Is the local fake planner enough?
A: It is enough to demonstrate the control plane safely. Real model quality is behind the `LlmClient` adapter.

Q: What would you add next?
A: GitHub App credentials, a queue-backed worker pool, per-session resource quotas, sandbox image scanning, policy checks, and PR review comments.
