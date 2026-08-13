import requests
import json

def audit_database():
    base_url = "http://localhost:8082/api/v1/schemes"
    
    print("==================================================")
    print("PHASE 2B POST-FIX MIGRATION AUDIT & API VERIFICATION")
    print("==================================================")
    
    r = requests.get(f"{base_url}/all")
    all_schemes = r.json().get("data", [])
    total_schemes = len(all_schemes)
    
    print(f"\n[1] TOTAL SCHEMES IN ORACLE DB VIA REST API: {total_schemes}")
    
    real_schemes = [s for s in all_schemes if s.get("slug") is not None]
    seed_schemes = [s for s in all_schemes if s.get("slug") is None]
    
    print(f"  - Real myScheme Records: {len(real_schemes)}")
    print(f"  - Preserved Seed Records: {len(seed_schemes)}")
    
    # Audit applicationProcess and benefits counts across all schemes via REST API
    app_proc_populated = 0
    benefits_row_count = 0
    schemes_with_benefits = 0
    
    print("\nAuditing full dataset via Core Service REST API...")
    for idx, s in enumerate(real_schemes):
        scheme_id = s.get("id")
        r_detail = requests.get(f"{base_url}/{scheme_id}")
        if r_detail.status_code == 200:
            d = r_detail.json().get("data", {})
            
            app_p = d.get("applicationProcess")
            if app_p and str(app_p).strip():
                app_proc_populated += 1
                
            bens = d.get("benefits", [])
            if bens and len(bens) > 0:
                schemes_with_benefits += 1
                benefits_row_count += len(bens)

    print("\n==================================================")
    print("VERIFIED POST-FIX METRICS:")
    print("==================================================")
    print(f"TOTAL SCHEMES DB COUNT              : {total_schemes} (Target: 4,682)")
    print(f"REAL MYSCHEME RECORDS               : {len(real_schemes)} (Target: 4,675)")
    print(f"PRESERVED SEED RECORDS              : {len(seed_schemes)} (Target: 7)")
    print(f"POPULATED APPLICATION_PROCESS       : {app_proc_populated} / {len(real_schemes)} ({app_proc_populated/len(real_schemes)*100:.2f}%)")
    print(f"TOTAL SCHEME_BENEFITS ROWS          : {benefits_row_count}")
    print(f"DISTINCT SCHEMES WITH BENEFITS      : {schemes_with_benefits} / {len(real_schemes)} ({schemes_with_benefits/len(real_schemes)*100:.2f}%)")

    print("\n==================================================")
    print("SAMPLING 5 REPRESENTATIVE REAL SCHEMES:")
    print("==================================================")
    sample_slugs = ["kcc", "108easuk", "15dsugt", "1pmy", "ab-pmjay"]
    for slug in sample_slugs:
        match = next((item for item in real_schemes if item.get("slug") == slug), None)
        if match:
            s_id = match.get("id")
            r_det = requests.get(f"{base_url}/{s_id}")
            if r_det.status_code == 200:
                d = r_det.json().get("data", {})
                print(f"\nScheme Slug: {d.get('slug')} (ID: {d.get('id')})")
                print(f"  Title              : {d.get('titleEnglish')}")
                print(f"  Category           : {d.get('categoryName')}")
                print(f"  Department         : {d.get('departmentName')}")
                print(f"  App Process Length : {len(str(d.get('applicationProcess')))} chars")
                print(f"  App Process Snippet: {str(d.get('applicationProcess'))[:160]}...")
                print(f"  Benefit Count      : {len(d.get('benefits', []))}")
                if d.get('benefits'):
                    print(f"  First Benefit Item : {d.get('benefits')[0].get('descriptionEnglish')}")

if __name__ == "__main__":
    audit_database()
