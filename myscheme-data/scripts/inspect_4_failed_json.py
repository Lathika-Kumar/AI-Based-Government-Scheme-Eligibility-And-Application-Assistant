import json
import os

files = ["esdp.json", "fafmftcwd.json", "visvasi.json", "vpby.json"]
base_dir = r"d:\schemeBridge\myscheme-data\raw\details"

for filename in files:
    filepath = os.path.join(base_dir, filename)
    print(f"\n==========================================")
    print(f"File: {filename} (Size: {os.path.getsize(filepath)} bytes)")
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
            print("Keys:", list(data.keys()))
            if "data" in data:
                print("data type:", type(data["data"]))
                print("data content sample:", str(data["data"])[:200])
            else:
                print("Raw JSON top-level sample:", str(data)[:200])
    except Exception as e:
        print(f"Error loading {filename}: {e}")
