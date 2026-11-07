#!/usr/bin/env python3
"""Build Night City's sparse map icons from seven verified Cyberpunk glyphs.

The remaining OpenStreetMap categories, destination target, and player caret
are Waypoint artwork. Source PNGs and their immutable hashes are recorded in
third_party/cyberpunk_map_icons/sources.json. No network access is needed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from generate_map_assets import Raster
from map_glyphs import draw_category_glyph


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SOURCES = ROOT / "third_party/cyberpunk_map_icons"
PREVIEW_DAY = ROOT / "docs/assets/poi-audit/night-city-icons.webp"
PREVIEW_NIGHT = ROOT / "docs/assets/poi-audit/night-city-icons-night.webp"
CATEGORIES = json.loads((ASSETS / "poi_categories.json").read_text())
CELL = 128
COLS = 5
ATLAS_SIZE = (COLS * CELL, 10 * CELL)
NATIVE_FOR_CATEGORY = {
    "place": "place_of_interest",
    "restaurant": "food",
    "fast_food": "food",
    "pub": "bar",
    "bar": "bar",
    "clothing": "clothing",
    "hospital": "medpoint",
    "clinic": "medpoint",
    "monument": "landmark",
    "police": "ncpd",
}


def verified_sources() -> dict[str, Image.Image]:
    manifest = json.loads((SOURCES / "sources.json").read_text())
    if set(manifest) != set(NATIVE_FOR_CATEGORY.values()):
        raise ValueError("Cyberpunk source manifest does not match the semantic mapping")
    images = {}
    for name, entry in manifest.items():
        path = SOURCES / f"{name}.png"
        data = path.read_bytes()
        if (hashlib.sha256(data).hexdigest() != entry["sha256"]
                or hashlib.sha1(data).hexdigest() != entry["source_sha1"]
                or len(data) != entry["size"]):
            raise ValueError(f"Cyberpunk source hash changed: {path}")
        with Image.open(path) as image:
            if image.size != (64, 64):
                raise ValueError(f"Cyberpunk source size changed: {path}")
            images[name] = image.convert("RGBA")
    return images


def original_glyph(kind: str) -> Image.Image:
    raster = Raster(CELL, CELL)
    draw_category_glyph(raster, kind, (246, 249, 248, 255), (42, 205, 221, 255), "night_city")
    return Image.frombytes("RGBA", (CELL, CELL), bytes(raster.pixels))


def centered_glyph(source: Image.Image) -> Image.Image:
    bounds = source.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("Empty Night City glyph")
    cropped = source.crop(bounds)
    scale = min(80 / cropped.width, 80 / cropped.height)
    size = (round(cropped.width * scale), round(cropped.height * scale))
    fitted = cropped.resize(size, Image.Resampling.LANCZOS)
    cell = Image.new("RGBA", (CELL, CELL))
    cell.alpha_composite(fitted, ((CELL - size[0]) // 2, (CELL - size[1]) // 2))
    return cell


def with_night_glow(icon: Image.Image) -> Image.Image:
    alpha = icon.getchannel("A").filter(ImageFilter.GaussianBlur(5))
    alpha = alpha.point(lambda value: round(value * 0.24))
    glow = Image.new("RGBA", icon.size, (46, 219, 234))
    glow.putalpha(alpha)
    glow.alpha_composite(icon)
    return glow


def build_atlas(sources: dict[str, Image.Image], night: bool) -> Image.Image:
    atlas = Image.new("RGBA", ATLAS_SIZE)
    for index, category in enumerate(CATEGORIES):
        kind = category["id"]
        source = sources[NATIVE_FOR_CATEGORY[kind]] if kind in NATIVE_FOR_CATEGORY else original_glyph(kind)
        icon = centered_glyph(source)
        if night:
            icon = with_night_glow(icon)
        atlas.alpha_composite(icon, ((index % COLS) * CELL, (index // COLS) * CELL))
    return atlas


def at_scale(points: list[tuple[int, int]], scale: int) -> list[tuple[int, int]]:
    return [(x * scale, y * scale) for x, y in points]


def build_destination(night: bool) -> Image.Image:
    scale = 4
    image = Image.new("RGBA", (CELL * scale, CELL * scale))
    draw = ImageDraw.Draw(image)
    diamond = at_scale([(64, 17), (107, 64), (64, 111), (21, 64), (64, 17)], scale)
    ink = (7, 17, 23, 255)
    gold = (255, 226, 81, 255) if night else (250, 210, 45, 255)
    draw.line(diamond, fill=ink, width=12 * scale, joint="curve")
    draw.line(diamond, fill=gold, width=5 * scale, joint="curve")
    draw.ellipse((58 * scale, 58 * scale, 70 * scale, 70 * scale), fill=ink)
    draw.ellipse((61 * scale, 61 * scale, 67 * scale, 67 * scale), fill=gold)
    marker = image.resize((CELL, CELL), Image.Resampling.LANCZOS)
    return with_night_glow(marker) if night else marker


def build_player() -> Image.Image:
    """An upward-pointing Night City caret shared by every travel mode."""
    scale = 4
    image = Image.new("RGBA", (96 * scale, 104 * scale))
    draw = ImageDraw.Draw(image)
    caret = at_scale([(22, 80), (48, 27), (74, 80)], scale)
    for width, color in (
        (20, (5, 17, 24, 255)),
        (12, (38, 212, 227, 255)),
        (3, (170, 255, 250, 255)),
    ):
        draw.line(caret, fill=color, width=width * scale, joint="curve")
        radius = width * scale // 2
        for x, y in (caret[0], caret[-1]):
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)
    return image.resize((96, 104), Image.Resampling.LANCZOS)


def build_preview(sources: dict[str, Image.Image], player: Image.Image, destination: Image.Image,
                  night: bool) -> Image.Image:
    width = 1120
    row_height = 142
    top = 190
    preview = Image.new("RGB", (width, top + 7 * row_height + 20),
                        (9, 19, 27) if night else (13, 27, 36))
    draw = ImageDraw.Draw(preview)
    font = ImageFont.load_default()
    draw.text((28, 22), f"NIGHT CITY | PLACE ICONS | {'NIGHT' if night else 'DAY'}",
              fill=(245, 248, 248), font=font)
    draw.text((28, 45), "Seven Cyberpunk 2077 glyphs, mapped to ten direct OSM matches; 39 original glyphs.",
              fill=(161, 188, 192), font=font)
    preview.paste(player, (38, 75), player)
    preview.paste(destination, (174, 69), destination)
    draw.text((42, 164), "PLAYER | ALL MODES", fill=(79, 222, 230), font=font)
    draw.text((200, 164), "DESTINATION", fill=(247, 220, 94), font=font)
    for index, category in enumerate(CATEGORIES):
        x = 22 + (index % 7) * 158
        y = top + (index // 7) * row_height
        draw.rounded_rectangle((x, y, x + 150, y + 132), radius=8, fill=(21, 43, 54), outline=(48, 78, 86))
        source = sources[NATIVE_FOR_CATEGORY[category["id"]]] if category["id"] in NATIVE_FOR_CATEGORY else original_glyph(category["id"])
        icon = centered_glyph(source)
        if night:
            icon = with_night_glow(icon)
        preview.paste(icon, (x + 11, y + 1), icon)
        label = category["id"].replace("_", " ").upper()
        draw.text((x + 12, y + 106), label, fill=(245, 249, 249), font=font)
        draw.text((x + 107, y + 9), "CP" if category["id"] in NATIVE_FOR_CATEGORY else "WP",
                  fill=(71, 229, 232) if category["id"] in NATIVE_FOR_CATEGORY else (139, 169, 175), font=font)
    return preview


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify the checked-in runtime assets without rewriting them")
    args = parser.parse_args()
    if len(CATEGORIES) != 49:
        raise ValueError("Night City expects the 49-category atlas registry")
    sources = verified_sources()
    poi_dir = ASSETS / "poi"
    marker_dir = ASSETS / "map-markers"
    player_dir = ASSETS / "player-markers"
    player = build_player()
    destination_day = build_destination(False)
    destination_night = build_destination(True)
    runtime_images = {
        poi_dir / "night_city.png": build_atlas(sources, False),
        poi_dir / "night_city_night.png": build_atlas(sources, True),
        marker_dir / "night_city.png": destination_day,
        marker_dir / "night_city_night.png": destination_night,
        player_dir / "night_city.png": player,
    }
    if args.check:
        for path, expected in runtime_images.items():
            with Image.open(path) as opened:
                actual = opened.convert("RGBA")
            if actual.size != expected.size or actual.tobytes() != expected.tobytes():
                raise ValueError(f"Night City runtime asset is out of date: {path}")
        print("Verified Night City runtime assets against the source manifest and generator")
        return
    player_dir.mkdir(exist_ok=True)
    for path, image in runtime_images.items():
        image.save(path, optimize=True)
    build_preview(sources, player, destination_day, False).save(PREVIEW_DAY, quality=92, method=6)
    build_preview(sources, player, destination_night, True).save(PREVIEW_NIGHT, quality=92, method=6)
    print("Generated Night City day/night POI atlases, destination targets, player caret, and previews")


if __name__ == "__main__":
    main()
