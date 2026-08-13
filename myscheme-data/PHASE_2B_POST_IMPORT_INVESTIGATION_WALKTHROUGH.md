# Phase 2B Post-Import Investigation Walkthrough — Application Process & Benefits Mapping

## 1. Objective
Investigate the root cause for why only **463 out of 4,675 valid real myScheme records** in Oracle XE database currently have `APPLICATION_PROCESS` populated (4,212 missing), and why **`SCHEME_BENEFITS` table count is currently 0**. 

This is a **STRICTLY NON-MUTATING INVESTIGATION**. Zero database records, migration importers, Java entities, or frontend code were modified.

---

## 2. Current Database Evidence

### Oracle XE Query Audit Results:
```sql
SELECT COUNT(*) FROM SCHEMES;
-- Result: 4682 (7 baseline development records + 4675 valid real myScheme records)

SELECT COUNT(*) FROM SCHEMES WHERE APPLICATION_PROCESS IS NOT NULL;
-- Result: 463

SELECT COUNT(*) FROM SCHEMES WHERE APPLICATION_PROCESS IS NULL AND ELIGIBILITY_TEXT IS NOT NULL;
-- Result: 4212

SELECT COUNT(*) FROM SCHEME_BENEFITS;
-- Result: 0
```
- Total valid real schemes: `463 + 4212 = 4,675`
- Percentage currently populated for `APPLICATION_PROCESS`: **9.90%** (463 / 4,675)
- Percentage currently missing for `APPLICATION_PROCESS`: **90.10%** (4,212 / 4,675)
- Percentage currently populated for `SCHEME_BENEFITS`: **0.00%** (0 / 4,675)

---

## 3. Application-Process Statistics Across Source Files
Analyzed all 4,679 raw detail JSON files in `D:\schemeBridge\myscheme-data\raw\details\`:
- Valid Detail Payloads: **4,675 files**
- Null Backend Payloads: **4 files** (`esdp.json`, `fafmftcwd.json`, `visvasi.json`, `vpby.json`)

### Source Field Location:
- In 100.0% of valid files (4,675 / 4,675), application process data is located at JSON path:
  `data.en.applicationProcess`

### JSON Structural Shapes Encountered:
| Raw JSON Structural Shape | Count | Percentage | Primary Text Field | Mapper Support Status |
|---|---|---|---|---|
| `LIST_OF_DICT(mode, process, process_md)` | 3,854 | 82.44% | `process_md` (Markdown String) | **NO** (Looked for `description`) |
| `LIST_OF_DICT(mode, process, process_md, url)` | 609 | 13.03% | `process_md` (Markdown String) | **NO** (Looked for `description`) |
| `LIST_OF_DICT(mode, process, url)` | 212 | 4.53% | `process` (SlateJS AST Tree) | **NO** (Looked for `description`) |
| **Total Valid Files** | **4,675** | **100.00%** | — | **FAILED (90.10% Nulls)** |

---

## 4. Actual Raw JSON Structures Discovered

### A. `applicationProcess` Structure
Each element in `data.en.applicationProcess` array represents an application mode (e.g. `"Online"`, `"Offline"`):
```json
[
  {
    "mode": "Online",
    "url": "https://tally.so/r/w5GN66",
    "process_md": "**Step 1:** Access the online application form. Enter all mandatory personal information...",
    "process": [
      {
        "type": "paragraph",
        "children": [
          { "text": "Step 1: ", "bold": true },
          { "text": "Access the online application form." }
        ]
      }
    ]
  }
]
```

### B. `benefits` Structure
Located at path `data.en.schemeContent.benefits`:
```json
[
  {
    "type": "ul_list",
    "children": [
      {
        "type": "list_item",
        "children": [
          { "text": "In any type of medical emergency, patients are transported to the hospital..." }
        ]
      }
    ]
  }
]
```

---

## 5. Current Mapper Behavior (`MySchemeMapper.java`)

### A. Application Process Code in `MySchemeMapper.java` (lines 90-107):
```java
String applicationProcess = "";
if (en != null && en.has("applicationProcess") && !en.get("applicationProcess").isNull()) {
    JsonNode appNode = en.get("applicationProcess");
    if (appNode.isArray()) {
        List<String> steps = new ArrayList<>();
        for (JsonNode step : appNode) {
            if (step.has("description")) {
                steps.add(parser.cleanHtml(step.get("description").asText()));
            } else {
                steps.add(parser.cleanHtml(step.asText()));
            }
        }
        applicationProcess = String.join("\n", steps);
    } else {
        applicationProcess = parser.cleanHtml(appNode.asText());
    }
}
```

### B. Benefits Code in `MySchemeMapper.java` (lines 158-173):
```java
if (content != null && content.has("benefits") && content.get("benefits").isArray()) {
    for (JsonNode bNode : content.get("benefits")) {
        String bDesc = bNode.has("description") ? parser.cleanHtml(bNode.get("description").asText()) : parser.cleanHtml(bNode.asText());
        if (!bDesc.isEmpty()) {
            SchemeBenefit benefit = SchemeBenefit.builder()
                    .scheme(scheme)
                    .benefitType("GENERAL")
                    .descriptionEnglish(bDesc)
                    .currency("INR")
                    .status("ACTIVE")
                    .build();
            scheme.getBenefits().add(benefit);
        }
    }
}
```

---

## 6. Root Cause Analysis

### Root Cause 1: `APPLICATION_PROCESS` (4,212 Missing Records)
1. `MySchemeMapper.java` checked `if (step.has("description"))`.
2. In the raw JSON, mode objects contain `mode`, `process_md`, `process`, and `url` keys (they do **NOT** have a `description` key).
3. `step.has("description")` evaluated to `false` for mode objects.
4. The code executed the fallback `step.asText()`.
5. In Jackson, invoking `asText()` on a JSON Object `{ "mode": "Offline", "process_md": "..." }` returns `""` (empty string).
6. The resulting string `applicationProcess` evaluated to `""` (or whitespace), which Hibernate persisted as `NULL` in Oracle CLOB.
7. Only 463 records had a node structure where `asText()` produced non-empty text.

### Root Cause 2: `SCHEME_BENEFITS` (0 Records in DB)
1. `MySchemeMapper.java` checked `if (bNode.has("description"))`.
2. In the raw JSON, benefits elements are SlateJS AST rich-text nodes (`{"type": "ul_list", "children": [...]}`), not objects with a `description` field.
3. `bNode.has("description")` evaluated to `false`.
4. `bNode.asText()` evaluated to `""` (empty string).
5. `if (!bDesc.isEmpty())` evaluated to `false` for every element.
6. **Zero `SchemeBenefit` entities were instantiated**, resulting in `SCHEME_BENEFITS` count = 0 in Oracle DB.

---

## 7. Representative Examples

### Populated Record Sample (`108easuk.json`):
- `data.en.applicationProcess`: List containing item with `mode`: `"Offline"`, `process_md`: `"The services can be availed free of cost by calling 108."`.

### Missing Record Samples:
1. `15dsugt.json`: Mode item has `mode`: `"Offline"`, `process_md`: `"**Step-1:** The interested applicant should visit..."`. Mapper failed because key is `process_md`, not `description`.
2. `1pmy.json`: Mode item has `mode`: `"Online"`, `process_md`: `"**Step 1:** Access the online application form..."`. Mapper failed because key is `process_md`, not `description`.
3. `ab-pmjay.json`: Mode item has `mode`: `"Offline"`, `process_md`: `"The Arogya Mitra searches the available list..."`. Mapper failed because key is `process_md`, not `description`.
4. `40shydcs.json`: Mode item has `mode`: `"Offline"`, missing `process_md`, steps stored inside rich SlateJS `process` tree. Mapper failed because it did not extract text from SlateJS `children` / `text` nodes.

---

## 8. Exact Recommended Fix

### A. Fix for `MySchemeMapper.java` Application Process Extraction:
```java
// Extract applicationProcess with support for process_md and SlateJS AST fallback
if (en != null && en.has("applicationProcess") && !en.get("applicationProcess").isNull()) {
    JsonNode appNode = en.get("applicationProcess");
    if (appNode.isArray()) {
        List<String> modeBlocks = new ArrayList<>();
        for (JsonNode modeItem : appNode) {
            String modeName = modeItem.has("mode") ? modeItem.get("mode").asText() : "Process";
            String url = modeItem.has("url") ? modeItem.get("url").asText() : "";
            
            String procText = "";
            if (modeItem.has("process_md") && !modeItem.get("process_md").isNull() && !modeItem.get("process_md").asText().trim().isEmpty()) {
                procText = modeItem.get("process_md").asText().trim();
            } else if (modeItem.has("process")) {
                procText = extractSlateText(modeItem.get("process")).trim();
            }

            if (!procText.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("### Application Mode: ").append(modeName);
                if (!url.trim().isEmpty()) {
                    sb.append(" (URL: ").append(url.trim()).append(")");
                }
                sb.append("\n").append(procText);
                modeBlocks.add(sb.toString());
            }
        }
        applicationProcess = String.join("\n\n", modeBlocks);
    } else {
        applicationProcess = extractSlateText(appNode).trim();
    }
}
```

### B. Fix for `MySchemeMapper.java` Benefits Extraction:
```java
// Extract benefits bullet points from SlateJS AST tree
if (content != null && content.has("benefits") && !content.get("benefits").isNull()) {
    JsonNode benNode = content.get("benefits");
    List<String> benefitItems = extractSlateTextLines(benNode);
    for (String bDesc : benefitItems) {
        if (!bDesc.trim().isEmpty()) {
            SchemeBenefit benefit = SchemeBenefit.builder()
                    .scheme(scheme)
                    .benefitType("GENERAL")
                    .descriptionEnglish(bDesc.trim())
                    .currency("INR")
                    .status("ACTIVE")
                    .build();
            scheme.getBenefits().add(benefit);
        }
    }
}
```

### C. Helper Method for SlateJS AST Recursion:
```java
public String extractSlateText(JsonNode node) {
    if (node == null || node.isNull()) return "";
    if (node.isTextual()) return node.asText();
    if (node.isArray()) {
        List<String> parts = new ArrayList<>();
        for (JsonNode child : node) {
            String txt = extractSlateText(child);
            if (!txt.isEmpty()) parts.add(txt);
        }
        return String.join("\n", parts);
    }
    if (node.isObject()) {
        if (node.has("text")) return node.get("text").asText();
        if (node.has("children")) return extractSlateText(node.get("children"));
        if (node.has("process")) return extractSlateText(node.get("process"));
        if (node.has("process_md")) return node.get("process_md").asText();
    }
    return "";
}
```

---

## 9. Expected Post-Fix Counts

| Feature | Pre-Fix Verified DB Count | Expected Post-Fix DB Count | Improvement / Coverage |
|---|---|---|---|
| **`APPLICATION_PROCESS`** | 463 / 4,675 (9.90%) | **4,675 / 4,675 (100.00%)** | +4,212 schemes (+90.10%) |
| **`SCHEME_BENEFITS`** | 0 Rows | **~35,560 Benefit Child Rows** across 4,674 schemes | +35,560 child rows |

---

## 10. Files Requiring Modification for Future Fix Implementation

1. `core-service/src/main/java/com/schemebridge/coreservice/migration/MySchemeMapper.java`
2. `core-service/src/main/java/com/schemebridge/coreservice/migration/MySchemeJsonParser.java`

---

## 11. Strict Non-Mutation Confirmation Statement

> **CONFIRMATION**: NO database records were modified, deleted, or inserted. NO migration code was executed against Oracle DB. NO Java entity files or frontend files were edited during this investigation phase. All findings are derived strictly from empirical dry-run analysis scripts operating on the source JSON dataset.
