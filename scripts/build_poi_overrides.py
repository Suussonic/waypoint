#!/usr/bin/env python3
"""Rebuild checked-in POI cell overrides from their documented source recipes.

The ImageGen originals live in ignored output/imagegen directories. Run this
script when those sources are available; the resulting 128 px PNG overrides
are checked in so ordinary atlas repair does not depend on ignored files.
"""
from __future__ import annotations

import argparse
from collections import deque
from hashlib import sha256
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

from generate_map_assets import Raster
from import_imagegen_poi_atlases import (
    DEFAULT_MANIFEST,
    DEFAULT_SOURCE,
    OVERRIDE_DIR,
    PILLOW_VERSION,
    ROOT,
    SOURCE_SIZE,
    load_crop_manifest,
    transparent_cell,
    transparent_sprite_from_crop,
)
from import_imagegen_poi_cell_repairs import centered_sprite
from map_glyphs import draw_category_glyph
from map_theme_catalog import THEMES


def color(value: str) -> tuple[int, int, int, int]:
    return tuple(bytes.fromhex(value.removeprefix("#"))) + (255,)


def keep_primary_component(sprite: Image.Image, threshold: int = 1, diagonal: bool = True) -> Image.Image:
    """Clear detached source fragments while retaining the complete badge."""
    alpha = sprite.getchannel("A").tobytes()
    seen = bytearray(128 * 128)
    components: list[list[int]] = []
    for start, value in enumerate(alpha):
        if value <= threshold or seen[start]:
            continue
        queue = deque([start])
        seen[start] = 1
        component = []
        while queue:
            pixel = queue.popleft()
            component.append(pixel)
            x, y = pixel % 128, pixel // 128
            for ny in range(max(0, y - 1), min(127, y + 1) + 1):
                for nx in range(max(0, x - 1), min(127, x + 1) + 1):
                    if not diagonal and abs(nx - x) + abs(ny - y) != 1:
                        continue
                    neighbor = ny * 128 + nx
                    if alpha[neighbor] > threshold and not seen[neighbor]:
                        seen[neighbor] = 1
                        queue.append(neighbor)
        components.append(component)
    if not components:
        raise ValueError("The source crop contains no visible badge")
    keep = set(max(components, key=len))
    pixels = sprite.load()
    for y in range(128):
        for x in range(128):
            if alpha[y * 128 + x] > threshold and y * 128 + x not in keep:
                pixels[x, y] = (0, 0, 0, 0)
    return sprite


def place_glyph(
    sheet: Image.Image,
    world: str,
    category: str,
    recipe: dict,
    column_bounds: list[int],
    row_bounds: list[int],
) -> Image.Image:
    sprite = transparent_cell(sheet, 0, column_bounds, row_bounds)
    cover = Image.new("RGBA", sprite.size, (0, 0, 0, 0))
    drawer = ImageDraw.Draw(cover)
    fill = recipe["cover_fill"]
    if recipe["shape"] == "ellipse":
        drawer.ellipse(tuple(recipe["cover_box"]), fill=fill)
    elif recipe["shape"] == "rectangle":
        drawer.rectangle(tuple(recipe["cover_box"]), fill=fill)
    elif recipe["shape"] == "polygon":
        drawer.polygon([tuple(point) for point in recipe["cover_points"]], fill=fill)
    else:
        raise ValueError(f"{world}/{category}: unknown cover shape")
    sprite.alpha_composite(cover)
    glyph = Raster(128, 128)
    draw_category_glyph(
        glyph, category, color(recipe["ink"]), color(recipe["accent"]),
        THEMES[world]["marker"].lower(),
    )
    glyph_image = Image.frombytes("RGBA", (128, 128), bytes(glyph.pixels))
    scale = recipe.get("glyph_scale", 1.0)
    if scale != 1.0:
        side = round(128 * scale)
        glyph_image = glyph_image.resize((side, side), Image.Resampling.LANCZOS)
    shifted = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    shifted.alpha_composite(glyph_image, ((128 - glyph_image.width) // 2, (128 - glyph_image.height) // 2 - 3))
    sprite.alpha_composite(shifted)
    return sprite


def generate_override(
    sheet: Image.Image,
    world: str,
    category: str,
    recipe: dict,
    entry: dict,
    repository: Path,
) -> Image.Image:
    kind = recipe["kind"]
    label = f"{world}__{category}"
    if kind == "standalone":
        source = repository / recipe["source"]
        if not source.is_file() or sha256(source.read_bytes()).hexdigest() != recipe["source_sha256"]:
            raise ValueError(f"{label}: standalone source is missing or differs from its reviewed SHA-256")
        with Image.open(source) as opened:
            sprite = centered_sprite(opened, label)
    elif kind == "sheet_crop":
        x0, y0, x1, y1 = recipe["bounds"]
        if not (0 <= x0 < x1 <= SOURCE_SIZE[0] and 0 <= y0 < y1 <= SOURCE_SIZE[1]):
            raise ValueError(f"{label}: invalid sheet crop bounds")
        sprite = transparent_sprite_from_crop(sheet.crop((x0, y0, x1, y1)), label)
    elif kind == "place_glyph":
        sprite = place_glyph(sheet, world, category, recipe, entry["column_bounds"], entry["row_bounds"])
    else:
        raise ValueError(f"{label}: unknown override recipe {kind}")
    if recipe.get("keep_primary_component"):
        sprite = keep_primary_component(
            sprite,
            threshold=recipe.get("component_threshold", 1),
            diagonal=recipe.get("component_diagonal", True),
        )
    return sprite


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output-dir", type=Path, default=OVERRIDE_DIR)
    parser.add_argument("--world", action="append", help="Rebuild only this world; repeatable")
    parser.add_argument("--check", action="store_true", help="Compare pixels with checked-in overrides")
    args = parser.parse_args()
    manifest = load_crop_manifest(args.manifest)
    if PILLOW_VERSION != manifest["pillow_version"]:
        raise SystemExit("Override rendering requires tools/imagegen/requirements.txt (Pillow 13.0.0)")
    selected = set(args.world or manifest["worlds"])
    if selected - set(manifest["worlds"]):
        raise SystemExit(f"Unsupported ImageGen world(s): {sorted(selected - set(manifest['worlds']))}")
    if not args.check:
        args.output_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for world in sorted(selected):
        entry = manifest["worlds"][world]
        if not entry["overrides"]:
            continue
        source = args.source_dir / f"{world}.png"
        if not source.is_file() or sha256(source.read_bytes()).hexdigest() != entry["sheet_sha256"]:
            raise ValueError(f"{world}: sheet is missing or differs from its reviewed SHA-256")
        with Image.open(source) as opened:
            if opened.size != SOURCE_SIZE:
                raise ValueError(f"{world}: expected source size {SOURCE_SIZE}, found {opened.size}")
            sheet = opened.convert("RGBA")
        for category, recipe in entry["overrides"].items():
            sprite = generate_override(sheet, world, category, recipe, entry, ROOT)
            output = args.output_dir / f"{world}__{category}.png"
            if args.check:
                with Image.open(output) as opened:
                    if opened.size != sprite.size or ImageChops.difference(opened.convert("RGBA"), sprite).getbbox():
                        raise ValueError(f"Override does not match source recipe: {output}")
            else:
                sprite.save(output, optimize=True)
            count += 1
    print(f"{'Verified' if args.check else 'Generated'} {count} reviewed POI overrides across {len(selected)} worlds.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
