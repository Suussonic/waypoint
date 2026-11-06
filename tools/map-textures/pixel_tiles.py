"""Generates the Pixel World texture kit: original block-style tiles with a 3D bevel.

Run:  python3 -I tools/map-textures/pixel_tiles.py
Output: app/src/main/assets/map-textures/pixel/*.png

Each tile is 16x16 chunky texels (4 px each = 64 px). Every tile has a lit top-left edge and a
shaded bottom-right edge, so when the tile repeats it reads as a grid of bevelled blocks.
Colours and noise are authored here; nothing is sampled from any game artwork.
"""
import os
import random
import struct
import zlib

TEXELS = 16
SCALE = 4
SIZE = TEXELS * SCALE
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "map-textures", "pixel")


def shade(rgb, amount):
    return tuple(max(0, min(255, round(c + amount))) for c in rgb)


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def write_png(name, texels):
    rows = []
    for ty in range(TEXELS):
        line = bytearray()
        for tx in range(TEXELS):
            r, g, b = texels[ty][tx]
            line += bytes((r, g, b, 255)) * SCALE
        for _ in range(SCALE):
            rows.append(b"\x00" + bytes(line))
    raw = b"".join(rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name), "wb") as f:
        f.write(png)
    print("wrote", name)


def noise_tile(palette, seed, weights=None):
    rnd = random.Random(seed)
    return [[rnd.choices(palette, weights=weights)[0] for _ in range(TEXELS)] for _ in range(TEXELS)]


def bevel(texels, light=22, dark=26):
    for i in range(TEXELS):
        texels[0][i] = shade(texels[0][i], light)       # top edge lit
        texels[i][0] = shade(texels[i][0], light)       # left edge lit
        texels[TEXELS - 1][i] = shade(texels[TEXELS - 1][i], -dark)  # bottom edge shaded
        texels[i][TEXELS - 1] = shade(texels[i][TEXELS - 1], -dark)  # right edge shaded
    return texels


def grass():
    base = [hexrgb(c) for c in ("#5f9440", "#5a8c3c", "#68a046", "#548538")]
    write_png("px-grass.png", bevel(noise_tile(base, 3)))


def leaves():
    base = [hexrgb(c) for c in ("#2f6a2b", "#285c26", "#367531", "#1c4a1d")]
    t = noise_tile(base, 7, [4, 4, 3, 2])
    write_png("px-leaves.png", bevel(t, 16, 24))


def stone():
    base = [hexrgb(c) for c in ("#8a8b86", "#7d7e79", "#96978f", "#6f706b")]
    write_png("px-stone.png", bevel(noise_tile(base, 11)))


def planks():
    """Oak-plank walls: four horizontal boards with grain, used on extruded building faces."""
    rnd = random.Random(19)
    board = [hexrgb(c) for c in ("#b08a52", "#a8814c", "#b99358")]
    t = [[board[0]] * TEXELS for _ in range(TEXELS)]
    for ty in range(TEXELS):
        tone = rnd.choice(board)
        for tx in range(TEXELS):
            c = tone if rnd.random() > 0.2 else shade(tone, rnd.choice((-14, 12)))
            t[ty][tx] = shade(c, -34) if ty % 4 == 3 else c  # dark gap between boards
    write_png("px-planks.png", bevel(t, 12, 20))


def path():
    base = [hexrgb(c) for c in ("#bda677", "#b09a6c", "#c8b283", "#a58f64")]
    write_png("px-path.png", bevel(noise_tile(base, 23, [5, 4, 3, 2]), 10, 14))


def water():
    rnd = random.Random(29)
    deep, mid, high = hexrgb("#356ab0"), hexrgb("#3f78bd"), hexrgb("#6b9ed6")
    t = [[mid if rnd.random() > 0.35 else deep for _ in range(TEXELS)] for _ in range(TEXELS)]
    for _ in range(9):  # short lighter streaks
        y, x, run = rnd.randrange(TEXELS), rnd.randrange(TEXELS), rnd.randrange(2, 5)
        for k in range(run):
            t[y][(x + k) % TEXELS] = high
    write_png("px-water.png", t)  # no bevel: water should flow, not tile as blocks


def sand():
    base = [hexrgb(c) for c in ("#e2d49c", "#d9ca92", "#ebdfa8", "#cfbf88")]
    write_png("px-sand.png", bevel(noise_tile(base, 31), 14, 18))


if __name__ == "__main__":
    for fn in (grass, leaves, stone, planks, path, water, sand):
        fn()
