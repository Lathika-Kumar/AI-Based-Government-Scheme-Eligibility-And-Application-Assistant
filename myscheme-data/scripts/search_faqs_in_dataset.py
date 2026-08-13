import os
import json
from collections import Counter

def search_faqs():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    details_dir = os.path.join(base_dir, "raw", "details")
    
    files = [os.path.join(details_dir, f) for f in os.listdir(details_dir) if f.endswith(".json")]
    print(f"Searching across {len(files)} raw JSON detail files for FAQ data...")

    faq_keys_found = Counter()
    faq_content_matches = Counter()
    samples = []

    for filepath in files:
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                raw = json.load(f)

            # Recursive function to find keys or patterns
            def inspect_obj(obj, current_path=""):
                if isinstance(obj, dict):
                    for k, v in obj.items():
                        new_path = f"{current_path}.{k}" if current_path else k
                        
                        # Check key names
                        if any(kw in k.lower() for kw in ["faq", "question", "qna", "q_a", "qa"]):
                            faq_keys_found[new_path] += 1
                            if len(samples) < 10:
                                samples.append({
                                    "file": os.path.basename(filepath),
                                    "path": new_path,
                                    "sample": str(v)[:150]
                                })
                        
                        inspect_obj(v, new_path)

                elif isinstance(obj, list):
                    for idx, item in enumerate(obj):
                        new_path = f"{current_path}[{idx}]"
                        if isinstance(item, dict):
                            # check if list item looks like Q&A
                            keys = [str(k).lower() for k in item.keys()]
                            if ("q" in keys or "question" in keys) and ("a" in keys or "answer" in keys):
                                faq_content_matches[current_path] += 1
                                if len(samples) < 10:
                                    samples.append({
                                        "file": os.path.basename(filepath),
                                        "path": new_path,
                                        "sample": str(item)[:150]
                                    })
                        inspect_obj(item, new_path)

            inspect_obj(raw)

        except Exception as e:
            pass

    results = {
        "totalFiles": len(files),
        "faqKeysFound": dict(faq_keys_found),
        "faqContentMatches": dict(faq_content_matches),
        "samples": samples
    }

    print(json.dumps(results, indent=2))
    return results

if __name__ == "__main__":
    search_faqs()
