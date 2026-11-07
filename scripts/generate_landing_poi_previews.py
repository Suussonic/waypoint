#!/usr/bin/env python3
"""Build compact website previews from Waypoint's checked-in POI atlases."""

import argparse
from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/poi"
DESTINATION = ROOT / "docs/assets/landing/poi-preview"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world", action="append", help="Refresh only this world; repeatable")
    args = parser.parse_args()
    encoder = shutil.which("cwebp")
    if encoder is None:
        raise SystemExit("cwebp is required to build landing-page POI previews")

    if args.world:
        atlases = [SOURCE / f"{world}.png" for world in sorted(set(args.world))]
    else:
        atlases = sorted(path for path in SOURCE.glob("*.png") if not path.stem.endswith("_night"))
    if not atlases:
        raise SystemExit(f"No daytime POI atlases found in {SOURCE}")
    missing = [path.name for path in atlases if not path.is_file()]
    if missing:
        raise SystemExit(f"Missing POI atlas(es): {missing}")

    DESTINATION.mkdir(parents=True, exist_ok=True)
    for atlas in atlases:
        output = DESTINATION / f"{atlas.stem}.webp"
        # The first two 128 px cells stay readable at the site's 48 px width.
        subprocess.run(
            [
                encoder,
                "-quiet",
                "-crop", "0", "0", "256", "128",
                "-resize", "96", "48",
                "-lossless",
                "-m", "6",
                str(atlas),
                "-o", str(output),
            ],
            check=True,
        )
    print(f"Generated {len(atlases)} previews in {DESTINATION}")


if __name__ == "__main__":
    main()
