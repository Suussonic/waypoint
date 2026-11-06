"""Batch three restyles (day styles).

Satellite-based: Far Cry 5, Death Stranding (Bridges), Horizon Zero Dawn.
Clean vector, no imagery: Forza Horizon, Assassin's Creed, Ghost of Tsushima, Spider-Man.
All satellite worlds (these three plus Elden Ring, Zelda and Hogwarts) have building footprints, edges
and extrusions hidden, so the imagery is never overlaid with forced blocks.

Run:  python3 -B tools/map-textures/apply_batch3_styles.py   (on restored originals, see batch one note)
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from apply_batch1_styles import ROADS, Style  # noqa: E402
from apply_satellite_styles import satellite, thin_land  # noqa: E402

BUILDING_LAYERS = ("buildings", "building-edges", "building-extrusions")


def hide_buildings(s):
    for short in BUILDING_LAYERS:
        if s.has(f"waypoint-{short}"):
            s.get(short).setdefault("layout", {})["visibility"] = "none"


def roads(s, colour, casing_colour=None, casing_opacity=0.9, opacity=1.0):
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color=colour, line_opacity=opacity)
        if s.has(f"waypoint-{klass}-casing"):
            if casing_colour:
                s.paint(f"{klass}-casing", line_color=casing_colour, line_opacity=casing_opacity)
            else:
                s.paint(f"{klass}-casing", line_opacity=0)


def hillshade(s, shadow, highlight, accent, exaggeration):
    if s.has("waypoint-terrain-relief"):
        s.paint("terrain-relief", hillshade_shadow_color=shadow, hillshade_highlight_color=highlight,
                hillshade_accent_color=accent, hillshade_exaggeration=exaggeration)


def land(s, **colours):
    for short, colour in colours.items():
        short = short.replace("__", "-")
        if s.has(f"waypoint-{short}"):
            s.paint(short, fill_color=colour, fill_opacity=1)


def before_first_symbol(s, layer):
    if s.has(layer["id"]):
        return
    idx = next(i for i, l in enumerate(s.layers) if l["type"] == "symbol")
    s.layers.insert(idx, layer)


def horizon():
    s = Style("horizon.json")  # Forza Horizon: clean terrain colours with relief, white roads, bright sea
    land(s, **{"landcover-woodland": "#4f7a3c", "landcover-open": "#7d9a4e", "landcover-wetland": "#5a8a5e",
               "landcover-bare": "#c8b98a", "land-park": "#5f8a45", "land-residential": "#d2c9a8",
               "land-urban": "#cbc2a1", "land-amenities": "#aab07a"})
    hillshade(s, "#3a4a2a", "#fff6d6", "#6a7a4a", 0.9)
    s.paint("water", fill_color="#3f86c6", fill_outline_color="#9fd0f5")
    s.line_over("water", "fz-shore", "water", line_color="#e8f4ff", line_width=1.4, line_opacity=0.8)
    roads(s, "#ffffff", "#6f7378", 0.9)
    s.road_widths(1.3)
    hide_buildings(s)
    s.save()


def far_cry():
    s = Style("far_cry.json")  # Far Cry 5: dark satellite, aqua water, pale tan roads
    satellite(s, {"raster-saturation": -0.2, "raster-contrast": 0.2, "raster-brightness-min": 0.0,
                  "raster-brightness-max": 0.72}, "#0a1a2a", 0.25, 0)
    thin_land(s, 0.1, 0.06, 0.1, 0.1, 0.05, 0.2, 0.2, 0.15)
    s.paint("water", fill_color="#12324a", fill_outline_color="#2c6b8a")
    roads(s, "#d8cba8", None)
    hide_buildings(s)
    s.save()


def bridges():
    s = Style("bridges.json")  # Death Stranding: a projected, scanned terrain hologram
    satellite(s, {"raster-saturation": -1.0, "raster-contrast": 0.42, "raster-brightness-min": 0.0,
                  "raster-brightness-max": 0.78}, "#0a3f86", 0.66, 0)
    thin_land(s, 0.05, 0.03, 0.06, 0.06, 0.03, 0.1, 0.1, 0.08)
    hillshade(s, "#010a18", "#8fd0ff", "#1c5a9a", 1.0)
    s.paint("water", fill_color="#021226", fill_outline_color="#3aa0e8")
    s.line_over("water", "ds-shore", "water", line_color="#6fd0ff", line_width=1.2, line_opacity=0.7, line_blur=0.8)
    s.paint("primary-roads", line_color="#8fe4ff")
    s.paint("secondary-roads", line_color="#6fd0f5")
    s.paint("local-roads", line_color="#5fc0ee", line_opacity=0.55)
    for klass in ROADS:
        if s.has(f"waypoint-{klass}-casing"):
            s.paint(f"{klass}-casing", line_opacity=0)
    s.glow_under_roads("#4fb8f0", 3.0, 0.22, 1.8)
    hide_buildings(s)
    before_first_symbol(s, {"id": "bs-scan", "type": "fill", "source": "world-tint",
                            "paint": {"fill-pattern": "bs-scan", "fill-opacity": 1}})
    s.save()


def horizon_wilds():
    s = Style("horizon_wilds.json")  # Horizon Zero Dawn: black void, frosted white land, orange route
    satellite(s, {"raster-saturation": -1.0, "raster-contrast": 0.6, "raster-brightness-min": 0.1,
                  "raster-brightness-max": 1.0}, "#000000", 0.3, 0)
    thin_land(s, 0.05, 0.03, 0.05, 0.05, 0.03, 0.1, 0.1, 0.08)
    s.paint("water", fill_color="#000000", fill_outline_color="#000000")
    roads(s, "#ff9a3c", None)
    hide_buildings(s)
    s.save()


def assassin():
    s = Style("assassin.json")  # Assassin's Creed: warm sepia relief, flat cream blocks, off-white roads
    land(s, **{"landcover-woodland": "#bba878", "landcover-open": "#cdb98c", "landcover-wetland": "#b4a67e",
               "landcover-bare": "#d3bf92", "land-park": "#bba878", "land-residential": "#d9c8a0",
               "land-urban": "#cfbd93", "land-amenities": "#c5b387"})
    hillshade(s, "#6a5430", "#f1e4bc", "#8a6e42", 0.8)
    s.paint("water", fill_color="#5a7080", fill_outline_color="#3d4f5a")
    s.paint("buildings", fill_color="#efe3c7", fill_opacity=1, fill_outline_color="#8a7455")
    if s.has("waypoint-building-extrusions"):
        s.get("building-extrusions").setdefault("layout", {})["visibility"] = "none"
    roads(s, "#f7efd9", "#7a6444", 0.85)
    s.save()


def ghost():
    s = Style("ghost.json")  # Ghost of Tsushima: ink and wash on warm paper, no imagery, no blocks
    land(s, **{"landcover-woodland": "#dcd5c1", "landcover-open": "#e8e2d0", "landcover-wetland": "#e0dac8",
               "landcover-bare": "#ebe4d2", "land-park": "#dcd5c1", "land-residential": "#ece6d5",
               "land-urban": "#e6dfcc", "land-amenities": "#e3dcc9"})
    hillshade(s, "#8f897a", "#fbf7ea", "#b8b2a0", 0.6)
    s.paint("water", fill_color="#f7f3e6", fill_outline_color="#2a2824")
    s.line_over("water", "gt-shore", "water", line_color="#2a2824", line_width=1.5, line_opacity=0.85)
    for short in ("landcover-woodland", "land-park", "landcover-bare"):
        s.pattern_over(short, f"gt-ink-{short.replace('landcover-', '').replace('land-', '')}", "gt-ink", 0.9)
    roads(s, "#26231f", None, opacity=0.92)
    hide_buildings(s)
    s.save()


def spider():
    s = Style("spider.json")  # Spider-Man: dark navy ground, light-blue building blocks, slate roads
    for short, colour in (("land-residential", "#0d1626"), ("land-urban", "#101b2e"), ("land-amenities", "#112238"),
                          ("landcover-woodland", "#0f2230"), ("landcover-open", "#0e1d2d"),
                          ("landcover-wetland", "#0d2230"), ("landcover-bare", "#0e1a2a"), ("land-park", "#12303c")):
        if s.has(f"waypoint-{short}"):
            s.paint(short, fill_color=colour, fill_opacity=1)
    s.paint("water", fill_color="#08111f", fill_outline_color="#1c3a5a")
    s.paint("buildings", fill_color="#5fa8dc", fill_opacity=0.8, fill_outline_color="#bfe3ff")
    if s.has("waypoint-building-extrusions"):
        ext = s.get("building-extrusions")["paint"]
        ext["fill-extrusion-color"] = "#6fb7e8"
        ext["fill-extrusion-opacity"] = 0.9
    roads(s, "#7d96b5", "#0a1424", 0.9)
    s.save()


def hide_only(name):
    """Earlier satellite worlds: no forced blocks over the imagery."""
    s = Style(name)
    hide_buildings(s)
    s.save()


if __name__ == "__main__":
    for fn in (horizon, far_cry, bridges, horizon_wilds, assassin, ghost, spider):
        fn()
    for name in ("lands_between.json", "hyrule.json", "hogwarts.json"):
        hide_only(name)
