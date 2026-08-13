import requests
import json

def verify_sample_schemes():
    base_url = "http://localhost:8082/api/v1/schemes"
    print("Fetching all schemes to get IDs...")
    r = requests.get(f"{base_url}/all")
    schemes = r.json().get("data", [])
    print(f"Total schemes fetched: {len(schemes)}")
    
    # Filter for real myScheme items (id length != 36 uuid or matching slug)
    real_schemes = [s for s in schemes if s.get("slug") is not None]
    print(f"Total real myScheme items in database: {len(real_schemes)}")
    
    print("\nVerifying 5 representative real schemes by ID:")
    for s in real_schemes[:5]:
        scheme_id = s.get("id")
        r_detail = requests.get(f"{base_url}/{scheme_id}")
        if r_detail.status_code == 200:
            d = r_detail.json().get("data", {})
            print(f"\nScheme ID: {d.get('id')}")
            print(f"  Title:            {d.get('titleEnglish')}")
            print(f"  Slug:             {d.get('slug')}")
            print(f"  Category:         {d.get('categoryName')} ({d.get('categoryCode')})")
            print(f"  Department:       {d.get('departmentName')}")
            print(f"  Scheme Type:      {d.get('schemeType')}")
            print(f"  State:            {d.get('applicableStates')}")
            print(f"  DBT Scheme:       {d.get('dbtScheme')}")
            print(f"  Beneficiaries:    {d.get('targetBeneficiaries')}")
            print(f"  Brief Desc:       {str(d.get('descriptionEnglish'))[:120]}...")
            print(f"  Detailed Desc:   {len(str(d.get('detailedDescription')))} chars")
            print(f"  Eligibility Text: {len(str(d.get('eligibilityText')))} chars")
            print(f"  App Process:     {len(str(d.get('applicationProcess')))} chars")
            print(f"  Tags:             {d.get('tags')}")
        else:
            print(f"Failed to fetch detail for ID: {scheme_id}, Status: {r_detail.status_code}")

if __name__ == "__main__":
    verify_sample_schemes()
