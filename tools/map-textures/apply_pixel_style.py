"""Gives Pixel World block textures and 3D buildings (day style).

Run:  python3 -I tools/map-textures/apply_pixel_style.py   (layer inserts are idempotent)

Changes: block-tile patterns on woodland, open land, parks, bare land and water; roads filled with a
path-block pattern on a dark dirt casing; flat building footprints swap to plank-faced extrusions
(visible when the map is tilted) from zoom 14.5.
"""
import copy
import json
import os

STYLES = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "styles")


def pattern_layer(anchor, new_id, image, minzoom=0):
    layer = {
        "id": new_id,
        "type": "fill",
        "source": anchor["source"],
        "source-layer": anchor["source-layer"],
        "paint": {"fill-pattern": image, "fill-opacity": 1},
    }
    if minzoom:
        layer["minzoom"] = minzoom
    if "filter" in anchor:
        layer["filter"] = copy.deepcopy(anchor["filter"])
    return layer


def scale_width(expr, factor):
    out = copy.deepcopy(expr)
    for i in range(4, len(out), 2):
        out[i] = round(out[i] * factor, 3)
    return out


def apply(name):
    path = os.path.join(STYLES, name)
    style = json.load(open(path))
    layers = style["layers"]
    by_id = {l["id"]: l for l in layers}

    # Swap the old 128 px tiles for the block kit.
    by_id["waypoint-landcover-woodland"]["paint"]["fill-pattern"] = "px-leaves"
    by_id["waypoint-water"]["paint"]["fill-pattern"] = "px-water"
    by_id["waypoint-buildings"]["paint"]["fill-pattern"] = "px-stone"
    by_id["waypoint-buildings"]["maxzoom"] = 14.6  # flat footprints below the extrusion range

    for klass in ("primary", "secondary", "local"):
        by_id[f"waypoint-{klass}-casing"]["paint"]["line-color"] = "#4a3f2a"
        road = by_id[f"waypoint-{klass}-roads"]["paint"]
        road.pop("line-color", None)
        road["line-pattern"] = "px-path"
        # Chunkier roads so the path-block texture can read (local roads most of all).
        factor = {"primary": 1.5, "secondary": 1.8, "local": 2.4}[klass]
        road["line-width"] = scale_width(road["line-width"], factor)
        casing = by_id[f"waypoint-{klass}-casing"]["paint"]
        casing["line-width"] = scale_width(casing["line-width"], factor)

    if not any(l["id"].startswith("px-") for l in layers):
        extrusion = {
            "id": "px-buildings-3d",
            "type": "fill-extrusion",
            "source": by_id["waypoint-buildings"]["source"],
            "source-layer": "building",
            "minzoom": 14.5,
            "paint": {
                "fill-extrusion-pattern": "px-planks",
                "fill-extrusion-height": ["interpolate", ["linear"], ["zoom"], 14.5, 0, 15.5, ["coalesce", ["get", "render_height"], 7]],
                "fill-extrusion-base": ["coalesce", ["get", "render_min_height"], 0],
                "fill-extrusion-opacity": 1,
                "fill-extrusion-vertical-gradient": True,
            },
        }
        inserts = {
            "waypoint-landcover-open": [pattern_layer(by_id["waypoint-landcover-open"], "px-open-grass", "px-grass")],
            "waypoint-landcover-bare": [pattern_layer(by_id["waypoint-landcover-bare"], "px-bare-sand", "px-sand")],
            "waypoint-land-park": [pattern_layer(by_id["waypoint-land-park"], "px-park-grass", "px-grass")],
            "waypoint-land-residential": [pattern_layer(by_id["waypoint-land-residential"], "px-residential-grass", "px-grass")],
            "waypoint-land-urban": [pattern_layer(by_id["waypoint-land-urban"], "px-urban-grass", "px-grass")],
            "waypoint-land-amenities": [pattern_layer(by_id["waypoint-land-amenities"], "px-amenity-grass", "px-grass")],
            "waypoint-building-edges": [extrusion],
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
    apply("pixel.json")  # night is left as authored for this pilot
