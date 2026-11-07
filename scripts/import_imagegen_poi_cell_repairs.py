#!/usr/bin/env python3
"""Apply checked-in POI cell repairs without needing ignored ImageGen sources.

The crop manifest records each override's provenance. This script applies the
tracked 128 px sprites to existing atlas cells and rebuilds night variants.
The standalone-image conversion helper is also used by build_poi_overrides.py.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageChops

from generate_night_icon_variants import night_atlas_variant
from import_imagegen_poi_atlases import DEFAULT_MANIFEST, OVERRIDE_DIR, load_crop_manifest, remove_edge_background
from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
CELL_SIZE = 128
COLUMNS = 5
SOURCE_SIZE = (1024, 1024)

def centered_sprite(source: Image.Image, label: str) -> Image.Image:
    if source.size != SOURCE_SIZE:
        raise ValueError(f"{label}: expected {SOURCE_SIZE}, found {source.size}")
    image = remove_edge_background(source.convert("RGBA"))
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"{label}: no artwork remains after magenta removal")
    artwork = image.crop(bounds)
    artwork.thumbnail((112, 112), Image.Resampling.LANCZOS)
    if artwork.width < 24 or artwork.height < 24:
        raise ValueError(f"{label}: artwork is too small to read at map scale")
    sprite = Image.new("RGBA", (CELL_SIZE, CELL_SIZE), (0, 0, 0, 0))
    sprite.alpha_composite(
        artwork,
        ((CELL_SIZE - artwork.width) // 2, (CELL_SIZE - artwork.height) // 2),
    )
    return sprite


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--override-dir", type=Path, default=OVERRIDE_DIR)
    parser.add_argument("--world", action="append", help="Repair only this ImageGen world; repeatable")
    parser.add_argument("--check", action="store_true", help="Check that existing day/night atlases contain all repairs")
    args = parser.parse_args()
    root = args.root.resolve()
    categories = json.loads((root / "app/src/main/assets/poi_categories.json").read_text())
    ids = [category["id"] for category in categories]
    if len(ids) != 49 or len(set(ids)) != 49:
        raise ValueError("Expected 49 unique POI categories")
    indices = {category: index for index, category in enumerate(ids)}
    manifest = load_crop_manifest(args.manifest)
    selected = set(args.world or manifest["worlds"])
    if selected - set(manifest["worlds"]):
        raise SystemExit(f"Unsupported ImageGen world(s): {sorted(selected - set(manifest['worlds']))}")
    count = 0
    for world in sorted(selected):
        overrides = manifest["worlds"][world]["overrides"]
        if not overrides:
            continue
        atlas_path = root / "app/src/main/assets/poi" / f"{world}.png"
        with Image.open(atlas_path) as opened:
            if opened.size != (COLUMNS * CELL_SIZE, 10 * CELL_SIZE):
                raise ValueError(f"Unexpected POI atlas size: {atlas_path}")
            atlas = opened.convert("RGBA")
        original = atlas.copy()
        unknown = set(overrides) - set(indices)
        if unknown:
            raise ValueError(f"{world}: unknown category override(s): {sorted(unknown)}")
        for category in sorted(overrides, key=indices.__getitem__):
            sprite_path = args.override_dir / f"{world}__{category}.png"
            with Image.open(sprite_path) as opened:
                if opened.size != (CELL_SIZE, CELL_SIZE):
                    raise ValueError(f"Unexpected override size: {sprite_path}")
                sprite = opened.convert("RGBA")
            index = indices[category]
            position = ((index % COLUMNS) * CELL_SIZE, (index // COLUMNS) * CELL_SIZE)
            atlas.paste(sprite, position)
            count += 1
        night = night_atlas_variant(atlas, THEMES[world])
        night_path = atlas_path.with_name(f"{world}_night.png")
        if args.check:
            if ImageChops.difference(original, atlas).getbbox():
                raise ValueError(f"Checked-in day atlas lacks tracked repairs: {atlas_path}")
            with Image.open(night_path) as opened:
                if opened.size != night.size or ImageChops.difference(opened.convert("RGBA"), night).getbbox():
                    raise ValueError(f"Checked-in night atlas differs from repaired day: {night_path}")
        else:
            for path, image in ((atlas_path, atlas), (night_path, night)):
                temp = path.with_name(path.name + ".tmp")
                image.save(temp, format="PNG", optimize=True)
                temp.replace(path)

    print(f"{'Verified' if args.check else 'Applied'} {count} tracked POI repairs across {len(selected)} ImageGen worlds.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
