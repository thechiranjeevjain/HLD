# Cloud AI Coding Agent Demo Script

## Verify

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\cloud-ai-coding-agent
mvn -pl backend -am test
cd frontend
npm.cmd install
npm.cmd test
npm.cmd run build
cd ..
docker compose config
```

## Run

Docker Desktop with Linux containers is required:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\cloud-ai-coding-agent
Copy-Item .env.example .env
docker compose up --build
```

Open `http://localhost:3000`.

## Walkthrough

1. Submit the sample repository, branch, and a small task.
2. Show the session creation response and WebSocket progress.
3. Point to each stored agent step: plan, tool call, output, and diff.
4. Explain the fake planner as a deterministic local demo.
5. Open the generated diff and show `AGENT_RESULT.md`.
6. Trigger cancel or retry and explain the session state transition.
7. Use `docker compose logs -f backend` to show backend orchestration logs.

## Interview Close

Say: the hard backend problem is not just calling an LLM. It is controlling durable workflow state, sandboxed tool execution, retries, cancellation, streaming, auditability, and provider boundaries.
