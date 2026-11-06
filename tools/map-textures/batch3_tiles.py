"""Tiles for batch three: Death Stranding scan lines and Ghost of Tsushima ink wash.

Run:  python3 -B tools/map-textures/batch3_tiles.py
Output: app/src/main/assets/map-textures/bridges/bs-scan.png
        app/src/main/assets/map-textures/ghost/gt-ink.png
Drawn procedurally; nothing is sampled from any artwork.
"""
import os
import random
import struct
import zlib

ASSETS = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "map-textures")


def write_png(world, name, w, h, pixels):
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    folder = os.path.join(ASSETS, world)
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, name), "wb") as f:
        f.write(png)
    print("wrote", world + "/" + name)


def scan():
    """Horizontal hologram scan lines: a thin bright cyan line and a dark line every 8 px, plus sparse
    grain, so the map reads as a projected display."""
    size = 8
    rnd = random.Random(53)
    px = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for x in range(size):
        px[0][x] = (90, 200, 255, 46)
        px[4][x] = (0, 8, 24, 70)
    for _ in range(3):
        px[rnd.randrange(size)][rnd.randrange(size)] = (160, 225, 255, 40)
    write_png("bridges", "bs-scan.png", size, size, px)


def ink():
    """Soft ink-wash blotches: two octaves of wrap-around value noise in a warm black, for Ghost land."""
    size = 128
    rnd = random.Random(97)

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

    coarse, fine = lattice(4), lattice(10)
    rows = []
    for y in range(size):
        row = []
        for x in range(size):
            n = 0.72 * sample(coarse, 4, x / size, y / size) + 0.28 * sample(fine, 10, x / size, y / size)
            alpha = max(0.0, min(1.0, (n - 0.40) * 1.6)) * 0.42
            row.append((0x2A, 0x28, 0x24, round(alpha * 255)))
        rows.append(row)
    write_png("ghost", "gt-ink.png", size, size, rows)


if __name__ == "__main__":
    scan()
    ink()
