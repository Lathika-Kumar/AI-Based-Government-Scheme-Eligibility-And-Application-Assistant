import json
import os
from collections import Counter

raw_dir = r"d:\schemeBridge\myscheme-data\raw\details"

def get_node_type(val):
    if val is None:
        return "null"
    elif isinstance(val, bool):
        return "boolean"
    elif isinstance(val, (int, float)):
        return "number"
    elif isinstance(val, str):
        return "string"
    elif isinstance(val, list):
        return "list"
    elif isinstance(val, dict):
        return "dict"
    return type(val).__name__

app_shapes = Counter()
app_paths = Counter()
benefits_shapes = Counter()
benefits_paths = Counter()

app_samples = {}
benefits_samples = {}

total_files = 0
valid_files = 0

for fname in os.listdir(raw_dir):
    if not fname.endswith(".json"):
        continue
    total_files += 1
    fpath = os.path.join(raw_dir, fname)
    
    try:
        with open(fpath, "r", encoding="utf-8") as f:
            root = json.load(f)
    except Exception as e:
        continue

    data = root.get("data")
    if not data or not isinstance(data, dict):
        continue
    valid_files += 1

    # 1. Analyze applicationProcess
    # Check possible paths for applicationProcess
    # Path 1: data.en.applicationProcess
    # Path 2: data.en.applicationProcess_md
    # Path 3: data.en.schemeContent.applicationProcess
    # Path 4: data.en.applicationProcess.process / steps / mode
    en = data.get("en") or {}
    content = en.get("schemeContent") or {}
    basic = en.get("basicDetails") or {}

    # Search for all keys related to application process across data
    app_val = None
    app_found_path = None

    if "applicationProcess" in en:
        app_val = en.get("applicationProcess")
        app_found_path = "data.en.applicationProcess"
    elif "applicationProcess" in content:
        app_val = content.get("applicationProcess")
        app_found_path = "data.en.schemeContent.applicationProcess"
    elif "applicationProcess" in basic:
        app_val = basic.get("applicationProcess")
        app_found_path = "data.en.basicDetails.applicationProcess"
    else:
        # Check recursively in en
        for k, v in en.items():
            if "application" in k.lower() or "process" in k.lower():
                app_val = v
                app_found_path = f"data.en.{k}"
                break

    if app_found_path is None:
        shape_key = "MISSING"
    else:
        app_paths[app_found_path] += 1
        t = get_node_type(app_val)
        if t == "list":
            if len(app_val) == 0:
                shape_key = "EMPTY_LIST"
            else:
                elem_types = sorted(list(set(get_node_type(item) for item in app_val)))
                shape_key = f"LIST_OF_{'_AND_'.join(elem_types).upper()}"
                
                # Check dict keys if list of dicts
                if "dict" in elem_types:
                    dict_keys = set()
                    for item in app_val:
                        if isinstance(item, dict):
                            dict_keys.update(item.keys())
                    shape_key += f"_KEYS({','.join(sorted(list(dict_keys)))})"
        elif t == "dict":
            shape_key = f"DICT_KEYS({','.join(sorted(list(app_val.keys())))})"
        elif t == "string":
            if not app_val.strip():
                shape_key = "EMPTY_STRING"
            else:
                shape_key = "STRING"
        elif t == "null":
            shape_key = "NULL"
        else:
            shape_key = t.upper()

    app_shapes[shape_key] += 1
    if shape_key not in app_samples:
        app_samples[shape_key] = {
            "file": fname,
            "path": app_found_path,
            "sample": app_val if not isinstance(app_val, str) else app_val[:150]
        }

    # 2. Analyze benefits
    ben_val = None
    ben_found_path = None

    if "benefits" in content:
        ben_val = content.get("benefits")
        ben_found_path = "data.en.schemeContent.benefits"
    elif "benefits" in en:
        ben_val = en.get("benefits")
        ben_found_path = "data.en.benefits"
    elif "benefits" in basic:
        ben_val = basic.get("benefits")
        ben_found_path = "data.en.basicDetails.benefits"

    if ben_found_path is None:
        b_shape_key = "MISSING"
    else:
        benefits_paths[ben_found_path] += 1
        t = get_node_type(ben_val)
        if t == "list":
            if len(ben_val) == 0:
                b_shape_key = "EMPTY_LIST"
            else:
                elem_types = sorted(list(set(get_node_type(item) for item in ben_val)))
                b_shape_key = f"LIST_OF_{'_AND_'.join(elem_types).upper()}"
                if "dict" in elem_types:
                    dict_keys = set()
                    for item in ben_val:
                        if isinstance(item, dict):
                            dict_keys.update(item.keys())
                    b_shape_key += f"_KEYS({','.join(sorted(list(dict_keys)))})"
        elif t == "dict":
            b_shape_key = f"DICT_KEYS({','.join(sorted(list(ben_val.keys())))})"
        elif t == "string":
            b_shape_key = "STRING"
        elif t == "null":
            b_shape_key = "NULL"
        else:
            b_shape_key = t.upper()

    benefits_shapes[b_shape_key] += 1
    if b_shape_key not in benefits_samples:
        benefits_samples[b_shape_key] = {
            "file": fname,
            "path": ben_found_path,
            "sample": ben_val if not isinstance(ben_val, str) else ben_val[:150]
        }

print("==================================================")
print(f"ANALYSIS RESULT FOR {valid_files} VALID FILES:")
print("==================================================")

print("\n--- APPLICATION PROCESS PATHS ---")
for p, c in app_paths.items():
    print(f"  {p:45s}: {c}")

print("\n--- APPLICATION PROCESS SHAPES ---")
for s, c in app_shapes.most_common():
    print(f"  {s:65s}: {c}")

print("\n--- BENEFITS PATHS ---")
for p, c in benefits_paths.items():
    print(f"  {p:45s}: {c}")

print("\n--- BENEFITS SHAPES ---")
for s, c in benefits_shapes.most_common():
    print(f"  {s:65s}: {c}")

print("\n==================================================")
print("SAMPLES BY SHAPE:")
print("==================================================")

print("\n[Application Process Samples]")
for s, data in app_samples.items():
    print(f"\nShape: {s}")
    print(f"  File:   {data['file']}")
    print(f"  Path:   {data['path']}")
    print(f"  Sample: {str(data['sample'])[:300]}")

print("\n[Benefits Samples]")
for s, data in benefits_samples.items():
    print(f"\nShape: {s}")
    print(f"  File:   {data['file']}")
    print(f"  Path:   {data['path']}")
    print(f"  Sample: {str(data['sample'])[:300]}")
