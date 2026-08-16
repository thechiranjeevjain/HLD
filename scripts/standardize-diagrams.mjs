import { readFile, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { trackedMarkdownFiles } from "./doc-files.mjs";

const projects = trackedMarkdownFiles()
  .filter((file) => /^[^/]+\/README\.md$/.test(file))
  .map((file) => file.split("/", 1)[0]);

let changed = 0;
for (const project of projects) {
  const candidates = [`${project}/docs/DIAGRAMS.md`, `${project}/docs/ARCHITECTURE.md`];
  const file = candidates.find(existsSync);
  if (!file) continue;

  const source = await readFile(file, "utf8");
  if (/^## High-Level (Design|Architecture)\s*$/im.test(source)) continue;

  const headings = [...source.matchAll(/^## (.+)$/gm)];
  if (headings.length < 2) continue;
  const highIndex = Math.max(
    0,
    headings.findIndex((heading) =>
      /architecture|system context|component|cluster|system view|data structure|microservice|safe processor/i.test(
        heading[1],
      ),
    ),
  );
  const lowOffset = headings
    .slice(highIndex + 1)
    .findIndex((heading) => /flow|path|sequence|boundary/i.test(heading[1]));
  const lowIndex = lowOffset >= 0 ? highIndex + lowOffset + 1 : highIndex + 1;

  let result = "";
  let cursor = 0;
  headings.forEach((heading, index) => {
    result += source.slice(cursor, heading.index);
    if (index === highIndex) result += "## High-Level Design\n\n";
    if (index === lowIndex) result += "## Low-Level Design\n\n";
    result += index >= highIndex ? `### ${heading[1]}` : heading[0];
    cursor = heading.index + heading[0].length;
  });
  result += source.slice(cursor);
  await writeFile(file, result, "utf8");
  changed += 1;
}

console.log(`Standardized HLD/LLD sections in ${changed} architecture file(s).`);
