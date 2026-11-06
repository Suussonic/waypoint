"""Restyles Los Santos toward the flat radar grammar: bright thin roads on dark casing, no building
masses, black water with a faint shoreline, and a mottled brown-grey terrain tint on the land.

Run:  python3 -I tools/map-textures/apply_los_santos_style.py   (idempotent for the layer inserts)
"""
import copy
import json
import os

STYLES = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "styles")
ROAD_COLOUR = {
    "primary": "#d8d8d8",
    "secondary": "#b6b6b6",
    "local": "#909090",
}
CASING = "#080808"


def scale_width(expr, factor):
    out = copy.deepcopy(expr)
    for i in range(4, len(out), 2):
        out[i] = round(out[i] * factor, 3)
    return out


def terrain_layer(anchor, new_id, opacity):
    layer = {
        "id": new_id,
        "type": "fill",
        "source": anchor["source"],
        "source-layer": anchor["source-layer"],
        "minzoom": 8,
        "paint": {"fill-pattern": "ls-terrain", "fill-opacity": opacity},
    }
    if "filter" in anchor:
        layer["filter"] = copy.deepcopy(anchor["filter"])
    return layer


def apply(name):
    path = os.path.join(STYLES, name)
    style = json.load(open(path))
    layers = style["layers"]
    by_id = {l["id"]: l for l in layers}

    # Land: warmer, browner greys so the ground reads as dim terrain.
    tints = {
        "waypoint-landcover-woodland": "#2f2b26",
        "waypoint-landcover-open": "#2c2925",
        "waypoint-landcover-wetland": "#2a2926",
        "waypoint-landcover-bare": "#312c26",
        "waypoint-land-park": "#2e302a",
    }
    for lid, colour in tints.items():
        by_id[lid]["paint"]["fill-color"] = colour

    # Water: near-black; the faint shoreline is added below.
    by_id["waypoint-water"]["paint"]["fill-color"] = "#05080b"
    by_id["waypoint-water"]["paint"]["fill-outline-color"] = "#05080b"

    # Buildings: the reference map draws no footprints, so keep only a whisper at street zoom.
    b = by_id["waypoint-buildings"]["paint"]
    b["fill-opacity"] = ["interpolate", ["linear"], ["zoom"], 14, 0.0, 16, 0.12, 18, 0.26]
    b["fill-outline-color"] = "#2a2a2a"

    # Roads: brighter strokes on a near-black casing, brightest on the main roads.
    for klass, colour in ROAD_COLOUR.items():
        by_id[f"waypoint-{klass}-roads"]["paint"]["line-color"] = colour
        casing = by_id[f"waypoint-{klass}-casing"]["paint"]
        casing["line-color"] = CASING
        casing["line-opacity"] = 0.95
        casing["line-width"] = scale_width(casing["line-width"], 1.12)

    if not any(l["id"].startswith("ls-") for l in layers):
        shore = {
            "id": "ls-shore",
            "type": "line",
            "source": by_id["waypoint-water"]["source"],
            "source-layer": "water",
            "paint": {
                "line-color": "#4a4e52",
                "line-width": ["interpolate", ["linear"], ["zoom"], 8, 0.4, 14, 0.8, 18, 1.6],
                "line-opacity": 0.55,
            },
        }
        inserts = {
            "waypoint-landcover-woodland": [terrain_layer(by_id["waypoint-landcover-woodland"], "ls-terrain-wood", 0.8)],
            "waypoint-landcover-open": [terrain_layer(by_id["waypoint-landcover-open"], "ls-terrain-open", 0.7)],
            "waypoint-landcover-bare": [terrain_layer(by_id["waypoint-landcover-bare"], "ls-terrain-bare", 0.85)],
            "waypoint-land-park": [terrain_layer(by_id["waypoint-land-park"], "ls-terrain-park", 0.7)],
            "waypoint-water": [shore],
        }
        rebuilt = []
        for layer in layers:
            rebuilt.append(layer)
            rebuilt.extend(inserts.get(layer["id"], []))
        style["layers"] = rebuilt

    with open(path, "w") as f:
        json.dump(style, f, separators=(",", ":"))
        f.write("\n")
    print("wrote", name, len(style["layers"]), "layers")


if __name__ == "__main__":
    apply("los_santos.json")
    apply("los_santos_night.json")
