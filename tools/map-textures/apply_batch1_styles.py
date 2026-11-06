"""Batch one restyles (day styles): San Andreas, Vice Coast, Liberty City, Night City, Green Terminal.

Run:  python3 -I tools/map-textures/apply_batch1_styles.py
Each world is restyled from its reference spec in docs/map-texture-specs/phase1-reference-analysis.md.
The script edits paint values of existing layers and adds a few layers; the added layers are
skipped if they already exist, so it is safe to re-run on an already restyled file.
"""
import copy
import json
import os

STYLES = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "styles")
ROADS = ("primary", "secondary", "local")


def scale_width(expr, factor):
    out = copy.deepcopy(expr)
    for i in range(4, len(out), 2):
        out[i] = round(out[i] * factor, 3)
    return out


class Style:
    def __init__(self, name):
        self.path = os.path.join(STYLES, name)
        self.data = json.load(open(self.path))
        self.name = name

    @property
    def layers(self):
        return self.data["layers"]

    def get(self, short):
        for layer in self.layers:
            if layer["id"] == f"waypoint-{short}" or layer["id"] == short:
                return layer
        raise KeyError(short)

    def has(self, lid):
        return any(l["id"] == lid for l in self.layers)

    def paint(self, short, **kv):
        self.get(short)["paint"].update({k.replace("_", "-"): v for k, v in kv.items()})

    def road_widths(self, factor, classes=ROADS, casing=True):
        for klass in classes:
            road = self.get(f"{klass}-roads")["paint"]
            road["line-width"] = scale_width(road["line-width"], factor)
            if casing and self.has(f"waypoint-{klass}-casing"):
                cas = self.get(f"{klass}-casing")["paint"]
                cas["line-width"] = scale_width(cas["line-width"], factor)

    def insert_after(self, anchor_short, layer):
        if self.has(layer["id"]):
            return
        anchor = self.get(anchor_short)
        idx = self.layers.index(anchor) + 1
        self.layers.insert(idx, layer)

    def pattern_over(self, anchor_short, new_id, image, opacity=1.0, minzoom=None):
        anchor = self.get(anchor_short)
        layer = {
            "id": new_id,
            "type": "fill",
            "source": anchor["source"],
            "source-layer": anchor["source-layer"],
            "paint": {"fill-pattern": image, "fill-opacity": opacity},
        }
        if minzoom is not None:
            layer["minzoom"] = minzoom
        if "filter" in anchor:
            layer["filter"] = copy.deepcopy(anchor["filter"])
        self.insert_after(anchor_short, layer)

    def line_over(self, anchor_short, new_id, source_layer, **paint):
        anchor = self.get(anchor_short)
        self.insert_after(anchor_short, {
            "id": new_id,
            "type": "line",
            "source": anchor["source"],
            "source-layer": source_layer,
            "paint": {k.replace("_", "-"): v for k, v in paint.items()},
        })

    def glow_under_roads(self, colour, factor, opacity, blur):
        for klass in ROADS:
            road = self.get(f"{klass}-roads")
            glow = copy.deepcopy(road)
            glow["id"] = f"glow-{klass}"
            glow["paint"] = {
                "line-color": colour,
                "line-width": scale_width(road["paint"]["line-width"], factor),
                "line-opacity": opacity,
                "line-blur": blur,
            }
            if not self.has(glow["id"]):
                idx = self.layers.index(road)
                self.layers.insert(idx, glow)

    def save(self):
        with open(self.path, "w") as f:
            json.dump(self.data, f, separators=(",", ":"))
            f.write("\n")
        print("wrote", self.name, len(self.layers), "layers")


def san_andreas():
    s = Style("san_andreas.json")
    # Flat poster colours: slate sea, dark and meadow greens, tan desert, white city blocks.
    s.paint("water", fill_color="#6f8fb8", fill_outline_color="#6f8fb8")
    s.paint("landcover-woodland", fill_color="#2f6a2b", fill_opacity=1)
    s.paint("landcover-open", fill_color="#6f9a3a", fill_opacity=1)
    s.paint("landcover-wetland", fill_color="#4f8a45", fill_opacity=1)
    s.paint("landcover-bare", fill_color="#9b7f5a", fill_opacity=1)
    s.paint("land-park", fill_color="#4f8a35", fill_opacity=1)
    s.paint("land-residential", fill_color="#b9b9b9", fill_opacity=1)
    s.paint("land-urban", fill_color="#adadad", fill_opacity=1)
    s.paint("land-amenities", fill_color="#9aa88a", fill_opacity=1)
    s.paint("buildings", fill_color="#fafafa", fill_opacity=1, fill_outline_color="#d6d6d6")
    # Thick black roads with a grey shoulder.
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#0c0c0c")
        s.paint(f"{klass}-casing", line_color="#8e8e8e", line_opacity=1)
    s.road_widths(1.5)
    s.save()


def vice_coast():
    s = Style("vice_coast.json")
    # Flat pastel: sky-blue sea, pale city blocks, green shoreline strips, cream beach.
    s.paint("water", fill_color="#8ec5ff", fill_outline_color="#8ec5ff")
    s.paint("landcover-woodland", fill_color="#2f8f4e", fill_opacity=1)
    s.paint("landcover-open", fill_color="#3f9a5a", fill_opacity=1)
    s.paint("landcover-wetland", fill_color="#4aa070", fill_opacity=1)
    s.paint("landcover-bare", fill_color="#f2eec2", fill_opacity=1)
    s.paint("land-park", fill_color="#2f8f4e", fill_opacity=1)
    s.paint("land-residential", fill_color="#d2d2d2", fill_opacity=1)
    s.paint("land-urban", fill_color="#c4c4c4", fill_opacity=1)
    s.paint("land-amenities", fill_color="#b9c9bd", fill_opacity=1)
    s.paint("buildings", fill_color="#fbfbfb", fill_opacity=1, fill_outline_color="#e0e0e0")
    # Thick black roads with no casing.
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#0b0b0b")
        s.paint(f"{klass}-casing", line_opacity=0)
    s.road_widths(1.7, casing=False)
    # Soft shadow under the coast: a blurred, offset stroke along the water edge.
    s.line_over("water", "vc-coast-shadow", "water", line_color="#5f86b8", line_width=7,
                line_blur=4, line_opacity=0.55, line_translate=[3, 3])
    s.save()


def liberty():
    s = Style("liberty.json")
    # Black ground; roads drawn as light double lines around a dark core.
    s.paint("water", fill_color="#08090a", fill_outline_color="#08090a")
    for short, colour, opacity in (
        ("land-residential", "#0e0f10", 1), ("land-urban", "#101213", 1), ("land-amenities", "#111314", 1),
        ("landcover-woodland", "#121513", 1), ("landcover-open", "#111412", 1),
        ("landcover-wetland", "#0f1210", 1), ("landcover-bare", "#101111", 1), ("land-park", "#14181a", 1),
    ):
        s.paint(short, fill_color=colour, fill_opacity=opacity)
    s.paint("buildings", fill_color="#1a1c1d", fill_opacity=0.35, fill_outline_color="#2e3133")
    for klass in ROADS:
        s.paint(f"{klass}-casing", line_color="#cfd3d4", line_opacity=1)
        s.paint(f"{klass}-roads", line_color="#0a0b0c")
    s.road_widths(1.7)
    s.save()


def night_city():
    s = Style("night_city.json")
    s.paint("water", fill_color="#050a14", fill_outline_color="#050a14")
    s.paint("land-residential", fill_color="#0b1220", fill_opacity=1)
    s.paint("land-urban", fill_color="#1a1522", fill_opacity=1)
    # Red extruded masses, brighter top and a darker side from the vertical gradient.
    ext = s.get("building-extrusions")["paint"]
    ext["fill-extrusion-color"] = "#d1242f"
    ext["fill-extrusion-opacity"] = 0.95
    ext["fill-extrusion-vertical-gradient"] = True
    # Cyan roads with a glow underneath.
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#46e6ee")
        s.paint(f"{klass}-casing", line_opacity=0)
    s.glow_under_roads("#14b6c4", 3.4, 0.28, 2.0)
    # Holographic water grid.
    s.pattern_over("water", "nc-water-grid", "nc-grid", 1.0)
    s.save()


def green_terminal():
    s = Style("green_terminal.json")
    s.paint("water", fill_color="#020703", fill_outline_color="#2c6b25")
    s.paint("buildings", fill_color="#14381a", fill_opacity=0.6, fill_outline_color="#5ccf3f")
    for short in ("building-edges",):
        s.paint(short, line_color="#6bdc4a", line_opacity=0.9)
    for klass in ROADS:
        s.paint(f"{klass}-roads", line_color="#7bf24e")
    s.glow_under_roads("#4fbf30", 3.0, 0.2, 1.8)
    s.line_over("water", "gt-shore", "water", line_color="#4fbf30", line_width=1.2, line_opacity=0.8)
    for short in ("land-park", "landcover-open", "landcover-woodland"):
        s.pattern_over(short, f"gt-dots-{short.replace('landcover-', '').replace('land-', '')}", "gt-dots", 1.0)
    s.save()


if __name__ == "__main__":
    for fn in (san_andreas, vice_coast, liberty, night_city, green_terminal):
        fn()
