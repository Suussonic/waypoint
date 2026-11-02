"""Install the newly generated, original Waypoint artwork into Android.

Run with a local Python environment containing Pillow and NumPy after the
imagegen batch has finished.
The generated PNGs in output/imagegen are local source intermediates. The
compressed Android assets are the checked-in deliverables. Run
prepare_public_assets.py before rendering the Remotion videos.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageOps


ROOT = Path(__file__).resolve().parents[2]
FULL = ROOT / "output/imagegen/full-v2"
PILOT = ROOT / "output/imagegen/pilot-v2"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"

COVERS = (
    "los_santos", "old_west", "pixel", "night_city", "green_terminal",
    "san_andreas", "heat", "rivia", "vice_coast", "liberty", "ctos",
    "hyrule", "skyrim", "lands_between", "horizon", "assassin",
    "far_cry", "ghost", "bridges", "horizon_wilds", "spider",
    "hogwarts", "starfield", "kingdom", "saints", "just_cause", "mafia",
)
def source(name: str) -> Path:
    for folder in (FULL, PILOT):
        candidate = folder / f"{name}_v2.png"
        if candidate.exists():
            return candidate
    raise FileNotFoundError(name)


def sticker_cutout(path: Path) -> Image.Image:
    original = Image.open(path).convert("RGB")
    rgb = np.asarray(original)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    # Imagegen's high-chroma magenta is deliberately outside the sticker palette.
    magenta = (r > 145) & (b > 135) & (g < 118) & (np.abs(r.astype(int) - b.astype(int)) < 105)
    # fromarray can be read-only; floodfill must receive a mutable copy.
    connected = Image.fromarray(np.where(magenta, 255, 0).astype("uint8"), "L").copy()
    for corner in ((0, 0), (original.width - 1, 0), (0, original.height - 1),
                   (original.width - 1, original.height - 1)):
        if connected.getpixel(corner) == 255:
            ImageDraw.floodfill(connected, corner, 128, thresh=0)
    # A letterform or handle can enclose an isolated pocket of the same
    # chroma backdrop. Remove that saturated key color inside the silhouette.
    strict_chroma = (r > 210) & (b > 210) & (g < 70) & (
        np.abs(r.astype(int) - b.astype(int)) < 50
    )
    background = (np.asarray(connected) == 128) | strict_chroma
    alpha = Image.fromarray(np.where(background, 0, 255).astype("uint8"), "L")
    alpha = alpha.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(.45))
    cutout = original.convert("RGBA")
    cutout.putalpha(alpha)
    bbox = cutout.getbbox()
    if bbox is None:
        raise ValueError(f"No sticker pixels in {path}")
    cutout = cutout.crop(bbox)
    cutout.thumbnail((492, 492), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    canvas.alpha_composite(cutout, ((512 - cutout.width) // 2, (512 - cutout.height) // 2))
    return canvas


def main() -> None:
    missing = [name for name in (f"art_{x}" for x in COVERS) if not any(
        (folder / f"{name}_v2.png").exists() for folder in (FULL, PILOT)
    )]
    if missing:
        raise SystemExit(f"Imagegen output still missing: {', '.join(missing)}")

    for name in COVERS:
        art = Image.open(source(f"art_{name}")).convert("RGB")
        art = ImageOps.fit(art, (768, 1152), method=Image.Resampling.LANCZOS)
        if name == "pixel":
            art.save(DRAWABLE / "art_pixel.png", optimize=True)
        else:
            art.save(DRAWABLE / f"art_{name}.jpg", quality=87, optimize=True,
                     subsampling=0)

    print(f"Installed {len(COVERS)} world covers")


if __name__ == "__main__":
    main()
