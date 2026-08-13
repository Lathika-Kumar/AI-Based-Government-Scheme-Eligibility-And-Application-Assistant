import os
import sys
import json
import time
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
import find_key

def extract_details(max_workers=8, limit=None):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.dirname(script_dir)
    raw_dir = os.path.join(base_dir, "raw")
    details_dir = os.path.join(raw_dir, "details")
    logs_dir = os.path.join(base_dir, "logs")
    os.makedirs(details_dir, exist_ok=True)
    os.makedirs(logs_dir, exist_ok=True)

    log_file_path = os.path.join(logs_dir, "extraction_details.log")

    def log(msg):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        formatted = f"[{timestamp}] {msg}"
        print(formatted)
        with open(log_file_path, "a", encoding="utf-8") as f:
            f.write(formatted + "\n")

    log("Starting myScheme details extraction...")

    list_file = os.path.join(raw_dir, "all-schemes-list.json")
    if not os.path.exists(list_file):
        log("ERROR: all-schemes-list.json not found. Run extract_scheme_list.py first.")
        sys.exit(1)

    with open(list_file, "r", encoding="utf-8") as f:
        list_data = json.load(f)

    items = list_data.get("items", [])
    log(f"Loaded {len(items)} items from combined list.")

    # Extract unique slugs
    slug_map = {}
    for idx, item in enumerate(items):
        # Fields could be in item or item.fields
        fields = item.get("fields", item)
        slug = fields.get("slug") or item.get("slug") or fields.get("schemeSlug") or fields.get("id") or item.get("id")
        scheme_name = fields.get("schemeName") or fields.get("title") or item.get("name") or "Unknown"
        scheme_id = item.get("id") or item.get("_id") or fields.get("id") or fields.get("_id") or slug

        if slug:
            if slug not in slug_map:
                slug_map[slug] = {
                    "id": scheme_id,
                    "slug": slug,
                    "name": scheme_name
                }

    unique_slugs = list(slug_map.values())
    log(f"Extracted {len(unique_slugs)} unique scheme slugs.")

    if limit and isinstance(limit, int):
        unique_slugs = unique_slugs[:limit]
        log(f"Limit applied: processing first {limit} schemes.")

    key = os.environ.get("MYSCHEME_API_KEY")
    if not key:
        key = find_key.find_myscheme_key()
        if key:
            os.environ["MYSCHEME_API_KEY"] = key
        else:
            log("ERROR: Could not resolve MYSCHEME_API_KEY.")
            sys.exit(1)

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
        "Origin": "https://www.myscheme.gov.in",
        "Referer": "https://www.myscheme.gov.in/",
        "x-api-key": key
    }

    manifest_file = os.path.join(raw_dir, "details-manifest.json")
    manifest = []
    if os.path.exists(manifest_file):
        try:
            with open(manifest_file, "r", encoding="utf-8") as mf:
                manifest = json.load(mf)
        except Exception:
            manifest = []

    completed_slugs = {m["slug"]: m for m in manifest if m.get("status") == "SUCCESS"}

    def process_scheme(scheme_info):
        slug = scheme_info["slug"]
        scheme_id = scheme_info["id"]
        scheme_name = scheme_info["name"]

        target_path = os.path.join(details_dir, f"{slug}.json")

        # Skip if already downloaded and valid
        if slug in completed_slugs and os.path.exists(target_path) and os.path.getsize(target_path) > 50:
            return completed_slugs[slug]

        detail_url = f"https://api.myscheme.gov.in/schemes/v6/public/schemes?slug={slug}&lang=en"

        manifest_entry = {
            "id": scheme_id,
            "slug": slug,
            "schemeName": scheme_name,
            "status": "FAILED",
            "httpStatus": 0,
            "filePath": target_path,
            "error": None,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ")
        }

        for attempt in range(1, 4):
            try:
                r = requests.get(detail_url, headers=headers, timeout=15)
                manifest_entry["httpStatus"] = r.status_code
                if r.status_code == 200:
                    data = r.json()
                    with open(target_path, "w", encoding="utf-8") as df:
                        json.dump(data, df, ensure_ascii=False, indent=2)
                    manifest_entry["status"] = "SUCCESS"
                    manifest_entry["error"] = None
                    break
                else:
                    manifest_entry["error"] = f"HTTP {r.status_code}: {r.text[:200]}"
                    time.sleep(1 * attempt)
            except Exception as ex:
                manifest_entry["error"] = str(ex)
                time.sleep(1 * attempt)

        return manifest_entry

    log(f"Starting bulk detail download across {len(unique_slugs)} schemes with {max_workers} worker threads...")
    results_manifest = []
    success_count = 0
    fail_count = 0

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_slug = {executor.submit(process_scheme, info): info["slug"] for info in unique_slugs}
        for count, future in enumerate(as_completed(future_to_slug), 1):
            entry = future.result()
            results_manifest.append(entry)
            if entry["status"] == "SUCCESS":
                success_count += 1
            else:
                fail_count += 1

            if count % 100 == 0 or count == len(unique_slugs):
                log(f"Processed {count}/{len(unique_slugs)} details. Success: {success_count}, Failed: {fail_count}")

    # Save manifest
    with open(manifest_file, "w", encoding="utf-8") as mf:
        json.dump(results_manifest, mf, ensure_ascii=False, indent=2)

    log(f"COMPLETED: Details extraction complete. Total: {len(results_manifest)}, Success: {success_count}, Failures: {fail_count}")
    log(f"Manifest saved to {manifest_file}")
    return results_manifest

if __name__ == "__main__":
    limit_val = None
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        limit_val = int(sys.argv[1])
    extract_details(max_workers=60, limit=limit_val)
