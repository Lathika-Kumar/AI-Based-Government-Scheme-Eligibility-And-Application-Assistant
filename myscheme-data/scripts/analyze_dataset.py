import os
import json
import time

def analyze():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.dirname(script_dir)
    raw_dir = os.path.join(base_dir, "raw")
    list_file = os.path.join(raw_dir, "all-schemes-list.json")

    if not os.path.exists(list_file):
        print(f"Error: {list_file} does not exist.")
        return None

    with open(list_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    items = data.get("items", [])

    total_records = len(items)

    ids = []
    slugs = []
    names = []

    missing_id_count = 0
    missing_slug_count = 0
    missing_name_count = 0

    central_count = 0
    state_ut_count = 0
    other_level_count = 0

    categories = set()
    ministries = set()
    beneficiary_states = set()

    with_closing_date = 0
    without_closing_date = 0

    for item in items:
        fields = item.get("fields", item)

        # ID
        sid = item.get("id") or item.get("_id") or fields.get("id") or fields.get("_id")
        if sid:
            ids.append(sid)
        else:
            missing_id_count += 1

        # Slug
        slug = fields.get("slug") or item.get("slug") or fields.get("schemeSlug")
        if slug:
            slugs.append(slug)
        else:
            missing_slug_count += 1

        # Name
        name = fields.get("schemeName") or fields.get("title") or item.get("name")
        if name:
            names.append(name)
        else:
            missing_name_count += 1

        # Level (Central vs State)
        level_val = ""
        level_obj = fields.get("level")
        if isinstance(level_obj, dict):
            level_val = (level_obj.get("value") or level_obj.get("label") or "").lower()
        elif isinstance(level_obj, str):
            level_val = level_obj.lower()
        
        if "central" in level_val:
            central_count += 1
        elif "state" in level_val or "ut" in level_val:
            state_ut_count += 1
        else:
            # Check state field
            state_name = fields.get("state") or fields.get("stateName")
            if state_name and state_name.lower() != "central":
                state_ut_count += 1
            else:
                central_count += 1

        # Category
        cat = fields.get("category") or fields.get("schemeCategory")
        if isinstance(cat, dict):
            cat_name = cat.get("label") or cat.get("name") or cat.get("value")
            if cat_name:
                categories.add(cat_name)
        elif isinstance(cat, list):
            for c in cat:
                if isinstance(c, str): categories.add(c)
                elif isinstance(c, dict): categories.add(c.get("label") or c.get("name") or "")
        elif isinstance(cat, str) and cat.strip():
            categories.add(cat.strip())

        # Ministry / Department
        min_name = fields.get("nodalMinistryName") or fields.get("ministry") or fields.get("department")
        if isinstance(min_name, dict):
            min_str = min_name.get("label") or min_name.get("name")
            if min_str: ministries.add(min_str)
        elif isinstance(min_name, str) and min_name.strip():
            ministries.add(min_name.strip())

        # Beneficiary state
        st = fields.get("state") or fields.get("stateName")
        if isinstance(st, str) and st.strip():
            beneficiary_states.add(st.strip())

        # Closing date
        close_dt = fields.get("closeDate") or fields.get("applicationEndDate") or fields.get("deadline")
        if close_dt and str(close_dt).strip() not in ["null", "None", ""]:
            with_closing_date += 1
        else:
            without_closing_date += 1

    unique_ids = len(set(ids))
    duplicate_ids = len(ids) - unique_ids

    unique_slugs = len(set(slugs))
    duplicate_slugs = len(slugs) - unique_slugs

    stats = {
        "totalRecords": total_records,
        "uniqueIds": unique_ids,
        "duplicateIds": duplicate_ids,
        "uniqueSlugs": unique_slugs,
        "duplicateSlugs": duplicate_slugs,
        "missingIdCount": missing_id_count,
        "missingSlugCount": missing_slug_count,
        "missingNameCount": missing_name_count,
        "centralSchemesCount": central_count,
        "stateUtSchemesCount": state_ut_count,
        "categoriesDiscovered": sorted(list(categories)),
        "categoryCount": len(categories),
        "ministriesDiscovered": sorted(list(ministries)),
        "ministryCount": len(ministries),
        "statesDiscovered": sorted(list(beneficiary_states)),
        "stateCount": len(beneficiary_states),
        "withClosingDate": with_closing_date,
        "withoutClosingDate": without_closing_date
    }

    print(json.dumps(stats, indent=2))
    return stats

if __name__ == "__main__":
    analyze()
