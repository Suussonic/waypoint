"""Stage only the Android images required to regenerate the three shipped videos."""

from pathlib import Path
from shutil import copy2


ROOT = Path(__file__).resolve().parents[2]
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
PUBLIC = Path(__file__).resolve().parent / "public"
ART = ("los_santos", "night_city", "pixel", "hyrule", "vice_coast")
MARKERS = ("place", "food", "cafe", "pub", "stay", "fuel", "parking",
           "shop", "sights", "health", "nature", "cash")


def copy_image(name: str, subdir: str = "") -> None:
    source = DRAWABLE / name
    if not source.is_file():
        raise FileNotFoundError(source)
    destination = PUBLIC / subdir / name
    destination.parent.mkdir(parents=True, exist_ok=True)
    copy2(source, destination)


for world in ART:
    copy_image(f"art_{world}.png" if world == "pixel" else f"art_{world}.jpg", "game-art")
for kind in MARKERS:
    copy_image(f"onboarding_marker_{kind}.webp", "map-icons")
copy_image("onboarding_globe.jpg")
print("Staged 18 Android images for Remotion; public/ stays ignored by Git")
