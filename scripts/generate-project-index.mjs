import { readFile, writeFile } from "node:fs/promises";
import prettier from "prettier";
import { loadProjectCatalog } from "./project-catalog.mjs";

const checkOnly = process.argv.includes("--check");
const catalog = await loadProjectCatalog();
const changedFiles = [];

const stageDefinitions = [
  [
    1,
    "Foundations",
    "Explain the primitive, its invariant, and its main failure mode",
  ],
  [
    2,
    "Service design",
    "Design a clean API, persistence boundary, validation, and authorization",
  ],
  [
    3,
    "Workflows",
    "Walk a stateful business flow through success, retry, and partial failure",
  ],
  [
    4,
    "Distributed correctness",
    "Defend ordering, idempotency, replay, state ownership, and recovery",
  ],
  [
    5,
    "Specialization",
    "Connect general patterns to domain-specific constraints",
  ],
];

function escapeCell(value) {
  return String(value).replaceAll("|", "\\|").replaceAll("\n", " ");
}

function generatedBlock(id, body) {
  return `<!-- project-catalog:${id}:start -->\n${body.trim()}\n<!-- project-catalog:${id}:end -->`;
}

function replaceBlock(source, id, body, file) {
  const start = `<!-- project-catalog:${id}:start -->`;
  const end = `<!-- project-catalog:${id}:end -->`;
  const startIndex = source.indexOf(start);
  const endIndex = source.indexOf(end);
  if (startIndex < 0 || endIndex < startIndex)
    throw new Error(`${file} is missing generated block ${id}`);
  return `${source.slice(0, startIndex)}${generatedBlock(id, body)}${source.slice(endIndex + end.length)}`;
}

async function update(file, transformations) {
  const source = await readFile(file, "utf8");
  const generated = transformations.reduce(
    (current, transformation) =>
      replaceBlock(current, transformation.id, transformation.body, file),
    source,
  );
  const expected = await prettier.format(generated, { filepath: file });
  if (source === expected) return;
  changedFiles.push(file);
  if (!checkOnly) await writeFile(file, expected, "utf8");
}

const summary = `The tracked portfolio contains **${catalog.projects.length} canonical projects**. Keep them separate: focused projects teach one hard idea, while flagship projects integrate multiple ideas without reimplementing every subsystem.`;

const stageRows = stageDefinitions.map(([stage, title, exitCondition]) => {
  const names = catalog.projects
    .filter((entry) => entry.stage === stage)
    .map((entry) => `\`${entry.id}\``)
    .join(", ");
  return `| ${stage} — ${title} | ${names} | ${exitCondition} |`;
});
stageRows.push(
  "| 6 — Interview delivery | Project interview guides plus the shared HLD framework | Reach R4: explain, draw, deliver, and defend without notes |",
);
const learningStages = `| Stage | Learn here | Exit condition |
| --- | --- | --- |
${stageRows.join("\n")}`;

const portfolioSections = catalog.categories.map((category) => {
  const rows = catalog.entries
    .filter((entry) => entry.category === category.id)
    .map((entry) => {
      const target = entry.readme ?? `${entry.path}/README.md`;
      return `| [${escapeCell(entry.name)}](${target}) | ${escapeCell(entry.owns)} | \`${escapeCell(entry.mavenEntry)}\` |`;
    })
    .join("\n");
  return `### ${category.title}

| Project | Owns | Maven entry |
| --- | --- | --- |
${rows}`;
});
const portfolio = portfolioSections.join("\n\n");

const hldRows = catalog.projects
  .filter((entry) => entry.hld)
  .sort((left, right) => left.hld.number - right.hld.number)
  .map(
    (entry) =>
      `| ${entry.hld.number} | ${escapeCell(entry.hld.topic)} | \`${escapeCell(entry.hld.canonicalLocation)}\` | ${escapeCell(entry.hld.runnableProof)} |`,
  )
  .join("\n");
const hldIndex = `| # | Topic | Canonical location | Runnable proof |
| --- | --- | --- | --- |
${hldRows}`;

await update("README.md", [
  { id: "summary", body: summary },
  { id: "learning-stages", body: learningStages },
  { id: "portfolio", body: portfolio },
]);
await update("HLD-26-30-INTERVIEW-PACK.md", [
  { id: "hld-index", body: hldIndex },
]);

if (checkOnly && changedFiles.length > 0) {
  console.error(
    `Generated project indexes are stale: ${changedFiles.join(", ")}`,
  );
  console.error("Run npm run docs:index and commit the generated changes.");
  process.exitCode = 1;
} else if (checkOnly) {
  console.log(
    `Project index check passed: ${catalog.projects.length} projects and ${catalog.entries.length} catalog entries.`,
  );
} else {
  console.log(
    `Generated project indexes for ${catalog.projects.length} projects in ${changedFiles.length} file(s): ${changedFiles.join(", ") || "already current"}.`,
  );
}
