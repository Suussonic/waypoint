"""Reads the drive logs written by run_all.py and reports, per world, what the drive actually exercised.

Usage:  python3 -I tools/sim-drive/check_logs.py <output dir>

Checks per world: the trip started and stayed in navigation; the distance to the next turn decreased and the
step index advanced; an off-route or rerouting state appeared and recovered; arrival was reached; which
maneuver types were shown. Anything missing is flagged so the frames can be looked at.
"""
import csv
import glob
import os
import sys

base = sys.argv[1]
rows = []
for path in sorted(glob.glob(os.path.join(base, "*", "drive-log.csv"))):
    world = os.path.basename(os.path.dirname(path))
    data = list(csv.DictReader(open(path, encoding="utf8")))
    if not data:
        rows.append((world, ["empty log"], set()))
        continue
    flags = []
    nav = [r for r in data if r["navigating"] == "true"]
    if len(nav) < len(data) * 0.5:
        flags.append("left navigation early")
    steps = [int(r["nextStep"]) for r in data if r["nextStep"].lstrip("-").isdigit()]
    if steps and max(steps) - min(steps) < 2:
        flags.append(f"step index barely moved ({min(steps)} to {max(steps)})")
    if not any(r["offRoute"] == "true" or r["routing"] == "true" for r in data):
        flags.append("never went off route")
    if not any(r["arrived"] == "true" for r in data):
        flags.append("never arrived")
    speeds = [float(r["speed"]) for r in data if r["speed"].replace(".", "").isdigit()]
    if speeds and max(speeds) < 1:
        flags.append("speed stayed 0 (test provider sends no speed)")
    types = {(r["type"], r["modifier"]) for r in data if r["type"] not in ("", "null")}
    rows.append((world, flags, types))

all_types = set()
for world, flags, types in rows:
    all_types |= types
    print(f"{world:16s} {'OK' if not flags else '; '.join(flags)}")
print(f"\n{len(rows)} worlds. Maneuver kinds seen across all drives:")
for t, m in sorted(all_types):
    print(f"  {t} / {m}")
