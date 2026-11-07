#!/usr/bin/env python3
"""Read-only check that selected checked-in styles match their generator output."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from generate_styles import build_style
from map_theme_catalog import THEMES

STYLE_DIR = Path(__file__).resolve().parents[1] / "app/src/main/assets/styles"


def drift_summary(checked: dict, generated: dict) -> str:
    top_level = sorted(key for key in checked.keys() | generated.keys()
                       if key != "layers" and checked.get(key) != generated.get(key))
    checked_layers = {layer["id"]: layer for layer in checked.get("layers", [])}
    generated_layers = {layer["id"]: layer for layer in generated.get("layers", [])}
    changed_layers = [layer_id for layer_id in checked_layers.keys() | generated_layers.keys()
                      if checked_layers.get(layer_id) != generated_layers.get(layer_id)]
    details = []
    if top_level:
        details.append("top-level: " + ", ".join(top_level))
    if changed_layers:
        details.append("layers: " + ", ".join(sorted(changed_layers)))
    if not details:
        details.append("JSON formatting or key order")
    return "; ".join(details)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", nargs="+", choices=sorted(THEMES),
                        help="World names to check, such as nier or night_city")
    args = parser.parse_args()
    failed = False
    for world in args.world:
        for night, suffix in ((False, ""), (True, "_night")):
            path = STYLE_DIR / f"{world}{suffix}.json"
            if not path.is_file():
                print(f"{path.name}: missing checked-in style")
                failed = True
                continue
            generated = build_style(world, night)
            generated_text = json.dumps(generated, separators=(",", ":")) + "\n"
            checked_text = path.read_text()
            if checked_text == generated_text:
                print(f"{path.name}: matches generator")
                continue
            checked = json.loads(checked_text)
            print(f"{path.name}: differs from generator ({drift_summary(checked, generated)})")
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
