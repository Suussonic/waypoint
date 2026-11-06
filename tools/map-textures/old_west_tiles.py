"""Generates the Old West texture kit: original, seamless, ink-on-paper tiles.

Run:  python3 -I tools/map-textures/old_west_tiles.py
Output: app/src/main/assets/map-textures/old_west/*.png

Every tile is drawn procedurally from simple strokes (no sampled artwork). Tiles wrap at
their edges so MapLibre fill-pattern repeats without seams.
"""
import math
import os
import random
import struct
import zlib

INK = (0x4A, 0x3A, 0x28)
SS = 4  # supersampling factor

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "map-textures", "old_west")


class Tile:
    def __init__(self, size):
        self.size = size
        self.n = size * SS
        self.a = [0.0] * (self.n * self.n)

    def dot(self, x, y, r, alpha):
        n = self.n
        cx, cy, rr = x * SS, y * SS, r * SS
        x0, x1 = int(cx - rr - 1), int(cx + rr + 1)
        y0, y1 = int(cy - rr - 1), int(cy + rr + 1)
        for iy in range(y0, y1 + 1):
            for ix in range(x0, x1 + 1):
                d = math.hypot(ix + 0.5 - cx, iy + 0.5 - cy)
                if d <= rr:
                    i = (iy % n) * n + (ix % n)
                    self.a[i] = max(self.a[i], alpha)

    def line(self, x0, y0, x1, y1, width, alpha):
        length = math.hypot(x1 - x0, y1 - y0)
        steps = max(2, int(length * SS * 2))
        for s in range(steps + 1):
            t = s / steps
            self.dot(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, width / 2, alpha)

    def png(self, path):
        size, n = self.size, self.n
        rows = []
        for y in range(size):
            row = bytearray([0])
            for x in range(size):
                acc = 0.0
                for sy in range(SS):
                    base = (y * SS + sy) * n + x * SS
                    acc += sum(self.a[base:base + SS])
                alpha = acc / (SS * SS)
                row += bytes((INK[0], INK[1], INK[2], max(0, min(255, round(alpha * 255)))))
            rows.append(bytes(row))
        raw = b"".join(rows)

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(png)


def jitter_grid(size, count, rnd):
    """Evenly spread points with jitter, so patterns do not look like a grid."""
    per = max(1, round(math.sqrt(count)))
    cell = size / per
    pts = []
    for gy in range(per):
        for gx in range(per):
            pts.append(((gx + 0.2 + rnd.random() * 0.6) * cell, (gy + 0.2 + rnd.random() * 0.6) * cell))
    return pts


def trees():
    """Mixed pines and rounded tree masses, unevenly spaced, so it reads as drawn rather than stamped."""
    k = 1.8
    t, rnd = Tile(200), random.Random(17)
    pts = jitter_grid(200, 9, rnd)
    for n, (x, y) in enumerate(pts):
        size = (3.4 + rnd.random() * 1.6) * k
        if n % 2 == 0:
            # pine: open triangle outline over a short trunk
            apex, left, right = (x, y - size * 1.5), (x - size * 0.9, y + size * 0.4), (x + size * 0.9, y + size * 0.4)
            for (x0, y0), (x1, y1) in ((apex, left), (apex, right), (left, right)):
                t.line(x0, y0, x1, y1, 1.0 * k, 0.8)
            t.line(x, y + size * 0.4, x, y + size * 0.9, 1.1 * k, 0.8)
        else:
            # rounded tree mass: three filled lobes with a darker base tick
            for dx, dy, r in ((-0.7, 0.1, 0.8), (0.7, 0.1, 0.8), (0.0, -0.6, 0.95)):
                t.dot(x + dx * size, y + dy * size, r * size * 0.62, 0.55)
            t.line(x - size * 0.9, y + size * 0.75, x + size * 0.9, y + size * 0.75, 0.9 * k, 0.75)
    t.png(os.path.join(OUT, "ow-trees.png"))


def grass():
    k = 1.8
    t, rnd = Tile(130), random.Random(23)
    for x, y in jitter_grid(130, 9, rnd):
        for ang in (-28, 0, 28):
            a = math.radians(ang - 90)
            t.line(x, y, x + math.cos(a) * 4.2 * k, y + math.sin(a) * 4.2 * k, 1.4 * k, 0.95)
    t.png(os.path.join(OUT, "ow-grass.png"))


def ripple():
    k = 1.8
    t, rnd = Tile(170), random.Random(5)
    for i, (x, y) in enumerate(jitter_grid(170, 9, rnd)):
        pts = []
        for s in range(0, 25):
            u = s / 24
            pts.append((x + u * 22 * k, y + math.sin(u * math.pi * 2 + i) * 1.6 * k))
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            t.line(x0, y0, x1, y1, 1.4 * k, 0.95)
    t.png(os.path.join(OUT, "ow-ripple.png"))


def hatch():
    t = Tile(12)
    for k in range(-3, 4):
        t.line(k * 4 - 2, -2, k * 4 + 14, 14, 0.9, 0.5)
    t.png(os.path.join(OUT, "ow-hatch.png"))


def stipple():
    t, rnd = Tile(80), random.Random(31)
    for _ in range(40):
        t.dot(rnd.random() * 80, rnd.random() * 80, 1.6 + rnd.random() * 0.8, 0.95)
    t.png(os.path.join(OUT, "ow-stipple.png"))


if __name__ == "__main__":
    for fn in (trees, grass, ripple, hatch, stipple):
        fn()
        print("wrote", fn.__name__)
