import requests
import json
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

def complete_audit():
    base_url = "http://localhost:8082/api/v1/schemes"
    print("==================================================")
    print("COMPLETE POST-FIX MIGRATION AUDIT (UTF-8)")
    print("==================================================")
    r = requests.get(f"{base_url}/all")
    schemes = r.json().get("data", [])
    total_schemes = len(schemes)
    print(f"Total schemes in Oracle XE DB via REST API: {total_schemes}")
    
    real_schemes = [s for s in schemes if s.get("slug") is not None]
    seed_schemes = [s for s in schemes if s.get("slug") is None]
    
    print(f"  - Real myScheme Records : {len(real_schemes)}")
    print(f"  - Preserved Seed Records: {len(seed_schemes)}")
    
    app_proc_count = 0
    benefits_schemes_count = 0
    total_benefit_rows = 0
    
    print("\nAuditing all 4,675 real schemes via REST API...")
    for s in real_schemes:
        s_id = s.get("id")
        r_det = requests.get(f"{base_url}/{s_id}")
        if r_det.status_code == 200:
            d = r_det.json().get("data", {})
            app_p = d.get("applicationProcess")
            if app_p and str(app_p).strip():
                app_proc_count += 1
            
            bens = d.get("benefits", [])
            if bens and len(bens) > 0:
                benefits_schemes_count += 1
                total_benefit_rows += len(bens)

    print("\n==================================================")
    print("FINAL VERIFIED POST-FIX METRICS:")
    print("==================================================")
    print(f"TOTAL SCHEMES DB COUNT              : {total_schemes} (Expected: 4,682)")
    print(f"REAL MYSCHEME RECORDS               : {len(real_schemes)} (Expected: 4,675)")
    print(f"PRESERVED SEED RECORDS              : {len(seed_schemes)} (Expected: 7)")
    print(f"POPULATED APPLICATION_PROCESS       : {app_proc_count} / {len(real_schemes)} ({app_proc_count/len(real_schemes)*100:.2f}%)")
    print(f"TOTAL SCHEME_BENEFITS ROWS          : {total_benefit_rows}")
    print(f"DISTINCT SCHEMES WITH BENEFITS      : {benefits_schemes_count} / {len(real_schemes)} ({benefits_schemes_count/len(real_schemes)*100:.2f}%)")

    print("\n==================================================")
    print("SAMPLING 5 REPRESENTATIVE REAL SCHEMES DETAIL:")
    print("==================================================")
    for slug in ["kcc", "108easuk", "15dsugt", "1pmy", "ab-pmjay"]:
        match = next((item for item in real_schemes if item.get("slug") == slug), None)
        if match:
            s_id = match.get("id")
            r_det = requests.get(f"{base_url}/{s_id}")
            if r_det.status_code == 200:
                d = r_det.json().get("data", {})
                print(f"\nScheme Slug: {d.get('slug')} (ID: {d.get('id')})")
                print(f"  Title              : {d.get('titleEnglish')}")
                print(f"  App Process Length : {len(str(d.get('applicationProcess')))} chars")
                print(f"  App Process Snippet: {str(d.get('applicationProcess'))[:140]}...")
                print(f"  Benefit Count      : {len(d.get('benefits', []))}")
                if d.get('benefits'):
                    print(f"  First Benefit Item : {d.get('benefits')[0].get('descriptionEnglish')[:120]}")

if __name__ == "__main__":
    complete_audit()
