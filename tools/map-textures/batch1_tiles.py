"""Tiles for batch one: Night City water grid and Green Terminal phosphor dots.

Run:  python3 -I tools/map-textures/batch1_tiles.py
Output: app/src/main/assets/map-textures/night_city/nc-grid.png
        app/src/main/assets/map-textures/green_terminal/gt-dots.png
Drawn procedurally; nothing is sampled from any artwork.
"""
import os
import random
import struct
import zlib

ASSETS = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "map-textures")


def write_png(world, name, size, pixels):
    rows = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            row += bytes(pixels[y][x])
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    folder = os.path.join(ASSETS, world)
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, name), "wb") as f:
        f.write(png)
    print("wrote", world + "/" + name)


def night_grid():
    """Faint cyan grid lines with a brighter node at each crossing: a holographic water sheet."""
    size, step = 48, 24
    px = [[(0, 0, 0, 0)] * size for _ in range(size)]
    line, node = (60, 220, 232, 64), (120, 245, 255, 150)
    for i in range(size):
        for k in range(0, size, step):
            px[k][i] = line
            px[i][k] = line
    for gy in range(0, size, step):
        for gx in range(0, size, step):
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    px[(gy + dy) % size][(gx + dx) % size] = node
    write_png("night_city", "nc-grid.png", size, px)


def green_dots():
    """Sparse phosphor dots, like a terminal dither, for land areas."""
    size = 32
    rnd = random.Random(41)
    px = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for _ in range(9):
        x, y = rnd.randrange(size), rnd.randrange(size)
        px[y][x] = (99, 212, 63, 150)
        px[y][(x + 1) % size] = (99, 212, 63, 90)
    write_png("green_terminal", "gt-dots.png", size, px)


if __name__ == "__main__":
    night_grid()
    green_dots()
