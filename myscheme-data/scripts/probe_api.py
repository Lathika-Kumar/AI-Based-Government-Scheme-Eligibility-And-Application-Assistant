import os
import sys
import json
import requests

def probe():
    url = "https://api.myscheme.gov.in/search/v6/schemes?lang=en&q=%5B%5D&keyword=&sort=&from=0&size=10"
    api_key = os.environ.get("MYSCHEME_API_KEY", "")
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
        "Origin": "https://www.myscheme.gov.in",
        "Referer": "https://www.myscheme.gov.in/",
    }
    if api_key:
        headers["x-api-key"] = api_key
        print("Using MYSCHEME_API_KEY from environment.")
    else:
        print("No MYSCHEME_API_KEY set in environment. Testing without x-api-key...")

    try:
        res = requests.get(url, headers=headers, timeout=15)
        print(f"Status Code: {res.status_code}")
        if res.status_code == 200:
            data = res.json()
            print("Response Keys:", list(data.keys()))
            if "data" in data:
                d = data["data"]
                print("Data Keys:", list(d.keys()) if isinstance(d, dict) else type(d))
                if isinstance(d, dict) and "hits" in d:
                    hits = d["hits"]
                    print("Hits Keys:", list(hits.keys()))
                    items = hits.get("items", [])
                    print(f"Items Count in page: {len(items)}")
                    page = hits.get("page", {})
                    print("Page Pagination Metadata:", page)
        else:
            print("Response Text:", res.text[:500])
    except Exception as e:
        print("Error probing API:", str(e))

if __name__ == "__main__":
    probe()
