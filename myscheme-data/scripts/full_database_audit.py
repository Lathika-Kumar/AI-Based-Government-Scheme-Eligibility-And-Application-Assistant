import requests
import json
import sys

def audit_full_system():
    print("==================================================")
    print("MYSCHEME REAL DATA MIGRATION — FULL DATABASE AUDIT")
    print("==================================================")
    
    base_url = "http://localhost:8082/api/v1/schemes"
    
    # 1. Total schemes via REST API
    print("\n[1] Testing GET /api/v1/schemes/all ...")
    r_all = requests.get(f"{base_url}/all", timeout=60)
    print(f"HTTP Status: {r_all.status_code}")
    all_schemes = r_all.json().get("data", [])
    print(f"TOTAL SCHEMES IN ORACLE DB VIA REST API: {len(all_schemes)}")
    
    # 2. Search API
    print("\n[2] Testing GET /api/v1/schemes/search?keyword=Kisan ...")
    r_search = requests.get(f"{base_url}/search?keyword=Kisan", timeout=30)
    print(f"HTTP Status: {r_search.status_code}")
    search_data = r_search.json().get("data", {})
    total_elements = search_data.get("totalElements", 0)
    print(f"Search 'Kisan' returned totalElements: {total_elements}")
    
    # 3. Category distribution
    categories = {}
    departments = {}
    scheme_types = {}
    states = {}
    
    for s in all_schemes:
        cat = s.get("categoryName") or "Uncategorized"
        dept = s.get("departmentName") or "Unassigned"
        stype = s.get("schemeType") or "UNKNOWN"
        state = s.get("applicableStates") or "CENTRAL"
        
        categories[cat] = categories.get(cat, 0) + 1
        departments[dept] = departments.get(dept, 0) + 1
        scheme_types[stype] = scheme_types.get(stype, 0) + 1
        states[state] = states.get(state, 0) + 1

    print("\n[3] Scheme Type Distribution:")
    for st, count in scheme_types.items():
        print(f"  - {st:25s}: {count} schemes")
        
    print(f"\n[4] Total Unique Categories in Oracle DB: {len(categories)}")
    for cat, count in sorted(categories.items(), key=lambda x: x[1], reverse=True)[:10]:
        print(f"  - {cat:40s}: {count} schemes")
        
    print(f"\n[5] Total Unique Departments in Oracle DB: {len(departments)}")
    for dept, count in sorted(departments.items(), key=lambda x: x[1], reverse=True)[:10]:
        print(f"  - {dept:50s}: {count} schemes")
        
    # 4. Detailed Scheme Verification
    print("\n[6] Sampling 3 Real Schemes Details Verification:")
    sample_slugs = ["kcc", "pm-kisan", "ab-pmjay"]
    for slug in sample_slugs:
        print(f"\n  Checking slug: {slug}")
        # Search by slug/code
        r_s = requests.get(f"{base_url}/search?keyword={slug}")
        items = r_s.json().get("data", {}).get("content", [])
        if items:
            item = items[0]
            print(f"    Title:           {item.get('titleEnglish')}")
            print(f"    Category:        {item.get('categoryName')}")
            print(f"    Department:      {item.get('departmentName')}")
            print(f"    State/Level:     {item.get('applicableStates')} ({item.get('schemeType')})")
            print(f"    Detailed Desc:   {len(str(item.get('detailedDescription')))} chars")
            print(f"    Eligibility:     {len(str(item.get('eligibilityText')))} chars")
            print(f"    App Process:     {len(str(item.get('applicationProcess')))} chars")
        else:
            print(f"    Slug {slug} not found in search results")

if __name__ == "__main__":
    audit_full_system()
