import os
import json

def build_missing():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.dirname(script_dir)
    raw_dir = os.path.join(base_dir, "raw")
    details_dir = os.path.join(raw_dir, "details")
    
    list_file = os.path.join(raw_dir, "all-schemes-list.json")
    if not os.path.exists(list_file):
        print(f"Error: {list_file} not found.")
        return []

    with open(list_file, "r", encoding="utf-8") as f:
        list_data = json.load(f)

    items = list_data.get("items", [])
    
    slug_map = {}
    for item in items:
        fields = item.get("fields", item)
        slug = fields.get("slug") or item.get("slug") or fields.get("schemeSlug") or fields.get("id") or item.get("id")
        scheme_name = fields.get("schemeName") or fields.get("title") or item.get("name") or "Unknown"
        scheme_id = item.get("id") or item.get("_id") or fields.get("id") or fields.get("_id") or slug

        if slug and slug not in slug_map:
            slug_map[slug] = {
                "slug": slug,
                "id": scheme_id,
                "schemeName": scheme_name
            }

    unique_slug_keys = set(slug_map.keys())
    existing_files = [f for f in os.listdir(details_dir) if f.endswith(".json")]
    existing_slugs = set(f.replace(".json", "") for f in existing_files)

    missing_slug_keys = sorted(list(unique_slug_keys - existing_slugs))
    missing_list = [slug_map[slug] for slug in missing_slug_keys]

    out_file = os.path.join(raw_dir, "missing-details-before-resume.json")
    with open(out_file, "w", encoding="utf-8") as out:
        json.dump({
            "generatedAt": "2026-08-13T07:06:36Z",
            "totalUniqueSlugs": len(unique_slug_keys),
            "existingDetailsCount": len(existing_slugs),
            "missingDetailsCount": len(missing_list),
            "missingSchemes": missing_list
        }, out, ensure_ascii=False, indent=2)

    print(f"Total Unique Slugs: {len(unique_slug_keys)}")
    print(f"Existing Details Count: {len(existing_slugs)}")
    print(f"Missing Details Count: {len(missing_list)}")
    print(f"Saved missing details list to: {out_file}")
    return missing_list

if __name__ == "__main__":
    build_missing()
