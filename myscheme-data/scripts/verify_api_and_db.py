import requests
import json

def verify_api():
    base_url = "http://localhost:8082/api/v1/schemes"
    print("Testing GET /api/v1/schemes/all ...")
    r = requests.get(f"{base_url}/all")
    print(f"HTTP Status: {r.status_code}")
    data = r.json().get("data", [])
    print(f"Total schemes returned by REST API: {len(data)}")
    
    for idx, s in enumerate(data[:5], 1):
        print(f"\nScheme #{idx}:")
        print(f"  ID:                  {s.get('id')}")
        print(f"  Code:                {s.get('schemeCode')}")
        print(f"  Slug:                {s.get('slug')}")
        print(f"  Title:               {s.get('titleEnglish')}")
        print(f"  Category:            {s.get('categoryName')} ({s.get('categoryCode')})")
        print(f"  Department:          {s.get('departmentName')}")
        print(f"  Brief Description:   {str(s.get('descriptionEnglish'))[:100]}...")
        print(f"  Detailed Desc:       {str(s.get('detailedDescription'))[:100]}...")
        print(f"  Eligibility Text:    {str(s.get('eligibilityText'))[:100]}...")
        print(f"  App Process:         {str(s.get('applicationProcess'))[:100]}...")
        print(f"  Benefits Count:      {len(s.get('benefits', []))}")
        print(f"  Tags Count:          {len(s.get('tags', []))}")

    # Test single scheme details
    if data:
        test_id = data[0].get("id")
        print(f"\nTesting GET /api/v1/schemes/{test_id} ...")
        r_single = requests.get(f"{base_url}/{test_id}")
        print(f"HTTP Status: {r_single.status_code}")
        single_data = r_single.json().get("data", {})
        print("Single Scheme Title:", single_data.get("titleEnglish"))
        print("Single Scheme Slug:", single_data.get("slug"))

if __name__ == "__main__":
    verify_api()
