import json
import os
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

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

            proc_md = mode_item.get("process_md")
            if proc_md and isinstance(proc_md, str) and proc_md.strip():
                proc_text = proc_md.strip()
            else:
                proc_text = extract_slate_text(mode_item.get("process")).strip()

            if proc_text:
                header = f"### Application Mode: {mode_name}"
                if url and url.strip():
                    header += f" (URL: {url.strip()})"
                mode_blocks.append(f"{header}\n{proc_text}")

        return "\n\n".join(mode_blocks)
    
    return extract_slate_text(app_node).strip()

def parse_benefits_text_list(ben_node):
    if ben_node is None:
        return []
    if isinstance(ben_node, str):
        return [ben_node.strip()] if ben_node.strip() else []
    
    items = []
    if isinstance(ben_node, list):
        for sub in ben_node:
            txt = extract_slate_text(sub).strip()
            if txt:
                for line in txt.split("\n"):
                    cleaned = line.strip()
                    if cleaned:
                        items.append(cleaned)
    elif isinstance(ben_node, dict):
        txt = extract_slate_text(ben_node).strip()
        if txt:
            items.append(txt)
            
    return items

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

out = []
out.append("==================================================")
out.append("DEEP SLATE.JS EXTRACTION DRY-RUN RESULTS:")
out.append("==================================================")
out.append(f"Total Valid Real Detail JSON Files : {total_valid}")
out.append(f"\n--- APPLICATION PROCESS ---")
out.append(f"Populated Count with Slate/MD Extractor: {app_proc_populated} / {total_valid} ({app_proc_populated/total_valid*100:.2f}%)")
out.append(f"Missing Count                          : {app_proc_missing}")

out.append(f"\n--- SCHEME BENEFITS ---")
out.append(f"Schemes with Benefits                  : {benefits_populated_schemes} / {total_valid} ({benefits_populated_schemes/total_valid*100:.2f}%)")
out.append(f"Total Benefit Bullet Points Extracted  : {benefits_total_items}")

out.append("\n--- SAMPLE APPLICATION PROCESS EXTRACTIONS ---")
for s in sample_apps:
    out.append(f"\nFile: {s['file']} (Length: {s['length']} chars)")
    out.append(f"Snippet:\n{s['text_snippet']}")

out.append("\n--- SAMPLE BENEFITS EXTRACTIONS ---")
for s in sample_bens:
    out.append(f"\nFile: {s['file']} (Item Count: {s['item_count']})")
    out.append(f"Items: {s['items_sample']}")

report_txt = "\n".join(out)
print(report_txt)

os.makedirs(r"d:\schemeBridge\myscheme-data\logs", exist_ok=True)
with open(r"d:\schemeBridge\myscheme-data\logs\phase2b_shapes_report.txt", "w", encoding="utf-8") as f:
    f.write(report_txt)
