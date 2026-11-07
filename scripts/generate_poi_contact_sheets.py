#!/usr/bin/env python3
"""Render paired day/night POI atlas contact sheets for visual review."""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from map_theme_catalog import THEMES

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/poi"
DEFAULT_OUTPUT = ROOT / "docs/assets/poi-audit"
WORLD_WIDTH = 660
WORLD_HEIGHT = 674
SCALE = (320, 640)
PER_SHEET = 6


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    font = ImageFont.load_default(size=16)
    worlds = sorted(THEMES)
    for sheet_index, start in enumerate(range(0, len(worlds), PER_SHEET), 1):
        batch = worlds[start:start + PER_SHEET]
        sheet = Image.new("RGBA", (WORLD_WIDTH * 3, WORLD_HEIGHT * ((len(batch) + 2) // 3)), "#303236")
        drawer = ImageDraw.Draw(sheet)
        for position, world in enumerate(batch):
            left = (position % 3) * WORLD_WIDTH
            top = (position // 3) * WORLD_HEIGHT
            drawer.text((left + 8, top + 6), f"{world}    day | night", font=font, fill="#f5f5f5")
            for phase, x_offset in (("", 8), ("_night", 332)):
                with Image.open(SOURCE / f"{world}{phase}.png") as opened:
                    atlas = opened.convert("RGBA").resize(SCALE, Image.Resampling.LANCZOS)
                sheet.alpha_composite(atlas, (left + x_offset, top + 28))
        destination = args.output_dir / f"contact-{sheet_index:02d}.webp"
        sheet.convert("RGB").save(destination, format="WEBP", quality=91, method=6)
    print(f"Rendered {len(worlds)} day/night POI atlas pairs across {sheet_index} contact sheets.")


if __name__ == "__main__":
    main()
