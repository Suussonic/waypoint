# Map icon assets

Waypoint ships 38 original world-specific icon atlases. Each 640×1280 PNG uses
a 5×10 grid of 128-pixel cells: 49 live OpenStreetMap place categories and one
empty cell. Each world has a separate 128×128 destination-marker PNG.
The icons keep the real place category recognizable while changing the
silhouette, border, ornament, material, and color language for that world.

The daytime atlases are original Waypoint artwork generated from dedicated
world prompts and imported by
[`import_imagegen_poi_atlases.py`](../scripts/import_imagegen_poi_atlases.py).
[`generate_night_icon_variants.py`](../scripts/generate_night_icon_variants.py)
derives a separate, high-contrast night palette for every world while
preserving its icon shapes, details, and transparent background. It processes
each 128-pixel icon cell independently, preventing night glows from crossing
into a neighboring symbol. A visual QA pass identified 43 contaminated source
cells across nine worlds; their individual replacement prompts and importer
are recorded in [`poi-cell-repairs.jsonl`](../tools/imagegen/poi-cell-repairs.jsonl)
and [`import_imagegen_poi_cell_repairs.py`](../scripts/import_imagegen_poi_cell_repairs.py).
The app shifts
between these assets with its local day/night map mode. The independent
[`generate_map_assets.py`](../scripts/generate_map_assets.py) renderer supplies
the original vector fallback assets and cartographic material textures.

The shipped atlases do not contain game screenshots, extracted game sprites,
publisher logos, or pasted game UI. The actual in-game references that guided
each map and HUD are documented in the
[`visual sourcebook`](visual-sourcebook.md); those references inform new
Waypoint artwork and layouts. Map geometry, place names, and category semantics
still come from live OpenStreetMap/OpenMapTiles data. Each themed map key uses
world-specific terminology, with the real OSM class retained underneath it.

The atlas order follows `app/src/main/assets/poi_categories.json`; the last
cell in the runtime atlas is intentionally transparent. Destination markers
are separate images. The in-app map key uses the same world's day or night
atlas. Run
`scripts/validate_map_assets.py` to verify world coverage, dimensions,
transparent padding, and distinct day/night assets before packaging.
