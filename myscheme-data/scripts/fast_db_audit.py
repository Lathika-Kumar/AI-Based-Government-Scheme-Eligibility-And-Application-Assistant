import requests
import json

def fast_audit():
    base_url = "http://localhost:8082/api/v1/schemes"
    print("==================================================")
    print("FAST POST-FIX MIGRATION AUDIT")
    print("==================================================")
    r = requests.get(f"{base_url}/all")
    schemes = r.json().get("data", [])
    print(f"Total schemes in DB via REST API: {len(schemes)}")
    
    real_schemes = [s for s in schemes if s.get("slug") is not None]
    print(f"Real myScheme items in DB: {len(real_schemes)}")
    
    app_proc_count = 0
    benefits_schemes_count = 0
    total_benefit_rows = 0
    
    sample_size = min(200, len(real_schemes))
    print(f"\nSampling first {sample_size} real schemes via REST API...")
    for s in real_schemes[:sample_size]:
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

    print("\nSAMPLE METRICS:")
    print(f"Populated APPLICATION_PROCESS : {app_proc_count} / {sample_size} ({app_proc_count/sample_size*100:.2f}%)")
    print(f"Distinct Schemes with Benefits: {benefits_schemes_count} / {sample_size} ({benefits_schemes_count/sample_size*100:.2f}%)")
    print(f"Total Benefit Rows in Sample  : {total_benefit_rows} (Avg: {total_benefit_rows/benefits_schemes_count:.1f} per scheme)")

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
    fast_audit()
