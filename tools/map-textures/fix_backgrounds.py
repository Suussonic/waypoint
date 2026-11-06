"""Gives each restyled world its own ground colour instead of the old shared "world-material" tile.

Why: wherever the vector tiles have no land polygon (rural land, and every low zoom) the style's
background shows. The old material tile is a light tan for most worlds, so a black Liberty City or a violet
Midnight Run turned cream as soon as the map was zoomed out or panned over open country.

Run:  python3 -B tools/map-textures/fix_backgrounds.py   then   make_night_styles.py   (night is derived).
Idempotent. Satellite worlds get a dark ground that only shows until the imagery tiles arrive.
"""
import json
import os

STYLES = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "styles")

GROUND = {
    "los_santos": "#1c1b1a", "liberty": "#0e0f10", "night_city": "#0b1220", "green_terminal": "#0a1a0d",
    "heat": "#2a1050", "san_andreas": "#5b8c3a", "vice_coast": "#3f9a5a", "pixel": "#5f9440",
    "ctos": "#0f2f5c", "spider": "#0d1626", "horizon": "#7d9a4e", "ghost": "#ece6d5",
    "rivia": "#cdb887", "skyrim": "#d9ca9c", "assassin": "#d3bf92", "old_west": "#d9c79a",
    "lands_between": "#2a2f22", "hyrule": "#2a2b1c", "hogwarts": "#262a25", "far_cry": "#16212a",
    "bridges": "#031a36", "horizon_wilds": "#000000",
}

for world, colour in GROUND.items():
    path = os.path.join(STYLES, world + ".json")
    style = json.load(open(path))
    for layer in style["layers"]:
        if layer["type"] == "background":
            layer["paint"].pop("background-pattern", None)
            layer["paint"]["background-color"] = colour
    with open(path, "w") as f:
        json.dump(style, f, separators=(",", ":"))
        f.write("\n")
    print("ground", world, colour)


def generic_pass():
    """Every style still using the shared material tile gets its own land colour as ground.

    Used for the worlds not listed above (their day styles) and for every night file after
    make_night_styles.py has run, so night grounds follow the night land colours.
    """
    changed = []
    for name in sorted(os.listdir(STYLES)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(STYLES, name)
        style = json.load(open(path))
        bg = next((l for l in style["layers"] if l["type"] == "background"), None)
        if bg is None or "background-pattern" not in bg.get("paint", {}):
            continue
        colour = None
        for lid in ("waypoint-land-residential", "waypoint-land-urban", "waypoint-landcover-open", "waypoint-land-amenities"):
            layer = next((l for l in style["layers"] if l["id"] == lid), None)
            value = layer["paint"].get("fill-color") if layer else None
            if isinstance(value, str) and value.startswith("#"):
                colour = value
                break
        if colour is None:
            continue
        bg["paint"].pop("background-pattern")
        bg["paint"]["background-color"] = colour
        with open(path, "w") as f:
            json.dump(style, f, separators=(",", ":"))
            f.write("\n")
        changed.append(name)
    print("generic ground for", len(changed), "styles")


if __name__ == "__main__" and os.environ.get("GROUND_GENERIC"):
    generic_pass()
