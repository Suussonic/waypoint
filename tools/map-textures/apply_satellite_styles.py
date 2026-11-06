"""Satellite-base themed worlds (day styles): Elden Ring, Zelda (Hyrule), Hogwarts.

Run:  python3 -B tools/map-textures/apply_satellite_styles.py   (on restored originals, see batch one note)

Each style gets: an Esri World Imagery raster source, a graded raster
layer directly above the background, a translucent theme tint over the world, and an optional
parchment overlay. The existing land fills become translucent so the imagery reads through; water,
roads, buildings and labels stay drawn by the world's own vector layers on top.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from apply_batch1_styles import ROADS, Style  # noqa: E402

# Single place to change the imagery provider. No API key is committed here.
TILES = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
ATTRIBUTION = "Esri, Maxar, Earthstar Geographics, and the GIS User Community"
WORLD = {
    "type": "Feature",
    "properties": {},
    "geometry": {"type": "Polygon", "coordinates": [[[-180, -85], [180, -85], [180, 85], [-180, 85], [-180, -85]]]},
}


def satellite(s, grade, tint, tint_opacity, paper_opacity):
    data = s.data
    data["sources"]["eox-sentinel"] = {
        "type": "raster", "tiles": [TILES], "tileSize": 256, "minzoom": 0, "maxzoom": 19, "attribution": ATTRIBUTION,
    }
    data["sources"]["world-tint"] = {"type": "geojson", "data": WORLD}
    if s.has("sat-base"):
        return
    layers = [
        {"id": "sat-base", "type": "raster", "source": "eox-sentinel",
         "paint": {**grade, "raster-resampling": "linear", "raster-fade-duration": 0}},
        {"id": "sat-tint", "type": "fill", "source": "world-tint",
         "paint": {"fill-color": tint, "fill-opacity": tint_opacity}},
    ]
    if paper_opacity:
        layers.append({"id": "sat-paper", "type": "fill", "source": "world-tint",
                       "paint": {"fill-pattern": "paper-fibre", "fill-opacity": paper_opacity}})
    background = next(i for i, l in enumerate(s.layers) if l["type"] == "background")
    s.layers[background + 1:background + 1] = layers


def thin_land(s, wood, open_, wet, bare, park, resi, urban, amen):
    """Let the imagery show through: keep a faint theme tint on every land fill."""
    for short, op in (("landcover-woodland", wood), ("landcover-open", open_), ("landcover-wetland", wet),
                      ("landcover-bare", bare), ("land-park", park), ("land-residential", resi),
                      ("land-urban", urban), ("land-amenities", amen)):
        if s.has(f"waypoint-{short}"):
            s.paint(short, fill_opacity=op)


def lands_between():
    s = Style("lands_between.json")
    satellite(s, {"raster-saturation": -0.25, "raster-contrast": 0.15, "raster-hue-rotate": 20,
                  "raster-brightness-min": 0.05, "raster-brightness-max": 0.95}, "#b69a62", 0.30, 0.18)
    thin_land(s, 0.18, 0.12, 0.2, 0.2, 0.15, 0.3, 0.3, 0.25)
    s.save()


def hyrule():
    s = Style("hyrule.json")
    satellite(s, {"raster-saturation": -0.75, "raster-contrast": 0.25, "raster-hue-rotate": 35,
                  "raster-brightness-min": 0.0, "raster-brightness-max": 0.9}, "#9a7a3a", 0.40, 0.12)
    thin_land(s, 0.12, 0.08, 0.15, 0.15, 0.1, 0.25, 0.25, 0.2)
    s.paint("water", fill_color="#5e7f8c", fill_outline_color="#3f5f6a")
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#ead27a")
        if s.has(f"waypoint-{klass}-casing"):
            s.paint(f"{klass}-casing", line_opacity=0)
    s.save()


def hogwarts():
    s = Style("hogwarts.json")
    satellite(s, {"raster-saturation": -0.55, "raster-contrast": 0.05, "raster-hue-rotate": 10,
                  "raster-brightness-min": 0.1, "raster-brightness-max": 0.85}, "#8a8672", 0.32, 0.22)
    thin_land(s, 0.14, 0.1, 0.15, 0.15, 0.1, 0.25, 0.25, 0.2)
    s.paint("water", fill_color="#7c8f98", fill_outline_color="#55676f")
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#e8e2cc", line_opacity=0.45)
        if s.has(f"waypoint-{klass}-casing"):
            s.paint(f"{klass}-casing", line_opacity=0)
    s.save()


if __name__ == "__main__":
    # Restore originals for these three first so thin_land and tints start from the authored values.
    for fn in (lands_between, hyrule, hogwarts):
        fn()
