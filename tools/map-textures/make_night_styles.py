"""Derives each restyled world's night style (and night texture tiles) from its day style, so the
auto switch at 19:00 gives a dark variation of the same theme instead of the pre-restyle look.

Run:  python3 -B tools/map-textures/make_night_styles.py
Writes <world>_night.json for the worlds below and "<tile>-n.png" next to each pattern tile that
would vanish on a dark ground. Run it after the day styles, never before: it overwrites the night files.

Rules (per world): land and water fills darken to a shared target lightness (about 0.17), keeping hue;
dark ink lines (roads) lift to a warm light so they stay legible; road casings darken; text becomes light
with a dark halo; hillshade and satellite imagery dim; pattern tiles swap to their -n variants.
"""
import colorsys
import copy
import json
import os
import re
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets")
STYLES = os.path.join(ROOT, "styles")
TEXTURES = os.path.join(ROOT, "map-textures")

WORLDS = ["old_west", "los_santos", "pixel", "san_andreas", "vice_coast", "liberty", "night_city", "green_terminal",
          "heat", "rivia", "ctos", "skyrim", "lands_between", "hyrule", "hogwarts", "horizon", "far_cry", "bridges",
          "horizon_wilds", "assassin", "ghost", "spider"]

# (world folder, tile, new RGB or None to darken, alpha scale or darken factor)
LIGHT_INK = [
    ("old_west", "ow-trees", (0xC9, 0xBC, 0x92), 1.0), ("old_west", "ow-grass", (0xC9, 0xBC, 0x92), 1.0),
    ("old_west", "ow-ripple", (0xB8, 0xC4, 0xBE), 1.0), ("old_west", "ow-hatch", (0xB8, 0xA8, 0x84), 1.0),
    ("old_west", "ow-stipple", (0xB8, 0xA8, 0x84), 1.0),
    ("skyrim", "sk-peaks", (0xD2, 0xC8, 0xA6), 1.0), ("lands_between", "eb-waves", (0x8E, 0xB0, 0xB6), 1.0),
    ("rivia", "wt-wash", (0xB8, 0x9A, 0x60), 0.8), ("ghost", "gt-ink", (0xB9, 0xB3, 0xA4), 0.75),
]
DARKEN = ["px-grass", "px-leaves", "px-stone", "px-planks", "px-path", "px-sand", "px-water"]
PATTERN_KEYS = ("fill-pattern", "line-pattern", "fill-extrusion-pattern", "background-pattern")
HEX = re.compile(r"^#[0-9a-fA-F]{6}$")


def read_png(path):
    data = open(path, "rb").read()
    pos, idat, w, h = 8, b"", 0, 0
    while pos < len(data):
        n = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + n]
        if tag == b"IHDR":
            w, h = struct.unpack(">II", body[:8])
        elif tag == b"IDAT":
            idat += body
        pos += 12 + n
    raw = zlib.decompress(idat)
    stride = w * 4 + 1
    return w, h, [[tuple(raw[y * stride + 1 + x * 4:y * stride + 5 + x * 4]) for x in range(w)] for y in range(h)]


def write_png(path, w, h, rows):
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)

    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    open(path, "wb").write(png)


def night_tiles():
    for world, tile, rgb, k in LIGHT_INK:
        w, h, rows = read_png(os.path.join(TEXTURES, world, tile + ".png"))
        out = [[(rgb[0], rgb[1], rgb[2], min(255, round(a * k))) for (_, _, _, a) in row] for row in rows]
        write_png(os.path.join(TEXTURES, world, tile + "-n.png"), w, h, out)
    for tile in DARKEN:
        w, h, rows = read_png(os.path.join(TEXTURES, "pixel", tile + ".png"))
        out = [[(round(r * 0.42), round(g * 0.42), round(b * 0.45), a) for (r, g, b, a) in row] for row in rows]
        write_png(os.path.join(TEXTURES, "pixel", tile + "-n.png"), w, h, out)
    print("night tiles written")


def hls(h):
    r, g, b = (int(h[i:i + 2], 16) / 255 for i in (1, 3, 5))
    hue, l, s = colorsys.rgb_to_hls(r, g, b)
    return hue, l, s


def hexof(hue, l, s):
    r, g, b = colorsys.hls_to_rgb(hue, max(0, min(1, l)), max(0, min(1, s)))
    return "#%02x%02x%02x" % (round(r * 255), round(g * 255), round(b * 255))


def walk(value, fn):
    if isinstance(value, str):
        return fn(value) if HEX.match(value) else value
    if isinstance(value, list):
        return [walk(v, fn) for v in value]
    return value


def land_lightness(style):
    values = []
    for layer in style["layers"]:
        if layer["id"].startswith(("waypoint-land", "waypoint-landcover")) and layer["type"] == "fill":
            c = layer["paint"].get("fill-color")
            if isinstance(c, str) and HEX.match(c):
                values.append(hls(c)[1])
    return sum(values) / len(values) if values else 0.2


def convert(name):
    day = json.load(open(os.path.join(STYLES, name + ".json")))
    night = copy.deepcopy(day)
    avg = land_lightness(day)
    f = max(0.28, min(0.85, 0.17 / max(avg, 0.05)))
    sat = 0.9 if f >= 0.5 else 0.5  # bright worlds lose more saturation at night, so they read dark, not muddy
    renames = {t: t + "-n" for _, t, _, _ in LIGHT_INK}
    renames.update({t: t + "-n" for t in DARKEN})
    night["name"] = day["name"].replace("Day", "Night")
    if isinstance(night.get("metadata"), dict):
        night["metadata"]["waypoint:phase"] = "night"
    for layer in night["layers"]:
        lid, paint = layer["id"], layer.get("paint", {})
        for key in PATTERN_KEYS:
            if paint.get(key) in renames:
                paint[key] = renames[paint[key]]
        for key, value in list(paint.items()):
            if not key.endswith("color"):
                continue
            if key.startswith("text-halo"):
                paint[key] = walk(value, lambda c: hexof(hls(c)[0], 0.07, 0.25))
            elif key == "text-color":
                paint[key] = walk(value, lambda c: hexof(hls(c)[0], 0.88, min(hls(c)[2], 0.2)))
            elif layer["type"] == "line":
                def line(c, lid=lid):
                    hue, l, s = hls(c)
                    if "casing" in lid:
                        return hexof(hue, l * 0.4, s)
                    return hexof(hue, l * 0.88 if l > 0.55 else 0.8 - l * 0.55, s * 0.9)
                paint[key] = walk(value, line)
            elif layer["type"] == "hillshade":
                shade = "shadow" in key
                paint[key] = walk(value, lambda c, shade=shade: hexof(hls(c)[0], hls(c)[1] * (0.25 if shade else f * 0.7), hls(c)[2]))
            else:
                paint[key] = walk(value, lambda c: hexof(hls(c)[0], hls(c)[1] * f, hls(c)[2] * sat))
        if layer["type"] == "raster":
            paint["raster-brightness-max"] = round(paint.get("raster-brightness-max", 1.0) * 0.42, 3)
            paint["raster-brightness-min"] = 0
        if lid == "sat-tint":
            paint["fill-opacity"] = round(min(0.85, paint.get("fill-opacity", 0.3) + 0.2), 3)
    json.dump(night, open(os.path.join(STYLES, name + "_night.json"), "w"), separators=(",", ":"))
    open(os.path.join(STYLES, name + "_night.json"), "a").write("\n")
    print("wrote", name + "_night.json", "land L %.2f factor %.2f" % (avg, f))


if __name__ == "__main__":
    night_tiles()
    for world in WORLDS:
        convert(world)
