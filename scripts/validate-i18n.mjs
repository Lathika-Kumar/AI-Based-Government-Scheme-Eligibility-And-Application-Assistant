import { readFileSync, readdirSync, statSync } from "fs";
import { resolve, join, extname } from "path";
import { fileURLToPath } from "url";

const __dirname = fileURLToPath(new URL(".", import.meta.url));

// ─── Load translations (raw file parse, no bundler needed) ────────────────
const translationsPath = resolve(__dirname, "../src/data/translations.js");
const translationsRaw = readFileSync(translationsPath, "utf8");
// Strip the `export const` and return the value
const translationsCode = translationsRaw
  .replace("export const TRANSLATIONS =", "return")
  .replace(/;\s*$/, "");

let TRANSLATIONS;
try {
  TRANSLATIONS = Function(translationsCode)();
} catch (err) {
  console.error("\u274c Failed to parse translations.js:", err.message);
  process.exit(1);
}

const EN_KEYS = new Set(Object.keys(TRANSLATIONS.en));
const LANGUAGES = ["hi", "ta", "te", "kn"];

let hasError = false;
const warn  = (msg) => console.warn ("  ⚠️  " + msg);
const fail  = (msg) => { console.error("  ❌ " + msg); hasError = true; };
const pass  = (msg) => console.log  ("  ✅ " + msg);

// ─── 1. Key Parity ────────────────────────────────────────────────────────
console.log("\n📋 Key Parity Check");
LANGUAGES.forEach((lang) => {
  const LANG_KEYS = new Set(Object.keys(TRANSLATIONS[lang] || {}));
  const missingInLang = [...EN_KEYS].filter((k) => !LANG_KEYS.has(k));
  const missingInEn = [...LANG_KEYS].filter((k) => !EN_KEYS.has(k));

  if (missingInLang.length) fail(`Keys in EN but missing in ${lang.toUpperCase()}: ${missingInLang.join(", ")}`);
  else pass(`All EN keys have ${lang.toUpperCase()} equivalents (${EN_KEYS.size} keys)`);
  if (missingInEn.length) fail(`Keys in ${lang.toUpperCase()} but missing in EN: ${missingInEn.join(", ")}`);
});

// ─── 2. Empty Value Check ─────────────────────────────────────────────────
console.log("\n🔍 Empty Value Check");
let emptyCount = 0;
[...EN_KEYS].forEach((k) => { if (!TRANSLATIONS.en[k]) { fail(`EN["${k}"] is empty`); emptyCount++; } });
LANGUAGES.forEach((lang) => {
  const LANG_DICT = TRANSLATIONS[lang] || {};
  [...EN_KEYS].forEach((k) => { if (!LANG_DICT[k]) { fail(`${lang.toUpperCase()}["${k}"] is empty`); emptyCount++; } });
});
if (emptyCount === 0) pass("No empty translation values");

// ─── 3. Template Placeholder Parity ──────────────────────────────────────
console.log("\n🔧 Template Placeholder Check");
const TEMPLATE_KEYS = ["tracker_docs_ready", "tracker_docs_more"];
let placeholderOk = true;
TEMPLATE_KEYS.forEach((key) => {
  const enVal = TRANSLATIONS.en[key] || "";
  const placeholders = (enVal.match(/\{[\w]+\}/g) || []);
  LANGUAGES.forEach((lang) => {
    const langVal = TRANSLATIONS[lang]?.[key] || "";
    placeholders.forEach((ph) => {
      if (!langVal.includes(ph)) {
        fail(`${lang.toUpperCase()}["${key}"] is missing placeholder "${ph}" (EN: "${enVal}", ${lang.toUpperCase()}: "${langVal}")`);
        placeholderOk = false;
      }
    });
  });
});
if (placeholderOk) pass("All template placeholders exist in all language equivalents");

// ─── 4. JSX Usage Scan (Node built-ins only) ─────────────────────────────
console.log("\n🔎 JSX Usage Scan");

function walkDir(dir, ext, results = []) {
  readdirSync(dir).forEach((name) => {
    const fullPath = join(dir, name);
    if (statSync(fullPath).isDirectory()) {
      walkDir(fullPath, ext, results);
    } else if (extname(name) === ext) {
      results.push(fullPath);
    }
  });
  return results;
}

const srcDir = resolve(__dirname, "../src");
const jsxFiles = walkDir(srcDir, ".jsx");

const usedKeys = new Set();
const dynamicPattern = /\bt\(["'](\w+)["']\)/g;
const translationKeyPropPattern = /translationKey:\s*["'](\w+)["']/g;

for (const file of jsxFiles) {
  const content = readFileSync(file, "utf8");
  for (const match of content.matchAll(dynamicPattern)) usedKeys.add(match[1]);
  for (const match of content.matchAll(translationKeyPropPattern)) usedKeys.add(match[1]);
}

// Keys used but not defined (critical error)
const undefinedUsed = [...usedKeys].filter(
  (k) => !EN_KEYS.has(k) && k !== "language" && !/^[A-Z]$/.test(k)
);
if (undefinedUsed.length) {
  fail(`t() called with undefined keys: ${undefinedUsed.join(", ")}`);
} else {
  pass(`All ${usedKeys.size} used keys exist in dictionary`);
}

// Keys defined but never used in JSX (warning only)
// nav_* are consumed via dynamic t(item.translationKey) — not literal t("key") pattern
// tracker stage/template keys are consumed via STAGE_KEY_MAP and tWith() helpers
const KNOWN_DYNAMIC_KEYS = [
  "tracker_stage_saved", "tracker_stage_preparing", "tracker_stage_ready",
  "tracker_stage_submitted", "tracker_stage_review", "tracker_stage_approved", "tracker_stage_rejected",
  "tracker_docs_ready", "tracker_docs_more", "tracker_timeline_show", "tracker_timeline_hide",
];

const unusedKeys = [...EN_KEYS].filter(
  (k) => !usedKeys.has(k) && !k.startsWith("gen_") && !KNOWN_DYNAMIC_KEYS.includes(k)
);
if (unusedKeys.length) {
  warn(`Possibly unused keys: ${unusedKeys.join(", ")}`);
} else {
  pass("No unexpected unused keys detected");
}

// ─── Summary ─────────────────────────────────────────────────────────────
console.log("\n" + "─".repeat(52));
if (hasError) {
  console.error(`\n❌ i18n validation FAILED. Fix errors above.\n`);
  process.exit(1);
} else {
  console.log(`\n✅ i18n validation PASSED`);
  console.log(`   EN: ${EN_KEYS.size} keys | HI: ${Object.keys(TRANSLATIONS.hi).length} keys | TA: ${Object.keys(TRANSLATIONS.ta).length} keys | TE: ${Object.keys(TRANSLATIONS.te).length} keys | KN: ${Object.keys(TRANSLATIONS.kn).length} keys | Used in JSX: ${usedKeys.size} keys\n`);
}
