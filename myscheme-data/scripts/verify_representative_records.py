import json
import os
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

raw_dir = r"d:\schemeBridge\myscheme-data\raw\details"

populated_samples = ["108easuk.json", "kcc.json", "zvy.json", "mgnrega.json", "ssy.json"]
missing_samples = [
    "15dsugt.json", "1pmy.json", "a-gainer.json", "40shydcs.json", "aal.json",
    "aamgsiscs.json", "aaw.json", "ab-pmjay.json", "abic.json", "able.json"
]

print("==================================================")
print("REPRESENTATIVE RECORDS VERIFICATION:")
print("==================================================")

print("\n--- 5 POPULATED RECORDS SAMPLES ---")
for fname in populated_samples:
    fpath = os.path.join(raw_dir, fname)
    if os.path.exists(fpath):
        with open(fpath, "r", encoding="utf-8") as f:
            data = json.load(f).get("data", {})
        en = data.get("en", {})
        app = en.get("applicationProcess")
        print(f"\nFile: {fname}")
        print(f"applicationProcess Type: {type(app).__name__}")
        if isinstance(app, list) and len(app) > 0:
            item = app[0]
            print(f"Keys in first mode item: {list(item.keys()) if isinstance(item, dict) else 'Not a dict'}")
            print(f"mode: {item.get('mode') if isinstance(item, dict) else ''}")
            print(f"process_md sample: {str(item.get('process_md'))[:150] if isinstance(item, dict) else ''}")

print("\n--- 10 MISSING RECORDS SAMPLES ---")
for fname in missing_samples:
    fpath = os.path.join(raw_dir, fname)
    if os.path.exists(fpath):
        with open(fpath, "r", encoding="utf-8") as f:
            data = json.load(f).get("data", {})
        en = data.get("en", {})
        app = en.get("applicationProcess")
        print(f"\nFile: {fname}")
        print(f"applicationProcess Type: {type(app).__name__}")
        if isinstance(app, list) and len(app) > 0:
            item = app[0]
            print(f"Keys in first mode item: {list(item.keys()) if isinstance(item, dict) else 'Not a dict'}")
            print(f"mode: {item.get('mode') if isinstance(item, dict) else ''}")
            has_proc_md = 'process_md' in item if isinstance(item, dict) else False
            has_proc = 'process' in item if isinstance(item, dict) else False
            print(f"has process_md: {has_proc_md}, has process: {has_proc}")
            if has_proc_md:
                print(f"process_md sample: {str(item.get('process_md'))[:150]}")
