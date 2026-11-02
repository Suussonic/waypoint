"""Install each world's original sticker emblem after its imagegen batch completes."""

from __future__ import annotations

from pathlib import Path

from PIL import Image

from install_artwork_v2 import ROOT, DRAWABLE, COVERS, sticker_cutout


SOURCE = ROOT / "output/imagegen/world-emblems-v1"


def main() -> None:
    missing = [name for name in COVERS if not (SOURCE / f"emblem_{name}.png").exists()]
    if missing:
        raise SystemExit(f"Emblem generation still missing: {', '.join(missing)}")
    for name in COVERS:
        cutout = sticker_cutout(SOURCE / f"emblem_{name}.png")
        cutout = cutout.resize((320, 320), Image.Resampling.LANCZOS)
        cutout.save(DRAWABLE / f"world_emblem_{name}.webp", format="WEBP",
                    lossless=True, method=6)
    print(f"Installed {len(COVERS)} world emblems")


if __name__ == "__main__":
    main()
