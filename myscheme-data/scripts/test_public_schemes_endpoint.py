import os
import json
import requests
import find_key

def test_endpoint():
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
    urls_to_test = [
        f"https://api.myscheme.gov.in/schemes/v6/public/schemes?slug={slug}&lang=en",
        f"https://api.myscheme.gov.in/schemes/v6/public/schemes/{slug}?lang=en",
        f"https://api.myscheme.gov.in/schemes/v6/public/schemes?fields=slug,basicDetails,schemeContent,eligibilityCriteria,applicationProcess&slug={slug}&lang=en",
    ]

    for url in urls_to_test:
        print("\n--- Testing GET:", url)
        try:
            r = requests.get(url, headers=headers, timeout=10)
            print("Status:", r.status_code)
            if r.status_code == 200:
                print("SUCCESS!")
                data = r.json()
                print("Data Keys:", list(data.keys()) if isinstance(data, dict) else type(data))
                print("JSON Snippet:", json.dumps(data, indent=2)[:600])
                return url, data
            else:
                print("Response:", r.text[:300])
        except Exception as e:
            print("Error:", e)

    # Test POST method as well
    post_url = "https://api.myscheme.gov.in/schemes/v6/public/schemes"
    post_bodies = [
        {"slug": slug, "lang": "en"},
        {"schemeSlug": slug, "lang": "en"},
        {"fields": ["basicDetails", "schemeContent", "applicationProcess", "eligibilityCriteria"], "slug": slug, "lang": "en"}
    ]
    for body in post_bodies:
        print("\n--- Testing POST:", post_url, "with body:", body)
        try:
            r = requests.post(post_url, headers=headers, json=body, timeout=10)
            print("Status:", r.status_code)
            if r.status_code == 200:
                print("SUCCESS POST!")
                data = r.json()
                print("Data Keys:", list(data.keys()) if isinstance(data, dict) else type(data))
                print("JSON Snippet:", json.dumps(data, indent=2)[:600])
                return post_url, data
            else:
                print("Response:", r.text[:300])
        except Exception as e:
            print("Error:", e)

    return None, None

if __name__ == "__main__":
    test_endpoint()
