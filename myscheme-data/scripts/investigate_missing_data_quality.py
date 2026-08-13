import os
import json
import requests
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

DETAILS_DIR = r"D:\schemeBridge\myscheme-data\raw\details"
BASE_URL = "http://localhost:8082/api/v1/schemes"

def main():
    print("==================================================")
    print("PHASE 2B - FINAL DATA QUALITY INVESTIGATION")
    print("==================================================")
    
    # Step 1: Fetch all schemes from Core Service REST API
    print("Fetching all schemes from Core Service REST API...")
    r = requests.get(f"{BASE_URL}/all")
    if r.status_code != 200:
        print(f"Error fetching schemes: HTTP {r.status_code}")
        return
        
    all_schemes = r.json().get("data", [])
    total_db_schemes = len(all_schemes)
    real_schemes = [s for s in all_schemes if s.get("slug") is not None]
    seed_schemes = [s for s in all_schemes if s.get("slug") is None]
    
    print(f"Total schemes in DB : {total_db_schemes}")
    print(f"Real schemes in DB  : {len(real_schemes)}")
    print(f"Seed schemes in DB  : {len(seed_schemes)}")
    
    # Build slug to DB record map
    db_scheme_by_slug = {}
    for s in real_schemes:
        slug = s.get("slug")
        if slug:
            db_scheme_by_slug[slug] = s
            
    print(f"Mapped {len(db_scheme_by_slug)} real schemes by slug from REST API.")
    
    # We will fetch detailed representation of all real schemes to get applicationProcess and benefits
    print("Fetching detail objects for all real schemes...")
    db_details = {} # slug -> detail_dto
    
    for idx, s in enumerate(real_schemes):
        s_id = s.get("id")
        slug = s.get("slug")
        r_det = requests.get(f"{BASE_URL}/{s_id}")
        if r_det.status_code == 200:
            db_details[slug] = r_det.json().get("data", {})
        if (idx + 1) % 500 == 0 or (idx + 1) == len(real_schemes):
            print(f"Fetched {idx+1}/{len(real_schemes)} DB scheme details...")

    # Identify missing application process and missing benefits schemes in DB
    db_missing_app_process_slugs = set()
    db_missing_benefits_slugs = set()
    
    for slug, d in db_details.items():
        app_p = d.get("applicationProcess")
        if not app_p or not str(app_p).strip():
            db_missing_app_process_slugs.add(slug)
            
        bens = d.get("benefits", [])
        if not bens or len(bens) == 0:
            db_missing_benefits_slugs.add(slug)
            
    print(f"\nDB Audit Summary:")
    print(f"DB Schemes missing Application Process: {len(db_missing_app_process_slugs)}")
    print(f"DB Schemes missing Benefits          : {len(db_missing_benefits_slugs)}")

    # ----------------------------------------------------
    # TASK 1 & 2: SOURCE JSON INVESTIGATION FOR ALL 4,675 FILES
    # ----------------------------------------------------
    # We also inspect source JSON files to verify every single file!
    
    valid_files_count = 0
    null_payload_files = []
    
    # Application process source shape trackers for missing 847
    app_proc_shapes_for_missing = {} # shape_name -> list of (slug, summary)
    app_proc_shapes_all = {}
    
    # Benefits source shape trackers for missing 848
    benefits_shapes_for_missing = {}
    benefits_shapes_all = {}

    # Source extraction simulator to check if parser CAN extract text
    # Let's inspect SlateJS extraction for applicationProcess and benefits
    
    def simulate_app_proc_extraction(app_proc_node):
        if app_proc_node is None:
            return None, "NULL_NODE"
        if not isinstance(app_proc_node, list):
            return None, "NOT_A_LIST"
        if len(app_proc_node) == 0:
            return None, "EMPTY_LIST"
            
        extracted_text = []
        for item in app_proc_node:
            if isinstance(item, dict):
                p_md = item.get("process_md")
                if p_md and str(p_md).strip():
                    extracted_text.append(str(p_md).strip())
                    continue
                proc = item.get("process")
                if proc:
                    text_from_proc = extract_slate_text(proc)
                    if text_from_proc and text_from_proc.strip():
                        extracted_text.append(text_from_proc.strip())
                        continue
        full_text = "\n".join(extracted_text).strip()
        if full_text:
            return full_text, "USABLE_TEXT"
        else:
            return None, "NO_TEXT_EXTRACTED"

    def extract_slate_text(node):
        if node is None:
            return ""
        if isinstance(node, str):
            return node
        if isinstance(node, (int, float, bool)):
            return str(node)
        if isinstance(node, list):
            res = []
            for elem in node:
                t = extract_slate_text(elem)
                if t:
                    res.append(t)
            return "\n".join(res)
        if isinstance(node, dict):
            # If text node
            if "text" in node:
                return str(node["text"])
            # Recursively check children
            if "children" in node and isinstance(node["children"], list):
                res = []
                for child in node["children"]:
                    t = extract_slate_text(child)
                    if t:
                        res.append(t)
                return "\n".join(res)
        return ""

    def simulate_benefits_extraction(benefits_node):
        if benefits_node is None:
            return [], "NULL_NODE"
        if not isinstance(benefits_node, list):
            return [], "NOT_A_LIST"
        if len(benefits_node) == 0:
            return [], "EMPTY_LIST"
            
        items = []
        for b_elem in benefits_node:
            t = extract_slate_text(b_elem).strip()
            if t:
                # split by newlines if bullet list
                lines = [l.strip() for l in t.split("\n") if l.strip()]
                items.extend(lines)
        if len(items) > 0:
            return items, "USABLE_BENEFITS"
        else:
            return [], "NO_BENEFITS_EXTRACTED"

    # Iterate through all raw JSON files
    json_files = [f for f in os.listdir(DETAILS_DIR) if f.endswith(".json")]
    
    app_proc_usable_count = 0
    app_proc_genuinely_missing_count = 0
    app_proc_parser_gap_count = 0

    benefits_usable_count = 0
    benefits_genuinely_missing_count = 0
    benefits_parser_gap_count = 0

    missing_app_proc_analysis = []
    missing_benefits_analysis = []

    for f_name in json_files:
        slug = f_name[:-5]
        f_path = os.path.join(DETAILS_DIR, f_name)
        with open(f_path, "r", encoding="utf-8") as fp:
            try:
                data_wrap = json.load(fp)
            except Exception as e:
                continue
                
        d_raw = data_wrap.get("data")
        if not d_raw:
            null_payload_files.append(slug)
            continue
            
        valid_files_count += 1
        en = d_raw.get("en", {})
        
        # 1. APP PROCESS INSPECTION
        app_proc_raw = en.get("applicationProcess")
        app_text, app_status = simulate_app_proc_extraction(app_proc_raw)
        
        is_db_missing_app = slug in db_missing_app_process_slugs
        
        if app_text:
            app_proc_usable_count += 1
            if is_db_missing_app:
                app_proc_parser_gap_count += 1
                missing_app_proc_analysis.append({
                    "slug": slug,
                    "reason": "PARSER_GAP",
                    "raw": app_proc_raw,
                    "extracted": app_text[:100]
                })
        else:
            app_proc_genuinely_missing_count += 1
            if is_db_missing_app:
                # categorize missing structure
                shape_key = ""
                if app_proc_raw is None:
                    shape_key = "NULL_OR_ABSENT"
                elif isinstance(app_proc_raw, list):
                    if len(app_proc_raw) == 0:
                        shape_key = "EMPTY_ARRAY_[]"
                    else:
                        # Inspect items inside list
                        item_types = [type(x).__name__ for x in app_proc_raw]
                        shape_key = f"ARRAY_OF_{'_'.join(set(item_types))}_EMPTY_CONTENT"
                else:
                    shape_key = f"NON_LIST_TYPE_{type(app_proc_raw).__name__}"
                    
                app_proc_shapes_for_missing[shape_key] = app_proc_shapes_for_missing.get(shape_key, 0) + 1
                missing_app_proc_analysis.append({
                    "slug": slug,
                    "reason": "GENUINELY_ABSENT",
                    "shape": shape_key,
                    "raw": str(app_proc_raw)[:200]
                })

        # 2. BENEFITS INSPECTION
        sc = en.get("schemeContent", {}) if isinstance(en, dict) else {}
        benefits_raw = sc.get("benefits") if isinstance(sc, dict) else None
        
        ben_items, ben_status = simulate_benefits_extraction(benefits_raw)
        is_db_missing_ben = slug in db_missing_benefits_slugs
        
        if len(ben_items) > 0:
            benefits_usable_count += 1
            if is_db_missing_ben:
                benefits_parser_gap_count += 1
                missing_benefits_analysis.append({
                    "slug": slug,
                    "reason": "PARSER_GAP",
                    "raw": str(benefits_raw)[:200],
                    "extracted": ben_items[:2]
                })
        else:
            benefits_genuinely_missing_count += 1
            if is_db_missing_ben:
                shape_key = ""
                if benefits_raw is None:
                    shape_key = "NULL_OR_ABSENT"
                elif isinstance(benefits_raw, list):
                    if len(benefits_raw) == 0:
                        shape_key = "EMPTY_ARRAY_[]"
                    else:
                        shape_key = "ARRAY_WITH_EMPTY_SLATE_AST"
                else:
                    shape_key = f"NON_LIST_TYPE_{type(benefits_raw).__name__}"
                    
                benefits_shapes_for_missing[shape_key] = benefits_shapes_for_missing.get(shape_key, 0) + 1
                missing_benefits_analysis.append({
                    "slug": slug,
                    "reason": "GENUINELY_ABSENT",
                    "shape": shape_key,
                    "raw": str(benefits_raw)[:200]
                })

    print("\n==================================================")
    print("DETAILED ANALYSIS RESULTS")
    print("==================================================")
    print(f"Total Source JSON Files Analyzed : {valid_files_count} valid + {len(null_payload_files)} null = {valid_files_count + len(null_payload_files)}")
    print(f"Valid Real Scheme Files         : {valid_files_count}")
    
    print("\n--- APPLICATION PROCESS ANALYSIS ---")
    print(f"Total Valid Real Schemes        : {valid_files_count}")
    print(f"DB Populated                    : {len(real_schemes) - len(db_missing_app_process_slugs)}")
    print(f"DB Missing                      : {len(db_missing_app_process_slugs)}")
    print(f"Source Usable Process           : {app_proc_usable_count}")
    print(f"Source Genuinely Missing        : {app_proc_genuinely_missing_count}")
    print(f"Parser Gap (Source usable but DB missing): {app_proc_parser_gap_count}")
    print(f"Breakdown of DB Missing 847 by Raw JSON Shape:")
    for k, v in sorted(app_proc_shapes_for_missing.items()):
        print(f"  - {k}: {v} files")

    print("\n--- BENEFITS ANALYSIS ---")
    print(f"Total Valid Real Schemes        : {valid_files_count}")
    print(f"DB Schemes with Benefits        : {len(real_schemes) - len(db_missing_benefits_slugs)}")
    print(f"DB Schemes without Benefits     : {len(db_missing_benefits_slugs)}")
    print(f"Source Usable Benefits          : {benefits_usable_count}")
    print(f"Source Genuinely Missing        : {benefits_genuinely_missing_count}")
    print(f"Parser Gap (Source usable but DB missing): {benefits_parser_gap_count}")
    print(f"Breakdown of DB Missing 848 by Raw JSON Shape:")
    for k, v in sorted(benefits_shapes_for_missing.items()):
        print(f"  - {k}: {v} files")

    # Sample representative files for missing applicationProcess
    print("\nRepresentative Examples of Missing APPLICATION_PROCESS:")
    sample_app = [item for item in missing_app_proc_analysis if item["reason"] == "GENUINELY_ABSENT"][:5]
    for s in sample_app:
        print(f"  Slug: {s['slug']} | Shape: {s['shape']} | Raw: {s['raw']}")

    # Sample representative files for missing benefits
    print("\nRepresentative Examples of Missing BENEFITS:")
    sample_ben = [item for item in missing_benefits_analysis if item["reason"] == "GENUINELY_ABSENT"][:5]
    for s in sample_ben:
        print(f"  Slug: {s['slug']} | Shape: {s['shape']} | Raw: {s['raw']}")

if __name__ == "__main__":
    main()
