#!/usr/bin/env python3
"""Frame sanitized Android Auto DHU screenshots for the public README.

Pass a directory outside Git containing exclusion-zone-before-turn.png and
exclusion-zone-after-turn.png from a public-route Desktop Head Unit capture. The script never
alters screen pixels, apart from uniform downsampling for each layout.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/images/promo"
FONTS = ROOT / "app/src/main/res/font"
INK = "#101511"
IVORY = "#F3F5E8"
LIME = "#D5F16C"
MUTED = "#B7C1B3"


def font(size, display=True):
    name = "barlow_condensed_bold.ttf" if display else "noto_sans.ttf"
    return ImageFont.truetype(str(FONTS / name), size)


def label(canvas, value, x, y, size, color=IVORY, display=True):
    face = font(size, display)
    draw = ImageDraw.Draw(canvas)
    bounds = draw.textbbox((0, 0), value, font=face)
    assert x + draw.textlength(value, font=face) < canvas.width - 32, value
    draw.text((x, y - bounds[1]), value, font=face, fill=color)


def mark(canvas, x, y, size):
    icon = Image.open(ROOT / "docs/images/waypoint-icon.png").convert("RGBA")
    icon = icon.resize((size, size), Image.Resampling.LANCZOS)
    canvas.paste(icon, (x, y), icon)
    label(canvas, "WAYPOINT", x + size + 16, y + 13, round(size * .66))


def screen(canvas, source, x, y, width):
    original = Image.open(source).convert("RGB")
    assert original.width * 9 == original.height * 16, source
    image = original.resize((width, width * 9 // 16), Image.Resampling.LANCZOS)
    canvas.paste(image, (x, y))


def save(canvas, name):
    path = OUT / name
    canvas.save(path, quality=92, optimize=True, progressive=True, subsampling=0)
    print(f"{path.relative_to(ROOT)}: {canvas.width}×{canvas.height}, {path.stat().st_size // 1024} KB")


def wide(captures):
    canvas = Image.new("RGB", (1600, 960), INK)
    mark(canvas, 64, 52, 64)
    label(canvas, "THE DASH, YOUR WORLD.", 64, 151, 112)
    label(canvas, "One public London route. Two moments in Exclusion Zone.", 69, 286, 29, display=False)
    screen(canvas, captures / "exclusion-zone-before-turn.png", 64, 365, 720)
    screen(canvas, captures / "exclusion-zone-after-turn.png", 816, 365, 720)
    label(canvas, "APPROACHING THE TURN", 64, 793, 48)
    label(canvas, "AFTER THE TURN", 816, 793, 48)
    label(canvas, "Guidance view in Android Auto", 64, 854, 23, MUTED, False)
    label(canvas, "Same route, vehicle on the road", 816, 854, 23, MUTED, False)
    ImageDraw.Draw(canvas).rectangle((64, 914, 1536, 917), fill=LIME)
    label(canvas, "ACTUAL DESKTOP HEAD UNIT CAPTURES  ·  SIMULATED LONDON JOURNEY", 64, 928, 20, LIME)
    save(canvas, "android-auto.jpg")


def narrow(captures):
    canvas = Image.new("RGB", (900, 1760), INK)
    mark(canvas, 48, 47, 70)
    label(canvas, "THE DASH,", 48, 157, 126)
    label(canvas, "YOUR WORLD.", 48, 274, 126)
    label(canvas, "One public London route. Two moments.", 52, 414, 29, display=False)
    screen(canvas, captures / "exclusion-zone-before-turn.png", 40, 500, 820)
    label(canvas, "APPROACHING THE TURN", 48, 974, 54)
    label(canvas, "Guidance view in Android Auto", 50, 1038, 24, MUTED, False)
    screen(canvas, captures / "exclusion-zone-after-turn.png", 40, 1100, 820)
    label(canvas, "AFTER THE TURN", 48, 1574, 54)
    label(canvas, "Same route, vehicle on the road", 50, 1638, 24, MUTED, False)
    ImageDraw.Draw(canvas).rectangle((48, 1700, 852, 1703), fill=LIME)
    label(canvas, "DESKTOP HEAD UNIT  ·  SIMULATED LONDON JOURNEY", 48, 1718, 22, LIME)
    save(canvas, "android-auto-mobile.jpg")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--captures", type=Path, required=True)
    captures = parser.parse_args().captures.resolve()
    wide(captures)
    narrow(captures)
