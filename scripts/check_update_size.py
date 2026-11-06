#!/usr/bin/env python3
"""Reject release APKs that installed Waypoint versions cannot download."""

import argparse
from pathlib import Path

# Older installed updaters, starting with v0.2.1, have this fixed limit.
MAX_UPDATE_BYTES = 150 * 1024 * 1024

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("apk", type=Path)
args = parser.parse_args()

size = args.apk.stat().st_size
print(f"Release APK: {size:,} bytes ({size / 1024 / 1024:.2f} MiB); legacy updater limit: 150 MiB")
if not 0 < size <= MAX_UPDATE_BYTES:
    raise SystemExit("APK cannot be downloaded by older installed Waypoint versions")
