import os
import json
import re

def clean_html(text):
    if not text: return ""
    if isinstance(text, (dict, list)):
        return json.dumps(text, ensure_ascii=False)
    cleaned = re.sub(r'<[^>]+>', ' ', str(text))
    return ' '.join(cleaned.split())

def normalize_scheme(raw_detail):
    if not raw_detail or "data" not in raw_detail:
        return None

    d = raw_detail["data"]
    if not isinstance(d, dict):
        return None

    en = d.get("en", {})
    basic = en.get("basicDetails", {})
    
    scheme_id = d.get("_id") or basic.get("schemeShortTitle") or basic.get("slug") or "UNKNOWN-ID"
    slug = basic.get("slug") or d.get("slug") or ""
    scheme_code = basic.get("schemeShortTitle") or slug.upper() or f"SCHEME-{str(scheme_id)[:8]}"
    
    title_en = basic.get("schemeName") or basic.get("schemeShortTitle") or ""
    desc_en = clean_html(basic.get("briefDescription") or basic.get("description") or "")
    
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
        
    ministry_name = basic.get("nodalMinistryName") or basic.get("implementingAgency") or ""
    tags = basic.get("tags") or []
    
    # Benefits
    benefits_list = []
    benefits_raw = en.get("benefits") or en.get("schemeContent", {}).get("benefits")
    if isinstance(benefits_raw, list):
        for b in benefits_raw:
            text = clean_html(b.get("description") if isinstance(b, dict) else b)
            if text: benefits_list.append(text)
    elif isinstance(benefits_raw, str) and benefits_raw.strip():
        benefits_list.append(clean_html(benefits_raw))
        
    # Eligibility
    eligibility_rules = []
    elig_raw = en.get("eligibilityCriteria") or en.get("schemeContent", {}).get("eligibility")
    if isinstance(elig_raw, list):
        for rule in elig_raw:
            rule_text = clean_html(rule.get("description") if isinstance(rule, dict) else rule)
            if rule_text:
                eligibility_rules.append({
                    "ruleName": "Eligibility Requirement",
                    "criterionType": "GENERAL",
                    "operator": "EQUALS",
                    "expectedValue": rule_text
                })
    elif isinstance(elig_raw, str) and elig_raw.strip():
        eligibility_rules.append({
            "ruleName": "Eligibility Requirement",
            "criterionType": "GENERAL",
            "operator": "EQUALS",
            "expectedValue": clean_html(elig_raw)
        })

    # Documents
    documents_list = []
    docs_raw = en.get("documentsRequired") or en.get("schemeContent", {}).get("documents")
    if isinstance(docs_raw, list):
        for doc in docs_raw:
            doc_name = clean_html(doc.get("description") if isinstance(doc, dict) else doc)
            if doc_name:
                documents_list.append({
                    "documentName": doc_name,
                    "documentType": "Identity / Proof",
                    "mandatory": True
                })

    # FAQs
    faqs_list = []
    faqs_raw = en.get("faqs") or en.get("schemeContent", {}).get("faqs")
    if isinstance(faqs_raw, list):
        for faq in faqs_raw:
            if isinstance(faq, dict):
                q = clean_html(faq.get("question") or faq.get("q"))
                a = clean_html(faq.get("answer") or faq.get("a"))
                if q and a:
                    faqs_list.append({"question": q, "answer": a})

    normalized = {
        "id": scheme_id,
        "schemeCode": scheme_code,
        "slug": slug,
        "titleEnglish": title_en,
        "descriptionEnglish": desc_en,
        "category": category_name,
        "department": ministry_name,
        "schemeType": scheme_type,
        "schemeUrl": f"https://www.myscheme.gov.in/schemes/{slug}",
        "tags": tags,
        "benefits": benefits_list,
        "eligibilityRules": eligibility_rules,
        "documents": documents_list,
        "faqs": faqs_list,
        "status": "ACTIVE"
    }

    return normalized

def generate_samples(limit=20):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.dirname(script_dir)
    details_dir = os.path.join(base_dir, "raw", "details")
    norm_dir = os.path.join(base_dir, "normalized")
    os.makedirs(norm_dir, exist_ok=True)

    detail_files = [f for f in os.listdir(details_dir) if f.endswith(".json")]
    print(f"Found {len(detail_files)} detail files in {details_dir}")

    normalized_samples = []

    for file_name in detail_files[:limit]:
        file_path = os.path.join(details_dir, file_name)
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                raw_data = json.load(f)
            norm = normalize_scheme(raw_data)
            if norm:
                normalized_samples.append(norm)
                slug = norm["slug"] or file_name.replace(".json", "")
                sample_file = os.path.join(norm_dir, f"{slug}-normalized.json")
                with open(sample_file, "w", encoding="utf-8") as sf:
                    json.dump(norm, sf, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"Error normalizing {file_name}: {e}")

    combined_sample_file = os.path.join(norm_dir, "sample-schemes-normalized.json")
    with open(combined_sample_file, "w", encoding="utf-8") as csf:
        json.dump(normalized_samples, csf, ensure_ascii=False, indent=2)
    print(f"Successfully normalized {len(normalized_samples)} schemes into {combined_sample_file}")

if __name__ == "__main__":
    generate_samples(50)
