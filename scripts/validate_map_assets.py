#!/usr/bin/env python3
"""Check generated Waypoint map styles and original PNG assets."""

from __future__ import annotations

import json
from hashlib import sha256
import struct
import sys
from pathlib import Path

from PIL import Image

from map_theme_catalog import THEMES

REPOSITORY = Path(__file__).resolve().parents[1]
ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else REPOSITORY
ASSETS = ROOT / "app/src/main/assets"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
CATEGORIES = json.loads((ASSETS / "poi_categories.json").read_text())
ATLAS_COLUMNS = 5
ATLAS_WIDTH = ATLAS_COLUMNS * 128
ATLAS_HEIGHT = ((len(CATEGORIES) + ATLAS_COLUMNS - 1) // ATLAS_COLUMNS) * 128


def dimensions(path: Path) -> tuple[int, int]:
    if not path.is_file() or path.is_symlink():
        raise ValueError(f"Missing regular asset: {path}")
    if path.stat().st_size > 4 * 1024 * 1024:
        raise ValueError(f"Unexpectedly large generated asset: {path}")
    with path.open("rb") as file:
        header = file.read(24)
    if len(header) != 24 or header[:8] != PNG_SIGNATURE:
        raise ValueError(f"Invalid PNG header: {path}")
    length, kind, width, height = struct.unpack(">I4sII", header[8:24])
    if length != 13 or kind != b"IHDR":
        raise ValueError(f"Invalid PNG IHDR: {path}")
    return width, height


def digest(path: Path) -> bytes:
    return sha256(path.read_bytes()).digest()


def edge_has_key_color(pixels: Image.Image) -> bool:
    width, height = pixels.size
    border = (
        [pixels.getpixel((x, y)) for x in range(width) for y in (0, height - 1)]
        + [pixels.getpixel((x, y)) for y in range(1, height - 1) for x in (0, width - 1)]
    )
    return any(
        alpha > 128 and red > 200 and green < 90 and blue > 160
        for red, green, blue, alpha in border
    )


def validate_transparent_sprite(
    image_source: Image.Image | Path,
    label: str,
    minimum_visible: int,
    safe_edge: int = 8,
) -> None:
    if isinstance(image_source, Image.Image):
        image = image_source.convert("RGBA")
    else:
        with Image.open(image_source) as opened:
            image = opened.convert("RGBA")
    alpha = image.getchannel("A")
    width, height = image.size
    if any(alpha.getpixel(point) != 0 for point in ((0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1))):
        raise ValueError(f"Sprite lacks transparent outer corners: {label}")
    bounds = alpha.getbbox()
    if bounds is not None and (
        bounds[0] < safe_edge or bounds[1] < safe_edge
        or width - bounds[2] < safe_edge or height - bounds[3] < safe_edge
    ):
        raise ValueError(f"Sprite artwork enters the {safe_edge}px safe edge: {label} bounds={bounds}")
    if sum(alpha.histogram()[17:]) < minimum_visible:
        raise ValueError(f"Sprite artwork is empty or too small: {label}")
    if edge_has_key_color(image):
        raise ValueError(f"Opaque magenta key color remains at an outer edge: {label}")


def validate_poi_atlas(path: Path) -> None:
    with Image.open(path) as opened:
        atlas = opened.convert("RGBA")
    for index in range(len(CATEGORIES)):
        column, row = index % ATLAS_COLUMNS, index // ATLAS_COLUMNS
        sprite = atlas.crop((column * 128, row * 128, (column + 1) * 128, (row + 1) * 128))
        # Night atlas glow is rendered per-cell and intentionally reaches the
        # tile edge without being able to enter a neighboring sprite.
        edge = 0 if path.stem.endswith("_night") else 8
        validate_transparent_sprite(sprite, f"{path.name} category {index}", 300, safe_edge=edge)
    # The 50th grid slot is intentionally empty: the destination gets its own
    # full-resolution marker asset and cannot leak into the POI atlas.
    unused = atlas.crop((4 * 128, 9 * 128, 5 * 128, 10 * 128))
    if unused.getchannel("A").getbbox() is not None:
        raise ValueError(f"The unused 50th POI cell must be transparent: {path}")


def validate() -> None:
    themes = set(THEMES)
    if len(CATEGORIES) != 49 or len({category["id"] for category in CATEGORIES}) != len(CATEGORIES):
        raise ValueError("The POI registry must contain 49 unique category sprites")
    profiles = {theme["icon_profile"] for theme in THEMES.values()}
    if len(profiles) != len(themes):
        raise ValueError("Each world must use its own icon profile")
    material_dir = ASSETS / "map-materials"
    poi_dir = ASSETS / "poi"
    marker_dir = ASSETS / "map-markers"
    style_dir = ASSETS / "styles"
    expected_materials = {f"{name}_{phase}.png" for name in themes for phase in ("day", "night")}
    expected_poi = {f"{name}{suffix}.png" for name in themes for suffix in ("", "_night")}
    expected_markers = expected_poi
    expected_styles = {f"{name}{suffix}.json" for name in themes for suffix in ("", "_night")}
    expected_styles.update(("division_holo.json", "division_holo_night.json"))

    for name in expected_materials:
        if dimensions(material_dir / name) != (128, 128):
            raise ValueError(f"Unexpected material dimensions: {name}")
    for name in expected_poi:
        if dimensions(poi_dir / name) != (ATLAS_WIDTH, ATLAS_HEIGHT):
            raise ValueError(f"Unexpected icon atlas dimensions: {name}")
        validate_poi_atlas(poi_dir / name)
    for name in expected_markers:
        if dimensions(marker_dir / name) != (128, 128):
            raise ValueError(f"Unexpected destination marker dimensions: {name}")
        edge = 0 if name.endswith("_night.png") else 8
        validate_transparent_sprite(marker_dir / name, name, 300, safe_edge=edge)
    for suffix in ("", "_night"):
        digests = {digest(poi_dir / f"{name}{suffix}.png") for name in themes}
        if len(digests) != len(themes):
            raise ValueError(f"The {suffix or 'day'} icon atlases are not unique per world")
    for index, category in enumerate(CATEGORIES):
        cell_digests = set()
        for name in themes:
            with Image.open(poi_dir / f"{name}.png") as opened:
                cell = opened.crop((
                    index % ATLAS_COLUMNS * 128,
                    index // ATLAS_COLUMNS * 128,
                    (index % ATLAS_COLUMNS + 1) * 128,
                    (index // ATLAS_COLUMNS + 1) * 128,
                )).convert("RGBA")
                cell_digests.add(sha256(cell.tobytes()).digest())
        if len(cell_digests) != len(themes):
            raise ValueError(f"Category {category['id']} reuses a sprite across worlds")
    for suffix in ("", "_night"):
        digests = {digest(marker_dir / f"{name}{suffix}.png") for name in themes}
        if len(digests) != len(themes):
            raise ValueError(f"The {suffix or 'day'} destination markers are not unique per world")
    for name in themes:
        if digest(poi_dir / f"{name}.png") == digest(poi_dir / f"{name}_night.png"):
            raise ValueError(f"Day and night POI atlases are identical: {name}")
        if digest(marker_dir / f"{name}.png") == digest(marker_dir / f"{name}_night.png"):
            raise ValueError(f"Day and night destination markers are identical: {name}")

    for name in expected_styles:
        path = style_dir / name
        if not path.is_file() or path.is_symlink():
            raise ValueError(f"Missing regular style: {path}")
        style = json.loads(path.read_text())
        metadata = style.get("metadata", {})
        if metadata.get("waypoint:authored") is not True:
            raise ValueError(f"Style is not Waypoint-authored: {name}")
        if "sprite" in style:
            raise ValueError(f"Unexpected preset sprite reference: {name}")
        if name.startswith("division_holo") and metadata.get("waypoint:holographic") is not True:
            raise ValueError(f"Missing hologram metadata: {name}")

    actual_materials = {p.name for p in material_dir.iterdir() if p.is_file()}
    actual_poi = {p.name for p in poi_dir.iterdir() if p.is_file()}
    actual_markers = {p.name for p in marker_dir.iterdir() if p.is_file()}
    actual_styles = {p.name for p in style_dir.iterdir() if p.is_file()}
    if actual_materials != expected_materials:
        raise ValueError(f"Material coverage mismatch: {len(actual_materials)} files")
    if actual_poi != expected_poi:
        raise ValueError(f"POI atlas coverage mismatch: {len(actual_poi)} files")
    if actual_markers != expected_markers:
        raise ValueError(f"Destination marker coverage mismatch: {len(actual_markers)} files")
    if actual_styles != expected_styles:
        raise ValueError(f"Style coverage mismatch: {len(actual_styles)} files")
    print(
        f"Verified {len(themes)} worlds, {len(expected_materials)} materials, "
        f"{len(expected_poi)} themed POI atlases, {len(expected_markers)} destination markers, "
        f"and {len(expected_styles)} authored styles; every category sprite and night atlas/marker "
        "is unique per world."
    )


if __name__ == "__main__":
    validate()
