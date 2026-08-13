import requests
import json
import sys

def trigger(mode="TEST_10"):
    url = f"http://localhost:8082/api/v1/migration/import?mode={mode}"
    print(f"Triggering migration request: POST {url}")
    try:
        r = requests.post(url, timeout=300)
        print(f"HTTP Status: {r.status_code}")
        print("Response JSON:")
        print(json.dumps(r.json(), indent=2))
        return r.json()
    except Exception as e:
        print(f"Error calling migration endpoint: {e}")
        sys.exit(1)

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "TEST_10"
    trigger(mode)
