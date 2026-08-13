import os
import sys
import json
import requests
import find_key

def probe_detail():
    key = os.environ.get("MYSCHEME_API_KEY")
    if not key:
        key = find_key.find_myscheme_key()
        os.environ["MYSCHEME_API_KEY"] = key

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
        "Origin": "https://www.myscheme.gov.in",
        "Referer": "https://www.myscheme.gov.in/",
        "x-api-key": key
    }

    slug = "kcc"
    candidate_urls = [
        f"https://api.myscheme.gov.in/search/v6/schemes/{slug}?lang=en",
        f"https://api.myscheme.gov.in/search/v6/scheme/details?slug={slug}&lang=en",
        f"https://api.myscheme.gov.in/search/v6/schemes/details?slug={slug}&lang=en",
        f"https://api.myscheme.gov.in/search/v6/details?slug={slug}&lang=en",
        f"https://api.myscheme.gov.in/search/v5/scheme/details?slug={slug}&lang=en",
        f"https://api.myscheme.gov.in/search/v4/scheme/details?slug={slug}&lang=en",
        f"https://api.myscheme.gov.in/schemes/v1/{slug}?lang=en",
        f"https://api.myscheme.gov.in/schemes/v2/{slug}?lang=en",
        f"https://api.myscheme.gov.in/search/v6/schemes/by-slug/{slug}?lang=en",
        f"https://api.myscheme.gov.in/search/v6/schemes?slug={slug}&lang=en"
    ]

    print("Testing candidate detail endpoints for slug:", slug)
    for url in candidate_urls:
        try:
            r = requests.get(url, headers=headers, timeout=10)
            print(f"URL: {url} -> Status: {r.status_code}")
            if r.status_code == 200:
                print("SUCCESS! Endpoint response sample:")
                data = r.json()
                print("Keys:", list(data.keys()) if isinstance(data, dict) else type(data))
                print(json.dumps(data, indent=2)[:500])
                return url
        except Exception as e:
            print(f"URL: {url} -> Error: {e}")

    # Also inspect Next.js frontend pages to see how myScheme fetches detail data
    print("\nInspecting frontend JS for detail endpoint pattern...")
    try:
        res = requests.get("https://www.myscheme.gov.in/schemes/kcc", headers=headers, timeout=15)
        html = res.text
        import re
        api_matches = re.findall(r'https://api\.myscheme\.gov\.in/[^\s"\'\`]+', html)
        print("Found API references in scheme page HTML:", set(api_matches))
    except Exception as ex:
        print("Error inspecting scheme page:", ex)

    return None

if __name__ == "__main__":
    probe_detail()
