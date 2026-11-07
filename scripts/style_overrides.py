"""Preserve intentional hand-tuned layers when regenerating authored map styles.

The Night City override records only fields that differ from the general
cartography recipe, plus its water grid and road glow layers. It is checked
against the shipped styles by check_style_generator_parity.py.
"""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path


@lru_cache(maxsize=1)
def _night_city_override() -> dict:
    path = Path(__file__).with_name("style_overrides") / "night_city.json"
    return json.loads(path.read_text())


def apply_style_overrides(name: str, night: bool, layers: list[dict]) -> list[dict]:
    if name != "night_city":
        return layers
    override = _night_city_override()["night" if night else "day"]
    by_id = {layer["id"]: layer for layer in layers}
    added = override["added"]
    changes = override["changes"]
    order = override["order"]
    if len(by_id) != len(layers) or set(added) & set(by_id) or not set(changes) <= set(by_id):
        raise ValueError("Night City override has duplicate or unknown layer IDs")
    if len(order) != len(set(order)) or set(order) != set(by_id) | set(added):
        raise ValueError("Night City layer order no longer matches its base recipe")
    result = []
    for layer_id in order:
        if layer_id in added:
            result.append(added[layer_id])
            continue
        layer = dict(by_id[layer_id])
        patch = changes.get(layer_id)
        if patch:
            for field in patch["remove"]:
                layer.pop(field, None)
            layer.update(patch["set"])
        if layer["id"] != layer_id:
            raise ValueError(f"Night City override renamed {layer_id}")
        result.append(layer)
    return result
