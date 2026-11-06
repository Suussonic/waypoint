#!/usr/bin/env python3
"""Build original README posters from local, sanitised demo captures.

Requires Pillow. Run from the repository root. No game artwork or personal
journey screenshots are used. Pass private phone and Desktop Head Unit capture
folders; keep captions and the README alt text in sync.
"""
import argparse
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / "docs/images"
parser = argparse.ArgumentParser()
parser.add_argument("--captures", type=Path, required=True,
                    help="Local directory with sanitized exclusion-zone.jpg and six featured-world JPGs")
parser.add_argument("--car-captures", type=Path, required=True,
                    help="Local directory with the public-route exclusion-zone-after-turn.png Desktop Head Unit capture")
ARGS = parser.parse_args()
CAPTURES = ARGS.captures.resolve()
CAR_CAPTURES = ARGS.car_captures.resolve()
OUT = IMAGES / "promo"
OUT.mkdir(exist_ok=True)
FONTS = ROOT / "app/src/main/res/font"
INK, IVORY, LIME = "#101511", "#F3F5E8", "#D5F16C"


def font(size, display=True):
    return ImageFont.truetype(str(FONTS / ("barlow_condensed_bold.ttf" if display else "noto_sans.ttf")), size)


def text(im, value, x, y, size, color=IVORY, display=True):
    face = font(size, display)
    # Use the actual ink bounds, so every baseline and margin is deliberate.
    box = face.getbbox(value)
    assert x + face.getlength(value) <= im.width - 32, f"Text exceeds poster margin: {value}"
    ImageDraw.Draw(im).text((x, y - box[1]), value, font=face, fill=color)


def lines(im, values, x, y, size, leading, color=IVORY):
    for index, value in enumerate(values):
        text(im, value, x, y + index * leading, size, color)


def map_surface(size, name, color=INK):
    surface = Image.new("RGB", size, color)
    shot = Image.open(CAPTURES / f"{name}.jpg").convert("RGB")
    # Only the unobstructed centre of the actual map becomes the backdrop.
    crop = shot.crop((100, 760, 650, 1350))
    crop = ImageOps.fit(crop, size, method=Image.Resampling.LANCZOS)
    return Image.blend(surface, crop, .09)


def phone(canvas, name, x, y, width, angle=0):
    shot = Image.open(CAPTURES / f"{name}.jpg").convert("RGBA")
    height = round(shot.height * width / shot.width)
    shot = shot.resize((width, height), Image.Resampling.LANCZOS)
    margin, bezel = 55, 12
    device = Image.new("RGBA", (width + margin * 2, height + margin * 2))
    shadow = Image.new("RGBA", device.size)
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((margin - 10, margin + 18, margin + width + 10, margin + height + 22), 50, fill=(0, 0, 0, 160))
    device.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(20)))
    d = ImageDraw.Draw(device)
    d.rounded_rectangle((margin - bezel, margin - bezel, margin + width + bezel, margin + height + bezel), 44, fill="#434A43")
    d.rounded_rectangle((margin - bezel + 3, margin - bezel + 3, margin + width + bezel - 3, margin + height + bezel - 3), 41, fill="#090C0A")
    mask = Image.new("L", shot.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, height - 1), 31, fill=255)
    device.paste(shot, (margin, margin), mask)
    if angle:
        device = device.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    canvas.paste(device, (x - margin, y - margin), device)


def car_screen(canvas, x, y, width):
    shot = Image.open(CAR_CAPTURES / "exclusion-zone-after-turn.png").convert("RGBA")
    assert shot.width * 9 == shot.height * 16, "The car capture must be a 16:9 Desktop Head Unit screenshot"
    height = round(width * 9 / 16)
    shot = shot.resize((width, height), Image.Resampling.LANCZOS)
    margin, bezel = 52, 12
    device = Image.new("RGBA", (width + margin * 2, height + margin * 2))
    shadow = Image.new("RGBA", device.size)
    ImageDraw.Draw(shadow).rounded_rectangle(
        (margin - 12, margin + 18, margin + width + 12, margin + height + 22),
        28, fill=(0, 0, 0, 170),
    )
    device.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(20)))
    ImageDraw.Draw(device).rounded_rectangle(
        (margin - bezel, margin - bezel, margin + width + bezel, margin + height + bezel),
        24, fill="#090C0A", outline="#434A43", width=4,
    )
    mask = Image.new("L", shot.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, height - 1), 15, fill=255)
    device.paste(shot, (margin, margin), mask)
    canvas.paste(device, (x - margin, y - margin), device)


def brand(canvas, x, y, size=62):
    mark = Image.open(IMAGES / "waypoint-icon.png").convert("RGBA")
    mark = mark.resize((size, size), Image.Resampling.LANCZOS)
    canvas.paste(mark, (x, y), mark)
    text(canvas, "WAYPOINT", x + size + 16, y + 12, round(size * .66))


def save(image, name):
    path = OUT / name
    image.save(path, quality=91, optimize=True, progressive=True, subsampling=0)
    print(f"{path.relative_to(ROOT)}: {image.width}×{image.height}, {path.stat().st_size // 1024} KB")


def hero(mobile=False):
    canvas = map_surface((900, 1450) if mobile else (1600, 980), "exclusion-zone")
    brand(canvas, 58 if mobile else 70, 52, 66)
    if mobile:
        lines(canvas, ["YOUR WORLD.", "EVERY SCREEN."], 58, 181, 126, 132)
        text(canvas, "Game-inspired maps on phone and Android Auto.", 62, 485, 31, display=False)
        text(canvas, "38 worlds. Every one included.", 62, 545, 32, LIME, False)
        car_screen(canvas, 42, 819, 816)
        phone(canvas, "exclusion-zone", 584, 660, 250, -4)
        ImageDraw.Draw(canvas).rectangle((0, 1340, 900, 1450), fill=INK)
        text(canvas, "PHONE  +  ANDROID AUTO  /  38 WORLDS", 62, 1383, 31, LIME)
    else:
        lines(canvas, ["YOUR WORLD.", "EVERY", "SCREEN."], 70, 204, 154, 147)
        text(canvas, "Game-inspired maps on phone and Android Auto.", 76, 711, 27, display=False)
        text(canvas, "38 worlds. Every one included.", 76, 764, 29, LIME, False)
        car_screen(canvas, 810, 397, 730)
        phone(canvas, "exclusion-zone", 1234, 209, 270, -4)
        ImageDraw.Draw(canvas).rectangle((0, 885, 1600, 980), fill=INK)
        text(canvas, "PHONE  +  ANDROID AUTO", 76, 920, 31, LIME)
        text(canvas, "REAL STREETS  ·  38 WORLDS", 1135, 920, 27)
    save(canvas, "hero-mobile.jpg" if mobile else "hero.jpg")


POSTERS = [
    ("los-santos", ["OWN", "THE STREETS."], "LOS SANTOS", "Grand Theft Auto V", "#C0A1F0"),
    ("old-west", ["GO A LITTLE", "MORE WEST."], "OLD WEST", "Red Dead Redemption 2", "#E8CCA0"),
    ("night-city", ["TAKE THE", "NEON ROUTE."], "NIGHT CITY", "Cyberpunk 2077", "#8FF6ED"),
    ("rivia", ["EVERY ROAD.", "A NEW STORY."], "THE CONTINENT", "The Witcher 3", "#E3CD96"),
    ("hyrule", ["FOLLOW YOUR", "CURIOSITY."], "SHEIKAH SLATE", "The Legend of Zelda: Breath of the Wild", "#B4D9E9"),
    ("pixel", ["THINK OUTSIDE", "THE BLOCKS."], "PIXEL WORLD", "Minecraft", "#C3E591"),
]


def poster(spec):
    name, headline, world, game, accent = spec
    canvas = map_surface((720, 1320), name)
    lines(canvas, headline, 48, 52, 94, 94, accent)
    text(canvas, world, 52, 285, 42)
    text(canvas, game, 52, 342, 25, "#CFD6C7", False)
    phone(canvas, name, 130, 421, 460)
    return canvas


hero()
hero(mobile=True)
posters = [poster(spec) for spec in POSTERS]
for index in range(3):
    left, right = posters[index * 2:index * 2 + 2]
    wide = Image.new("RGB", (1460, 1320), INK)
    wide.paste(left, (0, 0))
    wide.paste(right, (740, 0))
    save(wide, f"worlds-{index + 1}.jpg")
    narrow = Image.new("RGB", (720, 2660), INK)
    narrow.paste(left, (0, 0))
    narrow.paste(right, (0, 1340))
    save(narrow, f"worlds-{index + 1}-mobile.jpg")

# A compact supporting panel for the original, wallpaper-aware launcher mark.
identity = Image.new("RGB", (1600, 520), INK)
brand(identity, 70, 72, 86)
lines(identity, ["AT HOME ON", "YOUR HOME SCREEN."], 70, 208, 87, 87)
mark = Image.open(IMAGES / "waypoint-icon.png").convert("RGBA").resize((360, 360), Image.Resampling.LANCZOS)
identity.paste(mark, (1100, 80), mark)
save(identity, "identity.jpg")

# A readable GitHub-linked CTA; the accessible link text also appears in Markdown.
cta = Image.new("RGB", (680, 112), LIME)
text(cta, "DOWNLOAD FOR ANDROID", 38, 32, 43, INK)
d = ImageDraw.Draw(cta)
d.line([(582, 56), (633, 56)], fill=INK, width=5)
d.line([(615, 38), (633, 56), (615, 74)], fill=INK, width=5)
save(cta, "download.jpg")
