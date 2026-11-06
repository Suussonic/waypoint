"""Batch two restyles (day styles): NFS Heat, Witcher 3, Watch Dogs, Skyrim, Elden Ring.

Run:  python3 -I tools/map-textures/apply_batch2_styles.py   (on restored originals, see batch one note)
Uses the Style helper from apply_batch1_styles.py.
"""
from apply_batch1_styles import ROADS, Style


def land(s, **colours):
    """Set fill colour (and opacity 1) for land layers that exist in this style."""
    for short, colour in colours.items():
        short = short.replace("__", "-")
        if s.has(f"waypoint-{short}"):
            s.paint(short, fill_color=colour, fill_opacity=1)


def casing(s, klass, **kv):
    """Road casing exists in most styles but not all (Skyrim has none)."""
    if s.has(f"waypoint-{klass}-casing"):
        s.paint(f"{klass}-casing", **kv)


def hillshade(s, shadow, highlight, accent, exaggeration):
    if s.has("waypoint-terrain-relief"):
        s.paint("terrain-relief", hillshade_shadow_color=shadow, hillshade_highlight_color=highlight,
                hillshade_accent_color=accent, hillshade_exaggeration=exaggeration)


def heat():
    s = Style("heat.json")
    # Ultraviolet duotone: violet land, electric-blue water, pale hairline roads, lavender blocks.
    land(s, **{"landcover-woodland": "#2a0f4a", "landcover-open": "#331459", "landcover-wetland": "#2a1a5e",
               "landcover-bare": "#3a1c66", "land-park": "#2a0f4a", "land-residential": "#4a2a82",
               "land-urban": "#573196", "land-amenities": "#40237a"})
    hillshade(s, "#12062a", "#8a5cf0", "#3a1a7a", 0.5)
    s.paint("water", fill_color="#1d5bd0", fill_outline_color="#3be0e8")
    s.line_over("water", "heat-shore", "water", line_color="#5ff0f2", line_width=1.6, line_opacity=0.8, line_blur=1.2)
    s.paint("buildings", fill_color="#a583f0", fill_opacity=0.85, fill_outline_color="#c7b0ff")
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#e1dcf5")
        casing(s, klass, line_color="#1a0838", line_opacity=0.9)
    s.save()


def rivia():
    s = Style("rivia.json")
    # Painted ochre and olive, blue-grey water, dry-brush wash over the land.
    land(s, **{"landcover-woodland": "#6f7a45", "landcover-open": "#b59a5c", "landcover-wetland": "#6f8160",
               "landcover-bare": "#c9b27a", "land-park": "#6f7a45", "land-residential": "#cdb887",
               "land-urban": "#c2ac7c", "land-amenities": "#aa9f6e"})
    hillshade(s, "#5a4426", "#f0e0b0", "#7a5a2a", 0.7)
    s.paint("water", fill_color="#6a8f9a", fill_outline_color="#4d7480")
    for short, layer in (("landcover-woodland", "wt-wash-wood"), ("landcover-open", "wt-wash-open"),
                         ("landcover-bare", "wt-wash-bare"), ("land-park", "wt-wash-park")):
        s.pattern_over(short, layer, "wt-wash", 0.9)
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#f0e4bc")
        casing(s, klass, line_color="#6a5230", line_opacity=0.85)
    s.save()


def ctos():
    s = Style("ctos.json")
    # Holographic blue: deep-navy land, lighter-blue footprints, teal parks, glowing pale roads.
    land(s, **{"landcover-woodland": "#17606a", "landcover-open": "#1a5c70", "landcover-wetland": "#17566a",
               "landcover-bare": "#133e66", "land-park": "#1a6b70", "land-residential": "#0f2f5c",
               "land-urban": "#123766", "land-amenities": "#14456a"})
    s.paint("water", fill_color="#1e66b8", fill_outline_color="#5fc7ff")
    s.paint("buildings", fill_color="#2d73b8", fill_opacity=0.95, fill_outline_color="#8fd8ff")
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#cdeeff")
        casing(s, klass, line_color="#0a2342", line_opacity=0.9)
    s.glow_under_roads("#5fc7ff", 3.0, 0.22, 1.8)
    s.save()


def skyrim():
    s = Style("skyrim.json")
    # Aged parchment with inked peaks, flat muted sea, dark red-brown ink roads.
    land(s, **{"landcover-woodland": "#c9b98d", "landcover-open": "#d9ca9c", "landcover-wetland": "#bfb48e",
               "landcover-bare": "#cdbb8c", "land-park": "#c9b98d", "land-residential": "#d6c697",
               "land-urban": "#cdbd90", "land-amenities": "#c8b98c"})
    hillshade(s, "#6b5638", "#f1e5bd", "#8a6c42", 0.8)
    s.paint("water", fill_color="#6f95a8", fill_outline_color="#3f5f70")
    s.pattern_over("landcover-bare", "sk-peaks-bare", "sk-peaks", 1.0)
    s.pattern_over("landcover-woodland", "sk-peaks-wood", "sk-peaks", 0.7)
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#6b3a2a")
        casing(s, klass, line_opacity=0)
    s.save()


def lands_between():
    s = Style("lands_between.json")
    # Engraved blue-grey sea, painted brown and olive relief, golden thread roads with a glow.
    land(s, **{"landcover-woodland": "#6b7a55", "landcover-open": "#8a8a5e", "landcover-wetland": "#6e8068",
               "landcover-bare": "#a89572", "land-park": "#6b7a55", "land-residential": "#b0a07a",
               "land-urban": "#a69870", "land-amenities": "#9e9370"})
    hillshade(s, "#4e4430", "#efe3bc", "#6a5a3a", 0.8)
    s.paint("water", fill_color="#75898a", fill_outline_color="#4a6264")
    s.pattern_over("water", "eb-sea-waves", "eb-waves", 1.0)
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#f2c95a")
        casing(s, klass, line_opacity=0)
    s.glow_under_roads("#ffd36a", 3.2, 0.32, 2.2)
    s.save()


if __name__ == "__main__":
    for fn in (heat, rivia, ctos, skyrim, lands_between):
        fn()
