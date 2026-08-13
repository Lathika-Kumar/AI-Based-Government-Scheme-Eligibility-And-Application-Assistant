import json
import os

raw_dir = r"d:\schemeBridge\myscheme-data\raw\details"

def extract_slate_text(node):
    """Recursively extract plain text from SlateJS rich-text node / list / string / dict."""
    if node is None:
        return ""
    if isinstance(node, str):
        return node
    if isinstance(node, list):
        return "\n".join(filter(None, (extract_slate_text(item) for item in node)))
    if isinstance(node, dict):
        if "text" in node:
            return node["text"]
        if "children" in node:
            return extract_slate_text(node["children"])
        if "process" in node:
            return extract_slate_text(node["process"])
        if "process_md" in node and isinstance(node["process_md"], str):
            return node["process_md"]
    return ""

def parse_application_process_text(app_node):
    """
    Parse applicationProcess from raw myScheme JSON payload.
    Supports list of modes with process_md, process SlateJS tree, or direct strings.
    """
    if app_node is None:
        return ""
    if isinstance(app_node, str):
        return app_node.strip()
    
    if isinstance(app_node, list):
        mode_blocks = []
        for mode_item in app_node:
            if not isinstance(mode_item, dict):
                txt = str(mode_item).strip()
                if txt:
                    mode_blocks.append(txt)
                continue

            mode_name = mode_item.get("mode") or "Process"
            url = mode_item.get("url") or ""

            # Try process_md first
            proc_md = mode_item.get("process_md")
            if proc_md and isinstance(proc_md, str) and proc_md.strip():
                proc_text = proc_md.strip()
            else:
                # Fallback to rich SlateJS process tree
                proc_text = extract_slate_text(mode_item.get("process")).strip()

            if proc_text:
                header = f"### Application Mode: {mode_name}"
                if url and url.strip():
                    header += f" (URL: {url.strip()})"
                mode_blocks.append(f"{header}\n{proc_text}")

        return "\n\n".join(mode_blocks)
    
    return extract_slate_text(app_node).strip()

def parse_benefits_text_list(ben_node):
    """
    Parse benefits from data.en.schemeContent.benefits SlateJS AST tree.
    Extracts individual bullet points / list items / paragraphs.
    """
    if ben_node is None:
        return []
    if isinstance(ben_node, str):
        return [ben_node.strip()] if ben_node.strip() else []
    
    items = []
    if isinstance(ben_node, list):
        for sub in ben_node:
            txt = extract_slate_text(sub).strip()
            if txt:
                # Split multiline list items if any
                for line in txt.split("\n"):
                    cleaned = line.strip()
                    if cleaned:
                        items.append(cleaned)
    elif isinstance(ben_node, dict):
        txt = extract_slate_text(ben_node).strip()
        if txt:
            items.append(txt)
            
    return items

# Perform full dry-run analysis on all 4,679 raw detail files
total_valid = 0
app_proc_populated = 0
app_proc_missing = 0

benefits_populated_schemes = 0
benefits_total_items = 0

sample_apps = []
sample_bens = []

for fname in os.listdir(raw_dir):
    if not fname.endswith(".json"):
        continue
    fpath = os.path.join(raw_dir, fname)
    try:
        with open(fpath, "r", encoding="utf-8") as f:
            root = json.load(f)
    except Exception:
        continue
        
    data = root.get("data")
    if not data or not isinstance(data, dict):
        continue
    total_valid += 1

    en = data.get("en") or {}
    content = en.get("schemeContent") or {}

    # Application Process Extraction
    app_raw = en.get("applicationProcess")
    app_text = parse_application_process_text(app_raw)

    if app_text and len(app_text.strip()) > 0:
        app_proc_populated += 1
        if len(sample_apps) < 3:
            sample_apps.append({
                "file": fname,
                "length": len(app_text),
                "text_snippet": app_text[:200]
            })
    else:
        app_proc_missing += 1

    # Benefits Extraction
    ben_raw = content.get("benefits")
    ben_list = parse_benefits_text_list(ben_raw)

    if ben_list:
        benefits_populated_schemes += 1
        benefits_total_items += len(ben_list)
        if len(sample_bens) < 3:
            sample_bens.append({
                "file": fname,
                "item_count": len(ben_list),
                "items_sample": ben_list[:3]
            })

print("==================================================")
print("DEEP SLATE.JS EXTRACTION DRY-RUN RESULTS:")
print("==================================================")
print(f"Total Valid Real Detail JSON Files : {total_valid}")
print(f"\n--- APPLICATION PROCESS ---")
print(f"Populated Count with Slate/MD Extractor: {app_proc_populated} / {total_valid} ({app_proc_populated/total_valid*100:.2f}%)")
print(f"Missing Count                          : {app_proc_missing}")

print(f"\n--- SCHEME BENEFITS ---")
print(f"Schemes with Benefits                  : {benefits_populated_schemes} / {total_valid} ({benefits_populated_schemes/total_valid*100:.2f}%)")
print(f"Total Benefit Bullet Points Extracted  : {benefits_total_items}")

print("\n--- SAMPLE APPLICATION PROCESS EXTRACTIONS ---")
for s in sample_apps:
    print(f"\nFile: {s['file']} (Length: {s['length']} chars)")
    print(f"Snippet:\n{s['text_snippet']}")

print("\n--- SAMPLE BENEFITS EXTRACTIONS ---")
for s in sample_bens:
    print(f"\nFile: {s['file']} (Item Count: {s['item_count']})")
    print(f"Items: {s['items_sample']}")
