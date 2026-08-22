import { existsSync } from "node:fs";
import { readFile, readdir } from "node:fs/promises";

export const catalogFile = "project-catalog.json";

function unique(values) {
  return new Set(values).size === values.length;
}

function requireText(entry, field) {
  if (typeof entry[field] !== "string" || entry[field].trim() === "") {
    throw new Error(
      `Catalog entry ${entry.id ?? "<unknown>"} requires ${field}`,
    );
  }
}

export async function loadProjectCatalog() {
  const catalog = JSON.parse(await readFile(catalogFile, "utf8"));
  if (catalog.schemaVersion !== 1)
    throw new Error(`Unsupported catalog schema: ${catalog.schemaVersion}`);
  if (!Array.isArray(catalog.categories) || !Array.isArray(catalog.entries)) {
    throw new Error("Catalog requires categories and entries arrays");
  }

  const categoryIds = catalog.categories.map((category) => category.id);
  if (!unique(categoryIds))
    throw new Error("Catalog category IDs must be unique");
  const categorySet = new Set(categoryIds);

  const entryIds = catalog.entries.map((entry) => entry.id);
  if (!unique(entryIds)) throw new Error("Catalog entry IDs must be unique");
  const entryIdSet = new Set(entryIds);

  if (!Array.isArray(catalog.learningOrder) || !unique(catalog.learningOrder)) {
    throw new Error("Catalog learningOrder must contain unique entry IDs");
  }
  const missingFromLearningOrder = entryIds.filter(
    (id) => !catalog.learningOrder.includes(id),
  );
  const unknownLearningEntries = catalog.learningOrder.filter(
    (id) => !entryIdSet.has(id),
  );
  if (
    missingFromLearningOrder.length > 0 ||
    unknownLearningEntries.length > 0
  ) {
    throw new Error(
      `Learning order must contain every catalog entry exactly once; missing: ${missingFromLearningOrder.join(", ") || "none"}; unknown: ${unknownLearningEntries.join(", ") || "none"}`,
    );
  }

  const learningDependencies = catalog.learningDependencies ?? {};
  const learningRanks = new Map(
    catalog.learningOrder.map((id, index) => [id, index + 1]),
  );
  for (const [id, dependencies] of Object.entries(learningDependencies)) {
    if (
      !entryIdSet.has(id) ||
      !Array.isArray(dependencies) ||
      !unique(dependencies)
    ) {
      throw new Error(`Invalid learning dependencies for ${id}`);
    }
    for (const dependency of dependencies) {
      if (!entryIdSet.has(dependency))
        throw new Error(`Unknown learning dependency ${dependency} for ${id}`);
      if (learningRanks.get(dependency) >= learningRanks.get(id)) {
        throw new Error(
          `Learning dependency ${dependency} must rank before ${id}`,
        );
      }
    }
  }

  const roiIds = Object.keys(catalog.interviewRoi ?? {});
  const missingRoi = entryIds.filter((id) => !roiIds.includes(id));
  const unknownRoi = roiIds.filter((id) => !entryIdSet.has(id));
  if (missingRoi.length > 0 || unknownRoi.length > 0) {
    throw new Error(
      `Interview ROI must cover every catalog entry; missing: ${missingRoi.join(", ") || "none"}; unknown: ${unknownRoi.join(", ") || "none"}`,
    );
  }
  for (const [id, roi] of Object.entries(catalog.interviewRoi)) {
    if (!Number.isInteger(roi) || roi < 1 || roi > 5) {
      throw new Error(`Interview ROI for ${id} must be an integer from 1 to 5`);
    }
  }

  for (const entry of catalog.entries) {
    for (const field of [
      "id",
      "kind",
      "name",
      "root",
      "path",
      "category",
      "owns",
      "mavenEntry",
    ]) {
      requireText(entry, field);
    }
    if (!new Set(["project", "module"]).has(entry.kind)) {
      throw new Error(
        `Catalog entry ${entry.id} has unsupported kind ${entry.kind}`,
      );
    }
    if (!categorySet.has(entry.category)) {
      throw new Error(
        `Catalog entry ${entry.id} references unknown category ${entry.category}`,
      );
    }
    if (!Number.isInteger(entry.stage) || entry.stage < 1 || entry.stage > 5) {
      throw new Error(
        `Catalog entry ${entry.id} requires a learning stage from 1 to 5`,
      );
    }
    if (!existsSync(entry.path))
      throw new Error(`Catalog path does not exist: ${entry.path}`);
    const readme = entry.readme ?? `${entry.path}/README.md`;
    if (!existsSync(readme))
      throw new Error(`Catalog README does not exist: ${readme}`);
    if (!existsSync(`${entry.mavenEntry}/pom.xml`)) {
      throw new Error(
        `Catalog Maven entry does not contain pom.xml: ${entry.mavenEntry}`,
      );
    }
  }

  const projects = catalog.entries.filter((entry) => entry.kind === "project");
  const roots = projects.map((entry) => entry.root);
  if (!unique(roots)) throw new Error("Canonical project roots must be unique");

  const nonProjectRoots = new Set([
    "_archive",
    "_meta",
    "docs",
    "node_modules",
    "review",
    "scripts",
  ]);
  const discoveredRoots = (await readdir(".", { withFileTypes: true }))
    .filter((entry) => entry.isDirectory() && !nonProjectRoots.has(entry.name))
    .map((entry) => entry.name)
    .filter((root) => existsSync(`${root}/README.md`));
  const registeredRoots = new Set(roots);
  const missingRegistrations = discoveredRoots.filter(
    (root) => !registeredRoots.has(root),
  );
  const missingProjectRoots = roots.filter(
    (root) => !discoveredRoots.includes(root),
  );
  if (missingRegistrations.length > 0) {
    throw new Error(
      `Top-level projects missing from catalog: ${missingRegistrations.join(", ")}`,
    );
  }
  if (missingProjectRoots.length > 0) {
    throw new Error(
      `Catalog project roots missing a README: ${missingProjectRoots.join(", ")}`,
    );
  }

  const hldNumbers = projects.flatMap((entry) =>
    entry.hld ? [entry.hld.number] : [],
  );
  if (!unique(hldNumbers)) throw new Error("HLD numbers must be unique");
  for (const entry of projects.filter((candidate) => candidate.hld)) {
    if (!Number.isInteger(entry.hld.number))
      throw new Error(`HLD number must be an integer for ${entry.id}`);
    for (const field of ["topic", "canonicalLocation", "runnableProof"])
      requireText(entry.hld, field);
    if (!existsSync(entry.hld.canonicalLocation)) {
      throw new Error(
        `HLD canonical location does not exist: ${entry.hld.canonicalLocation}`,
      );
    }
  }

  return { ...catalog, projects };
}
