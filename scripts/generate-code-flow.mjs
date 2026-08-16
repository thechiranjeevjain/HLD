import { execFileSync } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";

const tracked = execFileSync("git", ["ls-files"], { encoding: "utf8" })
  .split(/\r?\n/)
  .filter(Boolean)
  .map((file) => file.replaceAll("\\", "/"));

const projects = tracked
  .filter((file) => /^[^/]+\/README\.md$/.test(file))
  .map((file) => file.split("/", 1)[0])
  .filter((project) => project !== "real-time-inventory-platform");

function titleCase(value) {
  return value
    .split(/[-_]/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function escapeCell(value) {
  return value.replaceAll("|", "\\|").replaceAll("\n", " ");
}

function classify(name, path) {
  if (/Application$|Main$|Server$/.test(name)) return "Entry point";
  if (/Controller$|Resource$|Endpoint$|Handler$/.test(name)) return "Inbound adapter";
  if (/Service$|Processor$|Engine$|Coordinator$|Scheduler$|Manager$|UseCase$/.test(name)) return "Application/domain logic";
  if (/Repository$|Dao$|Store$|Persistence$|Wal$/.test(name)) return "Persistence adapter";
  if (/Publisher$|Producer$|Consumer$|Listener$|Queue$|Outbox$/.test(name)) return "Messaging/async adapter";
  if (/Config$|Configuration$|Security$|Filter$|Interceptor$/.test(name)) return "Configuration/security";
  if (/Client$|Gateway$|Adapter$/.test(name)) return "Outbound adapter";
  if (/Request$|Response$|Dto$|View$|Command$|Event$|Result$/.test(name)) return "API/message contract";
  if (/Entity$|Model$|Aggregate$|Order$|User$|Product$|Ride$|Link$|Task$|Record$/.test(name)) return "Domain/data model";
  if (/frontend|src\/main\/resources\/static/i.test(path)) return "User interface";
  return "Supporting logic";
}

function responsibility(name, category) {
  const descriptions = {
    "Entry point": "Bootstraps the process and wires the runtime.",
    "Inbound adapter": "Accepts an inbound call, validates its boundary contract, and delegates work.",
    "Application/domain logic": "Coordinates the use case and enforces domain decisions.",
    "Persistence adapter": "Reads or writes durable state behind a storage boundary.",
    "Messaging/async adapter": "Publishes, consumes, retries, or records asynchronous work.",
    "Configuration/security": "Defines runtime wiring, authentication, authorization, or cross-cutting policy.",
    "Outbound adapter": "Calls an external system through an isolated integration boundary.",
    "API/message contract": "Carries validated data across an API or messaging boundary.",
    "Domain/data model": "Represents domain state, identity, or an invariant-bearing value.",
    "User interface": "Presents state and initiates user actions.",
    "Supporting logic": "Provides a focused algorithm or shared implementation detail.",
  };
  return `${name} ${descriptions[category].charAt(0).toLowerCase()}${descriptions[category].slice(1)}`;
}

function extractMethods(source, className) {
  const methods = [];
  const pattern = /\b(?:public|protected)\s+(?:static\s+)?(?:<[^>]+>\s+)?[\w<>?,.\[\] ]+\s+(\w+)\s*\([^;{}]*\)\s*(?:throws [^{]+)?\{/g;
  for (const match of source.matchAll(pattern)) {
    if (match[1] !== className && !methods.includes(match[1])) methods.push(match[1]);
  }
  return methods.slice(0, 6);
}

function extractEndpoints(source, className) {
  const endpoints = [];
  const pattern = /@(Get|Post|Put|Patch|Delete)Mapping(?:\s*\(\s*(?:value\s*=\s*)?["']([^"']*)["'][^)]*\))?/g;
  for (const match of source.matchAll(pattern)) {
    endpoints.push({ owner: className, verb: match[1].toUpperCase(), path: match[2] || "(class-level/default path)" });
  }
  return endpoints;
}

function firstUsefulParagraph(readme) {
  const blocks = readme.replace(/```[\s\S]*?```/g, "").split(/\n\s*\n/);
  return (
    blocks.find((block) => {
      const text = block.trim();
      return text && !text.startsWith("#") && !text.startsWith("|") && !text.startsWith("-") && !text.startsWith("[");
    })?.trim() ?? "See the project README for its business scope and runnable scenarios."
  );
}

for (const project of projects) {
  const sourcePaths = tracked.filter(
    (file) =>
      file.startsWith(`${project}/`) &&
      /\.(java|kt|py|ts|tsx|js|jsx)$/.test(file) &&
      !/(^|\/)(test|tests|node_modules|target|build|dist)(\/|$)/i.test(file),
  );
  const units = [];
  const endpoints = [];

  for (const path of sourcePaths) {
    if (!existsSync(path)) continue;
    const source = await readFile(path, "utf8");
    const fallback = path.split("/").at(-1).replace(/\.[^.]+$/, "");
    const name = source.match(/\b(?:class|interface|record|enum|object)\s+(\w+)/)?.[1] ?? fallback;
    const category = classify(name, path);
    const methods = extractMethods(source, name);
    units.push({ path, name, category, methods, responsibility: responsibility(name, category) });
    endpoints.push(...extractEndpoints(source, name));
  }

  const readme = await readFile(`${project}/README.md`, "utf8");
  const purpose = firstUsefulParagraph(readme);
  const byCategory = new Map();
  for (const unit of units) {
    if (!byCategory.has(unit.category)) byCategory.set(unit.category, []);
    byCategory.get(unit.category).push(unit.name);
  }

  const inbound = byCategory.get("Inbound adapter")?.[0] ?? byCategory.get("Entry point")?.[0] ?? units[0]?.name ?? "Caller";
  const domain = byCategory.get("Application/domain logic")?.[0] ?? byCategory.get("Supporting logic")?.[0] ?? "Core logic";
  const storage = byCategory.get("Persistence adapter")?.[0] ?? byCategory.get("Domain/data model")?.[0] ?? "State";
  const asyncUnit = byCategory.get("Messaging/async adapter")?.[0];
  const outbound = byCategory.get("Outbound adapter")?.[0];
  const title = titleCase(project);

  const componentLines = [...byCategory.entries()]
    .map(([category, names]) => `| ${category} | ${names.map((name) => `\`${name}\``).join(", ")} |`)
    .join("\n");
  const sourceLines = units
    .map(
      (unit) =>
        `| [\`${escapeCell(unit.path.split("/").at(-1))}\`](./${escapeCell(unit.path.slice(project.length + 1))}) | ${unit.category} | ${escapeCell(unit.responsibility)}${unit.methods.length ? ` Key methods: ${unit.methods.map((method) => `\`${method}()\``).join(", ")}.` : ""} |`,
    )
    .join("\n");
  const endpointLines = endpoints.length
    ? endpoints.map((item) => `| \`${item.verb}\` | \`${escapeCell(item.path)}\` | \`${item.owner}\` |`).join("\n")
    : "| N/A | No annotation-based HTTP endpoint; execution starts through the process API, CLI, test harness, or protocol adapter. | See entry points below. |";

  const asyncArrow = asyncUnit ? `\n    Domain --> Async["${asyncUnit}"]\n    Async --> Worker["Async consumer / worker"]` : "";
  const outboundArrow = outbound ? `\n    Domain --> External["${outbound}"]` : "";
  const asyncSequence = asyncUnit ? `\n    Domain->>Async: publish durable or retryable work\n    Async-->>Domain: accepted / recorded` : "";
  const persistSequence = storage !== "State" ? `\n    Domain->>Store: read or persist state\n    Store-->>Domain: current durable result` : "";

  const document = `# ${title} Code Flow

This is the single code-flow guide for the project. It connects the business request to concrete source files, explains both architectural levels, and shows where validation, state changes, asynchronous work, and responses occur.

## Scope and outcome

${purpose}

The tracked production-code inventory used by this guide contains **${units.length} source units** and **${endpoints.length} annotation-discovered HTTP operations**. Generated/build output and test sources are intentionally excluded from the runtime path.

## High-Level Design

At the system level, callers enter through an inbound adapter. The adapter owns transport concerns; application/domain code owns use-case decisions; repositories and gateways isolate state and external systems. Asynchronous adapters extend the flow without moving business invariants into controllers or consumers.

\`\`\`mermaid
flowchart LR
    Caller["Client / operator / upstream system"] --> Inbound["${inbound}"]
    Inbound --> Domain["${domain}"]
    Domain --> Store["${storage}"]${asyncArrow}${outboundArrow}
    Domain --> Result["Response / observable result"]
\`\`\`

### Runtime stages

1. **Enter:** a request, command, scheduled trigger, protocol message, or UI action reaches the inbound boundary.
2. **Validate:** transport shape and required fields are rejected before domain mutation.
3. **Decide:** application/domain logic loads required state and applies invariants, idempotency, authorization, limits, or algorithms.
4. **Commit:** durable state changes pass through a repository/store; external calls pass through gateways; asynchronous work passes through message boundaries.
5. **Return and observe:** the adapter maps the result to an HTTP response, protocol response, CLI output, event, or metric.

## Low-Level Design

The low-level path keeps orchestration directional: inbound adapter → application/domain unit → persistence/outbound adapter. Contracts carry data between layers; configuration and security apply cross-cutting policy without becoming business logic.

\`\`\`mermaid
sequenceDiagram
    autonumber
    actor Caller
    participant Inbound as ${inbound}
    participant Domain as ${domain}
    participant Store as ${storage}${asyncUnit ? `\n    participant Async as ${asyncUnit}` : ""}
    Caller->>Inbound: submit input
    Inbound->>Inbound: parse and boundary-validate
    Inbound->>Domain: invoke use case
    Domain->>Domain: enforce invariants and make decision${persistSequence}${asyncSequence}
    Domain-->>Inbound: domain result or typed failure
    Inbound-->>Caller: mapped response / output
\`\`\`

### Component map

| Responsibility | Concrete code |
| --- | --- |
${componentLines || "| Runtime implementation | See the source inventory below. |"}

### Inbound operations

| Verb/trigger | Path or input | Owning code |
| --- | --- | --- |
${endpointLines}

## Detailed source walkthrough

Read this table top-down by category, then follow the linked files. “Key methods” are discovered from the checked-in implementation and identify useful debugging/interview entry points.

| Source file | Role | Responsibility and important methods |
| --- | --- | --- |
${sourceLines || "| README/build configuration | Runtime is configuration- or script-driven; see the project README for its executable path. | Describes the runnable flow. |"}

## End-to-end code-flow narrative

1. Start at \`${inbound}\`. It receives the external input and should perform only boundary parsing, authentication/authorization handoff, and request validation.
2. Follow the call into \`${domain}\`. This is the principal place to explain the use case, invariant checks, deduplication/concurrency decision, and success/failure result.
3. Continue into \`${storage}\` for the durable or in-memory state transition. Transaction and consistency guarantees belong at this boundary, not in response mapping.
${asyncUnit ? `4. Follow \`${asyncUnit}\` when the synchronous decision emits follow-up work. Verify retry, duplicate-delivery, and dead-letter behavior independently of the request thread.` : "4. This flow completes synchronously; background work is not part of the primary checked-in path."}
${outbound ? `5. Inspect \`${outbound}\` for timeout, retry, circuit-breaking, and external-contract mapping.` : "5. External-system behavior is either absent or represented behind another listed adapter."}
6. Return to \`${inbound}\`, where typed outcomes become the public response or protocol result. Logging and metrics should preserve correlation identifiers without leaking secrets.

## Failure and correctness checkpoints

- **Invalid input:** rejected at the inbound contract before state mutation.
- **Domain conflict:** returned as a typed result; do not hide it as an infrastructure exception.
- **Duplicate or concurrent work:** handled where the project’s idempotency, locking, ordering, or atomic data structure is implemented.
- **Storage/integration failure:** transaction is rolled back or the operation remains safely retryable.
- **Async failure:** consumer retries must preserve idempotency; poison work must become observable rather than loop silently.
- **Observability:** trace the same request/correlation identity through inbound, domain, persistence, and async logs.

## How to trace a scenario in the debugger

1. Choose one operation from the inbound-operations table or the project’s demo script.
2. Break at \`${inbound}\`, then step into \`${domain}\` rather than framework internals.
3. Inspect the command/request object immediately after validation.
4. Stop before and after the call to \`${storage}\` to compare intended and durable state.
5. If messaging exists, capture the emitted identifier and continue from \`${asyncUnit ?? "the consumer/worker"}\`.
6. Confirm the final public result and the corresponding logs/metrics.

## Related project documentation

- [Project README](./README.md)
- ${existsSync(`${project}/docs/DIAGRAMS.md`) ? "[Specialized diagrams](./docs/DIAGRAMS.md)" : existsSync(`${project}/docs/ARCHITECTURE.md`) ? "[Architecture details](./docs/ARCHITECTURE.md)" : "Architecture material is contained in this guide."}
- ${existsSync(`${project}/docs/INTERVIEW_GUIDE.md`) ? "[Interview guide](./docs/INTERVIEW_GUIDE.md)" : existsSync(`${project}/INTERVIEW_GUIDE.md`) ? "[Interview guide](./INTERVIEW_GUIDE.md)" : "Use this guide as the primary interview walkthrough."}
`;

  await writeFile(`${project}/CODE_FLOW.md`, document, "utf8");
}

console.log(`Generated ${projects.length} project CODE_FLOW.md files from tracked source.`);
