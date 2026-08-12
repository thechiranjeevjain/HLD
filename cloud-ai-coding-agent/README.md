# Cloud AI Coding Agent

An interview-sized, end-to-end cloud coding agent: React UI, Spring Boot modular monolith, PostgreSQL source of truth, provider-neutral LLM planning, auditable tool calls, and disposable non-root Docker sandboxes.

## Run

Prerequisites: Docker Desktop with Linux containers and Compose v2.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Open `http://localhost:3000`, submit a public GitHub HTTPS repository, branch, and task. The default deterministic fake planner makes a safe `AGENT_RESULT.md` change. Set `LLM_PROVIDER=openai` and `OPENAI_API_KEY` for the real adapter. PostgreSQL is authoritative; WebSocket events are transient.

Useful checks: `http://localhost:8080/api/health`, `docker compose ps`, `docker compose logs -f backend`. Reset with `./scripts/reset.ps1`; remove leaked sandboxes with `./scripts/cleanup.ps1`.

## Implemented APIs

`POST /api/sessions`, `GET /api/sessions/{id}`, `/steps`, `/diff`, `POST /cancel`, `/retry`, `/pull-request`, and `GET /api/health`. Events publish to STOMP `/topic/sessions/{id}` through `/ws`.

## Honest limitations

Local Compose gives the orchestrator Docker access so it can create sandboxes; the sandbox itself never receives the socket. Kubernetes deliberately does not fake Docker-in-Docker: production needs a worker integration with a sandbox runtime. GitHub App/PR publishing is an explicit adapter boundary and returns 501 until credentials are configured. Cancellation is cooperative between steps. The fake planner is deterministic and safe but not a general coding intelligence. Redis is included for the scale-out coordination seam but is not required by the single-node loop.

## Test

```powershell
gradle :backend:test
Set-Location frontend; npm install; npm test; npm run build
docker compose config
```

See [ARCHITECTURE.md](ARCHITECTURE.md), [SECURITY.md](SECURITY.md), [RUNBOOK.md](RUNBOOK.md), [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md), and [REVISION_SHEET.md](REVISION_SHEET.md).
