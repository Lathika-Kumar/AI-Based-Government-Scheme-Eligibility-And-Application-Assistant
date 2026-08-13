import os
import json
import glob
from collections import Counter

def analyze_quality():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    details_dir = os.path.join(base_dir, "raw", "details")
    
    detail_files = [os.path.join(details_dir, f) for f in os.listdir(details_dir) if f.endswith(".json")]
    total_files = len(detail_files)

    missing_names = 0
    missing_descriptions = 0
    missing_benefits = 0
    missing_eligibility = 0
    missing_application_process = 0
    missing_documents = 0
    missing_ministry = 0
    missing_category = 0
    missing_deadline = 0
    inconsistent_types = 0

    field_counts = Counter()

    for filepath in detail_files:
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                raw = json.load(f)
                
            if not isinstance(raw, dict) or "data" not in raw:
                inconsistent_types += 1
                continue

            d = raw.get("data")
            if not isinstance(d, dict):
                inconsistent_types += 1
                continue

            en = d.get("en", {})
            basic = en.get("basicDetails", {})
            content = en.get("schemeContent", {})

            # Count fields present
            for k in d.keys(): field_counts[f"data.{k}"] += 1
            for k in en.keys(): field_counts[f"data.en.{k}"] += 1
            for k in basic.keys(): field_counts[f"data.en.basicDetails.{k}"] += 1

            # Check specific attributes
            name = basic.get("schemeName") or basic.get("schemeShortTitle") or content.get("schemeName")
            if not name or not str(name).strip():
                missing_names += 1

            desc = basic.get("briefDescription") or basic.get("description") or content.get("briefDescription")
            if not desc or not str(desc).strip():
                missing_descriptions += 1

            benefits = en.get("benefits") or content.get("benefits")
            if not benefits or (isinstance(benefits, (list, dict, str)) and len(benefits) == 0):
                missing_benefits += 1

            elig = en.get("eligibilityCriteria") or content.get("eligibility")
            if not elig or (isinstance(elig, (list, dict, str)) and len(elig) == 0):
                missing_eligibility += 1

            app_proc = en.get("applicationProcess") or content.get("applicationProcess")
            if not app_proc or (isinstance(app_proc, (list, dict, str)) and len(app_proc) == 0):
                missing_application_process += 1

            docs = en.get("documentsRequired") or content.get("documents")
            if not docs or (isinstance(docs, (list, dict, str)) and len(docs) == 0):
                missing_documents += 1

            ministry = basic.get("nodalMinistryName") or basic.get("implementingAgency")
            if not ministry or not str(ministry).strip():
                missing_ministry += 1

            category = basic.get("schemeCategory")
            if not category or (isinstance(category, (list, dict, str)) and len(category) == 0):
                missing_category += 1

            deadline = basic.get("closeDate") or basic.get("applicationEndDate") or basic.get("deadline")
            if not deadline or str(deadline).strip() in ["null", "None", ""]:
                missing_deadline += 1

        except Exception as e:
            inconsistent_types += 1

    results = {
        "totalFilesInspected": total_files,
        "missingNames": missing_names,
        "missingDescriptions": missing_descriptions,
        "missingBenefits": missing_benefits,
        "missingEligibility": missing_eligibility,
        "missingApplicationProcess": missing_application_process,
        "missingDocuments": missing_documents,
        "missingMinistry": missing_ministry,
        "missingCategory": missing_category,
        "missingDeadline": missing_deadline,
        "inconsistentTypes": inconsistent_types,
        "topFieldPresence": dict(field_counts.most_common(25))
    }

    print(json.dumps(results, indent=2))
    return results

if __name__ == "__main__":
    analyze_quality()
