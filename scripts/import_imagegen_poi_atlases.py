#!/usr/bin/env python3
"""Import reviewed ImageGen POI sheets using tracked crop and repair sources.

The original source sheets are kept outside Git. The checked-in crop manifest
records their checksums and nonuniform cell bounds; checked-in 128 px override
sprites cover omitted, reordered, or contaminated cells. Nier and Night City
have dedicated generators and are deliberately excluded. Pillow is required.
"""
from __future__ import annotations

import argparse
from hashlib import sha256
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageOps, __version__ as PILLOW_VERSION

from generate_night_icon_variants import night_atlas_variant, night_variant
from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "output/imagegen/poi-atlases-all"
DEFAULT_MARKER_SOURCE = ROOT / "output/imagegen/world-markers"
DEFAULT_MANIFEST = ROOT / "tools/imagegen/poi-crop-manifest.json"
OVERRIDE_DIR = ROOT / "tools/imagegen/poi-overrides"
POI_DIR = ROOT / "app/src/main/assets/poi"
MARKER_DIR = ROOT / "app/src/main/assets/map-markers"
SOURCE_COLUMNS = 5
SOURCE_ROWS = 10
SOURCE_CELL = 256
TARGET_CELL = 128
SOURCE_SIZE = (SOURCE_COLUMNS * SOURCE_CELL, SOURCE_ROWS * SOURCE_CELL)
EXCLUDED_GENERATORS = {
    "nier": "scripts/generate_map_assets.py",
    "night_city": "scripts/generate_night_city_icons.py",
}


def magenta_key_mask(image: Image.Image) -> Image.Image:
    """Return a broad magenta mask; use connectivity to protect interior art."""
    red, green, blue = image.convert("RGB").split()
    above_red = red.point([255 if value > 140 else 0 for value in range(256)])
    above_blue = blue.point([255 if value > 130 else 0 for value in range(256)])
    below_green = green.point([255 if value < 110 else 0 for value in range(256)])
    red_delta = ImageChops.subtract(red, green).point(
        [255 if value > 65 else 0 for value in range(256)]
    )
    blue_delta = ImageChops.subtract(blue, green).point(
        [255 if value > 55 else 0 for value in range(256)]
    )
    mask = ImageChops.multiply(above_red, above_blue)
    mask = ImageChops.multiply(mask, below_green)
    mask = ImageChops.multiply(mask, red_delta)
    return ImageChops.multiply(mask, blue_delta)


def remove_edge_background(image: Image.Image) -> Image.Image:
    """Make edge-connected chroma transparent without keying interior pink."""
    rgba = image.convert("RGBA")
    key = magenta_key_mask(rgba)
    remaining = key.copy()
    width, height = key.size

    # Remove every magenta component that touches any outer edge. A single
    # corner seed misses gradients and background islands in several cells.
    for x in range(width):
        if remaining.getpixel((x, 0)):
            ImageDraw.floodfill(remaining, (x, 0), 0, thresh=0)
        if remaining.getpixel((x, height - 1)):
            ImageDraw.floodfill(remaining, (x, height - 1), 0, thresh=0)
    for y in range(1, height - 1):
        if remaining.getpixel((0, y)):
            ImageDraw.floodfill(remaining, (0, y), 0, thresh=0)
        if remaining.getpixel((width - 1, y)):
            ImageDraw.floodfill(remaining, (width - 1, y), 0, thresh=0)

    background = ImageChops.difference(key, remaining)
    alpha = ImageChops.multiply(rgba.getchannel("A"), ImageOps.invert(background))
    rgba.putalpha(alpha)
    return rgba


def transparent_sprite_from_crop(cell: Image.Image, label: str) -> Image.Image:
    image = remove_edge_background(cell)
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"Generated POI sprite {label} has no visible artwork")
    artwork = image.crop(bounds)
    # A few illustrated symbols reach the edge of their generated cell. Fit
    # each one inside a common transparent margin so map labels and halos never
    # touch the sprite boundary or preserve a chroma-key fringe.
    artwork.thumbnail((112, 112), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (TARGET_CELL, TARGET_CELL), (0, 0, 0, 0))
    canvas.alpha_composite(
        artwork,
        ((TARGET_CELL - artwork.width) // 2, (TARGET_CELL - artwork.height) // 2),
    )
    return canvas


def transparent_cell(
    sheet: Image.Image,
    index: int,
    column_bounds: list[int],
    row_bounds: list[int],
) -> Image.Image:
    column = index % SOURCE_COLUMNS
    row = index // SOURCE_COLUMNS
    cell = sheet.crop((
        column_bounds[column], row_bounds[row],
        column_bounds[column + 1], row_bounds[row + 1],
    ))
    return transparent_sprite_from_crop(cell, str(index))


def load_crop_manifest(path: Path = DEFAULT_MANIFEST) -> dict:
    manifest = json.loads(path.read_text())
    expected_worlds = set(THEMES) - set(EXCLUDED_GENERATORS)
    if manifest.get("schema") != 1 or set(manifest.get("worlds", {})) != expected_worlds:
        raise ValueError("POI crop manifest must cover every ImageGen world exactly once")
    if manifest.get("source_dimensions") != list(SOURCE_SIZE) or manifest.get("target_cell_pixels") != TARGET_CELL:
        raise ValueError("POI crop manifest has unexpected source or target dimensions")
    if manifest.get("pillow_version") != "13.0.0":
        raise ValueError("POI crop manifest must record the pinned Pillow renderer")
    excluded = manifest.get("excluded_worlds", {})
    if set(excluded) != set(EXCLUDED_GENERATORS) or any(
        not excluded[world].startswith(script)
        for world, script in EXCLUDED_GENERATORS.items()
    ):
        raise ValueError("POI crop manifest must document the dedicated generators")
    for world, entry in manifest["worlds"].items():
        for field, last, size in (("column_bounds", 1280, 6), ("row_bounds", 2560, 11)):
            bounds = entry.get(field)
            if not isinstance(bounds, list) or len(bounds) != size or bounds[0] != 0 or bounds[-1] != last:
                raise ValueError(f"{world}: invalid {field}")
            if any(not isinstance(value, int) for value in bounds) or any(a >= b for a, b in zip(bounds, bounds[1:])):
                raise ValueError(f"{world}: {field} must increase strictly")
        if len(entry.get("sheet_sha256", "")) != 64:
            raise ValueError(f"{world}: missing source sheet SHA-256")
    return manifest


def destination_marker(source: Image.Image) -> Image.Image:
    image = remove_edge_background(source)
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("Generated destination marker has no visible pixels")
    artwork = image.crop(bounds)
    # Leave clear map-visible padding for collision and selection halos.
    artwork.thumbnail((112, 112), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (TARGET_CELL, TARGET_CELL), (0, 0, 0, 0))
    canvas.alpha_composite(
        artwork,
        ((TARGET_CELL - artwork.width) // 2, (TARGET_CELL - artwork.height) // 2),
    )
    return canvas


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--marker-source-dir", type=Path, default=DEFAULT_MARKER_SOURCE)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--world", action="append", help="Import only this ImageGen world; repeatable")
    parser.add_argument("--poi-only", action="store_true", help="Leave destination marker assets untouched")
    args = parser.parse_args()
    manifest = load_crop_manifest(args.manifest)
    selected = set(args.world or manifest["worlds"])
    for world in sorted(selected & set(EXCLUDED_GENERATORS)):
        raise SystemExit(f"{world} uses {EXCLUDED_GENERATORS[world]}, not the ImageGen atlas importer")
    unknown = selected - set(manifest["worlds"])
    if unknown:
        raise SystemExit(f"Unknown ImageGen world(s): {', '.join(sorted(unknown))}")
    if PILLOW_VERSION != manifest["pillow_version"]:
        raise SystemExit("Source sheet rendering requires tools/imagegen/requirements.txt (Pillow 13.0.0)")
    categories = json.loads((ROOT / "app/src/main/assets/poi_categories.json").read_text())
    category_ids = [category["id"] for category in categories]
    if len(category_ids) != 49 or len(set(category_ids)) != 49:
        raise ValueError("Expected 49 unique POI categories")
    markers = {world: args.marker_source_dir / f"{world}.png" for world in selected}
    if not args.poi_only:
        missing_markers = sorted(world for world, path in markers.items() if not path.is_file())
        if missing_markers:
            raise SystemExit(f"Missing standalone marker artwork for: {missing_markers}")

    POI_DIR.mkdir(parents=True, exist_ok=True)
    if not args.poi_only:
        MARKER_DIR.mkdir(parents=True, exist_ok=True)
    for name in sorted(selected):
        source = args.source_dir / f"{name}.png"
        if not source.is_file() or sha256(source.read_bytes()).hexdigest() != manifest["worlds"][name]["sheet_sha256"]:
            raise SystemExit(f"{name}: source sheet is missing or differs from the reviewed SHA-256")
        with Image.open(source) as opened:
            if opened.size != SOURCE_SIZE:
                raise SystemExit(f"{source.name}: expected {SOURCE_SIZE}, found {opened.size}")
            sheet = opened.convert("RGBA")

        entry = manifest["worlds"][name]
        unknown_overrides = set(entry["overrides"]) - set(category_ids)
        if unknown_overrides:
            raise ValueError(f"{name}: unknown override categories: {sorted(unknown_overrides)}")
        atlas = Image.new("RGBA", (SOURCE_COLUMNS * TARGET_CELL, SOURCE_ROWS * TARGET_CELL), (0, 0, 0, 0))
        for index, category in enumerate(category_ids):
            if category in entry["overrides"]:
                override = OVERRIDE_DIR / f"{name}__{category}.png"
                with Image.open(override) as opened:
                    if opened.size != (TARGET_CELL, TARGET_CELL):
                        raise ValueError(f"Unexpected override dimensions: {override}")
                    sprite = opened.convert("RGBA")
            else:
                sprite = transparent_cell(sheet, index, entry["column_bounds"], entry["row_bounds"])
            atlas.alpha_composite(sprite, (
                index % SOURCE_COLUMNS * TARGET_CELL,
                index // SOURCE_COLUMNS * TARGET_CELL,
            ))
        atlas.save(POI_DIR / f"{name}.png", optimize=True)
        night_atlas_variant(atlas, THEMES[name]).save(POI_DIR / f"{name}_night.png", optimize=True)
        if not args.poi_only:
            with Image.open(markers[name]) as opened:
                if opened.width != opened.height or opened.width < 512:
                    raise SystemExit(
                        f"{markers[name].name}: expected a square marker image at least 512px wide"
                    )
                marker = destination_marker(opened)
            marker.save(MARKER_DIR / f"{name}.png", optimize=True)
            night_variant(marker, THEMES[name]).save(MARKER_DIR / f"{name}_night.png", optimize=True)

    print(f"Imported {len(selected)} reviewed 49-icon atlases" + ("." if args.poi_only else " and destination markers."))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
