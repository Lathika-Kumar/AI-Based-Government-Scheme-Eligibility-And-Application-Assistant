import requests
import json

def run_full_import():
    url = "http://localhost:8082/api/v1/migration/import?mode=FULL"
    print("==================================================")
    print("STARTING FULL RE-IMPORT OF ALL 4,679 SCHEMES...")
    print("==================================================")
    r = requests.post(url)
    print(f"HTTP Status: {r.status_code}")
    print("Response:")
    print(json.dumps(r.json(), indent=2))

if __name__ == "__main__":
    run_full_import()
