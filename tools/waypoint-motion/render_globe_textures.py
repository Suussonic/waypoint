"""Generate original, theme-specific Earth textures from public-domain Natural Earth data."""

import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from shapely.affinity import translate
from shapely.geometry import box, shape, Polygon, MultiPolygon, GeometryCollection

ROOT = Path(__file__).resolve().parents[2]
DATA = Path(__file__).resolve().parent / "data"
OUT = ROOT / "app/src/main/assets/globe/textures"
COUNTRIES = json.loads((DATA / "countries.geojson").read_text())["features"]
PLACES = json.loads((DATA / "places.geojson").read_text())["features"]
FONT_DIR = ROOT / "app/src/main/res/font"
W, H = 4096, 2048

# Each palette was drawn for Waypoint's own map styles. The geographic shapes
# and place positions are the same in every texture so style changes do not jump.
THEMES = {
    "los_santos": ("#242824", "#bbc0a7", "#555c4f", "#1a241c", "#e1b6dd", "noto_sans.ttf"),
    "old_west": ("#979587", "#dfcba4", "#9a8663", "#3b3025", "#a24738", "kirsty_regular.ttf"),
    "green_terminal": ("#06190e", "#28521e", "#385f2c", "#a1ff50", "#8eff39", "vt323.ttf"),
    "pixel": ("#295885", "#69934d", "#bed6a0", "#f1ecd6", "#fff1a8", "monocraft.ttf"),
    "night_city": ("#071321", "#172d38", "#0ba2b4", "#8dfaff", "#fc555b", "orbitron.ttf"),
    "san_andreas": ("#8eaaa0", "#d0d1aa", "#879775", "#293b32", "#d66f48", "libre_baskerville.ttf"),
    "heat": ("#194e79", "#6a7666", "#9dc4c4", "#f5efda", "#f486c4", "rajdhani.ttf"),
    "rivia": ("#788474", "#b7ad89", "#756a50", "#302b22", "#a13d3b", "cinzel.ttf"),
    "skyrim": ("#586772", "#aab7b0", "#6a7c80", "#f2f4e9", "#afcad2", "cinzel.ttf"),
    "lands_between": ("#343a31", "#9d997a", "#6b6b51", "#eee0b9", "#d8bd75", "cinzel.ttf"),
    "horizon": ("#849cb2", "#a6ae83", "#798a64", "#282c35", "#d55c94", "rajdhani.ttf"),
    "hyrule": ("#152937", "#555c42", "#364538", "#d8d3a8", "#7fb8c5", "cinzel.ttf"),
    "assassin": ("#797b74", "#cdb589", "#8c826a", "#322a22", "#d1b669", "cinzel.ttf"),
    "ctos": ("#a8cbd5", "#dfe7db", "#a9baaa", "#233b42", "#318bb7", "noto_sans.ttf"),
    "hogwarts": ("#78838a", "#8c957b", "#687163", "#e7d6ac", "#ad9a75", "cinzel.ttf"),
    "vice_coast": ("#769eaa", "#e6dfcb", "#bca5a8", "#584959", "#f46ac0", "libre_baskerville.ttf"),
    "liberty": ("#536970", "#a8aaa2", "#646b68", "#20272a", "#efa937", "noto_sans.ttf"),
    "far_cry": ("#57746e", "#adb78d", "#617356", "#293828", "#f4d57b", "libre_baskerville.ttf"),
    "ghost": ("#758b8c", "#e3e4d9", "#a7b2a9", "#343d37", "#b84435", "cinzel.ttf"),
    "bridges": ("#25445d", "#84949b", "#566f79", "#e4e9e3", "#9ae5dd", "rajdhani.ttf"),
    "horizon_wilds": ("#4c6973", "#a4ad8b", "#6b7762", "#e7dfbf", "#e9ca7d", "cinzel.ttf"),
    "spider": ("#18465d", "#4d929c", "#236b79", "#edf3ed", "#ff9e9d", "rajdhani.ttf"),
    "starfield": ("#0d243e", "#4d7089", "#7c9aae", "#f1f4e9", "#f3c56f", "orbitron.ttf"),
    "kingdom": ("#8a9b91", "#c9b890", "#8c7958", "#342d24", "#dfba52", "cinzel.ttf"),
    "saints": ("#252d3b", "#4f4c60", "#373247", "#e4ddec", "#b893f3", "rajdhani.ttf"),
    "just_cause": ("#185d70", "#8eaa90", "#437f7c", "#e8e0c5", "#f0cf6a", "rajdhani.ttf"),
    "mafia": ("#7d9ca0", "#bbb99c", "#918d72", "#352f2a", "#bd624c", "libre_baskerville.ttf"),
}
THEMES.update({'division': ('#0c2027', '#1f3435', '#517575', '#e3e9df', '#819a98', 'rajdhani.ttf'), 'sea_chart': ('#8eaaa9', '#e2d4b1', '#938566', '#393327', '#f2e6c7', 'libre_baskerville.ttf'), 'dos_atlas': ('#090e11', '#1d170e', '#85673c', '#ffdf9b', '#b38d52', 'share_tech_mono.ttf'), 'aincrad': ('#81a8b0', '#decca4', '#9c906f', '#403c31', '#f7eacb', 'noto_sans.ttf'), 'counter_strike': ('#202b31', '#3c3c33', '#939084', '#f0ebd7', '#d6d2bd', 'roboto_condensed.ttf'), 'defcon': ('#01040c', '#071127', '#24476f', '#c6ddff', '#5d9bd4', 'share_tech_mono.ttf'), 'field_ops': ('#233238', '#3a4235', '#77816b', '#ecefdc', '#bbc2a8', 'roboto_condensed.ttf'), 'retro_quest': ('#437ea5', '#a4bf73', '#705541', '#f9f0d4', '#e5ca89', 'monocraft.ttf'), 'home_console': ('#9ccfe1', '#e2e9eb', '#a8bbc6', '#294b5b', '#ffffff', 'noto_sans.ttf')})

THEMES['nier']=('#9D9F94','#D5D1BB','#84816E','#35362E','#A14F37','noto_sans.ttf')
THEMES['stalker']=('#283435','#484b39','#818574','#e4e5d3','#9ee376','roboto_condensed.ttf')
for _world,_font in [('division','borda_regular.ttf'),('counter_strike','stratum2_regular.ttf'),('skyrim','futura_condensed_medium.ttf'),('pixel','minecraft_seven.ttf')]:
    THEMES[_world]=(*THEMES[_world][:-1],_font)

def polygons(geometry):
    if isinstance(geometry, Polygon):
        yield geometry
    elif isinstance(geometry, (MultiPolygon, GeometryCollection)):
        for piece in geometry.geoms:
            yield from polygons(piece)


def xy(lon, lat):
    return ((lon + 180) * W / 360, (90 - lat) * H / 180)


def draw_texture(name, palette):
    ocean, land, border, ink, accent, font_file = palette
    image = Image.new("RGB", (W, H), ocean)
    draw = ImageDraw.Draw(image)
    world = box(-180, -90, 180, 90)
    for feature in COUNTRIES:
        geometry = shape(feature["geometry"])
        for shift in (-360, 0, 360):
            moved = translate(geometry, xoff=shift)
            if not moved.intersects(world):
                continue
            for piece in polygons(moved.intersection(world)):
                outline = [xy(x, y) for x, y in piece.exterior.coords]
                if len(outline) < 3:
                    continue
                draw.polygon(outline, fill=land)
                draw.line(outline, fill=border, width=2, joint="curve")
                for ring in piece.interiors:
                    hole = [xy(x, y) for x, y in ring.coords]
                    draw.polygon(hole, fill=ocean)
    if name == "pixel":
        image = image.resize((512, 256), Image.Resampling.NEAREST).resize((W, H), Image.Resampling.NEAREST)
        draw = ImageDraw.Draw(image)
    if name == "green_terminal":
        scan = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        lines = ImageDraw.Draw(scan)
        for y in range(0, H, 8):
            lines.line((0, y, W, y), fill=(0, 0, 0, 13), width=2)
        image = Image.alpha_composite(image.convert("RGBA"), scan).convert("RGB")
        draw = ImageDraw.Draw(image)

    # At the selector's close globe zoom these remain about 24-30 device
    # pixels high, leaving room for coastlines and neighboring countries.
    country_font = ImageFont.truetype(str(FONT_DIR / font_file), 14 if name != "pixel" else 13)
    city_font = ImageFont.truetype(str(FONT_DIR / font_file), 11 if name != "pixel" else 10)
    occupied = []

    def label(lon, lat, text, font, color, margin=6):
        x, y = xy(lon, lat)
        text = text.upper()
        bounds = draw.textbbox((0, 0), text, font=font)
        width, height = bounds[2] - bounds[0], bounds[3] - bounds[1]
        rect = (x - width / 2 - margin, y - height / 2 - margin,
                x + width / 2 + margin, y + height / 2 + margin)
        if rect[0] < 3 or rect[2] > W - 3 or any(
            not (rect[2] < a or rect[0] > c or rect[3] < b or rect[1] > d)
            for a, b, c, d in occupied
        ):
            return False
        occupied.append(rect)
        draw.text((x - width / 2, y - height / 2), text, font=font,
                  fill=color, stroke_width=1, stroke_fill=ocean)
        return True

    featured = {"Finland", "Ukraine", "Turkey", "Kazakhstan", "Uzbekistan",
                "Iran", "Afghanistan", "Pakistan", "India", "Saudi Arabia",
                "Mongolia", "China", "Egypt", "Indonesia", "Japan", "Australia",
                "United States of America", "Canada", "Brazil", "Argentina"}
    for feature in sorted(COUNTRIES, key=lambda f: f["properties"].get("LABELRANK", 9)):
        p = feature["properties"]
        country = p.get("NAME_EN") or p["NAME"]
        if country == "People's Republic of China":
            country = "China"
        if country == "Democratic Republic of the Congo":
            country = "DR Congo"
        if country == "United States of America":
            country = "United States"
        if country != "Antarctica" and (p.get("LABELRANK", 9) <= 2 or country in featured):
            label(p["LABEL_X"], p["LABEL_Y"], country, country_font, ink, 7)

    major_cities = {"Moscow", "Kazan", "Novosibirsk", "Lahore", "Karachi",
                    "Mumbai", "New Delhi", "Kolkata", "Beijing", "Tokyo", "Shanghai",
                    "Tehran", "Istanbul", "Paris", "London", "Rome", "Berlin",
                    "New York", "Los Angeles", "Mexico City", "Rio de Janeiro",
                    "Cairo", "Lagos", "Cape Town", "Jakarta", "Sydney"}
    for feature in PLACES:
        p = feature["properties"]
        city = p.get("NAMEASCII") or p["NAME"]
        if city not in major_cities:
            continue
        lon, lat = feature["geometry"]["coordinates"]
        if label(lon, lat - 3.1, city, city_font, ink, 4):
            x, y = xy(lon, lat)
            draw.ellipse((x - 4, y - 4, x + 4, y + 4), fill=accent)

    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / f"{name}.png", optimize=True)
    print(f"{name}: {OUT / f'{name}.png'}", flush=True)


if __name__ == "__main__":
    for theme_name in sys.argv[1:] or THEMES:
        draw_texture(theme_name, THEMES[theme_name])
