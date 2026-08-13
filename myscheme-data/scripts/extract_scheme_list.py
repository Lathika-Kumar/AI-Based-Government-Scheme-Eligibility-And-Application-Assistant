import os
import sys
import json
import time
import requests
import find_key

def extract_listing():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.dirname(script_dir)
    raw_dir = os.path.join(base_dir, "raw")
    logs_dir = os.path.join(base_dir, "logs")
    os.makedirs(raw_dir, exist_ok=True)
    os.makedirs(logs_dir, exist_ok=True)

    log_file_path = os.path.join(logs_dir, "extraction_listing.log")

    def log(msg):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        formatted = f"[{timestamp}] {msg}"
        print(formatted)
        with open(log_file_path, "a", encoding="utf-8") as f:
            f.write(formatted + "\n")

    log("Starting myScheme list extraction...")

    key = os.environ.get("MYSCHEME_API_KEY")
    if not key:
        log("MYSCHEME_API_KEY environment variable not set. Attempting dynamic resolution...")
        try:
            key = find_key.find_myscheme_key()
            if key:
                os.environ["MYSCHEME_API_KEY"] = key
                log("Dynamic API key resolution successful.")
            else:
                log("ERROR: Could not resolve MYSCHEME_API_KEY.")
                sys.exit(1)
        except Exception as e:
            log(f"ERROR resolving API key: {e}")
            sys.exit(1)
    else:
        log("MYSCHEME_API_KEY found in environment.")

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
        "Origin": "https://www.myscheme.gov.in",
        "Referer": "https://www.myscheme.gov.in/",
        "x-api-key": key
    }

    base_url = "https://api.myscheme.gov.in/search/v6/schemes"
    page_size = 50
    offset = 0

    # Step 1: Initial request to get total count
    params = {
        "lang": "en",
        "q": "[]",
        "keyword": "",
        "sort": "",
        "from": offset,
        "size": page_size
    }

    log(f"Requesting initial page 0 (offset=0, size={page_size})...")
    try:
        res = requests.get(base_url, headers=headers, params=params, timeout=20)
        res.raise_for_status()
        first_data = res.json()
    except Exception as e:
        log(f"FATAL: Failed initial listing request: {e}")
        sys.exit(1)

    hits_obj = first_data.get("data", {}).get("hits", {})
    page_meta = hits_obj.get("page", {})
    total_records = page_meta.get("total", 0)

    log(f"API metadata returned total={total_records} schemes.")
    if total_records == 0:
        log("ERROR: Total schemes count is 0. Aborting.")
        sys.exit(1)

    all_items = []
    page_num = 0

    while offset < total_records:
        log(f"Fetching listing page {page_num} (from={offset}, size={page_size})...")
        page_params = {
            "lang": "en",
            "q": "[]",
            "keyword": "",
            "sort": "",
            "from": offset,
            "size": page_size
        }

        success = False
        for attempt in range(1, 4):
            try:
                r = requests.get(base_url, headers=headers, params=page_params, timeout=20)
                if r.status_code == 200:
                    data = r.json()
                    page_file = os.path.join(raw_dir, f"listing-page-{page_num:03d}.json")
                    with open(page_file, "w", encoding="utf-8") as pf:
                        json.dump(data, pf, ensure_ascii=False, indent=2)
                    
                    items = data.get("data", {}).get("hits", {}).get("items", [])
                    all_items.extend(items)
                    log(f"Page {page_num} saved ({len(items)} items). Total extracted so far: {len(all_items)}")
                    success = True
                    break
                else:
                    log(f"Attempt {attempt}: HTTP status {r.status_code}. Retrying...")
                    time.sleep(2 * attempt)
            except Exception as ex:
                log(f"Attempt {attempt}: Exception {ex}. Retrying...")
                time.sleep(2 * attempt)

        if not success:
            log(f"WARNING: Failed to fetch page {page_num} (offset {offset}) after 3 attempts.")

        offset += page_size
        page_num += 1
        time.sleep(0.15)  # Rate limiting pause

    # Save combined file
    combined_file = os.path.join(raw_dir, "all-schemes-list.json")
    with open(combined_file, "w", encoding="utf-8") as cf:
        json.dump({
            "extractedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "totalCount": len(all_items),
            "items": all_items
        }, cf, ensure_ascii=False, indent=2)

    log(f"SUCCESS: Extracted {len(all_items)} scheme records. Saved combined dataset to {combined_file}")
    return combined_file

if __name__ == "__main__":
    extract_listing()
