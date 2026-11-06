#!/usr/bin/env python3
"""Replace visibly contaminated POI atlas cells with isolated original icons.

ImageGen sheets sometimes place neighboring artwork inside a category crop.
This importer accepts one standalone high-resolution image per affected cell,
removes only edge-connected magenta, and centers the result in its atlas slot.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image

from generate_night_icon_variants import night_atlas_variant
from import_imagegen_poi_atlases import remove_edge_background
from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "output/imagegen/poi-cell-repairs"
DEFAULT_PROMPTS = ROOT / "tools/imagegen/poi-cell-repairs.jsonl"
POI_DIR = ROOT / "app/src/main/assets/poi"
LANDING_POI_DIR = ROOT / "docs/assets/landing/poi"
CELL_SIZE = 128
COLUMNS = 5
SOURCE_SIZE = (1024, 1024)

# These are the category cells visibly affected by overlap in the original
# atlas sheets. Indices follow poi_categories.json, so the source registry is
# still the authority for what each symbol represents.
REPAIR_INDICES = {
    "far_cry": (38, 39, 43, 44, 47, 48),
    "field_ops": (48,),
    "home_console": (24, 29, 33, 34, 38, 39, 44),
    "horizon": (48,),
    "kingdom": (47, 48),
    "pixel": (39, 44),
    "saints": (19, 33, 34, 39, 44, 48),
    "sea_chart": (19, 24, 29, 34, 39, 43, 44),
    "vice_coast": (19, 24, 29, 34, 38, 39, 42, 43, 44, 47, 48),
}


def load_jobs(path: Path) -> dict[str, dict[str, str]]:
    jobs = [json.loads(line) for line in path.read_text().splitlines() if line.strip()]
    indexed = {Path(job["out"]).stem: job for job in jobs}
    if len(indexed) != len(jobs):
        raise ValueError("Repair prompt outputs must be unique.")
    return indexed


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


def expected_outputs(categories: list[dict]) -> dict[str, tuple[str, int]]:
    outputs: dict[str, tuple[str, int]] = {}
    for world, indices in REPAIR_INDICES.items():
        for index in indices:
            category_id = categories[index]["id"]
            key = f"{world}__{category_id}"
            if key in outputs:
                raise ValueError(f"Duplicate repair target: {key}")
            outputs[key] = (world, index)
    return outputs


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--prompts", type=Path, default=DEFAULT_PROMPTS)
    args = parser.parse_args()
    root = args.root.resolve()
    source_dir = args.source_dir.resolve()
    categories = json.loads((root / "app/src/main/assets/poi_categories.json").read_text())
    jobs = load_jobs(args.prompts.resolve())
    targets = expected_outputs(categories)
    if set(jobs) != set(targets):
        missing = sorted(set(targets) - set(jobs))
        extra = sorted(set(jobs) - set(targets))
        raise ValueError(f"Repair prompt coverage mismatch; missing={missing}, extra={extra}")
    if len(targets) != 43:
        raise ValueError(f"Expected 43 audited cells, found {len(targets)}")
    missing_files = sorted(key for key in targets if not (source_dir / f"{key}.png").is_file())
    if missing_files:
        raise ValueError(f"Missing generated cell artwork: {missing_files}")

    staged: dict[str, tuple[Image.Image, Image.Image]] = {}
    for key, (world, index) in targets.items():
        with Image.open(source_dir / f"{key}.png") as opened:
            opened.load()
            sprite = centered_sprite(opened, key)
        atlas_path = root / f"app/src/main/assets/poi/{world}.png"
        with Image.open(atlas_path) as opened:
            opened.load()
            if opened.size != (COLUMNS * CELL_SIZE, 10 * CELL_SIZE):
                raise ValueError(f"Unexpected POI atlas size: {atlas_path}")
            atlas = opened.convert("RGBA")
        position = ((index % COLUMNS) * CELL_SIZE, (index // COLUMNS) * CELL_SIZE)
        atlas.paste(sprite, position)
        # Keep a single staged day atlas per world; later cells for that world
        # are applied to the staged image rather than overwriting earlier fixes.
        previous = staged.get(world)
        if previous is not None:
            previous[0].paste(sprite, position)
            atlas = previous[0]
        night = night_atlas_variant(atlas, THEMES[world])
        staged[world] = (atlas, night)

    poi_dir = root / "app/src/main/assets/poi"
    landing_dir = root / "docs/assets/landing/poi"
    for world, (day, night) in staged.items():
        for suffix, image in (("", day), ("_night", night)):
            target = poi_dir / f"{world}{suffix}.png"
            temp = target.with_name(target.name + ".tmp")
            image.save(temp, format="PNG", optimize=True)
            temp.replace(target)
            landing = landing_dir / f"{world}{suffix}.png"
            if landing_dir.is_dir() and landing.exists():
                landing.write_bytes(target.read_bytes())

    print(f"Imported 43 isolated POI icons across {len(staged)} world atlases and refreshed night variants.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
