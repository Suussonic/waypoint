"""Adds the Old West texture layers to the Old West styles. Safe to re-run (idempotent).

Run:  python3 -I tools/map-textures/apply_old_west_style.py

Day style: new pattern layers plus ink-line local roads. Night style: pattern layers only,
so night colours stay as authored.
"""
import copy
import json
import os

STYLES = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "styles")
INK = "#4a3a28"


def pattern_layer(anchor, new_id, image, minzoom, opacity=1.0):
    layer = {
        "id": new_id,
        "type": "fill",
        "source": anchor["source"],
        "source-layer": anchor["source-layer"],
        "minzoom": minzoom,
        "paint": {"fill-pattern": image, "fill-opacity": opacity},
    }
    if "filter" in anchor:
        layer["filter"] = copy.deepcopy(anchor["filter"])
    return layer


def scale_width(expr, factor):
    out = copy.deepcopy(expr)
    for i in range(4, len(out), 2):
        out[i] = round(max(out[i] * factor, 0.5), 3)
    return out


def apply(name, roads):
    path = os.path.join(STYLES, name)
    style = json.load(open(path))
    layers = style["layers"]
    if any(l["id"].startswith("ow-") for l in layers):
        print(name, "already has Old West layers, skipping layer insert")
    else:
        by_id = {l["id"]: l for l in layers}
        inserts = {
            "waypoint-landcover-woodland": [pattern_layer(by_id["waypoint-landcover-woodland"], "ow-trees", "ow-trees", 11, ["interpolate", ["linear"], ["zoom"], 11, 0.3, 14, 0.7, 17, 0.9])],
            "waypoint-landcover-open": [pattern_layer(by_id["waypoint-landcover-open"], "ow-grass", "ow-grass", 12)],
            "waypoint-landcover-bare": [pattern_layer(by_id["waypoint-landcover-bare"], "ow-stipple", "ow-stipple", 11)],
            "waypoint-land-park": [pattern_layer(by_id["waypoint-land-park"], "ow-park-grass", "ow-grass", 13)],
            "waypoint-water": [
                pattern_layer(by_id["waypoint-water"], "ow-ripples", "ow-ripple", 9, 0.9),
                {
                    "id": "ow-shore",
                    "type": "line",
                    "source": by_id["waypoint-water"]["source"],
                    "source-layer": "water",
                    "paint": {
                        "line-color": INK,
                        "line-width": ["interpolate", ["linear"], ["zoom"], 5, 0.4, 10, 0.8, 14, 1.3, 18, 2.2],
                        "line-opacity": 0.7,
                    },
                },
            ],
            "waypoint-buildings": [pattern_layer(by_id["waypoint-buildings"], "ow-hatch", "ow-hatch", 15, 0.9)],
        }
        rebuilt = []
        for layer in layers:
            rebuilt.append(layer)
            rebuilt.extend(inserts.get(layer["id"], []))
        style["layers"] = rebuilt
    if roads:
        # Ink bleed: a soft, wider, translucent stroke under every local road (inserted once).
        if not any(l["id"] == "ow-road-bleed" for l in style["layers"]):
            rebuilt = []
            for layer in style["layers"]:
                if layer["id"] == "waypoint-local-roads":
                    bleed = copy.deepcopy(layer)
                    bleed["id"] = "ow-road-bleed"
                    bleed["paint"] = {
                        "line-color": INK,
                        "line-width": scale_width(layer["paint"]["line-width"], 3.2),
                        "line-opacity": 0.16,
                        "line-blur": 1.4,
                    }
                    rebuilt.append(bleed)
                rebuilt.append(layer)
            style["layers"] = rebuilt
        for layer in style["layers"]:
            if layer["id"] == "waypoint-landcover-woodland":
                layer["paint"]["fill-color"] = "#a9a97c"
                layer["paint"]["fill-opacity"] = 0.55
        for layer in style["layers"]:
            if layer["id"] == "waypoint-local-casing":
                layer["paint"]["line-opacity"] = 0
            elif layer["id"] == "waypoint-local-roads":
                layer["paint"]["line-color"] = INK
                layer["paint"]["line-width"] = scale_width(layer["paint"]["line-width"], 1.6)
    with open(path, "w") as f:
        json.dump(style, f, separators=(",", ":"))  # keep the compact one-line format the styles use
        f.write("\n")
    print("wrote", name, len(style["layers"]), "layers")


if __name__ == "__main__":
    apply("old_west.json", roads=True)
    apply("old_west_night.json", roads=False)
