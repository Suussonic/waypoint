"""Tiles for batch two: Skyrim mountain glyphs, Elden Ring engraved waves, Witcher painted wash.

Run:  python3 -I tools/map-textures/batch2_tiles.py
Output: app/src/main/assets/map-textures/{skyrim,lands_between,rivia}/*.png
Reuses the stroke rasteriser from old_west_tiles.py with a different ink colour. Drawn procedurally;
nothing is sampled from any artwork.
"""
import math
import os
import random
import struct
import zlib

import old_west_tiles as ow

ASSETS = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "map-textures")


def stroke_tile(world, name, size, ink, draw):
    ow.INK = ink
    tile = ow.Tile(size)
    draw(tile)
    ow.OUT = os.path.join(ASSETS, world)
    tile.png(os.path.join(ow.OUT, name))
    print("wrote", world + "/" + name)


def peaks(t):
    """Little mountain glyphs: an inked peak with hatching down its shaded right slope."""
    rnd = random.Random(61)
    for x, y in ow.jitter_grid(192, 9, rnd):
        s = 1.0 + rnd.random() * 0.5
        left, apex, right = (x - 13 * s, y + 9 * s), (x, y - 11 * s), (x + 14 * s, y + 9 * s)
        t.line(*left, *apex, 1.6, 0.9)
        t.line(*apex, *right, 1.6, 0.9)
        for k in range(1, 5):  # hatch on the right slope
            u = k / 5
            x0, y0 = apex[0] + (right[0] - apex[0]) * u, apex[1] + (right[1] - apex[1]) * u
            t.line(x0, y0, x0 - 5 * s, y0 + 1.5 * s, 1.1, 0.7)


def waves(t):
    """Engraved sea lines: long, evenly spaced, gently waving, seamless left to right."""
    size = 160
    for row in range(0, size, 14):
        pts = []
        for x in range(0, size + 1, 4):
            pts.append((x, row + 2.4 * math.sin((x / size) * math.pi * 8 + row * 0.7)))
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            t.line(x0, y0, x1, y1, 1.2, 0.5)


def wash_tile():
    """Soft warm blotches, like dry brush on parchment (two octaves of wrap-around value noise)."""
    size = 128
    rnd = random.Random(73)

    def lattice(n):
        return [[rnd.random() for _ in range(n)] for _ in range(n)]

    def smooth(v):
        return v * v * (3 - 2 * v)

    def sample(grid, n, u, v):
        gx, gy = u * n, v * n
        x0, y0 = int(gx) % n, int(gy) % n
        x1, y1 = (x0 + 1) % n, (y0 + 1) % n
        fx, fy = smooth(gx - int(gx)), smooth(gy - int(gy))
        top = grid[y0][x0] * (1 - fx) + grid[y0][x1] * fx
        bottom = grid[y1][x0] * (1 - fx) + grid[y1][x1] * fx
        return top * (1 - fy) + bottom * fy

    coarse, fine = lattice(5), lattice(12)
    rows = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            n = 0.7 * sample(coarse, 5, x / size, y / size) + 0.3 * sample(fine, 12, x / size, y / size)
            alpha = max(0.0, min(1.0, (n - 0.38) * 1.5)) * 0.3
            row += bytes((0x8A, 0x6A, 0x30, round(alpha * 255)))
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    folder = os.path.join(ASSETS, "rivia")
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, "wt-wash.png"), "wb") as f:
        f.write(png)
    print("wrote rivia/wt-wash.png")


if __name__ == "__main__":
    stroke_tile("skyrim", "sk-peaks.png", 192, (0x5A, 0x3F, 0x26), peaks)
    stroke_tile("lands_between", "eb-waves.png", 160, (0x35, 0x54, 0x5A), waves)
    wash_tile()
