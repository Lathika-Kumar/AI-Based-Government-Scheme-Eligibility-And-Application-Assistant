import os
import json
import requests
import sys
from concurrent.futures import ThreadPoolExecutor

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

DETAILS_DIR = r"D:\schemeBridge\myscheme-data\raw\details"
BASE_URL = "http://localhost:8082/api/v1/schemes"

def fetch_detail(scheme):
    s_id = scheme.get("id")
    slug = scheme.get("slug")
    try:
        r = requests.get(f"{BASE_URL}/{s_id}", timeout=10)
        if r.status_code == 200:
            return slug, r.json().get("data", {})
    except Exception as e:
        pass
    return slug, None

def main():
    print("==================================================")
    print("FAST DATA QUALITY INVESTIGATION (THREADED)")
    print("==================================================")
    
    r = requests.get(f"{BASE_URL}/all")
    all_schemes = r.json().get("data", [])
    real_schemes = [s for s in all_schemes if s.get("slug") is not None]
    seed_schemes = [s for s in all_schemes if s.get("slug") is None]
    
    print(f"Total schemes in DB : {len(all_schemes)}")
    print(f"Real schemes in DB  : {len(real_schemes)}")
    print(f"Seed schemes in DB  : {len(seed_schemes)}")
    
    db_details = {}
    print(f"Fetching details for {len(real_schemes)} real schemes using 30 threads...")
    with ThreadPoolExecutor(max_workers=30) as executor:
        results = executor.map(fetch_detail, real_schemes)
        for slug, data in results:
            if slug and data:
                db_details[slug] = data

    print(f"Successfully retrieved {len(db_details)} DB details.")

    # Identify missing application process and missing benefits schemes in DB
    db_missing_app_proc_slugs = set()
    db_missing_benefits_slugs = set()
    
    for slug, d in db_details.items():
        app_p = d.get("applicationProcess")
        if not app_p or not str(app_p).strip():
            db_missing_app_proc_slugs.add(slug)
            
        bens = d.get("benefits", [])
        if not bens or len(bens) == 0:
            db_missing_benefits_slugs.add(slug)
            
    print(f"\nDB Audit Metrics:")
    print(f"DB Schemes missing Application Process: {len(db_missing_app_proc_slugs)}")
    print(f"DB Schemes missing Benefits          : {len(db_missing_benefits_slugs)}")

    # helper to extract text from SlateJS AST
    def extract_slate_text(node):
        if node is None:
            return ""
        if isinstance(node, str):
            return node
        if isinstance(node, (int, float, bool)):
            return str(node)
        if isinstance(node, list):
            res = [extract_slate_text(elem) for elem in node]
            return "\n".join([r for r in res if r])
        if isinstance(node, dict):
            if "text" in node:
                return str(node["text"])
            if "children" in node and isinstance(node["children"], list):
                res = [extract_slate_text(child) for child in node["children"]]
                return "\n".join([r for r in res if r])
        return ""

    def get_app_proc_usable_text(app_proc_node):
        if not app_proc_node or not isinstance(app_proc_node, list) or len(app_proc_node) == 0:
            return None
        texts = []
        for item in app_proc_node:
            if isinstance(item, dict):
                p_md = item.get("process_md")
                if p_md and str(p_md).strip():
                    texts.append(str(p_md).strip())
                    continue
                proc = item.get("process")
                if proc:
                    t = extract_slate_text(proc)
                    if t and t.strip():
                        texts.append(t.strip())
                        continue
        full = "\n".join(texts).strip()
        return full if full else None

    def get_benefits_usable_items(benefits_node):
        if not benefits_node or not isinstance(benefits_node, list) or len(benefits_node) == 0:
            return []
        items = []
        for elem in benefits_node:
            t = extract_slate_text(elem).strip()
            if t:
                lines = [l.strip() for l in t.split("\n") if l.strip()]
                items.extend(lines)
        return items

    # Analyze all 4,675 raw JSON files
    json_files = [f for f in os.listdir(DETAILS_DIR) if f.endswith(".json")]
    
    app_proc_usable_in_source = 0
    app_proc_genuinely_missing_in_source = 0
    app_proc_parser_gap = 0

    benefits_usable_in_source = 0
    benefits_genuinely_missing_in_source = 0
    benefits_parser_gap = 0

    app_proc_shapes = {}
    benefits_shapes = {}

    missing_app_examples = {}
    missing_benefits_examples = {}

    for f_name in json_files:
        slug = f_name[:-5]
        f_path = os.path.join(DETAILS_DIR, f_name)
        with open(f_path, "r", encoding="utf-8") as fp:
            try:
                data_wrap = json.load(fp)
            except Exception:
                continue
                
        d_raw = data_wrap.get("data")
        if not d_raw:
            continue # null payload
            
        en = d_raw.get("en", {})
        
        # 1. Application process analysis
        app_proc_raw = en.get("applicationProcess")
        app_usable = get_app_proc_usable_text(app_proc_raw)
        
        if app_usable:
            app_proc_usable_in_source += 1
            if slug in db_missing_app_proc_slugs:
                app_proc_parser_gap += 1
        else:
            app_proc_genuinely_missing_in_source += 1
            if slug in db_missing_app_proc_slugs:
                # Shape classification
                shape = ""
                if app_proc_raw is None:
                    shape = "NULL_OR_ABSENT"
                elif isinstance(app_proc_raw, list):
                    if len(app_proc_raw) == 0:
                        shape = "EMPTY_ARRAY_[]"
                    else:
                        shape = "ARRAY_OF_EMPTY_OBJECTS"
                else:
                    shape = f"UNEXPECTED_TYPE_{type(app_proc_raw).__name__}"
                    
                app_proc_shapes[shape] = app_proc_shapes.get(shape, 0) + 1
                if shape not in missing_app_examples:
                    missing_app_examples[shape] = (slug, str(app_proc_raw)[:150])

        # 2. Benefits analysis
        sc = en.get("schemeContent", {}) if isinstance(en, dict) else {}
        benefits_raw = sc.get("benefits") if isinstance(sc, dict) else None
        
        ben_usable = get_benefits_usable_items(benefits_raw)
        if len(ben_usable) > 0:
            benefits_usable_in_source += 1
            if slug in db_missing_benefits_slugs:
                benefits_parser_gap += 1
        else:
            benefits_genuinely_missing_in_source += 1
            if slug in db_missing_benefits_slugs:
                shape = ""
                if benefits_raw is None:
                    shape = "NULL_OR_ABSENT"
                elif isinstance(benefits_raw, list):
                    if len(benefits_raw) == 0:
                        shape = "EMPTY_ARRAY_[]"
                    else:
                        shape = "ARRAY_OF_EMPTY_AST_NODES"
                else:
                    shape = f"UNEXPECTED_TYPE_{type(benefits_raw).__name__}"
                    
                benefits_shapes[shape] = benefits_shapes.get(shape, 0) + 1
                if shape not in missing_benefits_examples:
                    missing_benefits_examples[shape] = (slug, str(benefits_raw)[:150])

    print("\n==================================================")
    print("RECONCILIATION SUMMARY")
    print("==================================================")
    print("\n--- APPLICATION PROCESS ---")
    print(f"Total Valid Real Schemes              : {len(db_details)}")
    print(f"Database Populated (`APPLICATION_PROCESS`): {len(db_details) - len(db_missing_app_proc_slugs)}")
    print(f"Database Missing (`APPLICATION_PROCESS`)  : {len(db_missing_app_proc_slugs)}")
    print(f"Source Contains Usable Process         : {app_proc_usable_in_source}")
    print(f"Source Genuinely Missing Process       : {app_proc_genuinely_missing_in_source}")
    print(f"Parser Gap Count                       : {app_proc_parser_gap}")
    print("\nMissing 847 APPLICATION_PROCESS Shape Breakdown:")
    for k, v in sorted(app_proc_shapes.items()):
        print(f"  - {k}: {v} files")
        ex_slug, ex_raw = missing_app_examples[k]
        print(f"    Example Slug ({ex_slug}): {ex_raw}")

    print("\n--- BENEFITS ---")
    print(f"Total Valid Real Schemes              : {len(db_details)}")
    print(f"Database Schemes with Benefit Rows    : {len(db_details) - len(db_missing_benefits_slugs)}")
    print(f"Database Schemes without Benefit Rows : {len(db_missing_benefits_slugs)}")
    print(f"Source Contains Usable Benefits       : {benefits_usable_in_source}")
    print(f"Source Genuinely Missing Benefits     : {benefits_genuinely_missing_in_source}")
    print(f"Parser Gap Count                      : {benefits_parser_gap}")
    print("\nMissing 848 BENEFITS Shape Breakdown:")
    for k, v in sorted(benefits_shapes.items()):
        print(f"  - {k}: {v} files")
        ex_slug, ex_raw = missing_benefits_examples[k]
        print(f"    Example Slug ({ex_slug}): {ex_raw}")

if __name__ == "__main__":
    main()
