#!/usr/bin/env python3
"""Convert original ImageGen sheets and world markers into Waypoint assets.

The atlas generator uses a magenta key. Background removal is per-cell and
edge-connected so pink artwork inside a closed badge stays opaque. Destination
markers are generated as their own images because the atlas's last cell can be
cropped by image generation. Pillow is required.
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageOps

from generate_night_icon_variants import night_atlas_variant, night_variant
from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "output/imagegen/poi-atlases-all"
DEFAULT_MARKER_SOURCE = ROOT / "output/imagegen/world-markers"
POI_DIR = ROOT / "app/src/main/assets/poi"
MARKER_DIR = ROOT / "app/src/main/assets/map-markers"
SOURCE_COLUMNS = 5
SOURCE_ROWS = 10
SOURCE_CELL = 256
TARGET_CELL = 128
SOURCE_SIZE = (SOURCE_COLUMNS * SOURCE_CELL, SOURCE_ROWS * SOURCE_CELL)


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


def transparent_cell(sheet: Image.Image, index: int) -> Image.Image:
    column = index % SOURCE_COLUMNS
    row = index // SOURCE_COLUMNS
    cell = sheet.crop((
        column * SOURCE_CELL,
        row * SOURCE_CELL,
        (column + 1) * SOURCE_CELL,
        (row + 1) * SOURCE_CELL,
    ))
    image = remove_edge_background(cell)
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"Generated POI sprite {index} has no visible artwork")
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
    args = parser.parse_args()
    sources = sorted(args.source_dir.glob("*.png"))
    if len(sources) != 38:
        raise SystemExit(f"Expected 38 world atlases in {args.source_dir}, found {len(sources)}")
    markers = {path.stem: path for path in args.marker_source_dir.glob("*.png")}
    if set(markers) != set(THEMES):
        raise SystemExit(
            f"Expected one standalone marker for each of {len(THEMES)} worlds in "
            f"{args.marker_source_dir}; found {len(markers)}"
        )

    POI_DIR.mkdir(parents=True, exist_ok=True)
    MARKER_DIR.mkdir(parents=True, exist_ok=True)
    for source in sources:
        name = source.stem
        if name not in THEMES:
            raise SystemExit(f"Unknown world atlas: {source.name}")
        with Image.open(source) as opened:
            if opened.size != SOURCE_SIZE:
                raise SystemExit(f"{source.name}: expected {SOURCE_SIZE}, found {opened.size}")
            sheet = opened.convert("RGBA")

        atlas = Image.new("RGBA", (SOURCE_COLUMNS * TARGET_CELL, SOURCE_ROWS * TARGET_CELL), (0, 0, 0, 0))
        for index in range(49):
            atlas.alpha_composite(transparent_cell(sheet, index), (
                index % SOURCE_COLUMNS * TARGET_CELL,
                index // SOURCE_COLUMNS * TARGET_CELL,
            ))
        with Image.open(markers[name]) as opened:
            if opened.width != opened.height or opened.width < 512:
                raise SystemExit(
                    f"{markers[name].name}: expected a square marker image at least 512px wide"
                )
            marker = destination_marker(opened)
        atlas.save(POI_DIR / f"{name}.png", optimize=True)
        night_atlas_variant(atlas, THEMES[name]).save(POI_DIR / f"{name}_night.png", optimize=True)
        marker.save(MARKER_DIR / f"{name}.png", optimize=True)
        night_variant(marker, THEMES[name]).save(MARKER_DIR / f"{name}_night.png", optimize=True)

    print(f"Imported {len(sources)} transparent 49-icon atlases and 38 destination markers (day/night variants).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
