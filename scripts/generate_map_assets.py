#!/usr/bin/env python3
"""Generate original Waypoint map textures and POI icon atlases.

The renderer uses only Python's standard library. It draws abstract map
materials and original semantic symbols; no game screenshots, game sprites,
third-party icon sheets or preset map themes are inputs.
"""

from __future__ import annotations

import binascii
import json
import math
import os
import random
import struct
import zlib
from pathlib import Path

from map_glyphs import draw_category_glyph
from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_ROOT = Path(os.environ.get("WAYPOINT_ASSET_OUTPUT", ROOT)).resolve()
MATERIAL_DIR = OUTPUT_ROOT / "app/src/main/assets/map-materials"
POI_DIR = OUTPUT_ROOT / "app/src/main/assets/poi"
MARKER_DIR = OUTPUT_ROOT / "app/src/main/assets/map-markers"
CATEGORY_REGISTRY = ROOT / "app/src/main/assets/poi_categories.json"
KINDS = tuple(item["id"] for item in json.loads(CATEGORY_REGISTRY.read_text()))
SIZE = 128
ATLAS_COLUMNS = 5

# Each game-inspired world gets a separate original symbol grammar. The profile
# controls the marker silhouette and the ornament surrounding the live-POI glyph.
PROFILE_STYLES = {
    "blip_arc": ("roundel", "target"), "trail_stamp": ("roundel", "rune"),
    "neon_circuit": ("hex", "circuit"), "voxel_pin": ("pixel", "stair"),
    "phosphor_grid": ("square", "scanline"), "street_burst": ("roundel", "radial"),
    "race_gate": ("ticket", "check"), "medallion_ink": ("roundel", "engrave"),
    "sunset_orbit": ("circle", "orbit"), "metro_roundel": ("roundel", "tabs"),
    "wayfinder_dots": ("soft", "dots"), "slate_eye": ("diamond", "eye"),
    "north_compass": ("roundel", "compass"), "golden_grace": ("shield", "rays"),
    "rally_checkpoint": ("ticket", "chevrons"), "animus_sigil": ("shield", "sigil"),
    "field_compass": ("crosshair", "ticks"), "sumi_brush": ("roundel", "brush"),
    "chiral_node": ("hex", "chiral"), "machine_circuit": ("diamond", "circuit"),
    "landmark_diamond": ("diamond", "landmark"), "crest_star": ("shield", "star"),
    "survey_reticle": ("crosshair", "survey"), "heraldic_seal": ("shield", "heraldic"),
    "satellite_target": ("circle", "satellite"), "coastal_drop": ("shield", "wave"),
    "printed_roundel": ("roundel", "registration"), "shepherd_grid": ("hex", "grid"),
    "nautical_rose": ("roundel", "rose"), "amber_panel": ("ticket", "scanline"),
    "layered_orbit": ("hex", "orbit"), "bunker_crosshair": ("square", "bracket"),
    "vector_sweep": ("crosshair", "sweep"), "field_grid": ("ticket", "grid"),
    "quest_pixel": ("pixel", "check"), "machine_bracket": ("diamond", "bracket"),
    "pda_bracket": ("square", "survey"), "soft_tile": ("circle", "dots"),
}
assert len(PROFILE_STYLES) == 38 and len(set(PROFILE_STYLES.values())) == 38

# Original map-culture signatures. The semantic core still communicates live
# place categories, while this 38-way mark gives every world its own readable
# visual vocabulary instead of recolouring one generic sprite sheet.
PROFILE_SIGNATURES = {
    "blip_arc": "broadcast", "trail_stamp": "spur", "neon_circuit": "circuit",
    "voxel_pin": "voxel", "phosphor_grid": "phosphor", "street_burst": "burst",
    "race_gate": "race", "medallion_ink": "engraving", "sunset_orbit": "sunset",
    "metro_roundel": "metro", "wayfinder_dots": "wayfinder", "slate_eye": "slate",
    "north_compass": "north", "golden_grace": "grace", "rally_checkpoint": "checkpoint",
    "animus_sigil": "animus", "field_compass": "field_compass", "sumi_brush": "sumi",
    "chiral_node": "chiral", "machine_circuit": "machine", "landmark_diamond": "landmark",
    "crest_star": "crest", "survey_reticle": "survey", "heraldic_seal": "heraldry",
    "satellite_target": "satellite", "coastal_drop": "coast", "printed_roundel": "print",
    "shepherd_grid": "shepherd", "nautical_rose": "nautical", "amber_panel": "amber",
    "layered_orbit": "orbit", "bunker_crosshair": "bunker", "vector_sweep": "sweep",
    "field_grid": "coordinates", "quest_pixel": "quest", "machine_bracket": "android",
    "pda_bracket": "pda", "soft_tile": "soft",
}
assert len(PROFILE_SIGNATURES) == 38 and len(set(PROFILE_SIGNATURES.values())) == 38


def rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[index:index + 2], 16) for index in (0, 2, 4))


def rgba(value: str, alpha: int = 255) -> tuple[int, int, int, int]:
    return (*rgb(value), alpha)


def blend(a: str, b: str, amount: float) -> str:
    left, right = rgb(a), rgb(b)
    return "#" + "".join(f"{round(x * (1 - amount) + y * amount):02x}" for x, y in zip(left, right))


def luminance(value: str) -> float:
    def linear(channel: int) -> float:
        c = channel / 255
        return c / 12.92 if c <= .04045 else ((c + .055) / 1.055) ** 2.4
    r, g, b = (linear(v) for v in rgb(value))
    return .2126 * r + .7152 * g + .0722 * b


def contrast(a: str, b: str) -> float:
    high, low = sorted((luminance(a), luminance(b)), reverse=True)
    return (high + .05) / (low + .05)


def palette_for(theme: dict[str, str], night: bool) -> dict[str, str]:
    result = dict(theme)
    if not night or theme["icon_profile"] == "machine_bracket":
        # Machine Atlas keeps its warm paper treatment after sunset.
        return result
    for key, amount in (("ground", .60), ("land", .56), ("park", .50),
                        ("water", .63), ("building", .58), ("casing", .50)):
        result[key] = blend(theme[key], "#071018", amount)
    result["halo"] = result["ground"]
    result["label"] = "#F0F4EF"
    result["road"] = blend(theme["road"], "#D8EDF3", .28)
    result["outline"] = blend(theme["outline"], "#D8EDF3", .24)
    return result


class Raster:
    """Tiny dependency-free RGBA rasterizer for our authored atlas art."""
    def __init__(self, width: int, height: int, color=(0, 0, 0, 0)):
        self.width, self.height = width, height
        self.pixels = bytearray(color * (width * height))

    def pixel(self, x: int, y: int, color: tuple[int, int, int, int]) -> None:
        if not (0 <= x < self.width and 0 <= y < self.height):
            return
        index = (y * self.width + x) * 4
        alpha = color[3]
        if alpha == 255:
            self.pixels[index:index + 4] = bytes(color)
        elif alpha:
            old_alpha = self.pixels[index + 3]
            out_alpha = alpha + old_alpha * (255 - alpha) // 255
            if out_alpha == 0:
                self.pixels[index:index + 4] = b"\0\0\0\0"
                return
            for channel in range(3):
                old = self.pixels[index + channel]
                self.pixels[index + channel] = (color[channel] * alpha + old * old_alpha * (255 - alpha) // 255) // out_alpha
            self.pixels[index + 3] = out_alpha

    def rect(self, x0: int, y0: int, x1: int, y1: int, color, fill=None, width=1, radius=0) -> None:
        x0, y0, x1, y1 = map(int, (x0, y0, x1, y1))
        if fill:
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    if radius:
                        cx = min(max(x, x0 + radius), x1 - radius)
                        cy = min(max(y, y0 + radius), y1 - radius)
                        if (x - cx) ** 2 + (y - cy) ** 2 > radius ** 2:
                            continue
                    self.pixel(x, y, fill)
        if color:
            self.line(((x0, y0), (x1, y0), (x1, y1), (x0, y1), (x0, y0)), color, width)

    def line(self, points, color, width=1) -> None:
        points = [(int(round(x)), int(round(y))) for x, y in points]
        radius = max(0, int(width) - 1) // 2
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            dx, dy = abs(x1 - x0), abs(y1 - y0)
            sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
            error = dx - dy
            while True:
                if radius == 0:
                    self.pixel(x0, y0, color)
                else:
                    for oy in range(-radius, radius + 1):
                        for ox in range(-radius, radius + 1):
                            if ox * ox + oy * oy <= (radius + .5) ** 2:
                                self.pixel(x0 + ox, y0 + oy, color)
                if x0 == x1 and y0 == y1:
                    break
                e2 = 2 * error
                if e2 > -dy:
                    error -= dy
                    x0 += sx
                if e2 < dx:
                    error += dx
                    y0 += sy

    def ellipse(self, cx, cy, rx, ry, outline, fill=None, width=1) -> None:
        x0, x1 = max(0, int(cx - rx - width)), min(self.width - 1, int(cx + rx + width))
        y0, y1 = max(0, int(cy - ry - width)), min(self.height - 1, int(cy + ry + width))
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                d = ((x - cx) / max(1, rx)) ** 2 + ((y - cy) / max(1, ry)) ** 2
                if fill and d <= 1:
                    self.pixel(x, y, fill)
                elif outline and 1 <= d <= (1 + width / max(1, rx)) ** 2:
                    self.pixel(x, y, outline)

    def polygon(self, points, outline, fill=None, width=1) -> None:
        pts = [(int(round(x)), int(round(y))) for x, y in points]
        if fill:
            low, high = max(0, min(y for _, y in pts)), min(self.height - 1, max(y for _, y in pts))
            for y in range(low, high + 1):
                hits = []
                for (x1, y1), (x2, y2) in zip(pts, pts[1:] + pts[:1]):
                    if y1 == y2 or not (min(y1, y2) <= y < max(y1, y2)):
                        continue
                    hits.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
                hits.sort()
                for left, right in zip(hits[::2], hits[1::2]):
                    for x in range(max(0, math.ceil(left)), min(self.width, math.floor(right) + 1)):
                        self.pixel(x, y, fill)
        if outline:
            self.line(pts + [pts[0]], outline, width)

    def blit(self, other: "Raster", left: int, top: int) -> None:
        for y in range(other.height):
            for x in range(other.width):
                index = (y * other.width + x) * 4
                self.pixel(left + x, top + y, tuple(other.pixels[index:index + 4]))

    def save_png(self, path: Path) -> None:
        raw = b"".join(b"\0" + self.pixels[y * self.width * 4:(y + 1) * self.width * 4]
                        for y in range(self.height))
        def chunk(kind: bytes, payload: bytes) -> bytes:
            body = kind + payload
            return struct.pack(">I", len(payload)) + body + struct.pack(">I", binascii.crc32(body) & 0xFFFFFFFF)
        data = b"\x89PNG\r\n\x1a\n"
        data += chunk(b"IHDR", struct.pack(">2I5B", self.width, self.height, 8, 6, 0, 0, 0))
        data += chunk(b"IDAT", zlib.compress(raw, 8)) + chunk(b"IEND", b"")
        path.write_bytes(data)


def material_family(value: str) -> str:
    name = value.lower()
    if "pixel" in name or "16bit" in name:
        return "pixel"
    if "scan" in name or "phosphor" in name or "pda" in name:
        return "scan"
    if "tactical" in name or "field" in name or "radar" in name:
        return "tactical"
    if "survey" in name or "graticule" in name:
        return "survey"
    if "coast" in name or "nautical" in name:
        return "waves"
    if "topo" in name or "contour" in name or "relief" in name or "ripple" in name:
        return "contour"
    if "circuit" in name or "hologram" in name or "chiral" in name:
        return "circuit"
    if "race" in name or "motorsport" in name:
        return "racing"
    if "console" in name or "dots" in name:
        return "dots"
    if "grid" in name or "block" in name or "street" in name or "concrete" in name:
        return "grid"
    if "paper" in name or "vellum" in name or "atlas" in name or "rice" in name or "machine" in name:
        return "paper"
    return "engraving"


def draw_material(theme: dict[str, str], seed: int, night: bool) -> Raster:
    colors = palette_for(theme, night)
    ground, road = colors["ground"], colors["road"]
    image = Raster(SIZE, SIZE, rgba(ground))
    art = material_family(theme["material"])
    rng = random.Random(seed + (10000 if night else 0))
    low = rgba(blend(ground, road, .25), 24 if night else 18)
    edge = rgba(blend(ground, road, .40), 42 if night else 34)
    accent = rgba(colors["outline"], 30 if night else 24)

    if art == "pixel":
        for y in range(0, SIZE, 8):
            for x in range(0, SIZE, 8):
                if ((x // 8 + y // 8 + seed) % 3) == 0:
                    image.rect(x, y, x + 7, y + 7, None, low)
        for p in range(0, SIZE, 32):
            image.line(((p, 0), (p, SIZE - 1)), edge)
            image.line(((0, p), (SIZE - 1, p)), edge)
    elif art in ("grid", "circuit", "racing"):
        step = 32 if art == "racing" else 16
        for p in range(0, SIZE, step):
            image.line(((p, 0), (p, SIZE - 1)), low)
            image.line(((0, p), (SIZE - 1, p)), low)
        for y in range(step // 2, SIZE, step):
            for x in range(step // 2, SIZE, step):
                image.ellipse(x, y, 1 if art == "grid" else 2, 1 if art == "grid" else 2, edge, edge)
        if art == "circuit":
            for y in range(12, SIZE, 32):
                x = (16 + y + seed * 3) % 64 + 32
                image.line(((0, y), (x, y), (x, y + 8), (SIZE - 1, y + 8)), edge)
                image.ellipse(x, y + 8, 2, 2, edge, edge)
        elif art == "racing":
            for y in range(0, SIZE, 8):
                for x in range(0, SIZE, 8):
                    if (x // 8 + y // 8) % 2 == 0:
                        image.rect(x, y, x + 7, y + 7, None, rgba(colors["road"], 9))
    elif art == "scan":
        for y in range(2, SIZE, 5):
            image.line(((0, y), (SIZE - 1, y)), low)
        for x in range(8, SIZE, 32):
            image.line(((x, 0), (x, SIZE - 1)), rgba(road, 15))
        for y in range(16, SIZE, 32):
            image.line(((2, y), (8, y)), edge, 2)
    elif art == "tactical":
        for p in range(16, SIZE, 32):
            image.line(((p, 0), (p, SIZE - 1)), low)
            image.line(((0, p), (SIZE - 1, p)), low)
        for cx, cy in ((32, 32), (96, 96)):
            for radius in (8, 16, 24):
                image.ellipse(cx, cy, radius, radius, edge)
            image.line(((cx - 28, cy), (cx + 28, cy)), edge)
            image.line(((cx, cy - 28), (cx, cy + 28)), edge)
    elif art == "survey":
        for p in range(8, SIZE, 16):
            image.line(((p, 0), (p, SIZE - 1)), low)
            image.line(((0, p), (SIZE - 1, p)), low)
            image.line(((p, 0), (p, 5)), edge)
            image.line(((0, p), (5, p)), edge)
    elif art == "waves":
        for row in range(8, SIZE, 16):
            phase = (row + seed % 17) * math.pi / 64
            points = [(x, row + math.sin(x / SIZE * math.tau + phase) * 3) for x in range(SIZE + 1)]
            image.line(points, low)
        for x in range(16, SIZE, 32):
            image.ellipse(x, 62, 1, 1, edge, edge)
    elif art == "contour":
        for row in range(8, SIZE, 16):
            phase = (row + seed % 31) * math.pi / 48
            points = [(x, row + math.sin(x / SIZE * math.tau + phase) * 5 + math.sin(x / SIZE * math.tau * 2) * 2)
                      for x in range(SIZE + 1)]
            image.line(points, low)
        for x in range(22, SIZE, 42):
            for y in (40, 64, 88):
                image.ellipse(x, y, 7, 13, edge)
    elif art == "dots":
        for y in range(6, SIZE, 12):
            for x in range(6, SIZE, 12):
                image.ellipse(x, y, 1, 1, low, low)
        for x in range(16, SIZE, 32):
            image.line(((x, 0), (x, SIZE - 1)), rgba(road, 12))
    else:
        for _ in range(240):
            x, y = rng.randrange(SIZE), rng.randrange(SIZE)
            length = rng.randrange(2, 10)
            tint = low if rng.random() < .72 else edge
            image.line(((x, y), (x + length, y + rng.choice((-1, 0, 1)))), tint)
        for x in range(8, SIZE, 32):
            image.line(((x, 0), (x, 4)), edge)
            image.line(((x, SIZE - 5), (x, SIZE - 1)), edge)

    if art in ("circuit", "racing", "tactical"):
        for x in range(16 + seed % 17, SIZE, 48):
            image.ellipse(x, 18, 1, 1, accent, accent)
    return image


def draw_glyph(image: Raster, kind: str, ink, accent, family: str, profile: str, profile_index: int) -> None:
    if kind == "destination":
        cx = cy = 64
        stroke = 5 if family in {"pixel", "terminal", "pda"} else 4
        def line(*points, width=stroke):
            image.line(points, ink, width)
        if family in {"pixel", "terminal", "pda", "machine"}:
            image.rect(48, 48, 80, 80, ink, None, stroke)
            line((64, 31), (64, 47));line((64, 81), (64, 97))
            line((31, 64), (47, 64));line((81, 64), (97, 64))
            image.rect(59, 59, 69, 69, accent, accent)
        else:
            image.ellipse(cx, cy, 21, 21, ink, None, stroke)
            image.ellipse(cx, cy, 7, 7, accent, accent)
            line((64, 25), (64, 37));line((64, 91), (64, 103))
            line((25, 64), (37, 64));line((91, 64), (103, 64))
        draw_profile_signature(image, profile, accent, profile_index, 2.1, 64)
        return
    draw_category_glyph(image, kind, ink, accent, family)
    draw_profile_signature(image, profile, accent, profile_index, 0.72, 27)


def draw_ornament(image: Raster, ornament: str, accent, profile_index: int) -> None:
    faint = (accent[0], accent[1], accent[2], 150)
    bright = (accent[0], accent[1], accent[2], 220)
    if ornament in {"target", "ticks", "compass", "rays", "rose", "sweep"}:
        count = {"target": 4, "ticks": 8, "compass": 4, "rays": 8, "rose": 8, "sweep": 6}[ornament]
        for i in range(count):
            angle = -math.pi / 2 + i * math.tau / count
            start, end = ((44, 55) if i % 2 == 0 else (47, 52)) if ornament == "rose" else (48, 55)
            image.line(((64 + math.cos(angle) * start, 64 + math.sin(angle) * start),
                        (64 + math.cos(angle) * end, 64 + math.sin(angle) * end)), faint, 2)
        if ornament in {"compass", "rose"}:
            image.polygon(((64, 19), (68, 32), (64, 28), (60, 32)), bright, bright)
    elif ornament in {"circuit", "chiral", "satellite"}:
        for y, direction in ((35, 1), (93, -1)):
            x = 19 if direction == 1 else 109
            elbow = 34 if direction == 1 else 94
            image.line(((x, y), (elbow, y), (elbow, y + direction * 9),
                        (elbow + direction * 10, y + direction * 9)), faint, 2)
            image.ellipse(elbow + direction * 10, y + direction * 9, 2, 2, bright, bright)
        if ornament == "chiral":
            image.line(((20, 64), (34, 64), (43, 55)), faint, 2)
            image.line(((108, 64), (94, 64), (85, 73)), faint, 2)
        elif ornament == "satellite":
            image.ellipse(64, 64, 44, 44, faint, None, 1)
            image.ellipse(64, 20, 3, 3, bright, bright)
    elif ornament in {"stair", "scan", "scanline", "grid", "bracket", "survey"}:
        if ornament in {"scan", "scanline"}:
            for y in (22, 28, 100, 106):
                image.line(((38, y), (90, y)), faint, 1)
        if ornament == "grid":
            for x in (26, 34, 94, 102):
                image.line(((x, 39), (x, 47)), faint, 2)
                image.line(((x, 81), (x, 89)), faint, 2)
        if ornament == "bracket" or ornament == "survey":
            for x, y, dx, dy in ((20, 32, 10, 0), (20, 32, 0, 10), (108, 32, -10, 0),
                                 (108, 32, 0, 10), (20, 96, 10, 0), (20, 96, 0, -10),
                                 (108, 96, -10, 0), (108, 96, 0, -10)):
                image.line(((x, y), (x + dx, y + dy)), bright, 2)
        if ornament in {"stair", "scanline"}:
            for x, y, direction in ((20, 24, 1), (108, 24, -1), (20, 104, 1), (108, 104, -1)):
                image.line(((x, y), (x + direction * 7, y),
                            (x + direction * 7, y + (7 if y < 64 else -7))), bright, 3)
    elif ornament in {"orbit", "dots", "tabs", "registration"}:
        if ornament == "orbit":
            image.ellipse(64, 64, 44, 38, faint, None, 2)
            image.ellipse(26, 50, 3, 3, bright, bright)
            image.ellipse(101, 78, 3, 3, bright, bright)
        elif ornament == "dots":
            for x, y in ((29, 35), (99, 35), (29, 93), (99, 93)):
                image.ellipse(x, y, 2, 2, bright, bright)
        elif ornament == "tabs":
            image.rect(46, 17, 61, 22, None, bright)
            image.rect(67, 106, 82, 111, None, bright)
        else:
            for x, y in ((25, 26), (103, 26), (25, 102), (103, 102)):
                image.line(((x - 4, y), (x + 4, y)), bright, 2)
                image.line(((x, y - 4), (x, y + 4)), bright, 2)
    elif ornament in {"rune", "engrave", "sigil", "heraldic", "star", "landmark"}:
        if ornament == "rune":
            for x, direction in ((25, 1), (103, -1)):
                image.line(((x, 35), (x + direction * 8, 43), (x, 51)), faint, 2)
                image.line(((x, 77), (x + direction * 8, 85), (x, 93)), faint, 2)
        elif ornament == "engrave":
            for y in (25, 103):
                for x in range(30, 99, 8):
                    image.line(((x, y), (x + 5, y + (5 if y == 25 else -5))), faint, 1)
        elif ornament in {"sigil", "heraldic"}:
            image.polygon(((64, 11), (71, 25), (64, 21), (57, 25)), bright, None, 2)
            image.line(((64, 103), (64, 114)), faint, 2)
        elif ornament == "star":
            points = []
            for i in range(10):
                angle = -math.pi / 2 + i * math.pi / 5
                radius = 8 if i % 2 == 0 else 4
                points.append((64 + math.cos(angle) * radius, 18 + math.sin(angle) * radius))
            image.polygon(points, bright, None, 2)
        else:
            for x, y in ((26, 26), (102, 26), (26, 102), (102, 102)):
                image.polygon(((x, y - 5), (x + 5, y), (x, y + 5), (x - 5, y)), bright, None, 2)
    elif ornament in {"check", "chevrons", "radial", "brush", "eye", "wave"}:
        if ornament == "check":
            for x, direction in ((24, 1), (104, -1)):
                for y in (36, 92):
                    image.line(((x, y - 5), (x + direction * 5, y), (x, y + 5)), bright, 2)
        elif ornament == "chevrons":
            for x, direction in ((17, 1), (111, -1)):
                image.line(((x, 46), (x + direction * 6, 52), (x, 58)), bright, 2)
                image.line(((x, 70), (x + direction * 6, 76), (x, 82)), bright, 2)
        elif ornament == "radial":
            for angle in range(0, 360, 45):
                rad = math.radians(angle)
                image.line(((64 + math.cos(rad) * 48, 64 + math.sin(rad) * 48),
                            (64 + math.cos(rad) * 56, 64 + math.sin(rad) * 56)), faint, 2)
        elif ornament == "brush":
            image.line(((18, 28), (24, 20), (37, 17)), bright, 3)
            image.line(((92, 109), (105, 104), (110, 96)), faint, 3)
        elif ornament == "eye":
            image.polygon(((42, 30), (64, 20), (86, 30), (64, 38)), bright, None, 2)
            image.ellipse(64, 29, 3, 3, bright, bright)
        else:
            points = [(x, 104 + math.sin(x / 12 + profile_index) * 3) for x in range(30, 100, 2)]
            image.line(points, faint, 2)


def draw_world_signature(image: Raster, profile: str, accent, profile_index: int) -> None:
    """Stamp an original world-specific HUD glyph above each semantic POI mark."""
    mark = PROFILE_SIGNATURES[profile]
    ink = (accent[0], accent[1], accent[2], 235)
    ghost = (accent[0], accent[1], accent[2], 150)
    if mark == "broadcast":
        for radius in (5, 10, 15):
            image.line(((64-radius, 31-radius//3), (64, 25-radius//3), (64+radius, 31-radius//3)), ink, 2)
        image.ellipse(64, 33, 2, 2, ink, ink)
    elif mark == "spur":
        image.line(((49, 25), (57, 32), (64, 24), (71, 32), (79, 25)), ink, 2)
        image.line(((54, 34), (74, 34)), ghost, 2)
    elif mark == "circuit":
        image.line(((48, 26), (57, 26), (57, 33), (70, 33), (70, 25), (80, 25)), ink, 2)
        image.ellipse(47, 26, 2, 2, ink, ink);image.ellipse(81, 25, 2, 2, ink, ink)
    elif mark == "voxel":
        for x, y, width in ((50, 29, 7), (57, 25, 7), (64, 21, 7), (71, 25, 7)):
            image.rect(x, y, x+width, y+4, ink, ink)
    elif mark == "phosphor":
        for y, end in ((22, 77), (27, 69), (32, 82)):
            image.line(((48, y), (end, y)), ink, 2)
        image.rect(72, 20, 78, 24, ghost, ghost)
    elif mark == "burst":
        for angle in range(0, 360, 45):
            rad=math.radians(angle)
            image.line(((64+math.cos(rad)*7,28+math.sin(rad)*7),(64+math.cos(rad)*15,28+math.sin(rad)*15)),ink,2)
        image.ellipse(64, 28, 2, 2, ink, ink)
    elif mark == "race":
        image.line(((48, 33), (58, 33), (64, 27), (72, 27), (80, 21)), ink, 2)
        for y in (23, 28, 33): image.line(((49, y), (54, y)), ghost, 2)
    elif mark == "engraving":
        image.ellipse(64, 28, 11, 8, ink, None, 1)
        for x in (59, 64, 69): image.line(((x, 23), (x-3, 30)), ghost, 1)
    elif mark == "sunset":
        image.line(((49, 31), (79, 31)), ink, 2)
        image.line(((54, 28), (57, 25), (61, 23), (67, 23), (71, 25), (74, 28)), ink, 2)
        image.line(((58, 34), (70, 34)), ghost, 2)
    elif mark == "metro":
        image.line(((49, 28), (79, 28)), ink, 2)
        for x in (51, 64, 77): image.ellipse(x, 28, 3, 3, ink, ink)
        image.line(((64, 19), (64, 37)), ghost, 1)
    elif mark == "wayfinder":
        points=((49,33),(56,25),(64,31),(71,23),(79,30))
        image.line(points, ghost, 2)
        for x,y in points: image.ellipse(x,y,2,2,ink,ink)
    elif mark == "slate":
        image.polygon(((48,28),(56,23),(64,21),(72,23),(80,28),(72,33),(64,35),(56,33)),ink,None,2)
        image.ellipse(64, 28, 2, 3, ink, ink)
    elif mark == "north":
        image.polygon(((64,19),(70,33),(64,29),(58,33)),ink,ink,1)
        image.line(((64,29),(64,37)),ghost,2)
    elif mark == "grace":
        image.ellipse(64, 29, 8, 7, ink, None, 2)
        for x in (49,56,72,79): image.line(((x,29),(64+(x-64)//2,20)),ghost,1)
    elif mark == "checkpoint":
        image.line(((54,20),(54,36)),ink,2)
        image.polygon(((55,21),(76,21),(70,27),(76,32),(55,32)),ink,None,2)
        image.line(((49,36),(79,36)),ghost,2)
    elif mark == "animus":
        image.line(((49,32),(57,24),(64,29),(72,21),(79,27)),ink,2)
        image.line(((55,36),(64,31),(73,35)),ghost,1)
    elif mark == "field_compass":
        image.ellipse(64, 28, 10, 8, ghost, None, 1)
        image.line(((64,18),(64,38)),ink,2);image.line(((52,28),(76,28)),ink,2)
        image.ellipse(64,28,2,2,ink,ink)
    elif mark == "sumi":
        image.line(((49,32),(56,29),(63,26),(71,24),(79,22)),ink,3)
        image.line(((52,36),(60,34),(70,31)),ghost,1)
    elif mark == "chiral":
        image.line(((50,23),(60,23),(64,28),(68,33),(78,33)),ink,2)
        image.line(((50,33),(60,33),(64,28),(68,23),(78,23)),ghost,2)
        image.ellipse(64,28,3,3,ink,ink)
    elif mark == "machine":
        image.polygon(((54,20),(72,20),(80,28),(72,36),(54,36),(48,28)),ink,None,2)
        image.line(((55,28),(73,28)),ghost,1)
    elif mark == "landmark":
        image.rect(50,27,55,36,ink,ink);image.rect(59,21,66,36,ink,ink);image.rect(70,25,78,36,ink,ink)
        image.line(((48,37),(80,37)),ghost,1)
    elif mark == "crest":
        image.polygon(((64,20),(67,26),(74,26),(69,30),(71,36),(64,32),(57,36),(59,30),(54,26),(61,26)),ink,None,2)
    elif mark == "survey":
        for x,y,dx,dy in ((49,22,7,0),(49,22,0,7),(79,22,-7,0),(79,22,0,7),(49,35,7,0),(49,35,0,-7),(79,35,-7,0),(79,35,0,-7)):
            image.line(((x,y),(x+dx,y+dy)),ink,2)
    elif mark == "heraldry":
        image.line(((51,26),(56,21),(60,27),(64,20),(68,27),(72,21),(77,26)),ink,2)
        image.polygon(((52,29),(76,29),(72,36),(64,39),(56,36)),ghost,None,1)
    elif mark == "satellite":
        image.ellipse(64,28,14,7,ghost,None,1)
        image.line(((57,22),(71,34)),ink,2);image.rect(52,19,58,24,ink,ink);image.rect(71,32,77,37,ink,ink)
    elif mark == "coast":
        image.line(((48,25),(54,22),(60,25),(66,28),(72,25),(78,22)),ink,2)
        image.line(((48,32),(54,29),(60,32),(66,35),(72,32),(78,29)),ghost,2)
    elif mark == "print":
        for x,y in ((52,22),(76,22),(52,34),(76,34)):
            image.line(((x-3,y),(x+3,y)),ink,1);image.line(((x,y-3),(x,y+3)),ink,1)
    elif mark == "shepherd":
        for y,amplitude in ((23,3),(28,5),(33,3)):
            pts=[(x,y+round(math.sin((x+profile_index)/5)*amplitude)) for x in range(50,79,3)]
            image.line(pts,ink if amplitude==5 else ghost,1)
    elif mark == "nautical":
        image.polygon(((64,18),(68,28),(64,25),(60,28)),ink,ink,1)
        image.polygon(((64,38),(68,28),(64,31),(60,28)),ghost,ghost,1)
        image.line(((51,28),(77,28)),ink,1)
    elif mark == "amber":
        image.line(((49,21),(57,21),(57,36),(49,36)),ink,2)
        image.line(((79,21),(71,21),(71,36),(79,36)),ghost,2)
        image.line(((60,25),(68,25)),ink,1);image.line(((60,31),(68,31)),ink,1)
    elif mark == "orbit":
        image.ellipse(64,28,15,7,ghost,None,1)
        image.ellipse(64,28,7,15,ink,None,1)
        image.ellipse(78,27,2,2,ink,ink)
    elif mark == "bunker":
        image.rect(54,20,74,36,ghost,None,2)
        image.line(((64,18),(64,38)),ink,2);image.line(((50,28),(78,28)),ink,2)
        image.ellipse(64,28,2,2,ink,ink)
    elif mark == "sweep":
        image.line(((49,35),(56,28),(64,23),(72,28),(79,35)),ink,2)
        image.line(((64,28),(77,21)),ghost,1);image.ellipse(64,28,2,2,ink,ink)
    elif mark == "coordinates":
        for x in (53,64,75): image.line(((x,20),(x,36)),ghost,1)
        for y in (22,28,34): image.line(((49,y),(79,y)),ghost,1)
        image.ellipse(64,28,3,3,ink,ink)
    elif mark == "quest":
        image.rect(50,29,56,35,ink,ink);image.rect(56,24,62,30,ink,ink)
        image.rect(62,19,68,25,ink,ink);image.rect(68,24,74,30,ink,ink);image.rect(74,29,80,35,ink,ink)
    elif mark == "android":
        image.line(((50,22),(58,22),(58,27),(54,27),(54,33),(60,33)),ink,2)
        image.line(((78,22),(70,22),(70,27),(74,27),(74,33),(68,33)),ghost,2)
        image.ellipse(64,28,2,2,ink,ink)
    elif mark == "pda":
        image.rect(53,19,75,37,ghost,None,2)
        image.line(((57,23),(71,23)),ink,2);image.line(((57,28),(68,28)),ink,1)
        image.rect(69,31,72,34,ink,ink)
    elif mark == "soft":
        image.rect(53,20,75,36,ghost,None,2,radius=4)
        for x,y in ((58,25),(70,25),(58,32),(70,32)): image.ellipse(x,y,2,2,ink,ink)


def draw_profile_signature(image: Raster, profile: str, accent, profile_index: int,
                           scale: float = 1.5, target_center_y: float = 28) -> None:
    """Enlarge the authored world emblem so each icon carries a visible identity."""
    stamp = Raster(SIZE, SIZE)
    draw_world_signature(stamp, profile, accent, profile_index)
    for y in range(12, 45):
        for x in range(40, 89):
            offset = (y * SIZE + x) * 4
            alpha = stamp.pixels[offset + 3]
            if not alpha:
                continue
            target_x = round(64 + (x - 64) * scale)
            target_y = round(target_center_y + (y - 28) * scale)
            if 0 <= target_x < SIZE and 0 <= target_y < SIZE:
                image.pixel(target_x, target_y, tuple(stamp.pixels[offset:offset + 4]))


def draw_machine_badge(kind: str, night: bool) -> Raster:
    """Small ink diamonds and monochrome pictograms from the original machine map."""
    art = Raster(SIZE, SIZE)
    charcoal = rgba("#35362E")
    rust = rgba("#844B37" if night else "#793D2B")
    cream = rgba("#F2ECD8" if night else "#EAE5CF")
    art.polygon(((64, 10), (118, 64), (64, 118), (10, 64)), charcoal, charcoal, 3)
    art.polygon(((64, 14), (114, 64), (64, 114), (14, 64)), rust, None, 3)
    if kind == "destination":
        art.rect(54, 54, 74, 74, cream, None, 3)
        for start, end in (((64, 35), (64, 51)), ((64, 77), (64, 93)),
                           ((35, 64), (51, 64)), ((77, 64), (93, 64))):
            art.line((start, end), cream, 4)
        art.rect(61, 61, 67, 67, rust, rust)
    else:
        draw_category_glyph(art, kind, cream, charcoal, "machine")
    return art


def draw_badge(kind: str, theme: dict[str, str], world: str, index: int, night: bool) -> Raster:
    if world == "nier":
        return draw_machine_badge(kind, night)
    colors = palette_for(theme, night)
    background = colors["ground"]
    surface = blend(background, "#101820" if luminance(background) < .32 else "#F4F0E4", .32)
    accent = colors["outline"]
    ink = colors["label"]
    if contrast(ink, surface) < 4.5:
        ink = "#111820" if luminance(surface) > .5 else "#F4F5EF"
    family = theme["marker"].lower()
    profile = theme["icon_profile"]
    shape, ornament = PROFILE_STYLES[profile]
    profile_index = tuple(THEMES).index(world)
    art = Raster(SIZE, SIZE)
    surface_rgba, accent_rgba = rgba(surface, 244), rgba(accent)
    if shape == "diamond":
        points = ((64, 5), (123, 64), (64, 123), (5, 64))
        art.polygon(points, accent_rgba, surface_rgba, 3)
        art.line(((64, 15), (64, 113)), rgba(accent, 88))
        art.line(((15, 64), (113, 64)), rgba(accent, 88))
    elif shape == "roundel":
        art.ellipse(64, 64, 55, 55, accent_rgba, surface_rgba, 4)
        art.ellipse(64, 64, 47, 47, rgba(accent, 115), None, 2)
        if family in {"fantasy", "ornate", "nautical", "engraved", "western"}:
            for angle in range(0, 360, 45):
                rad = math.radians(angle)
                art.line(((64 + math.cos(rad) * 47, 64 + math.sin(rad) * 47),
                          (64 + math.cos(rad) * 52, 64 + math.sin(rad) * 52)), accent_rgba, 2)
    elif shape == "circle":
        art.ellipse(64, 64, 56, 56, accent_rgba, surface_rgba, 4)
        art.ellipse(64, 64, 42, 42, rgba(accent, 120), None, 2)
        art.line(((64, 7), (64, 18)), accent_rgba, 3)
    elif shape == "hex":
        points = ((34, 8), (94, 8), (120, 34), (120, 94), (94, 120), (34, 120), (8, 94), (8, 34))
        art.polygon(points, accent_rgba, surface_rgba, 4)
        art.polygon(((36, 17), (92, 17), (111, 36), (111, 92), (92, 111), (36, 111), (17, 92), (17, 36)), rgba(accent, 130), None, 2)
    elif shape == "crosshair":
        art.ellipse(64, 64, 48, 48, accent_rgba, surface_rgba, 4)
        for start, end in ((0, 10), (118, 127)):
            art.line(((64, start), (64, end)), accent_rgba, 3)
            art.line(((start, 64), (end, 64)), accent_rgba, 3)
    elif shape == "ticket":
        art.rect(8, 18, 120, 110, accent_rgba, surface_rgba, 4, 8)
        for y in range(27, 104, 12):
            art.line(((14, y), (19, y)), rgba(accent, 130))
    elif shape == "soft":
        art.rect(9, 9, 119, 119, accent_rgba, surface_rgba, 4, 18)
    elif shape == "pixel":
        art.rect(12, 12, 116, 116, accent_rgba, surface_rgba, 4)
        for x, y in ((12, 12), (108, 12), (12, 108), (108, 108)):
            art.rect(x, y, x + 8, y + 8, accent_rgba, accent_rgba)
    elif shape == "shield":
        art.polygon(((22, 12), (106, 12), (110, 69), (98, 96), (64, 118), (30, 96), (18, 69)), accent_rgba, surface_rgba, 4)
        art.polygon(((29, 20), (99, 20), (102, 67), (92, 90), (64, 109), (36, 90), (26, 67)), rgba(accent, 115), None, 2)
    else:
        art.rect(10, 10, 118, 118, accent_rgba, surface_rgba, 4)
        art.rect(19, 19, 109, 109, rgba(accent, 105), None, 2)
    if shape in {"hex", "diamond", "crosshair"}:
        for x, y, dx, dy in ((20, 36, 7, -7), (108, 36, -7, -7),
                             (20, 92, 7, 7), (108, 92, -7, 7)):
            art.line(((x, y), (x + dx, y + dy)), accent_rgba, 2)
    draw_ornament(art, ornament, accent_rgba, profile_index + index)
    draw_glyph(art, kind, rgba(ink), accent_rgba, family, profile, profile_index)
    return art


def build() -> None:
    if len(THEMES) != 38:
        raise ValueError(f"Expected 38 theme recipes, found {len(THEMES)}")
    if set(PROFILE_STYLES) != {theme["icon_profile"] for theme in THEMES.values()}:
        raise ValueError("Every world needs exactly one authored icon profile")
    MATERIAL_DIR.mkdir(parents=True, exist_ok=True)
    POI_DIR.mkdir(parents=True, exist_ok=True)
    MARKER_DIR.mkdir(parents=True, exist_ok=True)
    for name, theme in THEMES.items():
        seed = sum((index + 1) * ord(char) for index, char in enumerate(name))
        for night, suffix in ((False, "day"), (True, "night")):
            draw_material(theme, seed, night).save_png(MATERIAL_DIR / f"{name}_{suffix}.png")
        for night, suffix in ((False, ""), (True, "_night")):
            atlas_path = POI_DIR / f"{name}{suffix}.png"
            atlas_size = (SIZE * ATLAS_COLUMNS, SIZE * math.ceil(len(KINDS) / ATLAS_COLUMNS))
            existing_size = None
            if atlas_path.is_file():
                with atlas_path.open("rb") as existing:
                    header = existing.read(24)
                if len(header) == 24 and header[:8] == b"\x89PNG\r\n\x1a\n":
                    existing_size = struct.unpack(">II", header[16:24])
            force_procedural = name == "nier" or os.environ.get("WAYPOINT_FORCE_PROCEDURAL_POI") == "1"
            if existing_size != atlas_size or force_procedural:
                atlas = Raster(*atlas_size)
                for cell, kind in enumerate(KINDS):
                    atlas.blit(draw_badge(kind, theme, name, cell, night),
                               cell % ATLAS_COLUMNS * SIZE, cell // ATLAS_COLUMNS * SIZE)
                atlas.save_png(atlas_path)
            marker_path = MARKER_DIR / f"{name}{suffix}.png"
            marker_size = None
            if marker_path.is_file():
                with marker_path.open("rb") as existing:
                    header = existing.read(24)
                if len(header) == 24 and header[:8] == b"\x89PNG\r\n\x1a\n":
                    marker_size = struct.unpack(">II", header[16:24])
            if marker_size != (SIZE, SIZE) or force_procedural:
                draw_badge("destination", theme, name, len(KINDS), night).save_png(marker_path)
    print(f"Generated {len(THEMES) * 2} original day/night map materials, {len(THEMES) * 2} per-world POI atlases, and {len(THEMES) * 2} themed destination markers")


if __name__ == "__main__":
    build()
