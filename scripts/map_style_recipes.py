"""Per-world cartographic rules translated from the reviewed in-game maps.

These are Waypoint layer recipes over live geographic features, not imported
MapLibre styles. A recipe changes the line grammar, land edging, label voice,
and map detail density independently of the shared data schema.
"""

from __future__ import annotations


def recipe(cap="round", join="round", *, land_edges=False, case=1.0,
           road=1.0, path_dash=None, boundary_dash=None, labels=None,
           spacing=.03, halo=1.4, detail="urban"):
    return {
        "line_cap": cap,
        "line_join": join,
        "land_edges": land_edges,
        "case_scale": case,
        "road_scale": road,
        "path_dash": path_dash,
        "boundary_dash": boundary_dash,
        "label_transform": labels,
        "label_spacing": spacing,
        "label_halo": halo,
        "detail": detail,
    }


# `detail` selects the map's visual language; all place and route geometry
# still comes from the same live OpenMapTiles features.
RECIPES = {
    "los_santos": recipe("butt", "miter", land_edges=True, case=.82, road=1.08, labels="uppercase", spacing=.08, halo=1.15, detail="municipal"),
    "old_west": recipe("round", "round", case=.78, road=.92, path_dash=[1.5, 1.8], boundary_dash=[1.1, 1.6], spacing=.09, halo=1.65, detail="engraved"),
    "night_city": recipe("square", "miter", case=.86, road=1.14, labels="uppercase", spacing=.06, halo=1.8, detail="neon"),
    "pixel": recipe("square", "miter", land_edges=True, case=1.1, road=1.0, path_dash=[1, 1], labels="uppercase", spacing=.04, halo=1.25, detail="voxel"),
    "green_terminal": recipe("square", "miter", case=.5, road=.9, path_dash=[1.2, 1.6], boundary_dash=[1, 2], labels="uppercase", spacing=.1, halo=1.65, detail="phosphor"),
    "san_andreas": recipe("butt", "miter", case=1.05, road=1.16, labels="uppercase", spacing=.06, halo=1.1, detail="sunlit"),
    "heat": recipe("round", "round", case=.9, road=1.18, path_dash=[1.3, 1.4], labels="uppercase", spacing=.06, halo=1.3, detail="motorsport"),
    "rivia": recipe("round", "round", case=.76, road=.88, path_dash=[1.3, 2], boundary_dash=[1.1, 1.5], spacing=.1, halo=1.8, detail="topographic"),
    "vice_coast": recipe("round", "round", land_edges=True, case=1.12, road=1.22, labels="uppercase", spacing=.04, halo=1.2, detail="coastal-neon"),
    "liberty": recipe("butt", "miter", land_edges=True, case=.88, road=1.05, labels="uppercase", spacing=.1, halo=1.2, detail="monochrome-city"),
    "ctos": recipe("round", "round", land_edges=True, case=1.0, road=1.14, labels="uppercase", spacing=.04, halo=1.25, detail="clean-network"),
    "hyrule": recipe("square", "bevel", land_edges=True, case=.55, road=.85, path_dash=[1.2, 1.8], boundary_dash=[1.2, 1.5], spacing=.12, halo=1.8, detail="slate"),
    "skyrim": recipe("round", "round", case=.46, road=.72, path_dash=[1, 2], boundary_dash=[1, 2], spacing=.13, halo=1.9, detail="cold-relief"),
    "lands_between": recipe("round", "round", case=.72, road=.9, path_dash=[1.3, 1.8], boundary_dash=[1.1, 1.4], spacing=.12, halo=1.75, detail="antique-atlas"),
    "horizon": recipe("round", "round", land_edges=True, case=1.06, road=1.22, path_dash=[1.4, 1.2], labels="uppercase", spacing=.05, halo=1.2, detail="racing-atlas"),
    "assassin": recipe("round", "round", case=.58, road=.82, path_dash=[1.2, 1.8], boundary_dash=[1.1, 1.5], spacing=.11, halo=1.7, detail="desert-relief"),
    "far_cry": recipe("round", "round", land_edges=True, case=.78, road=.98, path_dash=[1.4, 1.7], spacing=.07, halo=1.45, detail="rural-topographic"),
    "ghost": recipe("round", "round", case=.5, road=.76, path_dash=[1.1, 2], boundary_dash=[1.2, 1.7], spacing=.12, halo=1.8, detail="ink-wash"),
    "bridges": recipe("square", "miter", case=.48, road=.82, path_dash=[1.2, 1.6], boundary_dash=[1, 2], labels="uppercase", spacing=.12, halo=1.75, detail="logistics-network"),
    "horizon_wilds": recipe("round", "round", case=.52, road=.85, path_dash=[1.3, 2], boundary_dash=[1.2, 1.6], spacing=.1, halo=1.6, detail="organic-relief"),
    "spider": recipe("square", "miter", land_edges=True, case=.84, road=1.08, labels="uppercase", spacing=.08, halo=1.7, detail="extruded-grid"),
    "hogwarts": recipe("round", "round", case=.74, road=.9, path_dash=[1.2, 1.7], boundary_dash=[1.2, 1.7], spacing=.13, halo=1.8, detail="heraldic-relief"),
    "starfield": recipe("square", "miter", case=.42, road=.72, path_dash=[1, 2], boundary_dash=[1, 2.5], labels="uppercase", spacing=.16, halo=1.8, detail="survey-grid"),
    "kingdom": recipe("round", "round", case=.72, road=.92, path_dash=[1.3, 1.8], boundary_dash=[1.1, 1.6], spacing=.1, halo=1.7, detail="printed-medieval"),
    "saints": recipe("round", "round", land_edges=True, case=.9, road=1.08, labels="uppercase", spacing=.07, halo=1.45, detail="violet-city"),
    "just_cause": recipe("square", "bevel", case=.52, road=.82, path_dash=[1.2, 1.8], boundary_dash=[1.1, 1.8], spacing=.1, halo=1.8, detail="island-tactical"),
    "mafia": recipe("butt", "miter", land_edges=True, case=.92, road=1.08, labels="uppercase", spacing=.1, halo=1.2, detail="printed-city"),
    "division": recipe("square", "miter", land_edges=True, case=.7, road=1.02, path_dash=[1.2, 1.7], boundary_dash=[1, 2], labels="uppercase", spacing=.1, halo=1.8, detail="dark-tactical"),
    "sea_chart": recipe("round", "round", case=.82, road=.9, path_dash=[1.4, 1.8], boundary_dash=[1.1, 1.7], spacing=.13, halo=1.6, detail="nautical-engraving"),
    "dos_atlas": recipe("square", "miter", case=.5, road=.82, path_dash=[1, 1.8], boundary_dash=[1, 2.4], labels="uppercase", spacing=.13, halo=1.8, detail="amber-terminal"),
    "aincrad": recipe("round", "round", case=.78, road=.96, path_dash=[1.3, 1.8], boundary_dash=[1.2, 1.6], spacing=.12, halo=1.55, detail="quest-atlas"),
    "counter_strike": recipe("square", "miter", land_edges=True, case=1.25, road=1.22, path_dash=[1, 1.4], boundary_dash=[1, 2], labels="uppercase", spacing=.1, halo=1.45, detail="competitive-radar"),
    "defcon": recipe("square", "miter", case=.38, road=.68, path_dash=[1, 2.4], boundary_dash=[1, 2.8], labels="uppercase", spacing=.18, halo=1.8, detail="strategic-vector"),
    "field_ops": recipe("butt", "miter", land_edges=True, case=.76, road=.96, path_dash=[1.4, 1.8], boundary_dash=[1.2, 2], labels="uppercase", spacing=.1, halo=1.5, detail="field-briefing"),
    "retro_quest": recipe("square", "miter", land_edges=True, case=1.1, road=1.06, path_dash=[1, 1], boundary_dash=[1, 1.8], spacing=.05, halo=1.3, detail="16bit-adventure"),
    "home_console": recipe("round", "round", land_edges=True, case=1.0, road=1.1, path_dash=[1.4, 1.5], labels=None, spacing=.02, halo=1.2, detail="soft-console"),
    "nier": recipe("square", "miter", land_edges=True, case=.82, road=1.0, path_dash=[1.2, 1.8], boundary_dash=[1.1, 1.8], spacing=.02, halo=1.4, detail="machine-atlas"),
    "stalker": recipe("square", "miter", land_edges=True, case=.82, road=.95, path_dash=[1.2, 1.8], boundary_dash=[1.1, 2], labels="uppercase", spacing=.1, halo=1.65, detail="exclusion-pda"),
}


# Rendering order is part of each world's map grammar. Polygon strata stay
# below live road geometry; the ordered road and label layers set visual and
# collision priority without changing the underlying geography.
LAND_ORDERS = {
    "municipal": ("landuse", "landcover", "park"),
    "regional": ("landcover", "landuse", "park"),
    "wilderness": ("landcover", "park", "landuse"),
    "parkland": ("park", "landcover", "landuse"),
    "city-green": ("landuse", "park", "landcover"),
    "green-atlas": ("park", "landuse", "landcover"),
}

FEATURE_ORDERS = {
    "waterfront": ("hydro", "aeroway", "buildings", "boundaries", "hologram"),
    "building-led": ("hydro", "buildings", "aeroway", "boundaries", "hologram"),
    "boundary-survey": ("hydro", "boundaries", "aeroway", "buildings", "hologram"),
    "civic-grid": ("hydro", "aeroway", "boundaries", "buildings", "hologram"),
    "airfield": ("hydro", "buildings", "boundaries", "aeroway", "hologram"),
    "city-layer": ("hydro", "boundaries", "buildings", "aeroway", "hologram"),
}

ROAD_ORDERS = {
    "arterial": ("primary", "secondary", "local", "path", "rail", "ferry"),
    "trail": ("path", "local", "secondary", "primary", "rail", "ferry"),
    "motorsport": ("primary", "secondary", "local", "ferry", "path", "rail"),
    "field": ("local", "secondary", "primary", "path", "rail", "ferry"),
    "survey": ("rail", "ferry", "primary", "secondary", "local", "path"),
    "nautical": ("ferry", "rail", "path", "local", "secondary", "primary"),
    "exploration": ("path", "rail", "ferry", "local", "secondary", "primary"),
    "network": ("local", "path", "primary", "secondary", "rail", "ferry"),
    "coastal": ("ferry", "path", "local", "secondary", "primary", "rail"),
}

LABEL_ORDERS = {
    "city-first": ("city", "region", "road", "water", "river", "aerodrome"),
    "region-first": ("region", "city", "water", "river", "road", "aerodrome"),
    "airfield-led": ("region", "city", "aerodrome", "water", "river", "road"),
    "street-grid": ("city", "region", "water", "road", "river", "aerodrome"),
    "waterway-led": ("water", "river", "region", "city", "road", "aerodrome"),
    "road-led": ("city", "region", "road", "river", "water", "aerodrome"),
    "airport-grid": ("region", "city", "road", "aerodrome", "water", "river"),
    "town-led": ("city", "region", "aerodrome", "road", "river", "water"),
    "coastal-index": ("region", "city", "water", "aerodrome", "road", "river"),
    "route-atlas": ("city", "region", "water", "river", "aerodrome", "road"),
    "survey-index": ("city", "region", "aerodrome", "water", "road", "river"),
    "river-road": ("region", "city", "river", "water", "aerodrome", "road"),
    "shoreline-led": ("water", "city", "region", "river", "road", "aerodrome"),
}


def grammar(land: str, features: str, roads: str, labels: str) -> dict:
    """Select a reference-led layer stack and collision-priority grammar."""
    return {
        "land_order": LAND_ORDERS[land],
        "feature_order": FEATURE_ORDERS[features],
        "road_order": ROAD_ORDERS[roads],
        "label_order": LABEL_ORDERS[labels],
    }


# These choices control independent geography strata, road-class stacking,
# and label collision priority for every world. They are intentionally
# explicit so future art passes can tune one game's visual language without
# changing another world's grammar.
WORLD_GRAMMARS = {
    "aincrad": grammar("city-green", "airfield", "exploration", "airfield-led"),
    "assassin": grammar("wilderness", "boundary-survey", "trail", "region-first"),
    "bridges": grammar("wilderness", "airfield", "survey", "airport-grid"),
    "counter_strike": grammar("municipal", "city-layer", "field", "city-first"),
    "ctos": grammar("municipal", "civic-grid", "network", "street-grid"),
    "defcon": grammar("green-atlas", "city-layer", "survey", "shoreline-led"),
    "division": grammar("parkland", "civic-grid", "field", "town-led"),
    "dos_atlas": grammar("green-atlas", "airfield", "arterial", "survey-index"),
    "far_cry": grammar("wilderness", "building-led", "exploration", "road-led"),
    "field_ops": grammar("regional", "city-layer", "field", "river-road"),
    "ghost": grammar("parkland", "boundary-survey", "trail", "waterway-led"),
    "green_terminal": grammar("municipal", "city-layer", "field", "airport-grid"),
    "heat": grammar("municipal", "waterfront", "motorsport", "town-led"),
    "hogwarts": grammar("parkland", "city-layer", "exploration", "region-first"),
    "home_console": grammar("city-green", "building-led", "network", "route-atlas"),
    "horizon": grammar("municipal", "civic-grid", "motorsport", "airport-grid"),
    "horizon_wilds": grammar("wilderness", "boundary-survey", "trail", "road-led"),
    "hyrule": grammar("regional", "boundary-survey", "trail", "airfield-led"),
    "just_cause": grammar("regional", "city-layer", "survey", "coastal-index"),
    "kingdom": grammar("parkland", "airfield", "trail", "airfield-led"),
    "lands_between": grammar("wilderness", "airfield", "exploration", "airfield-led"),
    "liberty": grammar("municipal", "waterfront", "network", "region-first"),
    "los_santos": grammar("municipal", "waterfront", "arterial", "city-first"),
    "mafia": grammar("parkland", "boundary-survey", "arterial", "street-grid"),
    "nier": grammar("green-atlas", "airfield", "nautical", "coastal-index"),
    "night_city": grammar("municipal", "building-led", "network", "town-led"),
    "old_west": grammar("regional", "waterfront", "trail", "region-first"),
    "pixel": grammar("municipal", "waterfront", "arterial", "road-led"),
    "retro_quest": grammar("city-green", "waterfront", "exploration", "road-led"),
    "rivia": grammar("wilderness", "building-led", "trail", "airfield-led"),
    "saints": grammar("municipal", "civic-grid", "network", "road-led"),
    "san_andreas": grammar("municipal", "boundary-survey", "arterial", "street-grid"),
    "sea_chart": grammar("green-atlas", "city-layer", "nautical", "waterway-led"),
    "skyrim": grammar("regional", "city-layer", "trail", "river-road"),
    "spider": grammar("municipal", "civic-grid", "network", "city-first"),
    "stalker": grammar("green-atlas", "city-layer", "field", "airport-grid"),
    "starfield": grammar("green-atlas", "city-layer", "survey", "road-led"),
    "vice_coast": grammar("city-green", "waterfront", "coastal", "town-led"),
}

if set(WORLD_GRAMMARS) != set(RECIPES):
    raise ValueError("Every map recipe needs one reference-led structural grammar")
for theme_name, structural in WORLD_GRAMMARS.items():
    RECIPES[theme_name].update(structural)

assert len(RECIPES) == 38
assert len({(recipe["land_order"], recipe["feature_order"], recipe["road_order"], recipe["label_order"])
            for recipe in RECIPES.values()}) == 38
