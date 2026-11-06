"""Rebuilds road widths in every style so streets read like a navigation map (Google-style widths).

Run:  python3 -B tools/map-textures/broaden_roads.py   (idempotent: it sets absolute zoom curves)
Applies to all day and night styles. Fill width curves are per road class; casings, tunnels and the glow
and ink-bleed helper layers are derived from the fill width. Pixel World keeps its chunky block roads (x1.25).
Widths are map pixels at the stated zoom; they grow with zoom like the map itself.
"""
import json
import os
import re

STYLES = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "styles")
ZOOMS = [5, 10, 13, 15, 17, 18, 19, 20]
FILL = {
    "primary": [0.7, 1.4, 3.0, 5.6, 10.0, 15.0, 23.0, 34.0],
    "secondary": [0.5, 1.0, 2.2, 4.2, 7.5, 11.5, 18.0, 27.0],
    "local": [0.3, 0.6, 1.2, 2.6, 5.4, 9.0, 14.5, 22.0],
}
PATH = [0.3, 0.5, 0.8, 1.3, 2.0, 3.0, 4.5, 6.5]
ID = re.compile(r"^waypoint-(primary|secondary|local)-(roads|casing|tunnels)$|^glow-(primary|secondary|local)$|^ow-road-bleed$")


def curve(values, scale=1.0):
    expr = ["interpolate", ["linear"], ["zoom"]]
    for z, v in zip(ZOOMS, values):
        expr += [z, round(v * scale, 3)]
    return expr


def klass_of(layer_id):
    m = re.search(r"(primary|secondary|local)", layer_id)
    return m.group(1) if m else "local"


for name in sorted(os.listdir(STYLES)):
    if not name.endswith(".json"):
        continue
    path = os.path.join(STYLES, name)
    style = json.load(open(path))
    pixel = name.startswith("pixel")
    scale = 1.25 if pixel else 1.0
    changed = 0
    for layer in style["layers"]:
        lid = layer["id"]
        paint = layer.get("paint", {})
        if layer["type"] != "line":
            continue
        if ID.match(lid):
            base = FILL[klass_of(lid) if lid != "ow-road-bleed" else "local"]
            if "casing" in lid:
                width = [v * 1.18 + 1.2 for v in base]
            elif lid.startswith("glow-") or lid == "ow-road-bleed":
                width = [v * 3.0 for v in base]
            else:
                width = base
            paint["line-width"] = curve(width, scale)
            changed += 1
        elif lid == "waypoint-path-roads":
            paint["line-width"] = curve(PATH)
            changed += 1
    with open(path, "w") as f:
        json.dump(style, f, separators=(",", ":"))
        f.write("\n")
    print(name, "road layers", changed)
