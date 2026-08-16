import { readFile, writeFile } from "node:fs/promises";
import prettier from "prettier";
import { trackedMarkdownFiles } from "./doc-files.mjs";

let changed = 0;
for (const file of trackedMarkdownFiles()) {
  const source = await readFile(file, "utf8");
  const formatted = await prettier.format(source, { filepath: file });
  if (source.replaceAll("\r\n", "\n") !== formatted) {
    await writeFile(file, formatted, "utf8");
    changed += 1;
  }
}

console.log(`Formatted ${changed} Markdown file(s).`);
