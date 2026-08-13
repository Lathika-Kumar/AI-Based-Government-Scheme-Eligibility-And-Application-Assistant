import os
import json
import glob

def perform_audit():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    raw_dir = os.path.join(base_dir, "raw")
    details_dir = os.path.join(raw_dir, "details")
    logs_dir = os.path.join(base_dir, "logs")

    # 1. Listing files audit
    listing_files = sorted(glob.glob(os.path.join(raw_dir, "listing-page-*.json")))
    pages_expected = 96
    pages_present = len(listing_files)

    total_listing_records = 0
    all_listing_ids = []
    all_listing_slugs = []

    listing_api_total = 0

    for lfile in listing_files:
        try:
            with open(lfile, "r", encoding="utf-8") as f:
                data = json.load(f)
                if listing_api_total == 0:
                    listing_api_total = data.get("data", {}).get("hits", {}).get("page", {}).get("total", 4772)
                items = data.get("data", {}).get("hits", {}).get("items", [])
                total_listing_records += len(items)
                for item in items:
                    fields = item.get("fields", item)
                    sid = item.get("id") or item.get("_id") or fields.get("id") or fields.get("_id")
                    slug = fields.get("slug") or item.get("slug") or fields.get("schemeSlug")
                    if sid: all_listing_ids.append(sid)
                    if slug: all_listing_slugs.append(slug)
        except Exception as e:
            print(f"Error reading {lfile}: {e}")

    unique_listing_ids = set(all_listing_ids)
    duplicate_listing_ids_count = len(all_listing_ids) - len(unique_listing_ids)

    unique_listing_slugs = set(all_listing_slugs)
    duplicate_listing_slugs_count = len(all_listing_slugs) - len(unique_listing_slugs)

    # 2. Details files audit
    detail_files = [f for f in os.listdir(details_dir) if f.endswith(".json")]
    details_present_count = len(detail_files)

    # Check for empty or malformed detail files
    empty_detail_files = []
    malformed_detail_files = []
    detail_slugs_on_disk = set()

    for df in detail_files:
        slug_name = df.replace(".json", "")
        detail_slugs_on_disk.add(slug_name)
        filepath = os.path.join(details_dir, df)
        file_size = os.path.getsize(filepath)
        if file_size == 0 or file_size < 10:
            empty_detail_files.append(df)
        else:
            try:
                with open(filepath, "r", encoding="utf-8") as dfile:
                    content = json.load(dfile)
                    if not isinstance(content, dict) or "data" not in content:
                        malformed_detail_files.append(df)
            except Exception:
                malformed_detail_files.append(df)

    # Compare listing vs details
    matching_details = unique_listing_slugs.intersection(detail_slugs_on_disk)
    missing_details = unique_listing_slugs - detail_slugs_on_disk
    orphan_details = detail_slugs_on_disk - unique_listing_slugs

    completion_percentage = (len(matching_details) / len(unique_listing_slugs)) * 100 if unique_listing_slugs else 0

    # 3. Manifest & Log Audit
    manifest_file = os.path.join(raw_dir, "details-manifest.json")
    manifest_entries = []
    if os.path.exists(manifest_file):
        try:
            with open(manifest_file, "r", encoding="utf-8") as mf:
                manifest_entries = json.load(mf)
        except Exception as ex:
            print(f"Error reading manifest: {ex}")

    manifest_success_count = sum(1 for m in manifest_entries if m.get("status") == "SUCCESS")
    manifest_fail_count = sum(1 for m in manifest_entries if m.get("status") != "SUCCESS")

    error_groups = {}
    for m in manifest_entries:
        if m.get("status") != "SUCCESS":
            err_msg = str(m.get("error", "Unknown Error"))
            http_st = m.get("httpStatus", 0)
            key = f"HTTP {http_st}: {err_msg[:80]}"
            error_groups[key] = error_groups.get(key, 0) + 1

    # Read extraction_details.log
    details_log_file = os.path.join(logs_dir, "extraction_details.log")
    log_lines = []
    if os.path.exists(details_log_file):
        with open(details_log_file, "r", encoding="utf-8") as lf:
            log_lines = lf.readlines()

    audit_result = {
        "listing": {
            "apiTotal": listing_api_total,
            "pagesExpected": pages_expected,
            "pagesPresent": pages_present,
            "recordsExtracted": total_listing_records,
            "uniqueListingIds": len(unique_listing_ids),
            "uniqueListingSlugs": len(unique_listing_slugs),
            "duplicateListingIds": duplicate_listing_ids_count,
            "duplicateListingSlugs": duplicate_listing_slugs_count
        },
        "details": {
            "detailsExpected": len(unique_listing_slugs),
            "detailsPresent": details_present_count,
            "matchingDetails": len(matching_details),
            "missingDetails": len(missing_details),
            "orphanDetails": len(orphan_details),
            "emptyFiles": len(empty_detail_files),
            "malformedFiles": len(malformed_detail_files),
            "completionPercentage": round(completion_percentage, 2)
        },
        "manifest": {
            "totalRecorded": len(manifest_entries),
            "successCount": manifest_success_count,
            "failCount": manifest_fail_count,
            "errorBreakdown": error_groups
        }
    }

    print(json.dumps(audit_result, indent=2))
    return audit_result

if __name__ == "__main__":
    perform_audit()
