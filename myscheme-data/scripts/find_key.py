import re
import requests

def find_myscheme_key():
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    print("Fetching www.myscheme.gov.in homepage...")
    try:
        res = requests.get("https://www.myscheme.gov.in/search", headers=headers, timeout=15)
        html = res.text
        # Find _next/static/chunks/*.js files
        js_files = re.findall(r'/_next/static/chunks/[^"\']+\.js', html)
        print(f"Found {len(js_files)} JS chunks.")
        
        for js_path in set(js_files):
            js_url = "https://www.myscheme.gov.in" + js_path
            try:
                js_res = requests.get(js_url, headers=headers, timeout=10)
                js_content = js_res.text
                # Search for x-api-key or apiKey pattern
                keys = re.findall(r'["\']x-api-key["\']\s*:\s*["\']([^"\']+)["\']', js_content, re.IGNORECASE)
                if not keys:
                    keys = re.findall(r'x-api-key["\']?\s*,\s*["\']([^"\']+)["\']', js_content, re.IGNORECASE)
                if not keys:
                    keys = re.findall(r'["\']X-API-KEY["\']\s*:\s*["\']([^"\']+)["\']', js_content, re.IGNORECASE)
                if not keys:
                    # Look for 32-64 char alphanumeric string near api.myscheme.gov.in
                    if "api.myscheme.gov.in" in js_content:
                        print(f"Found api.myscheme.gov.in reference in {js_path}")
                        matches = re.findall(r'["\']([a-zA-Z0-9_\-]{20,64})["\']', js_content)
                        for m in matches:
                            if "NEXT_PUBLIC" in m or "api" in m.lower() or "key" in m.lower():
                                print("  Potential key candidate:", m)
                if keys:
                    print(f"FOUND KEY in {js_path}: {keys}")
                    return keys[0]
            except Exception as ex:
                pass
    except Exception as e:
        print("Error fetching myscheme.gov.in:", e)
    return None

if __name__ == "__main__":
    key = find_myscheme_key()
    if key:
        print(f"Extracted API key successfully: {key}")
    else:
        print("Could not auto-extract API key.")
