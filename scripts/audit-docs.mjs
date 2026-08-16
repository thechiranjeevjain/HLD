import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { existsSync } from "node:fs";
import prettier from "prettier";
import puppeteer from "puppeteer";
import { renderMermaid } from "@mermaid-js/mermaid-cli";
import { trackedMarkdownFiles, temporarilyExcludedPrefix } from "./doc-files.mjs";

const files = trackedMarkdownFiles();
const failures = [];
let mermaidBlocks = 0;
let jsonBlocks = 0;
const mermaidDefinitions = [];

function fail(file, message) {
  failures.push(`${file}: ${message}`);
}

for (const file of files) {
  const source = await readFile(file, "utf8");
  const normalized = source.replaceAll("\r\n", "\n");
  const formatted = await prettier.format(source, { filepath: file });
  if (normalized !== formatted) fail(file, "not formatted by Prettier");

  const fenceCount = (normalized.match(/^```/gm) ?? []).length;
  if (fenceCount % 2 !== 0) fail(file, "contains an unmatched fenced code block");

  const blockPattern = /^```([\w-]*)[^\n]*\n([\s\S]*?)^```\s*$/gm;
  for (const match of normalized.matchAll(blockPattern)) {
    const language = match[1].toLowerCase();
    const body = match[2].trim();
    if (language === "json") {
      jsonBlocks += 1;
      try {
        JSON.parse(body);
      } catch (error) {
        fail(file, `invalid JSON block: ${error.message}`);
      }
    }
    if (language === "mermaid") {
      mermaidBlocks += 1;
      mermaidDefinitions.push({ file, body });
    }
  }

  const linkPattern = /(?<!!)\[[^\]]+\]\(([^)]+)\)/g;
  for (const match of normalized.matchAll(linkPattern)) {
    const target = match[1].trim().replace(/^<|>$/g, "").split("#", 1)[0];
    if (!target || /^[a-z][a-z\d+.-]*:/i.test(target)) continue;
    const decoded = decodeURIComponent(target);
    if (!existsSync(resolve(dirname(file), decoded))) {
      fail(file, `broken local link: ${target}`);
    }
  }
}

if (mermaidDefinitions.length > 0) {
  const browser = await puppeteer.launch({ headless: "shell" });
  try {
    for (const { file, body } of mermaidDefinitions) {
      try {
        await renderMermaid(browser, body, "svg", {
          backgroundColor: "transparent",
          mermaidConfig: { theme: "default" },
        });
      } catch (error) {
        fail(file, `invalid Mermaid block: ${error.message}`);
      }
    }
  } finally {
    await browser.close();
  }
}

const projectRoots = new Set(
  files.filter((file) => /^[^/]+\/README\.md$/.test(file)).map((file) => file.split("/", 1)[0]),
);
for (const project of projectRoots) {
  const candidates = [`${project}/docs/DIAGRAMS.md`, `${project}/docs/ARCHITECTURE.md`];
  const architectureFile = candidates.find(existsSync);
  if (!architectureFile) {
    fail(project, "missing docs/DIAGRAMS.md or docs/ARCHITECTURE.md");
    continue;
  }
  const source = await readFile(architectureFile, "utf8");
  if (!/^## High-Level (Design|Architecture)\s*$/im.test(source)) {
    fail(architectureFile, "missing an explicit High-Level Design section");
  }
  if (!/^## Low-Level (Design|Architecture)\s*$/im.test(source)) {
    fail(architectureFile, "missing an explicit Low-Level Design section");
  }
  const diagrams = (source.match(/^```mermaid\s*$/gm) ?? []).length;
  if (diagrams < 2) fail(architectureFile, "requires at least two Mermaid diagrams");
}

if (failures.length > 0) {
  console.error(`Documentation audit failed with ${failures.length} issue(s):`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exitCode = 1;
} else {
  console.log(
    `Documentation audit passed: ${files.length} Markdown files, ${mermaidBlocks} Mermaid blocks, ${jsonBlocks} JSON blocks.`,
  );
  console.log(`Temporarily excluded user-owned worktree prefix: ${temporarilyExcludedPrefix}`);
}
