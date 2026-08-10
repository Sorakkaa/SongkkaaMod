import os
import hashlib
from collections import defaultdict

rp_path = r"D:\Songgka\run\resourcepacks"
target_dir = None
for item in os.listdir(rp_path):
    if "Sora" in item:
        target_dir = os.path.join(rp_path, item)
        break

if not target_dir:
    print("Could not find Sora directory.")
    exit(1)

print(f"Checking directory: {target_dir}")

all_files = []
for root, dirs, files in os.walk(target_dir):
    for file in files:
        all_files.append(os.path.join(root, file))

print(f"Total files: {len(all_files)}")

hashes = defaultdict(list)
names = defaultdict(list)

for file_path in all_files:
    try:
        with open(file_path, "rb") as f:
            file_hash = hashlib.md5(f.read()).hexdigest()
        hashes[file_hash].append(file_path)
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        
    name = os.path.basename(file_path).lower()
    names[name].append(file_path)

duplicates_found = False
print("\n--- Duplicate Files by Content (Hash) ---")
for h, paths in hashes.items():
    if len(paths) > 1:
        # Ignore common files that are supposed to be identical like pack.png or .json files with same content
        if paths[0].endswith(".json") and len(paths) < 5:
             pass # Maybe skip JSON duplicates if there are few? Let's print all.
             
        duplicates_found = True
        print(f"Duplicate files (MD5: {h}):")
        for p in paths:
            print(f"  - {os.path.relpath(p, target_dir)}")

if not duplicates_found:
    print("No duplicate files found.")

name_duplicates_found = False
print("\n--- Files with identical names in different folders ---")
for name, paths in names.items():
    if len(paths) > 1 and name not in ["pack.mcmeta", "sounds.json", "tick.json"] and not name.endswith(".lang"):
        name_duplicates_found = True
        print(f"Files with name '{name}':")
        for p in paths:
            print(f"  - {os.path.relpath(p, target_dir)}")

if not name_duplicates_found:
    print("No identical names found.")
