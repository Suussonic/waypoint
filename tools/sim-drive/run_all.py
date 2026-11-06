"""Runs the 5 minute simulated drive in every world (or the World names given), one after another.

Usage:  python3 -I tools/sim-drive/run_all.py <output dir> [WORLD_NAME ...]

For each world it runs NavDriveTest on the phone (the drive happens inside the app), then pulls the CSV log, the route
and every sixth frame into <output dir>/<world>/ and deletes the frames on the phone.
"""
import os
import subprocess
import sys
import time

PKG = "com.waypoint.app.preview"
OUT = sys.argv[1]
HERE = os.path.dirname(os.path.abspath(__file__))
ORDER = [l.split("(")[0].strip() for l in open(os.path.join(HERE, "..", "..", "app/src/main/java/com/waypoint/app/core/World.kt")).read().splitlines()
         if l.startswith("    ") and '("' in l and l.strip()[:1].isupper()]
worlds = sys.argv[2:] or ORDER
remote = f"/sdcard/Android/data/{PKG}/files/nav-drive"
for world in worlds:
    folder = os.path.join(OUT, world.lower())
    if os.path.exists(os.path.join(folder, "drive-log.csv")):
        print("skip", world, flush=True)
        continue
    os.makedirs(folder, exist_ok=True)
    subprocess.run(["adb", "shell", "rm", "-rf", remote])
    test = subprocess.run(["adb", "shell", "am", "instrument", "-w", "-e", "class", "com.waypoint.app.NavDriveTest", "-e", "world", world,
                           "-e", "seconds", "300", f"{PKG}.test/androidx.test.runner.AndroidJUnitRunner"],
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=420)
    out = test.stdout
    for name in ("drive-log.csv", "drive-route.txt", "drive-steps.txt"):
        subprocess.run(["adb", "pull", f"{remote}/{name}", folder], capture_output=True)
    listing = subprocess.run(["adb", "shell", "ls", remote], capture_output=True, text=True).stdout.split()
    for i, name in enumerate(n for n in sorted(listing) if n.startswith("f") and n.endswith(".png")):
        if i % 6 == 0:
            subprocess.run(["adb", "pull", f"{remote}/{name}", folder], capture_output=True)
    subprocess.run(["adb", "shell", "rm", "-rf", remote])
    ok = "OK (1 test)" in out
    print(world, "OK" if ok else "FAILED", flush=True)
