"""Generates the Los Santos texture kit: a seamless, mottled brown-grey terrain tint.

Run:  python3 -I tools/map-textures/los_santos_tiles.py
Output: app/src/main/assets/map-textures/los_santos/ls-terrain.png

The tile is two octaves of wrap-around value noise, mapped to a translucent brown-grey. It sits over
the dark land fills so the ground reads as dim terrain rather than flat colour. Drawn procedurally;
nothing is sampled from any artwork.
"""
import math
import os
import random
import struct
import zlib

SIZE = 128
COLOUR = (0x5E, 0x50, 0x42)
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "map-textures", "los_santos")


def lattice(n, rnd):
    return [[rnd.random() for _ in range(n)] for _ in range(n)]


def smooth(t):
    return t * t * (3 - 2 * t)


def sample(grid, n, x, y):
    gx, gy = x * n, y * n
    x0, y0 = int(gx) % n, int(gy) % n
    x1, y1 = (x0 + 1) % n, (y0 + 1) % n
    fx, fy = smooth(gx - int(gx)), smooth(gy - int(gy))
    top = grid[y0][x0] * (1 - fx) + grid[y0][x1] * fx
    bottom = grid[y1][x0] * (1 - fx) + grid[y1][x1] * fx
    return top * (1 - fy) + bottom * fy


def build():
    rnd = random.Random(2013)
    coarse, fine = lattice(6, rnd), lattice(14, rnd)
    rows = []
    for y in range(SIZE):
        row = bytearray([0])
        for x in range(SIZE):
            u, v = x / SIZE, y / SIZE
            n = 0.68 * sample(coarse, 6, u, v) + 0.32 * sample(fine, 14, u, v)
            alpha = max(0.0, min(1.0, (n - 0.44) * 1.3)) * 0.34
            row += bytes((COLOUR[0], COLOUR[1], COLOUR[2], round(alpha * 255)))
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "ls-terrain.png"), "wb") as f:
        f.write(png)
    print("wrote ls-terrain.png")


if __name__ == "__main__":
    build()
