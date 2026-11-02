"""Turn imagegen's 4x3 world icon sheets into small transparent map atlases.

Source imagegen sheets belong in the ignored local source-poi-sheets folder.
The generated WebP atlases are Android assets and can also supply onboarding stickers.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/waypoint-motion/source-poi-sheets"
DESTINATION = ROOT / "app/src/main/assets/poi"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
NATIVE = ROOT / "tools/waypoint-motion/source-blips"
KINDS = ("place", "food", "cafe", "pub", "stay", "fuel", "parking", "shop", "sights", "health", "nature", "cash")
WORLDS = (
    "los_santos", "old_west", "night_city", "pixel", "green_terminal",
    "san_andreas", "heat", "rivia", "vice_coast", "liberty", "ctos",
    "hyrule", "skyrim", "lands_between", "horizon", "assassin",
    "far_cry", "ghost", "bridges", "horizon_wilds", "spider",
    "hogwarts", "starfield", "kingdom", "saints", "just_cause", "mafia",
)
ONBOARDING_WORLD = {
    "place": "los_santos", "food": "los_santos", "cafe": "vice_coast",
    "pub": "old_west", "stay": "skyrim", "fuel": "night_city",
    "parking": "horizon", "shop": "ctos", "sights": "lands_between",
    "health": "green_terminal", "nature": "hyrule", "cash": "san_andreas",
}
GTA_V_BLIPS = {
    "place": 8, "pub": 93, "stay": 40, "parking": 357,
    "shop": 52, "sights": 85, "health": 61, "cash": 272,
}
RDR2_BLIPS = {
    "place", "food", "pub", "stay", "parking", "shop",
    "sights", "health", "nature", "cash",
}
NIGHT_CITY_BLIPS = {
    "place", "food", "pub", "stay", "parking", "shop",
    "sights", "health", "cash",
}
SAN_ANDREAS_BLIPS = {
    "place", "food", "cafe", "pub", "stay", "parking",
    "shop", "sights", "health", "cash", "education",
}
MINECRAFT_BLIPS = {"place": 0, "stay": 14, "sights": 4, "nature": 15}
WITCHER_BLIPS = {
    "place": "poi", "pub": "tavern", "shop": "merchant",
    "sights": "notice", "health": "herbalist",
}
ELDEN_BLIPS = {
    "place": "Site_of_Grace", "stay": "Shack", "shop": "Village",
    "sights": "Tower", "nature": "Stamp_Tree",
}
HYRULE_BLIPS = {
    "place": "objective", "sights": "shrine",
    "nature": "korok",
}


def icon_from_cell(cell: Image.Image, pixelated: bool, tile_size: int = 128) -> Image.Image:
    rgb = np.asarray(cell.convert("RGB"), dtype=np.int16)
    red, green, blue = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    key = (red > 145) & (blue > 135) & (green < 120) & (np.abs(red - blue) < 110)
    marker = Image.fromarray(np.where(key, 255, 0).astype("uint8"), "L").copy()
    for corner in ((0, 0), (cell.width - 1, 0), (0, cell.height - 1), (cell.width - 1, cell.height - 1)):
        if marker.getpixel(corner) == 255:
            ImageDraw.floodfill(marker, corner, 128, thresh=0)
    strict = (red > 210) & (blue > 210) & (green < 70) & (np.abs(red - blue) < 55)
    background = (np.asarray(marker) == 128) | strict
    alpha = Image.fromarray(np.where(background, 0, 255).astype("uint8"), "L")
    if not pixelated:
        alpha = alpha.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(.4))
    cutout = cell.convert("RGBA")
    cutout.putalpha(alpha)
    box = cutout.getbbox()
    if box is None:
        raise ValueError("Empty icon cell in generated sheet")
    cutout = cutout.crop(box)
    max_icon = int(tile_size * .875)
    cutout.thumbnail((max_icon, max_icon), Image.Resampling.NEAREST if pixelated else Image.Resampling.LANCZOS)
    tile = Image.new("RGBA", (tile_size, tile_size), (0, 0, 0, 0))
    tile.alpha_composite(cutout, ((tile_size - cutout.width) // 2, (tile_size - cutout.height) // 2))
    return tile


def clean_source_cell(cell: Image.Image, world: str) -> Image.Image:
    if world != "hyrule":
        return cell
    # The model added gold corner framing to this sheet. Its icons are cyan,
    # so remove gold hues before matting to keep map blips free of stray bits.
    rgb = np.asarray(cell.convert("RGB")).copy()
    red, green, blue = (rgb[:, :, channel].astype(np.int16) for channel in range(3))
    gold = (red > 130) & (green > 65) & (red > blue + 30) & (green > blue * .68)
    rgb[gold] = (255, 0, 255)
    return Image.fromarray(rgb, "RGB")


def native_icon(world: str, kind: str, tile_size: int = 128) -> Image.Image | None:
    if world == "los_santos" and kind in GTA_V_BLIPS:
        image = Image.open(NATIVE / "gtav" / f"{GTA_V_BLIPS[kind]}.webp").convert("RGBA")
        sampling = Image.Resampling.LANCZOS
    elif world == "old_west" and kind in RDR2_BLIPS:
        image = Image.open(NATIVE / "rdr2" / f"{kind}.png").convert("RGBA")
        sampling = Image.Resampling.LANCZOS
    elif world == "night_city" and kind in NIGHT_CITY_BLIPS:
        image = Image.open(NATIVE / "night_city" / f"{kind}.png").convert("RGBA")
        sampling = Image.Resampling.LANCZOS
    elif world == "san_andreas" and kind in SAN_ANDREAS_BLIPS:
        image = Image.open(NATIVE / "san_andreas" / f"{kind}.png").convert("RGBA")
        sampling = Image.Resampling.NEAREST
    elif world == "pixel" and kind in MINECRAFT_BLIPS:
        atlas = Image.open(NATIVE / "minecraft-map-icons.png").convert("RGBA")
        index = MINECRAFT_BLIPS[kind]
        image = atlas.crop(((index % 4) * 16, (index // 4) * 16,
                            ((index % 4) + 1) * 16, ((index // 4) + 1) * 16))
        sampling = Image.Resampling.NEAREST
    elif world == "rivia" and kind in WITCHER_BLIPS:
        image = Image.open(NATIVE / "rivia" / f"{WITCHER_BLIPS[kind]}.png").convert("RGBA")
        sampling = Image.Resampling.NEAREST
    elif world == "lands_between" and kind in ELDEN_BLIPS:
        image = Image.open(NATIVE / "lands_between" / f"{ELDEN_BLIPS[kind]}.png").convert("RGBA")
        sampling = Image.Resampling.LANCZOS
    elif world == "hyrule" and kind in HYRULE_BLIPS:
        image = Image.open(NATIVE / "hyrule" / f"{HYRULE_BLIPS[kind]}.png").convert("RGBA")
        sampling = Image.Resampling.NEAREST
    else:
        return None
    max_icon = int(tile_size * .75)
    scale = min(max_icon / image.width, max_icon / image.height)
    image = image.resize((round(image.width * scale), round(image.height * scale)), sampling)
    tile = Image.new("RGBA", (tile_size, tile_size), (0, 0, 0, 0))
    tile.alpha_composite(image, ((tile_size - image.width) // 2,
                                 (tile_size - image.height) // 2))
    return tile


def convert_sheet(world: str) -> None:
    path = SOURCE / f"poi_{world}.png"
    image = Image.open(path).convert("RGB")
    if image.size != (1024, 768):
        raise ValueError(f"{path} should be 1024x768, got {image.size}")
    atlas = Image.new("RGBA", (512, 384), (0, 0, 0, 0))
    for index, kind in enumerate(KINDS):
        x, y = index % 4, index // 4
        source = image.crop((x * 256, y * 256, (x + 1) * 256, (y + 1) * 256))
        icon = native_icon(world, kind) or icon_from_cell(clean_source_cell(source, world), world == "pixel")
        atlas.alpha_composite(icon, (x * 128, y * 128))
    DESTINATION.mkdir(parents=True, exist_ok=True)
    atlas.save(DESTINATION / f"{world}.webp", format="WEBP", lossless=True, method=6)


def install_onboarding_icons() -> None:
    for kind, world in ONBOARDING_WORLD.items():
        source = Image.open(SOURCE / f"poi_{world}.png").convert("RGB")
        index = KINDS.index(kind)
        x, y = index % 4, index // 4
        cell = source.crop((x * 256, y * 256, (x + 1) * 256, (y + 1) * 256))
        destination = DRAWABLE / f"onboarding_marker_{kind}.webp"
        icon = native_icon(world, kind, 256) or icon_from_cell(clean_source_cell(cell, world), world == "pixel", 256)
        icon.save(destination, format="WEBP", lossless=True, method=6)


def install_education_icon() -> None:
    school = native_icon("san_andreas", "education", 64)
    if school is None:
        raise ValueError("Missing San Andreas school marker")
    school.save(DESTINATION / "san_andreas_education.webp", format="WEBP", lossless=True, method=6)


if __name__ == "__main__":
    missing = [world for world in WORLDS if not (SOURCE / f"poi_{world}.png").exists()]
    if missing:
        raise SystemExit(f"Missing imagegen sheets: {', '.join(missing)}")
    for world in WORLDS:
        convert_sheet(world)
    install_education_icon()
    install_onboarding_icons()
    print(f"Installed {len(WORLDS)} POI atlases and {len(ONBOARDING_WORLD)} onboarding icons")
