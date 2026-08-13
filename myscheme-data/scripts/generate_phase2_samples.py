import os
import json
import re

def clean_html(text):
    if not text: return ""
    if isinstance(text, (dict, list)):
        return json.dumps(text, ensure_ascii=False)
    cleaned = re.sub(r'<[^>]+>', ' ', str(text))
    return ' '.join(cleaned.split())

def normalize_detail(raw_detail):
    if not raw_detail or "data" not in raw_detail:
        return None

    d = raw_detail["data"]
    if not isinstance(d, dict):
        return None

    en = d.get("en", {})
    basic = en.get("basicDetails", {})
    content = en.get("schemeContent", {})
    
    scheme_id = d.get("_id") or basic.get("schemeShortTitle") or basic.get("slug") or "UNKNOWN-ID"
    slug = basic.get("slug") or d.get("slug") or ""
    scheme_code = basic.get("schemeShortTitle") or slug.upper() or f"SCHEME-{str(scheme_id)[:8]}"
    
    title_en = basic.get("schemeName") or basic.get("schemeShortTitle") or content.get("schemeName") or ""
    desc_en = clean_html(basic.get("briefDescription") or basic.get("description") or content.get("briefDescription") or "")
    
    level_obj = basic.get("level", {})
    level_val = (level_obj.get("value") if isinstance(level_obj, dict) else str(level_obj)).upper()
    scheme_type = "CENTRAL" if "CENTRAL" in level_val else "STATE"
    
    category_name = ""
    cat_obj = basic.get("schemeCategory")
    if isinstance(cat_obj, list) and cat_obj:
        category_name = cat_obj[0].get("label") if isinstance(cat_obj[0], dict) else str(cat_obj[0])
    elif isinstance(cat_obj, dict):
        category_name = cat_obj.get("label") or cat_obj.get("name") or ""
    elif isinstance(cat_obj, str):
        category_name = cat_obj
        
    ministry_name = basic.get("nodalMinistryName") or basic.get("nodalDepartmentName") or basic.get("implementingAgency") or ""
    tags = basic.get("tags") or []
    dbt_scheme = basic.get("dbtScheme") is True or basic.get("dbtScheme") == "Yes"
    
    # State mapping
    state_obj = basic.get("state")
    applicable_states = "ALL_INDIA"
    if isinstance(state_obj, dict):
        applicable_states = state_obj.get("label") or state_obj.get("name") or "ALL_INDIA"
    elif isinstance(state_obj, str) and state_obj.strip():
        applicable_states = state_obj.strip()

    # Benefits
    benefits_list = []
    benefits_raw = en.get("benefits") or content.get("benefits")
    if isinstance(benefits_raw, list):
        for b in benefits_raw:
            text = clean_html(b.get("description") if isinstance(b, dict) else b)
            if text: benefits_list.append(text)
    elif isinstance(benefits_raw, str) and benefits_raw.strip():
        benefits_list.append(clean_html(benefits_raw))
        
    # Eligibility
    eligibility_rules = []
    elig_raw = en.get("eligibilityCriteria") or content.get("eligibility")
    if isinstance(elig_raw, list):
        for rule in elig_raw:
            rule_text = clean_html(rule.get("description") if isinstance(rule, dict) else rule)
            if rule_text:
                eligibility_rules.append({
                    "ruleType": "GENERAL",
                    "operator": "EQUALS",
                    "valueString": rule_text,
                    "description": rule_text,
                    "mandatory": True
                })
    elif isinstance(elig_raw, str) and elig_raw.strip():
        eligibility_rules.append({
            "ruleType": "GENERAL",
            "operator": "EQUALS",
            "valueString": clean_html(elig_raw),
            "description": clean_html(elig_raw),
            "mandatory": True
        })

    # Application Process
    app_proc_raw = en.get("applicationProcess") or content.get("applicationProcess")
    app_proc_text = ""
    if isinstance(app_proc_raw, list):
        steps = []
        for step in app_proc_raw:
            st = clean_html(step.get("description") if isinstance(step, dict) else step)
            if st: steps.append(st)
        app_proc_text = "\n".join(steps)
    elif isinstance(app_proc_raw, str):
        app_proc_text = clean_html(app_proc_raw)

    # FAQs
    faqs_list = []
    faqs_raw = en.get("faqs") or content.get("faqs")
    if isinstance(faqs_raw, list):
        for faq in faqs_raw:
            if isinstance(faq, dict):
                q = clean_html(faq.get("question") or faq.get("q"))
                a = clean_html(faq.get("answer") or faq.get("a"))
                if q and a:
                    faqs_list.append({"questionEnglish": q, "answerEnglish": a})

    normalized = {
        "id": scheme_id,
        "schemeCode": scheme_code,
        "slug": slug,
        "titleEnglish": title_en,
        "descriptionEnglish": desc_en,
        "categoryName": category_name,
        "departmentName": ministry_name,
        "schemeType": scheme_type,
        "applicableStates": applicable_states,
        "schemeUrl": f"https://www.myscheme.gov.in/schemes/{slug}",
        "dbtScheme": dbt_scheme,
        "tags": tags,
        "benefits": benefits_list,
        "eligibilityRules": eligibility_rules,
        "applicationProcess": app_proc_text,
        "faqs": faqs_list,
        "status": "ACTIVE"
    }

    return normalized

def create_phase2_samples():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    details_dir = os.path.join(base_dir, "raw", "details")
    norm_dir = os.path.join(base_dir, "normalized")
    os.makedirs(norm_dir, exist_ok=True)

    target_slugs = ["kcc", "sui", "pm-kisan", "pmay-u", "apy", "aamgsiscs", "pmuy", "ab-pmjay", "mgnrega", "ssy"]
    samples = []

    for slug in target_slugs:
        file_path = os.path.join(details_dir, f"{slug}.json")
        if os.path.exists(file_path):
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    raw = json.load(f)
                norm = normalize_detail(raw)
                if norm:
                    samples.append(norm)
            except Exception as e:
                print(f"Error processing {slug}: {e}")

    # Fallback to any 10 valid files if target slugs don't all exist
    if len(samples) < 10:
        all_files = [f for f in os.listdir(details_dir) if f.endswith(".json")]
        for f_name in all_files:
            if len(samples) >= 10: break
            f_path = os.path.join(details_dir, f_name)
            try:
                with open(f_path, "r", encoding="utf-8") as f:
                    raw = json.load(f)
                norm = normalize_detail(raw)
                if norm and norm["id"] not in [s["id"] for s in samples]:
                    samples.append(norm)
            except Exception:
                pass

    sample_out_file = os.path.join(norm_dir, "phase2-sample.json")
    with open(sample_out_file, "w", encoding="utf-8") as out:
        json.dump(samples, out, ensure_ascii=False, indent=2)

    print(f"Successfully generated {len(samples)} normalized scheme samples in {sample_out_file}")
    return len(samples)

if __name__ == "__main__":
    create_phase2_samples()
