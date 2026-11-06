#!/usr/bin/env python3
"""Build original Waypoint styles from live OpenMapTiles geographic data.

This generator deliberately has no preset-style input. It authors every layer,
filter, zoom range, label rule, road hierarchy and material reference below.
OpenMapTiles supplies geographic features only; no MapLibre theme or sprite sheet
is downloaded or copied.
"""

from __future__ import annotations

import json
import os
from pathlib import Path

from cartography_profiles import PROFILES
from map_theme_catalog import THEMES
from map_style_recipes import RECIPES

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_ROOT = Path(os.environ.get("WAYPOINT_ASSET_OUTPUT", ROOT)).resolve()
STYLE_DIR = OUTPUT_ROOT / "app/src/main/assets/styles"
ROAD_GROUPS = {
    "primary": ("motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link"),
    "secondary": ("secondary", "secondary_link", "tertiary", "tertiary_link"),
    "local": ("minor", "service", "residential", "living_street", "track"),
    "path": ("path", "footway", "cycleway", "steps", "pedestrian"),
    "rail": ("rail", "transit", "subway", "light_rail", "tram"),
    "ferry": ("ferry",),
}
RELIEF_ATTRIBUTION = "Terrain tiles © Mapzen / data providers"
OPENMAPTILES_ATTRIBUTION = "© OpenStreetMap contributors · © OpenMapTiles"


def rgba_mix(a: str, b: str, amount: float) -> str:
    left = tuple(int(a[index:index + 2], 16) for index in (1, 3, 5))
    right = tuple(int(b[index:index + 2], 16) for index in (1, 3, 5))
    return "#" + "".join(f"{round(x * (1 - amount) + y * amount):02x}" for x, y in zip(left, right))


def linear_luminance(value: str) -> float:
    def convert(channel: int) -> float:
        channel /= 255
        return channel / 12.92 if channel <= .04045 else ((channel + .055) / 1.055) ** 2.4
    r, g, b = (convert(int(value[i:i + 2], 16)) for i in (1, 3, 5))
    return .2126 * r + .7152 * g + .0722 * b


def contrast_ratio(a: str, b: str) -> float:
    first, second = sorted((linear_luminance(a), linear_luminance(b)), reverse=True)
    return (first + .05) / (second + .05)


def colors_for(theme: dict[str, str], night: bool) -> dict[str, str]:
    colors = dict(theme)
    if night:
        for key, amount in (
            ("ground", .60), ("land", .56), ("park", .50),
            ("water", .63), ("building", .58), ("casing", .50),
        ):
            colors[key] = rgba_mix(theme[key], "#071018", amount)
        colors["label"] = "#F0F4EF"
        colors["halo"] = colors["ground"]
        colors["road"] = rgba_mix(theme["road"], "#D8EDF3", .28)
        colors["outline"] = rgba_mix(theme["outline"], "#D8EDF3", .24)
    # Keep reference-specific lettering while ensuring labels remain readable.
    if contrast_ratio(colors["label"], colors["ground"]) < 4.5:
        colors["label"] = "#F3F4EE" if linear_luminance(colors["ground"]) < .35 else "#18211D"
    return colors


def any_class(values: tuple[str, ...]) -> list:
    return ["any", *[["==", ["get", "class"], value] for value in values]]


def layer_filter(classes: tuple[str, ...], brunnel: str | None = None) -> list:
    parts = [any_class(classes)]
    if brunnel == "surface":
        parts.append(["any", ["!", ["has", "brunnel"]], ["!=", ["get", "brunnel"], "tunnel"]])
    elif brunnel:
        parts.append(["==", ["get", "brunnel"], brunnel])
    return ["all", *parts]


def width_expr(base: float, minzoom: float = 5.0) -> list:
    return [
        "interpolate", ["linear"], ["zoom"],
        minzoom, max(.18, round(base * .18, 2)),
        10, max(.35, round(base * .38, 2)),
        14, max(.65, round(base * .82, 2)),
        18, round(base * 2.25, 2),
    ]


def fill_layer(layer_id: str, source_layer: str, color: str, opacity: float = 1.0,
               classes: tuple[str, ...] | None = None, minzoom: float | None = None,
               outline: str | None = None, pattern: str | None = None) -> dict:
    result = {
        "id": layer_id,
        "type": "fill",
        "source": "openmaptiles",
        "source-layer": source_layer,
        "paint": {"fill-color": color, "fill-opacity": opacity},
    }
    if minzoom is not None:
        result["minzoom"] = minzoom
    if classes:
        result["filter"] = any_class(classes)
    if outline:
        result["paint"]["fill-outline-color"] = outline
    if pattern:
        result["paint"]["fill-pattern"] = pattern
    return result


def line_layer(layer_id: str, source_layer: str, color: str, base_width: float,
               classes: tuple[str, ...] | None = None, brunnel: str | None = "surface",
               opacity: float = 1.0, minzoom: float | None = None,
               dash: list[float] | None = None, blur: float = 0.0,
               line_cap: str = "round", line_join: str = "round") -> dict:
    result = {
        "id": layer_id,
        "type": "line",
        "source": "openmaptiles",
        "source-layer": source_layer,
        "layout": {"line-cap": line_cap, "line-join": line_join},
        "paint": {
            "line-color": color,
            "line-width": width_expr(base_width),
            "line-opacity": opacity,
        },
    }
    if minzoom is not None:
        result["minzoom"] = minzoom
    if classes:
        result["filter"] = layer_filter(classes, brunnel)
    if dash:
        result["paint"]["line-dasharray"] = dash
    if blur:
        result["paint"]["line-blur"] = blur
    return result


def symbol_layer(layer_id: str, source_layer: str, font: str, color: str, halo: str,
                 minzoom: float, classes: tuple[str, ...] | None = None,
                 placement: str = "point", size: list | float = 11.0,
                 letter_spacing: float = .02, transform: str | None = None,
                 halo_width: float = 1.4) -> dict:
    layout = {
        "text-field": ["coalesce", ["get", "name:en"], ["get", "name_en"], ["get", "name"]],
        "text-font": [font],
        "text-size": size,
        "text-max-width": 8,
        "text-allow-overlap": False,
        "text-ignore-placement": False,
        "symbol-placement": placement,
        "text-letter-spacing": letter_spacing,
    }
    if transform:
        layout["text-transform"] = transform
    result = {
        "id": layer_id,
        "type": "symbol",
        "source": "openmaptiles",
        "source-layer": source_layer,
        "minzoom": minzoom,
        "layout": layout,
        "paint": {"text-color": color, "text-halo-color": halo,
                  "text-halo-width": halo_width, "text-halo-blur": .25},
    }
    if classes:
        result["filter"] = any_class(classes)
    return result


def label_size(start: float, end: float) -> list:
    return ["interpolate", ["linear"], ["zoom"], 8, start, 14, start + 1.5, 18, end]


def terrain_world(profile: dict) -> bool:
    return profile["relief"] >= .45


def build_layers(name: str, colors: dict[str, str], profile: dict,
                 holographic: bool = False) -> tuple[list[dict], dict[str, dict]]:
    theme = THEMES[name]
    recipe = RECIPES[name]
    detail = recipe["detail"]
    font = theme["font"]
    buildings = "extrude" if holographic else ("flat" if name == "division" else profile["building"])
    land_edge = colors["outline"] if recipe["land_edges"] else None
    land_opacity = {
        "municipal": (.88, .82, .62), "monochrome-city": (.82, .76, .52),
        "neon": (.72, .82, .58), "phosphor": (.54, .68, .46),
        "dark-tactical": (.62, .78, .58), "strategic-vector": (.44, .7, .36),
        "competitive-radar": (.74, .86, .6), "amber-terminal": (.52, .7, .46),
        "engraved": (.84, .6, .84), "antique-atlas": (.82, .58, .86),
        "nautical-engraving": (.8, .56, .84), "printed-medieval": (.84, .62, .86),
        "machine-atlas": (.86, .6, .86), "ink-wash": (.74, .58, .82),
    }.get(detail, (.92, .78, .9))

    def themed_line(layer_id: str, source_layer: str, color: str, base_width: float,
                    classes: tuple[str, ...] | None = None, brunnel: str | None = "surface",
                    opacity: float = 1.0, minzoom: float | None = None,
                    dash: list[float] | None = None, blur: float = 0.0) -> dict:
        return line_layer(layer_id, source_layer, color, base_width, classes, brunnel,
                          opacity, minzoom, dash, blur,
                          recipe["line_cap"], recipe["line_join"])

    layers: list[dict] = [
        {"id": "waypoint-ground", "type": "background", "paint": {"background-pattern": "world-material"}},
        fill_layer("waypoint-land-residential", "landuse", colors["land"], land_opacity[0],
                   ("residential", "suburb", "neighbourhood", "hamlet"), outline=land_edge),
        fill_layer("waypoint-land-urban", "landuse", rgba_mix(colors["land"], colors["building"], .22), land_opacity[1],
                   ("commercial", "retail", "industrial", "railway", "construction"), outline=land_edge),
        fill_layer("waypoint-land-amenities", "landuse", rgba_mix(colors["land"], colors["park"], .35), land_opacity[2],
                   ("school", "hospital", "university", "cemetery", "pitch", "playground"), outline=land_edge),
        fill_layer("waypoint-landcover-woodland", "landcover", colors["park"], profile["park"],
                   ("wood", "forest", "tree_cover")),
        fill_layer("waypoint-landcover-open", "landcover", rgba_mix(colors["park"], colors["land"], .28),
                   profile["park"] * .82, ("grass", "meadow", "scrub", "heath", "grassland")),
        fill_layer("waypoint-landcover-wetland", "landcover", rgba_mix(colors["park"], colors["water"], .28),
                   .72, ("wetland", "marsh")),
        fill_layer("waypoint-landcover-bare", "landcover", rgba_mix(colors["land"], colors["park"], .24),
                   .8, ("sand", "scree", "rock", "bare_rock", "ice")),
        fill_layer("waypoint-land-park", "park", colors["park"], min(1.0, profile["park"] + .12), outline=colors["outline"]),
    ]

    sources: dict[str, dict] = {
        "openmaptiles": {
            "type": "vector",
            "url": "https://tiles.openfreemap.org/planet",
            "attribution": OPENMAPTILES_ATTRIBUTION,
        },
    }
    if terrain_world(profile):
        sources["waypoint-terrain"] = {
            "type": "raster-dem",
            "tiles": ["https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"],
            "encoding": "terrarium",
            "tileSize": 256,
            "maxzoom": 15,
            "attribution": RELIEF_ATTRIBUTION,
        }
        layers.append({
            "id": "waypoint-terrain-relief",
            "type": "hillshade",
            "source": "waypoint-terrain",
            "paint": {
                "hillshade-exaggeration": profile["relief"],
                "hillshade-shadow-color": colors["casing"],
                "hillshade-highlight-color": colors["road"],
                "hillshade-accent-color": colors["outline"],
                "hillshade-illumination-direction": 315,
            },
        })

    layers.extend([
        fill_layer("waypoint-water", "water", colors["water"], 1.0, outline=rgba_mix(colors["water"], colors["road"], .12)),
        fill_layer("waypoint-aeroway", "aeroway", rgba_mix(colors["land"], colors["road"], .12), .88,
                   ("apron", "helipad", "taxiway")),
    ])
    waterway_width = 1.6 if detail in {"strategic-vector", "phosphor", "amber-terminal", "survey-grid"} else 2.4
    layers.append(themed_line("waypoint-waterways", "waterway", colors["water"], waterway_width,
                             ("river", "canal", "stream", "ditch", "drain"), brunnel="surface", opacity=.92))
    layers.append(themed_line("waypoint-aeroway-runways", "aeroway", colors["road"], 3.2,
                             ("runway", "taxiway"), brunnel=None, opacity=.74))

    if buildings != "hidden":
        opacity = (.18 if holographic else {"outline": .13, "ink": .28, "pixel": .95}.get(buildings, .78))
        min_building_zoom = 13.5 if buildings in ("extrude", "pixel") else 15.0
        building_color = "#226C70" if holographic else colors["building"]
        building_outline = "#55E4D5" if holographic else rgba_mix(colors["building"], colors["outline"], .38)
        layers.append(fill_layer("waypoint-buildings", "building", building_color, opacity,
                                 minzoom=min_building_zoom,
                                 outline=building_outline))
        if buildings in ("ink", "outline", "pixel") or holographic:
            layers.append(themed_line(
                "waypoint-building-edges", "building", colors["outline"],
                1.0 if holographic else (.9 if buildings == "ink" else .55),
                brunnel=None, opacity=.86 if holographic else .7, minzoom=14.5,
            ))
        if buildings == "extrude":
            layers.append({
                "id": "waypoint-building-extrusions",
                "type": "fill-extrusion",
                "source": "openmaptiles",
                "source-layer": "building",
                "minzoom": 14.2,
                "paint": {
                    "fill-extrusion-color": "#258E89" if holographic else colors["building"],
                    "fill-extrusion-height": ["interpolate", ["linear"], ["zoom"],
                                              14.2, 0, 15.5, ["coalesce", ["get", "render_height"], 7]],
                    "fill-extrusion-base": ["coalesce", ["get", "render_min_height"], 0],
                    "fill-extrusion-opacity": .36 if holographic else .9,
                },
            })

    if holographic:
        # The selected Dark Zone 3D mode is a new teal hologram over actual
        # building footprints and roads, with no fabricated mission markers.
        layers.append(themed_line("waypoint-hologram-building-glow", "building",
                                 "#62F6E1", 1.25, brunnel=None,
                                 opacity=.48, minzoom=14.5, blur=1.6))
        layers.append(themed_line("waypoint-hologram-road-glow", "transportation",
                                 "#32B8AA", 2.8, sum((list(values) for values in ROAD_GROUPS.values()), []),
                                 brunnel="surface", opacity=.26, minzoom=10, blur=2.2))

    road_scale = max(.42, profile["road"] * recipe["road_scale"])
    case_scale = max(.12, profile["case"] * recipe["case_scale"])
    casing_color = colors["casing"]
    road_color = colors["road"]
    if holographic:
        casing_color = "#124D4E"
        road_color = "#7DE8D8"
    for group, classes in ROAD_GROUPS.items():
        if group == "primary":
            base, priority = 3.25, 1.0
        elif group == "secondary":
            base, priority = 2.35, .78
        elif group == "local":
            base, priority = 1.55, .52
        elif group == "path":
            base, priority = 1.2, .36
        elif group == "rail":
            base, priority = 1.0, .5
        else:
            base, priority = 1.2, .45
        if group not in ("path", "rail", "ferry") and case_scale > .2:
            layers.append(themed_line(f"waypoint-{group}-casing", "transportation", casing_color,
                                     base * (1.55 + case_scale * .28) * road_scale * priority,
                                     classes, "surface", .9))
        dash = None
        if group == "path" and recipe["path_dash"]:
            dash = recipe["path_dash"]
        elif group == "path" and (profile["paper"] or name in {"counter_strike", "field_ops", "stalker"}):
            dash = [1.6, 1.8]
        if group == "rail":
            dash = [3.2, 1.6]
        layers.append(themed_line(f"waypoint-{group}-roads", "transportation", road_color,
                                 base * road_scale * priority, classes,
                                 "surface",
                                 .84 if group == "ferry" else 1.0, dash=dash))
        if group in ("primary", "secondary", "local"):
            layers.append(themed_line(f"waypoint-{group}-tunnels", "transportation",
                                     colors["outline"], base * road_scale * priority,
                                     classes, "tunnel", .78, dash=[1.2, 1.3]))

    layers.extend([
        themed_line("waypoint-bridge-structure", "transportation", colors["outline"],
                   2.1 * road_scale, tuple(sum((list(v) for v in ROAD_GROUPS.values()), [])),
                   "bridge", .94, minzoom=10),
        {
            "id": "waypoint-boundaries",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "filter": ["!=", ["get", "admin_level"], 2],
            "paint": {"line-color": colors["outline"],
                      "line-width": ["interpolate", ["linear"], ["zoom"], 3, .35, 12, .8, 16, 1.1],
                      "line-opacity": .72 if detail in {"strategic-vector", "competitive-radar", "dark-tactical", "survey-grid"} else .42 if detail in {"municipal", "monochrome-city", "clean-network"} else .56},
        },
        {
            "id": "waypoint-country-boundaries",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "filter": ["==", ["get", "admin_level"], 2],
            "paint": {"line-color": colors["outline"],
                      "line-width": ["interpolate", ["linear"], ["zoom"], 2, .7, 8, 1.1],
                      "line-dasharray": recipe["boundary_dash"] or [2, 1.5], "line-opacity": .72},
        },
    ])

    water_font_color = colors["label"]
    water_halo = colors["halo"]
    layers.extend([
        symbol_layer("waypoint-water-labels", "water_name", font, water_font_color, water_halo,
                     11.0, placement="point", size=label_size(9.5, 12.5), letter_spacing=recipe["label_spacing"],
                     transform=recipe["label_transform"], halo_width=recipe["label_halo"]),
        symbol_layer("waypoint-river-labels", "waterway", font, water_font_color, water_halo,
                     12.5, ("river", "canal"), placement="line", size=label_size(9, 11.5),
                     letter_spacing=recipe["label_spacing"], transform=recipe["label_transform"],
                     halo_width=recipe["label_halo"]),
        symbol_layer("waypoint-road-labels", "transportation_name", font, colors["label"], colors["halo"],
                     13.2, placement="line", size=label_size(profile["labels"] - 1.5, profile["labels"]),
                     letter_spacing=recipe["label_spacing"],
                     transform=recipe["label_transform"] or ("uppercase" if name in {"old_west", "green_terminal", "liberty", "counter_strike", "defcon", "dos_atlas"} else None),
                     halo_width=recipe["label_halo"]),
        symbol_layer("waypoint-city-labels", "place", font, colors["label"], colors["halo"],
                     4.0, ("city", "town", "village", "hamlet", "suburb", "neighbourhood"),
                     size=label_size(10, 15), letter_spacing=recipe["label_spacing"],
                     transform=recipe["label_transform"] or ("uppercase" if name in {"los_santos", "liberty", "green_terminal", "counter_strike", "defcon"} else None),
                     halo_width=recipe["label_halo"]),
        symbol_layer("waypoint-region-labels", "place", font, colors["label"], colors["halo"],
                     2.0, ("country", "state", "province", "region"),
                     size=label_size(12, 18), letter_spacing=max(.08, recipe["label_spacing"]),
                     transform=recipe["label_transform"], halo_width=recipe["label_halo"]),
        symbol_layer("waypoint-aerodrome-labels", "aerodrome_label", font, colors["label"], colors["halo"],
                     11.5, placement="point", size=label_size(10, 12), letter_spacing=recipe["label_spacing"],
                     transform=recipe["label_transform"], halo_width=recipe["label_halo"]),
    ])

    if name == "pixel":
        # Original 16-bit textures reinforce the pixel world while still using
        # actual landcover, water, road and building geometry.
        for layer in layers:
            if layer.get("id") == "waypoint-landcover-woodland":
                layer["paint"]["fill-pattern"] = "pixel-grass"
            elif layer.get("id") == "waypoint-water":
                layer["paint"]["fill-pattern"] = "pixel-water"
            elif layer.get("id") == "waypoint-buildings":
                layer["paint"]["fill-pattern"] = "pixel-stone"
    if name == "retro_quest":
        for layer in layers:
            if layer.get("id") == "waypoint-landcover-woodland":
                layer["paint"]["fill-pattern"] = "pixel-grass"
    return order_layers(name, layers), sources


def order_layers(name: str, layers: list[dict]) -> list[dict]:
    """Apply the world-specific geography, network, and label hierarchy."""
    recipe = RECIPES[name]
    buckets: dict[str, list[dict]] = {
        "background": [], "terrain": [], "landuse": [], "landcover": [], "park": [],
        "hydro": [], "aeroway": [], "buildings": [], "boundaries": [],
        "hologram": [], "roads": [], "labels": [],
    }
    road_buckets = {group: [] for group in ROAD_GROUPS}
    label_names = {
        "city": "waypoint-city-labels",
        "region": "waypoint-region-labels",
        "aerodrome": "waypoint-aerodrome-labels",
        "water": "waypoint-water-labels",
        "river": "waypoint-river-labels",
        "road": "waypoint-road-labels",
    }
    label_buckets = {kind: [] for kind in label_names}

    for layer in layers:
        layer_id = layer["id"]
        if layer_id == "waypoint-ground":
            buckets["background"].append(layer)
        elif layer_id == "waypoint-terrain-relief":
            buckets["terrain"].append(layer)
        elif layer_id.startswith("waypoint-landcover-"):
            buckets["landcover"].append(layer)
        elif layer_id == "waypoint-land-park":
            buckets["park"].append(layer)
        elif layer_id.startswith("waypoint-land-"):
            buckets["landuse"].append(layer)
        elif layer_id in {"waypoint-water", "waypoint-waterways"}:
            buckets["hydro"].append(layer)
        elif layer_id in {"waypoint-aeroway", "waypoint-aeroway-runways"}:
            buckets["aeroway"].append(layer)
        elif layer_id.startswith("waypoint-hologram-"):
            buckets["hologram"].append(layer)
        elif layer_id.startswith("waypoint-building"):
            buckets["buildings"].append(layer)
        elif layer_id in {"waypoint-boundaries", "waypoint-country-boundaries"}:
            buckets["boundaries"].append(layer)
        elif layer_id in label_names.values():
            kind = next(kind for kind, label_id in label_names.items() if label_id == layer_id)
            label_buckets[kind].append(layer)
        else:
            road_group = next((group for group in ROAD_GROUPS if layer_id.startswith(f"waypoint-{group}-")), None)
            if road_group:
                road_buckets[road_group].append(layer)
            elif layer_id == "waypoint-bridge-structure":
                buckets["roads"].append(layer)
            else:
                raise ValueError(f"Unclassified map layer in {name}: {layer_id}")

    if sorted(label_names) != sorted(recipe["label_order"]):
        raise ValueError(f"Invalid label priority for {name}")
    result = list(buckets["background"])
    result.extend(buckets["terrain"])
    for group in recipe["land_order"]:
        result.extend(buckets[group])
    for group in recipe["feature_order"]:
        result.extend(buckets[group])
    for group in recipe["road_order"]:
        result.extend(road_buckets[group])
    result.extend(buckets["roads"])
    for kind in recipe["label_order"]:
        result.extend(label_buckets[kind])

    if len(result) != len(layers):
        raise ValueError(f"The layer hierarchy dropped or duplicated layers in {name}")
    return result


def build_style(name: str, night: bool, holographic: bool = False) -> dict:
    colors = colors_for(THEMES[name], night)
    layers, sources = build_layers(name, colors, PROFILES[name], holographic)
    title = name.replace("_", " ").title()
    mode = "Dark Zone Hologram" if holographic else title
    phase = "Night" if night else "Day"
    return {
        "version": 8,
        "name": f"Waypoint · {mode} · {phase}",
        "metadata": {
            "waypoint:authored": True,
            "waypoint:theme": name,
            "waypoint:phase": phase.lower(),
            "waypoint:grammar": RECIPES[name]["detail"],
            "waypoint:land-order": ",".join(RECIPES[name]["land_order"]),
            "waypoint:feature-order": ",".join(RECIPES[name]["feature_order"]),
            "waypoint:road-order": ",".join(RECIPES[name]["road_order"]),
            "waypoint:label-priority": ",".join(RECIPES[name]["label_order"]),
            "waypoint:material": THEMES[name]["material"],
            "waypoint:holographic": holographic,
            "maplibre:autocomposite": False,
        },
        # Keep local custom typefaces where bundled; MapGlyphs falls back to
        # OpenFreeMap's Noto Sans for ranges a custom face does not contain.
        "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "sources": sources,
        "layers": layers,
    }


def main() -> None:
    if len(THEMES) != 38 or set(THEMES) != set(PROFILES) or set(THEMES) != set(RECIPES):
        raise ValueError("World catalogue, palettes, cartography profiles and style recipes must contain the same 38 worlds")
    signatures = {
        name: tuple((layer["id"], layer["type"], layer.get("source-layer"),
                     json.dumps(layer.get("filter"), sort_keys=True))
                   for layer in build_style(name, False)["layers"])
        for name in THEMES
    }
    if len(set(signatures.values())) != 38:
        raise ValueError("Every world must have a structurally distinct ordered map grammar")
    for name in THEMES:
        if signatures[name] != tuple((layer["id"], layer["type"], layer.get("source-layer"),
                                      json.dumps(layer.get("filter"), sort_keys=True))
                                     for layer in build_style(name, True)["layers"]):
            raise ValueError(f"Day and night map layer structure drifted for {name}")
    STYLE_DIR.mkdir(parents=True, exist_ok=True)
    generated: set[str] = set()
    for name in THEMES:
        styles = [(f"{name}.json", build_style(name, False, holographic=False)),
                  (f"{name}_night.json", build_style(name, True, holographic=False))]
        if name == "division":
            styles.extend([
                ("division_holo.json", build_style(name, False, holographic=True)),
                ("division_holo_night.json", build_style(name, True, holographic=True)),
            ])
        for filename, style in styles:
            (STYLE_DIR / filename).write_text(json.dumps(style, separators=(",", ":")) + "\n")
            generated.add(filename)
    for path in STYLE_DIR.glob("*.json"):
        if path.name not in generated:
            path.unlink()
    print(f"Generated {len(generated)} original styles for {len(THEMES)} worlds; no preset theme input")


if __name__ == "__main__":
    main()
