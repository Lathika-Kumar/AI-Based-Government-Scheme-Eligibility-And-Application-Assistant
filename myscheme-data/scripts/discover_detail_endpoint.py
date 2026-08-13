import re
import requests

def inspect_js_chunks():
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    print("Inspecting myscheme.gov.in JS chunks for detail API endpoints...")
    res = requests.get("https://www.myscheme.gov.in/schemes/kcc", headers=headers, timeout=15)
    html = res.text
    
    # Extract all JS script tags
    js_paths = re.findall(r'/_next/static/chunks/[^"\']+\.js', html)
    print(f"Found {len(set(js_paths))} unique JS chunks on scheme page.")
    
    for js_path in set(js_paths):
        js_url = "https://www.myscheme.gov.in" + js_path
        try:
            r = requests.get(js_url, headers=headers, timeout=10)
            text = r.text
            if "api.myscheme.gov.in" in text or "search/v" in text or "details" in text or "scheme" in text:
                # Search for fetch/axios/api endpoints
                endpoints = re.findall(r'["\'](https://api\.myscheme\.gov\.in/[^"\']+)["\']', text)
                relative_endpoints = re.findall(r'["\'](/search/v[^"\']+)["\']', text)
                relative_schemes = re.findall(r'["\'](/schemes/v[^"\']+)["\']', text)
                
                if endpoints or relative_endpoints or relative_schemes:
                    print(f"\nIn {js_path}:")
                    if endpoints:
                        print("  Full Endpoints:", set(endpoints))
                    if relative_endpoints:
                        print("  Relative Search Endpoints:", set(relative_endpoints))
                    if relative_schemes:
                        print("  Relative Scheme Endpoints:", set(relative_schemes))
                        
                # Look for functions mentioning details or slug
                slug_func = re.findall(r'([a-zA-Z0-9_$]+\s*\([^)]*slug[^)]*\)\s*\{[^}]{1,200}\})', text)
                if slug_func:
                    print(f"  Slug Func Snippets in {js_path}:", slug_func[:2])
        except Exception as e:
            pass

if __name__ == "__main__":
    inspect_js_chunks()
