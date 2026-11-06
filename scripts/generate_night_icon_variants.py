#!/usr/bin/env python3
"""Create readable, theme-colored night variants from each original day atlas.

The vector silhouettes and pictograms stay intact. A per-world hue shift uses
the night cartography palette, keeping all 49 live-place symbols and the
destination marker legible against each world’s night map.
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter

from generate_map_assets import palette_for
from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
POI_DIR = ROOT / "app/src/main/assets/poi"
MARKER_DIR = ROOT / "app/src/main/assets/map-markers"


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.lstrip("#")
    return tuple(int(value[offset:offset + 2], 16) for offset in (0, 2, 4))


def blend(first: str, second: str, amount: float) -> str:
    left, right = rgb(first), rgb(second)
    return "#" + "".join(f"{round(a * (1 - amount) + b * amount):02x}" for a, b in zip(left, right))


def night_variant(source: Image.Image, theme: dict[str, str]) -> Image.Image:
    """Darken quiet surfaces while keeping colored symbols vivid and legible."""
    rgba = source.convert("RGBA")
    palette = palette_for(theme, night=True)
    hue, saturation, value = rgba.convert("RGB").convert("HSV").split()
    alpha = rgba.getchannel("A")
    target_hue, target_saturation, _ = Image.new("RGB", (1, 1), rgb(palette["outline"])).convert("HSV").getpixel((0, 0))
    if target_saturation < 24:
        target_hue = Image.new("RGB", (1, 1), rgb(palette["road"])).convert("HSV").getpixel((0, 0))[0]

    chromatic = saturation.point(lambda channel: max(0, min(255, (channel - 28) * 4)))
    visible = alpha.point(lambda channel: 255 if channel >= 32 else 0)
    histogram = hue.histogram(mask=ImageChops.multiply(chromatic, visible))
    dominant_hue = max(range(256), key=histogram.__getitem__) if sum(histogram) else target_hue
    hue_delta = (target_hue - dominant_hue + 128) % 256 - 128
    hue_shift = round(hue_delta * 0.32) + 4
    hue = hue.point([(channel + hue_shift) % 256 for channel in range(256)])
    saturation = saturation.point([min(255, round(channel * 1.12 + 4)) for channel in range(256)])

    darkened = value.point([round(255 * (channel / 255) ** 1.30) for channel in range(256)])
    luminous = value.point([min(255, round(255 * (channel / 255) ** 0.84 + 8)) for channel in range(256)])
    accent_mask = saturation.point([max(0, min(255, (channel - 28) * 4)) for channel in range(256)])
    bright_mask = value.point([max(0, min(255, (channel - 56) * 3)) for channel in range(256)])
    accent_mask = ImageChops.multiply(accent_mask, bright_mask)
    accent_mask = ImageChops.multiply(accent_mask, visible)
    value = Image.composite(luminous, darkened, accent_mask)

    rgb_art = Image.merge("HSV", (hue, saturation, value)).convert("RGB")
    result = rgb_art.convert("RGBA")
    result.putalpha(alpha)

    profile = theme["icon_profile"]
    glow_strength = 0.20 if profile in {"neon_circuit", "phosphor_grid", "amber_panel", "pda_bracket", "chiral_node"} else 0.07
    glow = accent_mask.filter(ImageFilter.GaussianBlur(radius=3.0))
    glow = glow.point(lambda channel: round(channel * glow_strength))
    glow_color = blend(palette["outline"], palette["label"], 0.45)
    glow_layer = Image.new("RGBA", rgba.size, rgb(glow_color) + (0,))
    glow_layer.putalpha(glow)
    result = Image.alpha_composite(glow_layer, result)
    return result


def night_atlas_variant(source: Image.Image, theme: dict[str, str], cell_size: int = 128) -> Image.Image:
    """Tint each sprite independently so its glow cannot enter a neighbor cell."""
    rgba = source.convert("RGBA")
    if rgba.width <= cell_size or rgba.height <= cell_size:
        return night_variant(rgba, theme)
    if rgba.width % cell_size or rgba.height % cell_size:
        raise ValueError(f"Sprite atlas dimensions must be multiples of {cell_size}: {rgba.size}")
    atlas = Image.new("RGBA", rgba.size, (0, 0, 0, 0))
    for top in range(0, rgba.height, cell_size):
        for left in range(0, rgba.width, cell_size):
            cell = rgba.crop((left, top, left + cell_size, top + cell_size))
            atlas.alpha_composite(night_variant(cell, theme), (left, top))
    return atlas


def generate(root: Path) -> int:
    poi_dir = root / "app/src/main/assets/poi"
    marker_dir = root / "app/src/main/assets/map-markers"
    for name, theme in THEMES.items():
        pairs = (
            (poi_dir / f"{name}.png", poi_dir / f"{name}_night.png"),
            (marker_dir / f"{name}.png", marker_dir / f"{name}_night.png"),
        )
        for source_path, output_path in pairs:
            if not source_path.is_file():
                raise SystemExit(f"Missing original day artwork: {source_path}")
            with Image.open(source_path) as source:
                source.load()
                if source.mode not in {"RGBA", "RGB"}:
                    raise SystemExit(f"{source_path.name}: expected RGB or RGBA artwork, got {source.mode}")
            if source_path.parent.name == "poi":
                night_atlas_variant(source, theme).save(output_path, optimize=True)
            else:
                night_variant(source, theme).save(output_path, optimize=True)
    print(f"Generated night palettes for {len(THEMES)} worlds: 38 POI atlases and 38 destination markers.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    return generate(args.root.resolve())


if __name__ == "__main__":
    raise SystemExit(main())
