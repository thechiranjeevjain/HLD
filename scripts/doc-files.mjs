import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";

export const temporarilyExcludedPrefix = "real-time-inventory-platform/";

export function trackedMarkdownFiles() {
  return execFileSync("git", ["ls-files", "*.md"], { encoding: "utf8" })
    .split(/\r?\n/)
    .filter(Boolean)
    .map((file) => file.replaceAll("\\", "/"))
    .filter((file) => !file.startsWith(temporarilyExcludedPrefix))
    .filter(existsSync);
}
