import os
import sys
import json
import time
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
import find_key

def resume_extraction(max_workers=2, delay_sec=0.5, max_consecutive_429=5):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.dirname(script_dir)
    raw_dir = os.path.join(base_dir, "raw")
    details_dir = os.path.join(raw_dir, "details")
    logs_dir = os.path.join(base_dir, "logs")
    os.makedirs(details_dir, exist_ok=True)
    os.makedirs(logs_dir, exist_ok=True)

    log_file_path = os.path.join(logs_dir, "extraction_resume.log")

    def log(msg):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        formatted = f"[{timestamp}] {msg}"
        print(formatted)
        with open(log_file_path, "a", encoding="utf-8") as f:
            f.write(formatted + "\n")

    log("==================================================")
    log("Starting controlled rate-resilient extraction resume...")

    # Load missing schemes file
    missing_file = os.path.join(raw_dir, "missing-details-before-resume.json")
    if not os.path.exists(missing_file):
        log("ERROR: missing-details-before-resume.json not found. Run build_missing_list.py first.")
        sys.exit(1)

    with open(missing_file, "r", encoding="utf-8") as mf:
        missing_data = json.load(mf)

    total_expected = missing_data.get("totalUniqueSlugs", 4679)
    missing_schemes = missing_data.get("missingSchemes", [])

    # Verify existing details on disk
    existing_files = [f for f in os.listdir(details_dir) if f.endswith(".json") and os.path.getsize(os.path.join(details_dir, f)) > 50]
    existing_count = len(existing_files)
    remaining_count = total_expected - existing_count

    # Display required status header
    print(f"Expected details: {total_expected}")
    print(f"Existing details: {existing_count}")
    print(f"Remaining details: {remaining_count}")
    log(f"Header Displayed -> Expected: {total_expected}, Existing: {existing_count}, Remaining: {remaining_count}")

    if remaining_count <= 0:
        log("SUCCESS: All expected scheme details already exist on disk!")
        return

    # Resolve API Key securely
    key = os.environ.get("MYSCHEME_API_KEY")
    if not key:
        log("MYSCHEME_API_KEY environment variable not set. Resolving dynamically...")
        key = find_key.find_myscheme_key()
        if key:
            os.environ["MYSCHEME_API_KEY"] = key
            log("API key dynamically resolved.")
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

    # Load or initialize manifest map
    manifest_file = os.path.join(raw_dir, "details-manifest.json")
    manifest_map = {}
    if os.path.exists(manifest_file):
        try:
            with open(manifest_file, "r", encoding="utf-8") as mf:
                manifest_list = json.load(mf)
                for entry in manifest_list:
                    manifest_map[entry["slug"]] = entry
        except Exception:
            manifest_map = {}

    # Filter pending items
    pending_schemes = []
    for info in missing_schemes:
        slug = info["slug"]
        target_path = os.path.join(details_dir, f"{slug}.json")
        if os.path.exists(target_path) and os.path.getsize(target_path) > 50:
            manifest_map[slug] = {
                "id": info.get("id"),
                "slug": slug,
                "schemeName": info.get("schemeName"),
                "status": "SUCCESS",
                "httpStatus": 200,
                "filePath": target_path,
                "error": None,
                "retryCount": manifest_map.get(slug, {}).get("retryCount", 0),
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ")
            }
        else:
            pending_schemes.append(info)

    log(f"Checkpointed verification: {len(pending_schemes)} remaining pending missing schemes.")

    success_counter = 0
    failure_counter = 0
    consecutive_429_count = 0

    def download_one(info):
        nonlocal consecutive_429_count
        slug = info["slug"]
        scheme_id = info.get("id")
        scheme_name = info.get("schemeName")
        target_path = os.path.join(details_dir, f"{slug}.json")

        if os.path.exists(target_path) and os.path.getsize(target_path) > 50:
            return {
                "id": scheme_id,
                "slug": slug,
                "schemeName": scheme_name,
                "status": "SUCCESS",
                "httpStatus": 200,
                "filePath": target_path,
                "error": None,
                "retryCount": 0,
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ")
            }

        detail_url = f"https://api.myscheme.gov.in/schemes/v6/public/schemes?slug={slug}&lang=en"

        retries = 0
        max_retries = 3
        fetch_success = False
        last_error = None
        last_status = 0

        while retries < max_retries and not fetch_success:
            retries += 1
            try:
                r = requests.get(detail_url, headers=headers, timeout=15)
                last_status = r.status_code

                if r.status_code == 200:
                    data = r.json()
                    with open(target_path, "w", encoding="utf-8") as df:
                        json.dump(data, df, ensure_ascii=False, indent=2)
                    fetch_success = True
                    consecutive_429_count = 0
                    break

                elif r.status_code == 429:
                    consecutive_429_count += 1
                    retry_after = r.headers.get("Retry-After")
                    if retry_after and retry_after.isdigit():
                        backoff_sec = int(retry_after) + 2
                    else:
                        backoff_sec = 15 * (2 ** (retries - 1)) # 15s, 30s, 60s
                    
                    log(f"HTTP 429 for '{slug}' (attempt {retries}/{max_retries}). Consecutive 429s: {consecutive_429_count}. Backing off {backoff_sec}s...")
                    time.sleep(backoff_sec)
                    last_error = f"HTTP 429 Too Many Requests (attempt {retries})"

                    if consecutive_429_count >= max_consecutive_429:
                        log(f"CRITICAL: Reached {consecutive_429_count} consecutive 429 responses. Pausing safely.")
                        break

                else:
                    last_error = f"HTTP {r.status_code}: {r.text[:150]}"
                    time.sleep(2 * retries)

            except Exception as ex:
                last_error = str(ex)
                time.sleep(2 * retries)

        time.sleep(delay_sec)

        return {
            "id": scheme_id,
            "slug": slug,
            "schemeName": scheme_name,
            "status": "SUCCESS" if fetch_success else "FAILED",
            "httpStatus": last_status,
            "filePath": target_path,
            "error": None if fetch_success else last_error,
            "retryCount": retries,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ")
        }

    log(f"Launching controlled pool of {max_workers} workers for {len(pending_schemes)} items...")

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(download_one, info): info["slug"] for info in pending_schemes}
        for count, future in enumerate(as_completed(futures), 1):
            res = future.result()
            slug = res["slug"]
            manifest_map[slug] = res

            if res["status"] == "SUCCESS":
                success_counter += 1
            else:
                failure_counter += 1

            if count % 25 == 0 or count == len(pending_schemes):
                # Save manifest
                with open(manifest_file, "w", encoding="utf-8") as mf:
                    json.dump(list(manifest_map.values()), mf, ensure_ascii=False, indent=2)
                log(f"Progress: {count}/{len(pending_schemes)} processed. Success: {success_counter}, Failed: {failure_counter}")

            if consecutive_429_count >= max_consecutive_429:
                log(f"CRITICAL: Reached {consecutive_429_count} consecutive 429s. Safely pausing remaining downloads.")
                executor.shutdown(wait=False, cancel_futures=True)
                break

    # Save final manifest file
    with open(manifest_file, "w", encoding="utf-8") as mf:
        json.dump(list(manifest_map.values()), mf, ensure_ascii=False, indent=2)

    log(f"RESUME BATCH COMPLETE. Recovered in this batch: {success_counter}, Failed: {failure_counter}")

if __name__ == "__main__":
    resume_extraction(max_workers=2, delay_sec=0.5, max_consecutive_429=5)
