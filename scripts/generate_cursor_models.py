#!/usr/bin/env python3
"""Build two original, self-contained GLB map cursors without external assets.

The existing Waypoint car is maintained separately. These models face +Y, with
+Z above the road, matching the car and the phone map's snapshot renderer.
"""

from __future__ import annotations

import json
import math
import struct
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DESTINATION = ROOT / "app/src/main/assets/cursor-models"

PALETTE = {
    "tire": (0.025, 0.045, 0.075, 0.90, 0.02),
    "rubber": (0.075, 0.11, 0.16, 0.82, 0.03),
    "metal": (0.48, 0.59, 0.66, 0.35, 0.70),
    "dark_metal": (0.15, 0.22, 0.30, 0.42, 0.52),
    "cobalt": (0.025, 0.36, 0.78, 0.32, 0.18),
    "sky": (0.13, 0.78, 0.96, 0.28, 0.12),
    "orange": (0.98, 0.35, 0.075, 0.38, 0.08),
    "gold": (0.98, 0.70, 0.15, 0.36, 0.28),
    "white": (0.96, 0.98, 0.86, 0.25, 0.02),
    "red": (0.90, 0.08, 0.075, 0.35, 0.01),
    "glass": (0.10, 0.25, 0.35, 0.23, 0.12),
}


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def mul(a, value):
    return tuple(x * value for x in a)


def cross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )


def unit(vector):
    length = math.sqrt(sum(x * x for x in vector))
    return tuple(x / length for x in vector)


class Mesh:
    def __init__(self):
        self.parts = defaultdict(lambda: {"positions": [], "normals": [], "indices": []})

    def triangle(self, material, a, b, c):
        normal = unit(cross(sub(b, a), sub(c, a)))
        part = self.parts[material]
        start = len(part["positions"]) // 3
        for vertex in (a, b, c):
            part["positions"].extend(vertex)
            part["normals"].extend(normal)
        part["indices"].extend((start, start + 1, start + 2))

    def quad(self, material, a, b, c, d):
        self.triangle(material, a, b, c)
        self.triangle(material, a, c, d)

    def slab(self, material, outline, bottom, top):
        """Extrude a convex XY outline with flat shaded facets."""
        upper = [(x, y, top) for x, y in outline]
        lower = [(x, y, bottom) for x, y in outline]
        for i in range(1, len(outline) - 1):
            self.triangle(material, upper[0], upper[i], upper[i + 1])
            self.triangle(material, lower[0], lower[i + 1], lower[i])
        for i in range(len(outline)):
            j = (i + 1) % len(outline)
            self.quad(material, lower[i], lower[j], upper[j], upper[i])

    def box(self, material, x0, x1, y0, y1, z0, z1):
        self.slab(material, [(x0, y0), (x1, y0), (x1, y1), (x0, y1)], z0, z1)

    def rod(self, material, start, finish, radius, sides=10):
        axis = unit(sub(finish, start))
        reference = (0, 0, 1) if abs(axis[2]) < 0.8 else (0, 1, 0)
        u = unit(cross(axis, reference))
        v = cross(axis, u)
        near = []
        far = []
        for i in range(sides):
            angle = 2 * math.pi * i / sides
            offset = add(mul(u, radius * math.cos(angle)), mul(v, radius * math.sin(angle)))
            near.append(add(start, offset))
            far.append(add(finish, offset))
        for i in range(sides):
            j = (i + 1) % sides
            self.quad(material, near[i], near[j], far[j], far[i])
            self.triangle(material, start, near[j], near[i])
            self.triangle(material, finish, far[i], far[j])

    def wheel(self, y, radius, width, rim):
        self.rod("tire", (-width / 2, y, radius), (width / 2, y, radius), radius, 16)
        self.rod(rim, (-width / 2 - 0.008, y, radius), (width / 2 + 0.008, y, radius), radius * 0.57, 12)
        self.rod("dark_metal", (-width / 2 - 0.017, y, radius),
                 (width / 2 + 0.017, y, radius), radius * 0.22, 10)


def motorcycle():
    mesh = Mesh()
    mesh.wheel(-0.69, 0.22, 0.20, "metal")
    mesh.wheel(0.69, 0.22, 0.20, "metal")
    mesh.slab("dark_metal", [(-0.16, -0.68), (0.16, -0.68),
                             (0.23, -0.19), (0.16, 0.47), (-0.16, 0.47),
                             (-0.23, -0.19)], 0.22, 0.34)
    mesh.slab("cobalt", [(-0.18, -0.30), (0.18, -0.30), (0.23, 0.06),
                         (0.17, 0.43), (0.0, 0.58), (-0.17, 0.43),
                         (-0.23, 0.06)], 0.32, 0.47)
    mesh.slab("sky", [(-0.105, 0.05), (0.105, 0.05), (0.14, 0.33),
                      (0.0, 0.44), (-0.14, 0.33)], 0.47, 0.485)
    mesh.slab("rubber", [(-0.14, -0.53), (0.14, -0.53), (0.17, -0.36),
                         (0.13, -0.10), (-0.13, -0.10), (-0.17, -0.36)],
              0.40, 0.53)
    # Dark side cases make the motorcycle legible as a vehicle at map scale.
    for sign in (-1, 1):
        mesh.box("cobalt", *sorted((sign * 0.17, sign * 0.34)), -0.55, -0.27, 0.31, 0.43)
        mesh.box("sky", *sorted((sign * 0.19, sign * 0.31)), -0.51, -0.47, 0.43, 0.445)
        mesh.rod("metal", (sign * 0.11, -0.11, 0.27), (sign * 0.16, 0.51, 0.25), 0.027)
        mesh.box("rubber", *sorted((sign * 0.23, sign * 0.40)), 0.33, 0.43, 0.47, 0.55)
        mesh.box("dark_metal", *sorted((sign * 0.37, sign * 0.45)), 0.48, 0.57, 0.52, 0.57)
        mesh.box("orange", *sorted((sign * 0.18, sign * 0.28)), 0.53, 0.63, 0.43, 0.47)
    mesh.rod("metal", (-0.38, 0.36, 0.50), (0.38, 0.36, 0.50), 0.035)
    mesh.slab("glass", [(-0.14, 0.45), (0.14, 0.45),
                         (0.12, 0.62), (-0.12, 0.62)], 0.48, 0.56)
    mesh.slab("cobalt", [(-0.13, 0.57), (0.13, 0.57),
                         (0.12, 0.91), (0.0, 0.98), (-0.12, 0.91)], 0.33, 0.48)
    mesh.box("white", -0.095, 0.095, 0.91, 0.975, 0.48, 0.51)
    mesh.box("red", -0.13, 0.13, -0.91, -0.85, 0.37, 0.42)
    return mesh


def bicycle():
    mesh = Mesh()
    mesh.wheel(-0.70, 0.23, 0.12, "metal")
    mesh.wheel(0.70, 0.23, 0.12, "metal")
    # A pair of visible rails gives the compact cycle a clear top silhouette.
    for sign in (-1, 1):
        mesh.rod("gold", (sign * 0.16, -0.51, 0.32),
                 (sign * 0.11, 0.48, 0.39), 0.044)
        mesh.rod("metal", (sign * 0.10, 0.42, 0.39),
                 (sign * 0.065, 0.72, 0.23), 0.035)
        mesh.rod("orange", (sign * 0.17, -0.49, 0.33),
                 (sign * 0.03, -0.06, 0.35), 0.038)
        mesh.box("cobalt", *sorted((sign * 0.17, sign * 0.34)),
                 -0.53, -0.26, 0.30, 0.43)
        mesh.box("sky", *sorted((sign * 0.19, sign * 0.31)),
                 -0.50, -0.46, 0.43, 0.45)
        mesh.box("rubber", *sorted((sign * 0.30, sign * 0.46)),
                 0.42, 0.53, 0.41, 0.48)
    mesh.rod("metal", (-0.16, -0.07, 0.35), (0.16, -0.07, 0.35), 0.037)
    mesh.rod("gold", (0, -0.06, 0.35), (0, 0.50, 0.39), 0.045)
    mesh.slab("rubber", [(-0.12, -0.34), (0.12, -0.34),
                         (0.16, -0.13), (0.08, -0.04),
                         (-0.08, -0.04), (-0.16, -0.13)], 0.44, 0.53)
    mesh.rod("metal", (-0.38, 0.48, 0.44), (0.38, 0.48, 0.44), 0.034)
    mesh.box("white", -0.055, 0.055, 0.54, 0.60, 0.39, 0.45)
    mesh.box("red", -0.085, 0.085, -0.90, -0.84, 0.31, 0.38)
    mesh.box("dark_metal", -0.20, 0.20, -0.55, -0.42, 0.38, 0.405)
    return mesh


def encode_glb(mesh, name):
    binary = bytearray()
    views = []
    accessors = []
    primitives = []
    materials = []

    def append(data, target):
        offset = len(binary)
        binary.extend(data)
        binary.extend(b"\0" * (-len(binary) % 4))
        views.append({"buffer": 0, "byteOffset": offset, "byteLength": len(data), "target": target})
        return len(views) - 1

    for material_name, part in mesh.parts.items():
        positions = part["positions"]
        normals = part["normals"]
        indices = part["indices"]
        if len(positions) // 3 > 65535:
            raise ValueError("GLB primitive exceeds 16-bit index range")
        pview = append(struct.pack("<" + "f" * len(positions), *positions), 34962)
        nview = append(struct.pack("<" + "f" * len(normals), *normals), 34962)
        iview = append(struct.pack("<" + "H" * len(indices), *indices), 34963)
        points = list(zip(positions[0::3], positions[1::3], positions[2::3]))
        accessors.append({"bufferView": pview, "componentType": 5126,
                          "count": len(points), "type": "VEC3",
                          "min": [min(p[i] for p in points) for i in range(3)],
                          "max": [max(p[i] for p in points) for i in range(3)]})
        position_index = len(accessors) - 1
        accessors.append({"bufferView": nview, "componentType": 5126,
                          "count": len(points), "type": "VEC3"})
        normal_index = len(accessors) - 1
        accessors.append({"bufferView": iview, "componentType": 5123,
                          "count": len(indices), "type": "SCALAR"})
        index_index = len(accessors) - 1
        red, green, blue, roughness, metallic = PALETTE[material_name]
        materials.append({"name": material_name,
                          "pbrMetallicRoughness": {
                              "baseColorFactor": [red, green, blue, 1],
                              "metallicFactor": metallic,
                              "roughnessFactor": roughness},
                          "doubleSided": True})
        primitives.append({"attributes": {"POSITION": position_index, "NORMAL": normal_index},
                           "indices": index_index, "material": len(materials) - 1})

    document = {"asset": {"version": "2.0", "generator": "Waypoint original cursor models"},
                "scene": 0, "scenes": [{"nodes": [0]}],
                "nodes": [{"name": name, "mesh": 0}],
                "meshes": [{"primitives": primitives}],
                "materials": materials, "accessors": accessors,
                "bufferViews": views, "buffers": [{"byteLength": len(binary)}]}
    encoded = json.dumps(document, separators=(",", ":")).encode("utf-8")
    encoded += b" " * (-len(encoded) % 4)
    total = 12 + 8 + len(encoded) + 8 + len(binary)
    return (struct.pack("<III", 0x46546C67, 2, total)
            + struct.pack("<II", len(encoded), 0x4E4F534A) + encoded
            + struct.pack("<II", len(binary), 0x004E4942) + binary)


def main():
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for name, build in (("Motorcycle", motorcycle), ("Bicycle", bicycle)):
        data = encode_glb(build(), name)
        path = DESTINATION / f"{name}.glb"
        path.write_bytes(data)
        print(f"{path.relative_to(ROOT)}: {len(data):,} bytes")


if __name__ == "__main__":
    main()
